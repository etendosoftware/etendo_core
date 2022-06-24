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
 * All portions are Copyright (C) 2014-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.Styles.OBTreeGrid = OB.Styles.OBTreeGrid || {};
OB.Styles.OBTreeGrid.iconFolder = OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/tree-grid/iconFolder.png';
OB.Styles.OBTreeGrid.iconNode = OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/tree-grid/iconNode.png';
OB.Styles.OBTreeGrid.cellCSSText_filterHit = '';
OB.Styles.OBTreeGrid.cellCSSText_notFilterHit = 'color: #C0C0C0;';

isc.OBTreeGrid.addProperties({
  bodyStyleName: 'OBGridBody',
  baseStyle: 'OBTreeGridCell',
  recordStyleError: 'OBGridCellError',
  recordStyleSelectedViewInActive: 'OBGridCellSelectedViewInactive',
  headerBaseStyle: 'OBGridHeaderCell',
  headerBarStyle: 'OBGridHeaderBar',
  headerTitleStyle: 'OBGridHeaderCellTitle',
  emptyMessageStyle: 'OBGridNotificationText',
  emptyMessageLinkStyle: 'OBGridNotificationTextLink',
  errorMessageStyle: 'OBGridNotificationTextError',
  cellPadding: 0,
  /* Set in the CSS */
  cellAlign: 'center',
  leaveHeaderMenuButtonSpace: false,
  sorterConstructor: 'ImgButton',
  sortAscendingImage: {
    src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/grid/gridHeader_sortAscending.png',
    width: 7,
    height: 11
  },
  sortDescendingImage: {
    src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/grid/gridHeader_sortDescending.png',
    width: 7,
    height: 11
  },
  headerMenuButtonConstructor: 'OBGridHeaderImgButton',
  headerButtonConstructor: 'ImgButton',
  headerMenuButtonWidth: 17,
  headerMenuButtonSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/grid/gridHeaderMenuButton.png',
  hoverWidth: 200
});

isc.OBTreeGrid.changeDefaults('headerButtonDefaults', {
  showTitle: true,
  showDown: true,
  showFocused: false,
  // baseStyle / titleStyle is auto-assigned from headerBaseStyle
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/grid/gridHeader_bg.png'
});

isc.OBTreeGrid.changeDefaults('headerMenuButtonDefaults', {
  showDown: false,
  showTitle: true
  //src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/grid/gridHeader_bg.png'
});

isc.OBTreeGrid.changeDefaults('sorterDefaults', {});

isc.OBTreeGrid.changeDefaults('filterEditorDefaults', {
  height: 22,
  styleName: 'OBGridFilterBase',
  baseStyle: 'OBGridFilterCell'
});

isc.OBTreeGrid.changeDefaults('headerButtonDefaults', {
  showTitle: true,
  showDown: true,
  showFocused: false,
  // baseStyle / titleStyle is auto-assigned from headerBaseStyle
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/grid/gridHeader_bg.png'
});

isc.OBTreeGrid.changeDefaults('headerMenuButtonDefaults', {
  showDown: false,
  showTitle: true
  //  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/grid/gridHeader_bg.png'
});

isc.OBTreeGrid.addProperties({
  // note should be the same as the height of the OBGridButtonsComponent
  recordComponentHeight: 21,
  cellHeight: 25,
  bodyStyleName: 'OBViewGridBody'
});