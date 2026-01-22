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
 * All portions are Copyright (C) 2015-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.weld.test.testinfrastructure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.test.event.ObserverBaseTest;
import org.openbravo.client.application.test.event.OrderLineTestObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.geography.Country;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.TestConstants;

/**
 * Persistance observers require of cdi. Test cases covering observers are executed when using
 * WeldBaseTest.
 * 
 * @author alostale
 *
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DalPersistanceEventTest extends ObserverBaseTest {

  private static int lastBeginCount = 0;
  private static boolean isFirstTest = true;

  @Test
  @Order(1)
  public void beginTrxObserversShouldBeExecutedOnFirstTest() {
    if (isFirstTest) {
      lastBeginCount = 0;
      isFirstTest = false;
    }

    int currentCount = OrderLineTestObserver.getNumberOfStartedTrxs();
    int increment = currentCount - lastBeginCount;

    assertThat("begin transaction observer executions should increment by 1",
        increment, is(1));

    lastBeginCount = currentCount;
  }

  @Test
  @Order(2)
  public void beginTrxObserversShouldBeExecutedOnSubsequentTests() {
    int currentCount = OrderLineTestObserver.getNumberOfStartedTrxs();
    int increment = currentCount - lastBeginCount;

    assertThat("begin transaction observer executions should increment by 1",
        increment, is(1));

    lastBeginCount = currentCount;
  }

  @Test
  @Order(3)
  public void endTrxObserversShouldBeExecuted() {
    int closedBefore = OrderLineTestObserver.getNumberOfClosedTrxs();

    OBDal.getInstance().commitAndClose();

    int closedAfter = OrderLineTestObserver.getNumberOfClosedTrxs();
    int increment = closedAfter - closedBefore;

    assertThat("end transaction observer should execute exactly once",
        increment, is(1));

  }

  @Test
  @Order(4)
  public void persistanceObserversShouldBeExecuted() {
    try {
      setSystemAdministratorContext();
      Country newCountry = OBProvider.getInstance().get(Country.class);
      newCountry.setName("Wonderland");
      newCountry.setISOCountryCode("WL");
      newCountry.setAddressPrintFormat("-");
      newCountry.setDateformat("invalid date format");

      OBException ex = assertThrows(OBException.class, () -> {
        checkCorrectValues(newCountry.getNumericmask(), newCountry.getDatetimeformat(),
            newCountry.getDateformat());
        OBDal.getInstance().save(newCountry);
        OBDal.getInstance().flush();
      });

      assertThat("Invalid date format error is propagated",
          ex.getMessage(),
          is(OBMessageUtils.messageBD("InvalidDateFormat")));
    } finally {
      OBDal.getInstance().rollbackAndClose();
    }
  }

  @Test
  @Order(5)
  @Issue("45341")
  public void persistenceObserversShouldBeExecutedOnModifiedCreatedBy() {
    try {
      setSystemAdministratorContext();
      Country newCountry = OBProvider.getInstance().get(Country.class);
      newCountry.setName("Wonderland");
      newCountry.setISOCountryCode("WL");
      newCountry.setAddressPrintFormat("-");

      User user = OBDal.getInstance().get(User.class, TestConstants.Users.ADMIN);
      newCountry.setCreatedBy(user);
      OBInterceptor.setPreventUpdateInfoChange(true);
      OBDal.getInstance().save(newCountry);
      OBDal.getInstance().flush();

      String createdById = newCountry.getCreatedBy().getId();
      String updatedById = newCountry.getUpdatedBy().getId();

      assertEquals(user.getId(), createdById,
          "createdBy value is not the one assigned manually.");
      assertEquals(OBContext.getOBContext().getUser().getId(), updatedById,
          "updatedBy value has not been assigned using OBContext user.");
      assertNotEquals(createdById, updatedById,
          "createdBy and updatedBy are equal, those should be different.");
    } finally {
      OBDal.getInstance().rollbackAndClose();
    }
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
