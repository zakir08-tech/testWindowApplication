package com.test.window.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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

public class UIComponentsManager {

    private enum ColumnIndex {
        TEST_ID(0), REQUEST(1), END_POINT(2), HEADER_KEY(3), HEADER_VALUE(4),
        PARAM_KEY(5), PARAM_VALUE(6), PAYLOAD(7), PAYLOAD_TYPE(8),
        MODIFY_PAYLOAD_KEY(9), MODIFY_PAYLOAD_VALUE(10), RESPONSE_KEY_NAME(11),
        CAPTURE_VALUE(12), AUTHORIZATION(13), AUTH_FIELD1(14), AUTH_FIELD2(15),
        SSL_VALIDATION(16), EXPECTED_STATUS(17), VERIFY_RESPONSE(18), TEST_DESCRIPTION(19);

        private final int index;
        ColumnIndex(int index) { this.index = index; }
        public int getIndex() { return index; }
    }

    private static final ObservableList<String> AUTH_OPTIONS = 
        FXCollections.observableArrayList("", "Basic Auth", "Bearer Token");

    private static final ObservableList<String> HTTP_METHODS = 
        FXCollections.observableArrayList("", "GET", "POST", "PUT", "PATCH", "DELETE");

    private static final ObservableList<String> PAYLOAD_TYPES = 
        FXCollections.observableArrayList("", "json", "form-data", "urlencoded");

    private static final String FIELD_STYLE_UNFOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #BBBBBB; -fx-border-radius: 5px;";

    private static final String FIELD_STYLE_FOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #4A90E2; -fx-border-width: 2px; -fx-prompt-text-fill: #BBBBBB; -fx-border-radius: 5px;";

    private static final String FIELD_STYLE_DISABLED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: #888888; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #BBBBBB; -fx-border-radius: 5px;";

    private static final String BUTTON_STYLE = 
        "-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

    private static final String BUTTON_HOVER_STYLE = 
        "-fx-background-color: #6AB0FF; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

    private static final double TEXT_FIELD_HEIGHT = 30.0;

    private final TableView<String[]> table;
    private final Label statusLabel;
    private final String[] columnNames;
    private final TableManager tableManager;
    private final CreateEditAPITestTemplate app;
    private TextArea payloadField;
    private TextArea verifyResponseField;
    private ScrollPane headerFieldsScroll;
    private Button addAboveButton, addBelowButton, moveUpButton, moveDownButton, 
                   deleteStepButton, deleteTestCaseButton, saveTestButton, createNewTestButton;
    private static Stage envVarStage; // Track single EnvVarList window

    public UIComponentsManager(TableView<String[]> table, Label statusLabel, String[] columnNames, TableManager tableManager, CreateEditAPITestTemplate app) {
        this.table = table;
        this.statusLabel = statusLabel;
        this.columnNames = columnNames;
        this.tableManager = tableManager;
        this.app = app;
    }

