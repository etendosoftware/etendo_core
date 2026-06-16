/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.utility.reporting.printing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * Tests for {@link PrintControllerEmailSupport}.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerEmailSupportTest {

  private static final String BP1_ID = "bp1";
  private static final String BP2_ID = "bp2";
  private static final String CONTACT_EMAIL = "c@a.com";
  private static final String CONTACT1_EMAIL = "c1@a.com";
  private static final String CONTACT2_EMAIL = "c2@a.com";
  private static final String SALESREP_EMAIL = "s@a.com";
  private static final String SALESREP1_EMAIL = "s1@a.com";
  private static final String SALESREP2_EMAIL = "s2@a.com";
  private static final String MULTI_CUSTOMER_FLAG = "multiCustomerFlag";

  // -------------------------------------------------------------------------
  // updateEnvironmentInfo
  // -------------------------------------------------------------------------
  /** Null pocData does nothing to the checks map. */
  @Test
  public void testUpdateEnvironmentInfo_nullPocData_doesNothing() {
    HashMap<String, Boolean> checks = new HashMap<>();
    PrintControllerEmailSupport.updateEnvironmentInfo(null, checks);
    assertTrue(checks.isEmpty());
  }
  /** Single document sets all multi-flags to false. */
  @Test
  public void testUpdateEnvironmentInfo_singleDocument_allChecksFalse() {
    PocData doc = pocData(BP1_ID, CONTACT_EMAIL, SALESREP_EMAIL);
    HashMap<String, Boolean> checks = new HashMap<>();

    PrintControllerEmailSupport.updateEnvironmentInfo(new PocData[]{ doc }, checks);

    assertFalse(checks.get(PrintController.CHECK_MORE_THAN_ONE_DOCUMENT));
    assertFalse(checks.get(PrintController.CHECK_MORE_THAN_ONE_CUSTOMER));
    assertFalse(checks.get(PrintController.CHECK_MORE_THAN_ONE_SALES_REP));
  }
  /** Two different bpartner IDs sets moreThanOneCustomer to true. */
  @Test
  public void testUpdateEnvironmentInfo_twoDifferentCustomers_moreThanOneCustomerTrue() {
    PocData doc1 = pocData(BP1_ID, CONTACT1_EMAIL, SALESREP_EMAIL);
    PocData doc2 = pocData(BP2_ID, CONTACT2_EMAIL, SALESREP_EMAIL);
    HashMap<String, Boolean> checks = new HashMap<>();

    PrintControllerEmailSupport.updateEnvironmentInfo(new PocData[]{ doc1, doc2 }, checks);

    assertTrue(checks.get(PrintController.CHECK_MORE_THAN_ONE_DOCUMENT));
    assertTrue(checks.get(PrintController.CHECK_MORE_THAN_ONE_CUSTOMER));
    assertFalse(checks.get(PrintController.CHECK_MORE_THAN_ONE_SALES_REP));
  }
  /** Two different salesrep emails sets moreThanOneSalesRep to true. */
  @Test
  public void testUpdateEnvironmentInfo_twoDifferentSalesReps_moreThanOneSalesRepTrue() {
    PocData doc1 = pocData(BP1_ID, CONTACT_EMAIL, SALESREP1_EMAIL);
    PocData doc2 = pocData(BP1_ID, CONTACT_EMAIL, SALESREP2_EMAIL);
    HashMap<String, Boolean> checks = new HashMap<>();

    PrintControllerEmailSupport.updateEnvironmentInfo(new PocData[]{ doc1, doc2 }, checks);

    assertTrue(checks.get(PrintController.CHECK_MORE_THAN_ONE_DOCUMENT));
    assertFalse(checks.get(PrintController.CHECK_MORE_THAN_ONE_CUSTOMER));
    assertTrue(checks.get(PrintController.CHECK_MORE_THAN_ONE_SALES_REP));
  }
  /** Null elements in the array are skipped without error. */
  @Test
  public void testUpdateEnvironmentInfo_nullElementInArray_isSkipped() {
    PocData doc = pocData(BP1_ID, CONTACT_EMAIL, SALESREP_EMAIL);
    HashMap<String, Boolean> checks = new HashMap<>();

    PrintControllerEmailSupport.updateEnvironmentInfo(new PocData[]{ doc, null }, checks);

    assertFalse(checks.get(PrintController.CHECK_MORE_THAN_ONE_DOCUMENT));
    assertFalse(checks.get(PrintController.CHECK_MORE_THAN_ONE_CUSTOMER));
    assertFalse(checks.get(PrintController.CHECK_MORE_THAN_ONE_SALES_REP));
  }
  /** Null email fields do not throw a NullPointerException. */
  @Test
  public void testUpdateEnvironmentInfo_nullEmailFields_doesNotThrow() {
    PocData doc1 = pocData(BP1_ID, null, null);
    PocData doc2 = pocData(BP1_ID, null, null);
    HashMap<String, Boolean> checks = new HashMap<>();

    PrintControllerEmailSupport.updateEnvironmentInfo(new PocData[]{ doc1, doc2 }, checks);

    assertTrue(checks.get(PrintController.CHECK_MORE_THAN_ONE_DOCUMENT));
    assertFalse(checks.get(PrintController.CHECK_MORE_THAN_ONE_CUSTOMER));
    assertFalse(checks.get(PrintController.CHECK_MORE_THAN_ONE_SALES_REP));
  }

  // -------------------------------------------------------------------------
  // getHiddenTags
  // -------------------------------------------------------------------------
  /** Null pocData returns an empty discard array. */
  @Test
  public void testGetHiddenTags_nullPocData_returnsEmptyArray() {
    String[] result = PrintControllerEmailSupport.getHiddenTags(
        null, null, mock(VariablesSecureApp.class), new HashMap<>(), 1);

    assertNotNull(result);
    assertEquals(0, result.length);
  }
  /** Single customer and single salesrep discards multipleCustomer row, flag, and view tag. */
  @Test
  public void testGetHiddenTags_singleCustomerSingleSalesRep_noAttachment_discardMultipleCustomerAndView() {
    PocData doc = pocData(BP1_ID, CONTACT_EMAIL, SALESREP_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    String[] result = PrintControllerEmailSupport.getHiddenTags(
        new PocData[]{ doc }, null, vars, new HashMap<>(), 1);

    assertContains(result, "multipleCustomer");
    assertContains(result, "multipleCustomer_bottomMargin");
    assertContains(result, MULTI_CUSTOMER_FLAG);
    assertContains(result, "view");
  }
  /** More than one customer but single salesrep discards the salesrep count tags. */
  @Test
  public void testGetHiddenTags_moreThanOneCustomer_singleSalesRep_discardsSalesRepTags() {
    PocData doc1 = pocData(BP1_ID, CONTACT1_EMAIL, SALESREP_EMAIL);
    PocData doc2 = pocData(BP2_ID, CONTACT2_EMAIL, SALESREP_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    String[] result = PrintControllerEmailSupport.getHiddenTags(
        new PocData[]{ doc1, doc2 }, null, vars, new HashMap<>(), 1);

    assertContains(result, "multSalesRep");
    assertContains(result, "multSalesRepCount");
  }
  /** Single customer with more than one salesrep discards replyTo and multiCustomerFlag. */
  @Test
  public void testGetHiddenTags_singleCustomer_moreThanOneSalesRep_discardsReplyToAndFlag() {
    PocData doc1 = pocData(BP1_ID, CONTACT_EMAIL, SALESREP1_EMAIL);
    PocData doc2 = pocData(BP1_ID, CONTACT_EMAIL, SALESREP2_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    String[] result = PrintControllerEmailSupport.getHiddenTags(
        new PocData[]{ doc1, doc2 }, null, vars, new HashMap<>(), 1);

    assertContains(result, "replyTo");
    assertContains(result, "replyTo_bottomMargin");
    assertContains(result, MULTI_CUSTOMER_FLAG);
  }
  /** Multi-customer mode keeps multiCustomerFlag in the DOM. */
  @Test
  public void testGetHiddenTags_moreThanOneCustomer_doesNotDiscardMultiCustomerFlag() {
    PocData doc1 = pocData(BP1_ID, CONTACT1_EMAIL, SALESREP_EMAIL);
    PocData doc2 = pocData(BP2_ID, CONTACT2_EMAIL, SALESREP_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    String[] result = PrintControllerEmailSupport.getHiddenTags(
        new PocData[]{ doc1, doc2 }, null, vars, new HashMap<>(), 1);

    for (String tag : result) {
      assertFalse("multiCustomerFlag must not be discarded in multi-customer mode",
          MULTI_CUSTOMER_FLAG.equals(tag));
    }
  }
  /** More than one customer and more than one salesrep discards replyTo. */
  @Test
  public void testGetHiddenTags_moreThanOneCustomerAndSalesRep_discardsReplyTo() {
    PocData doc1 = pocData(BP1_ID, CONTACT1_EMAIL, SALESREP1_EMAIL);
    PocData doc2 = pocData(BP2_ID, CONTACT2_EMAIL, SALESREP2_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    String[] result = PrintControllerEmailSupport.getHiddenTags(
        new PocData[]{ doc1, doc2 }, null, vars, new HashMap<>(), 1);

    assertContains(result, "replyTo");
    assertContains(result, "replyTo_bottomMargin");
  }
  /** More than one distinct document type appends the discardSelect tag. */
  @Test
  public void testGetHiddenTags_differentDocTypesCountGreaterThanOne_appendsDiscardSelect() {
    PocData doc = pocData(BP1_ID, CONTACT_EMAIL, SALESREP_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    String[] result = PrintControllerEmailSupport.getHiddenTags(
        new PocData[]{ doc }, null, vars, new HashMap<>(), 2);

    assertContains(result, "discardSelect");
  }
  /** Attached content prevents the view tag from being discarded. */
  @Test
  public void testGetHiddenTags_withAttachedContent_doesNotAppendView() {
    PocData doc = pocData(BP1_ID, CONTACT_EMAIL, SALESREP_EMAIL);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    List<AttachContent> content = Arrays.asList(mock(AttachContent.class));

    String[] result = PrintControllerEmailSupport.getHiddenTags(
        new PocData[]{ doc }, content, vars, new HashMap<>(), 1);

    for (String tag : result) {
      assertFalse("view tag should not be present", "view".equals(tag));
    }
  }
  /** getHiddenTags populates the checks map with the customer and salesrep flags. */
  @Test
  public void testGetHiddenTags_updatesChecksMap() {
    PocData doc1 = pocData(BP1_ID, CONTACT1_EMAIL, SALESREP_EMAIL);
    PocData doc2 = pocData(BP2_ID, CONTACT2_EMAIL, SALESREP_EMAIL);
    HashMap<String, Boolean> checks = new HashMap<>();
    VariablesSecureApp vars = mock(VariablesSecureApp.class);

    PrintControllerEmailSupport.getHiddenTags(new PocData[]{ doc1, doc2 }, null, vars, checks, 1);

    assertTrue(checks.get(PrintController.CHECK_MORE_THAN_ONE_CUSTOMER));
    assertFalse(checks.get(PrintController.CHECK_MORE_THAN_ONE_SALES_REP));
  }

  // -------------------------------------------------------------------------
  // helpers
  // -------------------------------------------------------------------------

  private static PocData pocData(String bpartnerId, String contactEmail, String salesrepEmail) {
    PocData d = new PocData();
    d.bpartnerId = bpartnerId;
    d.contactEmail = contactEmail;
    d.salesrepEmail = salesrepEmail;
    return d;
  }

  private static void assertContains(String[] array, String value) {
    for (String s : array) {
      if (value.equals(s)) {
        return;
      }
    }
    throw new AssertionError(
        "Expected array to contain \"" + value + "\" but was: " + Arrays.toString(array));
  }
}
