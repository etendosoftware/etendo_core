package com.smf.jobs;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class to manage metadata for testing hooks.
 * <p>
 * This class provides a singleton instance to store and retrieve metadata
 * in the form of key-value pairs, where the key is a String and the value is a Boolean.
 */
public class SingletonToTestHooks {
  private static SingletonToTestHooks instance;
  private final Map<String, Boolean> metadata;

  /**
   * Private constructor to prevent instantiation.
   */
  private SingletonToTestHooks() {
    metadata = new HashMap<>();
  }

  /**
   * Returns the singleton instance of the class.
   * <p>
   * If the instance is null, it initializes a new instance.
   *
   * @return The singleton instance of SingletonToTestHooks.
   */
  public static synchronized SingletonToTestHooks getInstance() {
    if (instance == null) {
      instance = new SingletonToTestHooks();
    }
    return instance;
  }

  /**
   * Returns the metadata map.
   * <p>
   * If the instance or metadata is null, it initializes them.
   *
   * @return The metadata map.
   */
  public Map<String, Boolean> getMetadata() {
    return metadata;
  }

  /**
   * Sets a metadata key-value pair.
   * <p>
   * If the instance or metadata is null, it initializes them.
   *
   * @param key
   *     The key for the metadata.
   * @param value
   *     The value for the metadata.
   */
  public void setMetadata(String key, boolean value) {
    metadata.put(key, value);
  }
}