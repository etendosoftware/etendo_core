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
 * All portions are Copyright (C) 2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.datasource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.CostAdjustmentLine;

/**
 * Test case for LinkToParent tree Datasource
 *
 * @author jarmendariz
 */
public class LinkToParentTreeDataSourceTest extends BaseDataSourceTestDal {

  private static final Logger log = LogManager.getLogger();
  private static final int STATUS_OK = 0;
  private static final int NUMBER_OF_COST_ADJUSTMENT_LINES = 2;

  private CostAdjustmentTestDataHelper dataHelper = new CostAdjustmentTestDataHelper();
  private String costAdjustmentId;

  /**
   * Ensure that fetching cost adjustment lines both active and non-active lines are retrieved
   */
  @Test
  public void fetchIncludeNonActiveRecords() {
    OBContext.setOBContext(TEST_USER_ID);
    this.costAdjustmentId = this.dataHelper.createCostAdjustmentWithActiveAndNonActiveLines();

    assertThat("Fetched the expected number of records", this.getNumberOfCostAdjustmentLines(),
        equalTo(NUMBER_OF_COST_ADJUSTMENT_LINES));
  }

  /**
   * Test the case where the parent node has a children which is inactive. The parent should return
   * it has children in its status
   */
  @Test
  public void fetchIncludeHasChildrenHavingNonActiveChildren() {
    OBContext.setOBContext(TEST_USER_ID);
    this.costAdjustmentId = this.dataHelper.createCostAdjustmentWithANonActiveChildLine();

    assertThat("First node has children", this.doesFirstCostAdjustmentLineHasChildren(),
        equalTo(true));
  }

  @After
  public void tearDownLinkToParentData() {
    this.dataHelper.removeCostAdjustment(this.costAdjustmentId);
    this.costAdjustmentId = null;
  }

  private int getNumberOfCostAdjustmentLines() {
    try {
      JSONObject response = this.requestCostAdjustmentLines();
      if (this.isResponseOk(response)) {
        return this.getNumberOfDataItems(response);
      } else {
        log.error("DataSource response has a non-OK status");
        return 0;
      }
    } catch (Exception exception) {
      log.error("Cost Adjustment request from DataSource failed", exception);
      return 0;
    }
  }

  private boolean doesFirstCostAdjustmentLineHasChildren() {
    try {
      JSONObject response = this.requestCostAdjustmentLines();
      if (this.isResponseOk(response)) {
        return this.lineHasChildren(this.getFirstItem(response));
      } else {
        log.error("DataSource response has a non-OK status");
        return false;
      }
    } catch (Exception exception) {
      log.error("Cost Adjustment request from DataSource failed", exception);
      return false;
    }
  }

  private boolean isResponseOk(JSONObject response) throws JSONException {
    return response.getInt("status") == STATUS_OK;
  }

  private int getNumberOfDataItems(JSONObject response) throws JSONException {
    return response.getJSONArray("data").length();
  }

  private JSONObject getFirstItem(JSONObject response) throws JSONException {
    return response.getJSONArray("data").getJSONObject(0);
  }

  private boolean lineHasChildren(JSONObject item) throws JSONException {
    return item.getBoolean("_hasChildren");
  }

  private JSONObject requestCostAdjustmentLines() throws Exception {
    Map<String, String> params = this.generateCostAdjustmentLinesParams(this.costAdjustmentId);

    return new JSONObject(this.doRequest(
        "/org.openbravo.service.datasource/610BEAE5E223447DBE6FF672B703F72F", params, 200, "POST"))
            .getJSONObject("response");
  }

  private Map<String, String> generateCostAdjustmentLinesParams(String id) {
    Map<String, String> params = new HashMap<>();
    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "200");
    params.put("referencedTableId", "34E79323CEC847C2A9ED2C8430AC73D1");
    params.put("parentRecordId", id);
    params.put("tabId", "06DCB72BB6D24F82BCDA5FFF8EA0425C");
    params.put("@CostAdjustment.id@", id);
    params.put("criteria",
        "{\"_constructor\":\"AdvancedCriteria\",\"fieldName\":\"parentId\",\"value\":\"-1\",\"operator\":\"equals\"}");

    return params;
  }

  private class CostAdjustmentTestDataHelper {

    private static final String DOCUMENT_TYPE_ID = "82000D718BDA40C38F83FA1A5FFF6419";
    private static final String SOURCE_PROCESS = "MCC";
    private static final String DOCUMENT_NO = "::DOCUMENT-NO::";

    public String createCostAdjustmentWithActiveAndNonActiveLines() {
      try {
        OBContext.setAdminMode(false);

        CostAdjustment costAdjustment = this.createCostAdjustment();
        this.createActiveCostAdjustmentLine(costAdjustment);
        this.createNonActiveCostAdjustmentLine(costAdjustment);

        OBDal.getInstance().commitAndClose();

        return costAdjustment.getId();
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    public String createCostAdjustmentWithANonActiveChildLine() {
      try {
        OBContext.setAdminMode(false);

        CostAdjustment costAdjustment = this.createCostAdjustment();
        CostAdjustmentLine parentAdjustmentLine = this
            .createActiveCostAdjustmentLine(costAdjustment);
        this.createNonActiveChildAdjustmentLine(costAdjustment, parentAdjustmentLine);
        OBDal.getInstance().commitAndClose();

        return costAdjustment.getId();
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    public void removeCostAdjustment(String id) {
      try {
        OBContext.setAdminMode(false);
        OBDal obdal = OBDal.getInstance();
        obdal.remove(obdal.getProxy(CostAdjustment.class, id));
        obdal.commitAndClose();
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    private CostAdjustment createCostAdjustment() {
      OBDal obdal = OBDal.getInstance();

      CostAdjustment costAdjustment = OBProvider.getInstance().get(CostAdjustment.class);
      costAdjustment.setDocumentType(obdal.getProxy(DocumentType.class, DOCUMENT_TYPE_ID));
      costAdjustment.setDocumentNo(DOCUMENT_NO);
      costAdjustment.setSourceProcess(SOURCE_PROCESS);

      obdal.save(costAdjustment);

      return costAdjustment;
    }

    private CostAdjustmentLine createActiveCostAdjustmentLine(CostAdjustment costAdjustment) {
      return this.createCostAdjustmentLine(costAdjustment, null, 100L, true);
    }

    private CostAdjustmentLine createNonActiveCostAdjustmentLine(CostAdjustment costAdjustment) {
      return this.createCostAdjustmentLine(costAdjustment, null, 200L, false);
    }

    private CostAdjustmentLine createNonActiveChildAdjustmentLine(CostAdjustment costAdjustment,
        CostAdjustmentLine parent) {
      return this.createCostAdjustmentLine(costAdjustment, parent, 200L, false);
    }

    private CostAdjustmentLine createCostAdjustmentLine(CostAdjustment costAdjustment,
        CostAdjustmentLine parent, Long lineNo, boolean isActive) {
      OBDal obdal = OBDal.getInstance();

      CostAdjustmentLine line = OBProvider.getInstance().get(CostAdjustmentLine.class);
      line.setLineNo(lineNo);
      line.setCostAdjustment(costAdjustment);
      line.setCurrency(obdal.getProxy(Currency.class, EURO_ID));
      line.setActive(isActive);
      line.setParentCostAdjustmentLine(parent);

      obdal.save(line);

      return line;
    }

  }
}
