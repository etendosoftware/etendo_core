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
