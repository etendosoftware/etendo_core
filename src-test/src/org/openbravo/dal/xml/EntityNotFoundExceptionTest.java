package org.openbravo.dal.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;

@RunWith(MockitoJUnitRunner.Silent.class)
public class EntityNotFoundExceptionTest {

  @Test
  public void testDefaultConstructor() {
    EntityNotFoundException ex = new EntityNotFoundException();
    assertNull(ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  public void testMessageConstructor() {
    EntityNotFoundException ex = new EntityNotFoundException("Entity not found");
    assertEquals("Entity not found", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  public void testCauseConstructor() {
    RuntimeException cause = new RuntimeException("root cause");
    EntityNotFoundException ex = new EntityNotFoundException(cause);
    assertSame(cause, ex.getCause());
  }

  @Test
  public void testMessageAndCauseConstructor() {
    RuntimeException cause = new RuntimeException("root cause");
    EntityNotFoundException ex = new EntityNotFoundException("Entity not found", cause);
    assertEquals("Entity not found", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  @Test
  public void testExtendsOBException() {
    EntityNotFoundException ex = new EntityNotFoundException("test");
    assertTrue(ex instanceof OBException);
  }
}
