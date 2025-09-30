package com.test.window.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PostmanCollectionToExcel {
    // Class to hold extracted request details
    static class RequestInfo {
        String name;
        String url;
        Map<String, String> headers;
        Object body; // String for raw, Map for formdata/urlencoded, String "N/A" otherwise

        public RequestInfo(String name, String url, Map<String, String> headers, Object body) {
            this.name = name;
            this.url = url;
            this.headers = headers;
            this.body = body;
        }
    }

    public static void main(String[] args) {
    	
    	//args[0] = "C:\\Users\\zakir\\Downloads\\SignNow API.postman_collection.json";
        //if (args.length < 1) {
            //System.out.println("Please provide the path to the Postman collection JSON file.");
            //System.out.println("Optional: Provide output path as second argument (e.g., src/main/resources/output.xlsx)");
            //return;
        //}

        String filePath = "C:\\Users\\zakir\\Downloads\\SignNow API.postman_collection.json";
        //String filePath = "C:\\Users\\zakir\\Downloads\\SignNow API.postman_collection.json";
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

            // Extract URL (handle string or object)
            String url = "N/A";
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
                }
            }

            // Extract headers as a dictionary
            Map<String, String> headers = new HashMap<>();
            if (request.has("header")) {
                JSONArray headersArray = request.getJSONArray("header");
                for (int i = 0; i < headersArray.length(); i++) {
                    JSONObject header = headersArray.getJSONObject(i);
                    headers.put(header.getString("key"), header.getString("value"));
                }
            }

            // Extract body (handle raw as string, formdata/urlencoded as map)
            Object body = "N/A";
            if (request.has("body")) {
                JSONObject bodyObj = request.getJSONObject("body");
                String mode = bodyObj.optString("mode", "");
                if (mode.equals("raw")) {
                    body = bodyObj.optString("raw", "N/A");
                } else if (mode.equals("formdata") || mode.equals("urlencoded")) {
                    Map<String, String> bodyMap = new HashMap<>();
                    JSONArray bodyArray = bodyObj.optJSONArray(mode);
                    if (bodyArray != null) {
                        for (int i = 0; i < bodyArray.length(); i++) {
                            JSONObject param = bodyArray.getJSONObject(i);
                            bodyMap.put(param.getString("key"), param.optString("value", param.optString("src", "N/A")));
                        }
                    }
                    body = bodyMap;
                }
            }

            return new RequestInfo(fullName, url, headers, body);
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
        String[] columns = {"Number", "Name", "URL", "Headers", "Body"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Populate data rows
        for (int i = 0; i < requests.size(); i++) {
            RequestInfo request = requests.get(i);
            Row row = sheet.createRow(i + 1);

            // Number
            row.createCell(0).setCellValue(i + 1);

            // Name
            row.createCell(1).setCellValue(request.name);

            // URL
            row.createCell(2).setCellValue(request.url);

            // Headers
            row.createCell(3).setCellValue(request.headers.toString());

            // Body
            String bodyStr = request.body instanceof Map ? request.body.toString() : String.valueOf(request.body);
            row.createCell(4).setCellValue(bodyStr);
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