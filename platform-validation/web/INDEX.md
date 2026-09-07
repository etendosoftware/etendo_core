# WAR runtime

PlatformLifecycle initializes the real generated DAL from external properties and
closes its SessionFactory and WAR-owned JDBC drivers on undeploy. RequestServlet
uses fixture-scoped bearer tokens and real OBContext/OBDal transactions per request.
This authentication adapter is for validation, not production identity management.
