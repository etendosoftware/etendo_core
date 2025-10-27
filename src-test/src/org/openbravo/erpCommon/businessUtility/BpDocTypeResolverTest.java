package org.openbravo.erpCommon.businessUtility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.DocumentType;

/**
 * Unit test suite for {@link BpDocTypeResolver}.
 * Tests use Mockito to stub Hibernate {@link Session} and {@link NativeQuery} behavior and
 * to intercept static calls to {@link OBDal#getInstance()}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BpDocTypeResolverTest {

  private static final String BP = "BP";
  private static final String ORG = "ORG";
  private static final String DOC_BP = "DT_BP";
  private static final String DOC_CLIENT = "DT_CLIENT";
  private static final String DOC_DEF = "DT_DEF";
  private static final String DOC_FIRST = "DT_FIRST";
  private static final String SOO = "SOO";
  private static final String ARI = "ARI";

  @Mock private Session session;
  @Mock private OBDal obDal;
  @Mock private NativeQuery q1;   
  @Mock private NativeQuery q2;
  @Mock private NativeQuery q3;
  @Mock private NativeQuery q4;
  @Mock private DocumentType docType;

  private MockedStatic<OBDal> obDalStatic;
  private BpDocTypeResolver resolver;

  /**
   * Initializes the resolver and core mocks before each test.
   */
  @Before
  public void setUp() {
    resolver = new BpDocTypeResolver();
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
    when(obDal.getSession()).thenReturn(session);
    when(session.createNativeQuery(anyString())).thenReturn(q1, q2, q3, q4);
    lenientStub(q1); lenientStub(q2); lenientStub(q3); lenientStub(q4);
  }

  /**
   * Clears the resolver's thread-local cache and closes the static mock
   * for {@link OBDal} after each test.
   */
  @After
  public void tearDown() {
    BpDocTypeResolver.clearCache();
    if (obDalStatic != null) obDalStatic.close();
  }
  
  /**
   * Ensures resolution prefers a Business Partner mapping in the current
   * organization branch when available.
   */
  @Test
  public void prefersBpMappingOnOrg() {
    stubFirstQuery(DOC_BP);
    assertResolvedEquals(DOC_BP, ORG, BP, SOO, false, 1);
  }

  /**
   * Ensures resolution falls back to a Business Partner mapping on organization 0
   * (client level) when no org-specific mapping exists.
   */
  @Test
  public void fallsBackToOrgZero() {
    stubQueries(DOC_CLIENT, null);
    when(q1.uniqueResult()).thenReturn(null);
    String id = resolver.resolveId(ORG, BP, "API", false);
    assertEquals(DOC_CLIENT, id);
    verify(session, times(2)).createNativeQuery(anyString());
  }

  /**
   * Ensures the organization default document type is used when there is no BP
   * (or no BP mapping found).
   */
  @Test
  public void usesOrgDefaultWhenNoBp() {
    stubFirstQuery(DOC_DEF);
    assertResolvedEquals(DOC_DEF, ORG, null, ARI, false, 1);
  }

  /**
   * Ensures resolution falls back to the first matching document type in the organization tree
   * when no organization default is set.
   */
  @Test
  public void usesFirstMatchWhenNoDefault() {
    when(q1.uniqueResult()).thenReturn(null);
    when(q2.uniqueResult()).thenReturn(DOC_FIRST);
    String id = resolver.resolveId(ORG, null, "MMS", false);
    assertEquals(DOC_FIRST, id);
    verify(session, times(2)).createNativeQuery(anyString());
  }

  /**
   * Verifies that blank parameters and unknown {@code DocBaseType} values yield {@code null}
   * without issuing database queries.
   */
  @Test
  public void returnsNullOnBlankOrUnknown() {
    assertNull(resolver.resolveId("", BP, SOO, false));
    assertNull(resolver.resolveId(ORG, BP, "", false));
    assertNull(resolver.resolveId(ORG, BP, "UNKNOWN_DBT", false));
    verify(session, never()).createNativeQuery(anyString());
  }

  /**
   * Verifies that {@link BpDocTypeResolver#resolve(String, String, String)} loads and returns
   * a {@link DocumentType} entity when the identifier can be resolved.
   */
  @Test
  public void resolveReturnsEntity() {
    stubFirstQuery(DOC_DEF);
    when(obDal.get(DocumentType.class, DOC_DEF)).thenReturn(docType);
    DocumentType dt = resolver.resolve(ORG, null, "POO");
    assertNotNull(dt);
    assertEquals(docType, dt);
    verify(session, times(1)).createNativeQuery(anyString());
    verify(obDal, times(1)).get(DocumentType.class, DOC_DEF);
  }

  /**
   * Validates that repeated calls with the same key hit the in-thread cache and
   * therefore avoid issuing additional queries.
   */
  @Test
  public void usesThreadCache() {
    stubFirstQuery(DOC_DEF);
    String id1 = resolver.resolveId(ORG, null, ARI, false);
    String id2 = resolver.resolveId(ORG, null, ARI, false);
    assertEquals(DOC_DEF, id1);
    assertEquals(DOC_DEF, id2);
    verify(session, times(1)).createNativeQuery(anyString());
    verifyNoInteractions(q2, q3, q4);
  }
  
  /**
   * Applies lenient stubbing for fluent {@link NativeQuery} methods used across tests.
   * @param q the query mock to leniently stub
   */
  private static void lenientStub(NativeQuery q) {
    lenient().when(q.setParameter(anyString(), any())).thenReturn(q);
    lenient().when(q.setMaxResults(anyInt())).thenReturn(q);
  }

  /**
   * Stubs the first query (q1) to return the provided result from {@code uniqueResult()}.
   * @param result object to be returned by q1.uniqueResult()
   */
  private void stubFirstQuery(Object result) {
    when(q1.uniqueResult()).thenReturn(result);
  }

  /**
   * Stubs the first two queries (q1 and q2) to return the provided results from
   * {@code uniqueResult()} in order.
   * @param q2Result the result to be returned by q2.uniqueResult()
   * @param q1Result the result to be returned by q1.uniqueResult()
   */
  private void stubQueries(Object q2Result, Object q1Result) {
    when(q1.uniqueResult()).thenReturn(q1Result);
    when(q2.uniqueResult()).thenReturn(q2Result);
  }

  /**
   * Resolves an ID using the provided inputs and asserts the expected outcome.
   * Also verifies the number of times a native query was created (rough proxy
   * for how many resolution phases were executed).
   * @param expected expected resolved document type ID
   * @param org organization identifier
   * @param bp business partner identifier (nullable)
   * @param dbt DocBaseType to resolve
   * @param auto automation flag passed to the resolver
   * @param expectedQueries  expected number of {@code createNativeQuery} calls
   */
  private void assertResolvedEquals(
    String expected, String org, String bp, String dbt, boolean auto, int expectedQueries) {
    String id = resolver.resolveId(org, bp, dbt, auto);
    assertEquals(expected, id);
    verify(session, times(expectedQueries)).createNativeQuery(anyString());
  }
}
