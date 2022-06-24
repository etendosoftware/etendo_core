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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.ClassFactory.defineClass('OBHelpAboutLinkButton', isc.Button);

isc.ClassFactory.defineClass('OBHelpAbout', isc.OBQuickRun);

// = OB Help About =
// Provides the help/about widget in the navigation bar. It displays two
// links: about and help. The help link will only be
// displayed if the current selected window has a help view.
isc.OBHelpAbout.addProperties({
  showInPortal: false,

  layoutProperties: {},

  title: OB.I18N.getLabel('UINAVBA_Help'),

  // Set to empty to prevent an icon from being displayed on the button.
  src: '',
  aboutLink: null,
  helpLink: null,
  dummyFirstField: null,
  dummyLastField: null,

  showTitle: true,

  initWidget: function() {
    OB.TestRegistry.register(
      'org.openbravo.client.application.HelpAboutWidget',
      this
    );
    this.Super('initWidget', arguments);
  },

  doShow: function() {
    this.Super('doShow', arguments);
    var me = this,
      focusInFirstHelpItem;
    focusInFirstHelpItem = function() {
      if (me.members[0].members[1]) {
        me.members[0].members[1].focus();
      }
      if (isc.EH.getFocusCanvas() === me.members[0].members[1]) {
        // Sometimes the focus is not positioned in the previous step
        return;
      } else {
        setTimeout(function() {
          focusInFirstHelpItem();
        }, 10);
      }
    };
    focusInFirstHelpItem();
  },

  beforeShow: function() {
    // determine if the help should be displayed or not
    var tabPane = null,
      helpView = null;
    this.dummyFirstField = isc.OBFocusButton.create({
      getFocusTarget: function() {
        return isc.OBQuickRun.currentQuickRun.members[0].members[
          isc.OBQuickRun.currentQuickRun.members[0].getMembers().length - 2
        ];
      }
    });

    this.dummyLastField = isc.OBFocusButton.create({
      getFocusTarget: function() {
        return isc.OBQuickRun.currentQuickRun.members[0].members[1];
      }
    });

    this.aboutLink = isc.OBHelpAboutLinkButton.create({
      name: 'aboutLink',
      title: OB.I18N.getLabel('UINAVBA_About'),
      keyPress: function() {
        var key = isc.EventHandler.getKey();
        if (key === 'Escape') {
          if (isc.OBQuickRun.currentQuickRun) {
            isc.OBQuickRun.currentQuickRun.doHide();
          }
        }
        return true;
      },
      action: function() {
        isc.OBQuickRun.hide();
        OB.Layout.ClassicOBCompatibility.Popup.open(
          'About',
          620,
          500,
          OB.Application.contextUrl + 'ad_forms/about.html',
          '',
          window
        );
      }
    });

    // use null to be sure it is null for selenium test code
    this.helpLink = null;
    // remove from the testregistry also
    OB.TestRegistry.register(
      'org.openbravo.client.application.HelpAbout.HelpLink',
      null
    );

    // get the selected tab
    var selectedTab = OB.MainView.TabSet.getSelectedTab();
    if (selectedTab && selectedTab.pane && selectedTab.pane.getHelpView) {
      tabPane = selectedTab.pane;
    }
    // determine if a help link should be shown or not
    // destroy the current members
    if (this.members[0].getMembers()) {
      this.members[0].destroyAndRemoveMembers(
        this.members[0].getMembers().duplicate()
      );
    }
    if (!tabPane) {
      this.members[0].addMembers([this.aboutLink]);
    } else {
      helpView = tabPane.getHelpView();
      if (!helpView) {
        this.members[0].addMembers([this.aboutLink]);
      } else {
        this.helpLink = isc.OBHelpAboutLinkButton.create({
          name: 'helpLink',
          title: OB.I18N.getLabel('UINAVBA_Help'),
          keyPress: function() {
            var key = isc.EventHandler.getKey();
            if (key === 'Escape') {
              if (isc.OBQuickRun.currentQuickRun) {
                isc.OBQuickRun.currentQuickRun.doHide();
              }
            }
            return true;
          },
          action: function() {
            isc.OBQuickRun.hide();
            OB.Layout.ViewManager.openView(helpView.viewId, helpView);
          }
        });
        OB.TestRegistry.register(
          'org.openbravo.client.application.HelpAbout.HelpLink',
          this.helpLink
        );

        this.members[0].addMembers([this.helpLink, this.aboutLink]);
      }
    }
    this.members[0].addMembers(this.dummyFirstField, 0);
    this.members[0].addMembers(
      this.dummyLastField,
      this.members[0].getMembers().length
    );
    OB.TestRegistry.register(
      'org.openbravo.client.application.HelpAbout.AboutLink',
      this.aboutLink
    );
  },

  doHide: function() {
    if (this.aboutLink) {
      this.aboutLink.destroy();
      this.aboutLink = null;
      this.members[0].destroyAndRemoveMembers(this.aboutLink);
    }
    if (this.helpLink) {
      this.helpLink.destroy();
      this.helpLink = null;
      this.members[0].destroyAndRemoveMembers(this.helpLink);
    }
    if (this.dummyFirstField) {
      this.dummyFirstField.destroy();
      this.dummyFirstField = null;
    }
    if (this.dummyLastField) {
      this.dummyLastField.destroy();
      this.dummyLastField = null;
    }
    this.Super('doHide', arguments);
  },

  members: [
    isc.VLayout.create({
      height: 1,
      initWidget: function() {
        OB.TestRegistry.register(
          'org.openbravo.client.application.HelpAbout',
          this
        );
        this.Super('initWidget', arguments);
      }
    })
  ],

  keyboardShortcutId: 'NavBar_OBHelpAbout'
});
