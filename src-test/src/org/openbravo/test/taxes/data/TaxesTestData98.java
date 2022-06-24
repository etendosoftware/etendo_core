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

public class TaxesTestData98 extends TaxesTestData {

  @Override
  public void initialize() {
    // Header info
    setTaxDocumentLevel(true);
    setPriceIncludingTaxes(false);

    // Line info
    TaxesLineTestData line1 = new TaxesLineTestData();
    line1.setTaxid(TaxDataConstants.TAX_SUPER);
    line1.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line1.setQuantity(new BigDecimal("-1"));
    line1.setPrice(new BigDecimal("1.49"));
    line1.setQuantityUpdated(new BigDecimal("-2"));
    line1.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert,
    // taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> linesTaxes = new HashMap<String, String[]>();
    linesTaxes.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "-2.24", "-0.56", "-2.24", "-0.56", "-4.47", "-1.12", "-4.47", "-1.12" });
    linesTaxes.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-1.49", "-0.75", "-1.49", "-0.75", "-2.98", "-1.49", "-2.98", "-1.49" });
    line1.setLinetaxes(linesTaxes);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "-1.49", "-1.49", "-1.49", "-1.49", "-2.98", "-2.98",
        "-2.98", "-2.98" };
    line1.setLineAmounts(lineAmounts1);

    // Line info
    TaxesLineTestData line2 = new TaxesLineTestData();
    line2.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line2.setTaxid(TaxDataConstants.TAX_SUPER);
    line2.setQuantity(new BigDecimal("-1"));
    line2.setPrice(new BigDecimal("1.49"));
    line2.setQuantityUpdated(new BigDecimal("-2"));
    line2.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes2 = new HashMap<String, String[]>();
    lineTaxes2.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "-2.24", "-0.56", "-2.24", "-0.56", "-4.47", "-1.12", "-4.47", "-1.12" });
    lineTaxes2.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-1.49", "-0.75", "-1.49", "-0.75", "-2.98", "-1.49", "-2.98", "-1.49" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "-1.49", "-1.49", "-1.49", "-1.49", "-2.98", "-2.98",
        "-2.98", "-2.98" };
    line2.setLineAmounts(lineAmounts2);

    // Line info
    TaxesLineTestData line3 = new TaxesLineTestData();
    line3.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line3.setTaxid(TaxDataConstants.TAX_SUPER);
    line3.setQuantity(new BigDecimal("-1"));
    line3.setPrice(new BigDecimal("1.49"));
    line3.setQuantityUpdated(new BigDecimal("-2"));
    line3.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes3 = new HashMap<String, String[]>();
    lineTaxes3.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "-2.24", "-0.56", "-2.24", "-0.56", "-4.47", "-1.12", "-4.47", "-1.12" });
    lineTaxes3.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-1.49", "-0.75", "-1.49", "-0.75", "-2.98", "-1.49", "-2.98", "-1.49" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "-1.49", "-1.49", "-1.49", "-1.49", "-2.98", "-2.98",
        "-2.98", "-2.98" };
    line3.setLineAmounts(lineAmounts3);

    // Line info
    TaxesLineTestData line4 = new TaxesLineTestData();
    line4.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line4.setTaxid(TaxDataConstants.TAX_SUPER);
    line4.setQuantity(new BigDecimal("-1"));
    line4.setPrice(new BigDecimal("1.49"));
    line4.setQuantityUpdated(new BigDecimal("-2"));
    line4.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes4 = new HashMap<String, String[]>();
    lineTaxes4.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "-2.24", "-0.56", "-2.24", "-0.56", "-4.47", "-1.12", "-4.47", "-1.12" });
    lineTaxes4.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-1.49", "-0.75", "-1.49", "-0.75", "-2.98", "-1.49", "-2.98", "-1.49" });
    line4.setLinetaxes(lineTaxes4);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts4 = new String[] { "-1.49", "-1.49", "-1.49", "-1.49", "-2.98", "-2.98",
        "-2.98", "-2.98" };
    line4.setLineAmounts(lineAmounts4);

    // Line info
    TaxesLineTestData line5 = new TaxesLineTestData();
    line5.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line5.setTaxid(TaxDataConstants.TAX_SUPER);
    line5.setQuantity(new BigDecimal("-1"));
    line5.setPrice(new BigDecimal("1.49"));
    line5.setQuantityUpdated(new BigDecimal("-2"));
    line5.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes5 = new HashMap<String, String[]>();
    lineTaxes5.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "-2.24", "-0.56", "-2.24", "-0.56", "-4.47", "-1.12", "-4.47", "-1.12" });
    lineTaxes5.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-1.49", "-0.75", "-1.49", "-0.75", "-2.98", "-1.49", "-2.98", "-1.49" });
    line5.setLinetaxes(lineTaxes5);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts5 = new String[] { "-1.49", "-1.49", "-1.49", "-1.49", "-2.98", "-2.98",
        "-2.98", "-2.98" };
    line5.setLineAmounts(lineAmounts5);

    // Line info
    TaxesLineTestData line6 = new TaxesLineTestData();
    line6.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line6.setTaxid(TaxDataConstants.TAX_SUPER);
    line6.setQuantity(new BigDecimal("-1"));
    line6.setPrice(new BigDecimal("1.49"));
    line6.setQuantityUpdated(new BigDecimal("-2"));
    line6.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes6 = new HashMap<String, String[]>();
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "-2.24", "-0.56", "-2.24", "-0.56", "-4.47", "-1.12", "-4.47", "-1.12" });
    lineTaxes6.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-1.49", "-0.75", "-1.49", "-0.75", "-2.98", "-1.49", "-2.98", "-1.49" });
    line6.setLinetaxes(lineTaxes6);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts6 = new String[] { "-1.49", "-1.49", "-1.49", "-1.49", "-2.98", "-2.98",
        "-2.98", "-2.98" };
    line6.setLineAmounts(lineAmounts6);

    // Line info
    TaxesLineTestData line7 = new TaxesLineTestData();
    line7.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line7.setTaxid(TaxDataConstants.TAX_SUPER);
    line7.setQuantity(new BigDecimal("-1"));
    line7.setPrice(new BigDecimal("1.49"));
    line7.setQuantityUpdated(new BigDecimal("-2"));
    line7.setPriceUpdated(new BigDecimal("1.49"));

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes7 = new HashMap<String, String[]>();
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_25,
        new String[] { "-2.24", "-0.56", "-2.24", "-0.56", "-4.47", "-1.12", "-4.47", "-1.12" });
    lineTaxes7.put(TaxDataConstants.TAX_SUPER_50,
        new String[] { "-1.49", "-0.75", "-1.49", "-0.75", "-2.98", "-1.49", "-2.98", "-1.49" });
    line7.setLinetaxes(lineTaxes7);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts7 = new String[] { "-1.49", "-1.49", "-1.49", "-1.49", "-2.98", "-2.98",
        "-2.98", "-2.98" };
    line7.setLineAmounts(lineAmounts7);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3, line4, line5, line6, line7 });

    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_SUPER_25, new String[] { "-15.65", "-3.91", "-15.65", "-3.91",
        "-31.29", "-7.82", "-31.29", "-7.82" });
    taxes.put(TaxDataConstants.TAX_SUPER_50, new String[] { "-10.43", "-5.22", "-10.43", "-5.22",
        "-20.86", "-10.43", "-20.86", "-10.43" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "-19.56", "-10.43", "-19.56", "-10.43", "-39.11", "-20.86",
        "-39.11", "-20.86" };
    setDocAmounts(amounts);

  }
}
