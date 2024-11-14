package com.smf.securewebservices.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smf.securewebservices.SWSConfig;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.KernelServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationTree;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.service.db.DalConnectionProvider;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.PrivateKey;
import java.security.spec.X509EncodedKeySpec;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

/**
 * @author androettop
 */
public class SecureWebServicesUtils {
	private static final Logger log = LogManager.getLogger(SecureWebServicesUtils.class);

	static final long ONE_MINUTE_IN_MILLIS = 60000;
	private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
	private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";
	private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
	private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
	private static final String ES256_ALGORITHM = "ES256";
	private static final String PRIVATE_KEY = "private-key";
	private static final String PUBLIC_KEY = "public-key";
	private static final String EC = "EC";
	private static final String HS256_ALGORITHM = "HS256";

	/**
	 * Constructor for SecureWebServicesUtils.
	 * This constructor is private to prevent instantiation of this utility class.
	 * Attempting to instantiate this class will result in an IllegalStateException.
	 */
	private SecureWebServicesUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Retrieves the list of child organizations for a given organization.
	 *
	 * @param org The parent organization for which to retrieve the child organizations.
	 * @return A list of child organizations associated with the given parent organization.
	 */
	public static List<Organization> getChildrenOrganizations(Organization org) {
		List<Organization> organizations = new ArrayList<>();
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

	/**
	 * Retrieves the list of warehouses associated with a given organization and its child organizations.
	 *
	 * @param org The organization for which to retrieve the associated warehouses.
	 * @return A list of warehouses associated with the given organization and its child organizations.
	 */
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
			warehouses = new ArrayList<>();
		} finally {
			OBContext.restorePreviousMode();
		}
		return warehouses;
	}

	/**
	 * Retrieves the list of warehouses associated with a given organization.
	 *
	 * @param org The organization for which to retrieve the associated warehouses.
	 * @return A JSONArray containing the warehouses associated with the given organization.
	 *         Each warehouse is represented as a JSONObject with its ID and name.
	 */
	public static JSONArray getOrgWarehouses(Organization org) {
		OBContext.setAdminMode(true);
		JSONArray result = new JSONArray();
		try {
			// Retrieve the list of warehouses associated with the given organization
			List<Warehouse> warehouseList = SecureWebServicesUtils.getOrganizationWarehouses(org);
			for (Warehouse warehouse : warehouseList) {
				JSONObject wh = new JSONObject();
				wh.put("id", warehouse.getId());
				wh.put("name", warehouse.getName());
				result.put(wh);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			OBContext.restorePreviousMode();
		}
		return result;
	}

	/**
	 * Retrieves the organizations associated with a given role and optionally includes their warehouses.
	 *
	 * @param role The role for which to retrieve the associated organizations.
	 * @param showWarehouses A boolean flag indicating whether to include the warehouses for each organization.
	 * @return A JSONArray containing the organizations associated with the given role. Each organization is represented as a JSONObject with its ID, name, and optionally its warehouses.
	 */
	public static JSONArray getRoleOrgs(Role role, boolean showWarehouses) {
		OBContext.setAdminMode(true);
		JSONArray result = new JSONArray();
		try {
			// Retrieve the list of organizations associated with the given role
			List<RoleOrganization> roleOrgList = role.getADRoleOrganizationList();
			for (RoleOrganization roleOrg : roleOrgList) {
				JSONObject org = new JSONObject();
				org.put("id", roleOrg.getOrganization().getId());
				org.put("name", roleOrg.getOrganization().getName());
				// If showWarehouses is true, include the list of warehouses for each organization
				if (showWarehouses) {
					org.put("warehouseList", getOrgWarehouses(roleOrg.getOrganization()));
				}
				result.put(org);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			OBContext.restorePreviousMode();
		}
		return result;
	}

	public static Boolean areOBObjectEquals(BaseOBObject inv1, BaseOBObject inv2, String[] properties) {
		if (inv1 == null || inv2 == null) {
			return false;
		}
    for (String field : properties) {
      Object value1 = inv1.get(field);
      Object value2 = inv2.get(field);
      if ((value1 != null && !value1.equals(value2)) || (value2 != null && !value2.equals(value1))) {
        return false;
      }
    }
		return true;
	}

	/**
	 * Creates a JSON response object with the given data.
	 *
	 * @param data The data to be included in the response.
	 * @return A JSONObject containing the response data.
	 * @throws JSONException If there is an error creating the JSON object.
	 */
	public static JSONObject createWSResponse(JSONArray data) throws JSONException {
		return createWSResponse(null, data);
	}

	/**
	 * Creates a JSON response object with the given status.
	 *
	 * @param status The status to be included in the response.
	 * @return A JSONObject containing the response status.
	 * @throws JSONException If there is an error creating the JSON object.
	 */
	public static JSONObject createWSResponse(OBError status) throws JSONException {
		return createWSResponse(status, null);
	}

	/**
	 * Creates a JSON response object with the given status and data.
	 *
	 * @param status The status to be included in the response.
	 * @param data The data to be included in the response.
	 * @return A JSONObject containing the response status and data.
	 * @throws JSONException If there is an error creating the JSON object.
	 */
	public static JSONObject createWSResponse(OBError status, JSONArray data) throws JSONException {
		return createWSResponse(status, data, null);
	}

	/**
	 * Creates a JSON response object with the given status, data, and total rows.
	 *
	 * @param status The status to be included in the response.
	 * @param data The data to be included in the response.
	 * @param totalRows The total number of rows to be included in the response. If null, the length of the data array is used.
	 * @return A JSONObject containing the response status, data, and total rows.
	 * @throws JSONException If there is an error creating the JSON object.
	 */
	public static JSONObject createWSResponse(OBError status, JSONArray data, Integer totalRows) throws JSONException {
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

	/**
	 * Writes a JSON response to the HttpServletResponse.
	 *
	 * @param response The HttpServletResponse to which the JSON response will be written.
	 * @param json The JSONObject containing the response data.
	 * @throws IOException If an input or output exception occurs.
	 */
	public static void writeJsonResponse(HttpServletResponse response, JSONObject json) throws IOException {
		response.setContentType("application/json;charset=UTF-8");
		response.setHeader("Content-Type", "application/json;charset=UTF-8");
		final Writer w = response.getWriter();
		w.write(json.toString());
		w.close();
	}

	/**
	 * Retrieves the exception message from a Throwable.
	 * If the cause of the Throwable is a BatchUpdateException and it has a next exception,
	 * the message from the next exception is returned. Otherwise, the message from the Throwable is returned.
	 *
	 * @param t The Throwable from which to retrieve the exception message.
	 * @return The exception message as a String.
	 */
	public static String getExceptionMessage(Throwable t) {
		if (t.getCause() instanceof BatchUpdateException
				&& ((BatchUpdateException) t.getCause()).getNextException() != null) {
			final BatchUpdateException bue = (BatchUpdateException) t.getCause();
			return bue.getNextException().getMessage();
		}
		return t.getMessage();
	}

	/**
	 * Retrieves the root cause of a given Throwable.
	 * This method recursively traverses the cause chain of the Throwable
	 * until it finds the root cause (the Throwable with no cause).
	 *
	 * @param t The Throwable for which to retrieve the root cause.
	 * @return The root cause of the given Throwable.
	 */
	public static Throwable getRootCause(Throwable t){
		if (t.getCause() == null) {
			return t;
		} else {
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

	/**
 * Retrieves the roles and associated organizations for a given user.
 *
 * @param user The user for whom to retrieve the roles and organizations.
 * @param showOrgs A boolean flag indicating whether to include the organizations for each role.
 * @param showWarehouses A boolean flag indicating whether to include the warehouses for each organization.
 * @return A JSONArray containing the roles and associated organizations for the given user.
 *         Each role is represented as a JSONObject with its ID, name, and optionally its organizations and warehouses.
 */
public static JSONArray getUserRolesAndOrg(User user, boolean showOrgs, boolean showWarehouses) {
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
		log.error(e.getMessage(), e);
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

	/**
	 * Decodes and verifies a JWT token using the appropriate algorithm.
	 * This method extracts the token header, determines the signing algorithm (either ES256 or HS256),
	 * and verifies the token using the configured public key for ES256 or the private key for HS256.
	 *
	 * @param token The JWT token to be decoded and verified.
	 * @return The decoded {@link DecodedJWT} object containing the claims from the token.
	 * @throws JsonProcessingException If there is an issue parsing the JSON header.
	 * @throws JSONException If there is an issue parsing the public key JSON object.
	 * @throws UnsupportedEncodingException If there is an issue decoding the public key.
	 * @throws NoSuchAlgorithmException If the ES256 algorithm is not available in the environment.
	 * @throws InvalidKeySpecException If the public key specification is invalid.
	 */
	public static DecodedJWT decodeToken(String token)
			throws JsonProcessingException, JSONException, UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException {
		String[] tokenParts = token.split("\\.");
		if (tokenParts.length < 2) {
			throw new IllegalArgumentException(OBMessageUtils.messageBD("SMFSWS_InvalidToken"));
		}
		final String algorithmUsed = getAlgorithmUsed(tokenParts);
		SWSConfig config = SWSConfig.getInstance();
		Algorithm algorithm = getDecoderAlgorithm(config, algorithmUsed);
		JWTVerifier verifier = JWT.require(algorithm)
				.withIssuer("sws")
				.build();
		return verifier.verify(token);
	}

	/**
	 * Extracts the algorithm used to sign the JWT token from the token header.
	 * This method decodes the token header and retrieves the "alg" claim to determine the algorithm used.
	 *
	 * @param tokenParts The array of token parts, split by the period character.
	 * @return The algorithm used to sign the token.
	 * @throws JsonProcessingException If there is an issue parsing the JSON header.
	 */
	private static String getAlgorithmUsed(String[] tokenParts) throws JsonProcessingException {
		String headerJson = new String(Base64.getUrlDecoder().decode(tokenParts[0]));
		ObjectMapper mapper = new ObjectMapper();
		JsonNode header = mapper.readTree(headerJson);
		return header.get("alg").asText();
	}

	/**
	 * Determines the appropriate algorithm to use for decoding the JWT token.
	 * This method retrieves the public or private key from the SWSConfig configuration and selects the
	 * appropriate algorithm based on the configuration setting. It supports both ES256 and HS256 algorithms.
	 *
	 * @param config The SWSConfig instance containing the public or private key configuration.
	 * @param algorithmUsed The algorithm used to sign the token.
	 * @return The {@link Algorithm} to be used for decoding the token.
	 * @throws JSONException If there is an issue parsing the public key JSON object.
	 * @throws NoSuchAlgorithmException If the ES256 algorithm is not available in the environment.
	 * @throws InvalidKeySpecException If the public key specification is invalid.
	 * @throws UnsupportedEncodingException If there is an issue decoding the public key.
	 */
	private static Algorithm getDecoderAlgorithm(SWSConfig config,
			String algorithmUsed) throws JSONException, NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
		String publicKeyContent = config.getPrivateKey();
		boolean isNewVersion = StringUtils.startsWith(publicKeyContent, "{") && StringUtils.endsWith(publicKeyContent, "}");
		if (isNewVersion) {
			JSONObject keys = new JSONObject(publicKeyContent);
			publicKeyContent = StringUtils.equals(ES256_ALGORITHM, algorithmUsed) ? keys.getString(PUBLIC_KEY) : keys.getString(PRIVATE_KEY);
		}

		Algorithm algorithm;
		if (isNewVersion && StringUtils.equals(ES256_ALGORITHM, algorithmUsed)) {
			final ECPublicKey publicKey = getECPublicKey(publicKeyContent);
			algorithm = Algorithm.ECDSA256(publicKey);
		} else if (!isNewVersion || StringUtils.equals(HS256_ALGORITHM, algorithmUsed)) {
			algorithm = Algorithm.HMAC256(publicKeyContent);
		} else {
			String errorMessage = String.format(OBMessageUtils.messageBD("SMFSWS_UnsupportedSigningAlgorithm"), algorithmUsed);
			throw new IllegalArgumentException(errorMessage);
		}
		return algorithm;
	}

	/**
	 * Retrieves the ES256 public key from the SWSConfig configuration.
	 * This method decodes the public key in PEM format, removes the header and footer,
	 * and then converts it to an {@link ECPublicKey} object using the ES256 algorithm.
	 *
	 * @param publicKey The string that contains the public key in PEM format.
	 * @return The {@link ECPublicKey} to be used for verifying JWT tokens or other cryptographic operations.
	 * @throws NoSuchAlgorithmException If the ES256 algorithm is not available in the environment.
	 * @throws InvalidKeySpecException If the public key specification is invalid.
	 * @throws IllegalStateException If the public key is not configured or is blank.
	 */
	public static ECPublicKey getECPublicKey(String publicKey) throws NoSuchAlgorithmException,
			InvalidKeySpecException {
		if (StringUtils.isBlank(publicKey)) {
			throw new IllegalStateException(OBMessageUtils.messageBD("SMFSWS_PublicKeyNotConfigured"));
		}
		KeyFactory keyFactory = KeyFactory.getInstance(EC);

		String publicKeyPEM = publicKey.replace(BEGIN_PUBLIC_KEY, "")
				.replace(END_PUBLIC_KEY, "")
				.replaceAll("\\s+", "");

		byte[] publicBytes = Base64.getDecoder().decode(publicKeyPEM);
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicBytes);
		return (ECPublicKey) keyFactory.generatePublic(publicKeySpec);
	}

	/**
	 * Generates a JSON Web Token (JWT) for a given user with default role, organization, and warehouse details.
	 * This method dynamically selects the token signing algorithm based on the configuration setting.
	 * It supports both ECDSA and HMAC algorithms for signing the token.
	 *
	 * @param user The {@link User} object representing the user for whom the token is being generated.
	 * @return A signed JWT as a {@link String}.
	 * @throws Exception If there are any issues during token generation, such as missing roles, organizations, or warehouse information,
	 *                   or issues with key generation or token signing.
	 */
	public static String generateToken(User user) throws Exception {
		return generateToken(user, null, null, null);
	}

	/**
	 * Generates a JSON Web Token (JWT) for a given user with specified role and default organization and warehouse details.
	 * This method dynamically selects the token signing algorithm based on the configuration setting.
	 * It supports both ECDSA and HMAC algorithms for signing the token. It also handles role selection based on the input parameter
	 * or defaults when a specific role is not defined.
	 *
	 * @param user The {@link User} object representing the user for whom the token is being generated.
	 * @param role The {@link Role} to be associated with the user. If null, the user's default or first role is used.
	 * @return A signed JWT as a {@link String}.
	 * @throws Exception If there are any issues during token generation, such as missing roles, organizations, or warehouse information,
	 *                   or issues with key generation or token signing.
	 */
	public static String generateToken(User user, Role role) throws Exception {
		return generateToken(user, role, null, null);
	}

	/**
	 * Generates a JSON Web Token (JWT) for a given user with specified role and organization details.
	 * This method dynamically selects the token signing algorithm based on the configuration setting.
	 * It supports both ECDSA and HMAC algorithms for signing the token. It also handles role and organization selection
	 * based on the input parameters or defaults when specific entities are not defined.
	 *
	 * @param user The {@link User} object representing the user for whom the token is being generated.
	 * @param role The {@link Role} to be associated with the user. If null, the user's default or first role is used.
	 * @param org The {@link Organization} to be associated with the user. If null, the user's default or first organization is used.
	 * @return A signed JWT as a {@link String}.
	 * @throws Exception If there are any issues during token generation, such as missing roles, organizations, or warehouse information,
	 *                   or issues with key generation or token signing.
	 */
	public static String generateToken(User user, Role role, Organization org) throws Exception {
		return generateToken(user, role, org, null);
	}

	/**
	 * Generates a JSON Web Token (JWT) for a given user with specified role, organization, and warehouse details.
	 * This method dynamically selects the token signing algorithm based on the configuration setting.
	 * It supports both ECDSA and HMAC algorithms for signing the token. It also handles role, organization,
	 * and warehouse selection based on the input parameters or defaults when specific entities are not defined.
	 *
	 * @param user The {@link User} object representing the user for whom the token is being generated.
	 * @param role The {@link Role} to be associated with the user. If null, the user's default or first role is used.
	 * @param org The {@link Organization} to be associated with the user. If null, the user's default or first organization is used.
	 * @param warehouse The {@link Warehouse} to be associated with the organization. If null, the organization's default or first warehouse is used.
	 * @return A signed JWT as a {@link String}.
	 * @throws Exception If there are any issues during token generation, such as missing roles, organizations, or warehouse information,
	 *                   or issues with key generation or token signing.
	 */
	public static String generateToken(User user, Role role, Organization org, Warehouse warehouse) throws Exception {
		try {
			OBContext.setAdminMode(true);
			SWSConfig config = SWSConfig.getInstance();

			Role selectedRole = null;
			Warehouse selectedWarehouse = null;
			List<UserRoles> userRoleList = user.getADUserRolesList();
			Role defaultWsRole = user.getSmfswsDefaultWsRole();
			Role defaultRole = user.getDefaultRole();
			Organization defaultOrg = user.getDefaultOrganization();
			Warehouse defaultWarehouse = user.getDefaultWarehouse();

			selectedRole = getRole(role, userRoleList, defaultWsRole, defaultRole);
			Organization selectedOrg = getOrganization(org, selectedRole, defaultRole, defaultOrg);
			selectedWarehouse = getWarehouse(warehouse, selectedOrg, selectedWarehouse, defaultWarehouse);

			final Algorithm algorithm = getEncoderAlgorithm(config);

			Builder jwtBuilder = getJwtBuilder(user, selectedRole, selectedOrg, selectedWarehouse);

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

	/**
	 * Determines the appropriate algorithm to use for encoding the JWT token.
	 * This method retrieves the private key from the SWSConfig configuration and selects the
	 * appropriate algorithm based on the configuration setting. It supports both ES256 and HS256 algorithms.
	 *
	 * @param config The SWSConfig instance containing the private key configuration.
	 * @return The {@link Algorithm} to be used for encoding the token.
	 * @throws JSONException If there is an issue parsing the private key JSON object.
	 * @throws NoSuchAlgorithmException If the ES256 algorithm is not available in the environment.
	 * @throws InvalidKeySpecException If the private key specification is invalid.
	 * @throws UnsupportedEncodingException If there is an issue decoding the private key.
	 */
	private static Algorithm getEncoderAlgorithm(SWSConfig config)
      throws JSONException, NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException, PropertyException {
		Algorithm algorithm;

		String privateKeyContent = config.getPrivateKey();
		boolean isNewVersion = StringUtils.startsWith(privateKeyContent, "{") && StringUtils.endsWith(privateKeyContent, "}");
		if (isNewVersion) {
			JSONObject keys = new JSONObject(privateKeyContent);
			privateKeyContent = keys.getString(PRIVATE_KEY);
		}

		String value = Preferences.getPreferenceValue("SMFSWS_EncryptionAlgorithm", true,
				OBContext.getOBContext().getCurrentClient(),
				OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(),
				null);
		if (isNewVersion && StringUtils.equals(ES256_ALGORITHM, value)) {
			final PrivateKey privateKey = getECPrivateKey(privateKeyContent);
			algorithm = Algorithm.ECDSA256((ECPrivateKey) privateKey);
		} else {
			algorithm = Algorithm.HMAC256(privateKeyContent);
		}
		return algorithm;
	}

	/**
	 * Determines the appropriate warehouse for a user based on the provided organization and defaults.
	 * The method attempts to select the warehouse specified in the input. If the provided warehouse
	 * is invalid or null, it selects the default warehouse for the organization. If no default warehouse
	 * is available, it selects the first warehouse available for the organization.
	 *
	 * @param warehouse The provided {@link Warehouse} to be validated.
	 * @param selectedOrg The {@link Organization} associated with the warehouse.
	 * @param selectedWarehouse The warehouse selected during processing, initially null.
	 * @param defaultWarehouse The default {@link Warehouse} to be used if the provided warehouse is not valid.
	 * @return The selected {@link Warehouse}.
	 * @throws OBException If the organization has no available warehouses.
	 */
	private static Warehouse getWarehouse(Warehouse warehouse, Organization selectedOrg, Warehouse selectedWarehouse,
			Warehouse defaultWarehouse) {
		List<Warehouse> warehouseList = SecureWebServicesUtils.getOrganizationWarehouses(selectedOrg);
		// if warehouse is valid, select
		if (warehouse != null)
			for (Warehouse wh : warehouseList) {
				if (StringUtils.equals(wh.getId(), warehouse.getId())) {
					selectedWarehouse = warehouse;
					break;
				}
			}
		// if not valid select default warehouse for the selected org
		if (selectedWarehouse == null) {
			if (defaultWarehouse != null) {
				selectedWarehouse = defaultWarehouse;
			} else if (!warehouseList.isEmpty()) {
				selectedWarehouse = warehouseList.get(0);
			} else {
				String errorMessage = String.format("SWS - The selected organization (\"%s\") has no warehouses", selectedOrg.getId());
				log.error(errorMessage);
				throw new OBException(Utility.messageBD(new DalConnectionProvider(), "SMFSWS_OrgHasNoRole",
						OBContext.getOBContext().getLanguage().getLanguage()));
			}
		}
		return selectedWarehouse;
	}

	/**
	 * Determines the appropriate organization for a user based on the provided role and defaults.
	 * The method attempts to select the organization specified in the input. If the provided organization
	 * is invalid or null, it selects the default organization for the role. If no default organization
	 * is available, it selects the first available organization for the role.
	 *
	 * @param org The provided {@link Organization} to be validated.
	 * @param selectedRole The {@link Role} associated with the organization.
	 * @param defaultRole The default {@link Role} to use if the provided role is not valid.
	 * @param defaultOrg The default {@link Organization} to use if the provided organization is not valid.
	 * @return The selected {@link Organization}.
	 * @throws OBException If the role has no associated organizations.
   */
	private static Organization getOrganization(Organization org, Role selectedRole,
			Role defaultRole, Organization defaultOrg) throws OBException {
		Organization selectedOrg = null;
		List<RoleOrganization> roleOrgList = selectedRole.getADRoleOrganizationList();
		// if organization is valid, select
		if (org != null)
			for (RoleOrganization roleOrg : roleOrgList) {
				if (StringUtils.equals(roleOrg.getOrganization().getId(), org.getId())) {
					selectedOrg = org;
					break;
				}
			}
		// if not valid select default org for the selected role
		if (selectedOrg == null) {
			if (defaultRole != null && StringUtils.equals(defaultRole.getId(), selectedRole.getId()) && defaultOrg != null) {
				selectedOrg = defaultOrg;
			} else if (!roleOrgList.isEmpty()) {
				selectedOrg = roleOrgList.get(0).getOrganization();
			} else {
				String errorMessage = String.format("SWS - The selected role (\"%s\") has no organization", selectedRole.getId());
				log.error(errorMessage);
				throw new OBException(Utility.messageBD(new DalConnectionProvider(), "SMFSWS_RoleHasNoOrg",
						OBContext.getOBContext().getLanguage().getLanguage()));
			}
		}
		return selectedOrg;
	}

	/**
	 * Determines the appropriate role for a user based on the provided role and defaults.
	 * The method attempts to select the role specified in the input. If the provided role is invalid or
	 * null, it selects the user's default or first available role.
	 *
	 * @param role The provided {@link Role} to be validated.
	 * @param userRoleList The list of {@link UserRoles} associated with the user.
	 * @param defaultWsRole The default web service role to be used if the provided role is not valid.
	 * @param defaultRole The default {@link Role} to be used if the provided role is not valid.
	 * @return The selected {@link Role}.
	 * @throws Exception If no valid role can be determined for the user.
	 */
	private static Role getRole(Role role, List<UserRoles> userRoleList, Role defaultWsRole,
			Role defaultRole) throws Exception {
		Role selectedRole;
		if (role != null) {
			selectedRole = userRoleList.stream()
					.filter(userRole -> StringUtils.equals(userRole.getRole().getId(), role.getId()))
					.findFirst()
					.map(UserRoles::getRole)
					.orElseThrow(() -> new Exception("The user has no roles"));
		} else {
			if (defaultWsRole != null) {
				selectedRole = defaultWsRole;
			} else if (defaultRole != null) {
				selectedRole = defaultRole;
			} else if (!userRoleList.isEmpty()) {
				selectedRole = userRoleList.get(0).getRole();
			} else {
				throw new OBException("User has no roles defined");
			}
		}
		return selectedRole;
	}

	/**
	 * Retrieves the private key from the SWSConfig configuration.
	 * This method decodes the private key in PEM format, removes the header and footer,
	 * and then converts it to a {@link PrivateKey} object using the ES256 algorithm.
	 *
	 * @param privateKey The string that contains the private key in PEM format.
	 * @return The {@link PrivateKey} used for signing.
	 * @throws NoSuchAlgorithmException If the ES256 algorithm is not available in the environment.
	 * @throws InvalidKeySpecException If the private key specification is invalid.
	 */
	static PrivateKey getECPrivateKey(String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeyFactory keyFactory = KeyFactory.getInstance(EC);

		String replacedKey = privateKey.replace(BEGIN_PRIVATE_KEY, "")
				.replace(END_PRIVATE_KEY, "")
				.replaceAll("\\s+", "");

		byte[] privateBytes = Base64.getDecoder().decode(replacedKey);
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateBytes);
		return keyFactory.generatePrivate(privateKeySpec);
	}

	/**
	 * Creates a JWT builder with the standard claims for a user, role, organization, and warehouse.
	 * This method constructs a JWT with the provided information, such as the user ID, role, organization,
	 * and warehouse, and sets the issuer and audience to "sws".
	 *
	 * @param user The user for whom the token is being generated.
	 * @param selectedRole The role assigned to the user.
	 * @param selectedOrg The organization associated with the user.
	 * @param selectedWarehouse The warehouse associated with the organization.
	 * @return A {@link Builder} for the JWT with pre-set claims.
	 */
	private static Builder getJwtBuilder(User user, Role selectedRole, Organization selectedOrg,
			Warehouse selectedWarehouse) {
		return JWT.create()
				.withIssuer("sws")
				.withAudience("sws")
				.withClaim("user", user.getId())
				.withClaim("client", selectedRole.getClient().getId())
				.withClaim("role", selectedRole.getId())
				.withClaim("organization", selectedOrg.getId())
				.withClaim("warehouse", selectedWarehouse.getId())
				.withIssuedAt(new Date());
	}
}
