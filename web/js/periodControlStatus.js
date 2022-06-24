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
 * All portions are Copyright (C) 2013 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/
isc.defineClass('PeriodControlStatus_Field', isc.Label);

isc.PeriodControlStatus_Field.addProperties({
  height: 1,
  width: 100,
  initWidget: function () {
    if (this.record && this.record.periodStatus) {
      this.createField(this.record.periodStatus);
    }
    this.Super('initWidget', arguments);
  },

  createField: function (periodStatus) {
    var backGroundColor = '#C0C0C0',
        text, align = 'center';
    if (periodStatus === 'O') {
      backGroundColor = '#00FF00';
      text = '';
    } else if (periodStatus === 'C') {
      backGroundColor = '#FF0000';
      text = '';
    } else if (periodStatus === 'P') {
      backGroundColor = '#FF0000';
      text = '';
	} else if (periodStatus === 'N') {
	  backGroundColor = '#C2C2A3';
	  text = '';
	} else {
      text = 'periodControlStatus not supported';
    }
    this.setBackgroundColor(backGroundColor);
    this.setAlign(align);
  }
});

isc.defineClass('PeriodStatus_Field', isc.Label);

isc.PeriodStatus_Field.addProperties({
  height: 1,
  width: 100,
  initWidget: function () {
    if (this.record && this.record.status) {
      this.createField(this.record.status);
    }
    this.Super('initWidget', arguments);
  },

  createField: function (status) {
    var backGroundColor = '#C0C0C0',
        text, align = 'center';
    if (status === 'O') {
      backGroundColor = '#00FF00';
      text = '';
    } else if (status === 'C') {
      backGroundColor = '#FF0000';
      text = '';
    } else if (status === 'P') {
        backGroundColor = '#FF0000';
        text = '';
    } else if (status === 'M') {
      backGroundColor = '#FFA319';
      text = '';
	} else if (status === 'N') {
	  backGroundColor = '#C2C2A3';
	  text = '';
	} else {
      text = 'periodStatus not supported';
    }
    this.setBackgroundColor(backGroundColor);
    this.setAlign(align);
  }
});

OB = OB || {};

OB.OpenClose = {
  execute: function (params, view) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
        recordIdList = [],
        messageBar = view.getView(params.adTabId).messageBar,
        callback, validationMessage, validationOK = true;

    callback = function (rpcResponse, data, rpcRequest) {
      var status = rpcResponse.status,
          view = rpcRequest.clientContext.view.getView(params.adTabId);
      view.messageBar.setMessage(data.message.severity, null, data.message.text);

      // close process to refresh current view
      params.button.closeProcessPopup();
    };

    for (i = 0; i < selection.length; i++) {
      recordIdList.push(selection[i].id);
    }

     isc.OpenClosePeriodProcessPopup.create({
       recordIdList: recordIdList,
       view: view,
       params: params
     }).show();
  },

  openClose: function (params, view) {
    params.actionHandler = 'org.openbravo.client.application.event.OpenClosePeriodHandler';
    params.adTabId = view.activeView.tabId;
    params.processId = 'A832A5DA28FB4BB391BDE883E928DFC5';
    OB.OpenClose.execute(params, view);
  }

};

isc.defineClass('OpenClosePeriodProcessPopup', isc.OBPopup);

isc.OpenClosePeriodProcessPopup.addProperties({

  width: 320,
  height: 200,
  title: null,
  showMinimizeButton: false,
  showMaximizeButton: false,

  //Form
  mainform: null,

  //Button
  okButton: null,
  cancelButton: null,

  getActionList: function (form) {
    var send = {
      recordIdList: this.recordIdList
    },
        actionField, popup = this;
    send.action = 'ACTION_COMBO';
    OB.RemoteCallManager.call('org.openbravo.client.application.event.OpenClosePeriodHandler', send, {}, function (response, data, request) {
      if (response) {
        actionField = form.getField('Action');
        if (response.data) {
          popup.setTitle('Process Request');
          actionField.closePeriodStepId = response.data.nextStepId;
          actionField.setValueMap(response.data.actionComboBox.valueMap);
          actionField.setDefaultValue(response.data.actionComboBox.defaultValue);
        }
      }
    });
  },

  initWidget: function () {

  OB.TestRegistry.register('org.openbravo.client.application.OpenClosePeriod.popup', this);

    var recordIdList = this.recordIdList,
        originalView = this.view,
        params = this.params;

    this.mainform = isc.DynamicForm.create({
      numCols: 2,
      colWidths: ['50%', '50%'],
      fields: [{
        name: 'Action',
        title: OB.I18N.getLabel('Action'),
        height: 20,
        width: 255,
        required: true,
        type: '_id_17',
        closePeriodStepId: null,
        defaultToFirstOption: true
      }]
    });

    this.okButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OK'),
      popup: this,
      action: function () {
        var callback, action;

        callback = function (rpcResponse, data, rpcRequest) {
          var status = rpcResponse.status,
              view = rpcRequest.clientContext.originalView.getView(params.adTabId);
          if (data.message) {
            view.messageBar.setMessage(data.message.severity, null, data.message.text);
          }

          rpcRequest.clientContext.popup.closeClick();
          rpcRequest.clientContext.originalView.refresh(false, false);
        };

        action = this.popup.mainform.getItem('Action').getValue();

        OB.RemoteCallManager.call(params.actionHandler, {
          closePeriodStepId: this.popup.mainform.getItem('Action').closePeriodStepId,
          recordIdList:recordIdList,
          action: action
        }, {}, callback, {
          originalView: this.popup.view,
          popup: this.popup
        });
      }
    });
    
    OB.TestRegistry.register('org.openbravo.client.application.OpenClosePeriod.popup.okButton', this.okButton);

    this.cancelButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('Cancel'),
      popup: this,
      action: function () {
        this.popup.closeClick();
      }
    });

    this.getActionList(this.mainform);

    this.items = [
    isc.VLayout.create({
      defaultLayoutAlign: "center",
      align: "center",
      width: "100%",
      layoutMargin: 10,
      membersMargin: 6,
      members: [
      isc.HLayout.create({
        defaultLayoutAlign: "center",
        align: "center",
        layoutMargin: 30,
        membersMargin: 6,
        members: this.mainform
      }), isc.HLayout.create({
        defaultLayoutAlign: "center",
        align: "center",
        membersMargin: 10,
        members: [this.okButton, this.cancelButton]
      })]
    })];

    this.Super('initWidget', arguments);
  }

});
