package kata5;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    private static final String ADDRESS = "http://localhost:8080";
    private static final List<String> GENRES = List.of("FANTASY", "SCI_FI", "DYSTOPIAN", "MEME", "PROGRAMMING");
    private static final List<String> TITLES = List.of("1984", "Dune", "The Hobbit", "Clean Code", "HTML for Babies");

    static void main() throws InterruptedException, IOException {
        try (HttpClient client = client()) {
            while (true) {
                String genre = GENRES.get(ThreadLocalRandom.current().nextInt(GENRES.size()));
                String title = TITLES.get(ThreadLocalRandom.current().nextInt(TITLES.size()));

                client.sendAsync(requestV1(genre), HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> showResponse("v1", response));
                client.sendAsync(requestV2(title), HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> showResponse("v2", response));

                Thread.sleep(200);
            }
        }
    }

    private static void showResponse(String version, HttpResponse<String> response) {
        if (response.statusCode() / 100 != 2) {
            System.out.println("[" + version + "] NGINX response " + response.statusCode());
            return;
        }
        String node = response.headers().firstValue("X-Node").orElse("unknown");
        System.out.println("[" + version + "] Node: " + node + " | Body: " + response.body());
    }

    private static HttpRequest requestV1(String genre) {
        return HttpRequest.newBuilder(URI.create(ADDRESS).resolve("/api/v1/books/?genre=" + genre))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
    }

    private static HttpRequest requestV2(String title) {
        String encoded = URLEncoder.encode(title, StandardCharsets.UTF_8);
        return HttpRequest.newBuilder(URI.create(ADDRESS).resolve("/api/v2/books/?title=" + encoded))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
    }

    private static HttpClient client() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }
}
