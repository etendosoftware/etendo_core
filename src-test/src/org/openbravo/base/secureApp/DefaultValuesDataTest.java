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
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class DefaultValuesDataTest {

  private static final String TEST_COLUMN = "testColumn";
  private static final String COLUMNNAME = "columnname";

  private DefaultValuesData data;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    data = new DefaultValuesData();
  }
  /** Get field returns columnname. */

  @Test
  public void testGetFieldReturnsColumnname() {
    data.columnname = TEST_COLUMN;
    assertEquals(TEST_COLUMN, data.getField(COLUMNNAME));
  }
  /** Get field is case insensitive. */

  @Test
  public void testGetFieldIsCaseInsensitive() {
    data.columnname = TEST_COLUMN;
    assertEquals(TEST_COLUMN, data.getField("COLUMNNAME"));
  }
  /** Get field returns null for unknown field. */

  @Test
  public void testGetFieldReturnsNullForUnknownField() {
    assertNull(data.getField("unknownField"));
  }
  /** Get field returns null columnname. */

  @Test
  public void testGetFieldReturnsNullColumnname() {
    data.columnname = null;
    assertNull(data.getField(COLUMNNAME));
  }
  /**
   * Parse ids single id.
   * @throws Exception if an error occurs
   */

  @Test
  public void testParseIdsSingleId() throws Exception {
    Method parseIds = DefaultValuesData.class.getDeclaredMethod("parseIds", String.class);
    parseIds.setAccessible(true);
    String result = (String) parseIds.invoke(null, "'id1'");
    assertEquals("('id1')", result);
  }
  /**
   * Parse ids multiple ids.
   * @throws Exception if an error occurs
   */

  @Test
  public void testParseIdsMultipleIds() throws Exception {
    Method parseIds = DefaultValuesData.class.getDeclaredMethod("parseIds", String.class);
    parseIds.setAccessible(true);
    String result = (String) parseIds.invoke(null, "'id1','id2','id3'");
    assertEquals("('id1','id2','id3')", result);
  }
  /** Get field default is null. */

  @Test
  public void testGetFieldDefaultIsNull() {
    // Default String field value is null
    assertNull(data.getField(COLUMNNAME));
  }
}
