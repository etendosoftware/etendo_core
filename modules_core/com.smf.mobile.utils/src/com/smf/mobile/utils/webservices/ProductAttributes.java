package com.smf.mobile.utils.webservices;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.io.BufferedReader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBCriteria.PredicateFunction;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Attribute;
import org.openbravo.model.common.plm.AttributeInstance;
import org.openbravo.model.common.plm.AttributeSet;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.AttributeUse;
import org.openbravo.model.common.plm.AttributeValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.json.JsonUtils;
import org.openbravo.service.web.WebService;

public class ProductAttributes implements WebService {
  private static final Logger log = LogManager.getLogger(ProductAttributes.class);
  
  private static final String PROPERTY_VALUE = "value";
  private static final String PROPERTY_ATTRIBUTE_ID = "id";
  private static final String PROPERTY_ATTRIBUTE_IDENTIFIER = "_identifier";
  private static final String DATE_FORMAT = "yyyy-MM-dd";


  /**
   * Gets the UI Model of the Product Attribute Selector
   * Parameters:
   *    windowId
   *    productId
   *    nameValue (Attribute Description when already present)
   *    keyValue (ID of Attribute when already present) 
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    JSONObject jsonResponse = new JSONObject();

    try {
      OBContext.setAdminMode();

      String windowId = request.getParameter("windowId") != null ? request.getParameter("windowId") : "";
      String tabId = request.getParameter("windowId") != null ? request.getParameter("tabId") : "";
      String productId = request.getParameter("productId") != null ? request.getParameter("productId") : "";
      String description = request.getParameter("description") != null ? request.getParameter("description") : "";
      String attributeId = request.getParameter("id") != null ? request.getParameter("id") : "";

      String strAttrSetValueType = "";
      AttributeSet attributeSet = null;
      AttributeSetInstance productInstance = null;

      final Product product = OBDal.getInstance().get(Product.class, productId);

      if (attributeId.equals("") || attributeId.equals("0")) {
        if (product != null && product.getAttributeSet() != null) {
          attributeSet = product.getAttributeSet();
          productInstance = product.getAttributeSetValue();
        }
      } else {
        productInstance = OBDal.getInstance().get(AttributeSetInstance.class, attributeId);
        attributeSet = productInstance != null ? productInstance.getAttributeSet() : null;
      }

      if (attributeSet == null || (attributeSet != null && "0".equals(attributeSet.getId()))) {
        throw new OBException(OBMessageUtils.messageBD("PAttributeNoSelection"));
      } else {
        if (product != null) {
          strAttrSetValueType = product.getUseAttributeSetValueAs();
        }

        if ("F".equals(strAttrSetValueType)) {
          throw new OBException(OBMessageUtils.messageBD("AttrSetValTypeSpecification"));
        } else {
          jsonResponse = getModel(description, attributeSet, productInstance,  windowId, tabId, product, request);
        }

      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      jsonResponse = new JSONObject(JsonUtils.convertExceptionToJson(e));
    } finally {
      OBContext.restorePreviousMode();
    }

    log.debug(jsonResponse.toString());

    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(jsonResponse.toString());
    w.close();

  }


  private JSONObject getModel(String nameValue, AttributeSet attributeSet, AttributeSetInstance attributeSetInstance,
      String windowId, String tabId, Product product, HttpServletRequest request) throws JSONException {

    JSONObject attribute = new JSONObject();
    JSONObject attributeValues = new JSONObject();
    DataToJsonConverter dataConverter = new DataToJsonConverter();

    String issotrx = Utility.getContext(new DalConnectionProvider(), new VariablesSecureApp(request), "isSOTrx", windowId);
    if (issotrx.equals("")) {
      issotrx = "N";
    }

    dataConverter.setSelectedProperties("id,_identifier,name,serialNo,serialNoControl,lot,lotControl,expirationDate,requireAtLeastOneValue");
    attribute = dataConverter.toJsonObject(attributeSet, DataResolvingMode.FULL_TRANSLATABLE);
    attribute.put("description", nameValue);

    if (attributeSetInstance != null) {
      attributeValues.put("lot", attributeSetInstance.getLotName());
      attributeValues.put("serialNo", attributeSetInstance.getSerialNo());
      attributeValues.put("expirationDate", attributeSetInstance.getExpirationDate());
      attributeValues.put("id", attributeSet.getId());
      attribute.put("values", attributeValues);
    }

    // Get assigned attributes
    OBCriteria<AttributeUse> criteria = OBDal.getInstance().createCriteria(AttributeUse.class);
    criteria.addEqual(AttributeUse.PROPERTY_ATTRIBUTESET, attributeSet);
    criteria.addOrderBy(AttributeUse.PROPERTY_SEQUENCENUMBER, true);
    List<AttributeUse> results = criteria.list();

    if (!results.isEmpty()) {
      JSONArray assignedAttributes = new JSONArray();

      for (AttributeUse result : results) {
        Attribute customAttribute = result.getAttribute();

        DataToJsonConverter attrUseDataConverter = new DataToJsonConverter();
        attrUseDataConverter.setSelectedProperties("id,_identifier,name,mandatory,instanceAttribute,list");
        JSONObject assignedAttribute = attrUseDataConverter.toJsonObject(customAttribute, DataResolvingMode.FULL_TRANSLATABLE);

        if (attributeSetInstance != null) {
          OBCriteria<AttributeInstance> valueCriteria = OBDal.getInstance().createCriteria(AttributeInstance.class);
          valueCriteria.addEqual(AttributeInstance.PROPERTY_ATTRIBUTESETVALUE, attributeSetInstance);
          valueCriteria.addFunction((cb, obc) -> cb.isNull(obc.getPath(AttributeInstance.PROPERTY_ATTRIBUTEVALUE)));
          List<AttributeInstance> valueResults = valueCriteria.list();

          if (!valueResults.isEmpty()) {
            assignedAttribute.put("value", valueResults.get(0).getSearchKey());
          }
        }

        if (customAttribute.isList()) {
          JSONArray listValues = new JSONArray();
          for (AttributeValue value : customAttribute.getAttributeValueList()) {
            DataToJsonConverter listValueDataConverter = new DataToJsonConverter();
            listValueDataConverter.setSelectedProperties("id,_identifier,name,searchKey");
            JSONObject listValue = listValueDataConverter.toJsonObject(value, DataResolvingMode.FULL_TRANSLATABLE);
            listValue.put("value", value.getId());
            listValue.put("label", value.getName());

            if (attributeSetInstance != null) {
              OBCriteria<AttributeInstance> listValueCriteria = OBDal.getInstance().createCriteria(AttributeInstance.class);
              listValueCriteria.addEqual(AttributeInstance.PROPERTY_ATTRIBUTESETVALUE, attributeSetInstance);
              listValueCriteria.addEqual(AttributeInstance.PROPERTY_ATTRIBUTEVALUE, value);
              List<AttributeInstance> listValueResults = listValueCriteria.list();

              if (!listValueResults.isEmpty()) {
                assignedAttribute.put("selectedValue", listValueResults.get(0).getAttributeValue().getId());
              }
            }

            listValues.put(listValue);
          }
          assignedAttribute.put("listValues", listValues);
        }

        assignedAttributes.put(assignedAttribute);
      }

      attribute.put("assignedAttributes", assignedAttributes);
    }

    //TODO special case for windowId 191 and isstrox N, with lot control sequences

    return attribute;
  }

  /**
   * Creates an attribute instance. Returns its ID and identifier
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    JSONObject jsonResponse = new JSONObject();

    try {

      String data = null;

      final BufferedReader reader = request.getReader();
      if (reader != null) {
        String line;
        
        final StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
          if (sb.length() > 0) {
            sb.append("\n");
          }
          sb.append(line);
        }
        
        log.debug("REQUEST CONTENT>>>>");
        log.debug(sb.toString());
        
        data = sb.toString();
        
        JSONObject jsonData = new JSONObject(data);
        
        Product product = OBDal.getInstance().get(Product.class, jsonData.getString("productId"));
        
        // TODO check if attribute exists
        jsonResponse = findAttributeInstance(jsonData);
        if (!jsonResponse.has("attributeId")) {
          // Not found, create:
          AttributeSetInstance instance = createAttributeSetInstance(product, jsonData);
          jsonResponse.put("id", instance.getId());
          jsonResponse.put("_identifier", instance.getDescription());
        }
        
      }



    } catch (Exception e) {
      log.error(e.getMessage(), e);
      jsonResponse = new JSONObject(JsonUtils.convertExceptionToJson(e));
    }
    log.debug(jsonResponse.toString());

    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(jsonResponse.toString());
    w.close();
  }
  
  public static JSONObject findAttributeInstance(JSONObject jsonsent) throws JSONException {
    JSONObject data = new JSONObject();
    List<String> attributeInstancesValues = new ArrayList<>();

    JSONObject attributes = jsonsent.getJSONObject("values");
    String locatorId = jsonsent.optString("locatorId");
    Iterator<?> keys = attributes.keys();

    Locator storageBin = locatorId != null ? OBDal.getInstance().get(Locator.class, locatorId) : null;

    OBCriteria<AttributeSetInstance> attributeCriteria = OBDal.getInstance()
        .createCriteria(AttributeSetInstance.class);

    while (keys.hasNext()) {
      String key = (String) keys.next();
      if ("lot".equals(key)) {

        String lot = attributes.getString("lot");
        if (lot != null && !lot.isEmpty()) {
          attributeCriteria.addEqual(AttributeSetInstance.PROPERTY_LOTNAME, lot);
        }

      } else if ("serialNo".equals(key)) {

        String serialno = attributes.getString("serialNo");

        if (serialno != null && !serialno.isEmpty()) {

          attributeCriteria.addEqual(AttributeSetInstance.PROPERTY_SERIALNO, serialno);
        }

      } else if ("expirationDate".equals(key)) {

        String guaranteeDateStr = attributes.getString("expirationDate");

        if (guaranteeDateStr != null && !guaranteeDateStr.isEmpty()) {
          Date guaranteeDate = null;
          DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
          try {
            guaranteeDate = dateFormat.parse(guaranteeDateStr);
            attributeCriteria
                .addEqual(AttributeSetInstance.PROPERTY_EXPIRATIONDATE, guaranteeDate);
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
        }

      }
    }
    
    JSONArray assignedAttributes = jsonsent.optJSONArray("assignedAttributes");
    
    if (assignedAttributes != null) {
      for (int i = 0; i < assignedAttributes.length(); i++) {
        JSONObject assignedAttribute = assignedAttributes.getJSONObject(i);
        String value = null;
        
        if (assignedAttribute.has("selectedValue")) {
          // is a List attribute
          JSONArray listValues = assignedAttribute.getJSONArray("listValues");
          for (int j = 0; j < listValues.length(); j++) {
            if (assignedAttribute.getString("selectedValue").equals(listValues.getJSONObject(j).getString("id"))) {
              value = listValues.getJSONObject(i).getString("name");
            }
          }
        } else {
          value = assignedAttribute.getString(PROPERTY_VALUE);
        }
        
        attributeInstancesValues.add(value);
        
      }
    }

    List<AttributeSetInstance> attributeSetInstances = attributeCriteria.list();
    if (attributeSetInstances.size() > 1) {
      if (!attributeInstancesValues.isEmpty()) {
        // Several attribute set instances with the basic attribute set, find according to attribute
        // instances
        OBCriteria<AttributeInstance> attributeInstanceCriteria = OBDal.getInstance()
            .createCriteria(AttributeInstance.class);
        attributeInstanceCriteria.addIn(AttributeInstance.PROPERTY_ATTRIBUTESETVALUE, attributeSetInstances);

        attributeInstanceCriteria
            .addIn(AttributeInstance.PROPERTY_SEARCHKEY, attributeInstancesValues);

        List<AttributeInstance> attributeInstances = attributeInstanceCriteria.list();

        if (!attributeInstances.isEmpty()) {

          // Several instances found with extra attribute values found, try to find if one exists
          // for the selected (from) locator, otherwise pick the first one
          List<StorageDetail> storageDetails = getStorageDetailList(storageBin, null,
              attributeInstances);

          if (!storageDetails.isEmpty()) {
            StorageDetail sd = storageDetails.get(0);
            data.put(PROPERTY_ATTRIBUTE_ID, sd.getAttributeSetValue().getId());
            data.put(PROPERTY_ATTRIBUTE_IDENTIFIER, sd.getAttributeSetValue().getDescription());
          } else {
            AttributeSetInstance attSetInstance = attributeInstances.get(0).getAttributeSetValue();
            data.put(PROPERTY_ATTRIBUTE_ID, attSetInstance.getId());
            data.put(PROPERTY_ATTRIBUTE_IDENTIFIER, attSetInstance.getDescription());
          }
        }
      } else {
        // Several instances found but no extra attribute values found, try to find if one exists
        // for the selected (from) locator, otherwise pick the first one

        List<StorageDetail> storageDetails = getStorageDetailList(storageBin, attributeSetInstances,
            null);

        if (!storageDetails.isEmpty()) {
          StorageDetail sd = storageDetails.get(0);
          data.put(PROPERTY_ATTRIBUTE_ID, sd.getAttributeSetValue().getId());
          data.put(PROPERTY_ATTRIBUTE_IDENTIFIER, sd.getAttributeSetValue().getDescription());
        } else {
          AttributeSetInstance attSetInstance = attributeSetInstances.get(0);
          data.put(PROPERTY_ATTRIBUTE_ID, attSetInstance.getId());
          data.put(PROPERTY_ATTRIBUTE_IDENTIFIER, attSetInstance.getDescription());
        }
      }

    } else if (!attributeSetInstances.isEmpty()) {
      AttributeSetInstance attSetInstance = attributeSetInstances.get(0);
      data.put(PROPERTY_ATTRIBUTE_ID, attSetInstance.getId());
      data.put(PROPERTY_ATTRIBUTE_IDENTIFIER, attSetInstance.getDescription());
    }

    return data;
  }
  
  private static List<StorageDetail> getStorageDetailList(Locator storageBin,
      List<AttributeSetInstance> attributeSetInstances,
      List<AttributeInstance> attributeInstances) {
    
    if (storageBin == null) {
      return new ArrayList<StorageDetail>();
    }
    
    OBCriteria<StorageDetail> sdCriteria = OBDal.getInstance().createCriteria(StorageDetail.class);
    sdCriteria.addEqual(StorageDetail.PROPERTY_STORAGEBIN, storageBin);

    if (attributeSetInstances != null) {
      sdCriteria
          .addIn(StorageDetail.PROPERTY_ATTRIBUTESETVALUE, attributeSetInstances);
    }

    if (attributeInstances != null) {
      List<AttributeSetInstance> attributes = new ArrayList<>();
      for (AttributeInstance attributeInstance : attributeInstances) {
        attributes.add(attributeInstance.getAttributeSetValue());
      }
      sdCriteria.addIn(StorageDetail.PROPERTY_ATTRIBUTESETVALUE, attributes);
    }

    return sdCriteria.list();
  }
  
  public static AttributeSetInstance createAttributeSetInstance(Product product,
      JSONObject jsonObject) throws JSONException {

    if (product.getAttributeSet() != null) {
      AttributeSetInstance attSetInst = null;

        try {
          // Atts are created in the organization of the product. Make sense to skip org permissions
          OBContext.setAdminMode(false);
          final JSONObject jsonAttValues = jsonObject.getJSONObject("values");
          final String serialNo = jsonAttValues.has("serialNo") ? jsonAttValues.getString("serialNo") : "";
          final String lot = jsonAttValues.has("lot") ? jsonAttValues.getString("lot") : "";
          final String strDate = jsonAttValues.has("expirationDate") ? jsonAttValues.getString("expirationDate") : "";
          Date expirationDate = null;

          if (!strDate.isEmpty()) {
            try {
              final String date = strDate.indexOf("T") < 0 ? strDate : strDate.substring(0,
                  strDate.indexOf("T"));
              expirationDate = new SimpleDateFormat("yyyy-MM-dd").parse(date);
            } catch (Exception e) {
              log.error(e.getMessage(), e);
            }
          }

          attSetInst = OBProvider.getInstance().get(AttributeSetInstance.class);
          attSetInst.setClient(product.getClient());
          attSetInst.setOrganization(product.getOrganization());
          attSetInst.setAttributeSet(product.getAttributeSet());
          attSetInst.setSerialNo(serialNo);
          attSetInst.setLotName(lot);
          attSetInst.setExpirationDate(expirationDate);

          OBDal.getInstance().save(attSetInst);

          final OBCriteria<AttributeUse> attributeuseCriteria = OBDal.getInstance().createCriteria(
              AttributeUse.class);
          attributeuseCriteria.addEqual(AttributeUse.PROPERTY_ATTRIBUTESET,
              product.getAttributeSet());
          attributeuseCriteria.addOrderBy(AttributeUse.PROPERTY_SEQUENCENUMBER, true);

          final List<AttributeUse> attributeUseResults = attributeuseCriteria.list();
          String attSetInstanceDescription = "";
          for (AttributeUse attUse : attributeUseResults) {
            String attValue = null;
            Attribute att = attUse.getAttribute();

            try {
              attValue = getAttributeValue(jsonObject.getJSONArray("assignedAttributes"), att);
            } catch (JSONException e) {
              log.error("An error happened while reading att values for att "
                  + att.getIdentifier() + ". " + e.getMessage());
              throw new OBException("An error happened while reading att values for att "
                  + att.getIdentifier() + ". " + e.getMessage());
            }

            if (att.isMandatory() && attValue == null) {
              throw new JSONException("Attribute (" + att.getIdentifier()
                  + "is not present in json");
            }

            if (attValue != null) {
              attSetInstanceDescription = attSetInstanceDescription.isEmpty() ? attValue
                  : attSetInstanceDescription + "_" + attValue;
              final AttributeInstance attInstance = (AttributeInstance) OBProvider.getInstance()
                  .get(AttributeInstance.ENTITY_NAME);
              Attribute currentAttribute = attUse.getAttribute();
              attInstance.setAttribute(currentAttribute);
              attInstance.setSearchKey(attValue);
              attInstance.setAttributeSetValue(attSetInst);
              if (currentAttribute.isList()) {
                OBQuery<AttributeValue> attValueQuery = OBDal.getInstance().createQuery(
                    AttributeValue.class, "attribute.id = :attId and name = :value");
                attValueQuery.setNamedParameter("value", attValue);
                attValueQuery.setNamedParameter("attId", currentAttribute.getId());
                attValueQuery.setFetchSize(1);
                AttributeValue attValueFound = attValueQuery.uniqueResult();
                if (attValueFound != null) {
                  attInstance.setAttributeValue(attValueFound);
                }
              }

              OBDal.getInstance().save(attInstance);
            }
          }

          if (lot != null && !lot.isEmpty()) {
            attSetInstanceDescription = attSetInstanceDescription.isEmpty() ? "L" + lot
                : attSetInstanceDescription + "_L" + lot;
          }
          if (serialNo != null && !serialNo.isEmpty()) {
            attSetInstanceDescription = attSetInstanceDescription.isEmpty() ? "#" + serialNo
                : attSetInstanceDescription + "_#" + serialNo;
          }
          if (expirationDate != null) {
            final String javaFormat = (String) OBPropertiesProvider.getInstance()
                .getOpenbravoProperties().get("dateFormat.java");
            final String strExpirationDate = new SimpleDateFormat(javaFormat)
                .format(expirationDate);
            attSetInstanceDescription = attSetInstanceDescription.isEmpty() ? strExpirationDate
                : attSetInstanceDescription + "_" + strExpirationDate;
          }

          attSetInst.setDescription(attSetInstanceDescription);
          OBDal.getInstance().save(attSetInst);

          OBDal.getInstance().flush();

        } finally {
          OBContext.restorePreviousMode();
        }
        return attSetInst;
    }
    return OBDal.getInstance().get(AttributeSetInstance.class, "0");
  }
  
  private static String getAttributeValue(JSONArray attValues, Attribute att) throws JSONException {
    
    for (int i = 0; i < attValues.length(); i++) {
      JSONObject attValue = attValues.getJSONObject(i);
      
      if (att.getId().equals(attValue.getString("id"))) {
        
        if (att.isList()) {
          JSONArray listValues = attValue.getJSONArray("listValues");
          for (int j = 0; j < listValues.length(); j++) {
            if (attValue.getString("selectedValue").equals(listValues.getJSONObject(j).getString("id"))) {
              return listValues.getJSONObject(i).getString("name");
            }
          }
        }
        
        return attValue.getString("value");
      }
    }
    
    return null;
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

}
