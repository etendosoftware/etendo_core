//Wait to Execute
OB.Utilities.waitToExecute = (action, condition, time = 500) => {
  if (!action || !condition || !time) {
    throw new Error("All the parameters are required");
  }
  //if condition evaluates true, then execute the action, otherwise await the time established to check the condition again.
  if (condition()) {
    action();
  } else {
    setTimeout(() => OB.Utilities.waitToExecute(action, condition, time), time);
  }
};

// Pressed keys
OB.Utilities.Pkeys = {};

window.onkeyup = (e) => {
  OB.Utilities.Pkeys[e.keyCode] = false;
};
window.onkeydown = (e) => {
  OB.Utilities.Pkeys[e.keyCode] = true;
};
window.onblur = (e) => {
  OB.Utilities.Pkeys = [];
};

//multiview
OB.Utilities.waitToExecute(() => {
  let multiView = OB.Properties.SMFSCBU_MultiView === "Y";
  if (multiView) {
    //configure tabset 
    isc_OBTabSetMain_0_paneContainer.vertical = false;
    isc_OBTabSetMain_0_paneContainer.resizeBarClass = "DarkSnapbar"
    isc_OBTabSetMain_0.o$8c = isc_OBTabSetMain_0.$8c;
    isc_OBTabSetMain_0.$8c = function (_1) {
      let prevTab = this.getSelectedTab()
      isc_OBTabSetMain_0.o$8c(...arguments);
      //show prev tab
      if (OB.Utilities.Pkeys[18]) {
        prevTab.pane.show();
      }
    }
    OBTabBarButton.getPrototype().setSelected = function (_1) {
      let altPressed = !OB.Utilities.Pkeys[18];
      if (_1 && this.radioGroup != null) {
        var _2 = isc.StatefulCanvas.$1z[this.radioGroup];
        if (_2 == null) {
          this.logWarn("'radioGroup' property set for this widget, but no corresponding group exists. To set up a new radioGroup containing this widget, or add this  widget to an existing radioGroup at runtime, call 'addToRadioGroup(groupID)'")
        } else if (altPressed) {
          for (var i = 0; i < _2.length; i++) {
            if (_2[i] != this && _2[i].isSelected()) {
              _2[i].setSelected(false);
              _2[i].pane.hide();
              _2[i].pane.setShowResizeBar(false);
            }
          }
        }
      }

      this.selected = _1;
      if (this.label)
        this.label.setSelected(this.isSelected());
      if (this.selected) {
        this.pane.setWidth("100%");
        this.pane.show()
      }
      let visibleTabs = this.parentElement.members.filter(tab => tab.pane.isVisible());
      for (let i = 0; i < visibleTabs.length - 1; i++) {
        visibleTabs[i].pane.setShowResizeBar(true);
      }
      this.stateChanged()
    }

    //fix en createLoadingLayout
    OB.Utilities.createLoadingLayout = function (label) {
      var mainLayout = isc.HLayout.create({
        styleName: OB.Styles.LoadingPrompt.mainLayoutStyleName,
        height: "100%",
        align: "center",
        defaultLayoutAlign: "center"
      });
      var loadingLayout = isc.HLayout.create({
        align: "center",
        defaultLayoutAlign: "center",
        membersMargin: 0,
        overflow: "visible"
      });
      if (!label) {
        label = OB.I18N.getLabel("OBUIAPP_LOADING");
      }
      mainLayout.addMember(loadingLayout);
      loadingLayout.addMember(
        isc.Label.create({
          contents: label,
          styleName: OB.Styles.LoadingPrompt.loadingTextStyleName,
          width: 100,
          align: "right",
          overflow: "visible"
        })
      );
      loadingLayout.addMember(
        isc.Img.create(OB.Styles.LoadingPrompt.loadingImage)
      );
      return mainLayout;
    };
  }
}, () => !!window.isc_OBTabSetMain_0, 500);

//stylish snapbar
Snapbar.getPrototype().inito = Snapbar.getPrototype().init;
Snapbar.getPrototype().init = function () {
  this.canCollapse = false;
  this.inito(...arguments);
  this.className = "splitbar OBToolbar";
  this.baseStyle = "splitbar OBToolbar"
  this.label.icon = "";
  this.redraw();
};
isc.defineClass("DarkSnapbar", Snapbar);
DarkSnapbar.getPrototype().init = function () {
  this.canCollapse = false;
  this.className = "darksplitbar";
  this.baseStyle = "darksplitbar"
  this.inito(...arguments);
  this.label.icon = "";
  this.redraw();
};

