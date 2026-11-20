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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WebService {
    private final String id;
    private final String host;
    private final int port;
    private final String consult;

    public WebService(String id, String host, int port, String consult) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.consult = consult;
    }
    public void start()
    {
        Javalin app = Javalin.create(this::init).start(host, port);
        try {
            register();
            System.out.println("Registered: " + id + " as " + host() + ":" + port);
        } catch (Exception e) {
            app.stop();
            throw new RuntimeException(e);
        }
    }

    private void register() {
        try(HttpClient client = HttpClient.newHttpClient()) {
            CompletableFuture<HttpResponse<String>> response = client.sendAsync(requestWith(json()), HttpResponse.BodyHandlers.ofString());
            response.thenAccept(this::checkStatusCode).whenComplete((res, ex) -> client.close());
        }
    }

    private HttpRequest requestWith(String json) {
        return HttpRequest.newBuilder(URI.create(consult).resolve("/v1/agent/service/register"))
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(5))
                .headers("Content-Type", "application/json")
                .build();
    }

    private void init(JavalinConfig config) {
        config.routes.get("/", this::kilometersToMiles);
        config.routes.get("/health", this::health);
    }

    private void health(Context context) {
        context.result("OK");
    }

    private void kilometersToMiles(Context context) {
        try {
            double kilometers = Double.parseDouble(context.queryParam("km"));
            context.header("X-Node", id);
            context.result(new Gson().toJson(kilometersToMiles(kilometers)));
        } catch (NumberFormatException | NullPointerException e) {
            context.status(400).result("Missing or invalid query parameter 'km'");
        }
    }

    double kilometersToMiles(double kilometers) {
        return kilometers / 1.609344;
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
                "Name", "distance_service_v1",
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
