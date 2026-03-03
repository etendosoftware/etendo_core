package org.openbravo.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.etendoerp.sequences.DefaultSequenceGeneratorTest;
import com.etendoerp.sequences.dimensions.DimensionListOriginalColumnFormatTest;
import com.etendoerp.sequences.dimensions.DimensionListRequestParameterFormatTest;
import com.smf.jobs.ActionResultTest;
import com.smf.jobs.ActionTest;
import com.smf.jobs.FilterTest;
import com.smf.jobs.background.BackgroundRunnerTest;
import org.openbravo.advpaymentmngt.actionHandler.AddMultiplePaymentsProcessAfterProcessHookTest;
import org.openbravo.advpaymentmngt.actionHandler.AddPaymentActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.AddTransactionActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.FundsTransferActionHandlerTest;
import org.openbravo.advpaymentmngt.actionHandler.FundsTransferHookCallerTest;
import org.openbravo.advpaymentmngt.filterexpression.AddOrderOrInvoiceFilterExpressionHandlerTest;
import org.openbravo.advpaymentmngt.filterexpression.AddPaymentDefaultValuesHandlerTest;
import org.openbravo.advpaymentmngt.filterexpression.AddPaymentReadOnlyLogicsHandlerTest;
import org.openbravo.advpaymentmngt.process.FIN_DoubtfulDebtProcessTest;
import org.openbravo.advpaymentmngt.utility.APRMSQLFunctionRegisterTest;
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtilityTest;
import org.openbravo.authentication.AuthenticationExceptionTest;
import org.openbravo.authentication.AuthenticationExpirationPasswordExceptionTest;
import org.openbravo.authentication.basic.AutologonAuthenticationManagerTest;
import org.openbravo.base.gen.GenerateEntitiesTaskTest;
import org.openbravo.base.model.domaintype.AbsoluteDateTimeDomainTypeTest;
import org.openbravo.base.model.domaintype.AbsoluteTimeDomainTypeTest;
import org.openbravo.base.model.domaintype.BaseDomainTypeTest;
import org.openbravo.base.model.domaintype.BaseForeignKeyDomainTypeTest;
import org.openbravo.base.model.domaintype.BasePrimitiveDomainTypeTest;
import org.openbravo.base.secureApp.DefaultValidationExceptionTest;
import org.openbravo.base.secureApp.DefaultValuesDataTest;
import org.openbravo.base.validation.AccessLevelCheckerTest;
import org.openbravo.client.application.ADAlertDatasourceServiceTest;
import org.openbravo.client.application.AlertActionHandlerTest;
import org.openbravo.client.application.AlertManagementActionHandlerTest;
import org.openbravo.client.application.ApplicationComponentProviderTest;
import org.openbravo.client.application.ApplicationUtilsTest;
import org.openbravo.client.application.ComputeTranslatedNameActionHandlerTest;
import org.openbravo.client.application.ComputeWindowActionHandlerTest;
import org.openbravo.client.application.DeleteImageActionHandlerTest;
import org.openbravo.client.application.FixedValueExpressionCalloutTest;
import org.openbravo.client.application.attachment.AttachImplementationManagerTest;
import org.openbravo.client.application.attachment.AttachmentAHTest;
import org.openbravo.client.application.attachment.AttachmentWindowComponentTest;
import org.openbravo.client.application.attachment.CoreAttachImplementationTest;
import org.openbravo.client.application.businesslogic.DefaultsUploadDataActionHandlerTest;
import org.openbravo.client.application.event.AcctSchemaEventHandlerTest;
import org.openbravo.client.application.example.GridExampleActionHandlerTest;
import org.openbravo.client.application.navigationbarcomponents.ApplicationMenuComponentTest;
import org.openbravo.client.application.process.BaseProcessActionHandlerTest;
import org.openbravo.client.application.report.BaseReportActionHandlerTest;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructuresTest;
import org.openbravo.client.application.window.ComputeSelectedRecordActionHandlerTest;
import org.openbravo.client.application.window.GetNameGeneralLedgerTest;
import org.openbravo.client.application.window.GetTabMessageActionHandlerTest;
import org.openbravo.client.application.window.GridConfigurationSequenceNumberCalloutTest;
import org.openbravo.client.application.window.hooks.DataPoolSelectionWindowInjectorTest;
import org.openbravo.client.application.window.servlet.CalloutHttpServletResponseTest;
import org.openbravo.client.application.window.servlet.CalloutServletConfigTest;
import org.openbravo.client.kernel.BaseActionHandlerTest;
import org.openbravo.client.kernel.BaseComponentProviderTest;
import org.openbravo.client.kernel.BaseComponentTest;
import org.openbravo.client.kernel.BaseKernelServletTest;
import org.openbravo.client.kernel.BaseTemplateComponentTest;
import org.openbravo.client.kernel.BaseTemplateProcessorTest;
import org.openbravo.client.kernel.ComponentGeneratorTest;
import org.openbravo.client.kernel.GetLabelActionHandlerTest;
import org.openbravo.client.kernel.reference.AbsoluteDateTimeUIDefinitionTest;
import org.openbravo.client.kernel.reference.AbsoluteTimeUIDefinitionTest;
import org.openbravo.client.querylist.CheckOptionalFilterCalloutTest;
import org.openbravo.common.actionhandler.ChangeInventoryStatusActionHandlerTest;
import org.openbravo.common.actionhandler.CheckAvailableCreditActionHandlerTest;
import org.openbravo.common.actionhandler.copyfromorderprocess.CopyFromOrdersProcessFilterExpressionTest;
import org.openbravo.common.actionhandler.createlinesfromprocess.CreateInvoiceLinesFromInOutLinesTest;
import org.openbravo.common.actionhandler.createlinesfromprocess.CreateInvoiceLinesFromOrderLinesTest;
import org.openbravo.common.datasource.CostingTransactionsHQLTransformerTest;
import org.openbravo.common.filterexpression.AgingGeneralLedgerFilterExpressionTest;
import org.openbravo.common.hooks.ConvertQuotationIntoOrderHookManagerTest;
import org.openbravo.common.hooks.DataSourceFilterHookTest;
import org.openbravo.configuration.ConfigureOptionTest;
import org.openbravo.costing.AverageAlgorithmTest;
import org.openbravo.costing.AverageCostAdjustmentTest;
import org.openbravo.costing.CostingRuleProcessActionHandlerTest;
import org.openbravo.costing.CostingRuleProcessOnProcessHandlerTest;
import org.openbravo.dal.core.DalInitializingTaskTest;
import org.openbravo.dal.security.AcctSchemaStructureProviderTest;
import org.openbravo.dal.xml.BaseXMLEntityConverterTest;
import org.openbravo.dal.xml.EntityNotFoundExceptionTest;
import org.openbravo.dal.xml.EntityXMLExceptionTest;
import org.openbravo.email.EmailUtilsTest;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonUtilityTest;
import org.openbravo.erpCommon.ad_actionButton.CopyFromSettlementTest;
import org.openbravo.erpCommon.ad_actionButton.CreateStandardsTest;
import org.openbravo.erpCommon.ad_actionButton.CreateVatRegistersTest;
import org.openbravo.erpCommon.ad_actionButton.CreateWorkEffortTest;
import org.openbravo.erpCommon.ad_actionButton.EditCCPMeasureValuesTest;
import org.openbravo.erpCommon.ad_actionButton.ExportReferenceDataTest;
import org.openbravo.erpCommon.ad_callouts.AUM_ConversionRateTest;
import org.openbravo.erpCommon.ad_callouts.BackgroundProcessClusterConfigTest;
import org.openbravo.erpCommon.ad_callouts.BusinessPartnerDocTypeValidationTest;
import org.openbravo.erpCommon.ad_callouts.CalloutHelperTest;
import org.openbravo.erpCommon.ad_callouts.EmailConfiguration_PortTest;
import org.openbravo.erpCommon.ad_callouts.GtinFormatTest;
import org.openbravo.erpCommon.ad_forms.AboutTest;
import org.openbravo.erpCommon.ad_forms.AccountTest;
import org.openbravo.erpCommon.ad_forms.AcctSchemaElementTest;
import org.openbravo.erpCommon.ad_forms.AcctSchemaTest;
import org.openbravo.erpCommon.ad_forms.AcctServerTest;
import org.openbravo.erpCommon.ad_forms.DocAmortizationTest;
import org.openbravo.erpCommon.ad_forms.DocBankTest;
import org.openbravo.erpCommon.ad_forms.DocCashTest;
import org.openbravo.erpCommon.ad_forms.DocDPManagementTemplateTest;
import org.openbravo.erpCommon.ad_forms.DocDPManagementTest;
import org.openbravo.erpCommon.ad_forms.DocDoubtfulDebtTest;
import org.openbravo.erpCommon.ad_forms.DocFINBankStatementTemplateTest;
import org.openbravo.erpCommon.ad_forms.DocFINBankStatementTest;
import org.openbravo.erpCommon.ad_forms.DocFINFinAccTransactionTemplateTest;
import org.openbravo.erpCommon.ad_forms.DocFINPaymentTemplateTest;
import org.openbravo.erpCommon.ad_forms.DocFINReconciliationTemplateTest;
import org.openbravo.erpCommon.ad_forms.DocGLJournalTemplateTest;
import org.openbravo.erpCommon.ad_forms.DocInOutTemplateTest;
import org.openbravo.erpCommon.ad_forms.DocInternalConsumptionTemplateTest;
import org.openbravo.erpCommon.ad_forms.DocLine_AmortizationTest;
import org.openbravo.erpCommon.ad_forms.DocLine_BankTest;
import org.openbravo.erpCommon.ad_forms.DocLine_CashTest;
import org.openbravo.erpCommon.ad_forms.DocLine_DPManagementTest;
import org.openbravo.erpCommon.ad_forms.DocLine_PaymentTest;
import org.openbravo.erpCommon.ad_forms.DocMatchInvTemplateTest;
import org.openbravo.erpCommon.ad_forms.DocMovementTemplateTest;
import org.openbravo.erpCommon.ad_forms.DocOrderTemplateTest;
import org.openbravo.erpCommon.ad_forms.DocOrderTest;
import org.openbravo.erpCommon.ad_forms.DocPaymentTemplateTest;
import org.openbravo.erpCommon.ad_forms.DocPaymentTest;
import org.openbravo.erpCommon.ad_process.AcctServerProcessTest;
import org.openbravo.erpCommon.ad_process.AlertProcessTest;
import org.openbravo.erpCommon.ad_process.ApplyModulesTest;
import org.openbravo.erpCommon.ad_process.CalculatePromotionsTest;
import org.openbravo.erpCommon.ad_process.CashBankOperationsTest;
import org.openbravo.erpCommon.ad_process.CreateCashFlowStatementTest;
import org.openbravo.erpCommon.ad_process.CreateCustomModuleTest;
import org.openbravo.erpCommon.ad_process.assets.AssetLinearDepreciationMethodProcessTest;
import org.openbravo.erpCommon.ad_reports.AgingDaoTest;
import org.openbravo.erpCommon.ad_reports.AgingDataTest;
import org.openbravo.erpCommon.businessUtility.AccountTreeTest;
import org.openbravo.erpCommon.businessUtility.AuditTrailDeletedRecordsTest;
import org.openbravo.erpCommon.businessUtility.COADataTest;
import org.openbravo.erpCommon.businessUtility.COAUtilityTest;
import org.openbravo.erpCommon.businessUtility.CancelLayawayPaymentsHookCallerTest;
import org.openbravo.erpCommon.businessUtility.CancelOrderExecutorTest;
import org.openbravo.erpCommon.businessUtility.CloneOrderHookCallerTest;
import org.openbravo.erpCommon.info.AttributeSetInstanceTest;
import org.openbravo.erpCommon.info.BusinessPartnerSelectorFilterExpressionTest;
import org.openbravo.erpCommon.modules.ApplyModuleTaskTest;
import org.openbravo.erpCommon.modules.ApplyModuleTest;
import org.openbravo.erpCommon.modules.CheckLocalConsistencyTest;
import org.openbravo.erpCommon.modules.ExtractModuleTaskTest;
import org.openbravo.erpCommon.modules.ExtractModuleTest;
import org.openbravo.erpCommon.obps.ActivationTaskTest;
import org.openbravo.erpCommon.obps.ActiveInstanceProcessTest;
import org.openbravo.erpCommon.utility.AbstractScrollableFieldProviderFilterTest;
import org.openbravo.erpCommon.utility.AccDefUtilityTest;
import org.openbravo.erpCommon.utility.AlertTest;
import org.openbravo.erpCommon.utility.AttributeSetInstanceValueTest;
import org.openbravo.erpCommon.utility.BasicUtilityTest;
import org.openbravo.erpCommon.utility.CsrfUtilTest;
import org.openbravo.erpCommon.utility.ExecuteQueryTest;
import org.openbravo.erpCommon.utility.GenericTreeTest;
import org.openbravo.erpCommon.utility.poc.ClientAuthenticatorTest;
import org.openbravo.erpCommon.utility.poc.EmailAddressTypeTest;
import org.openbravo.erpCommon.utility.poc.EmailInfoTest;
import org.openbravo.erpCommon.utility.poc.EmailManagerTest;
import org.openbravo.erpCommon.utility.poc.EmailTypeTest;
import org.openbravo.erpCommon.utility.reporting.printing.AttachContentTest;
import org.openbravo.erpCommon.utility.reporting.printing.EmailUtilitiesTest;
import org.openbravo.event.ADTableEventHandlerTest;
import org.openbravo.event.ADTableNavigationEventHandlerTest;
import org.openbravo.materialmgmt.refinventory.DefaultBoxFilterProviderTest;
import org.openbravo.materialmgmt.refinventory.DefaultProcessorProviderTest;
import org.openbravo.portal.AccountCancelledEmailBodyTest;
import org.openbravo.portal.AccountCancelledEmailGeneratorTest;
import org.openbravo.role.inheritance.access.AccessTypeInjectorTest;
import org.openbravo.role.inheritance.access.AlertRecipientAccessInjectorTest;
import org.openbravo.service.datasource.ADTreeDatasourceServiceTest;
import org.openbravo.service.datasource.AccountTreeDatasourceServiceTest;
import org.openbravo.service.datasource.CheckTreeOperationManagerTest;
import org.openbravo.service.datasource.ComboTableDatasourceServiceTest;
import org.openbravo.service.datasource.treeChecks.AssetsTreeOperationManagerTest;
import org.openbravo.service.db.ClientImportEntityResolverTest;
import org.openbravo.service.json.AdvancedQueryBuilderTest;
import org.openbravo.userinterface.selector.DefaultExpressionCalloutTest;

