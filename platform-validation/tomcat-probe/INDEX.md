# Tomcat integration probe

Deploy the actual WAR in Apache Tomcat 10.1.59, embedded only as an isolated test
launcher. It uses a real Catalina webapp classloader, WAR expansion, lifecycle,
connector, and loopback HTTP port. No application classes are added to the server
classpath. HTTP checks run before and after stopping and redeploying the WAR.
The process and disposable PostgreSQL are stopped at the end of verifyTomcat.

CompatibilityServer runs the external-database WAR at the exact /etendo context
on a configurable loopback port and remains available until explicitly stopped.

ProductHttpValidation checks the running HTTP contract and context rejection.
ProductDatabaseValidation independently compares its test-client rows with JDBC
and confirms foreign-client control rows are excluded.

ErpUiServer verifies the owned Classic-copy Docker label and port before expanding
the ERP UI WAR into a private deployment directory. Configuration is written only
there, with scheduler, import, cluster and Redis disabled. It never deploys full
ERP against the original database configuration.
