// Utility to run test suites and publish reports, reducing Jenkinsfile size.

def runTestSuite(Map config) {
  def suiteName    = config.suiteName
  def testPattern  = config.testPattern ?: suiteName
  def execIndex    = config.execIndex
  def reportName   = config.reportName
  def stopCatalina = config.stopCatalina ?: false
  def stopGradle   = config.stopGradle ?: false
  def hasSpock     = config.hasSpockReport ?: false
  def spockReport  = config.spockReportName ?: ''
  def allowMissing = config.allowMissing != null ? config.allowMissing : true

  try {
    echo "--------------- Running ${suiteName} ---------------"
    sh "./gradlew test --tests \"${testPattern}\" --info"
    sh "mv build/jacoco/test.exec build/jacoco/test${execIndex}.exec"
    if (stopGradle) {
      sh "./gradlew --stop"
    }
    echo "--------------- ${suiteName} Successful ---------------"
    currentBuild.result = 'SUCCESS'
  } catch (Exception e) {
    env.ERROR_MESSAGE = "${suiteName} Failed"
    echo "--------------- ${env.ERROR_MESSAGE} ---------------"
    echo 'Exception occurred: ' + e.toString()
    currentBuild.result = 'UNSTABLE'
    unstable(env.ERROR_MESSAGE)
    env.STATUS_TEST = 'FAILED'

    def currentFailed = env.FAILED_SUITES ?: ""
    env.FAILED_SUITES = currentFailed.isEmpty() ? "${suiteName}" : "${currentFailed}, ${suiteName}"
  } finally {
    if (hasSpock && fileExists('build/spock-reports/')) {
      publishHTML([
        allowMissing: true,
        alwaysLinkToLastBuild: false,
        keepAll: true,
        reportDir: 'build/spock-reports/',
        reportFiles: '*.html',
        reportName: spockReport,
        reportTitles: ''
      ])
      sh "rm -rf build/spock-reports"
    }

    if (fileExists("build/reports/tests/test/")) {
      publishHTML([
        allowMissing: allowMissing,
        alwaysLinkToLastBuild: false,
        keepAll: true,
        reportDir: 'build/reports/tests/test',
        reportFiles: '*.html',
        reportName: reportName,
        reportTitles: ''
      ])
      sh "rm -rf build/reports/tests/test"
    } else {
      echo "Report directory of ${suiteName} does not exist. Skipping HTML report publishing."
    }

    if (stopCatalina) {
      sh "${WORKSPACE}/${env.TOMCAT_FOLDER}/bin/catalina.sh stop"
      sh "./gradlew --stop"
    }
    if (stopGradle && !stopCatalina) {
      // gradlew --stop already called in try block on success; ensure stop on failure too
    }
  }
}

return this