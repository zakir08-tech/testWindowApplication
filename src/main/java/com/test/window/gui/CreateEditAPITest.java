package com.test.window.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.test.window.app.UIConstants;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * A JavaFX application for creating and editing API test cases.
 * It provides a table-based interface to manage test data, supports JSON payload editing
 * with syntax highlighting for placeholders (e.g., {{variable}}), Excel file import/export,
 * and integration with environment variables via env.json updates.
 */
public class CreateEditAPITest extends Application {

    /**
     * Array of column names for the test data table.
     * These columns represent various aspects of an API test case, such as requests, parameters,
     * payloads, and verification criteria.
     */
    private static final String[] COLUMN_NAMES = {
        "Test ID", "Test Description", "Request", "End-Point", "Header (key)", "Header (value)",
        "Parameter (key)", "Parameter (value)", "Payload", "Payload Type",
        "Response (key) Name", "Capture (key) Value (env var)", "Authorization", "        ", "        ", "Proxy",
        "SSL Validation", "Expected Status", "Verify Response"
    };
    
    /**
     * CSS style for unfocused text fields and areas.
     * Defines a dark theme with white text, subtle borders, and rounded corners.
     */
    private static final String FIELD_STYLE_UNFOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #888888; -fx-border-radius: 5px;";

    /**
     * CSS style for focused text fields and areas.
     * Similar to unfocused but with a blue border to indicate focus.
     */
    private static final String FIELD_STYLE_FOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #4A90E2; -fx-border-width: 2px; -fx-prompt-text-fill: #888888; -fx-border-radius: 5px;";

    /**
     * Base CSS style for buttons.
     * Blue background with white text and rounded corners.
     */
    private static final String BUTTON_STYLE = 
        "-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

    /**
     * Hover style for buttons.
     * Lighter blue background on hover.
     */
    private static final String BUTTON_HOVER_STYLE = 
        "-fx-background-color: #6AB0FF; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

    /**
     * CSS style for highlighting placeholders like {{variable}}.
     * Uses an orange fill color.
     */
    private static final String HIGHLIGHT_STYLE = 
        "-fx-fill: #FF8C00;"; // Orange color for {{anytext}}

    /**
     * Inline CSS stylesheet for custom styling of table views, scroll panes, text areas,
     * combo boxes, and other UI components. Applies a dark theme with custom scroll bars.
     */
    private static final String CSS = """
        .table-view .scroll-bar:vertical,
        .table-view .scroll-bar:horizontal,
        .scroll-pane .scroll-bar:vertical,
        .scroll-pane .scroll-bar:horizontal {
            -fx-background-color: #252525;
        }
        .table-view .scroll-bar:vertical .track,
        .table-view .scroll-bar:horizontal .track,
        .scroll-pane .scroll-bar:vertical .track,
        .scroll-pane .scroll-bar:horizontal .track {
            -fx-background-color: #252525;
            -fx-border-color: transparent;
            -fx-background-radius: 0px;
        }
        .table-view .scroll-bar:vertical .thumb,
        .table-view .scroll-bar:horizontal .thumb,
        .scroll-pane .scroll-bar:vertical .thumb,
        .scroll-pane .scroll-bar:horizontal .thumb {
            -fx-background-color: #3C3F41;
            -fx-background-radius: 5px;
        }
        .table-view .scroll-bar:vertical .thumb:hover,
        .table-view .scroll-bar:horizontal .thumb:hover,
        .scroll-pane .scroll-bar:vertical .thumb:hover,
        .scroll-pane .scroll-bar:horizontal .thumb:hover {
            -fx-background-color: #4A90E2;
        }
        .table-view .scroll-bar:vertical .thumb:pressed,
        .table-view .scroll-bar:horizontal .thumb:pressed,
        .scroll-pane .scroll-bar:vertical .thumb:pressed,
        .scroll-pane .scroll-bar:horizontal .thumb:pressed {
            -fx-background-color: #4A90E2;
        }
        .table-view .scroll-bar .increment-button,
        .table-view .scroll-bar .decrement-button,
        .scroll-pane .scroll-bar .increment-button,
        .scroll-pane .scroll-bar .decrement-button {
            -fx-background-color: #252525;
            -fx-border-color: transparent;
        }
        .table-view .scroll-bar .increment-arrow,
        .table-view .scroll-bar .decrement-arrow,
        .scroll-pane .scroll-bar .increment-arrow,
        .scroll-pane .scroll-bar .decrement-arrow {
            -fx-shape: " ";
            -fx-background-color: #3C3F41;
        }
        .table-view .scroll-bar:vertical .increment-arrow:hover,
        .table-view .scroll-bar:vertical .decrement-arrow:hover,
        .table-view .scroll-bar:horizontal .increment-arrow:hover,
        .table-view .scroll-bar:horizontal .decrement-arrow:hover,
        .scroll-pane .scroll-bar:vertical .increment-arrow:hover,
        .scroll-pane .scroll-bar:vertical .decrement-arrow:hover,
        .scroll-pane .scroll-bar:horizontal .increment-arrow:hover,
        .scroll-pane .scroll-bar:horizontal .decrement-arrow:hover {
            -fx-background-color: #4A90E2;
        }
        .scroll-pane,
        .scroll-pane .viewport {
            -fx-background-color: #2E2E2E;
        }
        .table-view .table-row-cell {
            -fx-cell-size: 30px;
            -fx-pref-height: 30px;
            -fx-min-height: 30px;
            -fx-max-height: 30px;
        }
        .table-view .table-cell .text-field {
            -fx-pref-height: 26px;
            -fx-max-height: 26px;
            -fx-min-height: 26px;
            -fx-padding: 2px;
        }
        .text-area {
            -fx-font-family: 'Consolas', 'Menlo', 'Courier New', monospace;
            -fx-font-size: 12px;
            -fx-padding: 5px;
        }
        .text-field:disabled {
            -fx-background-color: #3C3F41;
            -fx-text-fill: #888888;
            -fx-opacity: 1.0;
        }
        .combo-box {
            -fx-background-color: #2E2E2E;
            -fx-text-fill: white;
            -fx-border-color: #3C3F41;
            -fx-border-width: 1px;
            -fx-border-radius: 5px;
        }
        .combo-box:focused {
            -fx-border-color: #4A90E2;
            -fx-border-width: 2px;
        }
        .combo-box .list-cell {
            -fx-background-color: #2E2E2E;
            -fx-text-fill: white;
        }
        .highlight-orange {
            -fx-fill: #FF8C00;
        }
        """;

