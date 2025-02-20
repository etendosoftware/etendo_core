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
 * All portions are Copyright (C) 2017-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

/**
 * @fileoverview Contains Javascript functions used by the Login window.
 */

/**
 * Global variables.
 */

var isRecBrowserMsgShown = false;

/**
 * Functions to perform the login operation and handle its result.
 */

function setLoginMessage(type, title, text) {
  if (type === 'Error') {
    var msgContainer = document.getElementById('errorMsg');
    var msgContainerTitle = document.getElementById('errorMsgTitle');
    var msgContainerTitleContainer = document.getElementById(
      'errorMsgTitle_Container'
    );
    var msgContainerContent = document.getElementById('errorMsgContent');
    if (typeof title !== 'undefined' && title !== '' && title !== null) {
      msgContainerTitle.innerHTML = title
        .replace(/\n/g, '<br>')
        .replace(/\\n/g, '<br>');
      msgContainerTitleContainer.style.display = '';
    } else {
      msgContainerTitle.innerHTML = '';
      msgContainerTitleContainer.style.display = 'none';
    }
    if (typeof text !== 'undefined' && text !== '' && text !== null) {
      msgContainerContent.innerHTML = text
        .replace(/\n/g, '<br>')
        .replace(/\\n/g, '<br>');
    } else {
      msgContainerContent.innerHTML = '';
    }
    msgContainer.style.display = '';
    isRecBrowserMsgShown = false;
    return false;
  } else if (type === 'Warning' || type === 'Confirmation') {
    var alertText = '';
    if (typeof title !== 'undefined' && title !== '' && title !== null) {
      alertText += title.replace(/<br>/g, '\n') + '\n';
    }
    if (typeof text !== 'undefined' && text !== '' && text !== null) {
      alertText += text.replace(/<br>/g, '\n');
    }
    if (type === 'Warning') {
      alert(alertText);
      return true;
    } else {
      return confirm(alertText);
    }
  }
}

function doLogin(command) {
  doLoginWithToken()
  var extraParams;
  if (
    document.getElementById('resetPassword').value === 'true' &&
    document.getElementById('newPass').value !==
      document.getElementById('password').value
  ) {
    setLoginMessage('Error', errorSamePassword, errorDifferentPasswordInFields);
    return true;
  }
  if (
    focusedWindowElement.id === 'user' &&
    document.getElementById('user').value !== '' &&
    document.getElementById('password').value === ''
  ) {
    setTimeout(function() {
      // To manage browser autocomplete feature if it is active
      if (
        focusedWindowElement.id === 'user' &&
        document.getElementById('password').value === ''
      ) {
        setWindowElementFocus(document.getElementById('password'));
      } else {
        return true;
      }
    }, 10);
  } else if (
    focusedWindowElement.id === 'password' &&
    document.getElementById('password').value !== '' &&
    document.getElementById('user').value === ''
  ) {
    setWindowElementFocus(document.getElementById('user'));
  } else {
    if (
      document.getElementById('user').value === '' ||
      document.getElementById('password').value === ''
    ) {
      setLoginMessage('Error', identificationFailureTitle, errorEmptyContent);
      return true;
    }
    disableButton('buttonOK');
    command =
      command ||
      (document.getElementById('resetPassword').value === 'true'
        ? 'FORCE_RESET_PASSWORD'
        : 'DEFAULT');
    extraParams = '&targetQueryString=' + getURLQueryString();
    submitXmlHttpRequest(
      loginResult,
      document.frmIdentificacion,
      command,
      '../secureApp/LoginHandler.html',
      false,
      extraParams,
      null
    );
  }

  return false;
}

