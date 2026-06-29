# Java Web Scraper

This project is a small Java 17 web crawler for [books.toscrape.com](https://books.toscrape.com). It starts from a seed URL, respects the site's `robots.txt` rules for `User-agent: *`, rate-limits requests, retries transient fetch failures, parses book cards with Jsoup, and exports the collected title, price, and URL data to JSON and CSV files.

## How to run

```bash
mvn exec:java
```

To use a different seed URL:

```bash
mvn exec:java -Dexec.args="https://books.toscrape.com"
```

To change the default seed or crawl limit, edit `DEFAULT_SEED` and `MAX_PAGES` in `src/main/java/com/scraper/ScraperApp.java`.

## Sample output

`output.json`

```json
[
  {
    "title": "A Light in the Attic",
    "price": "£51.77",
    "url": "https://books.toscrape.com/catalogue/a-light-in-the-attic_1000/index.html"
  }
]
```

`output.csv`

```csv
"Title","Price","URL"
"A Light in the Attic","£51.77","https://books.toscrape.com/catalogue/a-light-in-the-attic_1000/index.html"
```

## Architecture

```text
ScraperApp
  -> UrlFrontier      queue, visited set, URL normalization and deduplication
  -> RateLimiter      Guava token acquisition before each request
  -> HttpFetcher      Java HttpClient, redirects, retries, Retry-After handling
  -> JsoupParser      book item parsing and same-domain link extraction
  -> Exporters        JsonExporter and CsvExporter write final results
```

## Edge cases handled

- Rate limiting defaults to 0.5 requests per second, or 1 request every 2 seconds.
- Fetch retries use exponential backoff: 1s, 2s, then 4s.
- HTTP 429 responses honor the `Retry-After` header before retrying.
- `robots.txt` `Disallow:` prefixes for `User-agent: *` are checked before crawling a URL.
- URL deduplication uses normalized URLs with lowercase hosts, stripped trailing slashes, and removed tracking/session query parameters.
- Relative links are resolved with Jsoup `absUrl("href")`, avoiding fragile manual URL concatenation.
- Individual page failures are logged and skipped so one bad page does not stop the crawl.
