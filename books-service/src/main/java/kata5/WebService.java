package kata5;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class WebService {
    private final String id;
    private final String host;
    private final int port;
    private final String consul;
    private final String version;
    private final List<Books> books;

    public WebService(String id, String host, int port, String consul, String version, List<Books> books) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.consul = consul;
        this.version = version.toLowerCase();
        this.books = books;
    }

    public void start() {
        Javalin app = Javalin.create(this::init).start(host, port);
        try {
            register();
            System.out.println("Registered: " + id + " (" + version + ") as " + host() + ":" + port);
        } catch (Exception e) {
            app.stop();
            throw new RuntimeException(e);
        }
    }

    private void register() {
        try (HttpClient client = HttpClient.newHttpClient()) {
            CompletableFuture<HttpResponse<String>> response =
                    client.sendAsync(requestWith(json()), HttpResponse.BodyHandlers.ofString());
            response.thenAccept(this::checkStatusCode).whenComplete((res, ex) -> client.close());
        }
    }

    private HttpRequest requestWith(String json) {
        return HttpRequest.newBuilder(URI.create(consul).resolve("/v1/agent/service/register"))
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .build();
    }

    private void init(JavalinConfig config) {
        config.routes.get("/", "v1".equals(version) ? this::handleGenre : this::handleTitle);
        config.routes.get("/health", this::health);
    }

    private void health(Context context) {
        context.result("OK");
    }

    private void handleGenre(Context context) {
        try {
            String rawGenre = context.queryParam("genre");
            if (rawGenre == null || rawGenre.isBlank()) {
                throw new IllegalArgumentException("Missing genre");
            }
            Genre genre = Genre.valueOf(rawGenre.trim().toUpperCase());
            context.header("X-Node", id);
            context.result(new Gson().toJson(findByGenre(genre)));
        } catch (IllegalArgumentException e) {
            context.status(400).result("Missing or invalid query parameter 'genre'");
        }
    }

    private void handleTitle(Context context) {
        String title = context.queryParam("title");
        if (title == null || title.isBlank()) {
            context.status(400).result("Missing query parameter 'title'");
            return;
        }
        Optional<Books> match = findByTitle(title.trim());
        context.header("X-Node", id);
        if (match.isEmpty()) {
            context.status(404).result("Book not found");
            return;
        }
        context.result(new Gson().toJson(match.get()));
    }

    List<Books> findByGenre(Genre genre) {
        return books.stream().filter(book -> book.genre() == genre).toList();
    }

    Optional<Books> findByTitle(String title) {
        return books.stream().filter(book -> book.title().equalsIgnoreCase(title)).findFirst();
    }

    private void checkStatusCode(HttpResponse<String> response) {
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("Consul rechazó el registro (HTTP " + response.statusCode() + "): " + response.body());
        }
    }

    private String host() {
        return isLocalHost() ? "host.docker.internal" : host;
    }

    private boolean isLocalHost() {
        return "localhost".equals(host) ||
                "127.0.0.1".equals(host) ||
                "0.0.0.0".equals(host);
    }

    private String healthUrl() {
        return "http://" + host() + ":" + port + "/health";
    }

    private String json() {
        return new Gson().toJson(Map.of(
                "ID", id,
                "Name", "books_service_" + version,
                "Address", host(),
                "Port", port,
                "Check", Map.of(
                        "HTTP", healthUrl(),
                        "Interval", "5s",
                        "Timeout", "2s",
                        "DeregisterCriticalServiceAfter", "10m"
                )
        ));
    }
}
