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
 * All portions are Copyright (C) 2008-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.dal.core.OBContext;

/**
 * The main purpose of the user context cache is to support session-less http requests without a
 * large performance hit. With a session-less http request every request needs to log in. This can
 * be comparatively heavy as for each request a new {@link OBContext} is created.
 * <p>
 * The user context cache takes care of storing a cache of user contexts (on user id) which are
 * re-used when a web-service call is done. Note that the OBContext which is cached can be re-used
 * by multiple threads at the same time.
 * 
 * @see OBContext
 * 
 * @author mtaal
 */

public class UserContextCache implements OBSingleton {

  private final long EXPIRES_IN = 1000 * 60 * 30;

  private static UserContextCache instance;

  private static final Logger log = LogManager.getLogger();

  public static synchronized UserContextCache getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(UserContextCache.class);
    }
    return instance;
  }

  public static synchronized void setInstance(UserContextCache instance) {
    UserContextCache.instance = instance;
  }

  private Map<String, CacheEntry> cache = new ConcurrentHashMap<String, CacheEntry>();

  private boolean invalidated = false;

  /**
   * Searches the ContextCache for an OBContext. If none is found a new one is created and placed in
   * the cache.
   * 
   * @param userId
   *          the user for which an OBContext is required
   * @return the OBContext object
   * 
   * @see OBContext
   */
  public OBContext getCreateOBContext(String userId) {
    return getCreateOBContext(userId, null, null);
  }

  /**
   * Searches the ContextCache for an OBContext. If none is found a new one is created and placed in
   * the cache.
   * 
   * @param userId
   *          the user for which an OBContext is required
   * @param roleId
   *          the role id of the user
   * @param orgId
   *          the org id of the user
   * @return the OBContext object
   * 
   * @see OBContext
   */
  public OBContext getCreateOBContext(String userId, String roleId, String orgId) {
    final String cacheKey = userId + (roleId != null ? roleId : "") + (orgId != null ? orgId : "");
    purgeCache();
    CacheEntry ce = cache.get(cacheKey);

    if (ce != null) {
      if (!userId.equals(ce.getObContext().getUser().getId())) {
        // check cached OBContext has the same userId than the one used as key, if not invalidate it
        // and get new one this can happen in case of: existent ws cache entry for a user and login
        // in app with same browser and different user at this point OBContext is reset to new user
        // but still cached to old one
        if (log.isDebugEnabled()) {
          log.debug(
              "Found element in cache for userId {}, but had incorrect user {}. Removing it from cache.",
              userId, ce.getObContext().getUser());
        }
        cache.remove(cacheKey);
      } else {
        if (log.isDebugEnabled()) {
          log.debug("Found element in cache. User: {}, Role: {}", ce.getObContext().getUser(),
              ce.getObContext().getRole());
        }
        return ce.getObContext();
      }
    }

    final OBContext obContext = new OBContext();
    obContext.initialize(userId, roleId, null, orgId);

    ce = new CacheEntry();
    ce.setObContext(obContext);
    ce.setUserId(userId);
    cache.put(cacheKey, ce);
    if (log.isDebugEnabled()) {
      log.debug("Created new cache entry.  User: {}, Role: {}", ce.getObContext().getUser(),
          ce.getObContext().getRole());
    }
    return obContext;
  }

  /** Invalidates {@link UserContextCache}. */
  public void invalidate() {
    if (!invalidated) {
      invalidated = true;
      log.debug("Invalidating user context cache");
    }
  }

  private void purgeCache() {
    if (invalidated) {
      cache = new ConcurrentHashMap<>();
      invalidated = false;
      return;
    }

    final List<CacheEntry> toRemove = new ArrayList<CacheEntry>();
    for (final CacheEntry ce : cache.values()) {
      if (ce.hasExpired()) {
        toRemove.add(ce);
      }
    }
    for (final CacheEntry ce : toRemove) {
      cache.remove(ce.getUserId());
    }
  }

  class CacheEntry {
    private OBContext obContext;
    private String userId;
    private long cacheCreationTime;

    public CacheEntry() {
      cacheCreationTime = System.currentTimeMillis();
    }

    public boolean hasExpired() {
      return cacheCreationTime < (System.currentTimeMillis() - EXPIRES_IN);
    }

    public OBContext getObContext() {
      return obContext;
    }

    public void setObContext(OBContext obContext) {
      this.obContext = obContext;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }
  }
}
