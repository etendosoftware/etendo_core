package org.openbravo.event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.cost.LCReceipt;
import org.openbravo.model.materialmgmt.cost.LandedCost;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 * Observer class to validate duplicate receipt lines in the Landed Cost window.
 * This class listens to entity persistence events and checks for duplicate
 * entries to prevent multiple receipts from being associated with the same
 * Good Shipment Line.
 */
public class LandedCostDuplicateReceiptValidator extends EntityPersistenceEventObserver {

  /**
   * Array of entities that this observer will monitor.
   */
  private static final Entity[] entities = { ModelProvider.getInstance().getEntity(LCReceipt.ENTITY_NAME) };

  /**
   * Specifies the entities that this observer is interested in monitoring.
   *
   * @return an array of {@code Entity} objects that this observer will handle.
   */
  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  /**
   * Event handler for new receipts. Ensures that no duplicate receipt lines
   * exist in the Landed Cost window.
   *
   * @param event
   *     the {@link EntityNewEvent} triggered when a new receipt is created.
   */
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Entity entity = ModelProvider.getInstance().getEntity(LCReceipt.class);
    final Property landedCostProp = entity.getProperty(LCReceipt.PROPERTY_LANDEDCOST);
    final Property shipmentLineProp = entity.getProperty(LCReceipt.PROPERTY_GOODSSHIPMENTLINE);

    final LandedCost landedCost = (LandedCost) event.getCurrentState(landedCostProp);
    final ShipmentInOutLine shipmentLine = (ShipmentInOutLine) event.getCurrentState(shipmentLineProp);
    final LCReceipt currentReceipt = (LCReceipt) event.getTargetInstance();

    if(shipmentLine != null) {
      List<LCReceipt> receiptList = landedCost.getLandedCostReceiptList();
      receiptList.remove(currentReceipt);
      checkDuplicateReceipt(receiptList, shipmentLine);
    }
  }

  /**
   * Event handler for updating receipts. Ensures that no duplicate receipt lines
   * exist after modification in the Landed Cost window.
   *
   * @param event
   *     the {@link EntityUpdateEvent} triggered when a receipt is updated.
   */
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Entity entity = ModelProvider.getInstance().getEntity(LCReceipt.class);
    final Property landedCostProp = entity.getProperty(LCReceipt.PROPERTY_LANDEDCOST);
    final LandedCost landedCost = (LandedCost) event.getCurrentState(landedCostProp);
    final LCReceipt receipt = OBDal.getInstance().get(LCReceipt.class, event.getId());
    final ShipmentInOutLine shipmentLine = receipt.getGoodsShipmentLine();
    if(shipmentLine != null) {
      List<LCReceipt> receiptList = landedCost.getLandedCostReceiptList();
      receiptList.remove(receipt);
      checkDuplicateReceipt(receiptList, shipmentLine);
    }
  }

  /**
   * Validates whether a duplicate receipt line exists in the Landed Cost window.
   *
   * @param receiptList
   *     the list of existing receipts in the Landed Cost.
   * @param shipmentLine
   *     the Shipment In/Out Line to be validated.
   * @throws OBException
   *     if a duplicate receipt line is found.
   */
  private void checkDuplicateReceipt(List<LCReceipt> receiptList, ShipmentInOutLine shipmentLine) {
    for (LCReceipt receipt : receiptList) {
      ShipmentInOutLine shipmentInOutLine = receipt.getGoodsShipmentLine();
      if (shipmentInOutLine != null && shipmentInOutLine.equals(shipmentLine)) {
        throw new OBException(OBMessageUtils.messageBD("LandedCostDuplicateReceipt"));
      }
    }
  }
}
