package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link AcctSchemaElement}.
 * Tests the constructor and public fields, as well as segment type constants.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class AcctSchemaElementTest {

  private static final String ACCOUNT = "Account";

  private static final String TEST_ID = "ELEM_001";
  private static final String TEST_SEQ_NO = "10";
  private static final String TEST_NAME = "Organization";
  private static final String TEST_SEGMENT_TYPE = "OO";
  private static final String TEST_ELEMENT_ID = "C_ELEM_001";
  private static final String TEST_DEFAULT_VALUE = "DEFAULT_ORG";
  private static final String TEST_MANDATORY = "Y";
  private static final String TEST_BALANCED = "N";
  /** Constructor sets all fields. */

  @Test
  public void testConstructorSetsAllFields() {
    AcctSchemaElement element = new AcctSchemaElement(
        TEST_ID, TEST_SEQ_NO, TEST_NAME, TEST_SEGMENT_TYPE,
        TEST_ELEMENT_ID, TEST_DEFAULT_VALUE, TEST_MANDATORY, TEST_BALANCED);

    assertEquals(TEST_ID, element.m_ID);
    assertEquals(TEST_SEQ_NO, element.m_seqNo);
    assertEquals(TEST_NAME, element.m_name);
    assertEquals(TEST_SEGMENT_TYPE, element.m_segmentType);
    assertEquals(TEST_ELEMENT_ID, element.m_C_Element_ID);
    assertEquals(TEST_DEFAULT_VALUE, element.m_defaultValue);
    assertEquals(TEST_MANDATORY, element.m_mandatory);
    assertEquals(TEST_BALANCED, element.m_balanced);
  }
  /** Constructor with account segment. */

  @Test
  public void testConstructorWithAccountSegment() {
    AcctSchemaElement element = new AcctSchemaElement(
        "E2", "20", ACCOUNT, "AC", "ELEM2", "ACCT_VAL", "Y", "Y");

    assertEquals("AC", element.m_segmentType);
    assertEquals(ACCOUNT, element.m_name);
    assertEquals("Y", element.m_balanced);
  }
  /** Constructor with empty default value. */

  @Test
  public void testConstructorWithEmptyDefaultValue() {
    AcctSchemaElement element = new AcctSchemaElement(
        "E3", "30", "BPartner", "BP", "ELEM3", "", "N", "N");

    assertEquals("", element.m_defaultValue);
    assertEquals("N", element.m_mandatory);
  }
  /** Segment type constants. */

  @Test
  public void testSegmentTypeConstants() {
    assertEquals("OO", AcctSchemaElement.SEGMENT_Org);
    assertEquals("AC", AcctSchemaElement.SEGMENT_Account);
    assertEquals("BP", AcctSchemaElement.SEGMENT_BPartner);
    assertEquals("PR", AcctSchemaElement.SEGMENT_Product);
    assertEquals("AY", AcctSchemaElement.SEGMENT_Activity);
    assertEquals("LF", AcctSchemaElement.SEGMENT_LocationFrom);
    assertEquals("LT", AcctSchemaElement.SEGMENT_LocationTo);
    assertEquals("MC", AcctSchemaElement.SEGMENT_Campaign);
    assertEquals("OT", AcctSchemaElement.SEGMENT_OrgTrx);
    assertEquals("PJ", AcctSchemaElement.SEGMENT_Project);
    assertEquals("SR", AcctSchemaElement.SEGMENT_SalesRegion);
    assertEquals("U1", AcctSchemaElement.SEGMENT_User1);
    assertEquals("U2", AcctSchemaElement.SEGMENT_User2);
  }
  /** Default balanced value. */

  @Test
  public void testDefaultBalancedValue() {
    AcctSchemaElement element = new AcctSchemaElement(
        "E4", "40", "Product", "PR", "ELEM4", "PROD1", "N", "N");

    assertEquals("N", element.m_balanced);
  }
  /** Constructor with all segment types. */

  @Test
  public void testConstructorWithAllSegmentTypes() {
    String[][] segments = {
        {"OO", TEST_NAME},
        {"AC", ACCOUNT},
        {"BP", "Business Partner"},
        {"PR", "Product"},
        {"AY", "Activity"},
        {"LF", "Location From"},
        {"LT", "Location To"},
        {"MC", "Campaign"},
        {"OT", "Org Transaction"},
        {"PJ", "Project"},
        {"SR", "Sales Region"},
        {"U1", "User 1"},
        {"U2", "User 2"}
    };

    for (int i = 0; i < segments.length; i++) {
      AcctSchemaElement element = new AcctSchemaElement(
          "E" + i, String.valueOf((i + 1) * 10), segments[i][1],
          segments[i][0], "ELEM" + i, "DEF" + i, "Y", "N");

      assertEquals(segments[i][0], element.m_segmentType);
      assertEquals(segments[i][1], element.m_name);
    }
  }
}
