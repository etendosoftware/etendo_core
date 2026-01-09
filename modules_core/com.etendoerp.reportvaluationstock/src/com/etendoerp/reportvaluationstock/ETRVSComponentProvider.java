package com.etendoerp.reportvaluationstock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ETRVSComponentProvider extends BaseComponentProvider {

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    return null;
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    globalResources.add(
        createStaticResource("web/com.etendoerp.reportvaluationstock/js/etrvs-onchange.js", true));
    return globalResources;
  }
}
