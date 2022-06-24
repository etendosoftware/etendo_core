package com.smf.securewebservices.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ObjectNotFoundException;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.ActiveEnabled;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.json.JsonConstants;

/**
 * TODO: This class can be refactored for performance improvements
 * 
 * @author androettop
 * @deprecated
 * 
 */
@Deprecated
public class SecureDataToJson extends DataToJsonConverter {

	private Set<String> selectedProperties = new HashSet<>();
	private String displayProperty = null;
	private Boolean includeChildren = false;
	private Boolean includeIdentifier = true;
	private List<String> additionalProperties = new ArrayList<String>();

	public List<JSONObject> toJsonObjects(List<? extends BaseOBObject> bobs, DataResolvingMode dataResolvingMode,
			String parentProperty) {
		final List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
		for (BaseOBObject bob : bobs) {
			jsonObjects.add(toJsonObject(bob, dataResolvingMode, parentProperty));
		}
		return jsonObjects;
	}

	@Override
	public JSONObject toJsonObject(BaseOBObject bob, DataResolvingMode dataResolvingMode) {
		return toJsonObject(bob, dataResolvingMode, null);
	}

	public JSONObject toJsonObject(BaseOBObject bob, DataResolvingMode dataResolvingMode, String parentProperty) {
		try {
			final JSONObject jsonObject = new JSONObject();
			if (includeIdentifier
					|| !selectedProperties.isEmpty() && selectedProperties.contains(JsonConstants.IDENTIFIER)) {
				jsonObject.put(JsonConstants.IDENTIFIER, bob.getIdentifier());
			}
			jsonObject.put(JsonConstants.ENTITYNAME, bob.getEntityName());
			if (dataResolvingMode == DataResolvingMode.SHORT) {
				jsonObject.put(JsonConstants.ID, bob.getId());
				if (bob instanceof ActiveEnabled) {
					jsonObject.put(JsonConstants.ACTIVE, ((ActiveEnabled) bob).isActive());
				}
				return jsonObject;
			}

			final boolean isDerivedReadable = OBContext.getOBContext().getEntityAccessChecker()
					.isDerivedReadable(bob.getEntity());

			for (Property property : bob.getEntity().getProperties()) {
				String selectedPropertyPrefix = (parentProperty != null ? parentProperty + "." : "");
				String propertyWithParent = selectedPropertyPrefix + property.getName();
				// do not convert if the object is derived readable and the property is not
				if (isDerivedReadable && !property.allowDerivedRead()) {
					continue;
				}

				// check if the property is part of the selection
				if (selectedProperties.size() > 0 && !selectedProperties.contains(propertyWithParent)) {
					continue;
				}

				if (property.isOneToMany()) {
					if (includeChildren && selectedProperties.size() > 0) {
						try {
							// en este caso (isOneToMany = true) el get siempre devuelve una lista.
							@SuppressWarnings("unchecked")
							List<BaseOBObject> listResult = (List<BaseOBObject>) bob.get(property.getName());
							List<JSONObject> jsonList = toJsonObjects(listResult, dataResolvingMode,
									propertyWithParent);
							JSONArray jsonArray = new JSONArray();
							for (JSONObject jsonObj : jsonList) {
								jsonArray.put(jsonObj);
							}
							jsonObject.put(property.getName(), jsonArray);
						} catch (Exception e) {
							jsonObject.put(property.getName(), JSONObject.NULL);
						}
					}
					continue;
				}

				Object value;
				if (dataResolvingMode == DataResolvingMode.FULL_TRANSLATABLE) {
					value = bob.get(property.getName(), OBContext.getOBContext().getLanguage());
				} else {
					value = bob.get(property.getName());
				}
				if (value != null) {
					if (property.isPrimitive()) {
						jsonObject.put(property.getName(), convertPrimitiveValue(property, value));
					} else {
						addBaseOBObject(jsonObject, property, property.getName(), property.getReferencedProperty(),
								(BaseOBObject) value);
					}
				} else {
					jsonObject.put(property.getName(), JSONObject.NULL);
				}
			}
			for (String additionalProperty : additionalProperties) {
				// sometimes empty strings are passed in
				if (additionalProperty.length() == 0) {
					continue;
				}
				final Object value = DalUtil.getValueFromPath(bob, additionalProperty);
				if (value == null) {
					jsonObject.put(replaceDots(additionalProperty), (Object) null);
				} else if (value instanceof BaseOBObject) {
					final Property additonalPropertyObject = DalUtil.getPropertyFromPath(bob.getEntity(),
							additionalProperty);
					addBaseOBObject(jsonObject, additonalPropertyObject, additionalProperty,
							additonalPropertyObject.getReferencedProperty(), (BaseOBObject) value);
				} else {
					final Property property = DalUtil.getPropertyFromPath(bob.getEntity(), additionalProperty);
					// identifier
					if (additionalProperty.endsWith(JsonConstants.IDENTIFIER)) {
						jsonObject.put(replaceDots(additionalProperty), value);
					} else {
						jsonObject.put(replaceDots(additionalProperty), convertPrimitiveValue(property, value));
					}
				}
			}
			// When table references are set, the identifier should contain the display
			// property for as it
			// is done in the grid data. Refer
			// https://issues.openbravo.com/view.php?id=26696
			if (StringUtils.isNotEmpty(displayProperty)) {
				if (jsonObject.has(displayProperty + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER)
						&& !jsonObject.get(displayProperty + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER)
								.equals(JSONObject.NULL)) {
					jsonObject.put(JsonConstants.IDENTIFIER,
							jsonObject.get(displayProperty + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER));
				} else if (jsonObject.has(displayProperty)
						&& !jsonObject.get(displayProperty).equals(JSONObject.NULL)) {
					jsonObject.put(JsonConstants.IDENTIFIER, jsonObject.get(displayProperty));
				}
			}
			return jsonObject;
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}

	private void addBaseOBObject(JSONObject jsonObject, Property referencingProperty, String propertyName,
			Property referencedProperty, BaseOBObject obObject) throws JSONException {
		String identifier = null;
		// jsonObject.put(propertyName, toJsonObject(obObject,
		// DataResolvingMode.SHORT));
		if (referencedProperty != null) {
			try {
				jsonObject.put(propertyName.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR),
						obObject.get(referencedProperty.getName()));
			} catch (ObjectNotFoundException e) {
				// Referenced object does not exist, set UUID
				jsonObject.put(propertyName, e.getIdentifier());
				String identifierStr = propertyName + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER;
				if ((includeIdentifier && selectedProperties.isEmpty())
						|| (!selectedProperties.isEmpty() && selectedProperties.contains(identifierStr))) {
					jsonObject.put(identifierStr, e.getIdentifier());
				}
				return;
			}
		} else {
			jsonObject.put(propertyName, obObject.getId());
		}
		// jsonObject.put(propertyName + DalUtil.DOT + JsonConstants.ID,
		// obObject.getId());

		if (referencingProperty != null && referencingProperty.hasDisplayColumn()) {

			Property displayColumnProperty = DalUtil.getPropertyFromPath(referencedProperty.getEntity(),
					referencingProperty.getDisplayPropertyName());
			// translating the displayPropertyName before retrieving inside the condition
			// statements. The values will be automatically translated if
			// getIdentifier() is called.
			if (referencingProperty.hasDisplayColumn()) {
				Object referenceObject = obObject.get(referencingProperty.getDisplayPropertyName(),
						OBContext.getOBContext().getLanguage(), (String) obObject.getId());
				if (referenceObject instanceof BaseOBObject) {
					identifier = referenceObject != null ? ((BaseOBObject) referenceObject).getIdentifier() : "";
				} else {
					identifier = referenceObject != null ? referenceObject.toString() : "";
				}
				if (referencingProperty.isDisplayValue()) {
					if (obObject.getEntity().hasProperty("searchKey") && obObject.getEntity().getProperty("searchKey").allowDerivedRead()) {
						Object valueObject = obObject.get("searchKey", OBContext.getOBContext().getLanguage(),
								(String) obObject.getId());
						if (valueObject != null) {
							identifier = valueObject.toString() + " - " + identifier;
						} else {
							identifier = " - " + identifier;
						}
					}
				}
				// Allowing one level deep of displayed column pointing to references with
				// display column
				String identifierStr = propertyName.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR)
						+ DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER;
				if ((includeIdentifier && selectedProperties.isEmpty())
						|| (!selectedProperties.isEmpty() && selectedProperties.contains(identifierStr))) {
					jsonObject.put(identifierStr, identifier);

				}
			} else if (!displayColumnProperty.isPrimitive()) {
				// Displaying identifier for non primitive properties
				String identifierStr = propertyName.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR)
						+ DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER;
				if ((includeIdentifier && selectedProperties.isEmpty())
						|| (!selectedProperties.isEmpty() && selectedProperties.contains(identifierStr))) {
					jsonObject.put(identifierStr,
							((BaseOBObject) obObject.get(referencingProperty.getDisplayPropertyName()))
									.getIdentifier());
				}
			} else {
				String identifierStr = propertyName.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR)
						+ DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER;

