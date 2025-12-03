package com.test.window.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * Manages the creation and configuration of UI components for the API test editor application.
 * This class handles the dynamic generation of text fields, combo boxes, buttons, and scrollable areas
 * for editing test steps, including headers, parameters, payloads, and response verifications.
 */
public class UIComponentsManager {
    /**
     * Enum defining the column indices in the test table for easy reference.
     */
    private enum ColumnIndex {
        TEST_ID(0), TEST_DESCRIPTION(1), REQUEST(2), END_POINT(3), HEADER_KEY(4), HEADER_VALUE(5),
        PARAM_KEY(6), PARAM_VALUE(7), PAYLOAD(8), PAYLOAD_TYPE(9),
        RESPONSE_KEY_NAME(10), CAPTURE_VALUE(11), AUTHORIZATION(12),
        AUTH_FIELD1(13), AUTH_FIELD2(14), SSL_VALIDATION(15), EXPECTED_STATUS(16),
        VERIFY_RESPONSE(17);
        private final int index;
        ColumnIndex(int index) { this.index = index; }
        public int getIndex() { return index; }
    }
    
    private static UIComponentsManager instance;
    private static final ObservableList<String> SSL_VALIDATION_OPTIONS =
            FXCollections.observableArrayList("");
    private ComboBox<String> sslValidationComboBox;
    
    /**
     * Observable list of authorization options for the auth combo box.
     */
    private static final ObservableList<String> AUTH_OPTIONS =
        FXCollections.observableArrayList("", "Basic Auth", "Bearer Token");

    /**
     * Observable list of HTTP methods for the request combo box.
     */
    private static final ObservableList<String> HTTP_METHODS =
        FXCollections.observableArrayList("", "GET", "POST", "PUT", "PATCH", "DELETE");

    /**
     * Observable list of payload types for the payload type combo box.
     */
    private static final ObservableList<String> PAYLOAD_TYPES =
        FXCollections.observableArrayList("", "JSON", "form-data", "urlencoded");

    /**
     * CSS style for unfocused text fields and combo boxes.
     */
    private static final String FIELD_STYLE_UNFOCUSED =
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #BBBBBB; -fx-border-radius: 5px; " +
        "-fx-padding: 7px 0px 7px 15px;";

    /**
     * CSS style for focused text fields and combo boxes.
     */
    private static final String FIELD_STYLE_FOCUSED =
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #4A90E2; -fx-border-width: 2px; -fx-prompt-text-fill: #BBBBBB; -fx-border-radius: 5px; " +
        "-fx-padding: 7px 0px 7px 15px;";

    /**
     * CSS style for disabled text fields and combo boxes.
     */
    private static final String FIELD_STYLE_DISABLED =
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: #888888; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #BBBBBB; -fx-border-radius: 5px; " +
        "-fx-padding: 7px 0px 7px 15px;";

    /**
     * CSS style for unfocused centered text fields.
     */
    private static final String FIELD_STYLE_UNFOCUSED_CENTERED =
        FIELD_STYLE_UNFOCUSED + " -fx-alignment: center;";

    /**
     * CSS style for focused centered text fields.
     */
    private static final String FIELD_STYLE_FOCUSED_CENTERED =
        FIELD_STYLE_FOCUSED + " -fx-alignment: center;";

    /**
     * CSS style for disabled centered text fields.
     */
    private static final String FIELD_STYLE_DISABLED_CENTERED =
        FIELD_STYLE_DISABLED + " -fx-alignment: center;";

    /**
     * CSS style for unfocused left-centered (vertical center, horizontal left) text fields.
     */
    private static final String FIELD_STYLE_UNFOCUSED_LEFT_CENTERED =
        FIELD_STYLE_UNFOCUSED + " -fx-alignment: center-left;";

    /**
     * CSS style for focused left-centered text fields.
     */
    private static final String FIELD_STYLE_FOCUSED_LEFT_CENTERED =
        FIELD_STYLE_FOCUSED + " -fx-alignment: center-left;";

    /**
     * CSS style for disabled left-centered text fields.
     */
    private static final String FIELD_STYLE_DISABLED_LEFT_CENTERED =
        FIELD_STYLE_DISABLED + " -fx-alignment: center-left;";

    /**
     * CSS style for buttons in their default state.
     */
    private static final String BUTTON_STYLE =
        "-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

    /**
     * CSS style for buttons on hover.
     */
    private static final String BUTTON_HOVER_STYLE =
        "-fx-background-color: #6AB0FF; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

    /**
     * Standard height for text fields.
     */
    private static final double TEXT_FIELD_HEIGHT = 35.0;

    /**
     * CSS for caret in styled text areas.
     */
    private static final String CARET_CSS = ".styled-text-area .caret { -fx-fill: white; -fx-stroke: white; } " +
        ".styled-text-area:focused .caret { -fx-fill: white; -fx-stroke: white; } " +
        ".styled-text-area:disabled .caret { -fx-fill: #888888; -fx-stroke: #888888; }";

    /**
     * Reference to the main table view displaying test steps.
     */
    private final TableView<String[]> table;

    /**
     * Label for displaying status messages.
     */
    private final Label statusLabel;

    /**
     * Array of column names for the table.
     */
    private final String[] columnNames;

    /**
     * Manager for table operations.
     */
    private final TableManager tableManager;

    /**
     * Reference to the main application instance.
     */
    private final CreateEditAPITest app;

    /**
     * Text area for editing payload content.
     */
    private InlineCssTextArea payloadField;

    /**
     * Text area for verifying response content.
     */
    private InlineCssTextArea verifyResponseField;

    /**
     * Text area for endpoint URL with highlighting support.
     */
    private InlineCssTextArea endpointField;

    /**
     * Scroll pane containing header fields.
     */
    private ScrollPane headerFieldsScroll;

    /**
     * Button for adding a step above the selected row.
     */
    private Button addAboveButton, addBelowButton, moveUpButton, moveDownButton,
                   deleteStepButton, deleteTestCaseButton, saveTestButton, createNewTestButton;

    /**
     * Static reference to the environment variables stage to ensure only one instance.
     */
    private static Stage envVarStage; // Track single EnvVarList window

    private final String endpointPrompt = "End-Point";
    private final String statusPrompt = "Status";
    private String usernamePrompt = "Username/Token";
    private final String passwordPrompt = "Password";

    /**
     * Flag to prevent table updates during placeholder setting.
     */
    private boolean settingPlaceholder = false;

    /**
     * Constructs a UIComponentsManager instance.
     *
     * @param table the table view for test steps
     * @param statusLabel label for status updates
     * @param columnNames array of column names
     * @param tableManager manager for table operations
     * @param app the main application instance
     */
    public UIComponentsManager(TableView<String[]> table, Label statusLabel, String[] columnNames, TableManager tableManager, CreateEditAPITest app) {
        this.table = table;
        // Disable sorting for all columns
        for (TableColumn<?, ?> column : table.getColumns()) {
            column.setSortable(false);
        }
        this.statusLabel = statusLabel;
        this.columnNames = columnNames;
        this.tableManager = tableManager;
        this.app = app;
        instance = this;
    }

