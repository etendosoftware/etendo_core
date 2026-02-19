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

  @Test
  public void testGetPriorityDefault() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    assertEquals(100, injector.getPriority());
  }

  @Test
  public void testCompareToSamePriority() {
    TestableAccessTypeInjector injector1 = new TestableAccessTypeInjector();
    TestableAccessTypeInjector injector2 = new TestableAccessTypeInjector();
    assertEquals(0, injector1.compareTo(injector2));
  }

  @Test
  public void testCompareToLowerPriority() {
    TestableAccessTypeInjector injector1 = new TestableAccessTypeInjector();
    AccessTypeInjector injector2 = new TestableAccessTypeInjector() {
      @Override
      public int getPriority() {
        return 200;
      }
    };
    assertTrue(injector1.compareTo(injector2) < 0);
  }

  @Test
  public void testCompareToHigherPriority() {
    TestableAccessTypeInjector injector1 = new TestableAccessTypeInjector() {
      @Override
      public int getPriority() {
        return 200;
      }
    };
    TestableAccessTypeInjector injector2 = new TestableAccessTypeInjector();
    assertTrue(injector1.compareTo(injector2) > 0);
  }

  @Test
  public void testIsInheritableDefaultTrue() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    InheritedAccessEnabled access = mock(InheritedAccessEnabled.class);
    assertTrue(injector.isInheritable(access));
  }

  @Test
  public void testGetRoleProperty() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    assertEquals("role.id", injector.getRoleProperty());
  }

  @Test
  public void testGetSkippedProperties() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    List<String> skipped = injector.getSkippedProperties();
    assertNotNull(skipped);
    assertEquals(2, skipped.size());
    assertTrue(skipped.contains("creationDate"));
    assertTrue(skipped.contains("createdBy"));
  }

  @Test
  public void testAddEntityWhereClauseDefault() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    String whereClause = " as p where p.role.id = :roleId";
    String result = injector.addEntityWhereClause(whereClause);
    assertEquals(whereClause, result);
  }

  @Test
  public void testCheckAccessExistenceDoesNotThrow() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    InheritedAccessEnabled access = mock(InheritedAccessEnabled.class);
    injector.checkAccessExistence(access);
  }

  @Test
  public void testClearInheritFromFieldInChildsDoesNotThrow() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    InheritedAccessEnabled access = mock(InheritedAccessEnabled.class);
    injector.clearInheritFromFieldInChilds(access, true);
  }

  @Test
  public void testRemoveReferenceInParentListDoesNotThrow() {
    TestableAccessTypeInjector injector = new TestableAccessTypeInjector();
    InheritedAccessEnabled access = mock(InheritedAccessEnabled.class);
    injector.removeReferenceInParentList(access);
  }

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

  @Test
  public void testSelectorCreation() throws Exception {
    AccessTypeInjector.Selector selector = new AccessTypeInjector.Selector(
        "org.openbravo.base.structure.InheritedAccessEnabled");
    assertNotNull(selector.value());
    assertEquals(InheritedAccessEnabled.class, selector.value());
  }

  @Test(expected = Exception.class)
  public void testSelectorWithInvalidClass() throws Exception {
    new AccessTypeInjector.Selector("com.nonexistent.FakeClass");
  }
}
