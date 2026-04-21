/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.event;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.After;
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
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceDiscount;

/**
 * Unit tests for {@link InvoiceDiscountEventHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class InvoiceDiscountEventHandlerTest {

  private static final String LANG_CODE = "en_US";
  private static final String ERROR_MSG = "Document already processed";

  @Mock private EntityNewEvent newEvent;
  @Mock private InvoiceDiscount invoiceDiscount;
  @Mock private Invoice invoice;
  @Mock private OBContext obContext;
  @Mock private Language language;
  @Mock private ModelProvider modelProvider;
  @Mock private Entity entity;

  private MockedStatic<ModelProvider> staticModelProvider;
  private MockedStatic<OBContext> staticOBContext;
  private MockedStatic<Utility> staticUtility;

  /**
   * Initializes static mocks and common stub behaviour shared across all tests.
   */
  @Before
  public void setUp() {
    staticModelProvider = mockStatic(ModelProvider.class);
    staticOBContext = mockStatic(OBContext.class);
    staticUtility = mockStatic(Utility.class);

    staticModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
    lenient().when(modelProvider.getEntity(InvoiceDiscount.ENTITY_NAME)).thenReturn(entity);
    staticOBContext.when(OBContext::getOBContext).thenReturn(obContext);
    lenient().when(obContext.getLanguage()).thenReturn(language);
    lenient().when(language.getLanguage()).thenReturn(LANG_CODE);

    lenient().when(invoiceDiscount.getInvoice()).thenReturn(invoice);

    staticUtility.when(() -> Utility.messageBD(any(ConnectionProvider.class), eq("20501"), anyString()))
        .thenReturn(ERROR_MSG);
  }

  /**
   * Closes all static mocks opened in {@link #setUp()} to release resources.
   */
  @After
  public void tearDown() {
    staticModelProvider.close();
    staticOBContext.close();
    staticUtility.close();
  }

  /**
   * Saving a discount on a draft (unprocessed, unposted) invoice must succeed.
   */
  @Test
  public void testOnSaveAllowsDiscountOnDraftInvoice() {
    when(invoice.isProcessed()).thenReturn(false);
    when(invoice.getPosted()).thenReturn("N");
    when(newEvent.getTargetInstance()).thenReturn(invoiceDiscount);

    InvoiceDiscountEventHandler handler = createHandlerWithValidEvent();
    assertDoesNotThrow(() -> handler.onSave(newEvent));
  }

  /**
   * Saving a discount on a completed (processed) invoice must throw {@link OBException}.
   */
  @Test
  public void testOnSaveBlocksDiscountOnCompletedInvoice() {
    when(invoice.isProcessed()).thenReturn(true);
    lenient().when(invoice.getPosted()).thenReturn("N");
    when(newEvent.getTargetInstance()).thenReturn(invoiceDiscount);

    InvoiceDiscountEventHandler handler = createHandlerWithValidEvent();
    assertThrows(OBException.class, () -> handler.onSave(newEvent));
  }

  /**
   * Saving a discount on a posted invoice must throw {@link OBException}.
   */
  @Test
  public void testOnSaveBlocksDiscountOnPostedInvoice() {
    when(invoice.isProcessed()).thenReturn(false);
    when(invoice.getPosted()).thenReturn("Y");
    when(newEvent.getTargetInstance()).thenReturn(invoiceDiscount);

    InvoiceDiscountEventHandler handler = createHandlerWithValidEvent();
    assertThrows(OBException.class, () -> handler.onSave(newEvent));
  }

  /**
   * Saving a discount on an invoice that is both processed and posted must throw {@link OBException}.
   */
  @Test
  public void testOnSaveBlocksDiscountOnProcessedAndPostedInvoice() {
    when(invoice.isProcessed()).thenReturn(true);
    lenient().when(invoice.getPosted()).thenReturn("Y");
    when(newEvent.getTargetInstance()).thenReturn(invoiceDiscount);

    InvoiceDiscountEventHandler handler = createHandlerWithValidEvent();
    assertThrows(OBException.class, () -> handler.onSave(newEvent));
  }

  /**
   * When {@code getInvoice()} returns {@code null}, the defensive null check must
   * prevent any NPE and allow the save to proceed.
   */
  @Test
  public void testOnSaveAllowsDiscountWhenInvoiceIsNull() {
    when(invoiceDiscount.getInvoice()).thenReturn(null);
    when(newEvent.getTargetInstance()).thenReturn(invoiceDiscount);

    InvoiceDiscountEventHandler handler = createHandlerWithValidEvent();
    assertDoesNotThrow(() -> handler.onSave(newEvent));
  }

  /**
   * When {@code getPosted()} returns {@code null} and the invoice is not processed,
   * the null-safe comparison must not throw and the save must succeed.
   */
  @Test
  public void testOnSaveAllowsDiscountWhenPostedIsNull() {
    when(invoice.isProcessed()).thenReturn(false);
    when(invoice.getPosted()).thenReturn(null);
    when(newEvent.getTargetInstance()).thenReturn(invoiceDiscount);

    InvoiceDiscountEventHandler handler = createHandlerWithValidEvent();
    assertDoesNotThrow(() -> handler.onSave(newEvent));
  }

  /**
   * When triggers are disabled, {@code isValidEvent} returns {@code false} and the handler skips.
   */
  @Test
  public void testOnSaveSkipsWhenEventIsInvalid() {
    TriggerHandler mockTriggerHandler = mock(TriggerHandler.class);
    when(mockTriggerHandler.isDisabled()).thenReturn(true);
    try (MockedStatic<TriggerHandler> staticTrigger = mockStatic(TriggerHandler.class)) {
      staticTrigger.when(TriggerHandler::getInstance).thenReturn(mockTriggerHandler);
      InvoiceDiscountEventHandler handler = new InvoiceDiscountEventHandler();
      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * {@code getObservedEntities()} must return a non-{@code null} array.
   */
  @Test
  public void testGetObservedEntitiesReturnsNonNull() {
    InvoiceDiscountEventHandler handler = new InvoiceDiscountEventHandler();
    assertNotNull(handler.getObservedEntities());
  }

  private InvoiceDiscountEventHandler createHandlerWithValidEvent() {
    return new InvoiceDiscountEventHandler() {
      @Override
      protected boolean isValidEvent(EntityPersistenceEvent event) {
        return true;
      }
    };
  }
}
