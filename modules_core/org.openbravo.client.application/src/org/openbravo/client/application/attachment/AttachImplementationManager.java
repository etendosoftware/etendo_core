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
 * All portions are Copyright (C) 2015-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.attachment;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.HibernateException;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.application.ParameterValue;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.SecurityChecker;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.erpCommon.utility.MimeTypeUtil;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.List;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.ad.utility.AttachmentConfig;
import org.openbravo.model.ad.utility.AttachmentMethod;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.json.JsonUtils;
import org.openbravo.userinterface.selector.Selector;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Class that centralizes the Attachment Management. Any action to manage an attachment in Openbravo
 * should be done through this class.
 * 
 * The class checks what is the Attachment Method to use and calls the needed handler on each case.
 *
 */
@Dependent
public class AttachImplementationManager {

  private static final Logger log = LogManager.getLogger();
  private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

  public static final String REFERENCE_LIST = "17";
  public static final String REFERENCE_SELECTOR_REFERENCE = "95E2A8B50A254B2AAE6774B8C2F28120";

  @Inject
  @Any
  private Instance<AttachImplementation> attachImplementationHandlers;

  @Inject
  private ApplicationDictionaryCachedStructures adcs;

  private static final int DATA_TYPE_MAX_LENGTH = ModelProvider.getInstance()
      .getEntity(Attachment.class)
      .getProperty(Attachment.PROPERTY_DATATYPE)
      .getFieldLength();

