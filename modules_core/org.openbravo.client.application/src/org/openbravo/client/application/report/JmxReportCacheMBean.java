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

/**
 * This interface allows to define the {@link JmxReportCache} class as an standard MBean that allows
 * to view and clear the cached contents of the {@link CompiledReportManager} through JMX.
 */
public interface JmxReportCacheMBean {
  /**
   * @return the set of report names currently available in cache.
   */
  public Set<String> getCachedReports();

  /**
   * Clears the content of the compiled reports cache.
   */
  public void clearCache();

  /**
   * @return {@code true} if the reports cache is enabled, {@code false} otherwise.
   */
  public boolean isEnabled();

  /**
   * Enables/Disables the reports cache.
   * 
   * @param enabled
   *          {@code true} to enable the cache, {@code false} otherwise.
   */
  public void setEnabled(boolean enabled);
}
