package org.openbravo.test.costing;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaTable;
import org.openbravo.model.financialmgmt.gl.GLCategory;
import org.openbravo.model.materialmgmt.cost.CostingAlgorithm;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.test.costing.utils.TestCostingConstants;
import org.openbravo.test.costing.utils.TestCostingUtils;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestCostingBase extends WeldBaseTest {

  @BeforeEach
  public void setInitialConfiguration() {
    // FIXME: Change setInitialConfiguration to @BeforeAll and remove runBefore flag
    // once https://issues.openbravo.com/view.php?id=36326 is fixed
    if (TestCostingConstants.runBefore) {
      try {

        // Set System context
        OBContext.setOBContext(TestCostingConstants.USERADMIN_ID);
        OBContext.setAdminMode(true);

        // Set EUR currency costing precision
        Currency currrencyEur = OBDal.getInstance().get(Currency.class, EURO_ID);
        currrencyEur.setCostingPrecision(4L);
        OBDal.getInstance().save(currrencyEur);

        // Set USD currency costing precision
        Currency currrencyUsd = OBDal.getInstance().get(Currency.class, DOLLAR_ID);
        currrencyUsd.setCostingPrecision(4L);
        OBDal.getInstance().save(currrencyUsd);

        OBDal.getInstance().flush();

        // Set QA context
        OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
            TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
            TestCostingConstants.SPAIN_ORGANIZATION_ID);

        // Set Spain organization currency
        Organization organization = OBDal.getInstance()
            .get(Organization.class, TestCostingConstants.SPAIN_ORGANIZATION_ID);
        organization.setCurrency(OBDal.getInstance().get(Currency.class, EURO_ID));
        OBDal.getInstance().save(organization);

        // Create Internal Consumption Document Type if it does not exist
        OBCriteria<DocumentType> internalConsCrit = OBDal.getInstance().createCriteria(DocumentType.class);
        internalConsCrit.addEqual(DocumentType.PROPERTY_DOCUMENTCATEGORY,
            TestCostingConstants.MAT_INT_CONSUMPTION_DOC_CAT);
        internalConsCrit.addEqual(DocumentType.PROPERTY_TABLE, OBDal.getInstance().get(Table.class,
            TestCostingConstants.INTERNAL_CONSUMPTION_TABLE_ID));
        internalConsCrit.setMaxResults(1);

        if (internalConsCrit.uniqueResult() == null) {
          DocumentType internalConsumptionDocType = OBProvider.getInstance().get(DocumentType.class);
          internalConsumptionDocType.setName(TestCostingConstants.INTERNAL_CONSUMPTION);
          internalConsumptionDocType.setPrintText(TestCostingConstants.INTERNAL_CONSUMPTION);
          internalConsumptionDocType.setDocumentCategory(TestCostingConstants.MAT_INT_CONSUMPTION_DOC_CAT);
          internalConsumptionDocType.setGLCategory(
              OBDal.getInstance().get(GLCategory.class, TestCostingConstants.GL_CAT_STANDARD_ID));
          internalConsumptionDocType.setTable(OBDal.getInstance().get(Table.class,
              TestCostingConstants.INTERNAL_CONSUMPTION_TABLE_ID));
          OBDal.getInstance().save(internalConsumptionDocType);
        }

        // Set allow negatives in General Ledger
        AcctSchema acctSchema = OBDal.getInstance()
            .get(AcctSchema.class, TestCostingConstants.GENERALLEDGER_ID);
        acctSchema.setAllowNegative(false);
        OBDal.getInstance().save(acctSchema);

        // Activate tables in General Ledger Configuration
        List<Table> tableList = new ArrayList<>();
        tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INTERNAL_CONSUMPTION_ID));
        tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INTERNAL_MOVEMENT_ID));
        tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INVENTORY_COUNT_ID));
        tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INOUT_ID));
        tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_PRODUCTION_ID));
        tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_MATCH_INVOICE_ID));

        final OBCriteria<AcctSchemaTable> criteria1 =
            OBDal.getInstance().createCriteria(AcctSchemaTable.class);

        criteria1.addEqual(AcctSchemaTable.PROPERTY_ACCOUNTINGSCHEMA, acctSchema);
        criteria1.addIn(AcctSchemaTable.PROPERTY_TABLE, tableList);
        criteria1.setFilterOnActive(false);
        criteria1.setFilterOnReadableClients(false);
        criteria1.setFilterOnReadableOrganization(false);

        for (AcctSchemaTable acctSchemaTable : criteria1.list()) {
          acctSchemaTable.setActive(true);
          OBDal.getInstance().save(acctSchemaTable);
        }

        OBDal.getInstance().flush();

        // Create costing rule
        CostingRule costingRule = OBProvider.getInstance().get(CostingRule.class);
        TestCostingUtils.setGeneralData(costingRule);
        costingRule.setCostingAlgorithm(OBDal.getInstance()
            .get(CostingAlgorithm.class, TestCostingConstants.AVERAGE_COSTINGALGORITHM_ID));
        costingRule.setWarehouseDimension(true);
        costingRule.setBackdatedTransactionsFixed(true);
        costingRule.setValidated(false);
        costingRule.setStartingDate(null);
        costingRule.setEndingDate(null);

        OBDal.getInstance().save(costingRule);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(costingRule);

        TestCostingUtils.runCostingBackground();
        TestCostingUtils.validateCostingRule(costingRule.getId());

        OBDal.getInstance().commitAndClose();

      } catch (Exception e) {
        System.out.println(e.getMessage());
        throw new OBException(e);

      } finally {
        OBContext.restorePreviousMode();
        TestCostingConstants.runBefore = false;
      }
    }
  }

  @AfterAll
  public static void setFinalConfiguration() {
    try {
      // Set System context
      OBContext.setOBContext(TestCostingConstants.USERADMIN_ID);
      OBContext.setAdminMode(true);

      // Reset costing precision
      Currency currrencyEur = OBDal.getInstance().get(Currency.class, EURO_ID);
      currrencyEur.setCostingPrecision(2L);
      OBDal.getInstance().save(currrencyEur);

      Currency currrencyUsd = OBDal.getInstance().get(Currency.class, DOLLAR_ID);
      currrencyUsd.setCostingPrecision(2L);
      OBDal.getInstance().save(currrencyUsd);

      OBDal.getInstance().flush();

      // Reset QA context
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);

      // Restore organization currency
      Organization organization = OBDal.getInstance()
          .get(Organization.class, TestCostingConstants.SPAIN_ORGANIZATION_ID);
      organization.setCurrency(null);
      OBDal.getInstance().save(organization);

      // Restore allow negatives
      AcctSchema acctSchema = OBDal.getInstance()
          .get(AcctSchema.class, TestCostingConstants.GENERALLEDGER_ID);
      acctSchema.setAllowNegative(true);
      OBDal.getInstance().save(acctSchema);

      // Deactivate tables
      List<Table> tableList = new ArrayList<>();
      tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INTERNAL_CONSUMPTION_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INTERNAL_MOVEMENT_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INVENTORY_COUNT_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INOUT_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_PRODUCTION_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_MATCH_INVOICE_ID));

      final OBCriteria<AcctSchemaTable> criteria =
          OBDal.getInstance().createCriteria(AcctSchemaTable.class);

      criteria.addEqual(AcctSchemaTable.PROPERTY_ACCOUNTINGSCHEMA, acctSchema);
      criteria.addIn(AcctSchemaTable.PROPERTY_TABLE, tableList);
      for (AcctSchemaTable acctSchemaTable : criteria.list()) {
        acctSchemaTable.setActive(false);
        OBDal.getInstance().save(acctSchemaTable);
      }

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);

    } finally {
      OBContext.restorePreviousMode();
      TestCostingConstants.runBefore = true;
    }
  }
}
