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

package org.openbravo.erpCommon.ad_forms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.utility.SQLReturnObject;
import org.openbravo.service.centralrepository.CentralRepository;
import org.openbravo.service.centralrepository.CentralRepository.Service;

/**
 * Maintains a list of possible maturity levels for a module. This list is maintained in central
 * repository.
 * 
 */
@SuppressWarnings("serial")
public class MaturityLevel implements Serializable {
  private static final Logger log = LogManager.getLogger();
  public static final int CS_MATURITY = 500;
  public static final int QA_APPR_MATURITY = 200;

  private transient List<Level> levels;
  private transient boolean error = false;

  /**
   * Calls central repository webservice to obtain the list of possible statuses. In case the
   * service is not available or the request fails, the list is initialized with 500-Production.
   */
  public MaturityLevel() {
    log.debug("Connecting to Internet to obtain maturity levels");
    try {
      JSONObject jsonLevels = CentralRepository.executeRequest(Service.MATURITY_LEVEL);
      levels = Level.getFrom(jsonLevels.getJSONObject("response").getJSONArray("levels"));
    } catch (final Exception e) {
      log.error("Error obtaining maturity levels", e);
      error = true;
    }

    if (error) {
      // could not obtain actual levels, setting Confirmed Stable only
      log.warn("Setting default Confirmed Stable level");
      if (ActivationKey.getInstance().isActive()) {
        levels = Arrays.asList(new Level(MaturityLevel.CS_MATURITY, "Confirmed Stable"));
      } else {
        levels = Arrays.asList(new Level(MaturityLevel.QA_APPR_MATURITY, "QA Approved"));
      }
    }
  }

  /**
   * Obtains the FieldProvider[][] to populate the statuses drop down list.
   */
  public FieldProvider[] getCombo() {
    FieldProvider[] rt = new FieldProvider[levels.size()];
    int i = 0;
    for (Level level : levels) {
      SQLReturnObject l = new SQLReturnObject();
      l.setData("ID", Integer.toString(level.value));
      l.setData("NAME", level.name);
      rt[i] = l;
      i++;
    }
    return rt;
  }

  /**
   * Returns the name associated to a level
   */
  public String getLevelName(String maturityLevel) {
    int ml = Integer.parseInt(maturityLevel);
    return levels.stream()
        .filter(l -> l.value == ml)
        .map(Level::getName)
        .findAny()
        .orElseGet(() -> {
          log.warn("Could not find maturity level {}", maturityLevel);
          return "--";
        });
  }

  /**
   * Returns the less mature level name. Assumes the list is ordered
   */
  public String getLessMature() {
    return levels.get(0).getName();
  }

  /**
   * Returns the most mature level name. Assumes the list is ordered
   */
  public String getMostMature() {
    return levels.get(levels.size() - 1).getName();
  }

  boolean hasInternetError() {
    return error;
  }

  private static class Level {
    int value;
    String name;

    Level(int value, String name) {
      this.value = value;
      this.name = name;
    }

    String getName() {
      return name;
    }

    static List<Level> getFrom(JSONArray json) throws JSONException {
      List<Level> levels = new ArrayList<>(json.length());
      for (int i = 0; i < json.length(); i++) {
        JSONObject l = json.getJSONObject(i);
        levels.add(new Level(l.getInt("level"), l.getString("name")));
      }
      return levels;
    }
  }
}
