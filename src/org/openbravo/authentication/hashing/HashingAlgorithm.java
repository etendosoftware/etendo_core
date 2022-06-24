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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/** Algorithm used to hash password to store in Database */
abstract class HashingAlgorithm {

  /**
   * Generates a hash using current algorithm, hashes should look like: {@code version$salt$hashed}
   * where:
   * <p>
   * <ul>
   * <li>{@code version}: is a numeric value that determines the version of the
   * {@code HashingAlgorithm} that performed the hash. It must match the value returned by
   * {@link #getAlgorithmVersion()}.
   * <li>{@code salt}: salt used in the hash.
   * <li>{@code hashed}: actual hash, which is computed by {@link #hash(String, String)}.
   * </ul>
   */
  protected abstract String generateHash(String password);

  /**
   * Each {@link HashingAlgorithm} must be versioned, passwords hashed in Database with older
   * algorithms can be automatically upgraded to newer ones.
   * 
   * @see PasswordHash#getUserWithPassword(String, String)
   */
  protected abstract int getAlgorithmVersion();

  /** Checks whether a plain text password matches with a hashed password */
  protected abstract boolean check(String plainTextPassword, String hashedPassword);

  /** Returns the low level algorithm used to perform the hashing. */
  protected abstract MessageDigest getHashingBaseAlgorithm();

  /**
   * Performs the low level hashing of {@code plainText} salted with {@code salt} value. Salt can be
   * null in which case will be ignored.
   */
  protected final String hash(String plainText, String salt) {
    MessageDigest md = getHashingBaseAlgorithm();
    if (salt != null) {
      md.update(salt.getBytes(StandardCharsets.UTF_8));
    }

    byte[] bytes = md.digest(plainText.getBytes(StandardCharsets.UTF_8));

    return Base64.getEncoder().encodeToString(bytes);
  }
}
