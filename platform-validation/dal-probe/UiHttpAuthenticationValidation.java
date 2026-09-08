package com.etendoerp.platform.validation;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
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

/** Real-container authentication integration harness, not a deployable login endpoint. */
public final class UiHttpAuthenticationValidation {
  private static final ThreadLocal<Boolean> metadataScope = new ThreadLocal<>();
  public static void main(String[] args) throws Exception {
    OBPropertiesProvider.getInstance().setProperties(args[0]);
    var properties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    properties.setProperty("login.session.support.class", "org.openbravo.base.secureApp.PlatformLoginSessionSupport");
    require(DefaultAuthenticationManager.class.getProtectionDomain().getCodeSource().getLocation()
        .toString().contains("/platform-http-authentication/"), "Authenticator did not load from canonical source compilation");
    for (String name : new String[] {"common.plm.Product", "common.enterprise.Warehouse", "common.businesspartner.BusinessPartner"}) {
      if (UiHttpAuthenticationValidation.class.getClassLoader().getResource("org/openbravo/model/"
          + name.replace('.', '/') + ".class") != null) throw new AssertionError("ERP runtime present");
    }
    for (var entity : ModelProvider.getInstance().getModel()) {
      var type = entity.getMappingClass();
      OBProvider.getInstance().register(type, type, false);
      OBProvider.getInstance().register(entity.getName(), type, false);
    }
    var controller = new DalSessionFactoryController();
    SessionFactoryController.setInstance(controller);
    var factory = controller.getSessionFactory();
    Tomcat tomcat = new Tomcat();
    org.openbravo.database.ConnectionProviderImpl pool = null;
    try {
      // Test fixture setup is committed only in the disposable database owned by this gate.
      fixtureContext();
      String password = UUID.randomUUID().toString();
      var user = OBDal.getInstance().get(User.class, "U1");
      user.setUsername("platform-http-validation");
      user.setPassword(PasswordHash.generateHash(password));
      user.setDefaultRole(OBContext.getOBContext().getRole());
      user.setDefaultClient(OBContext.getOBContext().getCurrentClient());
      user.setDefaultOrganization(OBContext.getOBContext().getCurrentOrganization());
      user.setDefaultLanguage(OBContext.getOBContext().getLanguage());
      OBDal.getInstance().commitAndClose();
      clearThread();
      Path base = Files.createTempDirectory(Path.of("build"), "ui-http-auth-").toAbsolutePath();
      tomcat.setBaseDir(base.toString());
      tomcat.setPort(0);
      tomcat.getConnector().setProperty("address", "127.0.0.1");
      tomcat.getConnector().setProperty("maxThreads", "1");
      tomcat.getConnector().setProperty("minSpareThreads", "1");
      var context = tomcat.addContext("/platform", base.toString());
      context.setParentClassLoader(UiHttpAuthenticationValidation.class.getClassLoader());
      pool = new org.openbravo.database.ConnectionProviderImpl(properties);
      context.getServletContext().setAttribute("openbravoPool", pool);
      context.addApplicationLifecycleListener(new org.openbravo.erpCommon.security.SessionListener());
      context.addParameter("LoginServlet", "/login");
      Tomcat.addServlet(context, "authentication", new ProbeServlet()).setLoadOnStartup(1);
      context.addServletMappingDecoded("/*", "authentication");
      tomcat.start();
      var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
      var client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
      String url = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/platform";
      require(send(client, url + "/session", null).statusCode() == 401, "Anonymous session accepted");
      require(send(client, url + "/prepare", null).statusCode() == 204, "Prelogin cookie failed");
      require(send(client, url + "/login", "user=platform-http-validation&password=invalid").statusCode() == 401,
          "Incorrect password accepted");
      require(send(client, url + "/session", null).statusCode() == 401, "Failed login acquired authentication");
      require(send(client, url + "/prepare", null).statusCode() == 204, "Fresh prelogin cookie failed");
      String initial = cookies.getCookieStore().getCookies().get(0).getValue();
      require(send(client, url + "/login", "user=platform-http-validation&password=" + password).statusCode() == 204,
          "Original HTTP authentication failed");
      String authenticated = cookies.getCookieStore().getCookies().get(0).getValue();
      require(!initial.equals(authenticated), "Prelogin session ID survived authentication");
      var session = send(client, url + "/session", null);
      require(session.statusCode() == 200 && session.body().equals("U1|R1|C1|O1|csrf"), "Cookie session scope failed");
      var stranger = HttpClient.newHttpClient();
      require(send(stranger, url + "/session", null).statusCode() == 401, "Cookie-less request inherited context");
      var stale = stranger.send(HttpRequest.newBuilder(URI.create(url + "/session"))
          .timeout(Duration.ofSeconds(10)).header("Cookie", "JSESSIONID=" + initial).GET().build(),
          HttpResponse.BodyHandlers.ofString());
      require(stale.statusCode() == 401, "Prelogin cookie still authenticates after rotation");
      fixtureContext();
      var sessions = OBDal.getInstance().createQuery(org.openbravo.model.ad.access.Session.class,
          "username = :name").setNamedParameter("name", "platform-http-validation").list();
      require(sessions.stream().anyMatch(value -> "F".equals(value.getLoginStatus()) && !value.isSessionActive()),
          "Original failed AD_Session missing");
      require(sessions.stream().anyMatch(value -> "S".equals(value.getLoginStatus()) && value.isSessionActive()
          && authenticated.equals(value.getWebSession())), "Original successful AD_Session missing");
      String dbSession = sessions.stream().filter(value -> authenticated.equals(value.getWebSession()))
          .findFirst().orElseThrow().getId();
      clearThread();
      require(send(client, url + "/logout", "").statusCode() == 204, "Logout failed");
      require(send(client, url + "/session", null).statusCode() == 401, "Logout retained authentication");
      fixtureContext();
      require(!OBDal.getInstance().get(org.openbravo.model.ad.access.Session.class, dbSession).isSessionActive(),
          "Original listener did not persist logout");
      clearThread();
      require(send(client, url + "/login", "user=platform-http-validation&password=" + password).statusCode() == 204,
          "Second login failed");
      String finalCookie = cookies.getCookieStore().getCookies().get(0).getValue();
      tomcat.stop();
      fixtureContext();
      var closed = OBDal.getInstance().createQuery(org.openbravo.model.ad.access.Session.class,
          "webSession = :cookie").setNamedParameter("cookie", finalCookie).list();
      require(closed.size() == 1 && !closed.get(0).isSessionActive(), "Shutdown left an active database session");
      System.out.println("PASS: Canonical HTTP authenticator persists success/failure sessions without ERP entities");
      System.out.println("PASS: Real Tomcat cookie rotation/stale-cookie denial, full login scope, CSRF and single-worker anonymous isolation");
      System.out.println("PASS: Original system pre-authentication context and session listener logout/shutdown persistence");
    } finally {
      try { tomcat.stop(); tomcat.destroy(); }
      finally { clearThread(); factory.close(); if (pool != null) pool.destroy(); }
    }
  }

