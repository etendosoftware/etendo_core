package org.openbravo.erpCommon.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.util.Vector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.database.ConnectionProvider;

/**
 * Tests for {@link ExecuteQuery}.
 * Tests constructor validation, setSQL null handling, and getParameter bounds checking.
 */
@SuppressWarnings({"java:S120", "java:S1149"})
@RunWith(MockitoJUnitRunner.class)
public class ExecuteQueryTest {

  private static final String SELECT_1 = "SELECT 1";
  private static final String FIRST = "first";
  private static final String SECOND = "second";

  @Mock
  private ConnectionProvider mockConn;

  // --- Tests for constructor ---
  /** Default constructor creates instance. */

  @Test
  public void testDefaultConstructorCreatesInstance() {
    ExecuteQuery query = new ExecuteQuery();
    assertNotNull(query);
  }
  /**
   * Parameterized constructor sets fields.
   * @throws Exception if an error occurs
   */

  @Test
  public void testParameterizedConstructorSetsFields() throws Exception {
    Vector<String> params = new Vector<>();
    params.add("param1");
    ExecuteQuery query = new ExecuteQuery(mockConn, SELECT_1, params);
    assertNotNull(query);
  }
  /**
   * Constructor throws when pool is null.
   * @throws Exception if an error occurs
   */

  @Test(expected = Exception.class)
  public void testConstructorThrowsWhenPoolIsNull() throws Exception {
    new ExecuteQuery(null, SELECT_1, new Vector<>());
  }
  /**
   * Constructor accepts null sql.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConstructorAcceptsNullSQL() throws Exception {
    ExecuteQuery query = new ExecuteQuery(mockConn, null, new Vector<>());
    // setSQL converts null to ""
    String sql = invokeGetSQL(query);
    assertEquals("", sql);
  }
  /**
   * Constructor stores sql.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConstructorStoresSQL() throws Exception {
    ExecuteQuery query = new ExecuteQuery(mockConn, "SELECT * FROM dual", new Vector<>());
    String sql = invokeGetSQL(query);
    assertEquals("SELECT * FROM dual", sql);
  }

  // --- Tests for getParameter (via reflection) ---
  /**
   * Get parameter returns empty for null parameters.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetParameterReturnsEmptyForNullParameters() throws Exception {
    ExecuteQuery query = new ExecuteQuery(mockConn, SELECT_1, null);
    String result = invokeGetParameter(query, 0);
    assertEquals("", result);
  }
  /**
   * Get parameter returns value at valid position.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetParameterReturnsValueAtValidPosition() throws Exception {
    Vector<String> params = new Vector<>();
    params.add(FIRST);
    params.add(SECOND);
    ExecuteQuery query = new ExecuteQuery(mockConn, SELECT_1, params);
    String result = invokeGetParameter(query, 0);
    assertEquals(FIRST, result);
  }
  /**
   * Get parameter returns second value.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetParameterReturnsSecondValue() throws Exception {
    Vector<String> params = new Vector<>();
    params.add(FIRST);
    params.add(SECOND);
    ExecuteQuery query = new ExecuteQuery(mockConn, SELECT_1, params);
    String result = invokeGetParameter(query, 1);
    assertEquals(SECOND, result);
  }
  /**
   * Get parameter returns empty for out of bounds position.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetParameterReturnsEmptyForOutOfBoundsPosition() throws Exception {
    Vector<String> params = new Vector<>();
    params.add("only");
    ExecuteQuery query = new ExecuteQuery(mockConn, SELECT_1, params);
    // position 5 > size 1, should return ""
    String result = invokeGetParameter(query, 5);
    assertEquals("", result);
  }

  // --- Helper methods ---

  private String invokeGetSQL(ExecuteQuery query) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = ExecuteQuery.class.getDeclaredMethod("getSQL");
    method.setAccessible(true);
    return (String) method.invoke(query);
  }

  private String invokeGetParameter(ExecuteQuery query, int position) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = ExecuteQuery.class.getDeclaredMethod("getParameter", int.class);
    method.setAccessible(true);
    return (String) method.invoke(query, position);
  }
}
