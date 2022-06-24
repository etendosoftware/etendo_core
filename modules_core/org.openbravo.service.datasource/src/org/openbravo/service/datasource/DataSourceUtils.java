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
 * All portions are Copyright (C) 2014-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.datasource;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.service.json.JsonUtils;

/**
 * This class contains utility methods for dataSource related classes
 * 
 */
public class DataSourceUtils {

  private static final Logger log = LogManager.getLogger();

  /**
   * Returns a comma separated list of organization ids to filter the HQL. If an organization id is
   * provided its natural tree is returned. If no organization is provided or the given value is
   * invalid the readable organizations are returned.
   */
  public static String getOrgs(String orgId) {
    StringBuffer orgPart = new StringBuffer();
    if (StringUtils.isNotEmpty(orgId)) {
      final Set<String> orgSet = OBContext.getOBContext()
          .getOrganizationStructureProvider()
          .getNaturalTree(orgId);
      if (orgSet.size() > 0) {
        boolean addComma = false;
        for (String org : orgSet) {
          if (addComma) {
            orgPart.append(",");
          }
          orgPart.append("'" + org + "'");
          addComma = true;
        }
      }
    }
    if (orgPart.length() == 0) {
      String[] orgs = OBContext.getOBContext().getReadableOrganizations();
      boolean addComma = false;
      for (int i = 0; i < orgs.length; i++) {
        if (addComma) {
          orgPart.append(",");
        }
        orgPart.append("'" + orgs[i] + "'");
        addComma = true;
      }
    }
    return orgPart.toString();
  }

  /**
   * Extracts the criteria from a request parameter map and returns the number of selected records
   * according to that criteria.
   */
  public static int getNumberOfSelectedRecords(Map<String, String> parameters) {
    boolean hasCriteria = parameters.containsKey("criteria");
    if (!hasCriteria) {
      return 0;
    }
    return getSelectedRecordsFromCriteria(JsonUtils.buildCriteria(parameters)).size();
  }

  /**
   * Returns a set of selected record IDs from a criteria included in the JSONObject received as
   * parameter.
   */
  public static Set<String> getSelectedRecordsFromCriteria(JSONObject buildCriteria) {
    if (buildCriteria == null) {
      return Collections.emptySet();
    }
    Set<String> selectedRecords = new HashSet<>();
    try {
      JSONArray criteriaArray = buildCriteria.getJSONArray("criteria");
      for (int i = 0; i < criteriaArray.length(); i++) {
        JSONObject criteria = criteriaArray.getJSONObject(i);
        if (criteria.has("fieldName") && criteria.getString("fieldName").equals("id")
            && criteria.has("value")) {
          String value = criteria.getString("value");
          for (String recordId : value.split(",")) {
            selectedRecords.add(recordId.trim());
          }
        }
      }
    } catch (JSONException e) {
      log.error("Error getting selected records from criteria {}", buildCriteria, e);
    }
    return selectedRecords;
  }
}
