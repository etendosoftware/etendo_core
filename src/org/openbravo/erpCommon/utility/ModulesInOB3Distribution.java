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
 * All portions are Copyright (C) 2016 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */

package org.openbravo.erpCommon.utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains the a list of ids of the modules that are contained in the Openbravo 3.0 distribution
 */
public class ModulesInOB3Distribution {

  private static final List<String> ob3DistroModules = new ArrayList<String>() {
    private static final long serialVersionUID = 1L;
    {
      add("0"); // Core
      add("0138E7A89B5E4DC3932462252801FFBC"); // Openbravo 3.0
      add("0A060B2AF1974E8EAA8DB61388E9AECC"); // Query/List Widget
      add("2758CD25B2704AF6BBAD10365FC82C06"); // Workspace & Widgets
      add("2A5EE903D7974AC298C0504FBC4501A7"); // Payment Report
      add("3A3A943684D64DEF9EC39F588A656848"); // Orders Awaiting Delivery
      add("4B828F4D03264080AA1D2057B13F613C"); // User Interface Client Kernel
      add("5EB4F15C80684ACA904756BDC12ADBE5"); // User Interface Selector
      add("7E48CDD73B7E493A8BED4F7253E7C989"); // Openbravo 3.0 Framework
      add("883B5872CA0548F9AF2BBBE7D2DDFA61"); // Standard Roles
      add("96998CBC42744B3DBEE28AC8095C9335"); // 2.50 to 3.00 Compatibility Skin
      add("9BA0836A3CD74EE4AB48753A47211BCC"); // User Interface Application
      add("A44B9BA75C354D8FB2E3F7D6EB6BFDC4"); // JSON Datasource
      add("A918E3331C404B889D69AA9BFAFB23AC"); // Advanced Payables and Receivables Mngmt
      add("C70732EA90A14EC0916078B85CC33D2D"); // JBoss Weld
      add("D393BE6F22BB44B7B728259B34FC795A"); // HTML Widget
      add("D66395531D1E4364AFCD90FE6A8A5166"); // Openbravo 3 Demo Login Page
      add("EC356CEE3D46416CA1EBEEB9AB82EDB9"); // Smartclient
      add("F8D1B3ECB3474E8DA5C216473C840DF1"); // JSON REST Webservice
      add("FF8080812D842086012D844F3CC0003E"); // Widgets Collection
      add("FF8080813129ADA401312CA1222A0005"); // Integration with Google APIs
      add("FF8080813141B198013141B86DD70003"); // OpenID Service Integration
      add("FF8081812E008C6E012E00A613DC0019"); // Openbravo 3 Demo Sampledata API
      add("8A34B301DC524EA3A07513DF9F42CC90"); // Log Clean Up Utility
      add("8A098711BB324335A19833286BDB093D"); // Apache External Connection Pool

    }
  };

  public static List<String> getModules() {
    return ob3DistroModules;
  }

}
