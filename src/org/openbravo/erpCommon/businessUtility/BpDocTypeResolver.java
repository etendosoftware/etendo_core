package org.openbravo.erpCommon.businessUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.Query;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.DocumentType;

/**
 * Resolves the most appropriate {@code C_DocType} to assign when creating
 * new business documents (Orders, Invoices, Shipments)
 */
public final class BpDocTypeResolver {
  public static final String ORG_ZERO_ID = "0";
  private static final ThreadLocal<Map<Key, String>> CACHE = ThreadLocal.withInitial(HashMap::new);
  private static final String P_ORG = "orgId";
  private static final String P_ISSO = "isSO";
  private static final String P_DBT = "dbt";

  /**
   * Internal categorization of document families (order/invoice/shipment) used
   * to resolve Business Partner preferences independently of a specific DocBaseType.
   */
  protected enum Category {
    ORDER("ORD", "SOO", "POO"),
    INVOICE("INV", "ARI", "API"),
    SHIPMENT("SHIP", "MMS", "MMR");
    final String code;
    final String salesDBT;
    final String purchaseDBT;

    /**
     * Constructs a category with its mapping code and canonical sales/purchase DBTs.
     * @param code mapping code stored in {@code C_BPartner_DocType.DocumentCategory}
     * @param salesDBT canonical DocBaseType on the sales side
     * @param purchaseDBT canonical DocBaseType on the purchase side
     */
    Category(String code, String salesDBT, String purchaseDBT) {
      this.code = code;
      this.salesDBT = salesDBT;
      this.purchaseDBT = purchaseDBT;
    }

    /** @return the mapping code used for partner-level configuration (ORD/INV/SHIP) */
    String code() { return code; }

    /**
     * Returns the canonical DocBaseType for the category given the flow.
     * This is used mainly for organization-level fallbacks.
     * @param isSO {@code true} for sales, {@code false} for purchase
     * @return canonical DocBaseType (e.g., SOO or POO for orders)
     */
    String docBaseType(boolean isSO) { return isSO ? salesDBT : purchaseDBT; }

    /**
     * Maps a concrete DocBaseType (including return variants) to a category.
     * @param dbt a DocBaseType such as SOO, RSO, ARI, APC, MMS, RMR
     * @return the matching {@link Category} or {@code null} if unknown
     */
    static Category fromDocBaseType(String dbt) {
      if (StringUtils.isBlank(dbt)) return null;
      switch (dbt) {
        case "SOO": case "POO": case "RSO": case "RPO":
          return ORDER;
        case "ARI": case "API": case "ARC": case "APC":
          return INVOICE;
        case "MMS": case "MMR": case "RMS": case "RMR":
          return SHIPMENT;
        default:
          return null;
      }
    }

    /**
     * Resolves whether a DocBaseType belongs to the sales side.
     * @param dbt DocBaseType to check
     * @return {@code true} if sales (SOO/RSO, MMS/RMS, ARI/ARC), {@code false} otherwise
     */
    static boolean isSalesFlow(String dbt) {
      if (StringUtils.isBlank(dbt)) {
        return false;
      }
      switch (dbt) {
        case "SOO": case "RSO":
        case "MMS": case "RMS":
        case "ARI": case "ARC":
          return true;
        case "POO": case "RPO":
        case "MMR": case "RMR":
        case "API": case "APC":
          return false;
        default:
          return false;
      }
    }
  }

  /**
   * Resolves the most appropriate {@code C_DocType_ID} for the given context.
   * @param orgId the {@code AD_Org_ID} context (mandatory)
   * @param bpId the {@code C_BPartner_ID} (optional; may be {@code null} or blank)
   * @param docBaseType the explicit DocBaseType (e.g., SOO, RSO, ARI, APC, MMS, RMR)
   * @return the {@code C_DocType_ID} to assign, or {@code null} if it cannot be determined
   */
  public String resolveId(String orgId, String bpId, String docBaseType) {
    if (StringUtils.isAnyBlank(orgId, docBaseType)) {
      return null;
    }
    final boolean isSO = Category.isSalesFlow(docBaseType);
    final Category cat = Category.fromDocBaseType(docBaseType);
    if (cat == null) {
      return null;
    }
    final Key key = new Key(normalize(orgId), normalize(bpId), isSO, cat.code());
    final Map<Key, String> cache = CACHE.get();
    if (cache.containsKey(key)) {
      return cache.get(key);
    }
    String id = StringUtils.isNotBlank(bpId) ? findDocTypeForBp(bpId, orgId, isSO, cat.code()) : null;
    if (id == null && StringUtils.isNotBlank(bpId)) {
      id = findDocTypeForBp(bpId, ORG_ZERO_ID, isSO, cat.code());
    }
    if (id == null) {
      id = findDefaultDocType(orgId, isSO, cat, docBaseType);
    }
    cache.put(key, id);
    return id;
  }

