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
 * All portions are Copyright © 2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.database.ConnectionProvider;

/**
 * Callout for the Computation Dependency tab (EPL-1807, phase 3b) that auto-fills
 * {@code TARGET_LINK_COLUMN_ID} — the foreign-key column of the <em>source</em> table that points
 * at the <em>target</em> table (the table on which the stored-computed column lives).
 *
 * <p>Fires on both {@code SOURCE_TABLE_ID} and {@code AD_COLUMN_ID} changes (the latter fixes the
 * target table). It runs the FK-discovery routine and applies the precedence:</p>
 *
 * <ol>
 * <li>exactly one parent FK → use it;</li>
 * <li>else exactly one FK of any kind → use it;</li>
 * <li>else leave blank and surface a soft hint (many candidates / none).</li>
 * </ol>
 *
 * <p><b>Clobber rule:</b> an existing {@code TARGET_LINK_COLUMN_ID} is overwritten only when it is
 * blank or no longer belongs to the (new) source table — a still-valid deliberate override is never
 * silently replaced. When discovery does not yield a single column the existing value is preserved
 * unless it has become invalid.</p>
 */
public class SCD_TargetLinkColumn extends SimpleCallout {

  /**
   * Discovers FK columns on {@code sourceTableId} that reference {@code targetTableId}, covering the
   * three ways an Etendo FK column can name its target table: <i>TableDir</i> (reference 19,
   * implicit — the column name matches the target's key column), <i>Table</i> (reference 18, target
   * via {@code AD_REF_TABLE}) and <i>Search</i> (reference 30, target via {@code AD_REF_SEARCH}).
   * Ordered parent-first so the caller can apply the parent-preference precedence deterministically.
   */
  private static final String QUERY_FK_CANDIDATES =
      "SELECT c.ad_column_id, c.columnname, c.isparent "
    + "FROM   ad_column c "
    + "WHERE  c.ad_table_id = ? "
    + "AND    c.isactive = 'Y' "
    + "AND    ( "
    + "         c.ad_reference_id = '19' AND EXISTS ( "
    + "           SELECT 1 FROM ad_column k "
    + "           WHERE  k.ad_table_id = ? AND k.iskey = 'Y' "
    + "           AND    UPPER(k.columnname) = UPPER(c.columnname) ) "
    + "       OR "
    + "         c.ad_reference_id = '18' AND EXISTS ( "
    + "           SELECT 1 FROM ad_ref_table rt "
    + "           JOIN   ad_column k ON k.ad_column_id = rt.ad_key "
    + "           WHERE  rt.ad_reference_id = c.ad_reference_value_id "
    + "           AND    k.ad_table_id = ? ) "
    + "       OR "
    + "         c.ad_reference_id = '30' AND EXISTS ( "
    + "           SELECT 1 FROM ad_ref_search rs "
    + "           WHERE  rs.ad_reference_id = c.ad_reference_value_id "
    + "           AND    rs.ad_table_id = ? ) "
    + "       ) "
    + "ORDER  BY c.isparent DESC, c.columnname";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String sourceTableId = info.getStringParameter("inpsourceTableId", null);
    String targetColumnId = info.getStringParameter("inpadColumnId", null);
    String currentLinkColumnId = info.getStringParameter("inptargetLinkColumnId", null);

    if (StringUtils.isBlank(sourceTableId) || StringUtils.isBlank(targetColumnId)) {
      return;
    }

    OBContext.setAdminMode(true);
    try {
      Column targetColumn = OBDal.getInstance().get(Column.class, targetColumnId);
      if (targetColumn == null) {
        return;
      }
      String targetTableId = targetColumn.getTable().getId();

      if (!shouldClobber(currentLinkColumnId, sourceTableId)) {
        return;
      }

      List<FkCandidate> candidates = discoverFkCandidates(sourceTableId, targetTableId);
      applyPrecedence(info, candidates);
    } catch (Exception e) {
      throw new ServletException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * The current value is clobbered only when it is blank or no longer a column of the (new) source
   * table. A still-valid override is preserved.
   */
  private boolean shouldClobber(String currentLinkColumnId, String sourceTableId) {
    if (StringUtils.isBlank(currentLinkColumnId)) {
      return true;
    }
    Column current = OBDal.getInstance().get(Column.class, currentLinkColumnId);
    return current == null || !sourceTableId.equals(current.getTable().getId());
  }

  /**
   * Applies the discovery precedence and writes the result (or a soft hint).
   */
  private void applyPrecedence(CalloutInfo info, List<FkCandidate> candidates) {
    if (candidates.isEmpty()) {
      info.addResult("inptargetLinkColumnId", (Object) null);
      info.showMessage(OBMessageUtils.messageBD("ETGO_CompDepNoFk"));
      return;
    }

    List<FkCandidate> parents = new ArrayList<>();
    for (FkCandidate c : candidates) {
      if (c.isParent) {
        parents.add(c);
      }
    }

    if (parents.size() == 1) {
      info.addResult("inptargetLinkColumnId", parents.get(0).columnId);
    } else if (parents.isEmpty() && candidates.size() == 1) {
      info.addResult("inptargetLinkColumnId", candidates.get(0).columnId);
    } else {
      // ambiguous: several parents, or several non-parent FKs — leave blank, ask the user to pick.
      info.addResult("inptargetLinkColumnId", (Object) null);
      info.showMessage(OBMessageUtils.getI18NMessage("ETGO_CompDepManyFk",
          new String[] { Integer.toString(candidates.size()) }));
    }
  }

  /**
   * Runs the FK-discovery query (routine G) and returns the candidate columns parent-first.
   */
  private List<FkCandidate> discoverFkCandidates(String sourceTableId, String targetTableId)
      throws Exception {
    ConnectionProvider cp = new DalConnectionProvider(false);
    List<FkCandidate> out = new ArrayList<>();
    var ps = cp.getPreparedStatement(QUERY_FK_CANDIDATES);
    try {
      ps.setString(1, sourceTableId);
      ps.setString(2, targetTableId);
      ps.setString(3, targetTableId);
      ps.setString(4, targetTableId);
      var rs = ps.executeQuery();
      while (rs.next()) {
        FkCandidate fk = new FkCandidate();
        fk.columnId = rs.getString("ad_column_id");
        fk.isParent = "Y".equals(rs.getString("isparent"));
        out.add(fk);
      }
    } finally {
      cp.releasePreparedStatement(ps);
    }
    return out;
  }

  /** Lightweight FK candidate carrier. */
  private static class FkCandidate {
    private String columnId;
    private boolean isParent;
  }
}
