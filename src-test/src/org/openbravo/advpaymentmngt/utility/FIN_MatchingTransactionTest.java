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

@RunWith(MockitoJUnitRunner.class)
public class FIN_MatchingTransactionTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private FIN_BankStatementLine mockBankStatementLine;

    @Mock
    private FIN_FinaccTransaction mockFinaccTransaction;

    private FIN_MatchingTransaction matchingTransaction;

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

    @Test
    public void testConstructorNullAlgorithm() {
        // Given
        expectedException.expect(NoAlgorithmFoundException.class);
        expectedException.expectMessage("No algorithm has been defined to match bank statement lines");

        // When/Then
        matchingTransaction = new FIN_MatchingTransaction(null);
    }

    @Test
    public void testConstructorEmptyAlgorithm() {
        // Given
        expectedException.expect(NoAlgorithmFoundException.class);
        expectedException.expectMessage("No algorithm has been defined to match bank statement lines");

        // When/Then
        matchingTransaction = new FIN_MatchingTransaction("");
    }

    @Test
    public void testConstructorInvalidAlgorithm() {
        // Given
        String invalidAlgorithm = "org.openbravo.invalid.Algorithm";
        expectedException.expect(NoAlgorithmFoundException.class);

        // When/Then
        matchingTransaction = new FIN_MatchingTransaction(invalidAlgorithm);
    }

    @Test
    public void testConstructorValidAlgorithm() {
        // Given
        String validAlgorithm = "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm";

        // When
        matchingTransaction = new FIN_MatchingTransaction(validAlgorithm);

        // Then
        assertNotNull("Algorithm should be initialized", matchingTransaction.algorithm);
        assertTrue("Algorithm should be of correct type",
            matchingTransaction.algorithm instanceof MockMatchingAlgorithm);
    }

    @Test
    public void testMatchSuccess() throws Exception {
        // Given
        matchingTransaction = new FIN_MatchingTransaction(
            "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm");

        List<FIN_FinaccTransaction> excludedTransactions = new ArrayList<>();

        // When
        FIN_MatchedTransaction result = matchingTransaction.match(mockBankStatementLine, excludedTransactions);

        // Then
        assertNotNull("Match result should not be null", result);
    }

    @Test
    public void testMatchNullAlgorithm() throws Exception {
        // Given
        expectedException.expect(NoAlgorithmFoundException.class);
        expectedException.expectMessage("No algorithm has been defined to match bank statement lines");

        matchingTransaction = new FIN_MatchingTransaction(
            "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm");
        Field algorithmField = FIN_MatchingTransaction.class.getDeclaredField("algorithm");
        algorithmField.setAccessible(true);
        algorithmField.set(matchingTransaction, null);

        List<FIN_FinaccTransaction> excludedTransactions = new ArrayList<>();

        // When/Then
        matchingTransaction.match(mockBankStatementLine, excludedTransactions);
    }

    @Test
    public void testUnmatchSuccess() throws Exception {
        // Given
        matchingTransaction = new FIN_MatchingTransaction(
            "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm");

        // When
        matchingTransaction.unmatch(mockFinaccTransaction);

        // Then - no exception means success
    }

    @Test
    public void testUnmatchNullAlgorithm() throws Exception {
        // Given
        expectedException.expect(NoAlgorithmFoundException.class);
        expectedException.expectMessage("No algorithm has been defined to unmatch");

        matchingTransaction = new FIN_MatchingTransaction(
            "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm");
        Field algorithmField = FIN_MatchingTransaction.class.getDeclaredField("algorithm");
        algorithmField.setAccessible(true);
        algorithmField.set(matchingTransaction, null);

        // When/Then
        matchingTransaction.unmatch(mockFinaccTransaction);
    }

    @Test
    public void testMatch_AlgorithmThrowsServletException() throws Exception {
        // Given
        FIN_MatchingAlgorithm mockExceptionAlgorithm = mock(FIN_MatchingAlgorithm.class);
        when(mockExceptionAlgorithm.match(any(FIN_BankStatementLine.class), anyList()))
            .thenThrow(new ServletException("Test exception"));

        matchingTransaction = new FIN_MatchingTransaction(
            "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm");
        Field algorithmField = FIN_MatchingTransaction.class.getDeclaredField("algorithm");
        algorithmField.setAccessible(true);
        algorithmField.set(matchingTransaction, mockExceptionAlgorithm);

        List<FIN_FinaccTransaction> excludedTransactions = new ArrayList<>();

        // Expect exception
        expectedException.expect(ServletException.class);
        expectedException.expectMessage("Test exception");

        // When/Then
        matchingTransaction.match(mockBankStatementLine, excludedTransactions);
    }

    @Test
    public void testUnmatch_AlgorithmThrowsServletException() throws Exception {
        // Given
        FIN_MatchingAlgorithm mockExceptionAlgorithm = mock(FIN_MatchingAlgorithm.class);
        doThrow(new ServletException("Test unmatch exception"))
            .when(mockExceptionAlgorithm).unmatch(any(FIN_FinaccTransaction.class));

        matchingTransaction = new FIN_MatchingTransaction(
            "org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest$MockMatchingAlgorithm");
        Field algorithmField = FIN_MatchingTransaction.class.getDeclaredField("algorithm");
        algorithmField.setAccessible(true);
        algorithmField.set(matchingTransaction, mockExceptionAlgorithm);

        // Expect exception
        expectedException.expect(ServletException.class);
        expectedException.expectMessage("Test unmatch exception");

        // When/Then
        matchingTransaction.unmatch(mockFinaccTransaction);
    }

    @Test
    public void testConstructor_InstantiationException() {

        expectedException.expect(NoAlgorithmFoundException.class);

        matchingTransaction = new FIN_MatchingTransaction("java.util.AbstractList");
    }
}