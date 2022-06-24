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
 * All portions are Copyright (C) 2015-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.datasource;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.client.application.Note;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.SecurityChecker;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.service.json.JsonUtils;

/**
 * A data source for the notes which manages the security. Checks if a user has access to the record
 * of the note.
 * 
 * @author NaroaIriarte
 * 
 */
public class NoteDataSource extends DefaultDataSourceService {
  private static final Logger log = LogManager.getLogger();

  @Override
  public String fetch(Map<String, String> parameters) {
    String noteFetch = "";
    noteFetch = super.fetch(parameters, false);
    return noteFetch;
  }

  @Override
  public String add(Map<String, String> parameters, String content) {
    String noteAdd = "";
    noteAdd = super.add(parameters, content, false);
    return noteAdd;
  }

  @Override
  public String remove(Map<String, String> parameters) {
    String noteRemove = "";
    noteRemove = super.remove(parameters, false);
    return noteRemove;
  }

  @Override
  public void checkEditDatasourceAccess(Map<String, String> parameter) {
    String operationType = parameter.get(DataSourceConstants.OPERATION_TYPE_PARAM);
    if (StringUtils.isNotBlank(operationType)
        && DataSourceConstants.REMOVE_OPERATION.equals(operationType)) {
      // Removing a Note: Remove operation type
      OBContext.setAdminMode(false);
      try {
        String noteId = parameter.get("id");
        Note note = OBDal.getInstance().get(Note.class, noteId);
        Table table = note.getTable();
        String tableId = table.getId();
        String recordId = note.getRecord();
        readableAccesForUser(tableId, recordId);
      } catch (Exception ex) {
        log.error("Exception while trying to remove a note", ex);
        throw new OBException(ex);
      } finally {
        OBContext.restorePreviousMode();
      }
    } else {
      // Adding a Note: Add operation type
      try {
        String content = parameter.get(DataSourceConstants.ADD_CONTENT_OPERATION);
        final JSONObject jsonObject = new JSONObject(content);
        JSONObject noteData = jsonObject.getJSONObject("data");
        String tableId = noteData.getString("table");
        String recordId = noteData.getString("record");
        readableAccesForUser(tableId, recordId);
      } catch (JSONException ex) {
        log.error("Exception while trying to add a new note", ex);
        throw new OBException(ex);
      }
    }
  }

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameter) {
    try {
      // Fetching Notes: Fetch operation type
      JSONObject jsonCriteria = JsonUtils.buildCriteria(parameter);
      JSONArray notesCriteria;
      String tableId;
      String recordId;
      notesCriteria = jsonCriteria.getJSONArray("criteria");
      tableId = notesCriteria.getJSONObject(0).getString("value");
      recordId = notesCriteria.getJSONObject(1).getString("value");
      readableAccesForUser(tableId, recordId);
    } catch (JSONException ex) {
      log.error("Exception while trying to perform a fetch", ex);
      throw new OBException(ex);
    }
  }

  /**
   * Checks if the user has readable access to the record where the note is
   */
  private void readableAccesForUser(String tableId, String recordId) {
    Entity entity = ModelProvider.getInstance().getEntityByTableId(tableId);
    if (entity != null) {
      Object object = OBDal.getInstance().get(entity.getMappingClass(), recordId);
      if (object instanceof OrganizationEnabled) {
        SecurityChecker.getInstance().checkReadableAccess((OrganizationEnabled) object);
      }
    }
  }
}
