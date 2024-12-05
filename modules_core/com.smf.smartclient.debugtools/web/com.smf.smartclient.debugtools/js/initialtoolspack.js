class OnOffPlugin {
  constructor() {
    this.type = "ERP";
  }

  menuItems = [
    {
      dynamicTitle: () => OB.DebugToolsManager.state.enabled ? OB.I18N.getLabel('SMFSCDT_DisableDebugTools') : OB.I18N.getLabel('SMFSCDT_EnableDebugTools'),
      dynamicIcon: () => '../web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/smartclient/images/actions/' + (OB.DebugToolsManager.state.enabled ? 'remove' : 'accept') + '.png',
      click: () => OB.DebugToolsManager.setState({ enabled: !OB.DebugToolsManager.state.enabled })
    }
  ]

  onUpdate = (state, prevState) => {
    //update debug mode button state
    if (prevState.enabled === undefined && state.enabled !== undefined) {
      prevState = { ...prevState, enabled: !state.enabled }
    }
    if (!prevState.enabled && state.enabled) {
      this.setEnabled(true);
    } else if (prevState.enabled && !state.enabled) {
      this.setEnabled(false);
    }
  }

  setEnabled = (enabled) => {
    if (enabled) {
      localStorage.setItem('SMFSCDT.DebugON', true);
      OB.DebugToolsManager.btnMenu.show();
    } else {
      localStorage.setItem('SMFSCDT.DebugON', false);
      OB.DebugToolsManager.btnMenu.hide();
    }
  }

  init = (state) => {
    var egg = new window.Egg();
    egg.addCode("d,e,b,u,g,m,o,d,e,o,n", function () {
      OB.DebugToolsManager.setState({ enabled: true });
      console.log(`Debug tools enabled, to disable write "debugmodeoff"`);
    }).listen();

    egg.addCode("d,e,b,u,g,m,o,d,e,o,f,f", function () {
      OB.DebugToolsManager.setState({ enabled: false });
      console.log(`Debug tools disabled, to enable write "debugmodeon"`);
    }).listen();

    let debugFeaturesEnabled = OB.PropertyStore.get('SMFSCDT_EnableDebug');
    let enabled = debugFeaturesEnabled === 'Y' || debugFeaturesEnabled === '"Y"';

    OB.DebugToolsManager.setState({ enabled });
  }
}

class DarkModePlugin {
  constructor() {
    this.type = "ERP";
  }

  menuItems = [
    {
      title: OB.I18N.getLabel('SMFSCDT_DarkMode'),
      dynamicIcon: () => '../web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/smartclient/images/DynamicForm/' + (OB.DebugToolsManager.state.dark ? 'checked' : 'unchecked') + '.png',
      click: () => OB.DebugToolsManager.setState({ dark: !OB.DebugToolsManager.state.dark }),
      enableIf: () => OB.DebugToolsManager.state.enabled
    }
  ]

  onUpdate = (state, prevState) => {
    //update debug mode button state
    if (prevState.enabled && !state.enabled) {
      OB.DebugToolsManager.setState({ dark: false });
      return;
    }

    //update darkmode
    if (prevState.dark === undefined && state.dark !== undefined) {
      prevState = { dark: !state.dark }
    }
    if (!prevState.dark && state.dark) {
      this.setDarkMode(true);
    } else if (prevState.dark && !state.dark) {
      this.setDarkMode(false);
    }
  }

  setDarkMode = (enabled) => {
    let html = document.querySelector("html");
    if (enabled) {
      html.style.filter = "invert(1) hue-rotate(179deg) contrast(0.9) brightness(1.4)";
      html.style.backgroundColor = "white";
      localStorage.setItem('SMFSCDT.DarkModeON', true);
    } else {
      html.style.filter = "";
      html.style.backgroundColor = "";
      localStorage.setItem('SMFSCDT.DarkModeON', false);
    }
  }
  init = (state) => {
    //Enable/Disable dark mode by localstorage state
    let DMenable = localStorage.getItem('SMFSCDT.DarkModeON') === "true";
    OB.DebugToolsManager.setState({ dark: DMenable });
  }
}


class InputUtilsPlugin {
  constructor() {
    this.type = "ERP";
  }

