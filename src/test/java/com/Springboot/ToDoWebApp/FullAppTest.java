package com.Springboot.ToDoWebApp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts the whole app (Tomcat, security, JSPs and a database) and uses it the way a browser does.
 * This is what catches mistakes in the JSP pages, which the other tests never render.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FullAppTest {

    private static final Pattern CSRF_TOKEN = Pattern.compile("name=\"_csrf\"[^>]*?value=\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @Test
    void userCanAddUpdateAndDeleteATodo() throws Exception {
        Browser pranav = loggedInAs("Pranav");
        assertTrue(pranav.get("/").body().contains("Welcome Pranav"));

        String description = "Original " + UUID.randomUUID();
        addTodo(pranav, description);
        String list = pranav.get("/list-todos").body();
        assertTrue(list.contains(description));
        String id = idOfTodo(list, description);

        HttpResponse<String> updateForm = pranav.get("/update-todo?id=" + id);
        assertEquals(200, updateForm.statusCode());
        assertTrue(updateForm.body().contains(description));

        String updated = "Updated " + UUID.randomUUID();
        HttpResponse<String> afterUpdate = pranav.post("/update-todo", Map.of(
                "id", id, "description", updated, "targetDate", "2030-01-01", "done", "false",
                "_csrf", csrfToken(updateForm.body())));
        assertEquals(200, afterUpdate.statusCode());
        assertTrue(afterUpdate.body().contains(updated));
        assertFalse(afterUpdate.body().contains(description));

        HttpResponse<String> afterDelete = pranav.post("/delete-todo", Map.of(
                "id", id, "_csrf", csrfToken(afterUpdate.body())));
        assertEquals(200, afterDelete.statusCode());
        assertFalse(afterDelete.body().contains(updated));
    }

    @Test
    void userCanMarkATodoAsDoneAndUndoIt() throws Exception {
        Browser pranav = loggedInAs("Pranav");
        String description = "Toggle me " + UUID.randomUUID();
        addTodo(pranav, description);

        String list = pranav.get("/list-todos").body();
        assertEquals("Done", toggleButtonLabel(list, description));
        String id = idOfTodo(list, description);

        HttpResponse<String> afterDone = pranav.post("/toggle-todo", Map.of("id", id, "_csrf", csrfToken(list)));
        assertEquals(200, afterDone.statusCode());
        assertEquals("Undo", toggleButtonLabel(afterDone.body(), description));

        HttpResponse<String> afterUndo = pranav.post("/toggle-todo",
                Map.of("id", id, "_csrf", csrfToken(afterDone.body())));
        assertEquals(200, afterUndo.statusCode());
        assertEquals("Done", toggleButtonLabel(afterUndo.body(), description));
    }

    @Test
    void anotherUserCannotOpenOrDeleteATodo() throws Exception {
        Browser pranav = loggedInAs("Pranav");
        String description = "Private " + UUID.randomUUID();
        addTodo(pranav, description);
        String list = pranav.get("/list-todos").body();
        String id = idOfTodo(list, description);

        Browser pran = loggedInAs("Pran");
        assertFalse(pran.get("/list-todos").body().contains(description));
        assertEquals(404, pran.get("/update-todo?id=" + id).statusCode());

        String pransToken = csrfToken(pran.get("/add-todos").body());
        assertEquals(404, pran.post("/delete-todo", Map.of("id", id, "_csrf", pransToken)).statusCode());

        assertTrue(pranav.get("/list-todos").body().contains(description));
    }

    @Test
    void scriptsInTodoDescriptionsAreNotRun() throws Exception {
        Browser pranav = loggedInAs("Pranav");
        String marker = UUID.randomUUID().toString();
        String attack = "<script>alert('" + marker + "')</script>";

        addTodo(pranav, attack);

        String list = pranav.get("/list-todos").body();
        assertFalse(list.contains(attack), "The script must not reach the page as real HTML");
        assertTrue(list.contains("&lt;script&gt;"), "The script should be shown as plain text");

        String id = Pattern.compile("(?s)" + marker + ".*?update-todo\\?id=(\\d+)").matcher(list)
                .results().findFirst().orElseThrow().group(1);
        assertFalse(pranav.get("/update-todo?id=" + id).body().contains(attack));
    }

    @Test
    void forgedDeleteWithoutTokenIsRejected() throws Exception {
        Browser pranav = loggedInAs("Pranav");
        String description = "Keep me " + UUID.randomUUID();
        addTodo(pranav, description);
        String id = idOfTodo(pranav.get("/list-todos").body(), description);

        assertEquals(403, pranav.post("/delete-todo", Map.of("id", id)).statusCode());
        assertEquals(405, pranav.get("/delete-todo?id=" + id).statusCode());

        assertTrue(pranav.get("/list-todos").body().contains(description));
    }

    @Test
    void userCanLogOutThroughTheConfirmationPage() throws Exception {
        Browser pranav = loggedInAs("Pranav");

        HttpResponse<String> confirmation = pranav.get("/logout");
        assertEquals(200, confirmation.statusCode());

        HttpResponse<String> afterLogout = pranav.post("/logout", Map.of("_csrf", csrfToken(confirmation.body())));
        assertTrue(afterLogout.uri().toString().contains("/login"));

        HttpResponse<String> protectedPage = pranav.get("/list-todos");
        assertTrue(protectedPage.uri().toString().contains("/login"));
    }

    private void addTodo(Browser browser, String description) throws Exception {
        String form = browser.get("/add-todos").body();
        HttpResponse<String> response = browser.post("/add-todos", Map.of(
                "id", "0", "description", description, "targetDate", "2030-01-01", "done", "false",
                "_csrf", csrfToken(form)));
        assertEquals(200, response.statusCode());
    }

    private Browser loggedInAs(String username) throws Exception {
        Browser browser = new Browser();
        String loginPage = browser.get("/login").body();
        HttpResponse<String> response = browser.post("/login", Map.of(
                "username", username, "password", "qwerty", "_csrf", csrfToken(loginPage)));
        assertFalse(response.uri().toString().contains("error"), "Login failed for " + username);
        return browser;
    }

    private String idOfTodo(String listPage, String description) {
        Matcher matcher = Pattern.compile("(?s)" + Pattern.quote(description) + ".*?update-todo\\?id=(\\d+)")
                .matcher(listPage);
        assertTrue(matcher.find(), "Todo not found on the list page");
        return matcher.group(1);
    }

    private String toggleButtonLabel(String listPage, String description) {
        Matcher matcher = Pattern.compile("(?s)" + Pattern.quote(description)
                + ".*?action=\"toggle-todo\".*?<button[^>]*>\\s*(\\w+)\\s*</button>").matcher(listPage);
        assertTrue(matcher.find(), "Toggle button not found for the todo");
        return matcher.group(1);
    }

    private String csrfToken(String page) {
        Matcher matcher = CSRF_TOKEN.matcher(page);
        assertTrue(matcher.find(), "The page has no CSRF token");
        return matcher.group(1);
    }

    /** Keeps its own login cookie and follows redirects, like a browser. */
    private class Browser {

        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpResponse<String> get(String path) throws IOException, InterruptedException {
            return send(HttpRequest.newBuilder(uri(path)).GET().build());
        }

        HttpResponse<String> post(String path, Map<String, String> form) throws IOException, InterruptedException {
            String body = new LinkedHashMap<>(form).entrySet().stream()
                    .map(field -> encode(field.getKey()) + "=" + encode(field.getValue()))
                    .collect(Collectors.joining("&"));
            return send(HttpRequest.newBuilder(uri(path))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build());
        }

        private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        private URI uri(String path) {
            return URI.create("http://localhost:" + port + path);
        }

        private String encode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
    }
}
