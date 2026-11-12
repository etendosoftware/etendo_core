// pipelines/sonarUtils.groovy

/**
 * GET on the SonarQube API without the token reaching the build log:
 * curl gets it through the environment and the shell trace is off.
 * @param sonarToken SonarQube API token
 * @param url Full API URL
 * @return Response body
 */
def sonarGet(sonarToken, url) {
  withEnv(["SONAR_API_TOKEN=${sonarToken}", "SONAR_API_URL=${url}"]) {
    return sh(
      script: '''set +x
curl -s -u "$SONAR_API_TOKEN:" "$SONAR_API_URL"''',
      returnStdout: true
    ).trim()
  }
}

/**
 * Waits for the SonarQube background task that processes the report the scanner uploaded.
 * @param ceTaskId Task id from .scannerwork/report-task.txt
 * @param sonarToken SonarQube API token
 * @param sonarServer SonarQube server URL
 * @throws error if the task fails, is canceled or does not finish in time
 */
def waitForCeTask(ceTaskId, sonarToken, sonarServer) {
  int pollSeconds = 20
  int maxPolls = 90
  for (int poll = 1; poll <= maxPolls; poll++) {
    def resp = sonarGet(sonarToken, "${sonarServer}/api/ce/task?id=${ceTaskId}")
    def task = null
    try {
      task = (readJSON(text: resp))?.task
    } catch (Exception e) {
      echo "Unreadable SonarQube task response: ${resp}"
    }
    def status = task?.status ?: 'UNKNOWN'
    echo "SonarQube task ${ceTaskId}: ${status} (${poll}/${maxPolls})"
    if (status == 'SUCCESS') {
      return
    }
    if (status == 'FAILED' || status == 'CANCELED') {
      error("SonarQube could not process the analysis (task ${ceTaskId} ${status}): ${task?.errorMessage ?: 'no error message'}")
    }
    sleep(time: pollSeconds, unit: 'SECONDS')
  }
  error("SonarQube task ${ceTaskId} did not finish after ${maxPolls * pollSeconds / 60} minutes")
}

def githubPrBase(repoName, branch, accessToken) {
  try {
    def resp = withEnv(["GH_API_TOKEN=${accessToken}", "GH_API_URL=https://api.github.com/repos/etendosoftware/${repoName}/pulls", "GH_HEAD=etendosoftware:${branch}"]) {
      sh(
        script: '''set +x
curl -s -G -H "Authorization: token $GH_API_TOKEN" -H "Accept: application/vnd.github+json" --data-urlencode "head=$GH_HEAD" --data-urlencode "state=open" "$GH_API_URL"''',
        returnStdout: true
      ).trim()
    }
    def pulls = readJSON(text: resp)
    if (pulls instanceof List && pulls.size() > 0) {
      echo "Open PR #${pulls[0].number} for '${branch}' targets '${pulls[0].base.ref}'"
      return pulls[0].base.ref
    }
    echo "No open PR found for '${branch}'"
  } catch (Exception e) {
    echo "Could not look up the PR for '${branch}': ${e.getMessage()}"
  }
  return null
}

def hasAnalyses(branch, sonarProjectKey, sonarToken, sonarServer) {
  try {
    def encodedBranch = URLEncoder.encode(branch, "UTF-8")
    def resp = sonarGet(sonarToken, "${sonarServer}/api/project_analyses/search?project=${sonarProjectKey}&branch=${encodedBranch}&ps=1")
    return ((readJSON(text: resp))?.analyses ?: []).size() > 0
  } catch (Exception e) {
    echo "Could not check analyses for '${branch}': ${e.getMessage()}"
    return false
  }
}

