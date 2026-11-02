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
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;

/**
 * Unit tests for {@link AcctServer#run}: how a batch reports the documents it posted, the ones that
 * failed to post and the one that ended it early.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AcctServerRunTest {

  private static final String CLIENT_ID = "1000000";

  private AcctServer instance;

  /**
   * Creates a concrete AcctServer without running its constructor.
   */
  @Before
  public void setUp() {
    instance = new ObjenesisStd().newInstance(DocGLJournal.class);
  }

  /**
   * Tests that run() records a document that failed to post even though post() returned true:
   * the per-document error counter is what marks the failure. AcctServerProcess relies on it to
   * list failed documents and to detect batches that make no progress.
   * @throws Exception if an error occurs
   */
  @Test
  public void testRunRecordsDocumentsThatFailToPost() throws Exception {
    AcctServer failing = mock(AcctServer.class);
    failing.errors = 1;
    failing.DocumentNo = "GL-1";
    when(failing.getStatus()).thenReturn("E");
    when(failing.post(eq("DOC1"), eq(false), any(), any(), any())).thenReturn(true);
    AcctServer posting = mock(AcctServer.class);
    posting.success = 1;
    when(posting.post(eq("DOC2"), eq(false), any(), any(), any())).thenReturn(true);

    ConnectionProvider cp = runBatch(failing, posting);

    assertEquals(1, instance.getSuccess());
    assertEquals(1, instance.errors);
    Map<String, String[]> failed = instance.getFailedDocs();
    assertEquals(Collections.singleton("DOC1"), failed.keySet());
    assertArrayEquals(new String[] { "GL-1", "E" }, failed.get("DOC1"));
    assertNull(instance.getBlockedDoc());
    verify(cp, times(2)).releaseCommitConnection(any());
  }

  /**
   * Tests that a document post() could not handle (e.g. locked by a concurrent posting) ends the
   * batch and is reported as the blocking document.
   * @throws Exception if an error occurs
   */
  @Test
  public void testRunReportsTheDocumentThatEndedTheBatch() throws Exception {
    AcctServer locked = mock(AcctServer.class);
    locked.DocumentNo = "GL-1";
    when(locked.getStatus()).thenReturn(AcctServer.STATUS_DocumentLocked);
    when(locked.post(eq("DOC1"), eq(false), any(), any(), any())).thenReturn(false);

    ConnectionProvider cp = runBatch(locked, mock(AcctServer.class));

    assertArrayEquals(new String[] { "DOC1", "GL-1", "L", null }, instance.getBlockedDoc());
    assertEquals(0, instance.getSuccess());
    verify(cp).releaseRollbackConnection(any());
  }

  /**
   * Tests that an unexpected error while posting a document, which run() only logs, is reported
   * with that document as the blocking one.
   * @throws Exception if an error occurs
   */
  @Test
  public void testRunReportsTheDocumentThatFailedWithAnError() throws Exception {
    AcctServer broken = mock(AcctServer.class);
    when(broken.post(eq("DOC1"), eq(false), any(), any(), any()))
        .thenThrow(new IllegalStateException("boom"));

    runBatch(broken, mock(AcctServer.class));

    assertArrayEquals(new String[] { "DOC1", "", null, "boom" }, instance.getBlockedDoc());
  }

  /**
   * Runs one batch of documents DOC1 and DOC2, posted by the given servers in that order.
   */
  private ConnectionProvider runBatch(AcctServer first, AcctServer second) throws Exception {
    Field failedDocsField = AcctServer.class.getDeclaredField("failedDocs");
    failedDocsField.setAccessible(true);
    failedDocsField.set(instance, new LinkedHashMap<String, String[]>());
    ConnectionProvider cp = mock(ConnectionProvider.class);
    when(cp.getRDBMS()).thenReturn("POSTGRE");
    when(cp.getTransactionConnection()).thenReturn(mock(Connection.class));
    instance.connectionProvider = cp;
    instance.AD_Client_ID = CLIENT_ID;
    instance.AD_Org_ID = "0";
    instance.AD_Table_ID = "224";
    instance.tableName = "GL_Journal";
    instance.strDateColumn = "DateAcct";
    AcctServerData doc1 = new ObjenesisStd().newInstance(AcctServerData.class);
    doc1.id = "DOC1";
    AcctServerData doc2 = new ObjenesisStd().newInstance(AcctServerData.class);
    doc2.id = "DOC2";

    try (MockedStatic<OBContext> obContext = mockStatic(OBContext.class);
        MockedStatic<OBDal> obDal = mockStatic(OBDal.class);
        MockedStatic<AcctServerData> data = mockStatic(AcctServerData.class);
        MockedStatic<AcctServer> acctServer = mockStatic(AcctServer.class)) {
      OBContext context = mock(OBContext.class);
      OrganizationStructureProvider osp = mock(OrganizationStructureProvider.class);
      obContext.when(OBContext::getOBContext).thenReturn(context);
      when(context.getOrganizationStructureProvider(CLIENT_ID)).thenReturn(osp);
      when(osp.getChildTree("0", true)).thenReturn(Collections.singleton("0"));
      obDal.when(OBDal::getInstance).thenReturn(mock(OBDal.class));
      data.when(() -> AcctServerData.select(eq(cp), eq("GL_Journal"), eq("DateAcct"),
          eq(CLIENT_ID), anyString(), any(), any(), anyString()))
          .thenReturn(new AcctServerData[] { doc1, doc2 });
      acctServer.when(() -> AcctServer.get("224", CLIENT_ID, "0", cp)).thenReturn(first, second);

      instance.run(mock(VariablesSecureApp.class), "", "");
    }
    return cp;
  }
}
