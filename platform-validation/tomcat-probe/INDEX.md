# Tomcat integration probe

Deploy the actual WAR in Apache Tomcat 10.1.59, embedded only as an isolated test
launcher. It uses a real Catalina webapp classloader, WAR expansion, lifecycle,
connector, and loopback HTTP port. No application classes are added to the server
classpath. HTTP checks run before and after stopping and redeploying the WAR.
The process and disposable PostgreSQL are stopped at the end of verifyTomcat.
