package org.openbravo.test.materialMgmt.linevalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.openbravo.test.materialMgmt.linevalidation.GoodMovementUtils.createGoodsMovement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.TestConstants;

/**
 * Test class for validating Goods Movement processes.
 */
public class GoodMovementTest extends OBBaseTest {

  /**
   * Sets up the test environment before each test execution.
   *
   * @throws Exception
   *     if an error occurs during setup.
   */
  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP_NORTE);
    OBContext currentContext = OBContext.getOBContext();
    VariablesSecureApp vsa = new VariablesSecureApp(currentContext.getUser().getId(),
        currentContext.getCurrentClient().getId(), currentContext.getCurrentOrganization().getId(),
        currentContext.getRole().getId());
    RequestContext.get().setVariableSecureApp(vsa);
  }

  /**
   * Tests that processing an Internal Movement without lines should fail.
   * It ensures that the system returns an error message when trying to process
   * a Goods Movement document with no associated movement lines.
   */
  @Test
  public void testProcessInternalMovementWithoutLinesshouldFail() {
    InternalMovement goodsMovement = createGoodsMovement(OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization());
    try {
      OBError response = GoodMovementUtils.processGoodsMovement(goodsMovement.getId());
      assertEquals("Error", response.getType());
      assertEquals(response.getMessage(), OBMessageUtils.messageBD("NoLinesForMovement"));
    } catch (Exception e) {
      fail("Expected OBException to be thrown");
    }
  }

  /**
   * Cleans up the database state after each test execution.
   * Ensures that no data is persisted between tests.
   */
  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }
}
