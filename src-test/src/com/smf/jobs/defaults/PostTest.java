package com.smf.jobs.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.Entity;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonUtility;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

@RunWith(MockitoJUnitRunner.class)
public class PostTest {

  @Spy
  private Post post;

  @Mock
  private OBDal obDal;

  @Mock
  private RequestContext requestContext;
  @Mock
  private VariablesSecureApp vars;
  @Mock
  private BaseOBObject mockRecord;
  @Mock
  private Organization organization;
  @Mock
  private Client client;
  @Mock
  private Entity entity;

  @Before
  public void setUp() {

    when(organization.getId()).thenReturn("testOrgId");
    when(entity.getTableId()).thenReturn("318");

    when(mockRecord.getId()).thenReturn("testId");
    when(mockRecord.getEntity()).thenReturn(entity);
    when(mockRecord.get("organization")).thenReturn(organization);
    when(mockRecord.get("client")).thenReturn(client);

  }

  @Test
  public void testActionWithMultipleSuccessfulPostings() throws Exception {
    JSONObject parameters = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);

    BaseOBObject mockRecord2 = mock(BaseOBObject.class);
    when(mockRecord2.getId()).thenReturn("testId2");
    when(mockRecord2.get("posted")).thenReturn("N");
    when(mockRecord2.getEntity()).thenReturn(entity);
    when(mockRecord2.get("organization")).thenReturn(organization);
    when(mockRecord2.get("client")).thenReturn(client);

    List<BaseOBObject> records = Arrays.asList(mockRecord, mockRecord2);

    OBError successResult = new OBError();
    successResult.setType("Success");
    successResult.setMessage("Posted successfully");

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class);
         MockedStatic<ActionButtonUtility> actionButtonUtilityMock = mockStatic(ActionButtonUtility.class);
         MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<OBMessageUtils> messageMock = mockStatic(OBMessageUtils.class)) {

      requestContextMock.when(RequestContext::get).thenReturn(requestContext);
      when(requestContext.getVariablesSecureApp()).thenReturn(vars);
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);

      actionButtonUtilityMock.when(() -> ActionButtonUtility.processButton(
          any(VariablesSecureApp.class),
          anyString(),
          anyString(),
          anyString(),
          any(DalConnectionProvider.class)
      )).thenReturn(successResult);

      messageMock.when(() -> OBMessageUtils.messageBD(anyString())).thenReturn("DJOBS_PostUnpostMessage");
      doReturn(records).when(post).getInputContents(any());

      ActionResult result = post.action(parameters, isStopped);

      assertNotNull("Result should not be null", result);
      assertEquals("Should return success type", Result.Type.SUCCESS, result.getType());
      verify(mockRecord, times(1)).getId();
      verify(mockRecord2, times(1)).getId();
    }
  }

  @Test
  public void testActionWithMixedResults() throws Exception {
    JSONObject parameters = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);

    BaseOBObject mockRecord2 = mock(BaseOBObject.class);
    when(mockRecord2.getId()).thenReturn("testId2");
    when(mockRecord2.get("posted")).thenReturn("N");
    when(mockRecord2.getEntity()).thenReturn(entity);
    when(mockRecord2.get("organization")).thenReturn(organization);
    when(mockRecord2.get("client")).thenReturn(client);

    List<BaseOBObject> records = Arrays.asList(mockRecord, mockRecord2);

    OBError successResult = new OBError();
    successResult.setType("Success");
    OBError errorResult = new OBError();
    errorResult.setType("Error");

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class);
         MockedStatic<ActionButtonUtility> actionButtonUtilityMock = mockStatic(ActionButtonUtility.class);
         MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<OBMessageUtils> messageMock = mockStatic(OBMessageUtils.class)) {

      requestContextMock.when(RequestContext::get).thenReturn(requestContext);
      when(requestContext.getVariablesSecureApp()).thenReturn(vars);
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);

      actionButtonUtilityMock.when(() -> ActionButtonUtility.processButton(
          any(VariablesSecureApp.class),
          eq("testId"),
          anyString(),
          anyString(),
          any(DalConnectionProvider.class)
      )).thenReturn(successResult);

      actionButtonUtilityMock.when(() -> ActionButtonUtility.processButton(
          any(VariablesSecureApp.class),
          eq("testId2"),
          anyString(),
          anyString(),
          any(DalConnectionProvider.class)
      )).thenReturn(errorResult);

      messageMock.when(() -> OBMessageUtils.messageBD(anyString())).thenReturn("DJOBS_PostUnpostMessage");
      doReturn(records).when(post).getInputContents(any());

      ActionResult result = post.action(parameters, isStopped);

      assertNotNull("Result should not be null", result);
      assertEquals("Should return warning type for mixed results", Result.Type.WARNING, result.getType());
    }
  }

  @Test
  public void testActionWithEmptyRecordList() throws Exception {
    JSONObject parameters = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);
    List<BaseOBObject> records = Collections.emptyList();

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class);
         MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {

      requestContextMock.when(RequestContext::get).thenReturn(requestContext);
      when(requestContext.getVariablesSecureApp()).thenReturn(vars);
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);

      doReturn(records).when(post).getInputContents(any());

      ActionResult result = post.action(parameters, isStopped);

      assertNotNull("Result should not be null", result);
      assertEquals("Should return success type for empty list", Result.Type.SUCCESS, result.getType());
    }
  }


  @Test
  public void testActionWithException() throws Exception {
    JSONObject parameters = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);
    List<BaseOBObject> records = Collections.singletonList(mockRecord);

    try (MockedStatic<RequestContext> requestContextMock = mockStatic(RequestContext.class);
         MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {

      requestContextMock.when(RequestContext::get).thenReturn(requestContext);
      when(requestContext.getVariablesSecureApp()).thenReturn(vars);
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);

      doReturn(records).when(post).getInputContents(any());
      doThrow(new RuntimeException("Test exception")).when(mockRecord).get("posted");

      ActionResult result = post.action(parameters, isStopped);

      assertNotNull("Result should not be null", result);
      assertEquals("Should return error type", Result.Type.ERROR, result.getType());
      assertEquals("Should return exception message", "Test exception", result.getMessage());
    }
  }
}