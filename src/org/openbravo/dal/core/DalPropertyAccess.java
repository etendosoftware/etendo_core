package org.openbravo.dal.core;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.NamingUtil;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;

/**
 * Accessor for DAL entity properties compatible with Hibernate 6.
 */
@SuppressWarnings("rawtypes")
class DalPropertyAccess implements PropertyAccess {

  private static final Logger log = LogManager.getLogger();

  private final PropertyAccessStrategy strategy;
  private final GetterMethod getter;
  private final SetterMethod setter;

  DalPropertyAccess(PropertyAccessStrategy strategy, Class<?> containerJavaType,
      final String propertyName) {
    this.strategy = strategy;
    this.getter = new GetterMethod(containerJavaType, propertyName);
    this.setter = new SetterMethod(containerJavaType, propertyName);
  }

  @Override
  public PropertyAccessStrategy getPropertyAccessStrategy() {
    return strategy;
  }

  @Override
  public Getter getGetter() {
    return getter;
  }

  @Override
  public Setter getSetter() {
    return setter;
  }

  // ---------------------- Getter ----------------------

  private static class GetterMethod implements Getter {
    private static final long serialVersionUID = 1L;

    private final String propertyName;
    private final Class<?> theClass;
    private transient Method method;

    public GetterMethod(Class<?> theClass, String propertyName) {
      this.theClass = theClass;
      this.propertyName = NamingUtil.getStaticPropertyName(theClass, propertyName);
    }

    @Override
    public Method getMethod() {
      if (method != null) {
        return method;
      }
      Property property = ModelProvider.getInstance().getEntity(theClass).getProperty(propertyName);
      String methodName = property.getGetterSetterName();
      methodName = (property.isBoolean() ? "is" : "get") +
          methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
      try {
        method = theClass.getDeclaredMethod(methodName);
      } catch (NoSuchMethodException | SecurityException e) {
        log.debug("Could not find method {} of class {}", methodName, theClass.getName());
      }
      return method;
    }

    @Override
    public Member getMember() {
      return getMethod();
    }

    @Override
    public String getMethodName() {
      Method getter = getMethod();
      return getter != null ? getter.getName() : null;
    }

    @Override
    public Class<?> getReturnType() {
      Method getter = getMethod();
      return getter != null ? getter.getReturnType() : Object.class;
    }

    @Override
    public Class<?> getReturnTypeClass() {
      return getReturnType();
    }

    @Override
    public Object get(Object owner) {
      return ((BaseOBObject) owner).getValue(propertyName);
    }

    @Override
    public Object getForInsert(Object owner, Map mergeMap,
        SharedSessionContractImplementor session) {
      return get(owner);
    }
  }

  // ---------------------- Setter ----------------------

  private static class SetterMethod implements Setter {
    private static final long serialVersionUID = 1L;
    private static final String ID_SETTER = "setId";

    private final String propertyName;
    private final Class<?> theClass;
    private transient Method method;

    public SetterMethod(Class<?> theClass, String propertyName) {
      this.theClass = theClass;
      this.propertyName = NamingUtil.getStaticPropertyName(theClass, propertyName);
    }

    @Override
    public Method getMethod() {
      if (method != null) {
        return method;
      }
      if (!BaseOBObject.ID.equals(propertyName)) {
        return null;
      }
      try {
        method = theClass.getDeclaredMethod(ID_SETTER, String.class);
      } catch (NoSuchMethodException | SecurityException e) {
        log.debug("Could not find method setId(String) in class {}", theClass.getName());
      }
      return method;
    }

    @Override
    public String getMethodName() {
      Method setter = getMethod();
      return setter != null ? setter.getName() : null;
    }

    @Override
    public void set(Object target, Object value) {
      ((BaseOBObject) target).setValue(propertyName, value);
    }
  }
}
