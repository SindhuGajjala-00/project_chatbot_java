// import com.sun.net.httpserver.HttpServer;
// import com.sun.net.httpserver.HttpExchange;
// import com.sun.net.httpserver.Headers;

// import java.io.*;
// import java.net.*;
// import java.nio.charset.StandardCharsets;
// import java.util.*;

// public class ChatServer {

//     public static void main(String[] args) throws Exception {
//         HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

//         // Serve frontend files
//         server.createContext("/", exchange -> serveFile(exchange, "static/index.html"));
//         server.createContext("/static", exchange -> {
//             String file = exchange.getRequestURI().getPath().replace("/static/", "");
//             serveFile(exchange, "static/" + file);
//         });

//         // Chat endpoint
//         server.createContext("/chat", exchange -> {
//             if ("POST".equals(exchange.getRequestMethod())) {
//                 String requestBody = readBody(exchange);

//                 String userQuery = requestBody.trim();
//                 String answer = callTavilySearch(userQuery);

//                 sendResponse(exchange, 200, answer);
//             } else {
//                 sendResponse(exchange, 405, "Method Not Allowed");
//             }
//         });

//         System.out.println("Server running at http://localhost:8000");
//         server.start();
//     }

//     // Reads request body fully
//     private static String readBody(HttpExchange exchange) throws IOException {
//         InputStream in = exchange.getRequestBody();
//         ByteArrayOutputStream out = new ByteArrayOutputStream();
//         byte[] buf = new byte[1024];
//         int read;
//         while ((read = in.read(buf)) != -1) {
//             out.write(buf, 0, read);
//         }
//         return new String(out.toByteArray(), StandardCharsets.UTF_8);
//     }

//     // Serves static files
//     private static void serveFile(HttpExchange exchange, String filePath) throws IOException {
//         File file = new File(filePath);
//         if (!file.exists()) {
//             sendResponse(exchange, 404, "404 Not Found");
//             return;
//         }

//         byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
//         exchange.getResponseHeaders().add("Content-Type", getMimeType(filePath));
//         exchange.sendResponseHeaders(200, bytes.length);
//         OutputStream os = exchange.getResponseBody();
//         os.write(bytes);
//         os.close();
//     }

//     // Call Tavily API
//    private static String callTavilySearch(String query) {
//     try {
//         URL url = new URL("https://api.tavily.com/search");
//         HttpURLConnection conn = (HttpURLConnection) url.openConnection();

//         conn.setRequestMethod("POST");
//         conn.setRequestProperty("Content-Type", "application/json");
//         conn.setRequestProperty("Authorization", "Bearer " + ApiConfig.TAVILY_API_KEY);
//         conn.setDoOutput(true);

//         String body = "{\"query\":\"" + query + "\"}";
//         conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

//         BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//         StringBuilder json = new StringBuilder();
//         String line;
//         while ((line = reader.readLine()) != null) json.append(line);
//         reader.close();

//         String data = json.toString();

//         // Extract all "content" fields
//         List<String> contents = new ArrayList<>();
//         String marker = "\"content\":\"";
//         int start = 0;

//         while ((start = data.indexOf(marker, start)) != -1) {
//             start += marker.length();
//             int end = data.indexOf("\",", start);
//             if (end == -1) break;

//             String content = data.substring(start, end)
//                                  .replace("\\n", " ")
//                                  .replace("\\\"", "\"");

//             // Skip YouTube, product descriptions, irrelevant junk
//             if (!content.toLowerCase().contains("subscribe") &&
//                 !content.toLowerCase().contains("youtube") &&
//                 content.length() > 40) {
//                 contents.add(content);
//             }
//             start = end;
//         }

//         if (contents.isEmpty())
//             return "I couldn’t find a clear answer. Try asking differently!";

//         // Find best content that matches query words
//         String[] keywords = query.toLowerCase().split(" ");
//         String best = "";
//         int bestScore = -1;

//         for (String c : contents) {
//             int score = 0;
//             for (String k : keywords) {
//                 if (c.toLowerCase().contains(k)) score++;
//             }
//             if (score > bestScore) {
//                 bestScore = score;
//                 best = c;
//             }
//         }

//         // Light summary: first 4 lines or 400 chars
//         if (best.length() > 400) best = best.substring(0, 400) + "...";

//         return best;

//     } catch (Exception e) {
//         return "Hmm, I couldn't fetch information right now.";
//     }
// }

//     private static void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
//         byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
//         exchange.getResponseHeaders().add("Content-Type", "application/json");
//         exchange.sendResponseHeaders(status, bytes.length);

