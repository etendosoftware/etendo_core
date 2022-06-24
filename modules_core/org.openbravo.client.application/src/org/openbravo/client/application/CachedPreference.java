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
 * All portions are Copyright (C) 2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.SessionScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * This class is used to keep the value of some preferences in cache during the life cycle of a
 * session, avoiding the time spent to compute the preference value. The preference values that can
 * be cached by this class are those defined at System level.
 * 
 */
@SessionScoped
public class CachedPreference implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger log = LogManager.getLogger();

  public static final String ALLOW_UNPAGED_DS_MANUAL_REQUEST = "OBJSON_AllowUnpagedDatasourceManualRequest";
  public static final String ALLOW_UNSECURED_DS_REQUEST = "OBSERDS_AllowUnsecuredDatasourceRequest";
  public static final String ALLOW_WHERE_PARAMETER = "OBSERDS_AllowWhereParameter";
  public static final String RESTRICT_ERP_ACCESS_IN_STORE_SERVER = "RestrictErpAccessInStoreServer";
  public static final String RANK_NORMALIZATION = "FullTextSearchRankNormalization";

  private List<String> propertyList = new ArrayList<String>(
      Arrays.asList(ALLOW_UNPAGED_DS_MANUAL_REQUEST, ALLOW_UNSECURED_DS_REQUEST,
          ALLOW_WHERE_PARAMETER, RESTRICT_ERP_ACCESS_IN_STORE_SERVER, RANK_NORMALIZATION));
  private transient Map<String, String> cachedPreference;

  /**
   * It returns a String with the value of the preference whose related property name is entered as
   * parameter. In case the value is not stored in cache, then the value will be retrieved from
   * database.
   * 
   * @param propertyName
   *          The name of the property related to the preference
   * 
   * @return A String with the value of the cached preference
   */
  public String getPreferenceValue(String propertyName) {
    long t = System.nanoTime();
    if (cachedPreference == null) {
      cachedPreference = new HashMap<String, String>();
    }
    if (!cachedPreference.containsKey(propertyName)) {
      try {
        OBContext.setAdminMode(false);
        Client systemClient = OBDal.getInstance().get(Client.class, "0");
        Organization asterisk = OBDal.getInstance().get(Organization.class, "0");
        String value = Preferences.getPreferenceValue(propertyName, true, systemClient, asterisk,
            null, null, null);
        setPreferenceValue(propertyName, value);
      } catch (PropertyException ignore) {
        // Ignore the exception, caused because the preference was not found
        setPreferenceValue(propertyName, null);
      } finally {
        OBContext.restorePreviousMode();
      }
    }
    log.debug("preference value retrieved in {} ns", (System.nanoTime() - t));
    return cachedPreference.get(propertyName);
  }

  /**
   * Return the Preference value and store it into the cached variable
   * 
   * @param propertyName
   *          The name of the property related to the preference
   * @return The preference value of the propertyName given
   */
  public String getPreferenceValueAndStoreInCache(String propertyName) {
    String result = getPreferenceValue(propertyName);
    addCachedPreference(propertyName);
    return result;
  }

  /**
   * Checks if the preference related to the property name entered as parameter is contained in the
   * list of cached preferences.
   * 
   * @param propertyName
   *          The name of the property related to the preference
   * @return true if the preference related to the property name is a cached preference, false
   *         otherwise
   */
  public boolean isCachedPreference(String propertyName) {
    return propertyList.contains(propertyName);
  }

  /**
   * Sets the cached value of the preference. This method is defined as synchronized in order to
   * avoid concurrency problems.
   * 
   * @param propertyName
   *          The name of the property related to the preference
   * @param preferenceValue
   *          String with the value assigned to the preference
   */
  public synchronized void setPreferenceValue(String propertyName, String preferenceValue) {
    if (cachedPreference == null) {
      cachedPreference = new HashMap<String, String>();
    }
    cachedPreference.put(propertyName, preferenceValue);
  }

  /**
   * Invalidates the cached value of the preference. This method is defined as synchronized in order
   * to avoid concurrency problems.
   * 
   * @param propertyName
   *          The name of the property related to the preference
   */
  public synchronized void invalidatePreferenceValue(String propertyName) {
    if (cachedPreference != null) {
      cachedPreference.remove(propertyName);
    }
  }

  /**
   * Adds a new preference into the set of preferences whose value is stored in cache.
   * 
   * @param propertyName
   *          The name of the property related to the preference to be cached
   */
  public void addCachedPreference(String propertyName) {
    if (!isCachedPreference(propertyName)) {
      propertyList.add(propertyName);
    }
  }
}
