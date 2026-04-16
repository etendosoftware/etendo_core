/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.utility.poc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.mail.PasswordAuthentication;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link ClientAuthenticator}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class ClientAuthenticatorTest {
  /** Get password authentication returns credentials. */

  @Test
  public void testGetPasswordAuthenticationReturnsCredentials() {
    ClientAuthenticator authenticator = new ClientAuthenticator("user@example.com", "secret123");

    PasswordAuthentication auth = authenticator.getPasswordAuthentication();

    assertNotNull(auth);
    assertEquals("user@example.com", auth.getUserName());
    assertEquals("secret123", auth.getPassword());
  }
  /** Get password authentication with empty credentials. */

  @Test
  public void testGetPasswordAuthenticationWithEmptyCredentials() {
    ClientAuthenticator authenticator = new ClientAuthenticator("", "");

    PasswordAuthentication auth = authenticator.getPasswordAuthentication();

    assertNotNull(auth);
    assertEquals("", auth.getUserName());
    assertEquals("", auth.getPassword());
  }
  /** Get password authentication with null credentials. */

  @Test
  public void testGetPasswordAuthenticationWithNullCredentials() {
    ClientAuthenticator authenticator = new ClientAuthenticator(null, null);

    PasswordAuthentication auth = authenticator.getPasswordAuthentication();

    assertNotNull(auth);
    assertEquals(null, auth.getUserName());
    assertEquals(null, auth.getPassword());
  }
}
