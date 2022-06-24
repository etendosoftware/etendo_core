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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.scheduling;

import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * A class that allows to serialize and deserialize the parameters of a {@link ProcessBundle}.
 */
class ParameterSerializer {
  private static ParameterSerializer instance = new ParameterSerializer();
  private static Logger log = LogManager.getLogger();

  static ParameterSerializer getInstance() {
    return instance;
  }

  String serialize(Map<String, Object> parameters) {
    if (parameters.isEmpty()) {
      return "";
    }

    checkParameters(parameters);

    return new JSONObject(parameters).toString();
  }

  private void checkParameters(Map<String, Object> parameters) {
    List<Class<?>> invalidTypes = parameters.values()
        .stream()
        .filter(((Predicate<Object>) this::isSupportedType).negate())
        .map(Object::getClass)
        .distinct()
        .collect(Collectors.toList());

    if (!invalidTypes.isEmpty()) {
      log.error("Could not serialize parameters because it has unsupported types {}. Map: {}",
          invalidTypes, parameters);
      throw new ParameterSerializationException(
          "Could not serialize paramters with types " + invalidTypes);
    }
  }

  private boolean isSupportedType(Object value) {
    return value instanceof String || value instanceof JSONObject;
  }

  Map<String, Object> deserialize(String parameters) {
    JSONObject json;
    try {
      json = new JSONObject(parameters);
    } catch (JSONException e) {
      throw new ParameterSerializationException(
          "Could not deserialize map of parameters: " + parameters);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> pbParams = (Map<String, Object>) StreamSupport
        .stream(Spliterators.spliteratorUnknownSize(json.keys(), 0), false)
        .collect(Collectors.toMap(Function.identity(), k -> deserialize(json, (String) k)));

    return pbParams;
  }

  Object deserialize(JSONObject json, String key) {
    Object value = null;
    try {
      value = json.get(key);
    } catch (JSONException ex) {
      // It should not happen as the key will always exists
    }

    if (!isSupportedType(value)) {
      Class<?> clazz = value != null ? value.getClass() : null;
      log.error(
          "Could not deserialize parameter because it has unsupported type {}. Parameter: name = {}, value = {}",
          clazz, key, value);
      throw new ParameterSerializationException(
          "Could not deserialize parameter with type " + clazz);
    }
    return value;
  }
}