function doLoginWithToken() {
    const params = new URLSearchParams(window.location.hash.substring(1));
    const accessToken = params.get("access_token");

    if (accessToken) {
        var xhr = new XMLHttpRequest();
        var loginUrl = "../../google/org.openbravo.client.kernel/OBCLKER_Kernel/SessionDynamic"; // Igual que en doLogin()
        var requestData = new FormData();  // FormData para simular un formulario

        // Agregar parámetros requeridos
        requestData.append("access_token", accessToken);
        requestData.append("user", "admin"); // Ajustar dinámicamente según el usuario autenticado
        requestData.append("targetQueryString", getURLQueryString());

        xhr.open("POST", loginUrl, true);
        xhr.setRequestHeader("Authorization", "Bearer " + accessToken);

        xhr.onreadystatechange = function () {
            if (xhr.readyState === 4) {
                if (xhr.status >= 200 && xhr.status < 300) {
                    console.log("Login success:", xhr.responseText);
                } else {
                    console.error("HTTP error! Status:", xhr.status, xhr.responseText);
                }
            }
        };

        xhr.send(requestData); // Enviamos el formulario simulado
    }
}


function getURLQueryString() {
  return encodeURIComponent(window.location.search.substr(1));
}

function loginResult(paramXMLParticular, XMLHttpRequestObj) {
  var strText = '';
  if (getReadyStateHandler(XMLHttpRequestObj, null, false)) {
    if (XMLHttpRequestObj.responseText) {
      strText = XMLHttpRequestObj.responseText;
    }
    strText = strText.toString();
    var result = JSON.parse(strText);
    processResult(result);
  }
}

function processResult(result) {
  var shouldContinue = true;
  if (result.showMessage) {
    shouldContinue = setLoginMessage(
      result.messageType,
      result.messageTitle,
      result.messageText
    );
    if (!shouldContinue) {
      document.getElementById('password').value = '';
    }
  }
  if (result.resetPassword) {
    document.getElementById('resetPassword').value = result.resetPassword;
    document.getElementById('user').style.display = 'none';
    document.getElementById('newPass').style.display = '';
    document.getElementById('newPass').value = '';
    document.getElementById('password').placeholder = 'Confirm Password';
  }
  if (shouldContinue) {
    if (result.showMessage && result.messageType === 'Confirmation') {
      doLogin(result.command);
    } else {
      window.location = result.target;
    }
  } else if (result.resetPassword) {
    enableButton('buttonOK');
    setWindowElementFocus('newPass', 'id');
  } else {
    enableButton('buttonOK');
    setWindowElementFocus('password', 'id');
  }
}

/**
 * Functions invoked on login page load.
 */

function redirectWhenPopup() {
  var permission = false;
  try {
    if (top.opener.parent.frames['appFrame']) {
      permission = true;
    }
  } catch (e) {}
  if (permission && top.opener) {
    top.opener.parent.location.href = top.document.location.href;
    top.window.close();
  }
}

function redirectWhenInsideMDI() {
  if (typeof isWindowInMDIPage !== 'undefined' && isWindowInMDIPage) {
    var LayoutMDI = null;
    if (isWindowInMDIPopup && parent.opener) {
      LayoutMDI = parent.opener.getFrame('LayoutMDI'); // Since getFrame('LayoutMDI') function frameset checks equals the current opened Login_FS.html modal popup
    } else {
      LayoutMDI = getFrame('LayoutMDI');
    }
    if (
      LayoutMDI &&
      typeof parent.document.getElementById('framesetMenu') === 'object'
    ) {
      LayoutMDI.location.href = location.href || parent.window.location.href;
    }
  }
}

function manageVisualPreferences() {
  var topLogos = document.getElementById('TopLogos_Container');
  var bottomLogos = document.getElementById('BottomLogos_Container');
  if (showSupportLogo) {
    topLogos.className = 'Login_TopLogos_Container_Support';
  } else {
    topLogos.className = 'Login_TopLogos_Container_None';
  }

  if (showCompanyLogo && urlCompany !== '') {
    document.getElementById('CompanyLogo_Container').innerHTML =
      '<a href="' +
      urlCompany +
      '" target="_blank" class="Login_Img_Link">' +
      document.getElementById('CompanyLogo_Container').innerHTML +
      '</a>';
  }

  if (showSupportLogo && urlSupport !== '') {
    document.getElementById('SupportLogo_Container').innerHTML =
      '<a href="' +
      urlSupport +
      '" target="_blank" class="Login_Img_Link">' +
      document.getElementById('SupportLogo_Container').innerHTML +
      '</a>';
  }

  if (showCompanyLogo) {
    document.getElementById('CompanyLogo_Container').style.display = '';
  }
  topLogos.style.display = '';
  bottomLogos.style.display = '';
}

