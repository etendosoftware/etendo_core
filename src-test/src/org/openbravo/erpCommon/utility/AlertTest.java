package org.openbravo.erpCommon.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.database.ConnectionProvider;

/**
 * Tests for {@link Alert}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AlertTest {

  private static final int TEST_RULE_ID = 42;
  private static final String TEST_RECORD_ID = "REC001";
  private static final String TEST_DESCRIPTION = "Test alert description";

  @Mock
  private ConnectionProvider mockConn;

  private MockedStatic<AlertData> alertDataStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    alertDataStatic = mockStatic(AlertData.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (alertDataStatic != null) {
      alertDataStatic.close();
    }
  }
  /** Default constructor. */

  @Test
  public void testDefaultConstructor() {
    Alert alert = new Alert();
    assertEquals(0, alert.getAlertRuleId());
    assertNull(alert.getRecordId());
  }
  /** Constructor with rule id. */

  @Test
  public void testConstructorWithRuleId() {
    Alert alert = new Alert(TEST_RULE_ID);
    assertEquals(TEST_RULE_ID, alert.getAlertRuleId());
    assertNull(alert.getRecordId());
  }
  /** Constructor with rule id and record id. */

  @Test
  public void testConstructorWithRuleIdAndRecordId() {
    Alert alert = new Alert(TEST_RULE_ID, TEST_RECORD_ID);
    assertEquals(TEST_RULE_ID, alert.getAlertRuleId());
    assertEquals(TEST_RECORD_ID, alert.getRecordId());
  }
  /** Set and get alert rule id. */

  @Test
  public void testSetAndGetAlertRuleId() {
    Alert alert = new Alert();
    alert.setAlertRuleId(TEST_RULE_ID);
    assertEquals(TEST_RULE_ID, alert.getAlertRuleId());
  }
  /** Set and get record id. */

  @Test
  public void testSetAndGetRecordId() {
    Alert alert = new Alert();
    alert.setRecordId(TEST_RECORD_ID);
    assertEquals(TEST_RECORD_ID, alert.getRecordId());
  }
  /** Set and get description. */

  @Test
  public void testSetAndGetDescription() {
    Alert alert = new Alert();
    alert.setDescription(TEST_DESCRIPTION);
    assertEquals(TEST_DESCRIPTION, alert.getDescription());
  }
  /** Save returns false when rule id is zero. */

  @Test
  public void testSaveReturnsFalseWhenRuleIdIsZero() {
    Alert alert = new Alert();
    alert.setDescription(TEST_DESCRIPTION);

    boolean result = alert.save(mockConn);

    assertFalse(result);
  }
  /** Save returns false when description is null. */

  @Test
  public void testSaveReturnsFalseWhenDescriptionIsNull() {
    Alert alert = new Alert(TEST_RULE_ID);

    boolean result = alert.save(mockConn);

    assertFalse(result);
  }
  /** Save returns false when description is empty. */

  @Test
  public void testSaveReturnsFalseWhenDescriptionIsEmpty() {
    Alert alert = new Alert(TEST_RULE_ID);
    alert.setDescription("");

    boolean result = alert.save(mockConn);

    assertFalse(result);
  }
  /**
   * Save with record id uses select method.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSaveWithRecordIdUsesSelectMethod() throws Exception {
    Alert alert = new Alert(TEST_RULE_ID, TEST_RECORD_ID);
    alert.setDescription(TEST_DESCRIPTION);

    alertDataStatic.when(() -> AlertData.select(eq(mockConn),
        eq(String.valueOf(TEST_RULE_ID)), eq(TEST_RECORD_ID)))
        .thenReturn(new AlertData[0]);

    boolean result = alert.save(mockConn);

    assertTrue(result);
    alertDataStatic.verify(() -> AlertData.select(eq(mockConn),
        eq(String.valueOf(TEST_RULE_ID)), eq(TEST_RECORD_ID)));
  }
  /**
   * Save without record id uses select by description.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSaveWithoutRecordIdUsesSelectByDescription() throws Exception {
    Alert alert = new Alert(TEST_RULE_ID);
    alert.setDescription(TEST_DESCRIPTION);

    alertDataStatic.when(() -> AlertData.selectByDescription(eq(mockConn),
        eq(String.valueOf(TEST_RULE_ID)), eq(TEST_DESCRIPTION)))
        .thenReturn(new AlertData[0]);

    boolean result = alert.save(mockConn);

    assertTrue(result);
    alertDataStatic.verify(() -> AlertData.selectByDescription(eq(mockConn),
        eq(String.valueOf(TEST_RULE_ID)), eq(TEST_DESCRIPTION)));
  }
  /**
   * Save inserts when no existing data.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSaveInsertsWhenNoExistingData() throws Exception {
    Alert alert = new Alert(TEST_RULE_ID, TEST_RECORD_ID);
    alert.setDescription(TEST_DESCRIPTION);

    alertDataStatic.when(() -> AlertData.select(eq(mockConn),
        eq(String.valueOf(TEST_RULE_ID)), eq(TEST_RECORD_ID)))
        .thenReturn(new AlertData[0]);

    boolean result = alert.save(mockConn);

    assertTrue(result);
    alertDataStatic.verify(() -> AlertData.insert(eq(mockConn), eq(TEST_DESCRIPTION),
        eq(String.valueOf(TEST_RULE_ID)), eq(TEST_RECORD_ID), eq("0")));
  }
  /**
   * Save does not insert when data exists.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSaveDoesNotInsertWhenDataExists() throws Exception {
    Alert alert = new Alert(TEST_RULE_ID, TEST_RECORD_ID);
    alert.setDescription(TEST_DESCRIPTION);

    AlertData[] existingData = new AlertData[] { new AlertData() };
    alertDataStatic.when(() -> AlertData.select(eq(mockConn),
        eq(String.valueOf(TEST_RULE_ID)), eq(TEST_RECORD_ID)))
        .thenReturn(existingData);

    boolean result = alert.save(mockConn);

    assertTrue(result);
    alertDataStatic.verify(() -> AlertData.insert(eq(mockConn), anyString(),
        anyString(), anyString(), anyString()), never());
  }
  /**
   * Save returns false on exception.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSaveReturnsFalseOnException() throws Exception {
    Alert alert = new Alert(TEST_RULE_ID, TEST_RECORD_ID);
    alert.setDescription(TEST_DESCRIPTION);

    alertDataStatic.when(() -> AlertData.select(eq(mockConn),
        eq(String.valueOf(TEST_RULE_ID)), eq(TEST_RECORD_ID)))
        .thenThrow(new RuntimeException("DB error"));

    boolean result = alert.save(mockConn);

    assertFalse(result);
  }
}
