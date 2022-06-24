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
package org.openbravo.cluster;

import java.util.Date;
import java.util.Map;

/**
 * This interface allows to define the {@link JmxClusterServiceManager} class as an standard MBean
 * that allows to display and manage some of the cluster services settings through JMX.
 */
public interface JmxClusterServiceManagerMBean {

  /**
   * @return the unique identifier of the current cluster node.
   */
  public String getCurrentNodeId();

  /**
   * @return the name of the current cluster node.
   */
  public String getCurrentNodeName();

  /**
   * @return the Date of the last ping done (for any service) by the current node.
   */
  public Date getLastPingOfCurrentNode();

  /**
   * @return a Map with information (leader and last ping) per available cluster service.
   */
  public Map<String, String> getClusterServiceLeaders();

  /**
   * @return a Map with information of the settings for each available cluster service.
   */
  public Map<String, String> getClusterServiceSettings();

  /**
   * Enables the ping service for a particular cluster service.
   * 
   * @param serviceName
   *          the name of the service for which the ping is enabled.
   */
  public void enablePingForService(String serviceName);

  /**
   * Forces the current node to stop doing pings for a particular cluster service. In case the
   * current node is the node in charge of handling the service it will be unregistered as the
   * leader of the service.
   * 
   * @param serviceName
   *          the name of the service for which the ping is disabled.
   */
  public void disablePingForService(String serviceName);
}
