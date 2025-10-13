package com.test.window.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HtmlReportGeneratorApi {

    private static final String BOOTSTRAP_CSS = """
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07v4J8+LH7qKnuqkuIAvNWLzeN8tE5YBujZqJLB" crossorigin="anonymous">
        """;

    private static final String BOOTSTRAP_ICONS_CSS = """
        <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.13.1/font/bootstrap-icons.min.css" rel="stylesheet">
        """;

    private static final String BOOTSTRAP_JS = """
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js" integrity="sha384-FKyoEForCGlyvwx9Hj09JcYn3nv7wiPVlz7YYwJrWVcXK/BmnVDxM+D2scQbITxI" crossorigin="anonymous"></script>
        """;

    private static final String CSS_STYLE = """
         body {
             margin: 0;
             font-family: Arial, sans-serif;
             background-color: #FFFFFF;
             color: #000000;
         }
         .report-container {
             max-width: 1400px;
             margin: 20px auto;
             padding: 20px;
             background-color: #FFFFFF;
             color: #000000;
             min-height: 100vh;
         }
         .report-container h1 {
             font-size: 1.5rem;
             text-align: center;
         }
         .table-container {
             overflow-x: auto;
             overflow-y: auto;
             max-height: calc(70vh - 50px);
             min-height: 500px;
             max-width: 1400px;
             width: 100%;
             position: relative;
             border: 2px solid #dee2e6;
             background-color: #F8F9FA;
             margin-top: 20px;
         }
         table {
             table-layout: auto;
             background-color: #FFFFFF;
             width: 100%;
             font-size: 10px;
             border-collapse: collapse;
         }
         th, td {
             padding: 8px;
             border: 1px solid #dee2e6;
             color: #000000;
             text-align: left;
             vertical-align: top;
         }
         td {
             white-space: nowrap;
         }
         table th {
             background-color: #0D6EFD !important;
             color: #FFFFFF !important;
             position: sticky;
             top: 0;
             z-index: 2;
             white-space: nowrap;
         }
         #testReportTable td:nth-child(3) {
             text-align: left !important;
             vertical-align: top !important;
         }
         #testReportTable th:nth-child(6),
         #testReportTable td:nth-child(6) {
             min-width: 250px;
         }
         #testReportTable th:nth-child(12),
         #testReportTable td:nth-child(12) {
             min-width: 350px;
         }
         #testReportTable th:nth-child(11),
         #testReportTable td:nth-child(11) {
             width: 110px;
             min-width: 110px;
             max-width: 110px;
         }
         .pass {
             color: #008000 !important;
             font-weight: bold;
         }
         .fail {
             color: #FF0000 !important;
             font-weight: bold;
         }
         .pass-bg {
             background-color: #D4EDDA;
         }
         .fail-bg {
             background-color: #F8D7DA;
         }
         .payload {
             white-space: pre;
             color: #008000 !important;
             background-color: #F8F9FA;
             padding: 10px;
             border-radius: 4px;
             margin: 0;
             font-size: 10px;
         }
         .verify-response-green {
             white-space: pre;
             background-color: #D4EDDA;
             padding: 10px;
             border-radius: 4px;
             margin: 0;
             font-size: 10px;
         }
         .verify-response-red {
             white-space: pre;
             background-color: #F8D7DA;
             padding: 10px;
             border-radius: 4px;
             margin: 0;
             font-size: 10px;
         }
         .verify-response-gray {
             white-space: pre;
             background-color: #F5F5F5;
             padding: 10px;
             border-radius: 4px;
             margin: 0;
             font-size: 10px;
         }
         .response-body {
             white-space: pre;
             color: #8B008B !important;
             background-color: #F8F9FA;
             padding: 10px;
             border-radius: 4px;
             margin: 0;
             font-size: 10px;
         }
         .map-content {
             white-space: pre;
             padding: 10px;
             border-radius: 4px;
             margin: 0;
             background-color: #F8F9FA;
             color: #000000;
             font-size: 10px;
         }
         .not-available {
             color: #6c757d !important;
             font-style: italic;
             font-size: 10px;
         }
         .description, .failure-reason, .capture-issues {
             min-width: 200px;
             max-width: 300px;
             font-size: 10px;
             white-space: normal !important;
             word-wrap: break-word !important;
             overflow-wrap: break-word !important;
         }
         .description span, .capture-issues span {
             white-space: normal !important;
             word-wrap: break-word !important;
             overflow-wrap: break-word !important;
         }
         .wrap-long {
             white-space: normal !important;
             word-wrap: break-word !important;
             overflow-wrap: break-word !important;
         }
         .wrap-long span {
             white-space: normal !important;
             word-wrap: break-word !important;
             overflow-wrap: break-word !important;
         }
         .summary-buttons {
             display: flex;
             justify-content: center;
             align-items: center;
             gap: 10px;
             flex-wrap: wrap;
         }
         .summary-buttons .count-btn {
             margin: 0;
             padding: 0.125rem 0.375rem;
             border: 1px solid transparent;
             border-radius: 0.25rem;
             font-size: 0.875rem;
             font-weight: bold;
             color: white;
             min-width: 1.5rem;
             text-align: center;
             line-height: 1.2;
         }
         .date-line {
             text-align: center;
             margin-bottom: 10px;
         }
         .scroll-to-top button {
             position: fixed;
             bottom: 20px;
             right: 20px;
             z-index: 1000;
             padding: 0.25rem 0.5rem;
             border: none !important;
             border-width: 0 !important;
             border-style: none !important;
             outline: none !important;
             border-radius: 0.375rem;
             height: 2rem;
             width: 2rem;
         }
         .scroll-to-top .bi {
             font-size: 1rem;
         }
         pre:hover {
             z-index: 10;
             box-shadow: 0 0 10px rgba(0,0,0,0.2);
         }
         #filterAllBtn {
             background-color: #0d6efd !important;
             border-color: #0d6efd !important;
         }
         #filterPassBtn {
             background-color: #198754 !important;
             border-color: #198754 !important;
         }
         #filterFailBtn {
             background-color: #dc3545 !important;
             border-color: #dc3545 !important;
         }
         #scrollToTopBtn {
             background-color: #0d6efd !important;
             border-color: #0d6efd !important;
             color: white !important;
         }
         """;

    public void generateReport(List<Map<String, Object>> reportDataList, ObjectMapper objectMapper) {
        int totalTests = reportDataList.size();
        int passCount = 0;
        int failCount = 0;
        for (Map<String, Object> reportData : reportDataList) {
            String status = safeToString(reportData.get("status"));
            if (status.equalsIgnoreCase("Pass") || status.equalsIgnoreCase("passed")) {
                passCount++;
            } else if (status.equalsIgnoreCase("Fail") || status.equalsIgnoreCase("failed")) {
                failCount++;
            }
        }
        System.out.println("DEBUG - Total: " + totalTests + ", Pass: " + passCount + ", Fail: " + failCount);

        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss z");
        String dateTime = now.format(formatter);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html lang='en'>\n")
            .append("<head>\n")
            .append("<meta charset='UTF-8'>\n")
            .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n")
            .append("<title>API Test Execution Report</title>\n")
            .append(BOOTSTRAP_CSS)
            .append(BOOTSTRAP_ICONS_CSS)
            .append("<style>\n").append(CSS_STYLE).append("</style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("<div class='report-container'>\n")
            .append("<h1 class='display-6 text-center mb-4'>API Test Execution Report</h1>\n")
            .append("<div class='text-center mb-3'>\n")
            .append("<div class='date-line'><p>Generated on: ").append(escapeHtml(dateTime)).append("</p></div>\n")
            .append("<div class='summary-buttons'>\n")
            .append("Total Tests: <button id='filterAllBtn' class='count-btn'>").append(totalTests).append("</button> | ")
            .append("Passed: <button id='filterPassBtn' class='count-btn'>").append(passCount).append("</button> | ")
            .append("Failed: <button id='filterFailBtn' class='count-btn'>").append(failCount).append("</button>\n")
            .append("</div>\n")
            .append("</div>\n")
            .append("<div class='table-container' id='tableContainer'>\n")
            .append("<table class='table table-hover table-bordered' id='testReportTable'>\n")
            .append("<thead>\n")
            .append("<tr>\n")
            .append("<th>Test ID</th>\n")
            .append("<th>Description</th>\n")
            .append("<th>Status</th>\n")
            .append("<th>Request</th>\n")
            .append("<th>Endpoint</th>\n")
            .append("<th>Payload</th>\n")
            .append("<th>Headers</th>\n")
            .append("<th>Parameters</th>\n")
            .append("<th>Authentication</th>\n")
            .append("<th>Response Status</th>\n")
            .append("<th>Response Time (ms)</th>\n")
            .append("<th>Response Body</th>\n")
            .append("<th>Verify Response</th>\n")
            .append("<th>Failure Reason</th>\n")
            .append("<th>Capture Issues</th>\n")
            .append("</tr>\n")
            .append("</thead>\n")
            .append("<tbody>\n");

        int rowIndex = 0;
        for (Map<String, Object> reportData : reportDataList) {
            String status = safeToString(reportData.get("status"));
            html.append("<tr id='row-").append(rowIndex).append("' data-status='").append(status.toLowerCase()).append("'>\n");
            html.append("<td>").append(safeToString(reportData.get("testId"))).append("</td>\n");
            String description = safeToString(reportData.get("description"));
            html.append("<td class='description'><span>").append(escapeHtml(description)).append("</span></td>\n");

            html.append("<td class='").append(status.equalsIgnoreCase("Pass") ? "pass" : status.equalsIgnoreCase("Fail") ? "fail" : "").append("'>")
                .append(status).append("</td>\n");

            html.append("<td>").append(safeToString(reportData.get("request"))).append("</td>\n");
            String endpointRaw = reportData.get("endpoint") != null ? String.valueOf(reportData.get("endpoint")) : "";
            String endpointContent;
            if (endpointRaw.length() > 100) {
                String endpointWrapped = wrapLongValue(endpointRaw, 100);
                String endpointEscaped = escapeHtml(endpointWrapped);
                String endpointHighlighted = highlightPlaceholders(endpointEscaped);
                endpointContent = endpointHighlighted.replace("\n", "<br>");
            } else {
                String endpointEscaped = escapeHtml(endpointRaw);
                endpointContent = highlightPlaceholders(endpointEscaped);
            }
            html.append("<td>").append(endpointContent).append("</td>\n");

            String payloadStr = reportData.get("payload") != null ? String.valueOf(reportData.get("payload")) : "";
            html.append("<td>").append(formatJsonContent(payloadStr, objectMapper, "payload")).append("</td>\n");

            String headersContent = formatMap(reportData.get("headers"), objectMapper, false);
            html.append("<td>").append(headersContent.startsWith("<span") ? headersContent : "<pre class='map-content'>" + highlightPlaceholders(headersContent) + "</pre>").append("</td>\n");

            String parametersContent = formatMap(reportData.get("parameters"), objectMapper, false);
            html.append("<td>").append(parametersContent.startsWith("<span") ? parametersContent : "<pre class='map-content'>" + highlightPlaceholders(parametersContent) + "</pre>").append("</td>\n");

            String authContent;
            Object authObj = reportData.get("authentication");
            if (authObj == null || !(authObj instanceof Map) || ((Map<?, ?>) authObj).isEmpty() || 
                ((Map<?, ?>) authObj).get("Type") == null || 
                "".equals(String.valueOf(((Map<?, ?>) authObj).get("Type")).trim()) || 
                "None".equalsIgnoreCase(String.valueOf(((Map<?, ?>) authObj).get("Type")))) {
                authContent = "<span class='not-available'>None</span>";
            } else {
                authContent = formatMap(authObj, objectMapper, true);
            }
            html.append("<td>").append(authContent.startsWith("<span") ? authContent : "<pre class='map-content'>" + highlightPlaceholders(authContent) + "</pre>").append("</td>\n");

            html.append("<td>").append(safeToString(reportData.get("responseStatus"))).append("</td>\n");

            String responseTimeMs = safeToString(reportData.get("responseTimeMs"));
            System.out.println("DEBUG - Rendering Response Time (ms) for Test ID " + safeToString(reportData.get("testId")) + ": " + responseTimeMs);
            html.append("<td>").append(responseTimeMs).append("</td>\n");

            String responseBodyStr = reportData.get("responseBody") != null ? String.valueOf(reportData.get("responseBody")) : "";
            String contentType = detectContentType(responseBodyStr);
            html.append("<td>").append(formatContent(responseBodyStr, objectMapper, "response-body", contentType)).append("</td>\n");

            String verifyResponseStr = reportData.get("verifyResponse") != null ? String.valueOf(reportData.get("verifyResponse")) : "";
            String failureReasonStr = safeToString(reportData.get("failureReason"));
            boolean isStatusMismatchFail = "Fail".equalsIgnoreCase(status) && !failureReasonStr.isEmpty() && failureReasonStr.contains("Status code mismatch");
            boolean isNonResponseFailure = "Fail".equalsIgnoreCase(status) && (responseBodyStr.trim().isEmpty() || failureReasonStr.contains("Illegal character in path"));
            String verifyResponseContent;
            String verifyCssClass;

            // Debug statement to inspect input data
            System.out.println("DEBUG - Test ID: " + safeToString(reportData.get("testId")) + ", status: " + status + ", verifyResponse: " + verifyResponseStr + ", verificationPassed: " + reportData.get("verificationPassed") + ", failureReason: " + failureReasonStr + ", responseBody: " + responseBodyStr);

            if (verifyResponseStr.trim().isEmpty()) {
                verifyCssClass = "not-available";
                verifyResponseContent = "<span class='not-available'>None</span>";
            } else if (isNonResponseFailure || isStatusMismatchFail) {
                verifyCssClass = "verify-response-gray";
                verifyResponseContent = formatJsonContent(verifyResponseStr, objectMapper, verifyCssClass);
            } else {
                boolean verificationPassed = reportData.containsKey("verificationPassed") ? (Boolean) reportData.get("verificationPassed") : false;
                verifyCssClass = verificationPassed && status.equalsIgnoreCase("Pass") ? "verify-response-green" : "verify-response-red";
                verifyResponseContent = formatJsonContent(verifyResponseStr, objectMapper, verifyCssClass);
            }
            html.append("<td>").append(verifyResponseContent).append("</td>\n");

            html.append("<td class='failure-reason'>").append(formatFailureReason(failureReasonStr)).append("</td>\n");

            String captureIssues = safeToString(reportData.get("captureIssues"));
            if (captureIssues == null || captureIssues.trim().isEmpty()) {
                html.append("<td class='capture-issues'><span class='not-available'>None</span></td>\n");
            } else {
                html.append("<td class='capture-issues'><span>").append(escapeHtml(captureIssues)).append("</span></td>\n");
            }

            html.append("</tr>\n");
            rowIndex++;
        }

        html.append("</tbody>\n");
        html.append("</table>\n");
        html.append("</div>\n");
        html.append("<div class='scroll-to-top'>\n");
        html.append("<button id='scrollToTopBtn' class='btn btn-primary btn-sm'><i class='bi bi-arrow-up'></i></button>\n");
        html.append("</div>\n");
        html.append(BOOTSTRAP_JS);
        html.append("<script>\n");
        html.append("document.addEventListener('DOMContentLoaded', function() {\n");
        html.append(" const tableContainer = document.getElementById('tableContainer');\n");
        html.append(" const testReportTable = document.getElementById('testReportTable');\n");
        html.append(" const filterAllBtn = document.getElementById('filterAllBtn');\n");
        html.append(" const filterPassBtn = document.getElementById('filterPassBtn');\n");
        html.append(" const filterFailBtn = document.getElementById('filterFailBtn');\n");
        html.append(" const scrollToTopBtn = document.getElementById('scrollToTopBtn');\n");
        html.append(" function wrapLongContent() {\n");
        html.append(" if (!testReportTable) return;\n");
        html.append(" document.querySelectorAll('#testReportTable tbody td.description').forEach(cell => {\n");
        html.append(" const span = cell.querySelector('span');\n");
        html.append(" const rawText = span ? span.textContent.trim() : cell.textContent.trim();\n");
        html.append(" if (rawText.includes('{{')) {\n");
        html.append(" const highlightedText = rawText.replace(/\\{\\{[^}]+\\}\\}/g, match => `<span style=\\\"color: #FF0000;\\\">${match}</span>`);\n");
        html.append(" if (span) span.innerHTML = highlightedText;\n");
        html.append(" else cell.innerHTML = `<span>${highlightedText}</span>`;\n");
        html.append(" }\n");
        html.append(" if (rawText.length > 150) {\n");
        html.append(" cell.classList.add('wrap-long');\n");
        html.append(" if (span) span.classList.add('wrap-long');\n");
        html.append(" }\n");
        html.append(" });\n");
        html.append(" document.querySelectorAll('#testReportTable tbody td.capture-issues').forEach(cell => {\n");
        html.append(" const span = cell.querySelector('span');\n");
        html.append(" const rawText = span ? span.textContent.trim() : cell.textContent.trim();\n");
        html.append(" if (rawText && rawText.length > 150) {\n");
        html.append(" cell.classList.add('wrap-long');\n");
        html.append(" if (span) span.classList.add('wrap-long');\n");
        html.append(" }\n");
        html.append(" });\n");
        html.append(" }\n");
        html.append(" if (filterAllBtn) {\n");
        html.append(" filterAllBtn.addEventListener('click', function() {\n");
        html.append(" filterTests('all');\n");
        html.append(" });\n");
        html.append(" }\n");
        html.append(" if (filterPassBtn) {\n");
        html.append(" filterPassBtn.addEventListener('click', function() {\n");
        html.append(" filterTests('pass');\n");
        html.append(" });\n");
        html.append(" }\n");
        html.append(" if (filterFailBtn) {\n");
        html.append(" filterFailBtn.addEventListener('click', function() {\n");
        html.append(" filterTests('fail');\n");
        html.append(" });\n");
        html.append(" }\n");
        html.append(" if (scrollToTopBtn) {\n");
        html.append(" scrollToTopBtn.addEventListener('click', function() {\n");
        html.append(" scrollToTop();\n");
        html.append(" });\n");
        html.append(" }\n");
        html.append(" filterTests('all');\n");
        html.append(" function filterTests(filterType) {\n");
        html.append(" const rows = document.querySelectorAll('#testReportTable tbody tr');\n");
        html.append(" if (rows.length === 0) {\n");
        html.append(" return;\n");
        html.append(" }\n");
        html.append(" rows.forEach(row => {\n");
        html.append(" const status = row.getAttribute('data-status') || '';\n");
        html.append(" row.style.display = (filterType === 'all' || status === filterType) ? '' : 'none';\n");
        html.append(" });\n");
        html.append(" const container = document.getElementById('tableContainer');\n");
        html.append(" if (!container) {\n");
        html.append(" return;\n");
        html.append(" }\n");
        html.append(" try {\n");
        html.append(" container.scrollTo({ top: 0, behavior: 'smooth' });\n");
        html.append(" } catch (e) {\n");
        html.append(" container.scrollTop = 0;\n");
        html.append(" }\n");
        html.append(" try {\n");
        html.append(" wrapLongContent();\n");
        html.append(" } catch (e) {\n");
        html.append(" console.error('Error in wrapLongContent after filter: ', e);\n");
        html.append(" }\n");
        html.append(" }\n");
        html.append(" function scrollToTop() {\n");
        html.append(" const container = document.getElementById('tableContainer');\n");
        html.append(" if (container) {\n");
        html.append(" try {\n");
        html.append(" container.scrollTo({ top: 0, behavior: 'smooth' });\n");
        html.append(" } catch (e) {\n");
        html.append(" container.scrollTop = 0;\n");
        html.append(" }\n");
        html.append(" } else {\n");
        html.append(" try {\n");
        html.append(" window.scrollTo({ top: 0, behavior: 'smooth' });\n");
        html.append(" } catch (e) {\n");
        html.append(" window.scrollTop = 0;\n");
        html.append(" }\n");
        html.append(" }\n");
        html.append(" }\n");
        html.append("});\n");
        html.append("</script>\n");
        html.append("</body>\n")
            .append("</html>");

        try (FileWriter fileWriter = new FileWriter("report.html")) {
            fileWriter.write(html.toString());
            System.out.println("Report generated successfully at: report.html");
        } catch (IOException e) {
            System.err.println("Error writing HTML report: " + e.getMessage());
            throw new RuntimeException("Failed to write HTML report", e);
        }
    }

    private String wrapLongValue(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        StringBuilder wrapped = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            int end = Math.min(index + maxLength, text.length());
            wrapped.append(text.substring(index, end));
            if (end < text.length()) {
                wrapped.append("\n");
            }
            index = end;
        }
        return wrapped.toString();
    }

    private String wrapLongLines(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String[] lines = text.split("\n");
        return Arrays.stream(lines)
                .map(line -> wrapSingleLine(line, maxLength))
                .collect(Collectors.joining("\n"));
    }

    private String wrapSingleLine(String line, int maxLength) {
        if (line.length() <= maxLength) {
            return line;
        }
        List<String> words = Arrays.asList(line.split("\\s+"));
        StringBuilder wrapped = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxLength) {
                if (currentLine.length() > 0) {
                    if (wrapped.length() > 0) {
                        wrapped.append("\n");
                    }
                    wrapped.append(currentLine.toString().trim());
                    currentLine = new StringBuilder();
                }
            }
            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }
        if (currentLine.length() > 0) {
            if (wrapped.length() > 0) {
                wrapped.append("\n");
            }
            wrapped.append(currentLine.toString().trim());
        }
        return wrapped.toString();
    }

    public boolean isNoValidationFailure(String verifyResponse, String responseBody, ObjectMapper objectMapper) {
        String verifyValue = verifyResponse != null ? verifyResponse.trim() : "";
        String responseValue = responseBody != null ? responseBody.trim() : "";
        System.out.println("Verify Response raw value: '" + verifyValue + "' (length: " + verifyValue.length() + ")");
        System.out.println("Response Body raw value: '" + responseValue + "' (length: " + responseValue.length() + ")");

        if (verifyValue.isEmpty()) {
            System.out.println("Verify Response is empty or null, considered match");
            return true;
        }

        try {
            Object verifyJson = objectMapper.readValue(verifyValue, Object.class);
            Object responseJson = objectMapper.readValue(responseValue, Object.class);
            boolean isMatch = isResponseMatch(verifyJson, responseJson);
            System.out.println("Response match result: " + isMatch);
            return isMatch;
        } catch (Exception e) {
            System.out.println("JSON parsing failed: " + e.getMessage() + ", considered mismatch");
            return false;
        }
    }

    private boolean isResponseMatch(Object verifyJson, Object responseJson) {
        if (verifyJson == null || responseJson == null) {
            System.out.println("One or both JSON objects are null, considered mismatch");
            return false;
        }
        if (verifyJson instanceof Map && responseJson instanceof Map) {
            Map<?, ?> verifyMap = (Map<?, ?>) verifyJson;
            Map<?, ?> responseMap = (Map<?, ?>) responseJson;
            if (!verifyMap.keySet().containsAll(responseMap.keySet()) || !responseMap.keySet().containsAll(verifyMap.keySet())) {
                System.out.println("Key set mismatch: Verify keys: " + verifyMap.keySet() + ", Response keys: " + responseMap.keySet());
                return false;
            }
            for (Map.Entry<?, ?> entry : verifyMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object verifyValue = entry.getValue();
                Object responseValue = responseMap.get(key);
                System.out.println("Comparing key: '" + key + "', Verify value: '" + verifyValue + "' (" + verifyValue.getClass().getSimpleName() + "), Response value: '" + responseValue + "' (" + (responseValue != null ? responseValue.getClass().getSimpleName() : "null") + ")");

                if (verifyValue instanceof String && "$any-value".equals(verifyValue)) {
                    if (responseValue == null) {
                        System.out.println("Key '" + key + "' has $any-value but response value is null, considered mismatch");
                        return false;
                    }
                    System.out.println("Key '" + key + "' has $any-value, accepting any response value");
                    continue;
                }
                if (verifyValue instanceof Map && responseValue instanceof Map) {
                    if (!isResponseMatch(verifyValue, responseValue)) {
                        System.out.println("Nested object mismatch for key: '" + key + "'");
                        return false;
                    }
                } else {
                    String verifyStr = String.valueOf(verifyValue);
                    String responseStr = String.valueOf(responseValue);
                    if (!Objects.equals(verifyStr, responseStr)) {
                        System.out.println("Value mismatch for key: '" + key + "', Verify: '" + verifyStr + "', Response: '" + responseStr + "'");
                        return false;
                    }
                    System.out.println("Value matched for key: '" + key + "'");
                }
            }
            System.out.println("All keys matched for object");
            return true;
        }
        System.out.println("Verify or Response is not a Map, considered mismatch");
        return false;
    }

    private String formatJsonContent(String content, ObjectMapper objectMapper, String cssClass) {
        return formatContent(content, objectMapper, cssClass, "json");
    }

    private String formatContent(String content, ObjectMapper objectMapper, String cssClass, String contentType) {
        if (content == null || content.trim().isEmpty()) {
            String appliedClass = (cssClass.equals("verify-response-green") || cssClass.equals("verify-response-red") || cssClass.equals("verify-response-gray")) ? "not-available" : "not-available";
            System.out.println("formatContent: Content is null or empty, using class: " + appliedClass);
            return "<span class='" + appliedClass + "'>None</span>";
        }
        try {
            String formattedContent;
            if ("xml".equals(contentType)) {
                String prettyXml = prettyPrintXml(content);
                formattedContent = prettyXml;
                System.out.println("formatContent: Formatted XML for class: " + cssClass + ", content: " + prettyXml);
            } else {
                Object json = objectMapper.readValue(content, Object.class);
                String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                formattedContent = prettyJson;
                System.out.println("formatContent: Formatted JSON for class: " + cssClass + ", content: " + prettyJson);
            }
            formattedContent = wrapLongLines(formattedContent, 100);
            String escaped = escapeHtml(formattedContent);
            String highlighted = highlightPlaceholders(escaped);
            if (cssClass.startsWith("verify-response")) {
                highlighted = highlightAnyValue(highlighted);
            }
            return "<pre class='" + cssClass + "'>" + highlighted + "</pre>";
        } catch (Exception e) {
            System.out.println("formatContent: Failed to parse " + contentType + ", using raw content for class: " + cssClass + ", content: " + content);
            String escaped = escapeHtml(content);
            String formattedContent = wrapLongLines(escaped, 100);
            String highlighted = highlightPlaceholders(formattedContent);
            if (cssClass.startsWith("verify-response")) {
                highlighted = highlightAnyValue(highlighted);
            }
            return "<pre class='" + cssClass + "'>" + highlighted + "</pre>";
        }
    }

    private String detectContentType(String body) {
        if (body == null || body.trim().isEmpty()) {
            return "text";
        }
        String trimmed = body.trim().toLowerCase();
        if (trimmed.startsWith("<?xml") || (trimmed.startsWith("<") && trimmed.contains(">") && trimmed.endsWith(">"))) {
            return "xml";
        }
        return "json";
    }

    private String prettyPrintXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            return xml;
        }
    }

    private String highlightAnyValue(String text) {
        if (text == null || !text.contains("$any-value")) {
            return text;
        }
        Pattern pattern = Pattern.compile("\\$any-value");
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String anyValue = matcher.group();
            String quotedAnyValue = Matcher.quoteReplacement(anyValue);
            matcher.appendReplacement(result, "<span style=\"color: #000000;\">" + quotedAnyValue + "</span>");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String formatFailureReason(String original) {
        if (original == null || original.trim().isEmpty()) {
            return "<span class='not-available'>None</span>";
        }
        String text = original;
        Pattern expPattern = Pattern.compile("expected\\s+([^,]+?)\\s*,\\s*");
        Matcher matcher = expPattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String value = matcher.group(1).trim();
            String escapedValue = escapeHtml(value);
            String quotedValue = Matcher.quoteReplacement(escapedValue);
            String replacement = "expected <span style=\"color: #008000;\">" + quotedValue + "</span>, ";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        String temp = sb.toString();
        Pattern gotPattern = Pattern.compile("got\\s+(.+)");
        matcher = gotPattern.matcher(temp);
        sb = new StringBuffer();
        while (matcher.find()) {
            String value = matcher.group(1).trim();
            String escapedValue = escapeHtml(value);
            String quotedValue = Matcher.quoteReplacement(escapedValue);
            String replacement = "got <span style=\"color: #FF0000;\">" + quotedValue + "</span>";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return highlightPlaceholders(sb.toString());
    }

    private String highlightPlaceholders(String text) {
        if (text == null || !text.contains("{{")) {
            return text;
        }
        Pattern pattern = Pattern.compile("\\{\\{[^}]+\\}\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group();
            String escapedPlaceholder = escapeHtml(placeholder);
            String quotedPlaceholder = Matcher.quoteReplacement(escapedPlaceholder);
            matcher.appendReplacement(result, "<span style=\"color: #FF0000;\">" + quotedPlaceholder + "</span>");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String safeToString(Object obj) {
        return obj != null ? escapeHtml(String.valueOf(obj)) : "";
    }

    private String formatMap(Object mapObj, ObjectMapper objectMapper, boolean isAuthentication) {
        if (mapObj == null) {
            return "<span class='not-available'>None</span>";
        }
        if (mapObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) mapObj;
            if (isAuthentication) {
                Object type = map.get("Type");
                if (type == null || "None".equalsIgnoreCase(String.valueOf(type))) {
                    return "<span class='not-available'>None</span>";
                }
            }
            if (map.isEmpty()) {
                return "<span class='not-available'>None</span>";
            }
            try {
                ObjectNode jsonNode = objectMapper.createObjectNode();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String keyStr = String.valueOf(entry.getKey());
                    String valStr = String.valueOf(entry.getValue());
                    if (valStr.length() > 100) {
                        valStr = wrapLongValue(valStr, 100);
                    }
                    jsonNode.put(keyStr, valStr);
                }
                String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
                return highlightPlaceholders(escapeHtml(jsonString));
            } catch (Exception e) {
                return highlightPlaceholders(escapeHtml(String.valueOf(mapObj)));
            }
        }
        try {
            Object json = objectMapper.readValue(String.valueOf(mapObj), Object.class);
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            return highlightPlaceholders(escapeHtml(jsonString));
        } catch (Exception e) {
            return highlightPlaceholders(escapeHtml(String.valueOf(mapObj)));
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