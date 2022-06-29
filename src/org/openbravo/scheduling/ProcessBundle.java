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
 * All portions are Copyright (C) 2008-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.scheduling;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.ad_process.JasperProcess;
import org.openbravo.erpCommon.ad_process.PinstanceProcedure;

/**
 * A ProcessBundle is a 'container' holding all the relevant information required to execute a
 * process in the Openbravo ERP system, including security/contextual details such as User, Client
 * and Organization, execution parameters, a logger (used by the Process Monitor) and process
 * implementation specifics. Integrating with Quartz, the ProcessBundle is stored in the
 * JobExecutionContext, which is used by each execution of a particular Job.
 * 
 * @author awolski
 * 
 */
public class ProcessBundle implements Serializable {

  static final long serialVersionUID = 1L;

  /**
   * String constant to retrieve the ProcessBundle from the Quartz JobExecutionContext
   */
  public static final String KEY = "org.openbravo.scheduling.ProcessBundle.KEY";

  /**
   * String constant key for the pinstance identifier.
   */
  public static final String PINSTANCE = "process.param.pinstance";

  /**
   * String constant for retrieval of the process' database connection.
   */
  public static final String CONNECTION = "process.param.connection";

  /**
   * String constant for retrieval of the applications' configuration paramters.
   */
  public static final String CONFIG_PARAMS = "process.param.configParams";

  /**
   * String constants for write and read a map used in serialization / deserialization
   */
  public static final String FIELD_PROCESS_ID = "processId";
  public static final String FIELD_PROCESS_REQUEST_ID = "processRequestId";
  public static final String FIELD_PROCESS_RUN_ID = "processRunId";
  public static final String FIELD_CLOSE_CONNECTION = "closeConnection";
  public static final String FIELD_IMPL = "impl";
  public static final String FIELD_PARAMS = "params";
  public static final String FIELD_PROCESS_CLASS = "processClass";
  public static final String FIELD_CONFIG = "config";
  public static final String FIELD_RESULT = "result";
  public static final String FIELD_CHANNEL = "channel";
  public static final String FIELD_GROUP_INFO = "groupInfo";
  public static final String FIELD_CONTEXT = "context";
  public static final String FIELD_SERIALIZED_LOG = "serializedLog";

  private String processId;

  private String processRequestId;

  private String processRunId;

  private boolean closeConnection;

  private String impl;

  private Map<String, Object> params;

  private Class<? extends Process> processClass;

  private ProcessContext context;

  private transient ConnectionProvider connection;

  private ConfigParameters config;

  private transient ProcessLogger logger;

  /**
   * This property is a helper needed when the object is serialized and unserialized. It should never must be called
   * out of this class
   */
  private String serializedLog;

  private Object result;

  private Channel channel;

  private GroupInfo groupInfo;

  private List<String> bannedClusterInstanceNames;

  static Logger log = LogManager.getLogger();

  /**
   * Creates a new ProcessBundle for the given processId and application security variables.
   * 
   * @param processId
   *          the process id
   * @param vars
   *          clients security/context variables
   */
  public ProcessBundle(String processId, VariablesSecureApp vars) {
    this(processId, vars, Channel.DIRECT, vars.getClient(), vars.getOrg(), true);
  }

  /**
   * Creates a new ProcessBundle object with the given parameters.
   * 
   * @param processId
   *          the process id
   * @param vars
   *          clients security/application context variables
   * @param channel
   *          the channel through which this process was scheduled/executed
   * @param client
   *          the client that scheduled/executed this process
   * @param organization
   *          the organization under which this process will run
   */
  public ProcessBundle(String processId, VariablesSecureApp vars, Channel channel, String client,
      String organization, boolean roleSecurity) {
    this.processId = processId;
    this.context = new ProcessContext(vars, client, organization, roleSecurity);
    this.channel = channel;
    this.closeConnection = true;
  }

  /**
   * Creates a new ProcessBundle object with the given parameters.
   * 
   * @param processId
   *          the process id
   * @param processRequestId
   *          the process request id (the id of the schedule configuration definition record)
   * @param vars
   *          clients security/application context variables
   * @param channel
   *          the channel through which this process was scheduled/executed
   * @param client
   *          the client that scheduled/executed this process
   * @param organization
   *          the organization under which this process will run
   */
  public ProcessBundle(String processId, String processRequestId, VariablesSecureApp vars,
      Channel channel, String client, String organization, boolean roleSecurity) {
    this(processId, vars, channel, client, organization, roleSecurity);
    this.processRequestId = processRequestId;
  }

