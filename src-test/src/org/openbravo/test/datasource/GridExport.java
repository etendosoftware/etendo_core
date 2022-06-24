/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.datasource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.openbravo.test.base.TestConstants.Entities.COUNTRY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.erpCommon.utility.StringCollectionUtils;
import org.openbravo.service.datasource.DataSourceServiceProvider;
import org.openbravo.service.datasource.DefaultDataSourceService;
import org.openbravo.service.json.DefaultJsonDataService.QueryResultWriter;
import org.openbravo.service.json.JsonConstants;

/**
 * Test cases to verify the correct behavior when exporting an standard grid. Note that the tests in
 * this class does not need to execute any request as they are making direct use of the DataSource
 * API.
 */
public class GridExport extends WeldBaseTest {

  @Inject
  private DataSourceServiceProvider dataSourceServiceProvider;

  /** exports a grid filtered by a date column */
  @Test
  public void exportRecordsFilteredByDate() throws Exception {
    TestQueryResultWriter writer = new TestQueryResultWriter(COUNTRY,
        Arrays.asList("iSOCountryCode", "name", "addressPrintFormat", "currency", "hasRegions"));
    exportGrid(writer);
    assertThat(writer.getResults(), equalTo(getExpectedResult()));
  }

  private void exportGrid(TestQueryResultWriter writer) {
    DefaultDataSourceService dataSource = (DefaultDataSourceService) dataSourceServiceProvider
        .getDataSource(writer.getEntityName());

    Map<String, String> parameters = getRequestParameters();
    parameters.put(JsonConstants.NO_ACTIVE_FILTER, "true");
    parameters.put(JsonConstants.SELECTEDPROPERTIES_PARAMETER, writer.getFieldProperties());

    dataSource.fetch(parameters, writer);
  }

  private Map<String, String> getRequestParameters() {
    Map<String, String> params = new HashMap<String, String>();
    params.put("_operationType", "fetch");
    params.put("_noCount", "true");
    params.put("exportAs", "csv");
    params.put("_textMatchStyle", "substring");
    params.put("_UTCOffsetMiliseconds", "7200000");
    params.put("operator", "and");
    params.put("_constructor", "AdvancedCriteria");
    params.put("criteria", getFilterCriteria().toString());
    return params;
  }

  private JSONArray getFilterCriteria() {
    JSONArray criteria = new JSONArray();
    try {
      JSONObject criteriaObj = new JSONObject();
      criteriaObj.put("fieldName", "creationDate");
      criteriaObj.put("operator", "equals");
      criteriaObj.put("value", "2017-12-04");
      criteriaObj.put("minutesTimezoneOffset", "120");
      criteriaObj.put("_constructor", "AdvancedCriteria");
      criteria.put(criteriaObj);
    } catch (JSONException ignore) {
    }
    return criteria;
  }

  private String getExpectedResult() {
    JSONObject country = new JSONObject();
    try {
      country.put("iSOCountryCode", "IM");
      country.put("name", "Isle of Man");
      country.put("addressPrintFormat", "@C@,  @P@");
      country.put("currency", "114");
      country.put("hasRegions", false);
    } catch (JSONException ignore) {
    }
    return country.toString();
  }

  private class TestQueryResultWriter extends QueryResultWriter {
    private String entityName;
    private List<String> fieldProperties;
    private StringBuilder builder;

    TestQueryResultWriter(String entityName, List<String> fieldProperties) {
      this.entityName = entityName;
      this.fieldProperties = fieldProperties;
      builder = new StringBuilder();
    }

    @Override
    public void write(JSONObject row) {
      try {
        JSONObject json = new JSONObject();
        for (String property : fieldProperties) {
          if (!row.has(property)) {
            continue;
          }
          json.put(property, row.get(property));
        }
        builder.append(json.toString());
      } catch (Exception ex) {
        throw new OBException("Could not write results: " + row, ex);
      }
    }

    String getEntityName() {
      return entityName;
    }

    String getFieldProperties() {
      return StringCollectionUtils.commaSeparated(fieldProperties, false);
    }

    String getResults() {
      return builder.toString();
    }
  }
}
