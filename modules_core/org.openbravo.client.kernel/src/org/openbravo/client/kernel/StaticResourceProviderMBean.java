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
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.util.List;
import java.util.Set;

/**
 * This interface allows to define the {@link StaticResourceProvider} class as an standard MBean
 * that can be managed through JMX.
 */
public interface StaticResourceProviderMBean {

  /**
   * @return a Set with the keys used in the static resources cache.
   */
  public Set<String> getCachedStaticResourceKeys();

  /**
   * @return a List with the names of the files that contain static resources.
   */
  public List<String> getStaticResourceFileNames();

  /**
   * Removes the cached information related to a static resource whose identifying name is passed as
   * parameter.
   * 
   * @param resourceName
   *          the identifying name of the static resource
   */
  public void removeStaticResourceCachedInfo(String resourceName);

  /**
   * Removes all the cached information about the static resources.
   */
  public void removeAllStaticResourceCachedInfo();
}
