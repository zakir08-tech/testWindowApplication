package com.test.window.gui;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.test.window.app.GlueCode;
import com.test.window.app.UIConstants;
import com.test.window.app.Commons;

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
import javafx.stage.Stage;

/**
 * TestRunnerFX is a JavaFX-based GUI application for loading and executing automated tests from JSON files.
 * It displays tests in a table with columns for selection, test ID, description, and status.
 * Features include selecting tests to run, highlighting running tests, and displaying pass/fail results.
 * The application supports loading JSON files, refreshing the last loaded file, and stopping tests.
 */
public class TestRunner extends Application {
    // Maps to store test steps and elements
    private static HashMap<String, List<List<String>>> testStepsMap = new HashMap<>();
    private static HashMap<String, List<String>> globalElementListMap = new HashMap<>();
    private static JSONArray loadedJsonArray = null;
    private static File lastLoadedFile = null;
    private static volatile boolean isTestRunning = false;
    private static volatile boolean stopRequested = false;
    private static Button runButton, loadTestButton, refreshButton, stopButton;
    private static CheckBox selectAllCheckBox;
    private static TableView<TestCase> tableView;
    private static ObservableList<TestCase> tableData;
    // Color constants
    private static final String ENABLED_COLOR = "#32A852"; // Green
    private static final String HOVER_COLOR = "#288A48"; // Darker green
    private static final String DISABLED_COLOR = "#646464"; // Gray
    private static final String CHECKBOX_ENABLED_COLOR = "#C8C8C8"; // Light gray
    private static final String DEFAULT_FOREGROUND = "#C8C8C8"; // Light gray

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

        // Create table using the factory (this handles columns, styling, checkbox, and VBox layout)
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
        runButton = createStyledButton("Run", "Run the selected tests");
        loadTestButton = createStyledButton("Load Test", "Upload and load a test JSON file");
        refreshButton = createStyledButton("Refresh", "Reload the last loaded test JSON file");
        stopButton = createStyledButton("Stop", "Stop the running tests");