//         OutputStream os = exchange.getResponseBody();
//         os.write(bytes);
//         os.close();
//     }

//     private static String getMimeType(String path) {
//         if (path.endsWith(".html")) return "text/html";
//         if (path.endsWith(".css")) return "text/css";
//         if (path.endsWith(".js")) return "text/javascript";
//         return "text/plain";
//     }
// }
// ChatServer.java
// import com.sun.net.httpserver.HttpServer;
// import com.sun.net.httpserver.HttpExchange;

// import java.io.*;
// import java.net.*;
// import java.nio.charset.StandardCharsets;
// import java.nio.file.Files;
// import java.util.ArrayList;
// import java.util.List;

// public class ChatServer {

//     public static void main(String[] args) throws Exception {
//         int port = 8000;
//         HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

//         // static file handler: serves files from ./static
//         server.createContext("/", exchange -> {
//             String path = exchange.getRequestURI().getPath();
//             if ("/".equals(path)) path = "/index.html";
//             File file = new File("static" + path).getCanonicalFile();

//             // Simple security: deny files outside static/
//             File staticDir = new File("static").getCanonicalFile();
//             if (!file.getPath().startsWith(staticDir.getPath()) || !file.exists()) {
//                 String msg = "404 Not Found";
//                 sendPlain(exchange, 404, msg);
//                 return;
//             }
//             byte[] bytes = Files.readAllBytes(file.toPath());
//             String contentType = guessContentType(file.getName());
//             exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
//             exchange.sendResponseHeaders(200, bytes.length);
//             try (OutputStream os = exchange.getResponseBody()) {
//                 os.write(bytes);
//             }
//         });

//         // chat API: POST /chat
//         server.createContext("/chat", exchange -> {
//             if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
//                 sendPlain(exchange, 405, "Method Not Allowed");
//                 return;
//             }

//             // read full body (works for Java 8+)
//             String body = readAll(exchange.getRequestBody());
//             // The client sends plain text message as body (see frontend)
//             String userMessage = body == null ? "" : body.trim();

//             // Call Tavily
//             String answer = callTavilySmart(userMessage);

//             // Return JSON with answer and html snippet
//             String json = "{\"answer\": " + jsonString(answer) + "}";
//             exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
//             byte[] out = json.getBytes(StandardCharsets.UTF_8);
//             exchange.sendResponseHeaders(200, out.length);
//             try (OutputStream os = exchange.getResponseBody()) {
//                 os.write(out);
//             }
//         });

//         server.setExecutor(null);
//         server.start();
//         System.out.println("Server running at http://localhost:" + port);
//     }

//     // ---------------- helper methods ----------------

//     private static void sendPlain(HttpExchange exchange, int status, String message) throws IOException {
//         byte[] out = message.getBytes(StandardCharsets.UTF_8);
//         exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
//         exchange.sendResponseHeaders(status, out.length);
//         try (OutputStream os = exchange.getResponseBody()) {
//             os.write(out);
//         }
//     }

//     private static String readAll(InputStream is) throws IOException {
//         ByteArrayOutputStream baos = new ByteArrayOutputStream();
//         byte[] buf = new byte[2048];
//         int r;
//         while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
//         return new String(baos.toByteArray(), StandardCharsets.UTF_8);
//     }

//     private static String guessContentType(String name) {
//         String n = name.toLowerCase();
//         if (n.endsWith(".html")) return "text/html";
//         if (n.endsWith(".css")) return "text/css";
//         if (n.endsWith(".js")) return "application/javascript";
//         if (n.endsWith(".png")) return "image/png";
//         if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
//         return "text/plain";
//     }

//     // ---------------- Tavily call + smarter extraction ----------------
//     private static String callTavilySmart(String query) {
//         if (query == null || query.isEmpty()) return "Please ask a question.";

//         try {
//             URL url = new URL(ApiConfig.TAVILY_API_URL);
//             HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//             conn.setRequestMethod("POST");
//             conn.setConnectTimeout(15000);
//             conn.setReadTimeout(30000);
//             conn.setRequestProperty("Content-Type", "application/json");
//             conn.setRequestProperty("Authorization", "Bearer " + ApiConfig.TAVILY_API_KEY);
//             conn.setDoOutput(true);

//             // send minimal JSON: { "query": "..." }
//             String payload = "{\"query\":" + jsonString(query) + "}";
//             try (OutputStream os = conn.getOutputStream()) {
//                 os.write(payload.getBytes(StandardCharsets.UTF_8));
//             }

