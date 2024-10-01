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

return this
