package org.openbravo.dal.core;

import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

/**
 * Factory that creates DalPropertyAccess instances (Hibernate 6 version).
 */
public class DalPropertyAccessStrategy implements PropertyAccessStrategy {

  @Override
  public PropertyAccess buildPropertyAccess(Class<?> containerJavaType,
      String propertyName,
      boolean setterRequired) {
    return new DalPropertyAccess(this, containerJavaType, propertyName);
  }
}
