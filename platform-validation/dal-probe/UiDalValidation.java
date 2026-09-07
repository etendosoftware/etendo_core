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
    private static void verifyLoginRoleProjection() throws Exception {
        Class<?> login = Class.forName("org.openbravo.base.secureApp.LoginUtils");
        if (!login.getProtectionDomain().getCodeSource().getLocation().toString().contains("/platform-login-session/")) {
            throw new AssertionError("Login projection did not load from the canonical platform compilation");
        }
        var read = login.getDeclaredMethod("readRoleSession", String.class, String.class);
        read.setAccessible(true);
        var dal = OBDal.getInstance();
        var role = dal.get(org.openbravo.model.ad.access.Role.class, "R1");
        var client = role.getClient();
        Object[] values = (Object[]) read.invoke(null, "R1", "U1");
        if (values == null || !"O".equals(values[0]) || !client.getSearchKey().equals(values[1])) {
            throw new AssertionError("Generic login role/client values are incorrect");
        }
        for (String[] denied : new String[][] {{"R1", "0"}, {"missing", "U1"}, {"R1", "' OR 1=1 --"}}) {
            if (read.invoke(null, denied[0], denied[1]) != null) {
                throw new AssertionError("Login projection accepted an absent association or unbound input");
            }
        }
        try {
            // Fixture setup only: these legacy dictionary seeds have organization O1,
            // while ADRole's DAL write access level requires organization *. Do not
            // weaken production access-level checks to stage read-query controls.
            dal.getSession().createNativeMutationQuery("update ad_role set isactive='N' where ad_role_id='R1'").executeUpdate();
            if (read.invoke(null, "R1", "U1") != null) throw new AssertionError("Inactive role returned");
            dal.getSession().createNativeMutationQuery("update ad_role set isactive='Y' where ad_role_id='R1'").executeUpdate();
            dal.getSession().createNativeMutationQuery("update ad_client set isactive='N' where ad_client_id='C1'").executeUpdate();
            if (read.invoke(null, "R1", "U1") != null) throw new AssertionError("Inactive client returned");
        } finally {
            dal.getSession().createNativeMutationQuery("update ad_role set isactive='Y' where ad_role_id='R1'").executeUpdate();
            dal.getSession().createNativeMutationQuery("update ad_client set isactive='Y' where ad_client_id='C1'").executeUpdate();
        }
        System.out.println("PASS: Shared login role projection reads the ERP-free database and rejects missing/inactive scopes");
    }

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
                if (Boolean.getBoolean("validation.uiMenu")) verifyLoginRoleProjection();
                if (Boolean.getBoolean("validation.uiCache") || Boolean.getBoolean("validation.uiFields")) {
                    Class<?> cacheType = Class.forName(
                            "org.openbravo.client.application.window.ApplicationDictionaryCachedStructures");
                    cacheType.getDeclaredMethods();
                    var initializer = jakarta.enterprise.inject.se.SeContainerInitializer.newInstance()
                            .disableDiscovery().addBeanClasses(cacheType);
                    if (Boolean.getBoolean("validation.uiWindow")) {
                        for (String bean : java.util.List.of(
                                "org.openbravo.base.weld.WeldUtils",
                                "org.openbravo.client.kernel.TemplateProcessor$Registry",
                                "org.openbravo.client.kernel.freemarker.FreemarkerTemplateProcessor",
                                "org.openbravo.client.application.window.OBViewTab",
                                "org.openbravo.client.application.window.StandardWindowComponent",
                                "org.openbravo.client.application.window.OBViewFieldHandler",
                                "org.openbravo.client.application.window.OBViewFormComponent",
                                "org.openbravo.client.application.window.OBViewGridComponent",
                                "org.openbravo.client.application.CachedPreference",
                                "org.openbravo.service.datasource.DataSourceComponent",
                                "org.openbravo.service.datasource.DataSourceComponentProvider",
                                "org.openbravo.service.datasource.DataSourceServiceProvider",
                                "org.openbravo.service.datasource.DefaultDataSourceService",
                                "org.openbravo.service.datasource.NoteDataSource")) {
                            Class<?> beanType = Class.forName(bean);
                            // Resolve signatures before Weld can silently discard an incomplete bean.
                            for (var method : beanType.getDeclaredMethods()) method.getGenericReturnType();
                            for (var member : beanType.getDeclaredFields()) member.getGenericType();
                            initializer.addBeanClasses(beanType);
                        }
                    }
                    if (Boolean.getBoolean("validation.uiMenu")) {
                        for (String bean : java.util.List.of("org.openbravo.client.application.GlobalMenu",
                                "org.openbravo.client.application.MenuManager",
                                "org.openbravo.client.application.navigationbarcomponents.ApplicationMenuComponent")) {
                            initializer.addBeanClasses(Class.forName(bean));
                        }
                    }
                    try (var container = initializer.initialize()) {
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
                            if (Boolean.getBoolean("validation.uiWindow")) {
                                Class<?> viewType = Class.forName("org.openbravo.client.application.window.OBViewTab");
                                Object view = container.select(viewType).get();
                                viewType.getMethod("setTab", org.openbravo.model.ad.ui.Tab.class).invoke(view,
                                        OBDal.getInstance().get(org.openbravo.model.ad.ui.Tab.class, tabId));
                                viewType.getMethod("setRootTab", boolean.class).invoke(view, true);
                                viewType.getMethod("setGCSettings", java.util.Optional.class, java.util.Map.class)
                                        .invoke(view, java.util.Optional.empty(), java.util.Map.of(tabId, java.util.Optional.empty()));
                                var parameters = new java.util.HashMap<String, Object>();
                                parameters.put("Constants_FIELDSEPARATOR", "$");
                                parameters.put("Constants_IDENTIFIER", "_identifier");
                                viewType.getMethod("setParameters", java.util.Map.class).invoke(view, parameters);
                                String windowOutput = (String) viewType.getMethod("generate").invoke(view);
                                if (!windowOutput.contains("name: 'title'") || !windowOutput.contains("OBViewDataSource")
                                        || !windowOutput.contains("OBViewForm.create") || !windowOutput.contains("OBViewGrid")) {
                                    throw new AssertionError("Original tab omitted its fields, datasource, form or grid");
                                }
                                System.out.println("PASS: CDI-managed original tab composes application datasource, form and grid");
                                Class<?> windowType = Class.forName("org.openbravo.client.application.window.StandardWindowComponent");
                                for (var applicationWindow : dalWindows()) {
                                    Object windowComponent = container.select(windowType).get();
                                    windowType.getMethod("setWindow", org.openbravo.model.ad.ui.Window.class)
                                            .invoke(windowComponent, applicationWindow);
                                    windowType.getMethod("setParameters", java.util.Map.class).invoke(windowComponent,
                                            new java.util.HashMap<>(parameters));
                                    String completeWindow = (String) windowType.getMethod("generate").invoke(windowComponent);
                                    if (!completeWindow.contains("isc.ClassFactory.defineClass")
                                            || !completeWindow.contains("isc.OBStandardWindow")
                                            || !completeWindow.contains("isc.OBViewGrid.create")
                                            || !completeWindow.contains(applicationWindow.getId())) {
                                        throw new AssertionError("Original standard window was incomplete: " + applicationWindow.getName());
                                    }
                                    java.nio.file.Files.writeString(java.nio.file.Path.of("build", "ui-window-"
                                            + applicationWindow.getId() + ".js"), completeWindow);
                                }
                                System.out.println("PASS: Both original standard windows render through database-selected grid configuration");
                                verifySubtabHierarchy(tabId);
                                if (Boolean.getBoolean("validation.uiMenu")) {
                                    verifyBooleanMapping();
                                    var requestContext = container.select(jakarta.enterprise.context.control.RequestContextController.class).get();
                                    var originalContext = OBContext.getOBContext();
                                    try {
                                        for (String role : java.util.List.of("R1", "R_READ", "R_EXCLUDE")) {
                                            OBContext.setOBContext("U1", role, "C1", "O1", "en_US");
                                            requestContext.activate();
                                            try {
                                                Class<?> menuType = Class.forName("org.openbravo.client.application.navigationbarcomponents.ApplicationMenuComponent");
                                                Object menu = container.select(menuType).get();
                                                String menuOutput = (String) menuType.getMethod("generate").invoke(menu);
                                                boolean requests = menuOutput.contains("Requests");
                                                boolean categories = menuOutput.contains("Categories");
                                                if (requests != !role.equals("R_EXCLUDE") || categories != role.equals("R1")) {
                                                    throw new AssertionError("Original menu violates window access for " + role);
                                                }
                                                if (role.equals("R_READ") && !menuOutput.contains("readOnly: true")) {
                                                    throw new AssertionError("Original menu lost read-only window access");
                                                }
                                                java.nio.file.Files.writeString(java.nio.file.Path.of("build", "ui-menu-" + role + ".js"), menuOutput);
                                            } finally { requestContext.deactivate(); }
                                        }
                                    } finally { OBContext.setOBContext(originalContext); }
                                    System.out.println("PASS: Original menu renders own windows for editable/read-only roles and hides denied windows");
                                }
                            }
                            if (Boolean.getBoolean("validation.uiFieldDefinitions")) {
                                var settings = handlerClass.getDeclaredMethod("setGCSettings",
                                        java.util.Optional.class, java.util.Map.class);
                                settings.setAccessible(true);
                                settings.invoke(originalHandler, java.util.Optional.empty(),
                                        java.util.Map.of(tabId, java.util.Optional.empty()));
                                String fieldSource = java.nio.file.Files.readString(java.nio.file.Path.of(
                                        "../modules_core/org.openbravo.client.application/src/org/openbravo/client/application/templates/ob-view-field.js.ftl"));
                                // Invoke the unchanged production macro, including editor and grid properties.
                                String source = fieldSource + "\n[<#list fields as field><@createField field/>"
                                        + "<#if field_has_next>,</#if></#list>]";
                                var macroProcessor = new org.openbravo.client.kernel.freemarker.FreemarkerTemplateProcessor() {
                                    String render(String text, java.util.Map<String, Object> values) {
                                        return processTemplate(createTemplateImplementation(template, text), values);
                                    }
                                };
                                var values = new java.util.HashMap<String, Object>();
                                values.put("fields", originalFields);
                                values.put("Constants_FIELDSEPARATOR", "$");
                                values.put("Constants_IDENTIFIER", "_identifier");
                                String fieldOutput = macroProcessor.render(source, values);
                                if (!fieldOutput.contains("name: 'title'") || !fieldOutput.contains("name: 'category'")
                                        || !fieldOutput.contains("gridProps:")) {
                                    throw new AssertionError("Original field macro omitted application editors or grid properties");
                                }
                                System.out.println("PASS: Complete original field macro renders application editors and grid definitions");
                            }
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

    private static java.util.List<org.openbravo.model.ad.ui.Window> dalWindows() {
        var windows = OBDal.getInstance().createQuery(org.openbravo.model.ad.ui.Window.class, "").list();
        if (windows.size() != 2) throw new AssertionError("Expected both application windows");
        return windows;
    }

    /** Tests the shared Y/N mapping without rewriting HQL boolean literals as string predicates. */
    private static void verifyBooleanMapping() {
        var dal = OBDal.getInstance();
        var module = dal.get(org.openbravo.model.ad.module.Module.class, "PLATFORM");
        Boolean original = module.isEnabled();
        try {
            for (boolean value : new boolean[] {false, true}) {
                module.setEnabled(value);
                dal.flush();
                Long literal = dal.getSession().createQuery("select count(m) from ADModule m"
                        + " where m.id = :id and m.enabled = " + value, Long.class)
                        .setParameter("id", module.getId()).uniqueResult();
                Long parameter = dal.getSession().createQuery("select count(m) from ADModule m"
                        + " where m.id = :id and m.enabled = :enabled", Long.class)
                        .setParameter("id", module.getId()).setParameter("enabled", value).uniqueResult();
                if (literal != 1L || parameter != 1L) throw new AssertionError("Boolean literal/parameter mapping differs");
                Long legacy = dal.getSession().createQuery("select count(m) from ADModule m"
                        + " where m.id = :id and m.enabled = '" + (value ? "Y" : "N") + "'", Long.class)
                        .setParameter("id", module.getId()).uniqueResult();
                if (legacy != 1L) throw new AssertionError("Legacy Y/N HQL literals changed");
                Object stored = dal.getSession().createNativeQuery("select enabled from ad_module where ad_module_id = :id")
                        .setParameter("id", module.getId()).uniqueResult();
                if (!String.valueOf(stored).equals(value ? "Y" : "N")) throw new AssertionError("Noncanonical boolean storage");
                dal.getSession().evict(module);
                module = dal.get(org.openbravo.model.ad.module.Module.class, "PLATFORM");
                if (!Boolean.valueOf(value).equals(module.isEnabled())) throw new AssertionError("Boolean extraction differs");
            }
            var yesNo = org.openbravo.base.session.OBYesNoType.INSTANCE;
            if (!yesNo.getJavaTypeDescriptor().areEqual(null, Boolean.FALSE)
                    || yesNo.getJavaTypeDescriptor().areEqual(null, Boolean.TRUE)
                    || yesNo.isEqual(null, Boolean.FALSE)) {
                throw new AssertionError("Legacy descriptor/type null-comparison semantics changed");
            }
            System.out.println("PASS: Y/N mapping preserves boolean/string literals, parameters, storage, extraction and legacy null comparisons");
        } finally {
            module.setEnabled(original);
            dal.flush();
        }
    }

    /** Exercises SQLC-compatible hierarchy boundaries through the canonical Hibernate API. */
    private static void verifySubtabHierarchy(String rootId) throws Exception {
        var dal = OBDal.getInstance();
        var root = dal.get(org.openbravo.model.ad.ui.Tab.class, rootId);
        var added = new java.util.ArrayList<org.openbravo.model.ad.ui.Tab>();
        long[][] positions = {{20, 1}, {30, 2}, {40, 1}, {50, 0}, {60, 1}, {999999, 1}, {25, 1}};
        try {
            for (long[] position : positions) {
                var child = org.openbravo.base.provider.OBProvider.getInstance().get(org.openbravo.model.ad.ui.Tab.class);
                child.setClient(root.getClient());
                child.setOrganization(root.getOrganization());
                child.setModule(root.getModule());
                child.setTable(root.getTable());
                child.setWindow(position[0] == 25 ? dalWindows().stream()
                        .filter(window -> !window.getId().equals(root.getWindow().getId())).findFirst().orElseThrow()
                        : root.getWindow());
                child.setName("Hierarchy " + position[0]);
                child.setSequenceNumber(position[0]);
                child.setTabLevel(position[1]);
                child.setUIPattern("STD");
                child.setActive(position[0] != 40);
                dal.save(child);
                added.add(child);
            }
            dal.flush();
            Class<?> kernelType = Class.forName("org.openbravo.client.kernel.KernelUtils");
            Object kernel = kernelType.getMethod("getInstance").invoke(null);
            var traversal = kernelType.getMethod("getTabSubtabs", org.openbravo.model.ad.ui.Tab.class, boolean.class);
            Object[][] cases = {
                    {root, false, java.util.Set.of(20L, 30L, 40L)},
                    {root, true, java.util.Set.of(20L, 40L)},
                    {added.get(0), false, java.util.Set.of(30L)},
                    {added.get(0), true, java.util.Set.of(30L)},
                    {added.get(1), false, java.util.Set.of()},
                    {added.get(3), false, java.util.Set.of(60L)},
                    {added.get(3), true, java.util.Set.of(60L)}
            };
            for (Object[] test : cases) {
                var actual = new java.util.HashSet<Long>();
                for (Object result : (java.util.List<?>) traversal.invoke(kernel, test[0], test[1])) {
                    var child = (org.openbravo.model.ad.ui.Tab) result;
                    if (!child.getWindow().getId().equals(root.getWindow().getId())) {
                        throw new AssertionError("Subtab traversal crossed window boundaries");
                    }
                    actual.add(child.getSequenceNumber());
                }
                if (!actual.equals(test[2])) throw new AssertionError("Subtab hierarchy mismatch: " + actual + " != " + test[2]);
            }
            System.out.println("PASS: Hibernate subtab traversal preserves seven SQLC hierarchy and inactive-metadata cases");
        } finally {
            for (var child : added) dal.remove(child);
            dal.flush();
        }
    }
}
