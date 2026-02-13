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
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */

package org.openbravo.erpCommon.businessUtility;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
 * Tests use Mockito to stub static dependencies and internal method behavior.
 */
@RunWith(MockitoJUnitRunner.class)
public class AccountTreeTest {

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

  private static final String TEST_ELEMENT_ID = "TEST_ELEMENT_ID";
  private static final String TEST_ACCOUNTING_ELEMENT_ID = "TEST_ACC_ELEMENT_ID";
  private static final String TEST_ORG_TREE = "'0','100'";
  private static final String TEST_USER_CLIENT = "'0','1000000'";

  @Before
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    utilityStatic = mockStatic(Utility.class);
    accountTreeDataStatic = mockStatic(AccountTreeData.class);

    obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
    lenient().when(obDal.get(ElementValue.class, TEST_ELEMENT_ID)).thenReturn(elementValue);
    lenient().when(elementValue.getAccountingElement()).thenReturn(accountingElement);
    lenient().when(accountingElement.getId()).thenReturn(TEST_ACCOUNTING_ELEMENT_ID);

    utilityStatic.when(() -> Utility.getContext(any(ConnectionProvider.class),
        any(VariablesSecureApp.class), eq("#AccessibleOrgTree"), anyString()))
        .thenReturn(TEST_ORG_TREE);
    utilityStatic.when(() -> Utility.getContext(any(ConnectionProvider.class),
        any(VariablesSecureApp.class), eq("#User_Client"), anyString()))
        .thenReturn(TEST_USER_CLIENT);
  }

  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
    if (utilityStatic != null) {
      utilityStatic.close();
    }
    if (accountTreeDataStatic != null) {
      accountTreeDataStatic.close();
    }
  }

  // =========================================================================
  // Tests for applyShowValueCond (private method accessed via reflection)
  // =========================================================================

  @Test
  public void testApplyShowValueCondAlgebraicReturnsOriginalValue() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method applyShowValueCond = getApplyShowValueCondMethod();

    BigDecimal qty = new BigDecimal("100.50");
    BigDecimal result = (BigDecimal) applyShowValueCond.invoke(accountTree, qty, "A", true);

    assertEquals(qty, result);
  }

  @Test
  public void testApplyShowValueCondPositiveWithPositiveQuantity() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method applyShowValueCond = getApplyShowValueCondMethod();

    BigDecimal qty = new BigDecimal("100.50");
    BigDecimal result = (BigDecimal) applyShowValueCond.invoke(accountTree, qty, "P", true);

    assertEquals(qty, result);
  }

  @Test
  public void testApplyShowValueCondPositiveWithNegativeQuantityReturnsZero() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method applyShowValueCond = getApplyShowValueCondMethod();

    BigDecimal qty = new BigDecimal("-100.50");
    BigDecimal result = (BigDecimal) applyShowValueCond.invoke(accountTree, qty, "P", true);

    assertEquals(BigDecimal.ZERO, result);
  }

  @Test
  public void testApplyShowValueCondNegativeWithNegativeQuantity() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method applyShowValueCond = getApplyShowValueCondMethod();

    BigDecimal qty = new BigDecimal("-100.50");
    BigDecimal result = (BigDecimal) applyShowValueCond.invoke(accountTree, qty, "N", true);

    assertEquals(qty, result);
  }

  @Test
  public void testApplyShowValueCondNegativeWithPositiveQuantityReturnsZero() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method applyShowValueCond = getApplyShowValueCondMethod();

    BigDecimal qty = new BigDecimal("100.50");
    BigDecimal result = (BigDecimal) applyShowValueCond.invoke(accountTree, qty, "N", true);

    assertEquals(BigDecimal.ZERO, result);
  }

  @Test
  public void testApplyShowValueCondNonSummaryIgnoresSign() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method applyShowValueCond = getApplyShowValueCondMethod();

    BigDecimal qty = new BigDecimal("-100.50");
    BigDecimal result = (BigDecimal) applyShowValueCond.invoke(accountTree, qty, "P", false);

    assertEquals(qty, result);
  }

  @Test
  public void testApplyShowValueCondZeroQuantityWithPositiveCondition() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method applyShowValueCond = getApplyShowValueCondMethod();

    BigDecimal qty = BigDecimal.ZERO;
    BigDecimal result = (BigDecimal) applyShowValueCond.invoke(accountTree, qty, "P", true);

    assertEquals(BigDecimal.ZERO, result);
  }

  @Test
  public void testApplyShowValueCondZeroQuantityWithNegativeCondition() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method applyShowValueCond = getApplyShowValueCondMethod();

    BigDecimal qty = BigDecimal.ZERO;
    BigDecimal result = (BigDecimal) applyShowValueCond.invoke(accountTree, qty, "N", true);

    assertEquals(BigDecimal.ZERO, result);
  }

  @Test
  public void testApplyShowValueCondUnknownConditionReturnsOriginal() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method applyShowValueCond = getApplyShowValueCondMethod();

    BigDecimal qty = new BigDecimal("50.25");
    BigDecimal result = (BigDecimal) applyShowValueCond.invoke(accountTree, qty, "X", true);

    assertEquals(qty, result);
  }

  // =========================================================================
  // Tests for setDataQty (private method accessed via reflection)
  // =========================================================================

  @Test
  public void testSetDataQtyWithNullReportElementReturnsNull() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method setDataQty = getSetDataQtyMethod();

    AccountTreeData result = (AccountTreeData) setDataQty.invoke(accountTree, null, "D");

    assertNull(result);
  }

  @Test
  public void testSetDataQtyWithNullAccountsFactsReturnsOriginal() throws Exception {
    AccountTreeData[] accountsTree = createSingleNodeAccountTree("1", "0");
    AccountTree accountTree = createAccountTreeWithEmptyFacts(accountsTree);
    Method setDataQty = getSetDataQtyMethod();

    AccountTreeData reportElement = createAccountTreeDataNode("1", "0", "100", "D");
    AccountTreeData result = (AccountTreeData) setDataQty.invoke(accountTree, reportElement, "D");

    assertEquals(reportElement, result);
  }

  @Test
  public void testSetDataQtyWithCreditAccountSign() throws Exception {
    AccountTreeData[] accountsTree = createSingleNodeAccountTree("1", "0");
    AccountTreeData[] accountsFacts = createAccountsFactsWithCredit("1", "100", "200", "50", "75");
    AccountTree accountTree = createAccountTreeWithFacts(accountsTree, accountsFacts);
    Method setDataQty = getSetDataQtyMethod();

    AccountTreeData reportElement = createAccountTreeDataNode("1", "0", "0", "D");
    reportElement.showvaluecond = "A";
    reportElement.issummary = "N";

    AccountTreeData result = (AccountTreeData) setDataQty.invoke(accountTree, reportElement, "C");

    assertEquals("200", result.qtyOperation);
    assertEquals("75", result.qtyOperationRef);
  }

  @Test
  public void testSetDataQtyWithDebitAccountSign() throws Exception {
    AccountTreeData[] accountsTree = createSingleNodeAccountTree("1", "0");
    AccountTreeData[] accountsFacts = createAccountsFactsWithDebit("1", "150", "80");
    AccountTree accountTree = createAccountTreeWithFacts(accountsTree, accountsFacts);
    Method setDataQty = getSetDataQtyMethod();

    AccountTreeData reportElement = createAccountTreeDataNode("1", "0", "0", "D");
    reportElement.showvaluecond = "A";
    reportElement.issummary = "N";

    AccountTreeData result = (AccountTreeData) setDataQty.invoke(accountTree, reportElement, "D");

    assertEquals("150", result.qtyOperation);
    assertEquals("80", result.qtyOperationRef);
  }

  @Test
  public void testSetDataQtyNoMatchingIdReturnsUnmodified() throws Exception {
    AccountTreeData[] accountsTree = createSingleNodeAccountTree("1", "0");
    AccountTreeData[] accountsFacts = createAccountsFactsWithDebit("2", "150", "80");
    AccountTree accountTree = createAccountTreeWithFacts(accountsTree, accountsFacts);
    Method setDataQty = getSetDataQtyMethod();

    AccountTreeData reportElement = createAccountTreeDataNode("1", "0", "0", "D");
    reportElement.qtyOperation = "0";
    reportElement.qtyOperationRef = "0";

    AccountTreeData result = (AccountTreeData) setDataQty.invoke(accountTree, reportElement, "D");

    assertEquals("0", result.qtyOperation);
    assertEquals("0", result.qtyOperationRef);
  }

  // =========================================================================
  // Tests for hasOperand (private method accessed via reflection)
  // =========================================================================

  @Test
  public void testHasOperandWithNullIndexReturnsFalse() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method hasOperand = getHasOperandMethod();

    AccountTreeData[] forms = new AccountTreeData[1];
    forms[0] = createAccountTreeDataNode("1", "0", "100", "D");

    boolean result = (boolean) hasOperand.invoke(accountTree, null, forms);

    assertEquals(false, result);
  }

  @Test
  public void testHasOperandWithMatchingIndexReturnsTrue() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method hasOperand = getHasOperandMethod();

    AccountTreeData[] forms = new AccountTreeData[2];
    forms[0] = createAccountTreeDataNode("1", "0", "100", "D");
    forms[1] = createAccountTreeDataNode("2", "0", "200", "D");

    boolean result = (boolean) hasOperand.invoke(accountTree, "2", forms);

    assertEquals(true, result);
  }

  @Test
  public void testHasOperandWithNoMatchReturnsFalse() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method hasOperand = getHasOperandMethod();

    AccountTreeData[] forms = new AccountTreeData[2];
    forms[0] = createAccountTreeDataNode("1", "0", "100", "D");
    forms[1] = createAccountTreeDataNode("2", "0", "200", "D");

    boolean result = (boolean) hasOperand.invoke(accountTree, "3", forms);

    assertEquals(false, result);
  }

  @Test
  public void testHasOperandWithEmptyFormsReturnsFalse() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method hasOperand = getHasOperandMethod();

    AccountTreeData[] forms = new AccountTreeData[0];

    boolean result = (boolean) hasOperand.invoke(accountTree, "1", forms);

    assertEquals(false, result);
  }

  // =========================================================================
  // Tests for nodeIn (private method accessed via reflection)
  // =========================================================================

  @Test
  public void testNodeInWithMatchingNodeReturnsTrue() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method nodeIn = getNodeInMethod();

    String[] listOfNodes = {"1", "2", "3"};
    boolean result = (boolean) nodeIn.invoke(accountTree, "2", listOfNodes);

    assertEquals(true, result);
  }

  @Test
  public void testNodeInWithNoMatchReturnsFalse() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method nodeIn = getNodeInMethod();

    String[] listOfNodes = {"1", "2", "3"};
    boolean result = (boolean) nodeIn.invoke(accountTree, "4", listOfNodes);

    assertEquals(false, result);
  }

  @Test
  public void testNodeInWithEmptyListReturnsFalse() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method nodeIn = getNodeInMethod();

    String[] listOfNodes = {};
    boolean result = (boolean) nodeIn.invoke(accountTree, "1", listOfNodes);

    assertEquals(false, result);
  }

  @Test
  public void testNodeInWithFirstElementReturnsTrue() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method nodeIn = getNodeInMethod();

    String[] listOfNodes = {"1", "2", "3"};
    boolean result = (boolean) nodeIn.invoke(accountTree, "1", listOfNodes);

    assertEquals(true, result);
  }

  @Test
  public void testNodeInWithLastElementReturnsTrue() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method nodeIn = getNodeInMethod();

    String[] listOfNodes = {"1", "2", "3"};
    boolean result = (boolean) nodeIn.invoke(accountTree, "3", listOfNodes);

    assertEquals(true, result);
  }

  // =========================================================================
  // Tests for isAccountLevelLower (private method accessed via reflection)
  // =========================================================================

  @Test
  public void testIsAccountLevelLowerWithDetailLevelAndSummaryElementReturnsFalse()
      throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method isAccountLevelLower = getIsAccountLevelLowerMethod();

    AccountTreeData data = new AccountTreeData();
    data.elementlevel = "S";

    boolean result = (boolean) isAccountLevelLower.invoke(accountTree, "D", data);

    assertEquals(false, result);
  }

  @Test
  public void testIsAccountLevelLowerWithDetailLevelAndDetailElementReturnsTrue() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method isAccountLevelLower = getIsAccountLevelLowerMethod();

    AccountTreeData data = new AccountTreeData();
    data.elementlevel = "D";

    boolean result = (boolean) isAccountLevelLower.invoke(accountTree, "D", data);

    assertEquals(true, result);
  }

  @Test
  public void testIsAccountLevelLowerWithSummaryLevelReturnsTrue() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method isAccountLevelLower = getIsAccountLevelLowerMethod();

    AccountTreeData data = new AccountTreeData();
    data.elementlevel = "S";

    boolean result = (boolean) isAccountLevelLower.invoke(accountTree, "S", data);

    assertEquals(true, result);
  }

  // =========================================================================
  // Tests for applySignAsPerParent (private method accessed via reflection)
  // =========================================================================

  @Test
  public void testApplySignAsPerParentWithNullAccountsTree() throws Exception {
    AccountTree accountTree = createAccountTreeWithNullTree();
    Method applySignAsPerParent = getApplySignAsPerParentMethod();

    applySignAsPerParent.invoke(accountTree);
    // No exception thrown, method returns early
  }

  @Test
  public void testApplySignAsPerParentWithEmptyAccountsTree() throws Exception {
    AccountTreeData[] emptyTree = new AccountTreeData[0];
    AccountTree accountTree = createAccountTreeWithTree(emptyTree);
    Method applySignAsPerParent = getApplySignAsPerParentMethod();

    applySignAsPerParent.invoke(accountTree);
    // No exception thrown, method returns early
  }

  @Test
  public void testApplySignAsPerParentUpdatesChildSign() throws Exception {
    AccountTreeData parent = createAccountTreeDataNode("1", "0", "0", "D");
    parent.accountsign = "D";
    AccountTreeData child = createAccountTreeDataNode("2", "1", "0", "C");
    child.accountsign = "C";

    AccountTreeData[] accountsTree = {parent, child};
    AccountTree accountTree = createAccountTreeWithTree(accountsTree);
    Method applySignAsPerParent = getApplySignAsPerParentMethod();

    applySignAsPerParent.invoke(accountTree);

    assertEquals("D", child.accountsign);
  }

  @Test
  public void testApplySignAsPerParentPreservesMatchingSign() throws Exception {
    AccountTreeData parent = createAccountTreeDataNode("1", "0", "0", "D");
    parent.accountsign = "D";
    AccountTreeData child = createAccountTreeDataNode("2", "1", "0", "D");
    child.accountsign = "D";

    AccountTreeData[] accountsTree = {parent, child};
    AccountTree accountTree = createAccountTreeWithTree(accountsTree);
    Method applySignAsPerParent = getApplySignAsPerParentMethod();

    applySignAsPerParent.invoke(accountTree);

    assertEquals("D", child.accountsign);
  }

  // =========================================================================
  // Tests for updateTreeQuantitiesSign (private method)
  // =========================================================================

  @Test
  public void testUpdateTreeQuantitiesSignWithNullAccountsTreeReturnsNull() throws Exception {
    AccountTree accountTree = createAccountTreeWithNullTree();
    Method updateTreeQuantitiesSign = getUpdateTreeQuantitiesSignMethod();

    AccountTreeData[] result =
        (AccountTreeData[]) updateTreeQuantitiesSign.invoke(accountTree, null, 0, "D");

    assertNull(result);
  }

  @Test
  public void testUpdateTreeQuantitiesSignWithEmptyAccountsTreeReturnsEmpty() throws Exception {
    AccountTreeData[] emptyTree = new AccountTreeData[0];
    AccountTree accountTree = createAccountTreeWithTree(emptyTree);
    Method updateTreeQuantitiesSign = getUpdateTreeQuantitiesSignMethod();

    AccountTreeData[] result =
        (AccountTreeData[]) updateTreeQuantitiesSign.invoke(accountTree, null, 0, "D");

    assertNotNull(result);
    assertEquals(0, result.length);
  }

  @Test
  public void testUpdateTreeQuantitiesSignSetsElementLevel() throws Exception {
    AccountTreeData node = createAccountTreeDataNode("1", "0", "0", "D");
    node.qty = "0";
    node.qtyRef = "0";
    node.qtyOperation = "0";
    node.qtyOperationRef = "0";
    node.showvaluecond = "A";
    node.issummary = "N";

    AccountTreeData[] accountsTree = {node};
    AccountTree accountTree = createAccountTreeWithFacts(accountsTree, new AccountTreeData[0]);
    Method updateTreeQuantitiesSign = getUpdateTreeQuantitiesSignMethod();

    AccountTreeData[] result =
        (AccountTreeData[]) updateTreeQuantitiesSign.invoke(accountTree, null, 0, "D");

    assertNotNull(result);
    assertEquals(1, result.length);
    assertEquals("0", result[0].elementLevel);
  }

  // =========================================================================
  // Tests for getAccounts
  // =========================================================================

  @Test
  public void testGetAccountsReturnsReportElements() throws Exception {
    AccountTreeData node = createAccountTreeDataNode("1", "0", "100", "D");
    node.qty = "100";
    node.qtyRef = "50";
    node.qtyOperation = "100";
    node.qtyOperationRef = "50";
    node.showvaluecond = "A";
    node.issummary = "N";
    node.elementLevel = "0";
    node.calculated = "Y";

    AccountTreeData[] expected = {node};
    AccountTree accountTree = createAccountTreeWithReportElements(expected);

    AccountTreeData[] result = accountTree.getAccounts();

    assertArrayEquals(expected, result);
  }

  @Test
  public void testGetAccountsReturnsNullWhenNoReportElements() throws Exception {
    AccountTree accountTree = createAccountTreeWithReportElements(null);

    AccountTreeData[] result = accountTree.getAccounts();

    assertNull(result);
  }

  // =========================================================================
  // Tests for filterSVC
  // =========================================================================

  @Test
  public void testFilterSVCWithNullReportElements() throws Exception {
    AccountTree accountTree = createAccountTreeWithReportElements(null);

    accountTree.filterSVC();
    // No exception should be thrown
  }

  @Test
  public void testFilterSVCWithEmptyReportElements() throws Exception {
    AccountTree accountTree = createAccountTreeWithReportElements(new AccountTreeData[0]);

    accountTree.filterSVC();
    // No exception should be thrown
  }

  @Test
  public void testFilterSVCResetsChildrenOfResetParent() throws Exception {
    AccountTreeData parent = createAccountTreeDataNode("1", "0", "100", "D");
    parent.elementLevel = "0";
    parent.qty = "100";
    parent.qtyRef = "50";
    parent.svcreset = "Y";
    parent.svcresetref = "N";

    AccountTreeData child = createAccountTreeDataNode("2", "1", "50", "D");
    child.elementLevel = "1";
    child.qty = "50";
    child.qtyRef = "25";
    child.svcreset = "N";
    child.svcresetref = "N";

    AccountTreeData[] reportElements = {parent, child};
    AccountTree accountTree = createAccountTreeWithReportElements(reportElements);

    accountTree.filterSVC();

    assertEquals("0.0", child.qty);
    assertEquals("25", child.qtyRef);
  }

  @Test
  public void testFilterSVCResetsRefForChildren() throws Exception {
    AccountTreeData parent = createAccountTreeDataNode("1", "0", "100", "D");
    parent.elementLevel = "0";
    parent.qty = "100";
    parent.qtyRef = "50";
    parent.svcreset = "N";
    parent.svcresetref = "Y";

    AccountTreeData child = createAccountTreeDataNode("2", "1", "50", "D");
    child.elementLevel = "1";
    child.qty = "50";
    child.qtyRef = "25";
    child.svcreset = "N";
    child.svcresetref = "N";

    AccountTreeData[] reportElements = {parent, child};
    AccountTree accountTree = createAccountTreeWithReportElements(reportElements);

    accountTree.filterSVC();

    assertEquals("50", child.qty);
    assertEquals("0.0", child.qtyRef);
  }

  @Test
  public void testFilterSVCStopsResetAtSameLevelWithoutFlag() throws Exception {
    AccountTreeData parent = createAccountTreeDataNode("1", "0", "100", "D");
    parent.elementLevel = "0";
    parent.qty = "100";
    parent.qtyRef = "50";
    parent.svcreset = "Y";
    parent.svcresetref = "Y";

    AccountTreeData sibling = createAccountTreeDataNode("3", "0", "75", "D");
    sibling.elementLevel = "0";
    sibling.qty = "75";
    sibling.qtyRef = "30";
    sibling.svcreset = "N";
    sibling.svcresetref = "N";

    AccountTreeData[] reportElements = {parent, sibling};
    AccountTree accountTree = createAccountTreeWithReportElements(reportElements);

    accountTree.filterSVC();

    assertEquals("75", sibling.qty);
    assertEquals("30", sibling.qtyRef);
  }

  // =========================================================================
  // Tests for filter
  // =========================================================================

  @Test
  public void testFilterWithNullReportElementsLogsWarning() throws Exception {
    AccountTree accountTree = createAccountTreeWithReportElements(null);

    accountTree.filter(false, "1", false);
    // Should log warning but not throw exception
  }

  // =========================================================================
  // Tests for filterStructure
  // =========================================================================

  @Test
  public void testFilterStructureWithNullReportElementsReturnsNull() throws Exception {
    AccountTree accountTree = createAccountTreeWithReportElements(null);

    String[] indice = {"0"};
    AccountTreeData[] result = accountTree.filterStructure(indice, false, "1", false);

    assertNull(result);
  }

  @Test
  public void testFilterStructureWithEmptyReportElementsReturnsEmpty() throws Exception {
    AccountTree accountTree = createAccountTreeWithReportElements(new AccountTreeData[0]);

    String[] indice = {"0"};
    AccountTreeData[] result = accountTree.filterStructure(indice, false, "1", false);

    assertNotNull(result);
    assertEquals(0, result.length);
  }

  @Test
  public void testFilterStructureFiltersNonShownElements() throws Exception {
    AccountTreeData shown = createAccountTreeDataNode("1", "0", "100", "D");
    shown.parentId = "0";
    shown.showelement = "Y";
    shown.elementLevel = "1";
    shown.elementlevel = "1";
    shown.qty = "100";
    shown.qtyRef = "50";
    shown.showvaluecond = "A";
    shown.isalwaysshown = "N";

    AccountTreeData hidden = createAccountTreeDataNode("2", "0", "200", "D");
    hidden.parentId = "0";
    hidden.showelement = "N";
    hidden.elementLevel = "1";
    hidden.elementlevel = "1";
    hidden.qty = "200";
    hidden.qtyRef = "100";
    hidden.showvaluecond = "A";
    hidden.isalwaysshown = "N";

    AccountTreeData[] reportElements = {shown, hidden};
    AccountTree accountTree = createAccountTreeWithReportElements(reportElements);

    String[] indice = {"0"};
    AccountTreeData[] result = accountTree.filterStructure(indice, false, "1", false);

    assertEquals(1, result.length);
    assertEquals("1", result[0].id);
  }

  @Test
  public void testFilterStructureFiltersEmptyLinesWhenRequested() throws Exception {
    AccountTreeData nonEmpty = createAccountTreeDataNode("1", "0", "100", "D");
    nonEmpty.parentId = "0";
    nonEmpty.showelement = "Y";
    nonEmpty.elementLevel = "1";
    nonEmpty.elementlevel = "1";
    nonEmpty.qty = "100";
    nonEmpty.qtyRef = "0";
    nonEmpty.showvaluecond = "A";
    nonEmpty.isalwaysshown = "N";

    AccountTreeData empty = createAccountTreeDataNode("2", "0", "0", "D");
    empty.parentId = "0";
    empty.showelement = "Y";
    empty.elementLevel = "1";
    empty.elementlevel = "1";
    empty.qty = "0";
    empty.qtyRef = "0";
    empty.showvaluecond = "A";
    empty.isalwaysshown = "N";

    AccountTreeData[] reportElements = {nonEmpty, empty};
    AccountTree accountTree = createAccountTreeWithReportElements(reportElements);

    String[] indice = {"0"};
    AccountTreeData[] result = accountTree.filterStructure(indice, true, "1", false);

    assertEquals(1, result.length);
    assertEquals("1", result[0].id);
  }

  @Test
  public void testFilterStructureKeepsAlwaysShownElementsEvenIfEmpty() throws Exception {
    AccountTreeData alwaysShown = createAccountTreeDataNode("1", "0", "0", "D");
    alwaysShown.parentId = "0";
    alwaysShown.showelement = "Y";
    alwaysShown.elementLevel = "1";
    alwaysShown.elementlevel = "1";
    alwaysShown.qty = "0";
    alwaysShown.qtyRef = "0";
    alwaysShown.showvaluecond = "A";
    alwaysShown.isalwaysshown = "Y";

    AccountTreeData[] reportElements = {alwaysShown};
    AccountTree accountTree = createAccountTreeWithReportElements(reportElements);

    String[] indice = {"0"};
    AccountTreeData[] result = accountTree.filterStructure(indice, true, "1", false);

    assertEquals(1, result.length);
    assertNull(result[0].qty);
    assertNull(result[0].qtyRef);
  }

  // =========================================================================
  // Edge case tests
  // =========================================================================

  @Test
  public void testApplyShowValueCondWithVeryLargePositiveNumber() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method applyShowValueCond = getApplyShowValueCondMethod();

    BigDecimal largeQty = new BigDecimal("999999999999999999.99");
    BigDecimal result = (BigDecimal) applyShowValueCond.invoke(accountTree, largeQty, "P", true);

    assertEquals(largeQty, result);
  }

  @Test
  public void testApplyShowValueCondWithVeryLargeNegativeNumber() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method applyShowValueCond = getApplyShowValueCondMethod();

    BigDecimal largeNegQty = new BigDecimal("-999999999999999999.99");
    BigDecimal result = (BigDecimal) applyShowValueCond.invoke(accountTree, largeNegQty, "N", true);

    assertEquals(largeNegQty, result);
  }

  @Test
  public void testApplyShowValueCondCaseInsensitive() throws Exception {
    AccountTree accountTree = createMinimalAccountTree();
    Method applyShowValueCond = getApplyShowValueCondMethod();

    BigDecimal qty = new BigDecimal("100");

    BigDecimal resultLowerA = (BigDecimal) applyShowValueCond.invoke(accountTree, qty, "a", true);
    BigDecimal resultUpperA = (BigDecimal) applyShowValueCond.invoke(accountTree, qty, "A", true);

    assertEquals(resultLowerA, resultUpperA);
  }

  @Test
  public void testFilterSVCWithMultipleLevels() throws Exception {
    AccountTreeData level0 = createAccountTreeDataNode("1", "0", "100", "D");
    level0.elementLevel = "0";
    level0.qty = "100";
    level0.qtyRef = "50";
    level0.svcreset = "Y";
    level0.svcresetref = "N";

    AccountTreeData level1 = createAccountTreeDataNode("2", "1", "75", "D");
    level1.elementLevel = "1";
    level1.qty = "75";
    level1.qtyRef = "40";
    level1.svcreset = "N";
    level1.svcresetref = "N";

    AccountTreeData level2 = createAccountTreeDataNode("3", "2", "50", "D");
    level2.elementLevel = "2";
    level2.qty = "50";
    level2.qtyRef = "30";
    level2.svcreset = "N";
    level2.svcresetref = "N";

    AccountTreeData[] reportElements = {level0, level1, level2};
    AccountTree accountTree = createAccountTreeWithReportElements(reportElements);

    accountTree.filterSVC();

    assertEquals("0.0", level1.qty);
    assertEquals("0.0", level2.qty);
    assertEquals("40", level1.qtyRef);
    assertEquals("30", level2.qtyRef);
  }

  @Test
  public void testFilterStructureWithNullLevel() throws Exception {
    AccountTreeData element = createAccountTreeDataNode("1", "0", "100", "D");
    element.parentId = "0";
    element.showelement = "Y";
    element.elementLevel = "1";
    element.elementlevel = "1";
    element.qty = "100";
    element.qtyRef = "50";
    element.showvaluecond = "A";
    element.isalwaysshown = "N";

    AccountTreeData[] reportElements = {element};
    AccountTree accountTree = createAccountTreeWithReportElements(reportElements);

    String[] indice = {"0"};
    AccountTreeData[] result = accountTree.filterStructure(indice, false, null, false);

    assertNotNull(result);
  }

  @Test
  public void testFilterStructureWithEmptyLevel() throws Exception {
    AccountTreeData element = createAccountTreeDataNode("1", "0", "100", "D");
    element.parentId = "0";
    element.showelement = "Y";
    element.elementLevel = "1";
    element.elementlevel = "1";
    element.qty = "100";
    element.qtyRef = "50";
    element.showvaluecond = "A";
    element.isalwaysshown = "N";

    AccountTreeData[] reportElements = {element};
    AccountTree accountTree = createAccountTreeWithReportElements(reportElements);

    String[] indice = {"0"};
    AccountTreeData[] result = accountTree.filterStructure(indice, false, "", false);

    assertNotNull(result);
  }

  // =========================================================================
  // Helper methods for creating test data
  // =========================================================================

  private AccountTree createMinimalAccountTree() throws Exception {
    return createAccountTreeWithTree(new AccountTreeData[0]);
  }

  private AccountTree createAccountTreeWithNullTree() throws Exception {
    AccountTree accountTree = createAccountTreeInstance();
    setPrivateField(accountTree, "accountsTree", null);
    setPrivateField(accountTree, "accountsFacts", null);
    setPrivateField(accountTree, "reportElements", null);
    return accountTree;
  }

  private AccountTree createAccountTreeWithTree(AccountTreeData[] tree) throws Exception {
    AccountTree accountTree = createAccountTreeInstance();
    setPrivateField(accountTree, "accountsTree", tree);
    setPrivateField(accountTree, "accountsFacts", new AccountTreeData[0]);
    setPrivateField(accountTree, "reportElements", new AccountTreeData[0]);
    return accountTree;
  }

  private AccountTree createAccountTreeWithEmptyFacts(AccountTreeData[] tree) throws Exception {
    AccountTree accountTree = createAccountTreeInstance();
    setPrivateField(accountTree, "accountsTree", tree);
    setPrivateField(accountTree, "accountsFacts", null);
    setPrivateField(accountTree, "reportElements", new AccountTreeData[0]);
    return accountTree;
  }

  private AccountTree createAccountTreeWithFacts(AccountTreeData[] tree, AccountTreeData[] facts)
      throws Exception {
    AccountTree accountTree = createAccountTreeInstance();
    setPrivateField(accountTree, "accountsTree", tree);
    setPrivateField(accountTree, "accountsFacts", facts);
    setPrivateField(accountTree, "reportElements", new AccountTreeData[0]);
    return accountTree;
  }

  private AccountTree createAccountTreeWithReportElements(AccountTreeData[] reportElements)
      throws Exception {
    AccountTree accountTree = createAccountTreeInstance();
    setPrivateField(accountTree, "accountsTree", new AccountTreeData[0]);
    setPrivateField(accountTree, "accountsFacts", new AccountTreeData[0]);
    setPrivateField(accountTree, "reportElements", reportElements);
    setPrivateField(accountTree, "reportNodes", new String[] {"0"});
    return accountTree;
  }

  private AccountTree createAccountTreeInstance() throws Exception {
    // Use reflection to create instance without calling constructor
    java.lang.reflect.Constructor<AccountTree> constructor =
        AccountTree.class.getDeclaredConstructor(VariablesSecureApp.class, ConnectionProvider.class,
            AccountTreeData[].class, AccountTreeData[].class, String.class);
    constructor.setAccessible(true);

    // We can't use the constructor directly as it calls static methods
    // So we use sun.misc.Unsafe or just create it via a factory pattern
    // For simplicity, we'll use reflection to create an uninitialized instance
    AccountTree instance = createUninitializedInstance(AccountTree.class);
    setPrivateField(instance, "vars", vars);
    setPrivateField(instance, "conn", conn);
    return instance;
  }

  @SuppressWarnings("unchecked")
  private <T> T createUninitializedInstance(Class<T> clazz) throws Exception {
    // Use Objenesis to create instance without calling constructor
    ObjenesisStd objenesis = new ObjenesisStd();
    return objenesis.newInstance(clazz);
  }

  private AccountTreeData[] createSingleNodeAccountTree(String id, String parentId) {
    AccountTreeData node = createAccountTreeDataNode(id, parentId, "0", "D");
    return new AccountTreeData[] {node};
  }

  private AccountTreeData createAccountTreeDataNode(String id, String parentId, String qty,
      String accountSign) {
    AccountTreeData data = new AccountTreeData();
    data.id = id;
    data.nodeId = id;
    data.parentId = parentId;
    data.qty = qty;
    data.qtyRef = "0";
    data.qtyOperation = qty;
    data.qtyOperationRef = "0";
    data.accountsign = accountSign;
    data.showvaluecond = "A";
    data.issummary = "N";
    data.calculated = "N";
    data.svcreset = "N";
    data.svcresetref = "N";
    data.elementLevel = "0";
    data.elementlevel = "D";
    data.showelement = "Y";
    data.isalwaysshown = "N";
    return data;
  }

  private AccountTreeData[] createAccountsFactsWithCredit(String id, String qty, String qtycredit,
      String qtyRef, String qtycreditRef) {
    AccountTreeData fact = new AccountTreeData();
    fact.id = id;
    fact.qty = qty;
    fact.qtycredit = qtycredit;
    fact.qtyRef = qtyRef;
    fact.qtycreditRef = qtycreditRef;
    return new AccountTreeData[] {fact};
  }

  private AccountTreeData[] createAccountsFactsWithDebit(String id, String qty, String qtyRef) {
    AccountTreeData fact = new AccountTreeData();
    fact.id = id;
    fact.qty = qty;
    fact.qtyRef = qtyRef;
    fact.qtycredit = "0";
    fact.qtycreditRef = "0";
    return new AccountTreeData[] {fact};
  }

  // =========================================================================
  // Reflection helper methods
  // =========================================================================

  private Method getApplyShowValueCondMethod() throws Exception {
    Method method = AccountTree.class.getDeclaredMethod("applyShowValueCond", BigDecimal.class,
        String.class, boolean.class);
    method.setAccessible(true);
    return method;
  }

  private Method getSetDataQtyMethod() throws Exception {
    Method method =
        AccountTree.class.getDeclaredMethod("setDataQty", AccountTreeData.class, String.class);
    method.setAccessible(true);
    return method;
  }

  private Method getHasOperandMethod() throws Exception {
    Method method =
        AccountTree.class.getDeclaredMethod("hasOperand", String.class, AccountTreeData[].class);
    method.setAccessible(true);
    return method;
  }

  private Method getNodeInMethod() throws Exception {
    Method method = AccountTree.class.getDeclaredMethod("nodeIn", String.class, String[].class);
    method.setAccessible(true);
    return method;
  }

  private Method getIsAccountLevelLowerMethod() throws Exception {
    Method method = AccountTree.class.getDeclaredMethod("isAccountLevelLower", String.class,
        AccountTreeData.class);
    method.setAccessible(true);
    return method;
  }

  private Method getApplySignAsPerParentMethod() throws Exception {
    Method method = AccountTree.class.getDeclaredMethod("applySignAsPerParent");
    method.setAccessible(true);
    return method;
  }

  private Method getUpdateTreeQuantitiesSignMethod() throws Exception {
    Method method = AccountTree.class.getDeclaredMethod("updateTreeQuantitiesSign", String.class,
        int.class, String.class);
    method.setAccessible(true);
    return method;
  }

  private void setPrivateField(Object object, String fieldName, Object value) throws Exception {
    Field field = object.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(object, value);
  }
}
