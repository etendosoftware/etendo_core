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
package org.openbravo.client.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.openbravo.base.provider.OBSingleton;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.BaseComponent;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

/**
 * This class generates the javascript code of the navigation bar components.
 * 
 */
public class NavigationBarComponentGenerator implements OBSingleton {

  @Inject
  private WeldUtils weldUtils;

  private static NavigationBarComponentGenerator instance;

  protected static synchronized NavigationBarComponentGenerator getInstance() {
    if (instance == null) {
      instance = WeldUtils.getInstanceFromStaticBeanManager(NavigationBarComponentGenerator.class);
    }
    return instance;
  }

  /**
   * Generates the code of the navigation bar components.
   * 
   * @param parameters
   *          A map with the parameters to be used in the generation of the component.
   * 
   * @return a Collection with the generated navigation bar components
   */
  protected Collection<NBComponent> getNavigationBarComponents(Map<String, Object> parameters) {
    return generateNavigationBarComponents(parameters, false);
  }

  /**
   * Generates the code of the dynamic navigation bar components.
   * 
   * @param parameters
   *          A map with the parameters to be used in the generation of the component.
   * 
   * @return a Collection with the generated dynamic navigation bar components
   */
  protected Collection<NBComponent> getDynamicNavigationBarComponents(
      Map<String, Object> parameters) {
    return generateNavigationBarComponents(parameters, true);
  }

  private Collection<NBComponent> generateNavigationBarComponents(Map<String, Object> parameters,
      boolean generateDynamicComponents) {
    final List<NBComponent> nbComponents = new ArrayList<NBComponent>();
    for (NavBarComponent nbc : getNavigationBarComponentList()) {

      if (!isAccessible(nbc) || (nbc.isStaticcomponent() && generateDynamicComponents)) {
        continue;
      }

      final NBComponent nbComponent = new NBComponent();

      String jsCode;
      if (nbc.isStaticcomponent() || generateDynamicComponents) {
        jsCode = generateComponent(nbc, parameters);
      } else {
        jsCode = "{className: '_OBNavBarDynamicComponent'}";
      }
      nbComponent.setJscode(jsCode);
      nbComponents.add(nbComponent);
    }
    return nbComponents;
  }

  private List<NavBarComponent> getNavigationBarComponentList() {
    OBCriteria<NavBarComponent> criteria = OBDal.getInstance()
        .createCriteria(NavBarComponent.class);
    criteria.addOrderBy(NavBarComponent.PROPERTY_RECORDSORTNO, true);
    return criteria.list();
  }

  private boolean isAccessible(NavBarComponent navBarComponent) {
    if (OBContext.getOBContext().getRole().getId().equals("0")) {
      return true;
    }
    if (navBarComponent.isAllroles()) {
      return true;
    }
    final String currentRoleId = OBContext.getOBContext().getRole().getId();
    for (NavbarRoleaccess roleAccess : navBarComponent.getOBUIAPPNavbarRoleaccessList()) {
      if (currentRoleId.equals(roleAccess.getRole().getId())) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private String generateComponent(NavBarComponent nbc, Map<String, Object> parameters) {
    try {
      final Class<BaseTemplateComponent> clz = (Class<BaseTemplateComponent>) OBClassLoader
          .getInstance()
          .loadClass(nbc.getJavaClassName());
      final BaseComponent component = weldUtils.getInstance(clz);
      component.setId(nbc.getId());
      if (component instanceof BaseTemplateComponent && nbc.getTemplate() != null) {
        ((BaseTemplateComponent) component).setComponentTemplate(nbc.getTemplate());
      }
      component.setParameters(parameters);
      return component.generate();
    } catch (Exception e) {
      throw new IllegalStateException("Exception when creating component " + nbc.getId(), e);
    }
  }

  public static class NBComponent {
    // NB stands for: Navigation Bar
    private String jscode;

    public void setJscode(String jscode) {
      this.jscode = jscode;
    }

    public String getJscode() {
      return jscode;
    }

    @Override
    public String toString() {
      return jscode;
    }
  }
}
