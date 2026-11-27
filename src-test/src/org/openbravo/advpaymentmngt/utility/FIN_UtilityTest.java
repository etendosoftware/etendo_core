/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;

/**
 * Unit tests for {@link FIN_Utility}.
 */
@SuppressWarnings({"java:S101", "java:S112"})
@DisplayName("FIN_Utility")
public class FIN_UtilityTest {

  private static final String AMOUNT_100 = "100.00";
  private static final String AMOUNT_NEG_50 = "-50.00";
  private static final String FORMAT_PATTERN = "#,##0.00";
  private static final String VALUE2 = "value2";
  private static final String TRXTYPE = "trxtype";

  private static final String ARS_ID = "ARS_ID";
  private static final String USD_ID = "USD_ID";
  private static final String BP_ID = "BP_ID";
  private static final String BP_NAME = "Test BP";
  private static final String MESSAGE_TEMPLATE = "Currency of %s (%s) is not defined";
  private static final BigDecimal PSD_AMOUNT = new BigDecimal("121.00");
  private static final BigDecimal PSD_WRITEOFF = new BigDecimal("4.00");
  private static final BigDecimal CONVERTED_AMOUNT = new BigDecimal("181500.00");
  private static final BigDecimal CONVERTED_AMOUNT_WITH_WRITEOFF = new BigDecimal("187500.00");
  private static final BigDecimal CREDIT_USED = new BigDecimal("181500.00");

  private static Currency mockCurrency(String id) {
    Currency currency = mock(Currency.class);
    when(currency.getId()).thenReturn(id);
    return currency;
  }

  private static BusinessPartner mockBusinessPartner(Currency currency, BigDecimal creditUsed) {
    BusinessPartner bp = mock(BusinessPartner.class);
    when(bp.getId()).thenReturn(BP_ID);
    when(bp.getName()).thenReturn(BP_NAME);
    when(bp.getCurrency()).thenReturn(currency);
    when(bp.getCreditUsed()).thenReturn(creditUsed);
    return bp;
  }

  private static BigDecimal capturedCreditUsed(BusinessPartner bp) {
    ArgumentCaptor<BigDecimal> captor = ArgumentCaptor.forClass(BigDecimal.class);
    verify(bp).setCreditUsed(captor.capture());
    return captor.getValue();
  }

  private static void assertAmountEquals(String expected, BigDecimal actual) {
    assertEquals(0, new BigDecimal(expected).compareTo(actual), "Expected " + expected + " but was " + actual);
  }

  // ── getMapFromStringList ─────────────────────────────────────────────

  @Nested
  @DisplayName("getMapFromStringList")
  class GetMapFromStringList {

    @Test
    @DisplayName("parses parenthesized quoted IDs")
    void parsesParenthesizedQuotedIds() {
      Map<String, String> result = FIN_Utility.getMapFromStringList("('id1','id2','id3')");
      assertEquals(3, result.size());
      assertEquals("id1", result.get("id1"));
      assertEquals("id2", result.get("id2"));
      assertEquals("id3", result.get("id3"));
    }

    @Test
    @DisplayName("parses plain comma-separated IDs")
    void parsesPlainCommaSeparated() {
      Map<String, String> result = FIN_Utility.getMapFromStringList("id1,id2");
      assertEquals(2, result.size());
      assertEquals("id1", result.get("id1"));
      assertEquals("id2", result.get("id2"));
    }

    @Test
    @DisplayName("empty parentheses returns empty map")
    void emptyParentheses() {
      Map<String, String> result = FIN_Utility.getMapFromStringList("()");
      assertTrue(result.isEmpty());
    }
  }

  // ── getDaysBetween ───────────────────────────────────────────────────

  @Nested
  @DisplayName("getDaysBetween")
  class GetDaysBetween {

    @Test
    @DisplayName("same date returns 0")
    void sameDateReturnsZero() {
      Date now = new Date();
      assertEquals(0L, FIN_Utility.getDaysBetween(now, now));
    }

    @Test
    @DisplayName("dates 5 days apart returns 5")
    void fiveDaysApart() {
      Calendar cal = Calendar.getInstance();
      cal.set(2025, Calendar.JANUARY, 1, 12, 0, 0);
      Date begin = cal.getTime();
      cal.add(Calendar.DAY_OF_MONTH, 5);
      Date end = cal.getTime();
      assertEquals(5L, FIN_Utility.getDaysBetween(begin, end));
    }

