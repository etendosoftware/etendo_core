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

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;

/** Marshals module objects returned from {@link CentralRepository} service requests */
public class Module {
  private String author;
  private String dbPrefix;
  private ModuleDependency[] dependencies;
  private String description;
  private String help;
  private ModuleDependency[] includes;
  private String licenseAgreement;
  private String licenseType;
  private String moduleID;
  private String moduleVersionID;
  private String name;
  private String packageName;
  private String type;
  private String updateDescription;
  private String url;
  private String versionNo;
  private boolean isCommercial;
  private Map<String, Object> additionalInfo;

  public Module() {
  }

  private Module(String author, String dbPrefix, ModuleDependency[] dependencies,
      String description, String help, ModuleDependency[] includes, String licenseAgreement,
      String licenseType, String moduleID, String moduleVersionID, String name, String packageName,
      String type, String updateDescription, String url, String versionNo, boolean isCommercial,
      Map<String, Object> additionalInfo) {
    this.author = author;
    this.dbPrefix = dbPrefix;
    this.dependencies = dependencies;
    this.description = description;
    this.help = help;
    this.includes = includes;
    this.licenseAgreement = licenseAgreement;
    this.licenseType = licenseType;
    this.moduleID = moduleID;
    this.moduleVersionID = moduleVersionID;
    this.name = name;
    this.packageName = packageName;
    this.type = type;
    this.updateDescription = updateDescription;
    this.url = url;
    this.versionNo = versionNo;
    this.isCommercial = isCommercial;
    this.additionalInfo = additionalInfo;
  }

  /** Marshals json from central repository service into a module instance */
  public static Module fromJson(JSONObject jsonModule) {
    try {
      String author = jsonModule.getString("author");
      String dbPrefix = jsonModule.getString("dbPrefix");
      String description = jsonModule.getString("description");
      String help = jsonModule.getString("help");
      String licenseAgreement = jsonModule.getString("licenseAgreement");
      String licenseType = jsonModule.getString("licenseType");
      String moduleID = jsonModule.getString("moduleID");
      String moduleVersionID = jsonModule.getString("moduleVersionID");
      String name = jsonModule.getString("name");
      String packageName = jsonModule.getString("packageName");
      String type = jsonModule.getString("type");
      String updateDescription = jsonModule.getString("updateDescription");
      String url = jsonModule.getString("url");
      String versionNo = jsonModule.getString("versionNo");
      boolean isCommercial = jsonModule.getBoolean("isCommercial");

      Map<String, Object> additionalInfo = new HashMap<>();
      if (jsonModule.get("additionalInfo") instanceof JSONObject) {
        JSONObject jsonAdditionalInfo = jsonModule.getJSONObject("additionalInfo");
        JSONArray keys = jsonAdditionalInfo.names();

        for (int i = 0; i < keys.length(); i++) {
          String key = keys.getString(i);
          additionalInfo.put(key, jsonAdditionalInfo.getString(key));
        }
      }
      ModuleDependency[] dependencies = ModuleDependency
          .fromJson(jsonModule.getJSONArray("dependencies"));
      ModuleDependency[] includes = ModuleDependency.fromJson(jsonModule.getJSONArray("includes"));

      return new Module(author, dbPrefix, dependencies, description, help, includes,
          licenseAgreement, licenseType, moduleID, moduleVersionID, name, packageName, type,
          updateDescription, url, versionNo, isCommercial, additionalInfo);
    } catch (JSONException e) {
      throw new OBException(e);
    }
  }

  /** Marshals json from central repository service into a module array instance */
  public static Module[] fromJson(JSONArray jsonModules) {
    Module[] modules = new Module[jsonModules.length()];
    try {
      for (int i = 0; i < jsonModules.length(); i++) {
        modules[i] = fromJson(jsonModules.getJSONObject(i));
      }
    } catch (JSONException e) {
      throw new OBException(e);
    }
    return modules;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getDbPrefix() {
    return dbPrefix;
  }

  public void setDbPrefix(String dbPrefix) {
    this.dbPrefix = dbPrefix;
  }

  public ModuleDependency[] getDependencies() {
    return dependencies;
  }

  public void setDependencies(ModuleDependency[] dependencies) {
    this.dependencies = dependencies;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getHelp() {
    return help;
  }

  public void setHelp(String help) {
    this.help = help;
  }

  public ModuleDependency[] getIncludes() {
    return includes;
  }

  public void setIncludes(ModuleDependency[] includes) {
    this.includes = includes;
  }

  public String getLicenseAgreement() {
    return licenseAgreement;
  }

  public void setLicenseAgreement(String licenseAgreement) {
    this.licenseAgreement = licenseAgreement;
  }

  public String getLicenseType() {
    return licenseType;
  }

  public void setLicenseType(String licenseType) {
    this.licenseType = licenseType;
  }

  public String getModuleID() {
    return moduleID;
  }

  public void setModuleID(String moduleID) {
    this.moduleID = moduleID;
  }

  public String getModuleVersionID() {
    return moduleVersionID;
  }

  public void setModuleVersionID(String moduleVersionID) {
    this.moduleVersionID = moduleVersionID;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getUpdateDescription() {
    return updateDescription;
  }

  public void setUpdateDescription(String updateDescription) {
    this.updateDescription = updateDescription;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getVersionNo() {
    return versionNo;
  }

  public void setVersionNo(String versionNo) {
    this.versionNo = versionNo;
  }

  public boolean isIsCommercial() {
    return isCommercial;
  }

  public void setIsCommercial(boolean isCommercial) {
    this.isCommercial = isCommercial;
  }

  public Map<String, Object> getAdditionalInfo() {
    return additionalInfo;
  }

  public void setAdditionalInfo(Map<String, Object> additionalInfo) {
    this.additionalInfo = additionalInfo;
  }
}
