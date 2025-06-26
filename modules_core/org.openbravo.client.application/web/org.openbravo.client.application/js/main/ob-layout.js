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
 * All portions are Copyright (C) 2017-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.ClassFactory.defineClass('OBLayout', isc.Layout);

// make sure that the layout is loaded in the parent window if we accidentally end up
// in a child frame
try {
  if (window.parent && window.parent.OB && window.parent.OB.Layout) {
    isc.Log.logDebug('Reloading in parent frame', 'OB');
    window.parent.location.href = window.location.href;
  } else if (
    window.parent.parent &&
    window.parent.parent.OB &&
    window.parent.parent.OB.Layout
  ) {
    isc.Log.logDebug('Reloading in parent.parent frame', 'OB');
    window.parent.parent.location.href = window.location.href;
  } else {
    isc.Log.logDebug('loading in own frame', 'OB');
  }
} catch (e) {
  // ignoring on purpose
  isc.Log.logDebug('Error when checking parent frame: ' + e.message, 'OB');
}

// This preference is the one which enables the accessibility for the people with reduced visual capabilities.
if (OB.PropertyStore.get('EnableScreenReader') === 'Y') {
  isc.screenReader = true;
}

isc.Canvas.addClassProperties({
  neverUsePNGWorkaround: true
});

OB.KeyboardManager.Shortcuts.setPredefinedList('OBUIAPP_KeyboardShortcuts');
OB.KeyboardManager.Shortcuts.setPredefinedList('UINAVBA_KeyboardShortcuts');

// the OB.Layout contains everything
OB.Layout = isc.VLayout.create({
  width: '100%',
  height: '100%',
  overflow: 'hidden'
});

// initialize the content of the layout, as late as possible
// is called from index.jsp
OB.Layout.initialize = function() {
  // create the bar with navigation components
  OB.NavBar = isc.ToolStrip.create(
    {
      addMembers: function(members) {
        // encapsulate the members
        var newMembers = [],
          i;
        for (i = 0; i < members.length; i++) {
          // encapsulate in 2 hlayouts to handle correct mouse over/hover and show of box
          if (OB.User.isPortal && !members[i].showInPortal) {
            continue;
          }
          var newMember = isc.HLayout.create({
            layoutLeftMargin: 0,
            layoutRightMargin: 0,
            width: '100%',
            height: '100%',
            styleName: 'OBNavBarComponent',
            members: [members[i]]
          });
          newMembers[i] = newMember;
        }
        // note the array has to be placed in an array otherwise the newMembers
        // is considered to the argument list
        this.Super('addMembers', [newMembers]);
      },

      createMembers: function(allMembers) {
        var members = [],
          dynamicMembers = [],
          i,
          j = 0;
        if (!allMembers) {
          return;
        }
        if (OB.Application.dynamicNavigationBarComponents) {
          dynamicMembers = OB.Application.dynamicNavigationBarComponents();
        }
        for (i = 0; i < allMembers.length; i++) {
          if (!allMembers[i].className) {
            continue;
          }
          if (allMembers[i].className !== '_OBNavBarDynamicComponent') {
            this.translateLabels(allMembers[i]);
            members.push(
              isc.ClassFactory.newInstance(
                allMembers[i].className,
                allMembers[i].properties
              )
            );
          } else if (dynamicMembers && dynamicMembers[j]) {
            members.push(dynamicMembers[j]);
            j++;
          }
        }
        this.addMembers(members);
      },

      translateLabels: function(member) {
        if (!member.properties) {
          return;
        }
        if (member.properties.title) {
          member.properties.title = OB.I18N.getLabel(member.properties.title);
        }
        if (member.properties.itemPrompt) {
          member.properties.itemPrompt = OB.I18N.getLabel(
            member.properties.itemPrompt
          );
        }
      },

      isFirstDraw: true,

      draw: function() {
        this.Super('draw', arguments);
        if (isc.Browser.isIE && this.isFirstDraw) {
          this.isFirstDraw = false;
          this.markForRedraw(); //To solve issue https://issues.openbravo.com/view.php?id=18192 in IE
        }
      }
    },
    OB.Styles.TopLayout.NavBar
  );
  // the TopLayout has the navigation bar on the left and the logo on the right
  OB.TopLayout = isc.HLayout.create({}, OB.Styles.TopLayout);

  //create the navbar on the left and the logo on the right
  OB.TopLayout.CompanyImageLogo = isc.Img.create({
    width: OB.Application.companyImage.width,
    height: OB.Application.companyImage.height,
    src:
      OB.Application.contextUrl + 'utility/ShowImageLogo?logo=yourcompanymenu',
    imageType: 'normal'
  });

  OB.TestRegistry.register(
    'org.openbravo.client.application.companylogo',
    OB.TopLayout.CompanyImageLogo
  );

  OB.TopLayout.OpenbravoLogo = isc.Img.create({
    imageType: 'normal',
    imageWidth: '150',
    imageHeight: '42',
    src: OB.Application.contextUrl + 'web/images/PoweredByOpenbravo.svg',

    getInnerHTML: function() {
      var html = this.Super('getInnerHTML', arguments);
      if (!OB.Application.isActiveInstance) {
        return (
          '<a href="https://etendo.software/" target="_new">' +
          html +
          '</a>'
        );
      } else {
        return html;
      }
    }
  });
  OB.TestRegistry.register(
    'org.openbravo.client.application.openbravologo',
    OB.TopLayout.OpenbravoLogo
  );
  if (OB.Styles && OB.Styles.hideOpenbravoLogo) {
    OB.TopLayout.OpenbravoLogo.hide();
  }
  OB.TopLayout.addMember(
    isc.HLayout.create({}, OB.Styles.TopLayout.LeftSpacer)
  );
  OB.TopLayout.addMember(OB.NavBar);
  OB.TopLayout.addMember(
    isc.HLayout.create({}, OB.Styles.TopLayout.MiddleSpacer)
  );

  OB.TopLayout.addMember(
    isc.HLayout.create(
      {
        members: [OB.TopLayout.CompanyImageLogo, OB.TopLayout.OpenbravoLogo]
      },
      OB.Styles.TopLayout.LogosContainer
    )
  );
  //add the top part to the main layout
  OB.Layout.addMember(OB.TopLayout);
  OB.MainView = isc.VLayout.create({
    width: '100%',
    height: '100%'
  });
  OB.Layout.addMember(OB.MainView);

  OB.MainView.TabSet = isc.OBTabSetMain.create({});

  OB.MainView.addMember(OB.MainView.TabSet);

  OB.TestRegistry.register(
    'org.openbravo.client.application.mainview',
    OB.MainView
  );
  OB.TestRegistry.register(
    'org.openbravo.client.application.mainview.tabset',
    OB.MainView.TabSet
  );
  OB.TestRegistry.register(
    'org.openbravo.client.application.layout',
    OB.Layout
  );

  OB.NavBar.createMembers(OB.Application.navigationBarComponents);

  // show the heartbeat or registration popups (if it applies)
  OB.Application.showHeartBeatOrRegistration();

};
