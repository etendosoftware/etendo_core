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
 * All portions are Copyright (C) 2013-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.dal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.hibernate.LazyInitializationException;
import org.hibernate.query.Query;
import org.junit.Test;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.test.base.HiddenObjectHelper;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test cases to verify multiple ApplicationDictionaryCachedStructures behavior when working
 * concurrently with multiple threads.
 * 
 * @author alostale
 * 
 */
public class ADCachedMultiThreadTest extends OBBaseTest {
  private static final int TEST_EXECUTIONS = 10;
  private static final int MAX_DELAY = 250;

  private static final String BP_HEADER = "220";
  private static final String BP_CUSTOMER = "223";

  private static Logger log = LogManager.getLogger();

  /**
   * This test executes using cache:
   * <ul>
   * <li>Thread 1 gets tab from ApplicationDictionaryCachedStructures and finishes
   * <li>Thread 2 gets same tab and gets its parent
   * </ul>
   * 
   * Verifies tab object is completely initialized by Thread 1 so Thread 2 can work with it
   * 
   */
  @Test
  public void testParentTabCache() throws Exception {

    executeTestParentTab(true);
  }

  /**
   * This test executes without using cache:
   * <ul>
   * <li>Thread 1 gets tab from ApplicationDictionaryCachedStructures and finishes
   * <li>Thread 2 gets same tab and gets its parent
   * </ul>
   * 
   * Verifies tab object is completely initialized by Thread 1 so Thread 2 can work with it
   * 
   */
  @Test
  public void testParentTabNoCache() throws Exception {
    executeTestParentTab(false);
  }

  /**
   * This test creates multiple threads invoking all public methods in
   * ApplicationDictionaryCachedStructures for the same tab.
   * 
   * It is executed {@code TEST_EXECUTIONS} number of times to simulate high traffic in the same
   * session.
   * 
   * Using cache
   * 
   */
  @Test
  @Issue("24421")
  public void testMultiCallsCache() throws Exception {
    executeTestMultiCalls(true);
  }

  /**
   * This test creates multiple threads invoking all public methods in
   * ApplicationDictionaryCachedStructures for the same tab.
   * 
   * It is executed {@code TEST_EXECUTIONS} number of times to simulate high traffic in the same
   * session.
   * 
   * Not using cache
   * 
   */
  @Test
  @Issue("24421")
  public void testMultiCallsNoCache() throws Exception {
    executeTestMultiCalls(false);
  }

  private void executeTestParentTab(boolean cache) throws Exception {
    log.debug(" session id: {}",
        Integer.toString(System.identityHashCode(OBDal.getInstance().getSession())));
    setSystemAdministratorContext();
    ApplicationDictionaryCachedStructures adcs = new ApplicationDictionaryCachedStructures();

    // Force using cache even there are mods in dev
    HiddenObjectHelper.set(adcs, "useCache", cache);

    ExecutorService executor = Executors.newFixedThreadPool(1);

    ArrayList<Callable<Long>> threads = new ArrayList<Callable<Long>>();
    threads.add(new TabLoader(adcs, BP_CUSTOMER, false));
    threads.add(new ParentLoader(adcs, BP_CUSTOMER, true));
    List<Future<Long>> r = executor.invokeAll(threads, 5, TimeUnit.MINUTES);
    ResultSummary summary = new ResultSummary(r);
    summary.logResults();
    assertFalse("There are threads with errors", summary.hasErrors);
  }

  /**
   * 
   * Property columns is an especial case because they can link to a column in a different table
   * which needs to be completely initialized within current tab
   * 
   */
  @Test
  public void testPropertyColumn() throws Exception {
    // Expecting LazyInitializationException, disabling log not to display it
    Logger category = LogManager.getLogger();
    Level originalLevel = category.getLevel();
    setLoggerLevel(category, Level.FATAL);

    log.debug(" session id: {}",
        Integer.toString(System.identityHashCode(OBDal.getInstance().getSession())));
    setSystemAdministratorContext();
    ApplicationDictionaryCachedStructures adcs = new ApplicationDictionaryCachedStructures();

    // tabs with property fields that are linked to a different table than tab's one
    String hql = "select distinct f.tab.id " //
        + "from ADField f "//
        + "where f.column.table != f.tab.table";
    Query<String> qTabs = OBDal.getInstance().getSession().createQuery(hql, String.class);

    // Force using cache even there are mods in dev
    HiddenObjectHelper.set(adcs, "useCache", true);

    ExecutorService executor = Executors.newFixedThreadPool(20);

    ArrayList<Callable<Long>> threads = new ArrayList<Callable<Long>>();

    List<String> tabIds = qTabs.list();
    for (String tabId : tabIds) {
      threads.add(new TabLoader(adcs, tabId, false));
    }

    List<Future<Long>> r = executor.invokeAll(threads, 5, TimeUnit.MINUTES);
    ResultSummary summary = new ResultSummary(r);
    summary.logResults();

    boolean failed = false;
    for (String tabId : tabIds) {
      Tab tab = adcs.getTab(tabId);
      log.debug("Processing tab {} - {}", tab.getWindow().getName(), tab.getName());

      for (Field f : adcs.getFieldsOfTab(tabId)) {
        try {
          if (f.getColumn() != null) {
            f.getColumn().getTable().getName();
          }
        } catch (LazyInitializationException e) {
          failed = true;
          log.error("  Failed with {}, prop: {}", f.getColumn().getName(), f.getProperty());
        }
      }
    }
    assertFalse("There are propeties that failed", failed);
    setLoggerLevel(category, originalLevel);
  }

