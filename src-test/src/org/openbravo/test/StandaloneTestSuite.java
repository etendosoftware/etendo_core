/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2009-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.openbravo.advpaymentmngt.suite.AdvPaymentMngtTestSuite;
import org.openbravo.authentication.hashing.PasswordHashing;
import org.openbravo.base.weld.test.testinfrastructure.CdiInfrastructure;
import org.openbravo.base.weld.test.testinfrastructure.DalPersistanceEventTest;
import org.openbravo.base.weld.test.testinfrastructure.ParameterizedCdi;
import org.openbravo.client.application.test.ADCSInitialization;
import org.openbravo.client.application.test.ADCSTest;
import org.openbravo.client.application.test.DisplayLogicAtServerLevelTest;
import org.openbravo.client.application.test.DynamicExpressionParserTest;
import org.openbravo.client.application.test.MenuTest;
import org.openbravo.client.application.test.db.model.triggers.ProcessTest;
import org.openbravo.client.application.test.event.DatasourceEventObserver;
import org.openbravo.client.application.test.event.PersistanceObserver;
import org.openbravo.client.kernel.CSSMinifier;
import org.openbravo.erpCommon.ad_callouts.LandedCostTypeTest;
import org.openbravo.erpCommon.info.ClassicSelectorTest;
import org.openbravo.erpReports.PrintControllerHookTest;
import org.openbravo.erpReports.ReportTaxPaymentJRTest;
import org.openbravo.erpReports.RptC_InvoiceTest;
import org.openbravo.erpReports.RptC_OrderTest;
import org.openbravo.erpReports.RptC_ProposalJrTest;
import org.openbravo.erpReports.RptC_RemittanceTest;
import org.openbravo.erpReports.RptMA_ProcessPlanTest;
import org.openbravo.erpReports.RptM_RequisitionTest;
import org.openbravo.event.LandedCostDuplicateReceiptValidatorTest;
import org.openbravo.financial.FinancialUtilsTest;
import org.openbravo.materialmgmt.MaterialMgmtTestSuite;
import org.openbravo.scheduling.GroupInfoTest;
import org.openbravo.scheduling.JobDetailProviderTest;
import org.openbravo.scheduling.OBSchedulerTest;
import org.openbravo.scheduling.ProcessGroupTest;
import org.openbravo.scheduling.ProcessMonitorTest;
import org.openbravo.scheduling.ProcessSchedulingTest;
import org.openbravo.scheduling.SchedulerTimeUtilsTest;
import org.openbravo.scheduling.quartz.JobInitializationListenerTest;
import org.openbravo.scheduling.quartz.OpenbravoDailyTimeIntervalTriggerPersistenceDelegateTest;
import org.openbravo.scheduling.quartz.OpenbravoOracleJDBCDelegateTest;
import org.openbravo.scheduling.quartz.OpenbravoPostgreJDBCDelegateTest;
import org.openbravo.scheduling.trigger.MisfirePolicyTest;
import org.openbravo.scheduling.trigger.TriggerProviderTest;
import org.openbravo.test.accounting.PostDocumentTest;
import org.openbravo.test.accounting.PostedNoDocConfigTest;
import org.openbravo.test.accounting.RecordID2Test;
import org.openbravo.test.authentication.AuthenticationManagerTest;
import org.openbravo.test.cancelandreplace.CancelAndReplaceTest;
import org.openbravo.test.centralbroker.CentralBrokerTest;
import org.openbravo.test.conversionratedoc.ConversionRateDocUniqueTestSuite;
import org.openbravo.test.copyLinesFromOrders.CopyLinesFromOrdersTest;
import org.openbravo.test.costing.TestCosting;
import org.openbravo.test.createInvoiceFromOrder.CreateOrderFromQuotationTest;
import org.openbravo.test.createlinesfrom.CreateLinesFromTest;
import org.openbravo.test.dal.AdminContextTest;
import org.openbravo.test.dal.ComputedColumnsTest;
import org.openbravo.test.dal.DalComplexQueryRequisitionTest;
import org.openbravo.test.dal.DalComplexQueryTestOrderLine;
import org.openbravo.test.dal.DalConnectionProviderTest;
import org.openbravo.test.dal.DalLockingTest;
import org.openbravo.test.dal.DalPerformanceInventoryLineTest;
import org.openbravo.test.dal.DalPerformanceProductTest;
import org.openbravo.test.dal.DalPerformanceProxyTest;
import org.openbravo.test.dal.DalQueryTest;
import org.openbravo.test.dal.DalStoredProcedureTest;
import org.openbravo.test.dal.DalTest;
import org.openbravo.test.dal.DalUtilTest;
import org.openbravo.test.dal.DynamicEntityTest;
import org.openbravo.test.dal.HiddenUpdateTest;
import org.openbravo.test.dal.ImageTest;
import org.openbravo.test.dal.IssuesTest;
import org.openbravo.test.dal.MappingGenerationTest;
import org.openbravo.test.dal.OBContextTest;
import org.openbravo.test.dal.ReadByNameTest;
import org.openbravo.test.dal.ValidationTest;
import org.openbravo.test.dal.ViewTest;
import org.openbravo.test.datasource.GridExport;
import org.openbravo.test.db.model.functions.ADOrgTreeTest;
import org.openbravo.test.db.model.functions.Ad_isorgincludedTest;
import org.openbravo.test.db.model.functions.SqlCallableStatement;
import org.openbravo.test.db.pool.PoolHasNoConnectionsDetection;
import org.openbravo.test.expression.EvaluationTest;
import org.openbravo.test.expression.OBBindingsTest;
import org.openbravo.test.generalsetup.enterprise.organization.ADOrgPersistInfoTestSuite;
import org.openbravo.test.inventoryStatus.InventoryStatusTest;
import org.openbravo.test.invoice.CloneInvoiceTest;
import org.openbravo.test.materialMgmt.invoiceFromShipment.InvoiceFromShipmentTest;
import org.openbravo.test.materialMgmt.iscompletelyinvoicedshipment.IsCompletelyInvoicedShipment;
import org.openbravo.test.materialMgmt.linevalidation.GoodMovementTest;
import org.openbravo.test.materialMgmt.linevalidation.InventoryCountProcessTest;
import org.openbravo.test.model.ClassLoaderTest;
import org.openbravo.test.model.DBModifiedTest;
import org.openbravo.test.model.OneToManyTest;
import org.openbravo.test.model.RuntimeModelTest;
import org.openbravo.test.model.TrlColumnsOraTypeTest;
import org.openbravo.test.model.UniqueConstraintTest;
import org.openbravo.test.modularity.DBPrefixTest;
import org.openbravo.test.modularity.DatasetServiceTest;
import org.openbravo.test.modularity.ExecutionLimitsTest;
import org.openbravo.test.modularity.MergePropertiesTest;
import org.openbravo.test.modularity.TableNameTest;
import org.openbravo.test.preference.PreferenceTest;
import org.openbravo.test.pricelist.PriceListTest;
import org.openbravo.test.process.order.OrderProcessTest;
import org.openbravo.test.process.utils.ProcessUtilsTest;
import org.openbravo.test.productStatus.ProductStatusTest;
import org.openbravo.test.purchaseOrder.PurchaseOrderStatus;
import org.openbravo.test.referencedinventory.ReferencedInventoryTestSuite;
import org.openbravo.test.reporting.AllJrxmlCompilation;
import org.openbravo.test.reporting.CompiledReportsCacheTest;
import org.openbravo.test.reporting.ReportingUtilsTest;
import org.openbravo.test.role.RoleTestSuite;
import org.openbravo.test.role.inheritance.RoleInheritanceTestSuite;
import org.openbravo.test.security.AccessLevelTest;
import org.openbravo.test.security.AllowedOrganizationsTest;
import org.openbravo.test.security.BypassAccessLevelCheck;
import org.openbravo.test.security.CrossOrganizationUI;
import org.openbravo.test.security.CrossOrganizationUICDI;
import org.openbravo.test.security.EntityAccessTest;
import org.openbravo.test.security.OBContextCollectionsTest;
import org.openbravo.test.security.PasswordStrengthCheckerTest;
import org.openbravo.test.security.StandardCrossOrganizationReference;
import org.openbravo.test.security.WritableReadableOrganizationClientTest;
import org.openbravo.test.services.ServicesTest;
import org.openbravo.test.services.ServicesTest2;
import org.openbravo.test.services.ServicesTest3;
import org.openbravo.test.stockReservation.StockReservationTest;
import org.openbravo.test.stockValuationReport.ReportValuationStockTest;
import org.openbravo.test.system.CryptoUtilities;
import org.openbravo.test.system.ErrorTextParserIntegrationTest;
import org.openbravo.test.system.ErrorTextParserTest;
import org.openbravo.test.system.ImportEntryBuilderTest;
import org.openbravo.test.system.ImportEntrySizeTest;
import org.openbravo.test.system.Issue29934Test;
import org.openbravo.test.system.JSONSerialization;
import org.openbravo.test.system.OBPropertiesProviderTest;
import org.openbravo.test.system.Sessions;
import org.openbravo.test.system.SystemServiceTest;
import org.openbravo.test.system.SystemValidatorTest;
import org.openbravo.test.system.TestInfrastructure;
import org.openbravo.test.taxes.ModifyTaxesTest;
import org.openbravo.test.taxes.TaxesTest;
import org.openbravo.test.views.ConfigurableTransactionalFilters;
import org.openbravo.test.views.GCSequenceNumberTests;
import org.openbravo.test.views.SortingFilteringGridConfiguration;
import org.openbravo.test.views.ViewGenerationWithDifferentConfigLevelTest;
import org.openbravo.test.xml.DatasetExportTest;
import org.openbravo.test.xml.DefaultsDataset;
import org.openbravo.test.xml.EntityXMLImportTestBusinessObject;
import org.openbravo.test.xml.EntityXMLImportTestReference;
import org.openbravo.test.xml.EntityXMLImportTestSingle;
import org.openbravo.test.xml.EntityXMLImportTestWarning;
import org.openbravo.test.xml.EntityXMLIssues;
import org.openbravo.test.xml.UniqueConstraintImportTest;
import org.openbravo.userinterface.selectors.test.ExpressionsTest;

