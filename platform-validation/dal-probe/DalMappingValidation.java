package com.etendoerp.platform.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.dal.core.DalMappingGenerator;
import org.openbravo.dal.core.DalSessionFactoryController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import com.etendoerp.platform.fixture.Category;
import com.etendoerp.platform.fixture.Request;

/** Uses the real DAL mapping and session factory implementations, without replacement mappings. */
public final class DalMappingValidation {
    private DalMappingValidation() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected disposable properties path");
        org.openbravo.base.session.OBPropertiesProvider.getInstance().setProperties(args[0]);
        verify();
    }

    public static void verify() throws Exception {
        verifyReadableScopeRules();
        for (String absentType : new String[] {
                "org.openbravo.model.common.enterprise.Warehouse",
                "org.openbravo.client.application.Process",
                "org.openbravo.model.common.businesspartner.BusinessPartner"}) {
            try {
                Class.forName(absentType, false, DalMappingValidation.class.getClassLoader());
                throw new AssertionError("Excluded module type remains on the minimal runtime: " + absentType);
            } catch (ClassNotFoundException expected) {
                // Absence must hold on the actual runtime classpath, not just in the dictionary.
            }
        }
        for (var entity : ModelProvider.getInstance().getModel()) {
            Class<?> type = entity.getMappingClass();
            if (type == null) throw new AssertionError("Uncompiled entity " + entity.getClassName());
            OBProvider.getInstance().register(type, type, false);
            OBProvider.getInstance().register(entity.getName(), type, false);
        }
        String mapping = DalMappingGenerator.getInstance().generateMapping();
        Files.writeString(Path.of("build/dal-mapping.hbm.xml"), mapping);
        for (var entity : ModelProvider.getInstance().getModel()) {
            if (!mapping.contains(entity.getClassName())) throw new AssertionError("Unmapped entity " + entity.getName());
        }
        DalSessionFactoryController controller = new DalSessionFactoryController();
        SessionFactoryController.setInstance(controller);
        try {
            var factory = controller.getSessionFactory();
            try (var session = factory.openSession()) {
                Long count = session.createQuery("select count(r) from ProofRequest r where r.title = :title", Long.class)
                        .setParameter("title", "Not inserted yet").getSingleResult();
                if (count != 0) throw new AssertionError("Unexpected application data");
            }
            System.out.println("PASS: Real DAL SessionFactory loaded all generated entities and executed parameterized HQL");
            if ("v2".equals(System.getProperty("validation.phase"))) verifyUpgradedEntity();
            else if (Boolean.getBoolean("validation.obdal")) verifyPersistence();
        } finally {
            try {
                if (SessionHandler.existsOpenedSessions()) OBDal.getInstance().rollbackAndClose();
            } finally {
                OBContext.setOBContext((OBContext) null);
                SessionFactoryController.setInstance(null);
            }
        }
    }

    private static void verifyUpgradedEntity() throws Exception {
        OBContext.setOBContext("U1", "R1", "C1", "O1", "en_US");
        var property = ModelProvider.getInstance().getEntity("ProofRequest").getProperty("description");
        if (!"DESCRIPTION".equals(property.getColumnName())) throw new AssertionError("Missing upgraded dictionary property");
        var getter = Request.class.getMethod("getDescription");
        var setter = Request.class.getMethod("setDescription", String.class);
        Request request = OBDal.getInstance().get(Request.class, "REQUEST_1");
        if (!"Library access".equals(request.getTitle()) || !"General service requests".equals(request.getCategory().getName())) {
            throw new AssertionError("Operational row or managed category was not preserved/upgraded");
        }
        setter.invoke(request, "Updated through generated DAL v2");
        OBDal.getInstance().commitAndClose();
        var query = OBDal.getInstance().createQuery(Request.class, "description=:description")
                .setNamedParameter("description", "Updated through generated DAL v2");
        if (query.list().size() != 1 || !"Updated through generated DAL v2".equals(getter.invoke(query.list().get(0)))) {
            throw new AssertionError("Generated v2 property did not round-trip through OBDal");
        }
        if (OBDal.getInstance().createQuery(Request.class, "").list().size() != 1) throw new AssertionError("Upgrade lost access filters");
        OBDal.getInstance().commitAndClose();
        System.out.println("PASS: Generated v2 Java accessor, dictionary mapping and OBDal HQL use the XML-added property");
    }

    private static void verifyReadableScopeRules() {
        var system = org.openbravo.dal.security.ReadableScopeResolver.clients("S", () -> {
            throw new AssertionError("System scope must not load the role client");
        });
        if (!java.util.Arrays.equals(system, new String[] {"0"})) throw new AssertionError("System scope changed");
        for (String level : new String[] {"C", "O", "CO"}) {
            var clients = org.openbravo.dal.security.ReadableScopeResolver.clients(level, () -> "C1");
            if (!java.util.Arrays.equals(clients, new String[] {"C1", "0"})) throw new AssertionError("Client scope changed");
        }
        var root = org.openbravo.dal.security.ReadableScopeResolver.organizations(java.util.List.of("0", "O1"),
                () -> java.util.List.of("O1", "O2"), ignored -> { throw new AssertionError("Root grant must use client scope"); });
        if (!java.util.Set.of(root).equals(java.util.Set.of("0", "O1", "O2"))) throw new AssertionError("Root expansion changed");
        var branch = org.openbravo.dal.security.ReadableScopeResolver.organizations(java.util.List.of("O1", "O1"),
                () -> { throw new AssertionError("Branch grant must not expand to the entire client"); },
                ignored -> java.util.List.of("O1", "CHILD"));
        if (!java.util.Set.of(branch).equals(java.util.Set.of("0", "O1", "CHILD"))) throw new AssertionError("Branch expansion changed");
        var empty = org.openbravo.dal.security.ReadableScopeResolver.organizations(java.util.List.of(),
                () -> { throw new AssertionError("No grant must not load client organizations"); },
                ignored -> { throw new AssertionError("No grant must not load a tree"); });
        if (!java.util.Arrays.equals(empty, new String[] {"0"})) throw new AssertionError("Shared root scope changed");
        System.out.println("PASS: Shared readable-scope rules preserve client, root and branch isolation");
    }

    private static void verifyPersistence() {
        try {
            Request.class.getDeclaredMethod("getDescription");
            throw new AssertionError("The v1 process unexpectedly loaded a v2 generated entity");
        } catch (NoSuchMethodException expected) {
            // The second JVM must genuinely load a different generated Java shape.
        }
        OBContext.setOBContext("U1", "R1", "C1", "O1", "en_US");
        if (!OBContext.getOBContext().isInitialized() || OBContext.getOBContext().isInAdministratorMode()) {
            throw new AssertionError("Expected a real initialized non-admin context");
        }
        Category category = OBDal.getInstance().get(Category.class, "GENERAL");
        if (category == null || !"General requests".equals(category.getName())) throw new AssertionError("Missing v1 managed XML category");
        Category local = new Category();
        local.setId("LOCAL_CREATED");
        local.setNewOBObject(true);
        local.setName("Created through OBDal");
        local.setActive(true);
        OBDal.getInstance().save(local);
        Request request = new Request();
        request.setId("REQUEST_1");
        request.setNewOBObject(true);
        request.setTitle("Library access");
        request.setCategory(category);
        request.setActive(true);
        OBDal.getInstance().save(request);
        OBDal.getInstance().commitAndClose();
        var found = OBDal.getInstance().createQuery(Request.class, "category.name=:name")
                .setNamedParameter("name", "General requests").list();
        if (found.size() != 1 || !"Library access".equals(found.get(0).getTitle())) {
            throw new AssertionError("OBDal relationship query failed");
        }
        var filtered = OBDal.getInstance().createQuery(Request.class, "");
        if (filtered.list().size() != 1) throw new AssertionError("Default organization/client/active filters failed");
        filtered = OBDal.getInstance().createQuery(Request.class, "");
        filtered.setFilterOnReadableOrganization(false);
        if (filtered.list().size() != 2) throw new AssertionError("Client isolation failed with organization filter disabled");
        filtered = OBDal.getInstance().createQuery(Request.class, "");
        filtered.setFilterOnReadableOrganization(false);
        filtered.setFilterOnReadableClients(false);
        if (filtered.list().size() != 3) throw new AssertionError("Cross-client control rows are missing");
        filtered = OBDal.getInstance().createQuery(Request.class, "");
        filtered.setFilterOnReadableOrganization(false);
        filtered.setFilterOnReadableClients(false);
        filtered.setFilterOnActive(false);
        if (filtered.list().size() != 4) throw new AssertionError("Inactive control row is missing");
        OBDal.getInstance().commitAndClose();
        Request rolledBack = new Request();
        rolledBack.setId("ROLLBACK");
        rolledBack.setNewOBObject(true);
        rolledBack.setTitle("Not committed");
        rolledBack.setCategory(OBDal.getInstance().get(Category.class, "GENERAL"));
        rolledBack.setActive(true);
        OBDal.getInstance().save(rolledBack);
        OBDal.getInstance().flush();
        OBDal.getInstance().rollbackAndClose();
        if (!OBDal.getInstance().createQuery(Request.class, "id=:id").setNamedParameter("id", "ROLLBACK").list().isEmpty()) {
            throw new AssertionError("OBDal flushed insert survived rollback");
        }
        try {
            OBDal.getInstance().createQuery(org.openbravo.model.ad.system.Language.class, "").list();
            throw new AssertionError("Missing table grant did not deny access");
        } catch (org.openbravo.base.exception.OBSecurityException expected) {
            // A missing grant must not inherit access from an absent ERP window.
        }
        System.out.println("PASS: Non-admin OBDal persisted related entities, queried them and denied an ungranted entity");
        System.out.println("PASS: Real OBDal client, organization and active filters isolated control rows; flushed insert rolled back");
        OBDal.getInstance().commitAndClose();
        OBContext.setOBContext("U1", "R_READ", "C1", "O1", "en_US");
        if (OBDal.getInstance().createQuery(Request.class, "").list().size() != 1) throw new AssertionError("Read-only grant cannot read");
        try {
            OBDal.getInstance().save(new Request());
            throw new AssertionError("Read-only grant allowed a write");
        } catch (org.openbravo.base.exception.OBSecurityException expected) {
            OBDal.getInstance().rollbackAndClose();
        }
        for (String role : new String[] {"R_EXCLUDE", "R_INACTIVE"}) {
            OBContext.setOBContext("U1", role, "C1", "O1", "en_US");
            try {
                OBDal.getInstance().createQuery(Request.class, "").list();
                throw new AssertionError("Excluded or inactive grant allowed access: " + role);
            } catch (org.openbravo.base.exception.OBSecurityException expected) {
                OBDal.getInstance().rollbackAndClose();
            }
        }
        System.out.println("PASS: Read-only, excluded and inactive table grants enforce their access restrictions");
    }
}
