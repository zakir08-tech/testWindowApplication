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
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class RunApiTest extends Application {

    private static final String FIELD_STYLE_UNFOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #BBBBBB; -fx-border-radius: 5px;";

    private static final String RUN_BUTTON_STYLE = 
        "-fx-background-color: #90EE90; -fx-text-fill: black; -fx-border-radius: 5px; -fx-min-width: 100px;";

    private static final String RUN_BUTTON_HOVER_STYLE = 
        "-fx-background-color: #98FB98; -fx-text-fill: black; -fx-border-radius: 5px; -fx-min-width: 100px;";

    private static final String STOP_BUTTON_STYLE = 
        "-fx-background-color: #FFB6C1; -fx-text-fill: black; -fx-border-radius: 5px; -fx-min-width: 100px;";

    private static final String STOP_BUTTON_HOVER_STYLE = 
        "-fx-background-color: #FFC1CC; -fx-text-fill: black; -fx-border-radius: 5px; -fx-min-width: 100px;";

    private static final String LOAD_REFRESH_BUTTON_STYLE = 
        "-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

    private static final String LOAD_REFRESH_BUTTON_HOVER_STYLE = 
        "-fx-background-color: #6AB0FF; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

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

    private static class TestCase {
        private final BooleanProperty run;
        private final StringProperty testId;
        private final StringProperty testDescription;
        private final StringProperty status;

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

    private TableView<TestCase> table;
    private ObservableList<TestCase> testCases;
    private CheckBox selectAllCheckBox;
    private Button loadButton;
    private Button runButton;
    private Button stopButton;
    private Button refreshButton;
    private File lastLoadedFile;
    private Task<Void> runTask;
    private double lastScrollPosition = 0.0;

    private HashMap<Integer, HashMap<String, Object>> testDataMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> headersMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> paramsMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> modifyPayloadMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> responseCaptureMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> authMap = new HashMap<>();
    private List<Map<String, Object>> reportDataList = new ArrayList<>();

    private static final Set<String> REQUIRED_HEADERS = new HashSet<>(Arrays.asList(
        "Test ID", "Request", "End-Point", "Header (key)", "Header (value)", 
        "Parameter (key)", "Parameter (value)", "Payload", "Payload Type", 
        "Modify Payload (key)", "Modify Payload (value)", "Response (key) Name", 
        "Capture (key) Value (env var)", "Authorization", "", "", 
        "SSL Validation", "Expected Status", "Verify Response", 
        "Test Description"
    ));

    @Override
    public void start(Stage primaryStage) {
        testCases = FXCollections.observableArrayList();
        table = new TableView<>(testCases);
        table.setStyle("-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; -fx-table-cell-border-color: #3C3F41; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");

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

        TableColumn<TestCase, String> testIdColumn = new TableColumn<>("Test ID");
        testIdColumn.setCellValueFactory(cellData -> cellData.getValue().testIdProperty());
        testIdColumn.setMinWidth(TEST_ID_COL_WIDTH);
        testIdColumn.setMaxWidth(TEST_ID_COL_WIDTH);
        testIdColumn.setPrefWidth(TEST_ID_COL_WIDTH);
        testIdColumn.setResizable(false);
        testIdColumn.setStyle("-fx-alignment: CENTER;");

        TableColumn<TestCase, String> testDescriptionColumn = new TableColumn<>("Test Description");
        testDescriptionColumn.setCellValueFactory(cellData -> cellData.getValue().testDescriptionProperty());
        testDescriptionColumn.setMinWidth(TEST_DESC_MIN_WIDTH);
        testDescriptionColumn.setPrefWidth(TEST_DESC_MIN_WIDTH);

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

        table.getColumns().addAll(runColumn, testIdColumn, testDescriptionColumn, statusColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setEditable(true);

        HBox.setHgrow(table, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.ALWAYS);

        selectAllCheckBox = new CheckBox("Select All");
        selectAllCheckBox.setStyle(FIELD_STYLE_UNFOCUSED);
        selectAllCheckBox.setSelected(false);
        selectAllCheckBox.setOnAction(e -> {
            boolean selected = selectAllCheckBox.isSelected();
            for (TestCase testCase : testCases) {
                testCase.runProperty().set(selected);
            }
        });

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

        table.setOnScroll(event -> {
            ScrollBar verticalScrollBar = getVerticalScrollBar();
            if (verticalScrollBar != null && verticalScrollBar.isVisible()) {
                lastScrollPosition = verticalScrollBar.getValue();
            }
        });

        loadButton = new Button("Load");
        loadButton.setStyle(LOAD_REFRESH_BUTTON_STYLE);
        loadButton.setTooltip(new Tooltip("Load test cases from an Excel test suite"));
        loadButton.setOnAction(e -> loadTestCases(primaryStage, true));
        loadButton.setOnMouseEntered(e -> loadButton.setStyle(LOAD_REFRESH_BUTTON_HOVER_STYLE));
        loadButton.setOnMouseExited(e -> loadButton.setStyle(LOAD_REFRESH_BUTTON_STYLE));

        runButton = new Button("Run");
        runButton.setStyle(RUN_BUTTON_STYLE);
        runButton.setTooltip(new Tooltip("Run selected test cases"));
        runButton.setOnAction(e -> {
            reportDataList.clear(); // Clear previous report data
            runTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Platform.runLater(() -> {
                        runButton.setDisable(true);
                        loadButton.setDisable(true);
                        refreshButton.setDisable(true);
                        stopButton.setDisable(false);
                    });

                    ApiExecutor apiExecutor = new ApiExecutor();
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, String> envVars = new HashMap<>();

                    File envFile = new File("env.json");
                    if (envFile.exists()) {
                        envVars = objectMapper.readValue(envFile, HashMap.class);
                    } else {
                        System.err.println("env.json not found in project root. No environment variables will be replaced.");
                    }

                    for (TestCase testCase : testCases) {
                        if (isCancelled()) {
                            break;
                        }
                        if (testCase.runProperty().get()) {
                            Integer testId = Integer.parseInt(testCase.testIdProperty().get());
                            int index = testCases.indexOf(testCase);
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

                            // Initialize variables outside try block to ensure availability in catch block
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

                            // Create a map to store report data for this test case
                            Map<String, Object> reportData = new HashMap<>();
                            reportData.put("testId", testId.toString());
                            reportData.put("description", testCase.testDescriptionProperty().get());
                            reportData.put("request", method);
                            reportData.put("verifyResponse", verifyResponse);

                            try {
                                System.out.println("Debug: Starting processing for Test ID " + testId);
                                HashMap<String, Object> headers = headersMap.get(testId);
                                HashMap<String, Object> params = paramsMap.get(testId);
                                HashMap<String, Object> modifyPayload = modifyPayloadMap.get(testId);
                                HashMap<String, Object> authDetails = authMap.get(testId);
                                HashMap<String, Object> responseCapture = responseCaptureMap.get(testId);

                                System.out.println("Debug: Replacing placeholders in URL for Test ID " + testId);
                                String processedUrl = replacePlaceholders(url, envVars, testId);
                                testData.put("End-Point", processedUrl); // Update testDataMap with processed URL
                                reportData.put("endpoint", processedUrl);

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

                                System.out.println("Debug: Processing modify payload for Test ID " + testId);
                                HashMap<String, Object> processedModifyPayload = new HashMap<>();
                                for (Map.Entry<String, Object> entry : modifyPayload.entrySet()) {
                                    String modifyKey = entry.getKey();
                                    String modifyValue = entry.getValue() != null ? entry.getValue().toString() : "";
                                    String processedValue = replacePlaceholders(modifyValue, envVars, testId);
                                    processedModifyPayload.put(modifyKey, processedValue);
                                }
                                System.out.println("Debug: Processed modify payload for Test ID " + testId + ": " + processedModifyPayload);

                                if (payload != null && !payload.trim().isEmpty() && !processedModifyPayload.isEmpty()) {
                                    System.out.println("Debug: Parsing and modifying payload for Test ID " + testId);
                                    try {
                                        // Validate JSON payload before parsing
                                        try {
                                            objectMapper.readTree(payload);
                                        } catch (JsonProcessingException ex) {
                                            throw new Exception("Invalid JSON in payload for Test ID " + testId + ": " + ex.getMessage());
                                        }
                                        Map<String, Object> payloadObj = objectMapper.readValue(payload, HashMap.class);
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
                                        testData.put("Payload", modifiedPayload); // Update testDataMap with modified payload
                                    } catch (IOException ex) {
                                        System.err.println("Error parsing or modifying payload for Test ID " + testId + ": " + ex.getMessage());
                                        throw new Exception("Payload modification failed: " + ex.getMessage(), ex);
                                    }
                                }
                                reportData.put("payload", modifiedPayload);

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

                                System.out.println("Debug: Payload being sent to executeTest for Test ID " + testId + ": " + modifiedPayload);
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

                                // Store response data for report
                                reportData.put("responseStatus", response.getStatusCode());
                                reportData.put("responseBody", response.getBody());

                                // Verify status code
                                if (response.getStatusCode() != expectedStatus) {
                                    throw new Exception("Status code mismatch for Test ID " + testId + 
                                        ": expected " + expectedStatus + ", got " + response.getStatusCode());
                                }

                                // Process response capture
                                if (!responseCapture.isEmpty()) {
                                    System.out.println("Debug: Starting response capture for Test ID " + testId);
                                    Map<String, Object> responseObj;
                                    try {
                                        responseObj = objectMapper.readValue(response.getBody(), HashMap.class);
                                    } catch (Exception ex) {
                                        throw new Exception("Failed to parse response body as JSON for capture in Test ID " + testId + ": " + ex.getMessage(), ex);
                                    }

                                    int captureCount = 0;
                                    for (Map.Entry<String, Object> entry : responseCapture.entrySet()) {
                                        String responsePath = entry.getKey();
                                        String envVarName = entry.getValue() != null ? entry.getValue().toString() : "";
                                        if (responsePath == null || responsePath.trim().isEmpty() || envVarName == null || envVarName.trim().isEmpty()) {
                                            System.err.println("Warning: Invalid response capture entry for Test ID " + testId + ": path='" + responsePath + "', envVar='" + envVarName + "'");
                                            continue;
                                        }
                                        System.out.println("Debug: Attempting to capture value for path '" + responsePath + "' to env var '" + envVarName + "' for Test ID " + testId);
                                        Object capturedValue = getNestedValue(responseObj, responsePath, testId);
                                        if (capturedValue != null) {
                                            envVars.put(envVarName, capturedValue.toString());
                                            captureCount++;
                                            System.out.println("Debug: Captured value '" + capturedValue + "' from path '" + responsePath + "' and saved to env var '" + envVarName + "' for Test ID " + testId);
                                        } else {
                                            System.err.println("Warning: No value found at path '" + responsePath + "' in response for Test ID " + testId);
                                        }
                                    }
                                    System.out.println("Debug: Total values captured for Test ID " + testId + ": " + captureCount + " out of " + responseCapture.size() + " entries");

                                    // Save updated envVars back to env.json
                                    try {
                                        objectMapper.writeValue(envFile, envVars);
                                        System.out.println("Debug: Updated env.json with " + captureCount + " captured values for Test ID " + testId);
                                    } catch (IOException ex) {
                                        throw new Exception("Failed to write captured values to env.json for Test ID " + testId + ": " + ex.getMessage(), ex);
                                    }
                                } else {
                                    System.out.println("Debug: No response capture entries defined for Test ID " + testId);
                                }

                                // Verify response body if specified
                                if (processedVerifyResponse != null && !processedVerifyResponse.trim().isEmpty()) {
                                    System.out.println("Debug: Verifying response for Test ID " + testId);
                                    verifyResponse(response.getBody(), processedVerifyResponse, testId);
                                }

                                System.out.println("Test ID: " + testId);
                                System.out.println("Test Data: " + testData);
                                System.out.println("Headers: " + processedHeaders);
                                System.out.println("Parameters: " + processedParams);
                                System.out.println("Modified Payload: " + modifiedPayload);
                                System.out.println("Modify Payload Map: " + processedModifyPayload);
                                System.out.println("Response Capture: " + responseCapture);
                                System.out.println("Authorization: " + processedAuthDetails);
                                System.out.println("Expected Status: " + processedExpectedStatusStr);
                                System.out.println("Response Status: " + response.getStatusCode());
                                try {
                                    System.out.println("Response Body (Pretty JSON):\n" + ApiExecutor.toPrettyJson(response));
                                } catch (IOException ex) {
                                    System.out.println("Response Body: " + response.getBody());
                                }
                                Platform.runLater(() -> {
                                    testCase.statusProperty().set("Pass");
                                    reportData.put("status", "Pass");
                                    reportData.put("failureReason", ""); // No failure reason for passing tests
                                    reportDataList.add(reportData);
                                });
                            } catch (Exception ex) {
                                String failureReason = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                                System.err.println("Error executing Test ID " + testId + ": " + failureReason);
                                ex.printStackTrace();
                                Platform.runLater(() -> {
                                    testCase.statusProperty().set("Fail");
                                    reportData.put("status", "Fail");
                                    reportData.put("failureReason", failureReason); // Store failure reason
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

                        // Generate the HTML report
                        HtmlReportGeneratorApi reportGenerator = new HtmlReportGeneratorApi();
                        reportGenerator.generateReport(reportDataList, objectMapper);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Report Generated");
                        alert.setHeaderText("HTML Report Created");
                        alert.setContentText("Test execution report has been generated at 'report.html'.");
                        alert.showAndWait();
                    });
                    return null;
                }
            };
            new Thread(runTask).start();
        });
        runButton.setOnMouseEntered(e -> runButton.setStyle(RUN_BUTTON_HOVER_STYLE));
        runButton.setOnMouseExited(e -> runButton.setStyle(RUN_BUTTON_STYLE));

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

        refreshButton = new Button("Refresh");
        refreshButton.setStyle(LOAD_REFRESH_BUTTON_STYLE);
        refreshButton.setTooltip(new Tooltip("Refresh test cases from the last loaded file"));
        refreshButton.setOnAction(e -> loadTestCases(primaryStage, false));
        refreshButton.setOnMouseEntered(e -> refreshButton.setStyle(LOAD_REFRESH_BUTTON_HOVER_STYLE));
        refreshButton.setOnMouseExited(e -> refreshButton.setStyle(LOAD_REFRESH_BUTTON_STYLE));

        HBox buttonBox = new HBox(10, loadButton, refreshButton, runButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        HBox selectAllBox = new HBox(selectAllCheckBox);
        selectAllBox.setAlignment(Pos.CENTER_LEFT);
        selectAllBox.setPadding(new Insets(10));

        VBox root = new VBox(10, selectAllBox, table, buttonBox);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #252525;");

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add("data:text/css," + CSS);

        primaryStage.setTitle("API Test Runner");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

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

    private String formatJsonValue(Object value, ObjectMapper mapper) throws Exception {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof Number || value instanceof Boolean || value == null) {
            return String.valueOf(value);
        } else {
            return mapper.writeValueAsString(value);
        }
    }

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

    private ScrollBar getVerticalScrollBar() {
        for (javafx.scene.Node node : table.lookupAll(".scroll-bar:vertical")) {
            if (node instanceof ScrollBar) {
                return (ScrollBar) node;
            }
        }
        return null;
    }

    private String replacePlaceholders(String text, Map<String, String> envVars, Integer testId) {
        if (text == null || !text.contains("{{")) {
            return text;
        }

        try {
            Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            StringBuffer modifiedText = new StringBuffer();

            while (matcher.find()) {
                String placeholder = matcher.group(1);
                String value = envVars.get(placeholder);
                if (value != null) {
                    matcher.appendReplacement(modifiedText, java.util.regex.Matcher.quoteReplacement(value));
                } else {
                    String errorMessage = "No value found in env.json for placeholder: " + placeholder;
                    if (testId != null) {
                        errorMessage += " in Test ID " + testId;
                    }
                    System.err.println(errorMessage);
                    matcher.appendReplacement(modifiedText, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
                }
            }
            matcher.appendTail(modifiedText);
            return modifiedText.toString();
        } catch (Exception e) {
            System.err.println("Error replacing placeholders" + (testId != null ? " for Test ID " + testId : "") + ": " + e.getMessage());
            return text;
        }
    }

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
                
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    showError("No header row found in test suite '" + testSuiteName + "'.");
                    return;
                }

                Set<String> actualHeaders = new HashSet<>();
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    String header = headerRow.getCell(i) != null ? headerRow.getCell(i).getStringCellValue().trim() : "";
                    actualHeaders.add(header);
                }

                for (String reqHeader : REQUIRED_HEADERS) {
                    if (!reqHeader.isEmpty() && !actualHeaders.contains(reqHeader)) {
                        showError("Missing required header '" + reqHeader + "' in test suite '" + testSuiteName + "'.");
                        return;
                    }
                }

                if (sheet.getLastRowNum() < 1) {
                    showError("No data rows found in test suite '" + testSuiteName + "'.");
                    return;
                }

                testCases.clear();
                testDataMap.clear();
                headersMap.clear();
                paramsMap.clear();
                modifyPayloadMap.clear();
                responseCaptureMap.clear();
                authMap.clear();

                int testIdIndex = -1, requestIndex = -1, endPointIndex = -1, headerKeyIndex = -1, headerValueIndex = -1;
                int paramKeyIndex = -1, paramValueIndex = -1, payloadIndex = -1, payloadTypeIndex = -1;
                int modifyPayloadKeyIndex = -1, modifyPayloadValueIndex = -1, responseKeyNameIndex = -1;
                int captureKeyValueIndex = -1, authorizationIndex = -1, authField1Index = -1, authField2Index = -1;
                int sslValidationIndex = -1, expectedStatusIndex = -1, verifyResponseIndex = -1, testDescriptionIndex = -1;

                int emptyHeaderCount = 0;
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    String header = headerRow.getCell(i) != null ? headerRow.getCell(i).getStringCellValue().trim() : "";
                    switch (header) {
                        case "Test ID": testIdIndex = i; break;
                        case "Request": requestIndex = i; break;
                        case "End-Point": endPointIndex = i; break;
                        case "Header (key)": headerKeyIndex = i; break;
                        case "Header (value)": headerValueIndex = i; break;
                        case "Parameter (key)": paramKeyIndex = i; break;
                        case "Parameter (value)": paramValueIndex = i; break;
                        case "Payload": payloadIndex = i; break;
                        case "Payload Type": payloadTypeIndex = i; break;
                        case "Modify Payload (key)": modifyPayloadKeyIndex = i; break;
                        case "Modify Payload (value)": modifyPayloadValueIndex = i; break;
                        case "Response (key) Name": responseKeyNameIndex = i; break;
                        case "Capture (key) Value (env var)": captureKeyValueIndex = i; break;
                        case "Authorization": 
                            authorizationIndex = i; 
                            if (i + 1 < headerRow.getLastCellNum() && 
                                (headerRow.getCell(i + 1) == null || headerRow.getCell(i + 1).getStringCellValue().trim().isEmpty())) {
                                authField1Index = i + 1;
                            }
                            if (i + 2 < headerRow.getLastCellNum() && 
                                (headerRow.getCell(i + 2) == null || headerRow.getCell(i + 2).getStringCellValue().trim().isEmpty())) {
                                authField2Index = i + 2;
                            }
                            break;
                        case "SSL Validation": sslValidationIndex = i; break;
                        case "Expected Status": expectedStatusIndex = i; break;
                        case "Verify Response": verifyResponseIndex = i; break;
                        case "Test Description": testDescriptionIndex = i; break;
                        case "": emptyHeaderCount++; break;
                    }
                }

                if (authField1Index == -1 || authField2Index == -1) {
                    showError("Invalid headers in test suite '" + testSuiteName + "'. Missing empty headers after Authorization.");
                    return;
                }

                Map<Integer, List<Row>> rowsByTestId = new HashMap<>();
                Integer currentTestId = null;
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        String testIdStr = row.getCell(testIdIndex) != null ? row.getCell(testIdIndex).toString().trim() : "";
                        if (!testIdStr.isEmpty()) {
                            try {
                                currentTestId = Integer.parseInt(testIdStr);
                                rowsByTestId.computeIfAbsent(currentTestId, k -> new ArrayList<>()).add(row);
                            } catch (NumberFormatException e) {
                                continue;
                            }
                        } else if (currentTestId != null) {
                            rowsByTestId.computeIfAbsent(currentTestId, k -> new ArrayList<>()).add(row);
                        }
                    }
                }

                boolean hasValidRows = false;
                for (Map.Entry<Integer, List<Row>> entry : rowsByTestId.entrySet()) {
                    Integer testId = entry.getKey();
                    List<Row> rows = entry.getValue();
                    hasValidRows = true;

                    HashMap<String, Object> testData = new HashMap<>();
                    HashMap<String, Object> headers = new HashMap<>();
                    HashMap<String, Object> params = new HashMap<>();
                    HashMap<String, Object> modifyPayload = new HashMap<>();
                    HashMap<String, Object> responseCapture = new HashMap<>();
                    HashMap<String, Object> authDetails = new HashMap<>();
                    String testDescription = "";

                    for (Row row : rows) {
                        if (row.getCell(requestIndex) != null && !row.getCell(requestIndex).toString().trim().isEmpty()) {
                            testData.put("Request", row.getCell(requestIndex).toString().trim());
                        }
                        if (row.getCell(endPointIndex) != null && !row.getCell(endPointIndex).toString().trim().isEmpty()) {
                            testData.put("End-Point", row.getCell(endPointIndex).toString().trim());
                        }
                        if (row.getCell(payloadIndex) != null && !row.getCell(payloadIndex).toString().trim().isEmpty()) {
                            testData.put("Payload", row.getCell(payloadIndex).toString().trim());
                        }
                        if (row.getCell(payloadTypeIndex) != null && !row.getCell(payloadTypeIndex).toString().trim().isEmpty()) {
                            testData.put("Payload Type", row.getCell(payloadTypeIndex).toString().trim());
                        }
                        if (row.getCell(expectedStatusIndex) != null && !row.getCell(expectedStatusIndex).toString().trim().isEmpty()) {
                            testData.put("Expected Status", row.getCell(expectedStatusIndex).toString().trim());
                        }
                        if (row.getCell(verifyResponseIndex) != null && !row.getCell(verifyResponseIndex).toString().trim().isEmpty()) {
                            testData.put("Verify Response", row.getCell(verifyResponseIndex).toString().trim());
                        }
                        if (row.getCell(testDescriptionIndex) != null && !row.getCell(testDescriptionIndex).toString().trim().isEmpty()) {
                            testDescription = row.getCell(testDescriptionIndex).toString().trim();
                            testData.put("Test Description", testDescription);
                        }
                        if (row.getCell(sslValidationIndex) != null && !row.getCell(sslValidationIndex).toString().trim().isEmpty()) {
                            testData.put("SSL Validation", row.getCell(sslValidationIndex).toString().trim());
                        }

                        if (row.getCell(headerKeyIndex) != null && row.getCell(headerValueIndex) != null) {
                            String headerKey = row.getCell(headerKeyIndex).toString().trim();
                            String headerValue = row.getCell(headerValueIndex).toString().trim();
                            if (!headerKey.isEmpty()) {
                                headers.put(headerKey, headerValue);
                            }
                        }

                        if (row.getCell(paramKeyIndex) != null && row.getCell(paramValueIndex) != null) {
                            String paramKey = row.getCell(paramKeyIndex).toString().trim();
                            String paramValue = row.getCell(paramValueIndex).toString().trim();
                            if (!paramKey.isEmpty()) {
                                params.put(paramKey, paramValue);
                            }
                        }

                        if (row.getCell(modifyPayloadKeyIndex) != null && row.getCell(modifyPayloadValueIndex) != null) {
                            String modifyKey = row.getCell(modifyPayloadKeyIndex).toString().trim();
                            String modifyValue = row.getCell(modifyPayloadValueIndex).toString().trim();
                            if (!modifyKey.isEmpty()) {
                                modifyPayload.put(modifyKey, modifyValue);
                            }
                        }

                        if (row.getCell(responseKeyNameIndex) != null && row.getCell(captureKeyValueIndex) != null) {
                            String responseKey = row.getCell(responseKeyNameIndex).toString().trim();
                            String captureValue = row.getCell(captureKeyValueIndex) != null ? row.getCell(captureKeyValueIndex).toString().trim() : "";
                            if (!responseKey.isEmpty() && !captureValue.isEmpty()) {
                                String cleanCaptureValue = captureValue.replaceAll("^\\{\\{|\\}\\}$", "").trim();
                                if (cleanCaptureValue.isEmpty()) {
                                    System.err.println("Warning: Empty environment variable name after removing {{}} for response key '" + responseKey + "' in Test ID " + testId);
                                } else {
                                    responseCapture.put(responseKey, cleanCaptureValue);
                                    System.out.println("Debug: Stored response capture for Test ID " + testId + ": key='" + responseKey + "', envVar='" + cleanCaptureValue + "'");
                                }
                            }
                        }

                        if (row.getCell(authorizationIndex) != null && !row.getCell(authorizationIndex).toString().trim().isEmpty()) {
                            String authType = row.getCell(authorizationIndex).toString().trim();
                            String authField1 = row.getCell(authField1Index) != null ? row.getCell(authField1Index).toString().trim() : "";
                            String authField2 = row.getCell(authField2Index) != null ? row.getCell(authField2Index).toString().trim() : "";
                            authDetails.put("Type", authType);
                            if (authType.equalsIgnoreCase("Basic Auth")) {
                                authDetails.put("Username", authField1);
                                authDetails.put("Password", authField2);
                            } else if (authType.equalsIgnoreCase("Bearer Token")) {
                                authDetails.put("Token", authField1);
                            } else {
                                authDetails.put("Type", "None");
                            }
                        }
                    }

                    testDataMap.put(testId, testData);
                    headersMap.put(testId, headers);
                    paramsMap.put(testId, params);
                    modifyPayloadMap.put(testId, modifyPayload);
                    responseCaptureMap.put(testId, responseCapture);
                    authMap.put(testId, authDetails);

                    testCases.add(new TestCase(true, testId.toString(), testDescription, "No Run"));
                }

                if (!hasValidRows) {
                    showError("No rows with non-empty Test ID found in test suite '" + testSuiteName + "'.");
                    return;
                }

                lastLoadedFile = file;
                runButton.setDisable(false);
                loadButton.setDisable(false);
                refreshButton.setDisable(false);
                stopButton.setDisable(true);

                table.refresh();
                selectAllCheckBox.setSelected(true);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Test Suite Loaded");
                alert.setContentText("Test cases loaded successfully from test suite '" + testSuiteName + "'.");
                alert.showAndWait();
            } catch (IOException ex) {
                showError("Failed to load test suite '" + testSuiteName + "': " + ex.getMessage());
            }
        }
    }

    private void updateSelectAllState() {
        boolean allSelected = testCases.stream().allMatch(testCase -> testCase.runProperty().get());
        selectAllCheckBox.setSelected(allSelected);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error Loading Test Suite");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}