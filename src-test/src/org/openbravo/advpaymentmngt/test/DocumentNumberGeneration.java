package org.openbravo.advpaymentmngt.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.service.db.DalConnectionProvider;


/**
 * Defines test cases to guarantee uniqueness in document number generation.
 * 
 * There are 2 methods to obtain document numbers: Utility.getDocumentNo and
 * FIN_Uilitiy.getDocumentNo; first one uses PL function to obtain it, whereas second one uses only
 * DAL.
 * 
 * In case of concurrent requests for the same document type, locks should occur to ensure the
 * returned document number is unique. This locks should be seen in both directions DAL &lt;--&gt;
 * PL.
 * 
 * In case of 1st concurrent thread commits, 2nd thread should get a different doc number than first
 * one, if 1st rolls back, 2nd should get same number.
 * 
 * @author alostale
 * 
 */
public class DocumentNumberGeneration extends WeldBaseTest {
  public static final List<Boolean> PARAMS = Arrays.asList(false, true);

  @Rule
  public ParameterCdiTestRule<Boolean> parameterValuesRule = new ParameterCdiTestRule<Boolean>(PARAMS);

  private static final String DOC_TYPE_ID = "466AF4B0136A4A3F9F84129711DA8BD3";
  private static final String TABLE_NAME = "C_Order";
  private static final int WAIT_MS = 200;
  private @ParameterCdiTest boolean commitTrx;

  public DocumentNumberGeneration() {
  }

  /** 2 concurrent dal calls */
  @Test
  public void twoDalCalls() throws InterruptedException, ExecutionException {
    List<DocumentNumberGetter> threads = new ArrayList<DocumentNumberGetter>();
    threads.add(new DALDocumentNumberGetter().setThreadNo(1).commitTrx(commitTrx));
    threads.add(
        new DALDocumentNumberGetter().setThreadNo(2).waitBeforeStartMs(100).commitTrx(commitTrx));
    test(threads);
  }

  /** 2 concurrent PL calls */
  @Test
  public void twoPLCalls() throws InterruptedException, ExecutionException {
    List<DocumentNumberGetter> threads = new ArrayList<DocumentNumberGetter>();
    threads.add(new DBDocumentNumberGetter().setThreadNo(1).commitTrx(commitTrx));
    threads.add(new DBDocumentNumberGetter().setThreadNo(2).commitTrx(commitTrx));
    test(threads);
  }

  /** one dal, wait till it finishes, another dal call */
  @Test
  public void twoDalCallsSequential() throws InterruptedException, ExecutionException {
    List<DocumentNumberGetter> threads = new ArrayList<DocumentNumberGetter>();
    threads.add(new DALDocumentNumberGetter().setThreadNo(1).commitTrx(commitTrx));
    threads.add(new DALDocumentNumberGetter().setThreadNo(2)
        .waitBeforeStartMs(WAIT_MS + 100)
        .commitTrx(commitTrx));
    test(threads);
  }

  /** dal and pl concurrently, dal starts */
  @Test
  public void dalFirstThenPL() throws InterruptedException, ExecutionException {
    List<DocumentNumberGetter> threads = new ArrayList<DocumentNumberGetter>();
    threads.add(new DALDocumentNumberGetter().setThreadNo(1).commitTrx(commitTrx));
    threads.add(
        new DBDocumentNumberGetter().setThreadNo(2).waitBeforeStartMs(100).commitTrx(commitTrx));
    test(threads);
  }

  /** pl and dal concurrently, pl starts */
  @Test
  public void plFirstThenDal() throws InterruptedException, ExecutionException {
    List<DocumentNumberGetter> threads = new ArrayList<DocumentNumberGetter>();
    threads.add(new DALDocumentNumberGetter().setThreadNo(1).commitTrx(commitTrx));
    threads.add(
        new DBDocumentNumberGetter().setThreadNo(2).waitBeforeStartMs(100).commitTrx(commitTrx));
    test(threads);
  }

  /** executes all the threads and asserts the results */
  private void test(List<DocumentNumberGetter> threads)
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

  private abstract class DocumentNumberGetter implements Callable<String> {
    private int waitBeforeStart = 0;
    private boolean commitWhenTrxDone = true;
    private int threadNo = 0;

    protected abstract String getDocumentNumber() throws Exception;

    public DocumentNumberGetter setThreadNo(int threadNo) {
      this.threadNo = threadNo;
      return this;
    }

    @Override
    public final String call() throws Exception {
      Thread.sleep(waitBeforeStart);
      System.out.println("Starting thread " + (threadNo));
      setTestUserContext();
      long t = System.currentTimeMillis();
      String documentNo = getDocumentNumber();
      System.out.println("Thread " + threadNo + " got DocumentNo " + documentNo + " after "
          + (System.currentTimeMillis() - t) + " ms");

      // simulating now a long transaction
      Thread.sleep(WAIT_MS);

      if (commitWhenTrxDone) {
        SessionHandler.getInstance().commitAndClose();
      } else {
        SessionHandler.getInstance().rollback();
      }

      OBDal.getInstance().getSession().disconnect();
      return documentNo;
    }

    public DocumentNumberGetter waitBeforeStartMs(int wait) {
      this.waitBeforeStart = wait;
      return this;
    }

    public DocumentNumberGetter commitTrx(boolean commit) {
      this.commitWhenTrxDone = commit;
      return this;
    }
  }

  private class DBDocumentNumberGetter extends DocumentNumberGetter {
    @Override
    protected String getDocumentNumber() throws Exception {
      VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getCurrentClient().getId(),
          OBContext.getOBContext().getCurrentOrganization().getId());
      return Utility.getDocumentNo(OBDal.getInstance().getConnection(false),
          new DalConnectionProvider(false), vars, "", TABLE_NAME, "", DOC_TYPE_ID, false, true);
    }
  }

  private class DALDocumentNumberGetter extends DocumentNumberGetter {
    @Override
    protected String getDocumentNumber() throws Exception {
      return FIN_Utility.getDocumentNo(OBDal.getInstance().get(DocumentType.class, DOC_TYPE_ID),
          TABLE_NAME);
    }
  }
}
