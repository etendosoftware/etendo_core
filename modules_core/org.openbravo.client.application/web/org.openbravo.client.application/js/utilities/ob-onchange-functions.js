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
 * All portions are Copyright (C) 2014-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
// = On Change Functions =
//
// Contains on change functions that are defined in the fields.
OB = window.OB || {};
OB.OnChange = window.OB.OnChange || {};

// ** {{{OB.OnChange.organizationCurrency}}} **
// Used in the 'Organization' window, in the 'Currency' onchange field
// It shows a warning dialog when currency is changed
OB.OnChange.organizationCurrency = function(item, view, form, grid) {
  if (view && view.messageBar) {
    view.messageBar.setMessage(
      'warning',
      null,
      OB.I18N.getLabel('OBUIAPP_OrgCurrencyChange')
    );
  }
};

// ** {{{OB.OnChange.processDefinitionUIPattern}}} **
// Used in the 'Process Definition' window, in the 'UI Pattern' field
// When OBUIAPP_Report is selected and the Action Handler is empty it sets
// the BaseReportActionHandler as default value.
OB.OnChange.processDefinitionUIPattern = function(item, view, form, grid) {
  var classNameItem = form.getItem('javaClassName');
  if (item.getValue() === 'OBUIAPP_Report' && !classNameItem.getValue()) {
    classNameItem.setValue(
      'org.openbravo.client.application.report.BaseReportActionHandler'
    );
  }
};

//**  {{{OB.OnChange.agingProcessDefinitionOverdue}}}**
//Used by the parameters Overdue Days in Payable and Receivables Aging Balance Process Definition Reports.
//A warning message is shown if the range of overdue days is not correct.
OB.OnChange.agingProcessDefinitionOverdue = function(item, view, form, grid) {
  var column1 = form.getItem('Column1').getValue();
  var column2 = form.getItem('Column2').getValue();
  var column3 = form.getItem('Column3').getValue();
  var column4 = form.getItem('Column4').getValue();
  if (
    column1 &&
    column2 &&
    column3 &&
    column4 &&
    !(column1 < column2 && column2 < column3 && column3 < column4)
  ) {
    item.setValue('');
    view.messageBar.setMessage(
      isc.OBMessageBar.TYPE_WARNING,
      null,
      OB.I18N.getLabel('OBUIAPP_OverdueNotValid')
    );
  } else {
    view.messageBar.hide();
  }
};

//**  {{{OB.OnChange.agingProcessDefinitionOrganization}}}**
//Used to select the General Ledger in use by the selected Organization
OB.OnChange.agingProcessDefinitionOrganization = function(
  item,
  view,
  form,
  grid
) {
  var organization = form.getItem('Organization');
  var gl = form.getItem('AccSchema');
  var callbackGetGLbyOrganization;
  callbackGetGLbyOrganization = function(response, data, request) {
    gl.valueMap = gl.valueMap || {};
    gl.valueMap[data.response.value] = data.response.identifier;
    gl.setValue(data.response.value);
  };

  OB.RemoteCallManager.call(
    'org.openbravo.common.actionhandler.AgingGeneralLedgerByOrganizationActionHandler',
    {
      organization: organization.getValue()
    },
    {},
    callbackGetGLbyOrganization
  );
};

//**  {{{OB.OnChange.colorSelection}}}**
// Used to handle change of hex color selection in Color Palette - Color tab
OB.OnChange.colorSelection = function(item, view, form, grid) {
  const re = /^#([a-f0-9]{8}|[a-f0-9]{6}|[a-f0-9]{4}|[a-f0-9]{3})\b$/gi;
  const isValidHexValue = re.test(item.getValue());
  if (isValidHexValue && form && form.getFields()) {
    // Update color field background color and mark for redraw
    const a = form.getFields().find(f => f.name === 'Color');
    if (a && a.canvas) {
      a.canvas.setBackgroundColor(item.getValue());
      a.canvas.markForRedraw();
    }
  }
};
