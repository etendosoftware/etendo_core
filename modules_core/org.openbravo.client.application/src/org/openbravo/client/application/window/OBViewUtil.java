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
package org.openbravo.client.application.window;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Hibernate;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.internal.SessionImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.GCField;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.client.application.Parameter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Element;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;

/**
 * Utility methods used in generating Openbravo view representations.
 * 
 * @author mtaal
 */
public class OBViewUtil {
  public static final Element createdElement;
  public static final Element createdByElement;
  public static final Element updatedElement;
  public static final Element updatedByElement;
  private static final String SORTABLE_PROPERTY = "PROPERTY_SORTABLE";
  private static final String FILTERABLE_PROPERTY = "PROPERTY_FILTERABLE";
  private static final String TEXTFILTERBEHAVIOR_PROPERTY = "PROPERTY_TEXTFILTERBEHAVIOR";
  private static final String FILTERONCHANGE_PROPERTY = "PROPERTY_FILTERONCHANGE";
  private static final String ALLOWFILTERBYIDENTIFIER_PROPERTY = "PROPERTY_ALLOWFILTERBYIDENTIFIER";
  private static final String ISFKDROPDOWNUNFILTERED_PROPERTY = "PROPERTY_ISFKDROPDOWNUNFILTERED";
  private static final String DISABLEFKCOMBO_PROPERTY = "PROPERTY_DISABLEFKCOMBO";
  private static final String THRESHOLDTOFILTER_PROPERTY = "PROPERTY_THRESHOLDTOFILTER";
  private static final String ISLAZYFILTERING_PROPERTY = "PROPERTY_ISLAZYFILTERING";

  static {
    createdElement = OBDal.getInstance().get(Element.class, "245");
    createdByElement = OBDal.getInstance().get(Element.class, "246");
    updatedElement = OBDal.getInstance().get(Element.class, "607");
    updatedByElement = OBDal.getInstance().get(Element.class, "608");

    // force loading translations for these fields as they might be used for labels
    Hibernate.initialize(createdElement.getADElementTrlList());
    Hibernate.initialize(createdByElement.getADElementTrlList());
    Hibernate.initialize(updatedElement.getADElementTrlList());
    Hibernate.initialize(updatedByElement.getADElementTrlList());
  }

  private static Logger log = LogManager.getLogger();

  /**
   * Method for retrieving the label of a field on the basis of the current language of the user.
   * 
   * @see #getLabel(BaseOBObject, List)
   */
  public static String getLabel(Field fld) {
    return getLabel(fld, fld.getADFieldTrlList());
  }

  /**
   * Returns parameter's title. Because the same Parameter Definition can be used in different
   * windows (some being purchases, some other ones sales), sync terminology is not enough to
   * determine its title. If this process is invoked from a window, it is required to check the
   * window itself to decide if it is sales or purchases. Note this only takes effect in case the
   * parameter is associated with an element and the parameter is centrally maintained.
   * 
   * @param parameter
   *          Parameter to get the title for
   * @param purchaseTrx
   *          Is the window for purchases or sales
   * @return Parameter's title
   */
  public static String getParameterTitle(Parameter parameter, boolean purchaseTrx) {
    if (purchaseTrx && parameter.getApplicationElement() != null
        && parameter.isCentralMaintenance()) {
      return getLabel(parameter.getApplicationElement(),
          parameter.getApplicationElement().getADElementTrlList(),
          Element.PROPERTY_PURCHASEORDERNAME, Element.PROPERTY_NAME);
    }
    return getLabel(parameter, parameter.getOBUIAPPParameterTrlList());
  }

  /**
   * Generic method for computing the translated label/title. It assumes that the trlObjects have a
   * property called language and name and the owner object a property called name.
   * 
   * @param owner
   *          the owner of the trlObjects (for example Field)
   * @param trlObjects
   *          the trl objects (for example FieldTrl)
   * @return a translated name if found or otherwise the name of the owner
   */
  public static String getLabel(BaseOBObject owner, List<?> trlObjects) {
    return getLabel(owner, trlObjects, Field.PROPERTY_NAME);
  }

