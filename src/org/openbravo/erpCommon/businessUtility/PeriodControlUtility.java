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
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.businessUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.PeriodControlLog;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DalConnectionProvider;

public class PeriodControlUtility {

  public static OBError openClosePeriod(PeriodControlLog periodControlLog) {
    OBError myMessage = new OBError();
    OBContext.setAdminMode(true);
    Process process = null;
    try {
      process = OBDal.getInstance().get(Process.class, "167");
    } finally {
      OBContext.restorePreviousMode();
    }

    Map<String, String> parameters = new HashMap<String, String>();
    final ProcessInstance pinstance = CallProcess.getInstance()
        .call(process, periodControlLog.getId(), parameters);
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      ConnectionProvider conn = new DalConnectionProvider(false);
      myMessage = Utility.getProcessInstanceMessage(conn, vars,
          PInstanceProcessData.select(conn, pinstance.getId()));
    } catch (ServletException e) {
      throw new OBException("Failure getting error message", e);
    }
    return myMessage;
  }

  public static OBError openClosePeriodControl(String periodControlId) {
    OBError myMessage = new OBError();
    OBContext.setAdminMode(true);
    Process process = null;
    try {
      process = OBDal.getInstance().get(Process.class, "168");
    } finally {
      OBContext.restorePreviousMode();
    }

    Map<String, String> parameters = new HashMap<String, String>();
    final ProcessInstance pinstance = CallProcess.getInstance()
        .call(process, periodControlId, parameters);
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      ConnectionProvider conn = new DalConnectionProvider(false);
      myMessage = Utility.getProcessInstanceMessage(conn, vars,
          PInstanceProcessData.select(conn, pinstance.getId()));
    } catch (ServletException e) {
      throw new OBException("Failure getting error message", e);
    }
    return myMessage;
  }

  public static List<Period> getOrderedPeriods(JSONArray periodIdJSON) throws JSONException {
    List<String> periodIds = parseJSON(periodIdJSON);
    OBCriteria<Period> obc = OBDal.getInstance().createCriteria(Period.class);
    obc.setFilterOnActive(false);
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnReadableOrganization(false);
    obc.add(Restrictions.in(Period.PROPERTY_ID, periodIds));
    obc.addOrder(Order.asc(Period.PROPERTY_STARTINGDATE));
    return obc.list();
  }

  public static List<String> parseJSON(JSONArray idJSON) throws JSONException {
    List<String> ids = new ArrayList<String>();
    for (int i = 0; i < idJSON.length(); i++) {
      ids.add(idJSON.getString(i));
    }
    return ids;

  }
}
