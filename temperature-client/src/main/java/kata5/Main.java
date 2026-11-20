package kata5;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    private final static String address = "http://localhost:8090";

    static void main() throws InterruptedException, IOException {
        try (HttpClient client = client()) {
            while (true) {
                int fahrenheit = ThreadLocalRandom.current().nextInt(1, 48);
                HttpRequest request = request(fahrenheit);
                CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                future.thenAccept(Main::showResponse);
                Thread.sleep(100);
            }
        }
    }

    private static void showResponse(HttpResponse<String> response) {
        if (response.statusCode() / 100 != 2) System.out.println("NGINX response " + response.statusCode());
        else {
            String node = response.headers().firstValue("X-Node").orElse("unknown");
            System.out.println("Node: " + node + " | Body: " + response.body());
        }
    }

    private static HttpRequest request(int fahrenheit) {
        return HttpRequest.newBuilder(URI.create(address).resolve("/api/v1/temperature/?f=" + fahrenheit))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
    }

    private static HttpClient client() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }
}