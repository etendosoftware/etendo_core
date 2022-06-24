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
 * All portions are Copyright (C) 2011-2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.attachment;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.util.Check;
import org.openbravo.base.util.CheckException;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.Attachment;

/**
 * Action handler to manage attachments.<br>
 * It implements 3 different commands. 'LOAD' used to fill the attachment section with the record
 * attachments. 'EDIT' used to update the metadata of an attachment. And 'DELETE' used to remove an
 * attachment.
 *
 */
public class AttachmentAH extends BaseActionHandler {

  private static final Logger log = LogManager.getLogger();

  @Inject
  private AttachImplementationManager aim;

  @Inject
  private ApplicationDictionaryCachedStructures adcs;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    OBContext.setAdminMode(true);
    String recordIds = "";
    String buttonId = "";
    String tabId = (String) parameters.get("tabId");
    Tab tab = null;
    try {
      Check.isNotNull(tabId, OBMessageUtils.messageBD("OBUIAPP_Attachment_Tab_Mandatory"));
      tab = adcs.getTab(tabId);

      final JSONObject request = new JSONObject(content);
      String command = (String) parameters.get("Command");

      if ("LOAD".equals(command)) {
        buttonId = (String) parameters.get("buttonId");
        recordIds = parameters.get("recordIds").toString();
      } else if ("EDIT".equals(command)) {
        JSONObject params = request.getJSONObject("_params");
        recordIds = params.getString("inpKey");
        buttonId = params.getString("buttonId");
        doEdit(parameters, request, params, tabId);
      } else if ("DELETE".equals(command)) {
        buttonId = (String) parameters.get("buttonId");
        recordIds = parameters.get("recordIds").toString();
        doDelete(parameters, tab, recordIds);
      } else {
        throw new UnsupportedOperationException("Unknown command " + command);
      }

      JSONObject obj = getAttachmentJSONObject(tab, recordIds);
      obj.put("buttonId", buttonId);
      return obj;
    } catch (JSONException e) {
      throw new OBException(OBMessageUtils.messageBD("OBUIAPP_AttachParamJSON"), e);
    } catch (CheckException e) {
      log.error("Error checking input parameters.", e);
      JSONObject obj = new JSONObject();
      try {
        obj.put("buttonId", parameters.get("buttonId"));
        obj.put("viewId", parameters.get("viewId"));
        obj.put("status", -1);
        obj.put("errorMessage", e.getMessage());
      } catch (JSONException ex) {
        // do nothing
      }

      return obj;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error managing attachments of tab " + tabId + " and record(s) " + recordIds, e);
      JSONObject obj = getAttachmentJSONObject(tab, recordIds);
      try {
        obj.put("buttonId", parameters.get("buttonId"));
        obj.put("viewId", parameters.get("viewId"));
        obj.put("status", -1);
        obj.put("errorMessage", e.getMessage());
      } catch (JSONException ex) {
        // do nothing
      }

      return obj;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void doEdit(Map<String, Object> parameters, JSONObject request, JSONObject params,
      String tabId) throws JSONException, OBException {
    final String attachmentId = (String) parameters.get("attachmentId");
    String strAttMethodId = (String) parameters.get("attachmentMethod");
    if (StringUtils.isBlank(strAttMethodId)) {
      strAttMethodId = AttachmentUtils.DEFAULT_METHOD_ID;
    }

    Map<String, String> requestParams = fixRequestMap(parameters, request);
    for (Parameter param : adcs.getMethodMetadataParameters(strAttMethodId, tabId)) {
      if (param.isFixed()) {
        continue;
      }
      String value;
      if (params.isNull(param.getDBColumnName())) {
        value = null;
      } else {
        value = params.getString(param.getDBColumnName());
      }
      requestParams.put(param.getId(), value);
    }

    aim.update(requestParams, attachmentId, tabId);
  }

  private void doDelete(Map<String, Object> parameters, Tab tab, String recordIds)
      throws OBException {
    String attachmentId = (String) parameters.get("attachId");
    String tableId = tab.getTable().getId();

    OBCriteria<Attachment> attachmentFiles = OBDao.getFilteredCriteria(Attachment.class,
        Restrictions.eq("table.id", tableId),
        Restrictions.in("record", (Object[]) recordIds.split(",")));
    // do not filter by the attachment's organization
    // if the user has access to the record where the file its attached, it has access to all
    // its attachments
    attachmentFiles.setFilterOnReadableOrganization(false);
    if (attachmentId != null) {
      attachmentFiles.add(Restrictions.eq(Attachment.PROPERTY_ID, attachmentId));
    }

    for (Attachment attachment : attachmentFiles.list()) {
      aim.delete(attachment);
    }
  }

  /**
   * Method to build the JSONObject that contains all the attachments related to the given tab's
   * record ids.
   * 
   * @param tab
   *          The tab the attachments belong to.
   * @param recordIds
   *          A string of ids concatenated by comma identifying the records whose attachments has to
   *          be loaded.
   * @return a JSONObject with an "attachment" key that includes a JSONArray with all the
   *         attachments.
   */
  public static JSONObject getAttachmentJSONObject(Tab tab, String recordIds) {
    List<JSONObject> attachments = AttachmentUtils.getTabAttachmentsForRows(tab,
        recordIds.split(","));
    JSONObject jsonobj = new JSONObject();
    try {
      jsonobj.put("attachments", new JSONArray(attachments));
    } catch (JSONException e) {
      throw new OBException(e);
    }
    return jsonobj;

  }
}
