package org.openbravo.materialmgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;

/**
 * Test for {@link ManageVariantsCustomProductCharacteristicWhereClause} class
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageVariantsCustomProductCharacteristicWhereClauseTest {

  @InjectMocks
  private ManageVariantsCustomProductCharacteristicWhereClause whereClause;

  /**
   * Initializes Mockito mocks before each test.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * Test that the custom where clause is correctly generated for a given product ID
   */
  @Test
  public void testGetCustomWhereClause() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    Map<String, Object> queryNamedParameters = new HashMap<>();
    String productId = "12345ABCDE";
    String contextInfo = "{\"inpmProductId\":\"" + productId + "\"}";
    requestParameters.put("_buttonOwnerContextInfo", contextInfo);

    // WHEN
    String result = whereClause.getCustomWhereClause(requestParameters, queryNamedParameters);

    // THEN
    // Verify the where clause contains the correct HQL
    assertTrue(StringUtils.contains(result, "exists (from ProductCharacteristic pc"));
    assertTrue(StringUtils.contains(result,
        "where pc.characteristic = c and pc.product.id = :productId and pc.variant = true"));
    assertTrue(StringUtils.contains(result,
        "and (pc.characteristicSubset is null or exists (from CharacteristicSubsetValue csv"));

    // Verify the parameter was set correctly
    assertEquals(productId, queryNamedParameters.get("productId"));
  }

  /**
   * Test behavior when the request parameters contain invalid JSON
   */
  @Test
  public void testGetCustomWhereClauseWithInvalidJson() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    Map<String, Object> queryNamedParameters = new HashMap<>();
    requestParameters.put("_buttonOwnerContextInfo", "invalid json");

    // WHEN & THEN
    assertThrows(OBException.class, () -> whereClause.getCustomWhereClause(requestParameters, queryNamedParameters));
  }

  /**
   * Test behavior when the button owner context info is missing from the request parameters
   */
  @Test
  public void testGetCustomWhereClauseWithMissingContextInfo() {
    // GIVEN
    Map<String, String> requestParameters = new HashMap<>();
    Map<String, Object> queryNamedParameters = new HashMap<>();
    // No context info in request parameters

    // WHEN & THEN
    assertThrows(NullPointerException.class,
        () -> whereClause.getCustomWhereClause(requestParameters, queryNamedParameters));
  }

  /**
   * Test that the correct process ID qualifier is defined
   */
  @Test
  public void testProcessIdQualifier() {
    // GIVEN & WHEN & THEN
    assertEquals("FE3A8C134D41488DB3A69837BD54B56A",
        ManageVariantsCustomProductCharacteristicWhereClause.MANAGE_VARIANTS_PROCESS_ID);
  }
}
