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
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */

package org.openbravo.test.dal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.hibernate.criterion.Restrictions;
import org.hibernate.stat.Statistics;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.IdentifierProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.financialmgmt.tax.TaxTrl;
import org.openbravo.model.financialmgmt.tax.TaxZone;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.SystemLanguageFixture;
import org.openbravo.test.base.TestConstants.Clients;
import org.openbravo.test.base.TestConstants.Languages;
import org.openbravo.test.base.TestConstants.Orgs;
import org.openbravo.test.base.TestConstants.Roles;
import org.openbravo.test.base.TestConstants.Users;

/**
 * {@link IdentifierProvider#preloadTranslations(java.util.Collection)} must give the same labels
 * as {@code getIdentifier()} alone, with one query per translation entity instead of one per
 * object. Taxes are used: their identifier is a translatable name plus a foreign key to a tax
 * category, itself translatable, so one page covers two translation entities and lazy references.
 */
public class IdentifierTranslationPreloadTest extends OBBaseTest {
  private static final String MARK = "ETP6103-trl ";
  private static final int PAGE = 40;

  private Statistics statistics;

  // A translation is only looked up when a second system language is installed
  private static final SystemLanguageFixture SPANISH = new SystemLanguageFixture(
      Languages.ES_ES_LANG_ID);

  @BeforeClass
  public static void installSpanish() {
    SPANISH.install();
  }

  @AfterClass
  public static void restoreSpanish() {
    SPANISH.restore();
  }

  @Before
  public void setSpanishContext() {
    OBContext.setOBContext(Users.ADMIN, Roles.FB_GRP_ADMIN, Clients.FB_GRP, Orgs.MAIN,
        Languages.ES_ES_ISOCODE);
    OBContext.setAdminMode(true);
    SessionHandler.getInstance().setDoRollback(true);
    assertTrue("precondition: a translation is installed", OBContext.hasTranslationInstalled());
    statistics = SessionFactoryController.getInstance().getSessionFactory().getStatistics();
    statistics.setStatisticsEnabled(true);
  }

  @After
  public void restoreMode() {
    statistics.setStatisticsEnabled(false);
    OBContext.restorePreviousMode();
  }

  @Test
  public void preloadKeepsTheLabels() {
    final TaxTrl renamed = renameOneTranslation();
    final List<String> expected = labels(taxes());

    final List<TaxRate> rows = taxes();
    IdentifierProvider.getInstance().preloadTranslations(rows);

    assertEquals(expected, labels(rows));
    assertTrue("the renamed translation is shown",
        expected.stream().anyMatch(label -> label.contains(renamed.getName())));
  }

  @Test
  public void labelsRunNoQueryAfterThePreload() {
    final List<TaxRate> withoutPreload = taxes();
    statistics.clear();
    labels(withoutPreload);
    assertThat("precondition: without preload each row queries its translation",
        statistics.getPrepareStatementCount(), greaterThanOrEqualTo((long) withoutPreload.size()));

    final List<TaxRate> rows = taxes();
    IdentifierProvider.getInstance().preloadTranslations(rows);
    statistics.clear();
    labels(rows);

    assertEquals(0, statistics.getPrepareStatementCount());
  }

  @Test
  public void oneQueryPerTranslationEntity() {
    final List<TaxRate> rows = taxes();
    final List<BaseOBObject> objects = new ArrayList<>(rows);
    for (TaxRate tax : rows) {
      Hibernate.initialize(tax.getTaxCategory());
      // an object reached both as a row and through a foreign key is looked up once
      objects.add(tax.getTaxCategory());
    }
    statistics.clear();

    IdentifierProvider.getInstance().preloadTranslations(objects);

    // the translation rows reference their language by code, so it loads once per session too
    assertEquals("tax and tax category translations", 2, statistics.getQueryExecutionCount());
  }

  @Test
  public void objectWithoutTranslationRowDoesNotQueryLater() {
    final TaxTrl removed = firstTranslation();
    final String taxId = removed.getTax().getId();
    OBDal.getInstance().remove(removed);
    OBDal.getInstance().flush();
    final List<String> expected = labels(taxes());

    final List<TaxRate> rows = taxes();
    IdentifierProvider.getInstance().preloadTranslations(rows);
    statistics.clear();

    assertEquals(expected, labels(rows));
    assertEquals(0, statistics.getPrepareStatementCount());
    assertTrue(rows.stream().anyMatch(tax -> tax.getId().equals(taxId)));
  }