//             int status = conn.getResponseCode();
//             InputStream in = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
//             String response = readAll(in);

//             // crude but effective extraction:
//             // 1) try top-level "answer" field
//             String answer = extractJsonField(response, "answer");
//             if (isUsable(answer)) return tidy(answer);

//             // 2) look for results[].content with best match (skip youtube)
//             List<String> contents = extractAllJsonFields(response, "content");
//             List<String> candidates = new ArrayList<>();
//             for (String c : contents) {
//                 if (c == null) continue;
//                 String low = c.toLowerCase();
//                 if (low.contains("youtube") || low.contains("subscribe") || low.contains("watch on")) continue;
//                 if (c.trim().length() < 30) continue;
//                 candidates.add(c);
//             }
//             if (!candidates.isEmpty()) {
//                 // pick the candidate with most keyword matches
//                 String[] keywords = query.toLowerCase().split("\\s+");
//                 String best = candidates.get(0);
//                 int bestScore = -1;
//                 for (String cand : candidates) {
//                     int score = 0;
//                     String low = cand.toLowerCase();
//                     for (String k : keywords) if (k.length()>1 && low.contains(k)) score++;
//                     if (score > bestScore) { bestScore = score; best = cand; }
//                 }
//                 return tidy(best);
//             }

//             // 3) fallback: try "results" snippet or title
//             String snippet = extractJsonField(response, "snippet");
//             if (isUsable(snippet)) return tidy(snippet);

//             // 4) final fallback: return raw trimmed JSON (not ideal)
//             return "I found results but couldn't format them nicely. Raw response:\n" + truncate(response, 1000);

//         } catch (Exception e) {
//             e.printStackTrace();
//             return "Error contacting search API: " + e.getMessage();
//         }
//     }

//     // small helpers -------------------------------------------------

//     private static boolean isUsable(String s) {
//         return s != null && s.trim().length() > 20;
//     }

//     private static String tidy(String s) {
//         if (s == null) return "";
//         // unescape common sequences
//         String t = s.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"").replace("\\t", "\t");
//         // remove HTML tags if present
//         t = t.replaceAll("<[^>]+>", "");
//         // trim and shorten
//         t = t.trim();
//         if (t.length() > 800) t = t.substring(0, 800) + "...";
//         return t;
//     }

//     private static String truncate(String s, int max) {
//         if (s == null) return "";
//         return s.length() <= max ? s : s.substring(0, max - 3) + "...";
//     }

//     // extract first occurrence of "field": "value" (handles escaped quotes crudely)
//     private static String extractJsonField(String json, String field) {
//         if (json == null) return null;
//         String key = "\"" + field + "\"";
//         int idx = json.indexOf(key);
//         if (idx == -1) return null;
//         int colon = json.indexOf(":", idx + key.length());
//         if (colon == -1) return null;
//         int valStart = json.indexOf("\"", colon);
//         if (valStart == -1) return null;
//         int valEnd = findClosingQuote(json, valStart + 1);
//         if (valEnd == -1) return null;
//         return json.substring(valStart + 1, valEnd);
//     }

//     // extract all occurrences of "field": "value"
//     private static List<String> extractAllJsonFields(String json, String field) {
//         List<String> out = new ArrayList<>();
//         if (json == null) return out;
//         String key = "\"" + field + "\"";
//         int pos = 0;
//         while (true) {
//             int idx = json.indexOf(key, pos);
//             if (idx == -1) break;
//             int colon = json.indexOf(":", idx + key.length());
//             if (colon == -1) break;
//             int valStart = json.indexOf("\"", colon);
//             if (valStart == -1) break;
//             int valEnd = findClosingQuote(json, valStart + 1);
//             if (valEnd == -1) break;
//             String val = json.substring(valStart + 1, valEnd);
//             out.add(val);
//             pos = valEnd + 1;
//         }
//         return out;
//     }

//     // find closing unescaped quote index
//     private static int findClosingQuote(String s, int start) {
//         for (int i = start; i < s.length(); i++) {
//             if (s.charAt(i) == '"') {
//                 int back = i - 1;
//                 int bs = 0;
//                 while (back >= 0 && s.charAt(back) == '\\') { bs++; back--; }
//                 if (bs % 2 == 0) return i;
//             }
//         }
//         return -1;
//     }

//     private static String jsonString(String s) {
//         if (s == null) return "\"\"";
//         String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
//         return "\"" + escaped + "\"";
//     }
// }


// ChatServer.java
// import com.sun.net.httpserver.HttpServer;
// import com.sun.net.httpserver.HttpExchange;

