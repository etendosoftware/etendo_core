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

public class TaxesTestData142 extends TaxesTestData {

  @Override
  public void initialize() {

    // Header info
    setTaxDocumentLevel(true);
    setPriceIncludingTaxes(true);

    // Line info
    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setTaxid(TaxDataConstants.TAX_VAT_4);
    line1.setQuantity(new BigDecimal("-3"));
    line1.setPrice(new BigDecimal("15"));
    line1.setQuantityUpdated(new BigDecimal("-6"));
    line1.setPriceUpdated(new BigDecimal("15"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes1 = new HashMap<String, String[]>();
    lineTaxes1.put(TaxDataConstants.TAX_VAT_4, new String[] { "-43.27", "-1.73", "-43.27", "-1.73",
        "-86.54", "-3.46", "-86.54", "-3.46" });
    line1.setLinetaxes(lineTaxes1);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "-45.00", "-43.27", "-45.00", "-43.27", "-90.00",
        "-86.54", "-90.00", "-86.54" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_VAT_21);
    line2.setQuantity(new BigDecimal("-2"));
    line2.setPrice(new BigDecimal("9.9"));
    line2.setQuantityUpdated(new BigDecimal("-4"));
    line2.setPriceUpdated(new BigDecimal("9.9"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes2 = new HashMap<String, String[]>();
    lineTaxes2.put(TaxDataConstants.TAX_VAT_21, new String[] { "-16.36", "-3.44", "-16.36", "-3.44",
        "-32.73", "-6.87", "-32.73", "-6.87" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "-19.80", "-16.36", "-19.80", "-16.36", "-39.60",
        "-32.73", "-39.60", "-32.73" };
    line2.setLineAmounts(lineAmounts2);

    // Line info
    TaxesLineTestData line3 = new TaxesLineTestData();
    line3.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line3.setTaxid(TaxDataConstants.TAX_VAT_10);
    line3.setQuantity(new BigDecimal("-7"));
    line3.setPrice(new BigDecimal("1.2"));
    line3.setQuantityUpdated(new BigDecimal("-14"));
    line3.setPriceUpdated(new BigDecimal("1.2"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes3 = new HashMap<String, String[]>();
    lineTaxes3.put(TaxDataConstants.TAX_VAT_10,
        new String[] { "-7.64", "-0.76", "-7.64", "-0.76", "-15.27", "-1.53", "-15.27", "-1.53" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "-8.40", "-7.64", "-8.40", "-7.64", "-16.80", "-15.27",
        "-16.80", "-15.27" };
    line3.setLineAmounts(lineAmounts3);

    // Line info
    TaxesLineTestData line4 = new TaxesLineTestData();
    line4.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line4.setTaxid(TaxDataConstants.TAX_VAT_21);
    line4.setQuantity(new BigDecimal("-3"));
    line4.setPrice(new BigDecimal("109.9"));
    line4.setQuantityUpdated(new BigDecimal("-6"));
    line4.setPriceUpdated(new BigDecimal("109.9"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes4 = new HashMap<String, String[]>();
    lineTaxes4.put(TaxDataConstants.TAX_VAT_21, new String[] { "-272.48", "-57.22", "-272.48",
        "-57.22", "-544.96", "-114.44", "-544.96", "-114.44" });
    line4.setLinetaxes(lineTaxes4);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts4 = new String[] { "-329.70", "-272.48", "-329.70", "-272.48", "-659.40",
        "-544.96", "-659.40", "-544.96" };
    line4.setLineAmounts(lineAmounts4);

    // Line info
    TaxesLineTestData line5 = new TaxesLineTestData();
    line5.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line5.setTaxid(TaxDataConstants.TAX_VAT_10);
    line5.setQuantity(new BigDecimal("-3"));
    line5.setPrice(new BigDecimal("110.5"));
    line5.setQuantityUpdated(new BigDecimal("-6"));
    line5.setPriceUpdated(new BigDecimal("110.5"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes5 = new HashMap<String, String[]>();
    lineTaxes5.put(TaxDataConstants.TAX_VAT_10, new String[] { "-301.36", "-30.14", "-301.36",
        "-30.14", "-602.73", "-60.27", "-602.73", "-60.27" });
    line5.setLinetaxes(lineTaxes5);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts5 = new String[] { "-331.50", "-301.36", "-331.50", "-301.36", "-663.00",
        "-602.73", "-663.00", "-602.73" };
    line5.setLineAmounts(lineAmounts5);

    // Line info
    TaxesLineTestData line6 = new TaxesLineTestData();
    line6.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line6.setTaxid(TaxDataConstants.TAX_SUPER);
    line6.setQuantity(new BigDecimal("-4"));
    line6.setPrice(new BigDecimal("1.99"));
    line6.setQuantityUpdated(new BigDecimal("-8"));
    line6.setPriceUpdated(new BigDecimal("1.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes6 = new HashMap<String, String[]>();
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "-6.36", "-1.60", "-6.36", "-1.60", "-12.73", "-3.19", "-12.73", "-3.19" });
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-4.25", "-2.11", "-4.25", "-2.11", "-8.49", "-4.24", "-8.49", "-4.24" });
    line6.setLinetaxes(lineTaxes6);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts6 = new String[] { "-7.96", "-4.25", "-7.96", "-4.24", "-15.92", "-8.49",
        "-15.92", "-8.49" };
    line6.setLineAmounts(lineAmounts6);

    // Line info
    TaxesLineTestData line7 = new TaxesLineTestData();
    line7.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line7.setTaxid(TaxDataConstants.TAX_SUPER);
    line7.setQuantity(new BigDecimal("-4"));
    line7.setPrice(new BigDecimal("1.49"));
    line7.setQuantityUpdated(new BigDecimal("-8"));
    line7.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes7 = new HashMap<String, String[]>();
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "-4.77", "-1.19", "-4.77", "-1.19", "-9.54", "-2.38", "-9.54", "-2.38" });
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-3.18", "-1.59", "-3.18", "-1.59", "-6.36", "-3.18", "-6.36", "-3.18" });
    line7.setLinetaxes(lineTaxes7);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts7 = new String[] { "-5.96", "-3.18", "-5.96", "-3.18", "-11.92", "-6.36",
        "-11.92", "-6.36" };
    line7.setLineAmounts(lineAmounts7);

    // Line info
    TaxesLineTestData line8 = new TaxesLineTestData();
    line8.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line8.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line8.setQuantity(new BigDecimal("-3"));
    line8.setPrice(new BigDecimal("55.8"));
    line8.setQuantityUpdated(new BigDecimal("-6"));
    line8.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes8 = new HashMap<String, String[]>();
    lineTaxes8.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-144.31", "-23.09", "-144.31",
        "-23.09", "-288.62", "-46.18", "-288.62", "-46.18" });
    lineTaxes8.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-114.08", "-30.23", "-114.08",
        "-30.23", "-228.16", "-60.46", "-228.16", "-60.46" });
    line8.setLinetaxes(lineTaxes8);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts8 = new String[] { "-167.40", "-114.08", "-167.40", "-114.08", "-334.80",
        "-228.16", "-334.80", "-228.16" };
    line8.setLineAmounts(lineAmounts8);

    // Line info
    TaxesLineTestData line9 = new TaxesLineTestData();
    line9.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line9.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line9.setQuantity(new BigDecimal("-4"));
    line9.setPrice(new BigDecimal("55.8"));
    line9.setQuantityUpdated(new BigDecimal("-8"));
    line9.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes9 = new HashMap<String, String[]>();
    lineTaxes9.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-192.42", "-30.78", "-192.42",
        "-30.78", "-384.83", "-61.57", "-384.83", "-61.57" });
    lineTaxes9.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-152.11", "-40.31", "-152.11",
        "-40.31", "-304.21", "-80.62", "-304.21", "-80.62" });
    line9.setLinetaxes(lineTaxes9);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts9 = new String[] { "-223.20", "-152.11", "-223.20", "-152.11", "-446.40",
        "-304.21", "-446.40", "-304.21" };
    line9.setLineAmounts(lineAmounts9);

    // Line info
    TaxesLineTestData line10 = new TaxesLineTestData();
    line10.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line10.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line10.setQuantity(new BigDecimal("-5"));
    line10.setPrice(new BigDecimal("55.8"));
    line10.setQuantityUpdated(new BigDecimal("-10"));
    line10.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes10 = new HashMap<String, String[]>();
    lineTaxes10.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-240.52", "-38.48", "-240.52",
        "-38.48", "-481.03", "-76.97", "-481.03", "-76.97" });
    lineTaxes10.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-190.13", "-50.39", "-190.13",
        "-50.39", "-380.26", "-100.77", "-380.26", "-100.77" });
    line10.setLinetaxes(lineTaxes10);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts10 = new String[] { "-279.00", "-190.13", "-279.00", "-190.13", "-558.00",
        "-380.26", "-558.00", "-380.26" };
    line10.setLineAmounts(lineAmounts10);

    // Line info
    TaxesLineTestData line11 = new TaxesLineTestData();
    line11.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line11.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line11.setQuantity(new BigDecimal("-3"));
    line11.setPrice(new BigDecimal("294.99"));
    line11.setQuantityUpdated(new BigDecimal("-6"));
    line11.setPriceUpdated(new BigDecimal("294.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes11 = new HashMap<String, String[]>();
    lineTaxes11.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-762.90", "-122.07", "-762.90",
        "-122.07", "-1525.81", "-244.13", "-1525.81", "-244.13" });
    lineTaxes11.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-498.63", "-264.27", "-498.63",
        "-264.27", "-997.26", "-528.55", "-997.26", "-528.55" });
    line11.setLinetaxes(lineTaxes11);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts11 = new String[] { "-884.97", "-498.63", "-884.97", "-498.63", "-1769.94",
        "-997.26", "-1769.94", "-997.26" };
    line11.setLineAmounts(lineAmounts11);

    // Line info
    TaxesLineTestData line12 = new TaxesLineTestData();
    line12.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line12.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line12.setQuantity(new BigDecimal("-3"));
    line12.setPrice(new BigDecimal("254.95"));
    line12.setQuantityUpdated(new BigDecimal("-6"));
    line12.setPriceUpdated(new BigDecimal("254.95"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes12 = new HashMap<String, String[]>();
    lineTaxes12.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-659.35", "-105.50", "-659.35",
        "-105.50", "-1318.71", "-210.99", "-1318.71", "-210.99" });
    lineTaxes12.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-430.95", "-228.40", "-430.95",
        "-228.40", "-861.90", "-456.81", "-861.90", "-456.81" });
    line12.setLinetaxes(lineTaxes12);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts12 = new String[] { "-764.85", "-430.95", "-764.85", "-430.95", "-1529.70",
        "-861.90", "-1529.70", "-861.90" };
    line12.setLineAmounts(lineAmounts12);

    // Line info
    TaxesLineTestData line13 = new TaxesLineTestData();
    line13.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line13.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line13.setQuantity(new BigDecimal("-3"));
    line13.setPrice(new BigDecimal("299.36"));
    line13.setQuantityUpdated(new BigDecimal("-6"));
    line13.setPriceUpdated(new BigDecimal("299.36"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes13 = new HashMap<String, String[]>();
    lineTaxes13.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-774.21", "-123.87", "-774.21",
        "-123.87", "-1548.42", "-247.74", "-1548.42", "-247.74" });
    lineTaxes13.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-506.02", "-268.19", "-506.02",
        "-268.19", "-1012.04", "-536.38", "-1012.04", "-536.38" });
    line13.setLinetaxes(lineTaxes13);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts13 = new String[] { "-898.08", "-506.02", "-898.08", "-506.02", "-1796.16",
        "-1012.04", "-1796.16", "-1012.04" };
    line13.setLineAmounts(lineAmounts13);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3, line4, line5, line6, line7, line8,
        line9, line10, line11, line12, line13 });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_VAT_4, new String[] { "-43.27", "-1.73", "-43.27", "-1.73",
        "-86.54", "-3.46", "-86.54", "-3.46" });
    taxes.put(TaxDataConstants.TAX_VAT_21, new String[] { "-288.84", "-60.66", "-288.84", "-60.66",
        "-577.69", "-121.31", "-577.69", "-121.31" });
    taxes.put(TaxDataConstants.TAX_VAT_10, new String[] { "-309.00", "-30.90", "-309.00", "-30.90",
        "-618.00", "-61.80", "-618.00", "-61.80" });
    taxes.put(TaxDataConstants.TAX_SUPER_25, new String[] { "-11.13", "-2.78", "-11.13", "-2.79",
        "-22.28", "-5.57", "-22.27", "-5.57" });
    taxes.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-7.42", "-3.71", "-7.42", "-3.71", "-14.85", "-7.43", "-14.85", "-7.42" });
    taxes.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-577.24", "-92.36", "-577.24",
        "-92.36", "-1154.48", "-184.72", "-1154.48", "-184.72" });
    taxes.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-456.32", "-120.92", "-456.32",
        "-120.92", "-912.63", "-241.85", "-912.63", "-241.85" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-2196.47", "-351.44", "-2196.47",
        "-351.43", "-4392.94", "-702.87", "-4392.93", "-702.87" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-1435.60", "-760.87", "-1435.60",
        "-760.87", "-2871.20", "-1521.74", "-2871.20", "-1521.73" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "-3965.82", "-2540.45", "-3965.82", "-2540.45", "-7931.64",
        "-5080.91", "-7931.64", "-5080.91" };
    setDocAmounts(amounts);
  }
}
