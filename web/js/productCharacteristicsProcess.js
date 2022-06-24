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
 * All portions are Copyright (C) 2013-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB = OB || {};

OB.ProductCharacteristics = {
  execute: function (params, view) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
        productId, messageBar = view.getView(params.adTabId).messageBar,
        callback, validationMessage, validationOK = true;

    callback = function (rpcResponse, data, rpcRequest) {
      // When the product invariant characteristic data is received, the popup can be created
      this.view = view;
      isc.UpdateInvariantCharacteristicsPopup.create({
        productId: data.productId,
        characteristicList: data.productCharList,
        actionHandler: 'org.openbravo.client.application.event.UpdateInvariantCharacteristicsHandler',
        view: view
      }).show();
    };

    // Retrieves the productId and sends it to the handler to obtain the product invariant characteristic data
    productId = selection[0].id;
    OB.RemoteCallManager.call(params.actionHandler, {
      productId: productId,
      action: params.action
    }, {}, callback);
  },

  updateInvariants: function (params, view) {
    params.actionHandler = 'org.openbravo.client.application.event.UpdateInvariantCharacteristicsHandler';
    params.adTabId = view.activeView.tabId;
    params.processId = 'A832A5DA28FB4BB391BDE883E928DFC5';
    params.action = 'INITIALIZE';
    OB.ProductCharacteristics.execute(params, view);
  },

  //Shows a message in order to notify that number of records is too high, so
  //product characteristics should be changed.
  testOnGridLoad: function (grid) {
    var nRecordsReceived = grid.getData().getLength(),
        messageBar = grid.view.messageBar;
    if (nRecordsReceived == 0) {
      messageBar.setMessage(isc.OBMessageBar.TYPE_INFO, OB.I18N.getLabel('NoVariants'));
    }
  },
};

isc.defineClass('UpdateInvariantCharacteristicsPopup', isc.OBPopup);

