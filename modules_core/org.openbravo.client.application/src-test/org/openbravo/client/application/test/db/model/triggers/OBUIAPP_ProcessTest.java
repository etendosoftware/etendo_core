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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

public class OBUIAPP_ProcessTest extends OBBaseTest {

  private static Logger log = LogManager.getLogger();
  private static final String USER_INTERFACE_APP_MOD = "9BA0836A3CD74EE4AB48753A47211BCC";
  private static String OBUIAPP_PROCESS_ID = "EBC24A55293F4E4BAF56EF8DFA43D578"; // RegisterModule Process Definition
  private static String MESSAGE_MODULE_NOT_IN_DEVELOPMENT = "20532";
  private static String MESSAGE_INSERT_OR_DELETE_MODULE_NOT_IN_DEVELOPMENT = "20533";
  private static String MESSAGE_MOVE_TO_MODULE_NOT_IN_DEVELOPMENT = "ChangeNotInDevModule";
  private static String ERROR_MESSAGE = "Modifications in a module marked as development are not allowed.";

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
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_MOVE_TO_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }

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
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        assertEquals(MESSAGE_INSERT_OR_DELETE_MODULE_NOT_IN_DEVELOPMENT, message);
      }
    }
  }
}