  public static String getLabel(BaseOBObject owner, List<?> trlObjects, String propertyName) {
    return getLabel(owner, trlObjects, propertyName, null);
  }

  /**
   * Generic method for computing the translated label/title. It assumes that the trlObjects have a
   * property called language and name and the owner object a property called name.
   * 
   * @param owner
   *          the owner of the trlObjects (for example Field)
   * @param trlObjects
   *          the trl objects (for example FieldTrl)
   * @param primaryPropertyName
   *          first property to look for, if secondaryProperty is null or value of this property is
   *          not null this one will be used
   * @param secondaryPropertyName
   *          second property to look for, if this is sent to null, primaryProperty will be always
   *          used. If this property is not null and value of primaryProperty is null, this one will
   *          be used
   * @return a translated name if found or otherwise the name of the owner
   */
  private static String getLabel(BaseOBObject owner, List<?> trlObjects, String primaryPropertyName,
      String secondaryPropertyName) {
    if (OBContext.hasTranslationInstalled()) {
      final String userLanguageId = OBContext.getOBContext().getLanguage().getId();

      List<BaseOBObject> initializedTrlObjects;
      initializedTrlObjects = getInitializedTrlObjects(owner, trlObjects);

      for (BaseOBObject trlObject : initializedTrlObjects) {
        final String trlLanguageId = (String) ((BaseOBObject) trlObject
            .get(FieldTrl.PROPERTY_LANGUAGE)).getId();
        if (trlLanguageId.equals(userLanguageId)) {
          if (secondaryPropertyName == null || trlObject.get(primaryPropertyName) != null) {
            return (String) trlObject.get(primaryPropertyName);
          }
          return (String) trlObject.get(secondaryPropertyName);
        }
      }
    }

    // trl not found, return owner
    if (secondaryPropertyName == null || owner.get(primaryPropertyName) != null) {
      return (String) owner.get(primaryPropertyName);
    }
    return (String) owner.get(secondaryPropertyName);
  }

  @SuppressWarnings("unchecked")
  private static List<BaseOBObject> getInitializedTrlObjects(BaseOBObject owner,
      List<?> trlObjects) {
    List<BaseOBObject> initializedTrlObjects;
    // owner could have been loaded in a different DAL session via ADCS, as we are not caching trl
    // entries in ADCS, so we need to handle this case
    if (!Hibernate.isInitialized(trlObjects) && !OBDal.getInstance().getSession().contains(owner)) {
      // check if there is already a different instance for the same entry in current DAL session
      SessionImpl si = ((SessionImpl) OBDal.getInstance().getSession());
      EntityPersister p = si.getEntityPersister(owner.getEntityName(), owner);
      BaseOBObject ownerInSession = (BaseOBObject) si.getPersistenceContext()
          .getEntity(new EntityKey((String) owner.getId(), p));

      if (ownerInSession == null) {
        // there is no a different instance in this session, just load it
        ownerInSession = OBDal.getInstance().get(owner.getEntityName(), owner.getId());
      }

      String propName = ((PersistentBag) trlObjects).getRole();
      propName = propName.substring(propName.indexOf('.') + 1);
      initializedTrlObjects = (List<BaseOBObject>) ownerInSession.get(propName);
    } else {
      initializedTrlObjects = (List<BaseOBObject>) trlObjects;
    }
    return initializedTrlObjects;
  }

  /**
   * Returns the grid configuration based on the field and tab information
   * 
   * @return the grid configuration
   */
  public static JSONObject getGridConfigurationSettings(Optional<GCSystem> sysConf,
      Optional<GCTab> tabConf) {
    return getGridConfigurationSettings(null, sysConf, tabConf);
  }

