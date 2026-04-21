package org.openbravo.erpCommon.utility.reporting.printing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
/** Tests for {@link AttachContent}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.class)
public class AttachContentTest {

  private static final String TEST_DOC_NAME = "Invoice_001.pdf";
  private static final String TEST_FILE_NAME = "invoice.pdf";
  private static final String TEST_ID = "12345";
  private static final String TEST_VISIBLE = "Y";
  private static final String TEST_SELECTED = "N";

  private AttachContent instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new AttachContent();
  }
  /** Set and get doc name. */

  @Test
  public void testSetAndGetDocName() {
    instance.setDocName(TEST_DOC_NAME);
    assertEquals(TEST_DOC_NAME, instance.getDocName());
  }
  /** Set and get file name. */

  @Test
  public void testSetAndGetFileName() {
    instance.setFileName(TEST_FILE_NAME);
    assertEquals(TEST_FILE_NAME, instance.getFileName());
  }
  /** Set and get id. */

  @Test
  public void testSetAndGetId() {
    instance.setId(TEST_ID);
    assertEquals(TEST_ID, instance.getId());
  }
  /** Set and get visible. */

  @Test
  public void testSetAndGetVisible() {
    instance.setVisible(TEST_VISIBLE);
    assertEquals(TEST_VISIBLE, instance.getVisible());
  }
  /** Set and get selected. */

  @Test
  public void testSetAndGetSelected() {
    instance.setSelected(TEST_SELECTED);
    assertEquals(TEST_SELECTED, instance.getSelected());
  }
  /** Get field file name. */

  @Test
  public void testGetFieldFileName() {
    instance.setFileName(TEST_FILE_NAME);
    assertEquals(TEST_FILE_NAME, instance.getField("FILENAME"));
  }
  /** Get field file name case insensitive. */

  @Test
  public void testGetFieldFileNameCaseInsensitive() {
    instance.setFileName(TEST_FILE_NAME);
    assertEquals(TEST_FILE_NAME, instance.getField("filename"));
  }
  /** Get field id. */

  @Test
  public void testGetFieldId() {
    instance.setId(TEST_ID);
    assertEquals(TEST_ID, instance.getField("ID"));
  }
  /** Get field visible. */

  @Test
  public void testGetFieldVisible() {
    instance.setVisible(TEST_VISIBLE);
    assertEquals(TEST_VISIBLE, instance.getField("VISIBLE"));
  }
  /** Get field selected. */

  @Test
  public void testGetFieldSelected() {
    instance.setSelected(TEST_SELECTED);
    assertEquals(TEST_SELECTED, instance.getField("SELECTED"));
  }
  /** Get field doc name. */

  @Test
  public void testGetFieldDocName() {
    instance.setDocName(TEST_DOC_NAME);
    assertEquals(TEST_DOC_NAME, instance.getField("DOCNAME"));
  }
  /** Get field unknown returns null. */

  @Test
  public void testGetFieldUnknownReturnsNull() {
    assertNull(instance.getField("UNKNOWN_FIELD"));
  }
  /** Get field null values return null. */

  @Test
  public void testGetFieldNullValuesReturnNull() {
    assertNull(instance.getField("FILENAME"));
    assertNull(instance.getField("ID"));
    assertNull(instance.getField("VISIBLE"));
    assertNull(instance.getField("SELECTED"));
    assertNull(instance.getField("DOCNAME"));
  }
  /** Direct field access. */

  @Test
  public void testDirectFieldAccess() {
    instance.docName = TEST_DOC_NAME;
    instance.fileName = TEST_FILE_NAME;
    instance.id = TEST_ID;
    instance.visible = TEST_VISIBLE;
    instance.selected = TEST_SELECTED;

    assertEquals(TEST_DOC_NAME, instance.getDocName());
    assertEquals(TEST_FILE_NAME, instance.getFileName());
    assertEquals(TEST_ID, instance.getId());
    assertEquals(TEST_VISIBLE, instance.getVisible());
    assertEquals(TEST_SELECTED, instance.getSelected());
  }
}
