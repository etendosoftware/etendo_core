package org.openbravo.advpaymentmngt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.kernel.BaseComponentProvider;

/**
 * Test class for the APRMApplicationProvider.
 * This class contains unit tests for the APRMApplicationProvider class.
 */
@RunWith(MockitoJUnitRunner.class)
public class APRMApplicationProviderTest {

  private static final String[] EXPECTED_RESOURCES = { "web/org.openbravo.advpaymentmngt/js/ob-aprm-utilities.js", "web/org.openbravo.advpaymentmngt/js/ob-aprm-addPayment.js", "web/org.openbravo.advpaymentmngt/js/ob-aprm-matchStatement.js", "web/org.openbravo.advpaymentmngt/js/ob-aprm-addTransaction.js", "web/org.openbravo.advpaymentmngt/js/ob-aprm-findTransaction.js", "web/org.openbravo.advpaymentmngt/js/ob-aprm-fundsTransfer.js" };
  private APRMApplicationProvider provider;
  private Map<String, Object> parameters;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    provider = new APRMApplicationProvider();
    parameters = new HashMap<>();
  }

  /**
   * Tests the getComponent method with a valid component ID.
   * Verifies that the method returns null for any component ID.
   */
  @Test
  public void testGetComponent() {
    assertNull("Should return null for any component id", provider.getComponent("anyComponentId", parameters));
  }

  /**
   * Tests the getComponent method with null parameters.
   * Verifies that the method returns null for null parameters.
   */
  @Test
  public void testGetComponentNullParameters() {
    assertNull("Should return null for null parameters", provider.getComponent("anyComponentId", null));
  }

  /**
   * Tests the getGlobalComponentResources method.
   * Verifies that the method returns the correct number of resources and that each resource path matches the expected value.
   */
  @Test
  public void testGetGlobalComponentResources() {
    List<BaseComponentProvider.ComponentResource> resources = provider.getGlobalComponentResources();

    // Verify number of resources
    assertEquals("Should return correct number of resources", EXPECTED_RESOURCES.length, resources.size());

    // Verify each resource
    for (int i = 0; i < EXPECTED_RESOURCES.length; i++) {
      BaseComponentProvider.ComponentResource resource = resources.get(i);
      assertNotNull("Resource should not be null", resource);
      assertEquals("Resource path should match", EXPECTED_RESOURCES[i], resource.getPath());
    }
  }

  /**
   * Tests the content validation of global component resources.
   * Verifies that each resource path starts with the correct prefix and ends with .js.
   */
  @Test
  public void testGlobalComponentResourcesContentValidation() {
    List<BaseComponentProvider.ComponentResource> resources = provider.getGlobalComponentResources();

    for (BaseComponentProvider.ComponentResource resource : resources) {
      // Verify path format
      assertTrue("Resource path should start with web/org.openbravo.advpaymentmngt/js/",
          resource.getPath().startsWith("web/org.openbravo.advpaymentmngt/js/"));
      assertTrue("Resource path should end with .js", resource.getPath().endsWith(".js"));

      // Verify resource name format
      String name = resource.getPath().substring(resource.getPath().lastIndexOf("/") + 1);
      assertTrue("Resource name should start with ob-aprm-", name.startsWith("ob-aprm-"));
    }
  }

  /**
   * Tests the order of global component resources.
   * Verifies that specific resources are in the correct order.
   */
  @Test
  public void testGlobalComponentResourcesOrder() {
    List<BaseComponentProvider.ComponentResource> resources = provider.getGlobalComponentResources();

    // Verify specific resources are in correct order
    assertEquals("utilities.js should be first", "web/org.openbravo.advpaymentmngt/js/ob-aprm-utilities.js",
        resources.get(0).getPath());

    assertEquals("fundsTransfer.js should be last", "web/org.openbravo.advpaymentmngt/js/ob-aprm-fundsTransfer.js",
        resources.get(resources.size() - 1).getPath());
  }

}
