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
 * All portions are Copyright (C) 2011-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// = Openbravo Utilities =
// Defines utility methods in the top-level OB.Utilities object. Utility methods
// are related to opening views, opening popups, displaying yes/no, etc.
OB.Utilities = {};

OB.Utilities.isIE9Strict = false;
if (
  (navigator.userAgent.toUpperCase().indexOf('MSIE') !== -1 ||
    navigator.userAgent.toUpperCase().indexOf('TRIDENT') !== -1) &&
  (document.documentMode && document.documentMode >= 9)
) {
  OB.Utilities.isIE9Strict = true;
}

OB.Utilities.isEdge = false;
if (navigator.userAgent.toUpperCase().indexOf('EDGE') !== -1) {
  OB.Utilities.isEdge = true;
}

//** {{{OB.Utilities.checkProfessionalLicense}}} **
// Checks if the current instance is using a professional license
// (!= community). If the instance has a community instance then
// a popup message is shown and false is returned.
// The parameter can be used to add a custom message to the popup.
OB.Utilities.checkProfessionalLicense = function(msg, doNotShowMessage) {
  if (OB.Application.licenseType === 'C') {
    if (!doNotShowMessage) {
      if (!msg) {
        msg = '';
      }
      isc.warn(OB.I18N.getLabel('OBUIAPP_ActivateMessage', [msg]), {
        isModal: true,
        showModalMask: true,
        toolbarButtons: [isc.Dialog.OK]
      });
    }
    return false;
  }
  return true;
};

//** {{{OB.Utilities.notifyIfPortalIsNotAllowed}}} **
// Used in the 'Role' window, in the 'For Portal Users' onchange field
// It shows a warning dialog, if Portal is not available due to license restrictions
OB.Utilities.notifyIfPortalIsNotAllowed = function(item, view, form, grid) {
  if (item.getValue() && OB.Application.licenseType === 'C') {
    OB.Utilities.checkProfessionalLicense(
      OB.I18N.getLabel('OBUIAPP_ActivateMessagePortal')
    );
    item.setValue(false);
  }
};

//** {{{OB.Utilities.encodeSearchOperator}}} **
//Encodes and and or in a string with a forward slash
OB.Utilities.encodeSearchOperator = function(value) {
  var val = value;
  if (!val || !isc.isA.String(val)) {
    return val;
  }
  val = val.replace(/ and /g, ' \\and ');
  val = val.replace(/ or /g, ' \\or ');
  return val;
};

// ** {{{OB.Utilities.truncTitle}}} **
// Truncs a string after a specific length. Initial implementation is
// simple (just cuts of at the specified length). Returns the trunced title
// if no cutLength is set then the default length of 30 is chosen. If no
// suffix is set then ... is appended
// TODO: more advanced implementations can cut of at a space or dash for
// example
OB.Utilities.truncTitle = function(title, cutLength, suffix) {
  cutLength = cutLength || 30;
  if (!title) {
    return title;
  }

  if (title.length < cutLength) {
    return title.asHTML();
  }

  var newTitle = title.substring(0, cutLength);
  // To remove ugly title ends
  while (
    newTitle.length > 4 &&
    (newTitle.lastIndexOf(' - ') === newTitle.length - 3 ||
      newTitle.lastIndexOf(' -') === newTitle.length - 2 ||
      newTitle.lastIndexOf('  ') === newTitle.length - 2)
  ) {
    if (newTitle.lastIndexOf(' - ') === newTitle.length - 3) {
      newTitle = newTitle.substring(0, newTitle.length - 2);
    }
    if (newTitle.lastIndexOf(' -') === newTitle.length - 2) {
      newTitle = newTitle.substring(0, newTitle.length - 1);
    }
    if (newTitle.lastIndexOf('  ') === newTitle.length - 2) {
      newTitle = newTitle.substring(0, newTitle.length - 1);
    }
  }
  newTitle += suffix || '...';
  return newTitle.asHTML();
};

// ** {{{OB.Utilities.createDialog}}} **
// Creates a dialog with a title, an ok button and a layout in the middle.
// The dialog is not shown but returned. The caller needs to call setContent to
// set the content in the dialog and show it.
OB.Utilities.createDialog = function(title, focusOnOKButton, properties) {
  var dialog = isc.Dialog.create(
    {
      title: title,
      toolbarButtons: [isc.Dialog.OK],
      isModal: true,
      canDragReposition: true,
      keepInParentRect: true,
      autoSize: true,
      autoCenter: true,

      contentLayout: 'horizontal',
      autoChildParentMap: isc.addProperties(
        {},
        isc.Window.getInstanceProperty('autoChildParentMap'),
        {
          stack: 'body',
          layout: 'stack',
          toolbar: 'stack'
        }
      ),

      stackDefaults: {
        height: 1
      },

      toolbarDefaults: isc.addProperties(
        {},
        isc.Dialog.getInstanceProperty('toolbarDefaults'),
        {
          layoutAlign: 'center',
          buttonConstructor: isc.OBFormButton
        }
      ),

      createChildren: function() {
        this.showToolbar = false;
        this.Super('createChildren');
        this.addAutoChild('stack', null, isc.VStack);
        this.addAutoChild(
          'layout',
          {
            height: 1,
            width: '100%',
            overflow: 'visible'
          },
          isc.VLayout
        );
        this.showToolbar = true;
        this.makeToolbar();

        // can't be done via defaults because policy and direction are dynamically determined
        this.body.hPolicy = 'fill';
      },

      // will set the content and show it
      setContent: function(content) {
        // Note: we lazily create children on draw, so verify that the items have been
        // initialized before manipulating the label
        if (!this._isInitialized) {
          this.createChildren();
        }

        // Update the content in the body
        this.layout.addMember(content);
        this.toolbar.layoutChildren();
        if (this.isDrawn()) {
          this.stack.layoutChildren();
          this.body.layoutChildren();
          this.layoutChildren();
        }

        this.show();

        // focus in the first button so you can hit Enter to do the default thing
        if (this.toolbar && focusOnOKButton) {
          var firstButton = this.toolbar.getMember(0);
          firstButton.focus();
        }
      }
    },
    properties
  );
  return dialog;
};

