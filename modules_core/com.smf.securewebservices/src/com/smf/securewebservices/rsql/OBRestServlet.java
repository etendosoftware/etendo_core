package com.smf.securewebservices.rsql;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.json.DefaultJsonDataService;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.web.InvalidRequestException;
import org.openbravo.service.web.ResourceNotFoundException;

import com.smf.securewebservices.service.BaseWebService;
import com.smf.securewebservices.service.SecureJsonDataService;
import com.smf.securewebservices.utils.JSONStreamWriter;
import com.smf.securewebservices.utils.Result.Type;
import com.smf.securewebservices.utils.WSResult;
import com.smf.securewebservices.utils.WSResult.ResultType;
import com.smf.securewebservices.utils.WSResult.Status;

/**
 * @author androettop
 */
public class OBRestServlet extends BaseWebService {

	@Override
	public WSResult get(String path, Map<String, String> requestParams) throws Exception {
		Map<String, String> parameters = OBRestUtils.mapRestParameters(requestParams);

		WSResult wsResult = new WSResult();

		// Entity and ID parameter
		try {
			OBRestUtils.addEntitynameToParams(path, parameters);
		} catch (OBException e) {
			wsResult.setStatus(Status.BAD_REQUEST);
			wsResult.setMessage(e.getMessage());
			return wsResult;
		}

		JSONStreamWriter writer = new JSONStreamWriter();
		try {
			SecureJsonDataService.getInstance().fetch(parameters, writer);
		} finally {
			OBContext.restorePreviousMode();
		}

		wsResult.setStatus(Status.OK);
		wsResult.setData(writer.getJSONArray());
		return wsResult;
	}

	@Override
	public WSResult post(String path, Map<String, String> requestParams, JSONObject body) throws Exception {
		Map<String, String> parameters = OBRestUtils.mapRestParameters(requestParams);

		// Entity and ID parameter
		try {
			OBRestUtils.addEntitynameToParams(path, parameters);
		} catch (OBException e) {
			WSResult wsResult = new WSResult();
			wsResult.setStatus(Status.BAD_REQUEST);
			wsResult.setMessage(e.getMessage());
			return wsResult;
		}

		// JSON data service
		final String result = SecureJsonDataService.getInstance().add(parameters, body.toString());
		return OBRestUtils.oldResponseToWSResult(result);
	}

	@Override
	public WSResult put(String path, Map<String, String> requestParams, JSONObject body) throws Exception {
		Map<String, String> parameters = OBRestUtils.mapRestParameters(requestParams);

		// Entity and ID parameter
		try {
			OBRestUtils.addEntitynameToParams(path, parameters);
		} catch (OBException e) {
			WSResult wsResult = new WSResult();
			wsResult.setStatus(Status.BAD_REQUEST);
			wsResult.setMessage(e.getMessage());
			return wsResult;
		}

		// JSON data service
		final String result = SecureJsonDataService.getInstance().update(parameters, body.toString());
		return OBRestUtils.oldResponseToWSResult(result);
	}

	@Override
	public WSResult delete(String path, Map<String, String> requestParams, JSONObject body) throws Exception {
		Map<String, String> parameters = OBRestUtils.mapRestParameters(requestParams);

		// Entity and ID parameter
		try {
			OBRestUtils.addEntitynameToParams(path, parameters);
		} catch (OBException e) {
			WSResult wsResult = new WSResult();
			wsResult.setStatus(Status.BAD_REQUEST);
			wsResult.setMessage(e.getMessage());
			wsResult.setResultType(ResultType.MULTIPLE);
			return wsResult;
		}

		String entityName = parameters.get(JsonConstants.ENTITYNAME);
		String id = parameters.get(JsonConstants.ID);
		if (id == null) {
			JSONArray data = body.getJSONArray("data");
			JSONArray dataResult = new JSONArray();
			if (data == null || data.length() == 0) {
				throw new InvalidRequestException("No id parameter");
			}
			for (int i = 0; i < data.length(); i++) {
				String currentId = data.getString(i);
				Map<String, String> removeParams = new HashMap<String, String>();
				removeParams.put(JsonConstants.ID, currentId);
				removeParams.put(JsonConstants.ENTITYNAME, parameters.get(JsonConstants.ENTITYNAME));
				WSResult deleteItemResult = OBRestUtils
						.oldResponseToWSResult(DefaultJsonDataService.getInstance().remove(removeParams));

				if (deleteItemResult.getType() == Type.ERROR) {
					return deleteItemResult;
				}
				JSONArray removeData = deleteItemResult.getJSONResponse().getJSONArray("data");
				if (removeData.length() > 0) {
					dataResult.put(removeData.getJSONObject(0));
				}
			}
			WSResult wsResult = new WSResult();
			wsResult.setStatus(Status.OK);
			wsResult.setData(dataResult);
			wsResult.setResultType(ResultType.MULTIPLE);
			return wsResult;
		} else {
			final BaseOBObject bob = OBDal.getInstance().get(entityName, id);
			if (bob == null) {
				throw new ResourceNotFoundException("Object (" + id + "/" + entityName + ") not found");
			}
			final String result = DefaultJsonDataService.getInstance().remove(parameters);
			return OBRestUtils.oldResponseToWSResult(result);
		}
	}
}
