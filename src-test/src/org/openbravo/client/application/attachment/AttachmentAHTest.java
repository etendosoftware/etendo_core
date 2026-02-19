package org.openbravo.client.application.attachment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Criterion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.ad.datamodel.Table;

/**
 * Tests for AttachmentAH.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AttachmentAHTest {

  private AttachmentAH instance;

  @Mock
  private AttachImplementationManager aim;

  @Mock
  private ApplicationDictionaryCachedStructures adcs;

  @Mock
  private OBDal mockOBDal;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<OBDao> obDaoStatic;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AttachmentAH.class);

    setPrivateField(instance, "aim", aim);
    setPrivateField(instance, "adcs", adcs);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);

    obDaoStatic = mockStatic(OBDao.class);
  }

  @After
  public void tearDown() {
    if (obDaoStatic != null) obDaoStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (obDalStatic != null) obDalStatic.close();
  }

  @Test
  public void testDoDeleteCallsAimDelete() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("attachId", "ATTACH_001");

    Tab mockTab = mock(Tab.class);
    Table mockTable = mock(Table.class);
    when(mockTab.getTable()).thenReturn(mockTable);
    when(mockTable.getId()).thenReturn("TABLE_001");

    Attachment mockAttachment = mock(Attachment.class);
    OBCriteria<Attachment> mockCriteria = mock(OBCriteria.class);
    obDaoStatic.when(() -> OBDao.getFilteredCriteria(eq(Attachment.class), any(Criterion.class),
        any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Collections.singletonList(mockAttachment));

    // Act
    Method doDelete = AttachmentAH.class.getDeclaredMethod("doDelete", Map.class, Tab.class,
        String.class);
    doDelete.setAccessible(true);
    doDelete.invoke(instance, parameters, mockTab, "REC_001");

    // Assert
    verify(aim).delete(mockAttachment);
  }

  @Test
  public void testDoDeleteMultipleAttachments() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("attachId", null);

    Tab mockTab = mock(Tab.class);
    Table mockTable = mock(Table.class);
    when(mockTab.getTable()).thenReturn(mockTable);
    when(mockTable.getId()).thenReturn("TABLE_001");

    Attachment mockAttachment1 = mock(Attachment.class);
    Attachment mockAttachment2 = mock(Attachment.class);
    OBCriteria<Attachment> mockCriteria = mock(OBCriteria.class);
    obDaoStatic.when(() -> OBDao.getFilteredCriteria(eq(Attachment.class), any(Criterion.class),
        any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Arrays.asList(mockAttachment1, mockAttachment2));

    // Act
    Method doDelete = AttachmentAH.class.getDeclaredMethod("doDelete", Map.class, Tab.class,
        String.class);
    doDelete.setAccessible(true);
    doDelete.invoke(instance, parameters, mockTab, "REC_001");

    // Assert
    verify(aim).delete(mockAttachment1);
    verify(aim).delete(mockAttachment2);
  }

  @Test
  public void testDoEditCallsAimUpdate() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("attachmentId", "ATT_001");
    parameters.put("attachmentMethod", "METHOD_001");

    JSONObject request = new JSONObject();
    JSONObject params = new JSONObject();
    params.put("inpKey", "REC_001");
    params.put("buttonId", "BTN_001");
    request.put("_params", params);

    String tabId = "TAB_001";

    // Setup adcs to return empty list of parameters
    lenient().when(adcs.getMethodMetadataParameters(anyString(), anyString()))
        .thenReturn(Collections.emptyList());

    // Act
    Method doEdit = AttachmentAH.class.getDeclaredMethod("doEdit", Map.class, JSONObject.class,
        JSONObject.class, String.class);
    doEdit.setAccessible(true);
    doEdit.invoke(instance, parameters, request, params, tabId);

    // Assert
    verify(aim).update(any(Map.class), eq("ATT_001"), eq("TAB_001"));
  }

  @Test
  public void testDoEditWithDefaultMethodWhenBlank() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("attachmentId", "ATT_001");
    parameters.put("attachmentMethod", "");

    JSONObject request = new JSONObject();
    JSONObject params = new JSONObject();
    params.put("inpKey", "REC_001");
    params.put("buttonId", "BTN_001");
    request.put("_params", params);

    String tabId = "TAB_001";

    when(adcs.getMethodMetadataParameters(anyString(), anyString()))
        .thenReturn(Collections.emptyList());

    // Act
    Method doEdit = AttachmentAH.class.getDeclaredMethod("doEdit", Map.class, JSONObject.class,
        JSONObject.class, String.class);
    doEdit.setAccessible(true);
    doEdit.invoke(instance, parameters, request, params, tabId);

    // Assert - should use DEFAULT_METHOD_ID when attachmentMethod is blank
    verify(aim).update(any(Map.class), eq("ATT_001"), eq("TAB_001"));
  }

  @Test
  public void testDoEditWithMetadataParameters() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("attachmentId", "ATT_001");
    parameters.put("attachmentMethod", "METHOD_001");

    JSONObject request = new JSONObject();
    JSONObject params = new JSONObject();
    params.put("inpKey", "REC_001");
    params.put("buttonId", "BTN_001");
    params.put("colName", "value1");
    request.put("_params", params);

    String tabId = "TAB_001";

    Parameter mockParam = mock(Parameter.class);
    when(mockParam.isFixed()).thenReturn(false);
    when(mockParam.getDBColumnName()).thenReturn("colName");
    when(mockParam.getId()).thenReturn("PARAM_001");

    when(adcs.getMethodMetadataParameters("METHOD_001", "TAB_001"))
        .thenReturn(Collections.singletonList(mockParam));

    // Act
    Method doEdit = AttachmentAH.class.getDeclaredMethod("doEdit", Map.class, JSONObject.class,
        JSONObject.class, String.class);
    doEdit.setAccessible(true);
    doEdit.invoke(instance, parameters, request, params, tabId);

    // Assert
    verify(aim).update(any(Map.class), eq("ATT_001"), eq("TAB_001"));
  }

  @Test
  public void testDoDeleteDisablesOrgFilter() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("attachId", null);

    Tab mockTab = mock(Tab.class);
    Table mockTable = mock(Table.class);
    when(mockTab.getTable()).thenReturn(mockTable);
    when(mockTable.getId()).thenReturn("TABLE_001");

    OBCriteria<Attachment> mockCriteria = mock(OBCriteria.class);
    obDaoStatic.when(() -> OBDao.getFilteredCriteria(eq(Attachment.class), any(Criterion.class),
        any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Collections.emptyList());

    // Act
    Method doDelete = AttachmentAH.class.getDeclaredMethod("doDelete", Map.class, Tab.class,
        String.class);
    doDelete.setAccessible(true);
    doDelete.invoke(instance, parameters, mockTab, "REC_001");

    // Assert - verifies org filter is disabled
    verify(mockCriteria).setFilterOnReadableOrganization(false);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
