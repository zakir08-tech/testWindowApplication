package com.test.window.gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.Desktop;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Class for generating an HTML test report similar to the provided sample.
 * Integrates with TestRunner by accepting collected test data and producing an HTML file.
 * 
 * To integrate:
 * 1. In TestRunner, create a List reportEntries during test execution.
 * 2. For each executed TestCase, create a TestReportEntry and populate its steps while executing.
 * 3. For each step: Create StepReport, construct testStepStr based on action, measure duration, catch exceptions for error.
 * 4. If "OPEN_WINDOW", update lastAppFamilyName from testData.split("\\|")[0].
 * 5. If "TAKE_SCREENSHOT", call GlueCode.takeScreenshotAsBytes() and set stepReport.screenshotBytes.
 * 6. After execution: entry.status = testPassed ? "Passed" : "Failed";
 * 7. Count passed/failed from reportEntries.
 * 8. Compute totalRunTimeMs = sum of entry.totalDuration.
 * 9. Call HtmlReportGenerator.generateReport(new File("reports/report.html"), reportEntries, totalRunTimeMs, lastLoadedFile, passedCount, failedCount, 0);
 * 10. After calling generateReport, call HtmlReportGenerator.openReportAutomatically(new File("reports/report.html")) to open the report in the default browser, regardless of test outcomes.
 */
public class HtmlReportGenerator {

    /**
     * Data model for a single test in the report.
     */
    public static class TestReportEntry {
        public String testId;
        public String description;
        public String status; // "Passed", "Failed"
        public List<StepReport> steps = new ArrayList<>();
        public long totalDuration;
    }

    /**
     * Data model for a single step in the report.
     */
    public static class StepReport {
        public int stepNo;
        public String testStep;
        public long durationMs;
        public String stepDesc;
        public String error;
        public byte[] screenshotBytes;
    }

    /**
     * Generates the HTML report using Bootstrap 5.3.3 with dark theme and collapsible accordion for test cases.
     */
    public static void generateReport(File outputFile, List<TestReportEntry> entries, long totalRunTimeMs, 
                                      File testSuiteFile, int passed, int failed, int warnings) throws IOException {
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        // Set timezone to IST for accurate time representation
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("IST"));
        String runDate = sdf.format(new Date()); // Reflects 02:10 PM IST, Sep 25, 2025
        String totalRun = String.valueOf(entries.size());
        String runTime = formatTotalRunTime(totalRunTimeMs / 1000); // Convert to seconds for hrs mins sec
        String testSuite = testSuiteFile != null ? testSuiteFile.getName().replaceFirst("[.][^.]+$", "") : "Unknown";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Phantom: Window Test Report</title>\n");
        html.append("    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\" integrity=\"sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH\" crossorigin=\"anonymous\">\n");
        html.append("    <script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js\"></script>\n");
        html.append("    <style>\n");
        html.append("        body { background-color: #121212; color: #e0e0e0; } /* Fallback for dark theme */\n");
        html.append("        .passed { color: #00FF00 !important; }\n");
        html.append("        .failed { color: #FF6666 !important; }\n");
        html.append("        .step-passed { background-color: #198754; color: white; }\n");
        html.append("        .step-failed { background-color: #dc3545; color: white; }\n");
        html.append("        .screenshot-img { max-width: 50%; max-height: 50%; border: 1px solid #444; border-radius: 4px; cursor: pointer; }\n");
        html.append("        .modal-img { max-width: 90vw; max-height: 90vh; object-fit: contain; }\n");
        html.append("        .modal-content { background-color: #212529; border: none; }\n");
        html.append("        .accordion-button:not(.collapsed) { background-color: #343a40; color: #e0e0e0; }\n");
        html.append("        .accordion-button { background-color: #212529; color: #e0e0e0; }\n");
        html.append("        .accordion-body { background-color: #212529; }\n");
        html.append("        .btn.active { background-color: #343a40 !important; border-color: #343a40 !important; }\n");
        html.append("        .text-black { color: #000000 !important; }\n");
        html.append("        #testSummaryChart { max-width: 150px; max-height: 150px; }\n");
        html.append("        .custom-legend { text-align: center; margin-top: 5px; }\n");
        html.append("        .custom-legend span { display: inline-block; margin-right: 15px; font-size: 12px; color: #e0e0e0; }\n");
        html.append("        .custom-legend span::before { content: ''; width: 12px; height: 12px; display: inline-block; margin-right: 5px; border-radius: 2px; vertical-align: middle; }\n");
        html.append("        .custom-legend .passed::before { background-color: #00FF00; }\n");
        html.append("        .custom-legend .failed::before { background-color: #FF6666; }\n");
        html.append("        .custom-legend .warning::before { background-color: #FFC107; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body data-bs-theme=\"dark\">\n");
        html.append("    <div class=\"container my-4\">\n");

