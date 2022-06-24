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

public class TaxesTestData103 extends TaxesTestData {

  @Override
  public void initialize() {
    // Header info
    setTaxDocumentLevel(false);
    setPriceIncludingTaxes(true);

    // Line info
    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setTaxid(TaxDataConstants.TAX_SUPER);
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setQuantity(new BigDecimal("1"));
    line1.setPrice(new BigDecimal("1.49"));
    line1.setQuantityUpdated(new BigDecimal("2"));
    line1.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert,
    // taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> linesTaxes = new HashMap<String, String[]>();
    linesTaxes.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "1.19", "0.30", "1.19", "0.30", "2.38", "0.60", "2.38", "0.60" });
    linesTaxes.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "0.79", "0.40", "0.79", "0.40", "1.59", "0.79", "1.59", "0.79" });
    line1.setLinetaxes(linesTaxes);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "1.49", "0.79", "1.49", "0.79", "2.98", "1.59", "2.98",
        "1.59" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_SUPER);
    line2.setQuantity(new BigDecimal("1"));
    line2.setPrice(new BigDecimal("1.49"));
    line2.setQuantityUpdated(new BigDecimal("2"));
    line2.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes2 = new HashMap<String, String[]>();
    lineTaxes2.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "1.19", "0.30", "1.19", "0.30", "2.38", "0.60", "2.38", "0.60" });
    lineTaxes2.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "0.79", "0.40", "0.79", "0.40", "1.59", "0.79", "1.59", "0.79" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "1.49", "0.79", "1.49", "0.79", "2.98", "1.59", "2.98",
        "1.59" };
    line2.setLineAmounts(lineAmounts2);

    // Line info
    TaxesLineTestData line3 = new TaxesLineTestData();
    line3.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line3.setTaxid(TaxDataConstants.TAX_SUPER);
    line3.setQuantity(new BigDecimal("1"));
    line3.setPrice(new BigDecimal("1.49"));
    line3.setQuantityUpdated(new BigDecimal("2"));
    line3.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes3 = new HashMap<String, String[]>();
    lineTaxes3.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "1.19", "0.30", "1.19", "0.30", "2.38", "0.60", "2.38", "0.60" });
    lineTaxes3.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "0.79", "0.40", "0.79", "0.40", "1.59", "0.79", "1.59", "0.79" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "1.49", "0.79", "1.49", "0.79", "2.98", "1.59", "2.98",
        "1.59" };
    line3.setLineAmounts(lineAmounts3);

    // Line info
    TaxesLineTestData line4 = new TaxesLineTestData();
    line4.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line4.setTaxid(TaxDataConstants.TAX_SUPER);
    line4.setQuantity(new BigDecimal("1"));
    line4.setPrice(new BigDecimal("1.49"));
    line4.setQuantityUpdated(new BigDecimal("2"));
    line4.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes4 = new HashMap<String, String[]>();
    lineTaxes4.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "1.19", "0.30", "1.19", "0.30", "2.38", "0.60", "2.38", "0.60" });
    lineTaxes4.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "0.79", "0.40", "0.79", "0.40", "1.59", "0.79", "1.59", "0.79" });
    line4.setLinetaxes(lineTaxes4);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts4 = new String[] { "1.49", "0.79", "1.49", "0.79", "2.98", "1.59", "2.98",
        "1.59" };
    line4.setLineAmounts(lineAmounts4);

    // Line info
    TaxesLineTestData line5 = new TaxesLineTestData();
    line5.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line5.setTaxid(TaxDataConstants.TAX_SUPER);
    line5.setQuantity(new BigDecimal("1"));
    line5.setPrice(new BigDecimal("1.49"));
    line5.setQuantityUpdated(new BigDecimal("2"));
    line5.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes5 = new HashMap<String, String[]>();
    lineTaxes5.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "1.19", "0.30", "1.19", "0.30", "2.38", "0.60", "2.38", "0.60" });
    lineTaxes5.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "0.79", "0.40", "0.79", "0.40", "1.59", "0.79", "1.59", "0.79" });
    line5.setLinetaxes(lineTaxes5);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts5 = new String[] { "1.49", "0.79", "1.49", "0.79", "2.98", "1.59", "2.98",
        "1.59" };
    line5.setLineAmounts(lineAmounts5);

    // Line info
    TaxesLineTestData line6 = new TaxesLineTestData();
    line6.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line6.setTaxid(TaxDataConstants.TAX_SUPER);
    line6.setQuantity(new BigDecimal("1"));
    line6.setPrice(new BigDecimal("1.49"));
    line6.setQuantityUpdated(new BigDecimal("2"));
    line6.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes6 = new HashMap<String, String[]>();
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "1.19", "0.30", "1.19", "0.30", "2.38", "0.60", "2.38", "0.60" });
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "0.79", "0.40", "0.79", "0.40", "1.59", "0.79", "1.59", "0.79" });
    line6.setLinetaxes(lineTaxes6);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts6 = new String[] { "1.49", "0.79", "1.49", "0.79", "2.98", "1.59", "2.98",
        "1.59" };
    line6.setLineAmounts(lineAmounts6);

    // Line info
    TaxesLineTestData line7 = new TaxesLineTestData();
    line7.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line7.setTaxid(TaxDataConstants.TAX_SUPER);
    line7.setQuantity(new BigDecimal("1"));
    line7.setPrice(new BigDecimal("1.49"));
    line7.setQuantityUpdated(new BigDecimal("2"));
    line7.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes7 = new HashMap<String, String[]>();
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "1.19", "0.30", "1.19", "0.30", "2.38", "0.60", "2.38", "0.60" });
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "0.79", "0.40", "0.79", "0.40", "1.59", "0.79", "1.59", "0.79" });
    line7.setLinetaxes(lineTaxes7);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts7 = new String[] { "1.49", "0.79", "1.49", "0.79", "2.98", "1.59", "2.98",
        "1.59" };
    line7.setLineAmounts(lineAmounts7);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3, line4, line5, line6, line7 });

    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "8.33", "2.10", "8.33", "2.10", "16.66", "4.20", "16.66", "4.20" });
    taxes.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "5.53", "2.80", "5.53", "2.80", "11.13", "5.53", "11.13", "5.53" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "10.43", "5.53", "10.43", "5.53", "20.86", "11.13", "20.86",
        "11.13" };
    setDocAmounts(amounts);

  }
}
