package org.openbravo.base.session;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.jdbc.CharJdbcType;

/**
 * Implements the same logic as the hibernate yesno type, handles null values as false. As certain
 * methods can not be extended the solution is to catch the isDirty check by reimplementing the
 * areEqual method.
 */
public class OBYesNoType extends AbstractSingleColumnStandardBasicType<Boolean> {

  private static final long serialVersionUID = 1L;

  public static final OBYesNoType INSTANCE = new OBYesNoType();

  public OBYesNoType() {
    super(CharJdbcType.INSTANCE, new LocalBooleanJavaType());
  }

  @Override
  public String getName() {
    return "yes_no";
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
