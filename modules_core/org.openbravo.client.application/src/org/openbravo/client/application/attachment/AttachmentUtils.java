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
 * All portions are Copyright (C) 2015-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.attachment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.application.ParameterValue;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.ad.utility.AttachmentConfig;
import org.openbravo.model.ad.utility.AttachmentMethod;

/**
 * Utility class to manage Attachments. It includes a cached map of the attachment configuration of
 * each client in the system.
 *
 */
public class AttachmentUtils {
  private static Map<String, String> clientConfigs = new ConcurrentHashMap<String, String>();
  public static final String DEFAULT_METHOD = "Default";
  public static final String DEFAULT_METHOD_ID = "D7B1319FC2B340799283BBF8E838DF9F";
  private static final String CORE_DESC_PARAMETER = "E22E8E3B737D4A47A691A073951BBF16";
  private static final String DESCRIPTION_DELIMITER = "; ";
  private static final String SYSTEM_CLIENT_ID = "0";
  private static ApplicationDictionaryCachedStructures adcs = WeldUtils
      .getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class);

  /**
   * Gets the Attachment Configuration associated to the client active in OBContext
   * 
   * @return Activate Attachment Configuration for the current OBContext client
   */
  public static AttachmentConfig getAttachmentConfig() {
    Client client = OBContext.getOBContext().getCurrentClient();
    return getAttachmentConfig(client.getId());
  }

  /**
   * Gets the Attachment Configuration associated to the active client
   * 
   * @param clientId
   *          Client using openbravo
   * @return Activated Attachment Configuration for this client
   */
  public static AttachmentConfig getAttachmentConfig(String clientId) {
    String strAttachmentConfigId = clientConfigs.get(clientId);
    if (strAttachmentConfigId == null) {
      // Only one active AttachmentConfig is allowed per client.
      OBCriteria<AttachmentConfig> critAttConf = OBDal.getInstance()
          .createCriteria(AttachmentConfig.class);
      critAttConf.add(Restrictions.eq(AttachmentConfig.PROPERTY_CLIENT + ".id", clientId));
      if (!OBDal.getInstance().isActiveFilterEnabled()) {
        critAttConf.setFilterOnActive(true);
      }
      critAttConf.setMaxResults(1);
      AttachmentConfig attConf = (AttachmentConfig) critAttConf.uniqueResult();
      if (attConf == null && !SYSTEM_CLIENT_ID.equals(clientId)) {
        // If there is no specific configuration on the client search a generic configuration on
        // system client.
        attConf = getAttachmentConfig(SYSTEM_CLIENT_ID);
      }

      String attConfId = attConf != null ? attConf.getId() : "no-config";

      setAttachmentConfig(clientId, attConfId);
      return attConf;
    } else if ("no-config".equals(strAttachmentConfigId)) {
      return null;
    }
    return OBDal.getInstance().get(AttachmentConfig.class, strAttachmentConfigId);
  }

  /**
   * Updates the current active attachment configuration for the client.
   * 
   * @param strClient
   *          The Client whose attachment configuration has changed.
   * @param strAttConfig
   *          The new Attachment Configuration.
   */
  public static synchronized void setAttachmentConfig(String strClient, String strAttConfig) {
    if (strAttConfig == null) {
      clientConfigs.remove(strClient);
    } else {
      clientConfigs.put(strClient, strAttConfig);
    }
  }

  /**
   * Gets the Attachment Method related to the active Attachment Configuration of the current
   * client. In case the client does not have any attachment configuration the default Attachment
   * Method is returned.
   * 
   * @return The AttachmentMethod to use for the current client.
   */
  public static AttachmentMethod getAttachmentMethod() {
    AttachmentConfig attConfig = getAttachmentConfig();
    if (attConfig == null) {
      return AttachmentUtils.getDefaultAttachmentMethod();
    } else {
      return attConfig.getAttachmentMethod();
    }
  }

  /**
   * Gets the default Attachment Method
   * 
   * @return Default Attachment Method
   */
  public static AttachmentMethod getDefaultAttachmentMethod() {
    AttachmentMethod attMethod = OBDal.getInstance().get(AttachmentMethod.class, DEFAULT_METHOD_ID);
    if (attMethod == null) {
      throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoMethod"));
    }
    return attMethod;
  }

  /**
   * Get JSONObject list with data of the attachments in given tab and records
   * 
   * @param tab
   *          tab to take attachments
   * @param recordIds
   *          list of record IDs where taken attachments
   * @return List of JSONOject with attachments information values
   */
  public static List<JSONObject> getTabAttachmentsForRows(Tab tab, String[] recordIds) {
    String tableId = tab.getTable().getId();
    OBCriteria<Attachment> attachmentFiles = OBDao.getFilteredCriteria(Attachment.class,
        Restrictions.eq("table.id", tableId), Restrictions.in("record", (Object[]) recordIds));
    attachmentFiles.addOrderBy("creationDate", false);
    List<JSONObject> attachments = new ArrayList<>();
    // do not filter by the attachment's organization
    // if the user has access to the record where the file its attached, it has access to all its
    // attachments
    attachmentFiles.setFilterOnReadableOrganization(false);
    for (Attachment attachment : attachmentFiles.list()) {
      JSONObject attachmentobj = new JSONObject();
      try {
        attachmentobj.put("id", attachment.getId());
        attachmentobj.put("name", attachment.getName());
        attachmentobj.put("age",
            (new Date().getTime() - getLastUpdateOfAttachment(attachment).getTime()));
        attachmentobj.put("updatedby", attachment.getUpdatedBy().getName());
        String attachmentMethod = DEFAULT_METHOD_ID;
        if (attachment.getAttachmentConf() != null) {
          attachmentMethod = attachment.getAttachmentConf().getAttachmentMethod().getId();
        }
        attachmentobj.put("attmethod", attachmentMethod);
        attachmentobj.put("description",
            buildDescription(attachment, attachmentMethod, tab.getId()));
      } catch (JSONException ignore) {
      }
      attachments.add(attachmentobj);
    }
    return attachments;
  }

  private static Date getLastUpdateOfAttachment(Attachment attachment) {
    //@formatter:off
    String hql = 
            "select max(pv.updated) " +
            "  from OBUIAPP_ParameterValue pv " +
            " where pv.file.id = :fileId";
    //@formatter:on
    final Query<Date> query = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Date.class)
        .setParameter("fileId", attachment.getId())
        .setMaxResults(1);
    Date metadataLastUpdate = query.uniqueResult();
    if (metadataLastUpdate == null || attachment.getUpdated().after(metadataLastUpdate)) {
      return attachment.getUpdated();
    }
    return metadataLastUpdate;
  }

  /**
   * Get the String value of a parameter with a property path
   * 
   * @param parameter
   *          parameter in which is defined the property path
   * @param tabId
   *          table which stores the record with the desired value
   * @param recordId
   *          record which has the column with the value to search
   * @return the String value of the column indicated in the property path
   * @throws OBException
   *           generated if there is distinct than one record to search
   */
  public static Object getPropertyPathValue(Parameter parameter, String tabId, String recordId)
      throws OBException {
    Tab tab = adcs.getTab(tabId);
    Entity entity = ModelProvider.getInstance().getEntityByTableId(tab.getTable().getId());
    //@formatter:off
    final String hql = 
            "select a." + parameter.getPropertyPath() +
            "  from " + entity.getName() + " as a " +
            " where a.id=:recordId";
    //@formatter:on
    final Query<Object> query = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Object.class)
        .setParameter("recordId", recordId);
    try {
      return query.uniqueResult();
    } catch (Exception e) {
      throw new OBException(OBMessageUtils.messageBD("OBUIAPP_PropPathNotOneRecord"), e);
    }
  }

  @SuppressWarnings("deprecation")
  private static String buildDescription(Attachment attachment, String strAttMethodId,
      String tabId) {
    StringBuilder description = new StringBuilder();
    try {
      OBContext.setAdminMode(true);
      List<Parameter> parameters = adcs.getMethodMetadataParameters(strAttMethodId, tabId);
      boolean isfirst = true;
      for (Parameter param : parameters) {
        if (!param.isShowInDescription()) {
          continue;
        }

        final OBCriteria<ParameterValue> critStoredMetadata = OBDal.getInstance()
            .createCriteria(ParameterValue.class);
        critStoredMetadata.add(Restrictions.eq(ParameterValue.PROPERTY_FILE, attachment));
        critStoredMetadata.add(Restrictions.eq(ParameterValue.PROPERTY_PARAMETER, param));
        critStoredMetadata.setMaxResults(1);
        ParameterValue metadataStoredValue = (ParameterValue) critStoredMetadata.uniqueResult();
        if (CORE_DESC_PARAMETER.equals(param.getId()) && metadataStoredValue == null
            && StringUtils.isNotBlank(attachment.getText())) {
          // Attachment stored using old attach implementation with description. Create the
          // parameter value so the description text is shown and not lost.
          metadataStoredValue = OBProvider.getInstance().get(ParameterValue.class);
          metadataStoredValue.setFile(attachment);
          metadataStoredValue.setParameter(param);
          metadataStoredValue.setValueString(attachment.getText());
        }
        if (metadataStoredValue == null) {
          continue;
        }
        String value = ParameterUtils.getParameterStringValue(metadataStoredValue);
        if (StringUtils.isBlank(value)) {
          continue;
        }
        if (isfirst) {
          isfirst = false;
        } else {
          description.append(DESCRIPTION_DELIMITER);
        }
        description.append(
            (String) param.get(Parameter.PROPERTY_NAME, OBContext.getOBContext().getLanguage()));
        description.append(": ");
        description.append(value);
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return description.toString();
  }
}
