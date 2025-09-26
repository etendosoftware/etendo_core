package org.openbravo.erpCommon.businessUtility;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.enterprise.DocumentType;

/**
 * Unit tests for {@link BpDocTypeUtils}.
 * Verifies resolution logic for Business Partner overrides, organization defaults,
 * fallback ordering, and UI side effects in apply* methods.
 */
@RunWith(MockitoJUnitRunner.class)
public class BpDocTypeUtilsTest {

  private static final String BP = "BP";
  private static final String ORG = "ORG";
  private static final String DOCTYPE_ID = "c_doctype_id";
  private static final String DOCTYPE_ID_R = "c_doctype_id_R";
  private static final String ORDER_TYPE = "inpordertype";
  private static final String MOVEMENT_TYPE = "inpmovementtype";
  private static final String SO_SUBTYPE = "SO";
  private static final String MOV_CUSTOMER = "C-";
  private static final String MOV_VENDOR = "V+";
  private static final String DOC_ORG = "DT_ORG";
  private static final String DOC_CLIENT = "DT_CLIENT";
  private static final String DOC_DEF = "DT_DEF";
  private static final String DOC_FIRST = "DT_FIRST";
  private static final String DOC_BP = "DT_BP";
  private static final String DOC_9 = "DT9";
  private static final String DOC_INV = "DT_INV";
  private static final String DOC_SHIP = "DT_SHIP";
  private static final String DOC_REC = "DT_REC";
  private static final String IDF_DOC_9 = "Doc 9";
  private static final String IDF_INVOICE = "Invoice Doc";
  private static final String IDF_SHIP = "Ship Doc";
  private static final String IDF_RECEIPT = "Receipt Doc";

  @Mock private Session session;
  @Mock private OBDal obDal;
  @Mock private NativeQuery q1;
  @Mock private NativeQuery q2;
  @Mock private NativeQuery q3;
  @Mock private NativeQuery q4;

  @Mock private SimpleCallout.CalloutInfo info;
  @Mock private DocumentType docType;

  private MockedStatic<OBDal> obDalStatic;

  /**
   * Prepares static OBDal mocking and stubs Hibernate native queries used by the SUT.
   */
  @Before
  public void setUp() {
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
   * Closes the static OBDal mock to avoid leaking state across tests.
   */
  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
  }

  /**
   * Ensures BP-level doc type resolution prefers the closest org match.
   */
  @Test
  public void testFindDocTypeForBpPrefersOrgMatch() {
    when(q1.uniqueResult()).thenReturn(DOC_ORG);
    String id = BpDocTypeUtils.findDocTypeForBp(BP, ORG, true, BpDocTypeUtils.Category.ORDER);
    assertEquals(DOC_ORG, id);
  }

  /**
   * Ensures resolution falls back to client (org 0) when no org-specific row exists.
   */
  @Test
  public void testFindDocTypeForBpFallsBackToOrgZero() {
    when(q1.uniqueResult()).thenReturn(null);
    when(q2.uniqueResult()).thenReturn(DOC_CLIENT);
    String id = BpDocTypeUtils.findDocTypeForBp(BP, ORG, false, BpDocTypeUtils.Category.INVOICE);
    assertEquals(DOC_CLIENT, id);
  }

  /**
   * Returns null when inputs are blank or the category is null.
   */
  @Test
  public void testFindDocTypeForBpReturnsNullOnBlankInputs() {
    assertNull(BpDocTypeUtils.findDocTypeForBp("", ORG, true, BpDocTypeUtils.Category.SHIPMENT));
    assertNull(BpDocTypeUtils.findDocTypeForBp(BP, "", true, BpDocTypeUtils.Category.SHIPMENT));
    assertNull(BpDocTypeUtils.findDocTypeForBp(BP, ORG, true, null));
  }

  /**
   * Uses the org-level default DocType when present for the requested flow/category.
   */
  @Test
  public void testFindDefaultDocTypeUsesDefaultWhenPresent() {
    when(q1.uniqueResult()).thenReturn(DOC_DEF);
    String id = BpDocTypeUtils.findDefaultDocType(ORG, true, BpDocTypeUtils.Category.ORDER);
    assertEquals(DOC_DEF, id);
  }

  /**
   * Falls back to the first matching DocType (by hierarchy/name/default/id ordering) when no default exists.
   */
  @Test
  public void testFindDefaultDocTypeUsesFirstMatchWhenNoDefault() {
    when(q1.uniqueResult()).thenReturn(null);
    when(q2.uniqueResult()).thenReturn(DOC_FIRST);
    String id = BpDocTypeUtils.findDefaultDocType(ORG, false, BpDocTypeUtils.Category.INVOICE);
    assertEquals(DOC_FIRST, id);
  }

  /**
   * resolveDocTypeId prefers BP-specific configuration over org defaults.
   */
  @Test
  public void testResolveDocTypeIdPrefersBpOverDefault() {
    when(q1.uniqueResult()).thenReturn(DOC_BP);
    String id = BpDocTypeUtils.resolveDocTypeId(BP, ORG, true, BpDocTypeUtils.Category.SHIPMENT);
    assertEquals(DOC_BP, id);
  }

