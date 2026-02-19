/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.obps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Reference;

/**
 * Tests for {@link ActivationTask}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ActivationTaskTest {

  private static final String PURPOSE_REFERENCE_ID = "60E231391A7348DDA7171E780F62EF99";

  private MockedStatic<OBDal> obDalStatic;

  @Mock
  private OBDal obDal;

  @Mock
  private Reference purposeRef;

  private ActivationTask task;

  @Before
  public void setUp() throws Exception {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(obDal);

    ObjenesisStd objenesis = new ObjenesisStd();
    task = objenesis.newInstance(ActivationTask.class);

    // Set up a mock Project with a base dir so getProject().getBaseDir() works
    File baseDir = new File("/tmp/base");
    baseDir.mkdirs();
    Project antProject = new Project();
    antProject.setBaseDir(baseDir);
    task.setProject(antProject);
  }

  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }

  @Test
  public void testSetPublicKey() {
    task.setPublicKey("testKey123");
    String value = getFieldValue(task, "publicKey");
    assertEquals("testKey123", value);
  }

  @Test
  public void testSetPublicKeyFile() {
    File file = new File("/tmp/key.pub");
    task.setPublicKeyFile(file);
    File value = getFieldValue(task, "publicKeyFile");
    assertEquals(file, value);
  }

  @Test
  public void testSetPurpose() {
    task.setPurpose("testPurpose");
    String value = getFieldValue(task, "purpose");
    assertEquals("testPurpose", value);
  }

  @Test
  public void testVerifyParametersThrowsWhenPurposeIsNull() throws Exception {
    setField(task, "purpose", "purpose", null);
    setField(task, "publicKey", "publicKey", "someKey");

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for null purpose");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
      assertEquals("purpose parameter is required", e.getCause().getMessage());
    }
  }

  @Test
  public void testVerifyParametersThrowsWhenPurposeIsEmpty() throws Exception {
    setField(task, "purpose", "purpose", "");
    setField(task, "publicKey", "publicKey", "someKey");

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for empty purpose");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
      assertEquals("purpose parameter is required", e.getCause().getMessage());
    }
  }

  @Test
  public void testVerifyParametersThrowsWhenPurposeNotFoundInReference() throws Exception {
    setField(task, "purpose", "purpose", "invalidPurpose");
    setField(task, "publicKey", "publicKey", "someKey");

    when(obDal.get(Reference.class, PURPOSE_REFERENCE_ID)).thenReturn(purposeRef);

    org.openbravo.model.ad.domain.List listItem = mock(
        org.openbravo.model.ad.domain.List.class);
    when(listItem.getSearchKey()).thenReturn("validPurpose");
    when(listItem.getName()).thenReturn("Valid Purpose");

    List<org.openbravo.model.ad.domain.List> listItems = new ArrayList<>();
    listItems.add(listItem);
    when(purposeRef.getADListList()).thenReturn(listItems);

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for invalid purpose");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
      String message = e.getCause().getMessage();
      assertNotNull(message);
      assertEquals(true, message.contains("invalidPurpose"));
      assertEquals(true, message.contains("valid values for purpose"));
    }
  }

  @Test
  public void testVerifyParametersThrowsWhenNoPublicKeyProvided() throws Exception {
    setField(task, "purpose", "purpose", "validPurpose");
    setField(task, "publicKey", "publicKey", null);
    setField(task, "publicKeyFile", "publicKeyFile", null);

    when(obDal.get(Reference.class, PURPOSE_REFERENCE_ID)).thenReturn(purposeRef);

    org.openbravo.model.ad.domain.List listItem = mock(
        org.openbravo.model.ad.domain.List.class);
    when(listItem.getSearchKey()).thenReturn("validPurpose");

    List<org.openbravo.model.ad.domain.List> listItems = new ArrayList<>();
    listItems.add(listItem);
    when(purposeRef.getADListList()).thenReturn(listItems);

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for missing public key");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
      assertEquals(true, e.getCause().getMessage().contains("Public key must be provided"));
    }
  }

  @Test
  public void testVerifyParametersThrowsWhenBothPublicKeyAndFileProvided() throws Exception {
    setField(task, "purpose", "purpose", "validPurpose");
    setField(task, "publicKey", "publicKey", "somePublicKey");
    setField(task, "publicKeyFile", "publicKeyFile", new File("/tmp/notbase/key.pub"));

    when(obDal.get(Reference.class, PURPOSE_REFERENCE_ID)).thenReturn(purposeRef);

    org.openbravo.model.ad.domain.List listItem = mock(
        org.openbravo.model.ad.domain.List.class);
    when(listItem.getSearchKey()).thenReturn("validPurpose");

    List<org.openbravo.model.ad.domain.List> listItems = new ArrayList<>();
    listItems.add(listItem);
    when(purposeRef.getADListList()).thenReturn(listItems);

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for both publicKey and publicKeyFile set");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
      assertEquals(true, e.getCause().getMessage().contains("Only one of the publicKey"));
    }
  }

  @Test
  public void testVerifyParametersSucceedsWithValidPurposeAndPublicKey() throws Exception {
    setField(task, "purpose", "purpose", "validPurpose");
    setField(task, "publicKey", "publicKey", "somePublicKey");
    setField(task, "publicKeyFile", "publicKeyFile", null);

    when(obDal.get(Reference.class, PURPOSE_REFERENCE_ID)).thenReturn(purposeRef);

    org.openbravo.model.ad.domain.List listItem = mock(
        org.openbravo.model.ad.domain.List.class);
    when(listItem.getSearchKey()).thenReturn("validPurpose");

    List<org.openbravo.model.ad.domain.List> listItems = new ArrayList<>();
    listItems.add(listItem);
    when(purposeRef.getADListList()).thenReturn(listItems);

    // Should not throw
    invokeVerifyParameters();
  }

  @Test
  public void testVerifyParametersThrowsWhenPublicKeyFileIsEmpty() throws Exception {
    setField(task, "purpose", "purpose", "validPurpose");
    setField(task, "publicKey", "publicKey", null);

    // Create a temporary empty file
    File emptyFile = File.createTempFile("emptykey", ".pub");
    emptyFile.deleteOnExit();
    setField(task, "publicKeyFile", "publicKeyFile", emptyFile);

    when(obDal.get(Reference.class, PURPOSE_REFERENCE_ID)).thenReturn(purposeRef);

    org.openbravo.model.ad.domain.List listItem = mock(
        org.openbravo.model.ad.domain.List.class);
    when(listItem.getSearchKey()).thenReturn("validPurpose");

    List<org.openbravo.model.ad.domain.List> listItems = new ArrayList<>();
    listItems.add(listItem);
    when(purposeRef.getADListList()).thenReturn(listItems);

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for empty key file");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
      assertEquals(true, e.getCause().getMessage().contains("is empty"));
    }
  }

  @Test
  public void testVerifyParametersThrowsWhenPublicKeyFileDoesNotExist() throws Exception {
    setField(task, "purpose", "purpose", "validPurpose");
    setField(task, "publicKey", "publicKey", null);
    setField(task, "publicKeyFile", "publicKeyFile",
        new File("/tmp/nonexistent_key_file_12345.pub"));

    when(obDal.get(Reference.class, PURPOSE_REFERENCE_ID)).thenReturn(purposeRef);

    org.openbravo.model.ad.domain.List listItem = mock(
        org.openbravo.model.ad.domain.List.class);
    when(listItem.getSearchKey()).thenReturn("validPurpose");

    List<org.openbravo.model.ad.domain.List> listItems = new ArrayList<>();
    listItems.add(listItem);
    when(purposeRef.getADListList()).thenReturn(listItems);

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for nonexistent key file");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
    }
  }

  // --- Helper methods ---

  private void invokeVerifyParameters() throws Exception {
    Method method = ActivationTask.class.getDeclaredMethod("verifyParameters");
    method.setAccessible(true);
    method.invoke(task);
  }

  private void setField(Object target, String fieldName, String declaredFieldName, Object value)
      throws Exception {
    Field field = findField(target.getClass(), declaredFieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private void setField(Object target, String fieldName, Class<?> declaringClass, Object value)
      throws Exception {
    Field field = declaringClass.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }

  @SuppressWarnings("unchecked")
  private <T> T getFieldValue(Object target, String fieldName) {
    try {
      Field field = findField(target.getClass(), fieldName);
      field.setAccessible(true);
      return (T) field.get(target);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