  menuItems = [
    {
      title: OB.I18N.getLabel('SMFSCDT_FormFeatures'),
      dynamicIcon: () => '../web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/smartclient/images/DynamicForm/' + (OB.DebugToolsManager.state.inputFeatures ? 'checked' : 'unchecked') + '.png',
      click: () => OB.DebugToolsManager.setState({ inputFeatures: !OB.DebugToolsManager.state.inputFeatures }),
      enableIf: () => OB.DebugToolsManager.state.enabled
    }
  ]

  onUpdate = (state, prevState) => {
    //update state
    if (prevState.inputFeatures === undefined && state.inputFeatures !== undefined) {
      prevState = { inputFeatures: !state.inputFeatures }
    }
    if (!prevState.inputFeatures && state.inputFeatures) {
      this.setEnabled(true);
    } else if (prevState.inputFeatures && !state.inputFeatures) {
      this.setEnabled(false);
    }
  }

  setEnabled = (enabled) => {
    if (enabled) {
      OBViewGrid.getPrototype().canMultiSort = true;
      localStorage.setItem('SMFSCDT.InputFeaturesON', true);
    } else {
      OBViewGrid.getPrototype().canMultiSort = false;
      localStorage.setItem('SMFSCDT.InputFeaturesON', false);
    }
  }

  copyToClipboard = (str) => {
    const el = document.createElement("textarea");
    el.value = str;
    document.body.appendChild(el);
    el.select();
    document.execCommand("copy");
    document.body.removeChild(el);
  }

  init = () => {
    //Enable/Disable dark mode by localstorage state
    let enabled = localStorage.getItem('SMFSCDT.InputFeaturesON') === "false" ? false : true;
    OB.DebugToolsManager.setState({ inputFeatures: enabled });

    // Prepare contextual menu
    let self = this;
    let classList = [isc.FormItem, isc.OBCheckboxItem];
    
    // Helper function to check if debug tools are enabled
    const areDebugToolsEnabled = () => OB?.DebugToolsManager?.state?.enabled;

    // Are input features enabled
    const areInputFeaturesEnabled = () => OB?.DebugToolsManager?.state?.inputFeatures;

    // Helper function to check if the Alt key is pressed
    const isAltKeyPressed = (e) => e.code === 'AltLeft';

    // Helper function to toggle attributes on elements
    const toggleAttributes = (elements, removeAttr, addAttr, addAttrValue) => {
      elements.forEach(el => {
        if (el.hasAttribute(removeAttr)) {
          el.removeAttribute(removeAttr);
          el.setAttribute(addAttr, addAttrValue);
        }
      });
    };

    // Helper function to get all disabled elements
    const getDisabledElements = () => {
      return document.querySelectorAll(
        '[class*="OBFormFieldInputDisabled"], [class*="OBFormFieldDateInputRequiredDisabled"], [class*="OBFormFieldSelectInputRequiredDisabled"]'
      );
    };

    // Event listener for Alt key down
    document.addEventListener('keydown', (e) => {
      try {
        if (!areDebugToolsEnabled() || !areInputFeaturesEnabled || !isAltKeyPressed(e)) {
          return;
        }
        const disabledElements = getDisabledElements();
        toggleAttributes(disabledElements, 'disabled', 'readonly', true);
      } catch (error) {
        console.error(error);
      }
    });

    // Event listener for Alt key up
    document.addEventListener('keyup', (e) => {
      try {
        if (!areDebugToolsEnabled() || !areInputFeaturesEnabled || !isAltKeyPressed(e)) {
          return;
        }
        const disabledElements = getDisabledElements();
        toggleAttributes(disabledElements, 'readonly', 'disabled', true);
      } catch (error) {
        console.error(error);
      }
    });

    classList.forEach(classType => {
      classType.getPrototype().OhandleClick = classType.getPrototype().handleClick;
      classType.getPrototype().handleClick = function () {
        let item = this;
        if (OB.DebugToolsManager.state.enabled && OB.DebugToolsManager.state.inputFeatures && OB.Utilities.Pkeys[18]) {
          let itemValue = item.getValue ? item.getValue() : null;
          self.menu = isc.Menu.create({
            items: [
              {
                title: OB.I18N.getLabel('SMFSCDT_Copy') + " name: <b>" + item.name + "</b>", click: () => {
                  self.copyToClipboard(item.name);
                },
                icon: '../web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/smartclient/images/RichTextEditor/copy.png',
                enableIf: () => item.name !== null && item.name !== undefined
              },
              {
                title: OB.I18N.getLabel('SMFSCDT_Copy') + " columnName: <b>" + item.columnName + "</b>", click: () => {
                  self.copyToClipboard(item.columnName);
                },
                icon: '../web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/smartclient/images/RichTextEditor/copy.png',
                enableIf: () => item.columnName !== null && item.columnName !== undefined
              },
              {
                title: OB.I18N.getLabel('SMFSCDT_Copy') + " valor: <b>" + itemValue + "</b>", click: () => {
                  let value = itemValue;
                  if (value instanceof Date) {
                    value = new Date().toISOString().substr(0, 10)
                  }
                  self.copyToClipboard(value);
                },
                icon: '../web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/smartclient/images/RichTextEditor/copy.png',
                enableIf: () => itemValue !== null && itemValue !== undefined
              },
              {
                dynamicTitle: () => item.isDisabled && item.isDisabled() ? OB.I18N.getLabel('SMFSCDT_Enable') : OB.I18N.getLabel('SMFSCDT_Disable'),
                click: () => {
                  item.setDisabled(!item.isDisabled());
                },
                dynamicIcon: () => '../web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/smartclient/images/actions/' + (item.isDisabled() ? 'accept' : 'remove') + '.png'
              }, {
                dynamicTitle: () => item.isDisabled() ? OB.I18N.getLabel('SMFSCDT_ForceEnable') : OB.I18N.getLabel('SMFSCDT_ForceDisable'),
                click: () => {
                  let disable = item.isDisabled ? !item.isDisabled() : false;
                  item.isDisabled = () => disable;
                  item.setDisabled(disable);
                  item.redraw();
                },
                dynamicIcon: () => '../web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/smartclient/images/actions/' + (item.isDisabled() ? 'accept' : 'remove') + '.png'
              }, {
                title: "Ejecutar callout", click: () => {
                  item.form.doChangeFICCall(item);
                },
                icon: '../web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/smartclient/images/actions/forward.png',
              },
            ]
          });
          self.menu.showContextMenu();
        } else {
          this.OhandleClick(...arguments);
        }
      }
    });
  }
}

