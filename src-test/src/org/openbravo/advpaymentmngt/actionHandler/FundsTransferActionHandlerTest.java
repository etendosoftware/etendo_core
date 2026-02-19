package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;

/**
 * Tests for {@link FundsTransferActionHandler}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class FundsTransferActionHandlerTest {

  private static final String VAL_100_00 = "100.00";
  private static final String CONVERT_AMOUNT = "convertAmount";

  @Mock
  private FIN_FinancialAccount mockAccountFrom;

  @Mock
  private FIN_FinancialAccount mockAccountTo;

  @Mock
  private Currency mockCurrencyFrom;

  @Mock
  private Currency mockCurrencyTo;

  @Mock
  private Organization mockOrg;

  private MockedStatic<FinancialUtils> financialUtilsStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    financialUtilsStatic = Mockito.mockStatic(FinancialUtils.class);
    lenient().when(mockAccountFrom.getCurrency()).thenReturn(mockCurrencyFrom);
    lenient().when(mockAccountTo.getCurrency()).thenReturn(mockCurrencyTo);
    lenient().when(mockAccountFrom.getOrganization()).thenReturn(mockOrg);
    lenient().when(mockCurrencyTo.getStandardPrecision()).thenReturn(2L);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (financialUtilsStatic != null) {
      financialUtilsStatic.close();
    }
  }
  /**
   * Convert amount with rate.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConvertAmountWithRate() throws Exception {
    BigDecimal amount = new BigDecimal(VAL_100_00);
    BigDecimal rate = new BigDecimal("1.5");

    Method method = FundsTransferActionHandler.class.getDeclaredMethod(CONVERT_AMOUNT,
        BigDecimal.class, FIN_FinancialAccount.class, FIN_FinancialAccount.class, Date.class, BigDecimal.class);
    method.setAccessible(true);

    BigDecimal result = (BigDecimal) method.invoke(null, amount, mockAccountFrom, mockAccountTo, new Date(), rate);
    assertEquals(new BigDecimal("150.00"), result);
  }
  /**
   * Convert amount with rate rounds to scale.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConvertAmountWithRateRoundsToScale() throws Exception {
    BigDecimal amount = new BigDecimal(VAL_100_00);
    BigDecimal rate = new BigDecimal("1.333");

    Method method = FundsTransferActionHandler.class.getDeclaredMethod(CONVERT_AMOUNT,
        BigDecimal.class, FIN_FinancialAccount.class, FIN_FinancialAccount.class, Date.class, BigDecimal.class);
    method.setAccessible(true);

    BigDecimal result = (BigDecimal) method.invoke(null, amount, mockAccountFrom, mockAccountTo, new Date(), rate);
    assertEquals(new BigDecimal("133.30"), result);
  }
  /**
   * Convert amount with null rate uses financial utils.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConvertAmountWithNullRateUsesFinancialUtils() throws Exception {
    BigDecimal amount = new BigDecimal(VAL_100_00);
    BigDecimal expectedConverted = new BigDecimal("200.00");
    Date date = new Date();

    financialUtilsStatic.when(() -> FinancialUtils.getConvertedAmount(
        amount, mockCurrencyFrom, mockCurrencyTo, date, mockOrg, null))
        .thenReturn(expectedConverted);

    Method method = FundsTransferActionHandler.class.getDeclaredMethod(CONVERT_AMOUNT,
        BigDecimal.class, FIN_FinancialAccount.class, FIN_FinancialAccount.class, Date.class, BigDecimal.class);
    method.setAccessible(true);

    BigDecimal result = (BigDecimal) method.invoke(null, amount, mockAccountFrom, mockAccountTo, date, (BigDecimal) null);
    assertEquals(expectedConverted, result);
  }
  /**
   * Line number util get next line number.
   * @throws Exception if an error occurs
   */

  @Test
  public void testLineNumberUtilGetNextLineNumber() throws Exception {
    Class<?> lineNumClass = Class.forName(
        "org.openbravo.advpaymentmngt.actionHandler.FundsTransferActionHandler$LineNumberUtil");
    java.lang.reflect.Constructor<?> constructor = lineNumClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    Object lineNoUtil = constructor.newInstance();

    // Set the lastLineNo map with a pre-existing value
    java.lang.reflect.Field field = lineNumClass.getDeclaredField("lastLineNo");
    field.setAccessible(true);
    HashMap<FIN_FinancialAccount, Long> map = new HashMap<>();
    map.put(mockAccountFrom, 10L);
    field.set(lineNoUtil, map);

    Method getNext = lineNumClass.getDeclaredMethod("getNextLineNumber", FIN_FinancialAccount.class);
    getNext.setAccessible(true);

    Long result = (Long) getNext.invoke(lineNoUtil, mockAccountFrom);
    assertEquals(Long.valueOf(20L), result);
  }
  /**
   * Line number util increments sequentially.
   * @throws Exception if an error occurs
   */

  @Test
  public void testLineNumberUtilIncrementsSequentially() throws Exception {
    Class<?> lineNumClass = Class.forName(
        "org.openbravo.advpaymentmngt.actionHandler.FundsTransferActionHandler$LineNumberUtil");
    java.lang.reflect.Constructor<?> constructor = lineNumClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    Object lineNoUtil = constructor.newInstance();

    java.lang.reflect.Field field = lineNumClass.getDeclaredField("lastLineNo");
    field.setAccessible(true);
    HashMap<FIN_FinancialAccount, Long> map = new HashMap<>();
    map.put(mockAccountFrom, 10L);
    field.set(lineNoUtil, map);

    Method getNext = lineNumClass.getDeclaredMethod("getNextLineNumber", FIN_FinancialAccount.class);
    getNext.setAccessible(true);

    Long first = (Long) getNext.invoke(lineNoUtil, mockAccountFrom);
    Long second = (Long) getNext.invoke(lineNoUtil, mockAccountFrom);
    assertEquals(Long.valueOf(20L), first);
    assertEquals(Long.valueOf(30L), second);
  }
}
