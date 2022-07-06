// Popup definition

isc.defineClass('EAPM_ParameterPopup', isc.OBPopup);

function createFieldsFromProcessParameters(parametersArray) {
    var arr = [];
    if (Array.isArray(parametersArray) && parametersArray.length) {
        parametersArray.forEach((param, i) => {
            var paramType = 'OBCheckboxItem';
            var defaultValue = (param.defaultCheck.toUpperCase() == 'Y') ? true : false;

            if (param.inputType.toUpperCase() == "TEXT") {
                paramType = 'OBTextItem';
                defaultValue = param.defaultText;
            }

            arr.push({
                name: param.name,
                title: param.name,
                defaultValue: defaultValue,
                height: 20,
                width: 120,
                type: paramType,
                paramId: param.id,
                inputType: param.inputType
            })
        })
    }
    return arr;
}

function parseFieldsToArray(fieldsArr) {
    var arr = [];
    if (Array.isArray(fieldsArr) && fieldsArr.length) {
        // array exists and is not empty
        fieldsArr.forEach((field, i) => {
            arr.push({
                id : field.paramId,
                name: field.name,
                inputType: field.inputType,
                value: field.getValue()
            })
        })
    }
    return arr;
}

isc.EAPM_ParameterPopup.addProperties({
  width: 320,
  height: 200,
  title: null,
  showMinimizeButton: false,
  showMaximizeButton: false,

  view: null,
  params: null,
  actionHandler: null,

  mainform: null,
  okButton: null,
  cancelButton: null,

  initWidget: function () {

    this.title = this.params.executionProcessName;
    var fieldsParameters = createFieldsFromProcessParameters(this.params.processParameters);

    // Form that contains the parameters
    this.mainform = isc.DynamicForm.create({
      width: "100%",
      numCols: 2,
      fields: fieldsParameters
    });

    // OK Button
    this.okButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel("EAPM_PopupTitleOkButton"),
      popup: this,
      action: function () {
        var callback = function (rpcResponse, data, rpcRequest) {
          var paramObj = rpcRequest.clientContext.popup.params;
          var view = paramObj._processView
          var refreshGridAction = [{"refreshGrid":{}},{"refreshGridParameter":{"gridName":"payment_pick"}}]

          if (!data || data == null) {
            paramObj.msgType = "error"
            paramObj.msgText = "Error executing the process"
            OB.Utilities.Action.execute("showMsgInProcessView", paramObj);
            return;
          }

          if (data.success) {
            paramObj.msgType = "success"
            paramObj.msgText = data.message
            OB.Utilities.Action.executeJSON(refreshGridAction, null, null, view);
            OB.Utilities.Action.execute("showMsgInProcessView", paramObj);
          } else {
            paramObj.msgType = "error"
            paramObj.msgText = data.error
            OB.Utilities.Action.execute("showMsgInProcessView", paramObj);
          }

          // close process to refresh current view
          rpcRequest.clientContext.popup.closeClick();
        };

        OB.RemoteCallManager.call(this.popup.actionHandler, {
          _params: this.popup.params,
          processParameters : parseFieldsToArray(this.popup.mainform.getFields())
        }, {}, callback, {popup: this.popup});
      }
    });

   // Cancel Button
   this.cancelButton = isc.OBFormButton.create({
     title: OB.I18N.getLabel("EAPM_PopupTitleCancelButton"),
     popup: this,
     action: function () {
       this.popup.closeClick();
     }
   });

   //Add the elements into a layout
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
         }),
         isc.HLayout.create({
           defaultLayoutAlign: "center",
           align: "center",
           membersMargin: 10,
           members: [this.okButton, this.cancelButton]
         })
       ]
     })
   ];

   this.Super('initWidget', arguments);
  }

});

OB.Utilities.Action.set('EAPM_Popup', function (paramObj) {
    console.log("ACTION EAPM_Popup" + paramObj);

    // Create the PopUp
    isc.EAPM_ParameterPopup.create({
      params: paramObj,
      actionHandler: 'com.etendoerp.advpaymentmngt.actionHandler.PaymentExecutionProcessActionHandler'
    }).show();

});