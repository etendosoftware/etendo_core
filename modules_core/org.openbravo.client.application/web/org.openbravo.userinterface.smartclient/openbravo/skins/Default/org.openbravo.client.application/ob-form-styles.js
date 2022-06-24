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
 * All portions are Copyright (C) 2010-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

/* =====================================================================
 * Styling properties for:
 * 1) OB Form items
 * 2) OBImageItem
 =======================================================================*/

/* =====================================================================
 * FormItem styling properties
 =======================================================================*/
isc.OBViewForm.addProperties({
  styleName: 'OBViewForm',
  width: '100%',
  overflow: 'visible',
  //cellBorder: 1, // debug layout
  cellPadding: 0
});

isc.OBParameterWindowForm.addProperties({
  styleName: 'OBParameterWindowForm',
  width: '100%',
  overflow: 'visible',
  //  cellBorder: 1, // debug layout
  cellPadding: 0
});

isc.OBFormContainerLayout.addProperties({
  styleName: 'OBFormContainerLayout'
});

isc.OBFormButton.addProperties({
  baseStyle: 'OBFormButton',
  titleStyle: 'OBFormButtonTitle',
  height: 32,
  autoFit: true
});

OB.Styles.OBFormField = {};
OB.Styles.OBFormField.DefaultTextItem = {
  errorOrientation: 'left',
  height: 21,
  width: '100%',
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldInput'
};

isc.OBTextItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultTextItem));

isc.OBFKFilterTextItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultTextItem));

isc.OBEncryptedItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultTextItem));

isc.OBClientClassCanvasItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultTextItem));

isc.OBTextAreaItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultTextItem));
isc.OBTextAreaItem.addProperties({
  initStyle: function () {
    //To adapt the height this code is used because height: '*' doesn't work properly (conflicts with OBSectionItem).
    var rowSpan = 1;
    var singleRowHeight = this.getHeight();
    var multipleRowHeight = singleRowHeight + 24; // 24px = title height + form item padding defined in CSS
    if (this.rowSpan) {
      rowSpan = this.rowSpan;
    }
    var newHeight = singleRowHeight + (rowSpan - 1) * multipleRowHeight;
    this.setHeight(newHeight);
  }
});

isc.OBRichTextItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultTextItem));

isc.OBRichTextItem.addProperties({
  styleCanvasProperties: {
    styleName: 'OBFormFieldInput',
    editorStyleName: 'OBFormFieldInput',
    editAreaStyleName: 'OBFormFieldStatic',
    editAreaBackgroundColor: 'transparent'
  }
});

isc.OBRichTextItem.addProperties({
  initStyle: function () {
    var rowSpan = 4;
    var singleRowHeight = this.height;
    var multipleRowHeight = singleRowHeight + 24; // 24px = title height + form item padding defined in CSS
    this.canvasProperties = isc.addProperties({}, this.canvasProperties || {}, this.styleCanvasProperties);

    if (this.rowSpan) {
      rowSpan = this.rowSpan;
    }
    var newHeight = singleRowHeight + (rowSpan - 1) * multipleRowHeight;
    if (!this.colSpan) {
      this.colSpan = 2;
    }
    this.height = newHeight;
    this.overflow = "hidden";
    this.showEdges = true;
    // Standard control group options include
    // "fontControls", "formatControls", "styleControls" and "colorControls"
    this.controlGroups = ["fontControls", "styleControls"];

  }
});

isc.OBSpinnerItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultTextItem));
isc.OBSpinnerItem.INCREASE_ICON = isc.addProperties(isc.OBSpinnerItem.INCREASE_ICON, {
  width: 21,
  height: 11,
  showRollOver: true,
  showFocused: false,
  showDown: true,
  imgOnly: true,
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/spinnerControlIncrease.png'
});
isc.OBSpinnerItem.DECREASE_ICON = isc.addProperties(isc.OBSpinnerItem.DECREASE_ICON, {
  width: 21,
  height: 10,
  showRollOver: true,
  showFocused: false,
  showDown: true,
  imgOnly: true,
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/spinnerControlDecrease.png'
});

