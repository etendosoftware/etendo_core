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
public class EntityXMLExceptionTest {

  @Test
  public void testDefaultConstructor() {
    EntityXMLException ex = new EntityXMLException();
    assertNull(ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  public void testMessageConstructor() {
    EntityXMLException ex = new EntityXMLException("XML error");
    assertEquals("XML error", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  public void testCauseConstructor() {
    RuntimeException cause = new RuntimeException("root cause");
    EntityXMLException ex = new EntityXMLException(cause);
    assertSame(cause, ex.getCause());
  }

  @Test
  public void testMessageAndCauseConstructor() {
    RuntimeException cause = new RuntimeException("root cause");
    EntityXMLException ex = new EntityXMLException("XML error", cause);
    assertEquals("XML error", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  @Test
  public void testExtendsOBException() {
    EntityXMLException ex = new EntityXMLException("test");
    assertTrue(ex instanceof OBException);
  }
}
