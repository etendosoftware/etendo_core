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
 * All portions are Copyright (C) 2012-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.dal;

import java.util.List;
import java.util.UUID;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.junit.Assert;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test case to try and test proxy loading or stateless sessions.
 * 
 * @author mtaal
 */

public class DalPerformanceCriteriaTest extends OBBaseTest {

  private static final int CNT = 1000;

  @Test
  public void testPerformance() {
    createManyBPs();

    doTestCriteriaPerformance(new QueryTest1(false));
    doTestCriteriaPerformance(new QueryTest2(false));
    doTestCriteriaPerformance(new QueryTest3(false));
    doTestCriteriaPerformance(new QueryTest4(false));
    doTestCriteriaPerformance(new QueryTest5(false));
    doTestCriteriaPerformance(new QueryTest1(true));
    doTestCriteriaPerformance(new QueryTest2(true));
    doTestCriteriaPerformance(new QueryTest3(true));
    doTestCriteriaPerformance(new QueryTest4(true));
    doTestCriteriaPerformance(new QueryTest5(true));
  }

  public void doTestCriteriaPerformance(QueryTest queryTest) {
    OBDal.getInstance().commitAndClose();

    // usefull when printing
    if (true) {
      // warmup
      queryTest.doCriteriaQry();
      // show sql
      queryTest.doCriteriaQry();
      queryTest.doHqlQry();
    }

    // warmup
    for (int i = 0; i < 10; i++) {
      queryTest.doCriteriaQry();
    }

    long t1 = System.currentTimeMillis();
    int v1 = -1;
    for (int i = 0; i < CNT; i++) {
      int v2 = queryTest.doCriteriaQry();
      Assert.assertTrue(i == 0 || v1 == v2);
      v1 = v2;
      OBDal.getInstance().getSession().clear();
    }
    OBDal.getInstance().commitAndClose();
    t1 = System.currentTimeMillis() - t1;

    // warmup
    for (int i = 0; i < 10; i++) {
      queryTest.doHqlQry();
    }

    long t2 = System.currentTimeMillis();
    for (int i = 0; i < CNT; i++) {
      int v2 = queryTest.doHqlQry();
      // same output as previous criteria/hql queries
      Assert.assertTrue(v1 == v2);
      v1 = v2;
      OBDal.getInstance().getSession().clear();
    }
    OBDal.getInstance().commitAndClose();
    t2 = System.currentTimeMillis() - t2;
    int percentage = (int) ((100 * (t1 - t2)) / t2);
    System.err.println("HQL is " + percentage + "% faster - (resultCount: " + v1 + ") - "
        + (queryTest.isDoScroll() ? "SCROLL - Query: " : "Query: ") + queryTest.getId()
        + " - HQL time: " + t1 + "ms");
  }

  private abstract class QueryTest {
    public abstract int doCriteriaQry();

    public abstract int doHqlQry();

    public abstract String getId();

    public abstract boolean isDoScroll();

  }

  @Test
  public void testCriteriaScrollable() {

    OBCriteria<BusinessPartner> c = OBDal.getInstance().createCriteria(BusinessPartner.class);
    ScrollableResults iterator = c.scroll(ScrollMode.FORWARD_ONLY);
    iterator.next();
  }

  private class QueryTest1 extends QueryTest {

    private String qryStr = "";
    private boolean doScroll = false;

    public QueryTest1(boolean doScroll) {
      this.doScroll = doScroll;
    }

    @Override
    public int doCriteriaQry() {
      final OBCriteria<Currency> obc = OBDal.getInstance().createCriteria(Currency.class);
      obc.add(Restrictions.eq(Currency.PROPERTY_ISOCODE, DOLLAR));
      if (doScroll) {
        final ScrollableResults r = obc.scroll(ScrollMode.FORWARD_ONLY);
        int cnt = 0;
        while (r.next()) {
          cnt++;
        }
        return cnt;
      }
      final List<Currency> cs = obc.list();
      return cs.size();
    }

    @Override
    public int doHqlQry() {
      final OBQuery<Currency> obq = OBDal.getInstance()
          .createQuery(Currency.class, "iSOCode='USD'");
      final List<Currency> cs = obq.list();
      qryStr = "Currency with " + obq.getWhereAndOrderBy();
      return cs.size();
    }

    @Override
    public String getId() {
      return qryStr;
    }

    @Override
    public boolean isDoScroll() {
      return doScroll;
    }
  }

  private class QueryTest2 extends QueryTest {
    private String qryStr;
    private boolean doScroll = false;

    public QueryTest2(boolean doScroll) {
      this.doScroll = doScroll;
    }