function maskLoginWindow(errorMsg) {
  var client = document.getElementById('client');
  var blocker = document.getElementById('blocker');
  blocker.innerHTML =
    '<div class="Login_Home_Logo_Icon"></div><div class="error-text">' +
    errorMsg +
    '</div>';
  blocker.style.display = '';
  client.style.display = 'none';
}

function browserVersionToFloat(versionNum) {
  while (versionNum.indexOf('.') !== versionNum.lastIndexOf('.')) {
    versionNum =
      versionNum.substring(0, versionNum.lastIndexOf('.')) +
      versionNum.substring(versionNum.lastIndexOf('.') + 1, versionNum.length);
  }
  versionNum = parseFloat(versionNum, 10);
  return versionNum;
}

function browserVersionTrim(versionNum) {
  while (
    (versionNum.substring(versionNum.length - 1, versionNum.length) === '0' &&
      versionNum.indexOf('.') !== -1) ||
    versionNum.substring(versionNum.length - 1, versionNum.length) === '.'
  ) {
    versionNum = versionNum.substring(0, versionNum.length - 1);
  }
  return versionNum;
}

function isValidBrowser(browser) {
  return browser in validBrowserVersions;
}

function isRecBrowser(browser) {
  return browser in recBrowserVersions;
}

function checkBrowserCompatibility() {
  var browserName = getBrowserInfo('name');
  var browserVersion = getBrowserInfo('version');
  var isValid = false;
  if (
    browserName.toUpperCase().indexOf('FIREFOX') != -1 ||
    browserName.toUpperCase().indexOf('ICEWEASEL') != -1
  ) {
    if (
      isValidBrowser('firefox') &&
      browserVersionToFloat(browserVersion) >=
        browserVersionToFloat(validBrowserVersions.firefox)
    ) {
      isValid = true;
    }
  } else if (browserName.toUpperCase().indexOf('INTERNET EXPLORER') != -1) {
    if (
      isValidBrowser('explorer') &&
      browserVersionToFloat(browserVersion) >=
        browserVersionToFloat(validBrowserVersions.explorer)
    ) {
      isValid = true;
    }
  } else if (browserName.toUpperCase().indexOf('GOOGLE CHROME') != -1) {
    if (
      isValidBrowser('chrome') &&
      browserVersionToFloat(browserVersion) >=
        browserVersionToFloat(validBrowserVersions.chrome)
    ) {
      isValid = true;
    }
  } else if (browserName.toUpperCase().indexOf('APPLE SAFARI') != -1) {
    if (
      isValidBrowser('safari') &&
      browserVersionToFloat(browserVersion) >=
        browserVersionToFloat(validBrowserVersions.safari)
    ) {
      isValid = true;
    }
  } else if (browserName.toUpperCase().indexOf('MICROSOFT EDGE') != -1) {
    if (
      isValidBrowser('edge') &&
      browserVersionToFloat(browserVersion) >=
        browserVersionToFloat(validBrowserVersions.edge)
    ) {
      isValid = true;
    }
  }
  return isValid;
}

