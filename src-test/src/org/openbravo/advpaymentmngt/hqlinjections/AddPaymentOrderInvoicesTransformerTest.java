package org.openbravo.advpaymentmngt.hqlinjections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for AddPaymentOrderInvoicesTransformer
 * Tests the HQL query transformation for payment order/invoice selection
 */
@RunWith(MockitoJUnitRunner.class)
public class AddPaymentOrderInvoicesTransformerTest {

  private static final String TRANSACTION_TYPE_INVOICE = "I";
  private static final String TRANSACTION_TYPE_ORDER = "O";
  private static final String RECEIVED_FROM = "received_from";
  private static final String FIN_PAYMENT_ID = "fin_payment_id";
  private static final String AD_ORG_ID = "ad_org_id";
  private static final String C_CURRENCY_ID = "c_currency_id";
  private static final String ISSOTRX = "issotrx";
  public static final String ORD_DOCUMENT_NO = "ord.documentNo";
  public static final String CRITERIA = "criteria";
  public static final String INV_ID = "inv.id";
  public static final String TRANSACTION_IS_SALES_TRANSACTION = "ord.salesTransaction = :isSalesTransaction";
  public static final String MIXED = "MIXED";
  public static final String SALES_TRANSACTION_IS_SALES_TRANSACTION = "inv.salesTransaction = :isSalesTransaction";
  public static final String RESULT_SHOULD_NOT_BE_NULL = "Result should not be null";
  public static final String FIELD_NAME = "fieldName";
  public static final String VALUE = "value";

  private AddPaymentOrderInvoicesTransformer transformer;
  private Map<String, String> requestParameters;

  /**
   * Set up test fixtures before each test.
   * Initializes transformer instance and parameter maps.
   */
  @Before
  public void setUp() {
    transformer = new AddPaymentOrderInvoicesTransformer();
    requestParameters = new HashMap<>();

    // Set default required parameters
    requestParameters.put(C_CURRENCY_ID, "test-currency");
    requestParameters.put(ISSOTRX, "true");
    requestParameters.put(RECEIVED_FROM, "test-bp");
    requestParameters.put(AD_ORG_ID, "test-org");
  }

  /**
   * Verifies that getSelectClause generates correct clause for Invoice transaction type.
   */
  @Test
  public void testGetSelectClauseForInvoiceType() {
    StringBuffer result = transformer.getSelectClause(TRANSACTION_TYPE_INVOICE, false);
    
    assertNotNull(RESULT_SHOULD_NOT_BE_NULL, result);
    assertTrue("Should contain paymentScheduleDetail", result.toString().contains("paymentScheduleDetail"));
    assertTrue("Should contain salesOrderNo", result.toString().contains("salesOrderNo"));
    assertTrue("Should contain invoiceNo", result.toString().contains("invoiceNo"));
    assertTrue("Should contain paymentMethod", result.toString().contains("paymentMethod"));
    assertTrue("Should contain expectedAmount", result.toString().contains("expectedAmount"));
    assertTrue("Should contain outstandingAmount", result.toString().contains("outstandingAmount"));
    assertTrue("Should contain max for invoice", result.toString().contains("max(COALESCE(inv.grandTotalAmount"));
  }

  /**
   * Verifies that getSelectClause generates correct clause for Order transaction type.
   */
  @Test
  public void testGetSelectClauseForOrderType() {
    StringBuffer result = transformer.getSelectClause(TRANSACTION_TYPE_ORDER, false);
    
    assertNotNull(RESULT_SHOULD_NOT_BE_NULL, result);
    assertTrue("Should contain salesOrderNo", result.toString().contains("salesOrderNo"));
    assertTrue("Should contain sum for order", result.toString().contains("sum(COALESCE(inv.grandTotalAmount"));
    assertTrue("Should contain order fields", result.toString().contains(ORD_DOCUMENT_NO));
  }

  /**
   * Verifies that getSelectClause handles mixed type (neither I nor O).
   */
  @Test
  public void testGetSelectClauseForMixedType() {
    StringBuffer result = transformer.getSelectClause(MIXED, false);
    
    assertNotNull(RESULT_SHOULD_NOT_BE_NULL, result);
    assertTrue("Should contain COALESCE for createdBy", result.toString().contains("COALESCE(invCreatedBy"));
    assertTrue("Should contain COALESCE for updatedBy", result.toString().contains("COALESCE(invUpdatedBy"));
  }

