

/**
  * Sends a parameterized email notification based on the build status.
  *
  * @param subject The subject of the email.
  * @param icon An icon representing the status (e.g., '❗' for unstable, '⛔' for failed).
  * @param status The status of the build ('Unstable' or 'Failed').
  * @param message Optional additional message to include in the email body.
  */

def sendParameterizedEmail(String subject, String icon, String status, String message = "") {
  def emailSubject = "${icon} ${subject} - ${currentBuild.fullDisplayName}"
  def timestamp = new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))
  
  def statusColors = [
    'Unstable': '#FFA500', // Orange
    'Failed':   '#FF0000'  // Red
  ]  
  def statusColor = statusColors[status] ?: '#333'
  
  def emailBody = """
    <html>
      <head>
          <style>
              body { font-family: 'Arial', sans-serif; }
              .header { font-size: 16px; font-weight: bold; color: ${statusColor}; }
              .footer { margin-top: 20px; }
          </style>
      </head>
      <body>
        <p><em>${timestamp}</em></p>
        <p>__________________________________________________________</p>

        <h2 class="header">${icon} ${status} ${icon}</h2>

        ${message ? "<p><strong>⇢ Reasons: <em>${message}</em></strong></p>" : ""}

        ${env.COMMIT_INFO ?: ''}

        ${env.STACK_INFO ?: ''}
        
        <p>Check out the output in the following link: ${env.BUILD_URL}</p>

        <p class="footer"><em>Best regards,<br>#EtendoBot 🤖</em></p>
        <p>__________________________________________________________</p>
      </body>
    </html>
  """
  
  mail(
    to: EMAIL_ADDRESS,
    subject: emailSubject,
    mimeType: "text/html",
    body: emailBody
  )
}

return this