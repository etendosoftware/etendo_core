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
 * All portions are Copyright (C) 2009-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 * Modification july 2010 (c) openbravo SLU, based on contribution made by iferca
 ************************************************************************
 */

package org.openbravo.service.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;


import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.process.Parameter;
import org.openbravo.model.ad.process.ProcessInstance;

/**
 * This class is a service class to call a stored procedure using a set of parameters.
 * 
 * The {@link ProcessInstance} result is returned.
 * 
 * @see ProcessInstance
 * @see org.openbravo.model.ad.ui.Process
 * @see Parameter
 * 
 * @author mtaal
 */
public class CallProcess {

  private static CallProcess instance = new CallProcess();

  public static synchronized CallProcess getInstance() {
    return instance;
  }

  public static synchronized void setInstance(CallProcess instance) {
    CallProcess.instance = instance;
  }

  /**
   * Calls a process with the specified name. The recordID and parameters can be null. Parameters
   * are translated into {@link Parameter} instances.
   * 
   * @param processName
   *          the name of the stored procedure, must exist in the database, see
   *          {@link org.openbravo.model.ad.ui.Process#getProcedure()}.
   * @param recordID
   *          the recordID will be set in the {@link ProcessInstance}, see
   *          {@link ProcessInstance#getRecordID()}
   * @param parameters
   *          are translated into process parameters
   * @param doCommit
   *          do commit at the end of the procedure only if calledFromApp functionality is supported
   *          in the procedure, otherwise null should be passed
   * @return the created instance with the result ({@link ProcessInstance#getResult()}) or error (
   *         {@link ProcessInstance#getErrorMsg()})
   */
  public ProcessInstance call(String processName, String recordID, Map<String, String> parameters,
      Boolean doCommit) {
    final OBCriteria<org.openbravo.model.ad.ui.Process> processCriteria = OBDal.getInstance()
        .createCriteria(org.openbravo.model.ad.ui.Process.class);
    processCriteria
        .addEqual(org.openbravo.model.ad.ui.Process.PROPERTY_PROCEDURE, processName);
    List<org.openbravo.model.ad.ui.Process> processList = processCriteria.list();
    if (processList.size() != 1) {
      throw new OBException(
          "No process or more than one process found using procedurename " + processName);

    }
    return call(processList.get(0), recordID, parameters, doCommit);
  }

  /**
   * Calls a process. The recordID and parameters can be null. Parameters are translated into
   * {@link Parameter} instances.
   * 
   * @param process
   *          the process to execute
   * @param recordID
   *          the recordID will be set in the {@link ProcessInstance}, see
   *          {@link ProcessInstance#getRecordID()}
   * @param parameters
   *          are translated into process parameters, supports only string parameters, for support
   *          of other parameters see the next method:
   *          {@link #callProcess(org.openbravo.model.ad.ui.Process, String, Map)}
   * @param doCommit
   *          do commit at the end of the procedure only if calledFromApp functionality is supported
   *          in the procedure, otherwise null should be passed
   * @return the created instance with the result ({@link ProcessInstance#getResult()}) or error (
   *         {@link ProcessInstance#getErrorMsg()})
   */
  public ProcessInstance call(org.openbravo.model.ad.ui.Process process, String recordID,
      Map<String, String> parameters, Boolean doCommit) {
    return callProcess(process, recordID, parameters, doCommit);
  }

