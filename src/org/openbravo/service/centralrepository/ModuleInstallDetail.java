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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):
 ************************************************************************
 */

package org.openbravo.service.centralrepository;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;

/**
 * Marshals module details for installation objects returned from {@link CentralRepository} service
 * requests
 */
public class ModuleInstallDetail {
  private String[] dependencyErrors;
  private Module[] modulesToInstall;
  private Module[] modulesToUpdate;
  private boolean validConfiguration;

  private ModuleInstallDetail(String[] dependencyErrors, Module[] modulesToInstall,
      Module[] modulesToUpdate, boolean validConfiguration) {
    this.dependencyErrors = dependencyErrors;
    this.modulesToInstall = modulesToInstall;
    this.modulesToUpdate = modulesToUpdate;
    this.validConfiguration = validConfiguration;
  }

  /**
   * Marshals json from central repository service into a module details for installation instance
   */
  public static ModuleInstallDetail fromJson(JSONObject jsonResponse) {
    try {
      JSONObject jsonDetail = jsonResponse.getJSONObject("response");

      JSONArray errors = jsonDetail.getJSONArray("dependencyErrors");
      String[] dependencyErrors = new String[errors.length()];
      for (int i = 0; i < errors.length(); i++) {
        dependencyErrors[i] = errors.getString(i);
      }

      Module[] modulesToInstall = Module.fromJson(jsonDetail.getJSONArray("modulesToInstall"));
      Module[] modulesToUpdate = Module.fromJson(jsonDetail.getJSONArray("modulesToUpdate"));

      boolean validConfing = jsonDetail.getBoolean("validConfiguration");
      return new ModuleInstallDetail(dependencyErrors, modulesToInstall, modulesToUpdate,
          validConfing);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  public String[] getDependencyErrors() {
    return dependencyErrors;
  }

  public void setDependencyErrors(String[] dependencyErrors) {
    this.dependencyErrors = dependencyErrors;
  }

  public Module[] getModulesToInstall() {
    return modulesToInstall;
  }

  public void setModulesToInstall(Module[] modulesToInstall) {
    this.modulesToInstall = modulesToInstall;
  }

  public Module[] getModulesToUpdate() {
    return modulesToUpdate;
  }

  public void setModulesToUpdate(Module[] modulesToUpdate) {
    this.modulesToUpdate = modulesToUpdate;
  }

  public boolean isValidConfiguration() {
    return validConfiguration;
  }

  public void setValidConfiguration(boolean validConfiguration) {
    this.validConfiguration = validConfiguration;
  }
}
