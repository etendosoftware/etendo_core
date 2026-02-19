package org.openbravo.dal.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
/** Tests for {@link EntityXMLException}. */

@RunWith(MockitoJUnitRunner.Silent.class)
public class EntityXMLExceptionTest {

  private static final String XML_ERROR = "XML error";
  /** Default constructor. */

  @Test
  public void testDefaultConstructor() {
    EntityXMLException ex = new EntityXMLException();
    assertNull(ex.getMessage());
    assertNull(ex.getCause());
  }
  /** Message constructor. */

  @Test
  public void testMessageConstructor() {
    EntityXMLException ex = new EntityXMLException(XML_ERROR);
    assertEquals(XML_ERROR, ex.getMessage());
    assertNull(ex.getCause());
  }
  /** Cause constructor. */

  @Test
  public void testCauseConstructor() {
    RuntimeException cause = new RuntimeException("root cause");
    EntityXMLException ex = new EntityXMLException(cause);
    assertSame(cause, ex.getCause());
  }
  /** Message and cause constructor. */

  @Test
  public void testMessageAndCauseConstructor() {
    RuntimeException cause = new RuntimeException("root cause");
    EntityXMLException ex = new EntityXMLException(XML_ERROR, cause);
    assertEquals(XML_ERROR, ex.getMessage());
    assertSame(cause, ex.getCause());
  }
  /** Extends ob exception. */

  @Test
  public void testExtendsOBException() {
    EntityXMLException ex = new EntityXMLException("test");
    assertTrue(ex instanceof OBException);
  }
}
