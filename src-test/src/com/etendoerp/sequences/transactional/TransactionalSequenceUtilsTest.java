package com.etendoerp.sequences.transactional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;

public class TransactionalSequenceUtilsTest extends WeldBaseTest {

    public static final List<Boolean> PARAMS = Arrays.asList(false, true);

    @Rule
    public ParameterCdiTestRule<Boolean> parameterValuesRule = new ParameterCdiTestRule<Boolean>(PARAMS);

    private static final int WAIT_MS = 200;
    private @ParameterCdiTest boolean commitTrx;
    Sequence sequence;

    @Before
    public void before() {
        sequence = SequenceTestUtils.createTransactionalSequence(TEST_CLIENT_ID, TEST_ORG_ID);
    }

    @After
    public void after() {
        SequenceTestUtils.deleteSequence(sequence);
    }

    @Test
    public void twoCallsWait() throws ExecutionException, InterruptedException {
        List<SequenceValueGetter> threads = new ArrayList<>();
        threads.add(new SequenceValue().setThreadNo(1).commitTrx(commitTrx));
        threads.add(new SequenceValue().setThreadNo(2).waitBeforeStartMs(WAIT_MS + 100).commitTrx(commitTrx));

        test(threads);
    }

    @Test
    public void twoCallsNoWait() throws ExecutionException, InterruptedException {
        List<SequenceValueGetter> threads = new ArrayList<>();
        threads.add(new SequenceValue().setThreadNo(1).commitTrx(commitTrx));
        threads.add(new SequenceValue().setThreadNo(2).commitTrx(commitTrx));

        test(threads);
    }

    @Test
    public void twoCallsMinimumWait() throws ExecutionException, InterruptedException {
        List<SequenceValueGetter> threads = new ArrayList<>();
        threads.add(new SequenceValue().setThreadNo(1).commitTrx(commitTrx));
        threads.add(new SequenceValue().setThreadNo(2).waitBeforeStartMs(100).commitTrx(commitTrx));

        test(threads);
    }

    private void test(List<SequenceValueGetter> threads)
            throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(threads.size());
        List<Future<String>> r = executor.invokeAll(threads, 5, TimeUnit.HOURS);
        String doc1 = r.get(0).get();
        String doc2 = r.get(1).get();
        if (commitTrx) {
            assertThat(doc1, not(equalTo(doc2)));
        } else {
            assertThat(doc1, equalTo(doc2));
        }
    }

    private abstract class SequenceValueGetter implements Callable<String> {
        private int waitBeforeStart = 0;
        private boolean commitWhenTrxDone = true;
        private int threadNo = 0;

        protected abstract String getSequenceNextValue() throws Exception;

        public SequenceValueGetter setThreadNo(int threadNo) {
            this.threadNo = threadNo;
            return this;
        }

        @Override
        public final String call() throws Exception {
            Thread.sleep(waitBeforeStart);
            System.out.println("Starting thread " + (threadNo));

            setTestUserContext();

            long t = System.currentTimeMillis();
            String sequenceNextVal = getSequenceNextValue();
            System.out.println("Thread " + threadNo + " got SequenceNextValue " + sequenceNextVal + " after "
                    + (System.currentTimeMillis() - t) + " ms");

            // simulating now a long transaction
            Thread.sleep(WAIT_MS);

            if (commitWhenTrxDone) {
                SessionHandler.getInstance().commitAndClose();
            } else {
                SessionHandler.getInstance().rollback();
            }

            OBDal.getInstance().getSession().disconnect();
            return sequenceNextVal;
        }

        public SequenceValueGetter waitBeforeStartMs(int wait) {
            this.waitBeforeStart = wait;
            return this;
        }

        public SequenceValueGetter commitTrx(boolean commit) {
            this.commitWhenTrxDone = commit;
            return this;
        }
    }

    private class SequenceValue extends SequenceValueGetter {
        @Override
        protected String getSequenceNextValue() throws Exception {
            return TransactionalSequenceUtils.getNextValueFromSequence(sequence, true);
        }
    }

}
