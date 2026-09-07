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
    if (args.length != 2) throw new IllegalArgumentException("Expected private properties and loopback port");
    OBPropertiesProvider.getInstance().setProperties(args[0]);
    var properties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    properties.setProperty("login.session.support.class", "org.openbravo.base.secureApp.PlatformLoginSessionSupport");
    properties.setProperty("ui.userInfo.accessPolicy.class",
        "org.openbravo.client.application.navigationbarcomponents.PlatformUserInfoAccessPolicy");
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
    try {
      provisionInitialCredential();
      Path base = Files.createTempDirectory(Path.of("build"), "original-ui-tomcat-").toAbsolutePath();
      tomcat.setBaseDir(base.toString());
      tomcat.setPort(Integer.parseInt(args[1]));
      tomcat.getConnector().setProperty("address", "127.0.0.1");
      tomcat.getConnector().setProperty("maxThreads", "4");
      tomcat.getConnector().setProperty("minSpareThreads", "1");
      var context = tomcat.addContext("/platform", base.toString());
      context.setParentClassLoader(RetainedUiServer.class.getClassLoader());
      context.addParameter("LoginServlet", "/login");
      context.setSessionTimeout(30);
      Tomcat.addServlet(context, "platform-session", new SessionServlet()).setLoadOnStartup(1);
      context.addServletMappingDecoded("/login", "platform-session");
      context.addServletMappingDecoded("/session", "platform-session");
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
      org.openbravo.database.SessionInfo.init();
    }
  }

  /** Minimal HTTP composition; credential, lockout and session rules remain canonical. */
  private static final class SessionServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
      response.setHeader("Cache-Control", "no-store");
      boolean systemScope = false;
      try {
        if ("/session".equals(request.getServletPath()) && "GET".equals(request.getMethod())) {
          var session = request.getSession(false);
          if (session == null || session.getAttribute("#Authenticated_user") == null) {
            response.setStatus(401);
            return;
          }
          OBContext.setOBContext(request);
          if (OBContext.getOBContext().isInAdministratorMode()) throw new IllegalStateException("Elevated user session");
          new DefaultAuthenticationManager(this).authenticate(request, response);
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