  /**
   * Calls a process. The recordID and parameters can be null. Parameters are translated into
   * {@link Parameter} instances.
   * 
   * @param process
   *          the process to execute
   * @param recordID
   *          the recordID will be set in the {@link ProcessInstance}, see
   *          {@link ProcessInstance#getRecordID()}
   * @param parameters
   *          are translated into process parameters
   * @param doCommit
   *          do commit at the end of the procedure only if calledFromApp functionality is supported
   *          in the procedure, otherwise null should be passed
   * @return the created instance with the result ({@link ProcessInstance#getResult()}) or error (
   *         {@link ProcessInstance#getErrorMsg()})
   */
  public ProcessInstance callProcess(org.openbravo.model.ad.ui.Process process, String recordID,
      Map<String, ?> parameters, Boolean doCommit) {
    OBContext.setAdminMode();
    try {
      // Create the pInstance
      final ProcessInstance pInstance = OBProvider.getInstance().get(ProcessInstance.class);
      // sets its process
      pInstance.setProcess(process);
      // must be set to true
      pInstance.setActive(true);

      // allow it to be read by others also
      pInstance.setAllowRead(true);

      if (recordID != null) {
        pInstance.setRecordID(recordID);
      } else {
        pInstance.setRecordID("0");
      }

      // get the user from the context
      pInstance.setUserContact(OBContext.getOBContext().getUser());

      // now create the parameters and set their values
      if (parameters != null) {
        int index = 0;
        for (String key : parameters.keySet()) {
          index++;
          final Object value = parameters.get(key);
          final Parameter parameter = OBProvider.getInstance().get(Parameter.class);
          parameter.setSequenceNumber(index + "");
          parameter.setParameterName(key);
          if (value instanceof String) {
            parameter.setString((String) value);
          } else if (value instanceof Date) {
            parameter.setProcessDate((Date) value);
          } else if (value instanceof BigDecimal) {
            parameter.setProcessNumber((BigDecimal) value);
          }

          // set both sides of the bidirectional association
          pInstance.getADParameterList().add(parameter);
          parameter.setProcessInstance(pInstance);
        }
      }

      // persist to the db
      OBDal.getInstance().save(pInstance);

      // flush, this gives pInstance an ID
      OBDal.getInstance().flush();

      PreparedStatement ps = null;
      // call the SP
      try {
        // first get a connection
        final Connection connection = OBDal.getInstance().getConnection(false);

        String procedureParameters = "(?)";
        if (doCommit != null) {
          procedureParameters = "(?,?)";
        }

        final Properties obProps = OBPropertiesProvider.getInstance().getOpenbravoProperties();
        if (obProps.getProperty("bbdd.rdbms") != null
            && obProps.getProperty("bbdd.rdbms").equals("POSTGRE")) {
          ps = connection
              .prepareStatement("SELECT * FROM " + process.getProcedure() + procedureParameters);
        } else {
          ps = connection.prepareStatement(" CALL " + process.getProcedure() + procedureParameters);
        }

        ps.setString(1, pInstance.getId());
        if (doCommit != null) {
          ps.setString(2, doCommit ? "Y" : "N");
        }
        ps.execute();
      } catch (Exception e) {
        throw new IllegalStateException(e);
      } finally {
        try {
          ps.close();
        } catch (SQLException e) {
        }
      }

      // refresh the pInstance as the SP has changed it
      OBDal.getInstance().getSession().refresh(pInstance);
      return pInstance;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Calls a process with the specified name. The recordID and parameters can be null. Parameters
   * are translated into {@link Parameter} instances.
   * 
   * @param processName
   *          the name of the stored procedure, must exist in the database, see
   *          {@link org.openbravo.model.ad.ui.Process#getProcedure()}.
   * @param recordID
   *          the recordID will be set in the {@link ProcessInstance}, see
   *          {@link ProcessInstance#getRecordID()}
   * @param parameters
   *          are translated into process parameters
   * @return the created instance with the result ({@link ProcessInstance#getResult()}) or error (
   *         {@link ProcessInstance#getErrorMsg()})
   */
  public ProcessInstance call(String processName, String recordID, Map<String, String> parameters) {
    return call(processName, recordID, parameters, null);
  }

  /**
   * Calls a process. The recordID and parameters can be null. Parameters are translated into
   * {@link Parameter} instances.
   * 
   * @param process
   *          the process to execute
   * @param recordID
   *          the recordID will be set in the {@link ProcessInstance}, see
   *          {@link ProcessInstance#getRecordID()}
   * @param parameters
   *          are translated into process parameters, supports only string parameters, for support
   *          of other parameters see the next method:
   *          {@link #callProcess(org.openbravo.model.ad.ui.Process, String, Map)}
   * @return the created instance with the result ({@link ProcessInstance#getResult()}) or error (
   *         {@link ProcessInstance#getErrorMsg()})
   */
  public ProcessInstance call(org.openbravo.model.ad.ui.Process process, String recordID,
      Map<String, String> parameters) {
    return call(process, recordID, parameters, null);
  }

  /**
   * Calls a process. The recordID and parameters can be null. Parameters are translated into
   * {@link Parameter} instances.
   * 
   * @param process
   *          the process to execute
   * @param recordID
   *          the recordID will be set in the {@link ProcessInstance}, see
   *          {@link ProcessInstance#getRecordID()}
   * @param parameters
   *          are translated into process parameters
   * @return the created instance with the result ({@link ProcessInstance#getResult()}) or error (
   *         {@link ProcessInstance#getErrorMsg()})
   */
  public ProcessInstance callProcess(org.openbravo.model.ad.ui.Process process, String recordID,
      Map<String, ?> parameters) {
    return callProcess(process, recordID, parameters, null);
  }
}
