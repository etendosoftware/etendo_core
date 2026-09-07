package org.openbravo.base.session;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.ConvertedBasicType;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.jdbc.CharJdbcType;

/**
 * Implements the same logic as the hibernate yesno type, handles null values as false. As certain
 * methods can not be extended the solution is to catch the isDirty check by reimplementing the
 * areEqual method.
 */
public class OBYesNoType extends AbstractSingleColumnStandardBasicType<Boolean>
    implements ConvertedBasicType<Boolean> {

  private static final long serialVersionUID = 1L;

  private static final BasicValueConverter<Boolean, String> CONVERTER = new BasicValueConverter<>() {
    @Override
    public Boolean toDomainValue(String value) {
      return value == null ? null : "Y".equalsIgnoreCase(value);
    }

    @Override
    public String toRelationalValue(Boolean value) {
      return value == null ? null : value ? "Y" : "N";
    }

    @Override
    public JavaType<Boolean> getDomainJavaType() {
      return new LocalBooleanJavaType();
    }

    @Override
    public JavaType<String> getRelationalJavaType() {
      // Legacy HQL also compares these flags to string literals 'Y' and 'N'.
      return StringJavaType.INSTANCE;
    }
  };
  private static final ConvertedBasicTypeImpl<Boolean> JDBC_MAPPING = new ConvertedBasicTypeImpl<>(
      "yes_no", CharJdbcType.INSTANCE, CONVERTER);

  public static final OBYesNoType INSTANCE = new OBYesNoType();

  public OBYesNoType() {
    super(CharJdbcType.INSTANCE, new LocalBooleanJavaType());
  }

  @Override
  public String getName() {
    return "yes_no";
  }

  /** Exposes the relational Y/N representation to Hibernate 6 HQL literal inference. */
  @Override
  public BasicValueConverter<Boolean, ?> getValueConverter() {
    return CONVERTER;
  }

  @Override
  public JavaType<?> getJdbcJavaType() {
    return JDBC_MAPPING.getJdbcJavaType();
  }

  @Override
  public ValueBinder<Boolean> getJdbcValueBinder() {
    return JDBC_MAPPING.getJdbcValueBinder();
  }

  @Override
  public ValueExtractor<Boolean> getJdbcValueExtractor() {
    return JDBC_MAPPING.getJdbcValueExtractor();
  }

  @Override
  @SuppressWarnings("unchecked")
  public JdbcLiteralFormatter<Boolean> getJdbcLiteralFormatter() {
    return JDBC_MAPPING.getJdbcLiteralFormatter();
  }

  /**
   * Converts a Java Boolean value into its SQL literal representation.
   * Example: Y / N
   */
  public String objectToSQLString(Boolean value, Dialect dialect) {
    return '\'' + (value != null && value ? "Y" : "N") + '\'';
  }

  /**
   * Converts a string (database or XML) into a Boolean value.
   * Accepts Y, T, 1 as true; everything else is false.
   */
  public Boolean fromString(CharSequence string) {
    if (string == null || string.length() == 0) {
      return Boolean.FALSE;
    }
    char c = Character.toUpperCase(string.charAt(0));
    return c == 'Y' || c == 'T' || c == '1';
  }

  /**
   * Returns the default Boolean value for this type (false).
   */
  public Serializable getDefaultValue() {
    return Boolean.FALSE;
  }

  /**
   * Custom Boolean Java descriptor that treats null as false
   * when comparing values.
   */
  private static class LocalBooleanJavaType extends BooleanJavaType {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean areEqual(Boolean x, Boolean y) {
      if (x == y) return true;
      if (x == null) return y == null || !y;
      if (y == null) return !x;
      return super.areEqual(x, y);
    }
  }
}
