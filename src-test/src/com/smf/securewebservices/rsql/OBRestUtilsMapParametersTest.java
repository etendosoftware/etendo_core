package com.smf.securewebservices.rsql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openbravo.service.json.JsonConstants;

import com.smf.securewebservices.TestingConstants;

/**
 * Test class for OBRestUtils.mapRestParameters with isolation of WeldUtils.
 */
public class OBRestUtilsMapParametersTest {

  /**
   * Wrapper over mapRestParameters to avoid NPE in WeldUtils.
   * Isolates the call to WeldUtils that causes issues in tests.
   */
  private Map<String, String> mapRestParametersWrapper(Map<String, String> params) {
    Map<String, String> paramsCopy = new HashMap<>(params);
    paramsCopy.put(JsonConstants.WHERE_CLAUSE_HAS_BEEN_CHECKED, "true");
    return OBRestUtils.mapRestParameters(paramsCopy);
  }

  /**
   * Tests basic parameters mapping.
   */
  @Test
  public void testMapRestParametersBasicParameters() {
    // GIVEN
    Map<String, String> params = new HashMap<>();
    params.put(JsonConstants.ENTITYNAME, TestingConstants.TEST_ENTITY);
    params.put(JsonConstants.ID, "123");
    params.put(OBRestConstants.SORTBY_PARAMETER, "name asc");

    // WHEN
    Map<String, String> result = mapRestParametersWrapper(params);

    // THEN
    assertEquals(TestingConstants.TEST_ENTITY, result.get(JsonConstants.ENTITYNAME));
    assertEquals("123", result.get(JsonConstants.ID));
    assertEquals("name asc", result.get(JsonConstants.ORDERBY_PARAMETER));
    assertEquals("true", result.get(JsonConstants.USE_ALIAS));
    assertEquals(JsonConstants.TEXTMATCH_EXACT, result.get(JsonConstants.TEXTMATCH_PARAMETER));
    assertEquals(TestingConstants.BOOLEAN_FALSE, result.get(OBRestConstants.IDENTIFIERS_PARAMETER));
    assertEquals("0", result.get(JsonConstants.STARTROW_PARAMETER));
  }

  /**
   * Tests distinct parameter mapping.
   */
  @Test
  public void testMapRestParametersDistinct() {
    // GIVEN
    Map<String, String> params = new HashMap<>();
    params.put(JsonConstants.ENTITYNAME, TestingConstants.TEST_ENTITY);
    params.put(OBRestConstants.DISTINCT_PARAMETER, "true");

    // WHEN
    Map<String, String> result = mapRestParametersWrapper(params);

    // THEN
    assertEquals("true", result.get(JsonConstants.DISTINCT_PARAMETER));
  }

  /**
   * Tests use alias parameter mapping.
   */
  @Test
  public void testMapRestParametersUseAlias() {
    // GIVEN
    Map<String, String> params = new HashMap<>();
    params.put(JsonConstants.ENTITYNAME, TestingConstants.TEST_ENTITY);
    params.put(OBRestConstants.USEALIAS_PARAMETER, TestingConstants.BOOLEAN_FALSE);

    // WHEN
    Map<String, String> result = mapRestParametersWrapper(params);

    // THEN
    assertEquals(TestingConstants.BOOLEAN_FALSE, result.get(JsonConstants.USE_ALIAS));
  }

  /**
   * Tests query parameter mapping.
   */
  @Test
  public void testMapRestParametersQueryParameter() {
    // GIVEN
    Map<String, String> params = new HashMap<>();
    params.put(JsonConstants.ENTITYNAME, TestingConstants.TEST_ENTITY);

    // WHEN
    Map<String, String> result = mapRestParametersWrapper(params);

    // THEN
    assertNotNull(result);
  }

  /**
   * Tests children parameter mapping.
   */
  @Test
  public void testMapRestParametersChildrenParameter() {
    // GIVEN
    Map<String, String> params = new HashMap<>();
    params.put(JsonConstants.ENTITYNAME, TestingConstants.TEST_ENTITY);
    params.put(OBRestConstants.CHILDREN_PARAMETER, "lines");

    // WHEN
    Map<String, String> result = mapRestParametersWrapper(params);

    // THEN
    assertEquals("lines", result.get(OBRestConstants.CHILDREN_PARAMETER));
  }

