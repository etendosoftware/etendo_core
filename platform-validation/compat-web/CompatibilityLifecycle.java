package com.etendoerp.platform.compat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.dal.core.DalSessionFactoryController;

/** Starts only the platform DAL using external, read-only Classic database configuration. */
public final class CompatibilityLifecycle implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            Properties external = new Properties();
            try (var input = Files.newInputStream(Path.of(System.getProperty("platform.validation.properties")))) {
                external.load(input);
            }
            Properties safe = new Properties();
            for (String key : new String[] {"bbdd.rdbms", "bbdd.driver", "bbdd.url", "bbdd.sid", "bbdd.user", "bbdd.password"}) {
                String value = external.getProperty(key);
                if (value == null || value.isBlank() || value.contains("${")) throw new IllegalStateException("Missing configuration: " + key);
                safe.setProperty(key, value);
            }
            if (!"POSTGRE".equals(safe.getProperty("bbdd.rdbms"))
                    || !safe.getProperty("bbdd.url").matches("jdbc:postgresql://[A-Za-z0-9.\\-]+:[0-9]+")
                    || !safe.getProperty("bbdd.sid").matches("[A-Za-z0-9_\\-]+")) {
                throw new IllegalStateException("Expected a PostgreSQL host:port and plain database name");
            }
            safe.setProperty("bbdd.sid", safe.getProperty("bbdd.sid")
                    + "?options=-c%20default_transaction_read_only%3Don&connectTimeout=5&socketTimeout=30");
            safe.setProperty("bbdd.sessionConfig", "select 1");
            safe.setProperty("hibernate.hbm2ddl.auto", "none");
            OBPropertiesProvider.getInstance().setProperties(safe);
            for (var entity : ModelProvider.getInstance().getModel()) {
                if (entity.isDataSourceBased() || entity.isHQLBased()) continue;
                Class<?> type = entity.getMappingClass();
                if (type == null) throw new IllegalStateException("Missing generated mapping: " + entity.getClassName());
                OBProvider.getInstance().register(type, type, false);
                OBProvider.getInstance().register(entity.getName(), type, false);
            }
            SessionFactoryController.setRunningInWebContainer(true);
            SessionFactoryController.setInstance(new DalSessionFactoryController());
            SessionFactoryController.getInstance().getSessionFactory();
            event.getServletContext().setAttribute("platform.runtime", "platform-core");
        } catch (Exception failure) {
            throw new IllegalStateException("Platform compatibility startup failed", failure);
        } finally { org.openbravo.database.SessionInfo.init(); }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        SessionFactoryController.setInstance(null);
        java.sql.DriverManager.drivers().filter(driver -> driver.getClass().getClassLoader() == getClass().getClassLoader())
                .forEach(driver -> {
                    try { java.sql.DriverManager.deregisterDriver(driver); }
                    catch (java.sql.SQLException failure) { event.getServletContext().log("Driver cleanup failed", failure); }
                });
    }
}
