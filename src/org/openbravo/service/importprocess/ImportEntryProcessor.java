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
 * All portions are Copyright (C) 2015-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.importprocess;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ExternalConnectionPool;
import org.openbravo.database.SessionInfo;
import org.openbravo.model.common.enterprise.Organization;

/**
 * The {@link ImportEntryProcessor} is responsible for importing/processing {@link ImportEntry}
 * instances for a specific TypeOfData.
 * 
 * The {@link ImportEntryProcessor} is a singleton/applicationscoped, it implements a generic
 * approach to make to make it possible to do import of {@link ImportEntry} in parallel threads (
 * {@link ImportEntryProcessRunnable}) if possible.
 * 
 * It is important that a specific ImportEntry is assigned to the right processing thread to prevent
 * for example deadlocks in the database. To make this possible a concept of
 * {@link #getProcessSelectionKey(ImportEntry)} is used. The process selection key is a unique key
 * derived from the {@link ImportEntry} which can be used to create/identify the thread which should
 * process the {@link ImportEntry}. If no such thread exists a new
 * {@link ImportEntryProcessRunnable} is created. The exact type of
 * {@link ImportEntryProcessRunnable} is determined by the extending subclass through the
 * {@link #createImportEntryProcessRunnable()} method.
 * 
 * For example if ImportEntry records of the same organization should be processed after each other
 * (so not in parallel) to prevent DB deadlocks, this means that the records of the same
 * organization should be assigned to the same thread object. So that they are indeed processed
 * sequential and not in parallel. The {@link #getProcessSelectionKey(ImportEntry)} should in this
 * case return the {@link Organization#getId()} so that {@link ImportEntryProcessRunnable} are
 * keyed/registered using the organization. Other {@link ImportEntry} records of the same
 * organization are then processed by the same thread, always sequential, not parallel, preventing
 * DB deadlocks.
 * 
 * The {@link ImportEntryManager} passes new {@link ImportEntry} records to the the
 * {@link ImportEntryProcessor} by calling its {@link #handleImportEntry(ImportEntry)}. The
 * {@link ImportEntryProcessor} then can decide how to handle this {@link ImportEntry}, create a new
 * thread or assign it to an existing thread (which is busy processing previous entries). This is
 * all done in this generic class. An implementing subclass needs to implement the
 * {@link #getProcessSelectionKey(ImportEntry)} method. This method determines which/how the correct
 * {@link ImportEntryProcessRunnable} is chosen.
 * 
 * The default/base implementation of the {@link ImportEntryProcessRunnable} provides standard
 * features related to caching of {@link OBContext}, error handling and transaction handling.
 * 
 * Note: this implementation uses the executorService in the {@link ImportEntryManager}. Threads are
 * started by using the {@link ExecutorService#submit(Runnable)} method, see
 * {@link ImportEntryManager#submitRunnable(Runnable)}. Any exceptions inside the
 * {@link Runnable#run()} method are swallowed and won't directly show up in the console. Therefore
 * the default implementation in the {@link ImportEntryProcessRunnable#run()} has different
 * mechanisms to correctly log/record the error (in the {@link ImportEntry#getErrorinfo()}).
 * 
 * Note: the {@link ImportEntryProcessor} should be aware that the same {@link ImportEntry} can be
 * passed multiple times to it. Also after the {@link ImportEntryProcessor} has already processed
 * it. This can happen because of the parallel/multi-threaded approach followed here. So the
 * {@link ImportEntryProcessor} and the implementation of the {@link ImportEntryProcessRunnable}
 * should correctly and robustly handle this case. The default {@link ImportEntryProcessRunnable}
 * implementation has mechanism to prevent double processing in some cases.
 * 
 * Note: it is save for an ImportEntryProcessor to occasionally not process an {@link ImportEntry}.
 * The {@link ImportEntryManager} will offer the {@link ImportEntry} again in its next cycle. But it
 * is quite important that this only happens if the order of the entries being processed is
 * maintained or not relevant.
 * 
 * @author mtaal
 */
@ApplicationScoped
public abstract class ImportEntryProcessor {

  // a sufficient large number still preventing OOM and signaling strange situation
  private static final int MAX_QUEUE_SIZE = 50000;

  // not static to create a Logger for each subclass
  private Logger log = LogManager.getLogger(this.getClass());

  // multiple threads access this map, its access is handled through
  // synchronized methods
  private Map<String, ImportEntryProcessRunnable> runnables = new HashMap<>();

