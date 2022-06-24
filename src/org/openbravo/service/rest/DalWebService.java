/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2008-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.rest;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.hibernate.ScrollMode;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.Check;
import org.openbravo.base.util.CheckException;
import org.openbravo.dal.core.DalMappingGenerator;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.dal.xml.EntityExcelXMLConverter;
import org.openbravo.dal.xml.EntityResolver.ResolvingMode;
import org.openbravo.dal.xml.EntityXMLConverter;
import org.openbravo.dal.xml.ModelXMLConverter;
import org.openbravo.dal.xml.XMLEntityConverter;
import org.openbravo.dal.xml.XMLUtil;
import org.openbravo.database.SessionInfo;
import org.openbravo.service.db.DataImportService;
import org.openbravo.service.db.ImportResult;
import org.openbravo.service.web.InvalidContentException;
import org.openbravo.service.web.InvalidRequestException;
import org.openbravo.service.web.ResourceNotFoundException;
import org.openbravo.service.web.WebService;
import org.openbravo.service.web.WebServiceUtil;

/**
 * The main Data Access Layer REST web service implementation. It covers the four REST HTTP methods:
 * DELETE, GET, POST and PUT. This service makes heavily use of the XML converters which translate
 * Entity instances from and to XML.
 * 
 * @see EntityXMLConverter
 * @see XMLEntityConverter
 * @author mtaal
 */

public class DalWebService implements WebService {

  public static final String PARAMETER_WHERE = "where";
  public static final String PARAMETER_ORDERBY = "orderBy";
  public static final String PARAMETER_FIRSTRESULT = "firstResult";
  public static final String PARAMETER_MAXRESULT = "maxResult";
  public static final String PARAMETER_INCLUDECHILDREN = "includeChildren";
  public static final String PARAMETER_EXCEL = "excel";
  // Parameter to specify the list of properties to be returned
  public static final String PARAMETER_PROPERTIES = "_selectedProperties";
  public static final String PARAMETER_NO_ACTIVE_FILTER = "_noActiveFilter";

