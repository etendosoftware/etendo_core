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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.costing;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
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

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCostingBase extends WeldBaseTest {

  @Before
  public void setInitialConfiguration() {
    // FIXME: Change setInitialConfiguration to @BeforeClass and remove runBefore flag
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

        // Create Internal Consumption Document Type if it does not exist for this context
        OBCriteria<DocumentType> internalConsCrit = OBDal.getInstance().createCriteria(DocumentType.class);
        internalConsCrit.add(Restrictions.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY,
            TestCostingConstants.MAT_INT_CONSUMPTION_DOC_CAT));
        internalConsCrit.add(
            Restrictions.eq(DocumentType.PROPERTY_TABLE, OBDal.getInstance().get(Table.class,
                TestCostingConstants.INTERNAL_CONSUMPTION_TABLE_ID)));
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

        // Active tables in General Ledger Configuration
        List<Table> tableList = new ArrayList<Table>();
        tableList.add(OBDal.getInstance()
            .get(Table.class, TestCostingConstants.TABLE_INTERNAL_CONSUMPTION_ID));
        tableList.add(
            OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INTERNAL_MOVEMENT_ID));
        tableList.add(
            OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INVENTORY_COUNT_ID));
        tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INOUT_ID));
        tableList
            .add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_PRODUCTION_ID));
        tableList
            .add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_MATCH_INVOICE_ID));
        final OBCriteria<AcctSchemaTable> criteria1 = OBDal.getInstance()
            .createCriteria(AcctSchemaTable.class);
        criteria1.add(Restrictions.eq(AcctSchemaTable.PROPERTY_ACCOUNTINGSCHEMA, acctSchema));
        criteria1.add(Restrictions.in(AcctSchemaTable.PROPERTY_TABLE, tableList));
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
      }

      finally {
        OBContext.restorePreviousMode();
        TestCostingConstants.runBefore = false;
      }
    }
  }

  @AfterClass
  public static void setFinalConfiguration() {
    try {
      // Set System context
      OBContext.setOBContext(TestCostingConstants.USERADMIN_ID);
      OBContext.setAdminMode(true);

      // Set EUR currency costing precision
      Currency currrencyEur = OBDal.getInstance().get(Currency.class, EURO_ID);
      currrencyEur.setCostingPrecision(2L);
      OBDal.getInstance().save(currrencyEur);

      // Set USD currency costing precision
      Currency currrencyUsd = OBDal.getInstance().get(Currency.class, DOLLAR_ID);
      currrencyUsd.setCostingPrecision(2L);
      OBDal.getInstance().save(currrencyUsd);

      OBDal.getInstance().flush();

      // Set QA context
      OBContext.setOBContext(TestCostingConstants.ADMIN_USER_ID,
          TestCostingConstants.QATESTING_ROLE_ID, TestCostingConstants.QATESTING_CLIENT_ID,
          TestCostingConstants.SPAIN_ORGANIZATION_ID);

      // Set Spain organization currency
      Organization organization = OBDal.getInstance()
          .get(Organization.class, TestCostingConstants.SPAIN_ORGANIZATION_ID);
      organization.setCurrency(null);
      OBDal.getInstance().save(organization);

      // Set allow negatives in General Ledger
      AcctSchema acctSchema = OBDal.getInstance()
          .get(AcctSchema.class, TestCostingConstants.GENERALLEDGER_ID);
      acctSchema.setAllowNegative(true);
      OBDal.getInstance().save(acctSchema);

      // Active tables in General Ledger Configuration
      List<Table> tableList = new ArrayList<Table>();
      tableList.add(
          OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INTERNAL_CONSUMPTION_ID));
      tableList.add(
          OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INTERNAL_MOVEMENT_ID));
      tableList
          .add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INVENTORY_COUNT_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_INOUT_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_PRODUCTION_ID));
      tableList
          .add(OBDal.getInstance().get(Table.class, TestCostingConstants.TABLE_MATCH_INVOICE_ID));
      final OBCriteria<AcctSchemaTable> criteria = OBDal.getInstance()
          .createCriteria(AcctSchemaTable.class);
      criteria.add(Restrictions.eq(AcctSchemaTable.PROPERTY_ACCOUNTINGSCHEMA, acctSchema));
      criteria.add(Restrictions.in(AcctSchemaTable.PROPERTY_TABLE, tableList));
      for (AcctSchemaTable acctSchemaTable : criteria.list()) {
        acctSchemaTable.setActive(false);
        OBDal.getInstance().save(acctSchemaTable);
      }

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
      TestCostingConstants.runBefore = true;
    }
  }

}
