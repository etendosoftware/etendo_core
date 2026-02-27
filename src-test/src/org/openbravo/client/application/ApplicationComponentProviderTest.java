package org.openbravo.client.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.Component;
/** Tests for {@link ApplicationComponentProvider}. */

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApplicationComponentProviderTest {

  private static final String MAIN_LAYOUT_ID = "Application";
  private static final String VIEW_COMPONENT_ID = "View";
  private static final String UNSUPPORTED_ID = "UnsupportedComponent";

  private ApplicationComponentProvider instance;

  private MockedStatic<WeldUtils> weldUtilsStatic;

  @Mock
  private MainLayoutComponent mockMainLayout;
  @Mock
  private ViewComponent mockViewComponent;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ApplicationComponentProvider.class);

    weldUtilsStatic = mockStatic(WeldUtils.class);
    weldUtilsStatic.when(() -> WeldUtils.getInstanceFromStaticBeanManager(MainLayoutComponent.class))
        .thenReturn(mockMainLayout);
    weldUtilsStatic.when(() -> WeldUtils.getInstanceFromStaticBeanManager(ViewComponent.class))
        .thenReturn(mockViewComponent);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (weldUtilsStatic != null) weldUtilsStatic.close();
  }
  /** Get component main layout. */

  @Test
  public void testGetComponentMainLayout() {
    Map<String, Object> parameters = new HashMap<>();
    Component result = instance.getComponent(MAIN_LAYOUT_ID, parameters);

    assertNotNull(result);
    assertEquals(mockMainLayout, result);
  }
  /** Get component view component. */

  @Test
  public void testGetComponentViewComponent() {
    Map<String, Object> parameters = new HashMap<>();
    Component result = instance.getComponent(VIEW_COMPONENT_ID, parameters);

    assertNotNull(result);
    assertEquals(mockViewComponent, result);
  }
  /** Get component unsupported id throws exception. */

  @Test(expected = IllegalArgumentException.class)
  public void testGetComponentUnsupportedIdThrowsException() {
    Map<String, Object> parameters = new HashMap<>();
    instance.getComponent(UNSUPPORTED_ID, parameters);
  }
  /** Get component unsupported id message. */

  @Test
  public void testGetComponentUnsupportedIdMessage() {
    Map<String, Object> parameters = new HashMap<>();
    try {
      instance.getComponent(UNSUPPORTED_ID, parameters);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals("Component id " + UNSUPPORTED_ID + " not supported.", e.getMessage());
    }
  }
  /** Qualifier constant. */

  @Test
  public void testQualifierConstant() {
    assertEquals(ApplicationConstants.COMPONENT_TYPE, ApplicationComponentProvider.QUALIFIER);
  }
}
