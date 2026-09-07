# WAR runtime

PlatformLifecycle initializes the real generated DAL from external properties and
closes its SessionFactory and WAR-owned JDBC drivers on undeploy. RequestServlet
uses fixture-scoped bearer tokens and real OBContext/OBDal transactions per request.
This authentication adapter is for validation, not production identity management.
GET lists requests, POST creates them and PUT updates an existing readable request.
Successful responses support JSON content negotiation alongside the legacy text
format; errors currently retain status codes and plain-text/empty bodies.
