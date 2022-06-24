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
 * All portions are Copyright (C) 2010-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.attachment.AttachmentUtils;
import org.openbravo.client.application.attachment.AttachmentWindowComponent;
import org.openbravo.client.application.window.ParameterWindowComponent;
import org.openbravo.client.application.window.StandardWindowComponent;
import org.openbravo.client.kernel.BaseComponent;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.OBUserException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.obps.ActivationKey.FeatureRestriction;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.ModelImplementation;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.ad.utility.AttachmentMethod;
import org.openbravo.userinterface.selector.Selector;

/**
 * Reads the view and generates it.
 * 
 * @author mtaal
 */
@RequestScoped
public class ViewComponent extends BaseComponent {

  private static final String BUTTON_REFERENCE_ID = "28";
  private static final String SELECTOR_REFERENCE_ID = "95E2A8B50A254B2AAE6774B8C2F28120";

  private static Logger log = LogManager.getLogger();

  @Inject
  private StandardWindowComponent standardWindowComponent;

  @Inject
  private ParameterWindowComponent parameterWindowComponent;

  @Inject
  private AttachmentWindowComponent attachmentWindowComponent;

  @Inject
  private WeldUtils weldUtils;

  @Override
  public String generate() {
    long t = System.currentTimeMillis();
    final String viewId = getParameter("viewId");
    if (viewId == null) {
      throw new IllegalArgumentException("viewId parameter not found, it is mandatory");
    }

    try {
      OBContext.setAdminMode();

      final Window window = adcs.getWindow(correctViewId(viewId));

      if (window != null) {
        FeatureRestriction featureRestriction = ActivationKey.getInstance()
            .hasLicenseAccess("MW", window.getId());
        if (featureRestriction != FeatureRestriction.NO_RESTRICTION) {
          throw new OBUserException(featureRestriction.toString());
        }
        verifyOldCalloutUse(window);
        verifyUnsupportedCustomQuerySelector(window);
        return generateWindow(window);
      } else if (viewId.startsWith("processDefinition_")) {
        String processId = viewId.substring("processDefinition_".length());
        Process process = OBDal.getInstance().get(Process.class, processId);
        if (process == null) {
          throw new IllegalArgumentException("Not found process definition with ID " + processId);
        }
        return generateProcess(process);
      } else if (viewId.startsWith("attachment_")) {
        return generateAttachment(viewId);
      } else {
        return generateView(viewId);
      }
    } catch (Exception e) {
      log.error("Error generating view {}", viewId, e);
      throw e;
    } finally {
      // view generation is read only, remove from session whatever DAL loaded to make faster flush
      OBDal.getInstance().getSession().clear();

      OBContext.restorePreviousMode();
      log.debug("View {} generated in {} ms", viewId, System.currentTimeMillis() - t);
    }
  }

  private void verifyUnsupportedCustomQuerySelector(Window window) {
    window.getADTabList()
        .forEach(tab -> tab.getADFieldList().forEach(this::verifyUnsupportedCustomQuerySelector));

  }

  private void verifyUnsupportedCustomQuerySelector(Field field) {
    Column column = field.getColumn();
    if (column == null) {
      return;
    }
    Reference reference = column.getReference();
    if (reference.getId().equals(SELECTOR_REFERENCE_ID)) {
      try {
        checkReference(column.getReferenceSearchKey());
      } catch (Exception e) {
        String hql = e.getMessage();
        throw new IllegalStateException(String.format(
            "Wrong selector definition in (Window, Tab, Field) (%s, %s, %s), missing @additional_filters@ clause: %s",
            field.getTab().getWindow().getName(), field.getTab().getName(), field.getName(), hql),
            e);
      }
    } else if (reference.getId().equals(BUTTON_REFERENCE_ID)) {
      checkParameterWindow(column.getOBUIAPPProcess());
    }
  }

  private void checkParameterWindow(Process process) {
    if (process == null) {
      return;
    }
    getParameterList(process).forEach(this::verifyUnsupportedCustomQuerySelector);
  }

  private List<Parameter> getParameterList(Process process) {
    OBCriteria<Parameter> criteria = OBDal.getInstance().createCriteria(Parameter.class);
    criteria.add(Restrictions.eq(Parameter.PROPERTY_OBUIAPPPROCESS, process));
    criteria.setFilterOnReadableClients(false);
    criteria.setFilterOnReadableOrganization(false);
    return criteria.list();
  }

