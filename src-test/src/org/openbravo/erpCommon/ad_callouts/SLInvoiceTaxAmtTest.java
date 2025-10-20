package org.openbravo.erpCommon.ad_callouts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout.CalloutInfo;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Unit tests for {@link SL_InvoiceTax_Amt}.
 * All monetary values are rounded according to the returned {@code priceprecision}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SLInvoiceTaxAmtTest {

  private static final String TAX1 = "TAX-1";
  private static final String INV1 = "INV-1";
  private static final String INPTAXAMT = "inptaxamt";
  private static final String INPTAXBASEAMT = "inptaxbaseamt";

  @Mock
  private CalloutInfo info;
  private SL_InvoiceTax_Amt callout;

  /**
   * Initializes the callout under test before each execution.
   */
  @Before
  public void setUp() {
    callout = new SL_InvoiceTax_Amt();
    lenient().when(info.getStringParameter(eq("inpcTaxId"), any())).thenReturn(TAX1);
    lenient().when(info.getStringParameter(eq("inpcInvoiceId"), any())).thenReturn(INV1);
  }

  /**
   * Provides a single-row {@link SLInvoiceTaxAmtData} array with the specified rate and precision.
   * @param ratePercent tax rate as a percentage string (e.g., "21" for 21%)
   * @param pricePrecisionStr price precision as a string (e.g., "2")
   * @return an array containing one configured {@link SLInvoiceTaxAmtData} instance
   */
  private SLInvoiceTaxAmtData[] data(String ratePercent, String pricePrecisionStr) {
    SLInvoiceTaxAmtData row = new SLInvoiceTaxAmtData();
    row.rate = ratePercent;
    row.priceprecision = pricePrecisionStr;
    return new SLInvoiceTaxAmtData[] { row };
  }

  /**
   * Verifies that when the last changed field is {@code inptaxamt} and the manual tax amount
   * differs from the system amount by exactly 0.01, the callout does not add a WARNING and
   * returns the scaled {@code inptaxamt}.
   * @throws ServletException if the callout fails to execute
   */
  @Test
  public void testEditTaxAmtWithinToleranceNoWarning() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn(INPTAXAMT);
    when(info.getBigDecimalParameter(INPTAXBASEAMT)).thenReturn(new BigDecimal("100"));
    when(info.getBigDecimalParameter(INPTAXAMT)).thenReturn(new BigDecimal("21.01"));
    try (MockedStatic<SLInvoiceTaxAmtData> mockedData = mockStatic(SLInvoiceTaxAmtData.class)) {
      mockedData.when(() -> SLInvoiceTaxAmtData.select(callout, TAX1, INV1))
        .thenReturn(data("21", "2"));
      callout.execute(info);
      verify(info, never()).addResult(eq("WARNING"), any());
      verify(info).addResult(INPTAXAMT, new BigDecimal("21.01"));
    }
  }

  /**
   * Verifies that when the last changed field is {@code inptaxamt} and the manual tax amount
   * exceeds the ±0.01 tolerance, the callout adds a {@code WARNING} with the translated
   * message and returns the scaled {@code inptaxamt}.
   * @throws ServletException if the callout fails to execute
   */
  @Test
  public void testEditTaxAmtExceedsToleranceAddsWarning() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn(INPTAXAMT);
    when(info.getBigDecimalParameter(INPTAXBASEAMT)).thenReturn(new BigDecimal("100"));
    when(info.getBigDecimalParameter(INPTAXAMT)).thenReturn(new BigDecimal("21.03"));
    try (MockedStatic<SLInvoiceTaxAmtData> mockedData = mockStatic(SLInvoiceTaxAmtData.class);
         MockedStatic<OBMessageUtils> mockedMsg = mockStatic(OBMessageUtils.class)) {
      mockedData.when(() -> SLInvoiceTaxAmtData.select(callout, TAX1, INV1))
        .thenReturn(data("21", "2"));
      mockedMsg.when(() -> OBMessageUtils.messageBD(anyString()))
        .thenReturn("ETP_TaxAdjOutOfRange");
      callout.execute(info);
      verify(info).addResult("WARNING", "ETP_TaxAdjOutOfRange");
      verify(info).addResult(INPTAXAMT, new BigDecimal("21.03"));
    }
  }

  /**
   * Verifies that when the last changed field is {@code inptaxbaseamt}, the callout recomputes
   * {@code inptaxamt} using the configured tax rate and returns both amounts properly scaled.
   * @throws ServletException if the callout fails to execute
   */
  @Test
  public void testEditBaseRecomputesTaxAmtAndScales() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn(INPTAXBASEAMT);
    when(info.getBigDecimalParameter(INPTAXBASEAMT)).thenReturn(new BigDecimal("123.456"));
    try (MockedStatic<SLInvoiceTaxAmtData> mockedData = mockStatic(SLInvoiceTaxAmtData.class)) {
      mockedData.when(() -> SLInvoiceTaxAmtData.select(callout, TAX1, INV1))
        .thenReturn(data("21", "2"));
      callout.execute(info);
      verify(info).addResult(INPTAXAMT, new BigDecimal("25.93"));
      verify(info).addResult(INPTAXBASEAMT, new BigDecimal("123.46"));
    }
  }

  /**
   * Verifies default behavior when {@code SLInvoiceTaxAmtData.select(...)} returns no rows:
   * tax rate defaults to 0% and price precision defaults to 2.
   * @throws ServletException if the callout fails to execute
   */
  @Test
  public void testNoDataDefaultsToZeroRateAndScale2() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn(INPTAXBASEAMT);
    when(info.getBigDecimalParameter(INPTAXBASEAMT)).thenReturn(new BigDecimal("100"));
    try (MockedStatic<SLInvoiceTaxAmtData> mockedData = mockStatic(SLInvoiceTaxAmtData.class)) {
      mockedData.when(() -> SLInvoiceTaxAmtData.select(callout, TAX1, INV1))
        .thenReturn(new SLInvoiceTaxAmtData[0]);
      callout.execute(info);
      verify(info).addResult(INPTAXAMT, new BigDecimal("0.00"));
      verify(info).addResult(INPTAXBASEAMT, new BigDecimal("100.00"));
    }
  }
}
