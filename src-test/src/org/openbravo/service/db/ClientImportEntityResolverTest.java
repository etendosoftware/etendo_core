/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.service.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Tests for {@link ClientImportEntityResolver}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ClientImportEntityResolverTest {

  private ClientImportEntityResolver resolver;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBProvider mockOBProvider;

  @Mock
  private ModelProvider mockModelProvider;

  @Mock
  private Entity mockEntity;

  @Mock
  private BaseOBObject mockBaseOBObject;

  @Mock
  private Organization mockOrgZero;

  @Mock
  private Client mockClientZero;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBProvider> obProviderStatic;
  private MockedStatic<ModelProvider> modelProviderStatic;

  @Before
  public void setUp() throws Exception {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obProviderStatic = mockStatic(OBProvider.class);
    obProviderStatic.when(OBProvider::getInstance).thenReturn(mockOBProvider);

    modelProviderStatic = mockStatic(ModelProvider.class);
    modelProviderStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);

    lenient().when(mockModelProvider.getEntity(anyString())).thenReturn(mockEntity);

    ObjenesisStd objenesis = new ObjenesisStd();
    resolver = spy(objenesis.newInstance(ClientImportEntityResolver.class));

    // Initialize the data map via reflection (from parent EntityResolver)
    Map<Object, BaseOBObject> data = new HashMap<>();
    Field dataField = findField(resolver.getClass(), "data");
    dataField.setAccessible(true);
    dataField.set(resolver, data);

    // Set protected fields via reflection on parent class EntityResolver
    Field orgZeroField = findField(resolver.getClass(), "organizationZero");
    orgZeroField.setAccessible(true);
    orgZeroField.set(resolver, mockOrgZero);

    Field clientZeroField = findField(resolver.getClass(), "clientZero");
    clientZeroField.setAccessible(true);
    clientZeroField.set(resolver, mockClientZero);
  }

  @After
  public void tearDown() {
    if (modelProviderStatic != null) {
      modelProviderStatic.close();
    }
    if (obProviderStatic != null) {
      obProviderStatic.close();
    }
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }

  @Test
  public void testResolveOrganizationZeroReturnsOrgZero() {
    BaseOBObject result = resolver.resolve(Organization.ENTITY_NAME, "0", false);
    assertEquals(mockOrgZero, result);
  }

  @Test
  public void testResolveClientZeroReturnsClientZero() {
    BaseOBObject result = resolver.resolve(Client.ENTITY_NAME, "0", false);
    assertEquals(mockClientZero, result);
  }

  @Test
  public void testResolveClientZeroIdReturnsClientZero() {
    BaseOBObject result = resolver.resolve(Client.ENTITY_NAME, "0", false);
    assertEquals(mockClientZero, result);
  }

  @Test
  public void testResolveWithFilterOrganizationsDelegatesToResolve() {
    BaseOBObject result = resolver.resolve(Organization.ENTITY_NAME, "0", false, true);
    assertEquals(mockOrgZero, result);
  }

  @Test
  public void testResolveOrganizationWithFilterOrganizations() {
    BaseOBObject result = resolver.resolve(Organization.ENTITY_NAME, "0", false, true);
    assertEquals(mockOrgZero, result);
  }

  @Test
  public void testFindUniqueConstrainedObjectReturnsNull() throws Exception {
    java.lang.reflect.Method method = ClientImportEntityResolver.class.getDeclaredMethod(
        "findUniqueConstrainedObject", BaseOBObject.class);
    method.setAccessible(true);
    Object result = method.invoke(resolver, mockBaseOBObject);
    assertNull(result);
  }

  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }
}