// import java.io.*;
// import java.net.*;
// import java.nio.charset.StandardCharsets;
// import java.nio.file.Files;
// import java.util.*;
// import java.util.concurrent.*;

// public class ChatServer {

//     // sessionId -> SessionData
//     private static final Map<String, SessionData> sessions = new ConcurrentHashMap<>();
//     private static final Random rnd = new Random();

//     public static void main(String[] args) throws Exception {
//         int port = 8000;
//         HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

//         // Serve static files under ./static
//         server.createContext("/", ChatServer::handleStatic);

//         // Chat endpoint
//         server.createContext("/chat", ChatServer::handleChat);

//         server.setExecutor(Executors.newFixedThreadPool(12));
//         server.start();
//         System.out.println("Server running at http://localhost:" + port);
//     }

//     // ---------------- static file handler ----------------
//     private static void handleStatic(HttpExchange exchange) throws IOException {
//         String path = exchange.getRequestURI().getPath();
//         if (path.equals("/")) path = "/index.html";
//         // prevent path traversal
//         File staticDir = new File("static").getCanonicalFile();
//         File f = new File(staticDir, path).getCanonicalFile();
//         if (!f.getPath().startsWith(staticDir.getPath()) || !f.exists() || f.isDirectory()) {
//             writePlain(exchange, 404, "404 Not Found");
//             return;
//         }
//         String mime = guessContentType(f.getName());
//         byte[] bytes = Files.readAllBytes(f.toPath());
//         exchange.getResponseHeaders().set("Content-Type", mime + "; charset=utf-8");
//         exchange.sendResponseHeaders(200, bytes.length);
//         try (OutputStream os = exchange.getResponseBody()) {
//             os.write(bytes);
//         }
//     }

//     // ---------------- chat handler ----------------
//     private static void handleChat(HttpExchange exchange) throws IOException {
//         // only POST allowed
//         if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
//             writePlain(exchange, 405, "Method not allowed");
//             return;
//         }

//         // identify or create session
//         String sessionId = getSessionIdFromHeaders(exchange);
//         if (sessionId == null || !sessions.containsKey(sessionId)) {
//             sessionId = createSessionId();
//             sessions.put(sessionId, new SessionData(sessionId));
//         }
//         // set cookie in response headers later

//         // read user message (frontend sends plain text body)
//         String body = readAll(exchange.getRequestBody());
//         String userMsg = body == null ? "" : body.trim();

//         SessionData s = sessions.get(sessionId);
//         s.appendUser(userMsg);

//         // detect/update topic
//         String detected = detectTopic(userMsg, s);
//         if (detected != null && detected.length() > 0) {
//             s.currentTopic = detected;
//         }

//         // handle contextual follow-ups (if matches known follow-up patterns)
//         String contextualAnswer = handleContextualQuery(userMsg, s);
//         String answer;
//         if (contextualAnswer != null) {
//             answer = contextualAnswer;
//         } else {
//             // fallback to direct search
//             answer = callTavilySmart(userMsg, s);
//         }

//         s.appendBot(answer);

//         // build JSON response
//         String json = "{\"answer\":" + jsonString(answer) + ", \"topic\":" + jsonString(s.currentTopic) + "}";

//         // send response with SESSIONID cookie
//         exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
//         exchange.getResponseHeaders().add("Set-Cookie", "SESSIONID=" + sessionId + "; Path=/; HttpOnly");
//         byte[] out = json.getBytes(StandardCharsets.UTF_8);
//         exchange.sendResponseHeaders(200, out.length);
//         try (OutputStream os = exchange.getResponseBody()) {
//             os.write(out);
//         }
//     }

//     // ---------------- session & helpers ----------------

//     private static class SessionData {
//         final String id;
//         String currentTopic = "";
//         final LinkedList<String> history = new LinkedList<>(); // alternating user/bot messages

//         SessionData(String id) { this.id = id; }

//         void appendUser(String m) {
//             history.add("User: " + m);
//             if (history.size() > 20) history.removeFirst();
//         }
//         void appendBot(String m) {
//             history.add("Bot: " + m);
//             if (history.size() > 20) history.removeFirst();
//         }
//         List<String> lastN(int n) {
//             int start = Math.max(0, history.size()-n);
//             return new ArrayList<>(history.subList(start, history.size()));
//         }
//     }

//     private static String getSessionIdFromHeaders(HttpExchange exchange) {
//         List<String> cookies = exchange.getRequestHeaders().get("Cookie");
//         if (cookies == null) return null;
//         for (String cookie : cookies) {
//             for (String part : cookie.split(";")) {
//                 String[] kv = part.trim().split("=", 2);
//                 if (kv.length == 2 && kv[0].equals("SESSIONID")) return kv[1];
//             }
//         }
//         return null;
//     }

