package org.openbravo.event;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceTax;

/**
 * Entity persistence observer that validates manual tax adjustments on {@link InvoiceTax}
 * records when their parent {@link Invoice} is draft (document status {@code "DR"}).
 */
@ApplicationScoped
public class InvoiceTaxAmountEventHandler extends EntityPersistenceEventObserver {
  
  private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
  
  private static final Entity[] ENTITIES = {
    ModelProvider.getInstance().getEntity(InvoiceTax.ENTITY_NAME)
  };

  /**
   * Returns the entities observed by this observer.
   * @return the array containing the {@link InvoiceTax} entity
   */
  @Override
  protected Entity[] getObservedEntities() { 
    return ENTITIES; 
  }
  
  /**
   * Reacts to {@link EntityUpdateEvent} for {@link InvoiceTax} and triggers validation.
   * @param e the update-entity persistence event
   */
  public void onUpdate(@Observes EntityUpdateEvent e){ 
    validate(e); 
  }

  /**
   * Validates that, for draft invoices (document status {@code "DR"}), the absolute
   * difference between the current tax amount and the original tax amount does not exceed
   * {@code 0.01} in the document currency.
   * @param event the persistence event (new or update) containing the target {@link InvoiceTax}
   * @throws OBException if the tax adjustment is out of the allowed range
   */
  protected void validate(EntityPersistenceEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    InvoiceTax it = (InvoiceTax) event.getTargetInstance();
    Invoice inv = it.getInvoice();
    if (inv == null || !StringUtils.equals(inv.getDocumentStatus(), "DR")) {
      return;
    }
    int scale = (inv.getCurrency() != null && inv.getCurrency().getPricePrecision() != null) ? inv.getCurrency().getPricePrecision().intValue() : 2;
    BigDecimal amt = it.getTaxAmount() == null ? BigDecimal.ZERO : it.getTaxAmount().setScale(scale, RoundingMode.HALF_UP);
    BigDecimal orig = it.getOriginalTaxAmount() == null ? BigDecimal.ZERO : it.getOriginalTaxAmount().setScale(scale, RoundingMode.HALF_UP);
    if (orig.compareTo(BigDecimal.ZERO) == 0) {
      EntityUpdateEvent entityUpdateEvent = (EntityUpdateEvent) event;
      Entity entity = ModelProvider.getInstance().getEntity(InvoiceTax.ENTITY_NAME);
      org.openbravo.base.model.Property pTaxAmt = entity.getProperty(InvoiceTax.PROPERTY_TAXAMOUNT);
      org.openbravo.base.model.Property pOrig = entity.getProperty(InvoiceTax.PROPERTY_ORIGINALTAXAMOUNT);
      BigDecimal prevAmt = (BigDecimal) entityUpdateEvent.getPreviousState(pTaxAmt);
      BigDecimal baseForOrig = (prevAmt == null) ? amt : prevAmt.setScale(scale, RoundingMode.HALF_UP);
      event.setCurrentState(pOrig, baseForOrig);
      orig = baseForOrig;
    }
    BigDecimal delta = amt.subtract(orig).abs();
    if (delta.compareTo(TOLERANCE) > 0) {
      throw new OBException(OBMessageUtils.messageBD("TaxAdjOutOfRange"));
    }
  }
}
