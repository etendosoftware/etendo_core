package com.smf.securewebservices.service;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.smf.securewebservices.SWSConfig;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.authentication.hashing.PasswordHash;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.service.db.DalConnectionProvider;

import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.smf.securewebservices.utils.SecureWebServicesUtils;
import org.openbravo.base.secureApp.AllowedCrossDomainsHandler;

/**
 * 
 * @author androettop
 */
public class SecureLoginServlet extends HttpBaseServlet {
	private static final String APPLICATION_JSON = "application/json";
	private static final long serialVersionUID = 1L;
	static final long ONE_MINUTE_IN_MILLIS = 60000;
	private static final Logger log = LogManager.getLogger();

	public void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		AllowedCrossDomainsHandler.getInstance().setCORSHeaders(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		AllowedCrossDomainsHandler.getInstance().setCORSHeaders(request, response);
		JSONObject data;
		try {
			data = new JSONObject(IOUtils.toString(request.getReader()));
		} catch (JSONException | IOException e2) {
			data = new JSONObject();
		}
		JSONObject result = new JSONObject();
		OBContext.setAdminMode(true);

		String authStr = request.getHeader("Authorization");
		String token = null;
		if (authStr != null && authStr.startsWith("Bearer ")) {
			token = authStr.substring(7);
		}

		try {
			// Check if SWS is configured at first
			SWSConfig config = SWSConfig.getInstance();
			if(config.getPrivateKey() == null) {
				log.warn("SWS - SWS are misconfigured");
				throw new Exception(Utility.messageBD(new DalConnectionProvider(), "SMFSWS_Misconfigured",
						OBContext.getOBContext().getLanguage().getLanguage()));
			}

			User user = null;
			Role role = null;
			Organization org = null;
			Warehouse warehouse = null;

			if (data.has("username") && data.has("password")) {
				String username = data.getString("username");
				String pass = data.getString("password");
				String roleId = data.has("role") ? data.getString("role") : null;
				String orgId = data.has("organization") ? data.getString("organization") : null;
				String warehouseId = data.has("warehouse") ? data.getString("warehouse") : null;

				Optional<User> opUser = PasswordHash.getUserWithPassword(username,pass);
				if(opUser.isPresent()){
					user = opUser.get();
				}
				if (roleId != null)
					role = OBDal.getInstance().get(Role.class, roleId);
				if (orgId != null)
					org = OBDal.getInstance().get(Organization.class, orgId);
				if (warehouseId != null)
					warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
			} else if (token != null) {
				DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);
				if (decodedToken != null) {
					String userId = decodedToken.getClaim("user").asString();
					String roleId = data.has("role") ? data.getString("role")
							: decodedToken.getClaim("role").asString();
					String orgId = data.has("organization") ? data.getString("organization")
							: decodedToken.getClaim("organization").asString();
					String warehouseId = data.has("warehouse") ? data.getString("warehouse")
							: decodedToken.getClaim("warehouse").asString();

					user = OBDal.getInstance().get(User.class, userId);
					role = OBDal.getInstance().get(Role.class, roleId);
					org = OBDal.getInstance().get(Organization.class, orgId);
					warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);

				} else {
					log.warn("SWS - Token is not valid");
					throw new Exception(Utility.messageBD(new DalConnectionProvider(), "SMFSWS_InvalidToken",
							OBContext.getOBContext().getLanguage().getLanguage()));
				}
			} else {
				log.warn("SWS - You must specify a username and password or a valid token");
				throw new Exception(Utility.messageBD(new DalConnectionProvider(), "SMFSWS_PassOrTokenNeeded",
						OBContext.getOBContext().getLanguage().getLanguage()));
			}
			if (user != null) {
				try {
					token = SecureWebServicesUtils.generateToken(user, role, org, warehouse);
					Boolean showRoles = !("false".equals(request.getParameter("showRoles")));
					Boolean showOrgs = !("false".equals(request.getParameter("showOrgs")));
					Boolean showWarehouses = !("false".equals(request.getParameter("showWarehouses")));

					result.put("status", "success");
					result.put("token", token);
					if (showRoles) {
						JSONArray rolesAndOrgs = SecureWebServicesUtils.getUserRolesAndOrg(user, showOrgs,
								showWarehouses);
						result.put("roleList", rolesAndOrgs);
					}
				} catch (JWTCreationException exception) {
					log.warn("SWS - An error occurred while creating the token");
					throw new Exception(Utility.messageBD(new DalConnectionProvider(), "SMFSWS_ErrorCreatingToken",
							OBContext.getOBContext().getLanguage().getLanguage()));
				}

			} else {
				log.warn("SWS - Invalid user name or password.");
				throw new Exception(Utility.messageBD(new DalConnectionProvider(), "IDENTIFICATION_FAILURE_TITLE",
						OBContext.getOBContext().getLanguage().getLanguage()));
			}

		} catch (Exception e) {
			try {
				result.put("status", "error");
				String errMsg = e.getMessage();
				if (errMsg == null) {
					result.put("message", Utility.messageBD(new DalConnectionProvider(), "SMFSWS_GenericErrorLog",
							OBContext.getOBContext().getLanguage().getLanguage()));
				} else {
					result.put("message", e.getMessage());
				}
				e.printStackTrace();
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} finally {
			OBContext.restorePreviousMode();
		}

		response.setContentType(APPLICATION_JSON);
		Writer out = response.getWriter();
		out.write(result.toString());
		out.close();
	}
}
