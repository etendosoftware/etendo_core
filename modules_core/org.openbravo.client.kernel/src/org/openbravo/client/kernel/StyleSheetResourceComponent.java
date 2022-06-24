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
 * All portions are Copyright (C) 2010-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.model.ad.module.Module;

/**
 * Component which reads all stylesheets.
 * 
 * @author mtaal
 * @author iperdomo
 */
public class StyleSheetResourceComponent extends BaseComponent {
  private static final Logger log = LogManager.getLogger();
  private static final String IMGURLHOLDER = "__URLHOLDER__";

  protected static final String CSS = "CSS";

  @Inject
  @Any
  private Instance<ComponentProvider> componentProviders;

  @Inject
  private StaticResourceProvider resourceProvider;

  /**
   * @return returns this instance
   * @see org.openbravo.client.kernel.BaseComponent#getData()
   */
  @Override
  public Object getData() {
    return this;
  }

  @Override
  public String getContentType() {
    return KernelConstants.CSS_CONTENTTYPE;
  }

  @Override
  public boolean isJavaScriptComponent() {
    return false;
  }

  @Override
  public String getETag() {
    final String appNameKey = getAppNameKey();
    if (resourceProvider.getStaticResourceCachedInfo(appNameKey) == null) {
      // do something unique
      return String.valueOf(System.currentTimeMillis());
    } else {
      // compute the md5 of the CSS cached content
      return DigestUtils.md5Hex(resourceProvider.getStaticResourceCachedInfo(appNameKey));
    }
  }

