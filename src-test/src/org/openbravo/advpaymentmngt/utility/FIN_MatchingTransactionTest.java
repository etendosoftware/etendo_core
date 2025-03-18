package org.openbravo.advpaymentmngt.utility;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.exception.NoAlgorithmFoundException;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;

/**
 * Test class for the FIN_MatchingTransaction functionality.
 */
@RunWith(MockitoJUnitRunner.class)
public class FIN_MatchingTransactionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private FIN_BankStatementLine mockBankStatementLine;

  @Mock
  private FIN_FinaccTransaction mockFinaccTransaction;

  private FIN_MatchingTransaction matchingTransaction;

  /**
   * Tests the constructor with a null algorithm.
   * Expects a NoAlgorithmFoundException to be thrown.
   */
  @Test
  public void testConstructorNullAlgorithm() {
    expectedException.expect(NoAlgorithmFoundException.class);
    expectedException.expectMessage("No algorithm has been defined to match bank statement lines");

    matchingTransaction = new FIN_MatchingTransaction(null);
  }

  /**
   * Tests the constructor with an empty algorithm.
   * Expects a NoAlgorithmFoundException to be thrown.
   */
  @Test
  public void testConstructorEmptyAlgorithm() {
    expectedException.expect(NoAlgorithmFoundException.class);
    expectedException.expectMessage("No algorithm has been defined to match bank statement lines");

    matchingTransaction = new FIN_MatchingTransaction("");
  }

  /**
   * Tests the constructor with an invalid algorithm.
   * Expects a NoAlgorithmFoundException to be thrown.
   */
  @Test
  public void testConstructorInvalidAlgorithm() {
    String invalidAlgorithm = "org.openbravo.invalid.Algorithm";
    expectedException.expect(NoAlgorithmFoundException.class);

    matchingTransaction = new FIN_MatchingTransaction(invalidAlgorithm);
  }

  /**
   * Tests the constructor with a valid algorithm.
   * Verifies that the algorithm is initialized correctly.
   */
  @Test
  public void testConstructorValidAlgorithm() {
    String validAlgorithm = "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm";

    matchingTransaction = new FIN_MatchingTransaction(validAlgorithm);

    assertNotNull("Algorithm should be initialized", matchingTransaction.algorithm);
    assertTrue("Algorithm should be of correct type", matchingTransaction.algorithm instanceof MockMatchingAlgorithm);
  }

  /**
   * Tests the match method with a valid algorithm.
   * Verifies that the match result is not null.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testMatchSuccess() throws Exception {
    matchingTransaction = new FIN_MatchingTransaction(
        "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm");

    List<FIN_FinaccTransaction> excludedTransactions = new ArrayList<>();

    FIN_MatchedTransaction result = matchingTransaction.match(mockBankStatementLine, excludedTransactions);

    assertNotNull("Match result should not be null", result);
  }

  /**
   * Tests the match method with a null algorithm.
   * Expects a NoAlgorithmFoundException to be thrown.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testMatchNullAlgorithm() throws Exception {
    expectedException.expect(NoAlgorithmFoundException.class);
    expectedException.expectMessage("No algorithm has been defined to match bank statement lines");

    matchingTransaction = new FIN_MatchingTransaction(
        "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm");
    Field algorithmField = FIN_MatchingTransaction.class.getDeclaredField("algorithm");
    algorithmField.setAccessible(true);
    algorithmField.set(matchingTransaction, null);

    List<FIN_FinaccTransaction> excludedTransactions = new ArrayList<>();

    matchingTransaction.match(mockBankStatementLine, excludedTransactions);
  }

  /**
   * Tests the unmatch method with a valid algorithm.
   * Verifies that no exception is thrown.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testUnmatchSuccess() throws Exception {
    matchingTransaction = new FIN_MatchingTransaction(
        "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm");

    matchingTransaction.unmatch(mockFinaccTransaction);
  }

  /**
   * Tests the unmatch method with a null algorithm.
   * Expects a NoAlgorithmFoundException to be thrown.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testUnmatchNullAlgorithm() throws Exception {
    expectedException.expect(NoAlgorithmFoundException.class);
    expectedException.expectMessage("No algorithm has been defined to unmatch");

    matchingTransaction = new FIN_MatchingTransaction(
        "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm");
    Field algorithmField = FIN_MatchingTransaction.class.getDeclaredField("algorithm");
    algorithmField.setAccessible(true);
    algorithmField.set(matchingTransaction, null);

    matchingTransaction.unmatch(mockFinaccTransaction);
  }

  /**
   * Tests the match method when the algorithm throws a ServletException.
   * Expects a ServletException to be thrown.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testMatchAlgorithmThrowsServletException() throws Exception {
    FIN_MatchingAlgorithm mockExceptionAlgorithm = mock(FIN_MatchingAlgorithm.class);
    when(mockExceptionAlgorithm.match(any(FIN_BankStatementLine.class), anyList())).thenThrow(
        new ServletException("Test exception"));

    matchingTransaction = new FIN_MatchingTransaction(
        "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm");
    Field algorithmField = FIN_MatchingTransaction.class.getDeclaredField("algorithm");
    algorithmField.setAccessible(true);
    algorithmField.set(matchingTransaction, mockExceptionAlgorithm);

    List<FIN_FinaccTransaction> excludedTransactions = new ArrayList<>();

    expectedException.expect(ServletException.class);
    expectedException.expectMessage("Test exception");

    matchingTransaction.match(mockBankStatementLine, excludedTransactions);
  }

  /**
   * Tests the unmatch method when the algorithm throws a ServletException.
   * Expects a ServletException to be thrown.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testUnmatchAlgorithmThrowsServletException() throws Exception {
    FIN_MatchingAlgorithm mockExceptionAlgorithm = mock(FIN_MatchingAlgorithm.class);
    doThrow(new ServletException("Test unmatch exception")).when(mockExceptionAlgorithm).unmatch(
        any(FIN_FinaccTransaction.class));

    matchingTransaction = new FIN_MatchingTransaction(
        "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm");
    Field algorithmField = FIN_MatchingTransaction.class.getDeclaredField("algorithm");
    algorithmField.setAccessible(true);
    algorithmField.set(matchingTransaction, mockExceptionAlgorithm);

    expectedException.expect(ServletException.class);
    expectedException.expectMessage("Test unmatch exception");

    matchingTransaction.unmatch(mockFinaccTransaction);
  }

  /**
   * Tests the constructor with an instantiation exception.
   * Expects a NoAlgorithmFoundException to be thrown.
   */
  @Test
  public void testConstructorInstantiationException() {
    expectedException.expect(NoAlgorithmFoundException.class);

    matchingTransaction = new FIN_MatchingTransaction("java.util.AbstractList");
  }

  /**
   * Mock implementation of the FIN_MatchingAlgorithm interface for testing purposes.
   */
  public static class MockMatchingAlgorithm implements FIN_MatchingAlgorithm {
    @Override
    public FIN_MatchedTransaction match(FIN_BankStatementLine bankStatementLine,
        List<FIN_FinaccTransaction> excluded) throws ServletException {
      return new FIN_MatchedTransaction(null, "0");
    }

    @Override
    public void unmatch(FIN_FinaccTransaction transaction) {
    }
  }
}
