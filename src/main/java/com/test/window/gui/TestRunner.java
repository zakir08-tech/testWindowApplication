package com.test.window.gui;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.test.window.app.GlueCode;
import com.test.window.app.UIConstants;
import com.test.window.app.Commons;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * TestRunnerFX is a JavaFX-based GUI application for loading and executing automated tests from JSON files.
 * It displays tests in a table with columns for selection, test ID, description, and status.
 * Features include selecting tests to run, highlighting running tests, displaying pass/fail results,
 * and generating an HTML test report.
 */
public class TestRunner extends Application {
    // Maps to store test steps and elements
    private static HashMap<String, LinkedHashMap<String, Object>> testStepsMap = new HashMap<>();
    private static HashMap<String, List<String>> globalElementListMap = new HashMap<>();
    private static JSONArray loadedJsonArray = null;
    private static File lastLoadedFile = null;
    private static volatile boolean isTestRunning = false;
    private static volatile boolean stopRequested = false;
    private static Button runButton;
    private static Button loadTestButton;
    private static Button refreshButton;
    private static Button stopButton;
    private static CheckBox selectAllCheckBox;
    private static TableView<TestCase> tableView;
    private static ObservableList<TestCase> tableData;
    // Color constants
    private static final String ENABLED_COLOR = "#32A852"; // Green
    private static final String HOVER_COLOR = "#288A48"; // Darker green
    private static final String PINK_RED = "#FF4040"; // Pink-red for Stop button
    private static final String DISABLED_COLOR = "#646464"; // Gray
    private static final String CHECKBOX_ENABLED_COLOR = "#C8C8C8"; // Light gray
    private static final String DEFAULT_FOREGROUND = "#C8C8C8"; // Light gray
    private static final String RUNNING_HIGHLIGHT = "#0788b3"; // Light blue
    // Static fields to retain state within JVM session
    private static ObservableList<TestCase> savedTableData = null;
    private static JSONArray savedJsonArray = null;
    private static File savedLastLoadedFile = null;
    private static HashMap<String, List<String>> savedElementListMap = null;
    
    // Property to track the currently executing test
    private static final ObjectProperty<TestCase> currentlyExecutingTest = new SimpleObjectProperty<>(null);

    // Method to get the currently executing test
    public static TestCase getCurrentlyExecutingTest() {
        return currentlyExecutingTest.get();
    }

    // Method to set the currently executing test
    public static void setCurrentlyExecutingTest(TestCase testCase) {
        currentlyExecutingTest.set(testCase);
        System.out.println("DEBUG: Set currentlyExecutingTest to: " + (testCase != null ? testCase.testIdProperty().get() : "null"));
    }

    // Method to add a listener for currently executing test changes
    public static void addCurrentlyExecutingTestListener(ChangeListener<TestCase> listener) {
        currentlyExecutingTest.addListener(listener);
    }

    // Data model for table rows
    public static class TestCase {
        private final BooleanProperty run;
        private final StringProperty testId;
        private final StringProperty description;
        private final StringProperty status;

        public TestCase(boolean run, String testId, String description, String status) {
            this.run = new SimpleBooleanProperty(run);
            this.testId = new SimpleStringProperty(testId);
            this.description = new SimpleStringProperty(description);
            this.status = new SimpleStringProperty(status);
        }

        public BooleanProperty runProperty() { return run; }
        public StringProperty testIdProperty() { return testId; }
        public StringProperty descriptionProperty() { return description; }
        public StringProperty statusProperty() { return status; }
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize table data
        tableData = FXCollections.observableArrayList();

        // Restore previous state if available
        if (savedTableData != null) {
            tableData.addAll(savedTableData);
            loadedJsonArray = savedJsonArray != null ? new JSONArray(savedJsonArray.toString()) : null;
            lastLoadedFile = savedLastLoadedFile;
            globalElementListMap.putAll(savedElementListMap != null ? new HashMap<>(savedElementListMap) : new HashMap<>());
        }