  private static HttpResponse<String> send(HttpClient client, String url, String form) throws Exception {
    var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10));
    if (form == null) request.GET();
    else request.header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(form));
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static void fixtureContext() {
    // Harness-only metadata access; this is not a production pre-authentication identity.
    OBContext.setOBContext("U1", "R1", "C1", "O1", "en_US");
    OBContext.setAdminMode();
    metadataScope.set(true);
  }

  private static void leaveFixtureScope() {
    if (Boolean.TRUE.equals(metadataScope.get())) OBContext.restorePreviousMode();
    metadataScope.remove();
  }

  private static void systemContext() {
    OBContext.setOBContext((OBContext) null);
    OBContext.setAdminMode();
    metadataScope.set(true);
    require("0".equals(OBContext.getOBContext().getUser().getId())
        && "0".equals(OBContext.getOBContext().getRole().getId()), "Pre-authentication did not use system context");
  }

  private static void clearThread() {
    try {
      if (SessionHandler.isSessionHandlerPresent()) SessionHandler.getInstance().rollback();
    }
    finally {
      leaveFixtureScope();
      OBContext.setOBContext((OBContext) null);
      org.openbravo.database.SessionInfo.init();
    }
  }

  private static final class ProbeServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
      response.setHeader("Cache-Control", "no-store");
      try {
        if (request.getPathInfo().equals("/prepare")) {
          request.getSession(true);
          response.setStatus(204);
          return;
        }
        if (request.getPathInfo().equals("/logout")) {
          require("POST".equals(request.getMethod()), "Only POST logout is tested");
          if (request.getSession(false) != null) request.getSession().invalidate();
          response.setStatus(204);
          return;
        }
        if (request.getPathInfo().equals("/session")) {
          var session = request.getSession(false);
          if (session == null || session.getAttribute("#Authenticated_user") == null) {
            response.setStatus(401);
            return;
          }
          OBContext.setOBContext(request);
          require(!OBContext.getOBContext().isInAdministratorMode(), "Cookie session retained fixture admin privileges");
          String user = new DefaultAuthenticationManager(this).authenticate(request, response);
          var vars = new VariablesSecureApp(request);
          response.getWriter().write(user + "|" + vars.getRole() + "|" + vars.getClient() + "|" + vars.getOrg()
              + "|" + (vars.getSessionValue("#CSRF_Token").isBlank() ? "missing" : "csrf"));
          return;
        }
        require("POST".equals(request.getMethod()), "Only POST login is tested");
        systemContext();
        request.getSession(true);
        request.changeSessionId();
        request.getSession().removeAttribute("#Authenticated_user");
        String user = new DefaultAuthenticationManager(this).authenticate(request, response);
        require(user != null, "Authentication returned no user");
        var vars = new VariablesSecureApp(request);
        var cp = new DalConnectionProvider(false);
        var defaults = LoginUtils.getLoginDefaults(user, "", cp);
        require(LoginUtils.fillSessionArguments(cp, vars, user, "en_US", "N", defaults.role,
            defaults.client, defaults.org, defaults.warehouse), "Original full session rejected defaults");
        leaveFixtureScope();
        require(!OBContext.getOBContext().isInAdministratorMode(), "Login retained fixture admin privileges");
        OBContext.setOBContextInSession(request, OBContext.getOBContext());
        OBDal.getInstance().commitAndClose();
        response.setStatus(204);
      } catch (AuthenticationException denied) {
        if (request.getSession(false) != null) request.getSession().invalidate();
        response.setStatus(401);
      } catch (Exception | AssertionError failure) {
        getServletContext().log("HTTP authentication probe failed", failure);
        response.setStatus(500);
      } finally { clearThread(); }
    }
  }
}
