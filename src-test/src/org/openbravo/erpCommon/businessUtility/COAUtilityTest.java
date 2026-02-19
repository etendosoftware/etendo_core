package org.openbravo.erpCommon.businessUtility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link COAUtility}.
 * Focuses on testable private utility methods: setAccountType, setAccountSign,
 * operandProcess, nextSeqNo.
 */
@RunWith(MockitoJUnitRunner.class)
public class COAUtilityTest {

  private COAUtility instance;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(COAUtility.class);
  }

  // --- Tests for setAccountType(COAData) ---

  @Test
  public void testSetAccountTypeReturnsNullForNullData() throws Exception {
    String result = invokeSetAccountType(null);
    assertNull(result);
  }

  @Test
  public void testSetAccountTypeReturnsAssetType() throws Exception {
    COAData data = createCOAData("Asset", "");
    String result = invokeSetAccountType(data);
    assertEquals("A", result);
  }

  @Test
  public void testSetAccountTypeLiabilityType() throws Exception {
    COAData data = createCOAData("Liability", "");
    String result = invokeSetAccountType(data);
    assertEquals("L", result);
  }

  @Test
  public void testSetAccountTypeOwnerEquity() throws Exception {
    COAData data = createCOAData("Owner's Equity", "");
    String result = invokeSetAccountType(data);
    assertEquals("O", result);
  }

  @Test
  public void testSetAccountTypeExpense() throws Exception {
    COAData data = createCOAData("Expense", "");
    String result = invokeSetAccountType(data);
    assertEquals("E", result);
  }

  @Test
  public void testSetAccountTypeRevenue() throws Exception {
    COAData data = createCOAData("Revenue", "");
    String result = invokeSetAccountType(data);
    assertEquals("R", result);
  }

  @Test
  public void testSetAccountTypeMemo() throws Exception {
    COAData data = createCOAData("Memo", "");
    String result = invokeSetAccountType(data);
    assertEquals("M", result);
  }

  @Test
  public void testSetAccountTypeDefaultsToEForUnknown() throws Exception {
    COAData data = createCOAData("Unknown", "");
    String result = invokeSetAccountType(data);
    assertEquals("E", result);
  }

  @Test
  public void testSetAccountTypeDefaultsToEForEmpty() throws Exception {
    COAData data = createCOAData("", "");
    String result = invokeSetAccountType(data);
    assertEquals("E", result);
  }

  @Test
  public void testSetAccountTypeLowerCase() throws Exception {
    COAData data = createCOAData("asset", "");
    String result = invokeSetAccountType(data);
    assertEquals("A", result);
  }

  // --- Tests for setAccountSign(COAData) ---

  @Test
  public void testSetAccountSignDebit() throws Exception {
    COAData data = createCOAData("", "Debit");
    String result = invokeSetAccountSign(data);
    assertEquals("D", result);
  }

  @Test
  public void testSetAccountSignCredit() throws Exception {
    COAData data = createCOAData("", "Credit");
    String result = invokeSetAccountSign(data);
    assertEquals("C", result);
  }

  @Test
  public void testSetAccountSignDefaultsToNForEmpty() throws Exception {
    COAData data = createCOAData("", "");
    String result = invokeSetAccountSign(data);
    assertEquals("N", result);
  }

  @Test
  public void testSetAccountSignDefaultsToNForUnknown() throws Exception {
    COAData data = createCOAData("", "Unknown");
    String result = invokeSetAccountSign(data);
    assertEquals("N", result);
  }

  @Test
  public void testSetAccountSignLowerCase() throws Exception {
    COAData data = createCOAData("", "debit");
    String result = invokeSetAccountSign(data);
    assertEquals("D", result);
  }

  // --- Tests for operandProcess(String) ---

  @Test
  public void testOperandProcessReturnsNullForNull() throws Exception {
    String[][] result = invokeOperandProcess(null);
    assertNull(result);
  }

  @Test
  public void testOperandProcessReturnsNullForEmpty() throws Exception {
    String[][] result = invokeOperandProcess("");
    assertNull(result);
  }

  @Test
  public void testOperandProcessSingleValue() throws Exception {
    String[][] result = invokeOperandProcess("100");
    assertNotNull(result);
    assertEquals(1, result.length);
    assertEquals("100", result[0][0]);
    assertEquals("+", result[0][1]);
  }

  @Test
  public void testOperandProcessAddition() throws Exception {
    String[][] result = invokeOperandProcess("100+200");
    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals("100", result[0][0]);
    assertEquals("+", result[0][1]);
    assertEquals("200", result[1][0]);
  }

  @Test
  public void testOperandProcessSubtraction() throws Exception {
    String[][] result = invokeOperandProcess("100-200");
    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals("100", result[0][0]);
    assertEquals("+", result[0][1]);
    assertEquals("200", result[1][0]);
    assertEquals("-", result[1][1]);
  }

  @Test
  public void testOperandProcessMultipleOperands() throws Exception {
    String[][] result = invokeOperandProcess("100+200-300");
    assertNotNull(result);
    assertEquals(3, result.length);
    assertEquals("100", result[0][0]);
    assertEquals("200", result[1][0]);
    assertEquals("300", result[2][0]);
  }

  // --- Tests for nextSeqNo(String) ---

  @Test
  public void testNextSeqNoFromTen() throws Exception {
    String result = invokeNextSeqNo("10");
    assertEquals("20", result);
  }

  @Test
  public void testNextSeqNoFromZero() throws Exception {
    String result = invokeNextSeqNo("0");
    assertEquals("10", result);
  }

  @Test
  public void testNextSeqNoFromLargeNumber() throws Exception {
    String result = invokeNextSeqNo("990");
    assertEquals("1000", result);
  }

  // --- Helper methods ---

  private String invokeSetAccountType(COAData data) throws Exception {
    Method method = COAUtility.class.getDeclaredMethod("setAccountType", COAData.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, data);
  }

  private String invokeSetAccountSign(COAData data) throws Exception {
    Method method = COAUtility.class.getDeclaredMethod("setAccountSign", COAData.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, data);
  }

  private String[][] invokeOperandProcess(String operand) throws Exception {
    Method method = COAUtility.class.getDeclaredMethod("operandProcess", String.class);
    method.setAccessible(true);
    return (String[][]) method.invoke(instance, operand);
  }

  private String invokeNextSeqNo(String oldSeqNo) throws Exception {
    Method method = COAUtility.class.getDeclaredMethod("nextSeqNo", String.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, oldSeqNo);
  }

  private COAData createCOAData(String accountType, String accountSign) {
    ObjenesisStd objenesis = new ObjenesisStd();
    COAData data = objenesis.newInstance(COAData.class);
    data.accountType = accountType;
    data.accountSign = accountSign;
    data.accountValue = "";
    data.accountName = "";
    data.accountDescription = "";
    data.accountDocument = "";
    data.accountSummary = "";
    data.defaultAccount = "";
    data.accountParent = "";
    data.elementLevel = "";
    data.operands = "";
    data.balanceSheet = "";
    data.balanceSheetName = "";
    data.uS1120BalanceSheet = "";
    data.uS1120BalanceSheetName = "";
    data.profitAndLoss = "";
    data.profitAndLossName = "";
    data.uS1120IncomeStatement = "";
    data.uS1120IncomeStatementName = "";
    data.cashFlow = "";
    data.cashFlowName = "";
    data.showValueCond = "";
    data.titleNode = "";
    return data;
  }
}
