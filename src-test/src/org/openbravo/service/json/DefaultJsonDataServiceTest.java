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
package org.openbravo.service.json;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultJsonDataService}.
 */
@DisplayName("DefaultJsonDataService Tests")
class DefaultJsonDataServiceTest {

  @Nested
  @DisplayName("convertParameterToString tests")
  class ConvertParameterToStringTests {

    @Test
    @DisplayName("Should return empty string for empty map")
    void shouldReturnEmptyStringForEmptyMap() {
      Map<String, String> params = new HashMap<>();
      String result = DefaultJsonDataService.convertParameterToString(params);
      assertEquals("", result);
    }

    @Test
    @DisplayName("Should return single key-value pair for single entry map")
    void shouldReturnSinglePairForSingleEntry() {
      Map<String, String> params = new HashMap<>();
      params.put("key1", "value1");
      String result = DefaultJsonDataService.convertParameterToString(params);
      assertTrue(result.contains("key1:value1"));
    }

    @Test
    @DisplayName("Should return all key-value pairs for multiple entry map")
    void shouldReturnAllPairsForMultipleEntries() {
      Map<String, String> params = new HashMap<>();
      params.put("key1", "value1");
      params.put("key2", "value2");
      params.put("key3", "value3");
      String result = DefaultJsonDataService.convertParameterToString(params);
      assertTrue(result.contains("key1:value1"));
      assertTrue(result.contains("key2:value2"));
      assertTrue(result.contains("key3:value3"));
    }
  }

  @Nested
  @DisplayName("DataSourceAction enum tests")
  class DataSourceActionTests {

    @Test
    @DisplayName("Should have FETCH value")
    void shouldHaveFetchValue() {
      assertNotNull(DefaultJsonDataService.DataSourceAction.FETCH);
    }

    @Test
    @DisplayName("Should have ADD value")
    void shouldHaveAddValue() {
      assertNotNull(DefaultJsonDataService.DataSourceAction.ADD);
    }

    @Test
    @DisplayName("Should have UPDATE value")
    void shouldHaveUpdateValue() {
      assertNotNull(DefaultJsonDataService.DataSourceAction.UPDATE);
    }

    @Test
    @DisplayName("Should have REMOVE value")
    void shouldHaveRemoveValue() {
      assertNotNull(DefaultJsonDataService.DataSourceAction.REMOVE);
    }

    @Test
    @DisplayName("Should contain exactly four values")
    void shouldContainExactlyFourValues() {
      assertEquals(4, DefaultJsonDataService.DataSourceAction.values().length);
    }
  }

  @Nested
  @DisplayName("QueryResultWriter tests")
  class QueryResultWriterTests {

    @Test
    @DisplayName("Should allow subclassing of QueryResultWriter")
    void shouldAllowSubclassing() {
      DefaultJsonDataService.QueryResultWriter writer = new DefaultJsonDataService.QueryResultWriter() {
        @Override
        public void write(JSONObject json) {
          // no-op implementation for test
        }
      };
      assertNotNull(writer);
    }

    @Test
    @DisplayName("Subclass write method should be callable without error")
    void subclassWriteMethodShouldBeCallable() {
      DefaultJsonDataService.QueryResultWriter writer = new DefaultJsonDataService.QueryResultWriter() {
        @Override
        public void write(JSONObject json) {
          // no-op implementation for test
        }
      };
      assertDoesNotThrow(() -> writer.write(new JSONObject()));
    }
  }
}