  /**
   * Resolves and returns the {@link DocumentType} entity instead of the raw ID.
   * @param orgId the {@code AD_Org_ID} context (mandatory)
   * @param bpId the {@code C_BPartner_ID} (optional)
   * @param docBaseType the explicit DocBaseType to be used (e.g., ARI/APC, MMS/RMR)
   * @return the {@link DocumentType} or {@code null} if not resolvable
   */
  public DocumentType resolve(String orgId, String bpId, String docBaseType) {
    final String id = resolveId(orgId, bpId, docBaseType);
    return id == null ? null : OBDal.getInstance().get(DocumentType.class, id);
  }

  /**
   * Clears the per-thread cache. Useful between batch chunks or at the end of a request.
   */
  public static void clearCache() {
    CACHE.remove();
  }

  /**
   * Searches the Business Partner–specific mapping table ({@code C_BPartner_DocType})
   * for the closest applicable record given the organization context, category and flow.
   * @param bpId {@code C_BPartner_ID}
   * @param orgId {@code AD_Org_ID} used to evaluate org tree proximity
   * @param isSO {@code true} for sales flow, {@code false} for purchase flow
   * @param categoryCode category code (ORD/INV/SHIP)
   * @return {@code C_DocType_ID} or {@code null} if no partner mapping applies
   */
  protected String findDocTypeForBp(String bpId, String orgId, boolean isSO, String categoryCode) {
    final String sql =
      "select bpd.c_doctype_id " +
      "from c_bpartner_doctype bpd " +
      "where bpd.isactive = 'Y' " +
      "  and bpd.c_bpartner_id = :bpId " +
      "  and bpd.issotrx = :isSO " +
      "  and bpd.documentcategory = :cat " +
      "  and bpd.c_doctype_id is not null " +
      "  and AD_ISORGINCLUDED(:orgId, bpd.ad_org_id, bpd.ad_client_id) <> -1 " +
      "order by AD_ISORGINCLUDED(:orgId, bpd.ad_org_id, bpd.ad_client_id) asc";
    Query<?> q = OBDal.getInstance().getSession()
      .createNativeQuery(sql)
      .setParameter("bpId", bpId)
      .setParameter(P_ISSO, isSO ? "Y" : "N")
      .setParameter("cat", categoryCode)
      .setParameter(P_ORG, orgId)
      .setMaxResults(1);
    Object id = q.uniqueResult();
    return id == null ? null : id.toString();
  }

  /**
   * Finds the best organization-level doc type for the given category/flow and requested DocBaseType.
   * @param orgId {@code AD_Org_ID} of the context
   * @param isSO whether the flow is sales ({@code true}) or purchase ({@code false})
   * @param cat the document family (Order/Invoice/Shipment)
   * @param requestedDbt concrete DocBaseType being requested; if blank, category's canonical DBT is used
   * @return {@code C_DocType_ID} or {@code null} if none is found
   */
  protected String findDefaultDocType(String orgId, boolean isSO, Category cat, String requestedDbt) {
    final String dbt = StringUtils.isNotBlank(requestedDbt) ? requestedDbt : cat.docBaseType(isSO);
    String id = runDefaultDocTypeQuery(orgId, isSO, dbt);
    if (id != null) {
      return id;
    }
    return runFirstMatchingDocTypeQuery(orgId, isSO, dbt);
  }

