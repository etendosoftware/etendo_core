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
 * All portions are Copyright (C) 2017-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.materialmgmt.refinventory;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollableResults;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.service.db.CallProcess;

/**
 * Abstract class that should be extended by box/unbox referenced inventory concrete
 * implementations. It is in charge of boxing and unboxing referenced inventories.
 * 
 * The process basically reads and validates the storage details, quantities and destination bins in
 * order to create a new goods movement to process the request.
 * 
 * The reservation management is important. For each storage detail that is going to be box/unboxed
 * to/from a referenced inventory, it first tries to create a goods movement line without any
 * associated reservation. This is only possible when there is enough quantity on hand not reserved
 * yet to partially or fully fulfill the box/unbox movement qty. After that, if remaining quantity,
 * it tries to move an existing reservation if possible.
 * 
 * When a reservation is involved, the process tries to select first any valid reservation,
 * excluding reservations forced to a different (destination) bin, ordering by non-allocated first,
 * not forced to an attribute set first (because these are bit complex to manage due to referenced
 * inventory behavior) and by available qty asc. The movement line is created with a movement
 * quantity which is the minimum quantity between the available reservation quantity and the
 * quantity pending to box/unbox. This is the key to be able to successfully process the reservation
 * reallocation.
 */
abstract class ReferencedInventoryProcessor {
  private static final String M_MOVEMENT_POST_ID = "122";
  private static final String JS_STORAGEDETAIL_ID = "id";
  private static final String QUANTITY = "quantityOnHand";

  private ReferencedInventory referencedInventory;
  private JSONArray selectedStorageDetails;

  /**
   * Returns the right ReferencedInventory which will be associated to the given storage detail
   */
  protected abstract AttributeSetInstance getAttributeSetInstanceTo(
      final StorageDetail storageDetail);

  /**
   * Returns a string with the name for the generated goods movement
   */
  protected abstract String generateInternalMovementName();

  private String getFixedInternalMovementName() {
    return StringUtils.left(generateInternalMovementName(), 60);
  }

  /**
   * Returns the expected goods movement line bin to
   */
  protected abstract String getNewStorageBinId(final JSONObject storageDetailJS);

  private Locator getNewStorageBin(final JSONObject storageDetailJS) {
    return OBDal.getInstance().getProxy(Locator.class, getNewStorageBinId(storageDetailJS));
  }

  protected ReferencedInventoryProcessor(final ReferencedInventory referencedInventory) {
    setAndValidateReferencedInventory(referencedInventory);
  }

  /**
   * Returns the Referenced Inventory linked to this box/unbox process
   */
  protected ReferencedInventory getReferencedInventory() {
    return referencedInventory;
  }

  /**
   * Returns the Organization associated to the referenced inventory
   */
  protected Organization getReferencedInventoryOrganization() {
    return referencedInventory.getOrganization();
  }

  private void setAndValidateReferencedInventory(final ReferencedInventory referencedInventory) {
    Check.isNotNull(referencedInventory, "Referenced Inventory parameter can't be null");
    this.referencedInventory = referencedInventory;
  }

  protected void setSelectedStorageDetailsAndValidateThem(final JSONArray selectedStorageDetails)
      throws JSONException {
    this.selectedStorageDetails = selectedStorageDetails;
    checkThereAreStorageDetailsToProcessOrThrowException();
    checkValidQuantitiesOrThrowException();
  }

  private void checkThereAreStorageDetailsToProcessOrThrowException() {
    if (selectedStorageDetails == null || selectedStorageDetails.length() == 0) {
      throw new OBException(OBMessageUtils.messageBD("NotSelected"));
    }
  }

  private void checkValidQuantitiesOrThrowException() throws JSONException {
    for (int i = 0; i < selectedStorageDetails.length(); i++) {
      final JSONObject storageDetailJS = selectedStorageDetails.getJSONObject(i);
      final StorageDetail storageDetail = getStorageDetail(storageDetailJS);
      final BigDecimal qtySelected = getSelectedQty(storageDetailJS);
      checkIsPositiveQty(storageDetail, qtySelected);
      checkQtyIsLowerOrEqualThanQtyOnHand(storageDetail, qtySelected);
    }
  }

  private void checkIsPositiveQty(final StorageDetail storageDetail, final BigDecimal qtySelected) {
    if (!ReferencedInventoryUtil.isGreaterThanZero(qtySelected)) {
      throw new OBException(String.format(OBMessageUtils.messageBD("RefInv_NegativeQty"),
          storageDetail.getIdentifier()));
    }
  }

  private void checkQtyIsLowerOrEqualThanQtyOnHand(final StorageDetail storageDetail,
      final BigDecimal qtySelected) {
    final BigDecimal qtyOnHand = storageDetail.getQuantityOnHand();
    if (qtySelected.compareTo(qtyOnHand) > 0) {
      throw new OBException(String.format(
          OBMessageUtils.messageBD("RefInv_QtyGreaterThanOnHandQty"),
          FIN_Utility.formatNumber(qtySelected, "qty", "Relation"),
          FIN_Utility.formatNumber(qtyOnHand, "qty", "Relation"), storageDetail.getIdentifier()));
    }
  }

