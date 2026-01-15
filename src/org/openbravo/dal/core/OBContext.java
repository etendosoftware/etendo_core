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
 * All portions are Copyright (C) 2008-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.provider.OBNotSingleton;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.Check;
import org.openbravo.dal.security.AcctSchemaStructureProvider;
import org.openbravo.dal.security.EntityAccessChecker;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;

import jakarta.enterprise.context.Dependent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Models the context in which Data Access Layer actions are executed. Contains the user, the client
 * and the allowed organizations.
 * 
 * This class contains specific logic to compute the allowed organizations and clients for both read
 * and write access.
 * 
 * The OBContext instance is made available to other threads through the static ThreadLocal and the
 * getInstance method.
 * 
 * The OBContext can be serialized as part of the Tomcat persistent session mechanism.
 * 
 * @author mtaal
 */

// Note the getInstance/setInstance and ThreadLocal pattern should be reviewed
// when using a factory/dependency injection approach.
@Dependent
public class OBContext implements OBNotSingleton, Serializable {
  private static final long serialVersionUID = 1L;
  private static final Logger log = LogManager.getLogger();

  public static final String CONTEXT_PARAM = "#OBContext";

  private String userID;
  private String roleID;
  private String clientID;
  private String orgID;
  private String warehouseID;
  private String langID;

  private transient Client currentClient;
  private transient Organization currentOrganization;
  private transient Role role;
  private transient User user;
  private transient Language language;
  private transient boolean translationInstalled;
  private transient Warehouse warehouse;
  private transient List<String> organizationList;
  private transient List<String> deactivatedOrganizationList;
  private transient String[] readableOrganizations;
  private transient String[] readableClients;
  private transient Set<String> writableOrganizations;
  private transient Set<String> deactivatedOrganizations;
  private transient String userLevel;
  private transient Map<String, OrganizationStructureProvider> organizationStructureProviderByClient;
  private transient Map<String, AcctSchemaStructureProvider> acctSchemaStructureProviderByClient;
  private transient EntityAccessChecker entityAccessChecker;
  private transient RecordAccessChecker recordAccessChecker;
  private transient boolean isAdministrator;
  private transient boolean isInitialized = false;
  private transient boolean isRTL = false;
  private transient boolean isPortalRole = false;
  private transient boolean isWebServiceEnabled = false;
  private transient Set<String> additionalWritableOrganizations;
  private transient boolean newUI = false;
  private transient boolean checkAccessLevel = true;

  // set this to a higher value to enable admin mode tracing
  private static int ADMIN_TRACE_SIZE = 0;

  private static ThreadLocal<OBContext> instance = new ThreadLocal<>();
  private static ThreadLocal<OBContext> adminModeSet = new ThreadLocal<>();
  private static ThreadLocal<Stack<OBAdminMode>> adminModeStack = new ThreadLocal<>();
  private static ThreadLocal<List<String>> adminModeTrace = new ThreadLocal<>();
  private static ThreadLocal<Stack<OBAdminMode>> crossOrgAdminModeStack = new ThreadLocal<>();
  private static ThreadLocal<List<String>> crossOrgAdminModeTrace = new ThreadLocal<>();

  private static OBContext adminContext = null;

  private static final String AUTHENTICATED_USER = "#AD_User_ID";
  private static final String ROLE = "#AD_Role_ID";
  private static final String CLIENT = "#AD_Client_ID";
  private static final String ORG = "#AD_Org_ID";

  private enum AdminType {
    ADMIN_MODE("setAdminMode", "restorePreviousMode", adminModeStack, adminModeTrace), //
    CROSS_ORG_ADMIN_MODE("setCrossOrgReferenceAdminMode", "restorePreviousCrossOrgReferenceMode",
        crossOrgAdminModeStack, crossOrgAdminModeTrace);

    private String setMethod;
    private String restoreMethod;
    private ThreadLocal<Stack<OBAdminMode>> stack;
    private ThreadLocal<List<String>> trace;

    private AdminType(String setMethod, String restoreMethod, ThreadLocal<Stack<OBAdminMode>> stack,
        ThreadLocal<List<String>> trace) {
      this.setMethod = setMethod;
      this.restoreMethod = restoreMethod;
      this.stack = stack;
      this.trace = trace;
    }
  };

  /**
   * @return true if the current language is a RTL language, false in other cases
   */
  public static boolean isRightToLeft() {
    if (getOBContext() != null) {
      return getOBContext().isRTL();
    }
    return false;
  }

  public static boolean hasTranslationInstalled() {
    if (getOBContext() != null) {
      return getOBContext().isTranslationInstalled();
    }
    return false;
  }

  /**
   * @deprecated use {@link #setAdminMode()}
   */
  @Deprecated
  public static void setAdminContext() {
    if (adminContext == null) {
      setOBContext("0", "0", "0", "0");
      adminContext = getOBContext();
    } else {
      setOBContext(adminContext);
    }
  }

  private static void setAdminContextLocally() {
    if (adminContext == null) {
      setOBContext("0", "0", "0", "0");
      adminContext = getOBContext();
    } else {
      setOBContext(adminContext);
    }
  }

  /**
   * @deprecated use {@link #setAdminMode()}
   */
  @Deprecated
  public static void enableAsAdminContext() {
    setAdminMode();
  }

