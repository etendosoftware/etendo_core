/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2008-2021 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.security;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.openbravo.test.base.TestConstants.Orgs.ESP;
import static org.openbravo.test.base.TestConstants.Orgs.ESP_NORTE;
import static org.openbravo.test.base.TestConstants.Orgs.ESP_SUR;
import static org.openbravo.test.base.TestConstants.Orgs.FB_GROUP;
import static org.openbravo.test.base.TestConstants.Orgs.MAIN;
import static org.openbravo.test.base.TestConstants.Orgs.US;
import static org.openbravo.test.base.TestConstants.Orgs.US_EST;
import static org.openbravo.test.base.TestConstants.Orgs.US_WEST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.test.base.OBBaseTest;

/**
 * Tests computation of natural tree of an organization. This is used to compute the readable
 * organizations of a user.
 * 
 * @see OrganizationStructureProvider
 * @see OBContext#getReadableOrganizations()
 * 
 * @author mtaal
 */

@RunWith(Parameterized.class)
public class AllowedOrganizationsTest extends OBBaseTest {

  //@formatter:off
  private static final Map<String, String> ORG_NAMES = Map.of(
	      MAIN, "Main",
	      FB_GROUP, "F&B International Group",
	      US, "F&B US, Inc.",
	      US_EST, "F&B US East Coast",
	      US_WEST, "F&B US West Coast",
	      ESP, "F&B España, S.A",
	      ESP_SUR, "F&B España - Región Sur",
	      ESP_NORTE, "F&B España - Región Norte",
	      "Dummy", "Dummy"
	      );

  // note first parent must be first element in list
  private static final Map<String, List<TestOrg>> ORG_TREES = Map.ofEntries(
      entry(FB_GROUP, orgs(MAIN, FB_GROUP, US, US_EST, US_WEST, ESP, ESP_SUR, ESP_NORTE)),
      entry(US, orgs(FB_GROUP, US, US_EST, US_WEST, MAIN)),
      entry(US_WEST, orgs(US, US_WEST, MAIN, FB_GROUP)),
      entry(US_EST, orgs(US, US_EST, MAIN, FB_GROUP)),
      entry(ESP, orgs(FB_GROUP, MAIN, ESP, ESP_SUR, ESP_NORTE)),
      entry(ESP_SUR, orgs(ESP, ESP_SUR, MAIN, FB_GROUP)),
      entry(ESP_NORTE, orgs(ESP, ESP_NORTE, MAIN, FB_GROUP)),
      entry("Dummy", orgs("Dummy")) // special case: non-existent org returns itself
      );
  //@formatter:on

  @Parameter(0)
  public String testingOrgName;

  @Parameter(1)
  public String testingOrgId;

  @Parameter(2)
  public List<TestOrg> expectedNaturalTree;

  private OrganizationStructureProvider osp;

  @Parameters(name = "Tree for organization {0}")
  public static Collection<Object[]> parameters() throws IOException {
    final Collection<Object[]> allTrees = new ArrayList<>();

    for (Entry<String, List<TestOrg>> tree : ORG_TREES.entrySet()) {
      allTrees.add(new Object[] { //
          ORG_NAMES.get(tree.getKey()), //
          tree.getKey(), //
          tree.getValue() //
      });
    }

    return allTrees;
  }

  @Before
  public void setOSP() {
    setTestAdminContext();
    osp = new OrganizationStructureProvider();
    // osp.setClientId(TEST_CLIENT_ID);
  }

  /**
   * Tests valid organizations trees for different organizations.
   */
  @Test
  public void testOrganizationTree() {
    Set<String> naturalTree = osp.getNaturalTree(testingOrgId);
    assertThat("Natural tree for " + ORG_NAMES.get(testingOrgId), naturalTree,
        hasItems(TestOrg.getIDs(expectedNaturalTree)));
    assertThat("Natural tree for " + ORG_NAMES.get(testingOrgId), naturalTree,
        hasSize(expectedNaturalTree.size()));

    for (TestOrg org : expectedNaturalTree) {
      assertThat(ORG_NAMES.get(org.id) + " is in natural tree of " + testingOrgId,
          osp.isInNaturalTree(OBDal.getInstance().getProxy(Organization.class, org.id),
              OBDal.getInstance().getProxy(Organization.class, testingOrgId)),
          is(true));
    }
  }

