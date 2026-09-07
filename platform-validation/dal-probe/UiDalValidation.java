package com.etendoerp.platform.validation;

import java.util.Set;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.dal.core.DalSessionFactoryController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Window;

/** Executes generated visual metadata with the canonical ERP-free DAL. */
public final class UiDalValidation {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected disposable properties path");
        OBPropertiesProvider.getInstance().setProperties(args[0]);
        for (String type : new String[] {"org.openbravo.model.common.plm.Product",
                "org.openbravo.model.common.enterprise.Warehouse",
                "org.openbravo.model.common.businesspartner.BusinessPartner"}) {
            try {
                Class.forName(type, false, UiDalValidation.class.getClassLoader());
                throw new AssertionError("ERP runtime class present: " + type);
            } catch (ClassNotFoundException expected) { }
        }
        for (var entity : ModelProvider.getInstance().getModel()) {
            if (Set.of("Product", "Warehouse", "BusinessPartner").contains(entity.getName())) {
                throw new AssertionError("ERP model present");
            }
            Class<?> type = entity.getMappingClass();
            if (type == null || !type.getProtectionDomain().getCodeSource().getLocation().toString().contains("/ui-dal-classes-")) {
                throw new AssertionError("Entity did not load from current UI generation: " + entity.getName());
            }
            OBProvider.getInstance().register(type, type, false);
            OBProvider.getInstance().register(entity.getName(), type, false);
        }
        DalSessionFactoryController controller = new DalSessionFactoryController();
        SessionFactoryController.setInstance(controller);
        org.hibernate.SessionFactory factory = null;
        try {
            factory = controller.getSessionFactory();
            OBContext.setOBContext("U1", "R1", "C1", "O1", "en_US");
            OBContext.setAdminMode();
            try {
                var windows = OBDal.getInstance().createQuery(Window.class, "order by name").list();
                if (windows.size() != 2 || !windows.get(0).getName().equals("Categories")
                        || !windows.get(1).getName().equals("Requests")) throw new AssertionError("Window DAL query failed");
                var fields = OBDal.getInstance().createQuery(Field.class, "tab.window.name = :name order by sequenceNumber")
                        .setNamedParameter("name", "Requests").list();
                if (fields.size() != 6) throw new AssertionError("Request field DAL query failed");
                for (Field field : fields) {
                    if (!field.getColumn().getTable().getId().equals("PP_REQUEST")
                            || !field.getTab().getTable().getId().equals("PP_REQUEST")) {
                        throw new AssertionError("Generated UI metadata relationship failed");
                    }
                }
                System.out.println("PASS: OBDal reads two windows and six request fields through generated UI relationships");
                var template = OBDal.getInstance().get(org.openbravo.client.kernel.Template.class,
                        "C1D176407A354A40815DC46D24D70EB8");
                if (template == null) throw new AssertionError("Original form template missing");
                var processor = new org.openbravo.client.kernel.freemarker.FreemarkerTemplateProcessor();
                if (!processor.getClass().getProtectionDomain().getCodeSource().getLocation().toString()
                        .endsWith("/platform-ui-components.jar")) throw new AssertionError("Template processor is not shared");
                processor.validate(template);
                var handler = java.util.Map.of("hasStatusBarFields", false, "hasFieldsWithReadOnlyIf", true,
                        "hasFieldsWithShowIf", false, "fields", java.util.List.of(java.util.Map.of(
                                "name", "title", "readOnly", false, "readOnlyIf", "true", "showIf", "")));
                var data = new java.util.HashMap<String, Object>();
                data.put("data", java.util.Map.of("fieldHandler", handler));
                String rendered = processor.process(template, data);
                if (!rendered.contains("onFieldChanged") || !rendered.contains("f.disableItem('title', true)")) {
                    throw new AssertionError("Original form template failed to render its read-only rule");
                }
                System.out.println("PASS: Shared original FreeMarker processor resolves and renders database form template");
            } finally { OBContext.restorePreviousMode(); }
        } finally {
            try {
                if (SessionHandler.existsOpenedSessions()) OBDal.getInstance().rollbackAndClose();
            } finally {
                OBContext.setOBContext((OBContext) null);
                try { if (factory != null) factory.close(); }
                finally { SessionFactoryController.setInstance(null); }
            }
        }
    }
}
