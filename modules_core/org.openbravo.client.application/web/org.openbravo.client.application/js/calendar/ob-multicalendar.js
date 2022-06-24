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
 * All portions are Copyright (C) 2013-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.ClassFactory.defineClass('OBMultiCalendar', isc.HLayout);

isc.ClassFactory.defineClass('OBMultiCalendarLeftControls', isc.VLayout);

isc.ClassFactory.defineClass('OBMultiCalendarCalendar', isc.OBCalendar);

isc.ClassFactory.defineClass('OBMultiCalendarLegend', isc.VLayout);

isc.ClassFactory.defineClass('OBMultiCalendarLegendGroupElement', isc.HLayout);

isc.ClassFactory.defineClass('OBMultiCalendarLegendElement', isc.HLayout);

isc.OBMultiCalendarLegendGroupElement.addProperties({
  height: 20,
  width: 162,
  color: null,
  name: null,
  id: null,
  checked: true,
  overflow: 'hidden',
  nodes: [],
  show: function() {
    this.Super('show', arguments);
    this.updateCheckboxValue();
    this.updateChildsVisibility();
  },
  updateChildsVisibility: function() {
    var i;
    for (i = 0; i < this.nodes.length; i++) {
      if (this.checked) {
        this.nodes[i].show();
      } else {
        this.nodes[i].hide();
      }
    }
  },
  // Change action of tree button (opened/closed state)
  changedTree: function(value) {
    var calendarData = this.multiCalendar.calendarData,
      i;
    for (i = 0; i < calendarData.calendarGroups.length; i++) {
      if (calendarData.calendarGroups[i].id === this.id) {
        this.checked = value;
        calendarData.calendarGroups[i].checked = value;
      }
    }
    this.updateChildsVisibility();
  },
  // Refresh the checkbox state based on nodes state
  updateCheckboxValue: function() {
    var status = '',
      i; // true, all nodes are true -- false, all nodes are false -- null, are mixed nodes states -- '' is the starting point
    for (i = 0; i < this.nodes.length; i++) {
      if (this.nodes[i].checked) {
        if (status === '') {
          status = true;
        } else if (status === false) {
          status = null;
        }
      } else {
        if (status === '') {
          status = false;
        } else if (status === true) {
          status = null;
        }
      }
    }
    if (status !== '') {
      this.setCheckboxValue(status, true, false, false);
    }
  },
  // Programatically set the checkbox value
  setCheckboxValue: function(
    value,
    updateParent,
    refreshCalendar,
    updateNodes
  ) {
    this.members[1].items[0].setValue(value);
    this.doAfterCheckboxChange(updateParent, refreshCalendar, updateNodes);
  },
  doAfterCheckboxChange: function(updateParent, refreshCalendar, updateNodes) {
    var value = this.members[1].items[0].getValue(),
      i;
    if (updateNodes) {
      for (i = 0; i < this.nodes.length; i++) {
        this.nodes[i].setCheckboxValue(value, false, false);
      }
    }
    if (updateParent && this.parentNode) {
      this.parentNode.updateCheckboxValue();
    }
    if (refreshCalendar) {
      this.multiCalendar.refreshCalendar();
    }
  },
  // Checkbox for all/none nodes selected
  changed: function(form, item, value) {
    if (value === null) {
      // To avoid a "null" state if the user clicks in a "false" state checkbox. It jumps directly to the "true" state
      item.setValue(true);
      item.changed(form, item, true);
      return;
    }
    this.Super('changed', [form, item, value]);
    form.parentElement.doAfterCheckboxChange(true, true, true);
  },
  initWidget: function() {
    var buttonTree,
      checkboxGroup,
      name,
      me = this;
    this.Super('initWidget', arguments);
    if (this.checked === 'true') {
      this.checked = true;
    }
    if (this.checked === 'false') {
      this.checked = false;
    }
    if (this.color) {
      OB.Utilities.Style.addRule(
        '.bgColor_' + this.color,
        'background-color: ' +
          OB.Utilities.getRGBAStringFromOBColor(this.color) +
          ';' +
          'color: ' +
          (OB.Utilities.getBrightFromOBColor(this.color) > 125
            ? 'black'
            : 'white')
      );
    }
    buttonTree = isc.Layout.create({
      width: 15,
      height: 18,
      styleName: 'OBMultiCalendarLegendGroupElementTreeOpened',
      value: null,
      initWidget: function() {
        this.value = me.checked;
        this.updateIcon();
        this.Super('initWidget', arguments);
      },
      updateIcon: function() {
        if (this.value) {
          this.setStyleName('OBMultiCalendarLegendGroupElementTreeOpened');
        } else {
          this.setStyleName('OBMultiCalendarLegendGroupElementTreeClosed');
        }
      },
      click: function() {
        if (this.value) {
          this.value = false;
        } else {
          this.value = true;
        }
        this.updateIcon();
        this.parentElement.changedTree(this.value);
      }
    });
    checkboxGroup = isc.DynamicForm.create({
      width: 20,
      checked: this.checked,
      fields: [
        {
          height: 16,
          width: 20,
          allowEmptyValue: true,
          showUnsetImage: true,
          showTitle: false,
          value: this.checked,
          init: function() {
            this.unsetImage = this.partialSelectedImage;
          },
          changed: this.changed,
          type: 'checkbox'
        }
      ]
    });
    name = isc.Label.create({
      height: 10,
      width: 118,
      styleName: 'OBMultiCalendarLegendElementName',
      contents: this.name
    });
    this.addMembers([buttonTree]);
    this.addMembers([checkboxGroup]);
    this.addMembers([name]);
  }
});

