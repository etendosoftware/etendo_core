package com.etendoerp.platform.web;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.dal.core.DalSessionFactoryController;

/** Boots the actual DAL from externally supplied configuration when Tomcat deploys the WAR. */
public final class PlatformLifecycle implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        String file = System.getProperty("platform.validation.properties");
        if (file == null || file.isBlank()) throw new IllegalStateException("Missing external platform.validation.properties");
        OBPropertiesProvider.getInstance().setProperties(file);
        var properties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
        for (String key : new String[] {"platform.validation.token", "platform.validation.readOnlyToken"}) {
            if (properties.getProperty(key, "").length() < 32) throw new IllegalStateException("Missing validation access token");
        }
        if (properties.getProperty("platform.validation.token").equals(properties.getProperty("platform.validation.readOnlyToken"))) {
            throw new IllegalStateException("Validation access tokens must be distinct");
        }
        try {
            for (var entity : ModelProvider.getInstance().getModel()) {
                Class<?> type = entity.getMappingClass();
                if (type == null) throw new IllegalStateException("Missing generated entity " + entity.getClassName());
                OBProvider.getInstance().register(type, type, false);
                OBProvider.getInstance().register(entity.getName(), type, false);
            }
            SessionFactoryController.setRunningInWebContainer(true);
            SessionFactoryController.setInstance(new DalSessionFactoryController());
            SessionFactoryController.getInstance().getSessionFactory();
            event.getServletContext().setAttribute("platform.ready", Boolean.TRUE);
        } finally {
            org.openbravo.database.SessionInfo.init();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        try {
            SessionFactoryController.setInstance(null);
        } finally {
            // Remove only drivers owned by this WAR, never a container/shared driver.
            java.sql.DriverManager.drivers().filter(driver -> driver.getClass().getClassLoader()
                    == PlatformLifecycle.class.getClassLoader()).forEach(driver -> {
                        try { java.sql.DriverManager.deregisterDriver(driver); }
                        catch (java.sql.SQLException failure) { event.getServletContext().log("JDBC driver cleanup failed", failure); }
                    });
        }
    }
}
