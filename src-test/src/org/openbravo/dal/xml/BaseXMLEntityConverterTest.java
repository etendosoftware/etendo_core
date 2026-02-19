package org.openbravo.dal.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.ClientEnabled;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Tests for {@link BaseXMLEntityConverter}.
 */
@SuppressWarnings("java:S112")
@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseXMLEntityConverterTest {

  private static final String ERROR = "error";

  private BaseXMLEntityConverter instance;

  @Mock
  private EntityResolver mockEntityResolver;

  @Mock
  private Client mockClient;

  @Mock
  private Organization mockOrganization;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    instance = new BaseXMLEntityConverter();
    instance.setEntityResolver(mockEntityResolver);
    instance.setClient(mockClient);
    instance.setOrganization(mockOrganization);
    lenient().when(mockClient.getId()).thenReturn("TEST_CLIENT_ID");
    lenient().when(mockOrganization.getId()).thenReturn("TEST_ORG_ID");
  }
  /** Get set client. */

  @Test
  public void testGetSetClient() {
    Client client = mock(Client.class);
    instance.setClient(client);
    assertEquals(client, instance.getClient());
  }
  /** Get set organization. */

  @Test
  public void testGetSetOrganization() {
    Organization org = mock(Organization.class);
    instance.setOrganization(org);
    assertEquals(org, instance.getOrganization());
  }
  /** Get set option client import. */

  @Test
  public void testGetSetOptionClientImport() {
    assertFalse(instance.isOptionClientImport());
    instance.setOptionClientImport(true);
    assertTrue(instance.isOptionClientImport());
  }
  /** Get set option import audit info. */

  @Test
  public void testGetSetOptionImportAuditInfo() {
    assertFalse(instance.isOptionImportAuditInfo());
    instance.setOptionImportAuditInfo(true);
    assertTrue(instance.isOptionImportAuditInfo());
  }
  /** Get set import processor. */

  @Test
  public void testGetSetImportProcessor() {
    assertNull(instance.getImportProcessor());
    EntityXMLProcessor processor = mock(EntityXMLProcessor.class);
    instance.setImportProcessor(processor);
    assertEquals(processor, instance.getImportProcessor());
  }
  /** Get entity resolver. */

  @Test
  public void testGetEntityResolver() {
    assertEquals(mockEntityResolver, instance.getEntityResolver());
  }
  /** To insert and to update lists initially empty. */

  @Test
  public void testToInsertAndToUpdateListsInitiallyEmpty() {
    assertTrue(instance.getToInsert().isEmpty());
    assertTrue(instance.getToUpdate().isEmpty());
  }
  /** Error messages null when empty. */

  @Test
  public void testErrorMessagesNullWhenEmpty() {
    assertNull(instance.getErrorMessages());
  }
  /** Warning messages null when empty. */

  @Test
  public void testWarningMessagesNullWhenEmpty() {
    assertNull(instance.getWarningMessages());
  }
  /** Log messages null when empty. */

  @Test
  public void testLogMessagesNullWhenEmpty() {
    assertNull(instance.getLogMessages());
  }
  /**
   * Warn adds warning message.
   * @throws Exception if an error occurs
   */

  @Test
  public void testWarnAddsWarningMessage() throws Exception {
    Method warnMethod = BaseXMLEntityConverter.class.getDeclaredMethod("warn", String.class);
    warnMethod.setAccessible(true);

    warnMethod.invoke(instance, "test warning");
    assertEquals("test warning", instance.getWarningMessages());
  }
  /**
   * Warn multiple messages joined by newline.
   * @throws Exception if an error occurs
   */

  @Test
  public void testWarnMultipleMessagesJoinedByNewline() throws Exception {
    Method warnMethod = BaseXMLEntityConverter.class.getDeclaredMethod("warn", String.class);
    warnMethod.setAccessible(true);

    warnMethod.invoke(instance, "warning 1");
    warnMethod.invoke(instance, "warning 2");
    assertEquals("warning 1\nwarning 2", instance.getWarningMessages());
  }
  /**
   * Log adds log message.
   * @throws Exception if an error occurs
   */

  @Test
  public void testLogAddsLogMessage() throws Exception {
    Method logMethod = BaseXMLEntityConverter.class.getDeclaredMethod("log", String.class);
    logMethod.setAccessible(true);

    logMethod.invoke(instance, "log entry");
    assertEquals("log entry", instance.getLogMessages());
  }
  /**
   * Log multiple messages joined by newline.
   * @throws Exception if an error occurs
   */

  @Test
  public void testLogMultipleMessagesJoinedByNewline() throws Exception {
    Method logMethod = BaseXMLEntityConverter.class.getDeclaredMethod("log", String.class);
    logMethod.setAccessible(true);

    logMethod.invoke(instance, "log 1");
    logMethod.invoke(instance, "log 2");
    assertEquals("log 1\nlog 2", instance.getLogMessages());
  }
  /**
   * Error adds error message.
   * @throws Exception if an error occurs
   */

  @Test
  public void testErrorAddsErrorMessage() throws Exception {
    Method errorMethod = BaseXMLEntityConverter.class.getDeclaredMethod(ERROR, String.class);
    errorMethod.setAccessible(true);

    errorMethod.invoke(instance, "error message");
    assertEquals("error message", instance.getErrorMessages());
  }
  /**
   * Has error occured false initially.
   * @throws Exception if an error occurs
   */

  @Test
  public void testHasErrorOccuredFalseInitially() throws Exception {
    Method method = BaseXMLEntityConverter.class.getDeclaredMethod("hasErrorOccured");
    method.setAccessible(true);

    assertFalse((Boolean) method.invoke(instance));
  }
  /**
   * Has error occured true after error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testHasErrorOccuredTrueAfterError() throws Exception {
    Method errorMethod = BaseXMLEntityConverter.class.getDeclaredMethod(ERROR, String.class);
    errorMethod.setAccessible(true);
    errorMethod.invoke(instance, "some error");

    Method hasErrorMethod = BaseXMLEntityConverter.class.getDeclaredMethod("hasErrorOccured");
    hasErrorMethod.setAccessible(true);
    assertTrue((Boolean) hasErrorMethod.invoke(instance));
  }
  /**
   * Error throws after too many errors.
   * @throws IllegalAccessException if an error occurs
   * @throws InvocationTargetException if an error occurs
   * @throws NoSuchMethodException if an error occurs
   */

  @Test(expected = EntityXMLException.class)
  public void testErrorThrowsAfterTooManyErrors() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method errorMethod = BaseXMLEntityConverter.class.getDeclaredMethod(ERROR, String.class);
    errorMethod.setAccessible(true);

    try {
      for (int i = 0; i < 25; i++) {
        errorMethod.invoke(instance, "error " + i);
      }
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof EntityXMLException) {
        throw (EntityXMLException) e.getCause();
      }
      throw e;
    }
  }
  /** Replace value returns new value when no processor. */

  @Test
  public void testReplaceValueReturnsNewValueWhenNoProcessor() {
    instance.setImportProcessor(null);

    Method replaceValueMethod;
    try {
      replaceValueMethod = BaseXMLEntityConverter.class.getDeclaredMethod("replaceValue",
          BaseOBObject.class, Property.class, Object.class);
      replaceValueMethod.setAccessible(true);

      Object result = replaceValueMethod.invoke(instance, mock(BaseOBObject.class),
          mock(Property.class), "testValue");
      assertEquals("testValue", result);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  /**
   * Replace value delegates to processor when set.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceValueDelegatesToProcessorWhenSet() throws Exception {
    EntityXMLProcessor processor = mock(EntityXMLProcessor.class);
    BaseOBObject mockBob = mock(BaseOBObject.class);
    Property mockProperty = mock(Property.class);
    when(processor.replaceValue(mockBob, mockProperty, "input")).thenReturn("replaced");

    instance.setImportProcessor(processor);

    Method replaceValueMethod = BaseXMLEntityConverter.class.getDeclaredMethod("replaceValue",
        BaseOBObject.class, Property.class, Object.class);
    replaceValueMethod.setAccessible(true);

    Object result = replaceValueMethod.invoke(instance, mockBob, mockProperty, "input");
    assertEquals("replaced", result);
  }
  /**
   * Resolve calls entity resolver.
   * @throws Exception if an error occurs
   */

  @Test
  public void testResolveCallsEntityResolver() throws Exception {
    BaseOBObject mockBob = mock(BaseOBObject.class);
    when(mockEntityResolver.resolve("TestEntity", "ID001", false, false)).thenReturn(mockBob);

    Method resolveMethod = BaseXMLEntityConverter.class.getDeclaredMethod("resolve",
        String.class, String.class, boolean.class);
    resolveMethod.setAccessible(true);

    Object result = resolveMethod.invoke(instance, "TestEntity", "ID001", false);
    assertEquals(mockBob, result);
  }
  /**
   * Warn different client org skips when client import.
   * @throws Exception if an error occurs
   */

  @Test
  public void testWarnDifferentClientOrgSkipsWhenClientImport() throws Exception {
    instance.setOptionClientImport(true);

    Method warnMethod = BaseXMLEntityConverter.class.getDeclaredMethod("warnDifferentClientOrg",
        BaseOBObject.class, String.class);
    warnMethod.setAccessible(true);

    BaseOBObject mockBob = mock(BaseOBObject.class);
    Entity mockEntity = mock(Entity.class);
    when(mockBob.getEntity()).thenReturn(mockEntity);
    when(mockEntity.isClientEnabled()).thenReturn(true);

    warnMethod.invoke(instance, mockBob, "Creating");

    // No warning should be added since optionClientImport is true
    assertNull(instance.getWarningMessages());
  }
}
