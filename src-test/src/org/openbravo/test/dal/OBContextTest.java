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
 * All portions are Copyright (C) 2009-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.dal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.junit.Test;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.OBBaseTest;

/**
 * Tests the {@link OBContext} class.
 * 
 * @author mtaal
 */

public class OBContextTest extends OBBaseTest {

  /**
   * Tests if the warehouse is set correctly in the OBContext.
   */
  @Test
  public void testWarehouseInContext() {
    OBContext.setOBContext("100", "0", TEST_CLIENT_ID, TEST_ORG_ID, null, TEST_WAREHOUSE_ID);
    assertTrue(OBContext.getOBContext().getWarehouse().getId().equals(TEST_WAREHOUSE_ID));
  }

  /**
   * Tests if the language is set correctly in the OBContext.
   */
  @Test
  public void testLanguageInContext() {
    OBContext.setOBContext("100", "0", TEST_CLIENT_ID, TEST_ORG_ID, "en_US");
    assertTrue(OBContext.getOBContext().getLanguage().getId().equals("192"));
  }

  /**
   * Tests that inactive readable organizations are included in the list of readable organization by
   * the role.
   */
  @Test
  public void testReadableDeactivatedOrg() {
    try {
      Organization deactivatedOrg = OBDal.getInstance().get(Organization.class, TEST_US_ORG_ID);
      deactivatedOrg.setActive(false);
      OBDal.getInstance().flush();

      OBContext.setOBContext("100", "0", TEST_CLIENT_ID, TEST_ORG_ID, "en_US");
      assertThat(Arrays.asList(OBContext.getOBContext().getReadableOrganizations()),
          hasItem(TEST_US_ORG_ID));
    } finally {
      // Do not persist change in organization active flag
      OBDal.getInstance().rollbackAndClose();
    }
  }