  private void verifyUnsupportedCustomQuerySelector(Parameter parameter) {
    Reference reference = parameter.getReference();
    if (reference.getId().equals(SELECTOR_REFERENCE_ID)) {
      try {
        checkReference(parameter.getReferenceSearchKey());
      } catch (IllegalStateException e) {
        String hql = e.getMessage();
        throw new IllegalStateException(String.format(
            "Wrong selector definition in parameter %s of standard process %s, missing @additional_filters@ clause: %s",
            parameter.getName(), parameter.getObuiappProcess().getName(), hql), e);
      }
    }
  }

  private void checkReference(Reference reference) {
    if (reference == null) {
      return;
    }
    List<Selector> selectorList = reference.getOBUISELSelectorList();
    if (selectorList.isEmpty()) {
      return;
    }
    Selector selector = selectorList.get(0);
    String hql = selector.getHQL();
    if (!StringUtils.isBlank(hql) && !hql.toLowerCase().contains("@additional_filters@")) {
      throw new IllegalStateException(hql);
    }
  }

  private void verifyOldCalloutUse(final Window window) {
    window.getADTabList().forEach(tab -> tab.getADFieldList().forEach(this::verifyCallout));
  }

  /**
   * Verifies if a field contains a callout and if it is implemented using SimpleCallout class, if
   * not, it throws an IllegalStateException an exception
   *
   * @param fld
   *          Field to verify its callout
   */
  private void verifyCallout(Field fld) {
    if (fld.getColumn() != null && fld.getColumn().getCallout() != null) {
      List<ModelImplementation> modelImplementations = fld.getColumn()
          .getCallout()
          .getADModelImplementationList();
      final String windowName = fld.getTab().getWindow().getName();
      final String tabName = fld.getTab().getName();
      final String fldName = fld.getName();
      if (!modelImplementations.isEmpty()) {
        // Get first model implementation that will contain the callout class name
        ModelImplementation mi = modelImplementations.get(0);
        String calloutClassName = mi.getJavaClassName();
        try {
          if (!SimpleCallout.class.isAssignableFrom(
              Class.forName(calloutClassName, false, ViewComponent.class.getClassLoader()))) {
            throw new IllegalStateException(String.format(
                "A callout(not based on SimpleCallout) %s is present in (Window, Tab, Field) (%s, %s, %s). Fix or remove this callout.",
                calloutClassName, windowName, tabName, fldName));
          }
        } catch (ClassNotFoundException e) {
          throw new IllegalStateException(
              String.format("Class %s not found for callout in (Window, Tab, Field) (%s, %s, %s).",
                  calloutClassName, windowName, tabName, fldName),
              e);
        }
      } else {
        throw new OBException(
            String.format("Callout %s in (Window, Tab, Field) (%s, %s, %s), has no implementation.",
                fld.getColumn().getCallout(), windowName, tabName, fldName));
      }
    }
  }

  protected String generateWindow(Window window) {
    standardWindowComponent.setWindow(window);
    standardWindowComponent.setParameters(getParameters());
    final String jsCode = standardWindowComponent.generate();
    return jsCode;
  }

  protected String generateView(String viewName) {
    OBUIAPPViewImplementation viewImpDef = getView(viewName);

    final BaseTemplateComponent component;
    if (viewImpDef.getJavaClassName() != null) {
      try {
        @SuppressWarnings("unchecked")
        final Class<BaseTemplateComponent> clz = (Class<BaseTemplateComponent>) OBClassLoader
            .getInstance()
            .loadClass(viewImpDef.getJavaClassName());
        component = weldUtils.getInstance(clz);
      } catch (Exception e) {
        throw new OBException(e);
      }
    } else {
      component = weldUtils.getInstance(BaseTemplateComponent.class);
      if (viewImpDef.getTemplate() == null) {
        throw new IllegalStateException("No class and no template defined for view " + viewName);
      }
    }
    component.setId(viewImpDef.getId());
    component.setComponentTemplate(viewImpDef.getTemplate());
    component.setParameters(getParameters());

    final String jsCode = component.generate();
    return jsCode;
  }

