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

public class TaxesTestData130 extends TaxesTestData {

  @Override
  public void initialize() {

    // Header info
    setTaxDocumentLevel(true);
    setPriceIncludingTaxes(false);

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
    lineTaxes1.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-84704.40", "-13552.70",
        "-84704.40", "-13552.70", "-169408.80", "-27105.41", "-169408.80", "-27105.41" });
    lineTaxes1.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-66960.00", "-17744.40",
        "-66960.00", "-17744.40", "-133920.00", "-35488.80", "-133920.00", "-35488.80" });
    line1.setLinetaxes(lineTaxes1);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts1 = new String[] { "-66960.00", "-66960.00", "-66960.00", "-66960.00",
        "-133920.00", "-133920.00", "-133920.00", "-133920.00" };
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
    lineTaxes2.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-42352.20", "-6776.35",
        "-42352.20", "-6776.35", "-84704.40", "-13552.70", "-84704.40", "-13552.70" });
    lineTaxes2.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-33480.00", "-8872.20",
        "-33480.00", "-8872.20", "-66960.00", "-17744.40", "-66960.00", "-17744.40" });
    line2.setLinetaxes(lineTaxes2);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts2 = new String[] { "-33480.00", "-33480.00", "-33480.00", "-33480.00",
        "-66960.00", "-66960.00", "-66960.00", "-66960.00" };
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
    lineTaxes3.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-59293.08", "-9486.89",
        "-59293.08", "-9486.89", "-118586.16", "-18973.79", "-118586.16", "-18973.79" });
    lineTaxes3.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-46872.00", "-12421.08",
        "-46872.00", "-12421.08", "-93744.00", "-24842.16", "-93744.00", "-24842.16" });
    line3.setLinetaxes(lineTaxes3);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts3 = new String[] { "-46872.00", "-46872.00", "-46872.00", "-46872.00",
        "-93744.00", "-93744.00", "-93744.00", "-93744.00" };
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
    lineTaxes4.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-16699.38", "-2671.90",
        "-16699.38", "-2671.90", "-33398.77", "-5343.80", "-33398.77", "-5343.80" });
    lineTaxes4.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-10914.63", "-5784.75",
        "-10914.63", "-5784.75", "-21829.26", "-11569.51", "-21829.26", "-11569.51" });
    line4.setLinetaxes(lineTaxes4);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts4 = new String[] { "-10914.63", "-10914.63", "-10914.63", "-10914.63",
        "-21829.26", "-21829.26", "-21829.26", "-21829.26" };
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
    lineTaxes5.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-4680.88", "-748.94",
        "-4680.88", "-748.94", "-9361.76", "-1497.88", "-9361.76", "-1497.88" });
    lineTaxes5.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-3059.40", "-1621.48",
        "-3059.40", "-1621.48", "-6118.80", "-3242.96", "-6118.80", "-3242.96" });
    line5.setLinetaxes(lineTaxes5);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts5 = new String[] { "-3059.40", "-3059.40", "-3059.40", "-3059.40",
        "-6118.80", "-6118.80", "-6118.80", "-6118.80" };
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
    lineTaxes6.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-16488.75", "-2638.20",
        "-16488.75", "-2638.20", "-32977.50", "-5276.40", "-32977.50", "-5276.40" });
    lineTaxes6.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-10776.96", "-5711.79",
        "-10776.96", "-5711.79", "-21553.92", "-11423.58", "-21553.92", "-11423.58" });
    line6.setLinetaxes(lineTaxes6);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts6 = new String[] { "-10776.96", "-10776.96", "-10776.96", "-10776.96",
        "-21553.92", "-21553.92", "-21553.92", "-21553.92" };
    line6.setLineAmounts(lineAmounts6);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line1, line2, line3, line4, line5, line6 });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_IEPS_26_16, new String[] { "-186349.68", "-29815.95",
        "-186349.68", "-29815.95", "-372699.36", "-59631.90", "-372699.36", "-59631.90" });
    taxes.put(TaxDataConstants.TAX_IEPS_26_26, new String[] { "-147312.00", "-39037.68",
        "-147312.00", "-39037.68", "-294624.00", "-78075.36", "-294624.00", "-78075.36" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_16, new String[] { "-37869.01", "-6059.04", "-37869.01",
        "-6059.04", "-75738.03", "-12118.08", "-75738.03", "-12118.08" });
    taxes.put(TaxDataConstants.TAX_IEPS_53_53, new String[] { "-24750.99", "-13118.02", "-24750.99",
        "-13118.02", "-49501.98", "-26236.05", "-49501.98", "-26236.05" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "-260093.68", "-172062.99", "-260093.68", "-172062.99",
        "-520187.37", "-344125.98", "-520187.37", "-344125.98" };
    setDocAmounts(amounts);
  }
}
