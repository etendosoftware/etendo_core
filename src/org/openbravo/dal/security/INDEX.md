# DAL security

Existing authorization checks and organization-tree providers.
ReadableScopeResolver owns pure readable client/organization rules with only JDK
dependencies, targeted for platform-core. OBContext supplies legacy metadata and
tree adapters. EntityAccessChecker still contains the legacy window/process policy.
See docs/platform-module-boundaries.md for searchable extraction markers and gates.