  /**
   * Let's the current user run with Administrator privileges. If there is no current user then the
   * special Administrator context is used.
   * 
   * To restore the previous privileges call the {@link #restorePreviousMode()}.
   * 
   * @param doOrgClientAccessCheck
   *          Whether entity access (client+org) should also be checked
   * @see OBContext#restorePreviousMode()
   * @since 2.50MP18
   */
  public static void setAdminMode(boolean doOrgClientAccessCheck) {
    OBAdminMode am = new OBAdminMode();
    am.setAdminMode(true);
    am.setOrgClientAccessCheck(doOrgClientAccessCheck);
    getAdminModeStack(AdminType.ADMIN_MODE).push(am);
    if (OBContext.getOBContext() == null) {
      OBContext.setAdminContextLocally();
    } else if (OBContext.getOBContext() == adminContext) {
      return;
    }
    if (OBContext.getOBContext() != null && ADMIN_TRACE_SIZE != 0) {
      addStackTrace(AdminType.ADMIN_MODE);
    }
  }

  /**
   * Let's the current user run with Administrator privileges. If there is no current user then the
   * special Administrator context is used.
   * 
   * To restore the previous privileges call the {@link #restorePreviousMode()}.
   * 
   * If this method is used, entity access will also be checked. If you don't want entity access to
   * be checked, you should use {@link #setAdminMode(boolean checkEntityAccess)}
   * 
   * @see OBContext#restorePreviousMode()
   * @since 2.50MP18
   */
  public static void setAdminMode() {
    setAdminMode(false);
  }

  /**
   * Cross Organization Reference Administrator mode allows to create references from an object to
   * another one that it is not in the same Organization's natural tree.
   * 
   * This only applies for columns marked to allow this behavior.
   */
  public static void setCrossOrgReferenceAdminMode() {
    OBAdminMode am = new OBAdminMode();
    am.setAdminMode(false);
    am.setOrgClientAccessCheck(true);
    am.setCrossOrgAdminMode(true);

    getAdminModeStack(AdminType.CROSS_ORG_ADMIN_MODE).push(am);

    if (OBContext.getOBContext() != null && ADMIN_TRACE_SIZE != 0) {
      addStackTrace(AdminType.CROSS_ORG_ADMIN_MODE);
    }
  }

  private static Stack<OBAdminMode> getAdminModeStack(AdminType type) {
    ThreadLocal<Stack<OBAdminMode>> stack = type.stack;
    if (stack.get() == null) {
      stack.set(new Stack<OBAdminMode>());
    }
    return stack.get();
  }

  /**
   * @deprecated use {@link #restorePreviousMode()}
   */
  @Deprecated
  public static void resetAsAdminContext() {
    restorePreviousMode();
  }

  /**
   * Is used to restore the previous privileges after enabling Administrator privileges by calling
   * {@link #setAdminMode()}.
   * 
   * @see OBContext#setAdminMode()
   * @since 2.50MP18
   */
  public static void restorePreviousMode() {
    restorePreviousMode(AdminType.ADMIN_MODE);
    Stack<OBAdminMode> stack = getAdminModeStack(AdminType.ADMIN_MODE);

    if (stack.isEmpty() && OBContext.getOBContext() == adminContext) {
      OBContext.setOBContext((OBContext) null);
    }
  }

  /**
   * Used to restore the previous privileges after enabling Cross Organization Reference
   * Administrator privileges by calling {@link #setCrossOrgReferenceAdminMode()}.
   * 
   * @see OBContext#setCrossOrgReferenceAdminMode()
   */
  public static void restorePreviousCrossOrgReferenceMode() {
    restorePreviousMode(AdminType.CROSS_ORG_ADMIN_MODE);
  }

  private static void restorePreviousMode(AdminType type) {
    // remove the last admin mode from the stack
    final Stack<OBAdminMode> stack = getAdminModeStack(type);
    if (stack.size() > 0) {
      stack.pop();
    } else {
      printUnbalancedWarning(true, type);
    }

    if (OBContext.getOBContext() != null && ADMIN_TRACE_SIZE != 0) {
      addStackTrace(type);
    }
  }

  /**
   * Sets the size of stack trace to display in case of logging unbalanced set/restore admin modes
   * calls
   */
  public static void setAdminTraceSize(int s) {
    ADMIN_TRACE_SIZE = s;
  }

  private static void printUnbalancedWarning(boolean printLocationOfCaller, AdminType type) {
    if (ADMIN_TRACE_SIZE == 0) {
      String errMsg = "Unbalanced calls to " + type.setMethod + " and " + type.restoreMethod + ". "
          + "Consider setting the constant OBContext.ADMIN_TRACE_SIZE to a value higher than 0 to debug this situation";
      if (printLocationOfCaller) {
        log.warn(errMsg, new IllegalStateException());
      } else {
        log.warn(errMsg);
      }
      return;
    }

    // will only be executed with adminModeTrace debugging enabled
    List<String> adminModeTraceList = type.trace.get();

    final StringBuilder sb = new StringBuilder();
    if (adminModeTraceList != null) {
      for (String adminModeTraceValue : adminModeTraceList) {
        sb.append("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
        sb.append(adminModeTraceValue);
      }
    }
    if (printLocationOfCaller) {
      log.warn(
          "Unbalanced calls to " + type.setMethod + " and " + type.restoreMethod + sb.toString(),
          new IllegalStateException());
    } else {
      log.warn(
          "Unbalanced calls to " + type.setMethod + " and " + type.restoreMethod + sb.toString());
    }
  }

  private static void addStackTrace(AdminType adminMode) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    sw.write(adminMode + "\n");
    new Exception().printStackTrace(pw);
    final List<String> list;

    ThreadLocal<List<String>> trace = adminMode.trace;
    if (trace.get() == null) {
      trace.set(new ArrayList<String>());
    }
    list = trace.get();

    if (list.size() > 0 && list.size() >= ADMIN_TRACE_SIZE) {
      list.remove(0);
    }
    list.add(sw.toString());
  }