    @Override
    public int doCriteriaQry() {
      final OBCriteria<Currency> obc = OBDal.getInstance().createCriteria(Currency.class);
      if (doScroll) {
        final ScrollableResults r = obc.scroll(ScrollMode.FORWARD_ONLY);
        int cnt = 0;
        while (r.next()) {
          cnt++;
        }
        return cnt;
      }
      final List<Currency> cs = obc.list();
      return cs.size();
    }

    @Override
    public int doHqlQry() {
      final OBQuery<Currency> obq = OBDal.getInstance().createQuery(Currency.class, "");
      final List<Currency> cs = obq.list();
      qryStr = "Currency";
      return cs.size();
    }

    @Override
    public String getId() {
      return qryStr;
    }

    @Override
    public boolean isDoScroll() {
      return doScroll;
    }
  }

  private class QueryTest3 extends QueryTest {
    private String qryStr;
    private boolean doScroll = false;

    public QueryTest3(boolean doScroll) {
      this.doScroll = doScroll;
    }

    @Override
    public int doCriteriaQry() {
      final OBCriteria<MaterialTransaction> obc = OBDal.getInstance()
          .createCriteria(MaterialTransaction.class);
      obc.add(Restrictions.isNotNull(MaterialTransaction.PROPERTY_UOM));
      obc.addOrderBy(MaterialTransaction.PROPERTY_PRODUCT + "." + Product.PROPERTY_NAME, false);
      obc.setMaxResults(10);
      obc.setFirstResult(0);
      if (doScroll) {
        final ScrollableResults r = obc.scroll(ScrollMode.FORWARD_ONLY);
        int cnt = 0;
        while (r.next()) {
          cnt++;
        }
        return cnt;
      }
      final List<MaterialTransaction> cs = obc.list();
      return cs.size();
    }

    @Override
    public int doHqlQry() {
      final OBQuery<MaterialTransaction> obq = OBDal.getInstance()
          .createQuery(MaterialTransaction.class, " uOM <> null order by product.name desc");
      obq.setMaxResult(10);
      obq.setFirstResult(0);
      final List<MaterialTransaction> cs = obq.list();
      qryStr = "MaterialTransaction with " + obq.getWhereAndOrderBy();
      return cs.size();
    }

    @Override
    public String getId() {
      return qryStr;
    }

    @Override
    public boolean isDoScroll() {
      return doScroll;
    }
  }

  private class QueryTest4 extends QueryTest {
    private String qryStr;
    private boolean doScroll = false;

    public QueryTest4(boolean doScroll) {
      this.doScroll = doScroll;
    }

    @Override
    public int doCriteriaQry() {
      OBCriteria<Table> c = OBDal.getInstance().createCriteria(Table.class);
      c.add(Restrictions.eq(Table.PROPERTY_ID, "100"));
      if (doScroll) {
        final ScrollableResults r = c.scroll(ScrollMode.FORWARD_ONLY);
        int cnt = 0;
        while (r.next()) {
          cnt++;
        }
        return cnt;
      }
      final List<Table> cs = c.list();
      return cs.size();
    }

    @Override
    public int doHqlQry() {
      OBQuery<Table> q = OBDal.getInstance().createQuery(Table.class, "id = :id");
      q.setNamedParameter("id", "100");
      final List<Table> cs = q.list();
      qryStr = "Table with id=100";
      return cs.size();
    }

    @Override
    public String getId() {
      return qryStr;
    }

    @Override
    public boolean isDoScroll() {
      return doScroll;
    }
  }

  private class QueryTest5 extends QueryTest {
    private String qryStr;
    private boolean doScroll = false;

    public QueryTest5(boolean doScroll) {
      this.doScroll = doScroll;
    }

    @Override
    public int doCriteriaQry() {
      OBCriteria<BusinessPartner> c = OBDal.getInstance().createCriteria(BusinessPartner.class);
      c.setFilterOnActive(false);
      c.setFilterOnReadableClients(false);
      c.setFilterOnReadableOrganization(false);
      c.setMaxResults(1000);
      if (doScroll) {
        final ScrollableResults r = c.scroll(ScrollMode.FORWARD_ONLY);
        int cnt = 0;
        while (r.next()) {
          cnt++;
        }
        return cnt;
      }
      final List<BusinessPartner> cs = c.list();
      return cs.size();
    }

