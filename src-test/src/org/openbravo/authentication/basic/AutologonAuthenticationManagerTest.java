package org.openbravo.authentication.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.authentication.AuthenticationException;

/**
 * Tests for {@link AutologonAuthenticationManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AutologonAuthenticationManagerTest {

  private static final String VALID_USER_ID = "100";
  private static final String AUTOLOGON_USERNAME = "testuser";

  @Test
  public void testUseExternalLoginPageReturnsTrue() {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    assertTrue(manager.useExternalLoginPage());
  }

  @Test
  public void testDoAuthenticateReturnsUserIdWhenValid() throws Exception {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    setPrivateField(manager, "m_sUserId", VALID_USER_ID);
    setPrivateField(manager, "m_sAutologonUsername", AUTOLOGON_USERNAME);

    Method doAuthenticate = AutologonAuthenticationManager.class.getDeclaredMethod(
        "doAuthenticate",
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class);
    doAuthenticate.setAccessible(true);

    String result = (String) doAuthenticate.invoke(manager, null, null);
    assertEquals(VALID_USER_ID, result);
  }

  @Test(expected = AuthenticationException.class)
  public void testDoAuthenticateThrowsWhenUserIdIsNull() throws Throwable {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    setPrivateField(manager, "m_sUserId", null);
    setPrivateField(manager, "m_sAutologonUsername", AUTOLOGON_USERNAME);

    Method doAuthenticate = AutologonAuthenticationManager.class.getDeclaredMethod(
        "doAuthenticate",
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class);
    doAuthenticate.setAccessible(true);

    try {
      doAuthenticate.invoke(manager, null, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }

  @Test(expected = AuthenticationException.class)
  public void testDoAuthenticateThrowsWhenUserIdIsEmpty() throws Throwable {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    setPrivateField(manager, "m_sUserId", "");
    setPrivateField(manager, "m_sAutologonUsername", AUTOLOGON_USERNAME);

    Method doAuthenticate = AutologonAuthenticationManager.class.getDeclaredMethod(
        "doAuthenticate",
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class);
    doAuthenticate.setAccessible(true);

    try {
      doAuthenticate.invoke(manager, null, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }

  @Test(expected = AuthenticationException.class)
  public void testDoAuthenticateThrowsWhenUserIdIsMinusOne() throws Throwable {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    setPrivateField(manager, "m_sUserId", "-1");
    setPrivateField(manager, "m_sAutologonUsername", AUTOLOGON_USERNAME);

    Method doAuthenticate = AutologonAuthenticationManager.class.getDeclaredMethod(
        "doAuthenticate",
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class);
    doAuthenticate.setAccessible(true);

    try {
      doAuthenticate.invoke(manager, null, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }

  @Test(expected = AuthenticationException.class)
  public void testDoAuthenticateThrowsWithEmptyAutologonUsername() throws Throwable {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    setPrivateField(manager, "m_sUserId", null);
    setPrivateField(manager, "m_sAutologonUsername", "");

    Method doAuthenticate = AutologonAuthenticationManager.class.getDeclaredMethod(
        "doAuthenticate",
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class);
    doAuthenticate.setAccessible(true);

    try {
      doAuthenticate.invoke(manager, null, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }

  @Test(expected = AuthenticationException.class)
  public void testDoAuthenticateThrowsWithNullAutologonUsername() throws Throwable {
    AutologonAuthenticationManager manager = new AutologonAuthenticationManager();
    setPrivateField(manager, "m_sUserId", null);
    setPrivateField(manager, "m_sAutologonUsername", null);

    Method doAuthenticate = AutologonAuthenticationManager.class.getDeclaredMethod(
        "doAuthenticate",
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class);
    doAuthenticate.setAccessible(true);

    try {
      doAuthenticate.invoke(manager, null, null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = AutologonAuthenticationManager.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
