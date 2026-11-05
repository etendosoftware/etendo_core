/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.dal.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;

/**
 * Unit tests for {@link DalPropertyAccess}. Exercises the getter and setter implementations it
 * builds against simple POJO classes, with the DAL model resolved through a mocked
 * {@link ModelProvider}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DalPropertyAccessTest {

  private static final String NAME = "name";
  private static final String ACTIVE = "active";
  private static final String MISSING = "missing";
  private static final String ID = "id";
  private static final String VALUE = "value";

  /** Simple POJO with a regular getter, a boolean getter and an id setter. */
  public static class SamplePojo {
    public static final String PROPERTY_NAME = NAME;
    public static final String PROPERTY_ACTIVE = ACTIVE;

    /**
     * @return a fixed name
     */
    public String getName() {
      return VALUE;
    }

    /**
     * @return a fixed active flag
     */
    public Boolean isActive() {
      return Boolean.TRUE;
    }

    /**
     * @param id
     *          ignored
     */
    public void setId(String id) {
      // no-op
    }
  }

  /** POJO without any accessor methods. */
  public static class EmptyPojo {
    @Override
    public String toString() {
      return "EmptyPojo";
    }
  }

  @Mock
  private ModelProvider modelProvider;
  @Mock
  private Entity entity;
  @Mock
  private PropertyAccessStrategy strategy;

  private MockedStatic<ModelProvider> modelProviderStatic;

  /**
   * Mocks the static ModelProvider accessor.
   */
  @Before
  public void setUp() {
    modelProviderStatic = mockStatic(ModelProvider.class);
    modelProviderStatic.when(ModelProvider::getInstance).thenReturn(modelProvider);
    lenient().when(modelProvider.getEntity(SamplePojo.class)).thenReturn(entity);
    lenient().when(modelProvider.getEntity(EmptyPojo.class)).thenReturn(entity);
  }

  /**
   * Closes the static mock.
   */
  @After
  public void tearDown() {
    modelProviderStatic.close();
  }

  private Property stubProperty(String propertyName, String getterSetterName, boolean isBoolean) {
    Property property = mock(Property.class);
    when(property.getGetterSetterName()).thenReturn(getterSetterName);
    when(property.isBoolean()).thenReturn(isBoolean);
    when(entity.getProperty(propertyName)).thenReturn(property);
    return property;
  }

  /**
   * Verifies the strategy, getter and setter are exposed by the accessors.
   */
  @Test
  public void exposesStrategyGetterAndSetter() {
    DalPropertyAccess access = new DalPropertyAccess(strategy, SamplePojo.class, NAME);
    assertSame(strategy, access.getPropertyAccessStrategy());
    assertNotNull(access.getGetter());
    assertNotNull(access.getSetter());
  }

  /**
   * Verifies a non boolean getter resolves the get method, its name, member and return type, and
   * caches the method after the first lookup.
   */
  @Test
  public void getterResolvesRegularGetMethodAndCaches() {
    Property property = stubProperty(NAME, NAME, false);
    Getter getter = new DalPropertyAccess(strategy, SamplePojo.class, NAME).getGetter();

    Method method = getter.getMethod();
    assertNotNull(method);
    assertEquals("getName", method.getName());
    assertSame(method, getter.getMember());
    assertEquals("getName", getter.getMethodName());
    assertEquals(String.class, getter.getReturnType());
    verify(property, times(1)).getGetterSetterName();
  }

  /**
   * Verifies a boolean property resolves an "is" prefixed getter.
   */
  @Test
  public void getterResolvesBooleanIsMethod() {
    stubProperty(ACTIVE, ACTIVE, true);
    Getter getter = new DalPropertyAccess(strategy, SamplePojo.class, ACTIVE).getGetter();

    assertEquals("isActive", getter.getMethodName());
    assertEquals(Boolean.class, getter.getReturnType());
  }

  /**
   * Verifies a missing getter method yields null method, name and return type.
   */
  @Test
  public void getterReturnsNullWhenMethodMissing() {
    stubProperty(MISSING, MISSING, false);
    Getter getter = new DalPropertyAccess(strategy, EmptyPojo.class, MISSING).getGetter();

    assertNull(getter.getMethod());
    assertNull(getter.getMember());
    assertNull(getter.getMethodName());
    assertNull(getter.getReturnType());
  }

  /**
   * Verifies get and getForInsert read the value through BaseOBObject.getValue.
   */
  @Test
  public void getterReadsValueFromBaseOBObject() {
    Getter getter = new DalPropertyAccess(strategy, SamplePojo.class, NAME).getGetter();
    BaseOBObject owner = mock(BaseOBObject.class);
    when(owner.getValue(NAME)).thenReturn(VALUE);

    assertEquals(VALUE, getter.get(owner));
    assertEquals(VALUE, getter.getForInsert(owner, new HashMap<>(), null));
  }

  /**
   * Verifies the setter of a non id property has no method nor method name.
   */
  @Test
  public void setterReturnsNullForNonIdProperty() {
    Setter setter = new DalPropertyAccess(strategy, SamplePojo.class, NAME).getSetter();

    assertNull(setter.getMethod());
    assertNull(setter.getMethodName());
  }

  /**
   * Verifies the id setter resolves setId(String) and caches it.
   */
  @Test
  public void setterResolvesIdSetterAndCaches() {
    Setter setter = new DalPropertyAccess(strategy, SamplePojo.class, ID).getSetter();

    Method method = setter.getMethod();
    assertNotNull(method);
    assertEquals("setId", method.getName());
    assertSame(method, setter.getMethod());
    assertEquals("setId", setter.getMethodName());
  }

  /**
   * Verifies the id setter returns null when the class has no setId(String) method.
   */
  @Test
  public void setterReturnsNullWhenIdSetterMissing() {
    Setter setter = new DalPropertyAccess(strategy, EmptyPojo.class, ID).getSetter();

    assertNull(setter.getMethod());
    assertNull(setter.getMethodName());
  }

  /**
   * Verifies set writes the value through BaseOBObject.setValue.
   */
  @Test
  public void setterWritesValueToBaseOBObject() {
    Setter setter = new DalPropertyAccess(strategy, SamplePojo.class, NAME).getSetter();
    BaseOBObject target = mock(BaseOBObject.class);

    setter.set(target, VALUE, null);
    verify(target).setValue(NAME, VALUE);
  }
}
