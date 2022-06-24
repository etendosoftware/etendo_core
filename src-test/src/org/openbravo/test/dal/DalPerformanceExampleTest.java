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
 * All portions are Copyright (C) 2012-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.dal;

import java.util.List;
import java.util.UUID;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.Query;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.test.base.OBBaseTest;

/**
 * Contains examples used to explain Data Access Layer performance.
 * 
 * http://wiki.openbravo.com/wiki/Data_Access_Layer_Performance
 * 
 * @author mtaal
 */

public class DalPerformanceExampleTest extends OBBaseTest {

  /**
   * Simple query for all products.
   */
  @Test
  public void testSimpleQuery() {
    setTestUserContext();

    final OBCriteria<Product> productCriteria = OBDal.getInstance().createCriteria(Product.class);
    for (Product product : productCriteria.list()) {
      System.err.println(product.getId());
    }

    final OBQuery<Product> productQuery = OBDal.getInstance().createQuery(Product.class, "");
    for (Product product : productQuery.list()) {
      System.err.println(product.getId());
    }
  }

  /**
   * Scrollable results
   */
  @Test
  public void testSimpleScrollQuery() {
    setTestUserContext();

    final OBCriteria<Product> productCriteria = OBDal.getInstance().createCriteria(Product.class);
    // 1000 is normally a good fetch size
    productCriteria.setFetchSize(1000);
    final ScrollableResults productScroller1 = productCriteria.scroll(ScrollMode.FORWARD_ONLY);
    int i = 0;
    while (productScroller1.next()) {
      final Product product = (Product) productScroller1.get()[0];
      System.err.println(product.getId());
      // clear the session every 100 records
      if ((i % 100) == 0) {
        OBDal.getInstance().getSession().clear();
      }
      i++;
    }

    i = 0;
    final OBQuery<Product> productQuery = OBDal.getInstance().createQuery(Product.class, "");
    // 1000 is normally a good fetch size
    productQuery.setFetchSize(1000);
    final ScrollableResults productScroller2 = productQuery.scroll(ScrollMode.FORWARD_ONLY);
    while (productScroller2.next()) {
      final Product product = (Product) productScroller2.get()[0];
      System.err.println(product.getId());
      // clear the session every 100 records
      if ((i % 100) == 0) {
        OBDal.getInstance().getSession().clear();
      }
      i++;
    }
  }

  /**
   * Scrollable results
   */
  @Test
  public void testInsertBPs() {
    setTestUserContext();
    final int cnt = 100;
    final String[] names = new String[cnt];
    for (int i = 0; i < cnt; i++) {
      names[i] = UUID.randomUUID().toString();
    }
    int i = 0;
    for (String name : names) {
      BusinessPartner bp = OBProvider.getInstance().get(BusinessPartner.class);

      bp.setName(name);
      bp.setSearchKey(name);

      final Category category = (Category) OBDal.getInstance()
          .getProxy(Category.ENTITY_NAME, TEST_BP_CATEGORY_ID);
      bp.setBusinessPartnerCategory(category);
      OBDal.getInstance().save(bp);
      if ((i % 100) == 0) {
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();
        System.err.println(i);
      }
      i++;
    }
    OBDal.getInstance().commitAndClose();
  }

  /**
   * Proxy
   */
  @Test
  public void testShowProxy() {
    setTestUserContext();

    final OBQuery<BusinessPartner> bpQuery = OBDal.getInstance()
        .createQuery(BusinessPartner.class, "");
    bpQuery.setMaxResult(1);
    // read one business partner
    final BusinessPartner bp = bpQuery.list().get(0);
    final Category category = bp.getBusinessPartnerCategory();

    // this category is an uninitialized proxy
    Assert.assertTrue(category instanceof HibernateProxy);
    Assert.assertTrue(((HibernateProxy) category).getHibernateLazyInitializer().isUninitialized());

    // get the id and entityname in a way which does not load the object
    category.getId();

    // still unloaded
    Assert.assertTrue(((HibernateProxy) category).getHibernateLazyInitializer().isUninitialized());

    // now call a method directly on the object
    category.getName();

    // now it is loaded!
    Assert.assertFalse(((HibernateProxy) category).getHibernateLazyInitializer().isUninitialized());
  }