  /**
   * Performs the GET REST operation. This service handles multiple types of request: the request
   * for the XML Schema of the REST webservices, a single Business Object and a list of Business
   * Objects is handled. The HttpRequest parameter 'template' makes it possible to process the XML
   * result through a XSLT stylesheet.
   * 
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    final String segment = WebServiceUtil.getInstance().getFirstSegment(path);
    final String[] segments = WebServiceUtil.getInstance().getSegments(path);

    String xml;
    Entity entity = null;
    if (segment == null || segment.length() == 0) {
      xml = XMLUtil.getInstance().toString(ModelXMLConverter.getInstance().getEntitiesAsXML());
    } else if (segment.equals("schema")) {
      xml = XMLUtil.getInstance().toString(ModelXMLConverter.getInstance().getSchema());
    } else if (segment.equals("hbm")) {
      xml = DalMappingGenerator.getInstance().generateMapping();
    } else {
      final String entityName = segment;

      try {
        entity = ModelProvider.getInstance().getEntity(entityName);
      } catch (final CheckException ce) {
        throw new ResourceNotFoundException("Resource " + entityName + " not found", ce);
      }

      // now check the second segment and see if an operation is required
      String id = null;
      boolean countOperation = false;
      if (segments.length == 2) {
        if (segments[1].equals("count")) {
          countOperation = true;
        } else {
          id = segments[1];
        }
      }

      final String includeChildrenStr = request.getParameter(PARAMETER_INCLUDECHILDREN);
      boolean includeChildren = true;
      if (includeChildrenStr != null) {
        includeChildren = Boolean.parseBoolean(includeChildrenStr);
      }

      if (id == null) {
        // show all of type entityname

        // check if there is a whereClause
        final String where = request.getParameter(PARAMETER_WHERE);
        final String orderBy = request.getParameter(PARAMETER_ORDERBY);
        final String firstResult = request.getParameter(PARAMETER_FIRSTRESULT);
        final String maxResult = request.getParameter(PARAMETER_MAXRESULT);

        String whereOrderByClause = "";
        if (where != null) {
          whereOrderByClause += where;
        }
        if (orderBy != null && !countOperation) {
          whereOrderByClause += " order by " + orderBy;
        }

        SessionInfo.setQueryProfile("xmlWebService");
        final OBQuery<BaseOBObject> obq = OBDal.getInstance()
            .createQuery(entityName, whereOrderByClause);

        if (request.getParameter(PARAMETER_NO_ACTIVE_FILTER) != null
            && request.getParameter(PARAMETER_NO_ACTIVE_FILTER).equals("true")) {
          obq.setFilterOnActive(false);
        }

        if (firstResult != null) {
          try {
            obq.setFirstResult(Integer.parseInt(firstResult));
          } catch (NumberFormatException e) {
            throw new InvalidRequestException(
                "Value of firstResult parameter is not an integer: " + firstResult);
          }
        }
        if (maxResult != null) {
          try {
            obq.setMaxResult(Integer.parseInt(maxResult));
          } catch (NumberFormatException e) {
            throw new InvalidRequestException(
                "Value of maxResult parameter is not an integer: " + firstResult);
          }
        }

        if (countOperation) {
          response.setContentType("text/xml;charset=UTF-8");
          final String xmlResult = WebServiceUtil.getInstance().createResultXML("" + obq.count());
          final Writer w = response.getWriter();
          w.write(xmlResult);
          w.close();
          return;
        } else {
          // do a bit more efficient for large datasets if there is no
          // template, in this case send the output directly to the
          // browser
          final boolean sendOutputDirectToBrowser = request.getParameter("template") == null;
          final Writer writer;
          if (sendOutputDirectToBrowser) {
            response.setContentType("text/xml;charset=UTF-8");
            writer = response.getWriter();
          } else {
            writer = new StringWriter();
          }
          if (request.getParameter(PARAMETER_EXCEL) != null) {
            final EntityExcelXMLConverter exc = EntityExcelXMLConverter.newInstance();
            exc.setOutput(writer);
            exc.export(obq.list());
          } else {
            final EntityXMLConverter exc = EntityXMLConverter.newInstance();
            if (request.getParameter(PARAMETER_PROPERTIES) != null) {
              addSelectedPropertiesToEXC(exc, request.getParameter(PARAMETER_PROPERTIES), entity);
            }
            exc.setClient(OBContext.getOBContext().getCurrentClient());
            exc.setReadableClients(OBContext.getOBContext().getReadableClients());
            exc.setOptionEmbedChildren(true);
            exc.setOptionIncludeChildren(includeChildren);
            exc.setOptionIncludeReferenced(false);
            exc.setOptionExportClientOrganizationReferences(true);
            exc.setOutput(writer);
            exc.setIncludedComputedColumns(true);
            // use the iterator because it can handle large data sets
            exc.setDataScroller(obq.scroll(ScrollMode.FORWARD_ONLY));
            exc.process(new ArrayList<BaseOBObject>());
          }
          if (sendOutputDirectToBrowser) {
            // must be the response writer
            Check.isSameObject(writer, response.getWriter());
            writer.flush();
            writer.close();
            // and go away
            return;
          } else {
            // must be a string writer in this case
            Check.isInstanceOf(writer, StringWriter.class);
            xml = writer.toString();
          }
        }
      } else {
        final OBQuery<BaseOBObject> obq = OBDal.getInstance()
            .createQuery(entityName, " id = :bobId")
            .setNamedParameter("bobId", id)
            .setFilterOnActive(false)
            .setMaxResult(1);
        final BaseOBObject result = obq.uniqueResult();

        if (result == null) {
          throw new ResourceNotFoundException(
              "No resource found for entity " + entityName + " using id " + id);
        }
        final StringWriter sw = new StringWriter();
        final EntityXMLConverter exc = EntityXMLConverter.newInstance();
        exc.setClient(OBContext.getOBContext().getCurrentClient());
        exc.setReadableClients(OBContext.getOBContext().getReadableClients());
        exc.setOptionEmbedChildren(true);
        exc.setOptionIncludeChildren(includeChildren);
        exc.setOptionIncludeReferenced(false);
        exc.setOptionExportClientOrganizationReferences(true);
        exc.setOutput(sw);
        if (request.getParameter(PARAMETER_PROPERTIES) != null) {
          addSelectedPropertiesToEXC(exc, request.getParameter(PARAMETER_PROPERTIES), entity);
        }
        exc.process(result);
        xml = sw.toString();
      }
    }
    if (request.getParameter("template") != null) {
      final String url = request.getRequestURL().toString();
      // add the correct ending
      if (url.endsWith("dal")) {
        throw new OBException(
            "The templates expect an url to end with dal/, the current url ends with just dal (without the /)");
      }
      final String templatedXml = WebServiceUtil.getInstance()
          .applyTemplate(xml, this.getClass().getResourceAsStream(request.getParameter("template")),
              url);
      response.setContentType("text/html;charset=UTF-8");
      final Writer w = response.getWriter();
      w.write(templatedXml);
      w.close();
    } else {
      response.setContentType("text/xml;charset=UTF-8");
      final Writer w = response.getWriter();
      w.write(xml);
      w.close();
    }
  }

  /**
   * Parses the PARAMETER_PROPERTIES parameter, creates two maps with the list of child and not
   * child properties for each entity beind exported and sets them in the EntityXMLConverter
   * instance
   * 
   * @param exc
   *          instance of the EntityXMLConverter where the properties will be set
   * @param selectedProperties
   *          list of the properties to be exported
   * @param entity
   *          top level entity being exported
   */
  private void addSelectedPropertiesToEXC(EntityXMLConverter exc, String selectedProperties,
      Entity entity) {
    if (selectedProperties == null || selectedProperties.isEmpty()) {
      return;
    }
    Map<String, List<String>> properties = null;
    Map<String, List<String>> childProperties = null;
    boolean parseChildProperties = false;
    // First parse the non child properties...
    properties = parseProperties(selectedProperties, entity, parseChildProperties);
    parseChildProperties = true;
    // ... and then the child properties
    childProperties = parseProperties(selectedProperties, entity, parseChildProperties);
    exc.setPropertiesToBeFetched(properties);
    exc.setChildPropertiesToBeFetched(childProperties);
  }