  protected String generateProcess(Process process) {
    parameterWindowComponent.setProcess(process);
    parameterWindowComponent.setParameters(getParameters());
    parameterWindowComponent.setPoup(false);
    return parameterWindowComponent.generate();
  }

  protected String generateAttachment(String viewId) {
    String[] keys = viewId.split(KernelConstants.ID_PREFIX);
    String tabId = keys[1];
    Tab tab = adcs.getTab(tabId);
    if (tab == null) {
      throw new IllegalArgumentException("Not found process definition with ID " + tabId);
    }
    AttachmentMethod attMethod;
    if (keys.length >= 3) {
      String strAttMethodId = keys[2];
      if (StringUtils.isEmpty(strAttMethodId)) {
        // In case the attachment was created with old attachments.
        attMethod = AttachmentUtils.getDefaultAttachmentMethod();
      } else {
        attMethod = OBDal.getInstance().get(AttachmentMethod.class, strAttMethodId);
      }
    } else {
      // When uploading an attachment ("Add" button) AttachmentMethod is not sent, so there are less
      // than 3 elements in the array
      attMethod = AttachmentUtils.getAttachmentMethod();
    }
    attachmentWindowComponent.initialize(tab, attMethod);
    attachmentWindowComponent.setParameters(getParameters());
    return attachmentWindowComponent.generate();
  }

  private OBUIAPPViewImplementation getView(String viewName) {
    OBCriteria<OBUIAPPViewImplementation> obc = OBDal.getInstance()
        .createCriteria(OBUIAPPViewImplementation.class);
    obc.add(Restrictions.or(Restrictions.eq(OBUIAPPViewImplementation.PROPERTY_NAME, viewName),
        Restrictions.eq(OBUIAPPViewImplementation.PROPERTY_ID, viewName)));

    if (obc.list().size() > 0) {
      return obc.list().get(0);
    } else {
      throw new IllegalArgumentException("No view found using id/name " + viewName);
    }
  }

  @Override
  public Module getModule() {
    final String id = getParameter("viewId");
    final Window window = adcs.getWindow(correctViewId(id));
    if (window != null) {
      return window.getModule();
    } else if (id.startsWith("processDefinition_")) {
      String processId = id.substring("processDefinition_".length());
      Process process = OBDal.getInstance().get(Process.class, processId);
      if (process == null) {
        throw new IllegalArgumentException("Not found process definition with ID " + processId);
      }
      return process.getModule();
    } else if (id.startsWith("attachment_")) {
      String[] keys = id.split(KernelConstants.ID_PREFIX);
      String tabId = keys[1];
      Tab tab = adcs.getTab(tabId);
      if (tab == null) {
        throw new IllegalArgumentException("Not found tab with ID " + tabId);
      }
      return tab.getModule();
    } else {
      OBUIAPPViewImplementation view = getView(id);
      if (view != null) {
        return view.getModule();
      } else {
        return super.getModule();
      }
    }
  }

  protected String correctViewId(String viewId) {
    // the case if a window is in development and has a unique making postfix
    // see the StandardWindowComponent.getWindowClientClassName method
    // changes made here should also be done there
    String correctedViewId = (viewId.startsWith(KernelConstants.ID_PREFIX) ? viewId.substring(1)
        : viewId);
    // if in consultants mode, do another conversion
    if (correctedViewId.contains(KernelConstants.ID_PREFIX)) {
      final int index = correctedViewId.indexOf(KernelConstants.ID_PREFIX);
      correctedViewId = correctedViewId.substring(0, index);
    }
    return correctedViewId;
  }

  @Override
  public Object getData() {
    return this;
  }

  @Override
  public String getETag() {
    String etag = super.getETag();
    return etag + "_" + getViewVersionHash();
  }

