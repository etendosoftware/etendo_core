/*
 * Derived from OBContext, Copyright (C) 2008-2020 Openbravo SLU.
 * Licensed under the Openbravo Public License Version 1.1, available at
 * http://www.openbravo.com/legal/license.html. Distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND. The Original Code is Openbravo ERP.
 */
package org.openbravo.dal.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Shared readable-scope rules, independent of generated entities and persistence.
 *
 * <p>MODULE-BOUNDARY SEC-READ-SCOPE: destination platform-core. Legacy callers adapt
 * their metadata and tree providers; ERP and UI types must not enter this class.
 */
public final class ReadableScopeResolver {
  private ReadableScopeResolver() {
  }

  /** Returns the role client and shared system client, or only the system client. */
  public static String[] clients(String userLevel, Supplier<String> roleClient) {
    if (userLevel.equals("S")) {
      return new String[] { "0" };
    }
    String client = roleClient.get();
    return client.equals("0") ? new String[] { "0" } : new String[] { client, "0" };
  }

  /**
   * Resolves organization grants using the existing natural-tree semantics.
   * A root grant expands to the current client's organizations; shared root data
   * is readable even when there are no other grants.
   */
  public static String[] organizations(Collection<String> granted,
      Supplier<? extends Collection<String>> clientOrganizations,
      Function<String, ? extends Collection<String>> naturalTree) {
    Set<String> readable = new HashSet<>();
    Set<String> uniqueGrants = new HashSet<>(granted);
    if (uniqueGrants.contains("0")) {
      readable.addAll(clientOrganizations.get());
    } else {
      for (String organization : uniqueGrants) {
        readable.addAll(naturalTree.apply(organization));
      }
    }
    readable.add("0");
    return readable.toArray(new String[0]);
  }
}
