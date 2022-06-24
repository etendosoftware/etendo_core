/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.taxes.data;

import java.math.BigDecimal;
import java.util.HashMap;

public class TaxesTestData128 extends TaxesTestData {

  @Override
  public void initialize() {

    // Header info
    setTaxDocumentLevel(false);
    setPriceIncludingTaxes(true);

    // Line info
    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line1.setQuantity(new BigDecimal("-333"));
    line1.setPrice(new BigDecimal("294.99"));
    line1.setQuantityUpdated(new BigDecimal("-666"));
    line1.setPriceUpdated(new BigDecimal("294.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes1 = new HashMap<String, String[]>();
    lineTaxes1.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-84682.47", "-13549.20",
        "-84682.47", "-13549.20", "-169364.95", "-27098.39", "-169364.95", "-27098.39" });
    lineTaxes1.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-55348.02", "-29334.45",
        "-55348.02", "-29334.45", "-110696.04", "-58668.91", "-110696.04", "-58668.91" });
    line1.setLinetaxes(lineTaxes1);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "-98231.67", "-55348.02", "-98231.67", "-55348.02",
        "-196463.34", "-110696.04", "-196463.34", "-110696.04" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line2.setQuantity(new BigDecimal("-333"));
    line2.setPrice(new BigDecimal("254.95"));
    line2.setQuantityUpdated(new BigDecimal("-666"));
    line2.setPriceUpdated(new BigDecimal("254.95"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes2 = new HashMap<String, String[]>();
    lineTaxes2.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-73188.24", "-11710.11",
        "-73188.24", "-11710.11", "-146376.46", "-23420.24", "-146376.46", "-23420.24" });
    lineTaxes2.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-47835.45", "-25352.79",
        "-47835.45", "-25352.79", "-95670.89", "-50705.57", "-95670.89", "-50705.57" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "-84898.35", "-47835.45", "-84898.35", "-47835.45",
        "-169796.70", "-95670.89", "-169796.70", "-95670.89" };
    line2.setLineAmounts(lineAmounts2);

    // Line info
    TaxesLineTestData line3 = new TaxesLineTestData();
    line3.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line3.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line3.setQuantity(new BigDecimal("-333"));
    line3.setPrice(new BigDecimal("299.36"));
    line3.setQuantityUpdated(new BigDecimal("-666"));
    line3.setPriceUpdated(new BigDecimal("299.36"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes3 = new HashMap<String, String[]>();
    lineTaxes3.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-85936.96", "-13749.92",
        "-85936.96", "-13749.92", "-171873.93", "-27499.83", "-171873.93", "-27499.83" });
    lineTaxes3.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-56167.95", "-29769.01",
        "-56167.95", "-29769.01", "-112335.90", "-59538.03", "-112335.90", "-59538.03" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "-99686.88", "-56167.95", "-99686.88", "-56167.95",
        "-199373.76", "-112335.90", "-199373.76", "-112335.90" };
    line3.setLineAmounts(lineAmounts3);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3 });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-243807.67", "-39009.23",
        "-243807.67", "-39009.23", "-487615.34", "-78018.46", "-487615.34", "-78018.46" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-159351.42", "-84456.25",
        "-159351.42", "-84456.25", "-318702.83", "-168912.51", "-318702.83", "-168912.51" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "-282816.90", "-159351.42", "-282816.90", "-159351.42",
        "-565633.80", "-318702.83", "-565633.80", "-318702.83" };
    setDocAmounts(amounts);
  }
}
