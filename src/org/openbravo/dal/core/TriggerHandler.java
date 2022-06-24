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
 * All portions are Copyright (C) 2008-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.core;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.util.Check;
import org.openbravo.dal.service.OBDal;

/**
 * Supports disabling and again enabling of database triggers.
 * 
 * The user of this class should call disable() after beginning the transaction and enable at the
 * end, before committing.
 * 
 * @author martintaal
 */

public class TriggerHandler {
  private static final Logger log = LogManager.getLogger();

  private static TriggerHandler instance;
  private boolean isPostgreSQL = "POSTGRE".equals(
      OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("bbdd.rdbms"));

  public static TriggerHandler getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(TriggerHandler.class);
    }
    return instance;
  }

  private ThreadLocal<Boolean> sessionStatus = new ThreadLocal<Boolean>();

  /** Disables all triggers in the database */
  public void disable() {
    log.debug("Disabling triggers");
    Check.isNull(sessionStatus.get(),
        "Triggers were already disabled in this session, call enable before calling disable again");
    Connection con = OBDal.getInstance().getConnection();
    try (PreparedStatement ps = con.prepareStatement(getDisableStatement())) {
      ps.execute();
      sessionStatus.set(Boolean.TRUE);
    } catch (Exception e) {
      throw new OBException("Couldn't disable triggers: ", e);
    }
  }

  /**
   * @return true if the database triggers are disabled, false in other cases.
   */
  public boolean isDisabled() {
    return sessionStatus.get() != null;
  }

  /**
   * Clears the SessionStatus from the threadlocal, must be done in case of rollback
   */
  public void clear() {
    sessionStatus.set(null);
  }

  /** Enables triggers in the database */
  public void enable() {
    log.debug("Enabling triggers");
    Check.isNotNull(sessionStatus.get(),
        "Triggers were not disabled in this session, call disable before calling this method");

    try {
      Connection con = OBDal.getInstance().getConnection();
      try (PreparedStatement ps = con.prepareStatement(getEnableStatement())) {
        ps.execute();
      }
    } catch (Exception e) {
      throw new OBException("Couldn't enable triggers: ", e);
    } finally {
      // always clear the threadlocal
      clear();
    }
  }

  private String getDisableStatement() {
    if (isPostgreSQL) {
      return "SELECT set_config('my.triggers_disabled','Y',true)";
    } else {
      return "INSERT INTO AD_SESSION_STATUS (ad_session_status_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, isimporting)"
          + " VALUES (get_uuid(), '0', '0', 'Y', now(), '0', now(), '0', 'Y')";
    }
  }

  private String getEnableStatement() {
    if (isPostgreSQL) {
      return "SELECT set_config('my.triggers_disabled','N',true)";
    } else {
      return "DELETE FROM AD_SESSION_STATUS WHERE isimporting = 'Y'";
    }
  }

}
