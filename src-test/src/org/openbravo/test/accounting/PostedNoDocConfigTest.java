package org.openbravo.test.accounting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBConfigFileProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.ConnectionProviderImpl;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.PoolNotFoundException;
import org.openbravo.financial.ResetAccounting;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.gl.GLCategory;
import org.openbravo.test.costing.utils.TestCostingUtils;

import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaTable;
import org.openbravo.model.materialmgmt.transaction.InternalConsumption;
import org.openbravo.model.materialmgmt.transaction.InternalConsumptionLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.base.TestConstants;

/**
 * This class tests the behavior of internal consumption operations when document types
 * are not configured, ensuring the system handles the errors appropriately
 * and provides proper feedback to the user.
 *
 * <p>The test methods included verify that:
 * <ul>
 * <li>The first test verifies that attempting to post an "internal consumption" document without a configured base document type will result in an error, ensuring the system correctly prevents accounting.</li>
 * <li>The second test checks that attempting to unpost an already posted "internal consumption" document without a configured base document type will also result in an error, highlighting the need for proper configuration before document processing.</li>
 * </ul>
 * </p>
 *
 * <p>These tests are specifically designed for task [EPL-534] and leave
 * residuals in the database.</p>
 */

public class PostedNoDocConfigTest extends WeldBaseTest {
  private static final String ORGANIZATION_SPAIN = "B843C30461EA4501935CB1D125C9C25A";
  private static final String PRODUCT_ID = "C0E3824CC5184B7F9746D195ACAC2CCF"; // Cerveza Lager 0,5L
  private static final String STORAGE_BIN_ID = "54EB861A446D464EAA433477A1D867A6"; // Rn-0-0-0
  private static final String MATERIAL_MANAGEMENT_CONSUMPTION = "B70331659B1142FFB0AA0F862B3A9079";
  private static final String GL_CATEGORY = "2B5AE4A4047540A1A5DCEC9E3B9441C1"; // ES AP Invoice
  private static final String ORG_STR = "organization";
  private static final String CLIENT_STR = "client";
  private static final String DOC_TYPE_NAME = "Material Internal Consumption";
  private static final String DB_MESSAGE_NAME = "NoDocTypeForDocument";
  private static final Logger log4j = Logger.getLogger(PostedNoDocConfigTest.class);

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN,
        TestConstants.Clients.FB_GRP, ORGANIZATION_SPAIN);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);
  }

  @Test
  public void testCountWithoutDocTypeConfigured() {
    try {
      InternalConsumption internalConsumption = createInternalConsumption();

      createInternalConsumptionLine(internalConsumption);

      TestCostingUtils.runCostingBackground();

      processInternalConsumption(internalConsumption);

      activeOrDeactiveMaterialManagementConsumptionTable(true);


      OBDal.getInstance().commitAndClose();

      String result = postDocument(internalConsumption);

      assertEquals(OBMessageUtils.messageBD(DB_MESSAGE_NAME), result);
      assertEquals("N", internalConsumption.getPosted());
    } catch (Exception e) {
      log4j.error(e.getMessage(), e);
      fail(e.getMessage());
    }
  }

  @Test
  public void testDiscountWithoutDocTypeConfigured() {
    try {
      InternalConsumption internalConsumption = createInternalConsumption();

      createInternalConsumptionLine(internalConsumption);

      processInternalConsumption(internalConsumption);

      activeOrDeactiveMaterialManagementConsumptionTable(true);

      DocumentType docType = createDocumentType();

      TestCostingUtils.runCostingBackground();

      OBDal.getInstance().commitAndClose();

      postDocument(internalConsumption);

      OBDal.getInstance().refresh(internalConsumption);
      assertEquals("Y", internalConsumption.getPosted());

      OBDal.getInstance().remove(docType);
      OBDal.getInstance().flush();

      ResetAccounting.delete(internalConsumption.getClient().getId(),
              internalConsumption.getOrganization().getId(),
              internalConsumption.getEntity().getTableId(), internalConsumption.getId(), null, null);

      fail("Expected an OBException to be thrown");
    } catch (Exception e) {
      assertEquals("@NoDocTypeForDocument@", e.getMessage());
    }
  }

  private InternalConsumption createInternalConsumption() {
    try {
      InternalConsumption internalConsumption = OBProvider.getInstance().get(InternalConsumption.class);
      internalConsumption.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      internalConsumption.setMovementDate(new Date());
      internalConsumption.setName("Test");

      OBDal.getInstance().save(internalConsumption);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(internalConsumption);

      return internalConsumption;
    } catch (Exception e) {
      log4j.error(e.getMessage(), e);
      throw new OBException(e);
    }
  }

  private InternalConsumptionLine createInternalConsumptionLine(InternalConsumption internalConsumption) {
    try {
      Product product = OBDal.getInstance().get(Product.class, PRODUCT_ID);
      Locator storageBin = OBDal.getInstance().get(Locator.class, STORAGE_BIN_ID);
      InternalConsumptionLine internalConsumptionLine = OBProvider.getInstance().get(InternalConsumptionLine.class);
      internalConsumptionLine.setInternalConsumption(internalConsumption);
      internalConsumptionLine.setMovementQuantity(BigDecimal.ONE);
      internalConsumptionLine.setProduct(product);
      internalConsumptionLine.setStorageBin(storageBin);
      internalConsumptionLine.setUOM(product.getUOM());

      OBDal.getInstance().save(internalConsumptionLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(internalConsumptionLine);

      return internalConsumptionLine;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  private void processInternalConsumption(InternalConsumption internalConsumption) {
    try {
      List<Object> param = new ArrayList<>();
      param.add(null);
      param.add(internalConsumption.getId());
      param.add("CO");

      CallStoredProcedure.getInstance().call("m_internal_consumption_post1", param, null, true, false);
    } catch (Exception e) {
      log4j.error(e.getMessage(), e);
      throw new OBException(e);
    }
  }

  private void activeOrDeactiveMaterialManagementConsumptionTable(boolean active) {
    try {
      AcctSchemaTable acctSchemaTable = OBDal.getInstance().get(AcctSchemaTable.class, MATERIAL_MANAGEMENT_CONSUMPTION);
      acctSchemaTable.setActive(active);
      OBDal.getInstance().save(acctSchemaTable);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(acctSchemaTable);
    } catch (Exception e) {
      log4j.error(e.getMessage(), e);
      throw new OBException(e);
    }
  }

  private DocumentType createDocumentType() {
    try {
      DocumentType docType = OBProvider.getInstance().get(DocumentType.class);
      docType.setName(DOC_TYPE_NAME);
      docType.setPrintText(DOC_TYPE_NAME);
      docType.setGLCategory(OBDal.getInstance().get(GLCategory.class, GL_CATEGORY));
      docType.setDocumentCategory("MIC");
      docType.setActive(true);

      OBDal.getInstance().save(docType);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(docType);

      return docType;
    } catch (Exception e) {
      log4j.error(e.getMessage(), e);
      throw new OBException(e);
    }
  }

  public String postDocument(BaseOBObject document) {
    ConnectionProvider conn = getConnectionProvider();
    Connection con = null;

    try {
      final OBCriteria<Table> criteria = OBDal.getInstance().createCriteria(Table.class);
      criteria.add(Restrictions.eq(Table.PROPERTY_NAME, document.getEntityName()));
      criteria.setMaxResults(1);
      String tableId = ((Table) criteria.uniqueResult()).getId();
      con = conn.getTransactionConnection();
      AcctServer acct = AcctServer.get(tableId, ((Client) document.get(CLIENT_STR)).getId(),
          ((Organization) document.get(ORG_STR)).getId(), conn);

      return acct.catchPostError((String) document.getId(), false,
          new VariablesSecureApp("100", ((Client) document.get(CLIENT_STR)).getId(),
              ((Organization) document.get(ORG_STR)).getId()),
          conn, con);

    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  public ConnectionProvider getConnectionProvider() {
    try {
      final String propFile = OBConfigFileProvider.getInstance().getFileLocation();
      return new ConnectionProviderImpl(
          propFile + "/Openbravo.properties");
    } catch (PoolNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }
}
