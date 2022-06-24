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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.dal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LazyInitializationException;
import org.hibernate.LockMode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test cases covering
 * {@link OBDal#getObjectLockForNoKeyUpdate(org.openbravo.base.structure.BaseOBObject)}
 */
public class DalLockingTest extends OBBaseTest {
  private static final Logger log = LogManager.getLogger();
  private static String testingRuleId;

  private List<String> executionOrder;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void createTestEnvironment() {
    OBContext.setAdminMode(true);
    try {
      AlertRule newAlertRule = OBProvider.getInstance().get(AlertRule.class);
      newAlertRule.setName(DalLockingTest.class.getName() + " - Testing Alert Rule");
      OBDal.getInstance().save(newAlertRule);

      testingRuleId = newAlertRule.getId();
      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void lockedObjectShouldBeANewInstance() {
    AlertRule originalInstance = getTestingAlertRule();
    AlertRule lockedAlert = OBDal.getInstance().getObjectLockForNoKeyUpdate(originalInstance);

    assertThat(lockedAlert, not(sameInstance(originalInstance)));
  }

  @Test
  public void originalObjectShouldBeDetachedFromSession() {
    AlertRule originalInstance = getTestingAlertRule();
    OBDal.getInstance().getObjectLockForNoKeyUpdate(originalInstance);

    // originalInstance is now detached from DAL session. If any of its proxies is tried to be
    // initialized, it should fail.
    thrown.expect(LazyInitializationException.class);
    originalInstance.getADAlertRecipientList().size();
  }

  @Test
  public void objectShouldBeLockedInDB() throws InterruptedException, ExecutionException {
    CountDownLatch latch = new CountDownLatch(1);
    List<Callable<Void>> threads = Arrays.asList( //
        doWithDAL(() -> acquireLock(latch), "T1", 200), //
        doWithDAL(this::acquireLock, "T2", latch, 10));

    executeAndGetResults(threads);
    assertThat(
        "Execution Order: Thread that acquired lock (T1) should finish before the one trying to acquire it afterwards (T2).",
        executionOrder, is(Arrays.asList("T1", "T2")));
  }

  @Test
  public void lockedObjectShouldAllowChildrenCreation()
      throws InterruptedException, ExecutionException {
    CountDownLatch latch = new CountDownLatch(1);
    List<Callable<Void>> threads = Arrays.asList( //
        doWithDAL(() -> acquireLock(latch), "T1", 200), //
        doWithDAL(() -> {
          AlertRecipient recipient = OBProvider.getInstance().get(AlertRecipient.class);
          recipient.setRole(OBContext.getOBContext().getRole());
          recipient.setAlertRule(getTestingAlertRule());
          OBDal.getInstance().save(recipient);
        }, "T2", latch));

    executeAndGetResults(threads);
    assertThat(
        "Execution Order: Even T1 acquired a lock on parent, T2 can insert children without waiting for it",
        executionOrder, is(Arrays.asList("T2", "T1")));
  }

  @Test
  public void lockedInstanceGetsRefreshedFromDB() throws InterruptedException, ExecutionException {
    StringBuilder originalName = new StringBuilder();
    StringBuilder lockedName = new StringBuilder();
    CountDownLatch gotRule = new CountDownLatch(1);
    CountDownLatch ruleModified = new CountDownLatch(1);
    List<Callable<Void>> threads = Arrays.asList( //
        doWithDAL(() -> {
          AlertRule ar = getTestingAlertRule();
          originalName.append(ar.getName());
          gotRule.countDown();
          waitUnitl(ruleModified);
          lockedName.append(OBDal.getInstance().getObjectLockForNoKeyUpdate(ar).getName());
        }, "T1", 50), //
        doWithDAL(() -> {
          getTestingAlertRule().setName("Modified");
          OBDal.getInstance().commitAndClose();
          ruleModified.countDown();
        }, "T2", gotRule));

    executeAndGetResults(threads);

    assertThat("Execution Order", executionOrder, is(Arrays.asList("T2", "T1")));
    assertThat("Name changed", lockedName.toString(), not(originalName.toString()));
    assertThat("Name changed", lockedName.toString(), is("Modified"));
  }

  /** Note once HHH-13135 is fixed and Dal lock adapted to make use of it, this test should fail. */
  @Test
  public void dalLockIsNotAHibernateLock() {
    AlertRule lockedRule = acquireLock();
    LockMode lm = OBDal.getInstance().getSession().getCurrentLockMode(lockedRule);
    assertTrue("Hibernate lock mode doesn't match DAL's one", lm.lessThan(LockMode.WRITE));
  }

  @AfterClass
  public static void cleanUpTestEnvironment() {
    OBContext.setAdminMode();
    try {
      OBDal.getInstance().remove(getTestingAlertRule());
      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private Callable<Void> doWithDAL(Runnable r, String name, CountDownLatch waitFor) {
    return doWithDAL(r, name, waitFor, 0);
  }

  private Callable<Void> doWithDAL(Runnable r, String name, long waitAfter) {
    return doWithDAL(r, name, null, waitAfter);
  }

  private Callable<Void> doWithDAL(Runnable r, String name, CountDownLatch waitEvent,
      long waitAfter) {
    return () -> {
      boolean errorOccurred = false;
      try {
        OBContext.setAdminMode(true);
        waitUnitl(waitEvent);
        r.run();
        TimeUnit.MILLISECONDS.sleep(waitAfter);
        return null;
      } catch (Exception e) {
        log.error("Error occurred executing dal action", e);
        OBDal.getInstance().rollbackAndClose();
        errorOccurred = true;
        throw new OBException(e);
      } finally {
        synchronized (executionOrder) {
          log.info("Completed thread {}", name);
          executionOrder.add(name);
        }
        if (!errorOccurred) {
          OBDal.getInstance().commitAndClose();
        }
        OBContext.restorePreviousMode();
      }
    };
  }

  private <T extends Object> void executeAndGetResults(List<Callable<T>> threads)
      throws InterruptedException, ExecutionException {
    executionOrder = new ArrayList<>();
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    List<Future<T>> results = executorService.invokeAll(threads);
    for (Future<T> result : results) {
      // get to throw exception if it failed
      result.get();
    }
  }

  private AlertRule acquireLock() {
    return acquireLock(null);
  }

  private AlertRule acquireLock(CountDownLatch latch) {
    AlertRule lockedRule = OBDal.getInstance()
        .getObjectLockForNoKeyUpdate(OBDal.getInstance().getProxy(AlertRule.class, testingRuleId));
    if (latch != null) {
      latch.countDown();
    }
    return lockedRule;
  }

  private static AlertRule getTestingAlertRule() {
    return OBDal.getInstance().get(AlertRule.class, testingRuleId);
  }

  private void waitUnitl(CountDownLatch event) {
    if (event == null) {
      return;
    }
    try {
      if (!event.await(10L, TimeUnit.SECONDS)) {
        throw new OBException(new TimeoutException());
      }
    } catch (InterruptedException e) {
      throw new OBException(e);
    }
  }
}
