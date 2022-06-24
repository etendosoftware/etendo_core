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
 * All portions are Copyright (C) 2001-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.businessUtility.BpartnerMiscData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

public class SE_Invoice_BPartner extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strBPartner = info.getStringParameter("inpcBpartnerId", IsIDFilter.instance);
    String strDocType = info.getStringParameter("inpcDoctypetargetId", IsIDFilter.instance);
    String strLocation = info.getStringParameter("inpcBpartnerId_LOC", IsIDFilter.instance);
    String strContact = info.getStringParameter("inpcBpartnerId_CON", IsIDFilter.instance);
    String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());
    String strfinPaymentmethodId = info.getStringParameter("inpfinPaymentmethodId",
        IsIDFilter.instance);
    String strOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
    boolean isSales = StringUtils.equals(strIsSOTrx, "Y");

    // Payment Method changed
    if (StringUtils.equals(strChanged, "inpfinPaymentmethodId")
        && StringUtils.isNotEmpty(strBPartner)) {
      String message = isAutomaticCombination(info.vars, strBPartner, isSales,
          strfinPaymentmethodId, strOrgId);
      info.addResult("MESSAGE", message);
    }

    else {
      if (StringUtils.isEmpty(strBPartner)) {
        info.vars.removeSessionValue(info.getWindowId() + "|C_BPartner_ID");
      }

      BpartnerMiscData[] data = BpartnerMiscData.select(this, strBPartner);
      if (data == null || data.length == 0) {
        info.addResult("inpcBpartnerLocationId", "");
      }

      else {
        // BPartner Location
        if (StringUtils.isNotEmpty(strLocation)) {
          info.addResult("inpcBpartnerLocationId", strLocation);
        }

        // Price List
        String strPriceList = isSales ? data[0].mPricelistId : data[0].poPricelistId;
        if (StringUtils.isEmpty(strPriceList)) {
          strPriceList = SEOrderBPartnerData.defaultPriceList(this, strIsSOTrx,
              info.vars.getClient());
        }
        info.addResult("inpmPricelistId",
            StringUtils.isEmpty(strPriceList)
                ? Utility.getContext(this, info.vars, "#M_PriceList_ID", info.getWindowId())
                : strPriceList);

        // Payment Rule
        String strPaymentRule = isSales ? data[0].paymentrule : data[0].paymentrulepo;
        String docBaseType = SEInvoiceBPartnerData.docBaseType(this, strDocType);
        if (StringUtils.isEmpty(strPaymentRule) && StringUtils.endsWith(docBaseType, "C")) {
          strPaymentRule = "P";
        }
        info.addResult("inppaymentrule", strPaymentRule);

        // Payment Method
        String strFinPaymentMethodId = isSales ? data[0].finPaymentmethodId
            : data[0].poPaymentmethodId;
        info.addResult("inpfinPaymentmethodId", strFinPaymentMethodId);

        // Payment Terms
        String paymentTerm = isSales ? data[0].cPaymenttermId : data[0].poPaymenttermId;
        if (StringUtils.isEmpty(paymentTerm)) {
          BpartnerMiscData[] term = BpartnerMiscData.selectPaymentTerm(this, strOrgId,
              info.vars.getClient());
          if (term.length != 0) {
            paymentTerm = term[0].cPaymenttermId;
          }
        }
        info.addResult("inpcPaymenttermId", paymentTerm);

        // Sales Representative
        FieldProvider[] tld = null;
        try {
          ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLE", "",
              "AD_User SalesRep", "", Utility.getReferenceableOrg(info.vars, strOrgId),
              Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
          Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), "");
          tld = comboTableData.select(false);
          comboTableData = null;
        } catch (Exception ex) {
          throw new ServletException(ex);
        }
        if (tld != null && tld.length > 0) {
          String strUserRep = SEOrderBPartnerData.userIdSalesRep(this, data[0].salesrepId);
          if (StringUtils.isEmpty(strUserRep)) {
            strUserRep = info.vars.getUser();
          }
          info.addSelect("inpsalesrepId");
          for (int i = 0; i < tld.length; i++) {
            info.addSelectResult(tld[i].getField("id"), tld[i].getField("name"),
                StringUtils.equalsIgnoreCase(tld[i].getField("id"), strUserRep));
          }
          info.endSelect();
        } else {
          info.addResult("inpsalesrepId", null);
        }

        // Business Partner Contact
        FieldProvider[] tdv = null;
        try {
          ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLEDIR",
              "AD_User_ID", "", "AD_User C_BPartner User/Contacts",
              Utility.getReferenceableOrg(info.vars, strOrgId),
              Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
          Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), "");
          tdv = comboTableData.select(false);
          comboTableData = null;
        } catch (Exception ex) {
          throw new ServletException(ex);
        }
        if (tdv != null && tdv.length > 0) {
          info.addSelect("inpadUserId");
          if (StringUtils.isEmpty(strContact)) {
            // If a contact has not been specified, the first one is selected
            info.addSelectResult(tdv[0].getField("id"), tdv[0].getField("name"), true);
            for (int i = 1; i < tdv.length; i++) {
              info.addSelectResult(tdv[i].getField("id"), tdv[i].getField("name"), false);
            }
          } else {
            for (int i = 0; i < tdv.length; i++) {
              info.addSelectResult(tdv[i].getField("id"), tdv[i].getField("name"),
                  StringUtils.equalsIgnoreCase(tdv[i].getField("id"), strContact));
            }
          }
          info.endSelect();
        } else {
          info.addResult("inpadUserId", null);
        }

        // Withholding
        String strWithHolding = SEInvoiceBPartnerData.WithHolding(this, strBPartner);
        info.addResult("inpcWithholdingId", strWithHolding);

        // Print Discount
        info.addResult("inpisdiscountprinted", data[0].isdiscountprinted);

        // Project
        info.addResult("inpcProjectId", null);
        info.addResult("inpcProjectId_R", null);

        // If the Business Partner is blocked for this document, show an information message
        String message = "";
        if (FIN_Utility.isBlockedBusinessPartner(strBPartner, isSales, 3)) {
          BusinessPartner bPartner = OBDal.getInstance().get(BusinessPartner.class, strBPartner);
          message = message + OBMessageUtils.messageBD("ThebusinessPartner") + " "
              + bPartner.getIdentifier() + " " + OBMessageUtils.messageBD("BusinessPartnerBlocked");
        }

        // If the Business Partner has negative credit available, show an information message
        if (new BigDecimal(data[0].creditavailable).compareTo(BigDecimal.ZERO) < 0 && isSales) {
          String creditLimitExceed = "" + Double.parseDouble(data[0].creditavailable) * -1;
          String automationPaymentMethod = isAutomaticCombination(info.vars, strBPartner, isSales,
              strFinPaymentMethodId, strOrgId);
          if (StringUtils.isNotEmpty(message)) {
            message = message + "<br>";
          }
          message = message + Utility.messageBD(this, "CreditLimitOver", info.vars.getLanguage())
              + creditLimitExceed + "<br/>" + automationPaymentMethod;
        }

        info.addResult("MESSAGE", message);
      }
    }
  }

  /**
   * Verifies if the given payment method belongs to the default financial account of the given
   * business partner.
   * 
   * @param vars
   *          VariablesSecureApp.
   * @param strBPartnerId
   *          Business Partner id.
   * @param isSales
   *          Sales (true) or purchase (false) transaction.
   * @param strfinPaymentmethodId
   *          Payment Method id.
   * @return Message to be displayed in the application warning the user that automatic actions
   *         could not be performed because given payment method does not belong to the default
   *         financial account of the given business partner.
   */
  private String isAutomaticCombination(VariablesSecureApp vars, String strBPartnerId,
      boolean isSales, String strfinPaymentmethodId, String strOrgId) {
    BusinessPartner bpartner = OBDal.getInstance().get(BusinessPartner.class, strBPartnerId);
    FIN_PaymentMethod selectedPaymentMethod = OBDal.getInstance()
        .get(FIN_PaymentMethod.class, strfinPaymentmethodId);
    OBContext.setAdminMode(true);
    try {
      FIN_FinancialAccount account = null;
      String message = "";

      if (bpartner != null && selectedPaymentMethod != null && StringUtils.isNotEmpty(strOrgId)) {
        account = (isSales) ? bpartner.getAccount() : bpartner.getPOFinancialAccount();
        if (account != null) {
          OBCriteria<FinAccPaymentMethod> obc = OBDal.getInstance()
              .createCriteria(FinAccPaymentMethod.class);
          obc.setFilterOnReadableOrganization(false);
          obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, account));
          obc.add(
              Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, selectedPaymentMethod));
          obc.add(Restrictions.in(FinAccPaymentMethod.PROPERTY_ORGANIZATION + ".id",
              OBContext.getOBContext()
                  .getOrganizationStructureProvider()
                  .getNaturalTree(strOrgId)));

          // filter is on unique constraint so list() size <=1 always
          if (obc.uniqueResult() == null) {
            message = Utility.messageBD(this, "PaymentmethodNotbelongsFinAccount",
                vars.getLanguage());
          }
        } else {
          message = Utility.messageBD(this, "PaymentmethodNotbelongsFinAccount",
              vars.getLanguage());
        }
      }
      return message;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
