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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.obps.ActivationKey.LicenseClass;

/** Redirects to the proper community branding local widget based on current license **/
public class StaticCommunityBranding extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    printPage(response);
  }

  private void printPage(HttpServletResponse response) throws IOException {
    final LicenseClass licenseClass = ActivationKey.getInstance().getLicenseClass();

    String strFilename = "/StaticCommunityBranding-MyOB";
    if (LicenseClass.COMMUNITY.equals(licenseClass)) {
      strFilename += "-Comm";
    } else if (LicenseClass.BASIC.equals(licenseClass)) {
      strFilename += "-Basic";
    } else if (LicenseClass.STD.equals(licenseClass)) {
      strFilename += "-STD";
    } else {
      // Unknown license class, showing community content.
      log4j.error("unknown license class: " + licenseClass.getCode());
      strFilename += "-Comm";
    }
    strFilename += ".html";

    redirect(response, strFilename);
  }

  private void redirect(HttpServletResponse response, String filename) throws IOException {
    String strUrl = strDireccion + "/web/html/";
    strUrl += "en_US" + filename;
    response.sendRedirect(strUrl);
  }
}
