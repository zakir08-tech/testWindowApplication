package com.test.window.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        TEST_ID(0), REQUEST(1), END_POINT(2), HEADER_KEY(3), HEADER_VALUE(4),
        PARAM_KEY(5), PARAM_VALUE(6), PAYLOAD(7), PAYLOAD_TYPE(8),
        RESPONSE_KEY_NAME(9), CAPTURE_VALUE(10), AUTHORIZATION(11),
        AUTH_FIELD1(12), AUTH_FIELD2(13), SSL_VALIDATION(14), EXPECTED_STATUS(15),
        VERIFY_RESPONSE(16), TEST_DESCRIPTION(17);

        private final int index;
        ColumnIndex(int index) { this.index = index; }
        public int getIndex() { return index; }
    }

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
        FXCollections.observableArrayList("", "json", "form-data", "urlencoded");

    /**
     * CSS style for unfocused text fields and combo boxes.
     */
    private static final String FIELD_STYLE_UNFOCUSED =
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #BBBBBB; -fx-border-radius: 5px;";

    /**
     * CSS style for focused text fields and combo boxes.
     */
    private static final String FIELD_STYLE_FOCUSED =
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #4A90E2; -fx-border-width: 2px; -fx-prompt-text-fill: #BBBBBB; -fx-border-radius: 5px;";

    /**
     * CSS style for disabled text fields and combo boxes.
     */
    private static final String FIELD_STYLE_DISABLED =
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: #888888; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #BBBBBB; -fx-border-radius: 5px;";

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
    private static final double TEXT_FIELD_HEIGHT = 30.0;

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
    private TextArea payloadField;

    /**
     * Text area for verifying response content.
     */
    private TextArea verifyResponseField;

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
        this.statusLabel = statusLabel;
        this.columnNames = columnNames;
        this.tableManager = tableManager;
        this.app = app;
    }

    /**
     * Creates an HBox containing text fields and combo boxes for basic test step editing
     * (request method, endpoint, status, payload type, authorization).
     *
     * @return HBox containing the input fields
     */
    public HBox createTextFieldsBox() {
        // Create combo box for HTTP request methods
        ComboBox<String> requestComboBox = new ComboBox<>(HTTP_METHODS);
        requestComboBox.setPromptText("Request");
        requestComboBox.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Request");
                } else {
                    setText(item);
                }
            }
        });
        requestComboBox.setStyle(FIELD_STYLE_UNFOCUSED);
        requestComboBox.setPrefHeight(TEXT_FIELD_HEIGHT);
        requestComboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!requestComboBox.isDisable()) {
                requestComboBox.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
            }
        });
        requestComboBox.setDisable(true);
        requestComboBox.prefWidthProperty().bind(table.widthProperty().multiply(0.07));
        requestComboBox.maxWidthProperty().bind(requestComboBox.prefWidthProperty());
        requestComboBox.minWidthProperty().bind(requestComboBox.prefWidthProperty());

        // Create text field for endpoint URL
        TextField endpointField = new TextField();
        endpointField.setPromptText("End-Point");
        endpointField.setStyle(FIELD_STYLE_UNFOCUSED);
        endpointField.setPrefHeight(TEXT_FIELD_HEIGHT);
        endpointField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            endpointField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });
        endpointField.setDisable(true);
        endpointField.prefWidthProperty().bind(table.widthProperty().multiply(0.375));
        endpointField.maxWidthProperty().bind(endpointField.prefWidthProperty());
        endpointField.minWidthProperty().bind(endpointField.prefWidthProperty());

        // Create text field for expected status code
        TextField statusField = new TextField();
        statusField.setPromptText("Status");
        statusField.setStyle(FIELD_STYLE_UNFOCUSED);
        statusField.setPrefHeight(TEXT_FIELD_HEIGHT);
        statusField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            statusField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });
        statusField.setDisable(true);
        statusField.prefWidthProperty().bind(table.widthProperty().multiply(0.07));
        statusField.maxWidthProperty().bind(statusField.prefWidthProperty());
        statusField.minWidthProperty().bind(statusField.prefWidthProperty());

        // Create combo box for payload types
        ComboBox<String> payloadTypeComboBox = new ComboBox<>(PAYLOAD_TYPES);
        payloadTypeComboBox.setPromptText("Payload Type");
        payloadTypeComboBox.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Payload Type");
                } else {
                    setText(item);
                }
            }
        });
        payloadTypeComboBox.setStyle(FIELD_STYLE_UNFOCUSED);
        payloadTypeComboBox.setPrefHeight(TEXT_FIELD_HEIGHT);
        payloadTypeComboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!payloadTypeComboBox.isDisable()) {
                payloadTypeComboBox.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
            }
        });
        payloadTypeComboBox.setDisable(true);
        payloadTypeComboBox.prefWidthProperty().bind(table.widthProperty().multiply(0.0924));
        payloadTypeComboBox.maxWidthProperty().bind(payloadTypeComboBox.prefWidthProperty());
        payloadTypeComboBox.minWidthProperty().bind(payloadTypeComboBox.prefWidthProperty());

        // Create combo box for authorization types
        ComboBox<String> authComboBox = new ComboBox<>(AUTH_OPTIONS);
        authComboBox.setPromptText("Authorization");
        authComboBox.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Authorization");
                } else {
                    setText(item);
                }
            }
        });
        authComboBox.setStyle(FIELD_STYLE_UNFOCUSED);
        authComboBox.setPrefHeight(TEXT_FIELD_HEIGHT);
        authComboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!authComboBox.isDisable()) {
                authComboBox.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
            }
        });
        authComboBox.setDisable(true);
        authComboBox.prefWidthProperty().bind(table.widthProperty().multiply(0.0924));
        authComboBox.maxWidthProperty().bind(authComboBox.prefWidthProperty());
        authComboBox.minWidthProperty().bind(authComboBox.prefWidthProperty());

        // Create text field for username or token (depending on auth type)
        TextField usernameTokenField = new TextField();
        usernameTokenField.setPromptText("Username/Token");
        usernameTokenField.setStyle(FIELD_STYLE_UNFOCUSED);
        usernameTokenField.setPrefHeight(TEXT_FIELD_HEIGHT);
        usernameTokenField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            usernameTokenField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });
        usernameTokenField.setDisable(true);
        usernameTokenField.setVisible(false);
        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());

        // Create text field for password (for Basic Auth)
        TextField passwordField = new TextField();
        passwordField.setPromptText("Password");
        passwordField.setStyle(FIELD_STYLE_UNFOCUSED);
        passwordField.setPrefHeight(TEXT_FIELD_HEIGHT);
        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            passwordField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });
        passwordField.setDisable(true);
        passwordField.setVisible(false);
        passwordField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
        passwordField.maxWidthProperty().bind(passwordField.prefWidthProperty());
        passwordField.minWidthProperty().bind(passwordField.prefWidthProperty());

        // Create HBox to hold all fields with spacing
        HBox textFieldsBox = new HBox(10);
        textFieldsBox.setStyle("-fx-background-color: #2E2E2E;");
        textFieldsBox.getChildren().addAll(requestComboBox, endpointField, statusField, payloadTypeComboBox, authComboBox, usernameTokenField, passwordField);
        HBox.setHgrow(requestComboBox, Priority.NEVER);
        HBox.setHgrow(endpointField, Priority.ALWAYS);
        HBox.setHgrow(statusField, Priority.NEVER);
        HBox.setHgrow(payloadTypeComboBox, Priority.NEVER);
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
                requestComboBox.setValue(row[ColumnIndex.REQUEST.getIndex()]);
                endpointField.setText(row[ColumnIndex.END_POINT.getIndex()]);
                statusField.setText(row[ColumnIndex.EXPECTED_STATUS.getIndex()]);
                payloadTypeComboBox.setValue(row[ColumnIndex.PAYLOAD_TYPE.getIndex()] != null ? row[ColumnIndex.PAYLOAD_TYPE.getIndex()] : "");
                authComboBox.setValue(row[ColumnIndex.AUTHORIZATION.getIndex()]);
                usernameTokenField.setText(row[ColumnIndex.AUTH_FIELD1.getIndex()] != null ? row[ColumnIndex.AUTH_FIELD1.getIndex()] : "");
                passwordField.setText(row[ColumnIndex.AUTH_FIELD2.getIndex()] != null ? row[ColumnIndex.AUTH_FIELD2.getIndex()] : "");
                requestComboBox.setDisable(!isValidTestId);
                endpointField.setDisable(!isValidTestId);
                statusField.setDisable(!isValidTestId);
                payloadTypeComboBox.setDisable(!isValidTestId);
                authComboBox.setDisable(!isValidTestId);
                if (!isValidTestId) {
                    requestComboBox.setStyle(FIELD_STYLE_DISABLED);
                    authComboBox.setStyle(FIELD_STYLE_DISABLED);
                    payloadTypeComboBox.setStyle(FIELD_STYLE_DISABLED);
                    requestComboBox.setPromptText("Request");
                    authComboBox.setPromptText("Authorization");
                    payloadTypeComboBox.setPromptText("Payload Type");
                    requestComboBox.setValue(null);
                    requestComboBox.getSelectionModel().clearSelection();
                    payloadTypeComboBox.setValue(null);
                    payloadTypeComboBox.getSelectionModel().clearSelection();
                    authComboBox.setValue(null);
                    authComboBox.getSelectionModel().clearSelection();
                } else {
                    requestComboBox.setStyle(requestComboBox.isFocused() ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    authComboBox.setStyle(authComboBox.isFocused() ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    payloadTypeComboBox.setStyle(payloadTypeComboBox.isFocused() ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                }
                if (isValidTestId && authComboBox.getValue() != null) {
                    if ("Basic Auth".equals(authComboBox.getValue())) {
                        usernameTokenField.setPromptText("Username");
                        usernameTokenField.setVisible(true);
                        usernameTokenField.setDisable(false);
                        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
                        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        passwordField.setVisible(true);
                        passwordField.setDisable(false);
                    } else if ("Bearer Token".equals(authComboBox.getValue())) {
                        usernameTokenField.setPromptText("Token");
                        usernameTokenField.setVisible(true);
                        usernameTokenField.setDisable(false);
                        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.21));
                        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        passwordField.setVisible(false);
                        passwordField.setDisable(true);
                    } else {
                        usernameTokenField.setPromptText("Username/Token");
                        usernameTokenField.setVisible(false);
                        usernameTokenField.setDisable(true);
                        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
                        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        passwordField.setVisible(false);
                        passwordField.setDisable(true);
                    }
                } else {
                    usernameTokenField.setPromptText("Username/Token");
                    usernameTokenField.setVisible(false);
                    usernameTokenField.setDisable(true);
                    usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
                    usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                    usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                    passwordField.setVisible(false);
                    passwordField.setDisable(true);
                }
            } else {
                // Clear fields when no selection
                requestComboBox.setValue(null);
                requestComboBox.getSelectionModel().clearSelection();
                requestComboBox.setStyle(FIELD_STYLE_DISABLED);
                requestComboBox.setPromptText("Request");
                endpointField.clear();
                statusField.clear();
                payloadTypeComboBox.setValue(null);
                payloadTypeComboBox.getSelectionModel().clearSelection();
                payloadTypeComboBox.setStyle(FIELD_STYLE_DISABLED);
                payloadTypeComboBox.setPromptText("Payload Type");
                authComboBox.setValue(null);
                authComboBox.getSelectionModel().clearSelection();
                authComboBox.setStyle(FIELD_STYLE_DISABLED);
                authComboBox.setPromptText("Authorization");
                usernameTokenField.setPromptText("Username/Token");
                usernameTokenField.clear();
                usernameTokenField.setVisible(false);
                usernameTokenField.setDisable(true);
                passwordField.clear();
                passwordField.setVisible(false);
                passwordField.setDisable(true);
                requestComboBox.setDisable(true);
                endpointField.setDisable(true);
                statusField.setDisable(true);
                payloadTypeComboBox.setDisable(true);
                authComboBox.setDisable(true);
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
                        usernameTokenField.setPromptText("Username");
                        usernameTokenField.setVisible(true);
                        usernameTokenField.setDisable(false);
                        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
                        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        passwordField.setVisible(true);
                        passwordField.setDisable(false);
                    } else if ("Bearer Token".equals(newVal)) {
                        usernameTokenField.setPromptText("Token");
                        usernameTokenField.setVisible(true);
                        usernameTokenField.setDisable(false);
                        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.21));
                        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        passwordField.setVisible(false);
                        passwordField.setDisable(true);
                    } else {
                        usernameTokenField.setPromptText("Username/Token");
                        usernameTokenField.setVisible(false);
                        usernameTokenField.setDisable(true);
                        usernameTokenField.prefWidthProperty().bind(table.widthProperty().multiply(0.14));
                        usernameTokenField.maxWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        usernameTokenField.minWidthProperty().bind(usernameTokenField.prefWidthProperty());
                        passwordField.setVisible(false);
                        passwordField.setDisable(true);
                    }
                }
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

        // Listener for endpoint field changes to update table
        endpointField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[ColumnIndex.END_POINT.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });

        // Listener for status field changes to update table
        statusField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[ColumnIndex.EXPECTED_STATUS.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });

        // Listener for payload type combo box changes to update table
        payloadTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[ColumnIndex.PAYLOAD_TYPE.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });

        // Listener for username/token field changes to update table
        usernameTokenField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[ColumnIndex.AUTH_FIELD1.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });

        // Listener for password field changes to update table
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[ColumnIndex.AUTH_FIELD2.getIndex()] = newVal;
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
        // Main container for additional content
        VBox additionalContent = new VBox(10);
        additionalContent.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
        additionalContent.setAlignment(Pos.CENTER_LEFT);

        // VBox and ScrollPane for header key-value pairs
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

        // VBox and ScrollPane for parameter key-value pairs
        VBox paramFieldsVBox = new VBox(5);
        paramFieldsVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
        ScrollPane paramScroll = new ScrollPane(paramFieldsVBox);
        paramScroll.setStyle("-fx-background-color: #2E2E2E; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
        paramScroll.setFitToWidth(true);
        paramScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        paramScroll.setPrefHeight(200);
        paramScroll.setMaxHeight(200);
        paramScroll.setMinHeight(200);
        paramFieldsVBox.setMaxHeight(190);

        // VBox and ScrollPane for response capture key-value pairs
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

        // Text area for payload editing with JSON formatting support
        payloadField = new TextArea();
        payloadField.setPromptText("Payload");
        payloadField.setStyle(FIELD_STYLE_UNFOCUSED);
        payloadField.setPrefHeight(300);
        payloadField.setMinHeight(300);
        payloadField.setMaxHeight(300);
        payloadField.setWrapText(true);
        payloadField.setDisable(false);
        payloadField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            payloadField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });

        // Listener for payload field changes to update table
        payloadField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && payloadField.isEditable()) {
                table.getItems().get(selectedIndex)[ColumnIndex.PAYLOAD.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });

        // Handle TAB key in payload field for focus traversal (no tab insertion)
        payloadField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                event.consume(); // Prevent tab character insertion
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0 && payloadField.isEditable()) {
                    String rawText = payloadField.getText();
                    table.getItems().get(selectedIndex)[ColumnIndex.PAYLOAD.getIndex()] = rawText;
                    table.refresh();
                    app.setModified(true);
                }
                moveFocus(payloadField, !event.isShiftDown()); // Move focus forward or backward
            }
        });

        // Text area for response verification with JSON formatting support
        verifyResponseField = new TextArea();
        verifyResponseField.setPromptText("Verify Response");
        verifyResponseField.setStyle(FIELD_STYLE_UNFOCUSED);
        verifyResponseField.setPrefHeight(310);
        verifyResponseField.setMinHeight(310);
        verifyResponseField.setMaxHeight(310);
        verifyResponseField.setWrapText(true);
        verifyResponseField.setDisable(false);
        verifyResponseField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            verifyResponseField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });

        // Listener for verify response field changes to update table
        verifyResponseField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && verifyResponseField.isEditable()) {
                table.getItems().get(selectedIndex)[ColumnIndex.VERIFY_RESPONSE.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });

        // Handle TAB key in verify response field for focus traversal
        verifyResponseField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                event.consume(); // Prevent tab character insertion
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0 && verifyResponseField.isEditable()) {
                    String rawText = verifyResponseField.getText();
                    table.getItems().get(selectedIndex)[ColumnIndex.VERIFY_RESPONSE.getIndex()] = rawText;
                    table.refresh();
                    app.setModified(true);
                }
                moveFocus(verifyResponseField, !event.isShiftDown()); // Move focus forward or backward
            }
        });

        // Create left VBox for scroll panes
        VBox leftVBox = new VBox(10, headerFieldsScroll, paramScroll, responseCaptureScroll);
        leftVBox.setAlignment(Pos.TOP_LEFT);
        GridPane.setValignment(leftVBox, VPos.TOP);
        // Bind widths for left scrolls
        paramScroll.prefWidthProperty().bind(headerFieldsScroll.widthProperty());
        paramScroll.maxWidthProperty().bind(headerFieldsScroll.widthProperty());
        responseCaptureScroll.prefWidthProperty().bind(headerFieldsScroll.widthProperty());
        responseCaptureScroll.maxWidthProperty().bind(headerFieldsScroll.widthProperty());

        // Create right VBox for text areas, with verifyResponseField under payloadField
        VBox rightVBox = new VBox(10, payloadField, verifyResponseField);
        rightVBox.setAlignment(Pos.TOP_LEFT);
        GridPane.setValignment(rightVBox, VPos.TOP);

        // GridPane to layout the left and right VBoxes
        GridPane additionalFields = new GridPane();
        additionalFields.setHgap(10);
        additionalFields.setVgap(10);
        additionalFields.setAlignment(Pos.CENTER_LEFT);
        additionalFields.add(leftVBox, 0, 0);
        additionalFields.add(rightVBox, 1, 0);

        // Set column widths for the grid
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(40);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(60);
        additionalFields.getColumnConstraints().addAll(col1, col2);
        additionalContent.getChildren().add(additionalFields);

        // Listener for table selection to populate dynamic fields (headers, params, etc.) for test steps
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            // Clear all dynamic field containers
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
                payloadField.setDisable(!isValid);
                verifyResponseField.setDisable(!isValid);

                if (!isValid) {
                    payloadField.clear();
                    verifyResponseField.clear();
                    return;
                }

                // Populate payload and verify response fields with formatted JSON
                String payload = newItem[ColumnIndex.PAYLOAD.getIndex()] != null ? newItem[ColumnIndex.PAYLOAD.getIndex()] : "";
                payloadField.setText(CreateEditAPITest.formatJson(payload, statusLabel));
                String verify = newItem[ColumnIndex.VERIFY_RESPONSE.getIndex()] != null ? newItem[ColumnIndex.VERIFY_RESPONSE.getIndex()] : "";
                verifyResponseField.setText(CreateEditAPITest.formatJson(verify, statusLabel));

                // Find start of current test case block
                int start = selectedIndex;
                while (start >= 0 && (table.getItems().get(start)[ColumnIndex.TEST_ID.getIndex()] == null ||
                        table.getItems().get(start)[ColumnIndex.TEST_ID.getIndex()].isEmpty())) {
                    start--;
                }
                if (start < 0) start = 0;

                // Collect all row indices for the current test case
                List<Integer> rowIndices = new ArrayList<>();
                for (int i = start; i < table.getItems().size(); i++) {
                    String[] r = table.getItems().get(i);
                    if (i > start && r[ColumnIndex.TEST_ID.getIndex()] != null &&
                            !r[ColumnIndex.TEST_ID.getIndex()].isEmpty()) break;
                    rowIndices.add(i);
                }

                // Dynamically create and add fields for each row in the test case
                for (Integer rowIndex : rowIndices) {
                    String[] row = table.getItems().get(rowIndex);

                    // Header key-value pair
                    TextField headerKeyField = new TextField(row[ColumnIndex.HEADER_KEY.getIndex()] != null ?
                            row[ColumnIndex.HEADER_KEY.getIndex()] : "");
                    headerKeyField.setPromptText("Header Key");
                    headerKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                    headerKeyField.setPrefHeight(TEXT_FIELD_HEIGHT);
                    headerKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        headerKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    });

                    TextField headerValueField = new TextField(row[ColumnIndex.HEADER_VALUE.getIndex()] != null ?
                            row[ColumnIndex.HEADER_VALUE.getIndex()] : "");
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
                        table.refresh();
                        app.setModified(true);
                    });

                    headerValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[ColumnIndex.HEADER_VALUE.getIndex()] = newVal2.trim();
                        table.refresh();
                        app.setModified(true);
                    });

                    headerFieldsVBox.getChildren().add(headerPair);

                    // Parameter key-value pair
                    TextField paramKeyField = new TextField(row[ColumnIndex.PARAM_KEY.getIndex()] != null ?
                            row[ColumnIndex.PARAM_KEY.getIndex()] : "");
                    paramKeyField.setPromptText("Parameter Key");
                    paramKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                    paramKeyField.setPrefHeight(TEXT_FIELD_HEIGHT);
                    paramKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        paramKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    });

                    TextField paramValueField = new TextField(row[ColumnIndex.PARAM_VALUE.getIndex()] != null ?
                            row[ColumnIndex.PARAM_VALUE.getIndex()] : "");
                    paramValueField.setPromptText("Parameter Value");
                    paramValueField.setStyle(FIELD_STYLE_UNFOCUSED);
                    paramValueField.setPrefHeight(TEXT_FIELD_HEIGHT);
                    paramValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        paramValueField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    });

                    HBox paramPair = new HBox(5, paramKeyField, paramValueField);
                    paramPair.setAlignment(Pos.CENTER_LEFT);
                    paramKeyField.prefWidthProperty().bind(headerFieldsScroll.widthProperty().multiply(0.5).subtract(8.5));
                    paramKeyField.maxWidthProperty().bind(paramKeyField.prefWidthProperty());
                    paramKeyField.minWidthProperty().bind(paramKeyField.prefWidthProperty());
                    paramValueField.prefWidthProperty().bind(headerFieldsScroll.widthProperty().multiply(0.5).subtract(8.5));
                    paramValueField.maxWidthProperty().bind(paramValueField.prefWidthProperty());
                    paramValueField.minWidthProperty().bind(paramValueField.prefWidthProperty());

                    paramKeyField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[ColumnIndex.PARAM_KEY.getIndex()] = newVal2.trim();
                        table.refresh();
                        app.setModified(true);
                    });

                    paramValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[ColumnIndex.PARAM_VALUE.getIndex()] = newVal2.trim();
                        table.refresh();
                        app.setModified(true);
                    });

                    paramFieldsVBox.getChildren().add(paramPair);

                    // Response key-capture value pair
                    TextField responseKeyField = new TextField(row[ColumnIndex.RESPONSE_KEY_NAME.getIndex()] != null ?
                            row[ColumnIndex.RESPONSE_KEY_NAME.getIndex()] : "");
                    responseKeyField.setPromptText("Response Key Name");
                    responseKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                    responseKeyField.setPrefHeight(TEXT_FIELD_HEIGHT);
                    responseKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        responseKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    });

                    TextField captureValueField = new TextField(row[ColumnIndex.CAPTURE_VALUE.getIndex()] != null ?
                            row[ColumnIndex.CAPTURE_VALUE.getIndex()] : "");
                    captureValueField.setPromptText("Capture Value (env var)");
                    captureValueField.setStyle(FIELD_STYLE_UNFOCUSED);
                    captureValueField.setPrefHeight(TEXT_FIELD_HEIGHT);
                    captureValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        captureValueField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    });

                    HBox responsePair = new HBox(5, responseKeyField, captureValueField);
                    responsePair.setAlignment(Pos.CENTER_LEFT);
                    responseKeyField.prefWidthProperty().bind(headerFieldsScroll.widthProperty().multiply(0.5).subtract(8.5));
                    responseKeyField.maxWidthProperty().bind(responseKeyField.prefWidthProperty());
                    responseKeyField.minWidthProperty().bind(responseKeyField.prefWidthProperty());
                    captureValueField.prefWidthProperty().bind(headerFieldsScroll.widthProperty().multiply(0.5).subtract(8.5));
                    captureValueField.maxWidthProperty().bind(captureValueField.prefWidthProperty());
                    captureValueField.minWidthProperty().bind(captureValueField.prefWidthProperty());

                    responseKeyField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[ColumnIndex.RESPONSE_KEY_NAME.getIndex()] = newVal2.trim();
                        table.refresh();
                        app.setModified(true);
                    });

                    captureValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[ColumnIndex.CAPTURE_VALUE.getIndex()] = newVal2.trim();
                        table.refresh();
                        app.setModified(true);
                    });

                    responseCaptureVBox.getChildren().add(responsePair);
                }
            } else {
                // Disable and clear payload and verify fields when no selection
                payloadField.clear();
                payloadField.setDisable(true);
                verifyResponseField.clear();
                verifyResponseField.setDisable(true);
            }
        });

        return additionalContent;
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
        addBelowButton.setTooltip(new Tooltip("Add a new step below the selected row"));
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

        // Add all buttons to the VBox
        buttonsVBox.getChildren().addAll(
            addStepButton, addAboveButton, addBelowButton, moveUpButton, moveDownButton,
            deleteStepButton, deleteTestCaseButton, saveTestButton, loadTestButton,
            createNewTestButton, addEditEnvVarButton, importCollectionButton
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
    
    /**
     * Simple highlighter for {{any-text}} in pink (used by StyledTextArea).
     */
    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        Pattern pattern = Pattern.compile("\\{\\{[^}]+\\}\\}");  // Matches {{any-text}}
        Matcher matcher = pattern.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            // Plain text before match
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastEnd);
            // Highlighted match (pink + bold)
            spansBuilder.add(Collections.singleton("-fx-fill: pink; -fx-font-weight: bold;"), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        // Remaining plain text
        spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
        return spansBuilder.create();
    }
}