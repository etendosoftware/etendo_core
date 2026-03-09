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
package org.openbravo.event;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.criterion.Criterion;
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
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;

/**
 * Unit tests for {@link UserDefaultForDocsEventHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class UserDefaultForDocsEventHandlerTest {

  private static final String USER_ID = "USER-ID-001";
  private static final String CONFLICT_ERROR = "conflict error";

  @Mock private EntityNewEvent newEvent;
  @Mock private EntityUpdateEvent updateEvent;
  @Mock private User user;
  @Mock private BusinessPartner bp;
  @Mock private ModelProvider modelProvider;
  @Mock private Entity entity;
  @Mock private OBCriteria<User> criteria;
  @Mock private OBDal obDal;

  /**
   * Sets up common mock behaviour shared across all tests.
   */
  @Before
  public void setUp() {
    lenient().when(user.getId()).thenReturn(USER_ID);
    lenient().when(user.isDefaultfordocs()).thenReturn(true);
    lenient().when(user.getBusinessPartner()).thenReturn(bp);
  }

  /**
   * When {@code isDefaultfordocs} is {@code false} the handler must return
   * early without querying the database.
   */
  @Test
  public void testValidateDoesNothingWhenNotDefaultForDocs() {
    when(user.isDefaultfordocs()).thenReturn(false);
    UserDefaultForDocsEventHandler handler = new UserDefaultForDocsEventHandler();
    withStaticMocks(() -> {
      assertDoesNotThrow(() -> handler.validate(user));
      verify(obDal, never()).createCriteria(User.class);
    });
  }

  /**
   * When the user has no associated {@link BusinessPartner} the handler must
   * return early without querying the database.
   */
  @Test
  public void testValidateDoesNothingWhenBusinessPartnerIsNull() {
    when(user.getBusinessPartner()).thenReturn(null);
    UserDefaultForDocsEventHandler handler = new UserDefaultForDocsEventHandler();
    withStaticMocks(() -> {
      assertDoesNotThrow(() -> handler.validate(user));
      verify(obDal, never()).createCriteria(User.class);
    });
  }

  /**
   * When the user is marked as default for docs, has a {@link BusinessPartner},
   * and no other user in that Business Partner holds the flag, no exception
   * must be thrown.
   */
  @Test
  public void testValidateDoesNotThrowWhenNoConflict() {
    UserDefaultForDocsEventHandler handler = new UserDefaultForDocsEventHandler();
    withStaticMocks(() -> {
      stubCriteriaWithResult(null);
      assertDoesNotThrow(() -> handler.validate(user));
    });
  }

  /**
   * When the user is marked as default for docs and another user in the same
   * {@link BusinessPartner} already holds that flag, an {@link OBException}
   * must be thrown.
   * @throws OBException always, as expected by the assertion
   */
  @Test
  public void testValidateThrowsWhenConflictExists() {
    UserDefaultForDocsEventHandler handler = new UserDefaultForDocsEventHandler();
    withStaticMocksAndMessages(() -> {
      stubCriteriaWithResult(mock(User.class));
      assertThrows(OBException.class, () -> handler.validate(user));
    });
  }

  /**
   * Saving a new {@link User} that is the first default for docs in its
   * {@link BusinessPartner} must not throw any exception.
   */
  @Test
  public void testOnSaveDoesNotThrowWhenNoConflict() {
    UserDefaultForDocsEventHandler handler = createHandlerWithValidEvent();
    withStaticMocks(() -> {
      stubCriteriaWithResult(null);
      when(newEvent.getTargetInstance()).thenReturn(user);
      assertDoesNotThrow(() -> handler.onSave(newEvent));
    });
  }

  /**
   * Saving a new {@link User} as default for docs when another user in the
   * same {@link BusinessPartner} already holds that flag must throw an
   * {@link OBException}.
   * @throws OBException always, as expected by the assertion
   */
  @Test
  public void testOnSaveThrowsWhenConflictExists() {
    UserDefaultForDocsEventHandler handler = createHandlerWithValidEvent();
    withStaticMocksAndMessages(() -> {
      stubCriteriaWithResult(mock(User.class));
      when(newEvent.getTargetInstance()).thenReturn(user);
      assertThrows(OBException.class, () -> handler.onSave(newEvent));
    });
  }

  /**
   * Updating a {@link User} to be the default for docs when no other user in
   * the same {@link BusinessPartner} holds that flag must not throw any
   * exception.
   */
  @Test
  public void testOnUpdateDoesNotThrowWhenNoConflict() {
    UserDefaultForDocsEventHandler handler = createHandlerWithValidEvent();
    withStaticMocks(() -> {
      stubCriteriaWithResult(null);
      when(updateEvent.getTargetInstance()).thenReturn(user);
      assertDoesNotThrow(() -> handler.onUpdate(updateEvent));
    });
  }

  /**
   * Updating a {@link User} as default for docs when another user in the same
   * {@link BusinessPartner} already holds that flag must throw an
   * {@link OBException}.
   * @throws OBException always, as expected by the assertion
   */
  @Test
  public void testOnUpdateThrowsWhenConflictExists() {
    UserDefaultForDocsEventHandler handler = createHandlerWithValidEvent();
    withStaticMocksAndMessages(() -> {
      stubCriteriaWithResult(mock(User.class));
      when(updateEvent.getTargetInstance()).thenReturn(user);
      assertThrows(OBException.class, () -> handler.onUpdate(updateEvent));
    });
  }

  /**
   * {@code getObservedEntities()} must return a non-{@code null} array so the
   * CDI framework can register the handler correctly.
   */
  @Test
  public void testGetObservedEntitiesReturnsNonNull() {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(User.ENTITY_NAME)).thenReturn(entity);
      UserDefaultForDocsEventHandler handler = new UserDefaultForDocsEventHandler();
      assertNotNull(handler.getObservedEntities());
    }
  }

  /**
   * Creates a {@link UserDefaultForDocsEventHandler} whose
   * {@code isValidEvent} always returns {@code true}, bypassing CDI checks.
   * @return a handler instance suitable for {@code onSave} / {@code onUpdate}
   *         tests
   */
  private UserDefaultForDocsEventHandler createHandlerWithValidEvent() {
    return new UserDefaultForDocsEventHandler() {
      @Override
      protected boolean isValidEvent(EntityPersistenceEvent event) {
        return true;
      }
    };
  }

  /**
   * Stubs the shared {@link OBCriteria} mock so that
   * {@code uniqueResult()} returns the given value.
   * @param result the object to return from {@code uniqueResult()}, or
   *               {@code null} to simulate no conflict
   */
  private void stubCriteriaWithResult(User result) {
    when(obDal.createCriteria(User.class)).thenReturn(criteria);
    lenient().when(criteria.add(any(Criterion.class))).thenReturn(criteria);
    when(criteria.uniqueResult()).thenReturn(result);
  }

  /**
   * Opens {@link MockedStatic} scopes for {@link ModelProvider} and
   * {@link OBDal}, executes the given block, then closes them.
   * @param block the test logic to run within the static-mock scope
   */
  private void withStaticMocks(Runnable block) {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class);
         MockedStatic<OBDal> dal = mockStatic(OBDal.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(User.ENTITY_NAME)).thenReturn(entity);
      dal.when(OBDal::getInstance).thenReturn(obDal);
      block.run();
    }
  }

  /**
   * Opens {@link MockedStatic} scopes for {@link ModelProvider},
   * {@link OBDal}, and {@link OBMessageUtils}, executes the given block,
   * then closes them. The message utility is stubbed to return
   * {@link #CONFLICT_ERROR} for any message key.
   * @param block the test logic to run within the static-mock scope
   */
  private void withStaticMocksAndMessages(Runnable block) {
    try (MockedStatic<ModelProvider> mp = mockStatic(ModelProvider.class);
         MockedStatic<OBDal> dal = mockStatic(OBDal.class);
         MockedStatic<OBMessageUtils> msg = mockStatic(OBMessageUtils.class)) {
      mp.when(ModelProvider::getInstance).thenReturn(modelProvider);
      lenient().when(modelProvider.getEntity(User.ENTITY_NAME)).thenReturn(entity);
      dal.when(OBDal::getInstance).thenReturn(obDal);
      msg.when(() -> OBMessageUtils.messageBD(anyString())).thenReturn(CONFLICT_ERROR);
      block.run();
    }
  }
}
