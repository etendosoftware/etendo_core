/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2017-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.test.db.model.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.Ignore;
import org.junit.Test;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetup;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationTree;
import org.openbravo.service.db.CallProcess;

/**
 * Tests related to AD_Org_Tree table
 * 
 */
public class ADOrgTreeTest extends Ad_isorgincludedTest {
  private static final Logger log = LogManager.getLogger();

  private static final String ORGTYPE_ORGANIZATION = "0";
  private static final String CLIENT_0 = "0";

  private final static String[] allOrganizations = { ORG_FB_NORTE, ORG_FB_SUR, ORG_FB_FBGROUP,
      ORG_0, ORG_FB_SPAIN, ORG_FB_US, ORG_FB_WEST, ORG_FB_EAST, ORG_QA_MAIN, ORG_QA_SPAIN, null,
      "XX", "YY" };
  private final static String[] allClients = { CLIENT_FB, CLIENT_QA, CLIENT_0, null, "XX", "XY" };
  private final static int ALL_COMBINATIONS = allOrganizations.length * allOrganizations.length
      * allClients.length;

  @Override
  public void testIsOrgIncluded() {
    // Don't launch this test again, it will be launched by Ad_isorgincludedTest class itself
  }

  /**
   * Creates a new organization just below * and do not cascade
   */
  @Test
  public void testSingleNewOrganization() {
    singleOrgTestCreator("N");
  }

  /**
   * Creates a new organization just below * and do cascade
   */
  @Test
  public void testSingleNewOrganizationCascade() {
    singleOrgTestCreator("Y");
  }

