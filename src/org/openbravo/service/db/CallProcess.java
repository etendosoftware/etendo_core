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
 * All portions are Copyright (C) 2009-2025 Openbravo SLU
 * All Rights Reserved.
 ************************************************************************
 */

package org.openbravo.service.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.process.Parameter;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;

/**
 * Service class to execute database stored procedures.
 * <p>
 * This class acts as the central engine for database calls in Openbravo.
 * It supports two modes of execution:
 * <ul>
 * <li><b>Standard Mode:</b> Executes processes defined in {@link Process} using the
 * {@link ProcessInstance} mechanism. This is the standard way to run processes from the UI.</li>
 * <li><b>Raw Mode:</b> Executes arbitrary stored procedures with a list of objects as parameters.
 * This replaces the logic previously found in CallStoredProcedure.</li>
 * </ul>
 * </p>
 * * @author mtaal
 * @see ProcessInstance
 * @see Process
 */
public class CallProcess {

  private static volatile CallProcess instance;

  /**
   * Protected constructor to prevent direct instantiation while still enabling subclass overrides
   * that can be registered through {@link #setInstance(CallProcess)} when customization is needed.
   */
  protected CallProcess() {
  }

  /**
   * Gets the singleton instance of CallProcess.
   * @return the CallProcess instance.
   */
  public static synchronized CallProcess getInstance() {
    if (instance == null) {
      instance = new CallProcess();
    }
    return instance;
  }

  /**
   * Sets the singleton instance of CallProcess, allowing platform extensions or tests to inject a
   * specialized subclass. Passing {@code null} is not allowed.
   * @param instance custom implementation replacing the default behavior.
   */
  public static synchronized void setInstance(CallProcess instance) {
    if (instance == null) {
      throw new IllegalArgumentException("CallProcess instance cannot be null");
    }
    CallProcess.instance = instance;
  }

  // ===========================================================================
  // STANDARD MODE: Process Instance Execution (UI / Background Processes)
  // ===========================================================================

  /**
   * Calls a process by its name.
   * @param processName
   * the procedure name defined in AD_Process.
   * @param recordID
   * the record ID associated with the execution (optional).
   * @param parameters
   * a map of parameters to be injected into AD_PInstance_Para.
   * @return the result ProcessInstance.
   */
  public ProcessInstance call(String processName, String recordID, Map<String, String> parameters) {
    return call(processName, recordID, parameters, null);
  }

  /**
   * Calls a process by its name with commit control.
   */
  public ProcessInstance call(String processName, String recordID, Map<String, String> parameters,
      Boolean doCommit) {
    final OBCriteria<Process> processCriteria = OBDal.getInstance().createCriteria(Process.class);
    processCriteria.add(Restrictions.eq(Process.PROPERTY_PROCEDURE, processName));

    if (processCriteria.list().size() > 1) {
      throw new OBException("More than one process found with procedure name " + processName);
    }
    final Process process = (Process) processCriteria.uniqueResult();

    if (process == null) {
      throw new OBException("No process found with procedure name " + processName);
    }
    return call(process, recordID, parameters, doCommit);
  }

  /**
   * Overloaded call without doCommit.
   * @param process
   * the process definition.
   * @param recordID
   * the record ID.
   * @param parameters
   * map of parameters.
   * @return the updated ProcessInstance with results.
   */
  public ProcessInstance call(Process process, String recordID, Map<String, String> parameters) {
    return callProcess(process, recordID, parameters, null);
  }

  /**
   * Calls a process using the ProcessInstance mechanism.
   * @param process
   * the process definition.
   * @param recordID
   * the record ID.
   * @param parameters
   * map of parameters.
   * @param doCommit
   * explicit commit flag (if supported by the SP).
   * @return the updated ProcessInstance with results.
   */
  public ProcessInstance call(Process process, String recordID, Map<String, String> parameters, Boolean doCommit) {
    return callProcess(process, recordID, parameters, doCommit);
  }

