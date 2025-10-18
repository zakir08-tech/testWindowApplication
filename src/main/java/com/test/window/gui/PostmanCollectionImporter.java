package com.test.window.gui;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redesigned class to parse Postman collection JSON files and convert them into
 * table data as List<String[]> for UI table population. This class handles
 * nested folders, extracts requests, headers, query params, body, and maps them
 * to the expected column structure.
 */
public class PostmanCollectionImporter {
    /**
     * Inner class to hold extracted request details from Postman collection.
     * Stores method, URL, headers, query params, body content, and authorization info.
     */
    static class RequestInfo {
        String name;                    // Request or folder name
        String url;                     // Full URL of the request
        String method;                  // HTTP method (e.g., GET, POST)
        Map<String, String> headers;    // Map of header key-value pairs
        Map<String, String> queryParams; // Map of query parameter key-value pairs
        Object body;                    // Body content: String for raw, Map for formdata/urlencoded, empty String otherwise
        String bodyType;                // Type of the payload body (e.g., "json", "formdata", "urlencoded", "")
        String authorization;           // Authorization header value or empty string

        /**
         * Constructor for RequestInfo.
         *
         * @param name          the name of the request
         * @param url           the URL
         * @param method        the HTTP method
         * @param headers       the headers map
         * @param queryParams   the query params map
         * @param body          the body object
         * @param bodyType      the body type
         * @param authorization the authorization value
         */
        public RequestInfo(String name, String url, String method, Map<String, String> headers,
                           Map<String, String> queryParams, Object body, String bodyType, String authorization) {
            this.name = name;
            this.url = url;
            this.method = method;
            this.headers = headers;
            this.queryParams = queryParams;
            this.body = body;
            this.bodyType = bodyType;
            this.authorization = authorization;
        }
    }

    /**
     * Main entry method to import a Postman collection from a JSON file and return
     * a list of String arrays representing table rows.
     *
     * @param file the Postman collection JSON file
     * @return List of String[] for table rows
     * @throws IOException   if file reading fails
     * @throws JSONException if JSON parsing fails
     */
    public static List<String[]> importFromFile(File file) throws IOException, JSONException {
        // Validate input file
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Invalid file: " + (file != null ? file.getAbsolutePath() : "null"));
        }

        // Read file content
        String content;
        try {
            content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            throw new IOException("Failed to read file: " + file.getAbsolutePath(), e);
        }

        // Parse JSON collection
        JSONObject collection;
        try {
            collection = new JSONObject(content);
        } catch (JSONException e) {
            System.err.println("Invalid JSON in file: " + e.getMessage());
            throw new JSONException("Failed to parse JSON from file: " + file.getName(), e);
        }

        // Extract requests from collection
        List<RequestInfo> requests = extractFromCollection(collection);

        // Convert to table rows
        List<String[]> rows = convertToTableRows(requests);

        // Validate Test IDs format (digits, 3-5 length starting from 100; duplicates allowed for multi-row requests)
        if (!validateTestIds(rows)) {
            throw new IllegalArgumentException("Generated Test IDs are invalid. Please check the collection structure.");
        }

