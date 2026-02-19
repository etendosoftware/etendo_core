package org.openbravo.base.secureApp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link DefaultValuesData}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultValuesDataTest {

  private DefaultValuesData data;

  @Before
  public void setUp() {
    data = new DefaultValuesData();
  }

  @Test
  public void testGetFieldReturnsColumnname() {
    data.columnname = "testColumn";
    assertEquals("testColumn", data.getField("columnname"));
  }

  @Test
  public void testGetFieldIsCaseInsensitive() {
    data.columnname = "testColumn";
    assertEquals("testColumn", data.getField("COLUMNNAME"));
  }

  @Test
  public void testGetFieldReturnsNullForUnknownField() {
    assertNull(data.getField("unknownField"));
  }

  @Test
  public void testGetFieldReturnsNullColumnname() {
    data.columnname = null;
    assertNull(data.getField("columnname"));
  }

  @Test
  public void testParseIdsSingleId() throws Exception {
    Method parseIds = DefaultValuesData.class.getDeclaredMethod("parseIds", String.class);
    parseIds.setAccessible(true);
    String result = (String) parseIds.invoke(null, "'id1'");
    assertEquals("('id1')", result);
  }

  @Test
  public void testParseIdsMultipleIds() throws Exception {
    Method parseIds = DefaultValuesData.class.getDeclaredMethod("parseIds", String.class);
    parseIds.setAccessible(true);
    String result = (String) parseIds.invoke(null, "'id1','id2','id3'");
    assertEquals("('id1','id2','id3')", result);
  }

  @Test
  public void testGetFieldDefaultIsNull() {
    // Default String field value is null
    assertNull(data.getField("columnname"));
  }
}
