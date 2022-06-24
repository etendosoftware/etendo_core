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
 * All portions are Copyright (C) 2015-2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.datasource;

/**
 * Test cases for NotesDataSource
 * 
 * @author NaroaIriarte
 */

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.client.application.Note;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.json.JsonConstants;

public class TestNoteDatasource extends BaseDataSourceTestDal {
  private static final String CONTEXT_USER = "100";
  private static final String CONTEXT_ROLE = "45C861D70874409D86AE1CC7007AB43A";
  private static final String CLIENT = "23C59575B9CF467C9620760EB255B389";
  private static final String AMERICAN_ORGANIZATION = "BAE22373FEBE4CCCA24517E23F0C8A48";
  private static final String SPANISH_ORGANIZATION = "DC206C91AA6A4897B44DA897936E0EC3";
  private static final String DATASOURCE_ID = "090A37D22E61FE94012E621729090048";
  private static final String TABLE_ID = "259";
  private static final String RECORD_ID = "3EFF470687024F099FB40438AAB20BED";
  private static final String LANGUAGE_ID = "192";
  private static final String WAREHOUSE_ID = "4D45FE4C515041709047F51D139A21AC";

  /**
   * Required in order to have the CSRF token available
   */
  @Before
  public void authenticateUser() throws Exception {
    authenticate();
  }

  /**
   * Test to fetch values from NoteDataSource. At first a note is added by a user. After that the
   * organization of the note is changed and this test tests if it is possible to do the fetch and
   * delete the note.
   */
  @Test
  public void testFetchNotes() throws Exception {
    OBContext.setOBContext(CONTEXT_USER, CONTEXT_ROLE, CLIENT, AMERICAN_ORGANIZATION);
    OBContext.getOBContext().getRole();
    OBContext.setAdminMode(false);
    try {
      changeProfile(CONTEXT_ROLE, LANGUAGE_ID, AMERICAN_ORGANIZATION, WAREHOUSE_ID);
      String response = "";
      String noteId = "";
      String responseAdd = "";
      String responseRemove = "";
      JSONObject noteDataMid = new JSONObject();
      JSONArray noteData = new JSONArray();
      JSONObject noteResponseMid = new JSONObject();
      JSONArray noteResponseFetch = new JSONArray();

      // A request for adding a note is sent
      responseAdd = addANote();
      JSONObject jsonResponse = new JSONObject(responseAdd);
      noteDataMid = jsonResponse.getJSONObject("response");
      noteData = noteDataMid.getJSONArray("data");
      noteId = noteData.getJSONObject(0).getString("id");
      Note note = OBDal.getInstance().get(Note.class, noteId);
      JSONObject jsonResponseAdd = new JSONObject(responseAdd);
      assertThat(getStatus(jsonResponseAdd),
          is(String.valueOf(JsonConstants.RPCREQUEST_STATUS_SUCCESS)));
      assertThat(note, is(notNullValue()));

      // change the note's organization, the current user has no access to
      // the organization, but should have access to the note
      Organization org = OBDal.getInstance().get(Organization.class, SPANISH_ORGANIZATION);
      note.setOrganization(org);
      OBDal.getInstance().commitAndClose();

      // A request for doing the fetch of the notes is sent
      response = fetchNote();
      JSONObject jsonResponseFetch = new JSONObject(response);
      assertThat(getStatus(jsonResponseFetch),
          is(String.valueOf(JsonConstants.RPCREQUEST_STATUS_SUCCESS)));
      noteResponseMid = jsonResponseFetch.getJSONObject("response");
      noteResponseFetch = noteResponseMid.getJSONArray("data");
      assertThat(noteResponseFetch.length(), is(not(0)));
      assertThat(noteResponseFetch.toString(), containsString(noteId));
      OBDal.getInstance().commitAndClose();

      // A request for removing the note is sent
      responseRemove = removeNote(noteId);
      JSONObject jsonResponseRemove = new JSONObject(responseRemove);
      assertThat(getStatus(jsonResponseRemove),
          is(String.valueOf(JsonConstants.RPCREQUEST_STATUS_SUCCESS)));
      Note deleteNote = OBDal.getInstance().get(Note.class, noteId);
      assertThat(deleteNote, is(nullValue()));
    } finally {
      OBDal.getInstance().commitAndClose();
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This is a method for adding a note and it returns the response of the request.
   */
  private String addANote() throws Exception {
    JSONObject dataObject = new JSONObject();
    dataObject.put("client", CLIENT);
    dataObject.put("organization", AMERICAN_ORGANIZATION);
    dataObject.put("table", TABLE_ID);
    dataObject.put("record", RECORD_ID);
    dataObject.put("note", "Test");
    JSONObject contentJson = new JSONObject();
    contentJson.put("operationType", "add");
    contentJson.put("data", dataObject);
    contentJson.put("csrfToken", getSessionCsrfToken());
    String responseAdd = doRequest("/org.openbravo.service.datasource/" + DATASOURCE_ID,
        contentJson.toString(), 200, "POST", "application/json");
    return responseAdd;
  }

  /**
   * This is a method which does a fetch of the notes. It returns the response of making the fetch.
   */
  private String fetchNote() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    String entityName = "OBUIAPP_Note";
    String criteria = "{\"fieldName\":\"table\",\"operator\":\"equals\",\"value\":\"" + TABLE_ID
        + "\"}__;__{\"fieldName\":\"record\",\"operator\":\"equals\",\"value\":\"" + RECORD_ID
        + "\"}";
    params.put("criteria", criteria);
    params.put("_entityName", entityName);
    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "75");
    String response = doRequest("/org.openbravo.service.datasource/" + DATASOURCE_ID, params, 200,
        "POST");
    return response;
  }

  /**
   * This is a method which removes a note and it returns the response of the request.
   */
  private String removeNote(String noteId) throws Exception {
    Map<String, String> paramsRemove = new HashMap<String, String>();
    String responseRemove = doRequest("/org.openbravo.service.datasource/" + DATASOURCE_ID
        + "?_operationType=remove&id=" + noteId + "&csrfToken=" + getSessionCsrfToken(),
        paramsRemove, 200, "DELETE");
    return responseRemove;
  }

  private String getStatus(JSONObject jsonResponse) throws JSONException {
    return jsonResponse.getJSONObject("response").get("status").toString();
  }

}