  /**
   * Tests if the {@link OBContext#setAdminMode()} and {@link OBContext#restorePreviousMode()} work
   * correctly if the same OBContext is used by multiple threads. This is possible in case of
   * simultaneous ajax requests.
   */
  @Test
  @Issue("8853")
  public void testMultiThreadedOBContext() throws Exception {
    setTestUserContext();
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    final LocalThread t1 = new LocalThread();
    t1.setName("t1");
    final LocalThread t2 = new LocalThread();
    t2.setName("t2");
    t1.setPriority(Thread.MAX_PRIORITY);
    t2.setPriority(Thread.MAX_PRIORITY);

    // they all share the same obcontext
    t1.setLocalOBContext(OBContext.getOBContext());
    t2.setLocalOBContext(OBContext.getOBContext());

    // also tests if this thread influences the other two!
    // main thread is true
    OBContext.setAdminMode();

    try {
      t1.start();
      t2.start();

      // subthreads should have false
      assertFalse(t1.isAdminMode());
      assertFalse(t2.isAdminMode());

      assertTrue(OBContext.getOBContext().isInAdministratorMode());

      // t1 moves to the next phase
      long cnt = 0;
      t1.setFirstStep(true);
      while (!t1.isFirstStepDone()) {
        cnt++;
        Thread.sleep(5);
        // some thing to prevent infinite loops
        assertTrue(cnt < 1000000000);
      }
      // t1 in admin mode, t2 not
      assertFalse(t1.isPrevMode());
      assertTrue(t1.isAdminMode());
      assertFalse(t2.isAdminMode());

      assertTrue(OBContext.getOBContext().isInAdministratorMode());

      // let t2 do the first step
      t2.setFirstStep(true);
      cnt = 0;
      while (!t2.isFirstStepDone()) {
        cnt++;
        Thread.sleep(5);
        // some thing to prevent infinite loops
        assertTrue(cnt < 1000000000);
      }
      // second one should encounter adminmode = false as it is a different thread;
      // both t1 and t2 in admin mode
      assertFalse(t2.isPrevMode());
      assertTrue(t2.isAdminMode());
      assertTrue(t1.isAdminMode());

      assertTrue(OBContext.getOBContext().isInAdministratorMode());

      // move t1 to the next step
      t1.setNextStep(true);
      cnt = 0;
      while (!t1.isNextStepDone()) {
        cnt++;
        Thread.sleep(5);
        // some thing to prevent infinite loops
        assertTrue(cnt < 1000000000);
      }
      // t1 not in admin mode, t2 in admin mode still
      assertFalse(t1.isAdminMode());
      assertTrue(t2.isAdminMode());

      assertTrue(OBContext.getOBContext().isInAdministratorMode());

      // now move t2
      t2.setNextStep(true);
      cnt = 0;
      while (!t2.isNextStepDone()) {
        cnt++;
        Thread.sleep(5);
        // some thing to prevent infinite loops
        assertTrue(cnt < 1000000000);
      }
      // t2 not anymore in admin mode
      assertFalse(t2.isAdminMode());
      assertTrue(OBContext.getOBContext().isInAdministratorMode());
    } finally {
      // ensure that the threads stop
      t1.setFirstStep(true);
      t2.setFirstStep(true);
      t1.setNextStep(true);
      t2.setNextStep(true);
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Maintain and print stacktraces when calls to setAdminMode and restoreAdminMode are unbalanced
   * 
   * To test this issue set the OBContext.ADMIN_TRACE_SIZE to a higher value than 0
   */
  @Test
  @Issue("13572")
  public void testUnbalancedCallsToAdminMode() {
    OBContext.setAdminMode();
    OBContext.setAdminMode();
    OBContext.setAdminMode();
    OBContext.restorePreviousMode();
    OBContext.restorePreviousMode();
    OBContext.restorePreviousMode();
    OBContext.restorePreviousMode();
  }

  @Test
  public void basicSerializationShouldWork() throws IOException, ClassNotFoundException {
    OBContext originalCtx = OBContext.getOBContext();
    Path serializedPath = serializeContext(originalCtx);
    OBContext deserialized = deserializeContext(serializedPath);
    assertThat("Role ID is kept", deserialized.getRole().getId(),
        is(originalCtx.getRole().getId()));
  }

  @Test
  public void clientVisibilityIsCorrectAfterDeserialization()
      throws IOException, ClassNotFoundException {
    OBContext originalCtx = OBContext.getOBContext();
    Path serializedPath = serializeContext(originalCtx);
    OBContext deserialized = deserializeContext(serializedPath);
    assertThat("Readable clients are kept", Arrays.asList(deserialized.getReadableClients()),
        containsInAnyOrder(originalCtx.getReadableClients()));
  }

  @Test
  public void organizationVisibilityIsCorrectAfterDeserialization()
      throws IOException, ClassNotFoundException {
    OBContext originalCtx = OBContext.getOBContext();
    Path serializedPath = serializeContext(originalCtx);
    OBContext deserialized = deserializeContext(serializedPath);
    assertThat("Readable organizations are kept",
        Arrays.asList(deserialized.getReadableOrganizations()),
        containsInAnyOrder(originalCtx.getReadableOrganizations()));
    assertThat("Writable organizations are kept", deserialized.getWritableOrganizations(),
        containsInAnyOrder(originalCtx.getWritableOrganizations().toArray()));
  }

  private Path serializeContext(OBContext ctx) throws IOException {
    Path serializedPath = Files.createTempFile("serialized", ".tmp");
    try (OutputStream o = Files.newOutputStream(serializedPath);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(o)) {
      objectOutputStream.writeObject(ctx);
    }
    return serializedPath;
  }

  private OBContext deserializeContext(Path path) throws IOException, ClassNotFoundException {
    try (InputStream is = Files.newInputStream(path, StandardOpenOption.DELETE_ON_CLOSE);
        ObjectInputStream ois = new ObjectInputStream(is)) {
      return (OBContext) ois.readObject();
    }
  }

  // the scenario:
  // thread1 T1
  // thread2 T2
  // T2: setInAdminMode(true)
  // T1: setInAdminMode(true)
  // T2: restorePrevAdminMode --> sets admin mode to false
  // T1: fails because adminmode is false

  private class LocalThread extends Thread {

    private boolean firstStep = false;
    private boolean firstStepDone = false;
    private boolean nextStep = false;
    private boolean nextStepDone = false;
    private boolean adminMode = false; // start with true
    private boolean prevMode = false;

    private OBContext localOBContext;

    @Override
    public void run() {
      OBContext.setOBContext(getLocalOBContext());

      try {
        while (!firstStep) {
          adminMode = OBContext.getOBContext().isInAdministratorMode();
        }
        OBContext.setAdminMode();
        adminMode = OBContext.getOBContext().isInAdministratorMode();
        firstStepDone = true;
        while (!nextStep) {
          adminMode = OBContext.getOBContext().isInAdministratorMode();
        }
        OBContext.restorePreviousMode();
        adminMode = OBContext.getOBContext().isInAdministratorMode();
        nextStepDone = true;
      } catch (Exception e) {
        e.printStackTrace(System.err);
        throw new IllegalStateException(e);
      }
    }

    public void setNextStep(boolean nextStep) {
      this.nextStep = nextStep;
    }

    public boolean isAdminMode() {
      return adminMode;
    }

    public void setFirstStep(boolean firstStep) {
      this.firstStep = firstStep;
    }

    public boolean isPrevMode() {
      return prevMode;
    }

    public boolean isFirstStepDone() {
      return firstStepDone;
    }

    public boolean isNextStepDone() {
      return nextStepDone;
    }

    public OBContext getLocalOBContext() {
      return localOBContext;
    }

    public void setLocalOBContext(OBContext localOBContext) {
      this.localOBContext = localOBContext;
    }

  }
}
