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

public class TaxesTestData123 extends TaxesTestData {

  @Override
  public void initialize() {

    // Header info
    setTaxDocumentLevel(false);
    setPriceIncludingTaxes(false);

    // Line info
    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line1.setQuantity(new BigDecimal("333"));
    line1.setPrice(new BigDecimal("294.99"));
    line1.setQuantityUpdated(new BigDecimal("666"));
    line1.setPriceUpdated(new BigDecimal("294.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes1 = new HashMap<String, String[]>();
    lineTaxes1.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "150294.46", "24047.11",
        "150294.46", "24047.11", "300588.91", "48094.23", "300588.91", "48094.23" });
    lineTaxes1.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "98231.67", "52062.79",
        "98231.67", "52062.79", "196463.34", "104125.57", "196463.34", "104125.57" });
    line1.setLinetaxes(lineTaxes1);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "98231.67", "98231.67", "98231.67", "98231.67",
        "196463.34", "196463.34", "196463.34", "196463.34" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line2.setQuantity(new BigDecimal("333"));
    line2.setPrice(new BigDecimal("254.95"));
    line2.setQuantityUpdated(new BigDecimal("666"));
    line2.setPriceUpdated(new BigDecimal("254.95"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes2 = new HashMap<String, String[]>();
    lineTaxes2.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "129894.48", "20783.12",
        "129894.48", "20783.12", "259788.95", "41566.23", "259788.95", "41566.23" });
    lineTaxes2.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "84898.35", "44996.13",
        "84898.35", "44996.13", "169796.70", "89992.25", "169796.70", "89992.25" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "84898.35", "84898.35", "84898.35", "84898.35",
        "169796.70", "169796.70", "169796.70", "169796.70" };
    line2.setLineAmounts(lineAmounts2);

    // Line info
    TaxesLineTestData line3 = new TaxesLineTestData();
    line3.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line3.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line3.setQuantity(new BigDecimal("333"));
    line3.setPrice(new BigDecimal("299.36"));
    line3.setQuantityUpdated(new BigDecimal("666"));
    line3.setPriceUpdated(new BigDecimal("299.36"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes3 = new HashMap<String, String[]>();
    lineTaxes3.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "152520.93", "24403.35",
        "152520.93", "24403.35", "305041.85", "48806.70", "305041.85", "48806.70" });
    lineTaxes3.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "99686.88", "52834.05",
        "99686.88", "52834.05", "199373.76", "105668.09", "199373.76", "105668.09" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "99686.88", "99686.88", "99686.88", "99686.88",
        "199373.76", "199373.76", "199373.76", "199373.76" };
    line3.setLineAmounts(lineAmounts3);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3 });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "432709.87", "69233.58", "432709.87",
        "69233.58", "865419.71", "138467.16", "865419.71", "138467.16" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "282816.90", "149892.97", "282816.90",
        "149892.97", "565633.80", "299785.91", "565633.80", "299785.91" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "501943.45", "282816.90", "501943.45", "282816.90",
        "1003886.87", "565633.80", "1003886.87", "565633.80" };
    setDocAmounts(amounts);
  }
}
