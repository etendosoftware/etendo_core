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
 * All portions are Copyright (C) 2014-2016 Openbravo SLU
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


import jakarta.enterprise.context.Dependent;
import jakarta.servlet.ServletException;

import com.etendoerp.sequences.NextSequenceValue;
import com.etendoerp.sequences.UINextSequenceValueInterface;
import org.apache.commons.lang3.StringUtils;
import jakarta.persistence.criteria.CriteriaBuilder;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.businessUtility.BpDocTypeUtils;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;

import java.util.ArrayList;
import java.util.List;

@Dependent
public class SE_InOut_Organization extends SimpleCallout {
    private static final String WAREHOUSEID = "inpmWarehouseId";

    @Override
    protected void execute(CalloutInfo info) throws ServletException {
        String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());
        String strMWarehouseId = info.vars.getStringParameter(WAREHOUSEID);
        String strOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
        String strInOut = info.getStringParameter("M_InOut_ID", IsIDFilter.instance);
        String strBPId = info.getStringParameter("inpcBpartnerId", IsIDFilter.instance);
        FieldProvider[] td = null;

        final boolean isSales = StringUtils.equals("Y", strIsSOTrx);
        if (StringUtils.isNotBlank(strBPId)) {
          BpDocTypeUtils.applyShipmentDocType(info, strOrgId, strBPId, isSales, "inpcDoctypeId", "inpcDoctypeId_R");
        }
        
        /* Warehouse */
        OBCriteria<OrgWarehouse> orgWarehouseCriteria = OBDal.getInstance().createCriteria(OrgWarehouse.class);
        orgWarehouseCriteria.addEqual(OrgWarehouse.PROPERTY_ORGANIZATION, OBDal.getInstance().get(Organization.class, strOrgId));

        List<String> warehouseIds = new ArrayList<>();
        List<OrgWarehouse> warehouseList = orgWarehouseCriteria.list();
        for (OrgWarehouse orgWarehouse : warehouseList) {
            Warehouse warehouse = orgWarehouse.getWarehouse();
            warehouseIds.add(warehouse.getId());
        }

        if (warehouseIds.isEmpty()) {
            info.addResult(WAREHOUSEID, "");
        } else {
            try {
                ComboTableData comboTableData = new ComboTableData(info.vars, this, "18", "M_Warehouse_ID", "197", StringUtils.equals(strIsSOTrx, "Y") ? "C4053C0CD3DC420A9924F24FC1F860A0" : "", Utility.getReferenceableOrg(info.vars, strOrgId), Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
                Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), "");
                td = comboTableData.select(false);

                boolean validWarehouseFound = warehouseIds.contains(strMWarehouseId);

                if (td != null && td.length > 0) {
                    for (int i = 0; i < td.length; i++) {
                        if (StringUtils.equals(td[i].getField("id"), strMWarehouseId) && warehouseIds.contains(strMWarehouseId)) {
                            validWarehouseFound = true;
                            break;
                        }
                    }
                }
                if (validWarehouseFound) {
                    info.addResult(WAREHOUSEID, strMWarehouseId);
                } else {
                    info.addResult(WAREHOUSEID, warehouseIds.get(0));
                }
            } catch (Exception ex) {
                throw new ServletException(ex);
            }
        }

        // Check Document No. again.
        if (StringUtils.isBlank(strInOut)) {
            Field field = Utilities.getField(info);
            if (field != null) {
                UINextSequenceValueInterface sequenceHandler = null;
                sequenceHandler = NextSequenceValue.getInstance()
                        .getSequenceHandler(field.getColumn().getReference().getId());
                if (sequenceHandler != null) {
                    String documentNo = sequenceHandler.generateNextSequenceValue(field,
                            RequestContext.get());
                    info.addResult("inpdocumentno", "<" + documentNo + ">");
                }
            }
        }
    }
}
