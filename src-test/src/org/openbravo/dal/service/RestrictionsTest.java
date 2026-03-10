package org.openbravo.dal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RestrictionsTest {

  @Mock
  private CriteriaBuilder cb;
  @Mock
  private Root<Object> root;
  @Mock
  private Path<Object> path;
  @Mock
  private Predicate predicate;

  @BeforeEach
  public void setUp() {
    Restrictions.clearCurrentAliases();
  }

  @SuppressWarnings("unchecked")
  private void mockPath(String propertyName) {
    when(root.get(propertyName)).thenReturn(path);
  }

  // --- Factory method tests (verify correct Restriction types are created) ---

  @Test
  public void testEqCreatesSimpleRestriction() {
    Restriction r = Restrictions.eq("name", "test");
    assertNotNull(r);
    assertInstanceOf(Restrictions.SimpleRestriction.class, r);
  }

  @Test
  public void testNeCreatesSimpleRestriction() {
    Restriction r = Restrictions.ne("name", "test");
    assertNotNull(r);
    assertInstanceOf(Restrictions.SimpleRestriction.class, r);
  }

  @Test
  public void testGtCreatesSimpleRestriction() {
    Restriction r = Restrictions.gt("age", 18);
    assertNotNull(r);
    assertInstanceOf(Restrictions.SimpleRestriction.class, r);
  }

  @Test
  public void testGeCreatesSimpleRestriction() {
    assertInstanceOf(Restrictions.SimpleRestriction.class, Restrictions.ge("age", 18));
  }

  @Test
  public void testLtCreatesSimpleRestriction() {
    assertInstanceOf(Restrictions.SimpleRestriction.class, Restrictions.lt("age", 18));
  }

  @Test
  public void testLeCreatesSimpleRestriction() {
    assertInstanceOf(Restrictions.SimpleRestriction.class, Restrictions.le("age", 18));
  }

  @Test
  public void testLikeCreatesSimpleRestriction() {
    assertInstanceOf(Restrictions.SimpleRestriction.class, Restrictions.like("name", "%test%"));
  }

  @Test
  public void testIlikeCreatesILikeRestriction() {
    assertInstanceOf(Restrictions.ILikeRestriction.class, Restrictions.ilike("name", "%test%"));
  }

  @Test
  public void testInWithVarargsCreatesInRestriction() {
    assertInstanceOf(Restrictions.InRestriction.class, Restrictions.in("status", "A", "B", "C"));
  }

  @Test
  public void testInWithCollectionCreatesInRestriction() {
    List<String> values = Arrays.asList("A", "B", "C");
    assertInstanceOf(Restrictions.InRestriction.class, Restrictions.in("status", values));
  }

  @Test
  public void testIsNullCreatesNullRestriction() {
    assertInstanceOf(Restrictions.NullRestriction.class, Restrictions.isNull("name"));
  }

  @Test
  public void testIsNotNullCreatesNullRestriction() {
    assertInstanceOf(Restrictions.NullRestriction.class, Restrictions.isNotNull("name"));
  }

  @Test
  public void testBetweenCreatesBetweenRestriction() {
    assertInstanceOf(Restrictions.BetweenRestriction.class, Restrictions.between("age", 10, 20));
  }

  @Test
  public void testEqPropertyCreatesPropertyComparisonRestriction() {
    assertInstanceOf(Restrictions.PropertyComparisonRestriction.class,
        Restrictions.eqProperty("a", "b"));
  }

  @Test
  public void testNePropertyCreatesPropertyComparisonRestriction() {
    assertInstanceOf(Restrictions.PropertyComparisonRestriction.class,
        Restrictions.neProperty("a", "b"));
  }

  @Test
  public void testLtPropertyCreatesPropertyComparisonRestriction() {
    assertInstanceOf(Restrictions.PropertyComparisonRestriction.class,
        Restrictions.ltProperty("a", "b"));
  }

  @Test
  public void testLePropertyCreatesPropertyComparisonRestriction() {
    assertInstanceOf(Restrictions.PropertyComparisonRestriction.class,
        Restrictions.leProperty("a", "b"));
  }

  @Test
  public void testGtPropertyCreatesPropertyComparisonRestriction() {
    assertInstanceOf(Restrictions.PropertyComparisonRestriction.class,
        Restrictions.gtProperty("a", "b"));
  }

  @Test
  public void testGePropertyCreatesPropertyComparisonRestriction() {
    assertInstanceOf(Restrictions.PropertyComparisonRestriction.class,
        Restrictions.geProperty("a", "b"));
  }

  @Test
  public void testIsEmptyCreatesCollectionSizeRestriction() {
    assertInstanceOf(Restrictions.CollectionSizeRestriction.class,
        Restrictions.isEmpty("items"));
  }

  @Test
  public void testIsNotEmptyCreatesCollectionSizeRestriction() {
    assertInstanceOf(Restrictions.CollectionSizeRestriction.class,
        Restrictions.isNotEmpty("items"));
  }

  @Test
  public void testSizeEqCreatesCollectionSizeRestriction() {
    assertInstanceOf(Restrictions.CollectionSizeRestriction.class,
        Restrictions.sizeEq("items", 5));
  }

  @Test
  public void testSizeNeCreatesCollectionSizeRestriction() {
    assertInstanceOf(Restrictions.CollectionSizeRestriction.class,
        Restrictions.sizeNe("items", 5));
  }

  @Test
  public void testSizeGtCreatesCollectionSizeRestriction() {
    assertInstanceOf(Restrictions.CollectionSizeRestriction.class,
        Restrictions.sizeGt("items", 5));
  }

  @Test
  public void testSizeLtCreatesCollectionSizeRestriction() {
    assertInstanceOf(Restrictions.CollectionSizeRestriction.class,
        Restrictions.sizeLt("items", 5));
  }

  @Test
  public void testSizeGeCreatesCollectionSizeRestriction() {
    assertInstanceOf(Restrictions.CollectionSizeRestriction.class,
        Restrictions.sizeGe("items", 5));
  }

  @Test
  public void testSizeLeCreatesCollectionSizeRestriction() {
    assertInstanceOf(Restrictions.CollectionSizeRestriction.class,
        Restrictions.sizeLe("items", 5));
  }

  @Test
  public void testIdEqCreatesIdRestriction() {
    assertInstanceOf(Restrictions.IdRestriction.class, Restrictions.idEq("123"));
  }

  // --- toPredicate tests (type-safe ones only) ---

  @SuppressWarnings("unchecked")
  @Test
  public void testEqToPredicateCallsCbEqual() {
    mockPath("name");
    when(cb.equal(any(), eq("test"))).thenReturn(predicate);

    Predicate result = Restrictions.eq("name", "test").toPredicate(cb, root);

    assertNotNull(result);
    verify(cb).equal(path, "test");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testEqWithNullValueCallsIsNull() {
    mockPath("name");
    when(cb.isNull(any())).thenReturn(predicate);

    Restrictions.eq("name", null).toPredicate(cb, root);

    verify(cb).isNull(path);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNeToPredicateCallsCbNotEqual() {
    mockPath("name");
    when(cb.notEqual(any(), eq("test"))).thenReturn(predicate);

    Restrictions.ne("name", "test").toPredicate(cb, root);

    verify(cb).notEqual(path, "test");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNeWithNullCallsIsNotNull() {
    mockPath("name");
    when(cb.isNotNull(any())).thenReturn(predicate);

    Restrictions.ne("name", null).toPredicate(cb, root);

    verify(cb).isNotNull(path);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testIsNullToPredicateCallsCbIsNull() {
    mockPath("name");
    when(cb.isNull(any())).thenReturn(predicate);

    Restrictions.isNull("name").toPredicate(cb, root);

    verify(cb).isNull(path);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testIsNotNullToPredicateCallsCbIsNotNull() {
    mockPath("name");
    when(cb.isNotNull(any())).thenReturn(predicate);

    Restrictions.isNotNull("name").toPredicate(cb, root);

    verify(cb).isNotNull(path);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testInToPredicateCallsPathIn() {
    mockPath("status");
    when(path.in(any(Object[].class))).thenReturn(predicate);

    Restrictions.in("status", "A", "B").toPredicate(cb, root);

    verify(path).in(new Object[]{"A", "B"});
  }

  // --- Logical restriction tests ---

  @Test
  public void testAndWithTwoRestrictions() {
    Restriction lhs = Restrictions.eq("a", 1);
    Restriction rhs = Restrictions.eq("b", 2);
    Restriction result = Restrictions.and(lhs, rhs);
    assertInstanceOf(Restrictions.LogicalRestriction.class, result);
  }

  @Test
  public void testOrWithTwoRestrictions() {
    Restriction lhs = Restrictions.eq("a", 1);
    Restriction rhs = Restrictions.eq("b", 2);
    Restriction result = Restrictions.or(lhs, rhs);
    assertInstanceOf(Restrictions.LogicalRestriction.class, result);
  }

  @Test
  public void testAndWithMultipleRestrictions() {
    Restriction r1 = Restrictions.eq("a", 1);
    Restriction r2 = Restrictions.eq("b", 2);
    Restriction r3 = Restrictions.eq("c", 3);
    Restriction result = Restrictions.and(r1, r2, r3);
    assertInstanceOf(Restrictions.MultiLogicalRestriction.class, result);
  }

  @Test
  public void testAndWithSingleRestrictionReturnsSame() {
    Restriction r1 = Restrictions.eq("a", 1);
    Restriction result = Restrictions.and(r1);
    assertEquals(r1, result);
  }

  @Test
  public void testAndWithEmptyReturnsAlwaysTrue() {
    Restriction result = Restrictions.and();
    assertInstanceOf(Restrictions.AlwaysTrueRestriction.class, result);
  }

  @Test
  public void testAndWithNullReturnsAlwaysTrue() {
    Restriction result = Restrictions.and((Restriction[]) null);
    assertInstanceOf(Restrictions.AlwaysTrueRestriction.class, result);
  }

  @Test
  public void testOrWithMultipleRestrictions() {
    Restriction r1 = Restrictions.eq("a", 1);
    Restriction r2 = Restrictions.eq("b", 2);
    Restriction r3 = Restrictions.eq("c", 3);
    Restriction result = Restrictions.or(r1, r2, r3);
    assertInstanceOf(Restrictions.MultiLogicalRestriction.class, result);
  }

  @Test
  public void testOrWithSingleRestrictionReturnsSame() {
    Restriction r1 = Restrictions.eq("a", 1);
    assertEquals(r1, Restrictions.or(r1));
  }

  @Test
  public void testOrWithEmptyReturnsAlwaysFalse() {
    assertInstanceOf(Restrictions.AlwaysFalseRestriction.class, Restrictions.or());
  }

  @Test
  public void testOrWithNullReturnsAlwaysFalse() {
    assertInstanceOf(Restrictions.AlwaysFalseRestriction.class,
        Restrictions.or((Restriction[]) null));
  }

  @Test
  public void testNotCreatesNotRestriction() {
    assertInstanceOf(Restrictions.NotRestriction.class, Restrictions.not(Restrictions.eq("a", 1)));
  }

  // --- eqOrIsNull / neOrIsNotNull ---

  @Test
  public void testEqOrIsNullWithValueReturnsEq() {
    Restriction r = Restrictions.eqOrIsNull("name", "test");
    assertInstanceOf(Restrictions.SimpleRestriction.class, r);
  }

  @Test
  public void testEqOrIsNullWithNullReturnsIsNull() {
    Restriction r = Restrictions.eqOrIsNull("name", null);
    assertInstanceOf(Restrictions.NullRestriction.class, r);
  }

  @Test
  public void testNeOrIsNotNullWithValueReturnsNe() {
    Restriction r = Restrictions.neOrIsNotNull("name", "test");
    assertInstanceOf(Restrictions.SimpleRestriction.class, r);
  }

  @Test
  public void testNeOrIsNotNullWithNullReturnsIsNotNull() {
    Restriction r = Restrictions.neOrIsNotNull("name", null);
    assertInstanceOf(Restrictions.NullRestriction.class, r);
  }

  // --- allEq ---

  @Test
  public void testAllEqCreatesConjunctionOfEquals() {
    Map<String, Object> map = new HashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    Restriction result = Restrictions.allEq(map);
    assertNotNull(result);
  }

  // --- conjunction / disjunction ---

  @Test
  public void testConjunctionReturnsConjunction() {
    Restrictions.Conjunction c = Restrictions.conjunction();
    assertNotNull(c);
  }

  @Test
  public void testConjunctionAdd() {
    Restrictions.Conjunction c = Restrictions.conjunction();
    c.add(Restrictions.eq("a", 1));
    c.add(Restrictions.eq("b", 2));
    assertNotNull(c);
  }

  @Test
  public void testDisjunctionReturnsDisjunction() {
    Restrictions.Disjunction d = Restrictions.disjunction();
    assertNotNull(d);
  }

  @Test
  public void testDisjunctionAdd() {
    Restrictions.Disjunction d = Restrictions.disjunction();
    d.add(Restrictions.eq("a", 1));
    d.add(Restrictions.eq("b", 2));
    assertNotNull(d);
  }

  @Test
  public void testEmptyConjunctionToPredicateCallsCbConjunction() {
    when(cb.conjunction()).thenReturn(predicate);
    Restrictions.Conjunction c = Restrictions.conjunction();
    c.toPredicate(cb, root);
    verify(cb).conjunction();
  }

  @Test
  public void testEmptyDisjunctionToPredicateCallsCbDisjunction() {
    when(cb.disjunction()).thenReturn(predicate);
    Restrictions.Disjunction d = Restrictions.disjunction();
    d.toPredicate(cb, root);
    verify(cb).disjunction();
  }

  // --- conjunction(Restriction...) / disjunction(Restriction...) vararg methods ---

  @Test
  public void testConjunctionVarargEmptyReturnsAlwaysTrue() {
    assertInstanceOf(Restrictions.AlwaysTrueRestriction.class,
        Restrictions.conjunction(new Restriction[0]));
  }

  @Test
  public void testConjunctionVarargSingleReturnsSame() {
    Restriction r = Restrictions.eq("a", 1);
    assertEquals(r, Restrictions.conjunction(r));
  }

  @Test
  public void testConjunctionVarargMultipleReturnsLogical() {
    Restriction r1 = Restrictions.eq("a", 1);
    Restriction r2 = Restrictions.eq("b", 2);
    Restriction r3 = Restrictions.eq("c", 3);
    Restriction result = Restrictions.conjunction(r1, r2, r3);
    assertNotNull(result);
    assertInstanceOf(Restrictions.LogicalRestriction.class, result);
  }

  @Test
  public void testDisjunctionVarargEmptyReturnsAlwaysFalse() {
    assertInstanceOf(Restrictions.AlwaysFalseRestriction.class,
        Restrictions.disjunction(new Restriction[0]));
  }

  @Test
  public void testDisjunctionVarargSingleReturnsSame() {
    Restriction r = Restrictions.eq("a", 1);
    assertEquals(r, Restrictions.disjunction(r));
  }

  @Test
  public void testDisjunctionVarargMultipleReturnsLogical() {
    Restriction r1 = Restrictions.eq("a", 1);
    Restriction r2 = Restrictions.eq("b", 2);
    Restriction result = Restrictions.disjunction(r1, r2);
    assertNotNull(result);
    assertInstanceOf(Restrictions.LogicalRestriction.class, result);
  }

  // --- AlwaysTrue / AlwaysFalse ---

  @Test
  public void testAlwaysTrueCallsCbConjunction() {
    when(cb.conjunction()).thenReturn(predicate);
    new Restrictions.AlwaysTrueRestriction().toPredicate(cb, root);
    verify(cb).conjunction();
  }

  @Test
  public void testAlwaysFalseCallsCbDisjunction() {
    when(cb.disjunction()).thenReturn(predicate);
    new Restrictions.AlwaysFalseRestriction().toPredicate(cb, root);
    verify(cb).disjunction();
  }

  // --- IdRestriction ---

  @SuppressWarnings("unchecked")
  @Test
  public void testIdRestrictionCallsEqualOnIdPath() {
    when(root.get("id")).thenReturn(path);
    when(cb.equal(any(), eq("123"))).thenReturn(predicate);

    Restrictions.idEq("123").toPredicate(cb, root);

    verify(root).get("id");
    verify(cb).equal(path, "123");
  }

  // --- PropertyComparison toPredicate ---

  @SuppressWarnings("unchecked")
  @Test
  public void testEqPropertyToPredicate() {
    Path<Object> path2 = mock(Path.class);
    when(root.get("a")).thenReturn(path);
    when(root.get("b")).thenReturn(path2);
    when(cb.equal(any(), any())).thenReturn(predicate);

    Restrictions.eqProperty("a", "b").toPredicate(cb, root);

    verify(cb).equal(path, path2);
  }

  // --- ThreadLocal alias management ---

  @Test
  public void testSetAndGetCurrentAliases() {
    Map<String, jakarta.persistence.criteria.From<?, ?>> aliases = new HashMap<>();
    Restrictions.setCurrentAliases(aliases);
    assertEquals(aliases, Restrictions.getCurrentAliases());
    Restrictions.clearCurrentAliases();
  }

  @Test
  public void testGetCurrentAliasesReturnsEmptyMapWhenNull() {
    Restrictions.clearCurrentAliases();
    assertNotNull(Restrictions.getCurrentAliases());
    assertEquals(0, Restrictions.getCurrentAliases().size());
  }
}