OB.Utilities.uploadFinished = function(target, data) {
  var origButton = window[target];
  OB.Utilities.currentUploader = null;
  if (origButton && origButton.callback) {
    origButton.callback(data);
  }
};

OB.Utilities.writeErrorMessage = function(target, message) {
  var origView = window[target];
  origView.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, '', message);
};

OB.Utilities.currentUploader = null;
// ** {{{OB.Utilities.createLoadingLayout}}} **
// Creates a layout with the loading image.
OB.Utilities.createLoadingLayout = function(label) {
  var mainLayout = isc.HLayout.create({
    styleName: OB.Styles.LoadingPrompt.mainLayoutStyleName,
    width: '100%',
    height: '100%',
    align: 'center',
    defaultLayoutAlign: 'center'
  });
  var loadingLayout = isc.HLayout.create({
    align: 'center',
    defaultLayoutAlign: 'center',
    membersMargin: 0,
    overflow: 'visible'
  });
  if (!label) {
    label = OB.I18N.getLabel('OBUIAPP_LOADING');
  }
  mainLayout.addMember(loadingLayout);
  loadingLayout.addMember(
    isc.Label.create({
      contents: label,
      styleName: OB.Styles.LoadingPrompt.loadingTextStyleName,
      width: 100,
      align: 'right',
      overflow: 'visible'
    })
  );
  loadingLayout.addMember(isc.Img.create(OB.Styles.LoadingPrompt.loadingImage));
  return mainLayout;
};

// ** {{{OB.Utilities.addRequiredSuffixToBaseStyle}}} **
// Adds the Required suffix to a base style for a required formitem, to show it yellow in
// the forms.
OB.Utilities.addRequiredSuffixToBaseStyle = function(item) {
  if (
    item &&
    item.required &&
    item.textBoxStyle &&
    !item.textBoxStyle.endsWith('Required')
  ) {
    item.textBoxStyle = item.textBoxStyle + 'Required';
  }
};

// ** {{{OB.Utilities.determineViewOfFormItem}}} **
// Handles the different ways to find the view of a form item.
OB.Utilities.determineViewOfFormItem = function(item) {
  var form;
  if (!item) {
    return null;
  }
  form = item.form;
  if (form.view) {
    // form item in standard form
    return form.view;
  } else if (form.grid) {
    // row editor form item
    if (form.grid.view) {
      return form.grid.view;
    } else if (
      isc.isA.RecordEditor(form.grid) &&
      form.grid.sourceWidget &&
      form.grid.sourceWidget.view
    ) {
      // filter editor form item
      return form.grid.sourceWidget.view;
    }
  }
  return null;
};

// ** {{{OB.Utilities.callAction}}} **
// Calls the action defined by the action object, if the action object has a callback
// property, it is assumed to be a function and it is called. Otherwise the following
// properties are assumed to be in the action object: method (a function), target (the
// object to call the function on) and parameters (an array passed to the function).
// If action is null/undefined then nothing is done and undefined is returned.
// When the action is called the result of the action is returned.
OB.Utilities.callAction = function(action) {
  var response;

  function IEApplyHack(method, object, parameters) {
    if (!object) {
      object = window;
    }
    if (!parameters) {
      parameters = [];
    }

    object.customApplyMethod = method;

    var argsString = [],
      i,
      length = parameters.length;
    for (i = 0; i < length; i++) {
      argsString[i] = 'parameters[' + i + ']';
    }

    var argsList = argsString.join(',');

    var result = eval('object.customApplyMethod(' + argsList + ');');

    delete object.customApplyMethod;

    return result;
  }

  if (!action || !action.method) {
    return;
  }
  if (action.callback) {
    action.callback();
  } else {
    if (navigator.userAgent.toUpperCase().indexOf('MSIE') !== -1) {
      response = IEApplyHack(action.method, action.target, action.parameters);
    } else {
      response = action.method.apply(action.target, action.parameters);
    }
  }
  return response;
};

// ** {{{OB.Utilities.replaceNullStringValue}}} **
// Replaces values which are 'null' with null
OB.Utilities.replaceNullStringValue = function(form, values) {
  var prop;
  for (prop in values) {
    if (Object.prototype.hasOwnProperty.call(values, prop)) {
      var value = values[prop];
      if (value === 'null') {
        values[prop] = null;
      }
    }
  }
};

