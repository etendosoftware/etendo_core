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
 * All portions are Copyright (C) 2015-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.importprocess;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.openbravo.base.weld.WeldUtils;
import org.openbravo.cluster.ClusterServiceManager;

/**
 * Initializes the import process layer by calling {@link ImportEntryManager#start()} and
 * {@link ImportEntryManager#shutdown()} when the application stops. It also does the same for the
 * {@link ClusterServiceManager}.
 * 
 * @author mtaal
 */
public class ImportProcessContextListener implements ServletContextListener {

  private ImportEntryManager importEntryManager;
  private ClusterServiceManager clusterServiceManager;

  @Override
  public void contextInitialized(ServletContextEvent event) {
    importEntryManager = WeldUtils.getInstanceFromStaticBeanManager(ImportEntryManager.class);
    importEntryManager.start();
    clusterServiceManager = WeldUtils.getInstanceFromStaticBeanManager(ClusterServiceManager.class);
    clusterServiceManager.start();
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    importEntryManager.shutdown();
    clusterServiceManager.shutdown();
  }
}
