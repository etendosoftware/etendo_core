# DAL core

Existing session, mapping, persistence lifecycle and context implementation.
OBContext currently combines shared security context with legacy typed APIs.
ErpContextSupport isolates ERP association initialization during incremental
extraction; Warehouse remains a required compatibility type at this stage.

Search `MODULE-BOUNDARY` in these sources and consult
[the extraction map](../../../../../docs/platform-module-boundaries.md) for target
ownership, legacy signature constraints and acceptance gates.
