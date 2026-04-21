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
@SuppressWarnings("java:S112")
@RunWith(MockitoJUnitRunner.Silent.class)
public class AttachmentAHTest {

  private static final String ATTACH_ID = "attachId";
  private static final String TABLE_001 = "TABLE_001";
  private static final String DO_DELETE = "doDelete";
  private static final String REC_001 = "REC_001";
  private static final String ATTACHMENT_ID = "attachmentId";
  private static final String ATT_001 = "ATT_001";
  private static final String ATTACHMENT_METHOD = "attachmentMethod";
  private static final String METHOD_001 = "METHOD_001";
  private static final String INP_KEY = "inpKey";
  private static final String BTN_001 = "BTN_001";
  private static final String BUTTON_ID = "buttonId";
  private static final String PARAMS = "_params";
  private static final String TAB_001 = "TAB_001";
  private static final String DO_EDIT = "doEdit";

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
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

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
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDaoStatic != null) obDaoStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (obDalStatic != null) obDalStatic.close();
  }
  /**
   * Do delete calls aim delete.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoDeleteCallsAimDelete() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ATTACH_ID, "ATTACH_001");

    Tab mockTab = mock(Tab.class);
    Table mockTable = mock(Table.class);
    when(mockTab.getTable()).thenReturn(mockTable);
    when(mockTable.getId()).thenReturn(TABLE_001);

    Attachment mockAttachment = mock(Attachment.class);
    OBCriteria<Attachment> mockCriteria = mock(OBCriteria.class);
    obDaoStatic.when(() -> OBDao.getFilteredCriteria(eq(Attachment.class), any(Criterion.class),
        any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Collections.singletonList(mockAttachment));

    // Act
    Method doDelete = AttachmentAH.class.getDeclaredMethod(DO_DELETE, Map.class, Tab.class,
        String.class);
    doDelete.setAccessible(true);
    doDelete.invoke(instance, parameters, mockTab, REC_001);

    // Assert
    verify(aim).delete(mockAttachment);
  }
  /**
   * Do delete multiple attachments.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoDeleteMultipleAttachments() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ATTACH_ID, null);

    Tab mockTab = mock(Tab.class);
    Table mockTable = mock(Table.class);
    when(mockTab.getTable()).thenReturn(mockTable);
    when(mockTable.getId()).thenReturn(TABLE_001);

    Attachment mockAttachment1 = mock(Attachment.class);
    Attachment mockAttachment2 = mock(Attachment.class);
    OBCriteria<Attachment> mockCriteria = mock(OBCriteria.class);
    obDaoStatic.when(() -> OBDao.getFilteredCriteria(eq(Attachment.class), any(Criterion.class),
        any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Arrays.asList(mockAttachment1, mockAttachment2));

    // Act
    Method doDelete = AttachmentAH.class.getDeclaredMethod(DO_DELETE, Map.class, Tab.class,
        String.class);
    doDelete.setAccessible(true);
    doDelete.invoke(instance, parameters, mockTab, REC_001);

    // Assert
    verify(aim).delete(mockAttachment1);
    verify(aim).delete(mockAttachment2);
  }
  /**
   * Do edit calls aim update.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoEditCallsAimUpdate() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ATTACHMENT_ID, ATT_001);
    parameters.put(ATTACHMENT_METHOD, METHOD_001);

    JSONObject request = new JSONObject();
    JSONObject params = new JSONObject();
    params.put(INP_KEY, REC_001);
    params.put(BUTTON_ID, BTN_001);
    request.put(PARAMS, params);

    String tabId = TAB_001;

    // Setup adcs to return empty list of parameters
    lenient().when(adcs.getMethodMetadataParameters(anyString(), anyString()))
        .thenReturn(Collections.emptyList());

    // Act
    Method doEdit = AttachmentAH.class.getDeclaredMethod(DO_EDIT, Map.class, JSONObject.class,
        JSONObject.class, String.class);
    doEdit.setAccessible(true);
    doEdit.invoke(instance, parameters, request, params, tabId);

    // Assert
    verify(aim).update(any(Map.class), eq(ATT_001), eq(TAB_001));
  }
  /**
   * Do edit with default method when blank.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoEditWithDefaultMethodWhenBlank() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ATTACHMENT_ID, ATT_001);
    parameters.put(ATTACHMENT_METHOD, "");

    JSONObject request = new JSONObject();
    JSONObject params = new JSONObject();
    params.put(INP_KEY, REC_001);
    params.put(BUTTON_ID, BTN_001);
    request.put(PARAMS, params);

    String tabId = TAB_001;

    when(adcs.getMethodMetadataParameters(anyString(), anyString()))
        .thenReturn(Collections.emptyList());

    // Act
    Method doEdit = AttachmentAH.class.getDeclaredMethod(DO_EDIT, Map.class, JSONObject.class,
        JSONObject.class, String.class);
    doEdit.setAccessible(true);
    doEdit.invoke(instance, parameters, request, params, tabId);

    // Assert - should use DEFAULT_METHOD_ID when attachmentMethod is blank
    verify(aim).update(any(Map.class), eq(ATT_001), eq(TAB_001));
  }
  /**
   * Do edit with metadata parameters.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoEditWithMetadataParameters() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ATTACHMENT_ID, ATT_001);
    parameters.put(ATTACHMENT_METHOD, METHOD_001);

    JSONObject request = new JSONObject();
    JSONObject params = new JSONObject();
    params.put(INP_KEY, REC_001);
    params.put(BUTTON_ID, BTN_001);
    params.put("colName", "value1");
    request.put(PARAMS, params);

    String tabId = TAB_001;

    Parameter mockParam = mock(Parameter.class);
    when(mockParam.isFixed()).thenReturn(false);
    when(mockParam.getDBColumnName()).thenReturn("colName");
    when(mockParam.getId()).thenReturn("PARAM_001");

    when(adcs.getMethodMetadataParameters(METHOD_001, TAB_001))
        .thenReturn(Collections.singletonList(mockParam));

    // Act
    Method doEdit = AttachmentAH.class.getDeclaredMethod(DO_EDIT, Map.class, JSONObject.class,
        JSONObject.class, String.class);
    doEdit.setAccessible(true);
    doEdit.invoke(instance, parameters, request, params, tabId);

    // Assert
    verify(aim).update(any(Map.class), eq(ATT_001), eq(TAB_001));
  }
  /**
   * Do delete disables org filter.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDoDeleteDisablesOrgFilter() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ATTACH_ID, null);

    Tab mockTab = mock(Tab.class);
    Table mockTable = mock(Table.class);
    when(mockTab.getTable()).thenReturn(mockTable);
    when(mockTable.getId()).thenReturn(TABLE_001);

    OBCriteria<Attachment> mockCriteria = mock(OBCriteria.class);
    obDaoStatic.when(() -> OBDao.getFilteredCriteria(eq(Attachment.class), any(Criterion.class),
        any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Collections.emptyList());

    // Act
    Method doDelete = AttachmentAH.class.getDeclaredMethod(DO_DELETE, Map.class, Tab.class,
        String.class);
    doDelete.setAccessible(true);
    doDelete.invoke(instance, parameters, mockTab, REC_001);

    // Assert - verifies org filter is disabled
    verify(mockCriteria).setFilterOnReadableOrganization(false);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws IllegalAccessException, NoSuchFieldException {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
