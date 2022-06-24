package com.etendoerp.sequences.transactional;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class TransactionalSequenceUpdateNextTest extends WeldBaseTest {

    public static final List<Boolean> PARAMS = Arrays.asList(false, true);

    @Rule
    public ParameterCdiTestRule<Boolean> parameterValuesRule = new ParameterCdiTestRule<Boolean>(PARAMS);

    private @ParameterCdiTest boolean updateNext;
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
    public void twoCallsUpdateNextWait() throws ExecutionException, InterruptedException {
        List<SequenceValueGetter> threads = new ArrayList<>();
        threads.add(new SequenceValue().setThreadNo(1).setUpdateNext(updateNext));
        threads.add(new SequenceValue().setThreadNo(2).waitBeforeStartMs(100).setUpdateNext(updateNext));

        test(threads);
    }

    @Test
    public void twoCallsUpdateNextNoWait() throws ExecutionException, InterruptedException {
        List<SequenceValueGetter> threads = new ArrayList<>();
        threads.add(new SequenceValue().setThreadNo(1).setUpdateNext(updateNext));
        threads.add(new SequenceValue().setThreadNo(2).setUpdateNext(updateNext));

        test(threads);
    }

    private void test(List<SequenceValueGetter> threads)
            throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(threads.size());
        List<Future<String>> r = executor.invokeAll(threads, 5, TimeUnit.HOURS);
        String val1 = r.get(0).get();
        String val2 = r.get(1).get();
        if (updateNext) {
            assertThat(val1, not(equalTo(val2)));
        } else {
            assertThat(val1, equalTo(val2));
        }
    }

    private abstract class SequenceValueGetter implements Callable<String> {
        private int waitBeforeStart = 0;
        private boolean updateNext = true;
        private int threadNo = 0;

        protected abstract String getSequenceNextValue(Boolean updateNext) throws Exception;

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
            String sequenceNextVal = getSequenceNextValue(updateNext);

            System.out.println("Thread " + threadNo + " got SequenceNextValue " + sequenceNextVal + " after "
                    + (System.currentTimeMillis() - t) + " ms");


            SessionHandler.getInstance().commitAndClose();

            OBDal.getInstance().getSession().disconnect();
            return sequenceNextVal;
        }

        public SequenceValueGetter waitBeforeStartMs(int wait) {
            this.waitBeforeStart = wait;
            return this;
        }

        public SequenceValueGetter setUpdateNext(boolean updateNext) {
            this.updateNext = updateNext;
            return this;
        }
    }

    private class SequenceValue extends SequenceValueGetter {
        @Override
        protected String getSequenceNextValue(Boolean updateNext) throws Exception {
            return TransactionalSequenceUtils.getNextValueFromSequence(sequence, updateNext);
        }
    }

}
