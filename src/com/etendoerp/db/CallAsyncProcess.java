package com.etendoerp.db;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.service.db.CallProcess;

/**
 * Service class to execute database processes asynchronously.
 * <p>
 * This class extends {@link CallProcess} to inherit the core execution logic but runs the
 * database procedure in a separate thread. This is useful for long-running processes
 * to avoid blocking the user interface or triggering HTTP timeouts.
 * </p>
 * * <b>Key behaviors:</b>
 * <ul>
 * <li>Returns the {@link ProcessInstance} immediately with status 'Processing'.</li>
 * <li>Manages the transfer of {@link OBContext} to the worker thread.</li>
 * <li>Handles Hibernate session lifecycle (commit/close) for the background thread.</li>
 * </ul>
 */
public class CallAsyncProcess extends CallProcess {

  private static CallAsyncProcess instance = new CallAsyncProcess();

  // Thread pool to manage background executions.
  // Using a fixed pool prevents system resource exhaustion.
  private final ExecutorService executorService = Executors.newFixedThreadPool(10);

  public static synchronized CallAsyncProcess getInstance() {
    return instance;
  }

  public static synchronized void setInstance(CallAsyncProcess instance) {
    CallAsyncProcess.instance = instance;
  }

  /**
   * Overrides the main execution method to run asynchronously.
   * * @param process
   * the process definition.
   * @param recordID
   * the record ID.
   * @param parameters
   * map of parameters.
   * @param doCommit
   * explicit commit flag.
   * @return the ProcessInstance in 'Processing' state (Result=0, Msg='Processing...').
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
      final OBContext currentContext = OBContext.getOBContext();

      // 2. ASYNC PHASE: Submit to Executor
      executorService.submit(() -> runInBackground(pInstanceId, processId, currentContext, doCommit));

      // 3. RETURN IMMEDIATELY
      // The pInstance returned here is the initial snapshot. The UI should poll for updates.
      return pInstance;

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Internal method executed by the worker thread.
   */
  private void runInBackground(String pInstanceId, String processId, OBContext context, Boolean doCommit) {
    // A. Context Hydration
    // The new thread does not have the user session. We must set it manually.
    OBContext.setOBContext(context);
    OBContext.setAdminMode();

    try {
      // B. Re-attach Hibernate Objects
      // We cannot use the objects from the main thread (Process/ProcessInstance)
      // because they belong to a different (likely closed) Hibernate Session.
      ProcessInstance pInstance = OBDal.getInstance().get(ProcessInstance.class, pInstanceId);
      Process process = OBDal.getInstance().get(Process.class, processId);

      if (pInstance == null || process == null) {
        throw new OBException("Async Execution Failed: Process Instance or Definition not found.");
      }

      // C. Execute Logic (Reusing CallProcess logic)
      // This calls the protected method in the parent class (CallProcess)
      executeStandardProcedure(pInstance, process, doCommit);

      // D. Commit Transaction
      // In async threads, we are responsible for the transaction boundary.
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      // E. Error Handling
      // If something fails in the background, we must log it to the DB,
      // otherwise the PInstance will remain "Processing..." forever.
      try {
        OBDal.getInstance().rollbackAndClose();

        // Open a new transaction to save the error
        ProcessInstance pInstanceCtx = OBDal.getInstance().get(ProcessInstance.class, pInstanceId);
        if (pInstanceCtx != null) {
          pInstanceCtx.setResult(0L); // Error
          String msg = e.getMessage() != null ? e.getMessage() : e.toString();
          // Truncate to avoid DB errors if message is too long
          if (msg.length() > 2000) msg = msg.substring(0, 2000);
          pInstanceCtx.setErrorMsg("Async Error: " + msg);

          OBDal.getInstance().save(pInstanceCtx);
          OBDal.getInstance().commitAndClose();
        }
      } catch (Exception ex) {
        // Catastrophic failure (DB down?), just log to console
        ex.printStackTrace();
      }
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