// ** {{{OB.Utilities.openDirectTab}}} **
// Open a view using a tab id and record id. The tab can be a child tab. If the record id
// is not set then the tab is opened in grid mode. If command is not set then default is
// used.
OB.Utilities.openDirectTab = function(
  tabId,
  recordId,
  command,
  position,
  criteria,
  direct,
  urlParams
) {
  var callback,
    isDirect = direct;
  // if the url params are not passed to the function, obtain then from the url
  urlParams = urlParams || OB.Utilities.getUrlParameters();
  tabId = OB.Utilities.removeFragment(tabId);
  recordId = OB.Utilities.removeFragment(recordId);
  command = OB.Utilities.removeFragment(command);

  //added to have the additional filter clause and tabid. Mallikarjun M
  callback = function(response, data, request) {
    command = command || 'DEFAULT';
    var view = {
      viewId: '_' + data.windowId,
      tabTitle: data.tabTitle,
      windowId: data.windowId,
      tabId: data.tabId,
      id: data.tabId,
      command: command,
      tabPosition: position
    };
    // new is only supported for the top tab
    if (command !== 'NEW') {
      view.targetTabId = tabId;
    }

    if (recordId) {
      view.targetRecordId = recordId;
      if (direct !== false) {
        isDirect = true;
      }
    }

    if (urlParams.criteria) {
      view.additionalCriteriaTabId = data.tabId;
      view.additionalCriteria = urlParams.criteria;
      if (urlParams.fkCache) {
        view.fkCache = urlParams.fkCache;
      }
    }

    if (urlParams.emptyFilterClause) {
      view.emptyFilterClause = urlParams.emptyFilterClause;
    }

    if (criteria) {
      view.additionalCriteriaTabId = data.tabId;
      view.additionalCriteria = criteria;
    }
    OB.Layout.ViewManager.openView(view.viewId, view, null, isDirect);
  };

  OB.RemoteCallManager.call(
    'org.openbravo.client.application.ComputeWindowActionHandler',
    {},
    {
      tabId: tabId,
      recordId: recordId
    },
    callback
  );
};

// ** {{{OB.Utilities.removeFragment}}} **
// remove a # and the rest from a string
OB.Utilities.removeFragment = function(str) {
  if (!str) {
    return str;
  }
  var index = str.indexOf('#');
  if (index !== -1) {
    return str.substring(0, index);
  }
  return str;
};

// ** {{{OB.Utilities.openView}}} **
// Open a view taking into account if a specific window should be opened in classic mode or not.
// Returns the object used to open the window.
OB.Utilities.openView = function(
  windowId,
  tabId,
  tabTitle,
  recordId,
  command,
  icon,
  readOnly,
  singleRecord,
  direct,
  editOrDeleteOnly
) {
  var openObject,
    isDirect = direct;
  if (recordId) {
    if (direct !== false) {
      isDirect = true;
    }
  }
  if (recordId) {
    openObject = {
      viewId: '_' + windowId,
      id: tabId,
      targetRecordId: recordId,
      targetTabId: tabId,
      tabTitle: tabTitle,
      windowId: windowId,
      readOnly: readOnly,
      singleRecord: singleRecord,
      editOrDeleteOnly: editOrDeleteOnly
    };
  } else {
    openObject = {
      viewId: '_' + windowId,
      id: tabId,
      tabId: tabId,
      tabTitle: tabTitle,
      windowId: windowId,
      icon: icon,
      readOnly: readOnly,
      singleRecord: singleRecord,
      editOrDeleteOnly: editOrDeleteOnly
    };
  }
  if (command) {
    openObject.command = command;
  }
  OB.Layout.ViewManager.openView(openObject.viewId, openObject, null, isDirect);
  return openObject;
};

// ** {{{OB.Utilities.openDirectView}}} **
// Open the correct view for a passed in target definition, coming from a certain source Window.
OB.Utilities.openDirectView = function(
  sourceWindowId,
  keyColumn,
  targetEntity,
  recordId,
  fieldId
) {
  var actionURL = OB.Application.contextUrl + 'utility/ReferencedLink.html',
    callback,
    reqObj;

  callback = function(response, data, request) {
    OB.Utilities.openView(
      data.windowId,
      data.tabId,
      data.tabTitle,
      data.recordId,
      null,
      null,
      null,
      null,
      true
    );
  };

  reqObj = {
    params: {
      Command: 'JSON',
      inpEntityName: targetEntity,
      inpKeyReferenceId: recordId,
      inpwindowId: sourceWindowId,
      inpKeyReferenceColumnName: keyColumn,
      inpFieldId: fieldId
    },
    callback: callback,
    evalResult: true,
    httpMethod: 'GET',
    useSimpleHttp: true,
    actionURL: actionURL
  };

  isc.RPCManager.sendRequest(reqObj);
};

// ** {{{OB.Utilities.getPromptString}}} **
// Translates a string or array of strings to a string with html returns.
OB.Utilities.getPromptString = function(msg) {
  var msgString = '',
    i,
    length;
  if (!isc.isAn.Array(msg)) {
    msg = [msg];
  }
  length = msg.length;
  for (i = 0; i < length; i++) {
    msgString += (i > 0 ? '<br>' : '') + msg[i].asHTML();
  }
  return msgString;
};
// ** {{{OB.Utilities.getUrlParameters}}} **
// Returns the url parameters as a javascript object. Note works for simple cases
// where no & is used for character encoding, this is fine for most cases.
OB.Utilities.getUrlParameters = function(href) {
  href = href || window.location.href;
  var vars = {},
    hash,
    length,
    hashes = href.slice(href.indexOf('?') + 1).split('&'),
    i;

  length = hashes.length;

  for (i = 0; i < length; i++) {
    hash = hashes[i].split('=');
    if (hash[i] && hash[i].contains('#')) {
      hash[i] = hash[i].substring(0, hash[i].indexOf('#'));
    }
    vars[hash[0]] = hash[1];
  }
  return vars;
};

// ** {{{OB.Utilities.hasUrlParameter}}} **
// Returns true if the url has a certain parameter with a certain value.
OB.Utilities.hasUrlParameter = function(name, value) {
  var url = window.document.location.href,
    checkPoint = url.indexOf(name + '=' + value);
  return checkPoint !== -1;
};

// ** {{{OB.Utilities.getLocationUrlWithoutFragment()}}} **
// Returns the url of the page without the fragment (the part starting with #)
OB.Utilities.getLocationUrlWithoutFragment = function() {
  var url = window.document.location.href,
    checkPoint = url.indexOf('#');
  if (checkPoint !== -1) {
    url = url.substring(0, checkPoint);
  }
  return url;
};

