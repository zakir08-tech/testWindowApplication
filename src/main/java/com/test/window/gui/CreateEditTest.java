package com.test.window.gui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class CreateEditTest extends Application {

    private static File lastUsedFile = null;
    private static boolean isTableModified = false;
    private static List<String> previousElementNames = new ArrayList<>();

    private TableView<TestCaseRow> table1;
    private TableColumn<TestCaseRow, String> testIdColumn;
    private TableColumn<TestCaseRow, String> testStepColumn;
    private TableColumn<TestCaseRow, String> testActionColumn;
    private TableColumn<TestCaseRow, String> testElementColumn;
    private TableColumn<TestCaseRow, String> testDataColumn;
    private TableColumn<TestCaseRow, String> descriptionColumn;

    private TableView<ElementRow> table2;
    private TableColumn<ElementRow, String> elementNameColumn;
    private TableColumn<ElementRow, String> automationIdColumn;
    private TableColumn<ElementRow, String> nameColumn;
    private TableColumn<ElementRow, String> xpathColumn;

    private Button addRowButton;
    private Button addAboveButton;
    private Button addBelowButton;
    private Button deleteRowButton;
    private Button moveUpButton1;
    private Button moveDownButton1;
    private Button saveJsonButton;
    private Button loadJsonButton;
    private Button clearTableButton;
    private Button addElementButton;
    private Button moveUpButton2;
    private Button moveDownButton2;
    private Button deleteElementButton;

    private ObservableList<TestCaseRow> table1Data = FXCollections.observableArrayList();
    private ObservableList<ElementRow> table2Data = FXCollections.observableArrayList();
    private ComboBox<String> testActionComboBox = new ComboBox<>(FXCollections.observableArrayList("", "APP_ID", "SET", "CLICK", "TAKE_SCREENSHOT"));
    private ComboBox<String> testElementComboBox = new ComboBox<>();

    @Override
    public void start(Stage primaryStage) {
        // Initialize UI components
        Label titleLabel = new Label("Create Test Steps");
        titleLabel.setStyle("-fx-font: bold 12pt 'Arial'; -fx-text-fill: #C8C8C8; -fx-padding: 10 0 0 0;");

        // Table 1 setup
        table1 = new TableView<>();
        table1.setStyle("-fx-background-color: #323232; -fx-control-inner-background: #323232; -fx-table-cell-border-color: #444444; -fx-font: 12pt 'Arial';");
        table1.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        testIdColumn = new TableColumn<>("Test Id");
        testIdColumn.setPrefWidth(60);
        testIdColumn.setMaxWidth(60);
        testIdColumn.setMinWidth(60);
        testStepColumn = new TableColumn<>("Test Step");
        testStepColumn.setPrefWidth(80);
        testStepColumn.setMaxWidth(80);
        testStepColumn.setMinWidth(80);
        testActionColumn = new TableColumn<>("Test Action");
        testActionColumn.setPrefWidth(100);
        testElementColumn = new TableColumn<>("Test Element");
        testElementColumn.setPrefWidth(120);
        testDataColumn = new TableColumn<>("Test Data");
        testDataColumn.setPrefWidth(150);
        descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setPrefWidth(200);

        table1.getColumns().addAll(testIdColumn, testStepColumn, testActionColumn, testElementColumn, testDataColumn, descriptionColumn);

        // Table 2 setup
        table2 = new TableView<>();
        table2.setStyle("-fx-background-color: #323232; -fx-control-inner-background: #323232; -fx-table-cell-border-color: #444444; -fx-font: 12pt 'Arial';");
        table2.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        elementNameColumn = new TableColumn<>("Element Name");
        elementNameColumn.setPrefWidth(120);
        automationIdColumn = new TableColumn<>("Automation ID");
        automationIdColumn.setPrefWidth(90);
        nameColumn = new TableColumn<>("Name");
        nameColumn.setPrefWidth(90);
        xpathColumn = new TableColumn<>("Xpath");
        xpathColumn.setPrefWidth(300);

        table2.getColumns().addAll(elementNameColumn, automationIdColumn, nameColumn, xpathColumn);

        // Button setup
        addRowButton = createButton("Add Row", "Add a new test step at the end of the test case table");
        addAboveButton = createButton("Add Above", "Insert a new test step above the selected row");
        addBelowButton = createButton("Add Below", "Insert a new test step below the selected row");
        deleteRowButton = createButton("Delete Row", "Delete the selected test step");
        moveUpButton1 = createButton("Move Up", "Move the selected test step up");
        moveDownButton1 = createButton("Move Down", "Move the selected test step down");
        saveJsonButton = createButton("Save to JSON", "Save the tables to a JSON file");
        loadJsonButton = createButton("Load from JSON", "Load table data from a JSON file");
        clearTableButton = createButton("Clear Test", "Clear all data from both tables");
        addElementButton = createButton("Add Element", "Add a new element to the element table");
        moveUpButton2 = createButton("Move Up", "Move the selected element up");
        moveDownButton2 = createButton("Move Down", "Move the selected element down");
        deleteElementButton = createButton("Delete Element", "Delete the selected element");

        // Layout setup
        VBox tablePanel = new VBox(table1, table2);
        tablePanel.setStyle("-fx-background-color: #1E1E1E;");
        VBox.setVgrow(table1, Priority.ALWAYS);
        VBox.setVgrow(table2, Priority.ALWAYS);
        table1.setPrefHeight(0.7 * 600); // Approximate 70% height
        table2.setPrefHeight(0.3 * 600); // Approximate 30% height

        VBox buttonPanel = new VBox(10, addRowButton, addAboveButton, addBelowButton, deleteRowButton, moveUpButton1, moveDownButton1,
                saveJsonButton, loadJsonButton, clearTableButton, new Pane(), addElementButton, moveUpButton2, moveDownButton2, deleteElementButton);
        buttonPanel.setStyle("-fx-background-color: #1E1E1E; -fx-padding: 10;");
        buttonPanel.setPrefWidth(120);
        new Pane().setPrefHeight(200); // Spacer

        HBox mainLayout = new HBox(tablePanel, buttonPanel);
        HBox.setHgrow(tablePanel, Priority.ALWAYS);
        mainLayout.setStyle("-fx-background-color: #1E1E1E;");

        VBox root = new VBox(10, titleLabel, mainLayout);
        root.setStyle("-fx-background-color: #1E1E1E;");

        // Scene and stage setup
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Test Table Application");
        primaryStage.setMaximized(true);
        primaryStage.setResizable(false);

        // Initialize tables and buttons
        initializeTables();
        initializeButtons();

        // Window close handler
        primaryStage.setOnCloseRequest(event -> {
            if (isTableModified) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unsaved Changes");
                alert.setHeaderText("You have unsaved changes. Would you like to save before exiting?");
                alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.YES) {
                    if (lastUsedFile != null) {
                        if (saveFile(lastUsedFile)) {
                            primaryStage.close();
                        }
                    } else {
                        if (showSaveDialog(null)) {
                            primaryStage.close();
                        }
                    }
                    event.consume();
                } else if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                    event.consume();
                }
            }
        });

        primaryStage.show();
    }

    private Button createButton(String text, String tooltip) {
        Button button = new Button(text);
        button.setPrefSize(100, 20);
        button.setStyle("-fx-background-color: #32A852; -fx-text-fill: black; -fx-font: bold 10pt 'Arial'; -fx-cursor: hand;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #288A48; -fx-text-fill: black; -fx-font: bold 10pt 'Arial'; -fx-cursor: hand;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #32A852; -fx-text-fill: black; -fx-font: bold 10pt 'Arial'; -fx-cursor: hand;"));
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private void initializeTables() {
        // Table 1 setup
        testIdColumn.setCellValueFactory(cellData -> cellData.getValue().testIdProperty());
        testStepColumn.setCellValueFactory(cellData -> cellData.getValue().testStepProperty());
        testActionColumn.setCellValueFactory(cellData -> cellData.getValue().testActionProperty());
        testElementColumn.setCellValueFactory(cellData -> cellData.getValue().testElementProperty());
        testDataColumn.setCellValueFactory(cellData -> cellData.getValue().testDataProperty());
        descriptionColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());

        testIdColumn.setCellFactory(column -> new TextFieldTableCell<TestCaseRow, String>() {
            @Override
            public void startEdit() {
                super.startEdit();
                TextField textField = (TextField) getGraphic();
                textField.setStyle("-fx-background-color: #3C3C3C; -fx-text-fill: #C8C8C8; -fx-font: 12pt 'Arial';");
                textField.textProperty().addListener((obs, oldValue, newValue) -> {
                    if (!newValue.isEmpty() && !newValue.equals("#") && !newValue.matches("[0-9]*")) {
                        textField.setText(oldValue);
                    }
                });
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setStyle("-fx-text-fill: #C8C8C8; -fx-background-color: #323232; -fx-alignment: center-left;");
                } else {
                    setText(null);
                    setStyle("-fx-background-color: #323232;");
                }
            }

            @Override
            public void commitEdit(String newValue) {
                TestCaseRow row = getTableRow().getItem();
                int rowIndex = getTableRow().getIndex();
                if (!newValue.isEmpty() && !newValue.equals("#")) {
                    for (int i = 0; i < table1Data.size(); i++) {
                        if (i != rowIndex && newValue.equals(table1Data.get(i).getTestId())) {
                            showAlert(Alert.AlertType.ERROR, "Duplicate Error", "Duplicate Test Id not allowed");
                            return;
                        }
                    }
                }
                super.commitEdit(newValue);
                row.setTestId(newValue); // Ensure the value is saved
                isTableModified = true;
                updateTestStepNumbers();
            }
        });

        testStepColumn.setEditable(false);
        testStepColumn.setCellFactory(column -> new TableCell<TestCaseRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setStyle("-fx-text-fill: #C8C8C8; -fx-background-color: #323232; -fx-alignment: center-left;");
                } else {
                    setText(null);
                    setStyle("-fx-background-color: #323232;");
                }
            }
        });

        testActionColumn.setCellFactory(column -> new ComboBoxTableCell<TestCaseRow, String>(testActionComboBox.getItems()) {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setStyle("-fx-text-fill: orange; -fx-background-color: #323232; -fx-alignment: center-left;");
                } else {
                    setText(null);
                    setStyle("-fx-background-color: #323232;");
                }
            }
        });
        testActionColumn.setOnEditCommit(event -> {
            TestCaseRow row = event.getRowValue();
            row.setTestAction(event.getNewValue());
            isTableModified = true;
            table1.refresh();
        });

        testElementColumn.setCellFactory(column -> new ComboBoxTableCell<TestCaseRow, String>(testElementComboBox.getItems()) {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setStyle("-fx-text-fill: #FF6666; -fx-background-color: #323232; -fx-alignment: center-left;");
                } else {
                    setText(null);
                    setStyle("-fx-background-color: #323232;");
                }
            }
        });
        testElementColumn.setOnEditCommit(event -> {
            TestCaseRow row = event.getRowValue();
            row.setTestElement(event.getNewValue());
            isTableModified = true;
            table1.refresh();
        });

        testDataColumn.setCellFactory(column -> new TextFieldTableCell<TestCaseRow, String>() {
            @Override
            public void startEdit() {
                super.startEdit();
                TextField textField = (TextField) getGraphic();
                textField.setStyle("-fx-background-color: #3C3C3C; -fx-text-fill: #C8C8C8; -fx-font: 12pt 'Arial';");
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setStyle("-fx-text-fill: #87CEFA; -fx-background-color: #323232; -fx-alignment: center-left;");
                } else {
                    setText(null);
                    setStyle("-fx-background-color: #323232;");
                }
            }
        });
        testDataColumn.setOnEditCommit(event -> {
            TestCaseRow row = event.getRowValue();
            row.setTestData(event.getNewValue());
            isTableModified = true;
            table1.refresh();
        });

        descriptionColumn.setCellFactory(column -> new TextFieldTableCell<TestCaseRow, String>() {
            @Override
            public void startEdit() {
                super.startEdit();
                TextField textField = (TextField) getGraphic();
                textField.setStyle("-fx-background-color: #3C3C3C; -fx-text-fill: #C8C8C8; -fx-font: 12pt 'Arial';");
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setStyle("-fx-text-fill: #C8C8C8; -fx-background-color: #323232; -fx-alignment: center-left;");
                } else {
                    setText(null);
                    setStyle("-fx-background-color: #323232;");
                }
            }
        });
        descriptionColumn.setOnEditCommit(event -> {
            TestCaseRow row = event.getRowValue();
            row.setDescription(event.getNewValue());
            isTableModified = true;
            table1.refresh();
        });

        table1.setItems(table1Data);
        table1.setEditable(true);
        table1.getStylesheets().add(
            "data:text/css," +
            ".table-view .column-header-background {" +
            "    -fx-background-color: #2A2A2A;" +
            "}" +
            ".table-view .column-header {" +
            "    -fx-background-color: #2A2A2A;" +
            "}" +
            ".table-view .column-header .label {" +
            "    -fx-text-fill: #C8C8C8;" +
            "    -fx-font: bold 12pt 'Arial';" +
            "    -fx-alignment: center-left;" +
            "}" +
            ".table-row-cell:selected {" +
            "    -fx-background-color: #4682B4;" +
            "    -fx-text-fill: white;" +
            "}" +
            ".table-view .filler {" +
            "    -fx-background-color: #2A2A2A;" +
            "}" +
            ".table-view .show-hide-column {" +
            "    -fx-background-color: #FFD700;" +
            "}"
        );

        // Table 2 setup
        elementNameColumn.setCellValueFactory(cellData -> cellData.getValue().elementNameProperty());
        automationIdColumn.setCellValueFactory(cellData -> cellData.getValue().automationIdProperty());
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        xpathColumn.setCellValueFactory(cellData -> cellData.getValue().xpathProperty());

        elementNameColumn.setCellFactory(column -> new TextFieldTableCell<ElementRow, String>() {
            @Override
            public void startEdit() {
                super.startEdit();
                TextField textField = (TextField) getGraphic();
                textField.setStyle("-fx-background-color: #3C3C3C; -fx-text-fill: #C8C8C8; -fx-font: 12pt 'Arial';");
                textField.textProperty().addListener((obs, oldValue, newValue) -> {
                    if (!newValue.isEmpty() && !newValue.matches("[a-zA-Z][a-zA-Z0-9-_]*")) {
                        textField.setText(oldValue);
                    }
                });
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setStyle("-fx-text-fill: #C8C8C8; -fx-background-color: #323232; -fx-alignment: center-left;");
                } else {
                    setText(null);
                    setStyle("-fx-background-color: #323232;");
                }
            }

            @Override
            public void commitEdit(String newValue) {
                ElementRow row = getTableRow().getItem();
                int rowIndex = getTableRow().getIndex();
                if (!newValue.isEmpty()) {
                    for (int i = 0; i < table2Data.size(); i++) {
                        if (i != rowIndex && newValue.equalsIgnoreCase(table2Data.get(i).getElementName())) {
                            showAlert(Alert.AlertType.ERROR, "Duplicate Error", "Duplicate Element Name not allowed (case-insensitive)");
                            return;
                        }
                    }
                }
                String oldValue = row.getElementName();
                super.commitEdit(newValue);
                row.setElementName(newValue); // Ensure the value is saved
                if (!oldValue.isEmpty() && !oldValue.equals(newValue)) {
                    for (TestCaseRow testRow : table1Data) {
                        if (testRow.getTestElement().equals(oldValue)) {
                            testRow.setTestElement(newValue);
                        }
                    }
                }
                while (previousElementNames.size() <= rowIndex) {
                    previousElementNames.add("");
                }
                previousElementNames.set(rowIndex, newValue);
                updateTestElementComboBox();
                isTableModified = true;
            }
        });

        automationIdColumn.setCellFactory(column -> new TextFieldTableCell<ElementRow, String>() {
            @Override
            public void startEdit() {
                super.startEdit();
                TextField textField = (TextField) getGraphic();
                textField.setStyle("-fx-background-color: #3C3C3C; -fx-text-fill: #C8C8C8; -fx-font: 12pt 'Arial';");
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setStyle("-fx-text-fill: #C8C8C8; -fx-background-color: #323232; -fx-alignment: center-left;");
                } else {
                    setText(null);
                    setStyle("-fx-background-color: #323232;");
                }
            }
        });
        automationIdColumn.setOnEditCommit(event -> {
            ElementRow row = event.getRowValue();
            row.setAutomationId(event.getNewValue());
            isTableModified = true;
            table2.refresh();
        });

        nameColumn.setCellFactory(column -> new TextFieldTableCell<ElementRow, String>() {
            @Override
            public void startEdit() {
                super.startEdit();
                TextField textField = (TextField) getGraphic();
                textField.setStyle("-fx-background-color: #3C3C3C; -fx-text-fill: #C8C8C8; -fx-font: 12pt 'Arial';");
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setStyle("-fx-text-fill: #C8C8C8; -fx-background-color: #323232; -fx-alignment: center-left;");
                } else {
                    setText(null);
                    setStyle("-fx-background-color: #323232;");
                }
            }
        });
        nameColumn.setOnEditCommit(event -> {
            ElementRow row = event.getRowValue();
            row.setName(event.getNewValue());
            isTableModified = true;
            table2.refresh();
        });

        xpathColumn.setCellFactory(column -> new TextFieldTableCell<ElementRow, String>() {
            @Override
            public void startEdit() {
                super.startEdit();
                TextField textField = (TextField) getGraphic();
                textField.setStyle("-fx-background-color: #3C3C3C; -fx-text-fill: #C8C8C8; -fx-font: 12pt 'Arial';");
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setStyle("-fx-text-fill: #C8C8C8; -fx-background-color: #323232; -fx-alignment: center-left;");
                } else {
                    setText(null);
                    setStyle("-fx-background-color: #323232;");
                }
            }
        });
        xpathColumn.setOnEditCommit(event -> {
            ElementRow row = event.getRowValue();
            row.setXpath(event.getNewValue());
            isTableModified = true;
            table2.refresh();
        });

        table2.setItems(table2Data);
        table2.setEditable(true);
        table2.getStylesheets().add(
            "data:text/css," +
            ".table-view .column-header-background {" +
            "    -fx-background-color: #2A2A2A;" +
            "}" +
            ".table-view .column-header {" +
            "    -fx-background-color: #2A2A2A;" +
            "}" +
            ".table-view .column-header .label {" +
            "    -fx-text-fill: #C8C8C8;" +
            "    -fx-font: bold 12pt 'Arial';" +
            "    -fx-alignment: center-left;" +
            "}" +
            ".table-row-cell:selected {" +
            "    -fx-background-color: #4682B4;" +
            "    -fx-text-fill: white;" +
            "}" +
            ".table-view .filler {" +
            "    -fx-background-color: #2A2A2A;" +
            "}" +
            ".table-view .show-hide-column {" +
            "    -fx-background-color: #FFD700;" +
            "}"
        );

        // Table listeners
        table1Data.addListener((ListChangeListener<TestCaseRow>) c -> {
            updateTestStepNumbers();
            isTableModified = true;
        });

        table2Data.addListener((ListChangeListener<ElementRow>) c -> {
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved()) {
                    updateTestElementComboBox();
                    isTableModified = true;
                }
            }
        });

        // Mouse listeners for stopping editing
        table1.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> stopEditing());
        table2.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> stopEditing());

        // Initialize testElementComboBox
        testElementComboBox.getItems().add("");
        testElementComboBox.setStyle("-fx-background-color: #3C3C3C; -fx-text-fill: #C8C8C8; -fx-font: 12pt 'Arial';");
        updateTestElementComboBox();
    }

    private void initializeButtons() {
        addRowButton.setOnAction(e -> addRow());
        addAboveButton.setOnAction(e -> addAbove());
        addBelowButton.setOnAction(e -> addBelow());
        deleteRowButton.setOnAction(e -> deleteRow());
        moveUpButton1.setOnAction(e -> moveUp1());
        moveDownButton1.setOnAction(e -> moveDown1());
        saveJsonButton.setOnAction(e -> saveJson());
        loadJsonButton.setOnAction(e -> loadJson());
        clearTableButton.setOnAction(e -> clearTable());
        addElementButton.setOnAction(e -> addElement());
        moveUpButton2.setOnAction(e -> moveUp2());
        moveDownButton2.setOnAction(e -> moveDown2());
        deleteElementButton.setOnAction(e -> deleteElement());
    }

    private void stopEditing() {
        // Cancel editing for table1
        if (table1.getEditingCell() != null) {
            table1.edit(-1, null); // Cancel editing by setting to an invalid row
        }
        // Cancel editing for table2
        if (table2.getEditingCell() != null) {
            table2.edit(-1, null); // Cancel editing by setting to an invalid row
        }
    }

    private void updateTestStepNumbers() {
        int step = 1;
        for (int i = 0; i < table1Data.size(); i++) {
            String testId = table1Data.get(i).getTestId();
            if (!testId.isEmpty() && !testId.equals("#")) {
                step = 1;
            }
            table1Data.get(i).setTestStep(String.valueOf(step));
            step++;
        }
        table1.refresh();
    }

    private void updateTestElementComboBox() {
        testElementComboBox.getItems().clear();
        testElementComboBox.getItems().add("");
        Set<String> uniqueElements = new TreeSet<>();
        for (ElementRow row : table2Data) {
            String elementName = row.getElementName();
            if (!elementName.isEmpty()) {
                uniqueElements.add(elementName);
            }
        }
        testElementComboBox.getItems().addAll(uniqueElements);
    }

    private void addRow() {
        stopEditing();
        int stepNumber = table1Data.size() + 1;
        table1Data.add(new TestCaseRow("", String.valueOf(stepNumber), "", "", "", ""));
        int newRowIndex = table1Data.size() - 1;
        table1.getSelectionModel().select(newRowIndex);
        table1.scrollTo(newRowIndex);
        updateTestStepNumbers();
        isTableModified = true;
    }

    private void addAbove() {
        stopEditing();
        if (table1Data.isEmpty()) return;
        int selectedRow = table1.getSelectionModel().getSelectedIndex();
        if (selectedRow == 0) return;
        int insertIndex = selectedRow == -1 ? table1Data.size() : selectedRow;
        table1Data.add(insertIndex, new TestCaseRow("", String.valueOf(insertIndex + 1), "", "", "", ""));
        table1.getSelectionModel().select(insertIndex);
        table1.scrollTo(insertIndex);
        updateTestStepNumbers();
        isTableModified = true;
    }

    private void addBelow() {
        stopEditing();
        if (table1Data.isEmpty()) return;
        int selectedRow = table1.getSelectionModel().getSelectedIndex();
        int insertIndex = selectedRow == -1 ? table1Data.size() : selectedRow + 1;
        table1Data.add(insertIndex, new TestCaseRow("", String.valueOf(insertIndex + 1), "", "", "", ""));
        table1.getSelectionModel().select(insertIndex);
        table1.scrollTo(insertIndex);
        updateTestStepNumbers();
        isTableModified = true;
    }

    private void deleteRow() {
        stopEditing();
        int selectedRow = table1.getSelectionModel().getSelectedIndex();
        if (selectedRow == -1) {
            showAlert(Alert.AlertType.WARNING, "No Row Selected", "Please select a row to delete");
            return;
        }
        String testId = table1Data.get(selectedRow).getTestId();
        if (!testId.isEmpty() && !testId.equals("#")) {
            showAlert(Alert.AlertType.ERROR, "Deletion Error", "Cannot delete row with a Test Id");
            return;
        }
        table1Data.remove(selectedRow);
        if (!table1Data.isEmpty()) {
            int newSelection = Math.min(selectedRow, table1Data.size() - 1);
            table1.getSelectionModel().select(newSelection);
            table1.scrollTo(newSelection);
        }
        updateTestStepNumbers();
        isTableModified = true;
    }

    private void moveUp1() {
        stopEditing();
        int selectedRow = table1.getSelectionModel().getSelectedIndex();
        if (selectedRow == -1) {
            showAlert(Alert.AlertType.WARNING, "No Row Selected", "Please select a row to move");
            return;
        }
        if (selectedRow == 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Action", "Cannot move the first row of the table");
            return;
        }
        int testCaseStart = selectedRow;
        for (int i = selectedRow; i >= 0; i--) {
            String testId = table1Data.get(i).getTestId();
            if (!testId.isEmpty() && !testId.equals("#")) {
                testCaseStart = i;
                break;
            }
            if (i == 0) testCaseStart = 0;
        }
        if (selectedRow > testCaseStart) {
            Collections.swap(table1Data, selectedRow, selectedRow - 1);
            table1.getSelectionModel().select(selectedRow - 1);
            table1.scrollTo(selectedRow - 1);
            updateTestStepNumbers();
            isTableModified = true;
        }
    }

    private void moveDown1() {
        stopEditing();
        int selectedRow = table1.getSelectionModel().getSelectedIndex();
        if (selectedRow == -1) {
            showAlert(Alert.AlertType.WARNING, "No Row Selected", "Please select a row to move");
            return;
        }
        if (selectedRow == table1Data.size() - 1) {
            showAlert(Alert.AlertType.WARNING, "Invalid Action", "Cannot move the last row of the table");
            return;
        }
        Collections.swap(table1Data, selectedRow, selectedRow + 1);
        table1.getSelectionModel().select(selectedRow + 1);
        table1.scrollTo(selectedRow + 1);
        updateTestStepNumbers();
        isTableModified = true;
    }

    private void addElement() {
        stopEditing();
        table2Data.add(new ElementRow("", "", "", ""));
        int newRowIndex = table2Data.size() - 1;
        table2.getSelectionModel().select(newRowIndex);
        table2.scrollTo(newRowIndex);
        isTableModified = true;
    }

    private void moveUp2() {
        stopEditing();
        int selectedRow = table2.getSelectionModel().getSelectedIndex();
        if (selectedRow == -1) {
            showAlert(Alert.AlertType.WARNING, "No Row Selected", "Please select a row to move");
            return;
        }
        if (selectedRow == 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Action", "Cannot move the first row of the table");
            return;
        }
        Collections.swap(table2Data, selectedRow, selectedRow - 1);
        Collections.swap(previousElementNames, selectedRow, selectedRow - 1);
        table2.getSelectionModel().select(selectedRow - 1);
        table2.scrollTo(selectedRow - 1);
        isTableModified = true;
    }

    private void moveDown2() {
        stopEditing();
        int selectedRow = table2.getSelectionModel().getSelectedIndex();
        if (selectedRow == -1) {
            showAlert(Alert.AlertType.WARNING, "No Row Selected", "Please select a row to move");
            return;
        }
        if (selectedRow == table2Data.size() - 1) {
            showAlert(Alert.AlertType.WARNING, "Invalid Action", "Cannot move the last row of the table");
            return;
        }
        Collections.swap(table2Data, selectedRow, selectedRow + 1);
        Collections.swap(previousElementNames, selectedRow, selectedRow + 1);
        table2.getSelectionModel().select(selectedRow + 1);
        table2.scrollTo(selectedRow + 1);
        isTableModified = true;
    }

    private void deleteElement() {
        stopEditing();
        int selectedRow = table2.getSelectionModel().getSelectedIndex();
        if (selectedRow == -1) {
            showAlert(Alert.AlertType.WARNING, "No Row Selected", "Please select a row to delete");
            return;
        }
        if (selectedRow < previousElementNames.size()) {
            previousElementNames.remove(selectedRow);
        }
        table2Data.remove(selectedRow);
        if (!table2Data.isEmpty()) {
            int newSelection = Math.min(selectedRow, table2Data.size() - 1);
            table2.getSelectionModel().select(newSelection);
            table2.scrollTo(newSelection);
        }
        isTableModified = true;
    }

    private void saveJson() {
        stopEditing();
        if (table1Data.isEmpty() && table2Data.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Save Info", "No data to save.");
            return;
        }
        if (lastUsedFile == null) {
            showSaveDialog(null);
        } else {
            showSaveOptionsDialog(lastUsedFile);
        }
    }

    private void loadJson() {
        stopEditing();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Table from JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showOpenDialog(table1.getScene().getWindow());
        if (file == null) return;
        try (FileReader fileReader = new FileReader(file)) {
            StringBuilder content = new StringBuilder();
            int ch;
            while ((ch = fileReader.read()) != -1) {
                content.append((char) ch);
            }
            JSONArray rootArray = new JSONArray(content.toString());
            table1Data.clear();
            table2Data.clear();
            previousElementNames.clear();
            Set<String> validElementNames = new TreeSet<>();
            for (int i = 0; i < rootArray.length(); i++) {
                JSONObject obj = rootArray.getJSONObject(i);
                if (obj.has("Element_List")) {
                    JSONObject elementList = obj.getJSONObject("Element_List");
                    for (String key : elementList.keySet()) {
                        validElementNames.add(key);
                    }
                }
            }
            for (int i = 0; i < rootArray.length(); i++) {
                JSONObject obj = rootArray.getJSONObject(i);
                if (obj.has("Test_Id")) {
                    String testId = obj.getString("Test_Id");
                    Set<String> keys = new TreeSet<>((k1, k2) -> {
                        if (!k1.startsWith("Test_Step ") || !k2.startsWith("Test_Step ")) {
                            return k1.compareTo(k2);
                        }
                        int n1 = Integer.parseInt(k1.replace("Test_Step ", ""));
                        int n2 = Integer.parseInt(k2.replace("Test_Step ", ""));
                        return Integer.compare(n1, n2);
                    });
                    keys.addAll(obj.keySet());
                    keys.remove("Test_Id");
                    for (String key : keys) {
                        if (key.startsWith("Test_Step ")) {
                            JSONArray stepArray = obj.getJSONArray(key);
                            String stepNumber = key.replace("Test_Step ", "");
                            String testAction = stepArray.length() > 0 ? stepArray.getString(0) : "";
                            String testElement = stepArray.length() > 1 ? stepArray.getString(1) : "";
                            if (!testElement.isEmpty() && !validElementNames.contains(testElement)) {
                                testElement = "";
                            }
                            String testData = stepArray.length() > 2 ? stepArray.getString(2) : "";
                            String description = stepArray.length() > 3 ? stepArray.getString(3) : "";
                            table1Data.add(new TestCaseRow(testId, stepNumber, testAction, testElement, testData, description));
                            testId = "";
                        }
                    }
                } else if (obj.has("Element_List")) {
                    JSONObject elementList = obj.getJSONObject("Element_List");
                    for (String key : elementList.keySet()) {
                        JSONArray elementArray = elementList.getJSONArray(key);
                        String elementName = key;
                        String automationId = elementArray.length() > 0 ? elementArray.getString(0) : "";
                        String name = elementArray.length() > 1 ? elementArray.getString(1) : "";
                        String xpath = elementArray.length() > 2 ? elementArray.getString(2) : "";
                        table2Data.add(new ElementRow(elementName, automationId, name, xpath));
                        previousElementNames.add(elementName);
                    }
                }
            }
            updateTestStepNumbers();
            lastUsedFile = file;
            isTableModified = false;
            showAlert(Alert.AlertType.INFORMATION, "Load Successful", "Table loaded successfully from " + file.getAbsolutePath());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Load Error", "Error reading file: " + ex.getMessage());
        } catch (JSONException ex) {
            showAlert(Alert.AlertType.ERROR, "Load Error", "Invalid JSON format: " + ex.getMessage());
        }
    }

    private void clearTable() {
        stopEditing();
        if (!table1Data.isEmpty() || !table2Data.isEmpty()) {
            if (isTableModified) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unsaved Changes");
                alert.setHeaderText("Table has unsaved changes. Would you like to save before clearing?");
                alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.YES) {
                    if (lastUsedFile == null) {
                        if (showSaveDialog(null)) {
                            clearTables();
                        }
                    } else {
                        if (showSaveOptionsDialog(lastUsedFile)) {
                            clearTables();
                        }
                    }
                } else if (result.isPresent() && result.get() == ButtonType.NO) {
                    clearTables();
                }
            } else {
                clearTables();
            }
        }
    }

    private void clearTables() {
        table1Data.clear();
        table2Data.clear();
        previousElementNames.clear();
        lastUsedFile = null;
        isTableModified = false;
    }

    private boolean showSaveOptionsDialog(File file) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Save Options");
        alert.setHeaderText("Choose an option to save the table:");
        ButtonType saveButton = new ButtonType("Save");
        ButtonType saveAsButton = new ButtonType("Save As");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, saveAsButton, cancelButton);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == saveButton) {
            return saveFile(file);
        } else if (result.isPresent() && result.get() == saveAsButton) {
            return showSaveDialog(file);
        }
        return false;
    }

    private boolean showSaveDialog(File lastFile) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Table as JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        if (lastFile != null) {
            fileChooser.setInitialFileName(lastFile.getName());
        }
        File newFile = fileChooser.showSaveDialog(table1.getScene().getWindow());
        if (newFile == null) return false;
        String filePath = newFile.getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".json")) {
            newFile = new File(filePath + ".json");
        }
        if (newFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Save Options");
            alert.setHeaderText("File already exists. Overwrite?");
            alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                if (saveFile(newFile)) {
                    lastUsedFile = newFile;
                    return true;
                }
            } else if (result.isPresent() && result.get() == ButtonType.NO) {
                return showSaveDialog(newFile);
            }
        } else {
            if (saveFile(newFile)) {
                lastUsedFile = newFile;
                return true;
            }
        }
        return false;
    }

    private boolean saveFile(File file) {
        try {
            JSONArray rootArray = new JSONArray();
            String currentTestId = "";
            LinkedHashMap<String, Object> testCaseMap = null;
            int stepCount = 1;

            for (TestCaseRow row : table1Data) {
                String testId = row.getTestId();
                String testAction = row.getTestAction();
                String testElement = row.getTestElement();
                String testData = row.getTestData();
                String description = row.getDescription();

                if (!testId.isEmpty() && !testId.equals("#")) {
                    if (testCaseMap != null) {
                        LinkedHashMap<String, Object> sortedTestCaseMap = new LinkedHashMap<>();
                        sortedTestCaseMap.put("Test_Id", testCaseMap.get("Test_Id"));
                        TreeSet<String> sortedKeys = new TreeSet<>((k1, k2) -> {
                            if (!k1.startsWith("Test_Step ") || !k2.startsWith("Test_Step ")) {
                                return k1.compareTo(k2);
                            }
                            int n1 = Integer.parseInt(k1.replace("Test_Step ", ""));
                            int n2 = Integer.parseInt(k2.replace("Test_Step ", ""));
                            return Integer.compare(n1, n2);
                        });
                        sortedKeys.addAll(testCaseMap.keySet());
                        sortedKeys.remove("Test_Id");
                        for (String key : sortedKeys) {
                            sortedTestCaseMap.put(key, testCaseMap.get(key));
                        }
                        rootArray.put(new JSONObject(sortedTestCaseMap));
                    }
                    testCaseMap = new LinkedHashMap<>();
                    testCaseMap.put("Test_Id", testId);
                    currentTestId = testId;
                    stepCount = 1;
                }
                JSONArray stepArray = new JSONArray();
                stepArray.put(testAction);
                stepArray.put(testElement);
                stepArray.put(testData);
                stepArray.put(description);
                if (testCaseMap != null) {
                    testCaseMap.put("Test_Step " + stepCount, stepArray);
                    stepCount++;
                }
            }
            if (testCaseMap != null) {
                LinkedHashMap<String, Object> sortedTestCaseMap = new LinkedHashMap<>();
                sortedTestCaseMap.put("Test_Id", testCaseMap.get("Test_Id"));
                TreeSet<String> sortedKeys = new TreeSet<>((k1, k2) -> {
                    if (!k1.startsWith("Test_Step ") || !k2.startsWith("Test_Step ")) {
                        return k1.compareTo(k2);
                    }
                    int n1 = Integer.parseInt(k1.replace("Test_Step ", ""));
                    int n2 = Integer.parseInt(k2.replace("Test_Step ", ""));
                    return Integer.compare(n1, n2);
                });
                sortedKeys.addAll(testCaseMap.keySet());
                sortedKeys.remove("Test_Id");
                for (String key : sortedKeys) {
                    sortedTestCaseMap.put(key, testCaseMap.get(key));
                }
                rootArray.put(new JSONObject(sortedTestCaseMap));
            }

            if (!table2Data.isEmpty()) {
                LinkedHashMap<String, Object> elementMap = new LinkedHashMap<>();
                JSONObject elementObject = new JSONObject();
                for (ElementRow row : table2Data) {
                    String elementName = row.getElementName();
                    String automationId = row.getAutomationId();
                    String name = row.getName();
                    String xpath = row.getXpath();
                    if (!elementName.isEmpty()) {
                        JSONArray elementArray = new JSONArray();
                        elementArray.put(automationId);
                        elementArray.put(name);
                        elementArray.put(xpath);
                        elementObject.put(elementName, elementArray);
                    }
                }
                elementMap.put("Element_List", elementObject);
                rootArray.put(new JSONObject(elementMap));
            }

            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(rootArray.toString(2));
                fileWriter.flush();
                isTableModified = false;
                showAlert(Alert.AlertType.INFORMATION, "Save Successful", "Table saved successfully to " + file.getAbsolutePath());
                return true;
            }
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Save Error", "Error saving file: " + ex.getMessage());
            return false;
        } catch (JSONException ex) {
            showAlert(Alert.AlertType.ERROR, "Save Error", "Error creating JSON: " + ex.getMessage());
            return false;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Model classes
    public static class TestCaseRow {
        private final StringProperty testId;
        private final StringProperty testStep;
        private final StringProperty testAction;
        private final StringProperty testElement;
        private final StringProperty testData;
        private final StringProperty description;

        public TestCaseRow(String testId, String testStep, String testAction, String testElement, String testData, String description) {
            this.testId = new SimpleStringProperty(testId);
            this.testStep = new SimpleStringProperty(testStep);
            this.testAction = new SimpleStringProperty(testAction);
            this.testElement = new SimpleStringProperty(testElement);
            this.testData = new SimpleStringProperty(testData);
            this.description = new SimpleStringProperty(description);
        }

        public String getTestId() { return testId.get(); }
        public void setTestId(String value) { testId.set(value); }
        public StringProperty testIdProperty() { return testId; }

        public String getTestStep() { return testStep.get(); }
        public void setTestStep(String value) { testStep.set(value); }
        public StringProperty testStepProperty() { return testStep; }

        public String getTestAction() { return testAction.get(); }
        public void setTestAction(String value) { testAction.set(value); }
        public StringProperty testActionProperty() { return testAction; }

        public String getTestElement() { return testElement.get(); }
        public void setTestElement(String value) { testElement.set(value); }
        public StringProperty testElementProperty() { return testElement; }

        public String getTestData() { return testData.get(); }
        public void setTestData(String value) { testData.set(value); }
        public StringProperty testDataProperty() { return testData; }

        public String getDescription() { return description.get(); }
        public void setDescription(String value) { description.set(value); }
        public StringProperty descriptionProperty() { return description; }
    }

    public static class ElementRow {
        private final StringProperty elementName;
        private final StringProperty automationId;
        private final StringProperty name;
        private final StringProperty xpath;

        public ElementRow(String elementName, String automationId, String name, String xpath) {
            this.elementName = new SimpleStringProperty(elementName);
            this.automationId = new SimpleStringProperty(automationId);
            this.name = new SimpleStringProperty(name);
            this.xpath = new SimpleStringProperty(xpath);
        }

        public String getElementName() { return elementName.get(); }
        public void setElementName(String value) { elementName.set(value); }
        public StringProperty elementNameProperty() { return elementName; }

        public String getAutomationId() { return automationId.get(); }
        public void setAutomationId(String value) { automationId.set(value); }
        public StringProperty automationIdProperty() { return automationId; }

        public String getName() { return name.get(); }
        public void setName(String value) { name.set(value); }
        public StringProperty nameProperty() { return name; }

        public String getXpath() { return xpath.get(); }
        public void setXpath(String value) { xpath.set(value); }
        public StringProperty xpathProperty() { return xpath; }
    }

    // Custom ComboBoxTableCell
    private static class ComboBoxTableCell<S, T> extends TableCell<S, T> {
        private final ComboBox<T> comboBox;

        public ComboBoxTableCell(ObservableList<T> items) {
            this.comboBox = new ComboBox<>(items);
            comboBox.setEditable(false);
            comboBox.setMaxWidth(Double.MAX_VALUE);
            comboBox.setStyle("-fx-background-color: #3C3C3C; -fx-text-fill: #C8C8C8; -fx-font: 12pt 'Arial';");
            comboBox.getStylesheets().add(
                "data:text/css," +
                ".combo-box .list-cell {" +
                "    -fx-background-color: #3C3C3C;" +
                "    -fx-text-fill: #C8C8C8;" +
                "}" +
                ".combo-box-popup .list-view {" +
                "    -fx-background-color: #3C3C3C;" +
                "}" +
                ".combo-box-popup .list-view .list-cell {" +
                "    -fx-background-color: #3C3C3C;" +
                "    -fx-text-fill: #C8C8C8;" +
                "}" +
                ".combo-box-popup .list-view .list-cell:hover {" +
                "    -fx-background-color: #4682B4;" +
                "    -fx-text-fill: white;" +
                "}"
            );

            // Commit edit when a new value is selected
            comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                if (isEditing() && newValue != null) {
                    commitEdit(newValue);
                }
            });
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: #323232;");
            } else {
                if (isEditing()) {
                    comboBox.getSelectionModel().select(item);
                    setGraphic(comboBox);
                    setText(null);
                } else {
                    setText(item.toString());
                    setGraphic(null);
                }
            }
        }

        @Override
        public void startEdit() {
            super.startEdit();
            comboBox.getSelectionModel().select(getItem());
            setGraphic(comboBox);
            setText(null);
            comboBox.requestFocus();
        }

        @Override
        public void commitEdit(T newValue) {
            super.commitEdit(newValue);
            setText(newValue != null ? newValue.toString() : "");
            setGraphic(null);
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() != null ? getItem().toString() : "");
            setGraphic(null);
        }
    }
}