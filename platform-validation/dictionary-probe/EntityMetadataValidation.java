package com.etendoerp.platform.validation;

import java.util.List;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;

/** Verifies identifier join metadata without a database, servlet or UI kernel. */
public final class EntityMetadataValidation {
    public static void main(String[] args) throws Exception {
        Entity entity = new Entity();
        entity.setIdentifierProperties(List.of());
        check(!entity.hasNullableIdentifierProperties(), "Empty identifier must not be nullable");
        Property mandatory = new Property();
        mandatory.setMandatory(true);
        entity.setIdentifierProperties(List.of(mandatory));
        check(!entity.hasNullableIdentifierProperties(), "Mandatory identifier must not be nullable");
        Property optional = new Property();
        optional.setMandatory(false);
        entity.setIdentifierProperties(List.of(mandatory, optional));
        check(entity.hasNullableIdentifierProperties(), "Optional identifier must preserve outer joins");
        entity.setIdentifierProperties(List.of(optional, mandatory));
        check(entity.hasNullableIdentifierProperties(), "Identifier order must not change nullability");
        try {
            Class.forName("org.openbravo.client.kernel.KernelUtils", false,
                    EntityMetadataValidation.class.getClassLoader());
            throw new AssertionError("UI kernel must not be available to this model test");
        } catch (ClassNotFoundException expected) {
            // Check the actual test classpath, not a mocked UI implementation.
        }
        System.out.println("PASS: Identifier metadata without database or UI kernel");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
