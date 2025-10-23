package com.test.window.gui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.test.window.app.Commons;
import com.test.window.app.UIConstants;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TestManagerApp extends Application {

    // Color constants from TestRunner
    private static final String ENABLED_COLOR = "#32A852"; // Green
    private static final String HOVER_COLOR = "#288A48"; // Darker green
    private static final String DISABLED_COLOR = "#646464"; // Gray
    private static final String TABLE_BACKGROUND = "#323232"; // Dark gray
    private static final String DEFAULT_FOREGROUND = "#C8C8C8"; // Light gray
    private static final String SELECTION_COLOR = "#0788b3"; // Light blue for row selector

    private static File lastUsedFile = null;
    private static boolean isTableModified = false;

    // TestStep class to hold test steps table data
    public static class TestStep {
        private String testId;
        private String testStep;
        private String testAction;
        private String testElement;
        private String testData;
        private String testDescription;
        private boolean hasBorder;

        public TestStep(String testId, String testStep, String testAction, 
                       String testElement, String testData, String testDescription, boolean hasBorder) {
            this.testId = testId;
            this.testStep = testStep;
            this.testAction = testAction;
            this.testElement = testElement;
            this.testData = testData;
            this.testDescription = testDescription;
            this.hasBorder = hasBorder;
        }

        // Getters and setters
        public String getTestId() { return testId; }
        public void setTestId(String testId) { 
            this.testId = testId; 
            this.hasBorder = true;
            System.out.println("TestStep setTestId: " + testId);
        }
        public String getTestStep() { return testStep; }
        public void setTestStep(String testStep) { 
            this.testStep = testStep; 
            this.hasBorder = true;
            System.out.println("TestStep setTestStep: " + testStep);
        }
        public String getTestAction() { return testAction; }
        public void setTestAction(String testAction) { 
            this.testAction = testAction; 
            this.hasBorder = true;
            System.out.println("TestStep setTestAction: " + testAction);
        }
        public String getTestElement() { return testElement; }
        public void setTestElement(String testElement) { 
            this.testElement = testElement; 
            this.hasBorder = true;
            System.out.println("TestStep setTestElement: " + testElement);
        }
        public String getTestData() { return testData; }
        public void setTestData(String testData) { 
            this.testData = testData; 
            this.hasBorder = true;
            System.out.println("TestStep setTestData: " + testData);
        }
        public String getTestDescription() { return testDescription; }
        public void setTestDescription(String testDescription) { 
            this.testDescription = testDescription; 
            this.hasBorder = true;
            System.out.println("TestStep setTestDescription: " + testDescription);
        }
        public boolean hasBorder() { return hasBorder; }
        public void setHasBorder(boolean hasBorder) { this.hasBorder = hasBorder; }
    }

    // Element class to hold element table data
    public static class Element {
        private String elementName;
        private String automationId;
        private String name;
        private String xpath;

        public Element(String elementName, String automationId, String name, String xpath) {
            this.elementName = elementName;
            this.automationId = automationId;
            this.name = name;
            this.xpath = xpath;
        }

        // Getters and setters
        public String getElementName() { return elementName; }
        public void setElementName(String elementName) { 
            this.elementName = elementName; 
            System.out.println("Element setElementName: " + elementName);
        }
        public String getAutomationId() { return automationId; }
        public void setAutomationId(String automationId) { 
            this.automationId = automationId; 
            System.out.println("Element setAutomationId: " + automationId);
        }
        public String getName() { return name; }
        public void setName(String name) { 
            this.name = name; 
            System.out.println("Element setName: " + name);
        }
        public String getXpath() { return xpath; }
        public void setXpath(String xpath) { 
            this.xpath = xpath; 
            System.out.println("Element setXpath: " + xpath);
        }
    }

    // Custom TextFieldTableCell for TestStep table
    public static class CustomTextFieldTableCell extends TableCell<TestStep, String> {
        private TextField textField;
        private final int maxLength;
        private final boolean isTestId;
        private String oldValue;

        public CustomTextFieldTableCell(int maxLength, boolean isTestId) {
            this.maxLength = maxLength;
            this.isTestId = isTestId;
            this.textField = new TextField();

            // Enforce input validation for Test ID
            if (isTestId) {
                UnaryOperator<TextFormatter.Change> filter = change -> {
                    String newText = change.getControlNewText();
                    if (newText.matches("(#|[0-9]*)") && (maxLength <= 0 || newText.length() <= maxLength)) {
                        return change;
                    }
                    return null;
                };
                textField.setTextFormatter(new TextFormatter<>(filter));
            } else if (maxLength > 0) {
                UnaryOperator<TextFormatter.Change> filter = change -> {
                    String newText = change.getControlNewText();
                    if (newText.length() <= maxLength) {
                        return change;
                    }
                    return null;
                };
                textField.setTextFormatter(new TextFormatter<>(filter));
            }

            textField.textProperty().addListener((obs, oldText, newText) -> {
                if (isEditing()) {
                    oldValue = newText;
                }
            });

            textField.setOnAction(event -> {
                if (isEditing()) {
                    commitEdit(textField.getText());
                }
            });

            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused && isEditing()) {
                    commitEdit(textField.getText());
                }
            });

            // Handle TAB key to commit edit and highlight row selector
            textField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.TAB) {
                    System.out.println("hi, i am here");
                    commitEdit(textField.getText());
                    Platform.runLater(() -> {
                        TableView<?> table = getTableView();
                        if (table != null && table.isFocusTraversable()) {
                            int rowIndex = getIndex();
                            table.getSelectionModel().select(rowIndex);
                            table.refresh();
                            Platform.runLater(() -> {
                                table.requestFocus();
                                String tableName = table.getId() != null ? table.getId() : "unknown";
                                System.out.println("Tabbed out from text field cell edit, row selector highlighted: row=" + rowIndex + ", table=" + tableName + ", focused=" + table.isFocused() + ", selectedIndex=" + table.getSelectionModel().getSelectedIndex());
                            });
                        }
                    });
                    event.consume();
                }
            });
        }

        private boolean isDuplicate(String newValue, TableView<TestStep> table, int currentRow) {
            if (newValue == null || newValue.isEmpty() || newValue.equals("#")) {
                return false;
            }
            ObservableList<TestStep> items = table.getItems();
            for (int i = 0; i < items.size(); i++) {
                if (i != currentRow && newValue.equals(items.get(i).getTestId())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void startEdit() {
            if (!isEditing() && !isEmpty()) {
                super.startEdit();
                oldValue = getItem() == null ? "" : getItem();
                textField.setText(oldValue);
                setText(null);
                setGraphic(textField);
                textField.requestFocus();
                textField.selectAll();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() == null ? "" : getItem());
            setGraphic(null);
        }

        @Override
        public void commitEdit(String newValue) {
            if (isTestId && isDuplicate(newValue, getTableView(), getIndex())) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Duplicate Error");
                alert.setHeaderText(null);
                alert.setContentText("Duplicate Test Id not allowed");
                alert.showAndWait();
                textField.requestFocus();
                return;
            }
            super.commitEdit(newValue);
            setText(newValue == null ? "" : newValue);
            setGraphic(null);
            TestStep item = getTableRow().getItem();
            if (item != null) {
                String property = getTableColumn().getId();
                System.out.println("Committing edit in column: " + property + ", value: " + newValue + ", row: " + getIndex());
                switch (property) {
                    case "testId":
                        item.setTestId(newValue);
                        TestManagerApp.updateTestStepNumbers(getTableView().getItems());
                        break;
                    case "testStep":
                        item.setTestStep(newValue);
                        break;
                    case "testElement":
                        item.setTestElement(newValue);
                        break;
                    case "testData":
                        item.setTestData(newValue);
                        break;
                    case "testDescription":
                        item.setTestDescription(newValue);
                        break;
                }
            }
            getTableView().refresh();
            if (!newValue.equals(oldValue)) {
                isTableModified = true;
            }
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText("");
                setGraphic(null);
                setStyle("-fx-text-fill: #FFFFFF;");
            } else {
                setText(item);
                setGraphic(null);
                TestStep testStep = getTableRow().getItem();
                //if (testStep != null && testStep.hasBorder()) {
                    setStyle("-fx-text-fill: #FFFFFF; -fx-border-color: " + DISABLED_COLOR + "; -fx-border-width: 0.5;");
                //} else {
                    //setStyle("-fx-text-fill: #FFFFFF;");
                //}
            }
            System.out.println("updateItem in CustomTextFieldTableCell, column: " + 
                (getTableColumn() != null ? getTableColumn().getId() : "unknown") + 
                ", item: " + item + ", empty: " + empty + ", row: " + getIndex());
        }
    }

    // Custom cell for TestStep column (non-editable)
    public static class TestStepTableCell extends TableCell<TestStep, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText("");
                setGraphic(null);
                setStyle("-fx-text-fill: #FFFFFF;");
            } else {
                setText(item);
                setGraphic(null);
                TestStep testStep = getTableRow().getItem();
                //if (testStep != null && testStep.hasBorder()) {
                    setStyle("-fx-text-fill: #FFFFFF; -fx-border-color: " + DISABLED_COLOR + "; -fx-border-width: 0.5;");
                //} else {
                    //setStyle("-fx-text-fill: #FFFFFF;");
                //}
            }
            System.out.println("updateItem in TestStepTableCell, column: testStep, item: " + item + ", empty: " + empty + ", row: " + getIndex());
        }
    }

    // Custom TextFieldTableCell for Element table
    public static class ElementTextFieldTableCell extends TableCell<Element, String> {
        private TextField textField;
        private String oldValue;
        private final boolean isElementName;

        public ElementTextFieldTableCell(boolean isElementName) {
            this.isElementName = isElementName;
            this.textField = new TextField();

            if (isElementName) {
                UnaryOperator<TextFormatter.Change> filter = change -> {
                    String newText = change.getControlNewText();
                    if (newText.isEmpty() || newText.matches("[a-zA-Z][a-zA-Z0-9-_]*")) {
                        return change;
                    }
                    return null;
                };
                textField.setTextFormatter(new TextFormatter<>(filter));
            }

            textField.textProperty().addListener((obs, oldText, newText) -> {
                if (isEditing()) {
                    oldValue = newText;
                }
            });

            textField.setOnAction(event -> {
                if (isEditing()) {
                    commitEdit(textField.getText());
                }
            });

            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused && isEditing()) {
                    commitEdit(textField.getText());
                }
            });

            // Handle TAB key to commit edit and highlight row selector
            textField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.TAB) {
                    commitEdit(textField.getText());
                    Platform.runLater(() -> {
                        TableView<?> table = getTableView();
                        if (table != null && table.isFocusTraversable()) {
                            int rowIndex = getIndex();
                            table.getSelectionModel().select(rowIndex);
                            Platform.runLater(() -> {
                                table.requestFocus();
                                String tableName = table.getId() != null ? table.getId() : "unknown";
                                System.out.println("Tabbed out from text field cell edit, row selector highlighted: row=" + rowIndex + ", table=" + tableName + ", focused=" + table.isFocused() + ", selectedIndex=" + table.getSelectionModel().getSelectedIndex());
                            });
                        }
                    });
                    event.consume();
                }
            });
        }

        private boolean isDuplicate(String newValue, TableView<Element> table, int currentRow) {
            if (newValue == null || newValue.isEmpty()) {
                return false;
            }
            ObservableList<Element> items = table.getItems();
            for (int i = 0; i < items.size(); i++) {
                if (i != currentRow && newValue.equalsIgnoreCase(items.get(i).getElementName())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void startEdit() {
            if (!isEditing() && !isEmpty()) {
                super.startEdit();
                oldValue = getItem() == null ? "" : getItem();
                textField.setText(oldValue);
                setText(null);
                setGraphic(textField);
                textField.requestFocus();
                textField.selectAll();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() == null ? "" : getItem());
            setGraphic(null);
        }

        @Override
        public void commitEdit(String newValue) {
            if (isElementName && isDuplicate(newValue, getTableView(), getIndex())) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Duplicate Error");
                alert.setHeaderText(null);
                alert.setContentText("Duplicate Element Name not allowed (case-insensitive)");
                alert.showAndWait();
                textField.requestFocus();
                return;
            }
            if (isElementName && !newValue.isEmpty() && !newValue.matches("[a-zA-Z][a-zA-Z0-9-_]*")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Input");
                alert.setHeaderText(null);
                alert.setContentText("Element Name must start with a character (a-z or A-Z) and can only contain alphabets, numbers, '-' and '_'");
                alert.showAndWait();
                textField.requestFocus();
                return;
            }
            super.commitEdit(newValue);
            setText(newValue == null ? "" : newValue);
            setGraphic(null);
            Element item = getTableRow().getItem();
            if (item != null) {
                String property = getTableColumn().getId();
                System.out.println("Committing edit in ElementTextFieldTableCell, column: " + property + ", value: " + newValue + ", row: " + getIndex());
                switch (property) {
                    case "elementName":
                        item.setElementName(newValue);
                        break;
                    case "automationId":
                        item.setAutomationId(newValue);
                        break;
                    case "name":
                        item.setName(newValue);
                        break;
                    case "xpath":
                        item.setXpath(newValue);
                        break;
                }
            }
            getTableView().refresh();
            if (!newValue.equals(oldValue)) {
                isTableModified = true;
            }
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText("");
                setGraphic(null);
            } else {
                setText(item);
                setGraphic(null);
                setStyle("-fx-text-fill: #FFFFFF; -fx-border-color: " + DISABLED_COLOR + "; -fx-border-width: 0.5;");
            }
            System.out.println("updateItem in ElementTextFieldTableCell, column: " + 
                (getTableColumn() != null ? getTableColumn().getId() : "unknown") + 
                ", item: " + item + ", empty: " + empty + ", row: " + getIndex());
        }
    }

    public static class CustomComboBoxTableCell<S> extends TableCell<S, String> {
        private final ComboBox<String> comboBox;

        public CustomComboBoxTableCell(ObservableList<String> items) {
            this.comboBox = new ComboBox<>(items);
            this.comboBox.setEditable(false);
            this.comboBox.setMaxWidth(Double.MAX_VALUE);

            this.comboBox.setOnAction(event -> {
                String selectedValue = comboBox.getValue() != null ? comboBox.getValue() : "";
                if (isEditing()) {
                    commitEdit(selectedValue);
                }
            });

            this.comboBox.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.TAB && isEditing()) {
                    String selectedValue = comboBox.getValue() != null ? comboBox.getValue() : "";
                    commitEdit(selectedValue);
                    Platform.runLater(() -> {
                        TableView<?> table = getTableView();
                        if (table != null && table.isFocusTraversable()) {
                            int rowIndex = getIndex();
                            table.getSelectionModel().select(rowIndex);
                            Platform.runLater(() -> {
                                table.requestFocus();
                                String tableName = table.getId() != null ? table.getId() : "unknown";
                                System.out.println("Tabbed out from combo box cell edit, row selector highlighted: row=" + rowIndex + ", table=" + tableName + ", focused=" + table.isFocused() + ", selectedIndex=" + table.getSelectionModel().getSelectedIndex());
                            });
                        }
                    });
                    event.consume();
                }
            });
        }

        @Override
        public void startEdit() {
            if (!isEmpty() && !isEditing()) {
                super.startEdit();
                String currentValue = getItem() != null ? getItem() : "";
                comboBox.setValue(currentValue);
                setText(null);
                setGraphic(comboBox);
                comboBox.requestFocus();
                comboBox.show();
                System.out.println("Started editing in CustomComboBoxTableCell, column: " + 
                    (getTableColumn() != null ? getTableColumn().getId() : "unknown") + 
                    ", value: " + currentValue + ", row: " + getIndex());
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() != null ? getItem() : "");
            setGraphic(null);
            System.out.println("Cancelled editing in CustomComboBoxTableCell, column: " + 
                (getTableColumn() != null ? getTableColumn().getId() : "unknown") + 
                ", row: " + getIndex());
        }

        @Override
        public void commitEdit(String newValue) {
            String oldValue = getItem() != null ? getItem() : "";
            super.commitEdit(newValue);
            setText(newValue != null ? newValue : "");
            setGraphic(null);
            if (getTableRow().getItem() instanceof TestStep) {
                TestStep item = (TestStep) getTableRow().getItem();
                if (item != null) {
                    String columnId = getTableColumn() != null ? getTableColumn().getId() : "";
                    switch (columnId) {
                        case "testAction":
                            item.setTestAction(newValue);
                            break;
                        case "testElement":
                            item.setTestElement(newValue);
                            break;
                    }
                    if (!newValue.equals(oldValue)) {
                        isTableModified = true;
                    }
                }
            }
            getTableView().refresh();
            System.out.println("Committed edit in CustomComboBoxTableCell, column: " + 
                (getTableColumn() != null ? getTableColumn().getId() : "unknown") + 
                ", value: " + newValue + ", row: " + getIndex());
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText("");
                setGraphic(null);
                setStyle("-fx-text-fill: #FFFFFF;");
            } else {
                setText(item);
                setGraphic(null);
                setStyle("-fx-text-fill: #FFFFFF; -fx-border-color: " + DISABLED_COLOR + "; -fx-border-width: 0.5;");
            }
            System.out.println("updateItem in CustomComboBoxTableCell, column: " + 
                (getTableColumn() != null ? getTableColumn().getId() : "unknown") + 
                ", item: " + item + ", empty: " + empty + ", row: " + getIndex());
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // Create data lists
        ObservableList<TestStep> testData = FXCollections.observableArrayList();
        ObservableList<Element> elementData = FXCollections.observableArrayList();
        ObservableList<String> elementNameItems = FXCollections.observableArrayList();
        elementNameItems.add("");

        // Create tables using TableFactory
        TableFactory tableFactory = new TableFactory();
        TableView<TestStep> testTable = tableFactory.createTestTable(testData, elementNameItems);
        TableView<Element> elementTable = tableFactory.createElementTable(elementData, elementNameItems);

        // Left-align header text for testTable columns
        for (TableColumn<TestStep, ?> column : testTable.getColumns()) {
            column.setStyle("-fx-alignment: CENTER-LEFT;");
        }

        // Left-align header text for elementTable columns
        for (TableColumn<Element, ?> column : elementTable.getColumns()) {
            column.setStyle("-fx-alignment: CENTER-LEFT;");
        }

        // Configure elementTable for single-row selection and keyboard navigation
        elementTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        elementTable.setFocusTraversable(true);

        // Add focus listeners for debugging
        elementTable.focusedProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("ElementTable focus changed: focused=" + newVal + ", selectedIndex=" + elementTable.getSelectionModel().getSelectedIndex());
        });
        testTable.focusedProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("TestTable focus changed: focused=" + newVal + ", selectedIndex=" + testTable.getSelectionModel().getSelectedIndex());
        });

        // Update elementNameItems when elementData changes
        elementData.addListener((ListChangeListener<Element>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasUpdated()) {
                    elementNameItems.clear();
                    elementNameItems.add("");
                    for (Element element : elementData) {
                        if (element.getElementName() != null && !element.getElementName().isEmpty()) {
                            elementNameItems.add(element.getElementName());
                        }
                    }
                    isTableModified = true;
                    elementTable.refresh();
                    System.out.println("Element data updated, elementNameItems: " + elementNameItems);
                }
            }
        });

        // Listen for changes to set modification flag for test steps table
        testData.addListener((ListChangeListener<TestStep>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasUpdated()) {
                    updateTestStepNumbers(testData);
                    isTableModified = true;
                    testTable.refresh();
                    System.out.println("Test data updated, testData size: " + testData.size());
                }
            }
        });

        // Create buttons with uniform size and TestRunner styling
        Button addStepBtn = createStyledButton("Add Step", "Add a new test step");
        Button addUpBtn = createStyledButton("Add Up", "Add a step above selected");
        Button addDownBtn = createStyledButton("Add Down", "Add a step below selected");
        Button deleteStepBtn = createStyledButton("Delete Step", "Delete selected step");
        Button moveUpBtn = createStyledButton("Move Up", "Move selected step up");
        Button moveDownBtn = createStyledButton("Move Down", "Move selected step down");
        Button addElementBtn = createStyledButton("Add Element", "Add a new element to the element table");
        Button moveUpElementBtn = createStyledButton("Move Up", "Move selected element up");
        Button moveDownElementBtn = createStyledButton("Move Down", "Move selected element down");
        Button deleteElementBtn = createStyledButton("Delete Element", "Delete selected element");
        Button saveBtn = createStyledButton("Save Test", "Save test steps and elements to file");
        Button loadTestBtn = createStyledButton("Load Test", "Load test steps and elements from file");
        Button clearTestBtn = createStyledButton("Clear Test", "Clear all test steps and elements");

        // Add Step button action
        addStepBtn.setOnAction(e -> {
            TestStep newStep = new TestStep("", "1", "", "", "", "", true);
            testData.add(newStep);
            int newRowIndex = testData.size() - 1;
            testTable.getSelectionModel().select(newRowIndex);
            testTable.scrollTo(newRowIndex);
            isTableModified = true;
            updateTestStepNumbers(testData);
            testTable.refresh();
        });

        // Add Up button action
        addUpBtn.setOnAction(e -> {
            int selectedIndex = testTable.getSelectionModel().getSelectedIndex();
            if (selectedIndex == -1) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Row Selected");
                alert.setHeaderText(null);
                alert.setContentText("Please select a row to add above");
                alert.showAndWait();
                return;
            }
            TestStep newStep = new TestStep("", "1", "", "", "", "", true);
            testData.add(selectedIndex, newStep);
            testTable.getSelectionModel().select(selectedIndex);
            testTable.scrollTo(selectedIndex);
            isTableModified = true;
            updateTestStepNumbers(testData);
            testTable.refresh();
        });

        // Add Down button action
        addDownBtn.setOnAction(e -> {
            int selectedIndex = testTable.getSelectionModel().getSelectedIndex();
            if (selectedIndex == -1) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Row Selected");
                alert.setHeaderText(null);
                alert.setContentText("Please select a row to add below");
                alert.showAndWait();
                return;
            }
            TestStep newStep = new TestStep("", "1", "", "", "", "", true);
            testData.add(selectedIndex + 1, newStep);
            testTable.getSelectionModel().select(selectedIndex + 1);
            testTable.scrollTo(selectedIndex + 1);
            isTableModified = true;
            updateTestStepNumbers(testData);
            testTable.refresh();
        });

        // Delete Step button action
        deleteStepBtn.setOnAction(e -> {
            TestStep selected = testTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Row Selected");
                alert.setHeaderText(null);
                alert.setContentText("Please select a row to delete");
                alert.showAndWait();
                return;
            }
            int index = testData.indexOf(selected);
            if (index >= 0) {
                testData.remove(index);
                if (testData.size() > 0) {
                    int newSelection = Math.min(index, testData.size() - 1);
                    testTable.getSelectionModel().select(newSelection);
                    testTable.scrollTo(newSelection);
                } else {
                    testTable.getSelectionModel().clearSelection();
                }
                isTableModified = true;
                updateTestStepNumbers(testData);
                testTable.refresh();
                Platform.runLater(() -> {
                    testTable.requestFocus();
                    System.out.println("TestTable focus restored after delete step");
                });
                System.out.println("Deleted test step, index: " + index);
            }
        });

        // Move Up button action
        moveUpBtn.setOnAction(e -> {
            int selectedIndex = testTable.getSelectionModel().getSelectedIndex();
            if (selectedIndex == -1) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Row Selected");
                alert.setHeaderText(null);
                alert.setContentText("Please select a row to move");
                alert.showAndWait();
                return;
            }
            if (selectedIndex == 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Invalid Action");
                alert.setHeaderText(null);
                alert.setContentText("Cannot move the first row of the table");
                alert.showAndWait();
                return;
            }
            TestStep temp = testData.get(selectedIndex);
            testData.remove(selectedIndex);
            testData.add(selectedIndex - 1, temp);
            testTable.getSelectionModel().select(selectedIndex - 1);
            testTable.scrollTo(selectedIndex - 1);
            isTableModified = true;
            updateTestStepNumbers(testData);
            testTable.refresh();
            Platform.runLater(() -> {
                testTable.requestFocus();
                System.out.println("TestTable focus restored after move up step");
            });
            System.out.println("Moved step up, from: " + selectedIndex + " to: " + (selectedIndex - 1));
        });

        // Move Down button action
        moveDownBtn.setOnAction(e -> {
            int selectedIndex = testTable.getSelectionModel().getSelectedIndex();
            if (selectedIndex == -1) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Row Selected");
                alert.setHeaderText(null);
                alert.setContentText("Please select a row to move");
                alert.showAndWait();
                return;
            }
            if (selectedIndex == testData.size() - 1) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Invalid Action");
                alert.setHeaderText(null);
                alert.setContentText("Cannot move the last row of the table");
                alert.showAndWait();
                return;
            }
            TestStep temp = testData.get(selectedIndex);
            testData.remove(selectedIndex);
            testData.add(selectedIndex + 1, temp);
            testTable.getSelectionModel().select(selectedIndex + 1);
            testTable.scrollTo(selectedIndex + 1);
            isTableModified = true;
            updateTestStepNumbers(testData);
            testTable.refresh();
            Platform.runLater(() -> {
                testTable.requestFocus();
                System.out.println("TestTable focus restored after move down step");
            });
            System.out.println("Moved step down, from: " + selectedIndex + " to: " + (selectedIndex + 1));
        });

        // Add Element button action
        addElementBtn.setOnAction(e -> {
            Element newElement = new Element("", "", "", "");
            elementData.add(newElement);
            int newRowIndex = elementData.size() - 1;
            elementTable.getSelectionModel().select(newRowIndex);
            elementTable.scrollTo(newRowIndex);
            isTableModified = true;
            elementTable.refresh();
        });

        // Move Up Element button action
        moveUpElementBtn.setOnAction(e -> {
            int selectedIndex = elementTable.getSelectionModel().getSelectedIndex();
            if (selectedIndex == -1) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Row Selected");
                alert.setHeaderText(null);
                alert.setContentText("Please select a row to move");
                alert.showAndWait();
                return;
            }
            if (selectedIndex == 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Invalid Action");
                alert.setHeaderText(null);
                alert.setContentText("Cannot move the first row of the table");
                alert.showAndWait();
                return;
            }
            Element temp = elementData.get(selectedIndex);
            elementData.remove(selectedIndex);
            elementData.add(selectedIndex - 1, temp);
            elementTable.getSelectionModel().select(selectedIndex - 1);
            elementTable.scrollTo(selectedIndex - 1);
            isTableModified = true;
            elementTable.refresh();
            Platform.runLater(() -> {
                elementTable.requestFocus();
                System.out.println("ElementTable focus restored after move up element");
            });
            System.out.println("Moved element up, from: " + selectedIndex + " to: " + (selectedIndex - 1));
        });

        // Move Down Element button action
        moveDownElementBtn.setOnAction(e -> {
            int selectedIndex = elementTable.getSelectionModel().getSelectedIndex();
            if (selectedIndex == -1) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Row Selected");
                alert.setHeaderText(null);
                alert.setContentText("Please select a row to move");
                alert.showAndWait();
                return;
            }
            if (selectedIndex == elementData.size() - 1) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Invalid Action");
                alert.setHeaderText(null);
                alert.setContentText("Cannot move the last row of the table");
                alert.showAndWait();
                return;
            }
            Element temp = elementData.get(selectedIndex);
            elementData.remove(selectedIndex);
            elementData.add(selectedIndex + 1, temp);
            elementTable.getSelectionModel().select(selectedIndex + 1);
            elementTable.scrollTo(selectedIndex + 1);
            isTableModified = true;
            elementTable.refresh();
            Platform.runLater(() -> {
                elementTable.requestFocus();
                System.out.println("ElementTable focus restored after move down element");
            });
            System.out.println("Moved element down, from: " + selectedIndex + " to: " + (selectedIndex + 1));
        });

        // Delete Element button action
        deleteElementBtn.setOnAction(e -> {
            Element selected = elementTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Row Selected");
                alert.setHeaderText(null);
                alert.setContentText("Please select a row to delete");
                alert.showAndWait();
                return;
            }
            int index = elementData.indexOf(selected);
            if (index >= 0) {
                elementData.remove(index);
                if (elementData.size() > 0) {
                    int newSelection = Math.min(index, elementData.size() - 1);
                    elementTable.getSelectionModel().select(newSelection);
                    elementTable.scrollTo(newSelection);
                } else {
                    elementTable.getSelectionModel().clearSelection();
                }
                isTableModified = true;
                elementTable.refresh();
                Platform.runLater(() -> {
                    elementTable.requestFocus();
                    System.out.println("ElementTable focus restored after delete element");
                });
                System.out.println("Deleted element, index: " + index);
            }
        });

        // Save button action
        saveBtn.setOnAction(e -> {
            if (testData.isEmpty() && elementData.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Save Info");
                alert.setHeaderText(null);
                alert.setContentText("No data to save.");
                alert.showAndWait();
                return;
            }
            if (isTableModified) {
                if (lastUsedFile == null) {
                    showSaveDialog(primaryStage, testData, elementData);
                } else {
                    showSaveOptionsDialog(primaryStage, lastUsedFile, testData, elementData);
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Save Info");
                alert.setHeaderText(null);
                alert.setContentText("No unsaved changes.");
                alert.showAndWait();
            }
            Platform.runLater(() -> {
                elementTable.requestFocus();
                System.out.println("ElementTable focus restored after save");
            });
        });

        // Load Test button action
        loadTestBtn.setOnAction(e -> {
            if (isTableModified) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Unsaved Changes");
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText("You have unsaved changes. Proceed with loading?");
                ButtonType yes = new ButtonType("Yes");
                ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirmAlert.getButtonTypes().setAll(yes, no);
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == yes) {
                        loadFile(primaryStage, testData, elementData, testTable, elementTable);
                    }
                });
            } else {
                loadFile(primaryStage, testData, elementData, testTable, elementTable);
            }
            Platform.runLater(() -> {
                testTable.requestFocus();
                System.out.println("TestTable focus restored after load attempt");
            });
        });

        // Clear Test button action
        clearTestBtn.setOnAction(e -> {
            if (testData.isEmpty() && elementData.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Clear Info");
                alert.setHeaderText(null);
                alert.setContentText("No data to clear.");
                alert.showAndWait();
                return;
            }
            if (isTableModified) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Unsaved Changes");
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText("You have unsaved changes. Save before clearing?");
                ButtonType yes = new ButtonType("Save");
                ButtonType no = new ButtonType("Discard");
                ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirmAlert.getButtonTypes().setAll(yes, no, cancel);
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == yes) {
                        if (lastUsedFile == null) {
                            if (showSaveDialog(primaryStage, testData, elementData)) {
                                testData.clear();
                                elementData.clear();
                                updateTestStepNumbers(testData);
                                isTableModified = false;
                                lastUsedFile = null;
                                testTable.refresh();
                                elementTable.refresh();
                            }
                        } else {
                            if (showSaveOptionsDialog(primaryStage, lastUsedFile, testData, elementData)) {
                                testData.clear();
                                elementData.clear();
                                updateTestStepNumbers(testData);
                                isTableModified = false;
                                testTable.refresh();
                                elementTable.refresh();
                            }
                        }
                    } else if (response == no) {
                        testData.clear();
                        elementData.clear();
                        updateTestStepNumbers(testData);
                        isTableModified = false;
                        lastUsedFile = null;
                        testTable.refresh();
                        elementTable.refresh();
                    }
                });
            } else {
                testData.clear();
                elementData.clear();
                updateTestStepNumbers(testData);
                isTableModified = false;
                lastUsedFile = null;
                testTable.refresh();
                elementTable.refresh();
            }
            Platform.runLater(() -> {
                elementTable.requestFocus();
                System.out.println("ElementTable focus restored after clear");
            });
            System.out.println("Cleared all data, testData size: " + testData.size() + ", elementData size: " + elementData.size());
        });

        // Group test step buttons and file actions in a VBox
        VBox testButtonBox = new VBox(5, addStepBtn, addUpBtn, addDownBtn, 
                                      deleteStepBtn, moveUpBtn, moveDownBtn, 
                                      saveBtn, loadTestBtn, clearTestBtn);
        testButtonBox.setAlignment(Pos.TOP_RIGHT);
        testButtonBox.setPrefWidth(100);
        testButtonBox.setPadding(new Insets(0, 0, 0, 0));

        // Group element buttons in a separate VBox
        VBox elementButtonBox = new VBox(5, addElementBtn, moveUpElementBtn, 
                                        moveDownElementBtn, deleteElementBtn);
        elementButtonBox.setAlignment(Pos.TOP_RIGHT);
        elementButtonBox.setPrefWidth(100);

        // Create HBox for test table and buttons
        HBox testBox = new HBox(10, testTable, testButtonBox);
        testBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(testTable, Priority.ALWAYS);
        HBox.setMargin(testButtonBox, new Insets(0, 0, 0, 0));

        // Create HBox for element table and its buttons
        HBox elementBox = new HBox(10, elementTable, elementButtonBox);
        elementBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(elementTable, Priority.ALWAYS);
        HBox.setMargin(elementButtonBox, new Insets(0, 0, 0, 0));

        // Create label for test steps table
        Label testTableLabel = new Label("Create/Modified Test");
        testTableLabel.setStyle("-fx-text-fill: " + DEFAULT_FOREGROUND + "; " +
                               "-fx-font-family: 'Arial'; " +
                               "-fx-font-weight: bold; " +
                               "-fx-font-size: 14px;");
        testTableLabel.setPadding(new Insets(0, 0, 5, 0));

        // Main layout using BorderPane with VBox for tables and label
        VBox tableBox = new VBox(10, testTableLabel, testBox, elementBox);
        tableBox.setAlignment(Pos.TOP_LEFT);
        tableBox.setPadding(new Insets(10));
        testTable.prefHeightProperty().bind(Bindings.createDoubleBinding(() -> {
            double sceneHeight = primaryStage.getScene() != null ? primaryStage.getScene().getHeight() : 600;
            return (sceneHeight - 30 - 10 - 20) * 0.7;
        }, primaryStage.sceneProperty(), primaryStage.heightProperty()));
        elementTable.prefHeightProperty().bind(Bindings.createDoubleBinding(() -> {
            double sceneHeight = primaryStage.getScene() != null ? primaryStage.getScene().getHeight() : 600;
            return (sceneHeight - 30 - 10 - 20) * 0.3;
        }, primaryStage.sceneProperty(), primaryStage.heightProperty()));
        VBox.setVgrow(testBox, Priority.ALWAYS);
        VBox.setVgrow(elementBox, Priority.ALWAYS);
        BorderPane root = new BorderPane();
        root.setCenter(tableBox);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #1E1E1E;");

        // Create scene
        Scene scene = new Scene(root, 800, 600);

        // Add scene-level event filter for elementTable navigation
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (elementTable.isFocused() && (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN)) {
                int selectedIndex = elementTable.getSelectionModel().getSelectedIndex();
                if (event.getCode() == KeyCode.UP && selectedIndex > 0) {
                    elementTable.getSelectionModel().clearAndSelect(selectedIndex - 1);
                    elementTable.scrollTo(selectedIndex - 1);
                    System.out.println("ElementTable: Moved up to index " + (selectedIndex - 1));
                } else if (event.getCode() == KeyCode.DOWN && selectedIndex < elementTable.getItems().size() - 1) {
                    elementTable.getSelectionModel().clearAndSelect(selectedIndex + 1);
                    elementTable.scrollTo(selectedIndex + 1);
                    System.out.println("ElementTable: Moved down to index " + (selectedIndex + 1));
                }
                event.consume();
            }
        });

        // Set up stage
        Image backgroundImage = new Image("file:" + UIConstants.UI_ICON);
        primaryStage.getIcons().add(backgroundImage);
        primaryStage.setTitle("Create/Modified Test");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void updateTestStepNumbers(ObservableList<TestStep> testData) {
        int step = 1;
        for (int i = 0; i < testData.size(); i++) {
            TestStep item = testData.get(i);
            String currentTestId = item.getTestId() != null ? item.getTestId() : "";
            if (!currentTestId.isEmpty() && !currentTestId.equals("#")) {
                step = 1;
            }
            item.setTestStep(String.valueOf(step));
            // Update hasBorder based on non-empty fields
            item.setHasBorder(!currentTestId.isEmpty() && !currentTestId.equals("#") ||
                             !item.getTestAction().isEmpty() ||
                             !item.getTestElement().isEmpty() ||
                             !item.getTestData().isEmpty() ||
                             !item.getTestDescription().isEmpty());
            step++;
        }
        System.out.println("Updated test step numbers, testData size: " + testData.size());
    }

    private static Button createStyledButton(String text, String tooltip) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        button.setPrefSize(100, 25);
        button.setMinSize(100, 25);
        button.setMaxSize(100, 25);
        button.setStyle("-fx-background-color: " + ENABLED_COLOR + "; -fx-text-fill: black;");
        button.setOnMouseEntered(e -> {
            if (!button.isDisabled()) {
                button.setStyle("-fx-background-color: " + HOVER_COLOR + "; -fx-text-fill: black;");
            }
        });
        button.setOnMouseExited(e -> {
            if (!button.isDisabled()) {
                button.setStyle("-fx-background-color: " + ENABLED_COLOR + "; -fx-text-fill: black;");
            }
        });
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private static boolean showSaveOptionsDialog(Stage stage, File file, ObservableList<TestStep> testData, ObservableList<Element> elementData) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Save Options");
        alert.setHeaderText(null);
        alert.setContentText("Choose an option to save the table:");
        ButtonType save = new ButtonType("Save");
        ButtonType saveAs = new ButtonType("Save As");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(save, saveAs, cancel);
        boolean saveSuccessful = false;
        if (alert.showAndWait().isPresent()) {
            ButtonType response = alert.getResult();
            if (response == save) {
                saveSuccessful = saveFile(file, testData, elementData);
            } else if (response == saveAs) {
                saveSuccessful = showSaveDialog(stage, testData, elementData);
            }
        }
        return saveSuccessful;
    }

    private static boolean showSaveDialog(Stage stage, ObservableList<TestStep> testData, ObservableList<Element> elementData) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Table as JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialDirectory(Commons.getDocumentsDirectory());
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            String filePath = file.getAbsolutePath();
            File finalFile = filePath.toLowerCase().endsWith(".json") ? file : new File(filePath + ".json");
            if (finalFile.exists()) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Overwrite File");
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText("File already exists. Overwrite?");
                ButtonType yes = new ButtonType("Yes");
                ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirmAlert.getButtonTypes().setAll(yes, no);
                if (confirmAlert.showAndWait().orElse(no) == yes) {
                    if (saveFile(finalFile, testData, elementData)) {
                        lastUsedFile = finalFile;
                        isTableModified = false;
                        return true;
                    }
                }
                return false;
            } else {
                if (saveFile(finalFile, testData, elementData)) {
                    lastUsedFile = finalFile;
                    isTableModified = false;
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean saveFile(File file, ObservableList<TestStep> testData, ObservableList<Element> elementData) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            boolean hasTestCases = !testData.isEmpty();
            boolean hasElements = !elementData.isEmpty();

            if (hasTestCases) {
                appendTestCases(sb, testData);
                if (hasElements) {
                    sb.append(",\n");
                }
            }

            if (hasElements) {
                appendElementList(sb, elementData);
            }

            sb.append("]\n");

            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(sb.toString());
                fileWriter.flush();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Save Successful");
                alert.setHeaderText(null);
                alert.setContentText("Table saved successfully to " + file.getAbsolutePath());
                alert.showAndWait();
                isTableModified = false;
                return true;
            }
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText(null);
            alert.setContentText("Error saving file: " + ex.getMessage());
            alert.showAndWait();
            return false;
        } catch (JSONException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText(null);
            alert.setContentText("Error creating JSON: " + ex.getMessage());
            alert.showAndWait();
            return false;
        }
    }

    private static void appendTestCases(StringBuilder sb, ObservableList<TestStep> testData) {
        sb.append("  {\n");
        sb.append("    \"Test_Cases\": [\n");
        
        String currentTestId = "";
        JSONArray stepsArray = null;
        LinkedHashMap<String, Object> testCaseMap = null;
        int currentTestCaseIndex = 0;
        
        // Count test cases to handle comma placement
        int testCaseCount = 0;
        for (TestStep step : testData) {
            String testId = step.getTestId();
            if (!testId.isEmpty() && !testId.equals("#")) {
                testCaseCount++;
            }
        }

        for (int i = 0; i < testData.size(); i++) {
            TestStep step = testData.get(i);
            String testId = step.getTestId();
            String testStep = step.getTestStep();
            String testAction = step.getTestAction();
            String testElement = step.getTestElement();
            String testDataStr = step.getTestData();
            String testDescription = step.getTestDescription();

            if (!testId.isEmpty() && !testId.equals("#")) {
                if (testCaseMap != null) {
                    testCaseMap.put("Steps", stepsArray);
                    appendTestCase(sb, testCaseMap, currentTestCaseIndex < testCaseCount - 1);
                    currentTestCaseIndex++;
                }
                testCaseMap = new LinkedHashMap<>();
                testCaseMap.put("Test_Id", testId);
                stepsArray = new JSONArray();
                currentTestId = testId;
            }

            if (testCaseMap != null) {
                JSONObject stepObject = new JSONObject();
                stepObject.put("Test_Step", testStep);
                stepObject.put("Test_Action", testAction);
                stepObject.put("Test_Element", testElement);
                stepObject.put("Test_Data", testDataStr);
                stepObject.put("Test_Description", testDescription);
                stepsArray.put(stepObject);
            }

            // Handle the last test case
            if (i == testData.size() - 1 && testCaseMap != null) {
                testCaseMap.put("Steps", stepsArray);
                appendTestCase(sb, testCaseMap, false);
            }
        }

        sb.append("    ]\n");
        sb.append("  }");
    }

    private static void appendTestCase(StringBuilder sb, LinkedHashMap<String, Object> testCaseMap, boolean addComma) {
        sb.append("      {\n");
        sb.append("        \"Test_Id\": ").append(JSONObject.quote((String) testCaseMap.get("Test_Id"))).append(",\n");
        sb.append("        \"Steps\": [\n");

        JSONArray steps = (JSONArray) testCaseMap.get("Steps");
        for (int i = 0; i < steps.length(); i++) {
            JSONObject step = steps.getJSONObject(i);
            sb.append("          {\n");
            sb.append("            \"Test_Step\": ").append(JSONObject.quote(step.getString("Test_Step"))).append(",\n");
            sb.append("            \"Test_Action\": ").append(JSONObject.quote(step.getString("Test_Action"))).append(",\n");
            sb.append("            \"Test_Element\": ").append(JSONObject.quote(step.getString("Test_Element"))).append(",\n");
            sb.append("            \"Test_Data\": ").append(JSONObject.quote(step.getString("Test_Data"))).append(",\n");
            sb.append("            \"Test_Description\": ").append(JSONObject.quote(step.getString("Test_Description"))).append("\n");
            sb.append("          }");
            if (i < steps.length() - 1) {
                sb.append(",\n");
            } else {
                sb.append("\n");
            }
        }

        sb.append("        ]\n");
        sb.append("      }");
        if (addComma) {
            sb.append(",");
        }
        sb.append("\n");
    }

    private static void appendElementList(StringBuilder sb, ObservableList<Element> elementData) {
        TreeSet<String> sortedElementNames = new TreeSet<>();
        LinkedHashMap<String, JSONArray> elementMap = new LinkedHashMap<>();
        for (Element element : elementData) {
            String elementName = element.getElementName();
            if (!elementName.isEmpty()) {
                sortedElementNames.add(elementName);
                JSONArray elementArray = new JSONArray();
                elementArray.put(element.getAutomationId());
                elementArray.put(element.getName());
                elementArray.put(element.getXpath());
                elementMap.put(elementName, elementArray);
            }
        }

        sb.append("  {\n");
        sb.append("    \"Element_List\": {\n");
        boolean first = true;
        for (String name : sortedElementNames) {
            if (!first) {
                sb.append(",\n");
            }
            JSONArray arr = elementMap.get(name);
            sb.append("      \"").append(name).append("\": [\n");
            for (int j = 0; j < arr.length(); j++) {
                sb.append("        ").append(JSONObject.quote(arr.getString(j)));
                if (j < arr.length() - 1) {
                    sb.append(",\n");
                } else {
                    sb.append("\n");
                }
            }
            sb.append("      ]");
            first = false;
        }
        sb.append("\n    }\n");
        sb.append("  }");
    }

    private static void loadFile(Stage stage, ObservableList<TestStep> testData, ObservableList<Element> elementData, TableView<TestStep> testTable, TableView<Element> elementTable) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Table from JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialDirectory(Commons.getDocumentsDirectory());
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try (FileReader fileReader = new FileReader(file)) {
                StringBuilder content = new StringBuilder();
                int ch;
                while ((ch = fileReader.read()) != -1) {
                    content.append((char) ch);
                }
                JSONArray rootArray = new JSONArray(content.toString());
                testData.clear();
                elementData.clear();
                
                // Collect valid element names first
                Set<String> validElementNames = new TreeSet<>();
                for (int i = 0; i < rootArray.length(); i++) {
                    JSONObject obj = rootArray.getJSONObject(i);
                    if (obj.has("Element_List")) {
                        JSONObject elementList = obj.getJSONObject("Element_List");
                        for (String key : elementList.keySet()) {
                            validElementNames.add(key);
                            JSONArray elementArray = elementList.getJSONArray(key);
                            String elementName = key;
                            String automationId = elementArray.length() > 0 ? elementArray.getString(0) : "";
                            String name = elementArray.length() > 1 ? elementArray.getString(1) : "";
                            String xpath = elementArray.length() > 2 ? elementArray.getString(2) : "";
                            elementData.add(new Element(elementName, automationId, name, xpath));
                        }
                    }
                }

                // Load test cases
                for (int i = 0; i < rootArray.length(); i++) {
                    JSONObject obj = rootArray.getJSONObject(i);
                    if (obj.has("Test_Cases")) {
                        JSONArray testCases = obj.getJSONArray("Test_Cases");
                        for (int j = 0; j < testCases.length(); j++) {
                            JSONObject testCase = testCases.getJSONObject(j);
                            String testId = testCase.getString("Test_Id");
                            JSONArray steps = testCase.getJSONArray("Steps");
                            for (int k = 0; k < steps.length(); k++) {
                                JSONObject step = steps.getJSONObject(k);
                                String testStep = step.getString("Test_Step");
                                String testAction = step.getString("Test_Action");
                                String testElement = step.getString("Test_Element");
                                if (!testElement.isEmpty() && !validElementNames.contains(testElement)) {
                                    testElement = "";
                                }
                                String testDataStr = step.getString("Test_Data");
                                String testDescription = step.getString("Test_Description");
                                // Set Test_Id only for the first step of each test case
                                String stepTestId = (k == 0) ? testId : "";
                                boolean hasBorder = !stepTestId.isEmpty() && !stepTestId.equals("#") ||
                                                    !testAction.isEmpty() ||
                                                    !testElement.isEmpty() ||
                                                    !testDataStr.isEmpty() ||
                                                    !testDescription.isEmpty();
                                TestStep newStep = new TestStep(stepTestId, testStep, testAction, testElement, testDataStr, testDescription, hasBorder);
                                testData.add(newStep);
                            }
                        }
                    }
                }

                updateTestStepNumbers(testData);
                lastUsedFile = file;
                isTableModified = false;
                testTable.refresh();
                elementTable.refresh();
                elementTable.setFocusTraversable(true);
                Platform.runLater(() -> {
                    if (!testData.isEmpty()) {
                        testTable.getSelectionModel().select(0);
                        testTable.scrollTo(0);
                    }
                    if (!elementData.isEmpty()) {
                        elementTable.getSelectionModel().select(0);
                        elementTable.scrollTo(0);
                    }
                    elementTable.requestFocus();
                    System.out.println("ElementTable focus restored after load");
                });
                System.out.println("Loaded file, testData size: " + testData.size() + ", elementData size: " + elementData.size());
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Load Successful");
                alert.setHeaderText(null);
                alert.setContentText("Table loaded successfully from " + file.getAbsolutePath());
                alert.showAndWait();
            } catch (IOException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Load Error");
                alert.setHeaderText(null);
                alert.setContentText("Error reading file: " + ex.getMessage());
                alert.showAndWait();
            } catch (JSONException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Load Error");
                alert.setHeaderText(null);
                alert.setContentText("Invalid JSON format: " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}