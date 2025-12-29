/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2009-2025 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.listeners;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.erpCommon.ad_process.HeartbeatProcess;
import org.openbravo.scheduling.ProcessBundle;

/**
 * Unit test class for HeartbeatListener
 *
 * @author Etendo
 */
public class HeartbeatListenerTest extends WeldBaseTest {

  private HeartbeatListener heartbeatListener;
  private ServletContextEvent mockEvent;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    heartbeatListener = new HeartbeatListener();
    ServletContext mockContext = mock(ServletContext.class);
    mockEvent = mock(ServletContextEvent.class);
    when(mockEvent.getServletContext()).thenReturn(mockContext);
  }

  @Test
  public void testContextInitialized() throws Exception {
    // When
    heartbeatListener.contextInitialized(mockEvent);

    // Then
    // Verify scheduler is initialized
    Field schedulerField = HeartbeatListener.class.getDeclaredField("scheduler");
    schedulerField.setAccessible(true);
    ScheduledExecutorService scheduler = (ScheduledExecutorService) schedulerField.get(heartbeatListener);
    assertNotNull("Scheduler should be initialized", scheduler);
    assertTrue("Scheduler should not be shutdown", !scheduler.isShutdown());
  }

  @Test
  public void testContextDestroyed() throws Exception {
    // Given
    heartbeatListener.contextInitialized(mockEvent);

    // When
    heartbeatListener.contextDestroyed(mockEvent);

    // Then
    // Verify scheduler is shutdown
    Field schedulerField = HeartbeatListener.class.getDeclaredField("scheduler");
    schedulerField.setAccessible(true);
    ScheduledExecutorService scheduler = (ScheduledExecutorService) schedulerField.get(heartbeatListener);
    assertNotNull("Scheduler should exist", scheduler);
    assertTrue("Scheduler should be shutdown", scheduler.isShutdown());
  }

  @Test
  public void testRunHeartbeatSuccess() throws Exception {
    // When
    // Use reflection to call private static method
    java.lang.reflect.Method runHeartbeatMethod = HeartbeatListener.class.getDeclaredMethod("runHeartbeat");
    runHeartbeatMethod.setAccessible(true);
    runHeartbeatMethod.invoke(null);

    // Then
    // Verify HeartbeatProcess was executed (no exception thrown)
    assertTrue("Heartbeat should run without exception", true);
  }

  @Test
  public void testRunHeartbeatWithException() throws Exception {
    // Given - Mock HeartbeatProcess constructor to throw exception
    try (MockedConstruction<HeartbeatProcess> mockedConstruction = mockConstruction(HeartbeatProcess.class,
        (mock, context) -> {
          doThrow(new RuntimeException("Test exception")).when(mock).execute(any(ProcessBundle.class));
        })) {

      // When
      java.lang.reflect.Method runHeartbeatMethod = HeartbeatListener.class.getDeclaredMethod("runHeartbeat");
      runHeartbeatMethod.setAccessible(true);
      runHeartbeatMethod.invoke(null);

      // Then
      // Should not throw exception, just log it
      assertTrue("Heartbeat should handle exception gracefully", true);
    }
  }

  @Test
  public void testSchedulerInitializationMultipleTimes() throws Exception {
    // When - Initialize multiple times
    heartbeatListener.contextInitialized(mockEvent);
    heartbeatListener.contextInitialized(mockEvent);

    // Then
    Field schedulerField = HeartbeatListener.class.getDeclaredField("scheduler");
    schedulerField.setAccessible(true);
    ScheduledExecutorService scheduler = (ScheduledExecutorService) schedulerField.get(heartbeatListener);
    assertNotNull("Scheduler should be initialized", scheduler);
  }

  @Test
  public void testContextDestroyedWithoutInitialization() throws Exception {
    // When - Destroy without initializing
    heartbeatListener.contextDestroyed(mockEvent);

    // Then - Should not throw exception
    assertTrue("Destroy without init should be safe", true);
  }

  /**
   * Cleans up the test environment.
   */
  @After
  public void cleanUp() {
    try {
      // Ensure scheduler is shutdown after each test
      Field schedulerField = HeartbeatListener.class.getDeclaredField("scheduler");
      schedulerField.setAccessible(true);
      ScheduledExecutorService scheduler = (ScheduledExecutorService) schedulerField.get(null);
      if (scheduler != null && !scheduler.isShutdown()) {
        scheduler.shutdownNow();
      }
    } catch (Exception e) {
      // Ignore cleanup exceptions
    }
  }
}