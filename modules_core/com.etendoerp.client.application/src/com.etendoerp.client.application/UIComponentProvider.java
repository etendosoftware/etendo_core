package com.etendoerp.client.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(UIComponentProvider.COMPONENT_NAME)
public class UIComponentProvider extends BaseComponentProvider {

  protected static final String COMPONENT_NAME = "IMA_UIComponentProvider";

  protected static final String[] JS_FILES = new String[]{
      "direct-tab-link.js",
      "sales-order-tab-link.js"
  };

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component ID " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final GlobalResourcesHelper grHelper = new GlobalResourcesHelper();

    for (String file : JS_FILES) {
      grHelper.addEtendoResource(file);
    }

    return grHelper.getGlobalResources();
  }

  private class GlobalResourcesHelper {
    private final List<ComponentResource> globalResources = new ArrayList<>();

    public void addEtendoResource(String file) {
      String prefix = "web/com.etendoerp.client.application/js/";
      globalResources.add(createStaticResource(prefix + file, false));
    }

    public List<ComponentResource> getGlobalResources() {
      return globalResources;
    }
  }
}