// ** {{{ OB.Utilities.openProcessPopup(/*String*/ processId }}} **
// Opens a separate window for classic OB windows.
// Parameters:
// * {{{url}}}: the url of the html page to open
// * {{{noFrameSet}}}: if true then the page is opened directly without a
// * {{{postParams}}}: if this object is set and noFrameSet is not true, main Framset send
//                     properties of this object to url as POST, other case a GET to url is
//                     performed
// frameset
OB.Utilities.openProcessPopup = function(
  url,
  noFrameSet,
  postParams,
  height,
  width
) {
  height = height || 450;
  width = width || 625;
  var top = (screen.height - height) / 2;
  var left = (screen.width - width) / 2;
  var adds =
    'height=' + height + ', width=' + width + ', left=' + left + ', top=' + top;
  adds += ', location=0';
  adds += ', scrollbars=1';
  adds += ', status=1';
  adds += ', menubar=0';
  adds += ', toolbar=0';
  adds += ', resizable=1';
  var winPopUp;

  if (noFrameSet) {
    winPopUp = window.open(url, 'PROCESS', adds);
  } else {
    winPopUp = window.open('', 'PROCESS', adds);
    var mainFrameSrc = !postParams ? 'src="' + url + '"' : '',
      html =
        '<html>' +
        '<frameset cols="0%,100%" frameborder="no" border="0" framespacing="0" rows="*" id="framesetMenu">' +
        '<frame name="frameMenu" scrolling="no" src="' +
        OB.Application.contextUrl +
        'utility/VerticalMenu.html?Command=LOADING" id="paramFrameMenuLoading"></frame>' +
        '<frame name="mainframe" noresize="" ' +
        mainFrameSrc +
        ' id="fieldProcessId"></frame>' +
        '<frame name="hiddenFrame" scrolling="no" noresize="" src=""></frame>' +
        '</frameset>' +
        '</html>';

    winPopUp.document.write(html);
    if (postParams) {
      var doc = winPopUp.frames[1].document,
        frm = doc.createElement('form'),
        i;
      frm.setAttribute('method', 'post');
      frm.setAttribute('action', url);
      for (i in postParams) {
        if (Object.prototype.hasOwnProperty.call(postParams, i)) {
          var inp = winPopUp.document.createElement('input');
          inp.setAttribute('type', 'hidden');
          inp.setAttribute('name', i);
          inp.setAttribute('value', postParams[i]);
          frm.appendChild(inp);
        }
      }
      doc.body.appendChild(frm);
      frm.submit();
    }
    winPopUp.document.close();
  }
  OB.Utilities.registerClassicPopupInTestRegistry(url, winPopUp);
  winPopUp.focus();
  return winPopUp;
};

// ** {{{ OB.Utilities.registerClassicPopupInTestRegistry(/*String*/ url, /*Object*/ obj }}} **
// Registers the obj as a classic popup
OB.Utilities.registerClassicPopupInTestRegistry = function(url, obj) {
  if (url.startsWith('/')) {
    var index = url.indexOf('/', 1);
    url = url.substring(index + 1);
  }
  if (url.indexOf('?') !== -1 && url.indexOf('Command') === -1) {
    OB.TestRegistry.register(
      'org.openbravo.classicpopup.' + url.substring(0, url.indexOf('?')),
      obj
    );
  } else {
    OB.TestRegistry.register('org.openbravo.classicpopup.' + url, obj);
  }
};

// ** {{{ OB.Utilities.isNonEmptyString(/*String*/ strValue }}} **
// Returns true if the parameter is a valid String which has length > 0
// Parameters:
// * {{{strValue}}}: the value to check
OB.Utilities.isNonEmptyString = function(strValue) {
  if (!strValue) {
    return false;
  }
  strValue = strValue.replace(/^\s*/, '').replace(/\s*$/, '');
  return strValue.length > 0;
};

// ** {{{ OB.Utilities.areEqualWithTrim(/*String*/ str1, /*String*/ str2}}} **
// Returns true if the two strings are equal after trimming them.
// Parameters:
// * {{{str1}}}: the first String to check
// * {{{str2}}}: the second String to compare
OB.Utilities.areEqualWithTrim = function(str1, str2) {
  if (!str1 || !str2) {
    return false;
  }
  str1 = str1.replace(/^\s*/, '').replace(/\s*$/, '');
  str2 = str2.replace(/^\s*/, '').replace(/\s*$/, '');
  return str1 === str2;
};

//** {{{ OB.Utilities.trim(/*String*/ str)}}} **
//Trims a string
OB.Utilities.trim = function(str) {
  if (!str) {
    return str;
  }
  return str.replace(/^\s*/, '').replace(/\s*$/, '');
};

OB.Utilities.processLogoutQueue = function() {
  var q = OB.Utilities.logoutWorkQueue,
    qElement,
    result,
    tab;

  if (q && q.length === 0) {
    return;
  }

  if (typeof arguments[1] === 'string') {
    // The 2nd parameter in a sendRequest callback is the 'data' parameter
    // http://www.smartclient.com/docs/7.0rc2/a/b/c/go.html#type..RPCCallback
    result = eval('(' + arguments[1] + ')');
    if (result && result.oberror) {
      if (result.oberror.type === 'Error') {
        tab = OB.MainView.TabSet.getTab(arguments[2].params.tabID);
        if (tab) {
          tab.pane.getAppFrameWindow().location.href = result.redirect;
        }
        q = [];
        return;
      }
    }
  }
  // Process one element at the time,
  // the save callbacks will empty the queue
  qElement = q.pop();
  if (qElement.func && qElement.args !== undefined) {
    qElement.func.apply(qElement.self, qElement.args);
  }
};

