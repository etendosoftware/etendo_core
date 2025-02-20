package org.openbravo.base.secureApp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.TokenUser;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

public class LinkAuth0Account extends HttpBaseServlet {
  private static final String AUTH0_DOMAIN = "dev-fut-test.us.auth0.com";
  private static final String AUTH0_CLIENT_ID = "zxo9HykojJHT1HXg18KwUjCNlLPs3tZU";
  private static final String AUTH0_CLIENT_SECRET = "EAlNG8TK063hfFWmQHRdn94F7qzle04GQ7q3O067_lTMzcKAG4tPQ6P476hxdRAV";
  private static final String AUTH0_CALLBACK_URL = "http://localhost:8080/google/secureApp/LinkAuth0Account.html";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    String token = getAuthToken(req);
    HashMap<String, String> tokenValues = decodeToken(token);
    matchUser(token, tokenValues.get("sub"));
    res.sendRedirect("/" + OBPropertiesProvider.getInstance().getOpenbravoProperties().get("context.name"));
  }

  private void matchUser(String token, String sub) {
    try {
      OBContext.setAdminMode(true);
      TokenUser tokenUser = (TokenUser) OBDal.getInstance().createCriteria(TokenUser.class)
          .add(Restrictions.eq(TokenUser.PROPERTY_SUB, sub))
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false)
          .setMaxResults(1).uniqueResult();
      if (tokenUser != null) {
        OBDal.getInstance().remove(tokenUser);
      }
      tokenUser = OBProvider.getInstance().get(TokenUser.class);
      tokenUser.setSub(sub);
      tokenUser.setToken(token);
      String[] provider = StringUtils.split(sub, "|");
      tokenUser.setTokenProvider(provider[0]);
      tokenUser.setUser(OBContext.getOBContext().getUser());
      OBDal.getInstance().save(tokenUser);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log4j.error(e);
      throw new OBException("Error al vincular la cuenta de Google con la de Auth0", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private HashMap<String, String> decodeToken(String token) {

    HashMap<String, String> tokenValues = new HashMap<>();
    DecodedJWT decodedJWT = JWT.decode(token);

    tokenValues.put("given_name", decodedJWT.getClaim("given_name").asString());
    tokenValues.put("family_name", decodedJWT.getClaim("family_name").asString());
    tokenValues.put("email", decodedJWT.getClaim("email").asString());
    tokenValues.put("sid", decodedJWT.getClaim("sid").asString());
    tokenValues.put("sub", decodedJWT.getClaim("sub").asString());
    return tokenValues;
  }

  private String getAuthToken(HttpServletRequest request) {
    String code = request.getParameter("code");
    String token = "";
    String tokenEndpoint = "https://" + AUTH0_DOMAIN + "/oauth/token";
    try {
      URL url = new URL(tokenEndpoint);
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("POST");
      con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      con.setDoOutput(true);

      String codeVerifier = (String) request.getSession().getAttribute("code_verifier");
      boolean isPKCE = (codeVerifier != null && !codeVerifier.isEmpty());

      // Construcción de parámetros según PKCE o client_secret
      String params;
      if (isPKCE) {
        params = String.format(
            "grant_type=authorization_code&client_id=%s&code=%s&redirect_uri=%s&code_verifier=%s",
            URLEncoder.encode(AUTH0_CLIENT_ID, StandardCharsets.UTF_8),
            URLEncoder.encode(code, StandardCharsets.UTF_8),
            URLEncoder.encode(AUTH0_CALLBACK_URL, StandardCharsets.UTF_8),
            URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8)
        );
      } else {
        params = String.format(
            "grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s",
            URLEncoder.encode(AUTH0_CLIENT_ID, StandardCharsets.UTF_8),
            URLEncoder.encode(AUTH0_CLIENT_SECRET, StandardCharsets.UTF_8), // Solo si no usas PKCE
            URLEncoder.encode(code, StandardCharsets.UTF_8),
            URLEncoder.encode(AUTH0_CALLBACK_URL, StandardCharsets.UTF_8)
        );
      }

      try (OutputStream os = con.getOutputStream()) {
        byte[] input = params.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      int status = con.getResponseCode();
      if (status == 200) {
        try (InputStream in = con.getInputStream()) {
          String responseBody = new String(in.readAllBytes(), StandardCharsets.UTF_8);
          JSONObject jsonResponse = new JSONObject(responseBody);
          token = jsonResponse.getString("id_token");
        }
      } else {
        token = null;
      }
    } catch (JSONException | IOException e) {
      log4j.error(e);
    }
    return token;
  }

}
