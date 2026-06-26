import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class StressTest {

    // CONFIG
    private static final String URL = "http://localhost:8080/test/123";
    private static final int THREADS = 25;
    private static final int REQUESTS_PER_THREAD = 20000;

    public static void main(String[] args) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        long start = System.currentTimeMillis();

        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {

                for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                    try {

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(URL))
                                .GET()
                                .timeout(Duration.ofSeconds(10))
                                .build();

                        HttpResponse<String> response =
                                client.send(request, HttpResponse.BodyHandlers.ofString());

                        int code = response.statusCode();

                        if (code >= 200 && code < 300) {
                            success.incrementAndGet();
                        } else {
                            failed.incrementAndGet();
                            System.out.println("HTTP " + code);
                        }

                    } catch (Exception e) {
                        failed.incrementAndGet();
                        System.out.println("FAILED: " + e.getMessage());
                    }
                }

            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);

        long end = System.currentTimeMillis();

        int total = THREADS * REQUESTS_PER_THREAD;

        System.out.println("===== RESULT =====");
        System.out.println("Total Requests: " + total);
        System.out.println("Successful: " + success.get());
        System.out.println("Failed: " + failed.get());
        System.out.println("Time: " + (end - start) + " ms");
        System.out.println("Requests/s: " + (total * 1000.0 / (end - start)));
    }
}