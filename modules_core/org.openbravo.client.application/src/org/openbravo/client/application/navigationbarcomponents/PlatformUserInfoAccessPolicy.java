/* Copyright (C) 2026 Etendo Software. Licensed under Apache License 2.0. */
package org.openbravo.client.application.navigationbarcomponents;

import jakarta.servlet.http.HttpSession;

/** Platform-only contribution; the shared component still queries authorized active backend roles. */
public final class PlatformUserInfoAccessPolicy extends UserInfoAccessPolicy {
  @Override
  public boolean isSystemAdministratorOnly(HttpSession session) {
    return false;
  }
}
