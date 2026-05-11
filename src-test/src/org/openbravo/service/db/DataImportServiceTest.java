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
package org.openbravo.service.db;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.provider.OBProvider;

/**
 * Unit tests for {@link DataImportService}.
 */
@DisplayName("DataImportService Tests")
class DataImportServiceTest {

  private MockedStatic<OBProvider> obProviderMock;
  private OBProvider providerInstance;

  @BeforeEach
  void setUp() {
    // Reset the singleton instance before each test
    DataImportService.setInstance(null);

    providerInstance = mock(OBProvider.class);
    obProviderMock = mockStatic(OBProvider.class);
    obProviderMock.when(OBProvider::getInstance).thenReturn(providerInstance);
  }

  @AfterEach
  void tearDown() {
    obProviderMock.close();
    DataImportService.setInstance(null);
  }

  @Nested
  @DisplayName("getInstance tests")
  class GetInstanceTests {

    @Test
    @DisplayName("Should return non-null instance when OBProvider provides one")
    void shouldReturnNonNullInstance() {
      DataImportService expected = new DataImportService();
      when(providerInstance.get(DataImportService.class)).thenReturn(expected);

      DataImportService result = DataImportService.getInstance();

      assertNotNull(result);
      assertSame(expected, result);
    }

    @Test
    @DisplayName("Should return same instance on subsequent calls")
    void shouldReturnSameInstanceOnSubsequentCalls() {
      DataImportService expected = new DataImportService();
      when(providerInstance.get(DataImportService.class)).thenReturn(expected);

      DataImportService first = DataImportService.getInstance();
      DataImportService second = DataImportService.getInstance();

      assertSame(first, second);
    }
  }

  @Nested
  @DisplayName("setInstance tests")
  class SetInstanceTests {

    @Test
    @DisplayName("Should change the singleton instance")
    void shouldChangeInstance() {
      DataImportService custom = new DataImportService();
      DataImportService.setInstance(custom);

      // getInstance should now return the custom instance without calling OBProvider
      DataImportService result = DataImportService.getInstance();
      assertSame(custom, result);
    }

    @Test
    @DisplayName("Should allow setting a different instance")
    void shouldAllowSettingDifferentInstance() {
      DataImportService first = new DataImportService();
      DataImportService second = new DataImportService();

      DataImportService.setInstance(first);
      assertSame(first, DataImportService.getInstance());

      DataImportService.setInstance(second);
      assertSame(second, DataImportService.getInstance());
    }
  }

  @Nested
  @DisplayName("importDataFromXML tests")
  class ImportDataFromXMLTests {

    @Test
    @DisplayName("Should throw exception when called with null client, null org and empty XML")
    void shouldThrowExceptionWithNullClientNullOrgAndEmptyXml() {
      DataImportService service = new DataImportService();
      DataImportService.setInstance(service);

      assertThrows(Exception.class, () -> service.importDataFromXML(null, null, ""));
    }

    @Test
    @DisplayName("Should throw exception when called with null client, null org and null XML")
    void shouldThrowExceptionWithNullClientNullOrgAndNullXml() {
      DataImportService service = new DataImportService();
      DataImportService.setInstance(service);

      assertThrows(Exception.class, () -> service.importDataFromXML(null, null, null));
    }

    @Test
    @DisplayName("Should throw exception when called with null client, null org and invalid XML")
    void shouldThrowExceptionWithNullClientNullOrgAndInvalidXml() {
      DataImportService service = new DataImportService();
      DataImportService.setInstance(service);

      assertThrows(Exception.class, () -> service.importDataFromXML(null, null, "not-xml"));
    }
  }
}
