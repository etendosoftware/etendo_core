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
package org.openbravo.test.security;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.query.Query;
import org.junit.After;
import org.junit.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.test.datasource.BaseDataSourceTestDal;

/**
 * Tries to create a new Product object using a pre-made POST request. If succeeded, that means that
 * no CSRF control has been implemented.
 *
 * @author jarmendariz
 */
public class CSRFAttackTest extends BaseDataSourceTestDal {

  private static final int STATUS_OK = 0;
  private static final String FAKE_PRODUCT_SEARCHKEY = "FAKE_PRODUCT";

  @Test
  public void testRequestAreProtectedAgainstCSRFAttack() {
    assertFalse("Fake product were created. No CSRF check has been done",
        createFakeProductWithNoCSRFToken());
  }

  @Test
  public void testRequestVerifiesSessionCSRFToken() {
    assertTrue("Product should be created", createProduct());
  }

  private boolean createFakeProductWithNoCSRFToken() {
    JSONObject params = this.generateFakeProductParams();

    return requestCreateProduct(params.toString());
  }

  private boolean createProduct() {
    try {
      JSONObject params = this.generateFakeProductParams();
      params.put("csrfToken", getSessionCsrfToken());

      return requestCreateProduct(params.toString());
    } catch (JSONException e) {
      return false;
    }
  }

  private boolean requestCreateProduct(String params) {
    try {
      JSONObject response = new JSONObject(this.doRequest(
          "/org.openbravo.service.datasource/Product?windowId=140&tabId=180&moduleId=0&_operationType=update&_noActiveFilter=true&sendOriginalIDBack=true&_extraProperties=&Constants_FIELDSEPARATOR=%24&_className=OBViewDataSource&Constants_IDENTIFIER=_identifier&isc_dataFormat=json",
          params, 200, "POST", "application/json")).getJSONObject("response");

      return isResponseOk(response);
    } catch (Exception e) {
      return false;
    }

  }

  private JSONObject generateFakeProductParams() {
    try {
      JSONObject params = new JSONObject();
      params.put("operationType", "add");
      params.put("dataSource", "isc_OBViewDataSource_15");
      params.put("oldValues", "{}");

      JSONObject data = new JSONObject();
      data.put("isGeneric", false);
      data.put("purchase", true);
      data.put("sale", true);
      data.put("stocked", true);
      data.put("active", true);
      data.put("organization", "E443A31992CB4635AFCAEABE7183CE85");
      data.put("searchKey", FAKE_PRODUCT_SEARCHKEY);
      data.put("name", "Fake Product");
      data.put("productCategory", "8C9876258C064B7CB6B98EEBCCF7823E");
      data.put("taxCategory", "57B9430EE6DA49EEBEF1AC05B8B4A54C");
      data.put("productType", "I");
      data.put("Product_Org", "E443A31992CB4635AFCAEABE7183CE85");
      data.put("client", "23C59575B9CF467C9620760EB255B389");
      data.put("uOM", "100");

      params.put("data", data);

      return params;
    } catch (JSONException e) {
      return new JSONObject();
    }

  }

  private boolean isResponseOk(JSONObject response) throws JSONException {
    return response.getInt("status") == STATUS_OK;
  }

  @After
  public void removeFakeProduct() {
    StringBuilder delete = new StringBuilder();
    delete.append(" delete from " + Product.ENTITY_NAME);
    delete.append(" where " + Product.PROPERTY_SEARCHKEY + " = :searchKey");

    @SuppressWarnings("rawtypes")
    Query deleteQuery = OBDal.getInstance().getSession().createQuery(delete.toString());
    deleteQuery.setParameter("searchKey", FAKE_PRODUCT_SEARCHKEY);
    deleteQuery.executeUpdate();
  }
}
