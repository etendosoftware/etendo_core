# XML fixtures

Two related non-ERP tables, one schema evolution, and managed category data.
The formats are consumed by DBSM rather than translated by a custom XML parser.
categories.xml provides initial managed data; categories-v2.xml renames GENERAL
and adds IT for repeat-safe reconciliation. LOCAL is created only by the test and
is deliberately excluded from the managed dataset.

ui-beans.xml is packaged as META-INF/beans.xml only in the optional UI artifact
to enable explicit annotated CDI discovery of the canonical application provider.
