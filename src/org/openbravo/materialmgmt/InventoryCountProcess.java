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
 * All portions are Copyright (C) 2012-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.materialmgmt;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.GenericJDBCException;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.materialmgmt.hook.InventoryCountCheckHook;
import org.openbravo.materialmgmt.hook.InventoryCountProcessHook;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.calendar.PeriodControl;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;

public class InventoryCountProcess implements Process {
  private static final Logger log4j = LogManager.getLogger();

  @Inject
  @Any
  private Instance<InventoryCountCheckHook> inventoryCountChecks;

  @Inject
  @Any
  private Instance<InventoryCountProcessHook> inventoryCountProcesses;

  @Override
  public void execute(final ProcessBundle bundle) throws Exception {
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));

    try {
      // retrieve standard params
      final String recordID = (String) bundle.getParams().get("M_Inventory_ID");
      final InventoryCount inventory = OBDal.getInstance().get(InventoryCount.class, recordID);

      // lock inventory
      if (inventory.isProcessNow()) {
        throw new OBException(OBMessageUtils.parseTranslation("@OtherProcessActive@"));
      }
      inventory.setProcessNow(true);
      OBDal.getInstance().save(inventory);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(inventory);
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().commitAndStart();
      }

      OBContext.setAdminMode(false);
      try {
        msg = processInventory(inventory);
      } finally {
        OBContext.restorePreviousMode();
      }

      inventory.setProcessNow(false);

      OBDal.getInstance().save(inventory);
      OBDal.getInstance().flush();

      bundle.setResult(msg);

    } catch (final GenericJDBCException ge) {
      log4j.error("Exception processing physical inventory", ge);
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD(bundle.getConnection(), "Error",
          bundle.getContext().getLanguage()));
      msg.setMessage(ge.getSQLException().getMessage().split("\n")[0]);
      bundle.setResult(msg);
      OBDal.getInstance().rollbackAndClose();
      final String recordID = (String) bundle.getParams().get("M_Inventory_ID");
      final InventoryCount inventory = OBDal.getInstance().get(InventoryCount.class, recordID);
      inventory.setProcessNow(false);
      OBDal.getInstance().save(inventory);
    } catch (final Exception e) {
      log4j.error("Exception processing physical inventory", e);
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD(bundle.getConnection(), "Error",
          bundle.getContext().getLanguage()));
      msg.setMessage(FIN_Utility.getExceptionMessage(e));
      bundle.setResult(msg);
      OBDal.getInstance().rollbackAndClose();
      final String recordID = (String) bundle.getParams().get("M_Inventory_ID");
      final InventoryCount inventory = OBDal.getInstance().get(InventoryCount.class, recordID);
      inventory.setProcessNow(false);
      OBDal.getInstance().save(inventory);
    }

  }

  public OBError processInventory(final InventoryCount inventory) throws OBException {
    return processInventory(inventory, true);
  }

  public OBError processInventory(final InventoryCount inventory, final boolean checkReservationQty)
      throws OBException {
    return processInventory(inventory, checkReservationQty, false);
  }

  public OBError processInventory(final InventoryCount inventory, final boolean checkReservationQty,
      final boolean checkPermanentCost) throws OBException {
    final OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));
    runChecks(inventory);

    //@formatter:off
    String hqlInsert =
            "insert into MaterialMgmtMaterialTransaction" +
            "  (" +
            "    id " +
            "    , active" +
            "    , client" +
            "    , organization" +
            "    , creationDate" +
            "    , createdBy" +
            "    , updated" +
            "    , updatedBy" +
            "    , movementType" +
            "    , checkReservedQuantity" +
            "    , isCostPermanent" +
            "    , movementDate" +
            "    , storageBin" +
            "    , product" +
            "    , attributeSetValue" +
            "    , movementQuantity" +
            "    , uOM" +
            "    , orderQuantity" +
            "    , orderUOM" +
            "    , physicalInventoryLine" +
            "    , transactionProcessDate" +
            // select from inventory line
            "  )" +
            " select get_uuid()" +
            "   , e.active" +
            "   , e.client" +
            "   , e.organization" +
            "   , now()" +
            "   , u" +
            "   , now()" +
            "   , u" +
            "   , 'I+'";
    //@formatter:on
    // We have to set check reservation quantity flag equal to checkReservationQty
    // InventoryCountLine.PROPERTY_ACTIVE-->> Y
    // InventoryCountLine.PROPERTY_PHYSINVENTORY + "." + InventoryCount.PROPERTY_PROCESSED -->> N
    if (checkReservationQty) {
      //@formatter:off
      hqlInsert += 
            "   , e.active";
      //@formatter:on
    } else {
      //@formatter:off
      hqlInsert +=
            "   , e.physInventory.processed";
      //@formatter:on
    }
    // We have to set check permanent cost flag
    // InventoryCountLine.PROPERTY_ACTIVE-->> Y
    // InventoryCountLine.PROPERTY_PHYSINVENTORY + "." + InventoryCount.PROPERTY_PROCESSED -->> N
    if (checkPermanentCost) {
      //@formatter:off
      hqlInsert +=
            "   , e.active";
      //@formatter:on
    } else {
      //@formatter:off
      hqlInsert +=
            "   , e.physInventory.processed";
      //@formatter:on
    }
    //@formatter:off
    hqlInsert +=
            "   , e.physInventory.movementDate" +
            "   , e.storageBin" +
            "   , e.product" +
            "   , asi" +
            "   , e.quantityCount - COALESCE(" + "e.bookQuantity, 0)" +
            "   , e.uOM" +
            "   , e.orderQuantity - COALESCE(" + "e.quantityOrderBook, 0)" +
            "   , e.orderUOM" +
            "   , e" +
            "   , to_timestamp(to_char(:currentDate), to_char('DD-MM-YYYY HH24:MI:SS'))" +
            "   from MaterialMgmtInventoryCountLine as e" +
            "     , ADUser as u" +
            "     , AttributeSetInstance as asi" +
            "     , Product as p" +
            "  where e.physInventory.id = :invId" +
            "    and " +
            "      (" +
            "        e.quantityCount != e.bookQuantity" +
            "        or e.orderQuantity != e.quantityOrderBook" +
            "      )" +
            "    and u.id = :userId" +
            "    and asi.id = COALESCE(e.attributeSetValue.id , '0')" +
            // Non Stockable Products should not generate warehouse transactions
            "    and e.product.id = p.id" +
            "    and p.stocked = 'Y'" +
            "    and p.productType = 'I'";
    //@formatter:on

    try {
      final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
      OBDal.getInstance()
          .getSession()
          .createQuery(hqlInsert)
          .setParameter("invId", inventory.getId())
          .setParameter("userId", OBContext.getOBContext().getUser().getId())
          .setParameter("currentDate", dateFormatter.format(new Date()))
          .executeUpdate();

      if (!"C".equals(inventory.getInventoryType()) && !"O".equals(inventory.getInventoryType())) {
        checkStock(inventory);
      }

      executeHooks(inventoryCountProcesses, inventory);
    } catch (final Exception e) {
      throw new OBException(e.getMessage(), e.getCause());
    }
    inventory.setProcessed(true);
    OBDal.getInstance().flush();

    // Update inventory date for inventory count lines whose qtycount is equal to book quantity or
    // orderquantity is equals to quantity order book
    updateDateInventory(inventory);

    return msg;
  }

  private void runChecks(final InventoryCount inventory) {
    try {
      executeHooks(inventoryCountChecks, inventory);
    } catch (Exception genericException) {
      throw new OBException(genericException.getMessage(), genericException.getCause());
    }
    checkInventoryAlreadyProcessed(inventory);
    checkMandatoryAttributesWithoutVavlue(inventory);
    checkDuplicatedProducts(inventory);
    final Organization org = inventory.getOrganization();
    checkIfOrganizationIsReady(org);
    checkOrganizationAllowsTransactions(org);
    checkDifferentLegalInLinesAndHeader(inventory, org);
    checkPeriodsNotAvailable(inventory, org);
  }

  private void checkInventoryAlreadyProcessed(final InventoryCount inventory) {
    if (inventory.isProcessed()) {
      throw new OBException(OBMessageUtils.parseTranslation("@AlreadyPosted@"));
    }
  }

  private void checkMandatoryAttributesWithoutVavlue(final InventoryCount inventory) {
    final InventoryCountLine inventoryLine = getLineWithMandatoryAttributeWithoutValue(inventory);
    if (inventoryLine != null) {
      throw new OBException(OBMessageUtils.parseTranslation(
          "@Inline@ " + (inventoryLine).getLineNo() + " @productWithoutAttributeSet@"));
    }
  }

  private InventoryCountLine getLineWithMandatoryAttributeWithoutValue(
      final InventoryCount inventory) {
    //@formatter:off
    final String hqlWhere =
                  "as icl" +
                  "  join icl.product as p" +
                  "  join icl.storageBin as sb" +
                  "  join p.attributeSet as aset" +
                  " where icl.physInventory.id = :inventoryId" +
                  "   and aset.requireAtLeastOneValue = true" +
                  "   and coalesce(p.useAttributeSetValueAs, '-') <> 'F'" +
                  "   and coalesce(icl.attributeSetValue, '0') = '0' " +
                  // Allow to regularize to 0 any existing Stock without attribute for this Product
                  // (this situation can happen when there is a bug in a different part of the code,
                  // but the user should be able always to zero this stock)
                  "   and" +
                  "     (" +
                  "       icl.quantityCount <> 0" +
                  "       or" +
                  "       (" +
                  "         icl.quantityCount = 0" +
                  "         and not exists" +
                  "         (" +
                  "           select 1" +
                  "             from MaterialMgmtStorageDetail sd" +
                  "            where sd.storageBin.id = sb.id" +
                  "              and sd.product.id = p.id" +
                  "              and sd.attributeSetValue = '0'" +
                  "              and sd.uOM.id = icl.uOM.id" +
                  "              and sd.quantityOnHand <> 0" +
                  "              and sd.quantityInDraftTransactions <> 0" +
                  "         )" +
                  "       )" +
                  "     )" +
                  "  order by icl.lineNo ";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(InventoryCountLine.class, hqlWhere)
        .setNamedParameter("inventoryId", inventory.getId())
        .setMaxResult(1)
        .uniqueResult();
  }

  private void checkDuplicatedProducts(final InventoryCount inventory) {
    final List<InventoryCountLine> inventoryLineList = getLinesWithDuplicatedProducts(inventory);
    if (!inventoryLineList.isEmpty()) {
      final StringBuilder errorMessage = new StringBuilder("");
      for (final InventoryCountLine icl2 : inventoryLineList) {
        errorMessage.append(icl2.getLineNo().toString() + ", ");
      }
      throw new OBException(OBMessageUtils
          .parseTranslation("@Thelines@ " + errorMessage.toString() + "@sameInventorylines@"));
    }
  }

  private List<InventoryCountLine> getLinesWithDuplicatedProducts(final InventoryCount inventory) {
    //@formatter:off
    final String hqlWhere =
                  "as icl" +
                  " where icl.physInventory.id = :inventoryId" +
                  "   and exists" +
                  "     (" +
                  "       select 1 " +
                  "         from MaterialMgmtInventoryCountLine as icl2" +
                  "        where icl.physInventory = icl2.physInventory" +
                  "          and icl.product = icl2.product" +
                  "          and coalesce(icl.attributeSetValue, '0') = coalesce(icl2.attributeSetValue, '0')" +
                  "          and coalesce(icl.orderUOM, '0') = coalesce(icl2.orderUOM, '0')" +
                  "          and coalesce(icl.uOM, '0') = coalesce(icl2.uOM, '0')" +
                  "          and icl.storageBin = icl2.storageBin" +
                  "          and icl.lineNo <> icl2.lineNo" +
                  "     )" +
                  " order by icl.product" +
                  "   , icl.attributeSetValue" +
                  "   , icl.storageBin" +
                  "   , icl.orderUOM" +
                  "   , icl.lineNo";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(InventoryCountLine.class, hqlWhere)
        .setNamedParameter("inventoryId", inventory.getId())
        .list();
  }

  private void checkIfOrganizationIsReady(final Organization org) {
    if (!org.isReady()) {
      throw new OBException(OBMessageUtils.parseTranslation("@OrgHeaderNotReady@"));
    }
  }

  private void checkOrganizationAllowsTransactions(final Organization org) {
    if (!org.getOrganizationType().isTransactionsAllowed()) {
      throw new OBException(OBMessageUtils.parseTranslation("@OrgHeaderNotTransAllowed@"));
    }
  }

  private void checkDifferentLegalInLinesAndHeader(final InventoryCount inventory,
      final Organization org) {
    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(inventory.getClient().getId());
    final Organization inventoryLegalOrBusinessUnitOrg = osp.getLegalEntityOrBusinessUnit(org);
    final List<InventoryCountLine> inventoryLineList = getLinesWithDifferentOrganizationThanHeader(
        inventory, org);
    if (!inventoryLineList.isEmpty()) {
      for (final InventoryCountLine inventoryLine : inventoryLineList) {
        if (!inventoryLegalOrBusinessUnitOrg.getId()
            .equals(osp.getLegalEntityOrBusinessUnit(inventoryLine.getOrganization()).getId())) {
          throw new OBException(OBMessageUtils.parseTranslation("@LinesAndHeaderDifferentLEorBU@"));
        }
      }
    }
  }

  private List<InventoryCountLine> getLinesWithDifferentOrganizationThanHeader(
      final InventoryCount inventory, final Organization org) {
    //@formatter:off
    final String hql = 
                  "as py "+
                  " where py.physInventory.id = :inventoryId" +
                  "   and py.organization.id <> :organizationId";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(InventoryCountLine.class, hql)
        .setNamedParameter("inventoryId", inventory.getId())
        .setNamedParameter("organizationId", org.getId())
        .list();
  }

  private void checkPeriodsNotAvailable(final InventoryCount inventory, final Organization org) {
    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(inventory.getClient().getId());
    final Organization inventoryLegalOrBusinessUnitOrg = osp.getLegalEntityOrBusinessUnit(org);
    if (inventoryLegalOrBusinessUnitOrg.getOrganizationType().isLegalEntityWithAccounting()) {
      //@formatter:off
      final String hqlWhere =
                    "as pc " +
                    "  join pc.period as p" +
                    " where p.startingDate <= :dateStarting" +
                    "   and p.endingDate >= :dateEnding" +
                    "   and pc.documentCategory = 'MMI' " +
                    "   and pc.organization.id = :orgId" +
                    "   and pc.periodStatus = 'O'";
      //@formatter:on

      final PeriodControl result = OBDal.getInstance()
          .createQuery(PeriodControl.class, hqlWhere)
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false)
          .setNamedParameter("dateStarting", inventory.getMovementDate())
          .setNamedParameter("dateEnding",
              DateUtils.truncate(inventory.getMovementDate(), Calendar.DATE))
          .setNamedParameter("orgId", osp.getPeriodControlAllowedOrganization(org).getId())
          .setMaxResult(1)
          .uniqueResult();
      if (result == null) {
        throw new OBException(OBMessageUtils.parseTranslation("@PeriodNotAvailable@"));
      }
    }
  }

  private void updateDateInventory(final InventoryCount inventory) {

    try {
      for (final InventoryCountLine invCountLine : inventory
          .getMaterialMgmtInventoryCountLineList()) {
        if (invCountLine.getQuantityCount().compareTo(invCountLine.getBookQuantity()) == 0
            || (invCountLine.getOrderQuantity() != null
                && invCountLine.getQuantityOrderBook() != null && invCountLine.getOrderQuantity()
                    .compareTo(invCountLine.getQuantityOrderBook()) == 0)) {
          final org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
          final CallableStatement updateStockStatement = cp.getConnection()
              .prepareCall("{call M_UPDATE_INVENTORY (?,?,?,?,?,?,?,?,?,?,?,?,?)}");
          // client
          updateStockStatement.setString(1, invCountLine.getClient().getId());
          // org
          updateStockStatement.setString(2, invCountLine.getOrganization().getId());
          // user
          updateStockStatement.setString(3, OBContext.getOBContext().getUser().getId());
          // product
          updateStockStatement.setString(4, invCountLine.getProduct().getId());
          // locator
          updateStockStatement.setString(5, invCountLine.getStorageBin().getId());
          // attributesetinstance
          updateStockStatement.setString(6,
              invCountLine.getAttributeSetValue() != null
                  ? invCountLine.getAttributeSetValue().getId()
                  : null);
          // uom
          updateStockStatement.setString(7, invCountLine.getUOM().getId());
          // product uom
          updateStockStatement.setString(8,
              invCountLine.getOrderUOM() != null ? invCountLine.getOrderUOM().getId() : null);
          // p_qty
          updateStockStatement.setBigDecimal(9, BigDecimal.ZERO);
          // p_qtyorder
          updateStockStatement.setBigDecimal(10, BigDecimal.ZERO);
          // p_dateLastInventory --- **
          updateStockStatement.setDate(11,
              new java.sql.Date(inventory.getMovementDate().getTime()));
          // p_preqty
          updateStockStatement.setBigDecimal(12, BigDecimal.ZERO);
          // p_preqtyorder
          updateStockStatement.setBigDecimal(13, BigDecimal.ZERO);

          updateStockStatement.execute();
        }
      }
      OBDal.getInstance().flush();
    } catch (final Exception e) {
      log4j.error("Error in updateDateInventory while Inventory Count Process", e);
      throw new OBException(e.getMessage(), e);
    }
  }

  private void checkStock(final InventoryCount inventory) {
    String attribute;
    //@formatter:off
    final String hql =
                  "select sd.id " +
                  "  from MaterialMgmtInventoryCountLine as icl" +
                  "    , MaterialMgmtStorageDetail as sd" +
                  "    , Locator as l" +
                  "    , MaterialMgmtInventoryStatus as invs" +
                  " where icl.physInventory.id = :physInventoryId" +
                  "   and sd.product = icl.product" +
                  "   and " +
                  "     (" +
                  "       sd.quantityOnHand < 0" +
                  "       or sd.onHandOrderQuanity < 0" +
                  "     )" +
                  // Check only negative Stock for the Bins of the Lines of the Physical Inventory
                  "   and sd.storageBin.id = icl.storageBin.id" +
                  "   and l.id = icl.storageBin.id" +
                  "   and l.inventoryStatus.id = invs.id" +
                  "   and invs.overissue = false" +
                  " order by icl.lineNo";
    //@formatter:on

    final List<String> resultList = OBDal.getInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setParameter("physInventoryId", inventory.getId())
        .setMaxResults(1)
        .list();

    if (!resultList.isEmpty()) {
      final StorageDetail storageDetail = OBDal.getInstance()
          .get(StorageDetail.class, resultList.get(0));
      attribute = (!storageDetail.getAttributeSetValue().getIdentifier().isEmpty())
          ? " @PCS_ATTRIBUTE@ '" + storageDetail.getAttributeSetValue().getIdentifier() + "', "
          : "";
      throw new OBException(Utility
          .messageBD(new DalConnectionProvider(), "insuffient_stock",
              OBContext.getOBContext().getLanguage().getLanguage())
          .replace("%1", storageDetail.getProduct().getIdentifier())
          .replace("%2", attribute)
          .replace("%3", storageDetail.getUOM().getIdentifier())
          .replace("%4", storageDetail.getStorageBin().getIdentifier()));
    }
  }

  private void executeHooks(final Instance<? extends Object> hooks, final InventoryCount inventory)
      throws Exception {
    if (hooks != null) {
      for (final Iterator<? extends Object> procIter = hooks.iterator(); procIter.hasNext();) {
        final Object proc = procIter.next();
        if (proc instanceof InventoryCountProcessHook) {
          ((InventoryCountProcessHook) proc).exec(inventory);
        } else {
          ((InventoryCountCheckHook) proc).exec(inventory);
        }
      }
    }
  }
}