  // getValue, not get: the walk must not trip the derived-readable check of a non-admin caller
  @Test
  public void nonAdminCallerGetsTheSameLabelsWithNoQuery() {
    OBContext.restorePreviousMode();
    try {
      OBContext.setOBContext(TEST_USER_ID, TEST_ROLE_ID, TEST_CLIENT_ID, TEST_ORG_ID,
          Languages.ES_ES_ISOCODE);
      final List<String> expected = labels(taxes());

      final List<TaxRate> rows = taxes();
      IdentifierProvider.getInstance().preloadTranslations(rows);
      statistics.clear();

      assertEquals(expected, labels(rows));
      assertEquals(0, statistics.getPrepareStatementCount());
    } finally {
      // the @After restores the administrator mode this test left
      OBContext.setAdminMode(true);
    }
  }

  @Test
  public void displayColumnIdentifierKeepsItsLabel() {
    final List<String> expected = labels(taxZones());
    assumeFalse("the test client has tax zones", expected.isEmpty());

    final List<TaxZone> rows = taxZones();
    IdentifierProvider.getInstance().preloadTranslations(rows);

    assertEquals(expected, labels(rows));
  }

  @Test
  public void noContextDoesNothing() {
    final List<TaxRate> rows = taxes();
    final OBContext context = OBContext.getOBContext();
    statistics.clear();
    try {
      OBContext.setOBContext((OBContext) null);
      IdentifierProvider.getInstance().preloadTranslations(rows);
    } finally {
      OBContext.setOBContext(context);
    }

    assertEquals(0, statistics.getPrepareStatementCount());
  }

  @Test
  public void entriesThatAreNotObjectsAreIgnored() {
    final List<String> expected = labels(taxes());
    final List<TaxRate> rows = taxes();
    final List<Object> mixed = new ArrayList<>(Arrays.asList(null, "not an object", 42));
    mixed.addAll(rows);

    IdentifierProvider.getInstance().preloadTranslations(mixed);
    IdentifierProvider.getInstance().preloadTranslations(null);

    assertEquals(expected, labels(rows));
    assertFalse(expected.isEmpty());
  }

  private TaxTrl firstTranslation() {
    final OBCriteria<TaxTrl> criteria = OBDal.getInstance().createCriteria(TaxTrl.class);
    criteria.add(Restrictions.eq(TaxTrl.PROPERTY_TAX + ".id", taxes().get(0).getId()));
    criteria.add(Restrictions.eq(TaxTrl.PROPERTY_LANGUAGE + ".language",
        Languages.ES_ES_ISOCODE));
    criteria.setMaxResults(1);
    final TaxTrl translation = (TaxTrl) criteria.uniqueResult();
    assumeFalse("the first test tax has a Spanish translation", translation == null);
    return translation;
  }

  private TaxTrl renameOneTranslation() {
    final TaxTrl translation = firstTranslation();
    translation.setName(MARK + translation.getName());
    OBDal.getInstance().flush();
    return translation;
  }

  // a fresh session each time, so references are lazy and no translation is memoized
  private List<TaxRate> taxes() {
    OBDal.getInstance().getSession().clear();
    final OBCriteria<TaxRate> criteria = OBDal.getInstance().createCriteria(TaxRate.class);
    criteria.add(Restrictions.eq(TaxRate.PROPERTY_CLIENT + ".id", Clients.FB_GRP));
    criteria.addOrderBy(TaxRate.PROPERTY_ID, true);
    criteria.setMaxResults(PAGE);
    return criteria.list();
  }

  private List<TaxZone> taxZones() {
    OBDal.getInstance().getSession().clear();
    final OBCriteria<TaxZone> criteria = OBDal.getInstance().createCriteria(TaxZone.class);
    criteria.add(Restrictions.eq(TaxZone.PROPERTY_CLIENT + ".id", Clients.FB_GRP));
    criteria.addOrderBy(TaxZone.PROPERTY_ID, true);
    criteria.setMaxResults(PAGE);
    return criteria.list();
  }

  private static List<String> labels(List<? extends BaseOBObject> objects) {
    return objects.stream().map(BaseOBObject::getIdentifier).collect(Collectors.toList());
  }
}
