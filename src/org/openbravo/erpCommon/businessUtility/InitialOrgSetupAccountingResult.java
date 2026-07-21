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
package org.openbravo.erpCommon.businessUtility;

/**
 * Outcome returned by accounting setup handlers to report processing and failure state.
 */
public final class InitialOrgSetupAccountingResult {
  private final boolean handled;
  private final boolean success;
  private final String message;

  private InitialOrgSetupAccountingResult(boolean handled, boolean success, String message) {
    this.handled = handled;
    this.success = success;
    this.message = message;
  }

  /**
   * Creates a result for handlers that chose not to process the context.
   *
   * @return non-handled result
   */
  public static InitialOrgSetupAccountingResult notHandled() {
    return new InitialOrgSetupAccountingResult(false, true, null);
  }

  /**
   * Creates a result for handlers that completed accounting setup successfully.
   *
   * @return successful handled result
   */
  public static InitialOrgSetupAccountingResult success() {
    return new InitialOrgSetupAccountingResult(true, true, null);
  }

  /**
   * Creates a result for handlers that attempted setup but failed.
   *
   * @param message failure message to show in the setup result
   * @return failed handled result
   */
  public static InitialOrgSetupAccountingResult error(String message) {
    return new InitialOrgSetupAccountingResult(true, false, message);
  }

  /**
   * @return whether a handler processed the context
   */
  public boolean isHandled() {
    return handled;
  }

  /**
   * @return whether processing succeeded
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * @return failure message when processing did not succeed
   */
  public String getMessage() {
    return message;
  }
}
