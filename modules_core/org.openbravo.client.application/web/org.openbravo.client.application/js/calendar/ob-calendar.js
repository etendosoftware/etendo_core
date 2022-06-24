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
 * All portions are Copyright (C) 2013-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// == OBCalendar_EventDialogBridge ==
// Hack to allow the OBCalendar open its own full customizable EventDialog (OBEventEditor)
isc.ClassFactory.defineClass('OBCalendar_EventDialogBridge', isc.Window);

isc.OBCalendar_EventDialogBridge.addProperties({
  show: function() {
    this.Super('show', arguments);

    var calendar = this.creator,
      currentStart = this.currentStart,
      currentEnd = this.currentEnd,
      currentLane = this.currentLane,
      event = this.event;

    if (calendar.OBEventEditor) {
      calendar.OBEventEditor.setProperties({
        calendar: calendar,
        currentStart: currentStart,
        currentEnd: currentEnd,
        currentLane: currentLane,
        event: event
      });
      calendar.OBEventEditor.initComponents();
      calendar.OBEventEditor.show();
      this.closeClick();
    }
  }
});

// == OBCalendar_EventDialogBridge ==
// Hack to allow calendar TabSet style personalization
isc.ClassFactory.defineClass('OBCalendarTabSet', isc.TabSet);

// == OBClientClassCanvasItem ==
// Extends Calendar, with some customizations (most of them styling related)
isc.ClassFactory.defineClass('OBCalendar', isc.Calendar);

