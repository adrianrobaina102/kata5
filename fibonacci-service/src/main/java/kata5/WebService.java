package kata5;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

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
            throw new RuntimeException(e);
        }
    }

    private void register() {
        try(HttpClient client = HttpClient.newHttpClient()) {
            client.sendAsync(requestWith(json()), HttpResponse.BodyHandlers.ofString());
        }
    }

    private HttpRequest requestWith(String json) {
        return HttpRequest
                .newBuilder(URI.create(consult).resolve("/v1/agent/service/register"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    private void init(JavalinConfig config) {
        config.routes.get("/", this::fibonacci);
        config.routes.get("/health", this::health);
    }

    private void health(Context context) {
        context.result("OK");
    }


    private void fibonacci(Context context) {
        int terms = Integer.parseInt(context.queryParam("n"));
        context.header("X-Node", id);
        context.result(new Gson().toJson(fibonacci(terms)));
    }

    int[] fibonacci(int n) {
        int[] result = new int[n];
        int a = 0;
        int b = 1;
        for (int i = 0; i < n; i++) {
            result[i] = a;
            a = b;
            b += result[i];
        }
        return result;
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
                "Name", "fibonacci_service_v1",
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