/**
 * This test class is called from the ant task run.all.tests by the CI server. It contains all the
 * test cases which are runnable and valid and do not require Tomcat to be running.
 *
 * Test cases requiring Tomcat (ie. testing web service requests) should be inclued in
 * {@link WebserviceTestSuite}.
 *
 * @see WebserviceTestSuite
 *
 * @author mtaal
 */

@Suite
@SelectClasses({

    // dal
    IssuesTest.class, //

    // security
    AllowedOrganizationsTest.class, //

    // Price List
    PriceListTest.class, //

    // CopyFromOrders refactor
    CopyLinesFromOrdersTest.class,

    // Create Lines From refactor
    CreateLinesFromTest.class,

    // AD_IsOrgIncluded
    Ad_isorgincludedTest.class, //
    ADOrgTreeTest.class, //

    // C_Order_Post
    OrderProcessTest.class,

    // grid configuration
    ConfigurableTransactionalFilters.class,

    // costing
    TestCosting.class, //

    // Taxes
    TaxesTest.class, //
    ModifyTaxesTest.class, //

    // Referenced Inventory
    ReferencedInventoryTestSuite.class,

    // AD_Org Persist Information
    ADOrgPersistInfoTestSuite.class,

    // authentication
    AuthenticationManagerTest.class, //

    // dal
    DalComplexQueryRequisitionTest.class, //
    DalComplexQueryTestOrderLine.class, //
    DalPerformanceInventoryLineTest.class, //
    DalPerformanceProductTest.class, //
    DalPerformanceProxyTest.class, //
    DalQueryTest.class, //
    DalTest.class, //
    DalLockingTest.class, //
    CentralBrokerTest.class, //
    DalUtilTest.class, //
    DalConnectionProviderTest.class, //
    DynamicEntityTest.class, //
    HiddenUpdateTest.class, //
    MappingGenerationTest.class, //
    ValidationTest.class, //
    OBContextTest.class, //
    DalStoredProcedureTest.class, //
    ReadByNameTest.class, //
    AdminContextTest.class, //
    ViewTest.class, //
    ComputedColumnsTest.class, //
    DatasourceEventObserver.class, //
    PersistanceObserver.class, //
    ImageTest.class, //

    // expression
    EvaluationTest.class, //
    OBBindingsTest.class, //
    ExpressionsTest.class,

    // model
    RuntimeModelTest.class, //
    OneToManyTest.class, //
    UniqueConstraintTest.class, //
    ClassLoaderTest.class, //
    TrlColumnsOraTypeTest.class, //
    ADCSInitialization.class, //
    ADCSTest.class, //
    DBModifiedTest.class,

    // modularity
    DatasetServiceTest.class, //
    DBPrefixTest.class, //
    MergePropertiesTest.class, //
    TableNameTest.class,

    // security
    AccessLevelTest.class, //
    EntityAccessTest.class, //
    WritableReadableOrganizationClientTest.class, //
    StandardCrossOrganizationReference.class, //
    BypassAccessLevelCheck.class, //
    CrossOrganizationUI.class, //
    CrossOrganizationUICDI.class, //
    OBContextCollectionsTest.class, //
    PasswordStrengthCheckerTest.class, //

    // system
    SystemServiceTest.class, //
    SystemValidatorTest.class, //
    ErrorTextParserTest.class, //
    ErrorTextParserIntegrationTest.class, //
    TestInfrastructure.class, //
    Issue29934Test.class, //
    ImportEntrySizeTest.class, //
    ImportEntryBuilderTest.class, //
    CryptoUtilities.class, //
    Sessions.class, //
    JSONSerialization.class, //
    PasswordHashing.class, //
    OBPropertiesProviderTest.class, //

    // xml
    EntityXMLImportTestBusinessObject.class, //
    EntityXMLImportTestReference.class, //
    EntityXMLImportTestSingle.class, //
    EntityXMLImportTestWarning.class, //
    EntityXMLIssues.class, //
    UniqueConstraintImportTest.class, //
    DatasetExportTest.class, //
    DefaultsDataset.class, //

    // preferences
    PreferenceTest.class, //
    ClassicSelectorTest.class,

    // Accounting
    RecordID2Test.class, //
    PostDocumentTest.class, //


    // Inventory Status
    InventoryStatusTest.class, //

    // PLM Status
    ProductStatusTest.class, //

    // Material Management
    IsCompletelyInvoicedShipment.class, //

    // scheduling
    ProcessSchedulingTest.class, //
    TriggerProviderTest.class, //
    MisfirePolicyTest.class, //

    // cdi
    CdiInfrastructure.class, //
    ParameterizedCdi.class, //
    DalPersistanceEventTest.class, //

    // client application
//    ApplicationTest.class, //
    DynamicExpressionParserTest.class, //
    MenuTest.class, //
    DisplayLogicAtServerLevelTest.class, //
    CSSMinifier.class, //

    // buildValidations and moduleScripts
    ExecutionLimitsTest.class, //

    // role
    RoleTestSuite.class, //

    // role inheritance
    RoleInheritanceTestSuite.class, //

    // db
    SqlCallableStatement.class, //
    PoolHasNoConnectionsDetection.class, //
    ProcessTest.class,

    // grid configuration
    ViewGenerationWithDifferentConfigLevelTest.class, //
    GCSequenceNumberTests.class, //
    SortingFilteringGridConfiguration.class, //

    // jasper
    AllJrxmlCompilation.class, //
    CompiledReportsCacheTest.class,
    ReportingUtilsTest.class, //

    // Product Services
    ServicesTest.class, //
    ServicesTest2.class, //
    ServicesTest3.class,

    // others
    GridExport.class, //
    FinancialUtilsTest.class, //


    // Cancel and Replace Tests
    CancelAndReplaceTest.class, //

    // Automatic Invoice from Goods Shipment
    InvoiceFromShipmentTest.class,

    // Clone Invoice
    CloneInvoiceTest.class,

    // Conversion Rate Document Unique constraint refactor
    ConversionRateDocUniqueTestSuite.class,

    // Count and Discount a Document without Doc base type configured and get an error. [EPL-534]
    PostedNoDocConfigTest.class,

    // Report Valuation Stock Test
    ReportValuationStockTest.class,
      
    // Create Order From Quotation Test
    CreateOrderFromQuotationTest.class,

    // Create Order and Physical Inventory From Stock Reservation Test
    StockReservationTest.class,

    // Utils process Test
    ProcessUtilsTest.class,

    // Toolbar print hook Test
    PrintControllerHookTest.class,

    // Landed Cost Type Test
    LandedCostTypeTest.class,

    // Landed Cost Receipt Test
    LandedCostDuplicateReceiptValidatorTest.class,

    // Physical Inventory Test
    InventoryCountProcessTest.class,

    // Good Movement Test
    GoodMovementTest.class,

    // Purchase Order Status
    PurchaseOrderStatus.class,

    // MaterialMgmt
    MaterialMgmtTestSuite.class,

    // AdvPayment
    AdvPaymentMngtTestSuite.class, //

    // scheduling
    OpenbravoPostgreJDBCDelegateTest.class, //
    OpenbravoOracleJDBCDelegateTest.class, //
    OpenbravoDailyTimeIntervalTriggerPersistenceDelegateTest.class, //
    JobInitializationListenerTest.class, //
    ProcessGroupTest.class, //
    SchedulerTimeUtilsTest.class, //
    GroupInfoTest.class, //
    JobDetailProviderTest.class, //
    ProcessMonitorTest.class, //
    OBSchedulerTest.class, //

    // ReportTaxPaymentJR
    ReportTaxPaymentJRTest.class,

    // RptC_Invoice
    RptC_InvoiceTest.class,

    // RptC_Order Test
    RptC_OrderTest.class,

    // RptC_ProposalJr
    RptC_ProposalJrTest.class,

    // RptC_Remittance
    RptC_RemittanceTest.class,

    // RptM_Requisition
    RptM_RequisitionTest.class,

    // RptMA_ProcessPlan
    RptMA_ProcessPlanTest.class,
})
@SelectPackages({"org.openbravo.service.centralrepository",
    "org.openbravo.service.db",
    "org.openbravo.erpCommon.ad_process",
    "org.openbravo.common.actionhandler",
    "org.openbravo.common.filterexpression",
    "org.openbravo.common.datasource",
    "org.openbravo.portal",
    "org.openbravo.costing",
    "org.openbravo.cluster",
    "org.openbravo.role.inheritance",
    "org.openbravo.base.secureApp",

})
public class StandaloneTestSuite {
}
