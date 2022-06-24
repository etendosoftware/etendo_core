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
package org.openbravo.service.importprocess;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.service.importprocess.ImportEntryManager.DaemonThreadFactory;
import org.openbravo.service.importprocess.ImportEntryManager.ImportEntryProcessorSelector;

/**
 * Class responsible for moving {@link ImportEntry} objects to the {@link ImportEntryArchive} table.
 * 
 * Works currently with a hard coded interval of 1 hour, so every hour processed import entries are
 * moved to the archive .
 * 
 * @author mtaal
 */
@ApplicationScoped
public class ImportEntryArchiveManager {

  private static final Logger log = LogManager.getLogger();

  // TODO: make this a preference
  /**
   * Every {@link #ARCHIVE_INTERVAL} the thread will check if there are Processed
   * {@link ImportEntry} records. If so then they are moved to the {@link ImportEntryArchive} table
   * in batches of 1000 records.
   */
  private static final long ARCHIVE_INTERVAL = 60 * 60000;

  private static ImportEntryArchiveManager instance;

  public static ImportEntryArchiveManager getInstance() {
    return instance;
  }

  @Inject
  @Any
  private Instance<ImportEntryArchivePreProcessor> archiveEntryPreProcessors;

  @Inject
  private ImportEntryClusterService clusterService;

  private ImportEntryArchiveThread archiveThread;
  private ExecutorService executorService;

  private boolean isShutDown = false;

  @Inject
  @Any
  private Instance<ImportEntryProcessor> entryProcessors;

  private Map<String, ImportEntryProcessor> importEntryProcessors = new ConcurrentHashMap<String, ImportEntryProcessor>();

  public ImportEntryArchiveManager() {
    instance = this;
  }

  public void start() {
    executorService = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
    archiveThread = new ImportEntryArchiveThread(this);
    executorService.execute(archiveThread);
  }

  public void shutdown() {
    if (executorService == null) {
      return;
    }
    isShutDown = true;
    log.debug("Shutting down Import Entry Archive Framework");
    executorService.shutdownNow();
    executorService = null;
  }

  private ImportEntryProcessor getImportEntryProcessor(String qualifier) {
    ImportEntryProcessor importEntryProcessor = importEntryProcessors.get(qualifier);
    if (importEntryProcessor == null) {
      importEntryProcessor = entryProcessors.select(new ImportEntryProcessorSelector(qualifier))
          .get();
      if (importEntryProcessor != null) {
        importEntryProcessors.put(qualifier, importEntryProcessor);
      } else {
        // caller should handle it
        return null;
      }
    }
    return importEntryProcessor;
  }

  private static class ImportEntryArchiveThread implements Runnable {

    private final ImportEntryArchiveManager manager;

    ImportEntryArchiveThread(ImportEntryArchiveManager manager) {
      this.manager = manager;
    }

