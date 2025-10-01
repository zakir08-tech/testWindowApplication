package com.test.window.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

public class CreateEditAPITestTemplate2 extends Application {

    private enum ColumnIndex {
        TEST_ID(0), REQUEST(1), END_POINT(2), HEADER_KEY(3), HEADER_VALUE(4),
        PARAM_KEY(5), PARAM_VALUE(6), PAYLOAD(7), PAYLOAD_TYPE(8),
        MODIFY_PAYLOAD_KEY(9), MODIFY_PAYLOAD_VALUE(10), RESPONSE_KEY_NAME(11),
        CAPTURE_VALUE(12), AUTHORIZATION(13), SSL_VALIDATION(16), EXPECTED_STATUS(17),
        VERIFY_RESPONSE(18), TEST_DESCRIPTION(19);

        private final int index;
        ColumnIndex(int index) { this.index = index; }
        public int getIndex() { return index; }
    }

    private static final String[] COLUMN_NAMES = {
        "Test ID", "Request", "End-Point", "Header (key)", "Header (value)",
        "Parameter (key)", "Parameter (value)", "Payload", "Payload Type",
        "Modify Payload (key)", "Modify Payload (value)", "Response (key) Name",
        "Capture (key) Value (env var)", "Authorization", "", "",
        "SSL Validation", "Expected Status", "Verify Response", "Test Description"
    };

    private static final ObservableList<String> REQUEST_OPTIONS = 
        FXCollections.observableArrayList("", "GET", "POST", "PUT", "PATCH", "DELETE");

    private static final ObservableList<String> PAYLOAD_TYPE_OPTIONS = 
        FXCollections.observableArrayList("", "none", "form-data", "x-www-form-urlencoded", "JSON");

    private static final ObservableList<String> AUTH_OPTIONS = 
        FXCollections.observableArrayList("", "Basic Auth", "Bearer Token");

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
    private ScrollPane headerFieldsScroll;
    private File loadedFile;
    private boolean isModified;
    private TextArea payloadField;
    private TextArea verifyResponseField;
    private Button addAboveButton, addBelowButton, moveUpButton, moveDownButton, 
                   deleteStepButton, deleteTestCaseButton, saveTestButton, createNewTestButton;

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
        TableView<String[]> table = (TableView<String[]>) ((HBox) mainLayout.getChildren().get(0)).getChildren().get(0);
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