isc.OBPopUpTextAreaItem.addProperties({
  errorOrientation: 'left',
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldStatic'
});

OB.Styles.OBFormField.DefaultComboBox = {
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldSelectInput',
  pendingTextBoxStyle: null,
  //'OBFormFieldSelectInputPending',
  controlStyle: 'OBFormFieldSelectControl',
  pickerIconStyle: 'OBFormFieldSelectPickerIcon',
  pickListBaseStyle: 'OBFormFieldPickListCell',
  // tallbasestyle is used when the cellheight is different
  // from the standard
  pickListTallBaseStyle: 'OBFormFieldPickListCell',
  pickerIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/comboBoxPicker.png',
  height: 21,
  pickerIconWidth: 21,
  pickerIconHeight: 21,

  // note the menu-rollover.png which is the background for selected rows
  // is 20
  pickListCellHeight: 22,

  quickRunWidth: 267,
  // fixes issue https://issues.openbravo.com/view.php?id=15105
  quickRunPickListCellHeight: 22,
  pickListHeight: 200,
  autoSizePickList: true,

  pickListProperties: {
    showShadow: false,
    shadowDepth: 5,
    bodyStyleName: 'OBPickListBody'
  },

  errorOrientation: 'left'
};

isc.OBListItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultComboBox));

isc.OBFKItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultComboBox));

isc.OBFKItem.addProperties({
  newTabIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/ico-to-new-tab.png',
  newTabIconSize: 12
});

isc.OBYesNoItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultComboBox));

OB.Styles.OBFormField.DefaultCheckbox = {
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldLabel',
  showValueIconOver: true,
  showValueIconFocused: true,
  showFocused: true,
  defaultValue: false,
  checkedImage: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/checked.png',
  uncheckedImage: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/unchecked.png'
};

isc.OBCheckboxItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultCheckbox));

OB.Styles.OBFormField.DefaultSearch = {
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  // Hack to solve issues 25942 and 26190
  textBoxStyle: 'OBFormFieldInput_HeightHack OBFormFieldInput',
  pickerIconHeight: 21,
  pickerIconWidth: 21,
  height: 21,
  // align the button directly to the field
  pickerIconHspace: 0,
  pickerIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/search_picker.png',
  clearIcon: {
    showRollOver: true,
    showDown: true,
    height: 21,
    width: 21,
    hspace: 0,
    src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/clearField.png',
    prompt: OB.I18N.getLabel('OBUIAPP_ClearIconPrompt')
  },
  newTabIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/ico-to-new-tab.png',
  newTabIconSize: 12
};

isc.OBSearchItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultSearch));

isc.OBLinkItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultSearch));

isc.OBLinkButtonItem.addProperties({
  width: 1,
  //To allow button be just text width
  align: 'left',
  baseStyle: 'OBLinkButtonItem',
  showDown: true,
  showFocused: true,
  showFocusedAsOver: false,
  showRollOver: true,
  autoFit: true,
  height: 1,
  overflow: 'visible'
});