// This popup will show a combo for each of the product invariant
// characteristics, and OK and Cancel buttons.
isc.UpdateInvariantCharacteristicsPopup.addProperties({
  width: 400,
  height: 300,
  title: 'Update Characteristics',
  canDragResize: false,
  showMinimizeButton: false,
  showMaximizeButton: false,
  productId: null,
  actionHandler: null,
  characteristicCombos: null,

  okButton: null,
  cancelButton: null,

  initWidget: function () {
    OB.TestRegistry.register('org.openbravo.client.application.UpdateInvariantCharacteristics.popup', this);
    var originalView = this.view,
        params = this.params,
        i, emptyFunction, initTreeField;

    emptyFunction = function () {};
    initTreeField = function () {
      this.optionDataSource = OB.Datasource.create({
        createClassName: '',
        dataURL: '/openbravo/org.openbravo.service.datasource/CharacteristicValue',
        requestProperties: {
          params: {
            targetProperty: this.name,
            adTabId: 'A43B5CFC17754AED86B9E57F71460C4E',
            IsSelectorItem: 'true',
            Constants_FIELDSEPARATOR: '$',
            columnName: this.name,
            Constants_IDENTIFIER: '_identifier',
            _extraProperties: 'id,name'
          }
        },
        fields: [{
          name: 'id',
          type: '_id_13',
          primaryKey: true
        }, {
          name: 'client',
          type: '_id_19'
        }, {
          name: 'client$_identifier'
        }, {
          name: 'organization',
          type: '_id_19'
        }, {
          name: 'organization$_identifier'
        }, {
          name: 'active',
          type: '_id_20'
        }, {
          name: 'creationDate',
          type: '_id_16'
        }, {
          name: 'createdBy',
          type: '_id_30'
        }, {
          name: 'createdBy$_identifier'
        }, {
          name: 'updated',
          type: '_id_16'
        }, {
          name: 'updatedBy',
          type: '_id_30'
        }, {
          name: 'updatedBy$_identifier'
        }, {
          name: 'characteristic',
          type: '_id_19'
        }, {
          name: 'characteristic$_identifier'
        }, {
          name: 'name',
          type: '_id_10'
        }, {
          name: 'summaryLevel',
          type: '_id_20'
        }, {
          name: 'code',
          type: '_id_10'
        }, {
          name: 'description',
          type: '_id_10'
        }, {
          name: 'addProducts',
          type: '_id_28'
        }, {
          name: 'id',
          type: '_id_13',
          additional: true,
          primaryKey: true
        }, {
          name: 'name',
          type: '_id_10',
          additional: true
        }]
      });

      this.Super('init', arguments);

      this.tree.originalShow = this.tree.show;
      this.tree.treeItem.addParamsToRequest = function () {
        return {
          parentCharId: this.parentCharId,
          productCharSubsetId: this.productCharSubsetId
        }
      };
      // It will not be possible to open the dropdown until there exists a dataSource
      this.tree.show = function (explicitCriteria) {
        if (this.dataSource) {
          this.originalShow(explicitCriteria);
          this.bringToFront();
        }
      };
    };

    // Populates the combos using the provided characteristicList
    this.characteristicCombos = [];
    for (i = 0; i < this.characteristicList.length; i++) {
      this.characteristicCombos[i] = isc.DynamicForm.create({
        view: originalView.view,
        handleItemChange: emptyFunction,
        fields: [{
          id: this.characteristicList[i].id,
          name: this.characteristicList[i].name,
          title: this.characteristicList[i].title,
          valueMap: this.characteristicList[i].valueMap,
          defaultValue: this.characteristicList[i].selectedValue,
          existingProdChValue: this.characteristicList[i].existingProdChValue,
          height: 20,
          width: 255,
          required: false,
          form: {
            view: originalView.view
          },
          columnName: this.characteristicList[i].name,
          inpColumnName: this.characteristicList[i].name,
          refColumnName: 'name',
          targetEntity: 'CharacteristicValue',
          treeReferenceId: '95582A51651D415993D0FD3B64C8E861',
          parentCharId: this.characteristicList[i].id,
          dataSourceId: '90034CAE96E847D78FBEF6D38CB1930D',
          parentSelectionAllowed: true,
          popupTextMatchStyle: 'substring',
          textMatchStyle: 'substring',
          defaultPopupFilterField: 'name',
          displayField: 'name',
          valueField: 'id',
          referencedTableId: 'E913D17C9B3847CF92235082DBE2EC44',
          productCharSubsetId: this.characteristicList[i].productCharSubsetId,
          showTreePopupWindow: false,
          pickListFields: [{
            title: ' ',
            name: 'name',
            type: 'text'
          }],
          showSelectorGrid: true,
          treeGridFields: [{
            title: 'name',
            name: 'name',
            canSort: false,
            type: '_id_10'
          }],
          extraSearchFields: [],
          init: initTreeField,
          gridProps: {
            sort: 3,
            autoExpand: true,
            displayField: 'characteristicValue$name',
            displaylength: 32,
            fkField: true,
            showHover: true
          },
          type: '_id_D3D9A7BAF4594950922A22B2D7ABFA74'
        }]
      });
      // TODO: Remove popup icon because currently the popup does not
      // load any data (see issue 28887)
      this.characteristicCombos[i].getField(0).setIcons([]);
    }

    this.okButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OK'),
      popup: this,
      action: function () {
        var callback, action, updatedValues = {},
            i, characteristicCombo, existingProdChValues = {};

        callback = function (rpcResponse, data, rpcRequest) {
          rpcRequest.clientContext.popup.closeClick();
          rpcRequest.clientContext.popup.view.refresh(false, false);
          if (data.message) {
            rpcRequest.clientContext.popup.view.view.messageBar.setMessage(data.message.severity, null, data.message.text);
          }
        };
        // Saves in updatedValues the product invariant values selected by the user 
        for (i = 0; i < this.popup.items[0].members[0].members.length; i++) {
          characteristicCombo = this.popup.items[0].members[0].members[i];
          updatedValues[characteristicCombo.fields[0].id] = characteristicCombo.values[characteristicCombo.fields[0].name];
          existingProdChValues[characteristicCombo.fields[0].id] = characteristicCombo.fields[0].existingProdChValue;
        }
        // Send the updated values to the handler so that the product invariant selected values are updated
        OB.RemoteCallManager.call(this.popup.actionHandler, {
          productId: this.popup.productId,
          updatedValues: updatedValues,
          existingProdChValues: existingProdChValues,
          action: 'UPDATE'
        }, {}, callback, {
          originalView: this.popup.view,
          popup: this.popup
        });
      }
    });

    OB.TestRegistry.register('org.openbravo.client.application.UpdateInvariantCharacteristics.popup.okButton', this.okButton);

    this.cancelButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('Cancel'),
      popup: this,
      action: function () {
        this.popup.closeClick();
      }
    });

    this.items = [
    isc.VLayout.create({
      defaultLayoutAlign: 'center',
      align: 'center',
      width: '100%',
      height: 300,
      overflow: 'auto',
      layoutMargin: 10,
      membersMargin: 6,
      members: [
      isc.VLayout.create({
        defaultLayoutAlign: 'right',
        align: 'right',
        layoutMargin: 30,
        membersMargin: 6,
        members: this.characteristicCombos
      }), isc.HLayout.create({
        defaultLayoutAlign: 'center',
        align: 'center',
        membersMargin: 10,
        members: [this.okButton, this.cancelButton]
      })]
    })];
    this.Super('initWidget', arguments);
  }

});