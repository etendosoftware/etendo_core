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
package org.openbravo.test.security;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.service.password.PasswordStrengthChecker;

/**
 * Test PasswordStrengthChecker password policy with various sample passwords
 *
 * @author jarmendariz
 */
public class PasswordStrengthCheckerTest extends WeldBaseTest {

  @Inject
  private PasswordStrengthChecker checker;

  @Test
  public void shortPasswordsShouldBeRejected() {
    shouldReject("short");
  }

  @Test
  public void lowercasePasswordsShouldBeRejected() {
    shouldReject("longbutnotsecure");
  }

  @Test
  public void uppercaseAndLowercasePasswordsShouldBeRejected() {
    shouldReject("LongButNotSecureEnough");
  }

  @Test
  public void uppercaseLowercaseAndDigitsShouldBeAccepted() {
    shouldAccept("L0ngAndS3cureEn0ugh");
  }

  @Test
  public void uppercaseLowercaseAndSpecialCharactersShouldBeAccepted() {
    shouldAccept("LongWithSpeci/-\\\\lCharacters;");
  }

  @Test
  public void uppercaseDigitsAndSpecialCharactersShouldBeAccepted() {
    shouldAccept("UPPERWITHD1G1TSANDSP€CIALCHARS");
  }

  private void shouldReject(String password) {
    assertThat(checker.isStrongPassword(password), is(false));
  }

  private void shouldAccept(String password) {
    assertThat(checker.isStrongPassword(password), is(true));
  }

}
