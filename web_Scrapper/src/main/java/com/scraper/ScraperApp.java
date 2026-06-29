package com.scraper;

import com.scraper.exporter.CsvExporter;
import com.scraper.exporter.JsonExporter;
import com.scraper.fetcher.HttpFetcher;
import com.scraper.fetcher.RateLimiter;
import com.scraper.frontier.UrlFrontier;
import com.scraper.model.ScrapedItem;
import com.scraper.parser.JsoupParser;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScraperApp {
    private static final String DEFAULT_SEED = "https://books.toscrape.com";
    private static final int MAX_PAGES = 50;
    private static final String JSON_OUTPUT = "output.json";
    private static final String CSV_OUTPUT = "output.csv";

    public static void main(String[] args) throws IOException {
        String seedUrl = args.length > 0 ? args[0] : DEFAULT_SEED;
        UrlFrontier frontier = new UrlFrontier();
        HttpFetcher fetcher = new HttpFetcher();
        RateLimiter rateLimiter = new RateLimiter();
        JsoupParser parser = new JsoupParser();
        List<ScrapedItem> allItems = new ArrayList<>();
        Set<String> disallowedPaths = loadRobotsDisallow(seedUrl, fetcher);

        frontier.add(seedUrl);
        int pagesCrawled = 0;

        while (!frontier.isEmpty() && pagesCrawled < MAX_PAGES) {
            String url = frontier.poll();
            if (url == null || isDisallowed(url, disallowedPaths)) {
                System.out.println("Skipping disallowed URL: " + url);
                continue;
            }
            rateLimiter.acquire();
            try {
                String html = fetcher.fetch(url);
                allItems.addAll(parser.parse(html, url));
                Document doc = parser.document(html, url);
                parser.extractLinks(doc).forEach(frontier::add);
                pagesCrawled++;
            } catch (IOException ex) {
                System.out.println("Fetch failed for " + url + ": " + ex.getMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted while fetching " + url + ": " + ex.getMessage());
                break;
            } catch (RuntimeException ex) {
                System.out.println("Page failed for " + url + ": " + ex.getMessage());
            }
        }

        new JsonExporter().export(allItems, JSON_OUTPUT);
        new CsvExporter().export(allItems, CSV_OUTPUT);
        System.out.println("Pages crawled: " + pagesCrawled);
        System.out.println("Items found: " + allItems.size());
        System.out.println("Files written: " + JSON_OUTPUT + ", " + CSV_OUTPUT);
    }

    private static Set<String> loadRobotsDisallow(String seedUrl, HttpFetcher fetcher) {
        Set<String> disallowed = new HashSet<>();
        try {
            String robotsUrl = robotsUrl(seedUrl);
            String robotsTxt = fetcher.fetch(robotsUrl);
            parseRobotsForWildcard(robotsTxt, disallowed);
        } catch (Exception ex) {
            System.out.println("Robots check skipped: " + ex.getMessage());
        }
        return disallowed;
    }

    private static String robotsUrl(String seedUrl) {
        URI uri = URI.create(seedUrl);
        return uri.getScheme() + "://" + uri.getHost() + "/robots.txt";
    }

    private static void parseRobotsForWildcard(String robotsTxt, Set<String> disallowed) {
        boolean applies = false;
        for (String rawLine : robotsTxt.split("\\R")) {
            String line = rawLine.split("#", 2)[0].trim();
            if (line.isBlank()) {
                continue;
            }
            String lower = line.toLowerCase();
            if (lower.startsWith("user-agent:")) {
                applies = lower.substring("user-agent:".length()).trim().equals("*");
            } else if (applies && lower.startsWith("disallow:")) {
                String path = line.substring("disallow:".length()).trim();
                if (!path.isBlank()) {
                    disallowed.add(path);
                }
            }
        }
    }

    private static boolean isDisallowed(String url, Set<String> disallowedPaths) {
        String path = URI.create(url).getPath();
        return disallowedPaths.stream().anyMatch(path::startsWith);
    }
}
