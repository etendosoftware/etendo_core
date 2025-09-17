package org.openbravo.erpCommon.businessUtility;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BusinessPartnerDocType;
import org.openbravo.model.common.enterprise.DocumentType;

/**
 * Utility methods to resolve the preferred Document Type (C_DocType)
 * for a given Business Partner within a specific Organization and flow (Sales/Purchase).
 */
public final class BpDocTypeUtils {

  private BpDocTypeUtils() {}

  public static final String ORG_ZERO_ID = "0";
  private static final String ID_SUFFIX = ".id";

  /** Supported categories for BP default doc type mapping. */
  public enum Category {
    ORDER("ORD", "SOO", "POO"),
    INVOICE("INV", "ARI", "API"),
    SHIPMENT("SHIP", "MMS", "MMR");

    private final String code;
    private final String salesDocBaseType;
    private final String purchaseDocBaseType;

    /**
     * Creates a new category with its associated code and document base types
     * for both sales and purchase transactions.
     * @param code the unique identifier for the category (e.g., "ORD", "INV", "SHIP")
     * @param salesDBT the document base type used for sales transactions
     * @param purchaseDBT the document base type used for purchase transactions
     */
    Category(String code, String salesDBT, String purchaseDBT) {
      this.code = code;
      this.salesDocBaseType = salesDBT;
      this.purchaseDocBaseType = purchaseDBT;
    }

    /**
     * Returns the unique code associated with the category.
     * @return the category code as a {@link String}
     */
    public String code() { 
      return code; 
    }

    /**
     * Returns the document base type associated with this category,
     * depending on whether it is a sales or purchase transaction.
     * @param isSO {@code true} if the document is a sales order/invoice/shipment;
     *             {@code false} if it is a purchase transaction
     * @return the document base type as a {@link String}
     */
    public String docBaseType(boolean isSO) { 
      return isSO ? salesDocBaseType : purchaseDocBaseType; 
    }
  }

  /**
   * Resolves the Business Partner–specific <strong>Order</strong> document type.
   * @param bpId  Business Partner ID ({@code C_BPartner_ID}); must not be blank.
   * @param orgId Organization ID ({@code AD_Org_ID}); must not be blank.
   * @param isSO  {@code true} for Sales flow, {@code false} for Purchase flow.
   * @return the resolved {@code C_DocType_ID} for Orders, or {@code null} if none is configured.
   */
  public static String findOrderDocTypeForBp(String bpId, String orgId, boolean isSO) {
    return findDocTypeForBp(bpId, orgId, isSO, Category.ORDER);
  }

  /**
   * Resolves the Business Partner–specific <strong>Invoice</strong> document type.
   * @param bpId  Business Partner ID ({@code C_BPartner_ID}); must not be blank.
   * @param orgId Organization ID ({@code AD_Org_ID}); must not be blank.
   * @param isSO  {@code true} for Sales (AR) flow, {@code false} for Purchase (AP) flow.
   * @return the resolved {@code C_DocType_ID} for Invoices, or {@code null} if none is configured.
   */
  public static String findInvoiceDocTypeForBp(String bpId, String orgId, boolean isSO) {
    return findDocTypeForBp(bpId, orgId, isSO, Category.INVOICE);
  }

  /**
   * Resolves the Business Partner–specific <strong>Shipment/Receipt</strong> document type.
   * @param bpId  Business Partner ID ({@code C_BPartner_ID}); must not be blank.
   * @param orgId Organization ID ({@code AD_Org_ID}); must not be blank.
   * @param isSO  {@code true} for Sales shipments, {@code false} for Purchase receipts.
   * @return the resolved {@code C_DocType_ID} for Shipments/Receipts, or {@code null} if none is configured.
   */
  public static String findShipmentDocTypeForBp(String bpId, String orgId, boolean isSO) {
    return findDocTypeForBp(bpId, orgId, isSO, Category.SHIPMENT);
  }

  /**
   * Finds the document type ID for a given Business Partner and Organization,
   * based on the specified category (Order, Invoice, or Shipment).
   * @param bpId the Business Partner ID
   * @param orgId the Organization ID
   * @param isSO {@code true} if the document type is for a sales transaction;
   *                 {@code false} for a purchase transaction
   * @param category the document type category (Order, Invoice, or Shipment)
   * @return the document type ID as a {@link String}, or {@code null} if none is found
   */
  public static String findDocTypeForBp(String bpId, String orgId, boolean isSO, Category category) {
    if (StringUtils.isAnyBlank(bpId, orgId) || category == null) {
      return null;
    }
    String id = runBpDocTypeQueryHierarchy(bpId, orgId, isSO, category.code());
    return (id != null) ? id : runBpDocTypeQueryHierarchy(bpId, ORG_ZERO_ID, isSO, category.code());
  }

