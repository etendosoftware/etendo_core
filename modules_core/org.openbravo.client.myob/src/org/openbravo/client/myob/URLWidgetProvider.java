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
 * All portions are Copyright (C) 2010-2011 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.myob;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.NonUniqueResultException;

import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.*;
import org.openbravo.userinterface.selector.*;

/**
 * Responsible for creating the URL Widgets.
 * 
 * @author mtaal
 */
@RequestScoped
public class URLWidgetProvider extends WidgetProvider {
  private static final Logger log = LogManager.getLogger();

  private static final String URLWIDGETCLASSNAME = "OBUrlWidget";
  private static final String SRC = "src";

  @Override
  public String generate() {
    throw new UnsupportedOperationException(
        "URLWidget definition should be pre-loaded on the client");
  }

  @Override
  public String getClientSideWidgetClassName() {
    return URLWIDGETCLASSNAME;
  }

  @Override
  public JSONObject getWidgetClassDefinition() {
    try {
      final JSONObject jsonObject = super.getWidgetClassDefinition();
      final JSONObject parameters = new JSONObject();
      jsonObject.put(WidgetProvider.PARAMETERS, parameters);
      try {
        Map<String, Object> filters = new HashMap<>();
        filters.put(WidgetURL.PROPERTY_WIDGETCLASS, getWidgetClass());
        OBCriteria<WidgetURL> selFieldsCrit = OBDao.getFilteredCriteria(WidgetURL.class, filters);
        final WidgetURL widgetURL = (WidgetURL) selFieldsCrit.uniqueResult();
        if (widgetURL != null) {
          parameters.put(SRC, widgetURL.getURL());
        } else {
          log.warn("URLWidget does not have a URL defined.");
          parameters.put(SRC, "");
        }
      } catch (NonUniqueResultException e) {
        log.warn("URLWidget has more than one active URL defined.", e);
        parameters.put(SRC, "");
      }
      if (jsonObject.getJSONArray(WidgetProvider.FIELDDEFINITIONS).length() > 0) {
        log.warn("URLWidget does not support parameters. Ignoring field definitions.");
        jsonObject.put(WidgetProvider.FIELDDEFINITIONS, new JSONArray());
      }
      return jsonObject;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  @Override
  public JSONObject getWidgetInstanceDefinition(WidgetInstance widgetInstance) {
    final JSONObject jsonObject = new JSONObject();
    try {
      addDefaultWidgetProperties(jsonObject, widgetInstance);
      final JSONObject parameters = jsonObject.getJSONObject(WidgetProvider.PARAMETERS);
      Map<String, Object> filters = new HashMap<>();
      filters.put(WidgetURL.PROPERTY_WIDGETCLASS, getWidgetClass());
      OBCriteria<WidgetURL> selFieldsCrit = OBDao.getFilteredCriteria(WidgetURL.class, filters);
      final WidgetURL widgetURL = (WidgetURL) selFieldsCrit.uniqueResult();
      if (widgetURL != null) {
        parameters.put(SRC, widgetURL.getURL());
      } else {
        log.error("No url widget defined for widget class " + widgetInstance.getWidgetClass());
      }
    } catch (NonUniqueResultException e) {
      log.error("More than one active url defined for widget " + widgetInstance.getWidgetClass(),
          e);
    } catch (Exception e) {
      throw new OBException(e);
    }
    return jsonObject;
  }

  @Override
  public boolean validate() {
    if (getWidgetClass() != null && getWidgetClass().getOBKMOWidgetURLList().isEmpty()) {
      log.error("No url widget defined for widget class " + getWidgetClass().getIdentifier());
      return false;
    }
    return true;
  }
}
