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
 * All portions are Copyright (C) 2014-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.filterexpression;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.enterprise.context.Dependent;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;

@Dependent
@ComponentProvider.Qualifier(APRMConstants.TRANSACTION_WINDOW_ID)
public class TransactionAddPaymentDisplayLogics extends AddPaymentDisplayLogicsHandler {

  private static final long SEQUENCE = 100l;

  @Override
  protected long getSeq() {
    return SEQUENCE;
  }

  @Override
  public boolean getDocumentDisplayLogic(Map<String, String> requestMap) throws JSONException {
    // Document Type
    return true;
  }

  @Override
  public boolean getOrganizationDisplayLogic(Map<String, String> requestMap) throws JSONException {
    // Organization
    return false;
  }

  /**
   * Determine if the "Credit to use" field should be displayed based on the request map.
   *
   * The field is displayed if:
   * <ul>
   * <li>the document type is not RCIN</li>
   * <li>the generated credit is 0</li>
   * <li>the received from business partner has a credit</li>
   * </ul>
   *
   * @param requestMap
   *          the request map
   * @return true if the field should be displayed, false otherwise
   * @throws JSONException
   *           if the request map does not contain the expected keys
   */
  @Override
  public boolean getCreditToUseDisplayLogic(Map<String, String> requestMap) throws JSONException {
    JSONObject context = new JSONObject(requestMap.get(APRMConstants.CONTEXT));

    String receivedFrom = FIN_Utility.getFirstNonEmpty(context, APRMConstants.RECEIVED_FROM,
        APRMConstants.INPRECEIVED_FROM);
    String document = null;
    String orgId = null;
    String currencyId = null;

    if (StringUtils.isNotBlank(receivedFrom)) {
      document = context.optString(StringUtils.isNotBlank(
              context.optString(APRMConstants.TRXTYPE)) ? APRMConstants.TRXTYPE : APRMConstants.INPTRXTYPE,
          APRMConstants.DEFAULT_EMPTY_VALUE);
      orgId = context.optString(APRMConstants.AD_ORG_ID, APRMConstants.DEFAULT_EMPTY_VALUE);
      currencyId = context.optString(APRMConstants.C_CURRENCY_ID, APRMConstants.DEFAULT_EMPTY_VALUE);
    } else {
      receivedFrom = FIN_Utility.getFirstNonEmpty(context, APRMConstants.C_BPARTNER_ID, APRMConstants.INPC_BPARTNER_ID);
      if (StringUtils.isBlank(receivedFrom)) {
        return false;
      }
      orgId = FIN_Utility.getDefaultOrganization(context);
      currencyId = FIN_Utility.getDefaultFinancialAccount(context).getCurrency().getId();
      document = FIN_Utility.getDefaultAddPaymentDocument(context);
    }

    boolean isOrgAndCurrencyValid = StringUtils.isNotBlank(orgId) && StringUtils.isNotBlank(currencyId);
    boolean isReceivedFromValid = !StringUtils.equals(APRMConstants.NULL_VALUE, receivedFrom);
    boolean isCreditOrDocumentValid = getDefaultGeneratedCredit(requestMap).signum() == 0 || APRMConstants.RCIN.equals(document);

    if (isOrgAndCurrencyValid && isReceivedFromValid && isCreditOrDocumentValid) {
      BusinessPartner bpartner = OBDal.getInstance().get(BusinessPartner.class, receivedFrom);
      Organization org = OBDal.getInstance().get(Organization.class, orgId);
      Currency currency = OBDal.getInstance().get(Currency.class, currencyId);

      BigDecimal customerCredit = new AdvPaymentMngtDao().getCustomerCredit(
          bpartner,
          APRMConstants.RCIN.equals(document),
          org,
          currency
      );
      return customerCredit.signum() > 0;
    }
    return false;
  }

  BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) throws JSONException {
    return BigDecimal.ZERO;
  }

  @Override
  public boolean getBankStatementLineDisplayLogic(Map<String, String> requestMap)
      throws JSONException {
    JSONObject context = new JSONObject(requestMap.get(APRMConstants.CONTEXT));

    // BankStatementLineDisplayLogic
    if (context.has("trxtype")) {
      return true;
    } else {
      return false;

    }

  }
}
