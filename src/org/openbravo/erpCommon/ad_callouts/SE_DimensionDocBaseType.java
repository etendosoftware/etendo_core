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
 * All portions are Copyright (C) 2012-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.util.List;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.DimensionDisplayUtility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.DimensionMapping;

public class SE_DimensionDocBaseType extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    final String strDocBaseType = info.getStringParameter("inpdocbasetype", null);
    final String strDimension = info.getStringParameter("inpdimension", null);
    // Compute header
    java.util.List<DimensionMapping> dMapping = getMapping(strDocBaseType, strDimension,
        DimensionDisplayUtility.DIM_Header);
    if (dMapping.size() > 0) {
      info.addResult("inpdimShowH", "Y");
      info.addResult("inpdimRoH", dMapping.get(0).isMandatory() ? "Y" : "N");
    } else {
      info.addResult("inpdimShowH", "N");
      info.addResult("inpdimRoH", "N");
    }

    // Compute Lines
    dMapping = getMapping(strDocBaseType, strDimension, DimensionDisplayUtility.DIM_Lines);
    if (dMapping.size() > 0) {
      info.addResult("inpdimShowL", "Y");
      info.addResult("inpdimRoL", dMapping.get(0).isMandatory() ? "Y" : "N");
    } else {
      info.addResult("inpdimShowL", "N");
      info.addResult("inpdimRoL", "N");
    }

    // Compute breakdown
    dMapping = getMapping(strDocBaseType, strDimension, DimensionDisplayUtility.DIM_BreakDown);
    if (dMapping.size() > 0) {
      info.addResult("inpdimShowBd", "Y");
      info.addResult("inpdimRoBd", dMapping.get(0).isMandatory() ? "Y" : "N");
    } else {
      info.addResult("inpdimShowBd", "N");
      info.addResult("inpdimRoBd", "N");
    }
    Client client = OBContext.getOBContext().getCurrentClient();
    info.addResult("inpshowInHeader",
        getValue(client, DimensionDisplayUtility.DIM_Header, strDimension));
    info.addResult("inpshowInLines",
        getValue(client, DimensionDisplayUtility.DIM_Lines, strDimension));
    info.addResult("inpshowInBreakdown",
        getValue(client, DimensionDisplayUtility.DIM_BreakDown, strDimension));

  }

  private List<DimensionMapping> getMapping(String docbaseType, String dimension, String level) {
    OBCriteria<DimensionMapping> odm = OBDal.getInstance().createCriteria(DimensionMapping.class);
    odm.add(Restrictions.eq(DimensionMapping.PROPERTY_DOCUMENTCATEGORY, docbaseType));
    odm.add(Restrictions.eq(DimensionMapping.PROPERTY_ACCOUNTINGDIMENSION, dimension));
    odm.add(Restrictions.eq(DimensionMapping.PROPERTY_LEVEL, level));
    odm.setFilterOnReadableClients(false);
    odm.setFilterOnReadableOrganization(false);
    return odm.list();
  }

  private String getValue(Client client, String level, String dimension) {
    if (DimensionDisplayUtility.DIM_Organization.equals(dimension)) {
      if (DimensionDisplayUtility.DIM_Header.equals(level)) {
        return client.isOrgAcctdimHeader() ? "Y" : "N";
      } else if (DimensionDisplayUtility.DIM_Lines.equals(level)) {
        return client.isOrgAcctdimLines() ? "Y" : "N";
      } else {
        return client.isOrgAcctdimBreakdown() ? "Y" : "N";
      }
    } else if (DimensionDisplayUtility.DIM_BPartner.equals(dimension)) {
      if (DimensionDisplayUtility.DIM_Header.equals(level)) {
        return client.isBpartnerAcctdimHeader() ? "Y" : "N";
      } else if (DimensionDisplayUtility.DIM_Lines.equals(level)) {
        return client.isBpartnerAcctdimLines() ? "Y" : "N";
      } else {
        return client.isBpartnerAcctdimBreakdown() ? "Y" : "N";
      }
    } else if (DimensionDisplayUtility.DIM_Product.equals(dimension)) {
      if (DimensionDisplayUtility.DIM_Header.equals(level)) {
        return client.isProductAcctdimHeader() ? "Y" : "N";
      } else if (DimensionDisplayUtility.DIM_Lines.equals(level)) {
        return client.isProductAcctdimLines() ? "Y" : "N";
      } else {
        return client.isProductAcctdimBreakdown() ? "Y" : "N";
      }
    } else if (DimensionDisplayUtility.DIM_Project.equals(dimension)) {
      if (DimensionDisplayUtility.DIM_Header.equals(level)) {
        return client.isProjectAcctdimHeader() ? "Y" : "N";
      } else if (DimensionDisplayUtility.DIM_Lines.equals(level)) {
        return client.isProjectAcctdimLines() ? "Y" : "N";
      } else {
        return client.isProjectAcctdimBreakdown() ? "Y" : "N";
      }
    } else if (DimensionDisplayUtility.DIM_CostCenter.equals(dimension)) {
      if (DimensionDisplayUtility.DIM_Header.equals(level)) {
        return client.isCostcenterAcctdimHeader() ? "Y" : "N";
      } else if (DimensionDisplayUtility.DIM_Lines.equals(level)) {
        return client.isCostcenterAcctdimLines() ? "Y" : "N";
      } else {
        return client.isCostcenterAcctdimBreakdown() ? "Y" : "N";
      }
    } else if (DimensionDisplayUtility.DIM_User1.equals(dimension)) {
      if (DimensionDisplayUtility.DIM_Header.equals(level)) {
        return client.isUser1AcctdimHeader() ? "Y" : "N";
      } else if (DimensionDisplayUtility.DIM_Lines.equals(level)) {
        return client.isUser1AcctdimLines() ? "Y" : "N";
      } else {
        return client.isUser1AcctdimBreakdown() ? "Y" : "N";
      }
    } else if (DimensionDisplayUtility.DIM_User2.equals(dimension)) {
      if (DimensionDisplayUtility.DIM_Header.equals(level)) {
        return client.isUser2AcctdimHeader() ? "Y" : "N";
      } else if (DimensionDisplayUtility.DIM_Lines.equals(level)) {
        return client.isUser2AcctdimLines() ? "Y" : "N";
      } else {
        return client.isUser2AcctdimBreakdown() ? "Y" : "N";
      }
    } else {
      return "N";
    }
  }
}
