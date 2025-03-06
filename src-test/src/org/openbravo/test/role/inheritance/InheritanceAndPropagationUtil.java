package org.openbravo.test.role.inheritance;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class that provides predefined lists of various system elements such as
 * organizations, windows, tabs, fields, reports, and other access-related entities.
 * <p>
 * This class is designed to be used as a constant holder and cannot be instantiated.
 * </p>
 */
public class InheritanceAndPropagationUtil {

    private InheritanceAndPropagationUtil() {
    }

    protected static final List<String> organizations = Arrays.asList("F&B España - Región Norte", "F&B España - Región Sur");
    protected static final List<String> windows = Arrays.asList("Sales Invoice", "Sales Order");
    protected static final List<String> tabs = Arrays.asList("Bank Account", "Basic Discount");
    protected static final List<String> fields = Arrays.asList("Business Partner Category", "Commercial Name");
    protected static final List<String> reports = Arrays.asList("Alert Process", "Create Variants");
    protected static final List<String> forms = Arrays.asList("About", "Menu");
    protected static final List<String> widgets = Arrays.asList("Best Sellers", "Invoices to collect");
    protected static final List<String> views = Arrays.asList("OBUIAPP_AlertManagement", RoleInheritanceTestUtils.DUMMY_VIEW_IMPL_NAME);
    protected static final List<String> processes = Arrays.asList("Create Purchase Order Lines", "Grant Portal Access");
    protected static final List<String> tables = Arrays.asList("AD_User", "C_Order");
    protected static final List<String> alerts = Arrays.asList("Alert Taxes: Inversión del Sujeto Pasivo", "CUSTOMER WITHOUT ACCOUNTING");
    protected static final List<String> preferences = Arrays.asList("AllowAttachment", "AllowDelete");

    protected static final List<List<String>> accesses = Arrays.asList(organizations, windows, tabs, fields,
            reports, forms, widgets, views, processes, tables, alerts, preferences);
}