// ** {{{ OB.Utilities.logout }}} **
// Logout from the application, removes server side session info and redirects
// the client to the Login page.
OB.Utilities.logout = function(confirmed) {
  if (!confirmed) {
    isc.confirm(OB.I18N.getLabel('OBUIAPP_LogoutConfirmation'), function(ok) {
      if (ok) {
        OB.Utilities.logout(true);
      }
    });
    return;
  }
  OB.Utilities.logoutWorkQueue = [];
  var q = OB.Utilities.logoutWorkQueue,
    i,
    tabs = OB.MainView.TabSet.tabs,
    tabsLength = tabs.length,
    appFrame;

  // Push the logout process to the 'end' of the queue
  q.push({
    func: OB.RemoteCallManager.call,
    self: this,
    args: [
      'org.openbravo.client.application.LogOutActionHandler',
      {},
      {},
      function() {
        window.location.href = OB.Application.contextUrl;
      }
    ]
  });

  for (i = 0; i < tabsLength; i++) {
    if (tabs[i].pane.Class === 'OBClassicWindow') {
      appFrame =
        tabs[i].pane.appFrameWindow || tabs[i].pane.getAppFrameWindow();
      if (appFrame && appFrame.isUserChanges) {
        if (appFrame.validate && !appFrame.validate()) {
          q = [];
          return;
        }
        q.push({
          func: tabs[i].pane.saveRecord,
          self: tabs[i].pane,
          args: [tabs[i].ID, OB.Utilities.processLogoutQueue]
        });
      }
    }
  }
  OB.Utilities.processLogoutQueue();
};

// ** {{{ OB.Utilities.getYesNoDisplayValue }}} **
// Returns the Yes label if the passed value is true, the No label if false.
OB.Utilities.getYesNoDisplayValue = function(value) {
  if (value === true || value === 'true') {
    return OB.I18N.getLabel('OBUISC_Yes');
  } else if (value === false) {
    return OB.I18N.getLabel('OBUISC_No');
  } else {
    return '';
  }
};

// ** {{{ OB.Utilities.getClassicValue }}} **
// Returns the Y if the passed value is true, and N if false.
OB.Utilities.getClassicValue = function(value) {
  if (value) {
    return 'Y';
  } else if (value === false) {
    return 'N';
  } else {
    return '';
  }
};

// ** {{{ OB.Utilities.applyDefaultValues }}} **
//
// Sets the value for each property in the defaultValues in the Fields object
// if it is not set there yet.
//
// Parameters:
// * {{{fields}}}: the current values
// * {{{defaultValues}}}: the default values to set in the fields object (if the
// property is not set in the fields object).
OB.Utilities.applyDefaultValues = function(fields, defaultValues) {
  var fieldsLength = fields.length,
    i,
    property;
  for (i = 0; i < fieldsLength; i++) {
    var field = fields[i];
    for (property in defaultValues) {
      if (Object.prototype.hasOwnProperty.call(defaultValues, property)) {
        if (!field[property] && field[property] !== false) {
          field[property] = defaultValues[property];
        }
      }
    }
  }
};

// ** {{{ OB.Utilities.addFormInputsToCriteria }}} **
//
// Adds all input values on the standard OB form (document.frmMain) to the
// criteria object.
//
// Parameters:
// * {{{criteria}}}: the current criteria object.
// * {{{win}}}: (Optional) a reference to the global context (window) where to
// get the document
// and functions are located, if not passed, the current window is used
OB.Utilities.addFormInputsToCriteria = function(criteria, win) {
  var d = (win && win.document ? win.document : null) || window.document,
    elementsLength = d.frmMain ? d.frmMain.elements.length : 0,
    inputValue =
      (win && win.inputValue ? win.inputValue : null) || window.inputValue,
    i,
    elem;

  for (i = 0; i < elementsLength; i++) {
    elem = d.frmMain.elements[i];
    if (elem.name) {
      criteria[elem.name] = inputValue(elem);
    }
  }

  // the form can have an organization field,
  // in the server it is used to determine the accessible orgs
  // TODO: make this optional or make it possible to set the orgid html id
  if (d.frmMain.inpadOrgId) {
    criteria[OB.Constants.ORG_PARAMETER] = inputValue(d.frmMain.inpadOrgId);
  }
};

//** {{{ OB.Utilities.postThroughHiddenForm }}} **
//
// Global method to post a request through a hidden form located on:
// org.openbravo.client.application/index.html
//
// Parameters:
// * {{{url}}}: the url to post the request.
// * {{{data}}}: the data to include in the request.
OB.Utilities.postThroughHiddenForm = function(url, data) {
  var key;
  OB.GlobalHiddenForm.setAttribute('action', url);

  // remove all children, needs to be done like this as the
  // children array is getting updated while removing a child
  while (OB.GlobalHiddenForm.children[0]) {
    OB.GlobalHiddenForm.removeChild(OB.GlobalHiddenForm.children[0]);
  }

  var encodeProperties = {
    // prevents timezone issues
    encodeDate: function(dt) {
      var ret,
        oldXMLSchemaMode = isc.Comm.xmlSchemaMode;
      isc.Comm.xmlSchemaMode = true;
      ret = dt.toSerializeableDate();
      isc.Comm.xmlSchemaMode = oldXMLSchemaMode;
      return '"' + ret + '"';
    }
  };

  for (key in data) {
    if (Object.prototype.hasOwnProperty.call(data, key)) {
      var field = document.createElement('input');
      field.setAttribute('type', 'hidden');
      field.setAttribute('name', key);
      if (isc.isA.Object(data[key])) {
        field.setAttribute(
          'value',
          isc.JSON.encode(data[key], encodeProperties)
        );
      } else {
        field.setAttribute('value', data[key]);
      }
      OB.GlobalHiddenForm.appendChild(field);
    }
  }

  OB.GlobalHiddenForm.submit();
};