  /**
   * Creates a new ProcessBundle object with the given parameters.
   * 
   * @param processId
   *          the process id
   * @param vars
   *          clients security/application context variables
   * @param channel
   *          the channel through which this process was scheduled/executed
   * @param client
   *          the client that scheduled/executed this process
   * @param organization
   *          the organization under which this process will run
   * @param roleSecurity
   *          is role security
   * @param groupInfo
   *          the information of the process group which the process belongs to
   */
  public ProcessBundle(String processId, VariablesSecureApp vars, Channel channel, String client,
      String organization, boolean roleSecurity, GroupInfo groupInfo) {
    this.processId = processId;
    this.context = new ProcessContext(vars, client, organization, roleSecurity);
    this.channel = channel;
    this.closeConnection = true;
    this.groupInfo = groupInfo;
  }

  /**
   * Creates a new ProcessBundle object from a map. This constructor is required by quartz when the the store is
   * of type JDBC
   *
   * @param map
   *          a map with each of the process properties
   */

  public ProcessBundle(Map<String, Object> map) {
    if (map.containsKey(FIELD_PROCESS_ID))
      processId = (String) map.get(FIELD_PROCESS_ID);
    if (map.containsKey(FIELD_PROCESS_REQUEST_ID))
      processRequestId = (String) map.get(FIELD_PROCESS_REQUEST_ID);
    if (map.containsKey(FIELD_PROCESS_RUN_ID))
      processRunId = (String) map.get(FIELD_PROCESS_RUN_ID);
    if (map.containsKey(FIELD_CONTEXT))
      context = (ProcessContext) map.get(FIELD_CONTEXT);
    if (map.containsKey(FIELD_CLOSE_CONNECTION))
      closeConnection = (boolean) map.get(FIELD_CLOSE_CONNECTION);
    if (map.containsKey(FIELD_IMPL))
      impl = (String) map.get(FIELD_IMPL);
    if (map.containsKey(FIELD_PARAMS))
      params = (Map<String, Object>) map.get(FIELD_PARAMS);
    if (map.containsKey(FIELD_PROCESS_CLASS))
      processClass = (Class<? extends Process>) map.get(FIELD_PROCESS_CLASS);
    if (map.containsKey(FIELD_CONFIG))
      config = (ConfigParameters) map.get(FIELD_CONFIG);
    if (map.containsKey(FIELD_RESULT))
      result = map.get(FIELD_RESULT);
    if (map.containsKey(FIELD_CHANNEL))
      channel = (Channel) map.get(FIELD_CHANNEL);
    if (map.containsKey(FIELD_GROUP_INFO))
      groupInfo = (GroupInfo) map.get(FIELD_GROUP_INFO);
    if (map.containsKey(FIELD_SERIALIZED_LOG))
      serializedLog = (String) map.get(FIELD_SERIALIZED_LOG);
  }

  /**
   * Returns the unique id for the schedule configuration of this process.
   * 
   * @return the process request's id (primary key within the ProcessRequest entity)
   */
  public String getProcessRequestId() {
    return this.processRequestId;
  }

  /**
   * Returns the id of the process run.
   * 
   * @return Returns the id of the process run
   */
  public String getProcessRunId() {
    return this.processRunId;
  }

  /**
   * Returns the unique id for the implementation of this process.
   * 
   * @return the process' id
   */
  public String getProcessId() {
    return processId;
  }

  /**
   * Returns, in the case that this process is an instance of pinstance procedure, the process'
   * pinstance id.
   * 
   * @return the pinstance id
   */
  public String getPinstanceId() {
    return (String) getParams().get(PINSTANCE);
  }

  /**
   * Returns the implementation of the process. For example, if the procedure referenced by
   * processId is a database procedure and has a procedure name associated, getImpl will return the
   * name of the procedure to call.
   * 
   * @return the implementation of the process
   */
  public String getImpl() {
    return impl;
  }