  /**
   * Queries the organization's default doc type for the given DocBaseType and flow.
   * The result is constrained by {@code AD_ISORGINCLUDED} to the org tree.
   * @param orgId {@code AD_Org_ID}
   * @param isSO {@code true} for sales, {@code false} for purchase
   * @param docBaseType DocBaseType to filter by (e.g., SOO, ARI, MMS, ARC, RMR)
   * @return default {@code C_DocType_ID}, or {@code null} if none is marked as default
   */
  protected String runDefaultDocTypeQuery(String orgId, boolean isSO, String docBaseType) {
    final String sql =
      "select dt.c_doctype_id " +
      "from c_doctype dt " +
      "where dt.isactive = 'Y' " +
      "  and dt.isdefault = 'Y' " +
      "  and dt.issotrx  = :isSO " +
      "  and dt.docbasetype = :dbt " +
      "  and AD_ISORGINCLUDED(:orgId, dt.ad_org_id, dt.ad_client_id) <> -1 " +
      "order by AD_ISORGINCLUDED(:orgId, dt.ad_org_id, dt.ad_client_id) asc";
    Query<?> q = OBDal.getInstance().getSession()
      .createNativeQuery(sql)
      .setParameter(P_ISSO, isSO ? "Y" : "N")
      .setParameter(P_DBT, docBaseType)
      .setParameter(P_ORG, orgId)
      .setMaxResults(1);
    Object id = q.uniqueResult();
    return id == null ? null : id.toString();
  }

  /**
   * Returns the first active doc type that matches the flow and DocBaseType within
   * the organization tree, ordering by proximity, default flag, name, and ID.
   * @param orgId {@code AD_Org_ID} used to evaluate org tree proximity
   * @param isSO {@code true} for sales, {@code false} for purchase
   * @param docBaseType DocBaseType to match (e.g., SOO, API, RMS)
   * @return a {@code C_DocType_ID} or {@code null} if none matches
   */
  protected String runFirstMatchingDocTypeQuery(String orgId, boolean isSO, String docBaseType) {
    final String sql =
      "select dt.c_doctype_id " +
      "from c_doctype dt " +
      "where dt.isactive = 'Y' " +
      "  and dt.issotrx = :isSO " +
      "  and dt.docbasetype = :dbt " +
      "  and AD_ISORGINCLUDED(:orgId, dt.ad_org_id, dt.ad_client_id) <> -1 " +
      "order by " +
      "  AD_ISORGINCLUDED(:orgId, dt.ad_org_id, dt.ad_client_id) asc, " +
      "  coalesce(dt.isdefault,'N') desc, " +
      "  lower(trim(dt.name)) asc, " +
      "  dt.c_doctype_id asc";
    Query<?> q = OBDal.getInstance().getSession()
      .createNativeQuery(sql)
      .setParameter(P_ISSO, isSO ? "Y" : "N")
      .setParameter(P_DBT, docBaseType)
      .setParameter(P_ORG, orgId)
      .setMaxResults(1);
    Object id = q.uniqueResult();
    return id == null ? null : id.toString();
  }
  
  /**
   * Normalizes a nullable/blank string to a non-null key component for caching purposes.
   * @param s input string
   * @return {@code ""} if blank/null, otherwise the original string
   */
  protected static String normalize(String s) {
    return StringUtils.isBlank(s) ? "" : s;
  }

  /**
   * Cache key grouping the main lookup dimensions: organization, partner, flow and category.
   */
  protected static final class Key {
    final String orgId;
    final String bpId;
    final boolean isSO;
    final String categoryCode;

    /**
     * Constructs a new cache key.
     * @param orgId organization ID
     * @param bpId business partner ID (may be empty)
     * @param isSO sales/purchase flow flag
     * @param categoryCode category mapping code (ORD/INV/SHIP)
     */
    Key(String orgId, String bpId, boolean isSO, String categoryCode) {
      this.orgId = orgId;
      this.bpId = bpId;
      this.isSO = isSO;
      this.categoryCode = categoryCode;
    }

    @Override public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Key)) {
        return false;
      }
      Key k = (Key) o;
      return isSO == k.isSO
        && Objects.equals(orgId, k.orgId)
        && Objects.equals(bpId, k.bpId)
        && Objects.equals(categoryCode, k.categoryCode);
    }

    @Override public int hashCode() {
      return Objects.hash(orgId, bpId, isSO, categoryCode);
    }
  }
}