  /**
   * Verifies that getSelectClause includes selection flag when hasSelectedIds is false.
   */
  @Test
  public void testGetSelectClauseWithoutSelectedIds() {
    StringBuffer result = transformer.getSelectClause(TRANSACTION_TYPE_INVOICE, false);
    
    assertTrue("Should check max(fp.id) for selection", result.toString().contains("max(fp.id) is not null"));
  }

  /**
   * Verifies that getSelectClause includes different selection logic when hasSelectedIds is true.
   */
  @Test
  public void testGetSelectClauseWithSelectedIds() {
    StringBuffer result = transformer.getSelectClause(TRANSACTION_TYPE_INVOICE, true);
    
    assertTrue("Should use client-side selection", result.toString().contains("1 < 0"));
  }

  /**
   * Verifies that getJoinClauseOrder includes business partner filter when provided.
   */
  @Test
  public void testGetJoinClauseOrderWithBusinessPartner() {
    requestParameters.put(RECEIVED_FROM, "BP123");
    
    StringBuffer result = transformer.getJoinClauseOrder(requestParameters);
    
    assertTrue("Should include businessPartnerId filter", 
        result.toString().contains("ord.businessPartner.id = :businessPartnerId"));
    assertTrue("Should include currency filter", result.toString().contains("ord.currency.id = :currencyId"));
    assertTrue("Should include sales transaction filter", 
        result.toString().contains(TRANSACTION_IS_SALES_TRANSACTION));
  }

  /**
   * Verifies that getJoinClauseOrder excludes business partner filter when null.
   */
  @Test
  public void testGetJoinClauseOrderWithoutBusinessPartner() {
    requestParameters.put(RECEIVED_FROM, null);
    
    StringBuffer result = transformer.getJoinClauseOrder(requestParameters);
    
    assertFalse("Should not include businessPartnerId filter", 
        result.toString().contains("businessPartner.id"));
  }

  /**
   * Verifies that getJoinClauseOrder excludes business partner filter when value is "null" string.
   */
  @Test
  public void testGetJoinClauseOrderWithNullString() {
    requestParameters.put(RECEIVED_FROM, "null");
    
    StringBuffer result = transformer.getJoinClauseOrder(requestParameters);
    
    assertFalse("Should not include businessPartnerId filter for 'null' string", 
        result.toString().contains("businessPartner.id"));
  }

  /**
   * Verifies that getJoinClauseInvoice includes business partner filter when provided.
   */
  @Test
  public void testGetJoinClauseInvoiceWithBusinessPartner() {
    requestParameters.put(RECEIVED_FROM, "BP456");
    
    StringBuffer result = transformer.getJoinClauseInvoice(requestParameters);
    
    assertTrue("Should include businessPartnerId filter", 
        result.toString().contains("inv.businessPartner.id = :businessPartnerId"));
    assertTrue("Should include currency filter", result.toString().contains("inv.currency.id = :currencyId"));
    assertTrue("Should include sales transaction filter", 
        result.toString().contains(SALES_TRANSACTION_IS_SALES_TRANSACTION));
  }

  /**
   * Verifies that getWhereClause handles payment ID parameter correctly.
   */
  @Test
  public void testGetWhereClauseWithPaymentId() {
    requestParameters.put(FIN_PAYMENT_ID, "PAYMENT123");
    
    StringBuffer result = transformer.getWhereClause(TRANSACTION_TYPE_INVOICE, requestParameters, 
        new ArrayList<String>());
    
    assertTrue("Should include payment condition", 
        result.toString().contains("psd.paymentDetails is null or fp.id = :paymentId"));
  }

  /**
   * Verifies that getWhereClause handles null payment ID correctly.
   */
  @Test
  public void testGetWhereClauseWithoutPaymentId() {
    requestParameters.put(FIN_PAYMENT_ID, null);
    
    StringBuffer result = transformer.getWhereClause(TRANSACTION_TYPE_INVOICE, requestParameters, 
        new ArrayList<String>());
    
    assertTrue("Should only check null payment details", 
        result.toString().contains("psd.paymentDetails is null"));
    assertFalse("Should not include fp.id condition", result.toString().contains("fp.id = :paymentId"));
  }

