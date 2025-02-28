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
 * All portions are Copyright (C) 2015-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.test.db.model.triggers;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.advpaymentmngt.test.TestUtility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.Process;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.module.Module;
import org.openbravo.service.db.DbUtility;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test class for testing the OBUIAPP_Process functionality.
 */
public class ProcessTest extends OBBaseTest {

  private static final String USER_INTERFACE_APP_MOD = "9BA0836A3CD74EE4AB48753A47211BCC";
  private static final String OBUIAPP_PROCESS_ID = "EBC24A55293F4E4BAF56EF8DFA43D578"; // RegisterModule Process Definition
  private static final String MESSAGE_MODULE_NOT_IN_DEVELOPMENT = "20532";
  private static final String MESSAGE_INSERT_OR_DELETE_MODULE_NOT_IN_DEVELOPMENT = "20533";
  private static final String MESSAGE_MOVE_TO_MODULE_NOT_IN_DEVELOPMENT = "ChangeNotInDevModule";
  private static final String ERROR_MESSAGE = "Modifications in a module marked as development are not allowed.";
  private static final String RDBMS_ORACLE = "ORACLE";
  private String rdbms = null;
  /**
   * Sets up the test context before each test.
   *
   * @throws Exception if an error occurs during setup
   */
  @Before
  public void setUpRP() throws Exception {
    super.setUp();
    TestUtility.setTestContext();
    VariablesSecureApp vsa = new VariablesSecureApp(
        OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId(),
        OBContext.getOBContext().getRole().getId()
    );
    RequestContext.get().setVariableSecureApp(vsa);
    vsa.setSessionValue("#FormatOutput|generalQtyEdition", "#0.######");
    vsa.setSessionValue("#GroupSeparator|generalQtyEdition", ",");
    vsa.setSessionValue("#DecimalSeparator|generalQtyEdition", ".");
    rdbms = getConnectionProvider().getRDBMS();
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_1() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setActive(false);
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the search key of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_2() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setSearchKey("Demo Search Key");
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the name of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_3() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setName("TestName");
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the description of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_4() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setDescription("TestDescription");
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the help comment of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_5() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setHelpComment("TestHelpComment");
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the data access level of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_6() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setDataAccessLevel("7");
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the Java class name of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_7() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setJavaClassName("org.openbravo.client.application.businesslogic.DemoClassName");
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the background flag of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_8() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setBackground(true);
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the UI pattern of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_9() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setUIPattern("M");
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the multi-record flag of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_10() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setMultiRecord(true);
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the requires explicit access permission flag of the process
   * and expects an error indicating that modifications are not allowed because the module
   * is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_11() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setRequiresExplicitAccessPermission(true);
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the client-side validation of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_12() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setClientSideValidation("DemoClientSideValidation");
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the grid legacy flag of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_13() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setGridlegacy(true);
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the load function of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_14() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setLoadFunction("DemoLoadFunction");
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the "can add records to a selector" flag of the process
   * and expects an error indicating that modifications are not allowed because the module
   * is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_15() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setCanAddRecordsToASelector(true);
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the refresh function of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_16() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setRefreshFunction("DemoRefreshFunction");
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for updating the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the SMFMU scan flag of the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testUpdateOBUIAPP_Process_17() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        registerModuleProcess.setSmfmuScan(true);
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for replacing the module of the OBUIAPP_Process when the module is not in development.
   * This test attempts to set the module of the process to the core module and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testReplaceModuleOBUIAPP_Process() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        Module coreModule = OBDal.getInstance().get(Module.class, "0");
        registerModuleProcess.setModule(coreModule);
        OBDal.getInstance().save(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_MOVE_TO_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  /**
   * Test case for deleting the OBUIAPP_Process when the module is not in development.
   * This test attempts to delete the process and expects an error
   * indicating that modifications are not allowed because the module is not in development.
   */
  @Test
  @Issue("#291")
  public void testDeleteProcessOBUIAPP_Process() {
    setSystemAdministratorContext();
    Module userInterfaceModule = OBDal.getInstance().get(Module.class, USER_INTERFACE_APP_MOD);
    boolean wasInDev = userInterfaceModule.isInDevelopment();
    if (!wasInDev) {
      Process registerModuleProcess = OBDal.getInstance().get(Process.class, OBUIAPP_PROCESS_ID);
      try {
        OBDal.getInstance().remove(registerModuleProcess);
        OBDal.getInstance().flush();
        Assert.fail(ERROR_MESSAGE);
      } catch (Exception e) {
        checkTriggerException(e, MESSAGE_INSERT_OR_DELETE_MODULE_NOT_IN_DEVELOPMENT);
      }
    }
  }

  private void checkTriggerException(Exception exception, String expectedMessage) {
    Throwable ex = DbUtility.getUnderlyingSQLException(exception);
    String bdMessage = OBMessageUtils.translateError(ex.getMessage()).getMessage();
    if (StringUtils.equalsIgnoreCase(RDBMS_ORACLE, rdbms)) {
      expectedMessage =  OBMessageUtils.messageBD(expectedMessage);
    }
    assertEquals(expectedMessage, bdMessage);
    OBDal.getInstance().rollbackAndClose();
  }
}