  /**
   * Calls a process using the ProcessInstance mechanism.
   * <p>
   * This method follows the template pattern to facilitate asynchronous extensions:
   * 1. {@link #createAndPersistInstance}: Prepares data.
   * 2. {@link #executeStandardProcedure}: Executes DB logic.
   * 3. Refreshes the result.
   * </p>
   * @param process
   * the process definition.
   * @param recordID
   * the record ID.
   * @param parameters
   * map of parameters.
   * @param doCommit
   * explicit commit flag (if supported by the SP).
   * @return the updated ProcessInstance with results.
   */
  public ProcessInstance callProcess(Process process, String recordID, Map<String, ?> parameters, Boolean doCommit) {
    OBContext.setAdminMode();
    try {
      // 1. Prepare Data
      ProcessInstance pInstance = createAndPersistInstance(process, recordID, parameters);

      // 2. Execute DB Logic
      executeStandardProcedure(pInstance, process, doCommit);

      // 3. Refresh result
      OBDal.getInstance().getSession().refresh(pInstance);

      return pInstance;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Overloaded callProcess without doCommit.
   *
   * @param process instance of the process to be executed
   * @param recordID the record ID.
   * @param parameters map of parameters.
   * @return the updated ProcessInstance with results.
   */
  public ProcessInstance callProcess(Process process, String recordID, Map<String, ?> parameters) {
    return callProcess(process, recordID, parameters, null);
  }
    // ===========================================================================
  // RAW MODE: Arbitrary SQL Execution (Replaces CallStoredProcedure)
  // ===========================================================================

  /**
   * Executes a raw stored procedure or function directly via JDBC.
   * * @param procedureName
   * the name of the database procedure/function.
   * @param parameters
   * list of parameter values.
   * @param types
   * list of parameter classes (for null handling).
   * @param doFlush
   * whether to flush Hibernate session before execution.
   * @param returnResults
   * whether to capture the return value (Function vs Procedure).
   * @return the result object if returnResults is true, null otherwise.
   */
  Object executeRaw(String procedureName, List<Object> parameters, List<Class<?>> types, boolean doFlush, boolean returnResults) {
    String rdbms = new DalConnectionProvider(false).getRDBMS();
    int paramCount = (parameters != null) ? parameters.size() : 0;

    // 1. Build Query
    String sql = buildSqlQuery(procedureName, paramCount, rdbms, returnResults);

    // 2. Obtain Connection
    Connection conn = OBDal.getInstance().getConnection(doFlush);

    // 3. Execute
    try (PreparedStatement ps = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

      setParameters(ps, parameters, types);

      if (returnResults) {
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            Object res = rs.getObject("RESULT");
            return rs.wasNull() ? null : res;
          }
          return null;
        }
      } else {
        ps.execute();
        return null;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Error executing raw process " + procedureName + ": " + e.getMessage(), e);
    }
  }

  // ===========================================================================
  // PROTECTED HELPERS (Extension Points for Async Implementation)
  // ===========================================================================

  /**
   * Creates the ProcessInstance and Parameter records and persists them to the database.
   */
  protected ProcessInstance createAndPersistInstance(Process process, String recordID, Map<String, ?> parameters) {
    final ProcessInstance pInstance = OBProvider.getInstance().get(ProcessInstance.class);
    pInstance.setProcess(process);
    pInstance.setActive(true);
    pInstance.setAllowRead(true);
    pInstance.setRecordID(recordID != null ? recordID : "0");
    pInstance.setUserContact(OBContext.getOBContext().getUser());

    if (parameters != null && !parameters.isEmpty()) {
      int index = 0;
      for (Map.Entry<String, ?> entry : parameters.entrySet()) {
        index++;
        final Parameter parameter = OBProvider.getInstance().get(Parameter.class);
        parameter.setSequenceNumber(String.valueOf(index));
        parameter.setParameterName(entry.getKey());

        Object value = entry.getValue();
        if (value instanceof String) {
          parameter.setString((String) value);
        } else if (value instanceof Date) {
          parameter.setProcessDate((Date) value);
        } else if (value instanceof BigDecimal) {
          parameter.setProcessNumber((BigDecimal) value);
        }

        pInstance.getADParameterList().add(parameter);
        parameter.setProcessInstance(pInstance);
      }
    }

    OBDal.getInstance().save(pInstance);
    OBDal.getInstance().flush();

    return pInstance;
  }

  /**
   * Executes the standard protocol for Stored Procedures (PInstance ID based).
   */
  protected void executeStandardProcedure(ProcessInstance pInstance, Process process, Boolean doCommit) {
    // Construct the parameter list expected by standard OB procedures
    List<Object> rawParams = new ArrayList<>();
    List<Class<?>> types = new ArrayList<>();

    rawParams.add(pInstance.getId());
    types.add(String.class);

    if (doCommit != null) {
      rawParams.add(doCommit);
      types.add(Boolean.class);
    }

    // Reuse the Raw execution engine
    // Standard calls usually don't return a result set (they update AD_PInstance table)
    // However, on Postgres they are SELECTs, so returnResults=true prevents syntax errors
    boolean isPostgre = "POSTGRE".equals(new DalConnectionProvider(false).getRDBMS());

    executeRaw(process.getProcedure(), rawParams, types, false, isPostgre);
  }

  // ===========================================================================
  // PRIVATE SQL HELPERS
  // ===========================================================================

  /**
   * Generates SQL string based on RDBMS syntax.
   */
  private String buildSqlQuery(String name, int paramCount, String rdbms, boolean returnResults) {
    StringBuilder sb = new StringBuilder();
    boolean isOracle = "ORACLE".equalsIgnoreCase(rdbms);

    if (isOracle && !returnResults) {
      sb.append("CALL ").append(name);
    } else {
      sb.append("SELECT ").append(name);
    }

    sb.append("(");
    for (int i = 0; i < paramCount; i++) {
      sb.append(i > 0 ? ",?" : "?");
    }
    sb.append(")");

    if (returnResults || !isOracle) {
      sb.append(" AS RESULT FROM DUAL");
    }
    return sb.toString();
  }

  /**
   * Binds parameters to the PreparedStatement.
   */
  private void setParameters(PreparedStatement ps, List<Object> parameters, List<Class<?>> types) throws SQLException {
    if (parameters == null || parameters.isEmpty()) return;

    int index = 0;
    for (Object parameter : parameters) {
      int sqlIndex = index + 1;
      if (parameter == null) {
        int sqlType = (types != null && index < types.size()) ? getSqlType(types.get(index)) : Types.VARCHAR;
        ps.setNull(sqlIndex, sqlType);
      } else {
        setParameterValue(ps, sqlIndex, parameter);
      }
      index++;
    }
  }

  /**
   * Sets individual parameter value with type conversion.
   */
  private void setParameterValue(PreparedStatement ps, int index, Object parameter) throws SQLException {
    if (parameter instanceof String && ((String) parameter).isEmpty()) {
      ps.setNull(index, Types.VARCHAR);
    } else if (parameter instanceof Boolean) {
      ps.setString(index, BooleanUtils.toBoolean((Boolean) parameter) ? "Y" : "N");
    } else if (parameter instanceof BaseOBObject) {
      ps.setString(index, (String) ((BaseOBObject) parameter).getId());
    } else if (parameter instanceof Timestamp) {
      ps.setTimestamp(index, (Timestamp) parameter);
    } else if (parameter instanceof Date) {
      ps.setDate(index, new java.sql.Date(((Date) parameter).getTime()));
    } else {
      ps.setObject(index, parameter);
    }
  }

  /**
   * Maps Java classes to SQL Types.
   */
  private int getSqlType(Class<?> clz) {
    if (clz == null) return Types.VARCHAR;
    if (clz == Boolean.class || clz == String.class || BaseOBObject.class.isAssignableFrom(clz)) return Types.VARCHAR;
    else if (Number.class.isAssignableFrom(clz)) return Types.NUMERIC;
    else if (clz == Timestamp.class) return Types.TIMESTAMP;
    else if (Date.class.isAssignableFrom(clz)) return Types.DATE;
    return Types.VARCHAR;
  }
}