//     private static String createSessionId() {
//         return Long.toHexString(rnd.nextLong()) + Long.toHexString(System.nanoTime());
//     }

//     private static void writePlain(HttpExchange exchange, int status, String msg) throws IOException {
//         byte[] out = msg.getBytes(StandardCharsets.UTF_8);
//         exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
//         exchange.sendResponseHeaders(status, out.length);
//         try (OutputStream os = exchange.getResponseBody()) { os.write(out); }
//     }

//     private static String readAll(InputStream is) throws IOException {
//         ByteArrayOutputStream baos = new ByteArrayOutputStream();
//         byte[] buf = new byte[2048];
//         int r;
//         while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
//         return new String(baos.toByteArray(), StandardCharsets.UTF_8);
//     }

//     private static String guessContentType(String filename) {
//         filename = filename.toLowerCase();
//         if (filename.endsWith(".html")) return "text/html";
//         if (filename.endsWith(".css")) return "text/css";
//         if (filename.endsWith(".js")) return "application/javascript";
//         if (filename.endsWith(".png")) return "image/png";
//         if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
//         return "text/plain";
//     }

//     // ---------------- topic detection (universal) ----------------

//     private static String detectTopic(String msg, SessionData session) {
//         if (msg == null) return session.currentTopic;
//         String lower = msg.toLowerCase().trim();

//         // 1) patterns where user explicitly mentions 'about' or 'explain' etc.
//         String[] triggers = { "about", "explain", "explain about", "give about", "tell about", "details of", "details on", "short note", "note on", "summary of", "summary", "history of", "history" };
//         for (String t : triggers) {
//             int idx = lower.indexOf(t);
//             if (idx != -1) {
//                 String after = lower.substring(idx + t.length()).trim();
//                 if (after.length() > 1) {
//                     return cleanTopic(after);
//                 }
//             }
//         }

//         // 2) if user message is short (<=4 words), treat whole message as topic
//         String[] parts = lower.split("\\s+");
//         if (parts.length <= 4) {
//             // avoid generic small words
//             String candidate = cleanTopic(lower);
//             if (candidate.length() > 1 && !candidate.equals("yes") && !candidate.equals("no")) return candidate;
//         }

//         // 3) fallback: extract important nouns/words (remove stopwords)
//         String[] stop = { "the","is","a","an","in","on","of","to","and","for","with","about","please","give","me","something" };
//         List<String> words = new ArrayList<>();
//         for (String w : parts) {
//             w = w.replaceAll("[^a-z0-9\\- ]","").trim();
//             if (w.length() <= 2) continue;
//             boolean s = false;
//             for (String sw : stop) if (w.equals(sw)) { s=true; break; }
//             if (!s) words.add(w);
//         }
//         if (!words.isEmpty()) {
//             // pick first 1-3 words
//             int take = Math.min(3, words.size());
//             return String.join(" ", words.subList(0, take));
//         }
//         return session.currentTopic;
//     }

//     private static String cleanTopic(String raw) {
//         String w = raw.trim();
//         // remove trailing question marks/punctuation
//         w = w.replaceAll("^[^a-z0-9]+","").replaceAll("[^a-z0-9]+$","");
//         // collapse multi spaces and trim
//         w = w.replaceAll("\\s+", " ").trim();
//         return w;
//     }

//     // ---------------- contextual follow-up handling ----------------

//     private static String handleContextualQuery(String msg, SessionData s) {
//         if (msg == null) return null;
//         String lower = msg.toLowerCase();

//         // check short followups (no topic mention)
//         String topic = s.currentTopic;
//         boolean hasTopic = topic != null && topic.trim().length() > 0;

