package org.openbravo.advpaymentmngt.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.openbravo.advpaymentmngt.APRMActionHandlerTest;
import org.openbravo.advpaymentmngt.APRMApplicationProviderTest;
import org.openbravo.advpaymentmngt.actionHandler.AddMultiplePaymentsHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.AddPaymentDisplayLogicActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.AddPaymentDocumentNoActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.AddPaymentOnProcessActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.AddPaymentOrganizationActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.AddPaymentReloadLabelsActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.AddTransactionOnChangePaymentActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.CheckExistsOverissueBinForRFCShipmentWHTest;
import org.openbravo.advpaymentmngt.actionHandler.CheckRecordChangedActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.FindTransactionsToMatchActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.FundsTransferOnChangeDepositToActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.MatchStatementOnLoadGetPreferenceActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.MatchStatementOnLoadPreferenceActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.PaymentProposalPickEditLinesTest;
import org.openbravo.advpaymentmngt.actionHandler.ReceivedFromPaymentMethodActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.SalesOrderAddPaymentDefaultValuesTest;
import org.openbravo.advpaymentmngt.actionHandler.UnMatchSelectedTransactionsActionHandlerTest;
import org.openbravo.advpaymentmngt.ad_actionbutton.GLItemSelectorFilterExpressionTest;
import org.openbravo.advpaymentmngt.executionprocess.LeaveAsCreditTest;
import org.openbravo.advpaymentmngt.executionprocess.PrintCheckTest;
import org.openbravo.advpaymentmngt.filterexpression.AddOrderOrInvoiceFilterExpressionTest;
import org.openbravo.advpaymentmngt.filterexpression.AddPaymentDefaultValuesExpressionTest;
import org.openbravo.advpaymentmngt.filterexpression.AddTransactionFilterExpressionTest;
import org.openbravo.advpaymentmngt.filterexpression.BusinessPartnerCustomerFilterExpressionTest;
import org.openbravo.advpaymentmngt.filterexpression.BusinessPartnerVendorFilterExpressionTest;
import org.openbravo.advpaymentmngt.filterexpression.FundsTransferGLItemDefaultValueExpressionTest;
import org.openbravo.advpaymentmngt.filterexpression.MatchStatementFilterExpressionHandlerTest;
import org.openbravo.advpaymentmngt.filterexpression.MatchStatementFilterExpressionTest;
import org.openbravo.advpaymentmngt.filterexpression.PaymentInAddPaymentDefaultValuesTest;
import org.openbravo.advpaymentmngt.filterexpression.PaymentInAddPaymentDisplayLogicsTest;
import org.openbravo.advpaymentmngt.filterexpression.PaymentOutAddPaymentDefaultValuesTest;
import org.openbravo.advpaymentmngt.filterexpression.PaymentOutAddPaymentReadOnlyLogicsTest;
import org.openbravo.advpaymentmngt.filterexpression.PurchaseInvoiceAddPaymentDisplayLogicsTest;
import org.openbravo.advpaymentmngt.filterexpression.PurchaseInvoiceAddPaymentReadOnlyLogicsTest;
import org.openbravo.advpaymentmngt.filterexpression.PurchaseOrderAddPaymentDefaultValuesTest;
import org.openbravo.advpaymentmngt.filterexpression.PurchaseOrderAddPaymentDisplayLogicsTest;
import org.openbravo.advpaymentmngt.filterexpression.PurchaseOrderAddPaymentReadOnlyLogicsTest;
import org.openbravo.advpaymentmngt.filterexpression.SalesInvoiceAddPaymentDefaultValuesTest;
import org.openbravo.advpaymentmngt.filterexpression.SalesInvoiceAddPaymentReadOnlyLogicsTest;
import org.openbravo.advpaymentmngt.filterexpression.SalesOrderAddPaymentDisplayLogicsTest;
import org.openbravo.advpaymentmngt.filterexpression.SalesOrderAddPaymentReadOnlyLogicsTest;
import org.openbravo.advpaymentmngt.filterexpression.TransactionAddPaymentDefaultValuesTest;
import org.openbravo.advpaymentmngt.filterexpression.TransactionAddPaymentDisplayLogicsTest;
import org.openbravo.advpaymentmngt.filterexpression.TransactionAddPaymentReadOnlyLogicsTest;
import org.openbravo.advpaymentmngt.hook.PaymentProcessOrderHookTest;
import org.openbravo.advpaymentmngt.hqlinjections.AddPaymentCreditToUseInjectorTest;
import org.openbravo.advpaymentmngt.hqlinjections.CreditToUseTransformerTest;
import org.openbravo.advpaymentmngt.hqlinjections.MatchStatementTransformerTest;
import org.openbravo.advpaymentmngt.hqlinjections.TransactionsToMatchTransformerTest;
import org.openbravo.advpaymentmngt.test.DocumentNumberGeneration;
import org.openbravo.advpaymentmngt.test.FinancialAccountTest;
import org.openbravo.advpaymentmngt.test.PaymentMethodTest;
import org.openbravo.advpaymentmngt.test.PaymentTest_01;
import org.openbravo.advpaymentmngt.test.PaymentTest_02;
import org.openbravo.advpaymentmngt.test.PaymentTest_03;
import org.openbravo.advpaymentmngt.test.PaymentTest_04;
import org.openbravo.advpaymentmngt.test.PaymentTest_05;
import org.openbravo.advpaymentmngt.test.PaymentTest_06;
import org.openbravo.advpaymentmngt.test.PaymentTest_08;
import org.openbravo.advpaymentmngt.test.PaymentTest_11;
import org.openbravo.advpaymentmngt.test.ReversePaymentTest;
import org.openbravo.advpaymentmngt.utility.FIN_MatchingTransactionTest;
import org.openbravo.advpaymentmngt.utility.ValueTest;

