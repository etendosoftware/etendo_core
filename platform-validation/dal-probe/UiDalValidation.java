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
                var title = fields.stream().filter(field -> "TITLE".equalsIgnoreCase(
                        field.getColumn().getDBColumnName())).findFirst().orElseThrow();
                if (!"@IsActive@='Y'".equals(title.getDisplayLogic())
                        || !title.getDisplayLogic().equals(title.getDisplaylogicgrid())
                        || !Boolean.TRUE.equals(title.isStartnewline())
                        || !Boolean.TRUE.equals(title.isStartinoddcolumn())
                        || !Boolean.TRUE.equals(title.isShowInGridView())
                        || !Boolean.FALSE.equals(title.isShownInStatusBar())
                        || title.getClientclass() != null
                        || !"@IsActive@='N'".equals(title.getColumn().getReadOnlyLogic())
                        || !"Request details".equals(title.getFieldGroup().getName())
                        || !Boolean.FALSE.equals(title.getFieldGroup().isCollapsed())) {
                    throw new AssertionError("Original field layout/display metadata was lost");
                }
                var titleProperty = ModelProvider.getInstance().getEntityByTableId("PP_REQUEST")
                        .getPropertyByColumnName(title.getColumn().getDBColumnName());
                if (!"title".equals(titleProperty.getName())) throw new AssertionError("Field property resolution failed");
                if (!"10".equals(title.getColumn().getReference().getId())
                        || !"String".equals(title.getColumn().getReference().getName())) {
                    throw new AssertionError("Original column reference was not preserved");
                }
                var idField = fields.stream().filter(field -> "ID".equals(field.getColumn().getDBColumnName()))
                        .findFirst().orElseThrow();
                if (!"13".equals(idField.getColumn().getReference().getId())
                        || !"ID".equals(idField.getColumn().getReference().getName())
                        || !ModelProvider.getInstance().getEntityByTableId("PP_REQUEST").getIdProperties().get(0)
                                .getDomainType().getClass().getSimpleName().equals("UniqueIdDomainType")) {
                    throw new AssertionError("Original ID reference/domain was not preserved");
                }
                System.out.println("PASS: Generated column reference API preserves original String and ID metadata");
                var extensionColumn = OBDal.getInstance().get(org.openbravo.model.ad.datamodel.Column.class,
                        "CD3A95C8A05D45A0A2B6D250E9C83170");
                if (extensionColumn == null || !"org.openbravo.client.application"
                        .equals(extensionColumn.getModule().getJavaPackage())) {
                    throw new AssertionError("Original extension column module ownership was lost");
                }
                var process = OBProvider.getInstance().get(org.openbravo.client.application.Process.class);
                process.setName("Validation action");
                process.setSearchKey("PP_VALIDATION_ACTION");
                process.setClient(OBDal.getInstance().get(org.openbravo.model.ad.system.Client.class, "0"));
                process.setOrganization(OBDal.getInstance().get(org.openbravo.model.common.enterprise.Organization.class, "0"));
                process.setModule(OBDal.getInstance().get(org.openbravo.model.ad.module.Module.class,
                        "9BA0836A3CD74EE4AB48753A47211BCC"));
                OBDal.getInstance().save(process);
                title.getColumn().setOBUIAPPProcess(process);
                OBDal.getInstance().flush();
                String processId = process.getId();
                String titleColumnId = title.getColumn().getId();
                OBDal.getInstance().getSession().evict(title.getColumn());
                var reloadedColumn = OBDal.getInstance().get(org.openbravo.model.ad.datamodel.Column.class, titleColumnId);
                if (!processId.equals(reloadedColumn.getOBUIAPPProcess().getId())) {
                    throw new AssertionError("Original module extension accessor did not persist its process reference");
                }
                reloadedColumn.setOBUIAPPProcess(null);
                OBDal.getInstance().remove(process);
                OBDal.getInstance().flush();
                System.out.println("PASS: Original module extension API persists a process reference through generated OBDal");
                if (ModelProvider.getInstance().getEntityByTableId("PP_REQUEST")
                        .findPropertyByColumnId(title.getColumn().getId(), true) != titleProperty) {
                    throw new AssertionError("Shared column-ID resolution did not match the real dictionary property");
                }
                var fieldProperty = ModelProvider.getInstance().getEntity("ADField").getProperty("property");
                if (!fieldProperty.getDomainType().getClass().getName().equals(
                        "org.openbravo.userinterface.selector.model.domaintype.ModelElementDomainType")
                        || title.getProperty() != null) throw new AssertionError("Original selector property domain was replaced");
                var reference = fieldProperty.getDomainType().getReference();
                if (!"45B39681AFBC4808A64C9B776A290BA4".equals(reference.getId())
                        || !"95E2A8B50A254B2AAE6774B8C2F28120".equals(reference.getParentReference().getId())
                        || !fieldProperty.getDomainType().getClass().getProtectionDomain().getCodeSource()
                                .getLocation().toString().endsWith("/platform-ui-components.jar")) {
                    throw new AssertionError("Selector reference hierarchy or shared implementation was lost");
                }
                var selectorReference = OBDal.getInstance().get(org.openbravo.model.ad.domain.Reference.class,
                        "45B39681AFBC4808A64C9B776A290BA4");
                var selectorColumn = OBDal.getInstance().get(org.openbravo.model.ad.datamodel.Column.class,
                        fieldProperty.getColumnId());
                if (!"org.openbravo.userinterface.selector".equals(selectorReference.getModule().getJavaPackage())
                        || !"95E2A8B50A254B2AAE6774B8C2F28120".equals(selectorReference.getParentReference().getId())
                        || !selectorReference.getId().equals(selectorColumn.getReferenceSearchKey().getId())
                        || !selectorReference.getParentReference().getId().equals(selectorColumn.getReference().getId())) {
                    throw new AssertionError("Generated selector reference ownership or hierarchy is incomplete");
                }
                title.setProperty("title");
                OBDal.getInstance().flush();
                String fieldId = title.getId();
                OBDal.getInstance().getSession().clear();
                if (!"title".equals(OBDal.getInstance().get(Field.class, fieldId).getProperty())) {
                    throw new AssertionError("Original Field.Property API did not persist through its selector domain");
                }
                System.out.println("PASS: Original selector model-element domain preserves Field.Property API and persistence");
                System.out.println("PASS: Original field API resolves persisted display/layout rules and field group through OBDal");
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
                if (Boolean.getBoolean("validation.uiCache") || Boolean.getBoolean("validation.uiFields")) {
                    Class<?> cacheType = Class.forName(
                            "org.openbravo.client.application.window.ApplicationDictionaryCachedStructures");
                    cacheType.getDeclaredMethods();
                    try (var container = jakarta.enterprise.inject.se.SeContainerInitializer.newInstance()
                            .disableDiscovery().addBeanClasses(cacheType)
                            .initialize()) {
                        Object cache = Class.forName("org.openbravo.base.weld.WeldUtils")
                                .getMethod("getInstanceFromStaticBeanManager", Class.class).invoke(null, cacheType);
                        if (!Boolean.TRUE.equals(cacheType.getMethod("useCache").invoke(cache))) {
                            throw new AssertionError("Original production dictionary cache is disabled");
                        }
                        String tabId = OBDal.getInstance().get(Field.class, fieldId).getTab().getId();
                        Object cachedFields = cacheType.getMethod("getFieldsOfTab", String.class).invoke(cache, tabId);
                        if (!(cachedFields instanceof java.util.List<?> cachedList) || cachedList.size() != 6) {
                            throw new AssertionError("Original CDI cache did not resolve six request fields");
                        }
                        Object cachedTab = cacheType.getMethod("getTab", String.class).invoke(cache, tabId);
                        OBDal.getInstance().getSession().clear();
                        if (cacheType.getMethod("getTab", String.class).invoke(cache, tabId) != cachedTab) {
                            throw new AssertionError("Original CDI cache did not reuse the initialized tab after session clear");
                        }
                        for (Object cachedField : cachedList) {
                            if (((Field) cachedField).getColumn().getReference().getName() == null) {
                                throw new AssertionError("Cached field reference metadata was not initialized");
                            }
                        }
                        if (!java.util.List.of().equals(cacheType.getMethod("getAuxiliarInputList", String.class)
                                .invoke(cache, tabId))) throw new AssertionError("Unexpected auxiliary inputs");
                        System.out.println("PASS: Original CDI production cache initializes and reuses ERP-free window metadata");
                        if (Boolean.getBoolean("validation.uiFields")) {
                            Class<?> handlerClass = Class.forName("org.openbravo.client.application.window.OBViewFieldHandler");
                            Object originalHandler = handlerClass.getConstructor().newInstance();
                            handlerClass.getMethod("setTab", org.openbravo.model.ad.ui.Tab.class)
                                    .invoke(originalHandler, OBDal.getInstance().get(Field.class, fieldId).getTab());
                            Object originalFields = handlerClass.getMethod("getFields").invoke(originalHandler);
                            if (!(originalFields instanceof java.util.List<?> list) || list.isEmpty()) {
                                throw new AssertionError("Original field handler did not produce fields");
                            }
                            var fieldNames = new java.util.HashSet<String>();
                            Class<?> fieldDefinitionType = Class.forName(
                                    "org.openbravo.client.application.window.OBViewFieldHandler$OBViewFieldDefinition");
                            var fieldNameMethod = fieldDefinitionType.getMethod("getName");
                            // The original template-facing definition interface is package-private.
                            fieldNameMethod.setAccessible(true);
                            Object titleDefinition = null;
                            for (Object definition : list) {
                                String name = (String) fieldNameMethod.invoke(definition);
                                fieldNames.add(name);
                                if ("title".equals(name)) titleDefinition = definition;
                            }
                            if (!fieldNames.containsAll(java.util.Set.of("title", "category", "active"))) {
                                throw new AssertionError("Original handler omitted application fields: " + fieldNames);
                            }
                            String readOnlyRule = (String) titleDefinition.getClass().getMethod("getReadOnlyIf")
                                    .invoke(titleDefinition);
                            String displayRule = (String) titleDefinition.getClass().getMethod("getShowIf")
                                    .invoke(titleDefinition);
                            if (!readOnlyRule.contains("active") || !displayRule.contains("active")
                                    || readOnlyRule.equals(displayRule)) {
                                throw new AssertionError("Original handler lost the complementary active-state rules");
                            }
                            Class<?> definitionController = Class.forName("org.openbravo.client.kernel.reference.UIDefinitionController");
                            Object definitions = definitionController.getMethod("getInstance").invoke(null);
                            var expectedEditors = java.util.Map.of("TITLE", "StringUIDefinition",
                                    "ISACTIVE", "YesNoUIDefinition", "PP_CATEGORY_ID", "FKComboUIDefinition");
                            for (Field applicationField : OBDal.getInstance().get(org.openbravo.model.ad.ui.Tab.class, tabId).getADFieldList()) {
                                String editor = expectedEditors.get(applicationField.getColumn().getDBColumnName().toUpperCase(java.util.Locale.ROOT));
                                if (editor == null) continue;
                                Object definition = definitionController.getMethod("getUIDefinition", String.class)
                                        .invoke(definitions, applicationField.getColumn().getId());
                                if (definition == null || !definition.getClass().getSimpleName().equals(editor)) {
                                    throw new AssertionError("Original editor implementation missing: " + editor);
                                }
                            }
                            data.put("data", java.util.Map.of("fieldHandler", originalHandler));
                            String originalForm = processor.process(template, data);
                            if (!originalForm.contains("onFieldChanged") || !originalForm.contains("f.disableItem('title'")
                                    || !originalForm.contains("active")) {
                                throw new AssertionError("Original field handler/form rendering failed");
                            }
                            System.out.println("PASS: Original field handler builds fields and renders the original form template");
                        }
                    }
                }
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
