package org.openbravo.erpCommon.utility.reporting.printing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AttachContentTest {

  private static final String TEST_DOC_NAME = "Invoice_001.pdf";
  private static final String TEST_FILE_NAME = "invoice.pdf";
  private static final String TEST_ID = "12345";
  private static final String TEST_VISIBLE = "Y";
  private static final String TEST_SELECTED = "N";

  private AttachContent instance;

  @Before
  public void setUp() {
    instance = new AttachContent();
  }

  @Test
  public void testSetAndGetDocName() {
    instance.setDocName(TEST_DOC_NAME);
    assertEquals(TEST_DOC_NAME, instance.getDocName());
  }

  @Test
  public void testSetAndGetFileName() {
    instance.setFileName(TEST_FILE_NAME);
    assertEquals(TEST_FILE_NAME, instance.getFileName());
  }

  @Test
  public void testSetAndGetId() {
    instance.setId(TEST_ID);
    assertEquals(TEST_ID, instance.getId());
  }

  @Test
  public void testSetAndGetVisible() {
    instance.setVisible(TEST_VISIBLE);
    assertEquals(TEST_VISIBLE, instance.getVisible());
  }

  @Test
  public void testSetAndGetSelected() {
    instance.setSelected(TEST_SELECTED);
    assertEquals(TEST_SELECTED, instance.getSelected());
  }

  @Test
  public void testGetFieldFileName() {
    instance.setFileName(TEST_FILE_NAME);
    assertEquals(TEST_FILE_NAME, instance.getField("FILENAME"));
  }

  @Test
  public void testGetFieldFileNameCaseInsensitive() {
    instance.setFileName(TEST_FILE_NAME);
    assertEquals(TEST_FILE_NAME, instance.getField("filename"));
  }

  @Test
  public void testGetFieldId() {
    instance.setId(TEST_ID);
    assertEquals(TEST_ID, instance.getField("ID"));
  }

  @Test
  public void testGetFieldVisible() {
    instance.setVisible(TEST_VISIBLE);
    assertEquals(TEST_VISIBLE, instance.getField("VISIBLE"));
  }

  @Test
  public void testGetFieldSelected() {
    instance.setSelected(TEST_SELECTED);
    assertEquals(TEST_SELECTED, instance.getField("SELECTED"));
  }

  @Test
  public void testGetFieldDocName() {
    instance.setDocName(TEST_DOC_NAME);
    assertEquals(TEST_DOC_NAME, instance.getField("DOCNAME"));
  }

  @Test
  public void testGetFieldUnknownReturnsNull() {
    assertNull(instance.getField("UNKNOWN_FIELD"));
  }

  @Test
  public void testGetFieldNullValuesReturnNull() {
    assertNull(instance.getField("FILENAME"));
    assertNull(instance.getField("ID"));
    assertNull(instance.getField("VISIBLE"));
    assertNull(instance.getField("SELECTED"));
    assertNull(instance.getField("DOCNAME"));
  }

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
