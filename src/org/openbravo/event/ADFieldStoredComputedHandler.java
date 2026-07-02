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

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;

/**
 * Forces {@code AD_Field.ISREADONLY = 'Y'} for every field backed by a stored computed column
 * ({@code AD_Column.Computation_Mode = 'S'}).
 *
 * <p>Stored computed columns (EPL-1807) are recalculated by the database and are non-updatable at
 * the ORM layer (see {@code DalMappingGenerator}). Allowing their fields to be edited in the UI
 * would be misleading, so any programmatic or UI save of an {@code AD_Field} whose column is
 * stored-computed is coerced to read-only here. This complements the install-time SQL step
 * ({@code EnforceStoredComputedReadOnly}), which fixes pre-existing metadata, by covering every
 * subsequent save through the DAL (modern UI, web services, imports).</p>
 */
public class ADFieldStoredComputedHandler extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(Field.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    enforceReadOnly(event);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    enforceReadOnly(event);
  }

  /**
   * Sets the field read-only when its column is a stored computed column.
   *
   * <p>Following the {@link EntityPersistenceEventObserver} contract, the persisted values come from
   * the event's current state; setters on the target instance are ignored for the ongoing save.
   * Therefore the read-only flag is written through {@link EntityPersistenceEvent#setCurrentState}.</p>
   *
   * @param event
   *          the persistence event for the {@link Field} being saved
   */
  private void enforceReadOnly(EntityPersistenceEvent event) {
    final Column column = (Column) event.getCurrentState(getColumnProperty());
    if (column != null && "S".equals(column.getComputationMode())
        && !Boolean.TRUE.equals(event.getCurrentState(getReadOnlyProperty()))) {
      event.setCurrentState(getReadOnlyProperty(), true);
    }
  }

  private Property getColumnProperty() {
    return ENTITIES[0].getProperty(Field.PROPERTY_COLUMN);
  }

  private Property getReadOnlyProperty() {
    return ENTITIES[0].getProperty(Field.PROPERTY_READONLY);
  }
}
