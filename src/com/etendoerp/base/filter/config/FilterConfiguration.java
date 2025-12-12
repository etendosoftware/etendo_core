package com.etendoerp.base.filter.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.session.OBPropertiesProvider;

/**
 * Configuration manager for filter chain behavior.
 *
 * <p>Controls whether the application uses the legacy filter chain or the new
 * refactored implementation via feature flag.</p>
 *
 * <p><strong>Configuration Priority (highest to lowest):</strong></p>
 * <ol>
 *   <li>Environment variable: FILTER_CHAIN_LEGACY</li>
 *   <li>System property: filter.chain.legacy</li>
 *   <li>Config file: config/filter-chain.properties</li>
 *   <li>etendo/Openbravo properties via OBPropertiesProvider (fallback)</li>
 *   <li>Default: true (legacy mode for safety)</li>
 * </ol>
 *
 * <p><strong>Configuration Format:</strong></p>
 * <pre>
 * # config/filter-chain.properties
 * filter.chain.legacy=false
 * </pre>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * if (FilterConfiguration.isLegacyMode()) {
 *   // Use old filter chain
 *   chain.doFilter(request, response);
 * } else {
 *   // Use new filter chain coordinator
 *   coordinator.execute(context);
 * }
 * }</pre>
 *
 * <p>Thread-safe singleton with lazy initialization.</p>
 *
 * @since Etendo 24.Q4
 */
public class FilterConfiguration {

  private static final Logger log = LogManager.getLogger(FilterConfiguration.class);

  private static final String ENV_VAR_NAME = "FILTER_CHAIN_LEGACY";
  private static final String SYSTEM_PROPERTY_NAME = "filter.chain.legacy";
  private static final String CONFIG_FILE_PATH = "config/filter-chain.properties";
  private static final String PROPERTY_KEY = "filter.chain.legacy";

  private static volatile FilterConfiguration instance;
  private final boolean legacyMode;

  /**
   * Private constructor - use getInstance() for singleton access.
   */
  private FilterConfiguration() {
    this.legacyMode = loadConfiguration();
    logConfiguration();
  }

  /**
   * Returns the singleton instance.
   *
   * <p>Thread-safe lazy initialization with double-checked locking.</p>
   *
   * @return singleton instance (never null)
   */
  public static FilterConfiguration getInstance() {
    if (instance == null) {
      synchronized (FilterConfiguration.class) {
        if (instance == null) {
          instance = new FilterConfiguration();
        }
      }
    }
    return instance;
  }

  /**
   * Checks if legacy filter chain mode is enabled.
   *
   * <p>Static convenience method for common use case.</p>
   *
   * @return true if using legacy filters, false for new filter chain
   */
  public static boolean isLegacyMode() {
    return getInstance().legacyMode;
  }

  /**
   * Checks if new filter chain mode is enabled.
   *
   * <p>Static convenience method (inverse of isLegacyMode).</p>
   *
   * @return true if using new filter chain, false for legacy
   */
  public static boolean isNewFilterChainEnabled() {
    return !isLegacyMode();
  }

  /**
   * Loads configuration from all sources in priority order.
   *
   * @return true for legacy mode, false for new filter chain
   */
  private boolean loadConfiguration() {
    // Priority 1: Environment variable
    String envValue = System.getenv(ENV_VAR_NAME);
    if (envValue != null) {
      boolean result = parseBoolean(envValue, true);
      log.info("Filter chain mode from environment variable {}: legacy={}",
          ENV_VAR_NAME, result);
      return result;
    }

    // Priority 2: System property
    String sysPropValue = System.getProperty(SYSTEM_PROPERTY_NAME);
    if (sysPropValue != null) {
      boolean result = parseBoolean(sysPropValue, true);
      log.info("Filter chain mode from system property {}: legacy={}",
          SYSTEM_PROPERTY_NAME, result);
      return result;
    }

    // Priority 3: Config file
    Properties props = loadPropertiesFile();
    if (props != null && props.containsKey(PROPERTY_KEY)) {
      String propValue = props.getProperty(PROPERTY_KEY);
      boolean result = parseBoolean(propValue, true);
      log.info("Filter chain mode from config file {}: legacy={}",
          CONFIG_FILE_PATH, result);
      return result;
    }

    // Priority 4: OBPropertiesProvider fallback
    Boolean obPropValue = loadFromOBProperties();
    if (obPropValue != null) {
      return obPropValue;
    }

    // Priority 5: Default (legacy mode for safety)
    log.warn("No filter chain configuration found - defaulting to legacy mode for safety. " +
        "Set {}=false to enable new filter chain", ENV_VAR_NAME);
    return true;
  }