isc.OBMultiCalendarLegendElement.addProperties({
  height: 20,
  width: 162,
  color: null,
  name: null,
  id: null,
  checked: true,
  overflow: 'hidden',
  // Programatically set the checkbox value
  setCheckboxValue: function(value, updateParent, refreshCalendar) {
    value = !!value;
    this.members[2].items[0].setValue(value);
    this.doAfterCheckboxChange(updateParent, refreshCalendar);
  },
  doAfterCheckboxChange: function(updateParent, refreshCalendar) {
    var value = this.members[2].items[0].getValue(),
      calendarData = this.multiCalendar.calendarData,
      i;
    for (i = 0; i < calendarData.calendars.length; i++) {
      if (calendarData.calendars[i].id === this.id) {
        this.checked = value;
        calendarData.calendars[i].checked = value;
      }
    }
    if (updateParent && this.parentNode) {
      this.parentNode.updateCheckboxValue();
    }
    if (refreshCalendar) {
      this.multiCalendar.refreshCalendar();
    }
  },
  changed: function(form, item, value) {
    this.Super('changed', arguments);
    form.parentElement.doAfterCheckboxChange(true, true);
  },
  initWidget: function() {
    var leftMargin,
      checkbox,
      color,
      name,
      me = this;
    this.Super('initWidget', arguments);
    if (this.checked === 'true') {
      this.checked = true;
    }
    if (this.checked === 'false') {
      this.checked = false;
    }
    if (this.color) {
      OB.Utilities.Style.addRule(
        '.bgColor_' + this.color,
        'background-color: ' +
          OB.Utilities.getRGBAStringFromOBColor(this.color) +
          ';' +
          'color: ' +
          (OB.Utilities.getBrightFromOBColor(this.color) > 125
            ? 'black'
            : 'white')
      );
    }
    leftMargin = isc.Layout.create({
      width: me.parentNode ? 20 : 0,
      height: 1
    });
    checkbox = isc.DynamicForm.create({
      width: 20,
      checked: this.checked,
      fields: [
        {
          height: 16,
          width: 20,
          showTitle: false,
          value: this.checked,
          changed: this.changed,
          type: 'checkbox'
        }
      ]
    });
    color = isc.Layout.create({
      width: 15,
      height: 18,
      styleName: 'OBMultiCalendarLegendElementColor',
      backgroundColor: OB.Utilities.getRGBAStringFromOBColor(this.color)
    });
    name = isc.Label.create({
      height: 10,
      width: 118,
      styleName: 'OBMultiCalendarLegendElementName',
      contents: this.name
    });
    this.addMembers([leftMargin]);
    if (this.color) {
      this.addMembers([color]);
    }
    this.addMembers([checkbox]);
    this.addMembers([name]);
  }
});

