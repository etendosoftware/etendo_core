package org.openbravo.client.kernel;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;

/**
 * Tests for {@link ComponentGenerator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ComponentGeneratorTest {

  private static final String ORIGINAL_JS = "var x = 1;";
  private static final String COMPRESSED_JS = "var x=1;";

  private ComponentGenerator generator;

  @Mock
  private Component mockComponent;

  @Mock
  private JSCompressor mockCompressor;

  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<JSCompressor> jsCompressorStatic;

  @Before
  public void setUp() {
    generator = new ComponentGenerator();

    obContextStatic = mockStatic(OBContext.class);
    jsCompressorStatic = mockStatic(JSCompressor.class);
    jsCompressorStatic.when(JSCompressor::getInstance).thenReturn(mockCompressor);
  }

  @After
  public void tearDown() {
    if (obContextStatic != null) obContextStatic.close();
    if (jsCompressorStatic != null) jsCompressorStatic.close();
  }

  @Test
  public void testGenerateCompressesJavaScriptNotInDevelopment() {
    when(mockComponent.generate()).thenReturn(ORIGINAL_JS);
    when(mockComponent.isJavaScriptComponent()).thenReturn(true);
    when(mockComponent.isInDevelopment()).thenReturn(false);
    when(mockCompressor.compress(ORIGINAL_JS)).thenReturn(COMPRESSED_JS);

    String result = generator.generate(mockComponent);

    assertEquals(COMPRESSED_JS, result);
    verify(mockCompressor).compress(ORIGINAL_JS);
  }

  @Test
  public void testGenerateDoesNotCompressJavaScriptInDevelopment() {
    when(mockComponent.generate()).thenReturn(ORIGINAL_JS);
    when(mockComponent.isJavaScriptComponent()).thenReturn(true);
    when(mockComponent.isInDevelopment()).thenReturn(true);

    String result = generator.generate(mockComponent);

    assertEquals(ORIGINAL_JS, result);
    verify(mockCompressor, never()).compress(anyString());
  }

  @Test
  public void testGenerateDoesNotCompressNonJavaScriptComponent() {
    when(mockComponent.generate()).thenReturn("<div>html</div>");
    when(mockComponent.isJavaScriptComponent()).thenReturn(false);

    String result = generator.generate(mockComponent);

    assertEquals("<div>html</div>", result);
    verify(mockCompressor, never()).compress(anyString());
  }

  @Test
  public void testGetInstanceReturnsSingleton() {
    ComponentGenerator instance1 = ComponentGenerator.getInstance();
    ComponentGenerator instance2 = ComponentGenerator.getInstance();
    assertEquals(instance1, instance2);
  }

  @Test
  public void testSetInstanceUpdatesInstance() {
    ComponentGenerator custom = new ComponentGenerator();
    ComponentGenerator.setInstance(custom);

    assertEquals(custom, ComponentGenerator.getInstance());

    // Restore original
    ComponentGenerator.setInstance(generator);
  }
}
