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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * 
 * @author iperdomo
 */
@ApplicationScoped
@ComponentProvider.Qualifier(ApplicationConstants.COMPONENT_TYPE)
public class ApplicationComponentProvider extends BaseComponentProvider {
  public static final String QUALIFIER = ApplicationConstants.COMPONENT_TYPE;

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.client.kernel.ComponentProvider#getComponent(java.lang.String,
   * java.util.Map)
   */
  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    if (componentId.equals(ApplicationConstants.MAIN_LAYOUT_ID)) {
      final MainLayoutComponent component = getComponent(MainLayoutComponent.class);
      component.setId(ApplicationConstants.MAIN_LAYOUT_ID);
      component.setParameters(parameters);
      return component;
    } else if (componentId.equals(ApplicationConstants.MAIN_LAYOUT_VIEW_COMPONENT_ID)) {
      final ViewComponent component = getComponent(ViewComponent.class);
      component.setId(ApplicationConstants.MAIN_LAYOUT_VIEW_COMPONENT_ID);
      component.setParameters(parameters);
      return component;
    }
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.client.kernel.ComponentProvider#getGlobalResources()
   */
  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();

    String legacySkin = "N";
    try {
      legacySkin = org.openbravo.erpCommon.businessUtility.Preferences.getPreferenceValue(
              "SKINLEG_LegacySkin", true, OBContext.getOBContext().getCurrentClient(),
              OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
              OBContext.getOBContext().getRole(), null);
    } catch (Exception ignored) { }
    String skin = legacySkin.equals("N") ? KernelConstants.SKIN_PARAMETER : "Legacy";

    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-utilities.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-utilities-action.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-utilities-action-def.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-utilities-date.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-utilities-number.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-utilities-style.js", true));

    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-popup.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/ob-form-button.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-canvas.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-checkbox.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-datechooser.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-date.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-datetime.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-encrypted.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-linktitle.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-text.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-link.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-linkbutton.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-combo.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-list.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-list-filter.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-fk.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-fk-filter.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-minidaterange.js",
        true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-number.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-search.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-search-attribute.js",
        true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-section.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-section-audit.js",
        true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-textarea.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-richtext.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-time.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-yesno.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-spinner.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-characteristics.js",
        true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-pickeditgrid.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-processfileupload.js",
        true));

    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/toolbar/ob-toolbar.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-image.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/navbar/ob-application-menu.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.client.application/js/main/ob-tab.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/calendar/ob-calendar.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/calendar/ob-multicalendar.js", true));
    globalResources.add(
        createStaticResource("web/org.openbravo.client.application/js/grid/ob-grid.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/grid/ob-tree-grid.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/grid/ob-tree-view-grid.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-tree.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/formitem/ob-formitem-tree-filter.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/navbar/ob-quickrun-widget.js", false));

    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-property-store.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-function-registry.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-test-registry.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-remote-call-manager.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/classic/ob-classic-window.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/classic/ob-classic-help.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-external-page.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/main/ob-standard-window.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/main/ob-standard-view-datasource.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/main/ob-standard-view.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/main/ob-event-handler-registry.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/main/ob-base-view.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/ob-view-form-linked-items.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/ob-view-form-notes.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/ob-view-form-attachments.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/ob-view-form.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/grid/ob-view-grid.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/ob-onchange-registry.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-keyboard-manager.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/classic/ob-classic-popup.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/main/ob-messagebar.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/form/ob-statusbar.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-recent-utilities.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/navbar/ob-user-profile-widget.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/navbar/ob-logout-widget.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/navbar/ob-help-about-widget.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/toolbar/ob-action-button.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-eventhandler.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-section-stack.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-onchange-functions.js", false));

    // Alert Management
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/alert-management/ob-alert-manager.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/alert-management/ob-alert-grid.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/alert-management/ob-alert-management-view.js",
        false));

    // personalization
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/personalization/ob-personalization.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/personalization/ob-personalization-treegrid.js",
        false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/personalization/ob-personalize-form.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/personalization/ob-personalize-form-toolbar-button.js",
        false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/personalization/ob-manage-views.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/personalization/ob-manage-views-popups.js",
        false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/personalization/ob-manage-views-toolbar.js",
        false));

    // Process
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/process/ob-pick-and-execute-datasource.js",
        false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/process/ob-pick-and-execute-grid.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/process/ob-pick-and-execute-view.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/process/ob-base-parameter-window-view.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/process/ob-parameter-window-form.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/process/ob-parameter-window-view.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/process/ob-attachment-window-view.js", false));

    // Return Material
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/return-material/ob-return-material.js", false));
    // Costing - Landed Cost Match from Invoice
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/costing/ob-lc-matchfrominvoice.js", false));
    // Reservations
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/materialmgmt/ob-reservation.js", false));
    // Good Movement of reserved stock
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/materialmgmt/ob-reservedGoodMovement.js", false));
    // Procurement
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/procurement/ob-procurement.js", false));

    // Extra Window Settings Callbacks
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/callback/ob-extra-window-settings-actions.js",
        false));

    // Upload
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/upload/ob-upload-popup.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/upload/ob-upload-bp-select-popup.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/upload/ob-upload-bp-select-button.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/upload/ob-upload-product-button.js", false));

    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-upload-styles.css", false));

    // Styling
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-application-menu-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-tab-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-form-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-calendar-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-grid-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-tree-grid-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-navigation-bar-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-popup-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-process-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-dialog-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-toolbar-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-messagebar-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-statusbar-styles.css", false));
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-application-styles.css", false));


    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-application-menu-styles.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin + "/org.openbravo.client.application/ob-tab-styles.js",
        false));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-form-styles.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-calendar-styles.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-grid-styles.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-tree-grid-styles.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-toolbar-styles.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-messagebar-styles.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-statusbar-styles.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-popup-styles.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-process-styles.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-dialog-styles.js", false));

    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/navbar/ob-quick-launch.js", false));

    // before the main layout
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-application-styles.js", false));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-navigation-bar-styles.js", false));

    if (existsDynamicNavigationBarComponents()) {
      // Adding this dynamic resource will result in an extra request for the 'Application'
      // component in order to complete the creation of the navigation bar components that are
      // generated dynamically
      globalResources.add(createDynamicResource("org.openbravo.client.kernel/"
          + ApplicationConstants.COMPONENT_TYPE + "/" + ApplicationConstants.MAIN_LAYOUT_ID));
    }
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/main/ob-notes-datasource.js", false));
    globalResources.add(
        createStaticResource("web/org.openbravo.client.application/js/main/ob-layout.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-view-manager.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/classic/ob-classic-compatibility.js", false));
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/utilities/ob-history-manager.js", false));

    // personalization
    globalResources
        .add(createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-personalization-styles.css", false));

    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + skin
            + "/org.openbravo.client.application/ob-personalization-styles.js", false));

    // RTL files should be added at the end. Don't add more files after them
    if (OBContext.getOBContext().isRTL()) {
      globalResources.add(
          createStyleSheetResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
              + skin
              + "/org.openbravo.client.application/ob-rtl-styles.css", false));

      globalResources
          .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
              + skin
              + "/org.openbravo.client.application/ob-rtl-styles.js", false));
    }
    globalResources.add(createStaticResource("web/js/periodControlStatus.js", true));
    globalResources.add(createStaticResource("web/js/productCharacteristicsProcess.js", true));
    globalResources.add(createStaticResource("web/js/recalculatePermissionsProcess.js", true));
    globalResources.add(createStaticResource("web/js/validateCostingRuleProcess.js", true));
    globalResources.add(createStaticResource("web/js/checkAvailableCredit.js", true));

    // Product Services
    globalResources.add(createStaticResource("web/js/productServices.js", true));

    // Cancel and Replace
    globalResources.add(createStaticResource("web/js/cancelAndReplace.js", false));

    return globalResources;
  }

  private static boolean existsDynamicNavigationBarComponents() {
    OBCriteria<NavBarComponent> obc = OBDal.getInstance().createCriteria(NavBarComponent.class);
    obc.add(Restrictions.eq(NavBarComponent.PROPERTY_ISSTATICCOMPONENT, false));
    obc.setMaxResults(1);
    return obc.uniqueResult() != null;
  }
}
