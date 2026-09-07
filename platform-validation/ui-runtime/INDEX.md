# Retained original UI runtime composition

Transitional local launcher consuming the owned UI instance prepared by
`prepareOriginalUiInstance`. Canonical authentication and DAL implementations run
on the isolated UI classpath, never the complete Classic runtime.

`RetainedUiServer` starts loopback Tomcat and owns its session/DAL lifecycle.
The HTTP routes are integration plumbing, not a replacement frontend. Original
shell/window delivery remains pending. The launcher requires an explicit private
instance configuration and does not create or modify an external ERP database.

Start with JDK 17 using `./gradlew -p platform-validation runOriginalUi` from the
repository root. Port defaults to 8091; override with `-PplatformPort=PORT`.
The launcher checks the owned container label and matches its PostgreSQL binding
to the retained configuration before starting the application.

On the first start only, supply `PLATFORM_INITIAL_PASSWORD` through the local
environment to initialize the fixture's `admin` login. Never commit that value.
Later starts omit it; an already configured user cannot be overwritten this way.
POST form parameters `user` and `password` to `/platform/login`, then send the
returned cookie to GET `/platform/session`. There is no login page or original
shell route yet. The session response contains only the current scope identifiers.

Stop the foreground task, or identify the exact RetainedUiServer PID and send
SIGTERM to that process only. Keep the owned database container alive for Tomcat
restart validation. Existing cookies are intentionally invalid after restart;
the stored credentials remain available. Database session deactivation, logout,
expiry and UI routes are still pending; this is local integration, not a production
deployment or a completed UI profile.