  /**
   * Verifies that getWhereClause includes organization filter when provided.
   */
  @Test
  public void testGetWhereClauseWithOrganization() {
    requestParameters.put(AD_ORG_ID, "ORG123");
    
    StringBuffer result = transformer.getWhereClause(TRANSACTION_TYPE_INVOICE, requestParameters, 
        new ArrayList<String>());
    
    assertTrue("Should include organization filter", 
        result.toString().contains("psd.organization.id in :orgIds"));
  }

  /**
   * Verifies that getWhereClause includes selected PSDs when provided.
   */
  @Test
  public void testGetWhereClauseWithSelectedPSDs() {
    List<String> selectedPSDs = new ArrayList<>();
    selectedPSDs.add("PSD1");
    selectedPSDs.add("PSD2");
    selectedPSDs.add("PSD3");
    
    StringBuffer result = transformer.getWhereClause(TRANSACTION_TYPE_INVOICE, requestParameters, 
        selectedPSDs);
    
    assertTrue("Should include psd.id in clause", result.toString().contains("psd.id in ("));
    assertTrue("Should include PSD1", result.toString().contains("'PSD1'"));
    assertTrue("Should include PSD2", result.toString().contains("'PSD2'"));
    assertTrue("Should include PSD3", result.toString().contains("'PSD3'"));
  }

  /**
   * Verifies that getWhereClause handles Invoice transaction type conditions.
   */
  @Test
  public void testGetWhereClauseForInvoiceType() {
    StringBuffer result = transformer.getWhereClause(TRANSACTION_TYPE_INVOICE, requestParameters, 
        new ArrayList<String>());
    
    assertTrue("Should include invoice sales transaction", 
        result.toString().contains(SALES_TRANSACTION_IS_SALES_TRANSACTION));
    assertTrue("Should include invoice currency", result.toString().contains("inv.currency.id = :currencyId"));
  }

  /**
   * Verifies that getWhereClause handles Order transaction type conditions.
   */
  @Test
  public void testGetWhereClauseForOrderType() {
    StringBuffer result = transformer.getWhereClause(TRANSACTION_TYPE_ORDER, requestParameters, 
        new ArrayList<String>());
    
    assertTrue("Should include order sales transaction", 
        result.toString().contains(TRANSACTION_IS_SALES_TRANSACTION));
    assertTrue("Should include order currency", result.toString().contains("ord.currency.id = :currencyId"));
  }

  /**
   * Verifies that getWhereClause handles mixed type with both invoice and order conditions.
   */
  @Test
  public void testGetWhereClauseForMixedType() {
    StringBuffer result = transformer.getWhereClause(MIXED, requestParameters,
        new ArrayList<String>());
    
    assertTrue("Should include invoice conditions", 
        result.toString().contains(SALES_TRANSACTION_IS_SALES_TRANSACTION));
    assertTrue("Should include order conditions", 
        result.toString().contains(TRANSACTION_IS_SALES_TRANSACTION));
    assertTrue("Should use OR between conditions", result.toString().contains("or ("));
  }

  /**
   * Verifies that getGroupByClause generates correct grouping for Invoice type.
   */
  @Test
  public void testGetGroupByClauseForInvoiceType() {
    StringBuffer result = transformer.getGroupByClause(TRANSACTION_TYPE_INVOICE);
    
    assertNotNull(RESULT_SHOULD_NOT_BE_NULL, result);
    assertTrue("Should group by invoice id", result.toString().contains(INV_ID));
    assertTrue("Should group by invoice documentNo", result.toString().contains("inv.documentNo"));
    assertTrue("Should group by business partner", result.toString().contains("bp.id"));
    assertTrue("Should group by payment method", result.toString().contains("finPaymentmethod.id"));
  }

