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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.report;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

/**
 * An standard MBean that allows to view and clear the cached contents of the
 * {@link CompiledReportManager} through JMX.
 */
@ApplicationScoped
public class JmxReportCache implements JmxReportCacheMBean {

  public static final String MBEAN_NAME = "ReportCache";
  private static CompiledReportManager compiledReportManager = CompiledReportManager.getInstance();

  @Override
  public Set<String> getCachedReports() {
    return compiledReportManager.getCachedReports();
  }

  @Override
  public void clearCache() {
    compiledReportManager.clearCache();
  }

  @Override
  public boolean isEnabled() {
    return compiledReportManager.isCacheEnabled();
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled) {
      compiledReportManager.enableCache();
    } else {
      compiledReportManager.disableCache();
    }
  }

}