    @Override
    public void run() {
      Thread.currentThread().setName("Import Entry Archiver");

      // don't start right away at startup, give the system time to
      // really start
      log.debug("Started, first sleep " + ARCHIVE_INTERVAL);
      doWait();
      log.debug("Run loop started");

      if (manager.isShutDown) {
        // don't even start, the import entry manager has been shut down
        return;
      }

      Date lastCreated = null;
      while (true) {
        try {

          if (manager.isShutDown) {
            return;
          }

          if (shouldWait()) {
            doWait();
            // woken, re-start from beginning of loop
            continue;
          }

          // obcontext cleared or wrong obcontext, repair
          if (OBContext.getOBContext() == null
              || !"0".equals(OBContext.getOBContext().getUser().getId())) {
            // make ourselves an admin
            OBContext.setOBContext("0", "0", "0", "0");
          }

          boolean dataProcessed = false;
          try {
            // stored is used in the whereclause to make sure that the system will continue
            // processing next records if there is one failing or giving issues, so the failing
            // is skipped in the next cycle
            String additionalClause = "";
            if (lastCreated != null) {
              additionalClause = " AND " + ImportEntry.PROPERTY_CREATIONDATE + ">:created";
            }
            OBQuery<ImportEntry> entriesQry = OBDal.getInstance()
                .createQuery(ImportEntry.class, ImportEntry.PROPERTY_IMPORTSTATUS + "='Processed' "
                    + additionalClause + " order by " + ImportEntry.PROPERTY_CREATIONDATE);
            entriesQry.setFilterOnReadableClients(false);
            entriesQry.setFilterOnReadableOrganization(false);
            if (lastCreated != null) {
              entriesQry.setNamedParameter("created", lastCreated);
            }
            entriesQry.setMaxResult(1000);

            log.debug("Querying for entries to archive");

            // do a try catch block here
            final List<ImportEntry> entries = entriesQry.list();
            log.debug("Found " + entries.size() + " import entries");
            manager.clusterService.startProcessing();
            for (ImportEntry importEntry : entries) {
              if (!isHandlingImportEntryArchiving()) {
                // detected that we are not anymore in the node in charge of handling the import
                // entries, stop archiving them
                break;
              }
              dataProcessed = true;
              lastCreated = importEntry.getCreationDate();

              if (manager.isShutDown) {
                return;
              }
              ImportEntryProcessor importEntryProcessor = ImportEntryArchiveManager.getInstance()
                  .getImportEntryProcessor(importEntry.getTypeofdata());
              if (importEntryProcessor == null || importEntryProcessor.enableArchive()) {

                ImportEntryArchive archiveEntry = createArchiveEntry(importEntry);

                for (ImportEntryArchivePreProcessor processor : manager.archiveEntryPreProcessors) {
                  processor.beforeArchive(importEntry, archiveEntry);
                }

                log.debug("Processed one entry");
                OBDal.getInstance().save(archiveEntry);
              } else {
                log.debug("Skipped one entry of type " + importEntry.getTypeofdata());
              }

              OBDal.getInstance().remove(importEntry);
            }
            // commit in batches of 1000 records
            OBDal.getInstance().commitAndClose();
          } catch (Throwable t) {
            ImportProcessUtils.logError(log, t);
          } finally {
            manager.clusterService.endProcessing();
          }

          // nothing to do in last cycle wait one hour
          if (!dataProcessed) {
            lastCreated = null;
            doWait();
          }
        } catch (Throwable t) {
          log.error(t.getMessage(), t);
        }
      }
    }

    private boolean shouldWait() {
      // - in cluster: process if we are in the node in charge of handling the import entries,
      // otherwise just wait
      // - not in cluster: do not wait
      return !isHandlingImportEntryArchiving();
    }

    private boolean isHandlingImportEntryArchiving() {
      return manager.clusterService.isHandledInCurrentNode();
    }

    private void doWait() {
      log.debug("waiting");
      try {
        Thread.sleep(ARCHIVE_INTERVAL);
      } catch (Exception ignored) {
      }
    }

    private ImportEntryArchive createArchiveEntry(ImportEntry importEntry) {
      ImportEntryArchive archiveEntry = OBProvider.getInstance().get(ImportEntryArchive.class);
      Entity entryEntity = ModelProvider.getInstance().getEntity(ImportEntry.ENTITY_NAME);
      Entity archiveEntity = ModelProvider.getInstance().getEntity(ImportEntryArchive.ENTITY_NAME);
      // copy properties with the same name
      for (Property sourceProperty : entryEntity.getProperties()) {
        // ignore these ones
        if (sourceProperty.isOneToMany() || !archiveEntity.hasProperty(sourceProperty.getName())) {
          continue;
        }
        Property targetProperty = archiveEntity.getProperty(sourceProperty.getName());
        // should be the same type
        if (targetProperty.getDomainType().getClass() != sourceProperty.getDomainType()
            .getClass()) {
          continue;
        }
        archiveEntry.set(targetProperty.getName(), importEntry.getValue(sourceProperty.getName()));
      }
      // as the id is also copied set it explicitly to new
      archiveEntry.setNewOBObject(true);
      return archiveEntry;
    }
  }
}
