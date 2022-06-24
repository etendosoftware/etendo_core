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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.authentication.hashing;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;

/**
 * Handles hashing passwords to be stored in database supporting different
 * {@link HashingAlgorithm}s.
 *
 * @since 3.0PR20Q1
 */
public class PasswordHash {
  private static final Logger log = LogManager.getLogger();
  private static final int DEFAULT_CURRENT_ALGORITHM_VERSION = 1;

  private static final Map<Integer, HashingAlgorithm> ALGORITHMS;
  static {
    ALGORITHMS = new HashMap<>(2);
    ALGORITHMS.put(0, new SHA1());
    ALGORITHMS.put(1, new SHA512Salt());
  }

  private PasswordHash() {
  }

  /**
   * Checks if userName matches password, returning an {@link Optional} {@link User} in case it
   * matches.
   * <p>
   * <b>Important Note</b>: In case password matches with the current one for the user and it was
   * hashed with a {@link HashingAlgorithm} with a version lower than current default, hash will be
   * promoted to the default algorithm. In this case, user's password field will be updated and DAL
   * current transaction will be flushed to DB.
   * 
   * @param userName
   *          user name to check
   * @param password
   *          user's password in plain text as provided by the user
   * @return an {@code Optional} describing the {@code User} matching the provided {@code userName}
   *         and {@code password} pair; or an empty {@code Optional} if there is no {@code User}
   *         matching them
   */
  public static Optional<User> getUserWithPassword(String userName, String password) {
    OBContext.setAdminMode(false);
    try {
      User user = (User) OBDal.getInstance()
          .createCriteria(User.class)
          .add(Restrictions.eq(User.PROPERTY_USERNAME, userName))
          .setFilterOnActive(true)
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false)
          .uniqueResult();

      if (user == null || user.getPassword() == null) {
        // no user for given userName
        return Optional.empty();
      }

      HashingAlgorithm algorithm = getAlgorithm(user.getPassword());

      if (!algorithm.check(password, user.getPassword())) {
        // invalid password
        return Optional.empty();
      }

      if (algorithm.getAlgorithmVersion() < DEFAULT_CURRENT_ALGORITHM_VERSION
          && !user.isPasswordExpired()) {
        log.debug("Upgrading password hash for user {}, from algorithm version {} to {}.",
            user.getUsername(), algorithm.getAlgorithmVersion(), DEFAULT_CURRENT_ALGORITHM_VERSION);
        String newPassword = ALGORITHMS.get(DEFAULT_CURRENT_ALGORITHM_VERSION)
            .generateHash(password);
        user.setPassword(newPassword);
        OBDal.getInstance().flush();
      }
      return Optional.of(user);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /** Generates a hash for the {@code plainText} using current default {@link HashingAlgorithm} */
  public static String generateHash(String plainText) {
    return ALGORITHMS.get(DEFAULT_CURRENT_ALGORITHM_VERSION).generateHash(plainText);
  }

  /** Checks whether a plain text password matches with a hashed password */
  public static boolean matches(String plainTextPassword, String hashedPassword) {
    HashingAlgorithm algorithm = getAlgorithm(hashedPassword);
    log.trace("Checking password with algorithm {}", () -> algorithm.getClass().getSimpleName());
    return algorithm.check(plainTextPassword, hashedPassword);
  }

  /** Determines the algorithm used to hash a given password. */
  static HashingAlgorithm getAlgorithm(String hash) {
    HashingAlgorithm algorithm = ALGORITHMS.get(getVersion(hash));

    if (algorithm == null) {
      throw new IllegalStateException(
          "Hashing algorithm version " + getVersion(hash) + " is not implemented");
    }

    return algorithm;
  }

  private static int getVersion(String hash) {
    int idx = hash.indexOf('$');
    if (idx == -1) {
      return 0;
    }
    return Integer.parseInt(hash.substring(0, idx));
  }
}
