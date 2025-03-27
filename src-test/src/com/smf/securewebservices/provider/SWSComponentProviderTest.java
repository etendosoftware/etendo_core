package com.smf.securewebservices.provider;

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
 * Test class for {@link SWSComponentProvider}
 */
@RunWith(MockitoJUnitRunner.class)
public class SWSComponentProviderTest {

  @InjectMocks
  private SWSComponentProvider componentProvider;

  /**
   * Sets up the test environment before each test.
   * Initializes the `SWSComponentProvider` instance.
   */
  @Before
  public void setUp() {
    componentProvider = new SWSComponentProvider();
  }

  /**
   * Test for {@link SWSComponentProvider#getComponent(String, Map)}
   * Expected behavior: should throw IllegalArgumentException for any component id
   */
  @Test(expected = IllegalArgumentException.class)
  public void testGetComponentThrowsIllegalArgumentException() {
    // GIVEN
    String componentId = "anyComponentId";
    Map<String, Object> parameters = new HashMap<>();

    // WHEN & THEN
    // Should throw IllegalArgumentException
    componentProvider.getComponent(componentId, parameters);
  }

  /**
   * Test for {@link SWSComponentProvider#getComponent(String, Map)}
   * Expected behavior: should throw IllegalArgumentException with expected message
   */
  @Test
  public void testGetComponentContainsCorrectExceptionMessage() {
    // GIVEN
    String componentId = "testId";
    Map<String, Object> parameters = new HashMap<>();

    // WHEN
    try {
      componentProvider.getComponent(componentId, parameters);
      fail("Expected IllegalArgumentException was not thrown");
    } catch (IllegalArgumentException e) {
      // THEN
      assertEquals("Component id testId not supported.", e.getMessage());
    }
  }

  /**
   * Test for {@link SWSComponentProvider#getGlobalComponentResources()}
   * Expected behavior: should return a list with one resource for the JS file
   */
  @Test
  public void testGetGlobalComponentResourcesReturnsCorrectResources() {
    // WHEN
    List<BaseComponentProvider.ComponentResource> resources = componentProvider.getGlobalComponentResources();

    // THEN
    // Should return one resource
    assertNotNull("Resources list should not be null", resources);
    assertEquals("Should return exactly one resource", 1, resources.size());

    // The resource should be for the JS file
    BaseComponentProvider.ComponentResource resource = resources.get(0);
    assertEquals("Resource path should match", "web/com.smf.securewebservices/js/generateprivatekey.js",
        resource.getPath());
  }

  /**
   * Test for the component type constant
   */
  @Test
  public void testComponentTypeConstant() {
    assertEquals("SMFSWS_CompProv", SWSComponentProvider.COMPONENT_TYPE);
  }
}
