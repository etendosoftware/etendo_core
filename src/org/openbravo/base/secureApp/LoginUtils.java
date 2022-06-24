/*
 ************************************************************************************
 * Copyright (C) 2001-2021 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.base.secureApp;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.authentication.hashing.PasswordHash;
import org.openbravo.base.HttpBaseUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.dal.xml.XMLUtil;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.security.SessionLogin;
import org.openbravo.erpCommon.utility.DimensionDisplayUtility;
import org.openbravo.erpCommon.utility.OBLedgerUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.StringCollectionUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LoginUtils {

  public static Logger log4j = LogManager.getLogger();

  /** Creates a new instance of LoginUtils */
  private LoginUtils() {
  }

  /**
   * Returns a userId which matches the login and password. If no user is found then null is
   * returned. The combination of login and password is used to find the user.
   * 
   * Blocking users is taking into account
   * 
   * Note that only active users are returned.
   * 
   * @param connectionProvider
   *          , see the {@link DalConnectionProvider} for an instance of a ConnectionProvider for
   *          the DAL.
   * @param login
   *          the login
   * @param unHashedPassword
   *          the password, the unhashed password as it is entered by the user.
   * @return the user id or null if no user could be found or the user is locked.
   * @see PasswordHash
   */
  public static String getValidUserId(ConnectionProvider connectionProvider, String login,
      String unHashedPassword) {
    try {
      // Delay response and check for locked user
      UserLock lockSettings = new UserLock(login);
      lockSettings.delayResponse();
      if (lockSettings.isLockedUser()) {
        return null;
      }

      final String userId = checkUserPassword(connectionProvider, login, unHashedPassword);
      if (userId == null) {
        lockSettings.addFail();
      }
      return userId;
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Similar to {@link LoginUtils#getValidUserId(ConnectionProvider, String, String)} but not
   * blocking user accounts.
   * 
   */
  public static String checkUserPassword(ConnectionProvider connectionProvider, String login,
      String unHashedPassword) {
    try {
      Optional<User> user = PasswordHash.getUserWithPassword(login, unHashedPassword);
      return user.map(User::getId).orElse(null);
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  static boolean validUserRole(ConnectionProvider conn, String strUserAuth, String strRol)
      throws ServletException {
    boolean valid = SeguridadData.isUserRole(conn, strUserAuth, strRol);
    if (!valid) {
      log4j.error("Login role is not in user roles list");
      log4j.error("User: " + strUserAuth);
      log4j.error("Role: " + strRol);
    }
    return valid;
  }

  static boolean validRoleClient(ConnectionProvider conn, String role, String client)
      throws ServletException {
    boolean valid = SeguridadData.isRoleClient(conn, role, client);
    if (!valid) {
      log4j.error("Login client is not in role clients list. Role: " + role + ", Client: " + client,
          new Exception("stack trace"));
    }
    return valid;
  }

  static boolean validRoleOrg(ConnectionProvider conn, String role, String org)
      throws ServletException {
    boolean valid = SeguridadData.isLoginRoleOrg(conn, role, org);
    if (!valid) {
      log4j.error(
          "Login organization is not in role organizations list. Role: " + role + ", Org: " + org,
          new Exception("stack trace"));
    }
    return valid;
  }

  public static List<RoleOrganization> loadRoleOrganization(String strRol) {

    OBContext.setAdminMode();
    try {
      OBQuery<RoleOrganization> query = OBDal.getInstance()
          .createQuery(RoleOrganization.class,
              "WHERE role.id = :roleId ORDER BY client.id, organization.id");
      query.setNamedParameter("roleId", strRol);
      query.setFilterOnReadableClients(false);
      query.setFilterOnReadableOrganization(false);
      return query.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static String buildClientList(List<RoleOrganization> roleorglist) {
    StringBuilder clientlist = new StringBuilder();
    String currentclient = null;
    for (RoleOrganization roleorg : roleorglist) {
      if (currentclient == null || !currentclient.equals(roleorg.getClient().getId())) {
        currentclient = roleorg.getClient().getId();
        if (clientlist.length() > 0) {
          clientlist.append(',');
        }
        clientlist.append('\'');
        clientlist.append(roleorg.getClient().getId());
        clientlist.append('\'');
      }
    }
    return clientlist.toString();
  }

  public static String buildOrgList(List<RoleOrganization> roleorglist) {
    StringBuilder orglist = new StringBuilder();
    for (RoleOrganization roleorg : roleorglist) {
      if (orglist.length() > 0) {
        orglist.append(',');
      }
      orglist.append('\'');
      orglist.append(roleorg.getOrganization().getId());
      orglist.append('\'');
    }
    return orglist.toString();
  }

  public static boolean fillSessionArguments(ConnectionProvider conn, VariablesSecureApp vars,
      String strUserAuth, String strLanguage, String strIsRTL, String strRol, String strCliente,
      String strOrg, String strAlmacen) throws ServletException {

    boolean lightLogin = "Y".equals(vars.getSessionValue("#Light_Login"));

    Client client = null;
    boolean isAccountingDimensionConfigCentrally = false;

    // Check session options
    if (!validUserRole(conn, strUserAuth, strRol) || !validRoleClient(conn, strRol, strCliente)
        || !validRoleOrg(conn, strRol, strOrg)) {
      return false;
    }

    OBContext currentContext = OBContext.getOBContext();
    // set the obcontext
    try {
      boolean sameContext = currentContext != null
          && currentContext.getUser().getId().equals(strUserAuth)
          && currentContext.getRole().getId().equals(strRol)
          && currentContext.getCurrentClient().getId().equals(strCliente)
          && currentContext.getCurrentOrganization().getId().equals(strOrg)
          && currentContext.getLanguage() != null
          && currentContext.getLanguage().getLanguage().equals(strLanguage)
          && currentContext.getWarehouse() != null
          && currentContext.getWarehouse().getId().equals(strAlmacen);
      if (!lightLogin || !sameContext) {
        OBContext.setOBContext(strUserAuth, strRol, strCliente, strOrg, strLanguage, strAlmacen);
      }
    } catch (final OBSecurityException e) {
      log4j.error("Error trying to initialize OBContext: " + e.getMessage(), e);
      return false;
    }

    boolean shouldCheckAccessLevel = true;

    try {
      shouldCheckAccessLevel = Preferences.NO.equals(Preferences.getPreferenceValue(
          "BypassAccessLevelEntityCheck", true, OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), null));
    } catch (PropertyException prefNotDefined) {
    }

    String csrfToken = SequenceIdData.getUUID();

    OBContext.getOBContext().setCheckAccessLevel(shouldCheckAccessLevel);

    // Set session vars
    vars.setSessionValue("#AD_User_ID", strUserAuth);
    vars.setSessionValue("#SalesRep_ID", strUserAuth);
    vars.setSessionValue("#AD_Language", strLanguage);
    vars.setSessionValue("#AD_Role_ID", strRol);
    vars.setSessionValue("#AD_Client_ID", strCliente);
    vars.setSessionValue("#AD_Org_ID", strOrg);
    vars.setSessionValue("#M_Warehouse_ID", strAlmacen);
    vars.setSessionValue("#CSRF_Token", csrfToken);
    vars.setSessionValue("#StdPrecision", "2");

    // Organizations tree
    // enable admin mode, as normal non admin-role
    // has no read-access to i.e. AD_OrgType
    OBContext.setAdminMode();
    try {
      client = OBDal.getInstance().get(Client.class, strCliente);
      isAccountingDimensionConfigCentrally = client.isAcctdimCentrallyMaintained();

      vars.setSessionValue("#AccessibleOrgTree", StringCollectionUtils
          .commaSeparated(OBContext.getOBContext().getReadableOrganizations()));
    } catch (Exception e) {
      log4j.warn("Error while setting Organzation tree to session " + e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }

    try {
      SeguridadData[] data = SeguridadData.select(conn, strRol, strUserAuth);
      if (data == null || data.length == 0) {
        OBContext.setOBContext(currentContext);
        return false;
      }

      List<RoleOrganization> datarolelist = loadRoleOrganization(strRol);

      vars.setSessionValue("#User_Level", data[0].userlevel);
      vars.setSessionValue("#User_Client", buildClientList(datarolelist));
      vars.setSessionValue("#User_Org", buildOrgList(datarolelist));
      vars.setSessionValue("#Approval_C_Currency_ID", data[0].cCurrencyId);
      vars.setSessionValue("#Approval_Amt", data[0].amtapproval);
      vars.setSessionValue("#Client_Value", data[0].value);
      data = null;

      // Get General Ledger of login organization
      if (!lightLogin) {
        AttributeData[] attr = null;
        String acctSchemaId = OBLedgerUtils.getOrgLedger(strOrg);
        if (StringUtils.isNotEmpty(acctSchemaId)) {
          attr = AttributeData.selectAcctSchema(conn, acctSchemaId,
              Utility.getContext(conn, vars, "#User_Client", "LoginHandler"));
        }

        // Get General Ledger of context organizations
        // Before going for all organizations, check if any org has an accounting schema
        if (ArrayUtils.isEmpty(attr) && existsAnyOrgWithLedgerConfigured()) {
          String[] orgList = Utility.getContext(conn, vars, "#User_Org", "LoginHandler")
              .replace("'", "")
              .split(",");
          for (String orgId : orgList) {
            if (!StringUtils.equals(orgId, strOrg)) {
              acctSchemaId = OBLedgerUtils.getOrgLedger(orgId);
              if (StringUtils.isNotEmpty(acctSchemaId)) {
                attr = AttributeData.selectAcctSchema(conn, acctSchemaId,
                    Utility.getContext(conn, vars, "#User_Client", "LoginHandler"));
                if (ArrayUtils.isNotEmpty(attr)) {
                  break;
                }
              }
            }
          }
        }

        if (attr != null && attr.length > 0) {
          vars.setSessionValue("$C_AcctSchema_ID", attr[0].value);
          AttributeData[] orgCurrency = AttributeData.selectOrgCurrency(conn, strOrg, strCliente);
          if (orgCurrency.length > 0) {
            vars.setSessionValue("$C_Currency_ID", orgCurrency[0].cCurrencyId);
          } else {
            vars.setSessionValue("$C_Currency_ID", attr[0].attribute);
          }
          vars.setSessionValue("#StdPrecision",
              AttributeData.selectStdPrecision(conn, attr[0].attribute,
                  Utility.getContext(conn, vars, "#User_Client", "LoginHandler"),
                  Utility.getContext(conn, vars, "#User_Org", "LoginHandler")));
          vars.setSessionValue("$HasAlias", attr[0].hasalias);

          // Load also old accounting dimension visibility session variables
          // Some of the dimensions still use old behavior: Activity, Sales Campaign, Asset
          for (int i = 0; i < attr.length; i++) {
            vars.setSessionValue("$Element_" + attr[i].elementtype, "Y");
          }
        }
        attr = null;

        // Compute accounting dimensions visibility session variables
        // Project, Business Partner, Product, Cost Center, User1, User2
        vars.setSessionValue(DimensionDisplayUtility.IsAcctDimCentrally,
            isAccountingDimensionConfigCentrally ? "Y" : "N");
        if (isAccountingDimensionConfigCentrally) {
          Map<String, String> acctDimMap = DimensionDisplayUtility
              .getAccountingDimensionConfiguration(client);
          for (Map.Entry<String, String> entry : acctDimMap.entrySet()) {
            vars.setSessionValue(entry.getKey(), entry.getValue());
          }
        }
        // Load session variables for computing read only logic for accounting dimension
        // configuration in in Client window
        Map<String, String> readOnlySessionVariableMap = DimensionDisplayUtility
            .getReadOnlyLogicSessionVariables();
        for (Map.Entry<String, String> entry : readOnlySessionVariableMap.entrySet()) {
          vars.setSessionValue(entry.getKey(), entry.getValue());
        }

        List<Preference> preferences = Preferences.getAllPreferences(strCliente, strOrg,
            strUserAuth, strRol);
        for (Preference preference : preferences) {
          Preferences.savePreferenceInSession(vars, preference);
        }

        DefaultSessionValuesData[] ds = DefaultSessionValuesData.select(conn);
        if (ds != null && ds.length > 0) {
          for (int i = 0; i < ds.length; i++) {
            String value = DefaultValuesData.select(conn, ds[i].columnname, ds[i].tablename,
                Utility.getContext(conn, vars, "#User_Client", "LoginHandler"),
                Utility.getContext(conn, vars, "#AccessibleOrgTree", "LoginHandler"));
            if (ds[i].tablename.equals("C_DocType")) {
              vars.setSessionValue("#C_DocTypeTarget_ID", value);
            }
            vars.setSessionValue("#" + ds[i].columnname, value);
          }
        }
        vars.setSessionValue("#Date", Utility.getContext(conn, vars, "#Date", "LoginHandler"));
        vars.setSessionValue("#ShowTrl", Utility.getPreference(vars, "ShowTrl", ""));
        vars.setSessionValue("#ShowAcct", Utility.getPreference(vars, "ShowAcct", ""));
        vars.setSessionValue("#ShowAudit", Utility.getPreference(vars, "ShowAuditDefault", ""));
        vars.setSessionValue("#ShowConfirmation",
            Utility.getPreference(vars, "ShowConfirmationDefault", ""));
        vars.setSessionValue("#Autosave", Utility.getPreference(vars, "Autosave", ""));

        SystemPreferencesData[] dataSystem = SystemPreferencesData.select(conn);
        if (dataSystem != null && dataSystem.length > 0) {
          vars.setSessionValue("#RecordRange", dataSystem[0].tadRecordrange);
          vars.setSessionValue("#RecordRangeInfo", dataSystem[0].tadRecordrangeInfo);
          vars.setSessionValue("#Transactional$Range", dataSystem[0].tadTransactionalrange);
          if (strIsRTL.equals("Y")) {
            vars.setSessionValue("#Theme", "rtl/" + dataSystem[0].tadTheme);
            vars.setSessionValue("#TextDirection", "RTL");
          } else if (strIsRTL.equals("N")) {
            vars.setSessionValue("#Theme", "ltr/" + dataSystem[0].tadTheme);
            vars.setSessionValue("#TextDirection", "LTR");
          } else {
            OBContext.setOBContext(currentContext);
            log4j.error(
                "Can't detect direction of language: ltr? rtl? parameter isRTL missing in call to LoginUtils.getStringParameter");
            return false;
          }
        }
      }

    } catch (ServletException e) {
      OBContext.setOBContext(currentContext);
      log4j.warn("Error while loading session arguments: " + e);
      return false;
    }

    // Login process if finished, set the flag as not logging in
    // this flag may not be removed from the session, it must be set
    // to N to prevent re-initializing the session continuously
    // See the HttpSecureAppServlet
    vars.setSessionValue("#loggingIn", "N");
    return true;
  }

  private static boolean existsAnyOrgWithLedgerConfigured() {
    OBCriteria<Organization> orgCriteria = OBDal.getInstance().createCriteria(Organization.class);
    orgCriteria.add(Restrictions.isNotNull(Organization.PROPERTY_GENERALLEDGER));
    orgCriteria.setMaxResults(1);
    return orgCriteria.uniqueResult() != null;
  }

  /**
   * Obtains defaults defined for a user and throws DefaultValidationException in case they are not
   * correct.
   */
  public static RoleDefaults getLoginDefaults(String strUserAuth, String role,
      ConnectionProvider cp) throws ServletException, DefaultValidationException {
    String strRole = role;
    if (strRole.equals("")) {
      // use default role
      strRole = DefaultOptionsData.defaultRole(cp, strUserAuth);
      if (strRole == null || !LoginUtils.validUserRole(cp, strUserAuth, strRole)) {
        // if default not set or not valid take any one
        strRole = DefaultOptionsData.getDefaultRole(cp, strUserAuth);
      }
    }
    validateDefault(strRole, strUserAuth, "Role");

    String strOrg = DefaultOptionsData.defaultOrg(cp, strUserAuth);
    // use default org
    if (strOrg == null || !LoginUtils.validRoleOrg(cp, strRole, strOrg)) {
      // if default not set or not valid take any one
      strOrg = DefaultOptionsData.getDefaultOrg(cp, strRole);
    }
    validateDefault(strOrg, strRole, "Org");

    String strClient = DefaultOptionsData.defaultClient(cp, strUserAuth);
    // use default client
    if (strClient == null || !LoginUtils.validRoleClient(cp, strRole, strClient)) {
      // if default not set or not valid take any one
      strClient = DefaultOptionsData.getDefaultClient(cp, strRole);
    }
    validateDefault(strClient, strRole, "Client");

    String strWarehouse = DefaultOptionsData.defaultWarehouse(cp, strUserAuth);
    if (strWarehouse == null) {
      strWarehouse = getDefaultWarehouse(cp, strClient, strOrg, strRole);
    }
    RoleDefaults defaults = new RoleDefaults();
    defaults.role = strRole;
    defaults.client = strClient;
    defaults.org = strOrg;
    defaults.warehouse = strWarehouse;
    return defaults;
  }

  /**
   * Validates if a selected default value is null or empty String
   * 
   * @throws DefaultValidationException
   */
  private static void validateDefault(String strValue, String strKey, String strError)
      throws DefaultValidationException {
    if (strValue == null || strValue.equals("")) {
      throw new DefaultValidationException("Unable to read default " + strError + " for:" + strKey,
          strError);
    }
  }

  /**
   * Utility class to store login defaults
   * 
   */
  public static class RoleDefaults {
    public String role;
    public String client;
    public String org;
    public String warehouse;
  }

  /**
   * Reads the number format from the format file and sets it in VariablesSecureApp
   * 
   */
  public static void readNumberFormat(VariablesSecureApp vars, String strFormatFile) {
    String strNumberFormat = "###,##0.00"; // Default number format
    String strGroupingSeparator = ","; // Default grouping separator
    String strDecimalSeparator = "."; // Default decimal separator
    final String formatNameforJrxml = "euroInform"; // Name of the format to use
    final HashMap<String, String> formatMap = new HashMap<String, String>();

    try {
      // Reading number format configuration
      final DocumentBuilder docBuilder = XMLUtil.getInstance().newDocumentBuilder();
      final Document doc = docBuilder.parse(new File(strFormatFile));
      doc.getDocumentElement().normalize();
      final NodeList listOfNumbers = doc.getElementsByTagName("Number");
      final int totalNumbers = listOfNumbers.getLength();
      for (int s = 0; s < totalNumbers; s++) {
        final Node NumberNode = listOfNumbers.item(s);
        if (NumberNode.getNodeType() == Node.ELEMENT_NODE) {
          final Element NumberElement = (Element) NumberNode;
          final String strNumberName = NumberElement.getAttributes()
              .getNamedItem("name")
              .getNodeValue();
          // store in session all the formats
          final String strFormatOutput = NumberElement.getAttributes()
              .getNamedItem("formatOutput")
              .getNodeValue();
          formatMap.put(strNumberName, strFormatOutput);
          vars.setSessionValue("#FormatOutput|" + strNumberName, strFormatOutput);
          vars.setSessionValue("#DecimalSeparator|" + strNumberName,
              NumberElement.getAttributes().getNamedItem("decimal").getNodeValue());
          vars.setSessionValue("#GroupSeparator|" + strNumberName,
              NumberElement.getAttributes().getNamedItem("grouping").getNodeValue());
          // set the numberFormat to be used in the renderJR function
          if (strNumberName.equals(formatNameforJrxml)) {
            strDecimalSeparator = NumberElement.getAttributes()
                .getNamedItem("decimal")
                .getNodeValue();
            strGroupingSeparator = NumberElement.getAttributes()
                .getNamedItem("grouping")
                .getNodeValue();
            strNumberFormat = strFormatOutput;
          }
        }
      }
    } catch (final Exception e) {
      log4j.error("error reading number format", e);
    }
    vars.setSessionObject("#FormatMap", formatMap);
    vars.setSessionValue("#AD_ReportNumberFormat", strNumberFormat);
    vars.setSessionValue("#AD_ReportGroupingSeparator", strGroupingSeparator);
    vars.setSessionValue("#AD_ReportDecimalSeparator", strDecimalSeparator);
  }

  /**
   * It saves session in the DB when logging in
   * 
   */
  public static void saveLoginBD(HttpServletRequest request, VariablesSecureApp vars,
      String strCliente, String strOrganizacion) throws ServletException {

    if ("Y".equals(request.getSession().getAttribute("forceLogin"))) {
      // don't create a DB session for bypass authentication resources
      log4j.debug("Bypass session " + request.getRequestURI());
      return;
    }

    final SessionLogin sl = new SessionLogin(request, strCliente, strOrganizacion,
        vars.getSessionValue("#AD_User_ID"));

    // session_ID should have been created in LoginHandler
    String sessionId = vars.getDBSession();
    sl.setServerUrl(HttpBaseUtils.getLocalAddress(request));
    sl.setSessionID(sessionId);

    if (sessionId == null || sessionId.equals("")) {
      sl.setStatus("S");
      sl.save();
      vars.setSessionValue("#AD_Session_ID", sl.getSessionID());
    }
  }

  /**
   * Returns default language
   * 
   * @throws ServletException
   * 
   */
  public static String getDefaultLanguage(ConnectionProvider connectionProvider, String aduserid)
      throws ServletException {
    DefaultOptionsData dataLanguage[] = DefaultOptionsData.defaultLanguage(connectionProvider,
        aduserid);
    if (dataLanguage != null && dataLanguage.length > 0) {
      return dataLanguage[0].getField("DEFAULT_AD_LANGUAGE");
    }
    dataLanguage = DefaultOptionsData.getDefaultLanguage(connectionProvider);
    if (dataLanguage != null && dataLanguage.length > 0) {
      return dataLanguage[0].getField("DEFAULT_AD_LANGUAGE");
    }
    return null;
  }

  /**
   * Returns default value for Rtl
   * 
   * @throws ServletException
   * 
   */
  public static String isDefaultRtl(ConnectionProvider connectionProvider, String aduserid)
      throws ServletException {

    DefaultOptionsData dataLanguage[] = DefaultOptionsData.defaultLanguage(connectionProvider,
        aduserid);
    if (dataLanguage != null && dataLanguage.length > 0) {
      return dataLanguage[0].getField("ISRTL");
    }
    dataLanguage = DefaultOptionsData.getDefaultLanguage(connectionProvider);
    if (dataLanguage != null && dataLanguage.length > 0) {
      return dataLanguage[0].getField("ISRTL");
    }
    return null;

  }

  /**
   * Returns default warehouse for a given organization
   * 
   * @throws ServletException
   * 
   */
  public static String getDefaultWarehouse(ConnectionProvider connectionProvider, String strClient,
      String strOrg, String strRole) throws ServletException {
    long t = System.currentTimeMillis();
    String strWarehouse;
    if (!strRole.equals("0")) {
      // Pick the warehouse using the given organization
      strWarehouse = DefaultOptionsData.getDefaultWarehouse(connectionProvider, strClient,
          "'" + strOrg + "'");
      if (strWarehouse == null || strWarehouse.isEmpty()) {
        // If no warehouse for the default organization is available, pick using using the
        // accessible tree

        OrganizationStructureProvider osp = OBContext.getOBContext()
            .getOrganizationStructureProvider(strClient);

        Set<String> orgNaturalTree = osp.getNaturalTree(strOrg);

        strWarehouse = DefaultOptionsData.getDefaultWarehouse(connectionProvider, strClient,
            StringCollectionUtils.commaSeparated(orgNaturalTree));
      }
    } else {
      strWarehouse = "";
    }
    log4j.debug("getDefaultWarehouse " + (System.currentTimeMillis() - t));
    return strWarehouse;
  }
}