  /**
   * Clears the admin context stack.
   */
  public static void clearAdminModeStack() {
    for (AdminType type : AdminType.values()) {
      Stack<OBAdminMode> stack = getAdminModeStack(type);
      if (!stack.isEmpty()) {
        printUnbalancedWarning(false, type);
      }
      stack.clear();
      type.trace.remove();
    }

    if (adminModeSet.get() != null) {
      log.warn("Unbalanced calls to setAdminMode and restorePreviousMode");
      adminModeSet.remove();
    }
  }

  /**
   * Sets the OBContext through the information stored in the http session of the request (mainly
   * the authenticated user). Note will not set the context in the http session if the session is
   * not present.
   * 
   * @param request
   */
  public static void setOBContext(HttpServletRequest request) {
    final HttpSession session = request.getSession(false);

    if (session == null) {
      return;
    }

    synchronized (session) {
      OBContext context = (OBContext) session.getAttribute(CONTEXT_PARAM);

      if (context == null) {
        context = new OBContext();
        if (context.setFromRequest(request)) {
          setOBContextInSession(request, context);
          setOBContext(context);
        }
      } else {
        if (!context.isInSync(request)) {
          context.setFromRequest(request);
        }
        setOBContext(context);
      }
    }
  }

  /**
   * Sets the passed OBContext in the http session. Will not set it if the passed context is the
   * admin context or if the http session is invalidated.
   * 
   * @param request
   *          the http request used to get the http session
   * @param context
   *          the context which will be stored in the session
   */
  public static void setOBContextInSession(HttpServletRequest request, OBContext context) {
    final HttpSession session = request.getSession(false);
    if (session == null) {
      // this can happen at logout, then the session is invalidated
      return;
    }
    if (context != null && context == adminContext) {
      log.warn("Trying to set the admin context in the session, "
          + "this means that the context has not been reset correctly in a finally block."
          + " When using the admin context it should always be removed in a finally block by the application");
      return;
    }

    // Determine whether using new ui by #Hide_BackButton session attribute set in Menu class
    if (context != null) {
      String newUIValue = (String) session.getAttribute("#Hide_BackButton".toUpperCase());
      context.setNewUI("true".equals(newUIValue));
    }

    if (session.getAttribute(CONTEXT_PARAM) != context) {
      session.setAttribute(CONTEXT_PARAM, context);
    }
  }

  /**
   * Creates the context using the userId and sets it in the thread (as a ThreadLocal). The user
   * denoted by the userId will be automatically logged in.
   * 
   * @param userId
   *          the id of the user (as present in the database)
   */
  public static void setOBContext(String userId) {
    setOBContext(userId, null, null, null);
  }

  /**
   * Creates the context using the userId, roleId, clientId, orgId and sets it in the thread (as
   * ThreadLocal).
   * 
   * @param userId
   *          the id of the user
   * @param roleId
   *          the id of the role under which the user is currently working
   * @param clientId
   *          the id of the user's client
   * @param orgId
   *          the ud of the user's organization
   */
  public static void setOBContext(String userId, String roleId, String clientId, String orgId) {
    setOBContext(userId, roleId, clientId, orgId, null, null);
  }

  /**
   * Creates the context using the userId, roleId, clientId, orgId and sets it in the thread (as
   * ThreadLocal).
   * 
   * @param userId
   *          the id of the user
   * @param roleId
   *          the id of the role under which the user is currently working
   * @param clientId
   *          the id of the user's client
   * @param orgId
   *          the ud of the user's organization
   * @param languageCode
   *          the selected language, if null then the user language is read.
   */
  public static void setOBContext(String userId, String roleId, String clientId, String orgId,
      String languageCode) {
    setOBContext(userId, roleId, clientId, orgId, languageCode, null);
  }

  /**
   * Creates the context using the userId, roleId, clientId, orgId and sets it in the thread (as
   * ThreadLocal).
   * 
   * @param userId
   *          the id of the user
   * @param roleId
   *          the id of the role under which the user is currently working
   * @param clientId
   *          the id of the user's client
   * @param orgId
   *          the ud of the user's organization
   * @param languageCode
   *          the selected language, if null then the user language is read.
   * @param warehouseId
   *          the id of the current warehouse of the user.
   */
  public static void setOBContext(String userId, String roleId, String clientId, String orgId,
      String languageCode, String warehouseId) {
    final OBContext context = OBProvider.getInstance().get(OBContext.class);
    setOBContext((OBContext) null);
    context.initialize(userId, roleId, clientId, orgId, languageCode, warehouseId);
    setOBContext(context);
  }

