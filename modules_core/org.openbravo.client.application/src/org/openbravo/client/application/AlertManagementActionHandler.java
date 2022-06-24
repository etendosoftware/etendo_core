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
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.model.ad.alert.Alert;

/**
 * @author gorkaion
 * 
 */
@ApplicationScoped
public class AlertManagementActionHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();
  private static final String MOVE_TO_STATUS = "moveToStatus";

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.client.kernel.BaseActionHandler#execute(java.util.Map, java.lang.String)
   */
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject object = new JSONObject();

    OBContext.setAdminMode();
    try {
      JSONObject o = new JSONObject(content);
      final String strEventType = o.getString("eventType");
      if (MOVE_TO_STATUS.equals(strEventType)) {
        final String alertIDs = o.getString("alertIDs");
        final String oldStatus = o.getString("oldStatus");
        final String newStatus = o.getString("newStatus");
        setNewStatus(alertIDs, newStatus);
        object.put("oldStatus", oldStatus);
        object.put("newStatus", newStatus);
      } else {
        log.error("Unsupported event type: " + strEventType);
      }

    } catch (JSONException e) {
      log.error("Error executing action: " + e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return object;
  }

  private void setNewStatus(String alertIDs, String newStatus) {
    if (StringUtils.isEmpty(alertIDs)) {
      return;
    }
    List<Alert> alerts = OBDao.getOBObjectListFromString(Alert.class, alertIDs);
    for (Alert alert : alerts) {
      alert.setAlertStatus(newStatus.toUpperCase());
      OBDal.getInstance().save(alert);
    }
    OBDal.getInstance().flush();
  }
}
