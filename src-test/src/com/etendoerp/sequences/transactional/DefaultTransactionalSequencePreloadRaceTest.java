package com.etendoerp.sequences.transactional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;

/**
 * Reproduces ETP-4469's root cause with the exact interleaving observed in production
 * (two concurrent {@code /sws/neo/batch} requests, "Perez S.A." vs "Ortiz Group"):
 *
 * <ol>
 *   <li>T1 locks the sequence ({@code SELECT ... FOR UPDATE} via
 *       {@link TransactionalSequenceUtils#lockSequence(String)}), computes its value and
 *       keeps the transaction OPEN — a real batch holds the lock until the whole batch
 *       commits, so this window is large.</li>
 *   <li>While T1 holds the lock, T2 performs the UNLOCKED entity lookup that
 *       {@code DefaultTransactionalSequence.generateValue()} always does first
 *       ({@code getSequenceFromParameters()}), hydrating the {@link Sequence} entity —
 *       with the still-committed, pre-increment {@code nextAssignedNumber} — into T2's
 *       Hibernate session.</li>
 *   <li>T1 commits.</li>
 *   <li>T2 calls {@code getNextValueFromSequence(..., true)}. Its internal
 *       {@code lockSequence()} correctly blocks until T1's commit and the locking SQL
 *       returns the fresh row — but Hibernate resolves the query result against the entity
 *       instance already cached in the session (identity map) and discards the fresh
 *       column values. T2 computes its value from the stale {@code nextAssignedNumber}
 *       and produces a DUPLICATE of T1's value.</li>
 * </ol>
 *
 * <p>{@link TransactionalSequenceUtilsTest} never catches this because its threads touch
 * the {@link Sequence} entity for the first time inside {@code lockSequence()}, so the
 * locking query hydrates a fresh instance. The unlocked preload into the same session is
 * the poison, and it is exactly what the production caller does.</p>
 */
public class DefaultTransactionalSequencePreloadRaceTest extends WeldBaseTest {

  private static final int HOLD_LOCK_MS = 800;
  private static final long LATCH_TIMEOUT_S = 30;

  Sequence sequence;
  private CountDownLatch firstValueComputed;

  @Before
  public void before() {
    sequence = SequenceTestUtils.createTransactionalSequence(TEST_CLIENT_ID, TEST_ORG_ID);
    firstValueComputed = new CountDownLatch(1);
  }

  @After
  public void after() {
    SequenceTestUtils.deleteSequence(sequence);
  }

  @Test
  public void preloadedSessionMustNotReuseStaleSequenceValueAcrossConcurrentTransactions()
      throws ExecutionException, InterruptedException {
    List<Callable<String>> threads = new ArrayList<>();
    threads.add(new LockHoldingGetter());
    threads.add(new PreloadingRacerGetter());

    ExecutorService executor = Executors.newFixedThreadPool(threads.size());
    List<Future<String>> r = executor.invokeAll(threads, 5, TimeUnit.MINUTES);
    String doc1 = r.get(0).get();
    String doc2 = r.get(1).get();
    System.out.println("Holder value: " + doc1 + " | Racer value: " + doc2);

    assertThat(doc2, not(equalTo(doc1)));
  }

  /**
   * T1: computes a sequence value (taking the row lock) and then keeps the transaction
   * open for {@link #HOLD_LOCK_MS} before committing — mirroring a batch request that
   * keeps working after the BP insert and only commits at the end.
   */
  private class LockHoldingGetter implements Callable<String> {
    @Override
    public String call() throws Exception {
      setTestUserContext();
      Sequence preloaded = OBDal.getInstance().get(Sequence.class, sequence.getId());
      String value = TransactionalSequenceUtils.getNextValueFromSequence(preloaded, true);
      System.out.println("Holder computed " + value + ", holding transaction open...");
      firstValueComputed.countDown();
      Thread.sleep(HOLD_LOCK_MS);
      SessionHandler.getInstance().commitAndClose();
      OBDal.getInstance().getSession().disconnect();
      System.out.println("Holder committed.");
      return value;
    }
  }

  /**
   * T2: waits until T1 holds the lock, performs the UNLOCKED preload (the
   * {@code getSequenceFromParameters()} step of {@code generateValue()}) and only then
   * requests the locked next value, blocking on T1's lock until T1 commits.
   */
  private class PreloadingRacerGetter implements Callable<String> {
    @Override
    public String call() throws Exception {
      if (!firstValueComputed.await(LATCH_TIMEOUT_S, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Holder thread never computed its value");
      }
      setTestUserContext();
      Sequence preloaded = OBDal.getInstance().get(Sequence.class, sequence.getId());
      System.out.println("Racer preloaded sequence with nextAssignedNumber="
          + preloaded.getNextAssignedNumber() + " while holder still holds the lock");
      String value = TransactionalSequenceUtils.getNextValueFromSequence(preloaded, true);
      System.out.println("Racer computed " + value);
      SessionHandler.getInstance().commitAndClose();
      OBDal.getInstance().getSession().disconnect();
      return value;
    }
  }

}