  @Inject
  private ImportEntryManager importEntryManager;

  /**
   * Is called when the application context/tomcat stops, is called from
   * {@link ImportEntryManager#shutdown()}.
   */
  public void shutdown() {
  }

  /**
   * Is called from the {@link ImportEntryManager} thread, passes in a new ImportEntry to process.
   * Finds the Thread which can handle this entry, if none is found a new thread is created, if one
   * is found then the ImportEntry is passed/given to it.
   * 
   * If the processing of the entry does not happen fast enough then it can be that the
   * {@link ImportEntry} is again offered to the {@link ImportEntryProcessor} through a call to this
   * method. The implementation should be able to gracefully handle duplicate entries. Also the
   * implementation should check if the {@link ImportEntry} was possibly already handled and ignore
   * it then.
   */
  public void handleImportEntry(ImportEntry importEntry) {

    if (!canHandleImportEntry(importEntry)) {
      return;
    }
    // check if there is already a thread which should handle this
    // importentry.
    final String key = getProcessSelectionKey(importEntry);

    // the next call is synchronized to manage the case
    // that a thread deregisters itself at the same time
    assignEntryToThread(key, importEntry);
  }

  // synchronized to handle the case that a thread tries to deregister
  // itself at the same time
  protected synchronized void assignEntryToThread(String key, ImportEntry importEntry) {

    // runnables is a concurrent hashmap
    ImportEntryProcessRunnable runnable = runnables.get(key);

    // note: the runnable maybe is not running yet
    // as runnable can already be in a queue of the executorservice
    // waiting to be processed, but not yet started
    if (runnable != null) {
      // give it to the runnable, the addEntry checks if the import entry
      // is not already being handled, if so it is skipped
      runnable.addEntry(importEntry);

      // done
      return;
    }

    log.debug("Created new runnable for key " + key);

    // no runnable, create a new one
    runnable = createImportEntryProcessRunnable();

    // give it the entry
    runnable.setImportEntryManager(importEntryManager);
    runnable.setImportEntryProcessor(this);
    runnable.setKey(key);
    runnable.addEntry(importEntry);

    // and give it to the executorServer to run
    boolean submitted = importEntryManager.submitRunnable(runnable);
    if (submitted) {
      // and make sure it can get next entries by caching it
      runnables.put(key, runnable);
    }
  }

  /**
   * Is called when a {@link ImportEntryProcessRunnable} is ready with its current sets of
   * {@link ImportEntry} and stops running.
   * 
   * Is synchronized to be handle the case that deregistering happens while also an entry was added.
   * If an entry was added false is returned and the thread continues.
   */
  private synchronized boolean tryDeregisterProcessThread(ImportEntryProcessRunnable runnable) {
    if (!runnable.getImportEntryQueue().isEmpty()) {
      log.debug("Not deregistering process thread as new entries have been added to it");
      // a new entry was entered while we tried to deregister
      return false;
    }
    doDeregisterProcessThread(runnable);
    return true;
  }

  private synchronized void doDeregisterProcessThread(ImportEntryProcessRunnable runnable) {
    log.debug("Removing runnable " + runnable.getKey());
    runnables.remove(runnable.getKey());
  }

  /**
   * Create a concrete subclass of {@link ImportEntryProcessRunnable}
   */
  protected abstract ImportEntryProcessRunnable createImportEntryProcessRunnable();

  /**
   * Can be used by implementing subclass to check that the ImportEntry can be processed now. In
   * some cases other ImportEntries should be processed first. By returning false the ImportEntry is
   * ignored for now. It will again be picked up in a next execution cycle of the
   * {@link ImportEntryManager} thread and then offered again to this {@link ImportEntryProcessor}
   * to be processed.
   */
  protected abstract boolean canHandleImportEntry(ImportEntry importEntryInformation);

  /**
   * Based on the {@link ImportEntry} returns a key which uniquely identifies the thread which
   * should process this {@link ImportEntry}. Can be used to place import entries which block/use
   * the same records in the same import thread, in this way preventing DB (dead)locks.
   */
  protected abstract String getProcessSelectionKey(ImportEntry importEntry);

  /**
   * Declares if the import entry will later on be archived after it has been processed
   */
  protected boolean enableArchive() {
    return true;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "\n"
        + (runnables.isEmpty() ? "  No runnables"
            : runnables.entrySet()
                .stream()
                .map(e -> "  " + e.getKey() + "\n" + e.getValue())
                .collect(Collectors.joining("\n")));

  }

