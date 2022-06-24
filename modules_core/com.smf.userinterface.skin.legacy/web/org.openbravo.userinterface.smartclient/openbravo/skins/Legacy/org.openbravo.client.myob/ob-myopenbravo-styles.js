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

isc.OBMyOpenbravo.addProperties({
  styleName: 'OBMyOpenbravo'
});

OB.Styles.OBMyOpenbravo = {
  recentViewsLayout: {
    baseStyle: 'OBMyOBRecentViews',
    nodeIcons: {
      Window: OB.Styles.OBApplicationMenu.Icons.window,
      Process: OB.Styles.OBApplicationMenu.Icons.process,
      Report: OB.Styles.OBApplicationMenu.Icons.report,
      Form: OB.Styles.OBApplicationMenu.Icons.form,
      ExternalLink: OB.Styles.OBApplicationMenu.Icons.externalLink
    },
    Label: {
      baseStyle: 'OBMyOBRecentViewsEntry'
    },
    newIcon: {
      src: OB.Styles.skinsPath + 'Default/org.openbravo.client.myob/images/management/iconCreateNew.png'
    }
  },
  recentDocumentsLayout: {
    baseStyle: 'OBMyOBRecentViews',
    Label: {
      baseStyle: 'OBMyOBRecentViewsEntry',
      icon: OB.Styles.OBApplicationMenu.Icons.document
    }
  },
  actionTitle: {
    baseStyle: 'OBMyOBRecentViews'
  },
  refreshLayout: {
    styleName: 'OBMyOBLeftColumnLink'
  },
  addWidgetLayout: {
    styleName: 'OBMyOBLeftColumnLink'
  },
  adminOtherMyOBLayout: {
    styleName: 'OBMyOBLeftColumnLink'
  },
  leftColumnLayout: {
    styleName: 'OBMyOBLeftColumn'
  },
  portalLayout: {
    styleName: 'OBMyOBPortal',
    membersMargin: 22,
    columnProperties: {
      membersMargin: 18
    }
  }
};

OB.Styles.OBMyOBAddWidgetDialog = {
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldSelectInput',
  pendingTextBoxStyle: null,
  //'OBFormFieldSelectInputPending',
  controlStyle: 'OBFormFieldSelectControl',
  pickListBaseStyle: 'OBFormFieldPickListCell',
  pickListTallBaseStyle: 'OBFormFieldPickListCell',
  pickerIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/comboBoxPicker.png',
  height: 21,
  pickerIconWidth: 21,
  pickListCellHeight: 22,
  pickListProperties: {
    bodyStyleName: 'OBPickListBody'
  }
};

OB.Styles.OBMyOBAdminModeDialog = {
  cellStyle: 'OBFormField',
  titleStyle: 'OBFormFieldLabel',
  textBoxStyle: 'OBFormFieldSelectInput',
  pendingTextBoxStyle: null,
  //'OBFormFieldSelectInputPending',
  controlStyle: 'OBFormFieldSelectControl',
  pickListBaseStyle: 'OBFormFieldPickListCell',
  pickListTallBaseStyle: 'OBFormFieldPickListCell',
  pickerIconSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/comboBoxPicker.png',
  height: 21,
  pickerIconWidth: 21,
  pickListCellHeight: 22,
  pickListProperties: {
    bodyStyleName: 'OBPickListBody'
  }
};

OB.Styles.OBMyOBPublishChangesDialog = {
  form: {
    styleName: 'OBMyOBPublishLegend'
  }
};


// MyOpenbravo dialogs (left menu)
isc.OBMyOBDialog.addProperties({
  styleName: 'OBMyOBDialog',
  headerStyle: 'OBMyOBDialogHeader',
  bodyStyle: "OBMyOBDialogBody",
  showEdges: true,
  edgeImage: OB.Styles.skinsPath + 'Default/org.openbravo.client.myob/images/dialog/window.png',
  customEdges: null,
  edgeSize: 6,
  edgeTop: 23,
  edgeBottom: 6,
  edgeOffsetTop: 2,
  edgeOffsetRight: 5,
  edgeOffsetBottom: 5,
  showHeaderBackground: false,
  // part of edges
  showHeaderIcon: true,

  border: null,

  layoutMargin: 0,
  membersMargin: 0,

  showFooter: false,

  showShadow: false,
  shadowDepth: 5
});

isc.OBMyOBDialog.changeDefaults('headerDefaults', {
  layoutMargin: 0,
  height: 24
});

isc.OBMyOBDialog.changeDefaults('headerLabelDefaults', {
  styleName: 'OBMyOBDialogHeaderText',
  align: isc.Canvas.CENTER
});

isc.OBMyOBDialog.changeDefaults("closeButtonDefaults", {
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.myob/images/dialog/headerIcons/close.png',
  showRollOver: true,
  showDown: false,
  width: 15,
  height: 15
});