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
 * All portions are Copyright (C) 2008-2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.exception;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.service.db.DbUtility;

/**
 * This is the base exception for all exceptions in Openbravo. It is an unchecked exception which
 * logs itself if {@code logException=true} is used in constructor or it's logger has at least debug
 * level.
 * 
 * @author mtaal
 */
public class OBException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private boolean logExceptionNeeded;

  public OBException() {
    this(false);
  }

  public OBException(boolean logException) {
    super();
    logExceptionNeeded = logException;
    log("Exception", this);
  }

  public OBException(String message, Throwable cause) {
    this(message, cause, false);
  }

  public OBException(String message, Throwable cause, boolean logException) {
    super(message, DbUtility.getUnderlyingSQLException(cause));
    logExceptionNeeded = logException;
    log(message, cause);
  }

  public OBException(String message) {
    this(message, false);
  }

  public OBException(String message, boolean logException) {
    super(message);
    logExceptionNeeded = logException;
    log(message, this);
  }

  public OBException(Throwable cause) {
    this(cause, false);
  }

  public OBException(Throwable cause, boolean logException) {
    super(cause);
    logExceptionNeeded = logException;
    log(null, cause);
  }

  private void log(String message, Throwable cause) {
    boolean shouldLog = isLogExceptionNeeded() || getLogger().isDebugEnabled();
    if (!shouldLog) {
      return;
    }

    Throwable foundCause = DbUtility.getUnderlyingSQLException(cause);

    String msg;
    if (StringUtils.isBlank(message)) {
      msg = foundCause == cause ? cause.getMessage()
          : (cause.getMessage() + "-" + foundCause.getMessage());
    } else {
      msg = message;
    }
    getLogger().error(msg, cause);
  }

  /**
   * This method returns a logger which can be used by a subclass. The logger is specific for the
   * instance of the Exception (the subclass).
   * 
   * @return the class-specific Logger
   */
  protected Logger getLogger() {
    return LogManager.getLogger(this.getClass());
  }

  /**
   * This method returns if log exception is needed.
   * 
   * @return the logExceptionNeeded
   */
  public boolean isLogExceptionNeeded() {
    return logExceptionNeeded;
  }
}
