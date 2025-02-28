package com.smf.securewebservices.rsql;

import com.smf.securewebservices.SWSDataSourceService;
import com.smf.securewebservices.utils.WSResult;
import com.smf.securewebservices.utils.WSResult.ResultType;
import com.smf.securewebservices.utils.WSResult.Status;
import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.util.CheckException;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.web.WebServiceUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.util.*;

/**
 * @author androettop
 */
public class OBRestUtils {
	public enum Operators {
		CONTAINS("contains", "=c=", false), //
		CONTAINSFIELD("containsField", "=cf=", false), //
		ENDSWITH("endsWith", "=ew=", false), //
		ENDSWITHFIELD("endsWithField", "=ewf=", false), //
		EQUALS("equals", "==", false), //
		EQUALSFIELD("equalsField", "=ef=", false), //
		EXISTS("exists", "=exists=", false), //
		GREATEROREQUAL("greaterOrEqual", ">=", false), //
		GREATEROREQUALFIELD("greaterOrEqualField", "=gef=", false), //
		GREATERTHAN("greaterThan", ">", false), //
		GREATERTHANFIELD("greaterThanField", "=gtf=", false), //
		ICONTAINS("iContains", "=ic=", false), //
		IENDSWITH("iEndsWith", "=iew=", false), //
		IEQUALS("iEquals", "=ie=", false), //
		IGREATEROREQUAL("iGreaterOrEqual", "=ige=", false), //
		IGREATERTHAN("iGreaterThan", "=igt=", false), //
		ILESSOREQUAL("iLessOrEqual", "=ile=", false), //
		ILESSTHAN("iLessThan", "=ilt=", false), //
		INOTCONTAINS("iNotContains", "=inc=", false), //
		INOTENDSWITH("iNotEndsWith", "=inew=", false), //
		INOTEQUAL("iNotEqual", "=ine=", false), //
		INOTSTARTSWITH("iNotStartsWith", "=insw=", false), //
		INSET("inSet", "=ins=", true), //
		ISTARTSWITH("iStartsWith", "=isw=", false), //
		LESSOREQUAL("lessOrEqual", "<=", false), //
		LESSTHAN("lessThan", "<", false), //
		LESSTHANFIELD("lessThanField", "=ltf=", false), //
		NOTCONTAINS("notContains", "=nc=", false), //
		NOTENDSWITH("notEndsWith", "=new=", false), //
		NOTEQUAL("notEqual", "!=", false), //
		NOTEQUALFIELD("notEqualField", "=nef=", false), //
		NOTINSET("notInSet", "=nis=", true), //
		NOTSTARTSWITH("notStartsWith", "=nsw=", false), //
		STARTSWITH("startsWith", "=sw=", false), //
		STARTSWITHFIELD("startsWithField", "=swf=", false),

		ISNULL("isNull", "=is=", false),
		NOTNULL("notNull", "=isnot=", false);

		private final String name;
		private final String symbol;
		private final boolean multiValue;
		static public final Map<String, Operators> data = new HashMap<String, Operators>();

		static {
			for (Operators op : Operators.values()) {
				data.put(op.getSymbol(), op);
			}
		}

		Operators(String name, String symbol, boolean multiValue) {
			this.name = name;
			this.symbol = symbol;
			this.multiValue = multiValue;
		}

		public String getName() {
			return name;
		}

		public String getSymbol() {
			return symbol;
		}

		public boolean isMultiValue() {
			return multiValue;
		}

		public static Operators bySymbol(String symbol) {
			return data.get(symbol);
		}
	}

	static public WSResult oldResponseToWSResult(String response) throws JSONException {
		WSResult wsResult = new WSResult();
		JSONObject jsonResult = new JSONObject(response);
		JSONObject jsonResponse = jsonResult.getJSONObject("response");
		if (jsonResponse.has("data")) {
			wsResult.setStatus(Status.OK);
			wsResult.setData(jsonResponse.getJSONArray("data"));
			wsResult.setResultType(ResultType.MULTIPLE);
			return wsResult;
		} else {
			wsResult.setStatus(Status.INTERNAL_SERVER_ERROR);
		}
		if (jsonResponse.has("error") && jsonResponse.getJSONObject("error").has("message")) {
			String messageKey = jsonResponse.getJSONObject("error").getString("message");
			String message = null;
			try{
				message = OBMessageUtils.getI18NMessage(messageKey);
			}catch (OBException e){
				message = messageKey;
			}
			wsResult.setMessage(message);
		} else if (jsonResponse.has("errors")) {
			wsResult.setMessage(jsonResponse.getJSONObject("errors").toString());
		} else {
			wsResult.setMessage("Unexpected error");
		}
		return wsResult;
	}

