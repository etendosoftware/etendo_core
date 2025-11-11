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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.TestConstants;

/** Tests password hashing with different algorithms */
public class PasswordHashing extends OBBaseTest {

  private static final String SHA1_ADMIN = "0DPiKuNIrrVmD8IUCuw1hQxNqZc=";
  private static final String SHA512SALT_ADMIN = "1$H6E0K1Xx5nSb4h8y4YizSA$DozgBLpXFnxERX2++LhNVwC/w0dT8PObpkyKGpyuTtCWYMSfa+l6HfMk91PqhLzbYBr38JMILU4GA5FtAE6HoA==";

  @Test
  public void sha1IsAKnownAlgorithm() {
    assertThat(PasswordHash.getAlgorithm("whatever").getClass().getSimpleName(), is("SHA1"));
  }

  @Test
  public void sha512SaltIsAKnownAlgorithm() {
    assertThat(PasswordHash.getAlgorithm("1$salt$hash").getClass().getSimpleName(),
        is("SHA512Salt"));
  }

  @Test
  public void unknownAlgorithmsThrowException() {
    assertThrows(IllegalStateException.class, () -> PasswordHash.getAlgorithm("2$salt$hash"));
  }

  @Test
  public void oldHashesWork() {
    assertThat(PasswordHash.matches("admin", SHA1_ADMIN), is(true));
  }

  @Test
  public void newHashesWork() {
    assertThat(PasswordHash.matches("admin", SHA512SALT_ADMIN), is(true));
  }

  @Test
  public void saltPrventCollission() {
    assertThat("same password should generate different salted hashes",
        PasswordHash.generateHash("mySecret"), not(equalTo(PasswordHash.generateHash("mySecret"))));
  }

  @Test
  public void validUserNameAndPasswordReturnAUser() {
    Optional<User> user = PasswordHash.getUserWithPassword("admin", "admin");
    assertThat("Admin user is found", user.isPresent(), is(true));
  }

  @Test
  public void invalidPasswordDoesNotReturnAUser() {
    Optional<User> user = PasswordHash.getUserWithPassword("admin", "wrongPassword");
    assertThat("Admin user is found", user.isPresent(), is(false));
  }

  @Test
  public void invalidUserDoesNotReturnAUser() {
    Optional<User> user = PasswordHash.getUserWithPassword("wrongUser", "wrongPassword");
    assertThat("User is found", user.isPresent(), is(false));
  }

  @Test
  public void oldAlgorithmsGetPromoted() {
    setSystemAdministratorContext();

    // Given a user with a password hashed with old algorithm
    User obUser = OBDal.getInstance().get(User.class, TestConstants.Users.ADMIN);
    obUser.setPassword(SHA1_ADMIN);
    OBDal.getInstance().flush();

    // when credentials are checked first time
    Optional<User> user = PasswordHash.getUserWithPassword("admin", "admin");

    // then password gets promoted to new algorithm
    assertThat("password is promoted",
        PasswordHash.getAlgorithm(user.get().getPassword()).getClass().getSimpleName(),
        is("SHA512Salt"));
  }

  @Test
  public void newAlgorithmsRemainUntouched() {
    setSystemAdministratorContext();

    // Given a user with a password hashed with old algorithm
    User obUser = OBDal.getInstance().get(User.class, TestConstants.Users.ADMIN);
    obUser.setPassword(SHA512SALT_ADMIN);
    OBDal.getInstance().flush();

    // when credentials are checked first time
    Optional<User> user = PasswordHash.getUserWithPassword("admin", "admin");

    // then password gets promoted to new algorithm
    assertThat("password is not changed", user.get().getPassword(), is(SHA512SALT_ADMIN));
  }

  @Test
  public void oldPasswordsCanBeExpired() {
    try {
      setSystemAdministratorContext();

      // Given a user with an expired password hashed with old algorithm
      User obUser = OBDal.getInstance().get(User.class, TestConstants.Users.ADMIN);
      obUser.setPassword(SHA1_ADMIN);
      obUser.setPasswordExpired(true);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(obUser);
      Date lastPasswordUpdate = obUser.getLastPasswordUpdate();

      // when credentials are checked first time and password is automatically updated to new
      // algorithm
      Optional<User> opUser = PasswordHash.getUserWithPassword("admin", "admin");
      User user = opUser.get();
      OBDal.getInstance().refresh(user);

      // then password continues being expired
      assertThat("Last password update timestamp didn't change", user.getLastPasswordUpdate(),
          is(lastPasswordUpdate));
      assertThat("Password is expired", user.isPasswordExpired(), is(true));
    } finally {
      OBDal.getInstance().rollbackAndClose();
    }
  }
}
