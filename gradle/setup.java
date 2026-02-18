import com.sun.net.httpserver.*;
import java.net.http.*;
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * setup.java — Standalone GitHub auth UI before Gradle runs.
 *
 * Java 11+ single-file program (zero external dependencies).
 * Starts a mini HTTP server on :3850 with a browser UI for the
 * GitHub Device Flow, saves the token to gradle.properties,
 * then launches ./gradlew setup.web.
 *
 * Usage:  java setup.java [gradleTask]
 */
public class setup {

    static final String CLIENT_ID = "Ov23li1PuCseVXZZVH6O";
    static final String SCOPE = "read:packages";
    static final int PORT = 3850;
    static final Path PROPS_FILE = Path.of("gradle.properties");

    // Shared state between HTTP handlers and the polling thread
    static final AtomicReference<String> authStatus = new AtomicReference<>("pending");
    static final AtomicReference<String> errorMessage = new AtomicReference<>("");
    static volatile String userCode = "";
    static volatile String deviceCode = "";
    static volatile int pollInterval = 5;
    static volatile int expiresIn = 900;

    public static void main(String[] args) throws Exception {

        // 1. Start Device Flow
        System.out.println();
        System.out.println("githubToken is not set. Starting GitHub authentication...");
        System.out.println();

        if (!startDeviceFlow()) {
            System.exit(1);
        }

        // 2. Start HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", setup::handleIndex);
        server.createContext("/api/status", setup::handleStatus);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Auth UI running at http://localhost:" + PORT);

        // 3. Open browser
        openBrowser("http://localhost:" + PORT);

        // 4. Background thread polls GitHub until token received
        CompletableFuture<String> tokenFuture = CompletableFuture.supplyAsync(() -> {
            long deadline = System.currentTimeMillis() + (expiresIn * 1000L);
            while (System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(pollInterval * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                try {
                    String result = pollForToken();
                    if (result != null) return result;
                } catch (Exception e) {
                    authStatus.set("error");
                    errorMessage.set(e.getMessage());
                    return null;
                }
            }
            authStatus.set("error");
            errorMessage.set("Authorization timed out. Please run again.");
            return null;
        });

        String token = tokenFuture.get();
        // Give browser time to pick up the success status
        Thread.sleep(2000);
        server.stop(0);

        if (token == null || token.isEmpty()) {
            System.err.println("ERROR: " + errorMessage.get());
            System.exit(1);
        }

        // 5. Save token — setup.sh will launch Gradle
        saveToken(token);
        String masked = token.substring(0, 4) + "..." + token.substring(token.length() - 4);
        System.out.println("Token saved to " + PROPS_FILE + " (" + masked + ")");
        System.out.println();
        // Exit 0 — caller (setup.sh / setup.bat) continues to run gradlew
    }

    // ---- Device Flow ----

    static boolean startDeviceFlow() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/login/device/code"))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "client_id=" + CLIENT_ID + "&scope=" + SCOPE))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        String body = resp.body();

        userCode = jsonValue(body, "user_code");
        deviceCode = jsonValue(body, "device_code");
        String intervalStr = jsonValue(body, "interval");
        String expiresStr = jsonValue(body, "expires_in");

        if (userCode.isEmpty() || deviceCode.isEmpty()) {
            System.err.println("Error: Failed to start GitHub Device Flow.");
            System.err.println("Response: " + body);
            return false;
        }

        if (!intervalStr.isEmpty()) pollInterval = Integer.parseInt(intervalStr);
        if (!expiresStr.isEmpty()) expiresIn = Integer.parseInt(expiresStr);

        System.out.println("Your code: " + userCode);
        return true;
    }

    static String pollForToken() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/login/oauth/access_token"))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "client_id=" + CLIENT_ID
                                + "&device_code=" + deviceCode
                                + "&grant_type=urn:ietf:params:oauth:grant-type:device_code"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        String body = resp.body();

