# Classic compatibility probes

Read the existing Classic dictionary using real ModelProvider and separately
compiled generated entities. Database connections enforce read-only transactions;
the probes do not install modules or update the schema.

LegacyContextCaller is excluded from the normal source set. Its dedicated compile
task uses only the supplied Classic WAR API; verifyLegacyContextLinkage executes
that binary against the refactored ERP output and checks the actual class origin.
It covers thread context, Warehouse-typed accessors and the existing UI flag,
without database access. This is representative linkage, not all module APIs.

UiResourceValidation has its own compile task and verifies the resource extension
from platform-ui-components.jar, preserving legacy ordering and mode flags while
allowing alternative application contributions. It does not prove UI startup.

## Profile policy composition

`UserInfoPolicyValidation` checks explicit platform/default ERP selection, immutable
application policy and fail-closed blank/missing/wrong-type configuration in fresh JVMs.