  /**
   * Creates the context without setting the context in the thread.
   * 
   * @param userId
   *          the user used for creating the context
   * @return the created context
   */
  public static OBContext createOBContext(String userId) {
    final OBContext context = new OBContext();
    context.initialize(userId);
    return context;
  }

  /**
   * Set the context in the thread, this context will then be used by the Data Access Layer
   * internals.
   * 
   * @param obContext
   *          the context to set in the thread
   */
  public static void setOBContext(OBContext obContext) {
    // if (obContext != null && instance.get() != null)
    // throw new ArgumentException("OBContext already set");
    if (obContext != null) {
      instance.set(obContext);
    } else {
      instance.remove();
    }

    // nullify the admin context
    adminModeSet.remove();
  }

  /**
   * Returns the OBContext currently set in the thread. Will return null if no context was set.
   * 
   * @return the context in the thread, null if none present
   */
  public static OBContext getOBContext() {
    final OBContext localContext = instance.get();
    return localContext;
  }

  public String getUserLevel() {
    return userLevel;
  }

  public void setUserLevel(String userLevel) {
    this.userLevel = userLevel.trim();
  }

  /**
   * Computes the clients allowed for read access using the user level and the client of the role.
   * 
   * @param role
   *          the role used to initialize the readable clients
   */
  public void setReadableClients(Role role) {
    if (getUserLevel().equals("S")) {
      readableClients = new String[] { "0" };
    } else if (role.getClient().getId().equals("0")) {
      readableClients = new String[] { "0" };
    } else {
      readableClients = new String[] { role.getClient().getId(), "0" };
    }
  }

  // writable organization is determined as follows
  // 1) if the user has level S or C then they can only write in organization
  // 0
  // 2) in other cases read the organizations from the role
  // only: if user has userlevel O then he/she can not read organization 0
  // Utility.getContext and LoginUtils for current working
  private void setWritableOrganizations(Role role) {
    writableOrganizations = new HashSet<String>();
    final String localUserLevel = getUserLevel();
    if (localUserLevel.contains("S") || localUserLevel.contains("C")) {
      // Force org * in case of System, Client or Client/Organization
      writableOrganizations.add("0");
    }

    final List<String> os = getActiveOrganizationList(role);
    for (final String o : os) {
      writableOrganizations.add(o);
    }

    if (localUserLevel.equals("O")) { // remove *
      writableOrganizations.remove("0");
    }
    writableOrganizations.addAll(additionalWritableOrganizations);
  }

  private List<String> getActiveOrganizationList(Role thisRole) {
    return getOrganizationList(thisRole, organizationList, additionalWritableOrganizations, true);
  }

  private void setDeactivatedOrganizations(Role role) {
    deactivatedOrganizations = new HashSet<String>();
    final List<String> os = getDeactivatedOrganizationList(role);
    for (final String o : os) {
      deactivatedOrganizations.add(o);
    }
  }

  private List<String> getDeactivatedOrganizationList(Role thisRole) {
    return getOrganizationList(thisRole, deactivatedOrganizationList, null, false);
  }

  private List<String> getOrganizationList(Role targetRole, List<String> orgList,
      Set<String> additionalOrgs, boolean isActiveOrganization) {

    if (orgList != null) {
      return new ArrayList<>(orgList);
    }

    // @formatter:off
    final String orgsQryStr = "select o.id"
        + " from Organization o"
        + "   inner join ADRoleOrganization roa on (o.id=roa.organization.id)"
        + " where roa.role.id= :targetRoleId"
        + "   and roa.active='Y'"
        + "   and o.active= :active";
    // @formatter:on

    final Query<String> qry = SessionHandler.getInstance()
        .createQuery(orgsQryStr, String.class)
        .setParameter("targetRoleId", targetRole.getId())
        .setParameter("active", isActiveOrganization);

    List<String> currentOrgList = qry.list();

    if (additionalOrgs != null) {
      for (final String orgId : additionalOrgs) {
        if (!currentOrgList.contains(orgId)) {
          currentOrgList.add(orgId);
        }
      }
    }
    return new ArrayList<>(currentOrgList);
  }

  private List<String> getOrganizations(Client client) {
    // @formatter:off
    final String orgQryStr = "select o.id"
        + " from Organization o"
        + " where o.client.id=:clientId";
    // @formatter:on
    final Query<String> qry = SessionHandler.getInstance()
        .createQuery(orgQryStr, String.class)
        .setParameter("clientId", client.getId());
    organizationList = qry.list();
    return organizationList;
  }

  private void setReadableOrganizations(Role role) {
    long t = System.currentTimeMillis();
    final Set<String> os = new HashSet<>(getActiveOrganizationList(role));
    final Set<String> readableOrgs = new HashSet<String>();
    if (os.contains("0")) {
      // if zero is an organization then add them all!
      readableOrgs.addAll(getOrganizations(getCurrentClient()));
    } else {
      for (final String o : os) {
        readableOrgs.addAll(getOrganizationStructureProvider().getNaturalTree(o));
      }
    }
    readableOrgs.add("0");
    readableOrganizations = new String[readableOrgs.size()];
    int i = 0;
    for (final String s : readableOrgs) {
      readableOrganizations[i++] = s;
    }
    log.debug("setReadableOrganizations " + (System.currentTimeMillis() - t));
  }

