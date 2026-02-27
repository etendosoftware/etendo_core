/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.ad_process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.module.DataPackage;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.module.ModuleDBPrefix;
import org.openbravo.model.ad.module.ModuleDependency;
import org.openbravo.model.ad.system.Language;
import org.openbravo.scheduling.ProcessBundle;

/**
 * Tests for {@link CreateCustomModule}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateCustomModuleTest {

  private CreateCustomModule process;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBProvider mockOBProvider;

  @Mock
  private ProcessBundle mockBundle;

  @Mock
  private OBCriteria<Module> mockModuleCriteria;

  @Mock
  private OBCriteria<Language> mockLanguageCriteria;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBProvider> obProviderStatic;
  private MockedStatic<OBContext> obContextStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    process = new CreateCustomModule();

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obProviderStatic = mockStatic(OBProvider.class);
    obProviderStatic.when(OBProvider::getInstance).thenReturn(mockOBProvider);

    obContextStatic = mockStatic(OBContext.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obContextStatic != null) {
      obContextStatic.close();
    }
    if (obProviderStatic != null) {
      obProviderStatic.close();
    }
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }
  /**
   * Execute when module already exists.
   * @throws Exception if an error occurs
   */

  @SuppressWarnings("unchecked")
  @Test
  public void testExecuteWhenModuleAlreadyExists() throws Exception {
    when(mockOBDal.createCriteria(Module.class)).thenReturn(mockModuleCriteria);
    when(mockModuleCriteria.count()).thenReturn(1);

    process.execute(mockBundle);

    verify(mockBundle).setResult(any(OBError.class));
    verify(mockOBDal, never()).save(any());
  }
  /**
   * Execute creates module when not exists.
   * @throws Exception if an error occurs
   */

  @SuppressWarnings("unchecked")
  @Test
  public void testExecuteCreatesModuleWhenNotExists() throws Exception {
    when(mockOBDal.createCriteria(Module.class)).thenReturn(mockModuleCriteria);
    when(mockModuleCriteria.count()).thenReturn(0);

    Module mockModule = mock(Module.class);
    when(mockOBProvider.get(Module.class)).thenReturn(mockModule);

    ModuleDBPrefix mockDbPrefix = mock(ModuleDBPrefix.class);
    when(mockOBProvider.get(ModuleDBPrefix.class)).thenReturn(mockDbPrefix);

    DataPackage mockDataPackage = mock(DataPackage.class);
    when(mockOBProvider.get(DataPackage.class)).thenReturn(mockDataPackage);

    ModuleDependency mockDep = mock(ModuleDependency.class);
    when(mockOBProvider.get(ModuleDependency.class)).thenReturn(mockDep);

    when(mockOBDal.createCriteria(Language.class)).thenReturn((OBCriteria) mockLanguageCriteria);
    Language mockLang = mock(Language.class);
    lenient().when(mockLanguageCriteria.list()).thenReturn(Collections.singletonList(mockLang));

    Module mockCore = mock(Module.class);
    lenient().when(mockCore.getName()).thenReturn("Core");
    lenient().when(mockCore.getVersion()).thenReturn("1.0.0");
    when(mockOBDal.get(Module.class, "0")).thenReturn(mockCore);

    process.execute(mockBundle);

    verify(mockModule).setName("Custom Module");
    verify(mockModule).setJavaPackage("mySystem.customModule");
    verify(mockModule).setInDevelopment(true);
    verify(mockModule).setVersion("1.0.0");
    verify(mockOBDal).save(mockModule);
    verify(mockOBDal).save(mockDbPrefix);
    verify(mockOBDal).save(mockDataPackage);
    verify(mockOBDal).save(mockDep);
    verify(mockOBDal).commitAndClose();
    verify(mockBundle).setResult(any(OBError.class));
  }
  /**
   * Execute restores previous mode on success.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteRestoresPreviousModeOnSuccess() throws Exception {
    when(mockOBDal.createCriteria(Module.class)).thenReturn(mockModuleCriteria);
    when(mockModuleCriteria.count()).thenReturn(1);

    process.execute(mockBundle);

    obContextStatic.verify(OBContext::restorePreviousMode);
  }
  /**
   * Execute restores previous mode on exception.
   * @throws Exception if an error occurs
   */

  @SuppressWarnings("unchecked")
  @Test
  public void testExecuteRestoresPreviousModeOnException() throws Exception {
    when(mockOBDal.createCriteria(Module.class)).thenThrow(new RuntimeException("DB error"));

    try {
      process.execute(mockBundle);
    } catch (RuntimeException e) {
      // expected
    }

    obContextStatic.verify(OBContext::restorePreviousMode);
  }
}
