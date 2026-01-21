package com.etendoerp.hibernate;

import java.math.BigDecimal;
import java.util.Date;

import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.enterprise.context.Dependent;

/**
 * Utility class that defines return type resolvers for custom functions in Hibernate.
 * <p>
 * Provides resolvers for basic types such as Integer, BigDecimal, String, Timestamp, and Boolean.
 * These resolvers ensure that custom SQL functions return consistent types.
 * </p>
 */
@Dependent
public class FunctionReturnType {

  /**
   * Constructor for the `FunctionReturnType` utility class.
   * <p>
   * This constructor is private and throws an `UnsupportedOperationException`
   * to prevent instantiation of this utility class.
   * </p>
   */
  private FunctionReturnType() {
    throw new UnsupportedOperationException("Utility class");
  }

  /** Return type resolver for functions returning Integer values. */
  public static final FunctionReturnTypeResolver INTEGER_RETURN_TYPE;
  static {
    TypeConfiguration typeConfiguration = new TypeConfiguration();
    BasicType<Integer> integerBasicType = typeConfiguration.getBasicTypeRegistry()
        .resolve(StandardBasicTypes.INTEGER);
    INTEGER_RETURN_TYPE = StandardFunctionReturnTypeResolvers.invariant(integerBasicType);
  }

  /** Return type resolver for functions returning BigDecimal values. */
  public static final FunctionReturnTypeResolver BIGDECIMAL_RETURN_TYPE;
  static {
    TypeConfiguration typeConfiguration = new TypeConfiguration();
    BasicType<BigDecimal> bigDecimalBasicType = typeConfiguration.getBasicTypeRegistry()
        .resolve(StandardBasicTypes.BIG_DECIMAL);
    BIGDECIMAL_RETURN_TYPE = StandardFunctionReturnTypeResolvers.invariant(bigDecimalBasicType);
  }

  /** Return type resolver for functions returning String values. */
  public static final FunctionReturnTypeResolver STRING_RETURN_TYPE;
  static {
    TypeConfiguration typeConfiguration = new TypeConfiguration();
    BasicType<String> stringBasicType = typeConfiguration.getBasicTypeRegistry()
        .resolve(StandardBasicTypes.STRING);
    STRING_RETURN_TYPE = StandardFunctionReturnTypeResolvers.invariant(stringBasicType);
  }

  /** Return type resolver for functions returning Timestamp values. */
  public static final FunctionReturnTypeResolver TIMESTAMP_RETURN_TYPE;
  static {
    TypeConfiguration typeConfiguration = new TypeConfiguration();
    BasicType<Date> timestampBasicType = typeConfiguration.getBasicTypeRegistry()
        .resolve(StandardBasicTypes.TIMESTAMP);
    TIMESTAMP_RETURN_TYPE = StandardFunctionReturnTypeResolvers.invariant(timestampBasicType);
  }

  /** Return type resolver for functions returning Boolean values. */
  public static final FunctionReturnTypeResolver BOOLEAN_RETURN_TYPE;
  static {
    TypeConfiguration typeConfiguration = new TypeConfiguration();
    BasicType<Boolean> booleanBasicType = typeConfiguration.getBasicTypeRegistry()
        .resolve(StandardBasicTypes.BOOLEAN);
    BOOLEAN_RETURN_TYPE = StandardFunctionReturnTypeResolvers.invariant(booleanBasicType);
  }

}
