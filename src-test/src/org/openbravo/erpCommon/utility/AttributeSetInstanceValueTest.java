package org.openbravo.erpCommon.utility;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link AttributeSetInstanceValue}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AttributeSetInstanceValueTest {

  private AttributeSetInstanceValue instance;

  @Before
  public void setUp() {
    instance = new AttributeSetInstanceValue();
  }

  @Test
  public void testDefaultConstructorFieldsAreEmpty() {
    assertEquals("", instance.getLot());
    assertEquals("", instance.getSerialNumber());
    assertEquals("", instance.getGuaranteeDate());
    assertEquals("N", instance.getLocked());
    assertEquals("", instance.getLockDescription());
    assertEquals("", instance.getAttSetInstanceId());
  }

  @Test
  public void testParameterizedConstructor() {
    // Act
    AttributeSetInstanceValue val = new AttributeSetInstanceValue(
        "LOT01", "SER01", "2026-01-01", "Y", "Locked for testing");

    // Assert
    assertEquals("LOT01", val.getLot());
    assertEquals("SER01", val.getSerialNumber());
    assertEquals("2026-01-01", val.getGuaranteeDate());
    assertEquals("Y", val.getLocked());
    assertEquals("Locked for testing", val.getLockDescription());
  }

  @Test
  public void testParameterizedConstructorWithNullValues() {
    // Act
    AttributeSetInstanceValue val = new AttributeSetInstanceValue(
        null, null, null, null, null);

    // Assert - nulls should be converted to empty strings
    assertEquals("", val.getLot());
    assertEquals("", val.getSerialNumber());
    assertEquals("", val.getGuaranteeDate());
    assertEquals("", val.getLocked());
    assertEquals("", val.getLockDescription());
  }

  @Test
  public void testSetAndGetLot() {
    instance.setLot("LOT123");
    assertEquals("LOT123", instance.getLot());
  }

  @Test
  public void testSetLotWithNull() {
    instance.setLot(null);
    assertEquals("", instance.getLot());
  }

  @Test
  public void testSetAndGetSerialNumber() {
    instance.setSerialNumber("SN456");
    assertEquals("SN456", instance.getSerialNumber());
  }

  @Test
  public void testSetSerialNumberWithNull() {
    instance.setSerialNumber(null);
    assertEquals("", instance.getSerialNumber());
  }

  @Test
  public void testSetAndGetGuaranteeDate() {
    instance.setGuaranteeDate("2026-12-31");
    assertEquals("2026-12-31", instance.getGuaranteeDate());
  }

  @Test
  public void testSetGuaranteeDateWithNull() {
    instance.setGuaranteeDate(null);
    assertEquals("", instance.getGuaranteeDate());
  }

  @Test
  public void testSetAndGetLocked() {
    instance.setLocked("Y");
    assertEquals("Y", instance.getLocked());
  }

  @Test
  public void testSetLockedWithNull() {
    instance.setLocked(null);
    assertEquals("", instance.getLocked());
  }

  @Test
  public void testSetAndGetLockDescription() {
    instance.setLockDescription("Test lock");
    assertEquals("Test lock", instance.getLockDescription());
  }

  @Test
  public void testSetLockDescriptionWithNull() {
    instance.setLockDescription(null);
    assertEquals("", instance.getLockDescription());
  }

  @Test
  public void testReplaceRemovesSpecialCharacters() throws Exception {
    // Arrange
    Method replaceMethod = AttributeSetInstanceValue.class.getDeclaredMethod("replace", String.class);
    replaceMethod.setAccessible(true);

    // Act & Assert
    assertEquals("test", replaceMethod.invoke(instance, "test"));
    assertEquals("test", replaceMethod.invoke(instance, "te st"));
    assertEquals("test", replaceMethod.invoke(instance, "te&st"));
    assertEquals("test", replaceMethod.invoke(instance, "te,st"));
    assertEquals("test", replaceMethod.invoke(instance, "te#st"));
    assertEquals("test", replaceMethod.invoke(instance, "te(st)"));
  }

  @Test
  public void testReplaceRemovesMultipleSpecialCharacters() throws Exception {
    // Arrange
    Method replaceMethod = AttributeSetInstanceValue.class.getDeclaredMethod("replace", String.class);
    replaceMethod.setAccessible(true);

    // Act
    String result = (String) replaceMethod.invoke(instance, "a #b&c (d,e)");

    // Assert
    assertEquals("abcde", result);
  }

  @Test
  public void testGetDescriptionWithNullData() throws Exception {
    // Arrange
    Method getDescription = AttributeSetInstanceValue.class.getDeclaredMethod("getDescription",
        org.openbravo.database.ConnectionProvider.class,
        org.openbravo.base.secureApp.VariablesSecureApp.class,
        AttributeSetInstanceValueData[].class,
        String.class, String.class, java.util.Map.class);
    getDescription.setAccessible(true);

    // Act
    String result = (String) getDescription.invoke(instance, null, null, null, "N", "100", new java.util.HashMap<>());

    // Assert
    assertEquals("", result);
  }

  @Test
  public void testGetDescriptionWithEmptyData() throws Exception {
    // Arrange
    Method getDescription = AttributeSetInstanceValue.class.getDeclaredMethod("getDescription",
        org.openbravo.database.ConnectionProvider.class,
        org.openbravo.base.secureApp.VariablesSecureApp.class,
        AttributeSetInstanceValueData[].class,
        String.class, String.class, java.util.Map.class);
    getDescription.setAccessible(true);

    AttributeSetInstanceValueData[] emptyData = new AttributeSetInstanceValueData[0];

    // Act
    String result = (String) getDescription.invoke(instance, null, null, emptyData, "N", "100", new java.util.HashMap<>());

    // Assert
    assertEquals("", result);
  }
}
