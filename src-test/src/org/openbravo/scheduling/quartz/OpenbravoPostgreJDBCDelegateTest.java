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
 * Unit tests for OpenbravoPostgreJDBCDelegate class.
 * Tests database operations related to scheduler and boolean value handling.
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenbravoPostgreJDBCDelegateTest {

  @InjectMocks
  private OpenbravoPostgreJDBCDelegate delegate;

  @Mock
  private Connection mockConnection;

  @Mock
  private PreparedStatement mockPreparedStatement;

  @Mock
  private ResultSet mockResultSet;

  /**
   * Sets up the test environment before each test.
   * Configures mock behavior for database connections.
   *
   * @throws SQLException
   *     if database operations fail
   */
  @Before
  public void setUp() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
  }

  /**
   * Tests setting boolean values in prepared statements.
   * Verifies that true is converted to "Y" string.
   *
   * @throws SQLException
   *     if setting the boolean value fails
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
   * Tests retrieving boolean values by column name.
   * Verifies that "Y" string is converted to true.
   *
   * @throws SQLException
   *     if retrieving the boolean value fails
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
   * Tests retrieving boolean values by column index.
   * Verifies that "N" string is converted to false.
   *
   * @throws SQLException
   *     if retrieving the boolean value fails
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
   * Tests updating scheduler status information.
   * Verifies correct parameter setting and execution of update statement.
   *
   * @throws SQLException
   *     if the update operation fails
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
    verify(mockConnection).prepareStatement(anyString());
    verify(mockPreparedStatement).setLong(1, checkInTime);
    verify(mockPreparedStatement).setString(2, status);
    verify(mockPreparedStatement).setString(3, instanceId);
    verify(mockPreparedStatement).executeUpdate();
  }

  /**
   * Tests checking started schedulers when there are active ones.
   * Verifies true is returned when count is greater than zero.
   *
   * @throws SQLException
   *     if querying scheduler status fails
   */
  @Test
  public void testSchedulersStartedWithStartedSchedulers() throws SQLException {
    // GIVEN
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt(1)).thenReturn(2);

    // WHEN
    boolean result = delegate.schedulersStarted(mockConnection);

    // THEN
    assertTrue(result);
    verify(mockConnection).prepareStatement(anyString());
    verify(mockPreparedStatement).setString(1, OpenbravoJDBCDelegate.SCHEDULER_STATUS_STARTED);
    verify(mockPreparedStatement).executeQuery();
    verify(mockResultSet).next();
    verify(mockResultSet).getInt(1);
  }

  /**
   * Tests checking started schedulers when there are none active.
   * Verifies false is returned when count is zero.
   *
   * @throws SQLException
   *     if querying scheduler status fails
   */
  @Test
  public void testSchedulersStartedWithNoStartedSchedulers() throws SQLException {
    // GIVEN
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getInt(1)).thenReturn(0);

    // WHEN
    boolean result = delegate.schedulersStarted(mockConnection);

    // THEN
    assertFalse(result);
    verify(mockConnection).prepareStatement(anyString());
    verify(mockPreparedStatement).setString(1, OpenbravoJDBCDelegate.SCHEDULER_STATUS_STARTED);
    verify(mockPreparedStatement).executeQuery();
    verify(mockResultSet).next();
    verify(mockResultSet).getInt(1);
  }

  /**
   * Tests scheduler status check with no results.
   * Verifies SQLException is thrown when no results are returned.
   *
   * @throws SQLException
   *     expected to be thrown when no results are found
   */
  @Test(expected = SQLException.class)
  public void testSchedulersStartedNoResults() throws SQLException {
    // GIVEN
    when(mockResultSet.next()).thenReturn(false);

    // WHEN
    delegate.schedulersStarted(mockConnection);

    // THEN - should throw SQLException
  }
}
