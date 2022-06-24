package com.etendoerp.utilities;

import org.hibernate.dialect.function.SQLFunction;
import org.openbravo.dal.core.DalLayerInitializer;
import org.openbravo.dal.core.OBContext;
import org.openbravo.test.base.TestConstants;

import java.util.Map;

public class DalUtils {

    private DalUtils() {}

    public static void initializeDalLayer(Map<String, SQLFunction> sqlFunctions) {
        DalLayerInitializer initializer = DalLayerInitializer.getInstance();
        if (!initializer.isInitialized()) {
            initializer.setSQLFunctions(sqlFunctions);
            initializer.initialize(true);
        }
    }

    public static void changeContextToAdmin() {
        OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP, TestConstants.Orgs.MAIN);
    }
}
