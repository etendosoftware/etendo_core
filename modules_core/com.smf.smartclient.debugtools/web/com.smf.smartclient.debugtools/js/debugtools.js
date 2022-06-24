class DebugToolsManager {
  setState = (object) => {
    let prevState = this.state;
    this.state = { ...this.state, ...object };
    this.onUpdate(this.state, prevState);
  }

  onUpdate(state, prevState) {
    this[this.envType + "Plugins"].forEach(plugin => plugin.onUpdate(state, prevState));
  }

  constructor() {
    this.OB = window.OB;
    this.state = {};
    this.envType = this.OB && this.OB.Application ? "ERP" : (this.OB.MobileApp ? "POS" : "");
    this[this.envType + "Plugins"] = [];
    isc.defineClass("DebugMenu", isc.NavigationButton);
    DebugMenu.setProperties({
      title: "Debug",
      textColor: "black",
      width: 80,
      noDoubleClicks: true,
      contextMenu: isc.Menu.create({
        items: []
      }),
      action: function () {
        this.contextMenu.showContextMenu();
      }
    });
    OB.NavBar.createMembers([
      {
        className: "DebugMenu"
      }
    ]);
    this.btnMenu = OB.NavBar.members[OB.NavBar.members.length - 1].members[0];
  }

  loadPlugin(plugin) {
    this[plugin.type + "Plugins"] = this[plugin.type + "Plugins"] || [];
    this[plugin.type + "Plugins"].push(plugin);
    plugin.init(this.state);
    let menuItems = plugin.menuItems || [];
    this.btnMenu.contextMenu.items.push(...menuItems);
  }
}
OB.DebugTools = [];
//wait for smartclient ui to initialize
OB.Utilities.waitToExecute(function () {
  OB.DebugToolsManager = new DebugToolsManager();
  OB.DebugTools.forEach((tool) => {
    OB.DebugToolsManager.loadPlugin(tool);
  });
}, () => !!window.isc_OBTabSetMain_0, 500);
