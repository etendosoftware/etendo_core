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
 * new business documents (Orders, Invoices, Shipments).
 */
public final class BpDocTypeResolver {
  public static final String ORG_ZERO_ID = "0";
  /**
   * Per-thread cache to avoid repeated database lookups during a single request.
   * Cache key includes org, BP, sales/purchase side, document category and automation flag.
   */
  private static final ThreadLocal<Map<Key, String>> CACHE = ThreadLocal.withInitial(HashMap::new);
  private static final String P_ORG = "orgId";
  private static final String P_ISSO = "isSO";

  /**
   * Resolves the most appropriate {@code C_DocType_ID} for the given context.
   * @param orgId {@code AD_Org_ID} (mandatory, not blank)
   * @param bpId {@code C_BPartner_ID} (optional; may be {@code null} or blank)
   * @param docBaseType  explicit {@code DocBaseType} (e.g., {@code "SOO"}, {@code "RSO"},
   *   {@code "ARI"}, {@code "APC"}, {@code "MMS"}, {@code "RMR"}).
   *   Determines both category and sales/purchase side.
   * @param isAutomation whether the caller is an automatic process; when {@code true},
   *   BP-level lookup requires {@code isforceautomation = 'Y'}.
   * @return the resolved {@code C_DocType_ID}, or {@code null} if none can be determined
   */
  public String resolveId(String orgId, String bpId, String docBaseType, boolean isAutomation) {
    if (StringUtils.isAnyBlank(orgId, docBaseType)) {
      return null;
    }
    final boolean isSO = isSalesFlow(docBaseType);
    final BpDocTypeUtils.Category cat = categoryFromDocBaseType(docBaseType);
    if (cat == null) {
      return null;
    }
    final Key key = new Key(normalize(orgId), normalize(bpId), isSO, cat.code(), isAutomation);
    final Map<Key, String> cache = CACHE.get();
    if (cache.containsKey(key)) {
      return cache.get(key);
    }
    String id = StringUtils.isNotBlank(bpId)
      ? findDocTypeForBp(bpId, orgId, isSO, cat.code(), isAutomation)
      : null;
    if (id == null && StringUtils.isNotBlank(bpId)) {
      id = findDocTypeForBp(bpId, ORG_ZERO_ID, isSO, cat.code(), isAutomation);
    }
    if (id == null) {
      id = BpDocTypeUtils.findDefaultDocType(orgId, isSO, cat);
    }
    cache.put(key, id);
    return id;
  }

  /**
   * Resolves and loads the {@link DocumentType} entity for UI/consumer usage.
   * @param orgId {@code AD_Org_ID} (mandatory)
   * @param bpId {@code C_BPartner_ID} (optional; may be {@code null} or blank)
   * @param docBaseType explicit {@code DocBaseType} to resolve
   * @return the loaded {@link DocumentType} or {@code null} if not resolvable
   */
  public DocumentType resolve(String orgId, String bpId, String docBaseType) {
    final String id = resolveId(orgId, bpId, docBaseType, false);
    return id == null ? null : OBDal.getInstance().get(DocumentType.class, id);
  }

  /**
   * Clears the per-thread cache used by this resolver. Call this at the end of a request
   * or between batch chunks to avoid stale results leaking across logical operations
   * within the same thread.
   */
  public static void clearCache() {
    CACHE.remove();
  }

