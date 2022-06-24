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
 * All portions are Copyright (C) 2011-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.ClassFactory.defineClass('OBQuickLaunchRecentLinkButton', isc.Button);

isc.OBQuickLaunchRecentLinkButton.addProperties({
  recentObject: null,
  prefixLabel: null,
  action: function() {
    if (this.recentObject.viewId) {
      if (
        this.recentObject.openLinkInBrowser &&
        this.recentObject.viewId === 'OBExternalPage'
      ) {
        if (this.recentObject.contentsURL.indexOf('://') === -1) {
          this.recentObject.contentsURL =
            'http://' + this.recentObject.contentsURL;
        }
        OB.ViewManager.recentManager.addRecent(
          'OBUIAPP_RecentViewList',
          isc.addProperties(
            {
              icon: OB.Styles.OBApplicationMenu.Icons.externalLink
            },
            this.recentObject
          )
        );
        window.open(this.recentObject.contentsURL);
      } else {
        OB.Layout.ViewManager.openView(
          this.recentObject.viewId,
          this.recentObject
        );
      }
    } else {
      OB.Layout.ViewManager.openView('OBClassicWindow', this.recentObject);
    }

    if (isc.OBQuickRun.currentQuickRun) {
      isc.OBQuickRun.currentQuickRun.doHide();
    }
  },
  initWidget: function() {
    if (this.prefixLabel.length > 0) {
      this.title =
        OB.I18N.getLabel(this.prefixLabel) + ' ' + this.recentObject.tabTitle;
    } else {
      this.title = this.recentObject.tabTitle;
    }

    if (this.recentObject.icon) {
      if (this.recentObject.icon === 'Process') {
        this.setIcon(this.nodeIcons.Process);
      } else if (this.recentObject.icon === 'Report') {
        this.setIcon(this.nodeIcons.Report);
      } else if (this.recentObject.icon === 'Form') {
        this.setIcon(this.nodeIcons.Form);
      } else if (this.recentObject.icon === 'ExternalLink') {
        this.setIcon(this.nodeIcons.ExternalLink);
      } else {
        this.setIcon(this.nodeIcons.Window);
      }
    }
  }
});

isc.ClassFactory.defineClass('OBQuickLaunch', isc.OBQuickRun);

