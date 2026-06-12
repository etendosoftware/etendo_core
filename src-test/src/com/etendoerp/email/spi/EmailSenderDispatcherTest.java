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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.base.weld.WeldUtils;

/**
 * Unit tests for {@link EmailSenderDispatcher}: candidate ordering by priority,
 * configuration-based filtering, guaranteed fallback to the default SMTP sender and
 * propagation of send failures.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EmailSenderDispatcherTest {

  /**
   * Configurable fake sender used to drive the selection logic in tests.
   */
  private static class FakeSender implements EmailSender {
    private final int priority;
    private final boolean configured;
    private boolean sendInvoked = false;

    FakeSender(int priority, boolean configured) {
      this.priority = priority;
      this.configured = configured;
    }

    @Override
    public boolean isConfigured(EmailSendContext context) {
      return configured;
    }

    @Override
    public void send(EmailSendContext context) {
      sendInvoked = true;
    }

    @Override
    public int getPriority() {
      return priority;
    }
  }

  private static EmailSendContext emptyContext() {
    return new EmailSendContext.Builder().build();
  }

  /**
   * Verifies the {@link EmailSender} interface contract: implementations that do not
   * override {@code getPriority} sit at the neutral priority {@code 0}, above the default
   * SMTP sender's floor.
   */
  @Test
  void testEmailSenderDefaultPriorityIsZero() {
    EmailSender minimal = new EmailSender() {
      @Override
      public boolean isConfigured(EmailSendContext context) {
        return true;
      }

      @Override
      public void send(EmailSendContext context) {
        // intentionally empty: this fake only exercises the selection logic, never the send
      }
    };

    assertEquals(0, minimal.getPriority());
  }

  /**
   * Verifies that the highest-priority configured sender is selected among multiple
   * candidates, including the default SMTP sender at the floor.
   */
  @Test
  void testSelectSenderPicksHighestPriorityConfigured() {
    FakeSender low = new FakeSender(10, true);
    FakeSender high = new FakeSender(100, true);
    List<EmailSender> candidates = Arrays.asList(low, new DefaultSmtpEmailSender(), high);

    EmailSender selected = EmailSenderDispatcher.selectSender(candidates, emptyContext());

    assertSame(high, selected);
  }

  /**
   * Verifies that a higher-priority sender that is not configured is skipped in favor of
   * the next configured candidate.
   */
  @Test
  void testSelectSenderSkipsUnconfiguredSender() {
    FakeSender notConfigured = new FakeSender(100, false);
    FakeSender configured = new FakeSender(10, true);
    List<EmailSender> candidates = Arrays.asList(notConfigured, configured);

    EmailSender selected = EmailSenderDispatcher.selectSender(candidates, emptyContext());

    assertSame(configured, selected);
  }

  /**
   * Verifies that the default SMTP sender is selected when no alternative sender is
   * configured, since it always reports itself as configured at the priority floor.
   */
  @Test
  void testSelectSenderFallsBackToDefaultWhenNoAlternativeConfigured() {
    FakeSender notConfigured = new FakeSender(100, false);
    DefaultSmtpEmailSender defaultSender = new DefaultSmtpEmailSender();
    List<EmailSender> candidates = Arrays.asList(notConfigured, defaultSender);

    EmailSender selected = EmailSenderDispatcher.selectSender(candidates, emptyContext());

    assertSame(defaultSender, selected);
  }

  /**
   * Verifies that selection never returns {@code null}: with an empty candidate list (e.g.
   * CDI unavailable) a default SMTP sender instance is created as fallback.
   */
  @Test
  void testSelectSenderReturnsDefaultOnEmptyCandidates() {
    EmailSender selected = EmailSenderDispatcher.selectSender(Collections.emptyList(),
        emptyContext());

    assertInstanceOf(DefaultSmtpEmailSender.class, selected);
  }

  /**
   * Verifies that a candidate whose {@code isConfigured} throws is treated as not
   * configured instead of breaking the selection.
   */
  @Test
  void testSelectSenderTreatsIsConfiguredFailureAsNotConfigured() {
    EmailSender broken = new EmailSender() {
      @Override
      public boolean isConfigured(EmailSendContext context) {
        throw new IllegalStateException("broken capability check");
      }

      @Override
      public void send(EmailSendContext context) {
        // intentionally empty: this fake only exercises the selection logic, never the send
      }

      @Override
      public int getPriority() {
        return 100;
      }
    };
    FakeSender configured = new FakeSender(10, true);

    EmailSender selected = EmailSenderDispatcher.selectSender(Arrays.asList(broken, configured),
        emptyContext());

    assertSame(configured, selected);
  }

  /**
   * Verifies that {@link EmailSenderDispatcher#dispatch(EmailSendContext)} sends through
   * the highest-priority configured sender discovered via CDI.
   * @throws Exception never expected in this test
   */
  @Test
  void testDispatchSendsThroughSelectedSender() throws Exception {
    FakeSender alternative = new FakeSender(100, true);
    try (MockedStatic<WeldUtils> mockedWeld = mockStatic(WeldUtils.class)) {
      mockedWeld.when(() -> WeldUtils.getInstances(EmailSender.class))
          .thenReturn(Arrays.asList(new DefaultSmtpEmailSender(), alternative));

      EmailSenderDispatcher.dispatch(emptyContext());

      assertTrue(alternative.sendInvoked);
    }
  }

  /**
   * Verifies that an exception thrown by the selected sender's {@code send} propagates
   * unchanged through {@link EmailSenderDispatcher#dispatch(EmailSendContext)}, with no
   * retry through another sender.
   */
  @Test
  void testDispatchPropagatesSendFailureWithoutRetry() {
    FakeSender fallbackCandidate = new FakeSender(10, true);
    EmailSender failing = new EmailSender() {
      @Override
      public boolean isConfigured(EmailSendContext context) {
        return true;
      }

      @Override
      public void send(EmailSendContext context) throws Exception {
        throw new ServletException("transport failure");
      }

      @Override
      public int getPriority() {
        return 100;
      }
    };
    try (MockedStatic<WeldUtils> mockedWeld = mockStatic(WeldUtils.class)) {
      mockedWeld.when(() -> WeldUtils.getInstances(EmailSender.class))
          .thenReturn(Arrays.asList(failing, fallbackCandidate));

      ServletException thrown = assertThrows(ServletException.class,
          () -> EmailSenderDispatcher.dispatch(emptyContext()));

      assertEquals("transport failure", thrown.getMessage());
      assertFalse(fallbackCandidate.sendInvoked);
    }
  }

  /**
   * Verifies that {@link EmailSenderDispatcher#dispatch(EmailSendContext)} falls back to
   * the default SMTP behavior when CDI discovery is unavailable: with no SMTP configuration
   * in the context, the default sender raises a configuration error.
   */
  @Test
  void testDispatchFallsBackToDefaultWhenDiscoveryUnavailable() {
    try (MockedStatic<WeldUtils> mockedWeld = mockStatic(WeldUtils.class)) {
      mockedWeld.when(() -> WeldUtils.getInstances(EmailSender.class))
          .thenThrow(new IllegalStateException("no container"));

      assertThrows(ServletException.class,
          () -> EmailSenderDispatcher.dispatch(emptyContext()));
    }
  }

  /**
   * Verifies that {@link EmailSenderDispatcher#hasAlternativeSenderConfigured()} returns
   * {@code true} only when a configured sender other than the default SMTP one exists.
   */
  @Test
  void testHasAlternativeSenderConfigured() {
    try (MockedStatic<WeldUtils> mockedWeld = mockStatic(WeldUtils.class)) {
      mockedWeld.when(() -> WeldUtils.getInstances(EmailSender.class))
          .thenReturn(Arrays.asList(new DefaultSmtpEmailSender(), new FakeSender(100, true)));
      assertTrue(EmailSenderDispatcher.hasAlternativeSenderConfigured());
    }
  }

  /**
   * Verifies that {@link EmailSenderDispatcher#hasAlternativeSenderConfigured()} returns
   * {@code false} when the only candidates are the default SMTP sender or unconfigured
   * alternatives.
   */
  @Test
  void testHasAlternativeSenderConfiguredFalseWhenOnlyDefaultOrUnconfigured() {
    try (MockedStatic<WeldUtils> mockedWeld = mockStatic(WeldUtils.class)) {
      mockedWeld.when(() -> WeldUtils.getInstances(EmailSender.class))
          .thenReturn(Arrays.asList(new DefaultSmtpEmailSender(), new FakeSender(100, false)));
      assertFalse(EmailSenderDispatcher.hasAlternativeSenderConfigured());
    }
  }
}
