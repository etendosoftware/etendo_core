# Retained original UI runtime composition

Transitional local launcher consuming the owned UI instance prepared by
`prepareOriginalUiInstance`. Canonical authentication and DAL implementations run
on the isolated UI classpath, never the complete Classic runtime.

`RetainedUiServer` starts loopback Tomcat and owns its session/DAL lifecycle.
`UiComponents` composes canonical CDI beans and delegates rendering to the original
menu, navigation, layout and standard-window components. It contains no widgets.
The HTTP routes are integration plumbing, not a replacement frontend. Original
shell/window acceptance remains pending. The checkpoint includes experimental `PlatformShell`
and authenticated `/shell`, `/bootstrap.js` and `/types.js` routes; end-to-end
shell/window acceptance remains pending. See `../../docs/platform-resume.md`.
The launcher requires an explicit private instance configuration and does not
create or modify an external ERP database.

Start with JDK 17 using `./gradlew -p platform-validation runOriginalUi` from the
repository root. Port defaults to 8091; override with `-PplatformPort=PORT`.
The launcher checks the owned container label and matches its PostgreSQL binding
to the retained configuration before starting the application.

On the first start only, supply `PLATFORM_INITIAL_PASSWORD` through the local
environment to initialize the fixture's `admin` login. Never commit that value.
Later starts omit it; an already configured user cannot be overwritten this way.
POST form parameters `user` and `password` to `/platform/login`, then send the
returned cookie to GET `/platform/session`. There is no login page. The experimental
shell route is not a completed UI profile. The session response contains only the current scope identifiers.

Stop the foreground task, or identify the exact RetainedUiServer PID and send
SIGTERM to that process only. Keep the owned database container alive for Tomcat
restart validation. Existing cookies are intentionally invalid after restart;
the stored credentials remain available. Database session deactivation, logout,
expiry and shell/datasource routes are still pending; this is local integration, not a production
deployment or a completed UI profile.

`prepareOriginalUiWebResources` stages the original kernel/application/SmartClient/
selector web assets in `build/original-ui-public`. The read-only default servlet
serves that directory without directory listings. Source trees and private
configuration are outside its document root. `runOriginalUi` prepares these assets
automatically; neither headless build depends on this task. Static delivery alone
does not establish browser widget execution.

Authenticated GET routes `/platform/components/menu`, `/navigation`, `/layout`
(each under `/platform/components`) return the original generated JavaScript.
`/platform/components/window?windowId=ID` renders an original standard window
only when the canonical role-filtered MenuManager exposes it. Anonymous access is
401; unavailable windows are 403. Component generation activates a CDI request
scope and releases the original request/DAL thread context afterward.

Live validation rendered Request with its original OBStandardWindow, OBViewForm
and OBViewGrid, with JavaScript syntax checks for all four responses. An actual
R_EXCLUDE login received 403 for Request; the fixture user's temporary default
role change was restored to R1. This does not yet execute those widgets in a
browser or demonstrate UI CRUD. These limited integration routes are not a claim
of full KernelServlet protocol compatibility.
