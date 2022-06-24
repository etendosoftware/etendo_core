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
 * All portions are Copyright (C) 2012 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.obps;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.DalInitializingTask;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.domain.List;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.system.System;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * This class provides an ant task to activate instance in command line without need of use UI.
 * 
 * It can be executed with <code>ant activate.instance</code>
 * 
 * @author alostale
 * 
 */
public class ActivationTask extends DalInitializingTask {

  final static Logger log = LogManager.getLogger();

  private static final String PURPOSE_REFERENCE_ID = "60E231391A7348DDA7171E780F62EF99";

  private String publicKey;
  private File publicKeyFile;
  private String purpose;

  @Override
  protected void doExecute() {
    verifyParameters();
    VariablesSecureApp vars = new VariablesSecureApp("0", "0", "0");

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("activate", true);
    params.put("purpose", purpose);
    params.put("publicKey", publicKey);

    ProcessBundle pb = new ProcessBundle(null, vars);
    pb.setParams(params);

    try {
      new ActiveInstanceProcess().execute(pb);
      OBError msg = (OBError) pb.getResult();
      boolean success = "Success".equals(msg.getType());
      String msgTxt = Utility.parseTranslation(new DalConnectionProvider(false), vars, "en_US",
          msg.getMessage());

      if (success) {
        // Setting updated to force license reload
        OBDal.getInstance().get(System.class, "0").setUpdated(new Date());
        log.info(msgTxt);
      } else {
        throw new BuildException(msgTxt);
      }
    } catch (Exception e) {
      throw new BuildException(e);
    }
  }

  private void verifyParameters() {
    // Purpose
    if (StringUtils.isEmpty(purpose)) {
      throw new BuildException("purpose parameter is required");
    } else {
      Reference purposeRef = OBDal.getInstance().get(Reference.class, PURPOSE_REFERENCE_ID);
      String msg = "";
      boolean foundValue = false;
      for (List value : purposeRef.getADListList()) {
        if (purpose.equals(value.getSearchKey())) {
          foundValue = true;
          break;
        }
        msg += "\n  *" + value.getSearchKey() + " [" + value.getName() + "]";
      }
      if (!foundValue) {
        throw new BuildException(
            purpose + " is not one of the valid values for purpose parameter:" + msg);
      }
    }

    // Public key
    if (StringUtils.isEmpty(publicKey)
        && (publicKeyFile == null || getProject().getBaseDir().equals(publicKeyFile))) {
      throw new BuildException(
          "Public key must be provided through one of publicKey or publicKeyFile parameter");
    }

    if (StringUtils.isNotEmpty(publicKey) && publicKeyFile != null
        && !getProject().getBaseDir().equals(publicKeyFile)) {
      throw new BuildException(
          "Only one of the publicKey or publicKeyFile parameter can be set at the same time");
    }

    if (publicKeyFile != null && !getProject().getBaseDir().equals(publicKeyFile)) {
      try {
        publicKey = FileUtils.readFileToString(publicKeyFile, "utf-8");
      } catch (IOException e) {
        throw new BuildException(e);
      }
      if (StringUtils.isEmpty(publicKey)) {
        throw new BuildException(publicKeyFile.getAbsolutePath() + " is empty");
      }
    }
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  public void setPublicKeyFile(File publicKeyFile) {
    this.publicKeyFile = publicKeyFile;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }
}
