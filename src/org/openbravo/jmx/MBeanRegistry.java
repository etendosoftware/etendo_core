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
 * All portions are Copyright (C) 2017-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.jmx;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.DalContextListener;

/**
 * This class is intended to register the jmx beans defined in the application.
 */
public class MBeanRegistry {
  private static final Logger log = LogManager.getLogger();

  /**
   * Registers a pre-existing object as an MBean with the platform MBean server. The MBean will be
   * registered with the provided name inside a group called "Openbravo" with the current context
   * name as context: Openbravo-&gt; contextName -&gt; mBeanName
   * 
   * @param mBeanName
   *          the name of the MBean
   * @param mBean
   *          the MBean object
   */
  public static void registerMBean(String mBeanName, Object mBean) {
    try {
      ObjectName name = new ObjectName("Openbravo:" + getContextString() + "name=" + mBeanName);
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      mBeanServer.registerMBean(mBean, name);
    } catch (InstanceAlreadyExistsException alreadyRegistered) {
      log.debug("JMX instance already registered for {}, bean name: {}", mBeanName,
          alreadyRegistered.getMessage());
    } catch (Exception ignored) {
      log.error("Could not register {} as jmx bean", mBeanName, ignored);
    }
  }

  private static String getContextString() {
    String context = "";
    if (DalContextListener.getServletContext() != null) {
      context = "context="
          + DalContextListener.getServletContext().getContextPath().replace("/", "") + ",";
    }
    return context;
  }
}