    public HBox createTextFieldsBox() {
        ComboBox<String> requestComboBox = new ComboBox<>(HTTP_METHODS);
        requestComboBox.setPromptText("Request");
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

        ComboBox<String> payloadTypeComboBox = new ComboBox<>(PAYLOAD_TYPES);
        payloadTypeComboBox.setPromptText("Payload Type");
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

        ComboBox<String> authComboBox = new ComboBox<>(AUTH_OPTIONS);
        authComboBox.setPromptText("Authorization");
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
                boolean isValidTestId = CreateEditAPITestTemplate.isValidTestId(testId, testIds, testId);
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
                requestComboBox.setValue(null);
                requestComboBox.setStyle(FIELD_STYLE_DISABLED);
                requestComboBox.setPromptText("Request");
                endpointField.clear();
                statusField.clear();
                payloadTypeComboBox.setValue(null);
                payloadTypeComboBox.setStyle(FIELD_STYLE_DISABLED);
                payloadTypeComboBox.setPromptText("Payload Type");
                authComboBox.setValue(null);
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
                boolean isValidTestId = CreateEditAPITestTemplate.isValidTestId(testId, testIds, testId);
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
        });

        requestComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[ColumnIndex.REQUEST.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });

        endpointField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[ColumnIndex.END_POINT.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });

        statusField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[ColumnIndex.EXPECTED_STATUS.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });

        payloadTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[ColumnIndex.PAYLOAD_TYPE.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });

        usernameTokenField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[ColumnIndex.AUTH_FIELD1.getIndex()] = newVal;
                table.refresh();
                app.setModified(true);
            }
        });

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

    public VBox createAdditionalContent() {
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
        payloadField.setDisable(false);
        payloadField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            payloadField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });

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

        verifyResponseField = new TextArea();
        verifyResponseField.setPromptText("Verify Response");
        verifyResponseField.setStyle(FIELD_STYLE_UNFOCUSED);
        verifyResponseField.setPrefHeight(200);
        verifyResponseField.setMinHeight(200);
        verifyResponseField.setMaxHeight(200);
        verifyResponseField.setWrapText(true);
        verifyResponseField.setDisable(false);
        verifyResponseField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            verifyResponseField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });

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

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            headerFieldsVBox.getChildren().clear();
            modifyPayloadVBox.getChildren().clear();
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
                boolean isValid = CreateEditAPITestTemplate.isValidTestId(testId, testIds, testId);
                payloadField.setDisable(false);
                payloadField.setEditable(isValid);
                verifyResponseField.setDisable(false);
                verifyResponseField.setEditable(isValid);

                if (!isValid) {
                    payloadField.clear();
                    verifyResponseField.clear();
                    return;
                }

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

                for (Integer rowIndex : rowIndices) {
                    String[] row = table.getItems().get(rowIndex);
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
                    headerKeyField.setPrefHeight(TEXT_FIELD_HEIGHT);
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

                    TextField modifyKeyField = new TextField(row[ColumnIndex.MODIFY_PAYLOAD_KEY.getIndex()] != null ? 
                            row[ColumnIndex.MODIFY_PAYLOAD_KEY.getIndex()] : "");
                    modifyKeyField.setPromptText("Modify Payload Key");
                    modifyKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                    modifyKeyField.setPrefHeight(TEXT_FIELD_HEIGHT);
                    modifyKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        modifyKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    });

                    TextField modifyValueField = new TextField(row[ColumnIndex.MODIFY_PAYLOAD_VALUE.getIndex()] != null ? 
                            row[ColumnIndex.MODIFY_PAYLOAD_VALUE.getIndex()] : "");
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
                        table.refresh();
                        app.setModified(true);
                    });

                    modifyValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[ColumnIndex.MODIFY_PAYLOAD_VALUE.getIndex()] = newVal2.trim();
                        table.refresh();
                        app.setModified(true);
                    });

                    modifyPayloadVBox.getChildren().add(modifyPair);

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
                    paramKeyField.prefWidthProperty().bind(paramListField.widthProperty().multiply(0.5).subtract(8.5));
                    paramKeyField.maxWidthProperty().bind(paramKeyField.prefWidthProperty());
                    paramKeyField.minWidthProperty().bind(paramKeyField.prefWidthProperty());
                    paramValueField.prefWidthProperty().bind(paramListField.widthProperty().multiply(0.5).subtract(8.5));
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
                    responseKeyField.prefWidthProperty().bind(responseCaptureScroll.widthProperty().multiply(0.5).subtract(8.5));
                    responseKeyField.maxWidthProperty().bind(responseKeyField.prefWidthProperty());
                    responseKeyField.minWidthProperty().bind(responseKeyField.prefWidthProperty());
                    captureValueField.prefWidthProperty().bind(responseCaptureScroll.widthProperty().multiply(0.5).subtract(8.5));
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

                String payload = newItem[ColumnIndex.PAYLOAD.getIndex()] != null ? newItem[ColumnIndex.PAYLOAD.getIndex()] : "";
                payloadField.setText(CreateEditAPITestTemplate.formatJson(payload, statusLabel));
                String verify = newItem[ColumnIndex.VERIFY_RESPONSE.getIndex()] != null ? newItem[ColumnIndex.VERIFY_RESPONSE.getIndex()] : "";
                verifyResponseField.setText(CreateEditAPITestTemplate.formatJson(verify, statusLabel));
            } else {
                payloadField.clear();
                payloadField.setDisable(false);
                payloadField.setEditable(false);
                verifyResponseField.clear();
                verifyResponseField.setDisable(false);
                verifyResponseField.setEditable(false);
            }
        });

        return additionalContent;
    }

    public VBox createButtonsVBox(Stage primaryStage, Function<Stage, Boolean> checkUnsavedChanges, 
                                  BiFunction<File, Stage, Boolean> saveToFile, 
                                  Function<Stage, Boolean> saveAsToFile) {
        VBox buttonsVBox = new VBox(10);
        buttonsVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 10px;");
        buttonsVBox.setAlignment(Pos.TOP_CENTER);

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
                        app.setModified(true);
                    }
                } else {
                    table.getItems().remove(selectedIndex);
                    table.refresh();
                    app.setModified(true);
                }
            }
            updateButtonStates();
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
                    CreateEditAPITestTemplate.showError("Please select a row with a valid Test ID to delete the test case.");
                    return;
                }
                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                confirmation.setTitle("Confirm Delete Test Case");
                confirmation.setHeaderText("Delete Test Case: " + testId);
                confirmation.setContentText("Are you sure you want to delete all steps for Test ID: " + testId + "?");
                Optional<ButtonType> result = confirmation.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
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
                        CreateEditAPITestTemplate.showError("Invalid test suite " + fileName + ". Upload the valid test suite.");
                        return;
                    }

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
                                    if (!testId.isEmpty() && !CreateEditAPITestTemplate.isValidTestId(testId, testIds, null)) {
                                        String fileName = file.getName().replaceFirst("[.][^.]+$", "");
                                        CreateEditAPITestTemplate.showError("Invalid Test ID '" + testId + "' in row " + (i + 1) + " of " + fileName + ". Must be digits, length <= 5, and unique.");
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
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText("File Loaded");
                    alert.setContentText("Test case loaded successfully from " + file.getAbsolutePath());
                    alert.showAndWait();
                    try {
                        EnvJsonUpdater.updateEnvJsonFromTable(table);
                    } catch (IOException ex) {
                        CreateEditAPITestTemplate.showError("Failed to update env.json: " + ex.getMessage());
                    }
                } catch (IOException ex) {
                    String fileName = file.getName().replaceFirst("[.][^.]+$", "");
                    CreateEditAPITestTemplate.showError("Invalid test suite " + fileName + ". Upload the valid test suite.");
                }
            }
            updateButtonStates();
        });
        loadTestButton.setOnMouseEntered(e -> loadTestButton.setStyle(BUTTON_HOVER_STYLE));
        loadTestButton.setOnMouseExited(e -> loadTestButton.setStyle(BUTTON_STYLE));

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
                    CreateEditAPITestTemplate.showError("Failed to open environment variables window: " + ex.getMessage());
                }
            }
        });
        addEditEnvVarButton.setOnMouseEntered(e -> addEditEnvVarButton.setStyle(BUTTON_HOVER_STYLE));
        addEditEnvVarButton.setOnMouseExited(e -> addEditEnvVarButton.setStyle(BUTTON_STYLE));

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
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText("Collection Imported");
                    alert.setContentText("Postman collection imported successfully from " + file.getAbsolutePath());
                    alert.showAndWait();
                    EnvJsonUpdater.updateEnvJsonFromTable(table);
                } catch (Exception ex) {
                    CreateEditAPITestTemplate.showError("Failed to import Postman collection: " + ex.getMessage());
                }
            }
            updateButtonStates();
        });

        buttonsVBox.getChildren().addAll(
            addStepButton, addAboveButton, addBelowButton, moveUpButton, moveDownButton,
            deleteStepButton, deleteTestCaseButton, saveTestButton, loadTestButton,
            createNewTestButton, addEditEnvVarButton, importCollectionButton
        );

        return buttonsVBox;
    }

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
    
    private void moveFocus(Node fromNode, boolean next) {
        List<Node> traversables = new ArrayList<>();
        addTraversableNodes(fromNode.getScene().getRoot(), traversables);
        int currentIndex = traversables.indexOf(fromNode.getScene().getFocusOwner());
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

    private void addTraversableNodes(Node node, List<Node> list) {
        if (node.isFocusTraversable() && node.isVisible() && !node.isDisabled()) {
            list.add(node);
        }
        if (node instanceof Parent) {
            ((Parent) node).getChildrenUnmodifiable().forEach(child -> addTraversableNodes(child, list));
        }
    }
}