  /**
   * Loads properties from configuration file.
   *
   * @return properties object or null if file not found/readable
   */
  private Properties loadPropertiesFile() {
    Path configPath = Paths.get(CONFIG_FILE_PATH);

    if (!Files.exists(configPath)) {
      log.debug("Config file not found: {}", CONFIG_FILE_PATH);
      return null;
    }

    if (!Files.isReadable(configPath)) {
      log.warn("Config file exists but is not readable: {}", CONFIG_FILE_PATH);
      return null;
    }

    Properties props = new Properties();
    try (InputStream input = new FileInputStream(configPath.toFile())) {
      props.load(input);
      log.debug("Loaded properties from {}", CONFIG_FILE_PATH);
      return props;
    } catch (IOException e) {
      log.error("Failed to read config file: {}", CONFIG_FILE_PATH, e);
      return null;
    }
  }

  /**
   * Attempts to read configuration from OBPropertiesProvider as a fallback.
   *
   * @return Boolean value if found, or null if not present/unreadable
   */
  private Boolean loadFromOBProperties() {
    try {
      Properties obProps = OBPropertiesProvider.getInstance().getOpenbravoProperties();
      if (obProps == null) {
        log.debug("OBPropertiesProvider returned no properties");
        return null;
      }

      String value = obProps.getProperty(PROPERTY_KEY);
      if (value == null) {
        log.debug("Property {} not found in OBPropertiesProvider", PROPERTY_KEY);
        return null;
      }

      boolean result = parseBoolean(value, true);
      log.info("Filter chain mode from OBPropertiesProvider: legacy={}", result);
      return result;

    } catch (Exception e) {
      log.warn("Failed to read filter chain configuration from OBPropertiesProvider", e);
      return null;
    }
  }

  /**
   * Parses string value as boolean.
   *
   * <p>Accepts: "true"/"false" (case-insensitive), "1"/"0", "yes"/"no"</p>
   *
   * @param value string to parse
   * @param defaultValue value to return if parsing fails
   * @return parsed boolean value
   */
  private boolean parseBoolean(String value, boolean defaultValue) {
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    }

    String normalized = value.trim().toLowerCase();

    if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
      return true;
    }

    if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
      return false;
    }

    log.warn("Invalid boolean value '{}' - using default: {}", value, defaultValue);
    return defaultValue;
  }

  /**
   * Logs the active configuration.
   */
  private void logConfiguration() {
    if (legacyMode) {
      log.info("==================================================");
      log.info("FILTER CHAIN MODE: LEGACY (old implementation)");
      log.info("To enable new filter chain, set {}=false", ENV_VAR_NAME);
      log.info("==================================================");
    } else {
      log.info("==================================================");
      log.info("FILTER CHAIN MODE: NEW (refactored implementation)");
      log.info("To rollback to legacy mode, set {}=true", ENV_VAR_NAME);
      log.info("==================================================");
    }
  }

  /**
   * Returns whether legacy mode is enabled.
   *
   * @return true for legacy mode, false for new filter chain
   */
  public boolean isLegacy() {
    return legacyMode;
  }

  /**
   * Forces reload of configuration (for testing only).
   *
   * <p><strong>WARNING:</strong> Not thread-safe. Only use in test environments.</p>
   */
  static void resetForTesting() {
    synchronized (FilterConfiguration.class) {
      instance = null;
    }
  }
}