  /**
   * The default implementation of the ImportEntryProcessRunnable. It performs the following
   * actions:
   * <ul>
   * <li>able to get new {@link ImportEntry} records while the processing of other
   * {@link ImportEntry} records happens.</li>
   * <li>processes the ImportEntry, creates a new OBContext based on the user data of the
   * {@link ImportEntry}</li>
   * <li>makes sure that there is a {@link VariablesSecureApp} in the {@link RequestContext}.
   * <li>OBContexts are temporary cached in a {@link WeakHashMap}</li>
   * <li>the process checks the {@link ImportEntry} status just before it is processed, it also
   * prevents the same {@link ImportEntry} to be processed twice by one thread</li>
   * <li>each {@link ImportEntry} is processed in its own connection and transaction. Note that the
   * class delegates into the implementing subclass the ability of handling the commit/rollback of
   * the transaction. But in order to prevent possible connection leaks, this class closes all the
   * opened connections (if any) before ending.</li>
   * <li>the process sets admin mode, before calling the subclass</li>
   * <li>an error which ends up in the main loop here is stored in the {@link ImportEntry} in the
   * errorInfo property</li>
   * <li>subclasses implement the {@link #processEntry(ImportEntry)} method.
   * </ul>
   * 
   * @author mtaal
   *
   */
  public abstract static class ImportEntryProcessRunnable implements Runnable {
    private Queue<QueuedEntry> importEntries = new ConcurrentLinkedQueue<>();

    private Logger logger;

    // create concurrent hashset using util method
    private Set<String> importEntryIds = Collections
        .newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private ImportEntryManager importEntryManager;
    private ImportEntryProcessor importEntryProcessor;
    private String key = null;
    // use weakhashmap so that the content is automatically purged
    // when the garbagecollector runs
    private Map<String, OBContext> cachedOBContexts = new HashMap<>();

    private String currentProcessingEntry;
    private long currentProcessingEntryStarted;

    public ImportEntryProcessRunnable() {
      logger = LogManager.getLogger();
    }

    @Override
    public void run() {
      try {
        while (true) {
          importEntryManager.notifyStartProcessingInCluster();
          try {
            if (importEntryManager.isShutDown()) {
              return;
            }
            doRunCycle();
          } catch (Throwable logIt) {
            // prevent the loop from exiting, only log the exception
            // normally low level errors end up here
            ImportProcessUtils.logError(logger, logIt);
          } finally {

            // bit rough but ensures that the connection is released/closed
            try {
              OBDal.getInstance().rollbackAndClose();
            } catch (Exception ignored) {
            }

            try {
              if (TriggerHandler.getInstance().isDisabled()) {
                TriggerHandler.getInstance().enable();
              }
              OBDal.getInstance().commitAndClose();
            } catch (Exception ignored) {
            }

            logger.debug("Trying to deregister process " + key);

            // no more entries and deregistered, if so go away
            if (importEntryProcessor.tryDeregisterProcessThread(this)) {
              logger.debug("All entries processed, exiting thread");
              importEntryIds.clear();
              cachedOBContexts.clear();
              return;
            }
          }
        }
      } finally {
        // always deregister at this point to be sure that we are not re-used
        logger.debug("Loop finished removing runnable " + getKey());
        importEntryProcessor.doDeregisterProcessThread(this);
        importEntryManager.notifyEndProcessingInCluster();
      }
    }

