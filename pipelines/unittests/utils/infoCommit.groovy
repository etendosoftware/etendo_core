String getCommitDate() {
    def command = "git log -1 --date=format:'%d-%m-%Y %H:%M:%S' --pretty='%ad'"
    return sh(returnStdout: true, script: command).trim()
}

String getCommit() {
    def command = "git log -1 --format=format:'%H'"
    return sh(returnStdout: true, script: command).trim()
}

String getCommitAuthorName() {
    def command = "git log -1 --pretty=format:'%an'"
    return sh(returnStdout: true, script: command).trim()
}

String getCommitAuthorEmail() {
    def command = "git log -1 --pretty=format:'%ae'"
    return sh(returnStdout: true, script: command).trim()
}

String generateCommitInfo(String repoUrl) {
    def commit = getCommit()
    def commitDate = getCommitDate()
    def commitAuthorName = getCommitAuthorName()
    def commitAuthorEmail = getCommitAuthorEmail()

    def commitInfo = "<ul>\n" +
            "<li><strong>Last Commit:</strong> ${repoUrl}/commit/${commit}</li>\n" +
            "<li><strong>Author:</strong> ${commitAuthorName} (${commitAuthorEmail})</li>\n" +
            "<li><strong>Date:</strong> ${commitDate}\n" +
            "</ul>"

    return commitInfo
}

return this