  /**
   * Verifies that getGroupByClause generates correct grouping for Order type.
   */
  @Test
  public void testGetGroupByClauseForOrderType() {
    StringBuffer result = transformer.getGroupByClause(TRANSACTION_TYPE_ORDER);
    
    assertNotNull(RESULT_SHOULD_NOT_BE_NULL, result);
    assertTrue("Should group by order id", result.toString().contains("ord.id"));
    assertTrue("Should group by order documentNo", result.toString().contains(ORD_DOCUMENT_NO));
    assertFalse("Should not group by invoice fields", result.toString().contains(INV_ID));
  }

  /**
   * Verifies that getGroupByClause generates correct grouping for mixed type.
   */
  @Test
  public void testGetGroupByClauseForMixedType() {
    StringBuffer result = transformer.getGroupByClause(MIXED);
    
    assertNotNull(RESULT_SHOULD_NOT_BE_NULL, result);
    assertTrue("Should group by invoice id", result.toString().contains(INV_ID));
    assertTrue("Should group by order id", result.toString().contains("ord.id"));
    assertTrue("Should group by both document types", result.toString().contains("inv.documentNo"));
    assertTrue("Should group by both document types", result.toString().contains(ORD_DOCUMENT_NO));
  }

  /**
   * Verifies that removeGridFilters removes filters between AND and whereClause placeholder.
   */
  @Test
  public void testRemoveGridFilters() {
    String hqlQuery = "SELECT ... FROM ... where x = 1 AND psd.organization in (:org) AND filter1 = 'test' " +
        "AND filter2 = 'test2' and @whereClause@";
    
    String result = transformer.removeGridFilters(hqlQuery);
    
    assertFalse("Should remove grid filters", result.contains("filter1 = 'test'"));
    assertTrue("Should keep organization filter", result.contains("psd.organization in (:org)"));
    assertTrue("Should keep whereClause placeholder", result.contains("@whereClause@"));
  }

  /**
   * Verifies that removeGridFilters handles query without grid filters.
   */
  @Test
  public void testRemoveGridFiltersWithoutFilters() {
    String hqlQuery = "SELECT ... FROM ... where x = 1 AND psd.organization in (:org) and @whereClause@";
    
    String result = transformer.removeGridFilters(hqlQuery);
    
    assertEquals(hqlQuery, result, "Should return same query");
  }

  /**
   * Verifies that transformCriteria extracts selected PSDs from id field.
   * 
   * @throws JSONException if JSON parsing fails
   */
  @Test
  public void testTransformCriteriaWithIdField() throws JSONException {
    JSONObject criteria = new JSONObject();
    JSONArray criteriaArray = new JSONArray();
    
    JSONObject idCriteria = new JSONObject();
    idCriteria.put(FIELD_NAME, "id");
    idCriteria.put(VALUE, "PSD1,PSD2,PSD3");
    criteriaArray.put(idCriteria);
    
    criteria.put(CRITERIA, criteriaArray);
    
    List<String> selectedPSDs = new ArrayList<>();
    transformer.transformCriteria(criteria, selectedPSDs);
    
    assertEquals(3, selectedPSDs.size(), "Should extract 3 PSDs");
    assertTrue("Should contain PSD1", selectedPSDs.contains("PSD1"));
    assertTrue("Should contain PSD2", selectedPSDs.contains("PSD2"));
    assertTrue("Should contain PSD3", selectedPSDs.contains("PSD3"));
  }

  /**
   * Verifies that transformCriteria handles whitespace in comma-separated values.
   * 
   * @throws JSONException if JSON parsing fails
   */
  @Test
  public void testTransformCriteriaTrimsWhitespace() throws JSONException {
    JSONObject criteria = new JSONObject();
    JSONArray criteriaArray = new JSONArray();
    
    JSONObject idCriteria = new JSONObject();
    idCriteria.put(FIELD_NAME, "id");
    idCriteria.put(VALUE, "PSD1 , PSD2 , PSD3");
    criteriaArray.put(idCriteria);
    
    criteria.put(CRITERIA, criteriaArray);
    
    List<String> selectedPSDs = new ArrayList<>();
    transformer.transformCriteria(criteria, selectedPSDs);
    
    assertTrue("Should trim whitespace from PSD1", selectedPSDs.contains("PSD1"));
    assertFalse("Should not contain value with spaces", selectedPSDs.contains(" PSD2 "));
  }

