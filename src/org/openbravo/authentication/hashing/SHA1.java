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

package org.openbravo.authentication.hashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Passwords are hashed with SHA-1 algorithm represented as a {@code String} encoded in base 64.
 * <p>
 * Algorithm used before 3.0PR20Q1.
 */
class SHA1 extends HashingAlgorithm {
  @Override
  protected MessageDigest getHashingBaseAlgorithm() {
    try {
      return MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException wontHappen) {
      throw new IllegalStateException(wontHappen);
    }
  }

  @Override
  protected boolean check(String plainTextPassword, String hashedPassword) {
    return hash(plainTextPassword, null).equals(hashedPassword);
  }

  @Override
  protected int getAlgorithmVersion() {
    return 0;
  }

  @Override
  protected String generateHash(String password) {
    return hash(password, null);
  }

}
