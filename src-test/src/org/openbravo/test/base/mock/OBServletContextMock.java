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
 * All portions are Copyright (C) 2019-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.base.mock;

import java.nio.file.Path;

import org.openbravo.base.session.OBPropertiesProvider;

/**
 * ServletContext mock with the configuration and parameters required by Openbravo.
 */
public class OBServletContextMock extends ServletContextMock {

  /**
   * Create the mock ServletContext for Openbravo.
   */
  public OBServletContextMock() {
    super(getWebContentPath(), null);
    addInitParameter("BaseConfigPath", "WEB-INF");
    addInitParameter("BaseDesignPath", "src-loc");
    addInitParameter("DefaultDesignPath", "design");
    addInitParameter("AttachmentDirectory", getAttachmentPath());
  }

  /**
   * @param name
   *          the name of the file in the base context path.
   * 
   * @return the path of the provided file name in base context path.
   */
  public Path getPath(String name) {
    return getFile(name).toPath();
  }

  private static String getWebContentPath() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("source.path")
        + "/WebContent";
  }

  private static String getAttachmentPath() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("attach.path");
  }
}
