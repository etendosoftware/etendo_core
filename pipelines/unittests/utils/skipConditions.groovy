// pipelines/unittests/utils/skipConditions.groovy

/**
 * Skips the pipeline if the branch is a version-style hotfix branch (e.g. hotfix/25.4.12).
 * Only Jira-style hotfix branches run tests: hotfix/#12-ETP-900, hotfix/ETP-212.
 * Sets currentBuild.result = 'ABORTED' and updates the GitHub commit status.
 * The caller must execute 'return' if this method returns true.
 *
 * @return true if pipeline should be skipped, false otherwise
 */
def skipIfVersionHotfix(String branchName, String repoName, String successStatus,
                        String accessToken, String gitCommit, String buildUrl, String contextBuild) {
    def versionHotfixPattern = /^hotfix\/\d+\.\d+(\.\d+)*$/
    if (branchName ==~ versionHotfixPattern) {
        echo "⏭️ Version hotfix branch detected (${branchName}). Skipping pipeline."
        sh "./pipelines/unittests/build-update.sh ${repoName} ${successStatus} \"Pipeline skipped - version hotfix branch\" ${accessToken} ${gitCommit} ${buildUrl} \"${contextBuild}\""
        currentBuild.result = 'ABORTED'
        return true
    }
    return false
}

/**
 * Skips the pipeline if all changed files are in the allowed list.
 * Sets currentBuild.result = 'ABORTED' and updates the GitHub commit status.
 * The caller must execute 'return' if this method returns true.
 *
 * @return true if pipeline should be skipped, false otherwise
 */
def skipIfOnlyAllowedFilesChanged(List changedFiles, List allowedFiles,
                                   String repoName, String successStatus, String skipMessage,
                                   String accessToken, String gitCommit, String buildUrl, String contextBuild) {
    def onlyAllowed = changedFiles.every { file -> allowedFiles.contains(file) }
    def hasAllowed  = changedFiles.any  { file -> allowedFiles.contains(file) }
    if (onlyAllowed && hasAllowed) {
        echo "⏭️ Only pipeline config files changed. Skipping pipeline execution."
        echo "📋 Changed files: ${changedFiles}"
        sh "./pipelines/unittests/build-update.sh ${repoName} ${successStatus} \"${skipMessage}\" ${accessToken} ${gitCommit} ${buildUrl} \"${contextBuild}\""
        echo "✅ Pipeline execution aborted successfully. Commit marked as success."
        currentBuild.result = 'ABORTED'
        return true
    }
    return false
}

return this
