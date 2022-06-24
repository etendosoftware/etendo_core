package com.smf.securewebservices.rsql;

import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

/**
 * @author androettop
 */
public class RSQLCriteriaVisitor implements RSQLVisitor<JSONObject, String> {

	@Override
	public JSONObject visit(AndNode andNode, String param) {
		JSONObject node = new JSONObject();
		try {
			node.put("_constructor", "AdvancedCriteria");
			node.put("operator", "and");
			node.put("parameter", param);
			JSONArray criteria = new JSONArray();

			for (Node child : andNode.getChildren()) {
				criteria.put(child.accept(this));
			}
			node.put("criteria", criteria);
		} catch (JSONException e) {
			// TODO: crear mensaje
			throw new OBException(OBMessageUtils.messageBD("SMFSWS_JsonError"));
		}
		return node;
	}

	@Override
	public JSONObject visit(OrNode orNode, String param) {
		JSONObject node = new JSONObject();
		try {
			node.put("_constructor", "AdvancedCriteria");
			node.put("operator", "or");
			node.put("parameter", param);
			JSONArray criteria = new JSONArray();
			for (Node child : orNode.getChildren()) {
				criteria.put(child.accept(this));
			}
			node.put("criteria", criteria);
		} catch (JSONException e) {
			throw new OBException(OBMessageUtils.messageBD("SMFSWS_JsonError"));
		}
		return node;
	}

	@Override
	public JSONObject visit(ComparisonNode compNode, String param) {
		JSONObject node = new JSONObject();
		try {
			String operatorSymbol = compNode.getOperator().getSymbol();
			String operatorName = OBRestUtils.Operators.bySymbol(operatorSymbol).getName();
			if (operatorName == null) {
				// TODO: crear mensaje
				throw new OBException(OBMessageUtils.messageBD("SMFSWS_OperatorNotExists"));
			}
			if("isNull".equals(operatorName) || "notNull".equals(operatorName)){
				node.put("operator", operatorName);
				node.put("fieldName", compNode.getSelector());
			}else{
				node.put("operator", operatorName);
				node.put("parameter", param);
				node.put("fieldName", compNode.getSelector());
				List<String> arguments = compNode.getArguments();
				if (arguments.size() == 1) {
					node.put("value", arguments.get(0));
				} else if (arguments.size() > 1) {
					JSONArray values = new JSONArray();
					arguments.forEach(values::put);
					node.put("value", values);
				}
			}
		} catch (JSONException e) {
			throw new OBException(OBMessageUtils.messageBD("SMFSWS_JsonError"));
		}
		return node;
	}
}
