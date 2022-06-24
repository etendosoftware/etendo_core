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

public class TaxesTestData145 extends TaxesTestData {

  @Override
  public void initialize() {

    // Header info
    setTaxDocumentLevel(true);
    setPriceIncludingTaxes(false);

    // Line info
    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setTaxid(TaxDataConstants.TAX_VAT_4);
    line1.setQuantity(new BigDecimal("4"));
    line1.setPrice(new BigDecimal("15"));
    line1.setQuantityUpdated(new BigDecimal("8"));
    line1.setPriceUpdated(new BigDecimal("15"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes1 = new HashMap<String, String[]>();
    lineTaxes1.put(TaxDataConstants.TAX_VAT_4,
        new String[] { "60.00", "2.40", "60.00", "2.40", "120.00", "4.80", "120.00", "4.80" });
    line1.setLinetaxes(lineTaxes1);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "60.00", "60.00", "60.00", "60.00", "120.00", "120.00",
        "120.00", "120.00" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_VAT_21);
    line2.setQuantity(new BigDecimal("3"));
    line2.setPrice(new BigDecimal("9.9"));
    line2.setQuantityUpdated(new BigDecimal("6"));
    line2.setPriceUpdated(new BigDecimal("9.9"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes2 = new HashMap<String, String[]>();
    lineTaxes2.put(TaxDataConstants.TAX_VAT_21,
        new String[] { "29.70", "6.24", "29.70", "6.24", "59.40", "12.47", "59.40", "12.47" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "29.70", "29.70", "29.70", "29.70", "59.40", "59.40",
        "59.40", "59.40" };
    line2.setLineAmounts(lineAmounts2);

    // Line info
    TaxesLineTestData line3 = new TaxesLineTestData();
    line3.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line3.setTaxid(TaxDataConstants.TAX_VAT_10);
    line3.setQuantity(new BigDecimal("8"));
    line3.setPrice(new BigDecimal("1.2"));
    line3.setQuantityUpdated(new BigDecimal("16"));
    line3.setPriceUpdated(new BigDecimal("1.2"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes3 = new HashMap<String, String[]>();
    lineTaxes3.put(TaxDataConstants.TAX_VAT_10,
        new String[] { "9.60", "0.96", "9.60", "0.96", "19.20", "1.92", "19.20", "1.92" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "9.60", "9.60", "9.60", "9.60", "19.20", "19.20",
        "19.20", "19.20" };
    line3.setLineAmounts(lineAmounts3);

    // Line info
    TaxesLineTestData line4 = new TaxesLineTestData();
    line4.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line4.setTaxid(TaxDataConstants.TAX_VAT_21);
    line4.setQuantity(new BigDecimal("4"));
    line4.setPrice(new BigDecimal("109.9"));
    line4.setQuantityUpdated(new BigDecimal("8"));
    line4.setPriceUpdated(new BigDecimal("109.9"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes4 = new HashMap<String, String[]>();
    lineTaxes4.put(TaxDataConstants.TAX_VAT_21, new String[] { "439.60", "92.32", "439.60", "92.32",
        "879.20", "184.63", "879.20", "184.63" });
    line4.setLinetaxes(lineTaxes4);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts4 = new String[] { "439.60", "439.60", "439.60", "439.60", "879.20",
        "879.20", "879.20", "879.20" };
    line4.setLineAmounts(lineAmounts4);

    // Line info
    TaxesLineTestData line5 = new TaxesLineTestData();
    line5.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line5.setTaxid(TaxDataConstants.TAX_VAT_10);
    line5.setQuantity(new BigDecimal("4"));
    line5.setPrice(new BigDecimal("110.5"));
    line5.setQuantityUpdated(new BigDecimal("8"));
    line5.setPriceUpdated(new BigDecimal("110.5"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes5 = new HashMap<String, String[]>();
    lineTaxes5.put(TaxDataConstants.TAX_VAT_10, new String[] { "442.00", "44.20", "442.00", "44.20",
        "884.00", "88.40", "884.00", "88.40" });
    line5.setLinetaxes(lineTaxes5);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts5 = new String[] { "442.00", "442.00", "442.00", "442.00", "884.00",
        "884.00", "884.00", "884.00" };
    line5.setLineAmounts(lineAmounts5);

    // Line info
    TaxesLineTestData line6 = new TaxesLineTestData();
    line6.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line6.setTaxid(TaxDataConstants.TAX_SUPER);
    line6.setQuantity(new BigDecimal("5"));
    line6.setPrice(new BigDecimal("1.99"));
    line6.setQuantityUpdated(new BigDecimal("10"));
    line6.setPriceUpdated(new BigDecimal("1.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes6 = new HashMap<String, String[]>();
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "14.93", "3.73", "14.93", "3.73", "29.85", "7.46", "29.85", "7.46" });
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "9.95", "4.98", "9.95", "4.98", "19.90", "9.95", "19.90", "9.95" });
    line6.setLinetaxes(lineTaxes6);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts6 = new String[] { "9.95", "9.95", "9.95", "9.95", "19.90", "19.90",
        "19.90", "19.90" };
    line6.setLineAmounts(lineAmounts6);

    // Line info
    TaxesLineTestData line7 = new TaxesLineTestData();
    line7.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line7.setTaxid(TaxDataConstants.TAX_SUPER);
    line7.setQuantity(new BigDecimal("5"));
    line7.setPrice(new BigDecimal("1.49"));
    line7.setQuantityUpdated(new BigDecimal("10"));
    line7.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes7 = new HashMap<String, String[]>();
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "11.18", "2.80", "11.18", "2.80", "22.35", "5.59", "22.35", "5.59" });
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "7.45", "3.73", "7.45", "3.73", "14.90", "7.45", "14.90", "7.45" });
    line7.setLinetaxes(lineTaxes7);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts7 = new String[] { "7.45", "7.45", "7.45", "7.45", "14.90", "14.90",
        "14.90", "14.90" };
    line7.setLineAmounts(lineAmounts7);

    // Line info
    TaxesLineTestData line8 = new TaxesLineTestData();
    line8.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line8.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line8.setQuantity(new BigDecimal("4"));
    line8.setPrice(new BigDecimal("55.8"));
    line8.setQuantityUpdated(new BigDecimal("8"));
    line8.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes8 = new HashMap<String, String[]>();
    lineTaxes8.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "282.35", "45.18", "282.35",
        "45.18", "564.70", "90.35", "564.70", "90.35" });
    lineTaxes8.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "223.20", "59.15", "223.20",
        "59.15", "446.40", "118.30", "446.40", "118.30" });
    line8.setLinetaxes(lineTaxes8);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts8 = new String[] { "223.20", "223.20", "223.20", "223.20", "446.40",
        "446.40", "446.40", "446.40" };
    line8.setLineAmounts(lineAmounts8);

    // Line info
    TaxesLineTestData line9 = new TaxesLineTestData();
    line9.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line9.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line9.setQuantity(new BigDecimal("5"));
    line9.setPrice(new BigDecimal("55.8"));
    line9.setQuantityUpdated(new BigDecimal("10"));
    line9.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes9 = new HashMap<String, String[]>();
    lineTaxes9.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "352.94", "56.47", "352.94",
        "56.47", "705.87", "112.94", "705.87", "112.94" });
    lineTaxes9.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "279.00", "73.94", "279.00",
        "73.94", "558.00", "147.87", "558.00", "147.87" });
    line9.setLinetaxes(lineTaxes9);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts9 = new String[] { "279.00", "279.00", "279.00", "279.00", "558.00",
        "558.00", "558.00", "558.00" };
    line9.setLineAmounts(lineAmounts9);

    // Line info
    TaxesLineTestData line10 = new TaxesLineTestData();
    line10.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line10.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line10.setQuantity(new BigDecimal("6"));
    line10.setPrice(new BigDecimal("55.8"));
    line10.setQuantityUpdated(new BigDecimal("12"));
    line10.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes10 = new HashMap<String, String[]>();
    lineTaxes10.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "423.52", "67.76", "423.52",
        "67.76", "847.04", "135.53", "847.04", "135.53" });
    lineTaxes10.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "334.80", "88.72", "334.80",
        "88.72", "669.60", "177.44", "669.60", "177.44" });
    line10.setLinetaxes(lineTaxes10);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts10 = new String[] { "334.80", "334.80", "334.80", "334.80", "669.60",
        "669.60", "669.60", "669.60" };
    line10.setLineAmounts(lineAmounts10);

    // Line info
    TaxesLineTestData line11 = new TaxesLineTestData();
    line11.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line11.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line11.setQuantity(new BigDecimal("4"));
    line11.setPrice(new BigDecimal("294.99"));
    line11.setQuantityUpdated(new BigDecimal("8"));
    line11.setPriceUpdated(new BigDecimal("294.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes11 = new HashMap<String, String[]>();
    lineTaxes11.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "1805.34", "288.85", "1805.34",
        "288.85", "3610.68", "577.71", "3610.68", "577.71" });
    lineTaxes11.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "1179.96", "625.38", "1179.96",
        "625.38", "2359.92", "1250.76", "2359.92", "1250.76" });
    line11.setLinetaxes(lineTaxes11);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts11 = new String[] { "1179.96", "1179.96", "1179.96", "1179.96", "2359.92",
        "2359.92", "2359.92", "2359.92" };
    line11.setLineAmounts(lineAmounts11);

    // Line info
    TaxesLineTestData line12 = new TaxesLineTestData();
    line12.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line12.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line12.setQuantity(new BigDecimal("4"));
    line12.setPrice(new BigDecimal("254.95"));
    line12.setQuantityUpdated(new BigDecimal("8"));
    line12.setPriceUpdated(new BigDecimal("254.95"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes12 = new HashMap<String, String[]>();
    lineTaxes12.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "1560.29", "249.65", "1560.29",
        "249.65", "3120.59", "499.29", "3120.59", "499.29" });
    lineTaxes12.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "1019.80", "540.49", "1019.80",
        "540.49", "2039.60", "1080.99", "2039.60", "1080.99" });
    line12.setLinetaxes(lineTaxes12);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts12 = new String[] { "1019.80", "1019.80", "1019.80", "1019.80", "2039.60",
        "2039.60", "2039.60", "2039.60" };
    line12.setLineAmounts(lineAmounts12);

    // Line info
    TaxesLineTestData line13 = new TaxesLineTestData();
    line13.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line13.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line13.setQuantity(new BigDecimal("4"));
    line13.setPrice(new BigDecimal("299.36"));
    line13.setQuantityUpdated(new BigDecimal("8"));
    line13.setPriceUpdated(new BigDecimal("299.36"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes13 = new HashMap<String, String[]>();
    lineTaxes13.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "1832.08", "293.13", "1832.08",
        "293.13", "3664.17", "586.27", "3664.17", "586.27" });
    lineTaxes13.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "1197.44", "634.64", "1197.44",
        "634.64", "2394.88", "1269.29", "2394.88", "1269.29" });
    line13.setLinetaxes(lineTaxes13);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts13 = new String[] { "1197.44", "1197.44", "1197.44", "1197.44", "2394.88",
        "2394.88", "2394.88", "2394.88" };
    line13.setLineAmounts(lineAmounts13);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3, line4, line5, line6, line7, line8,
        line9, line10, line11, line12, line13 });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_VAT_21, new String[] { "469.30", "98.55", "469.30", "98.55",
        "938.60", "197.11", "938.60", "197.11" });
    taxes.put(TaxDataConstants.TAX_VAT_10, new String[] { "451.60", "45.16", "451.60", "45.16",
        "903.20", "90.32", "903.20", "90.32" });
    taxes.put(TaxDataConstants.TAX_VAT_4,
        new String[] { "60.00", "2.40", "60.00", "2.40", "120.00", "4.80", "120.00", "4.80" });
    taxes.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "26.10", "6.53", "26.10", "6.53", "52.20", "13.05", "52.20", "13.05" });
    taxes.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "17.40", "8.70", "17.40", "8.70", "34.80", "17.40", "34.80", "17.40" });
    taxes.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "1058.81", "169.41", "1058.81",
        "169.41", "2117.61", "338.82", "2117.61", "338.82" });
    taxes.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "837.00", "221.81", "837.00",
        "221.81", "1674.00", "443.61", "1674.00", "443.61" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "5197.72", "831.64", "5197.72",
        "831.64", "10395.43", "1663.27", "10395.43", "1663.27" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "3397.20", "1800.52", "3397.20",
        "1800.52", "6794.40", "3601.03", "6794.40", "3601.03" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "8417.22", "5232.50", "8417.22", "5232.50", "16834.41",
        "10465.00", "16834.41", "10465.00" };
    setDocAmounts(amounts);

  }
}
