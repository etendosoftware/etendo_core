/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.email;

import java.io.File;
import java.util.List;

/**
 * Classes implementing this interface will be listening to email events. In case the event is valid
 * for them ({@link EmailEventContentGenerator#isValidEvent(String, Object)} returns
 * <code>true</code>) they are in charge of generating the content for the email.
 * 
 * <code>event</code> in all methods is a <code>String</code> that identifies the event.
 * 
 * @author alostale
 * @see EmailEventManager
 * 
 */
public interface EmailEventContentGenerator {

  /**
   * Checks if an email should be generated for the <code>event</code>. Same class can be listening
   * to several events.
   * 
   * @param event
   *          Event to check
   * @param data
   *          Data the email will be generated for (this can also determine whether the email must
   *          be sent)
   * @return <code>true</code> if the email must be sent, <code>false</code> if not.
   */
  public boolean isValidEvent(String event, Object data);

  /**
   * Returns the email subject for the event and data.
   */
  public String getSubject(Object data, String event);

  /**
   * Returns the email body for the event and data.
   */
  public String getBody(Object data, String event);

  /**
   * Returns the type of content of the email. Tipically <code>"text/html; charset=utf-8"</code> or
   * <code>"text/plain; charset=utf-8"</code>
   */
  public String getContentType();

  /**
   * When there are several classes listening to the same event, the order the emails are sent is
   * based on this value.
   * 
   * @see EmailEventContentGenerator#preventsOthersExecution()
   */
  public int getPriority();

  /**
   * In case there are several classes listening to the same event, when this method returns
   * <code>true</code>, other emails with lower priority that might exist, will not be sent.
   * 
   */
  public boolean preventsOthersExecution();

  /**
   * Asynchronous emails are sent in a separate thread not waiting them to finish to continue the
   * rest of the execution flow.
   */
  public boolean isAsynchronous();

  /**
   * Returns the list of files to be attached to the email. Return null for no attachments
   */
  public List<File> getAttachments(Object data, String event);
}
