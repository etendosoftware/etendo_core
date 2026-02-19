package org.openbravo.erpCommon.ad_actionButton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.financial.ResetAccounting;

/**
 * Unit tests for ActionButtonUtility.
 * Many methods depend heavily on database calls, so we focus on
 * the resetAccounting method which has testable logic paths.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ActionButtonUtilityTest {

  private static final String TEST_CLIENT = "testClient";
  private static final String TEST_ORG = "testOrg";
  private static final String TEST_TABLE_ID = "318";
  private static final String TEST_KEY = "testKey001";
  private static final String TEST_DATE = "2024-01-01";
  private static final String TEST_LANGUAGE = "en_US";

  @Mock
  private VariablesSecureApp mockVars;
  @Mock
  private ConnectionProvider mockConn;

  private MockedStatic<ResetAccounting> resetAccountingStatic;
  private MockedStatic<Utility> utilityStatic;

  @Before
  public void setUp() {
    lenient().when(mockVars.getLanguage()).thenReturn(TEST_LANGUAGE);

    resetAccountingStatic = mockStatic(ResetAccounting.class);
    utilityStatic = mockStatic(Utility.class);
  }

  @After
  public void tearDown() {
    if (resetAccountingStatic != null) resetAccountingStatic.close();
    if (utilityStatic != null) utilityStatic.close();
  }

  @Test
  public void testResetAccountingSuccess() {
    // Arrange
    HashMap<String, Integer> resultMap = new HashMap<>();
    resultMap.put("updated", 5);
    resultMap.put("deleted", 3);

    resetAccountingStatic.when(() -> ResetAccounting.delete(
        eq(TEST_CLIENT), eq(TEST_ORG), eq(TEST_TABLE_ID),
        eq(TEST_KEY), eq(TEST_DATE), eq(TEST_DATE)
    )).thenReturn(resultMap);

    utilityStatic.when(() -> Utility.parseTranslation(
        any(ConnectionProvider.class), any(VariablesSecureApp.class),
        anyString(), anyString()
    )).thenReturn("Unposted = 5, Deleted = 3");

    utilityStatic.when(() -> Utility.messageBD(
        any(ConnectionProvider.class), anyString(), anyString()
    )).thenReturn("Success");

    // Act
    OBError result = ActionButtonUtility.resetAccounting(
        mockVars, TEST_CLIENT, TEST_ORG, TEST_TABLE_ID, TEST_KEY, TEST_DATE, mockConn);

    // Assert
    assertNotNull(result);
    assertEquals("SUCCESS", result.getType());
  }

  @Test
  public void testResetAccountingOBExceptionSetsError() {
    // Arrange
    resetAccountingStatic.when(() -> ResetAccounting.delete(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
    )).thenThrow(new OBException("Reset failed"));

    utilityStatic.when(() -> Utility.parseTranslation(
        any(ConnectionProvider.class), any(VariablesSecureApp.class),
        anyString(), anyString()
    )).thenReturn("Reset failed");

    // Act
    OBError result = ActionButtonUtility.resetAccounting(
        mockVars, TEST_CLIENT, TEST_ORG, TEST_TABLE_ID, TEST_KEY, TEST_DATE, mockConn);

    // Assert
    assertNotNull(result);
    assertEquals("Error", result.getType());
  }
}
