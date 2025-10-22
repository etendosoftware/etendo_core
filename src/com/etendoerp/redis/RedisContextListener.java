package com.etendoerp.redis;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class RedisContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        RedisClient.getInstance().initialize();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        RedisClient.getInstance().shutdown();
    }
}
