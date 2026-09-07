# WAR descriptor

WEB-INF/web.xml registers the DAL lifecycle listener and /requests servlet.
The Gradle war task packages it without secrets or integration probe classes.
