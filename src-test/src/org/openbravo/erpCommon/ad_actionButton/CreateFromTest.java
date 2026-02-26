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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;

import javax.servlet.ServletException;

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

  @Mock
  private Connection conn;

  @Mock
  private VariablesSecureApp vars;

  /**
   * Verifies that {@code updateInvoiceAndBOMStructure(...)} updates the invoice and BOM structure
   * when the related shipment line identifier is {@code null}.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  void updateInvoiceAndBOMStructureShouldUpdateInvoiceWhenInOutLineIdIsNull() throws Exception {
    CreateFrom createFrom = new CreateFrom();
    String sequence = "sequence-id";
    String key = "shipment-id";
    String invoiceLineId = "invoice-line-id";

    try (MockedStatic<CreateFromShipmentData> shipmentData = org.mockito.Mockito.mockStatic(
        CreateFromShipmentData.class)) {
      shipmentData.when(() -> CreateFromShipmentData.selectInvoiceInOut(conn, createFrom, invoiceLineId)).thenReturn(
          null);

      invokeUpdateInvoiceAndBOMStructure(createFrom, conn, vars, sequence, key, invoiceLineId);

      shipmentData.verify(() -> CreateFromShipmentData.selectInvoiceInOut(conn, createFrom, invoiceLineId), times(1));
      shipmentData.verify(() -> CreateFromShipmentData.updateInvoice(conn, createFrom, sequence, invoiceLineId),
          times(1));
      shipmentData.verify(() -> CreateFromShipmentData.updateBOMStructure(conn, createFrom, key, sequence), times(1));
      shipmentData.verify(() -> CreateFromShipmentData.insertMatchSI(any(), any(), any(), any(), any()), times(0));
      shipmentData.verifyNoMoreInteractions();
      verify(vars, times(0)).getUser();
    }
  }

  /**
   * Verifies that {@code updateInvoiceAndBOMStructure(...)} updates the invoice and BOM structure
   * when the related shipment line identifier is empty.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  void updateInvoiceAndBOMStructureShouldUpdateInvoiceWhenInOutLineIdIsEmpty() throws Exception {
    CreateFrom createFrom = new CreateFrom();
    String sequence = "sequence-id";
    String key = "shipment-id";
    String invoiceLineId = "invoice-line-id";

    try (MockedStatic<CreateFromShipmentData> shipmentData = org.mockito.Mockito.mockStatic(
        CreateFromShipmentData.class)) {
      shipmentData.when(() -> CreateFromShipmentData.selectInvoiceInOut(conn, createFrom, invoiceLineId)).thenReturn(
          "");

      invokeUpdateInvoiceAndBOMStructure(createFrom, conn, vars, sequence, key, invoiceLineId);

      shipmentData.verify(() -> CreateFromShipmentData.selectInvoiceInOut(conn, createFrom, invoiceLineId), times(1));
      shipmentData.verify(() -> CreateFromShipmentData.updateInvoice(conn, createFrom, sequence, invoiceLineId),
          times(1));
      shipmentData.verify(() -> CreateFromShipmentData.updateBOMStructure(conn, createFrom, key, sequence), times(1));
      shipmentData.verify(() -> CreateFromShipmentData.insertMatchSI(any(), any(), any(), any(), any()), times(0));
      shipmentData.verifyNoMoreInteractions();
      verify(vars, times(0)).getUser();
    }
  }

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
    String sequence = "sequence-id";
    String key = "shipment-id";
    String invoiceLineId = "invoice-line-id";
    String inOutLineId = "inout-line-id";
    String userId = "user-id";
    when(vars.getUser()).thenReturn(userId);

    try (MockedStatic<CreateFromShipmentData> shipmentData = org.mockito.Mockito.mockStatic(
        CreateFromShipmentData.class)) {
      shipmentData.when(() -> CreateFromShipmentData.selectInvoiceInOut(conn, createFrom, invoiceLineId)).thenReturn(
          inOutLineId);

      invokeUpdateInvoiceAndBOMStructure(createFrom, conn, vars, sequence, key, invoiceLineId);

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
   * Invokes {@code updateInvoiceAndBOMStructure(...)} via reflection using the expected method signature.
   * <p>
   * If the invoked method throws a {@link ServletException}, it is propagated to the caller.
   *
   * @param createFrom
   *     the {@link CreateFrom} instance that contains the method to invoke
   * @param conn
   *     the database connection passed to the invoked method
   * @param vars
   *     the application context passed to the invoked method
   * @param sequence
   *     the sequence identifier passed to the invoked method
   * @param key
   *     the key passed to the invoked method
   * @param invoiceLineId
   *     the invoice line identifier passed to the invoked method
   * @throws Exception
   *     if an error occurs during test execution
   * @throws ServletException
   *     if an error occurs while invoking the target method
   */
  private void invokeUpdateInvoiceAndBOMStructure(CreateFrom createFrom, Connection conn, VariablesSecureApp vars,
      String sequence, String key, String invoiceLineId) throws Exception {
    Method method = CreateFrom.class.getDeclaredMethod("updateInvoiceAndBOMStructure", Connection.class,
        VariablesSecureApp.class, String.class, String.class, String.class);
    method.setAccessible(true);
    try {
      method.invoke(createFrom, conn, vars, sequence, key, invoiceLineId);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof ServletException) {
        throw (ServletException) e.getCause();
      }
      throw e;
    }
  }
}
