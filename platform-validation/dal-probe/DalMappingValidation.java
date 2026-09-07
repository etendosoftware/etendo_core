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

    public static void verify() throws Exception {
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
            if (Boolean.getBoolean("validation.obdal")) verifyPersistence();
        } finally {
            try {
                if (SessionHandler.existsOpenedSessions()) OBDal.getInstance().rollbackAndClose();
            } finally {
                OBContext.setOBContext((OBContext) null);
                SessionFactoryController.setInstance(null);
            }
        }
    }

    private static void verifyPersistence() {
        OBContext.setOBContext("U1", "R1", "C1", "O1", "en_US");
        if (!OBContext.getOBContext().isInitialized() || OBContext.getOBContext().isInAdministratorMode()) {
            throw new AssertionError("Expected a real initialized non-admin context");
        }
        Category category = new Category();
        category.setId("GENERAL");
        category.setNewOBObject(true);
        category.setName("General requests");
        category.setActive(true);
        OBDal.getInstance().save(category);
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
            OBDal.getInstance().createQuery(org.openbravo.model.common.enterprise.Warehouse.class, "").list();
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
