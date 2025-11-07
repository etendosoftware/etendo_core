package org.openbravo.base.weld.test;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that initializes Weld components after CDI injection.
 *
 * With Arquillian + JUnit 5, CDI injection happens after all @BeforeEach methods
 * but before the test method. This callback runs at that exact moment.
 *
 * Execution order:
 * 1. @BeforeEach methods
 * 2. Arquillian injects CDI beans
 * 3. BeforeTestExecutionCallback (THIS CALLBACK - initializes Weld)
 * 4. @Test method
 */
public class WeldInitializerExtension implements BeforeTestExecutionCallback {

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    Object testInstance = context.getRequiredTestInstance();

    // Only initialize if the test extends WeldBaseTest
    if (testInstance instanceof WeldBaseTest) {
      WeldBaseTest weldTest = (WeldBaseTest) testInstance;
      weldTest.initializeWeldComponents();
    }
  }
}
