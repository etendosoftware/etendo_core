package org.openbravo.test.process.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
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
    int success = 2;
    int errors = 0;

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
    int success = 0;
    int errors = 2;

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
    int success = 1;
    int errors = 1;

    ProcessUtils.massiveMessageHandler(result, inputs, errors, success, originalInput);

    assertEquals(Result.Type.WARNING, result.getType());
    assertEquals(String.format(OBMessageUtils.messageBD(DJOBS_POST_UNPOST_MESSAGE), success, errors),
        result.getMessage());
  }

}
