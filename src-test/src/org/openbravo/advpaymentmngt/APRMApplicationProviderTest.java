package org.openbravo.advpaymentmngt;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.kernel.BaseComponentProvider;

@RunWith(MockitoJUnitRunner.class)
public class APRMApplicationProviderTest {

  private APRMApplicationProvider provider;
  private Map<String, Object> parameters;

  private static final String[] EXPECTED_RESOURCES = {
      "web/org.openbravo.advpaymentmngt/js/ob-aprm-utilities.js",
      "web/org.openbravo.advpaymentmngt/js/ob-aprm-addPayment.js",
      "web/org.openbravo.advpaymentmngt/js/ob-aprm-matchStatement.js",
      "web/org.openbravo.advpaymentmngt/js/ob-aprm-addTransaction.js",
      "web/org.openbravo.advpaymentmngt/js/ob-aprm-findTransaction.js",
      "web/org.openbravo.advpaymentmngt/js/ob-aprm-fundsTransfer.js"
  };

  @Before
  public void setUp() {
    provider = new APRMApplicationProvider();
    parameters = new HashMap<>();
  }

  @Test
  public void testGetComponent() {
    assertNull("Should return null for any component id",
        provider.getComponent("anyComponentId", parameters));
  }

  @Test
  public void testGetComponent_NullParameters() {
    assertNull("Should return null for null parameters",
        provider.getComponent("anyComponentId", null));
  }

  @Test
  public void testGetGlobalComponentResources() {
    List<BaseComponentProvider.ComponentResource> resources = provider.getGlobalComponentResources();

    // Verify number of resources
    assertEquals("Should return correct number of resources",
        EXPECTED_RESOURCES.length, resources.size());

    // Verify each resource
    for (int i = 0; i < EXPECTED_RESOURCES.length; i++) {
      BaseComponentProvider.ComponentResource resource = resources.get(i);
      assertNotNull("Resource should not be null", resource);
      assertEquals("Resource path should match",
          EXPECTED_RESOURCES[i], resource.getPath());
    }
  }

  @Test
  public void testGlobalComponentResources_ContentValidation() {
    List<BaseComponentProvider.ComponentResource> resources = provider.getGlobalComponentResources();

    for (BaseComponentProvider.ComponentResource resource : resources) {
      // Verify path format
      assertTrue("Resource path should start with web/org.openbravo.advpaymentmngt/js/",
          resource.getPath().startsWith("web/org.openbravo.advpaymentmngt/js/"));
      assertTrue("Resource path should end with .js",
          resource.getPath().endsWith(".js"));

      // Verify resource name format
      String name = resource.getPath().substring(resource.getPath().lastIndexOf("/") + 1);
      assertTrue("Resource name should start with ob-aprm-",
          name.startsWith("ob-aprm-"));
    }
  }

  @Test
  public void testGlobalComponentResources_Order() {
    List<BaseComponentProvider.ComponentResource> resources = provider.getGlobalComponentResources();

    // Verify specific resources are in correct order
    assertEquals("utilities.js should be first",
        "web/org.openbravo.advpaymentmngt/js/ob-aprm-utilities.js",
        resources.get(0).getPath());

    assertEquals("fundsTransfer.js should be last",
        "web/org.openbravo.advpaymentmngt/js/ob-aprm-fundsTransfer.js",
        resources.get(resources.size() - 1).getPath());
  }

}
