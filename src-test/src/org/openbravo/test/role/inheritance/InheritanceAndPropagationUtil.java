package org.openbravo.test.role.inheritance;

import java.util.Arrays;
import java.util.List;

public class InheritanceAndPropagationUtil {

    public static final List<String> organizations = Arrays.asList("F&B España - Región Norte", "F&B España - Región Sur");
    public static final List<String> windows = Arrays.asList("Sales Invoice", "Sales Order");
    public static final List<String> tabs = Arrays.asList("Bank Account", "Basic Discount");
    public static final List<String> fields = Arrays.asList("Business Partner Category", "Commercial Name");
    public static final List<String> reports = Arrays.asList("Alert Process", "Create Variants");
    public static final List<String> forms = Arrays.asList("About", "Menu");
    public static final List<String> widgets = Arrays.asList("Best Sellers", "Invoices to collect");
    public static final List<String> views = Arrays.asList("OBUIAPP_AlertManagement", RoleInheritanceTestUtils.DUMMY_VIEW_IMPL_NAME);
    public static final List<String> processes = Arrays.asList("Create Purchase Order Lines", "Grant Portal Access");
    public static final List<String> tables = Arrays.asList("AD_User", "C_Order");
    public static final List<String> alerts = Arrays.asList("Alert Taxes: Inversión del Sujeto Pasivo", "CUSTOMER WITHOUT ACCOUNTING");
    public static final List<String> preferences = Arrays.asList("AllowAttachment", "AllowDelete");

    public static final List<List<String>> accesses = Arrays.asList(organizations, windows, tabs, fields,
            reports, forms, widgets, views, processes, tables, alerts, preferences);
}
