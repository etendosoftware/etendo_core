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

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.model.ad.module.Module;

/**
 * This class generates other components called session dynamic resources. This kind of resources
 * are loaded before the javascript content used in the application and they are provided by those
 * classes extending the {@link SessionDynamicTemplateComponent} class.
 */
public class SessionDynamicResourceComponent extends BaseComponent {

  private static final Logger log = LogManager.getLogger();

  @Inject
  @Any
  private Instance<SessionDynamicTemplateComponent> components;

  @Override
  public String generate() {
    StringBuilder result = new StringBuilder();
    for (SessionDynamicTemplateComponent component : getSortedComponentList()) {
      component.setParameters(getParameters());
      long t = System.currentTimeMillis();
      result.append(component.generate());
      log.debug("Generation of session dynamic resource {} took {} ms", component.getId(),
          System.currentTimeMillis() - t);
    }
    return escapeInvalidJSCharacters(result.toString());
  }

  @Override
  public Object getData() {
    return this;
  }

  @Override
  public String getETag() {
    // session dynamic resources are never cached
    return "" + System.currentTimeMillis();
  }

  public List<SessionDynamicTemplateComponent> getSortedComponentList() {
    List<SessionDynamicTemplateComponent> componentList = new ArrayList<>();
    final List<Module> modules = KernelUtils.getInstance().getModulesOrderedByDependency();
    for (Module module : modules) {
      for (SessionDynamicTemplateComponent component : components) {
        if (!component.getModule().getId().equals(module.getId())) {
          continue;
        }
        componentList.add(component);
      }
    }
    return componentList;
  }

  private String escapeInvalidJSCharacters(String js) {
    if (js == null) {
      return null;
    }

    // TODO: workaround for issue in current freemarker version, it should be removed once it gets
    // updated (see issue #36857)
    return js.replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
  }
}
