package com.test.window.gui;

import javafx.application.Application;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class RunApiTest extends Application {

    private static final String FIELD_STYLE_UNFOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #BBBBBB; -fx-border-radius: 5px;";

    private static final String RUN_BUTTON_STYLE = 
        "-fx-background-color: #90EE90; -fx-text-fill: black; -fx-border-radius: 5px; -fx-min-width: 100px;"; // Light green

    private static final String RUN_BUTTON_HOVER_STYLE = 
        "-fx-background-color: #98FB98; -fx-text-fill: black; -fx-border-radius: 5px; -fx-min-width: 100px;"; // Lighter green

    private static final String STOP_BUTTON_STYLE = 
        "-fx-background-color: #FFB6C1; -fx-text-fill: black; -fx-border-radius: 5px; -fx-min-width: 100px;"; // Light pink

    private static final String STOP_BUTTON_HOVER_STYLE = 
        "-fx-background-color: #FFC1CC; -fx-text-fill: black; -fx-border-radius: 5px; -fx-min-width: 100px;"; // Lighter pink

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
    private File lastLoadedFile; // Track the last loaded file
    private Task<Void> runTask; // Track the running task for stopping

    // HashMaps to store test data
    private HashMap<Integer, HashMap<String, Object>> testDataMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> headersMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> paramsMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> modifyPayloadMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> responseCaptureMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Object>> authMap = new HashMap<>();

    private static final Set<String> REQUIRED_HEADERS = new HashSet<>(Arrays.asList(
        "Test ID", "Request", "End-Point", "Header (key)", "Header (value)", 
        "Parameter (key)", "Parameter (value)", "Payload", "Payload Type", 
        "Modify Payload (key)", "Modify Payload (value)", "Response (key) Name", 
        "Capture (key) Value (env var)", "Authorization", "", "", 
        "SSL Validation", "Expected Status", "Verify Response", "Test Description"
    ));

    @Override
    public void start(Stage primaryStage) {
        // Initialize table and data
        testCases = FXCollections.observableArrayList();
        table = new TableView<>(testCases);
        table.setStyle("-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; -fx-table-cell-border-color: #3C3F41; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");

        // Column width calculations (assuming average character width of 8 pixels)
        final double CHAR_WIDTH = 8.0;
        final double RUN_COL_WIDTH = 5 * CHAR_WIDTH; // 5 characters
        final double TEST_ID_COL_WIDTH = 8 * CHAR_WIDTH; // 8 characters
        final double STATUS_COL_WIDTH = 12 * CHAR_WIDTH; // 12 characters
        final double TEST_DESC_MIN_WIDTH = 300.0; // Minimum width to ensure text visibility

        // Run column (CheckBox)
        TableColumn<TestCase, Boolean> runColumn = new TableColumn<>("Run");
        runColumn.setCellValueFactory(cellData -> cellData.getValue().runProperty());
        runColumn.setCellFactory(col -> {
            CheckBoxTableCell<TestCase, Boolean> cell = new CheckBoxTableCell<>(index -> {
                BooleanProperty property = testCases.get(index).runProperty();
                property.addListener((obs, oldVal, newVal) -> {
                    // Set focus to the row when checkbox is toggled
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

        // Test Description column
        TableColumn<TestCase, String> testDescriptionColumn = new TableColumn<>("Test Description");
        testDescriptionColumn.setCellValueFactory(cellData -> cellData.getValue().testDescriptionProperty());
        testDescriptionColumn.setMinWidth(TEST_DESC_MIN_WIDTH);
        testDescriptionColumn.setPrefWidth(TEST_DESC_MIN_WIDTH);

        // Status column
        TableColumn<TestCase, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusColumn.setMinWidth(STATUS_COL_WIDTH);
        statusColumn.setMaxWidth(STATUS_COL_WIDTH);
        statusColumn.setPrefWidth(STATUS_COL_WIDTH);
        statusColumn.setResizable(false);
        statusColumn.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(runColumn, testIdColumn, testDescriptionColumn, statusColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setEditable(true); // Enable editing for the table

        // Ensure table grows with window
        HBox.setHgrow(table, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Select All CheckBox
        selectAllCheckBox = new CheckBox("Select All");
        selectAllCheckBox.setStyle(FIELD_STYLE_UNFOCUSED);
        selectAllCheckBox.setSelected(false); // Unchecked by default
        selectAllCheckBox.setOnAction(e -> {
            boolean selected = selectAllCheckBox.isSelected();
            for (TestCase testCase : testCases) {
                testCase.runProperty().set(selected);
            }
        });

        // Monitor table for changes to update Select All CheckBox
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

        // Buttons
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
            runTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    // Disable Run, Load, Refresh; Enable Stop
                    javafx.application.Platform.runLater(() -> {
                        runButton.setDisable(true);
                        loadButton.setDisable(true);
                        refreshButton.setDisable(true);
                        stopButton.setDisable(false);
                    });

                    // Print selected test details
                    for (TestCase testCase : testCases) {
                        if (isCancelled()) {
                            break; // Stop if task is cancelled
                        }
                        if (testCase.runProperty().get()) {
                            try {
                                Integer testId = Integer.parseInt(testCase.testIdProperty().get());
                                System.out.println("Test ID: " + testId);
                                System.out.println("Test Data: " + testDataMap.get(testId));
                                System.out.println("Headers: " + headersMap.get(testId));
                                System.out.println("Parameters: " + paramsMap.get(testId));
                                System.out.println("Modify Payload: " + modifyPayloadMap.get(testId));
                                System.out.println("Response Capture: " + responseCaptureMap.get(testId));
                                System.out.println("Authorization: " + authMap.get(testId));
                                System.out.println("---");

                                // Simulate execution with sleep
                                Thread.sleep(1000); // 1 second per test case
                            } catch (NumberFormatException ex) {
                                // Skip invalid Test IDs
                                continue;
                            }
                        }
                    }
                    return null;
                }

                @Override
                protected void succeeded() {
                    // Re-enable buttons
                    javafx.application.Platform.runLater(() -> {
                        runButton.setDisable(false);
                        loadButton.setDisable(false);
                        refreshButton.setDisable(false);
                        stopButton.setDisable(true);
                    });
                }

                @Override
                protected void cancelled() {
                    // Re-enable buttons
                    javafx.application.Platform.runLater(() -> {
                        runButton.setDisable(false);
                        loadButton.setDisable(false);
                        refreshButton.setDisable(false);
                        stopButton.setDisable(true);
                    });
                }

                @Override
                protected void failed() {
                    // Re-enable buttons on failure
                    javafx.application.Platform.runLater(() -> {
                        runButton.setDisable(false);
                        loadButton.setDisable(false);
                        refreshButton.setDisable(false);
                        stopButton.setDisable(true);
                    });
                }
            };

            // Start the task in a new thread
            new Thread(runTask).start();
        });
        runButton.setOnMouseEntered(e -> runButton.setStyle(RUN_BUTTON_HOVER_STYLE));
        runButton.setOnMouseExited(e -> runButton.setStyle(RUN_BUTTON_STYLE));
        runButton.setDisable(true); // Disabled by default

        stopButton = new Button("Stop");
        stopButton.setStyle(STOP_BUTTON_STYLE);
        stopButton.setTooltip(new Tooltip("Stop running test cases"));
        stopButton.setOnAction(e -> {
            if (runTask != null && !runTask.isDone()) {
                runTask.cancel(); // Cancel the running task
                System.out.println("Run task stopped.");
            }
        });
        stopButton.setOnMouseEntered(e -> stopButton.setStyle(STOP_BUTTON_HOVER_STYLE));
        stopButton.setOnMouseExited(e -> stopButton.setStyle(STOP_BUTTON_STYLE));
        stopButton.setDisable(true); // Disabled by default

        refreshButton = new Button("Refresh");
        refreshButton.setStyle(LOAD_REFRESH_BUTTON_STYLE);
        refreshButton.setTooltip(new Tooltip("Refresh the test case list"));
        refreshButton.setOnAction(e -> {
            if (lastLoadedFile != null) {
                loadTestCases(primaryStage, false); // Reload the last loaded file
            } else {
                showError("No test suite has been loaded to refresh.");
            }
        });
        refreshButton.setOnMouseEntered(e -> refreshButton.setStyle(LOAD_REFRESH_BUTTON_HOVER_STYLE));
        refreshButton.setOnMouseExited(e -> refreshButton.setStyle(LOAD_REFRESH_BUTTON_STYLE));
        refreshButton.setDisable(true); // Disabled by default

        // Button layout
        HBox buttonBox = new HBox(10, loadButton, runButton, stopButton, refreshButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));
        HBox.setHgrow(buttonBox, Priority.ALWAYS);
        buttonBox.setMaxWidth(Double.MAX_VALUE);

        // Layout
        VBox topBox = new VBox(10, selectAllCheckBox, table);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(10));
        VBox.setVgrow(topBox, Priority.ALWAYS);

        VBox root = new VBox(10, topBox, buttonBox);
        root.setStyle("-fx-background-color: #2E2E2E;");
        root.setPadding(new Insets(10));
        VBox.setVgrow(root, Priority.ALWAYS);

        // Scene and Stage
        Scene scene = new Scene(root, 720, 400);
        scene.getStylesheets().add("data:text/css," + CSS.replaceAll("\n", "%0A"));
        primaryStage.setTitle("Run API Test");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(720);
        primaryStage.show();

        // Ensure table resizes when window is maximized
        primaryStage.maximizedProperty().addListener((obs, wasMaximized, isNowMaximized) -> {
            if (isNowMaximized) {
                table.setPrefWidth(scene.getWidth() - 40);
                table.refresh();
            }
        });

        // Handle window resize
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            table.setPrefWidth(newVal.doubleValue() - 40);
            table.refresh();
        });
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
                
                // Validate headers
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

                if (!actualHeaders.equals(REQUIRED_HEADERS)) {
                    showError("Invalid headers in test suite '" + testSuiteName + "'. The test suite does not contain the required headers.");
                    return;
                }

                // Check for data rows
                if (sheet.getLastRowNum() < 1) {
                    showError("No data rows found in test suite '" + testSuiteName + "'.");
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

                // Find column indices for all required fields
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
                            // Assign next two columns as authField1 and authField2
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
                        case "":
                            emptyHeaderCount++;
                            // Skip assigning to authField indices here; handled under Authorization
                            break;
                    }
                }

                // Verify that authField indices were set correctly
                if (authField1Index == -1 || authField2Index == -1) {
                    showError("Invalid headers in test suite '" + testSuiteName + "'. Missing empty headers after Authorization.");
                    return;
                }

                // Load test cases and populate HashMaps
                boolean hasValidRows = false;
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        String testIdStr = row.getCell(testIdIndex) != null ? row.getCell(testIdIndex).toString().trim() : "";
                        if (!testIdStr.isEmpty()) {
                            try {
                                Integer testId = Integer.parseInt(testIdStr); // Parse Test ID as integer
                                hasValidRows = true;

                                // Populate testDataMap
                                HashMap<String, Object> testData = new HashMap<>();
                                testData.put("Request", row.getCell(requestIndex) != null ? row.getCell(requestIndex).toString().trim() : "");
                                testData.put("End-Point", row.getCell(endPointIndex) != null ? row.getCell(endPointIndex).toString().trim() : "");
                                testData.put("Payload", row.getCell(payloadIndex) != null ? row.getCell(payloadIndex).toString().trim() : "");
                                testData.put("Payload Type", row.getCell(payloadTypeIndex) != null ? row.getCell(payloadTypeIndex).toString().trim() : "");
                                testData.put("Expected Status", row.getCell(expectedStatusIndex) != null ? row.getCell(expectedStatusIndex).toString().trim() : "");
                                testData.put("Verify Response", row.getCell(verifyResponseIndex) != null ? row.getCell(verifyResponseIndex).toString().trim() : "");
                                testData.put("Test Description", row.getCell(testDescriptionIndex) != null ? row.getCell(testDescriptionIndex).toString().trim() : "");
                                testData.put("SSL Validation", row.getCell(sslValidationIndex) != null ? row.getCell(sslValidationIndex).toString().trim() : "");
                                testDataMap.put(testId, testData);

                                // Populate headersMap
                                HashMap<String, Object> headers = new HashMap<>();
                                if (row.getCell(headerKeyIndex) != null && row.getCell(headerValueIndex) != null) {
                                    String headerKey = row.getCell(headerKeyIndex).toString().trim();
                                    String headerValue = row.getCell(headerValueIndex).toString().trim();
                                    if (!headerKey.isEmpty()) {
                                        headers.put(headerKey, headerValue);
                                    }
                                }
                                headersMap.put(testId, headers);

                                // Populate paramsMap
                                HashMap<String, Object> params = new HashMap<>();
                                if (row.getCell(paramKeyIndex) != null && row.getCell(paramValueIndex) != null) {
                                    String paramKey = row.getCell(paramKeyIndex).toString().trim();
                                    String paramValue = row.getCell(paramValueIndex).toString().trim();
                                    if (!paramKey.isEmpty()) {
                                        params.put(paramKey, paramValue);
                                    }
                                }
                                paramsMap.put(testId, params);

                                // Populate modifyPayloadMap
                                HashMap<String, Object> modifyPayload = new HashMap<>();
                                if (row.getCell(modifyPayloadKeyIndex) != null && row.getCell(modifyPayloadValueIndex) != null) {
                                    String modifyKey = row.getCell(modifyPayloadKeyIndex).toString().trim();
                                    String modifyValue = row.getCell(modifyPayloadValueIndex).toString().trim();
                                    if (!modifyKey.isEmpty()) {
                                        modifyPayload.put(modifyKey, modifyValue);
                                    }
                                }
                                modifyPayloadMap.put(testId, modifyPayload);

                                // Populate responseCaptureMap
                                HashMap<String, Object> responseCapture = new HashMap<>();
                                if (row.getCell(responseKeyNameIndex) != null && row.getCell(captureKeyValueIndex) != null) {
                                    String responseKey = row.getCell(responseKeyNameIndex).toString().trim();
                                    String captureValue = row.getCell(captureKeyValueIndex).toString().trim();
                                    if (!responseKey.isEmpty()) {
                                        responseCapture.put(responseKey, captureValue);
                                    }
                                }
                                responseCaptureMap.put(testId, responseCapture);

                                // Populate authMap
                                HashMap<String, Object> authDetails = new HashMap<>();
                                String authType = row.getCell(authorizationIndex) != null ? row.getCell(authorizationIndex).toString().trim() : "";
                                String authField1 = row.getCell(authField1Index) != null ? row.getCell(authField1Index).toString().trim() : "";
                                String authField2 = row.getCell(authField2Index) != null ? row.getCell(authField2Index).toString().trim() : "";
                                
                                if (authType.equalsIgnoreCase("Basic Auth")) {
                                    authDetails.put("Type", "Basic Auth");
                                    authDetails.put("Username", authField1);
                                    authDetails.put("Password", authField2);
                                } else if (authType.equalsIgnoreCase("Bearer Token")) {
                                    authDetails.put("Type", "Bearer Token");
                                    authDetails.put("Token", authField1);
                                } else {
                                    authDetails.put("Type", "None");
                                }
                                authMap.put(testId, authDetails);

                                // Add to table
                                String testDescription = row.getCell(testDescriptionIndex) != null ? row.getCell(testDescriptionIndex).toString().trim() : "";
                                testCases.add(new TestCase(true, testIdStr, testDescription, "No Run"));
                            } catch (NumberFormatException e) {
                                // Skip rows with invalid Test ID
                                continue;
                            }
                        }
                    }
                }

                if (!hasValidRows) {
                    showError("No rows with non-empty Test ID found in test suite '" + testSuiteName + "'.");
                    return;
                }

                // Update last loaded file and enable buttons
                lastLoadedFile = file;
                runButton.setDisable(false);
                loadButton.setDisable(false);
                refreshButton.setDisable(false);
                stopButton.setDisable(true); // Ensure Stop is disabled after load

                table.refresh();
                selectAllCheckBox.setSelected(true); // Check Select All after loading
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