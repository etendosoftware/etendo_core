/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2014-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.APRM.MatchStatement = {};

OB.APRM.MatchStatement.addPreference = function(view) {
  var onLoadCallback;
  onLoadCallback = function(response, data, request) {};
  OB.RemoteCallManager.call(
    'org.openbravo.advpaymentmngt.actionHandler.MatchStatementOnLoadPreferenceActionHandler',
    {},
    {},
    onLoadCallback
  );
};

OB.APRM.MatchStatement.onLoad = function(view) {
  var execute,
    grid = view.theForm.getItem('match_statement').canvas.viewGrid,
    buttons = view.popupButtons.members[0].members,
    i,
    button,
    propertyButtonValue = '_buttonValue';
  view.cancelButton.hide();
  view.parentElement.parentElement.closeButton.hide();

  for (i = 0; i < buttons.length; i++) {
    button = buttons[i];
    if (button[propertyButtonValue] === 'UN') {
      view.unmatchButton = button;
      button.hide();
      break;
    }
  }

  button.action = function() {
    var callback = function(response, data, request) {
      view.onRefreshFunction(view);
      if (data && data.message && data.message.severity === 'error') {
        view.messageBar.setMessage(
          isc.OBMessageBar.TYPE_ERROR,
          data.message.title,
          data.message.text
        );
      } else if (data && data.message && data.message.severity === 'success') {
        view.messageBar.setMessage(
          isc.OBMessageBar.TYPE_SUCCESS,
          data.message.title,
          data.message.text
        );
      } else if (data && data.message && data.message.severity === 'warning') {
        view.messageBar.setMessage(
          isc.OBMessageBar.TYPE_WARNING,
          data.message.title,
          data.message.text
        );
      }
    };
    OB.RemoteCallManager.call(
      'org.openbravo.advpaymentmngt.actionHandler.UnMatchSelectedTransactionsActionHandler',
      {
        bankStatementLineIds: grid.getSelectedRecords()
      },
      {},
      callback
    );
  };

  grid.dataSourceOrig = grid.dataSource;
  grid.dataSource = null;
  execute = function(ok) {
    var onLoadCallback,
      params = {};
    if (grid.view.sourceView) {
      params.context = grid.view.sourceView.getContextInfo();
    }
    params.executeMatching = ok;
    onLoadCallback = function(response, data, request) {
      if (data.responseActions) {
        OB.Utilities.Action.executeJSON(data.responseActions, null, null, view);
      }
      grid.dataSource = grid.dataSourceOrig;
      grid.filterByEditor();
    };
    OB.RemoteCallManager.call(
      'org.openbravo.advpaymentmngt.actionHandler.MatchStatementOnLoadActionHandler',
      {},
      params,
      onLoadCallback
    );
  };
  if (grid && grid.parentElement && grid.parentElement.messageBar) {
    var onLoadCallback;
    onLoadCallback = function(response, data, request) {
      if (!data.preference) {
        grid.parentElement.messageBar.setMessage(
          isc.OBMessageBar.TYPE_INFO,
          '<div><div class="' +
            OB.Styles.MessageBar.leftMsgContainerStyle +
            '">' +
            OB.I18N.getLabel('APRM_GRID_PERSIST_MESSAGE') +
            '</div><div class="' +
            OB.Styles.MessageBar.rightMsgContainerStyle +
            '"><a href="#" class="' +
            OB.Styles.MessageBar.rightMsgTextStyle +
            '" onclick="' +
            "window['" +
            grid.parentElement.messageBar.ID +
            '\'].hide(); OB.APRM.MatchStatement.addPreference();">' +
            OB.I18N.getLabel('OBUIAPP_NeverShowMessageAgain') +
            '</a></div></div>',
          ' '
        );
      }
    };
    OB.RemoteCallManager.call(
      'org.openbravo.advpaymentmngt.actionHandler.MatchStatementOnLoadGetPreferenceActionHandler',
      {},
      {},
      onLoadCallback
    );
  }
  isc.confirm(OB.I18N.getLabel('APRM_AlgorithmConfirm'), execute);
};

OB.APRM.MatchStatement.onRefresh = function(view) {
  var grid = view.theForm.getItem('match_statement').canvas.viewGrid,
    newCriteria = {};
  newCriteria.criteria = [];
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  grid.invalidateCache();
  view.theForm.redraw();
};

OB.APRM.MatchStatement.onProcess = function(
  view,
  actionHandlerCall,
  clientSideValidationFail
) {
  actionHandlerCall();
};

OB.APRM.MatchStatement.selectionChanged = function(
  grid,
  changedRecord,
  recordList
) {
  if (changedRecord.obSelected && changedRecord.cleared) {
    grid.view.unmatchButton.show();
    return;
  } else {
    var i,
      record,
      selection = grid.getSelectedRecords() || [],
      len = selection.length;
    for (i = 0; i < len; i++) {
      record = grid.getEditedRecord(grid.getRecordIndex(selection[i]));
      if (record && record.obSelected && record.cleared) {
        grid.view.unmatchButton.show();
        return;
      }
    }
  }

  grid.view.unmatchButton.hide();
};

isc.ClassFactory.defineClass('APRMMatchStatGridButtonsComponent', isc.HLayout);

