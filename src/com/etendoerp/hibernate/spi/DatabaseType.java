package com.etendoerp.hibernate.spi;

/**
 * Enumeration of supported database types for function contributors.
 */
public enum DatabaseType {
  /**
   * Functions that work identically in both Oracle and PostgreSQL.
   * These will be registered in both dialects.
   */
  COMMON,

  /**
   * Functions specific to PostgreSQL dialect.
   */
  POSTGRESQL,

  /**
   * Functions specific to Oracle dialect.
   */
  ORACLE
}
