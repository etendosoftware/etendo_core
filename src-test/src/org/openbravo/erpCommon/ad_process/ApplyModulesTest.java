package org.openbravo.erpCommon.ad_process;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.xmlEngine.XmlDocument;
import org.openbravo.xmlEngine.XmlEngine;
import org.openbravo.xmlEngine.XmlTemplate;

import java.lang.reflect.Field;

/**
 * Tests for {@link ApplyModules}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplyModulesTest {

  private ApplyModules instance;

  @Mock
  private HttpServletResponse mockResponse;

  @Mock
  private VariablesSecureApp mockVars;

  @Mock
  private XmlEngine mockXmlEngine;

  @Mock
  private XmlTemplate mockXmlTemplate;

  @Mock
  private XmlDocument mockXmlDocument;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ApplyModules.class);

    // Set xmlEngine field from HttpBaseServlet
    Field xmlEngineField = findField(instance.getClass(), "xmlEngine");
    xmlEngineField.setAccessible(true);
    xmlEngineField.set(instance, mockXmlEngine);

    // Set strReplaceWith field from HttpBaseServlet
    Field strReplaceWithField = findField(instance.getClass(), "strReplaceWith");
    strReplaceWithField.setAccessible(true);
    strReplaceWithField.set(instance, "/openbravo");
  }

  @Test
  public void testPrintExternalRebuildWithRestartTrue() throws Exception {
    // Arrange
    when(mockVars.getLanguage()).thenReturn("en_US");
    when(mockVars.getTheme()).thenReturn("Default");
    when(mockXmlEngine.readXmlTemplate(anyString(), any(String[].class)))
        .thenReturn(mockXmlTemplate);
    when(mockXmlTemplate.createXmlDocument()).thenReturn(mockXmlDocument);
    when(mockXmlDocument.print()).thenReturn("<html>test</html>");

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(mockResponse.getWriter()).thenReturn(printWriter);

    // Act
    Method method = ApplyModules.class.getDeclaredMethod("printExternalRebuild",
        HttpServletResponse.class, VariablesSecureApp.class, boolean.class);
    method.setAccessible(true);
    method.invoke(instance, mockResponse, mockVars, true);

    // Assert
    verify(mockResponse).setContentType("text/html; charset=UTF-8");
    verify(mockXmlDocument).setParameter("language", "defaultLang=\"en_US\";");
    verify(mockXmlDocument).setParameter("theme", "Default");
  }

  @Test
  public void testPrintExternalRebuildWithRestartFalse() throws Exception {
    // Arrange
    when(mockVars.getLanguage()).thenReturn("en_US");
    when(mockVars.getTheme()).thenReturn("Default");
    when(mockXmlEngine.readXmlTemplate(anyString(), any(String[].class)))
        .thenReturn(mockXmlTemplate);
    when(mockXmlTemplate.createXmlDocument()).thenReturn(mockXmlDocument);
    when(mockXmlDocument.print()).thenReturn("<html>test</html>");

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(mockResponse.getWriter()).thenReturn(printWriter);

    // Act
    Method method = ApplyModules.class.getDeclaredMethod("printExternalRebuild",
        HttpServletResponse.class, VariablesSecureApp.class, boolean.class);
    method.setAccessible(true);
    method.invoke(instance, mockResponse, mockVars, false);

    // Assert
    verify(mockResponse).setContentType("text/html; charset=UTF-8");
    verify(mockXmlEngine).readXmlTemplate(
        org.mockito.ArgumentMatchers.eq("org/openbravo/erpCommon/ad_process/ApplyModulesExternal"),
        any(String[].class));
  }

  private Field findField(Class<?> clazz, String fieldName) {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new RuntimeException("Field not found: " + fieldName);
  }
}
