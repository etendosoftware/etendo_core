package com.etendoerp.platform.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.ApplicationComponentProvider;
import org.openbravo.client.kernel.ApplicationComponent;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.freemarker.FreemarkerTemplateProcessor;
import org.openbravo.dal.core.OBContext;

/** Platform-only document/branding composition; templates, resources and widgets remain canonical. */
final class PlatformShell extends FreemarkerTemplateProcessor {
  static void initializeFormats() throws Exception {
    try (var input = Files.newInputStream(Path.of("build/original-ui-bootstrap/Format.xml"))) {
      OBPropertiesProvider.getInstance().setFormatXML(input);
    }
  }

  String bootstrap(VariablesSecureApp vars) throws Exception {
    OBContext.setAdminMode();
    try {
      var application = new ApplicationComponent();
      application.setParameters(new HashMap<>(Map.of(KernelConstants.CONTEXT_URL, "/platform/")));
      var formats = new HashMap<String, String>();
      for (var element : OBPropertiesProvider.getInstance().getFormatXMLDocument().getRootElement().elements()) {
        formats.put(element.attributeValue("name"), element.attributeValue("formatOutput"));
      }
      org.openbravo.client.kernel.RequestContext.get().setSessionAttribute("#formatMap", formats);
      String staticJs = render("application-js.ftl", application);
      var context = OBContext.getOBContext();
      var data = new HashMap<String, Object>();
      // Project optional profile fields: BaseOBObject throws for absent ERP properties,
      // whereas the canonical template intentionally accepts missing optional values.
      data.put("user", profile(context.getUser(), "id", "firstName", "lastName", "username", "name", "email"));
      var role = profile(context.getRole(), "id", "name", "forPortalUsers", "portalAdmin");
      role.putIfAbsent("forPortalUsers", false);
      role.putIfAbsent("portalAdmin", false);
      data.put("role", role);
      data.put("client", context.getCurrentClient());
      data.put("organization", context.getCurrentOrganization());
      data.put("writableOrganizations", context.getWritableOrganizations());
      var entities = new java.util.HashSet<>(context.getEntityAccessChecker().getReadableEntities());
      entities.addAll(context.getEntityAccessChecker().getWritableEntities());
      data.put("accessibleEntities", entities);
      data.put("csrfToken", vars.getSessionValue("#CSRF_Token"));
      data.put("languageId", context.getLanguage().getId());
      data.put("language", context.getLanguage().getLanguage());
      data.put("systemVersion", "platform-validation");
      data.put("instancePurpose", "Development");
      data.put("licenseType", "");
      data.put("trialStringValue", "false");
      data.put("goldenStringValue", "false");
      data.put("activeInstanceStringValue", "false");
      data.put("versionDescription", "Platform validation");
      data.put("companyImageLogoData", Map.of("width", "122", "height", "34"));
      data.put("communityBrandingUrl", "");
      return staticJs + "\n" + render("application-dynamic-js.ftl", data) + "\n"
          + render("properties.js.ftl", new org.openbravo.client.application.PropertiesComponent())
          + "\nOB.Application.startupContributions = [];\n";
    } finally { OBContext.restorePreviousMode(); }
  }

  private String render(String name, Object data) throws Exception {
    return processTemplate(createTemplateImplementation(null,
        Files.readString(Path.of("build/original-ui-bootstrap", name))), Map.of("data", data));
  }

  String types() throws Exception {
    OBContext.setAdminMode();
    try { return render("smartclient_types_js.ftl", new org.openbravo.userinterface.smartclient.TypesComponent()); }
    finally { OBContext.restorePreviousMode(); }
  }

  private static Map<String, Object> profile(org.openbravo.base.structure.BaseOBObject object, String... properties) {
    var values = new HashMap<String, Object>();
    for (String property : properties) {
      if (object.getEntity().hasProperty(property)) values.put(property, object.get(property));
    }
    return values;
  }

  String document(String windowId) {
    if (windowId != null && !windowId.matches("[A-F0-9]{32}")) throw new IllegalArgumentException("Invalid window ID");
    StringBuilder html = new StringBuilder("<!doctype html><html><head><meta charset=\"UTF-8\">"
        + "<title>Platform validation</title><style>html,body{height:100%;width:100%;margin:0}</style>"
        + "<link rel=\"stylesheet\" href=\"/platform/web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/smartclient/skin_styles.css\"></head><body>"
        + "<script>var isomorphicDir='/platform/web/org.openbravo.userinterface.smartclient/isomorphic/';</script>");
    script(html, "web/org.openbravo.userinterface.smartclient/isomorphic/ISC_Combined.js");
    script(html, "web/org.openbravo.userinterface.smartclient/isomorphic/ISC_History.js");
    script(html, "web/org.openbravo.client.kernel/js/BigDecimal-all-1.0.3.min.js");
    script(html, "bootstrap.js");
    script(html, "web/org.openbravo.client.kernel/js/ob-i18n.js");
    script(html, "web/org.openbravo.client.kernel/js/ob-kernel-utilities.js");
    script(html, "web/org.openbravo.service.datasource/js/ob-datasource-utilities.js");
    script(html, "web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/smartclient/load_skin.js");
    script(html, "web/org.openbravo.userinterface.smartclient/js/ob-smartclient-labels.js");
    script(html, "web/org.openbravo.userinterface.smartclient/js/ob-smartclient.js");
    script(html, "types.js");
    script(html, "components/navigation");
    OBContext.setAdminMode();
    try {
      var provider = new ApplicationComponentProvider() {
        @Override protected void addApplicationSpecificResources(List<ComponentResource> resources) {
          // ERP workflows are not part of this composition. Generic permission UI remains required.
          resources.add(createStaticResource("web/js/recalculatePermissionsProcess.js", true));
        }
      };
      for (var resource : provider.getGlobalComponentResources()) {
        if (!resource.isIncludeInNewUIMode()) continue;
        String path = resource.getPath().replace(KernelConstants.SKIN_PARAMETER, "Default");
        if (path.startsWith("org.openbravo.client.kernel/")) { script(html, "components/layout"); continue; }
        if (path.endsWith(".css")) html.append("<link rel=\"stylesheet\" href=\"/platform/").append(path).append("\">");
        else script(html, path);
      }
    } finally { OBContext.restorePreviousMode(); }
    if (windowId != null) script(html, "components/window?windowId=" + windowId);
    html.append("<script>window.addEventListener('load',function(){OB.Layout.initialize();OB.Layout.draw();");
    if (windowId != null) html.append("OB.Layout.ViewManager.openView('_").append(windowId)
        .append("',{windowId:'").append(windowId).append("',tabTitle:'Application window',addToRecents:false});");
    return html.append("});</script></body></html>").toString();
  }

  private static void script(StringBuilder html, String path) {
    html.append("<script src=\"/platform/").append(path.replace("&", "&amp;").replace("\"", "&quot;"))
        .append("\"></script>");
  }
}
