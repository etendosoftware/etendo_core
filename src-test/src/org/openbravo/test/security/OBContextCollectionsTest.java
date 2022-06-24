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
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.security;

import static org.junit.Assert.assertNotSame;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.dal.core.OBContext;
import org.openbravo.test.base.OBBaseTest;

/**
 * This test is intended to check that the different collections that can be retrieved using the
 * OBContext class are always returned in a new instance.
 */
@RunWith(Parameterized.class)
public class OBContextCollectionsTest extends OBBaseTest {
  private String method;

  public OBContextCollectionsTest(String method) {
    this.method = method;
  }

  @Parameters(name = "{index}: method = {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { { "getReadableOrganizations" },
        { "getWritableOrganizations" }, { "getDeactivatedOrganizations" }, { "getReadableClients" },
        { "getNaturalTree" }, { "getParentTree" }, { "getParentList" }, { "getChildTree" } });
  }

  @Test
  public void methodReturnsNewCollectionInstance() throws Exception {
    OBContextCollectionProvider provider = new OBContextCollectionProvider();
    Object o1 = provider.invokeMethod(method);
    Object o2 = provider.invokeMethod(method);
    assertNotSame("Method " + method + " returns a new instance of the collection", o1, o2);
  }

  @SuppressWarnings("unused")
  private class OBContextCollectionProvider {
    private OBContext context;

    private OBContextCollectionProvider() {
      context = OBContext.getOBContext();
    }

    public Object invokeMethod(String methodName) throws Exception {
      Method m = this.getClass().getMethod(method);
      return m.invoke(this);
    }

    public String[] getReadableOrganizations() {
      return context.getReadableOrganizations();
    }

    public Set<String> getWritableOrganizations() {
      return context.getWritableOrganizations();
    }

    public Set<String> getDeactivatedOrganizations() {
      return context.getDeactivatedOrganizations();
    }

    public String[] getReadableClients() {
      return context.getReadableClients();
    }

    public Set<String> getNaturalTree() {
      return context.getOrganizationStructureProvider().getNaturalTree(TEST_ORG_ID);
    }

    public Set<String> getParentTree() {
      return context.getOrganizationStructureProvider().getParentTree(TEST_ORG_ID, true);
    }

    public List<String> getParentList() {
      return context.getOrganizationStructureProvider().getParentList(TEST_ORG_ID, true);
    }

    public Set<String> getChildTree() {
      return context.getOrganizationStructureProvider().getChildTree(TEST_ORG_ID, true);
    }
  }
}
