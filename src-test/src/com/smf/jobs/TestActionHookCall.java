package com.smf.jobs;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.test.base.TestConstants;

/**
 * Test class for the TestAction class.
 */
public class TestActionHookCall extends WeldBaseTest {

  @Before
  public void setUp() throws Exception {
    super.setUp();

    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP_NORTE);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(), OBContext.getOBContext().getCurrentOrganization().getId());

  }

  /**
   * Tests that the onSave method does not throw an exception when the event is not valid.
   */
  @Test
  public void execTestAction() throws JSONException, NoSuchFieldException, IllegalAccessException {
    TestAction testAct = WeldUtils.getInstanceFromStaticBeanManager(TestAction.class);

    JSONObject contentJson = new JSONObject();
    contentJson.put("_entityName", "ADClient");
    contentJson.put("recordIds", new JSONArray().put("0"));
    contentJson.put("_params", new JSONObject().put("myparam", "dummy"));

    String content = contentJson.toString();
    HashMap<String, Object> params = new HashMap<String, Object>();
    JSONObject response = testAct.doExecute(params, content);
    HashMap<String, Boolean> data = SingletonToTestHooks.getInstance().getMetadata();
    assertTrue(data.get("propAddedByPreHook"));
    assertTrue(data.get("actionExecuted"));
    assertTrue(data.get("propAddedByPostHook"));
  }
}