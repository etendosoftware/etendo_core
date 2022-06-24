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
 * All portions are Copyright (C) 2012-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// ** {{{ custom }}} **
// It executes a custom action. Only for testing purposes.
// Parameters:
// * {{{func}}}: The function to be executed. It can be defined in several ways. Examples:
//    OB.Utilities.Action.execute('custom', { func: "alert('Test')" });
//    OB.Utilities.Action.execute('custom', { func: "(function (){alert('Test')})()" });
//    OB.Utilities.Action.execute('custom', { func: function() { alert('Test'); }});
//    OB.Utilities.Action.execute('custom', { func: function(paramObj) { alert(paramObj.text) }, text: 'Test' });
//    OB.Utilities.Action.executeJSON( { custom: { func: function(paramObj) { alert(paramObj.text) }, text: 'Test' } });
OB.Utilities.Action.set('custom', function(paramObj) {
  if (Object.prototype.toString.apply(paramObj.func) === '[object Function]') {
    paramObj.func(paramObj);
  } else if (
    Object.prototype.toString.apply(paramObj.func) === '[object String]'
  ) {
    var execFunction = new Function(paramObj.func);
    execFunction();
  }
});

// ** {{{ showMsgInView }}} **
// It shows a message in the current active view. In the end, it calls to messageBar.setMessage of the current active view
// Parameters:
// * {{{msgType}}}: The message type. It can be 'success', 'error', 'info' or 'warning'
// * {{{msgTitle}}}: The title of the message.
// * {{{msgText}}}: The text of the message.
OB.Utilities.Action.set('showMsgInView', function(paramObj) {
  var view = OB.MainView.TabSet.getSelectedTab().pane.activeView;
  if (view && view.messageBar) {
    view.messageBar.setMessage(
      paramObj.msgType,
      paramObj.msgTitle,
      paramObj.msgText
    );
  } else {
    // If the window is not loaded, wait and try again
    var i = 0,
      messageInterval;
    messageInterval = setInterval(function() {
      view = OB.MainView.TabSet.getSelectedTab().pane.activeView;
      if (view && view.messageBar) {
        clearInterval(messageInterval);
        view.messageBar.setMessage(
          paramObj.msgType,
          paramObj.msgTitle,
          paramObj.msgText
        );
      } else if (i === 5) {
        clearInterval(messageInterval);
      }
      i++;
    }, 500); //Call this action again with a 500ms delay
  }
});

//** {{{ showMsgInProcessView }}} **
// It shows a message in the view that invoked the process.
//Parameters:
//* {{{msgType}}}: The message type. It can be 'success', 'error', 'info' or 'warning'
//* {{{msgTitle}}}: The title of the message.
//* {{{msgText}}}: The text of the message.
//* {{{force}}}: (Optional) If it should force the message to be show in the popup.
//               Typically it is used in 'error' cases with 'retryExecution' set as 'true'
OB.Utilities.Action.set('showMsgInProcessView', function(paramObj) {
  var processView = paramObj._processView;
  if (processView.messageBar && paramObj.force === true) {
    processView.messageBar.setMessage(
      paramObj.msgType,
      paramObj.msgTitle,
      paramObj.msgText
    );
  } else if (
    processView.callerField &&
    processView.callerField.view &&
    processView.callerField.view.messageBar
  ) {
    // In the case we are inside a process called from another process we want to show the message inside the caller process instead of the main window.
    processView.callerField.view.messageBar.setMessage(
      paramObj.msgType,
      paramObj.msgTitle,
      paramObj.msgText
    );
  } else if (
    processView.popup &&
    processView.buttonOwnerView &&
    processView.buttonOwnerView.messageBar
  ) {
    processView.buttonOwnerView.messageBar.setMessage(
      paramObj.msgType,
      paramObj.msgTitle,
      paramObj.msgText
    );
  } else if (processView.messageBar) {
    processView.messageBar.setMessage(
      paramObj.msgType,
      paramObj.msgTitle,
      paramObj.msgText
    );
  }
});

// ** {{{ openDirectTab }}} **
// Open a view using a tab id and record id. The tab can be a child tab. If the record id
// is not set then the tab is opened in grid mode. If command is not set then default is
// used. In the end, it calls to OB.Utilities.openDirectTab
// Parameters:
// * {{{tabId}}}: The tab id of the view to be opened
// * {{{recordId}}}: The record id of the view to be opened
// * {{{command}}}: The command with which the view to be opened
// * {{{wait}}}: If true, the thread in which this action was called (if there is any) will be paused until the view be opened.
OB.Utilities.Action.set('openDirectTab', function(paramObj) {
  var processIndex,
    tabPosition,
    isTabOpened = false;
  if (!paramObj.newTabPosition) {
    tabPosition = OB.Utilities.getTabNumberById(paramObj.tabId); // Search if the tab has been opened before
    if (tabPosition !== -1) {
      paramObj.newTabPosition = tabPosition;
      isTabOpened = true;
    } else {
      processIndex = OB.Utilities.getProcessTabBarPosition(
        paramObj._processView
      );
      if (processIndex === -1) {
        // If the process is not found in the main tab bar, add the new window in the last position
        paramObj.newTabPosition =
          OB.MainView.TabSet.paneContainer.members.length;
      } else {
        // If the process is found in the main tab bar, add the new window in its next position
        paramObj.newTabPosition = processIndex + 1;
      }
    }
  }
  if (!paramObj.isOpening) {
    OB.Utilities.openDirectTab(
      paramObj.tabId,
      paramObj.recordId,
      paramObj.command,
      paramObj.newTabPosition,
      paramObj.criteria
    );
  }
  if (
    (paramObj.wait === true || paramObj.wait === 'true') &&
    paramObj.threadId
  ) {
    if (
      !OB.MainView.TabSet.getTabObject(paramObj.newTabPosition) ||
      OB.MainView.TabSet.getTabObject(paramObj.newTabPosition).pane
        .isLoadingTab === true ||
      isTabOpened
    ) {
      OB.Utilities.Action.pauseThread(paramObj.threadId);
      paramObj.isOpening = true;
      OB.Utilities.Action.execute('openDirectTab', paramObj, 100); //Call this action again with a 100ms delay
    } else {
      OB.Utilities.Action.resumeThread(paramObj.threadId, 1500); //Call this action again with a 1500ms delay
    }
  }
});

