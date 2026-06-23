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
package com.etendoerp.email.spi;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.dal.core.OBContext;
import org.openbravo.email.ResolvedSmtpConfig;
import org.openbravo.erpCommon.utility.poc.EmailInfo;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for {@link EmailSendContext}: builder population and the
 * {@link EmailSendContext#create} factory, which must populate client/organization from the
 * current {@link OBContext} and tolerate its absence or failure.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EmailSendContextTest {

  /**
   * Verifies that the builder carries every field as provided.
   */
  @Test
  void testBuilderPopulatesAllFields() {
    Client client = mock(Client.class);
    Organization organization = mock(Organization.class);
    EmailServerConfiguration smtpConfig = mock(EmailServerConfiguration.class);
    ResolvedSmtpConfig resolvedConfig = mock(ResolvedSmtpConfig.class);
    EmailInfo email = new EmailInfo.Builder().build();

    EmailSendContext context = new EmailSendContext.Builder()
        .setClient(client)
        .setOrganization(organization)
        .setSmtpConfig(smtpConfig)
        .setResolvedSmtpConfig(resolvedConfig)
        .setEmail(email)
        .build();

    assertSame(client, context.getClient());
    assertSame(organization, context.getOrganization());
    assertSame(smtpConfig, context.getSmtpConfig());
    assertSame(resolvedConfig, context.getResolvedSmtpConfig());
    assertSame(email, context.getEmail());
  }

  /**
   * Verifies that {@link EmailSendContext#create} populates client and organization from
   * the current {@link OBContext} together with the provided configuration and email.
   */
  @Test
  void testCreatePopulatesClientAndOrganizationFromOBContext() {
    Client client = mock(Client.class);
    Organization organization = mock(Organization.class);
    OBContext obContext = mock(OBContext.class);
    when(obContext.getCurrentClient()).thenReturn(client);
    when(obContext.getCurrentOrganization()).thenReturn(organization);
    ResolvedSmtpConfig resolvedConfig = mock(ResolvedSmtpConfig.class);
    EmailInfo email = new EmailInfo.Builder().build();

    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {
      mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);

      EmailSendContext context = EmailSendContext.create(null, resolvedConfig, email);

      assertSame(client, context.getClient());
      assertSame(organization, context.getOrganization());
      assertNull(context.getSmtpConfig());
      assertSame(resolvedConfig, context.getResolvedSmtpConfig());
      assertSame(email, context.getEmail());
    }
  }

  /**
   * Verifies that {@link EmailSendContext#create} leaves client and organization empty when
   * no {@link OBContext} is available (e.g. background threads), instead of failing.
   */
  @Test
  void testCreateToleratesMissingOBContext() {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {
      mockedOBContext.when(OBContext::getOBContext).thenReturn(null);

      EmailSendContext context = EmailSendContext.create(null, null, null);

      assertNull(context.getClient());
      assertNull(context.getOrganization());
    }
  }

  /**
   * Verifies that {@link EmailSendContext#create} treats a failure reading the
   * {@link OBContext} as missing context information instead of propagating the error,
   * so context building can never break a send.
   */
  @Test
  void testCreateToleratesOBContextFailure() {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class)) {
      mockedOBContext.when(OBContext::getOBContext)
          .thenThrow(new IllegalStateException("context unavailable"));

      EmailSendContext context = EmailSendContext.create(null, null, null);

      assertNull(context.getClient());
      assertNull(context.getOrganization());
    }
  }
}