  /**
   * Queries the Business Partner–specific mapping table ({@code C_BPartner_DocType}) to
   * resolve a document type for the given category and flow within the organization tree.
   * @param bpId {@code C_BPartner_ID} to check
   * @param orgId {@code AD_Org_ID} that defines the starting point for org-tree proximity
   * @param isSO {@code true} for sales flow; {@code false} for purchase flow
   * @param categoryCode category code to match, e.g. {@code "ORD"}, {@code "INV"}, {@code "SHIP"}
   * @param isAutomation whether the caller is an automatic process (applies automation filter)
   * @return the matching {@code C_DocType_ID} or {@code null} if none applies
   */
  protected String findDocTypeForBp(String bpId, String orgId, boolean isSO, String categoryCode, boolean isAutomation) {
    final String sql =
      "select bpd.c_doctype_id " +
      "from c_bpartner_doctype bpd " +
      "where bpd.isactive = 'Y' " +
      "  and bpd.c_bpartner_id = :bpId " +
      "  and bpd.issotrx = :isSO " +
      "  and bpd.documentcategory = :cat " +
      "  and bpd.c_doctype_id is not null " +
      "  and ( :isAutomation = 'N' OR coalesce(bpd.isforceautomation,'N') = 'Y' ) " +
      "  and AD_ISORGINCLUDED(:orgId, bpd.ad_org_id, bpd.ad_client_id) <> -1 " +
      "order by AD_ISORGINCLUDED(:orgId, bpd.ad_org_id, bpd.ad_client_id) asc";
    Query<?> q = OBDal.getInstance().getSession()
      .createNativeQuery(sql)
      .setParameter("bpId", bpId)
      .setParameter(P_ISSO, isSO ? "Y" : "N")
      .setParameter("cat", categoryCode)
      .setParameter("isAutomation", isAutomation ? "Y" : "N")
      .setParameter(P_ORG, orgId)
      .setMaxResults(1);
    Object id = q.uniqueResult();
    return id == null ? null : id.toString();
  }

  /**
   * Maps a concrete {@code DocBaseType} (including return/credit variants) to the
   * corresponding {@link BpDocTypeUtils.Category}.
   * @param dbt a concrete {@code DocBaseType} string
   * @return the matching {@link BpDocTypeUtils.Category}, or {@code null} if unknown/blank
   */
  private static BpDocTypeUtils.Category categoryFromDocBaseType(String dbt) {
    if (StringUtils.isBlank(dbt)) return null;
    switch (dbt) {
      case "SOO": case "POO": case "RSO": case "RPO":
        return BpDocTypeUtils.Category.ORDER;
      case "ARI": case "API": case "ARC": case "APC":
        return BpDocTypeUtils.Category.INVOICE;
      case "MMS": case "MMR": case "RMS": case "RMR":
        return BpDocTypeUtils.Category.SHIPMENT;
      default:
        return null;
    }
  }

  /**
   * Determines whether a given {@code DocBaseType} belongs to the sales side.
   * @param dbt a concrete {@code DocBaseType} string
   * @return {@code true} if sales flow; {@code false} if purchase or unknown
   */
  private static boolean isSalesFlow(String dbt) {
    if (StringUtils.isBlank(dbt)) return false;
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

  /**
   * Normalizes a possibly null/blank string for use as part of a cache key.
   * @param s input string
   * @return {@code ""} when {@code s} is null/blank; otherwise {@code s}
   */
  private static String normalize(String s) {
    return StringUtils.isBlank(s) ? "" : s;
  }

  /**
   * Cache key grouping the primary lookup dimensions:
   * organization, Business Partner, sales/purchase side, document category and automation flag.
   */
  protected static final class Key {
    final String orgId;
    final String bpId;
    final boolean isSO;
    final String categoryCode;
    final boolean isAutomation;

    /**
     * Builds a new cache key.
     * @param orgId organization identifier (already normalized)
     * @param bpId business partner identifier (already normalized)
     * @param isSO {@code true} if sales flow; {@code false} if purchase flow
     * @param categoryCode category mapping code
     * @param isAutomation automation flag used for BP-level filtering
     */
    Key(String orgId, String bpId, boolean isSO, String categoryCode, boolean isAutomation) {
      this.orgId = orgId;
      this.bpId = bpId;
      this.isSO = isSO;
      this.categoryCode = categoryCode;
      this.isAutomation = isAutomation;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Key)) return false;
      Key k = (Key) o;
      return isSO == k.isSO
        && isAutomation == k.isAutomation
        && Objects.equals(orgId, k.orgId)
        && Objects.equals(bpId, k.bpId)
        && Objects.equals(categoryCode, k.categoryCode);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
      return Objects.hash(orgId, bpId, isSO, categoryCode, isAutomation);
    }
  }
}
