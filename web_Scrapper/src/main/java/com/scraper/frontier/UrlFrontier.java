package com.scraper.frontier;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class UrlFrontier {
    private final Queue<String> queue = new LinkedList<>();
    private final Set<String> visited = new HashSet<>();

    public synchronized void add(String url) {
        // The frontier is shared crawl state, so synchronized keeps queue and visited updates atomic.
        String normalized = normalize(url);
        if (normalized != null && visited.add(normalized)) {
            queue.add(normalized);
        }
    }

    public synchronized String poll() {
        return queue.poll();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    private String normalize(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase();
            String host = uri.getHost() == null ? null : uri.getHost().toLowerCase();
            String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            String query = cleanQuery(uri.getRawQuery());
            return new URI(scheme, uri.getUserInfo(), host, uri.getPort(), path, query, null).toString();
        } catch (IllegalArgumentException | java.net.URISyntaxException ex) {
            return url.trim();
        }
    }

    private String cleanQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        StringBuilder cleaned = new StringBuilder();
        for (String pair : rawQuery.split("&")) {
            String name = pair.split("=", 2)[0].toLowerCase();
            if (isTrackingParam(name)) {
                continue;
            }
            if (!cleaned.isEmpty()) {
                cleaned.append('&');
            }
            cleaned.append(pair);
        }
        return cleaned.isEmpty() ? null : cleaned.toString();
    }

    private boolean isTrackingParam(String name) {
        return name.startsWith("utm_")
                || name.equals("fbclid")
                || name.equals("gclid")
                || name.equals("jsessionid")
                || name.equals("phpsessid")
                || name.equals("session")
                || name.equals("sessionid")
                || name.equals("sid");
    }
}
