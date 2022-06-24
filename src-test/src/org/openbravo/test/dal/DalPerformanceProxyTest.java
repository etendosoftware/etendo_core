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
 * All portions are Copyright (C) 2012-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.dal;

import java.util.Date;
import java.util.UUID;

import org.hibernate.StatelessSession;
import org.hibernate.proxy.HibernateProxy;
import org.junit.Assert;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test case to try and test proxy loading or stateless sessions.
 * 
 * @author mtaal
 */

public class DalPerformanceProxyTest extends OBBaseTest {

  private static final int CNT = 10;

  @Test
  public void testProxyBPCreate() {
    try {
      setTestAdminContext();

      OBDal.getInstance().commitAndClose();

      for (int i = 0; i < CNT; i++) {
        BusinessPartner bp = OBProvider.getInstance().get(BusinessPartner.class);

        // Generating random strings for testing
        UUID name = UUID.randomUUID();
        UUID key = UUID.randomUUID();

        bp.setName(name.toString());
        bp.setSearchKey(key.toString());

        final Category category = (Category) OBDal.getInstance()
            .getProxy(Category.ENTITY_NAME, TEST_BP_CATEGORY_ID);
        bp.setBusinessPartnerCategory(category);

        // should not be initialized
        // only check the first time as after the first loop
        // the category is loaded because of the refresh below.
        if (i == 0) {
          Assert.assertTrue(
              ((HibernateProxy) category).getHibernateLazyInitializer().isUninitialized());
        } else {
          Assert.assertFalse(
              ((HibernateProxy) category).getHibernateLazyInitializer().isUninitialized());
        }

        OBDal.getInstance().save(bp);
        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
          System.err.println(i);
          // this all works
          // note: this loads the category proxy
          OBDal.getInstance().refresh(bp);
          Assert.assertTrue(bp.getId() != null);

          // check that if really loading that still the proxy object is returned
          Assert.assertTrue(
              category == OBDal.getInstance().get(Category.ENTITY_NAME, TEST_BP_CATEGORY_ID));
        }
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  @Test
  public void testStatelessBPCreate() {
    try {
      setTestAdminContext();

      DalConnectionProvider dcp = new DalConnectionProvider(false);
      final StatelessSession session = SessionFactoryController.getInstance()
          .getSessionFactory()
          .openStatelessSession(dcp.getConnection());
      session.beginTransaction();
      for (int i = 0; i < CNT; i++) {
        BusinessPartner bp = OBProvider.getInstance().get(BusinessPartner.class);

        // Generating random strings for testing
        UUID name = UUID.randomUUID();
        UUID key = UUID.randomUUID();

        bp.setName(name.toString());
        bp.setSearchKey(key.toString());

        bp.setBusinessPartnerCategory(createReferencedObject(Category.class, TEST_BP_CATEGORY_ID));

        // note the following things are currently done in the OBInterceptor
        // it is quite easy to add a util method which can do this in a generic
        // way for any business object
        bp.setOrganization(createReferencedObject(Organization.class, TEST_ORG_ID));
        bp.setClient(createReferencedObject(Client.class, TEST_CLIENT_ID));
        bp.setCreatedBy(createReferencedObject(User.class, "100"));
        bp.setCreationDate(new Date());
        bp.setUpdatedBy(createReferencedObject(User.class, "100"));
        bp.setUpdated(new Date());

        session.insert(BusinessPartner.ENTITY_NAME, bp);
        // session.refresh(BusinessPartner.ENTITY_NAME, bp);
        Assert.assertTrue(bp.getId() != null);
      }
      session.getTransaction().commit();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  public <T extends BaseOBObject> T createReferencedObject(Class<T> clz, String id) {
    final T instance = OBProvider.getInstance().get(clz);
    instance.setId(id);
    return instance;
  }

}
