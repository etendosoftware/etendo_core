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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.service.json.DefaultJsonDataService.DataSourceAction;
import org.openbravo.test.base.OBBaseTest;

/**
 * Integration tests for {@link DefaultJsonDataService}.
 * <p>
 * Note: Tests that call static methods on DefaultJsonDataService (getInstance,
 * convertParameterToString, fetch) are excluded because the class static initializer
 * requires the Weld CDI container (ServletContext), which is not available in
 * standalone test execution.
 */
public class DefaultJsonDataServiceIntegrationTest extends OBBaseTest {

  // --- DataSourceAction enum ---

  @Test
  public void testDataSourceActionValues() {
    DataSourceAction[] values = DataSourceAction.values();
    assertEquals(4, values.length);
  }

  @Test
  public void testDataSourceActionFetch() {
    assertEquals(DataSourceAction.FETCH, DataSourceAction.valueOf("FETCH"));
  }

  @Test
  public void testDataSourceActionAdd() {
    assertEquals(DataSourceAction.ADD, DataSourceAction.valueOf("ADD"));
  }

  @Test
  public void testDataSourceActionUpdate() {
    assertEquals(DataSourceAction.UPDATE, DataSourceAction.valueOf("UPDATE"));
  }

  @Test
  public void testDataSourceActionRemove() {
    assertEquals(DataSourceAction.REMOVE, DataSourceAction.valueOf("REMOVE"));
  }

  // --- QueryResultWriter ---

  @Test
  public void testQueryResultWriterCanBeSubclassed() {
    DefaultJsonDataService.QueryResultWriter writer = new DefaultJsonDataService.QueryResultWriter() {
      @Override
      public void write(JSONObject json) {
        // no-op for test
      }
    };
    assertNotNull(writer);
    // Should not throw
    writer.write(new JSONObject());
  }
}