    /**
     * Fixed height for text fields in the UI.
     */
    private static final double TEXT_FIELD_HEIGHT = 30.0;

    /**
     * ObjectMapper instance configured for pretty-printing JSON with ordered keys.
     * Used for formatting and parsing JSON payloads.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    /**
     * Defines the preferred order of top-level keys in the JSON payload.
     * Ensures consistent formatting when serializing.
     */
    private static final String[] PAYLOAD_TOP_LEVEL_KEY_ORDER = {"client_timestamp", "fields"};

    /**
     * Defines the preferred order of keys within each field object in the JSON payload.
     * Ensures consistent formatting for field properties.
     */
    private static final String[] PAYLOAD_FIELDS_KEY_ORDER = {
        "page_number", "role", "name", "width", "x", "y", "type", "required", "height"
    };

    // UI and state management fields
    /**
     * Label used to display status messages and errors in the UI.
     */
    private Label statusLabel;

    /**
     * Main vertical layout container for the application UI.
     */
    private VBox mainLayout;

    /**
     * The currently loaded file, if any.
     */
    private File loadedFile;

    /**
     * Flag indicating whether the current document has unsaved modifications.
     */
    private boolean isModified;

    /**
     * Manager for the test data table, handling row additions, deletions, and data validation.
     */
    private TableManager tableManager;

    /**
     * Manager for additional UI components like buttons, text fields, and JSON viewers.
     */
    private UIComponentsManager uiComponents;
    
    /**
     * Getter method to access the HIGHLIGHT_STYLE constant.
     * Used by UI components for applying placeholder highlighting.
     * 
     * @return The CSS style string for highlighting.
     */
    public static String getHighlightStyle() {
        return HIGHLIGHT_STYLE;
    }
    
