/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2009-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.json;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ObjectNotFoundException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.AbsoluteDateTimeDomainType;
import org.openbravo.base.model.domaintype.AbsoluteTimeDomainType;
import org.openbravo.base.model.domaintype.BinaryDomainType;
import org.openbravo.base.model.domaintype.EncryptedStringDomainType;
import org.openbravo.base.model.domaintype.HashedStringDomainType;
import org.openbravo.base.model.domaintype.TimestampDomainType;
import org.openbravo.base.structure.ActiveEnabled;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;

/**
 * Is responsible for converting Openbravo business objects ({@link BaseOBObject} to a json
 * representation. This converter supports both converting single BaseOBObject instances and a
 * collection of business objects.
 * 
 * Values are converted as follows:
 * <ul>
 * <li>Reference values are converted as a JSONObject with only the id and identifier set.</li>
 * <li>Primitive date values are converted to a representation following the xml formatting.</li>
 * <li>Other primitive values are converted by the JSONObject itself.</li>
 * </ul>
 * 
 * @author mtaal
 */
public class DataToJsonConverter {

  public static final String REF_SEPARATOR = "/";

  // TODO: need to be revisited when client side data formatting is solved
  private final SimpleDateFormat xmlDateFormat = JsonUtils.createDateFormat();
  private final SimpleDateFormat xmlDateTimeFormat = JsonUtils.createDateTimeFormat();
  private final SimpleDateFormat jsTimeFormat = JsonUtils.createJSTimeFormat();
  // private final static SimpleDateFormat xmlTimeFormat = JsonUtils.createTimeFormat();
  private final static SimpleDateFormat xmlTimeFormatWithoutMTOffset = JsonUtils
      .createTimeFormatWithoutGMTOffset();

  // additional properties to return as a flat list
  private List<String> additionalProperties = new ArrayList<String>();

  // limit the json serialization to these properties
  private Set<String> selectedProperties = new HashSet<>();

  // display property used for table reference fields
  private String displayProperty = null;

  // entity of the data being converted. will be used in the convertToJsonObjects, as there are no
  // BaseOBObjects from which to infer the entity
  private Entity entity;

  private static final Logger log = LogManager.getLogger();

