package com.etendoerp.base.filter.filters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.etendoerp.base.filter.core.FilterContext;
import com.etendoerp.base.filter.core.FilterException;
import com.etendoerp.base.filter.core.FilterExecutor;
import org.openbravo.dal.service.OBDal;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.sql.Connection;

/**
 * Core filter for database transaction management.
 *
 * <p>Manages transaction lifecycle with READ_COMMITTED isolation level.
 * Integrates with Hibernate/OBDal transaction infrastructure.</p>
 *
 * <p><strong>Transaction Lifecycle:</strong></p>
 * <ol>
 *   <li><strong>execute():</strong> Begin transaction with READ_COMMITTED isolation</li>
 *   <li><em>(Other filters and business logic execute)</em></li>
 *   <li><strong>cleanup():</strong> Commit on success, rollback on error</li>
 * </ol>
 *
 * <p><strong>Isolation Level:</strong></p>
 * <ul>
 *   <li>Uses READ_COMMITTED to prevent dirty reads while allowing concurrent access</li>
 *   <li>Suitable for most business operations in ERP systems</li>
 *   <li>Balances consistency with performance</li>
 * </ul>
 *
 * <p><strong>Error Handling:</strong></p>
 * <ul>
 *   <li>Automatic rollback on any exception</li>
 *   <li>Proper cleanup even if commit/rollback fails</li>
 *   <li>Thread-local transaction state cleared in all cases</li>
 * </ul>
 *
 * <p><strong>Context Attributes:</strong></p>
 * <ul>
 *   <li>{@code "transaction"} - Hibernate Transaction object</li>
 *   <li>{@code "transaction.active"} - Boolean flag indicating active transaction</li>
 *   <li>{@code "transaction.startTime"} - Long timestamp when transaction started</li>
 * </ul>
 *
 * <p><strong>Priority:</strong> 10 (executes early to establish transaction context)</p>
 *
 * @since Etendo 24.Q4
 */
public class TransactionManagementFilter implements FilterExecutor {

  private static final Logger log = LogManager.getLogger(TransactionManagementFilter.class);

  private static final String ATTR_TRANSACTION = "transaction";
  private static final String ATTR_TRANSACTION_ACTIVE = "transaction.active";
  private static final String ATTR_TRANSACTION_START_TIME = "transaction.startTime";

  private static final int PRIORITY = 10;
  private static final int TIMEOUT_SECONDS = 60;

  @Override
  public void execute(FilterContext context) throws FilterException {
    log.debug("[{}] Checking database transaction status",
        context.getRequestId());

    try {
      // Get Hibernate session
      Session session = OBDal.getInstance().getSession();
      if (session == null) {
        throw new FilterException("Hibernate session not available", 500);
      }

      // Check if transaction is already active (from legacy filters)
      Transaction existingTx = session.getTransaction();
      boolean transactionAlreadyActive = existingTx != null && existingTx.isActive();

      if (transactionAlreadyActive) {
        log.debug("[{}] Transaction already active - using existing transaction (legacy mode compatibility)",
            context.getRequestId());
        context.setAttribute(ATTR_TRANSACTION, existingTx);
        context.setAttribute(ATTR_TRANSACTION_ACTIVE, false); // We didn't create it
        context.setAttribute("transaction.ownedByFilter", false);
        return;
      }

      // Begin new transaction
      log.debug("[{}] Beginning new database transaction (isolation=READ_COMMITTED)",
          context.getRequestId());
      Transaction transaction = session.beginTransaction();
      if (transaction == null) {
        throw new FilterException("Failed to begin transaction", 500);
      }

      // Set isolation level to READ_COMMITTED
      Connection connection = session.doReturningWork(conn -> {
        try {
          conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
          return conn;
        } catch (Exception e) {
          log.warn("[{}] Failed to set isolation level to READ_COMMITTED: {}",
              context.getRequestId(), e.getMessage());
          return conn;
        }
      });

      // Store transaction in context
      context.setAttribute(ATTR_TRANSACTION, transaction);
      context.setAttribute(ATTR_TRANSACTION_ACTIVE, true);
      context.setAttribute("transaction.ownedByFilter", true); // We created it
      context.setAttribute(ATTR_TRANSACTION_START_TIME, System.currentTimeMillis());

      log.debug("[{}] Transaction begun successfully (isolation=READ_COMMITTED)",
          context.getRequestId());

    } catch (Exception e) {
      log.error("[{}] Failed to begin transaction", context.getRequestId(), e);
      if (e instanceof FilterException) {
        throw (FilterException) e;
      }
      throw new FilterException("Failed to begin database transaction: " + e.getMessage(), 500);
    }
  }