isc.OBMultiCalendarLegend.addProperties({
  // height: '*',
  overflow: 'auto',
  membersMargin: 5,

  initWidget: function() {
    this.multiCalendar.OBMultiCalendarLegend = this;
    this.Super('initWidget', arguments);
  },

  updateMembers: function(newMembers) {
    var calendarGroups = [],
      i,
      j;
    if (this.members) {
      for (i = this.members.length - 1; i > -1; i--) {
        this.members[i].destroy();
      }
    }
    this.multiCalendar.eventStyles = {};

    // Create calendar groups
    for (
      i = 0;
      i < this.multiCalendar.calendarData.calendarGroups.length;
      i++
    ) {
      calendarGroups.push(
        isc.OBMultiCalendarLegendGroupElement.create({
          multiCalendar: this.multiCalendar,
          name: this.multiCalendar.calendarData.calendarGroups[i].name,
          id: this.multiCalendar.calendarData.calendarGroups[i].id,
          checked: this.multiCalendar.calendarData.calendarGroups[i].checked,
          nodes: []
        })
      );
    }

    // Add nodes to groups
    for (i = 0; i < newMembers.length; i++) {
      if (newMembers[i].calendarGroupId) {
        for (
          j = 0;
          j < this.multiCalendar.calendarData.calendarGroups.length;
          j++
        ) {
          if (
            newMembers[i].calendarGroupId ===
            this.multiCalendar.calendarData.calendarGroups[j].id
          ) {
            calendarGroups[j].nodes.push(
              isc.OBMultiCalendarLegendElement.create({
                multiCalendar: this.multiCalendar,
                color: newMembers[i].color,
                name: newMembers[i].name,
                id: newMembers[i].id,
                //calendarGroupId: newMembers[i].calendarGroupId,
                parentNode: calendarGroups[j],
                checked: newMembers[i].checked
              })
            );
          }
        }
      }
    }

    // Add orphan members
    for (i = 0; i < newMembers.length; i++) {
      if (!newMembers[i].calendarGroupId) {
        this.addMember(
          isc.OBMultiCalendarLegendElement.create({
            multiCalendar: this.multiCalendar,
            color: newMembers[i].color,
            name: newMembers[i].name,
            id: newMembers[i].id,
            calendarGroupId: newMembers[i].calendarGroupId,
            checked: newMembers[i].checked
          })
        );
      }
    }

    // Add calendar groups (if they have nodes) and its nodes
    for (i = 0; i < calendarGroups.length; i++) {
      if (calendarGroups[i].nodes.length > 0) {
        this.addMember(calendarGroups[i]);
        for (j = 0; j < calendarGroups[i].nodes.length; j++) {
          this.addMember(calendarGroups[i].nodes[j]);
        }
      }
    }

    // Save the colors
    for (i = 0; i < newMembers.length; i++) {
      this.multiCalendar.eventStyles[newMembers[i].id] =
        'bgColor_' + newMembers[i].color;
    }

    if (this.multiCalendar.leftControls) {
      // initialized so refresh
      this.multiCalendar.refreshCalendar();
    }
  }
});

