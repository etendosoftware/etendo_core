/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use. this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2016-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// == OB.EventHandlerRegistry ==
// A registry which can be used to register actions for a certain
// tab and event type combination. Multiple actions can be registered
// for one tab.
isc.ClassFactory.defineClass('OBEventHandlerRegistry', isc.OBFunctionRegistry);

isc.OBEventHandlerRegistry.addProperties({
  PRESAVE: 'PRESAVE',
  POSTSAVE: 'POSTSAVE',
  PREDELETE: 'PREDELETE',

  actionTypes: ['PRESAVE', 'POSTSAVE', 'PREDELETE'],

  isValidElement: function(actionType) {
    var findType;
    findType = function(type) {
      return type === actionType;
    };
    return this.actionTypes.find(findType);
  },

  hasAction: function(tabId, actionType) {
    return this.getEntries(tabId, actionType);
  },

  // Overrides call function in order to implement the asynchronous execution of the actions.
  call: function(params) {
    var entries = this.getEntries(params.tabId, params.actionType),
      actions;

    if (params.callback && !isc.isA.Function(params.callback)) {
      return;
    }
    actions = isc.clone(entries) || [];
    if (params.callback) {
      actions.push(params.callback);
    }
    this.callbackExecutor(
      params.view,
      params.form,
      params.grid,
      params.extraParameters,
      actions
    );
  },

  // Function to be invoked by event handler actions once they are done
  // in order to continue with the action chain.
  callbackExecutor: function(view, form, grid, extraParameters, actions) {
    var func;
    func = actions.shift();
    if (func) {
      func(view, form, grid, extraParameters, actions);
    }
  }
});

OB.EventHandlerRegistry = isc.OBEventHandlerRegistry.create();