    protected void doRunCycle() {
      int cnt = 0;
      long totalT = 0;
      QueuedEntry queuedImportEntry;
      while ((queuedImportEntry = importEntries.poll()) != null) {
        try {
          if (importEntryManager.isShutDown()) {
            return;
          }
          final long t0 = System.currentTimeMillis();

          currentProcessingEntry = queuedImportEntry.importEntryId;
          currentProcessingEntryStarted = System.currentTimeMillis();

          // set the same obcontext as was being used for the original
          // entry
          setOBContext(queuedImportEntry);

          OBContext.setAdminMode(true);
          ImportEntry localImportEntry;
          try {
            // reload the importEntry
            localImportEntry = OBDal.getInstance()
                .get(ImportEntry.class, queuedImportEntry.importEntryId);

            // check if already processed, if so skip it
            if (localImportEntry == null || !"Initial".equals(localImportEntry.getImportStatus())) {
              logger
                  .debug("Entry already processed skipping it " + queuedImportEntry.importEntryId);
              continue;
            }
          } finally {
            OBContext.restorePreviousMode();
          }

          // not changed, process
          final String typeOfData = localImportEntry.getTypeofdata();

          if (logger.isDebugEnabled()) {
            logger.debug("Processing entry {} {}", localImportEntry.getIdentifier(), typeOfData);
          }

          processEntry(localImportEntry);

          if (logger.isDebugEnabled()) {
            logger.debug("Finished Processing entry {} {} in {} ms",
                localImportEntry.getIdentifier(), typeOfData, System.currentTimeMillis() - t0);
          }

          // don't use the import entry anymore, touching methods on it
          // may re-open a session
          localImportEntry = null;

          // processed so can be removed
          importEntryIds.remove(queuedImportEntry.importEntryId);

          // keep some stats
          cnt++;
          final long timeForEntry = (System.currentTimeMillis() - t0);
          totalT += timeForEntry;
          importEntryManager.reportStats(typeOfData, timeForEntry);
          if ((cnt % 100) == 0 && logger.isDebugEnabled()) {
            logger.debug("Runnable: " + key + ", processed " + cnt + " import entries in " + totalT
                + " millis, " + (totalT / cnt) + " per import entry, current queue size: "
                + importEntries.size());
          }

          if (TriggerHandler.getInstance().isDisabled()) {
            logger.error("Triggers disabled at end of processing an entry, this is a coding error, "
                + "call TriggerHandler.enable in your code. Triggers are enabled again for now!");
            TriggerHandler.getInstance().enable();
            OBDal.getInstance().commitAndClose();
          }

          // close sessions in case the import entry processEntry left them opened
          if (SessionHandler.isSessionHandlerPresent()) {
            OBDal.getInstance().commitAndClose();
          }
          if (SessionHandler.existsOpenedSessions()) {
            SessionHandler.getInstance().cleanUpSessions();
          }

        } catch (Throwable t) {
          ImportProcessUtils.logError(logger, t);

          // bit rough but ensures that the connection is released/closed
          try {
            OBDal.getInstance().rollbackAndClose();
          } catch (Exception ignored) {
          }
          try {
            if (TriggerHandler.getInstance().isDisabled()) {
              TriggerHandler.getInstance().enable();
            }
            OBDal.getInstance().commitAndClose();
          } catch (Exception ignored) {
          }

          ExternalConnectionPool pool = ExternalConnectionPool.getInstance();
          if (pool != null && pool.hasNoConnections(t)) {
            // If the exception was caused by not having connections in pool, import entry will be
            // kept in Initial status to be processed in next cycle if there are connections. We
            // also break the loop to stop trying to process any other pending entry in this cycle.
            break;
          } else {
            try {
              importEntryManager.setImportEntryErrorIndependent(queuedImportEntry.importEntryId, t);
            } catch (Throwable ignore) {
              ImportProcessUtils.logError(logger, ignore);
            }
          }
        } finally {
          cleanUpThreadForNextCycle();
        }
      }
      if (logger.isDebugEnabled() && cnt > 0) {
        logger.debug("Runnable: " + key + ", processed " + cnt + " import entries in " + totalT
            + " millis, " + (totalT / cnt) + " per import entry, current queue size: "
            + importEntries.size());

      }
    }

    protected Queue<QueuedEntry> getImportEntryQueue() {
      return importEntries;
    }

    protected void setOBContext(QueuedEntry queuedEntry) {
      final String userId = queuedEntry.userId;
      final String orgId = queuedEntry.orgId;
      final String roleId = queuedEntry.roleId;
      final String cacheKey = userId + "_" + orgId + "_" + roleId;
      OBContext obContext = cachedOBContexts.get(cacheKey);
      if (obContext != null) {
        OBContext.setOBContext(obContext);
      } else {
        final String clientId = queuedEntry.clientId;
        OBContext.setOBContext(userId, roleId, clientId, orgId);
        cachedOBContexts.put(cacheKey, OBContext.getOBContext());
        obContext = OBContext.getOBContext();

        // initialize several things so that they are not initialized during the processing
        obContext.getEntityAccessChecker(); // forcing access checker initialization
        obContext.getOrganizationStructureProvider().reInitialize();
      }
      setVariablesSecureApp(obContext);

      // and start with a new clean session
      OBDal.getInstance().getSession().clear();

      setAuditContextInfo(userId);
    }

    private void setAuditContextInfo(String userId) {
      SessionInfo.setUserId(userId);
      SessionInfo.setProcessType(SessionInfo.IMPORT_ENTRY_PROCESS);
      SessionInfo.setProcessId(getProcessIdForAudit());
    }

