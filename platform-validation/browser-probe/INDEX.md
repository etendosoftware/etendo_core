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
field parity, role isolation and all ERP workflows are not implied by this test.
