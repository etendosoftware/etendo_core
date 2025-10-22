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

package org.openbravo.client.application.window;

import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Exposes {@link ApplicationDictionaryCachedStructures} as a JMX Bean. */
@ApplicationScoped
public class JmxApplicationDictionaryCachedStructures
    implements JmxApplicationDictionaryCachedStructuresMBean {
  public static final String MBEAN_NAME = "ApplicationDictionaryCachedStructures";

  @Inject
  private ApplicationDictionaryCachedStructures adcs;

  @Override
  public void resetCache() {
    adcs.init();
  }

  @Override
  public boolean isEnabled() {
    return adcs.useCache();
  }

  @Override
  public Collection<String> getCachedWindows() {
    return adcs.getCachedWindows();
  }
}
