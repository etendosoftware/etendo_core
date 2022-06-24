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
 * All portions are Copyright (C) 2013-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.utility;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.Task;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.attachment.CoreAttachImplementation;
import org.openbravo.database.ConnectionProviderImpl;
import org.openbravo.utils.FileUtility;

/**
 * Migration of attachments based on the new attachment model
 * 
 * @author Shankar Balachandran
 */

public class MigrateAttachments extends Task {

  private static Logger log = LogManager.getLogger();

  @Override
  public void execute() {
    String attachPath = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("attach.path");
    log.info("Migrating Attachments");
    try {
      migrateAttachments(attachPath);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    log.info("Migration Successful");
  }

  /**
   * Delete the directory and all the sub files recursively.Extended from FileUtility as source
   * directory is not removed in FileUtility.delete method.
   * 
   * @param source
   *          Path of the file to be deleted
   */
  public static void delete(File source) throws Exception {
    FileUtility.delete(source);
    // delete the source directory
    source.delete();
  }

  /**
   * Migrates the attachments to new attachment model
   * 
   * @param attachPath
   *          The path of the attachments folder specified in Openbravo.properties
   * 
   */
  public static void migrateAttachments(String attachPath) throws Exception {
    int fileCount = 0;
    String[] names = null;
    String tableId = null, recordId = null, attachmentDirectory = null,
        newDirectoryStructure = null;
    Connection connection = null;
    PreparedStatement statement = null;
    try {
      connection = (new ConnectionProviderImpl(
          OBPropertiesProvider.getInstance().getOpenbravoProperties())).getConnection();
      boolean createDirectory = true;
      File files = new File(attachPath);
      if (files.isDirectory()) {
        for (File directory : files.listFiles()) {
          tableId = null;
          recordId = null;
          if (directory.isDirectory()) {
            if (directory.getName().contains("-")) {
              names = directory.getName().split("-");
              if (names.length == 2) {
                tableId = names[0];
                recordId = names[1];
                newDirectoryStructure = tableId + "/"
                    + CoreAttachImplementation.splitPath(recordId);
                attachmentDirectory = attachPath + "/" + newDirectoryStructure;
                if (!new File(attachmentDirectory).exists()) {
                  createDirectory = new File(attachmentDirectory).mkdirs();
                } else {
                  createDirectory = true;
                }
                if (createDirectory) {
                  for (File file : directory.listFiles()) {
                    File destination = new File(attachmentDirectory + "/" + file.getName());
                    FileUtility.copyFile(file.getAbsoluteFile(), destination);
                    if (destination.exists()) {
                      fileCount++;
                      // update path in c_file
                      if (connection != null) {
                        statement = null;
                        try {
                          String query = "UPDATE C_FILE SET PATH=? WHERE AD_TABLE_ID=? AND AD_RECORD_ID=? AND NAME=?";
                          statement = connection.prepareStatement(query);
                          statement.setString(1, newDirectoryStructure);
                          statement.setString(2, tableId);
                          statement.setString(3, recordId);
                          statement.setString(4, file.getName());
                          statement.executeUpdate();
                        } finally {
                          if (statement != null && !statement.isClosed()) {
                            statement.close();
                          }
                        }
                      } else {
                        log.error("Connection Failed!");
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      for (File directory : files.listFiles()) {
        if (directory.getName().contains("-") && directory.isDirectory()) {
          delete(directory);
        }
      }
      log.info("Number of files migrated: " + fileCount);
    } catch (Exception e) {
      log.error(e.getCause().getMessage(), e);
    } finally {
      try {
        if (connection != null) {
          connection.close();
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }
}