function checkRecommendedBrowser() {
  var browserName = getBrowserInfo('name');
  var browserVersion = getBrowserInfo('version');
  var isRecommended = false;
  if (
    browserName.toUpperCase().indexOf('FIREFOX') != -1 ||
    browserName.toUpperCase().indexOf('ICEWEASEL') != -1
  ) {
    if (
      isRecBrowser('firefox') &&
      browserVersionToFloat(browserVersion) >=
        browserVersionToFloat(recBrowserVersions.firefox)
    ) {
      isRecommended = true;
    }
  } else if (browserName.toUpperCase().indexOf('INTERNET EXPLORER') != -1) {
    if (
      isRecBrowser('explorer') &&
      browserVersionToFloat(browserVersion) >=
        browserVersionToFloat(recBrowserVersions.explorer)
    ) {
      isRecommended = true;
    }
  } else if (browserName.toUpperCase().indexOf('GOOGLE CHROME') != -1) {
    if (
      isRecBrowser('chrome') &&
      browserVersionToFloat(browserVersion) >=
        browserVersionToFloat(recBrowserVersions.chrome)
    ) {
      isRecommended = true;
    }
  } else if (browserName.toUpperCase().indexOf('APPLE SAFARI') != -1) {
    if (
      isRecBrowser('safari') &&
      browserVersionToFloat(browserVersion) >=
        browserVersionToFloat(recBrowserVersions.safari)
    ) {
      isRecommended = true;
    }
  } else if (browserName.toUpperCase().indexOf('MICROSOFT EDGE') != -1) {
    if (
      isRecBrowser('edge') &&
      browserVersionToFloat(browserVersion) >=
        browserVersionToFloat(recBrowserVersions.edge)
    ) {
      isRecommended = true;
    }
  }
  return isRecommended;
}

function buildValidBrowserMsg() {
  var displayValidBrowserMsg = validBrowserMsg;
  if (isValidBrowser('firefox')) {
    displayValidBrowserMsg +=
      '<br>' +
      ' * Mozilla Firefox ' +
      browserVersionTrim(validBrowserVersions.firefox) +
      ' ' +
      validBrowserMsgOrHigher;
  }
  if (isValidBrowser('chrome')) {
    displayValidBrowserMsg +=
      '<br>' +
      ' * Google Chrome ' +
      browserVersionTrim(validBrowserVersions.chrome) +
      ' ' +
      validBrowserMsgOrHigher;
  }
  if (isValidBrowser('explorer')) {
    displayValidBrowserMsg +=
      '<br>' +
      ' * Microsoft Internet Explorer ' +
      browserVersionTrim(validBrowserVersions.explorer) +
      ' ' +
      validBrowserMsgOrHigher;
  }
  if (isValidBrowser('edge')) {
    displayValidBrowserMsg +=
      '<br>' +
      ' * Microsoft Edge ' +
      browserVersionTrim(validBrowserVersions.edge) +
      ' ' +
      validBrowserMsgOrHigher;
  }
  if (isValidBrowser('safari')) {
    displayValidBrowserMsg +=
      '<br>' +
      ' * Apple Safari ' +
      browserVersionTrim(validBrowserVersions.safari) +
      ' ' +
      validBrowserMsgOrHigher;
  }

  return displayValidBrowserMsg;
}

function buildRecBrowserMsgText() {
  var displayRecBrowserMsgText = recBrowserMsgText;
  xxReplaceMsg = '';
  yyReplaceMsg = '';
  if (isRecBrowser('chrome')) {
    xxReplaceMsg +=
      'Google Chrome ' + browserVersionTrim(recBrowserVersions.chrome) + ', ';
  }
  if (isRecBrowser('firefox')) {
    xxReplaceMsg +=
      'Mozilla Firefox ' +
      browserVersionTrim(recBrowserVersions.firefox) +
      ', ';
  }
  if (isRecBrowser('explorer')) {
    xxReplaceMsg +=
      'Internet Explorer ' +
      browserVersionTrim(recBrowserVersions.explorer) +
      ', ';
  }
  if (isRecBrowser('edge')) {
    xxReplaceMsg +=
      'Microsoft Edge ' + browserVersionTrim(recBrowserVersions.edge) + ', ';
  }
  if (isRecBrowser('safari')) {
    yyReplaceMsg +=
      'Apple Safari ' + browserVersionTrim(recBrowserVersions.safari);
  }
  displayRecBrowserMsgText = displayRecBrowserMsgText.replace(
    'XX',
    xxReplaceMsg
  );
  displayRecBrowserMsgText = displayRecBrowserMsgText.replace(
    'YY',
    yyReplaceMsg
  );
  return displayRecBrowserMsgText;
}

