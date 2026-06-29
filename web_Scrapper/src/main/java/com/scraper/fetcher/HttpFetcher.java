package com.scraper.fetcher;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpFetcher {
    private static final int MAX_ATTEMPTS = 3;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; JavaScraper/1.0)";
    private final HttpClient client;

    public HttpFetcher() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String fetch(String url) throws IOException, InterruptedException {
        IOException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            System.out.println("Fetching attempt " + attempt + ": " + url);
            try {
                HttpResponse<String> response = client.send(request(url), HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                if (statusCode == 200) {
                    return response.body();
                }
                if (statusCode == 429 && attempt < MAX_ATTEMPTS) {
                    sleepRetryAfter(response);
                } else {
                    throw new IOException("HTTP " + statusCode);
                }
            } catch (IOException ex) {
                lastError = ex;
                if (attempt == MAX_ATTEMPTS) {
                    break;
                }
                // Exponential backoff gives transient network/server failures time to clear.
                sleepBackoff(attempt);
            }
        }
        throw lastError == null ? new IOException("Fetch failed") : lastError;
    }

    private HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
    }

    private void sleepRetryAfter(HttpResponse<String> response) throws InterruptedException {
        long seconds = response.headers()
                .firstValue("Retry-After")
                .map(this::parseRetryAfter)
                .orElse(1L);
        System.out.println("HTTP 429 received. Sleeping " + seconds + "s before retry.");
        Thread.sleep(seconds * 1_000L);
    }

    private long parseRetryAfter(String value) {
        try {
            return Math.max(1L, Long.parseLong(value.trim()));
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }

    private void sleepBackoff(int attempt) throws InterruptedException {
        long seconds = (long) Math.pow(2, attempt - 1);
        System.out.println("Backing off for " + seconds + "s.");
        Thread.sleep(seconds * 1_000L);
    }
}
