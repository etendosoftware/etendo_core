/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.utility;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Session;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.hibernate.query.Query;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentProposal;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DbUtility;
import org.openbravo.utils.Replace;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;


public class FIN_Utility {

  private static final Logger log4j = LogManager.getLogger();
  private static AdvPaymentMngtDao dao;

  @Inject
  @Any
  Instance<FIN_SequenceActionInterface> sequenceAction;

  public static FIN_Utility getInstance() {
    return WeldUtils.getInstanceFromStaticBeanManager(FIN_Utility.class);
  }

  /**
   * @see OBDateUtils#getDate(String)
   */
  public static Date getDate(String strDate) {
    try {
      return OBDateUtils.getDate(strDate);
    } catch (ParseException e) {
      log4j.error("Error parsing date", e);
      return null;
    }

  }

  /**
   * @see OBDateUtils#getDateTime(String)
   */
  public static Date getDateTime(String strDate) {
    try {
      return OBDateUtils.getDateTime(strDate);
    } catch (ParseException e) {
      log4j.error("Error parsing date", e);
      return null;
    }
  }

  /**
   * Parses the string of comma separated id's to return a List object of the given class
   * 
   * @param <T>
   * @param t
   *          class of the OBObject the id's belong to
   * @param _strSelectedIds
   *          String containing a comma separated list of id's
   * @return a List object containing the parsed OBObjects
   */
  public static <T extends BaseOBObject> List<T> getOBObjectList(Class<T> t,
      String _strSelectedIds) {
    dao = new AdvPaymentMngtDao();
    String strSelectedIds = _strSelectedIds;
    final List<T> OBObjectList = new ArrayList<T>();
    // selected scheduled payments list
    if (strSelectedIds.startsWith("(")) {
      strSelectedIds = strSelectedIds.substring(1, strSelectedIds.length() - 1);
    }
    if (!strSelectedIds.equals("")) {
      strSelectedIds = Replace.replace(strSelectedIds, "'", "");
      StringTokenizer st = new StringTokenizer(strSelectedIds, ",", false);
      while (st.hasMoreTokens()) {
        String strScheduledPaymentId = st.nextToken().trim();
        OBObjectList.add(dao.getObject(t, strScheduledPaymentId));
      }
    }
    return OBObjectList;
  }

  /**
   * 
   * @param _strSelectedIds
   *          Identifiers string list with the following structure: ('ID', 'ID', 'ID')
   * @return Map&lt;K,V&gt; using the ID as key and value &lt;ID,ID&gt; for each identifier.
   */
  public static Map<String, String> getMapFromStringList(String _strSelectedIds) {
    String strSelectedIds = _strSelectedIds;
    final Map<String, String> map = new HashMap<String, String>();
    if (strSelectedIds.startsWith("(")) {
      strSelectedIds = strSelectedIds.substring(1, strSelectedIds.length() - 1);
    }
    if (!strSelectedIds.equals("")) {
      strSelectedIds = Replace.replace(strSelectedIds, "'", "");
      StringTokenizer st = new StringTokenizer(strSelectedIds, ",", false);
      while (st.hasMoreTokens()) {
        String strItem = st.nextToken().trim();
        map.put(strItem, strItem);
      }
    }
    return map;
  }

