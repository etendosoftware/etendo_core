package org.openbravo.scheduling.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Sets up the test environment before each test.
 * Configures mock behavior for database connections and statements.
 *
 * @throws SQLException if database mock setup fails
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenbravoOracleJDBCDelegateTest {

  @InjectMocks
  private OpenbravoOracleJDBCDelegate delegate;

  @Mock
  private Connection mockConnection;

  @Mock
  private PreparedStatement mockPreparedStatement;

  @Mock
  private ResultSet mockResultSet;

  /**
   * Sets up the test environment before each test.
   * Configures mock behavior for database connections and statements.
   *
   * @throws SQLException
   *     if database mock setup fails
   */
  @Before
  public void setUp() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
  }

  /**
   * Tests the setBoolean method to ensure proper conversion to 'Y'/'N' format.
   *
   * @throws SQLException
   *     if the database operation fails
   */
  @Test
  public void testSetBoolean() throws SQLException {
    // GIVEN
    boolean value = true;

    // WHEN
    delegate.setBoolean(mockPreparedStatement, 1, value);

    // THEN
    verify(mockPreparedStatement).setString(1, "Y");
  }

  /**
   * Tests the getBoolean method using column name to ensure proper conversion from 'Y'/'N' format.
   *
   * @throws SQLException
   *     if the database operation fails
   */
  @Test
  public void testGetBooleanByColumnName() throws SQLException {
    // GIVEN
    when(mockResultSet.getString("columnName")).thenReturn("Y");

    // WHEN
    boolean result = delegate.getBoolean(mockResultSet, "columnName");

    // THEN
    assertTrue(result);
  }

  /**
   * Tests the getBoolean method using column index to ensure proper conversion from 'Y'/'N' format.
   *
   * @throws SQLException
   *     if the database operation fails
   */
  @Test
  public void testGetBooleanByColumnIndex() throws SQLException {
    // GIVEN
    when(mockResultSet.getString(1)).thenReturn("N");

    // WHEN
    boolean result = delegate.getBoolean(mockResultSet, 1);

    // THEN
    assertFalse(result);
  }

  /**
   * Tests updating the scheduler status with instance ID, check-in time and status.
   *
   * @throws SQLException
   *     if the database update operation fails
   */
  @Test
  public void testUpdateSchedulerStatus() throws SQLException {
    // GIVEN
    String instanceId = "instance1";
    long checkInTime = System.currentTimeMillis();
    String status = "STARTED";

    when(mockPreparedStatement.executeUpdate()).thenReturn(1);

    // WHEN
    int result = delegate.updateSchedulerStatus(mockConnection, instanceId, checkInTime, status);

    // THEN
    assertEquals(1, result);
    verify(mockPreparedStatement).setLong(1, checkInTime);
    verify(mockPreparedStatement).setString(2, status);
    verify(mockPreparedStatement).setString(3, instanceId);
  }

  /**
   * Tests checking if any schedulers are in STARTED status.
   *
   * @throws SQLException
   *     if the database query operation fails
   */
  @Test
  public void testSchedulersStarted() throws SQLException {
    // GIVEN
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt(1)).thenReturn(1);

    // WHEN
    boolean result = delegate.schedulersStarted(mockConnection);

    // THEN
    assertTrue(result);
    verify(mockPreparedStatement).setString(1, "STARTED");
  }

}
