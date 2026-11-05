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
package org.openbravo.service.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.json.DefaultJsonDataService.QueryResultWriter;
import org.openbravo.service.json.JsonConstants;

/**
 * Unit tests for {@link ReadOnlyDataSourceService}, exercised through a small in-test concrete
 * subclass whose data and count are configurable. {@link OBProvider} is mocked statically to
 * return a mocked {@link DataToJsonConverter}, so no DAL or Weld context is needed.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReadOnlyDataSourceServiceTest {

  private static final String NEW_END_ROW = "_newEndRow";
  private static final String OB_SELECTED = "obSelected";
  private static final String NAME = "name";
  private static final String TRUE = "true";

  @Mock
  private OBProvider obProvider;
  @Mock
  private DataToJsonConverter converter;

  private MockedStatic<OBProvider> obProviderStatic;
  private TestService service;
  private Map<String, String> params;

  /**
   * Concrete subclass used to test the abstract service. Each getData call returns the next
   * configured page (the last one is repeated) and records the requested rows.
   */
  private static class TestService extends ReadOnlyDataSourceService {
    private final List<List<Map<String, Object>>> pages = new ArrayList<>();
    private final List<int[]> calls = new ArrayList<>();
    private int count = -1;

    @Override
    protected void addFetchParameters(Map<String, String> parameters) {
      // no-op: avoids DAL/preference access of the default implementation
    }

    @Override
    protected int getCount(Map<String, String> parameters) {
      return count;
    }

    @Override
    protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
        int endRow) {
      calls.add(new int[] { startRow, endRow });
      int idx = Math.min(calls.size() - 1, pages.size() - 1);
      return idx < 0 ? new ArrayList<>() : pages.get(idx);
    }
  }

  /** Wires the mocked converter and prepares a fresh service and parameter map. */
  @Before
  public void setUp() {
    obProviderStatic = mockStatic(OBProvider.class);
    obProviderStatic.when(OBProvider::getInstance).thenReturn(obProvider);
    lenient().when(obProvider.get(DataToJsonConverter.class)).thenReturn(converter);
    lenient().when(converter.convertToJsonObjects(anyList())).thenAnswer(inv -> {
      List<Map<String, Object>> data = inv.getArgument(0);
      List<JSONObject> result = new ArrayList<>();
      for (Map<String, Object> row : data) {
        result.add(new JSONObject(row));
      }
      return result;
    });
    service = new TestService();
    params = new HashMap<>();
  }

  /** Closes the static mocks. */
  @After
  public void tearDown() {
    obProviderStatic.close();
  }

  private static List<Map<String, Object>> rows(int n) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      Map<String, Object> row = new HashMap<>();
      row.put("id", "R" + i);
      list.add(row);
    }
    return list;
  }

  private static List<Map<String, Object>> rowsLastSelected(int n, Object selected) {
    List<Map<String, Object>> list = rows(n);
    list.get(n - 1).put(OB_SELECTED, selected);
    return list;
  }

  private static Map<String, Object> named(Object value) {
    Map<String, Object> row = new HashMap<>();
    if (value != null) {
      row.put(NAME, value);
    }
    return row;
  }

  private JSONObject response(String json) throws Exception {
    return new JSONObject(json).getJSONObject(JsonConstants.RESPONSE_RESPONSE);
  }

  private void setRange(String start, String end) {
    if (start != null) {
      params.put(JsonConstants.STARTROW_PARAMETER, start);
    }
    if (end != null) {
      params.put(JsonConstants.ENDROW_PARAMETER, end);
    }
  }

  /** Without paging parameters the total is the number of rows returned. */
  @Test
  public void fetchWithoutPagingReturnsAllRows() throws Exception {
    service.pages.add(rows(3));

    JSONObject resp = response(service.fetch(params));

    assertEquals(JsonConstants.RPCREQUEST_STATUS_SUCCESS,
        resp.getInt(JsonConstants.RESPONSE_STATUS));
    assertEquals(0, resp.getInt(JsonConstants.RESPONSE_STARTROW));
    assertEquals(2, resp.getInt(JsonConstants.RESPONSE_ENDROW));
    assertEquals(3, resp.getInt(JsonConstants.RESPONSE_TOTALROWS));
    assertEquals(3, resp.getJSONArray(JsonConstants.RESPONSE_DATA).length());
    assertEquals(-1, service.calls.get(0)[0]);
    assertEquals(-1, service.calls.get(0)[1]);
  }

  /** With paging, the count from getCount is used when available. */
  @Test
  public void fetchWithPagingUsesCount() throws Exception {
    setRange("0", "1");
    service.count = 10;
    service.pages.add(rows(2));

    JSONObject resp = response(service.fetch(params));

    assertEquals(10, resp.getInt(JsonConstants.RESPONSE_TOTALROWS));
    assertEquals(0, service.calls.get(0)[0]);
    assertEquals(1, service.calls.get(0)[1]);
  }

  /** Unknown count and fewer rows than the page: total is start plus rows returned. */
  @Test
  public void fetchWithUnknownCountAndShortPage() throws Exception {
    setRange("5", "15");
    service.pages.add(rows(3));

    JSONObject resp = response(service.fetch(params));

    assertEquals(8, resp.getInt(JsonConstants.RESPONSE_TOTALROWS));
    assertEquals(5, resp.getInt(JsonConstants.RESPONSE_STARTROW));
    assertEquals(7, resp.getInt(JsonConstants.RESPONSE_ENDROW));
  }

  /** Unknown count and a full page: total is endRow + 2 so the grid asks for more. */
  @Test
  public void fetchWithUnknownCountAndFullPage() throws Exception {
    setRange("0", "2");
    service.pages.add(rows(3));

    JSONObject resp = response(service.fetch(params));

    assertEquals(4, resp.getInt(JsonConstants.RESPONSE_TOTALROWS));
  }

  /** Only an end row is enough to trigger a count request. */
  @Test
  public void fetchWithOnlyEndRowCounts() throws Exception {
    setRange(null, "5");
    service.count = 7;
    service.pages.add(rows(2));

    JSONObject resp = response(service.fetch(params));

    assertEquals(7, resp.getInt(JsonConstants.RESPONSE_TOTALROWS));
    assertEquals(-1, service.calls.get(0)[0]);
  }

  /** With noCount and one extra row returned, the extra row is dropped and the total bumped. */
  @Test
  public void fetchNoCountTrimsExtraRow() throws Exception {
    setRange("0", "2");
    params.put(JsonConstants.NOCOUNT_PARAMETER, TRUE);
    service.count = 99;
    service.pages.add(rows(3));

    JSONObject resp = response(service.fetch(params));

    assertEquals(4, resp.getInt(JsonConstants.RESPONSE_TOTALROWS));
    assertEquals(1, resp.getInt(JsonConstants.RESPONSE_ENDROW));
    assertEquals(2, resp.getJSONArray(JsonConstants.RESPONSE_DATA).length());
  }

  /** With noCount and fewer rows than requested, nothing is trimmed. */
  @Test
  public void fetchNoCountWithShortPage() throws Exception {
    setRange("0", "10");
    params.put(JsonConstants.NOCOUNT_PARAMETER, TRUE);
    service.pages.add(rows(2));

    JSONObject resp = response(service.fetch(params));

    assertEquals(2, resp.getInt(JsonConstants.RESPONSE_TOTALROWS));
    assertEquals(2, resp.getJSONArray(JsonConstants.RESPONSE_DATA).length());
  }

  /** With noCount and no end row, the total is start plus the rows returned. */
  @Test
  public void fetchNoCountWithoutEndRow() throws Exception {
    setRange("4", null);
    params.put(JsonConstants.NOCOUNT_PARAMETER, TRUE);
    service.pages.add(rows(2));

    JSONObject resp = response(service.fetch(params));

    assertEquals(6, resp.getInt(JsonConstants.RESPONSE_TOTALROWS));
  }

  /** A tableId resolves the entity which is passed to the converter, with extra properties. */
  @Test
  public void fetchResolvesEntityAndAdditionalProperties() {
    ModelProvider modelProvider = org.mockito.Mockito.mock(ModelProvider.class);
    Entity entity = org.mockito.Mockito.mock(Entity.class);
    when(modelProvider.getEntityByTableId("T1")).thenReturn(entity);
    params.put("tableId", "T1");
    params.put(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER, "a, b");
    service.pages.add(rows(1));

    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      service.fetch(params);
    }

    verify(converter).setEntity(entity);
    verify(converter).setAdditionalProperties(Arrays.asList("a", "b"));
  }

  /** Pick and edit flag set to false uses the normal data path. */
  @Test
  public void fetchPickAndEditFalseUsesNormalPath() throws Exception {
    setRange("0", "2");
    params.put(JsonConstants.IS_PICK_AND_EDIT, "false");
    service.count = 5;
    service.pages.add(rowsLastSelected(3, Boolean.TRUE));

    service.fetch(params);

    assertEquals(1, service.calls.size());
    assertNull(params.get(NEW_END_ROW));
  }

  /** Pick and edit with more selected records than the page size loads all selected ones. */
  @Test
  public void fetchPickAndEditWithManySelectedRecords() throws Exception {
    setRange("0", "2");
    params.put(JsonConstants.IS_PICK_AND_EDIT, TRUE);
    service.pages.add(rows(150));

    String json;
    try (MockedStatic<DataSourceUtils> dsu = mockStatic(DataSourceUtils.class)) {
      dsu.when(() -> DataSourceUtils.getNumberOfSelectedRecords(any())).thenReturn(150);
      json = service.fetch(params);
    }

    assertEquals(150, service.calls.get(0)[1]);
    assertEquals("150", params.get(NEW_END_ROW));
    // unknown count: newEndRow + 2
    assertEquals(152, response(json).getInt(JsonConstants.RESPONSE_TOTALROWS));
  }

  /** Pick and edit increases the page size once when the last row of a full page is selected. */
  @Test
  public void fetchPickAndEditIncreasesPageOnce() throws Exception {
    setRange("0", "2");
    params.put(JsonConstants.IS_PICK_AND_EDIT, TRUE);
    params.put(JsonConstants.NOCOUNT_PARAMETER, TRUE);
    service.pages.add(rowsLastSelected(3, Boolean.TRUE));
    service.pages.add(rowsLastSelected(3, Boolean.TRUE));

    JSONObject resp = response(service.fetch(params));

    assertEquals(2, service.calls.size());
    assertEquals(4, service.calls.get(1)[1]);
    assertEquals("4", params.get(NEW_END_ROW));
    assertEquals(3, resp.getJSONArray(JsonConstants.RESPONSE_DATA).length());
  }

  /** Pick and edit stops increasing the page size after the maximum number of attempts. */
  @Test
  public void fetchPickAndEditStopsAtMaxIncrease() {
    setRange("0", "2");
    params.put(JsonConstants.IS_PICK_AND_EDIT, TRUE);
    service.count = 50;
    service.pages.add(rowsLastSelected(20, "Y"));

    service.fetch(params);

    assertEquals(4, service.calls.size());
    assertEquals(8, service.calls.get(3)[1]);
    assertEquals("8", params.get(NEW_END_ROW));
  }

  /** Pick and edit does not increase when the last row is explicitly not selected ("N"). */
  @Test
  public void fetchPickAndEditLastRecordNotSelectedString() {
    assertSingleCallFor(rowsLastSelected(3, "N"));
  }

  /** Pick and edit does not increase when the last row has no selection flag. */
  @Test
  public void fetchPickAndEditLastRecordWithoutFlag() {
    assertSingleCallFor(rows(3));
  }

  /** Pick and edit does not increase when the selection flag has an unsupported type. */
  @Test
  public void fetchPickAndEditLastRecordFlagOfOtherType() {
    assertSingleCallFor(rowsLastSelected(3, Integer.valueOf(1)));
  }

  /** Pick and edit does not increase when the page is not full. */
  @Test
  public void fetchPickAndEditShortPage() {
    assertSingleCallFor(rowsLastSelected(2, Boolean.TRUE));
  }

  /** Pick and edit with empty data and a negative end row checks the empty-data guard. */
  @Test
  public void fetchPickAndEditEmptyData() {
    setRange("0", "-5");
    params.put(JsonConstants.IS_PICK_AND_EDIT, TRUE);
    service.count = 0;

    service.fetch(params);

    assertEquals(1, service.calls.size());
    assertNull(params.get(NEW_END_ROW));
  }

  private void assertSingleCallFor(List<Map<String, Object>> page) {
    setRange("0", "2");
    params.put(JsonConstants.IS_PICK_AND_EDIT, TRUE);
    service.count = 3;
    service.pages.add(page);

    service.fetch(params);

    assertEquals(1, service.calls.size());
    assertNull(params.get(NEW_END_ROW));
  }

  /** The writer variant writes every converted row. */
  @Test
  public void fetchWithWriterWritesEachRow() {
    service.pages.add(rows(3));
    List<JSONObject> written = new ArrayList<>();
    QueryResultWriter writer = new QueryResultWriter() {
      @Override
      public void write(JSONObject json) {
        written.add(json);
      }
    };

    service.fetch(params, writer);

    assertEquals(3, written.size());
  }

  /** remove is not supported. */
  @Test(expected = UnsupportedOperationException.class)
  public void removeIsUnsupported() {
    service.remove(params);
  }

  /** add is not supported. */
  @Test(expected = UnsupportedOperationException.class)
  public void addIsUnsupported() {
    service.add(params, "{}");
  }

  /** update is not supported. */
  @Test(expected = UnsupportedOperationException.class)
  public void updateIsUnsupported() {
    service.update(params, "{}");
  }

  /** Sorting orders string values with null values first. */
  @Test
  public void sortOrdersStringsWithNullsFirst() {
    List<Map<String, Object>> data = new ArrayList<>(
        Arrays.asList(named("b"), named(null), named("a"), named(null)));

    service.sort(NAME, data);

    assertNull(data.get(0).get(NAME));
    assertNull(data.get(1).get(NAME));
    assertEquals("a", data.get(2).get(NAME));
    assertEquals("b", data.get(3).get(NAME));
  }

  /** Sorting places a non-null value after a null one regardless of comparison direction. */
  @Test
  public void sortHandlesNullOnEitherSide() {
    List<Map<String, Object>> data = new ArrayList<>(Arrays.asList(named(null), named("z")));
    service.sort(NAME, data);
    assertNull(data.get(0).get(NAME));

    data = new ArrayList<>(Arrays.asList(named("z"), named(null)));
    service.sort(NAME, data);
    assertNull(data.get(0).get(NAME));
  }

  /** Sorting non-string values keeps the original order (comparator returns 0). */
  @Test
  public void sortNonStringValuesKeepsOrder() {
    List<Map<String, Object>> data = new ArrayList<>(
        Arrays.asList(named(Integer.valueOf(2)), named(Integer.valueOf(1)), named("x")));

    service.sort(NAME, data);

    assertEquals(Integer.valueOf(2), data.get(0).get(NAME));
    assertEquals(Integer.valueOf(1), data.get(1).get(NAME));
    assertEquals("x", data.get(2).get(NAME));
  }

  /** Sorting the same instance against itself returns equal and keeps the list intact. */
  @Test
  public void sortSameValueInstance() {
    String shared = "same";
    List<Map<String, Object>> data = new ArrayList<>(Arrays.asList(named(shared), named(shared)));

    service.sort(NAME, data);

    assertEquals(2, data.size());
    assertSame(shared, data.get(0).get(NAME));
    assertSame(shared, data.get(1).get(NAME));
  }
}