isc.OBMultiCalendarLeftControls.addProperties({
  width: '200',
  height: '100%',
  layoutLeftMargin: 10,
  layoutRightMargin: 10,
  layoutTopMargin: 10,
  membersMargin: 5,
  defaultLayoutAlign: 'center',
  filter: null,
  dateChooser: null,
  legend: null,
  getFilterValueMap: function() {
    var filterObj = {},
      calendarData = this.multiCalendar.calendarData,
      i;
    for (i = 0; i < calendarData.filters.length; i++) {
      filterObj[calendarData.filters[i].id] = calendarData.filters[i].name;
    }
    return filterObj;
  },
  getLegendValueMap: function() {
    var calendarData = this.multiCalendar.calendarData,
      legendArray = [],
      i;
    for (i = 0; i < calendarData.calendars.length; i++) {
      if (
        calendarData.hasFilter === false ||
        calendarData.calendars[i].filterId === this.filter.getValue('filter')
      ) {
        legendArray.push(calendarData.calendars[i]);
      }
    }
    return legendArray;
  },
  initWidget: function() {
    var button,
      label,
      customFilterObj,
      leftControls = this,
      currentFilter = null,
      i;
    this.Super('initWidget', arguments);
    if (this.multiCalendar.calendarData.hasFilter) {
      for (
        i = 0;
        i < leftControls.multiCalendar.calendarData.filters.length;
        i++
      ) {
        if (leftControls.multiCalendar.calendarData.filters[i].checked) {
          currentFilter = leftControls.multiCalendar.calendarData.filters[i].id;
          break;
        }
      }
      this.filter = isc.DynamicForm.create({
        fields: [
          {
            name: 'filter',
            title: leftControls.multiCalendar.filterName,
            type: 'comboBox',
            valueMap: leftControls.getFilterValueMap(),
            value: currentFilter,
            width: 180,
            titleOrientation: 'top',
            required: true,
            changed: function(form, item, value) {
              this.Super('changed', arguments);
              for (
                i = 0;
                i < leftControls.multiCalendar.calendarData.filters.length;
                i++
              ) {
                if (
                  leftControls.multiCalendar.calendarData.filters[i].id ===
                  value
                ) {
                  leftControls.multiCalendar.calendarData.filters[
                    i
                  ].checked = true;
                } else {
                  leftControls.multiCalendar.calendarData.filters[
                    i
                  ].checked = false;
                }
              }
              leftControls.legend.updateMembers(
                leftControls.getLegendValueMap()
              );
            },

            cellStyle: OB.Styles.OBFormField.DefaultComboBox.cellStyle,
            titleStyle: OB.Styles.OBFormField.DefaultComboBox.titleStyle,
            textBoxStyle: OB.Styles.OBFormField.DefaultComboBox.textBoxStyle,
            pendingTextBoxStyle:
              OB.Styles.OBFormField.DefaultComboBox.pendingTextBoxStyle,
            controlStyle: OB.Styles.OBFormField.DefaultComboBox.controlStyle,
            pickListBaseStyle:
              OB.Styles.OBFormField.DefaultComboBox.pickListBaseStyle,
            pickListTallBaseStyle:
              OB.Styles.OBFormField.DefaultComboBox.pickListTallBaseStyle,
            pickerIconStyle:
              OB.Styles.OBFormField.DefaultComboBox.pickerIconStyle,
            pickerIconSrc: OB.Styles.OBFormField.DefaultComboBox.pickerIconSrc,
            height: OB.Styles.OBFormField.DefaultComboBox.height,
            pickerIconWidth:
              OB.Styles.OBFormField.DefaultComboBox.pickerIconWidth,
            // fixes issue https://issues.openbravo.com/view.php?id=15105
            pickListCellHeight:
              OB.Styles.OBFormField.DefaultComboBox.quickRunPickListCellHeight,
            recentPropertyName: this.recentPropertyName,
            pickListProperties: {
              textMatchStyle: 'substring',
              selectionType: 'single',
              bodyStyleName:
                OB.Styles.OBFormField.DefaultComboBox.pickListProperties
                  .bodyStyleName
            },
            pickListHeaderHeight: 0
          }
        ]
      });
    } else {
      this.filter = isc.VLayout.create({
        height: 8
      });
    }
    button = isc.OBFormButton.create({
      width: 180,
      title: this.multiCalendar.addEventButtonName,
      click: function() {
        var startDate = OB.Utilities.Date.roundToNextHalfHour(new Date()),
          endDate = new Date(startDate);
        endDate.setHours(endDate.getHours() + 1);
        leftControls.multiCalendar.calendar.addEventWithDialog(
          startDate,
          endDate
        );
      }
    });
    this.dateChooser = isc.OBDateChooser.create({
      autoHide: false,
      showCancelButton: false,
      firstDayOfWeek: this.multiCalendar.firstDayOfWeek,
      dataChanged: function(param) {
        this.parentElement.multiCalendar.calendar.setChosenDate(this.getData());
        this.parentElement.multiCalendar.calendar.setCurrentViewName('day');
      }
    });
    label = isc.Label.create({
      height: 10,
      contents: this.multiCalendar.legendName + ' :'
    });
    this.customFiltersContainer = isc.VLayout.create({
      width: 1,
      height: 1,
      initWidget: function() {
        var i;
        if (leftControls.multiCalendar.calendarData.hasCustomFilters) {
          for (
            i = 0;
            i < leftControls.multiCalendar.calendarData.customFilters.length;
            i++
          ) {
            if (
              leftControls.multiCalendar.calendarData.customFilters[i].handler
                .constructor
            ) {
              customFilterObj = leftControls.multiCalendar.calendarData.customFilters[
                i
              ].handler.constructor.create(
                {
                  multiCalendar: leftControls.multiCalendar,
                  checked:
                    leftControls.multiCalendar.calendarData.customFilters[i]
                      .checked,
                  customFilter:
                    leftControls.multiCalendar.calendarData.customFilters[i]
                },
                leftControls.multiCalendar.calendarData.customFilters[i].handler
                  .constructorProps
              );
              this.addMembers([customFilterObj]);
            }
          }
        }
        this.Super('initWidget', arguments);
      }
    });
    this.legend = isc.OBMultiCalendarLegend.create({
      multiCalendar: this.multiCalendar
    });
    this.legend.updateMembers(leftControls.getLegendValueMap());
    this.addMembers([this.filter]);
    if (this.multiCalendar.canCreateEvents) {
      this.addMembers([button]);
    }
    this.addMembers([
      this.dateChooser,
      this.customFiltersContainer,
      label,
      this.legend
    ]);
  }
});

