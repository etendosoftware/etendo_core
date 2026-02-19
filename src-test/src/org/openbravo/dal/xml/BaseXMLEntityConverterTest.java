package org.openbravo.dal.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseXMLEntityConverterTest {

  private BaseXMLEntityConverter instance;

  @Mock
  private EntityResolver mockEntityResolver;

  @Mock
  private Client mockClient;

  @Mock
  private Organization mockOrganization;

  @Before
  public void setUp() throws Exception {
    instance = new BaseXMLEntityConverter();
    instance.setEntityResolver(mockEntityResolver);
    instance.setClient(mockClient);
    instance.setOrganization(mockOrganization);
    lenient().when(mockClient.getId()).thenReturn("TEST_CLIENT_ID");
    lenient().when(mockOrganization.getId()).thenReturn("TEST_ORG_ID");
  }

  @Test
  public void testGetSetClient() {
    Client client = mock(Client.class);
    instance.setClient(client);
    assertEquals(client, instance.getClient());
  }

  @Test
  public void testGetSetOrganization() {
    Organization org = mock(Organization.class);
    instance.setOrganization(org);
    assertEquals(org, instance.getOrganization());
  }

  @Test
  public void testGetSetOptionClientImport() {
    assertFalse(instance.isOptionClientImport());
    instance.setOptionClientImport(true);
    assertTrue(instance.isOptionClientImport());
  }

  @Test
  public void testGetSetOptionImportAuditInfo() {
    assertFalse(instance.isOptionImportAuditInfo());
    instance.setOptionImportAuditInfo(true);
    assertTrue(instance.isOptionImportAuditInfo());
  }

  @Test
  public void testGetSetImportProcessor() {
    assertNull(instance.getImportProcessor());
    EntityXMLProcessor processor = mock(EntityXMLProcessor.class);
    instance.setImportProcessor(processor);
    assertEquals(processor, instance.getImportProcessor());
  }

  @Test
  public void testGetEntityResolver() {
    assertEquals(mockEntityResolver, instance.getEntityResolver());
  }

  @Test
  public void testToInsertAndToUpdateListsInitiallyEmpty() {
    assertTrue(instance.getToInsert().isEmpty());
    assertTrue(instance.getToUpdate().isEmpty());
  }

  @Test
  public void testErrorMessagesNullWhenEmpty() {
    assertNull(instance.getErrorMessages());
  }

  @Test
  public void testWarningMessagesNullWhenEmpty() {
    assertNull(instance.getWarningMessages());
  }

  @Test
  public void testLogMessagesNullWhenEmpty() {
    assertNull(instance.getLogMessages());
  }

  @Test
  public void testWarnAddsWarningMessage() throws Exception {
    Method warnMethod = BaseXMLEntityConverter.class.getDeclaredMethod("warn", String.class);
    warnMethod.setAccessible(true);

    warnMethod.invoke(instance, "test warning");
    assertEquals("test warning", instance.getWarningMessages());
  }

  @Test
  public void testWarnMultipleMessagesJoinedByNewline() throws Exception {
    Method warnMethod = BaseXMLEntityConverter.class.getDeclaredMethod("warn", String.class);
    warnMethod.setAccessible(true);

    warnMethod.invoke(instance, "warning 1");
    warnMethod.invoke(instance, "warning 2");
    assertEquals("warning 1\nwarning 2", instance.getWarningMessages());
  }

  @Test
  public void testLogAddsLogMessage() throws Exception {
    Method logMethod = BaseXMLEntityConverter.class.getDeclaredMethod("log", String.class);
    logMethod.setAccessible(true);

    logMethod.invoke(instance, "log entry");
    assertEquals("log entry", instance.getLogMessages());
  }

  @Test
  public void testLogMultipleMessagesJoinedByNewline() throws Exception {
    Method logMethod = BaseXMLEntityConverter.class.getDeclaredMethod("log", String.class);
    logMethod.setAccessible(true);

    logMethod.invoke(instance, "log 1");
    logMethod.invoke(instance, "log 2");
    assertEquals("log 1\nlog 2", instance.getLogMessages());
  }

  @Test
  public void testErrorAddsErrorMessage() throws Exception {
    Method errorMethod = BaseXMLEntityConverter.class.getDeclaredMethod("error", String.class);
    errorMethod.setAccessible(true);

    errorMethod.invoke(instance, "error message");
    assertEquals("error message", instance.getErrorMessages());
  }

  @Test
  public void testHasErrorOccuredFalseInitially() throws Exception {
    Method method = BaseXMLEntityConverter.class.getDeclaredMethod("hasErrorOccured");
    method.setAccessible(true);

    assertFalse((Boolean) method.invoke(instance));
  }

  @Test
  public void testHasErrorOccuredTrueAfterError() throws Exception {
    Method errorMethod = BaseXMLEntityConverter.class.getDeclaredMethod("error", String.class);
    errorMethod.setAccessible(true);
    errorMethod.invoke(instance, "some error");

    Method hasErrorMethod = BaseXMLEntityConverter.class.getDeclaredMethod("hasErrorOccured");
    hasErrorMethod.setAccessible(true);
    assertTrue((Boolean) hasErrorMethod.invoke(instance));
  }

  @Test(expected = EntityXMLException.class)
  public void testErrorThrowsAfterTooManyErrors() throws Exception {
    Method errorMethod = BaseXMLEntityConverter.class.getDeclaredMethod("error", String.class);
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
