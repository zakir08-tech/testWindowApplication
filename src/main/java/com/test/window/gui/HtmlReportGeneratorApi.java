package com.test.window.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlReportGeneratorApi {

    private static final String BOOTSTRAP_CSS = """
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-9ndCyUaIbzAi2FUVXJi0CjmCapSmO7SnpJef0486qhLnuZ2cdeRhO02iuK6FUUVM" crossorigin="anonymous">
        """;

    private static final String BOOTSTRAP_ICONS_CSS = """
        <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
        """;

    private static final String BOOTSTRAP_JS = """
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js" integrity="sha384-geWF76RCwLtnZ8qwWowPQNguL3RmwHVBC9FhGdlKrxdiJJigb/j/68SIy3Te4Bkz" crossorigin="anonymous"></script>
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
            font-size: calc(1.5 * 0.5rem); /* ~0.75rem */
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
        .summary-buttons .btn {
            margin: 0 5px;
        }
        .scroll-to-top button {
            position: fixed;
            bottom: 20px;
            right: 20px;
            z-index: 1000;
            padding: 5px 10px;
            border: none !important;
            border-width: 0 !important;
            border-style: none !important;
            outline: none !important;
        }
        .scroll-to-top .bi {
            font-size: 1.2rem;
        }
        pre:hover {
            z-index: 10;
            box-shadow: 0 0 10px rgba(0,0,0,0.2);
        }
        """;

    public void generateReport(List<Map<String, Object>> reportDataList, ObjectMapper objectMapper) {
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
        html.append(BOOTSTRAP_ICONS_CSS);
        html.append("<style>\n").append(CSS_STYLE).append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class='report-container'>\n");
        html.append("<h1 class='display-6 text-center mb-4'>API Test Execution Report</h1>\n");
        html.append("<div class='text-center mb-3'>\n");
        html.append("<p>Generated on: ").append(escapeHtml(dateTime)).append("</p>\n");
        html.append("<div class='summary-buttons'>\n");
        html.append("Total Tests: <button id='filterAllBtn' class='btn btn-primary btn-sm'>").append(totalTests).append("</button> | ");
        html.append("Passed: <button id='filterPassBtn' class='btn btn-success btn-sm'>").append(passCount).append("</button> | ");
        html.append("Failed: <button id='filterFailBtn' class='btn btn-danger btn-sm'>").append(failCount).append("</button>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        html.append("<div class='table-container' id='tableContainer'>\n");
        html.append("<table class='table table-hover table-bordered' id='testReportTable'>\n");
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
        html.append("<th>Capture Issues</th>\n");
        html.append("</tr>\n");
        html.append("</thead>\n");
        html.append("<tbody>\n");

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
            html.append("<td>").append(highlightPlaceholders(safeToString(reportData.get("endpoint")))).append("</td>\n");

            html.append("<td>").append(formatJsonContent(safeToString(reportData.get("payload")), objectMapper, "payload")).append("</td>\n");

            String headersContent = formatMap(reportData.get("headers"), objectMapper, false);
            html.append("<td>").append(headersContent.startsWith("<span") ? headersContent : "<pre class='map-content'>" + highlightPlaceholders(headersContent) + "</pre>").append("</td>\n");

            String parametersContent = formatMap(reportData.get("parameters"), objectMapper, false);
            html.append("<td>").append(parametersContent.startsWith("<span") ? parametersContent : "<pre class='map-content'>" + highlightPlaceholders(parametersContent) + "</pre>").append("</td>\n");

            String authContent = formatMap(reportData.get("authentication"), objectMapper, true);
            html.append("<td>").append(authContent.startsWith("<span") ? authContent : "<pre class='map-content'>" + highlightPlaceholders(authContent) + "</pre>").append("</td>\n");

            html.append("<td>").append(safeToString(reportData.get("responseStatus"))).append("</td>\n");

            String responseBody = safeToString(reportData.get("responseBody"));
            html.append("<td>").append(formatJsonContent(responseBody, objectMapper, "response-body")).append("</td>\n");

            String verifyResponse = safeToString(reportData.get("verifyResponse"));
            // Use "verificationPassed" from reportData instead of recomputing isMatch
            boolean verificationPassed = reportData.containsKey("verificationPassed") ? (Boolean) reportData.get("verificationPassed") : true; // Default true if missing
            String verifyResponseContent = (verifyResponse == null || verifyResponse.trim().isEmpty()) 
                ? "<span class='not-available'>None</span>" 
                : formatJsonContent(verifyResponse, objectMapper, verificationPassed ? "verify-response-green" : "verify-response-red");
            html.append("<td>").append(verifyResponseContent).append("</td>\n");

            html.append("<td class='failure-reason'>").append(formatFailureReason(safeToString(reportData.get("failureReason")))).append("</td>\n");

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
        html.append(" console.log('DOM fully loaded at: ', new Date().toLocaleString());\n");
        html.append(" const tableContainer = document.getElementById('tableContainer');\n");
        html.append(" const testReportTable = document.getElementById('testReportTable');\n");
        html.append(" const filterAllBtn = document.getElementById('filterAllBtn');\n");
        html.append(" const filterPassBtn = document.getElementById('filterPassBtn');\n");
        html.append(" const filterFailBtn = document.getElementById('filterFailBtn');\n");
        html.append(" const scrollToTopBtn = document.getElementById('scrollToTopBtn');\n");
        html.append(" if (!tableContainer) console.error('Table container (#tableContainer) not found');\n");
        html.append(" else console.log('Table container found');\n");
        html.append(" if (!testReportTable) console.error('Table (#testReportTable) not found');\n");
        html.append(" else console.log('Table found, rows: ', testReportTable.querySelectorAll('tbody tr').length);\n");
        html.append(" if (!filterAllBtn) console.error('Total Tests button (#filterAllBtn) not found');\n");
        html.append(" else console.log('Total Tests button found');\n");
        html.append(" if (!filterPassBtn) console.error('Passed button (#filterPassBtn) not found');\n");
        html.append(" else console.log('Passed button found');\n");
        html.append(" if (!filterFailBtn) console.error('Failed button (#filterFailBtn) not found');\n");
        html.append(" else console.log('Failed button found');\n");
        html.append(" if (!scrollToTopBtn) console.error('Scroll to top button (#scrollToTopBtn) not found');\n");
        html.append(" else console.log('Scroll to top button found');\n");
        html.append(" function wrapLongContent() {\n");
        html.append("  console.log('Running wrapLongContent at: ', new Date().toLocaleString());\n");
        html.append("  document.querySelectorAll('#testReportTable tbody td.description').forEach(cell => {\n");
        html.append("   const span = cell.querySelector('span');\n");
        html.append("   const rawText = span ? span.textContent.trim() : cell.textContent.trim();\n");
        html.append("   console.log('Description text:', rawText);\n");
        html.append("   console.log('Description text length:', rawText.length);\n");
        html.append("   if (rawText.includes('{{')) {\n");
        html.append("    const highlightedText = rawText.replace(/\\{\\{[^}]+\\}\\}/g, match => `<span style=\"color: #FF0000;\">${match}</span>`);\n");
        html.append("    if (span) span.innerHTML = highlightedText;\n");
        html.append("    else cell.innerHTML = `<span>${highlightedText}</span>`;\n");
        html.append("   }\n");
        html.append("   if (rawText.length > 150) {\n");
        html.append("    console.log('Applying wrap-long to Description');\n");
        html.append("    cell.classList.add('wrap-long');\n");
        html.append("    if (span) span.classList.add('wrap-long');\n");
        html.append("   } else {\n");
        html.append("    console.log('Skipping wrap-long for Description');\n");
        html.append("   }\n");
        html.append("  });\n");
        html.append("  document.querySelectorAll('#testReportTable tbody td.capture-issues').forEach(cell => {\n");
        html.append("   const span = cell.querySelector('span');\n");
        html.append("   const rawText = span ? span.textContent.trim() : cell.textContent.trim();\n");
        html.append("   console.log('Capture Issues text:', rawText);\n");
        html.append("   console.log('Capture Issues text length:', rawText.length);\n");
        html.append("   if (rawText.includes('{{')) {\n");
        html.append("    const highlightedText = rawText.replace(/\\{\\{[^}]+\\}\\}/g, match => `<span style=\"color: #FF0000;\">${match}</span>`);\n");
        html.append("    if (span) span.innerHTML = highlightedText;\n");
        html.append("    else cell.innerHTML = `<span>${highlightedText}</span>`;\n");
        html.append("   }\n");
        html.append("   if (rawText.length > 150) {\n");
        html.append("    console.log('Applying wrap-long to Capture Issues');\n");
        html.append("    cell.classList.add('wrap-long');\n");
        html.append("    if (span) span.classList.add('wrap-long');\n");
        html.append("   } else {\n");
        html.append("    console.log('Skipping wrap-long for Capture Issues');\n");
        html.append("   }\n");
        html.append("  });\n");
        html.append("  document.querySelectorAll('#testReportTable tbody td').forEach(cell => {\n");
        html.append("   const pre = cell.querySelector('pre.verify-response-green, pre.verify-response-red');\n");
        html.append("   if (pre) {\n");
        html.append("    const computedStyle = window.getComputedStyle(pre);\n");
        html.append("    console.log('Verify Response cell class:', pre.className, 'color:', computedStyle.color);\n");
        html.append("   }\n");
        html.append("   const span = cell.querySelector('span.not-available');\n");
        html.append("   if (span) {\n");
        html.append("    const computedStyle = window.getComputedStyle(span);\n");
        html.append("    console.log('None class:', span.className, 'color:', computedStyle.color);\n");
        html.append("   }\n");
        html.append("  });\n");
        html.append(" }\n");
        html.append(" try {\n");
        html.append("  wrapLongContent();\n");
        html.append("  console.log('wrapLongContent executed successfully');\n");
        html.append(" } catch (e) {\n");
        html.append("  console.error('Error in wrapLongContent: ', e);\n");
        html.append(" }\n");
        html.append(" if (filterAllBtn) {\n");
        html.append("  filterAllBtn.addEventListener('click', function() {\n");
        html.append("   console.log('Total Tests button clicked at: ', new Date().toLocaleString());\n");
        html.append("   filterTests('all');\n");
        html.append("  });\n");
        html.append(" }\n");
        html.append(" if (filterPassBtn) {\n");
        html.append("  filterPassBtn.addEventListener('click', function() {\n");
        html.append("   console.log('Passed button clicked at: ', new Date().toLocaleString());\n");
        html.append("   filterTests('pass');\n");
        html.append("  });\n");
        html.append(" }\n");
        html.append(" if (filterFailBtn) {\n");
        html.append("  filterFailBtn.addEventListener('click', function() {\n");
        html.append("   console.log('Failed button clicked at: ', new Date().toLocaleString());\n");
        html.append("   filterTests('fail');\n");
        html.append("  });\n");
        html.append(" }\n");
        html.append(" if (scrollToTopBtn) {\n");
        html.append("  scrollToTopBtn.addEventListener('click', function() {\n");
        html.append("   console.log('Scroll to top button clicked at: ', new Date().toLocaleString());\n");
        html.append("   filterTests('all');\n");
        html.append("  });\n");
        html.append(" }\n");
        html.append(" filterTests('all');\n");
        html.append(" function filterTests(filterType) {\n");
        html.append("  console.log('filterTests called with type:', filterType, 'at: ', new Date().toLocaleString());\n");
        html.append("  const rows = document.querySelectorAll('#testReportTable tbody tr');\n");
        html.append("  console.log('Number of rows found:', rows.length);\n");
        html.append("  if (rows.length === 0) {\n");
        html.append("   console.warn('No table rows found to filter');\n");
        html.append("   return;\n");
        html.append("  }\n");
        html.append("  rows.forEach(row => {\n");
        html.append("   const status = row.getAttribute('data-status') || '';\n");
        html.append("   console.log('Row ID:', row.id, 'Status:', status);\n");
        html.append("   row.style.display = (filterType === 'all' || status === filterType) ? '' : 'none';\n");
        html.append("   console.log(row.style.display === '' ? 'Showing row:' : 'Hiding row:', row.id);\n");
        html.append("  });\n");
        html.append("  const container = document.getElementById('tableContainer');\n");
        html.append("  if (!container) {\n");
        html.append("   console.error('Table container (#tableContainer) not found during filter');\n");
        html.append("   return;\n");
        html.append("  }\n");
        html.append("  console.log('Resetting table scroll on filter');\n");
        html.append("  try {\n");
        html.append("   container.scrollTo({ top: 0, behavior: 'smooth' });\n");
        html.append("   console.log('scrollTo executed on filter');\n");
        html.append("  } catch (e) {\n");
        html.append("   console.warn('scrollTo failed on filter, using scrollTop:', e);\n");
        html.append("   container.scrollTop = 0;\n");
        html.append("   console.log('scrollTop set to 0 on filter');\n");
        html.append("  }\n");
        html.append("  try {\n");
        html.append("   wrapLongContent();\n");
        html.append("   console.log('wrapLongContent called after filter');\n");
        html.append("  } catch (e) {\n");
        html.append("   console.error('Error in wrapLongContent after filter: ', e);\n");
        html.append("  }\n");
        html.append(" }\n");
        html.append(" function scrollToTop() {\n");
        html.append("  console.log('scrollToTop called at: ', new Date().toLocaleString());\n");
        html.append("  const container = document.getElementById('tableContainer');\n");
        html.append("  if (!container) {\n");
        html.append("   console.error('Table container (#tableContainer) not found');\n");
        html.append("   return;\n");
        html.append("  }\n");
        html.append("  const scrollHeight = container.scrollHeight;\n");
        html.append("  const clientHeight = container.clientHeight;\n");
        html.append("  const scrollTop = container.scrollTop;\n");
        html.append("  console.log('Table container stats - scrollHeight:', scrollHeight, 'clientHeight:', clientHeight, 'scrollTop:', scrollTop);\n");
        html.append("  if (scrollHeight <= clientHeight) {\n");
        html.append("   console.warn('Table container is not scrollable (scrollHeight <= clientHeight)');\n");
        html.append("   return;\n");
        html.append("  }\n");
        html.append("  if (scrollTop === 0) {\n");
        html.append("   console.log('Already at the top of the table');\n");
        html.append("   return;\n");
        html.append("  }\n");
        html.append("  console.log('Attempting to scroll table container to top');\n");
        html.append("  try {\n");
        html.append("   container.scrollTo({ top: 0, behavior: 'smooth' });\n");
        html.append("   console.log('scrollTo executed successfully');\n");
        html.append("  } catch (e) {\n");
        html.append("   console.warn('scrollTo failed, using scrollTop:', e);\n");
        html.append("   container.scrollTop = 0;\n");
        html.append("   console.log('scrollTop set to 0');\n");
        html.append("  }\n");
        html.append(" }\n");
        html.append("});\n");
        html.append("</script>\n");
        html.append("</body>\n");
        html.append("</html>");

        try (FileWriter fileWriter = new FileWriter("report.html")) {
            fileWriter.write(html.toString());
            System.out.println("Report generated successfully at: report.html");
        } catch (IOException e) {
            System.err.println("Error writing HTML report: " + e.getMessage());
        }
    }

    // CHANGE: Made public to allow reuse in RunApiTest without duplication (was private)
    public boolean isNoValidationFailure(String verifyResponse, String responseBody, ObjectMapper objectMapper) {
        String verifyValue = safeToString(verifyResponse).trim();
        String responseValue = safeToString(responseBody).trim();
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
        if (content == null || content.trim().isEmpty()) {
            String appliedClass = (cssClass.equals("verify-response-green") || cssClass.equals("verify-response-red")) ? "not-available" : "not-available";
            System.out.println("formatJsonContent: Content is null or empty, using class: " + appliedClass);
            return "<span class='" + appliedClass + "'>None</span>";
        }
        try {
            Object json = objectMapper.readValue(content, Object.class);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            System.out.println("formatJsonContent: Formatted JSON for class: " + cssClass + ", content: " + prettyJson);
            String highlighted = highlightPlaceholders(escapeHtml(prettyJson));
            if (cssClass.startsWith("verify-response")) {
                highlighted = highlightAnyValue(highlighted);
            }
            return "<pre class='" + cssClass + "'>" + highlighted + "</pre>";
        } catch (Exception e) {
            System.out.println("formatJsonContent: Failed to parse JSON, using raw content for class: " + cssClass + ", content: " + content);
            String highlighted = highlightPlaceholders(escapeHtml(content));
            if (cssClass.startsWith("verify-response")) {
                highlighted = highlightAnyValue(highlighted);
            }
            return "<pre class='" + cssClass + "'>" + highlighted + "</pre>";
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
            matcher.appendReplacement(result, "<span style=\"color: #000000;\">" + anyValue + "</span>");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String formatFailureReason(String original) {
        if (original == null || original.trim().isEmpty()) {
            return "<span class='not-available'>None</span>";
        }
        String text = highlightPlaceholders(original);
        // Handle expected <value>,
        Pattern expPattern = Pattern.compile("expected\\s+([^,]+?)\\s*,\\s*");
        Matcher matcher = expPattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String value = matcher.group(1).trim();
            String escapedValue = escapeHtml(value);
            String replacement = "expected <span style=\"color: #008000;\">" + escapedValue + "</span>, ";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        String temp = sb.toString();
        // Handle got <value>
        Pattern gotPattern = Pattern.compile("got\\s+(.+)");
        matcher = gotPattern.matcher(temp);
        sb = new StringBuffer();
        while (matcher.find()) {
            String value = matcher.group(1).trim();
            String escapedValue = escapeHtml(value);
            String replacement = "got <span style=\"color: #FF0000;\">" + escapedValue + "</span>";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
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
            matcher.appendReplacement(result, "<span style=\"color: #FF0000;\">" + escapeHtml(placeholder) + "</span>");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String safeToString(Object obj) {
        return obj != null ? String.valueOf(obj).replace("<", "&lt;").replace(">", "&gt;") : "";
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
                    jsonNode.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
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