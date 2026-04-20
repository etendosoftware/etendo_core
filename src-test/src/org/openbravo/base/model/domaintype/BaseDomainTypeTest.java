package org.openbravo.base.model.domaintype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Reference;

/**
 * Tests for {@link BaseDomainType}.
 * Uses a concrete anonymous subclass since BaseDomainType is abstract.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseDomainTypeTest {

  private BaseDomainType instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new BaseDomainType() {
      /**
       * Check is valid value.
       * @throws ValidationException if an error occurs
       */
      @Override
      public void checkIsValidValue(org.openbravo.base.model.Property property, Object value)
          throws org.openbravo.base.validation.ValidationException {
        // no-op for testing
      }
    };
  }
  /** Initialize does not throw. */

  @Test
  public void testInitializeDoesNotThrow() {
    instance.initialize();
  }
  /** Set and get reference. */

  @Test
  public void testSetAndGetReference() {
    Reference ref = mock(Reference.class);
    instance.setReference(ref);
    assertEquals(ref, instance.getReference());
  }
  /** Get reference default null. */

  @Test
  public void testGetReferenceDefaultNull() {
    assertNull(instance.getReference());
  }
  /** Set and get model provider. */

  @Test
  public void testSetAndGetModelProvider() {
    ModelProvider provider = mock(ModelProvider.class);
    instance.setModelProvider(provider);
    assertEquals(provider, instance.getModelProvider());
  }
  /** Get model provider default null. */

  @Test
  public void testGetModelProviderDefaultNull() {
    assertNull(instance.getModelProvider());
  }
  /** Get classes returns empty list. */

  @Test
  public void testGetClassesReturnsEmptyList() {
    List<Class<?>> classes = instance.getClasses();
    assertNotNull(classes);
    assertTrue(classes.isEmpty());
  }
  /** Get classes returns mutable list. */

  @Test
  public void testGetClassesReturnsMutableList() {
    List<Class<?>> classes = instance.getClasses();
    classes.add(String.class);
    assertEquals(1, classes.size());
  }
  /**
   * Check object is valid.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCheckObjectIsValid() throws Exception {
    // Create a concrete subclass that tracks calls
    final boolean[] called = { false };
    BaseDomainType tracker = new BaseDomainType() {
      /**
       * Check is valid value.
       * @throws ValidationException if an error occurs
       */
      @Override
      public void checkIsValidValue(org.openbravo.base.model.Property property, Object value)
          throws org.openbravo.base.validation.ValidationException {
        called[0] = true;
      }
    };

    org.openbravo.base.model.Property mockProperty = mock(org.openbravo.base.model.Property.class);
    when(mockProperty.getName()).thenReturn("testProp");

    org.openbravo.base.model.BaseOBObjectDef mockObj = mock(
        org.openbravo.base.model.BaseOBObjectDef.class);
    when(mockObj.get("testProp")).thenReturn("someValue");

    tracker.checkObjectIsValid(mockObj, mockProperty);
    assertTrue(called[0]);
  }
}
