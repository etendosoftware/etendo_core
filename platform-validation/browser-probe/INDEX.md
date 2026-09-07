# Browser acceptance

verify-platform.mjs exercises the optional UI in Chrome through Playwright against
the running fixture server. It reads owner-only fixture properties without logging
tokens, creates and updates a test-owned request, then checks read-only rejection.
Requires locally installed playwright and Chrome; no production database is used.

verify-erp-ui.mjs logs in through the original ERP page on the owned copy at 8093,
then calls the original Product datasource with that browser session. Credentials
come from PLATFORM_TEST_USERNAME and PLATFORM_TEST_PASSWORD, never from source.
It verifies selected fields and reference identifiers, ordering, inclusive-end
pagination and the original anonymous login redirect without Product data. Full
field parity and all ERP workflows are not implied by this test.
It also waits for the original MainView tab set and OBViewGrid class, and checks
an exact search-key filter. A loading screen with a working session is a failure.
The generated original user profile must also contain its current session role and
organization, and retain the selected ERP warehouse in that organization's options.

erp-scope-snapshot.mjs reads independent Product and organization-grant expectations
from the running labeled Classic copy using Docker/psql and a read-only transaction.
Set PLATFORM_COPY_CONTAINER to that container name. The browser probe compares
the default client and restricted session roles, then checks a fresh default login.

Set `PLATFORM_ERP_URL=http://127.0.0.1:8095/etendo/` to test a temporary ERP
deployment without interrupting 8093. Only loopback HTTP `/etendo/` URLs are
accepted; all login, datasource, role-switch and fresh-session requests use it.
