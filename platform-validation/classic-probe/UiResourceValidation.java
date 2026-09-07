package com.etendoerp.platform.validation;

import java.util.ArrayList;
import java.util.List;
import org.openbravo.client.application.ApplicationComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource;

/** Verifies the canonical resource extension without database or servlet startup. */
public class UiResourceValidation {
  private static class LegacyResources extends ApplicationComponentProvider {
    List<ComponentResource> resources() {
      List<ComponentResource> resources = new ArrayList<>();
      addApplicationSpecificResources(resources);
      return resources;
    }
  }

  private static class AlternativeResources extends LegacyResources {
    @Override
    protected void addApplicationSpecificResources(List<ComponentResource> resources) {
      resources.add(createStaticResource("web/application/request.js", false));
    }
  }

  public static void main(String[] args) {
    String origin = ApplicationComponentProvider.class.getProtectionDomain()
        .getCodeSource().getLocation().toString();
    require(origin.endsWith("/platform-ui-components.jar"), "Provider must load from shared artifact");
    List<String> expected = List.of("periodControlStatus", "productCharacteristicsProcess",
        "recalculatePermissionsProcess", "validateCostingRuleProcess", "checkAvailableCredit",
        "productServices", "cancelAndReplace");
    List<ComponentResource> actual = new LegacyResources().resources();
    require(actual.size() == expected.size(), "Legacy resource count changed");
    for (int i = 0; i < expected.size(); i++) {
      ComponentResource resource = actual.get(i);
      require(resource.getPath().equals("web/js/" + expected.get(i) + ".js"), "Resource order changed");
      require(resource.getType() == ComponentResource.ComponentResourceType.Static, "Resource type changed");
      require(resource.isValidForApp(ComponentResource.APP_OB3), "Original UI flag missing");
      require(resource.isValidForApp(ComponentResource.APP_CLASSIC) == (i < 6), "Classic flag changed");
    }
    List<ComponentResource> alternative = new AlternativeResources().resources();
    require(alternative.size() == 1 && alternative.get(0).getPath().equals("web/application/request.js"),
        "Alternative contribution must not inherit ERP scripts");
    System.out.println("PASS: Shared UI artifact origin, seven legacy resources and alternative contribution");
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
}
