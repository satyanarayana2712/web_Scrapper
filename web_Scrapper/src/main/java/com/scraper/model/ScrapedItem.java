package com.scraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScrapedItem {
    @JsonProperty("title")
    private final String title;

    @JsonProperty("price")
    private final String price;

    @JsonProperty("url")
    private final String url;

    public ScrapedItem(String title, String price, String url) {
        this.title = title;
        this.price = price;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getPrice() {
        return price;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "ScrapedItem{title='" + title + "', price='" + price + "', url='" + url + "'}";
    }
}
