package com.etendoerp.redis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.provider.OBConfigFileProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton which holds a RedissonClient instance.
 * Use {@link #isAvailable()} to check if the environment is configured to use redis.
 * If the environment is available, then use {@link #getClient()} to obtain the {@link RedissonClient} instance and operate on it.
 * @see RedissonClient
 */
public enum RedisClient {
    INSTANCE;
    private boolean initialized = false;
    private boolean available = false;
    private RedissonClient redisson;
    private static final Logger log = LogManager.getLogger();

    /**
     * @return this singleton's instance
     */
    public static RedisClient getInstance() {
        return INSTANCE;
    }

    /**
     * @return the {@link RedissonClient} instance
     */
    public RedissonClient getClient() {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("Redis client not available. Please check your configuration.");
        }
        return redisson;
    }

    /**
     * @return True when the the {@link RedissonClient} instance was created successfully.
     * If a server does not have a redis instance configured, this will return false.
     */
    public boolean isAvailable() {
        if (!initialized) {
            initialize();
        }
        return available;
    }

    /**
     * Initialises the {@link RedissonClient} instance with some configuration
     * Configuration is obtained from the Openbravo.properties file.
     * The possible configurations are the default which is just the redis host URI, or a path to a custom YAML file
     */
    protected void initialize() {
        Properties properties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
        String redisHost = properties.getProperty("redis.host");
        String redisYAMLPath = properties.getProperty("redis.yaml");

        try {
            Config config = null;
            if (redisYAMLPath != null && !redisYAMLPath.isBlank()) {
                log.debug("Using Redisson configuration from file: {}", redisYAMLPath);

                // Search the file in Tomcat's context
                final InputStream is = getResource("/WEB-INF/" + redisYAMLPath);
                if (is != null) {
                    config = Config.fromYAML(is);
                }
            } else if (redisHost != null && !redisHost.isBlank()) {
                log.debug("Using Redisson default configuration with host: {}", redisHost);
                config = new Config();
                config
                        .useSingleServer()
                        .setAddress(redisHost);
            }

            if (config != null) {
                redisson = Redisson.create(config);
                available = true;
            }

        } catch (IOException e) {
            log.error("Error while loading Redis YAML configuration file.", e);
            available = false;
        } catch (Exception e) {
            log.error("Error while creating Redisson instance. Check your configuration." , e);
            available = false;
        }

        initialized = true;
    }

    /**
     * Obtains a resource from the servlet context if available, or from this class context
     * @param name the name of the resource to find
     * @return the InputStream pointing to the requested resource, null if not found.
     */
    private InputStream getResource(String name) {
        if (OBConfigFileProvider.getInstance().getServletContext() != null) {
            return OBConfigFileProvider.getInstance().getServletContext().getResourceAsStream(name);
        } else {
            return this.getClass().getResourceAsStream(name);
        }
    }

    protected void shutdown() {
        redisson.shutdown();
    }
}
