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
 * All portions are Copyright (C) 2015-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.weld.test;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * Rule to make possible to run parameterized test cases with Arquillian runner. The Rule field
 * defines the parameter values which will be taken by the field annotated with @ParameterCdiTest.
 * 
 * Based on https://gist.github.com/poolik/8764414
 * 
 * @see ParameterCdiTest
 * 
 * @author alostale
 */
public class ParameterCdiTestRule<T> implements MethodRule {
  private final List<T> params;
  private static final Logger log = LogManager.getLogger();

  public ParameterCdiTestRule(List<T> params) {
    if (params == null || params.size() == 0) {
      throw new IllegalArgumentException(
          "'params' must be specified and have more than zero length!");
    }
    this.params = params;
  }

  @Override
  public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        evaluateParamsToTarget(base, target, method);
      }
    };
  }

  private void evaluateParamsToTarget(Statement base, Object target, FrameworkMethod method)
      throws Throwable {
    Field targetField = getTargetField(target);

    boolean isAccesible = targetField.canAccess(target);
    if (!isAccesible) {
      targetField.setAccessible(true);
    }

    for (Object param : params) {
      targetField.set(target, param);

      log.info(
          "============================================================================================================");
      log.info("   Paremeterized test {}.{} ", target.getClass().getName(), method.getName());
      log.info("       {}: {}", targetField.getName(), param);
      log.info(
          "============================================================================================================");

      base.evaluate();
    }
  }

  private Field getTargetField(Object target) throws NoSuchFieldException {
    Field[] allFields = target.getClass().getDeclaredFields();
    Field paramField = null;
    for (Field field : allFields) {
      if (field.getAnnotation(ParameterCdiTest.class) != null) {
        if (paramField != null) {
          throw new IllegalStateException(
              "More than one field with @ParameterCdiTest. There should be a single @ParameterCdiTest field.");
        }

        paramField = field;
      }
    }
    if (paramField == null) {
      throw new IllegalStateException(
          "No field with @ParameterCdiTest annotation found. There should be a single @ParameterCdiTest field.");
    }
    return paramField;
  }
}
