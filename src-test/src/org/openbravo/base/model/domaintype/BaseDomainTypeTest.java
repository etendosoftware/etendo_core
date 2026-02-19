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

  @Before
  public void setUp() {
    instance = new BaseDomainType() {
      @Override
      public void checkIsValidValue(org.openbravo.base.model.Property property, Object value)
          throws org.openbravo.base.validation.ValidationException {
        // no-op for testing
      }
    };
  }

  @Test
  public void testInitializeDoesNotThrow() {
    instance.initialize();
  }

  @Test
  public void testSetAndGetReference() {
    Reference ref = mock(Reference.class);
    instance.setReference(ref);
    assertEquals(ref, instance.getReference());
  }

  @Test
  public void testGetReferenceDefaultNull() {
    assertNull(instance.getReference());
  }

  @Test
  public void testSetAndGetModelProvider() {
    ModelProvider provider = mock(ModelProvider.class);
    instance.setModelProvider(provider);
    assertEquals(provider, instance.getModelProvider());
  }

  @Test
  public void testGetModelProviderDefaultNull() {
    assertNull(instance.getModelProvider());
  }

  @Test
  public void testGetClassesReturnsEmptyList() {
    List<Class<?>> classes = instance.getClasses();
    assertNotNull(classes);
    assertTrue(classes.isEmpty());
  }

  @Test
  public void testGetClassesReturnsMutableList() {
    List<Class<?>> classes = instance.getClasses();
    classes.add(String.class);
    assertEquals(1, classes.size());
  }

  @Test
  public void testCheckObjectIsValid() throws Exception {
    // Create a concrete subclass that tracks calls
    final boolean[] called = { false };
    BaseDomainType tracker = new BaseDomainType() {
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
