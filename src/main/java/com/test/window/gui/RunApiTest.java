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
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.window.app.UIConstants;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    // UI Components
    private TableView<TestCase> table;
    private ObservableList<TestCase> testCases;
    private CheckBox selectAllCheckBox;
    private Button loadButton;
    private Button runButton;
    private Button stopButton;
    private Button refreshButton;
    private TextField testTypeField;
    private Label testTypeLabel;
    private File lastLoadedFile;
    private Task<Void> runTask;
    private double lastScrollPosition = 0.0;

    // Data storage for test configurations
    private HashMap<Integer, HashMap<String, Object>> testDataMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> headersMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> paramsMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> modifyPayloadMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> responseCaptureMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> authMap = new HashMap<>();
    private List<Map<String, Object>> reportDataList = new ArrayList<>();
    private String currentTimestamp = null; // Global variable to hold the timestamp for the current test

    private static final Set<String> REQUIRED_HEADERS = new HashSet<>(Arrays.asList(
        "Test ID", "Test Description", "Request", "End-Point", "Header (key)", "Header (value)",
        "Parameter (key)", "Parameter (value)", "Payload", "Payload Type",
        "Response (key) Name", "Capture (key) Value (env var)", "Authorization", "", "",
        "SSL Validation", "Expected Status", "Verify Response"
    ));

    @Override
    public void start(Stage primaryStage) {
        // Set the window icon
        try {
            Image backgroundImage = new Image("file:" + UIConstants.UI_ICON);
            primaryStage.getIcons().add(backgroundImage);
        } catch (Exception e) {
            System.err.println("Error setting window icon: " + e.getMessage());
        }

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
                    // Check if all are now selected manually
                    if (newVal && isAllSelected()) {
                        String testTypeText = testTypeField.getText().trim();
                        if (!testTypeText.isEmpty()) {
                            testTypeField.setText("");
                        }
                    }
                    updateSelectAllState();
                    updateRunButtonState();
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

        loadButton = new Button("Load");
        loadButton.setStyle(LOAD_REFRESH_BUTTON_STYLE);
        loadButton.setTooltip(new Tooltip("Load test cases from an Excel test suite"));
        loadButton.setOnMouseEntered(e -> loadButton.setStyle(LOAD_REFRESH_BUTTON_HOVER_STYLE));
        loadButton.setOnMouseExited(e -> loadButton.setStyle(LOAD_REFRESH_BUTTON_STYLE));

        runButton = new Button("Run");
        runButton.setStyle(RUN_BUTTON_STYLE);
        runButton.setTooltip(new Tooltip("Run selected test cases"));
        runButton.setOnMouseEntered(e -> runButton.setStyle(RUN_BUTTON_HOVER_STYLE));
        runButton.setOnMouseExited(e -> runButton.setStyle(RUN_BUTTON_STYLE));

        stopButton = new Button("Stop");
        stopButton.setStyle(STOP_BUTTON_STYLE);
        stopButton.setTooltip(new Tooltip("Stop running test cases"));
        stopButton.setDisable(true);
        stopButton.setOnMouseEntered(e -> stopButton.setStyle(STOP_BUTTON_HOVER_STYLE));
        stopButton.setOnMouseExited(e -> stopButton.setStyle(STOP_BUTTON_STYLE));

        refreshButton = new Button("Refresh");
        refreshButton.setStyle(LOAD_REFRESH_BUTTON_STYLE);
        refreshButton.setTooltip(new Tooltip("Refresh test cases from the last loaded file"));
        refreshButton.setOnMouseEntered(e -> refreshButton.setStyle(LOAD_REFRESH_BUTTON_HOVER_STYLE));
        refreshButton.setOnMouseExited(e -> refreshButton.setStyle(LOAD_REFRESH_BUTTON_STYLE));
        refreshButton.setDisable(true);

        testTypeLabel = new Label();
        testTypeLabel.setStyle("-fx-text-fill: white;");
        testTypeField = new TextField();
        testTypeField.setStyle(FIELD_STYLE_UNFOCUSED);
        testTypeField.setPrefWidth(150);
        testTypeField.setPromptText("Enter test type");
        testTypeField.setDisable(true);

        testCases.addListener((ListChangeListener<TestCase>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (TestCase testCase : change.getAddedSubList()) {
                        testCase.runProperty().addListener((obs, oldVal, newVal) -> {
                            // Check if all are now selected manually
                            if (newVal && isAllSelected()) {
                                String testTypeText = testTypeField.getText().trim();
                                if (!testTypeText.isEmpty()) {
                                    testTypeField.setText("");
                                }
                            }
                            updateSelectAllState();
                            updateRunButtonState();
                        });
                    }
                }
            }
            updateSelectAllState();
            updateRunButtonState();
            updateComponentStates();
        });

        selectAllCheckBox.setOnAction(e -> {
            boolean selected = selectAllCheckBox.isSelected();
            for (TestCase testCase : testCases) {
                testCase.runProperty().set(selected);
            }
            // Do not clear testTypeField when using Select All checkbox
            updateRunButtonState();
        });

        updateSelectAllState();
        updateRunButtonState();
        updateComponentStates();

        table.setOnScroll(event -> {
            ScrollBar verticalScrollBar = getVerticalScrollBar();
            if (verticalScrollBar != null && verticalScrollBar.isVisible()) {
                lastScrollPosition = verticalScrollBar.getValue();
            }
        });

        loadButton.setOnAction(e -> {
            testTypeField.setText("");
            loadTestCases(primaryStage, true);
        });

        // Add focus listener to testTypeField to apply filter on lose focus
        testTypeField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && wasFocused) {
                applyTestTypeFilter();
            }
        });

        runButton.setOnAction(e -> {
            // Apply filter before running if needed
            applyTestTypeFilter();

            reportDataList.clear();

            // Determine filter on FX thread
            String testType = testTypeField.getText().trim();
            String filterStr = null;
            if (testType.startsWith("@") && testType.length() > 1) {
                filterStr = testType.substring(1).trim();
            }

            // Collect tests to run on FX thread
            List<TestCase> testsToRun = new ArrayList<>();
            for (TestCase tc : testCases) {
                if (tc.runProperty().get() &&
                    (filterStr == null || tc.testDescriptionProperty().get().toLowerCase().contains(filterStr.toLowerCase()))) {
                    testsToRun.add(tc);
                }
            }

            if (testsToRun.isEmpty()) {
                if (filterStr != null && !filterStr.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("No Matching Tests");
                    alert.setHeaderText("No test cases match the specified test type");
                    alert.setContentText("No test cases found matching the test type '@" + filterStr + "'. Please check the test type or select tests manually.");
                    alert.showAndWait();
                }
                return;
            }

            Set<Integer> selectedIds = testsToRun.stream()
                .map(tc -> Integer.parseInt(tc.testIdProperty().get()))
                .collect(Collectors.toSet());

            loadSelectedTestData(selectedIds);

            runTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Platform.runLater(() -> {
                        runButton.setDisable(true);
                        loadButton.setDisable(true);
                        refreshButton.setDisable(true);
                        stopButton.setDisable(false);
                        testTypeField.setDisable(true);
                    });

                    ApiExecutor apiExecutor = new ApiExecutor();
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, String> envVars = new HashMap<>();
                    Map<String, String> originalEnvVars = new HashMap<>();

                    File envFile = new File("env.json");
                    if (envFile.exists()) {
                        try {
                            envVars = objectMapper.readValue(envFile, HashMap.class);
                            originalEnvVars.putAll(envVars);
                            System.out.println("Debug: Loaded env.json: " + envVars);
                        } catch (IOException ex) {
                            System.err.println("Error loading env.json: " + ex.getMessage());
                        }
                    } else {
                        System.err.println("env.json not found in project root. Initializing empty environment variables.");
                    }

                    for (TestCase testCase : testsToRun) {
                        if (isCancelled()) {
                            break;
                        }
                        currentTimestamp = null; // Reset timestamp for each test
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
                        String modifiedPayload = payload;
                        String processedVerifyResponse = null;
                        StringBuilder captureIssues = new StringBuilder();

                        Map<String, Object> reportData = new HashMap<>();
                        reportData.put("testId", testId.toString());
                        reportData.put("description", testCase.testDescriptionProperty().get());
                        reportData.put("request", method);

                        long responseTimeMs = 0L;
                        String originalPayload = null;
                        String originalVerifyResponse = null;

                        try {
                            System.out.println("Debug: Starting processing for Test ID " + testId);
                            HashMap<String, Object> headers = headersMap.get(testId);
                            HashMap<String, Object> params = paramsMap.get(testId);
                            HashMap<String, Object> modifyPayload = modifyPayloadMap.get(testId);
                            HashMap<String, Object> authDetails = authMap.get(testId);
                            HashMap<String, Object> responseCapture = responseCaptureMap.get(testId);

                            System.out.println("Debug: Replacing placeholders in URL for Test ID " + testId);
                            String processedUrl = replacePlaceholders(url, envVars, testId);
                            testData.put("End-Point", processedUrl);
                            reportData.put("endpoint", processedUrl);

                            System.out.println("Debug: Processing headers for Test ID " + testId);
                            HashMap<String, Object> processedHeaders = new HashMap<>();
                            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                                String headerKey = entry.getKey();
                                String headerValue = entry.getValue() != null ? entry.getValue().toString() : "";
                                String processedValue = replacePlaceholders(headerValue, envVars, testId);
                                processedHeaders.put(headerKey, processedValue);
                            }
                            headersMap.put(testId, processedHeaders);
                            reportData.put("headers", processedHeaders);

                            System.out.println("Debug: Processing parameters for Test ID " + testId);
                            HashMap<String, Object> processedParams = new HashMap<>();
                            for (Map.Entry<String, Object> entry : params.entrySet()) {
                                String paramKey = entry.getKey();
                                String paramValue = entry.getValue() != null ? entry.getValue().toString() : "";
                                String processedValue = replacePlaceholders(paramValue, envVars, testId);
                                processedParams.put(paramKey, processedValue);
                            }
                            paramsMap.put(testId, processedParams);
                            reportData.put("parameters", processedParams);

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
                            String lowerPayloadType = payloadType != null ? payloadType.toLowerCase() : "";
                            boolean isJsonPayload = "json".equals(lowerPayloadType);
                            if (payload != null && !payload.trim().isEmpty()) {
                                System.out.println("Debug: Replacing placeholders in payload for Test ID " + testId);
                                modifiedPayload = replacePlaceholders(payload, envVars, testId);
                                originalPayload = modifiedPayload;
                                if (isJsonPayload) {
                                    modifiedPayload = postProcessForJson(modifiedPayload);
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
                                originalPayload = modifiedPayload;
                            }
                            testData.put("Payload", modifiedPayload);
                            reportData.put("payload", originalPayload != null ? originalPayload : modifiedPayload);
                            reportData.put("payloadType", payloadType);

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
                            authMap.put(testId, processedAuthDetails);
                            reportData.put("authentication", processedAuthDetails);
                            String authType = (String) processedAuthDetails.get("Type");
                            if (authType != null && !authType.equals("None")) {
                                String username = (String) processedAuthDetails.get("Username");
                                String password = (String) processedAuthDetails.get("Password");
                                String token = (String) processedAuthDetails.get("Token");
                                auth = new ApiExecutor.Auth(authType, username, password, token);
                            }

                            System.out.println("Debug: Processing expected status for Test ID " + testId);
                            String processedExpectedStatusStr = replacePlaceholders(expectedStatusStr, envVars, testId);
                            testData.put("Expected Status", processedExpectedStatusStr);
                            processedVerifyResponse = replacePlaceholders(verifyResponse, envVars, testId);
                            originalVerifyResponse = processedVerifyResponse;
                            if (processedVerifyResponse != null && !processedVerifyResponse.trim().isEmpty()) {
                                processedVerifyResponse = postProcessForJson(processedVerifyResponse);
                            }
                            testData.put("Verify Response", processedVerifyResponse);

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
                            responseTimeMs = (endTime - startTime) / 1_000_000;
                            System.out.println("Debug: Response time for Test ID " + testId + ": " + responseTimeMs + " ms");

                            reportData.put("responseStatus", String.valueOf(response.getStatusCode()));
                            reportData.put("responseBody", response.getBody());
                            reportData.put("responseTimeMs", responseTimeMs);

                            if (response.getStatusCode() != expectedStatus) {
                                throw new Exception("Status code mismatch for Test ID " + testId +
                                    ": expected " + expectedStatus + ", got " + response.getStatusCode());
                            }

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
                                    System.out.println("Debug: Attempting to capture value for path '" + responsePath + "' to env var '" + envVarName + "' for Test ID " + testId);
                                    Object capturedValue = getNestedValue(responseObj, responsePath, testId);
                                    if (capturedValue != null) {
                                        String valueStr = capturedValue.toString();
                                        String cleanedValue = valueStr.replaceAll("\\{\\{|}}", "");
                                        if (!cleanedValue.equals(valueStr)) {
                                            System.out.println("Debug: Removed braces from captured value '" + valueStr + "' to '" + cleanedValue + "' for env var '" + envVarName + "' in Test ID " + testId);
                                        }
                                        if (cleanedValue.contains("{{") || cleanedValue.contains("}}")) {
                                            captureIssues.append("Captured value for '").append(envVarName)
                                                .append("' contains invalid braces after cleaning: '").append(cleanedValue).append("'. ");
                                            System.err.println("Warning: Captured value for env var '" + envVarName + "' in Test ID " + testId + " contains invalid braces after cleaning: '" + cleanedValue + "'");
                                            continue;
                                        }
                                        if (envVarName.contains("{{") || envVarName.contains("}}")) {
                                            captureIssues.append("Environment variable name '").append(envVarName)
                                                .append("' contains invalid braces. ");
                                            System.err.println("Warning: Environment variable name '" + envVarName + "' for Test ID " + testId + " contains invalid braces");
                                            continue;
                                        }
                                        // Avoid saving {{null}} to envVars
                                        if (!cleanedValue.equals("{{null}}")) {
                                            envVars.put(envVarName, cleanedValue);
                                            capturedValues.put(envVarName, cleanedValue);
                                        }
                                        captureCount++;
                                        captureIssues.append("Captured key '").append(responsePath)
                                            .append("' as env var '").append(envVarName).append("': ").append(cleanedValue).append(". ");
                                        System.out.println("Debug: Captured value '" + cleanedValue + "' from path '" + responsePath + "' and saved to env var '" + envVarName + "' for Test ID " + testId);
                                    } else {
                                        captureIssues.append("Key '").append(responsePath)
                                            .append("' not found in response for env var '").append(envVarName).append("'. ");
                                        System.err.println("Warning: No value found at path '" + responsePath + "' in response for Test ID " + testId);
                                    }
                                }
                                System.out.println("Debug: Total values captured for Test ID " + testId + ": " + captureCount + " out of " + responseCapture.size() + " entries");

                                Map<String, String> envVarsToSave = new HashMap<>();
                                for (Map.Entry<String, String> entry : envVars.entrySet()) {
                                    String value = entry.getValue();
                                    // Skip saving {{null}} to env.json
                                    if (!"{{null}}".equals(value)) {
                                        envVarsToSave.put(entry.getKey(), value);
                                    }
                                }
                                for (Map.Entry<String, String> entry : originalEnvVars.entrySet()) {
                                    if ("$timestamp".equals(entry.getValue()) || "$last-timestamp".equals(entry.getValue())) {
                                        envVarsToSave.put(entry.getKey(), entry.getValue());
                                    }
                                }

                                try {
                                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(new FileOutputStream(envFile), envVarsToSave);
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

                            reportData.put("captureIssues", captureIssues.toString());

                            boolean verificationPassed = true;
                            String finalVerifyResponse = processedVerifyResponse != null ? processedVerifyResponse : "";

                            if (processedVerifyResponse != null && !processedVerifyResponse.trim().isEmpty()) {
                                System.out.println("Debug: Verifying response for Test ID " + testId);
                                try {
                                    finalVerifyResponse = processVerifyExpression(processedVerifyResponse, response.getBody(), testId);
                                    Object actualJson = objectMapper.readValue(response.getBody(), Object.class);
                                    Object expectedJson = objectMapper.readValue(finalVerifyResponse, Object.class);
                                    compareJson(actualJson, expectedJson, "", testId);
                                } catch (Exception e) {
                                    verificationPassed = false;
                                    throw new Exception("Response verification failed for Test ID " + testId + ": " + e.getMessage(), e);
                                }
                            }

                            reportData.put("verifyResponse", originalVerifyResponse != null ? originalVerifyResponse : "");
                            reportData.put("verificationPassed", verificationPassed);

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
                            reportData.put("payload", originalPayload != null ? originalPayload : modifiedPayload);
                            reportData.put("verifyResponse", originalVerifyResponse != null ? originalVerifyResponse : "");
                            Platform.runLater(() -> {
                                testCase.statusProperty().set("Fail");
                                reportData.put("status", "Fail");
                                reportData.put("failureReason", failureReason);
                                reportDataList.add(reportData);
                            });
                        }
                    }

                    Platform.runLater(() -> {
                        runButton.setDisable(false);
                        loadButton.setDisable(false);
                        refreshButton.setDisable(false);
                        stopButton.setDisable(true);
                        testTypeField.setDisable(!testCases.isEmpty());
                        updateRunButtonState();

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
                        } finally {
                            reportDataList.clear();
                            testDataMap.clear();
                            headersMap.clear();
                            paramsMap.clear();
                            modifyPayloadMap.clear();
                            responseCaptureMap.clear();
                            authMap.clear();
                            currentTimestamp = null;
                            updateComponentStates();
                            updateRunButtonState();
                            updateSelectAllState();
                        }
                    });
                    return null;
                }
            };
            new Thread(runTask).start();
        });

        stopButton.setOnAction(e -> {
            if (runTask != null) {
                runTask.cancel();
            }
            runButton.setDisable(false);
            loadButton.setDisable(false);
            refreshButton.setDisable(false);
            stopButton.setDisable(true);
            testTypeField.setDisable(!testCases.isEmpty());
            updateRunButtonState();
        });

        refreshButton.setOnAction(e -> {
            testTypeField.setText("");
            loadTestCases(primaryStage, false);
        });

        HBox leftButtons = new HBox(10, loadButton, refreshButton, testTypeLabel, testTypeField);
        leftButtons.setAlignment(Pos.CENTER_LEFT);

        HBox rightButtons = new HBox(10, runButton, stopButton);
        rightButtons.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBox = new HBox(10, leftButtons, spacer, rightButtons);
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

    private boolean isAllSelected() {
        if (testCases.isEmpty()) {
            return false;
        }
        for (TestCase testCase : testCases) {
            if (!testCase.runProperty().get()) {
                return false;
            }
        }
        return true;
    }

    private void loadSelectedTestData(Set<Integer> selectedIds) {
        if (lastLoadedFile == null || selectedIds.isEmpty()) {
            return;
        }
        String testSuiteName = lastLoadedFile.getName().substring(0, lastLoadedFile.getName().lastIndexOf('.'));
        try (FileInputStream fileIn = new FileInputStream(lastLoadedFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fileIn)) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                showError("No header row found in test suite '" + testSuiteName + "'.");
                return;
            }

            testDataMap.clear();
            headersMap.clear();
            paramsMap.clear();
            modifyPayloadMap.clear();
            responseCaptureMap.clear();
            authMap.clear();

            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String header = headerRow.getCell(i) != null ? headerRow.getCell(i).getStringCellValue().trim() : "";
                headerMap.put(header, i);
            }

            for (String requiredHeader : REQUIRED_HEADERS) {
                if (!headerMap.containsKey(requiredHeader) && !requiredHeader.isEmpty()) {
                    showError("Missing required column '" + requiredHeader + "' in test suite '" + testSuiteName + "'.");
                    return;
                }
            }

            Integer currentTestId = null;
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
                Integer tempId = testIdStr != null && !testIdStr.trim().isEmpty() ? Integer.parseInt(testIdStr.trim()) : null;

                if (tempId != null) {
                    // Finish previous test if applicable
                    if (currentTestId != null) {
                        testDataMap.put(currentTestId, currentTestData);
                        headersMap.put(currentTestId, currentHeaders);
                        paramsMap.put(currentTestId, currentParams);
                        modifyPayloadMap.put(currentTestId, currentModifyPayload);
                        responseCaptureMap.put(currentTestId, currentResponseCapture);
                        authMap.put(currentTestId, currentAuthDetails);
                    }

                    // Start new test if selected
                    if (selectedIds.contains(tempId)) {
                        currentTestId = tempId;

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

                        addMultiColumnData(row, headerMap, "Header (key)", "Header (value)", currentHeaders);
                        addMultiColumnData(row, headerMap, "Parameter (key)", "Parameter (value)", currentParams);
                        addMultiColumnData(row, headerMap, "Modify Payload (key)", "Modify Payload (value)", currentModifyPayload);
                        addResponseCaptureData(row, headerMap, "Response (key) Name", "Capture (key) Value (env var)", currentResponseCapture, currentTestId);

                        String username = getCellValue(row, headerMap.get("Username"));
                        String password = getCellValue(row, headerMap.get("Password"));
                        String token = getCellValue(row, headerMap.get("Token"));
                        if (username != null) currentAuthDetails.put("Username", username);
                        if (password != null) currentAuthDetails.put("Password", password);
                        if (token != null) currentAuthDetails.put("Token", token);
                    } else {
                        currentTestId = null;
                        currentTestData = null;
                        currentHeaders = null;
                        currentParams = null;
                        currentModifyPayload = null;
                        currentResponseCapture = null;
                        currentAuthDetails = null;
                    }
                } else if (currentTestId != null) {
                    // Continuation row for current test
                    addMultiColumnData(row, headerMap, "Header (key)", "Header (value)", currentHeaders);
                    addMultiColumnData(row, headerMap, "Parameter (key)", "Parameter (value)", currentParams);
                    addMultiColumnData(row, headerMap, "Modify Payload (key)", "Modify Payload (value)", currentModifyPayload);
                    addResponseCaptureData(row, headerMap, "Response (key) Name", "Capture (key) Value (env var)", currentResponseCapture, currentTestId);
                }
            }

            // Finish the last test if applicable
            if (currentTestId != null) {
                testDataMap.put(currentTestId, currentTestData);
                headersMap.put(currentTestId, currentHeaders);
                paramsMap.put(currentTestId, currentParams);
                modifyPayloadMap.put(currentTestId, currentModifyPayload);
                responseCaptureMap.put(currentTestId, currentResponseCapture);
                authMap.put(currentTestId, currentAuthDetails);
            }

            System.out.println("Debug: Loaded selected test data for IDs: " + selectedIds);
        } catch (Exception e) {
            showError("Failed to load selected test data from test suite '" + testSuiteName + "': " + e.getMessage());
            System.err.println("Error loading selected test data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateComponentStates() {
        boolean hasData = !testCases.isEmpty();
        refreshButton.setDisable(lastLoadedFile == null || !lastLoadedFile.exists());
        testTypeField.setDisable(!hasData);
    }

    private void applyTestTypeFilter() {
        String testType = testTypeField.getText().trim();
        if (testType.isEmpty() || !testType.startsWith("@") || testType.length() <= 1) {
            return; // do nothing
        }
        String filterStr = testType.substring(1).trim();
        if (filterStr.isEmpty()) {
            return;
        }
        // Check if any test cases match the filter
        boolean anyMatch = false;
        for (TestCase tc : testCases) {
            String desc = tc.testDescriptionProperty().get() != null ? tc.testDescriptionProperty().get().toLowerCase() : "";
            if (desc.contains(filterStr.toLowerCase())) {
                anyMatch = true;
                break;
            }
        }
        if (!anyMatch) {
            return; // Do not apply filter or change selections
        }
        // Apply the filter
        for (TestCase tc : testCases) {
            String desc = tc.testDescriptionProperty().get() != null ? tc.testDescriptionProperty().get().toLowerCase() : "";
            boolean matches = desc.contains(filterStr.toLowerCase());
            tc.runProperty().set(matches);
        }
        updateRunButtonState();
    }

    private String postProcessForJson(String jsonStr) {
        if (jsonStr == null || !jsonStr.contains("{{null}}")) {
            return jsonStr;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        boolean inString = false;
        while (i < jsonStr.length()) {
            char c = jsonStr.charAt(i);
            if (!inString) {
                if (c == '"') {
                    inString = true;
                    sb.append(c);
                    i++;
                    continue;
                } else if (jsonStr.startsWith("{{null}}", i)) {
                    // Not in string, replace with null
                    sb.append("null");
                    i += 8; // length of {{null}}
                    continue;
                }
            } else {
                // Simple escape handling: check for \"
                if (c == '"' && (i == 0 || jsonStr.charAt(i - 1) != '\\')) {
                    inString = false;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private void updateRunButtonState() {
        boolean anySelected = testCases.stream().anyMatch(tc -> tc.runProperty().get());
        runButton.setDisable(!anySelected);
    }

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

    private String processVerifyExpression(String verifyExpression, String actualResponseBody, Integer testId) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Object actualJson = mapper.readValue(actualResponseBody, Object.class);
        String tempVerifyExpression = verifyExpression;
        Map<String, String> pathToValueMap = new HashMap<>();

        if (verifyExpression != null && verifyExpression.contains("$any-value")) {
            try {
                tempVerifyExpression = verifyExpression
                    .replaceAll("\\$any-value\\b(?=\\s*(,|\\}|\\]))", "\"_any_value_\"")
                    .replaceAll("\"\\$any-value\"", "\"_any_value_\"");
                Object expectedJsonForExtract = mapper.readValue(tempVerifyExpression, Object.class);
                extractAnyValuePathsFromObject(expectedJsonForExtract, actualJson, pathToValueMap, "", testId);
                tempVerifyExpression = replaceAnyValueWithActual(tempVerifyExpression, pathToValueMap, testId);
            } catch (JsonProcessingException e) {
                throw new Exception("Failed to parse verifyExpression with $any-value for Test ID " + testId + ": " + e.getMessage(), e);
            } catch (Exception e) {
                throw new Exception("Failed to process $any-value in verifyExpression for Test ID " + testId + ": " + e.getMessage(), e);
            }
        }
        return tempVerifyExpression;
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
            for (String actualKey : actualMap.keySet()) {
                if (!expectedMap.containsKey(actualKey)) {
                    String newPath = path.isEmpty() ? actualKey : path + "." + actualKey;
                    throw new Exception("Unexpected extra key '" + actualKey + "' in response at path '" + newPath + "' for Test ID " + testId);
                }
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
            Matcher matcher = pattern.matcher(text);
            StringBuffer modifiedText = new StringBuffer();

            while (matcher.find()) {
                String placeholder = matcher.group(1);
                String replacement;
                String val = envVars.get(placeholder);
                if (!envVars.containsKey(placeholder) || val == null || val.trim().isEmpty() || "null".equals(val)) {
                    replacement = "{{null}}";
                    envVars.put(placeholder, replacement);
                    System.out.println("Debug: Placeholder '" + placeholder + "' not found or null/empty/'null' in env.json for Test ID " + testId + ", replacing with '{{null}}' and updating hashmap");
                } else {
                    String value = envVars.get(placeholder);
                    if ("$timestamp".equals(value)) {
                        if (currentTimestamp == null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMddyyssSSS");
                            currentTimestamp = sdf.format(new Date());
                            System.out.println("Debug: Generated new timestamp '" + currentTimestamp + "' for Test ID " + testId);
                        }
                        replacement = currentTimestamp;
                        envVars.put(placeholder, replacement); // Update envVars for consistency within this test
                        System.out.println("Debug: Replaced placeholder '" + placeholder + "' with timestamp value '" + replacement + "' for Test ID " + testId);
                    } else if ("$last-timestamp".equals(value)) {
                        if (currentTimestamp == null) {
                            replacement = "{{null}}";
                            System.out.println("Debug: Placeholder '" + placeholder + "' with $last-timestamp has no current timestamp available for Test ID " + testId + ", replacing with '{{null}}'");
                        } else {
                            replacement = currentTimestamp;
                            System.out.println("Debug: Replaced placeholder '" + placeholder + "' with $last-timestamp value '" + replacement + "' for Test ID " + testId);
                        }
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

                testCases.clear();
                testDataMap.clear();
                headersMap.clear();
                paramsMap.clear();
                modifyPayloadMap.clear();
                responseCaptureMap.clear();
                authMap.clear();

                Map<String, Integer> headerMap = new HashMap<>();
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    String header = headerRow.getCell(i) != null ? headerRow.getCell(i).getStringCellValue().trim() : "";
                    headerMap.put(header, i);
                }

                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;

                    String testIdStr = getCellValue(row, headerMap.get("Test ID"));
                    if (testIdStr != null && !testIdStr.trim().isEmpty()) {
                        Integer tempId = Integer.parseInt(testIdStr.trim());
                        String testDescription = getCellValue(row, headerMap.get("Test Description"));
                        testCases.add(new TestCase(true, tempId.toString(), testDescription, "No Run"));
                    }
                }

                lastLoadedFile = file;
                System.out.println("Debug: Loaded test cases from '" + file.getAbsolutePath() + "'. Test cases count: " + testCases.size());
            } catch (Exception e) {
                showError("Failed to load test suite '" + testSuiteName + "': " + e.getMessage());
                System.err.println("Error loading test suite: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void addMultiColumnData(Row row, Map<String, Integer> headerMap, String keyHeader, String valueHeader, HashMap<String, Object> targetMap) {
        String key = getCellValue(row, headerMap.get(keyHeader));
        String value = getCellValue(row, headerMap.get(valueHeader));
        if (key != null && !key.trim().isEmpty()) {
            targetMap.put(key, value != null ? value : "");
        }
    }

    private void addResponseCaptureData(Row row, Map<String, Integer> headerMap, String keyHeader, String valueHeader, HashMap<String, Object> targetMap, Integer testId) {
        String key = getCellValue(row, headerMap.get(keyHeader));
        String value = getCellValue(row, headerMap.get(valueHeader));
        if (key != null && !key.trim().isEmpty() && value != null && !value.trim().isEmpty()) {
            String cleanedValue = value.replaceAll("\\{\\{|}}", "");
            if (!cleanedValue.equals(value)) {
                System.out.println("Debug: Cleaned environment variable name from '" + value + "' to '" + cleanedValue + "' for Test ID " + testId);
            }
            if (cleanedValue.trim().isEmpty()) {
                System.err.println("Warning: Environment variable name is empty after cleaning for Test ID " + testId + ": original value '" + value + "'");
            } else {
                targetMap.put(key, cleanedValue);
            }
        }
    }

    private String getCellValue(Row row, Integer columnIndex) {
        if (columnIndex == null || row == null) return null;
        var cell = row.getCell(columnIndex);
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double numericValue = cell.getNumericCellValue();
                if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                    return String.valueOf((int) numericValue);
                } else {
                    return String.valueOf(numericValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Operation Failed");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}