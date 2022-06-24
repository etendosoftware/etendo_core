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
 * All portions are Copyright (C) 2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// == OBColorItem ==
// UI Implementation for Color Selector references
isc.ClassFactory.defineClass('OBColorItem', isc.OBFKComboItem);

isc.OBColorItem.addProperties({
  valueField: 'id',
  displayField: '_identifier',

  pickListFields: [
    {
      title: OB.I18N.getLabel('OBUISC_Identifier'),
      name: '_identifier',
      type: 'text'
    },
    {
      title: OB.I18N.getLabel('ColorLbl'),
      name: 'hexColor',
      type: 'text'
    }
  ],

  pickListProperties: {
    formatCellValue: (value, record, field, viewer) => {
      if (value.startsWith('#')) {
        // Color field
        this.backgroundColor = record.hexColor;
        return `<div style='background-color:${record.hexColor}; height:1.5rem'></div>`;
      }
      return value;
    }
  },

  getDisplayFieldName: function() {
    return '_identifier';
  },

  init: function() {
    this.displayField = '_identifier';
    this.valueField = 'id';
    this.Super('init', arguments);

    this.optionDataSource = OB.Datasource.create({
      createClassName: '',
      dataURL: '/openbravo/org.openbravo.service.datasource/Color',
      requestProperties: {
        params: {
          Constants_IDENTIFIER: '_identifier',
          Constants_FIELDSEPARATOR: '$'
        }
      },
      fields: [
        {
          name: '_identifier',
          escapeHTML: true
        }
      ]
    });
  },

  filterDataBoundPickList: function(requestProperties, dropCache) {
    // This is a workaround to fix displayField and valueField being modified by a previous _filter call
    this.displayField = '_identifier';
    this.valueField = 'id';

    return this.Super('filterDataBoundPickList', [
      requestProperties,
      dropCache
    ]);
  }
});
