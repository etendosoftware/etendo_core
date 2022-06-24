package com.smf.securewebservices.utils;

import org.openbravo.erpCommon.utility.OBError;
/**
 * @author androettop
 */
public class Result {
	public enum Type {
		SUCCESS, ERROR, WARNING, INFO
	}

	private Type type;
	private String message;

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public OBError toOBError() {
		OBError error = new OBError();
		String type = this.type.name();
		error.setType(type);
		// capitalize title error
		error.setTitle(type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase());
		error.setMessage(this.message);
		return error;
	}

	public static Result fromOBError(OBError error) {
		Result result = new Result();
		result.setMessage(error.getMessage());
		try {
			result.setType(Type.valueOf(error.getType().toUpperCase()));
		} catch (IllegalArgumentException e) {
			result.setType(Type.INFO);
		}
		return result;
	}
}