//         // Patterns -> prioritized
//         if (containsAny(lower, "who invented", "who created", "who founded", "founder", "inventor", "who made", "who started")) {
//             if (hasTopic) return callTavilySmart(topic + " inventor", s);
//         }
//         if (containsAny(lower, "who is the father", "father of", "father", "who is father")) {
//             if (hasTopic) return callTavilySmart(topic + " founder", s);
//         }
//         if (containsAny(lower, "advantages", "benefits", "pros")) {
//             if (hasTopic) return callTavilySmart("advantages of " + topic, s);
//         }
//         if (containsAny(lower, "disadvantages", "drawbacks", "cons")) {
//             if (hasTopic) return callTavilySmart("disadvantages of " + topic, s);
//         }
//         if (containsAny(lower, "history", "origin", "when was", "started")) {
//             if (hasTopic) return callTavilySmart("history of " + topic, s);
//         }
//         if (containsAny(lower, "short note", "short", "paragraph", "summary", "brief")) {
//             if (hasTopic) return callTavilySmart(topic + " short summary", s);
//         }
//         if (containsAny(lower, "story", "anecdote", "funny story")) {
//             if (hasTopic) return callTavilySmart("story about " + topic, s);
//         }
//         if (containsAny(lower, "explain simply", "explain simply", "in simple")) {
//             if (hasTopic) return callTavilySmart(topic + " explained simply", s);
//         }
//         // if user's message is short (like 1-3 words) and no obvious intent, treat as follow-up
//         if (lower.length() < 25 && hasTopic) {
//             // map single words to likely intents: "who?" -> inventor; "when?" -> history; "why?" -> reason
//             if (lower.equals("who") || lower.equals("who?")) return callTavilySmart(topic + " inventor", s);
//             if (lower.equals("when") || lower.equals("when?")) return callTavilySmart("history of " + topic, s);
//             if (lower.equals("why") || lower.equals("why?")) return callTavilySmart(topic + " reasons importance", s);
//             if (lower.equals("advantages") || lower.equals("pros")) return callTavilySmart("advantages of " + topic, s);
//             if (lower.equals("disadvantages") || lower.equals("cons")) return callTavilySmart("disadvantages of " + topic, s);
//             // short ambiguous, try to combine topic + message
//             return callTavilySmart(topic + " " + lower, s);
//         }

//         return null;
//     }

//     private static boolean containsAny(String text, String... keys) {
//         for (String k : keys) if (text.contains(k)) return true;
//         return false;
//     }

//     // ---------------- Tavily call + result extraction ----------------

//     private static String callTavilySmart(String userQuery, SessionData session) {
//         // If query contains obvious topic words "it" or "this", replace with session topic
//         if (session != null && session.currentTopic != null && session.currentTopic.length() > 0) {
//             userQuery = userQuery.replaceAll("\\bit\\b", session.currentTopic);
//             userQuery = userQuery.replaceAll("\\bthis\\b", session.currentTopic);
//         }

//         try {
//             URL url = new URL(ApiConfig.TAVILY_API_URL);
//             HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//             conn.setRequestMethod("POST");
//             conn.setConnectTimeout(15000);
//             conn.setReadTimeout(30000);
//             conn.setRequestProperty("Content-Type", "application/json");
//             conn.setRequestProperty("Authorization", "Bearer " + ApiConfig.TAVILY_API_KEY);
//             conn.setDoOutput(true);

//             String payload = "{\"query\":" + jsonString(userQuery) + "}";

//             try (OutputStream os = conn.getOutputStream()) {
//                 os.write(payload.getBytes(StandardCharsets.UTF_8));
//             }

//             int status = conn.getResponseCode();
//             InputStream in = status >=200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
//             String raw = readAll(in);

//             // 1) Try "answer" top-level
//             String topAnswer = extractJsonField(raw, "answer");
//             if (isUsable(topAnswer)) return tidy(topAnswer);

//             // 2) Extract contents from results[].content and snippets; filter out youtube/product junk
//             List<String> contents = extractAllJsonFields(raw, "content");
//             List<String> snippets = extractAllJsonFields(raw, "snippet");
//             List<Candidate> candidates = new ArrayList<>();

//             for (String c : contents) {
//                 if (!isRelevantText(c)) continue;
//                 candidates.add(new Candidate(c, scoreMatch(c, userQuery)));
//             }
//             for (String s : snippets) {
//                 if (!isRelevantText(s)) continue;
//                 candidates.add(new Candidate(s, scoreMatch(s, userQuery)));
//             }

//             // if no candidates, try "title" fields
//             if (candidates.isEmpty()) {
//                 List<String> titles = extractAllJsonFields(raw, "title");
//                 for (String t : titles) if (isRelevantText(t)) candidates.add(new Candidate(t, scoreMatch(t, userQuery)));
//             }

//             if (!candidates.isEmpty()) {
//                 // pick best by score (and length moderate)
//                 Collections.sort(candidates, new Comparator<Candidate>() {
//                     public int compare(Candidate a, Candidate b) { return b.score - a.score; }
//                 });
//                 String best = candidates.get(0).text;
//                 return tidy(best);
//             }

//             // fallback: return trimmed raw text (not ideal)
//             return "I found results but couldn't format them. Raw: " + truncate(raw, 800);

