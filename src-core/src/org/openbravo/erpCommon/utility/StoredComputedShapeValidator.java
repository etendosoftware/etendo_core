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
package org.openbravo.erpCommon.utility; // NOSONAR - package name fixed by Etendo core convention

/**
 * Dependency-free shape predicate for stored computed column definitions (EPL-1807). This is the
 * single source of truth for the shape rule V1–V3, shared by both trees that need it without creating
 * an illegal dependency edge:
 *
 * <ul>
 * <li>the runtime DAL guard {@code org.openbravo.event.ColumnStoredComputedHandler} (compiled into the
 *     core webapp);</li>
 * <li>the build-time validator {@code org.openbravo.modulescript.StoredComputedValidator} (compiled
 *     separately under {@code src-util/modulescript/}), whose own {@code checkShape} delegates here.</li>
 * </ul>
 *
 * <p><b>Why this class lives in {@code src-core/} and MUST NOT be moved back to {@code src/}.</b> The
 * build-time consumer runs inside {@code GenerateStoredComputedTriggers} during
 * {@code update.database}, whose Ant {@code runtime-classpath}
 * ({@code src-db/database/build.xml}) carries {@code build/classes} but does <b>not</b> depend on any
 * target that populates it: {@code src/} is compiled by {@code compile.complete}, which runs
 * <i>after</i> {@code update.database} in the standard flow (pull &rarr; update.database &rarr;
 * smartbuild). A copy of this class under {@code src/} therefore resolves only on machines that happen
 * to hold a stale {@code build/classes} from an earlier compile, and throws
 * {@code NoClassDefFoundError} on every clean checkout. {@code src-core/} has no such hole: it is
 * packaged into {@code openbravo-core.jar} by the {@code core.lib} target, which
 * {@code compile.modulescript} declares as a dependency ({@code build.xml}, {@code depends="init,
 * core.lib"}), so it is guaranteed present before any modulescript exists. This is the same placement
 * that lets {@code org.openbravo.utils.FormatUtilities} and {@code org.openbravo.data.UtilSql} be used
 * from modulescripts today. The resulting split of package {@code org.openbravo.erpCommon.utility}
 * across two source trees is deliberate and mirrors the existing split of {@code org.openbravo.base}.</p>
 *
 * <p>It enforces only the <b>shape</b> subset of the catalogue; the full V-catalogue
 * (V4–V11, V14–V16 and the composite-PK target rule) lives in the modulescript
 * {@code StoredComputedValidator}, whose class javadoc holds the canonical Rule index.</p>
 *
 * <p><b>Validation checks performed here</b> (worded to match the Rule index in
 * {@code StoredComputedValidator} so the two never drift):</p>
 * <ul>
 *   <li><b>V1–V3</b> — shape of a {@code Computation_Mode='S'} column, all HARD under
 *       {@link #ETGO_STORED_COMPUTED_COL_DEF}: <b>V1</b> SQLLogic must be blank, <b>V2</b>
 *       Computation_Function must be set, <b>V3</b> Computation_Sequence_Number must be &gt; 0.</li>
 *   <li><b>V18</b> — Refresh Mode of a {@code Computation_Mode='S'} column, HARD under
 *       {@link #ETGO_STORED_COMPUTED_REFRESH_MODE} (see
 *       {@link #checkRefreshMode(String, String)}): {@code Refresh_Mode} must be one of {@code S}
 *       (Synchronous), {@code Q} (Queued) or {@code M} (Manual). Left null/blank,
 *       {@code GenerateStoredComputedTriggers} silently skips the column (warn only) and it never
 *       recomputes, so this rule rejects the misconfiguration at save time instead. Unlike V1–V3,
 *       V18 is runtime-only: it is enforced by {@code ColumnStoredComputedHandler} and is
 *       <b>not</b> part of the build-time {@code StoredComputedValidator} catalogue.</li>
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
  public static final String ETGO_STORED_COMPUTED_COL_DEF = "ETGO_StoredComputedColDef";

  /**
   * V18 refresh-mode rule message code — an {@code AD_MESSAGE} row rendered by the runtime DAL handler
   * ({@code ColumnStoredComputedHandler}). Runtime-only: there is no build-time counterpart.
   */
  public static final String ETGO_STORED_COMPUTED_REFRESH_MODE = "ETGO_StoredComputedRefreshMode";

  /** {@code AD_Column.Refresh_Mode} value: Synchronous. */
  private static final String REFRESH_SYNCHRONOUS = "S";
  /** {@code AD_Column.Refresh_Mode} value: Queued. */
  private static final String REFRESH_QUEUED = "Q";
  /** {@code AD_Column.Refresh_Mode} value: Manual. */
  private static final String REFRESH_MANUAL = "M";

  /**
   * Shape rule V1–V3, shared verbatim between the runtime DAL guard {@code ColumnStoredComputedHandler}
   * and the build-time {@code StoredComputedValidator}. Pure: only String/Long arguments, no DB, no DAL
   * types.
   *
   * <p>When {@code computationMode = 'S'} the column is recomputed by a database function, so
   * {@code sqlLogic} MUST be blank, {@code fn} MUST be set, and {@code seq} MUST be a positive number.
   * Returns {@link #ETGO_STORED_COMPUTED_COL_DEF} when any of the three is violated, otherwise
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
      return ETGO_STORED_COMPUTED_COL_DEF;
    }
    return null;
  }

  /**
   * V18 refresh-mode rule, runtime-only: enforced solely by the DAL guard
   * {@code ColumnStoredComputedHandler}, deliberately kept out of the build-time
   * {@code StoredComputedValidator} catalogue. Pure: only String arguments, no DB, no DAL types.
   *
   * <p>When {@code computationMode = 'S'} the column is recomputed by a database function that some
   * drain (synchronous, queued, or manually triggered) must invoke, so {@code refreshMode} MUST be one
   * of {@code S} (Synchronous), {@code Q} (Queued) or {@code M} (Manual) — null/blank/anything else is
   * a violation. Left unset, {@code GenerateStoredComputedTriggers} silently skips the column (warn
   * only) and it never recomputes; this rule rejects that misconfiguration at save time. Columns that
   * are not stored computed are always valid here.</p>
   *
   * @param computationMode
   *          {@code AD_Column.Computation_Mode}
   * @param refreshMode
   *          {@code AD_Column.Refresh_Mode}
   * @return {@link #ETGO_STORED_COMPUTED_REFRESH_MODE} when the column is stored computed and
   *         {@code refreshMode} is not one of {@code S}/{@code Q}/{@code M}, otherwise {@code null}
   */
  public static String checkRefreshMode(String computationMode, String refreshMode) {
    if (!STORED_COMPUTED.equals(computationMode)) {
      return null;
    }
    if (!isValidRefreshMode(refreshMode)) {
      return ETGO_STORED_COMPUTED_REFRESH_MODE;
    }
    return null;
  }

  private static boolean isValidRefreshMode(String refreshMode) {
    return REFRESH_SYNCHRONOUS.equals(refreshMode) || REFRESH_QUEUED.equals(refreshMode)
        || REFRESH_MANUAL.equals(refreshMode);
  }

  private static boolean isNotBlank(String s) {
    return s != null && !s.trim().isEmpty();
  }
}