// ** {{{ OB.Utilities.updateSmartClientComponentValue }}} **
//
// Updates the value of a smartclient component.
//
// Parameters:
// * {{{input}}}: the input field (html dom input element)
// * {{{component}}}: the Smartclient component (must have a setValue function)
OB.Utilities.updateSmartClientComponentValue = function(input, component) {
  component.setValue(input.value);
};

//** {{{ OB.Utilities.fixNull250 }}} **
//
// Transforms null values into '' to adapt display logic to 2.50 behavior.
//
// Parameters:
// * {{{currentValues}}}: array of values
OB.Utilities.fixNull250 = function(currentValues) {
  var i;
  for (i in currentValues) {
    if (
      Object.prototype.hasOwnProperty.call(currentValues, i) &&
      (currentValues[i] === null || currentValues[i] === undefined)
    ) {
      currentValues[i] = '';
    }
  }
};

// ** {{{ OB.Utilities.isValidURL}}} **
// Checks if a String is a valid URL
//
// Parameters:
// * {{{url}}}: String url
OB.Utilities.isValidURL = function(url) {
  // Validation based on: http://view.jquery.com/trunk/plugins/validate/jquery.validate.js
  // Note: http://localhost is not a valid URL, http://localhost.localdomain is a valid one
  if (!url) {
    return false;
  }
  // prettier-ignore
  return (/^(https?|ftp|file):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!$&'()*+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!$&'()*+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!$&'()*+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!$&'()*+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!$&'()*+,;=]|:|@)|\/|\?)*)?$/i).test(
    url
  );
};

// ** {{{ applicationUrl(path) }}} **
//
// Get the full URL to the supplied path under the application context
//
// Parameters:
//  * {{{path}}} path portion of URL
//
OB.Utilities.applicationUrl = function(path) {
  var appUrl = OB.Application.contextUrl + path;
  if (appUrl.indexOf('//') === 0) {
    // Double slash at start of relative URL only keeps scheme, not server
    appUrl = appUrl.substring(1);
  }
  return appUrl;
};

// ** {{{ refreshWorkspaceWidget(widgetId) }}} **
//
// Refresh all instances of the widget placed in the Workspace of the given widget id
//
// Parameters:
//  * {{{widgetId}}} the widget id
//
OB.Utilities.refreshWorkspaceWidget = function(widgetId) {
  var i;
  if (!OB.MyOB || !OB.MyOB.widgets) {
    return;
  }
  for (i = 0; i < OB.MyOB.widgets.length; i++) {
    if (OB.MyOB.widgets[i].widgetClassId === widgetId) {
      if (OB.MyOB.widgets[i].ID && isc.Canvas.getById(OB.MyOB.widgets[i].ID)) {
        isc.Canvas.getById(OB.MyOB.widgets[i].ID).refresh();
      }
    }
  }
};

OB.Utilities.formatTimePassedMessage = function(n, messageId) {
  var message = OB.I18N.getLabel(messageId, [n]);
  return message;
};

OB.Utilities.getTimePassed = function(created) {
  // 0-59 minutes: minutes
  // 1-24 hours: hours
  // >24 hours: days
  // >7 days: weeks
  // >30 days: months
  var now = new Date(),
    msCreated = created.getTime(),
    msNow = now.getTime();

  // time difference in days
  return OB.Utilities.getTimePassedInterval(msNow - msCreated);
};

OB.Utilities.getTimePassedInterval = function(timeInMiliseconds) {
  var n;
  var diffDays = Math.floor(timeInMiliseconds / (1000 * 60 * 60 * 24));
  if (diffDays >= 30) {
    n = Math.floor(diffDays / 30);
    return OB.Utilities.formatTimePassedMessage(n, 'OBUIAPP_months_ago_1');
  } else if (diffDays >= 7) {
    n = Math.floor(diffDays / 7);
    return OB.Utilities.formatTimePassedMessage(n, 'OBUIAPP_weeks_ago_1');
  } else if (diffDays >= 1) {
    n = diffDays;
    return OB.Utilities.formatTimePassedMessage(n, 'OBUIAPP_days_ago_1');
  }

  // time difference in hours
  var diffHours = Math.floor(timeInMiliseconds / (1000 * 60 * 60));
  if (diffHours >= 1) {
    n = diffHours;
    return OB.Utilities.formatTimePassedMessage(n, 'OBUIAPP_hours_ago_1');
  }

  // time difference in minutes
  n = Math.floor(timeInMiliseconds / (1000 * 60));
  return OB.Utilities.formatTimePassedMessage(n, 'OBUIAPP_minutes_ago_1');
};

//** {{{ OB.Utilities.getValue }}} **
//
// Gets the value of a field using the square bracket notation
// This prevents errors from happening when the name of the property
// is a reserved javascript word
OB.Utilities.getValue = function(object, property) {
  return object[property];
};