  /**
   * Verifies that when no BP-specific row is found (or BP is null/blank),
   * the resolver falls back to the organization-level default DocType.
   */
  @Test
  public void testResolveDocTypeIdUsesDefaultWhenNoBp() {
    when(q1.uniqueResult()).thenReturn(null);
    when(q2.uniqueResult()).thenReturn(DOC_DEF);
    String id = BpDocTypeUtils.resolveDocTypeId(null, ORG, true, BpDocTypeUtils.Category.ORDER);
    assertEquals(DOC_DEF, id);
  }

  /**
   * Ensures that applyOrderDocType pushes the DocType ID, identifier and the SO subtype
   * into the CalloutInfo when a resolvable Order DocType is found.
   */
  @Test
  public void testApplyOrderDocTypeSetsIdIdentifierAndSubtype() {
    when(q1.uniqueResult()).thenReturn(null);
    when(q2.uniqueResult()).thenReturn(null);
    when(q3.uniqueResult()).thenReturn(DOC_9);
    when(obDal.get(DocumentType.class, DOC_9)).thenReturn(docType);
    when(docType.getId()).thenReturn(DOC_9);
    when(docType.getIdentifier()).thenReturn(IDF_DOC_9);
    when(docType.getSOSubType()).thenReturn(SO_SUBTYPE);
    String result = BpDocTypeUtils.applyOrderDocType(info, ORG, BP, true, DOCTYPE_ID, DOCTYPE_ID_R);
    assertEquals(DOC_9, result);
    verify(info).addResult(DOCTYPE_ID, DOC_9);
    verify(info).addResult(DOCTYPE_ID_R, IDF_DOC_9);
    verify(info).addResult(ORDER_TYPE, SO_SUBTYPE);
  }

  /**
   * Ensures that applyInvoiceDocType pushes the DocType ID and identifier
   * into the CalloutInfo when a resolvable Invoice DocType is found.
   */
  @Test
  public void testApplyInvoiceDocTypeSetsIdAndIdentifier() {
    when(q1.uniqueResult()).thenReturn(null);
    when(q2.uniqueResult()).thenReturn(DOC_INV);
    when(obDal.get(DocumentType.class, DOC_INV)).thenReturn(docType);
    when(docType.getId()).thenReturn(DOC_INV);
    when(docType.getIdentifier()).thenReturn(IDF_INVOICE);
    String result = BpDocTypeUtils.applyInvoiceDocType(info, ORG, "", false, DOCTYPE_ID, DOCTYPE_ID_R);
    assertEquals(DOC_INV, result);
    verify(info).addResult(DOCTYPE_ID, DOC_INV);
    verify(info).addResult(DOCTYPE_ID_R, IDF_INVOICE);
  }

  /**
   * Ensures that applyShipmentDocType, for Sales flow, pushes the DocType ID and identifier
   * and sets the movement type to "C-" (customer shipment).
   */
  @Test
  public void testApplyShipmentDocTypeSetsMovementTypeSO() {
    when(q1.uniqueResult()).thenReturn(DOC_SHIP);
    when(obDal.get(DocumentType.class, DOC_SHIP)).thenReturn(docType);
    when(docType.getId()).thenReturn(DOC_SHIP);
    when(docType.getIdentifier()).thenReturn(IDF_SHIP);
    String result = BpDocTypeUtils.applyShipmentDocType(info, ORG, BP, true, DOCTYPE_ID, DOCTYPE_ID_R);
    assertEquals(DOC_SHIP, result);
    verify(info).addResult(DOCTYPE_ID, DOC_SHIP);
    verify(info).addResult(DOCTYPE_ID_R, IDF_SHIP);
    verify(info).addResult(MOVEMENT_TYPE, MOV_CUSTOMER);
  }

  /**
   * Ensures that applyShipmentDocType, for Purchase flow, pushes the DocType ID and identifier
   * and sets the movement type to "V+" (vendor receipt).
   */
  @Test
  public void testApplyShipmentDocTypeSetsMovementTypePO() {
    when(q1.uniqueResult()).thenReturn(DOC_REC);
    when(obDal.get(DocumentType.class, DOC_REC)).thenReturn(docType);
    when(docType.getId()).thenReturn(DOC_REC);
    when(docType.getIdentifier()).thenReturn(IDF_RECEIPT);
    String result = BpDocTypeUtils.applyShipmentDocType(info, ORG, BP, false, DOCTYPE_ID, DOCTYPE_ID_R);
    assertEquals(DOC_REC, result);
    verify(info).addResult(DOCTYPE_ID, DOC_REC);
    verify(info).addResult(DOCTYPE_ID_R, IDF_RECEIPT);
    verify(info).addResult(MOVEMENT_TYPE, MOV_VENDOR);
  }

  /**
   * Verifies that apply* methods return null when the DocType cannot be resolved
   * (no BP override nor default/first-match available) or cannot be loaded.
   */
  @Test
  public void testApplyMethodsReturnNullWhenUnresolvable() {
    when(q1.uniqueResult()).thenReturn(null);
    when(q2.uniqueResult()).thenReturn(null);
    String a = BpDocTypeUtils.applyOrderDocType(info, ORG, BP, true, "f1", "f1_R");
    assertNull(a);
  }
}
