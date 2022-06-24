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

public class TaxesTestData134 extends TaxesTestData {

  @Override
  public void initialize() {

    // Header info
    setTaxDocumentLevel(true);
    setPriceIncludingTaxes(true);

    // Line info
    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line1.setQuantity(new BigDecimal("-1200"));
    line1.setPrice(new BigDecimal("55.8"));
    line1.setQuantityUpdated(new BigDecimal("-2400"));
    line1.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes1 = new HashMap<String, String[]>();
    lineTaxes1.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-57724.14", "-9235.86",
        "-57724.14", "-9235.86", "-115448.28", "-18471.72", "-115448.28", "-18471.72" });
    lineTaxes1.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-45631.73", "-12092.41",
        "-45631.73", "-12092.41", "-91263.46", "-24184.82", "-91263.46", "-24184.82" });
    line1.setLinetaxes(lineTaxes1);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "-66960.00", "-45631.73", "-66960.00", "-45631.74",
        "-133920.00", "-91263.46", "-133920.00", "-91263.46" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line2.setQuantity(new BigDecimal("-600"));
    line2.setPrice(new BigDecimal("55.8"));
    line2.setQuantityUpdated(new BigDecimal("-1200"));
    line2.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes2 = new HashMap<String, String[]>();
    lineTaxes2.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-28862.07", "-4617.93",
        "-28862.07", "-4617.93", "-57724.14", "-9235.86", "-57724.14", "-9235.86" });
    lineTaxes2.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-22815.86", "-6046.21",
        "-22815.86", "-6046.21", "-45631.73", "-12092.41", "-45631.73", "-12092.41" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "-33480.00", "-22815.86", "-33480.00", "-22815.86",
        "-66960.00", "-45631.73", "-66960.00", "-45631.73" };
    line2.setLineAmounts(lineAmounts2);

    // Line info
    TaxesLineTestData line3 = new TaxesLineTestData();
    line3.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line3.setTaxid(TaxDataConstants.TAX_IEPS_26);
    line3.setQuantity(new BigDecimal("-840"));
    line3.setPrice(new BigDecimal("55.8"));
    line3.setQuantityUpdated(new BigDecimal("-1680"));
    line3.setPriceUpdated(new BigDecimal("55.8"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes3 = new HashMap<String, String[]>();
    lineTaxes3.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-40406.90", "-6465.10",
        "-40406.90", "-6465.10", "-80813.79", "-12930.21", "-80813.79", "-12930.21" });
    lineTaxes3.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-31942.21", "-8464.69",
        "-31942.21", "-8464.69", "-63884.42", "-16929.37", "-63884.42", "-16929.37" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "-46872.00", "-31942.21", "-46872.00", "-31942.21",
        "-93744.00", "-63884.42", "-93744.00", "-63884.42" };
    line3.setLineAmounts(lineAmounts3);

    // Line info
    TaxesLineTestData line4 = new TaxesLineTestData();
    line4.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line4.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line4.setQuantity(new BigDecimal("-37"));
    line4.setPrice(new BigDecimal("294.99"));
    line4.setQuantityUpdated(new BigDecimal("-74"));
    line4.setPriceUpdated(new BigDecimal("294.99"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes4 = new HashMap<String, String[]>();
    lineTaxes4.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-9409.16", "-1505.47",
        "-9409.16", "-1505.47", "-18818.33", "-3010.93", "-18818.33", "-3010.93" });
    lineTaxes4.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-6149.78", "-3259.38",
        "-6149.78", "-3259.38", "-12299.56", "-6518.77", "-12299.56", "-6518.77" });
    line4.setLinetaxes(lineTaxes4);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts4 = new String[] { "-10914.63", "-6149.78", "-10914.63", "-6149.78",
        "-21829.26", "-12299.56", "-21829.26", "-12299.56" };
    line4.setLineAmounts(lineAmounts4);

    // Line info
    TaxesLineTestData line5 = new TaxesLineTestData();
    line5.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line5.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line5.setQuantity(new BigDecimal("-12"));
    line5.setPrice(new BigDecimal("254.95"));
    line5.setQuantityUpdated(new BigDecimal("-24"));
    line5.setPriceUpdated(new BigDecimal("254.95"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes5 = new HashMap<String, String[]>();
    lineTaxes5.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-2637.41", "-421.99",
        "-2637.41", "-421.99", "-5274.83", "-843.97", "-5274.83", "-843.97" });
    lineTaxes5.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-1723.80", "-913.61",
        "-1723.80", "-913.61", "-3447.60", "-1827.23", "-3447.60", "-1827.23" });
    line5.setLinetaxes(lineTaxes5);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts5 = new String[] { "-3059.40", "-1723.80", "-3059.40", "-1723.80",
        "-6118.80", "-3447.60", "-6118.80", "-3447.60" };
    line5.setLineAmounts(lineAmounts5);

    // Line info
    TaxesLineTestData line6 = new TaxesLineTestData();
    line6.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line6.setTaxid(TaxDataConstants.TAX_IEPS_53);
    line6.setQuantity(new BigDecimal("-36"));
    line6.setPrice(new BigDecimal("299.36"));
    line6.setQuantityUpdated(new BigDecimal("-72"));
    line6.setPriceUpdated(new BigDecimal("299.36"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes6 = new HashMap<String, String[]>();
    lineTaxes6.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-9290.48", "-1486.48",
        "-9290.48", "-1486.48", "-18580.96", "-2972.96", "-18580.96", "-2972.96" });
    lineTaxes6.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-6072.21", "-3218.27",
        "-6072.21", "-3218.27", "-12144.42", "-6436.54", "-12144.42", "-6436.54" });
    line6.setLinetaxes(lineTaxes6);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts6 = new String[] { "-10776.96", "-6072.21", "-10776.96", "-6072.21",
        "-21553.92", "-12144.42", "-21553.92", "-12144.42" };
    line6.setLineAmounts(lineAmounts6);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3, line4, line5, line6 });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-126993.11", "-20318.90",
        "-126993.11", "-20318.89", "-253986.21", "-40637.79", "-253986.21", "-40637.79" });
    taxes.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-100389.81", "-26603.30",
        "-100389.81", "-26603.30", "-200779.61", "-53206.60", "-200779.61", "-53206.60" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-21337.06", "-3413.93", "-21337.06",
        "-3413.93", "-42674.12", "-6827.86", "-42674.12", "-6827.86" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-13945.79", "-7391.27", "-13945.79",
        "-7391.27", "-27891.58", "-14782.54", "-27891.58", "-14782.54" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "-172062.99", "-114335.60", "-172062.99", "-114335.60",
        "-344125.98", "-228671.19", "-344125.98", "-228671.19" };
    setDocAmounts(amounts);
  }
}
