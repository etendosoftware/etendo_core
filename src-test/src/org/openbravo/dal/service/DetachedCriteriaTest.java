package org.openbravo.dal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.JoinType;

public class DetachedCriteriaTest {

  @Test
  public void testForClassByClass() {
    DetachedCriteria dc = DetachedCriteria.forClass(String.class);
    assertNotNull(dc);
    assertEquals(String.class, dc.getEntityClass());
  }

  @Test
  public void testForClassByClassWithAlias() {
    DetachedCriteria dc = DetachedCriteria.forClass(String.class, "s");
    assertNotNull(dc);
    assertEquals(String.class, dc.getEntityClass());
  }

  @Test
  public void testForClassByName() {
    DetachedCriteria dc = DetachedCriteria.forClass("java.lang.String");
    assertNotNull(dc);
    assertEquals(String.class, dc.getEntityClass());
  }

  @Test
  public void testForClassByInvalidNameThrows() {
    assertThrows(RuntimeException.class, () ->
        DetachedCriteria.forClass("com.nonexistent.FakeClass"));
  }

  @Test
  public void testForEntityName() {
    DetachedCriteria dc = DetachedCriteria.forEntityName("java.lang.String", "alias");
    assertNotNull(dc);
    assertEquals(String.class, dc.getEntityClass());
  }

  @Test
  public void testForEntityNameInvalidThrows() {
    assertThrows(RuntimeException.class, () ->
        DetachedCriteria.forEntityName("com.nonexistent.FakeClass", "alias"));
  }

  @Test
  public void testAddReturnsThis() {
    DetachedCriteria dc = DetachedCriteria.forClass(String.class);
    DetachedCriteria result = dc.add(Restrictions.eq("value", "test"));
    assertSame(dc, result);
  }

  @Test
  public void testCreateAliasReturnsThis() {
    DetachedCriteria dc = DetachedCriteria.forClass(String.class);
    DetachedCriteria result = dc.createAlias("assoc", "a");
    assertSame(dc, result);
  }

  @Test
  public void testCreateAliasWithJoinTypeReturnsThis() {
    DetachedCriteria dc = DetachedCriteria.forClass(String.class);
    DetachedCriteria result = dc.createAlias("assoc", "a", JoinType.LEFT);
    assertSame(dc, result);
  }

  @Test
  public void testSetProjectionReturnsThis() {
    DetachedCriteria dc = DetachedCriteria.forClass(String.class);
    DetachedCriteria result = dc.setProjection(Projections.property("value"));
    assertSame(dc, result);
  }

  @Test
  public void testMethodChaining() {
    DetachedCriteria dc = DetachedCriteria.forClass(String.class)
        .add(Restrictions.eq("a", 1))
        .add(Restrictions.gt("b", 2))
        .createAlias("assoc", "al")
        .setProjection(Projections.property("id"));
    assertNotNull(dc);
    assertEquals(String.class, dc.getEntityClass());
  }

  // --- Inner classes ---

  @Test
  public void testRestrictionEntryGetRestriction() {
    Restriction r = Restrictions.eq("a", 1);
    DetachedCriteria.RestrictionEntry entry = new DetachedCriteria.RestrictionEntry(r);
    assertSame(r, entry.getRestriction());
  }

  @Test
  public void testAliasEntryGetters() {
    DetachedCriteria.AliasEntry entry = new DetachedCriteria.AliasEntry("path", "alias", JoinType.LEFT);
    assertEquals("path", entry.getPath());
    assertEquals("alias", entry.getAlias());
    assertEquals(JoinType.LEFT, entry.getJoinType());
  }

  @Test
  public void testAliasEntryWithNullJoinType() {
    DetachedCriteria.AliasEntry entry = new DetachedCriteria.AliasEntry("path", "alias", null);
    assertEquals("path", entry.getPath());
    assertEquals("alias", entry.getAlias());
    assertEquals(null, entry.getJoinType());
  }
}
