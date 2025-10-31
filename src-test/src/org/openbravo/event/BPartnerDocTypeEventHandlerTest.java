package org.openbravo.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.BusinessPartnerDocType;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for {@link BPartnerDocTypeEventHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BPartnerDocTypeEventHandlerTest {

  @Mock
  private EntityNewEvent newEvent;
  @Mock
  private EntityUpdateEvent updateEvent;
  @Mock
  private BusinessPartnerDocType bpDocType;
  @Mock
  private Client client;
  @Mock
  private BusinessPartner bp;
  @Mock
  private Organization org;
  @Mock
  private ModelProvider modelProvider;
  @Mock
  private Entity entity;
  
  /**
   * Initializes the test environment before each execution.
   */
  @Before
  public void setUp() {
    lenient().when(bpDocType.getId()).thenReturn("123");
    lenient().when(bpDocType.isActive()).thenReturn(true);
    lenient().when(bpDocType.getClient()).thenReturn(client);
    lenient().when(bpDocType.getBusinessPartner()).thenReturn(bp);
    lenient().when(bpDocType.getOrganization()).thenReturn(org);
    lenient().when(bpDocType.getDocumentcategory()).thenReturn("SOO");
    lenient().when(bpDocType.isSotrx()).thenReturn(true);

  }

  /**
   * Verifies that saving a new {@link BusinessPartnerDocType} without
   * any duplicates in the database does not throw an exception.
   */
  @Test
  public void testOnSaveNoDuplicate() {
    BPartnerDocTypeEventHandler handler =
      new BPartnerDocTypeEventHandler() {
        @Override
        protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
          return true;
        }
      };
    try (MockedStatic<ModelProvider> modelProviderMock = mockStatic(ModelProvider.class);
         MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {
      obContextMock.when(() -> OBContext.setAdminMode(true)).thenAnswer(inv -> null);
      obContextMock.when(OBContext::restorePreviousMode).thenAnswer(inv -> null);
      modelProviderMock.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(BusinessPartnerDocType.ENTITY_NAME)).thenReturn(entity);
      OBDal obDal = mock(OBDal.class);
      OBCriteria<BusinessPartnerDocType> criteria = mock(OBCriteria.class);
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(BusinessPartnerDocType.class)).thenReturn(criteria);
      lenient().when(criteria.add(any(Predicate.class))).thenReturn(criteria);
      when(criteria.uniqueResult()).thenReturn(null);
      when(newEvent.getTargetInstance()).thenReturn(bpDocType);
      assertDoesNotThrow(() -> handler.onSave(newEvent));
    }
  }

  /**
   * Verifies that saving a new {@link BusinessPartnerDocType} when a duplicate exists
   * results in an {@link OBException} being thrown with the appropriate error message.
   */
  @Test
  public void testOnSaveWithDuplicate() {
    BPartnerDocTypeEventHandler handler =
      new BPartnerDocTypeEventHandler() {
        @Override
        protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
          return true;
        }
      };
    try (MockedStatic<ModelProvider> modelProviderMock = mockStatic(ModelProvider.class);
         MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<OBMessageUtils> messageUtilsMock = mockStatic(OBMessageUtils.class);
         MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {
      obContextMock.when(() -> OBContext.setAdminMode(true)).thenAnswer(inv -> null);
      obContextMock.when(OBContext::restorePreviousMode).thenAnswer(inv -> null);
      modelProviderMock.when(ModelProvider::getInstance).thenReturn(modelProvider);
      OBDal obDal = mock(OBDal.class);
      OBCriteria<BusinessPartnerDocType> criteria = mock(OBCriteria.class);
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(BusinessPartnerDocType.class)).thenReturn(criteria);
      when(criteria.add(any(Predicate.class))).thenReturn(criteria);
      when(criteria.uniqueResult()).thenReturn(mock(BusinessPartnerDocType.class));
      messageUtilsMock.when(() -> OBMessageUtils.messageBD(anyString()))
        .thenReturn("BPDocTypeUnique");
      when(newEvent.getTargetInstance()).thenReturn(bpDocType);
      assertThrows(OBException.class, () -> handler.onSave(newEvent));
    }
  }

  /**
   * Verifies that updating an existing {@link BusinessPartnerDocType} without
   * any duplicates in the database does not throw an exception.
   */
  @Test
  public void testOnUpdateNoDuplicate() {
    BPartnerDocTypeEventHandler handler =
      new BPartnerDocTypeEventHandler() {
        @Override
        protected boolean isValidEvent(org.openbravo.client.kernel.event.EntityPersistenceEvent event) {
          return true;
        }
      };
    try (MockedStatic<ModelProvider> modelProviderMock = mockStatic(ModelProvider.class);
         MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {
      obContextMock.when(() -> OBContext.setAdminMode(true)).thenAnswer(inv -> null);
      obContextMock.when(OBContext::restorePreviousMode).thenAnswer(inv -> null);
      modelProviderMock.when(ModelProvider::getInstance).thenReturn(modelProvider);
      OBDal obDal = mock(OBDal.class);
      OBCriteria<BusinessPartnerDocType> criteria = mock(OBCriteria.class);
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(BusinessPartnerDocType.class)).thenReturn(criteria);
      when(criteria.add(any(Predicate.class))).thenReturn(criteria);
      when(criteria.uniqueResult()).thenReturn(null);
      when(updateEvent.getTargetInstance()).thenReturn(bpDocType);
      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
    }
  }
}
