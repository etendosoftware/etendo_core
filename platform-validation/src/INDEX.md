# Validation source

An executable component-level integration check. No Etendo application classes or
test doubles are added to the runtime classpath.

ClassicDatabaseValidation separately checks an external Classic PostgreSQL
configuration in a server-enforced read-only transaction. It verifies dictionary,
module registrations, and persisted products without booting the application.

ClassicDatabaseCopy snapshots the existing database read-only and restores an
owned local copy for Classic lifecycle tests. Private configuration stays in an
ignored mode-0700 directory; the source database is never modified.
