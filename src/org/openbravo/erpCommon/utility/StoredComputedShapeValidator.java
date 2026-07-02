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
package org.openbravo.erpCommon.utility;

/**
 * Dependency-free shape predicate for stored computed column definitions (EPL-1807). This is the
 * single source of truth for the shape rule V1–V3, living in core {@code src/} so it can be shared by
 * both trees that need it without creating an illegal dependency edge:
 *
 * <ul>
 * <li>the runtime DAL guard {@code org.openbravo.event.ColumnStoredComputedHandler} (compiled into the
 *     core webapp);</li>
 * <li>the build-time validator {@code org.openbravo.modulescript.StoredComputedValidator} (compiled
 *     separately under {@code src-util/modulescript/}), whose own {@code checkShape} delegates here.</li>
 * </ul>
 *
 * <p>The class is intentionally kept free of any DB, Ant or DAL types (only {@code java.*}) so it is
 * callable from every layer.</p>
 */
public final class StoredComputedShapeValidator {

  private StoredComputedShapeValidator() {
  }

  /** {@code AD_Column.Computation_Mode} value marking a stored computed column. */
  private static final String STORED_COMPUTED = "S";

  /**
   * V1–V3 shape rule message code — an {@code AD_MESSAGE} row rendered by the runtime DAL handler and
   * reused as a label by the build-time validator.
   */
  public static final String ETGO_StoredComputedColDef = "ETGO_StoredComputedColDef";

  /**
   * Shape rule V1–V3, shared verbatim between the runtime DAL guard {@code ColumnStoredComputedHandler}
   * and the build-time {@code StoredComputedValidator}. Pure: only String/Long arguments, no DB, no DAL
   * types.
   *
   * <p>When {@code computationMode = 'S'} the column is recomputed by a database function, so
   * {@code sqlLogic} MUST be blank, {@code fn} MUST be set, and {@code seq} MUST be a positive number.
   * Returns {@link #ETGO_StoredComputedColDef} when any of the three is violated, otherwise
   * {@code null}. Columns that are not stored computed are always valid here.</p>
   *
   * @param computationMode
   *          {@code AD_Column.Computation_Mode}
   * @param sqlLogic
   *          {@code AD_Column.SQLLogic}
   * @param fn
   *          {@code AD_Column.Computation_Function}
   * @param seq
   *          {@code AD_Column.Computation_Sequence_Number}
   * @return the violation code, or {@code null} when the shape is valid
   */
  public static String checkShape(String computationMode, String sqlLogic, String fn, Long seq) {
    if (!STORED_COMPUTED.equals(computationMode)) {
      return null;
    }
    boolean hasSqlLogic = isNotBlank(sqlLogic);
    boolean hasFunction = isNotBlank(fn);
    boolean hasSequence = seq != null && seq > 0;
    if (hasSqlLogic || !hasFunction || !hasSequence) {
      return ETGO_StoredComputedColDef;
    }
    return null;
  }

  private static boolean isNotBlank(String s) {
    return s != null && !s.trim().isEmpty();
  }
}
