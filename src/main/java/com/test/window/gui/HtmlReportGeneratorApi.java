package com.test.window.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class HtmlReportGeneratorApi {

    private static final String BOOTSTRAP_CSS = """
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-9ndCyUaIbzAi2FUVXJi0CjmCapSmO7SnpJef0486qhLnuZ2cdeRhO02iuK6FUUVM" crossorigin="anonymous">
        """;

    private static final String BOOTSTRAP_JS = """
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js" integrity="sha384-geWF76RCwLtnZ8qwWowPQNguL3RmwHVBC9FhGdlKrxdiJJigb/j/68SIy3Te4Bkz" crossorigin="anonymous"></script>
        """;

    private static final String CSS_STYLE = """
        .report-container {
            max-width: 1400px;
            margin: 20px auto;
            padding: 20px;
            background-color: #FFFFFF;
        }
        table {
            font-size: 13.6px; /* Reduced by 15% from default 16px */
            transform: scale(0.85); /* Reduce table size by 15% */
            transform-origin: top left; /* Anchor scaling to top-left */
            background-color: #FFFFFF; /* Uniform background for all rows */
        }
        th, td {
            padding: 6.8px; /* Reduced by 15% from 8px */
        }
        .pass {
            color: green !important;
            font-weight: bold;
        }
        .fail {
            color: red !important;
            font-weight: bold;
        }
        .pass-bg {
            background-color: #D4EDDA; /* Light green for Pass status */
        }
        .fail-bg {
            background-color: #F8D7DA; /* Light red for Fail status */
        }
        pre {
            background-color: #FFFFFF;
            padding: 8.5px; /* Reduced by 15% from 10px */
            border-radius: 4px;
            overflow-x: auto;
            max-width: 100%;
            margin: 0; /* Ensure no extra margin affects hover */
        }
        .payload {
            color: #008000;
        }
        .response-body {
            color: #8B008B;
        }
        .not-available {
            color: #6c757d; /* Bootstrap secondary gray */
            font-style: italic;
            white-space: nowrap; /* Prevent text wrapping */
        }
        .failure-reason {
            min-width: 200px;
            word-wrap: break-word;
            overflow-wrap: break-word;
        }
        .summary-buttons .btn {
            margin: 0 5px;
        }
        """;

    public void generateReport(List<Map<String, Object>> reportDataList, ObjectMapper objectMapper) {
        // Calculate pass and fail counts
        int totalTests = reportDataList.size();
        int passCount = 0;
        int failCount = 0;
        for (Map<String, Object> reportData : reportDataList) {
            String status = safeToString(reportData.get("status"));
            if (status.equalsIgnoreCase("Pass")) {
                passCount++;
            } else if (status.equalsIgnoreCase("Fail")) {
                failCount++;
            }
        }

        // Get current date and time
        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss z");
        String dateTime = now.format(formatter);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='en'>\n");
        html.append("<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("<title>API Test Execution Report</title>\n");
        html.append(BOOTSTRAP_CSS);
        html.append("<style>\n").append(CSS_STYLE).append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class='report-container'>\n");
        html.append("<h1 class='display-5 text-center mb-4'>API Test Execution Report</h1>\n");
        html.append("<div class='text-center mb-3'>\n");
        html.append("<p>Generated on: ").append(escapeHtml(dateTime)).append("</p>\n");
        html.append("<div class='summary-buttons'>\n");
        html.append("Total Tests: <button class='btn btn-primary btn-sm' onclick='filterTests(\"all\")'>").append(totalTests).append("</button> | ");
        html.append("Passed: <button class='btn btn-success btn-sm' onclick='filterTests(\"pass\")'>").append(passCount).append("</button> | ");
        html.append("Failed: <button class='btn btn-danger btn-sm' onclick='filterTests(\"fail\")'>").append(failCount).append("</button>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        html.append("<table class='table table-hover table-bordered'>\n");
        html.append("<thead>\n");
        html.append("<tr>\n");
        html.append("<th>Test ID</th>\n");
        html.append("<th>Description</th>\n");
        html.append("<th>Status</th>\n");
        html.append("<th>Request</th>\n");
        html.append("<th>Endpoint</th>\n");
        html.append("<th>Payload</th>\n");
        html.append("<th>Headers</th>\n");
        html.append("<th>Parameters</th>\n");
        html.append("<th>Authentication</th>\n");
        html.append("<th>Response Status</th>\n");
        html.append("<th>Response Body</th>\n");
        html.append("<th>Verify Response</th>\n");
        html.append("<th>Failure Reason</th>\n");
        html.append("</tr>\n");
        html.append("</thead>\n");
        html.append("<tbody>\n");

        int rowIndex = 0;
        for (Map<String, Object> reportData : reportDataList) {
            String status = safeToString(reportData.get("status"));
            html.append("<tr id='row-").append(rowIndex).append("' data-status='").append(status.toLowerCase()).append("'>\n");
            html.append("<td>").append(safeToString(reportData.get("testId"))).append("</td>\n");
            html.append("<td>").append(safeToString(reportData.get("description"))).append("</td>\n");

            html.append("<td class='").append(status.equalsIgnoreCase("Pass") ? "pass" : status.equalsIgnoreCase("Fail") ? "fail" : "").append("'>")
                .append(status).append("</td>\n");

            html.append("<td>").append(safeToString(reportData.get("request"))).append("</td>\n");
            html.append("<td>").append(safeToString(reportData.get("endpoint"))).append("</td>\n");

            String payload = safeToString(reportData.get("payload"));
            if (payload == null || payload.trim().isEmpty()) {
                html.append("<td><span class='not-available'>Not Available</span></td>\n");
            } else {
                try {
                    Object json = objectMapper.readValue(payload, Object.class);
                    String prettyPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                    html.append("<td><pre class='payload'>").append(escapeHtml(prettyPayload)).append("</pre></td>\n");
                } catch (Exception e) {
                    html.append("<td><pre class='payload'>").append(escapeHtml(payload)).append("</pre></td>\n");
                }
            }

            String headersContent = formatMap(reportData.get("headers"), objectMapper, false);
            html.append("<td>").append(headersContent.startsWith("<span") ? headersContent : "<pre>" + headersContent + "</pre>").append("</td>\n");

            String parametersContent = formatMap(reportData.get("parameters"), objectMapper, false);
            html.append("<td>").append(parametersContent.startsWith("<span") ? parametersContent : "<pre>" + parametersContent + "</pre>").append("</td>\n");

            String authContent = formatMap(reportData.get("authentication"), objectMapper, true);
            html.append("<td>").append(authContent.startsWith("<span") ? authContent : "<pre>" + authContent + "</pre>").append("</td>\n");

            html.append("<td>").append(safeToString(reportData.get("responseStatus"))).append("</td>\n");

            String responseBody = safeToString(reportData.get("responseBody"));
            try {
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    Object json = objectMapper.readValue(responseBody, Object.class);
                    String prettyResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                    html.append("<td><pre class='response-body'>").append(escapeHtml(prettyResponse)).append("</pre></td>\n");
                } else {
                    html.append("<td><span class='not-available'>Not Available</span></td>\n");
                }
            } catch (Exception e) {
                html.append("<td><pre class='response-body'>").append(escapeHtml(responseBody)).append("</pre></td>\n");
            }

            String verifyResponse = safeToString(reportData.get("verifyResponse"));
            if (verifyResponse == null || verifyResponse.trim().isEmpty()) {
                html.append("<td><span class='not-available'>Not Available</span></td>\n");
            } else {
                try {
                    Object json = objectMapper.readValue(verifyResponse, Object.class);
                    String prettyVerifyResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                    html.append("<td><pre class='response-body'>").append(escapeHtml(prettyVerifyResponse)).append("</pre></td>\n");
                } catch (Exception e) {
                    html.append("<td><pre class='response-body'>").append(escapeHtml(verifyResponse)).append("</pre></td>\n");
                }
            }

            html.append("<td class='failure-reason'>").append(safeToString(reportData.get("failureReason"))).append("</td>\n");

            html.append("</tr>\n");
            rowIndex++;
        }

        html.append("</tbody>\n");
        html.append("</table>\n");
        html.append("<script>\n");
        html.append("function filterTests(filterType) {\n");
        html.append("  const rows = document.querySelectorAll('tbody tr');\n");
        html.append("  rows.forEach(row => {\n");
        html.append("    if (filterType === 'all') {\n");
        html.append("      row.style.display = '';\n");
        html.append("    } else {\n");
        html.append("      row.style.display = row.getAttribute('data-status') === filterType ? '' : 'none';\n");
        html.append("    }\n");
        html.append("  });\n");
        html.append("}\n");
        html.append("</script>\n");
        html.append(BOOTSTRAP_JS);
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>");

        try (FileWriter fileWriter = new FileWriter("report.html")) {
            fileWriter.write(html.toString());
        } catch (IOException e) {
            System.err.println("Error writing HTML report: " + e.getMessage());
        }
    }

    private String safeToString(Object obj) {
        return obj != null ? String.valueOf(obj).replace("<", "&lt;").replace(">", "&gt;") : "";
    }

    private String formatMap(Object mapObj, ObjectMapper objectMapper, boolean isAuthentication) {
        if (mapObj == null) {
            return "<span class='not-available'>Not Available</span>";
        }
        if (mapObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) mapObj;
            if (isAuthentication) {
                Object type = map.get("Type");
                if (type == null || "None".equalsIgnoreCase(String.valueOf(type))) {
                    return "<span class='not-available'>Not Available</span>";
                }
            }
            if (map.isEmpty()) {
                return "<span class='not-available'>Not Available</span>";
            }
            try {
                ObjectNode jsonNode = objectMapper.createObjectNode();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    jsonNode.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
                return escapeHtml(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
            } catch (Exception e) {
                return escapeHtml(String.valueOf(mapObj));
            }
        }
        try {
            Object json = objectMapper.readValue(String.valueOf(mapObj), Object.class);
            return escapeHtml(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
        } catch (Exception e) {
            return escapeHtml(String.valueOf(mapObj));
        }
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}