        return rows;
    }

    /**
     * Validates Test IDs in the rows: non-empty ones must be digits only, length 3-5 (to ensure >=100 and <=99999).
     * Blanks are allowed in multi-row groups.
     *
     * @param rows the table rows
     * @return true if all non-empty Test IDs are valid in format
     */
    private static boolean validateTestIds(List<String[]> rows) {
        if (rows.isEmpty()) {
            return true;
        }
        for (String[] row : rows) {
            String testId = row[0];  // Column 0: Test ID
            if (!testId.isEmpty() && !testId.matches("\\d{3,5}")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extracts all RequestInfo objects from the collection JSON, handling nested folders.
     *
     * @param collection the root JSON object of the collection
     * @return list of RequestInfo
     */
    private static List<RequestInfo> extractFromCollection(JSONObject collection) {
        List<RequestInfo> requests = new ArrayList<>();
        recurseItems(collection, requests, ""); // Start recursion from root with empty parent name
        return requests;
    }

    /**
     * Recursively traverses the collection items (requests and folders) to extract requests.
     * Handles nested folders by building full names.
     *
     * @param collectionObj the current JSON object (collection or folder)
     * @param requests      the list to add extracted requests to
     * @param parentName    the parent folder path for naming
     */
    private static void recurseItems(JSONObject collectionObj, List<RequestInfo> requests, String parentName) {
        // Get the items array (can be at root or nested folders)
        JSONArray items = collectionObj.optJSONArray("item");
        if (items == null) {
            return; // No items to process
        }

        for (int i = 0; i < items.length(); i++) {
            try {
                Object item = items.get(i);
                if (!(item instanceof JSONObject)) {
                    continue; // Skip non-object items
                }

                JSONObject itemObj = (JSONObject) item;
                String currentName = itemObj.optString("name", "Unnamed");
                String fullName = parentName.isEmpty() ? currentName : parentName + " > " + currentName;

                // If item has a request, extract and add it
                if (itemObj.has("request")) {
                    RequestInfo requestInfo = extractRequest(itemObj, fullName);
                    if (requestInfo != null) {
                        requests.add(requestInfo);
                    }
                }

                // Recurse into nested items (for sub-folders)
                recurseItems(itemObj, requests, fullName);
            } catch (JSONException e) {
                System.err.println("Error processing item at index " + i + ": " + e.getMessage());
                // Continue with next item
            }
        }
    }

    /**
     * Extracts detailed request information from a Postman item JSON.
     *
     * @param item      the JSON object for the item
     * @param fullName  the full name including parent folders
     * @return RequestInfo object, or null if extraction fails
     */
    private static RequestInfo extractRequest(JSONObject item, String fullName) {
        try {
            // Validate presence of request
            if (!item.has("request")) {
                return null;
            }

            JSONObject request = item.getJSONObject("request");

            // Extract HTTP method (default to empty if missing)
            String method = request.optString("method", "");

            // Extract URL and query parameters (handle both string and object formats)
            String url = "";
            Map<String, String> queryParams = new HashMap<>();
            if (request.has("url")) {
                Object urlField = request.get("url");
                if (urlField instanceof String) {
                    url = (String) urlField;
                } else if (urlField instanceof JSONObject) {
                    JSONObject urlObj = (JSONObject) urlField;
                    if (urlObj.has("raw")) {
                        url = urlObj.getString("raw");
                    } else {
                        // Fallback: compose URL from components
                        StringBuilder urlBuilder = new StringBuilder();
                        if (urlObj.has("protocol")) {
                            urlBuilder.append(urlObj.getString("protocol")).append("://");
                        }
                        if (urlObj.has("host")) {
                            try {
                                Object host = urlObj.get("host");
                                if (host instanceof JSONArray) {
                                    List<Object> hostList = ((JSONArray) host).toList();
                                    List<String> hostStrings = hostList.stream()
                                            .map(Object::toString)
                                            .collect(Collectors.toList());
                                    urlBuilder.append(String.join(".", hostStrings));
                                } else {
                                    urlBuilder.append(host.toString());
                                }
                            } catch (JSONException e) {
                                System.err.println("Error parsing host: " + e.getMessage());
                            }
                        }
                        if (urlObj.has("path")) {
                            try {
                                Object path = urlObj.get("path");
                                if (path instanceof JSONArray) {
                                    List<Object> pathList = ((JSONArray) path).toList();
                                    List<String> pathStrings = pathList.stream()
                                            .map(Object::toString)
                                            .collect(Collectors.toList());
                                    urlBuilder.append("/").append(String.join("/", pathStrings));
                                } else {
                                    urlBuilder.append("/").append(path.toString());
                                }
                            } catch (JSONException e) {
                                System.err.println("Error parsing path: " + e.getMessage());
                            }
                        }
                        url = urlBuilder.toString();
                    }
                    // Extract query parameters if present
                    if (urlObj.has("query")) {
                        try {
                            JSONArray queryArray = urlObj.getJSONArray("query");
                            for (int i = 0; i < queryArray.length(); i++) {
                                JSONObject param = queryArray.getJSONObject(i);
                                String key = param.getString("key");
                                String value = param.optString("value", "");
                                queryParams.put(key, value);
                            }
                        } catch (JSONException e) {
                            System.err.println("Error parsing query params: " + e.getMessage());
                        }
                    }
                } else {
                    System.err.println("Unexpected URL format: " + urlField.getClass().getSimpleName());
                }
            }

            // Extract headers as a map, and capture authorization if present
            Map<String, String> headers = new HashMap<>();
            String authorization = "";
            if (request.has("header")) {
                try {
                    JSONArray headersArray = request.getJSONArray("header");
                    for (int i = 0; i < headersArray.length(); i++) {
                        JSONObject header = headersArray.getJSONObject(i);
                        String key = header.getString("key");
                        String value = header.getString("value");
                        headers.put(key, value);
                        if ("Authorization".equalsIgnoreCase(key)) {
                            authorization = value;
                        }
                    }
                } catch (JSONException e) {
                    System.err.println("Error parsing headers: " + e.getMessage());
                }
            }

            // Extract body content and determine type
            Object body = "";
            String bodyType = "";
            if (request.has("body")) {
                try {
                    JSONObject bodyObj = request.getJSONObject("body");
                    bodyType = bodyObj.optString("mode", "");
                    // Determine if raw body is JSON based on Content-Type header
                    String contentType = headers.getOrDefault("Content-Type", "").toLowerCase();
                    if (bodyType.equals("raw") && contentType.contains("json")) {
                        bodyType = "json";
                        body = bodyObj.optString("raw", "");
                    } else if (bodyType.equals("raw")) {
                        body = bodyObj.optString("raw", "");
                    } else if (bodyType.equals("formdata") || bodyType.equals("urlencoded")) {
                        Map<String, String> bodyMap = new HashMap<>();
                        JSONArray bodyArray = bodyObj.optJSONArray(bodyType);
                        if (bodyArray != null) {
                            for (int i = 0; i < bodyArray.length(); i++) {
                                JSONObject param = bodyArray.getJSONObject(i);
                                String key = param.getString("key");
                                String value = param.optString("value", param.optString("src", ""));
                                bodyMap.put(key, value);
                            }
                        }
                        body = bodyMap;
                    }
                } catch (JSONException e) {
                    System.err.println("Error parsing body: " + e.getMessage());
                }
            }

            return new RequestInfo(fullName, url, method, headers, queryParams, body, bodyType, authorization);
        } catch (JSONException e) {
            System.err.println("JSONException in extractRequest for '" + fullName + "': " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error extracting request '" + fullName + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts a list of RequestInfo to table rows (List<String[]>), pairing
     * headers and parameters in the same rows where possible. Uses Math.max for total rows
     * to align headers and params side-by-side.
     *
     * @param requests the list of extracted requests
     * @return list of String[] representing table rows
     */
    private static List<String[]> convertToTableRows(List<RequestInfo> requests) {
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>(); // Return empty list if no requests
        }

        List<String[]> rows = new ArrayList<>();
        String[] columns = {
            "Test ID", "Request", "End-Point", "Header (key)", "Header (value)",
            "Parameter (key)", "Parameter (value)", "Payload", "Payload Type",
            "Response (key) Name", "Capture (key) Value (env var)", "Authorization", "", "",
            "SSL Validation", "Expected Status", "Verify Response", "Test Description"
        };

        // Initialize counter for sequential Test IDs starting from 100
        int currentTestId = 100;

        // Process each request
        for (RequestInfo request : requests) {
            if (request == null) {
                continue; // Skip null requests
            }

            // Get counts for headers and params
            int headerCount = request.headers != null ? request.headers.size() : 0;
            int paramCount = request.queryParams != null ? request.queryParams.size() : 0;
            int totalRows = Math.max(1, Math.max(headerCount, paramCount)); // At least one row, pair by max count

            // Use sequential Test ID for this request (ensures no duplicates across requests)
            int testId = currentTestId++;

            // Track indices for headers and params
            int headerIndex = 0;
            int paramIndex = 0;

            // Pre-compute shared values
            String method = request.method != null ? request.method : "";
            String url = request.url != null ? request.url : "";
            String bodyStr = "";
            if ("urlencoded".equals(request.bodyType) && request.body instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> bodyMap = (Map<String, String>) request.body;
                if (bodyMap != null && !bodyMap.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    int k = 0;
                    for (Map.Entry<String, String> entry : bodyMap.entrySet()) {
                        sb.append(entry.getKey()).append(":").append(entry.getValue());
                        if (k < bodyMap.size() - 1) {
                            sb.append("\n");
                        }
                        k++;
                    }
                    bodyStr = sb.toString();
                }
            } else if ("json".equals(request.bodyType) && request.body instanceof String) {
                try {
                    String rawBody = (String) request.body;
                    if (rawBody != null && !rawBody.isEmpty()) {
                        // Try parsing as JSONObject or JSONArray for pretty-print
                        try {
                            JSONObject jsonObject = new JSONObject(rawBody);
                            bodyStr = jsonObject.toString(2); // Pretty-print with indent of 2
                        } catch (JSONException e1) {
                            try {
                                JSONArray jsonArray = new JSONArray(rawBody);
                                bodyStr = jsonArray.toString(2);
                            } catch (JSONException e2) {
                                bodyStr = rawBody; // Fallback to raw string if not valid JSON
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error formatting JSON body: " + e.getMessage());
                    bodyStr = String.valueOf(request.body); // Fallback
                }
            } else {
                bodyStr = request.body instanceof Map ? request.body.toString() : String.valueOf(request.body);
            }
            String bodyType = request.bodyType != null ? mapBodyType(request.bodyType) : "";
            String name = request.name != null ? request.name : "";
            boolean hasAuthorizationHeader = request.headers != null &&
                    request.headers.keySet().stream().anyMatch(key -> key.equalsIgnoreCase("Authorization"));
            String authValue = hasAuthorizationHeader ? "" : (request.authorization != null ? request.authorization : "");

            for (int j = 0; j < totalRows; j++) {
                String[] row = new String[columns.length];
                // Initialize all cells to empty string
                for (int col = 0; col < columns.length; col++) {
                    row[col] = "";
                }

                // Populate shared fields only on first row (j == 0) to avoid repetition
                if (j == 0) {
                    row[0] = String.valueOf(testId); // Test ID
                    row[1] = method; // Request
                    row[2] = url; // End-Point
                    row[7] = bodyStr; // Payload
                    row[8] = bodyType; // Payload Type
                    row[9] = ""; // Response (key) Name
                    row[10] = ""; // Capture (key) Value (env var)
                    row[11] = authValue; // Authorization
                    row[12] = ""; // Empty
                    row[13] = ""; // Empty
                    row[14] = ""; // SSL Validation
                    row[15] = "200"; // Expected Status (default)
                    row[16] = ""; // Verify Response
                    row[17] = name; // Test Description
                } else {
                    // For subsequent rows, leave shared fields blank
                    row[0] = ""; // Test ID blank
                    row[1] = ""; // Request blank
                    row[2] = ""; // End-Point blank
                    row[7] = ""; // Payload blank
                    row[8] = ""; // Payload Type blank
                    row[9] = ""; // Response (key) Name
                    row[10] = ""; // Capture (key) Value (env var)
                    row[11] = ""; // Authorization blank
                    row[12] = ""; // Empty
                    row[13] = ""; // Empty
                    row[14] = ""; // SSL Validation
                    row[15] = ""; // Expected Status blank
                    row[16] = ""; // Verify Response
                    row[17] = ""; // Test Description blank
                }

                // Set header if available for this row index
                if (headerIndex < headerCount && request.headers != null) {
                    try {
                        Map.Entry<String, String>[] headerArray = request.headers.entrySet().toArray(new Map.Entry[0]);
                        if (headerIndex < headerArray.length) {
                            Map.Entry<String, String> header = headerArray[headerIndex];
                            row[3] = header.getKey(); // Header (key)
                            row[4] = header.getValue(); // Header (value)
                            headerIndex++;
                        }
                    } catch (Exception e) {
                        System.err.println("Error setting header at index " + headerIndex + ": " + e.getMessage());
                    }
                }

                // Set parameter if available for this row index
                if (paramIndex < paramCount && request.queryParams != null) {
                    try {
                        Map.Entry<String, String>[] paramArray = request.queryParams.entrySet().toArray(new Map.Entry[0]);
                        if (paramIndex < paramArray.length) {
                            Map.Entry<String, String> param = paramArray[paramIndex];
                            row[5] = param.getKey(); // Parameter (key)
                            row[6] = param.getValue(); // Parameter (value)
                            paramIndex++;
                        }
                    } catch (Exception e) {
                        System.err.println("Error setting param at index " + paramIndex + ": " + e.getMessage());
                    }
                }

                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Maps the raw body type from Postman to the desired display format.
     *
     * @param rawBodyType the raw body type from the Postman collection
     * @return the display-friendly body type
     */
    private static String mapBodyType(String rawBodyType) {
        if ("json".equalsIgnoreCase(rawBodyType)) {
            return "JSON";
        } else if ("formdata".equalsIgnoreCase(rawBodyType)) {
            return "form-data";
        }
        return rawBodyType; // Return as-is for other types (e.g., "urlencoded", "")
    }
}