    @Override
    public void start(Stage primaryStage) {
        try {
            TableView<String[]> table = createTable();
            VBox buttonsVBox = createButtonsVBox(table, primaryStage);
            HBox tableWithButtons = new HBox(10, table, buttonsVBox);
            tableWithButtons.setStyle("-fx-background-color: #2E2E2E;");
            tableWithButtons.prefHeightProperty().bind(table.prefHeightProperty());
            HBox.setHgrow(table, Priority.ALWAYS);
            HBox.setHgrow(buttonsVBox, Priority.NEVER);
            VBox.setVgrow(tableWithButtons, Priority.NEVER);
            table.prefHeightProperty().bind(primaryStage.heightProperty().multiply(0.6));

            VBox additionalContent = new VBox(10);
            additionalContent.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
            additionalContent.setAlignment(Pos.CENTER_LEFT);

            VBox headerFieldsVBox = new VBox(5);
            headerFieldsVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
            headerFieldsScroll = new ScrollPane(headerFieldsVBox);
            headerFieldsScroll.setStyle("-fx-background-color: #2E2E2E; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
            headerFieldsScroll.setFitToWidth(true);
            headerFieldsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            headerFieldsScroll.setPrefHeight(200);
            headerFieldsScroll.setMaxHeight(200);
            headerFieldsScroll.setMinHeight(200);
            headerFieldsVBox.setMaxHeight(190);

            VBox paramFieldsVBox = new VBox(5);
            paramFieldsVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
            ScrollPane paramListField = new ScrollPane(paramFieldsVBox);
            paramListField.setStyle("-fx-background-color: #2E2E2E; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
            paramListField.setFitToWidth(true);
            paramListField.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            paramListField.setPrefHeight(200);
            paramListField.setMaxHeight(200);
            paramListField.setMinHeight(200);
            paramFieldsVBox.setMaxHeight(190);

            VBox modifyPayloadVBox = new VBox(5);
            modifyPayloadVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
            ScrollPane modifyPayloadScroll = new ScrollPane(modifyPayloadVBox);
            modifyPayloadScroll.setStyle("-fx-background-color: #2E2E2E; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
            modifyPayloadScroll.setFitToWidth(true);
            modifyPayloadScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            modifyPayloadScroll.setPrefHeight(200);
            modifyPayloadScroll.setMaxHeight(200);
            modifyPayloadScroll.setMinHeight(200);
            modifyPayloadVBox.setMaxHeight(190);

            VBox responseCaptureVBox = new VBox(5);
            responseCaptureVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
            ScrollPane responseCaptureScroll = new ScrollPane(responseCaptureVBox);
            responseCaptureScroll.setStyle("-fx-background-color: #2E2E2E; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
            responseCaptureScroll.setFitToWidth(true);
            responseCaptureScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            responseCaptureScroll.setPrefHeight(200);
            responseCaptureScroll.setMaxHeight(200);
            responseCaptureScroll.setMinHeight(200);
            responseCaptureVBox.setMaxHeight(190);

            payloadField = new TextArea();
            payloadField.setPromptText("Payload");
            payloadField.setStyle(FIELD_STYLE_UNFOCUSED);
            payloadField.setPrefHeight(200);
            payloadField.setMinHeight(200);
            payloadField.setMaxHeight(200);
            payloadField.setWrapText(true);
            payloadField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                payloadField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
            });

            payloadField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.TAB) {
                    int selectedIndex = table.getSelectionModel().getSelectedIndex();
                    if (selectedIndex >= 0) {
                        String rawText = payloadField.getText();
                        table.getItems().get(selectedIndex)[ColumnIndex.PAYLOAD.getIndex()] = rawText;
                        isModified = true;
                        table.refresh();
                        String formattedText = formatJson(rawText);
                        payloadField.setText(formattedText);
                    }
                    if (!headerFieldsVBox.getChildren().isEmpty()) {
                        HBox firstHeaderPair = (HBox) headerFieldsVBox.getChildren().get(0);
                        TextField firstHeaderField = (TextField) firstHeaderPair.getChildren().get(0);
                        firstHeaderField.requestFocus();
                    } else {
                        table.requestFocus();
                        if (selectedIndex >= 0) {
                            table.getSelectionModel().select(selectedIndex);
                            table.getFocusModel().focus(selectedIndex, table.getColumns().get(0));
                        }
                    }
                    event.consume();
                }
            });

            verifyResponseField = new TextArea();
            verifyResponseField.setPromptText("Verify Response");
            verifyResponseField.setStyle(FIELD_STYLE_UNFOCUSED);
            verifyResponseField.setPrefHeight(200);
            verifyResponseField.setMinHeight(200);
            verifyResponseField.setMaxHeight(200);
            verifyResponseField.setWrapText(true);
            verifyResponseField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                verifyResponseField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
            });

            verifyResponseField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.TAB) {
                    int selectedIndex = table.getSelectionModel().getSelectedIndex();
                    if (selectedIndex >= 0) {
                        String rawText = verifyResponseField.getText();
                        table.getItems().get(selectedIndex)[ColumnIndex.VERIFY_RESPONSE.getIndex()] = rawText;
                        isModified = true;
                        table.refresh();
                        String formattedText = formatJson(rawText);
                        verifyResponseField.setText(formattedText);
                    }
                    table.requestFocus();
                    if (selectedIndex >= 0) {
                        table.getSelectionModel().select(selectedIndex);
                        table.getFocusModel().focus(selectedIndex, table.getColumns().get(0));
                    }
                    event.consume();
                }
            });

            GridPane additionalFields = new GridPane();
            additionalFields.setHgap(10);
            additionalFields.setVgap(10);
            additionalFields.setAlignment(Pos.CENTER_LEFT);
            additionalFields.add(headerFieldsScroll, 0, 0);
            GridPane.setValignment(headerFieldsScroll, VPos.TOP);
            additionalFields.add(modifyPayloadScroll, 0, 1);
            GridPane.setValignment(modifyPayloadScroll, VPos.TOP);
            additionalFields.add(paramListField, 1, 0);
            GridPane.setValignment(paramListField, VPos.TOP);
            additionalFields.add(responseCaptureScroll, 1, 1);
            GridPane.setValignment(responseCaptureScroll, VPos.TOP);
            additionalFields.add(payloadField, 2, 0);
            GridPane.setValignment(payloadField, VPos.TOP);
            additionalFields.add(verifyResponseField, 2, 1);
            GridPane.setValignment(verifyResponseField, VPos.TOP);

            ColumnConstraints col1 = new ColumnConstraints();
            col1.setPercentWidth(26.73);
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setPercentWidth(26.73);
            ColumnConstraints col3 = new ColumnConstraints();
            col3.setPercentWidth(46.54);
            additionalFields.getColumnConstraints().addAll(col1, col2, col3);
            additionalContent.getChildren().add(additionalFields);

            HBox textFieldsBox = createTextFieldsBox(table, additionalFields);

            table.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
                headerFieldsVBox.getChildren().clear();
                modifyPayloadVBox.getChildren().clear();
                paramFieldsVBox.getChildren().clear();
                responseCaptureVBox.getChildren().clear();
                TextField endpointField = (TextField) textFieldsBox.getChildren().get(0);
                ComboBox<String> authComboBox = (ComboBox<String>) textFieldsBox.getChildren().get(1);
                TextField authField1 = (TextField) textFieldsBox.getChildren().get(2);
                TextField authField2 = (TextField) textFieldsBox.getChildren().get(3);
                if (newItem != null) {
                    int selectedIndex = table.getSelectionModel().getSelectedIndex();
                    String testId = newItem[ColumnIndex.TEST_ID.getIndex()];
                    Set<String> testIds = new HashSet<>();
                    for (String[] row : table.getItems()) {
                        if (row[ColumnIndex.TEST_ID.getIndex()] != null && !row[ColumnIndex.TEST_ID.getIndex()].isEmpty()) {
                            testIds.add(row[ColumnIndex.TEST_ID.getIndex()]);
                        }
                    }
                    boolean isValid = isValidTestId(testId, testIds, testId, selectedIndex, table);
                    endpointField.setDisable(!isValid);
                    authComboBox.setDisable(!isValid);
                    authField1.setDisable(!isValid);
                    authField2.setDisable(!isValid);
                    if (!isValid) {
                        payloadField.clear();
                        verifyResponseField.clear();
                        endpointField.clear();
                        authComboBox.getSelectionModel().clearSelection();
                        authField1.clear();
                        authField2.clear();
                        return;
                    }

                    endpointField.setText(newItem[ColumnIndex.END_POINT.getIndex()] != null ? newItem[ColumnIndex.END_POINT.getIndex()] : "");
                    String authData = newItem[ColumnIndex.AUTHORIZATION.getIndex()] != null ? newItem[ColumnIndex.AUTHORIZATION.getIndex()] : "";
                    if (authData.startsWith("Basic:")) {
                        authComboBox.setValue("Basic Auth");
                        String[] parts = authData.split(":", 3);
                        authField1.setText(parts.length > 1 ? parts[1] : "");
                        authField2.setText(parts.length > 2 ? parts[2] : "");
                        authField1.setPromptText("Username");
                        authField2.setPromptText("Password");
                        authField2.setDisable(false);
                    } else if (authData.startsWith("Bearer:")) {
                        authComboBox.setValue("Bearer Token");
                        String[] parts = authData.split(":", 2);
                        authField1.setText(parts.length > 1 ? parts[1] : "");
                        authField2.setText("");
                        authField1.setPromptText("Token");
                        authField2.setPromptText("");
                        authField2.setDisable(true);
                    } else {
                        authComboBox.setValue("");
                        authField1.setText("");
                        authField2.setText("");
                        authField1.setPromptText("Username");
                        authField2.setPromptText("Password");
                        authField2.setDisable(false);
                    }

                    int start = selectedIndex;
                    while (start >= 0 && (table.getItems().get(start)[ColumnIndex.TEST_ID.getIndex()] == null || table.getItems().get(start)[ColumnIndex.TEST_ID.getIndex()].isEmpty())) {
                        start--;
                    }
                    if (start < 0) start = 0;

                    List<Integer> rowIndices = new ArrayList<>();
                    for (int i = start; i < table.getItems().size(); i++) {
                        String[] r = table.getItems().get(i);
                        if (i > start && r[ColumnIndex.TEST_ID.getIndex()] != null && !r[ColumnIndex.TEST_ID.getIndex()].isEmpty()) break;
                        rowIndices.add(i);
                    }

                    for (Integer rowIndex : rowIndices) {
                        String[] row = table.getItems().get(rowIndex);
                        TextField headerKeyField = new TextField(row[ColumnIndex.HEADER_KEY.getIndex()] != null ? row[ColumnIndex.HEADER_KEY.getIndex()] : "");
                        headerKeyField.setPromptText("Header Key");
                        headerKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                        headerKeyField.setPrefHeight(TEXT_FIELD_HEIGHT);
                        headerKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                            headerKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                        });

                        TextField headerValueField = new TextField(row[ColumnIndex.HEADER_VALUE.getIndex()] != null ? row[ColumnIndex.HEADER_VALUE.getIndex()] : "");
                        headerValueField.setPromptText("Header Value");
                        headerValueField.setStyle(FIELD_STYLE_UNFOCUSED);
                        headerValueField.setPrefHeight(TEXT_FIELD_HEIGHT);
                        headerValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                            headerValueField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                        });

                        HBox headerPair = new HBox(5, headerKeyField, headerValueField);
                        headerPair.setAlignment(Pos.CENTER_LEFT);
                        headerKeyField.prefWidthProperty().bind(headerFieldsScroll.widthProperty().multiply(0.5).subtract(8.5));
                        headerKeyField.maxWidthProperty().bind(headerKeyField.prefWidthProperty());
                        headerKeyField.minWidthProperty().bind(headerKeyField.prefWidthProperty());
                        headerValueField.prefWidthProperty().bind(headerFieldsScroll.widthProperty().multiply(0.5).subtract(8.5));
                        headerValueField.maxWidthProperty().bind(headerValueField.prefWidthProperty());
                        headerValueField.minWidthProperty().bind(headerValueField.prefWidthProperty());

                        headerKeyField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                            table.getItems().get(rowIndex)[ColumnIndex.HEADER_KEY.getIndex()] = newVal2.trim();
                            isModified = true;
                            table.refresh();
                        });

                        headerValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                            table.getItems().get(rowIndex)[ColumnIndex.HEADER_VALUE.getIndex()] = newVal2.trim();
                            isModified = true;
                            table.refresh();
                        });

                        headerFieldsVBox.getChildren().add(headerPair);

                        TextField modifyKeyField = new TextField(row[ColumnIndex.MODIFY_PAYLOAD_KEY.getIndex()] != null ? row[ColumnIndex.MODIFY_PAYLOAD_KEY.getIndex()] : "");
                        modifyKeyField.setPromptText("Modify Payload Key");
                        modifyKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                        modifyKeyField.setPrefHeight(TEXT_FIELD_HEIGHT);
                        modifyKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                            modifyKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                        });

                        TextField modifyValueField = new TextField(row[ColumnIndex.MODIFY_PAYLOAD_VALUE.getIndex()] != null ? row[ColumnIndex.MODIFY_PAYLOAD_VALUE.getIndex()] : "");
                        modifyValueField.setPromptText("Modify Payload Value");
                        modifyValueField.setStyle(FIELD_STYLE_UNFOCUSED);
                        modifyValueField.setPrefHeight(TEXT_FIELD_HEIGHT);
                        modifyValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                            modifyValueField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                        });

                        HBox modifyPair = new HBox(5, modifyKeyField, modifyValueField);
                        modifyPair.setAlignment(Pos.CENTER_LEFT);
                        modifyKeyField.prefWidthProperty().bind(modifyPayloadScroll.widthProperty().multiply(0.5).subtract(8.5));
                        modifyKeyField.maxWidthProperty().bind(modifyKeyField.prefWidthProperty());
                        modifyKeyField.minWidthProperty().bind(modifyKeyField.prefWidthProperty());
                        modifyValueField.prefWidthProperty().bind(modifyPayloadScroll.widthProperty().multiply(0.5).subtract(8.5));
                        modifyValueField.maxWidthProperty().bind(modifyValueField.prefWidthProperty());
                        modifyValueField.minWidthProperty().bind(modifyValueField.prefWidthProperty());

                        modifyKeyField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                            table.getItems().get(rowIndex)[ColumnIndex.MODIFY_PAYLOAD_KEY.getIndex()] = newVal2.trim();
                            isModified = true;
                            table.refresh();
                        });

                        modifyValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                            table.getItems().get(rowIndex)[ColumnIndex.MODIFY_PAYLOAD_VALUE.getIndex()] = newVal2.trim();
                            isModified = true;
                            table.refresh();
                        });

                        modifyPayloadVBox.getChildren().add(modifyPair);

                        TextField paramKeyField = new TextField(row[ColumnIndex.PARAM_KEY.getIndex()] != null ? row[ColumnIndex.PARAM_KEY.getIndex()] : "");
                        paramKeyField.setPromptText("Parameter Key");
                        paramKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                        paramKeyField.setPrefHeight(TEXT_FIELD_HEIGHT);
                        paramKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                            paramKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                        });

                        TextField paramValueField = new TextField(row[ColumnIndex.PARAM_VALUE.getIndex()] != null ? row[ColumnIndex.PARAM_VALUE.getIndex()] : "");
                        paramValueField.setPromptText("Parameter Value");
                        paramValueField.setStyle(FIELD_STYLE_UNFOCUSED);
                        paramValueField.setPrefHeight(TEXT_FIELD_HEIGHT);
                        paramValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                            paramValueField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                        });

                        HBox paramPair = new HBox(5, paramKeyField, paramValueField);
                        paramPair.setAlignment(Pos.CENTER_LEFT);
                        paramKeyField.prefWidthProperty().bind(paramListField.widthProperty().multiply(0.5).subtract(8.5));
                        paramKeyField.maxWidthProperty().bind(paramKeyField.prefWidthProperty());
                        paramKeyField.minWidthProperty().bind(paramKeyField.prefWidthProperty());
                        paramValueField.prefWidthProperty().bind(paramListField.widthProperty().multiply(0.5).subtract(8.5));
                        paramValueField.maxWidthProperty().bind(paramValueField.prefWidthProperty());
                        paramValueField.minWidthProperty().bind(paramValueField.prefWidthProperty());

                        paramKeyField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                            table.getItems().get(rowIndex)[ColumnIndex.PARAM_KEY.getIndex()] = newVal2.trim();
                            isModified = true;
                            table.refresh();
                        });

                        paramValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                            table.getItems().get(rowIndex)[ColumnIndex.PARAM_VALUE.getIndex()] = newVal2.trim();
                            isModified = true;
                            table.refresh();
                        });

                        paramFieldsVBox.getChildren().add(paramPair);

                        TextField responseKeyField = new TextField(row[ColumnIndex.RESPONSE_KEY_NAME.getIndex()] != null ? row[ColumnIndex.RESPONSE_KEY_NAME.getIndex()] : "");
                        responseKeyField.setPromptText("Response Key Name");
                        responseKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                        responseKeyField.setPrefHeight(TEXT_FIELD_HEIGHT);
                        responseKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                            responseKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                        });

                        TextField captureValueField = new TextField(row[ColumnIndex.CAPTURE_VALUE.getIndex()] != null ? row[ColumnIndex.CAPTURE_VALUE.getIndex()] : "");
                        captureValueField.setPromptText("Capture Value (env var)");
                        captureValueField.setStyle(FIELD_STYLE_UNFOCUSED);
                        captureValueField.setPrefHeight(TEXT_FIELD_HEIGHT);
                        captureValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                            captureValueField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                        });

                        HBox responsePair = new HBox(5, responseKeyField, captureValueField);
                        responsePair.setAlignment(Pos.CENTER_LEFT);
                        responseKeyField.prefWidthProperty().bind(responseCaptureScroll.widthProperty().multiply(0.5).subtract(8.5));
                        responseKeyField.maxWidthProperty().bind(responseKeyField.prefWidthProperty());
                        responseKeyField.minWidthProperty().bind(responseKeyField.prefWidthProperty());
                        captureValueField.prefWidthProperty().bind(responseCaptureScroll.widthProperty().multiply(0.5).subtract(8.5));
                        captureValueField.maxWidthProperty().bind(captureValueField.prefWidthProperty());
                        captureValueField.minWidthProperty().bind(captureValueField.prefWidthProperty());

                        responseKeyField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                            table.getItems().get(rowIndex)[ColumnIndex.RESPONSE_KEY_NAME.getIndex()] = newVal2.trim();
                            isModified = true;
                            table.refresh();
                        });

                        captureValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                            table.getItems().get(rowIndex)[ColumnIndex.CAPTURE_VALUE.getIndex()] = newVal2.trim();
                            isModified = true;
                            table.refresh();
                        });

                        responseCaptureVBox.getChildren().add(responsePair);
                    }

                    String payload = newItem[ColumnIndex.PAYLOAD.getIndex()] != null ? newItem[ColumnIndex.PAYLOAD.getIndex()] : "";
                    payloadField.setText(formatJson(payload));
                    String verify = newItem[ColumnIndex.VERIFY_RESPONSE.getIndex()] != null ? newItem[ColumnIndex.VERIFY_RESPONSE.getIndex()] : "";
                    verifyResponseField.setText(formatJson(verify));
                } else {
                    payloadField.clear();
                    verifyResponseField.clear();
                    endpointField.clear();
                    authComboBox.getSelectionModel().clearSelection();
                    authField1.clear();
                    authField2.clear();
                    endpointField.setDisable(true);
                    authComboBox.setDisable(true);
                    authField1.setDisable(true);
                    authField2.setDisable(true);
                }
            });

            payloadField.textProperty().addListener((obs, oldVal, newVal) -> {
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0) {
                    table.getItems().get(selectedIndex)[ColumnIndex.PAYLOAD.getIndex()] = newVal;
                    isModified = true;
                    table.refresh();
                    String formattedText = formatJson(newVal);
                    if (!formattedText.equals(newVal)) {
                        Platform.runLater(() -> payloadField.setText(formattedText));
                    }
                }
            });

            verifyResponseField.textProperty().addListener((obs, oldVal, newVal) -> {
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0) {
                    table.getItems().get(selectedIndex)[ColumnIndex.VERIFY_RESPONSE.getIndex()] = newVal;
                    isModified = true;
                    table.refresh();
                    String formattedText = formatJson(newVal);
                    if (!formattedText.equals(newVal)) {
                        Platform.runLater(() -> verifyResponseField.setText(formattedText));
                    }
                }
            });

            ScrollPane scrollPane = new ScrollPane(additionalContent);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: #2E2E2E;");

            mainLayout = new VBox(10, tableWithButtons, textFieldsBox, scrollPane);
            mainLayout.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 10px; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
            mainLayout.setAlignment(Pos.TOP_CENTER);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            updateButtonStates(table);

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

    private TableView<String[]> createTable() {
        TableView<String[]> table = new TableView<>();
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setStyle("-fx-background-color: #2E2E2E; -fx-table-cell-border-color: transparent; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
        table.setPrefWidth(480);

        Label placeholderLabel = new Label("No steps defined");
        placeholderLabel.setStyle("-fx-text-fill: white;");
        table.setPlaceholder(placeholderLabel);

        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            final int index = i;
            TableColumn<String[], String> column = new TableColumn<>(COLUMN_NAMES[i]);
            column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[index]));
            if (index == ColumnIndex.REQUEST.getIndex()) {
                column.setCellFactory(col -> new CustomComboBoxTableCell(table, index, REQUEST_OPTIONS));
                column.setGraphic(new Label(COLUMN_NAMES[i]) {{
                    setTooltip(new Tooltip("Double-click to select a request type"));
                }});
            } else {
                column.setCellFactory(col -> new CustomTextFieldTableCell(table, index, statusLabel));
            }
            double charWidth = 7.0;
            double minWidth = COLUMN_NAMES[i].length() * charWidth + 20;
            column.setMinWidth(minWidth);
            column.setPrefWidth(index == ColumnIndex.TEST_ID.getIndex() ? 50 : 100);
            column.setStyle("-fx-text-fill: white;");
            column.setOnEditCommit(event -> {
                String newValue = event.getNewValue();
                int colIndex = event.getTablePosition().getColumn();
                int rowIndex = event.getTablePosition().getRow();
                if (colIndex == ColumnIndex.EXPECTED_STATUS.getIndex() && !newValue.matches("\\d+|^$")) {
                    statusLabel.setText("Status must be a number");
                    return;
                }
                statusLabel.setText("");
                event.getTableView().getItems().get(rowIndex)[colIndex] = newValue;
                isModified = true;
                if (colIndex == ColumnIndex.PAYLOAD.getIndex() && rowIndex == table.getSelectionModel().getSelectedIndex()) {
                    if (payloadField != null) {
                        payloadField.setText(formatJson(newValue));
                    }
                    table.refresh();
                }
                if (colIndex == ColumnIndex.VERIFY_RESPONSE.getIndex() && rowIndex == table.getSelectionModel().getSelectedIndex()) {
                    if (verifyResponseField != null) {
                        verifyResponseField.setText(formatJson(newValue));
                    }
                    table.refresh();
                }
                if (colIndex == ColumnIndex.TEST_ID.getIndex() && rowIndex == table.getSelectionModel().getSelectedIndex()) {
                    HBox textFieldsBox = (HBox) mainLayout.getChildren().get(1);
                    TextField endpointField = (TextField) textFieldsBox.getChildren().get(0);
                    ComboBox<String> authComboBox = (ComboBox<String>) textFieldsBox.getChildren().get(1);
                    TextField authField1 = (TextField) textFieldsBox.getChildren().get(2);
                    TextField authField2 = (TextField) textFieldsBox.getChildren().get(3);
                    Set<String> testIds = new HashSet<>();
                    for (String[] row : table.getItems()) {
                        if (row[ColumnIndex.TEST_ID.getIndex()] != null && !row[ColumnIndex.TEST_ID.getIndex()].isEmpty()) {
                            testIds.add(row[ColumnIndex.TEST_ID.getIndex()]);
                        }
                    }
                    boolean isValid = isValidTestId(newValue, testIds, newValue, rowIndex, table);
                    endpointField.setDisable(!isValid);
                    authComboBox.setDisable(!isValid);
                    authField1.setDisable(!isValid);
                    authField2.setDisable(!isValid);
                }
            });
            table.getColumns().add(column);
        }

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> new TableRow<String[]>() {
            @Override
            protected void updateItem(String[] item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    int index = getIndex();
                    String currentStyle = getStyle();
                    if (currentStyle.contains("4A90E2")) {
                        return;
                    }
                    if (index == table.getSelectionModel().getSelectedIndex()) {
                        setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px;");
                    } else {
                        setStyle("-fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px;");
                    }
                }
            }
        });

        table.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            table.refresh();
            updateButtonStates(table);
        });

        table.getItems().addListener((javafx.collections.ListChangeListener<String[]>) c -> {
            isModified = true;
            if (!table.getItems().isEmpty() && table.getSelectionModel().getSelectedIndex() < 0) {
                table.getSelectionModel().select(0);
            }
            updateButtonStates(table);
        });

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY && !table.getSelectionModel().isEmpty()) {
                TablePosition<String[], ?> pos = table.getFocusModel().getFocusedCell();
                if (pos != null && pos.getColumn() == ColumnIndex.REQUEST.getIndex() && table.getColumns().get(ColumnIndex.REQUEST.getIndex()).isEditable()) {
                    table.edit(pos.getRow(), table.getColumns().get(ColumnIndex.REQUEST.getIndex()));
                    event.consume();
                }
            }
        });

        return table;
    }

    private VBox createButtonsVBox(TableView<String[]> table, Stage primaryStage) {
        VBox buttonsVBox = new VBox(10);
        buttonsVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 10px;");
        buttonsVBox.setAlignment(Pos.TOP_CENTER);

        Button addStepButton = new Button("Add Step");
        addStepButton.setStyle(BUTTON_STYLE);
        addStepButton.setTooltip(new Tooltip("Add a new test step to the table"));
        addStepButton.setOnAction(e -> {
            table.getItems().add(new String[COLUMN_NAMES.length]);
            table.getSelectionModel().select(table.getItems().size() - 1);
            table.refresh();
            HBox textFieldsBox = (HBox) mainLayout.getChildren().get(1);
            TextField endpointField = (TextField) textFieldsBox.getChildren().get(0);
            ComboBox<String> authComboBox = (ComboBox<String>) textFieldsBox.getChildren().get(1);
            TextField authField1 = (TextField) textFieldsBox.getChildren().get(2);
            TextField authField2 = (TextField) textFieldsBox.getChildren().get(3);
            endpointField.setDisable(true);
            authComboBox.setDisable(true);
            authField1.setDisable(true);
            authField2.setDisable(true);
            updateButtonStates(table);
        });
        addStepButton.setOnMouseEntered(e -> addStepButton.setStyle(BUTTON_HOVER_STYLE));
        addStepButton.setOnMouseExited(e -> addStepButton.setStyle(BUTTON_STYLE));

        addAboveButton = new Button("Add Above");
        addAboveButton.setStyle(BUTTON_STYLE);
        addAboveButton.setTooltip(new Tooltip("Add a new step above the selected row"));
        addAboveButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().add(selectedIndex, new String[COLUMN_NAMES.length]);
                table.getSelectionModel().select(selectedIndex);
                table.refresh();
                HBox textFieldsBox = (HBox) mainLayout.getChildren().get(1);
                TextField endpointField = (TextField) textFieldsBox.getChildren().get(0);
                ComboBox<String> authComboBox = (ComboBox<String>) textFieldsBox.getChildren().get(1);
                TextField authField1 = (TextField) textFieldsBox.getChildren().get(2);
                TextField authField2 = (TextField) textFieldsBox.getChildren().get(3);
                endpointField.setDisable(true);
                authComboBox.setDisable(true);
                authField1.setDisable(true);
                authField2.setDisable(true);
            }
            updateButtonStates(table);
        });
        addAboveButton.setOnMouseEntered(e -> addAboveButton.setStyle(BUTTON_HOVER_STYLE));
        addAboveButton.setOnMouseExited(e -> addAboveButton.setStyle(BUTTON_STYLE));

        addBelowButton = new Button("Add Below");
        addBelowButton.setStyle(BUTTON_STYLE);
        addBelowButton.setTooltip(new Tooltip("Add a new step below the selected row"));
        addBelowButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().add(selectedIndex + 1, new String[COLUMN_NAMES.length]);
                table.getSelectionModel().select(selectedIndex + 1);
                table.refresh();
                HBox textFieldsBox = (HBox) mainLayout.getChildren().get(1);
                TextField endpointField = (TextField) textFieldsBox.getChildren().get(0);
                ComboBox<String> authComboBox = (ComboBox<String>) textFieldsBox.getChildren().get(1);
                TextField authField1 = (TextField) textFieldsBox.getChildren().get(2);
                TextField authField2 = (TextField) textFieldsBox.getChildren().get(3);
                endpointField.setDisable(true);
                authComboBox.setDisable(true);
                authField1.setDisable(true);
                authField2.setDisable(true);
            }
            updateButtonStates(table);
        });
        addBelowButton.setOnMouseEntered(e -> addBelowButton.setStyle(BUTTON_HOVER_STYLE));
        addBelowButton.setOnMouseExited(e -> addBelowButton.setStyle(BUTTON_STYLE));

        moveUpButton = new Button("Move Up");
        moveUpButton.setStyle(BUTTON_STYLE);
        moveUpButton.setTooltip(new Tooltip("Move the selected step up"));
        moveUpButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex > 0) {
                ObservableList<String[]> items = table.getItems();
                String[] temp = items.get(selectedIndex - 1);
                items.set(selectedIndex - 1, items.get(selectedIndex));
                items.set(selectedIndex, temp);
                table.getSelectionModel().select(selectedIndex - 1);
                table.refresh();
            }
            updateButtonStates(table);
        });
        moveUpButton.setOnMouseEntered(e -> moveUpButton.setStyle(BUTTON_HOVER_STYLE));
        moveUpButton.setOnMouseExited(e -> moveUpButton.setStyle(BUTTON_STYLE));

        moveDownButton = new Button("Move Down");
        moveDownButton.setStyle(BUTTON_STYLE);
        moveDownButton.setTooltip(new Tooltip("Move the selected step down"));
        moveDownButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < table.getItems().size() - 1) {
                ObservableList<String[]> items = table.getItems();
                String[] temp = items.get(selectedIndex + 1);
                items.set(selectedIndex + 1, items.get(selectedIndex));
                items.set(selectedIndex, temp);
                table.getSelectionModel().select(selectedIndex + 1);
                table.refresh();
            }
            updateButtonStates(table);
        });
        moveDownButton.setOnMouseEntered(e -> moveDownButton.setStyle(BUTTON_HOVER_STYLE));
        moveDownButton.setOnMouseExited(e -> moveDownButton.setStyle(BUTTON_STYLE));

        deleteStepButton = new Button("Delete Step");
        deleteStepButton.setStyle(BUTTON_STYLE);
        deleteStepButton.setTooltip(new Tooltip("Delete the selected step"));
        deleteStepButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                String[] selectedRow = table.getItems().get(selectedIndex);
                String testId = selectedRow[ColumnIndex.TEST_ID.getIndex()];
                if (testId != null && !testId.isEmpty()) {
                    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmation.setTitle("Confirm Delete");
                    confirmation.setHeaderText("Delete Test Step");
                    confirmation.setContentText("The selected step has a Test ID: " + testId + ". Are you sure you want to delete it?");
                    Optional<ButtonType> result = confirmation.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        table.getItems().remove(selectedIndex);
                        table.refresh();
                    }
                } else {
                    table.getItems().remove(selectedIndex);
                    table.refresh();
                }
            }
            updateButtonStates(table);
        });
        deleteStepButton.setOnMouseEntered(e -> deleteStepButton.setStyle(BUTTON_HOVER_STYLE));
        deleteStepButton.setOnMouseExited(e -> deleteStepButton.setStyle(BUTTON_STYLE));

        deleteTestCaseButton = new Button("Delete Test Case");
        deleteTestCaseButton.setStyle(BUTTON_STYLE);
        deleteTestCaseButton.setTooltip(new Tooltip("Delete all steps for the selected test case"));
        deleteTestCaseButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                String[] selectedRow = table.getItems().get(selectedIndex);
                String testId = selectedRow[ColumnIndex.TEST_ID.getIndex()];
                if (testId == null || testId.isEmpty()) {
                    showError("Please select a row with a valid Test ID to delete the test case.");
                    return;
                }

                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                confirmation.setTitle("Confirm Delete Test Case");
                confirmation.setHeaderText("Delete Test Case: " + testId);
                confirmation.setContentText("Are you sure you want to delete all steps for Test ID: " + testId + "?");
                Optional<ButtonType> result = confirmation.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    int startIndex = selectedIndex;
                    while (startIndex > 0 && (table.getItems().get(startIndex - 1)[ColumnIndex.TEST_ID.getIndex()] == null || table.getItems().get(startIndex - 1)[ColumnIndex.TEST_ID.getIndex()].isEmpty())) {
                        startIndex--;
                    }
                    int endIndex = startIndex;
                    while (endIndex < table.getItems().size() && (table.getItems().get(endIndex)[ColumnIndex.TEST_ID.getIndex()] == null || table.getItems().get(endIndex)[ColumnIndex.TEST_ID.getIndex()].isEmpty() || table.getItems().get(endIndex)[ColumnIndex.TEST_ID.getIndex()].equals(testId))) {
                        endIndex++;
                    }
                    if (endIndex > startIndex) {
                        table.getItems().subList(startIndex, endIndex).clear();
                    } else {
                        table.getItems().remove(startIndex);
                    }
                    table.refresh();
                    if (!table.getItems().isEmpty()) {
                        int newIndex = Math.min(startIndex, table.getItems().size() - 1);
                        table.getSelectionModel().select(newIndex);
                    } else {
                        table.getSelectionModel().clearSelection();
                    }
                }
            }
            updateButtonStates(table);
        });
        deleteTestCaseButton.setOnMouseEntered(e -> deleteTestCaseButton.setStyle(BUTTON_HOVER_STYLE));
        deleteTestCaseButton.setOnMouseExited(e -> deleteTestCaseButton.setStyle(BUTTON_STYLE));

        saveTestButton = new Button("Save Test");
        saveTestButton.setStyle(BUTTON_STYLE);
        saveTestButton.setTooltip(new Tooltip("Save the test case to an Excel file"));
        saveTestButton.setOnAction(e -> {
            Alert saveOptionAlert = new Alert(Alert.AlertType.CONFIRMATION);
            saveOptionAlert.setTitle("Save Options");
            saveOptionAlert.setHeaderText("Save Test Case");
            saveOptionAlert.setContentText("Choose an option to save your test case:");
            ButtonType saveButton = new ButtonType("Save");
            ButtonType saveAsButton = new ButtonType("Save As");
            ButtonType cancelButton = new ButtonType("Cancel");
            saveOptionAlert.getButtonTypes().setAll(saveButton, saveAsButton, cancelButton);
            Optional<ButtonType> result = saveOptionAlert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == saveButton) {
                    if (loadedFile != null) {
                        saveToFile(loadedFile, primaryStage);
                    } else {
                        saveAsToFile(primaryStage);
                    }
                } else if (result.get() == saveAsButton) {
                    saveAsToFile(primaryStage);
                }
            }
            updateButtonStates(table);
        });
        saveTestButton.setOnMouseEntered(e -> saveTestButton.setStyle(BUTTON_HOVER_STYLE));
        saveTestButton.setOnMouseExited(e -> saveTestButton.setStyle(BUTTON_STYLE));

        Button loadTestButton = new Button("Load Test");
        loadTestButton.setStyle(BUTTON_STYLE);
        loadTestButton.setTooltip(new Tooltip("Load a test case from an Excel file"));
        loadTestButton.setOnAction(e -> {
            if (!checkUnsavedChanges(primaryStage)) {
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Test Case");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Documents"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                try (FileInputStream fileIn = new FileInputStream(file);
                     XSSFWorkbook workbook = new XSSFWorkbook(fileIn)) {
                    Sheet sheet = workbook.getSheetAt(0);
                    Row headerRow = sheet.getRow(0);
                    boolean headersValid = headerRow != null && headerRow.getPhysicalNumberOfCells() == COLUMN_NAMES.length;
                    if (headersValid) {
                        for (int i = 0; i < COLUMN_NAMES.length; i++) {
                            Cell cell = headerRow.getCell(i);
                            String headerValue = cell != null ? cell.getStringCellValue() : "";
                            if (!COLUMN_NAMES[i].equals(headerValue)) {
                                headersValid = false;
                                break;
                            }
                        }
                    }
                    if (!headersValid) {
                        String fileName = file.getName().replaceFirst("[.][^.]+$", "");
                        showError("Invalid test suite " + fileName + ". Upload the valid test suite.");
                        return;
                    }

                    table.getItems().clear();
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row != null) {
                            String[] data = new String[COLUMN_NAMES.length];
                            for (int j = 0; j < COLUMN_NAMES.length; j++) {
                                Cell cell = row.getCell(j);
                                data[j] = cell != null ? cell.toString() : "";
                            }
                            table.getItems().add(data);
                        }
                    }
                    table.refresh();
                    if (!table.getItems().isEmpty()) {
                        table.getSelectionModel().select(0);
                    }
                    isModified = false;
                    loadedFile = file;
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText("File Loaded");
                    alert.setContentText("Test case loaded successfully from " + file.getAbsolutePath());
                    alert.showAndWait();
                } catch (IOException ex) {
                    String fileName = file.getName().replaceFirst("[.][^.]+$", "");
                    showError("Invalid test suite " + fileName + ". Upload the valid test suite.");
                }
            }
            updateButtonStates(table);
        });
        loadTestButton.setOnMouseEntered(e -> loadTestButton.setStyle(BUTTON_HOVER_STYLE));
        loadTestButton.setOnMouseExited(e -> loadTestButton.setStyle(BUTTON_STYLE));

        createNewTestButton = new Button("Create New Test");
        createNewTestButton.setStyle(BUTTON_STYLE);
        createNewTestButton.setTooltip(new Tooltip("Start a new test case"));
        createNewTestButton.setOnAction(e -> {
            if (!checkUnsavedChanges(primaryStage)) {
                return;
            }
            table.getItems().clear();
            table.refresh();
            HBox textFieldsBox = (HBox) mainLayout.getChildren().get(1);
            TextField endpointField = (TextField) textFieldsBox.getChildren().get(0);
            ComboBox<String> authComboBox = (ComboBox<String>) textFieldsBox.getChildren().get(1);
            TextField authField1 = (TextField) textFieldsBox.getChildren().get(2);
            TextField authField2 = (TextField) textFieldsBox.getChildren().get(3);
            endpointField.setDisable(true);
            authComboBox.setDisable(true);
            authField1.setDisable(true);
            authField2.setDisable(true);
            isModified = false;
            loadedFile = null;
            updateButtonStates(table);
        });
        createNewTestButton.setOnMouseEntered(e -> createNewTestButton.setStyle(BUTTON_HOVER_STYLE));
        createNewTestButton.setOnMouseExited(e -> createNewTestButton.setStyle(BUTTON_STYLE));

        Button addEditEnvVarButton = new Button("Add/Edit Env Var");
        addEditEnvVarButton.setStyle(BUTTON_STYLE);
        addEditEnvVarButton.setTooltip(new Tooltip("Add or edit environment variables"));
        addEditEnvVarButton.setOnAction(e -> {
            try {
                Stage envVarStage = new Stage();
                EnvVarList simpleTableWindow = new EnvVarList();
                simpleTableWindow.start(envVarStage);
            } catch (Exception ex) {
                showError("Failed to open environment variables window: " + ex.getMessage());
            }
        });
        addEditEnvVarButton.setOnMouseEntered(e -> addEditEnvVarButton.setStyle(BUTTON_HOVER_STYLE));
        addEditEnvVarButton.setOnMouseExited(e -> addEditEnvVarButton.setStyle(BUTTON_STYLE));

        buttonsVBox.getChildren().addAll(
            addStepButton, addAboveButton, addBelowButton, moveUpButton, moveDownButton,
            deleteStepButton, deleteTestCaseButton, saveTestButton, loadTestButton,
            createNewTestButton, addEditEnvVarButton
        );

        return buttonsVBox;
    }

    private void updateButtonStates(TableView<String[]> table) {
        if (mainLayout == null) return;

        boolean isTableEmpty = table.getItems().isEmpty();
        int selectedIndex = table.getSelectionModel().getSelectedIndex();
        int lastIndex = table.getItems().size() - 1;

        addAboveButton.setDisable(isTableEmpty);
        addBelowButton.setDisable(isTableEmpty);
        moveUpButton.setDisable(isTableEmpty || selectedIndex <= 0);
        moveDownButton.setDisable(isTableEmpty || selectedIndex < 0 || selectedIndex >= lastIndex);
        deleteStepButton.setDisable(isTableEmpty);
        deleteTestCaseButton.setDisable(isTableEmpty);
        saveTestButton.setDisable(isTableEmpty);
        createNewTestButton.setDisable(isTableEmpty);
    }

    private HBox createTextFieldsBox(TableView<String[]> table, GridPane additionalFields) {
        TextField endpointField = new TextField();
        endpointField.setPromptText("End-Point");
        endpointField.setStyle(FIELD_STYLE_UNFOCUSED);
        endpointField.setPrefHeight(TEXT_FIELD_HEIGHT);
        endpointField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            endpointField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });
        endpointField.setDisable(true);
        endpointField.prefWidthProperty().bind(additionalFields.widthProperty().multiply(0.5346));
        endpointField.maxWidthProperty().bind(endpointField.prefWidthProperty());
        endpointField.minWidthProperty().bind(endpointField.prefWidthProperty());

        endpointField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[ColumnIndex.END_POINT.getIndex()] = newVal;
                isModified = true;
                table.refresh();
            }
        });

        ComboBox<String> authComboBox = new ComboBox<>(AUTH_OPTIONS);
        authComboBox.setPromptText("Authorization");
        authComboBox.setStyle(FIELD_STYLE_UNFOCUSED);
        authComboBox.setPrefHeight(TEXT_FIELD_HEIGHT);
        authComboBox.setDisable(true);
        authComboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            authComboBox.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });
        authComboBox.prefWidthProperty().bind(additionalFields.widthProperty().multiply(0.15));
        authComboBox.maxWidthProperty().bind(authComboBox.prefWidthProperty());
        authComboBox.minWidthProperty().bind(authComboBox.prefWidthProperty());

        TextField authField1 = new TextField();
        authField1.setPromptText("Username");
        authField1.setStyle(FIELD_STYLE_UNFOCUSED);
        authField1.setPrefHeight(TEXT_FIELD_HEIGHT);
        authField1.setDisable(true);
        authField1.focusedProperty().addListener((obs, oldVal, newVal) -> {
            authField1.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });
        authField1.prefWidthProperty().bind(additionalFields.widthProperty().multiply(0.15));
        authField1.maxWidthProperty().bind(authField1.prefWidthProperty());
        authField1.minWidthProperty().bind(authField1.prefWidthProperty());

        TextField authField2 = new TextField();
        authField2.setPromptText("Password");
        authField2.setStyle(FIELD_STYLE_UNFOCUSED);
        authField2.setPrefHeight(TEXT_FIELD_HEIGHT);
        authField2.setDisable(true);
        authField2.focusedProperty().addListener((obs, oldVal, newVal) -> {
            authField2.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });
        authField2.prefWidthProperty().bind(additionalFields.widthProperty().multiply(0.15));
        authField2.maxWidthProperty().bind(authField2.prefWidthProperty());
        authField2.minWidthProperty().bind(authField2.prefWidthProperty());

        authComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                if ("Basic Auth".equals(newVal)) {
                    authField1.setPromptText("Username");
                    authField2.setPromptText("Password");
                    authField2.setDisable(false);
                } else if ("Bearer Token".equals(newVal)) {
                    authField1.setPromptText("Token");
                    authField2.setPromptText("");
                    authField2.setText("");
                    authField2.setDisable(true);
                } else {
                    authField1.setPromptText("Username");
                    authField2.setPromptText("Password");
                    authField1.setText("");
                    authField2.setText("");
                    authField2.setDisable(false);
                }
                String authData = "";
                if ("Basic Auth".equals(newVal)) {
                    authData = "Basic:" + authField1.getText() + ":" + authField2.getText();
                } else if ("Bearer Token".equals(newVal)) {
                    authData = "Bearer:" + authField1.getText();
                }
                table.getItems().get(selectedIndex)[ColumnIndex.AUTHORIZATION.getIndex()] = authData;
                isModified = true;
                table.refresh();
            }
        });

        authField1.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                String authType = authComboBox.getValue();
                String authData = "";
                if ("Basic Auth".equals(authType)) {
                    authData = "Basic:" + newVal + ":" + authField2.getText();
                } else if ("Bearer Token".equals(authType)) {
                    authData = "Bearer:" + newVal;
                }
                table.getItems().get(selectedIndex)[ColumnIndex.AUTHORIZATION.getIndex()] = authData;
                isModified = true;
                table.refresh();
            }
        });

        authField2.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && "Basic Auth".equals(authComboBox.getValue())) {
                String authData = "Basic:" + authField1.getText() + ":" + newVal;
                table.getItems().get(selectedIndex)[ColumnIndex.AUTHORIZATION.getIndex()] = authData;
                isModified = true;
                table.refresh();
            }
        });

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #FF5555;");
        statusLabel.setWrapText(true);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        HBox textFieldsBox = new HBox(10, endpointField, authComboBox, authField1, authField2, statusLabel);
        textFieldsBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
        HBox.setHgrow(endpointField, Priority.NEVER);
        HBox.setHgrow(authComboBox, Priority.NEVER);
        HBox.setHgrow(authField1, Priority.NEVER);
        HBox.setHgrow(authField2, Priority.NEVER);
        return textFieldsBox;
    }

    private String convertRowToJson(String[] row) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (int i = 0; i < row.length; i++) {
            if (row[i] != null && !row[i].isEmpty() && !COLUMN_NAMES[i].isEmpty()) {
                if (!first) {
                    json.append(", ");
                }
                json.append(String.format("\"%s\": \"%s\"", COLUMN_NAMES[i], row[i].replace("\"", "\\\"")));
                first = false;
            }
        }
        json.append("}");
        return json.toString();
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    private class CustomComboBoxTableCell extends TableCell<String[], String> {
        private final ComboBox<String> comboBox;
        private final TableView<String[]> table;
        private final int columnIndex;

        public CustomComboBoxTableCell(TableView<String[]> table, int columnIndex, ObservableList<String> items) {
            this.table = table;
            this.columnIndex = columnIndex;
            this.comboBox = new ComboBox<>(items);
            comboBox.setStyle(FIELD_STYLE_UNFOCUSED);
            comboBox.setPrefHeight(26);
            comboBox.setMaxHeight(26);
            comboBox.setMinHeight(26);
            comboBox.setEditable(false);

            comboBox.setCellFactory(listView -> new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: #2E2E2E; -fx-text-fill: white;");
                    } else {
                        setText(item);
                        setStyle("-fx-background-color: #2E2E2E; -fx-text-fill: white;");
                        if (comboBox.getValue() != null && comboBox.getValue().equals(item)) {
                            setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white;");
                        }
                    }
                }
            });

            comboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (isEditing() && newValue != null) {
                    commitEdit(newValue);
                }
            });

            comboBox.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                    commitEdit(comboBox.getValue());
                    event.consume();
                } else if (event.getCode() == KeyCode.TAB) {
                    commitEdit(comboBox.getValue());
                    int rowIndex = getTableRow().getIndex();
                    int nextColumnIndex = (columnIndex + 1) % table.getColumns().size();
                    table.getFocusModel().focus(rowIndex, table.getColumns().get(nextColumnIndex));
                    table.getSelectionModel().select(rowIndex, table.getColumns().get(nextColumnIndex));
                    if (table.getColumns().get(nextColumnIndex).isEditable()) {
                        table.edit(rowIndex, table.getColumns().get(nextColumnIndex));
                    }
                    event.consume();
                } else if (event.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                    event.consume();
                }
            });
        }

        @Override
        public void startEdit() {
            if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
                return;
            }
            super.startEdit();
            comboBox.setValue(getItem());
            setText(null);
            setGraphic(comboBox);
            comboBox.requestFocus();
            if (!comboBox.isShowing()) {
                comboBox.show();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        public void commitEdit(String newValue) {
            super.commitEdit(newValue);
            setText(newValue);
            setGraphic(null);
            table.getItems().get(getTableRow().getIndex())[columnIndex] = newValue;
            isModified = true;
            table.refresh();
            int rowIndex = getTableRow().getIndex();
            TablePosition<String[], ?> pos = new TablePosition<>(table, rowIndex, getTableColumn());
            table.getFocusModel().focus(pos);
            table.getSelectionModel().select(rowIndex, getTableColumn());
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    comboBox.setValue(item);
                    setGraphic(comboBox);
                    setText(null);
                } else {
                    setText(item != null ? item : "");
                    setGraphic(null);
                }
            }
        }
    }
    
    private class CustomTextFieldTableCell extends TextFieldTableCell<String[], String> {
        private final TableView<String[]> table;
        private final int columnIndex;
        private final Label statusLabel;
        private final Set<String> testIds = new HashSet<>();
        private String originalValue;

        public CustomTextFieldTableCell(TableView<String[]> table, int columnIndex, Label statusLabel) {
            super(new StringConverter<String>() {
                @Override
                public String toString(String object) {
                    return object == null ? "" : object;
                }
                @Override
                public String fromString(String string) {
                    return string;
                }
            });
            this.table = table;
            this.columnIndex = columnIndex;
            this.statusLabel = statusLabel;

            table.getItems().addListener((javafx.collections.ListChangeListener<String[]>) c -> {
                updateTestIds();
            });
            updateTestIds();
        }

        private void updateTestIds() {
            testIds.clear();
            for (String[] row : table.getItems()) {
                String testId = row[0];
                if (testId != null && !testId.isEmpty()) {
                    testIds.add(testId);
                }
            }
        }

        private String suggestUniqueTestId(String input) {
            if (input == null || input.isEmpty()) {
                return generateUniqueId("1");
            }
            String base = input.replaceAll("[^0-9#]", "");
            if (base.isEmpty()) {
                base = "1";
            }
            return generateUniqueId(base);
        }

        private String generateUniqueId(String base) {
            String candidate = base;
            int suffix = 1;
            while (testIds.contains(candidate)) {
                candidate = base + suffix;
                suffix++;
                if (candidate.length() > 5) {
                    candidate = candidate.substring(0, 5);
                    if (testIds.contains(candidate)) {
                        candidate = String.valueOf(suffix);
                    }
                }
            }
            return candidate;
        }

        private void highlightDuplicateRow(String testId) {
            for (int index = 0; index < table.getItems().size(); index++) {
                String[] row = table.getItems().get(index);
                if (row[0] != null && row[0].equals(testId) && index != getTableRow().getIndex()) {
                    TableRow<String[]> tableRow = (TableRow<String[]>) table.lookup(".table-row-cell[index=" + index + "]");
                    if (tableRow != null) {
                        tableRow.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px;");
                    }
                }
            }
        }

        private void clearDuplicateHighlights() {
            table.getItems().forEach((row) -> {
                int index = table.getItems().indexOf(row);
                TableRow<String[]> tableRow = (TableRow<String[]>) table.lookup(".table-row-cell[index=" + index + "]");
                if (tableRow != null && index != table.getSelectionModel().getSelectedIndex()) {
                    tableRow.setStyle("-fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px;");
                }
            });
            statusLabel.setText("");
        }

        @Override
        public void startEdit() {
            if (table.getItems().isEmpty() || isEditing()) {
                return;
            }
            super.startEdit();
            originalValue = getItem();
            if (getGraphic() instanceof TextField) {
                TextField textField = (TextField) getGraphic();
                textField.setPrefHeight(26);
                textField.setMaxHeight(26);
                textField.setMinHeight(26);
                if (columnIndex == 0) {
                    UnaryOperator<TextFormatter.Change> filter = change -> {
                        String newText = change.getControlNewText();
                        if (newText.length() > 5) {
                            return null;
                        }
                        if (!newText.matches("^[0-9#]*$")) {
                            return null;
                        }
                        if (newText.startsWith("#") && newText.matches(".*[0-9].*")) {
                            return null;
                        }
                        if (newText.matches("^[0-9].*") && newText.contains("#")) {
                            return null;
                        }
                        if (!newText.isEmpty() && !newText.equals(originalValue) && testIds.contains(newText)) {
                            String suggestedId = suggestUniqueTestId(newText);
                            statusLabel.setText("Test ID '" + newText + "' already exists. Try: " + suggestedId);
                            highlightDuplicateRow(newText);
                        } else {
                            statusLabel.setText("");
                            clearDuplicateHighlights();
                        }
                        return change;
                    };
                    textField.setTextFormatter(new TextFormatter<>(filter));
                } else if (columnIndex == 17) {
                    UnaryOperator<TextFormatter.Change> filter = change -> {
                        String newText = change.getControlNewText();
                        if (newText.matches("\\d*")) {
                            return change;
                        }
                        return null;
                    };
                    textField.setTextFormatter(new TextFormatter<>(filter));
                }
                textField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.TAB) {
                        String text = textField.getText() != null ? textField.getText() : "";
                        if (columnIndex == 0) {
                            int rowIndex = getTableRow().getIndex();
                            if (!isValidTestId(text, testIds, originalValue, rowIndex, table)) {
                                @SuppressWarnings("unchecked")
                                TableColumn<String[], String> column = (TableColumn<String[], String>) table.getColumns().get(columnIndex);
                                showError("Cannot commit duplicate/invalid Test ID: " + text);
                                cancelEdit();
                                setText(getItem() != null ? getItem() : "");
                                setGraphic(null);
                                table.refresh();
                                Platform.runLater(() -> {
                                    int newColumn = (columnIndex + 1) % table.getColumns().size();
                                    table.getFocusModel().focus(rowIndex, table.getColumns().get(newColumn));
                                    table.getSelectionModel().select(rowIndex);
                                    table.edit(rowIndex, table.getColumns().get(newColumn));
                                    if (newColumn == 1) {
                                        TableCell<String[], String> nextCell = (TableCell<String[], String>) table.lookup(".table-cell[column=" + newColumn + "][index=" + rowIndex + "]");
                                        if (nextCell instanceof CustomComboBoxTableCell && nextCell.getGraphic() instanceof ComboBox) {
                                            @SuppressWarnings("unchecked")
                                            ComboBox<String> comboBox = (ComboBox<String>) nextCell.getGraphic();
                                            comboBox.show();
                                            comboBox.requestFocus();
                                        }
                                    }
                                    table.scrollTo(rowIndex);
                                    table.scrollToColumn(table.getColumns().get(newColumn));
                                });
                                e.consume();
                                return;
                            }
                        }
                        commitEdit(getConverter().fromString(text));
                        TablePosition<String[], ?> pos = table.getFocusModel().getFocusedCell();
                        int newColumn = (pos.getColumn() + 1) % table.getColumns().size();
                        int newRow = pos.getRow();
                        table.getFocusModel().focus(newRow, table.getColumns().get(newColumn));
                        table.edit(newRow, table.getColumns().get(newColumn));
                        if (newColumn == 1) {
                            Platform.runLater(() -> {
                                TableCell<String[], String> nextCell = (TableCell<String[], String>) table.lookup(".table-cell[column=" + newColumn + "][index=" + newRow + "]");
                                if (nextCell instanceof CustomComboBoxTableCell && nextCell.getGraphic() instanceof ComboBox) {
                                    @SuppressWarnings("unchecked")
                                    ComboBox<String> comboBox = (ComboBox<String>) nextCell.getGraphic();
                                    comboBox.show();
                                    comboBox.requestFocus();
                                }
                            });
                        }
                        table.scrollTo(newRow);
                        table.scrollToColumn(table.getColumns().get(newColumn));
                        e.consume();
                    } else if (e.getCode() == KeyCode.ENTER) {
                        String text = textField.getText() != null ? textField.getText() : "";
                        if (columnIndex == 0) {
                            int rowIndex = getTableRow().getIndex();
                            if (!isValidTestId(text, testIds, originalValue, rowIndex, table)) {
                                showError("Cannot commit duplicate/invalid Test ID: " + text);
                                return;
                            }
                        }
                        commitEdit(getConverter().fromString(text));
                        e.consume();
                    } else if (e.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                        e.consume();
                    }
                });
            }
        }

        @Override
        public void commitEdit(String newValue) {
            if (columnIndex == 0) {
                int rowIndex = getTableRow().getIndex();
                if (!isValidTestId(newValue, testIds, originalValue, rowIndex, table)) {
                    return;
                }
            }
            super.commitEdit(newValue);
            isModified = true;
            if (columnIndex == 0) {
                clearDuplicateHighlights();
                updateTestIds();
                int rowIndex = getTableRow().getIndex();
                String[] row = table.getItems().get(rowIndex);
                if (row[3] == null || row[3].isEmpty()) {
                    row[3] = "Content-Type";
                }
                if (row[4] == null || row[4].isEmpty()) {
                    row[4] = "application/json";
                }
                table.refresh();
                int currentIndex = getTableRow().getIndex();
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().select(currentIndex);
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() != null ? getItem() : "");
            setGraphic(null);
            if (columnIndex == 0) {
                clearDuplicateHighlights();
                if (getTableRow() != null && getTableRow().getItem() != null && originalValue != null) {
                    getTableRow().getItem()[0] = originalValue;
                    table.refresh();
                    if (getTableRow().getIndex() == table.getSelectionModel().getSelectedIndex()) {
                        HBox textFieldsBox = (HBox) mainLayout.getChildren().get(1);
                        TextField endpointField = (TextField) textFieldsBox.getChildren().get(0);
                        boolean isValid = isValidTestId(originalValue, testIds, originalValue, getTableRow().getIndex(), table);
                        endpointField.setDisable(!isValid);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}