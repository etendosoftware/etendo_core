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
@SuppressWarnings({"java:S1075", "java:S120", "java:S112"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class ActivationTaskTest {

  private static final String PUBLIC_KEY = "publicKey";
  private static final String PUBLIC_KEY_FILE = "publicKeyFile";
  private static final String PURPOSE = "purpose";
  private static final String SOME_KEY = "someKey";
  private static final String VALID_PURPOSE = "validPurpose";

  private static final String PURPOSE_REFERENCE_ID = "60E231391A7348DDA7171E780F62EF99";

  private MockedStatic<OBDal> obDalStatic;

  @Mock
  private OBDal obDal;

  @Mock
  private Reference purposeRef;

  private ActivationTask task;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

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
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }
  /** Set public key. */

  @Test
  public void testSetPublicKey() {
    task.setPublicKey("testKey123");
    String value = getFieldValue(task, PUBLIC_KEY);
    assertEquals("testKey123", value);
  }
  /** Set public key file. */

  @Test
  public void testSetPublicKeyFile() {
    File file = new File("/tmp/key.pub");
    task.setPublicKeyFile(file);
    File value = getFieldValue(task, PUBLIC_KEY_FILE);
    assertEquals(file, value);
  }
  /** Set purpose. */

  @Test
  public void testSetPurpose() {
    task.setPurpose("testPurpose");
    String value = getFieldValue(task, PURPOSE);
    assertEquals("testPurpose", value);
  }
  /**
   * Verify parameters throws when purpose is null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testVerifyParametersThrowsWhenPurposeIsNull() throws Exception {
    setField(task, PURPOSE, null);
    setField(task, PUBLIC_KEY, SOME_KEY);

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for null purpose");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
      assertEquals("purpose parameter is required", e.getCause().getMessage());
    }
  }
  /**
   * Verify parameters throws when purpose is empty.
   * @throws Exception if an error occurs
   */

  @Test
  public void testVerifyParametersThrowsWhenPurposeIsEmpty() throws Exception {
    setField(task, PURPOSE, "");
    setField(task, PUBLIC_KEY, SOME_KEY);

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for empty purpose");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
      assertEquals("purpose parameter is required", e.getCause().getMessage());
    }
  }
  /**
   * Verify parameters throws when purpose not found in reference.
   * @throws Exception if an error occurs
   */

  @Test
  public void testVerifyParametersThrowsWhenPurposeNotFoundInReference() throws Exception {
    setField(task, PURPOSE, "invalidPurpose");
    setField(task, PUBLIC_KEY, SOME_KEY);

    when(obDal.get(Reference.class, PURPOSE_REFERENCE_ID)).thenReturn(purposeRef);

    org.openbravo.model.ad.domain.List listItem = mock(
        org.openbravo.model.ad.domain.List.class);
    when(listItem.getSearchKey()).thenReturn(VALID_PURPOSE);
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
  /**
   * Verify parameters throws when no public key provided.
   * @throws Exception if an error occurs
   */

  @Test
  public void testVerifyParametersThrowsWhenNoPublicKeyProvided() throws Exception {
    setField(task, PURPOSE, VALID_PURPOSE);
    setField(task, PUBLIC_KEY, null);
    setField(task, PUBLIC_KEY_FILE, null);

    setupPurposeReferenceMock(VALID_PURPOSE);

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for missing public key");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
      assertEquals(true, e.getCause().getMessage().contains("Public key must be provided"));
    }
  }
  /**
   * Verify parameters throws when both public key and file provided.
   * @throws Exception if an error occurs
   */

  @Test
  public void testVerifyParametersThrowsWhenBothPublicKeyAndFileProvided() throws Exception {
    setField(task, PURPOSE, VALID_PURPOSE);
    setField(task, PUBLIC_KEY, "somePublicKey");
    setField(task, PUBLIC_KEY_FILE, new File("/tmp/notbase/key.pub"));

    setupPurposeReferenceMock(VALID_PURPOSE);

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for both publicKey and publicKeyFile set");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
      assertEquals(true, e.getCause().getMessage().contains("Only one of the publicKey"));
    }
  }
  /**
   * Verify parameters succeeds with valid purpose and public key.
   * @throws Exception if an error occurs
   */

  @Test
  public void testVerifyParametersSucceedsWithValidPurposeAndPublicKey() throws Exception {
    setField(task, PURPOSE, VALID_PURPOSE);
    setField(task, PUBLIC_KEY, "somePublicKey");
    setField(task, PUBLIC_KEY_FILE, null);

    setupPurposeReferenceMock(VALID_PURPOSE);

    // Should not throw
    invokeVerifyParameters();
  }
  /**
   * Verify parameters throws when public key file is empty.
   * @throws Exception if an error occurs
   */

  @Test
  public void testVerifyParametersThrowsWhenPublicKeyFileIsEmpty() throws Exception {
    setField(task, PURPOSE, VALID_PURPOSE);
    setField(task, PUBLIC_KEY, null);

    // Create a temporary empty file
    File emptyFile = File.createTempFile("emptykey", ".pub");
    emptyFile.deleteOnExit();
    setField(task, PUBLIC_KEY_FILE, emptyFile);

    setupPurposeReferenceMock(VALID_PURPOSE);

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for empty key file");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
      assertEquals(true, e.getCause().getMessage().contains("is empty"));
    }
  }
  /**
   * Verify parameters throws when public key file does not exist.
   * @throws Exception if an error occurs
   */

  @Test
  public void testVerifyParametersThrowsWhenPublicKeyFileDoesNotExist() throws Exception {
    setField(task, PURPOSE, VALID_PURPOSE);
    setField(task, PUBLIC_KEY, null);
    setField(task, PUBLIC_KEY_FILE,
        new File("/tmp/nonexistent_key_file_12345.pub"));

    setupPurposeReferenceMock(VALID_PURPOSE);

    try {
      invokeVerifyParameters();
      fail("Expected BuildException for nonexistent key file");
    } catch (InvocationTargetException e) {
      assertNotNull(e.getCause());
      assertEquals(BuildException.class, e.getCause().getClass());
    }
  }

  // --- Helper methods ---

  private void setupPurposeReferenceMock(String searchKey) {
    when(obDal.get(Reference.class, PURPOSE_REFERENCE_ID)).thenReturn(purposeRef);

    org.openbravo.model.ad.domain.List listItem = mock(
        org.openbravo.model.ad.domain.List.class);
    when(listItem.getSearchKey()).thenReturn(searchKey);

    List<org.openbravo.model.ad.domain.List> listItems = new ArrayList<>();
    listItems.add(listItem);
    when(purposeRef.getADListList()).thenReturn(listItems);
  }

  private void invokeVerifyParameters() throws Exception{
    Method method = ActivationTask.class.getDeclaredMethod("verifyParameters");
    method.setAccessible(true);
    method.invoke(task);
  }

  private void setField(Object target, String declaredFieldName, Object value)
      throws IllegalAccessException, NoSuchFieldException {
    Field field = findField(target.getClass(), declaredFieldName);
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
