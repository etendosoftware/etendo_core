package org.openbravo.test.accounting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBConfigFileProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.ConnectionProviderImpl;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.PoolNotFoundException;
import org.openbravo.financial.ResetAccounting;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaTable;
import org.openbravo.model.financialmgmt.gl.GLCategory;
import org.openbravo.model.materialmgmt.transaction.InternalConsumption;
import org.openbravo.model.materialmgmt.transaction.InternalConsumptionLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.base.TestConstants;
import org.openbravo.test.costing.utils.TestCostingUtils;

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
  private static final String GL_CATEGORY = "EDA7B85AF9A5486D9B00CAFFD3B86FC2"; // ES Standard
  private static final String DOC_TYPE_NAME = "Material Internal Consumption";
  private static final String DB_MESSAGE_NAME = "NoDocTypeForDocument";
  private static final Logger log4j = Logger.getLogger(PostedNoDocConfigTest.class);

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN,
        TestConstants.Clients.FB_GRP, ORGANIZATION_SPAIN);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);
  }

  /**
   * Test to verify the correct processing of an internal consumption document
   * when no document type is configured. The test simulates the creation and
   * processing of an internal consumption, ensures the document is posted
   * correctly, and checks the expected error message when no document type is configured.
   * <p>
   * Steps:
   * 1. Create an internal consumption document with material management lines.
   * 2. Process the internal consumption and refresh its status.
   * 3. Activate the material management consumption table and run costing background.
   * 4. Attempt to post the document and verify the expected posting result.
   * 5. If the document is in the "Completed" status, it is voided; otherwise, it is deleted.
   * <p>
   * Expected Result:
   * - The expected error message is displayed when no document type is configured.
   */
  @Test
  public void testCountWithoutDocTypeConfigured() {
    InternalConsumption internalConsumption = null;
    try {
      internalConsumption = createHeaderAndMaterialManagementConsumptionLines();
      processInternalConsumption(internalConsumption, "CO");
      OBDal.getInstance().refresh(internalConsumption);

      activeOrDeactiveMaterialManagementConsumptionTable(true);
      TestCostingUtils.runCostingBackground();

      Table materialManagementConsumptionTable = getMaterialManagementConsumptionTable(internalConsumption);
      String result = postDocument(internalConsumption, materialManagementConsumptionTable);
      OBDal.getInstance().refresh(internalConsumption);

      assertEquals(OBMessageUtils.messageBD(DB_MESSAGE_NAME), result);
      assertEquals("DT", internalConsumption.getPosted());
    } catch (Exception e) {
      log4j.error(e.getMessage(), e);
      fail(e.getMessage());
    } finally {
      if (internalConsumption != null) {
        OBDal.getInstance().refresh(internalConsumption);
        if (StringUtils.equals("CO", internalConsumption.getStatus())) {
          processInternalConsumption(internalConsumption, "VO");
        } else {
          OBDal.getInstance().remove(internalConsumption);
        }
        OBDal.getInstance().flush();
      }
    }
  }

  /**
   * Test to ensure that an OBException is thrown when posting an internal consumption document
   * with a document type that is later removed. The test handles document creation, processing,
   * and posting, and it verifies that the proper exception is raised when the document type is missing.
   * <p>
   * Steps:
   * 1. Create and process an internal consumption document with material management lines.
   * 2. Activate the material management consumption table.
   * 3. Create a document type and associate it with the internal consumption.
   * 4. Run the costing background and attempt to post the document.
   * 5. Verify the posting status and remove the document type.
   * 6. Attempt to delete accounting records and expect an OBException due to the missing document type.
   * 7. In the finally block, ensure proper cleanup of the internal consumption and document type.
   * <p>
   * Expected Result:
   * - The document should post with a status of "Y".
   * - An OBException with the message "@NoDocTypeForDocument@" should be thrown when the document type is missing.
   */
  @Test
  public void testDiscountWithoutDocTypeConfigured() {
    String docTypeId = null;
    InternalConsumption internalConsumption = null;
    try {
      internalConsumption = createHeaderAndMaterialManagementConsumptionLines();
      processInternalConsumption(internalConsumption, "CO");

      Table materialManagementConsumptionTable = getMaterialManagementConsumptionTable(internalConsumption);
      activeOrDeactiveMaterialManagementConsumptionTable(true);
      docTypeId = createDocumentType(materialManagementConsumptionTable).getId();
      TestCostingUtils.runCostingBackground();

      postDocument(internalConsumption, materialManagementConsumptionTable);
      OBDal.getInstance().refresh(internalConsumption);
      assertEquals("Y", internalConsumption.getPosted());

      removeDocType(docTypeId);

      ResetAccounting.delete(internalConsumption.getClient().getId(),
          internalConsumption.getOrganization().getId(),
          internalConsumption.getEntity().getTableId(), internalConsumption.getId(),
          OBDateUtils.formatDate(internalConsumption.getMovementDate()), null);

      fail("Expected an OBException to be thrown");
    } catch (Exception e) {
      assertEquals("@NoDocTypeForDocument@", e.getMessage());
    } finally {
      if (internalConsumption != null) {
        OBDal.getInstance().refresh(internalConsumption);
        if (StringUtils.equals("Y", internalConsumption.getPosted())) {
          processInternalConsumption(internalConsumption, "VO");
          OBDal.getInstance().flush();
        }
      }
      if (!StringUtils.isEmpty(docTypeId)) {
        removeDocType(docTypeId);
      }
    }
  }

  /**
   * Creates an internal consumption object and its corresponding consumption lines.
   *
   * @return the internal consumption object created
   */
  private InternalConsumption createHeaderAndMaterialManagementConsumptionLines() {
    InternalConsumption internalConsumption = createInternalConsumption();
    createInternalConsumptionLine(internalConsumption);

    return internalConsumption;
  }

  /**
   * Removes a document type by its ID.
   *
   * @param docTypeId
   *     the ID of the document type to be removed
   */
  private void removeDocType(String docTypeId) {
    DocumentType docType = OBDal.getInstance().get(DocumentType.class, docTypeId);
    if (docType != null) {
      OBDal.getInstance().remove(docType);
      OBDal.getInstance().flush();
    }
  }

  /**
   * Retrieves a Table object related to material management consumption based on the provided document.
   *
   * @param document
   *     the BaseOBObject representing the document
   * @return the Table object related to material management consumption or null if not found
   */
  private Table getMaterialManagementConsumptionTable(BaseOBObject document) {
    final OBCriteria<Table> criteria = OBDal.getInstance().createCriteria(Table.class);
    criteria.addEqual(Table.PROPERTY_NAME, document.getEntityName());
    criteria.setMaxResults(1);
    return (Table) criteria.uniqueResult();
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

  private void processInternalConsumption(InternalConsumption internalConsumption, String status) {
    try {
      List<Object> param = new ArrayList<>();
      param.add(null);
      param.add(internalConsumption.getId());
      param.add(status);

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

  private DocumentType createDocumentType(Table table) {
    try {
      DocumentType docType = OBProvider.getInstance().get(DocumentType.class);
      docType.setName(DOC_TYPE_NAME);
      docType.setPrintText(DOC_TYPE_NAME);
      docType.setGLCategory(OBDal.getInstance().get(GLCategory.class, GL_CATEGORY));
      docType.setDocumentCategory("MIC");
      docType.setActive(true);
      docType.setTable(table);

      OBDal.getInstance().save(docType);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(docType);

      return docType;
    } catch (Exception e) {
      log4j.error(e.getMessage(), e);
      throw new OBException(e);
    }
  }

  public String postDocument(InternalConsumption internalConsumption, Table table) {
    ConnectionProvider conn = getConnectionProvider();
    Connection con = null;

    try {
      String tableId = table.getId();
      con = conn.getConnection();
      AcctServer acct = AcctServer.get(tableId, internalConsumption.getClient().getId(),
          internalConsumption.getOrganization().getId(), conn);

      return acct.catchPostError(internalConsumption.getId(), false,
          new VariablesSecureApp("100", internalConsumption.getClient().getId(),
              internalConsumption.getOrganization().getId()),
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
