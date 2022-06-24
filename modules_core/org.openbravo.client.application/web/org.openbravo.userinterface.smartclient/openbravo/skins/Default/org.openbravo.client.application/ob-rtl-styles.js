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
 * All portions are Copyright (C) 2011-2012 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

/* ob-application-menu-styles.js */

isc.OBApplicationMenuTreeChild.addProperties({
  iconBodyStyleName_rtl: 'OBApplicationMenuTreeIconBody'
});

isc.OBApplicationMenuTree.addProperties({
  iconBodyStyleName_rtl: 'OBApplicationMenuTreeIconBody'
});


/* ob-form-styles.js */

isc.OBDateItem.addProperties({
  textAlign: 'right'
});

isc.OBSectionItemButton.changeDefaults('backgroundDefaults', {
  icon: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/sectionItem-ico-RTL.png'
});


/* ob-grid-styles.js */

isc.OBGridButtonsComponent.addProperties({
  layoutLeftMargin: 0,
  layoutRightMargin: -2
});


/* ob-navigation-bar-styles.js */

isc.OBQuickLaunch.addProperties({
  createNew_src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/navbar/iconCreateNew-RTL.png',
  quickLaunch_src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/navbar/iconQuickLaunch-RTL.png'
});

isc.OBLogout.addProperties({
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/navbar/iconClose-RTL.png'
});

OB.Styles.TopLayout.LogosContainer.align = 'left';


/* ob-personalization-styles.js */

OB.Styles.Personalization.Icons.fieldGroup = OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/personalization/iconFolder-RTL.png';
OB.Styles.Personalization.TabSet.tabBarAlign = 'right';
OB.Styles.Personalization.Menu.iconBodyStyleName_rtl = 'OBPersonalizationPullDownMenuBody';
OB.Styles.Personalization.Menu.itemIcon = OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/personalization/iconSelectedView-RTL.png';


/* ob-process-styles.js */

OB.Styles.Process.PickAndExecute.addNewButtonAlign = 'right';


/* ob-statusbar-styles.js */

isc.OBStatusBarLeftBar.addProperties({
  layoutLeftMargin: 0,
  layoutRightMargin: 7,
  align: 'right'
});

isc.OBStatusBarIconButtonBar.addProperties({
  align: 'left'
});


/* ob-tab-styles.js */

isc.OBTabSetMain.addProperties({
  tabBarAlign: 'right'
});

isc.OBTabSetChild.addProperties({
  tabBarAlign: 'right'
});