package org.openbravo.dal.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
/** Tests for {@link EntityNotFoundException}. */

@RunWith(MockitoJUnitRunner.Silent.class)
public class EntityNotFoundExceptionTest {

  private static final String ENTITY_NOT_FOUND = "Entity not found";
  /** Default constructor. */

  @Test
  public void testDefaultConstructor() {
    EntityNotFoundException ex = new EntityNotFoundException();
    assertNull(ex.getMessage());
    assertNull(ex.getCause());
  }
  /** Message constructor. */

  @Test
  public void testMessageConstructor() {
    EntityNotFoundException ex = new EntityNotFoundException(ENTITY_NOT_FOUND);
    assertEquals(ENTITY_NOT_FOUND, ex.getMessage());
    assertNull(ex.getCause());
  }
  /** Cause constructor. */

  @Test
  public void testCauseConstructor() {
    RuntimeException cause = new RuntimeException("root cause");
    EntityNotFoundException ex = new EntityNotFoundException(cause);
    assertSame(cause, ex.getCause());
  }
  /** Message and cause constructor. */

  @Test
  public void testMessageAndCauseConstructor() {
    RuntimeException cause = new RuntimeException("root cause");
    EntityNotFoundException ex = new EntityNotFoundException(ENTITY_NOT_FOUND, cause);
    assertEquals(ENTITY_NOT_FOUND, ex.getMessage());
    assertSame(cause, ex.getCause());
  }
  /** Extends ob exception. */

  @Test
  public void testExtendsOBException() {
    EntityNotFoundException ex = new EntityNotFoundException("test");
    assertTrue(ex instanceof OBException);
  }
}
