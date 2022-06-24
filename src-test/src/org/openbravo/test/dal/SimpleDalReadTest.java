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
 * All portions are Copyright (C) 2010-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.dal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.test.base.OBBaseTest;

/**
 * Contains one simple test case to test reading of in-memory model and a simple query action.
 * 
 * @author mtaal
 */

public class SimpleDalReadTest extends OBBaseTest {
  private static final Logger log = LogManager.getLogger();

  /**
   * Read a couple of BPCategories.
   */
  @Test
  public void testDoRead() {
    setSystemAdministratorContext();
    final OBQuery<Category> obQuery = OBDal.getInstance().createQuery(Category.class, "");
    for (Category category : obQuery.list()) {
      log.debug(category.getIdentifier());
    }
  }
}
