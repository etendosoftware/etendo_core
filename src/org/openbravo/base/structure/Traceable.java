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
 * All portions are Copyright (C) 2008-2010 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.structure;

import java.util.Date;

import org.openbravo.model.ad.access.User;

/**
 * An interface modeling open bravo objects which have audit info fields such as created, createdBy,
 * etc.
 * 
 * @author mtaal
 */

public interface Traceable {

  /**
   * Created by audit user
   * 
   * @return User
   */
  public User getCreatedBy();

  /**
   * Created by audit user
   * 
   * @param user
   */
  public void setCreatedBy(User user);

  /**
   * Creation date of audit
   * 
   * @return Date of creation
   */
  public Date getCreationDate();

  /**
   * Creation date of audit
   * 
   * @param date
   */
  public void setCreationDate(Date date);

  /**
   * Update by audit user
   * 
   * @return User who updated
   */
  public User getUpdatedBy();

  /**
   * Update by audit user
   * 
   * @param user
   */
  public void setUpdatedBy(User user);

  /**
   * Update date of audit
   * 
   * @return Date of update
   */
  public Date getUpdated();

  /**
   * Update date of audit
   * 
   * @param date
   */
  public void setUpdated(Date date);
}
