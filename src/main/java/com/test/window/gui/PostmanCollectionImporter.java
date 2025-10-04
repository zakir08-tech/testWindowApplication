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

// Redesigned class to parse Postman collection and return table data as List<String[]> for UI table population
public class PostmanCollectionImporter {
    // Class to hold extracted request details (same as original)
    static class RequestInfo {
        String name;
        String url;
        String method;
        Map<String, String> headers;
        Map<String, String> queryParams;
        Object body; // String for raw, Map for formdata/urlencoded, String "" otherwise
        String bodyType; // Type of the payload body (e.g., json, formdata, urlencoded, "")
        String authorization; // Authorization header value or ""

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

    // Main method to import from file and return List<String[]> for table rows
    public static List<String[]> importFromFile(File file) throws IOException, JSONException {
        String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
        JSONObject collection = new JSONObject(content);

        // Extract requests
        List<RequestInfo> requests = extractFromCollection(collection);

        // Convert to table rows
        return convertToTableRows(requests);
    }

    private static List<RequestInfo> extractFromCollection(JSONObject collection) {
        List<RequestInfo> requests = new ArrayList<>();
        recurseItems(collection, requests, "");
        return requests;
    }

    private static void recurseItems(JSONObject collection, List<RequestInfo> requests, String parentName) {
        // Get the items array (can be at root or nested)
        JSONArray items = collection.optJSONArray("item");
        if (items == null) return;

        for (int i = 0; i < items.length(); i++) {
            Object item = items.get(i);
            if (item instanceof JSONObject) {
                JSONObject itemObj = (JSONObject) item;
                String currentName = itemObj.optString("name", "Unnamed");
                String fullName = parentName.isEmpty() ? currentName : parentName + " > " + currentName;

                // If item has a request, process it
                if (itemObj.has("request")) {
                    RequestInfo requestInfo = extractRequest(itemObj, fullName);
                    if (requestInfo != null) {
                        requests.add(requestInfo);
                    }
                }

                // Recurse into nested items (for folders)
                recurseItems(itemObj, requests, fullName);
            }
        }
    }

    private static RequestInfo extractRequest(JSONObject item, String fullName) {
        try {
            JSONObject request = item.getJSONObject("request");

            // Extract method
            String method = request.optString("method", "");

            // Extract URL (handle string or object)
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
                        // Fallback to composing URL
                        StringBuilder urlBuilder = new StringBuilder();
                        if (urlObj.has("protocol")) {
                            urlBuilder.append(urlObj.getString("protocol")).append("://");
                        }
                        if (urlObj.has("host")) {
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
                        }
                        if (urlObj.has("path")) {
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
                        }
                        url = urlBuilder.toString();
                    }
                    // Extract query parameters
                    if (urlObj.has("query")) {
                        JSONArray queryArray = urlObj.getJSONArray("query");
                        for (int i = 0; i < queryArray.length(); i++) {
                            JSONObject param = queryArray.getJSONObject(i);
                            queryParams.put(param.getString("key"), param.optString("value", ""));
                        }
                    }
                }
            }

            // Extract headers as a dictionary
            Map<String, String> headers = new HashMap<>();
            String authorization = "";
            if (request.has("header")) {
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
            }

