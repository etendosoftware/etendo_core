package org.openbravo.role.inheritance.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.structure.InheritedAccessEnabled;

/**
 * Tests for {@link AccessTypeInjector}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AccessTypeInjectorTest {

  /**
   * Concrete subclass for testing the non-abstract methods of AccessTypeInjector.
   */
  private static class TestableAccessTypeInjector extends AccessTypeInjector {
    /** Get class name. */

    @Override
    public String getClassName() {
      return "org.openbravo.model.ad.access.Role";
    }

    @Override
    protected String getSecuredElementGetter() {
      return "getRole";
    }

    @Override
    protected String getSecuredElementName() {
      return "role";
    }
  }
  /** Get priority default. */

  @Test
  public void testGetPriorityDefault() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    assertEquals(100, injector.getPriority());
  }
  /** Compare to same priority. */

  @Test
  public void testCompareToSamePriority() {
    TestableAccessTypeInjector injector1 = new TestableAccessTypeInjector();
    TestableAccessTypeInjector injector2 = new TestableAccessTypeInjector();
    assertEquals(0, injector1.compareTo(injector2));
  }
  /** Compare to lower priority. */

  @Test
  public void testCompareToLowerPriority() {
    TestableAccessTypeInjector injector1 = new TestableAccessTypeInjector();
    AccessTypeInjector injector2 = new TestableAccessTypeInjector() {
      /** Get priority. */
      @Override
      public int getPriority() {
        return 200;
      }
    };
    assertTrue(injector1.compareTo(injector2) < 0);
  }
  /** Compare to higher priority. */

  @Test
  public void testCompareToHigherPriority() {
    TestableAccessTypeInjector injector1 = new TestableAccessTypeInjector() {
      /** Get priority. */
      @Override
      public int getPriority() {
        return 200;
      }
    };
    TestableAccessTypeInjector injector2 = new TestableAccessTypeInjector();
    assertTrue(injector1.compareTo(injector2) > 0);
  }
  /** Is inheritable default true. */

  @Test
  public void testIsInheritableDefaultTrue() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    InheritedAccessEnabled access = mock(InheritedAccessEnabled.class);
    assertTrue(injector.isInheritable(access));
  }
  /** Get role property. */

  @Test
  public void testGetRoleProperty() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    assertEquals("role.id", injector.getRoleProperty());
  }
  /** Get skipped properties. */

  @Test
  public void testGetSkippedProperties() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    List<String> skipped = injector.getSkippedProperties();
    assertNotNull(skipped);
    assertEquals(2, skipped.size());
    assertTrue(skipped.contains("creationDate"));
    assertTrue(skipped.contains("createdBy"));
  }
  /** Add entity where clause default. */

  @Test
  public void testAddEntityWhereClauseDefault() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    String whereClause = " as p where p.role.id = :roleId";
    String result = injector.addEntityWhereClause(whereClause);
    assertEquals(whereClause, result);
  }
  /** Check access existence does not throw. */

  @Test
  public void testCheckAccessExistenceDoesNotThrow() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    InheritedAccessEnabled access = mock(InheritedAccessEnabled.class);
    injector.checkAccessExistence(access);
  }
  /** Clear inherit from field in childs does not throw. */

  @Test
  public void testClearInheritFromFieldInChildsDoesNotThrow() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    InheritedAccessEnabled access = mock(InheritedAccessEnabled.class);
    injector.clearInheritFromFieldInChilds(access, true);
  }
  /** Remove reference in parent list does not throw. */

  @Test
  public void testRemoveReferenceInParentListDoesNotThrow() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    InheritedAccessEnabled access = mock(InheritedAccessEnabled.class);
    injector.removeReferenceInParentList(access);
  }
  /**
   * Clear inherited from field with null inherited from.
   * @throws Exception if an error occurs
   */

  @Test
  public void testClearInheritedFromFieldWithNullInheritedFrom() throws Exception {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    InheritedAccessEnabled access = mock(InheritedAccessEnabled.class);
    when(access.getInheritedFrom()).thenReturn(null);

    java.lang.reflect.Method method = AccessTypeInjector.class
        .getDeclaredMethod("clearInheritedFromField", InheritedAccessEnabled.class);
    method.setAccessible(true);
    method.invoke(injector, access);
    // Should not throw - inherited from is null, so nothing happens
  }
  /**
   * Clear inherited from field with role id not matching.
   * @throws Exception if an error occurs
   */

  @Test
  public void testClearInheritedFromFieldWithRoleIdNotMatching() throws Exception {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    InheritedAccessEnabled access = mock(InheritedAccessEnabled.class);
    org.openbravo.model.ad.access.Role role = mock(org.openbravo.model.ad.access.Role.class);
    when(access.getInheritedFrom()).thenReturn(role);
    when(role.getId()).thenReturn("role1");

    java.lang.reflect.Method method = AccessTypeInjector.class
        .getDeclaredMethod("clearInheritedFromField", InheritedAccessEnabled.class, String.class);
    method.setAccessible(true);
    method.invoke(injector, access, "role2");
    // Should not nullify since role ids don't match
  }
  /**
   * Selector creation.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSelectorCreation() throws Exception {
    AccessTypeInjector.Selector selector = new AccessTypeInjector.Selector(
        "org.openbravo.base.structure.InheritedAccessEnabled");
    assertNotNull(selector.value());
    assertEquals(InheritedAccessEnabled.class, selector.value());
  }
  /**
   * Selector with invalid class.
   * @throws Exception if an error occurs
   */

  @Test(expected = Exception.class)
  public void testSelectorWithInvalidClass() throws Exception {
    new AccessTypeInjector.Selector("com.nonexistent.FakeClass");
  }
}