        String accessToken = jsonValue(body, "access_token");
        if (!accessToken.isEmpty()) {
            authStatus.set("success");
            return accessToken;
        }

        String error = jsonValue(body, "error");
        switch (error) {
            case "authorization_pending":
                break;
            case "slow_down":
                pollInterval += 5;
                break;
            case "expired_token":
                authStatus.set("error");
                errorMessage.set("Authorization expired. Please run again.");
                throw new RuntimeException(errorMessage.get());
            case "access_denied":
                authStatus.set("error");
                errorMessage.set("Authorization denied by user.");
                throw new RuntimeException(errorMessage.get());
            default:
                if (!error.isEmpty()) {
                    authStatus.set("error");
                    errorMessage.set(error);
                    throw new RuntimeException(error);
                }
                break;
        }
        return null;
    }

    // ---- HTTP Handlers ----

    static void handleIndex(HttpExchange ex) throws IOException {
        String html = INDEX_HTML.replace("{{USER_CODE}}", escapeHtml(userCode));
        byte[] bytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static void handleStatus(HttpExchange ex) throws IOException {
        String status = authStatus.get();
        String json;
        if ("error".equals(status)) {
            json = "{\"status\":\"error\",\"message\":\"" + escapeJson(errorMessage.get()) + "\"}";
        } else {
            json = "{\"status\":\"" + status + "\"}";
        }
        byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ---- Token persistence ----

    static String getExistingToken() {
        try {
            if (!Files.exists(PROPS_FILE)) return null;
            for (String line : Files.readAllLines(PROPS_FILE)) {
                if (line.startsWith("githubToken=")) {
                    String val = line.substring("githubToken=".length()).trim();
                    return val.isEmpty() ? null : val;
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return null;
    }

    static void saveToken(String token) throws IOException {
        if (!Files.exists(PROPS_FILE)) {
            Files.writeString(PROPS_FILE, "githubToken=" + token + "\n");
            return;
        }
        List<String> lines = Files.readAllLines(PROPS_FILE);
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("githubToken=")) {
                lines.set(i, "githubToken=" + token);
                found = true;
                break;
            }
        }
        if (!found) {
            lines.add("githubToken=" + token);
        }
        Files.write(PROPS_FILE, lines);
    }

    // ---- Browser ----

    static void openBrowser(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("mac")) Runtime.getRuntime().exec(new String[]{"open", url});
            else if (os.contains("win")) Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            else Runtime.getRuntime().exec(new String[]{"xdg-open", url});
        } catch (Exception ignored) {}
    }

    // ---- Minimal JSON helpers (no dependencies) ----

    static String jsonValue(String json, String key) {
        // Handles both "key":"stringVal" and "key":numberVal
        String strPattern = "\"" + key + "\":\"";
        int idx = json.indexOf(strPattern);
        if (idx >= 0) {
            int start = idx + strPattern.length();
            int end = json.indexOf('"', start);
            return end > start ? json.substring(start, end) : "";
        }
        String numPattern = "\"" + key + "\":";
        idx = json.indexOf(numPattern);
        if (idx >= 0) {
            int start = idx + numPattern.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
                end++;
            }
            return end > start ? json.substring(start, end) : "";
        }
        return "";
    }

    static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // ---- HTML ----

    static final String INDEX_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Etendo Setup</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  :root {
    --surface: #ffffff;
    --text-primary: #0b0c10;
    --text-secondary: #475467;
    --border-color: #e4e7ec;
    --background-main: #f5f6fa;
    --primary-color: #004aca;
    --primary-dark: #003494;
    --primary-light: #e3f2fd;
    --success: #4caf50;
    --error: #f44336;
    --sidebar-width: 260px;
  }
  body {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    background: var(--background-main);
    color: var(--text-primary);
    margin: 0;
  }
  .app-layout {
    display: flex;
    min-height: 100vh;
  }
  /* ---- Sidebar ---- */
  .sidebar {
    width: var(--sidebar-width);
    min-width: var(--sidebar-width);
    height: 100vh;
    position: fixed;
    left: 0; top: 0;
    background: var(--surface);
    border-right: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    z-index: 100;
  }
  .sidebar-header { padding: 24px 20px 16px; }
  .sidebar-title { font-size: 16px; font-weight: 700; color: var(--text-primary); letter-spacing: -0.01em; }
  .sidebar-subtitle { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
  .sidebar-divider { height: 1px; background: var(--border-color); margin: 0 0 8px; }
  .sidebar-nav { padding: 0 8px; flex: 1; }
  .nav-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border-radius: 8px;
    margin-bottom: 2px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-secondary);
    cursor: default;
    user-select: none;
  }
  .nav-item.active {
    background: var(--primary-color);
    color: #ffffff;
  }
  .nav-item.active .nav-icon svg { stroke: #ffffff; }
  .nav-item-label { display: flex; flex-direction: column; gap: 1px; }
  .nav-item-desc { font-size: 11px; font-weight: 400; opacity: 0.75; }
  .nav-icon { width: 20px; height: 20px; flex-shrink: 0; }
  .sidebar-footer {
    padding: 16px 20px;
    border-top: 1px solid var(--border-color);
    font-size: 12px;
    color: var(--text-secondary);
  }
  /* ---- Main content ---- */
  .main-content {
    margin-left: var(--sidebar-width);
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    padding: 32px;
  }
  /* ---- Auth card (mirrors .status-card) ---- */
  .auth-card {
    border: 1px solid var(--border-color);
    border-radius: 16px;
    padding: 32px;
    background: var(--surface);
    box-shadow: 0 18px 48px rgba(15,23,42,0.08);
    max-width: 540px;
  }
  .section-title {
    font-size: 22px;
    font-weight: 700;
    color: var(--text-primary);
    letter-spacing: -0.02em;
    margin-bottom: 6px;
  }
  .section-subtitle {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.5;
    margin-bottom: 24px;
  }
  .card-divider { height: 1px; background: var(--border-color); margin: 0 0 24px; }
  .step-row {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    margin-bottom: 20px;
  }
  .step-num {
    width: 24px; height: 24px;
    border-radius: 50%;
    background: var(--primary-light);
    color: var(--primary-color);
    font-size: 12px;
    font-weight: 700;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0;
    margin-top: 2px;
  }
  .step-body { flex: 1; }
  .step-label { font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
  .url-link {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: var(--primary-color);
    font-size: 13px;
    font-weight: 500;
    text-decoration: none;
    padding: 4px 10px;
    border: 1px solid var(--border-color);
    border-radius: 6px;
    background: var(--background-main);
    transition: background 0.15s;
  }
  .url-link:hover { background: var(--primary-light); border-color: var(--primary-color); }
  .code-box {
    background: var(--background-main);
    border: 2px solid var(--primary-color);
    border-radius: 8px;
    padding: 16px 20px;
    font-family: 'SF Mono', 'Fira Code', Menlo, Consolas, monospace;
    font-size: 28px;
    font-weight: 700;
    letter-spacing: 6px;
    color: var(--primary-color);
    user-select: all;
    display: inline-block;
    margin-bottom: 6px;
  }
  .copied {
    font-size: 12px;
    color: var(--success);
    height: 16px;
    opacity: 0;
    transition: opacity 0.2s;
    margin-bottom: 16px;
  }
  .copied.show { opacity: 1; }
  .btn-row { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 24px; }
  .btn {
    padding: 8px 18px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 600;
    font-family: inherit;
    cursor: pointer;
    text-decoration: none;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    transition: background 0.15s, box-shadow 0.15s;
    border: 1px solid var(--border-color);
    outline: none;
    line-height: 1.4;
  }
  .btn-outlined {
    background: var(--surface);
    color: var(--text-primary);
  }
  .btn-outlined:hover { background: var(--background-main); }
  .btn-contained {
    background: var(--primary-color);
    color: #ffffff;
    border-color: var(--primary-color);
    box-shadow: 0 1px 4px rgba(0,74,202,0.2);
  }
  .btn-contained:hover { background: var(--primary-dark); }
  .status-bar {
    font-size: 13px;
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: var(--background-main);
    border: 1px solid var(--border-color);
    border-radius: 8px;
  }
  .spinner {
    width: 14px; height: 14px;
    border: 2px solid var(--border-color);
    border-top-color: var(--primary-color);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
    flex-shrink: 0;
  }
  @keyframes spin { to { transform: rotate(360deg); } }
  .success { color: var(--success); font-weight: 600; }
  .error { color: var(--error); font-weight: 600; }
</style>
</head>
<body>
<div class="app-layout">

  <!-- Sidebar — mirrors EtendoTool layout -->
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="sidebar-title">ETENDO TOOL</div>
      <div class="sidebar-subtitle">Control Center</div>
    </div>
    <div class="sidebar-divider"></div>
    <nav class="sidebar-nav">
      <div class="nav-item active">
        <span class="nav-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="3"/><path d="M19.07 4.93a10 10 0 010 14.14M4.93 4.93a10 10 0 000 14.14"/>
            <path d="M15.54 8.46a5 5 0 010 7.07M8.46 8.46a5 5 0 000 7.07"/>
          </svg>
        </span>
        <span class="nav-item-label">
          <span>Setup</span>
          <span class="nav-item-desc">GitHub authentication</span>
        </span>
      </div>
    </nav>
    <div class="sidebar-footer">Etendo Development Kit</div>
  </aside>

  <!-- Main content -->
  <main class="main-content">
    <div class="auth-card">
      <div class="section-title">GitHub Authentication</div>
      <div class="section-subtitle">
        A GitHub token is required to download Etendo packages from GitHub Packages.<br>
        Complete the two steps below to authorize.
      </div>
      <div class="card-divider"></div>

      <div class="step-row">
        <div class="step-num">1</div>
        <div class="step-body">
          <div class="step-label">Open GitHub device activation</div>
          <a class="url-link" href="https://github.com/login/device" target="_blank" rel="noopener">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 01-2 2H5a2 2 0 01-2-2V8a2 2 0 012-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
            github.com/login/device
          </a>
        </div>
      </div>

      <div class="step-row">
        <div class="step-num">2</div>
        <div class="step-body">
          <div class="step-label">Enter this code on GitHub</div>
          <div class="code-box" id="code">{{USER_CODE}}</div><br>
          <div class="copied" id="copied-msg">&#10003; Copied to clipboard</div>
          <div class="btn-row">
            <button class="btn btn-outlined" onclick="copyCode()">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>
              Copy code
            </button>
            <a class="btn btn-contained" href="https://github.com/login/device" target="_blank" rel="noopener">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 01-2 2H5a2 2 0 01-2-2V8a2 2 0 012-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
              Open GitHub
            </a>
          </div>
        </div>
      </div>

      <div class="status-bar" id="status">
        <span class="spinner"></span>
        <span>Waiting for authorization...</span>
      </div>
    </div>
  </main>

</div>

<script>
function copyCode() {
  var code = document.getElementById('code').textContent.trim();
  navigator.clipboard.writeText(code).then(function() {
    var msg = document.getElementById('copied-msg');
    msg.classList.add('show');
    setTimeout(function() { msg.classList.remove('show'); }, 2000);
  });
}

function poll() {
  fetch('/api/status')
    .then(function(r) { return r.json(); })
    .then(function(data) {
      var el = document.getElementById('status');
      if (data.status === 'success') {
        el.innerHTML = '<span class="success">&#10003; Token saved — launching Etendo setup...</span>';
      } else if (data.status === 'error') {
        el.innerHTML = '<span class="error">&#x26A0; ' + (data.message || 'Unknown error') + '</span>';
      } else {
        setTimeout(poll, 3000);
      }
    })
    .catch(function() { setTimeout(poll, 3000); });
}
setTimeout(poll, 3000);
</script>
</body>
</html>
""";
}