// ** {{{ setSelectorValueFromRecord }}} **
// It sets a given value in the selector caller field (if it exists)
// Parameters:
// * {{{record}}}: The record to be set in the selector
OB.Utilities.Action.set('setSelectorValueFromRecord', function(paramObj) {
  var callerField = paramObj._processView.callerField;
  if (!callerField) {
    return;
  }
  callerField.setValueFromRecord(paramObj.record, true, true);
});

//** {{{ refreshGrid }}} **
//It refreshes the grid where the process button is defined. Only needed if the process adds or deletes records from this tab
OB.Utilities.Action.set('refreshGrid', function(paramObj) {
  var processView = paramObj._processView;
  if (
    processView &&
    processView.buttonOwnerView &&
    processView.buttonOwnerView.viewGrid
  ) {
    processView.buttonOwnerView.viewGrid.refreshGrid();
  }
});

//** {{{ refreshGridParameter }}} **
//It refreshes a grid parameter defined within a parameter window
//Parameters:
//* {{{gridName}}}: The name of the grid parameter
OB.Utilities.Action.set('refreshGridParameter', function(paramObj) {
  var processView = paramObj._processView,
    gridName = paramObj.gridName,
    gridItem;
  if (
    processView &&
    processView.theForm &&
    processView.theForm.getItem &&
    gridName
  ) {
    gridItem = processView.theForm.getItem(gridName);
    if (gridItem && gridItem.canvas && gridItem.canvas.viewGrid) {
      // force parameter grid refresh by invalidating cache
      gridItem.canvas.viewGrid.invalidateCache();
    }
  }
});

//** {{{ OBUIAPP_downloadReport }}} **
//This action is used by the BaseReportActionHandler to download the generated file with the
//report result from the temporary location using the postThroughHiddenForm function. The mode is
//changed to DOWNLOAD so the BaseReportActionHandler executes the logic to download the report.
//Parameters:
//* {{{processParameters}}}: The process parameters is an object that includes the action handler implementing the download, the report id that it is being executed and the process definition id.
//* {{{tmpfileName}}}: Name of the temporary file.
//* {{{fileName}}}: The name to be used in the file to download.
OB.Utilities.Action.set('OBUIAPP_downloadReport', function(paramObj) {
  var processParameters = paramObj.processParameters,
    params = isc.clone(processParameters);
  params._action = processParameters.actionHandler;
  params.reportId = processParameters.reportId;
  params.processId = processParameters.processId;
  params.tmpfileName = paramObj.tmpfileName;
  params.fileName = paramObj.fileName;
  params.mode = 'DOWNLOAD';
  OB.Utilities.postThroughHiddenForm(
    OB.Application.contextUrl + 'org.openbravo.client.kernel',
    params
  );
});

//** {{{ OBUIAPP_browseReport }}} **
//This action is used by the BaseReportActionHandler to show in a new tab the generated file with the
//report result from the temporary location. The mode is changed to BROWSE so the BaseReportActionHandler
//executes the logic to display the report.
//Parameters:
//* {{{processParameters}}}: The process parameters is an object that includes the action handler implementing the browsing, the report id that it is being executed and the process definition id.
//* {{{tmpfileName}}}: Name of the temporary file.
//* {{{fileName}}}: The name to be used in the file to download.
OB.Utilities.Action.set('OBUIAPP_browseReport', function(paramObj) {
  var processParameters = paramObj.processParameters;
  OB.Layout.ViewManager.openView('OBClassicWindow', {
    tabTitle: paramObj.tabTitle,
    addToRecents: false,
    isProcessDefinitionReport: true,
    obManualURL:
      '/org.openbravo.client.kernel?_action=' +
      processParameters.actionHandler +
      '&reportId=' +
      processParameters.reportId +
      '&processId=' +
      processParameters.processId +
      '&tmpfileName=' +
      paramObj.tmpfileName +
      '&fileName=' +
      paramObj.fileName +
      '&mode=BROWSE&vScroll=auto',
    command: 'DEFAULT'
  });
});