  /**
   * Returns the grid configuration based on the field and tab information
   * 
   * @param field
   *          field whose grid configuration is to be obtained it can be null
   * @return the grid configuration
   */
  public static JSONObject getGridConfigurationSettings(Field field, Optional<GCSystem> sysConf,
      Optional<GCTab> tabConf) {
    GridConfigSettings settings = new GridConfigSettings(field);

    if (tabConf.isPresent()) {
      if (field != null && field.getId() != null) {
        // Grid Configurations at field level for this tab configuration
        // (tabConf.getOBUIAPPGCFieldList) gets cached on Hibernate's first level cache so they can
        // be reused for all fields without the need of reach DB again
        Optional<GCField> fieldConf = tabConf.get()
            .getOBUIAPPGCFieldList() //
            .stream() //
            .filter(fieldGC -> fieldGC.getField().getId().equals(field.getId())) //
            .findFirst();
        if (fieldConf.isPresent()) {
          settings.processConfig(fieldConf.get());
        }
      }

      if (settings.shouldContinueProcessing()) {
        // Trying to get parameters from "Grid Configuration (Tab/Field)" -> "Tab" window
        settings.processConfig(tabConf.get());
      }
    }

    if (settings.shouldContinueProcessing() && sysConf.isPresent()) {
      settings.processConfig(sysConf.get());
    }

    return settings.processJSONResult();
  }

  private static class GridConfigSettings {
    private Boolean canSort = null;
    private Boolean canFilter = null;
    private Boolean filterOnChange = null;
    private Boolean lazyFiltering = null;
    private Boolean allowFkFilterByIdentifier = null;
    private Boolean showFkDropdownUnfiltered = null;
    private Boolean disableFkDropdown = null;
    private String operator = null;
    private Long thresholdToFilter = null;
    private boolean isSortingColumnConfig;
    private boolean isFilteringColumnConfig;
    private Field theField;

    private GridConfigSettings(Field field) {
      isFilteringColumnConfig = true;
      isSortingColumnConfig = true;
      this.theField = field;
      if (theField != null) {
        canSort = theField.getColumn().isAllowSorting();
        canFilter = theField.getColumn().isAllowFiltering();
      } else {
        canSort = true;
        canFilter = true;
      }
    }

    private boolean shouldContinueProcessing() {
      return canSort == null || canFilter == null || operator == null || filterOnChange == null
          || thresholdToFilter == null || allowFkFilterByIdentifier == null
          || showFkDropdownUnfiltered == null || disableFkDropdown == null || lazyFiltering == null;
    }

    private void processConfig(BaseOBObject gcItem) {
      Class<? extends BaseOBObject> itemClass = gcItem.getClass();
      try {
        sortingPropertyValue(gcItem);
        filteringPropertyValue(gcItem);
        if (operator == null) {
          if (gcItem
              .get(itemClass.getField(TEXTFILTERBEHAVIOR_PROPERTY).get(gcItem).toString()) != null
              && !"D".equals(gcItem
                  .get(itemClass.getField(TEXTFILTERBEHAVIOR_PROPERTY).get(gcItem).toString()))) {
            operator = (String) gcItem
                .get(itemClass.getField(TEXTFILTERBEHAVIOR_PROPERTY).get(gcItem).toString());
          }
        }
        if (filterOnChange == null) {
          filterOnChange = convertBoolean(gcItem, FILTERONCHANGE_PROPERTY);
        }
        if (allowFkFilterByIdentifier == null) {
          allowFkFilterByIdentifier = convertBoolean(gcItem, ALLOWFILTERBYIDENTIFIER_PROPERTY);
        }
        if (showFkDropdownUnfiltered == null) {
          showFkDropdownUnfiltered = convertBoolean(gcItem, ISFKDROPDOWNUNFILTERED_PROPERTY);
        }
        if (disableFkDropdown == null) {
          disableFkDropdown = convertBoolean(gcItem, DISABLEFKCOMBO_PROPERTY);
        }
        if (thresholdToFilter == null) {
          thresholdToFilter = (Long) gcItem
              .get(itemClass.getField(THRESHOLDTOFILTER_PROPERTY).get(gcItem).toString());
        }
        if (lazyFiltering == null && !(gcItem instanceof GCField)) {
          lazyFiltering = convertBoolean(gcItem, ISLAZYFILTERING_PROPERTY);
        }
      } catch (Exception e) {
        log.error("Error while getting the properties of " + gcItem, e);
      }
    }