  private void singleOrgTestCreator(final String isCascade) {
    try {
      OBContext.setAdminMode(true);
      final long number = System.currentTimeMillis();
      final String newOrgId = createOrganization("Test_" + number, ORGTYPE_ORGANIZATION, ORG_0);

      List<OrganizationTree> orgTreeRecords = getOrganizationTreeRecords(newOrgId, null);
      assertEquals("Records found at OrgTree at this point", 0, orgTreeRecords.size());

      setAsReady(newOrgId, isCascade);
      orgTreeRecords = getOrganizationTreeRecords(newOrgId, null);
      assertEquals("Records found at OrgTree at this point", 2, orgTreeRecords.size());

      orgTreeRecords = getOrganizationTreeRecords(newOrgId, newOrgId);
      assertEquals("Records found at OrgTree at this point", 1, orgTreeRecords.size());
      assertEquals("Level found", 1, orgTreeRecords.get(0).getLevelno().longValue());

      orgTreeRecords = getOrganizationTreeRecords(newOrgId, ORG_0);
      assertEquals("Records found at OrgTree at this point", 1, orgTreeRecords.size());
      assertEquals("Level found", 2, orgTreeRecords.get(0).getLevelno().longValue());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test creation of two new organizations (one below the other) and set as ready individually:
   * <ul>
   * <li>*</li>
   * <li>|__firstOrg</li>
   * <li>___|__secondOrg</li>
   * </ul>
   */
  @Test
  public void testTwoNewOrgsIndividually() {
    twoOrgTestBuilder(false);
  }

  /**
   * Test creation of two new organizations (one below the other) and set as ready in Cascade:
   * <ul>
   * <li>*</li>
   * <li>|__firstOrg</li>
   * <li>___|__secondOrg</li>
   * </ul>
   */
  @Test
  public void testTwoNewOrgsCascade() {
    twoOrgTestBuilder(true);
  }

  private void twoOrgTestBuilder(boolean isCascade) {
    try {
      OBContext.setAdminMode(true);
      long number = System.currentTimeMillis();
      // First Org
      final String firstOrgId = createOrganization("Test_" + number, ORGTYPE_ORGANIZATION, ORG_0);

      List<OrganizationTree> orgTreeRecords = getOrganizationTreeRecords(firstOrgId, null);
      assertEquals("Records found at OrgTree at this point", 0, orgTreeRecords.size());

      String secondOrgId = null;
      if (!isCascade) {
        setAsReady(firstOrgId, "N");
      } else { // Second Org
        number = System.currentTimeMillis();
        secondOrgId = createOrganization("Test_" + number, ORGTYPE_ORGANIZATION, firstOrgId);

        orgTreeRecords = getOrganizationTreeRecords(secondOrgId, null);
        assertEquals("Records found at OrgTree at this point", 0, orgTreeRecords.size());

        setAsReady(firstOrgId, "Y");
      }

      // First org checks
      orgTreeRecords = getOrganizationTreeRecords(firstOrgId, null);
      assertEquals("Records found at OrgTree at this point", 2, orgTreeRecords.size());

      orgTreeRecords = getOrganizationTreeRecords(firstOrgId, firstOrgId);
      assertEquals("Records found at OrgTree at this point", 1, orgTreeRecords.size());
      assertEquals("Level found", 1, orgTreeRecords.get(0).getLevelno().longValue());

      orgTreeRecords = getOrganizationTreeRecords(firstOrgId, ORG_0);
      assertEquals("Records found at OrgTree at this point", 1, orgTreeRecords.size());
      assertEquals("Level found", 2, orgTreeRecords.get(0).getLevelno().longValue());

      // Second org checks
      if (!isCascade) {
        number = System.currentTimeMillis();
        secondOrgId = createOrganization("Test_" + number, ORGTYPE_ORGANIZATION, firstOrgId);

        orgTreeRecords = getOrganizationTreeRecords(secondOrgId, null);
        assertEquals("Records found at OrgTree at this point", 0, orgTreeRecords.size());

        setAsReady(secondOrgId, "N");
      }

      orgTreeRecords = getOrganizationTreeRecords(secondOrgId, null);
      assertEquals("Records found at OrgTree at this point", 3, orgTreeRecords.size());

      orgTreeRecords = getOrganizationTreeRecords(secondOrgId, secondOrgId);
      assertEquals("Records found at OrgTree at this point", 1, orgTreeRecords.size());
      assertEquals("Level found", 1, orgTreeRecords.get(0).getLevelno().longValue());

      orgTreeRecords = getOrganizationTreeRecords(secondOrgId, firstOrgId);
      assertEquals("Records found at OrgTree at this point", 1, orgTreeRecords.size());
      assertEquals("Level found", 2, orgTreeRecords.get(0).getLevelno().longValue());

      orgTreeRecords = getOrganizationTreeRecords(secondOrgId, ORG_0);
      assertEquals("Records found at OrgTree at this point", 1, orgTreeRecords.size());
      assertEquals("Level found", 3, orgTreeRecords.get(0).getLevelno().longValue());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Creates parent and child organization and tries to set as ready the child organization (cascade
   * and not cascade). Verify it fails because parent org is not set as ready yet
   */
  @Test
  public void testChildOrgValidation() {
    try {
      OBContext.setAdminMode(true);
      long number = System.currentTimeMillis();
      final String firstOrgId = createOrganization("Test_" + number, ORGTYPE_ORGANIZATION, ORG_0);
      number = System.currentTimeMillis();
      final String secondOrgId = createOrganization("Test_" + number, ORGTYPE_ORGANIZATION,
          firstOrgId);

      try {
        setAsReady(secondOrgId, "N");
        fail("Expected exception when setting second organization was not occured.");
      } catch (Exception expectedException) {
      }

      try {
        setAsReady(secondOrgId, "Y");
        fail("Expected exception when setting second organization was not occured.");
      } catch (Exception expectedException) {
      }

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test we get expected records in AD_ORG_Tree table when the org tree has few levels
   */
  @Test
  public void testCreateExtendedOrgTreeFewLevels() {
    checkExtendedOrgTreeBuilder(7, 2);
  }

  /**
   * Test we get expected records in AD_ORG_Tree table when the org tree has many levels
   */
  @Test
  public void testCreateExtendedOrgTreeManyLevels() {
    checkExtendedOrgTreeBuilder(23, 6);
  }

  private void checkExtendedOrgTreeBuilder(int totalOrgs, int maxLevel) {
    try {
      OBContext.setAdminMode(true);
      String superParentOrgId = createOrganizationTree(totalOrgs, maxLevel);
      setAsReady(superParentOrgId, "Y");
      List<OrganizationTree> orgTreeRecords = getOrganizationTreeRecords(null, superParentOrgId);
      assertEquals("Records found at OrgTree at this point", totalOrgs, orgTreeRecords.size());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test all the possible combinations for calling AD_IsOrgIncluded function and compares the
   * result to the original AD_IsOrgIncluded_TreeNode
   */
  @Test
  public void testCallAllCombinations() {
    int i = 0;
    for (final String client : allClients) {
      for (final String organizationChild : allOrganizations) {
        for (final String organizationParent : allOrganizations) {
          assertEquals(
              "Failed combination with parameters: " + organizationChild + ", " + organizationParent
                  + ", " + client,
              isOrgIncludedLegacy(organizationChild, organizationParent, client),
              isOrgIncluded(organizationChild, organizationParent, client));
          i++;
        }
      }
    }
    assertEquals("Combinations tested", ALL_COMBINATIONS, i);
  }

  /**
   * Test performance of new function is better than legacy one when used as a filter criteria in
   * the where clause. Verify we get the same results.
   */
  @Ignore("This test is ignored because it might create false positive in CI machines due to different server load.")
  @Test
  public void testPerformanceI() {
    try {
      OBContext.setAdminMode(true);
      final String hql = "select count(*) from FinancialMgmtAccountingFact fa where ad_isorgincluded(fa.organization.id, :parentOrgId, :clientId) <> -1";
      final Session session = OBDal.getInstance().getSession();
      final Query<Long> hqlQuery = session.createQuery(hql.toString(), Long.class);
      hqlQuery.setParameter("parentOrgId", ORG_FB_SPAIN);
      hqlQuery.setParameter("clientId", CLIENT_FB);
      long start = System.currentTimeMillis();
      final long hqlCount = hqlQuery.uniqueResult();
      long hqlTime = System.currentTimeMillis() - start;
      log.info("AD_IsOrgIncluded time: " + hqlTime);

      final String hqlLegacy = "select count(*) from FinancialMgmtAccountingFact fa where ad_isorgincluded_treenode(fa.organization.id, :parentOrgId, :clientId) <> -1";
      final Query<Long> hqlLegacyQuery = session.createQuery(hqlLegacy.toString(), Long.class);
      hqlLegacyQuery.setParameter("parentOrgId", ORG_FB_SPAIN);
      hqlLegacyQuery.setParameter("clientId", CLIENT_FB);
      start = System.currentTimeMillis();
      final long hqlLegacyCount = (long) hqlLegacyQuery.uniqueResult();
      long hqlLegacyTime = System.currentTimeMillis() - start;
      log.info("AD_IsOrgIncluded_TreeNode time: " + hqlLegacyTime);

      assertEquals(hqlCount, hqlLegacyCount);
      assertTrue(
          "ad_isorgincluded_treenode ( " + hqlLegacyTime
              + ") should be slower than ad_isorgincluded (" + hqlTime + ")",
          hqlLegacyTime > hqlTime);
      // Set to 2 to be conservative (in local testing about 2,70x)
      assertTrue(
          "ad_isorgincluded_treenode ( " + hqlLegacyTime
              + ") should be much slower than ad_isorgincluded (" + hqlTime + ")",
          hqlLegacyTime > hqlTime * 2);
      log.info(String.format("Performance gain: %.2fx", (double) hqlLegacyTime / hqlTime));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test performance of new function is equivalent to legacy one when using individually. 300
   * consecutive calls done in a batch to have better range.
   * 
   * Verify we get the same results
   */
  @Ignore("This test is ignored because it might create false positive in CI machines due to different server load.")
  @Test
  public void testPerformanceII() {
    final int numberOfCalls = 300;
    int[] result = new int[numberOfCalls];

    int i = 0;
    long init = System.currentTimeMillis();
    do {
      for (final String client : allClients) {
        for (final String organizationChild : allOrganizations) {
          for (final String organizationParent : allOrganizations) {
            if (i >= numberOfCalls) {
              break;
            }
            result[i++] = isOrgIncluded(organizationChild, organizationParent, client);
          }
        }
      }
    } while (i < numberOfCalls);
    long time = System.currentTimeMillis() - init;

    int[] resultLegacy = new int[numberOfCalls];
    i = 0;
    init = System.currentTimeMillis();
    do {
      for (final String client : allClients) {
        for (final String organizationChild : allOrganizations) {
          for (final String organizationParent : allOrganizations) {
            if (i >= numberOfCalls) {
              break;
            }
            resultLegacy[i++] = isOrgIncludedLegacy(organizationChild, organizationParent, client);
          }
        }
      }
    } while (i < numberOfCalls);
    long legacyTime = System.currentTimeMillis() - init;

    assertTrue(Arrays.equals(result, resultLegacy));
    // Set to < 50 ms to be conservative (in local testing about -5ms)
    assertTrue(
        "ad_isorgincluded_treenode ( " + legacyTime
            + ") should be more or less equal than ad_isorgincluded (" + time + ")",
        time - legacyTime < 50);
    log.info(
        "Difference actual (" + time + ") - legacy (" + legacyTime + ") = " + (time - legacyTime));
  }

  private List<OrganizationTree> getOrganizationTreeRecords(final String newOrgId,
      final String parentOrgId) {
    final Map<String, Object> parameters = new HashMap<String, Object>(3);
    String whereClause = "client.id = :clientId ";
    parameters.put("clientId", OBContext.getOBContext().getCurrentClient().getId());
    if (!StringUtils.isBlank(newOrgId)) {
      whereClause += " and organization.id = :orgId ";
      parameters.put("orgId", newOrgId);
    }
    if (!StringUtils.isBlank(parentOrgId)) {
      whereClause += " and parentOrganization.id = :parentOrgId ";
      parameters.put("parentOrgId", parentOrgId);
    }
    OBQuery<OrganizationTree> query = OBDal.getInstance()
        .createQuery(OrganizationTree.class, whereClause);
    query.setNamedParameters(parameters);
    return query.list();
  }

  private String createOrganizationTree(int totalOrgs, int maxLevel) {
    long number = System.currentTimeMillis();
    // Org just below * (the first created)
    final String superParent = createOrganization("SuperParent_" + number, ORGTYPE_ORGANIZATION,
        ORG_0);
    String strParentOrg = superParent;
    int i = 1;
    while (i < totalOrgs) {
      for (int j = 0; i < totalOrgs && j < maxLevel; j++) {
        strParentOrg = createOrganization("Test" + number + "_" + i + "_" + j, ORGTYPE_ORGANIZATION,
            strParentOrg);
        i++;
      }
      strParentOrg = superParent;
    }

    return superParent;
  }

  private String createOrganization(String newOrgName, String newOrgType, String strParentOrg) {
    InitialOrgSetup initialOrg = new InitialOrgSetup(OBContext.getOBContext().getCurrentClient());
    initialOrg.createOrganization(newOrgName, "", newOrgType, strParentOrg, "", "", "", false, null,
        "", false, false, false, false, false);
    OBDal.getInstance().get(Organization.class, initialOrg.getOrgId()).setSummaryLevel(true);
    return initialOrg.getOrgId();
  }

  private void setAsReady(final String orgId, final String isCascade) {
    final Map<String, String> parameters = new HashMap<String, String>(1);
    parameters.put("Cascade", isCascade);
    final ProcessInstance pinstance = CallProcess.getInstance()
        .call("AD_Org_Ready", orgId, parameters);
    if (pinstance.getResult() == 0L) {
      throw new RuntimeException(pinstance.getErrorMsg());
    }
  }

  private int isOrgIncludedLegacy(String orgId, String parentOrgId, String clientId) {
    return callOrgIncludedFunction(orgId, parentOrgId, clientId, "AD_ISORGINCLUDED_TREENODE");
  }

}
