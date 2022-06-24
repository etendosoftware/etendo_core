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
 * All portions are Copyright (C) 2013-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Utility class intended to be used in jUnit tests.
 * 
 * It allows to read/modify non visible properties.
 * 
 * @author alostale
 * 
 */
public class HiddenObjectHelper {
  /**
   * Initializes field in obj with its default constructor
   * 
   */
  public static void initializeField(Object obj, String fieldName) throws Exception {
    Field fld = getField(obj, fieldName);

    boolean originallyAccessible = fld.canAccess(obj);
    fld.setAccessible(true);

    Object o = get(obj, fieldName);
    for (Constructor<?> c : o.getClass().getConstructors()) {
      if (c.getParameterTypes().length == 0) {
        // default constructor found
        o = c.newInstance();
        break;
      }
    }

    fld.setAccessible(originallyAccessible);
  }

  /**
   * Gets fieldName in obj
   */
  public static Object get(Object obj, String fieldName) throws Exception {
    Field fld = getField(obj, fieldName);

    boolean originallyAccessible = fld.canAccess(obj);
    fld.setAccessible(true);

    Object o = fld.get(obj);
    fld.setAccessible(originallyAccessible);
    return o;
  }

  /**
   * Sets value to obj.fieldName field
   */
  public static void set(Object obj, String fieldName, Object value) throws Exception {
    Field fld = getField(obj, fieldName);
    boolean originallyAccessible = fld.canAccess(obj);
    fld.setAccessible(true);
    fld.set(obj, value);
    fld.setAccessible(originallyAccessible);
  }

  private static Field getField(Object obj, String fieldName) throws Exception {
    Class<? extends Object> clazz = obj.getClass();
    return clazz.getDeclaredField(fieldName);
  }

}
