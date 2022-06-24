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
 * All portions are Copyright (C) 2012-2017 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 **/

package org.openbravo.erpCommon.ad_reports;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;

public class AgingDao {
  private static Logger log4j = LogManager.getLogger();
  private static String salesInvoiceTab = "263";
  private static String purchaseInvoiceTab = "290";
  private static String paymentInTab = "C4B6506838E14A349D6717D6856F1B56";
  private static String paymentOutTab = "F7A52FDAAA0346EFA07D53C125B40404";
  private static final int CREDIT_SCOPE = 6;

  public AgingDao() {
  }

  /**
   * This method recovers the necessary data from the database to create an array fieldProviders of
   * objects containing the information for the report
   * 
   */
  public FieldProvider[] getOpenReceivablesAgingSchedule(ConnectionProvider connectionProvider,
      String strcBpartnerId, String strAccSchema, Date currentDate, String strcolumn1,
      String strcolumn2, String strcolumn3, String strcolumn4, String strOrg,
      Set<String> organizations, String recOrPay, boolean showDoubtfulDebt, boolean excludeVoids)
      throws IOException, ServletException {
    return getOpenReceivablesAgingSchedule(connectionProvider, strcBpartnerId, strAccSchema,
        currentDate, strcolumn1, strcolumn2, strcolumn3, strcolumn4, strOrg, organizations,
        recOrPay, showDoubtfulDebt, excludeVoids, false);
  }

