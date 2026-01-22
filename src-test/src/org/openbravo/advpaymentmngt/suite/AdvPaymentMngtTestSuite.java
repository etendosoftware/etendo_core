package org.openbravo.advpaymentmngt.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.openbravo.advpaymentmngt.hqlinjections.AccountingFactEndYearTransformerTest;
import org.openbravo.advpaymentmngt.hqlinjections.AddPaymentGLItemInjectorTest;
import org.openbravo.advpaymentmngt.hqlinjections.AddPaymentOrderInvoicesTransformerTest;

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
    // tests hqlinjections
    AccountingFactEndYearTransformerTest.class,
    AddPaymentGLItemInjectorTest.class,
    AddPaymentOrderInvoicesTransformerTest.class,
})
public class AdvPaymentMngtTestSuite {
}
