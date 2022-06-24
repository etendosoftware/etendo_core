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
 * All portions are Copyright (C) 2012-2021 Openbravo SLU 
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.application.DynamicExpressionParser;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.attachment.AttachmentWindowComponent;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.ListTrl;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.FieldGroup;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.userinterface.selector.reference.FKSelectorUIDefinition;

public class OBViewParameterHandler {
  private static final Logger log = LogManager.getLogger();
  private static final String WINDOW_REFERENCE_ID = "FF80818132D8F0F30132D9BC395D0038";
  private static final int NUMBER_COLUMNS = 4;
  private BaseTemplateComponent paramWindow;
  private List<Parameter> parameters = new ArrayList<Parameter>();

  public void setParameters(List<Parameter> parameters) {
    this.parameters = parameters;
  }

  public List<OBViewParameter> getParameters() {

    List<Parameter> parametersInExpression = new ArrayList<Parameter>();

    // Computes the display logic of the parameters
    // It has to be done in advance in order to determine the dynamic parameters
    Map<Parameter, String> displayLogicMap = new HashMap<Parameter, String>();
    for (Parameter param : parameters) {
      if (param.isActive() && param.getDisplayLogic() != null
          && !param.getDisplayLogic().isEmpty()) {
        final DynamicExpressionParser parser = new DynamicExpressionParser(param.getDisplayLogic(),
            param, parameters, true);
        displayLogicMap.put(param, parser.getJSExpression());
        for (Parameter parameterExpression : parser.getParameters()) {
          if (!parametersInExpression.contains(parameterExpression)) {
            parametersInExpression.add(parameterExpression);
          }
        }
      }
    }

    // Computes read-only logic
    Map<Parameter, String> readOnlyLogicMap = new HashMap<Parameter, String>();
    for (Parameter param : parameters) {
      if (param.isActive() && !param.isFixed() && param.getReadOnlyLogic() != null
          && !param.getReadOnlyLogic().isEmpty()) {
        final DynamicExpressionParser parser = new DynamicExpressionParser(param.getReadOnlyLogic(),
            param, parameters, true);
        readOnlyLogicMap.put(param, parser.getJSExpression());
        for (Parameter parameterExpression : parser.getParameters()) {
          if (!parametersInExpression.contains(parameterExpression)) {
            parametersInExpression.add(parameterExpression);
          }
        }
      }
    }

    List<OBViewParameter> params = new ArrayList<OBViewParameterHandler.OBViewParameter>();
    OBViewParamGroup currentGroup = null;
    FieldGroup currentADFieldGroup = null;
    int pos = 1;
    for (Parameter param : parameters) {
      if (!(param.isActive()
          && (!param.isFixed() || param.getReference().getId().equals(WINDOW_REFERENCE_ID))
          && (!param.getReference()
              .getId()
              .equals(ParameterWindowComponent.BUTTON_LIST_REFERENCE_ID)))) {
        continue;
      }

      // change in fieldgroup
      if (param.getFieldGroup() != null && param.getFieldGroup() != currentADFieldGroup) {
        OBViewParamGroup group = new OBViewParamGroup();
        params.add(group);
        group.setFieldGroup(param.getFieldGroup());

        currentGroup = group;
        currentADFieldGroup = param.getFieldGroup();
      }

      if (currentGroup != null) {
        currentGroup.addChild(param);
      }

      OBViewParameter parameter = new OBViewParameter(param);
      if (displayLogicMap.containsKey(param)) {
        parameter.setShowIf(displayLogicMap.get(param));
      }

      if (readOnlyLogicMap.containsKey(param)) {
        parameter.setReadOnlyIf(readOnlyLogicMap.get(param));
      }
      // 17 is the list reference
      if (param.getReferenceSearchKey() != null
          && param.getReferenceSearchKey().getParentReference() != null
          && param.getReferenceSearchKey().getParentReference().getId().equals("17")) {
        parameter.addListReferenceValues(param.getReferenceSearchKey());
      }

      // Add spacers to order the field in the column number defined
      if (param.isStartinnewline()) {
        pos = 1;
      }
      if (pos > NUMBER_COLUMNS) {
        pos = pos - NUMBER_COLUMNS;
      }

      if (param.getNumColumn() != null) {
        int spaces = 0;
        if (pos > param.getNumColumn().intValue()) {
          spaces = NUMBER_COLUMNS - (pos - param.getNumColumn().intValue());
        } else {
          spaces = param.getNumColumn().intValue() - pos;
        }
        for (int i = 0; i < spaces; i++) {
          final OBViewParamSpacer spacer = new OBViewParamSpacer();
          params.add(spacer);
          pos++;
        }

      }
      params.add(parameter);
      pos++;

    }
    return params;
  }