//         } catch (Exception e) {
//             e.printStackTrace();
//             return "Error contacting search API: " + e.getMessage();
//         }
//     }

//     private static class Candidate {
//         final String text;
//         final int score;
//         Candidate(String t, int s) { text = t; score = s; }
//     }

//     private static boolean isRelevantText(String s) {
//         if (s == null) return false;
//         String low = s.toLowerCase();
//         if (low.contains("youtube") || low.contains("subscribe") || low.contains("watch on") || low.contains("video")) return false;
//         if (low.length() < 30) return false;
//         return true;
//     }

//     private static int scoreMatch(String text, String query) {
//         String t = text.toLowerCase();
//         String[] q = query.toLowerCase().split("\\s+");
//         int score = 0;
//         for (String w : q) {
//             if (w.length() <= 2) continue;
//             if (t.contains(w)) score += 2;
//             if (t.contains(w + " ")) score += 1;
//         }
//         // prefer medium-length answers
//         int len = Math.min(400, text.length());
//         score += Math.max(0, 200 - Math.abs(160 - len)) / 50;
//         return score;
//     }

//     private static boolean isUsable(String s) {
//         return s != null && s.trim().length() > 20;
//     }

//     private static String tidy(String s) {
//         if (s == null) return "";
//         String t = s.replaceAll("\\\\n", "\n").replaceAll("\\\\r", "").replaceAll("\\\\\"", "\"");
//         t = t.replaceAll("<[^>]+>", ""); // strip HTML tags
//         t = t.replaceAll("\\s{2,}", " ").trim();
//         if (t.length() > 1000) t = t.substring(0, 1000) + "...";
//         return t;
//     }

//     private static String truncate(String s, int max) {
//         if (s == null) return "";
//         return s.length() <= max ? s : s.substring(0, max-3) + "...";
//     }

//     // ---------------- simple JSON helpers (no libs) ----------------

//     private static String jsonString(String s) {
//         if (s == null) return "\"\"";
//         String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
//         return "\"" + escaped + "\"";
//     }

//     private static String extractJsonField(String json, String field) {
//         if (json == null) return null;
//         String key = "\"" + field + "\"";
//         int idx = json.indexOf(key);
//         if (idx == -1) return null;
//         int colon = json.indexOf(":", idx + key.length());
//         if (colon == -1) return null;
//         int quote = json.indexOf("\"", colon);
//         if (quote == -1) return null;
//         int end = findClosingQuote(json, quote + 1);
//         if (end == -1) return null;
//         return json.substring(quote + 1, end);
//     }

//     private static List<String> extractAllJsonFields(String json, String field) {
//         List<String> out = new ArrayList<>();
//         if (json == null) return out;
//         String key = "\"" + field + "\"";
//         int pos = 0;
//         while (true) {
//             int idx = json.indexOf(key, pos);
//             if (idx == -1) break;
//             int colon = json.indexOf(":", idx + key.length());
//             if (colon == -1) break;
//             int quote = json.indexOf("\"", colon);
//             if (quote == -1) break;
//             int end = findClosingQuote(json, quote + 1);
//             if (end == -1) break;
//             out.add(json.substring(quote + 1, end));
//             pos = end + 1;
//         }
//         return out;
//     }