  /**
   * Returns the organization-level default C_DocType_ID for a given category.
   * Under the hood it resolves the category's DocBaseType by flow (isSO) and queries C_DocType.
   * @param orgId AD_Org_ID
   * @param isSO true for sales flow, false for purchase flow
   * @param category {@link Category}
   * @return C_DocType_ID or null if none matches
   */
  public static String findDefaultDocType(String orgId, boolean isSO, Category category) {
    if (StringUtils.isBlank(orgId) || category == null) {
      return null;
    }
    return runDefaultDocTypeQuery(orgId, isSO, category.docBaseType(isSO));
  }

  /**
   * Runs a single native query to get the default org-level DocType by DocBaseType and flow.
   * Keeps AD_ISORGINCLUDED in filter & ordering to respect org trees.
   */
  private static String runDefaultDocTypeQuery(String orgId, boolean isSO, String docBaseType) {
    final String sql =
      "select dt.c_doctype_id " +
      "from c_doctype dt " +
      "where dt.isactive = 'Y' " +
      " and dt.isdefault = 'Y' " +
      " and dt.issotrx  = :isSO " +
      " and dt.docbasetype = :dbt " +
      " and AD_ISORGINCLUDED(dt.ad_org_id, :orgId, dt.ad_client_id) <> -1 " +
      "order by AD_ISORGINCLUDED(dt.ad_org_id, :orgId, dt.ad_client_id) asc";
    Query q = OBDal.getInstance().getSession()
      .createNativeQuery(sql)
      .setParameter("isSO", isSO ? "Y" : "N")
      .setParameter("dbt", docBaseType)
      .setParameter("orgId", orgId)
      .setMaxResults(1);
    Object id = q.uniqueResult();
    return id == null ? null : id.toString();
  }

  /**
   * Resolves the Business Partner–specific document type by walking the organization
   * hierarchy, using {@code AD_ISORGINCLUDED} to prefer the closest applicable record.
   * @param bpId Business Partner ID ({@code C_BPartner_ID}); must not be blank.
   * @param orgId Context Organization ID ({@code AD_Org_ID}) from which the hierarchy
   * resolution is performed; must not be blank.
   * @param isSO {@code true} for sales flow, {@code false} for purchase flow.
   * @param categoryCode Document category code to match (e.g., {@code "ORD"}, {@code "INV"}, {@code "SHIP"}).
   * @return The resolved {@code C_DocType_ID} as a string, or {@code null} if no applicable row is found.
   */
  private static String runBpDocTypeQueryHierarchy(String bpId, String orgId, boolean isSO, String categoryCode) {
    final String sql =
      "select bpd.c_doctype_id " +
      "  from c_bpartner_doctype bpd " +
      "  where bpd.isactive = 'Y' " +
      "    and bpd.c_bpartner_id = :bpId " +
      "    and bpd.issotrx = :isSO " +
      "    and bpd.documentcategory = :cat " +
      "    and bpd.c_doctype_id is not null " +
      "    and AD_ISORGINCLUDED(bpd.ad_org_id, :orgId, bpd.ad_client_id) <> -1 " +
      "  order by AD_ISORGINCLUDED(bpd.ad_org_id, :orgId, bpd.ad_client_id) asc";

    Query q = OBDal.getInstance().getSession()
      .createNativeQuery(sql)
      .setParameter("bpId", bpId)
      .setParameter("isSO", isSO ? "Y" : "N")
      .setParameter("cat", categoryCode)
      .setParameter("orgId", orgId)
      .setMaxResults(1);
    Object id = q.uniqueResult();
    return id == null ? null : id.toString();
  }

  /**
   * Resolves the most appropriate {@code C_DocType_ID} for the given context.
   * @param bpId Business Partner ID ({@code C_BPartner_ID}). May be {@code null} or blank; if so,
   *  the method skips BP-specific lookup and jumps to the organization default.
   * @param orgId Organization ID ({@code AD_Org_ID}). Must not be blank.
   * @param isSO {@code true} for sales flow, {@code false} for purchase flow.
   * @param category Document category to resolve (ORDER/INVOICE/SHIPMENT).
   * @return The resolved {@code C_DocType_ID}, or {@code null} if none can be determined.
   */
  public static String resolveDocTypeId(String bpId, String orgId, boolean isSO, Category category) {
    if (StringUtils.isBlank(orgId) || category == null) {
      return null;
    }
    String id = StringUtils.isNotBlank(bpId) ? findDocTypeForBp(bpId, orgId, isSO, category) : null;
    return id != null ? id : findDefaultDocType(orgId, isSO, category);
  }