// grid & form 
// Summary functions only consider selected records when there is more than one selected.
OBViewGrid.getPrototype().original_recalculateGridSummary = OBViewGrid.getPrototype().recalculateGridSummary;
OBViewGrid.getPrototype().recalculateGridSummary = function() {
  var selected = this.getSelectedRecords ? this.getSelectedRecords() : [];
  var subCriteria = [];

  if (selected.length > 1) {   
    for (var i = 0; i < selected.length; i++) {
       subCriteria.push({ 
        operator: "equals", 
        fieldName: "id", 
        value: selected[i].id
      });
    }

    this.summaryRowCriteria = { 
      _constructor: "AdvancedCriteria", 
      operator: "and", 
      criteria: [{ 
        operator: "or", 
        fieldName: "id", 
        criteria: subCriteria 
      }]
    };
  } else {
    this.summaryRowCriteria = null
  }

  this.original_recalculateGridSummary();
}

OBViewGrid.getPrototype().original_selectionUpdated = OBViewGrid.getPrototype().selectionUpdated;
OBViewGrid.getPrototype().selectionUpdated = function (record, recordList) {
  var refreshSummary = true;

  if (this.filterEditor && this.filterEditor.getEditForm()) {
        var fld = this.filterEditor
          .getEditForm()
          .getField(this.getCheckboxField().name);
        if (fld && recordList) {
          // do not refresh summary when selecting a single record
          // when previouslt there was only one selected.
          refreshSummary = !((fld.getValue() == 1 || fld.getValue() == "&nbsp;") && recordList.length <= 1)
        }
  }

  if (
      (!recordList || recordList.length === 1) &&
      record === this.lastSelectedRecord &&
      (this.lastSelectedRecord || record)
    ) {
      return;
    }


  this.original_selectionUpdated(record, recordList);

  if (refreshSummary) {
    this.recalculateGridSummary();
  }

}


//when clicking row in gridForm mode select record
OBViewGrid.getPrototype().orowClick = OBViewGrid.getPrototype().rowClick;
OBViewGrid.getPrototype().rowClick = function () {
  if (this.view.isShowingForm) {
    this.rowDoubleClick(...arguments);
  } else {
    this.orowClick(...arguments);
  }
}
OBTreeViewGrid.getPrototype().orowClick = OBTreeViewGrid.getPrototype().rowClick;
OBTreeViewGrid.getPrototype().rowClick = function () {
  if (this.view.isShowingForm) {
    this.rowDoubleClick(...arguments);
  } else {
    this.orowClick(...arguments);
  }
}


OB.Utilities.showGridAndForm = function (view) {
  const grid = view.isShowingTree ? view.treeGrid : view.viewGrid;
  grid.tabTitle = "";
  grid.setShowResizeBar(true);
  if (!view.isShowingForm) {
    if (!view.viewForm.getDataSource()) {
      view.prepareViewForm();
    } else {
      view.viewForm.updateAlwaysTakeSpaceInSections();
    }
    view.statusBarFormLayout.show();
    view.statusBarFormLayout.setHeight('100%');
    if (view.isActiveView()) {
      view.viewForm.focus();
    }
    view.isShowingForm = true;
    console.log(view);
    grid.recordDoubleClick(null,grid.getSelectedRecord())
  } else {
    grid.show();
    if (view.isActiveView()) {
      if (
        view.viewGrid.getSelectedRecords() &&
        view.viewGrid.getSelectedRecords().length === 1
      ) {
        view.viewGrid.focus();
      } else {
        view.viewGrid.focusInFirstFilterEditor();
      }
    }
    view.viewGrid.setHeight('100%');
  }
  view.updateTabTitle();
};

OBStandardView.getPrototype().oswitchFormGridVisibility = OBStandardView.getPrototype().switchFormGridVisibility;
OBStandardView.getPrototype().switchFormGridVisibility = function () {
  this.oswitchFormGridVisibility(...arguments);
  if(!this.isShowingForm && this.isShowingTree){
    this.treeGrid.setWidth("100%")
    this.treeGrid.setShowResizeBar(false);
  } else if(!this.isShowingForm){
    this.viewGrid.setWidth("100%")
    this.viewGrid.setShowResizeBar(false);
  }
}

let buttonProps = {
  action: function () {
    OB.Utilities.showGridAndForm(this.view);
  },
  buttonType: 'gridAndFilter',
  prompt: 'Show table and form',
  updateState: function () {}
};
OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 700);