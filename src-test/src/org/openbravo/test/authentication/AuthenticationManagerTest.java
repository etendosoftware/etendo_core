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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.authentication;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.openbravo.test.base.TestConstants.Users.ADMIN;

import org.junit.Test;
import org.openbravo.authentication.basic.DefaultAuthenticationManager;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test cases to cover the authentication manager mechanism.
 */
public class AuthenticationManagerTest extends OBBaseTest {

  private static final String USER_NAME = "admin";
  private static final String PASSWORD = "admin";

  /**
   * Test the authentication intended for non standard REST web services (such as SOAP).
   */
  @Test
  public void webServiceAuthenticate() {
    DefaultAuthenticationManager authManager = new DefaultAuthenticationManager();
    String userId = authManager.webServiceAuthenticate(USER_NAME, PASSWORD);
    assertThat(userId, equalTo(ADMIN));
  }

}
