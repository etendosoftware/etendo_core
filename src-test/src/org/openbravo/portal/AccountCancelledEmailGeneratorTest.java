/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;

/**
 * Tests for {@link AccountCancelledEmailGenerator}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AccountCancelledEmailGeneratorTest {

  private static final String ACCOUNT_CANCELLED = "accountCancelled";

  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;

  @Mock
  private AccountCancelledEmailBody body;

  @Mock
  private User user;

  @Mock
  private Client client;

  private AccountCancelledEmailGenerator generator;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);

    ObjenesisStd objenesis = new ObjenesisStd();
    generator = objenesis.newInstance(AccountCancelledEmailGenerator.class);

    // Inject the mock body
    Field bodyField = AccountCancelledEmailGenerator.class.getDeclaredField("body");
    bodyField.setAccessible(true);
    bodyField.set(generator, body);

    lenient().when(user.getClient()).thenReturn(client);
    lenient().when(client.getName()).thenReturn("Test Client");
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obMessageUtilsStatic != null) {
      obMessageUtilsStatic.close();
    }
  }
  /** Get subject uses client name. */

  @Test
  public void testGetSubjectUsesClientName() {
    obMessageUtilsStatic.when(
        () -> OBMessageUtils.getI18NMessage(eq("Portal_AccountCancelledSubject"), any(String[].class)))
        .thenReturn("Account cancelled for Test Client");

    String result = generator.getSubject(user, ACCOUNT_CANCELLED);
    assertEquals("Account cancelled for Test Client", result);
  }
  /** Get body sets data and generates. */

  @Test
  public void testGetBodySetsDataAndGenerates() {
    when(body.generate()).thenReturn("<html>body</html>");

    String result = generator.getBody(user, ACCOUNT_CANCELLED);

    verify(body).setData(user);
    verify(body).generate();
    assertEquals("<html>body</html>", result);
  }
  /** Get content type. */

  @Test
  public void testGetContentType() {
    assertEquals("text/html; charset=utf-8", generator.getContentType());
  }
  /** Is valid event returns true for account cancelled. */

  @Test
  public void testIsValidEventReturnsTrueForAccountCancelled() {
    assertTrue(generator.isValidEvent(ACCOUNT_CANCELLED, user));
  }
  /** Is valid event returns false for other event. */

  @Test
  public void testIsValidEventReturnsFalseForOtherEvent() {
    assertFalse(generator.isValidEvent("accountCreated", user));
  }
  /** Is valid event returns false for null. */

  @Test
  public void testIsValidEventReturnsFalseForNull() {
    assertFalse(generator.isValidEvent(null, user));
  }
  /** Get priority. */

  @Test
  public void testGetPriority() {
    assertEquals(100, generator.getPriority());
  }
  /** Prevents others execution returns false. */

  @Test
  public void testPreventsOthersExecutionReturnsFalse() {
    assertFalse(generator.preventsOthersExecution());
  }
  /** Is asynchronous returns true. */

  @Test
  public void testIsAsynchronousReturnsTrue() {
    assertTrue(generator.isAsynchronous());
  }
  /** Get attachments returns null. */

  @Test
  public void testGetAttachmentsReturnsNull() {
    assertNull(generator.getAttachments(user, ACCOUNT_CANCELLED));
  }
}
