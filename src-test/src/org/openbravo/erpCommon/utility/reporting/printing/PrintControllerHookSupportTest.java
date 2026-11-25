/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.common.hooks.PrintControllerHookManager;
import org.openbravo.erpCommon.utility.reporting.DocumentType;

/**
 * Tests for {@link PrintControllerHookSupport} JSON-parameter building methods.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerHookSupportTest {

  private static final String DOCUMENT_ID_KEY = "documentId";
  private static final String DOCUMENT_TYPE_KEY = "documentType";
  private static final String REPORT_INPUT_STREAM_KEY = "reportInputStream";
  private static final String REPORT_OUTPUT_STREAM_KEY = "reportOutputStream";
  private static final String TEST_DOC_ID = "DOC-001";

  /**
   * Pre-hook params populate documentId and documentType in the JSONObject.
   * @throws JSONException if a JSON assertion fails
   */
  @Test
  public void testSetPreHookParams_populatesDocumentIdAndType() throws JSONException {
    JSONObject params = new JSONObject();

    PrintControllerHookSupport.setPreHookParams(DocumentType.SALESINVOICE, params, TEST_DOC_ID);

    assertEquals(TEST_DOC_ID, params.getString(DOCUMENT_ID_KEY));
    assertEquals(DocumentType.SALESINVOICE, params.get(DOCUMENT_TYPE_KEY));
  }

  /**
   * Pre-hook params use the provided document type enum value.
   * @throws JSONException if a JSON assertion fails
   */
  @Test
  public void testSetPreHookParams_preservesDocumentType() throws JSONException {
    JSONObject params = new JSONObject();

    PrintControllerHookSupport.setPreHookParams(DocumentType.SALESORDER, params, TEST_DOC_ID);

    assertEquals(DocumentType.SALESORDER, params.get(DOCUMENT_TYPE_KEY));
  }

  /**
   * Post-hook params populate all four fields: documentId, documentType, inputStream,
   * outputStream.
   * @throws PrintControllerHookManager.PrintControllerHookException if the JSON put fails
   * @throws JSONException if a JSON assertion fails
   */
  @Test
  public void testSetPostHookParams_populatesAllFields()
      throws PrintControllerHookManager.PrintControllerHookException, JSONException {
    JSONObject params = new JSONObject();
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
    OutputStream outputStream = new ByteArrayOutputStream();

    PrintControllerHookSupport.setPostHookParams(
        DocumentType.SHIPMENT, params, TEST_DOC_ID, inputStream, outputStream);

    assertEquals(TEST_DOC_ID, params.getString(DOCUMENT_ID_KEY));
    assertEquals(DocumentType.SHIPMENT, params.get(DOCUMENT_TYPE_KEY));
    assertSame(inputStream, params.get(REPORT_INPUT_STREAM_KEY));
    assertSame(outputStream, params.get(REPORT_OUTPUT_STREAM_KEY));
  }

  /**
   * Post-hook params accept null streams without throwing.
   * @throws PrintControllerHookManager.PrintControllerHookException if the JSON put fails
   * @throws JSONException if a JSON assertion fails
   */
  @Test
  public void testSetPostHookParams_nullStreams_doesNotThrow()
      throws PrintControllerHookManager.PrintControllerHookException, JSONException {
    JSONObject params = new JSONObject();

    PrintControllerHookSupport.setPostHookParams(
        DocumentType.PAYMENT, params, TEST_DOC_ID, null, null);

    assertEquals(TEST_DOC_ID, params.getString(DOCUMENT_ID_KEY));
    assertEquals(DocumentType.PAYMENT, params.get(DOCUMENT_TYPE_KEY));
  }
}
