package com.etendoerp.hibernate.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

/**
 * Qualifier annotation to mark FunctionContributor implementations and specify
 * which database dialects they support.
 * <p>
 * Use {@link DatabaseType#COMMON} for functions that work identically in both databases.
 * Use specific types for database-specific implementations.
 * </p>
 *
 * Example:
 * <pre>
 * {@code
 * @DatabaseFunction(DatabaseType.COMMON)
 * @ApplicationScoped
 * public class MyModuleFunctions implements FunctionContributor {
 *     public void contribute(FunctionContributions contributions) {
 *         // Register functions
 *     }
 * }
 * }
 * </pre>
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface DatabaseFunction {

  /**
   * The database type(s) this contributor supports.
   * This must be marked as @Nonbinding because arrays are not allowed
   * in qualifiers used for CDI injection matching.
   */
  @Nonbinding
  DatabaseType[] value() default DatabaseType.COMMON;

  /**
   * Optional description of the functions provided.
   * This must be marked as @Nonbinding to prevent it from being used
   * in CDI injection matching.
   */
  @Nonbinding
  String description() default "";

}
