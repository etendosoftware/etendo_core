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
 * All portions are Copyright (C) 2008-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):
 *   Martin Taal <martin.taal@openbravo.com>,
 *   Ivan Perdomo <ivan.perdomo@openbravo.com>,
 *   Leo Arias <leo.arias@openbravo.com>.
 ************************************************************************
 */

package org.openbravo.test.dal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.domain.ModelImplementation;
import org.openbravo.model.ad.domain.ModelImplementationMapping;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.businesspartner.CategoryAccounts;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test different parts of the dal api: {@link OBDal}, {@link OBCriteria} and {@link OBQuery}.
 * 
 * Note the testcases assume that they are run in the order defined in this class.
 * 
 * @author mtaal
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DalQueryTest extends OBBaseTest {
  private static final Logger log = LogManager.getLogger();

  /**
   * Tests a left join with {@link ModelImplementation} as the main class.
   */
  @Test
  public void testADalFirstWhereLeftJoinClause() {
    setSystemAdministratorContext();
    final String where = "as mo left join mo.callout moc left join mo.reference mor left join mo.specialForm mof left join mo.process mop left join mo.tab mot where moc.module.id='0' or mor.module.id='0' or mof.module.id='0' or mop.module.id='0' or mot.module.id='0'";
    final OBQuery<ModelImplementation> obq = OBDal.getInstance()
        .createQuery(ModelImplementation.class, where);
    assertTrue(obq.list().size() > 0);
  }

  /**
   * Tests a left join with {@link ModelImplementation} as the main class.
   */
  @Test
  @SuppressWarnings("deprecation")
  public void testBDalExtraJoinWhereLeftJoinClause() {
    setSystemAdministratorContext();
    final String where = "as mom left join mom." + ModelImplementationMapping.PROPERTY_MODELOBJECT
        + " as mo left join mo." + ModelImplementation.PROPERTY_CALLOUT + " moc left join mo."
        + ModelImplementation.PROPERTY_REFERENCE + " mor left join mo."
        + ModelImplementation.PROPERTY_SPECIALFORM + " mof left join mo."
        + ModelImplementation.PROPERTY_PROCESS + " mop left join mo."
        + ModelImplementation.PROPERTY_TAB
        + " mot where moc.module.id='0' or mor.module.id='0' or mof.module.id='0' or mop.module.id='0' or mot.module.id='0'";
    final OBQuery<ModelImplementationMapping> obq = OBDal.getInstance()
        .createQuery(ModelImplementationMapping.class, where);
    assertTrue(obq.list().size() > 0);
  }

  /**
   * Tests a left join with {@link ModelImplementation} as the main class.
   */
  @Test
  public void testCDalWhereLeftJoinClause() {
    setSystemAdministratorContext();
    final String where = "as mo left join mo.callout moc left join mo.reference mor where moc.module.id='0' or mor.module.id='0'";
    final OBQuery<ModelImplementation> obq = OBDal.getInstance()
        .createQuery(ModelImplementation.class, where);
    assertTrue(obq.list().size() > 0);
  }

  /**
   * Tests a left join with {@link ModelImplementation} as the main class.
   */
  @Test
  public void testDDalOtherWhereLeftJoinClause() {
    setSystemAdministratorContext();
    final String where = "as mo left join mo.callout moc left join mo.reference mor where (moc.module.id='0' or mor.module.id='0') and exists(from ADUser where id<>'0')";
    final OBQuery<ModelImplementation> obq = OBDal.getInstance()
        .createQuery(ModelImplementation.class, where);
    assertTrue(obq.list().size() > 0);
  }

  /**
   * Tests a left join with {@link ModelImplementation} as the main class.
   */
  @Test
  public void testEDalAnOtherWhereLeftJoinClause() {
    setSystemAdministratorContext();
    final String where = "exists(from ADUser where id<>'0')";
    final OBQuery<ModelImplementation> obq = OBDal.getInstance()
        .createQuery(ModelImplementation.class, where);
    assertTrue(obq.list().size() > 0);
  }

  /**
   * Test creates a new {@link Category} and saves it. The new object is removed in the next test.
   */
  @Test
  public void testFCreateBPGroup() {
    setTestUserContext();
    addReadWriteAccess(Category.class);
    final Category bpg = OBProvider.getInstance().get(Category.class);
    bpg.setDefault(true);
    bpg.setDescription("testdescription");
    bpg.setName("testname");
    bpg.setSearchKey("testvalue");
    bpg.setActive(true);
    OBDal.getInstance().save(bpg);
    OBDal.getInstance().commitAndClose();
  }

  /**
   * Test queries for the created {@link Category} and removes it.
   */
  @Test
  public void testGRemoveBPGroup() {
    setTestUserContext();
    addReadWriteAccess(Category.class);
    addReadWriteAccess(CategoryAccounts.class);
    final OBQuery<Category> obQuery = OBDal.getInstance()
        .createQuery(Category.class, Category.PROPERTY_NAME + "='testname' or "
            + Category.PROPERTY_SEARCHKEY + "='testvalue'");
    final List<Category> bpgs = obQuery.list();
    assertEquals(1, bpgs.size());
    final Category bpg = bpgs.get(0);
    final OBContext obContext = OBContext.getOBContext();
    assertEquals(obContext.getUser().getId(), bpg.getCreatedBy().getId());
    assertEquals(obContext.getUser().getId(), bpg.getUpdatedBy().getId());

    // first delete the related accounts
    final Map<String, Object> parameters = new HashMap<>(1);
    parameters.put("category", bpgs.get(0));
    final OBQuery<CategoryAccounts> q2 = OBDal.getInstance()
        .createQuery(CategoryAccounts.class,
            " " + CategoryAccounts.PROPERTY_BUSINESSPARTNERCATEGORY + "=:category", parameters);
    final List<CategoryAccounts> bpgas = q2.list();
    for (final CategoryAccounts bga : bpgas) {
      OBDal.getInstance().refresh(bga);
      OBDal.getInstance().remove(bga);
    }
    OBDal.getInstance().remove(bpgs.get(0));
    OBDal.getInstance().commitAndClose();
  }

  /**
   * Check that the {@link Category} was indeed removed.
   */
  @Test
  public void testHCheckBPGroupRemoved() {
    setTestUserContext();
    addReadWriteAccess(Category.class);
    final OBQuery<Category> obQuery = OBDal.getInstance()
        .createQuery(Category.class, Category.PROPERTY_NAME + "='testname' or "
            + Category.PROPERTY_SEARCHKEY + "='testvalue'");
    final List<Category> bpgs = obQuery.list();
    assertEquals(0, bpgs.size());
    OBDal.getInstance().flush();
  }

  /**
   * Tests queries for a currency and then updates it. The test should fail as the user does not
   * have update authorisation.
   */
  @Test
  public void testIUpdateCurrencyByUser() {
    setUserContext("E12DC7B3FF8C4F64924A98195223B1F8");
    final OBCriteria<Currency> obc = OBDal.getInstance().createCriteria(Currency.class);
    obc.add(Restrictions.eq(Currency.PROPERTY_ISOCODE, DOLLAR));
    final List<Currency> cs = obc.list();
    assertEquals(1, cs.size());
    final Currency c = cs.get(0);
    // Call getValue and setValue directly to work around security checks on the description
    // that are not the objective of this test.
    c.setValue(Currency.PROPERTY_DESCRIPTION,
        c.getValue(Currency.PROPERTY_DESCRIPTION) + " a test");
    try {
      OBDal.getInstance().save(c);
      fail("No security check");
    } catch (final OBSecurityException e) {
      // successful check, do not commit the change
      rollback();
    }
  }

  /**
   * Tests a paged read of transactions and print of the identifier. The identifier of a transaction
   * has been implemented such that it reads all the references (which are non-null) and uses their
   * identifier to create the identifier of the transaction. The test sorts on product.name.
   */
  @Test
  public void testJTransaction25PageRead() {
    setTestUserContext();
    addReadWriteAccess(MaterialTransaction.class);
    final OBQuery<MaterialTransaction> cq = OBDal.getInstance()
        .createQuery(MaterialTransaction.class, " order by product.name");
    final int count = cq.count();
    final int pageSize = 25;
    int pageCount = 1 + (count / pageSize);
    if (pageCount > 25) {
      pageCount = 25;
    }
    for (int i = 0; i < pageCount; i++) {
      final OBQuery<MaterialTransaction> obq = OBDal.getInstance()
          .createQuery(MaterialTransaction.class,
              " order by " + MaterialTransaction.PROPERTY_PRODUCT + "." + Product.PROPERTY_NAME);
      final Query<MaterialTransaction> qry = obq.createQuery();
      qry.setMaxResults(pageSize);
      qry.setFirstResult(i * pageSize);

      log.debug("PAGE>>> " + (1 + i));
      for (final Object o : qry.list()) {
        log.debug(((MaterialTransaction) o).getIdentifier());
      }
    }
  }
}
