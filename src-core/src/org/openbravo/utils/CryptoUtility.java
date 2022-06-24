/*
 ************************************************************************************
 * Copyright (C) 2001-2017 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */

package org.openbravo.utils;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;

/** Basic utilities to encrypt/decrypt Strings. */
public class CryptoUtility {
  private static final SecretKey KEY = new SecretKeySpec(
      new byte[] { 100, 25, 28, -122, -26, 94, -3, -72 }, "DES");
  private static final String TRANSFORMATION = "DES/ECB/PKCS5Padding";

  /**
   * Encrypts a String
   * 
   * @param value
   *          Plain text String to be encrypted.
   * @return Encrypted {@code value}.
   */
  public static String encrypt(String value) throws ServletException {
    String clearText = value == null ? "" : value;

    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, KEY);
      byte[] encString = cipher.doFinal(clearText.getBytes());
      return new String(encodeBase64(encString), "UTF-8");
    } catch (Exception ex) {
      throw new ServletException("CryptoUtility.encrypt() - Can't init cipher", ex);
    }
  }

  /**
   * Decrypts a String
   * 
   * @param value
   *          Encrypted String
   * @return Decrypted {@code value}.
   */
  public static String decrypt(String value) throws ServletException {
    if (value == null || value.length() == 0) {
      return value;
    }

    try {
      byte[] decode = decodeBase64(value.getBytes("UTF-8"));
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, KEY, cipher.getParameters());
      return new String(cipher.doFinal(decode));
    } catch (Exception ex) {
      throw new ServletException("CryptoUtility.decrypt() - Can't init cipher", ex);
    }
  }
}