				if ((includeIdentifier && selectedProperties.isEmpty())
						|| (!selectedProperties.isEmpty() && selectedProperties.contains(identifierStr))) {
					Object referenceObject = obObject.get(referencingProperty.getDisplayPropertyName(),
							OBContext.getOBContext().getLanguage(), (String) obObject.getId());
					identifier = referenceObject != null ? referenceObject.toString() : "";
					jsonObject.put(identifierStr, identifier);
				}
			}
		} else {
			String identifierStr = propertyName.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR) + DalUtil.FIELDSEPARATOR
					+ JsonConstants.IDENTIFIER;
			if ((includeIdentifier && selectedProperties.isEmpty())
					|| (!selectedProperties.isEmpty() && selectedProperties.contains(identifierStr))) {
				jsonObject.put(identifierStr, obObject.getIdentifier());
			}
		}
	}

	@Override
	public void setSelectedProperties(String selectedPropertiesStr) {
		if (selectedPropertiesStr == null || selectedPropertiesStr.trim().isEmpty()) {
			return;
		}
		for (String selectedProp : selectedPropertiesStr.split(",")) {
			if (!selectedProp.isEmpty()) {
				selectedProperties.add(selectedProp);
			}
		}
	}

	public void setIncludeChildren(Boolean includeChildrenValue) {
		includeChildren = includeChildrenValue;
	}

	public void setIncludeIdentifier(Boolean value) {
		includeIdentifier = value;
	}

	private String replaceDots(String value) {
		return value.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR);
	}
}