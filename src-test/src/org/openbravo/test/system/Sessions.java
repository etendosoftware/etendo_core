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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.system;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.openbravo.erpCommon.security.SessionListener;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.test.base.mock.HttpSessionMock;

/** Test cases covering Session management */
public class Sessions {
  private static final Logger log = LogManager.getLogger();
  private static int NUMBER_OF_THREADS = 4;
  private static int NUMBER_OF_SESSIONS_PER_THREAD = 1_000;

  /** Covers bug #37893 */
  @Test
  public void canCreateAndCheckSessionsInParallel()
      throws InterruptedException, ExecutionException {
    log.info("Starting {} threads to create {} sessions each", NUMBER_OF_THREADS,
        NUMBER_OF_SESSIONS_PER_THREAD);
    SessionListener sl = new SessionListener();
    List<SessionCreatorAndChecker> tasks = new ArrayList<>();
    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      tasks.add(new SessionCreatorAndChecker(sl));
    }

    long t = System.currentTimeMillis();
    ExecutorService es = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    List<Future<Void>> executions = es.invokeAll(tasks);

    for (Future<Void> e : executions) {
      // if assertion failed, this will throw exception
      e.get();
    }
    log.info("All finished in {} ms", System.currentTimeMillis() - t);
  }

  private class SessionCreatorAndChecker implements Callable<Void> {
    private SessionListener sl;

    public SessionCreatorAndChecker(SessionListener sl) {
      this.sl = sl;
    }

    @Override
    public Void call() {
      long t = System.currentTimeMillis();
      for (int i = 0; i < NUMBER_OF_SESSIONS_PER_THREAD; i++) {
        String sessionId = SequenceIdData.getUUID();

        HttpSessionMock s = new HttpSessionMock();
        s.setAttribute("#AD_SESSION_ID", sessionId);
        sl.sessionCreated(new HttpSessionEvent(s));

        HttpSession sessionFromContext = SessionListener.getActiveSession(sessionId);

        // before fix of #37893, sessionFromContext is null in some cases because of caught
        // ConcurrentModificationException
        assertThat(sessionFromContext, is(s));
      }

      log.info("Thread finished in {} ms", System.currentTimeMillis() - t);
      return null;
    }
  }
}
