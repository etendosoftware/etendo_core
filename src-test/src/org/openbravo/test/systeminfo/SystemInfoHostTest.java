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
 * All portions are Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.systeminfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;
import org.openbravo.erpCommon.utility.SystemInfo;

/**
 * Tests the identity of the server hosting the instance, gathered by {@link SystemInfo} and
 * reported by the Heartbeat to the License Server.
 */
public class SystemInfoHostTest {

  private static final int MAX_HOSTNAME_LENGTH = 60;

  /**
   * The labels are the keys the License Server persists the values under. Renaming them silently
   * breaks the Hostname and IP Address columns of the instance record.
   */
  @Test
  public void testHostItemLabelsMatchLicenseServerKeys() {
    // WHEN / THEN
    assertEquals("hostname", SystemInfo.Item.HOSTNAME.getLabel());
    assertEquals("ip", SystemInfo.Item.IP_ADDRESS.getLabel());
  }

  /**
   * Only items that are not ID information are gathered by {@code SystemInfo.load()}, which is the
   * method the Heartbeat calls before building the payload.
   */
  @Test
  public void testHostItemsAreGatheredOnLoad() {
    // WHEN / THEN
    assertFalse("The hostname must be gathered by SystemInfo.load()",
        SystemInfo.Item.HOSTNAME.isIdInfo());
    assertFalse("The IP address must be gathered by SystemInfo.load()",
        SystemInfo.Item.IP_ADDRESS.isIdInfo());
  }

  /**
   * The hostname must never be null so that the Heartbeat is not interrupted and no other field is
   * lost when the name cannot be resolved.
   */
  @Test
  public void testGetHostnameNeverReturnsNull() {
    // WHEN
    String hostname = SystemInfo.getHostname();

    // THEN
    assertNotNull("The hostname must never be null", hostname);
    assertTrue("The hostname must not exceed " + MAX_HOSTNAME_LENGTH + " characters",
        hostname.length() <= MAX_HOSTNAME_LENGTH);
  }

  /**
   * When the hostname can be resolved, the reported value is the name of this server.
   */
  @Test
  public void testGetHostnameReportsTheLocalHostName() {
    // GIVEN
    String expected;
    try {
      expected = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      // the host name cannot be resolved in this environment, the empty value is already covered
      // by testGetHostnameNeverReturnsNull
      return;
    }

    // WHEN
    String hostname = SystemInfo.getHostname();

    // THEN
    assertTrue("The hostname must be the name of the local host",
        expected.startsWith(hostname) && !hostname.isEmpty());
  }

  /**
   * The IP address must never be null and, when it can be resolved, it must be a numerical label,
   * not a name and not the wildcard address.
   *
   * @throws UnknownHostException
   *     if the reported address is not a numerical label, in which case parsing it back forces a
   *     name resolution that this environment cannot satisfy
   */
  @Test
  public void testGetIpAddressReturnsANumericalLabel() throws UnknownHostException {
    // WHEN
    String ip = SystemInfo.getIpAddress();

    // THEN
    assertNotNull("The IP address must never be null", ip);
    if (!ip.isEmpty()) {
      assertEquals("The IP address must be a numerical label", ip,
          InetAddress.getByName(ip).getHostAddress());
      assertFalse("The wildcard address must not be reported", "0.0.0.0".equals(ip));
    }
  }
}