  /**
   * Creates, process and returns a goods movement with the referenced inventory change.
   * 
   * @throws Exception
   *           In case of exception, the transaction is rollback and the exception is thrown.
   * 
   */
  public InternalMovement createAndProcessGoodsMovement() throws Exception {
    try {
      OBContext.setAdminMode(true);
      final InternalMovement goodsMovementHeader = ReferencedInventoryUtil
          .createAndSaveGoodsMovementHeader(getReferencedInventoryOrganization(),
              getFixedInternalMovementName());
      createMovementLines(goodsMovementHeader);
      processGoodsMovement(goodsMovementHeader.getId());
      return goodsMovementHeader;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw e;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * For each storage detail that is going to be box/unboxed to a referenced inventory, it first
   * tries to create a goods movement line without any associated reservation. This is only possible
   * when there is enough quantity on hand not reserved yet to partially or fully fulfill the
   * box/unbox movement qty.
   * 
   * After that, if remaining quantity, then it tries to move an existing reservation if possible.
   * 
   * The process is exactly the same for boxing and unboxing.
   */
  private void createMovementLines(final InternalMovement goodsMovementHeader)
      throws JSONException {
    long lineNo = 10l;
    for (int i = 0; i < selectedStorageDetails.length(); i++) {
      final JSONObject storageDetailJS = selectedStorageDetails.getJSONObject(i);
      final StorageDetail storageDetail = getStorageDetail(storageDetailJS);
      final BigDecimal qtyMovement = getSelectedQty(storageDetailJS);
      final Locator newStorageBin = getNewStorageBin(storageDetailJS);

      final BigDecimal qtyMovedWithoutReservation = createAndSaveGoodsMovementLineWithoutReservation(
          goodsMovementHeader, storageDetail, qtyMovement, newStorageBin, lineNo);
      final BigDecimal remainingQty = qtyMovement.subtract(qtyMovedWithoutReservation);
      createAndSaveGoodsMovementLineWithReservation(goodsMovementHeader, storageDetail,
          remainingQty, newStorageBin, lineNo);
      lineNo = lineNo + 10l;
    }
  }

  private BigDecimal createAndSaveGoodsMovementLineWithoutReservation(
      final InternalMovement internalMovement, final StorageDetail storageDetail,
      final BigDecimal qtyMovement, final Locator newStorageBin, long lineNo) {
    final BigDecimal qtyOnHand = storageDetail.getQuantityOnHand();
    final BigDecimal qtyReserved = storageDetail.getReservedQty();
    final BigDecimal qtyOnHandNotReserved = qtyOnHand.subtract(qtyReserved);

    final BigDecimal qtyToMoveWithoutReservation = qtyMovement.min(qtyOnHandNotReserved);
    if (ReferencedInventoryUtil.isGreaterThanZero(qtyToMoveWithoutReservation)) {
      ReferencedInventoryUtil.createAndSaveMovementLine(internalMovement,
          qtyToMoveWithoutReservation, newStorageBin, getAttributeSetInstanceTo(storageDetail),
          lineNo, storageDetail, null);
    }

    return qtyToMoveWithoutReservation;
  }

  private void createAndSaveGoodsMovementLineWithReservation(
      final InternalMovement internalMovement, final StorageDetail storageDetail,
      final BigDecimal remainingQty, final Locator newStorageBin, final long lineNo) {
    if (ReferencedInventoryUtil.isGreaterThanZero(remainingQty)
        && ReferencedInventoryUtil.isGreaterThanZero(storageDetail.getReservedQty())) {
      BigDecimal remainingQtyToReleaseInReservations = remainingQty;
      final ScrollableResults reservationStockScroll = ReferencedInventoryUtil
          .getAvailableStockReservations(storageDetail, newStorageBin);
      try {
        while (reservationStockScroll.next()
            && ReferencedInventoryUtil.isGreaterThanZero(remainingQtyToReleaseInReservations)) {
          final ReservationStock reservationStock = (ReservationStock) reservationStockScroll
              .get()[0];
          final BigDecimal currentReservedQty = (BigDecimal) reservationStockScroll.get()[1];
          final BigDecimal qtyToMoveInThisReservation = remainingQtyToReleaseInReservations
              .min(currentReservedQty);

          ReferencedInventoryUtil.createAndSaveMovementLine(internalMovement,
              qtyToMoveInThisReservation, newStorageBin, getAttributeSetInstanceTo(storageDetail),
              lineNo, storageDetail, reservationStock.getReservation());

          remainingQtyToReleaseInReservations = remainingQtyToReleaseInReservations
              .subtract(qtyToMoveInThisReservation);
        }

        if (ReferencedInventoryUtil.isGreaterThanZero(remainingQtyToReleaseInReservations)) {
          throw new OBException(
              String.format(OBMessageUtils.messageBD("RefInventoryCannotReallocateAllQuantity"),
                  remainingQtyToReleaseInReservations));
        }
      } finally {
        reservationStockScroll.close();
      }
    }
  }

  protected StorageDetail getStorageDetail(final JSONObject storageDetailJS) throws JSONException {
    return OBDal.getInstance().get(StorageDetail.class, getStorageDetailId(storageDetailJS));
  }

  private String getStorageDetailId(JSONObject jsStorageDetail) throws JSONException {
    return jsStorageDetail.getString(JS_STORAGEDETAIL_ID);
  }

  private BigDecimal getSelectedQty(final JSONObject storageDetailJS) throws JSONException {
    return new BigDecimal(storageDetailJS.getString(QUANTITY));
  }

  private void processGoodsMovement(final String goodsMovementId) {
    final Process process = OBDal.getInstance().get(Process.class, M_MOVEMENT_POST_ID);
    final ProcessInstance pinstance = CallProcess.getInstance()
        .call(process, goodsMovementId, null);
    final OBError result = OBMessageUtils.getProcessInstanceMessage(pinstance);
    if (StringUtils.equals("Error", result.getType())) {
      throw new OBException(
          OBMessageUtils.messageBD("ErrorProcessingGoodMovement") + ": " + result.getMessage());
    } else {
      OBDal.getInstance().flush(); // Flush in admin mode
    }
  }

}