        // Header
        html.append("        <h1 class=\"text-center mb-4 text-light\">Phantom: Window Test Report</h1>\n");

        // Summary Card
        html.append("        <div class=\"card bg-dark text-light mb-4\">\n");
        html.append("            <div class=\"card-body d-flex align-items-start justify-content-between\">\n");
        html.append("                <div>\n");
        html.append("                    <h5 class=\"card-title\">Test Summary</h5>\n");
        html.append("                    <div class=\"mb-1\">Test Run Date: ").append(escapeHtml(runDate)).append("</div>\n");
        html.append("                    <div class=\"mb-1\">Total Run: ").append(totalRun).append("</div>\n");
        html.append("                    <div class=\"mb-1\"><span class=\"passed\">Passed: ").append(passed).append("</span></div>\n");
        html.append("                    <div class=\"mb-1\"><span class=\"failed\">Failed: ").append(failed).append("</span></div>\n");
        html.append("                    <div class=\"mb-1\">Warning: ").append(warnings).append("</div>\n");
        html.append("                    <div class=\"mb-1\">Run Time: ").append(runTime).append("</div>\n");
        html.append("                    <div class=\"mb-1\">Test Suite: ").append(escapeHtml(testSuite)).append("</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"ms-auto w-auto text-center\" id=\"chartContainer\" style=\"min-width: 150px;\">\n");
        html.append("                    <canvas id=\"testSummaryChart\"></canvas>\n");
        html.append("                    <div id=\"customLegend\" class=\"custom-legend\"></div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        // Filters with Proper Bootstrap Buttons
        html.append("        <div class=\"btn-group mb-3\" role=\"group\" id=\"filterButtons\">\n");
        html.append("            <button type=\"button\" class=\"btn btn-primary me-2 active\" onclick=\"filterTests('all')\">Show All</button>\n");
        html.append("            <button type=\"button\" class=\"btn btn-success me-2\" onclick=\"filterTests('passed')\">Show Pass</button>\n");
        html.append("            <button type=\"button\" class=\"btn btn-danger\" onclick=\"filterTests('failed')\">Show Fail</button>\n");
        html.append("        </div>\n");

        // Collapsible Test Cases (Accordion)
        html.append("        <h5 class=\"text-light mb-3\">Test Details</h5>\n");
        html.append("        <div class=\"accordion\" id=\"testAccordion\">\n");
        for (int i = 0; i < entries.size(); i++) {
            TestReportEntry entry = entries.get(i);
            String statusClass = entry.status.equalsIgnoreCase("Passed") ? "passed" : "failed";
            boolean isFirst = (i == 0);
            String collapseId = "collapseTest" + i;
            String headerId = "headingTest" + i;
            html.append("            <div class=\"accordion-item bg-dark\">\n");
            html.append("                <h2 class=\"accordion-header\" id=\"" + headerId + "\">\n");
            html.append("                    <button class=\"accordion-button collapsed text-light " + statusClass + "\" type=\"button\" data-bs-toggle=\"collapse\" data-bs-target=\"#" + collapseId + "\" aria-expanded=\"false\" aria-controls=\"" + collapseId + "\">\n");
            html.append("                        [Test ID: " + escapeHtml(entry.testId) + "] " + escapeHtml(entry.description) + "\n");
            html.append("                    </button>\n");
            html.append("                </h2>\n");
            html.append("                <div id=\"" + collapseId + "\" class=\"accordion-collapse collapse\" data-bs-parent=\"#testAccordion\">\n");
            html.append("                    <div class=\"accordion-body\">\n");
            html.append("                        <table class=\"table table-dark table-striped table-sm\">\n");
            html.append("                            <thead>\n");
            html.append("                                <tr>\n");
            html.append("                                    <th scope=\"col\">Step No.</th>\n");
            html.append("                                    <th scope=\"col\">Test Step</th>\n");
            html.append("                                    <th scope=\"col\">Duration [min:sec:ms]</th>\n");
            html.append("                                    <th scope=\"col\">Step Desc.</th>\n");
            html.append("                                </tr>\n");
            html.append("                            </thead>\n");
            html.append("                            <tbody>\n");
            for (StepReport step : entry.steps) {
                String stepClass = step.error == null ? "step-passed" : "step-failed";
                html.append("                                <tr class=\"" + stepClass + "\">\n");
                html.append("                                    <td>" + step.stepNo + "</td>\n");
                // Embed screenshot in Test Step column for TAKE_SCREENSHOT steps
                html.append("                                    <td>");
                if ("Take screen-shot".equals(step.testStep) && step.screenshotBytes != null && step.screenshotBytes.length > 0) {
                    String base64Image = Base64.getEncoder().encodeToString(step.screenshotBytes);
                    String modalId = "screenshotModal" + entry.testId + "_" + step.stepNo;
                    html.append("<img src=\"data:image/png;base64," + base64Image + "\" alt=\"Screenshot\" class=\"screenshot-img\" data-bs-toggle=\"modal\" data-bs-target=\"#" + modalId + "\">");
                    // Modal for full-screen screenshot
                    html.append("<div class=\"modal fade\" id=\"" + modalId + "\" tabindex=\"-1\" aria-labelledby=\"" + modalId + "Label\" aria-hidden=\"true\">");
                    html.append("<div class=\"modal-dialog modal-dialog-centered modal-xl\">");
                    html.append("<div class=\"modal-content\">");
                    html.append("<div class=\"modal-header\">");
                    html.append("<h5 class=\"modal-title\" id=\"" + modalId + "Label\">Screenshot for Test ID: " + escapeHtml(entry.testId) + ", Step " + step.stepNo + "</h5>");
                    html.append("<button type=\"button\" class=\"btn-close\" data-bs-dismiss=\"modal\" aria-label=\"Close\"></button>");
                    html.append("</div>");
                    html.append("<div class=\"modal-body text-center\">");
                    html.append("<img src=\"data:image/png;base64," + base64Image + "\" alt=\"Full Screenshot\" class=\"modal-img\">");
                    html.append("</div>");
                    html.append("</div>");
                    html.append("</div>");
                    html.append("</div>");
                } else {
                    html.append(escapeHtml(step.testStep));
                }
                html.append("</td>\n");
                html.append("                                    <td>" + formatDuration(step.durationMs) + "</td>\n");
                html.append("                                    <td>" + escapeHtml(step.stepDesc != null ? step.stepDesc : "") + "</td>\n");
                html.append("                                </tr>\n");

                // If error, add row for error details
                if (step.error != null) {
                    html.append("                                <tr class=\"table-danger\">\n");
                    html.append("                                    <td colspan=\"4\"><small class=\"text-black\">Error: " + escapeHtml(step.error) + "</small></td>\n");
                    html.append("                                </tr>\n");
                }
            }
            html.append("                            </tbody>\n");
            html.append("                        </table>\n");
            html.append("                    </div>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
        }
        html.append("        </div>\n");
        html.append("    </div>\n");

        // Bootstrap and Chart.js JS
        html.append("    <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js\" integrity=\"sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz\" crossorigin=\"anonymous\"></script>\n");
        html.append("    <script>\n");
        html.append("        // Chart.js initialization for Test Summary pie chart\n");
        html.append("        const ctx = document.getElementById('testSummaryChart').getContext('2d');\n");
        html.append("        const chart = new Chart(ctx, {\n");
        html.append("            type: 'pie',\n");
        html.append("            data: {\n");
        html.append("                labels: ['Passed', 'Failed', 'Warning'],\n");
        html.append("                datasets: [{\n");
        html.append("                    data: [" + passed + ", " + failed + ", " + warnings + "],\n");
        html.append("                    backgroundColor: ['#00FF00', '#FF6666', '#FFC107'],\n");
        html.append("                    borderColor: ['#00CC00', '#CC3333', '#FFA500'],\n");
        html.append("                    borderWidth: 1\n");
        html.append("                }]\n");
        html.append("            },\n");
        html.append("            options: {\n");
        html.append("                responsive: true,\n");
        html.append("                maintainAspectRatio: true,\n");
        html.append("                plugins: {\n");
        html.append("                    legend: { display: false }\n");
        html.append("                }\n");
        html.append("            },\n");
        html.append("            plugins: [{\n");
        html.append("                id: 'htmlLegend',\n");
        html.append("                afterUpdate(chart) {\n");
        html.append("                    const items = chart.options.plugins.legend.labels.generateLabels(chart);\n");
        html.append("                    const legendContainer = document.getElementById('customLegend');\n");
        html.append("                    legendContainer.innerHTML = '';\n");
        html.append("                    items.forEach(item => {\n");
        html.append("                        const label = item.text.toLowerCase();\n");
        html.append("                        const span = document.createElement('span');\n");
        html.append("                        span.className = `custom-legend ${label}`;\n");
        html.append("                        span.innerHTML = item.text;\n");
        html.append("                        legendContainer.appendChild(span);\n");
        html.append("                    });\n");
        html.append("                }\n");
        html.append("            }]\n");
        html.append("        });\n");
        html.append("        // Filter function\n");
        html.append("        function filterTests(type) {\n");
        html.append("            const buttons = document.querySelectorAll('#filterButtons .btn');\n");
        html.append("            buttons.forEach(btn => btn.classList.remove('active', 'btn-dark'));\n");
        html.append("            const activeBtn = Array.from(buttons).find(btn => btn.getAttribute('onclick').includes(type));\n");
        html.append("            if (activeBtn) activeBtn.classList.add('active', 'btn-dark');\n");
        html.append("            const items = document.querySelectorAll('#testAccordion .accordion-item');\n");
        html.append("            items.forEach(item => {\n");
        html.append("                const button = item.querySelector('.accordion-button');\n");
        html.append("                const collapse = item.querySelector('.accordion-collapse');\n");
        html.append("                const status = button.classList.contains('passed') ? 'passed' : 'failed';\n");
        html.append("                if (type === 'all' || type === status) {\n");
        html.append("                    item.style.display = 'block';\n");
        html.append("                    collapse.classList.remove('show');\n");
        html.append("                    button.classList.add('collapsed');\n");
        html.append("                    button.setAttribute('aria-expanded', 'false');\n");
        html.append("                } else {\n");
        html.append("                    item.style.display = 'none';\n");
        html.append("                    collapse.classList.remove('show');\n");
        html.append("                    button.classList.add('collapsed');\n");
        html.append("                    button.setAttribute('aria-expanded', 'false');\n");
        html.append("                }\n");
        html.append("            });\n");
        html.append("        }\n");
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        // Write to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(html.toString());
        }
    }

