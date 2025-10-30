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
 * All portions are Copyright (C) 2008-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.EntityAccessChecker;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.currency.CurrencyTrl;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;

/**
 * Tests access on the basis of window and table definitions. Also tests derived read access.
 * 
 * IMPORTANT: Test cases are called by one of them called testContent(). The name of the rest of the
 * test cases NOT begin by "test...".
 * 
 * @see EntityAccessChecker
 * 
 * @author mtaal
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EntityAccessTest extends CrossOrganizationReference {

  private static final Logger log = LogManager.getLogger();

  /**
   * Creates test data, a {@link Currency}.
   */
  @Test
  public void testACreateCurrency() {
    setTestAdminContext();
    final OBCriteria<Currency> obc = OBDal.getInstance().createCriteria(Currency.class);
    obc.addEqual(Currency.PROPERTY_ISOCODE, "TE2");
    final List<Currency> cs = obc.list();
    if (cs.size() == 0) {
      final Currency c = OBProvider.getInstance().get(Currency.class);
      c.setSymbol("TE2");
      c.setDescription("test currency");
      c.setISOCode("TE2");
      c.setPricePrecision((long) 5);
      c.setStandardPrecision((long) 6);
      c.setCostingPrecision((long) 4);
      OBDal.getInstance().save(c);
    }

  }

  /**
   * Test tries to remove the {@link Currency}. Which should fail as it is not deletable.
   * 
   * After fixing issue #0010139, all entities are deletable. Therefore this test case is not going
   * to be executed.
   */
  @Ignore("This test is currently disabled because after fixing issue #0010139, all entities are deletable.")
  @Test
  public void testBDoNotExecutetestNonDeletable() {
    setTestUserContext();
    addReadWriteAccess(Currency.class);
    final OBCriteria<Currency> obc = OBDal.getInstance().createCriteria(Currency.class);
    obc.addEqual(Currency.PROPERTY_ISOCODE, "TE2");
    final List<Currency> cs = obc.list();
    assertEquals(1, cs.size());
    final Currency c = cs.get(0);
    try {
      OBDal.getInstance().remove(c);
      OBDal.getInstance().commitAndClose();
      fail("Currency should be non-deletable");
    } catch (final OBSecurityException e) {
      assertTrue("Wrong exception thrown:  " + e.getMessage(),
          e.getMessage().indexOf("is not deletable") != -1);
    }
  }

  /**
   * Checks the derived readable concept, only identifier fields of a derived readable object may be
   * read. Also checks the allowRead concept of a BaseOBObject (
   * {@link BaseOBObject#setAllowRead(boolean)})
   */
  @Test
  public void testCCheckDerivedReadableCurrency() {
    setUserContext(TEST2_USER_ID);
    final Currency c = OBDal.getInstance().get(Currency.class, DOLLAR_ID);
    log.debug(c.getIdentifier());
    log.debug(c.getId());
    try {
      log.debug(c.getCostingPrecision());
      fail("Derived readable not applied");
    } catch (final OBSecurityException e) {
      assertTrue("Wrong exception thrown:  " + e.getMessage(),
          e.getMessage().indexOf("is not directly readable") != -1);

      try {
        c.setAllowRead(true);
        fail("Allow read my only be called in adminmode");
      } catch (OBSecurityException x) {
        OBContext.setAdminMode();
        try {
          c.setAllowRead(true);
        } finally {
          OBContext.restorePreviousMode();
        }
        // this should be allowed
        log.debug(c.getCostingPrecision());
        // set back
        OBContext.setAdminMode();
        try {
          c.setAllowRead(false);
        } finally {
          OBContext.restorePreviousMode();
        }
        try {
          c.setAllowRead(true);
          fail("Allow read my only be called in adminmode");
        } catch (OBSecurityException y) {
          // okay
        }
      }
    }
  }

  /**
   * Test derived readable on a set method, also there this check must be done.
   */
  @Test
  public void testDUpdateCurrencyDerivedRead() {
    setUserContext(TEST2_USER_ID);
    final Currency c = OBDal.getInstance().get(Currency.class, DOLLAR_ID);
    try {
      c.setCostingPrecision((long) 5);
      fail("Derived readable not checked on set");
    } catch (final OBSecurityException e) {
      assertTrue("Wrong exception thrown:  " + e.getMessage(),
          e.getMessage().indexOf("is not directly readable") != -1);
    }
    try {
      OBDal.getInstance().save(c);
      fail("No security check");
    } catch (final OBSecurityException e) {
      // successfull check
      assertTrue("Wrong exception thrown:  " + e.getMessage(),
          e.getMessage().indexOf("is not writable by this user") != -1);
    }
  }

  /**
   * Checks non-readable, if an object/entity is not readable then it may not be read through the
   * {@link OBDal}.
   */
  @Test
  public void testENonReadable() {
    assertTrue(true);
    // FIXME: find a test case for this!

    // setUserContext(getRandomUserId());
    // try {
    // final OBCriteria<Costing> obc = OBDal.getInstance().createCriteria(Costing.class);
    // obc.addEqual(Costing.PROPERTY_ID, "FE8370A36E91432688A323A07D606622");
    // final List<Costing> cs = obc.list();
    // assertTrue(cs.size() > 0);
    // fail("Non readable check not enforced");
    // } catch (final OBSecurityException e) {
    // assertTrue("Wrong exception thrown: " + e.getMessage(), e.getMessage().indexOf(
    // "is not readable") != -1);
    // }
  }

  /**
   * Removes the test data by using the administrator account.
   */
  public void testFDeleteTestData() {
    setTestUserContext();
    addReadWriteAccess(Currency.class);
    addReadWriteAccess(CurrencyTrl.class);
    final OBCriteria<Currency> obc = OBDal.getInstance().createCriteria(Currency.class);
    obc.addEqual(Currency.PROPERTY_ISOCODE, "TE2");
    final List<Currency> cs = obc.list();
    assertEquals(1, cs.size());
    OBDal.getInstance().remove(cs.get(0));
  }

  /**
   * Covers issue #36628: it was not possible to update organization if entity had computed columns
   */
  @Test
  public void changeOrgIsAllowedHavingComputedColumns() {
    setQAAdminContext();
    Order order = createOrder(SPAIN_ORG);
    String orderId = order.getId();

    // reload it from DB so that computed columns property is not null
    OBDal.getInstance().getSession().evict(order);
    order = OBDal.getInstance().get(Order.class, orderId);

    order.setOrganization(OBDal.getInstance().getProxy(Organization.class, USA_ORG));
    order.setWarehouse(OBDal.getInstance().getProxy(Warehouse.class, USA_WAREHOUSE));
    order.setBusinessPartner(OBDal.getInstance().getProxy(BusinessPartner.class, USA_BP));
    OBDal.getInstance().flush();
  }
}