  /**
   * Parses the PARAMETER_PROPERTIES parameter and returns a map with a list of the properties that
   * should be exported for each entity being exported
   * 
   * @param propertiesStr
   *          PARAMETER_PROPERTIES parameter
   * @param entity
   *          top level entity being exported
   * @param childProperties
   *          flag to specify whether the child or the non child properties should be parsed
   */
  private Map<String, List<String>> parseProperties(String propertiesStr, Entity entity,
      boolean childProperties) {
    Map<String, List<String>> properties = new HashMap<String, List<String>>();
    for (String propertyStr : propertiesStr.split(",")) {
      propertyStr = propertyStr.trim();
      if (!propertyStr.isEmpty()) {
        String key = null;
        String value = null;
        Property property = null;
        if (propertyStr.contains(DalUtil.DOT)) {
          // property of a not top-level entity
          key = propertyStr.substring(0, propertyStr.lastIndexOf(DalUtil.DOT));
          value = propertyStr.substring(propertyStr.lastIndexOf(DalUtil.DOT) + 1);
          String[] propertyList;
          if (key.contains(DalUtil.DOT)) {
            propertyList = key.split("\\" + DalUtil.DOT);
          } else {
            propertyList = new String[1];
            propertyList[0] = key;
          }
          Entity auxEntity = entity;
          // Gets the entity the property belongs to
          for (String propertyName : propertyList) {
            auxEntity = auxEntity.getProperty(propertyName).getTargetEntity();
          }
          property = auxEntity.getProperty(value);
        } else {
          // property of the top level entity
          key = "_top";
          value = propertyStr;
          property = entity.getProperty(propertyStr);
        }
        if (property == null) {
          continue;
        }
        if (!childProperties && property.isChild()) {
          continue;
        }
        if (childProperties && !property.isChild()) {
          continue;
        }
        List<String> propertyList = properties.get(key);
        if (propertyList == null) {
          propertyList = new ArrayList<String>();
          propertyList.add("id");
        }
        if (!propertyList.contains(value)) {
          propertyList.add(value);
        }
        properties.put(key, propertyList);
      }
    }
    return properties;
  }

