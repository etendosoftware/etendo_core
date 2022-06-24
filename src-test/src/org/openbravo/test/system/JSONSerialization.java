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
package org.openbravo.test.system;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.scheduling.ParameterSerializationException;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessContext;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test cases used to ensure the correct JSON serialization of different objects.
 */
public class JSONSerialization extends OBBaseTest {

  private static final String PROCESS_CONTEXT = "{\"org.openbravo.scheduling.ProcessContext\":"
      + "{\"user\":\"4028E6C72959682B01295A0735CB0120\",\"role\":\"\",\"language\":\"en_US\","
      + "\"theme\":\"ltr\\/org.openbravo.userinterface.skin.250to300Comp\\/250to300Comp\","
      + "\"client\":\"4028E6C72959682B01295A070852010D\",\"organization\":\"357947E87C284935AD1D783CF6F099A1\","
      + "\"warehouse\":\"\",\"command\":\"DEFAULT\",\"userClient\":\"\","
      + "\"userOrganization\":\"\",\"dbSessionID\":\"\",\"javaDateFormat\":\"\",\"jsDateFormat\":\"\","
      + "\"sqlDateFormat\":\"\",\"accessLevel\":\"\",\"roleSecurity\":true}}";

  private static final String PB_PARAMS = "{\"mChValueId\":\"9E16B9CCFC7B4836B72840F0AF68C151\",\"mProductId\":\"\"}";
  private static final String PB_PARAMS_WRONG = "[]";
  private static final String M_CH_VALUE_ID = "9E16B9CCFC7B4836B72840F0AF68C151";
  private static final String AD_PROCESS_ID = "58591E3E0F7648E4A09058E037CE49FC";

  private static final String OB_ERROR = "{\"OBError\":{\"title\":\"Title\",\"message\":\"Message\",\"type\":\"Error\"}}";

  /** ProcessContext is correctly serialized */
  @Test
  public void serializeProcessContext() {
    setUserContext(QA_TEST_ADMIN_USER_ID);
    ProcessContext processContext = new ProcessContext(getVars());
    assertThat(processContext.toString(), equalTo(PROCESS_CONTEXT));
  }

  /** Test correct deserialization of a JSONObject containing a ProcessContext definition */
  @Test
  public void deserializeProcessContext() {
    String obContext = OBDal.getInstance()
        .getSession()
        .createQuery(
            "SELECT openbravoContext FROM ProcessRequest WHERE id = '078147FA19124BA69786EA7374807D0D'",
            String.class)
        .uniqueResult();
    ProcessContext processContext = ProcessContext.newInstance(obContext);

    List<Object> collection = Arrays.asList(processContext.getUser(), processContext.getRole(),
        processContext.getLanguage(), processContext.getTheme(), processContext.getClient(),
        processContext.getOrganization(), processContext.getWarehouse(),
        processContext.getCommand(), processContext.getUserClient(),
        processContext.getUserOrganization(), processContext.getDbSessionID(),
        processContext.getJavaDateFormat(), processContext.getJavaDateTimeFormat(),
        processContext.getJsDateFormat(), processContext.getSqlDateFormat(),
        processContext.getAccessLevel(), processContext.isRoleSecurity());

    assertThat(collection, contains("100", "4028E6C72959682B01295A071429011E", "en_US",
        "ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp",
        "4028E6C72959682B01295A070852010D", "0", "4028E6C72959682B01295ECFEF4502A0",
        "SAVE_BUTTONProcessing100", "'4028E6C72959682B01295A070852010D'",
        "'5EFF95EB540740A3B10510D9814EFAD5','43D590B4814049C6B85C6545E8264E37','0','357947E87C284935AD1D783CF6F099A1'",
        "A9220D77CB54469A99320051BB0D74C5", "dd-MM-yyyy", "dd-MM-yyyy HH:mm:ss", "%d-%m-%Y",
        "DD-MM-YYYY", "3", true));
  }

  /** Test consistency of ProcessContext serialization */
  @Test
  public void isConsistentSerialization() {
    ProcessContext processContext = ProcessContext.newInstance(PROCESS_CONTEXT);
    assertThat(processContext.toString(), equalTo(PROCESS_CONTEXT));
  }

  /**
   * ProcessBundle parameters are correctly serialized
   * 
   * @throws ServletException
   */
  @Test
  public void serializeProcessBundleParameters() throws ServletException {
    ProcessBundle pb = new ProcessBundle(AD_PROCESS_ID, getVars())
        .init(new DalConnectionProvider(false));

    HashMap<String, Object> parameters = new HashMap<>();
    parameters.put("mProductId", "");
    parameters.put("mChValueId", M_CH_VALUE_ID);
    pb.setParams(parameters);

    assertThat(pb.getParamsDeflated(), equalTo(PB_PARAMS));
  }

  /**
   * ProcessBundle parameters are correctly deserialized
   * 
   * @throws ServletException
   */
  @Test
  public void deserializeProcessBundleParameters() throws ServletException {
    String processRequestId = null;
    try {
      processRequestId = createProcessRequest(PB_PARAMS);
      ProcessBundle pb = ProcessBundle.request(processRequestId, getVars(),
          new DalConnectionProvider(false));
      assertThat(pb.getParams().get("mProductId"), equalTo(""));
      assertThat(pb.getParams().get("mChValueId"), equalTo(M_CH_VALUE_ID));
    } finally {
      removeProcessRequest(processRequestId);
    }
  }

  /**
   * Expected exception is thrown when ProcessBundle parameters serialization fails
   * 
   * @throws ServletException
   */
  @Test(expected = ParameterSerializationException.class)
  public void exceptionIsThrownWhenSerializationFails() throws ServletException {
    ProcessBundle pb = new ProcessBundle(AD_PROCESS_ID, getVars())
        .init(new DalConnectionProvider(false));

    HashMap<String, Object> parameters = new HashMap<>();
    parameters.put("unsupportedParam", Collections.emptyList());
    pb.setParams(parameters);

    pb.getParamsDeflated(); // should throw ParameterSerializationException

  }

  /**
   * Expected exception is thrown when ProcessBundle parameters deserialization fails
   * 
   * @throws ServletException
   */
  @Test(expected = ParameterSerializationException.class)
  public void exceptionIsThrownWhenDeserializationFails() throws ServletException {
    String processRequestId = null;
    try {
      processRequestId = createProcessRequest(PB_PARAMS_WRONG);
      ProcessBundle.request(processRequestId, getVars(), new DalConnectionProvider(false));
    } finally {
      removeProcessRequest(processRequestId);
    }
  }

  /** OBError is serialized as expected */
  @Test
  public void serializeOBError() {
    OBError error = new OBError();
    error.setType("Error");
    error.setTitle("Title");
    error.setMessage("Message");
    assertThat(error.toJSON().toString(), equalTo(OB_ERROR));
  }

  private VariablesSecureApp getVars() {
    return new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId());
  }

  private String createProcessRequest(String parameters) {
    ProcessRequest pr = OBProvider.getInstance().get(ProcessRequest.class);
    pr.setProcess(
        OBDal.getInstance().getProxy(org.openbravo.model.ad.ui.Process.class, AD_PROCESS_ID));
    pr.setParams(parameters);
    OBDal.getInstance().save(pr);
    OBDal.getInstance().commitAndClose();
    return pr.getId();
  }

  private void removeProcessRequest(String processRequestId) {
    if (processRequestId == null) {
      return;
    }
    OBDal.getInstance()
        .remove(OBDal.getInstance().getProxy(ProcessRequest.class, processRequestId));
    OBDal.getInstance().commitAndClose();
  }

}
