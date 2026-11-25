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
package org.openbravo.materialmgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.materialmgmt.onhandquantity.ValuedStockAggregated;

/**
 * Tests how {@link ResetValuedStockAggregated} decides which Periods belong to a Legal Entity.
 * <p>
 * Periods created through the Initial Client Setup wizard are owned by the '0' Organization, so
 * resolving them by {@code Period.organization} silently yields nothing. The scope has to be
 * resolved through the Period Control table instead, which is the one kept per Organization.
 */
public class ResetValuedStockAggregatedPeriodScopeTest {

  private static final String CLIENT_ID = "clientUnderTest";
  private static final String REQUESTED_ORG_ID = "requestedOrg";
  private static final String LEGAL_ENTITY_ID = "legalEntityOrg";
  private static final String PERIOD_CONTROL_ORG_ID = "periodControlOrg";
  private static final String ORG_ID_PARAMETER = "orgId";

  private MockedStatic<OBDal> staticOBDal;
  private MockedStatic<OBContext> staticOBContext;

  private OBDal dal;
  private OrganizationStructureProvider osp;
  private OBQuery<Period> periodQuery;
  private Organization legalEntity;
  private Organization periodControlOrg;

  /**
   * Wires the minimum set of collaborators needed to reach the Period query.
   */
  @Before
  @SuppressWarnings("unchecked")
  public void setUp() {
    dal = mock(OBDal.class);
    osp = mock(OrganizationStructureProvider.class);
    periodQuery = mock(OBQuery.class);
    legalEntity = mockOrganization(LEGAL_ENTITY_ID);
    periodControlOrg = mockOrganization(PERIOD_CONTROL_ORG_ID);

    staticOBDal = mockStatic(OBDal.class);
    staticOBDal.when(OBDal::getInstance).thenReturn(dal);

    final OBContext context = mock(OBContext.class);
    staticOBContext = mockStatic(OBContext.class);
    staticOBContext.when(OBContext::getOBContext).thenReturn(context);
    when(context.getOrganizationStructureProvider(CLIENT_ID)).thenReturn(osp);

    final Organization requestedOrg = mockOrganization(REQUESTED_ORG_ID);
    when(dal.get(Organization.class, REQUESTED_ORG_ID)).thenReturn(requestedOrg);
    when(osp.getLegalEntity(any(Organization.class))).thenReturn(legalEntity);

    // Date of the last aggregated row: none yet
    final OBCriteria<ValuedStockAggregated> criteria = mock(OBCriteria.class);
    when(dal.createCriteria(ValuedStockAggregated.class)).thenReturn(criteria);
    when(criteria.add(any())).thenReturn(criteria);
    when(criteria.setProjection(any())).thenReturn(criteria);
    when(criteria.uniqueResult()).thenReturn(null);

    // Starting date of the first not closed Period
    final Session session = mock(Session.class);
    final Query<Date> dateQuery = mock(Query.class);
    when(dal.getSession()).thenReturn(session);
    when(session.createQuery(anyString(), eq(Date.class))).thenReturn(dateQuery);
    when(dateQuery.setParameter(anyString(), any())).thenReturn(dateQuery);
    when(dateQuery.setMaxResults(anyInt())).thenReturn(dateQuery);
    when(dateQuery.list()).thenReturn(Collections.singletonList(new Date(Long.MAX_VALUE)));

    when(dal.createQuery(eq(Period.class), anyString())).thenReturn(periodQuery);
    when(periodQuery.setNamedParameter(anyString(), any())).thenReturn(periodQuery);
    when(periodQuery.list()).thenReturn(Collections.singletonList(mock(Period.class)));
  }

  /**
   * Releases the static mocks opened for the test.
   */
  @After
  public void tearDown() {
    if (staticOBDal != null) {
      staticOBDal.close();
    }
    if (staticOBContext != null) {
      staticOBContext.close();
    }
  }

  private Organization mockOrganization(final String id) {
    final Organization organization = mock(Organization.class);
    when(organization.getId()).thenReturn(id);
    return organization;
  }

  /**
   * The Period scope has to be resolved through the Period Control table, never through the
   * Organization owning the Period, which is '0' for calendars created by the client wizard.
   */
  @Test
  public void periodsAreScopedThroughPeriodControl() {
    when(osp.getPeriodControlAllowedOrganization(legalEntity)).thenReturn(periodControlOrg);

    ResetValuedStockAggregated.getClosedPeriodsToAggregate(new Date(), CLIENT_ID, REQUESTED_ORG_ID);

    final ArgumentCaptor<String> hql = ArgumentCaptor.forClass(String.class);
    verify(dal).createQuery(eq(Period.class), hql.capture());

    assertTrue("the Period scope must come from the Period Control table",
        hql.getValue().contains("FinancialMgmtPeriodControl"));
    assertTrue("the Period Control rows must be restricted to one Organization",
        hql.getValue().contains("pc.organization.id = :orgId"));
    assertEquals("the Period must not be filtered by the Organization owning it", -1,
        hql.getValue().indexOf("p.organization.id"));
  }

  /**
   * The Organization the Periods are looked up for is the one allowed to control them, which is
   * not necessarily the Legal Entity itself.
   */
  @Test
  public void periodsAreLookedUpForThePeriodControlOrganization() {
    when(osp.getPeriodControlAllowedOrganization(legalEntity)).thenReturn(periodControlOrg);

    ResetValuedStockAggregated.getClosedPeriodsToAggregate(new Date(), CLIENT_ID, REQUESTED_ORG_ID);

    final ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
    verify(periodQuery).setNamedParameter(eq(ORG_ID_PARAMETER), value.capture());

    assertEquals(PERIOD_CONTROL_ORG_ID, value.getValue());
  }

  /**
   * With no Organization controlling the Periods none of them can be closed, so no Period query is
   * worth running.
   */
  @Test
  public void noPeriodControlOrganizationYieldsNoPeriods() {
    when(osp.getPeriodControlAllowedOrganization(legalEntity)).thenReturn(null);

    final List<Period> result = ResetValuedStockAggregated.getClosedPeriodsToAggregate(new Date(),
        CLIENT_ID, REQUESTED_ORG_ID);

    assertTrue("no Period can be closed without an Organization controlling it", result.isEmpty());
    verify(dal, never()).createQuery(eq(Period.class), anyString());
  }
}