isc.OBDateChooser.addProperties({
  className: 'OBDateChooser',
  headerStyle: 'OBDateChooserButton',
  weekendHeaderStyle: 'OBDateChooserWeekendButton',
  baseNavButtonStyle: 'OBDateChooserNavButton',
  baseWeekdayStyle: 'OBDateChooserWeekday',
  baseWeekendStyle: 'OBDateChooserWeekend',
  baseBottomButtonStyle: 'OBDateChooserBottomButton',
  disabledWeekdayStyle: "OBDateChooserWeekdayDisabled",
  disabledWeekendStyle: "OBDateChooserWeekendDisabled",

  alternateWeekStyles: false,
  firstDayOfWeek: 1,

  showEdges: true,

  edgeImage: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/blank.gif',
  edgeSize: 3,
  edgeTop: 26,
  edgeBottom: 3,
  edgeOffsetTop: 1,
  edgeOffsetRight: 3,
  edgeOffsetLeft: 3,
  edgeOffsetBottom: 5,

  todayButtonHeight: 20,

  headerHeight: 24,

  edgeCenterBackgroundColor: '#E5E5E5',
  backgroundColor: null,

  showShadow: false,
  shadowDepth: 6,
  shadowOffset: 5,

  showDoubleYearIcon: false,
  prevYearIcon: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/dateChooser-doubleArrow_left.png',
  prevYearIconWidth: 16,
  prevYearIconHeight: 16,
  nextYearIcon: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/dateChooser-doubleArrow_right.png',
  nextYearIconWidth: 16,
  nextYearIconHeight: 16,
  prevMonthIcon: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/dateChooser-arrow_left.png',
  prevMonthIconWidth: 16,
  prevMonthIconHeight: 16,
  nextMonthIcon: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/dateChooser-arrow_right.png',
  nextMonthIconWidth: 16,
  nextMonthIconHeight: 16
});

OB.Styles.OBFormField.DefaultDateInput = {
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldDateInput',
  errorOrientation: 'left',

  pickerIconHSpace: '0',

  height: 21,

  pickerIconWidth: 21,
  pickerIconHeight: 21,
  pickerIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/date_control.png'
};

isc.OBDateItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultDateInput));
isc.OBDateItem.addProperties({
  textAlign: 'left',
  pickerIconPrompt: OB.I18N.getLabel('OBUIAPP_pickerIconPrompt')
});

isc.OBDateTimeItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultDateInput));

isc.OBTimeItem.addProperties(isc.addProperties({}, OB.Styles.OBFormField.DefaultTextItem));

isc.OBTimeItem.addProperties({
  height: 25
});

isc.OBTimeItem.addProperties({
  // Hack to solve issue: https://issues.openbravo.com/view.php?id=25916
  textBoxStyle: 'OBFormFieldInput_HeightHack OBFormFieldInput'
});

isc.OBTimeItemGrid.addProperties({
  baseStyle: 'OBFormFieldPickListCell',
  bodyStyleName: 'OBPickListBody',
  className: 'scrollingMenu',
  characterWidth: 6,
  height: 178
});

isc.OBNumberItem.addProperties({
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldNumberInput',
  errorOrientation: 'left'
});

isc.OBTreeItemTree.addProperties({
  baseStyle: 'OBFormFieldPickListCell',
  bodyStyleName: 'OBPickListBody',
  className: 'scrollingMenu',
  cellHeight: 22
});

/* =====================================================================
 * Date range filter item and dialog
 =======================================================================*/

isc.OBDateRangeDialog.addProperties({
  // rounded frame edges
  showEdges: true,
  edgeImage: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/blank.gif',
  customEdges: null,
  edgeSize: 2,
  edgeTop: 27,
  edgeBottom: 2,
  edgeOffsetTop: 2,
  edgeOffsetRight: 2,
  edgeOffsetBottom: 2,
  showHeaderBackground: false,
  // part of edges
  showHeaderIcon: true,
  isModal: true,
  showModalMask: true,
  dragAppearance: 'target',

  // clear backgroundColor and style since corners are rounded
  backgroundColor: null,
  border: null,
  styleName: 'OBPopup',
  edgeCenterBackgroundColor: '#FFFFFF',
  bodyColor: 'transparent',
  bodyStyle: 'OBPopupBody',
  headerStyle: 'OBPopupHeader',

  layoutMargin: 0,
  membersMargin: 0,

  showShadow: false,
  shadowDepth: 5,
  width: 420,
  height: 170
});

isc.OBDateRangeDialog.changeDefaults('headerDefaults', {
  layoutMargin: 0,
  height: 25
});

