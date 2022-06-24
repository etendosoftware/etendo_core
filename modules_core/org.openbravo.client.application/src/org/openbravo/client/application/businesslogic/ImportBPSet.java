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
 * Contributor(s):
 ************************************************************************
 */

package org.openbravo.client.application.businesslogic;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.BusinessPartnerSet;
import org.openbravo.model.common.businesspartner.BusinessPartnerSetLine;
import org.openbravo.service.json.JsonUtils;

public class ImportBPSet extends ProcessUploadedFile {
  private static final long serialVersionUID = 1L;
  private static final DateFormat dateFormat = JsonUtils.createDateFormat();

  @Override
  protected void clearBeforeImport(String ownerId, JSONObject paramValues) {
    @SuppressWarnings("unchecked")
    NativeQuery<String> qry = OBDal.getInstance()
        .getSession()
        .createNativeQuery(
            "update c_bp_set_line set updated=now(), updatedby=:userId, isactive='N' where c_bp_set_id = :c_bp_set_id");
    qry.setParameter("userId", OBContext.getOBContext().getUser().getId());
    qry.setParameter("c_bp_set_id", ownerId);
    qry.executeUpdate();
  }

  @Override
  protected UploadResult doProcessFile(JSONObject paramValues, File file) throws Exception {
    final UploadResult uploadResult = new UploadResult();
    final String bpSetId = paramValues.getString("inpOwnerId");
    final Date startDate = getDate(paramValues.getString("startDate"));
    final Date endDate = getDate(paramValues.getString("endDate"));

    final String errorMsgBPNotFound = OBMessageUtils.getI18NMessage("OBUIAPP_BPNotFound",
        new String[0]);
    final String errorMsgBPNotUnique = OBMessageUtils.getI18NMessage("OBUIAPP_BPNotUnique",
        new String[0]);

    try (BufferedReader br = Files.newBufferedReader(Paths.get(file.getAbsolutePath()))) {
      String line;
      while ((line = br.readLine()) != null) {
        final String bpKey = line.trim();

        // ignore spaces
        if (bpKey.length() == 0) {
          continue;
        }

        uploadResult.incTotalCount();

        final List<String> bpIds = getBusinessPartnerIds(bpKey);
        if (bpIds.size() == 0) {
          uploadResult.incErrorCount();
          uploadResult.addErrorMessage(bpKey + " --> " + errorMsgBPNotFound + "\n");
        } else if (bpIds.size() > 1) {
          uploadResult.incErrorCount();
          uploadResult.addErrorMessage(bpKey + " --> " + errorMsgBPNotUnique + "\n");
        } else {
          // check if the line already exists
          final String bpId = bpIds.get(0);
          final OBQuery<BusinessPartnerSetLine> bpSetLineQry = OBDal.getInstance()
              .createQuery(BusinessPartnerSetLine.class,
                  "c_bp_set_id=:c_bp_set_id and c_bpartner_id=:c_bpartner_id");
          bpSetLineQry.setNamedParameter("c_bp_set_id", bpSetId);
          bpSetLineQry.setNamedParameter("c_bpartner_id", bpId);
          bpSetLineQry.setFilterOnActive(false);
          final List<BusinessPartnerSetLine> lines = bpSetLineQry.list();
          BusinessPartnerSetLine bpSetLine = null;
          if (lines.size() == 0) {
            BusinessPartnerSet bpSet = OBDal.getInstance().get(BusinessPartnerSet.class, bpSetId);
            // create a new one
            bpSetLine = OBProvider.getInstance().get(BusinessPartnerSetLine.class);
            bpSetLine.setClient(bpSet.getClient());
            bpSetLine.setOrganization(bpSet.getOrganization());
            bpSetLine.setBpSet(bpSet);
            bpSetLine.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpId));
          } else {
            // get the line from the result
            bpSetLine = lines.get(0);
          }
          bpSetLine.setActive(true);
          bpSetLine.setStartingDate(startDate);
          bpSetLine.setEndingDate(endDate);
          OBDal.getInstance().save(bpSetLine);
        }
      }
    }
    return uploadResult;
  }

  // note synchronized as the access to parse is not threadsafe
  private synchronized Date getDate(String value) throws Exception {
    if (value == null || value.equals("null") || value.trim().length() == 0) {
      return null;
    }
    return dateFormat.parse(value);
  }

  @SuppressWarnings("unchecked")
  protected List<String> getBusinessPartnerIds(String bpKey) {
    String sql = "SELECT c_bpartner_id from c_bpartner where value=:value";
    Session session = OBDal.getInstance().getSession();
    @SuppressWarnings("rawtypes")
    NativeQuery qry = session.createNativeQuery(sql).setParameter("value", bpKey);
    // only need max 2 to identify that there is more than one
    qry.setMaxResults(2);
    return (List<String>) qry.list();
  }

}
