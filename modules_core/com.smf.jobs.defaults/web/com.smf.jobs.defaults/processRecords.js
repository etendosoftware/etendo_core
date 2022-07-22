OB.Jobs = OB.Jobs || {};
OB.Jobs.ProcessInvoices = OB.Jobs.ProcessInvoices || {};
OB.Jobs.ProcessOrders = OB.Jobs.ProcessOrders || {};
OB.Jobs.ProcessShipment = OB.Jobs.ProcessShipment || {};

OB.Jobs.ProcessInvoices.onLoad = function(view) {
    var form = view.theForm;
    var docActionField = form.getItem('DocAction');
    var voidDateField = form.getItem('VoidDate');
    var voidAcctDateField = form.getItem('VoidAccountingDate');
    var selectedRecords = view.parentWindow.view.viewGrid.getSelectedRecords();
    var isProcessing = "";
    var tableId = "318";
    var documentStatuses = [];
    var documentActions = [];

    for (i = 0; i < selectedRecords.length; i++) {
      var record = selectedRecords[i]
      if (!documentStatuses.includes(record.documentStatus)) {
        documentStatuses.add(record.documentStatus);
      }
      if (!documentActions.includes(record.documentAction)) {
        documentActions.add(record.documentAction);
      }
    }

    voidDateField.hide();
    voidAcctDateField.hide();


    OB.RemoteCallManager.call(
        'com.smf.jobs.defaults.ProcessInvoicesDefaults',
        {
          documentStatuses: documentStatuses,
          isProcessing: isProcessing,
          tableId: tableId
        },
        {
        },
        function(response, data, request) {
          var actions = data.actions;
          var currentValues = docActionField.getValueMap();
          var newValues = {};

          for (i = 0; i < actions.length; i++) {
            var action = actions[i];
            if (currentValues[action]) {
                newValues[action] = currentValues[action];
            }
          }

          docActionField.setValueMap(newValues);
          if (newValues[documentActions[0]]) {
            docActionField.setValueProgrammatically(documentActions[0]);
          } else {
            // Set the first option if the record has an action that is not available in the list
            docActionField.setValueProgrammatically(docActionField.getFirstOptionValue());
          }
        }
      );

}

OB.Jobs.ProcessInvoices.onChangeDocumentAction = function(item, view, form, grid) {
    var docActionField = form.getItem('DocAction');
    if (view.windowId === '183' && (docActionField.getValue() === 'VO' || docActionField.getValue() === 'RC')) {
        form.getItem('VoidDate').show();
        form.getItem('VoidAccountingDate').show();
    }

};

OB.Jobs.ProcessOrders.onLoad = function(view) {
    var form = view.theForm;
    var docActionField = form.getItem('DocAction');
    var selectedRecords = view.parentWindow.view.viewGrid.getSelectedRecords();
    var tabId = view.processOwnerView.tabId || view.parentWindow.tabId;
    var isProcessing = "";
    var documentStatuses = [];
    var documentActions = [];

    for (i = 0; i < selectedRecords.length; i++) {
      var record = selectedRecords[i]
      if (!documentStatuses.includes(record.documentStatus)) {
        documentStatuses.add(record.documentStatus);
      }
      if (!documentActions.includes(record.documentAction)) {
        documentActions.add(record.documentAction);
      }
    }

    OB.RemoteCallManager.call(
        'com.smf.jobs.defaults.ProcessOrdersDefaults',
        {
          documentStatuses: documentStatuses,
          isProcessing: isProcessing,
          tabId: tabId
        },
        {
        },
        function(response, data, request) {
          var actions = data.actions;
          var currentValues = docActionField.getValueMap();
          var newValues = {};

          for (i = 0; i < actions.length; i++) {
            var action = actions[i];
            if (currentValues[action]) {
                newValues[action] = currentValues[action];
            }
          }

          docActionField.setValueMap(newValues);
          // Avoid setting an action that is on the record(s) but not in the dropdown list
          if (newValues[documentActions[0]]) {
            docActionField.setValueProgrammatically(documentActions[0]);
          } else {
            // Set the first option if the record has an action that is not available in the list
            docActionField.setValueProgrammatically(docActionField.getFirstOptionValue());
          }

          // If OK Button is not enabled but there are actions to select, enable it manually
          if (actions.length > 0 && view.okButton && !view.okButton.isEnabled()) {
            view.okButton.enable();
          }
        }
      );
}

OB.Jobs.ProcessShipment.onLoad = function(view) {
    var form = view.theForm;
    var docActionField = form.getItem('DocAction');
    var selectedRecords = view.parentWindow.view.viewGrid.getSelectedRecords();
    var isProcessing = "";
    var tableId = "319";
    var documentStatuses = [];
    var documentActions = [];

    for (i = 0; i < selectedRecords.length; i++) {
      var record = selectedRecords[i]
      if (!documentStatuses.includes(record.documentStatus)) {
        documentStatuses.add(record.documentStatus);
      }
    }

    OB.RemoteCallManager.call(
        'com.smf.jobs.defaults.ProcessShipmentDefaults',
        {
          documentStatuses: documentStatuses,
          isProcessing: isProcessing,
          tableId: tableId
        },
        {
        },
        function(response, data, request) {
            var actions = data.actions;
            var currentValues = docActionField.getValueMap();
            var newValues = {};

            for (i = 0; i < actions.length; i++) {
                var action = actions[i];
                if (currentValues[action]) {
                    newValues[action] = currentValues[action];
                    }
            }

          docActionField.setValueMap(newValues);
          // Avoid setting an action that is on the record(s) but not in the dropdown list
          if (newValues[documentActions[0]]) {
            docActionField.setValueProgrammatically(documentActions[0]);
          } else {
            // Set the first option if the record has an action that is not available in the list
            docActionField.setValueProgrammatically(docActionField.getFirstOptionValue());
          }

          // If OK Button is not enabled but there are actions to select, enable it manually
          if (actions.length > 0 && view.okButton && !view.okButton.isEnabled()) {
            view.okButton.enable();
          }
        }
    );
}

OB.Utilities.Action.set('smartclientSay', function(paramObj) {
    isc.say(paramObj.message);
});