//     private static int findClosingQuote(String s, int start) {
//         for (int i = start; i < s.length(); i++) {
//             char c = s.charAt(i);
//             if (c == '"') {
//                 int back = i - 1;
//                 int bs = 0;
//                 while (back >= 0 && s.charAt(back) == '\\') { bs++; back--; }
//                 if (bs % 2 == 0) return i;
//             }
//         }
//         return -1;
//     }
// }
// ChatServer.java
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {

    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // static file handler: serves files from ./static
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) path = "/index.html";
            File file = new File("static" + path).getCanonicalFile();

            // Simple security: deny files outside static/
            File staticDir = new File("static").getCanonicalFile();
            if (!file.getPath().startsWith(staticDir.getPath()) || !file.exists()) {
                String msg = "404 Not Found";
                sendPlain(exchange, 404, msg);
                return;
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            String contentType = guessContentType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // chat API: POST /chat
        server.createContext("/chat", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendPlain(exchange, 405, "Method Not Allowed");
                return;
            }

            // read full body (works for Java 8+)
            String body = readAll(exchange.getRequestBody());
            // The client sends plain text message as body (see frontend)
            String userMessage = body == null ? "" : body.trim();

            // Call Tavily
            String answer = callTavilySmart(userMessage);

            // Return JSON with answer and html snippet
            String json = "{\"answer\": " + jsonString(answer) + "}";
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server running at http://localhost:" + port);
    }

    // ---------------- helper methods ----------------

    private static void sendPlain(HttpExchange exchange, int status, String message) throws IOException {
        byte[] out = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, out.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
        }
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int r;
        while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String guessContentType(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".html")) return "text/html";
        if (n.endsWith(".css")) return "text/css";
        if (n.endsWith(".js")) return "application/javascript";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        return "text/plain";
    }

    // ---------------- Tavily call + smarter extraction ----------------
    private static String callTavilySmart(String query) {
        if (query == null || query.isEmpty()) return "Please ask a question.";

        try {
            URL url = new URL(ApiConfig.TAVILY_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + ApiConfig.TAVILY_API_KEY);
            conn.setDoOutput(true);

            // send minimal JSON: { "query": "..." }
            String payload = "{\"query\":" + jsonString(query) + "}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            InputStream in = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
            String response = readAll(in);

            // crude but effective extraction:
            // 1) try top-level "answer" field
            String answer = extractJsonField(response, "answer");
            if (isUsable(answer)) return tidy(answer);

            // 2) look for results[].content with best match (skip youtube)
            List<String> contents = extractAllJsonFields(response, "content");
            List<String> candidates = new ArrayList<>();
            for (String c : contents) {
                if (c == null) continue;
                String low = c.toLowerCase();
                if (low.contains("youtube") || low.contains("subscribe") || low.contains("watch on")) continue;
                if (c.trim().length() < 30) continue;
                candidates.add(c);
            }
            if (!candidates.isEmpty()) {
                // pick the candidate with most keyword matches
                String[] keywords = query.toLowerCase().split("\\s+");
                String best = candidates.get(0);
                int bestScore = -1;
                for (String cand : candidates) {
                    int score = 0;
                    String low = cand.toLowerCase();
                    for (String k : keywords) if (k.length()>1 && low.contains(k)) score++;
                    if (score > bestScore) { bestScore = score; best = cand; }
                }
                return tidy(best);
            }

            // 3) fallback: try "results" snippet or title
            String snippet = extractJsonField(response, "snippet");
            if (isUsable(snippet)) return tidy(snippet);

            // 4) final fallback: return raw trimmed JSON (not ideal)
            return "I found results but couldn't format them nicely. Raw response:\n" + truncate(response, 1000);

        } catch (Exception e) {
            e.printStackTrace();
            return "Error contacting search API: " + e.getMessage();
        }
    }

    // small helpers -------------------------------------------------

    private static boolean isUsable(String s) {
        return s != null && s.trim().length() > 20;
    }

    private static String tidy(String s) {
        if (s == null) return "";
        // unescape common sequences
        String t = s.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"").replace("\\t", "\t");
        // remove HTML tags if present
        t = t.replaceAll("<[^>]+>", "");
        // trim and shorten
        t = t.trim();
        if (t.length() > 800) t = t.substring(0, 800) + "...";
        return t;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    // extract first occurrence of "field": "value" (handles escaped quotes crudely)
    private static String extractJsonField(String json, String field) {
        if (json == null) return null;
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx + key.length());
        if (colon == -1) return null;
        int valStart = json.indexOf("\"", colon);
        if (valStart == -1) return null;
        int valEnd = findClosingQuote(json, valStart + 1);
        if (valEnd == -1) return null;
        return json.substring(valStart + 1, valEnd);
    }

    // extract all occurrences of "field": "value"
    private static List<String> extractAllJsonFields(String json, String field) {
        List<String> out = new ArrayList<>();
        if (json == null) return out;
        String key = "\"" + field + "\"";
        int pos = 0;
        while (true) {
            int idx = json.indexOf(key, pos);
            if (idx == -1) break;
            int colon = json.indexOf(":", idx + key.length());
            if (colon == -1) break;
            int valStart = json.indexOf("\"", colon);
            if (valStart == -1) break;
            int valEnd = findClosingQuote(json, valStart + 1);
            if (valEnd == -1) break;
            String val = json.substring(valStart + 1, valEnd);
            out.add(val);
            pos = valEnd + 1;
        }
        return out;
    }

    // find closing unescaped quote index
    private static int findClosingQuote(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '"') {
                int back = i - 1;
                int bs = 0;
                while (back >= 0 && s.charAt(back) == '\\') { bs++; back--; }
                if (bs % 2 == 0) return i;
            }
        }
        return -1;
    }

    private static String jsonString(String s) {
        if (s == null) return "\"\"";
        String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }
}
