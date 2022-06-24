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
 * All portions are Copyright (C) 2016-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.model.domaintype.DateDomainType;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.application.report.BaseReportActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.JRFieldProviderDataSource;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import net.sf.jasperreports.engine.JRDataSource;

/**
 * Cashflow Forecast Action Handler for the Process Definition
 */
public class CashflowForecastReportActionHandler extends BaseReportActionHandler {

  private static final String BREAK_BY_DATE2 = "breakByDate";
  private static final String _PARAMS = "_params";
  private static final String DATE_FORMAT_JAVA = "dateFormat.java";
  private static final String DATE_PLANNED2 = "datePlanned";
  private static final String BREAK_BY_DATE = "BreakByDate";
  private static final String DATE_PLANNED = "DatePlanned";
  private static final String FINANCIAL_ACCOUNT_ID = "Fin_Financial_Account_ID";
  private static final String PARAM_PROVIDER_SUB_REPORT = "fieldProviderSubReport";
  private static final String PARAM_PROVIDER_SUMMARY = "fieldProviderSummary";
  private static final String PARAM_FORMAT = "OUTPUT_FORMAT";
  private static final String XLS_FORMAT = "XLS";

  @Override
  protected ConnectionProvider getReportConnectionProvider() {
    return DalConnectionProvider.getReadOnlyConnectionProvider();
  }

  @Override
  protected void addAdditionalParameters(ReportDefinition process, JSONObject jsonContent,
      Map<String, Object> parameters) {

    CashflowForecastData[][] data = null;
    Map<String, Object> dataResult = new HashMap<String, Object>();
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    ConnectionProvider conn = new DalConnectionProvider(false);

    try {
      // Get parameters
      JSONObject params = jsonContent.getJSONObject(_PARAMS);
      DateDomainType dateDomainType = new DateDomainType();
      Date datePlanned = (Date) dateDomainType.createFromString(params.getString(DATE_PLANNED2));
      String strDatePlanned = DateFormatUtils.format(datePlanned,
          OBPropertiesProvider.getInstance()
              .getOpenbravoProperties()
              .getProperty(DATE_FORMAT_JAVA));
      String strFinancialAccountId = params.getString(FINANCIAL_ACCOUNT_ID).equals("null") ? ""
          : params.getString(FINANCIAL_ACCOUNT_ID);
      boolean breakByDate = params.getBoolean(BREAK_BY_DATE2);

      CashflowForecastData[] dataSummary = obtainSummaryFieldProvider(strFinancialAccountId,
          strDatePlanned, vars, conn);
      dataResult = obtainLinesFieldProvider(dataSummary, strDatePlanned, breakByDate, vars, conn);

      dataSummary = (CashflowForecastData[]) dataResult.get("dataSummary");
      data = (CashflowForecastData[][]) dataResult.get("data");

      parameters.clear();
      parameters.put(BREAK_BY_DATE, breakByDate);
      parameters.put(DATE_PLANNED, datePlanned);
      parameters.put(FINANCIAL_ACCOUNT_ID, strFinancialAccountId);
      parameters.put(PARAM_PROVIDER_SUB_REPORT,
          new JRFieldProviderDataSource(unifyData(data), vars.getJavaDateFormat()));
      parameters.put(PARAM_PROVIDER_SUMMARY,
          new JRFieldProviderDataSource(dataSummary, vars.getJavaDateFormat()));
      parameters.put(PARAM_FORMAT, jsonContent.getString(ApplicationConstants.BUTTON_VALUE));
    } catch (Exception e) {
    }
  }

  @Override
  protected JRDataSource getReportData(Map<String, Object> parameters) {
    @SuppressWarnings("unchecked")
    HashMap<String, Object> jrParams = (HashMap<String, Object>) parameters
        .get(JASPER_REPORT_PARAMETERS);
    if (StringUtils.equals((String) jrParams.get(PARAM_FORMAT), XLS_FORMAT)) {
      // Pass data only for XLS
      return (JRFieldProviderDataSource) jrParams.get(PARAM_PROVIDER_SUB_REPORT);
    } else {
      // PDF and HTML don't require data, as it's passed as parameter to the subreports
      return null;
    }
  }

  private Map<String, Object> obtainLinesFieldProvider(CashflowForecastData[] dataSummary,
      String strDatePlanned, boolean breakByDate, VariablesSecureApp vars, ConnectionProvider conn)
      throws ServletException {
    Vector<CashflowForecastData[]> vDatas = new Vector<>();
    Vector<Object> vHeader = new Vector<Object>();
    CashflowForecastData[] dataDetail = null;
    CashflowForecastData[][] data = null;
    Map<String, Object> dataResult = new HashMap<String, Object>();

    // For each financial account get the details of all the movements
    for (int j = 0; j < dataSummary.length; j++) {
      dataDetail = CashflowForecastData.selectLines(conn, vars.getLanguage(),
          dataSummary[j].finFinancialAccountId, strDatePlanned,
          breakByDate ? ", duedate asc, isreceipt desc" : ", isreceipt desc, duedate asc");
      BigDecimal payment = BigDecimal.ZERO;
      BigDecimal income = BigDecimal.ZERO;

      BigDecimal acum = new BigDecimal(dataSummary[j].initialbalance);
      // Summary information is retrieved
      for (int i = 0; i < dataDetail.length; i++) {
        dataDetail[i].initialbalance = dataSummary[j].initialbalance;
        dataDetail[i].currentbalance = dataSummary[j].initialbalance;
        acum = acum.add(new BigDecimal(dataDetail[i].convertedamount));
        if ("Y".equals(dataDetail[i].isreceipt)) {
          income = income.add(new BigDecimal(dataDetail[i].convertedamount));
        } else {
          payment = payment.subtract(new BigDecimal(dataDetail[i].convertedamount));
        }
      }
      dataSummary[j].income = income.toString();
      dataSummary[j].payment = payment.toString();
      dataSummary[j].finalsummary = acum.toString();
      if (dataDetail.length > 0) {
        vDatas.addElement(dataDetail);
        vHeader.addElement(dataSummary[j]);
      }
    }
    data = new CashflowForecastData[vDatas.size()][];
    vDatas.copyInto(data);

    dataResult.put("data", data);
    dataResult.put("dataSummary", dataSummary);

    return dataResult;
  }

  private CashflowForecastData[] obtainSummaryFieldProvider(String strFinancialAccount,
      String strDatePlanned, VariablesSecureApp vars, ConnectionProvider conn)
      throws ServletException {
    return CashflowForecastData.selectSummary(conn, vars.getLanguage(),
        Utility.getContext(conn, vars, "#User_Client", "CashflowForecast"),
        Utility.getContext(conn, vars, "#AccessibleOrgTree", "CashflowForecast"),
        strFinancialAccount, strDatePlanned);
  }

  private FieldProvider[] unifyData(CashflowForecastData[][] dataToUnify) {
    int length = 0;
    for (int i = 0; i < dataToUnify.length; i++) {
      length += dataToUnify[i].length;
    }
    FieldProvider[] result = new FieldProvider[length];
    int index = 0;
    for (int i = 0; i < dataToUnify.length; i++) {
      for (int j = 0; j < dataToUnify[i].length; j++) {
        result[index++] = dataToUnify[i][j];
      }
    }
    return result;
  }

}
