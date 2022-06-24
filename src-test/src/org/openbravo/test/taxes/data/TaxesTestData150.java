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

public class TaxesTestData150 extends TaxesTestData {

  @Override
  public void initialize() {

    // Header info
    setTaxDocumentLevel(true);
    setPriceIncludingTaxes(true);

    // Line info
    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setTaxid(TaxDataConstants.TAX_VAT_4);
    line1.setQuantity(new BigDecimal("-4"));
    line1.setPrice(new BigDecimal("15"));
    line1.setQuantityUpdated(new BigDecimal("-8"));
    line1.setPriceUpdated(new BigDecimal("15"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes1 = new HashMap<String, String[]>();
    lineTaxes1.put(TaxDataConstants.TAX_VAT_4, new String[] { "-57.69", "-2.31", "-57.69", "-2.31",
        "-115.38", "-4.62", "-115.38", "-4.62" });
    line1.setLinetaxes(lineTaxes1);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "-60.00", "-57.69", "-60.00", "-57.69", "-120.00",
        "-115.38", "-120.00", "-115.38" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_VAT_21);
    line2.setQuantity(new BigDecimal("-3"));
    line2.setPrice(new BigDecimal("9.9"));
    line2.setQuantityUpdated(new BigDecimal("-6"));
    line2.setPriceUpdated(new BigDecimal("9.9"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes2 = new HashMap<String, String[]>();
    lineTaxes2.put(TaxDataConstants.TAX_VAT_21, new String[] { "-24.55", "-5.15", "-24.55", "-5.15",
        "-49.09", "-10.31", "-49.09", "-10.31" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "-29.70", "-24.55", "-29.70", "-24.55", "-59.40",
        "-49.09", "-59.40", "-49.09" };
    line2.setLineAmounts(lineAmounts2);

    // Line info
    TaxesLineTestData line3 = new TaxesLineTestData();
    line3.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line3.setTaxid(TaxDataConstants.TAX_VAT_10);
    line3.setQuantity(new BigDecimal("-8"));
    line3.setPrice(new BigDecimal("1.2"));
    line3.setQuantityUpdated(new BigDecimal("-16"));
    line3.setPriceUpdated(new BigDecimal("1.2"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes3 = new HashMap<String, String[]>();
    lineTaxes3.put(TaxDataConstants.TAX_VAT_10,
        new String[] { "-8.73", "-0.87", "-8.73", "-0.87", "-17.45", "-1.75", "-17.45", "-1.75" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "-9.60", "-8.73", "-9.60", "-8.73", "-19.20", "-17.45",
        "-19.20", "-17.45" };
    line3.setLineAmounts(lineAmounts3);

    // Line info
    TaxesLineTestData line4 = new TaxesLineTestData();
    line4.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line4.setTaxid(TaxDataConstants.TAX_VAT_21);
    line4.setQuantity(new BigDecimal("-4"));
    line4.setPrice(new BigDecimal("109.9"));
    line4.setQuantityUpdated(new BigDecimal("-8"));
    line4.setPriceUpdated(new BigDecimal("109.9"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes4 = new HashMap<String, String[]>();
    lineTaxes4.put(TaxDataConstants.TAX_VAT_21, new String[] { "-363.31", "-76.29", "-363.31",
        "-76.29", "-726.61", "-152.59", "-726.61", "-152.59" });
    line4.setLinetaxes(lineTaxes4);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts4 = new String[] { "-439.60", "-363.31", "-439.60", "-363.30", "-879.20",
        "-726.61", "-879.20", "-726.61" };
    line4.setLineAmounts(lineAmounts4);

    // Line info
    TaxesLineTestData line5 = new TaxesLineTestData();
    line5.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line5.setTaxid(TaxDataConstants.TAX_VAT_10);
    line5.setQuantity(new BigDecimal("-4"));
    line5.setPrice(new BigDecimal("110.5"));
    line5.setQuantityUpdated(new BigDecimal("-8"));
    line5.setPriceUpdated(new BigDecimal("110.5"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes5 = new HashMap<String, String[]>();
    lineTaxes5.put(TaxDataConstants.TAX_VAT_10, new String[] { "-401.82", "-40.18", "-401.82",
        "-40.18", "-803.64", "-80.36", "-803.64", "-80.36" });
    line5.setLinetaxes(lineTaxes5);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts5 = new String[] { "-442.00", "-401.82", "-442.00", "-401.82", "-884.00",
        "-803.64", "-884.00", "-803.64" };
    line5.setLineAmounts(lineAmounts5);

    // Line info
    TaxesLineTestData line6 = new TaxesLineTestData();
    line6.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line6.setTaxid(TaxDataConstants.TAX_SUPER);
    line6.setQuantity(new BigDecimal("-5"));
    line6.setPrice(new BigDecimal("1.99"));
    line6.setQuantityUpdated(new BigDecimal("-10"));
    line6.setPriceUpdated(new BigDecimal("1.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes6 = new HashMap<String, String[]>();
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "-7.96", "-1.99", "-7.96", "-1.99", "-15.92", "-3.98", "-15.92", "-3.98" });
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-5.31", "-2.65", "-5.31", "-2.65", "-10.61", "-5.31", "-10.61", "-5.31" });
    line6.setLinetaxes(lineTaxes6);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts6 = new String[] { "-9.95", "-5.31", "-9.95", "-5.31", "-19.90", "-10.61",
        "-19.90", "-10.61" };
    line6.setLineAmounts(lineAmounts6);

    // Line info
    TaxesLineTestData line7 = new TaxesLineTestData();
    line7.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line7.setTaxid(TaxDataConstants.TAX_SUPER);
    line7.setQuantity(new BigDecimal("-5"));
    line7.setPrice(new BigDecimal("1.49"));
    line7.setQuantityUpdated(new BigDecimal("-10"));
    line7.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes7 = new HashMap<String, String[]>();
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "-5.96", "-1.49", "-5.96", "-1.49", "-11.92", "-2.98", "-11.92", "-2.98" });
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-3.97", "-1.99", "-3.97", "-1.99", "-7.95", "-3.97", "-7.95", "-3.97" });
    line7.setLinetaxes(lineTaxes7);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts7 = new String[] { "-7.45", "-3.97", "-7.45", "-3.97", "-14.90", "-7.95",
        "-14.90", "-7.95" };
    line7.setLineAmounts(lineAmounts7);

    // Line info
    TaxesLineTestData line8 = new TaxesLineTestData();
    line8.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line8.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line8.setQuantity(new BigDecimal("-4"));
    line8.setPrice(new BigDecimal("55.8"));
    line8.setQuantityUpdated(new BigDecimal("-8"));
    line8.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes8 = new HashMap<String, String[]>();
    lineTaxes8.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-192.42", "-30.78", "-192.42",
        "-30.78", "-384.83", "-61.57", "-384.83", "-61.57" });
    lineTaxes8.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-152.11", "-40.31", "-152.11",
        "-40.31", "-304.21", "-80.62", "-304.21", "-80.62" });
    line8.setLinetaxes(lineTaxes8);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts8 = new String[] { "-223.20", "-152.11", "-223.20", "-152.11", "-446.40",
        "-304.21", "-446.40", "-304.21" };
    line8.setLineAmounts(lineAmounts8);

    // Line info
    TaxesLineTestData line9 = new TaxesLineTestData();
    line9.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line9.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line9.setQuantity(new BigDecimal("-5"));
    line9.setPrice(new BigDecimal("55.8"));
    line9.setQuantityUpdated(new BigDecimal("-10"));
    line9.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes9 = new HashMap<String, String[]>();
    lineTaxes9.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-240.52", "-38.48", "-240.52",
        "-38.48", "-481.03", "-76.97", "-481.03", "-76.97" });
    lineTaxes9.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-190.13", "-50.39", "-190.13",
        "-50.39", "-380.26", "-100.77", "-380.26", "-100.77" });
    line9.setLinetaxes(lineTaxes9);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts9 = new String[] { "-279.00", "-190.13", "-279.00", "-190.13", "-558.00",
        "-380.26", "-558.00", "-380.26" };
    line9.setLineAmounts(lineAmounts9);

    // Line info
    TaxesLineTestData line10 = new TaxesLineTestData();
    line10.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line10.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line10.setQuantity(new BigDecimal("-6"));
    line10.setPrice(new BigDecimal("55.8"));
    line10.setQuantityUpdated(new BigDecimal("-12"));
    line10.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes10 = new HashMap<String, String[]>();
    lineTaxes10.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-288.62", "-46.18", "-288.62",
        "-46.18", "-577.24", "-92.36", "-577.24", "-92.36" });
    lineTaxes10.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-228.16", "-60.46", "-228.16",
        "-60.46", "-456.32", "-120.92", "-456.32", "-120.92" });
    line10.setLinetaxes(lineTaxes10);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts10 = new String[] { "-334.80", "-228.16", "-334.80", "-228.16", "-669.60",
        "-456.32", "-669.60", "-456.32" };
    line10.setLineAmounts(lineAmounts10);

    // Line info
    TaxesLineTestData line11 = new TaxesLineTestData();
    line11.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line11.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line11.setQuantity(new BigDecimal("-4"));
    line11.setPrice(new BigDecimal("294.99"));
    line11.setQuantityUpdated(new BigDecimal("-8"));
    line11.setPriceUpdated(new BigDecimal("294.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes11 = new HashMap<String, String[]>();
    lineTaxes11.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-1017.21", "-162.75",
        "-1017.21", "-162.75", "-2034.41", "-325.51", "-2034.41", "-325.51" });
    lineTaxes11.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-664.84", "-352.37", "-664.84",
        "-352.37", "-1329.68", "-704.73", "-1329.68", "-704.73" });
    line11.setLinetaxes(lineTaxes11);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts11 = new String[] { "-1179.96", "-664.84", "-1179.96", "-664.84",
        "-2359.92", "-1329.68", "-2359.92", "-1329.68" };
    line11.setLineAmounts(lineAmounts11);

    // Line info
    TaxesLineTestData line12 = new TaxesLineTestData();
    line12.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line12.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line12.setQuantity(new BigDecimal("-4"));
    line12.setPrice(new BigDecimal("254.95"));
    line12.setQuantityUpdated(new BigDecimal("-8"));
    line12.setPriceUpdated(new BigDecimal("254.95"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes12 = new HashMap<String, String[]>();
    lineTaxes12.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-879.14", "-140.66", "-879.14",
        "-140.66", "-1758.28", "-281.32", "-1758.28", "-281.32" });
    lineTaxes12.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-574.60", "-304.54", "-574.60",
        "-304.54", "-1149.20", "-609.08", "-1149.20", "-609.08" });
    line12.setLinetaxes(lineTaxes12);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts12 = new String[] { "-1019.80", "-574.60", "-1019.80", "-574.60",
        "-2039.60", "-1149.20", "-2039.60", "-1149.20" };
    line12.setLineAmounts(lineAmounts12);

    // Line info
    TaxesLineTestData line13 = new TaxesLineTestData();
    line13.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line13.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line13.setQuantity(new BigDecimal("-4"));
    line13.setPrice(new BigDecimal("299.36"));
    line13.setQuantityUpdated(new BigDecimal("-8"));
    line13.setPriceUpdated(new BigDecimal("299.36"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes13 = new HashMap<String, String[]>();
    lineTaxes13.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-1032.28", "-165.16",
        "-1032.28", "-165.16", "-2064.55", "-330.33", "-2064.55", "-330.33" });
    lineTaxes13.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-674.69", "-357.59", "-674.69",
        "-357.59", "-1349.38", "-715.17", "-1349.38", "-715.17" });
    line13.setLinetaxes(lineTaxes13);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts13 = new String[] { "-1197.44", "-674.69", "-1197.44", "-674.69",
        "-2394.88", "-1349.38", "-2394.88", "-1349.38" };
    line13.setLineAmounts(lineAmounts13);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3, line4, line5, line6, line7, line8,
        line9, line10, line11, line12, line13 });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_VAT_4, new String[] { "-57.69", "-2.31", "-57.69", "-2.31",
        "-115.38", "-4.62", "-115.38", "-4.62" });
    taxes.put(TaxDataConstants.TAX_VAT_21, new String[] { "-387.85", "-81.45", "-387.85", "-81.45",
        "-775.70", "-162.90", "-775.70", "-162.90" });
    taxes.put(TaxDataConstants.TAX_VAT_10, new String[] { "-410.55", "-41.06", "-410.55", "-41.05",
        "-821.09", "-82.11", "-821.09", "-82.11" });
    taxes.put(TaxDataConstants.TAX_SUPER_25, new String[] { "-13.92", "-3.48", "-13.92", "-3.48",
        "-27.84", "-6.96", "-27.84", "-6.96" });
    taxes.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-9.28", "-4.64", "-9.28", "-4.64", "-18.56", "-9.28", "-18.56", "-9.28" });
    taxes.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-721.56", "-115.45", "-721.55",
        "-115.45", "-1443.10", "-230.90", "-1443.10", "-230.90" });
    taxes.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-570.40", "-151.16", "-570.40",
        "-151.15", "-1140.79", "-302.31", "-1140.79", "-302.31" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-2928.62", "-468.58", "-2928.62",
        "-468.58", "-5857.24", "-937.16", "-5857.24", "-937.16" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-1914.13", "-1014.49", "-1914.13",
        "-1014.49", "-3828.26", "-2028.98", "-3828.26", "-2028.98" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "-5232.50", "-3349.90", "-5232.50", "-3349.90", "-10465.00",
        "-6699.78", "-10465.00", "-6699.78" };
    setDocAmounts(amounts);

  }
}
