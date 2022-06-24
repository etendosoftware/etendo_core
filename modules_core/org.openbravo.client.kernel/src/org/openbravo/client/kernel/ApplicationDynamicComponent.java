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
 * All portions are Copyright (C) 2010-2020 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openbravo.base.model.Entity;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.utility.OBVersion;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.SystemInformation;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * The component responsible for generating some dynamic elements of the application js file which
 * are related to the user of the current context.
 * 
 * @author mtaal
 */
public class ApplicationDynamicComponent extends SessionDynamicTemplateComponent {

  private ActivationKey activationKey;
  private Map<String, String> imageProperties;

  @Override
  public String getId() {
    return KernelConstants.APPLICATION_DYNAMIC_COMPONENT_ID;
  }

  @Override
  protected String getTemplateId() {
    return KernelConstants.APPLICATION_DYNAMIC_TEMPLATE_ID;
  }

  public Set<Entity> getAccessibleEntities() {
    final Set<Entity> entities = OBContext.getOBContext()
        .getEntityAccessChecker()
        .getReadableEntities();
    entities.addAll(OBContext.getOBContext().getEntityAccessChecker().getWritableEntities());
    return entities;
  }

  public User getUser() {
    return OBContext.getOBContext().getUser();
  }

  public Client getClient() {
    return OBContext.getOBContext().getCurrentClient();
  }

  public Organization getOrganization() {
    return OBContext.getOBContext().getCurrentOrganization();
  }

  public Role getRole() {
    return OBContext.getOBContext().getRole();
  }

  public String getLanguageId() {
    return OBContext.getOBContext().getLanguage().getId();
  }

  public String getLanguage() {
    return OBContext.getOBContext().getLanguage().getLanguage();
  }

  public Map<String, String> getCompanyImageLogoData() {
    if (imageProperties != null) {
      return imageProperties;
    }
    imageProperties = new HashMap<>();
    Image img = Utility.getImageLogoObject("yourcompanymenu", "");
    String imageWidth = "122";
    String imageHeight = "34";
    if (img != null) {
      if (img.getWidth() != null) {
        imageWidth = String.valueOf(img.getWidth().intValue());
      }
      if (img.getHeight() != null) {
        imageHeight = String.valueOf(img.getHeight().intValue());
      }
    }
    imageProperties.put("width", imageWidth);
    imageProperties.put("height", imageHeight);
    return imageProperties;
  }

  public String getSystemVersion() {
    return KernelUtils.getInstance().getVersionParameters(getModule());
  }

  public String getInstancePurpose() {
    final String purpose = OBDal.getInstance()
        .get(SystemInformation.class, "0")
        .getInstancePurpose();
    if (purpose == null) {
      return "";
    }
    return purpose;
  }

  private ActivationKey getActivationKey() {
    if (activationKey == null) {
      activationKey = ActivationKey.getInstance();
    }
    return activationKey;
  }

  public String getLicenseType() {
    return getActivationKey().getLicenseClass().getCode();
  }

  public String getTrialStringValue() {
    return Boolean.toString(getActivationKey().isTrial());
  }

  public String getGoldenStringValue() {
    return Boolean.toString(getActivationKey().isGolden());
  }

  public Set<String> getWritableOrganizations() {
    return OBContext.getOBContext().getWritableOrganizations();
  }

  public String getActiveInstanceStringValue() {
    if (SessionFactoryController.isRunningInWebContainer()) {
      return Boolean.toString(ActivationKey.isActiveInstance());
    }
    return Boolean.FALSE.toString();
  }

  public String getVersionDescription() {
    ActivationKey ak = getActivationKey();
    String strVersion = OBVersion.getInstance().getMajorVersion();
    strVersion += " - ";
    strVersion += Utility.getListValueName("OBPSLicenseEdition", ak.getLicenseClass().getCode(),
        "en_US");

    if (ak.isTrial()) {
      strVersion += " - ";
      strVersion += Utility.messageBD(new DalConnectionProvider(false), "OPSTrial",
          OBContext.getOBContext().getLanguage().getLanguage());
    }

    strVersion += " - ";
    strVersion += OBVersion.getInstance().getMP();
    return strVersion;
  }

  public String getCsrfToken() {
    String token = (String) RequestContext.get().getSessionAttribute("#CSRF_TOKEN");
    return token != null ? token : "";
  }

  public String getCommunityBrandingUrl() {
    return Utility.getCommunityBrandingUrl();
  }
}
