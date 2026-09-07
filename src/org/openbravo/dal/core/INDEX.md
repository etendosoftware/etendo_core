# DAL core

Existing session, mapping, persistence lifecycle and context implementation.
OBContext currently combines shared security context with legacy typed APIs.
ErpContextSupport isolates ERP association initialization during incremental
extraction. Warehouse is required only by the complete ERP facade; the generated
platform variant excludes the explicitly marked ERP compatibility sections.

Search `MODULE-BOUNDARY` in these sources and consult
[the extraction map](../../../../../docs/platform-module-boundaries.md) for target
ownership, legacy signature constraints and acceptance gates.
