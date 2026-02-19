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
 * All portions are Copyright (C) 2021-2025 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */

package org.openbravo.erpCommon.businessUtility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.accounting.coa.Element;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;

/**
 * Unit test suite for {@link AccountTree}.
 * Tests the account tree processing logic used for accounting reports.
 * Uses Mockito to mock static dependencies and ObjenesisStd for instance creation
 * without invoking constructors.
 */
@RunWith(MockitoJUnitRunner.class)
public class AccountTreeTest {

  private static final String NODE_ID_1 = "NODE001";
  private static final String NODE_ID_2 = "NODE002";
  private static final String NODE_ID_3 = "NODE003";
  private static final String PARENT_ID_ROOT = "0";
  private static final String ELEMENT_ID = "ELEM001";
  private static final String QTY_100 = "100";
  private static final String QTY_50 = "50";
  private static final String QTY_ZERO = "0";

  @Mock
  private VariablesSecureApp vars;

  @Mock
  private ConnectionProvider conn;

  @Mock
  private OBDal obDal;

  @Mock
  private ElementValue elementValue;

  @Mock
  private Element accountingElement;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<Utility> utilityStatic;
  private MockedStatic<AccountTreeData> accountTreeDataStatic;

  private ObjenesisStd objenesis;
  private AccountTree accountTree;

  /**
   * Sets up mock objects before each test.
   * Initializes static mocks for OBDal, Utility, and AccountTreeData.
   */
  @Before
  public void setUp() throws ServletException {
    objenesis = new ObjenesisStd();

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(obDal);

    utilityStatic = mockStatic(Utility.class);
    lenient().when(Utility.getContext(any(ConnectionProvider.class), any(VariablesSecureApp.class),
        anyString(), anyString())).thenReturn("ORG_TREE");

    accountTreeDataStatic = mockStatic(AccountTreeData.class);
    accountTreeDataStatic.when(() -> AccountTreeData.selectOperands(
        any(ConnectionProvider.class), anyString(), anyString(), anyString()))
        .thenReturn(new AccountTreeData[0]);
    accountTreeDataStatic.when(() -> AccountTreeData.selectOperands(
        any(ConnectionProvider.class), anyString(), anyString(), anyString(), anyInt(), anyInt()))
        .thenReturn(new AccountTreeData[0]);

    lenient().when(obDal.get(eq(ElementValue.class), anyString())).thenReturn(elementValue);
    lenient().when(elementValue.getAccountingElement()).thenReturn(accountingElement);
    lenient().when(accountingElement.getId()).thenReturn(ELEMENT_ID);
    lenient().when(elementValue.isActive()).thenReturn(true);
  }

  /**
   * Cleans up static mocks after each test to prevent leaks.
   */
  @After
  public void tearDown() {
    if (accountTreeDataStatic != null) {
      accountTreeDataStatic.close();
    }
    if (utilityStatic != null) {
      utilityStatic.close();
    }
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }

  /**
   * Tests that getAccounts returns the reportElements array.
   */
  @Test
  public void testGetAccountsReturnsReportElements() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    AccountTreeData[] testData = createTestTreeData(2);

    setPrivateField(instance, "reportElements", testData);

