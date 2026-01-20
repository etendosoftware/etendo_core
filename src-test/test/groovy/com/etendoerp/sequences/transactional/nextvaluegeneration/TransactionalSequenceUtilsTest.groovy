package com.etendoerp.sequences.transactional.nextvaluegeneration

import com.etendoerp.base.EBaseSpecification
import com.etendoerp.sequences.transactional.TransactionalSequenceUtils
import org.openbravo.dal.core.SessionHandler
import org.openbravo.dal.service.OBDal
import org.openbravo.model.ad.utility.Sequence
import spock.lang.Issue
import spock.lang.Narrative
import spock.lang.Title

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

@Title("Transactional Sequence next value generation")
@Narrative(""" Runs threads concurrently to obtain the next value of a Sequence """)
class TransactionalSequenceUtilsTest extends EBaseSpecification{

    private static final int WAIT_MS = 200
    Sequence sequence

    def setup() {
        sequence = GenerationUtils.createTransactionalSequence(TEST_CLIENT_ID, TEST_ORG_ID)
    }

    def cleanup() {
        GenerationUtils.deleteSequence(sequence)
    }

    @Issue("ERP-574")
    def "Obtaining two values from the same Sequence concurrently,with updateNext value set to '#updateNext', the second one is obtained after '#runAftertime' ms, with the commit transaction set to '#commitTransaction'. The values obtained MUST be '#result'"() throws ExecutionException, InterruptedException {
        expect:
        List<Future<String>> r = executeThreads(commitTransaction, updateNext, runAftertime) // == equal
        def (String val1, time1) = r.get(0).get()
        def (String val2, time2) = r.get(1).get()

        areEqualsValues(val1 , val2) == equal

        where:
        commitTransaction | updateNext | runAftertime  || equal
        true              | true       | 0             || false
        true              | true       | 100           || false
        true              | true       | WAIT_MS       || false
        true              | true       | WAIT_MS + 100 || false
        false             | true       | 0             || true
        false             | true       | 100           || true
        false             | true       | WAIT_MS       || true
        false             | true       | WAIT_MS + 100 || true

        true              | false      | 0             || true
        true              | false      | 100           || true
        true              | false      | WAIT_MS       || true
        true              | false      | WAIT_MS + 100 || true
        false             | false      | 0             || true
        false             | false      | 100           || true
        false             | false      | WAIT_MS       || true
        false             | false      | WAIT_MS + 100 || true

        result = equal ? "equals" : "not equals"
    }

    private executeThreads(Boolean commitTransaction, Boolean updateNext, int waitBeforeStart) {
        List<SequenceValueGetter> threads = new ArrayList<>()
        threads.add(new SequenceValue().setThreadNo(1).commitTrx(commitTransaction).setUpdateNext(updateNext))
        threads.add(new SequenceValue().setThreadNo(2).waitBeforeStartMs(waitBeforeStart).commitTrx(commitTransaction).setUpdateNext(updateNext))

        ExecutorService executor = Executors.newFixedThreadPool(threads.size())
        return executor.invokeAll(threads, 5, TimeUnit.HOURS)
    }

    private static Boolean areEqualsValues(String val1, String val2) {
        return val1 == val2
    }

    private abstract class SequenceValueGetter implements Callable<Object> {
        private int waitBeforeStart = 0
        private boolean commitWhenTrxDone = true
        private int threadNo = 0
        private boolean updateNext = true

        protected abstract String getSequenceNextValue(Boolean updateNext) throws Exception;

        SequenceValueGetter setThreadNo(int threadNo) {
            this.threadNo = threadNo
            return this
        }

        @Override
        def call() throws Exception {
            Thread.sleep(waitBeforeStart)
            System.out.println("Starting thread " + (threadNo))

            setTestUserContext()

            long t = System.currentTimeMillis()
            String sequenceNextVal = getSequenceNextValue(updateNext)
            long time = System.currentTimeMillis() - t
            System.out.println("Thread " + threadNo + " got SequenceNextValue " + sequenceNextVal + " after "
                    + time + " ms")

            // simulating now a long transaction
            Thread.sleep(WAIT_MS)

            if (commitWhenTrxDone) {
                SessionHandler.getInstance().commitAndClose()
            } else {
                SessionHandler.getInstance().rollback()
            }

            return [sequenceNextVal, time]
        }

        SequenceValueGetter waitBeforeStartMs(int wait) {
            this.waitBeforeStart = wait
            return this
        }

        SequenceValueGetter commitTrx(boolean commit) {
            this.commitWhenTrxDone = commit
            return this
        }

        SequenceValueGetter setUpdateNext(boolean updateNext) {
            this.updateNext = updateNext
            return this
        }

    }

    private class SequenceValue extends SequenceValueGetter {
        @Override
        protected String getSequenceNextValue(Boolean updateNext) throws Exception {
            return TransactionalSequenceUtils.getNextValueFromSequence(sequence, updateNext)
        }
    }

}