        // Setup UI components
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Title
        Label titleLabel = new Label("Test Runner");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        titleLabel.setTextFill(Color.web(DEFAULT_FOREGROUND));
        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(10));
        titleBox.setStyle("-fx-background-color: #1E1E1E;");

        // Create table using the factory
        VBox tableBox = TestTableFactory.createTestTable(tableData);
        // Extract references for later use
        selectAllCheckBox = (CheckBox) ((HBox) tableBox.getChildren().get(0)).getChildren().get(0);
        tableView = (TableView<TestCase>) tableBox.getChildren().get(1);

        // Enable editing for the TableView
        tableView.setEditable(true);

        // Set up Select All checkbox action
        selectAllCheckBox.setOnAction(e -> {
            boolean selected = selectAllCheckBox.isSelected();
            tableData.forEach(testCase -> testCase.runProperty().set(selected));
            updateButtonStates();
        });

        // Buttons
        runButton = new Button("Run");
        runButton.setTooltip(new Tooltip("Run the selected tests"));
        loadTestButton = new Button("Load Test");
        loadTestButton.setTooltip(new Tooltip("Upload and load a test JSON file"));
        refreshButton = new Button("Refresh");
        refreshButton.setTooltip(new Tooltip("Reload the last loaded test JSON file"));
        stopButton = new Button("Stop");
        stopButton.setTooltip(new Tooltip("Stop the running tests"));

        HBox buttonBox = new HBox(10, runButton, stopButton, refreshButton, loadTestButton);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setStyle("-fx-background-color: #1E1E1E;");

        // Initialize button styles
        initializeButton(runButton);
        initializeButton(loadTestButton);
        initializeButton(refreshButton);
        initializeButton(stopButton);

