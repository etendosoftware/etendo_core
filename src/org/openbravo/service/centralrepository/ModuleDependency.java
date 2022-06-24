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
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;

/** Marshals module dependency objects returned from {@link CentralRepository} service requests */
public class ModuleDependency {
  private String moduleID;
  private String moduleName;
  private String moduleVersionDependencyID;
  private String versionEnd;
  private String versionStart;

  public ModuleDependency() {
  }

  private ModuleDependency(String moduleID, String moduleName, String moduleVersionDependencyID,
      String versionEnd, String versionStart) {
    this.moduleID = moduleID;
    this.moduleName = moduleName;
    this.moduleVersionDependencyID = moduleVersionDependencyID;
    this.versionEnd = versionEnd;
    this.versionStart = versionStart;
  }

  /** Marshals json from central repository service into a module dependency array instance */
  public static ModuleDependency[] fromJson(JSONArray jsonArray) {
    ModuleDependency[] deps = new ModuleDependency[jsonArray.length()];
    try {
      for (int i = 0; i < jsonArray.length(); i++) {
        deps[i] = fromJson(jsonArray.getJSONObject(i));
      }
      return deps;
    } catch (JSONException e) {
      throw new OBException(e);
    }
  }

  private static ModuleDependency fromJson(JSONObject jsonDep) {
    try {
      String moduleID = jsonDep.getString("moduleID");
      String moduleName = jsonDep.getString("moduleName");
      String moduleVersionDependencyID = jsonDep.getString("moduleVersionDependencyID");
      String versionStart = jsonDep.getString("versionStart");
      String versionEnd = jsonDep.isNull("versionEnd") ? null : jsonDep.getString("versionEnd");
      return new ModuleDependency(moduleID, moduleName, moduleVersionDependencyID, versionEnd,
          versionStart);
    } catch (JSONException e) {
      throw new OBException(e);
    }
  }

  public String getModuleID() {
    return moduleID;
  }

  public void setModuleID(String moduleID) {
    this.moduleID = moduleID;
  }

  public String getModuleName() {
    return moduleName;
  }

  public void setModuleName(String moduleName) {
    this.moduleName = moduleName;
  }

  public String getModuleVersionDependencyID() {
    return moduleVersionDependencyID;
  }

  public void setModuleVersionDependencyID(String moduleVersionDependencyID) {
    this.moduleVersionDependencyID = moduleVersionDependencyID;
  }

  public String getVersionEnd() {
    return versionEnd;
  }

  public void setVersionEnd(String versionEnd) {
    this.versionEnd = versionEnd;
  }

  public String getVersionStart() {
    return versionStart;
  }

  public void setVersionStart(String versionStart) {
    this.versionStart = versionStart;
  }

}
