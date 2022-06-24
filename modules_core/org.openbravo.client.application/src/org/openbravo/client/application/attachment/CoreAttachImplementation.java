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
 * All portions are Copyright (C) 2015-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.attachment;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.utility.Attachment;

/**
 * Default implementation of Attachment Management. This method saves the attached files in the
 * "attach.path" folder of Openbravo server. It is the method used when no other configuration is
 * provided.
 *
 */
@ApplicationScoped
@ComponentProvider.Qualifier(AttachmentUtils.DEFAULT_METHOD)
public class CoreAttachImplementation extends AttachImplementation {
  private static final Logger log = LogManager.getLogger();

  @Override
  public void uploadFile(Attachment attachment, String dataType, Map<String, Object> parameters,
      File file, String tabId) throws OBException {
    log.debug("CoreAttachImplemententation - Uploading files");
    String tableId = attachment.getTable().getId();
    String key = attachment.getRecord();
    String fileDirPath = getAttachmentDirectoryForNewAttachments(tableId, key);

    String attachmentFolderPath = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("attach.path");
    File uploadDir = null;
    uploadDir = new File(attachmentFolderPath + File.separator + fileDirPath);
    log.debug("Destination file before renaming: {}", uploadDir);
    try {
      // moveFileToDirectory not used as it does not allow to overwrite the destination file if it
      // exists.
      FileUtils.copyFileToDirectory(file, uploadDir, true);
      FileUtils.deleteQuietly(file);
    } catch (IOException e) {
      log.error("Error moving the file to: " + uploadDir, e);
      throw new OBException(
          OBMessageUtils.messageBD("UnreachableDestination") + " " + e.getMessage(), e);
    }

    attachment.setPath(getPath(fileDirPath));
    OBDal.getInstance().save(attachment);
  }

  @Override
  public File downloadFile(Attachment attachment) {
    log.debug("CoreAttachImplemententation - download file");
    String fileDirPath = getAttachmentDirectory(attachment.getTable().getId(),
        attachment.getRecord(), attachment.getName());
    String attachmentFolderPath = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("attach.path");
    final File file = new File(attachmentFolderPath + File.separator + fileDirPath,
        attachment.getName());
    return file;
  }

  @Override
  public void deleteFile(Attachment attachment) {
    log.debug("CoreAttachImplemententation - Removing files");
    String attachmentFolderPath = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("attach.path");
    String fileFolderPath = getAttachmentDirectory(attachment.getTable().getId(),
        attachment.getRecord(), attachment.getName());
    String absoluteFileFolderPath = attachmentFolderPath + "/" + fileFolderPath;
    final File file = new File(absoluteFileFolderPath, attachment.getName());
    if (file.exists()) {
      file.delete();
    } else {
      log.warn("No file was removed as file could not be found");
    }
  }

  @Override
  public void updateFile(Attachment attachment, String tabId, Map<String, Object> parameters)
      throws OBException {
    // Do nothing the metadata is saved in the database by the AttachImplementationManager.
    log.debug("CoreAttachImplemententation - Updating files");
  }

  @Override
  public boolean isTempFile() {
    // Return false as the downloaded file is the original file stored in the server.
    return false;
  }

  /**
   * Provides the directory in which the attachment has to be stored. For example for tableId "259",
   * recordId "0F3A10E019754BACA5844387FB37B0D5", the file directory returned is
   * "259/0F3/A10/E01/975/4BA/CA5/844/387/FB3/7B0/D5". In case 'SaveAttachmentsOldWay' preference is
   * enabled then the file directory returned is "259-0F3A10E019754BACA5844387FB37B0D5"
   * 
   * @param tableID
   *          UUID of the table
   * 
   * @param recordID
   *          UUID of the record
   * 
   * @return file directory to save the attachment
   */
  public static String getAttachmentDirectoryForNewAttachments(String tableID, String recordID) {
    String fileFolderPath = tableID + "-" + recordID;
    String saveAttachmentsOldWay = null;
    try {
      saveAttachmentsOldWay = Preferences.getPreferenceValue("SaveAttachmentsOldWay", true,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), null);
    } catch (PropertyException e) {
      // if property not found, save attachments the new way
      saveAttachmentsOldWay = "N";
    }

    if (Preferences.YES.equals(saveAttachmentsOldWay)) {
      return fileFolderPath;
    } else {
      fileFolderPath = tableID + "/" + splitPath(recordID);
    }
    return fileFolderPath;
  }

  /**
   * Provides the directory in which the attachment is stored. For example for tableId "259",
   * recordId "0F3A10E019754BACA5844387FB37B0D5", and fileName "test.txt" the file directory
   * returned is "259/0F3/A10/E01/975/4BA/CA5/844/387/FB3/7B0/D5". In case 'SaveAttachmentsOldWay'
   * preference is enabled then the file directory returned is
   * "259-0F3A10E019754BACA5844387FB37B0D5"
   * 
   * @param tableID
   *          UUID of the table
   * 
   * @param recordID
   *          UUID of the record
   * 
   * @param fileName
   *          Name of the file
   * 
   * @return file directory in which the attachment is stored
   */
  public static String getAttachmentDirectory(String tableID, String recordID, String fileName) {
    String fileFolderPath = tableID + "-" + recordID;
    Table attachmentTable = null;
    try {
      OBContext.setAdminMode();
      attachmentTable = OBDal.getInstance().get(Table.class, tableID);
      OBCriteria<Attachment> attachmentCriteria = OBDal.getInstance()
          .createCriteria(Attachment.class);
      attachmentCriteria.add(Restrictions.eq(Attachment.PROPERTY_RECORD, recordID));
      attachmentCriteria.add(Restrictions.eq(Attachment.PROPERTY_TABLE, attachmentTable));
      attachmentCriteria.add(Restrictions.eq(Attachment.PROPERTY_NAME, fileName));

      attachmentCriteria.setFilterOnReadableOrganization(false);
      attachmentCriteria.setMaxResults(1);

      Attachment attachment = (Attachment) attachmentCriteria.uniqueResult();
      if (attachment != null && attachment.getPath() != null) {
        fileFolderPath = attachment.getPath();
      }
    } catch (Exception e) {
      log.error("Error building attachment folder " + e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return fileFolderPath;
  }

  /**
   * Provides the value to be saved in path field in c_file. The path field is used to get the
   * location of the attachment. For example 259/0F3/A10/E01/975/4BA/CA5/844/387/FB3/7B0/D5. This
   * path is relative to the attachments folder
   * 
   * @param fileDirectory
   *          the directory that is retrieved from getFileDirectory()
   * 
   * @return value to be saved in path in c_file
   */
  public static String getPath(String fileDirectory) {
    if (fileDirectory != null && fileDirectory.contains("-")) {
      return null;
    } else {
      return fileDirectory;
    }
  }

  /**
   * Splits the path name component so that the resulting path name is 3 characters long sub
   * directories. For example 12345 is split to 123/45
   * 
   * @param origname
   *          Original name
   * @return split name.
   */
  public static String splitPath(final String origname) {
    String newname = "";
    for (int i = 0; i < origname.length(); i += 3) {
      if (i != 0) {
        newname += "/";
      }
      newname += origname.substring(i, Math.min(i + 3, origname.length()));
    }
    return newname;
  }

}