//** {{{ OB.Utilities.generateRandomString }}} **
//
// Generates a random string based on the arguments
// Parameters:
//  * {{{stringLength}}} Length of the generated random string
//  * {{{allowLowerCaseChars}}} Boolean to check if lower case characters are allowed (true by default)
//  * {{{allowUpperCaseChars}}} Boolean to check if upper case characters are allowed (true by default)
//  * {{{allowDigits}}} Boolean to check if digits are allowed (false by default)
//  * {{{allowSpecialChars}}} Boolean to check if special characters are allowed (false by default)
OB.Utilities.generateRandomString = function(
  stringLength,
  allowLowerCaseChars,
  allowUpperCaseChars,
  allowDigits,
  allowSpecialChars
) {
  stringLength = parseInt(stringLength, 10);
  if (!stringLength) {
    stringLength = 1;
  }
  allowLowerCaseChars = allowLowerCaseChars !== false ? true : false;
  allowUpperCaseChars = allowUpperCaseChars !== false ? true : false;
  allowDigits = allowDigits !== true ? false : true;
  allowSpecialChars = allowSpecialChars !== true ? false : true;

  var chars = '',
    randomString = '',
    i,
    rnum;
  if (allowLowerCaseChars) {
    chars += 'abcdefghijklmnopqrstuvwxyz';
  }
  if (allowUpperCaseChars) {
    chars += 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  }
  if (allowDigits) {
    chars += '0123456789';
  }
  if (allowSpecialChars) {
    chars += '!@#$%^&*()+=-[]\\\';,./{}|":<>?~_';
  }
  if (chars === '') {
    return '';
  }

  for (i = 0; i < stringLength; i++) {
    rnum = Math.floor(Math.random() * chars.length);
    randomString += chars.substring(rnum, rnum + 1);
  }
  return randomString;
};

/* This function will return true if it receives a string parameter, and
 * which complies with the OB UUID format (that is, its a
 * hexadecimal number of length 32 or numeric numbers of length less than or equal to 10)
 */
OB.Utilities.isUUID = function(object) {
  if (typeof object !== 'string') {
    return false;
  }
  if (object.length > 10 && object.length !== 32) {
    return false;
  }
  if (object.length === 32) {
    // prettier-ignore
    return (/[A-Fa-f0-9]{32,32}/).test(object);
  } else if (object.length <= 10) {
    //return true if uuid contains only numbers
    return /^\d+$/.test(object);
  }
};

//** {{{ OB.Utilities.clientClassSplitProps }}} **
//
// Auxiliary function to split a string containing the clientClass and its properties
// Returns as an array the clientClass (first element) and its attributes (second element)
OB.Utilities.clientClassSplitProps = function(clientClass) {
  var clientClassPropsStartPosition, clientClassProps, ret;
  if (!clientClass) {
    clientClass = '';
  }
  clientClassPropsStartPosition = clientClass.indexOf('{');
  if (clientClassPropsStartPosition > 0) {
    clientClassProps = clientClass.substring(
      clientClassPropsStartPosition,
      clientClass.length
    );
    try {
      clientClassProps = JSON.parse(clientClassProps);
    } catch (e) {
      clientClassProps = {};
    }
    clientClass = clientClass.substring(0, clientClassPropsStartPosition);
  } else {
    clientClassProps = {};
  }
  clientClass = clientClass.replace(/\s+/g, '');
  ret = [clientClass, clientClassProps];
  return ret;
};

//** {{{ OB.Utilities.getCanvasProp }}} **
//
// Returns the value of the property of the canvas item
// Parameters:
//  * {{{canvas}}} String the canvas item
//  * {{{prop}}} String the property to get the value
OB.Utilities.getCanvasProp = function(canvas, prop) {
  if (canvas.indexOf('{') !== -1) {
    canvas = OB.Utilities.clientClassSplitProps(canvas)[0];
  }
  if (new Function('if (window.' + canvas + ') { return true; }')()) {
    if (
      new Function(
        'if (typeof ' +
          canvas +
          '.getInstanceProperty === "function") { return true; }'
      )()
    ) {
      // 'getInstanceProperty' is a Smartclient function to determine the property value of a canvas
      return new Function(
        'return ' + canvas + '.getInstanceProperty("' + prop + '")'
      )();
    }
  }
  return;
};

//** {{{ OB.Utilities.getRGBAStringFromOBColor }}} **
//
// Returns a string like "rgba(*RedValue*, *GreenValue*, *BlueValue*, *OpacityValue*)" from an OBColor value
// Parameters:
//  * {{{color}}} OBColor value
OB.Utilities.getRGBAStringFromOBColor = function(color) {
  var rgbaColor = 'rgba(',
    colorArray,
    i;
  if (!color) {
    return;
  }
  colorArray = color.split('-');
  for (i = 0; i < colorArray.length; i++) {
    if (i < 3) {
      rgbaColor += parseInt(colorArray[i], 10);
      rgbaColor += ',';
    } else {
      rgbaColor += parseInt(colorArray[i], 10) / 100;
    }
  }
  rgbaColor += ')';
  return rgbaColor;
};

//** {{{ OB.Utilities.getBrightFromOBColor }}} **
//
// Returns the bright value (from 0 to 255) from an OBColor value
// Parameters:
//  * {{{color}}} OBColor value
OB.Utilities.getBrightFromOBColor = function(color) {
  var bright = 0,
    colorArray;
  if (!color) {
    return;
  }
  colorArray = color.split('-');
  bright = bright + parseInt(colorArray[0], 10) * 299;
  bright = bright + parseInt(colorArray[1], 10) * 587;
  bright = bright + parseInt(colorArray[2], 10) * 114;
  bright = bright / 1000;
  bright = parseInt(bright, 10);
  return bright;
};

