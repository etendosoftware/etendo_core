package com.etendoerp.platform.validation;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/** Audits actual WAR and nested dependency class entries without loading application code. */
public final class WarBoundaryValidation {
    private static final Set<String> UI_ENTRY_POINTS = Set.of(
            "org/openbravo/client/kernel/KernelServlet",
            "org/openbravo/client/kernel/BaseKernelServlet",
            "org/openbravo/client/kernel/BaseTemplateComponent",
            "org/openbravo/service/datasource/DataSourceServlet");
    private static final Set<String> ERP_ENTITIES = Set.of(
            "org/openbravo/model/common/plm/Product",
            "org/openbravo/model/common/enterprise/Warehouse",
            "org/openbravo/model/common/businesspartner/BusinessPartner");

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--verify-rules".equals(args[0])) {
            verifyRules();
            return;
        }
        if (args.length != 3 || !Set.of("platform", "erp-headless").contains(args[1])) {
            throw new IllegalArgumentException("Expected WAR, platform|erp-headless and report path");
        }
        Path war = Path.of(args[0]), report = Path.of(args[2]);
        List<String> classes = new ArrayList<>(), violations = new ArrayList<>();
        int libraries = 0;
        try (ZipFile zip = new ZipFile(war.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("WEB-INF/classes/") && name.endsWith(".class")) {
                    inspect(name.substring("WEB-INF/classes/".length()), "WAR", args[1], classes, violations);
                } else if (name.startsWith("WEB-INF/lib/") && name.endsWith(".jar")) {
                    libraries++;
                    try (ZipInputStream nested = new ZipInputStream(zip.getInputStream(entry))) {
                        java.util.zip.ZipEntry child;
                        while ((child = nested.getNextEntry()) != null) {
                            if (child.getName().endsWith(".class")) {
                                inspect(child.getName(), name, args[1], classes, violations);
                            }
                        }
                    }
                }
            }
        }
        if (classes.isEmpty() || libraries == 0) throw new AssertionError("Missing application classes or libraries");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(war)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        classes.sort(String::compareTo);
        Files.createDirectories(report.toAbsolutePath().getParent());
        Files.writeString(report, "Profile: " + args[1] + "\nSHA-256: " + HexFormat.of().formatHex(digest.digest())
                + "\nLibraries: " + libraries + "\nClass entries: " + classes.size()
                + "\nViolations: " + violations.size() + "\n" + String.join("\n", violations)
                + "\nClass origins:\n" + String.join("\n", classes) + "\n");
        if (!violations.isEmpty()) throw new AssertionError("WAR boundary violations: " + String.join(", ", violations));
        System.out.println("PASS: " + args[1] + " nested WAR audit; " + libraries + " libraries, " + classes.size() + " class entries");
    }

    private static void inspect(String entry, String origin, String profile,
            List<String> classes, List<String> violations) {
        String type = entry.replaceFirst("^META-INF/versions/[0-9]+/", "");
        type = type.substring(0, type.length() - ".class".length());
        String topLevel = type.split("\\$", 2)[0];
        classes.add(origin + "!" + entry);
        boolean forbidden = type.startsWith("org/apache/jasper/") || UI_ENTRY_POINTS.contains(topLevel);
        forbidden |= type.startsWith("com/etendoerp/client/application/")
                || type.startsWith("com/smf/smartclient/") || type.startsWith("com/smf/userinterface/")
                || ((type.startsWith("org/openbravo/") || type.startsWith("com/etendoerp/")
                        || type.startsWith("com/smf/")) && topLevel.endsWith("ComponentProvider"));
        if ("platform".equals(profile)) {
            forbidden |= ERP_ENTITIES.contains(topLevel) || type.startsWith("org/openbravo/client/")
                    || type.startsWith("org/openbravo/userinterface/");
        }
        if (forbidden) violations.add(origin + "!" + entry);
    }

    private static void verifyRules() {
        String[][] cases = {
            {"platform", "org/openbravo/model/common/plm/Product.class", "true"},
            {"erp-headless", "org/openbravo/model/common/plm/Product.class", "false"},
            {"platform", "org/openbravo/client/application/Process.class", "true"},
            {"erp-headless", "com/etendoerp/client/application/UIComponentProvider.class", "true"},
            {"erp-headless", "com/smf/jobs/defaults/provider/JobsComponentProvider.class", "true"},
            {"erp-headless", "org/openbravo/client/kernel/KernelServlet$Nested.class", "true"},
            {"erp-headless", "META-INF/versions/17/org/apache/jasper/servlet/JspServlet.class", "true"},
            {"platform", "META-INF/versions/17/org/openbravo/model/common/enterprise/Warehouse.class", "true"},
            {"platform", "com/etendoerp/platform/fixture/Request.class", "false"},
            {"erp-headless", "org/openbravo/service/datasource/DefaultDataSourceService.class", "false"}
        };
        for (String[] test : cases) {
            List<String> entries = new ArrayList<>(), violations = new ArrayList<>();
            inspect(test[1], "nested-test.jar", test[0], entries, violations);
            if ((!violations.isEmpty()) != Boolean.parseBoolean(test[2]) || entries.size() != 1) {
                throw new AssertionError("Boundary rule failed: " + test[0] + " " + test[1]);
            }
        }
        System.out.println("PASS: WAR boundary rules reject forbidden, inner and multi-release types");
    }
}
