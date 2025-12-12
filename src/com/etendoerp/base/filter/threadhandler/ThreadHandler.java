package com.etendoerp.base.filter.threadhandler;

import com.etendoerp.base.filter.core.FilterContext;
import com.etendoerp.base.filter.core.FilterException;

/**
 * Contract for thread-local state handlers in the filter chain.
 *
 * <p>ThreadHandlers manage thread-local resources and state across filter execution.
 * They execute in three phases: BEFORE (setup), ACTION (implicit), CLEANUP (teardown).</p>
 *
 * <p><strong>Lifecycle:</strong></p>
 * <ol>
 *   <li><strong>before():</strong> Initialize thread-local state, begin transactions</li>
 *   <li><em>(Filter execution happens here)</em></li>
 *   <li><strong>cleanup():</strong> Release resources, commit/rollback transactions</li>
 * </ol>
 *
 * <p><strong>Execution Order:</strong></p>
 * <ul>
 *   <li>BEFORE: Execute in registration order (handler1, handler2, handler3)</li>
 *   <li>CLEANUP: Execute in REVERSE order (handler3, handler2, handler1)</li>
 * </ul>
 *
 * <p><strong>Error Handling:</strong></p>
 * <ul>
 *   <li>cleanup() ALWAYS executes for handlers that completed before()</li>
 *   <li>cleanup() must be idempotent (safe to call multiple times)</li>
 *   <li>cleanup() exceptions are logged but don't prevent other cleanups</li>
 * </ul>
 *
 * <p><strong>Implementation Example:</strong></p>
 * <pre>{@code
 * public class TransactionThreadHandler implements ThreadHandler {
 *
 *   private static final ThreadLocal<Transaction> currentTx = new ThreadLocal<>();
 *
 *   @Override
 *   public void before(FilterContext context) throws FilterException {
 *     Transaction tx = TransactionManager.beginTransaction();
 *     currentTx.set(tx);
 *     context.setAttribute("transaction", tx);
 *   }
 *
 *   @Override
 *   public void cleanup(FilterContext context, boolean errorOccurred) {
 *     Transaction tx = currentTx.get();
 *     if (tx != null) {
 *       try {
 *         if (errorOccurred) {
 *           tx.rollback();
 *         } else {
 *           tx.commit();
 *         }
 *       } finally {
 *         currentTx.remove();
 *       }
 *     }
 *   }
 *
 *   @Override
 *   public String getName() {
 *     return "TransactionThreadHandler";
 *   }
 * }
 * }</pre>
 *
 * @since Etendo 24.Q4
 */
public interface ThreadHandler {

  /**
   * Executes BEFORE phase setup.
   *
   * <p>Initialize thread-local state, begin transactions, set up context.
   * This method is called before filter execution in registration order.</p>
   *
   * <p>If this method throws an exception, cleanup() will still be called
   * for handlers that completed before() successfully.</p>
   *
   * @param context filter execution context
   * @throws FilterException if setup fails
   */
  void before(FilterContext context) throws FilterException;

  /**
   * Executes CLEANUP phase teardown.
   *
   * <p>Release resources, commit/rollback transactions, clear thread-local state.
   * This method is called after filter execution in REVERSE registration order.</p>
   *
   * <p><strong>CRITICAL:</strong> This method MUST be idempotent and must NOT throw
   * exceptions that would prevent other handlers from cleaning up. Log errors
   * instead of throwing.</p>
   *
   * @param context filter execution context
   * @param errorOccurred true if an error occurred during before() or filter execution
   */
  void cleanup(FilterContext context, boolean errorOccurred);

  /**
   * Returns the unique name of this handler for logging and debugging.
   *
   * @return handler name (must not be null or empty)
   */
  String getName();

  /**
   * Indicates whether this handler requires transaction support.
   *
   * <p>Default: false. Override to return true if this handler manages
   * database transactions or requires transactional context.</p>
   *
   * @return true if handler needs transaction support
   */
  default boolean requiresTransaction() {
    return false;
  }

  /**
   * Indicates whether this handler modifies thread-local state.
   *
   * <p>Default: true. If false, the handler is stateless and cleanup
   * may be optimized.</p>
   *
   * @return true if handler uses thread-local storage
   */
  default boolean usesThreadLocalState() {
    return true;
  }
}
