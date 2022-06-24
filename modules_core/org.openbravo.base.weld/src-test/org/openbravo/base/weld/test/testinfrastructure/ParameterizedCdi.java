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
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.weld.test.testinfrastructure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;

/**
 * Test cases checking test case parameterization with cdi.
 * 
 * @author alostale
 *
 */
public class ParameterizedCdi extends WeldBaseTest {
  public static final List<String> PARAMS = Arrays.asList("param1", "param2", "param3");

  /** defines the values the parameter will take. */
  @Rule
  public ParameterCdiTestRule<String> parameterValuesRule = new ParameterCdiTestRule<String>(
      PARAMS);

  /** this field will take the values defined by parameterValuesRule field. */
  private @ParameterCdiTest String parameter;

  private static int counterTest1 = 0;
  private static int counterTest2 = 0;
  private static String test1Execution = "";
  private static String test2Execution = "";

  /** Test case to be executed once per parameter value */
  @Test
  public void test1() {
    assertThat("parameter value", parameter, equalTo(PARAMS.get(counterTest1)));
    counterTest1++;
    test1Execution += parameter;
  }

  /** Test case to be executed once per parameter value */
  @Test
  public void test2() {
    assertThat("parameter value", parameter, equalTo(PARAMS.get(counterTest2)));

    counterTest2++;
    test2Execution += parameter;
  }

  /** Checks the previous test cases were executed as many times as parameter values in the list. */
  @AfterClass
  public static void testsShouldBeExecutedOncePerParameter() {
    String expectedValue = "";
    for (String paramValue : PARAMS) {
      expectedValue += paramValue;
    }
    assertThat("# of executions for test 1", PARAMS.size(), is(counterTest1));
    assertThat("# of executions for test 2", PARAMS.size(), is(counterTest2));

    assertThat("test 1 result", test1Execution, equalTo(expectedValue));
    assertThat("test 2 result", test2Execution, equalTo(expectedValue));
  }
}