  /**
   * Returns the parameters for this process. This is guaranteed not to be null. For the moment only
   * parameters of type String or JSONObject are supported.
   * 
   * @return the process parameter map
   */
  public Map<String, Object> getParams() {
    if (params == null) {
      params = new HashMap<>();
    }
    return params;
  }

  /**
   * Returns a string representation of the process parameters in key=value pairs.
   * 
   * @return a deflated string representation of the process' parameter map
   * 
   * @deprecated Use instead {@link #getParamsDeflated()}
   */
  @Deprecated
  public String getParamsDefalated() {
    return this.getParamsDeflated();
  }

  public String getParamsDeflated() {
    return ParameterSerializer.getInstance().serialize(getParams());
  }

  /**
   * Returns true if the process is a group
   * 
   * @return true if the process is a group
   */
  public boolean isGroup() {
    return this.processId.equals(GroupInfo.processGroupId);
  }

  /**
   * Returns the info of the group
   * 
   * @return the info of the group
   */
  public GroupInfo getGroupInfo() {
    return this.groupInfo;
  }

  public List<String> getBannedClusterInstanceNames() {
    return bannedClusterInstanceNames;
  }

  public void setProcessRunId(String strProcessRunId) {
    this.processRunId = strProcessRunId;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params;
  }

  public Class<? extends Process> getProcessClass() {
    return processClass;
  }

  public void setProcessClass(Class<? extends Process> processClass) {
    this.processClass = processClass;
  }

  public ProcessContext getContext() {
    return context;
  }

  public ConnectionProvider getConnection() {
    // Force session info change so first time this ConnectionProvider is used, session audit info
    // is set in DB
    SessionInfo.infoChanged();
    return connection;
  }

  public void setConnection(ConnectionProvider connection) {
    this.connection = connection;
  }

  public ConfigParameters getConfig() {
    return config;
  }

  public void setConfig(ConfigParameters config) {
    this.config = config;
  }

  public ProcessLogger getLogger() {
    if(logger == null) {
      if(connection == null) {
        throw new OBException("You must set the connection before calling the logger");
      }
      logger = new ProcessLogger(connection);
      if(serializedLog != null) {
        logger.setLog(serializedLog);
      }
    }
    return logger;
  }

  public void setLog(ProcessLogger logger) {
    this.logger = logger;
  }

  public String getLog() {
    if (logger == null) {
      return serializedLog;
    } else {
      return logger.getLog();
    }
  }

  public Object getResult() {
    return result;
  }

  public void setResult(Object result) {
    this.result = result;
  }