/**
 * Test suite for the Advanced Payment Management module.
 * <p>
 * This suite aggregates all test classes related to the Advanced Payment Management
 * functionalities, ensuring comprehensive testing of the module's components.
 * </p>
 * <p>
 * The suite includes tests for action handlers, application providers, and HQL injections.
 * </p>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({

    CheckExistsOverissueBinForRFCShipmentWHTest.class,
    MatchStatementOnLoadGetPreferenceActionHandlerTest.class,
    ReceivedFromPaymentMethodActionHandlerTest.class,
    UnMatchSelectedTransactionsActionHandlerTest.class,
    AddPaymentReloadLabelsActionHandlerTest.class,
    AddPaymentOnProcessActionHandlerTest.class,
    AddPaymentDisplayLogicActionHandlerTest.class,
    AddTransactionOnChangePaymentActionHandlerTest.class,
    AddPaymentDocumentNoActionHandlerTest.class,
    MatchStatementOnLoadPreferenceActionHandlerTest.class,
    FindTransactionsToMatchActionHandlerTest.class,
    FundsTransferOnChangeDepositToActionHandlerTest.class,
    SalesOrderAddPaymentDefaultValuesTest.class,
    PaymentProposalPickEditLinesTest.class,
    CheckRecordChangedActionHandlerTest.class,
    AddPaymentOrganizationActionHandlerTest.class,
    AddMultiplePaymentsHandlerTest.class,

    //tests hqlinjections
    AddPaymentCreditToUseInjectorTest.class,
    CreditToUseTransformerTest.class,
    MatchStatementTransformerTest.class,
    //TransactionsToMatchTransformerTest.class,
    //executionprocess
    PrintCheckTest.class,
    LeaveAsCreditTest.class,
    //tests filterexpression
    MatchStatementFilterExpressionHandlerTest.class,
    AddTransactionFilterExpressionTest.class,
    AddOrderOrInvoiceFilterExpressionTest.class,
    BusinessPartnerCustomerFilterExpressionTest.class,
    BusinessPartnerVendorFilterExpressionTest.class,
    MatchStatementFilterExpressionTest.class,
    FundsTransferGLItemDefaultValueExpressionTest.class,
    PaymentInAddPaymentDefaultValuesTest.class,
    PaymentOutAddPaymentDefaultValuesTest.class,
    PaymentInAddPaymentDisplayLogicsTest.class,
    TransactionAddPaymentReadOnlyLogicsTest.class,
    TransactionAddPaymentDisplayLogicsTest.class,
    TransactionAddPaymentDefaultValuesTest.class,
    SalesOrderAddPaymentReadOnlyLogicsTest.class,
    SalesOrderAddPaymentDisplayLogicsTest.class,
    AddPaymentDefaultValuesExpressionTest.class,
    SalesInvoiceAddPaymentReadOnlyLogicsTest.class,
    SalesInvoiceAddPaymentDefaultValuesTest.class,
    PurchaseOrderAddPaymentReadOnlyLogicsTest.class,
    PurchaseOrderAddPaymentDisplayLogicsTest.class,
    PurchaseOrderAddPaymentDefaultValuesTest.class,
    PurchaseInvoiceAddPaymentReadOnlyLogicsTest.class,
    PurchaseInvoiceAddPaymentDisplayLogicsTest.class,
    //PaymentOutAddPaymentReadOnlyLogicsTest.class,

    //hooks
    //PaymentProcessOrderHookTest.class,

    //ad_actionbutton
    GLItemSelectorFilterExpressionTest.class,
    //utility
    FIN_MatchingTransactionTest.class,
    ValueTest.class,
    //other tests
    PaymentTest_01.class,
    PaymentTest_02.class,
    PaymentTest_03.class,
    PaymentTest_04.class,
    PaymentTest_08.class,
    PaymentTest_05.class,
    PaymentTest_06.class,
    PaymentTest_11.class,
    ReversePaymentTest.class,
    DocumentNumberGeneration.class,
    FinancialAccountTest.class,
    PaymentMethodTest.class,
    APRMActionHandlerTest.class,
    APRMApplicationProviderTest.class,



})
public class AdvPaymentMngtTestSuite {
}
