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
 * All portions are Copyright (C) 2006-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;

import javax.net.ssl.HttpsURLConnection;
import jakarta.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.SystemInformation;
import org.openbravo.utils.FormatUtilities;

public class HttpsUtils {

  private static Logger log4j = LogManager.getLogger();

  static String sendSecure(HttpsURLConnection conn, String data) throws IOException {
    String result = null;
    BufferedReader br = null;
    try {
      String s = null;
      StringBuilder sb = new StringBuilder();
      br = new BufferedReader(
          new InputStreamReader(sendSecureHttpsConnection(conn, data).getInputStream()));
      while ((s = br.readLine()) != null) {
        sb.append(s + "\n");
      }
      br.close();
      result = sb.toString();
    } catch (IOException e) {
      log4j.error(e.getMessage(), e);
      throw e;
    }
    return result;
  }

  private static HttpsURLConnection sendSecureHttpsConnection(HttpsURLConnection conn, String data)
      throws IOException {
    BufferedWriter bw = null;
    try {
      conn.setDoOutput(true);

      bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
      bw.write(data);
      bw.flush();
      bw.close();

      return conn;
    } catch (IOException e) {
      log4j.error(e.getMessage(), e);
      throw e;
    }
  }

  public static String sendSecure(URL url, String data)
      throws GeneralSecurityException, IOException {
    HttpsURLConnection conn = getHttpsConn(url);
    return sendSecure(conn, data);
  }

  public static HttpURLConnection sendHttpsRequest(URL url, String data)
      throws GeneralSecurityException, IOException {

    HttpsURLConnection conn = getHttpsConn(url);
    return sendSecureHttpsConnection(conn, data);

  }

  public static HttpsURLConnection getHttpsConn(URL url)
      throws KeyStoreException, GeneralSecurityException, IOException {
    return (HttpsURLConnection) url.openConnection();
  }

  /**
   * Checks Internet availability. In case system information is defined to use proxy, proxy is set.
   * Therefore this method should be invoked before each Internet connection.
   * 
   * @return true in case Internet (https://activation.futit.cloud) is reachable.
   */
  public static boolean isInternetAvailable() {
    OBContext.setAdminMode();
    try {
      final SystemInformation sys = OBDal.getInstance().get(SystemInformation.class, "0");
      if (sys.isProxyRequired()) {
        // Proxy is required for connection.
        String host = sys.getProxyServer();
        int port = sys.getProxyPort().intValue();
        System.getProperties().put("proxySet", "true");
        System.getProperties().put("http.proxyHost", host);
        System.getProperties().put("https.proxyHost", host);
        System.getProperties().put("http.proxyPort", String.valueOf(port));
        System.getProperties().put("https.proxyPort", String.valueOf(port));

        System.setProperty("java.net.useSystemProxies", "true");

        if (sys.isRequiresProxyAuthentication()) {
          final String user = sys.getProxyUser();
          String pass = "";
          try {
            pass = FormatUtilities.encryptDecrypt(sys.getProxyPassword(), false);
          } catch (ServletException e) {
            log4j.error("Error setting proxy authenticator", e);
          }
          final String password = pass;

          // Used for standard http and https connections
          Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(user, password.toCharArray());
            }
          });

          // Used for SOAP webservices
          System.getProperties().setProperty("http.proxyUser", user);
          System.getProperties().setProperty("http.proxyPassword", password);
        }
      } else {
        System.getProperties().put("proxySet", "false");
        System.getProperties().remove("http.proxyHost");
        System.getProperties().remove("http.proxyPort");
        System.getProperties().remove("https.proxyHost");
        System.getProperties().remove("https.proxyPort");
        System.getProperties().remove("http.proxyUser");
        System.getProperties().remove("http.proxyPassword");
        System.setProperty("java.net.useSystemProxies", "false");
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    try {
      // Double check.
      URL url = new URL("https://activation.futit.cloud/license-server");
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setConnectTimeout(3000);
      conn.connect();
      if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
        return false;
      }
    } catch (Exception e) {
      log4j.info("Unable to reach activation.futit.cloud");
      return false;
    }
    return true;
  }

}