def resolveCompareBranch(repoName, accessToken, sonarProjectKey, sonarToken, sonarServer) {
  def branch = env.GIT_BRANCH
  def candidates = []
  def prBase = githubPrBase(repoName, branch, accessToken)
  if (prBase) {
    candidates << prBase
  }
  if (env.FROM_BACKPORT == 'true' && env.BACKPORT_BRANCH) {
    candidates << env.BACKPORT_BRANCH
  }
  if (env.FROM_PRERELEASE == 'true' && env.PRERELEASE_BRANCH) {
    candidates << env.PRERELEASE_BRANCH
  }
  if (branch.startsWith('feature/') && env.EPIC_BRANCH) {
    candidates << env.EPIC_BRANCH
  }
  if (branch.startsWith('feature/') || branch.startsWith('epic/')) {
    candidates << 'develop'
  }
  candidates << env.MAIN_BRANCH
  candidates = candidates.findAll { it && it != branch }.unique()
  echo "Coverage base candidates for '${branch}': ${candidates.join(', ')}"
  for (candidate in candidates) {
    if (hasAnalyses(candidate, sonarProjectKey, sonarToken, sonarServer)) {
      return candidate
    }
    echo "No SonarQube analyses for '${candidate}', trying the next candidate"
  }
  return env.MAIN_BRANCH
}

/**
 * Checks if this is the first analysis for a given branch
 * @param branch Branch name to check
 * @param sonarToken SonarQube API token
 * @param sonarServer SonarQube server URL
 * @return true if this is the first analysis, false otherwise
 */
def isFirstAnalysisForBranch(branch, sonarProjectKey, sonarToken, sonarServer) {
  try {
    def analysisResp = sonarGet(sonarToken, "${sonarServer}/api/project_analyses/search?project=${sonarProjectKey}&branch=${branch}&ps=1")
    
    echo "Checking if first analysis for branch '${branch}': ${analysisResp}"
    
    def analysisJson = readJSON text: analysisResp
    def analyses = analysisJson?.analyses ?: []
    
    if (analyses.size() == 0) {
      echo "No previous analyses found for branch '${branch}' - this is the first analysis"
      return true
    } else {
      echo "Found ${analyses.size()} previous analysis(es) for branch '${branch}' - not the first analysis"
      return false
    }
  } catch (Exception e) {
    echo "Error checking first analysis status for branch '${branch}': ${e.getMessage()}"
    // In case of error, we assume it's not the first analysis to be conservative
    return false
  }
}

/**
 * Utility function to retrieve SonarQube coverage for a branch, with retry logic and commit verification.
 * @param branch Branch name to query coverage for
 * @param checkCommit Whether to verify the latest analysis matches the current commit
 * @param sonarProjectKey SonarQube project key
 * @param sonarToken SonarQube API token
 * @param sonarServer SonarQube server URL
 * @param gitCommit Current commit SHA
 * @return Coverage value as float
 * @throws error if coverage cannot be retrieved after max retries
 */
