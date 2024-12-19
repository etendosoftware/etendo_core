package com.etendoerp.reportvaluationstock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.openbravo.client.kernel.BaseComponentProvider;

import java.util.List;
import java.util.HashMap;

/**
 * Unit tests for the {@link ETRVSComponentProvider} class.
 * <p>
 * This class tests the behavior of the component provider methods, ensuring
 * correct handling of global component resources and specific components.
 * </p>
 */
public class ETRVSComponentProviderTest {

  private ETRVSComponentProvider componentProvider;

  /**
   * Sets up the test environment by initializing the component provider instance.
   */
  @Before
  public void setUp() {
    componentProvider = new ETRVSComponentProvider();
  }

  /**
   * Tests the {@link ETRVSComponentProvider(String, HashMap)} method.
   * <p>
   * Ensures that the method returns null when provided with an arbitrary ID and empty parameters.
   * </p>
   */
  @Test
  public void testGetComponent() {
    assertNull(componentProvider.getComponent("testId", new HashMap<>()));
  }

  /**
   * Tests the {@link ETRVSComponentProvider#getGlobalComponentResources()} method.
   * <p>
   * Verifies that the method returns a non-null list containing a single valid resource.
   * </p>
   */
  @Test
  public void testGetGlobalComponentResources() {
    List<BaseComponentProvider.ComponentResource> resources = componentProvider.getGlobalComponentResources();

    assertNotNull(resources);

    assertEquals(1, resources.size());

    BaseComponentProvider.ComponentResource resource = resources.get(0);
    assertNotNull(resource);

    assertEquals(
        "web/com.etendoerp.reportvaluationstock/js/etrvs-onchange.js",
        resource.getPath()
    );

  }

  /**
   * Tests that {@link ETRVSComponentProvider#getGlobalComponentResources()} returns a non-empty list.
   * <p>
   * Ensures the presence of global component resources.
   * </p>
   */
  @Test
  public void testGetGlobalComponentResourcesNotEmpty() {
    assertFalse(componentProvider.getGlobalComponentResources().isEmpty());
  }
}