  /**
   * Method to upload files. This method calls needed handler class
   * 
   * @param strTab
   *          the tab Id where the attachment is done
   * @param strKey
   *          the recordId where the attachment is done
   * @param strDocumentOrganization
   *          the organization ID of the record where the attachment is done
   * @param file
   *          The file to be uploaded
   * @throws OBException
   *           any exception thrown during the attachment uploading
   */
  public void upload(Map<String, String> requestParams, String strTab, String strKey,
      String strDocumentOrganization, File file) throws OBException {
    if (file == null) {
      throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoFileToAttach"));
    }

    Organization org = OBDal.getInstance().get(Organization.class, strDocumentOrganization);

    String strName = file.getName();

    AttachmentMethod attachMethod;
    Attachment attachment = null;
    try {
      OBContext.setAdminMode(true);
      Table table = adcs.getTab(strTab).getTable();
      attachment = getAttachment(table, strKey, strName);
      if (attachment == null) {
        attachment = OBProvider.getInstance().get(Attachment.class);
        attachment.setSequenceNumber(getSequenceNumber(table, strKey));
        attachment.setName(strName);
        attachment.setTable(table);
        attachment.setRecord(strKey);

        AttachmentConfig attachConf = AttachmentUtils.getAttachmentConfig();
        if (attachConf == null) {
          attachMethod = AttachmentUtils.getDefaultAttachmentMethod();
        } else {
          attachMethod = attachConf.getAttachmentMethod();
        }

        attachment.setAttachmentConf(attachConf);
      } else {
        // There is an attachment with the same file name for the record. Overwrite the file and
        // update the existing attachment.
        if (attachment.getAttachmentConf() != null) {
          attachMethod = attachment.getAttachmentConf().getAttachmentMethod();
        } else {
          attachMethod = AttachmentUtils.getDefaultAttachmentMethod();
        }
        // The information of the existing attachment may have not changed. Force the update of the
        // 'updated' field, the rest of the audit information will be updated automatically.
        attachment.setUpdated(new Date());
      }
      attachment.setOrganization(org);
      String strDataType = MimeTypeUtil.getInstance().getMimeTypeName(file);

      if (strDataType != null && strDataType.length() <= DATA_TYPE_MAX_LENGTH) {
        attachment.setDataType(strDataType);
      }

      checkReadableAccess(attachment);

      OBDal.getInstance().save(attachment);

      AttachImplementation handler = getHandler(attachMethod.getValue());

      if (handler == null) {
        throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoMethod"));
      }
      Map<String, Object> typifiedParameters = saveMetadata(requestParams, attachment, strTab,
          strKey, attachMethod);
      handler.uploadFile(attachment, strDataType, typifiedParameters, file, strTab);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * Method to update file's metadata. This method calls needed handler class
   * 
   * @param requestParams
   * 
   * @param attachID
   *          the attachmentID that will be updated
   * @param tabId
   *          the TabId where the attachment is being modified
   * @throws OBException
   *           any exception thrown when updating the document
   */
  public void update(Map<String, String> requestParams, String attachID, String tabId)
      throws OBException {
    try {
      OBContext.setAdminMode(true);

      Attachment attachment = OBDal.getInstance().get(Attachment.class, attachID);
      if (attachment == null) {
        throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoAttachmentFound"));
      }

      checkReadableAccess(attachment);

      AttachmentMethod attachMethod;
      if (attachment.getAttachmentConf() == null) {
        attachMethod = AttachmentUtils.getDefaultAttachmentMethod();
      } else {
        attachMethod = attachment.getAttachmentConf().getAttachmentMethod();
      }
      AttachImplementation handler = getHandler(attachMethod.getValue());

      if (handler == null) {
        throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoMethod"));
      }

      Map<String, Object> typifiedParameters = saveMetadata(requestParams, attachment, tabId,
          attachment.getRecord(), attachMethod);
      handler.updateFile(attachment, tabId, typifiedParameters);
      OBDal.getInstance().save(attachment);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Method to download a file. This method calls needed handler class
   * 
   * @param attachmentId
   *          the attachment Id that will be downloaded
   * @param os
   *          The output stream to dump the file
   * @throws OBException
   *           any exception thrown during the download
   */
  public void download(String attachmentId, OutputStream os) throws OBException {

    try {
      OBContext.setAdminMode(true);
      Attachment attachment = OBDal.getInstance().get(Attachment.class, attachmentId);

      if (attachment == null) {
        throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoAttachmentFound"));
      }

      checkReadableAccess(attachment);

      AttachImplementation handler = getHandler(attachment.getAttachmentConf() == null ? "Default"
          : attachment.getAttachmentConf().getAttachmentMethod().getValue());
      if (handler == null) {
        throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoMethod"));
      }
      File file = handler.downloadFile(attachment);
      if (file.exists()) {
        Files.copy(file.toPath(), os);
      } else {
        throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoAttachmentFound"));
      }

      boolean isTempFile = handler.isTempFile();
      if (isTempFile) {
        deleteTempFile(file);
      }

    } catch (IOException e) {
      throw new OBException(OBMessageUtils.messageBD("Error downloading file"), e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void deleteTempFile(File file) {
    Path parent = file.toPath().getParent();
    Path tmpDir = Paths.get(TEMP_DIR);
    file.delete();
    if (parent.equals(tmpDir)) {
      return;
    }
    try {
      // Delete also temporary file's parent directory if it is empty
      Files.delete(parent);
    } catch (IOException ioex) {
      log.error("Could not delete directory {}", parent, ioex);
    }
  }

  /**
   * Method to download all the files related to the record, in a single .zip file. This method
   * calls needed handler class
   * 
   * @param tabId
   *          The tab Id where the download process is being executed
   * @param recordIds
   *          All RecordIds from where are downloading the documents
   * @param os
   * @throws OBException
   *           any exception thrown during the download of all documents
   */

  public void downloadAll(String tabId, String recordIds, OutputStream os) throws OBException {

    try {
      OBContext.setAdminMode(true);
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      String tableId = tab.getTable().getId();
      final ZipOutputStream dest = new ZipOutputStream(os);
      HashMap<String, Integer> writtenFiles = new HashMap<String, Integer>();
      OBCriteria<Attachment> attachmentFiles = OBDal.getInstance().createCriteria(Attachment.class);
      attachmentFiles.add(Restrictions.eq("table.id", tableId));
      attachmentFiles.add(Restrictions.in("record", Arrays.asList(recordIds.split(","))));
      attachmentFiles.setFilterOnReadableOrganization(false);
      for (Attachment attachmentFile : attachmentFiles.list()) {
        checkReadableAccess(attachmentFile);
        AttachImplementation handler = getHandler(
            attachmentFile.getAttachmentConf() == null ? "Default"
                : attachmentFile.getAttachmentConf().getAttachmentMethod().getValue());
        if (handler == null) {
          throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoMethod"));
        }
        File file = handler.downloadFile(attachmentFile);
        if (!file.exists()) {
          throw new OBException(
              OBMessageUtils.messageBD("OBUIAPP_NoAttachmentFound") + " :" + file.getName());
        }
        String zipName = "";
        if (!writtenFiles.containsKey(file.getName())) {
          zipName = file.getName();
          writtenFiles.put(file.getName(), 0);
        } else {
          int num = writtenFiles.get(file.getName()) + 1;
          int indDot = file.getName().lastIndexOf(".");
          if (indDot == -1) {
            // file has no extension
            indDot = attachmentFile.getName().length();
          }
          zipName = attachmentFile.getName().substring(0, indDot) + " (" + num + ")"
              + attachmentFile.getName().substring(indDot);
          writtenFiles.put(attachmentFile.getName(), num);
        }
        byte[] buf = new byte[1024];
        dest.putNextEntry(new ZipEntry(zipName));

        FileInputStream in = new FileInputStream(file.toString());
        int len;
        while ((len = in.read(buf)) > 0) {
          dest.write(buf, 0, len);
        }
        dest.closeEntry();
        in.close();
        boolean isTempFile = handler.isTempFile();
        if (isTempFile) {
          deleteTempFile(file);
        }
      }
      dest.close();

    } catch (IOException e) {
      throw new OBException(OBMessageUtils.messageBD("OBUIAPP_ErrorWiththeFile"), e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * Method to delete files. This method calls needed handler class
   * 
   * @param attachment
   *          the attachment that will be removed
   * @throws OBException
   *           any exception thrown when deleting an attachment
   */
  public void delete(Attachment attachment) throws OBException {
    try {
      OBContext.setAdminMode(true);
      checkReadableAccess(attachment);
      AttachImplementation handler = getHandler(attachment.getAttachmentConf() == null ? "Default"
          : attachment.getAttachmentConf().getAttachmentMethod().getValue());
      if (handler == null) {
        throw new OBException(OBMessageUtils.messageBD("OBUIAPP_NoMethod"));
      }
      handler.deleteFile(attachment);
      OBDal.getInstance().remove(attachment);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * It gets the sequence number for the attachment
   * 
   * @param table
   *          the table of the attachment
   * @param recordId
   *          the recordId of the attachment
   * @return returns the sequence number.
   */
  private Long getSequenceNumber(Table table, String recordId) {
    OBCriteria<Attachment> obc = OBDal.getInstance().createCriteria(Attachment.class);
    obc.add(Restrictions.eq(Attachment.PROPERTY_RECORD, recordId));
    obc.add(Restrictions.eq(Attachment.PROPERTY_TABLE, table));
    obc.addOrderBy(Attachment.PROPERTY_SEQUENCENUMBER, false);
    obc.setFilterOnReadableOrganization(false);
    obc.setMaxResults(1);
    Attachment attach = (Attachment) obc.uniqueResult();
    if (attach == null) {
      return 10L;
    }
    return attach.getSequenceNumber() + 10L;
  }

  /**
   * Gets the attachment for given parameters.
   * 
   * @param table
   *          the table where the attachment is done
   * @param recordId
   *          The record ID where the attachment is done
   * @param fileName
   *          The name of the attachment
   * @return If exists, the attachment is returned. Else, null is returned
   */
  private Attachment getAttachment(Table table, String recordId, String fileName) {
    OBCriteria<Attachment> obc = OBDal.getInstance().createCriteria(Attachment.class);
    obc.add(Restrictions.eq(Attachment.PROPERTY_RECORD, recordId));
    obc.add(Restrictions.eq(Attachment.PROPERTY_NAME, fileName));
    obc.add(Restrictions.eq(Attachment.PROPERTY_TABLE, table));
    obc.setFilterOnReadableOrganization(false);
    obc.setMaxResults(1);
    return (Attachment) obc.uniqueResult();
  }

  /**
   * It gets the class that must be used, depending on the given attachMethod.
   * 
   * @param strAttachMethod
   *          attachmentMethod, that is the qualifier of the class.
   * @return Class needed which extends from AttachImplementation
   */
  public AttachImplementation getHandler(String strAttachMethod) {
    AttachImplementation handler = null;
    for (AttachImplementation nextHandler : attachImplementationHandlers
        .select(new ComponentProvider.Selector(strAttachMethod))) {
      if (handler == null) {
        handler = nextHandler;
      } else {
        throw new OBException(OBMessageUtils.messageBD("MoreThanOneImplementation"));
      }
    }
    return handler;
  }

  /**
   * Checks if the user has readable access to the record where the file is attached
   * 
   * @param attachment
   *          attachment to check access.
   */
  private void checkReadableAccess(Attachment attachment) {
    Entity entity = ModelProvider.getInstance().getEntityByTableId(attachment.getTable().getId());
    if (entity != null) {
      Object object = OBDal.getInstance().get(entity.getMappingClass(), attachment.getRecord());
      if (object instanceof OrganizationEnabled) {
        SecurityChecker.getInstance().checkReadableAccess((OrganizationEnabled) object);
      } else if (object == null) {
        throw new OBSecurityException(
            "Trying to create an attachment in table " + attachment.getTable()
                + " for a record with ID " + attachment.getRecord() + " that does not exists.");
      }
    }
  }

  /**
   * Save metadata in OBUIAPP_Parameter_Value records. It also updates the description of the
   * attachment based on the new metadata values.
   * 
   * @param requestParams
   *          Map with all the request parameters including the new values of the metadata as
   *          Strings.
   * @param attachment
   *          attachment for which is saving metadata.
   * @param tabId
   *          The tab id where the attachment is being done.
   * @param strKey
   *          The record id owner of the attachment.
   * @param attachMethod
   * @return Map of parameters with typified values
   * @throws OBException
   *           any exception thrown while saving metadata
   */
  private Map<String, Object> saveMetadata(Map<String, String> requestParams, Attachment attachment,
      String tabId, String strKey, AttachmentMethod attachMethod) throws OBException {
    Map<String, Object> metadataValues = new HashMap<String, Object>();
    for (Parameter parameter : adcs.getMethodMetadataParameters(attachMethod.getId(), tabId)) {
      final String strMetadataId = parameter.getId();

      ParameterValue metadataStoredValue = null;
      final OBCriteria<ParameterValue> critStoredMetadata = OBDal.getInstance()
          .createCriteria(ParameterValue.class);
      critStoredMetadata.add(Restrictions.eq(ParameterValue.PROPERTY_FILE, attachment));
      critStoredMetadata.add(Restrictions.eq(ParameterValue.PROPERTY_PARAMETER, parameter));
      critStoredMetadata.setMaxResults(1);
      try {
        metadataStoredValue = (ParameterValue) critStoredMetadata.uniqueResult();
      } catch (HibernateException e) {
        log.error("Error getting stored value of metadata. Attachment: " + attachment.getId()
            + " metadata: " + parameter.getId(), e);
        throw new OBException(OBMessageUtils.messageBD("OBUIAPP_ErrorInsertMetadata"), e);
      }
      if (metadataStoredValue == null) {
        metadataStoredValue = OBProvider.getInstance().get(ParameterValue.class);
        metadataStoredValue.setFile(attachment);
        metadataStoredValue.setParameter(parameter);
      }

      Object value = "";
      // Load the value. If the parameter is fixed calculate it, if not retrieve from the request
      // parameters.
      if (parameter.isFixed()) {
        if (parameter.getPropertyPath() != null) {
          value = AttachmentUtils.getPropertyPathValue(parameter, tabId, strKey);
        } else if (parameter.isEvaluateFixedValue()) {
          value = ParameterUtils.getParameterFixedValue(requestParams, parameter);
        } else {
          value = parameter.getFixedValue();
        }
      } else {
        value = requestParams.get(strMetadataId);
      }

      String strValue = "";
      if (value == null || (value instanceof String && StringUtils.isEmpty((String) value))) {
        metadataValues.put(strMetadataId, null);
        // There is no value for this parameter. Just do not create it if is new or set its value to
        // null if it already existed in order to keep the correct last updated information of the
        // attachment
        if (metadataStoredValue.isNewOBObject()) {
          OBDal.getInstance().remove(metadataStoredValue);
        } else {
          ParameterUtils.setParameterValue(metadataStoredValue, getJSONValue(""));
        }
      } else {
        String strReferenceId = parameter.getReference().getId();
        if (REFERENCE_LIST.equals(strReferenceId)) {
          strValue = (String) value;
          Reference reference = parameter.getReferenceSearchKey();
          for (List currentList : reference.getADListList()) {
            if (currentList.getSearchKey().equals(strValue)) {
              metadataStoredValue.setValueKey(currentList.getId());
              metadataStoredValue.setValueString(currentList.getName());
              JSONObject jsonValue = new JSONObject();
              try {
                jsonValue.put("id", currentList.getSearchKey());
                jsonValue.put("name", currentList.getName());
              } catch (JSONException ignore) {
              }
              metadataValues.put(strMetadataId, jsonValue);
              break;
            }
          }
        } else if (REFERENCE_SELECTOR_REFERENCE.equals(strReferenceId)) {
          strValue = (String) value;
          Reference reference = parameter.getReferenceSearchKey();
          Selector selector = reference.getOBUISELSelectorList().get(0);
          BaseOBObject object = OBDal.getInstance()
              .get(selector.getTable().getName(), strValue);
          metadataStoredValue.setValueKey(object.getId().toString());
          metadataStoredValue.setValueString(object.getIdentifier());
          JSONObject jsonValue = new JSONObject();
          try {
            jsonValue.put("id", object.getId().toString());
            jsonValue.put("name", object.getIdentifier());
          } catch (JSONException ignore) {
          }
          metadataValues.put(strMetadataId, jsonValue);
        } else {
          if (value instanceof Date) {
            strValue = JsonUtils.createDateFormat().format((Date) value);
          } else if (value instanceof BigDecimal) {
            strValue = ((BigDecimal) value).toPlainString();
          } else {
            strValue = value.toString();
          }

          ParameterUtils.setParameterValue(metadataStoredValue, getJSONValue(strValue));
          metadataValues.put(strMetadataId, ParameterUtils.getParameterValue(metadataStoredValue));
        }
        OBDal.getInstance().save(metadataStoredValue);
      }
    }

    return metadataValues;
  }

  private JSONObject getJSONValue(String value) {
    JSONObject jsonValue = new JSONObject();
    try {
      jsonValue.put("value", value);
    } catch (JSONException ignore) {
    }
    return jsonValue;
  }
}