    @Test
    @DisplayName("end before begin returns negative")
    void endBeforeBeginReturnsNegative() {
      Calendar cal = Calendar.getInstance();
      cal.set(2025, Calendar.JANUARY, 10, 12, 0, 0);
      Date begin = cal.getTime();
      cal.set(2025, Calendar.JANUARY, 7, 12, 0, 0);
      Date end = cal.getTime();
      assertEquals(-3L, FIN_Utility.getDaysBetween(begin, end));
    }
  }

  // ── getDepositAmount ─────────────────────────────────────────────────

  @Nested
  @DisplayName("getDepositAmount")
  class GetDepositAmount {

    @Test
    @DisplayName("receipt with positive amount returns amount")
    void receiptPositive() {
      BigDecimal amt = new BigDecimal(AMOUNT_100);
      assertEquals(amt, FIN_Utility.getDepositAmount(true, amt));
    }

    @Test
    @DisplayName("receipt with negative amount returns ZERO")
    void receiptNegative() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getDepositAmount(true, new BigDecimal(AMOUNT_NEG_50)));
    }

    @Test
    @DisplayName("receipt with zero returns ZERO")
    void receiptZero() {
      assertEquals(BigDecimal.ZERO, FIN_Utility.getDepositAmount(true, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("payment with negative amount returns absolute value")
    void paymentNegative() {
      assertEquals(new BigDecimal("50.00"),
          FIN_Utility.getDepositAmount(false, new BigDecimal(AMOUNT_NEG_50)));
    }

    @Test
    @DisplayName("payment with positive amount returns ZERO")
    void paymentPositive() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getDepositAmount(false, new BigDecimal(AMOUNT_100)));
    }
  }

  // ── getPaymentAmount ─────────────────────────────────────────────────

  @Nested
  @DisplayName("getPaymentAmount")
  class GetPaymentAmount {

    @Test
    @DisplayName("receipt with negative returns absolute value")
    void receiptNegative() {
      assertEquals(new BigDecimal("50.00"),
          FIN_Utility.getPaymentAmount(true, new BigDecimal(AMOUNT_NEG_50)));
    }

    @Test
    @DisplayName("receipt with positive returns ZERO")
    void receiptPositive() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getPaymentAmount(true, new BigDecimal(AMOUNT_100)));
    }

    @Test
    @DisplayName("payment with positive returns amount")
    void paymentPositive() {
      BigDecimal amt = new BigDecimal("200.00");
      assertEquals(amt, FIN_Utility.getPaymentAmount(false, amt));
    }

    @Test
    @DisplayName("payment with negative returns ZERO")
    void paymentNegative() {
      assertEquals(BigDecimal.ZERO,
          FIN_Utility.getPaymentAmount(false, new BigDecimal("-100.00")));
    }
  }

  // ── formatNumber(BigDecimal, String, String, String) ─────────────────

  @Nested
  @DisplayName("formatNumber (4-arg)")
  class FormatNumber {

    @Test
    @DisplayName("formats with standard pattern")
    void formatsWithStandardPattern() {
      String result = FIN_Utility.formatNumber(
          new BigDecimal("1234567.89"), FORMAT_PATTERN, ".", ",");
      assertEquals("1,234,567.89", result);
    }

    @Test
    @DisplayName("null separators default to dot and comma")
    void nullSeparatorsDefault() {
      String result = FIN_Utility.formatNumber(
          new BigDecimal("1000.50"), FORMAT_PATTERN, null, null);
      assertEquals("1,000.50", result);
    }

    @Test
    @DisplayName("zero formats correctly")
    void zeroFormats() {
      String result = FIN_Utility.formatNumber(
          BigDecimal.ZERO, FORMAT_PATTERN, ".", ",");
      assertEquals("0.00", result);
    }
  }

  // ── getFirstNonEmpty ─────────────────────────────────────────────────

  @Nested
  @DisplayName("getFirstNonEmpty")
  class GetFirstNonEmpty {

    @Test
    @DisplayName("returns first matching key value")
    void returnsFirstMatch() throws Exception {
      JSONObject json = new JSONObject();
      json.put("key1", "value1");
      json.put("key2", VALUE2);
      assertEquals("value1", FIN_Utility.getFirstNonEmpty(json, "key1", "key2"));
    }

    @Test
    @DisplayName("skips blank and returns second")
    void skipsBlankReturnsSecond() throws Exception {
      JSONObject json = new JSONObject();
      json.put("key1", "");
      json.put("key2", VALUE2);
      assertEquals(VALUE2, FIN_Utility.getFirstNonEmpty(json, "key1", "key2"));
    }

    @Test
    @DisplayName("returns null when no keys match")
    void returnsNullWhenNoMatch() throws Exception {
      JSONObject json = new JSONObject();
      assertNull(FIN_Utility.getFirstNonEmpty(json, "missing1", "missing2"));
    }
  }

  // ── getDefaultAddPaymentDocument ─────────────────────────────────────

  @Nested
  @DisplayName("getDefaultAddPaymentDocument")
  class GetDefaultAddPaymentDocument {

    @Test
    @DisplayName("BPD returns RCIN")
    void bpdReturnsRCIN() throws Exception {
      JSONObject json = new JSONObject();
      json.put(TRXTYPE, "BPD");
      assertEquals("RCIN", FIN_Utility.getDefaultAddPaymentDocument(json));
    }

    @Test
    @DisplayName("BPW returns PDOUT")
    void bpwReturnsPDOUT() throws Exception {
      JSONObject json = new JSONObject();
      json.put(TRXTYPE, "BPW");
      assertEquals("PDOUT", FIN_Utility.getDefaultAddPaymentDocument(json));
    }

    @Test
    @DisplayName("unknown type returns empty")
    void unknownReturnsEmpty() throws Exception {
      JSONObject json = new JSONObject();
      json.put(TRXTYPE, "OTHER");
      assertEquals("", FIN_Utility.getDefaultAddPaymentDocument(json));
    }

    @Test
    @DisplayName("no trxtype returns empty")
    void noTrxTypeReturnsEmpty() throws Exception {
      JSONObject json = new JSONObject();
      assertEquals("", FIN_Utility.getDefaultAddPaymentDocument(json));
    }
  }

  // ── escape (private) ─────────────────────────────────────────────────

  @Nested
  @DisplayName("escape (private)")
  class Escape {

    private String invokeEscape(String input) throws Exception {
      Method method = FIN_Utility.class.getDeclaredMethod("escape", String.class);
      method.setAccessible(true);
      return (String) method.invoke(null, input);
    }

    @Test
    @DisplayName("replaces > and <")
    void replacesAngleBrackets() throws Exception {
      assertEquals("&lt;b&gt;text&lt;/b&gt;", invokeEscape("<b>text</b>"));
    }

    @Test
    @DisplayName("no special chars returns unchanged")
    void noSpecialChars() throws Exception {
      assertEquals("normal text", invokeEscape("normal text"));
    }

    @Test
    @DisplayName("replaces only >")
    void replacesGreaterThan() throws Exception {
      assertEquals("a &gt; b", invokeEscape("a > b"));
    }

    @Test
    @DisplayName("replaces only <")
    void replacesLessThan() throws Exception {
      assertEquals("a &lt; b", invokeEscape("a < b"));
    }
  }

  // ── Business Partner credit used (currency handling) ─────────────────

  /**
   * Base fixture for the groups that need static mocks. The static mocks are opened before each
   * test and always closed afterwards, so they never leak into the rest of the tests.
   */
  abstract class StaticMocksFixture {

    MockedStatic<OBDal> obDalStatic;
    MockedStatic<FIN_AddPayment> addPaymentStatic;
    MockedStatic<FinancialUtils> financialUtilsStatic;
    MockedStatic<OBMessageUtils> messageUtilsStatic;

    Currency ars;
    Currency usd;
    Organization organization;
    Date invoiceDate;
    List<ConversionRateDoc> rateDocs;

    @BeforeEach
    void setUp() {
      obDalStatic = mockStatic(OBDal.class);
      obDalStatic.when(OBDal::getInstance).thenReturn(mock(OBDal.class));
      addPaymentStatic = mockStatic(FIN_AddPayment.class);
      financialUtilsStatic = mockStatic(FinancialUtils.class);
      messageUtilsStatic = mockStatic(OBMessageUtils.class);
      messageUtilsStatic.when(() -> OBMessageUtils.messageBD("InitBPCurrencyLnk", false)).thenReturn(MESSAGE_TEMPLATE);

      ars = mockCurrency(ARS_ID);
      usd = mockCurrency(USD_ID);
      organization = mock(Organization.class);
      invoiceDate = new Date();
      rateDocs = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
      obDalStatic.close();
      addPaymentStatic.close();
      financialUtilsStatic.close();
      messageUtilsStatic.close();
    }

    Invoice mockInvoice(BusinessPartner bp, Currency currency, boolean isSales) {
      Invoice invoice = mock(Invoice.class);
      when(invoice.getBusinessPartner()).thenReturn(bp);
      when(invoice.getCurrency()).thenReturn(currency);
      when(invoice.isSalesTransaction()).thenReturn(isSales);
      when(invoice.getInvoiceDate()).thenReturn(invoiceDate);
      when(invoice.getOrganization()).thenReturn(organization);
      when(invoice.getCurrencyConversionRateDocList()).thenReturn(rateDocs);
      return invoice;
    }

    /**
     * Mocks the conversion of the given amount from the invoice currency to the BP currency.
     */
    void mockConversion(BigDecimal amount, Currency from, Currency to, BigDecimal result) {
      financialUtilsStatic.when(
          () -> FinancialUtils.getConvertedAmount(eq(amount), eq(from), eq(to), eq(invoiceDate), eq(organization),
              isNull(), eq(rateDocs))).thenReturn(result);
    }

    FIN_PaymentScheduleDetail mockInvoicePsd(Invoice invoice, boolean isReceipt) {
      FIN_PaymentScheduleDetail psd = mock(FIN_PaymentScheduleDetail.class);
      FIN_PaymentSchedule invoiceSchedule = mock(FIN_PaymentSchedule.class);
      when(invoiceSchedule.getInvoice()).thenReturn(invoice);
      FIN_PaymentDetail paymentDetail = mock(FIN_PaymentDetail.class);
      FIN_Payment payment = mock(FIN_Payment.class);
      when(payment.isReceipt()).thenReturn(isReceipt);
      when(paymentDetail.getFinPayment()).thenReturn(payment);
      when(psd.getInvoicePaymentSchedule()).thenReturn(invoiceSchedule);
      when(psd.getPaymentDetails()).thenReturn(paymentDetail);
      when(psd.getAmount()).thenReturn(PSD_AMOUNT);
      when(psd.getWriteoffAmount()).thenReturn(PSD_WRITEOFF);
      return psd;
    }
  }

  // ── convertAmountToBPCurrency ────────────────────────────────────────

  @Nested
  @DisplayName("convertAmountToBPCurrency")
  class ConvertAmountToBPCurrency extends StaticMocksFixture {

    @Test
    @DisplayName("returns the same amount without converting when currencies match")
    void returnsSameAmountWhenSameCurrency() {
      Invoice invoice = mockInvoice(mockBusinessPartner(usd, BigDecimal.ZERO), usd, true);

      BigDecimal result = FIN_Utility.convertAmountToBPCurrency(invoice, PSD_AMOUNT);

      assertSame(PSD_AMOUNT, result);
      financialUtilsStatic.verifyNoInteractions();
    }

    @Test
    @DisplayName("converts using invoice date, invoice organization and invoice rate documents")
    void convertsWithInvoiceDataWhenCurrenciesDiffer() {
      Invoice invoice = mockInvoice(mockBusinessPartner(ars, CREDIT_USED), usd, true);
      mockConversion(PSD_AMOUNT, usd, ars, CONVERTED_AMOUNT);

      BigDecimal result = FIN_Utility.convertAmountToBPCurrency(invoice, PSD_AMOUNT);

      assertSame(CONVERTED_AMOUNT, result);
    }

    @Test
    @DisplayName("throws OBException with the formatted message when the BP has no currency")
    void throwsWhenBPCurrencyIsNull() {
      Invoice invoice = mockInvoice(mockBusinessPartner(null, CREDIT_USED), usd, true);

      OBException exception = assertThrows(OBException.class,
          () -> FIN_Utility.convertAmountToBPCurrency(invoice, PSD_AMOUNT));

      assertEquals(String.format(MESSAGE_TEMPLATE, BP_ID, BP_NAME), exception.getMessage());
    }

    @Test
    @DisplayName("propagates the exception when no conversion rate exists")
    void propagatesMissingRateException() {
      Invoice invoice = mockInvoice(mockBusinessPartner(ars, CREDIT_USED), usd, true);
      financialUtilsStatic.when(
          () -> FinancialUtils.getConvertedAmount(any(), any(), any(), any(), any(), any(), any())).thenThrow(
          new OBException("@NoCurrencyConversion@"));

      assertThrows(OBException.class, () -> FIN_Utility.convertAmountToBPCurrency(invoice, PSD_AMOUNT));
    }
  }

  // ── updatePaymentAmounts ─────────────────────────────────────────────

  @Nested
  @DisplayName("updatePaymentAmounts")
  class UpdatePaymentAmounts extends StaticMocksFixture {

    @Test
    @DisplayName("sales invoice in foreign currency subtracts the converted amount")
    void salesInvoiceUsesConvertedAmount() {
      BusinessPartner bp = mockBusinessPartner(ars, CREDIT_USED);
      Invoice invoice = mockInvoice(bp, usd, true);
      mockConversion(PSD_AMOUNT, usd, ars, CONVERTED_AMOUNT);
      FIN_PaymentScheduleDetail psd = mockInvoicePsd(invoice, true);

      FIN_Utility.updatePaymentAmounts(psd);

      // Ticket scenario: 181500 ARS owed, 121 USD paid -> balance back to 0, not 181379
      assertAmountEquals("0", capturedCreditUsed(bp));
    }

    @Test
    @DisplayName("payment schedule amounts keep the raw invoice currency amounts")
    void paymentScheduleAmountsAreNotConverted() {
      BusinessPartner bp = mockBusinessPartner(ars, CREDIT_USED);
      Invoice invoice = mockInvoice(bp, usd, true);
      mockConversion(PSD_AMOUNT, usd, ars, CONVERTED_AMOUNT);
      FIN_PaymentScheduleDetail psd = mockInvoicePsd(invoice, true);

      FIN_Utility.updatePaymentAmounts(psd);

      FIN_PaymentDetail paymentDetail = psd.getPaymentDetails();
      FIN_PaymentSchedule schedule = psd.getInvoicePaymentSchedule();
      addPaymentStatic.verify(
          () -> FIN_AddPayment.updatePaymentScheduleAmounts(eq(paymentDetail), eq(schedule), eq(PSD_AMOUNT),
              eq(PSD_WRITEOFF)));
    }

    @Test
    @DisplayName("same currency keeps the unconverted behavior")
    void sameCurrencyIsUnchanged() {
      BusinessPartner bp = mockBusinessPartner(usd, new BigDecimal("200.00"));
      Invoice invoice = mockInvoice(bp, usd, true);
      FIN_PaymentScheduleDetail psd = mockInvoicePsd(invoice, true);

      FIN_Utility.updatePaymentAmounts(psd);

      assertAmountEquals("79", capturedCreditUsed(bp));
      financialUtilsStatic.verifyNoInteractions();
    }

    @Test
    @DisplayName("purchase invoice adds the converted amount")
    void purchaseInvoiceAddsConvertedAmount() {
      BusinessPartner bp = mockBusinessPartner(ars, new BigDecimal("-181500.00"));
      Invoice invoice = mockInvoice(bp, usd, false);
      mockConversion(PSD_AMOUNT, usd, ars, CONVERTED_AMOUNT);
      FIN_PaymentScheduleDetail psd = mockInvoicePsd(invoice, false);

      FIN_Utility.updatePaymentAmounts(psd);

      assertAmountEquals("0", capturedCreditUsed(bp));
    }

    @Test
    @DisplayName("throws OBException when the BP has no currency")
    void throwsWhenBPCurrencyIsNull() {
      BusinessPartner bp = mockBusinessPartner(null, CREDIT_USED);
      Invoice invoice = mockInvoice(bp, usd, true);
      FIN_PaymentScheduleDetail psd = mockInvoicePsd(invoice, true);

      assertThrows(OBException.class, () -> FIN_Utility.updatePaymentAmounts(psd));
    }

    @Test
    @DisplayName("credit schedule detail without invoice or order is not converted")
    void creditScheduleDetailIsNotConverted() {
      BusinessPartner bp = mockBusinessPartner(usd, new BigDecimal("500.00"));
      FIN_PaymentScheduleDetail psd = mock(FIN_PaymentScheduleDetail.class);
      FIN_PaymentDetail paymentDetail = mock(FIN_PaymentDetail.class);
      FIN_Payment payment = mock(FIN_Payment.class);
      when(payment.getBusinessPartner()).thenReturn(bp);
      when(payment.isReceipt()).thenReturn(true);
      when(paymentDetail.getFinPayment()).thenReturn(payment);
      when(paymentDetail.isPrepayment()).thenReturn(true);
      when(psd.getPaymentDetails()).thenReturn(paymentDetail);
      when(psd.getAmount()).thenReturn(PSD_AMOUNT);

      FIN_Utility.updatePaymentAmounts(psd);

      assertAmountEquals("379", capturedCreditUsed(bp));
      financialUtilsStatic.verifyNoInteractions();
    }
  }

  // ── restorePaidAmounts ───────────────────────────────────────────────

  @Nested
  @DisplayName("restorePaidAmounts")
  class RestorePaidAmounts extends StaticMocksFixture {

    @Test
    @DisplayName("receipt restores the converted amount including write-off")
    void receiptIncreasesCreditByConvertedAmount() {
      BusinessPartner bp = mockBusinessPartner(ars, BigDecimal.ZERO);
      Invoice invoice = mockInvoice(bp, usd, true);
      mockConversion(PSD_AMOUNT.add(PSD_WRITEOFF), usd, ars, CONVERTED_AMOUNT_WITH_WRITEOFF);
      FIN_PaymentScheduleDetail psd = mockInvoicePsd(invoice, true);

      FIN_Utility.restorePaidAmounts(psd);

      assertAmountEquals("187500", capturedCreditUsed(bp));
      verify(psd).setInvoicePaid(false);
    }

    @Test
    @DisplayName("payment out decreases the credit by the converted amount")
    void paymentOutDecreasesCreditByConvertedAmount() {
      BusinessPartner bp = mockBusinessPartner(ars, new BigDecimal("187500.00"));
      Invoice invoice = mockInvoice(bp, usd, false);
      mockConversion(PSD_AMOUNT.add(PSD_WRITEOFF), usd, ars, CONVERTED_AMOUNT_WITH_WRITEOFF);
      FIN_PaymentScheduleDetail psd = mockInvoicePsd(invoice, false);

      FIN_Utility.restorePaidAmounts(psd);

      assertAmountEquals("0", capturedCreditUsed(bp));
    }

    @Test
    @DisplayName("same currency keeps the unconverted behavior")
    void sameCurrencyIsUnchanged() {
      BusinessPartner bp = mockBusinessPartner(usd, new BigDecimal("10.00"));
      Invoice invoice = mockInvoice(bp, usd, true);
      FIN_PaymentScheduleDetail psd = mockInvoicePsd(invoice, true);

      FIN_Utility.restorePaidAmounts(psd);

      assertAmountEquals("135", capturedCreditUsed(bp));
      financialUtilsStatic.verifyNoInteractions();
    }

    @Test
    @DisplayName("generated credit schedule detail is not converted")
    void generatedCreditIsNotConverted() {
      BusinessPartner bp = mockBusinessPartner(usd, new BigDecimal("10.00"));
      FIN_PaymentScheduleDetail psd = mock(FIN_PaymentScheduleDetail.class);
      FIN_PaymentDetail paymentDetail = mock(FIN_PaymentDetail.class);
      FIN_Payment payment = mock(FIN_Payment.class);
      when(payment.getBusinessPartner()).thenReturn(bp);
      when(payment.isReceipt()).thenReturn(true);
      when(paymentDetail.getFinPayment()).thenReturn(payment);
      when(paymentDetail.isRefund()).thenReturn(false);
      when(psd.getPaymentDetails()).thenReturn(paymentDetail);
      when(psd.getAmount()).thenReturn(PSD_AMOUNT);
      when(psd.getWriteoffAmount()).thenReturn(BigDecimal.ZERO);

      FIN_Utility.restorePaidAmounts(psd);

      assertAmountEquals("131", capturedCreditUsed(bp));
      financialUtilsStatic.verifyNoInteractions();
    }

    @Test
    @DisplayName("the payment schedule is restored with the raw amounts")
    void paymentScheduleIsRestoredWithRawAmounts() {
      BusinessPartner bp = mockBusinessPartner(ars, BigDecimal.ZERO);
      Invoice invoice = mockInvoice(bp, usd, true);
      mockConversion(PSD_AMOUNT.add(PSD_WRITEOFF), usd, ars, CONVERTED_AMOUNT_WITH_WRITEOFF);
      FIN_PaymentScheduleDetail psd = mockInvoicePsd(invoice, true);

      FIN_Utility.restorePaidAmounts(psd);

      FIN_PaymentDetail paymentDetail = psd.getPaymentDetails();
      FIN_PaymentSchedule schedule = psd.getInvoicePaymentSchedule();
      addPaymentStatic.verify(
          () -> FIN_AddPayment.updatePaymentScheduleAmounts(eq(paymentDetail), eq(schedule), eq(PSD_AMOUNT.negate()),
              eq(PSD_WRITEOFF.negate())));
    }
  }
}