isc.OBDateRangeDialog.changeDefaults('headerLabelDefaults', {
  wrap: false,
  width: '100%',
  inherentWidth: true,
  styleName: 'OBPopupHeaderText',
  align: isc.Canvas.CENTER
});

isc.OBDateRangeDialog.changeDefaults('buttonLayoutDefaults', {
  align: 'center'
});

isc.OBDateRangeDialog.changeDefaults('closeButtonDefaults', {
  baseStyle: 'OBPopupIconClose',
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/popup/close.png',
  width: 24,
  height: 20
});

isc.OBDateRangeDialog.changeDefaults('headerIconProperties', {
  styleName: 'OBPopupHeaderIcon',
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/popup/iconHeader.png',
  width: 20,
  height: 16
});

isc.OBDateRangeDialog.addProperties({
  clearButtonConstructor: isc.OBFormButton,
  cancelButtonConstructor: isc.OBFormButton,
  okButtonConstructor: isc.OBFormButton,
  okButtonTitle: OB.I18N.getLabel('OBUISC_Dialog.OK_BUTTON_TITLE'),
  clearButtonTitle: OB.I18N.getLabel('OBUIAPP_Clear'),
  cancelButtonTitle: OB.I18N.getLabel('OBUISC_Dialog.CANCEL_BUTTON_TITLE'),
  headerTitle: OB.I18N.getLabel('OBUIAPP_SelectDateRange')
});

isc.OBMiniDateRangeItem.changeDefaults('pickerIconDefaults', {
  width: 21,
  height: 21,
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/date_control.png'
});

isc.OBMiniDateRangeItem.addProperties({
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldInput',
  showFocused: true,
  fromDateOnlyPrefix: OB.I18N.getLabel('OBUIAPP_fromDateOnlyPrefix'),
  toDateOnlyPrefix: OB.I18N.getLabel('OBUIAPP_toDateOnlyPrefix'),
  pickerIconPrompt: OB.I18N.getLabel('OBUIAPP_pickerIconPrompt')
});

isc.OBDateRangeItem.changeDefaults('dateRangeFormDefaults', {
  titleSuffix: '</b>',
  titlePrefix: '<b>',
  requiredTitleSuffix: ' *</b>',
  requiredRightTitlePrefix: '<b>* ',
  rightTitlePrefix: '<b>',
  rightTitleSuffix: '</b>'
});

isc.OBDateRangeItem.addProperties({
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldInput',
  titleOrientation: 'left',
  fromTitle: OB.I18N.getLabel('OBUIAPP_From'),
  toTitle: OB.I18N.getLabel('OBUIAPP_To')
});

isc.OBRelativeDateItem.addProperties({
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldSelectInput',
  controlStyle: 'OBFormFieldSelectControl'
});

isc.OBRelativeDateItem.changeDefaults('quantityFieldDefaults', {
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldSelectInput',
  controlStyle: 'OBFormFieldSelectControl',
  width: 65
});

isc.OBRelativeDateItem.changeDefaults('valueFieldDefaults', {
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldSelectInput',
  textBoxStyleNormal: 'OBFormFieldSelectInput',
  textBoxStyleError: 'OBFormFieldSelectInputError',
  controlStyle: 'OBFormFieldSelectControl',
  pickerIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/comboBoxPicker.png',
  pickerIconWidth: 21,
  pickerIconHeight: 21,
  calendarIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/date_control.png',
  calendarIconWidth: 21,
  calendarIconHeight: 21,
  calendarIconHspace: 0
});

isc.OBRelativeDateItem.changeDefaults('calculatedDateFieldDefaults', {
  canFocus: false,
  disabled: true,
  showDisabled: false,
  width: 85
});

/* =====================================================================
 * OBImageItem
 =======================================================================*/

isc.OBImageItem.addProperties({
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel'
});