	static public void addEntitynameToParams(String path, Map<String, String> parameters) {
		final String[] pathParts = WebServiceUtil.getInstance().getSegments(path);
		if (pathParts.length == 0) {
			throw new OBException("Entity name is required");
		}
		final String entityName = pathParts[0];
		// check it the entity
		try {
			ModelProvider.getInstance().getEntity(entityName);
		} catch (CheckException e) {
			throw new OBException("Entity name is invalid");
		}
		parameters.put(JsonConstants.ENTITYNAME, entityName);
		if (pathParts.length > 1) {
			parameters.put(JsonConstants.ID, pathParts[1]);
		}
	}

	static public JSONObject getBodyData(HttpServletRequest request) throws Exception {
		final BufferedReader reader = request.getReader();
		if (reader == null) {
			return new JSONObject();
		}
		String line;
		final StringBuilder sb = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append(line);
		}
		return new JSONObject(sb.toString());
	}

	static public Map<String, String> requestParamsToMap(HttpServletRequest request) {
		Map<String, String> requestParams = new HashMap<String, String>();
		request.getParameterMap().keySet().forEach(key -> requestParams.put(key, request.getParameter(key)));
		return requestParams;
	}

	static public JSONObject criteriaFromRSQL(String rsql) throws OBException {
		if (rsql == null || rsql.isEmpty()) {
			// TODO: crear mensaje
			throw new OBException(OBMessageUtils.messageBD("SMFSWS_EmptyQuery"));
		}
		try {
			Node rootNode = new RSQLParser(defaultRestOperators()).parse(rsql);
			JSONObject criteria = rootNode.accept(new RSQLCriteriaVisitor());
			return criteria;
		} catch (Exception e) {
			// TODO: crear mensaje
			throw new OBException(OBMessageUtils.messageBD("SMFSWS_ParseErrorQuery"), e);
		}
	}

	static public void writeWSResponse(WSResult result, HttpServletResponse response) throws Exception {
		WSResult wsResult = new WSResult();
		try {
			if (result == null) {
				wsResult.setStatus(Status.METHOD_NOT_ALLOWED);
				wsResult.setMessage("Method not allowed");
			} else {
				wsResult = result;
			}
			wsResult.writeResponse(response);
		} catch (Exception e) {
			wsResult.setStatus(Status.INTERNAL_SERVER_ERROR);
			wsResult.setMessage(e.getMessage());
			wsResult.writeResponse(response);
		}
	}

	static public Set<ComparisonOperator> defaultRestOperators() {
		Set<ComparisonOperator> ops = new HashSet<ComparisonOperator>();
		for (Operators op : Operators.values()) {
			ops.add(new ComparisonOperator(op.getSymbol(), op.isMultiValue()));
		}
		return ops;
	}

	static public Map<String, String> mapRestParameters(Map<String, String> params) {
		final Map<String, String> parameters = new HashMap<String, String>();

		// entityName and Id
		parameters.put(JsonConstants.ENTITYNAME, params.get(JsonConstants.ENTITYNAME));
		if (params.containsKey(JsonConstants.ID)) {
			parameters.put(JsonConstants.ID, params.get(JsonConstants.ID));
		}

		// Sort
		parameters.put(JsonConstants.ORDERBY_PARAMETER, params.getOrDefault(OBRestConstants.SORTBY_PARAMETER,""));

		// Distinct
		if (params.containsKey(OBRestConstants.DISTINCT_PARAMETER)) {
			parameters.put(JsonConstants.DISTINCT_PARAMETER, params.get(OBRestConstants.DISTINCT_PARAMETER));
		}

		// Use alias
		parameters.put(JsonConstants.USE_ALIAS, params.getOrDefault(OBRestConstants.USEALIAS_PARAMETER,"true"));


		// Text match
		parameters.put(JsonConstants.TEXTMATCH_PARAMETER, JsonConstants.TEXTMATCH_EXACT);

		// criteria query
		String query = params.get(OBRestConstants.QUERY_PARAMETER);
		if (query != null && !query.isEmpty()) {
			String criteria = OBRestUtils.criteriaFromRSQL(query).toString();
			parameters.put(OBRestConstants.CRITERIA_PARAMETER, criteria);
		}

		// include children
		if (params.containsKey(OBRestConstants.CHILDREN_PARAMETER)) {
			parameters.put(OBRestConstants.CHILDREN_PARAMETER, params.get(OBRestConstants.CHILDREN_PARAMETER));
		}

		// include identifiers
		parameters.put(OBRestConstants.IDENTIFIERS_PARAMETER, Boolean.toString("true".equals(params.get(OBRestConstants.IDENTIFIERS_PARAMETER))));

		// No active filter
		if (params.containsKey(OBRestConstants.NO_ACTIVE_FILTER)) {
			parameters.put(JsonConstants.NO_ACTIVE_FILTER, params.get(OBRestConstants.NO_ACTIVE_FILTER));
		}

		// selected properties
		if (params.containsKey(OBRestConstants.FIELDS_PARAMETER)) {
			String fields = params.get(OBRestConstants.FIELDS_PARAMETER);
			List<String> originalFields = Arrays.asList(fields.split(","));
			Set<String> expandedProperties = new HashSet<String>();

			for (String originalProperty : originalFields) {
				// original field can be like "order.bPartner.name"
				String selectedProperty = "";
				List<String> propertyParts = Arrays.asList(originalProperty.trim().split("\\."));
				for (String part : propertyParts) {
					if (!selectedProperty.isEmpty()) {
						selectedProperty += ".";
					}
					selectedProperty += part.trim();
					expandedProperties.add(selectedProperty);
				}
			}
			String selectedProperties = String.join(",", expandedProperties);
			;
			parameters.put(JsonConstants.SELECTEDPROPERTIES_PARAMETER, selectedProperties);
		}

		// pagination parameters
		try {
			Integer startRow = 0;
			String strFirstResult = params.get(OBRestConstants.FIRSTRESULT_PARAMETER);
			if (strFirstResult != null && !strFirstResult.isEmpty()) {
				startRow = Integer.parseInt(strFirstResult);
				startRow = startRow > 0 ? startRow : 0;
			}

			// put startRow parameter
			parameters.put(JsonConstants.STARTROW_PARAMETER, startRow.toString());

			String strMaxResults = params.get(OBRestConstants.MAXRESULTS_PARAMETER);
			Integer maxResults = null;
			if (strMaxResults != null && !strMaxResults.isEmpty()) {
				maxResults = Integer.parseInt(strMaxResults);
				if (maxResults >= 0) {
					Integer endRow = startRow + maxResults - 1;
					parameters.put(JsonConstants.ENDROW_PARAMETER, endRow.toString());
				}
			}
		} catch (Exception e) {
			// TODO: crear mensaje
			throw new OBException(OBMessageUtils.messageBD("SMFSWS_PaginationError"));
		}

		// Tab where clause parameters
		if (!"true".equals(params.get(JsonConstants.WHERE_CLAUSE_HAS_BEEN_CHECKED))) {
			SWSDataSourceService dataSourceService = WeldUtils.getInstanceFromStaticBeanManager(SWSDataSourceService.class);
			String whereAndFilterClause = dataSourceService.getWhereAndFilterClause(params);
			if (StringUtils.isNotBlank(whereAndFilterClause)) {
				parameters.put(JsonConstants.WHERE_AND_FILTER_CLAUSE, whereAndFilterClause);
			}
		}

		//Context paramters
		params.keySet().stream()
				.filter(key -> key.startsWith("@") && key.endsWith("@"))
				.forEach(key -> parameters.put(key, params.get(key)));

		return parameters;
	}
}