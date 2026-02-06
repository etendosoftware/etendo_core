package org.openbravo.advpaymentmngt.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.service.db.DalConnectionProvider;


/**
 * Defines test cases to guarantee uniqueness in document number generation.
 * There are 2 methods to obtain document numbers: Utility.getDocumentNo and
 * FIN_Uilitiy.getDocumentNo; first one uses PL function to obtain it, whereas second one uses only
 * DAL.
 * In case of concurrent requests for the same document type, locks should occur to ensure the
 * returned document number is unique. This locks should be seen in both directions DAL &lt;--&gt;
 * PL.
 * In case of 1st concurrent thread commits, 2nd thread should get a different doc number than first
 * one, if 1st rolls back, 2nd should get same number.
 *
 * @author alostale
 */
public class DocumentNumberGeneration extends WeldBaseTest {

  private static final Logger log = LogManager.getLogger();

  private static final String DOC_TYPE_ID = "466AF4B0136A4A3F9F84129711DA8BD3";
  private static final String TABLE_NAME = "C_Order";
  private static final int WAIT_MS = 200;

  public DocumentNumberGeneration() {
  }

  @Override
  protected void initializeDalLayer() throws Exception {
    super.initializeDalLayer();
    log.info("DAL Layer initialized for ReversePaymentTest");
  }

  @BeforeAll
  public static void setUpTestContext() {
    try {
      TestUtility.setTestContext();
      VariablesSecureApp vsa = new VariablesSecureApp(
          OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getCurrentClient().getId(),
          OBContext.getOBContext().getCurrentOrganization().getId(),
          OBContext.getOBContext().getRole().getId()
      );
      RequestContext.get().setVariableSecureApp(vsa);
      vsa.setSessionValue("#FormatOutput|generalQtyEdition", "#0.######");
      vsa.setSessionValue("#GroupSeparator|generalQtyEdition", ",");
      vsa.setSessionValue("#DecimalSeparator|generalQtyEdition", ".");
      log.info("Test context initialized for ReversePaymentTest");
    } catch (Exception e) {
      log.error("Failed to set test context", e);
      throw new RuntimeException("Cannot set test context", e);
    }
  }

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  /** 2 concurrent dal calls */
  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void twoDalCalls(boolean commitTrx) throws InterruptedException, ExecutionException {
    List<DocumentNumberGetter> threads = new ArrayList<DocumentNumberGetter>();
    threads.add(new DALDocumentNumberGetter().setThreadNo(1).commitTrx(commitTrx));
    threads.add(
        new DALDocumentNumberGetter().setThreadNo(2).waitBeforeStartMs(100).commitTrx(commitTrx));
    test(threads, commitTrx);
  }

  /** 2 concurrent PL calls */
  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void twoPLCalls(boolean commitTrx) throws InterruptedException, ExecutionException {
    List<DocumentNumberGetter> threads = new ArrayList<DocumentNumberGetter>();
    threads.add(new DBDocumentNumberGetter().setThreadNo(1).commitTrx(commitTrx));
    threads.add(new DBDocumentNumberGetter().setThreadNo(2).commitTrx(commitTrx));
    test(threads, commitTrx);
  }

  /** one dal, wait till it finishes, another dal call */
  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void twoDalCallsSequential(boolean commitTrx) throws InterruptedException, ExecutionException {
    List<DocumentNumberGetter> threads = new ArrayList<DocumentNumberGetter>();
    threads.add(new DALDocumentNumberGetter().setThreadNo(1).commitTrx(commitTrx));
    threads.add(new DALDocumentNumberGetter().setThreadNo(2)
        .waitBeforeStartMs(WAIT_MS + 100)
        .commitTrx(commitTrx));
    test(threads, commitTrx);
  }

  /** dal and pl concurrently, dal starts */
  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void dalFirstThenPL(boolean commitTrx) throws InterruptedException, ExecutionException {
    List<DocumentNumberGetter> threads = new ArrayList<DocumentNumberGetter>();
    threads.add(new DALDocumentNumberGetter().setThreadNo(1).commitTrx(commitTrx));
    threads.add(
        new DBDocumentNumberGetter().setThreadNo(2).waitBeforeStartMs(100).commitTrx(commitTrx));
    test(threads, commitTrx);
  }

  /** pl and dal concurrently, pl starts */
  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  public void plFirstThenDal(boolean commitTrx) throws InterruptedException, ExecutionException {
    List<DocumentNumberGetter> threads = new ArrayList<DocumentNumberGetter>();
    threads.add(new DALDocumentNumberGetter().setThreadNo(1).commitTrx(commitTrx));
    threads.add(
        new DBDocumentNumberGetter().setThreadNo(2).waitBeforeStartMs(100).commitTrx(commitTrx));
    test(threads, commitTrx);
  }

  /** executes all the threads and asserts the results */
  private void test(List<DocumentNumberGetter> threads, boolean commitTrx)
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

      OBDal.getInstance().getSession().close();
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