isc.OBImageCanvas.addProperties({
  height: '0px',
  // Hack to avoid second line be desplaced in Firefox and IE
  zoomInCursorSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/system/zoomIn.cur',
  zoomOutCursorSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/system/zoomOut.cur',
  imageNotAvailableSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/imageNotAvailable.png',
  imageNotAvailableHeight: 49,
  imageNotAvailableWidth: 57
});

isc.OBImageItemSmallImageContainer.addProperties({
  styleName: 'OBFormFieldImageInput',
  height: 21,
  width: '100%',
  align: 'center',
  defaultLayoutAlign: 'center',
  initWidgetStyle: function () {
    //To adapt the height this code is used because the automatic height doesn't fill empty rows.
    var rowSpan = 1;
    var singleRowHeight = this.getHeight();
    var multipleRowHeight = singleRowHeight + 24; // 24px = title height + form item padding defined in CSS
    if (this.imageItem.rowSpan) {
      rowSpan = this.imageItem.rowSpan;
    }
    var newHeight = singleRowHeight + (rowSpan - 1) * multipleRowHeight;
    this.setHeight(newHeight);
  }
});

isc.OBImageItemSmallImage.addProperties({
  styleName: 'OBImageItemSmallImage',
  imageType: 'stretch'
});

isc.OBImageItemBigImage.addProperties({
  styleName: 'OBImageItemBigImage',
  zoomInCursorSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/system/zoomIn.cur',
  zoomOutCursorSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/system/zoomOut.cur',
  align: 'center'
});

isc.OBImageItemButton.addProperties({
  showRollOver: true,
  showDisabled: true,
  showDown: true,
  showFocusedAsOver: true,
  width: 21,
  height: 21,
  uploadIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/upload_icon.png',
  eraseIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/erase_icon.png',
  initWidgetStyle: function () {
    if (this.buttonType === 'upload') {
      this.setSrc(this.uploadIconSrc);
    } else if (this.buttonType === 'erase') {
      this.setSrc(this.eraseIconSrc);
    }
  }
});

/* =====================================================================
 * SectionItem Button Styles
 =======================================================================*/

isc.OBSectionItem.addProperties({
  sectionHeaderClass: 'OBSectionItemButton',
  height: 45
});

isc.ClassFactory.defineClass('OBSectionItemButton', isc.ImgSectionHeader);
isc.OBSectionItemButton.changeDefaults('backgroundDefaults', {
  showRollOver: true,
  showDown: false,
  showDisabled: false,
  showDisabledIcon: false,
  showRollOverIcon: false,
  src: null, //OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/blank.gif',
  icon: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/sectionItem-ico.png',
  iconSize: 12,
  capSize: 12,
  titleStyle: 'OBSectionItemButton_Title_',
  backgroundColor: 'transparent'
});
isc.OBSectionItemButton.addProperties({
  focusChanged: function () {
    /*
    // "ImgSectionHeader" is not a StatefulCanvas so -Focused- status should be done programmatically
    if (this.background) {
      if (this.containsFocus()) {
        this.background.setSrc(this.background.src.replace(/(\.)(png)$/, '_Focused.png'));
        this.background.setIcon(this.background.icon.replace(/(\.)(png)$/, '_Focused.png'));
      } else {
        this.background.setSrc(this.background.src.replace(/(_Focused)(\.)(png)$/, '.png'));
        this.background.setIcon(this.background.icon.replace(/(_Focused)(\.)(png)$/, '.png'));
      }
    }
    */
    this.Super("focusChanged", arguments);
  }
});

/* =====================================================================
  * Image Popup Styles
  =======================================================================*/

isc.OBImageSelector.addProperties({
  hlayoutTopMargin: 10,
  hlayoutBottomMargin: 10,
  height: 50,
  width: 450,
  align: 'center'
});

/* =====================================================================
 * Grid FormItem Styles
 =======================================================================*/

isc.OBPickEditGridItem.addProperties({
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel'
});