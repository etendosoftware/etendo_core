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

import java.util.HashMap;

public abstract class TaxesTestData {
  private boolean isTaxDocumentLevel;
  private boolean isPriceIncludingTaxes;
  private HashMap<String, String[]> docTaxes;
  private String[] docAmounts;
  private TaxesLineTestData[] linesData;

  public TaxesTestData() {
    initialize();
  }

  public boolean isTaxDocumentLevel() {
    return isTaxDocumentLevel;
  }

  public void setTaxDocumentLevel(boolean isTaxDocumentLevel) {
    this.isTaxDocumentLevel = isTaxDocumentLevel;
  }

  public boolean isPriceIncludingTaxes() {
    return isPriceIncludingTaxes;
  }

  public void setPriceIncludingTaxes(boolean isPriceIncludingTaxes) {
    this.isPriceIncludingTaxes = isPriceIncludingTaxes;
  }

  public HashMap<String, String[]> getDocTaxes() {
    return docTaxes;
  }

  public void setDoctaxes(HashMap<String, String[]> docTaxes) {
    this.docTaxes = docTaxes;
  }

  public String[] getDocAmounts() {
    return docAmounts;
  }

  public void setDocAmounts(String[] docAmounts) {
    this.docAmounts = docAmounts;
  }

  public TaxesLineTestData[] getLinesData() {
    return linesData;
  }

  public void setLinesData(TaxesLineTestData[] linesData) {
    this.linesData = linesData;
  }

  public abstract void initialize();

}
