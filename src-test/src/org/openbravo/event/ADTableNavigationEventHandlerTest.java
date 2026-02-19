package org.openbravo.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.TableNavigation;
import org.openbravo.model.ad.ui.Field;

/**
 * Unit tests for {@link ADTableNavigationEventHandler}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ADTableNavigationEventHandlerTest {

  private static final String TEST_TABLE_ID = "100";

  private ADTableNavigationEventHandler handler;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private ModelProvider mockModelProvider;
  @Mock
  private KernelUtils mockKernelUtils;
  @Mock
  private EntityNewEvent mockNewEvent;
  @Mock
  private TableNavigation mockTableNavigation;
  @Mock
  private Field mockField;
  @Mock
  private Property mockFieldProperty;
  @Mock
  private Entity mockTargetEntity;
  @Mock
  private Table mockTable;
  @Mock
  private Entity mockTableNavEntity;
  @Mock
  private Property mockTableNavTableProperty;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<ModelProvider> modelProviderStatic;
  private MockedStatic<KernelUtils> kernelUtilsStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    handler = objenesis.newInstance(ADTableNavigationEventHandler.class);

    obDalStatic = mockStatic(OBDal.class);
    modelProviderStatic = mockStatic(ModelProvider.class);
    kernelUtilsStatic = mockStatic(KernelUtils.class);

    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    modelProviderStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
    kernelUtilsStatic.when(() -> KernelUtils.getProperty(any(Field.class)))
        .thenReturn(mockFieldProperty);
  }

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (modelProviderStatic != null) modelProviderStatic.close();
    if (kernelUtilsStatic != null) kernelUtilsStatic.close();
  }

  @Test
  public void testUpdateTableIdSetsTableWhenFieldIsNotNull() throws Exception {
    when(mockNewEvent.getTargetInstance()).thenReturn(mockTableNavigation);
    when(mockTableNavigation.getField()).thenReturn(mockField);
    when(mockFieldProperty.getTargetEntity()).thenReturn(mockTargetEntity);
    when(mockTargetEntity.getTableId()).thenReturn(TEST_TABLE_ID);
    when(mockOBDal.get(Table.class, TEST_TABLE_ID)).thenReturn(mockTable);
    when(mockModelProvider.getEntity(TableNavigation.ENTITY_NAME)).thenReturn(mockTableNavEntity);
    when(mockTableNavEntity.getProperty(TableNavigation.PROPERTY_TABLE))
        .thenReturn(mockTableNavTableProperty);

    Method method = ADTableNavigationEventHandler.class.getDeclaredMethod("updateTableId",
        EntityPersistenceEvent.class);
    method.setAccessible(true);

    // isValidEvent will return false since entities array is not set, but we invoke updateTableId
    // directly which has the isValidEvent check inside. We need to handle this.
    // Actually, updateTableId checks isValidEvent internally, and since the entities array
    // is not initialized (Objenesis skipped the static init), isValidEvent may return false.
    // So we test the method by subclassing to bypass isValidEvent.

    ADTableNavigationEventHandler testHandler = new ADTableNavigationEventHandler() {
      @Override
      protected boolean isValidEvent(EntityPersistenceEvent event) {
        return true;
      }

      @Override
      protected Entity[] getObservedEntities() {
        return new Entity[0];
      }
    };

    Method updateMethod = ADTableNavigationEventHandler.class.getDeclaredMethod("updateTableId",
        EntityPersistenceEvent.class);
    updateMethod.setAccessible(true);
    updateMethod.invoke(testHandler, mockNewEvent);

    verify(mockNewEvent).setCurrentState(mockTableNavTableProperty, mockTable);
  }

  @Test
  public void testUpdateTableIdDoesNothingWhenFieldIsNull() throws Exception {
    ADTableNavigationEventHandler testHandler = new ADTableNavigationEventHandler() {
      @Override
      protected boolean isValidEvent(EntityPersistenceEvent event) {
        return true;
      }

      @Override
      protected Entity[] getObservedEntities() {
        return new Entity[0];
      }
    };

    when(mockNewEvent.getTargetInstance()).thenReturn(mockTableNavigation);
    when(mockTableNavigation.getField()).thenReturn(null);

    Method updateMethod = ADTableNavigationEventHandler.class.getDeclaredMethod("updateTableId",
        EntityPersistenceEvent.class);
    updateMethod.setAccessible(true);
    updateMethod.invoke(testHandler, mockNewEvent);

    verify(mockOBDal, never()).get(any(Class.class), any(String.class));
  }

  @Test
  public void testUpdateTableIdReturnsWhenEventNotValid() throws Exception {
    ADTableNavigationEventHandler testHandler = new ADTableNavigationEventHandler() {
      @Override
      protected boolean isValidEvent(EntityPersistenceEvent event) {
        return false;
      }

      @Override
      protected Entity[] getObservedEntities() {
        return new Entity[0];
      }
    };

    Method updateMethod = ADTableNavigationEventHandler.class.getDeclaredMethod("updateTableId",
        EntityPersistenceEvent.class);
    updateMethod.setAccessible(true);
    updateMethod.invoke(testHandler, mockNewEvent);

    verify(mockNewEvent, never()).getTargetInstance();
  }
}
