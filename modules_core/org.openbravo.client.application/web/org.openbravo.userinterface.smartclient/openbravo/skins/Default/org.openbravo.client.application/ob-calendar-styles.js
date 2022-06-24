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
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.OBCalendarTabSet.addProperties({
  tabBarProperties: {
    simpleTabBaseStyle: 'OBTabBarButtonMain',
    paneContainerClassName: 'OBTabSetMainContainer',
    buttonConstructor: isc.OBTabBarButton,

    buttonProperties: {
      // prevent the orange hats
      customState: 'Inactive',

      src: '',
      capSize: 14,
      titleStyle: 'OBTabBarButtonMainTitle'
    }
  },
  tabBarPosition: 'top',
  tabBarAlign: 'left',
  width: '100%',
  height: '100%',
  overflow: 'hidden',

  showTabPicker: false,

  // get rid of the margin around the content of a pane
  paneMargin: 0,
  paneContainerMargin: 0,
  paneContainerPadding: 0,
  showPaneContainerEdges: false,

  useSimpleTabs: true,
  tabBarThickness: 30,
  styleName: 'OBTabSetMain',
  simpleTabBaseStyle: 'OBTabBarButtonMain',
  paneContainerClassName: 'OBTabSetMainContainer',

  scrollerSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/tab/tabBarButtonMain_OverflowIcon.png',
  pickerButtonSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/tab/tabBarButtonMain_OverflowIconPicker.png'
});


isc.OBCalendar.addProperties({
  eventWindowStyle: 'OBEventWindow',
  baseStyle: 'OBCalendarGridCell',
  workdayBaseStyle: 'OBCalendarGridCellWorkday',
  selectedCellStyle: 'OBCalendarGridCellSelected'
});

isc.OBCalendar.changeDefaults('dayViewDefaults', {
  alternateRecordStyles: false,
  headerBaseStyle: 'OBCalendarGridHeaderCell'
});

isc.OBCalendar.changeDefaults('weekViewDefaults', {
  headerBaseStyle: 'OBCalendarGridHeaderCell'
});

isc.OBCalendar.changeDefaults('monthViewDefaults', {
  headerBaseStyle: 'OBCalendarGridHeaderCell'
});

isc.OBCalendar.changeDefaults('datePickerButtonDefaults', {
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/date_control.png',
  width: 21,
  height: 21
});

isc.OBCalendar.changeDefaults('previousButtonDefaults', {
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/statusbar/iconButton-previous.png',
  width: 20,
  height: 20
});

isc.OBCalendar.changeDefaults('nextButtonDefaults', {
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/statusbar/iconButton-next.png',
  width: 20,
  height: 20
});

isc.OBCalendar.changeDefaults('addEventButtonDefaults', {
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/statusbar/iconButton-add.png',
  width: 18,
  height: 18
});

isc.OBCalendar.changeDefaults('dayLanesToggleButtonDefaults', {
  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/calendar/iconDayLanesToggle.png',
  width: 21,
  height: 21
});

//isc.OBCalendar.changeDefaults('addEventButtonDefaults', {
//  src: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/form/add_icon.png',
//  width: 21,
//  height: 21
//});
isc.OBCalendar.changeDefaults('controlsBarDefaults', {
  layoutTopMargin: 6
});