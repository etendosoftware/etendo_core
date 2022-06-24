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
 * All portions are Copyright (C) 2017 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.system;

import static org.apache.commons.lang.math.NumberUtils.DOUBLE_ZERO;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.openbravo.test.base.Issue;
import org.openbravo.utils.CryptoUtility;

/** Test cases for org.openbravo.utils.CryptoUtility */
public class CryptoUtilities {
  private static final int THREAD_NUM = 8;
  private static final int LOOPS = 100;

  private static Logger log = LogManager.getLogger();

  /** Covers concurrent utilization of encrypt/decrypt methods. */
  @Test
  @Issue("36909")
  public void shouldWorkConcurrently() throws InterruptedException, ExecutionException {
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_NUM);

    List<Callable<List<String>>> tasks = new ArrayList<>();

    for (int i = 0; i < THREAD_NUM; i++) {
      tasks.add(new Callable<List<String>>() {
        @Override
        public List<String> call() throws Exception {
          List<String> msg = new ArrayList<>(LOOPS);
          for (int j = 0; j < LOOPS; j++) {
            try {
              String raw = RandomStringUtils.randomAlphanumeric(j);
              String encrypted = CryptoUtility.encrypt(raw);
              String decrypted = CryptoUtility.decrypt(encrypted);
              if (!decrypted.equals(raw)) {
                msg.add("Failed to decrypt");
              }
            } catch (Exception e) {
              msg.add(e.getMessage());
            }
          }
          return msg;
        }
      });
    }

    List<Future<List<String>>> execs = executor.invokeAll(tasks, 5, TimeUnit.MINUTES);
    double totalErrors = 0;
    for (Future<List<String>> exec : execs) {
      for (String msg : exec.get()) {
        log.error(msg);
        totalErrors += 1;
      }
    }
    assertThat("Error ratio", totalErrors / (THREAD_NUM * LOOPS), is(DOUBLE_ZERO));
  }
}