  private void setLoggerLevel(Logger logger, Level level) {
    final LoggerContext context = LoggerContext.getContext(false);
    final Configuration config = context.getConfiguration();

    LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());

    LoggerConfig specificConfig = loggerConfig;
    String loggerName = logger.getName();
    if (!loggerConfig.getName().equals(loggerName)) {
      specificConfig = new LoggerConfig(loggerName, level, true);
      specificConfig.setParent(loggerConfig);
      config.addLogger(loggerName, specificConfig);
    }
    specificConfig.setLevel(level);
  }

  private void executeTestMultiCalls(boolean cache) throws Exception {
    long t0 = System.currentTimeMillis();
    setSystemAdministratorContext();

    Map<String, Integer> totalSummary = new HashMap<String, Integer>();
    for (int i = 0; i < TEST_EXECUTIONS; i++) {
      log.debug("====== Starting execution #{} ============", i);

      long t = System.currentTimeMillis();

      t = System.currentTimeMillis();
      ApplicationDictionaryCachedStructures adcs = new ApplicationDictionaryCachedStructures();
      // Force using cache even there are mods in dev
      HiddenObjectHelper.set(adcs, "useCache", cache);

      ExecutorService executor = Executors.newFixedThreadPool(10);

      ArrayList<Loader> threads = new ArrayList<Loader>();
      threads.add(new TabLoader(adcs, BP_CUSTOMER, false));
      threads.add(new ParentLoader(adcs, BP_CUSTOMER, true));
      threads.add(new FieldListLoader(adcs, BP_CUSTOMER, true));
      threads.add(new ColumnsList(adcs, BP_CUSTOMER, true));
      threads.add(new AuxiliarInputList(adcs, BP_CUSTOMER, true));

      threads.add(new TabLoader(adcs, BP_HEADER, false));
      threads.add(new ParentLoader(adcs, BP_HEADER, true));
      threads.add(new FieldListLoader(adcs, BP_HEADER, true));
      threads.add(new ColumnsList(adcs, BP_HEADER, true));
      threads.add(new AuxiliarInputList(adcs, BP_HEADER, true));

      List<Future<Long>> r = executor.invokeAll(threads, 5, TimeUnit.MINUTES);
      log.debug("All threads done in {} ms", System.currentTimeMillis() - t);
      ResultSummary summary = new ResultSummary(r, totalSummary);
      summary.logResults();

      HiddenObjectHelper.initializeField(adcs, "tabMap");
      HiddenObjectHelper.initializeField(adcs, "tableMap");
      HiddenObjectHelper.initializeField(adcs, "fieldMap");
      HiddenObjectHelper.initializeField(adcs, "columnMap");
      HiddenObjectHelper.initializeField(adcs, "auxInputMap");
      HiddenObjectHelper.initializeField(adcs, "comboTableDataMap");

      log.debug("====== Done execution #{} ============\n\n", i);
    }

    log.debug("Total Summary:");
    for (String key : totalSummary.keySet()) {
      if (key.equals("Success")) {
        log.debug("{}\t: {}", key, totalSummary.get(key));
      } else {
        log.error("{}\t: {}", key, totalSummary.get(key));
      }
    }

    assertTrue("Threads with errors",
        totalSummary.size() == 1 && totalSummary.get("Success") != null);

    log.debug("Total time {}", System.currentTimeMillis() - t0);
  }

  private abstract class Loader implements Callable<Long> {
    protected ApplicationDictionaryCachedStructures cs;
    protected String tabId;
    protected boolean delay;
    private boolean forcedDelay = false;
    private int delayTime;

    Loader(ApplicationDictionaryCachedStructures cs, String tabId, boolean delay) {
      this.cs = cs;
      this.tabId = tabId;
      this.delay = delay;
    }

    public Loader(ApplicationDictionaryCachedStructures cs, String tabId, int delayMs) {
      this.cs = cs;
      this.tabId = tabId;
      this.forcedDelay = true;
      this.delayTime = delayMs;
      this.delay = true;
    }

    protected abstract void doAction();

    @Override
    public Long call() {
      setSystemAdministratorContext();
      long t = System.currentTimeMillis();
      log.debug("Start thread {} for tab {}", this.getClass().getName(), tabId);
      delay();

      doAction();

      SessionHandler.getInstance().commitAndClose();
      OBDal.getInstance().getSession().disconnect();

      log.debug("thread {} done in {} ms", this.getClass().getName(),
          System.currentTimeMillis() - t);
      return System.currentTimeMillis() - t;
    }

    private void delay() {
      if (delay) {
        int delayMs;
        if (!forcedDelay) {
          Random randomGenerator = new Random();
          delayMs = randomGenerator.nextInt(MAX_DELAY);
        } else {
          delayMs = delayTime;
        }
        log.debug("  delaying thread {} ms", delayMs);
        try {
          Thread.sleep(delayMs);
        } catch (InterruptedException e) {

        }
        log.debug("  delay done");
      }
    }
  }

  private class TabLoader extends Loader {
    TabLoader(ApplicationDictionaryCachedStructures cs, String tabId, boolean delay) {
      super(cs, tabId, delay);
    }

    public TabLoader(ApplicationDictionaryCachedStructures cs, String tabId, int delayMs) {
      super(cs, tabId, delayMs);
    }

    @Override
    protected void doAction() {
      log.debug("Loading tab {}", tabId);
      cs.getTab(tabId);
    }
  }

  private class ParentLoader extends Loader {
    ParentLoader(ApplicationDictionaryCachedStructures cs, String tabId, boolean delay) {
      super(cs, tabId, delay);
    }

    @Override
    protected void doAction() {
      KernelUtils.getInstance().getParentTab(cs.getTab(tabId));
    }
  }

  private class FieldListLoader extends Loader {
    FieldListLoader(ApplicationDictionaryCachedStructures cs, String tabId, boolean delay) {
      super(cs, tabId, delay);
    }

    @Override
    protected void doAction() {
      cs.getFieldsOfTab(tabId);
    }
  }

  private class AuxiliarInputList extends Loader {
    AuxiliarInputList(ApplicationDictionaryCachedStructures cs, String tabId, boolean delay) {
      super(cs, tabId, delay);
    }

    @Override
    protected void doAction() {
      cs.getAuxiliarInputList(tabId);
    }
  }

  private class ColumnsList extends Loader {
    ColumnsList(ApplicationDictionaryCachedStructures cs, String tabId, boolean delay) {
      super(cs, tabId, delay);
    }

    @Override
    protected void doAction() {
      cs.getColumnsOfTable(cs.getTab(tabId).getTable().getId());
    }
  }

  private class ResultSummary {

    private List<Future<Long>> r;
    private boolean hasErrors;
    private Map<String, Integer> totalSummary;

    public ResultSummary(List<Future<Long>> r) {
      this.r = r;
    }

    public ResultSummary(List<Future<Long>> r, Map<String, Integer> totalSummary) {
      this.r = r;
      this.totalSummary = totalSummary;
    }

    public void logResults() {
      Map<String, Integer> summary = new HashMap<String, Integer>();
      for (Future<Long> f : r) {
        try {
          if (f.isCancelled()) {
            addSummary(summary, totalSummary, "Cancelled");
          } else if (f.get() > 0) {
            addSummary(summary, totalSummary, "Success");
          }
        } catch (Throwable tr) {
          addSummary(summary, totalSummary, tr.getMessage());
        }
      }
      for (String key : summary.keySet()) {
        if (key.equals("Success")) {
          log.debug("{}\t: {}", key, summary.get(key));
        } else {
          hasErrors = true;
          log.error("{}\t: {}", key, summary.get(key));
        }
      }
    }

    private void addSummary(Map<String, Integer> summary, Map<String, Integer> totalS, String key) {
      int total = (summary.get(key) == null ? 0 : summary.get(key)) + 1;
      summary.put(key, total);
      if (totalS != null) {
        addSummary(totalS, null, key);
      }
    }
  }
}
