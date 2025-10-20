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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):
 *************************************************************************
 */

package org.openbravo.erpCommon.businessUtility;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * This class allows PriceAdjustment discounts to be extended from external modules and to be
 * correctly calculated on the callouts that show the price before the booking process.
 * 
 * @author aaroncalero
 * 
 */

@ApplicationScoped
public abstract class PriceAdjustmentHqlExtension {

  /**
   * Returns a String that will be used to extend the hql query string of {@link PriceAdjustment}
   * 
   */
  public abstract String getHQLStringExtension();
}
