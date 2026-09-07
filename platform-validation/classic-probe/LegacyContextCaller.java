package com.etendoerp.platform.validation;

import org.openbravo.dal.core.OBContext;
import org.openbravo.model.common.enterprise.Warehouse;

/** Compiled against the supplied Classic binary API, executed against refactored classes. */
public final class LegacyContextCaller {
    public static void main(String[] args) {
        String origin = OBContext.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (!origin.endsWith("/build/classes/java/classicEntities/")) {
            throw new AssertionError("Expected refactored ERP context, not baseline: " + origin);
        }
        OBContext context = new OBContext();
        try {
            OBContext.setOBContext(context);
            if (OBContext.getOBContext() != context) throw new AssertionError("Legacy thread context changed");
            context.setWarehouse((Warehouse) null);
            Warehouse warehouse = context.getWarehouse();
            if (warehouse != null) throw new AssertionError("Legacy Warehouse descriptor changed");
            context.setNewUI(true);
            if (!context.isNewUI()) throw new AssertionError("Legacy UI flag contract changed");
        } finally {
            OBContext.setOBContext((OBContext) null);
        }
        System.out.println("PASS: Baseline-compiled caller linked against refactored ERP context and Warehouse APIs");
    }
}
