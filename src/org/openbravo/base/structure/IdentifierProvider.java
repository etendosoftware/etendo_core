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
 * All portions are Copyright (C) 2008-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.structure;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Language;

/**
 * Provides the identifier/title of an object using the {@link Entity#getIdentifierProperties()
 * identifierProperties} of the {@link Entity Entity}.
 * 
 * Note: the getIdentifier can also be generated in the java entity but the current approach makes
 * it possible to change the identifier definition at runtime.
 * 
 * @author mtaal
 */

public class IdentifierProvider implements OBSingleton {

  public static final String SEPARATOR = " - ";
  private static final Logger log = LogManager.getLogger();
  // Oracle rejects an IN list longer than 1000 elements
  private static final int MAX_IN_SIZE = 1000;
  private static IdentifierProvider instance;

  public static synchronized IdentifierProvider getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(IdentifierProvider.class);
    }
    return instance;
  }

  public static synchronized void setInstance(IdentifierProvider instance) {
    IdentifierProvider.instance = instance;
  }

  private SimpleDateFormat dateFormat = null;
  private SimpleDateFormat dateTimeFormat = null;
  private SimpleDateFormat timeFormat = null;

  /**
   * Returns the identifier of the object. The identifier is computed using the identifier
   * properties of the Entity of the object. It is translated (if applicable) to the current
   * language
   * 
   * @param o
   *          the object for which the identifier is generated
   * @return the identifier
   */
  public String getIdentifier(Object o) {
    final Language lang = OBContext.getOBContext() != null ? OBContext.getOBContext().getLanguage()
        : null;
    return getIdentifier(o, true, lang);
  }

  /**
   * Looks up in bulk the translations that {@link #getIdentifier(Object)} would otherwise look up
   * one object at a time, for the given objects and for every object their identifiers reach
   * through foreign keys. Runs one query per translation entity (per 1000 objects) and stores the
   * result in each object's translation memo, so the later {@code getIdentifier()} calls do not
   * query. Labels do not change: an object this method misses is still looked up as before.
   *
   * Identifiers shown through a display column are not walked. Does nothing when no translation is
   * installed or there is no current language. Never throws: a failed lookup is logged and those
   * objects fall back to the per-object lookup.
   *
   * @param objects
   *          the objects whose identifiers are about to be computed, typically a page of rows
   */
  public void preloadTranslations(Collection<?> objects) {
    final OBContext context = OBContext.getOBContext();
    final Language language = context != null ? context.getLanguage() : null;
    if (objects == null || objects.isEmpty() || language == null
        || !OBContext.hasTranslationInstalled()) {
      return;
    }
    // translation parent property -> object id -> objects with that id
    final Map<Property, Map<Object, List<BaseOBObject>>> pending = new LinkedHashMap<>();
    final Set<BaseOBObject> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    final Deque<Object> queue = new ArrayDeque<>();
    for (Object o : objects) {
      if (o != null) {
        queue.add(o);
      }
    }
    while (!queue.isEmpty()) {
      try {
        final Object real = Hibernate.unproxy(queue.pop());
        if (real instanceof BaseOBObject && visited.add((BaseOBObject) real)) {
          collectTranslationLookups((BaseOBObject) real, pending, queue);
        }
      } catch (Exception e) {
        // e.g. a dangling foreign key: getIdentifier() deals with this object as it always did
        log.debug("Skipping an object while preloading translations", e);
      }
    }
    pending.forEach((trlParent, byId) -> preloadTranslations(trlParent, byId, language));
  }

  // Mirrors the branches of getIdentifier(Object, boolean, Language) that read a translation
  private void collectTranslationLookups(BaseOBObject bob,
      Map<Property, Map<Object, List<BaseOBObject>>> pending, Deque<Object> queue) {
    final Entity entity = bob.getEntity();
    boolean translationSeen = false;
    for (final Property identifier : entity.getIdentifierProperties()) {
      final Property property = entity.getProperty(identifier.getName());
      if (property.hasDisplayColumn()) {
        // read without id, so it shows the base value and never queries
        continue;
      }
      if (property.isTranslatable()) {
        // the object memoizes the first translation lookup, whatever its outcome
        final Object id = bob.getId();
        if (!translationSeen && property.getTrlParentProperty() != null && id instanceof String) {
          pending.computeIfAbsent(property.getTrlParentProperty(), k -> new HashMap<>())
              .computeIfAbsent(id, k -> new ArrayList<>())
              .add(bob);
        }
        translationSeen = true;
      } else if (!property.isPrimitive()) {
        final Object value = bob.getValue(property.getName());
        if (value != null) {
          queue.add(value);
        }
      }
    }
  }

  private void preloadTranslations(Property trlParent, Map<Object, List<BaseOBObject>> byId,
      Language language) {
    final String parentName = trlParent.getName();
    final List<Object> ids = new ArrayList<>(byId.keySet());
    for (int from = 0; from < ids.size(); from += MAX_IN_SIZE) {
      final List<Object> chunk = ids.subList(from, Math.min(from + MAX_IN_SIZE, ids.size()));
      final Map<Object, BaseOBObject> found = new HashMap<>();
      try {
        final OBCriteria<BaseOBObject> criteria = OBDal.getInstance()
            .createCriteria(trlParent.getEntity().getName())
            .add(Restrictions.in(parentName + ".id", chunk))
            .add(Restrictions.eq("language", language))
            .setFilterOnReadableClients(false)
            .setFilterOnReadableOrganization(false);
        for (BaseOBObject trl : criteria.list()) {
          // getValue, not get: no derived-readable check for non-admin callers
          found.putIfAbsent(DalUtil.getId(trl.getValue(parentName)), trl);
        }
      } catch (Exception e) {
        log.debug("Could not preload translations of {}. Looking them up one by one", trlParent, e);
        continue;
      }
      for (Object id : chunk) {
        for (BaseOBObject bob : byId.get(id)) {
          bob.setPreloadedTranslation(found.get(id));
        }
      }
    }
  }

  // identifyDeep determines if refered to objects are used
  // to identify the object
  private String getIdentifier(Object o, boolean identifyDeep, Language language) {
    // TODO: add support for null fields
    final StringBuilder sb = new StringBuilder();
    final DynamicEnabled dob = (DynamicEnabled) o;
    final String entityName = ((Identifiable) dob).getEntityName();
    final List<Property> identifiers = ModelProvider.getInstance()
        .getEntity(entityName)
        .getIdentifierProperties();

    for (final Property identifier : identifiers) {
      if (sb.length() > 0) {
        sb.append(SEPARATOR);
      }
      Property property = ((BaseOBObject) dob).getEntity().getProperty(identifier.getName());
      Object value;

      if (property.hasDisplayColumn()) {
        Property displayColumnProperty = DalUtil.getPropertyFromPath(
            property.getReferencedProperty().getEntity(), property.getDisplayPropertyName());
        BaseOBObject referencedObject = (BaseOBObject) dob.get(property.getName());
        if (referencedObject == null) {
          continue;
        }
        if (displayColumnProperty.hasDisplayColumn()) {
          // Allowing one level deep of displayed column pointing to references with display column
          value = ((BaseOBObject) dob.get(property.getDisplayPropertyName()))
              .get(displayColumnProperty.getDisplayPropertyName());
        } else if (!displayColumnProperty.isPrimitive()) {
          // Displaying identifier for non primitive properties

          value = ((BaseOBObject) referencedObject.get(property.getDisplayPropertyName()))
              .getIdentifier();
        } else {
          value = referencedObject.get(property.getDisplayPropertyName(), language);
        }

        // Assign displayColumnProperty to apply formatting if needed
        property = displayColumnProperty;
      } else if (property.isTranslatable()) {
        // Trying to get id of translatable object.
        Object id = dob.get("id");
        if (id instanceof BaseOBObject) {
          // When the object is created for a drop down list filter, it is incorrect: id is not a
          // String but a BaseOBject. This code deals with this exception.

          // TODO: once issue #23706 is fixed, this should not be needed anymore
          id = ((BaseOBObject) id).get("id");
        }

        if (id instanceof String) {
          value = ((BaseOBObject) dob).get(identifier.getName(), language, (String) id);
        } else {
          // give up, couldn't find the id
          value = ((BaseOBObject) dob).get(identifier.getName(), language);
        }

      } else if (!property.isPrimitive() && identifyDeep) {
        if (dob.get(property.getName()) != null) {
          value = ((BaseOBObject) dob.get(property.getName())).getIdentifier();
        } else {
          value = "";
        }
      } else {
        value = dob.get(identifier.getName());
      }

      if (value instanceof Identifiable && identifyDeep) {
        sb.append(getIdentifier(value, false, language));
      } else if (value != null) {

        // TODO: add number formatting...
        if (property.isDate() || property.isDatetime() || property.isAbsoluteDateTime()) {
          value = formatDate(property, (Date) value);
        } else if (property.isTime() || property.isAbsoluteTime()) {
          value = formatTime(property, (Date) value);
        }

        sb.append(value);
      }
    }
    if (identifiers.size() == 0) {
      return entityName + " (" + ((Identifiable) dob).getId() + ")";
    }
    return sb.toString();
  }

  protected String getSeparator() {
    return SEPARATOR;
  }

  private synchronized String formatDate(Property property, Date date) {
    if (date == null) {
      return "";
    }
    if (dateFormat == null) {
      final String dateFormatString = OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .getProperty("dateFormat.java");
      final String dateTimeFormatString = OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .getProperty("dateTimeFormat.java");
      dateFormat = new SimpleDateFormat(dateFormatString);
      dateTimeFormat = new SimpleDateFormat(dateTimeFormatString);
    }
    if (property.isDatetime() || property.isAbsoluteDateTime()) {
      return dateTimeFormat.format(date);
    } else {
      return dateFormat.format(date);
    }
  }

  private synchronized String formatTime(Property property, Date date) {
    if (date == null) {
      return "";
    }
    if (timeFormat == null) {
      final String dateTimeFormatString = OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .getProperty("dateTimeFormat.java");
      if (dateTimeFormatString.toUpperCase().endsWith("A")) {
        timeFormat = new SimpleDateFormat("hh:mm:ss a");
      } else {
        timeFormat = new SimpleDateFormat("HH:mm:ss");
      }
    }
    return timeFormat.format(date);
  }
}
