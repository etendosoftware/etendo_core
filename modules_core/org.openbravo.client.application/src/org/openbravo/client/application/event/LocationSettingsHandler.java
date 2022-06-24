/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2012-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.event;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.geography.Country;

/**
 * Action handler that checks if the location settings defined by the user contains legal values
 * 
 * 
 * @author openbravo
 */
class LocationSettingsHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Country.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    Country instanceCountry = (Country) event.getTargetInstance();
    checkCorrectValues(instanceCountry.getNumericmask(), instanceCountry.getDatetimeformat(),
        instanceCountry.getDateformat());
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    Country instanceCountry = (Country) event.getTargetInstance();
    checkCorrectValues(instanceCountry.getNumericmask(), instanceCountry.getDatetimeformat(),
        instanceCountry.getDateformat());
  }

  private void checkCorrectValues(String numericmask, String datetimeformat, String dateformat) {
    if (numericmask != null) {
      if (checkNumericMask(numericmask)) {
        try {
          new DecimalFormat(numericmask);
        } catch (IllegalArgumentException iaex) {
          throw new OBException(OBMessageUtils.messageBD("InvalidNumericMask"));
        }
      } else {
        throw new OBException(OBMessageUtils.messageBD("InvalidNumericMask"));
      }
    }
    try {
      if (datetimeformat != null) {
        new SimpleDateFormat(datetimeformat);
      }
    } catch (IllegalArgumentException iaex) {
      throw new OBException(OBMessageUtils.messageBD("InvalidDateTimeFormat"));
    }
    try {
      if (dateformat != null) {
        if (checkDateFormat(dateformat)) {
          new SimpleDateFormat(dateformat);
        } else {
          throw new OBException(OBMessageUtils.messageBD("InvalidDateFormat"));
        }
      }
    } catch (IllegalArgumentException iaex) {
      throw new OBException(OBMessageUtils.messageBD("InvalidDateFormat"));
    }
  }

  private boolean checkNumericMask(String numericmask) {
    return numericmask.matches("[#0\\.,]+");
  }

  private boolean checkDateFormat(String date) {
    return date.matches("[^aHkKhmsSzZ]+");
  }
}
