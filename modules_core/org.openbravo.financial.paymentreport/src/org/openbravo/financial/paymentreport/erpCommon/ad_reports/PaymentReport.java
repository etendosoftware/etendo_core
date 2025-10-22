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
 * The Initial Developer of the Original Code is Openbravo SL 
 * All portions are Copyright (C) 2009-2017 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.financial.paymentreport.erpCommon.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.ValueListFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.Sqlc;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.info.SelectorUtilityData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SQLReturnObject;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.project.Project;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class PaymentReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static PaymentReportDao dao;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strOrg = vars.getGlobalVariable("inpOrg", "PaymentReport|Organization", "0");
      String strInclSubOrg = vars.getGlobalVariable("inpInclSubOrg",
          "PaymentReport|IncludeSubOrganization", "");
      String strDueDateFrom = vars.getGlobalVariable("inpDueDateFrom", "PaymentReport|DueDateFrom",
          "");
      String strDueDateTo = vars.getRequestGlobalVariable("inpDueDateTo",
          "PaymentReport|DueDateTo");
      String strExpectedDateFrom = vars.getGlobalVariable("inpExpectedDateFrom",
          "PaymentReport|ExpectedDateFrom", "");
      String strExpectedDateTo = vars.getRequestGlobalVariable("inpExpectedDateTo",
          "PaymentReport|ExpectedDateTo");
      String strAmountFrom = vars.getNumericGlobalVariable("inpAmountFrom",
          "PaymentReport|AmountFrom", "");
      String strAmountTo = vars.getNumericGlobalVariable("inpAmountTo", "PaymentReport|AmountTo",
          "");
      String strDocumentDateFrom = vars.getGlobalVariable("inpDocumentDateFrom",
          "PaymentReport|DocumentDateFrom", "");
      String strDocumentDateTo = vars.getGlobalVariable("inpDocumentDateTo",
          "PaymentReport|DocumentDateTo", "");
      String strPaymentDateFrom = vars.getGlobalVariable("inpPaymentDateFrom",
          "PaymentReport|PaymentDateFrom", "");
      String strPaymentDateTo = vars.getGlobalVariable("inpPaymentDateTo",
          "PaymentReport|PaymentDateTo", "");
      String strcBPartnerIdIN = vars.getInGlobalVariable("inpcBPartnerId_IN",
          "PaymentReport|BusinessPartner", "", IsIDFilter.instance);
      String strcBPGroupIdIN = vars.getGlobalVariable("inpcBPGroupId_IN",
          "PaymentReport|BusinessPartnerCategory", "");
      String strsalesrepId = vars.getGlobalVariable("inpSalesrepId", "PaymentReport|SalesRepId", "",
          IsIDFilter.instance);
      String strcNoBusinessPartner = vars.getGlobalVariable("inpcNoBPartner",
          "PaymentReport|noBusinessPartner", "include");
      String strcProjectIdIN = vars.getInGlobalVariable("inpcProjectId_IN", "PaymentReport|Project",
          "", IsIDFilter.instance);
      dao = new PaymentReportDao();
      String strfinPaymSt = vars.getInGlobalVariable("inpfinPaymSt", "", "",
          new ValueListFilter(dao.getReferenceListValues("FIN_Payment status", true)));
      String strPaymentMethodId = vars.getGlobalVariable("inpPaymentMethodId",
          "PaymentReport|PaymentMethodId", "", IsIDFilter.instance);
      String strFinancialAccountId = vars.getGlobalVariable("inpFinancialAccountId",
          "PaymentReport|FinancialAccountId", "", IsIDFilter.instance);
      String strcCurrency = vars.getGlobalVariable("inpcCurrencyId", "PaymentReport|Currency", "",
          IsIDFilter.instance);
      String strConvertCurrency = null;
      OBContext.setAdminMode(true);
      try {
        strConvertCurrency = vars.getGlobalVariable("inpConvertCurrencyId",
            "PaymentReport|ConvertCurrency",
            OBContext.getOBContext().getCurrentClient().getCurrency().getId());
      } finally {
        OBContext.restorePreviousMode();
      }
      if (strConvertCurrency == null) {
        strConvertCurrency = vars.getGlobalVariable("inpConvertCurrencyId",
            "PaymentReport|ConvertCurrency", "");
      }
      ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
      String strConversionDate = vars.getGlobalVariable("inpConversionDate",
          "PaymentReport|ConversionDate", DateTimeData.today(readOnlyCP));
      String strPaymType = vars.getGlobalVariable("inpPaymType", "PaymentReport|PaymentType",
          "FINPR_RecPay");
      String strOverdue = vars.getGlobalVariable("inpOverdue", "PaymentReport|Overdue", "");
      String strBAZero = vars.getGlobalVariable("inpBAZero", "PaymentReport|BAZero", "Y");
      String strGroupCrit = vars.getGlobalVariable("inpGroupCrit", "PaymentReport|GroupCrit", "");
      String strOrdCrit = vars.getInGlobalVariable("inpShown", "PaymentReport|OrdCrit", "",
          new ValueListFilter("Date", "APRM_FATS_BPARTNER", "Project", "INS_CURRENCY",
              "FINPR_BPartner_Category", "ACCS_ACCOUNT_ID_D", "DueDate", ""));
      final String strInclPaymentUsingCredit = vars.getGlobalVariable("inpInclPaymentUsingCredit",
          "PaymentReport|IncludePaymentUsingCredit", "Y");
      printPageDataSheet(response, vars, strOrg, strInclSubOrg, strDueDateFrom, strDueDateTo,
          strExpectedDateFrom, strExpectedDateTo, strAmountFrom, strAmountTo, strDocumentDateFrom,
          strDocumentDateTo, strcBPartnerIdIN, strcBPGroupIdIN, strcNoBusinessPartner,
          strcProjectIdIN, strfinPaymSt, strPaymentMethodId, strFinancialAccountId, strcCurrency,
          strConvertCurrency, strConversionDate, strPaymType, strOverdue, strBAZero, strGroupCrit,
          strOrdCrit, strInclPaymentUsingCredit, strPaymentDateFrom, strPaymentDateTo,
          strsalesrepId);
      // DIRECT is used when coming from Aging Balance Report
    } else if (vars.commandIn("FIND", "DIRECT")) {
      String strOrg = vars.getRequestGlobalVariable("inpOrg", "PaymentReport|Organization");
      String strInclSubOrg = vars.getRequestGlobalVariable("inpInclSubOrg",
          "PaymentReport|IncludeSubOrganization");
      String strDueDateFrom = vars.getRequestGlobalVariable("inpDueDateFrom",
          "PaymentReport|DueDateFrom");
      String strDueDateTo = vars.getRequestGlobalVariable("inpDueDateTo",
          "PaymentReport|DueDateTo");
      String strExpectedDateFrom = vars.getRequestGlobalVariable("inpExpectedDateFrom",
          "PaymentReport|ExpectedDateFrom");
      String strExpectedDateTo = vars.getRequestGlobalVariable("inpExpectedDateTo",
          "PaymentReport|ExpectedDateTo");
      if (StringUtils.isEmpty(vars.getNumericParameter("inpAmountFrom"))) {
        vars.removeSessionValue("PaymentReport|AmountFrom");
      }
      String strAmountFrom = vars.getNumericRequestGlobalVariable("inpAmountFrom",
          "PaymentReport|AmountFrom");
      if (StringUtils.isEmpty(vars.getNumericParameter("inpAmountTo"))) {
        vars.removeSessionValue("PaymentReport|AmountTo");
      }
      String strAmountTo = vars.getNumericRequestGlobalVariable("inpAmountTo",
          "PaymentReport|AmountTo");
      String strDocumentDateFrom = vars.getRequestGlobalVariable("inpDocumentDateFrom",
          "PaymentReport|DocumentDateFrom");
      String strDocumentDateTo = vars.getRequestGlobalVariable("inpDocumentDateTo",
          "PaymentReport|DocumentDateTo");
      String strPaymentDateFrom = vars.getRequestGlobalVariable("inpPaymentDateFrom",
          "PaymentReport|PaymentDateFrom");
      String strPaymentDateTo = vars.getRequestGlobalVariable("inpPaymentDateTo",
          "PaymentReport|PaymentDateTo");
      String strcBPartnerIdIN = "";
      if (vars.commandIn("FIND")) {
        strcBPartnerIdIN = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
            "PaymentReport|BusinessPartner", IsIDFilter.instance);
      } else {// vars.commandIn("DIRECT")
        strcBPartnerIdIN = vars.getRequestGlobalVariable("inpcBPartnerId_IN", "");
      }
      String strcBPGroupIdIN = vars.getRequestGlobalVariable("inpcBPGroupId_IN",
          "PaymentReport|BusinessPartnerCategory");
      String strcNoBusinessPartner = vars.getGlobalVariable("inpcNoBPartner",
          "PaymentReport|noBusinessPartner");
      String strcProjectIdIN = vars.getRequestInGlobalVariable("inpcProjectId_IN",
          "PaymentReport|Project", IsIDFilter.instance);
      String strsalesrepId = vars.getRequestGlobalVariable("inpSalesrepId",
          "PaymentReport|SalesRepId", IsIDFilter.instance);
      dao = new PaymentReportDao();
      String strfinPaymSt = "";
      if (vars.commandIn("FIND")) {
        strfinPaymSt = vars.getRequestInGlobalVariable("inpfinPaymSt", "",
            new ValueListFilter(dao.getReferenceListValues("FIN_Payment status", true)));
      } else {// vars.commandIn("DIRECT")
        strfinPaymSt = vars.getRequestGlobalVariable("inpFinPaymSt", "");
      }
      String strPaymentMethodId = vars.getRequestGlobalVariable("inpPaymentMethodId",
          "PaymentReport|PaymentMethodId", IsIDFilter.instance);
      String strFinancialAccountId = vars.getRequestGlobalVariable("inpFinancialAccountId",
          "PaymentReport|FinancialAccountId", IsIDFilter.instance);
      String strcCurrency = vars.getRequestGlobalVariable("inpcCurrencyId",
          "PaymentReport|Currency", IsIDFilter.instance);
      String strConvertCurrency = vars.getRequestGlobalVariable("inpConvertCurrencyId",
          "PaymentReport|ConvertCurrency", IsIDFilter.instance);
      String strConversionDate = vars.getRequestGlobalVariable("inpConversionDate",
          "PaymentReport|ConversionDate");
      String strPaymType = vars.getRequestGlobalVariable("inpPaymType",
          "PaymentReport|PaymentType");
      String strOverdue = vars.getRequestGlobalVariable("inpOverdue", "PaymentReport|Overdue");
      String strBAZero = vars.getRequestGlobalVariable("inpBAZero", "PaymentReport|BAZero");
      String strGroupCrit = vars.getRequestGlobalVariable("inpGroupCrit",
          "PaymentReport|GroupCrit");
      String strOrdCrit = vars.getRequestInGlobalVariable("inpShown", "PaymentReport|OrdCrit",
          new ValueListFilter("Date", "APRM_FATS_BPARTNER", "Project", "INS_CURRENCY",
              "FINPR_BPartner_Category", "ACCS_ACCOUNT_ID_D", "DueDate", ""));
      final String strInclPaymentUsingCredit = vars.getStringParameter("inpInclPaymentUsingCredit",
          "N");
      vars.setSessionValue("PaymentReport|IncludePaymentUsingCredit", strInclPaymentUsingCredit);
      printPageDataSheet(response, vars, strOrg, strInclSubOrg, strDueDateFrom, strDueDateTo,
          strExpectedDateFrom, strExpectedDateTo, strAmountFrom, strAmountTo, strDocumentDateFrom,
          strDocumentDateTo, strcBPartnerIdIN, strcBPGroupIdIN, strcNoBusinessPartner,
          strcProjectIdIN, strfinPaymSt, strPaymentMethodId, strFinancialAccountId, strcCurrency,
          strConvertCurrency, strConversionDate, strPaymType, strOverdue, strBAZero, strGroupCrit,
          strOrdCrit, strInclPaymentUsingCredit, strPaymentDateFrom, strPaymentDateTo,
          strsalesrepId);
    } else if (vars.commandIn("PDF", "XLS")) {
      String strOrg = vars.getRequestGlobalVariable("inpOrg", "PaymentReport|Organization");
      String strInclSubOrg = vars.getRequestGlobalVariable("inpInclSubOrg",
          "PaymentReport|IncludeSubOrganization");
      String strDueDateFrom = vars.getRequestGlobalVariable("inpDueDateFrom",
          "PaymentReport|DueDateFrom");
      String strDueDateTo = vars.getRequestGlobalVariable("inpDueDateTo",
          "PaymentReport|DueDateTo");
      String strExpectedDateFrom = vars.getRequestGlobalVariable("inpExpectedDateFrom",
          "PaymentReport|ExpectedDateFrom");
      String strExpectedDateTo = vars.getRequestGlobalVariable("inpExpectedDateTo",
          "PaymentReport|ExpectedDateTo");
      String strAmountFrom = vars.getNumericRequestGlobalVariable("inpAmountFrom",
          "PaymentReport|AmountFrom");
      String strAmountTo = vars.getNumericRequestGlobalVariable("inpAmountTo",
          "PaymentReport|AmountTo");
      String strDocumentDateFrom = vars.getRequestGlobalVariable("inpDocumentDateFrom",
          "PaymentReport|DocumentDateFrom");
      String strDocumentDateTo = vars.getRequestGlobalVariable("inpDocumentDateTo",
          "PaymentReport|DocumentDateTo");
      String strPaymentDateFrom = vars.getRequestGlobalVariable("inpPaymentDateFrom",
          "PaymentReport|PaymentDateFrom");
      String strPaymentDateTo = vars.getRequestGlobalVariable("inpPaymentDateTo",
          "PaymentReport|PaymentDateTo");
      String strcBPartnerIdIN = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "PaymentReport|BusinessPartner", IsIDFilter.instance);
      String strcBPGroupIdIN = vars.getRequestGlobalVariable("inpcBPGroupId_IN",
          "PaymentReport|BusinessPartnerCategory");
      String strcNoBusinessPartner = vars.getGlobalVariable("inpcNoBPartner",
          "PaymentReport|noBusinessPartner");
      String strcProjectIdIN = vars.getRequestInGlobalVariable("inpcProjectId_IN",
          "PaymentReport|Project", IsIDFilter.instance);
      dao = new PaymentReportDao();
      String strfinPaymSt = vars.getRequestInGlobalVariable("inpfinPaymSt", "",
          new ValueListFilter(dao.getReferenceListValues("FIN_Payment status", true)));
      String strPaymentMethodId = vars.getRequestGlobalVariable("inpPaymentMethodId",
          "PaymentReport|PaymentMethodId", IsIDFilter.instance);
      String strFinancialAccountId = vars.getRequestGlobalVariable("inpFinancialAccountId",
          "PaymentReport|FinancialAccountId", IsIDFilter.instance);
      String strcCurrency = vars.getRequestGlobalVariable("inpcCurrencyId",
          "PaymentReport|Currency", IsIDFilter.instance);
      String strConvertCurrency = vars.getRequestGlobalVariable("inpConvertCurrencyId",
          "PaymentReport|ConvertCurrency", IsIDFilter.instance);
      String strConversionDate = vars.getRequestGlobalVariable("inpConversionDate",
          "PaymentReport|ConversionDate");
      String strPaymType = vars.getRequestGlobalVariable("inpPaymType",
          "PaymentReport|PaymentType");
      String strOverdue = vars.getRequestGlobalVariable("inpOverdue", "PaymentReport|Overdue");
      String strBAZero = vars.getRequestGlobalVariable("inpBAZero", "PaymentReport|BAZero");
      String strGroupCrit = vars.getRequestGlobalVariable("inpGroupCrit",
          "PaymentReport|GroupCrit");
      String strOrdCrit = vars.getRequestInGlobalVariable("inpShown", "PaymentReport|OrdCrit",
          new ValueListFilter("Date", "APRM_FATS_BPARTNER", "Project", "INS_CURRENCY",
              "FINPR_BPartner_Category", "ACCS_ACCOUNT_ID_D", "DueDate", ""));
      String strOutput = "html";
      final String strInclPaymentUsingCredit = vars.getStringParameter("inpInclPaymentUsingCredit",
          "N");
      vars.setSessionValue("PaymentReport|IncludePaymentUsingCredit", strInclPaymentUsingCredit);
      String strsalesrepId = vars.getRequestGlobalVariable("inpSalesrepId",
          "PaymentReport|SalesRepId", IsIDFilter.instance);
      if (vars.commandIn("PDF")) {
        strOutput = "pdf";
      } else if (vars.commandIn("XLS")) {
        strOutput = "xls";
      }

      printPage(request, response, vars, strOrg, strInclSubOrg, strDueDateFrom, strDueDateTo,
          strExpectedDateFrom, strExpectedDateTo, strAmountFrom, strAmountTo, strDocumentDateFrom,
          strDocumentDateTo, strcBPartnerIdIN, strcBPGroupIdIN, strcNoBusinessPartner,
          strcProjectIdIN, strfinPaymSt, strPaymentMethodId, strFinancialAccountId, strcCurrency,
          strConvertCurrency, strConversionDate, strPaymType, strOverdue, strBAZero, strOutput,
          strGroupCrit, strOrdCrit, strInclPaymentUsingCredit, strPaymentDateFrom, strPaymentDateTo,
          strsalesrepId);

    } else if (vars.commandIn("LINK")) {
      String strTableId = vars.getRequiredStringParameter("inpAdTableId", IsIDFilter.instance);
      String strIsReceipt = vars.getRequiredStringParameter("inpIsReceipt");
      String strRecordId = vars.getRequiredStringParameter("inpRecordId", IsIDFilter.instance);

      HashMap<String, String> data = PaymentReportDao.getLinkParameters(strTableId, strIsReceipt);
      if (data == null || data.size() == 0) {
        bdError(request, response, "RecordError", vars.getLanguage());
      } else {
        String inputName = "inp" + Sqlc.TransformaNombreColumna(data.get("adColumnName"));

        String strWindowPath = Utility.getTabURL(data.get("adTabId"), "R", true);
        if (StringUtils.isEmpty(strWindowPath)) {
          strWindowPath = strDefaultServlet;
        }
        response
            .sendRedirect(strWindowPath + "?" + "Command=DIRECT&" + inputName + "=" + strRecordId);
      }
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strOrg, String strInclSubOrg, String strDueDateFrom, String strDueDateTo,
      String strExpectedDateFrom, String strExpectedDateTo, String strAmountFrom,
      String strAmountTo, String strDocumentDateFrom, String strDocumentDateTo,
      String strcBPartnerIdIN, String strcBPGroupIdIN, String strcNoBusinessPartner,
      String strcProjectIdIN, String strfinPaymSt, String strPaymentMethodId,
      String strFinancialAccountId, String strcCurrency, String strConvertCurrency,
      String strConversionDate, String strPaymType, String strOverdue, String strBAZero,
      String strGroupCrit, String strOrdCrit, String strInclPaymentUsingCredit,
      String strPaymentDateFrom, String strPaymentDateTo, String strsalesrepId)
      throws IOException, ServletException {

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    XmlDocument xmlDocument = null;
    ArrayList<String> discardAL = new ArrayList<String>();
    FieldProvider[] data = null;
    String[] discard = null;

    if (vars.commandIn("FIND", "DIRECT")) {
      dao = new PaymentReportDao();

      try {
        data = dao.getPaymentReport(vars, strOrg, strInclSubOrg, strDueDateFrom, strDueDateTo,
            strAmountFrom, strAmountTo, strDocumentDateFrom, strDocumentDateTo, strcBPartnerIdIN,
            strcBPGroupIdIN, strcNoBusinessPartner, strcProjectIdIN, strfinPaymSt,
            strPaymentMethodId, strFinancialAccountId, strcCurrency, strConvertCurrency,
            strConversionDate, strPaymType, strOverdue, strBAZero, strGroupCrit, strOrdCrit,
            strInclPaymentUsingCredit, strPaymentDateFrom, strPaymentDateTo, strExpectedDateFrom,
            strExpectedDateTo, strsalesrepId, "HTML");
      } catch (OBException e) {
        discardAL.add("sectionGroupCrit");
        discardAL.add("sectionStatus");
        discardAL.add("sectionTotal");
        discardAL.add("sectionTotal2");
        discardAL.add("sectionSubtotalGroupCrit");
        discard = new String[discardAL.size()];

        xmlDocument = xmlEngine
            .readXmlTemplate(
                "org/openbravo/financial/paymentreport/erpCommon/ad_reports/PaymentReport",
                discardAL.toArray(discard))
            .createXmlDocument();

        xmlDocument.setParameter("messageType", "WARNING");
        xmlDocument.setParameter("messageTitle",
            Utility.messageBD(readOnlyCP, "ProcessStatus-W", vars.getLanguage()));
        String noConversionFound = "";
        if (e.getMessage() != null && e.getMessage().contains("->")) {
          noConversionFound = Utility.messageBD(readOnlyCP, "FINPR_NoConversionFound",
              vars.getLanguage());
        }
        xmlDocument.setParameter("messageMessage",
            noConversionFound + Utility.messageBD(readOnlyCP, e.getMessage(), vars.getLanguage()));
      }

      if (data != null && data.length == 0) {

        discardAL.add("sectionGroupCrit");
        discardAL.add("sectionStatus");
        discardAL.add("sectionTotal");
        discardAL.add("sectionTotal2");
        discardAL.add("sectionSubtotalGroupCrit");
        discard = new String[discardAL.size()];

        xmlDocument = xmlEngine
            .readXmlTemplate(
                "org/openbravo/financial/paymentreport/erpCommon/ad_reports/PaymentReport",
                discardAL.toArray(discard))
            .createXmlDocument();

        xmlDocument.setParameter("messageType", "WARNING");
        xmlDocument.setParameter("messageTitle",
            Utility.messageBD(readOnlyCP, "ProcessStatus-W", vars.getLanguage()));
        xmlDocument.setParameter("messageMessage",
            Utility.messageBD(readOnlyCP, "FINPR_NoDataFound", vars.getLanguage()));

      } else if (data != null && data.length == 1 && data[0].getField("conversionDate") != null) {

        String transCurrency = OBDal.getReadOnlyInstance()
            .get(Currency.class, data[0].getField("transCurrency"))
            .getISOCode();
        String baseCurrency = OBDal.getReadOnlyInstance()
            .get(Currency.class, data[0].getField("baseCurrency"))
            .getISOCode();

        String message = transCurrency + " -> " + baseCurrency + " "
            + data[0].getField("conversionDate");

        discardAL.add("sectionGroupCrit");
        discardAL.add("sectionStatus");
        discardAL.add("sectionTotal");
        discardAL.add("sectionTotal2");
        discardAL.add("sectionSubtotalGroupCrit");
        discard = new String[discardAL.size()];

        xmlDocument = xmlEngine
            .readXmlTemplate(
                "org/openbravo/financial/paymentreport/erpCommon/ad_reports/PaymentReport",
                discardAL.toArray(discard))
            .createXmlDocument();

        xmlDocument.setParameter("messageType", "WARNING");
        xmlDocument.setParameter("messageTitle",
            Utility.messageBD(readOnlyCP, "ProcessStatus-W", vars.getLanguage()));
        xmlDocument.setParameter("messageMessage",
            Utility.messageBD(readOnlyCP, "FINPR_NoConversionFound", vars.getLanguage()) + message);

      } else if (data != null) {

        if (StringUtils.isEmpty(strGroupCrit)) {
          discardAL.add("sectionGroupCrit");
          discardAL.add("sectionTotal2");
          discardAL.add("sectionSubtotalGroupCrit");
        } else {
          discardAL.add("sectionStatus");
          discardAL.add("sectionTotal");
        }
        discard = new String[discardAL.size()];

        xmlDocument = xmlEngine
            .readXmlTemplate(
                "org/openbravo/financial/paymentreport/erpCommon/ad_reports/PaymentReport",
                discardAL.toArray(discard))
            .createXmlDocument();

        if (StringUtils.isEmpty(strGroupCrit)) {
          xmlDocument.setData("structure1", data);
        } else {
          xmlDocument.setData("structure2", data);
        }
      }

    } else {
      discardAL.add("sectionGroupCrit");
      discardAL.add("sectionStatus");
      discardAL.add("sectionTotal");
      discardAL.add("sectionTotal2");
      discardAL.add("sectionSubtotalGroupCrit");

      discard = new String[discardAL.size()];

      xmlDocument = xmlEngine
          .readXmlTemplate(
              "org/openbravo/financial/paymentreport/erpCommon/ad_reports/PaymentReport",
              discardAL.toArray(discard))
          .createXmlDocument();
    }

    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "PaymentReport", false, "", "",
        "printPDF();return false;", false, "ad_reports", strReplaceWith, false, true);
    toolbar.setEmail(false);
    toolbar.prepareSimpleToolBarTemplate();
    toolbar.prepareRelationBarTemplate(false, false, "printXLS();return false;");
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.financial.paymentreport.erpCommon.ad_reports.PaymentReport");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(), "PaymentReport.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(), "PaymentReport.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("PaymentReport");
      vars.removeMessage("PaymentReport");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("org", strOrg);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR", "AD_Org_ID",
          "", "", Utility.getContext(readOnlyCP, vars, "#User_Org", "PaymentReport"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "PaymentReport"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "PaymentReport", strOrg);
      xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setParameter("dueDateFrom", strDueDateFrom);
    xmlDocument.setParameter("dueDateFromdisplaySave", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dueDateTo", strDueDateTo);
    xmlDocument.setParameter("dueDateTodisplaySave", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("expectedDateFrom", strExpectedDateFrom);
    xmlDocument.setParameter("expectedDateFromdisplaySave",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("expectedDateTo", strExpectedDateTo);
    xmlDocument.setParameter("expectedDateTodisplaySave",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("amountFrom", strAmountFrom);
    xmlDocument.setParameter("amountTo", strAmountTo);
    xmlDocument.setParameter("documentDateFrom", strDocumentDateFrom);
    xmlDocument.setParameter("documentDateFromdisplaySave",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("documentDateTo", strDocumentDateTo);
    xmlDocument.setParameter("documentDateTodisplaySave",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("paymentDateFrom", strPaymentDateFrom);
    xmlDocument.setParameter("paymentDateFromdisplaySave",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("paymentDateTo", strPaymentDateTo);
    xmlDocument.setParameter("paymentDateTodisplaySave", vars.getSessionValue("#AD_SqlDateFormat"));

    xmlDocument.setData("paramcBPartnerId_IN", "liststructure",
        SelectorUtilityData.selectBpartner(readOnlyCP,
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(readOnlyCP, vars, "#User_Client", ""), strcBPartnerIdIN));

    xmlDocument.setParameter("cBPGroupId_IN", strcBPGroupIdIN);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_BP_Group_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "PaymentReport"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "PaymentReport"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "PaymentReport",
          strcBPGroupIdIN);
      xmlDocument.setData("reportBPGroupId_IN", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("noBusinessPartner", strcNoBusinessPartner);

    xmlDocument.setData("paramcProjectId_IN", "liststructure",
        SelectorUtilityData.selectProject(readOnlyCP,
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(readOnlyCP, vars, "#User_Client", ""), strcProjectIdIN));

    xmlDocument.setParameter("selectedStatuses", strfinPaymSt);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "LIST", "",
          "FIN_Payment status", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "PaymentReport"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "PaymentReport"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "PaymentReport",
          strfinPaymSt);
      xmlDocument.setData("reportStatus", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("paymentMethodId", strPaymentMethodId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "FIN_PaymentMethod_ID", "", "FIN_PaymentMethodsWithAccount",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "PaymentReport"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "PaymentReport"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "PaymentReport",
          strPaymentMethodId);
      xmlDocument.setData("reportPaymentMethod_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("financialAccountId", strFinancialAccountId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "FIN_Financial_Account_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "PaymentReport"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "PaymentReport"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "PaymentReport",
          strFinancialAccountId);
      xmlDocument.setData("reportFinancialAccount_ID", "liststructure",
          comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("cCurrencyId", strcCurrency);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Currency_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "PaymentReport"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "PaymentReport"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "PaymentReport",
          strcCurrency);
      xmlDocument.setData("reportC_Currency_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("cConvertCurrencyId", strConvertCurrency);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Currency_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "PaymentReport"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "PaymentReport"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "PaymentReport",
          strConvertCurrency);
      xmlDocument.setData("reportConvertCurrency_ID", "liststructure",
          comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setParameter("conversionDate", strConversionDate);
    xmlDocument.setParameter("conversionDatedisplaySave",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("conversionDatedisplaySave",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("paymType", strPaymType);
    xmlDocument.setParameter("inclPaymentUsingCredit", strInclPaymentUsingCredit);

    Vector<SQLReturnObject> vector = new Vector<>(0);
    SQLReturnObject sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "FINPR_Receivables");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "FINPR_Receivables", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);
    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "FINPR_Payables");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "FINPR_Payables", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);
    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "FINPR_RecPay");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "FINPR_RecPay", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);

    FieldProvider objectListData[] = new FieldProvider[vector.size()];
    vector.copyInto(objectListData);

    xmlDocument.setData("reportPaymType", "liststructure", objectListData);

    xmlDocument.setParameter("paramOver", strOverdue);
    xmlDocument.setParameter("paramZero", strBAZero);

    xmlDocument.setParameter("groupCrit", strGroupCrit);

    vector = new Vector<>(0);
    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "APRM_FATS_BPARTNER");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "APRM_FATS_BPARTNER", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);
    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "Project");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "Project", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);
    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "FINPR_BPartner_Category");
    sqlRO.setData("NAME",
        Utility.messageBD(readOnlyCP, "FINPR_BPartner_Category", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);
    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "INS_CURRENCY");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "INS_CURRENCY", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);
    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "ACCS_ACCOUNT_ID_D");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "ACCS_ACCOUNT_ID_D", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);

    objectListData = new FieldProvider[vector.size()];
    vector.copyInto(objectListData);

    xmlDocument.setData("reportGroupCrit", "liststructure", objectListData);

    vector = new Vector<>(0);
    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "Date");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "Date", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);

    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "APRM_FATS_BPARTNER");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "APRM_FATS_BPARTNER", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);

    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "Project");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "Project", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);

    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "FINPR_BPartner_Category");
    sqlRO.setData("NAME",
        Utility.messageBD(readOnlyCP, "FINPR_BPartner_Category", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);

    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "INS_CURRENCY");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "INS_CURRENCY", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);

    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "ACCS_ACCOUNT_ID_D");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "ACCS_ACCOUNT_ID_D", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);

    sqlRO = new SQLReturnObject();
    sqlRO.setData("ID", "DueDate");
    sqlRO.setData("NAME", Utility.messageBD(readOnlyCP, "DueDate", vars.getLanguage()));
    sqlRO.setData("DESCRIPTION", "");
    vector.addElement(sqlRO);

    objectListData = new FieldProvider[vector.size()];
    vector.copyInto(objectListData);

    xmlDocument.setData("reportOrdCrit", "liststructure", objectListData);
    xmlDocument.setData("reportShown", "liststructure", PaymentReportDao.getObjectList(strOrdCrit));

    xmlDocument.setParameter("salesrepId", strsalesrepId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLE", "SalesRep_ID",
          "AD_User SalesRep", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "PaymentReport"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "PaymentReport"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "PaymentReport",
          strsalesrepId);
      xmlDocument.setData("reportSalesRep_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("showOneAsterisk", showAsterisk(data, "*") ? "" : "Display:none");
    xmlDocument.setParameter("showTwoAsterisk", showAsterisk(data, "**") ? "" : "Display:none");

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private boolean showAsterisk(FieldProvider[] data, String asterisk) {
    if (StringUtils.isEmpty(asterisk) || data == null) {
      return false;
    }
    boolean showAsterisk = false;
    for (final FieldProvider fp : data) {
      if (fp.getField("INVOICE_NUMBER") != null
          && StringUtils.isNotEmpty(fp.getField("INVOICE_NUMBER"))
          && fp.getField("INVOICE_NUMBER").length() >= asterisk.length()
          && StringUtils.equals(asterisk,
              fp.getField("INVOICE_NUMBER").substring(0, asterisk.length()))
          && !StringUtils.equals(
              fp.getField("INVOICE_NUMBER").substring(asterisk.length(), asterisk.length() + 1),
              "*")) {
        showAsterisk = true;
        break;
      }
    }
    return showAsterisk;
  }

  private void printPage(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strOrg, String strInclSubOrg, String strDueDateFrom,
      String strDueDateTo, String strExpectedDateFrom, String strExpectedDateTo,
      String strAmountFrom, String strAmountTo, String strDocumentDateFrom,
      String strDocumentDateTo, String strcBPartnerIdIN, String strcBPGroupIdIN,
      String strcNoBusinessPartner, String strcProjectIdIN, String strfinPaymSt,
      String strPaymentMethodId, String strFinancialAccountId, String strcCurrency,
      String strConvertCurrency, String strConversionDate, String strPaymType, String strOverdue,
      String strBAZero, String strOutput, String strGroupCrit, String strOrdCrit,
      String strInclPaymentUsingCredit, String strPaymentDateFrom, String strPaymentDateTo,
      String strsalesrepId) throws IOException, ServletException {

    response.setContentType("text/html; charset=UTF-8");

    dao = new PaymentReportDao();
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    FieldProvider[] data = null;
    try {
      data = dao.getPaymentReport(vars, strOrg, strInclSubOrg, strDueDateFrom, strDueDateTo,
          strAmountFrom, strAmountTo, strDocumentDateFrom, strDocumentDateTo, strcBPartnerIdIN,
          strcBPGroupIdIN, strcNoBusinessPartner, strcProjectIdIN, strfinPaymSt, strPaymentMethodId,
          strFinancialAccountId, strcCurrency, strConvertCurrency, strConversionDate, strPaymType,
          strOverdue, strBAZero, strGroupCrit, strOrdCrit, strInclPaymentUsingCredit,
          strPaymentDateFrom, strPaymentDateTo, strExpectedDateFrom, strExpectedDateTo,
          strsalesrepId, strOutput);
    } catch (OBException e) {
      advisePopUp(request, response, "WARNING",
          Utility.messageBD(readOnlyCP, "ProcessStatus-W", vars.getLanguage()),
          Utility.messageBD(readOnlyCP, "FINPR_NoConversionFound", vars.getLanguage())
              + e.getMessage());
    }

    if (data != null && data.length == 1 && data[0] == null) {

      advisePopUp(request, response, "WARNING",
          Utility.messageBD(readOnlyCP, "ProcessStatus-W", vars.getLanguage()),
          Utility.messageBD(readOnlyCP, "FINPR_NoDataFound", vars.getLanguage()));

    } else if (data != null && data.length == 1 && data[0].getField("conversionDate") != null) {

      String transCurrency = OBDal.getReadOnlyInstance()
          .get(Currency.class, data[0].getField("transCurrency"))
          .getISOCode();
      String baseCurrency = OBDal.getReadOnlyInstance()
          .get(Currency.class, data[0].getField("baseCurrency"))
          .getISOCode();

      String message = transCurrency + " -> " + baseCurrency + " "
          + data[0].getField("conversionDate");
      advisePopUp(request, response, "WARNING",
          Utility.messageBD(readOnlyCP, "ProcessStatus-W", vars.getLanguage()),
          Utility.messageBD(readOnlyCP, "FINPR_NoConversionFound", vars.getLanguage()) + message);

    }

    String strReportName = "";

    if (StringUtils.equals(strOutput, "pdf")) {
      // Launch the report as usual, calling the JRXML file
      strReportName = "@basedesign@/org/openbravo/financial/paymentreport/erpCommon/ad_reports/PaymentReportPDF.jrxml";
      response.setHeader("Content-disposition", "inline; filename=PaymentReport.pdf");
    } else if (StringUtils.equals(strOutput, "xls")) {
      strReportName = "@basedesign@/org/openbravo/financial/paymentreport/erpCommon/ad_reports/PaymentReportExcel.jrxml";
      response.setHeader("Content-disposition", "inline; filename=PaymentReportExcel.xls");
    }

    String strSubTitle = "";
    strSubTitle = Utility.messageBD(readOnlyCP, "From", vars.getLanguage()) + " " + strDueDateFrom
        + " " + Utility.messageBD(readOnlyCP, "To", vars.getLanguage()) + " " + strDueDateTo;

    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("REPORT_SUBTITLE", strSubTitle);
    parameters.put("GROUP_CRIT", strGroupCrit);

    String strOrgShow = null;
    String strBPartnerShow = null;
    String strBPGroupShow = null;
    String strProjectShow = null;
    String strStatusShow = null;
    String strPaymentMethodShow = null;
    String strFinancialAccountShow = null;
    String strCurrencyShow = null;
    String strConvertCurrencyShow = null;
    String strOrdCritShow = null;

    String[] elements;

    strOrgShow = StringUtils.isNotEmpty(strOrg)
        ? dao.getObject(Organization.class, strOrg).getIdentifier()
        : "";

    if (StringUtils.isNotEmpty(strcBPartnerIdIN)) {
      strBPartnerShow = strcBPartnerIdIN.substring(2, strcBPartnerIdIN.length() - 2);
      elements = strBPartnerShow.split("', '");
      strBPartnerShow = "";
      for (int i = 0; i < elements.length; i++) {
        if (i < (elements.length - 1)) {
          strBPartnerShow = strBPartnerShow
              .concat(dao.getObject(BusinessPartner.class, elements[i]).getIdentifier()) + ", ";
        } else {
          strBPartnerShow = strBPartnerShow
              .concat(dao.getObject(BusinessPartner.class, elements[i]).getIdentifier());
        }
      }
    } else {
      strBPartnerShow = "";
    }

    strBPGroupShow = StringUtils.isNotEmpty(strcBPGroupIdIN)
        ? dao.getObject(Category.class, strcBPGroupIdIN).getIdentifier()
        : "";

    if (StringUtils.isNotEmpty(strcProjectIdIN)) {
      strProjectShow = strcProjectIdIN.substring(2, strcProjectIdIN.length() - 2);
      elements = strProjectShow.split("', '");
      strProjectShow = "";
      for (int i = 0; i < elements.length; i++) {
        if (i < (elements.length - 1)) {
          strProjectShow = strProjectShow
              .concat(dao.getObject(Project.class, elements[i]).getIdentifier()) + ", ";
        } else {
          strProjectShow = strProjectShow
              .concat(dao.getObject(Project.class, elements[i]).getIdentifier());
        }
      }
    } else {
      strProjectShow = "";
    }

    if (StringUtils.isNotEmpty(strfinPaymSt)) {
      strStatusShow = strfinPaymSt.substring(2, strfinPaymSt.length() - 2);
      elements = strStatusShow.split("', '");
      strStatusShow = "";
      for (int i = 0; i < elements.length; i++) {
        if (i < (elements.length - 1)) {
          strStatusShow = strStatusShow
              .concat(PaymentReportDao.translateRefList(elements[i]) + ", ");
        } else {
          strStatusShow = strStatusShow.concat(PaymentReportDao.translateRefList(elements[i]));
        }
      }
    } else {
      strStatusShow = "";
    }

    strPaymentMethodShow = StringUtils.isNotEmpty(strPaymentMethodId)
        ? dao.getObject(FIN_PaymentMethod.class, strPaymentMethodId).getIdentifier()
        : "";
    strFinancialAccountShow = StringUtils.isNotEmpty(strFinancialAccountId)
        ? dao.getObject(FIN_FinancialAccount.class, strFinancialAccountId).getIdentifier()
        : "";
    strCurrencyShow = StringUtils.isNotEmpty(strcCurrency)
        ? dao.getObject(Currency.class, strcCurrency).getIdentifier()
        : "";
    strConvertCurrencyShow = StringUtils.isNotEmpty(strConvertCurrency)
        ? dao.getObject(Currency.class, strConvertCurrency).getIdentifier()
        : "";

    if (StringUtils.isNotEmpty(strOrdCrit)) {
      elements = strOrdCrit.substring(2, strOrdCrit.length() - 2).split("', '");
      strOrdCritShow = "";

      for (int i = 0; i < elements.length; i++) {
        if (i < (elements.length - 1)) {
          strOrdCritShow = strOrdCritShow
              .concat(Utility.messageBD(readOnlyCP, elements[i], vars.getLanguage()) + ", ");
        } else {
          strOrdCritShow = strOrdCritShow
              .concat(Utility.messageBD(readOnlyCP, elements[i], vars.getLanguage()));
        }
      }
    } else {
      strOrdCritShow = "";
    }

    parameters.put("ORG_SHOW", strOrgShow);
    if (StringUtils.equalsIgnoreCase(strInclSubOrg, "include")) {
      parameters.put("INCL_SUBORG_SHOW", Utility.messageBD(readOnlyCP, "Yes", vars.getLanguage()));
    } else {
      parameters.put("INCL_SUBORG_SHOW", Utility.messageBD(readOnlyCP, "No", vars.getLanguage()));
    }
    parameters.put("DUEDATEFROM_SHOW", strDueDateFrom);
    parameters.put("DUEDATETO_SHOW", strDueDateTo);
    parameters.put("EXPECTEDDATEFROM_SHOW", strExpectedDateFrom);
    parameters.put("EXPECTEDDATETO_SHOW", strExpectedDateTo);
    parameters.put("AMTFROM_SHOW", strAmountFrom);
    parameters.put("AMTTO_SHOW", strAmountTo);
    parameters.put("DOCDATEFROM_SHOW", strDocumentDateFrom);
    parameters.put("DOCDATETO_SHOW", strDocumentDateTo);
    parameters.put("PAYDATEFROM_SHOW", strPaymentDateFrom);
    parameters.put("PAYDATETO_SHOW", strPaymentDateTo);
    parameters.put("BPARTNER_SHOW", strBPartnerShow);
    parameters.put("BPGROUP_SHOW", strBPGroupShow);
    parameters.put("PROJECT_SHOW", strProjectShow);
    parameters.put("STATUS_SHOW", strStatusShow);
    parameters.put("PAYMENT_METHOD_SHOW", strPaymentMethodShow);
    parameters.put("FINANCIAL_ACCOUNT_SHOW", strFinancialAccountShow);
    parameters.put("CURRENCY_SHOW", strCurrencyShow);
    parameters.put("CONVERT_CURRENCY_SHOW", strConvertCurrencyShow);
    parameters.put("CONVERSIONDATE_SHOW", strConversionDate);
    parameters.put("PAYMTYPE_SHOW", Utility.messageBD(readOnlyCP, strPaymType, vars.getLanguage()));
    if (StringUtils.equalsIgnoreCase(strOverdue, "Y")) {
      parameters.put("OVERDUE_SHOW", Utility.messageBD(readOnlyCP, "Yes", vars.getLanguage()));
    } else {
      parameters.put("OVERDUE_SHOW", Utility.messageBD(readOnlyCP, "No", vars.getLanguage()));
    }
    parameters.put("GROUPCRIT_SHOW",
        Utility.messageBD(readOnlyCP, strGroupCrit, vars.getLanguage()));
    parameters.put("ORDCRIT_SHOW", strOrdCritShow);
    if (StringUtils.equals(strInclPaymentUsingCredit, "Y")) {
      parameters.put("INCL_PAYMENTUSINGCREDIT",
          Utility.messageBD(readOnlyCP, "Yes", vars.getLanguage()));
    } else {
      parameters.put("INCL_PAYMENTUSINGCREDIT",
          Utility.messageBD(readOnlyCP, "No", vars.getLanguage()));
    }

    parameters.put("ONE_ASTERISK_SHOW", showAsterisk(data, "*"));
    parameters.put("TWO_ASTERISK_SHOW", showAsterisk(data, "**"));

    if (data != null) {
      renderJR(vars, response, strReportName, strOutput, parameters, data, null);
    }
  }

  @Override
  public String getServletInfo() {
    return "Servlet PaymentReport.";
  }
}