    /**
     * Returns the identifier to be set for this process for audit trail:
     * <ul>
     * <li>It can be, at most, 32 characters length
     * <li>If an {@code AD_Message} entry with the same value exists, it will be used in UI
     * </ul>
     */
    protected String getProcessIdForAudit() {
      return SessionInfo.IMPORT_ENTRY_PROCESS;
    }

    protected void setVariablesSecureApp(OBContext obContext) {
      OBContext.setAdminMode(true);
      try {
        final VariablesSecureApp variablesSecureApp = new VariablesSecureApp(
            obContext.getUser().getId(), obContext.getCurrentClient().getId(),
            obContext.getCurrentOrganization().getId(), obContext.getRole().getId(),
            obContext.getLanguage().getLanguage());
        RequestContext.get().setVariableSecureApp(variablesSecureApp);
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    protected void cleanUpThreadForNextCycle() {
      OBContext.setOBContext((OBContext) null);
      RequestContext.get().setVariableSecureApp(null);
      currentProcessingEntry = null;
    }

    /**
     * Must be implemented by a subclass. Note subclass implementation must perform a commit and
     * close ({@link OBDal#commitAndClose()}) at the end of the processEntry, before it returns.
     * 
     * So that at the end of processing there should not an active Session anymore the implementor
     * is responsible for correctly closing any open session/transaction. A warning will be logged
     * if this is somehow forgotten.
     */
    protected abstract void processEntry(ImportEntry importEntry) throws Exception;

    public void setImportEntryManager(ImportEntryManager importEntryManager) {
      this.importEntryManager = importEntryManager;
    }

    public void setKey(String key) {
      this.key = key;
    }

    // is called by the processor in the main EntityManagerThread
    private void addEntry(ImportEntry importEntry) {

      // ignore the entry, queue is too large
      // prevents memory problems
      if (importEntries.size() > MAX_QUEUE_SIZE) {
        // set to level debug until other changes have been made in subclassing code
        logger.warn(
            "Ignoring import entry {} - {}, will be reprocessed later, too many queue entries {}",
            importEntry.getTypeofdata(), key, importEntries.size());
        return;
      }

      if (!importEntryIds.contains(importEntry.getId())) {
        logger.debug("Adding entry to runnable with key {} - {}", importEntry.getTypeofdata(), key);

        importEntryIds.add(importEntry.getId());
        // cache a queued entry as it has a much lower mem foot print than the import
        // entry itself
        importEntries.add(new QueuedEntry(importEntry));
      } else {
        logger.debug("Not adding entry, it is already in the list of ids {} - {} - {} ",
            importEntry.getTypeofdata(), key, importEntry.getId());
      }
    }

    public void setImportEntryProcessor(ImportEntryProcessor importEntryProcessor) {
      this.importEntryProcessor = importEntryProcessor;
    }

    public String getKey() {
      return key;
    }

    @Override
    public String toString() {
      String currentProcessing = currentProcessingEntry == null ? "none"
          : (currentProcessingEntry + " - "
              + (System.currentTimeMillis() - currentProcessingEntryStarted) + "ms");

      int queueSize = importEntries.size();
      int idsSize = importEntryIds.size();

      // IDs should always be in sync with queue (or 1 item more while synchronizing as it gets
      // first dequeued when processing starts and then removed from the IDs when it finishes).
      // Let's log them in case they are not in sync.
      boolean queuAndIdsInSync = idsSize == queueSize || idsSize == queueSize + 1;

      return "   processing: " + currentProcessing + "\n" + //
          "   queue: (" + importEntries.size() + ") - " + importEntries + //
          (queuAndIdsInSync ? "" : "\n   ids: (" + importEntryIds.size() + ") - " + importEntryIds);
    }

    // Local cache to make sure that there is a much lower mem foot print in the queue
    // of entries, so only keep the needed info to create an obcontext
    private static class QueuedEntry {
      final String importEntryId;
      final String orgId;
      final String userId;
      final String clientId;
      final String roleId;

      QueuedEntry(ImportEntry importEntry) {
        importEntryId = importEntry.getId();
        userId = importEntry.getCreatedBy().getId();
        orgId = importEntry.getOrganization().getId();
        if (importEntry.getRole() != null) {
          roleId = importEntry.getRole().getId();
        } else {
          // will use the default role of the user
          roleId = null;
        }
        clientId = importEntry.getClient().getId();
      }

      @Override
      public String toString() {
        return importEntryId;
      }
    }
  }

}
