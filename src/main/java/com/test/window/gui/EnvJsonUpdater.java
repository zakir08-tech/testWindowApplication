package com.test.window.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.scene.control.TableView;
import javafx.collections.ObservableList;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class for updating an environment JSON file (env.json) based on placeholders
 * found in a JavaFX TableView. The class extracts keys from cells containing {{key}}
 * patterns, adds any missing keys to the JSON map with empty string values, and persists
 * the updates if changes were made.
 */
public class EnvJsonUpdater {

    /**
     * A pre-configured ObjectMapper instance for JSON serialization and deserialization.
     * It enables indented output for readability and orders map entries by keys for consistency.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    /**
     * Updates the env.json file by scanning the provided TableView for placeholder keys
     * in the format {{key}}. Any keys not already present in the JSON are added with
     * empty string values. The file is written only if updates are made.
     *
     * @param table The TableView containing String[] rows to scan for placeholders.
     * @throws IOException If an error occurs while reading or writing the JSON file.
     */
    public static void updateEnvJsonFromTable(TableView<String[]> table) throws IOException {
        // Define the path to the env.json file in the project root.
        String jsonPath = "env.json";
        File jsonFile = new File(jsonPath);

        // Initialize an empty LinkedHashMap to preserve insertion order.
        Map<String, String> envMap = new LinkedHashMap<>();
        if (jsonFile.exists()) {
            // Read existing key-value pairs from the JSON file.
            envMap = objectMapper.readValue(jsonFile, new TypeReference<LinkedHashMap<String, String>>() {});
        } else {
            // Create parent directories if needed and create the file.
            if (jsonFile.getParentFile() != null) {
                jsonFile.getParentFile().mkdirs();
            }
            jsonFile.createNewFile();
        }

        // Extract unique keys from the table cells.
        Set<String> newKeys = extractKeysFromTable(table);

        // Flag to track if any updates were made.
        boolean updated = false;
        for (String key : newKeys) {
            // Add missing keys with empty values.
            if (!envMap.containsKey(key)) {
                envMap.put(key, "");
                updated = true;
            }
        }

        // Persist changes only if updates occurred.
        if (updated) {
            objectMapper.writeValue(jsonFile, envMap);
        }
    }

    /**
     * Extracts unique placeholder keys from all cells in the TableView.
     * Scans for patterns matching {{key}} and returns the captured 'key' parts.
     *
     * @param table The TableView to scan.
     * @return A Set of unique extracted keys.
     */
    private static Set<String> extractKeysFromTable(TableView<String[]> table) {
        // Use a HashSet to ensure uniqueness of keys.
        Set<String> keys = new HashSet<>();
        // Compile regex pattern to match {{key}} where 'key' is any non-empty string.
        Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");
        // Get the observable list of rows (String[]).
        ObservableList<String[]> items = table.getItems();
        for (String[] row : items) {
            for (String cell : row) {
                if (cell != null) {
                    // Find all matches in the cell content.
                    Matcher matcher = pattern.matcher(cell);
                    while (matcher.find()) {
                        // Add the captured group (the key inside {{}}).
                        keys.add(matcher.group(1));
                    }
                }
            }
        }
        return keys;
    }
}