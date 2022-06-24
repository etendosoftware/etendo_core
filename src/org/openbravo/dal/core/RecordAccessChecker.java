package org.openbravo.dal.core;

import org.openbravo.base.provider.OBNotSingleton;

public interface RecordAccessChecker extends OBNotSingleton {
    Boolean canCreate(Object o);
    Boolean canRead(Object o);
    Boolean canUpdate(Object o);
    Boolean canDelete(Object o);
}
