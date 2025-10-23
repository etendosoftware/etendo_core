package org.openbravo.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
import org.openbravo.base.model.Property;
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
  private static final String DOCSTATUS_DR = "DR";

  @Mock private EntityUpdateEvent updateEvent;
  @Mock private InvoiceTax invoiceTax;
  @Mock private Invoice invoice;
  @Mock private Currency currency;
  @Mock private ModelProvider modelProvider;
  @Mock private Entity entity;
  @Mock private Property pTaxAmt;
  @Mock private Property pOrigAmt;

  /**
   * Common stubbing for invoice/tax/currency used by tests.
   */
  @Before
  public void setUp() {
    lenient().when(invoiceTax.getInvoice()).thenReturn(invoice);
    lenient().when(invoice.getCurrency()).thenReturn(currency);
    lenient().when(currency.getPricePrecision()).thenReturn(2L);
    lenient().when(invoice.getDocumentStatus()).thenReturn(DOCSTATUS_DR);
  }

  /**
   * Builds a handler that always considers the event valid by overriding
   * @return a handler with validation forcibly enabled
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
   * Ensures no validation is performed when the invoice is not in draft (docstatus != DR).
   */
  @Test
  public void testOnUpdateNotDraftNoValidation() {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(InvoiceTax.ENTITY_NAME)).thenReturn(entity);
      when(invoice.getDocumentStatus()).thenReturn("CO");
      when(updateEvent.getTargetInstance()).thenReturn(invoiceTax);
      InvoiceTaxAmountEventHandler handler = buildHandlerAlwaysValid();
      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
    }
  }

  /**
   * Verifies that a delta exactly equal to 0.01 (after rounding to currency precision)
   * is allowed and does not raise an exception.
   */
  @Test
  public void testOnUpdateToleranceBoundaryNoException() {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(InvoiceTax.ENTITY_NAME)).thenReturn(entity);
      when(invoice.getDocumentStatus()).thenReturn(DOCSTATUS_DR);
      when(invoiceTax.getOriginalTaxAmount()).thenReturn(new BigDecimal("100.00"));
      when(invoiceTax.getTaxAmount()).thenReturn(new BigDecimal("100.01"));
      when(updateEvent.getTargetInstance()).thenReturn(invoiceTax);
      InvoiceTaxAmountEventHandler handler = buildHandlerAlwaysValid();
      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
    }
  }

  /**
   * Verifies that a delta greater than 0.01 triggers an {@link OBException}
   * with the expected message key.
   */
  @Test
  public void testOnUpdateExceedsToleranceThrows() {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class);
         MockedStatic<OBMessageUtils> msg = mockStatic(OBMessageUtils.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(InvoiceTax.ENTITY_NAME)).thenReturn(entity);
      msg.when(() -> OBMessageUtils.messageBD(anyString())).thenReturn("TaxAdjOutOfRange");
      when(invoice.getDocumentStatus()).thenReturn(DOCSTATUS_DR);
      when(invoiceTax.getOriginalTaxAmount()).thenReturn(new BigDecimal("10.01"));
      when(invoiceTax.getTaxAmount()).thenReturn(new BigDecimal("10.03"));
      when(updateEvent.getTargetInstance()).thenReturn(invoiceTax);
      InvoiceTaxAmountEventHandler handler = buildHandlerAlwaysValid();
      assertThrows(OBException.class, () -> handler.onUpdate(updateEvent));
    }
  }

  /**
   * Verifies that null {@code TaxAmount} and/or {@code OriginalTaxAmount} are treated as zero,
   * resulting in a delta of zero and no exception.
   */
  @Test
  public void testOnUpdateNullsAsZeroNoException() {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(InvoiceTax.ENTITY_NAME)).thenReturn(entity);
      when(invoice.getDocumentStatus()).thenReturn(DOCSTATUS_DR);
      when(invoiceTax.getOriginalTaxAmount()).thenReturn(null);
      when(invoiceTax.getTaxAmount()).thenReturn(null);
      when(updateEvent.getTargetInstance()).thenReturn(invoiceTax);
      when(entity.getProperty(InvoiceTax.PROPERTY_TAXAMOUNT)).thenReturn(pTaxAmt);
      when(entity.getProperty(InvoiceTax.PROPERTY_ORIGINALTAXAMOUNT)).thenReturn(pOrigAmt);
      InvoiceTaxAmountEventHandler handler = buildHandlerAlwaysValid();
      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
    }
  }

  /**
   * Verifies that when {@code OriginalTaxAmount} is zero, the handler initializes it with
   * the previous {@code TaxAmount} value obtained from {@link EntityUpdateEvent#getPreviousState(Property)}.
   */
  @Test
  public void testOnUpdateInitOrigFromPreviousTaxAmt() {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntity(InvoiceTax.ENTITY_NAME)).thenReturn(entity);
      when(invoice.getDocumentStatus()).thenReturn(DOCSTATUS_DR);
      when(invoiceTax.getOriginalTaxAmount()).thenReturn(BigDecimal.ZERO);
      when(invoiceTax.getTaxAmount()).thenReturn(new BigDecimal("0.33"));
      when(updateEvent.getTargetInstance()).thenReturn(invoiceTax);
      when(entity.getProperty(InvoiceTax.PROPERTY_TAXAMOUNT)).thenReturn(pTaxAmt);
      when(entity.getProperty(InvoiceTax.PROPERTY_ORIGINALTAXAMOUNT)).thenReturn(pOrigAmt);
      when(updateEvent.getPreviousState(pTaxAmt)).thenReturn(new BigDecimal("0.32"));
      InvoiceTaxAmountEventHandler handler = buildHandlerAlwaysValid();
      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
      verify(updateEvent).setCurrentState(pOrigAmt, new BigDecimal("0.32"));
    }
  }
}
