package org.openbravo.event;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceTax;

/**
 * Entity persistence observer that validates manual tax adjustments on {@link InvoiceTax}
 * records when their parent {@link Invoice} is completed (document status {@code "CO"}).
 */
@ApplicationScoped
public class InvoiceTaxAmountEventHandler extends EntityPersistenceEventObserver {

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
   * Reacts to {@link EntityNewEvent} for {@link InvoiceTax} and triggers validation.
   * @param e the new-entity persistence event
   */
  public void onSave(@Observes EntityNewEvent e)    { 
    validate(e); 
  }

  /**
   * Reacts to {@link EntityUpdateEvent} for {@link InvoiceTax} and triggers validation.
   * @param e the update-entity persistence event
   */
  public void onUpdate(@Observes EntityUpdateEvent e){ 
    validate(e); 
  }

  /**
   * Validates that, for completed invoices (document status {@code "CO"}), the absolute
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
    if (inv == null) {
      return;
    }
    String docStatus = inv.getDocumentStatus();
    if (!StringUtils.equals("CO", docStatus)) {
      return;
    }
    int scale = 2;
    if (inv.getCurrency() != null && inv.getCurrency().getPricePrecision() != null) {
      scale = inv.getCurrency().getPricePrecision().intValue();
    }    
    BigDecimal amt = it.getTaxAmount() == null ? BigDecimal.ZERO : it.getTaxAmount().setScale(scale, RoundingMode.HALF_UP);
    BigDecimal orig = it.getOriginalTaxAmount(); 
    if (orig == null) orig = BigDecimal.ZERO;
    orig = orig.setScale(scale, RoundingMode.HALF_UP);
    BigDecimal delta = amt.subtract(orig).abs();
    if (delta.compareTo(new BigDecimal("0.01")) > 0) {
      throw new OBException(OBMessageUtils.messageBD("TaxAdjOutOfRange"));
    }
  }
}
