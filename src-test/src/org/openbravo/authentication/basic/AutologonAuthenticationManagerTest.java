package org.openbravo.authentication.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.authentication.AuthenticationException;

/**
 * Tests for {@link AutologonAuthenticationManager}.
 */
@SuppressWarnings("java:S112")
@RunWith(MockitoJUnitRunner.class)
public class AutologonAuthenticationManagerTest {

  private static final String M_S_USER_ID = "m_sUserId";
  private static final String M_S_AUTOLOGON_USERNAME = "m_sAutologonUsername";
  private static final String DO_AUTHENTICATE = "doAuthenticate";

  private static final String VALID_USER_ID = "100";
  private static final String AUTOLOGON_USERNAME = "testuser";
  /** Use external login page returns true. */

  @Test
  public void testUseExternalLoginPageReturnsTrue() {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    assertTrue(manager.useExternalLoginPage());
  }
  /**
   * Do authenticate returns user id when valid.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoAuthenticateReturnsUserIdWhenValid() throws Exception {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    setPrivateField(manager, M_S_USER_ID, VALID_USER_ID);
    setPrivateField(manager, M_S_AUTOLOGON_USERNAME, AUTOLOGON_USERNAME);

    Method doAuthenticate = AutologonAuthenticationManager.class.getDeclaredMethod(
        DO_AUTHENTICATE,
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class);
    doAuthenticate.setAccessible(true);

    String result = (String) doAuthenticate.invoke(manager, null, null);
    assertEquals(VALID_USER_ID, result);
  }
  /**
   * Do authenticate throws when user id is null.
   * @throws Throwable if an error occurs
   */

  @Test(expected = AuthenticationException.class)
  public void testDoAuthenticateThrowsWhenUserIdIsNull() throws Throwable {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    setPrivateField(manager, M_S_USER_ID, null);
    setPrivateField(manager, M_S_AUTOLOGON_USERNAME, AUTOLOGON_USERNAME);

    Method doAuthenticate = AutologonAuthenticationManager.class.getDeclaredMethod(
        DO_AUTHENTICATE,
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class);
    doAuthenticate.setAccessible(true);

    try {
      doAuthenticate.invoke(manager, null, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }
  /**
   * Do authenticate throws when user id is empty.
   * @throws Throwable if an error occurs
   */

  @Test(expected = AuthenticationException.class)
  public void testDoAuthenticateThrowsWhenUserIdIsEmpty() throws Throwable {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    setPrivateField(manager, M_S_USER_ID, "");
    setPrivateField(manager, M_S_AUTOLOGON_USERNAME, AUTOLOGON_USERNAME);

    Method doAuthenticate = AutologonAuthenticationManager.class.getDeclaredMethod(
        DO_AUTHENTICATE,
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class);
    doAuthenticate.setAccessible(true);

    try {
      doAuthenticate.invoke(manager, null, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }
  /**
   * Do authenticate throws when user id is minus one.
   * @throws Throwable if an error occurs
   */

  @Test(expected = AuthenticationException.class)
  public void testDoAuthenticateThrowsWhenUserIdIsMinusOne() throws Throwable {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    setPrivateField(manager, M_S_USER_ID, "-1");
    setPrivateField(manager, M_S_AUTOLOGON_USERNAME, AUTOLOGON_USERNAME);

    Method doAuthenticate = AutologonAuthenticationManager.class.getDeclaredMethod(
        DO_AUTHENTICATE,
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class);
    doAuthenticate.setAccessible(true);

    try {
      doAuthenticate.invoke(manager, null, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }
  /**
   * Do authenticate throws with empty autologon username.
   * @throws Throwable if an error occurs
   */

  @Test(expected = AuthenticationException.class)
  public void testDoAuthenticateThrowsWithEmptyAutologonUsername() throws Throwable {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    setPrivateField(manager, M_S_USER_ID, null);
    setPrivateField(manager, M_S_AUTOLOGON_USERNAME, "");

    Method doAuthenticate = AutologonAuthenticationManager.class.getDeclaredMethod(
        DO_AUTHENTICATE,
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class);
    doAuthenticate.setAccessible(true);

    try {
      doAuthenticate.invoke(manager, null, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }
  /**
   * Do authenticate throws with null autologon username.
   * @throws Throwable if an error occurs
   */

  @Test(expected = AuthenticationException.class)
  public void testDoAuthenticateThrowsWithNullAutologonUsername() throws Throwable {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    setPrivateField(manager, M_S_USER_ID, null);
    setPrivateField(manager, M_S_AUTOLOGON_USERNAME, null);

    Method doAuthenticate = AutologonAuthenticationManager.class.getDeclaredMethod(
        DO_AUTHENTICATE,
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class);
    doAuthenticate.setAccessible(true);

    try {
      doAuthenticate.invoke(manager, null, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws IllegalAccessException, NoSuchFieldException {
    Field field = AutologonAuthenticationManager.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
