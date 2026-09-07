# Platform validation

Run `./gradlew -p platform-validation verifyPlatform` from the repository root
with JDK 17, Docker, and a local postgres:16 image. This executes real generated
DAL v1/v2, security, XML migrations, preservation, and idempotence on disposable
PostgreSQL without installing ERP business modules.
See ../docs/platform-validation.md for prerequisites, provenance, and limits.

- src: executable validation.
- fixtures: XML schema and managed reference data.
- dictionary-probe: dictionary, security data, generation, and upgrade orchestration.
- dal-probe: actual OBDal runtime and security checks in separate JVMs.
- build.gradle: isolated dependencies and verification tasks.
- ui-dependency-inventory.mjs: read-only UI source dependency inventory and matching controls.
- ui-closure-inventory.mjs: JDK static UI closure and batch generated-model API gap report; not a runtime completeness gate.
- impact-classpath.init.gradle: guarded export of the UI login child classpath without executing the fixture.
- ui-navigation-validation.mjs: evaluate original generated navigation descriptors and dynamic role gating without launching a browser.
- web and webapp: deployable WAR runtime and Servlet 6.0 descriptor.
- tomcat-probe: real WAR deployment, HTTP checks, and redeployment in isolated Tomcat.
- classic-probe: read-only external dictionary bootstrap and entity generation.
- classic-service-probe: authenticated original Product datasource fetch validation.
- compat-web: external-database Product HTTP adapter and platform-only lifecycle.
- classicJson source set: original JSON/datasource implementations compiled against
  explicit Classic WAR support classes; runtime integration remains separate.

Build the WAR with `./gradlew -p platform-validation war`; verify deployment with
`./gradlew -p platform-validation verifyTomcat`. Neither leaves a server running.
