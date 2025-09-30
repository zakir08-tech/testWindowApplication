package com.test.window.gui.com;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javafx.scene.control.TableView;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class TableConfig {
    private final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    public String formatJson(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input != null ? input : "";
        }
        try {
            Object parsedJson = objectMapper.readValue(input, LinkedHashMap.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedJson);
        } catch (Exception e) {
            System.err.println("JSON formatting error: " + e.getMessage());
            return input;
        }
    }

    public boolean isValidTestId(String testId, Set<String> testIds, String currentId, int rowIndex, TableView<String[]> table) {
        if (testId == null || testId.isEmpty()) {
            return false;
        }
        if (!testId.equals(currentId) && testIds.contains(testId)) {
            return false;
        }
        if (testId.length() > 5) {
            return false;
        }
        if (!testId.matches("^[0-9#]*$")) {
            return false;
        }
        if (testId.startsWith("#") && testId.matches(".*[0-9].*")) {
            return false;
        }
        if (testId.matches("^[0-9].*") && testId.contains("#")) {
            return false;
        }
        return true;
    }
}