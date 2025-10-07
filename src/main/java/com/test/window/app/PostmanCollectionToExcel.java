package com.test.window.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class PostmanCollectionToExcel {
    // Class to hold extracted request details
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

    public static void main(String[] args) {
        String filePath = "C:\\Users\\zakir\\Downloads\\SignNow API.postman_collection.json";
        // Default to src/main/resources, or use second argument if provided
        String outputPath = args.length > 1 ? args[1] : "src/main/resources/PostmanCollectionOutput.xlsx";

        // Declare content outside try block
        String content = null;
        try {
            // Print current working directory for debugging
            System.out.println("Current working directory: " + System.getProperty("user.dir"));
            File outputFile = new File(outputPath);
            System.out.println("Attempting to save Excel file to: " + outputFile.getAbsolutePath());

            // Create output directory if it doesn't exist
            File outputDir = outputFile.getParentFile();
            if (outputDir != null && !outputDir.exists()) {
                System.out.println("Creating directory: " + outputDir.getAbsolutePath());
                if (!outputDir.mkdirs()) {
                    System.err.println("Failed to create directory: " + outputDir.getAbsolutePath());
                }
            }

            // Read the JSON file
            content = new String(Files.readAllBytes(Paths.get(filePath)));
            JSONObject collection = new JSONObject(content);

            // Extract requests
            List<RequestInfo> requests = extractFromCollection(collection);

            // Save to Excel
            saveToExcel(requests, outputFile.getAbsolutePath());

            System.out.println("Output saved to " + outputFile.getAbsolutePath());
            System.out.println("Extracted " + requests.size() + " requests.");
        } catch (IOException e) {
            System.err.println("Error reading file or writing Excel to " + outputPath + ": " + e.getMessage());
            e.printStackTrace();
            // Fallback to user home directory if content was read successfully
            if (content != null) {
                String fallbackPath = System.getProperty("user.home") + File.separator + "PostmanCollectionOutput.xlsx";
                System.out.println("Attempting fallback save to: " + fallbackPath);
                try {
                    saveToExcel(extractFromCollection(new JSONObject(content)), fallbackPath);
                    System.out.println("Output saved to fallback: " + fallbackPath);
                } catch (IOException fallbackEx) {
                    System.err.println("Fallback save failed: " + fallbackEx.getMessage());
                    fallbackEx.printStackTrace();
                } catch (Exception fallbackEx) {
                    System.err.println("Error processing collection in fallback: " + fallbackEx.getMessage());
                    fallbackEx.printStackTrace();
                }
            } else {
                System.err.println("Cannot attempt fallback save: JSON file could not be read.");
            }
        } catch (Exception e) {
            System.err.println("Error processing collection: " + e.getMessage());
            e.printStackTrace();
        }
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

    private static void saveToExcel(List<RequestInfo> requests, String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("API Requests");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] columns = {
            "Test ID", "Request", "End-Point", "Header (key)", "Header (value)",
            "Parameter (key)", "Parameter (value)", "Payload", "Payload Type",
            "Modify Payload (key)", "Modify Payload (value)", "Response (key) Name",
            "Capture (key) Value (env var)", "Authorization", "", "",
            "SSL Validation", "Expected Status", "Verify Response", "Test Description"
        };
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Create cell style for integer format
        CellStyle integerStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        integerStyle.setDataFormat(format.getFormat("0"));

        // Initialize counter for sequential Test IDs starting from 100
        int currentTestId = 100;

        // Populate data rows
        int rowNum = 1;
        for (int i = 0; i < requests.size(); i++) {
            RequestInfo request = requests.get(i);
            int headerCount = request.headers.size();
            int paramCount = request.queryParams.size();
            int totalRows = Math.max(1, headerCount + paramCount); // At least one row, plus rows for headers and params

            // Use sequential Test ID
            int testId = currentTestId++;

            // Track headers and params used
            int headerIndex = 0;
            int paramIndex = 0;

            for (int j = 0; j < totalRows; j++) {
                Row row = sheet.createRow(rowNum++);

                // First row: populate all shared fields and first header/parameter if available
                if (j == 0) {
                    // Test ID (numeric, whole number)
                    Cell testIdCell = row.createCell(0);
                    testIdCell.setCellValue(testId);
                    testIdCell.setCellStyle(integerStyle);

                    // Request
                    row.createCell(1).setCellValue(request.method);

                    // End-Point
                    row.createCell(2).setCellValue(request.url);

                    // Header (key) and Header (value)
                    if (headerIndex < headerCount) {
                        Map.Entry<String, String> header = request.headers.entrySet().toArray(new Map.Entry[0])[headerIndex];
                        row.createCell(3).setCellValue(header.getKey());
                        row.createCell(4).setCellValue(header.getValue());
                        headerIndex++;
                    } else {
                        row.createCell(3).setCellValue("");
                        row.createCell(4).setCellValue("");
                    }

                    // Parameter (key) and Parameter (value)
                    if (paramIndex < paramCount && headerIndex >= headerCount) {
                        Map.Entry<String, String> param = request.queryParams.entrySet().toArray(new Map.Entry[0])[paramIndex];
                        row.createCell(5).setCellValue(param.getKey());
                        row.createCell(6).setCellValue(param.getValue());
                        paramIndex++;
                    } else {
                        row.createCell(5).setCellValue("");
                        row.createCell(6).setCellValue("");
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
                    row.createCell(7).setCellValue(bodyStr);

                    // Payload Type
                    row.createCell(8).setCellValue(request.bodyType);

                    // Modify Payload (key), Modify Payload (value)
                    row.createCell(9).setCellValue("");
                    row.createCell(10).setCellValue("");

                    // Response (key) Name, Capture (key) Value (env var)
                    row.createCell(11).setCellValue("");
                    row.createCell(12).setCellValue("");

                    // Authorization (set to "" if Authorization header exists, else use request.authorization)
                    boolean hasAuthorizationHeader = request.headers.keySet().stream()
                            .anyMatch(key -> key.equalsIgnoreCase("Authorization"));
                    row.createCell(13).setCellValue(hasAuthorizationHeader ? "" : request.authorization);

                    // Empty columns
                    row.createCell(14).setCellValue("");
                    row.createCell(15).setCellValue("");

                    // SSL Validation
                    row.createCell(16).setCellValue("");

                    // Expected Status (default to 200)
                    row.createCell(17).setCellValue("200");

                    // Verify Response
                    row.createCell(18).setCellValue("");

                    // Test Description
                    row.createCell(19).setCellValue(request.name);

                    // Auto-adjust row height based on content
                    if (bodyStr.contains("\n") || request.url.contains("\n") || request.name.contains("\n")) {
                        int lineCount = Math.max(1, bodyStr.split("\n").length);
                        lineCount = Math.max(lineCount, request.url.split("\n").length);
                        lineCount = Math.max(lineCount, request.name.split("\n").length);
                        // Estimate height: 15 points per line (default font size ~12pt + padding)
                        row.setHeight((short) (lineCount * 15 * 20)); // Convert to twips (1 pt = 20 twips)
                    } else {
                        row.setHeight((short) -1); // Auto-height for single-line content
                    }
                } else {
                    // Additional rows for headers
                    if (headerIndex < headerCount) {
                        Map.Entry<String, String> header = request.headers.entrySet().toArray(new Map.Entry[0])[headerIndex];
                        row.createCell(3).setCellValue(header.getKey());
                        row.createCell(4).setCellValue(header.getValue());
                        headerIndex++;
                        // Auto-adjust row height for header value
                        if (header.getValue().contains("\n")) {
                            int lineCount = header.getValue().split("\n").length;
                            row.setHeight((short) (lineCount * 15 * 20));
                        } else {
                            row.setHeight((short) -1);
                        }
                    }
                    // Additional rows for parameters (after headers are exhausted)
                    else if (paramIndex < paramCount) {
                        Map.Entry<String, String> param = request.queryParams.entrySet().toArray(new Map.Entry[0])[paramIndex];
                        row.createCell(5).setCellValue(param.getKey());
                        row.createCell(6).setCellValue(param.getValue());
                        paramIndex++;
                        // Auto-adjust row height for parameter value
                        if (param.getValue().contains("\n")) {
                            int lineCount = param.getValue().split("\n").length;
                            row.setHeight((short) (lineCount * 15 * 20));
                        } else {
                            row.setHeight((short) -1);
                        }
                    }
                    // All other columns remain empty
                    for (int col = 0; col < columns.length; col++) {
                        if (col != 3 && col != 4 && col != 5 && col != 6) {
                            row.createCell(col).setCellValue("");
                        }
                    }
                }
            }
        }

        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            System.err.println("Failed to write Excel file to " + filePath + ": " + e.getMessage());
            throw e;
        }
        workbook.close();
    }
}