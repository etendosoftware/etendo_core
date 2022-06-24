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
 * All portions are Copyright (C) 2015-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.taxes.data;

import java.math.BigDecimal;
import java.util.HashMap;

public class TaxesTestData27 extends TaxesTestData {

  @Override
  public void initialize() {

    // Header info
    setTaxDocumentLevel(true);
    setPriceIncludingTaxes(true);

    // Line info
    TaxesLineTestData line = new TaxesLineTestData();
    line.setProductId(ProductDataConstants.FINAL_GOOD_A);
    line.setQuantity(BigDecimal.ONE);
    line.setPrice(new BigDecimal("3"));
    line.setQuantityUpdated(new BigDecimal("2"));
    line.setPriceUpdated(new BigDecimal("3"));
    line.setTaxid(TaxDataConstants.TAX_VAT_CHARGE);

    // Taxes for line level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> lineTaxes = new HashMap<String, String[]>();
    lineTaxes.put(TaxDataConstants.TAX_VAT_3_Child,
        new String[] { "2.90", "0.09", "2.90", "0.09", "5.80", "0.17", "5.80", "0.17" });
    lineTaxes.put(TaxDataConstants.TAX_CHARGE,
        new String[] { "2.90", "0.01", "2.90", "0.01", "5.80", "0.03", "5.80", "0.03" });
    line.setLinetaxes(lineTaxes);

    // Amounts for line level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] lineAmounts = new String[] { "3", "2.9", "3", "2.9", "6", "5.8", "6", "5.8" };
    line.setLineAmounts(lineAmounts);

    // Add lines
    setLinesData(new TaxesLineTestData[] { line });

    // Taxes for document level are provided
    // taxID - {taxableAmtDraftAfterInsert, taxAmtDraftAfterInsert, taxableAmtCompletedAfterInsert,
    // taxAmtCompletedAfterInsert, taxableAmtDraftAfterUpdate, taxAmtDraftAfterUpdate,
    // taxableAmtCompletedAfterUpdate, taxAmtCompletedAfterUpdate}
    HashMap<String, String[]> taxes = new HashMap<String, String[]>();
    taxes.put(TaxDataConstants.TAX_VAT_3_Child,
        new String[] { "2.90", "0.09", "2.90", "0.09", "5.80", "0.17", "5.80", "0.17" });
    taxes.put(TaxDataConstants.TAX_CHARGE,
        new String[] { "2.90", "0.01", "2.90", "0.01", "5.80", "0.03", "5.80", "0.03" });
    setDoctaxes(taxes);

    // Amounts for document level are provided
    // {totalGrossDraftAfterInsert, totalNetDraftAfterInsert, totalGrossCompletedAfterInsert,
    // totalNetCompletedAfterInsert, totalGrossDraftAfterUpdate, totalNetDraftAfterUpdate,
    // totalGrossCompletedAfterUpdate, totalNetCompletedAfterUpdate}
    String[] amounts = new String[] { "3", "2.9", "3", "2.9", "6", "5.8", "6", "5.8" };
    setDocAmounts(amounts);
  }
}
