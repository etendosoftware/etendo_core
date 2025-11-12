package org.openbravo.scheduling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.openbravo.scheduling.OBScheduler.OB_GROUP;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

/**
 * Test cases to cover the JobDetailProvider functionality for creating JobDetail objects.
 */
public class JobDetailProviderTest {
  public static final String ERROR_JOB_DETAIL_NULL = "JobDetail should not be null";
  public static final String ERROR_CONNECTION_NOT_SET = "Connection should be set in the bundle";

  private JobDetailProvider provider;
  private ProcessBundle testBundle;
  private String testJobName;
  private ConnectionProvider mockConnectionProvider;

  /**
   * Sets up the test environment before each test.
   * Initializes the JobDetailProvider, test data, and mocks.
   */
  @BeforeEach
  public void setup() {
    provider = JobDetailProvider.getInstance();
    testJobName = "testJob";
    VariablesSecureApp vars = new VariablesSecureApp("0", "0", "0");
    testBundle = new ProcessBundle("TEST", vars);
    mockConnectionProvider = mock(ConnectionProvider.class);
  }

  /**
   * Tests the singleton pattern implementation of JobDetailProvider.
   * Verifies that multiple getInstance() calls return the same instance.
   */
  @Test
  public void testGetInstance() {
    // Test singleton pattern
    JobDetailProvider instance1 = JobDetailProvider.getInstance();
    JobDetailProvider instance2 = JobDetailProvider.getInstance();

    assertThat("JobDetailProvider is a singleton", instance1, is(instance2));
  }

  /**
   * Tests creating a JobDetail without an explicit connection provider.
   * Verifies that a default DalConnectionProvider is used.
   *
   * @throws SchedulerException
   *     if job creation fails
   */
  @Test
  public void testCreateJobDetailWithoutConnection() throws SchedulerException {
    // Test creating job detail without explicit connection provider
    JobDetail jobDetail = provider.createJobDetail(testJobName, testBundle);

    assertThat(ERROR_JOB_DETAIL_NULL, jobDetail, is(notNullValue()));
    assertThat("JobDetail should have the correct name", jobDetail.getKey().getName(), is(equalTo(testJobName)));
    assertThat("JobDetail should have the correct group", jobDetail.getKey().getGroup(), is(equalTo(OB_GROUP)));
    assertThat("JobDetail should use DefaultJob class", jobDetail.getJobClass(), is(equalTo(DefaultJob.class)));

    // Verify process bundle is stored in job data map
    Map<String, Object> bundleMap = (Map<String, Object>) jobDetail.getJobDataMap().get(ProcessBundle.KEY);
    assertThat("Process bundle map should be stored in job data map", bundleMap, is(notNullValue()));
    assertThat("Process bundle map should match the original", bundleMap, is(equalTo(testBundle.getMap())));

    // Verify connection is set and is a DalConnectionProvider
    assertThat(ERROR_CONNECTION_NOT_SET, testBundle.getConnection(), is(notNullValue()));
    assertThat("Connection should be a DalConnectionProvider", testBundle.getConnection(),
        is(instanceOf(DalConnectionProvider.class)));
  }

  /**
   * Tests creating a JobDetail with an explicit connection provider.
   * Verifies that the provided connection is used.
   *
   * @throws SchedulerException
   *     if job creation fails
   */
  @Test
  public void testCreateJobDetailWithConnection() throws SchedulerException {
    // Test creating job detail with explicit connection provider
    JobDetail jobDetail = provider.createJobDetail(mockConnectionProvider, testJobName, testBundle);

    assertThat(ERROR_JOB_DETAIL_NULL, jobDetail, is(notNullValue()));
    assertThat("JobDetail should have the correct name", jobDetail.getKey().getName(), is(equalTo(testJobName)));
    assertThat("JobDetail should have the correct group", jobDetail.getKey().getGroup(), is(equalTo(OB_GROUP)));

    // Verify connection is set to the provided mock
    assertThat(ERROR_CONNECTION_NOT_SET, testBundle.getConnection(), is(notNullValue()));
    assertThat("Connection should be the mock provider", testBundle.getConnection(),
        is(equalTo(mockConnectionProvider)));
  }

  /**
   * Tests that creating a JobDetail with a null bundle throws an exception.
   */
  @Test
  public void testCreateJobDetailWithNullBundle() {
    assertThrows(SchedulerException.class, () -> provider.createJobDetail(testJobName, null));
  }

  /**
   * Tests that a null connection provider results in using the default provider.
   * Verifies that DalConnectionProvider is used as fallback.
   *
   * @throws SchedulerException
   *     if job creation fails
   */
  @Test
  public void testCreateJobDetailWithNullConnectionUsesDefault() throws SchedulerException {
    // Test that passing null connection uses DalConnectionProvider
    JobDetail jobDetail = provider.createJobDetail(null, testJobName, testBundle);

    assertThat(ERROR_JOB_DETAIL_NULL, jobDetail, is(notNullValue()));
    assertThat(ERROR_CONNECTION_NOT_SET, testBundle.getConnection(), is(notNullValue()));
    assertThat("Connection should be a DalConnectionProvider", testBundle.getConnection(),
        is(instanceOf(DalConnectionProvider.class)));
  }

  /**
   * Tests creating multiple JobDetails with different names.
   * Verifies that each JobDetail maintains its unique name.
   *
   * @throws SchedulerException
   *     if job creation fails
   */
  @Test
  public void testJobDetailWithDifferentNames() throws SchedulerException {
    // Test creating multiple job details with different names
    String secondJobName = "anotherTestJob";

    JobDetail jobDetail1 = provider.createJobDetail(testJobName, testBundle);
    JobDetail jobDetail2 = provider.createJobDetail(secondJobName, testBundle);

    assertThat("First JobDetail should have the correct name", jobDetail1.getKey().getName(), is(equalTo(testJobName)));
    assertThat("Second JobDetail should have the correct name", jobDetail2.getKey().getName(),
        is(equalTo(secondJobName)));
    assertThat("JobDetails should have different names", jobDetail1.getKey().getName(),
        is(not(equalTo(jobDetail2.getKey().getName()))));
  }

  /**
   * Tests that the connection provider is properly set in the ProcessBundle.
   * Verifies that the bundle contains the correct connection reference.
   *
   * @throws SchedulerException
   *     if job creation fails
   */
  @Test
  public void testConnectionSetInBundle() throws SchedulerException {
    // Test that the connection is properly set in the bundle
    provider.createJobDetail(mockConnectionProvider, testJobName, testBundle);

    assertThat(ERROR_CONNECTION_NOT_SET, testBundle.getConnection(), is(notNullValue()));
    assertThat("Connection should be the mock provider", testBundle.getConnection(),
        is(equalTo(mockConnectionProvider)));
  }
}
