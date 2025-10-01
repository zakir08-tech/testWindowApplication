package com.test.window.gui;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class CreateEditAPITestTemplate extends Application {

    private static final String[] COLUMN_NAMES = {
        "Test ID", "Request", "End-Point", "Header (key)", "Header (value)",
        "Parameter (key)", "Parameter (value)", "Payload", "Payload Type",
        "Modify Payload (key)", "Modify Payload (value)", "Response (key) Name",
        "Capture (key) Value (env var)", "Authorization", "", "",
        "SSL Validation", "Expected Status", "Verify Response", "Test Description"
    };

    private static final String FIELD_STYLE_UNFOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #888888; -fx-border-radius: 5px;";

    private static final String FIELD_STYLE_FOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #4A90E2; -fx-border-width: 2px; -fx-prompt-text-fill: #888888; -fx-border-radius: 5px;";

    private static final String BUTTON_STYLE = 
        "-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

    private static final String BUTTON_HOVER_STYLE = 
        "-fx-background-color: #6AB0FF; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

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
        """;

    private static final double TEXT_FIELD_HEIGHT = 30.0;
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private Label statusLabel;
    private VBox mainLayout;
    private File loadedFile;
    private boolean isModified;
    private TableManager tableManager;
    private UIComponentsManager uiComponents;

    @Override
    public void start(Stage primaryStage) {
        try {
            statusLabel = new Label();
            statusLabel.setStyle("-fx-text-fill: #FF5555;");
            statusLabel.setWrapText(true);

            tableManager = new TableManager(COLUMN_NAMES, statusLabel);
            uiComponents = new UIComponentsManager(tableManager.getTable(), statusLabel, COLUMN_NAMES);

            TableView<String[]> table = tableManager.getTable();
            VBox buttonsVBox = uiComponents.createButtonsVBox(primaryStage, this::checkUnsavedChanges, this::saveToFile, this::saveAsToFile);
            HBox tableWithButtons = new HBox(10, table, buttonsVBox);
            tableWithButtons.setStyle("-fx-background-color: #2E2E2E;");
            tableWithButtons.prefHeightProperty().bind(table.prefHeightProperty());
            HBox.setHgrow(table, Priority.ALWAYS);
            HBox.setHgrow(buttonsVBox, Priority.NEVER);
            VBox.setVgrow(tableWithButtons, Priority.NEVER);
            table.prefHeightProperty().bind(primaryStage.heightProperty().multiply(0.6));

            VBox additionalContent = uiComponents.createAdditionalContent();
            ScrollPane scrollPane = new ScrollPane(additionalContent);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: #2E2E2E;");

            HBox textFieldsBox = uiComponents.createTextFieldsBox();

            mainLayout = new VBox(10, tableWithButtons, textFieldsBox, scrollPane);
            mainLayout.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 10px; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
            mainLayout.setAlignment(Pos.TOP_CENTER);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            uiComponents.updateButtonStates();

            Scene scene = new Scene(mainLayout);
            scene.getStylesheets().add("data:text/css," + CSS.replaceAll("\n", "%0A"));
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setMaximized(true);
            primaryStage.setTitle("Table with JSON Viewer");
            primaryStage.show();

            scene.getStylesheets().addListener((javafx.collections.ListChangeListener<String>) change -> {
                if (scene.getStylesheets().isEmpty()) {
                    System.err.println("CSS loading failed, falling back to default style.");
                    mainLayout.setStyle("-fx-background-color: #2E2E2E;");
                }
            });

        } catch (Exception e) {
            System.err.println("Application startup failed: " + e.getMessage());
            e.printStackTrace();
            showError("Failed to start application: " + e.getMessage());
        }
    }

    private String formatJson(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input != null ? input : "";
        }
        try {
            Object parsedJson = objectMapper.readValue(input, LinkedHashMap.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedJson);
        } catch (Exception e) {
            Platform.runLater(() -> statusLabel.setText("Invalid JSON: " + e.getMessage()));
            return input;
        }
    }

    private boolean isValidTestId(String testId, Set<String> testIds, String currentId, int rowIndex, TableView<String[]> table) {
        if (testId == null || testId.isEmpty()) {
            return false;
        }
        if (!testId.equals(currentId) && testIds.contains(testId)) {
            return false;
        }
        if (testId.length() > 5) {
            return false;
        }
        if (!testId.matches("^[0-9#]*$")) {
            return false;
        }
        if (testId.startsWith("#") && testId.matches(".*[0-9].*")) {
            return false;
        }
        if (testId.matches("^[0-9].*") && testId.contains("#")) {
            return false;
        }
        return true;
    }

    private boolean checkUnsavedChanges(Stage primaryStage) {
        if (isModified) {
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
                    if (loadedFile != null) {
                        return saveToFile(loadedFile, primaryStage);
                    } else {
                        return saveAsToFile(primaryStage);
                    }
                } else if (result.get() == saveAsButton) {
                    return saveAsToFile(primaryStage);
                } else if (result.get() == discardButton) {
                    isModified = false;
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
        return true;
    }

    private boolean saveToFile(File file, Stage primaryStage) {
        TableView<String[]> table = tableManager.getTable();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Test Data");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COLUMN_NAMES.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(COLUMN_NAMES[i]);
            }
            for (int i = 0; i < table.getItems().size(); i++) {
                Row row = sheet.createRow(i + 1);
                String[] data = table.getItems().get(i);
                for (int j = 0; j < data.length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(data[j] != null ? data[j] : "");
                }
            }
            for (int i = 0; i < COLUMN_NAMES.length; i++) {
                sheet.autoSizeColumn(i);
            }
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            } catch (IOException ex) {
                String message = "Failed to save file: " + ex.getMessage();
                if (ex instanceof java.nio.file.AccessDeniedException) {
                    message = "Permission denied while saving file: " + file.getAbsolutePath();
                } else if (ex instanceof java.nio.file.NoSuchFileException) {
                    message = "Invalid file path: " + file.getAbsolutePath();
                }
                showError(message);
                return false;
            }
            isModified = false;
            loadedFile = file;
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("File Saved");
            alert.setContentText("Test case saved successfully to " + file.getAbsolutePath());
            alert.showAndWait();
            return true;
        } catch (IOException ex) {
            showError("Failed to save file: " + ex.getMessage());
            return false;
        }
    }

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

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}