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
 * The Initial Developer of the Original Code is Openbravo SL 
 * All portions are Copyright (C) 2010-2019 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.secureApp;

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;

/**
 * Utility class to manage user locking and time delays
 * 
 */
public class UserLock {
  private static Logger log4j = LogManager.getLogger();

  private static float delayInc;
  private static int delayMax;
  private static int lockAfterTrials;
  private static boolean lockingConfigured;

  private float delay;

  private String userName;
  private int numberOfFails;
  private User user;

  static {
    Properties obProp = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    String propInc = obProp.getProperty("login.trial.delay.increment", "0");
    String propMax = obProp.getProperty("login.trial.delay.max", "0");
    String propLock = obProp.getProperty("login.trial.user.lock", "0");
    if (propInc.equals("")) {
      propInc = "0";
    }
    if (propMax.equals("")) {
      propMax = "0";
    }
    if (propLock.equals("")) {
      propLock = "0";
    }

    try {
      delayInc = Float.parseFloat(propInc);
    } catch (NumberFormatException e) {
      log4j.error("Could not set login.trial.delay.increment property " + propInc, e);
      delayInc = 0;
    }
    try {
      delayMax = Integer.parseInt(propMax);
    } catch (NumberFormatException e) {
      log4j.error("Could not set login.trial.delay.max property " + propMax, e);
      delayMax = 0;
    }
    try {
      lockAfterTrials = Integer.parseInt(propLock);
    } catch (NumberFormatException e) {
      log4j.error("Could not set login.trial.user.lock property" + propMax, e);
      lockAfterTrials = 0;
    }
    lockingConfigured = delayInc != 0 || lockAfterTrials != 0;
  }

  public UserLock(String userName) {
    if (!isLockingConfigured()) {
      return;
    }

    this.userName = userName;
    setUser();

    // Count how many times this user has attempted to login without success
    long t = System.currentTimeMillis();

    // to improve performance this query is not done as subquery of the main one,
    // see issue #25466
    //@formatter:off
    String hql = 
            "select max(s1.creationDate) " +
            "  from ADSession s1 " +
            " where s1.username = :name " +
            "   and s1.loginStatus != 'F'";
    //@formatter:on
    Query<Date> q1 = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Date.class)
        .setParameter("name", userName);
    Date lastFailedAttempt = q1.list().get(0);

    if (lastFailedAttempt == null) {
      Calendar yesterday = Calendar.getInstance();
      yesterday.add(Calendar.DATE, -1);
      lastFailedAttempt = yesterday.getTime();
    }
    log4j.debug("Time taken to check user lock 1st query " + (System.currentTimeMillis() - t));

    long t1 = System.currentTimeMillis();
    //@formatter:off
    hql = 
            "select count(*) " +
            "  from ADSession s " +
            " where s.loginStatus = 'F' " +
            "   and s.username = :name " +
            "   and s.creationDate > :lastFail";
    //@formatter:on
    Query<Long> q = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Long.class)
        .setParameter("name", userName)
        .setParameter("lastFail", lastFailedAttempt);

    numberOfFails = q.list().get(0).intValue();
    log4j.debug("Time taken to check user lock " + (System.currentTimeMillis() - t)
        + "ms. Time of 2nd query " + (System.currentTimeMillis() - t1)
        + "ms. Number of failed login attempts: " + numberOfFails);

    if (numberOfFails == 0) {
      delay = 0;
      return;
    }

    delay = delayInc * numberOfFails;
    if (delayMax > 0 && delay > delayMax) {
      delay = delayMax;
    }

  }

  private void setUser() {
    OBContext.setAdminMode();
    try {
      OBCriteria<User> obCriteria = OBDal.getInstance().createCriteria(User.class);
      obCriteria.add(Restrictions.eq(User.PROPERTY_USERNAME, userName));
      obCriteria.setFilterOnReadableClients(false);
      obCriteria.setFilterOnReadableOrganization(false);

      user = (User) obCriteria.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * A new failed login attempt, increments the count of fails and blocks the user if needed
   */
  public void addFail() {
    if (!isLockingConfigured()) {
      return;
    }
    numberOfFails++;
    boolean lockUser = (lockAfterTrials != 0) && (numberOfFails >= lockAfterTrials);
    log4j.debug("lock: " + lockUser + " -lock after:" + lockAfterTrials + "- fails:" + numberOfFails
        + " - user:" + user);
    if (lockUser && user != null && !user.isLocked()) {
      try {
        OBContext.setAdminMode();

        // re-attach user to session as it was detached in commit and close
        user = OBDal.getInstance().get(User.class, user.getId());
        user.setLocked(true);
        OBDal.getInstance().flush();
        log4j.warn(userName + " is locked after " + numberOfFails + " failed logins.");
      } finally {
        OBContext.restorePreviousMode();
      }
    }
  }

  public boolean isLockedUser() {
    if (!isLockingConfigured()) {
      return false;
    }

    // User does not need to check org and client access
    OBContext.setAdminMode(false);
    try {
      return user != null && user.isLocked();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Delays the response of checking in case it is configured in Openbravo.properties
   * (login.trial.delay.increment and login.trial.delay.max), and the current username has login
   * attempts failed.
   */
  public void delayResponse() {
    if (delay == 0) {
      return;
    }
    // release DB connection while delaying response
    OBDal.getInstance().commitAndClose();
    log4j.debug("Delaying response " + delay + " seconds because of the previous login failed.");
    try {
      TimeUnit.MILLISECONDS.sleep((long) (delay * 1_000L));
    } catch (InterruptedException e) {
      log4j.error("Error delaying login response", e);
    }
  }

  /** Returns whether configuration is set to lock users */
  private boolean isLockingConfigured() {
    return lockingConfigured;
  }
}