        // Listen for changes in tableData and individual TestCase run properties
        tableData.addListener((ListChangeListener<TestCase>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (TestCase testCase : c.getAddedSubList()) {
                        testCase.runProperty().addListener((obs, oldValue, newValue) -> updateButtonStates());
                    }
                }
            }
            updateButtonStates();
        });

        // Button actions
        runButton.setOnAction(e -> {
            if (loadedJsonArray == null) {
                showAlert(Alert.AlertType.ERROR, "Run Error", "No test(s) loaded. Please load a test file first.");
                return;
            }
            if (isTestRunning) {
                showAlert(Alert.AlertType.ERROR, "Run Error", "A test run is already in progress.");
                return;
            }
            isTestRunning = true;
            stopRequested = false;
            disableUI(true);
            runTests();
        });

        stopButton.setOnAction(e -> {
            if (isTestRunning) {
                stopRequested = true;
                Platform.runLater(() -> {
                    if (currentlyExecutingTest.get() != null) {
                        currentlyExecutingTest.get().statusProperty().set("Stopped");
                        setCurrentlyExecutingTest(null);
                        tableView.refresh();
                    }
                    isTestRunning = false;
                    disableUI(false);
                    showAlert(Alert.AlertType.INFORMATION, "Stop", "Test execution stopped.");
                });
            } else {
                showAlert(Alert.AlertType.WARNING, "Stop", "No tests are running.");
            }
        });

        loadTestButton.setOnAction(e -> loadJsonFile(false, primaryStage));
        refreshButton.setOnAction(e -> loadJsonFile(true, primaryStage));

        // Layout
        root.setTop(titleBox);
        root.setCenter(tableBox);
        root.setBottom(buttonBox);
        root.setStyle("-fx-background-color: #1E1E1E;");

        // Scene and stage
        Scene scene = new Scene(root, 600, 400);
        Image backgroundImage = new Image("file:" + UIConstants.UI_ICON);
        primaryStage.getIcons().add(backgroundImage);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Test Runner");

        // Set window size to 80% of screen size
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = screenBounds.getWidth() * 0.8;
        double height = screenBounds.getHeight() * 0.8;
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.setX((screenBounds.getWidth() - width) / 2);
        primaryStage.setY((screenBounds.getHeight() - height) / 2);

        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);
        primaryStage.setOnCloseRequest(e -> {
            if (isTestRunning) {
                showAlert(Alert.AlertType.WARNING, "Close Error", "Tests are running. Please stop tests before closing.");
                e.consume();
            } else {
                savedTableData = FXCollections.observableArrayList(tableData);
                savedJsonArray = loadedJsonArray != null ? new JSONArray(loadedJsonArray.toString()) : null;
                savedLastLoadedFile = lastLoadedFile;
                savedElementListMap = new HashMap<>(globalElementListMap);
            }
        });
        primaryStage.show();

        // Update UI after state restoration
        Platform.runLater(() -> {
            selectAllCheckBox.setSelected(!tableData.isEmpty() && tableData.stream().allMatch(testCase -> testCase.runProperty().get()));
            selectAllCheckBox.setDisable(tableData.isEmpty());
            selectAllCheckBox.setTextFill(Color.web(tableData.isEmpty() ? DISABLED_COLOR : CHECKBOX_ENABLED_COLOR));
            updateButtonStates();
        });
    }

    private void initializeButton(Button button) {
        button.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        button.setPrefSize(100, 25);
        button.setMinSize(100, 25);
        button.setMaxSize(100, 25);
        updateButtonStyle(button, button == runButton && !isAnyTestSelected());
        button.setOnMouseEntered(e -> {
            if (!button.isDisabled()) {
                String hoverColor = (button == stopButton) ? PINK_RED : HOVER_COLOR;
                button.setStyle("-fx-background-color: " + hoverColor + "; -fx-text-fill: black;");
            }
        });
        button.setOnMouseExited(e -> updateButtonStyle(button, button.isDisabled()));
    }

    private void updateButtonStyle(Button button, boolean isDisabled) {
        String color;
        if (button == stopButton) {
            color = isDisabled ? DISABLED_COLOR : PINK_RED;
        } else {
            color = isDisabled ? DISABLED_COLOR : 
                    (button == runButton && isTestRunning ? RUNNING_HIGHLIGHT : ENABLED_COLOR);
        }
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: black;");
    }

    private void disableUI(boolean disable) {
        runButton.setDisable(disable || !isAnyTestSelected());
        runButton.setStyle("-fx-background-color: " + (disable || !isAnyTestSelected() ? DISABLED_COLOR : isTestRunning ? RUNNING_HIGHLIGHT : ENABLED_COLOR) + "; -fx-text-fill: black;");
        loadTestButton.setDisable(disable);
        loadTestButton.setStyle("-fx-background-color: " + (disable ? DISABLED_COLOR : ENABLED_COLOR) + "; -fx-text-fill: black;");
        refreshButton.setDisable(disable);
        refreshButton.setStyle("-fx-background-color: " + (disable ? DISABLED_COLOR : ENABLED_COLOR) + "; -fx-text-fill: black;");
        stopButton.setDisable(disable && !isTestRunning); // Stop button enabled only during test execution
        stopButton.setStyle("-fx-background-color: " + ((disable && !isTestRunning) ? DISABLED_COLOR : PINK_RED) + "; -fx-text-fill: black;");
        selectAllCheckBox.setDisable(tableData.isEmpty());
        selectAllCheckBox.setTextFill(Color.web(tableData.isEmpty() ? DISABLED_COLOR : CHECKBOX_ENABLED_COLOR));
        tableView.setEditable(!disable); // Prevent table edits during test run
    }

    private boolean isAnyTestSelected() {
        return tableData.stream().anyMatch(testCase -> testCase.runProperty().get());
    }

    private void updateButtonStates() {
        boolean anySelected = isAnyTestSelected();
        runButton.setDisable(!anySelected || isTestRunning);
        runButton.setStyle("-fx-background-color: " + (anySelected && !isTestRunning ? ENABLED_COLOR : isTestRunning ? RUNNING_HIGHLIGHT : DISABLED_COLOR) + "; -fx-text-fill: black;");
        selectAllCheckBox.setSelected(tableData.stream().allMatch(testCase -> testCase.runProperty().get()));
    }

    private void runTests() {
        List<HtmlReportGenerator.TestReportEntry> reportEntries = new ArrayList<>();
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    List<TestCase> testsToRun = new ArrayList<>();
                    for (TestCase testCase : tableData) {
                        if (testCase.runProperty().get()) {
                            testsToRun.add(testCase);
                        }
                    }

                    boolean anySelected = !testsToRun.isEmpty();
                    long totalRunStart = System.currentTimeMillis();
                    String lastAppFamilyName = null;

                    for (TestCase testCase : testsToRun) {
                        if (stopRequested) break;
                        String testId = testCase.testIdProperty().get();
                        if (testId == null || testId.trim().isEmpty()) {
                            Platform.runLater(() -> {
                                testCase.statusProperty().set("Failed");
                                setCurrentlyExecutingTest(null);
                                tableView.refresh();
                                showAlert(Alert.AlertType.ERROR, "Run Error", "Invalid Test ID for " + testId);
                            });
                            continue;
                        }

                        HtmlReportGenerator.TestReportEntry entry = new HtmlReportGenerator.TestReportEntry();
                        entry.testId = testId;
                        entry.description = testCase.descriptionProperty().get();
                        entry.steps = new ArrayList<>();
                        long testStart = System.currentTimeMillis();
                        boolean testPassed = true;

                        // Set the currently executing test and ensure UI updates
                        Platform.runLater(() -> {
                            setCurrentlyExecutingTest(testCase);
                            testCase.statusProperty().set("Running");
                            tableView.refresh();
                            System.out.println("DEBUG: Started test: " + testId);
                        });

                        LinkedHashMap<String, Object> steps = testStepsMap.get(testId);
                        if (steps == null || steps.isEmpty()) {
                            Platform.runLater(() -> {
                                testCase.statusProperty().set("Failed");
                                setCurrentlyExecutingTest(null);
                                tableView.refresh();
                                showAlert(Alert.AlertType.ERROR, "Run Error", "No steps found for Test ID " + testId);
                            });
                            testPassed = false;
                        } else {
                            int stepNo = 1;
                            for (Map.Entry<String, Object> stepEntry : steps.entrySet()) {
                                if (stopRequested) {
                                    testPassed = false;
                                    break;
                                }
                                LinkedHashMap<String, String> stepData = (LinkedHashMap<String, String>) stepEntry.getValue();
                                String testAction = stepData.get("Test_Action");
                                String testElement = stepData.get("Test_Element");
                                String testData = stepData.get("Test_Data");
                                String stepDesc = stepData.get("Test_Description");

                                HtmlReportGenerator.StepReport stepReport = new HtmlReportGenerator.StepReport();
                                stepReport.stepNo = stepNo++;
                                stepReport.testStep = constructTestStepString(testAction, testElement, testData);
                                stepReport.stepDesc = stepDesc != null ? stepDesc : "";
                                long stepStart = System.currentTimeMillis();

                                try {
                                    List<String> stepList = new ArrayList<>();
                                    stepList.add(testAction != null ? testAction : "");
                                    stepList.add(testElement != null ? testElement : "");
                                    stepList.add(testData != null ? testData : "");
                                    stepList.add(stepDesc != null ? stepDesc : "");

                                    if ("OPEN_WINDOW".equals(testAction)) {
                                        lastAppFamilyName = testData != null ? testData.split("\\|")[0] : null;
                                    } else if ("TAKE_SCREENSHOT".equals(testAction)) {
                                        stepReport.screenshotBytes = GlueCode.takeScreenshotAsBytes();
                                    }

                                    executeTheStep(stepList, lastAppFamilyName);
                                    stepReport.durationMs = System.currentTimeMillis() - stepStart;
                                    entry.steps.add(stepReport);
                                } catch (Exception ex) {
                                    stepReport.error = ex.toString();
                                    stepReport.durationMs = System.currentTimeMillis() - stepStart;
                                    // Capture screenshot on failure with debugging
                                    byte[] screenshotBytes = GlueCode.takeScreenshotAsBytes();
                                    if (screenshotBytes != null && screenshotBytes.length > 0) {
                                        System.out.println("DEBUG: Screenshot captured successfully for failed step: " + stepReport.testStep + ", size: " + screenshotBytes.length + " bytes");
                                        stepReport.screenshotBytes = screenshotBytes;
                                    } else {
                                        System.err.println("DEBUG: Failed to capture screenshot for step: " + stepReport.testStep + ". Bytes are null or empty.");
                                    }
                                    entry.steps.add(stepReport);
                                    testPassed = false;
                                    Platform.runLater(() -> {
                                        testCase.statusProperty().set("Failed");
                                        setCurrentlyExecutingTest(null);
                                        tableView.refresh();
                                        //showAlert(Alert.AlertType.ERROR, "Step Error", "Test ID " + testId + " failed at step " + stepReport.testStep + ": " + ex.getMessage());
                                    });
                                    break;
                                }
                            }
                        }

                        entry.status = testPassed ? "Passed" : "Failed";
                        entry.totalDuration = System.currentTimeMillis() - testStart;
                        reportEntries.add(entry);

                        // Capture testPassed state for use in Platform.runLater
                        final boolean finalTestPassed = testPassed;
                        Platform.runLater(() -> {
                            testCase.statusProperty().set(finalTestPassed ? "Passed" : "Failed");
                            setCurrentlyExecutingTest(null);
                            tableView.refresh();
                        });
                    }

                    // Generate HTML report
                    int passedCount = (int) reportEntries.stream().filter(e -> "Passed".equals(e.status)).count();
                    int failedCount = reportEntries.size() - passedCount;
                    long totalRunTimeMs = System.currentTimeMillis() - totalRunStart;
                    File reportFile = new File("reports/TestReport_" + new SimpleDateFormat("ddMMyyHHmmss").format(new Date()) + ".html");
                    try {
                        HtmlReportGenerator.generateReport(reportFile, reportEntries, totalRunTimeMs, lastLoadedFile, passedCount, failedCount, 0);
                        //Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Report Generated", "Test report saved to " + reportFile.getAbsolutePath()));
                    } catch (IOException ex) {
                        //Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Report Error", "Failed to generate report: " + ex.getMessage()));
                    }
                } finally {
                    // Always generate and open report, even on exception or stop
                    Platform.runLater(() -> {
                        isTestRunning = false;
                        setCurrentlyExecutingTest(null);
                        tableView.refresh();
                        disableUI(false);
                        File reportFile = new File("reports/TestReport_" + new SimpleDateFormat("ddMMyyHHmmss").format(new Date()) + ".html");
                        try {
                            int passedCount = (int) reportEntries.stream().filter(e -> "Passed".equals(e.status)).count();
                            int failedCount = reportEntries.size() - passedCount;
                            long totalRunTimeMs = reportEntries.stream().mapToLong(e -> e.totalDuration).sum();
                            HtmlReportGenerator.generateReport(reportFile, reportEntries, totalRunTimeMs, lastLoadedFile, passedCount, failedCount, 0);
                            HtmlReportGenerator.openReportAutomatically(reportFile);
                            //showAlert(Alert.AlertType.INFORMATION, "Report Generated", "Test report saved and opened at " + reportFile.getAbsolutePath());
                        } catch (IOException ex) {
                            //showAlert(Alert.AlertType.ERROR, "Report Error", "Failed to generate or open report: " + ex.getMessage());
                        }
                    });
                }

                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    isTestRunning = false;
                    setCurrentlyExecutingTest(null);
                    tableView.refresh();
                    disableUI(false);
                    //showAlert(Alert.AlertType.INFORMATION, "Run", "Selected tests processed");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    isTestRunning = false;
                    setCurrentlyExecutingTest(null);
                    tableView.refresh();
                    disableUI(false);
                    //showAlert(Alert.AlertType.ERROR, "Run Error", "Test execution failed: " + getException().getMessage());
                });
            }

            @Override
            protected void cancelled() {
                Platform.runLater(() -> {
                    isTestRunning = false;
                    setCurrentlyExecutingTest(null);
                    tableView.refresh();
                    disableUI(false);
                    showAlert(Alert.AlertType.INFORMATION, "Stop", "Test execution stopped.");
                });
            }
        };
        new Thread(task).start();
    }

    private String constructTestStepString(String testAction, String testElement, String testData) {
        if (testAction == null) return "";
        switch (testAction) {
            case "OPEN_WINDOW":
                return "Launch URL \"" + (testData != null ? testData : "") + "\"";
            case "CLICK":
                return "Click \"" + (testElement != null ? testElement : "") + "\"";
            case "SET":
                return "Set value \"" + (testData != null ? testData : "") + "\" to \"" + (testElement != null ? testElement : "") + "\"";
            case "TAKE_SCREENSHOT":
                return "Take screen-shot";
            case "CLOSE_WINDOW":
                return "Close window \"" + (testData != null ? testData : "") + "\"";
            default:
                return testAction + (testElement != null ? " on \"" + testElement + "\"" : "") + (testData != null ? " with \"" + testData + "\"" : "");
        }
    }

    private void loadJsonFile(boolean isRefresh, Stage ownerStage) {
        File file;
        if (isRefresh) {
            if (lastLoadedFile == null) {
                showAlert(Alert.AlertType.ERROR, "Refresh Error", "No Test file has been loaded yet. Please use Load Test first.");
                return;
            }
            file = lastLoadedFile;
        } else {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Test JSON File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            fileChooser.setInitialDirectory(Commons.getDocumentsDirectory());
            file = fileChooser.showOpenDialog(ownerStage);
            if (file == null) return;
            lastLoadedFile = file;
        }

        try (FileReader fileReader = new FileReader(file)) {
            if (!file.exists() || !file.canRead()) {
                throw new IOException("File does not exist or is not readable: " + file.getAbsolutePath());
            }
            StringBuilder content = new StringBuilder();
            int ch;
            while ((ch = fileReader.read()) != -1) {
                content.append((char) ch);
            }
            try {
                loadedJsonArray = new JSONArray(content.toString());
                if (loadedJsonArray.length() == 0) {
                    throw new JSONException("JSON file is empty: " + file.getAbsolutePath());
                }
                tableData.clear();
                globalElementListMap.clear();
                testStepsMap.clear();

                for (int i = 0; i < loadedJsonArray.length(); i++) {
                    JSONObject obj = loadedJsonArray.getJSONObject(i);

                    if (obj.has("Element_List")) {
                        try {
                            JSONObject elementListObj = obj.getJSONObject("Element_List");
                            Iterator<String> elementKeys = elementListObj.keys();
                            while (elementKeys.hasNext()) {
                                String elementKey = elementKeys.next();
                                try {
                                    JSONArray elementArray = elementListObj.getJSONArray(elementKey);
                                    List<String> elementList = new ArrayList<>();
                                    for (int j = 0; j < elementArray.length(); j++) {
                                        elementList.add(elementArray.getString(j));
                                    }
                                    globalElementListMap.put(elementKey, elementList);
                                    System.out.println("DEBUG: Added Element_List with key: " + elementKey);
                                } catch (JSONException ex) {
                                    System.err.println("WARNING: Skipping malformed element array for key " + elementKey + ": " + ex.getMessage());
                                }
                            }
                        } catch (JSONException ex) {
                            System.err.println("WARNING: Skipping malformed Element_List: " + ex.getMessage());
                        }
                    }

                    if (obj.has("Test_Cases")) {
                        JSONArray testCases = obj.getJSONArray("Test_Cases");
                        for (int j = 0; j < testCases.length(); j++) {
                            JSONObject testCase = testCases.getJSONObject(j);
                            if (!testCase.has("Test_Id")) {
                                System.err.println("WARNING: Skipping JSON object at index " + j + ": Missing Test_Id");
                                continue;
                            }
                            String testId = testCase.getString("Test_Id");
                            if (testId == null || testId.trim().isEmpty()) {
                                System.err.println("WARNING: Skipping JSON object at index " + j + ": Test_Id is empty");
                                continue;
                            }
                            String testDescription = "";
                            LinkedHashMap<String, Object> stepsMap = new LinkedHashMap<>();
                            if (testCase.has("Steps")) {
                                JSONArray stepsArray = testCase.getJSONArray("Steps");
                                System.out.println("DEBUG: Loading steps for Test ID '" + testId + "': " + stepsArray.toString(2));
                                for (int k = 0; k < stepsArray.length(); k++) {
                                    JSONObject stepObj = stepsArray.getJSONObject(k);
                                    LinkedHashMap<String, String> stepData = new LinkedHashMap<>();
                                    String testAction = stepObj.optString("Test_Action", "");
                                    stepData.put("Test_Step", stepObj.optString("Test_Step", ""));
                                    stepData.put("Test_Action", testAction);
                                    stepData.put("Test_Element", stepObj.optString("Test_Element", ""));
                                    String testData = stepObj.optString("Test_Data", "");
                                    stepData.put("Test_Data", testData);
                                    stepData.put("Test_Description", stepObj.optString("Test_Description", ""));
                                    if ("CLOSE_WINDOW".equals(testAction) && (testData == null || testData.trim().isEmpty())) {
                                        System.err.println("WARNING: Empty or null Test_Data for CLOSE_WINDOW in Test ID '" + testId + "', Step " + stepObj.optString("Test_Step", "unknown"));
                                    }
                                    String stepKey = "Test_Step " + stepObj.getString("Test_Step");
                                    stepsMap.put(stepKey, stepData);
                                    if (k == 0) {
                                        testDescription = stepObj.optString("Test_Description", "");
                                    }
                                }
                            }
                            testStepsMap.put(testId, stepsMap);
                            tableData.add(new TestCase(true, testId, testDescription, "Not Run"));
                        }
                    }
                }
                if (tableData.isEmpty() && globalElementListMap.isEmpty()) {
                    throw new JSONException("No valid test cases or element lists found in Test file");
                }
                Platform.runLater(() -> {
                    selectAllCheckBox.setSelected(!tableData.isEmpty());
                    selectAllCheckBox.setDisable(tableData.isEmpty());
                    selectAllCheckBox.setTextFill(Color.web(tableData.isEmpty() ? DISABLED_COLOR : CHECKBOX_ENABLED_COLOR));
                    setCurrentlyExecutingTest(null);
                    tableView.refresh();
                    updateButtonStates();
                    System.out.println("DEBUG: globalElementListMap contents: " + globalElementListMap);
                    System.out.println("DEBUG: testStepsMap contents: " + testStepsMap);
                    //showAlert(Alert.AlertType.INFORMATION, "Load Successful", "Test loaded successfully from " + file.getAbsolutePath());
                });
            } catch (JSONException ex) {
                System.err.println("ERROR: Error parsing JSON: " + ex.getMessage());
                //showAlert(Alert.AlertType.ERROR, "Load Error", "Error parsing file: " + ex.getMessage());
            }
        } catch (IOException ex) {
            System.err.println("ERROR: Error reading JSON file: " + ex.getMessage());
            //showAlert(Alert.AlertType.ERROR, "Load Error", "Error reading file: " + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static boolean executeTheStep(List<String> step, String lastAppFamilyName) {
        if (step == null || step.size() < 4) {
            throw new IllegalArgumentException("Invalid step: must contain at least 4 elements (action, element, data, description)");
        }
        for (int i = 0; i < 4; i++) {
            if (step.get(i) == null) {
                throw new IllegalArgumentException("Invalid step: element " + i + " is null");
            }
        }

        String testAction = step.get(0).trim();
        String testElement = step.get(1).trim();
        String testData = step.get(2).trim();

        switch (testAction) {
            case "OPEN_WINDOW":
                try {
                    String[] parts = testData.split("\\|");
                    if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                        throw new IllegalArgumentException("Invalid OPEN_WINDOW format: expected 'appId|appTitle'");
                    }
                    String appId = parts[0].trim();
                    String appTitle = parts[1].trim();
                    GlueCode.invokeTheApplication(appId, appTitle);
                    return true;
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to execute OPEN_WINDOW step: " + e.getMessage(), e);
                }
            case "CLICK":
                try {
                    if (!globalElementListMap.containsKey(testElement)) {
                        throw new IllegalArgumentException("Element key not found in globalElementListMap: " + testElement);
                    }
                    List<String> elementData = globalElementListMap.get(testElement);
                    GlueCode.clickElement(elementData.get(0), elementData.get(1), elementData.get(2));
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to execute CLICK step: " + e.getMessage(), e);
                }
            case "SET":
                try {
                    if (!globalElementListMap.containsKey(testElement)) {
                        throw new IllegalArgumentException("Element key not found in globalElementListMap: " + testElement);
                    }
                    List<String> elementData = globalElementListMap.get(testElement);
                    GlueCode.setValueToElement(elementData.get(0), elementData.get(1), elementData.get(2), testData);
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to execute SET step: " + e.getMessage(), e);
                }
            case "TAKE_SCREENSHOT":
                try {
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to execute TAKE_SCREENSHOT step: " + e.getMessage(), e);
                }
            case "CLOSE_WINDOW":
                try {
                    String effectiveAppFamilyName = testData;
                    if (testData == null || testData.isEmpty()) {
                        if (lastAppFamilyName != null && !lastAppFamilyName.isEmpty()) {
                            System.out.println("DEBUG: Using OPEN_WINDOW appFamilyName '" + lastAppFamilyName + "' for CLOSE_WINDOW due to empty Test_Data");
                            effectiveAppFamilyName = lastAppFamilyName;
                        } else {
                            System.err.println("WARNING: Skipping CLOSE_WINDOW step due to empty Test_Data and no OPEN_WINDOW appFamilyName available");
                            return true;
                        }
                    }
                    System.out.println("DEBUG: Attempting to close window with appFamilyName: " + effectiveAppFamilyName);
                    try {
                        GlueCode.closeApplication(effectiveAppFamilyName);
                        System.out.println("DEBUG: Successfully closed application with appFamilyName: " + effectiveAppFamilyName);
                        try {
                            if (GlueCode.getDriver() != null) {
                                GlueCode.getDriver().quit();
                                System.out.println("DEBUG: WinAppDriver session closed for appFamilyName: " + effectiveAppFamilyName);
                            } else {
                                System.out.println("DEBUG: No active WinAppDriver session to close for appFamilyName: " + effectiveAppFamilyName);
                            }
                        } catch (Exception driverEx) {
                            System.err.println("WARNING: Failed to quit WinAppDriver session: " + driverEx.getMessage());
                        }
                    } catch (Exception e) {
                        System.err.println("WARNING: GlueCode.closeApplication failed for appFamilyName: " + effectiveAppFamilyName + ". Attempting fallback closure: " + e.getMessage());
                        try {
                            ProcessBuilder pb = new ProcessBuilder("taskkill", "/IM", "ApplicationFrameHost.exe", "/F");
                            Process process = pb.start();
                            int exitCode = process.waitFor();
                            if (exitCode == 0) {
                                System.out.println("DEBUG: Fallback closure succeeded: Terminated ApplicationFrameHost.exe");
                            } else {
                                System.err.println("ERROR: Fallback closure failed with exit code: " + exitCode);
                                throw new RuntimeException("Fallback closure failed for ApplicationFrameHost.exe. Exit code: " + exitCode);
                            }
                        } catch (Exception fallbackEx) {
                            throw new RuntimeException("Failed to execute CLOSE_WINDOW step after fallback attempt: " + fallbackEx.getMessage(), fallbackEx);
                        }
                    }
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to execute CLOSE_WINDOW step: " + e.getMessage(), e);
                }
            default:
                throw new UnsupportedOperationException("Unsupported test action: " + testAction);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}