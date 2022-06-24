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
 * All portions are Copyright (C) 2012-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
// = OBMultiSelectorItem =
// OBMultiSelectorItem is a selector that allows selecting multiple records,
// specially intended for be used in parameter windows
isc.ClassFactory.defineClass('OBMultiSelectorItem', isc.CanvasItem);
isc.OBMultiSelectorItem.addProperties({
  rowSpan: 2,
  selectionLayout: null,
  selectorGridFields: [
    {
      title: OB.I18N.getLabel('OBUISC_Identifier'),
      name: OB.Constants.IDENTIFIER
    }
  ],
  // Overwrites CanvasItem standard behaviour to force saving values on DynamicForms
  shouldSaveValue: true,
  init: function() {
    this.icons = [
      {
        selector: this,
        src: this.popupIconSrc,
        width: this.popupIconWidth,
        height: this.popupIconHeight,
        hspace: this.popupIconHspace,
        keyPress: function(keyName, character, form, item, icon) {
          var response = OB.KeyboardManager.Shortcuts.monitor(
            'OBSelectorItem.icon',
            this.selector
          );
          if (response !== false) {
            response = this.Super('keyPress', arguments);
          }
          return response;
        },
        click: function(form, item, icon) {
          item.openSelectorWindow();
        }
      }
    ];

    if (this.selectorGridFields.length === 0) {
      this.showSelectorGrid = true;
      this.selectorGridFields = [
        {
          title: 'Identifier',
          name: OB.Constants.IDENTIFIER,
          type: 'text'
        }
      ];
    }

    if (this.disabled) {
      // TODO: disable, remove icons
      this.icons = null;
    }
    if (!this.showSelectorGrid) {
      this.icons = null;
    }

    if (this.showSelectorGrid && !this.form.isPreviewForm) {
      // adds pin field, which is marked as pin whenever the
      // record is part of the selection
      // only adds it if the first field is not a pin field already
      if (
        this.selectorGridFields.length === 0 ||
        this.selectorGridFields[0].name !== '_pin'
      ) {
        this.selectorGridFields.unshift({
          name: '_pin',
          type: 'boolean',
          title: '&nbsp;',
          escapeHTML: false,
          canEdit: false,
          disableFilter: true,
          canSort: false,
          canReorder: false,
          canHide: false,
          frozen: true,
          canFreeze: false,
          canDragResize: false,
          canGroupBy: false,
          autoExpand: false,
          width: OB.Styles.Process.PickAndExecute.pinColumnWidth,
          formatCellValue: function(value, record, rowNum, colNum, grid) {
            if (
              grid &&
              grid.selector &&
              grid.selector.selectorWindow.selectedIds.contains(
                record[OB.Constants.ID]
              )
            ) {
              return (
                '<img src="' +
                OB.Styles.Process.PickAndExecute.iconPinSrc +
                '" />'
              );
            }
            return '';
          },
          formatEditorValue: function(value, record, rowNum, colNum, grid) {
            return this.formatCellValue(arguments);
          }
        });
      }

      this.selectorWindow = isc.OBSelectorPopupWindow.create({
        // solves issue: https://issues.openbravo.com/view.php?id=17268
        title:
          this.form && this.form.grid
            ? this.form.grid.getField(this.name).title
            : this.title,
        dataSource: this.optionDataSource,
        selector: this,
        valueField: this.valueField,
        displayField: this.displayField,
        selectorGridFields: isc.shallowClone(this.selectorGridFields),
        selectionAppearance: 'checkbox',
        multiselect: true,
        selectedIds: this.getValue(),
        selectId: function(id) {
          if (!this.selectedIds.contains(id)) {
            this.selectedIds.push(id);
          }
        },
        closeClick: function() {
          var i,
            records = [];
          for (i = 0; i < this.origSelection.length; i++) {
            records.push(
              this.selectorGrid.data.find(
                OB.Constants.ID,
                this.origSelection[i]
              )
            );
          }
          this.selector.setSelectedRecords(records);
          this.Super('closeClick', arguments);
        }
      });

      this.selectorWindow.selectorGrid.recordClick = function(
        viewer,
        record,
        recordnum,
        field,
        fieldnum
      ) {
        // If a field other than the checkbox is clicked, select/deselect the record manually
        if (fieldnum !== 0) {
          if (this.isSelected(record)) {
            this.deselectRecord(record);
          } else {
            this.selectRecord(record);
          }
        }
      };

      this.selectorWindow.selectorGrid.recordDoubleClick = function(
        viewer,
        record,
        recordnum,
        field,
        fieldnum
      ) {};

      // overridden to support hover on the header for the checkbox field
      this.selectorWindow.selectorGrid.setFieldProperties = function(
        field,
        properties
      ) {
        var localField = field;
        if (isc.isA.Number(localField)) {
          localField = this.fields[localField];
        }
        if (this.isCheckboxField(localField) && properties) {
          properties.showHover = true;
          properties.prompt = OB.I18N.getLabel(
            'OBUIAPP_GridSelectAllColumnPrompt'
          );
        }

        return this.Super('setFieldProperties', arguments);
      };
    }

    this.optionCriteria = {
      _selectorDefinitionId: this.selectorDefinitionId
    };

    this.canvas = isc.OBMultiSelectorSelectorLayout.create({
      selectorItem: this
    });

    this.Super('init', arguments);

    this.selectionLayout = this.canvas;

    if (this.initStyle) {
      this.initStyle();
    }
  },

  // resets whole selection to the records passed as parameter
  setSelectedRecords: function(records) {
    var i;
    this.storeValue([]);
    this.selectionLayout.removeMembers(this.selectionLayout.getMembers());
    if (records.length === 0) {
      // Also handle form change when no record has been selected
      this.handleSelectorItemsChange();
    }
    for (i = 0; i < records.length; i++) {
      this.setValueFromRecord(records[i]);
    }
  },

  // set value is invoked by isString default validator causing array to be converted into
  // a string, let's recover the array
  setValue: function(value) {
    if (value) {
      if (isc.isA.String(value)) {
        // Reset value, because it's not an array anymore but a string
        this.storeValue([]);
        value = value.split(',');
      }
      this.storeValue(value);
    } else {
      this.Super('setValue', arguments);
    }
  },

  // adds a new record to the selection
  setValueFromRecord: function(record) {
    var me = this,
      selectedElement,
      currentValue = this.getValue();

    // add record to selected values
    currentValue.push(record[OB.Constants.ID]);
    this.storeValue(currentValue);

    // display it in the layout
    selectedElement = isc.OBMultiSelectorItemLabel.create({
      contents: record[OB.Constants.IDENTIFIER],
      icon: me.buttonDefaults.icon,
      height: 1,
      width: '90%',
      // Setting width to reserve some space for vertical scrollbar
      value: record[OB.Constants.ID],
      iconClick: function() {
        var currentValues = me.getValue();
        currentValues.remove(this.value);
        me.selectionLayout.removeMember(this);
        // Refresh form after each item removal, important when there are dependant grids/fields
        me.handleSelectorItemsChange();
      }
    });
    this.selectionLayout.addMember(selectedElement);

    this.handleSelectorItemsChange();

    if (this.form.focusInNextItem && isc.EH.getKeyName() !== 'Tab') {
      this.form.focusInNextItem(this.name);
    }
  },

  // Refreshes form when there is an onChange handle on the form of the selector
  handleSelectorItemsChange: function() {
    if (this.form && this.form.handleItemChange) {
      this._hasChanged = true;
      this.form.handleItemChange(this);
    }
  },

  disable: function() {
    var i;
    this.Super('disable', arguments);
    // Remove the icon that removes a selection
    for (i = 0; i < this.selectionLayout.members.length; i++) {
      this.selectionLayout.members[i].setIcon(null);
    }
  },

  openSelectorWindow: function() {
    // always refresh the content of the grid to force a reload
    // if the organization has changed
    if (this.selectorWindow.selectorGrid) {
      this.selectorWindow.selectorGrid.invalidateCache();
    }
    this.selectorWindow.selectedIds = this.getValue();
    this.selectorWindow.origSelection = isc.shallowClone(
      this.selectorWindow.selectedIds
    );
    this.selectorWindow.open();
  },

  destroy: function() {
    // Explicitly destroy elements avoid memory leaks
    if (this.canvas) {
      this.canvas.destroy();
      this.canvas = null;
    }

    if (this.selectorWindow) {
      this.selectorWindow.destroy();
      this.selectorWindow = null;
    }
    this.Super('destroy', arguments);
  },

  getValue: function() {
    var value = this.Super('getValue', arguments);
    return value ? value : [];
  }
});

// = OBMultiSelectorItemLabel =
// OBMultiSelectorItemLabel is used for selected element label
isc.ClassFactory.defineClass('OBMultiSelectorItemLabel', isc.Label);

// = OBMultiSelectorSelectorLayout =
// Utility layout to display selected records in a OBMultiSelectorItem
isc.ClassFactory.defineClass('OBMultiSelectorSelectorLayout', isc.VStack);

isc.OBMultiSelectorSelectorLayout.addProperties({
  popupTextMatchStyle: 'startswith',
  suggestionTextMatchStyle: 'startswith',
  showOptionsFromDataSource: true,
  autoDraw: false,
  overflow: 'auto',
  members: [],
  animateMembers: true,
  animateMemberTime: 100,
  width: '*',
  initWidget: function() {
    this.Super('initWidget', arguments);
  }
});