  /**
   * Tests identifiers parameter mapping.
   */
  @Test
  public void testMapRestParametersIdentifiersParameter() {
    // GIVEN
    Map<String, String> params = new HashMap<>();
    params.put(JsonConstants.ENTITYNAME, TestingConstants.TEST_ENTITY);
    params.put(OBRestConstants.IDENTIFIERS_PARAMETER, "true");

    // WHEN
    Map<String, String> result = mapRestParametersWrapper(params);

    // THEN
    assertEquals("true", result.get(OBRestConstants.IDENTIFIERS_PARAMETER));
  }

  /**
   * Tests no active filter parameter mapping.
   */
  @Test
  public void testMapRestParametersNoActiveFilter() {
    // GIVEN
    Map<String, String> params = new HashMap<>();
    params.put(JsonConstants.ENTITYNAME, TestingConstants.TEST_ENTITY);
    params.put(OBRestConstants.NO_ACTIVE_FILTER, "true");

    // WHEN
    Map<String, String> result = mapRestParametersWrapper(params);

    // THEN
    assertEquals("true", result.get(JsonConstants.NO_ACTIVE_FILTER));
  }

  /**
   * Tests fields parameter mapping.
   */
  @Test
  public void testMapRestParametersFieldsParameter() {
    // GIVEN
    Map<String, String> params = new HashMap<>();
    params.put(JsonConstants.ENTITYNAME, TestingConstants.TEST_ENTITY);
    params.put(OBRestConstants.FIELDS_PARAMETER, "name,order.bPartner.name,documentNo");

    // WHEN
    Map<String, String> result = mapRestParametersWrapper(params);

    // THEN
    String selectedProperties = result.get(JsonConstants.SELECTEDPROPERTIES_PARAMETER);
    assertNotNull(selectedProperties);
    assertTrue(selectedProperties.contains("name"));
    assertTrue(selectedProperties.contains("order"));
    assertTrue(selectedProperties.contains("order.bPartner"));
    assertTrue(selectedProperties.contains("order.bPartner.name"));
    assertTrue(selectedProperties.contains("documentNo"));
  }

  /**
   * Tests pagination parameters mapping.
   */
  @Test
  public void testMapRestParametersPaginationParameters() {
    // GIVEN
    Map<String, String> params = new HashMap<>();
    params.put(JsonConstants.ENTITYNAME, TestingConstants.TEST_ENTITY);
    params.put(OBRestConstants.FIRSTRESULT_PARAMETER, "10");
    params.put(OBRestConstants.MAXRESULTS_PARAMETER, "20");

    // WHEN
    Map<String, String> result = mapRestParametersWrapper(params);

    // THEN
    assertEquals("10", result.get(JsonConstants.STARTROW_PARAMETER));
    assertEquals("29", result.get(JsonConstants.ENDROW_PARAMETER));
  }

  /**
   * Tests pagination parameters mapping with negative first result.
   */
  @Test
  public void testMapRestParametersPaginationParametersNegative() {
    // GIVEN
    Map<String, String> params = new HashMap<>();
    params.put(JsonConstants.ENTITYNAME, TestingConstants.TEST_ENTITY);
    params.put(OBRestConstants.FIRSTRESULT_PARAMETER, "-5");
    params.put(OBRestConstants.MAXRESULTS_PARAMETER, "20");

    // WHEN
    Map<String, String> result = mapRestParametersWrapper(params);

    // THEN
    assertEquals("0", result.get(JsonConstants.STARTROW_PARAMETER));
    assertEquals("19", result.get(JsonConstants.ENDROW_PARAMETER));
  }

  /**
   * Tests context parameters mapping.
   */
  @Test
  public void testMapRestParametersContextParameters() {
    // GIVEN
    Map<String, String> params = new HashMap<>();
    params.put(JsonConstants.ENTITYNAME, TestingConstants.TEST_ENTITY);
    params.put("@AD_Client_ID@", TestingConstants.LARGE_NUMBER);
    params.put("@AD_Org_ID@", TestingConstants.LARGE_NUMBER);
    params.put("regular_param", "value");

    // WHEN
    Map<String, String> result = mapRestParametersWrapper(params);

    // THEN
    assertEquals(TestingConstants.LARGE_NUMBER, result.get("@AD_Client_ID@"));
    assertEquals(TestingConstants.LARGE_NUMBER, result.get("@AD_Org_ID@"));
    assertNull(result.get("regular_param"));
  }
}