  @Override
  public String generate() {
    final String appNameKey = getAppNameKey();
    String cssContent = resourceProvider.getStaticResourceCachedInfo(appNameKey);
    if (cssContent != null) {
      return cssContent;
    }

    final List<Module> modules = KernelUtils.getInstance().getModulesOrderedByDependency();
    final ServletContext context = (ServletContext) getParameters()
        .get(KernelConstants.SERVLET_CONTEXT);
    final StringBuffer sb = new StringBuffer();

    final String appName = getApplicationName();

    final boolean makeCssDataUri = getParameters().get("_cssDataUri") != null
        && getParameters().get("_cssDataUri").equals("true");

    final String skinParam;
    if (getParameters().containsKey(KernelConstants.SKIN_PARAMETER)) {
      skinParam = (String) getParameters().get(KernelConstants.SKIN_PARAMETER);
    } else {
      skinParam = KernelConstants.SKIN_DEFAULT;
    }

    for (Module module : modules) {
      for (ComponentProvider provider : componentProviders) {
        final List<ComponentResource> resources = provider.getGlobalComponentResources();
        if (resources == null || resources.size() == 0) {
          continue;
        }

        if (provider.getModule().getId().equals(module.getId())) {
          for (ComponentResource resource : resources) {

            if (resource.getType() == ComponentResourceType.Stylesheet
                && resource.isValidForApp(appName)) {

              log.debug("Processing resource: " + resource);

              String resourcePath = resource.getPath();

              // Skin version handling
              if (resourcePath.contains(KernelConstants.SKIN_PARAMETER)) {
                resourcePath = resourcePath.replaceAll(KernelConstants.SKIN_PARAMETER, skinParam);
              }
              if (!resourcePath.startsWith("/")) {
                // Tomcat 8 forces getRealPath to start with a slash
                resourcePath = "/" + resourcePath;
              }

              try {
                final String realResourcePath = context.getRealPath(resourcePath);
                final File file = new File(realResourcePath);
                if (!file.exists() || !file.canRead()) {
                  log.error(file.getAbsolutePath() + " cannot be read");
                  continue;
                }
                String resourceContents = FileUtils.readFileToString(file, "UTF-8");

                final String contextPath = (getContextUrl()
                    + resourcePath.substring(0, resourcePath.lastIndexOf("/"))).replaceAll("//",
                        "/");
                String realPath = "";
                if (realResourcePath.lastIndexOf("/") != -1) {
                  realPath = realResourcePath.substring(0, realResourcePath.lastIndexOf("/"));
                } else if (realResourcePath.lastIndexOf("\\") != -1) {
                  realPath = realResourcePath.substring(0, realResourcePath.lastIndexOf("\\"));
                }

                // repair urls
                resourceContents = resourceContents.replace("url(./", "url(" + IMGURLHOLDER + "/");
                resourceContents = resourceContents.replace("url(images",
                    "url(" + IMGURLHOLDER + "/images");
                resourceContents = resourceContents.replace("url(\"images",
                    "url(\"" + IMGURLHOLDER + "/images");
                resourceContents = resourceContents.replace("url('images",
                    "url('" + IMGURLHOLDER + "/images");
                resourceContents = resourceContents.replace("url('./",
                    "url('" + IMGURLHOLDER + "/");
                resourceContents = resourceContents.replace("url(\"./",
                    "url(\"" + IMGURLHOLDER + "/");

                if (!module.isInDevelopment()) {
                  resourceContents = CSSMinimizer.formatString(resourceContents);
                  if (makeCssDataUri) {
                    String resourceContentsLine;
                    BufferedReader resourceContentsReader = new BufferedReader(
                        new StringReader(resourceContents));
                    StringBuffer resourceContentsBuffer = new StringBuffer();

                    int indexOfUrl;
                    String imgUrl, imgExt, imgDataUri, newUrlParam;
                    while ((resourceContentsLine = resourceContentsReader.readLine()) != null) {
                      indexOfUrl = 0;
                      while ((indexOfUrl = resourceContentsLine.indexOf("url(",
                          indexOfUrl)) != -1) {
                        imgUrl = resourceContentsLine.substring(indexOfUrl + 4,
                            resourceContentsLine.indexOf(")", indexOfUrl));
                        if (imgUrl.indexOf("\"") == 0 || imgUrl.indexOf("'") == 0) {
                          imgUrl = imgUrl.substring(1, imgUrl.length());
                        }
                        if (imgUrl.indexOf("\"") == imgUrl.length() - 1
                            || imgUrl.indexOf("'") == imgUrl.length() - 1) {
                          imgUrl = imgUrl.substring(0, imgUrl.length() - 1);
                        }
                        imgExt = imgUrl.substring(imgUrl.lastIndexOf(".") + 1, imgUrl.length());
                        imgExt = imgExt.toLowerCase();
                        if (imgExt.equals("jpg")) {
                          imgExt = "jpeg";
                        }
                        if (imgExt.equals("jpeg") || imgExt.equals("png") || imgExt.equals("gif")) {
                          imgDataUri = filePathToBase64(imgUrl.replace(IMGURLHOLDER, realPath));
                        } else {
                          imgDataUri = "";
                        }
                        if (imgDataUri != "") {
                          newUrlParam = "data:image/" + imgExt + ";base64," + imgDataUri;
                          resourceContentsLine = resourceContentsLine.replace(imgUrl, newUrlParam);
                        }
                        indexOfUrl = indexOfUrl + 1;
                      }
                      resourceContentsBuffer.append(resourceContentsLine).append("\n");
                    }
                    resourceContents = resourceContentsBuffer.toString();
                  }
                }
                resourceContents = resourceContents.replace(IMGURLHOLDER, contextPath);
                sb.append(resourceContents);
              } catch (Exception e) {
                log.error("Error reading file: " + resource, e);
              }
            }
          }
        }
      }
    }

    cssContent = sb.toString();
    if (!isInDevelopment()) {
      resourceProvider.putStaticResourceCachedInfo(appNameKey, cssContent);
    }
    return cssContent;
  }

  private String getAppNameKey() {
    String appNameKey = getApplicationName() + "_" + CSS;

    if (getParameters().containsKey(KernelConstants.SKIN_PARAMETER)) {
      appNameKey += "_" + (String) getParameters().get(KernelConstants.SKIN_PARAMETER);
    } else {
      appNameKey += "_" + KernelConstants.SKIN_DEFAULT;
    }
    if ("true".equals(getParameters().get("_cssDataUri"))) {
      appNameKey += "_cssDataUri";
    }
    return appNameKey;
  }

  @Override
  public String getId() {
    return KernelConstants.STYLE_SHEET_COMPONENT_ID;
  }

  private String filePathToBase64(String path) {
    try {
      final File f = new File(path);
      if (!f.exists() || !f.canRead()) {
        return "";
      }
      byte[] fileBase64Bytes = Base64.encodeBase64(FileUtils.readFileToByteArray(f));
      return new String(fileBase64Bytes);
    } catch (final Exception e) {
      log.error("Error processing file: " + path + " - " + e.getMessage(), e);
    }
    return "";
  }

  @Override
  public boolean bypassAuthentication() {
    return true;
  }
}
