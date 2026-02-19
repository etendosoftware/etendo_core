package org.openbravo.role.inheritance.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.access.User;

/**
 * Tests for {@link AlertRecipientAccessInjector}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AlertRecipientAccessInjectorTest {

  private AlertRecipientAccessInjector injector;

  @Mock
  private OBDal mockOBDal;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<Utility> utilityStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    injector = objenesis.newInstance(AlertRecipientAccessInjector.class);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (utilityStatic != null) utilityStatic.close();
  }

  @Test
  public void testGetSecuredElementGetter() {
    assertEquals("getAlertRule", injector.getSecuredElementGetter());
  }

  @Test
  public void testGetSecuredElementName() {
    assertEquals(AlertRecipient.PROPERTY_ALERTRULE, injector.getSecuredElementName());
  }

  @Test
  public void testIsInheritableReturnsTrueWhenUserContactIsNull() {
    AlertRecipient mockRecipient = mock(AlertRecipient.class);
    when(mockRecipient.getUserContact()).thenReturn(null);

    boolean result = injector.isInheritable(mockRecipient);

    assertTrue(result);
  }

  @Test
  public void testIsInheritableReturnsFalseWhenUserContactIsSet() {
    AlertRecipient mockRecipient = mock(AlertRecipient.class);
    User mockUser = mock(User.class);
    when(mockRecipient.getUserContact()).thenReturn(mockUser);

    boolean result = injector.isInheritable(mockRecipient);

    assertFalse(result);
  }

  @Test
  public void testAddEntityWhereClause() {
    String baseWhere = "where p.role.id = :roleId";

    String result = injector.addEntityWhereClause(baseWhere);

    assertEquals(baseWhere + " and p.userContact is null", result);
  }

  @Test
  public void testGetSkippedPropertiesContainsRole() {
    List<String> skippedProperties = injector.getSkippedProperties();

    assertNotNull(skippedProperties);
    assertTrue(skippedProperties.contains("role"));
    assertTrue(skippedProperties.contains("creationDate"));
    assertTrue(skippedProperties.contains("createdBy"));
  }

  @Test
  public void testCheckAccessExistenceWhenNoDuplicate() {
    AlertRecipient mockRecipient = mock(AlertRecipient.class);
    when(mockRecipient.getAlertRule()).thenReturn(mock(org.openbravo.model.ad.alert.AlertRule.class));
    when(mockRecipient.getRole()).thenReturn(mock(org.openbravo.model.ad.access.Role.class));
    when(mockRecipient.getUserContact()).thenReturn(null);

    OBCriteria<AlertRecipient> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(AlertRecipient.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(new ArrayList<>());

    // Should not throw when no duplicate exists
    injector.checkAccessExistence(mockRecipient);
  }

  @Test
  public void testCheckAccessExistenceWhenDuplicateExists() {
    AlertRecipient mockRecipient = mock(AlertRecipient.class);
    when(mockRecipient.getAlertRule()).thenReturn(mock(org.openbravo.model.ad.alert.AlertRule.class));
    when(mockRecipient.getRole()).thenReturn(mock(org.openbravo.model.ad.access.Role.class));
    when(mockRecipient.getUserContact()).thenReturn(null);

    OBCriteria<AlertRecipient> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(AlertRecipient.class)).thenReturn(mockCriteria);
    List<AlertRecipient> existingList = new ArrayList<>();
    existingList.add(mock(AlertRecipient.class));
    when(mockCriteria.list()).thenReturn(existingList);

    utilityStatic = mockStatic(Utility.class);

    injector.checkAccessExistence(mockRecipient);

    utilityStatic.verify(() -> Utility.throwErrorMessage("DuplicatedAlertRecipientForTemplate"));
  }

  @Test
  public void testCheckAccessExistenceWithUserContact() {
    AlertRecipient mockRecipient = mock(AlertRecipient.class);
    User mockUser = mock(User.class);
    when(mockRecipient.getAlertRule()).thenReturn(mock(org.openbravo.model.ad.alert.AlertRule.class));
    when(mockRecipient.getRole()).thenReturn(mock(org.openbravo.model.ad.access.Role.class));
    when(mockRecipient.getUserContact()).thenReturn(mockUser);

    OBCriteria<AlertRecipient> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(AlertRecipient.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(new ArrayList<>());

    // Should not throw when no duplicate exists
    injector.checkAccessExistence(mockRecipient);
  }
}
