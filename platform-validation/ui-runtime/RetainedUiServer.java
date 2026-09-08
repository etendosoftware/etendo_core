package com.etendoerp.platform.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.startup.Tomcat;
import org.openbravo.authentication.AuthenticationException;
import org.openbravo.authentication.basic.DefaultAuthenticationManager;
import org.openbravo.authentication.hashing.PasswordHash;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.dal.core.DalSessionFactoryController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Local platform composition delegating authentication and persistence to the original engine.
 * MODULE-BOUNDARY ui-http: optional UI hosting; never include in either headless runtime.
 */
public final class RetainedUiServer {
  private RetainedUiServer() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 3) throw new IllegalArgumentException("Expected private properties, loopback port and public assets");
    Path publicAssets = Path.of(args[2]).toRealPath();
    if (!Files.isRegularFile(publicAssets.resolve(
        "web/org.openbravo.userinterface.smartclient/isomorphic/ISC_Combined.js"))) {
      throw new IllegalArgumentException("Canonical SmartClient public assets are missing");
    }
    OBPropertiesProvider.getInstance().setProperties(args[0]);
    var properties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    properties.setProperty("login.session.support.class", "org.openbravo.base.secureApp.PlatformLoginSessionSupport");
    properties.setProperty("ui.userInfo.accessPolicy.class",
        "org.openbravo.client.application.navigationbarcomponents.PlatformUserInfoAccessPolicy");
    PlatformShell.initializeFormats();
    for (String name : new String[] {"common.plm.Product", "common.enterprise.Warehouse", "common.businesspartner.BusinessPartner"}) {
      if (RetainedUiServer.class.getClassLoader().getResource("org/openbravo/model/"
          + name.replace('.', '/') + ".class") != null) throw new IllegalStateException("ERP runtime class present");
    }
    for (var entity : ModelProvider.getInstance().getModel()) {
      var type = entity.getMappingClass();
      if (type == null) throw new IllegalStateException("Missing generated class: " + entity.getClassName());
      OBProvider.getInstance().register(type, type, false);
      OBProvider.getInstance().register(entity.getName(), type, false);
    }
    var controller = new DalSessionFactoryController();
    SessionFactoryController.setInstance(controller);
    var factory = controller.getSessionFactory();
    Tomcat tomcat = new Tomcat();
    try (var components = new UiComponents()) {
      provisionInitialCredential();
      Path base = Files.createTempDirectory(Path.of("build"), "original-ui-tomcat-").toAbsolutePath();
      tomcat.setBaseDir(base.toString());
      tomcat.setPort(Integer.parseInt(args[1]));
      tomcat.getConnector().setProperty("address", "127.0.0.1");
      tomcat.getConnector().setProperty("maxThreads", "4");
      tomcat.getConnector().setProperty("minSpareThreads", "1");
      var context = tomcat.addContext("/platform", publicAssets.toString());
      context.setParentClassLoader(RetainedUiServer.class.getClassLoader());
      var staticFiles = Tomcat.addServlet(context, "default", "org.apache.catalina.servlets.DefaultServlet");
      staticFiles.addInitParameter("listings", "false");
      staticFiles.addInitParameter("readonly", "true");
      context.addServletMappingDecoded("/", "default");
      context.addMimeMapping("js", "application/javascript");
      context.addMimeMapping("css", "text/css");
      context.addMimeMapping("png", "image/png");
      context.addMimeMapping("gif", "image/gif");
      context.addParameter("LoginServlet", "/login");
      context.setSessionTimeout(30);
      org.openbravo.client.kernel.RequestContext.setServletContext(context.getServletContext());
      Tomcat.addServlet(context, "platform-session", new SessionServlet(components)).setLoadOnStartup(1);
      context.addServletMappingDecoded("/login", "platform-session");
      context.addServletMappingDecoded("/session", "platform-session");
      context.addServletMappingDecoded("/components/*", "platform-session");
      context.addServletMappingDecoded("/shell", "platform-session");
      context.addServletMappingDecoded("/bootstrap.js", "platform-session");
      context.addServletMappingDecoded("/types.js", "platform-session");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try { tomcat.stop(); }
        catch (Exception failure) { System.err.println("UI shutdown failed: " + failure.getClass().getSimpleName()); }
      }, "original-ui-shutdown"));
      tomcat.start();
      if (!context.getState().isAvailable()) throw new IllegalStateException("UI HTTP composition failed");
      System.out.println("READY: http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/platform/session");
      System.out.println("PENDING: original shell and browser CRUD integration");
      tomcat.getServer().await();
    } finally {
      try { tomcat.stop(); tomcat.destroy(); }
      finally { clearThread(); factory.close(); }
    }
  }

  private static void provisionInitialCredential() {
    String password = System.getenv("PLATFORM_INITIAL_PASSWORD");
    if (password == null || password.isBlank()) return;
    OBContext.setAdminMode();
    try {
      var dal = OBDal.getInstance();
      var user = dal.get(User.class, "U1");
      if (user == null || (user.getPassword() != null && !user.getPassword().isBlank())) {
        throw new IllegalStateException("Initial credential requires an unconfigured owned fixture user");
      }
      user.setUsername("admin");
      user.setPassword(PasswordHash.generateHash(password));
      user.setDefaultRole(dal.get(org.openbravo.model.ad.access.Role.class, "R1"));
      user.setDefaultClient(dal.get(org.openbravo.model.ad.system.Client.class, "C1"));
      user.setDefaultOrganization(dal.get(org.openbravo.model.common.enterprise.Organization.class, "O1"));
      user.setDefaultLanguage(dal.get(org.openbravo.model.ad.system.Language.class, "LANG"));
      dal.commitAndClose();
    } finally { OBContext.restorePreviousMode(); clearThread(); }
  }

  private static void clearThread() {
    try {
      if (SessionHandler.isSessionHandlerPresent()) SessionHandler.getInstance().rollback();
    } finally {
      OBContext.setOBContext((OBContext) null);
      org.openbravo.client.kernel.RequestContext.clear();
      org.openbravo.database.SessionInfo.init();
    }
  }

  /** Minimal HTTP composition; credential, lockout and session rules remain canonical. */
  private static final class SessionServlet extends HttpServlet {
    private final UiComponents components;
    SessionServlet(UiComponents components) { this.components = components; }
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
      response.setHeader("Cache-Control", "no-store");
      boolean systemScope = false;
      try {
        if (java.util.Set.of("/session", "/components", "/shell", "/bootstrap.js", "/types.js").contains(request.getServletPath())
            && "GET".equals(request.getMethod())) {
          var session = request.getSession(false);
          if (session == null || session.getAttribute("#Authenticated_user") == null) {
            response.setStatus(401);
            return;
          }
          OBContext.setOBContext(request);
          if (OBContext.getOBContext().isInAdministratorMode()) throw new IllegalStateException("Elevated user session");
          new DefaultAuthenticationManager(this).authenticate(request, response);
          var requestContext = org.openbravo.client.kernel.RequestContext.get();
          requestContext.setRequest(request);
          requestContext.setResponse(response);
          if ("/shell".equals(request.getServletPath())) {
            String html = new PlatformShell().document(request.getParameter("windowId"));
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(html);
            return;
          }
          if ("/bootstrap.js".equals(request.getServletPath())) {
            String javascript = new PlatformShell().bootstrap(new VariablesSecureApp(request));
            response.setContentType("application/javascript;charset=UTF-8");
            response.getWriter().write(javascript);
            return;
          }
          if ("/types.js".equals(request.getServletPath())) {
            String javascript = new PlatformShell().types();
            response.setContentType("application/javascript;charset=UTF-8");
            response.getWriter().write(javascript);
            return;
          }
          if ("/components".equals(request.getServletPath())) {
            String output = components.render(request.getPathInfo().substring(1), request.getParameter("windowId"));
            response.setContentType("application/javascript;charset=UTF-8");
            response.getWriter().write(output);
            return;
          }
          response.setContentType("application/json");
          var vars = new VariablesSecureApp(request);
          response.getWriter().write(new org.codehaus.jettison.json.JSONObject()
              .put("user", vars.getUser()).put("role", vars.getRole()).put("client", vars.getClient())
              .put("organization", vars.getOrg()).toString());
          return;
        }
        if (!"/login".equals(request.getServletPath()) || !"POST".equals(request.getMethod())) {
          response.setStatus(405);
          return;
        }
        if (request.getSession(false) != null) request.getSession().invalidate();
        request.getSession(true);
        request.changeSessionId();
        OBContext.setAdminMode();
        systemScope = true;
        String user = new DefaultAuthenticationManager(this).authenticate(request, response);
        if (user == null) throw new AuthenticationException("Missing authenticated user");
        var vars = new VariablesSecureApp(request);
        var connection = new DalConnectionProvider(false);
        var defaults = LoginUtils.getLoginDefaults(user, "", connection);
        if (!LoginUtils.fillSessionArguments(connection, vars, user, "en_US", "N", defaults.role,
            defaults.client, defaults.org, defaults.warehouse)) throw new AuthenticationException("Invalid login scope");
        OBContext.restorePreviousMode();
        systemScope = false;
        if (OBContext.getOBContext().isInAdministratorMode()) throw new IllegalStateException("Elevated login scope");
        OBContext.setOBContextInSession(request, OBContext.getOBContext());
        OBDal.getInstance().commitAndClose();
        response.setStatus(204);
      } catch (SecurityException denied) {
        response.setStatus(403);
      } catch (IllegalArgumentException invalid) {
        response.setStatus(400);
      } catch (AuthenticationException denied) {
        if (request.getSession(false) != null) request.getSession().invalidate();
        response.setStatus(401);
      } catch (Exception failure) {
        if (request.getSession(false) != null) request.getSession().invalidate();
        getServletContext().log("Platform UI session failed", failure);
        response.setStatus(500);
      } finally {
        if (systemScope) OBContext.restorePreviousMode();
        clearThread();
      }
    }
  }
}
