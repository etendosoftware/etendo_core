package org.openbravo.test.process.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.commons.lang.mutable.MutableInt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.TestConstants;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Data;
import com.smf.jobs.Result;
import com.smf.jobs.defaults.Utils.ProcessUtils;

public class ProcessUtilsTest extends OBBaseTest {

  public static final String DJOBS_POST_UNPOST_MESSAGE = "DJOBS_PostUnpostMessage";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP_NORTE);
    OBContext currentContext = OBContext.getOBContext();
    VariablesSecureApp vsa = new VariablesSecureApp(currentContext.getUser().getId(),
        currentContext.getCurrentClient().getId(), currentContext.getCurrentOrganization().getId(),
        currentContext.getRole().getId());
    RequestContext.get().setVariableSecureApp(vsa);
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void cleanUp() throws Exception {
    OBContext.setOBContext((OBContext) null);
  }

  @Test
  public void testMassiveMessageHandler_AllSuccess() {
    ActionResult result = new ActionResult();
    List<ShipmentInOut> inputs = List.of(mock(ShipmentInOut.class), mock(ShipmentInOut.class));
    Data originalInput = mock(Data.class);
    var errors = new MutableInt(0);
    var success = new MutableInt(2);

    ProcessUtils.massiveMessageHandler(result, inputs, errors, success, originalInput);

    assertEquals(Result.Type.SUCCESS, result.getType());
    assertEquals(String.format(OBMessageUtils.messageBD(DJOBS_POST_UNPOST_MESSAGE), success, errors),
        result.getMessage());
  }

  @Test
  public void testMassiveMessageHandler_AllErrors() {
    ActionResult result = new ActionResult();
    List<ShipmentInOut> inputs = List.of(mock(ShipmentInOut.class), mock(ShipmentInOut.class));
    Data originalInput = mock(Data.class);
    var errors = new MutableInt(2);
    var success = new MutableInt(0);

    ProcessUtils.massiveMessageHandler(result, inputs, errors, success, originalInput);

    assertEquals(Result.Type.ERROR, result.getType());
    assertEquals(String.format(OBMessageUtils.messageBD(DJOBS_POST_UNPOST_MESSAGE), success, errors),
        result.getMessage());
  }

  @Test
  public void testMassiveMessageHandler_MixedResults() {
    ActionResult result = new ActionResult();
    List<ShipmentInOut> inputs = List.of(mock(ShipmentInOut.class), mock(ShipmentInOut.class));
    Data originalInput = mock(Data.class);
    var errors = new MutableInt(1);
    var success = new MutableInt(1);

    ProcessUtils.massiveMessageHandler(result, inputs, errors, success, originalInput);

    assertEquals(Result.Type.WARNING, result.getType());
    assertEquals(String.format(OBMessageUtils.messageBD(DJOBS_POST_UNPOST_MESSAGE), success, errors),
        result.getMessage());
  }

  @Test
  public void testUpdateResult_ErrorMessage() {
    testUpdateResultHelper("Error", "Error Title", "Error Message", 1, 0, "Error Title: Error Message");
  }

  @Test
  public void testUpdateResult_SuccessMessage() {
    testUpdateResultHelper("Success", "", "Success Message", 0, 1, "Success Message");
  }

  @Test
  public void testUpdateResult_OtherTypeMessage() {
    testUpdateResultHelper("Warning", "Warning Title", "Warning Message", 0, 0, "Warning Title: Warning Message");
  }

  private void testUpdateResultHelper(String type, String title, String message, int expectedErrors, int expectedSuccess, String expectedResultMessage) {
    ActionResult result = new ActionResult();
    OBError obMessage = new OBError();
    obMessage.setType(type);
    obMessage.setTitle(title);
    obMessage.setMessage(message);
    MutableInt errors = new MutableInt(0);
    MutableInt success = new MutableInt(0);

    ProcessUtils.updateResult(result, obMessage, errors, success);

    assertEquals(expectedErrors, errors.intValue());
    assertEquals(expectedSuccess, success.intValue());
    assertEquals(expectedResultMessage, result.getMessage());
  }

}
