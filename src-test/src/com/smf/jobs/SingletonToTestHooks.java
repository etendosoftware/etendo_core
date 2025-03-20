package com.smf.jobs;

import java.util.HashMap;

/**
 * Singleton class to manage metadata for testing hooks.
 * <p>
 * This class provides a singleton instance to store and retrieve metadata
 * in the form of key-value pairs, where the key is a String and the value is a Boolean.
 */
public class SingletonToTestHooks {
  private static SingletonToTestHooks instance;
  private HashMap<String, Boolean> metadata;

  /**
   * Private constructor to prevent instantiation.
   */
  private SingletonToTestHooks() {
  }

  /**
   * Returns the singleton instance of the class.
   * <p>
   * If the instance is null, it initializes a new instance.
   *
   * @return The singleton instance of SingletonToTestHooks.
   */
  public static SingletonToTestHooks getInstance() {
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
  public HashMap<String, Boolean> getMetadata() {
    if (instance == null) {
      instance = new SingletonToTestHooks();
    }
    if (instance.metadata == null) {
      instance.metadata = new HashMap<String, Boolean>();
    }

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
    if (instance == null) {
      instance = new SingletonToTestHooks();
    }
    if (instance.metadata == null) {
      instance.metadata = new HashMap<String, Boolean>();
    }

    metadata.put(key, value);
  }
}