class AccentProdPlugin {
  constructor() {
    this.type = "ERP";
  }

  menuItems = [
    {
      title: OB.I18N.getLabel('SMFSCDT_HighlightProdEnv'),
      dynamicIcon: () => '../web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/smartclient/images/DynamicForm/' + (OB.DebugToolsManager.state.prodAccent ? 'checked' : 'unchecked') + '.png',
      click: () => OB.DebugToolsManager.setState({ prodAccent: !OB.DebugToolsManager.state.prodAccent }),
      enableIf: () => OB.DebugToolsManager.state.enabled
    }
  ]

  onUpdate = (state) => {
    this.senEnabled(state.enabled && state.prodAccent);
  }

  senEnabled = (enabled) => {
    let html = document.querySelector("html");
    if (enabled) {
      if (OB.Application.purpose === "P") {
        //fix
        setTimeout(() => {
          html.style.filter = "grayscale(1) brightness(0.7) sepia(100%) hue-rotate(-50deg) saturate(2.7) contrast(2.3)";
          html.style.backgroundColor = "white";
        });
      }
      localStorage.setItem('SMFSCDT.AccentProdON', true);
    } else {
      html.style.filter = "";
      html.style.backgroundColor = "";
      localStorage.setItem('SMFSCDT.AccentProdON', false);
    }
  }
  init = (state) => {
    //Enable/Disable prodAccent mode by localstorage state
    let DMenable = localStorage.getItem('SMFSCDT.AccentProdON') !== "false" ? true : false;
    OB.DebugToolsManager.setState({ prodAccent: DMenable });
  }
}

class RemoveFiltersPlugin {
  constructor() {
    this.type = "ERP";
  }

  menuItems = []

  onUpdate = () => { }

