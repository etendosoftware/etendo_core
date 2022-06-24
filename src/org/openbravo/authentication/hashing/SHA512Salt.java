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
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

/**
 * Passwords are hashed using SHA-512 algorithm with a random salt of 16 bytes represented as a
 * {@code String} encoded in base 64.
 * <p>
 * The full hash looks like {@code 1$salt$hashedPassword}, where {@code 1} is this algorithm's
 * version.
 */
class SHA512Salt extends HashingAlgorithm {
  private static final Random RANDOM = new SecureRandom();

  @Override
  protected MessageDigest getHashingBaseAlgorithm() {
    try {
      return MessageDigest.getInstance("SHA-512");
    } catch (NoSuchAlgorithmException wontHappen) {
      throw new IllegalStateException(wontHappen);
    }
  }

  @Override
  protected boolean check(String plainTextPassword, String hashedPassword) {
    String[] hashParts = hashedPassword.split("\\$");
    String salt = hashParts[1];
    String orginalHash = hashParts[2];

    return hash(plainTextPassword, salt).equals(orginalHash);
  }

  @Override
  protected int getAlgorithmVersion() {
    return 1;
  }

  @Override
  protected String generateHash(String password) {
    byte[] rawSalt = new byte[16];
    RANDOM.nextBytes(rawSalt);
    String salt = Base64.getEncoder().withoutPadding().encodeToString(rawSalt);
    String hash = hash(password, salt);
    return getAlgorithmVersion() + "$" + salt + "$" + hash;
  }
}