isc.APRMMatchStatGridButtonsComponent.addProperties({
  canExpandRecord: true,

  click: function() {
    this.grid.selectSingleRecord(this.record);
    return this.Super('click', arguments);
  },

  initWidget: function() {
    this.view = this.grid.view;
    var me = this,
      searchButton,
      addButton,
      clearButton,
      buttonSeparator1,
      buttonSeparator2;

    searchButton = isc.OBGridToolStripIcon.create({
      buttonType: 'search',
      showDisabled: true,
      originalPrompt: OB.I18N.getLabel('APRM_MATCHTRANSACTION_SEARCH_BUTTON'),
      prompt: OB.I18N.getLabel('APRM_MATCHTRANSACTION_SEARCH_BUTTON'),
      action: function() {
        var processId = '154CB4F9274A479CB38A285E16984539',
          grid = me.grid,
          record = me.record,
          standardWindow = grid.view.parentWindow.view.standardWindow,
          callback,
          bankStatementLineId = me.record.id,
          updated = new Date(),
          view = me.grid.view;
        updated.setTime(me.record.bslUpdated.getTime());
        callback = function(response, data, request) {
          view.onRefreshFunction(view);
          if (data && data.message && data.message.severity === 'error') {
            view.messageBar.setMessage(
              isc.OBMessageBar.TYPE_ERROR,
              data.message.title,
              data.message.text
            );
          } else {
            standardWindow.openProcess({
              callerField: me,
              paramWindow: true,
              processId: processId,
              windowId: grid.view.windowId,
              externalParams: {
                bankStatementLineId: record.id,
                transactionDate: record.transactionDate
              },
              windowTitle: OB.I18N.getLabel(
                'APRM_MATCHTRANSACTION_SEARCH_BUTTON',
                [this.title]
              )
            });
          }
        };
        OB.RemoteCallManager.call(
          'org.openbravo.advpaymentmngt.actionHandler.CheckRecordChangedActionHandler',
          {
            bankStatementLineId: bankStatementLineId,
            updated: updated
          },
          {},
          callback
        );
      }
    });
    // Disable searchButton button if record is linked to a transaction
    // and update Unmatch All button
    searchButton.setDisabled(me.record.cleared);
    OB.APRM.MatchStatement.selectionChanged(me.grid, me.record);

    addButton = isc.OBGridToolStripIcon.create({
      buttonType: 'add',
      showDisabled: true,
      originalPrompt: OB.I18N.getLabel('APRM_MATCHTRANSACTION_ADD_BUTTON'),
      prompt: OB.I18N.getLabel('APRM_MATCHTRANSACTION_ADD_BUTTON'),
      action: function() {
        var processId = 'E68790A7B65F4D45AB35E2BAE34C1F39',
          grid = me.grid,
          standardWindow = grid.view.parentWindow.view.standardWindow,
          callback,
          bankStatementLineId = me.record.id,
          updated = new Date(),
          view = me.grid.view;
        updated.setTime(me.record.bslUpdated.getTime());
        callback = function(response, data, request) {
          view.onRefreshFunction(view);
          if (data && data.message && data.message.severity === 'error') {
            view.messageBar.setMessage(
              isc.OBMessageBar.TYPE_ERROR,
              data.message.title,
              data.message.text
            );
          } else {
            standardWindow.openProcess({
              callerField: me,
              paramWindow: true,
              processId: processId,
              windowId: grid.view.windowId,
              externalParams: {
                bankStatementLineId: me.record.id
              },
              windowTitle: OB.I18N.getLabel(
                'APRM_MATCHTRANSACTION_ADD_BUTTON',
                [this.title]
              )
            });
          }
        };
        OB.RemoteCallManager.call(
          'org.openbravo.advpaymentmngt.actionHandler.CheckRecordChangedActionHandler',
          {
            bankStatementLineId: bankStatementLineId,
            updated: updated
          },
          {},
          callback
        );
      }
    });
    // Disable addButton button if record is linked to a transaction
    // and update Unmatch All button
    addButton.setDisabled(me.record.cleared);
    OB.APRM.MatchStatement.selectionChanged(me.grid, me.record);

    clearButton = isc.OBGridToolStripIcon.create({
      buttonType: 'clearRight',
      showDisabled: true,
      originalPrompt: OB.I18N.getLabel('APRM_MATCHTRANSACTION_DELETE_BUTTON'),
      prompt: OB.I18N.getLabel('APRM_MATCHTRANSACTION_DELETE_BUTTON'),
      action: function() {
        var callback,
          bankStatementLineId = me.record.id,
          updated = new Date(),
          view = me.grid.view;
        updated.setTime(me.record.bslUpdated.getTime());
        callback = function(response, data, request) {
          view.onRefreshFunction(view);
          if (data && data.message && data.message.severity === 'error') {
            view.messageBar.setMessage(
              isc.OBMessageBar.TYPE_ERROR,
              data.message.title,
              data.message.text
            );
          }
        };
        OB.RemoteCallManager.call(
          'org.openbravo.advpaymentmngt.actionHandler.UnMatchTransactionActionHandler',
          {
            bankStatementLineId: bankStatementLineId,
            updated: updated
          },
          {},
          callback
        );
      }
    });

    buttonSeparator1 = isc.OBGridToolStripSeparator.create({});
    buttonSeparator2 = isc.OBGridToolStripSeparator.create({});

    // Disable clear button if record is not linked to a transaction
    // and update Unmatch All button
    clearButton.setDisabled(!me.record.cleared);
    OB.APRM.MatchStatement.selectionChanged(me.grid, me.record);

    this.addMembers([
      searchButton,
      buttonSeparator1,
      addButton,
      buttonSeparator2,
      clearButton
    ]);
    this.Super('initWidget', arguments);
  }
});

isc.APRMMatchStatGridButtonsComponent.addProperties({
  cellAlign: 'center',

  height: 21,
  width: '100%',
  overflow: 'hidden',
  align: 'center',
  defaultLayoutAlign: 'center',
  styleName: 'OBGridToolStrip',
  layoutLeftMargin: -2,
  layoutRightMargin: 0,
  membersMargin: 4
});
