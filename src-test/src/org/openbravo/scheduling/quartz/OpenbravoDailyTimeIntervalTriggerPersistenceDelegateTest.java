package org.openbravo.scheduling.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.SimplePropertiesTriggerProperties;
import org.quartz.impl.jdbcjobstore.TriggerPersistenceDelegate;
import org.quartz.impl.jdbcjobstore.Util;
import org.quartz.impl.triggers.DailyTimeIntervalTriggerImpl;
import org.quartz.spi.OperableTrigger;

/**
 * Test class for the OpenbravoDailyTimeIntervalTriggerPersistenceDelegate.
 * Tests CRUD operations for trigger persistence.
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenbravoDailyTimeIntervalTriggerPersistenceDelegateTest {


  /**
   * Test implementation of the delegate that allows mocking trigger properties.
   */
  private static class TestableDelegate extends OpenbravoDailyTimeIntervalTriggerPersistenceDelegate {
    private final SimplePropertiesTriggerProperties mockedProperties;

    public TestableDelegate(SimplePropertiesTriggerProperties props) {
      this.mockedProperties = props;
    }

    @Override
    protected SimplePropertiesTriggerProperties getTriggerProperties(OperableTrigger trigger) {
      return mockedProperties;
    }

    @Override
    protected TriggerPropertyBundle getTriggerPropertyBundle(SimplePropertiesTriggerProperties props) {
      TriggerPropertyBundle bundle = mock(TriggerPropertyBundle.class);
      return bundle;
    }
  }

  @Mock private Connection connection;
  @Mock private PreparedStatement preparedStatement;
  @Mock private ResultSet resultSet;
  @Mock private DailyTimeIntervalTriggerImpl trigger;
  @Mock private JobDetail jobDetail;
  @Mock private TriggerKey triggerKey;
  @Mock private SimplePropertiesTriggerProperties properties;

  private TestableDelegate delegate;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks and configures common behavior.
   */
  @Before
  public void setUp() throws SQLException {
    delegate = spy(new TestableDelegate(properties));

    when(connection.prepareStatement(isNull())).thenReturn(preparedStatement);

    when(preparedStatement.executeUpdate()).thenReturn(1);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);

    when(trigger.getKey()).thenReturn(triggerKey);
    when(triggerKey.getName()).thenReturn("testTriggerName");
    when(triggerKey.getGroup()).thenReturn("testTriggerGroup");

    configureMockProperties();
  }

  /**
   * Configures the mock properties with test values.
   */
  private void configureMockProperties() {
    when(properties.getString1()).thenReturn("string1");
    when(properties.getString2()).thenReturn("string2");
    when(properties.getString3()).thenReturn("string3");
    when(properties.getInt1()).thenReturn(1);
    when(properties.getInt2()).thenReturn(2);
    when(properties.getLong1()).thenReturn(3L);
    when(properties.getLong2()).thenReturn(4L);
    when(properties.getDecimal1()).thenReturn(new BigDecimal("5.5"));
    when(properties.getDecimal2()).thenReturn(new BigDecimal("6.6"));
    when(properties.isBoolean1()).thenReturn(true);
    when(properties.isBoolean2()).thenReturn(false);
  }

  /**
   * Sets up static mocks for JDBC support and utility classes.
   */
  private void setupStaticMocks(MockedStatic<OpenbravoJDBCPersistenceSupport> jdbcSupport,
      MockedStatic<Util> utilMock,
      String sqlTemplate) {
    jdbcSupport.when(() -> OpenbravoJDBCPersistenceSupport.setBooleanValue(any(), anyInt(), anyBoolean()))
        .thenAnswer(invocation -> null);
    jdbcSupport.when(() -> OpenbravoJDBCPersistenceSupport.getBooleanValue(any(), anyString()))
        .thenReturn(true);

    utilMock.when(() -> Util.rtp(anyString(), anyString(), anyString()))
        .thenReturn(sqlTemplate);
  }

  /**
   * Sets up result set values for testing.
   */
  private void setupResultSetValues() throws SQLException {
    when(resultSet.getString("STR_PROP_1")).thenReturn("string1");
    when(resultSet.getString("STR_PROP_2")).thenReturn("string2");
    when(resultSet.getString("STR_PROP_3")).thenReturn("string3");
    when(resultSet.getInt("INT_PROP_1")).thenReturn(1);
    when(resultSet.getInt("INT_PROP_2")).thenReturn(2);
    when(resultSet.getInt("LONG_PROP_1")).thenReturn(3);
    when(resultSet.getInt("LONG_PROP_2")).thenReturn(4);
    when(resultSet.getBigDecimal("DEC_PROP_1")).thenReturn(new BigDecimal("5.5"));
    when(resultSet.getBigDecimal("DEC_PROP_2")).thenReturn(new BigDecimal("6.6"));
  }


  /**
   * Tests the insertion of extended trigger properties.
   */
  @Test
  public void testInsertExtendedTriggerProperties() throws SQLException, IOException {
    try (MockedStatic<OpenbravoJDBCPersistenceSupport> jdbcSupport = mockStatic(OpenbravoJDBCPersistenceSupport.class);
         MockedStatic<Util> utilMock = mockStatic(Util.class)) {

      setupStaticMocks(jdbcSupport, utilMock, "INSERT_STMT");

      int result = delegate.insertExtendedTriggerProperties(connection, trigger, "WAITING", jobDetail);

      assertEquals("El método debería devolver 1 como número de filas afectadas", 1, result);
      verify(preparedStatement).setString(1, "testTriggerName");
      verify(preparedStatement).setString(2, "testTriggerGroup");
      verify(preparedStatement).setString(3, "string1");
      verify(preparedStatement).setBigDecimal(10, new BigDecimal("5.5"));

      jdbcSupport.verify(() -> OpenbravoJDBCPersistenceSupport.setBooleanValue(
          eq(preparedStatement), eq(12), eq(true)));
      jdbcSupport.verify(() -> OpenbravoJDBCPersistenceSupport.setBooleanValue(
          eq(preparedStatement), eq(13), eq(false)));

      utilMock.verify(() -> Util.closeStatement(preparedStatement));
    }
  }

  /**
   * Tests loading extended trigger properties.
   */
  @Test
  public void testLoadExtendedTriggerProperties() throws SQLException {
    try (MockedStatic<OpenbravoJDBCPersistenceSupport> jdbcSupport = mockStatic(OpenbravoJDBCPersistenceSupport.class);
         MockedStatic<Util> utilMock = mockStatic(Util.class)) {

      setupStaticMocks(jdbcSupport, utilMock, "SELECT_STMT");

      setupResultSetValues();

      TriggerPersistenceDelegate.TriggerPropertyBundle result =
          delegate.loadExtendedTriggerProperties(connection, triggerKey);

      verify(preparedStatement).setString(1, "testTriggerName");
      verify(preparedStatement).setString(2, "testTriggerGroup");

      jdbcSupport.verify(() -> OpenbravoJDBCPersistenceSupport.getBooleanValue(resultSet, "BOOL_PROP_1"));
      jdbcSupport.verify(() -> OpenbravoJDBCPersistenceSupport.getBooleanValue(resultSet, "BOOL_PROP_2"));

      utilMock.verify(() -> Util.closeResultSet(resultSet));
      utilMock.verify(() -> Util.closeStatement(preparedStatement));
    }
  }

  /**
   * Tests loading extended trigger properties when no record is found.
   */
  @Test
  public void testLoadExtendedTriggerPropertiesNoRecordFound() throws SQLException {
    try (MockedStatic<Util> utilMock = mockStatic(Util.class)) {
      utilMock.when(() -> Util.rtp(anyString(), anyString(), anyString()))
          .thenReturn("SELECT_STMT");

      when(resultSet.next()).thenReturn(false);

      try {
        delegate.loadExtendedTriggerProperties(connection, triggerKey);
        fail("Should throw IllegalStateException when no record is found");
      } catch (IllegalStateException e) {
      }
    }
  }


  @Test
  public void testUpdateExtendedTriggerProperties() throws SQLException, IOException {
    try (MockedStatic<OpenbravoJDBCPersistenceSupport> jdbcSupport = mockStatic(OpenbravoJDBCPersistenceSupport.class);
         MockedStatic<Util> utilMock = mockStatic(Util.class)) {

      setupStaticMocks(jdbcSupport, utilMock, "UPDATE_STMT");

      int result = delegate.updateExtendedTriggerProperties(connection, trigger, "WAITING", jobDetail);

      assertEquals("Method should return 1 as number of affected rows", 1, result);
      verify(preparedStatement).setString(1, "string1");
      verify(preparedStatement).setString(12, "testTriggerName");
      verify(preparedStatement).setString(13, "testTriggerGroup");

      jdbcSupport.verify(() -> OpenbravoJDBCPersistenceSupport.setBooleanValue(
          eq(preparedStatement), eq(10), eq(true)));
      jdbcSupport.verify(() -> OpenbravoJDBCPersistenceSupport.setBooleanValue(
          eq(preparedStatement), eq(11), eq(false)));

      utilMock.verify(() -> Util.closeStatement(preparedStatement));
    }
  }
}
