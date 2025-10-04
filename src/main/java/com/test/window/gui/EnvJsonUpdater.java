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

public class EnvJsonUpdater {

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    public static void updateEnvJsonFromTable(TableView<String[]> table) throws IOException {
        String jsonPath = "env.json"; // Changed to project root folder
        File jsonFile = new File(jsonPath);

        Map<String, String> envMap = new LinkedHashMap<>();
        if (jsonFile.exists()) {
            envMap = objectMapper.readValue(jsonFile, new TypeReference<LinkedHashMap<String, String>>() {});
        } else {
            if (jsonFile.getParentFile() != null) {
                jsonFile.getParentFile().mkdirs();
            }
            jsonFile.createNewFile();
        }

        Set<String> newKeys = extractKeysFromTable(table);

        boolean updated = false;
        for (String key : newKeys) {
            if (!envMap.containsKey(key)) {
                envMap.put(key, "");
                updated = true;
            }
        }

        if (updated) {
            objectMapper.writeValue(jsonFile, envMap);
        }
    }

    private static Set<String> extractKeysFromTable(TableView<String[]> table) {
        Set<String> keys = new HashSet<>();
        Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");
        ObservableList<String[]> items = table.getItems();
        for (String[] row : items) {
            for (String cell : row) {
                if (cell != null) {
                    Matcher matcher = pattern.matcher(cell);
                    while (matcher.find()) {
                        keys.add(matcher.group(1));
                    }
                }
            }
        }
        return keys;
    }
}