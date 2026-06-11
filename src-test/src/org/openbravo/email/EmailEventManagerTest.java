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
package org.openbravo.email;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.etendoerp.email.spi.EmailSenderDispatcher;

/**
 * Unit tests for {@link EmailEventManager}, focused on the SMTP configuration pre-check:
 * with no SMTP configuration at any level, the send is rejected only when no alternative
 * {@link com.etendoerp.email.spi.EmailSender} is configured either.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EmailEventManagerTest {

  private static final String EVENT = "testEvent";
  private static final String RECIPIENT = "recipient@example.com";
  private static final String NOT_FOUND_MESSAGE = "Email configuration not found";

  private MockedStatic<SmtpCascadeResolver> mockedResolver;
  private MockedStatic<EmailSenderDispatcher> mockedDispatcher;
  private MockedStatic<OBMessageUtils> mockedMessageUtils;

  private EmailEventManager manager;

  /**
   * Sets up the manager with an empty generator set and the static mocks for the resolver,
   * the dispatcher and the message utilities.
   * @throws Exception if reflection-based injection fails
   */
  @BeforeEach
  void setUp() throws Exception {
    mockedResolver = mockStatic(SmtpCascadeResolver.class);
    mockedDispatcher = mockStatic(EmailSenderDispatcher.class);
    mockedMessageUtils = mockStatic(OBMessageUtils.class);
    mockedMessageUtils.when(() -> OBMessageUtils.getI18NMessage(any(), any()))
        .thenReturn(NOT_FOUND_MESSAGE);

    manager = new EmailEventManager();
    @SuppressWarnings("unchecked")
    Instance<EmailEventContentGenerator> emptyGenerators = mock(Instance.class);
    when(emptyGenerators.iterator()).thenReturn(Collections.emptyIterator());
    Field generatorsField = EmailEventManager.class.getDeclaredField("emailGenerators");
    generatorsField.setAccessible(true);
    generatorsField.set(manager, emptyGenerators);
  }

  /**
   * Closes the static mocks after each test to prevent leaks.
   */
  @AfterEach
  void tearDown() {
    mockedResolver.close();
    mockedDispatcher.close();
    mockedMessageUtils.close();
  }

  /**
   * Verifies that with no SMTP configuration and no alternative sender configured, the
   * send is rejected upfront with an {@link EmailEventException}, preserving the historical
   * behavior when no email transport exists at all.
   */
  @Test
  void testSendEmailThrowsWhenNoConfigAndNoAlternativeSender() {
    mockedResolver.when(SmtpCascadeResolver::resolve).thenReturn(null);
    mockedDispatcher.when(EmailSenderDispatcher::hasAlternativeSenderConfigured)
        .thenReturn(false);

    assertThrows(EmailEventException.class,
        () -> manager.sendEmail(EVENT, RECIPIENT, new Object()));
  }

  /**
   * Verifies that with no SMTP configuration but an alternative sender configured, the
   * send proceeds past the configuration pre-check (returning {@code false} here only
   * because no content generator listens to the event).
   * @throws EmailEventException never expected in this test
   */
  @Test
  void testSendEmailProceedsWhenAlternativeSenderConfigured() throws EmailEventException {
    mockedResolver.when(SmtpCascadeResolver::resolve).thenReturn(null);
    mockedDispatcher.when(EmailSenderDispatcher::hasAlternativeSenderConfigured)
        .thenReturn(true);

    boolean sent = manager.sendEmail(EVENT, RECIPIENT, new Object());

    assertFalse(sent);
  }
}
