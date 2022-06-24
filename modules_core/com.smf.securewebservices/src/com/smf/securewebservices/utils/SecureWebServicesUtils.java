package com.smf.securewebservices.utils;

import java.io.IOException;
import java.io.Writer;
import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.smf.securewebservices.SWSConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.KernelServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationTree;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.service.db.DalConnectionProvider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.smf.securewebservices.data.SMFSWSConfig;

/**
 * @author androettop
 */
public class SecureWebServicesUtils {
	private static final Logger log = LogManager.getLogger(SecureWebServicesUtils.class);

	static final long ONE_MINUTE_IN_MILLIS = 60000;


	public static List<Organization> getChildrenOrganizations(Organization org) {
		List<Organization> organizations = new ArrayList<Organization>();
		OBContext.setAdminMode();
		try {
			OBCriteria<OrganizationTree> crit = OBDal.getInstance().createCriteria(OrganizationTree.class);
			crit.add(Restrictions.eq(OrganizationTree.PROPERTY_PARENTORGANIZATION, org));
			crit.setFilterOnReadableClients(false);
			crit.setFilterOnReadableOrganization(false);
			List<OrganizationTree> orgTreeList = crit.list();
			for (OrganizationTree orgT : orgTreeList) {
				organizations.add(orgT.getOrganization());
			}
		} finally {
			OBContext.restorePreviousMode();
		}
		return organizations;
	}

	public static List<Warehouse> getOrganizationWarehouses(Organization org) {
		List<Organization> childrenOrg = getChildrenOrganizations(org);
		List<Warehouse> warehouses = null;
		OBContext.setAdminMode();
		try {
			OBCriteria<Warehouse> crit = OBDal.getInstance().createCriteria(Warehouse.class);
			crit.add(Restrictions.in(Warehouse.PROPERTY_ORGANIZATION, childrenOrg));
			crit.setFilterOnReadableClients(false);
			crit.setFilterOnReadableOrganization(false);
			warehouses = crit.list();
		} catch (Exception e) {
			warehouses = new ArrayList<Warehouse>();
		} finally {
			OBContext.restorePreviousMode();
		}
		return warehouses;
	}

