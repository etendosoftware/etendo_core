<%@ page import="org.openbravo.base.session.OBPropertiesProvider"%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page session="false" %>
<%
  // Get the configured next-gen UI URL
  OBPropertiesProvider propsProvider = OBPropertiesProvider.getInstance();
  String nextgenUrl = propsProvider.getOpenbravoProperties().getProperty("ui.url", "http://localhost:3000");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>New UI Not Available - Etendo</title>
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }

    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }

    .container {
      background: white;
      border-radius: 12px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      max-width: 600px;
      width: 100%;
      padding: 40px;
      text-align: center;
    }

    .icon {
      width: 80px;
      height: 80px;
      margin: 0 auto 24px;
      background: #fee;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40px;
    }

    h1 {
      color: #dc2626;
      font-size: 28px;
      margin-bottom: 16px;
      font-weight: 600;
    }

    p {
      color: #4b5563;
      font-size: 16px;
      line-height: 1.6;
      margin-bottom: 12px;
    }

    .url-info {
      background: #f3f4f6;
      border-left: 4px solid #667eea;
      padding: 16px;
      margin: 24px 0;
      text-align: left;
      border-radius: 4px;
    }

    .url-info strong {
      color: #1f2937;
      display: block;
      margin-bottom: 8px;
    }

    .url-info code {
      background: #e5e7eb;
      padding: 2px 8px;
      border-radius: 4px;
      font-family: 'Courier New', monospace;
      color: #dc2626;
      font-size: 14px;
    }

    .actions {
      margin-top: 32px;
      padding-top: 24px;
      border-top: 1px solid #e5e7eb;
    }

    .actions h2 {
      color: #1f2937;
      font-size: 18px;
      margin-bottom: 16px;
      font-weight: 600;
    }

    .actions ol {
      text-align: left;
      padding-left: 20px;
      color: #4b5563;
    }

    .actions li {
      margin-bottom: 12px;
      line-height: 1.6;
    }

    .actions code {
      background: #f3f4f6;
      padding: 2px 6px;
      border-radius: 3px;
      font-family: 'Courier New', monospace;
      font-size: 13px;
      color: #dc2626;
    }

    .btn {
      display: inline-block;
      margin-top: 24px;
      padding: 12px 32px;
      background: #667eea;
      color: white;
      text-decoration: none;
      border-radius: 6px;
      font-weight: 500;
      transition: background 0.2s;
    }

    .btn:hover {
      background: #5568d3;
    }

    .footer {
      margin-top: 24px;
      padding-top: 24px;
      border-top: 1px solid #e5e7eb;
      color: #6b7280;
      font-size: 14px;
    }

    .dev-section {
      margin-top: 32px;
      padding-top: 24px;
      border-top: 1px solid #e5e7eb;
      background: #f9fafb;
      padding: 24px;
      border-radius: 8px;
      text-align: left;
    }

    .dev-section h2 {
      color: #1f2937;
      font-size: 18px;
      margin-bottom: 16px;
      font-weight: 600;
    }

    .dev-section h3 {
      color: #374151;
      font-size: 16px;
      margin-top: 20px;
      margin-bottom: 12px;
      font-weight: 600;
    }

    .dev-section p {
      margin-bottom: 16px;
    }

    .dev-section .command-box {
      background: #1f2937;
      color: #10b981;
      padding: 16px;
      border-radius: 6px;
      margin: 16px 0;
      font-family: 'Courier New', monospace;
      font-size: 14px;
      overflow-x: auto;
    }

    .dev-section .command-box .prompt {
      color: #6b7280;
      user-select: none;
    }

    .dev-section .command-box .command {
      color: #10b981;
    }

    .dev-section ul {
      padding-left: 20px;
      color: #4b5563;
    }

    .dev-section li {
      margin-bottom: 8px;
      line-height: 1.6;
    }

    .dev-section .note {
      background: #fef3c7;
      border-left: 4px solid #f59e0b;
      padding: 12px 16px;
      margin: 16px 0;
      border-radius: 4px;
    }

    .dev-section .note strong {
      color: #92400e;
      display: block;
      margin-bottom: 4px;
    }

    .dev-section .note p {
      color: #78350f;
      margin: 0;
      font-size: 14px;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="icon">⚠️</div>
    <h1>New UI Not Available</h1>
    <p>The system is configured to use the Next-Gen UI, but the server is not available at this time.</p>

    <div class="url-info">
      <strong>Configured URL:</strong>
      <code><%=nextgenUrl%></code>
    </div>

    <div class="actions">
      <h2>What can you do?</h2>
      <ol>
        <li>Verify that the New UI server is running at <code><%=nextgenUrl%></code></li>
        <li>If you need to start the server, run the appropriate command in your development environment</li>
        <li>If you prefer to use the Classic UI temporarily, change <code>ui.mode</code> to <code>classic</code> in the <code>Openbravo.properties</code> file</li>
      </ol>
    </div>

    <div class="dev-section">
      <h2>🛠️ For Developers</h2>
      <p>If you're developing on the platform and need to start the UI server:</p>

      <h3>Start UI Server</h3>
      <div class="command-box">
        <span class="prompt">$</span> <span class="command">./gradlew ui</span>
      </div>

      <p>This will start the development server on <code><%=nextgenUrl%></code></p>

      <h3>Stop Server</h3>
      <p>Press <strong>Ctrl+C</strong> in the terminal to stop the server.</p>

      <h3>Other Commands</h3>
      <div class="command-box">
        <span class="prompt">$</span> <span class="command">./gradlew uiInstall</span><br>
        <span style="color: #6b7280;"># Install dependencies</span>
      </div>
      <div class="command-box">
        <span class="prompt">$</span> <span class="command">./gradlew uiBuild</span><br>
        <span style="color: #6b7280;"># Build for production</span>
      </div>
    </div>

    <a href="../../index.jsp" class="btn">Retry</a>

    <div class="footer">
      If the problem persists, contact your system administrator
    </div>
  </div>
</body>
</html>