  /**
   * Show object graph loading
   */
  @Test
  public void testShowObjectGraph() {
    setTestUserContext();

    final OBQuery<BusinessPartner> bpQuery = OBDal.getInstance()
        .createQuery(BusinessPartner.class, "");
    bpQuery.setMaxResult(1);
    // read one business partner
    final BusinessPartner bp = bpQuery.list().get(0);
    final Category category = bp.getBusinessPartnerCategory();

    // now load the category directly
    final OBQuery<Category> categoryQuery = OBDal.getInstance()
        .createQuery(Category.class, "id=:id");
    categoryQuery.setFilterOnActive(false);
    categoryQuery.setFilterOnReadableClients(false);
    categoryQuery.setFilterOnReadableOrganization(false);
    categoryQuery.setNamedParameter("id", category.getId());
    final Category category2 = categoryQuery.list().get(0);

    // category2 and category are the same object:
    Assert.assertTrue(category == category2);
  }

  /**
   * Show collection loading
   */
  @Test
  public void testShowCollectionLoading() {
    setTestUserContext();

    final OBQuery<Order> orderQuery = OBDal.getInstance().createQuery(Order.class, "");
    orderQuery.setMaxResult(1);
    Order order = orderQuery.uniqueResult();
    final List<OrderLine> orderLineList = order.getOrderLineList();

    // is a hibernate special thing
    Assert.assertTrue(orderLineList instanceof PersistentBag);

    // this will load the list, all OrderLines are loaded in Memory
    System.err.println(orderLineList.size());
  }

  /**
   * Show explicit joining of referenced objects
   */
  @Test
  public void testJoinReferencedObjects() {
    setTestUserContext();
    {
      int i = 0;
      final OBQuery<BusinessPartner> bpQuery = OBDal.getInstance()
          .createQuery(BusinessPartner.class, "");
      bpQuery.setMaxResult(1000);
      final ScrollableResults scroller = bpQuery.scroll(ScrollMode.FORWARD_ONLY);
      while (scroller.next()) {
        final BusinessPartner bp = (BusinessPartner) scroller.get()[0];

        // this will load the category object with a separate query
        System.err.println(bp.getBusinessPartnerCategory().getIdentifier());

        // clear the session every 100 records
        if ((i % 10) == 0) {
          OBDal.getInstance().getSession().clear();
        }
        i++;
      }
    }

    // start with an empty session
    OBDal.getInstance().getSession().clear();

    {
      int i = 0;
      // for joining referenced objects we can't use OBQuery, but have to
      // use a direct Hibernate query object
      final String queryStr = "from BusinessPartner as bp left join bp.businessPartnerCategory where bp.organization.id in :orgs";

      final Query<Object[]> qry = OBDal.getInstance()
          .getSession()
          .createQuery(queryStr, Object[].class)
          .setParameterList("orgs", OBContext.getOBContext().getReadableOrganizations())
          .setMaxResults(1000);
      final ScrollableResults scroller = qry.scroll(ScrollMode.FORWARD_ONLY);
      while (scroller.next()) {
        final BusinessPartner bp = (BusinessPartner) scroller.get()[0];

        // the category is already loaded, so this won't fire a query
        System.err.println(bp.getBusinessPartnerCategory().getIdentifier());

        // clear the session every 100 records
        if ((i % 100) == 0) {
          OBDal.getInstance().getSession().clear();
        }
        i++;
      }
    }
  }

  /**
   * DML Style operations
   */
  @Test
  public void testDML() {
    // for example add an a to all categories
    String hqlVersionedUpdate = "update BusinessPartnerCategory set name = CONCAT(name, 'a') where name <> null";
    int updatedEntities = OBDal.getInstance()
        .getSession()
        .createQuery(hqlVersionedUpdate)
        .executeUpdate();
    System.err.println(updatedEntities);
  }

  /**
   * Scrollable results
   */
  @Test
  @Ignore("Example that creates 1000000 copies of an order")
  public void testInsertSalesOrders() {
    setTestUserContext();
    final int cnt = 100;
    final String[] names = new String[cnt];
    for (int i = 0; i < cnt; i++) {
      names[i] = UUID.randomUUID().toString();
    }
    final Order baseOrder = OBDal.getInstance()
        .get(Order.class, "00247EB519C941DDA87A7F5630421924");
    for (int i = 0; i < 1000000; i++) {
      final Order copy = (Order) DalUtil.copy(baseOrder, true, true);
      copy.getOrderLineTaxList().clear();
      copy.getOrderTaxList().clear();
      for (OrderLine ol : copy.getOrderLineList()) {
        ol.getOrderLineTaxList().clear();
      }
      copy.setPosted("N");
      copy.setProcessed(false);
      OBDal.getInstance().save(copy);
      if ((i % 100) == 0) {
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();
        System.err.println(i);
      }
      i++;
    }
    OBDal.getInstance().commitAndClose();
  }
}
