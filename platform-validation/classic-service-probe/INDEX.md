# Existing JSON service validation

Execute the original datasource and JSON services against Classic after the
read-only DAL bootstrap. Test credentials are supplied only through environment
variables and are not written to reports. This is an internal service gate;
the public HTTP adapter and its security checks remain separate.

`ClassicLoginValidation` compares original and extracted `LoginUtils` in separate
JVMs through `verifyClassicLogin`. It checks full/light sessions and denied scopes,
and compares hashes of all deterministic session strings. Private ignored reports
exclude CSRF tokens and raw preference values. Both runs use the owned Classic copy.
