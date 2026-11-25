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
package org.openbravo.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Base class for core Etendo class unit tests. Provides common mock setup
 * for OBDal, OBContext, OBProvider and OBPropertiesProvider singletons.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class BaseCoreTest {

  @Mock
  protected OBDal obDal;
  @Mock
  protected OBContext obContext;
  @Mock
  protected Client client;
  @Mock
  protected Organization organization;
  @Mock
  protected User user;
  @Mock
  protected Role role;
  @Mock
  protected OBProvider obProvider;
  @Mock
  protected OBPropertiesProvider obPropertiesProvider;
  @Mock
  protected OrganizationStructureProvider orgStructureProvider;

  protected MockedStatic<OBDal> obDalMock;
  protected MockedStatic<OBContext> obContextMock;
  protected MockedStatic<OBProvider> obProviderMock;
  protected MockedStatic<OBPropertiesProvider> obPropertiesProviderMock;

  @BeforeEach
  void baseSetUp() {
    obDalMock = mockStatic(OBDal.class);
    obContextMock = mockStatic(OBContext.class);
    obProviderMock = mockStatic(OBProvider.class);
    obPropertiesProviderMock = mockStatic(OBPropertiesProvider.class);

    obDalMock.when(OBDal::getInstance).thenReturn(obDal);
    obDalMock.when(OBDal::getReadOnlyInstance).thenReturn(obDal);
    obContextMock.when(OBContext::getOBContext).thenReturn(obContext);
    obProviderMock.when(OBProvider::getInstance).thenReturn(obProvider);
    obPropertiesProviderMock.when(OBPropertiesProvider::getInstance)
        .thenReturn(obPropertiesProvider);

    when(obContext.getCurrentClient()).thenReturn(client);
    when(obContext.getCurrentOrganization()).thenReturn(organization);
    when(obContext.getUser()).thenReturn(user);
    when(obContext.getRole()).thenReturn(role);
    when(obContext.getOrganizationStructureProvider()).thenReturn(orgStructureProvider);
  }

  @AfterEach
  void baseTearDown() {
    obDalMock.close();
    obContextMock.close();
    obProviderMock.close();
    obPropertiesProviderMock.close();
  }

  @SuppressWarnings("unchecked")
  protected <T extends BaseOBObject> OBCriteria<T> mockCriteria(Class<T> entityClass) {
    OBCriteria<T> crit = mock(OBCriteria.class);
    when(obDal.createCriteria(entityClass)).thenReturn(crit);
    when(crit.add(any())).thenReturn(crit);
    when(crit.setMaxResults(anyInt())).thenReturn(crit);
    return crit;
  }

  protected void mockDateFormats(String dateFormat, String dateTimeFormat) {
    Properties props = new Properties();
    props.setProperty("dateFormat.java", dateFormat);
    props.setProperty("dateTimeFormat.java", dateTimeFormat);
    when(obPropertiesProvider.getOpenbravoProperties()).thenReturn(props);
  }
}
