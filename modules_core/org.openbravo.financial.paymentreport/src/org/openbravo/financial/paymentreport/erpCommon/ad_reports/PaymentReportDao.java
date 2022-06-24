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
 * All portions are Copyright (C) 2009-2020 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.financial.paymentreport.erpCommon.ad_reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.hibernate.sql.JoinType;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.SQLReturnObject;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.List;
import org.openbravo.model.ad.domain.ListTrl;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.project.Project;
import org.openbravo.utils.Replace;

public class PaymentReportDao {

  private static final long milisecDayConv = (1000 * 60 * 60 * 24);
  static Logger log4j = LogManager.getLogger();
  private java.util.List<String> bpList;
  private java.util.List<String> bpCategoryList;
  private java.util.List<String> projectList;
  private java.util.List<String> acctList;

  public PaymentReportDao() {
  }

  public <T extends BaseOBObject> T getObject(Class<T> t, String strId) {
    return OBDal.getReadOnlyInstance().get(t, strId);
  }

  public FieldProvider[] getPaymentReport(VariablesSecureApp vars, String strOrg,
      String strInclSubOrg, String strDueDateFrom, String strDueDateTo, String strAmountFrom,
      String strAmountTo, String strDocumentDateFrom, String strDocumentDateTo,
      String strcBPartnerIdIN, String strcBPGroupIdIN, String strcProjectIdIN, String strfinPaymSt,
      String strPaymentMethodId, String strFinancialAccountId, String strcCurrency,
      String strConvertCurrency, String strConversionDate, String strPaymType, String strOverdue,
      String strGroupCrit, String strOrdCrit) {

    try {
      return getPaymentReport(vars, strOrg, strInclSubOrg, strDueDateFrom, strDueDateTo,
          strAmountFrom, strAmountTo, strDocumentDateFrom, strDocumentDateTo, strcBPartnerIdIN,
          strcBPGroupIdIN, "include", strcProjectIdIN, strfinPaymSt, strPaymentMethodId,
          strFinancialAccountId, strcCurrency, strConvertCurrency, strConversionDate, strPaymType,
          strOverdue, strGroupCrit, strOrdCrit, "Y", "", "");
    } catch (OBException e) {
      FieldProvider[] fp = new FieldProvider[1];
      HashMap<String, String> hm = new HashMap<String, String>();
      hm.put("transCurrency", strcCurrency);
      hm.put("baseCurrency", strConvertCurrency);
      hm.put("conversionDate", strConversionDate);

      fp[0] = new FieldProviderFactory(hm);
      FieldProvider[] data = fp;

      OBContext.restorePreviousMode();
      return data;
    }
  }

  public FieldProvider[] getPaymentReport(VariablesSecureApp vars, String strOrg,
      String strInclSubOrg, String strDueDateFrom, String strDueDateTo, String strAmountFrom,
      String strAmountTo, String strDocumentDateFrom, String strDocumentDateTo,
      String strcBPartnerIdIN, String strcBPGroupIdIN, String strcProjectIdIN, String strfinPaymSt,
      String strPaymentMethodId, String strFinancialAccountId, String strcCurrency,
      String strConvertCurrency, String strConversionDate, String strPaymType, String strOverdue,
      String strGroupCrit, String strOrdCrit, String strInclPaymentUsingCredit) {

    try {
      return getPaymentReport(vars, strOrg, strInclSubOrg, strDueDateFrom, strDueDateTo,
          strAmountFrom, strAmountTo, strDocumentDateFrom, strDocumentDateTo, strcBPartnerIdIN,
          strcBPGroupIdIN, "include", strcProjectIdIN, strfinPaymSt, strPaymentMethodId,
          strFinancialAccountId, strcCurrency, strConvertCurrency, strConversionDate, strPaymType,
          strOverdue, strGroupCrit, strOrdCrit, strInclPaymentUsingCredit, "", "");
    } catch (OBException e) {
      FieldProvider[] fp = new FieldProvider[1];
      HashMap<String, String> hm = new HashMap<String, String>();
      hm.put("transCurrency", strcCurrency);
      hm.put("baseCurrency", strConvertCurrency);
      hm.put("conversionDate", strConversionDate);

      fp[0] = new FieldProviderFactory(hm);
      FieldProvider[] data = fp;

      OBContext.restorePreviousMode();
      return data;
    }
  }

  public FieldProvider[] getPaymentReport(VariablesSecureApp vars, String strOrg,
      String strInclSubOrg, String strDueDateFrom, String strDueDateTo, String strAmountFrom,
      String strAmountTo, String strDocumentDateFrom, String strDocumentDateTo,
      String strcBPartnerIdIN, String strcBPGroupIdIN, String strcNoBusinessPartner,
      String strcProjectIdIN, String strfinPaymSt, String strPaymentMethodId,
      String strFinancialAccountId, String strcCurrency, String strConvertCurrency,
      String strConversionDate, String strPaymType, String strOverdue, String strGroupCrit,
      String strOrdCrit, String strInclPaymentUsingCredit, String strPaymentDateFrom,
      String strPaymentDateTo) {

    try {
      return getPaymentReport(vars, strOrg, strInclSubOrg, strDueDateFrom, strDueDateTo,
          strAmountFrom, strAmountTo, strDocumentDateFrom, strDocumentDateTo, strcBPartnerIdIN,
          strcBPGroupIdIN, "include", strcProjectIdIN, strfinPaymSt, strPaymentMethodId,
          strFinancialAccountId, strcCurrency, strConvertCurrency, strConversionDate, strPaymType,
          strOverdue, strGroupCrit, strOrdCrit, strInclPaymentUsingCredit, "", "");
    } catch (OBException e) {
      FieldProvider[] fp = new FieldProvider[1];
      HashMap<String, String> hm = new HashMap<String, String>();
      hm.put("transCurrency", strcCurrency);
      hm.put("baseCurrency", strConvertCurrency);
      hm.put("conversionDate", strConversionDate);

      fp[0] = new FieldProviderFactory(hm);
      FieldProvider[] data = fp;

      OBContext.restorePreviousMode();
      return data;
    }
  }

  @Deprecated
  // Deprecated when adding output format to check number of lines allowed
  public FieldProvider[] getPaymentReport(VariablesSecureApp vars, String strOrg,
      String strInclSubOrg, String strDueDateFrom, String strDueDateTo, String strAmountFrom,
      String strAmountTo, String strDocumentDateFrom, String strDocumentDateTo,
      String strcBPartnerIdIN, String strcBPGroupIdIN, String strcNoBusinessPartner,
      String strcProjectIdIN, String strfinPaymSt, String strPaymentMethodId,
      String strFinancialAccountId, String strcCurrency, String strConvertCurrency,
      String strConversionDate, String strPaymType, String strOverdue, String strGroupCrit,
      String strOrdCrit, String strInclPaymentUsingCredit, String strPaymentDateFrom,
      String strPaymentDateTo, String strExpectedDateFrom, String strExpectedDateTo)
      throws OBException {
    return getPaymentReport(vars, strOrg, strInclSubOrg, strDueDateFrom, strDueDateTo,
        strAmountFrom, strAmountTo, strDocumentDateFrom, strDocumentDateTo, strcBPartnerIdIN,
        strcBPGroupIdIN, strcNoBusinessPartner, strcProjectIdIN, strfinPaymSt, strPaymentMethodId,
        strFinancialAccountId, strcCurrency, strConvertCurrency, strConversionDate, strPaymType,
        strOverdue, "Y", strGroupCrit, strOrdCrit, strInclPaymentUsingCredit, strPaymentDateFrom,
        strPaymentDateTo, strExpectedDateFrom, strExpectedDateTo, "", "dummy");
  }

  @Deprecated
  // Deprecated when adding filter for payments with amount 0
  FieldProvider[] getPaymentReport(VariablesSecureApp vars, String strOrg, String strInclSubOrg,
      String strDueDateFrom, String strDueDateTo, String strAmountFrom, String strAmountTo,
      String strDocumentDateFrom, String strDocumentDateTo, String strcBPartnerIdIN,
      String strcBPGroupIdIN, String strcNoBusinessPartner, String strcProjectIdIN,
      String strfinPaymSt, String strPaymentMethodId, String strFinancialAccountId,
      String strcCurrency, String strConvertCurrency, String strConversionDate, String strPaymType,
      String strOverdue, String strGroupCrit, String strOrdCrit, String strInclPaymentUsingCredit,
      String strPaymentDateFrom, String strPaymentDateTo, String strExpectedDateFrom,
      String strExpectedDateTo, String strOutput) throws OBException {
    return getPaymentReport(vars, strOrg, strInclSubOrg, strDueDateFrom, strDueDateTo,
        strAmountFrom, strAmountTo, strDocumentDateFrom, strDocumentDateTo, strcBPartnerIdIN,
        strcBPGroupIdIN, strcNoBusinessPartner, strcProjectIdIN, strfinPaymSt, strPaymentMethodId,
        strFinancialAccountId, strcCurrency, strConvertCurrency, strConversionDate, strPaymType,
        strOverdue, "Y", strGroupCrit, strOrdCrit, strInclPaymentUsingCredit, strPaymentDateFrom,
        strPaymentDateTo, strExpectedDateFrom, strExpectedDateTo, "", strOutput);
  }

  FieldProvider[] getPaymentReport(VariablesSecureApp vars, String strOrg, String strInclSubOrg,
      String strDueDateFrom, String strDueDateTo, String strAmountFrom, String strAmountTo,
      String strDocumentDateFrom, String strDocumentDateTo, String strcBPartnerIdIN,
      String strcBPGroupIdIN, String strcNoBusinessPartner, String strcProjectIdIN,
      String strfinPaymSt, String strPaymentMethodId, String strFinancialAccountId,
      String strcCurrency, String strConvertCurrency, String strConversionDate, String strPaymType,
      String strOverdue, String strBAZero, String strGroupCrit, String strOrdCrit,
      String strInclPaymentUsingCredit, String strPaymentDateFrom, String strPaymentDateTo,
      String strExpectedDateFrom, String strExpectedDateTo, String strOutput) throws OBException {
    return getPaymentReport(vars, strOrg, strInclSubOrg, strDueDateFrom, strDueDateTo,
        strAmountFrom, strAmountTo, strDocumentDateFrom, strDocumentDateTo, strcBPartnerIdIN,
        strcBPGroupIdIN, strcNoBusinessPartner, strcProjectIdIN, strfinPaymSt, strPaymentMethodId,
        strFinancialAccountId, strcCurrency, strConvertCurrency, strConversionDate, strPaymType,
        strOverdue, strBAZero, strGroupCrit, strOrdCrit, strInclPaymentUsingCredit,
        strPaymentDateFrom, strPaymentDateTo, strExpectedDateFrom, strExpectedDateTo, "",
        strOutput);
  }

