package com.scraper.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scraper.model.ScrapedItem;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JsonExporter {
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void export(List<ScrapedItem> items, String filePath) throws IOException {
        mapper.writeValue(new File(filePath), items);
    }
}
