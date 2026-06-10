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
 * All portions are Copyright (C) 2025 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.base;

import static org.junit.Assert.fail;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

import jakarta.enterprise.context.Dependent;

/**
 * Hibernate interceptor for tests that asserts no write operations (insert, update, delete) occur
 * during a session.
 */
@Dependent
public class NoWriteInterceptor implements Interceptor {

  private static final long serialVersionUID = 1L;

  @Override
  public boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames,
      Type[] types) {
    return false;
  }

  @Override
  public void onDelete(Object entity, Object id, Object[] state, String[] propertyNames,
      Type[] types) {
    fail();
  }

  @Override
  public boolean onFlushDirty(Object entity, Object id, Object[] currentState,
      Object[] previousState, String[] propertyNames, Type[] types) {
    fail();
    return false;
  }

  @Override
  public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames,
      Type[] types) {
    fail();
    return false;
  }
}