isc.OBMultiCalendar.addProperties({
  width: '100%',
  height: '100%',
  filterName: OB.I18N.getLabel('OBUIAPP_CalWidget_Filter'),
  legendName: OB.I18N.getLabel('OBUIAPP_CalWidget_Legend'),
  addEventButtonName: OB.I18N.getLabel('OBUIAPP_CalWidget_AddEvent'),
  defaultViewName: null,
  calendarData: null,
  showLeftControls: true,
  showCustomEventsBgColor: true,

  parseCalendarData: function(calendarData) {
    var canCreateEvents, cPropAttr, i;
    if (calendarData.filters) {
      calendarData.hasFilter = true;
    } else {
      calendarData.hasFilter = false;
    }
    if (calendarData.customFilters) {
      calendarData.hasCustomFilters = true;
    } else {
      calendarData.hasCustomFilters = false;
    }
    for (i = 0; i < calendarData.calendarGroups.length; i++) {
      // calendarGroups.checked means if the tree is opened or not
      if (typeof calendarData.calendarGroups[i].checked === 'undefined') {
        calendarData.calendarGroups[i].checked = false;
      }
    }
    for (i = 0; i < calendarData.calendars.length; i++) {
      if (typeof calendarData.calendars[i].checked === 'undefined') {
        calendarData.calendars[i].checked = true;
      }
      if (typeof calendarData.calendars[i].color === 'undefined') {
        calendarData.calendars[i].color = OB.Utilities.generateOBColor(
          null,
          null,
          null,
          100,
          calendarData.calendars[i].id
        );
      }
      if (
        i === 0 &&
        typeof calendarData.calendars[i].canCreateEvents !== 'undefined'
      ) {
        canCreateEvents = false;
      }
      if (
        typeof calendarData.calendars[i].canCreateEvents !== 'undefined' &&
        canCreateEvents === false &&
        calendarData.calendars[i].canCreateEvents === true
      ) {
        canCreateEvents = true;
      }
      if (
        canCreateEvents === false &&
        i === calendarData.calendars.length - 1
      ) {
        this.canCreateEvents = false;
        this.calendarProps.canCreateEvents = false;
      }
    }
    if (calendarData.hasFilter) {
      for (i = 0; i < calendarData.filters.length; i++) {
        if (typeof calendarData.filters[i].checked === 'undefined') {
          calendarData.filters[i].checked = false;
        }
      }
    }
    if (calendarData.hasCustomFilters) {
      for (i = 0; i < calendarData.customFilters.length; i++) {
        if (typeof calendarData.customFilters[i].checked === 'undefined') {
          calendarData.customFilters[i].checked = false;
        }
        if (
          typeof calendarData.customFilters[i].handler === 'string' &&
          this.calendarProps[calendarData.customFilters[i].handler]
        ) {
          calendarData.customFilters[i].handler = this.calendarProps[
            calendarData.customFilters[i].handler
          ];
        }
        if (
          typeof calendarData.customFilters[i].handler.constructor === 'string'
        ) {
          calendarData.customFilters[i].handler.constructor = new Function(
            'return ' + calendarData.customFilters[i].handler.constructor
          )();
        }
      }
    }
    if (calendarData.calendarProps && this.calendarProps) {
      for (cPropAttr in calendarData.calendarProps) {
        if (
          Object.prototype.hasOwnProperty.call(
            calendarData.calendarProps,
            cPropAttr
          )
        ) {
          this.calendarProps[cPropAttr] = calendarData.calendarProps[cPropAttr];
        }
      }
    }
    if (typeof this.calendarProps.customParseCalendarData === 'function') {
      calendarData = this.calendarProps.customParseCalendarData(calendarData);
    }

    return calendarData;
  },
  setLoading: function(value) {
    if (value !== false) {
      if (this.members[1]) {
        this.members[1].hide();
      }
      if (this.members[2]) {
        this.members[2].hide();
      }
      if (this.members[0]) {
        this.members[0].show();
      }
    } else {
      if (this.members[0]) {
        this.members[0].hide();
      }
      if (this.members[1]) {
        this.members[1].show();
      }
      if (this.members[2]) {
        this.members[2].show();
      }
    }
  },
  initComponents: function() {
    var callback,
      i,
      me = this;
    for (i = this.members.length - 1; i > -1; i--) {
      this.members[i].destroy();
    }
    if (this.calendarProps.firstDayOfWeek) {
      this.firstDayOfWeek = this.calendarProps.firstDayOfWeek;
    } else {
      this.firstDayOfWeek = 1;
    }
    if (this.calendarProps.filterName) {
      this.filterName = this.calendarProps.filterName;
    }
    if (this.calendarProps.legendName) {
      this.legendName = this.calendarProps.legendName;
    }
    if (this.calendarProps.addEventButtonName) {
      this.addEventButtonName = this.calendarProps.addEventButtonName;
    }
    if (typeof this.calendarProps.showLeftControls !== 'undefined') {
      this.showLeftControls = this.calendarProps.showLeftControls;
    }
    if (typeof this.calendarProps.showCustomEventsBgColor !== 'undefined') {
      this.showCustomEventsBgColor = this.calendarProps.showCustomEventsBgColor;
    }
    if (typeof this.calendarProps.canCreateEvents !== 'undefined') {
      this.canCreateEvents = this.calendarProps.canCreateEvents;
    }
    if (this.calendarProps.showDayView === false) {
      this.calendarProps.showDayLanesToggleControl = false;
    }
    this.addMembers([OB.Utilities.createLoadingLayout()]);
    callback = function(rpcResponse, data, rpcRequest) {
      if (data.message) {
        isc.warn(
          data.message.text,
          function() {
            return true;
          },
          {
            icon: '[SKINIMG]Dialog/error.png',
            title: OB.I18N.getLabel('OBUIAPP_Error')
          }
        );
      }
      me.calendarData = me.parseCalendarData(data);
      me.drawComponents();
    };
    OB.RemoteCallManager.call(
      this.calendarProps.calendarDataActionHandler,
      {
        action: this.calendarProps.calendarDataActionHandler_Action
      },
      {},
      callback
    );
  },

  initWidget: function() {
    this.initComponents();
    this.Super('initWidget', arguments);
  },
  drawComponents: function() {
    var initialLanes;
    if (
      this.calendarProps.showDayLanes ||
      this.calendarProps.showDayLanesToggleControl !== false
    ) {
      //Inside this 'if' statement to avoid extra computational tasks if lanes are not going to be shown
      initialLanes = this.calculateLanes();
    }
    if (this.canCreateEvents) {
      this.showCustomEventsBgColor = true;
    }

    this.leftControls = isc.OBMultiCalendarLeftControls.create({
      multiCalendar: this
    });
    this.calendar = isc.OBMultiCalendarCalendar.create(
      isc.addProperties(this.calendarProps, {
        multiCalendar: this,
        lanes: initialLanes,
        autoFetchData: false
      })
    );

    this.setLoading(false);
    if (this.showLeftControls) {
      this.addMembers([this.leftControls]);
    }
    this.addMembers([
      isc.VLayout.create({
        members: [this.calendar]
      })
    ]);
    this.refreshCalendar();
  },

  calculateLanes: function() {
    var showedLanes = [],
      laneDefObj = {},
      selectedOrg,
      i,
      calendarData = this.calendarData;
    for (i = 0; i < calendarData.filters.length; i++) {
      if (calendarData.filters[i].checked) {
        selectedOrg = calendarData.filters[i].id;
        break;
      }
    }
    for (i = 0; i < calendarData.calendars.length; i++) {
      if (
        calendarData.calendars[i].filterId === selectedOrg &&
        calendarData.calendars[i].checked
      ) {
        laneDefObj = {};
        laneDefObj.name = calendarData.calendars[i].id;
        laneDefObj.title = calendarData.calendars[i].name;
        showedLanes.push(laneDefObj);
      }
    }
    return showedLanes;
  },

  refreshCalendar: function() {
    if (this.calendar) {
      if (
        this.calendar.showDayLanes ||
        this.calendar.showDayLanesToggleControl !== false
      ) {
        //Inside this 'if' statement to avoid extra computational tasks if lanes are not going to be shown
        this.calendar.setLanes(this.calculateLanes());
      }
      this.calendar.filterData();
    }
  }
});
