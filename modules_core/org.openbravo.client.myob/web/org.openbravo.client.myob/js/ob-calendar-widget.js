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

isc.ClassFactory.defineClass('OBCalendarWidget', isc.OBWidget);

isc.OBCalendarWidget.addProperties({
  widgetId: null,
  widgetInstanceId: null,

  createWindowContents: function() {
    var layout, calendarProps;
    this.calendarProps = new Function(
      'return ' + this.parameters.calendarProps
    )();
    if (!this.calendarProps.isMultiCalendar && this.parameters.legendId) {
      this.calendarProps.legendId = this.parameters.legendId;
    }
    calendarProps = this.calendarProps;

    if (!calendarProps) {
      return isc.VStack.create({
        members: [
          isc.Label.create({
            contents:
              'This widget must have a parameter "calendarProps" (DB Column Name) with a fixed value pointing to the object with the calendar properties'
          })
        ]
      });
    }

    if (
      this.viewMode === 'maximized' &&
      calendarProps.maximizedDefaultViewName
    ) {
      calendarProps.defaultViewName = calendarProps.maximizedDefaultViewName;
    } else if (calendarProps.restoredDefaultViewName) {
      calendarProps.defaultViewName = calendarProps.restoredDefaultViewName;
    }

    layout = isc.VStack.create({
      height: '100%',
      width: '100%',
      styleName: ''
    });

    if (calendarProps.isMultiCalendar) {
      layout.addMember(
        isc.OBMultiCalendar.create({
          calendarProps: calendarProps
        })
      );
    } else {
      layout.addMember(
        isc.OBMultiCalendarCalendar.create(isc.addProperties(calendarProps, {}))
      );
    }

    return layout;
  },

  refresh: function() {
    this.members[1].members[0].members[0].initComponents();
  },

  maximize: function() {
    OB.Layout.ViewManager.openView('OBCalendarWidgetView', {
      tabTitle: this.title,
      widgetInstanceId: this.dbInstanceId,
      widgetId: this.widgetId,
      parameters: this.parameters,
      menuItems: this.menuItems
    });
  }
});

isc.ClassFactory.defineClass('OBCalendarWidgetView', isc.PortalLayout);

isc.OBCalendarWidgetView.addProperties({
  //Set PortalLayout common parameters
  numColumns: 1,
  showColumnMenus: false,
  canDropComponents: false,

  initWidget: function(args) {
    var widgetInstance, i;
    this.Super('initWidget', arguments);

    if (isc['_' + this.widgetId]) {
      widgetInstance = isc['_' + this.widgetId].create(
        isc.addProperties({
          viewMode: 'maximized',
          title: this.tabTitle,
          widgetInstanceId: this.widgetInstanceId,
          widgetId: this.widgetId,
          dbInstanceId: this.widgetInstanceId,
          parameters: this.parameters,
          menuItems: this.menuItems,
          canDelete: false
        })
      );
      this.addPortlet(widgetInstance);
    } else {
      //If the instance doesn't exist, just close the tab
      for (i = 0; i < OB.MainView.TabSet.tabs.length; i++) {
        if (OB.MainView.TabSet.tabs[i].title === 'OBCalendarWidgetView') {
          OB.MainView.TabSet.removeTabs(i);
          break;
        }
      }
    }
  },

  isSameTab: function(viewName, params) {
    return this.widgetInstanceId === params.widgetInstanceId;
  }
});