	public static JSONArray getOrgWarehouses(Organization org) {
		OBContext.setAdminMode(true);
		JSONArray result = new JSONArray();
		try {
			List<Warehouse> warehouseList = SecureWebServicesUtils.getOrganizationWarehouses(org);
			for (Warehouse warehouse : warehouseList) {
				JSONObject wh = new JSONObject();
				wh.put("id", warehouse.getId());
				wh.put("name", warehouse.getName());
				result.put(wh);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			OBContext.restorePreviousMode();
		}
		return result;
	}

	public static JSONArray getRoleOrgs(Role role, Boolean showWarehouses) {
		OBContext.setAdminMode(true);
		JSONArray result = new JSONArray();
		try {
			List<RoleOrganization> roleOrgList = role.getADRoleOrganizationList();
			for (RoleOrganization roleOrg : roleOrgList) {
				JSONObject org = new JSONObject();
				org.put("id", roleOrg.getOrganization().getId());
				org.put("name", roleOrg.getOrganization().getName());
				if (showWarehouses) {
					org.put("warehouseList", getOrgWarehouses(roleOrg.getOrganization()));
				}
				result.put(org);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			OBContext.restorePreviousMode();
		}
		return result;
	}

	public static Boolean areOBObjectEquals(BaseOBObject inv1, BaseOBObject inv2, String[] properties) {
		if (inv1 == null || inv2 == null) {
			return false;
		}
		for (int i = 0; i < properties.length; i++) {
			String field = properties[i];
			Object value1 = inv1.get(field);
			Object value2 = inv2.get(field);
			if ((value1 != null && !value1.equals(value2)) || (value2 != null && !value2.equals(value1))) {
				return false;
			}
		}
		return true;
	}

	static public JSONObject createWSResponse(JSONArray data) throws JSONException {
		return createWSResponse(null, data);
	}

	static public JSONObject createWSResponse(OBError status) throws JSONException {
		return createWSResponse(status, null);
	}

	static public JSONObject createWSResponse(OBError status, JSONArray data) throws JSONException {
		return createWSResponse(status, null, null);
	}

	static public JSONObject createWSResponse(OBError status, JSONArray data, Integer totalRows) throws JSONException {
		JSONObject result = new JSONObject();
		// OBError part
		if (status != null) {
			result.put("status", status.getType());
			result.put("title", status.getTitle());
			result.put("message", status.getMessage());
		}
		// Data part
		if (data != null) {
			result.put("totalRows", totalRows == null ? data.length() : totalRows);
			result.put("data", data);
		}
		return result;
	}

	static public void writeJsonResponse(HttpServletResponse response, JSONObject json) throws IOException {
		response.setContentType("application/json;charset=UTF-8");
		response.setHeader("Content-Type", "application/json;charset=UTF-8");
		final Writer w = response.getWriter();
		w.write(json.toString());
		w.close();
	}

	static public String getExceptionMessage(Throwable t) {
		if (t.getCause() instanceof BatchUpdateException
				&& ((BatchUpdateException) t.getCause()).getNextException() != null) {
			final BatchUpdateException bue = (BatchUpdateException) t.getCause();
			return bue.getNextException().getMessage();
		}
		return t.getMessage();
	}

	static public Throwable getRootCause(Throwable t){
		if(t.getCause() == null){
			return t;
		}else{
			return getRootCause(t.getCause());
		}
	}

	public static void fillSessionVariables(HttpServletRequest request) throws ServletException {
		VariablesSecureApp vars = new VariablesSecureApp(request);
		ConnectionProvider conn = new DalConnectionProvider(false);
		OBContext context = OBContext.getOBContext();
		Warehouse warehouse = context.getWarehouse();
		LoginUtils.fillSessionArguments(conn, vars, context.getUser().getId(), context.getLanguage().getLanguage(),
				context.isRTL() ? "Y" : "N", context.getRole().getId(), context.getCurrentClient().getId(),
				context.getCurrentOrganization().getId(), warehouse != null ? warehouse.getId() : null);
		ConfigParameters globalParameters = KernelServlet.getGlobalParameters();
		if (globalParameters != null) {
			LoginUtils.readNumberFormat(vars, globalParameters.getFormatPath());
		}
	}

	public static JSONArray getUserRolesAndOrg(User user, Boolean showOrgs, Boolean showWarehouses) {
		OBContext.setAdminMode(true);
		JSONArray result = new JSONArray();
		try {
			List<UserRoles> userRoleList = user.getADUserRolesList();
			for (UserRoles userRole : userRoleList) {
				JSONObject role = new JSONObject();
				role.put("id", userRole.getRole().getId());
				role.put("name", userRole.getRole().getName());
				if (showOrgs) {
					role.put("orgList", SecureWebServicesUtils.getRoleOrgs(userRole.getRole(), showWarehouses));
				}
				result.put(role);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			OBContext.restorePreviousMode();
		}
		return result;
	}

	public static OBContext createContext(String userId, String roleId, String orgId, String warehouseId,
			String clientId) {
		OBContext.setOBContext(userId, roleId, clientId, orgId, null, warehouseId);
		return OBContext.getOBContext();
	}

	public static DecodedJWT decodeToken(String token) throws Exception {
		SWSConfig config = SWSConfig.getInstance();
		Algorithm algorithm = Algorithm.HMAC256(config.getPrivateKey());
		JWTVerifier verifier = JWT.require(algorithm).withIssuer("sws").build();
		DecodedJWT jwt = verifier.verify(token);
		return jwt;
	}

	public static String generateToken(User user) throws Exception {
		return generateToken(user, null, null, null);
	}

	public static String generateToken(User user, Role role) throws Exception {
		return generateToken(user, role, null, null);
	}

	public static String generateToken(User user, Role role, Organization org) throws Exception {
		return generateToken(user, role, org, null);
	}

	public static String generateToken(User user, Role role, Organization org, Warehouse warehouse) throws Exception {
		OBContext.setAdminMode(true);
		try {
			SWSConfig config = SWSConfig.getInstance();

			Role selectedRole = null;
			Organization selectedOrg = null;
			Warehouse selectedWarehouse = null;

			List<UserRoles> userRoleList = user.getADUserRolesList();
			Role defaultWsRole = user.getSmfswsDefaultWsRole();
			Role defaultRole = user.getDefaultRole();
			Organization defaultOrg = user.getDefaultOrganization();
			Warehouse defaultWarehouse = user.getDefaultWarehouse();

			if (role != null)
				for (UserRoles userRole : userRoleList) {
					if (userRole.getRole().getId().equals(role.getId())) {
						selectedRole = role;
						break;
					}
				}

			// if user has
			if (selectedRole == null) {
				if (defaultWsRole != null) {
					selectedRole = defaultWsRole;
				} else if (defaultRole != null) {
					selectedRole = defaultRole;
				} else if (userRoleList.size() > 0) {
					selectedRole = userRoleList.get(0).getRole();
				} else {
					log.error("SWS - The selected user (\"" + user.getId() + "\") has no roles");
					throw new Exception(Utility.messageBD(new DalConnectionProvider(), "SMFSWS_UserHasNoRole",
							OBContext.getOBContext().getLanguage().getLanguage()));
				}
			}
			List<RoleOrganization> roleOrgList = selectedRole.getADRoleOrganizationList();
			// if organization is valid, select
			if (org != null)
				for (RoleOrganization roleOrg : roleOrgList) {
					if (roleOrg.getOrganization().getId().equals(org.getId())) {
						selectedOrg = org;
						break;
					}
				}
			// if not valid select default org for the selected role
			if (selectedOrg == null) {
				if (defaultRole != null && defaultRole.getId().equals(selectedRole.getId()) && defaultOrg != null) {
					selectedOrg = defaultOrg;
				} else if (roleOrgList.size() > 0) {
					selectedOrg = roleOrgList.get(0).getOrganization();
				} else {
					log.error("SWS - The selected role (\"" + selectedRole.getId() + "\") has no organization");
					throw new Exception(Utility.messageBD(new DalConnectionProvider(), "SMFSWS_RoleHasNoOrg",
							OBContext.getOBContext().getLanguage().getLanguage()));
				}
			}

			List<Warehouse> warehouseList = SecureWebServicesUtils.getOrganizationWarehouses(selectedOrg);
			// if warehouse is valid, select
			if (warehouse != null)
				for (Warehouse wh : warehouseList) {
					if (wh.getId().equals(warehouse.getId())) {
						selectedWarehouse = warehouse;
						break;
					}
				}
			// if not valid select default warehouse for the selected org
			if (selectedWarehouse == null) {
				if (defaultWarehouse != null) {
					selectedWarehouse = defaultWarehouse;
				} else if (warehouseList.size() > 0) {
					selectedWarehouse = warehouseList.get(0);
				} else {
					log.error("SWS - The selected organization (\"" + selectedOrg.getId() + "\") has no warehouses");
					throw new Exception(Utility.messageBD(new DalConnectionProvider(), "SMFSWS_OrgHasNoRole",
							OBContext.getOBContext().getLanguage().getLanguage()));
				}
			}

			Algorithm algorithm = Algorithm.HMAC256(config.getPrivateKey());
			Builder jwtBuilder = JWT.create().withIssuer("sws").withAudience("sws").withClaim("user", user.getId())
					.withClaim("role", selectedRole.getId()).withClaim("organization", selectedOrg.getId())
					.withClaim("warehouse", selectedWarehouse.getId())
					.withClaim("client", selectedRole.getClient().getId()).withIssuedAt(new Date());

			if (config.getExpirationTime() > 0) {
				Calendar date = Calendar.getInstance();
				long t = date.getTimeInMillis();
				Date expirationDate = new Date(t + (config.getExpirationTime() * ONE_MINUTE_IN_MILLIS));
				jwtBuilder = jwtBuilder.withExpiresAt(expirationDate);
			}

			return jwtBuilder.sign(algorithm);
		} finally {
			OBContext.restorePreviousMode();
		}
	}

}