function addInputChangeCheck(input) {
  setObjAttribute(input, 'onkeypress', 'checkInputKeyDown(this); return true;');
  setObjAttribute(input, 'oncut', 'checkInputKeyDown(this); return true;');
  setObjAttribute(input, 'oncopy', 'checkInputKeyDown(this); return true;');
  setObjAttribute(input, 'onpaste', 'checkInputKeyDown(this); return true;');
}

function setRecommendedBrowserMessage(title, text) {
  var msgContainer = document.getElementById('errorMsg');
  var msgContainerTitle = document.getElementById('errorMsgTitle');
  var msgContainerContent = document.getElementById('errorMsgContent');
  msgContainerTitle.innerHTML = '';
  if (typeof title !== 'undefined' && title !== '' && title !== null) {
    msgContainerContent.innerHTML =
      '<span class="Login_RecBrowserMsg_Title">' +
      title.replace(/\n/g, '<br>').replace(/\\n/g, '<br>') +
      ': ' +
      '</span>';
  } else {
    msgContainerContent.innerHTML = '';
  }
  if (typeof text !== 'undefined' && text !== '' && text !== null) {
    msgContainerContent.innerHTML =
      msgContainerContent.innerHTML +
      '<span class="Login_RecBrowserMsg_Content">' +
      text.replace(/\n/g, '<br>').replace(/\\n/g, '<br>') +
      '</span>';
  }
  msgContainer.style.display = '';
  isRecBrowserMsgShown = true;
}

function resetLoginMessage() {
  var msgContainer = document.getElementById('errorMsg');
  var msgContainerTitle = document.getElementById('errorMsgTitle');
  var msgContainerTitleContainer = document.getElementById(
    'errorMsgTitle_Container'
  );
  var msgContainerContent = document.getElementById('errorMsgContent');
  msgContainerTitle.innerHTML = '';
  msgContainerTitleContainer.style.display = '';
  msgContainerContent.innerHTML = '';
  msgContainer.style.display = 'none';
  isRecBrowserMsgShown = false;
}

function checkInputKeyDown(input, valueLength) {
  var msgContainer = document.getElementById('errorMsg');
  if (msgContainer.style.display !== 'none' && typeof input === 'object') {
    if (typeof valueLength === 'undefined' || valueLength === null) {
      valueLength = input.value.length;
      setTimeout(function() {
        checkInputKeyDown(input, valueLength);
      }, 100);
    } else {
      if (valueLength !== input.value.length && !isRecBrowserMsgShown) {
        resetLoginMessage();
      }
    }
  }
  return true;
}

function beforeLoadDo() {
  redirectWhenPopup();
  redirectWhenInsideMDI();
}

function onLoadDo() {

  const params = new URLSearchParams(window.location.hash.substring(1));
  const accessToken = params.get("access_token");

  if (accessToken) {
    var xhr = new XMLHttpRequest();
    xhr.open("POST", "../../google/org.openbravo.client.kernel", true);
    xhr.setRequestHeader("Authorization", "Bearer " + accessToken);
    xhr.setRequestHeader("Content-Type", "application/json");

    xhr.onreadystatechange = function () {
      if (xhr.readyState === 4) {
        if (xhr.status >= 200 && xhr.status < 300) {
          console.log("Success:", xhr.responseText);
        } else {
          console.error("HTTP error! Status:", xhr.status);
        }
      }
    };

    xhr.send();
  }

  var msgContainerTitle = document.getElementById('errorMsgTitle');
  var msgContainerTitleContainer = document.getElementById(
    'errorMsgTitle_Container'
  );
  var msgContainerContent = document.getElementById('errorMsgContent');

  if (msgContainerTitle.innerHTML.length === 0) {
    msgContainerTitleContainer.style.display = 'none';
  }
  try {
    // To avoid in a release upgrade, that a change in code depending on these functions cause revisionControl message not being displayed
    addInputChangeCheck(document.getElementById('user'));
    addInputChangeCheck(document.getElementById('password'));
    this.windowTables = new Array(new windowTableId('client', 'buttonOK'));
    setWindowTableParentElement();
    enableEditionShortcuts();
    setWindowElementFocus('user', 'id');
  } catch (e) {}

  if (
    !revisionControl(currentRevision) ||
    isOpsInstance() != isOpsInstanceCached()
  ) {
    maskLoginWindow(cacheMsg);
    setLoginMessage('Warning', '', cacheMsg);
  }

  if (!checkBrowserCompatibility()) {
    var displayValidBrowserMsg = buildValidBrowserMsg();
    setLoginMessage('Warning', '', displayValidBrowserMsg);
  }
  if (
    !checkRecommendedBrowser() &&
    msgContainerTitle.innerHTML.length === 0 &&
    msgContainerContent.innerHTML.length === 0
  ) {
    var displayRecBrowserMsgText = buildRecBrowserMsgText();
    setRecommendedBrowserMessage(recBrowserMsgTitle, displayRecBrowserMsgText);
  }

  if (expirationMessage) {
    setLoginMessage(
      expirationMessage.type,
      expirationMessage.title,
      expirationMessage.text
    );
    if (expirationMessage.disableLogin) {
      disableButton('buttonOK');
      document.frmIdentificacion.user.disabled = true;
      document.frmIdentificacion.password.disabled = true;
    }
  }
}