  /**
   * Resolves and applies the Order document type to the UI in an Order window callout.
   * @param info The current {@link SimpleCallout.CalloutInfo} used to push values to the client.
   * @param orgId Organization ID ({@code AD_Org_ID}).
   * @param bpId Business Partner ID ({@code C_BPartner_ID}); may be {@code null}/blank.
   * @param isSO {@code true} for sales flow, {@code false} for purchase flow.
   * @param idField Name of the target field to receive the DocType ID (mandatory).
   * @param idRefField Name of the companion “_R” field to receive the DocType identifier (optional; pass {@code null} or blank to skip).
   * @return The applied {@code C_DocType_ID}, or {@code null} if resolution failed or the DocType could not be loaded.
   */
  public static String applyOrderDocType(SimpleCallout.CalloutInfo info, String orgId, String bpId, boolean isSO, String idField, String idRefField) {
    String id = resolveDocTypeId(bpId, orgId, isSO, Category.ORDER);
    if (StringUtils.isBlank(id)) {
      return null;
    }
    DocumentType dt = OBDal.getInstance().get(DocumentType.class, id);
    if (dt == null) {
      return null;
    }
    info.addResult(idField, dt.getId());
    if (StringUtils.isNotBlank(idRefField)) {
      info.addResult(idRefField, dt.getIdentifier());
    }
    if (StringUtils.isNotBlank(dt.getSOSubType())) {
      info.addResult("inpordertype", dt.getSOSubType());
    }
    return dt.getId();
  }

  /**
   * Resolves and applies the Invoice document type to the UI in an Invoice window callout.
   * @param info The current {@link SimpleCallout.CalloutInfo}.
   * @param orgId Organization ID ({@code AD_Org_ID}).
   * @param bpId Business Partner ID ({@code C_BPartner_ID}); may be {@code null}/blank.
   * @param isSO {@code true} for sales (AR) invoices, {@code false} for purchase (AP) invoices.
   * @param idField Name of the target field to receive the DocType ID (mandatory).
   * @param idRefField Name of the companion “_R” field to receive the DocType identifier (optional; pass {@code null} or blank to skip).
   * @return The applied {@code C_DocType_ID}, or {@code null} if resolution failed or the DocType could not be loaded.
   */
  public static String applyInvoiceDocType(SimpleCallout.CalloutInfo info, String orgId, String bpId, boolean isSO, String idField, String idRefField) {
    String id = resolveDocTypeId(bpId, orgId, isSO, Category.INVOICE);
    if (StringUtils.isBlank(id)) {
      return null;
    }
    DocumentType dt = OBDal.getInstance().get(DocumentType.class, id);
    if (dt == null) {
      return null;
    }
    info.addResult(idField, dt.getId());
    if (StringUtils.isNotBlank(idRefField)) {
      info.addResult(idRefField, dt.getIdentifier());
    }
    return dt.getId();
  }

  /**
   * Resolves and applies the Shipment/Receipt document type to the UI in a Shipment (In/Out) window callout.
   * @param info The current {@link SimpleCallout.CalloutInfo}.
   * @param orgId Organization ID ({@code AD_Org_ID}).
   * @param bpId Business Partner ID ({@code C_BPartner_ID}); may be {@code null}/blank.
   * @param isSO {@code true} for sales shipments (MMS), {@code false} for purchase receipts (MMR).
   * @param idField Name of the target field to receive the DocType ID (mandatory).
   * @param idRefField Name of the companion “_R” field to receive the DocType identifier (optional; pass {@code null} or blank to skip).
   * @return The applied {@code C_DocType_ID}, or {@code null} if resolution failed or the DocType could not be loaded.
   */
  public static String applyShipmentDocType(SimpleCallout.CalloutInfo info, String orgId, String bpId, boolean isSO, String idField, String idRefField) {
    String id = resolveDocTypeId(bpId, orgId, isSO, Category.SHIPMENT);
    if (StringUtils.isBlank(id)) {
      return null;
    }
    DocumentType dt = OBDal.getInstance().get(DocumentType.class, id);
    if (dt == null) {
      return null;
    }
    info.addResult(idField, dt.getId());
    if (StringUtils.isNotBlank(idRefField)) {
      info.addResult(idRefField, dt.getIdentifier());
    }
    info.addResult("inpmovementtype", isSO ? "C-" : "V+");
    return dt.getId();
  }
}