  /**
   * Convert a list of Maps with key value pairs to a list of {@link JSONObject}.
   * 
   * @param data
   *          the list of Maps
   * @return the corresponding list of JSONObjects
   */
  public List<JSONObject> convertToJsonObjects(List<Map<String, Object>> data) {
    try {
      final List<JSONObject> jsonObjects = new ArrayList<>();
      for (Map<String, Object> dataInstance : data) {
        final JSONObject jsonObject = new JSONObject();
        for (Entry<String, Object> entry : dataInstance.entrySet()) {
          String key = entry.getKey();
          Property property = null;
          if (this.entity != null) {
            property = entity.getProperty(key, false);
          }
          final Object value = entry.getValue();
          if (value instanceof BaseOBObject) {
            Property referencedProperty = property != null ? property.getReferencedProperty()
                : null;
            addBaseOBObject(jsonObject, property, key, referencedProperty, (BaseOBObject) value);
          } else {
            Object convertedValue = null;
            if (value != null && property != null && property.isPrimitive()) {
              // if the property is known and the value is not null, use the
              // convertPrimitiveValue(property, value) method to convert the value. It is more
              // complete than convertPrimitiveValue(value) as the former converts dates from the
              // server timezone offset to UTC, among other things
              convertedValue = convertPrimitiveValue(property, value);
            } else {
              convertedValue = convertPrimitiveValue(value);
            }
            jsonObject.put(key, convertedValue);
          }
        }
        jsonObjects.add(jsonObject);
      }
      return jsonObjects;
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Convert a list of {@link BaseOBObject} to a list of {@link JSONObject}.
   * 
   * @param bobs
   *          the list of BaseOBObjects to convert
   * @return the corresponding list of JSONObjects
   */
  public List<JSONObject> toJsonObjects(List<? extends BaseOBObject> bobs) {
    final List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
    for (BaseOBObject bob : bobs) {
      jsonObjects.add(toJsonObject(bob, DataResolvingMode.FULL));
    }
    return jsonObjects;
  }

  /**
   * Convert a single {@link BaseOBObject} into a {@link JSONObject}.
   * 
   * @param bob
   *          the BaseOBObject to convert
   * @param dataResolvingMode
   *          the data resolving mode determines how much information is converted (only the
   *          identifying info or everything).
   * @return the converted object
   */
  public JSONObject toJsonObject(BaseOBObject bob, DataResolvingMode dataResolvingMode) {
    try {
      final JSONObject jsonObject = new JSONObject();
      jsonObject.put(JsonConstants.IDENTIFIER, bob.getIdentifier());
      jsonObject.put(JsonConstants.ENTITYNAME, bob.getEntityName());
      jsonObject.put(JsonConstants.REF, encodeReference(bob));
      if (dataResolvingMode == DataResolvingMode.SHORT) {
        jsonObject.put(JsonConstants.ID, bob.getId());
        if (bob instanceof ActiveEnabled) {
          jsonObject.put(JsonConstants.ACTIVE, ((ActiveEnabled) bob).isActive());
        }
        return jsonObject;
      }
      final boolean isDerivedReadable = OBContext.getOBContext()
          .getEntityAccessChecker()
          .isDerivedReadable(bob.getEntity());

      for (Property property : bob.getEntity().getProperties()) {
        if (property.isOneToMany()) {
          // ignore these for now....
          continue;
        }
        // do not convert if the object is derived readable and the property is not
        if (isDerivedReadable && !property.allowDerivedRead()) {
          continue;
        }

        // check if the property is part of the selection
        if (selectedProperties.size() > 0 && !selectedProperties.contains(property.getName())) {
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
            // TODO: format!
            jsonObject.put(property.getName(), convertPrimitiveValue(property, value));
          } else {
            addBaseOBObject(jsonObject, property, property.getName(),
                property.getReferencedProperty(), (BaseOBObject) value);
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
          final Property property = DalUtil.getPropertyFromPath(bob.getEntity(),
              additionalProperty);
          // identifier
          if (additionalProperty.endsWith(JsonConstants.IDENTIFIER)) {
            jsonObject.put(replaceDots(additionalProperty), value);
          } else {
            jsonObject.put(replaceDots(additionalProperty), convertPrimitiveValue(property, value));
          }
        }
      }
      // When table references are set, the identifier should contain the display property for as it
      // is done in the grid data. Refer https://issues.openbravo.com/view.php?id=26696
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

      // The recordTime is also added. This is the time (in milliseconds) at which the record was
      // generated. This time can be used in the client side to compute the record "age", for
      // example, or how much time has passed since the record was updated
      jsonObject.put("recordTime", new Date().getTime());
      return jsonObject;
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
  }

  private String replaceDots(String value) {
    return value.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR);
  }

  private void addBaseOBObject(JSONObject jsonObject, Property referencingProperty,
      String propertyName, Property referencedProperty, BaseOBObject obObject)
      throws JSONException {
    String identifier = null;
    // jsonObject.put(propertyName, toJsonObject(obObject, DataResolvingMode.SHORT));
    if (referencedProperty != null) {
      try {
        jsonObject.put(propertyName.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR),
            obObject.get(referencedProperty.getName()));
      } catch (ObjectNotFoundException e) {
        // Referenced object does not exist, set UUID
        jsonObject.put(propertyName, e.getIdentifier());
        jsonObject.put(propertyName + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER,
            e.getIdentifier());
        return;
      }
    } else {
      jsonObject.put(propertyName, obObject.getId());
    }
    // jsonObject.put(propertyName + DalUtil.DOT + JsonConstants.ID, obObject.getId());

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
          identifier = referenceObject != null ? ((BaseOBObject) referenceObject).getIdentifier()
              : "";
        } else {
          identifier = referenceObject != null ? referenceObject.toString() : "";
        }
        if (referencingProperty.isDisplayValue()) {
          if (obObject.getEntity().hasProperty("searchKey")) {
            Object valueObject = obObject.get("searchKey", OBContext.getOBContext().getLanguage(),
                (String) obObject.getId());
            if (valueObject != null) {
              identifier = valueObject.toString() + " - " + identifier;
            } else {
              identifier = " - " + identifier;
            }
          } else {
            log.warn("Entity " + obObject.getEntity().getName()
                + " does not have a searchKey property, the flag Displayed Value should not be used");
          }
        }
        // Allowing one level deep of displayed column pointing to references with display column
        jsonObject.put(propertyName.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR)
            + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER, identifier);
      } else if (!displayColumnProperty.isPrimitive()) {
        // Displaying identifier for non primitive properties
        jsonObject.put(
            propertyName.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR) + DalUtil.FIELDSEPARATOR
                + JsonConstants.IDENTIFIER,
            ((BaseOBObject) obObject.get(referencingProperty.getDisplayPropertyName()))
                .getIdentifier());
      } else {
        Object referenceObject = obObject.get(referencingProperty.getDisplayPropertyName(),
            OBContext.getOBContext().getLanguage(), (String) obObject.getId());
        identifier = referenceObject != null ? referenceObject.toString() : "";
        jsonObject.put(propertyName.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR)
            + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER, identifier);
      }
    } else {
      jsonObject.put(propertyName.replace(DalUtil.DOT, DalUtil.FIELDSEPARATOR)
          + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER, obObject.getIdentifier());
    }
  }

  // TODO: do some form of formatting here?
  protected Object convertPrimitiveValue(Property property, Object value) {
    final Class<?> clz = property.getPrimitiveObjectType();
    if (Date.class.isAssignableFrom(clz)) {
      if (property.getDomainType() instanceof TimestampDomainType) {

        Timestamp localTime = (Timestamp) value;
        Date UTCTime = convertToUTC(localTime);

        return xmlTimeFormatWithoutMTOffset.format(UTCTime);
      } else if (property.getDomainType() instanceof AbsoluteTimeDomainType) {
        final String formattedValue = jsTimeFormat.format(value);
        return JsonUtils.convertToCorrectXSDFormat(formattedValue);
      } else if (property.getDomainType() instanceof AbsoluteDateTimeDomainType) {
        final String formattedValue = jsTimeFormat.format(value);
        return JsonUtils.convertToCorrectXSDFormat(formattedValue);
      } else if (property.isDatetime() || Timestamp.class.isAssignableFrom(clz)) {
        final String formattedValue = xmlDateTimeFormat.format(value);
        return JsonUtils.convertToCorrectXSDFormat(formattedValue);
      } else {
        return xmlDateFormat.format(value);
      }
      // for the properties of type password -> do not return raw-value at all
    } else if (property.getDomainType() instanceof HashedStringDomainType
        || property.getDomainType() instanceof EncryptedStringDomainType) {
      return "***";
    } else if (property.getDomainType() instanceof BinaryDomainType && value instanceof byte[]) {
      return Base64.encodeBase64String((byte[]) value);
    }
    return value;
  }

  private static Date convertToUTC(Date localTime) {
    Calendar now = Calendar.getInstance();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(localTime);
    calendar.set(Calendar.DATE, now.get(Calendar.DATE));
    calendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
    calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));

    int gmtMillisecondOffset = (now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET));
    calendar.add(Calendar.MILLISECOND, -gmtMillisecondOffset);

    return calendar.getTime();
  }

  protected Object convertPrimitiveValue(Object value) {
    if (value == null) {
      // Do not return null, or they key and value of this particular column will
      // not be added to the JSON object
      // See issue https://issues.openbravo.com/view.php?id=22971
      return "";
    }
    if (value instanceof Timestamp) {
      return xmlDateTimeFormat.format(value);
    }
    if (value instanceof Date) {
      return xmlDateFormat.format(value);
    }
    return value;
  }

  protected String encodeReference(BaseOBObject bob) {
    return bob.getEntityName() + REF_SEPARATOR + bob.getId();
  }

  public List<String> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(List<String> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

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

  public void setDisplayProperty(String displayPropertyValue) {
    displayProperty = displayPropertyValue;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }
}