/**
 * Test suite for auto-generated coverage tests.
 * <p>
 * This suite aggregates all test classes generated to increase code coverage.
 * New test classes should be added here as they are generated and validated.
 * </p>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    // com.etendoerp.sequences
    DefaultSequenceGeneratorTest.class,
    DimensionListOriginalColumnFormatTest.class,
    DimensionListRequestParameterFormatTest.class,

    // com.smf.jobs
    ActionResultTest.class,
    ActionTest.class,
    FilterTest.class,
    BackgroundRunnerTest.class,

    // advpaymentmngt.actionHandler
    AddMultiplePaymentsProcessAfterProcessHookTest.class,
    AddPaymentActionHandlerTest.class,
    AddTransactionActionHandlerTest.class,
    FundsTransferActionHandlerTest.class,
    FundsTransferHookCallerTest.class,

    // advpaymentmngt.filterexpression
    AddOrderOrInvoiceFilterExpressionHandlerTest.class,
    AddPaymentDefaultValuesHandlerTest.class,
    AddPaymentReadOnlyLogicsHandlerTest.class,

    // advpaymentmngt.process
    FIN_DoubtfulDebtProcessTest.class,

    // advpaymentmngt.utility
    APRMSQLFunctionRegisterTest.class,
    APRM_MatchingUtilityTest.class,

    // authentication
    AuthenticationExceptionTest.class,
    AuthenticationExpirationPasswordExceptionTest.class,
    AutologonAuthenticationManagerTest.class,

    // base
    GenerateEntitiesTaskTest.class,
    AbsoluteDateTimeDomainTypeTest.class,
    AbsoluteTimeDomainTypeTest.class,
    BaseDomainTypeTest.class,
    BaseForeignKeyDomainTypeTest.class,
    BasePrimitiveDomainTypeTest.class,
    DefaultValidationExceptionTest.class,
    DefaultValuesDataTest.class,
    AccessLevelCheckerTest.class,

    // client.application
    ADAlertDatasourceServiceTest.class,
    AlertActionHandlerTest.class,
    AlertManagementActionHandlerTest.class,
    ApplicationComponentProviderTest.class,
    ApplicationUtilsTest.class,
    ComputeTranslatedNameActionHandlerTest.class,
    ComputeWindowActionHandlerTest.class,
    DeleteImageActionHandlerTest.class,
    FixedValueExpressionCalloutTest.class,

    // client.application.attachment
    AttachImplementationManagerTest.class,
    AttachmentAHTest.class,
    AttachmentWindowComponentTest.class,
    CoreAttachImplementationTest.class,

    // client.application.businesslogic
    DefaultsUploadDataActionHandlerTest.class,

    // client.application.event
    AcctSchemaEventHandlerTest.class,

    // client.application.example
    GridExampleActionHandlerTest.class,

    // client.application.navigationbarcomponents
    ApplicationMenuComponentTest.class,

    // client.application.process
    BaseProcessActionHandlerTest.class,

    // client.application.report
    BaseReportActionHandlerTest.class,

    // client.application.window
    ApplicationDictionaryCachedStructuresTest.class,
    ComputeSelectedRecordActionHandlerTest.class,
    GetNameGeneralLedgerTest.class,
    GetTabMessageActionHandlerTest.class,
    GridConfigurationSequenceNumberCalloutTest.class,
    DataPoolSelectionWindowInjectorTest.class,
    CalloutHttpServletResponseTest.class,
    CalloutServletConfigTest.class,

    // client.kernel
    BaseActionHandlerTest.class,
    BaseComponentProviderTest.class,
    BaseComponentTest.class,
    BaseKernelServletTest.class,
    BaseTemplateComponentTest.class,
    BaseTemplateProcessorTest.class,
    ComponentGeneratorTest.class,
    GetLabelActionHandlerTest.class,
    AbsoluteDateTimeUIDefinitionTest.class,
    AbsoluteTimeUIDefinitionTest.class,

    // client.querylist
    CheckOptionalFilterCalloutTest.class,

    // common.actionhandler
    ChangeInventoryStatusActionHandlerTest.class,
    CheckAvailableCreditActionHandlerTest.class,
    CopyFromOrdersProcessFilterExpressionTest.class,
    CreateInvoiceLinesFromInOutLinesTest.class,
    CreateInvoiceLinesFromOrderLinesTest.class,

    // common.datasource
    CostingTransactionsHQLTransformerTest.class,

    // common.filterexpression
    AgingGeneralLedgerFilterExpressionTest.class,

    // common.hooks
    ConvertQuotationIntoOrderHookManagerTest.class,
    DataSourceFilterHookTest.class,

    // configuration
    ConfigureOptionTest.class,

    // costing
    AverageAlgorithmTest.class,
    AverageCostAdjustmentTest.class,
    CostingRuleProcessActionHandlerTest.class,
    CostingRuleProcessOnProcessHandlerTest.class,

    // dal
    DalInitializingTaskTest.class,
    AcctSchemaStructureProviderTest.class,
    BaseXMLEntityConverterTest.class,
    EntityNotFoundExceptionTest.class,
    EntityXMLExceptionTest.class,

    // email
    EmailUtilsTest.class,

    // erpCommon.ad_actionButton
    ActionButtonUtilityTest.class,
    CopyFromSettlementTest.class,
    CreateStandardsTest.class,
    CreateVatRegistersTest.class,
    CreateWorkEffortTest.class,
    EditCCPMeasureValuesTest.class,
    ExportReferenceDataTest.class,

    // erpCommon.ad_callouts
    AUM_ConversionRateTest.class,
    BackgroundProcessClusterConfigTest.class,
    BusinessPartnerDocTypeValidationTest.class,
    CalloutHelperTest.class,
    EmailConfiguration_PortTest.class,
    GtinFormatTest.class,

    // erpCommon.ad_forms
    AboutTest.class,
    AccountTest.class,
    AcctSchemaElementTest.class,
    AcctSchemaTest.class,
    AcctServerTest.class,
    DocAmortizationTest.class,
    DocBankTest.class,
    DocCashTest.class,
    DocDPManagementTemplateTest.class,
    DocDPManagementTest.class,
    DocDoubtfulDebtTest.class,
    DocFINBankStatementTemplateTest.class,
    DocFINBankStatementTest.class,
    DocFINFinAccTransactionTemplateTest.class,
    DocFINPaymentTemplateTest.class,
    DocFINReconciliationTemplateTest.class,
    DocGLJournalTemplateTest.class,
    DocInOutTemplateTest.class,
    DocInternalConsumptionTemplateTest.class,
    DocLine_AmortizationTest.class,
    DocLine_BankTest.class,
    DocLine_CashTest.class,
    DocLine_DPManagementTest.class,
    DocLine_PaymentTest.class,
    DocMatchInvTemplateTest.class,
    DocMovementTemplateTest.class,
    DocOrderTemplateTest.class,
    DocOrderTest.class,
    DocPaymentTemplateTest.class,
    DocPaymentTest.class,

    // erpCommon.ad_process
    AcctServerProcessTest.class,
    AlertProcessTest.class,
    ApplyModulesTest.class,
    CalculatePromotionsTest.class,
    CashBankOperationsTest.class,
    CreateCashFlowStatementTest.class,
    CreateCustomModuleTest.class,
    AssetLinearDepreciationMethodProcessTest.class,

    // erpCommon.ad_reports
    AgingDaoTest.class,
    AgingDataTest.class,

    // erpCommon.businessUtility
    AccountTreeTest.class,
    AuditTrailDeletedRecordsTest.class,
    COADataTest.class,
    COAUtilityTest.class,
    CancelLayawayPaymentsHookCallerTest.class,
    CancelOrderExecutorTest.class,
    CloneOrderHookCallerTest.class,

    // erpCommon.info
    AttributeSetInstanceTest.class,
    BusinessPartnerSelectorFilterExpressionTest.class,

    // erpCommon.modules
    ApplyModuleTaskTest.class,
    ApplyModuleTest.class,
    CheckLocalConsistencyTest.class,
    ExtractModuleTaskTest.class,
    ExtractModuleTest.class,

    // erpCommon.obps
    ActivationTaskTest.class,
    ActiveInstanceProcessTest.class,

    // erpCommon.utility
    AbstractScrollableFieldProviderFilterTest.class,
    AccDefUtilityTest.class,
    AlertTest.class,
    AttributeSetInstanceValueTest.class,
    BasicUtilityTest.class,
    CsrfUtilTest.class,
    ExecuteQueryTest.class,
    GenericTreeTest.class,
    ClientAuthenticatorTest.class,
    EmailAddressTypeTest.class,
    EmailInfoTest.class,
    EmailManagerTest.class,
    EmailTypeTest.class,
    AttachContentTest.class,
    EmailUtilitiesTest.class,

    // event
    ADTableEventHandlerTest.class,
    ADTableNavigationEventHandlerTest.class,

    // materialmgmt.refinventory
    DefaultBoxFilterProviderTest.class,
    DefaultProcessorProviderTest.class,

    // portal
    AccountCancelledEmailBodyTest.class,
    AccountCancelledEmailGeneratorTest.class,

    // role.inheritance.access
    AccessTypeInjectorTest.class,
    AlertRecipientAccessInjectorTest.class,

    // service.datasource
    ADTreeDatasourceServiceTest.class,
    AccountTreeDatasourceServiceTest.class,
    CheckTreeOperationManagerTest.class,
    ComboTableDatasourceServiceTest.class,
    AssetsTreeOperationManagerTest.class,

    // service.db
    ClientImportEntityResolverTest.class,

    // service.json
    AdvancedQueryBuilderTest.class,

    // userinterface.selector
    DefaultExpressionCalloutTest.class,
})
public class CoverageTestSuite {
}