  public class OBViewParameter {
    UIDefinition uiDefinition;
    Parameter parameter;
    String showIf = "";
    String readOnlyIf = "";
    List<ValueMapValue> valueMap = new ArrayList<ValueMapValue>();

    public OBViewParameter() {

    }

    public OBViewParameter(Parameter param) {
      // UIDefinition may be in sub-reference, under Reference Search Key, check and use this first
      // in case the sub-reference doesn't define a custom UIDefinition use parameter base reference
      // UIDefinition
      Reference paramReference;
      if (param.getReferenceSearchKey() != null
          && !param.getReferenceSearchKey().getOBCLKERUIDefinitionList().isEmpty()) {
        paramReference = param.getReferenceSearchKey();
      } else {
        paramReference = param.getReference();
      }
      uiDefinition = UIDefinitionController.getInstance().getUIDefinition(paramReference);
      parameter = param;
    }

    public boolean isValueMapPresent() {
      return !valueMap.isEmpty();
    }

    public List<ValueMapValue> getValueMap() {
      Collections.sort(valueMap, new Comparator<ValueMapValue>() {
        @Override
        public int compare(ValueMapValue v1, ValueMapValue v2) {
          final long seqno1 = v1.seqno;
          final long seqno2 = v2.seqno;

          // compare the names if no seqno set.
          if (seqno1 == -1 || seqno2 == -1) {
            return v1.getValue().compareTo(v2.getValue());
          }

          return (int) (seqno1 - seqno2);
        }
      });
      return valueMap;
    }

    public void addListReferenceValues(Reference reference) {
      for (org.openbravo.model.ad.domain.List list : reference.getADListList()) {
        if (list.isActive()) {
          addListValueReference(list);
        }
      }
    }

    public void addListValueReference(org.openbravo.model.ad.domain.List listValue) {
      String name = listValue.getName();
      if (OBContext.hasTranslationInstalled()) {
        final String languageId = OBContext.getOBContext().getLanguage().getId();
        for (ListTrl listTrl : listValue.getADListTrlList()) {
          if (!listTrl.isActive()) {
            continue;
          }
          if (listTrl.getLanguage().getId().equals(languageId)) {
            name = listTrl.getName();
            break;
          }
        }
      }
      final ValueMapValue vmv = new ValueMapValue(listValue.getSearchKey(), name,
          listValue.getSequenceNumber());
      valueMap.add(vmv);
    }

    public String getId() {
      return parameter.getId();
    }

    public String getType() {
      return uiDefinition != null ? uiDefinition.getName() : "--";
    }

    public String getTitle() {
      Window parentWindow = null;
      if (paramWindow instanceof ParameterWindowComponent) {
        parentWindow = ((ParameterWindowComponent) paramWindow).parentWindow;
      } else if (paramWindow instanceof AttachmentWindowComponent) {
        parentWindow = ((AttachmentWindowComponent) paramWindow).getParentWindow();
      }
      boolean purchaseTrx = parentWindow != null && !parentWindow.isSalesTransaction();
      return OBViewUtil.getParameterTitle(parameter, purchaseTrx);
    }

    public String getName() {
      return parameter.getDBColumnName();
    }

    public boolean isRequired() {
      // grid params should always be marked as not mandatory
      return !isGrid() && parameter.isMandatory();
    }

    public boolean isGrid() {
      return parameter.getReferenceSearchKey() != null
          && parameter.getReferenceSearchKey().getOBUIAPPRefWindowList().size() > 0;
    }

    public String getTabView() {
      String tabId = OBDal.getInstance()
          .getSession()
          .createQuery(
              "select t.id from OBUIAPP_Parameter p join p.referenceSearchKey r join r.oBUIAPPRefWindowList rw join rw.window w join w.aDTabList t where p.id=:param",
              String.class) //
          .setParameter("param", parameter.getId()) //
          .setMaxResults(1) //
          .uniqueResult();

      if (tabId == null) {
        log.error("Window definition for parameter " + parameter + " has no tabs");
        return null;
      }

      // parameters are not cached in ADCS
      Tab tab = paramWindow.getADCS().getTab(tabId);

      final OBViewTab tabComponent = paramWindow.createComponent(OBViewTab.class);
      tabComponent.setTab(tab);
      tabComponent.setGCSettings(StandardWindowComponent.getSystemGridConfig(),
          StandardWindowComponent.getTabsGridConfig(tab.getWindow()));
      return tabComponent.generate();
    }

