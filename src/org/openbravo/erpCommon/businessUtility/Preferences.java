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
 * All portions are Copyright (C) 2010-2019 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.businessUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.PropertyConflictException;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Handles preferences, resolving priorities in case there are values for a same property at
 * different visibility levels
 * 
 */
public class Preferences {
  private static final Logger log4j = LogManager.getLogger();
  private static final String SYSTEM = "0";
  public static final String YES = "Y";
  public static final String NO = "N";

  public enum QueryFilter {
    ACTIVE, CLIENT, ORGANIZATION
  }

  /**
   * Obtains a list of all preferences that are applicable at the given visibility level (client,
   * org, user, role).
   * <p>
   * In case of different values for a single property at a same visibility level, one of them is
   * taken.
   */
  public static List<Preference> getAllPreferences(String client, String org, String user,
      String role) {
    try {
      OBContext.setAdminMode();
      List<String> parentTree = OBContext.getOBContext()
          .getOrganizationStructureProvider()
          .getParentList(org, true);

      List<Preference> allPreferences = getPreferences(null, false, client, org, user, role, null,
          false, false, null);
      Map<String, Preference> preferences = new HashMap<>(allPreferences.size());
      for (Preference pref : allPreferences) {
        String prefKey = getPrefKey(pref);
        Preference existentPreference = preferences.get(prefKey);
        if (existentPreference == null) {
          // There is not a preference for the current property, add it to the list
          preferences.put(prefKey, pref);
        } else if (getHighestPriority(pref, existentPreference, parentTree) == 1) {
          // There is a preference for the current property, check whether it is higher priority and
          // if so replace it. In case of conflict leave current preference.
          preferences.put(prefKey, pref);
        }
      }
      return new ArrayList<>(preferences.values());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static String getPrefKey(Preference pref) {
    return pref.isPropertyList() //
        + (pref.isPropertyList() ? pref.getProperty() : pref.getAttribute()) //
        + (pref.getWindow() != null ? pref.getWindow().getId() : "");
  }

  /**
   * Saves the property/value as a preference. If a preference with exactly the same visualization
   * priority already exists, it is overwritten; if not, a new one is created.
   * <p>
   * It also saves the new preference in session, in case the vars parameter is not null. If it is
   * null, the preference is not stored in session.
   * 
   * @param property
   *          Name of the property or attribute for the preference.
   * @param value
   *          New value to set.
   * @param isListProperty
   *          Determines whether list of properties or attribute should be used.
   * @param client
   *          Client visibility.
   * @param org
   *          Organization visibility.
   * @param user
   *          User visibility.
   * @param role
   *          Role visibility.
   * @param window
   *          Window visibility.
   * @param vars
   *          VariablesSecureApp to store new property value.
   * @return The preference that has been created or modified
   */
  public static Preference setPreferenceValue(String property, String value, boolean isListProperty,
      Client client, Organization org, User user, Role role, Window window,
      VariablesSecureApp vars) {
    try {
      OBContext.setAdminMode();
      Preference preference;
      String clientId = client == null ? null : client.getId();
      String orgId = org == null ? null : org.getId();
      String userId = user == null ? null : user.getId();
      String roleId = role == null ? null : role.getId();
      String windowId = window == null ? null : window.getId();

      List<Preference> prefs = getPreferences(property, isListProperty, clientId, orgId, userId,
          roleId, windowId, true, true, null);
      if (prefs.size() == 0) {
        // New preference
        preference = OBProvider.getInstance().get(Preference.class);
        preference.setClient(OBDal.getInstance().get(Client.class, "0"));
        preference.setOrganization(OBDal.getInstance().get(Organization.class, "0"));

        preference.setPropertyList(isListProperty);
        if (isListProperty) {
          preference.setProperty(property);
        } else {
          preference.setAttribute(property);
        }
        preference.setVisibleAtClient(client);
        preference.setVisibleAtOrganization(org);
        preference.setVisibleAtRole(role);
        preference.setUserContact(user);
        preference.setWindow(window);
      } else {
        // Rewrite value (assume there's no conflicting properties
        preference = prefs.get(0);
      }
      preference.setSearchKey(value);
      OBDal.getInstance().save(preference);

      if (vars != null) {
        savePreferenceInSession(vars, preference);
      }
      return preference;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Obtains the value for a given property with the visibility defined by the parameters. In case
   * of conflict or the property is not defined an exception is thrown.
   * <p>
   * This method is used to query in database for the property value, note that when properties are
   * set, they are also saved as session values and it is possible to obtain them using
   * {@link Utility#getPreference(VariablesSecureApp, String, String) Utility.getPreference}.
   * 
   * @throws PropertyException
   *           if the property cannot be resolved in a single value:
   *           <ul>
   *           <li>{@link PropertyNotFoundException} if the property is not defined.
   *           <li>{@link PropertyConflictException} in case of conflict
   *           </ul>
   */
  public static String getPreferenceValue(String property, boolean isListProperty, Client client,
      Organization org, User user, Role role, Window window) throws PropertyException {
    try {
      OBContext.setAdminMode();
      String clientId = client == null ? null : client.getId();
      String orgId = org == null ? null : org.getId();
      String userId = user == null ? null : user.getId();
      String roleId = role == null ? null : role.getId();
      String windowId = window == null ? null : window.getId();
      return getPreferenceValue(property, isListProperty, clientId, orgId, userId, roleId,
          windowId);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * @see Preferences#getPreferenceValue(String, boolean, Client, Organization, User, Role, Window)
   */
  public static String getPreferenceValue(String property, boolean isListProperty, String clientId,
      String orgId, String userId, String roleId, String windowId) throws PropertyException {
    return getPreferenceValue(property, isListProperty, clientId, orgId, userId, roleId, windowId,
        null);
  }

  /**
   * @see Preferences#getPreferenceValue(String, boolean, Client, Organization, User, Role, Window)
   */
  public static String getPreferenceValue(String property, boolean isListProperty, String clientId,
      String orgId, String userId, String roleId, String windowId,
      Map<QueryFilter, Boolean> queryFilters) throws PropertyException {
    OBContext.setAdminMode();
    try {
      List<Preference> prefs = getPreferences(property, isListProperty, clientId, orgId, userId,
          roleId, windowId, false, true, queryFilters);
      Preference selectedPreference = null;
      List<String> parentTree = OBContext.getOBContext()
          .getOrganizationStructureProvider(clientId)
          .getParentList(orgId, true);
      boolean conflict = false;
      for (Preference preference : prefs) {
        // select the highest priority or raise exception in case of conflict
        if (selectedPreference == null) {
          selectedPreference = preference;
          continue;
        }
        int higherPriority = getHighestPriority(selectedPreference, preference, parentTree);
        switch (higherPriority) {
          case 1:
            // do nothing, selected one has higher priority
            break;
          case 2:
            selectedPreference = preference;
            conflict = false;
            break;
          default:
            conflict = true;
            break;
        }
      }
      if (conflict) {
        throw new PropertyConflictException();
      }
      if (selectedPreference == null) {
        throw new PropertyNotFoundException();
      }
      return selectedPreference.getSearchKey();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Utility method to determine if exists a preference with the same settings passed as parameters
   * 
   * @param property
   *          Name of the property or attribute for the preference.
   * @param isListProperty
   *          Determines whether list of properties or attribute should be used.
   * @param clientId
   *          Client visibility.
   * @param orgId
   *          Organization visibility.
   * @param userId
   *          User visibility.
   * @param roleId
   *          Role visibility.
   * @param windowId
   *          Window visibility.
   * @return true if exists a preference with the same settings passed as parameters, false
   *         otherwise
   */
  public static boolean existsPreference(String property, boolean isListProperty, String clientId,
      String orgId, String userId, String roleId, String windowId) {
    List<Preference> prefs = getPreferences(property, isListProperty, clientId, orgId, userId,
        roleId, windowId, true, true, null);
    return (prefs.size() > 0);
  }

  /**
   * Utility method to determine if exists a preference with the same settings as the preference
   * passed as parameter
   * 
   * @param preference
   *          the preference to check
   * @return true if exists a preference with the same settings passed as parameters, false
   *         otherwise
   */
  public static boolean existsPreference(Preference preference) {
    String property = preference.isPropertyList() ? preference.getProperty()
        : preference.getAttribute();
    String clientId = preference.getVisibleAtClient() != null
        ? preference.getVisibleAtClient().getId()
        : null;
    String orgId = preference.getVisibleAtOrganization() != null
        ? preference.getVisibleAtOrganization().getId()
        : null;
    String userId = preference.getUserContact() != null ? preference.getUserContact().getId()
        : null;
    String roleId = preference.getVisibleAtRole() != null ? preference.getVisibleAtRole().getId()
        : null;
    String windowId = preference.getWindow() != null ? preference.getWindow().getId() : null;
    return existsPreference(property, preference.isPropertyList(), clientId, orgId, userId, roleId,
        windowId);
  }

  /**
   * Utility method which returns a list of preferences with the settings passed as parameters. The
   * resulting list includes the preferences which are not active (if any).
   * 
   * @param property
   *          Name of the property or attribute for the preference.
   * @param isListProperty
   *          Determines whether list of properties or attribute should be used.
   * @param clientId
   *          Client visibility.
   * @param orgId
   *          Organization visibility.
   * @param userId
   *          User visibility.
   * @param roleId
   *          Role visibility.
   * @param windowId
   *          Window visibility.
   * @return a list of preference with the same settings as those passed as parameters
   */
  public static List<Preference> getPreferences(String property, boolean isListProperty,
      String clientId, String orgId, String userId, String roleId, String windowId) {
    Map<QueryFilter, Boolean> queryFilters = new HashMap<>();
    queryFilters.put(QueryFilter.ACTIVE, false);
    queryFilters.put(QueryFilter.CLIENT, true);
    queryFilters.put(QueryFilter.ORGANIZATION, true);
    return getPreferences(property, isListProperty, clientId, orgId, userId, roleId, windowId, true,
        true, queryFilters);
  }

  /**
   * Stores the preference as a session value
   * 
   * @param vars
   *          VariablesSecureApp of the current session to store preference in
   * @param preference
   *          Preference to save in session
   */
  public static void savePreferenceInSession(VariablesSecureApp vars, Preference preference) {
    String prefName = "P|"
        + (preference.getWindow() == null ? "" : (preference.getWindow().getId() + "|"))
        + (preference.isPropertyList() ? preference.getProperty() : preference.getAttribute());
    vars.setSessionValue(prefName, preference.getSearchKey());
    log4j.debug("Set preference " + prefName + " - " + preference.getSearchKey());
  }

  /**
   * Obtains a list of preferences. All the parameters can be null; when a parameter is null, it
   * will not be used in the filtering for the preference.
   * <p>
   * exactMatch parameter determines whether the returned list of properties matches exactly the
   * visibility defined by the parameters, or if it is obtained any preference that is applicable to
   * the given visibility. For no exact match, visibility prioritization and conflicts are not
   * resolved in this method.
   * 
   */
  private static List<Preference> getPreferences(String property, boolean isListProperty,
      String client, String org, String user, String role, String window, boolean exactMatch,
      boolean checkWindow, Map<QueryFilter, Boolean> queryFilters) {

    Map<String, Object> parameters = new HashMap<>();
    //@formatter:off
    String hql = 
            " as p " + 
            " where ";
    //@formatter:on
    if (exactMatch) {
      if (client != null) {
        hql += " p.visibleAtClient.id = :clientId ";
        parameters.put("clientId", client);
      } else {
        hql += " p.visibleAtClient is null ";
      }
      if (org != null) {
        hql += " and p.visibleAtOrganization.id = :orgId ";
        parameters.put("orgId", org);
      } else {
        hql += " and p.visibleAtOrganization is null ";
      }

      if (user != null) {
        hql += " and p.userContact.id = :userId ";
        parameters.put("userId", user);
      } else {
        hql += " and p.userContact is null ";
      }

      if (role != null) {
        hql += " and p.visibleAtRole.id = :roleId";
        parameters.put("roleId", role);
      } else {
        hql += " and p.visibleAtRole is null ";
      }

      if (window != null) {
        hql += " and p.window.id = :windowId ";
        parameters.put("windowId", window);
      } else {
        hql += " and p.window is null ";
      }
    } else {
      if (client != null) {
        hql += " (p.visibleAtClient.id = :clientId or ";
        parameters.put("clientId", client);
      } else {
        hql += " (";
      }
      hql += " coalesce(p.visibleAtClient, '0')='0') ";

      if (role != null) {
        hql += " and (p.visibleAtRole.id = :roleId or ";
        parameters.put("roleId", role);
      } else {
        hql += " and (";
      }
      hql += " p.visibleAtRole is null) ";

      List<String> parentOrgs;
      if (org == null) {
        parentOrgs = Arrays.asList("0");
      } else {
        parentOrgs = OBContext.getOBContext()
            .getOrganizationStructureProvider(client)
            .getParentList(org, true);
      }

      hql += " and coalesce(p.visibleAtOrganization.id, '0') in :parentOrgs";
      parameters.put("parentOrgs", parentOrgs);

      if (user != null) {
        hql += " and (p.userContact.id = :userId or ";
        parameters.put("userId", user);
      } else {
        hql += " and (";
      }
      hql += " p.userContact is null) ";
      if (checkWindow) {
        if (window != null) {
          hql += " and (p.window.id = :windowId or ";
          parameters.put("windowId", window);
        } else {
          hql += " and (";
        }
        hql += " p.window is null) ";
      }
    }

    if (property != null) {
      hql += " and p.propertyList = :isListProperty";
      parameters.put("isListProperty", isListProperty);
      if (isListProperty) {
        hql += " and p.property = :property ";
      } else {
        hql += " and p.attribute = :property";
      }
      parameters.put("property", property);
    }

    hql += " order by p.id";

    OBQuery<Preference> qPref = OBDal.getInstance().createQuery(Preference.class, hql);
    qPref.setNamedParameters(parameters);
    if (queryFilters != null && queryFilters.size() > 0) {
      qPref.setFilterOnActive(queryFilters.get(QueryFilter.ACTIVE));
      qPref.setFilterOnReadableClients(queryFilters.get(QueryFilter.CLIENT));
      qPref.setFilterOnReadableOrganization(queryFilters.get(QueryFilter.ORGANIZATION));
    }
    return qPref.list();
  }

  /**
   * Determines which of the 2 preferences has higher visibility priority.
   * 
   * @param pref1
   *          First preference to compare
   * @param pref2
   *          Second preference to compare
   * @param parentTree
   *          Parent tree of organizations including the current one, used to assign more priority
   *          to organizations nearer in the tree.
   * @return
   *         <ul>
   *         <li>1 in case pref1 is more visible than pref2
   *         <li>2 in case pref2 is more visible than pref1
   *         <li>0 in case of conflict (both have identical visibility and value)
   *         </ul>
   */
  private static int getHighestPriority(Preference pref1, Preference pref2,
      List<String> parentTree) {
    // Check priority by client

    // undefined client visibility is handled as system
    String clientId1 = pref1.getVisibleAtClient() == null ? SYSTEM
        : pref1.getVisibleAtClient().getId();
    String clientId2 = pref2.getVisibleAtClient() == null ? SYSTEM
        : pref2.getVisibleAtClient().getId();
    if (!SYSTEM.equals(clientId1) && SYSTEM.equals(clientId2)) {
      return 1;
    }

    if (SYSTEM.equals(clientId1) && !SYSTEM.equals(clientId2)) {
      return 2;
    }

    // Check priority by organization
    Organization org1 = pref1.getVisibleAtOrganization();
    Organization org2 = pref2.getVisibleAtOrganization();
    if (org1 != null && org2 == null) {
      return 1;
    }

    if (org1 == null && org2 != null) {
      return 2;
    }

    if (org1 != null && org2 != null) {
      int depth1 = parentTree.indexOf(org1.getId());
      int depth2 = parentTree.indexOf(org2.getId());

      if (depth1 < depth2) {
        return 1;
      } else if (depth1 > depth2) {
        return 2;
      }
    }

    // Check priority by user
    if (pref1.getUserContact() != null && pref2.getUserContact() == null) {
      return 1;
    }

    if (pref1.getUserContact() == null && pref2.getUserContact() != null) {
      return 2;
    }

    // Check priority by role
    if (pref1.getVisibleAtRole() != null && pref2.getVisibleAtRole() == null) {
      return 1;
    }

    if (pref1.getVisibleAtRole() == null && pref2.getVisibleAtRole() != null) {
      return 2;
    }

    // Check window
    if (pref1.getWindow() != null && pref2.getWindow() == null) {
      return 1;
    }

    if (pref1.getWindow() == null && pref2.getWindow() != null) {
      return 2;
    }

    // Same priority, check selected
    if (pref1.isSelected() && !pref2.isSelected()) {
      return 1;
    }

    if (!pref1.isSelected() && pref2.isSelected()) {
      return 2;
    }

    if ((pref1.getSearchKey() == null && pref2.getSearchKey() == null)
        || (pref1.getSearchKey() != null && pref2.getSearchKey() != null
            && pref1.getSearchKey().equals(pref2.getSearchKey()))) {
      // Conflict with same value, it does not matter priority
      return 2;
    }

    // Actual conflict
    return 0;
  }

}