//** {{{ OB.Utilities.generateOBColor }}} **
//
// Returns a string with an OBColor
// Parameters:
//  * {{{r}}} Red component. From 0 to 255. If not set, a random one will be generated
//  * {{{g}}} Green component. From 0 to 255. If not set, a random one will be generated
//  * {{{b}}} Blue component. From 0 to 255. If not set, a random one will be generated
//  * {{{a}}} Alpha channel (opacity) component. From 0 to 100. If not set, a random one will be generated
//  * {{{seed}}} Optional seed for the random color generation
OB.Utilities.generateOBColor = function(r, g, b, a, seed) {
  var getRandomInt,
    randomInt,
    getChannel,
    obcolor = '';
  getRandomInt = function(seed) {
    if (seed === null || typeof seed === 'undefined') {
      seed = new Date().getTime();
    }
    seed = seed.toString();
    seed = seed.replace(/[&\/\\#,+()$~%.'":*?<>{}]/g, '0'); //eslint-disable-line
    seed = seed
      .replace(/[a-j]/g, '1')
      .replace(/[j-t]/g, '2')
      .replace(/[u-z]/g, '3');
    seed = seed
      .replace(/[A-J]/g, '4')
      .replace(/[J-T]/g, '4')
      .replace(/[U-Z]/g, '6');
    seed = seed + seed.length.toString();
    seed = parseInt(seed, 10);

    return {
      next: function(min, max) {
        var randomNum;
        seed *= 1234;
        seed += 56;
        seed *= 7890;
        randomNum = (seed % 10000000000000) / 100000;
        randomNum = randomNum - Math.floor(randomNum);
        randomNum = Math.floor(randomNum * (max - min + 1)) + min;
        return randomNum;
      }
    };
  };
  randomInt = getRandomInt(seed);
  getChannel = function(channel, min, max) {
    var randomValueInt = randomInt.next(min, max);
    if (channel) {
      channel = parseInt(channel.toString(), 10);
    } else {
      channel = randomValueInt;
    }
    if (channel < min) {
      channel = min;
    }
    if (channel > max) {
      channel = max;
    }
    channel = channel.toString();
    if (channel.length === 2) {
      channel = '0' + channel;
    } else if (channel.length === 1) {
      channel = '00' + channel;
    }
    return channel;
  };
  r = getChannel(r, 0, 255);
  obcolor += r;
  obcolor += '-';
  g = getChannel(g, 0, 255);
  obcolor += g;
  obcolor += '-';
  b = getChannel(b, 0, 255);
  obcolor += b;
  obcolor += '-';
  a = getChannel(a, 0, 100);
  obcolor += a;

  return obcolor;
};

//** {{{ OB.Utilities.getProcessTabBarPosition }}} **
//
// Returns the position of a process window in the main tab bar
// Parameters:
//  * {{{processView}}} The process view
OB.Utilities.getProcessTabBarPosition = function(processView) {
  var len = OB.MainView.TabSet.paneContainer.members.length,
    i;
  for (i = 0; i < len; i++) {
    if (processView.ID === OB.MainView.TabSet.paneContainer.members[i].ID) {
      return i;
    }
  }
  return -1;
};

OB.Utilities.yesNoSortNormalizer = function(item, field, context) {
  var value = item[field];
  if (value === true) {
    return 1;
  } else if (value === false) {
    return 2;
  } else {
    return 3;
  }
};

OB.Utilities.enumSortNormalizer = function(item, field, context) {
  var value = item[field],
    undef;
  if (value === null || value === undef) {
    return '-'; // hack to sort nulls last
  }
  return ' ' + value;
};

//** {{{ OB.Utilities.getTabNumber }}} **
//
// Returns the position of a window in the main tab bar
// Parameters:
//  * {{{tabId}}} The if of the tab to search
OB.Utilities.getTabNumberById = function(tabId) {
  var i = 0,
    len = OB.MainView.TabSet.paneContainer.members.length;
  for (i; i < len; i++) {
    if (
      tabId === OB.MainView.TabSet.getTabObject(i).id ||
      tabId === OB.MainView.TabSet.getTabObject(i).pane.targetTabId
    ) {
      return i;
    }
  }
  return -1;
};

//** {{{ OB.Utilities.getObjectSize }}} **
//
// Returns the size of an object (only the first level)
// Parameters:
//  * {{{object}}} The object to get the size
OB.Utilities.getObjectSize = function(object) {
  var size = 0,
    key;
  if (Object.prototype.toString.call(object) !== '[object Object]') {
    return false;
  }
  for (key in object) {
    if (Object.prototype.hasOwnProperty.call(object, key)) {
      size++;
    }
  }
  return size;
};

//** {{{ OB.Utilities.createResultSetManually }}} **
//
// Creates a ResultSet manually for a grid
// Parameters:
//  * {{{grid}}} The grid whose ResultSet will be created manually
OB.Utilities.createResultSetManually = function(grid) {
  grid.dataProperties.dataSource = grid.dataSource;
  grid.dataProperties.initialData = [];
  grid.dataProperties.resultSize = 100;
  grid.dataProperties.useClientFiltering = false;
  grid.dataProperties.manualResultSet = true;
  grid.setData(grid.createDataModel());
  grid.serverDataNotLoaded = true;
};

//** {{{ OB.Utilities.getTemporaryId }}} **
//
// Returns a temporary id that starts with _
OB.Utilities.getTemporaryId = function() {
  return '_' + new Date().getTime();
};

//** {{{ OB.Utilities.removeAccents }}} **
//
// Returns the string without accents
// Parameters:
//  * {{{value}}} The string value to be cleaned of accents
OB.Utilities.removeAccents = function(value) {
  if (value && typeof value === 'string') {
    value = value.replace(/á|à|ä|â/g, 'a').replace(/Á|À|Ä|Â/g, 'A');
    value = value.replace(/é|è|ë|ê/g, 'e').replace(/É|È|Ë|Ê/g, 'E');
    value = value.replace(/í|ì|ï|î/g, 'i').replace(/Í|Ì|Ï|Î/g, 'I');
    value = value.replace(/ó|ò|ö|ô/g, 'o').replace(/Ó|Ò|Ö|Ô/g, 'O');
    value = value.replace(/ú|ù|ü|û/g, 'u').replace(/Ú|Ù|Ü|Û/g, 'U');
    value = value.replace(/ç/g, 'c').replace(/Ç/g, 'C');
    value = value.replace(/ñ/g, 'n').replace(/Ñ/g, 'N');
  }
  return value;
};
