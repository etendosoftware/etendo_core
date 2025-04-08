package com.etendoerp.advpaymentmngt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.kernel.BaseComponentProvider;

/**
 * Unit tests for the {@link EtendoComponentProvider} class.
 */
@RunWith(MockitoJUnitRunner.class)
public class EtendoComponentProviderTest {

  @InjectMocks
  private EtendoComponentProvider provider;

  private Map<String, Object> parameters;

  /**
   * Sets up the test environment by initializing the parameters map.
   */
  @Before
  public void setUp() {
    parameters = new HashMap<>();
  }

  /**
   * Tests that the {@code getGlobalComponentResources} method returns a list
   * containing exactly two expected resources.
   */
  @Test
  public void testGetGlobalComponentResources() {
    List<BaseComponentProvider.ComponentResource> resources = provider.getGlobalComponentResources();

    assertNotNull("Resources should not be null", resources);
    assertEquals("Should return 2 resources", 2, resources.size());

    BaseComponentProvider.ComponentResource firstResource = resources.get(0);
    BaseComponentProvider.ComponentResource secondResource = resources.get(1);

    assertEquals("web/com.etendoerp.advpaymentmngt/js/received_in-paid_out-onchange.js", firstResource.getPath());
    assertEquals("web/com.etendoerp.advpaymentmngt/js/payment-action-popup.js", secondResource.getPath());

  }

  /**
   * Tests that the {@code getComponent} method throws an IllegalArgumentException
   * when an invalid component ID is provided.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testGetComponentThrowsException() {
    provider.getComponent("invalidId", parameters);
  }

  /**
   * Tests that the {@code getComponent} method throws an exception with the correct
   * error message when an unsupported component ID is requested.
   */
  @Test
  public void testGetComponentExceptionMessage() {
    String componentId = "testComponent";
    try {
      provider.getComponent(componentId, parameters);
      fail("Should throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals("Component id " + componentId + " not supported.", e.getMessage());
    }
  }
}
