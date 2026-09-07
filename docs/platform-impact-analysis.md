# Platform login impact analysis

## Scope

Use Java Impact Analyzer 0.2.0 against the existing `verifyUiLogin` child-JVM
composition. Do not build the target, execute application code, access databases,
or alter running services. Reports are diagnostic, not deployment approval.

The analyzer's initial platform profile uses DAL security model classes and omits
the UI-generated classes, ui-fields, and ui-window outputs used by the fixture.
Resolve the actual Gradle runtime dependencies and preserve classpath precedence
before interpreting missing collaborators as extraction work.

The existing generated UI class directory must be selected explicitly. Its
presence does not establish source/binary correspondence or prove that it came
from a successful run. Record that limitation alongside analysis results.

## Procedure

The `platform-validation/impact-classpath.init.gradle` task reads the first
configuration action of `verifyUiLogin` to obtain its child runtime classpath.
It does not execute the JavaExec action or dependencies. Guards require the
expected task shape and the child classpath property. Review this integration
if the verification task's configuration actions change.

Supply `impactGeneratedClasses`, `impactTemplate`, and `impactOutput`. The
template is the analyzer's platform-login JSON profile. The output is a new JSON
configuration, not an overwrite of the analyzer agent's profile. Run the analyzer
separately. Use the same artifacts for a bounded baseline and expanded body scope.

Record phase completion, excluded bodies, unresolved dependencies, selected
definition origins, and paths to ERP entities. A completed bounded traversal is
not a complete login implementation audit. No absence result authorizes deletion.

## Reproduction

From the repository root, with JDK 17 selected:

```sh
./gradlew -p platform-validation -I impact-classpath.init.gradle exportUiLoginImpact \
  -PimpactGeneratedClasses=build/ui-dal-classes-15421033937214347934 \
  -PimpactTemplate=/Users/sebastianbarrozo/Documents/work/epic/java-impact-analyzer/profiles/etendo-platform-login.json \
  -PimpactOutput=/private/tmp/et27-login-impact-reproduction.json --offline

/Users/sebastianbarrozo/Documents/work/epic/java-impact-analyzer/build/install/java-impact-analyzer/bin/java-impact-analyzer \
  analyze /private/tmp/et27-login-impact-reproduction.json
```

Use a new output filename for each export; existing configurations are protected.
The generated directory above was selected from existing local outputs by timestamp
and checked for the fixture and Window classes. The historical successful test did
not persist its classpath, so exact identity with that past JVM cannot be attested.
The export uses the current verification task's runtime configuration, not the
analyzer's substitute classpath. Absent Gradle resource directories contain no
bytecode and are reported; missing code artifacts fail the export.

For expanded analysis, copy the JSON to a separate file, select a distinct report
prefix, and append these exact class patterns to `bodyIncludes`:

```json
[
  "org\\.openbravo\\.erpCommon\\.utility\\.Utility",
  "org\\.openbravo\\.erpCommon\\.businessUtility\\.Preferences"
]
```

These are additional patterns, not a replacement for the login contract patterns.

## Independently executed results — 2026-09-07

Analyzer installation reports version 0.2.0. Its repository HEAD at inspection was
0840111 with documentation/evidence changes already present. This run did not rebuild
the analyzer or rerun its unit tests; executable/build correspondence is not attested.

The exporter resolved 103 ordered existing artifacts. Analysis of
`LoginUtils.fillSessionArguments` completed with a 2048 MB heap and a 60-second limit:

| Scope | Wall time | Expanded method bodies | Missing-method findings |
|---|---:|---:|---:|
| Original bounded login patterns, fixture classpath | 10.793 s | 16 | 0 |
| Login plus Utility and Preferences | 13.977 s | 28 | 1 |

Full local reports: `/private/tmp/et27-login-impact-actual-report.json` and
`/private/tmp/et27-login-impact-expanded-v2-report.json` (with companion Markdown).
An intermediate Utility-only expansion also completed; it is not the final scope.

The guarded exporter was rerun successfully and produced the same 103 ordered
artifacts. Reusing its output path failed as expected without overwriting it.
The analyzer comparison of baseline and expanded reports correctly marked absence
as non-comparable because `bodyIncludes` differs, rather than asserting removals.
`git diff --check` passed. Objective Guard alignment was not configured; it is not
claimed as a passing analysis or acceptance gate.

Utility and Preferences resolve from `build/ui-window`; Preference and Window
resolve from the explicitly selected UI-generated directory. The four absent
collaborators in the analyzer's initial diagnostic profile therefore were not
missing implementations in this fixture composition.

The expanded analysis finds a real binary API mismatch:

```text
Utility.getContext(ConnectionProvider, VariablesSecureApp, String, String)
  -> Window.isSalesTransaction(): Boolean [missing method]
```

Source inspection at `src/org/openbravo/erpCommon/utility/Utility.java:393` shows
this fallback requires the `IsSOTrx` context key. The calls in
`LoginUtils.fillSessionArguments` pass `#User_Client`, `#AccessibleOrgTree` and
`#Date`; they do not request that fallback. This is a latent generic-utility/domain
coupling candidate, not evidence that the tested login executes a failing call.
No application code was changed to suppress the finding.

Neither scope finds a witness to Product, Warehouse or BusinessPartner. Both
still exclude other bodies and retain reflection, invokedynamic and framework
resolution limitations. Completing these bounded graphs does not establish
whole-login or application completeness.

## Next extraction decision

Retain LoginSessionSupport as the explicit login domain contribution. Do not add
ERP fields to Window merely to silence this diagnostic. Review the `IsSOTrx`
fallback as a domain-context extension point while preserving Utility.getContext's
public signature and ERP behavior. Before implementing it, characterize both
ordinary context lookup and the ERP sales/purchase fallback, then widen the next
analysis block to the original session/navbar consumers. This report does not
authorize a production deletion or certify platform browser login.
