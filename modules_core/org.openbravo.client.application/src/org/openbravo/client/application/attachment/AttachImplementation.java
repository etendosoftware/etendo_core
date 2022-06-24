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
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.attachment;

import java.io.File;
import java.util.Map;

import org.openbravo.base.exception.OBException;
import org.openbravo.model.ad.utility.Attachment;

/**
 * Public class to allow extend the Attachment Functionality. Modules implementing new attachment
 * method must implement this class. The class must implement the
 * org.openbravo.client.kernel.ComponentProvider.Qualifier with the Attachment Method's Search Key
 * so the AttachImplementationManager can load the corresponding class.
 */

public abstract class AttachImplementation {

  /**
   * Method in charge of File upload. Invoked when a new file is attached to a record.
   * 
   * @param attachment
   *          The attachment created in c_file
   * @param dataType
   *          DataType of the attachment
   * @param parameters
   *          A map with the metadata and its values to be updated in the corresponding file
   *          management system and in the attachment
   * @param file
   *          The file to be uploaded
   * @param tabId
   *          The tabID where the file is attached
   * @throws OBException
   *           Thrown when any error occurs during the upload
   */
  public abstract void uploadFile(Attachment attachment, String dataType,
      Map<String, Object> parameters, File file, String tabId) throws OBException;

  /**
   * Method invoked when an attached file needs to be downloaded from the corresponding file
   * management system.
   * 
   * @param attachment
   *          The attachment that will be downloaded
   * @return the attached file.
   * @throws OBException
   *           Thrown when any error occurs during the download
   */
  public abstract File downloadFile(Attachment attachment) throws OBException;

  /**
   * Method invoked when an attached file needs to be removed from the corresponding file management
   * system.
   * 
   * @param attachment
   *          The attachment to be removed
   * @throws OBException
   *           Thrown when any error occurs when deleting the file
   */
  public abstract void deleteFile(Attachment attachment) throws OBException;

  /**
   * Method invoked when an existing attachment metadata is updated.
   * 
   * @param attachment
   *          The attachment to be modified
   * @param tabId
   *          The tabID where the file was attached
   * @param parameters
   *          The metadata with the new values.
   * @throws OBException
   *           Thrown when any error occurs when updating the file
   */
  public abstract void updateFile(Attachment attachment, String tabId,
      Map<String, Object> parameters) throws OBException;

  /**
   * Method invoked to check if the downloaded file has to be deleted when the download is completed
   * by the user. Some attachment methods might create temporary files in Openbravo server that
   * should be removed when the download is completed.
   * 
   * @return true if the downloaded File has to be deleted.
   */
  public abstract boolean isTempFile();

}
