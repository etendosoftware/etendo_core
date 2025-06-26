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
 * All portions are Copyright (C) 2017-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.io.File;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.SessionDynamicTemplateComponent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.ddlutils.util.DBSMOBUtil;
import org.openbravo.erpCommon.ad_process.HeartbeatProcess;
import org.openbravo.erpCommon.ad_process.HeartbeatProcess.HeartBeatOrRegistration;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.module.Module;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * The component responsible for generating the content of the function used to determine if the
 * HeartBeat or any kind of registration pop-up should be displayed once the user has logged in into
 * the application.
 */
public class HeartBeatPopUpComponent extends SessionDynamicTemplateComponent {

  private static final Logger log = LogManager.getLogger();
  private static final String APRM_MIGRATION_TOOL_ID = "4BD3D4B262B048518FE62496EF09D549";
  private static final String COMPONENT_ID = "HeartbeatRegistration";
  private static final String TEMPLATE_ID = "EE5CEC203AEA4B039CCDAD0BE8E07E3C";

  @Override
  public String getId() {
    return COMPONENT_ID;
  }

  @Override
  protected String getTemplateId() {
    return TEMPLATE_ID;
  }

  public String getHeartBeatRegistrationFunction() {
    try {
      if (isUpgrading()) {
        if (!isSystemAdmin()) {
          return "OB.Layout.ClassicOBCompatibility.Popup.standardUpgrading()";
        } else {
          boolean usingAprm = true;
          if (OBDal.getInstance().exists(Module.ENTITY_NAME, APRM_MIGRATION_TOOL_ID)) {
            usingAprm = new AdvPaymentMngtDao().existsAPRMReadyPreference();
          }
          if (!usingAprm) {
            return "OB.Layout.ClassicOBCompatibility.Popup.openAPRMPopup()";
          }
          String oldScripts = getConfigScriptsNotExported();
          if (!oldScripts.isEmpty()) {
            return "OB.Layout.ClassicOBCompatibility.Popup.openConfigScriptPopup(" + oldScripts
                + ")";
          } else {
            return "OB.Layout.ClassicOBCompatibility.Popup.openSuccessUpgradePopup()";
          }
        }
      }

      switch (getPopUpToShow()) {
        case InstancePurpose:
          return "OB.Layout.ClassicOBCompatibility.Popup.openInstancePurpose()";
        case HeartBeat:
          return "OB.Layout.ClassicOBCompatibility.Popup.openHeartbeat()";
        default:
          return "return";
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  private boolean isUpgrading() {
    boolean isUpgrading;
    try {
      isUpgrading = Preferences.YES
          .equals(Preferences.getPreferenceValue("isUpgrading", true, "0", "0", null, null, null));
    } catch (PropertyException ignore) {
      isUpgrading = false;
    }
    return isUpgrading;
  }

  private boolean isSystemAdmin() {
    return "S".equals(OBContext.getOBContext().getRole().getUserLevel());
  }

  private String getConfigScriptsNotExported() {
    // Get all applied configuration scripts which are not exported in 3.0
    OBCriteria<Module> qMod = OBDal.getInstance().createCriteria(Module.class);
    qMod.add(Restrictions.eq(Module.PROPERTY_TYPE, "T"));
    qMod.add(Restrictions.eq(Module.PROPERTY_ENABLED, true));
    qMod.add(Restrictions.eq(Module.PROPERTY_APPLYCONFIGURATIONSCRIPT, true));
    String obDir = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("source.path");
    String oldScripts = "";
    for (Module mod : qMod.list()) {
      File cfScript = new File(obDir + "/modules/" + mod.getJavaPackage() + "/src-db/database",
          "configScript.xml");
      if (cfScript.exists() && DBSMOBUtil.isOldConfigScript(cfScript)) {
        if (!oldScripts.isEmpty()) {
          oldScripts += ", ";
        }
        oldScripts += "'" + mod.getName() + "'";
        log.info(mod.getName() + " config script is not exported in 3.0");
      }
    }
    return oldScripts;
  }

  private HeartBeatOrRegistration getPopUpToShow() throws ServletException {
    Object sessionObject = getParameters().get(KernelConstants.HTTP_SESSION);
    if (sessionObject == null) {
      // not showing any pop-up
      log.info("Could not check the type of registration pop-up to be displayed");
      return HeartBeatOrRegistration.None;
    }
    HttpSession session = (HttpSession) sessionObject;
    String roleId = (String) session.getAttribute("#AD_ROLE_ID");
    String javaDateFormat = (String) session.getAttribute("#AD_JAVADATEFORMAT");
    return HeartbeatProcess.isLoginPopupRequired(roleId, javaDateFormat,
        new DalConnectionProvider(false));
  }
}
