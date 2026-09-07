# Browser acceptance

verify-platform.mjs exercises the optional UI in Chrome through Playwright against
the running fixture server. It reads owner-only fixture properties without logging
tokens, creates and updates a test-owned request, then checks read-only rejection.
Requires locally installed playwright and Chrome; no production database is used.
