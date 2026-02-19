/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.client.kernel.Template;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;

/**
 * Tests for {@link AccountCancelledEmailBody}.
 */
@SuppressWarnings("java:S112")
@RunWith(MockitoJUnitRunner.Silent.class)
public class AccountCancelledEmailBodyTest {

  private MockedStatic<OBDal> obDalStatic;

  @Mock
  private OBDal obDal;

  @Mock
  private Template template;

  private AccountCancelledEmailBody emailBody;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(obDal);

    ObjenesisStd objenesis = new ObjenesisStd();
    emailBody = objenesis.newInstance(AccountCancelledEmailBody.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }
  /** Set data and get user. */

  @Test
  public void testSetDataAndGetUser() {
    User user = mock(User.class);
    emailBody.setData(user);
    assertEquals(user, emailBody.getUser());
  }
  /** Get user returns null before set data. */

  @Test
  public void testGetUserReturnsNullBeforeSetData() {
    assertNull(emailBody.getUser());
  }
  /** Get data returns self. */

  @Test
  public void testGetDataReturnsSelf() {
    Object result = emailBody.getData();
    assertSame(emailBody, result);
  }
  /** Get component template uses correct id. */

  @Test
  public void testGetComponentTemplateUsesCorrectId() {
    when(obDal.get(Template.class, "E5D5653B19734DA5AE3BEB7019B3D1E7")).thenReturn(template);

    java.lang.reflect.Method method;
    try {
      method = AccountCancelledEmailBody.class.getDeclaredMethod("getComponentTemplate");
      method.setAccessible(true);
      Template result = (Template) method.invoke(emailBody);
      assertEquals(template, result);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  /** Set data overwrites previous user. */

  @Test
  public void testSetDataOverwritesPreviousUser() {
    User user1 = mock(User.class);
    User user2 = mock(User.class);

    emailBody.setData(user1);
    assertEquals(user1, emailBody.getUser());

    emailBody.setData(user2);
    assertEquals(user2, emailBody.getUser());
  }
}
