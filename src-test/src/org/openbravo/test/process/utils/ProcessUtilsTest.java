package org.openbravo.test.process.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  public static final String ERROR = "error";
  public static final String SUCCESS = "success";

  @Override
  @BeforeEach
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

  @AfterEach
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
    OBError message = new OBError();
    message.setType(SUCCESS);

    ProcessUtils.massiveMessageHandler(result, message, inputs, errors, success, originalInput);

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
    OBError message = new OBError();
    message.setType(ERROR);

    ProcessUtils.massiveMessageHandler(result, message, inputs, errors, success, originalInput);

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
    OBError message = new OBError();
    message.setType("warning");

    ProcessUtils.massiveMessageHandler(result, message, inputs, errors, success, originalInput);

    assertEquals(Result.Type.WARNING, result.getType());
    assertEquals(String.format(OBMessageUtils.messageBD(DJOBS_POST_UNPOST_MESSAGE), success, errors),
        result.getMessage());
  }

  @Test
  public void testMassiveMessageHandler_SingleInputError() {
    ActionResult result = new ActionResult();
    List<ShipmentInOut> inputs = List.of(mock(ShipmentInOut.class));
    Data originalInput = mock(Data.class);
    OBError message = new OBError();
    message.setType(ERROR);
    message.setMessage("Error Message");

    ProcessUtils.massiveMessageHandler(result, message, inputs, new MutableInt(0), new MutableInt(0), originalInput);

    assertEquals(Result.Type.ERROR, result.getType());
    assertEquals("Error Message", result.getMessage());
  }

  @Test
  public void testMassiveMessageHandler_SingleInputSuccess() {
    ActionResult result = new ActionResult();
    List<ShipmentInOut> inputs = List.of(mock(ShipmentInOut.class));
    Data originalInput = mock(Data.class);
    OBError message = new OBError();
    message.setType(SUCCESS);
    message.setMessage("Success Message");

    ProcessUtils.massiveMessageHandler(result, message, inputs, new MutableInt(0), new MutableInt(0), originalInput);

    assertEquals(Result.Type.SUCCESS, result.getType());
    assertEquals("Success Message", result.getMessage());
  }

  @Test
  public void testUpdateResult_ErrorMessage() {
    MutableInt errors = new MutableInt(0);
    MutableInt success = new MutableInt(0);
    OBError message = new OBError();
    message.setType(ERROR);

    ProcessUtils.updateResult(message, errors, success);

    assertEquals(1, errors.intValue());
    assertEquals(0, success.intValue());
  }

  @Test
  public void testUpdateResult_SuccessMessage() {
    MutableInt errors = new MutableInt(0);
    MutableInt success = new MutableInt(0);
    OBError message = new OBError();
    message.setType(SUCCESS);

    ProcessUtils.updateResult(message, errors, success);

    assertEquals(0, errors.intValue());
    assertEquals(1, success.intValue());
  }

}
