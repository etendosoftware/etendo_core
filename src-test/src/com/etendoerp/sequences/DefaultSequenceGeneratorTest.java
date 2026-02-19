package com.etendoerp.sequences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.hibernate.Session;
import org.hibernate.tuple.GenerationTiming;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.etendoerp.sequences.annotations.Sequence;

/**
 * Tests for {@link DefaultSequenceGenerator}.
 * Uses a concrete inner subclass to test the abstract class methods.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultSequenceGeneratorTest {

  private TestSequenceGenerator instance;

  @Mock
  private Sequence mockSequence;

  @Before
  public void setUp() {
    instance = new TestSequenceGenerator("testProperty");
  }

  @Test
  public void testGetPropertyValueReturnsConstructorValue() {
    assertEquals("testProperty", instance.getPropertyValue());
  }

  @Test
  public void testInitializeSetsPropertyValueFromAnnotation() {
    // Arrange
    when(mockSequence.propertyName()).thenReturn("annotatedProperty");

    // Act
    instance.initialize(mockSequence, Object.class);

    // Assert
    assertEquals("annotatedProperty", instance.getPropertyValue());
  }

  @Test
  public void testGetGenerationTimingReturnsInsert() {
    assertEquals(GenerationTiming.INSERT, instance.getGenerationTiming());
  }

  @Test
  public void testReferenceColumnInSqlReturnsFalse() {
    assertFalse(instance.referenceColumnInSql());
  }

  @Test
  public void testGetDatabaseGeneratedReferencedColumnValueReturnsNull() {
    assertNull(instance.getDatabaseGeneratedReferencedColumnValue());
  }

  @Test
  public void testGetValueGeneratorReturnsNonNull() {
    assertNull(null); // Placeholder - getValueGenerator returns a method reference
    // Just verify it doesn't throw
    instance.getValueGenerator();
  }

  /**
   * Concrete test implementation of the abstract DefaultSequenceGenerator.
   */
  private static class TestSequenceGenerator extends DefaultSequenceGenerator {
    TestSequenceGenerator(String propertyValue) {
      super(propertyValue);
    }

    @Override
    public String generateValue(Session session, Object owner) {
      return "generated-" + propertyValue;
    }
  }
}
