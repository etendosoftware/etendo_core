/*
 ************************************************************************************
 * Copyright (C) 2001-2019 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FormatUtilities {
  static Logger log4j = LogManager.getLogger();

  public static String truncate(String _s, int i) {
    String s = _s;
    if (s == null || s.length() == 0) {
      return "";
    }
    if (i < s.length()) {
      s = s.substring(0, i) + "...";
    }
    return s;
  }

  public static String replaceTildes(String strIni) {
    // Delete tilde characters
    return strIni.replace('á', 'a')
        .replace('é', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('ú', 'u')
        .replace('Á', 'A')
        .replace('É', 'E')
        .replace('Í', 'I')
        .replace('Ó', 'O')
        .replace('Ú', 'U');
  }

  private static final char[] delChars = { '-', '/', '#', ' ', '&', ',', '(', ')' };

  public static String replace(String strIni) {
    // delete characters: " ","&",","
    String result = replaceTildes(Replace.delChars(strIni, delChars));
    return result;
  }

  public static String replaceJS(String strIni) {
    return replaceJS(strIni, true);
  }

  public static String replaceJS(String strIni, boolean isUnderQuotes) {
    if (strIni == null) {
      // nothing to return if the provided string is null
      return null;
    }
    return Replace.replace(Replace.replace(Replace
        .replace(Replace.replace(strIni, "'", (isUnderQuotes ? "\\'" : "&#039;")), "\"", "\\\""),
        "\n", "\\n"), "\r", "").replace("<", "\\<").replace(">", "\\>");
  }

  /**
   * Hashes text using SHA-1 algorithm. This method is deprecated in favor of PasswordHash which
   * supports more modern algorithms.
   *
   * @deprecated Use PasswordHash instead (since = "3.0PR20Q1", forRemoval = true)
   */
  @Deprecated
  public static String sha1Base64(String text) throws ServletException {
    if (text == null || text.trim().equals("")) {
      return "";
    }

    MessageDigest md = null;

    try {
      md = MessageDigest.getInstance("SHA"); // SHA-1 generator instance
    } catch (NoSuchAlgorithmException e) {
      throw new ServletException(e.getMessage());
    }

    md.update(text.getBytes(StandardCharsets.UTF_8));

    byte[] raw = md.digest(); // Message summary reception
    return Base64.getEncoder().encodeToString(raw);
  }

  public static String encryptDecrypt(String text, boolean encrypt) throws ServletException {
    if (text == null || text.trim().equals("")) {
      return "";
    }
    String result = text;
    if (encrypt) {
      result = CryptoUtility.encrypt(text);
    } else {
      result = CryptoUtility.decrypt(text);
    }
    return result;
  }

  public static String sanitizeInput(String text) {
    String sanitized = text;
    String[] tags = { "<[/]?applet>", "<[/]?body>", "<[/]?embed>", "<[/]?frame>", "<[/]?script>",
        "<[/]?frameset>", "<[/]?html>", "<[/]?iframe>", "<[/]?img>", "<[/]?style>", "<[/]?layer>",
        "<[/]?link>", "<[/]?ilayer>", "<[/]?meta>", "<[/]?object>", "\\r" };
    for (int i = 0; i < tags.length; i++) {
      sanitized = sanitized.replaceAll("(?i)" + tags[i], "");
    }
    return sanitized;
  }

  public static String[] sanitizeInput(String[] text) {
    for (int i = 0; i < text.length; i++) {
      text[i] = sanitizeInput(text[i]);
    }
    return text;
  }
}
