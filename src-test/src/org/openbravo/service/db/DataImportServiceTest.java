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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for {@link DataImportService}.
 */
@DisplayName("DataImportService")
public class DataImportServiceTest {

  // ── getInstance / setInstance ─────────────────────────────────────────

  @Nested
  @DisplayName("Singleton getInstance / setInstance")
  class SingletonTests {

    @Test
    @DisplayName("getInstance returns instance via OBProvider")
    void getInstanceReturnsInstance() {
      DataImportService mockService = mock(DataImportService.class);

      try (MockedStatic<OBProvider> providerMock = mockStatic(OBProvider.class)) {
        OBProvider mockProvider = mock(OBProvider.class);
        providerMock.when(OBProvider::getInstance).thenReturn(mockProvider);
        when(mockProvider.get(DataImportService.class)).thenReturn(mockService);

        // Reset the static instance to null so getInstance fetches via OBProvider
        DataImportService.setInstance(null);
        DataImportService result = DataImportService.getInstance();
        assertNotNull(result);
        assertEquals(mockService, result);
      }
    }

    @Test
    @DisplayName("setInstance changes singleton")
    void setInstanceChangesSingleton() {
      DataImportService custom = mock(DataImportService.class);
      DataImportService.setInstance(custom);
      assertEquals(custom, DataImportService.getInstance());

      // Clean up
      DataImportService.setInstance(null);
    }
  }

  // ── importDataFromXML error cases ─────────────────────────────────────

  @Nested
  @DisplayName("importDataFromXML with invalid XML")
  class ImportDataFromXML {

    @Test
    @DisplayName("null XML throws OBException")
    void nullXmlThrows() {
      DataImportService service = new DataImportService();
      Client mockClient = mock(Client.class);
      Organization mockOrg = mock(Organization.class);

      assertThrows(OBException.class,
          () -> service.importDataFromXML(mockClient, mockOrg, null));
    }

    @Test
    @DisplayName("empty XML throws OBException")
    void emptyXmlThrows() {
      DataImportService service = new DataImportService();
      Client mockClient = mock(Client.class);
      Organization mockOrg = mock(Organization.class);

      assertThrows(OBException.class,
          () -> service.importDataFromXML(mockClient, mockOrg, ""));
    }
  }
}