/**
 * Functions used to define keyboard operation shortcuts.
 */

function enableEditionShortcuts() {
  try {
    getEditionShortcuts();
    enableDefaultAction();
  } catch (e) {}
  keyDownManagement();
  keyUpManagement();
}

function getEditionShortcuts() {
  this.keyArray = [
    new keyArrayItem(
      'TAB',
      'windowTabKey(true);',
      null,
      null,
      false,
      'onkeydown'
    ),
    new keyArrayItem(
      'TAB',
      'windowTabKey(false);',
      null,
      null,
      false,
      'onkeyup'
    ),
    new keyArrayItem(
      'TAB',
      'windowShiftTabKey(true);',
      null,
      'shiftKey',
      false,
      'onkeydown'
    ),
    new keyArrayItem(
      'TAB',
      'windowShiftTabKey(false);',
      null,
      'shiftKey',
      false,
      'onkeyup'
    ),
    new keyArrayItem(
      'ENTER',
      'windowCtrlShiftEnterKey();',
      null,
      'ctrlKey+shiftKey',
      false,
      'onkeydown'
    ),
    new keyArrayItem(
      'ENTER',
      'windowCtrlEnterKey();',
      null,
      'ctrlKey',
      true,
      'onkeydown'
    ),
    new keyArrayItem(
      'ENTER',
      'windowEnterKey();',
      null,
      null,
      true,
      'onkeydown'
    )
  ];
}

/**
 * Functions used to handle the state of UI elements, like the login button.
 */

function disableButton(id) {
  var link = null;
  try {
    link = document.getElementById(id);
    if (
      link.className.indexOf('ButtonLink') != -1 &&
      link.className.indexOf('ButtonLink_disabled') == -1
    ) {
      link.className = link.className.replace(
        'ButtonLink_default',
        'ButtonLink'
      );
      link.className = link.className.replace('ButtonLink_focus', 'ButtonLink');
      link.className = link.className.replace(
        'ButtonLink',
        'ButtonLink_disabled'
      );
      link.setAttribute('id', link.getAttribute('id') + '_disabled');
      link.disabled = true;
      disableAttributeWithFunction(link, 'obj', 'onclick');
    }
  } catch (e) {
    return false;
  }
  return true;
}

function enableButton(id) {
  var link = null;
  try {
    link = document.getElementById(id + '_disabled');
    if (link.className.indexOf('ButtonLink_disabled') != -1) {
      link.className = link.className.replace(
        'ButtonLink_disabled',
        'ButtonLink'
      );
      link.setAttribute('id', link.getAttribute('id').replace('_disabled', ''));
      link.disabled = false;
      enableAttributeWithFunction(link, 'obj', 'onclick');
    }
  } catch (e) {
    return false;
  }
  activateDefaultAction();
  return true;
}

