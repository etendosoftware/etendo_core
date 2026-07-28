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
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.BaseCoreTest;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for {@link OBCurrencyUtils}.
 */
@DisplayName("OBCurrencyUtils")
public class OBCurrencyUtilsTest extends BaseCoreTest {

  @Test
  void testNullOrgId() {
    assertNull(OBCurrencyUtils.getOrgCurrency(null));
  }

  @Test
  void testBlankOrgId() {
    assertNull(OBCurrencyUtils.getOrgCurrency("   "));
  }

  @Test
  void testEmptyOrgId() {
    assertNull(OBCurrencyUtils.getOrgCurrency(""));
  }

  @Test
  void testOrgNotFound() {
    when(obDal.get(Organization.class, "UNKNOWN")).thenReturn(null);
    assertNull(OBCurrencyUtils.getOrgCurrency("UNKNOWN"));
  }

  @Test
  void testOrgWithCurrency() {
    Organization org = mock(Organization.class);
    Currency currency = mock(Currency.class);
    when(currency.getId()).thenReturn("102");
    when(org.getCurrency()).thenReturn(currency);
    when(obDal.get(Organization.class, "ORG1")).thenReturn(org);

    assertEquals("102", OBCurrencyUtils.getOrgCurrency("ORG1"));
  }

  @Test
  void testFallbackToLegalEntity() {
    Organization org = mock(Organization.class);
    when(org.getCurrency()).thenReturn(null);
    when(obDal.get(Organization.class, "ORG1")).thenReturn(org);

    Organization legalEntity = mock(Organization.class);
    Currency leCurrency = mock(Currency.class);
    when(leCurrency.getId()).thenReturn("USD");
    when(legalEntity.getCurrency()).thenReturn(leCurrency);
    when(orgStructureProvider.getLegalEntity(org)).thenReturn(legalEntity);

    assertEquals("USD", OBCurrencyUtils.getOrgCurrency("ORG1"));
  }

  @Test
  void testFallbackToClientBaseCurrency() {
    Organization org = mock(Organization.class);
    when(org.getCurrency()).thenReturn(null);
    when(org.getClient()).thenReturn(client);
    when(client.getId()).thenReturn("CLIENT1");
    when(obDal.get(Organization.class, "ORG1")).thenReturn(org);

    Organization legalEntity = mock(Organization.class);
    when(legalEntity.getCurrency()).thenReturn(null);
    when(orgStructureProvider.getLegalEntity(org)).thenReturn(legalEntity);

    try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
      utilityMock.when(() -> Utility.stringBaseCurrencyId(any(), anyString()))
          .thenReturn("EUR");
      assertEquals("EUR", OBCurrencyUtils.getOrgCurrency("ORG1"));
    }
  }

  @Test
  void testNoLegalEntityFallback() {
    Organization org = mock(Organization.class);
    when(org.getCurrency()).thenReturn(null);
    when(org.getClient()).thenReturn(client);
    when(client.getId()).thenReturn("CLIENT1");
    when(obDal.get(Organization.class, "ORG1")).thenReturn(org);
    when(orgStructureProvider.getLegalEntity(org)).thenReturn(null);

    try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
      utilityMock.when(() -> Utility.stringBaseCurrencyId(any(), anyString()))
          .thenReturn("GBP");
      assertEquals("GBP", OBCurrencyUtils.getOrgCurrency("ORG1"));
    }
  }
}
