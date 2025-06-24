package com.smf.securewebservices;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.cfg.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.dal.core.OBInterceptor;

/**
 * Test class for InitializeSingleton.
 */
@RunWith(MockitoJUnitRunner.class)
public class InitializeSingletonTest {

  @Mock
  private SWSInterceptor mockSWSInterceptor;

  @Mock
  private SessionFactoryController mockSessionFactoryController;

  @Mock
  private Configuration mockConfiguration;

  @Mock
  private OBInterceptor mockOBInterceptor;

  @Mock
  private SWSConfig mockSWSConfig;

  @InjectMocks
  private InitializeSingleton initializeSingleton;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    // No setup required for now
  }

  /**
   * Tests the initialize method.
   */
  @Test
  public void testInitialize() {
    // GIVEN
    try (MockedStatic<SWSConfig> mockedSWSConfig = mockStatic(
        SWSConfig.class); MockedStatic<SessionFactoryController> mockedSessionFactory = mockStatic(
        SessionFactoryController.class)) {

      mockedSWSConfig.when(SWSConfig::getInstance).thenReturn(mockSWSConfig);
      doNothing().when(mockSWSConfig).refresh();

      mockedSessionFactory.when(SessionFactoryController::getInstance).thenReturn(mockSessionFactoryController);
      when(mockSessionFactoryController.getConfiguration()).thenReturn(mockConfiguration);
      when(mockConfiguration.getInterceptor()).thenReturn(mockOBInterceptor);

      // WHEN
      initializeSingleton.initialize();

      // THEN
      verify(mockSWSConfig).refresh();
      verify(mockOBInterceptor).setInterceptorListener(mockSWSInterceptor);
    }
  }

  /**
   * Tests the setInterceptor method.
   */
  @Test
  public void testSetInterceptor() {
    // GIVEN
    try (MockedStatic<SessionFactoryController> mockedSessionFactory = mockStatic(SessionFactoryController.class)) {
      mockedSessionFactory.when(SessionFactoryController::getInstance).thenReturn(mockSessionFactoryController);
      when(mockSessionFactoryController.getConfiguration()).thenReturn(mockConfiguration);
      when(mockConfiguration.getInterceptor()).thenReturn(mockOBInterceptor);

      // WHEN
      initializeSingleton.setInterceptor();

      // THEN
      verify(mockOBInterceptor).setInterceptorListener(mockSWSInterceptor);
      verify(mockSessionFactoryController).getConfiguration();
      verify(mockConfiguration).getInterceptor();
    }
  }
}