function disableAttributeWithFunction(element, type, attribute) {
  var obj;
  if (type == 'obj') {
    obj = element;
  }
  if (type == 'id') {
    obj = document.getElementById(element);
  }
  var attribute_text = getObjAttribute(obj, attribute);
  attribute_text = 'return true; tmp_water_mark; ' + attribute_text;
  setObjAttribute(obj, attribute, attribute_text);
}

function enableAttributeWithFunction(element, type, attribute) {
  var obj;
  if (type == 'obj') {
    obj = element;
  }
  if (type == 'id') {
    obj = document.getElementById(element);
  }
  var attribute_text = getObjAttribute(obj, attribute);
  attribute_text = attribute_text.replace('return true; tmp_water_mark; ', '');
  setObjAttribute(obj, attribute, attribute_text);
}

/**
 * Functions for asynchronous ajax calls. Used to submit the login request.
 */

function submitXmlHttpRequest(
  callbackFunction,
  formObject,
  Command,
  Action,
  debug,
  extraParams,
  paramXMLReq
) {
  var XMLHttpRequestObj = null;
  XMLHttpRequestObj = getXMLHttpRequest();
  if (formObject === null) {
    formObject = document.forms[0];
  }
  if (debug === null) {
    debug = false;
  }
  if (Action === null) {
    Action = formObject.action;
  }
  if (!XMLHttpRequestObj) {
    alert("Your browser doesn't support this technology");
    return false;
  }
  var sendText = 'Command=' + encodeURIComponent(Command);
  sendText += '&IsAjaxCall=1';
  var length = formObject.elements.length;
  for (var i = 0; i < length; i++) {
    if (formObject.elements[i].type) {
      var text = inputValueForms(
        formObject.elements[i].name,
        formObject.elements[i]
      );
      if (text && text.indexOf('=') !== 0) {
        sendText += '&' + text;
      }
    }
  }
  if (extraParams !== null && extraParams !== '' && extraParams !== 'null') {
    sendText += extraParams;
  }

  if (debug) {
    if (!debugXmlHttpRequest(Command)) {
      return false;
    }
  }
  XMLHttpRequestObj.open('POST', Action);
  try {
    XMLHttpRequestObj.setRequestHeader(
      'Content-Type',
      'application/x-www-form-urlencoded'
    );
  } catch (e) {}
  var paramXMLParticular = paramXMLReq;
  XMLHttpRequestObj.onreadystatechange = function() {
    return callbackFunction(paramXMLParticular, XMLHttpRequestObj);
  };
  XMLHttpRequestObj.send(sendText);

  return true;
}

function getXMLHttpRequest() {
  // Create XMLHttpRequest object in non-Microsoft browsers
  var XMLHttpRequestObj = null;

  try {
    XMLHttpRequestObj = new XMLHttpRequest();
  } catch (e) {
    XMLHttpRequestObj = false;
  }

  if (window.ActiveXObject) {
    try {
      // Try to create XMLHttpRequest in later versions
      // of Internet Explorer
      XMLHttpRequestObj = new ActiveXObject('Msxml2.XMLHTTP');
    } catch (e1) {
      // Failed to create required ActiveXObject
      try {
        // Try version supported by older versions
        // of Internet Explorer
        XMLHttpRequestObj = new ActiveXObject('Microsoft.XMLHTTP');
      } catch (e2) {
        // Unable to create an XMLHttpRequest by any means
        XMLHttpRequestObj = false;
      }
    }
  }
  return XMLHttpRequestObj;
}

function getReadyStateHandler(req, responseXmlHandler, notifyError) {
  if (req === null) {
    return false;
  }
  if (notifyError === null || typeof notifyError === 'undefined') {
    notifyError = true;
  }
  // If the request's status is "complete"
  if (req.readyState == 4) {
    // Check that we received a successful response from the server
    if (req.status == 200) {
      // Pass the XML payload of the response to the handler function.
      //responseXmlHandler(req.responseXML);
      return true;
    } else {
      // An HTTP problem has occurred
      if (notifyError) {
        alert('HTTP error ' + req.status + ': ' + req.statusText);
      }
      return false;
    }
  }
  return false;
}
