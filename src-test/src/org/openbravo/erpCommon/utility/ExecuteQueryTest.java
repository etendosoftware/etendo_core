package org.openbravo.erpCommon.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
@RunWith(MockitoJUnitRunner.class)
public class ExecuteQueryTest {

  @Mock
  private ConnectionProvider mockConn;

  // --- Tests for constructor ---

  @Test
  public void testDefaultConstructorCreatesInstance() {
    ExecuteQuery query = new ExecuteQuery();
    assertNotNull(query);
  }

  @Test
  public void testParameterizedConstructorSetsFields() throws Exception {
    Vector<String> params = new Vector<>();
    params.add("param1");
    ExecuteQuery query = new ExecuteQuery(mockConn, "SELECT 1", params);
    assertNotNull(query);
  }

  @Test(expected = Exception.class)
  public void testConstructorThrowsWhenPoolIsNull() throws Exception {
    new ExecuteQuery(null, "SELECT 1", new Vector<>());
  }

  @Test
  public void testConstructorAcceptsNullSQL() throws Exception {
    ExecuteQuery query = new ExecuteQuery(mockConn, null, new Vector<>());
    // setSQL converts null to ""
    String sql = invokeGetSQL(query);
    assertEquals("", sql);
  }

  @Test
  public void testConstructorStoresSQL() throws Exception {
    ExecuteQuery query = new ExecuteQuery(mockConn, "SELECT * FROM dual", new Vector<>());
    String sql = invokeGetSQL(query);
    assertEquals("SELECT * FROM dual", sql);
  }

  // --- Tests for getParameter (via reflection) ---

  @Test
  public void testGetParameterReturnsEmptyForNullParameters() throws Exception {
    ExecuteQuery query = new ExecuteQuery(mockConn, "SELECT 1", null);
    String result = invokeGetParameter(query, 0);
    assertEquals("", result);
  }

  @Test
  public void testGetParameterReturnsValueAtValidPosition() throws Exception {
    Vector<String> params = new Vector<>();
    params.add("first");
    params.add("second");
    ExecuteQuery query = new ExecuteQuery(mockConn, "SELECT 1", params);
    String result = invokeGetParameter(query, 0);
    assertEquals("first", result);
  }

  @Test
  public void testGetParameterReturnsSecondValue() throws Exception {
    Vector<String> params = new Vector<>();
    params.add("first");
    params.add("second");
    ExecuteQuery query = new ExecuteQuery(mockConn, "SELECT 1", params);
    String result = invokeGetParameter(query, 1);
    assertEquals("second", result);
  }

  @Test
  public void testGetParameterReturnsEmptyForOutOfBoundsPosition() throws Exception {
    Vector<String> params = new Vector<>();
    params.add("only");
    ExecuteQuery query = new ExecuteQuery(mockConn, "SELECT 1", params);
    // position 5 > size 1, should return ""
    String result = invokeGetParameter(query, 5);
    assertEquals("", result);
  }

  // --- Helper methods ---

  private String invokeGetSQL(ExecuteQuery query) throws Exception {
    Method method = ExecuteQuery.class.getDeclaredMethod("getSQL");
    method.setAccessible(true);
    return (String) method.invoke(query);
  }

  private String invokeGetParameter(ExecuteQuery query, int position) throws Exception {
    Method method = ExecuteQuery.class.getDeclaredMethod("getParameter", int.class);
    method.setAccessible(true);
    return (String) method.invoke(query, position);
  }
}
