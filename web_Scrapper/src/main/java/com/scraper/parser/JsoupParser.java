package com.scraper.parser;

import com.scraper.model.ScrapedItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public class JsoupParser {
    public List<ScrapedItem> parse(String html, String baseUrl) {
        Document doc = Jsoup.parse(html, baseUrl);
        return doc.select("article.product_pod").stream()
                .map(this::toItem)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<String> extractLinks(Document doc) {
        String baseHost = host(doc.location());
        return doc.select("a[href]").stream()
                // absUrl resolves relative links safely using the document base URI.
                .map(link -> link.absUrl("href"))
                .filter(url -> url.startsWith("https://"))
                .filter(url -> baseHost != null && baseHost.equals(host(url)))
                .distinct()
                .toList();
    }

    public Document document(String html, String baseUrl) {
        return Jsoup.parse(html, baseUrl);
    }

    private ScrapedItem toItem(Element card) {
        try {
            Element titleLink = card.selectFirst("h3 > a[title]");
            Element price = card.selectFirst("p.price_color");
            Element href = card.selectFirst("a[href]");
            return new ScrapedItem(
                    titleLink.attr("title"),
                    price.text(),
                    href.absUrl("href")
            );
        } catch (NullPointerException ex) {
            System.out.println("Skipping item with missing element: " + ex.getMessage());
            return null;
        }
    }

    private String host(String url) {
        try {
            return URI.create(url).getHost().toLowerCase();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
