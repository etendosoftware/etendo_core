package org.openbravo.dal.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.structure.BaseOBObject;

/**
 * Util class for DAL
 *
 * @author gorkaion
 */
public class OBDao {

  /**
   * Generic OBCriteria.
   *
   * @param clazz
   *          Class (entity).
   * @param restrictions
   *          List of Restriction instances which are used as filters
   * @return An OBCriteria object with the constraints.
   */
  public static <T extends BaseOBObject> OBCriteria<T> getFilteredCriteria(Class<T> clazz,
      Restriction... restrictions) {
    OBCriteria<T> obc = OBDal.getInstance().createCriteria(clazz);
    for (Restriction r : restrictions) {
      obc.add(r);
    }
    return obc;
  }

  /**
   * Returns a List of BaseOBOBjects of the Property identified by the property from the
   * BaseOBObject obj. This method enables the activeFilter so inactive BaseOBObjects are not
   * included on the returned List.
   */
  @SuppressWarnings("unchecked")
  public static <T extends BaseOBObject> List<T> getActiveOBObjectList(BaseOBObject obj,
      String property) {
    boolean isActiveFilterEnabled = OBDal.getInstance().isActiveFilterEnabled();
    if (!isActiveFilterEnabled) {
      OBDal.getInstance().enableActiveFilter();
    }
    try {
      return (List<T>) obj.get(property);
    } finally {
      if (!isActiveFilterEnabled) {
        OBDal.getInstance().disableActiveFilter();
      }
    }
  }

  /**
   * Parses the string of comma separated id's to return a List with the BaseOBObjects of the given
   * class. If there is an invalid id a null value is added to the List.
   */
  public static <T extends BaseOBObject> List<T> getOBObjectListFromString(Class<T> t,
      String _IDs) {
    String strBaseOBOBjectIDs = _IDs;
    final List<T> baseOBObjectList = new ArrayList<>();
    if (strBaseOBOBjectIDs.startsWith("(")) {
      strBaseOBOBjectIDs = strBaseOBOBjectIDs.substring(1, strBaseOBOBjectIDs.length() - 1);
    }
    if (!strBaseOBOBjectIDs.isEmpty()) {
      strBaseOBOBjectIDs = StringUtils.remove(strBaseOBOBjectIDs, "'");
      StringTokenizer st = new StringTokenizer(strBaseOBOBjectIDs, ",", false);
      while (st.hasMoreTokens()) {
        String strBaseOBObjectID = st.nextToken().trim();
        baseOBObjectList.add(OBDal.getInstance().get(t, strBaseOBObjectID));
      }
    }
    return baseOBObjectList;
  }

  public static <T extends BaseOBObject> List<String> getIDListFromOBObject(List<T> list) {
    List<String> idList = new ArrayList<>();
    for (BaseOBObject o : list) {
      idList.add(o.getId().toString());
    }
    return idList;
  }
}
