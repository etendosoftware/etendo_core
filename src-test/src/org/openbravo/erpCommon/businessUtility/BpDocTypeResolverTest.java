package org.openbravo.erpCommon.businessUtility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
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
 * Unit tests for {@link BpDocTypeResolver}.
 * Verifies resolution order (BP -> Org 0 -> Org default -> first match),
 * handling of blank/unknown inputs, cache behavior, and entity lookups.
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
   * Initializes test fixtures and shared mocks before each test method runs.
   */
  @Before
  public void setUp() {
    resolver = new BpDocTypeResolver();
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
    when(obDal.getSession()).thenReturn(session);
    when(session.createNativeQuery(anyString())).thenReturn(q1, q2, q3, q4);
    lenient().when(q1.setParameter(anyString(), any())).thenReturn(q1);
    lenient().when(q1.setMaxResults(anyInt())).thenReturn(q1);
    lenient().when(q2.setParameter(anyString(), any())).thenReturn(q2);
    lenient().when(q2.setMaxResults(anyInt())).thenReturn(q2);
    lenient().when(q3.setParameter(anyString(), any())).thenReturn(q3);
    lenient().when(q3.setMaxResults(anyInt())).thenReturn(q3);
    lenient().when(q4.setParameter(anyString(), any())).thenReturn(q4);
    lenient().when(q4.setMaxResults(anyInt())).thenReturn(q4);
  }

  /**
   * Cleans up per-test state after each test method finishes.
   */
  @After
  public void tearDown() {
    BpDocTypeResolver.clearCache();
    if (obDalStatic != null) obDalStatic.close();
  }

  /**
   * Prefers BP-level mapping on the current organization branch.
   */
  @Test
  public void testResolveIdPrefersBpMappingOnOrg() {
    when(q1.uniqueResult()).thenReturn(DOC_BP);
    String id = resolver.resolveId(ORG, BP, SOO);
    assertEquals(DOC_BP, id);
    verify(session, times(1)).createNativeQuery(anyString());
  }

  /**
   * Falls back to BP-level mapping on Org 0 when org-specific mapping is not present.
   */
  @Test
  public void testResolveIdFallsBackToOrgZeroMapping() {
    when(q1.uniqueResult()).thenReturn(null);
    when(q2.uniqueResult()).thenReturn(DOC_CLIENT);
    String id = resolver.resolveId(ORG, BP, "API");
    assertEquals(DOC_CLIENT, id);
    verify(session, times(2)).createNativeQuery(anyString());
  }

  /**
   * Uses organization default when there is no BP (or no BP mapping found).
   */
  @Test
  public void testResolveIdUsesOrgDefaultWhenNoBp() {
    when(q1.uniqueResult()).thenReturn(DOC_DEF);
    String id = resolver.resolveId(ORG, null, ARI);
    assertEquals(DOC_DEF, id);
    verify(session, times(1)).createNativeQuery(anyString());
  }

  /**
   * Falls back to first matching doc type in org tree when no default exists.
   */
  @Test
  public void testResolveIdUsesFirstMatchWhenNoDefault() {
    when(q1.uniqueResult()).thenReturn(null);
    when(q2.uniqueResult()).thenReturn(DOC_FIRST);
    String id = resolver.resolveId(ORG, null, "MMS");
    assertEquals(DOC_FIRST, id);
    verify(session, times(2)).createNativeQuery(anyString());
  }

  /**
   * Returns null for blank org/DBT or unknown DocBaseType.
   */
  @Test
  public void testResolveIdReturnsNullOnBlankOrUnknown() {
    assertNull(resolver.resolveId("", BP, SOO));
    assertNull(resolver.resolveId(ORG, BP, ""));
    assertNull(resolver.resolveId(ORG, BP, "UNKNOWN_DBT"));
    verify(session, times(0)).createNativeQuery(anyString());
  }

  /**
   * resolve(...) returns the DocumentType entity when resolvable.
   */
  @Test
  public void testResolveReturnsEntity() {
    when(q1.uniqueResult()).thenReturn(DOC_DEF);
    when(obDal.get(DocumentType.class, DOC_DEF)).thenReturn(docType);
    DocumentType dt = resolver.resolve(ORG, null, "POO"); 
    assertNotNull(dt);
    assertEquals(docType, dt);
    verify(session, times(1)).createNativeQuery(anyString());
    verify(obDal, times(1)).get(DocumentType.class, DOC_DEF);
  }

  /**
   * Subsequent calls with the same key should hit the in-thread cache and avoid extra DB queries.
   */
  @Test
  public void testResolveIdUsesThreadCache() {
    when(q1.uniqueResult()).thenReturn(DOC_DEF);
    String id1 = resolver.resolveId(ORG, null, ARI);
    assertEquals(DOC_DEF, id1);
    String id2 = resolver.resolveId(ORG, null, ARI);
    assertEquals(DOC_DEF, id2);
    verify(session, times(1)).createNativeQuery(anyString());
    verifyNoInteractions(q2, q3, q4);
  }
}
