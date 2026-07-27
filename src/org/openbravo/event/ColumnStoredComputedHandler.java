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
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.StoredComputedShapeValidator;
import org.openbravo.model.ad.datamodel.Column;

/**
 * Enforces the definition invariants of a stored computed column (EPL-1807): when
 * {@code AD_Column.Computation_Mode = 'S'} the column is recomputed by a database function, not by
 * SQL logic, so:
 *
 * <ul>
 * <li>{@code SQLLogic} MUST be empty — a stored computed column derives its value from its
 * {@code Computation_Function}, never from an inline SQL default;</li>
 * <li>{@code Computation_Function} MUST be set — it is the function the recompute engine invokes;</li>
 * <li>{@code Computation_Sequence_Number} MUST be set (greater than zero) — it orders the recompute
 * of dependent stored columns;</li>
 * <li>{@code Refresh_Mode} MUST be set to one of Synchronous, Queued or Manual (V18) — left unset, the
 * recompute engine silently skips the column and it never recomputes. Unlike the first three, V18 is
 * enforced <b>only</b> here — it is deliberately not part of the build-time
 * {@code StoredComputedValidator} catalogue.</li>
 * </ul>
 *
 * <p>Any save that sets {@code Computation_Mode = 'S'} without satisfying all four is rejected with the
 * {@code ETGO_StoredComputedColDef} (V1–V3) or {@code ETGO_StoredComputedRefreshMode} (V18) message. The
 * UI also guides these fields via DisplayLogic, but this observer guarantees the invariant for every
 * save through the DAL (modern UI, web services, imports).</p>
 */
public class ColumnStoredComputedHandler extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(Column.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateStoredComputedDefinition((Column) event.getTargetInstance());
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateStoredComputedDefinition((Column) event.getTargetInstance());
  }

  /**
   * Validates the stored computed definition invariants. Does nothing when the column is not stored
   * computed ({@code Computation_Mode != 'S'}).
   *
   * @param column
   *          the {@link Column} being persisted
   * @throws OBException
   *           if the column is stored computed but has SQL logic set, is missing its computation
   *           function or computation sequence number (V1–V3), or has no valid refresh mode (V18)
   */
  private void validateStoredComputedDefinition(Column column) {
    String code = StoredComputedShapeValidator.checkShape(column.getComputationMode(),
        column.getSqllogic(), column.getComputationFunction(),
        column.getComputationSequenceNumber());
    if (code == null) {
      code = StoredComputedShapeValidator.checkRefreshMode(column.getComputationMode(),
          column.getRefreshMode());
    }
    if (code != null) {
      throw new OBException(OBMessageUtils.messageBD(code));
    }
  }
}