    /**
     * Creates an HBox containing text fields and combo boxes for basic test step editing
     * (request method, endpoint, status, authorization).
     *
     * @return HBox containing the input fields
     */
    public HBox createTextFieldsBox() {
        // Create combo box for HTTP request methods
        ComboBox<String> requestComboBox = new ComboBox<>(HTTP_METHODS);
        requestComboBox.setPromptText("Request");

        // Add CSS for dark theme text visibility
        requestComboBox.getStylesheets().add("data:text/css," +
            ".combo-box .list-cell {" +
            "    -fx-text-fill: white;" +
            "}" +
            ".combo-box-popup .list-view .list-cell {" +
            "    -fx-text-fill: white;" +
            "    -fx-background-color: #2E2E2E;" +
            "}" +
            ".combo-box-popup .list-view .list-cell:selected {" +
            "    -fx-text-fill: white;" +
            "    -fx-background-color: #4A90E2;" +
            "}" +
            ".combo-box-popup .list-view .list-cell:focused {" +
            "    -fx-text-fill: white;" +
            "    -fx-background-color: #4A90E2;" +
            "}"
        );

        requestComboBox.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.isEmpty() ? "" : item);
                }
                setStyle("-fx-background-color: #2E2E2E; -fx-text-fill: white;");
            }
        });

        requestComboBox.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String object) {
                if (object == null || object.isEmpty()) {
                    return "";
                }
                return object;
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        });
        requestComboBox.setStyle(FIELD_STYLE_UNFOCUSED_CENTERED);
        requestComboBox.setPrefHeight(38.0);
        requestComboBox.setMinHeight(38.0);
        requestComboBox.setMaxHeight(38.0);
        requestComboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!requestComboBox.isDisable()) {
                requestComboBox.setStyle(newVal ? FIELD_STYLE_FOCUSED_CENTERED : FIELD_STYLE_UNFOCUSED_CENTERED);
            }
        });
        requestComboBox.setDisable(true);
        requestComboBox.prefWidthProperty().bind(table.widthProperty().multiply(0.08));
        requestComboBox.maxWidthProperty().bind(requestComboBox.prefWidthProperty());
        requestComboBox.minWidthProperty().bind(requestComboBox.prefWidthProperty());

        // Create text area for endpoint URL with highlighting support
        endpointField = new InlineCssTextArea();
        endpointField.setStyle(FIELD_STYLE_UNFOCUSED);
        endpointField.setPrefHeight(35.0);
        endpointField.setMinHeight(35.0);
        endpointField.setMaxHeight(35.0);
        endpointField.setWrapText(true);
        endpointField.setEditable(true);
        endpointField.replaceText(0, endpointField.getLength(), "");
        endpointField.getStylesheets().add("data:text/css," + CARET_CSS);
        updateFieldStyle(endpointField, endpointPrompt, "center-left");

        // Listener for endpoint field changes to update table
        endpointField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && endpointField.isEditable() && !settingPlaceholder) {
                table.getItems().get(selectedIndex)[ColumnIndex.END_POINT.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
            updateFieldStyle(endpointField, endpointPrompt, "center-left");
        });

        // Handle TAB key in endpoint field for focus traversal (no tab insertion)
        endpointField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                event.consume(); // Prevent tab character insertion
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0 && endpointField.isEditable()) {
                    String rawText = endpointField.getText();
                    table.getItems().get(selectedIndex)[ColumnIndex.END_POINT.getIndex()] = rawText;
                    table.refresh();
                    app.setModified(true);
                }
                moveFocus(endpointField, !event.isShiftDown()); // Move focus forward or backward
            }
        });

        // Focused listener for endpoint
        endpointField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                if (endpointField.getText().equals(endpointPrompt)) {
                    endpointField.replaceText(0, endpointField.getLength(), "");
                }
            } else {
                if (endpointField.isEditable() && endpointField.getText().isEmpty()) {
                    settingPlaceholder = true;
                    endpointField.replaceText(0, endpointField.getLength(), endpointPrompt);
                    settingPlaceholder = false;
                }
            }
            updateFieldStyle(endpointField, endpointPrompt, "center-left");
            endpointField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });
        endpointField.prefWidthProperty().bind(table.widthProperty().multiply(0.500));
        endpointField.maxWidthProperty().bind(endpointField.prefWidthProperty());
        endpointField.minWidthProperty().bind(endpointField.prefWidthProperty());

        // Create text area for expected status code with highlighting support
        InlineCssTextArea statusField = new InlineCssTextArea();
        statusField.setStyle(FIELD_STYLE_UNFOCUSED);
        statusField.setPrefHeight(TEXT_FIELD_HEIGHT);
        statusField.setMinHeight(TEXT_FIELD_HEIGHT);
        statusField.setMaxHeight(TEXT_FIELD_HEIGHT);
        statusField.setWrapText(false);
        statusField.setEditable(true);
        statusField.replaceText(0, statusField.getLength(), "");
        statusField.getStylesheets().add("data:text/css," + CARET_CSS);
        updateFieldStyle(statusField, statusPrompt, "center");

        // Listener for status field changes to update table
        statusField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && statusField.isEditable() && !settingPlaceholder) {
                table.getItems().get(selectedIndex)[ColumnIndex.EXPECTED_STATUS.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
            updateFieldStyle(statusField, statusPrompt, "center");
        });

        // Handle TAB key in status field for focus traversal (no tab insertion)
        statusField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                event.consume(); // Prevent tab character insertion
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0 && statusField.isEditable()) {
                    String rawText = statusField.getText();
                    table.getItems().get(selectedIndex)[ColumnIndex.EXPECTED_STATUS.getIndex()] = rawText;
                    table.refresh();
                    app.setModified(true);
                }
                moveFocus(statusField, !event.isShiftDown()); // Move focus forward or backward
            }
        });

        // Focused listener for status
        statusField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                if (statusField.getText().equals(statusPrompt)) {
                    statusField.replaceText(0, statusField.getLength(), "");
                }
            } else {
                if (statusField.isEditable() && statusField.getText().isEmpty()) {
                    settingPlaceholder = true;
                    statusField.replaceText(0, statusField.getLength(), statusPrompt);
                    settingPlaceholder = false;
                }
            }
            updateFieldStyle(statusField, statusPrompt, "center");
            statusField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });
        statusField.setEditable(false);
        statusField.prefWidthProperty().bind(table.widthProperty().multiply(0.05));
        statusField.maxWidthProperty().bind(statusField.prefWidthProperty());
        statusField.minWidthProperty().bind(statusField.prefWidthProperty());

        // Create combo box for authorization types
        ComboBox<String> authComboBox = new ComboBox<>(AUTH_OPTIONS);
        authComboBox.setPromptText("Authorization");

        // Add CSS for dark theme text visibility
        authComboBox.getStylesheets().add("data:text/css," +
            ".combo-box .list-cell {" +
            "    -fx-text-fill: white;" +
            "}" +
            ".combo-box-popup .list-view .list-cell {" +
            "    -fx-text-fill: white;" +
            "    -fx-background-color: #2E2E2E;" +
            "}" +
            ".combo-box-popup .list-view .list-cell:selected {" +
            "    -fx-text-fill: white;" +
            "    -fx-background-color: #4A90E2;" +
            "}" +
            ".combo-box-popup .list-view .list-cell:focused {" +
            "    -fx-text-fill: white;" +
            "    -fx-background-color: #4A90E2;" +
            "}"
        );

        authComboBox.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.isEmpty() ? "" : item);
                }
                setStyle("-fx-background-color: #2E2E2E; -fx-text-fill: white;");
            }
        });

        authComboBox.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String object) {
                if (object == null || object.isEmpty()) {
                    return "";
                }
                return object;
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        });
        authComboBox.setStyle(FIELD_STYLE_UNFOCUSED_CENTERED);
        authComboBox.setPrefHeight(38.0);
        authComboBox.setMinHeight(38.0);
        authComboBox.setMaxHeight(38.0);
        authComboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!authComboBox.isDisable()) {
                authComboBox.setStyle(newVal ? FIELD_STYLE_FOCUSED_CENTERED : FIELD_STYLE_UNFOCUSED_CENTERED);
            }
        });
        authComboBox.setDisable(true);
        authComboBox.prefWidthProperty().bind(table.widthProperty().multiply(0.0950));
        authComboBox.maxWidthProperty().bind(authComboBox.prefWidthProperty());
        authComboBox.minWidthProperty().bind(authComboBox.prefWidthProperty());

        // Create text area for username or token (depending on auth type)
        InlineCssTextArea usernameTokenField = new InlineCssTextArea();
        usernameTokenField.replaceText(0, usernameTokenField.getLength(), "");
        usernameTokenField.setStyle(FIELD_STYLE_UNFOCUSED);
        usernameTokenField.setPrefHeight(TEXT_FIELD_HEIGHT);
        usernameTokenField.setMinHeight(TEXT_FIELD_HEIGHT);
        usernameTokenField.setMaxHeight(TEXT_FIELD_HEIGHT);
        usernameTokenField.setWrapText(false);
        usernameTokenField.setEditable(true);
        usernameTokenField.getStylesheets().add("data:text/css," + CARET_CSS);
        updateFieldStyle(usernameTokenField, usernamePrompt, "center-left");

        // Listener for username/token field changes to update table
        usernameTokenField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && usernameTokenField.isEditable() && !settingPlaceholder) {
                table.getItems().get(selectedIndex)[ColumnIndex.AUTH_FIELD1.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
            updateFieldStyle(usernameTokenField, usernamePrompt, "center-left");
        });

        // Handle TAB key in username/token field for focus traversal (no tab insertion)
        usernameTokenField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                event.consume(); // Prevent tab character insertion
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0 && usernameTokenField.isEditable()) {
                    String rawText = usernameTokenField.getText();
                    table.getItems().get(selectedIndex)[ColumnIndex.AUTH_FIELD1.getIndex()] = rawText;
                    table.refresh();
                    app.setModified(true);
                }
                moveFocus(usernameTokenField, !event.isShiftDown()); // Move focus forward or backward
            }
        });

        // Focused listener for username/token
        usernameTokenField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                if (usernameTokenField.getText().equals(usernamePrompt)) {
                    usernameTokenField.replaceText(0, usernameTokenField.getLength(), "");
                }
            } else {
                if (usernameTokenField.isEditable() && usernameTokenField.getText().isEmpty()) {
                    settingPlaceholder = true;
                    usernameTokenField.replaceText(0, usernameTokenField.getLength(), usernamePrompt);
                    settingPlaceholder = false;
                }
            }
            updateFieldStyle(usernameTokenField, usernamePrompt, "center-left");
            usernameTokenField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });
        usernameTokenField.setEditable(false);
        usernameTokenField.setVisible(false);
        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());

        // Create text area for password (for Basic Auth)
        InlineCssTextArea passwordField = new InlineCssTextArea();
        passwordField.replaceText(0, passwordField.getLength(), "");
        passwordField.setStyle(FIELD_STYLE_UNFOCUSED);
        passwordField.setPrefHeight(TEXT_FIELD_HEIGHT);
        passwordField.setMinHeight(TEXT_FIELD_HEIGHT);
        passwordField.setMaxHeight(TEXT_FIELD_HEIGHT);
        passwordField.setWrapText(false);
        passwordField.setEditable(true);
        passwordField.getStylesheets().add("data:text/css," + CARET_CSS);
        updateFieldStyle(passwordField, passwordPrompt, "center-left");

        // Listener for password field changes to update table
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && passwordField.isEditable() && !settingPlaceholder) {
                table.getItems().get(selectedIndex)[ColumnIndex.AUTH_FIELD2.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
            updateFieldStyle(passwordField, passwordPrompt, "center-left");
        });

        // Handle TAB key in password field for focus traversal (no tab insertion)
        passwordField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                event.consume(); // Prevent tab character insertion
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0 && passwordField.isEditable()) {
                    String rawText = passwordField.getText();
                    table.getItems().get(selectedIndex)[ColumnIndex.AUTH_FIELD2.getIndex()] = rawText;
                    table.refresh();
                    app.setModified(true);
                }
                moveFocus(passwordField, !event.isShiftDown()); // Move focus forward or backward
            }
        });

        // Focused listener for password
        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                if (passwordField.getText().equals(passwordPrompt)) {
                    passwordField.replaceText(0, passwordField.getLength(), "");
                }
            } else {
                if (passwordField.isEditable() && passwordField.getText().isEmpty()) {
                    settingPlaceholder = true;
                    passwordField.replaceText(0, passwordField.getLength(), passwordPrompt);
                    settingPlaceholder = false;
                }
            }
            updateFieldStyle(passwordField, passwordPrompt, "center-left");
            passwordField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });
        passwordField.setEditable(false);
        passwordField.setVisible(false);
        passwordField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
        passwordField.maxWidthProperty().bind(passwordField.prefWidthProperty());
        passwordField.minWidthProperty().bind(passwordField.prefWidthProperty());

        // Create HBox to hold all fields with spacing
        HBox textFieldsBox = new HBox(10);
        textFieldsBox.setStyle("-fx-background-color: #2E2E2E;");
        textFieldsBox.getChildren().addAll(requestComboBox, endpointField, statusField, authComboBox, usernameTokenField, passwordField);
        HBox.setHgrow(requestComboBox, Priority.NEVER);
        HBox.setHgrow(endpointField, Priority.ALWAYS);
        HBox.setHgrow(statusField, Priority.NEVER);
        HBox.setHgrow(authComboBox, Priority.NEVER);
        HBox.setHgrow(usernameTokenField, Priority.NEVER);
        HBox.setHgrow(passwordField, Priority.NEVER);

        // Listener for table selection changes to populate and enable/disable fields based on valid Test ID
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                String[] row = table.getItems().get(selectedIndex);
                String testId = row[ColumnIndex.TEST_ID.getIndex()];
                Set<String> testIds = new HashSet<>();
                for (String[] tableRow : table.getItems()) {
                    if (tableRow[ColumnIndex.TEST_ID.getIndex()] != null && !tableRow[ColumnIndex.TEST_ID.getIndex()].isEmpty()) {
                        testIds.add(tableRow[ColumnIndex.TEST_ID.getIndex()]);
                    }
                }
                boolean isValidTestId = CreateEditAPITest.isValidTestId(testId, testIds, testId);
                // Set editable and disabled states first
                requestComboBox.setDisable(!isValidTestId);
                endpointField.setEditable(isValidTestId);
                statusField.setEditable(isValidTestId);
                authComboBox.setDisable(!isValidTestId);
                usernameTokenField.setEditable(isValidTestId);
                passwordField.setEditable(isValidTestId);
                // Now set values
                requestComboBox.setValue(row[ColumnIndex.REQUEST.getIndex()]);
                String endpointText = row[ColumnIndex.END_POINT.getIndex()] != null ? row[ColumnIndex.END_POINT.getIndex()] : "";
                endpointField.replaceText(0, endpointField.getLength(), endpointText);
                String statusText = row[ColumnIndex.EXPECTED_STATUS.getIndex()] != null ? row[ColumnIndex.EXPECTED_STATUS.getIndex()] : "";
                statusField.replaceText(0, statusField.getLength(), statusText);
                authComboBox.setValue(row[ColumnIndex.AUTHORIZATION.getIndex()]);
                String authField1Text = row[ColumnIndex.AUTH_FIELD1.getIndex()] != null ? row[ColumnIndex.AUTH_FIELD1.getIndex()] : "";
                usernameTokenField.replaceText(0, usernameTokenField.getLength(), authField1Text);
                String authField2Text = row[ColumnIndex.AUTH_FIELD2.getIndex()] != null ? row[ColumnIndex.AUTH_FIELD2.getIndex()] : "";
                passwordField.replaceText(0, passwordField.getLength(), authField2Text);
                if (!isValidTestId) {
                    requestComboBox.setStyle(FIELD_STYLE_DISABLED_CENTERED);
                    endpointField.setStyle(FIELD_STYLE_DISABLED);
                    statusField.setStyle(FIELD_STYLE_DISABLED);
                    authComboBox.setStyle(FIELD_STYLE_DISABLED_CENTERED);
                    requestComboBox.setPromptText("Request");
                    authComboBox.setPromptText("Authorization");
                    requestComboBox.setValue(null);
                    requestComboBox.getSelectionModel().clearSelection();
                    authComboBox.setValue(null);
                    authComboBox.getSelectionModel().clearSelection();
                } else {
                    requestComboBox.setStyle(requestComboBox.isFocused() ? FIELD_STYLE_FOCUSED_CENTERED : FIELD_STYLE_UNFOCUSED_CENTERED);
                    endpointField.setStyle(endpointField.isFocused() ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    statusField.setStyle(statusField.isFocused() ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    authComboBox.setStyle(authComboBox.isFocused() ? FIELD_STYLE_FOCUSED_CENTERED : FIELD_STYLE_UNFOCUSED_CENTERED);
                }
                updateFieldStyle(endpointField, endpointPrompt, "center-left");
                updateFieldStyle(statusField, statusPrompt, "center");
                updateFieldStyle(usernameTokenField, usernamePrompt, "center-left");
                updateFieldStyle(passwordField, passwordPrompt, "center-left");
                if (isValidTestId && authComboBox.getValue() != null) {
                    if ("Basic Auth".equals(authComboBox.getValue())) {
                        usernamePrompt = "Username";
                        usernameTokenField.setVisible(true);
                        usernameTokenField.setEditable(true);
                        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
                        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        passwordField.setVisible(true);
                        passwordField.setEditable(true);
                    } else if ("Bearer Token".equals(authComboBox.getValue())) {
                        usernamePrompt = "Token";
                        usernameTokenField.setVisible(true);
                        usernameTokenField.setEditable(true);
                        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.21));
                        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        passwordField.setVisible(false);
                        passwordField.setEditable(false);
                    } else {
                        usernamePrompt = "Username/Token";
                        usernameTokenField.setVisible(false);
                        usernameTokenField.setEditable(false);
                        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
                        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        passwordField.setVisible(false);
                        passwordField.setEditable(false);
                    }
                } else {
                    usernamePrompt = "Username/Token";
                    usernameTokenField.setVisible(false);
                    usernameTokenField.setEditable(false);
                    usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
                    usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                    usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                    passwordField.setVisible(false);
                    passwordField.setEditable(false);
                }
                updateFieldStyle(usernameTokenField, usernamePrompt, "center-left");
                updateFieldStyle(passwordField, passwordPrompt, "center-left");
            } else {
                // Clear fields when no selection
                requestComboBox.setValue(null);
                requestComboBox.getSelectionModel().clearSelection();
                requestComboBox.setStyle(FIELD_STYLE_DISABLED_CENTERED);
                requestComboBox.setPromptText("Request");
                endpointField.replaceText(0, endpointField.getLength(), "");
                statusField.replaceText(0, statusField.getLength(), "");
                authComboBox.setValue(null);
                authComboBox.getSelectionModel().clearSelection();
                authComboBox.setStyle(FIELD_STYLE_DISABLED_CENTERED);
                authComboBox.setPromptText("Authorization");
                usernameTokenField.replaceText(0, usernameTokenField.getLength(), "");
                usernameTokenField.setVisible(false);
                usernameTokenField.setEditable(false);
                passwordField.replaceText(0, passwordField.getLength(), "");
                passwordField.setVisible(false);
                passwordField.setEditable(false);
                requestComboBox.setDisable(true);
                endpointField.setEditable(false);
                endpointField.setStyle(FIELD_STYLE_DISABLED);
                statusField.setEditable(false);
                statusField.setStyle(FIELD_STYLE_DISABLED);
                authComboBox.setDisable(true);
                updateFieldStyle(endpointField, endpointPrompt, "center-left");
                updateFieldStyle(statusField, statusPrompt, "center");
                updateFieldStyle(usernameTokenField, usernamePrompt, "center-left");
                updateFieldStyle(passwordField, passwordPrompt, "center-left");
            }
            updateButtonStates();
        });

        // Listener for auth combo box changes to update auth fields visibility and update table
        authComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                String[] row = table.getItems().get(selectedIndex);
                String testId = row[ColumnIndex.TEST_ID.getIndex()];
                Set<String> testIds = new HashSet<>();
                for (String[] tableRow : table.getItems()) {
                    if (tableRow[ColumnIndex.TEST_ID.getIndex()] != null && !tableRow[ColumnIndex.TEST_ID.getIndex()].isEmpty()) {
                        testIds.add(tableRow[ColumnIndex.TEST_ID.getIndex()]);
                    }
                }
                boolean isValidTestId = CreateEditAPITest.isValidTestId(testId, testIds, testId);
                table.getItems().get(selectedIndex)[ColumnIndex.AUTHORIZATION.getIndex()] = newVal;
                table.refresh();
                tableManager.updateAuthFieldHeaders(selectedIndex);
                app.setModified(true);
                if (isValidTestId) {
                    if ("Basic Auth".equals(newVal)) {
                        usernamePrompt = "Username";
                        usernameTokenField.setVisible(true);
                        usernameTokenField.setEditable(true);
                        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
                        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        passwordField.setVisible(true);
                        passwordField.setEditable(true);
                        usernameTokenField.replaceText(0, usernameTokenField.getLength(), ""); // Clear previous content
                    } else if ("Bearer Token".equals(newVal)) {
                        usernamePrompt = "Token";
                        usernameTokenField.setVisible(true);
                        usernameTokenField.setEditable(true);
                        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.21));
                        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        passwordField.setVisible(false);
                        passwordField.setEditable(false);
                        usernameTokenField.replaceText(0, usernameTokenField.getLength(), ""); // Clear previous content
                    } else {
                        usernamePrompt = "Username/Token";
                        usernameTokenField.setVisible(false);
                        usernameTokenField.setEditable(false);
                        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
                        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        passwordField.setVisible(false);
                        passwordField.setEditable(false);
                        usernameTokenField.replaceText(0, usernameTokenField.getLength(), ""); // Clear previous content
                    }
                }
                updateFieldStyle(usernameTokenField, usernamePrompt, "center-left");
                updateFieldStyle(passwordField, passwordPrompt, "center-left");
            }
        });

        // Listener for request combo box changes to update table
        requestComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[ColumnIndex.REQUEST.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });
        return textFieldsBox;
    }

    /**
     * Creates a VBox containing additional content areas for headers, parameters, payload editing,
     * response captures, and response verification.
     *
     * @return VBox containing the additional UI components
     */
    public VBox createAdditionalContent() {
        VBox additionalContent = new VBox(10);
        additionalContent.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
        additionalContent.setAlignment(Pos.CENTER_LEFT);

        // ========================= SSL VALIDATION — BELOW THE TOP BAR =========================
        sslValidationComboBox = new ComboBox<>(SSL_VALIDATION_OPTIONS);
        sslValidationComboBox.setPromptText("SSL");

        sslValidationComboBox.getStylesheets().add("data:text/css," +
            ".combo-box .list-cell { -fx-text-fill: white; }" +
            ".combo-box-popup .list-view .list-cell { -fx-text-fill: white; -fx-background-color: #2E2E2E; }" +
            ".combo-box-popup .list-view .list-cell:selected { -fx-text-fill: white; -fx-background-color: #4A90E2; }" +
            ".combo-box-popup .list-view .list-cell:focused { -fx-text-fill: white; -fx-background-color: #4A90E2; }"
        );

        sslValidationComboBox.setCellFactory(lv -> new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.isEmpty() ? "" : item));
                setStyle("-fx-background-color: #2E2E2E; -fx-text-fill: white;");
            }
        });

        sslValidationComboBox.setConverter(new StringConverter<String>() {
            @Override public String toString(String o) { return o == null || o.isEmpty() ? "" : o; }
            @Override public String fromString(String s) { return s; }
        });

        sslValidationComboBox.setStyle(FIELD_STYLE_UNFOCUSED_CENTERED);
        sslValidationComboBox.setPrefHeight(25.0);
        sslValidationComboBox.focusedProperty().addListener((obs, o, n) ->
            sslValidationComboBox.setStyle(n ? FIELD_STYLE_FOCUSED_CENTERED : FIELD_STYLE_UNFOCUSED_CENTERED));

        sslValidationComboBox.setDisable(true);
        sslValidationComboBox.setPrefWidth(120);
        sslValidationComboBox.setMaxWidth(140);
        
        // Load profiles from ssl.json
        loadSslProfilesIntoComboBox();
        
        sslValidationComboBox.valueProperty().addListener((obs, old, newVal) -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                table.getItems().get(idx)[ColumnIndex.SSL_VALIDATION.getIndex()] = newVal != null ? newVal : "";
                table.refresh();
                app.setModified(true);
            }
        });

        HBox sslRow = new HBox(sslValidationComboBox);
        sslRow.setAlignment(Pos.CENTER_LEFT);
        sslRow.setPadding(new javafx.geometry.Insets(0, 0, 0, 0)); // aligns with End-Point
        sslRow.setStyle("-fx-background-color: #2E2E2E;");

        additionalContent.getChildren().add(0, sslRow); // Right under the top bar
        // ===================================================================================

        // Add "Headers" heading above headerFieldsScroll
        Label headersLabel = new Label("Headers");
        headersLabel.setStyle("-fx-text-fill: #4A90E2; -fx-font-size: 14px; -fx-padding: 5px 0px 5px 5px;");

        VBox headerFieldsVBox = new VBox(5);
        headerFieldsVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
        headerFieldsScroll = new ScrollPane(headerFieldsVBox);
        headerFieldsScroll.setStyle("-fx-background-color: #2E2E2E; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
        headerFieldsScroll.setFitToWidth(true);
        headerFieldsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        headerFieldsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        headerFieldsScroll.setPrefHeight(150);
        headerFieldsScroll.setMaxHeight(150);
        headerFieldsScroll.setMinHeight(150);

        VBox headersSection = new VBox(5, headersLabel, headerFieldsScroll);
        headersSection.setAlignment(Pos.TOP_LEFT);

        // Add "Params" heading above paramScroll
        Label paramsLabel = new Label("Params");
        paramsLabel.setStyle("-fx-text-fill: #4A90E2; -fx-font-size: 14px; -fx-padding: 5px 0px 5px 5px;");

        VBox paramFieldsVBox = new VBox(5);
        paramFieldsVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
        ScrollPane paramScroll = new ScrollPane(paramFieldsVBox);
        paramScroll.setStyle("-fx-background-color: #2E2E2E; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
        paramScroll.setFitToWidth(true);
        paramScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        paramScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        paramScroll.setPrefHeight(150);
        paramScroll.setMaxHeight(150);
        paramScroll.setMinHeight(150);

        VBox paramsSection = new VBox(5, paramsLabel, paramScroll);
        paramsSection.setAlignment(Pos.TOP_LEFT);

        HBox headerParamBox = new HBox(10, headersSection, paramsSection);
        headerParamBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(headersSection, Priority.ALWAYS);
        HBox.setHgrow(paramsSection, Priority.ALWAYS);

        // Capture Response Data
        Label responseCaptureLabel = new Label("Capture Response Data");
        responseCaptureLabel.setStyle("-fx-text-fill: #4A90E2; -fx-font-size: 14px; -fx-padding: 5px 0px 5px 5px;");

        VBox responseCaptureVBox = new VBox(5);
        responseCaptureVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
        ScrollPane responseCaptureScroll = new ScrollPane(responseCaptureVBox);
        responseCaptureScroll.setStyle("-fx-background-color: #2E2E2E; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
        responseCaptureScroll.setFitToWidth(true);
        responseCaptureScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        responseCaptureScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        responseCaptureScroll.setPrefHeight(150);

        responseCaptureScroll.prefWidthProperty().bind(headerFieldsScroll.widthProperty());
        responseCaptureScroll.maxWidthProperty().bind(headerFieldsScroll.maxWidthProperty());
        responseCaptureScroll.minWidthProperty().bind(headerFieldsScroll.minWidthProperty());

        VBox captureSection = new VBox(5, responseCaptureLabel, responseCaptureScroll);
        captureSection.setAlignment(Pos.TOP_LEFT);
        captureSection.prefWidthProperty().bind(headersSection.widthProperty());
        captureSection.maxWidthProperty().bind(headersSection.maxWidthProperty());
        captureSection.minWidthProperty().bind(headersSection.minWidthProperty());

        HBox captureRow = new HBox(10, captureSection, new Region());
        HBox.setHgrow(new Region(), Priority.ALWAYS);

        VBox leftVBox = new VBox(10, headerParamBox, captureRow);
        leftVBox.setAlignment(Pos.TOP_LEFT);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        leftVBox.getChildren().add(spacer);

        // Payload Type ComboBox
        ComboBox<String> payloadTypeComboBox = new ComboBox<>(PAYLOAD_TYPES);
        payloadTypeComboBox.setPromptText("Payload Type");
        payloadTypeComboBox.getStylesheets().add("data:text/css," +
            ".combo-box .list-cell { -fx-text-fill: white; }" +
            ".combo-box-popup .list-view .list-cell { -fx-text-fill: white; -fx-background-color: #2E2E2E; }" +
            ".combo-box-popup .list-view .list-cell:selected { -fx-text-fill: white; -fx-background-color: #4A90E2; }"
        );
        payloadTypeComboBox.setCellFactory(lv -> new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-background-color: #2E2E2E; -fx-text-fill: white;");
            }
        });
        payloadTypeComboBox.setStyle(FIELD_STYLE_UNFOCUSED_CENTERED);
        payloadTypeComboBox.setPrefHeight(38.0);
        payloadTypeComboBox.focusedProperty().addListener((o, ov, nv) ->
            payloadTypeComboBox.setStyle(nv ? FIELD_STYLE_FOCUSED_CENTERED : FIELD_STYLE_UNFOCUSED_CENTERED));
        payloadTypeComboBox.setDisable(true);
        payloadTypeComboBox.prefWidthProperty().bind(table.widthProperty().multiply(0.10));

        payloadTypeComboBox.valueProperty().addListener((obs, old, newVal) -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                table.getItems().get(idx)[ColumnIndex.PAYLOAD_TYPE.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });

        // Payload & Verify Response
        payloadField = new InlineCssTextArea();
        payloadField.setStyle(FIELD_STYLE_UNFOCUSED);
        payloadField.setPrefHeight(310);
        payloadField.setWrapText(true);
        payloadField.setEditable(true);
        payloadField.getStylesheets().add("data:text/css," + CARET_CSS);
        payloadField.prefWidthProperty().bind(table.widthProperty().multiply(0.52));
        updatePayloadStyle();

        payloadField.textProperty().addListener((obs, old, val) -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && payloadField.isEditable()) {
                table.getItems().get(idx)[ColumnIndex.PAYLOAD.getIndex()] = val;
                table.refresh();
                app.setModified(true);
            }
            updatePayloadStyle();
        });

        payloadField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB) {
                e.consume();
                int idx = table.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && payloadField.isEditable()) {
                    table.getItems().get(idx)[ColumnIndex.PAYLOAD.getIndex()] = payloadField.getText();
                    table.refresh();
                    app.setModified(true);
                }
                moveFocus(payloadField, !e.isShiftDown());
            }
        });

        payloadField.focusedProperty().addListener((o, ov, nv) -> {
            updatePayloadStyle();
            payloadField.setStyle(nv ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });

        verifyResponseField = new InlineCssTextArea();
        verifyResponseField.setStyle(FIELD_STYLE_UNFOCUSED);
        verifyResponseField.setPrefHeight(310);
        verifyResponseField.setWrapText(true);
        verifyResponseField.setEditable(true);
        verifyResponseField.getStylesheets().add("data:text/css," + CARET_CSS);
        verifyResponseField.prefWidthProperty().bind(table.widthProperty().multiply(0.52));
        updateVerifyStyle();

        verifyResponseField.textProperty().addListener((obs, old, val) -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && verifyResponseField.isEditable()) {
                table.getItems().get(idx)[ColumnIndex.VERIFY_RESPONSE.getIndex()] = val;
                table.refresh();
                app.setModified(true);
            }
            updateVerifyStyle();
        });

        verifyResponseField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB) {
                e.consume();
                int idx = table.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && verifyResponseField.isEditable()) {
                    table.getItems().get(idx)[ColumnIndex.VERIFY_RESPONSE.getIndex()] = verifyResponseField.getText();
                    table.refresh();
                    app.setModified(true);
                }
                moveFocus(verifyResponseField, !e.isShiftDown());
            }
        });

        verifyResponseField.focusedProperty().addListener((o, ov, nv) -> {
            updateVerifyStyle();
            verifyResponseField.setStyle(nv ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });

        Label payloadLabel = new Label("Payload");
        payloadLabel.setStyle("-fx-text-fill: #4A90E2; -fx-font-size: 14px; -fx-padding: 5px 0px 5px 5px;");

        Label verifyResponseLabel = new Label("Verify Response");
        verifyResponseLabel.setStyle("-fx-text-fill: #4A90E2; -fx-font-size: 14px; -fx-padding: 5px 0px 5px 5px;");

        HBox payloadOptionsBox = new HBox(10, payloadTypeComboBox);
        payloadOptionsBox.setAlignment(Pos.CENTER_LEFT);

        VBox rightVBox = new VBox(10);
        rightVBox.setAlignment(Pos.TOP_LEFT);
        rightVBox.getChildren().addAll(payloadLabel, payloadOptionsBox, payloadField, verifyResponseLabel, verifyResponseField);
        VBox.setVgrow(payloadField, Priority.ALWAYS);
        VBox.setVgrow(verifyResponseField, Priority.ALWAYS);

        GridPane additionalFields = new GridPane();
        additionalFields.setHgap(10);
        additionalFields.setVgap(10);
        additionalFields.add(leftVBox, 0, 0);
        additionalFields.add(rightVBox, 1, 0);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(48);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(52);
        additionalFields.getColumnConstraints().addAll(col1, col2);

        additionalContent.getChildren().add(additionalFields);

        // ========================= FULL ORIGINAL TABLE SELECTION LISTENER (100% unchanged) =========================
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            headerFieldsVBox.getChildren().clear();
            paramFieldsVBox.getChildren().clear();
            responseCaptureVBox.getChildren().clear();

            if (newItem != null) {
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                String testId = newItem[ColumnIndex.TEST_ID.getIndex()];
                Set<String> testIds = new HashSet<>();
                for (String[] row : table.getItems()) {
                    if (row[ColumnIndex.TEST_ID.getIndex()] != null && !row[ColumnIndex.TEST_ID.getIndex()].isEmpty()) {
                        testIds.add(row[ColumnIndex.TEST_ID.getIndex()]);
                    }
                }
                boolean isValid = CreateEditAPITest.isValidTestId(testId, testIds, testId);

                if (!isValid) {
                    payloadField.replaceText(0, payloadField.getLength(), "");
                    payloadField.setEditable(false);
                    payloadField.setStyle(FIELD_STYLE_DISABLED);
                    verifyResponseField.replaceText(0, verifyResponseField.getLength(), "");
                    verifyResponseField.setEditable(false);
                    verifyResponseField.setStyle(FIELD_STYLE_DISABLED);
                    payloadTypeComboBox.setValue(null);
                    payloadTypeComboBox.setDisable(true);
                    payloadTypeComboBox.setStyle(FIELD_STYLE_DISABLED_CENTERED);
                    sslValidationComboBox.setValue(null);
                    sslValidationComboBox.setDisable(true);
                    sslValidationComboBox.setStyle(FIELD_STYLE_DISABLED_CENTERED);
                    return;
                }

                // Payload & Verify
                String payload = newItem[ColumnIndex.PAYLOAD.getIndex()] != null ? newItem[ColumnIndex.PAYLOAD.getIndex()] : "";
                String formattedPayload = CreateEditAPITest.formatJson(payload, statusLabel);
                payloadField.replaceText(0, payloadField.getLength(), formattedPayload);
                updatePayloadStyle();

                String verify = newItem[ColumnIndex.VERIFY_RESPONSE.getIndex()] != null ? newItem[ColumnIndex.VERIFY_RESPONSE.getIndex()] : "";
                String formattedVerify = CreateEditAPITest.formatJson(verify, statusLabel);
                verifyResponseField.replaceText(0, verifyResponseField.getLength(), formattedVerify);
                updateVerifyStyle();

                payloadField.setEditable(true);
                verifyResponseField.setEditable(true);
                payloadField.setStyle(payloadField.isFocused() ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                verifyResponseField.setStyle(verifyResponseField.isFocused() ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);

                payloadTypeComboBox.setValue(newItem[ColumnIndex.PAYLOAD_TYPE.getIndex()] != null ? newItem[ColumnIndex.PAYLOAD_TYPE.getIndex()] : "");
                payloadTypeComboBox.setDisable(false);
                payloadTypeComboBox.setStyle(payloadTypeComboBox.isFocused() ? FIELD_STYLE_FOCUSED_CENTERED : FIELD_STYLE_UNFOCUSED_CENTERED);

                String sslVal = newItem[ColumnIndex.SSL_VALIDATION.getIndex()];
                sslValidationComboBox.setValue(sslVal != null && !sslVal.isEmpty() ? sslVal : "");
                sslValidationComboBox.setDisable(false);
                sslValidationComboBox.setStyle(sslValidationComboBox.isFocused() ? FIELD_STYLE_FOCUSED_CENTERED : FIELD_STYLE_UNFOCUSED_CENTERED);

                // Find start of current test case block
                int start = selectedIndex;
                while (start >= 0 && (table.getItems().get(start)[ColumnIndex.TEST_ID.getIndex()] == null ||
                        table.getItems().get(start)[ColumnIndex.TEST_ID.getIndex()].isEmpty())) {
                    start--;
                }
                if (start < 0) start = 0;

                List<Integer> rowIndices = new ArrayList<>();
                for (int i = start; i < table.getItems().size(); i++) {
                    String[] r = table.getItems().get(i);
                    if (i > start && r[ColumnIndex.TEST_ID.getIndex()] != null &&
                            !r[ColumnIndex.TEST_ID.getIndex()].isEmpty()) break;
                    rowIndices.add(i);
                }

                // Dynamically create fields for each row in the test case
                for (Integer rowIndex : rowIndices) {
                    String[] row = table.getItems().get(rowIndex);

                    // Header key-value pair
                    String headerKeyPrompt = "Header Key";
                    InlineCssTextArea headerKeyField = new InlineCssTextArea();
                    String headerKeyInitial = row[ColumnIndex.HEADER_KEY.getIndex()] != null ? row[ColumnIndex.HEADER_KEY.getIndex()] : "";
                    headerKeyField.replaceText(0, headerKeyField.getLength(), headerKeyInitial);
                    headerKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                    headerKeyField.setPrefHeight(TEXT_FIELD_HEIGHT);
                    headerKeyField.setMinHeight(TEXT_FIELD_HEIGHT);
                    headerKeyField.setMaxHeight(TEXT_FIELD_HEIGHT);
                    headerKeyField.setWrapText(false);
                    headerKeyField.setEditable(true);
                    headerKeyField.getStylesheets().add("data:text/css," + CARET_CSS);
                    updateFieldStyle(headerKeyField, headerKeyPrompt, "center-left");
                    headerKeyField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        if (!settingPlaceholder) {
                            table.getItems().get(rowIndex)[ColumnIndex.HEADER_KEY.getIndex()] = newVal2;
                            table.refresh();
                            app.setModified(true);
                        }
                        updateFieldStyle(headerKeyField, headerKeyPrompt, "center-left");
                    });
                    headerKeyField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                        if (event.getCode() == KeyCode.TAB) {
                            event.consume();
                            table.getItems().get(rowIndex)[ColumnIndex.HEADER_KEY.getIndex()] = headerKeyField.getText();
                            table.refresh();
                            app.setModified(true);
                            moveFocus(headerKeyField, !event.isShiftDown());
                        }
                    });
                    headerKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        if (newVal2 && headerKeyField.getText().equals(headerKeyPrompt)) headerKeyField.replaceText("");
                        if (!newVal2 && headerKeyField.getText().isEmpty()) {
                            settingPlaceholder = true;
                            headerKeyField.replaceText(headerKeyPrompt);
                            settingPlaceholder = false;
                        }
                        updateFieldStyle(headerKeyField, headerKeyPrompt, "center-left");
                        headerKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED_LEFT_CENTERED : FIELD_STYLE_UNFOCUSED_LEFT_CENTERED);
                    });

                    String headerValuePrompt = "Header Value";
                    InlineCssTextArea headerValueField = new InlineCssTextArea();
                    String headerValueInitial = row[ColumnIndex.HEADER_VALUE.getIndex()] != null ? row[ColumnIndex.HEADER_VALUE.getIndex()] : "";
                    headerValueField.replaceText(0, headerValueField.getLength(), headerValueInitial);
                    headerValueField.setStyle(FIELD_STYLE_UNFOCUSED);
                    headerValueField.setPrefHeight(TEXT_FIELD_HEIGHT);
                    headerValueField.setWrapText(false);
                    headerValueField.setEditable(true);
                    headerValueField.getStylesheets().add("data:text/css," + CARET_CSS);
                    updateFieldStyle(headerValueField, headerValuePrompt, "center-left");
                    headerValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        if (!settingPlaceholder) {
                            table.getItems().get(rowIndex)[ColumnIndex.HEADER_VALUE.getIndex()] = newVal2;
                            table.refresh();
                            app.setModified(true);
                        }
                        updateFieldStyle(headerValueField, headerValuePrompt, "center-left");
                    });
                    headerValueField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                        if (event.getCode() == KeyCode.TAB) {
                            event.consume();
                            table.getItems().get(rowIndex)[ColumnIndex.HEADER_VALUE.getIndex()] = headerValueField.getText();
                            table.refresh();
                            app.setModified(true);
                            moveFocus(headerValueField, !event.isShiftDown());
                        }
                    });
                    headerValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        if (newVal2 && headerValueField.getText().equals(headerValuePrompt)) headerValueField.replaceText("");
                        if (!newVal2 && headerValueField.getText().isEmpty()) {
                            settingPlaceholder = true;
                            headerValueField.replaceText(headerValuePrompt);
                            settingPlaceholder = false;
                        }
                        updateFieldStyle(headerValueField, headerValuePrompt, "center-left");
                        headerValueField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED_LEFT_CENTERED : FIELD_STYLE_UNFOCUSED_LEFT_CENTERED);
                    });

                    GridPane headerPair = new GridPane();
                    headerPair.setHgap(5);
                    ColumnConstraints hc1 = new ColumnConstraints();
                    hc1.setPercentWidth(50);
                    ColumnConstraints hc2 = new ColumnConstraints();
                    hc2.setPercentWidth(50);
                    headerPair.getColumnConstraints().addAll(hc1, hc2);
                    headerPair.add(headerKeyField, 0, 0);
                    headerPair.add(headerValueField, 1, 0);
                    headerFieldsVBox.getChildren().add(headerPair);

                    // Param key-value pair
                    String paramKeyPrompt = "Parameter Key";
                    InlineCssTextArea paramKeyField = new InlineCssTextArea();
                    String paramKeyInitial = row[ColumnIndex.PARAM_KEY.getIndex()] != null ? row[ColumnIndex.PARAM_KEY.getIndex()] : "";
                    paramKeyField.replaceText(0, paramKeyField.getLength(), paramKeyInitial);
                    paramKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                    paramKeyField.setPrefHeight(TEXT_FIELD_HEIGHT);
                    paramKeyField.setWrapText(false);
                    paramKeyField.setEditable(true);
                    paramKeyField.getStylesheets().add("data:text/css," + CARET_CSS);
                    updateFieldStyle(paramKeyField, paramKeyPrompt, "center-left");
                    paramKeyField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        if (!settingPlaceholder) {
                            table.getItems().get(rowIndex)[ColumnIndex.PARAM_KEY.getIndex()] = newVal2;
                            table.refresh();
                            app.setModified(true);
                        }
                        updateFieldStyle(paramKeyField, paramKeyPrompt, "center-left");
                    });
                    paramKeyField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                        if (event.getCode() == KeyCode.TAB) {
                            event.consume();
                            table.getItems().get(rowIndex)[ColumnIndex.PARAM_KEY.getIndex()] = paramKeyField.getText();
                            table.refresh();
                            app.setModified(true);
                            moveFocus(paramKeyField, !event.isShiftDown());
                        }
                    });
                    paramKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        if (newVal2 && paramKeyField.getText().equals(paramKeyPrompt)) paramKeyField.replaceText("");
                        if (!newVal2 && paramKeyField.getText().isEmpty()) {
                            settingPlaceholder = true;
                            paramKeyField.replaceText(paramKeyPrompt);
                            settingPlaceholder = false;
                        }
                        updateFieldStyle(paramKeyField, paramKeyPrompt, "center-left");
                        paramKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED_LEFT_CENTERED : FIELD_STYLE_UNFOCUSED_LEFT_CENTERED);
                    });

                    String paramValuePrompt = "Parameter Value";
                    InlineCssTextArea paramValueField = new InlineCssTextArea();
                    String paramValueInitial = row[ColumnIndex.PARAM_VALUE.getIndex()] != null ? row[ColumnIndex.PARAM_VALUE.getIndex()] : "";
                    paramValueField.replaceText(0, paramValueField.getLength(), paramValueInitial);
                    paramValueField.setStyle(FIELD_STYLE_UNFOCUSED);
                    paramValueField.setPrefHeight(TEXT_FIELD_HEIGHT);
                    paramValueField.setWrapText(false);
                    paramValueField.setEditable(true);
                    paramValueField.getStylesheets().add("data:text/css," + CARET_CSS);
                    updateFieldStyle(paramValueField, paramValuePrompt, "center-left");
                    paramValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        if (!settingPlaceholder) {
                            table.getItems().get(rowIndex)[ColumnIndex.PARAM_VALUE.getIndex()] = newVal2;
                            table.refresh();
                            app.setModified(true);
                        }
                        updateFieldStyle(paramValueField, paramValuePrompt, "center-left");
                    });
                    paramValueField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                        if (event.getCode() == KeyCode.TAB) {
                            event.consume();
                            table.getItems().get(rowIndex)[ColumnIndex.PARAM_VALUE.getIndex()] = paramValueField.getText();
                            table.refresh();
                            app.setModified(true);
                            moveFocus(paramValueField, !event.isShiftDown());
                        }
                    });
                    paramValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        if (newVal2 && paramValueField.getText().equals(paramValuePrompt)) paramValueField.replaceText("");
                        if (!newVal2 && paramValueField.getText().isEmpty()) {
                            settingPlaceholder = true;
                            paramValueField.replaceText(paramValuePrompt);
                            settingPlaceholder = false;
                        }
                        updateFieldStyle(paramValueField, paramValuePrompt, "center-left");
                        paramValueField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED_LEFT_CENTERED : FIELD_STYLE_UNFOCUSED_LEFT_CENTERED);
                    });

                    GridPane paramPair = new GridPane();
                    paramPair.setHgap(5);
                    ColumnConstraints pc1 = new ColumnConstraints();
                    pc1.setPercentWidth(50);
                    ColumnConstraints pc2 = new ColumnConstraints();
                    pc2.setPercentWidth(50);
                    paramPair.getColumnConstraints().addAll(pc1, pc2);
                    paramPair.add(paramKeyField, 0, 0);
                    paramPair.add(paramValueField, 1, 0);
                    paramFieldsVBox.getChildren().add(paramPair);

                    // Response key-capture value pair
                    String responseKeyPrompt = "Response Key Name";
                    InlineCssTextArea responseKeyField = new InlineCssTextArea();
                    String responseKeyInitial = row[ColumnIndex.RESPONSE_KEY_NAME.getIndex()] != null ? row[ColumnIndex.RESPONSE_KEY_NAME.getIndex()] : "";
                    responseKeyField.replaceText(0, responseKeyField.getLength(), responseKeyInitial);
                    responseKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                    responseKeyField.setPrefHeight(TEXT_FIELD_HEIGHT);
                    responseKeyField.setWrapText(false);
                    responseKeyField.setEditable(true);
                    responseKeyField.getStylesheets().add("data:text/css," + CARET_CSS);
                    updateFieldStyle(responseKeyField, responseKeyPrompt, "center-left");
                    responseKeyField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        if (!settingPlaceholder) {
                            table.getItems().get(rowIndex)[ColumnIndex.RESPONSE_KEY_NAME.getIndex()] = newVal2;
                            table.refresh();
                            app.setModified(true);
                        }
                        updateFieldStyle(responseKeyField, responseKeyPrompt, "center-left");
                    });
                    responseKeyField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                        if (event.getCode() == KeyCode.TAB) {
                            event.consume();
                            table.getItems().get(rowIndex)[ColumnIndex.RESPONSE_KEY_NAME.getIndex()] = responseKeyField.getText();
                            table.refresh();
                            app.setModified(true);
                            moveFocus(responseKeyField, !event.isShiftDown());
                        }
                    });
                    responseKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        if (newVal2 && responseKeyField.getText().equals(responseKeyPrompt)) responseKeyField.replaceText("");
                        if (!newVal2 && responseKeyField.getText().isEmpty()) {
                            settingPlaceholder = true;
                            responseKeyField.replaceText(responseKeyPrompt);
                            settingPlaceholder = false;
                        }
                        updateFieldStyle(responseKeyField, responseKeyPrompt, "center-left");
                        responseKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED_LEFT_CENTERED : FIELD_STYLE_UNFOCUSED_LEFT_CENTERED);
                    });

                    String captureValuePrompt = "Capture Value (env var)";
                    InlineCssTextArea captureValueField = new InlineCssTextArea();
                    String captureValueInitial = row[ColumnIndex.CAPTURE_VALUE.getIndex()] != null ? row[ColumnIndex.CAPTURE_VALUE.getIndex()] : "";
                    captureValueField.replaceText(0, captureValueField.getLength(), captureValueInitial);
                    captureValueField.setStyle(FIELD_STYLE_UNFOCUSED);
                    captureValueField.setPrefHeight(TEXT_FIELD_HEIGHT);
                    captureValueField.setWrapText(false);
                    captureValueField.setEditable(true);
                    captureValueField.getStylesheets().add("data:text/css," + CARET_CSS);
                    updateFieldStyle(captureValueField, captureValuePrompt, "center-left");
                    captureValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        if (!settingPlaceholder) {
                            table.getItems().get(rowIndex)[ColumnIndex.CAPTURE_VALUE.getIndex()] = newVal2;
                            table.refresh();
                            app.setModified(true);
                        }
                        updateFieldStyle(captureValueField, captureValuePrompt, "center-left");
                    });
                    captureValueField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                        if (event.getCode() == KeyCode.TAB) {
                            event.consume();
                            table.getItems().get(rowIndex)[ColumnIndex.CAPTURE_VALUE.getIndex()] = captureValueField.getText();
                            table.refresh();
                            app.setModified(true);
                            moveFocus(captureValueField, !event.isShiftDown());
                        }
                    });
                    captureValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        if (newVal2 && captureValueField.getText().equals(captureValuePrompt)) captureValueField.replaceText("");
                        if (!newVal2 && captureValueField.getText().isEmpty()) {
                            settingPlaceholder = true;
                            captureValueField.replaceText(captureValuePrompt);
                            settingPlaceholder = false;
                        }
                        updateFieldStyle(captureValueField, captureValuePrompt, "center-left");
                        captureValueField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED_LEFT_CENTERED : FIELD_STYLE_UNFOCUSED_LEFT_CENTERED);
                    });

                    GridPane responsePair = new GridPane();
                    responsePair.setHgap(5);
                    ColumnConstraints rc1 = new ColumnConstraints();
                    rc1.setPercentWidth(50);
                    ColumnConstraints rc2 = new ColumnConstraints();
                    rc2.setPercentWidth(50);
                    responsePair.getColumnConstraints().addAll(rc1, rc2);
                    responsePair.add(responseKeyField, 0, 0);
                    responsePair.add(captureValueField, 1, 0);
                    responseCaptureVBox.getChildren().add(responsePair);
                }
            } else {
                payloadField.replaceText("");
                payloadField.setEditable(false);
                payloadField.setStyle(FIELD_STYLE_DISABLED);
                verifyResponseField.replaceText("");
                verifyResponseField.setEditable(false);
                verifyResponseField.setStyle(FIELD_STYLE_DISABLED);
                payloadTypeComboBox.setValue(null);
                payloadTypeComboBox.setDisable(true);
                sslValidationComboBox.setValue(null);
                sslValidationComboBox.setDisable(true);
                sslValidationComboBox.setStyle(FIELD_STYLE_DISABLED_CENTERED);
            }
        });

        return additionalContent;
    }
    
    void loadSslProfilesIntoComboBox() {
        ObservableList<String> sslItems = FXCollections.observableArrayList();

        // Always add empty option first
        sslItems.add("");

        File sslFile = new File("ssl.json");
        if (!sslFile.exists()) {
            System.out.println("ssl.json not found. Only empty option available.");
            sslValidationComboBox.setItems(sslItems);
            return;
        }

        try (FileReader reader = new FileReader(sslFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            // Sort keys alphabetically (optional, looks nicer)
            List<String> keys = new ArrayList<>();
            for (String key : json.keySet()) {
                keys.add(key);
            }
            Collections.sort(keys);

            sslItems.addAll(keys);

            System.out.println("Loaded SSL profiles: " + keys);
        } catch (Exception e) {
            System.err.println("Failed to read ssl.json: " + e.getMessage());
            e.printStackTrace();
        }

        sslValidationComboBox.setItems(sslItems);
    }
    
    private void updateFieldStyle(InlineCssTextArea field, String prompt, String alignment) {
        if (!field.isEditable()) {
            field.setStyleSpans(0, new StyleSpansBuilder<String>().add("-fx-fill: #888888;", field.getLength()).create());
            field.setParagraphStyle(0, "-fx-alignment: " + alignment + ";");
            return;
        }
        String text = field.getText();
        boolean isEmpty = text.isEmpty();
        boolean showPlaceholder = isEmpty && !field.isFocused();
        if (showPlaceholder) {
            settingPlaceholder = true;
            field.replaceText(0, field.getLength(), prompt);
            settingPlaceholder = false;
            field.setStyleSpans(0, computePlaceholderStyle(prompt));
        } else if (text.equals(prompt) && !field.isFocused()) {
            field.setStyleSpans(0, computePlaceholderStyle(prompt));
        } else {
            field.setStyleSpans(0, computeHighlighting(text));
        }
        field.setParagraphStyle(0, "-fx-alignment: " + alignment + ";");
    }

    private void updatePayloadStyle() {
        String text = payloadField.getText();
        payloadField.setStyleSpans(0, computeHighlighting(text));
        payloadField.setParagraphStyle(0, "-fx-alignment: top-left;");
    }

    private void updateVerifyStyle() {
        String text = verifyResponseField.getText();
        verifyResponseField.setStyleSpans(0, computeHighlighting(text));
        verifyResponseField.setParagraphStyle(0, "-fx-alignment: top-left;");
    }

    /**
     * Simple highlighter for {{any-text}} in pink (used by InlineCssTextArea).
     * Uses inline CSS styles; default is empty string to inherit white from control.
     */
    private StyleSpans<String> computeHighlighting(String text) {
        StyleSpansBuilder<String> spansBuilder = new StyleSpansBuilder<>();
        Pattern pattern = Pattern.compile("\\{\\{[^}]+\\}\\}"); // Matches {{any-text}}
        Matcher matcher = pattern.matcher(text);
        int lastEnd = 0;
        while (matcher.find()) {
            // Plain text before match (white)
            spansBuilder.add("-fx-fill: white;", matcher.start() - lastEnd);
            // Highlighted match
            spansBuilder.add("-fx-fill: pink;", matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        // Remaining plain text (white)
        spansBuilder.add("-fx-fill: white;", text.length() - lastEnd);
        return spansBuilder.create();
    }

    private StyleSpans<String> computePlaceholderStyle(String prompt) {
        StyleSpansBuilder<String> spansBuilder = new StyleSpansBuilder<>();
        spansBuilder.add("-fx-fill: #BBBBBB;", prompt.length());
        return spansBuilder.create();
    }

    /**
     * Creates a VBox containing action buttons for managing test steps and files.
     *
     * @param primaryStage the primary application stage
     * @param checkUnsavedChanges function to check for unsaved changes
     * @param saveToFile function to save to a specific file
     * @param saveAsToFile function to save as a new file
     * @return VBox containing the buttons
     */
    public VBox createButtonsVBox(Stage primaryStage, Function<Stage, Boolean> checkUnsavedChanges,
                                  BiFunction<File, Stage, Boolean> saveToFile,
                                  Function<Stage, Boolean> saveAsToFile) {
        // Main container for buttons
        VBox buttonsVBox = new VBox(10);
        buttonsVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 10px;");
        buttonsVBox.setAlignment(Pos.TOP_CENTER);

        // Button to add a new test step at the end
        Button addStepButton = new Button("Add Step");
        addStepButton.setStyle(BUTTON_STYLE);
        addStepButton.setTooltip(new Tooltip("Add a new test step to the table"));
        addStepButton.setOnAction(e -> {
            table.getItems().add(new String[columnNames.length]);
            table.getSelectionModel().select(table.getItems().size() - 1);
            table.refresh();
            app.setModified(true);
            updateButtonStates();
        });
        addStepButton.setOnMouseEntered(e -> addStepButton.setStyle(BUTTON_HOVER_STYLE));
        addStepButton.setOnMouseExited(e -> addStepButton.setStyle(BUTTON_STYLE));

        // Button to add a step above the selected row
        addAboveButton = new Button("Add Above");
        addAboveButton.setStyle(BUTTON_STYLE);
        addAboveButton.setTooltip(new Tooltip("Add a new step above the selected row"));
        addAboveButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().add(selectedIndex, new String[columnNames.length]);
                table.getSelectionModel().select(selectedIndex);
                table.refresh();
                app.setModified(true);
            }
            updateButtonStates();
        });
        addAboveButton.setOnMouseEntered(e -> addAboveButton.setStyle(BUTTON_HOVER_STYLE));
        addAboveButton.setOnMouseExited(e -> addAboveButton.setStyle(BUTTON_STYLE));

        // Button to add a step below the selected row
        addBelowButton = new Button("Add Below");
        addBelowButton.setStyle(BUTTON_STYLE);
        addBelowButton.setTooltip(new Tooltip("Add a step below the selected row"));
        addBelowButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().add(selectedIndex + 1, new String[columnNames.length]);
                table.getSelectionModel().select(selectedIndex + 1);
                table.refresh();
                app.setModified(true);
            }
            updateButtonStates();
        });
        addBelowButton.setOnMouseEntered(e -> addBelowButton.setStyle(BUTTON_HOVER_STYLE));
        addBelowButton.setOnMouseExited(e -> addBelowButton.setStyle(BUTTON_STYLE));

        // Button to move selected step up
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
                app.setModified(true);
            }
            updateButtonStates();
        });
        moveUpButton.setOnMouseEntered(e -> moveUpButton.setStyle(BUTTON_HOVER_STYLE));
        moveUpButton.setOnMouseExited(e -> moveUpButton.setStyle(BUTTON_STYLE));

        // Button to move selected step down
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
                app.setModified(true);
            }
            updateButtonStates();
        });
        moveDownButton.setOnMouseEntered(e -> moveDownButton.setStyle(BUTTON_HOVER_STYLE));
        moveDownButton.setOnMouseExited(e -> moveDownButton.setStyle(BUTTON_STYLE));

        // Button to delete the selected step
        deleteStepButton = new Button("Delete Step");
        deleteStepButton.setStyle(BUTTON_STYLE);
        deleteStepButton.setTooltip(new Tooltip("Delete the selected step"));
        deleteStepButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                String[] selectedRow = table.getItems().get(selectedIndex);
                String testId = selectedRow[ColumnIndex.TEST_ID.getIndex()];
                if (testId != null && !testId.isEmpty()) {
                    // Confirmation dialog for steps with Test ID
                    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmation.setTitle("Confirm Delete");
                    confirmation.setHeaderText("Delete Test Step");
                    confirmation.setContentText("The selected step has a Test ID: " + testId + ". Are you sure you want to delete it?");
                    Optional<ButtonType> result = confirmation.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        table.getItems().remove(selectedIndex);
                        table.refresh();
                        app.setModified(true);
                    }
                } else {
                    // Direct delete for non-Test ID steps
                    table.getItems().remove(selectedIndex);
                    table.refresh();
                    app.setModified(true);
                }
            }
            updateButtonStates();
        });
        deleteStepButton.setOnMouseEntered(e -> deleteStepButton.setStyle(BUTTON_HOVER_STYLE));
        deleteStepButton.setOnMouseExited(e -> deleteStepButton.setStyle(BUTTON_STYLE));

        // Button to delete the entire test case
        deleteTestCaseButton = new Button("Delete Test Case");
        deleteTestCaseButton.setStyle(BUTTON_STYLE);
        deleteTestCaseButton.setTooltip(new Tooltip("Delete all steps for the selected test case"));
        deleteTestCaseButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                String[] selectedRow = table.getItems().get(selectedIndex);
                String testId = selectedRow[ColumnIndex.TEST_ID.getIndex()];
                if (testId == null || testId.isEmpty()) {
                    CreateEditAPITest.showError("Please select a row with a valid Test ID to delete the test case.");
                    return;
                }
                // Confirmation dialog for test case deletion
                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                confirmation.setTitle("Confirm Delete Test Case");
                confirmation.setHeaderText("Delete Test Case: " + testId);
                confirmation.setContentText("Are you sure you want to delete all steps for Test ID: " + testId + "?");
                Optional<ButtonType> result = confirmation.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // Find start and end of test case block
                    int startIndex = selectedIndex;
                    while (startIndex > 0 && (table.getItems().get(startIndex - 1)[ColumnIndex.TEST_ID.getIndex()] == null ||
                            table.getItems().get(startIndex - 1)[ColumnIndex.TEST_ID.getIndex()].isEmpty())) {
                        startIndex--;
                    }
                    int endIndex = startIndex;
                    while (endIndex < table.getItems().size() && (table.getItems().get(endIndex)[ColumnIndex.TEST_ID.getIndex()] == null ||
                            table.getItems().get(endIndex)[ColumnIndex.TEST_ID.getIndex()].isEmpty() ||
                            table.getItems().get(endIndex)[ColumnIndex.TEST_ID.getIndex()].equals(testId))) {
                        endIndex++;
                    }
                    if (endIndex > startIndex) {
                        table.getItems().subList(startIndex, endIndex).clear();
                    } else {
                        table.getItems().remove(startIndex);
                    }
                    table.refresh();
                    app.setModified(true);
                    if (!table.getItems().isEmpty()) {
                        int newIndex = Math.min(startIndex, table.getItems().size() - 1);
                        table.getSelectionModel().select(newIndex);
                    } else {
                        table.getSelectionModel().clearSelection();
                    }
                }
            }
            updateButtonStates();
        });
        deleteTestCaseButton.setOnMouseEntered(e -> deleteTestCaseButton.setStyle(BUTTON_HOVER_STYLE));
        deleteTestCaseButton.setOnMouseExited(e -> deleteTestCaseButton.setStyle(BUTTON_STYLE));

        // Button to save the test case (Save or Save As)
        saveTestButton = new Button("Save Test");
        saveTestButton.setStyle(BUTTON_STYLE);
        saveTestButton.setTooltip(new Tooltip("Save the test case to an Excel file"));
        saveTestButton.setOnAction(e -> {
            // Dialog to choose Save or Save As
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
                    if (app.getLoadedFile() == null) {
                        saveAsToFile.apply(primaryStage);
                    } else {
                        saveToFile.apply(app.getLoadedFile(), primaryStage);
                    }
                } else if (result.get() == saveAsButton) {
                    saveAsToFile.apply(primaryStage);
                }
            }
            updateButtonStates();
        });
        saveTestButton.setOnMouseEntered(e -> saveTestButton.setStyle(BUTTON_HOVER_STYLE));
        saveTestButton.setOnMouseExited(e -> saveTestButton.setStyle(BUTTON_STYLE));

        // Button to load a test case from Excel file
        Button loadTestButton = new Button("Load Test");
        loadTestButton.setStyle(BUTTON_STYLE);
        loadTestButton.setTooltip(new Tooltip("Load a test case from an Excel file"));
        loadTestButton.setOnAction(e -> {
            if (!checkUnsavedChanges.apply(primaryStage)) {
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
                    // Validate headers
                    boolean headersValid = headerRow != null && headerRow.getPhysicalNumberOfCells() == columnNames.length;
                    if (headersValid) {
                        for (int i = 0; i < columnNames.length; i++) {
                            Cell cell = headerRow.getCell(i);
                            String headerValue = cell != null ? cell.getStringCellValue() : "";
                            if (!columnNames[i].equals(headerValue)) {
                                headersValid = false;
                                break;
                            }
                        }
                    }
                    if (!headersValid) {
                        String fileName = file.getName().replaceFirst("[.][^.]+$", "");
                        CreateEditAPITest.showError("Invalid test suite " + fileName + ". Upload the valid test suite.");
                        return;
                    }
                    // Load data rows and validate Test IDs
                    table.getItems().clear();
                    Set<String> testIds = new HashSet<>();
                    List<String[]> validRows = new ArrayList<>();
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row != null) {
                            String[] data = new String[columnNames.length];
                            for (int j = 0; j < columnNames.length; j++) {
                                Cell cell = row.getCell(j);
                                if (j == ColumnIndex.TEST_ID.getIndex() && cell != null) {
                                    String testId;
                                    if (cell.getCellType() == CellType.NUMERIC) {
                                        int wholeNumber = (int) Math.floor(cell.getNumericCellValue());
                                        testId = String.valueOf(wholeNumber);
                                    } else {
                                        testId = cell.toString().trim();
                                    }
                                    if (!testId.isEmpty() && !CreateEditAPITest.isValidTestId(testId, testIds, testId)) {
                                        String fileName = file.getName().replaceFirst("[.][^.]+$", "");
                                        CreateEditAPITest.showError("Invalid Test ID '" + testId + "' in row " + (i + 1) + " of " + fileName + ". Must be digits, length <= 5, and unique.");
                                        return;
                                    }
                                    if (!testId.isEmpty()) {
                                        testIds.add(testId);
                                    }
                                    data[j] = testId;
                                } else {
                                    data[j] = cell != null ? cell.toString().trim() : "";
                                }
                            }
                            validRows.add(data);
                        }
                    }
                    table.getItems().addAll(validRows);
                    table.refresh();
                    if (!table.getItems().isEmpty()) {
                        table.getSelectionModel().select(0);
                    }
                    app.setModified(false);
                    app.setLoadedFile(file);
                    // Success message
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText("File Loaded");
                    alert.setContentText("Test case loaded successfully from " + file.getAbsolutePath());
                    alert.showAndWait();
                    try {
                        EnvJsonUpdater.updateEnvJsonFromTable(table);
                    } catch (IOException ex) {
                        CreateEditAPITest.showError("Failed to update env.json: " + ex.getMessage());
                    }
                } catch (IOException ex) {
                    String fileName = file.getName().replaceFirst("[.][^.]+$", "");
                    CreateEditAPITest.showError("Invalid test suite " + fileName + ". Upload the valid test suite.");
                }
            }
            updateButtonStates();
        });
        loadTestButton.setOnMouseEntered(e -> loadTestButton.setStyle(BUTTON_HOVER_STYLE));
        loadTestButton.setOnMouseExited(e -> loadTestButton.setStyle(BUTTON_STYLE));

        // Button to create a new empty test case
        createNewTestButton = new Button("Create New Test");
        createNewTestButton.setStyle(BUTTON_STYLE);
        createNewTestButton.setTooltip(new Tooltip("Start a new test case"));
        createNewTestButton.setOnAction(e -> {
            if (!checkUnsavedChanges.apply(primaryStage)) {
                return;
            }
            table.getItems().clear();
            table.refresh();
            app.setModified(false);
            app.setLoadedFile(null);
            updateButtonStates();
        });
        createNewTestButton.setOnMouseEntered(e -> createNewTestButton.setStyle(BUTTON_HOVER_STYLE));
        createNewTestButton.setOnMouseExited(e -> createNewTestButton.setStyle(BUTTON_STYLE));

        // Button to open environment variables editor
        Button addEditEnvVarButton = new Button("Add/Edit Env Var");
        addEditEnvVarButton.setStyle(BUTTON_STYLE);
        addEditEnvVarButton.setTooltip(new Tooltip("Add or edit environment variables"));
        addEditEnvVarButton.setOnAction(e -> {
            if (envVarStage != null && envVarStage.isShowing()) {
                Platform.runLater(() -> envVarStage.requestFocus());
            } else {
                try {
                    envVarStage = new Stage();
                    EnvVarList simpleTableWindow = new EnvVarList();
                    simpleTableWindow.start(envVarStage);
                    envVarStage.setOnCloseRequest(event -> {
                        envVarStage = null; // Clear reference when window is closed
                    });
                } catch (Exception ex) {
                    CreateEditAPITest.showError("Failed to open environment variables window: " + ex.getMessage());
                }
            }
        });
        addEditEnvVarButton.setOnMouseEntered(e -> addEditEnvVarButton.setStyle(BUTTON_HOVER_STYLE));
        addEditEnvVarButton.setOnMouseExited(e -> addEditEnvVarButton.setStyle(BUTTON_STYLE));

        // Button to import Postman collection
        Button importCollectionButton = new Button("Import Collection");
        importCollectionButton.setStyle(BUTTON_STYLE);
        importCollectionButton.setTooltip(new Tooltip("Import a collection of test cases"));
        importCollectionButton.setOnMouseEntered(e -> importCollectionButton.setStyle(BUTTON_HOVER_STYLE));
        importCollectionButton.setOnMouseExited(e -> importCollectionButton.setStyle(BUTTON_STYLE));
        importCollectionButton.setOnAction(e -> {
            if (!checkUnsavedChanges.apply(primaryStage)) {
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Import Postman Collection");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Documents"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                try {
                    List<String[]> rows = PostmanCollectionImporter.importFromFile(file);
                    table.getItems().clear();
                    table.getItems().addAll(rows);
                    table.refresh();
                    if (!rows.isEmpty()) {
                        table.getSelectionModel().select(0);
                    }
                    app.setModified(false);
                    app.setLoadedFile(null); // Imported from JSON, not XLSX
                    // Success message
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText("Collection Imported");
                    alert.setContentText("Postman collection imported successfully from " + file.getAbsolutePath());
                    alert.showAndWait();
                    EnvJsonUpdater.updateEnvJsonFromTable(table);
                } catch (Exception ex) {
                    CreateEditAPITest.showError("Failed to import Postman collection: " + ex.getMessage());
                }
            }
            updateButtonStates();
        });
        
        Button sslConfigButton = new Button("SSL Configuration");
        sslConfigButton.setStyle(BUTTON_STYLE);
        sslConfigButton.setTooltip(new Tooltip("Open SSL keystore and truststore configuration"));
        sslConfigButton.setOnMouseEntered(e -> sslConfigButton.setStyle(BUTTON_HOVER_STYLE));
        sslConfigButton.setOnMouseExited(e -> sslConfigButton.setStyle(BUTTON_STYLE));

        sslConfigButton.setOnAction(e -> {
            if (SSLConfigurationScreen.isShowing()) {
                Platform.runLater(() -> SSLConfigurationScreen.instance.requestFocus());
            } else {
                try {
                    Stage sslStage = new Stage();
                    new SSLConfigurationScreen().start(sslStage);
                } catch (Exception ex) {
                    CreateEditAPITest.showError("Failed to open SSL Configuration window: " + ex.getMessage());
                }
            }
        });

        // Add all buttons to the VBox
        buttonsVBox.getChildren().addAll(
                addStepButton,
                addAboveButton, addBelowButton,
                moveUpButton, moveDownButton,
                deleteStepButton, deleteTestCaseButton,
                saveTestButton, loadTestButton,
                createNewTestButton,
                addEditEnvVarButton,
                importCollectionButton,
                sslConfigButton
            );
        
        return buttonsVBox;
    }

    /**
     * Updates the enabled/disabled state of action buttons based on current table selection and content.
     */
    public void updateButtonStates() {
        int selectedIndex = table.getSelectionModel().getSelectedIndex();
        boolean hasSelection = selectedIndex >= 0;
        boolean hasItems = !table.getItems().isEmpty();
        addAboveButton.setDisable(!hasSelection);
        addBelowButton.setDisable(!hasSelection);
        moveUpButton.setDisable(!hasSelection || selectedIndex == 0);
        moveDownButton.setDisable(!hasSelection || selectedIndex == table.getItems().size() - 1);
        deleteStepButton.setDisable(!hasSelection);
        deleteTestCaseButton.setDisable(!hasSelection);
        saveTestButton.setDisable(!hasItems);
        createNewTestButton.setDisable(false);
    }

    /**
     * Moves focus to the next or previous traversable node in the scene.
     *
     * @param fromNode the current focused node
     * @param next true for next node, false for previous
     */
    private void moveFocus(Node fromNode, boolean next) {
        List<Node> traversables = new ArrayList<>();
        addTraversableNodes(fromNode.getScene().getRoot(), traversables);
        int currentIndex = traversables.indexOf(fromNode);
        if (currentIndex >= 0) {
            int targetIndex = currentIndex + (next ? 1 : -1);
            if (targetIndex < 0) {
                targetIndex = traversables.size() - 1;
            } else if (targetIndex >= traversables.size()) {
                targetIndex = 0;
            }
            traversables.get(targetIndex).requestFocus();
        }
    }

    /**
     * Recursively collects all focus-traversable, visible, and enabled nodes in the scene hierarchy.
     *
     * @param node the root node to start traversal
     * @param list the list to add traversable nodes to
     */
    private void addTraversableNodes(Node node, List<Node> list) {
        if (node.isFocusTraversable() && node.isVisible() && !node.isDisabled()) {
            list.add(node);
        }
        if (node instanceof Parent) {
            ((Parent) node).getChildrenUnmodifiable().forEach(child -> addTraversableNodes(child, list));
        }
    }

    /**
     * Closes the environment variables stage if it is open.
     */
    public static void closeEnvVarStage() {
        if (envVarStage != null && envVarStage.isShowing()) {
            envVarStage.close();
        }
        envVarStage = null;
    }

    public static UIComponentsManager getInstance() {
        return instance;
    }
}