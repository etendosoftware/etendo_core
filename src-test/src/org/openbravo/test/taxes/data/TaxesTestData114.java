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

public class TaxesTestData114 extends TaxesTestData {

  @Override
  public void initialize() {
    // Header info
    setTaxDocumentLevel(true);
    setPriceIncludingTaxes(false);

    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setTaxid(TaxDataConstants.TAX_SUPER);
    line1.setQuantity(new BigDecimal("-5"));
    line1.setPrice(new BigDecimal("10"));
    line1.setQuantityUpdated(new BigDecimal("-10"));
    line1.setPriceUpdated(new BigDecimal("10"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes1 = new HashMap<String, String[]>();
    lineTaxes1.put(TaxDataConstants.TAX_SUPER_25, new String[] { "-75.00", "-18.75", "-75.00",
        "-18.75", "-150.00", "-37.50", "-150.00", "-37.50" });
    lineTaxes1.put(TaxDataConstants.TAX_SUPER_50, new String[] { "-50.00", "-25.00", "-50.00",
        "-25.00", "-100.00", "-50.00", "-100.00", "-50.00" });
    line1.setLinetaxes(lineTaxes1);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "-50.00", "-50.00", "-50.00", "-50.00", "-100.00",
        "-100.00", "-100.00", "-100.00" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_SUPER);
    line2.setQuantity(new BigDecimal("-3"));
    line2.setPrice(new BigDecimal("1.99"));
    line2.setQuantityUpdated(new BigDecimal("-6"));
    line2.setPriceUpdated(new BigDecimal("1.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes2 = new HashMap<String, String[]>();
    lineTaxes2.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "-8.96", "-2.24", "-8.96", "-2.24", "-17.91", "-4.48", "-17.91", "-4.48" });
    lineTaxes2.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-5.97", "-2.99", "-5.97", "-2.99", "-11.94", "-5.97", "-11.94", "-5.97" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "-5.97", "-5.97", "-5.97", "-5.97", "-11.94", "-11.94",
        "-11.94", "-11.94" };
    line2.setLineAmounts(lineAmounts2);

    // Line info
    TaxesLineTestData line3 = new TaxesLineTestData();
    line3.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line3.setTaxid(TaxDataConstants.TAX_SUPER);
    line3.setQuantity(new BigDecimal("-6"));
    line3.setPrice(new BigDecimal("1.49"));
    line3.setQuantityUpdated(new BigDecimal("-12"));
    line3.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes3 = new HashMap<String, String[]>();
    lineTaxes3.put(TaxDataConstants.TAX_SUPER_25, new String[] { "-13.41", "-3.35", "-13.41",
        "-3.35", "-26.82", "-6.71", "-26.82", "-6.71" });
    lineTaxes3.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-8.94", "-4.47", "-8.94", "-4.47", "-17.88", "-8.94", "-17.88", "-8.94" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "-8.94", "-8.94", "-8.94", "-8.94", "-17.88", "-17.88",
        "-17.88", "-17.88" };
    line3.setLineAmounts(lineAmounts3);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3 });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_SUPER_25, new String[] { "-97.37", "-24.34", "-97.37", "-24.34",
        "-194.73", "-48.68", "-194.73", "-48.68" });
    taxes.put(TaxDataConstants.TAX_SUPER_50, new String[] { "-64.91", "-32.46", "-64.91", "-32.46",
        "-129.82", "-64.91", "-129.82", "-64.91" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "-121.71", "-64.91", "-121.71", "-64.91", "-243.41",
        "-129.82", "-243.41", "-129.82" };
    setDocAmounts(amounts);
  }
}
