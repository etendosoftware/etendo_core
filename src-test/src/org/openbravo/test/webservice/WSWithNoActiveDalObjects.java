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
 * All portions are Copyright (C) 2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.webservice;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.test.base.Issue;

/**
 * Tests that ensure dal web services are able to work with non active dal objets.
 * 
 * @author inigo.sanchez
 *
 */
@Issue("32584")
public class WSWithNoActiveDalObjects extends BaseWSTest {

  private static final String CURRENCY_ID_DAL = "<id>100</id>";
  private static final String CURRENCY_ID = "100";

  @Test
  public void dalWebServiceWithNoActiveCurrencyObject() {
    try {
      setActiveOrNoActiveCurrencyObject(false);
      String dalRespNoActive = dalRequest("/ws/dal/Currency/" + CURRENCY_ID);
      assertThat("Response data", dalRespNoActive, containsString(CURRENCY_ID_DAL));
    } finally {
      setActiveOrNoActiveCurrencyObject(true);
    }
  }

  @Test
  public void dalWebServiceWithActiveCurrencyObject() {
    try {
      String dalResp = dalRequest("/ws/dal/Currency/" + CURRENCY_ID);
      assertThat("Response data", dalResp, containsString(CURRENCY_ID_DAL));
    } finally {

    }
  }

  private void setActiveOrNoActiveCurrencyObject(boolean isActive) {
    OBContext.setAdminMode();
    try {
      Currency currency = OBDal.getInstance().get(Currency.class, CURRENCY_ID);
      currency.setActive(isActive);
      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String dalRequest(String urlDalRequest) {
    String currencyDataString;
    OBContext.setAdminMode();
    try {
      currencyDataString = doTestGetRequest(urlDalRequest, null, 200);
    } catch (Exception e) {
      currencyDataString = e.getMessage();
    } finally {
      OBContext.restorePreviousMode();
    }
    return currencyDataString;
  }
}
