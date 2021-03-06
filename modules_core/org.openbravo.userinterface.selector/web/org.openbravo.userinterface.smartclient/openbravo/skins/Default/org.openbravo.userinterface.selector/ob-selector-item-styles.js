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
 * All portions are Copyright (C) 2011-2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
isc.OBSelectorPopupWindow.addProperties({
  autoSize: false,
  width: '85%',
  height: '85%',
  align: 'center',
  autoCenter: true,
  isModal: true,
  showModalMask: true,
  animateMinimize: false,
  showMaximizeButton: true,
  headerControls: ['headerIcon', 'headerLabel', 'minimizeButton', 'maximizeButton', 'closeButton'],
  //  headerIconProperties: {
  //    width: 16,
  //    height: 16,
  //    src: 'OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/search_picker.png'
  //  },
  buttonBarHeight: 40,
  buttonBarSpace: 20,
  buttonBarStyleName: null,

  selectorGridProperties: {
    width: '100%',
    height: '100%',
    alternateRecordStyles: true
  }
});

isc.OBSelectorItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultComboBox));

isc.OBSelectorItem.addProperties({
  newTabIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/ico-to-new-tab.png',
  newTabIconSize: 12,

  popupIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/search_picker.png',
  popupIconWidth: 21,
  popupIconHeight: 21,
  popupIconHspace: 0,

  addIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/add_icon.png',
  addIconWidth: 21,
  addIconHeight: 21,
  addIconHspace: 0
});

isc.OBSelectorLinkItem.addProperties({
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldInput',
  newTabIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/ico-to-new-tab.png',
  newTabIconSize: 12,
  pickerIconHeight: 21,
  pickerIconWidth: 21,
  height: 21,
  pickerIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/search_picker.png',
  showPickerIcon: true,
  clearIcon: {
    showRollOver: true,
    showDown: true,
    height: 21,
    width: 21,
    hspace: 0,
    src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/clearField.png',
    prompt: OB.I18N.getLabel('OBUIAPP_ClearIconPrompt')
  }
});

isc.OBMultiSelectorItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultComboBox));
isc.OBMultiSelectorItem.addProperties({

  comboBoxProperties: OB.Styles.OBFormField.DefaultComboBox,
  requiredStyle: 'OBFormFieldInputRequired',

  popupIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/search_picker.png',
  popupIconWidth: 21,
  popupIconHeight: 21,
  popupIconHspace: 0,
  buttonDefaults: {
    iconOrientation: 'left',
    align: 'left',
    icon: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/clearField.png'
  },
  initStyle: function () {
    if (this.required) {
      this.selectionLayout.setStyleName(this.requiredStyle);
    }

    //To adapt the height this code is used because height: '*' doesn't work properly (conflicts with OBSectionItem).
    var rowSpan = 3;
    var singleRowHeight = this.getHeight();
    var multipleRowHeight = singleRowHeight + 24; // 24px = title height + form item padding defined in CSS
    if (this.rowSpan) {
      rowSpan = this.rowSpan;
    }
    var newHeight = singleRowHeight + (rowSpan - 1) * multipleRowHeight;
    this.setHeight(newHeight);
  }
});

isc.OBMultiSelectorItemLabel.addProperties({
  styleName: 'OBMultiSelectorItemLabel'
});

isc.OBMultiSelectorSelectorLayout.addProperties({
  styleName: 'OBFormFieldInput'
});