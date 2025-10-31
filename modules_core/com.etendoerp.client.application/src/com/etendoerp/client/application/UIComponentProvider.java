package com.etendoerp.client.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

import jakarta.enterprise.context.Dependent;

/**
 * Component provider for UI-related resources in Etendo ERP.
 *
 * <p>Registers JavaScript files required for navigation and UI functionality.</p>
 */
@Dependent
@ComponentProvider.Qualifier(UIComponentProvider.COMPONENT_NAME)
public class UIComponentProvider extends BaseComponentProvider {

  protected static final String COMPONENT_NAME = "IMA_UIComponentProvider";

  /** JavaScript files required for UI navigation. */
  protected static final String[] JS_FILES = new String[]{
      "direct-tab-link.js",
      "sales-order-tab-link.js"
  };

  /**
   * Retrieves a specific component by its ID.
   *
   * <p>Currently, this method does not support retrieving components and will always throw an
   * {@link IllegalArgumentException}.</p>
   *
   * @param componentId The ID of the requested component.
   * @param parameters A map containing parameters for the component.
   * @return Never returns a component, as this provider does not support dynamic components.
   * @throws IllegalArgumentException Always thrown since component retrieval is not implemented.
   */
  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component ID " + componentId + " not supported.");
  }

  /**
   * Retrieves the list of global component resources for the UI module.
   *
   * <p>This method registers JavaScript files required for navigation and user interaction.</p>
   *
   * @return A list of {@link ComponentResource} objects representing global UI resources.
   */
  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final GlobalResourcesHelper grHelper = new GlobalResourcesHelper();

    for (String file : JS_FILES) {
      grHelper.addEtendoResource(file);
    }

    return grHelper.getGlobalResources();
  }

  /**
   * Helper class to manage global UI resources.
   *
   * <p>This class is responsible for registering JavaScript files as static resources
   * for use in the Etendo UI.</p>
   */
  private class GlobalResourcesHelper {
    private final List<ComponentResource> globalResources = new ArrayList<>();

    /**
     * Adds a JavaScript resource to the list of global resources.
     *
     * @param file The name of the JavaScript file to be added.
     */
    public void addEtendoResource(String file) {
      String prefix = "web/com.etendoerp.client.application/js/";
      globalResources.add(createStaticResource(prefix + file, false));
    }

    /**
     * Retrieves the list of registered global resources.
     *
     * @return A list of {@link ComponentResource} objects representing UI resources.
     */
    public List<ComponentResource> getGlobalResources() {
      return globalResources;
    }
  }
}
