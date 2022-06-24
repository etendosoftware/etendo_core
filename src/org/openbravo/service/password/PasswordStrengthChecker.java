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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.password;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

/**
 * Utility class used to check that passwords meets a minimum strength policy.
 *
 * Strong passwords should be at least 8 characters long and contain at least 3 out of the
 * following: Uppercase letters, lowercase letters, digits or special characters.
 * 
 * @author jarmendariz
 */
@ApplicationScoped
public class PasswordStrengthChecker {
  private static final int MINIMUM_LENGTH = 8;
  private static final int MIN_REQUIRED_CRITERIA = 3;

  private List<PasswordStrengthCriterion> strengthCriteria;

  @PostConstruct
  private void init() {
    strengthCriteria = new ArrayList<>(4);
    strengthCriteria.add(getUppercaseCriterion());
    strengthCriteria.add(getLowercaseCriterion());
    strengthCriteria.add(getDigitsCriterion());
    strengthCriteria.add(getSpecialCharactersCriterion());
  }

  /**
   * Verifies that the given password meets the minimum strength criteria
   *
   * @param password
   *          The password to evaluate
   * @return true if the password is strong enough, false otherwise
   */
  public boolean isStrongPassword(String password) {
    return hasMinimumLength(password) && (getCriteriaScore(password) >= MIN_REQUIRED_CRITERIA);
  }

  private int getCriteriaScore(String password) {
    int score = 0;

    for (PasswordStrengthCriterion criterion : strengthCriteria) {
      if (criterion.match(password)) {
        score += 1;
      }
    }

    return score;
  }

  private boolean hasMinimumLength(String password) {
    return password.length() >= MINIMUM_LENGTH;
  }

  private PasswordStrengthCriterion getUppercaseCriterion() {
    return new PasswordStrengthCriterion() {
      @Override
      public boolean match(String password) {
        return password.matches(".*[A-Z].*");
      }
    };
  }

  private PasswordStrengthCriterion getLowercaseCriterion() {
    return new PasswordStrengthCriterion() {
      @Override
      public boolean match(String password) {
        return password.matches(".*[a-z].*");
      }
    };
  }

  private PasswordStrengthCriterion getDigitsCriterion() {
    return new PasswordStrengthCriterion() {
      @Override
      public boolean match(String password) {
        return password.matches(".*[0-9].*");
      }
    };
  }

  private PasswordStrengthCriterion getSpecialCharactersCriterion() {
    return new PasswordStrengthCriterion() {
      @Override
      public boolean match(String password) {
        return password.matches(".*[`~!@#$%€^&*()_\\-+={}\\[\\]|:;\"' <>,.?/].*");
      }
    };
  }

  private interface PasswordStrengthCriterion {
    boolean match(String password);
  }
}
