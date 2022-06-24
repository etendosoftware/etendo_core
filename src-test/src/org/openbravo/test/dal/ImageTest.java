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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.dal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.test.base.TestConstants.Orgs;

/**
 * Tests the correct image handling when saving/updating/deleting entities that have image
 * properties.
 */
public class ImageTest extends WeldBaseTest {

  @Test
  public void imageIsDeleted() {
    String categoryId = createTestProductCategory();
    ProductCategory category = OBDal.getInstance().get(ProductCategory.class, categoryId);
    assertNotNull(category.getImage());
    String imageId = category.getImage().getId();
    OBDal.getInstance().remove(category);
    OBDal.getInstance().commitAndClose();
    assertNull(OBDal.getInstance().get(Image.class, imageId));
  }

  @Test
  public void imageOrgIsUpdated() {
    String categoryId = createTestProductCategory();
    try {
      ProductCategory category = OBDal.getInstance().get(ProductCategory.class, categoryId);
      category.setOrganization(OBDal.getInstance().getProxy(Organization.class, Orgs.ESP));
      OBDal.getInstance().flush();
      assertThat(category.getImage().getOrganization().getId(), equalTo(Orgs.ESP));
    } catch (OBSecurityException ex) {
      OBDal.getInstance().rollbackAndClose();
      fail(ex.getMessage());
    } finally {
      cleanUp(categoryId);
    }
  }

  private String createTestProductCategory() {
    ProductCategory category = OBProvider.getInstance().get(ProductCategory.class);
    category.setOrganization(OBDal.getInstance().getProxy(Organization.class, Orgs.US));
    category.setSearchKey("TestCategory");
    category.setName("TestCategory");
    category.setPlannedMargin(new BigDecimal(0));

    Image img = OBProvider.getInstance().get(Image.class);
    img.setOrganization(OBDal.getInstance().getProxy(Organization.class, Orgs.US));
    img.setName("TestImage");
    category.setImage(img);

    OBDal.getInstance().save(img);
    OBDal.getInstance().save(category);

    OBDal.getInstance().commitAndClose();
    return category.getId();
  }

  private void cleanUp(String categoryId) {
    ProductCategory category = OBDal.getInstance().get(ProductCategory.class, categoryId);
    if (category == null) {
      return;
    }
    OBDal.getInstance().remove(category);
    OBDal.getInstance().commitAndClose();
  }
}
