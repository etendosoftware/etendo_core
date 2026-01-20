package com.etendoerp.db;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.service.db.CallProcess;

/**
 * Service class to execute database processes asynchronously.
 * <p>
 * This class extends {@link CallProcess} to inherit the core execution logic but runs the
 * database procedure in a separate thread using an {@link ExecutorService}. This is useful
 * for long-running processes to avoid blocking the user interface or triggering HTTP timeouts.
 * </p>
 * <p>
 * <b>Key behaviors:</b>
 * <ul>
 *   <li>Returns the {@link ProcessInstance} immediately with a 'Processing' status.</li>
 *   <li>Manages the secure transfer of {@link OBContext} values to the worker thread.</li>
 *   <li>Handles Hibernate session lifecycle (commit/close) and transaction boundaries
 *       for the background thread.</li>
 *   <li>Provides automatic error reporting back to the {@link ProcessInstance} if the
 *       background execution fails.</li>
 * </ul>
 * </p>
 *
 * @author etendo
 * @see CallProcess
 * @see ProcessInstance
 * @see OBContext
 */
public class CallAsyncProcess extends CallProcess {

  public static Logger log4j = LogManager.getLogger(CallAsyncProcess.class);

  private static final int DEFAULT_THREAD_POOL_SIZE = 10;

  private static CallAsyncProcess instance;