  public FieldProvider[] getOpenReceivablesAgingSchedule(ConnectionProvider connectionProvider,
      String strcBpartnerId, String strAccSchema, Date currentDate, String strcolumn1,
      String strcolumn2, String strcolumn3, String strcolumn4, String strOrg,
      Set<String> organizations, String recOrPay, boolean showDoubtfulDebt, boolean excludeVoids,
      boolean excludeReverseds) throws IOException, ServletException {

    // Initialization of some variables
    List<String> paidStatus = FIN_Utility.getListPaymentConfirmed();
    HashMap<String, AgingData> agingBalanceData = new HashMap<String, AgingData>();
    FieldProvider[] dataFP = null;
    Currency convCurrency = null;
    Organization organization = null;

    OBContext.setAdminMode(true);
    try {
      if (StringUtils.isEmpty(strAccSchema)) {
        organization = OBDal.getReadOnlyInstance().get(Organization.class, strOrg);
        convCurrency = organization.getCurrency();
      } else {
        AcctSchema acctSchema = OBDal.getReadOnlyInstance().get(AcctSchema.class, strAccSchema);
        convCurrency = acctSchema.getCurrency();
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    OBContext.setAdminMode(true);
    AgingDaoData dataSR = null;
    AgingDaoData dataCreditSR = null;
    long init = System.currentTimeMillis();
    final VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    int limit = (int) vars.getSessionObject("reportsLimit");
    String pgLimit = null, oraLimit = null;
    if (limit > 0) {
      if (StringUtils.equalsIgnoreCase(connectionProvider.getRDBMS(), "ORACLE")) {
        oraLimit = String.valueOf(limit + 1);
      } else {
        pgLimit = String.valueOf(limit + 1);
      }
    }

    try {
      // Amounts coming from normal PSD (non credit)
      dataSR = AgingDaoData.select(connectionProvider, showDoubtfulDebt ? "Y" : "N",
          OBDateUtils.formatDate(currentDate), convCurrency.getId(),
          OBDateUtils.formatDate(convertToDate(currentDate, strcolumn1)),
          OBDateUtils.formatDate(convertToDate(currentDate, strcolumn2)),
          OBDateUtils.formatDate(convertToDate(currentDate, strcolumn3)),
          OBDateUtils.formatDate(convertToDate(currentDate, strcolumn4)),
          Utility.getInStrSet(organizations), strcBpartnerId,
          StringUtils.equals(recOrPay, "RECEIVABLES") ? "Y" : "N",
          excludeVoids ? "excludeVoids" : "", pgLimit, oraLimit);
      log4j.debug("Query: " + (System.currentTimeMillis() - init));
      init = System.currentTimeMillis();
      int i = 0;
      while (dataSR.next()) {
        final AgingDaoData dd = dataSR.get();
        final String strBusinessPartnerId = dd.bpid;
        final String strBpName = dd.bpname;
        final BigDecimal psdAmt = new BigDecimal(dd.amount);
        final BigDecimal psddd = new BigDecimal(dd.doubtfuldebt);
        int intScope = Integer.parseInt(dd.scope);

        // if there is the first time the Business Partner is inserted
        if (agingBalanceData.containsKey(strBusinessPartnerId)) {
          // if the business partner has been inserted already
          agingBalanceData.get(strBusinessPartnerId).addAmount(psdAmt, intScope);
          agingBalanceData.get(strBusinessPartnerId).addDoubtfulDebt(psddd);
        } else { // if there is the first time the Business Partner is inserted
          agingBalanceData.put(strBusinessPartnerId,
              new AgingData(strBusinessPartnerId, strBpName, psdAmt, intScope));
        }

        i++;
        if (i % 100 == 0) {
          log4j.debug("records processed: " + i);
        }
      }
      log4j.debug("Total records processed: " + i);
      log4j.debug("Time to process: " + (System.currentTimeMillis() - init));

      // Credits: In this section the Credits are going to be processed.
      init = System.currentTimeMillis();
      // Query for credit payments
      dataCreditSR = AgingDaoData.selectCredit(connectionProvider, convCurrency.getId(),
          Utility.getInStrSet(organizations), Utility.getInStrSet(new HashSet<String>(paidStatus)),
          OBDateUtils.formatDate(currentDate),
          StringUtils.equals(recOrPay, "RECEIVABLES") ? "Y" : "N", strcBpartnerId,
          excludeReverseds ? "excludeReverseds" : "", pgLimit, oraLimit);
      log4j.debug("Credit Query: " + (System.currentTimeMillis() - init));
      init = System.currentTimeMillis();
      i = 0;
      while (dataCreditSR.next()) {
        final AgingDaoData dd = dataCreditSR.get();
        final BigDecimal creditLeft = new BigDecimal(dd.credit);
        final String strBusinessPartnerId = dd.bpid;
        final String strBpName = dd.bpname;
        if (agingBalanceData.containsKey(strBusinessPartnerId)) {
          // if the business partner has been inserted already
          agingBalanceData.get(strBusinessPartnerId).addCredit(creditLeft);
        } else {
          // if there is the first time the Business Partner is inserted
          agingBalanceData.put(strBusinessPartnerId,
              new AgingData(strBusinessPartnerId, strBpName, BigDecimal.ZERO, BigDecimal.ZERO,
                  BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, creditLeft,
                  BigDecimal.ZERO));
        }

        i++;
        if (i % 100 == 0) {
          log4j.debug("credit records processed: " + i);
        }
      }
      log4j.debug("Total credit records: " + i);
      log4j.debug("Time to process: " + (System.currentTimeMillis() - init));
    } catch (Exception e) {
      log4j.error("Error", e);
      throw new OBException("Error", e);
    } finally {
      if (dataSR != null) {
        dataSR.close();
      }
      if (dataCreditSR != null) {
        dataCreditSR.close();
      }
      OBContext.restorePreviousMode();
    }

    // Sorting and transform to field provider
    init = System.currentTimeMillis();
    List<AgingData> allData = new ArrayList<AgingData>(agingBalanceData.values());
    Collections.sort(allData);
    dataFP = FieldProviderFactory.getFieldProviderArray(allData);
    setFieldsIntoFieldProvider(dataFP, allData);

    log4j.debug("Sorting and transform to Field Provider: " + (System.currentTimeMillis() - init));
    return dataFP;
  }

  /**
   * This method returns an array of fieldProviders with the necessary information to print the
   * Aging Schedule Details report.
   */

  public FieldProvider[] getOpenReceivablesAgingScheduleDetails(
      ConnectionProvider connectionProvider, Date currentDate, SimpleDateFormat dateFormat,
      Currency convCurrency, Set<String> organizations, String recOrPay, String strcolumn1,
      String strcolumn2, String strcolumn3, String strcolumn4, String strcBpartnerId,
      boolean showDoubtfulDebt, Boolean excludeVoid) throws IOException, ServletException {
    return getOpenReceivablesAgingScheduleDetails(connectionProvider, currentDate, dateFormat,
        convCurrency, organizations, recOrPay, strcolumn1, strcolumn2, strcolumn3, strcolumn4,
        strcBpartnerId, showDoubtfulDebt, excludeVoid, false);
  }

  public FieldProvider[] getOpenReceivablesAgingScheduleDetails(
      ConnectionProvider connectionProvider, Date currentDate, SimpleDateFormat dateFormat,
      Currency convCurrency, Set<String> organizations, String recOrPay, String strcolumn1,
      String strcolumn2, String strcolumn3, String strcolumn4, String strcBpartnerId,
      boolean showDoubtfulDebt, Boolean excludeVoid, Boolean excludeReversed)
      throws IOException, ServletException {

    List<HashMap<String, String>> hashMapList = new ArrayList<HashMap<String, String>>();
    FieldProvider[] data = null;
    AgingDaoData dataSR = null;
    AgingDaoData dataCreditSR = null;

    OBContext.setAdminMode(true);
    long init = System.currentTimeMillis();
    int limit = (int) RequestContext.get().getVariablesSecureApp().getSessionObject("reportsLimit");
    String pgLimit = null, oraLimit = null;
    if (limit > 0) {
      if (StringUtils.equalsIgnoreCase(connectionProvider.getRDBMS(), "ORACLE")) {
        oraLimit = String.valueOf(limit + 1);
      } else {
        pgLimit = String.valueOf(limit + 1);
      }
    }

    try {
      // Amounts coming from normal PSD (non credit)
      dataSR = AgingDaoData.selectDetail(connectionProvider, convCurrency.getId(),
          showDoubtfulDebt ? "Y" : "N", OBDateUtils.formatDate(currentDate),
          OBDateUtils.formatDate(convertToDate(currentDate, strcolumn1)),
          OBDateUtils.formatDate(convertToDate(currentDate, strcolumn2)),
          OBDateUtils.formatDate(convertToDate(currentDate, strcolumn3)),
          OBDateUtils.formatDate(convertToDate(currentDate, strcolumn4)),
          Utility.getInStrSet(organizations), strcBpartnerId,
          StringUtils.equals(recOrPay, "RECEIVABLES") ? "Y" : "N",
          excludeVoid ? "excludeVoids" : "", pgLimit, oraLimit);

      log4j.debug("Query Detail: " + (System.currentTimeMillis() - init));
      init = System.currentTimeMillis();
      int i = 0;
      while (dataSR.next()) {
        final AgingDaoData dd = dataSR.get();
        final String strBusinessPartnerId = dd.bpid;
        final String strBpName = dd.bpname;
        final BigDecimal amount = new BigDecimal(dd.amount);
        int intScope = Integer.parseInt(dd.scope);
        final String docNo = dd.docno;
        final String invoiceId = dd.invoiceid;
        final Date invoiceDate = OBDateUtils.getDate(dd.dateinvoiced);
        final BigDecimal doubtfulDebtAmount = new BigDecimal(dd.doubtfuldebt);

        HashMap<String, String> psData = insertData(docNo, invoiceId, invoiceDate, amount,
            strBusinessPartnerId, strBpName, intScope,
            StringUtils.equals(recOrPay, "RECEIVABLES") ? salesInvoiceTab : purchaseInvoiceTab,
            dateFormat, false, doubtfulDebtAmount);
        hashMapList.add(psData);

        i++;
        if (i % 100 == 0) {
          log4j.debug("records processed: " + i);
        }
      }
      log4j.debug("Total records processed: " + i);
      log4j.debug("Time to process: " + (System.currentTimeMillis() - init));

      // Credits: In this section the Credits are going to be processed.
      init = System.currentTimeMillis();

      final List<String> paidStatus = FIN_Utility.getListPaymentConfirmed();
      // Query for credit payments
      dataCreditSR = AgingDaoData.selectCredit(connectionProvider, convCurrency.getId(),
          Utility.getInStrSet(organizations), Utility.getInStrSet(new HashSet<String>(paidStatus)),
          OBDateUtils.formatDate(currentDate),
          StringUtils.equals(recOrPay, "RECEIVABLES") ? "Y" : "N", strcBpartnerId,
          excludeReversed ? "excludeReverseds" : "", pgLimit, oraLimit);
      log4j.debug("Credit Query: " + (System.currentTimeMillis() - init));
      init = System.currentTimeMillis();
      i = 0;
      while (dataCreditSR.next()) {
        final AgingDaoData dd = dataCreditSR.get();
        final String strBusinessPartnerId = dd.bpid;
        final String strBpName = dd.bpname;
        final String documentNo = dd.docno;
        final String paymentId = dd.invoiceid;
        final Date paymentDate = OBDateUtils.getDate(dd.dateinvoiced);
        final BigDecimal creditLeft = new BigDecimal(dd.credit);

        HashMap<String, String> psData = insertData(documentNo, paymentId, paymentDate, creditLeft,
            strBusinessPartnerId, strBpName, CREDIT_SCOPE,
            StringUtils.equals(recOrPay, "RECEIVABLES") ? paymentInTab : paymentOutTab, dateFormat,
            true, BigDecimal.ZERO);
        hashMapList.add(psData);

        i++;
        if (i % 100 == 0) {
          log4j.debug("credit records processed: " + i);
        }
      }
      log4j.debug("Total credit records: " + i);
      log4j.debug("Time to process: " + (System.currentTimeMillis() - init));

      // Sorting and transform to field provider
      init = System.currentTimeMillis();
      Collections.sort(hashMapList, new Comparator<HashMap<String, String>>() {
        @Override
        public int compare(HashMap<String, String> a, HashMap<String, String> b) {
          int compare = a.get("BPARTNERNAME").compareToIgnoreCase(b.get("BPARTNERNAME"));
          if (compare == 0) {
            compare = a.get("BPARTNER").compareToIgnoreCase(b.get("BPARTNER"));
            if (compare == 0) {
              try {
                compare = OBDateUtils.getDate(a.get("INVOICE_DATE"))
                    .compareTo(OBDateUtils.getDate(b.get("INVOICE_DATE")));
                if (compare == 0) {
                  compare = a.get("INVOICE_NUMBER").compareToIgnoreCase(b.get("INVOICE_NUMBER"));
                }
              } catch (ParseException e) {
                compare = 0;
              }
            }
          }
          return compare;
        }
      });
      data = FieldProviderFactory.getFieldProviderArray(hashMapList);

      log4j
          .debug("Sorting and transform to Field Provider: " + (System.currentTimeMillis() - init));
    } catch (Exception e) {
      log4j.error("Error", e);
      throw new OBException("Error", e);
    } finally {
      if (dataSR != null) {
        dataSR.close();
      }
      if (dataCreditSR != null) {
        dataCreditSR.close();
      }
      OBContext.restorePreviousMode();
    }

    return data;
  }

  private HashMap<String, String> insertData(String documentNo, String id, Date date,
      BigDecimal amount, String bpartnerId, String bpartnerName, int group, String tabId,
      SimpleDateFormat dateFormat, boolean credits, BigDecimal doubtfulDebt) {
    HashMap<String, String> psData = new HashMap<String, String>();
    psData.put("INVOICE_NUMBER", documentNo);
    psData.put("INVOICE_ID", id);
    psData.put("INVOICE_DATE", dateFormat.format(date));
    psData.put("AMOUNT" + group, amount.compareTo(BigDecimal.ZERO) == 0 ? null : amount.toString());
    psData.put("DOUBTFUL_DEBT",
        doubtfulDebt.compareTo(BigDecimal.ZERO) == 0 ? null : doubtfulDebt.toString());
    BigDecimal percentage = calculatePercentage(amount.add(doubtfulDebt), doubtfulDebt);
    psData.put("PERCENTAGE",
        percentage.compareTo(BigDecimal.ZERO) == 0 ? null : percentage.toString());
    if (credits) {
      psData.put("SHOW_NETDUE", amount.add(doubtfulDebt).toString());
    } else {
      psData.put("NETDUE", amount.add(doubtfulDebt).toString());
      psData.put("SHOW_NETDUE", amount.add(doubtfulDebt).toString());
    }
    psData.put("BPARTNER", bpartnerId);
    psData.put("BPARTNERNAME", bpartnerName);
    psData.put("TABID", tabId);
    return psData;

  }

  /**
   * Returns the result date of subtracting the date range to the as Of Date field.
   */
  private Date convertToDate(Date currentDate, String strcolumn) {
    Calendar cal = new GregorianCalendar();
    cal.setTime(currentDate);
    cal.add(Calendar.DATE, -Integer.parseInt(strcolumn));
    return cal.getTime();
  }

  /**
   * This method transforms all the amounts into Strings for the report
   * 
   */
  private void setFieldsIntoFieldProvider(FieldProvider[] data, List<AgingData> agingBalanceData) {
    for (int i = 0; i < data.length; i++) {
      FieldProviderFactory.setField(data[i], "amount0",
          agingBalanceData.get(i).getcurrent().toString());
      FieldProviderFactory.setField(data[i], "amount1",
          agingBalanceData.get(i).getamount1().toString());
      FieldProviderFactory.setField(data[i], "amount2",
          agingBalanceData.get(i).getamount2().toString());
      FieldProviderFactory.setField(data[i], "amount3",
          agingBalanceData.get(i).getamount3().toString());
      FieldProviderFactory.setField(data[i], "amount4",
          agingBalanceData.get(i).getamount4().toString());
      FieldProviderFactory.setField(data[i], "amount5",
          agingBalanceData.get(i).getamount5().toString());
      FieldProviderFactory.setField(data[i], "Total",
          agingBalanceData.get(i).getTotal().toString());
      FieldProviderFactory.setField(data[i], "credit",
          agingBalanceData.get(i).getCredit().toString());
      FieldProviderFactory.setField(data[i], "net", agingBalanceData.get(i).getNet().toString());
      FieldProviderFactory.setField(data[i], "doubtfulDebt",
          agingBalanceData.get(i).getDoubtfulDebt().toString());
      FieldProviderFactory.setField(data[i], "percentage",
          agingBalanceData.get(i).getPercentage().toString());
      FieldProviderFactory.setField(data[i], "BPartner", agingBalanceData.get(i).getBPartner());
      FieldProviderFactory.setField(data[i], "BPartnerID", agingBalanceData.get(i).getBPartnerID());
    }
  }

  private BigDecimal calculatePercentage(BigDecimal totalAmount, BigDecimal doubtfulDebtAmount) {
    if (doubtfulDebtAmount.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return doubtfulDebtAmount.divide(totalAmount, 5, RoundingMode.HALF_UP)
        .multiply(new BigDecimal("100"));
  }

}
