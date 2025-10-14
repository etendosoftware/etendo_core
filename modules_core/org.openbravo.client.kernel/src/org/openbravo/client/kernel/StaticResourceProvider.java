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
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

import com.etendoerp.redis.interfaces.CachedConcurrentMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is used as a cache for the static resources (js and css) used in the application. It
 * keeps the information needed to make use of those resources without the need of generating them
 * again.
 */
@ApplicationScoped
public class StaticResourceProvider implements StaticResourceProviderMBean {
  private static final Logger log = LogManager.getLogger();

  private String genTargetLocation;
  private CachedConcurrentMap<String, String> staticResources = new CachedConcurrentMap<>("staticResources");

  /**
   * Returns the information stored for a particular static resource whose identifying name is
   * passed as parameter.
   * 
   * @param resourceName
   *          the identifying name of the static resource
   * 
   * @return a String with information of the static resource
   */
  public String getStaticResourceCachedInfo(String resourceName) {
    return staticResources.get(resourceName);
  }

  /**
   * Returns the file name of a particular static resource whose identifying name is passed as
   * parameter, if the file exists. If the file does not exists, this method returns null and
   * removes the mapping of the static resource.
   * 
   * @param resourceName
   *          the identifying name of the static resource
   * 
   * @return a String with information of the static resource
   */
  public String getStaticResourceCachedFileName(String resourceName) {
    String resource = staticResources.get(resourceName);
    if (resource == null) {
      return null;
    }
    String jsFileName = resource + ".js";
    Path resourceFile = Paths.get(getGenTargetLocation(), jsFileName);
    if (!Files.exists(resourceFile)) {
      staticResources.remove(resourceName);
      log.info("Static resource file with name {} not found, removed its information from cache.",
          jsFileName);
      return null;
    }
    return resource;
  }

  private String getGenTargetLocation() {
    if (genTargetLocation == null) {
      genTargetLocation = RequestContext.getServletContext()
          .getRealPath(StaticResourceComponent.GEN_TARGET_LOCATION);
    }
    return genTargetLocation;
  }

  /**
   * Stores the information related to a particular static resource.
   * 
   * @param resourceName
   *          the identifying name of the static resource
   * @param content
   *          the information about the static resource to keep in cache
   */
  public void putStaticResourceCachedInfo(String resourceName, String content) {
    String value = staticResources.putIfAbsent(resourceName, content);
    if (value == null) {
      log.debug("Information of {} static resource stored in cache", resourceName);
    }
  }

  /**
   * @return a Set with the keys used in the static resources cache.
   */
  @Override
  public Set<String> getCachedStaticResourceKeys() {
    return staticResources.keySet();
  }

  /**
   * Removes the cached information related to a static resource whose identifying name is passed as
   * parameter.
   * 
   * @param resourceName
   *          the identifying name of the static resource
   */
  @Override
  public void removeStaticResourceCachedInfo(String resourceName) {
    if (staticResources.containsKey(resourceName)) {
      staticResources.remove(resourceName);
      log.info("Information of {} static resource removed from cache", resourceName);
    }
  }

  /**
   * Removes all the cached information of the static resources.
   */
  @Override
  public void removeAllStaticResourceCachedInfo() {
    staticResources.clear();
    log.info("Static resources information removed from cache");
  }

  /**
   * @return a List with the names of the files that contain static resources.
   */
  @Override
  public List<String> getStaticResourceFileNames() {
    List<String> fileNames = new ArrayList<>();
    for (String key : staticResources.keySet()) {
      if (!key.contains(StyleSheetResourceComponent.CSS)) {
        fileNames.add(key + ": " + staticResources.get(key) + ".js");
      }
    }
    return fileNames;
  }
}