isc.OBQuickLaunch.addProperties({
  showInPortal: false,

  draw: function() {
    this.Super('draw', arguments);
    if (this.itemPrompt) {
      this.setPrompt(this.itemPrompt); // itemPrompt declared at quick-launch.js.ftl
      /* Avoid declare directly "prompt: " in this widget definition.
         Declared as "setPrompt" inside "draw" function to solve issue https://issues.openbravo.com/view.php?id=18192 in FF */
    }
  },

  beforeShow: function() {
    var valueField = this.members[2].getField('value'),
      recent = OB.RecentUtilities.getRecentValue(this.recentPropertyName);
    if (recent && recent.length > 0) {
      var newFields = [];
      var index = 0,
        recentIndex,
        length = recent.length;
      for (recentIndex = 0; recentIndex < length; recentIndex++) {
        if (recent[recentIndex]) {
          newFields[index] = isc.OBQuickLaunchRecentLinkButton.create({
            recentObject: recent[recentIndex],
            prefixLabel: this.prefixLabel,
            nodeIcons: this.nodeIcons
          });
          newFields[index].recentPropertyName = this.recentPropertyName;
          index++;
        }
      }
      if (this.members[1].getMembers()) {
        this.members[1].destroyAndRemoveMembers(
          this.members[1].getMembers().duplicate()
        );
      }

      this.members[1].setMembers(newFields);

      if (this.separatorHeight) {
        this.members[1].layoutBottomMargin = this.separatorHeight;
        this.members[1].setLayoutMargin();
      }

      this.layout.showMember(this.members[1]);
    }

    if (valueField.pickList) {
      valueField.pickList.deselectAllRecords();
      valueField.pickList.clearLastHilite();
      valueField.pickList.scrollRecordIntoView(0);
    }
    valueField.setValue(null);
    valueField.setElementValue('', null);
  },

  click: function() {
    if (!this.showing) {
      if (!this.executingAction) {
        this.setExecutingAction();
      } else {
        // do nothing: action has just been launched, preventing it to be triggered
        // twice at the same time
        // see issue #25910
        return false;
      }
    }
    return this.Super('click', arguments);
  },

  // handle the case that someone entered a url in the quick launch
  doHide: function() {
    if (
      this.members[2].getField('value').getValue() &&
      this.members[2]
        .getField('value')
        .getValue()
        .contains('?')
    ) {
      var params = OB.Utilities.getUrlParameters(
        this.members[2].getField('value').getValue()
      );
      if (params && params.tabId) {
        OB.Utilities.openDirectTab(
          params.tabId,
          params.recordId,
          params.command,
          null,
          null,
          null,
          params
        );
      }
    }
    if (this.members[1].getMembers()) {
      this.members[1].destroyAndRemoveMembers(
        this.members[1].getMembers().duplicate()
      );
    }
    this.setExecutingAction();
    this.Super('doHide', arguments);
  },

  initWidget: function() {
    var dummyFirstField, dummyLastField, me;
    dummyFirstField = isc.OBFocusButton.create({
      getFocusTarget: function() {
        return this.parentElement.members[
          this.parentElement.members.length - 2
        ];
      }
    });

    dummyLastField = isc.OBFocusButton.create({
      getFocusTarget: function() {
        var firstFocusableItem;
        if (this.parentElement.members[1].members.length > 0) {
          firstFocusableItem = this.parentElement.members[1].members[0];
        } else {
          firstFocusableItem = this.parentElement.members[
            this.parentElement.members.length - 2
          ];
        }
        return firstFocusableItem;
      }
    });

    me = this;
    this.members = [
      dummyFirstField,
      isc.VLayout.create({
        // To allow height grow with its contents
        height: 1,
        visibility: 'hidden'
      }),
      isc.DynamicForm.create({
        autoFocus: true,
        width: '100%',
        titleSuffix: '',
        quickMenuWidget: me,
        fields: [
          {
            name: 'value',
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
            displayField: OB.Constants.TITLE,
            entries: [],

            getControlTableCSS: function() {
              // prevent extra width settings, super class
              // sets width to 0 on purpose
              return 'cursor:default;';
            },

            makePickList: function() {
              var quickMenu = this.containerWidget.quickMenuWidget;

              quickMenu.getQuickMenuItems(OB.Application.menu, this.entries);
              quickMenu.sortQuickMenuItems(this.entries);
              quickMenu.setQuickMenuValueMap(this);
              this.Super('makePickList', arguments);
            },

            getClientPickListData: function() {
              return this.entries;
            },

            selectOnFocus: true,
            textMatchStyle: 'substring',
            width: OB.Styles.OBFormField.DefaultComboBox.quickRunWidth,

            // client filtering does not always work great...
            pickListProperties: {
              textMatchStyle: 'substring',
              selectionType: 'single',
              bodyStyleName:
                OB.Styles.OBFormField.DefaultComboBox.pickListProperties
                  .bodyStyleName
            },
            pickListHeaderHeight: 0,

            // this is to prevent this issue:
            // http://forums.isomorphic.com/showthread.php?t=17949&goto=newpost
            autoSizePickList: false,

            getPickListFilterCriteria: function() {
              // only filter on identifier, ignoring accents
              var criteria = {};
              criteria[OB.Constants.IDENTIFIER] = OB.Utilities.removeAccents(
                this.getDisplayValue()
              );
              return criteria;
            },

            pickListFields: [
              {
                showValueIconOnly: true,
                name: 'icon',
                valueIcons: {
                  Process: this.nodeIcons.Process,
                  Report: this.nodeIcons.Report,
                  Form: this.nodeIcons.Form,
                  Window: this.nodeIcons.Window,
                  ExternalLink: this.nodeIcons.ExternalLink
                }
              },
              {
                name: OB.Constants.IDENTIFIER,
                displayField: OB.Constants.TITLE,
                valueField: OB.Constants.ID
              }
            ],
            autoFetchData: true,
            titleOrientation: 'top',
            title: OB.I18N.getLabel(this.titleLabel),
            editorType: 'comboBox',

            // local filtering enabled, remove the Id filter
            // explicitly from the criteria list, see getPickListFilter
            filterLocally: true,
            fetchDelay: 50,

            valueField: OB.Constants.ID,
            emptyPickListMessage: OB.I18N.getLabel(
              'OBUISC_ListGrid.emptyMessage'
            ),

            command: this.command,

            pickValue: function(theValue) {
              var record;
              this.Super('pickValue', arguments);
              record = this.getPickListRecordForValue(theValue);
              if (record) {
                var viewValue = record.viewValue;
                isc.OBQuickRun.currentQuickRun.doHide();
                var openObject = isc.addProperties({}, record);
                if (record.optionType && record.optionType === 'tab') {
                  openObject = OB.Utilities.openView(
                    record.windowId,
                    viewValue,
                    record[OB.Constants.TITLE],
                    null,
                    this.command,
                    record.icon,
                    record.readOnly,
                    record.singleRecord,
                    null,
                    record.editOrDeleteOnly
                  );
                  if (openObject) {
                    OB.RecentUtilities.addRecent(
                      this.recentPropertyName,
                      openObject
                    );
                  }
                  return;
                } else if (
                  record.optionType &&
                  record.optionType === 'external'
                ) {
                  openObject = {
                    viewId: 'OBExternalPage',
                    id: viewValue,
                    contentsURL: viewValue,
                    tabTitle: record[OB.Constants.TITLE]
                  };
                } else if (
                  record.optionType &&
                  record.optionType === 'process'
                ) {
                  var viewName = record.modal
                    ? 'OBClassicPopupModal'
                    : 'OBPopupClassicWindow';
                  openObject = {
                    viewId: viewName,
                    processId: record.processId,
                    id: record.processId,
                    obManualURL: viewValue,
                    popup: true,
                    command: 'BUTTON' + record.processId,
                    tabTitle: record[OB.Constants.TITLE]
                  };
                } else if (
                  record.optionType &&
                  record.optionType === 'processManual'
                ) {
                  openObject = {
                    viewId: 'OBClassicWindow',
                    processId: record.processId,
                    id: record.processId,
                    obManualURL: viewValue,
                    command: 'DEFAULT',
                    tabTitle: record[OB.Constants.TITLE]
                  };
                } else if (
                  record.optionType &&
                  record.optionType === 'processDefinition'
                ) {
                  openObject = {
                    viewId: 'processDefinition_' + record.processId,
                    tabTitle: record[OB.Constants.TITLE]
                  };
                } else if (record.viewId) {
                  openObject = record;
                } else if (record.formId) {
                  openObject = {
                    viewId: 'OBClassicWindow',
                    formId: record.formId,
                    id: viewValue,
                    obManualURL: viewValue,
                    command: this.command,
                    tabTitle: record[OB.Constants.TITLE]
                  };
                } else {
                  openObject = {
                    viewId: 'OBClassicWindow',
                    id: viewValue,
                    obManualURL: viewValue,
                    command: this.command,
                    tabTitle: record[OB.Constants.TITLE]
                  };
                }
                openObject.singleRecord = record.singleRecord;
                openObject.readOnly = record.readOnly;

                openObject.icon = record.icon;
                openObject = isc.addProperties({}, record, openObject);

                if (
                  openObject.openLinkInBrowser &&
                  openObject.viewId === 'OBExternalPage'
                ) {
                  if (openObject.contentsURL.indexOf('://') === -1) {
                    openObject.contentsURL = 'http://' + openObject.contentsURL;
                  }
                  OB.ViewManager.recentManager.addRecent(
                    'OBUIAPP_RecentViewList',
                    isc.addProperties(
                      {
                        icon: OB.Styles.OBApplicationMenu.Icons.externalLink
                      },
                      openObject
                    )
                  );
                  window.open(openObject.contentsURL);
                } else {
                  OB.Layout.ViewManager.openView(openObject.viewId, openObject);
                }

                OB.RecentUtilities.addRecent(
                  this.recentPropertyName,
                  openObject
                );

                this.setValue(null);
              }
            },

            handleKeyPress: function() {
              var result = this.Super('handleKeyPress', arguments);

              var key = isc.EH.lastEvent.keyName;
              if (key === 'Escape' || key === 'Enter') {
                if (isc.OBQuickRun.currentQuickRun) {
                  isc.OBQuickRun.currentQuickRun.doHide();
                }
              }
              return result;
            }
          }
        ]
      }),
      dummyLastField
    ];

    var ret = this.Super('initWidget', arguments);

    // register the field in the registry
    var suggestionField = this.members[2].getField('value');
    OB.TestRegistry.register(
      this.recentPropertyName + '_RECENTFORM',
      this.members[1]
    );
    OB.TestRegistry.register(
      this.recentPropertyName + '_FORM',
      this.members[2]
    );
    OB.TestRegistry.register(this.recentPropertyName + '_BUTTON', this);
    OB.TestRegistry.register(
      this.recentPropertyName + '_FIELD',
      suggestionField
    );

    return ret;
  },

  getQuickMenuItems: function(menu, quickMenu) {
    var i, menuItem, validMenuItem;
    for (i = 0; i < menu.length; i++) {
      menuItem = menu[i];
      if (menuItem.submenu) {
        this.getQuickMenuItems(menuItem.submenu, quickMenu);
      } else if (this.isValidMenuItem(menuItem)) {
        validMenuItem = isc.clone(menuItem);
        validMenuItem._identifier = OB.Utilities.removeAccents(
          validMenuItem.title
        );
        validMenuItem.icon = this.getMenuItemIcon(validMenuItem);
        quickMenu.add(validMenuItem);
      }
    }
  },

  getMenuItemIcon: function(menuItem) {
    if (menuItem.type === 'process' || menuItem.type === 'processManual') {
      return 'Process';
    } else if (menuItem.type === 'processDefinition') {
      if (menuItem.uiPattern === 'OBUIAPP_Report') {
        return 'Report';
      } else {
        return 'Process';
      }
    } else if (menuItem.type === 'report') {
      return 'Report';
    } else if (menuItem.type === 'form') {
      return 'Form';
    } else if (menuItem.type === 'external') {
      return 'ExternalLink';
    }
    return 'Window';
  },

  isValidMenuItem: function(menuItem) {
    return false;
  },

  sortQuickMenuItems: function(menuItems) {
    menuItems.sort(function(a, b) {
      return (a._identifier > b._identifier) - (a._identifier < b._identifier);
    });
  },

  isFolder: function(menuItem) {
    return menuItem.type === 'folder';
  },

  isWindowAndCanCreateNewRecord: function(menuItem) {
    return (
      menuItem.type === 'window' &&
      !menuItem.readOnly &&
      !menuItem.singleRecord &&
      !menuItem.editOrDeleteOnly
    );
  },

  setQuickMenuValueMap: function(quickMenuCombo) {
    var i,
      menuEntry,
      menuEntries = isc.clone(quickMenuCombo.entries),
      valueMap = {};

    if (!quickMenuCombo.setValueMap) {
      return;
    }

    for (i = 0; i < menuEntries.length; i++) {
      menuEntry = menuEntries[i];
      valueMap[menuEntry.id] = menuEntry.title;
    }
    quickMenuCombo.preventPickListRequest = true; // preventing 1st request triggered by setValueMap
    quickMenuCombo.setValueMap(valueMap);

    if (quickMenuCombo.pickList) {
      quickMenuCombo.pickList.data = menuEntries;
      quickMenuCombo.pickList.data.initialData = menuEntries;
      quickMenuCombo.pickList.data.allRows = menuEntries;
      quickMenuCombo.pickList.data.fetchMode = 'local';
      quickMenuCombo.pickList.data.useClientFiltering = true;
      quickMenuCombo.pickList.data.useClientSorting = true;
      quickMenuCombo.pickList.data.disableCacheSync = true;
      quickMenuCombo.pickList.data.neverDropCache = true;
    }
  }
});