  /**
   * Verifies that transformCriteria preserves non-id criteria.
   * 
   * @throws JSONException if JSON parsing fails
   */
  @Test
  public void testTransformCriteriaPreservesOtherFields() throws JSONException {
    JSONObject criteria = new JSONObject();
    JSONArray criteriaArray = new JSONArray();
    
    JSONObject otherCriteria = new JSONObject();
    otherCriteria.put(FIELD_NAME, "amount");
    otherCriteria.put(VALUE, "100");
    criteriaArray.put(otherCriteria);
    
    criteria.put(CRITERIA, criteriaArray);
    
    List<String> selectedPSDs = new ArrayList<>();
    transformer.transformCriteria(criteria, selectedPSDs);
    
    assertTrue("Should not extract from non-id fields", selectedPSDs.isEmpty());
    JSONArray resultArray = criteria.getJSONArray(CRITERIA);
    assertEquals(1, resultArray.length(), "Should preserve original criteria");
  }

  /**
   * Verifies that getAggregatorFunction wraps expression correctly.
   */
  @Test
  public void testGetAggregatorFunction() {
    String result = transformer.getAggregatorFunction("test.field");
    
    assertEquals(" hqlagg(test.field)", result, "Should wrap in hqlagg function");
  }

  /**
   * Verifies that getAggregatorFunction handles complex expressions.
   */
  @Test
  public void testGetAggregatorFunctionWithComplexExpression() {
    String result = transformer.getAggregatorFunction("CASE WHEN x > 0 THEN x ELSE 0 END");
    
    assertEquals(" hqlagg(CASE WHEN x > 0 THEN x ELSE 0 END)", result, "Should wrap complex expression");
  }

  /**
   * Verifies that transformCriteria handles empty criteria array.
   * 
   * @throws JSONException if JSON parsing fails
   */
  @Test
  public void testTransformCriteriaWithEmptyArray() throws JSONException {
    JSONObject criteria = new JSONObject();
    criteria.put(CRITERIA, new JSONArray());
    
    List<String> selectedPSDs = new ArrayList<>();
    transformer.transformCriteria(criteria, selectedPSDs);
    
    assertTrue("Should return empty list", selectedPSDs.isEmpty());
  }

  /**
   * Verifies that transformCriteria handles single PSD value (no comma).
   * 
   * @throws JSONException if JSON parsing fails
   */
  @Test
  public void testTransformCriteriaWithSingleValue() throws JSONException {
    JSONObject criteria = new JSONObject();
    JSONArray criteriaArray = new JSONArray();
    
    JSONObject idCriteria = new JSONObject();
    idCriteria.put(FIELD_NAME, "id");
    idCriteria.put(VALUE, "PSD1");
    criteriaArray.put(idCriteria);
    
    criteria.put(CRITERIA, criteriaArray);
    
    List<String> selectedPSDs = new ArrayList<>();
    transformer.transformCriteria(criteria, selectedPSDs);
    
    assertEquals(1, selectedPSDs.size(),"Should extract single PSD");
    assertEquals("PSD1", selectedPSDs.get(0), "Should be PSD1");
  }

  /**
   * Verifies that getWhereClause handles large number of PSDs with batching.
   */
  @Test
  public void testGetWhereClauseWithManyPSDs() {
    List<String> selectedPSDs = new ArrayList<>();
    for (int i = 0; i < 3000; i++) {
      selectedPSDs.add("PSD" + i);
    }
    
    StringBuffer result = transformer.getWhereClause(TRANSACTION_TYPE_INVOICE, requestParameters, 
        selectedPSDs);
    
    String resultStr = result.toString();
    assertTrue("Should contain multiple psd.id in clauses for batching", 
        resultStr.contains("psd.id in (") && resultStr.contains("or psd.id in ("));
  }

  /**
   * Verifies that getWhereClause includes business partner filter when provided.
   */
  @Test
  public void testGetWhereClauseWithBusinessPartner() {
    requestParameters.put(RECEIVED_FROM, "BP789");
    
    StringBuffer result = transformer.getWhereClause(TRANSACTION_TYPE_INVOICE, requestParameters, 
        new ArrayList<String>());
    
    assertTrue("Should include business partner filter", 
        result.toString().contains("bp.id = :businessPartnerId"));
  }
}
