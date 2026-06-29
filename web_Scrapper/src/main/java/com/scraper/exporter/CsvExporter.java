package com.scraper.exporter;

import com.opencsv.CSVWriter;
import com.scraper.model.ScrapedItem;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {
    public void export(List<ScrapedItem> items, String filePath) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            writer.writeNext(new String[]{"Title", "Price", "URL"});
            for (ScrapedItem item : items) {
                writer.writeNext(new String[]{item.getTitle(), item.getPrice(), item.getUrl()});
            }
        }
    }
}
