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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Element;
import org.openbravo.dal.xml.XMLUtil;

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

  /**
   * Returns the instance of the MimeTypeUtil class
   * 
   * @return MimeTypeUtil instance
   */
  public static MimeTypeUtil getInstance() {
    return instance;
  }

  /**
   * Returns the MIME type name, (e.g. image/png) based on the byte array passed as parameter.
   * Returns application/octet-stream if no better match is found.
   * 
   * @param data
   *          byte array from which we want to detect the MIME type
   * @return MIME Type Name detected or application/octet-stream
   */
  public String getMimeTypeName(byte[] data) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
        BufferedInputStream bis = new BufferedInputStream(bais)) {
      return getMimeTypeName(bis);
    } catch (IOException ex) {
      logger.error("Failed to retrieve Mime Type.", ex);
      return NULL_MIME_TYPE;
    }
  }

  /**
   * Returns the MIME type name, (e.g. image/png) based on the file content passed as parameter.
   * Returns application/octet-stream if no better match is found.
   *
   * @param file
   *          file from which we want to detect the MIME type
   * @return MIME Type Name detected or application/octet-stream
   */
  public String getMimeTypeName(File file) {
    try (FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis)) {
      return getMimeTypeName(bis);
    } catch (IOException ex) {
      logger.error("File {} has not been found on Mime Type detection.", file.getName(), ex);
      return NULL_MIME_TYPE;
    }
  }

  /**
   * Returns the MIME type name, (e.g. image/png) based on the InputStream passed as parameter.
   * Returns application/octet-stream if no match is found.
   *
   * @param is
   *          InputStream used to determine MIME type
   * @return MIME Type Name detected or application/octet-stream
   */
  private String getMimeTypeName(InputStream is) {
    try {
      String mimeType = URLConnection.guessContentTypeFromStream(is);
      if (mimeType == null) {
        return NULL_MIME_TYPE;
      }
      if ("application/xml".equals(mimeType)) {
        Element xmlRootElement = XMLUtil.getInstance().getRootElement(is);
        if (xmlRootElement != null && "svg".equals(xmlRootElement.getName())) {
          return "image/svg+xml";
        }
      }
      return mimeType;
    } catch (IOException ex) {
      logger.error("Failed to detect MimeType.", ex);
      return NULL_MIME_TYPE;
    }
  }
}