  @Test
  public void parentOrganization() {
    assumeThat(testingOrgId, not("Dummy"));

    String org = osp.getParentOrg(OBDal.getInstance().getProxy(Organization.class, testingOrgId))
        .getId();

    assertThat(ORG_NAMES.get(testingOrgId) + "'s parent", org, is(expectedNaturalTree.get(0).id));
  }

  @Test
  public void legalOrganization() {
    assumeThat(testingOrgId, not("Dummy"));
    Organization org = OBDal.getInstance().getProxy(Organization.class, testingOrgId);

    if (!FB_GROUP.equals(testingOrgId)) {
      assertThat(ORG_NAMES.get(testingOrgId) + "'s legal entity", osp.getLegalEntity(org).getId(),
          is(TestOrg.getFirstLegal(expectedNaturalTree)));
    } else {
      assertThat(ORG_NAMES.get(testingOrgId) + " has no legal entity ", osp.getLegalEntity(org),
          is(nullValue()));
    }
  }

  @Test
  public void legalOrBUOrganization() {
    assumeThat(testingOrgId, not("Dummy"));
    Organization org = OBDal.getInstance().getProxy(Organization.class, testingOrgId);

    if (!FB_GROUP.equals(testingOrgId)) {
      assertThat(ORG_NAMES.get(testingOrgId) + "'s legal entity",
          osp.getLegalEntityOrBusinessUnit(org).getId(),
          is(TestOrg.getFirstLegalOrBU(expectedNaturalTree)));
    } else {
      assertThat(ORG_NAMES.get(testingOrgId) + " has no legal entity ",
          osp.getLegalEntityOrBusinessUnit(org), is(nullValue()));
    }
  }

  @Test
  public void periodControlOrganization() {
    assumeThat(testingOrgId, not("Dummy"));
    Organization org = OBDal.getInstance().getProxy(Organization.class, testingOrgId);

    if (!FB_GROUP.equals(testingOrgId)) {
      assertThat(ORG_NAMES.get(testingOrgId) + "'s legal entity",
          osp.getPeriodControlAllowedOrganization(org).getId(),
          is(TestOrg.getFirstPeriodControl(expectedNaturalTree)));
    } else {
      assertThat(ORG_NAMES.get(testingOrgId) + " has no legal entity ",
          osp.getPeriodControlAllowedOrganization(org), is(nullValue()));
    }
  }

  private static List<TestOrg> orgs(String... ids) {
    List<TestOrg> orgs = new ArrayList<>(ids.length);
    for (String id : ids) {
      orgs.add(new TestOrg(id));
    }
    return orgs;
  }

  private static class TestOrg {
    String id;

    public TestOrg(String id) {
      this.id = id;
    }

    public static String[] getIDs(List<TestOrg> orgs) {
      List<String> ids = new ArrayList<>();
      for (TestOrg org : orgs) {
        ids.add(org.id);
      }
      return ids.toArray(new String[] {});
    }

    public static String getFirstLegal(List<TestOrg> orgs) {
      for (TestOrg org : orgs) {
        if (OBDal.getInstance()
            .get(Organization.class, org.id)
            .getOrganizationType()
            .isLegalEntity()) {
          return org.id;
        }
      }
      return null;
    }

    public static Object getFirstLegalOrBU(List<TestOrg> orgs) {
      for (TestOrg org : orgs) {
        OrganizationType ot = OBDal.getInstance()
            .get(Organization.class, org.id)
            .getOrganizationType();
        if (ot.isLegalEntity() || ot.isBusinessUnit()) {
          return org.id;
        }
      }
      return null;
    }

    public static String getFirstPeriodControl(List<TestOrg> orgs) {
      for (TestOrg org : orgs) {
        if (OBDal.getInstance().get(Organization.class, org.id).isAllowPeriodControl()) {
          return org.id;
        }
      }
      return null;
    }
  }

}
