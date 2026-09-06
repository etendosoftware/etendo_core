package com.etendoerp.platform.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.dal.core.DalMappingGenerator;
import org.openbravo.dal.core.DalSessionFactoryController;

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
        } finally {
            SessionFactoryController.setInstance(null);
        }
    }
}
