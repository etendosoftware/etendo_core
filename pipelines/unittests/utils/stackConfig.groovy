String getJavaVersion() {
  def javaVersion = sh(script: "echo ${JAVA_HOME} | awk -F'/' '{print \$NF}'", returnStdout: true).trim()
  return javaVersion
}

String getTomcatFolder() {
  def tomcatFolder = sh(script: "basename ${TOMCAT_URL} .tar.gz", returnStdout: true).trim()
  return tomcatFolder
}

String generateStackMessage() {
  def oracleVersion = env.ORCL_VERSION?.trim() ? env.ORCL_VERSION : null
  def postgresVersion = env.POSTGRES_VERSION?.trim() ? env.POSTGRES_VERSION : null
  def tomcatFolder = env.TOMCAT_URL?.trim() ? getTomcatFolder() : null
  def javaVersion = env.JAVA_HOME?.trim() ? getJavaVersion() : null

  def stackMessage = "<em>💡 The stack for this execution is:</em>\n"
  stackMessage += "<ul>\n"
  stackMessage += oracleVersion ? "<li><strong>Oracle:</strong> ${oracleVersion}</li>\n" : ''
  stackMessage += postgresVersion ? "<li><strong>Postgres:</strong> ${postgresVersion}</li>\n" : ''
  stackMessage += tomcatFolder ? "<li><strong>Tomcat:</strong> ${tomcatFolder}</li>\n" : ''
  stackMessage += javaVersion ? "<li><strong>Java:</strong> ${javaVersion}</li>\n" : ''
  stackMessage += "</ul>\n"

  return stackMessage
}

/**
 * Resolves the stack configuration based on the values of FROM_BACKPORT and FROM_PRERELEASE
 * @param fromBackport value of env.FROM_BACKPORT
 * @param fromPrerelease value of env.FROM_PRERELEASE
 * @return Map with stackType, tomcatUrl, javaHome
 */
Map resolveStackConfiguration(String fromBackport, String fromPrerelease) {
    def stackType = "DEFAULT"
    def tomcatUrl = env.TOMCAT_URL_DEFAULT
    def javaHome = env.JAVA_HOME_DEFAULT
    
    if (fromPrerelease == env.TRUE) {
        stackType = "PRERELEASE"
        tomcatUrl = env.TOMCAT_URL_PRERELEASE
        javaHome = env.JAVA_HOME_PRERELEASE
        echo "✅ Using PRERELEASE stack configuration"
    } else if (fromBackport == env.TRUE) {
        stackType = "BACKPORT"
        tomcatUrl = env.TOMCAT_URL_BACKPORT
        javaHome = env.JAVA_HOME_BACKPORT
        echo "✅ Using BACKPORT stack configuration"
    } else {
        echo "✅ Using DEFAULT stack configuration"
    }
    
    return [
        stackType: stackType,
        tomcatUrl: tomcatUrl,
        javaHome: javaHome
    ]
}

return this