  // Thread pool to manage background executions.
  // Using a fixed pool prevents system resource exhaustion.
  private final ExecutorService executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);

  /**
   * Gets the singleton instance of {@code CallAsyncProcess}.
   *
   * @return the singleton instance.
   */
  public static synchronized CallAsyncProcess getInstance() {
    if (instance == null) {
      instance = new CallAsyncProcess();
    }
    return instance;
  }

  /**
   * Internal class to encapsulate {@link OBContext} values for thread-safe transfer.
   * <p>
   * Instead of passing the full {@code OBContext} object (which may have session-specific state
   * or non-thread-safe references), we extract and pass only the essential IDs needed to
   * recreate the context in the worker thread.
   * </p>
   */
  private static class ContextValues {
    final String userId;
    final String roleId;
    final String clientId;
    final String organizationId;
    final String warehouseId;
    final String languageId;

    /**
     * Captures the current state of the provided {@link OBContext}.
     *
     * @param context
     *     the context to capture values from.
     */
    ContextValues(OBContext context) {
      this.userId = context.getUser() != null ? context.getUser().getId() : null;
      this.roleId = context.getRole() != null ? context.getRole().getId() : null;
      this.clientId = context.getCurrentClient() != null ? context.getCurrentClient().getId() : null;
      this.organizationId = context.getCurrentOrganization() != null ? context.getCurrentOrganization().getId() : null;
      this.warehouseId = context.getWarehouse() != null ? context.getWarehouse().getId() : null;
      this.languageId = context.getLanguage() != null ? context.getLanguage().getId() : null;
    }
  }


  /**
   * Overrides the main execution method to run the process asynchronously.
   * <p>
   * This method performs a synchronous "Preparation Phase" where the {@link ProcessInstance}
   * is created and persisted, followed by an "Asynchronous Phase" where the actual
   * database procedure is submitted to the thread pool.
   * </p>
   *
   * @param process
   *     the process definition to execute.
   * @param recordID
   *     the ID of the record associated with the execution (optional).
   * @param parameters
   *     a map of parameters to be passed to the process.
   * @param doCommit
   *     explicit commit flag to be passed to the stored procedure.
   * @return a {@link ProcessInstance} in 'Processing' state. The caller should poll this
   *     instance for updates on the execution result.
   */
  @Override
  public ProcessInstance callProcess(Process process, String recordID, Map<String, ?> parameters, Boolean doCommit) {
    OBContext.setAdminMode();
    try {
      // 1. SYNC PHASE: Prepare Data
      // We must create the PInstance in the main thread to return the ID immediately to the user.
      ProcessInstance pInstance = createAndPersistInstance(process, recordID, parameters);

      // Set initial status specifically for Async (though createAndPersist usually sets defaults)
      pInstance.setResult(0L);
      pInstance.setErrorMsg("Processing in background...");
      OBDal.getInstance().save(pInstance);
      OBDal.getInstance().flush();

      // Capture critical IDs and Context to pass to the thread
      final String pInstanceId = pInstance.getId();
      final String processId = process.getId();
      final ContextValues contextValues = new ContextValues(OBContext.getOBContext());

      // 2. ASYNC PHASE: Submit to Executor
      executorService.submit(() -> runInBackground(pInstanceId, processId, contextValues, doCommit));

      // 3. RETURN IMMEDIATELY
      // The pInstance returned here is the initial snapshot. The UI should poll for updates.
      return pInstance;

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Internal method executed by the worker thread.
   * <p>
   * This method performs the following steps:
   * <ol>
   *   <li>Hydrates the {@link OBContext} for the new thread.</li>
   *   <li>Retrieves the {@link ProcessInstance} and {@link Process} from the database.</li>
   *   <li>Executes the database procedure.</li>
   *   <li>Manages the transaction (commit or clear).</li>
   * </ol>
   * </p>
   *
   * @param pInstanceId
   *     the ID of the {@link ProcessInstance}.
   * @param processId
   *     the ID of the {@link Process}.
   * @param contextValues
   *     the captured context values to hydrate the new thread.
   * @param doCommit
   *     whether to commit the transaction after execution.
   */
  private void runInBackground(String pInstanceId, String processId, ContextValues contextValues, Boolean doCommit) {
    try {
      // A. Context Hydration
      hydrateContext(contextValues);

      // B. Re-attach Hibernate Objects
      ProcessInstance pInstance = OBDal.getInstance().get(ProcessInstance.class, pInstanceId);
      Process process = OBDal.getInstance().get(Process.class, processId);

      if (pInstance == null || process == null) {
        throw new OBException("Async Execution Failed: Process Instance or Definition not found.");
      }

      // C. Execute Logic
      executeStandardProcedure(pInstance, process, doCommit);

      // D. Commit Transaction
      if (Boolean.TRUE.equals(doCommit)) {
        OBDal.getInstance().commitAndClose();
      } else {
        OBDal.getInstance().getSession().clear();
      }

    } catch (Exception e) {
      handleAsyncError(pInstanceId, e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Recreates the {@link OBContext} in the worker thread using captured values.
   * This is necessary because the worker thread starts with an empty context.
   *
   * @param contextValues
   *     the values used to populate the new context.
   */
  private void hydrateContext(ContextValues contextValues) {
    OBContext newContext = OBContext.getOBContext();
    if (contextValues.userId != null) {
      newContext.setUser(OBDal.getInstance().get(User.class, contextValues.userId));
    }
    if (contextValues.roleId != null) {
      newContext.setRole(OBDal.getInstance().get(Role.class, contextValues.roleId));
    }
    if (contextValues.clientId != null) {
      newContext.setCurrentClient(OBDal.getInstance().get(Client.class, contextValues.clientId));
    }
    if (contextValues.organizationId != null) {
      newContext.setCurrentOrganization(OBDal.getInstance().get(Organization.class, contextValues.organizationId));
    }
    if (contextValues.warehouseId != null) {
      newContext.setWarehouse(OBDal.getInstance().get(Warehouse.class, contextValues.warehouseId));
    }
    if (contextValues.languageId != null) {
      newContext.setLanguage(OBDal.getInstance().get(Language.class, contextValues.languageId));
    }
  }

  /**
   * Handles errors occurring during background execution by logging them to the {@link ProcessInstance}.
   * It rolls back the current transaction and starts a new one to persist the error message.
   *
   * @param pInstanceId
   *     the ID of the {@link ProcessInstance} to update.
   * @param e
   *     the exception that occurred.
   */
  private void handleAsyncError(String pInstanceId, Exception e) {
    try {
      OBDal.getInstance().rollbackAndClose();

      // Open a new transaction to save the error
      ProcessInstance pInstanceCtx = OBDal.getInstance().get(ProcessInstance.class, pInstanceId);
      if (pInstanceCtx != null) {
        pInstanceCtx.setResult(0L); // Error
        String msg = e.getMessage() != null ? e.getMessage() : e.toString();
        // Truncate to avoid DB errors if message is too long
        if (msg.length() > 2000) {
          msg = msg.substring(0, 2000);
        }
        pInstanceCtx.setErrorMsg("Async Error: " + msg);

        OBDal.getInstance().save(pInstanceCtx);
        OBDal.getInstance().commitAndClose();
      }
    } catch (Exception ex) {
      log4j.error("Failed to log async error to ProcessInstance " + pInstanceId, ex);
    }
  }
}
