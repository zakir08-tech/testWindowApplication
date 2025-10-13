package com.test.window.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main application class for the API Test Runner GUI.
 * This JavaFX application allows users to load API test cases from an Excel file,
 * select and run tests, monitor status, and generate HTML reports.
 */
public class RunApiTest extends Application {

    /**
     * CSS style for unfocused input fields, defining dark theme appearance.
     */
    private static final String FIELD_STYLE_UNFOCUSED =
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #BBBBBB; -fx-border-radius: 5px;";

    /**
     * CSS style for the Run button in its default state.
     */
    private static final String RUN_BUTTON_STYLE =
        "-fx-background-color: #90EE90; -fx-text-fill: black; -fx-border-radius: 5px; -fx-min-width: 100px;";

    /**
     * CSS style for the Run button on hover.
     */
    private static final String RUN_BUTTON_HOVER_STYLE =
        "-fx-background-color: #98FB98; -fx-text-fill: black; -fx-border-radius: 5px; -fx-min-width: 100px;";

    /**
     * CSS style for the Stop button in its default state.
     */
    private static final String STOP_BUTTON_STYLE =
        "-fx-background-color: #FFB6C1; -fx-text-fill: black; -fx-border-radius: 5px; -fx-min-width: 100px;";

    /**
     * CSS style for the Stop button on hover.
     */
    private static final String STOP_BUTTON_HOVER_STYLE =
        "-fx-background-color: #FFC1CC; -fx-text-fill: black; -fx-border-radius: 5px; -fx-min-width: 100px;";

    /**
     * CSS style for Load and Refresh buttons in their default state.
     */
    private static final String LOAD_REFRESH_BUTTON_STYLE =
        "-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

    /**
     * CSS style for Load and Refresh buttons on hover.
     */
    private static final String LOAD_REFRESH_BUTTON_HOVER_STYLE =
        "-fx-background-color: #6AB0FF; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

    /**
     * Inline CSS styles for customizing the table view's appearance, including scroll bars and row styling.
     */
    private static final String CSS = """
        .table-view .scroll-bar:vertical,
        .table-view .scroll-bar:horizontal {
            -fx-background-color: #252525;
        }
        .table-view .scroll-bar:vertical .track,
        .table-view .scroll-bar:horizontal .track {
            -fx-background-color: #252525;
            -fx-border-color: transparent;
            -fx-background-radius: 0px;
        }
        .table-view .scroll-bar:vertical .thumb,
        .table-view .scroll-bar:horizontal .thumb {
            -fx-background-color: #3C3F41;
            -fx-background-radius: 5px;
        }
        .table-view .scroll-bar:vertical .thumb:hover,
        .table-view .scroll-bar:horizontal .thumb:hover {
            -fx-background-color: #4A90E2;
        }
        .table-view .scroll-bar:vertical .thumb:pressed,
        .table-view .scroll-bar:horizontal .thumb:pressed {
            -fx-background-color: #4A90E2;
        }
        .table-view .scroll-bar .increment-button,
        .table-view .scroll-bar .decrement-button {
            -fx-background-color: #252525;
            -fx-border-color: transparent;
        }
        .table-view .scroll-bar .increment-arrow,
        .table-view .scroll-bar .decrement-arrow {
            -fx-shape: " ";
            -fx-background-color: #3C3F41;
        }
        .table-view .scroll-bar:vertical .increment-arrow:hover,
        .table-view .scroll-bar:vertical .decrement-arrow:hover,
        .table-view .scroll-bar:horizontal .increment-arrow:hover,
        .table-view .scroll-bar:horizontal .decrement-arrow:hover {
            -fx-background-color: #4A90E2;
        }
        .table-view .table-row-cell {
            -fx-cell-size: 30px;
            -fx-pref-height: 30px;
            -fx-min-height: 30px;
            -fx-max-height: 30px;
        }
        .table-view .table-cell {
            -fx-alignment: center-left;
        }
        .table-view .table-row-cell:selected {
            -fx-background-color: #4A90E2;
            -fx-text-fill: white;
        }
        .table-view .table-row-cell:focused {
            -fx-background-color: #4A90E2;
            -fx-text-fill: white;
        }
        """;

    /**
     * Inner class representing a single test case model with observable properties for UI binding.
     */
    private static class TestCase {
        private final BooleanProperty run; // Whether to run this test case
        private final StringProperty testId; // Unique ID of the test case
        private final StringProperty testDescription; // Description of the test case
        private final StringProperty status; // Current status of the test (e.g., "No Run", "Running...", "Pass", "Fail")

        /**
         * Constructor to initialize a TestCase with initial values.
         *
         * @param run initial run flag
         * @param testId initial test ID
         * @param testDescription initial description
         * @param status initial status
         */
        public TestCase(boolean run, String testId, String testDescription, String status) {
            this.run = new SimpleBooleanProperty(run);
            this.testId = new SimpleStringProperty(testId);
            this.testDescription = new SimpleStringProperty(testDescription);
            this.status = new SimpleStringProperty(status);
        }

        public BooleanProperty runProperty() {
            return run;
        }

        public StringProperty testIdProperty() {
            return testId;
        }

        public StringProperty testDescriptionProperty() {
            return testDescription;
        }

        public StringProperty statusProperty() {
            return status;
        }
    }

    // UI Components
    private TableView<TestCase> table; // Table to display test cases
    private ObservableList<TestCase> testCases; // Observable list of test cases
    private CheckBox selectAllCheckBox; // Checkbox to select/deselect all test cases
    private Button loadButton; // Button to load a new Excel file
    private Button runButton; // Button to start running selected tests
    private Button stopButton; // Button to stop running tests
    private Button refreshButton; // Button to reload from the last loaded file
    private File lastLoadedFile; // Reference to the last loaded Excel file
    private Task<Void> runTask; // Background task for running tests
    private double lastScrollPosition = 0.0; // Last scroll position to maintain focus

    // Data storage for test configurations
    private HashMap<Integer, HashMap<String, Object>> testDataMap = new HashMap<>(); // Test-specific data like endpoint, payload
    private HashMap<Integer, HashMap<String, Object>> headersMap = new HashMap<>(); // Headers for each test
    private HashMap<Integer, HashMap<String, Object>> paramsMap = new HashMap<>(); // Query parameters for each test
    private HashMap<Integer, HashMap<String, Object>> modifyPayloadMap = new HashMap<>(); // Modifications to payload for each test
    private HashMap<Integer, HashMap<String, Object>> responseCaptureMap = new HashMap<>(); // Response capture configurations
    private HashMap<Integer, HashMap<String, Object>> authMap = new HashMap<>(); // Authentication details for each test
    private List<Map<String, Object>> reportDataList = new ArrayList<>(); // List to hold report data for all tests

    /**
     * Set of required column headers in the Excel file for validation.
     */
    private static final Set<String> REQUIRED_HEADERS = new HashSet<>(Arrays.asList(
        "Test ID", "Request", "End-Point", "Header (key)", "Header (value)",
        "Parameter (key)", "Parameter (value)", "Payload", "Payload Type",
        "Modify Payload (key)", "Modify Payload (value)", "Response (key) Name",
        "Capture (key) Value (env var)", "Authorization", "", "",
        "SSL Validation", "Expected Status", "Verify Response",
        "Test Description"
    ));