  init = (state) => {
    isc.OBViewDataSource.getPrototype().ogetTabInfoRequestProperties = isc.OBViewDataSource.getPrototype().getTabInfoRequestProperties;
    isc.OBViewDataSource.getPrototype().getTabInfoRequestProperties = function (a, b) {
      if (OB.DebugToolsManager.state.rmFilters) {
        return b;
      }
      return isc.OBViewDataSource.getPrototype().ogetTabInfoRequestProperties(...arguments);
    }
    OB.DebugToolsManager.setState({ rmFilters: false });
    let buttonProps = {
      action: function () {
        OB.DebugToolsManager.setState({ rmFilters: !OB.DebugToolsManager.state.rmFilters });
        this.updateState();
      },
      buttonType: 'rmFilters',
      prompt: OB.I18N.getLabel('SMFSCDT_ToggleWindowFilter'),
      updateState: function () {
        if (OB.DebugToolsManager.state.rmFilters) {
          this.prompt = OB.I18N.getLabel('SMFSCDT_EnableWindowFilter');
          this.setBaseStyle("OBToolbarIconButton_icon_rmFilters OBToolbarIconButtonOver a");
        } else {
          this.prompt = OB.I18N.getLabel('SMFSCDT_DisableWindowFilter');
          this.setBaseStyle("OBToolbarIconButton_icon_rmFilters OBToolbarIconButton");
        }
      }
    };
    let debugFeaturesEnabled = OB.PropertyStore.get('SMFSCDT_EnableDebug');
    if (debugFeaturesEnabled === 'Y' || debugFeaturesEnabled === '"Y"') {
      OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 800);
    }
  }
}

class ShowFieldsPlugin {
  constructor() {
    this.type = "ERP";
  }

  menuItems = []

  onUpdate = () => { }

  init = (state) => {
    let buttonProps = {
      action: function () {
        let form = this.view.viewForm;
        Object.keys(form.dataSource.fields).forEach((key) => {
          let field = form.fields.find(e => e.name === key);
          if (!field) {
            let dsField = form.dataSource.fields[key];
            if (dsField.editorType) {
              form.addField(dsField);
            }
          }
          field = form.fields.find(e => e.name === key);
          if (field && field.show) field.show();
        });

      },
      buttonType: 'showFields',
      prompt: OB.I18N.getLabel('SMFSCDT_ShowHiddenFields'),
      updateState: function () {
        let view = this.view
        if (view.isShowingForm) {
          this.setDisabled(false);
        } else {
          this.setDisabled(true);
        }
      }
    };

    let debugFeaturesEnabled = OB.PropertyStore.get('SMFSCDT_EnableDebug');
    if (debugFeaturesEnabled === 'Y' || debugFeaturesEnabled === '"Y"') {
      OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 800);
    }
  }
}

class ShowGridId {
  constructor() {
    this.type = "ERP";
  }

  menuItems = []

  onUpdate = () => { }

  init = (state) => {
    let buttonProps = {
      action: function () {
        let grid = this.view.viewGrid;
        grid.dataSource.fields.id.width = 200;
        if (!grid.fields.some(e => e.name === "id")) {
          grid.addField(grid.dataSource.fields.id, 0);
        } else {
          grid.removeField(grid.dataSource.fields.id);
        }
      },
      buttonType: 'showGridId',
      prompt: OB.I18N.getLabel('SMFSCDT_ShowIdInGrid'),
      updateState: function () {
        let view = this.view
        if (view.isShowingForm) {
          this.setDisabled(true);
        } else {
          this.setDisabled(false);
        }
      }
    };

    let debugFeaturesEnabled = OB.PropertyStore.get('SMFSCDT_EnableDebug');
    if (debugFeaturesEnabled === 'Y' || debugFeaturesEnabled === '"Y"') {
      OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 800);
    }
  }
}

OB.DebugTools.push(new OnOffPlugin());
OB.DebugTools.push(new InputUtilsPlugin());
OB.DebugTools.push(new AccentProdPlugin());
OB.DebugTools.push(new RemoveFiltersPlugin());
OB.DebugTools.push(new ShowFieldsPlugin());
OB.DebugTools.push(new ShowGridId());
OB.DebugTools.push(new DarkModePlugin());