  public Channel getChannel() {
    return channel;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public boolean getCloseConnection() {
    return closeConnection;
  }

  public void setCloseConnection(boolean closeConnection) {
    this.closeConnection = closeConnection;
  }

  /**
   * Initializes the ProcessBundle, setting its implementation, parameters and actual Java class
   * implementation that will be executed by the Quartz Scheduler or a direct ProcessRunner
   * 
   * @param conn
   *          a connection to the database
   * @return the initialized ProcessBundle
   * @throws ServletException
   *           if there is an error initializing the bundle
   */
  public ProcessBundle init(ConnectionProvider conn) throws ServletException {
    if (processId == null) {
      throw new ServletException("Process Id cannot be null");
    }
    final ProcessData data = ProcessData.select(conn, processId);

    if (data.classname != null && !data.classname.equals("")) {
      try {
        setProcessClass(Class.forName(data.classname).asSubclass(Process.class));
      } catch (final ClassNotFoundException e) {
        log.error(e.getMessage(), e);
        throw new ServletException(e.getMessage(), e);
      }
    } else if (data.isjasper != null && data.isjasper.equals("Y")) {
      this.impl = data.procedurename;
      this.processClass = JasperProcess.class;

    } else if (data.procedurename != null && !data.procedurename.equals("")) {
      this.impl = data.procedurename;
      this.processClass = PinstanceProcedure.class;
    }
    // TODO Load parameters - not required as we're still using pinstanceId
    setParams(new HashMap<String, Object>());
    setConnection(conn);
    setLog(new ProcessLogger(conn));

    setBannedClusterInstances();

    return this;
  }

  private void setBannedClusterInstances() {
    ClusterInstanceProcessAccess clusterAccess = new ClusterInstanceProcessAccess();
    bannedClusterInstanceNames = clusterAccess.getBannedClusterInstanceNames(processId);
  }

  /**
   * Utility method to create a new Process bundle from the details of in AD_PINSTANCE for the given
   * pinstanceId
   * 
   * @param pinstanceId
   *          Pinstance Id.
   * @param vars
   *          VariablesSecureApp to be converted into the ProcessContext
   * @param conn
   *          ConnectionProvider
   * @return a new instance of this class created from the pInstance record in the db
   * @throws ServletException
   */
  public static final ProcessBundle pinstance(String pinstanceId, VariablesSecureApp vars,
      ConnectionProvider conn) throws ServletException {
    final String processId = PinstanceData.select(conn, pinstanceId).adProcessId;

    final ProcessBundle bundle = new ProcessBundle(processId, vars).init(conn);
    bundle.setProcessClass(PinstanceProcedure.class);
    bundle.getParams().put(PINSTANCE, pinstanceId);

    return bundle;
  }

  /**
   * Constructions a ProcessBundle object from a scheduled request id. This is normally called from
   * the Process Scheduler window.
   * 
   * @param requestId
   *          the request id
   * @param vars
   *          the application context/security variables
   * @param conn
   *          a connection to the database
   * @return a new ProcessBundle object based on the information in the request
   * @throws ServletException
   *           if an error occurs retrieving the request from the database
   */
  public static final ProcessBundle request(String requestId, VariablesSecureApp vars,
      ConnectionProvider conn) throws ServletException {
    final ProcessRequestData data = ProcessRequestData.select(conn, requestId);
    String processId = "";

    if (data.isgroup.equals("Y")) {
      processId = GroupInfo.processGroupId;
    } else {
      processId = data.processId;
    }

    final boolean isRoleSecurity = data.isrolesecurity != null && data.isrolesecurity.equals("Y");
    final ProcessBundle bundle = new ProcessBundle(processId, requestId, vars, Channel.SCHEDULED,
        data.client, data.organization, isRoleSecurity).init(conn);

    final String paramString = data.params;
    if (paramString == null || paramString.trim().equals("")) {
      bundle.setParams(new HashMap<String, Object>());
    } else {
      bundle.setParams(ParameterSerializer.getInstance().deserialize(paramString));
    }

    return bundle;
  }

  /**
   * Method which returns a map with the serializable properties. This mehtod must exclude the logger and
   * the connection, both of which are setted when the process is runs
   * @return a map with the values of the current processBundle
   */
  public Map<String, Object> getMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(FIELD_PROCESS_ID, processId);
    map.put(FIELD_PROCESS_REQUEST_ID, processRequestId);
    map.put(FIELD_PROCESS_RUN_ID, processRunId);
    map.put(FIELD_CLOSE_CONNECTION, closeConnection);
    map.put(FIELD_IMPL, impl);
    map.put(FIELD_PARAMS, params);
    map.put(FIELD_PROCESS_CLASS, processClass);
    map.put(FIELD_CONFIG, config);
    map.put(FIELD_RESULT, result);
    map.put(FIELD_CHANNEL, channel);
    map.put(FIELD_GROUP_INFO, groupInfo);
    map.put(FIELD_CONTEXT, context);
    map.put(FIELD_SERIALIZED_LOG, getLogger().getLog());
    return map;
  }

  /**
   * Static method which create a new {@link ProcessBundle} object from a map
   * @param map
   * @return
   */
  public static ProcessBundle mapToObject(Map<String, Object> map) {
    if(map != null) {
      return new ProcessBundle(map);
    }
    return null;
  }

  /**
   * @author awolski
   * 
   */
  public enum Channel {
    DIRECT {
      @Override
      public String toString() {
        return "Direct";
      }
    },
    BACKGROUND {
      @Override
      public String toString() {
        return "Background";
      }
    },
    SCHEDULED {
      @Override
      public String toString() {
        return "Process Scheduler";
      }
    },
    WEBSERVICE {
      @Override
      public String toString() {
        return "Webservice";
      }
    },
  }

  public void applyLog() {
    this.serializedLog = getLogger().getLog();
  }
}
