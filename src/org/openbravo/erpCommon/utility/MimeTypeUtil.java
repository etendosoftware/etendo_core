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
 * All portions are Copyright (C) 2010-2020 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;

/**
 * Utility class to detect MIME type based on data array.
 * 
 * @author iperdomo
 */
public class MimeTypeUtil {
  private static MimeTypeUtil instance = new MimeTypeUtil();

  private static final Logger logger = LogManager.getLogger();

  // MIME type to be returned instead of null
  private static final String NULL_MIME_TYPE = "application/octet-stream";

  // Apache Tika instance (thread-safe)
  private static final Tika tika = new Tika();

  /**
   * Returns the instance of the MimeTypeUtil class
   * 
   * @return MimeTypeUtil instance
   */
  public static MimeTypeUtil getInstance() {
    return instance;
  }

  /**
   * Detects the MIME type of the given byte array using Apache Tika.
   * <p>
   * This method analyzes the content of the byte array to determine the most accurate
   * MIME type (e.g. {@code image/png}, {@code image/jpeg}, {@code image/x-ms-bmp}).
   * It does not rely on file extensions, making it useful for validating uploaded
   * file content.
   * </p>
   *
   * @param data
   *     the byte array representing the file content
   * @return the detected MIME type, or {@code application/octet-stream} if detection fails
   */
  public String getMimeTypeName(byte[] data) {
    try {
      return tika.detect(data);
    } catch (Exception ex) {
      logger.error("Failed to detect MIME type from byte array", ex);
      return NULL_MIME_TYPE;
    }
  }

  /**
   * Detects the MIME type of the given file using Apache Tika.
   * <p>
   * This method analyzes the file's content to determine its MIME type. It may also use
   * the file's extension as a secondary hint when needed. Useful for validating or processing
   * uploaded or temporary files.
   * </p>
   *
   * @param file
   *     the file to analyze
   * @return the detected MIME type, or {@code application/octet-stream} if detection fails
   */
  public String getMimeTypeName(File file) {
    try {
      return tika.detect(file);
    } catch (IOException ex) {
      logger.error("Failed to detect MIME type from file: {}", file.getName(), ex);
      return NULL_MIME_TYPE;
    }
  }
}
