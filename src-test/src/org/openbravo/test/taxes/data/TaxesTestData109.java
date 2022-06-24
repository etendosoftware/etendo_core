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
 * All portions are Copyright (C) 2015-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.taxes.data;

import java.math.BigDecimal;
import java.util.HashMap;

public class TaxesTestData109 extends TaxesTestData {

  @Override
  public void initialize() {

    // Header info
    setTaxDocumentLevel(true);
    setPriceIncludingTaxes(true);

    // Line info
    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setTaxid(TaxDataConstants.TAX_SUPER);
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setQuantity(new BigDecimal("10"));
    line1.setPrice(new BigDecimal("10"));
    line1.setQuantityUpdated(new BigDecimal("20"));
    line1.setPriceUpdated(new BigDecimal("10"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes1 = new HashMap<String, String[]>();
    lineTaxes1.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "80.00", "20.00", "80.00", "20.00", "160.00", "40.00", "160.00", "40.00" });
    lineTaxes1.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "53.33", "26.67", "53.33", "26.67", "106.67", "53.33", "106.67", "53.33" });
    line1.setLinetaxes(lineTaxes1);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "100.00", "53.33", "100.00", "53.33", "200.00", "106.67",
        "200.00", "106.67" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_SUPER);
    line2.setQuantity(new BigDecimal("5"));
    line2.setPrice(new BigDecimal("1.99"));
    line2.setQuantityUpdated(new BigDecimal("10"));
    line2.setPriceUpdated(new BigDecimal("1.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> line2Taxes = new HashMap<String, String[]>();
    line2Taxes.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "7.96", "1.99", "7.96", "1.99", "15.92", "3.98", "15.92", "3.98" });
    line2Taxes.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "5.31", "2.65", "5.31", "2.65", "10.61", "5.31", "10.61", "5.31" });
    line2.setLinetaxes(line2Taxes);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "9.95", "5.31", "9.95", "5.31", "19.90", "10.61",
        "19.90", "10.61" };
    line2.setLineAmounts(lineAmounts2);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2 });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "87.96", "21.99", "87.96", "21.99", "175.92", "43.98", "175.92", "43.98" });
    taxes.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "58.64", "29.32", "58.64", "29.32", "117.28", "58.64", "117.28", "58.64" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "109.95", "58.64", "109.95", "58.64", "219.90", "117.28",
        "219.90", "117.28" };
    setDocAmounts(amounts);
  }
}