  @Override
  public void cleanup(FilterContext context, boolean errorOccurred) {
    Transaction transaction = context.getAttribute(ATTR_TRANSACTION, Transaction.class);
    Boolean transactionActive = context.getAttribute(ATTR_TRANSACTION_ACTIVE, Boolean.class);
    Boolean ownedByFilter = context.getAttribute("transaction.ownedByFilter", Boolean.class);

    if (transaction == null || !Boolean.TRUE.equals(transactionActive)) {
      log.debug("[{}] No active transaction to clean up", context.getRequestId());
      return;
    }

    // If we didn't create the transaction, don't try to commit/rollback it
    if (!Boolean.TRUE.equals(ownedByFilter)) {
      log.debug("[{}] Transaction not owned by this filter - skipping commit/rollback (legacy filter will handle it)",
          context.getRequestId());
      return;
    }

    try {
      if (errorOccurred) {
        // Rollback on error
        if (transaction.isActive()) {
          log.info("[{}] Rolling back transaction (error occurred)", context.getRequestId());
          transaction.rollback();
          logTransactionDuration(context, "ROLLBACK");
        } else {
          log.warn("[{}] Transaction already inactive (cannot rollback)", context.getRequestId());
        }
      } else {
        // Commit on success
        if (transaction.isActive()) {
          log.debug("[{}] Committing transaction", context.getRequestId());
          transaction.commit();
          logTransactionDuration(context, "COMMIT");
        } else {
          log.warn("[{}] Transaction already inactive (cannot commit)", context.getRequestId());
        }
      }

    } catch (Exception e) {
      log.error("[{}] Error during transaction cleanup (errorOccurred={})",
          context.getRequestId(), errorOccurred, e);

      // Attempt rollback if commit failed
      if (!errorOccurred) {
        try {
          if (transaction.isActive()) {
            log.warn("[{}] Commit failed - attempting rollback", context.getRequestId());
            transaction.rollback();
          }
        } catch (Exception rollbackEx) {
          log.error("[{}] Rollback after commit failure also failed",
              context.getRequestId(), rollbackEx);
        }
      }

    } finally {
      // Clear transaction state
      context.removeAttribute(ATTR_TRANSACTION);
      context.setAttribute(ATTR_TRANSACTION_ACTIVE, false);

      log.debug("[{}] Transaction cleanup completed", context.getRequestId());
    }
  }

  /**
   * Logs transaction duration and outcome.
   *
   * @param context filter context
   * @param outcome transaction outcome (COMMIT or ROLLBACK)
   */
  private void logTransactionDuration(FilterContext context, String outcome) {
    Long startTime = context.getAttribute(ATTR_TRANSACTION_START_TIME, Long.class);
    if (startTime != null) {
      long duration = System.currentTimeMillis() - startTime;
      log.info("[{}] Transaction {} completed (duration={}ms)",
          context.getRequestId(), outcome, duration);
    }
  }

  @Override
  public boolean shouldExecute(FilterContext context) {
    // Execute for all requests except static resources
    String path = context.getPath();

    // Skip for static resources
    if (path.endsWith(".js") || path.endsWith(".css") ||
        path.endsWith(".png") || path.endsWith(".jpg") ||
        path.endsWith(".gif") || path.endsWith(".ico") ||
        path.endsWith(".svg") || path.endsWith(".woff") ||
        path.endsWith(".woff2") || path.endsWith(".ttf")) {
      log.trace("[{}] Skipping transaction for static resource: {}",
          context.getRequestId(), path);
      return false;
    }

    return true;
  }

  @Override
  public int getPriority() {
    return PRIORITY;
  }

  @Override
  public String getName() {
    return "TransactionManagementFilter";
  }

  @Override
  public int getTimeout() {
    return TIMEOUT_SECONDS;
  }
}
