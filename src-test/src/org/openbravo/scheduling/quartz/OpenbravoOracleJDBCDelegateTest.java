package org.openbravo.scheduling.quartz;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.runner.RunWith;
import org.openbravo.scheduling.ClusterInstanceProcessAccess;

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

  @Mock
  private ClusterInstanceProcessAccess mockClusterInstanceProcessAccess;

  @Before
  public void setUp() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
  }

  @Test
  public void testSetBoolean() throws SQLException {
    // GIVEN
    boolean value = true;

    // WHEN
    delegate.setBoolean(mockPreparedStatement, 1, value);

    // THEN
    verify(mockPreparedStatement).setString(1, "Y");
  }

  @Test
  public void testGetBooleanByColumnName() throws SQLException {
    // GIVEN
    when(mockResultSet.getString("columnName")).thenReturn("Y");

    // WHEN
    boolean result = delegate.getBoolean(mockResultSet, "columnName");

    // THEN
    assertTrue(result);
  }

  @Test
  public void testGetBooleanByColumnIndex() throws SQLException {
    // GIVEN
    when(mockResultSet.getString(1)).thenReturn("N");

    // WHEN
    boolean result = delegate.getBoolean(mockResultSet, 1);

    // THEN
    assertFalse(result);
  }

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
