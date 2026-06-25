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

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.onhandquantity.ColumnCompDependency;

/**
 * Enforces the exactly-one-of XOR rule between the two ways a stored computed column dependency can
 * locate its target row (EPL-1807, phase 3b):
 *
 * <ul>
 * <li>{@code Target_ID_Resolver_SQL} — a SQL expression returning the target record id, or</li>
 * <li>{@code Target_Link_Column_ID} — a foreign-key column of the source table that already points
 * at the target row.</li>
 * </ul>
 *
 * <p>Exactly one of the two MUST be provided. Supplying both is contradictory; supplying neither
 * leaves the dependency unable to resolve its target. Both cases are rejected with the
 * {@code ETGO_CompDepTargetXor} message. The UI also hides the inapplicable field via DisplayLogic,
 * but this observer guarantees the invariant for every save through the DAL (modern UI, web
 * services, imports).</p>
 */
public class ColumnCompDependencyTargetHandler extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(ColumnCompDependency.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateTargetXor((ColumnCompDependency) event.getTargetInstance());
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateTargetXor((ColumnCompDependency) event.getTargetInstance());
  }

  /**
   * Validates that exactly one of {@code targetIDResolverSQL} and {@code targetLinkColumn} is set.
   *
   * @param dependency
   *          the {@link ColumnCompDependency} being persisted
   * @throws OBException
   *           if both are set or neither is set
   */
  private void validateTargetXor(ColumnCompDependency dependency) {
    boolean hasResolverSql = StringUtils.isNotBlank(dependency.getTargetIDResolverSQL());
    boolean hasLinkColumn = dependency.getTargetLinkColumn() != null;
    if (hasResolverSql == hasLinkColumn) {
      throw new OBException(OBMessageUtils.messageBD("ETGO_CompDepTargetXor"));
    }
  }
}