    @Override
    public int doHqlQry() {
      OBQuery<BusinessPartner> q = OBDal.getInstance().createQuery(BusinessPartner.class, "");
      q.setFilterOnActive(false);
      q.setFilterOnReadableClients(false);
      q.setFilterOnReadableOrganization(false);
      q.setMaxResult(1000);
      qryStr = "All BusinessPartners";
      if (doScroll) {
        int cnt = 0;
        ScrollableResults scroller = q.scroll(ScrollMode.FORWARD_ONLY);
        while (scroller.next()) {
          cnt++;
        }
        return cnt;
      }
      final List<BusinessPartner> cs = q.list();
      return cs.size();
    }

    @Override
    public String getId() {
      return qryStr;
    }

    @Override
    public boolean isDoScroll() {
      return doScroll;
    }
  }

  private void createManyBPs() {
    try {
      setTestAdminContext();

      final OBQuery<BusinessPartner> bps = OBDal.getInstance()
          .createQuery(BusinessPartner.class, "");
      bps.setFilterOnActive(false);
      bps.setFilterOnReadableClients(false);
      bps.setFilterOnReadableOrganization(false);
      System.err.println(bps.count());
      if (bps.count() > 10000) {
        return;
      }

      OBDal.getInstance().commitAndClose();

      for (int i = 0; i < 10000; i++) {
        BusinessPartner bp = OBProvider.getInstance().get(BusinessPartner.class);

        // Generating random strings for testing
        UUID name = UUID.randomUUID();
        UUID key = UUID.randomUUID();

        bp.setName(name.toString());
        bp.setSearchKey(key.toString());

        final Category category = OBDal.getInstance().getProxy(Category.class, TEST_BP_CATEGORY_ID);
        bp.setBusinessPartnerCategory(category);

        OBDal.getInstance().save(bp);
        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
        }
        // this all works
        // OBDal.getInstance().refresh(bp);
        // Assert.assertTrue(bp.getId() != null);

        // check that if really loading that still the proxy object is returned
        // Assert.assertTrue(category == OBDal.getInstance().get(Category.ENTITY_NAME,
        // TEST_BP_CATEGORY_ID));
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /*
   * List<FIN_PaymentSchedule> lQuery, lReturn = new ArrayList<FIN_PaymentSchedule>();
   * OBCriteria<FIN_PaymentSchedule> obcPS = OBDal.getInstance().createCriteria(
   * FIN_PaymentSchedule.class); obcPS.add(Restrictions.eq(FIN_PaymentSchedule.PROPERTY_INVOICE,
   * invoice)); lQuery = obcPS.list();
   * 
   * // 1) Remove not paid payment schedule detail lines OBCriteria<FIN_PaymentScheduleDetail>
   * obcPSD = OBDal.getInstance().createCriteria( FIN_PaymentScheduleDetail.class);
   * obcPSD.add(Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE,
   * invoicePS));
   * obcPSD.add(Restrictions.isNull(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS));
   * 
   * OBCriteria<FIN_PaymentScheduleDetail> orderedPSDs = OBDal.getInstance().createCriteria(
   * FIN_PaymentScheduleDetail.class);
   * orderedPSDs.add(Restrictions.in(FIN_PaymentScheduleDetail.PROPERTY_ID, psdSet));
   * orderedPSDs.addOrderBy(FIN_PaymentScheduleDetail.PROPERTY_AMOUNT, true);
   * 
   * OBCriteria<FinAccPaymentMethod> psdFilter = OBDal.getInstance().createCriteria(
   * FinAccPaymentMethod.class); psdFilter.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT,
   * finAcc)); psdFilter.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD,
   * finPmtMethod));
   * 
   * OBCriteria<FIN_Payment> obcPayment = OBDal.getInstance().createCriteria(FIN_Payment.class);
   * obcPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_BUSINESSPARTNER, bp));
   * obcPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_RECEIPT, isReceipt));
   * obcPayment.add(Restrictions.ne(FIN_Payment.PROPERTY_GENERATEDCREDIT, BigDecimal.ZERO));
   * obcPayment.add(Restrictions.ne(FIN_Payment.PROPERTY_USEDCREDIT, BigDecimal.ZERO));
   * obcPayment.addOrderBy(FIN_Payment.PROPERTY_PAYMENTDATE, false);
   * obcPayment.addOrderBy(FIN_Payment.PROPERTY_DOCUMENTNO, false); return obcPayment.list();
   * 
   * final OBCriteria<RoleOrganization> roleOrgs = OBDal.getInstance().createCriteria(
   * RoleOrganization.class); roleOrgs.add(Restrictions.eq(RoleOrganization.PROPERTY_ROLE, role));
   * roleOrgs.add(Restrictions.eq(RoleOrganization.PROPERTY_ORGADMIN, true));
   * 
   * OBCriteria<ModuleInstall> qModInstall = OBDal.getInstance().createCriteria(
   * ModuleInstall.class);
   */

}
