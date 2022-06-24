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
 * All portions are Copyright (C) 2009-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.model.ad.module.Module;

/**
 * Base implementation, can be extended.
 * 
 * @author mtaal
 */
public abstract class BaseComponentProvider implements ComponentProvider {

  private Module module;

  private static Map<String, List<String>> appDependencies = new ConcurrentHashMap<>();

  /**
   * Return a component of the correct implementation using Weld.
   * 
   * @param clz
   *          an instance of this class will be returned
   * @return an instance of clz
   */
  protected <U extends Component> U getComponent(Class<U> clz) {
    return WeldUtils.getInstanceFromStaticBeanManager(clz);
  }

  @Override
  public Module getModule() {
    if (module != null) {
      return module;
    }
    module = KernelUtils.getInstance().getModule(getModulePackageName());
    return module;
  }

  public static void setAppDependencies(String app, List<String> dependencies) {
    appDependencies.put(app, dependencies);
  }

  /**
   * Computes parameters to add to a link of a resource. The parameters include the version and
   * language of the user.
   * 
   * The version computation logic depends on if the module is in development (
   * {@link Module#isInDevelopment()}. If in developers mode then the
   * {@link System#currentTimeMillis()} is used. If not in developers mode then the
   * {@link Module#getVersion()} is used. These values are prepended with the language id of the
   * user. This makes it possible to generate language specific components on the server.
   * 
   * @param resource
   *          , the resource to compute the version string for, is typically a resource provided by
   *          the getGlobalResources method
   * @return the version parameter string, a concatenation of the version and language with
   *         parameter names
   * @see KernelConstants#RESOURCE_VERSION_PARAMETER
   * @see KernelConstants#RESOURCE_LANGUAGE_PARAMETER
   * @see KernelUtils#getVersionParameters(Module)
   */
  @Override
  public String getVersionParameters(String resource) {
    return KernelUtils.getInstance().getVersionParameters(getModule());
  }

  /**
   * Override this method if the component is in a different package than the module.
   */
  protected String getModulePackageName() {
    return this.getClass().getPackage().getName();
  }

  protected ComponentResource createComponentResource(ComponentResourceType type, String path) {
    final ComponentResource resource = new ComponentResource();
    resource.setType(type);
    resource.setPath(path);
    return resource;
  }

  protected ComponentResource createComponentResource(ComponentResourceType type, String path,
      String validForApp) {
    final ComponentResource resource = new ComponentResource();
    resource.setType(type);
    resource.setPath(path);
    resource.addValidForApp(validForApp);
    return resource;
  }

  protected ComponentResource createComponentResource(ComponentResourceType type, String path,
      List<String> validForAppList) {
    final ComponentResource resource = new ComponentResource();
    resource.setType(type);
    resource.setPath(path);
    resource.setValidForAppList(validForAppList);
    return resource;
  }

  protected ComponentResource createStaticResource(String path, boolean includeAlsoInClassicMode,
      boolean includeInNewUIMode) {

    final ComponentResource componentResource = createComponentResource(
        ComponentResourceType.Static, path);

    if (includeAlsoInClassicMode) {
      componentResource.addValidForApp(ComponentResource.APP_CLASSIC);
    }

    if (includeInNewUIMode) {
      componentResource.addValidForApp(ComponentResource.APP_OB3);
    }

    return componentResource;
  }

  protected ComponentResource createStaticResource(String path, boolean includeAlsoInClassicMode) {
    final ComponentResource componentResource = createComponentResource(
        ComponentResourceType.Static, path);

    // mimic old behavior *always* include for OB3
    componentResource.addValidForApp(ComponentResource.APP_OB3);

    if (includeAlsoInClassicMode) {
      componentResource.addValidForApp(ComponentResource.APP_CLASSIC);
    }

    return componentResource;
  }

  protected ComponentResource createStyleSheetResource(String path,
      boolean includeAlsoInClassicMode, boolean includeInNewUIMode) {
    final ComponentResource componentResource = createComponentResource(
        ComponentResourceType.Stylesheet, path);

    if (includeAlsoInClassicMode) {
      componentResource.addValidForApp(ComponentResource.APP_CLASSIC);
    }

    if (includeInNewUIMode) {
      componentResource.addValidForApp(ComponentResource.APP_OB3);
    }

    return componentResource;
  }

  protected ComponentResource createStyleSheetResource(String path,
      boolean includeAlsoInClassicMode) {
    final ComponentResource componentResource = new ComponentResource();
    componentResource.setType(ComponentResourceType.Stylesheet);
    componentResource.setPath(path);

    // mimic old behavior *always* include for OB3
    componentResource.addValidForApp(ComponentResource.APP_OB3);

    if (includeAlsoInClassicMode) {
      componentResource.addValidForApp(ComponentResource.APP_CLASSIC);
    }

    return componentResource;
  }

  protected ComponentResource createDynamicResource(String path) {
    return createComponentResource(ComponentResourceType.Dynamic, path, ComponentResource.APP_OB3);
  }

  /**
   * Implemented here for backward compatibility, calls the {@link #getGlobalResources()}
   */
  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> globalResources = new ArrayList<>();
    for (String globalResource : getGlobalResources()) {
      globalResources.add(createStaticResource(globalResource, true));
    }
    return globalResources;
  }

  // Implemented for backward compatibility
  @Deprecated
  @Override
  public List<String> getGlobalResources() {
    return Collections.emptyList();
  }

  public static class ComponentResource {

    public enum ComponentResourceType {
      Static, Dynamic, Stylesheet
    }

    public static final String APP_OB3 = "OB3";
    public static final String APP_CLASSIC = "CLASSIC";
    public static final List<String> ALL_CORE_APPS = Arrays
        .asList(new String[] { APP_OB3, APP_CLASSIC });

    private ComponentResourceType type;
    private String path;

    @Deprecated
    private boolean includeAlsoInClassicMode = false;

    @Deprecated
    private boolean includeInNewUIMode = true;

    private List<String> validForAppList = new ArrayList<>();

    public ComponentResourceType getType() {
      return type;
    }

    public void setType(ComponentResourceType componentResourceType) {
      this.type = componentResourceType;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    @Override
    public String toString() {
      return type + " " + path;
    }

    @Deprecated
    public boolean isIncludeAlsoInClassicMode() {
      return includeAlsoInClassicMode;
    }

    @Deprecated
    public void setIncludeAlsoInClassicMode(boolean includeAlsoInClassicMode) {
      this.includeAlsoInClassicMode = includeAlsoInClassicMode;
    }

    @Deprecated
    public boolean isIncludeInNewUIMode() {
      return includeInNewUIMode;
    }

    @Deprecated
    public void setIncludeInNewUIMode(boolean includeInNewUIMode) {
      this.includeInNewUIMode = includeInNewUIMode;
    }

    public List<String> getValidForAppList() {
      return validForAppList;
    }

    public void setValidForAppList(List<String> validForAppList) {

      this.validForAppList = validForAppList;
    }

    public void addValidForApp(String app) {
      this.validForAppList.add(app);
    }

    public boolean isValidForApp(String app) {
      boolean valid = this.validForAppList.contains(app);
      if (valid) {
        return true;
      }
      if (appDependencies.containsKey(app)) {
        for (String dep : appDependencies.get(app)) {
          valid = isValidForApp(dep);
          if (valid) {
            return true;
          }
        }
      }
      return false;
    }

  }
}