        HBox buttonBox = new HBox(10, runButton, stopButton, refreshButton, loadTestButton);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setStyle("-fx-background-color: #1E1E1E;");

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
                showAlert(Alert.AlertType.ERROR, "Run Error", "No JSON data loaded. Please load a test file first.");
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
                showAlert(Alert.AlertType.INFORMATION, "Stop", "Stopping tests...");
            } else {
                showAlert(Alert.AlertType.WARNING, "Stop", "No tests are running.");
            }
        });

        loadTestButton.setOnAction(e -> loadJsonFile(false));
        refreshButton.setOnAction(e -> loadJsonFile(true));

        // Layout
        root.setTop(titleBox);
        root.setCenter(tableBox);
        root.setBottom(buttonBox);
        root.setStyle("-fx-background-color: #1E1E1E;");

        // Scene and stage
        Scene scene = new Scene(root, 600, 400);
        // Optional: Load external CSS file (uncomment to use)
        // scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        Image backgroundImage = new Image("file:"+UIConstants.UI_ICON);
        primaryStage.getIcons().add(backgroundImage);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Test Runner");
        primaryStage.setMaximized(true);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);
        primaryStage.setOnCloseRequest(e -> {
            if (isTestRunning) {
                showAlert(Alert.AlertType.WARNING, "Close Error", "Tests are running. Please stop tests before closing.");
                e.consume();
            }
        });
        primaryStage.show();
    }

    private Button createStyledButton(String text, String tooltip) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        button.setPrefSize(100, 25);
        button.setMinSize(100, 25);
        button.setMaxSize(100, 25);
        button.setStyle("-fx-background-color: " + (text.equals("Run") ? DISABLED_COLOR : ENABLED_COLOR) + "; -fx-text-fill: black;");
        button.setOnMouseEntered(e -> {
            if (button.isDisabled()) return;
            button.setStyle("-fx-background-color: #288A48; -fx-text-fill: black;");
        });
        button.setOnMouseExited(e -> {
            if (button.isDisabled()) return;
            button.setStyle("-fx-background-color: " + (button == runButton && !isAnyTestSelected() ? DISABLED_COLOR : ENABLED_COLOR) + "; -fx-text-fill: black;");
        });
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private void disableUI(boolean disable) {
        runButton.setDisable(disable || !isAnyTestSelected());
        runButton.setStyle("-fx-background-color: " + (disable || !isAnyTestSelected() ? DISABLED_COLOR : ENABLED_COLOR) + "; -fx-text-fill: black;");
        loadTestButton.setDisable(disable);
        loadTestButton.setStyle("-fx-background-color: " + (disable ? DISABLED_COLOR : ENABLED_COLOR) + "; -fx-text-fill: black;");
        refreshButton.setDisable(disable);
        refreshButton.setStyle("-fx-background-color: " + (disable ? DISABLED_COLOR : ENABLED_COLOR) + "; -fx-text-fill: black;");
        selectAllCheckBox.setDisable(disable || tableData.isEmpty());
        selectAllCheckBox.setTextFill(Color.web(disable || tableData.isEmpty() ? DISABLED_COLOR : CHECKBOX_ENABLED_COLOR));
        tableView.setDisable(disable);
        tableView.setEditable(!disable); // Enable editing when tests are not running
    }

    private boolean isAnyTestSelected() {
        return tableData.stream().anyMatch(testCase -> testCase.runProperty().get());
    }

    private void updateButtonStates() {
        boolean anySelected = isAnyTestSelected();
        runButton.setDisable(!anySelected || isTestRunning);
        runButton.setStyle("-fx-background-color: " + (anySelected && !isTestRunning ? ENABLED_COLOR : DISABLED_COLOR) + "; -fx-text-fill: black;");
        selectAllCheckBox.setSelected(tableData.stream().allMatch(testCase -> testCase.runProperty().get()));
    }

    private void runTests() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    testStepsMap.clear();
                    boolean anySelected = false;

                    for (TestCase testCase : tableData) {
                        if (!testCase.runProperty().get() || stopRequested) continue;
                        anySelected = true;
                        String testId = testCase.testIdProperty().get();
                        if (testId == null || testId.trim().isEmpty()) {
                            Platform.runLater(() -> {
                                testCase.statusProperty().set("Failed");
                                showAlert(Alert.AlertType.ERROR, "Run Error", "Invalid Test ID for " + testId);
                            });
                            continue;
                        }

                        Platform.runLater(() -> testCase.statusProperty().set("Running"));
                        boolean testFound = false;
                        for (int i = 0; i < loadedJsonArray.length(); i++) {
                            JSONObject obj = loadedJsonArray.getJSONObject(i);
                            if (!obj.has("Test_Id") || !obj.getString("Test_Id").equals(testId)) continue;
                            testFound = true;
                            List<List<String>> testSteps = new ArrayList<>();
                            Iterator<String> keys = obj.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                if (key.startsWith("Test_Step")) {
                                    try {
                                        JSONArray stepArray = obj.getJSONArray(key);
                                        List<String> stepData = new ArrayList<>();
                                        for (int j = 0; j < stepArray.length(); j++) {
                                            stepData.add(stepArray.get(j).toString());
                                        }
                                        if (stepData.size() >= 4) {
                                            testSteps.add(stepData);
                                        }
                                    } catch (JSONException ex) {
                                        System.err.println("Skipping malformed test step " + key + " for Test ID " + testId + ": " + ex.getMessage());
                                    }
                                }
                            }
                            testStepsMap.put(testId, testSteps);
                        }

                        if (!testFound) {
                            Platform.runLater(() -> {
                                testCase.statusProperty().set("Failed");
                                showAlert(Alert.AlertType.ERROR, "Run Error", "Test ID " + testId + " not found in JSON");
                            });
                            continue;
                        }

                        List<List<String>> steps = testStepsMap.get(testId);
                        if (steps == null || steps.isEmpty()) {
                            Platform.runLater(() -> {
                                testCase.statusProperty().set("Failed");
                                showAlert(Alert.AlertType.ERROR, "Run Error", "No valid steps for Test ID " + testId);
                            });
                            continue;
                        }

                        boolean testPassed = true;
                        for (int i = 0; i < steps.size() && !stopRequested; i++) {
                            List<String> step = steps.get(i);
                            try {
                                executeTheStep(step);
                            } catch (Exception ex) {
                                testPassed = false;
                                final String errorMessage = "Test ID " + testId + ", Step " + (i + 1) + " failed: " + ex.getMessage();
                                System.err.println(errorMessage);
                                Platform.runLater(() -> {
                                    testCase.statusProperty().set("Failed");
                                    showAlert(Alert.AlertType.ERROR, "Run Error", errorMessage);
                                });
                                break;
                            }
                        }
                        if (testPassed) {
                            Platform.runLater(() -> testCase.statusProperty().set("Passed"));
                        }
                    }

                    if (!anySelected) {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Run Error", "No tests selected to run.");
                            isTestRunning = false;
                        });
                    }
                    return null;
                } catch (Exception ex) {
                    System.err.println("Unexpected error during test execution: " + ex.getMessage());
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Run Error", "Unexpected error: " + ex.getMessage());
                        isTestRunning = false;
                    });
                    return null;
                }
            }

            @Override
            protected void succeeded() {
                isTestRunning = false;
                Platform.runLater(() -> {
                    disableUI(false);
                    showAlert(Alert.AlertType.INFORMATION, "Run", "Selected tests processed");
                });
            }

            @Override
            protected void failed() {
                isTestRunning = false;
                Platform.runLater(() -> {
                    disableUI(false);
                    showAlert(Alert.AlertType.ERROR, "Run Error", "Test execution failed: " + getException().getMessage());
                });
            }
        };
        new Thread(task).start();
    }

    private void loadJsonFile(boolean isRefresh) {
        File file;
        if (isRefresh) {
            if (lastLoadedFile == null) {
                showAlert(Alert.AlertType.ERROR, "Refresh Error", "No JSON file has been loaded yet. Please use Load Test first.");
                return;
            }
            file = lastLoadedFile;
        } else {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Test JSON File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            fileChooser.setInitialDirectory(Commons.getDocumentsDirectory());
            file = fileChooser.showOpenDialog(tableView.getScene().getWindow());
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
                                    System.out.println("Added Element_List with key: " + elementKey);
                                } catch (JSONException ex) {
                                    System.err.println("Skipping malformed element array for key " + elementKey + " at index " + i + ": " + ex.getMessage());
                                }
                            }
                        } catch (JSONException ex) {
                            System.err.println("Skipping malformed Element_List at index " + i + ": " + ex.getMessage());
                        }
                    }

                    if (!obj.has("Test_Id")) {
                        System.err.println("Skipping JSON object at index " + i + ": Missing Test_Id");
                        continue;
                    }
                    String testId = obj.getString("Test_Id");
                    if (testId == null || testId.trim().isEmpty()) {
                        System.err.println("Skipping JSON object at index " + i + ": Test_Id is empty");
                        continue;
                    }
                    String testDescription = "";
                    try {
                        if (obj.has("Test_Step 1")) {
                            JSONArray testStep1 = obj.getJSONArray("Test_Step 1");
                            if (testStep1.length() >= 4) {
                                testDescription = testStep1.getString(3);
                            }
                        }
                    } catch (JSONException ex) {
                        System.err.println("Skipping malformed Test_Step 1 for Test ID " + testId + ": " + ex.getMessage());
                    }
                    tableData.add(new TestCase(true, testId, testDescription, "Not Run"));
                }
                if (tableData.isEmpty() && globalElementListMap.isEmpty()) {
                    throw new JSONException("No valid test cases or element lists found in JSON file");
                }
                Platform.runLater(() -> {
                    selectAllCheckBox.setSelected(!tableData.isEmpty());
                    selectAllCheckBox.setDisable(tableData.isEmpty());
                    selectAllCheckBox.setTextFill(Color.web(tableData.isEmpty() ? DISABLED_COLOR : CHECKBOX_ENABLED_COLOR));
                    updateButtonStates();
                    System.out.println("globalElementListMap contents: " + globalElementListMap);
                    showAlert(Alert.AlertType.INFORMATION, "Load Successful", "JSON loaded successfully from " + file.getAbsolutePath());
                });
            } catch (JSONException ex) {
                System.err.println("Error parsing JSON: " + ex.getMessage());
                showAlert(Alert.AlertType.ERROR, "Load Error", "Error parsing JSON: " + ex.getMessage());
            }
        } catch (IOException ex) {
            System.err.println("Error reading JSON file: " + ex.getMessage());
            showAlert(Alert.AlertType.ERROR, "Load Error", "Error reading file: " + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static boolean executeTheStep(List<String> step) {
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
            case "APP_ID":
                try {
                    String[] parts = testData.split("\\|");
                    if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                        throw new IllegalArgumentException("Invalid APP_ID format: expected 'appId|appTitle'");
                    }
                    String appId = parts[0].trim();
                    String appTitle = parts[1].trim();
                    GlueCode.invokeTheApplication(appId, appTitle);
                    return true;
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to execute APP_ID step: " + e.getMessage(), e);
                }
            case "CLICK":
                try {
                    if (!globalElementListMap.containsKey(testElement)) {
                        throw new IllegalArgumentException("Element key not found in globalElementListMap: " + testElement);
                    }
                    List<String> elementData = globalElementListMap.get(testElement);
                    GlueCode.clickElement(testElement, testAction, elementData.toString());
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
                    // TODO: Uncomment and implement GlueCode.setValueToElement when available
                    // GlueCode.setValueToElement(testElement, testData, testAction, elementData.toString());
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