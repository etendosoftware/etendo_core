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
 * All portions are Copyright (C) 2017-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.utility;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;

/** Provides common utilities to handle {@code Collection}s as {@code String}s. */
public class StringCollectionUtils {
  private static final char COMMA = ',';
  private static final char QUOUTE = '\'';
  private static final String EMPTY_COLLECTION = "''";

  /**
   * Returns a {@code String} including all elements in the {@code Collection} received as parameter
   * separated by a comma and quoted with single quotes ({@code '}).
   *
   * @param col
   *          A {@code Collection} to be represented as comma separated {@code String}.
   * @return A {@code String} representation of {@code col}.
   */
  public static String commaSeparated(Collection<?> col) {
    return commaSeparated(col, true);
  }

  /** Equivalent to {@link #commaSeparated(Collection)} using an Array as parameter. */
  public static String commaSeparated(Object[] col) {
    return commaSeparated(Arrays.asList(col), true);
  }

  /**
   * Returns a {@code String} including all elements in the {@code Collection} received as parameter
   * separated by a comma.
   *
   * @param col
   *          A {@code Collection} to be represented as comma separated {@code String}.
   * @param addQuotes
   *          Should each value in the {@code Collection} be surrounded by single quotes {@code '}
   * @return A {@code String} representation of {@code col}.
   */
  public static String commaSeparated(Collection<?> col, boolean addQuotes) {
    Iterator<?> it = col.iterator();
    if (!it.hasNext()) {
      return "";
    }

    // typically used for lists of UUIDs, assuming it to calculate a proper initial capacity
    int initialCapacity = col.size() * (32 + (addQuotes ? 3 : 1));
    StringBuilder sb = new StringBuilder(initialCapacity);

    for (;;) {
      Object e = it.next();
      if (addQuotes) {
        sb.append(QUOUTE).append(e).append(QUOUTE);
      } else {
        sb.append(e);
      }
      if (!it.hasNext()) {
        return sb.toString();
      }
      sb.append(COMMA);
    }
  }

  /**
   * Checks if a comma separated representation of a collection represents an empty collection.
   *
   * @param collection
   *          A comma separated representation of a collection
   * @return {@code true} if the provided {@code String} represents an empty collection. Otherwise,
   *         return {@code false}.
   */
  public static boolean isEmptyCollection(String collection) {
    return StringUtils.isBlank(collection) || EMPTY_COLLECTION.equals(collection);
  }
}