    public String getParameterProperties() {
      String jsonString = uiDefinition.getParameterProperties(parameter).trim();
      if (jsonString == null || jsonString.trim().length() == 0) {
        return "";
      }
      // strip the first and last { }
      if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
        // note -2 is done because the first substring takes of 1 already
        return "," + jsonString.substring(1).substring(0, jsonString.length() - 2);
      } else if (jsonString.equals("{}")) {
        return "";
      }
      // be lenient just return the string as it is...
      return ","
          + (jsonString.trim().endsWith(",") ? jsonString.substring(0, jsonString.length() - 2)
              : jsonString);
    }

    public void setShowIf(String showIf) {
      this.showIf = showIf;
    }

    public String getShowIf() {
      return showIf;
    }

    public void setReadOnlyIf(String readOnlyIf) {
      this.readOnlyIf = readOnlyIf;
    }

    public String getReadOnlyIf() {
      return readOnlyIf;
    }

    public String getWidth() {
      return this.uiDefinition.getParameterWidth(this.parameter);
    }

    public Long getLength() {
      if (parameter == null || parameter.getLength() == 0L) {
        return -1L;
      }
      return parameter.getLength();
    }

    public String getTargetEntity() {
      String entityName = "";
      if (uiDefinition instanceof FKSelectorUIDefinition
          && parameter.getReferenceSearchKey() != null) {
        String idOfTheTable = parameter.getReferenceSearchKey()
            .getOBUISELSelectorList()
            .get(0)
            .getTable()
            .getId();
        entityName = ModelProvider.getInstance().getEntityByTableId(idOfTheTable).getName();
      }
      return entityName;
    }

    public String getOnChangeFunction() {
      return parameter.getOnChangeFunction();
    }

    public Long getNumberOfDisplayedRows() {
      return parameter.getDisplayedRows() != null ? parameter.getDisplayedRows() : 8;
    }

    public String getOnGridLoadFunction() {
      return parameter.getOnGridLoadFunction();
    }

    public boolean getShowTitle() {
      return parameter.isDisplayTitle();
    }

    public class ValueMapValue {
      final String key;
      final String value;
      final long seqno;

      ValueMapValue(String key, String value, Long seqno) {
        this.key = key;
        this.value = value;
        this.seqno = (seqno != null ? seqno : -1);
      }

      public String getKey() {
        return key;
      }

      public String getValue() {
        return value;
      }

      public long getSeqno() {
        return seqno;
      }
    }
  }

  public class OBViewParamGroup extends OBViewParameter {
    private FieldGroup fieldGroup;
    private List<Parameter> children = new ArrayList<Parameter>();

    @Override
    public String getType() {
      return "OBSectionItem";
    }

    public void setFieldGroup(FieldGroup fieldGroup) {
      this.fieldGroup = fieldGroup;
    }

    @Override
    public String getName() {
      return fieldGroup.getId();
    }

    @Override
    public String getTitle() {
      return OBViewUtil.getLabel(fieldGroup, fieldGroup.getADFieldGroupTrlList());
    }

    @Override
    public boolean isGrid() {
      return false;
    }

    public void addChild(Parameter param) {
      children.add(param);
    }

    public List<Parameter> getChildren() {
      return children;
    }

    public boolean isExpanded() {
      return !(fieldGroup.isCollapsed() == null ? false : fieldGroup.isCollapsed());
    }
  }

  public class OBViewParamSpacer extends OBViewParameter {
    @Override
    public String getType() {
      return "spacer";
    }

    @Override
    public String getName() {
      return "";
    }

    public boolean getPersonalizable() {
      return false;

    }

    @Override
    public boolean isGrid() {
      return false;
    }

    @Override
    public String getTitle() {
      return "";
    }

    @Override
    public String getId() {
      return "";
    }

    @Override
    public String getWidth() {
      return "";
    }

    @Override
    public boolean isRequired() {
      return false;
    }

    @Override
    public String getParameterProperties() {
      return "";
    }

    @Override
    public String getOnChangeFunction() {
      return "";
    }

  }

  public void setParamWindow(BaseTemplateComponent baseTemplateComponent) {
    this.paramWindow = baseTemplateComponent;
  }
}
