package org.openbravo.test.utils;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.test.base.TestConstants;

/**
 * Utility methods for tests.
 */
public class Utils {

  /**
   * Private constructor to prevent instantiation of the utility class.
   */
  private Utils() {
  }

  /**
   * Initializes the context for tests. This sets the user to the admin user and the client to the
   * F&B Group client.
   */
  public static void initializeTestContext() {
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP);

    OBContext currentContext = OBContext.getOBContext();
    VariablesSecureApp vsa = new VariablesSecureApp(currentContext.getUser().getId(),
        currentContext.getCurrentClient().getId(), currentContext.getCurrentOrganization().getId(),
        currentContext.getRole().getId());

    RequestContext.get().setVariableSecureApp(vsa);
  }
}