    /**
     * Entry point for the JavaFX application.
     * Initializes the UI components, sets up the scene with custom CSS,
     * and displays the main window.
     * 
     * @param primaryStage The primary stage for the application.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
        	 // Set the window icon
            try {
            	Image backgroundImage = new Image("file:" + UIConstants.UI_ICON);
                primaryStage.getIcons().add(backgroundImage);
            } catch (Exception e) {
                System.err.println("Error setting window icon: " + e.getMessage());
            }

            // Initialize status label for displaying messages
            statusLabel = new Label();
            statusLabel.setStyle("-fx-text-fill: #FF5555;");
            statusLabel.setWrapText(true);

            // Create managers for table and UI components
            tableManager = new TableManager(COLUMN_NAMES, statusLabel, this);
            uiComponents = new UIComponentsManager(tableManager.getTable(), statusLabel, COLUMN_NAMES, tableManager, this);

            // Get the main table view
            TableView<String[]> table = tableManager.getTable();
            
            // Create button layout and combine with table
            VBox buttonsVBox = uiComponents.createButtonsVBox(primaryStage, this::checkUnsavedChanges, this::saveToFile, this::saveAsToFile);
            HBox tableWithButtons = new HBox(10, table, buttonsVBox);
            tableWithButtons.setStyle("-fx-background-color: #2E2E2E;");
            tableWithButtons.prefHeightProperty().bind(table.prefHeightProperty());
            HBox.setHgrow(table, Priority.ALWAYS);
            HBox.setHgrow(buttonsVBox, Priority.NEVER);
            VBox.setVgrow(tableWithButtons, Priority.NEVER);
            table.prefHeightProperty().bind(primaryStage.heightProperty().multiply(0.66));

            // Create additional content area (e.g., JSON viewer)
            VBox additionalContent = uiComponents.createAdditionalContent();
            ScrollPane scrollPane = new ScrollPane(additionalContent);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: #2E2E2E;");

            // Create text fields box for quick input
            HBox textFieldsBox = uiComponents.createTextFieldsBox();

            // Assemble main layout
            mainLayout = new VBox(10, tableWithButtons, textFieldsBox, scrollPane);
            mainLayout.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 10px; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
            mainLayout.setAlignment(Pos.TOP_CENTER);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            // Update initial button states
            uiComponents.updateButtonStates();

            // Set up scene with inline CSS
            Scene scene = new Scene(mainLayout);
            scene.getStylesheets().add("data:text/css," + CSS.replaceAll("\n", "%0A"));
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setTitle("Table with JSON Viewer");
            primaryStage.show();
            primaryStage.setMaximized(true);
            primaryStage.setResizable(false);

            // Listener for CSS loading issues
            scene.getStylesheets().addListener((javafx.collections.ListChangeListener<String>) change -> {
                if (scene.getStylesheets().isEmpty()) {
                    System.err.println("CSS loading failed, falling back to default style.");
                    mainLayout.setStyle("-fx-background-color: #2E2E2E;");
                }
            });

        } catch (Exception e) {
            // Handle startup exceptions
            System.err.println("Application startup failed: " + e.getMessage());
            e.printStackTrace();
            showError("Failed to start application: " + e.getMessage());
        }
    }

    /**
     * Returns the currently loaded file.
     * 
     * @return The loaded File object, or null if none.
     */
    public File getLoadedFile() {
        return loadedFile;
    }

    /**
     * Sets the currently loaded file.
     * 
     * @param file The File to set as loaded.
     */
    public void setLoadedFile(File file) {
        this.loadedFile = file;
    }

    /**
     * Sets the modification flag for the document.
     * 
     * @param modified True if the document has unsaved changes.
     */
    public void setModified(boolean modified) {
        this.isModified = modified;
    }