  public Client getCurrentClient() {
    return currentClient;
  }

  public void setCurrentClient(Client currentClient) {
    this.currentClient = currentClient;
  }

  public void setCurrentOrganization(Organization currentOrganization) {
    this.currentOrganization = currentOrganization;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
    setRTL(language.isRTLLanguage());
  }

  public Organization getCurrentOrganization() {
    return currentOrganization;
  }

  public void removeWritableOrganization(String orgId) {
    additionalWritableOrganizations.remove(orgId);
  }

  public void removeFromWritableOrganization(String orgId) {
    writableOrganizations.remove(orgId);
  }

  /**
   * Adds a new organization for which write access is allowed.
   * 
   * @param orgId
   *          the id of the additional writable organization
   */
  public void addWritableOrganization(String orgId) {
    additionalWritableOrganizations.add(orgId);
    // nullify will be recomputed at first occasion
    organizationList = null;
    deactivatedOrganizationList = null;
    readableOrganizations = null;
    writableOrganizations = null;
    deactivatedOrganizations = null;
  }

  /**
   * Sets the OBContext using the information stored in the HttpSession
   * 
   * @param request
   *          the http request used to set the OBContext
   * @return false if no user was specified in the session, true otherwise
   */
  public boolean setFromRequest(HttpServletRequest request) {
    final HttpSession session = request.getSession(false);

    if (session == null) {
      // not set
      return false;
    }

    String userId = null;
    for (final Enumeration<?> e = session.getAttributeNames(); e.hasMoreElements();) {
      final String name = (String) e.nextElement();
      if (name.equalsIgnoreCase(AUTHENTICATED_USER)) {
        userId = (String) session.getAttribute(name);
        break;
      }
    }
    if (userId == null) {
      return false; // not yet set
    }
    try {
      return initialize(userId, getSessionValue(request, ROLE), getSessionValue(request, CLIENT),
          getSessionValue(request, ORG));
    } catch (final OBSecurityException e) {
      // remove the authenticated user
      session.setAttribute(AUTHENTICATED_USER, null);
      throw e;
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    if (log.isTraceEnabled()) {
      log.trace("Write context: " + this);
    }
    out.defaultWriteObject();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (log.isTraceEnabled()) {
      log.trace("Read context: " + this);
    }
    initialize(userID, roleID, clientID, orgID, langID, warehouseID);
  }

  // sets the context by reading all user information
  public boolean initialize(String userId) {
    return initialize(userId, null, null, null);
  }

  // sets the context by reading all user information
  public boolean initialize(String userId, String roleId, String clientId, String orgId) {
    return initialize(userId, roleId, clientId, orgId, null);
  }

  // sets the context by reading all user information
  private boolean initialize(String userId, String roleId, String clientId, String orgId,
      String languageCode) {
    return initialize(userId, roleId, clientId, orgId, languageCode, null);
  }

  // sets the context by reading all user information
  private boolean initialize(String userId, String roleId, String clientId, String orgId,
      String languageCode, String warehouseId) {
    userID = userId;
    roleID = roleId;
    clientID = clientId;
    orgID = orgId;
    langID = languageCode;
    warehouseID = warehouseId;

    additionalWritableOrganizations = new HashSet<>();

    String localClientId = clientId;
    final User u = SessionHandler.getInstance().find(User.class, userId);
    if (u == null) {
      return false;
    }
    setInitialized(false);

    // can't use enableAsAdminContext here otherwise there is a danger of
    // recursive/infinite calls.
    // enableAsAdminContext();
    OBAdminMode am = new OBAdminMode();
    am.setAdminMode(true);
    am.setOrgClientAccessCheck(true);
    getAdminModeStack(AdminType.ADMIN_MODE).push(am);
    try {
      setUser(u);
      Hibernate.initialize(getUser().getClient());
      Hibernate.initialize(getUser().getOrganization());
      Hibernate.initialize(getUser().getDefaultOrganization());
      Hibernate.initialize(getUser().getDefaultWarehouse());
      Hibernate.initialize(getUser().getDefaultClient());
      Hibernate.initialize(getUser().getDefaultRole());
      Hibernate.initialize(getUser().getDefaultLanguage());
      if (getUser().getBusinessPartner() != null) {
        Hibernate.initialize(getUser().getBusinessPartner());
      }

      organizationStructureProviderByClient = new HashMap<String, OrganizationStructureProvider>();
      acctSchemaStructureProviderByClient = new HashMap<String, AcctSchemaStructureProvider>();

      // first take the passed role, if any
      // now check if the default role is active, if not another one needs
      // to be
      // selected.
      if (roleId != null) {
        // @formatter:off
        final String roleQryStr = "select r"
            + " from ADRole r"
            + " where r.id=:roleId";
        // @formatter:on
        Map<String, String> params = new HashMap<>(1);
        params.put("roleId", roleId);
        final Role r = getOne(Role.class, roleQryStr, params, true);
        setRole(r);
      } else if (getUser().getDefaultRole() != null && getUser().getDefaultRole().isActive()) {
        setRole(getUser().getDefaultRole());
      } else {
        // @formatter:off
        final String userRolesQryStr = "select ur"
            + " from ADUserRoles ur"
            + " where ur.userContact.id=:userId"
            + "  and ur.active='Y'"
            + "  and ur.role.active='Y'"
            + " order by ur.role.id asc";
        // @formatter:on
        Map<String, String> params = new HashMap<>(1);
        params.put("userId", u.getId());
        final UserRoles ur = getOne(UserRoles.class, userRolesQryStr, params, false);
        if (ur == null) {
          throw new OBSecurityException(
              "Your user is not assigned to a Role and it is required to login into Etendo. Ask the Security Administrator");
        }
        Hibernate.initialize(ur.getRole());
        setRole(ur.getRole());
      }

      Check.isNotNull(getRole(), "Role may not be null");

      if (orgId != null) {
        // @formatter:off
        final String orgQryStr = "select r"
            + " from Organization r"
            + " where r.id=:orgId";
        // @formatter:on
        Map<String, String> params = new HashMap<>(1);
        params.put("orgId", orgId);
        final Organization o = getOne(Organization.class, orgQryStr, params, true);
        setCurrentOrganization(o);
      } else if (getUser().getDefaultOrganization() != null
          && getUser().getDefaultOrganization().isActive()) {
        setCurrentOrganization(getUser().getDefaultOrganization());
      } else {
        // @formatter:off
        final String roleOrgQryStr = "select roa"
            + " from ADRoleOrganization roa"
            + " where roa.role.id=:roleId"
            + "  and roa.active='Y'"
            + "  and roa.organization.active='Y'"
            + " order by roa.organization.id desc";
        // @formatter:on
        Map<String, String> params = new HashMap<>(1);
        params.put("roleId", getRole().getId());
        final RoleOrganization roa = getOne(RoleOrganization.class, roleOrgQryStr, params, false);
        Hibernate.initialize(roa.getOrganization());
        setCurrentOrganization(roa.getOrganization());

        // if no client id then use the client of the role
        if (localClientId == null) {
          localClientId = roa.getClient().getId();
        }
      }

      Check.isNotNull(getCurrentOrganization(), "Organization may not be null");

      // check that the current organization is actually writable!
      final Set<String> writableOrgs = getWritableOrganizations();
      if (!writableOrgs.contains(getCurrentOrganization().getId())) {
        if (writableOrgs.isEmpty()) {
          log.warn(
              "User {} with role {} (default organization {}) does not have write access to any organization",
              getUser(), getRole(), getCurrentOrganization(), new Exception("stack trace"));
        } else {
          // take the first writableOrganization
          Organization newDefaultOrg = SessionHandler.getInstance()
              .find(Organization.class, writableOrgs.iterator().next());
          log.warn(
              "User {} with role {} does not have write access to its current organization {}. Setting current organization to {}",
              getUser(), getRole(), getCurrentOrganization(), newDefaultOrg,
              new Exception("stack trace"));
          setCurrentOrganization(newDefaultOrg);
        }
      }

      if (localClientId != null) {
        // @formatter:off
        final String clientQryStr = "select r"
            + " from ADClient r"
            + " where r.id=:clientId";
        // @formatter:on
        Map<String, String> params = new HashMap<>(1);
        params.put("clientId", localClientId);
        final Client c = getOne(Client.class, clientQryStr, params, true);
        setCurrentClient(c);
      } else if (getUser().getDefaultClient() != null && getUser().getDefaultClient().isActive()) {
        setCurrentClient(getUser().getDefaultClient());
      } else {
        // The HttpSecureAppServlet reads the client after the
        // organization
        // which
        // theoretically can
        // result in a current organization which does not belong to the
        // client
        // other comment, use the client of the organization
        Hibernate.initialize(getCurrentOrganization().getClient());
        setCurrentClient(getCurrentOrganization().getClient());
      }
      Hibernate.initialize(getCurrentClient().getClientInformationList());

      Check.isNotNull(getCurrentClient(), "Client may not be null");
      Check.isTrue(getCurrentClient().isActive(),
          "Current Client " + getCurrentClient().getName() + " is not active!");
      if (languageCode != null) {
        // @formatter:off
        final String langQryStr = "select l"
            + " from ADLanguage l"
            + " where l.language=:languageCode";
        // @formatter:on
        final Query<Language> qry = SessionHandler.getInstance()
            .createQuery(langQryStr, Language.class)
            .setParameter("languageCode", languageCode);
        List<Language> languages = qry.list();
        if (languages.isEmpty()) {
          throw new IllegalArgumentException("No language found for code " + languageCode);
        }
        setLanguage(languages.get(0));
      } else if (getUser().getDefaultLanguage() != null
          && getUser().getDefaultLanguage().isActive()) {
        setLanguage(getUser().getDefaultLanguage());
      } else if (getCurrentClient().getLanguage() != null) {
        setLanguage(getCurrentClient().getLanguage());
      } else {
        final Client systemClient = OBDal.getInstance().get(Client.class, "0");
        setLanguage(systemClient.getLanguage());
      }
      Hibernate.initialize(getLanguage());

      Check.isNotNull(getLanguage(), "Language may not be null");

      // @formatter:off
      final String trlQryStr = "select count(*)"
          + " from ADLanguage l"
          + " where l.systemLanguage= true ";
      // @formatter:on
      final Query<Long> trl = SessionHandler.getInstance().createQuery(trlQryStr, Long.class);

      // There are translations installed in the system when there are more than one system
      // language. There's always at last one which is the base language.
      setTranslationInstalled(trl.list().get(0) > 1);

      setReadableClients(role);

      // note sometimes the warehouseId is an empty string
      // this happens when it is set from the session variables
      if (warehouseId != null && warehouseId.trim().length() > 0) {
        // @formatter:off
        final String warehouseQryStr = "select w"
            + " from Warehouse w"
            + " where w.id=:id";
        // @formatter:on
        final Query<Warehouse> qry = SessionHandler.getInstance()
            .createQuery(warehouseQryStr, Warehouse.class)
            .setParameter("id", warehouseId);
        setWarehouse(qry.uniqueResult());
      } else if (getUser().getDefaultWarehouse() != null) {
        setWarehouse(getUser().getDefaultWarehouse());
      }

      // initialize some proxys
      Hibernate.initialize(getCurrentOrganization().getClient());
      Hibernate.initialize(getCurrentClient().getOrganization());
      Hibernate.initialize(getRole().getClient());
      Hibernate.initialize(getRole().getOrganization());
      Hibernate.initialize(getLanguage().getClient());
      Hibernate.initialize(getLanguage().getOrganization());
      if (getWarehouse() != null) {
        Hibernate.initialize(getWarehouse());
        Hibernate.initialize(getWarehouse().getClient());
        Hibernate.initialize(getWarehouse().getOrganization());
      }

      // TODO: add logging of all context information
    } finally {
      // can't use resetAsAdminContext here otherwise there is a danger of
      // recursive/infinite calls.
      // resetAsAdminContext();
      getAdminModeStack(AdminType.ADMIN_MODE).pop();
      setInitialized(true);
    }
    return true;
  }

  private <T extends Object> T getOne(Class<T> clz, String qryStr, Map<String, String> parameters,
      boolean doCheck) {
    final Query<T> qry = SessionHandler.getInstance().createQuery(qryStr, clz);
    qry.setProperties(parameters);
    qry.setMaxResults(1);
    final List<T> result = qry.list();
    if (doCheck && result.size() != 1) {
      log.error("The query '" + qryStr + "' returned " + result.size()
          + " results while only 1 result was expected");
    }
    if (result.isEmpty()) {
      return null;
    }
    return result.get(0);
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    isAdministrator = (role.getId()).equals("0");
    isPortalRole = role.isForPortalUsers();
    isWebServiceEnabled = role.isWebServiceEnabled();
    setUserLevel(role.getUserLevel());
    entityAccessChecker = null;
    writableOrganizations = null;
    deactivatedOrganizations = null;
    readableClients = null;
    readableOrganizations = null;
    this.role = role;
  }

  public OrganizationStructureProvider getOrganizationStructureProvider() {
    return getOrganizationStructureProvider(getCurrentClient().getId());
  }

  public OrganizationStructureProvider getOrganizationStructureProvider(String clientId) {
    OrganizationStructureProvider orgProvider = organizationStructureProviderByClient.get(clientId);

    // create one
    if (orgProvider == null) {
      orgProvider = OBProvider.getInstance().get(OrganizationStructureProvider.class);
      orgProvider.setClientId(clientId);
      organizationStructureProviderByClient.put(clientId, orgProvider);
    }
    return orgProvider;
  }

  public AcctSchemaStructureProvider getAcctSchemaStructureProvider() {
    return getAcctSchemaStructureProvider(getCurrentClient().getId());
  }

  public AcctSchemaStructureProvider getAcctSchemaStructureProvider(String clientId) {
    AcctSchemaStructureProvider acctSchemaProvider = acctSchemaStructureProviderByClient
        .get(clientId);

    // create one
    if (acctSchemaProvider == null) {
      acctSchemaProvider = OBProvider.getInstance().get(AcctSchemaStructureProvider.class);
      acctSchemaProvider.setClientId(clientId);
      acctSchemaStructureProviderByClient.put(clientId, acctSchemaProvider);
    }
    return acctSchemaProvider;
  }

  public String[] getReadableOrganizations() {
    if (readableOrganizations == null) {
      setReadableOrganizations(getRole());
    }
    return readableOrganizations.clone();
  }

  public Set<String> getWritableOrganizations() {
    if (writableOrganizations == null) {
      setWritableOrganizations(getRole());
    }
    return new HashSet<String>(writableOrganizations);
  }

  public Set<String> getDeactivatedOrganizations() {
    if (deactivatedOrganizations == null) {
      setDeactivatedOrganizations(getRole());
    }
    return new HashSet<String>(deactivatedOrganizations);
  }

  public String[] getReadableClients() {
    if (readableClients == null) {
      setReadableClients(getRole());
    }
    return readableClients.clone();
  }

  public RecordAccessChecker getRecordAccessChecker() {
    return recordAccessChecker;
  }

  public void setRecordAccessChecker(RecordAccessChecker recordAccessChecker) {
    this.recordAccessChecker = recordAccessChecker;
  }

  public void setEntityAccessChecker(EntityAccessChecker entityAccessChecker) {
    this.entityAccessChecker = entityAccessChecker;
  }

  public EntityAccessChecker getEntityAccessChecker() {
    if (entityAccessChecker == null) {
      entityAccessChecker = OBProvider.getInstance().get(EntityAccessChecker.class);
      // use the DalUtil.getId because it does not resolve hibernate
      // proxies
      entityAccessChecker.setRoleId(getRole().getId());
      entityAccessChecker.setObContext(this);
      entityAccessChecker.initialize();
    }
    return entityAccessChecker;
  }

  public boolean isInAdministratorMode() {
    if (getAdminModeStack(AdminType.ADMIN_MODE).size() > 0
        && getAdminModeStack(AdminType.ADMIN_MODE).peek().isAdminMode()) {
      return true;
    }
    return adminModeSet.get() != null || isAdministrator;
  }

  public boolean isInCrossOrgAdministratorMode() {
    return getAdminModeStack(AdminType.CROSS_ORG_ADMIN_MODE).size() > 0
        && getAdminModeStack(AdminType.CROSS_ORG_ADMIN_MODE).peek().isCrossOrgAdminMode();
  }

  public boolean doOrgClientAccessCheck() {
    if (getAdminModeStack(AdminType.ADMIN_MODE).size() > 0
        && !getAdminModeStack(AdminType.ADMIN_MODE).peek().doOrgClientAccessCheck()) {
      return false;
    }
    return !(adminModeSet.get() != null || isAdministrator);
  }

  /**
   * Defines whether entity check should or not compare entity's access level with role's user level
   * in order to completely prevent access to that entity.
   */
  public boolean doAccessLevelCheck() {
    return checkAccessLevel;
  }

  /** @see OBContext#doAccessLevelCheck() */
  public void setCheckAccessLevel(boolean checkAccessLevel) {
    this.checkAccessLevel = checkAccessLevel;
  }

  public boolean isAdminContext() {
    return this == adminContext;
  }

  /**
   * @deprecated use {@link #setAdminMode()} and {@link #restorePreviousMode()}.
   */
  @Deprecated
  public boolean setInAdministratorMode(boolean inAdministratorMode) {
    final boolean prevMode = isInAdministratorMode() && !isAdministrator;
    if (inAdministratorMode) {
      adminModeSet.set(this);
    } else {
      adminModeSet.remove();
    }
    return prevMode;
  }

  public boolean isInitialized() {
    return isInitialized;
  }

  public void setInitialized(boolean isInitialized) {
    this.isInitialized = isInitialized;
  }

  private boolean isInSync(HttpServletRequest request) {
    if (unequal(request, AUTHENTICATED_USER, getUser())) {
      return false;
    }
    if (unequal(request, ROLE, getRole())) {
      return false;
    }
    if (unequal(request, CLIENT, getCurrentClient())) {
      return false;
    }
    if (unequal(request, ORG, getCurrentOrganization())) {
      return false;
    }
    return true;
  }

  private boolean unequal(HttpServletRequest request, String param, BaseOBObject bob) {
    if (bob == null) {
      return true;
    }
    final String sessionValue = getSessionValue(request, param);
    if (sessionValue == null) {
      return false;
    }
    return !bob.getId().equals(sessionValue);
  }

  private String getSessionValue(HttpServletRequest request, String param) {
    final HttpSession session = request.getSession(false);
    if (session == null) {
      return null;
    }
    return (String) session.getAttribute(param.toUpperCase());
  }

  public Warehouse getWarehouse() {
    return warehouse;
  }

  public void setWarehouse(Warehouse warehouse) {
    this.warehouse = warehouse;
  }

  public boolean isNewUI() {
    return newUI;
  }

  public void setNewUI(boolean newUI) {
    this.newUI = newUI;
  }

  @Dependent
  private static class OBAdminMode {

    private boolean adminMode;
    private boolean doOrgClientAccessCheck;
    private boolean crossOrgAdminMode = false;

    public void setAdminMode(boolean adminMode) {
      this.adminMode = adminMode;
    }

    public void setCrossOrgAdminMode(boolean crossOrgAdminMode) {
      this.crossOrgAdminMode = crossOrgAdminMode;
    }

    public boolean isAdminMode() {
      return adminMode;
    }

    public boolean isCrossOrgAdminMode() {
      return crossOrgAdminMode;
    }

    public void setOrgClientAccessCheck(boolean doOrgClientAccessCheck) {
      this.doOrgClientAccessCheck = doOrgClientAccessCheck;
    }

    public boolean doOrgClientAccessCheck() {
      return doOrgClientAccessCheck;
    }
  }

  public boolean isRTL() {
    return isRTL;
  }

  public boolean isPortalRole() {
    return isPortalRole;
  }

  public boolean isWebServiceEnabled() {
    return isWebServiceEnabled;
  }

  public void setRTL(boolean isRTL) {
    this.isRTL = isRTL;
  }

  public boolean isTranslationInstalled() {
    return translationInstalled;
  }

  private void setTranslationInstalled(boolean translationInstalled) {
    this.translationInstalled = translationInstalled;
  }

  @Override
  public String toString() {
    return "[user: " + userID + ", role:" + roleID + ", client:" + clientID + ", org:" + orgID
        + ", warehouse: " + warehouseID + ", lang:" + langID + "]";
  }

}
