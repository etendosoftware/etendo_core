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
@RunWith(MockitoJUnitRunner.Silent.class)
public class AlertTest {

  private static final int TEST_RULE_ID = 42;
  private static final String TEST_RECORD_ID = "REC001";
  private static final String TEST_DESCRIPTION = "Test alert description";

  @Mock
  private ConnectionProvider mockConn;

  private MockedStatic<AlertData> alertDataStatic;

  @Before
  public void setUp() {
    alertDataStatic = mockStatic(AlertData.class);
  }

  @After
  public void tearDown() {
    if (alertDataStatic != null) {
      alertDataStatic.close();
    }
  }

  @Test
  public void testDefaultConstructor() {
    Alert alert = new Alert();
    assertEquals(0, alert.getAlertRuleId());
    assertNull(alert.getRecordId());
  }

  @Test
  public void testConstructorWithRuleId() {
    Alert alert = new Alert(TEST_RULE_ID);
    assertEquals(TEST_RULE_ID, alert.getAlertRuleId());
    assertNull(alert.getRecordId());
  }

  @Test
  public void testConstructorWithRuleIdAndRecordId() {
    Alert alert = new Alert(TEST_RULE_ID, TEST_RECORD_ID);
    assertEquals(TEST_RULE_ID, alert.getAlertRuleId());
    assertEquals(TEST_RECORD_ID, alert.getRecordId());
  }

  @Test
  public void testSetAndGetAlertRuleId() {
    Alert alert = new Alert();
    alert.setAlertRuleId(TEST_RULE_ID);
    assertEquals(TEST_RULE_ID, alert.getAlertRuleId());
  }

  @Test
  public void testSetAndGetRecordId() {
    Alert alert = new Alert();
    alert.setRecordId(TEST_RECORD_ID);
    assertEquals(TEST_RECORD_ID, alert.getRecordId());
  }

  @Test
  public void testSetAndGetDescription() {
    Alert alert = new Alert();
    alert.setDescription(TEST_DESCRIPTION);
    assertEquals(TEST_DESCRIPTION, alert.getDescription());
  }

  @Test
  public void testSaveReturnsFalseWhenRuleIdIsZero() {
    Alert alert = new Alert();
    alert.setDescription(TEST_DESCRIPTION);

    boolean result = alert.save(mockConn);

    assertFalse(result);
  }

  @Test
  public void testSaveReturnsFalseWhenDescriptionIsNull() {
    Alert alert = new Alert(TEST_RULE_ID);

    boolean result = alert.save(mockConn);

    assertFalse(result);
  }

  @Test
  public void testSaveReturnsFalseWhenDescriptionIsEmpty() {
    Alert alert = new Alert(TEST_RULE_ID);
    alert.setDescription("");

    boolean result = alert.save(mockConn);

    assertFalse(result);
  }

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