  FieldProvider[] getPaymentReport(VariablesSecureApp vars, String strOrg, String strInclSubOrg,
      String strDueDateFrom, String strDueDateTo, String strAmountFrom, String strAmountTo,
      String strDocumentDateFrom, String strDocumentDateTo, String strcBPartnerIdIN,
      String strcBPGroupIdIN, String strcNoBusinessPartner, String strcProjectIdIN,
      String strfinPaymSt, String strPaymentMethodId, String strFinancialAccountId,
      String strcCurrency, String strConvertCurrency, String strConversionDate, String strPaymType,
      String strOverdue, String strBAZero, String strGroupCrit, String strOrdCrit,
      String strInclPaymentUsingCredit, String strPaymentDateFrom, String strPaymentDateTo,
      String strExpectedDateFrom, String strExpectedDateTo, String strsalesrepId, String strOutput)
      throws OBException {

    String hsqlScript = "";
    final Map<String, Object> parameters = new HashMap<>();

    String dateFormatString = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
    Currency transCurrency;
    BigDecimal transAmount = null;
    ConversionRate convRate = null;
    ArrayList<FieldProvider> totalData = new ArrayList<>();
    int numberOfElements = 0;
    int lastElement = 0;
    ScrollableResults scroller = null;

    OBContext.setAdminMode(true);
    try {
      // @formatter:off
      hsqlScript += " from FIN_Payment_ScheduleDetail as fpsd "
                 +  "   left join fpsd.paymentDetails as fpd"
                 +  "   left join fpd.finPayment as pay"
                 +  "   left join fpsd.invoicePaymentSchedule as invps"
                 +  "   left join invps.invoice as inv";
      
      if (StringUtils.equalsIgnoreCase(strGroupCrit, "INS_CURRENCY")
          || StringUtils.contains(strOrdCrit, "INS_CURRENCY")) {
        hsqlScript += " left join pay.currency as paycur"
                   +  " left join inv.currency as invcur";
      }
      if (StringUtils.equalsIgnoreCase(strGroupCrit, "Project")
          || StringUtils.contains(strOrdCrit, "Project")) {
        hsqlScript += " left join pay.project as paypro"
                   +  " left join inv.project as invpro";
      }
      if (StringUtils.equalsIgnoreCase(strGroupCrit, "APRM_FATS_BPARTNER")
          || StringUtils.contains(strOrdCrit, "APRM_FATS_BPARTNER")
          || StringUtils.equalsIgnoreCase(strGroupCrit, "FINPR_BPartner_Category")
          || StringUtils.contains(strOrdCrit, "FINPR_BPartner_Category")
          || (StringUtils.isNotEmpty(strcBPGroupIdIN)
              && (StringUtils.equals(strcNoBusinessPartner, "include")
                  || StringUtils.equals(strcNoBusinessPartner, "exclude")))) {
        hsqlScript += " left join pay.businessPartner as paybp"
                   + "  left join inv.businessPartner as invbp";
        if (StringUtils.equalsIgnoreCase(strGroupCrit, "FINPR_BPartner_Category")
            || StringUtils.contains(strOrdCrit, "FINPR_BPartner_Category")
            || (StringUtils.isNotEmpty(strcBPGroupIdIN)
                && (StringUtils.equals(strcNoBusinessPartner, "include")
                    || StringUtils.equals(strcNoBusinessPartner, "exclude")))) {
         hsqlScript +=" left join paybp.businessPartnerCategory as paybpc"
                    + " left join invbp.businessPartnerCategory as invbpc";
        }
      } else if (StringUtils.isNotEmpty(strFinancialAccountId)) {
        hsqlScript += " left join inv.businessPartner as invbp";
      }
      hsqlScript += " where (fpsd.paymentDetails is not null or fpsd.invoicePaymentSchedule is not null)";

      // organization + include sub-organization
      if (StringUtils.isEmpty(strOrg) || StringUtils.equals(strOrg, "0")) {
        hsqlScript +="  and fpsd.organization.id in :readableOrgs";
        parameters.put("readableOrgs", OBContext.getOBContext().getReadableOrganizations());
      } else {
        if (!StringUtils.equalsIgnoreCase(strInclSubOrg, "include")) {
         hsqlScript +=" and fpsd.organization.id = :org";
         parameters.put("org", strOrg);
        } else {
          hsqlScript+=" and fpsd.organization.id in :childTree";
          parameters.put("childTree", OBContext.getOBContext()
                 .getOrganizationStructureProvider()
                 .getChildTree(strOrg, true));
        }
      }

      // Exclude payments that use credit payment
      if (!StringUtils.equalsIgnoreCase(strInclPaymentUsingCredit, "Y")) {
        hsqlScript += " and ("
                   +  "     not (pay.amount = 0 and pay.usedCredit > pay.generatedCredit)"
                   +  "     or pay is null)";
      }

      // due date from - due date to
      if (StringUtils.isNotEmpty(strDueDateFrom)) {
        hsqlScript += " and invps.dueDate >= :dueDateFrom";
        parameters.put("dueDateFrom", FIN_Utility.getDate(strDueDateFrom));
      }
      if (StringUtils.isNotEmpty(strDueDateTo)) {
        hsqlScript += " and invps.dueDate <= :dueDateTo";
        parameters.put("dueDateTo", FIN_Utility.getDate(strDueDateTo));
      }

      // expected date from - expected date to
      if (StringUtils.isNotEmpty(strExpectedDateFrom)) {
        hsqlScript += " and invps.expectedDate >= :expectedDateFrom";
        parameters.put("expectedDateFrom", FIN_Utility.getDate(strExpectedDateFrom));
      }
      if (StringUtils.isNotEmpty(strExpectedDateTo)) {
        hsqlScript += " and invps.expectedDate <= :expectedDateTo";
        parameters.put("expectedDateTo", FIN_Utility.getDate(strExpectedDateTo));
      }

      // document date from - document date to
      if (StringUtils.isNotEmpty(strDocumentDateFrom)) {
        hsqlScript += " and coalesce(inv.invoiceDate, pay.paymentDate) >= :documentDateFrom";
        parameters.put("documentDateFrom", FIN_Utility.getDate(strDocumentDateFrom));
      }
      if (StringUtils.isNotEmpty(strDocumentDateTo)) {
        hsqlScript += " and coalesce(inv.invoiceDate, pay.paymentDate) <= :documentDateTo";
        parameters.put("documentDateTo", FIN_Utility.getDate(strDocumentDateTo));
      }

      // payment date from - payment date to
      if (StringUtils.isNotEmpty(strPaymentDateFrom)) {
        hsqlScript += " and ((pay.paymentDate >= :paymentDateFrom)  "
                   + "      or (pay.paymentDate is null and invps.expectedDate >= :paymentDateFrom))";
        parameters.put("paymentDateFrom", FIN_Utility.getDate(strPaymentDateFrom));
      }
      if (StringUtils.isNotEmpty(strPaymentDateTo)) {
        hsqlScript += " and coalesce(pay.paymentDate, invps.expectedDate) <= :paymentDateTo";
        parameters.put("paymentDateTo", FIN_Utility.getDate(strPaymentDateTo));
      } 

      // Empty Business Partner included
      if (StringUtils.equals(strcNoBusinessPartner, "include")) {

        // business partner
        if (StringUtils.isNotEmpty(strcBPartnerIdIN)) {
          hsqlScript+=" and ((coalesce(pay.businessPartner.id, inv.businessPartner.id) "
                    + "      in :bpIdIn) "
                    + "      or (pay.businessPartner is null and inv.businessPartner is null))";
          parameters.put("bpIdIn", Utility.stringToArrayList(strcBPartnerIdIN.replaceAll("\\(|\\)|'", "")));
        }
        // business partner category
        if (StringUtils.isNotEmpty(strcBPGroupIdIN)) {
          hsqlScript+=" and (coalesce(paybpc.id, invbpc.id) = :bpGroupIdIn "
                    + "     or (pay.businessPartner is null and inv.businessPartner is null))";
          parameters.put("bpGroupIdIn", Utility.stringToArrayList(strcBPGroupIdIN.replaceAll("\\(|\\)|'", "")));
        }

        // Empty Businesss Partner excluded
      } else if (StringUtils.equals(strcNoBusinessPartner, "exclude")) {

        // business partner
        if (StringUtils.isNotEmpty(strcBPartnerIdIN)) {
          hsqlScript+=" and coalesce(pay.businessPartner.id, inv.businessPartner.id) in :bpIdIn";
          parameters.put("bpIdIn", Utility.stringToArrayList(strcBPartnerIdIN.replaceAll("\\(|\\)|'", "")));
        }

        // business partner category
        if (StringUtils.isNotEmpty(strcBPGroupIdIN)) {
          hsqlScript+=" and coalesce(paybpc.id, invbpc.id) = :bpGroupIdIn";
          parameters.put("bpGroupIdIn", Utility.stringToArrayList(strcBPGroupIdIN.replaceAll("\\(|\\)|'", "")));
        }
        // exclude empty business partner
        if (StringUtils.isEmpty(strcBPartnerIdIN) && StringUtils.isEmpty(strcBPGroupIdIN)) {
          hsqlScript+=" and (pay.businessPartner is not null or inv.businessPartner is not null) ";
        }

        // Only Empty Business Partner
      } else {// if ((strcNoBusinessPartner.equals("only")))
          hsqlScript+=" and pay.businessPartner is null and inv.businessPartner is null ";
      }

      // project
      if (StringUtils.isNotEmpty(strcProjectIdIN)) {
        hsqlScript += " and coalesce(pay.project.id, inv.project.id) in :projectIdIn";
        parameters.put("projectIdIn", Utility.stringToArrayList(strcProjectIdIN.replaceAll("\\(|\\)|'", "")));
      }

      // status
      if (StringUtils.isNotEmpty(strfinPaymSt)
          && !StringUtils.equalsIgnoreCase(strfinPaymSt, "('')")) {
        hsqlScript += " and (pay.status in :paymentStatus";
        parameters.put("paymentStatus", Utility.stringToArrayList(strfinPaymSt.replaceAll("\\(|\\)|'", "")));
        if (strfinPaymSt.contains("RPAP")) {
          hsqlScript+="    or fpsd.paymentDetails is null)";
        } else {
          hsqlScript += " )";
        }
      }

      // payment method
      if (StringUtils.isNotEmpty(strPaymentMethodId)) {
        hsqlScript += " and coalesce(pay.paymentMethod.id, invps.finPaymentmethod.id) = :paymentMethodId";
        parameters.put("paymentMethodId", strPaymentMethodId);
      }

      // financial account
      if (StringUtils.isNotEmpty(strFinancialAccountId)) {
        hsqlScript += " and  (pay is not null"
                   + "     and (select case when trans is not null "
                   + "            then trans.account.id "
                   + "            else payment.account.id end "
                   + "          from FIN_Finacc_Transaction trans right "
                   + "            outer join trans.finPayment payment "
                   + "          where payment = pay)"
                   + "        = :financialAccountId"
                   + "     or ((pay is null and inv.salesTransaction = 'Y'"
                   + "          and invbp.account.id = :financialAccountId)"
                   + "         or (pay is null and inv.salesTransaction = 'N'"
                   + "             and invbp.pOFinancialAccount.id = :financialAccountId)"
                   + "        )"
                   + "       )";
        parameters.put("financialAccountId", strFinancialAccountId);
      }

      // currency
      if (StringUtils.isNotEmpty(strcCurrency)) {
        hsqlScript += " and coalesce(pay.currency.id, inv.currency.id) = :currencyId";
        parameters.put("currencyId", strcCurrency);
      }

      // strsalesrepId
      if (StringUtils.isNotEmpty(strsalesrepId)) {
        hsqlScript += " and inv.salesRepresentative.id = :salesRepId";
        parameters.put("salesRepId", strsalesrepId);
      }

      // payment type
      if (StringUtils.equalsIgnoreCase(strPaymType, "FINPR_Receivables")) {
        hsqlScript += " and (pay.receipt = 'Y'"
                   +  "      or inv.salesTransaction = 'Y')";
      } else if (StringUtils.equalsIgnoreCase(strPaymType, "FINPR_Payables")) {
        hsqlScript += " and (pay.receipt = 'N'"
                   +  "      or inv.salesTransaction = 'N')";
      }

      // overdue
      if (StringUtils.isNotEmpty(strOverdue)) {
        hsqlScript += " and invps.outstandingAmount != '0'"
                   +  " and invps.dueDate <  :dueDate";
        parameters.put("dueDate", DateUtils.truncate(new Date(), Calendar.DATE));
      }

      if (!StringUtils.equals(strBAZero, "Y")) {
        hsqlScript += " and not (pay.amount = 0 )";
      }

      if (StringUtils.equals(strOutput, "HTML")) {
        int maxRecords = 1000;
        final Session sessionCount = OBDal.getReadOnlyInstance().getSession();
        final Query<Long> queryCount = sessionCount.createQuery("select count(*)" + hsqlScript, Long.class);
        queryCount.setProperties(parameters);
        final Long hqlRecordsCount = queryCount.list().get(0);
        if ((int) (long) hqlRecordsCount > maxRecords) {
          String message = "FINPR_TooManyRecords";
          throw new OBException(message);
        }
      }

      final String firstLineQuery = ""
          + " select fpsd.id, "
          + "   (select a.sequenceNumber "
          + "    from ADList a "
          + "    where a.reference.id = '575BCB88A4694C27BC013DE9C73E6FE7' "
          + "      and a.searchKey = coalesce(pay.status, 'RPAP')"
          + "   ) as a"
          + hsqlScript
          + " order by ";
      hsqlScript = firstLineQuery;

      if (StringUtils.equalsIgnoreCase(strGroupCrit, "APRM_FATS_BPARTNER")) {
        hsqlScript += " coalesce(paybp.name, invbp.name), ";
      } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "Project")) {
        hsqlScript += " coalesce(paypro.name, invpro.name), ";
      } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "FINPR_BPartner_Category")) {
        hsqlScript += " coalesce(paybpc.name, invbpc.name), ";
      } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "INS_CURRENCY")) {
        hsqlScript += " coalesce(paycur.iSOCode, invcur.iSOCode), ";
      } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "ACCS_ACCOUNT_ID_D")) {
        hsqlScript += " coalesce("
                   + "    (select trans.account.name "
                   + "     from FIN_Finacc_Transaction trans "
                   + "     left outer join trans.finPayment payment "
                   + "     where payment.id=pay.id),"
                   + "    pay.account.name, "
                   + "    'Awaiting Payment'"
                   + "  ), ";
      }
      hsqlScript += " a, coalesce(pay.status, 'RPAP')";

      if (StringUtils.isNotEmpty(strOrdCrit)) {
        String[] strOrdCritList = strOrdCrit.substring(2, strOrdCrit.length() - 2).split("', '");

        for (int i = 0; i < strOrdCritList.length; i++) {
          if (StringUtils.equalsIgnoreCase(strOrdCritList[i], "Date")) {
            hsqlScript += ", inv.invoiceDate";
          }
          if (strOrdCritList[i].contains("Project")) {
            hsqlScript += ",  coalesce(paypro.name, invpro.name)";
          }
          if (strOrdCritList[i].contains("FINPR_BPartner_Category")) {
            hsqlScript += ",  coalesce(paybpc.name, invbpc.name)";
          }
          if (strOrdCritList[i].contains("APRM_FATS_BPARTNER")) {
            hsqlScript += ",  coalesce(paybp.name, invbp.name)";
          }
          if (strOrdCritList[i].contains("INS_CURRENCY")) {
            hsqlScript += ",  coalesce(paycur.iSOCode, invcur.iSOCode)";
          }
          if (StringUtils.equalsIgnoreCase(strOrdCritList[i], "ACCS_ACCOUNT_ID_D")) {
            hsqlScript += ", coalesce("
                       +  "    (select trans.account.name "
                       +  "     from FIN_Finacc_Transaction trans "
                       +  "      left outer join trans.finPayment payment "
                       +  "     where payment.id=pay.id),"
                       +  "    pay.account.name)";
          }
          if (StringUtils.equalsIgnoreCase(strOrdCritList[i], "DueDate")) {
            hsqlScript += ", invps.dueDate";
          }
        }
      }
      
      hsqlScript += ", fpsd.invoicePaymentSchedule.id";

      // @formatter:on
      final Session session = OBDal.getReadOnlyInstance().getSession();
      final Query<Object[]> query = session.createQuery(hsqlScript, Object[].class);
      query.setProperties(parameters);

      scroller = query.scroll(ScrollMode.FORWARD_ONLY);

      FIN_PaymentDetail finPaymDetail;
      Boolean mustGroup;
      String previousFPSDInvoiceId = null;
      String previousPaymentId = null;
      BigDecimal amountSum = BigDecimal.ZERO;
      BigDecimal balanceSum = BigDecimal.ZERO;
      FieldProvider previousRow = null;
      FieldProvider lastGroupedDatarow = null;
      ConversionRate previousConvRate = null;
      boolean isReceipt = false;
      boolean isAmtInLimit = false;

      // Before processing the data the Transactions without a Payment associated are recovered
      java.util.List<FIN_FinaccTransaction> transactionsList = new ArrayList<FIN_FinaccTransaction>();
      if (StringUtils.isEmpty(strsalesrepId)) {
        transactionsList = getTransactionsList(strInclSubOrg, strOrg, strcBPartnerIdIN,
            strFinancialAccountId, strDocumentDateFrom, strDocumentDateTo, strPaymentDateFrom,
            strPaymentDateTo, strAmountFrom, strAmountTo, strcBPGroupIdIN, strcProjectIdIN,
            strfinPaymSt, strcCurrency, strPaymType, strGroupCrit, strOrdCrit,
            strcNoBusinessPartner, strDueDateFrom, strDueDateTo, strExpectedDateFrom,
            strExpectedDateTo);
      }

      // There are three variables involved in this loop. The first one is data, wich is the
      // the one the loop processes. Then grouped data is used to group similar data lines into
      // one. Finally total data adds the remaining information that is not in data.
      int i = 0;
      while (scroller.next()) {
        i++;
        FIN_PaymentScheduleDetail fpsd = OBDal.getReadOnlyInstance()
            .get(FIN_PaymentScheduleDetail.class, scroller.get(0));

        // make a empty FieldProvider instead of saving link to DAL-object
        FieldProvider data = FieldProviderFactory.getFieldProvider(null);

        if (i % 100 == 0) {
          OBDal.getReadOnlyInstance().getSession().clear();
        }
        OBDal.getReadOnlyInstance()
            .getSession()
            .buildLockRequest(LockOptions.NONE)
            .lock(FIN_PaymentScheduleDetail.ENTITY_NAME, fpsd);

        // search for fin_finacc_transaction for this payment
        FIN_FinaccTransaction trx = null;
        FIN_Payment payment = null;
        Invoice invoice = null;
        if (fpsd.getPaymentDetails() != null) {
          payment = fpsd.getPaymentDetails().getFinPayment();
          OBCriteria<FIN_FinaccTransaction> trxQuery = OBDal.getReadOnlyInstance()
              .createCriteria(FIN_FinaccTransaction.class);
          trxQuery.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_FINPAYMENT, payment));
          // uniqueness guaranteed via unique constraint in db
          trx = (FIN_FinaccTransaction) trxQuery.uniqueResult();
        } else {
          invoice = fpsd.getInvoicePaymentSchedule().getInvoice();
        }
        // If the payment schedule detail has a payment detail, then, the information is taken from
        // the payment. If not, the information is taken from the invoice (the else).
        if (fpsd.getPaymentDetails() != null) {
          BusinessPartner bp = getDocumentBusinessPartner(fpsd);
          if (bp == null) {
            FieldProviderFactory.setField(data, "BP_GROUP", "");
            FieldProviderFactory.setField(data, "BPARTNER", "");
          } else {
            // bp_group -- bp_category
            FieldProviderFactory.setField(data, "BP_GROUP",
                bp.getBusinessPartnerCategory().getName());
            // bpartner
            FieldProviderFactory.setField(data, "BPARTNER", bp.getName());
          }

          // transCurrency
          transCurrency = payment.getCurrency();
          FieldProviderFactory.setField(data, "TRANS_CURRENCY", transCurrency.getISOCode());
          // paymentMethod
          FieldProviderFactory.setField(data, "PAYMENT_METHOD",
              payment.getPaymentMethod().getIdentifier());

          // payment
          FieldProviderFactory.setField(data, "PAYMENT",
              ((payment.getPaymentDate() != null) ? dateFormat.format(payment.getPaymentDate())
                  : "Null") + " - " + payment.getDocumentNo());
          // payment description
          FieldProviderFactory.setField(data, "PAYMENT_DESC", payment.getDescription());
          // payment_id
          FieldProviderFactory.setField(data, "PAYMENT_ID", payment.getId());
          // payment_date
          FieldProviderFactory.setField(data, "PAYMENT_DATE",
              (payment.getPaymentDate() != null) ? dateFormat.format(payment.getPaymentDate())
                  : "Null");
          // payment_docNo
          FieldProviderFactory.setField(data, "PAYMENT_DOCNO", payment.getDocumentNo());
          // payment yes / no
          FieldProviderFactory.setField(data, "PAYMENT_Y_N", "");
          // financialAccount
          FieldProviderFactory.setField(data, "FINANCIAL_ACCOUNT",
              !payment.getFINFinaccTransactionList().isEmpty()
                  ? payment.getFINFinaccTransactionList().get(0).getAccount().getName()
                  : payment.getAccount().getName());
          // status
          FieldProviderFactory.setField(data, "STATUS", translateRefList(payment.getStatus()));
          FieldProviderFactory.setField(data, "STATUS_CODE", payment.getStatus());
          // is receipt
          if (payment.isReceipt()) {
            FieldProviderFactory.setField(data, "ISRECEIPT", "Y");
            isReceipt = true;
          } else {
            FieldProviderFactory.setField(data, "ISRECEIPT", "N");
            isReceipt = false;
          }
          // deposit/withdraw date
          if (trx != null) {
            FieldProviderFactory.setField(data, "DEPOSIT_WITHDRAW_DATE",
                dateFormat.format(trx.getTransactionDate()));
          } else {
            FieldProviderFactory.setField(data, "DEPOSIT_WITHDRAW_DATE", "");
          }
        } else {

          // bp_group -- bp_category
          FieldProviderFactory.setField(data, "BP_GROUP",
              invoice.getBusinessPartner().getBusinessPartnerCategory().getName());
          // bpartner
          FieldProviderFactory.setField(data, "BPARTNER", invoice.getBusinessPartner().getName());
          // transCurrency
          transCurrency = invoice.getCurrency();
          FieldProviderFactory.setField(data, "TRANS_CURRENCY", transCurrency.getISOCode());
          // paymentMethod
          FieldProviderFactory.setField(data, "PAYMENT_METHOD",
              fpsd.getInvoicePaymentSchedule().getFinPaymentmethod().getIdentifier());
          // payment
          FieldProviderFactory.setField(data, "PAYMENT", "");
          // payment_id
          FieldProviderFactory.setField(data, "PAYMENT_ID", "");
          // payment_date
          FieldProviderFactory.setField(data, "PAYMENT_DATE", "");
          // payment_docNo
          FieldProviderFactory.setField(data, "PAYMENT_DOCNO", "");
          // payment yes / no
          FieldProviderFactory.setField(data, "PAYMENT_Y_N", "Display:None");
          // financialAccount
          FieldProviderFactory.setField(data, "FINANCIAL_ACCOUNT", "");
          // status
          FieldProviderFactory.setField(data, "STATUS", translateRefList("RPAP"));
          FieldProviderFactory.setField(data, "STATUS_CODE", "RPAP");
          // is receipt
          if (invoice.isSalesTransaction()) {
            FieldProviderFactory.setField(data, "ISRECEIPT", "Y");
            isReceipt = true;
          } else {
            FieldProviderFactory.setField(data, "ISRECEIPT", "N");
            isReceipt = false;
          }
          // deposit/withdraw date
          FieldProviderFactory.setField(data, "DEPOSIT_WITHDRAW_DATE", "");
        }

        /*
         * - If the payment schedule detail has an invoice, the line is filled normally.
         * 
         * - If it has a payment it does not have an invoice or it should have entered the first if,
         * thus, it is a credit payment. If it is a credit payment, it is checked whether it pays
         * one or multiple invoices. If it is one, the information of that invoice is provided. If
         * not, it is filled with '**'.
         * 
         * - Otherwise, it is filled empty.
         */
        if (fpsd.getInvoicePaymentSchedule() != null) {
          fillLine(dateFormat, data, fpsd, fpsd.getInvoicePaymentSchedule(), false);
        } else if (payment != null) {
          java.util.List<Invoice> invoices = getInvoicesUsingCredit(payment);
          if (invoices.size() == 1) {
            java.util.List<FIN_PaymentSchedule> ps = getInvoicePaymentSchedules(payment);
            fillLine(dateFormat, data, fpsd, ps.get(0), true);
          } else {
            // project
            FieldProviderFactory.setField(data, "PROJECT", "");
            // salesPerson
            FieldProviderFactory.setField(data, "SALES_PERSON", "");
            // invoiceNumber.
            FieldProviderFactory.setField(data, "INVOICE_NUMBER",
                invoices.size() > 1 ? "**" + getInvoicesDocNos(invoices) : "");
            // payment plan id
            FieldProviderFactory.setField(data, "PAYMENT_PLAN_ID", "");
            // payment plan yes / no
            FieldProviderFactory.setField(data, "PAYMENT_PLAN_Y_N",
                invoices.size() != 1 ? "Display:none" : "");
            // payment plan yes / no
            FieldProviderFactory.setField(data, "NOT_PAYMENT_PLAN_Y_N",
                invoices.size() > 1 ? "" : "Display:none");
            // invoiceDate
            FieldProviderFactory.setField(data, "INVOICE_DATE", "");
            // dueDate.
            FieldProviderFactory.setField(data, "DUE_DATE", "");
            // expectedDate.
            FieldProviderFactory.setField(data, "EXPECTED_DATE", "");
            // plannedDSO
            FieldProviderFactory.setField(data, "PLANNED_DSO", "0");
            // currentDSO
            FieldProviderFactory.setField(data, "CURRENT_DSO", "0");
            // daysOverdue
            FieldProviderFactory.setField(data, "OVERDUE", "0");
          }
        } else {
          // project
          FieldProviderFactory.setField(data, "PROJECT", "");
          // salesPerson
          FieldProviderFactory.setField(data, "SALES_PERSON", "");
          // invoiceNumber.
          FieldProviderFactory.setField(data, "INVOICE_NUMBER", "");
          // payment plan id
          FieldProviderFactory.setField(data, "PAYMENT_PLAN_ID", "");
          // payment plan yes / no
          FieldProviderFactory.setField(data, "PAYMENT_PLAN_Y_N", "Display:none");
          // payment plan yes / no
          FieldProviderFactory.setField(data, "NOT_PAYMENT_PLAN_Y_N", "Display:none");
          // invoiceDate
          FieldProviderFactory.setField(data, "INVOICE_DATE", "");
          // dueDate.
          FieldProviderFactory.setField(data, "DUE_DATE", "");
          // expectedDate.
          FieldProviderFactory.setField(data, "EXPECTED_DATE", "");
          // plannedDSO
          FieldProviderFactory.setField(data, "PLANNED_DSO", "0");
          // currentDSO
          FieldProviderFactory.setField(data, "CURRENT_DSO", "0");
          // daysOverdue
          FieldProviderFactory.setField(data, "OVERDUE", "0");

        }

        // transactional and base amounts
        transAmount = fpsd.getAmount();

        Currency baseCurrency = OBDal.getReadOnlyInstance().get(Currency.class, strConvertCurrency);

        boolean sameCurrency = StringUtils.equalsIgnoreCase(baseCurrency.getISOCode(),
            transCurrency.getISOCode());

        if (!sameCurrency) {
          convRate = this.getConversionRate(transCurrency, baseCurrency, strConversionDate);

          if (convRate != null) {
            final int stdPrecission = convRate.getToCurrency().getStandardPrecision().intValue();
            if (isReceipt) {
              FieldProviderFactory.setField(data, "TRANS_AMOUNT", transAmount.toString());
              FieldProviderFactory.setField(data, "BASE_AMOUNT",
                  transAmount.multiply(convRate.getMultipleRateBy())
                      .setScale(stdPrecission, RoundingMode.HALF_UP)
                      .toString());
            } else {
              FieldProviderFactory.setField(data, "TRANS_AMOUNT", transAmount.negate().toString());
              FieldProviderFactory.setField(data, "BASE_AMOUNT",
                  transAmount.multiply(convRate.getMultipleRateBy())
                      .setScale(stdPrecission, RoundingMode.HALF_UP)
                      .negate()
                      .toString());
            }
          } else {
            String message = transCurrency.getISOCode() + " -> " + baseCurrency.getISOCode() + " "
                + strConversionDate;
            throw new OBException(message);
          }
        } else {
          convRate = null;
          if (isReceipt) {
            FieldProviderFactory.setField(data, "TRANS_AMOUNT", transAmount.toString());
            FieldProviderFactory.setField(data, "BASE_AMOUNT", transAmount.toString());
          } else {
            FieldProviderFactory.setField(data, "TRANS_AMOUNT", transAmount.negate().toString());
            FieldProviderFactory.setField(data, "BASE_AMOUNT", transAmount.negate().toString());
          }
        }

        // currency
        FieldProviderFactory.setField(data, "BASE_CURRENCY", baseCurrency.getISOCode());
        // baseCurrency
        FieldProviderFactory.setField(data, "TRANS_CURRENCY", transCurrency.getISOCode());

        // Balance
        String status = "RPAE";
        try {
          status = payment.getStatus();
        } catch (NullPointerException e) {
        }
        final boolean isCreditPayment = fpsd.getInvoicePaymentSchedule() == null && payment != null;

        BigDecimal balance = BigDecimal.ZERO;
        if (isCreditPayment && status != null && "PWNC RPR RPPC PPM RDNC".indexOf(status) >= 0) {
          balance = payment.getGeneratedCredit().subtract(payment.getUsedCredit());
          if (isReceipt) {
            balance = balance.negate();
          }
        } else if (!isCreditPayment && status != null
            && "PWNC RPR RPPC PPM RDNC RPVOID".indexOf(status) == -1) {
          balance = isReceipt ? transAmount : transAmount.negate();
        }
        if (convRate != null) {
          final int stdPrecission = convRate.getToCurrency().getStandardPrecision().intValue();
          balance = balance.multiply(convRate.getMultipleRateBy())
              .setScale(stdPrecission, RoundingMode.HALF_UP);
        }
        FieldProviderFactory.setField(data, "BALANCE", balance.toString());

        finPaymDetail = fpsd.getPaymentDetails();

        // Payment Schedule Detail grouping criteria
        if (finPaymDetail != null && fpsd.getInvoicePaymentSchedule() != null) {
          mustGroup = StringUtils.equalsIgnoreCase(payment.getId(), previousPaymentId)
              && StringUtils.equalsIgnoreCase(fpsd.getInvoicePaymentSchedule().getId(),
                  previousFPSDInvoiceId);
          previousFPSDInvoiceId = fpsd.getInvoicePaymentSchedule().getId();
          previousPaymentId = payment.getId();
        } else if (finPaymDetail != null && fpsd.getInvoicePaymentSchedule() == null) {
          mustGroup = StringUtils.equalsIgnoreCase(payment.getId(), previousPaymentId)
              && previousFPSDInvoiceId == null;
          previousPaymentId = payment.getId();
          previousFPSDInvoiceId = null;
        } else if (finPaymDetail == null && fpsd.getInvoicePaymentSchedule() != null) {
          mustGroup = previousPaymentId == null && StringUtils
              .equalsIgnoreCase(fpsd.getInvoicePaymentSchedule().getId(), previousFPSDInvoiceId);
          previousPaymentId = null;
          previousFPSDInvoiceId = fpsd.getInvoicePaymentSchedule().getId();
        } else {
          mustGroup = false;
        }

        if (mustGroup) {
          amountSum = amountSum.add(transAmount);
          balanceSum = balanceSum.add(balance);
        } else {
          if (previousRow != null) {
            // The current row has nothing to do with the previous one. Because of that, the
            // previous row has to be added to grouped data.
            if (StringUtils.equalsIgnoreCase(previousRow.getField("ISRECEIPT"), "Y")) {
              FieldProviderFactory.setField(previousRow, "TRANS_AMOUNT", amountSum.toString());
            } else {
              FieldProviderFactory.setField(previousRow, "TRANS_AMOUNT",
                  amountSum.negate().toString());
            }
            FieldProviderFactory.setField(previousRow, "BALANCE", balanceSum.toString());
            if (previousConvRate == null) {
              if (StringUtils.equalsIgnoreCase(previousRow.getField("ISRECEIPT"), "Y")) {
                FieldProviderFactory.setField(previousRow, "BASE_AMOUNT", amountSum.toString());
              } else {
                FieldProviderFactory.setField(previousRow, "BASE_AMOUNT",
                    amountSum.negate().toString());
              }
            } else {
              final int stdPrecission = previousConvRate.getToCurrency()
                  .getStandardPrecision()
                  .intValue();
              if (StringUtils.equalsIgnoreCase(previousRow.getField("ISRECEIPT"), "Y")) {
                FieldProviderFactory.setField(previousRow, "BASE_AMOUNT",
                    amountSum.multiply(previousConvRate.getMultipleRateBy())
                        .setScale(stdPrecission, RoundingMode.HALF_UP)
                        .toString());
              } else {
                FieldProviderFactory.setField(previousRow, "BASE_AMOUNT",
                    amountSum.multiply(previousConvRate.getMultipleRateBy())
                        .setScale(stdPrecission, RoundingMode.HALF_UP)
                        .negate()
                        .toString());
              }
            }

            if (StringUtils.isEmpty(strAmountFrom) && StringUtils.isEmpty(strAmountTo)) {
              isAmtInLimit = true;
            } else if (StringUtils.isNotEmpty(strAmountFrom) && StringUtils.isEmpty(strAmountTo)) {
              isAmtInLimit = Double.parseDouble(previousRow.getField("TRANS_AMOUNT")) >= Double
                  .parseDouble(strAmountFrom);
            } else if (StringUtils.isEmpty(strAmountFrom) && StringUtils.isNotEmpty(strAmountTo)) {
              isAmtInLimit = Double.parseDouble(previousRow.getField("TRANS_AMOUNT")) <= Double
                  .parseDouble(strAmountTo);
            } else {
              isAmtInLimit = Double.parseDouble(previousRow.getField("TRANS_AMOUNT")) >= Double
                  .parseDouble(strAmountFrom)
                  && Double.parseDouble(previousRow.getField("TRANS_AMOUNT")) <= Double
                      .parseDouble(strAmountTo);
            }
            if (isAmtInLimit) {
              lastGroupedDatarow = previousRow;
              isAmtInLimit = false;
              numberOfElements++;
            }
          }
          previousRow = data;
          previousConvRate = convRate;
          amountSum = transAmount;
          balanceSum = balance;
        }

        // group_crit_id this is the column that has the ids of the grouping criteria selected
        if (StringUtils.equalsIgnoreCase(strGroupCrit, "APRM_FATS_BPARTNER")) {
          FieldProviderFactory.setField(previousRow, "GROUP_CRIT_ID",
              previousRow.getField("BPARTNER"));
          FieldProviderFactory.setField(previousRow, "GROUP_CRIT", "Business Partner");
        } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "Project")) {
          FieldProviderFactory.setField(previousRow, "GROUP_CRIT_ID",
              previousRow.getField("PROJECT"));
          FieldProviderFactory.setField(previousRow, "GROUP_CRIT", "Project");
        } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "FINPR_BPartner_Category")) {
          FieldProviderFactory.setField(previousRow, "GROUP_CRIT_ID",
              previousRow.getField("BP_GROUP"));
          FieldProviderFactory.setField(previousRow, "GROUP_CRIT", "Business Partner Category");
        } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "INS_CURRENCY")) {
          FieldProviderFactory.setField(previousRow, "GROUP_CRIT_ID",
              previousRow.getField("TRANS_CURRENCY"));
          FieldProviderFactory.setField(previousRow, "GROUP_CRIT", "Currency");
        } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "ACCS_ACCOUNT_ID_D")) {
          FieldProviderFactory.setField(previousRow, "GROUP_CRIT_ID",
              previousRow.getField("FINANCIAL_ACCOUNT"));
          FieldProviderFactory.setField(previousRow, "GROUP_CRIT", "Financial Account");
        } else {
          FieldProviderFactory.setField(previousRow, "GROUP_CRIT_ID", "");
        }

        // Insert the transactions without payment if necessary
        if (lastElement != numberOfElements) {
          if (!transactionsList.isEmpty()) {
            try {
              insertIntoTotal(lastGroupedDatarow, transactionsList, totalData, strGroupCrit,
                  strOrdCrit, strConvertCurrency, strConversionDate);
            } catch (OBException e) {
              // If there is no conversion rate
              throw e;
            }
          }
          totalData.add(lastGroupedDatarow);
          lastElement++;
        }

      }
      if (previousRow != null) {
        // The current row has nothing to do with the previous one. Because of that, the
        // previous row has to be added to grouped data.
        if (StringUtils.equalsIgnoreCase(previousRow.getField("ISRECEIPT"), "Y")) {
          FieldProviderFactory.setField(previousRow, "TRANS_AMOUNT", amountSum.toString());
        } else {
          FieldProviderFactory.setField(previousRow, "TRANS_AMOUNT", amountSum.negate().toString());
        }
        FieldProviderFactory.setField(previousRow, "BALANCE", balanceSum.toString());
        if (previousConvRate == null) {
          if (StringUtils.equalsIgnoreCase(previousRow.getField("ISRECEIPT"), "Y")) {
            FieldProviderFactory.setField(previousRow, "BASE_AMOUNT", amountSum.toString());
          } else {
            FieldProviderFactory.setField(previousRow, "BASE_AMOUNT",
                amountSum.negate().toString());
          }
        } else {
          final int stdPrecission = previousConvRate.getToCurrency()
              .getStandardPrecision()
              .intValue();
          if (StringUtils.equalsIgnoreCase(previousRow.getField("ISRECEIPT"), "Y")) {
            FieldProviderFactory.setField(previousRow, "BASE_AMOUNT",
                amountSum.multiply(previousConvRate.getMultipleRateBy())
                    .setScale(stdPrecission, RoundingMode.HALF_UP)
                    .toString());
          } else {
            FieldProviderFactory.setField(previousRow, "BASE_AMOUNT",
                amountSum.multiply(previousConvRate.getMultipleRateBy())
                    .setScale(stdPrecission, RoundingMode.HALF_UP)
                    .negate()
                    .toString());
          }
        }

        if (StringUtils.isEmpty(strAmountFrom) && StringUtils.isEmpty(strAmountTo)) {
          isAmtInLimit = true;
        } else if (StringUtils.isNotEmpty(strAmountFrom) && StringUtils.isEmpty(strAmountTo)) {
          isAmtInLimit = Double.parseDouble(previousRow.getField("TRANS_AMOUNT")) >= Double
              .parseDouble(strAmountFrom);
        } else if (StringUtils.isEmpty(strAmountFrom) && StringUtils.isNotEmpty(strAmountTo)) {
          isAmtInLimit = Double.parseDouble(previousRow.getField("TRANS_AMOUNT")) <= Double
              .parseDouble(strAmountTo);
        } else {
          isAmtInLimit = Double.parseDouble(previousRow.getField("TRANS_AMOUNT")) >= Double
              .parseDouble(strAmountFrom)
              && Double.parseDouble(previousRow.getField("TRANS_AMOUNT")) <= Double
                  .parseDouble(strAmountTo);
        }
        if (isAmtInLimit) {
          lastGroupedDatarow = previousRow;
          isAmtInLimit = false;
          numberOfElements++;
        }
      }

      // Insert the transactions without payment if necessary
      if (lastElement != numberOfElements) {
        if (!transactionsList.isEmpty()) {
          try {
            insertIntoTotal(lastGroupedDatarow, transactionsList, totalData, strGroupCrit,
                strOrdCrit, strConvertCurrency, strConversionDate);
          } catch (OBException e) {
            // If there is no conversion rate
            throw e;
          }
        }
        totalData.add(lastGroupedDatarow);
        lastElement++;
      }

      // Insert the remaining transactions wihtout payment if necessary
      while (!transactionsList.isEmpty()) {
        // throws OBException if there is no conversion rate
        FieldProvider transactionData = createFieldProviderForTransaction(transactionsList.get(0),
            strGroupCrit, strConvertCurrency, strConversionDate);
        totalData.add(transactionData);
        transactionsList.remove(0);
      }
    } finally {
      if (scroller != null) {
        scroller.close();
      }
      OBContext.restorePreviousMode();
    }
    return totalData.toArray(new FieldProvider[totalData.size()]);
  }

  /**
   * This method combines the information from the transactions list and the last element inserted
   * into grouped data into total data.
   * 
   * @throws OBException
   */
  private boolean insertIntoTotal(FieldProvider data,
      java.util.List<FIN_FinaccTransaction> transactionsList, ArrayList<FieldProvider> totalData,
      String strGroupCrit, String strOrdCrit, String strConvertCurrency, String strConversionDate)
      throws OBException {

    while (!transactionsList.isEmpty()
        && transactionIsBefore(transactionsList.get(0), data, strGroupCrit, strOrdCrit)) {
      // throws OBException if there is no conversion rate
      FieldProvider transactionData = createFieldProviderForTransaction(transactionsList.get(0),
          strGroupCrit, strConvertCurrency, strConversionDate);
      totalData.add(transactionData);
      transactionsList.remove(0);
    }
    return true;
  }

  /**
   * This method creates a field provider with the information of the transaction
   * 
   * @throws OBException
   */
  private FieldProvider createFieldProviderForTransaction(FIN_FinaccTransaction transaction,
      String strGroupCrit, String strConvertCurrency, String strConversionDate) throws OBException {
    String dateFormatString = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
    BigDecimal transAmount = null;
    ConversionRate convRate = null;

    // call with null to return empty map without any link to a dal object
    FieldProvider transactionData = FieldProviderFactory.getFieldProvider(null);

    // bp_group -- bp_category
    if (transaction.getBusinessPartner() != null) {
      FieldProviderFactory.setField(transactionData, "BP_GROUP",
          transaction.getBusinessPartner().getBusinessPartnerCategory().getName());
      // bpartner
      FieldProviderFactory.setField(transactionData, "BPARTNER",
          transaction.getBusinessPartner().getName());
    } else {
      // bp_group -- bp_category & bpartner
      FieldProviderFactory.setField(transactionData, "BP_GROUP", "");
      FieldProviderFactory.setField(transactionData, "BPARTNER", "");
    }
    // transCurrency
    FieldProviderFactory.setField(transactionData, "TRANS_CURRENCY",
        transaction.getCurrency().getISOCode());
    // paymentMethod
    FieldProviderFactory.setField(transactionData, "PAYMENT_METHOD", "");
    // payment
    FieldProviderFactory.setField(transactionData, "PAYMENT", "");
    // description
    FieldProviderFactory.setField(transactionData, "PAYMENT_DESC", transaction.getDescription());
    // payment_id
    FieldProviderFactory.setField(transactionData, "PAYMENT_ID", "");
    // payment_date
    FieldProviderFactory.setField(transactionData, "PAYMENT_DATE",
        dateFormat.format(transaction.getDateAcct()));
    // payment_docNo
    FieldProviderFactory.setField(transactionData, "PAYMENT_DOCNO", "");
    // payment yes / no
    FieldProviderFactory.setField(transactionData, "PAYMENT_Y_N", "Display:None");
    // financialAccount
    FieldProviderFactory.setField(transactionData, "FINANCIAL_ACCOUNT",
        transaction.getAccount().getName());
    // status
    FieldProviderFactory.setField(transactionData, "STATUS",
        translateRefList(transaction.getStatus()));
    FieldProviderFactory.setField(transactionData, "STATUS_CODE", transaction.getStatus());
    // is receipt
    if (StringUtils.equals(transaction.getStatus(), "PWNC")) {
      FieldProviderFactory.setField(transactionData, "ISRECEIPT", "Y");
      // isReceipt = true;
    } else if (StringUtils.equals(transaction.getStatus(), "RDNC")) {
      FieldProviderFactory.setField(transactionData, "ISRECEIPT", "N");
      // isReceipt = false;
    }
    // deposit/withdraw date
    FieldProviderFactory.setField(transactionData, "DEPOSIT_WITHDRAW_DATE",
        dateFormat.format(transaction.getDateAcct()));
    // project
    FieldProviderFactory.setField(transactionData, "PROJECT", "");
    // salesPerson
    FieldProviderFactory.setField(transactionData, "SALES_PERSON", "");
    // invoiceNumber.
    FieldProviderFactory.setField(transactionData, "INVOICE_NUMBER", "");
    // payment plan id
    FieldProviderFactory.setField(transactionData, "PAYMENT_PLAN_ID", "");
    // payment plan yes / no
    FieldProviderFactory.setField(transactionData, "PAYMENT_PLAN_Y_N", "Display:none");
    // payment plan yes / no
    FieldProviderFactory.setField(transactionData, "NOT_PAYMENT_PLAN_Y_N", "Display:none");
    // invoiceDate
    FieldProviderFactory.setField(transactionData, "INVOICE_DATE", "");
    // dueDate.
    FieldProviderFactory.setField(transactionData, "DUE_DATE",
        dateFormat.format(transaction.getDateAcct()));
    // expectedDate.
    FieldProviderFactory.setField(transactionData, "EXPECTED_DATE",
        dateFormat.format(transaction.getDateAcct()));
    // plannedDSO
    FieldProviderFactory.setField(transactionData, "PLANNED_DSO", "0");
    // currentDSO
    FieldProviderFactory.setField(transactionData, "CURRENT_DSO", "0");
    // daysOverdue
    FieldProviderFactory.setField(transactionData, "OVERDUE", "0");

    // transactional and base amounts
    transAmount = transaction.getDepositAmount().subtract(transaction.getPaymentAmount());

    Currency baseCurrency = OBDal.getReadOnlyInstance().get(Currency.class, strConvertCurrency);

    boolean sameCurrency = StringUtils.equalsIgnoreCase(baseCurrency.getISOCode(),
        transaction.getCurrency().getISOCode());

    if (!sameCurrency) {
      convRate = this.getConversionRate(transaction.getCurrency(), baseCurrency, strConversionDate);

      if (convRate != null) {
        final int stdPrecission = convRate.getToCurrency().getStandardPrecision().intValue();
        FieldProviderFactory.setField(transactionData, "TRANS_AMOUNT", transAmount.toString());
        FieldProviderFactory.setField(transactionData, "BASE_AMOUNT",
            transAmount.multiply(convRate.getMultipleRateBy())
                .setScale(stdPrecission, RoundingMode.HALF_UP)
                .toString());
      } else {
        String message = transaction.getCurrency().getISOCode() + " -> " + baseCurrency.getISOCode()
            + " " + strConversionDate;

        throw new OBException(message);
      }
    } else {
      // convRate = null;
      FieldProviderFactory.setField(transactionData, "TRANS_AMOUNT", transAmount.toString());
      FieldProviderFactory.setField(transactionData, "BASE_AMOUNT", transAmount.toString());
    }
    // currency
    FieldProviderFactory.setField(transactionData, "BASE_CURRENCY", baseCurrency.getISOCode());
    // group_crit_id this is the column that has the ids of the grouping criteria selected
    if (StringUtils.equalsIgnoreCase(strGroupCrit, "APRM_FATS_BPARTNER")) {
      FieldProviderFactory.setField(transactionData, "GROUP_CRIT_ID",
          transactionData.getField("BPARTNER"));
      FieldProviderFactory.setField(transactionData, "GROUP_CRIT", "Business Partner");
    } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "Project")) {
      FieldProviderFactory.setField(transactionData, "GROUP_CRIT_ID",
          transactionData.getField("PROJECT"));
      FieldProviderFactory.setField(transactionData, "GROUP_CRIT", "Project");
    } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "FINPR_BPartner_Category")) {
      FieldProviderFactory.setField(transactionData, "GROUP_CRIT_ID",
          transactionData.getField("BP_GROUP"));
      FieldProviderFactory.setField(transactionData, "GROUP_CRIT", "Business Partner Category");
    } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "INS_CURRENCY")) {
      FieldProviderFactory.setField(transactionData, "GROUP_CRIT_ID",
          transactionData.getField("TRANS_CURRENCY"));
      FieldProviderFactory.setField(transactionData, "GROUP_CRIT", "Currency");
    } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "ACCS_ACCOUNT_ID_D")) {
      FieldProviderFactory.setField(transactionData, "GROUP_CRIT_ID",
          transactionData.getField("FINANCIAL_ACCOUNT"));
      FieldProviderFactory.setField(transactionData, "GROUP_CRIT", "Financial Account");
    } else {
      FieldProviderFactory.setField(transactionData, "GROUP_CRIT_ID", "");
      FieldProviderFactory.setField(transactionData, "GROUP_CRIT", "");
    }

    return transactionData;
  }

  /**
   * This method compares the transaction element with the previous data element and returns true if
   * the transaction goes before according to the comparing parameters
   */
  private boolean transactionIsBefore(FIN_FinaccTransaction transaction, FieldProvider data,
      String strGroupCrit, String strOrdCrit) {

    boolean isBefore = false;
    String bpName = "";
    String bpCategory = "";
    String strProject = "";
    if (transaction.getBusinessPartner() != null) {
      bpName = transaction.getBusinessPartner().getName();
      bpCategory = transaction.getBusinessPartner().getBusinessPartnerCategory().getName();
    }
    if (transaction.getProject() != null) {
      strProject = transaction.getProject().getId();
    }

    if (StringUtils.isNotEmpty(strGroupCrit)) {

      // General boolean rule for comparation when A!=B -->[ (A<B || B="") && A!="" ]
      if (StringUtils.equalsIgnoreCase(strGroupCrit, "APRM_FATS_BPARTNER")) {
        if (bpList == null) {
          createBPList();
        }
        int posData = bpList.indexOf(data.getField("BPARTNER"));
        int pos = bpList.indexOf(bpName);

        if (StringUtils.equals(bpName, data.getField("BPARTNER"))) {
          isBefore = isBeforeStatusAndOrder(transaction, data, strOrdCrit, bpName, bpCategory,
              strProject);
        } else if ((pos < posData || StringUtils.isEmpty(data.getField("BPARTNER")))
            && StringUtils.isNotEmpty(bpName)) {
          isBefore = true;
        }
      } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "Project")) {
        if (projectList == null) {
          createProjectList();
        }
        int posData = projectList.indexOf(data.getField("PROJECT"));
        int pos = projectList.indexOf(strProject);

        if (StringUtils.equals(strProject, data.getField("PROJECT"))) {
          isBefore = isBeforeStatusAndOrder(transaction, data, strOrdCrit, bpName, bpCategory,
              strProject);
        } else if ((pos < posData || StringUtils.isEmpty(data.getField("PROJECT")))
            && StringUtils.isNotEmpty(strProject)) {
          isBefore = true;
        }
      } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "FINPR_BPartner_Category")) {
        if (bpCategoryList == null) {
          createBPCategoryList();
        }
        int posData = bpList.indexOf(data.getField("BP_GROUP"));
        int pos = bpList.indexOf(bpCategory);

        if (StringUtils.equals(bpCategory, data.getField("BP_GROUP"))) {
          isBefore = isBeforeStatusAndOrder(transaction, data, strOrdCrit, bpName, bpCategory,
              strProject);
        } else if ((pos < posData || StringUtils.isEmpty(data.getField("BP_GROUP")))
            && StringUtils.isNotEmpty(bpCategory)) {
          isBefore = true;
        }
      } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "INS_CURRENCY")) {
        if (transaction.getCurrency()
            .getISOCode()
            .compareTo(data.getField("TRANS_CURRENCY")) == 0) {
          isBefore = isBeforeStatusAndOrder(transaction, data, strOrdCrit, bpName, bpCategory,
              strProject);
        } else if (transaction.getCurrency()
            .getISOCode()
            .compareTo(data.getField("TRANS_CURRENCY")) < 0) {
          isBefore = true;
        }
      } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "ACCS_ACCOUNT_ID_D")) {
        if (acctList == null) {
          createAcctList();
        }
        int posData = acctList.indexOf(data.getField("FINANCIAL_ACCOUNT"));
        int pos = acctList.indexOf(transaction.getAccount().getName());

        if (StringUtils.equals(transaction.getAccount().getName(),
            data.getField("FINANCIAL_ACCOUNT"))) {
          isBefore = isBeforeStatusAndOrder(transaction, data, strOrdCrit, bpName, bpCategory,
              strProject);
        } else if ((pos < posData || StringUtils.isEmpty(data.getField("FINANCIAL_ACCOUNT")))
            && StringUtils.isNotEmpty(transaction.getAccount().getName())) {
          isBefore = true;
        }
      }

    } else {
      isBefore = isBeforeStatusAndOrder(transaction, data, strOrdCrit, bpName, bpCategory,
          strProject);
    }
    return isBefore;
  }

  /**
   * This method compares the status and the order criteria of the transactions and data
   */
  private boolean isBeforeStatusAndOrder(FIN_FinaccTransaction transaction, FieldProvider data,
      String strOrdCrit, String BPName, String BPCategory, String strProject) {
    boolean isBefore = false;

    if (StringUtils.equals(transaction.getStatus(), data.getField("STATUS_CODE"))) {
      if (StringUtils.isNotEmpty(strOrdCrit)) {
        String[] strOrdCritList = strOrdCrit.substring(2, strOrdCrit.length() - 2).split("', '");
        isBefore = isBeforeOrder(transaction, data, strOrdCritList, 0, BPName, BPCategory,
            strProject);
      }
    } else if (isBeforeStatus(transaction.getStatus(), data.getField("STATUS_CODE"))) {
      isBefore = true;
    }
    return isBefore;
  }

  /**
   * This method compares recursively the order criteria of the transactions and data
   */
  private boolean isBeforeOrder(FIN_FinaccTransaction transaction, FieldProvider data,
      String[] strOrdCritList, int i, String bpName, String bpCategory, String strProject) {
    boolean isBefore = false;

    if (i == strOrdCritList.length - 1) {
      if (strOrdCritList[i].contains("Project")) {
        if (projectList == null) {
          createProjectList();
        }
        int posData = projectList.indexOf(data.getField("PROJECT"));
        int pos = projectList.indexOf(strProject);

        isBefore = isBefore || (((pos < posData) || StringUtils.isEmpty(data.getField("PROJECT")))
            && StringUtils.isNotEmpty(strProject));
      }
      if (strOrdCritList[i].contains("FINPR_BPartner_Category")) {
        if (bpCategoryList == null) {
          createBPCategoryList();
        }
        int posData = bpCategoryList.indexOf(data.getField("BP_GROUP"));
        int pos = bpCategoryList.indexOf(bpCategory);

        isBefore = isBefore || (((pos < posData) || StringUtils.isEmpty(data.getField("BP_GROUP")))
            && StringUtils.isNotEmpty(bpCategory));
      }
      if (strOrdCritList[i].contains("APRM_FATS_BPARTNER")) {
        if (bpList == null) {
          createBPList();
        }
        int posData = bpList.indexOf(data.getField("BPARTNER"));
        int pos = bpList.indexOf(bpName);

        isBefore = isBefore || (((pos < posData) || StringUtils.isEmpty(data.getField("BPARTNER")))
            && StringUtils.isNotEmpty(bpName));
      }
      if (strOrdCritList[i].contains("INS_CURRENCY")) {
        isBefore = isBefore || (transaction.getCurrency()
            .getISOCode()
            .compareTo(data.getField("TRANS_CURRENCY")) < 0);
      }
      if (StringUtils.equalsIgnoreCase(strOrdCritList[i], "DueDate")) {
        Date dataDate = FIN_Utility.getDate(data.getField("DUE_DATE"));
        isBefore = isBefore || (transaction.getDateAcct().compareTo(dataDate) < 0);
      }
      if (StringUtils.equalsIgnoreCase(strOrdCritList[i], "ACCS_ACCOUNT_ID_D")) {
        if (acctList == null) {
          createAcctList();
        }
        int posData = acctList.indexOf(data.getField("FINANCIAL_ACCOUNT"));
        int pos = acctList.indexOf(transaction.getAccount().getName());
        isBefore = isBefore
            || (((pos < posData) || StringUtils.isEmpty(data.getField("FINANCIAL_ACCOUNT")))
                && StringUtils.isNotEmpty(transaction.getAccount().getName()));
      }
      return isBefore;
    } else {
      if (strOrdCritList[i].contains("Project")) {
        if (projectList == null) {
          createProjectList();
        }
        int posData = projectList.indexOf(data.getField("PROJECT"));
        int pos = projectList.indexOf(strProject);

        if ((pos < posData || StringUtils.isEmpty(data.getField("PROJECT")))
            && StringUtils.isNotEmpty(strProject)) {
          isBefore = true;
        } else if (StringUtils.equals(strProject, data.getField("PROJECT"))) {
          isBefore = isBeforeOrder(transaction, data, strOrdCritList, i + 1, bpName, bpCategory,
              strProject);
        }
      } else if (strOrdCritList[i].contains("FINPR_BPartner_Category")) {
        if (bpCategoryList == null) {
          createBPCategoryList();
        }
        int posData = bpCategoryList.indexOf(data.getField("BP_GROUP"));
        int pos = bpCategoryList.indexOf(bpCategory);

        if ((pos < posData || StringUtils.isEmpty(data.getField("BP_GROUP")))
            && StringUtils.isNotEmpty(bpCategory)) {
          isBefore = true;
        } else if (StringUtils.equals(bpCategory, data.getField("BP_GROUP"))) {
          isBefore = isBeforeOrder(transaction, data, strOrdCritList, i + 1, bpName, bpCategory,
              strProject);
        }
      } else if (strOrdCritList[i].contains("APRM_FATS_BPARTNER")) {
        if (bpList == null) {
          createBPList();
        }
        int posData = bpList.indexOf(data.getField("BPARTNER"));
        int pos = bpList.indexOf(bpName);

        if ((pos < posData || StringUtils.isEmpty(data.getField("BPARTNER")))
            && StringUtils.isNotEmpty(bpName)) {
          isBefore = true;
        } else if (StringUtils.equals(bpName, data.getField("BPARTNER"))) {
          isBefore = isBeforeOrder(transaction, data, strOrdCritList, i + 1, bpName, bpCategory,
              strProject);
        }
      } else if (strOrdCritList[i].contains("INS_CURRENCY")) {
        if (transaction.getCurrency().getISOCode().compareTo(data.getField("TRANS_CURRENCY")) < 0) {
          isBefore = true;
        } else if (transaction.getCurrency()
            .getISOCode()
            .compareTo(data.getField("TRANS_CURRENCY")) == 0) {
          isBefore = isBeforeOrder(transaction, data, strOrdCritList, i + 1, bpName, bpCategory,
              strProject);
        }
      } else if (StringUtils.equalsIgnoreCase(strOrdCritList[i], "Date")) {
        Date dataDate = FIN_Utility.getDate(data.getField("INVOICE_DATE"));
        if (dataDate != null) {
          isBefore = false;
        } else {
          isBefore = isBeforeOrder(transaction, data, strOrdCritList, i + 1, bpName, bpCategory,
              strProject);
        }
      } else if (StringUtils.equalsIgnoreCase(strOrdCritList[i], "DueDate")) {
        Date dataDate = FIN_Utility.getDate(data.getField("DUE_DATE"));
        if (dataDate == null) {
          isBefore = true;
        } else if ((transaction.getDateAcct().compareTo(dataDate) < 0)) {
          isBefore = true;
        } else {
          isBefore = isBeforeOrder(transaction, data, strOrdCritList, i + 1, bpName, bpCategory,
              strProject);
        }
      } else if (StringUtils.equalsIgnoreCase(strOrdCritList[i], "ACCS_ACCOUNT_ID_D")) {
        if (acctList == null) {
          createAcctList();
        }
        int posData = acctList.indexOf(data.getField("FINANCIAL_ACCOUNT"));
        int pos = acctList.indexOf(transaction.getAccount().getName());

        if ((pos < posData || StringUtils.isEmpty(data.getField("FINANCIAL_ACCOUNT")))
            && StringUtils.isNotEmpty(transaction.getAccount().getName())) {
          isBefore = true;
        } else if (StringUtils.equals(transaction.getAccount().getName(),
            data.getField("FINANCIAL_ACCOUNT"))) {
          isBefore = isBeforeOrder(transaction, data, strOrdCritList, i + 1, bpName, bpCategory,
              strProject);
        }
      }

      return isBefore;
    }
  }

  /**
   * Compares two DIFFERENT payment status. If the first one goes before the second it returns true,
   * elsewise it returns false
   */
  private boolean isBeforeStatus(String firstValue, String secondValue) {
    Object[] strStatus = { firstValue, secondValue };
    boolean isBefore = false;

    OBContext.setAdminMode(true);
    try {
      OBCriteria<List> obCriteria = OBDal.getReadOnlyInstance().createCriteria(List.class);
      obCriteria.createAlias(List.PROPERTY_REFERENCE, "r", JoinType.LEFT_OUTER_JOIN);
      obCriteria.add(Restrictions.ilike(
          "r." + org.openbravo.model.ad.domain.Reference.PROPERTY_NAME, "FIN_Payment status"));
      obCriteria.add(Restrictions.in(List.PROPERTY_SEARCHKEY, strStatus));
      obCriteria.addOrderBy(List.PROPERTY_SEQUENCENUMBER, true);
      obCriteria.addOrderBy(List.PROPERTY_SEARCHKEY, true);
      final java.util.List<List> statusList = obCriteria.list();
      List status = statusList.get(0);
      if (StringUtils.equals(status.getSearchKey(), firstValue)) {
        isBefore = true;
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return isBefore;
  }

  /**
   * 
   * This method returns a list of transactions without a payment associated
   */
  private java.util.List<FIN_FinaccTransaction> getTransactionsList(String strInclSubOrg,
      String strOrg, String strcBPartnerIdIN, String strFinancialAccountId,
      String strDocumentDateFrom, String strDocumentDateTo, String strPaymentDateFrom,
      String strPaymentDateTo, String strAmountFrom, String strAmountTo, String strcBPGroupIdIN,
      String strcProjectIdIN, String strfinPaymSt, String strcCurrency, String strPaymType,
      String strGroupCrit, String strOrdCrit, String strcNoBusinessPartner, String strDueDateFrom,
      String strDueDateTo, String strExpectedDateFrom, String strExpectedDateTo) {
    String localStrfinPaymSt = strfinPaymSt;
    Organization[] organizations;
    if (StringUtils.equalsIgnoreCase(strInclSubOrg, "include")) {
      Set<String> orgChildTree = OBContext.getOBContext()
          .getOrganizationStructureProvider()
          .getChildTree(strOrg, true);
      organizations = getOrganizations(orgChildTree);
    } else {
      organizations = new Organization[1];
      organizations[0] = OBDal.getReadOnlyInstance().get(Organization.class, strOrg);
    }
    java.util.List<BusinessPartner> bPartners = OBDao
        .getOBObjectListFromString(BusinessPartner.class, strcBPartnerIdIN);
    java.util.List<Project> projects = OBDao.getOBObjectListFromString(Project.class,
        strcProjectIdIN);
    OBContext.setAdminMode(true);
    try {
      OBCriteria<FIN_FinaccTransaction> obCriteriaTrans = OBDal.getReadOnlyInstance()
          .createCriteria(FIN_FinaccTransaction.class);
      obCriteriaTrans.createAlias(FIN_FinaccTransaction.PROPERTY_BUSINESSPARTNER, "bp",
          JoinType.LEFT_OUTER_JOIN);
      obCriteriaTrans.createAlias("bp." + BusinessPartner.PROPERTY_BUSINESSPARTNERCATEGORY, "bpc",
          JoinType.LEFT_OUTER_JOIN);
      obCriteriaTrans.createAlias(FIN_FinaccTransaction.PROPERTY_PROJECT, "p",
          JoinType.LEFT_OUTER_JOIN);
      obCriteriaTrans.createAlias(FIN_FinaccTransaction.PROPERTY_CURRENCY, "c",
          JoinType.LEFT_OUTER_JOIN);
      obCriteriaTrans.createAlias(FIN_FinaccTransaction.PROPERTY_ACCOUNT, "acc",
          JoinType.LEFT_OUTER_JOIN);
      obCriteriaTrans.add(Restrictions.isNull(FIN_FinaccTransaction.PROPERTY_FINPAYMENT));
      obCriteriaTrans.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_PROCESSED, true));
      obCriteriaTrans.add(
          Restrictions.in(FIN_FinaccTransaction.PROPERTY_ORGANIZATION, (Object[]) organizations));

      // Empty Business Partner included
      if (StringUtils.equals(strcNoBusinessPartner, "include")) {

        // BPartners
        if (!bPartners.isEmpty()) {
          obCriteriaTrans.add(Restrictions.or(
              Restrictions.in(FIN_FinaccTransaction.PROPERTY_BUSINESSPARTNER, bPartners),
              Restrictions.isNull(FIN_FinaccTransaction.PROPERTY_BUSINESSPARTNER)));
        }

        // BPartner Category
        if (StringUtils.isNotEmpty(strcBPGroupIdIN)) {
          obCriteriaTrans.add(Restrictions.or(
              Restrictions.eq("bp." + BusinessPartner.PROPERTY_BUSINESSPARTNERCATEGORY,
                  strcBPGroupIdIN),
              Restrictions.isNull(FIN_FinaccTransaction.PROPERTY_BUSINESSPARTNER)));
        }

        // Empty Business Partner excluded
      } else if (StringUtils.equals(strcNoBusinessPartner, "exclude")) {

        // BPartners
        if (!bPartners.isEmpty()) {
          obCriteriaTrans
              .add(Restrictions.in(FIN_FinaccTransaction.PROPERTY_BUSINESSPARTNER, bPartners));
        }

        // BPartner Category
        if (StringUtils.isNotEmpty(strcBPGroupIdIN)) {
          obCriteriaTrans.add(Restrictions
              .eq("bp." + BusinessPartner.PROPERTY_BUSINESSPARTNERCATEGORY, strcBPGroupIdIN));
        }

        if (bPartners.isEmpty() && StringUtils.isEmpty(strcBPGroupIdIN)) {
          obCriteriaTrans
              .add(Restrictions.isNotNull(FIN_FinaccTransaction.PROPERTY_BUSINESSPARTNER));
        }

        // Only empty Business Partners
      } else { // if if (strcNoBusinessPartner.equals("only"))
        obCriteriaTrans.add(Restrictions.isNull(FIN_FinaccTransaction.PROPERTY_BUSINESSPARTNER));
      }

      // Financial Account
      if (StringUtils.isNotEmpty(strFinancialAccountId)) {
        obCriteriaTrans.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_ACCOUNT,
            OBDal.getReadOnlyInstance().get(FIN_FinancialAccount.class, strFinancialAccountId)));
      }

      // Document Date, Payment Date & Due Date
      if (StringUtils.isNotEmpty(strDocumentDateFrom)) {
        obCriteriaTrans.add(Restrictions.ge(FIN_FinaccTransaction.PROPERTY_DATEACCT,
            FIN_Utility.getDate(strDocumentDateFrom)));
      }
      if (StringUtils.isNotEmpty(strDocumentDateTo)) {
        obCriteriaTrans.add(Restrictions.le(FIN_FinaccTransaction.PROPERTY_DATEACCT,
            FIN_Utility.getDate(strDocumentDateTo)));
      }
      if (StringUtils.isNotEmpty(strPaymentDateFrom)) {
        obCriteriaTrans.add(Restrictions.ge(FIN_FinaccTransaction.PROPERTY_DATEACCT,
            FIN_Utility.getDate(strPaymentDateFrom)));
      }
      if (StringUtils.isNotEmpty(strPaymentDateTo)) {
        obCriteriaTrans.add(Restrictions.le(FIN_FinaccTransaction.PROPERTY_DATEACCT,
            FIN_Utility.getDate(strPaymentDateTo)));
      }
      if (StringUtils.isNotEmpty(strDueDateFrom)) {
        obCriteriaTrans.add(Restrictions.ge(FIN_FinaccTransaction.PROPERTY_DATEACCT,
            FIN_Utility.getDate(strDueDateFrom)));
      }
      if (StringUtils.isNotEmpty(strExpectedDateTo)) {
        obCriteriaTrans.add(Restrictions.le(FIN_FinaccTransaction.PROPERTY_DATEACCT,
            FIN_Utility.getDate(strExpectedDateFrom)));
      }

      // Amount
      if (StringUtils.isNotEmpty(strAmountFrom)) {
        obCriteriaTrans.add(Restrictions.or(
            Restrictions.ge(FIN_FinaccTransaction.PROPERTY_DEPOSITAMOUNT,
                new BigDecimal(strAmountFrom)),
            Restrictions.ge(FIN_FinaccTransaction.PROPERTY_PAYMENTAMOUNT,
                new BigDecimal(strAmountFrom))));
      }
      if (StringUtils.isNotEmpty(strAmountTo)) {
        obCriteriaTrans.add(Restrictions.or(
            Restrictions.le(FIN_FinaccTransaction.PROPERTY_DEPOSITAMOUNT,
                new BigDecimal(strAmountTo)),
            Restrictions.le(FIN_FinaccTransaction.PROPERTY_PAYMENTAMOUNT,
                new BigDecimal(strAmountTo))));
      }

      // Projects
      if (!projects.isEmpty()) {
        obCriteriaTrans.add(Restrictions.in(FIN_FinaccTransaction.PROPERTY_PROJECT, projects));
      }

      // Status
      if (StringUtils.isNotEmpty(localStrfinPaymSt)
          && !StringUtils.equalsIgnoreCase(localStrfinPaymSt, "('')")) {
        localStrfinPaymSt = localStrfinPaymSt.replace("(", "");
        localStrfinPaymSt = localStrfinPaymSt.replace(")", "");
        localStrfinPaymSt = localStrfinPaymSt.replace("'", "");
        localStrfinPaymSt = localStrfinPaymSt.replace(" ", "");
        Object[] status = localStrfinPaymSt.split(",");
        obCriteriaTrans.add(Restrictions.in(FIN_FinaccTransaction.PROPERTY_STATUS, status));
      }

      // Currency
      if (StringUtils.isNotEmpty(strcCurrency)) {
        obCriteriaTrans.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_CURRENCY,
            OBDal.getReadOnlyInstance().get(Currency.class, strcCurrency)));
      }

      // payment type
      if (StringUtils.equalsIgnoreCase(strPaymType, "FINPR_Receivables")) {
        Object[] status = { "PWNC", "RPPC" };
        obCriteriaTrans.add(Restrictions.in(FIN_FinaccTransaction.PROPERTY_STATUS, status));
      } else if (StringUtils.equalsIgnoreCase(strPaymType, "FINPR_Payables")) {
        Object[] status = { "RDNC", "RPPC" };
        obCriteriaTrans.add(Restrictions.in(FIN_FinaccTransaction.PROPERTY_STATUS, status));
      }

      // order

      if (StringUtils.equalsIgnoreCase(strGroupCrit, "APRM_FATS_BPARTNER")) {
        obCriteriaTrans.addOrder(Order.asc("bp." + BusinessPartner.PROPERTY_NAME));
      } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "Project")) {
        obCriteriaTrans.addOrder(Order.asc("p." + Project.PROPERTY_NAME));
      } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "FINPR_BPartner_Category")) {
        obCriteriaTrans.addOrder(Order.asc("bpc." + Category.PROPERTY_NAME));
      } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "INS_CURRENCY")) {
        obCriteriaTrans.addOrder(Order.asc("c." + Currency.PROPERTY_ISOCODE));
      } else if (StringUtils.equalsIgnoreCase(strGroupCrit, "ACCS_ACCOUNT_ID_D")) {
        obCriteriaTrans.addOrder(Order.asc("acc." + FIN_FinancialAccount.PROPERTY_NAME));
      }

      obCriteriaTrans.addOrder(Order.asc(FIN_FinaccTransaction.PROPERTY_STATUS));

      if (StringUtils.isNotEmpty(strOrdCrit)) {
        String[] strOrdCritList = strOrdCrit.substring(2, strOrdCrit.length() - 2).split("', '");
        for (int i = 0; i < strOrdCritList.length; i++) {
          if (StringUtils.equalsIgnoreCase(strOrdCritList[i], "Date")) {
            obCriteriaTrans.addOrder(Order.asc(FIN_FinaccTransaction.PROPERTY_DATEACCT));
          }
          if (strOrdCritList[i].contains("Project")) {
            obCriteriaTrans.addOrder(Order.asc("p." + Project.PROPERTY_NAME));
          }
          if (strOrdCritList[i].contains("FINPR_BPartner_Category")) {
            obCriteriaTrans.addOrder(Order.asc("bpc." + Category.PROPERTY_NAME));
          }
          if (strOrdCritList[i].contains("APRM_FATS_BPARTNER")) {
            obCriteriaTrans.addOrder(Order.asc("bp." + BusinessPartner.PROPERTY_NAME));
          }
          if (strOrdCritList[i].contains("INS_CURRENCY")) {
            obCriteriaTrans.addOrder(Order.asc("c." + Currency.PROPERTY_ISOCODE));
          }
          if (strOrdCritList[i].contains("ACCS_ACCOUNT_ID_D")) {
            obCriteriaTrans.addOrder(Order.asc("acc." + FIN_FinancialAccount.PROPERTY_NAME));
          }
          if (StringUtils.equalsIgnoreCase(strOrdCritList[i], "DueDate")) {
            obCriteriaTrans.addOrder(Order.asc(FIN_FinaccTransaction.PROPERTY_TRANSACTIONDATE));
          }
        }
      }
      obCriteriaTrans.addOrderBy(FIN_FinaccTransaction.PROPERTY_ID, true);

      final java.util.List<FIN_FinaccTransaction> transList = obCriteriaTrans.list();
      return transList;

    } catch (Exception e) {
      log4j.error(e.getMessage(), e);
      return new ArrayList<FIN_FinaccTransaction>();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void fillLine(SimpleDateFormat dateFormat, FieldProvider data,
      FIN_PaymentScheduleDetail fIN_PaymentScheduleDetail, FIN_PaymentSchedule paymentSchedule,
      boolean creditPaysInvoice) {
    Date invoicedDate;
    long plannedDSO = 0;
    long currentDSO = 0;
    long currentTime = 0;
    // project
    if (paymentSchedule.getInvoice().getProject() != null) {
      FieldProviderFactory.setField(data, "PROJECT",
          paymentSchedule.getInvoice().getProject().getIdentifier());
    } else {
      FieldProviderFactory.setField(data, "PROJECT", "");
    }
    // salesPerson
    if (paymentSchedule.getInvoice().getSalesRepresentative() != null) {
      FieldProviderFactory.setField(data, "SALES_PERSON",
          paymentSchedule.getInvoice().getSalesRepresentative().getIdentifier());
    } else {
      FieldProviderFactory.setField(data, "SALES_PERSON", "");
    }
    // invoiceNumber
    FieldProviderFactory.setField(data, "INVOICE_NUMBER",
        (creditPaysInvoice ? "*" : "") + paymentSchedule.getInvoice().getDocumentNo());
    // payment plan id
    FieldProviderFactory.setField(data, "PAYMENT_PLAN_ID", paymentSchedule.getId());
    // payment plan yes / no
    FieldProviderFactory.setField(data, "PAYMENT_PLAN_Y_N", "");
    // payment plan yes / no
    FieldProviderFactory.setField(data, "NOT_PAYMENT_PLAN_Y_N", "Display:none");
    // invoiceDate
    invoicedDate = paymentSchedule.getInvoice().getInvoiceDate();
    FieldProviderFactory.setField(data, "INVOICE_DATE", dateFormat.format(invoicedDate));
    // dueDate
    FieldProviderFactory.setField(data, "DUE_DATE",
        dateFormat.format(paymentSchedule.getDueDate()).toString());
    // expectedDate
    FieldProviderFactory.setField(data, "EXPECTED_DATE",
        dateFormat.format(paymentSchedule.getExpectedDate()).toString());
    // plannedDSO
    plannedDSO = (paymentSchedule.getDueDate().getTime() - invoicedDate.getTime()) / milisecDayConv;
    FieldProviderFactory.setField(data, "PLANNED_DSO", String.valueOf(plannedDSO));
    // currentDSO
    if (fIN_PaymentScheduleDetail.getPaymentDetails() != null) {
      currentDSO = (((fIN_PaymentScheduleDetail.getPaymentDetails()
          .getFinPayment()
          .getPaymentDate() != null)
              ? fIN_PaymentScheduleDetail.getPaymentDetails()
                  .getFinPayment()
                  .getPaymentDate()
                  .getTime()
              : 0)
          - invoicedDate.getTime()) / milisecDayConv;
    } else {
      currentTime = System.currentTimeMillis();
      currentDSO = (currentTime - invoicedDate.getTime()) / milisecDayConv;
    }
    FieldProviderFactory.setField(data, "CURRENT_DSO", String.valueOf((currentDSO)));
    // daysOverdue
    FieldProviderFactory.setField(data, "OVERDUE", String.valueOf((currentDSO - plannedDSO)));
  }

  public ConversionRate getConversionRate(Currency transCurrency, Currency baseCurrency,
      String conversionDate) {
    OBContext.setAdminMode(true);
    try {
      Date conversionDateObj = FIN_Utility.getDate(conversionDate);
      final OBCriteria<ConversionRate> obcConvRate = OBDal.getReadOnlyInstance()
          .createCriteria(ConversionRate.class);
      obcConvRate.add(Restrictions.eq(ConversionRate.PROPERTY_CURRENCY, transCurrency));
      obcConvRate.add(Restrictions.eq(ConversionRate.PROPERTY_TOCURRENCY, baseCurrency));
      obcConvRate.add(Restrictions.le(ConversionRate.PROPERTY_VALIDFROMDATE, conversionDateObj));
      obcConvRate.add(Restrictions.ge(ConversionRate.PROPERTY_VALIDTODATE, conversionDateObj));
      obcConvRate.setMaxResults(1);
      ConversionRate convRate = (ConversionRate) obcConvRate.uniqueResult();
      return convRate;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public String[] getReferenceListValues(String refName, boolean inclEmtyValue) {
    OBContext.setAdminMode(true);
    String values[];
    try {
      final OBCriteria<Reference> obc = OBDal.getReadOnlyInstance().createCriteria(Reference.class);
      obc.add(Restrictions.eq(Reference.PROPERTY_NAME, refName));
      final OBCriteria<List> obcValue = OBDal.getReadOnlyInstance().createCriteria(List.class);
      obcValue.add(Restrictions.eq(List.PROPERTY_REFERENCE, obc.list().get(0)));
      java.util.List<List> v = obcValue.list();
      int n = v.size();

      if (inclEmtyValue) {
        values = new String[n + 1];
      } else {
        values = new String[n];
      }

      for (int i = 0; i < n; i++) {
        values[i] = v.get(i).getSearchKey();
      }

      if (inclEmtyValue) {
        values[values.length - 1] = new String("");
      }

    } finally {
      OBContext.restorePreviousMode();
    }

    return values;
  }

  public static String translateRefList(String strCode) {
    String strMessage = "";
    OBContext.setAdminMode(true);
    try {
      Language language = OBContext.getOBContext().getLanguage();

      if (!StringUtils.equals(language.getLanguage(), "en_US")) {
        OBCriteria<ListTrl> obcTrl = OBDal.getReadOnlyInstance().createCriteria(ListTrl.class);
        obcTrl.add(Restrictions.eq(ListTrl.PROPERTY_LANGUAGE, language));
        obcTrl.createAlias(ListTrl.PROPERTY_LISTREFERENCE, "lr");
        obcTrl.add(Restrictions.eq("lr." + List.PROPERTY_SEARCHKEY, strCode));
        obcTrl.setFilterOnReadableClients(false);
        obcTrl.setFilterOnReadableOrganization(false);
        obcTrl.setMaxResults(1);
        ListTrl listTrl = (ListTrl) obcTrl.uniqueResult();
        strMessage = listTrl != null ? listTrl.getName() : null;
      }
      if (StringUtils.equals(language.getLanguage(), "en_US") || strMessage == null) {
        OBCriteria<List> obc = OBDal.getReadOnlyInstance().createCriteria(List.class);
        obc.setFilterOnReadableClients(false);
        obc.setFilterOnReadableOrganization(false);
        obc.add(Restrictions.eq(List.PROPERTY_SEARCHKEY, strCode));
        obc.setMaxResults(1);
        List list = (List) obc.uniqueResult();
        strMessage = list != null ? list.getName() : null;
      }

      if (StringUtils.isEmpty(strMessage)) {
        strMessage = strCode;
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return Replace.replace(Replace.replace(strMessage, "\n", "\\n"), "\"", "&quot;");
  }

  public static HashMap<String, String> getLinkParameters(String adTableId, String isReceipt) {
    HashMap<String, String> hmValues = new HashMap<String, String>();

    OBContext.setAdminMode(true);
    try {
      Table adTable = OBDal.getReadOnlyInstance().get(Table.class, adTableId);

      Window adWindow = null;
      if (StringUtils.equalsIgnoreCase(isReceipt, "Y")) {
        adWindow = adTable.getWindow();
      } else {
        adWindow = adTable.getPOWindow();
      }
      hmValues.put("adWindowName", adWindow.getName());

      java.util.List<Tab> adTabList = adWindow.getADTabList();
      for (int i = 0; i < adTabList.size(); i++) {
        if (StringUtils.equalsIgnoreCase(adTabList.get(i).getTable().getId(), adTableId)) {
          hmValues.put("adTabName", adTabList.get(i).getName());
          hmValues.put("adTabId", adTabList.get(i).getId());
        }
      }

      java.util.List<Column> adColumnList = adTable.getADColumnList();
      for (int i = 0; i < adColumnList.size(); i++) {
        if (adColumnList.get(i).isKeyColumn()) {
          hmValues.put("adColumnName", adColumnList.get(i).getDBColumnName());
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return hmValues;
  }

  public static FieldProvider[] getObjectList(String objectNames) {
    if (StringUtils.isEmpty(objectNames)) {
      return new FieldProvider[0];
    } else {
      String[] names = objectNames.substring(1, objectNames.length() - 1).split(", ");
      SQLReturnObject sqlRO;
      String name = null;

      Vector<FieldProvider> vector = new Vector<>(0);
      for (int i = 0; i < names.length; i++) {
        sqlRO = new SQLReturnObject();
        names[i] = names[i];
        name = names[i].substring(1, names[i].length() - 1);
        sqlRO.setData("ID", name);
        sqlRO.setData("NAME", FIN_Utility.messageBD(name));
        sqlRO.setData("DESCRIPTION", "");
        vector.addElement(sqlRO);
      }

      FieldProvider objectListData[] = new FieldProvider[vector.size()];
      vector.copyInto(objectListData);

      return objectListData;
    }
  }

  public java.util.List<Invoice> getInvoicesUsingCredit(final FIN_Payment payment) {
    final java.util.List<Invoice> result = new ArrayList<>();

    // @formatter:off
    final String sql = " select distinct(psiv.invoice.id) "
                     + " from FIN_Payment_Sched_Inv_V psiv "
                     + " where exists ("
                     + "   select 1 "
                     + "   from FIN_Payment_Credit pc,"
                     + "    FIN_Payment_Detail pd,"
                     + "    FIN_Payment_ScheduleDetail psd"
                     + "   where pc.payment.id = pd.finPayment.id"
                     + "     and pd.id = psd.paymentDetails.id"
                     + "     and pc.creditPaymentUsed.id = :paymentId"
                     + "     and psd.invoicePaymentSchedule.id = psiv.id"
                     + "   )";
    // @formatter:on

    try {
      OBContext.setAdminMode(true);
      final Session session = OBDal.getReadOnlyInstance().getSession();
      final Query<String> query = session.createQuery(sql, String.class);
      query.setParameter("paymentId", payment.getId());
      for (final String o : query.list()) {
        result.add(OBDal.getReadOnlyInstance().get(Invoice.class, o));
      }

      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public java.util.List<FIN_PaymentSchedule> getInvoicePaymentSchedules(
      FIN_Payment credit_payment) {
    // @formatter:off
    final String sql = " select ps "
                     + " from FIN_Payment_Credit pc,"
                     + "  FIN_Payment_Detail_V pdv,"
                     + "  FIN_Payment_Schedule ps "
                     + " where pc.payment = pdv.payment "
                     + "  and ps.id = pdv.paymentPlanInvoice "
                     + "  and pc.creditPaymentUsed.id = :creditPaymentId";
    // @formatter:on
    try {
      OBContext.setAdminMode(true);
      final Session session = OBDal.getReadOnlyInstance().getSession();
      final Query<FIN_PaymentSchedule> query = session.createQuery(sql, FIN_PaymentSchedule.class);
      query.setParameter("creditPaymentId", credit_payment.getId());
      return query.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public String getInvoicesDocNos(Collection<Invoice> invoices) {
    final StringBuilder sb = new StringBuilder();
    for (Invoice i : invoices) {
      sb.append(i.getDocumentNo());
      sb.append(", ");
    }
    return sb.delete(sb.length() - 2, sb.length()).toString();
  }

  private BusinessPartner getDocumentBusinessPartner(FIN_PaymentScheduleDetail psd) {
    BusinessPartner bp = null;
    if (psd.getInvoicePaymentSchedule() != null) { // Invoice
      bp = psd.getInvoicePaymentSchedule().getInvoice().getBusinessPartner();
    }
    if (psd.getOrderPaymentSchedule() != null) { // Order
      bp = psd.getOrderPaymentSchedule().getOrder().getBusinessPartner();
    }
    if (bp == null) {
      bp = psd.getPaymentDetails().getFinPayment().getBusinessPartner();
    }
    return bp;
  }

  /**
   * Given a String of organizations this method returns an array of organizations
   */
  private Organization[] getOrganizations(Set<String> strOrgFamily) {
    Iterator<String> orgChildTreeIter = strOrgFamily.iterator();
    Organization[] organizations = new Organization[strOrgFamily.size()];
    int i = 0;
    while (orgChildTreeIter.hasNext()) {
      organizations[i] = OBDal.getReadOnlyInstance()
          .get(Organization.class, orgChildTreeIter.next());
      i++;
    }
    return organizations;
  }

  @SuppressWarnings("unchecked")
  private void createBPList() {
    // @formatter:off
    final String hql = " select bp.name "
                     + " from BusinessPartner as bp"
                     + " order by bp.name";
    // @formatter:on
    bpList = OBDal.getReadOnlyInstance().getSession().createQuery(hql).list();
  }

  @SuppressWarnings("unchecked")
  private void createBPCategoryList() {
    // @formatter:off
    final String hql = " select c.name "
                     + " from BusinessPartnerCategory as c"
                     + " order by c.name";
    // @formatter:on
    bpCategoryList = OBDal.getReadOnlyInstance().getSession().createQuery(hql).list();
  }

  @SuppressWarnings("unchecked")
  private void createProjectList() {
    // @formatter:off
    final String hql = " select p.name "
                     + " from Project as p"
                     + " order by p.name";
    // @formatter:on
    projectList = OBDal.getReadOnlyInstance().getSession().createQuery(hql).list();
  }

  @SuppressWarnings("unchecked")
  private void createAcctList() {
    // @formatter:off
    final String hql = " select fa.name"
                     + " from FIN_Financial_Account as fa"
                     + " order by fa.name";
    // @formatter:on
    acctList = OBDal.getReadOnlyInstance().getSession().createQuery(hql).list();
  }
}