  /**
   * This function returns the last grid configuration change made into a window at any level (at
   * whole system level or just a for particuar tab or field).
   * 
   * This value is needed for the eTag calculation, so, if there has been any grid configuration
   * change, the eTag should change in order to load again the view definition.
   * 
   * @param window
   *          the window to obtain its last grid configuration change
   * @return a String with the last grid configuration change
   */
  private String getLastGridConfigurationChange(Window window) {
    Date lastModification = new Date(0);

    List<GCSystem> sysConfs = OBDal.getInstance().createQuery(GCSystem.class, "").list();
    if (!sysConfs.isEmpty()) {
      if (lastModification.compareTo(sysConfs.get(0).getUpdated()) < 0) {
        lastModification = sysConfs.get(0).getUpdated();
      }
    }

    String tabHql = "select max(updated) from OBUIAPP_GC_Tab where tab.window.id = :windowId";
    Query<Date> qryTabData = OBDal.getInstance().getSession().createQuery(tabHql, Date.class);
    qryTabData.setParameter("windowId", window.getId());
    Date tabUpdated = qryTabData.uniqueResult();
    if (tabUpdated != null && lastModification.compareTo(tabUpdated) < 0) {
      lastModification = tabUpdated;
    }

    String fieldHql = "select max(updated) from OBUIAPP_GC_Field where obuiappGcTab.tab.window.id = :windowId";
    Query<Date> qryFieldData = OBDal.getInstance().getSession().createQuery(fieldHql, Date.class);
    qryFieldData.setParameter("windowId", window.getId());
    Date fieldUpdated = qryFieldData.uniqueResult();
    if (fieldUpdated != null && lastModification.compareTo(fieldUpdated) < 0) {
      lastModification = fieldUpdated;
    }

    return lastModification.toString();
  }

  private synchronized String getViewVersionHash() {
    final String viewId = getParameter("viewId");
    OBContext.setAdminMode();
    try {
      String fixedViewId = correctViewId(viewId);
      Window window = null;
      if (!"processDefinition".equals(fixedViewId)) {
        window = adcs.getWindow(correctViewId(viewId));
      }
      if (window == null) {
        return "";
      }

      StringBuilder viewVersions = new StringBuilder();
      viewVersions.append(getAuditStatus(window)) //
          .append(getLastGridConfigurationChange(window))
          .append("|") //
          .append(getLastSystemPreferenceChange(window));
      return DigestUtils.md5Hex(viewVersions.toString());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String getLastSystemPreferenceChange(Window window) {
    Set<String> preferences = new HashSet<String>();

    Pattern p = Pattern.compile(DynamicExpressionParser.REPLACE_DISPLAY_LOGIC_SERVER_PATTERN);
    for (String displayLogic : getFieldsWithDisplayLogicAtServerLevel(window.getId())) {
      Matcher m = p.matcher(displayLogic);
      while (m.find()) {
        preferences.add(m.group(1));
      }
    }

    Date updated = !preferences.isEmpty() ? getLastUpdated(preferences) : null;
    return updated == null ? "" : updated.toString();
  }

  private List<String> getFieldsWithDisplayLogicAtServerLevel(String windowID) {
    String where = " select displayLogicEvaluatedInTheServer" //
        + " from ADField as f" //
        + " where f.displayLogicEvaluatedInTheServer is not null" //
        + " and f.tab.id in (select t.id" //
        + "                  from ADTab t" //
        + "                  where t.window.id = :windowId)";

    Session session = OBDal.getInstance().getSession();
    Query<String> query = session.createQuery(where, String.class);
    query.setParameter("windowId", windowID);

    return query.list();
  }

  private Date getLastUpdated(Set<String> preferenceSet) {
    String where = " select max(p.updated)" //
        + " from ADPreference p" //
        + " where p.propertyList = true" //
        + " and p.property in :properties" //
        + " and p.client.id = '0'" //
        + " and p.organization = '0'" //
        + " and coalesce(p.visibleAtClient, '0') = '0'" //
        + " and coalesce(p.visibleAtOrganization, '0') = '0'";

    Session session = OBDal.getInstance().getSession();
    Query<Date> query = session.createQuery(where, Date.class);
    query.setParameterList("properties", preferenceSet);
    Date lastUpdated = query.uniqueResult();

    return lastUpdated;
  }

  private String getAuditStatus(Window window) {
    String where = "select t.table.isFullyAudited " //
        + "  from ADTab t " //
        + " where t.window = :window " //
        + " order by t.sequenceNumber, t.id"; //
    Query<Boolean> q = OBDal.getInstance().getSession().createQuery(where, Boolean.class);
    q.setParameter("window", window);

    return Arrays.asList(q.list()).toString();
  }
}
