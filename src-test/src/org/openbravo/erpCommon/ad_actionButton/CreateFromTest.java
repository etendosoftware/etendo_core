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
package org.openbravo.erpCommon.ad_actionButton;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * Unit tests for {@link CreateFrom}.
 */
@ExtendWith(MockitoExtension.class)
public class CreateFromTest {
  private static final String SEQUENCE_ID = "sequence-id";
  private static final String SHIPMENT_ID = "shipment-id";
  private static final String INVOICE_ID = "invoice-id";
  private static final String USER_ID = "user-id";
  private static final String INVOICE_LINE_ID = "invoice-line-id";
  private static final String ORDER_LINE_ID = "order-line-id";

  @Mock
  private Connection conn;

  @Mock
  private VariablesSecureApp vars;

  /**
   * Verifies that {@code updateInvoiceAndBOMStructure(...)} inserts a match record and updates the BOM structure
   * when a related shipment line identifier exists.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  void updateInvoiceAndBOMStructureShouldInsertMatchSIWhenInOutLineIdExists() throws Exception {
    CreateFrom createFrom = new CreateFrom();
    String sequence = SEQUENCE_ID;
    String key = SHIPMENT_ID;
    String invoiceLineId = INVOICE_LINE_ID;
    String inOutLineId = "inout-line-id";
    String userId = USER_ID;
    when(vars.getUser()).thenReturn(userId);

    try (MockedStatic<CreateFromShipmentData> shipmentData = mockStatic(CreateFromShipmentData.class)) {
      shipmentData.when(() -> CreateFromShipmentData.selectInvoiceInOut(conn, createFrom, invoiceLineId)).thenReturn(
          inOutLineId);

      createFrom.updateInvoiceAndBOMStructure(conn, vars, sequence, key, INVOICE_ID, invoiceLineId, ORDER_LINE_ID);

      shipmentData.verify(() -> CreateFromShipmentData.selectInvoiceInOut(conn, createFrom, invoiceLineId), times(1));
      shipmentData.verify(() -> CreateFromShipmentData.insertMatchSI(conn, createFrom, userId, invoiceLineId, sequence),
          times(1));
      shipmentData.verify(() -> CreateFromShipmentData.updateBOMStructure(conn, createFrom, key, sequence), times(1));
      shipmentData.verify(() -> CreateFromShipmentData.updateInvoice(any(), any(), any(), any()), times(0));
      shipmentData.verifyNoMoreInteractions();
      verify(vars, times(1)).getUser();
    }
  }

  /**
   * Verifies that {@code updateInvoiceAndBOMStructure(...)} updates the order when the source
   * invoice identifier is empty.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  void updateInvoiceAndBOMStructureShouldUpdateInvoiceOrderWhenInvoiceIsEmpty() throws Exception {
    CreateFrom createFrom = new CreateFrom();
    String sequence = SEQUENCE_ID;
    String orderLineId = ORDER_LINE_ID;

    try (MockedStatic<CreateFromShipmentData> shipmentData = mockStatic(CreateFromShipmentData.class)) {
      createFrom.updateInvoiceAndBOMStructure(conn, vars, sequence, SHIPMENT_ID, "", INVOICE_LINE_ID, orderLineId);

      shipmentData.verify(() -> CreateFromShipmentData.updateInvoiceOrder(conn, createFrom, sequence, orderLineId),
          times(1));
      shipmentData.verifyNoMoreInteractions();
      verify(vars, times(0)).getUser();
    }
  }

  /**
   * Verifies that {@code updateInvoiceAndBOMStructure(...)} updates the invoice line
   * when no related shipment line identifier is found (null).
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  void updateInvoiceAndBOMStructureShouldUpdateInvoiceWhenInOutLineIdIsNull() throws Exception {
    CreateFrom createFrom = new CreateFrom();
    String sequence = SEQUENCE_ID;
    String key = SHIPMENT_ID;
    String invoiceLineId = INVOICE_LINE_ID;
    String userId = USER_ID;
    when(vars.getUser()).thenReturn(userId);

    try (MockedStatic<CreateFromShipmentData> shipmentData = mockStatic(CreateFromShipmentData.class)) {
      shipmentData.when(() -> CreateFromShipmentData.selectInvoiceInOut(conn, createFrom, invoiceLineId)).thenReturn(
          null);

      createFrom.updateInvoiceAndBOMStructure(conn, vars, sequence, key, INVOICE_ID, invoiceLineId, ORDER_LINE_ID);

      shipmentData.verify(() -> CreateFromShipmentData.selectInvoiceInOut(conn, createFrom, invoiceLineId), times(1));
      shipmentData.verify(() -> CreateFromShipmentData.updateInvoice(conn, createFrom, sequence, invoiceLineId),
          times(1));
      shipmentData.verify(() -> CreateFromShipmentData.insertMatchSI(conn, createFrom, userId, invoiceLineId, sequence),
          times(1));
      shipmentData.verify(() -> CreateFromShipmentData.updateBOMStructure(conn, createFrom, key, sequence), times(1));
      shipmentData.verifyNoMoreInteractions();
      verify(vars, times(1)).getUser();
    }
  }

  /**
   * Verifies that {@code updateInvoiceAndBOMStructure(...)} updates the invoice line
   * when the related shipment line identifier is empty.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  void updateInvoiceAndBOMStructureShouldUpdateInvoiceWhenInOutLineIdIsEmpty() throws Exception {
    CreateFrom createFrom = new CreateFrom();
    String sequence = SEQUENCE_ID;
    String key = SHIPMENT_ID;
    String invoiceLineId = INVOICE_LINE_ID;
    String userId = USER_ID;
    when(vars.getUser()).thenReturn(userId);

    try (MockedStatic<CreateFromShipmentData> shipmentData = mockStatic(CreateFromShipmentData.class)) {
      shipmentData.when(() -> CreateFromShipmentData.selectInvoiceInOut(conn, createFrom, invoiceLineId)).thenReturn(
          "");

      createFrom.updateInvoiceAndBOMStructure(conn, vars, sequence, key, INVOICE_ID, invoiceLineId, ORDER_LINE_ID);

      shipmentData.verify(() -> CreateFromShipmentData.selectInvoiceInOut(conn, createFrom, invoiceLineId), times(1));
      shipmentData.verify(() -> CreateFromShipmentData.updateInvoice(conn, createFrom, sequence, invoiceLineId),
          times(1));
      shipmentData.verify(() -> CreateFromShipmentData.insertMatchSI(conn, createFrom, userId, invoiceLineId, sequence),
          times(1));
      shipmentData.verify(() -> CreateFromShipmentData.updateBOMStructure(conn, createFrom, key, sequence), times(1));
      shipmentData.verifyNoMoreInteractions();
      verify(vars, times(1)).getUser();
    }
  }
}
