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

public class TaxesTestData137 extends TaxesTestData {

  @Override
  public void initialize() {

    // Header info
    setTaxDocumentLevel(true);
    setPriceIncludingTaxes(false);

    // Line info
    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setTaxid(TaxDataConstants.TAX_VAT_4);
    line1.setQuantity(new BigDecimal("3"));
    line1.setPrice(new BigDecimal("15"));
    line1.setQuantityUpdated(new BigDecimal("6"));
    line1.setPriceUpdated(new BigDecimal("15"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes1 = new HashMap<String, String[]>();
    lineTaxes1.put(TaxDataConstants.TAX_VAT_4,
        new String[] { "45.00", "1.80", "45.00", "1.80", "90.00", "3.60", "90.00", "3.60" });
    line1.setLinetaxes(lineTaxes1);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "45.00", "45.00", "45.00", "45.00", "90.00", "90.00",
        "90.00", "90.00" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_VAT_21);
    line2.setQuantity(new BigDecimal("2"));
    line2.setPrice(new BigDecimal("9.9"));
    line2.setQuantityUpdated(new BigDecimal("4"));
    line2.setPriceUpdated(new BigDecimal("9.9"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes2 = new HashMap<String, String[]>();
    lineTaxes2.put(TaxDataConstants.TAX_VAT_21,
        new String[] { "19.80", "4.16", "19.80", "4.16", "39.60", "8.32", "39.60", "8.32" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "19.80", "19.80", "19.80", "19.80", "39.60", "39.60",
        "39.60", "39.60" };
    line2.setLineAmounts(lineAmounts2);

    // Line info
    TaxesLineTestData line3 = new TaxesLineTestData();
    line3.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line3.setTaxid(TaxDataConstants.TAX_VAT_10);
    line3.setQuantity(new BigDecimal("7"));
    line3.setPrice(new BigDecimal("1.2"));
    line3.setQuantityUpdated(new BigDecimal("14"));
    line3.setPriceUpdated(new BigDecimal("1.2"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes3 = new HashMap<String, String[]>();
    lineTaxes3.put(TaxDataConstants.TAX_VAT_10,
        new String[] { "8.40", "0.84", "8.40", "0.84", "16.80", "1.68", "16.80", "1.68" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "8.40", "8.40", "8.40", "8.40", "16.80", "16.80",
        "16.80", "16.80" };
    line3.setLineAmounts(lineAmounts3);

    // Line info
    TaxesLineTestData line4 = new TaxesLineTestData();
    line4.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line4.setTaxid(TaxDataConstants.TAX_VAT_21);
    line4.setQuantity(new BigDecimal("3"));
    line4.setPrice(new BigDecimal("109.9"));
    line4.setQuantityUpdated(new BigDecimal("6"));
    line4.setPriceUpdated(new BigDecimal("109.9"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes4 = new HashMap<String, String[]>();
    lineTaxes4.put(TaxDataConstants.TAX_VAT_21, new String[] { "329.70", "69.24", "329.70", "69.24",
        "659.40", "138.47", "659.40", "138.47" });
    line4.setLinetaxes(lineTaxes4);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts4 = new String[] { "329.70", "329.70", "329.70", "329.70", "659.40",
        "659.40", "659.40", "659.40" };
    line4.setLineAmounts(lineAmounts4);

    // Line info
    TaxesLineTestData line5 = new TaxesLineTestData();
    line5.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line5.setTaxid(TaxDataConstants.TAX_VAT_10);
    line5.setQuantity(new BigDecimal("3"));
    line5.setPrice(new BigDecimal("110.5"));
    line5.setQuantityUpdated(new BigDecimal("6"));
    line5.setPriceUpdated(new BigDecimal("110.5"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes5 = new HashMap<String, String[]>();
    lineTaxes5.put(TaxDataConstants.TAX_VAT_10, new String[] { "331.50", "33.15", "331.50", "33.15",
        "663.00", "66.30", "663.00", "66.30" });
    line5.setLinetaxes(lineTaxes5);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts5 = new String[] { "331.50", "331.50", "331.50", "331.50", "663.00",
        "663.00", "663.00", "663.00" };
    line5.setLineAmounts(lineAmounts5);

    // Line info
    TaxesLineTestData line6 = new TaxesLineTestData();
    line6.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line6.setTaxid(TaxDataConstants.TAX_SUPER);
    line6.setQuantity(new BigDecimal("4"));
    line6.setPrice(new BigDecimal("1.99"));
    line6.setQuantityUpdated(new BigDecimal("8"));
    line6.setPriceUpdated(new BigDecimal("1.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes6 = new HashMap<String, String[]>();
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "11.94", "2.99", "11.94", "2.99", "23.88", "5.97", "23.88", "5.97" });
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "7.96", "3.98", "7.96", "3.98", "15.92", "7.96", "15.92", "7.96" });
    line6.setLinetaxes(lineTaxes6);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts6 = new String[] { "7.96", "7.96", "7.96", "7.96", "15.92", "15.92",
        "15.92", "15.92" };
    line6.setLineAmounts(lineAmounts6);

    // Line info
    TaxesLineTestData line7 = new TaxesLineTestData();
    line7.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line7.setTaxid(TaxDataConstants.TAX_SUPER);
    line7.setQuantity(new BigDecimal("4"));
    line7.setPrice(new BigDecimal("1.49"));
    line7.setQuantityUpdated(new BigDecimal("8"));
    line7.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes7 = new HashMap<String, String[]>();
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "8.94", "2.24", "8.94", "2.24", "17.88", "4.47", "17.88", "4.47" });
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "5.96", "2.98", "5.96", "2.98", "11.92", "5.96", "11.92", "5.96" });
    line7.setLinetaxes(lineTaxes7);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts7 = new String[] { "5.96", "5.96", "5.96", "5.96", "11.92", "11.92",
        "11.92", "11.92" };
    line7.setLineAmounts(lineAmounts7);

    // Line info
    TaxesLineTestData line8 = new TaxesLineTestData();
    line8.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line8.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line8.setQuantity(new BigDecimal("3"));
    line8.setPrice(new BigDecimal("55.8"));
    line8.setQuantityUpdated(new BigDecimal("6"));
    line8.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes8 = new HashMap<String, String[]>();
    lineTaxes8.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "211.76", "33.88", "211.76",
        "33.88", "423.52", "67.76", "423.52", "67.76" });
    lineTaxes8.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "167.40", "44.36", "167.40",
        "44.36", "334.80", "88.72", "334.80", "88.72" });
    line8.setLinetaxes(lineTaxes8);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts8 = new String[] { "167.40", "167.40", "167.40", "167.40", "334.80",
        "334.80", "334.80", "334.80" };
    line8.setLineAmounts(lineAmounts8);

    // Line info
    TaxesLineTestData line9 = new TaxesLineTestData();
    line9.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line9.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line9.setQuantity(new BigDecimal("4"));
    line9.setPrice(new BigDecimal("55.8"));
    line9.setQuantityUpdated(new BigDecimal("8"));
    line9.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes9 = new HashMap<String, String[]>();
    lineTaxes9.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "282.35", "45.18", "282.35",
        "45.18", "564.70", "90.35", "564.70", "90.35" });
    lineTaxes9.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "223.20", "59.15", "223.20",
        "59.15", "446.40", "118.30", "446.40", "118.30" });
    line9.setLinetaxes(lineTaxes9);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts9 = new String[] { "223.20", "223.20", "223.20", "223.20", "446.40",
        "446.40", "446.40", "446.40" };
    line9.setLineAmounts(lineAmounts9);

    // Line info
    TaxesLineTestData line10 = new TaxesLineTestData();
    line10.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line10.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line10.setQuantity(new BigDecimal("5"));
    line10.setPrice(new BigDecimal("55.8"));
    line10.setQuantityUpdated(new BigDecimal("10"));
    line10.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes10 = new HashMap<String, String[]>();
    lineTaxes10.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "352.94", "56.47", "352.94",
        "56.47", "705.87", "112.94", "705.87", "112.94" });
    lineTaxes10.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "279.00", "73.94", "279.00",
        "73.94", "558.00", "147.87", "558.00", "147.87" });
    line10.setLinetaxes(lineTaxes10);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts10 = new String[] { "279.00", "279.00", "279.00", "279.00", "558.00",
        "558.00", "558.00", "558.00" };
    line10.setLineAmounts(lineAmounts10);

    // Line info
    TaxesLineTestData line11 = new TaxesLineTestData();
    line11.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line11.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line11.setQuantity(new BigDecimal("3"));
    line11.setPrice(new BigDecimal("294.99"));
    line11.setQuantityUpdated(new BigDecimal("6"));
    line11.setPriceUpdated(new BigDecimal("294.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes11 = new HashMap<String, String[]>();
    lineTaxes11.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "1354.00", "216.64", "1354.00",
        "216.64", "2708.01", "433.28", "2708.01", "433.28" });
    lineTaxes11.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "884.97", "469.03", "884.97",
        "469.03", "1769.94", "938.07", "1769.94", "938.07" });
    line11.setLinetaxes(lineTaxes11);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts11 = new String[] { "884.97", "884.97", "884.97", "884.97", "1769.94",
        "1769.94", "1769.94", "1769.94" };
    line11.setLineAmounts(lineAmounts11);

    // Line info
    TaxesLineTestData line12 = new TaxesLineTestData();
    line12.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line12.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line12.setQuantity(new BigDecimal("3"));
    line12.setPrice(new BigDecimal("254.95"));
    line12.setQuantityUpdated(new BigDecimal("6"));
    line12.setPriceUpdated(new BigDecimal("254.95"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes12 = new HashMap<String, String[]>();
    lineTaxes12.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "1170.22", "187.24", "1170.22",
        "187.24", "2340.44", "374.47", "2340.44", "374.47" });
    lineTaxes12.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "764.85", "405.37", "764.85",
        "405.37", "1529.70", "810.74", "1529.70", "810.74" });
    line12.setLinetaxes(lineTaxes12);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts12 = new String[] { "764.85", "764.85", "764.85", "764.85", "1529.70",
        "1529.70", "1529.70", "1529.70" };
    line12.setLineAmounts(lineAmounts12);

    // Line info
    TaxesLineTestData line13 = new TaxesLineTestData();
    line13.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line13.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line13.setQuantity(new BigDecimal("3"));
    line13.setPrice(new BigDecimal("299.36"));
    line13.setQuantityUpdated(new BigDecimal("6"));
    line13.setPriceUpdated(new BigDecimal("299.36"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes13 = new HashMap<String, String[]>();
    lineTaxes13.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "1374.06", "219.85", "1374.06",
        "219.85", "2748.12", "439.70", "2748.12", "439.70" });
    lineTaxes13.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "898.08", "475.98", "898.08",
        "475.98", "1796.16", "951.96", "1796.16", "951.96" });
    line13.setLinetaxes(lineTaxes13);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts13 = new String[] { "898.08", "898.08", "898.08", "898.08", "1796.16",
        "1796.16", "1796.16", "1796.16" };
    line13.setLineAmounts(lineAmounts13);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3, line4, line5, line6, line7, line8,
        line9, line10, line11, line12, line13 });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_VAT_21, new String[] { "349.50", "73.40", "349.50", "73.40",
        "699.00", "146.79", "699.00", "146.79" });
    taxes.put(TaxDataConstants.TAX_VAT_10, new String[] { "339.90", "33.99", "339.90", "33.99",
        "679.80", "67.98", "679.80", "67.98" });
    taxes.put(TaxDataConstants.TAX_VAT_4,
        new String[] { "45.00", "1.80", "45.00", "1.80", "90.00", "3.60", "90.00", "3.60" });
    taxes.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "20.88", "5.22", "20.88", "5.22", "41.76", "10.44", "41.76", "10.44" });
    taxes.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "13.92", "6.96", "13.92", "6.96", "27.84", "13.92", "27.84", "13.92" });
    taxes.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "847.04", "135.53", "847.04",
        "135.53", "1694.09", "271.05", "1694.09", "271.05" });
    taxes.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "669.60", "177.44", "669.60",
        "177.44", "1339.20", "354.89", "1339.20", "354.89" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "3898.29", "623.73", "3898.29",
        "623.73", "7796.57", "1247.45", "7796.57", "1247.45" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "2547.90", "1350.39", "2547.90",
        "1350.39", "5095.80", "2700.77", "5095.80", "2700.77" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "6374.28", "3965.82", "6374.28", "3965.82", "12748.53",
        "7931.64", "12748.53", "7931.64" };
    setDocAmounts(amounts);
  }
}