    /**
     * Opens the generated report file automatically in the default system browser.
     * Uses Desktop.browse() with a fallback to Runtime.exec() for broader platform support.
     */
    public static void openReportAutomatically(File reportFile) {
        if (reportFile.exists()) {
            try {
                // Try using Desktop.browse() first
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(reportFile.toURI());
                    return;
                }
                // Fallback to Runtime.exec() with refined commands
                String os = System.getProperty("os.name").toLowerCase();
                String[] command;
                if (os.contains("win")) {
                    command = new String[]{"cmd.exe", "/c", "start", "", reportFile.getAbsolutePath()};
                } else if (os.contains("mac")) {
                    command = new String[]{"open", reportFile.getAbsolutePath()};
                } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                    command = new String[]{"xdg-open", reportFile.getAbsolutePath()};
                } else {
                    System.err.println("Unsupported operating system. Please open the report manually at: " + reportFile.getAbsolutePath());
                    return;
                }
                Process process = Runtime.getRuntime().exec(command);
                process.waitFor(); // Ensure the command completes
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to open report automatically. Please open it manually at: " + reportFile.getAbsolutePath());
                e.printStackTrace();
            }
        } else {
            System.err.println("Report file does not exist at: " + reportFile.getAbsolutePath());
        }
    }

    /**
     * Escapes HTML entities for safe output.
     */
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;");
    }

    /**
     * Formats duration in ms to "00:00:000" (min:sec:ms).
     */
    private static String formatDuration(long ms) {
        long min = ms / 60000;
        long sec = (ms % 60000) / 1000;
        long milli = ms % 1000;
        return String.format("%02d:%02d:%03d", min, sec, milli);
    }

    /**
     * Formats total run time in seconds to "0 hrs 1 mins 38 sec".
     */
    private static String formatTotalRunTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours + " hrs " + minutes + " mins " + seconds + " sec";
    }
}