    private Boolean convertBoolean(BaseOBObject gcItem, String property) {
      Boolean isPropertyEnabled = true;
      Class<? extends BaseOBObject> itemClass = gcItem.getClass();
      try {
        if (gcItem instanceof GCSystem) {
          if (gcItem.get(itemClass.getField(property).get(gcItem).toString()).equals(true)) {
            isPropertyEnabled = true;
          } else if (gcItem.get(itemClass.getField(property).get(gcItem).toString())
              .equals(false)) {
            isPropertyEnabled = false;
          }
        } else {
          if ("Y".equals(gcItem.get(itemClass.getField(property).get(gcItem).toString()))) {
            isPropertyEnabled = true;
          } else if ("N".equals(gcItem.get(itemClass.getField(property).get(gcItem).toString()))) {
            isPropertyEnabled = false;
          } else if ("D".equals(gcItem.get(itemClass.getField(property).get(gcItem).toString()))) {
            isPropertyEnabled = null;
          }
        }
      } catch (Exception e) {
        log.error("Error while converting a value to boolean", e);
      }
      return isPropertyEnabled;
    }

    private void sortingPropertyValue(BaseOBObject gcItem) {
      Boolean sortingConfiguration = convertBoolean(gcItem, SORTABLE_PROPERTY);
      if (sortingConfiguration == null) {
        return;
      }
      if (gcItem instanceof GCField) {
        isSortingColumnConfig = false;
        canSort = sortingConfiguration;
      } else if (gcItem instanceof GCTab && isSortingColumnConfig) {
        isSortingColumnConfig = false;
        if (!sortingConfiguration) {
          canSort = sortingConfiguration;
        }
      } else if (gcItem instanceof GCSystem && isSortingColumnConfig && !sortingConfiguration) {
        canSort = sortingConfiguration;
      }
    }

    private void filteringPropertyValue(BaseOBObject gcItem) {
      Boolean filteringConfiguration = convertBoolean(gcItem, FILTERABLE_PROPERTY);
      if (filteringConfiguration == null) {
        return;
      }
      if (gcItem instanceof GCField) {
        isFilteringColumnConfig = false;
        canFilter = filteringConfiguration;
      } else if (gcItem instanceof GCTab && isFilteringColumnConfig) {
        isFilteringColumnConfig = false;
        if (!filteringConfiguration) {
          canFilter = filteringConfiguration;
        }
      } else if (gcItem instanceof GCSystem && isFilteringColumnConfig && !filteringConfiguration) {
        canFilter = filteringConfiguration;
      }
    }

    public JSONObject processJSONResult() {
      if (operator != null) {
        if ("IC".equals(operator)) {
          operator = "iContains";
        } else if ("IS".equals(operator)) {
          operator = "iStartsWith";
        } else if ("IE".equals(operator)) {
          operator = "iEquals";
        } else if ("C".equals(operator)) {
          operator = "contains";
        } else if ("S".equals(operator)) {
          operator = "startsWith";
        } else if ("E".equals(operator)) {
          operator = "equals";
        }
      }

      JSONObject result = new JSONObject();
      try {
        if (canSort != null) {
          result.put("canSort", canSort);
        }
        if (canFilter != null) {
          result.put("canFilter", canFilter);
        }
        if (operator != null) {
          result.put("operator", operator);
        }
        // If the tab uses lazy filtering, the fields should not filter on change
        if (Boolean.TRUE.equals(lazyFiltering)) {
          filterOnChange = false;
        }
        if (filterOnChange != null) {
          result.put("filterOnChange", filterOnChange);
        }
        if (thresholdToFilter != null) {
          result.put("thresholdToFilter", thresholdToFilter);
        }
        if (allowFkFilterByIdentifier != null) {
          result.put("allowFkFilterByIdentifier", allowFkFilterByIdentifier);
        }
        if (showFkDropdownUnfiltered != null) {
          result.put("showFkDropdownUnfiltered", showFkDropdownUnfiltered);
        }
        if (disableFkDropdown != null) {
          result.put("disableFkDropdown", disableFkDropdown);
        }
      } catch (JSONException e) {
        log.error("Couldn't get field property value", e);
      }

      return result;
    }
  }
}
