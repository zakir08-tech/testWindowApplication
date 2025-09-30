package com.test.window.gui.com;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UIBuilder {
    private final TableViewManager tableManager;
    private final ButtonActions buttonActions;
    private final FileOperations fileOps;
    private final Stage primaryStage;
    private TextArea payloadField;
    private TextArea verifyResponseField;
    private ScrollPane headerFieldsScroll;

    public UIBuilder(TableViewManager tableManager, ButtonActions buttonActions, FileOperations fileOps, Stage primaryStage) {
        this.tableManager = tableManager;
        this.buttonActions = buttonActions;
        this.fileOps = fileOps;
        this.primaryStage = primaryStage;
    }

    public VBox createMainLayout() {
        TableView<String[]> table = tableManager.getTable();
        VBox buttonsVBox = buttonActions.createButtonsVBox();
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
        payloadField.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
        payloadField.setPrefHeight(200);
        payloadField.setMinHeight(200);
        payloadField.setMaxHeight(200);
        payloadField.setWrapText(true);
        payloadField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            payloadField.setStyle(newVal ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
        });

        payloadField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB) {
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0) {
                    String rawText = payloadField.getText();
                    table.getItems().get(selectedIndex)[7] = rawText;
                    fileOps.setModified(true);
                    table.refresh();
                    String formattedText = tableManager.getTableConfig().formatJson(rawText);
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
        verifyResponseField.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
        verifyResponseField.setPrefHeight(200);
        verifyResponseField.setMinHeight(200);
        verifyResponseField.setMaxHeight(200);
        verifyResponseField.setWrapText(true);
        verifyResponseField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            verifyResponseField.setStyle(newVal ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
        });

        verifyResponseField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB) {
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0) {
                    String rawText = verifyResponseField.getText();
                    table.getItems().get(selectedIndex)[18] = rawText;
                    fileOps.setModified(true);
                    table.refresh();
                    String formattedText = tableManager.getTableConfig().formatJson(rawText);
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
                String testId = newItem[0];
                Set<String> testIds = new HashSet<>();
                for (String[] row : table.getItems()) {
                    if (row[0] != null && !row[0].isEmpty()) {
                        testIds.add(row[0]);
                    }
                }
                boolean isValid = tableManager.getTableConfig().isValidTestId(testId, testIds, testId, selectedIndex, table);
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

                endpointField.setText(newItem[2] != null ? newItem[2] : "");
                String authType = newItem[13] != null ? newItem[13] : "";
                authComboBox.setValue(authType);
                updateAuthFields(authType, authField1, authField2);
                authField1.setText(newItem[14] != null ? newItem[14] : "");
                authField2.setText(newItem[15] != null ? newItem[15] : "");

                int start = selectedIndex;
                while (start >= 0 && (table.getItems().get(start)[0] == null || table.getItems().get(start)[0].isEmpty())) {
                    start--;
                }
                if (start < 0) start = 0;

                List<Integer> rowIndices = new ArrayList<>();
                for (int i = start; i < table.getItems().size(); i++) {
                    String[] r = table.getItems().get(i);
                    if (i > start && r[0] != null && !r[0].isEmpty()) break;
                    rowIndices.add(i);
                }

                for (Integer rowIndex : rowIndices) {
                    String[] row = table.getItems().get(rowIndex);
                    TextField headerKeyField = new TextField(row[3] != null ? row[3] : "");
                    headerKeyField.setPromptText("Header Key");
                    headerKeyField.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
                    headerKeyField.setPrefHeight(Constants.TEXT_FIELD_HEIGHT);
                    headerKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        headerKeyField.setStyle(newVal2 ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
                    });

                    TextField headerValueField = new TextField(row[4] != null ? row[4] : "");
                    headerValueField.setPromptText("Header Value");
                    headerValueField.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
                    headerValueField.setPrefHeight(Constants.TEXT_FIELD_HEIGHT);
                    headerValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        headerValueField.setStyle(newVal2 ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
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
                        table.getItems().get(rowIndex)[3] = newVal2.trim();
                        fileOps.setModified(true);
                        table.refresh();
                    });

                    headerValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[4] = newVal2.trim();
                        fileOps.setModified(true);
                        table.refresh();
                    });

                    headerFieldsVBox.getChildren().add(headerPair);

                    TextField modifyKeyField = new TextField(row[9] != null ? row[9] : "");
                    modifyKeyField.setPromptText("Modify Payload Key");
                    modifyKeyField.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
                    modifyKeyField.setPrefHeight(Constants.TEXT_FIELD_HEIGHT);
                    modifyKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        modifyKeyField.setStyle(newVal2 ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
                    });

                    TextField modifyValueField = new TextField(row[10] != null ? row[10] : "");
                    modifyValueField.setPromptText("Modify Payload Value");
                    modifyValueField.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
                    modifyValueField.setPrefHeight(Constants.TEXT_FIELD_HEIGHT);
                    modifyValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        modifyValueField.setStyle(newVal2 ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
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
                        table.getItems().get(rowIndex)[9] = newVal2.trim();
                        fileOps.setModified(true);
                        table.refresh();
                    });

                    modifyValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[10] = newVal2.trim();
                        fileOps.setModified(true);
                        table.refresh();
                    });

                    modifyPayloadVBox.getChildren().add(modifyPair);

                    TextField paramKeyField = new TextField(row[5] != null ? row[5] : "");
                    paramKeyField.setPromptText("Parameter Key");
                    paramKeyField.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
                    paramKeyField.setPrefHeight(Constants.TEXT_FIELD_HEIGHT);
                    paramKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        paramKeyField.setStyle(newVal2 ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
                    });

                    TextField paramValueField = new TextField(row[6] != null ? row[6] : "");
                    paramValueField.setPromptText("Parameter Value");
                    paramValueField.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
                    paramValueField.setPrefHeight(Constants.TEXT_FIELD_HEIGHT);
                    paramValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        paramValueField.setStyle(newVal2 ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
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
                        table.getItems().get(rowIndex)[5] = newVal2.trim();
                        fileOps.setModified(true);
                        table.refresh();
                    });

                    paramValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[6] = newVal2.trim();
                        fileOps.setModified(true);
                        table.refresh();
                    });

                    paramFieldsVBox.getChildren().add(paramPair);

                    TextField responseKeyField = new TextField(row[11] != null ? row[11] : "");
                    responseKeyField.setPromptText("Response Key Name");
                    responseKeyField.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
                    responseKeyField.setPrefHeight(Constants.TEXT_FIELD_HEIGHT);
                    responseKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        responseKeyField.setStyle(newVal2 ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
                    });

                    TextField captureValueField = new TextField(row[12] != null ? row[12] : "");
                    captureValueField.setPromptText("Capture Value (env var)");
                    captureValueField.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
                    captureValueField.setPrefHeight(Constants.TEXT_FIELD_HEIGHT);
                    captureValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        captureValueField.setStyle(newVal2 ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
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
                        table.getItems().get(rowIndex)[11] = newVal2.trim();
                        fileOps.setModified(true);
                        table.refresh();
                    });

                    captureValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[12] = newVal2.trim();
                        fileOps.setModified(true);
                        table.refresh();
                    });

                    responseCaptureVBox.getChildren().add(responsePair);
                }

                String payload = newItem[7] != null ? newItem[7] : "";
                payloadField.setText(tableManager.getTableConfig().formatJson(payload));
                String verify = newItem[18] != null ? newItem[18] : "";
                verifyResponseField.setText(tableManager.getTableConfig().formatJson(verify));
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
                table.getItems().get(selectedIndex)[7] = newVal;
                fileOps.setModified(true);
                table.refresh();
                String formattedText = tableManager.getTableConfig().formatJson(newVal);
                if (!formattedText.equals(newVal)) {
                    Platform.runLater(() -> payloadField.setText(formattedText));
                }
            }
        });

        verifyResponseField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[18] = newVal;
                fileOps.setModified(true);
                table.refresh();
                String formattedText = tableManager.getTableConfig().formatJson(newVal);
                if (!formattedText.equals(newVal)) {
                    Platform.runLater(() -> verifyResponseField.setText(formattedText));
                }
            }
        });

        ScrollPane scrollPane = new ScrollPane(additionalContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #2E2E2E;");

        VBox mainLayout = new VBox(10, tableWithButtons, textFieldsBox, scrollPane);
        mainLayout.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 10px; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
        mainLayout.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        updateButtonStates(table);

        return mainLayout;
    }

    private HBox createTextFieldsBox(TableView<String[]> table, GridPane additionalFields) {
        TextField endpointField = new TextField();
        endpointField.setPromptText("End-Point");
        endpointField.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
        endpointField.setPrefHeight(Constants.TEXT_FIELD_HEIGHT);
        endpointField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            endpointField.setStyle(newVal ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
        });
        endpointField.setDisable(true);
        endpointField.prefWidthProperty().bind(additionalFields.widthProperty().multiply(0.35));
        endpointField.maxWidthProperty().bind(endpointField.prefWidthProperty());
        endpointField.minWidthProperty().bind(endpointField.prefWidthProperty());

        ComboBox<String> authComboBox = new ComboBox<>(Constants.AUTH_OPTIONS);
        authComboBox.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
        authComboBox.setPrefHeight(Constants.TEXT_FIELD_HEIGHT);
        authComboBox.setPromptText("Authorization");
        authComboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            authComboBox.setStyle(newVal ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
        });
        authComboBox.setDisable(true);
        authComboBox.setPrefWidth(135);
        authComboBox.maxWidthProperty().bind(authComboBox.prefWidthProperty());
        authComboBox.minWidthProperty().bind(authComboBox.prefWidthProperty());

        TextField authField1 = new TextField();
        authField1.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
        authField1.setPrefHeight(Constants.TEXT_FIELD_HEIGHT);
        authField1.focusedProperty().addListener((obs, oldVal, newVal) -> {
            authField1.setStyle(newVal ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
        });
        authField1.setDisable(true);
        authField1.prefWidthProperty().bind(additionalFields.widthProperty().multiply(0.14).subtract(50)); // Reduced from 0.2 to 0.14 (30% less)
        authField1.maxWidthProperty().bind(authField1.prefWidthProperty());
        authField1.minWidthProperty().bind(authField1.prefWidthProperty());

        TextField authField2 = new TextField();
        authField2.setStyle(Constants.FIELD_STYLE_UNFOCUSED);
        authField2.setPrefHeight(Constants.TEXT_FIELD_HEIGHT);
        authField2.focusedProperty().addListener((obs, oldVal, newVal) -> {
            authField2.setStyle(newVal ? Constants.FIELD_STYLE_FOCUSED : Constants.FIELD_STYLE_UNFOCUSED);
        });
        authField2.setDisable(true);
        authField2.prefWidthProperty().bind(additionalFields.widthProperty().multiply(0.14).subtract(50)); // Reduced from 0.2 to 0.14 (30% less)
        authField2.maxWidthProperty().bind(authField2.prefWidthProperty());
        authField2.minWidthProperty().bind(authField2.prefWidthProperty());

        // Update auth fields based on ComboBox selection
        authComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateAuthFields(newVal, authField1, authField2);
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[13] = newVal != null ? newVal : "";
                fileOps.setModified(true);
                table.refresh();
            }
        });

        // Update table when authField1 changes
        authField1.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[14] = newVal;
                fileOps.setModified(true);
                table.refresh();
            }
        });

        // Update table when authField2 changes
        authField2.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().get(selectedIndex)[15] = newVal;
                fileOps.setModified(true);
                table.refresh();
            }
        });

        // Update authComboBox and auth fields when table row selection changes
        table.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = newVal.intValue();
            if (selectedIndex >= 0 && selectedIndex < table.getItems().size()) {
                String[] selectedRow = table.getItems().get(selectedIndex);
                String authValue = selectedRow[13] != null ? selectedRow[13] : "";
                authComboBox.setValue(authValue);
                updateAuthFields(authValue, authField1, authField2);
                authField1.setText(selectedRow[14] != null ? selectedRow[14] : "");
                authField2.setText(selectedRow[15] != null ? selectedRow[15] : "");
                endpointField.setText(selectedRow[2] != null ? selectedRow[2] : "");
                authComboBox.setDisable(false);
            } else {
                authComboBox.setValue("");
                authComboBox.setDisable(true);
                authField1.setText("");
                authField1.setDisable(true);
                authField2.setText("");
                authField2.setDisable(true);
                endpointField.setText("");
                endpointField.setDisable(true);
            }
        });

        HBox textFieldsBox = new HBox(10, endpointField, authComboBox, authField1, authField2, tableManager.getStatusLabel());
        textFieldsBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
        HBox.setHgrow(endpointField, Priority.NEVER);
        HBox.setHgrow(authComboBox, Priority.NEVER);
        HBox.setHgrow(authField1, Priority.NEVER);
        HBox.setHgrow(authField2, Priority.NEVER);
        HBox.setHgrow(tableManager.getStatusLabel(), Priority.ALWAYS);
        return textFieldsBox;
    }
    
    private void updateAuthFields(String authType, TextField authField1, TextField authField2) {
        if ("Basic Auth".equals(authType)) {
            authField1.setPromptText("Username");
            authField1.setDisable(false);
            authField2.setPromptText("Password");
            authField2.setDisable(false);
        } else if ("Bearer Token".equals(authType)) {
            authField1.setPromptText("Token");
            authField1.setDisable(false);
            authField2.setPromptText("");
            authField2.setDisable(true);
            authField2.clear();
        } else {
            authField1.setPromptText("");
            authField1.setDisable(true);
            authField1.clear();
            authField2.setPromptText("");
            authField2.setDisable(true);
            authField2.clear();
        }
    }

    private void updateButtonStates(TableView<String[]> table) {
        boolean isTableEmpty = table.getItems().isEmpty();
        int selectedIndex = table.getSelectionModel().getSelectedIndex();
        int lastIndex = table.getItems().size() - 1;

        VBox buttonsVBox = buttonActions.createButtonsVBox();
        Button addAboveButton = (Button) buttonsVBox.getChildren().get(1);
        Button addBelowButton = (Button) buttonsVBox.getChildren().get(2);
        Button moveUpButton = (Button) buttonsVBox.getChildren().get(3);
        Button moveDownButton = (Button) buttonsVBox.getChildren().get(4);
        Button deleteStepButton = (Button) buttonsVBox.getChildren().get(5);
        Button deleteTestCaseButton = (Button) buttonsVBox.getChildren().get(6);
        Button saveTestButton = (Button) buttonsVBox.getChildren().get(7);
        Button createNewTestButton = (Button) buttonsVBox.getChildren().get(9);

        addAboveButton.setDisable(isTableEmpty);
        addBelowButton.setDisable(isTableEmpty);
        moveUpButton.setDisable(isTableEmpty || selectedIndex <= 0);
        moveDownButton.setDisable(isTableEmpty || selectedIndex < 0 || selectedIndex >= lastIndex);
        deleteStepButton.setDisable(isTableEmpty);
        deleteTestCaseButton.setDisable(isTableEmpty);
        saveTestButton.setDisable(isTableEmpty);
        createNewTestButton.setDisable(isTableEmpty);
    }
}