    /**
     * Entry point for the JavaFX application.
     * Sets up the UI, table columns, event handlers, and displays the stage.
     *
     * @param primaryStage the primary stage for this application
     */
    @Override
    public void start(Stage primaryStage) {
        testCases = FXCollections.observableArrayList();
        table = new TableView<>(testCases);
        table.setStyle("-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
                "-fx-table-cell-border-color: #3C3F41; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");

        final double CHAR_WIDTH = 8.0;
        final double RUN_COL_WIDTH = 5 * CHAR_WIDTH;
        final double TEST_ID_COL_WIDTH = 8 * CHAR_WIDTH;
        final double STATUS_COL_WIDTH = 12 * CHAR_WIDTH;
        final double TEST_DESC_MIN_WIDTH = 300.0;

        TableColumn<TestCase, Boolean> runColumn = new TableColumn<>("Run");
        runColumn.setCellValueFactory(cellData -> cellData.getValue().runProperty());
        runColumn.setCellFactory(col -> {
            CheckBoxTableCell<TestCase, Boolean> cell = new CheckBoxTableCell<>(index -> {
                BooleanProperty property = testCases.get(index).runProperty();
                property.addListener((obs, oldVal, newVal) -> {
                    table.getSelectionModel().select(index);
                    table.requestFocus();
                });
                return property;
            });
            cell.setAlignment(Pos.CENTER);
            cell.setEditable(true);
            return cell;
        });
        runColumn.setMinWidth(RUN_COL_WIDTH);
        runColumn.setMaxWidth(RUN_COL_WIDTH);
        runColumn.setPrefWidth(RUN_COL_WIDTH);
        runColumn.setResizable(false);
        runColumn.setStyle("-fx-alignment: CENTER;");

        // Test ID column
        TableColumn<TestCase, String> testIdColumn = new TableColumn<>("Test ID");
        testIdColumn.setCellValueFactory(cellData -> cellData.getValue().testIdProperty());
        testIdColumn.setMinWidth(TEST_ID_COL_WIDTH);
        testIdColumn.setMaxWidth(TEST_ID_COL_WIDTH);
        testIdColumn.setPrefWidth(TEST_ID_COL_WIDTH);
        testIdColumn.setResizable(false);
        testIdColumn.setStyle("-fx-alignment: CENTER;");

        // Test Description column (flexible width)
        TableColumn<TestCase, String> testDescriptionColumn = new TableColumn<>("Test Description");
        testDescriptionColumn.setCellValueFactory(cellData -> cellData.getValue().testDescriptionProperty());
        testDescriptionColumn.setMinWidth(TEST_DESC_MIN_WIDTH);
        testDescriptionColumn.setPrefWidth(TEST_DESC_MIN_WIDTH);

        // Status column with color-coded text
        TableColumn<TestCase, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusColumn.setCellFactory(column -> new TableCell<TestCase, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                } else {
                    setText(status);
                    setStyle("-fx-alignment: CENTER;");
                    if (status.equals("Pass")) {
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: #90EE90;");
                    } else if (status.equals("Fail")) {
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: #FFB6C1;");
                    } else {
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: white;");
                    }
                }
            }
        });
        statusColumn.setMinWidth(STATUS_COL_WIDTH);
        statusColumn.setMaxWidth(STATUS_COL_WIDTH);
        statusColumn.setPrefWidth(STATUS_COL_WIDTH);
        statusColumn.setResizable(false);

        // Add columns to table and set resize policy
        table.getColumns().addAll(runColumn, testIdColumn, testDescriptionColumn, statusColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setEditable(true);

        // Set layout priorities for table
        HBox.setHgrow(table, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Select All checkbox with event handler
        selectAllCheckBox = new CheckBox("Select All");
        selectAllCheckBox.setStyle(FIELD_STYLE_UNFOCUSED);
        selectAllCheckBox.setSelected(false);
        selectAllCheckBox.setOnAction(e -> {
            boolean selected = selectAllCheckBox.isSelected();
            for (TestCase testCase : testCases) {
                testCase.runProperty().set(selected);
            }
        });

        // Listener for test cases list changes to update Select All state
        testCases.addListener((ListChangeListener<TestCase>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (TestCase testCase : change.getAddedSubList()) {
                        testCase.runProperty().addListener((obs, oldVal, newVal) -> updateSelectAllState());
                    }
                }
            }
            updateSelectAllState();
        });

        // Scroll event listener to track vertical scroll position
        table.setOnScroll(event -> {
            ScrollBar verticalScrollBar = getVerticalScrollBar();
            if (verticalScrollBar != null && verticalScrollBar.isVisible()) {
                lastScrollPosition = verticalScrollBar.getValue();
            }
        });

        // Load button setup
        loadButton = new Button("Load");
        loadButton.setStyle(LOAD_REFRESH_BUTTON_STYLE);
        loadButton.setTooltip(new Tooltip("Load test cases from an Excel test suite"));
        loadButton.setOnAction(e -> loadTestCases(primaryStage, true));
        loadButton.setOnMouseEntered(e -> loadButton.setStyle(LOAD_REFRESH_BUTTON_HOVER_STYLE));
        loadButton.setOnMouseExited(e -> loadButton.setStyle(LOAD_REFRESH_BUTTON_STYLE));

        // Run button setup with background task
        runButton = new Button("Run");
        runButton.setStyle(RUN_BUTTON_STYLE);
        runButton.setTooltip(new Tooltip("Run selected test cases"));
        runButton.setOnAction(e -> {
            reportDataList.clear(); // Clear previous report data
            runTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    // Disable UI controls during execution
                    Platform.runLater(() -> {
                        runButton.setDisable(true);
                        loadButton.setDisable(true);
                        refreshButton.setDisable(true);
                        stopButton.setDisable(false);
                    });

                    // Initialize API executor and JSON mapper
                    ApiExecutor apiExecutor = new ApiExecutor();
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, String> envVars = new HashMap<>();

                    // Load environment variables from env.json
                    File envFile = new File("env.json");
                    if (envFile.exists()) {
                        try {
                            envVars = objectMapper.readValue(envFile, HashMap.class);
                            System.out.println("Debug: Loaded env.json: " + envVars);
                        } catch (IOException ex) {
                            System.err.println("Error loading env.json: " + ex.getMessage());
                        }
                    } else {
                        System.err.println("env.json not found in project root. Initializing empty environment variables.");
                    }

                    // Process each selected test case
                    for (TestCase testCase : testCases) {
                        if (isCancelled()) {
                            break;
                        }
                        if (testCase.runProperty().get()) {
                            Integer testId = Integer.parseInt(testCase.testIdProperty().get());
                            int index = testCases.indexOf(testCase);
                            // Update UI for current test
                            Platform.runLater(() -> {
                                testCase.statusProperty().set("Running...");
                                table.getSelectionModel().clearAndSelect(index);
                                double rowHeight = 30.0;
                                double headerHeight = 24.0;
                                double visibleHeight = table.getHeight() - headerHeight;
                                int visibleRowCount = (int) Math.floor(visibleHeight / rowHeight);
                                ScrollBar verticalScrollBar = getVerticalScrollBar();
                                double scrollValue = verticalScrollBar != null && verticalScrollBar.isVisible() ?
                                    verticalScrollBar.getValue() : lastScrollPosition;
                                int totalRows = testCases.size();
                                int firstVisibleIndex = totalRows > 0 ?
                                    (int) Math.round(scrollValue * (totalRows - visibleRowCount)) : 0;
                                if (firstVisibleIndex < 0) firstVisibleIndex = 0;
                                int lastVisibleIndex = firstVisibleIndex + visibleRowCount - 1;
                                if (index < firstVisibleIndex || index > lastVisibleIndex) {
                                    table.scrollTo(index);
                                    if (verticalScrollBar != null && verticalScrollBar.isVisible()) {
                                        lastScrollPosition = verticalScrollBar.getValue();
                                    }
                                }
                                table.requestFocus();
                            });

                            // Initialize variables for this test case
                            ApiExecutor.Auth auth = new ApiExecutor.Auth("NONE", null, null, null);
                            String sslValidationStr = null;
                            HashMap<String, Object> testData = testDataMap.get(testId);
                            String method = (String) testData.get("Request");
                            String url = (String) testData.get("End-Point");
                            String payload = (String) testData.get("Payload");
                            String payloadType = (String) testData.get("Payload Type");
                            String expectedStatusStr = (String) testData.get("Expected Status");
                            String verifyResponse = (String) testData.get("Verify Response");
                            sslValidationStr = (String) testData.get("SSL Validation");
                            String modifiedPayload = payload; // Initialize to original payload
                            String processedVerifyResponse = null; // Initialize for catch block
                            StringBuilder captureIssues = new StringBuilder(); // To capture issues

                            // Create a map to store report data for this test case
                            Map<String, Object> reportData = new HashMap<>();
                            reportData.put("testId", testId.toString());
                            reportData.put("description", testCase.testDescriptionProperty().get());
                            reportData.put("request", method);
                            reportData.put("endpoint", url);
                            reportData.put("payload", payload);
                            reportData.put("headers", headersMap.get(testId));
                            reportData.put("parameters", paramsMap.get(testId));
                            reportData.put("authentication", authMap.get(testId));
                            reportData.put("verifyResponse", verifyResponse);

                            // Declare responseTimeMs outside try-catch
                            long responseTimeMs = 0L;

                            try {
                                System.out.println("Debug: Starting processing for Test ID " + testId);
                                HashMap<String, Object> headers = headersMap.get(testId);
                                HashMap<String, Object> params = paramsMap.get(testId);
                                HashMap<String, Object> modifyPayload = modifyPayloadMap.get(testId);
                                HashMap<String, Object> authDetails = authMap.get(testId);

                                // Process URL with placeholders
                                System.out.println("Debug: Replacing placeholders in URL for Test ID " + testId);
                                String processedUrl = replacePlaceholders(url, envVars, testId);
                                testData.put("End-Point", processedUrl); // Update testDataMap with processed URL
                                reportData.put("endpoint", processedUrl);

                                // Process headers
                                System.out.println("Debug: Processing headers for Test ID " + testId);
                                HashMap<String, Object> processedHeaders = new HashMap<>();
                                for (Map.Entry<String, Object> entry : headers.entrySet()) {
                                    String headerKey = entry.getKey();
                                    String headerValue = entry.getValue() != null ? entry.getValue().toString() : "";
                                    String processedValue = replacePlaceholders(headerValue, envVars, testId);
                                    processedHeaders.put(headerKey, processedValue);
                                }
                                headersMap.put(testId, processedHeaders); // Update headersMap with processed headers
                                reportData.put("headers", processedHeaders);

                                // Process parameters
                                System.out.println("Debug: Processing parameters for Test ID " + testId);
                                HashMap<String, Object> processedParams = new HashMap<>();
                                for (Map.Entry<String, Object> entry : params.entrySet()) {
                                    String paramKey = entry.getKey();
                                    String paramValue = entry.getValue() != null ? entry.getValue().toString() : "";
                                    String processedValue = replacePlaceholders(paramValue, envVars, testId);
                                    processedParams.put(paramKey, processedValue);
                                }
                                paramsMap.put(testId, processedParams); // Update paramsMap with processed parameters
                                reportData.put("parameters", processedParams);

                                // Process payload modifications
                                System.out.println("Debug: Processing modify payload for Test ID " + testId);
                                HashMap<String, Object> processedModifyPayload = new HashMap<>();
                                for (Map.Entry<String, Object> entry : modifyPayload.entrySet()) {
                                    String modifyKey = entry.getKey();
                                    String modifyValue = entry.getValue() != null ? entry.getValue().toString() : "";
                                    String processedValue = replacePlaceholders(modifyValue, envVars, testId);
                                    processedModifyPayload.put(modifyKey, processedValue);
                                }
                                modifyPayloadMap.put(testId, processedModifyPayload);
                                System.out.println("Debug: Processed modify payload for Test ID " + testId + ": " + processedModifyPayload);

                                modifiedPayload = payload;
                                if (payload != null && !payload.trim().isEmpty()) {
                                    System.out.println("Debug: Replacing placeholders in payload for Test ID " + testId);
                                    modifiedPayload = replacePlaceholders(payload, envVars, testId);
                                    String lowerPayloadType = payloadType != null ? payloadType.toLowerCase() : "";
                                    boolean isJsonPayload = "json".equals(lowerPayloadType);
                                    if (isJsonPayload) {
                                        // Validate JSON payload before parsing
                                        try {
                                            objectMapper.readTree(modifiedPayload);
                                        } catch (JsonProcessingException ex) {
                                            throw new Exception("Invalid JSON in payload for Test ID " + testId + ": " + ex.getMessage());
                                        }
                                        if (!processedModifyPayload.isEmpty()) {
                                            System.out.println("Debug: Parsing and modifying payload for Test ID " + testId);
                                            Map<String, Object> payloadObj = objectMapper.readValue(modifiedPayload, HashMap.class);
                                            System.out.println("Debug: Original JSON payload for Test ID " + testId + ": " + objectMapper.writeValueAsString(payloadObj));
                                            for (Map.Entry<String, Object> entry : processedModifyPayload.entrySet()) {
                                                String key = entry.getKey();
                                                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                                                System.out.println("Debug: Attempting to set key '" + key + "' to value '" + value + "' for Test ID " + testId);
                                                boolean success = setNestedValue(payloadObj, key, value, testId);
                                                if (!success) {
                                                    System.err.println("Warning: Unable to set key '" + key + "' in payload for Test ID " + testId);
                                                } else {
                                                    System.out.println("Debug: Successfully set key '" + key + "' to value '" + value + "' for Test ID " + testId);
                                                }
                                            }
                                            modifiedPayload = objectMapper.writeValueAsString(payloadObj);
                                            System.out.println("Debug: Modified JSON payload for Test ID " + testId + ": " + modifiedPayload);
                                        } else {
                                            System.out.println("Debug: No payload modification needed for Test ID " + testId);
                                        }
                                    } else {
                                        System.out.println("Debug: Skipping JSON-specific processing for non-JSON payload type '" + lowerPayloadType + "' for Test ID " + testId);
                                    }
                                } else {
                                    System.out.println("Debug: No payload to process for Test ID " + testId);
                                }
                                testData.put("Payload", modifiedPayload); // Update testDataMap with modified payload
                                reportData.put("payload", modifiedPayload);
                                reportData.put("payloadType", payloadType);

                                // Process authentication
                                System.out.println("Debug: Processing authorization for Test ID " + testId);
                                HashMap<String, Object> processedAuthDetails = new HashMap<>();
                                processedAuthDetails.put("Type", authDetails.get("Type"));
                                for (Map.Entry<String, Object> entry : authDetails.entrySet()) {
                                    String key = entry.getKey();
                                    if (key.equals("Username") || key.equals("Password") || key.equals("Token")) {
                                        String value = entry.getValue() != null ? entry.getValue().toString() : "";
                                        String processedValue = replacePlaceholders(value, envVars, testId);
                                        processedAuthDetails.put(key, processedValue);
                                    }
                                }
                                authMap.put(testId, processedAuthDetails); // Update authMap with processed auth details
                                reportData.put("authentication", processedAuthDetails); // Add auth details to report
                                String authType = (String) processedAuthDetails.get("Type");
                                if (authType != null && !authType.equals("None")) {
                                    String username = (String) processedAuthDetails.get("Username");
                                    String password = (String) processedAuthDetails.get("Password");
                                    String token = (String) processedAuthDetails.get("Token");
                                    auth = new ApiExecutor.Auth(authType, username, password, token);
                                }

                                // Process expected status and verify response
                                System.out.println("Debug: Processing expected status for Test ID " + testId);
                                String processedExpectedStatusStr = replacePlaceholders(expectedStatusStr, envVars, testId);
                                processedVerifyResponse = replacePlaceholders(verifyResponse, envVars, testId);

                                boolean sslValidation = sslValidationStr != null && sslValidationStr.equalsIgnoreCase("true");

                                int expectedStatus;
                                try {
                                    expectedStatus = processedExpectedStatusStr != null ?
                                        Integer.parseInt(processedExpectedStatusStr) : 200;
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException("Invalid Expected Status for Test ID " + testId +
                                        ": " + (processedExpectedStatusStr != null ? processedExpectedStatusStr : "null") +
                                        " (after placeholder replacement)", e);
                                }

                                // Execute the API test
                                System.out.println("Debug: Payload being sent to executeTest for Test ID " + testId + ": " + modifiedPayload);
                                long startTime = System.nanoTime();
                                ApiExecutor.Response response = apiExecutor.executeTest(
                                    method,
                                    processedUrl,
                                    processedHeaders,
                                    processedParams,
                                    modifiedPayload,
                                    payloadType,
                                    processedModifyPayload,
                                    auth,
                                    sslValidation
                                );
                                long endTime = System.nanoTime();
                                responseTimeMs = (endTime - startTime) / 1_000_000; // Calculate response time
                                System.out.println("Debug: Response time for Test ID " + testId + ": " + responseTimeMs + " ms");

                                // Store response data for report
                                reportData.put("responseStatus", String.valueOf(response.getStatusCode()));
                                reportData.put("responseBody", response.getBody());
                                reportData.put("responseTimeMs", responseTimeMs);

                                // Verify status code
                                if (response.getStatusCode() != expectedStatus) {
                                    throw new Exception("Status code mismatch for Test ID " + testId +
                                        ": expected " + expectedStatus + ", got " + response.getStatusCode());
                                }

                                // Process response capture
                                HashMap<String, Object> responseCapture = responseCaptureMap.get(testId);
                                if (!responseCapture.isEmpty()) {
                                    System.out.println("Debug: Starting response capture for Test ID " + testId);
                                    Map<String, Object> responseObj;
                                    try {
                                        responseObj = objectMapper.readValue(response.getBody(), HashMap.class);
                                    } catch (Exception ex) {
                                        captureIssues.append("Failed to parse response body as JSON for capture: ").append(ex.getMessage()).append(". ");
                                        System.err.println("Error parsing response body for capture in Test ID " + testId + ": " + ex.getMessage());
                                        throw new Exception("Failed to parse response body as JSON for capture in Test ID " + testId + ": " + ex.getMessage(), ex);
                                    }

                                    int captureCount = 0;
                                    Map<String, String> capturedValues = new HashMap<>();
                                    for (Map.Entry<String, Object> entry : responseCapture.entrySet()) {
                                        String responsePath = entry.getKey();
                                        String envVarName = entry.getValue() != null ? entry.getValue().toString() : "";
                                        if (responsePath == null || responsePath.trim().isEmpty() || envVarName == null || envVarName.trim().isEmpty()) {
                                            captureIssues.append("Invalid response capture entry: path='").append(responsePath)
                                                .append("', envVar='").append(envVarName).append("'. ");
                                            System.err.println("Warning: Invalid response capture entry for Test ID " + testId + ": path='" + responsePath + "', envVar='" + envVarName + "'");
                                            continue;
                                        }
                                        // Clean envVarName to remove surrounding {{ }} if present
                                        Pattern pattern = Pattern.compile("^\\s*\\{\\{(.*?)\\}\\}\\s*$");
                                        Matcher matcher = pattern.matcher(envVarName);
                                        if (matcher.matches()) {
                                            envVarName = matcher.group(1).trim();
                                            System.out.println("Debug: Cleaned env var name from '" + entry.getValue() + "' to '" + envVarName + "' for Test ID " + testId);
                                        }
                                        System.out.println("Debug: Attempting to capture value for path '" + responsePath + "' to env var '" + envVarName + "' for Test ID " + testId);
                                        Object capturedValue = getNestedValue(responseObj, responsePath, testId);
                                        if (capturedValue != null) {
                                            String valueStr = capturedValue.toString();
                                            envVars.put(envVarName, valueStr);
                                            capturedValues.put(envVarName, valueStr);
                                            captureCount++;
                                            captureIssues.append("Captured key '").append(responsePath)
                                                .append("' as env var '").append(envVarName).append("': ").append(valueStr).append(". ");
                                            System.out.println("Debug: Captured value '" + valueStr + "' from path '" + responsePath + "' and saved to env var '" + envVarName + "' for Test ID " + testId);
                                        } else {
                                            captureIssues.append("Key '").append(responsePath)
                                                .append("' not found in response for env var '").append(envVarName).append("'. ");
                                            System.err.println("Warning: No value found at path '" + responsePath + "' in response for Test ID " + testId);
                                        }
                                    }
                                    System.out.println("Debug: Total values captured for Test ID " + testId + ": " + captureCount + " out of " + responseCapture.size() + " entries");

                                    // Save updated envVars back to env.json
                                    try {
                                        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new FileOutputStream(envFile), envVars);
                                        captureIssues.append("Successfully updated env.json with ").append(captureCount).append(" captured values. ");
                                        System.out.println("Debug: Successfully updated env.json with " + captureCount + " captured values: " + capturedValues + " for Test ID " + testId);
                                    } catch (IOException ex) {
                                        captureIssues.append("Failed to save env.json: ").append(ex.getMessage()).append(". ");
                                        System.err.println("Error writing to env.json for Test ID " + testId + ": " + ex.getMessage());
                                        throw new Exception("Failed to write captured values to env.json for Test ID " + testId + ": " + ex.getMessage(), ex);
                                    }
                                } else {
                                    captureIssues.append("No response capture defined. ");
                                    System.out.println("Debug: No response capture entries defined for Test ID " + testId);
                                }

                                // Store capture issues in report data
                                reportData.put("captureIssues", captureIssues.toString());

                                // Initialize verificationPassed to true (passed by default if no verification needed)
                                boolean verificationPassed = true;

                                // Verify response body if specified
                                if (processedVerifyResponse != null && !processedVerifyResponse.trim().isEmpty()) {
                                    System.out.println("Debug: Verifying response for Test ID " + testId);
                                    try {
                                        verifyResponse(response.getBody(), processedVerifyResponse, testId);
                                    } catch (Exception e) {
                                        verificationPassed = false;
                                        throw new Exception("Response verification failed for Test ID " + testId + ": " + e.getMessage(), e);
                                    }
                                }

                                // Store verificationPassed in reportData for success path
                                reportData.put("verificationPassed", verificationPassed);

                                // Debug output
                                System.out.println("Test ID: " + testId);
                                System.out.println("Test Data: " + testData);
                                System.out.println("Headers: " + processedHeaders);
                                System.out.println("Parameters: " + processedParams);
                                System.out.println("Modified Payload: " + modifiedPayload);
                                System.out.println("Modify Payload Map: " + processedModifyPayload);
                                System.out.println("Response Capture: " + responseCapture);
                                System.out.println("Authorization: " + processedAuthDetails);
                                System.out.println("Expected Status: " + processedExpectedStatusStr);
                                System.out.println("Response Time (ms): " + responseTimeMs);
                                try {
                                    System.out.println("Response Body (Pretty JSON):\n" + ApiExecutor.toPrettyJson(response));
                                } catch (IOException ex) {
                                    System.out.println("Response Body: " + response.getBody());
                                }
                                Platform.runLater(() -> {
                                    testCase.statusProperty().set("Pass");
                                    reportData.put("status", "Pass");
                                    reportData.put("failureReason", "");
                                    reportDataList.add(reportData);
                                });
                            } catch (Exception ex) {
                                String failureReason = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                                System.err.println("Error executing Test ID " + testId + ": " + failureReason);
                                ex.printStackTrace();
                                reportData.put("captureIssues", captureIssues.toString());
                                boolean verificationPassed = !failureReason.startsWith("Response verification failed");
                                reportData.put("verificationPassed", verificationPassed);
                                reportData.put("responseTimeMs", responseTimeMs);
                                Platform.runLater(() -> {
                                    testCase.statusProperty().set("Fail");
                                    reportData.put("status", "Fail");
                                    reportData.put("failureReason", failureReason);
                                    reportDataList.add(reportData);
                                });
                            }
                        }
                    }

                    // Generate HTML report after all tests are executed
                    Platform.runLater(() -> {
                        runButton.setDisable(false);
                        loadButton.setDisable(false);
                        refreshButton.setDisable(false);
                        stopButton.setDisable(true);

                        try {
                            HtmlReportGeneratorApi reportGenerator = new HtmlReportGeneratorApi();
                            reportGenerator.generateReport(reportDataList, objectMapper);
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Report Generated");
                            alert.setHeaderText("HTML Report Created");
                            alert.setContentText("Test execution report has been generated at 'report.html'.");
                            alert.showAndWait();
                        } catch (Exception e) {
                            System.err.println("Error generating report: " + e.getMessage());
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Report Generation Failed");
                            alert.setContentText("Failed to generate report: " + e.getMessage());
                            alert.showAndWait();
                        }
                    });
                    return null;
                }
            };
            new Thread(runTask).start();
        });
        runButton.setOnMouseEntered(e -> runButton.setStyle(RUN_BUTTON_HOVER_STYLE));
        runButton.setOnMouseExited(e -> runButton.setStyle(RUN_BUTTON_STYLE));

        // Stop button setup
        stopButton = new Button("Stop");
        stopButton.setStyle(STOP_BUTTON_STYLE);
        stopButton.setTooltip(new Tooltip("Stop running test cases"));
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> {
            if (runTask != null) {
                runTask.cancel();
            }
            runButton.setDisable(false);
            loadButton.setDisable(false);
            refreshButton.setDisable(false);
            stopButton.setDisable(true);
        });
        stopButton.setOnMouseEntered(e -> stopButton.setStyle(STOP_BUTTON_HOVER_STYLE));
        stopButton.setOnMouseExited(e -> stopButton.setStyle(STOP_BUTTON_STYLE));

        // Refresh button setup
        refreshButton = new Button("Refresh");
        refreshButton.setStyle(LOAD_REFRESH_BUTTON_STYLE);
        refreshButton.setTooltip(new Tooltip("Refresh test cases from the last loaded file"));
        refreshButton.setOnAction(e -> loadTestCases(primaryStage, false));
        refreshButton.setOnMouseEntered(e -> refreshButton.setStyle(LOAD_REFRESH_BUTTON_HOVER_STYLE));
        refreshButton.setOnMouseExited(e -> refreshButton.setStyle(LOAD_REFRESH_BUTTON_STYLE));

        // Button layout
        HBox buttonBox = new HBox(10, loadButton, refreshButton, runButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        // Select All layout
        HBox selectAllBox = new HBox(selectAllCheckBox);
        selectAllBox.setAlignment(Pos.CENTER_LEFT);
        selectAllBox.setPadding(new Insets(10));

        // Root layout
        VBox root = new VBox(10, selectAllBox, table, buttonBox);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #252525;");

        // Scene setup with inline CSS
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add("data:text/css," + CSS);

        // Stage setup
        primaryStage.setTitle("API Test Runner");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Updates the Select All checkbox state based on the run status of all test cases.
     */
    private void updateSelectAllState() {
        if (testCases.isEmpty()) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
            return;
        }
        boolean allSelected = true;
        boolean anySelected = false;
        for (TestCase testCase : testCases) {
            boolean isSelected = testCase.runProperty().get();
            allSelected &= isSelected;
            anySelected |= isSelected;
        }
        if (allSelected) {
            selectAllCheckBox.setSelected(true);
            selectAllCheckBox.setIndeterminate(false);
        } else if (anySelected) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(true);
        } else {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
        }
    }

    /**
     * Verifies the actual response body against the expected JSON structure.
     * Supports $any-value placeholders for dynamic value matching.
     *
     * @param actualResponseBody the actual response from the API
     * @param verifyExpression the expected JSON structure as a string
     * @param testId the ID of the test case
     * @throws Exception if verification fails
     */
    private void verifyResponse(String actualResponseBody, String verifyExpression, Integer testId) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Object actualJson;
        Object expectedJson;
        try {
            actualJson = mapper.readValue(actualResponseBody, Object.class);
        } catch (JsonProcessingException e) {
            throw new Exception("Failed to parse actual response body as JSON for Test ID " + testId + ": " + e.getMessage(), e);
        }

        // Initialize tempVerifyExpression with the original verifyExpression
        String tempVerifyExpression = verifyExpression;
        Map<String, String> pathToValueMap = new HashMap<>();

        // Handle $any-value replacement if present
        if (verifyExpression != null && verifyExpression.contains("$any-value")) {
            try {
                // Replace $any-value with "_any_value_" (quoted string) to ensure valid JSON
                tempVerifyExpression = verifyExpression
                    .replaceAll("\\$any-value\\b(?=\\s*(,|\\}|\\]))", "\"_any_value_\"")
                    .replaceAll("\"\\$any-value\"", "\"_any_value_\"");
                expectedJson = mapper.readValue(tempVerifyExpression, Object.class);
                extractAnyValuePathsFromObject(expectedJson, actualJson, pathToValueMap, "", testId);
                tempVerifyExpression = replaceAnyValueWithActual(tempVerifyExpression, pathToValueMap, testId);
            } catch (JsonProcessingException e) {
                throw new Exception("Failed to parse verifyExpression with $any-value for Test ID " + testId + ": " + e.getMessage(), e);
            }
        }

        try {
            expectedJson = mapper.readValue(tempVerifyExpression, Object.class);
        } catch (JsonProcessingException e) {
            throw new Exception("Failed to parse modified verifyExpression as JSON for Test ID " + testId + ": " + e.getMessage(), e);
        }

        compareJson(actualJson, expectedJson, "", testId);
    }

    /**
     * Extracts paths where $any-value is used in the expected JSON and maps them to actual values.
     *
     * @param expected the expected JSON object (or sub-object)
     * @param actual the actual JSON object
     * @param pathToValueMap map to store path-value mappings
     * @param path current path in the JSON structure
     * @param testId the test case ID
     * @throws Exception if extraction fails
     */
    private void extractAnyValuePathsFromObject(Object expected, Object actual, Map<String, String> pathToValueMap, String path, Integer testId) throws Exception {
        if (expected instanceof Map) {
            extractAnyValuePaths(expected, actual, pathToValueMap, path, testId);
        } else if (expected instanceof List) {
            extractAnyValuePaths(expected, actual, pathToValueMap, path, testId);
        } else if (expected instanceof String && "_any_value_".equals(expected)) {
            String formattedValue = formatJsonValue(actual, new ObjectMapper());
            pathToValueMap.put(path, formattedValue);
            System.out.println("Debug: Mapping path '" + path + "' to value '" + formattedValue + "' (type: " + (actual != null ? actual.getClass().getSimpleName() : "null") + ") for Test ID " + testId);
        }
    }

    /**
     * Recursively extracts $any-value paths from maps or lists in the expected JSON.
     *
     * @param expected the expected JSON object
     * @param actual the actual JSON object
     * @param pathToValueMap map to store path-value mappings
     * @param path current path
     * @param testId the test case ID
     * @throws Exception if extraction fails
     */
    private void extractAnyValuePaths(Object expected, Object actual, Map<String, String> pathToValueMap, String path, Integer testId) throws Exception {
        if (expected instanceof Map && actual instanceof Map) {
            Map<String, Object> expectedMap = (Map<String, Object>) expected;
            Map<String, Object> actualMap = (Map<String, Object>) actual;
            for (Map.Entry<String, Object> entry : expectedMap.entrySet()) {
                String key = entry.getKey();
                String newPath = path.isEmpty() ? key : path + "." + key;
                if (actualMap.containsKey(key)) {
                    extractAnyValuePathsFromObject(entry.getValue(), actualMap.get(key), pathToValueMap, newPath, testId);
                }
            }
        } else if (expected instanceof List && actual instanceof List) {
            List<Object> expectedList = (List<Object>) expected;
            List<Object> actualList = (List<Object>) actual;
            for (int i = 0; i < expectedList.size(); i++) {
                String newPath = path + "[" + i + "]";
                if (i < actualList.size()) {
                    extractAnyValuePathsFromObject(expectedList.get(i), actualList.get(i), pathToValueMap, newPath, testId);
                }
            }
        } else {
            extractAnyValuePathsFromObject(expected, actual, pathToValueMap, path, testId);
        }
    }

    /**
     * Formats a value as a JSON-compatible string.
     *
     * @param value the value to format
     * @param mapper JSON mapper instance
     * @return formatted string
     * @throws Exception if formatting fails
     */
    private String formatJsonValue(Object value, ObjectMapper mapper) throws Exception {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof Number || value instanceof Boolean || value == null) {
            return String.valueOf(value);
        } else {
            return mapper.writeValueAsString(value);
        }
    }

    /**
     * Replaces $any-value placeholders in the verify expression with actual values from the map.
     *
     * @param verifyExpression the original verify expression
     * @param pathToValueMap path-value mappings
     * @param testId the test case ID
     * @return modified verify expression with actual values
     * @throws Exception if replacement fails
     */
    private String replaceAnyValueWithActual(String verifyExpression, Map<String, String> pathToValueMap, Integer testId) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Object jsonObject;
        try {
            jsonObject = mapper.readValue(verifyExpression, Object.class);
        } catch (JsonProcessingException e) {
            throw new Exception("Failed to parse verifyExpression for replacement for Test ID " + testId + ": " + e.getMessage(), e);
        }

        Object modifiedJson = replaceAnyValueInJson(jsonObject, pathToValueMap, "", testId);
        return mapper.writeValueAsString(modifiedJson);
    }

    /**
     * Recursively replaces _any_value_ placeholders in the JSON object with actual values.
     *
     * @param jsonObject the JSON object to modify
     * @param pathToValueMap path-value mappings
     * @param path current path
     * @param testId the test case ID
     * @return modified JSON object
     * @throws Exception if replacement fails
     */
    private Object replaceAnyValueInJson(Object jsonObject, Map<String, String> pathToValueMap, String path, Integer testId) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        if (jsonObject instanceof Map) {
            Map<String, Object> jsonMap = (Map<String, Object>) jsonObject;
            Map<String, Object> resultMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String newPath = path.isEmpty() ? key : path + "." + key;
                if (value instanceof String && "_any_value_".equals(value)) {
                    String actualValueStr = pathToValueMap.get(newPath);
                    if (actualValueStr == null) {
                        throw new Exception("No value mapped for path '" + newPath + "' in pathToValueMap for Test ID " + testId);
                    }
                    Object actualValue;
                    try {
                        actualValue = mapper.readValue(actualValueStr, Object.class);
                    } catch (JsonProcessingException e) {
                        actualValue = actualValueStr.replaceAll("^\"|\"$", "");
                    }
                    System.out.println("Debug: Replacing _any_value_ at path '" + newPath + "' with value '" + actualValue + "' for Test ID " + testId);
                    resultMap.put(key, actualValue);
                } else {
                    resultMap.put(key, replaceAnyValueInJson(value, pathToValueMap, newPath, testId));
                }
            }
            return resultMap;
        } else if (jsonObject instanceof List) {
            List<Object> jsonList = (List<Object>) jsonObject;
            List<Object> resultList = new ArrayList<>();
            for (int i = 0; i < jsonList.size(); i++) {
                String newPath = path + "[" + i + "]";
                Object value = jsonList.get(i);
                if (value instanceof String && "_any_value_".equals(value)) {
                    String actualValueStr = pathToValueMap.get(newPath);
                    if (actualValueStr == null) {
                        throw new Exception("No value mapped for path '" + newPath + "' in pathToValueMap for Test ID " + testId);
                    }
                    Object actualValue;
                    try {
                        actualValue = mapper.readValue(actualValueStr, Object.class);
                    } catch (JsonProcessingException e) {
                        actualValue = actualValueStr.replaceAll("^\"|\"$", "");
                    }
                    System.out.println("Debug: Replacing _any_value_ at path '" + newPath + "' with value '" + actualValue + "' for Test ID " + testId);
                    resultList.add(actualValue);
                } else {
                    resultList.add(replaceAnyValueInJson(value, pathToValueMap, newPath, testId));
                }
            }
            return resultList;
        } else if (jsonObject instanceof String && "_any_value_".equals(jsonObject)) {
            String actualValueStr = pathToValueMap.get(path);
            if (actualValueStr == null) {
                throw new Exception("No value mapped for path '" + path + "' in pathToValueMap for Test ID " + testId);
            }
            Object actualValue;
            try {
                actualValue = mapper.readValue(actualValueStr, Object.class);
            } catch (JsonProcessingException e) {
                actualValue = actualValueStr.replaceAll("^\"|\"$", "");
            }
            System.out.println("Debug: Replacing _any_value_ at path '" + path + "' with value '" + actualValue + "' for Test ID " + testId);
            return actualValue;
        } else {
            return jsonObject;
        }
    }

    /**
     * Recursively compares two JSON objects for structural and value equality.
     *
     * @param actual the actual JSON object
     * @param expected the expected JSON object
     * @param path current path for error reporting
     * @param testId the test case ID
     * @throws Exception if comparison fails
     */
    private void compareJson(Object actual, Object expected, String path, Integer testId) throws Exception {
        if (expected instanceof Map) {
            if (!(actual instanceof Map)) {
                throw new Exception("Type mismatch at path '" + path + "' for Test ID " + testId +
                    ": expected Map, got " + actual.getClass().getSimpleName());
            }
            Map<String, Object> actualMap = (Map<String, Object>) actual;
            Map<String, Object> expectedMap = (Map<String, Object>) expected;
            for (Map.Entry<String, Object> entry : expectedMap.entrySet()) {
                String key = entry.getKey();
                Object expectedValue = entry.getValue();
                String newPath = path.isEmpty() ? key : path + "." + key;
                if (!actualMap.containsKey(key)) {
                    throw new Exception("Key not found in response at path '" + newPath + "' for Test ID " + testId);
                }
                compareJson(actualMap.get(key), expectedValue, newPath, testId);
            }
        } else if (expected instanceof List) {
            if (!(actual instanceof List)) {
                throw new Exception("Type mismatch at path '" + path + "' for Test ID " + testId +
                    ": expected List, got " + actual.getClass().getSimpleName());
            }
            List<Object> actualList = (List<Object>) actual;
            List<Object> expectedList = (List<Object>) expected;
            if (actualList.size() != expectedList.size()) {
                throw new Exception("List size mismatch at path '" + path + "' for Test ID " + testId +
                    ": expected " + expectedList.size() + ", got " + actualList.size());
            }
            for (int i = 0; i < expectedList.size(); i++) {
                String newPath = path + "[" + i + "]";
                compareJson(actualList.get(i), expectedList.get(i), newPath, testId);
            }
        } else {
            if (!Objects.equals(actual, expected)) {
                throw new Exception("Value mismatch at path '" + path + "' for Test ID " + testId +
                    ": expected '" + expected + "', got '" + actual + "'");
            }
        }
    }

    /**
     * Retrieves the vertical scrollbar from the table for scroll position tracking.
     *
     * @return the vertical ScrollBar or null if not found
     */
    private ScrollBar getVerticalScrollBar() {
        for (javafx.scene.Node node : table.lookupAll(".scroll-bar:vertical")) {
            if (node instanceof ScrollBar) {
                return (ScrollBar) node;
            }
        }
        return null;
    }

    /**
     * Replaces placeholders in text (e.g., {{var}}) with values from environment variables.
     * Handles missing or null values by replacing with an empty string.
     *
     * @param text the text containing placeholders
     * @param envVars map of environment variables
     * @param testId the test case ID for logging
     * @return text with placeholders replaced
     */
    private String replacePlaceholders(String text, Map<String, String> envVars, Integer testId) {
        if (text == null || !text.contains("{{")) {
            return text;
        }

        try {
            Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
            Matcher matcher = pattern.matcher(text);
            StringBuffer modifiedText = new StringBuffer();

            while (matcher.find()) {
                String placeholder = matcher.group(1);
                String replacement;
                if (!envVars.containsKey(placeholder)) {
                    replacement = "";
                    System.out.println("Debug: Placeholder '" + placeholder + "' not found in env.json for Test ID " + testId + ", replacing with empty string");
                } else {
                    String value = envVars.get(placeholder);
                    if (value == null || value.trim().isEmpty()) {
                        replacement = "";
                        System.out.println("Debug: Placeholder '" + placeholder + "' has null/empty value in env.json for Test ID " + testId + ", replacing with empty string");
                    } else {
                        replacement = value;
                        System.out.println("Debug: Replaced placeholder '" + placeholder + "' with value '" + value + "' for Test ID " + testId);
                    }
                }
                matcher.appendReplacement(modifiedText, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(modifiedText);
            return modifiedText.toString();
        } catch (Exception e) {
            System.err.println("Error replacing placeholders" + (testId != null ? " for Test ID " + testId : "") + ": " + e.getMessage());
            return text;
        }
    }

    /**
     * Parses a nested key path (e.g., "a.b.c") into a list of parts.
     * Handles array indices if present.
     *
     * @param key the key path string
     * @return list of path parts
     */
    private List<String> parseKeyPath(String key) {
        List<String> parts = new ArrayList<>();
        if (key == null || key.trim().isEmpty()) {
            System.err.println("Error: Empty or invalid key path: " + key);
            return parts;
        }

        String[] split = key.split("\\.(?![^\\[]*\\])");
        for (String part : split) {
            part = part.trim();
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        if (parts.isEmpty()) {
            System.err.println("Error: Parsed key path is empty for key: " + key);
        } else {
            System.out.println("Debug: Parsed key path for '" + key + "': " + parts);
        }
        return parts;
    }

    /**
     * Parses a value to match the type of the existing value in the parent object.
     * Defaults to string if type mismatch or parsing fails.
     *
     * @param value the value string to parse
     * @param parent the parent object
     * @param finalPart the final key part
     * @param testId the test case ID
     * @return parsed object
     */
    private Object parseValue(String value, Object parent, String finalPart, Integer testId) {
        Object existingValue = null;
        if (parent instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) parent;
            existingValue = map.get(finalPart);
        }

        if (existingValue instanceof Number) {
            try {
                if (existingValue instanceof Integer || existingValue instanceof Long) {
                    return Integer.parseInt(value);
                } else if (existingValue instanceof Double || existingValue instanceof Float) {
                    return Double.parseDouble(value);
                }
            } catch (NumberFormatException e) {
                System.err.println("Warning: Failed to parse value '" + value + "' as number for Test ID " + testId + ", using string instead");
            }
        } else if (existingValue instanceof Boolean) {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(value);
            } else {
                System.err.println("Warning: Value '" + value + "' is not a boolean for Test ID " + testId + ", using string instead");
            }
        }

        return value;
    }

    /**
     * Sets a nested value in a JSON object using a key path (e.g., "a.b.c").
     * Creates type-compatible values and logs traversal.
     *
     * @param jsonObj the JSON object to modify
     * @param key the nested key path
     * @param value the value to set
     * @param testId the test case ID
     * @return true if successful, false otherwise
     */
    private boolean setNestedValue(Map<String, Object> jsonObj, String key, String value, Integer testId) {
        try {
            List<String> parts = parseKeyPath(key);
            if (parts.isEmpty()) {
                System.err.println("Error: Empty key path for '" + key + "' in Test ID " + testId);
                return false;
            }

            System.out.println("Debug: Processing key path '" + key + "' with parts: " + parts + " for Test ID " + testId);

            Map<String, Object> current = jsonObj;
            if (parts.size() > 1) {
                for (int i = 0; i < parts.size() - 1; i++) {
                    String part = parts.get(i);
                    System.out.println("Debug: Traversing part '" + part + "' at index " + i + ", current object: " + new ObjectMapper().writeValueAsString(current));

                    if (!current.containsKey(part)) {
                        System.err.println("Error: Key '" + part + "' not found in path '" + key + "' for Test ID " + testId);
                        return false;
                    }
                    Object next = current.get(part);
                    if (!(next instanceof Map)) {
                        System.err.println("Error: Path '" + key + "' for Test ID " + testId + " is invalid: '" + part + "' is not a map");
                        return false;
                    }
                    current = (Map<String, Object>) next;
                    System.out.println("Debug: Navigated to key '" + part + "', current object: " + new ObjectMapper().writeValueAsString(current));
                }
            }

            String finalPart = parts.get(parts.size() - 1);
            if (!current.containsKey(finalPart)) {
                System.err.println("Error: Final key '" + finalPart + "' not found in path '" + key + "' for Test ID " + testId);
                return false;
            }
            System.out.println("Debug: Setting final part '" + finalPart + "' in object: " + new ObjectMapper().writeValueAsString(current));
            Object parsedValue = parseValue(value, current, finalPart, testId);
            current.put(finalPart, parsedValue);
            System.out.println("Debug: Updated key '" + finalPart + "' to value: " + parsedValue + " in map: " + new ObjectMapper().writeValueAsString(current));
            System.out.println("Debug: Final JSON object after update: " + new ObjectMapper().writeValueAsString(jsonObj));
            return true;
        } catch (Exception e) {
            System.err.println("Error setting nested value for key '" + key + "' in Test ID " + testId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a nested value from a JSON object using a key path.
     *
     * @param jsonObj the JSON object
     * @param key the nested key path
     * @param testId the test case ID
     * @return the value or null if not found
     */
    private Object getNestedValue(Map<String, Object> jsonObj, String key, Integer testId) {
        List<String> parts = parseKeyPath(key);
        if (parts.isEmpty()) {
            System.err.println("Error: Empty key path for getNestedValue '" + key + "' in Test ID " + testId);
            return null;
        }

        Object current = jsonObj;
        for (String part : parts) {
            if (current instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) current;
                if (!map.containsKey(part)) {
                    System.err.println("Error: Key '" + part + "' not found in path '" + key + "' for Test ID " + testId);
                    return null;
                }
                current = map.get(part);
            } else {
                System.err.println("Error: Path '" + key + "' for Test ID " + testId + " is invalid: '" + part + "' is not a map");
                return null;
            }
        }
        return current;
    }

    /**
     * Loads or refreshes test cases from an Excel file.
     * Validates headers, parses rows, and populates UI and data maps.
     *
     * @param primaryStage the application stage
     * @param promptForFile true to show file chooser, false to use last loaded file
     */
    private void loadTestCases(Stage primaryStage, boolean promptForFile) {
        File file = lastLoadedFile;
        if (promptForFile) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Test Cases");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Test Suites", "*.xlsx"));
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Documents"));
            file = fileChooser.showOpenDialog(primaryStage);
        }

        if (file != null) {
            String testSuiteName = file.getName().substring(0, file.getName().lastIndexOf('.'));
            try (FileInputStream fileIn = new FileInputStream(file);
                 XSSFWorkbook workbook = new XSSFWorkbook(fileIn)) {
                Sheet sheet = workbook.getSheetAt(0);

                // Validate header row
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    showError("No header row found in test suite '" + testSuiteName + "'.");
                    return;
                }

                // Clear existing data
                testCases.clear();
                testDataMap.clear();
                headersMap.clear();
                paramsMap.clear();
                modifyPayloadMap.clear();
                responseCaptureMap.clear();
                authMap.clear();

                // Map headers to column indices
                Map<String, Integer> headerMap = new HashMap<>();
                int responseTimeIndex = -1;
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    String header = headerRow.getCell(i) != null ? headerRow.getCell(i).getStringCellValue().trim() : "";
                    headerMap.put(header, i);
                    if ("Response Time (ms)".equalsIgnoreCase(header)) {
                        responseTimeIndex = i;
                    }
                }

                // Validate required headers
                for (String requiredHeader : REQUIRED_HEADERS) {
                    if (!headerMap.containsKey(requiredHeader) && !requiredHeader.isEmpty()) {
                        showError("Missing required column '" + requiredHeader + "' in test suite '" + testSuiteName + "'.");
                        return;
                    }
                }

                // Parse test cases
                Integer lastTestId = null;
                HashMap<String, Object> currentTestData = null;
                HashMap<String, Object> currentHeaders = null;
                HashMap<String, Object> currentParams = null;
                HashMap<String, Object> currentModifyPayload = null;
                HashMap<String, Object> currentResponseCapture = null;
                HashMap<String, Object> currentAuthDetails = null;

                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;

                    String testIdStr = getCellValue(row, headerMap.get("Test ID"));
                    Integer testId = testIdStr != null && !testIdStr.trim().isEmpty() ? Integer.parseInt(testIdStr.trim()) : null;

                    if (testId != null) {
                        // New test case
                        if (lastTestId != null && !lastTestId.equals(testId)) {
                            // Save previous test case data
                            testDataMap.put(lastTestId, currentTestData);
                            headersMap.put(lastTestId, currentHeaders);
                            paramsMap.put(lastTestId, currentParams);
                            modifyPayloadMap.put(lastTestId, currentModifyPayload);
                            responseCaptureMap.put(lastTestId, currentResponseCapture);
                            authMap.put(lastTestId, currentAuthDetails);
                        }

                        lastTestId = testId;
                        String testDescription = getCellValue(row, headerMap.get("Test Description"));
                        testCases.add(new TestCase(true, testId.toString(), testDescription, "No Run"));

                        // Initialize maps for new test case
                        currentTestData = new HashMap<>();
                        currentTestData.put("Request", getCellValue(row, headerMap.get("Request")));
                        currentTestData.put("End-Point", getCellValue(row, headerMap.get("End-Point")));
                        currentTestData.put("Payload", getCellValue(row, headerMap.get("Payload")));
                        currentTestData.put("Payload Type", getCellValue(row, headerMap.get("Payload Type")));
                        currentTestData.put("Expected Status", getCellValue(row, headerMap.get("Expected Status")));
                        currentTestData.put("Verify Response", getCellValue(row, headerMap.get("Verify Response")));
                        currentTestData.put("SSL Validation", getCellValue(row, headerMap.get("SSL Validation")));

                        currentHeaders = new HashMap<>();
                        currentParams = new HashMap<>();
                        currentModifyPayload = new HashMap<>();
                        currentResponseCapture = new HashMap<>();
                        currentAuthDetails = new HashMap<>();
                        currentAuthDetails.put("Type", getCellValue(row, headerMap.get("Authorization")));

                        // Add header, param, modify payload, response capture, and auth data
                        addMultiColumnData(row, headerMap, "Header (key)", "Header (value)", currentHeaders);
                        addMultiColumnData(row, headerMap, "Parameter (key)", "Parameter (value)", currentParams);
                        addMultiColumnData(row, headerMap, "Modify Payload (key)", "Modify Payload (value)", currentModifyPayload);
                        addMultiColumnData(row, headerMap, "Response (key) Name", "Capture (key) Value (env var)", currentResponseCapture);

                        String username = getCellValue(row, headerMap.get("Username"));
                        String password = getCellValue(row, headerMap.get("Password"));
                        String token = getCellValue(row, headerMap.get("Token"));
                        if (username != null) currentAuthDetails.put("Username", username);
                        if (password != null) currentAuthDetails.put("Password", password);
                        if (token != null) currentAuthDetails.put("Token", token);
                    } else if (lastTestId != null) {
                        // Continue adding data to the current test case
                        addMultiColumnData(row, headerMap, "Header (key)", "Header (value)", currentHeaders);
                        addMultiColumnData(row, headerMap, "Parameter (key)", "Parameter (value)", currentParams);
                        addMultiColumnData(row, headerMap, "Modify Payload (key)", "Modify Payload (value)", currentModifyPayload);
                        addMultiColumnData(row, headerMap, "Response (key) Name", "Capture (key) Value (env var)", currentResponseCapture);
                    }
                }

                // Save the last test case data
                if (lastTestId != null) {
                    testDataMap.put(lastTestId, currentTestData);
                    headersMap.put(lastTestId, currentHeaders);
                    paramsMap.put(lastTestId, currentParams);
                    modifyPayloadMap.put(lastTestId, currentModifyPayload);
                    responseCaptureMap.put(lastTestId, currentResponseCapture);
                    authMap.put(lastTestId, currentAuthDetails);
                }

                lastLoadedFile = file;
                System.out.println("Debug: Loaded test cases from '" + file.getAbsolutePath() + "'. Test IDs: " + testDataMap.keySet());
            } catch (Exception e) {
                showError("Failed to load test suite '" + testSuiteName + "': " + e.getMessage());
                System.err.println("Error loading test suite: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Adds multi-column data (e.g., headers, parameters) to a map.
     *
     * @param row the current row
     * @param headerMap map of headers to column indices
     * @param keyHeader the header for the key column
     * @param valueHeader the header for the value column
     * @param targetMap the map to store key-value pairs
     */
    private void addMultiColumnData(Row row, Map<String, Integer> headerMap, String keyHeader, String valueHeader, HashMap<String, Object> targetMap) {
        String key = getCellValue(row, headerMap.get(keyHeader));
        String value = getCellValue(row, headerMap.get(valueHeader));
        if (key != null && !key.trim().isEmpty()) {
            targetMap.put(key, value != null ? value : "");
        }
    }

    /**
     * Retrieves the string value of a cell, handling different cell types.
     *
     * @param row the row to read from
     * @param columnIndex the column index
     * @return the cell value as a string, or null if empty
     */
    private String getCellValue(Row row, Integer columnIndex) {
        if (row == null || columnIndex == null || columnIndex < 0) {
            return null;
        }
        var cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double numericValue = cell.getNumericCellValue();
                if (numericValue == (int) numericValue) {
                    return String.valueOf((int) numericValue);
                }
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    /**
     * Displays an error alert to the user.
     *
     * @param message the error message to display
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Operation Failed");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Main method to launch the JavaFX application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}