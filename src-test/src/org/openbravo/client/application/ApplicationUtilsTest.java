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

  @Before
  public void setUp() {
    obContextStatic = mockStatic(OBContext.class);
    obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);
    lenient().when(mockOBContext.getRole()).thenReturn(mockRole);
    lenient().when(mockOBContext.getUser()).thenReturn(mockUser);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  @After
  public void tearDown() {
    if (obContextStatic != null) obContextStatic.close();
    if (obDalStatic != null) obDalStatic.close();
  }

  @Test
  public void testIsClientAdminReturnsTrue() {
    when(mockRole.isClientAdmin()).thenReturn(true);
    assertTrue(ApplicationUtils.isClientAdmin());
  }

  @Test
  public void testIsClientAdminReturnsFalse() {
    when(mockRole.isClientAdmin()).thenReturn(false);
    assertFalse(ApplicationUtils.isClientAdmin());
  }

  @Test
  public void testIsOrgAdminReturnsTrueWhenAdminOrgsExist() {
    @SuppressWarnings("unchecked")
    OBCriteria<RoleOrganization> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(RoleOrganization.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Arrays.asList(mock(RoleOrganization.class)));

    assertTrue(ApplicationUtils.isOrgAdmin());
  }

  @Test
  public void testIsOrgAdminReturnsFalseWhenNoAdminOrgs() {
    @SuppressWarnings("unchecked")
    OBCriteria<RoleOrganization> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(RoleOrganization.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Collections.emptyList());

    assertFalse(ApplicationUtils.isOrgAdmin());
  }

  @Test
  public void testIsRoleAdminReturnsTrueWhenAdminRolesExist() {
    @SuppressWarnings("unchecked")
    OBCriteria<UserRoles> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(UserRoles.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Arrays.asList(mock(UserRoles.class)));

    assertTrue(ApplicationUtils.isRoleAdmin());
  }

  @Test
  public void testIsRoleAdminReturnsFalseWhenNoAdminRoles() {
    @SuppressWarnings("unchecked")
    OBCriteria<UserRoles> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(UserRoles.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Collections.emptyList());

    assertFalse(ApplicationUtils.isRoleAdmin());
  }

  @Test
  public void testGetAdminOrgsReturnsEmptyListOnException() {
    when(mockOBDal.createCriteria(RoleOrganization.class)).thenThrow(new RuntimeException("DB error"));

    assertEquals(Collections.emptyList(), ApplicationUtils.getAdminOrgs());
  }

  @Test
  public void testGetAdminRolesReturnsEmptyListOnException() {
    when(mockOBDal.createCriteria(UserRoles.class)).thenThrow(new RuntimeException("DB error"));

    assertEquals(Collections.emptyList(), ApplicationUtils.getAdminRoles());
  }

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

  @Test
  public void testIsUIButtonReturnsFalseWhenColumnIsNull() {
    Field field = mock(Field.class);
    when(field.getColumn()).thenReturn(null);

    assertFalse(ApplicationUtils.isUIButton(field));
  }
}
