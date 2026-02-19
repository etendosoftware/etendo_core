package org.openbravo.client.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
/** Tests for {@link ApplicationUtils}. */

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApplicationUtilsTest {

  @Mock
  private OBContext mockOBContext;
  @Mock
  private Role mockRole;
  @Mock
  private User mockUser;
  @Mock
  private OBDal mockOBDal;

  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<OBDal> obDalStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    obContextStatic = mockStatic(OBContext.class);
    obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);
    lenient().when(mockOBContext.getRole()).thenReturn(mockRole);
    lenient().when(mockOBContext.getUser()).thenReturn(mockUser);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obContextStatic != null) obContextStatic.close();
    if (obDalStatic != null) obDalStatic.close();
  }
  /** Is client admin returns true. */

  @Test
  public void testIsClientAdminReturnsTrue() {
    when(mockRole.isClientAdmin()).thenReturn(true);
    assertTrue(ApplicationUtils.isClientAdmin());
  }
  /** Is client admin returns false. */

  @Test
  public void testIsClientAdminReturnsFalse() {
    when(mockRole.isClientAdmin()).thenReturn(false);
    assertFalse(ApplicationUtils.isClientAdmin());
  }
  /** Is org admin returns true when admin orgs exist. */

  @Test
  public void testIsOrgAdminReturnsTrueWhenAdminOrgsExist() {
    @SuppressWarnings("unchecked")
    OBCriteria<RoleOrganization> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(RoleOrganization.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Arrays.asList(mock(RoleOrganization.class)));

    assertTrue(ApplicationUtils.isOrgAdmin());
  }
  /** Is org admin returns false when no admin orgs. */

  @Test
  public void testIsOrgAdminReturnsFalseWhenNoAdminOrgs() {
    @SuppressWarnings("unchecked")
    OBCriteria<RoleOrganization> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(RoleOrganization.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Collections.emptyList());

    assertFalse(ApplicationUtils.isOrgAdmin());
  }
  /** Is role admin returns true when admin roles exist. */

  @Test
  public void testIsRoleAdminReturnsTrueWhenAdminRolesExist() {
    @SuppressWarnings("unchecked")
    OBCriteria<UserRoles> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(UserRoles.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Arrays.asList(mock(UserRoles.class)));

    assertTrue(ApplicationUtils.isRoleAdmin());
  }
  /** Is role admin returns false when no admin roles. */

  @Test
  public void testIsRoleAdminReturnsFalseWhenNoAdminRoles() {
    @SuppressWarnings("unchecked")
    OBCriteria<UserRoles> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(UserRoles.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Collections.emptyList());

    assertFalse(ApplicationUtils.isRoleAdmin());
  }
  /** Get admin orgs returns empty list on exception. */

  @Test
  public void testGetAdminOrgsReturnsEmptyListOnException() {
    when(mockOBDal.createCriteria(RoleOrganization.class)).thenThrow(new RuntimeException("DB error"));

    assertEquals(Collections.emptyList(), ApplicationUtils.getAdminOrgs());
  }
  /** Get admin roles returns empty list on exception. */

  @Test
  public void testGetAdminRolesReturnsEmptyListOnException() {
    when(mockOBDal.createCriteria(UserRoles.class)).thenThrow(new RuntimeException("DB error"));

    assertEquals(Collections.emptyList(), ApplicationUtils.getAdminRoles());
  }
  /** Is ui button returns true for button reference. */

  @Test
  public void testIsUIButtonReturnsTrueForButtonReference() {
    Field field = mock(Field.class);
    Column column = mock(Column.class);
    Reference reference = mock(Reference.class);

    when(field.getColumn()).thenReturn(column);
    when(column.getReference()).thenReturn(reference);
    when(reference.getId()).thenReturn("28");

    assertTrue(ApplicationUtils.isUIButton(field));
  }
  /** Is ui button returns false for non button reference. */

  @Test
  public void testIsUIButtonReturnsFalseForNonButtonReference() {
    Field field = mock(Field.class);
    Column column = mock(Column.class);
    Reference reference = mock(Reference.class);

    when(field.getColumn()).thenReturn(column);
    when(column.getReference()).thenReturn(reference);
    when(reference.getId()).thenReturn("17");

    assertFalse(ApplicationUtils.isUIButton(field));
  }
  /** Is ui button returns false when column is null. */

  @Test
  public void testIsUIButtonReturnsFalseWhenColumnIsNull() {
    Field field = mock(Field.class);
    when(field.getColumn()).thenReturn(null);

    assertFalse(ApplicationUtils.isUIButton(field));
  }
}
