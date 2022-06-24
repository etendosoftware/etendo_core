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
package org.openbravo.client.application.window;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Order;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.Template;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;

/**
 * The component which takes care of creating a class for a specific Openbravo window.
 * 
 * @author mtaal
 */
public class StandardWindowComponent extends BaseTemplateComponent {
  private static final Logger log = LogManager.getLogger();
  private static final String DEFAULT_TEMPLATE_ID = "ADD5EF45333C458098286D0E639B3290";

  private Window window;
  private OBViewTab rootTabComponent = null;
  private String uniqueString = "" + System.currentTimeMillis();
  private List<String> processViews = new ArrayList<String>();

  @Override
  protected Template getComponentTemplate() {

    return OBDal.getInstance().get(Template.class, DEFAULT_TEMPLATE_ID);
  }

  public String getWindowClientClassName() {
    // see the ViewComponent#correctViewId
    // changes made in this if statement should also be done in that method
    if (isInDevelopment()) {
      return KernelConstants.ID_PREFIX + window.getId() + KernelConstants.ID_PREFIX + uniqueString;
    }
    return KernelConstants.ID_PREFIX + getWindowId();
  }

  public void setUniqueString(String uniqueString) {
    this.uniqueString = uniqueString;
  }

  @Override
  public String generate() {
    final String jsCode = super.generate();
    return jsCode;
  }

  public String getTabView() {
    return getRootTabComponent().generate();
  }

  public String getWindowId() {
    return getWindow().getId();
  }

  public String getThreadSafe() {
    final Boolean value = getWindow().isThreadsafe();
    if (value != null) {
      return value.toString();
    }
    return "false";
  }

  public Window getWindow() {
    return window;
  }

  public void setWindow(Window window) {
    this.window = window;

    // reset fields here to be able to use this code in testing: being request scoped will share
    // instance if it is invoked several times in same test case.
    rootTabComponent = null;
    processViews = new ArrayList<>();
  }

  public OBViewTab getRootTabComponent() {
    if (rootTabComponent != null) {
      return rootTabComponent;
    }

    Optional<GCSystem> systemGridConfig = getSystemGridConfig();
    Map<String, Optional<GCTab>> tabsGridConfig = getTabsGridConfig(window);

    final List<OBViewTab> tempTabs = new ArrayList<OBViewTab>();
    for (Tab tab : getWindow().getADTabList()) {
      // NOTE: grid sequence and field sequence tabs do not have any fields defined!
      if (!tab.isActive()) {
        continue;
      }
      final OBViewTab tabComponent = createComponent(OBViewTab.class);
      tabComponent.setTab(tab);
      tabComponent.setUniqueString(uniqueString);
      tabComponent.setGCSettings(systemGridConfig, tabsGridConfig);
      tempTabs.add(tabComponent);
      final String processView = tabComponent.getProcessViews();
      if (!"".equals(processView)) {
        processViews.add(tabComponent.getProcessViews());
      }
    }

    // compute the correct hierarchical structure of the tabs
    for (OBViewTab tabComponent : tempTabs) {
      OBViewTab parentTabComponent = null;
      for (OBViewTab testTabComponent : tempTabs) {
        if (testTabComponent.getTab().getTabLevel() == (tabComponent.getTab().getTabLevel() - 1)
            && testTabComponent.getTab().getSequenceNumber() < tabComponent.getTab()
                .getSequenceNumber()) {
          if (parentTabComponent != null) {
            // if the new potential parent has a higher sequence number then that one is the correct
            // one
            if (parentTabComponent.getTab().getSequenceNumber() < testTabComponent.getTab()
                .getSequenceNumber()) {
              parentTabComponent = testTabComponent;
            }
          } else {
            parentTabComponent = testTabComponent;
          }
        }
      }
      if (parentTabComponent != null) {
        parentTabComponent.addChildTabComponent(tabComponent);
      }
    }

    // handle a special case, multiple root tab components
    // now get the root tabs
    for (OBViewTab tabComponent : tempTabs) {
      if (tabComponent.getParentTabComponent() == null) {
        if (rootTabComponent != null) {
          if (tabComponent.getTab().getTabLevel() == rootTabComponent.getTab().getTabLevel()) {
            // warn for a special case, multiple root tab components
            log.warn("Window " + window.getName() + " " + window.getId()
                + " has more than on tab on level 0, choosing an arbitrary root tab");
          } else {
            // warn for a special case, two tabs with different tab levels but same sequence number
            log.warn("Window " + window.getName() + " " + window.getId()
                + " two tabs with the same sequence number but different tab level");
          }
          rootTabComponent.addChildTabComponent(tabComponent);
        } else {
          rootTabComponent = tabComponent;
        }
      }
    }
    if (rootTabComponent != null) {
      rootTabComponent.setRootTab(true);
    }
    return rootTabComponent;
  }

  public List<String> getProcessViews() {
    return processViews;
  }

  /** Returns the applicable System Grid Configuration if any. */
  public static Optional<GCSystem> getSystemGridConfig() {
    OBCriteria<GCSystem> gcSystemCriteria = OBDal.getInstance().createCriteria(GCSystem.class);
    gcSystemCriteria.addOrder(Order.desc(GCTab.PROPERTY_SEQNO));
    gcSystemCriteria.addOrder(Order.desc(GCTab.PROPERTY_ID));
    gcSystemCriteria.setMaxResults(1);
    return Optional.ofNullable((GCSystem) gcSystemCriteria.uniqueResult());
  }

  /**
   * For a given window, it returns a Map being its key all the tab ids in that window and the
   * values the applicable Tab Grid Configuration for each tab if any.
   */
  public static Map<String, Optional<GCTab>> getTabsGridConfig(Window window) {
    // window comes from ADCS, we need to retrieve GC from DB as it might have changed
    OBQuery<GCTab> qGCTab = OBDal.getInstance()
        .createQuery(GCTab.class, "as g where g.tab.window = :window");
    qGCTab.setNamedParameter("window", window);
    Map<String, List<GCTab>> gcsByTab = qGCTab.stream() //
        .collect(groupingBy(gcTab -> gcTab.getTab().getId()));

    return window.getADTabList()
        .stream() //
        .map(tab -> getTabConfig(tab, gcsByTab)) //
        .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));
  }

  private static SimpleEntry<String, Optional<GCTab>> getTabConfig(Tab tab,
      Map<String, List<GCTab>> gcsByTab) {
    Stream<GCTab> candidates = gcsByTab.containsKey(tab.getId())
        ? gcsByTab.get(tab.getId()).stream()
        : Stream.empty();

    Optional<GCTab> selectedGC = candidates //
        .sorted( //
            comparing(GCTab::getSeqno) //
                .thenComparing(GCTab::getId)) //
        .findFirst();

    return new SimpleEntry<>(tab.getId(), selectedGC);
  }
}
