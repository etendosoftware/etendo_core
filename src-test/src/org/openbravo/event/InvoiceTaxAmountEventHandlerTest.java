package org.openbravo.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceTax;

/**
 * Unit tests for {@link InvoiceTaxAmountEventHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class InvoiceTaxAmountEventHandlerTest {

  @Mock private EntityNewEvent newEvent;
  @Mock private EntityUpdateEvent updateEvent;
  @Mock private InvoiceTax invoiceTax;
  @Mock private Invoice invoice;
  @Mock private Currency currency;
  @Mock private ModelProvider modelProvider;
  @Mock private Entity entity;

  /**
   * Initializes common mocks before each test.
   */
  @Before
  public void setUp() {
    lenient().when(invoiceTax.getInvoice()).thenReturn(invoice);
    lenient().when(invoice.getCurrency()).thenReturn(currency);
    lenient().when(currency.getPricePrecision()).thenReturn(2L);
    lenient().when(invoice.getDocumentStatus()).thenReturn("CO");
  }

  /**
   * Builds an {@link InvoiceTaxAmountEventHandler} instance whose
   * @return a handler with {@code isValidEvent} overridden to always validate.
   */
  private InvoiceTaxAmountEventHandler buildHandlerAlwaysValid() {
    return new InvoiceTaxAmountEventHandler() {
      @Override
      protected boolean isValidEvent(EntityPersistenceEvent event) {
        return true;
      }
    };
  }

  /**
   * Verifies that when the invoice is not completed (docStatus != "CO"),
   * the handler performs no validation and therefore does not throw an exception.
   */
  @Test
  public void testOnSaveNotCompletedNoValidation() {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(InvoiceTax.ENTITY_NAME)).thenReturn(entity);
      when(invoice.getDocumentStatus()).thenReturn("DR");
      when(newEvent.getTargetInstance()).thenReturn(invoiceTax);
      InvoiceTaxAmountEventHandler handler = buildHandlerAlwaysValid();
      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Verifies that a delta equal to 0.01 (after rounding to currency price precision)
   * is accepted and does not raise an exception.
   */
  @Test
  public void testOnSaveToleranceBoundaryNoException() {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(InvoiceTax.ENTITY_NAME)).thenReturn(entity);
      when(invoice.getDocumentStatus()).thenReturn("CO");
      when(invoiceTax.getOriginalTaxAmount()).thenReturn(new BigDecimal("100.00"));
      when(invoiceTax.getTaxAmount()).thenReturn(new BigDecimal("100.01"));
      when(newEvent.getTargetInstance()).thenReturn(invoiceTax);
      InvoiceTaxAmountEventHandler handler = buildHandlerAlwaysValid();
      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Verifies that a delta greater than 0.01 causes the handler to throw an {@link OBException}.
   */
  @Test
  public void testOnSaveExceedsToleranceThrows() {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class);
         MockedStatic<OBMessageUtils> msg = mockStatic(OBMessageUtils.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(InvoiceTax.ENTITY_NAME)).thenReturn(entity);
      msg.when(() -> OBMessageUtils.messageBD(anyString())).thenReturn("TaxAdjOutOfRange");
      when(invoice.getDocumentStatus()).thenReturn("CO");
      when(invoiceTax.getOriginalTaxAmount()).thenReturn(new BigDecimal("10.01"));
      when(invoiceTax.getTaxAmount()).thenReturn(new BigDecimal("10.03"));
      when(newEvent.getTargetInstance()).thenReturn(invoiceTax);
      InvoiceTaxAmountEventHandler handler = buildHandlerAlwaysValid();
      assertThrows(OBException.class, () -> handler.onSave(newEvent));
    }
  }

  /**
   * Verifies that null amounts are treated as zero for both original and actual tax amounts,
   * which results in a delta of zero and therefore no exception.
   */
  @Test
  public void testOnSaveNullsAsZeroNoException() {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(InvoiceTax.ENTITY_NAME)).thenReturn(entity);
      when(invoice.getDocumentStatus()).thenReturn("CO");
      when(invoiceTax.getOriginalTaxAmount()).thenReturn(null);
      when(invoiceTax.getTaxAmount()).thenReturn(null);
      when(newEvent.getTargetInstance()).thenReturn(invoiceTax);
      InvoiceTaxAmountEventHandler handler = buildHandlerAlwaysValid();
      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Verifies the same tolerance behavior when updating an existing {@link InvoiceTax}:
   * a delta equal to or below 0.01 must not raise an exception.
   */
  @Test
  public void testOnUpdateWithinToleranceNoException() {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(InvoiceTax.ENTITY_NAME)).thenReturn(entity);
      when(invoice.getDocumentStatus()).thenReturn("CO");
      when(invoiceTax.getOriginalTaxAmount()).thenReturn(new BigDecimal("50.00"));
      when(invoiceTax.getTaxAmount()).thenReturn(new BigDecimal("50.01"));
      when(updateEvent.getTargetInstance()).thenReturn(invoiceTax);
      InvoiceTaxAmountEventHandler handler = buildHandlerAlwaysValid();
      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
    }
  }
}