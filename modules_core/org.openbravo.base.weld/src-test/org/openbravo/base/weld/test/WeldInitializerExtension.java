/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
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