def getCoverageWithRetry(branch, checkCommit, sonarProjectKey, sonarToken, sonarServer, gitCommit) {
  int maxRetries = 5
  float coverage = -1
  def encodedBranch = URLEncoder.encode(branch, "UTF-8")

  // Only check if it's the first analysis when we need a specific commit
  def isFirstAnalysis = false
  if (checkCommit) {
    isFirstAnalysis = isFirstAnalysisForBranch(encodedBranch, sonarProjectKey, sonarToken, sonarServer)
    
    if (isFirstAnalysis) {
      echo "🆕 This is the FIRST analysis for branch '${branch}' - waiting 10 minutes for SonarQube to process the analysis..."
      sleep(time: 10, unit: 'MINUTES')
      echo "⏰ 10-minute wait completed. Proceeding with coverage retrieval..."
    }
  } else {
    echo "📊 Getting most recent coverage for branch '${branch}' (no commit verification needed)"
  }
  
  for (int attempt = 0; attempt < maxRetries; attempt++) {
    echo "🔍 Attempt ${attempt + 1}/${maxRetries} - Retrieving analysis data for branch '${branch}'..."
    
    def analysisResp = sonarGet(sonarToken, "${sonarServer}/api/project_analyses/search?project=${sonarProjectKey}&branch=${encodedBranch}&ps=5")
    
    echo "📊 Analysis API response: ${analysisResp}"
    def analysisJson = readJSON text: analysisResp
    def analyses = analysisJson?.analyses ?: []
    
    if (analyses.size() == 0) {
      echo "❌ No analyses found for branch '${branch}' on attempt ${attempt + 1}"
      if (checkCommit && isFirstAnalysis) {
        echo "⏳ First analysis - waiting 2 minutes before retry..."
        sleep(time: 2, unit: 'MINUTES')
      } else {
        echo "⏳ Waiting 40 seconds before retry..."
        sleep(time: 40, unit: 'SECONDS')
      }
      continue
    }
    
    // Show information of all recent analyses for debugging
    echo "📋 Recent analyses for branch '${branch}':"
    analyses.eachWithIndex { analysis, index ->
      def analysisDate = analysis.date ?: 'N/A'
      def analysisRevision = analysis.revision ?: 'N/A'
      def analysisKey = analysis.key ?: 'N/A'
      echo "  ${index + 1}. Date: ${analysisDate}, Revision: ${analysisRevision}, Key: ${analysisKey}"
    }
    
    def targetAnalysis = null
    
    if (checkCommit && gitCommit) {
      // Search for specific analysis by commit
      targetAnalysis = analyses.find { it.revision == gitCommit }
      if (targetAnalysis) {
        echo "✅ Found analysis matching commit ${gitCommit}: ${targetAnalysis.key}"
      } else {
        echo "❌ No analysis found matching commit ${gitCommit}"
        echo "🔍 Available commits in recent analyses: ${analyses.collect { it.revision }.join(', ')}"
        
        if (isFirstAnalysis) {
          echo "⏳ First analysis - commit might still be processing. Waiting 2 minutes..."
          sleep(time: 2, unit: 'MINUTES')
        } else {
          echo "⏳ Waiting 40 seconds for analysis to process..."
          sleep(time: 40, unit: 'SECONDS')
        }
        continue
      }
    } else {
      // Use the most recent analysis
      targetAnalysis = analyses[0]
      echo "🎯 Using most recent analysis: ${targetAnalysis.key} (revision: ${targetAnalysis.revision})"
    }
    
    // Get coverage using the specific analysis
    def response
    
    if (checkCommit && targetAnalysis) {
      // Method 1: Try to get measures directly from the specific analysis using its key
      echo "📊 Getting coverage for specific analysis: ${targetAnalysis.key}"
      echo "🎯 Target analysis details: Date=${targetAnalysis.date}, Revision=${targetAnalysis.revision}"
      
      // First try: Get measures directly from analysis key (if API supports it)
      response = sonarGet(sonarToken, "${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${encodedBranch}&metricKeys=coverage")
      
      echo "📈 Component measures API response: ${response}"
      def json = readJSON text: response
      
      // Check if we got valid data from the specific analysis
      if (json.component && json.component.measures) {
        def measures = json.component.measures
        def coverageMeasure = measures.find { it.metric == 'coverage' }
        
        if (coverageMeasure) {
          def covStr = coverageMeasure.value
          echo "📊 Coverage from specific analysis ${targetAnalysis.key}: '${covStr}'"
          
          if (covStr && covStr != "0" && covStr != "0.0") {
            coverage = covStr.toFloat()
            echo "✅ Coverage successfully retrieved from specific analysis ${targetAnalysis.key} for commit ${gitCommit}: ${coverage}%"
            break
          } else {
            echo "⚠️ Coverage is 0 for specific analysis ${targetAnalysis.key}"
          }
        } else {
          echo "❌ No coverage metric found in specific analysis ${targetAnalysis.key}"
          echo "📊 Available metrics: ${measures.collect { it.metric }.join(', ')}"
        }
      } else {
        echo "❌ No component data found for specific analysis ${targetAnalysis.key}"
        echo "🔄 Falling back to branch-level measures..."
        
        // Fallback to normal branch API if analysis-specific fails
        response = sonarGet(sonarToken, "${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${encodedBranch}&metricKeys=coverage")
        echo "📈 Fallback branch measures API response: ${response}"
      }
    } else {
      // For most recent analysis, use normal API
      echo "📊 Getting coverage for most recent analysis"
      response = sonarGet(sonarToken, "${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${encodedBranch}&metricKeys=coverage")
    }
    
    echo "📈 Coverage API response for branch '${branch}': ${response}"
    def json = readJSON text: response
    
    // Verify if the response contains the expected component
    if (!json.component) {
      echo "❌ No component data found in coverage response"
      if (isFirstAnalysis) {
        echo "⏳ First analysis - component might still be processing. Waiting 2 minutes..."
        sleep(time: 2, unit: 'MINUTES')
      } else {
        echo "⏳ Waiting 40 seconds before retry..."
        sleep(time: 40, unit: 'SECONDS')
      }
      continue
    }
    
    def measures = json.component.measures ?: []
    def coverageMeasure = measures.find { it.metric == 'coverage' }
    
    if (!coverageMeasure) {
      echo "❌ No coverage metric found in measures"
      echo "📊 Available metrics: ${measures.collect { it.metric }.join(', ')}"
      if (isFirstAnalysis) {
        echo "⏳ First analysis - coverage metric might still be calculating. Waiting 2 minutes..."
        sleep(time: 2, unit: 'MINUTES')
      } else {
        echo "⏳ Waiting 40 seconds before retry..."
        sleep(time: 40, unit: 'SECONDS')
      }
      continue
    }
    
    def covStr = coverageMeasure.value
    echo "📊 Raw coverage value: '${covStr}' (type: ${covStr?.class?.simpleName})"
    
    if (covStr && covStr != "0" && covStr != "0.0") {
      coverage = covStr.toFloat()
      echo "✅ Coverage successfully retrieved for branch '${branch}': ${coverage}%"
      if (checkCommit) {
        echo "🎯 Coverage corresponds to commit: ${gitCommit}"
      }
      break
    } else {
      echo "⚠️ Coverage is 0 or empty for branch '${branch}' (attempt ${attempt + 1}/${maxRetries})"
      if (isFirstAnalysis) {
        echo "⏳ First analysis - waiting 2 minutes before retry..."
        sleep(time: 2, unit: 'MINUTES')
      } else {
        echo "⏳ Waiting 40 seconds before retry..."
        sleep(time: 40, unit: 'SECONDS')
      }
    }
  }
  if (coverage == -1) {
    if (!checkCommit) {
      // If it is the main branch and after the attempts the coverage is 0, return 0
      def response = sonarGet(sonarToken, "${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${encodedBranch}&metricKeys=coverage")
      def json = readJSON text: response
      def measures = json?.component?.measures ?: []
      def covStr = measures.find { it.metric == 'coverage' }?.value
      
      if (covStr == "0" || covStr == "0.0" || measures.size() == 0) {
        if (isFirstAnalysis) {
          echo "⚠️  First analysis for branch '${branch}' with no coverage data - this is expected for new branches"
        } else {
          echo "Final attempt: No coverage info for branch '${branch}', returning 0."
        }
        return 0.0
      }
      // If coverage is not 0 and measures exist, throw error as before
      error("Could not retrieve coverage for branch '${branch}' after ${maxRetries} attempts.")
    } else {
      if (isFirstAnalysis) {
        error("⚠️  Could not retrieve coverage for FIRST analysis of branch '${branch}' and commit '${gitCommit}' - this might be expected")
      }
      error("Could not retrieve coverage for branch '${branch}' and commit '${gitCommit}' after ${maxRetries} attempts.")
    }
  }
  
  return coverage
}

return this