  /**
   * The POSt action corresponds to an import (of XML) of new Business Objects.
   * 
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response) {
    doChangeAction(path, request, response, ChangeAction.CREATE);
  }

  /**
   * The DELETE action can work in two modes: 1) if the URL points to a single Business Object then
   * that one is deleted, 2) if not then the posted information is assumed to be a XML String
   * identifying the Business Objects to delete.
   * 
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) {

    // check if the url points to a specific business object, if so remove
    // it!
    final String[] segments = WebServiceUtil.getInstance().getSegments(path);
    if (segments.length == 2) {
      final String entityName = segments[0];
      final String id = segments[1];
      final BaseOBObject result = OBDal.getInstance().get(entityName, id);
      if (result == null) {
        throw new OBException("Not found record for deletion: entity " + entityName + ", id " + id);
      }
      final String resIdentifier = result.getIdentifier();
      OBDal.getInstance().remove(result);
      OBDal.getInstance().commitAndClose();

      final String resultXml = WebServiceUtil.getInstance()
          .createResultXMLWithLogWarning("Action performed successfully",
              "Removed business object " + resIdentifier, null);
      try {
        response.setContentType("text/xml;charset=UTF-8");
        final Writer w = response.getWriter();
        w.write(resultXml);
        w.close();
      } catch (final Exception e) {
        throw new OBException(e);
      }
      return;
    }

    if (segments.length == 1) {
      final String entityName = segments[0];

      try {
        ModelProvider.getInstance().getEntity(entityName);
      } catch (final CheckException ce) {
        throw new ResourceNotFoundException("Resource " + entityName + " not found", ce);
      }

      final String where = request.getParameter(PARAMETER_WHERE);
      String whereOrderByClause = "";
      if (where != null) {
        whereOrderByClause += where;
      }

      try {
        final OBQuery<BaseOBObject> obq = OBDal.getInstance()
            .createQuery(entityName, whereOrderByClause);

        Object o = obq.deleteQuery().executeUpdate();

        OBDal.getInstance().commitAndClose();

        final String resultXml = WebServiceUtil.getInstance()
            .createResultXMLWithLogWarning("Action performed successfully",
                "Removed business objects " + o, null);
        response.setContentType("text/xml;charset=UTF-8");
        final Writer w = response.getWriter();
        w.write(resultXml);
        w.close();
      } catch (final Exception e) {
        throw new OBException(e);
      }

      return;
    }

    // use the content of the request
    doChangeAction(path, request, response, ChangeAction.DELETE);
  }

  /**
   * The PUT action will update existing business objects using the posted xml string.
   * 
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response) {
    // update a resource
    doChangeAction(path, request, response, ChangeAction.UPDATE);
  }

  protected void doChangeAction(String path, HttpServletRequest request,
      HttpServletResponse response, ChangeAction changeAction) {
    response.setContentType("text/xml;charset=UTF-8");
    final String resultXml = doChangeActionXML(path, request, response, changeAction);

    OBDal.getInstance().commitAndClose();

    try {
      final Writer w = response.getWriter();
      w.write(resultXml);
      w.close();
    } catch (final Exception e) {
      throw new OBException(e);
    }
    return;
  }

  protected String doChangeActionXML(String path, HttpServletRequest request,
      HttpServletResponse response, ChangeAction changeAction) {
    // get the resource

    // Disabled this check as it makes the system much more flexible....
    // it makes it possible to post xml with multiple entities to a url like
    // http://localhost:8080/openbravo/ws/dal
    // final String segment = WebServiceUtil.getInstance().getFirstSegment(path);
    // try {
    // ModelProvider.getInstance().getEntity(segment);
    // } catch (final CheckException ce) {
    // throw new ResourceNotFoundException("Resource " + segment + " not found", ce);
    // }

    try {
      final SAXReader reader = XMLUtil.getInstance().newSAXReader();
      final Document document = reader.read(request.getInputStream());

      // now parse the xml and let it be translated to a set of
      // of objects, note that referenced objects are supposed to be
      // present and are not inserted/updated.
      return importDataFromXML(document, changeAction);

    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  protected String importDataFromXML(Document doc, ChangeAction changeAction) {

    final XMLEntityConverter xec = XMLEntityConverter.newInstance();
    xec.setClient(OBContext.getOBContext().getCurrentClient());
    xec.setOrganization(OBContext.getOBContext().getCurrentOrganization());

    // for a webservice referenced entities should not be created at all!
    xec.getEntityResolver().setOptionCreateReferencedIfNotFound(false);

    // Retrieves the current resolving mode, to restore it after importing the data
    ResolvingMode prevResolvingMode = xec.getEntityResolver().getResolvingMode();

    // the create action also supports updating
    // an update action should only update
    // and a delete action should be lenient, allowing non existing
    // entities
    // so only update does not allow non-existing entities
    if (changeAction == ChangeAction.UPDATE) {
      xec.getEntityResolver().setResolvingMode(ResolvingMode.MUST_EXIST);
    }

    final List<BaseOBObject> processedObjects = xec.process(doc);

    xec.getEntityResolver().setResolvingMode(prevResolvingMode);

    if (xec.getErrorMessages() != null) {
      throw new InvalidContentException(xec.getErrorMessages());
    }

    // the create action also allows updating
    if (changeAction == ChangeAction.CREATE || changeAction == ChangeAction.UPDATE) {
      return doCreateUpdate(xec);
    } else if (changeAction == ChangeAction.DELETE) {
      return doDeleteAction(xec, processedObjects);
    } else {
      throw new OBException("Unsupported change action " + changeAction);
    }
  }

  protected String doDeleteAction(XMLEntityConverter xec, List<BaseOBObject> processedObjects) {
    final StringBuilder sb = new StringBuilder();
    int deleted = 0;
    int notDeleted = 0;
    final List<BaseOBObject> deletedList = new ArrayList<BaseOBObject>();
    for (final BaseOBObject bob : processedObjects) {
      final String originalId = xec.getEntityResolver().getOriginalId(bob);
      if (!bob.isNewOBObject()) {
        OBDal.getInstance().remove(bob);
        if (sb.length() > 0) {
          sb.append("\n");
        }
        sb.append("Removed business object " + bob.getIdentifier());
        if (originalId != null) {
          sb.append(", import id: " + originalId);
        }
        deletedList.add(bob);
        deleted++;
      } else {
        sb.append("Not removed business object " + bob.getIdentifier()
            + " because it could not be found");
        if (originalId != null) {
          sb.append(", import id: " + originalId);
        }
        notDeleted++;
      }
    }
    if (sb.length() > 0) {
      sb.append("\n");
    }
    sb.append("Removed " + deleted + " business objects, " + notDeleted
        + " business objects could not be found, so not removed");
    return WebServiceUtil.getInstance()
        .createResultXMLWithObjectsAndWarning("Action performed successfully", sb.toString(), null,
            null, null, deletedList);
  }

  protected String doCreateUpdate(XMLEntityConverter xec) {
    final ImportResult ir = new ImportResult();

    DataImportService.getInstance().saveUpdateConvertedObjects(xec, ir, false, null);
    if (ir.getErrorMessages() != null) {
      throw new InvalidContentException(ir.getErrorMessages());
    }
    final String log = (xec.getLogMessages() != null ? xec.getLogMessages() : "")
        + (xec.getLogMessages() != null ? "\n" : "") + "Updated " + xec.getToUpdate().size()
        + " business objects, Inserted " + xec.getToInsert().size() + " business objects ";

    return WebServiceUtil.getInstance()
        .createResultXMLWithObjectsAndWarning("Action performed successfully", log,
            xec.getWarningMessages(), xec.getToInsert(), xec.getToUpdate(), null);
  }
}
