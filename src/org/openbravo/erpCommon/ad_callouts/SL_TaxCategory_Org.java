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
package org.openbravo.erpCommon.ad_callouts;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.util.List;

import jakarta.servlet.ServletException;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.financialmgmt.tax.TaxCategory;

public class SL_TaxCategory_Org extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    final String strOrgId = info.getStringParameter("inpadOrgId", null);

    Organization organization = OBDal.getInstance().get(Organization.class, strOrgId);
    String taxCategoryId = "";
    String parentOrgId = "";
    String whereClause = "";

    while ("".equals(taxCategoryId)) {
      whereClause = "as tn where tn.node = :organizationId and tn.client.id = :clientId";
      OBCriteria<TaxCategory> taxCategory = OBDal.getInstance().createCriteria(TaxCategory.class);
      taxCategory.addEqual(TaxCategory.PROPERTY_ORGANIZATION, organization);
      taxCategory.addEqual(TaxCategory.PROPERTY_DEFAULT, true);
      taxCategory.setMaxResults(1);
      List<TaxCategory> listTaxCategory = taxCategory.list();
      TaxCategory taxCategoryObject = (!listTaxCategory.isEmpty() ? listTaxCategory.get(0) : null);
      if (taxCategoryObject == null && !"0".equals(organization.getId())) {
        OBQuery<TreeNode> query = OBDal.getInstance().createQuery(TreeNode.class, whereClause);
        query.setNamedParameter("organizationId", organization.getId());
        query.setNamedParameter("clientId", organization.getClient().getId());
        query.setMaxResult(1);
        List<TreeNode> listTreeNode = query.list();
        TreeNode treeNode = listTreeNode.get(0);
        parentOrgId = treeNode.getReportSet();
        organization = OBDal.getInstance().get(Organization.class, parentOrgId);
      } else {
        taxCategoryId = taxCategoryObject != null ? taxCategoryObject.getId() : "";
        break;
      }
    }
    info.addResult("inpcTaxcategoryId", taxCategoryId);

    if (strOrgId != null && !"".equals(strOrgId)) {
      try {
        OBContext.setAdminMode();
        String defaultCategoryId = getDefaultCategory(strOrgId);
        if (!defaultCategoryId.isEmpty()) {
          info.addResult("inpmProductCategoryId", defaultCategoryId);
        }
      } finally {
        OBContext.restorePreviousMode();
      }
    }
  }

  private String getDefaultCategory(String strOrgId) {
    OBContext.setAdminMode();
    try {
      OBCriteria<ProductCategory> productCatCrit = OBDal.getInstance().createCriteria(ProductCategory.class);
      productCatCrit.addAnd((cb, obc) -> cb.equal(obc.getPath(ProductCategory.PROPERTY_ORGANIZATION + "." + Organization.PROPERTY_ID), strOrgId),
                            (cb, obc) -> cb.equal(obc.getPath(ProductCategory.PROPERTY_DEFAULT), true));
      productCatCrit.addEqual(ProductCategory.PROPERTY_SUMMARYLEVEL, false);
      productCatCrit.setMaxResults(1);
      List<ProductCategory> categories = productCatCrit.list();
      if (categories.size() > 0) {
        return categories.get(0).getId();
      } else {
        String parentOrg = OBContext.getOBContext()
            .getOrganizationStructureProvider()
            .getParentOrg(strOrgId);
        if (parentOrg != null && !"".equals(parentOrg)) {
          return getDefaultCategory(parentOrg);
        }
      }
      return "";
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
