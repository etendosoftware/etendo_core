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

import javax.servlet.ServletException;

import com.etendoerp.sequences.NextSequenceValue;
import com.etendoerp.sequences.UINextSequenceValueInterface;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;

import java.util.ArrayList;
import java.util.List;

public class SE_InOut_Organization extends SimpleCallout {
    private static final String WAREHOUSEID = "inpmWarehouseId";

    @Override
    protected void execute(CalloutInfo info) throws ServletException {
        String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());
        String strMWarehouseId = info.vars.getStringParameter(WAREHOUSEID);
        String strOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
        String strInOut = info.getStringParameter("M_InOut_ID", IsIDFilter.instance);
        boolean updateWarehouse = true;
        FieldProvider[] td = null;

        /* Warehouse */
        OBCriteria<OrgWarehouse> orgWarehouseCriteria = OBDal.getInstance().createCriteria(OrgWarehouse.class);
        orgWarehouseCriteria.add(Restrictions.eq(OrgWarehouse.PROPERTY_ORGANIZATION, OBDal.getInstance().get(Organization.class, strOrgId)));
        orgWarehouseCriteria.setProjection(Projections.property(OrgWarehouse.PROPERTY_WAREHOUSE));

        List<String> warehouseIds = new ArrayList<>();
        for (Object obj : orgWarehouseCriteria.list()) {
            Warehouse warehouse = (Warehouse) obj;
            warehouseIds.add(warehouse.getId());
        }

        if (warehouseIds.isEmpty()) {
            info.addResult(WAREHOUSEID, "");
        } else {
            try {
                ComboTableData comboTableData = new ComboTableData(info.vars, this, "18", "M_Warehouse_ID", "197", strIsSOTrx.equals("Y") ? "C4053C0CD3DC420A9924F24FC1F860A0" : "", Utility.getReferenceableOrg(info.vars, strOrgId), Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
                Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), "");
                td = comboTableData.select(false);

                if (td != null && td.length > 0) {
                    for (int i = 0; i < td.length; i++) {
                        if (td[i].getField("id").equals(strMWarehouseId) && warehouseIds.contains(strMWarehouseId)) {
                            updateWarehouse = false;
                            break;
                        }
                    }
                    if (updateWarehouse) {
                        info.addResult(WAREHOUSEID, td[0].getField("id"));
                    }
                } else {
                    info.addResult(WAREHOUSEID, "");
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
