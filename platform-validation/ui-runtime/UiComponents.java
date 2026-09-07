package com.etendoerp.platform.ui;

import java.util.HashMap;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.enterprise.context.control.RequestContextController;
import org.openbravo.client.application.MenuManager;
import org.openbravo.client.application.window.StandardWindowComponent;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.dal.core.OBContext;

/** Optional CDI composition of canonical UI components, with no widget implementations. */
final class UiComponents implements AutoCloseable {
  private final SeContainer container;

  UiComponents() throws ClassNotFoundException {
    var initializer = SeContainerInitializer.newInstance().disableDiscovery();
    for (String bean : new String[] {
        "org.openbravo.client.application.window.ApplicationDictionaryCachedStructures",
        "org.openbravo.base.weld.WeldUtils",
        "org.openbravo.client.kernel.TemplateProcessor$Registry",
        "org.openbravo.client.kernel.freemarker.FreemarkerTemplateProcessor",
        "org.openbravo.client.application.window.OBViewTab",
        "org.openbravo.client.application.window.StandardWindowComponent",
        "org.openbravo.client.application.window.OBViewFieldHandler",
        "org.openbravo.client.application.window.OBViewFormComponent",
        "org.openbravo.client.application.window.OBViewGridComponent",
        "org.openbravo.client.application.CachedPreference",
        "org.openbravo.service.datasource.DataSourceComponent",
        "org.openbravo.service.datasource.DataSourceComponentProvider",
        "org.openbravo.service.datasource.DataSourceServiceProvider",
        "org.openbravo.service.datasource.DefaultDataSourceService",
        "org.openbravo.service.datasource.NoteDataSource",
        "org.openbravo.client.application.GlobalMenu",
        "org.openbravo.client.application.MenuManager",
        "org.openbravo.client.application.navigationbarcomponents.ApplicationMenuComponent",
        "org.openbravo.client.application.MainLayoutComponent",
        "org.openbravo.client.application.NavigationBarComponent",
        "org.openbravo.client.application.NavigationBarComponentGenerator",
        "org.openbravo.client.application.navigationbarcomponents.UserInfoComponent",
        "org.openbravo.client.kernel.BaseTemplateComponent"}) {
      initializer.addBeanClasses(Class.forName(bean));
    }
    container = initializer.initialize();
  }

  String render(String componentId, String windowId) throws ClassNotFoundException {
    var requestScope = container.select(RequestContextController.class).get();
    requestScope.activate();
    try {
      OBContext.setAdminMode();
      try {
        BaseTemplateComponent component;
        if ("window".equals(componentId)) {
          var option = findWindow(container.select(MenuManager.class).get().getMenu(), windowId);
          if (option == null) throw new SecurityException("Window is not available to this role");
          var window = container.select(StandardWindowComponent.class).get();
          window.setWindow(option.getMenu().getWindow());
          component = window;
        } else {
          String type = switch (componentId) {
            case "menu" -> "org.openbravo.client.application.navigationbarcomponents.ApplicationMenuComponent";
            case "navigation" -> "org.openbravo.client.application.NavigationBarComponent";
            case "layout" -> "org.openbravo.client.application.MainLayoutComponent";
            default -> throw new IllegalArgumentException("Unknown UI component");
          };
          component = (BaseTemplateComponent) container.select(Class.forName(type)).get();
        }
        var parameters = new HashMap<String, Object>();
        parameters.put("Constants_FIELDSEPARATOR", "$");
        parameters.put("Constants_IDENTIFIER", "_identifier");
        component.setParameters(parameters);
        return component.generate();
      } finally { OBContext.restorePreviousMode(); }
    } finally { requestScope.deactivate(); }
  }

  private static MenuManager.MenuOption findWindow(MenuManager.MenuOption option, String id) {
    if (option.isWindow() && option.isVisible() && option.isAccessible() && option.getMenu() != null
        && option.getMenu().getWindow().getId().equals(id)) return option;
    for (var child : option.getChildren()) {
      var found = findWindow(child, id);
      if (found != null) return found;
    }
    return null;
  }

  @Override public void close() { container.close(); }
}