  /**
   * Returns a FieldProvider object containing the Scheduled Payments.
   * 
   * @param vars
   * @param selectedScheduledPayments
   *          List of FIN_PaymentSchedule that need to be selected by default
   * @param filteredScheduledPayments
   *          List of FIN_PaymentSchedule that need to unselected by default
   */
  public static FieldProvider[] getShownScheduledPayments(VariablesSecureApp vars,
      List<FIN_PaymentSchedule> selectedScheduledPayments,
      List<FIN_PaymentSchedule> filteredScheduledPayments) {
    final List<FIN_PaymentSchedule> shownScheduledPayments = new ArrayList<FIN_PaymentSchedule>();
    shownScheduledPayments.addAll(selectedScheduledPayments);
    shownScheduledPayments.addAll(filteredScheduledPayments);
    FIN_PaymentSchedule[] FIN_PaymentSchedules = new FIN_PaymentSchedule[0];
    FIN_PaymentSchedules = shownScheduledPayments.toArray(FIN_PaymentSchedules);
    // FieldProvider[] data =
    // FieldProviderFactory.getFieldProviderArray(FIN_PaymentSchedules);

    // FieldProvider[] data = new
    // FieldProviderFactory[selectedScheduledPayments.size()];
    FieldProvider[] data = FieldProviderFactory.getFieldProviderArray(shownScheduledPayments);
    String dateFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);
    // set in administrator mode to be able to access FIN_PaymentSchedule entity
    OBContext.setAdminMode();
    try {

      for (int i = 0; i < data.length; i++) {
        FieldProviderFactory.setField(data[i], "finSelectedPaymentId",
            (selectedScheduledPayments.contains(FIN_PaymentSchedules[i]))
                ? FIN_PaymentSchedules[i].getId()
                : "");
        FieldProviderFactory.setField(data[i], "finScheduledPaymentId",
            FIN_PaymentSchedules[i].getId());
        if (FIN_PaymentSchedules[i].getOrder() != null) {
          FieldProviderFactory.setField(data[i], "orderNr",
              FIN_PaymentSchedules[i].getOrder().getDocumentNo());
        }
        if (FIN_PaymentSchedules[i].getInvoice() != null) {
          FieldProviderFactory.setField(data[i], "invoiceNr",
              FIN_PaymentSchedules[i].getInvoice().getDocumentNo());
          FieldProviderFactory.setField(data[i], "invoicedAmount",
              FIN_PaymentSchedules[i].getInvoice().getGrandTotalAmount().toString());
        }
        FieldProviderFactory.setField(data[i], "dueDate",
            dateFormater.format(FIN_PaymentSchedules[i].getDueDate()).toString());
        FieldProviderFactory.setField(data[i], "expectedAmount",
            FIN_PaymentSchedules[i].getAmount().toString());
        String strPaymentAmt = vars
            .getStringParameter("inpPaymentAmount" + FIN_PaymentSchedules[i].getId(), "");
        FieldProviderFactory.setField(data[i], "paymentAmount", strPaymentAmt);
        FieldProviderFactory.setField(data[i], "rownum", String.valueOf(i));

      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return data;
  }

  /**
   * Returns the cause of a trigger exception (BatchupdateException).
   * 
   * Hibernate and JDBC will wrap the exception thrown by the trigger in another exception (the
   * java.sql.BatchUpdateException) and this exception is sometimes wrapped again. Also the
   * java.sql.BatchUpdateException stores the underlying trigger exception in the nextException and
   * not in the cause property.
   * 
   * @param t
   *          exception.
   * @return the underlying trigger message.
   */
  public static String getExceptionMessage(Throwable t) {
    Throwable throwable = DbUtility.getUnderlyingSQLException(t);
    return throwable.getMessage();
  }

  /**
   * Returns the DocumentType defined for the Organization (or parent organization tree) and
   * document category.
   * 
   * @param org
   *          the Organization for which the Document Type is defined. The Document Type can belong
   *          to the parent organization tree of the specified Organization.
   * @param docCategory
   *          the document category of the Document Type.
   * @return the Document Type
   */
  public static DocumentType getDocumentType(Organization org, String docCategory) {
    Client client = null;
    if ("0".equals(org.getId())) {
      client = OBContext.getOBContext().getCurrentClient();
      if ("0".equals(client.getId())) {
        return null;
      }
    } else {
      client = org.getClient();
    }

    OBContext.setAdminMode(false);
    try {
      // @formatter:off
      final String where = ""
          + " as dt"
          + " where dt.organization.id in (:orgIdList)"
          + "   and dt.client.id = :clientId"
          + "   and dt.documentCategory = :docCategory"
          + " order by ad_isorgincluded(:orgId, dt.organization.id, :clientId)"
          + "   , dt.default desc"
          + "   , dt.id desc";

      // @formatter:on
      OBQuery<DocumentType> dt = OBDal.getInstance().createQuery(DocumentType.class, where);
      dt.setFilterOnReadableClients(false);
      dt.setFilterOnReadableOrganization(false);
      dt.setMaxResult(1);
      dt.setNamedParameter("orgIdList",
          new OrganizationStructureProvider().getParentTree(org.getId(), true));
      dt.setNamedParameter("clientId", client.getId());
      dt.setNamedParameter("docCategory", docCategory);
      dt.setNamedParameter("orgId", org.getId());

      return dt.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns the next sequence number of the Document Type defined for the Organization and document
   * category. The current number of the sequence is also updated.
   *
   * @param docType
   *          Document type of the document
   * @param tableName
   *          the name of the table from which the sequence will be taken if the Document Type does
   *          not have any sequence associated.
   * @return the next sequence number of the Document Type defined for the Organization and document
   *         category. Null if no sequence is found.
   */
  public static String getDocumentNo(DocumentType docType, String tableName) {
    return getDocumentNo(docType, tableName, true);
  }

  /**
   * Returns the next sequence number of the Document Type defined for the Organization and document
   * category.
   *
   * @param docType
   *          Document type of the document
   * @param tableName
   *          the name of the table from which the sequence will be taken if the Document Type does
   *          not have any sequence associated.
   * @param updateNext
   *          Flag to update the current number of the sequence
   * @return the next sequence number of the Document Type defined for the Organization and document
   *         category. Null if no sequence is found.
   */
  public static String getDocumentNo(DocumentType docType, String tableName, boolean updateNext) {
    if (getInstance().sequenceAction.get() != null) {
      return getInstance().sequenceAction.get().getDocumentNo(docType, tableName, updateNext);
    }
    return null;
  }

  public static String getDocumentNo(boolean updateNext, Sequence seqParam) {
    if(getInstance().sequenceAction.get() != null) {
      return getInstance().sequenceAction.get().getDocumentNo(updateNext, seqParam);
    }
    return null;
  }

  private static Sequence getSequenceAndLockIfUpdateNext(final boolean updateNext,
                                                         final Sequence seqParam) {
    if(getInstance().sequenceAction.get() != null) {
      return getInstance().sequenceAction.get().getSequenceAndLockIfUpdateNext(updateNext, seqParam);
    }
    return null;
  }

  private static Sequence lockSequence(String sequenceId) {
    if(getInstance().sequenceAction.get() != null) {
      return getInstance().sequenceAction.get().lockSequence(sequenceId);
    }
    return null;
  }

  private static String getNextDocNumberAndIncrementSeqIfUpdateNext(final boolean updateNext,
                                                                    final Sequence seq) {
    if(getInstance().sequenceAction.get() != null) {
      return getInstance().sequenceAction.get().getNextDocNumberAndIncrementSeqIfUpdateNext(updateNext, seq);
    }
    return null;
  }

  private static void incrementSeqIfUpdateNext(final boolean updateNext, final Sequence seq) {
    if(getInstance().sequenceAction.get() != null) {
      getInstance().sequenceAction.get().incrementSeqIfUpdateNext(updateNext, seq);
    }
  }

  /**
   * Returns the next sequence number of the Document Type defined for the Organization and document
   * category. The current number of the sequence is also updated.
   *
   * @param org
   *          the Organization for which the Document Type is defined. The Document Type can belong
   *          to the parent organization tree of the specified Organization.
   * @param docCategory
   *          the document category of the Document Type.
   * @param tableName
   *          the name of the table from which the sequence will be taken if the Document Type does
   *          not have any sequence associated.
   * @return the next sequence number of the Document Type defined for the Organization and document
   *         category. Null if no sequence is found.
   */
  public static String getDocumentNo(Organization org, String docCategory, String tableName) {
    if(getInstance().sequenceAction.get() != null) {
      return getInstance().sequenceAction.get().getDocumentNo(org, docCategory, tableName);
    }
    return null;
  }

  /**
   * Returns the next sequence number of the Document Type defined for the Organization and document
   * category.
   *
   * @param org
   *          the Organization for which the Document Type is defined. The Document Type can belong
   *          to the parent organization tree of the specified Organization.
   * @param docCategory
   *          the document category of the Document Type.
   * @param tableName
   *          the name of the table from which the sequence will be taken if the Document Type does
   *          not have any sequence associated.
   * @return the next sequence number of the Document Type defined for the Organization and document
   *         category. Null if no sequence is found.
   */
  public static String getDocumentNo(Organization org, String docCategory, String tableName,
                                     boolean updateNext) {
    if(getInstance().sequenceAction.get() != null) {
      return getInstance().sequenceAction.get().getDocumentNo(org, docCategory, tableName, updateNext);
    }
    return null;
  }

  /**
   * Gets the available Payment Methods and returns in a String the html code containing all the
   * Payment Methods in the natural tree of the given organization filtered by the Financial
   * Account.
   * 
   * @param strPaymentMethodId
   *          the Payment Method id that will be selected by default in case it is present in the
   *          list.
   * @param strFinancialAccountId
   *          optional Financial Account id to filter the Payment Methods.
   * @param strOrgId
   *          the Organization id the record belongs to.
   * @param isMandatory
   *          boolean parameter to add an extra blank option if the drop-down is optional.
   * @param excludePaymentMethodWithoutAccount
   *          if the strPaymentMethodId is empty or null then depending on this parameter the list
   *          will include payment methods with no Financial Accounts associated or only show the
   *          Payment Methods that belongs to at least on Financial Account
   * @param isInPayment
   *          specifies the type of payment to get payment methods for. If true, will return payment
   *          methods with Payment In enabled, if false will return payment methods with Payment Out
   *          enabled.
   * @return a String with the html code with the options to fill the drop-down of Payment Methods.
   */
  public static String getPaymentMethodList(String strPaymentMethodId, String strFinancialAccountId,
      String strOrgId, boolean isMandatory, boolean excludePaymentMethodWithoutAccount,
      boolean isInPayment) {
    dao = new AdvPaymentMngtDao();
    String selectedPaymentMethodId = strPaymentMethodId;
    List<FIN_PaymentMethod> paymentMethods = dao.getFilteredPaymentMethods(strFinancialAccountId,
        strOrgId, excludePaymentMethodWithoutAccount,
        isInPayment ? AdvPaymentMngtDao.PaymentDirection.IN
            : AdvPaymentMngtDao.PaymentDirection.OUT);
    if ("".equals(selectedPaymentMethodId) && !"".equals(strFinancialAccountId)) {
      selectedPaymentMethodId = dao.getDefaultPaymentMethodId(
          OBDal.getInstance().get(FIN_FinancialAccount.class, strFinancialAccountId), isInPayment);
    }
    String options = getOptionsList(paymentMethods, selectedPaymentMethodId, isMandatory);
    return options;
  }

  /**
   * Gets the available Financial Accounts and returns in a String the html code containing all the
   * Financial Accounts in the natural tree of the given organization filtered by the Payment
   * Method.
   * 
   * @param strPaymentMethodId
   *          optional Payment Method id to filter the Financial Accounts.
   * @param strFinancialAccountId
   *          the Financial Account id that will be selected by default in case it is present in the
   *          list.
   * @param strOrgId
   *          the Organization id the record belongs to.
   * @param strCurrencyId
   *          optional Currency id to filter the Financial Accounts.
   * @param isInPayment
   *          specifies the type of payment to that is being made. If true, will return accounts
   *          with payment methods that have Payment In enabled, if false will return accounts with
   *          payment methods that have Payment Out enabled.
   * @return a String with the html code with the options to fill the drop-down of Financial
   *         Accounts.
   */
  public static String getFinancialAccountList(String strPaymentMethodId,
      String strFinancialAccountId, String strOrgId, boolean isMandatory, String strCurrencyId,
      boolean isInPayment) {

    try {
      OBContext.setAdminMode(true);
      List<FIN_FinancialAccount> financialAccounts = dao.getFilteredFinancialAccounts(
          strPaymentMethodId, strOrgId, strCurrencyId,
          isInPayment ? AdvPaymentMngtDao.PaymentDirection.IN
              : AdvPaymentMngtDao.PaymentDirection.OUT);
      String options = getOptionsList(financialAccounts, strFinancialAccountId, isMandatory);
      return options;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns a String containing the html code with the options based on the given List of
   * BaseOBObjects
   * 
   * @param <T>
   *          Class that extends BaseOBObject.
   * @param obObjectList
   *          List containing the values to be included in the options.
   * @param selectedValue
   *          value to set as selected by default.
   * @param isMandatory
   *          boolean to add a blank option in the options list.
   * @return a String containing the html code with the options. *
   */
  public static <T extends BaseOBObject> String getOptionsList(List<T> obObjectList,
      String selectedValue, boolean isMandatory) {
    return getOptionsList(obObjectList, selectedValue, isMandatory, false);
  }

  /**
   * Returns a String containing the html code with the options based on the given List of
   * BaseOBObjects
   * 
   * @param <T>
   *          Class that extends BaseOBObject.
   * @param obObjectList
   *          List containing the values to be included in the options.
   * @param selectedValue
   *          value to set as selected by default.
   * @param isMandatory
   *          boolean to add a blank option in the options list.
   * @param isRefList
   *          boolean to let know if the options belong to a refList. In that case, the value must
   *          be the search key of the list item instead of it's id.
   * @return a String containing the html code with the options. *
   */
  public static <T extends BaseOBObject> String getOptionsList(List<T> obObjectList,
      String selectedValue, boolean isMandatory, boolean isRefList) {
    StringBuilder strOptions = new StringBuilder();
    if (!isMandatory) {
      strOptions.append("<option value=\"\"></option>");
    }

    for (T obObject : obObjectList) {
      strOptions.append("<option value=\"")
          .append((isRefList) ? obObject.getValue("searchKey") : obObject.getId())
          .append("\"");
      if (obObject.getId().equals(selectedValue)) {
        strOptions.append(" selected=\"selected\"");
      }
      strOptions.append(">");
      strOptions.append(escape(obObject.getIdentifier()));
      strOptions.append("</option>");
    }
    return strOptions.toString();
  }

  public static <T extends BaseOBObject> String getOptionsListFromFieldProvider(
      FieldProvider[] fieldProvider, String selectedValue, boolean isMandatory) {
    StringBuilder strOptions = new StringBuilder();
    if (!isMandatory) {
      strOptions.append("<option value=\"\"></option>");
    }

    for (int i = 0; i < fieldProvider.length; i++) {
      strOptions.append("<option value=\"").append(fieldProvider[i].getField("ID")).append("\"");
      if (fieldProvider[i].getField("ID").equals(selectedValue)) {
        strOptions.append(" selected=\"selected\"");
      }
      strOptions.append(">");
      strOptions.append(escape(fieldProvider[i].getField("NAME")));
      strOptions.append("</option>");
    }
    return strOptions.toString();
  }

  /**
   * Method to replace special characters to print properly in an html. Changes are: ">" to "&gt"
   * and "<" to "&lt"
   * 
   * @param toEscape
   *          String to be replaced.
   * @return the given String with the special characters replaced.
   */
  private static String escape(String toEscape) {
    String result = toEscape.replaceAll(">", "&gt;");
    result = result.replaceAll("<", "&lt;");
    return result;
  }

  /**
   * Method used to calculate the Day still due for the payment.
   * 
   * @param date
   *          . Due date of the payment.
   * @return dayStillDue. Calculated Day Still due.
   */
  public static Long getDaysToDue(Date date) {
    final Date now = DateUtils.truncate(new Date(), Calendar.DATE);
    return getDaysBetween(now, date);
  }

  /**
   * Returns the amount of days between two given dates
   */
  public static Long getDaysBetween(Date beginDate, Date endDate) {
    final TimeZone tz = TimeZone.getDefault();
    final long nowDstOffset = (tz.inDaylightTime(beginDate)) ? tz.getDSTSavings() : 0L;
    final long dateDstOffset = (tz.inDaylightTime(endDate)) ? tz.getDSTSavings() : 0L;
    return (endDate.getTime() + dateDstOffset - beginDate.getTime() - nowDstOffset)
        / DateUtils.MILLIS_PER_DAY;
  }

  public static boolean isAutomaticDepositWithdrawn(FIN_FinancialAccount account,
      FIN_PaymentMethod paymentMethod, boolean isReceipt) {
    FinAccPaymentMethod financialAccountPaymentMethod = new AdvPaymentMngtDao()
        .getFinancialAccountPaymentMethod(account, paymentMethod);
    if (financialAccountPaymentMethod == null) {
      return false;
    }
    return isReceipt ? financialAccountPaymentMethod.isAutomaticDeposit()
        : financialAccountPaymentMethod.isAutomaticWithdrawn();
  }

  public static boolean isAutomaticDepositWithdrawn(FIN_Payment payment) {
    return isAutomaticDepositWithdrawn(payment.getAccount(), payment.getPaymentMethod(),
        payment.isReceipt());
  }

  public static boolean isAutomaticDepositWithdrawn(FIN_PaymentProposal paymentProposal) {
    return isAutomaticDepositWithdrawn(paymentProposal.getAccount(),
        paymentProposal.getPaymentMethod(), paymentProposal.isReceipt());
  }

  /**
   * @see OBMessageUtils#messageBD(String)
   */
  public static String messageBD(String strCode) {
    return OBMessageUtils.messageBD(strCode);
  }

  /**
   * Generic OBCriteria.
   * 
   * @param clazz
   *          Class (entity).
   * @param setFilterClient
   *          If true then only objects from readable clients are returned, if false then objects
   *          from all clients are returned
   * @param setFilterOrg
   *          If true then when querying (for example call list()) a filter on readable
   *          organizations is added to the query, if false then this is not done
   * @param values
   *          Value. Property, value and operator.
   * @return All the records that satisfy the conditions.
   */
  public static <T extends BaseOBObject> List<T> getAllInstances(Class<T> clazz,
      boolean setFilterClient, boolean setFilterOrg, Value... values) {
    OBCriteria<T> obc = OBDal.getInstance().createCriteria(clazz);
    obc.setFilterOnReadableClients(setFilterClient);
    obc.setFilterOnReadableOrganization(setFilterOrg);
    for (Value value : values) {
      if (value.getValue() == null && "==".equals(value.getOperator())) {
        obc.add(Restrictions.isNull(value.getField()));
      } else if (value.getValue() == null && "!=".equals(value.getOperator())) {
        obc.add(Restrictions.isNotNull(value.getField()));
      } else if ("==".equals(value.getOperator())) {
        obc.add(Restrictions.eq(value.getField(), value.getValue()));
      } else if ("!=".equals(value.getOperator())) {
        obc.add(Restrictions.ne(value.getField(), value.getValue()));
      } else if ("<".equals(value.getOperator())) {
        obc.add(Restrictions.lt(value.getField(), value.getValue()));
      } else if (">".equals(value.getOperator())) {
        obc.add(Restrictions.gt(value.getField(), value.getValue()));
      } else if ("<=".equals(value.getOperator())) {
        obc.add(Restrictions.le(value.getField(), value.getValue()));
      } else if (">=".equals(value.getOperator())) {
        obc.add(Restrictions.ge(value.getField(), value.getValue()));
      } else {
        obc.add(Restrictions.eq(value.getField(), value.getValue()));
      }
    }
    return obc.list();
  }

  /**
   * Generic OBCriteria with filter on readable clients and organizations active.
   * 
   * @param clazz
   *          Class (entity).
   * @param values
   *          Value. Property, value and operator.
   * @return All the records that satisfy the conditions.
   */
  public static <T extends BaseOBObject> List<T> getAllInstances(Class<T> clazz, Value... values) {
    return getAllInstances(clazz, true, true, values);
  }

  /**
   * Generic OBCriteria.
   * 
   * @param clazz
   *          Class (entity).
   * @param values
   *          Value. Property, value and operator.
   * @return One record that satisfies the conditions.
   */
  public static <T extends BaseOBObject> T getOneInstance(Class<T> clazz, Value... values) {
    OBCriteria<T> obc = OBDal.getInstance().createCriteria(clazz);
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnReadableOrganization(false);
    obc.add(Restrictions.ne(Client.PROPERTY_ID, "0"));
    for (Value value : values) {
      if (value.getValue() == null && "==".equals(value.getOperator())) {
        obc.add(Restrictions.isNull(value.getField()));
      } else if (value.getValue() == null && "!=".equals(value.getOperator())) {
        obc.add(Restrictions.isNotNull(value.getField()));
      } else if ("==".equals(value.getOperator())) {
        obc.add(Restrictions.eq(value.getField(), value.getValue()));
      } else if ("!=".equals(value.getOperator())) {
        obc.add(Restrictions.ne(value.getField(), value.getValue()));
      } else if ("<".equals(value.getOperator())) {
        obc.add(Restrictions.lt(value.getField(), value.getValue()));
      } else if (">".equals(value.getOperator())) {
        obc.add(Restrictions.gt(value.getField(), value.getValue()));
      } else if ("<=".equals(value.getOperator())) {
        obc.add(Restrictions.le(value.getField(), value.getValue()));
      } else if (">=".equals(value.getOperator())) {
        obc.add(Restrictions.ge(value.getField(), value.getValue()));
      } else {
        obc.add(Restrictions.eq(value.getField(), value.getValue()));
      }
    }

    final List<T> listt = obc.list();
    if (listt != null && listt.size() > 0) {
      return listt.get(0);
    } else {
      return null;
    }

  }

  public static BigDecimal getDepositAmount(Boolean isReceipt, BigDecimal amount) {
    BigDecimal deposit = BigDecimal.ZERO;
    if (isReceipt) {
      if (amount.compareTo(BigDecimal.ZERO) == 1) {
        deposit = amount;
      }
      // else received payment was negative so treat as payment
    } else {
      if (amount.compareTo(BigDecimal.ZERO) == -1) {
        // Negative payment out is a deposit
        deposit = amount.abs();
      }
    }
    return deposit;
  }

  public static BigDecimal getPaymentAmount(Boolean isReceipt, BigDecimal amount) {
    BigDecimal payment = BigDecimal.ZERO;
    if (isReceipt) {
      if (amount.compareTo(BigDecimal.ZERO) == -1) {
        // Negative payment in, treat as payment
        payment = amount.abs();
      }
    } else {
      if (amount.compareTo(BigDecimal.ZERO) == 1) {
        payment = amount;
      }
      // else sent payment was negative so treat as deposit
    }
    return payment;

  }

  /**
   * Convert a multi currency amount to a string for display in the UI. If amount has been converted
   * to a different currency, then output that converted amount and currency as well
   * 
   * @param amt
   *          Amount of payment
   * @param currency
   *          Currency payment was made in
   * @param convertedAmt
   *          Amount of payment in converted currency
   * @param convertedCurrency
   *          Currency payment was converted to/from
   * @return String version of amount formatted for display to user
   */
  public static String multiCurrencyAmountToDisplay(BigDecimal amt, Currency currency,
      BigDecimal convertedAmt, Currency convertedCurrency) {
    StringBuffer out = new StringBuffer();
    final UIDefinitionController.FormatDefinition formatDef = UIDefinitionController.getInstance()
        .getFormatDefinition("euro", "Edition");

    String formatWithDot = formatDef.getFormat();
    DecimalFormatSymbols dfs = new DecimalFormatSymbols();
    try {
      dfs.setDecimalSeparator(formatDef.getDecimalSymbol().charAt(0));
      dfs.setGroupingSeparator(formatDef.getGroupingSymbol().charAt(0));
      // Use . as decimal separator
      final String DOT = ".";
      if (!DOT.equals(formatDef.getDecimalSymbol())) {
        formatWithDot = formatWithDot.replace(formatDef.getGroupingSymbol(), "@");
        formatWithDot = formatWithDot.replace(formatDef.getDecimalSymbol(), ".");
        formatWithDot = formatWithDot.replace("@", ",");
      }
    } catch (Exception e) {
      // If any error use euroEdition default format
      formatWithDot = "#0.00";
    }
    DecimalFormat amountFormatter = new DecimalFormat(formatWithDot, dfs);
    amountFormatter.setMaximumFractionDigits(currency.getStandardPrecision().intValue());

    out.append(amountFormatter.format(amt));
    if (convertedCurrency != null && !currency.equals(convertedCurrency)
        && amt.compareTo(BigDecimal.ZERO) != 0) {
      amountFormatter.setMaximumFractionDigits(convertedCurrency.getStandardPrecision().intValue());
      out.append(" (")
          .append(amountFormatter.format(convertedAmt))
          .append(" ")
          .append(convertedCurrency.getISOCode())
          .append(")");
    }

    return out.toString();
  }

  /**
   * Determine the conversion rate from one currency to another on a given date. Will use the spot
   * conversion rate defined by the system for that date
   * 
   * @param fromCurrency
   *          Currency to convert from
   * @param toCurrency
   *          Currency being converted to
   * @param conversionDate
   *          Date conversion is being performed
   * @return A valid conversion rate for the parameters, or null if no conversion rate can be found
   */
  public static ConversionRate getConversionRate(Currency fromCurrency, Currency toCurrency,
      Date conversionDate, Organization org) {
    java.util.List<ConversionRate> conversionRateList;
    ConversionRate conversionRate;
    OBContext.setAdminMode(true);
    try {
      final OBCriteria<ConversionRate> obcConvRate = OBDal.getInstance()
          .createCriteria(ConversionRate.class);
      obcConvRate.setFilterOnReadableOrganization(false);
      obcConvRate.add(Restrictions.eq(ConversionRate.PROPERTY_ORGANIZATION, org));
      obcConvRate.add(Restrictions.eq(ConversionRate.PROPERTY_CURRENCY, fromCurrency));
      obcConvRate.add(Restrictions.eq(ConversionRate.PROPERTY_TOCURRENCY, toCurrency));
      obcConvRate.add(Restrictions.le(ConversionRate.PROPERTY_VALIDFROMDATE, conversionDate));
      long oneDay = 24 * 60 * 60 * 1000;
      obcConvRate.add(Restrictions.ge(ConversionRate.PROPERTY_VALIDTODATE,
          new Date(conversionDate.getTime() - oneDay)));
      conversionRateList = obcConvRate.list();
      if ((conversionRateList != null) && (conversionRateList.size() != 0)) {
        conversionRate = conversionRateList.get(0);
      } else {
        if ("0".equals(org.getId())) {
          conversionRate = null;
        } else {
          return getConversionRate(fromCurrency, toCurrency, conversionDate,
              OBDal.getInstance()
                  .get(Organization.class,
                      OBContext.getOBContext()
                          .getOrganizationStructureProvider()
                          .getParentOrg(org.getId())));
        }
      }
    } catch (Exception e) {
      log4j.error("Error getting conversion rate", e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
    return conversionRate;
  }

  /**
   * Determine the conversion rate from one currency to another on a given date and given
   * documentId. Will use the spot conversion rate defined by the system for that date
   * 
   * @param fromCurrency
   *          Currency to convert from
   * @param toCurrency
   *          Currency being converted to
   * @param documentId
   *          DocumentId to find the value in table c_conversion_rate_document
   * @param entity
   *          Entity type of the document
   * @return A valid conversion rate for the parameters, or null if no conversion rate can be found
   */
  public static ConversionRateDoc getConversionRateDoc(Currency fromCurrency, Currency toCurrency,
      String documentId, Entity entity) {

    final OBCriteria<ConversionRateDoc> obcConvRateDoc = OBDal.getInstance()
        .createCriteria(ConversionRateDoc.class);

    if (entity.equals(ModelProvider.getInstance().getEntity("Invoice"))) {
      obcConvRateDoc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_INVOICE,
          OBDal.getInstance().get(Invoice.class, documentId)));
    } else if (entity.equals(ModelProvider.getInstance().getEntity("FIN_Payment"))) {
      obcConvRateDoc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_PAYMENT,
          OBDal.getInstance().get(FIN_Payment.class, documentId)));
    } else if (entity.equals(ModelProvider.getInstance().getEntity("FIN_Finacc_Transaction"))) {
      obcConvRateDoc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_FINANCIALACCOUNTTRANSACTION,
          OBDal.getInstance().get(FIN_FinaccTransaction.class, documentId)));
    }
    obcConvRateDoc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_CURRENCY, fromCurrency));
    obcConvRateDoc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_TOCURRENCY, toCurrency));
    obcConvRateDoc.setMaxResults(1);
    if (obcConvRateDoc.uniqueResult() != null) {
      return (ConversionRateDoc) obcConvRateDoc.uniqueResult();
    } else {
      return null;
    }
  }

  public static int getConversionRatePrecision(VariablesSecureApp vars) {
    try {
      String formatOutput = vars.getSessionValue("#FormatOutput|generalQtyRelation",
          "#,##0.######");
      String decimalSeparator = ".";
      if (formatOutput.contains(decimalSeparator)) {
        formatOutput = formatOutput.substring(formatOutput.indexOf(decimalSeparator),
            formatOutput.length());
        return formatOutput.length() - decimalSeparator.length();
      } else {
        return 0;
      }
    } catch (Exception e) {
      log4j.error("Error getting conversion rate precission", e);
      return 6; // by default precision of 6 decimals as is defaulted in
                // Format.xml
    }
  }

  /**
   * Formats a number using the given format, decimal and grouping separator.
   * 
   * @param number
   *          Number to be formatted.
   * @param javaFormat
   *          Java number format pattern.
   * @param _decimalSeparator
   *          Symbol used as decimal separator.
   * @param _groupingSeparator
   *          Symbol used as grouping separator.
   * @return Formatted string.
   */
  public static String formatNumber(BigDecimal number, String javaFormat, String _decimalSeparator,
      String _groupingSeparator) {
    if (StringUtils.isEmpty(javaFormat)) {
      return formatNumber(number);
    }
    String decimalSeparator = _decimalSeparator;
    String groupingSeparator = _groupingSeparator;
    if (StringUtils.isEmpty(decimalSeparator) || StringUtils.isEmpty(groupingSeparator)) {
      decimalSeparator = ".";
      groupingSeparator = ",";
    }
    DecimalFormatSymbols dfs = new DecimalFormatSymbols();
    DecimalFormat dc;
    try {
      dfs.setDecimalSeparator(decimalSeparator.charAt(0));
      dfs.setGroupingSeparator(groupingSeparator.charAt(0));
      dc = new DecimalFormat(javaFormat, dfs);

    } catch (Exception e) {
      // If any error use euroEdition default format
      dc = new DecimalFormat("#0.00", dfs);
    }
    return dc.format(number);
  }

  /**
   * Formats a number using the euroEdition (see Format.xml) format.
   * 
   * @param number
   *          Number to be formatted.
   * @return Formatted string.
   */
  public static String formatNumber(BigDecimal number) {
    return formatNumber(number, "euro", "Edition");
  }

  /**
   * Formats a number using the provided formatId and qualifier (see Format.xml) format.
   * 
   * @param number
   *          Number to be formatted.
   * @param formatId
   *          euro, price, qty, etc.
   * @param qualifier
   *          Edition, Relation, etc,
   * @return Formatted string.
   */
  public static String formatNumber(BigDecimal number, String formatId, String qualifier) {
    final UIDefinitionController.FormatDefinition formatDef = UIDefinitionController.getInstance()
        .getFormatDefinition(formatId, qualifier);

    String formatWithDot = formatDef.getFormat();
    DecimalFormatSymbols dfs = new DecimalFormatSymbols();
    DecimalFormat amountFormatter;
    try {
      dfs.setDecimalSeparator(formatDef.getDecimalSymbol().charAt(0));
      dfs.setGroupingSeparator(formatDef.getGroupingSymbol().charAt(0));
      // Use . as decimal separator
      final String DOT = ".";
      if (!DOT.equals(formatDef.getDecimalSymbol())) {
        formatWithDot = formatWithDot.replace(formatDef.getGroupingSymbol(), "@");
        formatWithDot = formatWithDot.replace(formatDef.getDecimalSymbol(), ".");
        formatWithDot = formatWithDot.replace("@", ",");
      }
      amountFormatter = new DecimalFormat(formatWithDot, dfs);
    } catch (Exception e) {
      // If any error use euroEdition default format
      amountFormatter = new DecimalFormat("#0.00", dfs);
    }
    return amountFormatter.format(number);
  }

  /**
   * Returns either the Invoice's Document Number or the Invoice's Supplier Reference based on the
   * Organization's configuration. In case the Supplier Reference is empty, the invoice's document
   * number is returned
   * 
   * @param organization
   *          to get its configuration. In case no configuration is available, the invoice's
   *          document number is returned
   */
  public static String getDesiredDocumentNo(final Organization organization,
      final Invoice invoice) {
    String invoiceDocNo;
    try {
      // By default take the invoice document number
      invoiceDocNo = invoice.getDocumentNo();

      final String paymentDescription = OBDal.getInstance()
          .get(OrganizationInformation.class, (organization.getId()))
          .getAPRMPaymentDescription();
      // In case of a purchase invoice and the Supplier Reference is selected use
      // Reference
      if (paymentDescription.equals("Supplier Reference") && !invoice.isSalesTransaction()) {
        invoiceDocNo = invoice.getOrderReference();
        if (invoiceDocNo == null) {
          invoiceDocNo = invoice.getDocumentNo();
        }
      }
    } catch (Exception e) {
      invoiceDocNo = invoice.getDocumentNo();
    }

    return invoiceDocNo;
  }

  /**
   * Returns if given payment status and related payment schedule detail belong to a confirmed
   * payment
   * 
   */
  public static boolean isPaymentConfirmed(String status, FIN_PaymentScheduleDetail psd) {
    List<Object> parameters = new ArrayList<Object>();
    parameters.add(status);
    parameters.add((psd != null) ? psd.getId() : "");
    String result = (String) CallStoredProcedure.getInstance()
        .call("APRM_ISPAYMENTCONFIRMED", parameters, null, false);

    return "Y".equals(result);
  }

  /**
   * Returns a list of Payment Status. If isConfirmed equals true, then the status returned are
   * confirmed payments. Else they are pending of execution
   * 
   */
  private static List<String> getListPaymentConfirmedOrNot(Boolean isConfirmed) {

    List<String> listPaymentConfirmedOrNot = new ArrayList<String>();
    OBContext.setAdminMode(true);
    try {
      final OBCriteria<org.openbravo.model.ad.domain.List> obCriteria = OBDal.getInstance()
          .createCriteria(org.openbravo.model.ad.domain.List.class);
      obCriteria.add(cb.equal(root.get("reference.id"), "575BCB88A4694C27BC013DE9C73E6FE7"));
      List<org.openbravo.model.ad.domain.List> adRefList = obCriteria.list();
      for (org.openbravo.model.ad.domain.List adRef : adRefList) {
        if (isConfirmed.equals(isPaymentConfirmed(adRef.getSearchKey(), null))) {
          listPaymentConfirmedOrNot.add(adRef.getSearchKey());
        }
      }
      return listPaymentConfirmedOrNot;
    } catch (Exception e) {
      log4j.error("Error getting list of confirmed payments", e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns a list of Payment Status. If isConfirmed equals true, then the status returned are
   * confirmed payments. Else they are pending of execution
   * 
   */
  private static List<String> getListPaymentConfirmedOrNot(Boolean isConfirmed,
      FIN_PaymentScheduleDetail psd) {

    List<String> listPaymentConfirmedOrNot = new ArrayList<String>();
    OBContext.setAdminMode(true);
    try {
      final OBCriteria<org.openbravo.model.ad.domain.List> obCriteria = OBDal.getInstance()
          .createCriteria(org.openbravo.model.ad.domain.List.class);
      obCriteria.add(cb.equal(root.get("reference.id"), "575BCB88A4694C27BC013DE9C73E6FE7"));
      List<org.openbravo.model.ad.domain.List> adRefList = obCriteria.list();
      for (org.openbravo.model.ad.domain.List adRef : adRefList) {
        if (isConfirmed.equals(isPaymentConfirmed(adRef.getSearchKey(), psd))) {
          listPaymentConfirmedOrNot.add(adRef.getSearchKey());
        }
      }
      return listPaymentConfirmedOrNot;
    } catch (Exception e) {
      log4j.error("Error building the payment confirmed list", e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns a list confirmed Payment Status
   * 
   */
  public static List<String> getListPaymentConfirmed() {
    return getListPaymentConfirmedOrNot(true);
  }

  /**
   * Returns a list confirmed Payment Status
   * 
   */
  public static List<String> getListPaymentConfirmed(FIN_PaymentScheduleDetail psd) {
    return getListPaymentConfirmedOrNot(true, psd);
  }

  /**
   * Returns a list not confirmed Payment Status
   * 
   */
  public static List<String> getListPaymentNotConfirmed() {
    return getListPaymentConfirmedOrNot(false);
  }

  /**
   * Returns a list not confirmed Payment Status
   * 
   */
  public static List<String> getListPaymentNotConfirmed(FIN_PaymentScheduleDetail psd) {
    return getListPaymentConfirmedOrNot(false, psd);
  }

  /**
   * Returns the legal entity of the given organization
   * 
   * @param org
   *          organization to get its legal entity
   * @return legal entity (with or without accounting) organization or null if not found
   */
  public static Organization getLegalEntityOrg(final Organization org) {
    try {
      OBContext.setAdminMode(true);
      final OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider(org.getClient().getId());
      for (final String orgId : osp.getParentList(org.getId(), true)) {
        final Organization parentOrg = OBDal.getInstance().get(Organization.class, orgId);
        if (parentOrg.getOrganizationType().isLegalEntity()) {
          return parentOrg;
        }
      }
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Return true if the period is open for a client, document type, organization and accounting date
   * 
   * @param client
   *          the client for which it wants to know if the period is open
   * @param documentType
   *          It is the docbasetype from the document type
   * @param org
   *          the Organization for which it wants to know if the period is open
   * @param dateAcct
   *          The accounting date from the document
   * @return boolean
   */
  public static boolean isPeriodOpen(String client, String documentType, String org,
      String dateAcct) {
    final Session session = OBDal.getInstance().getSession();

    // @formatter:off
    final String hql = ""
        + "select max(p.id) as period "
        + " from FinancialMgmtPeriodControl pc "
        + "   left join pc.period p "
        + " where p.client.id = :clientId"
        + "   and pc.documentCategory = :documentType"
        + "   and pc.periodStatus = 'O' "
        + "   and pc.organization = ad_org_getcalendarowner(:org) "
        + "   and to_date(:dateAcct) >= p.startingDate "
        + "   and to_date(:dateAcct) < p.endingDate + 1 ";
    // @formatter:on
    final Query<String> qry = session.createQuery(hql, String.class);
    qry.setParameter("clientId", client);
    qry.setParameter("documentType", documentType);
    qry.setParameter("dateAcct", dateAcct);
    qry.setParameter("org", org);

    String period = qry.list().get(0);
    return period != null;
  }

  public static boolean periodControlOpened(String tableName, String recordId, String idColumnName,
      String orgType) {

    List<Object> parameters = new ArrayList<Object>();
    parameters.add(tableName);
    parameters.add(recordId);
    parameters.add(idColumnName);
    parameters.add(orgType);
    Object result = CallStoredProcedure.getInstance()
        .call("ad_get_doc_le_bu", parameters, null, false, true);

    Organization org = OBDal.getInstance().get(Organization.class, result);

    return org.getOrganizationType().isLegalEntityWithAccounting();
  }

  /**
   * Returns true if the Business Partner is blocked for the document type selected.
   * 
   * @param strBPartnerId
   *          . Business Partner Id.
   * @param issotrx
   *          . True if Sales, False if Purchase.
   * @param docType
   *          1: Order. 2: Goods Receipt / Shipment. 3: Invoice. 4: Payment.
   */
  public static boolean isBlockedBusinessPartner(String strBPartnerId, boolean issotrx,
      int docType) {
    try {
      OBContext.setAdminMode(true);
      BusinessPartner bPartner = OBDal.getInstance().get(BusinessPartner.class, strBPartnerId);
      switch (docType) {
        case 1: {
          // Order
          return ((issotrx && bPartner.isCustomerBlocking() && bPartner.isSalesOrder())
              || (!issotrx && bPartner.isVendorBlocking() && bPartner.isPurchaseOrder()));

        }
        case 2: {
          // Goods Shipment / Receipt
          return ((issotrx && bPartner.isCustomerBlocking() && bPartner.isGoodsShipment())
              || (!issotrx && bPartner.isVendorBlocking() && bPartner.isGoodsReceipt()));

        }
        case 3: {
          // Invoice
          return ((issotrx && bPartner.isCustomerBlocking() && bPartner.isSalesInvoice())
              || (!issotrx && bPartner.isVendorBlocking() && bPartner.isPurchaseInvoice()));
        }
        case 4: {
          // Payment
          return ((issotrx && bPartner.isCustomerBlocking() && bPartner.isPaymentIn())
              || (!issotrx && bPartner.isVendorBlocking() && bPartner.isPaymentOut()));

        }
        default:
          log4j.error("Error in isBusinessPartnerBlocking: docType must be between 1 and 4");
          return false;
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns Payment Details from a Payment ordered by Invoice and Order
   */
  public static List<String> getOrderedPaymentDetailList(String paymentId) {

    List<String> pdList = null;

    OBContext.setAdminMode(true);
    try {
      // @formatter:off
      final String whereClause = ""
          + " select pd.id"
          + " from FIN_Payment_Detail as pd"
          + "   inner join pd.fINPaymentScheduleDetailList as psd"
          + " where pd.finPayment.id = :paymentId "
          + "   and pd.active = true"
          + " order by psd.invoicePaymentSchedule"
          + "   , coalesce(psd.orderPaymentSchedule"
          + "   ,'0')";
      // @formatter:on
      Query<String> query = OBDal.getInstance().getSession().createQuery(whereClause, String.class);
      query.setParameter("paymentId", paymentId);
      pdList = query.list();

    } finally {
      OBContext.restorePreviousMode();
    }

    return pdList;
  }

  /**
   * Returns true if the payment is a reverse payment not a reversed one.
   */
  public static boolean isReversePayment(FIN_Payment payment) {
    final Session session = OBDal.getInstance().getSession();

    // @formatter:off
    final String hql = ""
        + "select count(p) "
        + "from FIN_Payment p "
        + "where p.reversedPayment.id = :paymentId";
    // @formatter:on

    final Query<Long> qry = session.createQuery(hql, Long.class);
    qry.setParameter("paymentId", payment.getId());
    return qry.list().get(0) > Long.parseLong("0");
  }

  /**
   * Returns the invoice payment status value configured in the payment method in the financial
   * account for a payment
   */
  public static String invoicePaymentStatus(FIN_Payment payment) {
    return invoicePaymentStatus(payment.getPaymentMethod(), payment.getAccount(),
        payment.isReceipt());
  }

  /**
   * Returns the invoice payment status value configured in the payment method in the financial
   * account for a payment
   */
  public static String invoicePaymentStatus(FIN_PaymentMethod paymentMethod,
      FIN_FinancialAccount financialAccount, boolean isReceipt) {
    String status = null;
    // Reload financial account to avoid lazy initialization when no session
    FIN_FinancialAccount finAcct = OBDal.getInstance()
        .get(FIN_FinancialAccount.class, financialAccount.getId());
    for (FinAccPaymentMethod finaccpaymentmethod : finAcct
        .getFinancialMgmtFinAccPaymentMethodList()) {
      if (finaccpaymentmethod.getPaymentMethod().getId().equals(paymentMethod.getId())) {
        if (isReceipt) {
          status = finaccpaymentmethod.getPayinInvoicepaidstatus();
        } else {
          status = finaccpaymentmethod.getPayoutInvoicepaidstatus();
        }
      }
    }
    return status;
  }

  /**
   * This function should only be called when it should update the payment amounts
   */
  public static void updatePaymentAmounts(FIN_PaymentScheduleDetail psd) {

    if (psd.getInvoicePaymentSchedule() != null) {
      BusinessPartner bPartner = psd.getInvoicePaymentSchedule().getInvoice().getBusinessPartner();
      BigDecimal creditUsed = bPartner.getCreditUsed();
      BigDecimal amountWithSign = psd.getInvoicePaymentSchedule().getInvoice().isSalesTransaction()
          ? psd.getAmount()
          : psd.getAmount().negate();
      creditUsed = creditUsed.subtract(amountWithSign);
      bPartner.setCreditUsed(creditUsed);
      OBDal.getInstance().save(bPartner);
      FIN_AddPayment.updatePaymentScheduleAmounts(psd.getPaymentDetails(),
          psd.getInvoicePaymentSchedule(), psd.getAmount(), psd.getWriteoffAmount());
    }
    if (psd.getOrderPaymentSchedule() != null) {
      FIN_AddPayment.updatePaymentScheduleAmounts(psd.getPaymentDetails(),
          psd.getOrderPaymentSchedule(), psd.getAmount(), psd.getWriteoffAmount());
    }
    if (psd.getPaymentDetails().isPrepayment() && psd.getOrderPaymentSchedule() == null
        && psd.getInvoicePaymentSchedule() == null) {
      // This PSD is credit
      BusinessPartner bPartner = psd.getPaymentDetails().getFinPayment().getBusinessPartner();
      BigDecimal creditUsed = bPartner.getCreditUsed();
      BigDecimal amountWithSign = psd.getPaymentDetails().getFinPayment().isReceipt()
          ? psd.getAmount()
          : psd.getAmount().negate();
      creditUsed = creditUsed.subtract(amountWithSign);
      bPartner.setCreditUsed(creditUsed);
      OBDal.getInstance().save(bPartner);
    }

  }

  public static void updateBusinessPartnerCredit(FIN_Payment payment) {
    if (payment == null) {
      return;
    }
    // When credit is used (consumed) we compensate so_creditused as this amount is
    // already
    // included in the payment details. Credit consumed should not affect to
    // so_creditused
    if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 0
        && payment.getUsedCredit().compareTo(BigDecimal.ZERO) != 0) {
      BusinessPartner bp = payment.getBusinessPartner();
      if (payment.isReceipt()) {
        bp.setCreditUsed(bp.getCreditUsed().add(payment.getUsedCredit()));
      } else {
        bp.setCreditUsed(bp.getCreditUsed().subtract(payment.getUsedCredit()));
      }
      OBDal.getInstance().save(bp);
    }
  }

  /**
   * Returns the sequence number of payment status in reference list
   * 
   * 
   */
  public static int seqnumberpaymentstatus(String status) {
    List<Object> parameters = new ArrayList<Object>();
    parameters.add(status);
    int result = Integer.parseInt((String) CallStoredProcedure.getInstance()
        .call("aprm_seqnumberpaymentstatus", parameters, null, false));

    return result;
  }

  /**
   * This function should only be called when it should update the payment amounts
   */
  public static void restorePaidAmounts(FIN_PaymentScheduleDetail paymentScheduleDetail) {

    BigDecimal psdWriteoffAmount = paymentScheduleDetail.getWriteoffAmount();
    BigDecimal psdAmount = paymentScheduleDetail.getAmount();
    BigDecimal amount = psdAmount.add(psdWriteoffAmount);
    BusinessPartner businessPartner = paymentScheduleDetail.getPaymentDetails()
        .getFinPayment()
        .getBusinessPartner();
    if (paymentScheduleDetail.getInvoicePaymentSchedule() != null) {

      FIN_AddPayment.updatePaymentScheduleAmounts(paymentScheduleDetail.getPaymentDetails(),
          paymentScheduleDetail.getInvoicePaymentSchedule(), psdAmount.negate(),
          psdWriteoffAmount.negate());
      // BP SO_CreditUsed
      businessPartner = paymentScheduleDetail.getInvoicePaymentSchedule()
          .getInvoice()
          .getBusinessPartner();
      if (paymentScheduleDetail.getPaymentDetails().getFinPayment().isReceipt()) {
        increaseCustomerCredit(businessPartner, amount);
      } else {
        decreaseCustomerCredit(businessPartner, amount);
      }

    }
    if (paymentScheduleDetail.getOrderPaymentSchedule() != null) {
      FIN_AddPayment.updatePaymentScheduleAmounts(paymentScheduleDetail.getPaymentDetails(),
          paymentScheduleDetail.getOrderPaymentSchedule(), psdAmount.negate(),
          psdWriteoffAmount.negate());
    }
    // when generating credit for a BP SO_CreditUsed is also updated
    if (paymentScheduleDetail.getInvoicePaymentSchedule() == null
        && paymentScheduleDetail.getOrderPaymentSchedule() == null
        && paymentScheduleDetail.getPaymentDetails().getGLItem() == null
        && !paymentScheduleDetail.getPaymentDetails().isRefund()) {
      // BP SO_CreditUsed
      if (paymentScheduleDetail.getPaymentDetails().getFinPayment().isReceipt()) {
        increaseCustomerCredit(businessPartner, amount);
      } else {
        decreaseCustomerCredit(businessPartner, amount);
      }
    }
    paymentScheduleDetail.setInvoicePaid(false);
    OBDal.getInstance().save(paymentScheduleDetail);

  }

  /**
   * Method used to update the credit used when the user doing invoice processing or payment
   * processing
   * 
   * @param amount
   *          Payment amount
   */
  private static void updateCustomerCredit(BusinessPartner businessPartner, BigDecimal amount,
      boolean add) {
    BigDecimal creditUsed = businessPartner.getCreditUsed();
    if (add) {
      creditUsed = creditUsed.add(amount);
    } else {
      creditUsed = creditUsed.subtract(amount);
    }
    businessPartner.setCreditUsed(creditUsed);
    OBDal.getInstance().save(businessPartner);
    // OBDal.getInstance().flush();
  }

  private static void increaseCustomerCredit(BusinessPartner businessPartner, BigDecimal amount) {
    updateCustomerCredit(businessPartner, amount, true);
  }

  private static void decreaseCustomerCredit(BusinessPartner businessPartner, BigDecimal amount) {
    updateCustomerCredit(businessPartner, amount, false);
  }

  public static FinAccPaymentMethod getFinancialAccountPaymentMethod(String paymentMethodId,
      String financialAccountId, boolean issotrx, String currencyId) {
    return getFinancialAccountPaymentMethod(paymentMethodId, financialAccountId, issotrx,
        currencyId, null);
  }

  /**
   * Get an active FinAccPaymentMethod related to paymentMethodId FIN_PaymentMethod and
   * financialAccountId FIN_FinancialAccount, if exists. If paymentMethodId is null it will retrieve
   * any FinAccPaymentMethod related to paymentMethodId FIN_PaymentMethod ordered by default field.
   * FinAccPaymentMethod must have pay in/out active and must be compatible with currencyId Currency
   * if currencyId is not null.
   */
  public static FinAccPaymentMethod getFinancialAccountPaymentMethod(String paymentMethodId,
      String financialAccountId, boolean issotrx, String currencyId, String orgId) {
    // @formatter:off
    String where = ""
          + " as fapm"
          + "   join fapm.account as fa"
          + " where fapm.paymentMethod.id = :paymentMethodId"
          + "   and fa.active = true";
    if (issotrx) {
     where += " and fapm.payinAllow = true";
    } else {
     where += " and fapm.payoutAllow = true";
    }
    if (StringUtils.isNotEmpty(financialAccountId)) {
     where += " and fa.id = :financialAccountId";
    }
    if (StringUtils.isNotEmpty(currencyId)) {
     where += " and (fa.currency.id = :currencyId";
      if (issotrx) {
        where += "   or fapm.payinIsMulticurrency = true)";
      } else {
        where += "   or fapm.payoutIsMulticurrency = true)";
      }
    }
    if (StringUtils.isNotEmpty(orgId)) {
     where += " and ad_org_isinnaturaltree(fa.organization.id, :orgId, fa.client.id) = 'Y'";
    }
    where += " order by fapm.default desc";
    // @formatter:on

    OBQuery<FinAccPaymentMethod> qry = OBDal.getInstance()
        .createQuery(FinAccPaymentMethod.class, where);
    qry.setFilterOnReadableOrganization(false);
    qry.setMaxResult(1);

    qry.setNamedParameter("paymentMethodId", paymentMethodId);
    if (StringUtils.isNotEmpty(financialAccountId)) {
      qry.setNamedParameter("financialAccountId", financialAccountId);
    }
    if (StringUtils.isNotEmpty(currencyId)) {
      qry.setNamedParameter("currencyId", currencyId);
    }
    if (StringUtils.isNotEmpty(orgId)) {
      qry.setNamedParameter("orgId", orgId);
    }

    return (FinAccPaymentMethod) qry.uniqueResult();
  }

  /**
   * Appends existing Financial Account Transaction description with either GL Item Description or
   * Payment description in a new line
   */

  public static String getFinAccTransactionDescription(String description, String removeDescription,
      String appendDescription) {
    try {
      String returnDescription = description == null ? "" : description;
      if (StringUtils.isBlank(removeDescription)
          && StringUtils.contains(description, StringUtils.trim(appendDescription))) {
        return description;
      }
      if (description != null && !description.equals("null") && !StringUtils.isBlank(description)) {
        if (!StringUtils.isBlank(removeDescription)
            && description.indexOf(removeDescription) != -1) {
          returnDescription = returnDescription.substring(0, description.indexOf(removeDescription))
              + (StringUtils.isBlank(appendDescription) ? "" : appendDescription);
        } else if (StringUtils.isNotBlank(appendDescription)) {
          returnDescription = returnDescription + "\n" + appendDescription;
        }
      } else {
        returnDescription = appendDescription;
      }
      return returnDescription;
    } catch (Exception e) {
      if (StringUtils.isNotBlank(appendDescription)) {
        return appendDescription;
      } else {
        return description;
      }
    }
  }

  public static FIN_FinaccTransaction getFinAccTransaction(FIN_Payment payment) {
    OBCriteria<FIN_FinaccTransaction> finAccTransactionCriteria = OBDal.getInstance()
        .createCriteria(FIN_FinaccTransaction.class);
    finAccTransactionCriteria
        .add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_FINPAYMENT, payment));
    finAccTransactionCriteria.setMaxResults(1);
    return (FIN_FinaccTransaction) finAccTransactionCriteria.uniqueResult();
  }

  /**
   * Get the default document for a payment based on the transaction type.
   *
   * If the transaction type is BP Deposit, the default document is RCIN.
   * If the transaction type is BP Withdrawal, the default document is PDOUT.
   * If the transaction type is neither BP Deposit nor BP Withdrawal, an empty string is returned.
   *
   * @param context the JSON context object
   * @return the default document for the payment, or an empty string if the transaction type is
   *         neither BP Deposit nor BP Withdrawal
   */
  public static String getDefaultAddPaymentDocument(JSONObject context) {
    String docType = getFirstNonEmpty(context, APRMConstants.TRXTYPE, APRMConstants.INPTRXTYPE);

    if (StringUtils.isBlank(docType)) {
      docType = APRMConstants.DEFAULT_EMPTY_VALUE;
    }

    switch (docType) {
      case APRMConstants.TRXTYPE_BPDeposit:
        return APRMConstants.RCIN;
      case APRMConstants.TRXTYPE_BPWithdrawal:
        return APRMConstants.PDOUT;
      default:
        return APRMConstants.DEFAULT_EMPTY_VALUE;
    }
  }

  /**
   * Gets the default organization for a payment based on the given context.
   *
   * If the context contains the organization ID, that ID is returned. Otherwise, the organization of the default financial account is returned.
   * If no default financial account is found, null is returned.
   *
   * @param context the JSON context object
   * @return the default organization for the payment, or null if no default financial account is found
   */
  public static String getDefaultOrganization(JSONObject context) {
    String orgId = getFirstNonEmpty(context, APRMConstants.AD_ORG_ID, APRMConstants.INPAD_ORG_ID);

    if (StringUtils.isBlank(orgId)) {
      FIN_FinancialAccount account = getDefaultFinancialAccount(context);
      return account != null ? account.getOrganization().getId() : null;
    }
    return orgId;
  }

  /**
   * Retrieves the default financial account from the given JSON context.
   *
   * This method checks for financial account identifiers within the context and returns
   * the corresponding financial account if found. It searches for identifiers using
   * specific keys defined in APRMConstants.
   *
   * @param context the JSON context object containing potential financial account identifiers
   * @return the FIN_FinancialAccount object if a valid identifier is found, otherwise null
   */
  public static FIN_FinancialAccount getDefaultFinancialAccount(JSONObject context) {
    String strFinancialAccount = getFirstNonEmpty(context,
        APRMConstants.INPFIN_FINANCIAL_ACCOUNT_ID,
        APRMConstants.FIN_FINANCIAL_ACCOUNT_ID);

    return StringUtils.isNotBlank(strFinancialAccount)
        ? OBDal.getInstance().get(FIN_FinancialAccount.class, strFinancialAccount)
        : null;
  }

  /**
   * Retrieves the first non-empty value from the given keys in the given context.
   *
   * @param context the JSON context
   * @param keys the keys to look up
   * @return the first non-empty value, or null
   */
  public static String getFirstNonEmpty(JSONObject context, String... keys) {
    for (String key : keys) {
      String value = context.optString(key, APRMConstants.DEFAULT_EMPTY_VALUE);
      if (StringUtils.isNotBlank(value)) {
        return value;
      }
    }
    return null;
  }
}