    /**
     * Checks for unsaved changes and prompts the user if necessary.
     * Allows saving, discarding, or canceling the operation.
     * 
     * @param primaryStage The main stage for dialog display.
     * @return True if changes are handled and operation can proceed, false otherwise.
     */
    private boolean checkUnsavedChanges(Stage primaryStage) {
        if (isModified) {
            // Create confirmation dialog with multiple options
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes");
            alert.setContentText("Do you want to save your changes before proceeding?");
            ButtonType saveButton = new ButtonType("Save");
            ButtonType saveAsButton = new ButtonType("Save As");
            ButtonType discardButton = new ButtonType("Discard");
            ButtonType cancelButton = new ButtonType("Cancel");
            alert.getButtonTypes().setAll(saveButton, saveAsButton, discardButton, cancelButton);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == saveButton) {
                    // Save to existing file if loaded
                    if (loadedFile != null) {
                        return saveToFile(loadedFile, primaryStage);
                    } else {
                        return saveAsToFile(primaryStage);
                    }
                } else if (result.get() == saveAsButton) {
                    // Prompt for new file location
                    return saveAsToFile(primaryStage);
                } else if (result.get() == discardButton) {
                    // Discard changes
                    isModified = false;
                    return true;
                } else {
                    // Cancel operation
                    return false;
                }
            }
            return false;
        }
        return true; // No changes, proceed
    }

    /**
     * Saves the current table data to an Excel file.
     * Updates env.json if successful and shows confirmation.
     * 
     * @param file The target file to save to.
     * @param primaryStage The main stage for dialog display.
     * @return True if save was successful, false otherwise.
     */
    private boolean saveToFile(File file, Stage primaryStage) {
        TableView<String[]> table = tableManager.getTable();
        File targetFile = (file != null) ? file : loadedFile;
        if (targetFile == null) {
            return false;
        }
        // Check write permissions
        if (!targetFile.exists() && !targetFile.getParentFile().canWrite()) {
            showError("Cannot write to directory: " + targetFile.getParentFile().getAbsolutePath());
            return false;
        }
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Create sheet and header row
            Sheet sheet = workbook.createSheet("Test Data");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COLUMN_NAMES.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(COLUMN_NAMES[i]);
            }
            // Populate data rows
            for (int i = 0; i < table.getItems().size(); i++) {
                Row row = sheet.createRow(i + 1);
                String[] data = table.getItems().get(i);
                for (int j = 0; j < data.length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(data[j] != null ? data[j] : "");
                }
            }
            // Auto-size columns
            for (int i = 0; i < COLUMN_NAMES.length; i++) {
                sheet.autoSizeColumn(i);
            }
            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(targetFile)) {
                workbook.write(fileOut);
                fileOut.flush();
            } catch (IOException ex) {
                // Handle specific IO exceptions
                String message = "Failed to save file: " + ex.getMessage();
                if (ex instanceof java.nio.file.AccessDeniedException) {
                    message = "Permission denied while saving file: " + targetFile.getAbsolutePath();
                } else if (ex instanceof java.nio.file.NoSuchFileException) {
                    message = "Invalid file path: " + targetFile.getAbsolutePath();
                }
                showError(message);
                return false;
            }
            // Update state and show success
            isModified = false;
            loadedFile = targetFile;
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("File Saved");
            alert.setContentText("Test case saved successfully to " + targetFile.getAbsolutePath());
            alert.showAndWait();
            // Update environment JSON
            try {
                EnvJsonUpdater.updateEnvJsonFromTable(tableManager.getTable());
            } catch (IOException ex) {
                showError("Failed to update env.json: " + ex.getMessage());
            }
            return true;
        } catch (IOException ex) {
            showError("Failed to save file: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Prompts the user to select a new file location and saves the data there.
     * 
     * @param primaryStage The main stage for the file chooser dialog.
     * @return True if save was successful, false otherwise.
     */
    private boolean saveAsToFile(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Test Case");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Documents"));
        fileChooser.setInitialFileName("TestCase.xlsx");
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            return saveToFile(file, primaryStage);
        }
        return false;
    }

    /**
     * Formats a JSON string with pretty-printing and enforces key ordering for payloads.
     * Handles top-level keys and nested 'fields' array keys specifically.
     * 
     * @param input The raw JSON string to format.
     * @param statusLabel Label to update with error messages if parsing fails.
     * @return The formatted JSON string, or original input on error.
     */
    public static String formatJson(String input, Label statusLabel) {
        if (input == null || input.trim().isEmpty()) {
            return input != null ? input : "";
        }
        try {
            ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

            // Parse input JSON
            Object parsedJson = mapper.readValue(input, Object.class);

            // If it's a map (expected for payload), reorder keys
            if (parsedJson instanceof Map) {
                Map<String, Object> inputMap = (Map<String, Object>) parsedJson;
                Map<String, Object> orderedMap = new LinkedHashMap<>();

                // Reorder top-level keys
                for (String key : PAYLOAD_TOP_LEVEL_KEY_ORDER) {
                    if (inputMap.containsKey(key)) {
                        Object value = inputMap.get(key);
                        if (key.equals("fields") && value instanceof List) {
                            // Reorder each object in fields array
                            List<Map<String, Object>> fields = (List<Map<String, Object>>) value;
                            List<Map<String, Object>> orderedFields = new ArrayList<>();
                            for (Map<String, Object> field : fields) {
                                Map<String, Object> orderedField = new LinkedHashMap<>();
                                for (String fieldKey : PAYLOAD_FIELDS_KEY_ORDER) {
                                    if (field.containsKey(fieldKey)) {
                                        orderedField.put(fieldKey, field.get(fieldKey));
                                    }
                                }
                                orderedFields.add(orderedField);
                            }
                            orderedMap.put(key, orderedFields);
                        } else {
                            orderedMap.put(key, value);
                        }
                    }
                }
                // Add any remaining keys (for robustness)
                for (String key : inputMap.keySet()) {
                    if (!orderedMap.containsKey(key)) {
                        orderedMap.put(key, inputMap.get(key));
                    }
                }
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(orderedMap);
            }
            // Fallback for non-map JSON (e.g., arrays, primitives)
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedJson);
        } catch (Exception e) {
            // Report error on UI thread
            Platform.runLater(() -> statusLabel.setText("Invalid JSON: " + e.getMessage()));
            return input;
        }
    }

    /**
     * Validates a Test ID string against rules: unique, numeric, <=5 digits.
     * 
     * @param testId The ID to validate.
     * @param testIds Set of existing IDs for uniqueness check.
     * @param currentId The current row's ID (to allow self-validation).
     * @return True if valid, false otherwise.
     */
    public static boolean isValidTestId(String testId, Set<String> testIds, String currentId) {
        if (testId == null || testId.isEmpty()) {
            return false;
        }
        if (!testId.equals(currentId) && testIds.contains(testId)) {
            return false;
        }
        if (testId.length() > 5) {
            return false;
        }
        if (!testId.matches("[0-9]+")) {
            return false;
        }
        return true;
    }

    /**
     * Displays an error dialog to the user.
     * 
     * @param message The error message to show.
     */
    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    /**
     * Applies real-time syntax highlighting to a TextField for placeholders like {{variable}}.
     * Updates style on text changes and focus events.
     * 
     * @param textField The TextField to enhance.
     */
    public static void applyHighlighting(TextField textField) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            TextFlow textFlow = new TextFlow();
            if (newVal == null || newVal.isEmpty()) {
                textField.setText(newVal);
                return;
            }
            // Split text around placeholders and apply styles
            String[] parts = newVal.split("(\\{\\{[^\\}]+\\}\\})");
            for (String part : parts) {
                Text text = new Text(part);
                if (part.matches("\\{\\{[^\\}]+\\}\\}")) {
                    text.setStyle(HIGHLIGHT_STYLE);
                } else {
                    text.setStyle("-fx-fill: white;");
                }
                textFlow.getChildren().add(text);
            }
            // Update field style based on presence of pattern
            textField.setStyle(partContainsPattern(newVal) ? HIGHLIGHT_STYLE : FIELD_STYLE_UNFOCUSED);
        });
        // Handle focus changes
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            textField.setStyle(newVal ? FIELD_STYLE_FOCUSED : 
                (partContainsPattern(textField.getText()) ? HIGHLIGHT_STYLE : FIELD_STYLE_UNFOCUSED));
        });
    }

    /**
     * Applies real-time syntax highlighting to a TextArea for placeholders like {{variable}}.
     * Updates style on text changes and focus events.
     * 
     * @param textArea The TextArea to enhance.
     */
    public static void applyHighlighting(TextArea textArea) {
        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            TextFlow textFlow = new TextFlow();
            if (newVal == null || newVal.isEmpty()) {
                textArea.setText(newVal);
                return;
            }
            // Split text around placeholders and apply styles
            String[] parts = newVal.split("(\\{\\{[^\\}]+\\}\\})");
            for (String part : parts) {
                Text text = new Text(part);
                if (part.matches("\\{\\{[^\\}]+\\}\\}")) {
                    text.setStyle(HIGHLIGHT_STYLE);
                } else {
                    text.setStyle("-fx-fill: white;");
                }
                textFlow.getChildren().add(text);
            }
            // Update area style based on presence of pattern
            textArea.setStyle(partContainsPattern(newVal) ? HIGHLIGHT_STYLE : "-fx-text-fill: white;");
        });
        // Handle focus changes
        textArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            textArea.setStyle(newVal ? FIELD_STYLE_FOCUSED : 
                (partContainsPattern(textArea.getText()) ? HIGHLIGHT_STYLE : "-fx-text-fill: white;"));
        });
    }

    /**
     * Checks if the given text contains any placeholder pattern like {{...}}.
     * 
     * @param text The text to check.
     * @return True if a pattern is found.
     */
    private static boolean partContainsPattern(String text) {
        return text != null && text.matches(".*\\{\\{[^\\}]+\\}\\}.*");
    }

    /**
     * Launches the JavaFX application.
     * 
     * @param args Command-line arguments (unused).
     */
    public static void main(String[] args) {
        launch(args);
    }
}