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
 * All portions are Copyright (C) 2012 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */


if (!isc.OBClassicWindow) {
  isc.ClassFactory.defineClass('OBClassicWindow', isc.Window);
}

isc.OBClassicWindow.addProperties({
  // rounded frame edges
  showEdges: true,
  edgeImage: OB.Styles.skinsPath + '250to300Comp/org.openbravo.userinterface.selector/window/window.png',
  customEdges: null,
  edgeSize: 6,
  edgeTop: 23,
  edgeBottom: 6,
  edgeOffsetTop: 2,
  edgeOffsetRight: 5,
  edgeOffsetBottom: 5,
  minimizeHeight: 29,
  showHeaderBackground: false,
  // part of edges
  showHeaderIcon: true,

  // clear backgroundColor and style since corners are rounded
  backgroundColor: null,
  border: null,
  styleName: 'normal',
  edgeCenterBackgroundColor: '#FFFFFF',
  bodyColor: 'transparent',
  bodyStyle: 'ob_windowBody',

  layoutMargin: 0,
  membersMargin: 0,

  showFooter: false,

  showShadow: false,
  shadowDepth: 5
});

isc.OBClassicWindow.changeDefaults('headerDefaults', {
  layoutMargin: 0,
  height: 20
});
isc.OBClassicWindow.changeDefaults('resizerDefaults', {
  src: '[SKIN]/Window/resizer.png'
});

isc.OBClassicWindow.changeDefaults('headerIconDefaults', {
  width: 15,
  height: 15,
  src: '[SKIN]/Window/headerIcon.png'
});
isc.OBClassicWindow.changeDefaults('restoreButtonDefaults', {
  src: OB.Styles.skinsPath + '250to300Comp/org.openbravo.userinterface.selector/window/restore.png',
  showRollOver: true,
  showDown: false,
  width: 15,
  height: 15
});
isc.OBClassicWindow.changeDefaults('closeButtonDefaults', {
  src: OB.Styles.skinsPath + '250to300Comp/org.openbravo.userinterface.selector/window/close.png',
  showRollOver: true,
  showDown: false,
  width: 15,
  height: 15
});
isc.OBClassicWindow.changeDefaults('maximizeButtonDefaults', {
  src: OB.Styles.skinsPath + '250to300Comp/org.openbravo.userinterface.selector/window/maximize.png',
  showRollOver: true,
  width: 15,
  height: 15
});
isc.OBClassicWindow.changeDefaults('minimizeButtonDefaults', {
  src: OB.Styles.skinsPath + '250to300Comp/org.openbravo.userinterface.selector/window/minimize.png',
  showRollOver: true,
  showDown: false,
  width: 15,
  height: 15
});
isc.OBClassicWindow.changeDefaults('toolbarDefaults', {
  buttonConstructor: 'IButton'
});



if (!isc.OBClassicIButton) {
  isc.ClassFactory.defineClass('OBClassicIButton', isc.IButton);
}

isc.OBClassicIButton.addProperties({
  src: OB.Styles.skinsPath + '250to300Comp/org.openbravo.userinterface.selector/btn/btn.png',
  height: 22,
  width: 100,
  capSize: 4,
  vertical: false,
  titleStyle: 'ob_buttonTitle',
  showFocused: true,
  showFocusedAsOver: true
});


if (!isc.OBClassicGrid) {
  isc.ClassFactory.defineClass('OBClassicGrid', isc.ListGrid);
}

isc.OBClassicGrid.addProperties({
  alternateRecordStyles: true,

  editFailedCSSText: 'color:FF6347;',
  errorIconSrc: '[SKINIMG]actions/exclamation.png',
  baseStyle: 'ob_cell',
  tallBaseStyle: 'ob_tallCell',

  headerButtonConstructor: 'Button',
  sorterConstructor: 'ImgButton',

  sortAscendingImage: {
    src: '[SKIN]sort_ascending.png',
    width: 9,
    height: 6
  },
  sortDescendingImage: {
    src: '[SKIN]sort_descending.png',
    width: 9,
    height: 6
  },

  backgroundColor: null,
  bodyBackgroundColor: null,

  headerHeight: 23,
  summaryRowHeight: 21,
  cellHeight: 22,
  normalCellHeight: 22,
  headerBackgroundColor: null,
  headerBaseStyle: 'ob_headerButton',
  bodyStyleName: 'ob_gridBody',
  alternateBodyStyleName: 'ob_alternateGridBody',

  summaryRowStyle: 'ob_gridSummaryCell',
  groupSummaryStyle: 'ob_groupSummaryCell',

  showHeaderMenuButton: true,
  headerMenuButtonConstructor: 'HeaderImgButton',
  headerMenuButtonWidth: 17,
  headerMenuButtonSrc: OB.Styles.skinsPath + '250to300Comp/org.openbravo.userinterface.selector/listgrid/header_menu.png',
  headerMenuButtonIcon: '[SKINIMG]/ListGrid/sort_descending.png',
  headerMenuButtonIconWidth: 9,
  headerMenuButtonIconHeight: 6,

  groupLeadingIndent: 1,
  groupIconPadding: 3,
  groupIcon: '[SKINIMG]/ListGrid/group.png',

  expansionFieldTrueImage: '[SKINIMG]/ListGrid/row_expanded.png',
  expansionFieldFalseImage: '[SKINIMG]/ListGrid/row_collapsed.png',
  expansionFieldImageWidth: 16,
  expansionFieldImageHeight: 16,
  checkboxFieldImageWidth: 13,
  checkboxFieldImageHeight: 13
});
isc.OBClassicGrid.changeDefaults('sorterDefaults', {
  // baseStyle / titleStyle is auto-assigned from headerBaseStyle
  showFocused: false,
  src: OB.Styles.skinsPath + '250to300Comp/org.openbravo.userinterface.selector/listgrid/header.png',
  baseStyle: 'ob_sorterButton'
});
isc.OBClassicGrid.changeDefaults('headerButtonDefaults', {
  showRollOver: true,
  showDown: false,
  showFocused: false,
  baseStyle: 'ob_headerButton'
});
isc.OBClassicGrid.changeDefaults('headerMenuButtonDefaults', {
  showDown: false,
  showTitle: true,
  src: OB.Styles.skinsPath + '250to300Comp/org.openbravo.userinterface.selector/listgrid/header.png'
});
isc.OBClassicGrid.changeDefaults('summaryRowDefaults', {
  bodyBackgroundColor: null,
  bodyStyleName: 'ob_summaryRowBody'
});
isc.OBClassicGrid.changeDefaults('filterEditorDefaults', {
  baseStyle: 'ob_recordEditorCell',
  styleName: 'ob_recordEditorCell'
});

isc.ComboBoxItem.addProperties({
  showFocusedPickerIcon: true,
  pickerIconSrc: OB.Styles.skinsPath + '250to300Comp/org.openbravo.userinterface.selector/control/cBoxPicker.png',
  height: 17,
  pickerIconWidth: 18
});