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

public class TaxesTestData117 extends TaxesTestData {

  @Override
  public void initialize() {
    // Header info
    setTaxDocumentLevel(true);
    setPriceIncludingTaxes(true);

    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setTaxid(TaxDataConstants.TAX_SUPER);
    line1.setQuantity(new BigDecimal("5"));
    line1.setPrice(new BigDecimal("10"));
    line1.setQuantityUpdated(new BigDecimal("10"));
    line1.setPriceUpdated(new BigDecimal("10"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes1 = new HashMap<String, String[]>();
    lineTaxes1.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "40.00", "10.00", "40.00", "10.00", "80.00", "20.00", "80.00", "20.00" });
    lineTaxes1.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "26.67", "13.33", "26.67", "13.33", "53.33", "26.67", "53.33", "26.67" });
    line1.setLinetaxes(lineTaxes1);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "50.00", "26.67", "50.00", "26.67", "100.00", "53.33",
        "100.00", "53.33" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_SUPER);
    line2.setQuantity(new BigDecimal("3"));
    line2.setPrice(new BigDecimal("1.99"));
    line2.setQuantityUpdated(new BigDecimal("6"));
    line2.setPriceUpdated(new BigDecimal("1.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes2 = new HashMap<String, String[]>();
    lineTaxes2.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "4.77", "1.20", "4.77", "1.20", "9.55", "2.39", "9.55", "2.39" });
    lineTaxes2.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "3.18", "1.59", "3.18", "1.59", "6.37", "3.18", "6.37", "3.18" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "5.97", "3.18", "5.97", "3.18", "11.94", "6.37", "11.94",
        "6.37" };
    line2.setLineAmounts(lineAmounts2);

    // Line info
    TaxesLineTestData line3 = new TaxesLineTestData();
    line3.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line3.setTaxid(TaxDataConstants.TAX_SUPER);
    line3.setQuantity(new BigDecimal("6"));
    line3.setPrice(new BigDecimal("1.49"));
    line3.setQuantityUpdated(new BigDecimal("12"));
    line3.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes3 = new HashMap<String, String[]>();
    lineTaxes3.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "7.15", "1.79", "7.15", "1.79", "14.31", "3.57", "14.31", "3.57" });
    lineTaxes3.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "4.77", "2.38", "4.77", "2.38", "9.54", "4.77", "9.54", "4.77" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "8.94", "4.77", "8.94", "4.77", "17.88", "9.54", "17.88",
        "9.54" };
    line3.setLineAmounts(lineAmounts3);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3 });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "51.93", "12.98", "51.93", "12.98", "103.86", "25.97", "103.86", "25.96" });
    taxes.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "34.62", "17.31", "34.62", "17.31", "69.24", "34.62", "69.24", "34.62" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "64.91", "34.62", "64.91", "34.62", "129.82", "69.24",
        "129.82", "69.24" };
    setDocAmounts(amounts);
  }
}
