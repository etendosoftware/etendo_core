package com.smf.securewebservices.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;

import com.smf.securewebservices.utils.SecureWebServicesUtils;
import com.smf.securewebservices.utils.WSResult;
import com.smf.securewebservices.utils.WSResult.ResultType;
import com.smf.securewebservices.utils.WSResult.Status;

/**
 * @author androettop
 */
public class ContextInfoServlet extends BaseWebService {

	@Override
	public WSResult get(String path, Map<String, String> parameters) throws Exception {
		JSONObject result = new JSONObject();
		Client client = OBContext.getOBContext().getCurrentClient();
		Role role = OBContext.getOBContext().getRole();
		Organization org = OBContext.getOBContext().getCurrentOrganization();
		User user = OBContext.getOBContext().getUser();
		Language lang = OBContext.getOBContext().getLanguage();
		Warehouse warehouse = OBContext.getOBContext().getWarehouse();
		String userLevel = OBContext.getOBContext().getUserLevel();
		String[] rclients = OBContext.getOBContext().getReadableClients();
		String[] rorgs = OBContext.getOBContext().getReadableOrganizations();
		Set<String> worgs = OBContext.getOBContext().getWritableOrganizations();
		JSONArray readableClients = new JSONArray();
		JSONArray readableOrganizations = new JSONArray();
		JSONArray writableOrganizations = new JSONArray();

		for (String rclient : rclients) {
			readableClients.put(rclient);
		}
		for (String rorg : rorgs) {
			readableOrganizations.put(rorg);
		}
		for (String worg : worgs) {
			writableOrganizations.put(worg);
		}

		result.put("client", client != null ? client.getId() : JSONObject.NULL);
		result.put("role", role != null ? role.getId() : JSONObject.NULL);
		result.put("organization", org != null ? org.getId() : JSONObject.NULL);

		List<Organization> childrenOrgs = SecureWebServicesUtils.getChildrenOrganizations(org);
		JSONArray jsonChildrenOrgs = new JSONArray();
		for (Organization organization : childrenOrgs) {
			jsonChildrenOrgs.put(organization.getId());
		}
		result.put("childrenOrganization", jsonChildrenOrgs);

		result.put("user", user != null ? user.getId() : JSONObject.NULL);
		result.put("language", lang != null ? lang.getLanguage() : JSONObject.NULL);
		result.put("warehouse", warehouse != null ? warehouse.getId() : JSONObject.NULL);
		result.put("readableClients", readableClients);
		result.put("readableOrganizations", readableOrganizations);
		result.put("writableOrganizations", writableOrganizations);
		result.put("userLevel", userLevel);

		WSResult wsResult = new WSResult();
		wsResult.setStatus(Status.OK);
		wsResult.setData(result);
		wsResult.setResultType(ResultType.SINGLE);
		return wsResult;
	}

	@Override
	public WSResult post(String path, Map<String, String> parameters, JSONObject body) throws Exception {
		return null;
	}

	@Override
	public WSResult put(String path, Map<String, String> parameters, JSONObject body) throws Exception {
		return null;
	}

	@Override
	public WSResult delete(String path, Map<String, String> parameters, JSONObject body) throws Exception {
		return null;
	}

}
