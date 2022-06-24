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

public class TaxesTestData159 extends TaxesTestData {

  @Override
  public void initialize() {

    // Header info
    setTaxDocumentLevel(false);
    setPriceIncludingTaxes(true);

    // Line info
    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setTaxid(TaxDataConstants.TAX_VAT_4);
    line1.setQuantity(new BigDecimal("40"));
    line1.setPrice(new BigDecimal("15"));
    line1.setQuantityUpdated(new BigDecimal("80"));
    line1.setPriceUpdated(new BigDecimal("15"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes1 = new HashMap<String, String[]>();
    lineTaxes1.put(TaxDataConstants.TAX_VAT_4, new String[] { "576.92", "23.08", "576.92", "23.08",
        "1153.85", "46.15", "1153.85", "46.15" });
    line1.setLinetaxes(lineTaxes1);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "600.00", "576.92", "600.00", "576.92", "1200.00",
        "1153.85", "1200.00", "1153.85" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_VAT_21);
    line2.setQuantity(new BigDecimal("30"));
    line2.setPrice(new BigDecimal("9.9"));
    line2.setQuantityUpdated(new BigDecimal("60"));
    line2.setPriceUpdated(new BigDecimal("9.9"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes2 = new HashMap<String, String[]>();
    lineTaxes2.put(TaxDataConstants.TAX_VAT_21, new String[] { "245.45", "51.55", "245.45", "51.55",
        "490.91", "103.09", "490.91", "103.09" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "297.00", "245.45", "297.00", "245.45", "594.00",
        "490.91", "594.00", "490.91" };
    line2.setLineAmounts(lineAmounts2);

    // Line info
    TaxesLineTestData line3 = new TaxesLineTestData();
    line3.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line3.setTaxid(TaxDataConstants.TAX_VAT_10);
    line3.setQuantity(new BigDecimal("80"));
    line3.setPrice(new BigDecimal("1.2"));
    line3.setQuantityUpdated(new BigDecimal("160"));
    line3.setPriceUpdated(new BigDecimal("1.2"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes3 = new HashMap<String, String[]>();
    lineTaxes3.put(TaxDataConstants.TAX_VAT_10,
        new String[] { "87.27", "8.73", "87.27", "8.73", "174.55", "17.45", "174.55", "17.45" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "96.00", "87.27", "96.00", "87.27", "192.00", "174.55",
        "192.00", "174.55" };
    line3.setLineAmounts(lineAmounts3);

    // Line info
    TaxesLineTestData line4 = new TaxesLineTestData();
    line4.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line4.setTaxid(TaxDataConstants.TAX_VAT_21);
    line4.setQuantity(new BigDecimal("40"));
    line4.setPrice(new BigDecimal("109.9"));
    line4.setQuantityUpdated(new BigDecimal("80"));
    line4.setPriceUpdated(new BigDecimal("109.9"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes4 = new HashMap<String, String[]>();
    lineTaxes4.put(TaxDataConstants.TAX_VAT_21, new String[] { "3633.06", "762.94", "3633.06",
        "762.94", "7266.12", "1525.88", "7266.12", "1525.88" });
    line4.setLinetaxes(lineTaxes4);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts4 = new String[] { "4396.00", "3633.06", "4396.00", "3633.06", "8792.00",
        "7266.12", "8792.00", "7266.12" };
    line4.setLineAmounts(lineAmounts4);

    // Line info
    TaxesLineTestData line5 = new TaxesLineTestData();
    line5.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line5.setTaxid(TaxDataConstants.TAX_VAT_10);
    line5.setQuantity(new BigDecimal("40"));
    line5.setPrice(new BigDecimal("110.5"));
    line5.setQuantityUpdated(new BigDecimal("80"));
    line5.setPriceUpdated(new BigDecimal("110.5"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes5 = new HashMap<String, String[]>();
    lineTaxes5.put(TaxDataConstants.TAX_VAT_10, new String[] { "4018.18", "401.82", "4018.18",
        "401.82", "8036.36", "803.64", "8036.36", "803.64" });
    line5.setLinetaxes(lineTaxes5);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts5 = new String[] { "4420.00", "4018.18", "4420.00", "4018.18", "8840.00",
        "8036.36", "8840.00", "8036.36" };
    line5.setLineAmounts(lineAmounts5);

    // Line info
    TaxesLineTestData line6 = new TaxesLineTestData();
    line6.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line6.setTaxid(TaxDataConstants.TAX_SUPER);
    line6.setQuantity(new BigDecimal("50"));
    line6.setPrice(new BigDecimal("1.99"));
    line6.setQuantityUpdated(new BigDecimal("100"));
    line6.setPriceUpdated(new BigDecimal("1.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes6 = new HashMap<String, String[]>();
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "79.60", "19.90", "79.60", "19.90", "159.20", "39.80", "159.20", "39.80" });
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "53.07", "26.53", "53.07", "26.53", "106.13", "53.07", "106.13", "53.07" });
    line6.setLinetaxes(lineTaxes6);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts6 = new String[] { "99.50", "53.07", "99.50", "53.07", "199.00", "106.13",
        "199.00", "106.13" };
    line6.setLineAmounts(lineAmounts6);

    // Line info
    TaxesLineTestData line7 = new TaxesLineTestData();
    line7.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line7.setTaxid(TaxDataConstants.TAX_SUPER);
    line7.setQuantity(new BigDecimal("50"));
    line7.setPrice(new BigDecimal("1.49"));
    line7.setQuantityUpdated(new BigDecimal("100"));
    line7.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes7 = new HashMap<String, String[]>();
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "59.60", "14.90", "59.60", "14.90", "119.20", "29.80", "119.20", "29.80" });
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "39.73", "19.87", "39.73", "19.87", "79.47", "39.73", "79.47", "39.73" });
    line7.setLinetaxes(lineTaxes7);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts7 = new String[] { "74.50", "39.73", "74.50", "39.73", "149.00", "79.47",
        "149.00", "79.47" };
    line7.setLineAmounts(lineAmounts7);

    // Line info
    TaxesLineTestData line8 = new TaxesLineTestData();
    line8.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line8.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line8.setQuantity(new BigDecimal("40"));
    line8.setPrice(new BigDecimal("55.8"));
    line8.setQuantityUpdated(new BigDecimal("80"));
    line8.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes8 = new HashMap<String, String[]>();
    lineTaxes8.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "1924.14", "307.86", "1924.14",
        "307.86", "3848.28", "615.72", "3848.28", "615.72" });
    lineTaxes8.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "1521.06", "403.08", "1521.06",
        "403.08", "3042.12", "806.16", "3042.12", "806.16" });
    line8.setLinetaxes(lineTaxes8);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts8 = new String[] { "2232.00", "1521.06", "2232.00", "1521.06", "4464.00",
        "3042.12", "4464.00", "3042.12" };
    line8.setLineAmounts(lineAmounts8);

    // Line info
    TaxesLineTestData line9 = new TaxesLineTestData();
    line9.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line9.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line9.setQuantity(new BigDecimal("50"));
    line9.setPrice(new BigDecimal("55.8"));
    line9.setQuantityUpdated(new BigDecimal("100"));
    line9.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes9 = new HashMap<String, String[]>();
    lineTaxes9.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "2405.17", "384.83", "2405.17",
        "384.83", "4810.34", "769.66", "4810.34", "769.66" });
    lineTaxes9.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "1901.32", "503.85", "1901.32",
        "503.85", "3802.64", "1007.70", "3802.64", "1007.70" });
    line9.setLinetaxes(lineTaxes9);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts9 = new String[] { "2790.00", "1901.32", "2790.00", "1901.32", "5580.00",
        "3802.64", "5580.00", "3802.64" };
    line9.setLineAmounts(lineAmounts9);

    // Line info
    TaxesLineTestData line10 = new TaxesLineTestData();
    line10.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line10.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line10.setQuantity(new BigDecimal("60"));
    line10.setPrice(new BigDecimal("55.8"));
    line10.setQuantityUpdated(new BigDecimal("120"));
    line10.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes10 = new HashMap<String, String[]>();
    lineTaxes10.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "2886.21", "461.79", "2886.21",
        "461.79", "5772.41", "923.59", "5772.41", "923.59" });
    lineTaxes10.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "2281.59", "604.62", "2281.59",
        "604.62", "4563.17", "1209.24", "4563.17", "1209.24" });
    line10.setLinetaxes(lineTaxes10);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts10 = new String[] { "3348.00", "2281.59", "3348.00", "2281.59", "6696.00",
        "4563.17", "6696.00", "4563.17" };
    line10.setLineAmounts(lineAmounts10);

    // Line info
    TaxesLineTestData line11 = new TaxesLineTestData();
    line11.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line11.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line11.setQuantity(new BigDecimal("40"));
    line11.setPrice(new BigDecimal("294.99"));
    line11.setQuantityUpdated(new BigDecimal("80"));
    line11.setPriceUpdated(new BigDecimal("294.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes11 = new HashMap<String, String[]>();
    lineTaxes11.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "10172.07", "1627.53",
        "10172.07", "1627.53", "20344.14", "3255.06", "20344.14", "3255.06" });
    lineTaxes11.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "6648.41", "3523.66", "6648.41",
        "3523.66", "13296.82", "7047.32", "13296.82", "7047.32" });
    line11.setLinetaxes(lineTaxes11);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts11 = new String[] { "11799.60", "6648.41", "11799.60", "6648.41",
        "23599.20", "13296.82", "23599.20", "13296.82" };
    line11.setLineAmounts(lineAmounts11);

    // Line info
    TaxesLineTestData line12 = new TaxesLineTestData();
    line12.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line12.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line12.setQuantity(new BigDecimal("40"));
    line12.setPrice(new BigDecimal("254.95"));
    line12.setQuantityUpdated(new BigDecimal("80"));
    line12.setPriceUpdated(new BigDecimal("254.95"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes12 = new HashMap<String, String[]>();
    lineTaxes12.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "8791.38", "1406.62", "8791.38",
        "1406.62", "17582.76", "2813.24", "17582.76", "2813.24" });
    lineTaxes12.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "5746.00", "3045.38", "5746.00",
        "3045.38", "11492.00", "6090.76", "11492.00", "6090.76" });
    line12.setLinetaxes(lineTaxes12);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts12 = new String[] { "10198.00", "5746.00", "10198.00", "5746.00",
        "20396.00", "11492.00", "20396.00", "11492.00" };
    line12.setLineAmounts(lineAmounts12);

    // Line info
    TaxesLineTestData line13 = new TaxesLineTestData();
    line13.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line13.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line13.setQuantity(new BigDecimal("40"));
    line13.setPrice(new BigDecimal("299.36"));
    line13.setQuantityUpdated(new BigDecimal("80"));
    line13.setPriceUpdated(new BigDecimal("299.36"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes13 = new HashMap<String, String[]>();
    lineTaxes13.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "10322.76", "1651.64",
        "10322.76", "1651.64", "20645.52", "3303.28", "20645.52", "3303.28" });
    lineTaxes13.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "6746.90", "3575.86", "6746.90",
        "3575.86", "13493.80", "7151.72", "13493.80", "7151.72" });
    line13.setLinetaxes(lineTaxes13);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts13 = new String[] { "11974.40", "6746.90", "11974.40", "6746.90",
        "23948.80", "13493.80", "23948.80", "13493.80" };
    line13.setLineAmounts(lineAmounts13);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3, line4, line5, line6, line7, line8,
        line9, line10, line11, line12, line13 });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_VAT_4, new String[] { "576.92", "23.08", "576.92", "23.08",
        "1153.85", "46.15", "1153.85", "46.15" });
    taxes.put(TaxDataConstants.TAX_VAT_21, new String[] { "3878.51", "814.49", "3878.51", "814.49",
        "7757.03", "1628.97", "7757.03", "1628.97" });
    taxes.put(TaxDataConstants.TAX_VAT_10, new String[] { "4105.45", "410.55", "4105.45", "410.55",
        "8210.91", "821.09", "8210.91", "821.09" });
    taxes.put(TaxDataConstants.TAX_SUPER_25, new String[] { "139.20", "34.80", "139.20", "34.80",
        "278.40", "69.60", "278.40", "69.60" });
    taxes.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "92.80", "46.40", "92.80", "46.40", "185.60", "92.80", "185.60", "92.80" });
    taxes.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "7215.52", "1154.48", "7215.52",
        "1154.48", "14431.03", "2308.97", "14431.03", "2308.97" });
    taxes.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "5703.97", "1511.55", "5703.97",
        "1511.55", "11407.93", "3023.10", "11407.93", "3023.10" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "29286.21", "4685.79", "29286.21",
        "4685.79", "58572.42", "9371.58", "58572.42", "9371.58" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "19141.31", "10144.90", "19141.31",
        "10144.90", "38282.62", "20289.80", "38282.62", "20289.80" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "52325.00", "33498.96", "52325.00", "33498.96", "104650.00",
        "66997.94", "104650.00", "66997.94" };
    setDocAmounts(amounts);
  }
}
