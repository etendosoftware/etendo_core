package org.openbravo.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Language;

import java.lang.reflect.Method;

/**
 * Unit tests for {@link ADTableEventHandler}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ADTableEventHandlerTest {

  private static final String TEST_TABLE_ID = "TEST_TABLE_001";
  private static final String TEST_JAVA_CLASS = "com.example.TestEntity";
  private static final String TEST_LANGUAGE = "en_US";

  private ADTableEventHandler handler;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private ModelProvider mockModelProvider;
  @Mock
  private Entity mockTableEntity;
  @Mock
  private Property mockJavaClassNameProperty;
  @Mock
  private Property mockPackageNameProperty;
  @Mock
  private Property mockDataOriginTypeProperty;
  @Mock
  private EntityNewEvent mockNewEvent;
  @Mock
  private EntityUpdateEvent mockUpdateEvent;
  @Mock
  private OBCriteria<Table> mockCriteria;
  @Mock
  private Language mockLanguage;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<ModelProvider> modelProviderStatic;
  private MockedStatic<Utility> utilityStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    handler = objenesis.newInstance(ADTableEventHandler.class);

    obDalStatic = mockStatic(OBDal.class);
    obContextStatic = mockStatic(OBContext.class);
    modelProviderStatic = mockStatic(ModelProvider.class);
    utilityStatic = mockStatic(Utility.class);

    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);
    modelProviderStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);

    lenient().when(mockModelProvider.getEntity(Table.ENTITY_NAME)).thenReturn(mockTableEntity);
    lenient().when(mockTableEntity.getProperty(Table.PROPERTY_JAVACLASSNAME))
        .thenReturn(mockJavaClassNameProperty);
    lenient().when(mockTableEntity.getProperty(Table.PROPERTY_DATAPACKAGE))
        .thenReturn(mockPackageNameProperty);
    lenient().when(mockTableEntity.getProperty(Table.PROPERTY_DATAORIGINTYPE))
        .thenReturn(mockDataOriginTypeProperty);

    lenient().when(mockOBContext.getLanguage()).thenReturn(mockLanguage);
    lenient().when(mockLanguage.getLanguage()).thenReturn(TEST_LANGUAGE);
  }

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (modelProviderStatic != null) modelProviderStatic.close();
    if (utilityStatic != null) utilityStatic.close();
  }

  @Test
  public void testCheckClassNameNoDuplicateDoesNotThrow() throws Exception {
    when(mockNewEvent.getTargetInstance()).thenReturn(mock(Table.class));
    when(mockNewEvent.getTargetInstance().getId()).thenReturn(TEST_TABLE_ID);
    when(mockNewEvent.getCurrentState(mockJavaClassNameProperty)).thenReturn(TEST_JAVA_CLASS);
    when(mockNewEvent.getCurrentState(mockPackageNameProperty)).thenReturn("com.example");
    when(mockNewEvent.getCurrentState(mockDataOriginTypeProperty))
        .thenReturn(ApplicationConstants.TABLEBASEDTABLE);

    when(mockOBDal.createCriteria(Table.class)).thenReturn(mockCriteria);
    when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.count()).thenReturn(0);

    Method method = ADTableEventHandler.class.getDeclaredMethod("checkClassNameForDuplicates",
        EntityPersistenceEvent.class);
    method.setAccessible(true);
    method.invoke(handler, mockNewEvent);
  }

  @Test(expected = OBException.class)
  public void testCheckClassNameWithDuplicateThrowsException() throws Throwable {
    when(mockNewEvent.getTargetInstance()).thenReturn(mock(Table.class));
    when(mockNewEvent.getTargetInstance().getId()).thenReturn(TEST_TABLE_ID);
    when(mockNewEvent.getCurrentState(mockJavaClassNameProperty)).thenReturn(TEST_JAVA_CLASS);
    when(mockNewEvent.getCurrentState(mockPackageNameProperty)).thenReturn("com.example");
    when(mockNewEvent.getCurrentState(mockDataOriginTypeProperty))
        .thenReturn(ApplicationConstants.TABLEBASEDTABLE);

    when(mockOBDal.createCriteria(Table.class)).thenReturn(mockCriteria);
    when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.count()).thenReturn(1);

    utilityStatic.when(() -> Utility.messageBD(any(), anyString(), anyString()))
        .thenReturn("Duplicate Java Class Name");

    Method method = ADTableEventHandler.class.getDeclaredMethod("checkClassNameForDuplicates",
        EntityPersistenceEvent.class);
    method.setAccessible(true);
    try {
      method.invoke(handler, mockNewEvent);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testCheckClassNameSkipsWhenNotTableBased() throws Exception {
    when(mockNewEvent.getTargetInstance()).thenReturn(mock(Table.class));
    when(mockNewEvent.getTargetInstance().getId()).thenReturn(TEST_TABLE_ID);
    when(mockNewEvent.getCurrentState(mockJavaClassNameProperty)).thenReturn(TEST_JAVA_CLASS);
    when(mockNewEvent.getCurrentState(mockPackageNameProperty)).thenReturn("com.example");
    when(mockNewEvent.getCurrentState(mockDataOriginTypeProperty)).thenReturn("OTHER");

    Method method = ADTableEventHandler.class.getDeclaredMethod("checkClassNameForDuplicates",
        EntityPersistenceEvent.class);
    method.setAccessible(true);
    method.invoke(handler, mockNewEvent);

    verify(mockOBDal, never()).createCriteria(Table.class);
  }

  @Test
  public void testCheckClassNameSkipsWhenJavaClassNameIsNull() throws Exception {
    when(mockNewEvent.getTargetInstance()).thenReturn(mock(Table.class));
    when(mockNewEvent.getTargetInstance().getId()).thenReturn(TEST_TABLE_ID);
    when(mockNewEvent.getCurrentState(mockJavaClassNameProperty)).thenReturn(null);
    when(mockNewEvent.getCurrentState(mockPackageNameProperty)).thenReturn("com.example");
    when(mockNewEvent.getCurrentState(mockDataOriginTypeProperty))
        .thenReturn(ApplicationConstants.TABLEBASEDTABLE);

    Method method = ADTableEventHandler.class.getDeclaredMethod("checkClassNameForDuplicates",
        EntityPersistenceEvent.class);
    method.setAccessible(true);
    method.invoke(handler, mockNewEvent);

    verify(mockOBDal, never()).createCriteria(Table.class);
  }
}
