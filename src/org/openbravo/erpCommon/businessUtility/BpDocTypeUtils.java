package org.openbravo.erpCommon.businessUtility;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartnerDocType;

/**
 * Utility methods to resolve the preferred Document Type (C_DocType) for a
 * given Business Partner within a specific Organization and flow (Sales/Purchase).
 */
public final class BpDocTypeUtils {
  public static final String ORG_ZERO_ID = "0";
  public static final String CATEGORY_ORDER = "ORD";
  public static final String CATEGORY_INVOICE = "INV";
  public static final String CATEGORY_SHIPMENT = "SHIP";
  public static final String ID = ".id";

  /**
   * Resolves the preferred <strong>Order</strong> document type for the given Business Partner.
   * @param bpId  Business Partner ID (C_BPartner_ID)
   * @param orgId Organization ID (AD_Org_ID)
   * @param isSO  {@code true} for Sales flow, {@code false} for Purchase flow
   * @return the C_DocType_ID to use for Orders, or {@code null} if not configured
   */
  public static String findOrderDocTypeForBp(String bpId, String orgId, boolean isSO) {
    return findWithFallback(bpId, orgId, isSO, CATEGORY_ORDER);
  }

  /**
   * Resolves the preferred <strong>Invoice</strong> document type for the given Business Partner.
   * @param bpId  Business Partner ID (C_BPartner_ID)
   * @param orgId Organization ID (AD_Org_ID)
   * @param isSO  {@code true} for Sales flow, {@code false} for Purchase flow
   * @return the C_DocType_ID to use for Invoices, or {@code null} if not configured
   */
  public static String findInvoiceDocTypeForBp(String bpId, String orgId, boolean isSO) {
    return findWithFallback(bpId, orgId, isSO, CATEGORY_INVOICE);
  }

  /**
   * Resolves the preferred <strong>Shipment</strong> document type for the given Business Partner.
   * @param bpId  Business Partner ID (C_BPartner_ID)
   * @param orgId Organization ID (AD_Org_ID)
   * @param isSO  {@code true} for Sales flow (Goods Shipment), {@code false} for Purchase flow (Material Receipt)
   * @return the C_DocType_ID to use for Shipments, or {@code null} if not configured
   */
  public static String findShipmentDocTypeForBp(String bpId, String orgId, boolean isSO) {
    return findWithFallback(bpId, orgId, isSO, CATEGORY_SHIPMENT);
  }

  /**
   * Generic resolver for any supported category. It first searches a record in
   * {@code C_BPartner_DocType} for the exact {@code orgId}. If not found, it tries again
   * with {@link #ORG_ZERO_ID} to apply a global fallback.
   * @param bpId Business Partner ID (C_BPartner_ID)
   * @param orgId Organization ID (AD_Org_ID)
   * @param isSO {@code true} for Sales flow, {@code false} for Purchase flow
   * @param category One of {@link #CATEGORY_ORDER}, {@link #CATEGORY_INVOICE}, {@link #CATEGORY_SHIPMENT}
   * @return the C_DocType_ID matching the criteria, or {@code null} if none is found
   */
  public static String findWithFallback(String bpId, String orgId, boolean isSO, String category) {
    if (StringUtils.isBlank(bpId) || StringUtils.isBlank(orgId) || StringUtils.isBlank(category)) {
      return null;
    }
    String id = findSingle(bpId, orgId, isSO, category);
    if (id != null) {
      return id;
    }
    return findSingle(bpId, ORG_ZERO_ID, isSO, category);
  }

  /**
   * Executes the actual DAL query against {@code C_BPartner_DocType} using the provided filters.
   * @param bpId Business Partner ID
   * @param orgId Organization ID
   * @param isSO Sales/Purchase flag
   * @param category Category code (ORD/INV/SHIP)
   * @return the C_DocType_ID, or {@code null} if not found
   */
  private static String findSingle(String bpId, String orgId, boolean isSO, String category) {
    OBCriteria<BusinessPartnerDocType> businessPartnerDocTypeCriteria = OBDal.getInstance()
      .createCriteria(BusinessPartnerDocType.class);
    businessPartnerDocTypeCriteria.add(Restrictions.eq(BusinessPartnerDocType.PROPERTY_BUSINESSPARTNER + ID, bpId));
    businessPartnerDocTypeCriteria.add(Restrictions.eq(BusinessPartnerDocType.PROPERTY_ORGANIZATION + ID, orgId));
    businessPartnerDocTypeCriteria.add(Restrictions.eq(BusinessPartnerDocType.PROPERTY_ISSOTRX, isSO));
    businessPartnerDocTypeCriteria.add(Restrictions.eq(BusinessPartnerDocType.PROPERTY_DOCUMENTCATEGORY, category));
    businessPartnerDocTypeCriteria.setMaxResults(1);
    BusinessPartnerDocType businessPartnerDocType = (BusinessPartnerDocType) businessPartnerDocTypeCriteria.uniqueResult();
    return (businessPartnerDocType != null && businessPartnerDocType.getDoctype() != null) ? businessPartnerDocType.getDoctype().getId() : null;
  }

  /**
   * Returns the organization-level default Order document type (C_DocType_ID) for the
   * given Organization and flow (Sales/Purchase).
   * @param orgId the Organization ID ({@code AD_Org_ID}) to resolve the default for; must be non-null
   * @param isSO {@code true} for sales flow (DocBaseType {@code SOO}); {@code false} for purchase flow (DocBaseType {@code POO})
   * @return the {@code C_DocType_ID} of the default Order document type, or {@code null} if none matches
   */
  public static String findDefaultOrderDocType(String orgId, boolean isSO) {
    final String docBaseType = isSO ? "SOO" : "POO";
    String sql =
      "select dt.c_doctype_id " +
        "from c_doctype dt " +
        "where dt.isactive='Y' " +
        " and dt.isdefault='Y' " +
        " and dt.isSotrx = :isSO " +
        " and dt.docbasetype = :dbt " +
        " and AD_ISORGINCLUDED(dt.ad_org_id, :orgId, dt.ad_client_id) <> -1 " +
        "order by AD_ISORGINCLUDED(dt.ad_org_id, :orgId, dt.ad_client_id) asc ";
    Query q = OBDal.getInstance().getSession()
      .createNativeQuery(sql)
      .setParameter("isSO", isSO ? "Y" : "N")
      .setParameter("dbt", docBaseType)
      .setParameter("orgId", orgId)
      .setMaxResults(1);

    Object id = q.uniqueResult();
    return id == null ? null : id.toString();
  }

}