    AccountTreeData[] result = instance.getAccounts();

    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(NODE_ID_1, result[0].nodeId);
  }

  /**
   * Tests that getAccounts returns null when reportElements is null.
   */
  @Test
  public void testGetAccountsReturnsNullWhenEmpty() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    setPrivateField(instance, "reportElements", null);

    AccountTreeData[] result = instance.getAccounts();

    assertNull(result);
  }

  /**
   * Tests the applyShowValueCond method with "A" (Always) sign.
   * Should return the original quantity.
   */
  @Test
  public void testApplyShowValueCondWithAlways() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    BigDecimal qty = new BigDecimal("100.50");
    BigDecimal result = invokeApplyShowValueCond(instance, qty, "A", true);

    assertEquals(qty, result);
  }

  /**
   * Tests the applyShowValueCond method with "P" (Positive) sign for positive values.
   * Should return the quantity when positive.
   */
  @Test
  public void testApplyShowValueCondWithPositiveSignAndPositiveValue() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    BigDecimal qty = new BigDecimal("50.00");
    BigDecimal result = invokeApplyShowValueCond(instance, qty, "P", true);

    assertEquals(qty, result);
  }

  /**
   * Tests the applyShowValueCond method with "P" (Positive) sign for negative values.
   * Should return zero when value is negative.
   */
  @Test
  public void testApplyShowValueCondWithPositiveSignAndNegativeValue() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    BigDecimal qty = new BigDecimal("-50.00");
    BigDecimal result = invokeApplyShowValueCond(instance, qty, "P", true);

    assertEquals(BigDecimal.ZERO, result);
  }

  /**
   * Tests the applyShowValueCond method with "N" (Negative) sign for negative values.
   * Should return the quantity when negative.
   */
  @Test
  public void testApplyShowValueCondWithNegativeSignAndNegativeValue() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    BigDecimal qty = new BigDecimal("-75.00");
    BigDecimal result = invokeApplyShowValueCond(instance, qty, "N", true);

    assertEquals(qty, result);
  }

  /**
   * Tests the applyShowValueCond method with "N" (Negative) sign for positive values.
   * Should return zero when value is positive.
   */
  @Test
  public void testApplyShowValueCondWithNegativeSignAndPositiveValue() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    BigDecimal qty = new BigDecimal("75.00");
    BigDecimal result = invokeApplyShowValueCond(instance, qty, "N", true);

    assertEquals(BigDecimal.ZERO, result);
  }

  /**
   * Tests the applyShowValueCond method when isSummary is false.
   * Should return the original quantity regardless of sign.
   */
  @Test
  public void testApplyShowValueCondWhenNotSummary() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    BigDecimal qty = new BigDecimal("-100.00");
    BigDecimal result = invokeApplyShowValueCond(instance, qty, "P", false);

    assertEquals(qty, result);
  }

  /**
   * Tests the setDataQty method when reportElement is null.
   * Should return null without throwing exception.
   */
  @Test
  public void testSetDataQtyWithNullElement() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData result = invokeSetDataQty(instance, null, "D");

    assertNull(result);
  }

  /**
   * Tests the setDataQty method when accountsFacts is null.
   * Should return the original element unchanged.
   */
  @Test
  public void testSetDataQtyWithNullFacts() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    setPrivateField(instance, "accountsFacts", null);

    AccountTreeData element = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    AccountTreeData result = invokeSetDataQty(instance, element, "D");

    assertEquals(element, result);
  }

  /**
   * Tests the setDataQty method when accountsFacts is empty.
   * Should return the original element unchanged.
   */
  @Test
  public void testSetDataQtyWithEmptyFacts() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    setPrivateField(instance, "accountsFacts", new AccountTreeData[0]);

    AccountTreeData element = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    AccountTreeData result = invokeSetDataQty(instance, element, "D");

    assertEquals(element, result);
  }

  /**
   * Tests the setDataQty method with Debit sign.
   * Should use qty field from accountsFacts.
   */
  @Test
  public void testSetDataQtyWithDebitSign() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData fact = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    fact.id = NODE_ID_1;
    fact.qty = QTY_100;
    fact.qtyRef = QTY_50;
    fact.qtycredit = "200";
    fact.qtycreditRef = "150";

    setPrivateField(instance, "accountsFacts", new AccountTreeData[]{fact});

    AccountTreeData element = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    element.id = NODE_ID_1;
    element.showvaluecond = "A";
    element.issummary = "N";

    AccountTreeData result = invokeSetDataQty(instance, element, "D");

    assertEquals(QTY_100, result.qtyOperation);
    assertEquals(QTY_50, result.qtyOperationRef);
  }

  /**
   * Tests the setDataQty method with Credit sign.
   * Should use qtycredit field from accountsFacts.
   */
  @Test
  public void testSetDataQtyWithCreditSign() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData fact = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    fact.id = NODE_ID_1;
    fact.qty = QTY_100;
    fact.qtyRef = QTY_50;
    fact.qtycredit = "200";
    fact.qtycreditRef = "150";

    setPrivateField(instance, "accountsFacts", new AccountTreeData[]{fact});

    AccountTreeData element = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    element.id = NODE_ID_1;
    element.showvaluecond = "A";
    element.issummary = "N";

    AccountTreeData result = invokeSetDataQty(instance, element, "C");

    assertEquals("200", result.qtyOperation);
    assertEquals("150", result.qtyOperationRef);
  }

  /**
   * Tests the hasOperand method when operand exists.
   */
  @Test
  public void testHasOperandReturnsTrue() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] forms = new AccountTreeData[2];
    forms[0] = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    forms[0].id = NODE_ID_1;
    forms[1] = createAccountTreeDataElement(NODE_ID_2, NODE_ID_1);
    forms[1].id = NODE_ID_2;

    boolean result = invokeHasOperand(instance, NODE_ID_2, forms);

    assertTrue(result);
  }

  /**
   * Tests the hasOperand method when operand does not exist.
   */
  @Test
  public void testHasOperandReturnsFalse() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] forms = new AccountTreeData[1];
    forms[0] = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    forms[0].id = NODE_ID_1;

    boolean result = invokeHasOperand(instance, "NONEXISTENT", forms);

    assertTrue(!result);
  }

  /**
   * Tests the hasOperand method with null index.
   * Should return false.
   */
  @Test
  public void testHasOperandWithNullIndex() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] forms = new AccountTreeData[1];
    forms[0] = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    forms[0].id = NODE_ID_1;

    boolean result = invokeHasOperand(instance, null, forms);

    assertTrue(!result);
  }

  /**
   * Tests the updateTreeQuantitiesSign method with null accountsTree.
   * Should return null.
   */
  @Test
  public void testUpdateTreeQuantitiesSignWithNullTree() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    setPrivateField(instance, "accountsTree", null);

    AccountTreeData[] result = invokeUpdateTreeQuantitiesSign(instance, null, 0, "D");

    assertNull(result);
  }

  /**
   * Tests the updateTreeQuantitiesSign method with empty accountsTree.
   * Should return empty array.
   */
  @Test
  public void testUpdateTreeQuantitiesSignWithEmptyTree() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    setPrivateField(instance, "accountsTree", new AccountTreeData[0]);

    AccountTreeData[] result = invokeUpdateTreeQuantitiesSign(instance, null, 0, "D");

    assertNotNull(result);
    assertEquals(0, result.length);
  }

  /**
   * Tests the nodeIn method when node is in the list.
   */
  @Test
  public void testNodeInReturnsTrue() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    String[] nodes = {NODE_ID_1, NODE_ID_2, NODE_ID_3};
    boolean result = invokeNodeIn(instance, NODE_ID_2, nodes);

    assertTrue(result);
  }

  /**
   * Tests the nodeIn method when node is not in the list.
   */
  @Test
  public void testNodeInReturnsFalse() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    String[] nodes = {NODE_ID_1, NODE_ID_2};
    boolean result = invokeNodeIn(instance, NODE_ID_3, nodes);

    assertTrue(!result);
  }

  /**
   * Tests the isAccountLevelLower method with "D" level and "S" element level.
   * Should return false as "S" is not lower than "D".
   */
  @Test
  public void testIsAccountLevelLowerWithDAndS() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData account = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    account.elementlevel = "S";

    boolean result = invokeIsAccountLevelLower(instance, "D", account);

    assertTrue(!result);
  }

  /**
   * Tests the isAccountLevelLower method with "D" level and "D" element level.
   * Should return true.
   */
  @Test
  public void testIsAccountLevelLowerWithDAndD() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData account = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    account.elementlevel = "D";

    boolean result = invokeIsAccountLevelLower(instance, "D", account);

    assertTrue(result);
  }

  /**
   * Tests the isAccountLevelLower method with non-D level.
   * Should always return true.
   */
  @Test
  public void testIsAccountLevelLowerWithNonDLevel() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData account = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    account.elementlevel = "S";

    boolean result = invokeIsAccountLevelLower(instance, "X", account);

    assertTrue(result);
  }

  /**
   * Tests the filter method when reportElements is null.
   * Should not throw exception.
   */
  @Test
  public void testFilterWithNullReportElements() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    setPrivateField(instance, "reportElements", null);
    setPrivateField(instance, "reportNodes", new String[]{NODE_ID_1});

    instance.filter(true, "1", true);

    // Should complete without exception
    assertNull(instance.getAccounts());
  }

  /**
   * Tests the filterSVC method when reportElements is null.
   * Should not throw exception.
   */
  @Test
  public void testFilterSVCWithNullReportElements() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    setPrivateField(instance, "reportElements", null);

    instance.filterSVC();

    // Should complete without exception
    assertNull(instance.getAccounts());
  }

  /**
   * Tests the filterSVC method when reportElements is empty.
   * Should not throw exception.
   */
  @Test
  public void testFilterSVCWithEmptyReportElements() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    setPrivateField(instance, "reportElements", new AccountTreeData[0]);

    instance.filterSVC();

    AccountTreeData[] result = instance.getAccounts();
    assertNotNull(result);
    assertEquals(0, result.length);
  }

  /**
   * Tests the filterSVC method resets qty for children when parent has svcreset=Y.
   */
  @Test
  public void testFilterSVCResetsChildQuantities() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] elements = new AccountTreeData[3];

    elements[0] = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    elements[0].elementLevel = "0";
    elements[0].svcreset = "Y";
    elements[0].svcresetref = "N";
    elements[0].qty = QTY_100;
    elements[0].qtyRef = QTY_50;

    elements[1] = createAccountTreeDataElement(NODE_ID_2, NODE_ID_1);
    elements[1].elementLevel = "1";
    elements[1].svcreset = "N";
    elements[1].svcresetref = "N";
    elements[1].qty = "200";
    elements[1].qtyRef = "150";

    elements[2] = createAccountTreeDataElement(NODE_ID_3, NODE_ID_2);
    elements[2].elementLevel = "2";
    elements[2].svcreset = "N";
    elements[2].svcresetref = "N";
    elements[2].qty = "300";
    elements[2].qtyRef = "250";

    setPrivateField(instance, "reportElements", elements);

    instance.filterSVC();

    AccountTreeData[] result = instance.getAccounts();
    assertEquals(QTY_100, result[0].qty);
    assertEquals("0.0", result[1].qty);
    assertEquals("0.0", result[2].qty);
  }

  /**
   * Tests the filterSVC method resets qtyRef for children when parent has svcresetref=Y.
   */
  @Test
  public void testFilterSVCResetsRefQuantities() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] elements = new AccountTreeData[2];

    elements[0] = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    elements[0].elementLevel = "0";
    elements[0].svcreset = "N";
    elements[0].svcresetref = "Y";
    elements[0].qty = QTY_100;
    elements[0].qtyRef = QTY_50;

    elements[1] = createAccountTreeDataElement(NODE_ID_2, NODE_ID_1);
    elements[1].elementLevel = "1";
    elements[1].svcreset = "N";
    elements[1].svcresetref = "N";
    elements[1].qty = "200";
    elements[1].qtyRef = "150";

    setPrivateField(instance, "reportElements", elements);

    instance.filterSVC();

    AccountTreeData[] result = instance.getAccounts();
    assertEquals(QTY_50, result[0].qtyRef);
    assertEquals("0.0", result[1].qtyRef);
  }

  /**
   * Tests the filterStructure method with null reportElements.
   * Should return null.
   */
  @Test
  public void testFilterStructureWithNullReportElements() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    setPrivateField(instance, "reportElements", null);

    AccountTreeData[] result = instance.filterStructure(new String[]{NODE_ID_1}, true, "1", true);

    assertNull(result);
  }

  /**
   * Tests the filterStructure method with empty reportElements.
   * Should return empty array.
   */
  @Test
  public void testFilterStructureWithEmptyReportElements() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    setPrivateField(instance, "reportElements", new AccountTreeData[0]);

    AccountTreeData[] result = instance.filterStructure(new String[]{NODE_ID_1}, true, "1", true);

    assertNotNull(result);
    assertEquals(0, result.length);
  }

  /**
   * Tests the filterStructure method filters by showelement=Y.
   */
  @Test
  public void testFilterStructureFiltersShowElement() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] elements = new AccountTreeData[2];

    elements[0] = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    elements[0].elementlevel = "1";
    elements[0].showelement = "Y";
    elements[0].showvaluecond = "A";
    elements[0].qty = QTY_100;
    elements[0].qtyRef = QTY_50;
    elements[0].isalwaysshown = "N";

    elements[1] = createAccountTreeDataElement(NODE_ID_2, PARENT_ID_ROOT);
    elements[1].elementlevel = "1";
    elements[1].showelement = "N";
    elements[1].showvaluecond = "A";
    elements[1].qty = "200";
    elements[1].qtyRef = "150";
    elements[1].isalwaysshown = "N";

    setPrivateField(instance, "reportElements", elements);

    AccountTreeData[] result = instance.filterStructure(new String[]{PARENT_ID_ROOT}, false, "1", true);

    assertEquals(1, result.length);
    assertEquals(NODE_ID_1, result[0].nodeId);
  }

  /**
   * Tests the filterStructure method includes isalwaysshown=Y elements.
   */
  @Test
  public void testFilterStructureIncludesAlwaysShown() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] elements = new AccountTreeData[1];

    elements[0] = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    elements[0].elementlevel = "1";
    elements[0].showelement = "Y";
    elements[0].showvaluecond = "A";
    elements[0].qty = QTY_ZERO;
    elements[0].qtyRef = QTY_ZERO;
    elements[0].isalwaysshown = "Y";

    setPrivateField(instance, "reportElements", elements);

    AccountTreeData[] result = instance.filterStructure(new String[]{PARENT_ID_ROOT}, true, "1", true);

    assertEquals(1, result.length);
    assertNull(result[0].qty);
    assertNull(result[0].qtyRef);
  }

  /**
   * Tests the applySignAsPerParent method with null accountsTree.
   * Should not throw exception.
   */
  @Test
  public void testApplySignAsPerParentWithNullTree() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    setPrivateField(instance, "accountsTree", null);

    invokeApplySignAsPerParent(instance);

    // Should complete without exception
  }

  /**
   * Tests the applySignAsPerParent method with empty accountsTree.
   * Should not throw exception.
   */
  @Test
  public void testApplySignAsPerParentWithEmptyTree() throws Exception {
    AccountTree instance = createAccountTreeInstance();
    setPrivateField(instance, "accountsTree", new AccountTreeData[0]);

    invokeApplySignAsPerParent(instance);

    // Should complete without exception
  }

  /**
   * Tests the applyShowValueCond method with zero quantity.
   * Zero should be returned as positive (compareTo > 0 is false).
   */
  @Test
  public void testApplyShowValueCondWithZeroQuantity() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    BigDecimal qty = BigDecimal.ZERO;
    BigDecimal result = invokeApplyShowValueCond(instance, qty, "P", true);

    assertEquals(BigDecimal.ZERO, result);
  }

  /**
   * Tests the applyShowValueCond method with unknown sign value.
   * Should return the original quantity.
   */
  @Test
  public void testApplyShowValueCondWithUnknownSign() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    BigDecimal qty = new BigDecimal("123.45");
    BigDecimal result = invokeApplyShowValueCond(instance, qty, "X", true);

    assertEquals(qty, result);
  }

  /**
   * Tests updateTreeQuantitiesSign builds correct tree structure.
   */
  @Test
  public void testUpdateTreeQuantitiesSignBuildsTree() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] tree = new AccountTreeData[2];
    tree[0] = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    tree[0].accountsign = "D";
    tree[1] = createAccountTreeDataElement(NODE_ID_2, NODE_ID_1);
    tree[1].accountsign = "D";

    setPrivateField(instance, "accountsTree", tree);
    setPrivateField(instance, "accountsFacts", new AccountTreeData[0]);

    AccountTreeData[] result = invokeUpdateTreeQuantitiesSign(instance, PARENT_ID_ROOT, 0, "D");

    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals("0", result[0].elementLevel);
    assertEquals("1", result[1].elementLevel);
  }

  /**
   * Tests nodeIn with empty array.
   */
  @Test
  public void testNodeInWithEmptyArray() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    String[] nodes = {};
    boolean result = invokeNodeIn(instance, NODE_ID_1, nodes);

    assertTrue(!result);
  }

  /**
   * Tests nodeIn with single element array matching.
   */
  @Test
  public void testNodeInWithSingleElementMatch() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    String[] nodes = {NODE_ID_1};
    boolean result = invokeNodeIn(instance, NODE_ID_1, nodes);

    assertTrue(result);
  }

  /**
   * Tests hasOperand with empty forms array.
   */
  @Test
  public void testHasOperandWithEmptyForms() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] forms = new AccountTreeData[0];
    boolean result = invokeHasOperand(instance, NODE_ID_1, forms);

    assertTrue(!result);
  }

  /**
   * Tests that applySignAsPerParent keeps signs unchanged when they match parent.
   */
  @Test
  public void testApplySignAsPerParentKeepsMatchingSign() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] tree = new AccountTreeData[2];

    tree[0] = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    tree[0].id = NODE_ID_1;
    tree[0].accountsign = "D";

    tree[1] = createAccountTreeDataElement(NODE_ID_2, NODE_ID_1);
    tree[1].id = NODE_ID_2;
    tree[1].accountsign = "D";

    setPrivateField(instance, "accountsTree", tree);

    invokeApplySignAsPerParent(instance);

    assertEquals("D", tree[0].accountsign);
    assertEquals("D", tree[1].accountsign);
  }

  /**
   * Tests the filterSVC resets values correctly when levels are equal.
   */
  @Test
  public void testFilterSVCWithSameLevelElements() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] elements = new AccountTreeData[2];

    elements[0] = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    elements[0].elementLevel = "1";
    elements[0].svcreset = "Y";
    elements[0].svcresetref = "N";
    elements[0].qty = QTY_100;
    elements[0].qtyRef = QTY_50;

    elements[1] = createAccountTreeDataElement(NODE_ID_2, NODE_ID_1);
    elements[1].elementLevel = "1";
    elements[1].svcreset = "N";
    elements[1].svcresetref = "N";
    elements[1].qty = "200";
    elements[1].qtyRef = "150";

    setPrivateField(instance, "reportElements", elements);

    instance.filterSVC();

    AccountTreeData[] result = instance.getAccounts();
    assertEquals(QTY_100, result[0].qty);
    assertEquals("200", result[1].qty);
  }

  /**
   * Tests filterStructure with null strLevel parameter.
   */
  @Test
  public void testFilterStructureWithNullLevel() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] elements = new AccountTreeData[1];
    elements[0] = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    elements[0].showelement = "Y";
    elements[0].qty = QTY_100;
    elements[0].qtyRef = QTY_50;
    elements[0].isalwaysshown = "N";

    setPrivateField(instance, "reportElements", elements);

    AccountTreeData[] result = instance.filterStructure(new String[]{PARENT_ID_ROOT}, false, null, true);

    assertNotNull(result);
    assertEquals(1, result.length);
  }

  /**
   * Tests filterStructure with empty strLevel parameter.
   */
  @Test
  public void testFilterStructureWithEmptyLevel() throws Exception {
    AccountTree instance = createAccountTreeInstance();

    AccountTreeData[] elements = new AccountTreeData[1];
    elements[0] = createAccountTreeDataElement(NODE_ID_1, PARENT_ID_ROOT);
    elements[0].showelement = "Y";
    elements[0].qty = QTY_100;
    elements[0].qtyRef = QTY_50;
    elements[0].isalwaysshown = "N";

    setPrivateField(instance, "reportElements", elements);

    AccountTreeData[] result = instance.filterStructure(new String[]{PARENT_ID_ROOT}, false, "", true);

    assertNotNull(result);
    assertEquals(1, result.length);
  }

  // ==================== Helper Methods ====================

  /**
   * Creates an AccountTree instance using Objenesis without invoking constructor.
   */
  private AccountTree createAccountTreeInstance() throws Exception {
    AccountTree instance = objenesis.newInstance(AccountTree.class);
    setPrivateField(instance, "vars", vars);
    setPrivateField(instance, "conn", conn);
    setPrivateField(instance, "accountsTree", new AccountTreeData[0]);
    setPrivateField(instance, "accountsFacts", new AccountTreeData[0]);
    setPrivateField(instance, "reportElements", new AccountTreeData[0]);
    setPrivateField(instance, "reportNodes", new String[]{NODE_ID_1});
    setPrivateField(instance, "resetFlag", false);
    setPrivateField(instance, "recursiveOperands", false);
    return instance;
  }

  /**
   * Creates an array of test AccountTreeData elements.
   */
  private AccountTreeData[] createTestTreeData(int count) {
    AccountTreeData[] data = new AccountTreeData[count];
    for (int i = 0; i < count; i++) {
      String nodeId = "NODE00" + (i + 1);
      String parentId = i == 0 ? PARENT_ID_ROOT : "NODE00" + i;
      data[i] = createAccountTreeDataElement(nodeId, parentId);
    }
    return data;
  }

  /**
   * Creates a single AccountTreeData element with default values.
   */
  private AccountTreeData createAccountTreeDataElement(String nodeId, String parentId) {
    AccountTreeData data = new AccountTreeData();
    data.nodeId = nodeId;
    data.parentId = parentId;
    data.id = nodeId;
    data.name = "Account " + nodeId;
    data.issummary = "N";
    data.accountsign = "D";
    data.showelement = "Y";
    data.elementLevel = "0";
    data.qty = QTY_ZERO;
    data.qtyRef = QTY_ZERO;
    data.qtyOperation = QTY_ZERO;
    data.qtyOperationRef = QTY_ZERO;
    data.qtycredit = QTY_ZERO;
    data.qtycreditRef = QTY_ZERO;
    data.showvaluecond = "A";
    data.elementlevel = "D";
    data.calculated = "N";
    data.svcreset = "N";
    data.svcresetref = "N";
    data.isalwaysshown = "N";
    data.sign = "1";
    return data;
  }

  /**
   * Sets a private field value using reflection.
   */
  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = AccountTree.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  /**
   * Invokes the private applyShowValueCond method.
   */
  private BigDecimal invokeApplyShowValueCond(AccountTree instance, BigDecimal qty, String sign,
      boolean isSummary) throws Exception {
    Method method = AccountTree.class.getDeclaredMethod("applyShowValueCond", BigDecimal.class,
        String.class, boolean.class);
    method.setAccessible(true);
    return (BigDecimal) method.invoke(instance, qty, sign, isSummary);
  }

  /**
   * Invokes the private setDataQty method.
   */
  private AccountTreeData invokeSetDataQty(AccountTree instance, AccountTreeData reportElement,
      String isDebitCredit) throws Exception {
    Method method = AccountTree.class.getDeclaredMethod("setDataQty", AccountTreeData.class,
        String.class);
    method.setAccessible(true);
    return (AccountTreeData) method.invoke(instance, reportElement, isDebitCredit);
  }

  /**
   * Invokes the private hasOperand method.
   */
  private boolean invokeHasOperand(AccountTree instance, String indice, AccountTreeData[] forms)
      throws Exception {
    Method method = AccountTree.class.getDeclaredMethod("hasOperand", String.class,
        AccountTreeData[].class);
    method.setAccessible(true);
    return (Boolean) method.invoke(instance, indice, forms);
  }

  /**
   * Invokes the private updateTreeQuantitiesSign method.
   */
  private AccountTreeData[] invokeUpdateTreeQuantitiesSign(AccountTree instance, String rootElement,
      int level, String accountSign) throws Exception {
    Method method = AccountTree.class.getDeclaredMethod("updateTreeQuantitiesSign", String.class,
        int.class, String.class);
    method.setAccessible(true);
    return (AccountTreeData[]) method.invoke(instance, rootElement, level, accountSign);
  }

  /**
   * Invokes the private nodeIn method.
   */
  private boolean invokeNodeIn(AccountTree instance, String node, String[] listOfNodes)
      throws Exception {
    Method method = AccountTree.class.getDeclaredMethod("nodeIn", String.class, String[].class);
    method.setAccessible(true);
    return (Boolean) method.invoke(instance, node, listOfNodes);
  }

  /**
   * Invokes the private isAccountLevelLower method.
   */
  private boolean invokeIsAccountLevelLower(AccountTree instance, String reportAccountLevel,
      AccountTreeData accountToBeAdded) throws Exception {
    Method method = AccountTree.class.getDeclaredMethod("isAccountLevelLower", String.class,
        AccountTreeData.class);
    method.setAccessible(true);
    return (Boolean) method.invoke(instance, reportAccountLevel, accountToBeAdded);
  }

  /**
   * Invokes the private applySignAsPerParent method.
   */
  private void invokeApplySignAsPerParent(AccountTree instance) throws Exception {
    Method method = AccountTree.class.getDeclaredMethod("applySignAsPerParent");
    method.setAccessible(true);
    method.invoke(instance);
  }
}
