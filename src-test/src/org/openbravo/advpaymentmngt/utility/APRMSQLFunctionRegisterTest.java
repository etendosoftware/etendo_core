package org.openbravo.advpaymentmngt.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.dialect.function.SQLFunction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Unit tests for {@link APRMSQLFunctionRegister}.
 */
@SuppressWarnings({"java:S112", "java:S1191"})
@RunWith(MockitoJUnitRunner.class)
public class APRMSQLFunctionRegisterTest {

  private APRMSQLFunctionRegister instance;
  private String originalRDBMS;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(APRMSQLFunctionRegister.class);
    // Save original value
    Field rdbmsField = APRMSQLFunctionRegister.class.getDeclaredField("RDBMS");
    rdbmsField.setAccessible(true);
    originalRDBMS = (String) rdbmsField.get(null);
  }
  /**
   * Tears down test fixtures.
   * @throws Exception if an error occurs
   */

  @After
  public void tearDown() throws Exception {
    // Restore original RDBMS value
    if (originalRDBMS != null) {
      setRDBMS(originalRDBMS);
    }
  }

  private void setRDBMS(String rdbms) throws Exception {
    Field rdbmsField = APRMSQLFunctionRegister.class.getDeclaredField("RDBMS");
    rdbmsField.setAccessible(true);

    // Use sun.misc.Unsafe to set the final static field on JDK 21
    Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    unsafeField.setAccessible(true);
    sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

    Object base = unsafe.staticFieldBase(rdbmsField);
    long offset = unsafe.staticFieldOffset(rdbmsField);
    unsafe.putObject(base, offset, rdbms);
  }
  /**
   * Get sql functions returns expected functions.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSQLFunctionsReturnsExpectedFunctions() throws Exception {
    setRDBMS("POSTGRE");

    Map<String, SQLFunction> functions = instance.getSQLFunctions();

    assertNotNull(functions);
    assertEquals(2, functions.size());
    assertTrue(functions.containsKey("ad_message_get2"));
    assertTrue(functions.containsKey("hqlagg"));
  }
  /**
   * Get aggregation sql for oracle.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetAggregationSQLForOracle() throws Exception {
    setRDBMS("ORACLE");

    Method method = APRMSQLFunctionRegister.class.getDeclaredMethod("getAggregationSQL");
    method.setAccessible(true);
    String result = (String) method.invoke(instance);

    assertEquals("listagg(to_char(?1), ',') WITHIN GROUP (ORDER BY ?1)", result);
  }
  /**
   * Get aggregation sql for postgres.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetAggregationSQLForPostgres() throws Exception {
    setRDBMS("POSTGRE");

    Method method = APRMSQLFunctionRegister.class.getDeclaredMethod("getAggregationSQL");
    method.setAccessible(true);
    String result = (String) method.invoke(instance);

    assertEquals("array_to_string(array_agg(?1), ',')", result);
  }
}
