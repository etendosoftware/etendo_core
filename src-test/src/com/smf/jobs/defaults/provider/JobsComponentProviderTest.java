package com.smf.jobs.defaults.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.openbravo.client.kernel.ComponentProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import javax.enterprise.context.ApplicationScoped;

/**
 * Test suite for the JobsComponentProvider class, responsible for testing its functionality and annotations.
 * This class verifies the following:
 * <ul>
 *   <li>Presence and values of specific annotations.</li>
 *   <li>Constant definitions.</li>
 *   <li>Behavior of methods with and without expected exceptions.</li>
 *   <li>Proper structure and content of global component resources.</li>
 * </ul>
 */
public class JobsComponentProviderTest {

  private JobsComponentProvider jobsComponentProvider;

  /**
   * Sets up the testing environment by initializing mocks and the instance of the class under test.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    jobsComponentProvider = new JobsComponentProvider();
  }

  /**
   * Tests the presence and value of the {@link ComponentProvider.Qualifier} annotation in the class under test.
   */
  @Test
  public void testComponentProviderQualifier() {
    ComponentProvider.Qualifier qualifier = jobsComponentProvider.getClass().getAnnotation(ComponentProvider.Qualifier.class);
    assertNotNull("ComponentProvider.Qualifier annotation should be present", qualifier);
    assertEquals("Qualifier value should match COMPONENT_TYPE",
        JobsComponentProvider.COMPONENT_TYPE,
        qualifier.value());
  }

  /**
   * Tests the value of the COMPONENT_TYPE constant.
   */
  @Test
  public void testConstantComponentType() {
    assertEquals("COMPONENT_TYPE should be 'JOBSPR_CompProv'",
        "JOBSPR_CompProv",
        JobsComponentProvider.COMPONENT_TYPE);
  }

  /**
   * Verifies that calling getComponent with specific parameters throws an {@link IllegalArgumentException}.
   *
   * @throws IllegalArgumentException when an invalid component ID is passed.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testGetComponentThrowsException() {
    Map<String, Object> parameters = new HashMap<>();
    jobsComponentProvider.getComponent("testComponentId", parameters);
  }

  /**
   * Tests the contents and properties of the global component resources list.
   * Ensures it is non-null, non-empty, contains specific elements, and has the expected size.
   */
  @Test
  public void testGetGlobalComponentResources() {
    List<? extends Object> globalResources =
        jobsComponentProvider.getGlobalComponentResources();

    assertNotNull("Global resources list should not be null", globalResources);
    assertFalse("Global resources list should not be empty", globalResources.isEmpty());

    assertEquals("Should have exactly 3 global resources", 3, globalResources.size());

    boolean hasProcessRecordsJs = false;
    boolean hasCloneRecordJs = false;
    boolean hasCreateFromOrdersJs = false;

    for (Object resource : globalResources) {
      String resourceString = resource.toString();
      if (resourceString.contains("processRecords.js")) {
        hasProcessRecordsJs = true;
      }
      if (resourceString.contains("ob-clone-record.js")) {
        hasCloneRecordJs = true;
      }
      if (resourceString.contains("createFromOrders.js")) {
        hasCreateFromOrdersJs = true;
      }
    }

    assertTrue("Should contain processRecords.js", hasProcessRecordsJs);
    assertTrue("Should contain ob-clone-record.js", hasCloneRecordJs);
    assertTrue("Should contain createFromOrders.js", hasCreateFromOrdersJs);
  }

  /**
   * Verifies the presence of the {@link ApplicationScoped} annotation on the class under test.
   */
  @Test
  public void testApplicationScopedAnnotation() {
    ApplicationScoped applicationScoped =
        jobsComponentProvider.getClass().getAnnotation(ApplicationScoped.class);
    assertNotNull("Class should be annotated with @ApplicationScoped", applicationScoped);
  }
}