isc.OBCalendar.addProperties({
  autoFetchData: true,
  mainViewConstructor: isc.OBCalendarTabSet,
  useEventCanvasRolloverControls: false,
  eventDialogConstructor: isc.OBCalendar_EventDialogBridge,
  eventsAdaptationRequired: true,
  initWidget: function() {
    var calendar = this,
      multiCalendar = this.multiCalendar;

    if (!this.eventIdField) {
      this.eventIdField = 'eventId';
    }
    if (typeof this.showAddEventControl === 'undefined') {
      this.showAddEventControl = true;
    }
    if (typeof this.showDatePickerControl === 'undefined') {
      this.showDatePickerControl = true;
    }
    if (this.OBEventEditorClass) {
      this.OBEventEditor = isc[this.OBEventEditorClass].create({});
    }

    this.adaptEvents = function(events) {
      var i, j;
      for (i = 0; i < events.getLength(); i++) {
        if (typeof events[i][calendar.startDateField] === 'string') {
          events[i][calendar.startDateField] = new Date(
            events[i][calendar.startDateField]
          );
        }
        if (typeof events[i][calendar.endDateField] === 'string') {
          events[i][calendar.endDateField] = new Date(
            events[i][calendar.endDateField]
          );
        }
        if (typeof events[i][calendar.nameField] === 'undefined') {
          //To avoid the event displays 'undefined' when no name has been set
          events[i][calendar.nameField] = '';
        }
        if (typeof events[i][calendar.descriptionField] === 'undefined') {
          events[i][calendar.descriptionField] = '';
        }
        if (multiCalendar && multiCalendar.showCustomEventsBgColor) {
          events[i].eventWindowStyle =
            multiCalendar.eventStyles[events[i][calendar.legendIdField]] +
            ' ' +
            calendar.eventWindowStyle;
        }
        if (typeof calendar.customTransformResponse === 'function') {
          events[i] = calendar.customTransformResponse(events[i], calendar);
        }
        if (multiCalendar && multiCalendar.calendarData.hasCustomFilters) {
          for (
            j = 0;
            j < multiCalendar.calendarData.customFilters.length;
            j++
          ) {
            if (
              typeof multiCalendar.calendarData.customFilters[j].handler
                .transformResponse === 'function'
            ) {
              events[i] = multiCalendar.calendarData.customFilters[
                j
              ].handler.transformResponse(
                events[i],
                calendar,
                multiCalendar.calendarData.customFilters[j]
              );
            }
          }
        }
      }
      return events;
    };

    if (this.data && this.eventsAdaptationRequired) {
      //If 'data' (local data) is being used instead of 'dataSource', adapt/transform/process the raw events.
      this.data = this.adaptEvents(this.data);
    }

    if (this.dataSourceProps) {
      this.dataSource = OB.Datasource.create({
        dataURL: this.dataSourceProps.dataURL,
        fields: [
          {
            name: this.eventIdField,
            primaryKey: true
          },
          {
            name: this.nameField
          },
          {
            name: this.descriptionField
          },
          {
            name: this.startDateField,
            type: 'datetime'
          },
          {
            name: this.endDateField,
            type: 'datetime'
          }
        ],

        // these are read extra from the server with the events
        additionalProperties: this.dataSourceProps.additionalProperties,

        dataSourceProps: this.dataSourceProps,

        transformRequest: function(dsRequest) {
          dsRequest.params = dsRequest.params || {};
          dsRequest.params._extraProperties = this.additionalProperties;
          dsRequest.willHandleError = true;

          return this.Super('transformRequest', arguments);
        },
        transformResponse: function(dsResponse, dsRequest, data) {
          var showDSAlert,
            records = data && data.response && data.response.data;

          showDSAlert = function(text) {
            if (calendar.OBEventEditor && calendar.OBEventEditor.messageBar) {
              // Display message in event editor
              calendar.OBEventEditor.messageBar.setMessage(
                isc.OBMessageBar.TYPE_ERROR,
                OB.I18N.getLabel('OBUIAPP_Error'),
                text
              );
            } else {
              // there is no message bar in editor, showing a popup warn
              isc.warn(
                text,
                function() {
                  return true;
                },
                {
                  icon: '[SKINIMG]Dialog/error.png',
                  title: OB.I18N.getLabel('OBUIAPP_Error')
                }
              );
            }
            if (calendar.OBEventEditor) {
              // needs to keep the popup open once, because data is refreshed
              calendar.OBEventEditor.keepOpen = true;
            }
          };

          // handle error
          if (data && data.response && data.response.error) {
            showDSAlert(data.response.error.message);
            return;
          } else if (data && data.response && data.response.errors) {
            showDSAlert(JSON.stringify(data.response.errors));
            return;
          } else {
            if (records && calendar.eventsAdaptationRequired) {
              records = calendar.adaptEvents(records);
            }
            if (typeof calendar.OBEventEditor.closeClick === 'function') {
              // close editor popup on success
              calendar.OBEventEditor.closeClick();
            }
          }
          return this.Super('transformResponse', arguments);
        },

        // override the addData, updateData and removeData to wrap
        // the calendar callback to prevent adding events in cased
        // of errors
        addData: function(newRecord, callback, requestProperties) {
          var dataSourceProps = this.dataSourceProps,
            newCallBack = function(dsResponse, data, dsRequest) {
              // don't call if there is an error
              if (dsResponse.status < 0) {
                return;
              }
              callback(dsResponse, data, dsRequest);
              if (dataSourceProps.addEventCallback) {
                dataSourceProps.addEventCallback(newRecord, requestProperties);
              }
            };
          return this.Super('addData', [
            newRecord,
            newCallBack,
            requestProperties
          ]);
        },
        updateData: function(updatedRecord, callback, requestProperties) {
          var dataSourceProps = this.dataSourceProps,
            newCallBack = function(dsResponse, data, dsRequest) {
              // don't call if there is an error
              if (dsResponse.status < 0) {
                return;
              }
              callback(dsResponse, data, dsRequest);
              if (dataSourceProps.updateEventCallback) {
                dataSourceProps.updateEventCallback(
                  updatedRecord,
                  requestProperties
                );
              }
            };
          return this.Super('updateData', [
            updatedRecord,
            newCallBack,
            requestProperties
          ]);
        },
        removeData: function(recordKeys, callback, requestProperties) {
          var dataSourceProps = this.dataSourceProps,
            newCallBack = function(dsResponse, data, dsRequest) {
              // don't call if there is an error
              if (dsResponse.status < 0) {
                return;
              }
              callback(dsResponse, data, dsRequest);
              if (dataSourceProps.removeEventCallback) {
                dataSourceProps.removeEventCallback(
                  recordKeys,
                  requestProperties
                );
              }
            };
          return this.Super('removeData', [
            recordKeys,
            newCallBack,
            requestProperties
          ]);
        }
      });
    }

    this.Super('initWidget', arguments);
    this.controlsBar.reorderMember(4, 1); // Moves the 'next' button to the second position
    this.controlsBar.reorderMember(2, 4); // Moves the 'displayed date' to last position
    if (this.showDayView !== false && this.showDayLanesToggleControl) {
      this.controlsBar.addMember(
        isc.ImgButton.create(
          {
            prompt: OB.I18N.getLabel('OBUIAPP_CalendarShowHideLanes'),
            showDown: false,
            showRollOver: false,
            action: function() {
              var calendar = this.parentElement.creator;
              if (calendar.showDayLanes) {
                calendar.setShowDayLanes(false);
              } else {
                calendar.setShowDayLanes(true);
              }
            }
          },
          this.dayLanesToggleButtonDefaults
        ),
        2
      );
    }

    if (
      this.defaultViewName &&
      ((this.showDayView !== false && this.showWeekView !== false) ||
        (this.showDayView !== false && this.showMonthView !== false) ||
        (this.showWeekView !== false && this.showMonthView !== false))
    ) {
      this.setCurrentViewName(this.defaultViewName);
    }
    if (!this.showAddEventControl) {
      this.addEventButton.hide();
    }
    if (!this.showDatePickerControl) {
      this.datePickerButton.hide();
    }
  },

  directEventEdit: function(event, popupCallback) {
    var callback,
      openEventDialog,
      calendar = this;
    callback = function(dsResponse, data, dsRequest) {
      if (data && data[0]) {
        openEventDialog(data[0]);
      }
    };
    openEventDialog = function(event) {
      if (event) {
        if (calendar.OBEventEditor) {
          calendar.eventDialog.event = event;
          calendar.eventDialog.event.popupCallback = popupCallback;
          calendar.eventDialog.currentStart = event[calendar.startDateField];
          calendar.eventDialog.currentEnd = event[calendar.endDateField];
          calendar.eventDialog.currentLane = event[calendar.laneNameField];
          calendar.eventDialog.calendar = calendar;
          try {
            if (
              event[calendar.canEditField] === false &&
              event[calendar.canRemoveField] === false
            ) {
              isc.warn(
                OB.I18N.getLabel('OBUIAPP_CalendarCanNotUpdateEvent'),
                function() {
                  return true;
                },
                {
                  icon: '[SKINIMG]Dialog/error.png',
                  title: OB.I18N.getLabel('OBUIAPP_Error')
                }
              );
            } else {
              calendar.eventDialog.show();
            }
          } catch (e) {
            //To avoid js error due to conflicts with Smartclient default EventDialog
          }
        }
      }
    };

    if (typeof event === 'string') {
      this.dataSource.fetchRecord(event, callback);
    } else {
      openEventDialog(event);
    }
  },

  eventResized: function(newDate, event) {
    newDate.setSeconds(0);
    if (this.showEventDialogOnEventResize) {
      this.eventDialog.event = event;
      this.eventDialog.currentStart = event[this.startDateField];
      this.eventDialog.currentEnd = newDate;
      this.eventDialog.currentLane = event[this.laneNameField];
      this.eventDialog.calendar = this;
      try {
        this.eventDialog.show();
      } catch (e) {
        //To avoid js error due to conflicts with Smartclient default EventDialog
      }
      return false;
    } else {
      return this.Super('eventResized', arguments);
    }
  },
  eventMoved: function(newDate, event, newLane) {
    newDate.setSeconds(0);
    if (this.showEventDialogOnEventMove) {
      //Event duration
      var dateDiff = event[this.endDateField] - event[this.startDateField],
        newEndDate = newDate.getTime() + dateDiff; //Add the event duration to the new startDate
      newEndDate = new Date(newEndDate);
      this.eventDialog.event = event;
      this.eventDialog.currentStart = newDate;
      this.eventDialog.currentEnd = newEndDate;
      this.eventDialog.currentLane = newLane;
      this.eventDialog.calendar = this;
      try {
        this.eventDialog.show();
      } catch (e) {
        //To avoid js error due to conflicts with Smartclient default EventDialog
      }
      return false;
    } else {
      return this.Super('eventMoved', arguments);
    }
  },
  eventRemoveClick: function(event) {
    if (this.showEventDialogOnEventDelete) {
      this.eventDialog.event = event;
      this.eventDialog.currentStart = event[this.startDateField];
      this.eventDialog.currentEnd = event[this.endDateField];
      this.eventDialog.currentLane = event[this.laneNameField];
      this.eventDialog.calendar = this;
      try {
        this.eventDialog.show();
      } catch (e) {
        //To avoid js error due to conflicts with Smartclient default EventDialog
      }
      return false;
    } else {
      return this.Super('eventRemoveClick', arguments);
    }
  },
  showOBEventDialog: function() {
    var dialog = isc.OBPopup.create({});
    dialog.show();
  },

  getCriteria: function(criteria) {
    var startTime,
      endTime,
      legend,
      i,
      startDateCriteria,
      middleDateCriteria,
      endDateCriteria,
      dateCriteriaOrPart,
      orPart = {
        operator: 'or',
        criteria: []
      };

    if (!criteria || !criteria.operator) {
      criteria = {
        _constructor: 'AdvancedCriteria',
        operator: 'and'
      };
    }
    criteria.criteria = criteria.criteria || [];

    if (this.month === 0) {
      startTime = new Date(this.year - 1, 11, 23, 0, 0, 0);
    } else {
      startTime = new Date(this.year, this.month - 1, 23, 0, 0, 0);
    }

    // add the date criteria
    if (this.month === 11) {
      endTime = new Date(this.year + 1, 0, 7, 0, 0, 0);
    } else {
      endTime = new Date(this.year, this.month + 1, 7, 0, 0, 0);
    }

    // To set an 'OR' logic for the following three cases. If at least one of them match, the event will be shown.
    dateCriteriaOrPart = {
      operator: 'or',
      criteria: []
    };

    // To show events that starts in the current month
    startDateCriteria = {
      operator: 'and',
      criteria: []
    };
    startDateCriteria.criteria.push({
      fieldName: this.startDateField,
      operator: 'greaterOrEqual',
      value: startTime
    });
    startDateCriteria.criteria.push({
      fieldName: this.startDateField,
      operator: 'lessThan',
      value: endTime
    });
    dateCriteriaOrPart.criteria.push(startDateCriteria);

    // To show events that starts before current month and ends after current month
    middleDateCriteria = {
      operator: 'and',
      criteria: []
    };
    middleDateCriteria.criteria.push({
      fieldName: this.startDateField,
      operator: 'lessThan',
      value: startTime
    });
    middleDateCriteria.criteria.push({
      fieldName: this.endDateField,
      operator: 'greaterThan',
      value: endTime
    });
    dateCriteriaOrPart.criteria.push(middleDateCriteria);

    // To show events that ends in the current month
    endDateCriteria = {
      operator: 'and',
      criteria: []
    };
    endDateCriteria.criteria.push({
      fieldName: this.endDateField,
      operator: 'greaterThan',
      value: startTime
    });
    endDateCriteria.criteria.push({
      fieldName: this.endDateField,
      operator: 'lessOrEqual',
      value: endTime
    });
    dateCriteriaOrPart.criteria.push(endDateCriteria);

    criteria.criteria.push(dateCriteriaOrPart);

    if (this.multiCalendar) {
      legend = this.multiCalendar.leftControls.getLegendValueMap();
      for (i = 0; i < legend.getLength(); i++) {
        if (legend[i].checked) {
          orPart.criteria.push({
            fieldName: this.legendIdField,
            operator: 'equals',
            value: legend[i].id
          });
        }
      }
      // some dummy value to force an empty resultset
      if (orPart.criteria.getLength() === 0) {
        orPart.criteria.push({
          fieldName: this.legendIdField,
          operator: 'equals',
          value: new Date().getTime().toString()
        });
      }
    } else if (this.legendId) {
      orPart.criteria.push({
        fieldName: this.legendIdField,
        operator: 'equals',
        value: this.legendId
      });
    }

    if (orPart.criteria.getLength() > 0) {
      criteria.criteria.push(orPart);
    }

    if (
      this.multiCalendar &&
      this.multiCalendar.calendarData.hasCustomFilters
    ) {
      for (
        i = 0;
        i < this.multiCalendar.calendarData.customFilters.length;
        i++
      ) {
        if (
          typeof this.multiCalendar.calendarData.customFilters[i].handler
            .filterCriteria === 'function'
        ) {
          criteria.criteria.push(
            this.multiCalendar.calendarData.customFilters[
              i
            ].handler.filterCriteria(
              this,
              this.multiCalendar.calendarData.customFilters[i]
            )
          );
        }
      }
    }

    if (typeof this.getCustomCriteria === 'function') {
      criteria.criteria.push(this.getCustomCriteria(this));
    }

    // always force a reload
    criteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());

    return criteria;
  },

  // This is needed, because the first time (and only the first time) we switch to week view (if we load the day view),
  // or the other way around, we need to set also the initialScroll to this other view.
  isInitialScrollAlreadyBeenSet: false,

  draw: function() {
    var ret,
      _originalTabSelected = this.mainView.tabSelected,
      calendar = this;
    ret = this.Super('draw', arguments);

    // If change filter/legend parameters in day/week view and you switch to the other one,
    // data needs to be refreshed in order to show changes
    if (
      this.multiCalendar &&
      this.mainView &&
      typeof this.mainView.selectTab === 'function'
    ) {
      this.mainView.tabSelected = function(tabNum) {
        var actionObject, mvret;
        actionObject = {
          target: this,
          method: _originalTabSelected,
          parameters: arguments
        };
        mvret = OB.Utilities.callAction(actionObject);
        calendar.refreshSelectedView();
        if (!calendar.isInitialScrollAlreadyBeenSet && tabNum <= 1) {
          calendar.isInitialScrollAlreadyBeenSet = true;
          // Timeout to allow new selected tab grid be fully loaded
          if (calendar.initialScrollTime) {
            setTimeout(function() {
              try {
                calendar.scrollToTime(calendar.initialScrollTime);
              } catch (e) {
                // Ignoring calendar exception
              }
            }, 100);
          }
        }
        return mvret;
      };
    }
    // Timeout to allow the tab grid be fully loaded
    if (calendar.initialScrollTime) {
      setTimeout(function() {
        try {
          calendar.scrollToTime(calendar.initialScrollTime);
        } catch (e) {
          // Ignoring calendar exception
        }
      }, 100);
    }
    return ret;
  },

  fetchData: function(criteria, callback, request) {
    return this.Super('fetchData', [
      this.getCriteria(criteria),
      callback,
      request
    ]);
  },

  filterData: function(criteria) {
    var newCriteria, ret;
    newCriteria = this.getCriteria(criteria);
    ret = this.Super('filterData', [newCriteria]);
    if (this.doPreFilterData) {
      this.doPreFilterData(newCriteria);
    }
    return ret;
  },

  // read the dates for the current month
  dateChanged: function() {
    if (this.multiCalendar) {
      this.multiCalendar.leftControls.dateChooser.setData(this.chosenDate);
    }

    // no change
    if (this.month === this.prevMonth) {
      return;
    }

    this.prevMonth = this.month;
    this.filterData();
  },

  addEvent: function(
    startDate,
    endDate,
    name,
    description,
    otherFields,
    laneName,
    ignoreDataChanged
  ) {
    otherFields = otherFields || {};

    // solve bug that otherwise time fields are not passed in
    startDate.logicalDate = false;
    endDate.logicalDate = false;

    return this.Super('addEvent', arguments);
  },

  addEventWithDialog: function(startDate, endDate) {
    if (!startDate) {
      startDate = new Date();
    }
    if (!endDate) {
      endDate = new Date();
    }
    this.eventDialog.event = null;
    this.eventDialog.currentStart = startDate;
    this.eventDialog.currentEnd = endDate;
    this.eventDialog.currentLane = null;
    this.eventDialog.calendar = this;
    try {
      this.eventDialog.show();
    } catch (e) {
      //To avoid js error due to conflicts with Smartclient default EventDialog
    }
  }
});