            // Extract body and body type
            Object body = "";
            String bodyType = "";
            if (request.has("body")) {
                JSONObject bodyObj = request.getJSONObject("body");
                bodyType = bodyObj.optString("mode", "");
                // Determine if raw body is JSON based on Content-Type
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
                            bodyMap.put(param.getString("key"), param.optString("value", param.optString("src", "")));
                        }
                    }
                    body = bodyMap;
                }
            }

            return new RequestInfo(fullName, url, method, headers, queryParams, body, bodyType, authorization);
        } catch (Exception e) {
            System.err.println("Error extracting request '" + fullName + "': " + e.getMessage());
            return null;
        }
    }

    private static List<String[]> convertToTableRows(List<RequestInfo> requests) {
        List<String[]> rows = new ArrayList<>();
        String[] columns = {
            "Test ID", "Request", "End-Point", "Header (key)", "Header (value)",
            "Parameter (key)", "Parameter (value)", "Payload", "Payload Type",
            "Modify Payload (key)", "Modify Payload (value)", "Response (key) Name",
            "Capture (key) Value (env var)", "Authorization", "", "",
            "SSL Validation", "Expected Status", "Verify Response", "Test Description"
        };

        // Initialize counter for sequential Test IDs starting from 100
        int currentTestId = 100;

        // Populate data rows
        for (RequestInfo request : requests) {
            int headerCount = request.headers.size();
            int paramCount = request.queryParams.size();
            int totalRows = Math.max(1, headerCount + paramCount); // At least one row, plus rows for headers and params

            // Use sequential Test ID
            int testId = currentTestId++;

            // Track headers and params used
            int headerIndex = 0;
            int paramIndex = 0;

            for (int j = 0; j < totalRows; j++) {
                String[] row = new String[columns.length];

                // First row: populate all shared fields and first header/parameter if available
                if (j == 0) {
                    // Test ID
                    row[0] = String.valueOf(testId);

                    // Request
                    row[1] = request.method;

                    // End-Point
                    row[2] = request.url;

                    // Header (key) and Header (value)
                    if (headerIndex < headerCount) {
                        Map.Entry<String, String> header = request.headers.entrySet().toArray(new Map.Entry[0])[headerIndex];
                        row[3] = header.getKey();
                        row[4] = header.getValue();
                        headerIndex++;
                    } else {
                        row[3] = "";
                        row[4] = "";
                    }

                    // Parameter (key) and Parameter (value)
                    if (paramIndex < paramCount && headerIndex >= headerCount) {
                        Map.Entry<String, String> param = request.queryParams.entrySet().toArray(new Map.Entry[0])[paramIndex];
                        row[5] = param.getKey();
                        row[6] = param.getValue();
                        paramIndex++;
                    } else {
                        row[5] = "";
                        row[6] = "";
                    }

                    // Payload
                    String bodyStr;
                    if ("urlencoded".equals(request.bodyType) && request.body instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> bodyMap = (Map<String, String>) request.body;
                        if (bodyMap.isEmpty()) {
                            bodyStr = "";
                        } else {
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
                            if (rawBody.isEmpty()) {
                                bodyStr = "";
                            } else {
                                // Try parsing as JSONObject or JSONArray
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
                            bodyStr = String.valueOf(request.body); // Fallback to raw string
                        }
                    } else {
                        bodyStr = request.body instanceof Map ? request.body.toString() : String.valueOf(request.body);
                    }
                    row[7] = bodyStr;

                    // Payload Type
                    row[8] = request.bodyType;

                    // Modify Payload (key), Modify Payload (value)
                    row[9] = "";
                    row[10] = "";

                    // Response (key) Name, Capture (key) Value (env var)
                    row[11] = "";
                    row[12] = "";

                    // Authorization (set to "" if Authorization header exists, else use request.authorization)
                    boolean hasAuthorizationHeader = request.headers.keySet().stream()
                            .anyMatch(key -> key.equalsIgnoreCase("Authorization"));
                    row[13] = hasAuthorizationHeader ? "" : request.authorization;

                    // Empty columns
                    row[14] = "";
                    row[15] = "";

                    // SSL Validation
                    row[16] = "";

                    // Expected Status (default to 200)
                    row[17] = "200";

                    // Verify Response
                    row[18] = "";

                    // Test Description
                    row[19] = request.name;
                } else {
                    // Additional rows for headers
                    if (headerIndex < headerCount) {
                        Map.Entry<String, String> header = request.headers.entrySet().toArray(new Map.Entry[0])[headerIndex];
                        row[3] = header.getKey();
                        row[4] = header.getValue();
                        headerIndex++;
                    }
                    // Additional rows for parameters (after headers are exhausted)
                    else if (paramIndex < paramCount) {
                        Map.Entry<String, String> param = request.queryParams.entrySet().toArray(new Map.Entry[0])[paramIndex];
                        row[5] = param.getKey();
                        row[6] = param.getValue();
                        paramIndex++;
                    }
                    // All other columns remain empty
                    for (int col = 0; col < columns.length; col++) {
                        if (col != 3 && col != 4 && col != 5 && col != 6) {
                            row[col] = "";
                        }
                    }
                }
                rows.add(row);
            }
        }
        return rows;
    }
}