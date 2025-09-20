package com.test.window.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class TableFactory {

    // Color constants
    private static final String TABLE_BACKGROUND = "#323232";
    private static final String DISABLED_COLOR = "#646464";
    private static final String HOVER_COLOR = "#288A48";
    private static final String SELECTION_COLOR = "#0788b3"; // Light blue for row selector

    public TableView<TestManagerApp.TestStep> createTestTable(ObservableList<TestManagerApp.TestStep> testData, 
                                                             ObservableList<String> elementNameItems) {
        TableView<TestManagerApp.TestStep> testTable = new TableView<>();
        testTable.setId("testTable"); // Set table ID
        testTable.setItems(testData);
        testTable.setEditable(true);
        testTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        testTable.setStyle("-fx-background-color: " + TABLE_BACKGROUND + "; -fx-control-inner-background: " + 
                TABLE_BACKGROUND + "; -fx-table-cell-border-color: " + TABLE_BACKGROUND + 
                "; -fx-horizontal-grid-lines-visible: true; -fx-vertical-grid-lines-visible: true;" +
                " -fx-selection-bar: " + SELECTION_COLOR + "; -fx-selection-bar-non-focused: " + SELECTION_COLOR + ";");
        testTable.getSelectionModel().setCellSelectionEnabled(false); // Ensure row-based selection

        // Create columns
        TableColumn<TestManagerApp.TestStep, String> idColumn = new TableColumn<>("Test ID");
        idColumn.setId("testId");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("testId"));
        idColumn.setCellFactory(col -> new TestManagerApp.CustomTextFieldTableCell(6, true));
        idColumn.setOnEditCommit(event -> {
            TestManagerApp.TestStep item = event.getRowValue();
            if (item != null) {
                item.setTestId(event.getNewValue());
                TestManagerApp.updateTestStepNumbers(testData);
            }
        });
        idColumn.setSortable(false);
        idColumn.setMinWidth(80);
        idColumn.setMaxWidth(80);
        idColumn.setPrefWidth(80);
        idColumn.setReorderable(false);

        TableColumn<TestManagerApp.TestStep, String> stepColumn = new TableColumn<>("Test Step");
        stepColumn.setId("testStep"); // Ensure the ID is set for identification
        stepColumn.setCellValueFactory(new PropertyValueFactory<>("testStep"));
        stepColumn.setCellFactory(col -> new TestManagerApp.CustomTextFieldTableCell(0, false)); // Use CustomTextFieldTableCell
        stepColumn.setOnEditCommit(event -> {
            TestManagerApp.TestStep item = event.getRowValue();
            if (item != null) {
                item.setTestStep(event.getNewValue());
                TestManagerApp.updateTestStepNumbers(testData); // Update step numbers if needed
            }
        });
        stepColumn.setEditable(false);
        stepColumn.setSortable(false);
        stepColumn.setMinWidth(80);
        stepColumn.setMaxWidth(80);
        stepColumn.setPrefWidth(80);
        stepColumn.setReorderable(false);

        TableColumn<TestManagerApp.TestStep, String> actionColumn = new TableColumn<>("Test Action");
        actionColumn.setId("testAction");
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("testAction"));
        ObservableList<String> actionItems = FXCollections.observableArrayList(
            "", "OPEN_WINDOW", "SET", "CLICK", "TAKE_SCREENSHOT", "ASSERT_TEXT", "CLOSE_WINDOW");
        actionColumn.setCellFactory(col -> new TestManagerApp.CustomComboBoxTableCell<>(actionItems));
        actionColumn.setOnEditCommit(event -> {
            TestManagerApp.TestStep item = event.getRowValue();
            if (item != null) {
                String newValue = event.getNewValue() != null ? event.getNewValue() : "";
                item.setTestAction(newValue);
                System.out.println("Test Action updated to: " + newValue + " for row: " + testData.indexOf(item));
            }
        });
        actionColumn.setEditable(true);
        actionColumn.setSortable(false);
        actionColumn.setReorderable(false);

        TableColumn<TestManagerApp.TestStep, String> testElementColumn = new TableColumn<>("Test Element");
        testElementColumn.setId("testElement");
        testElementColumn.setCellValueFactory(new PropertyValueFactory<>("testElement"));
        testElementColumn.setCellFactory(col -> new TestManagerApp.CustomComboBoxTableCell<>(elementNameItems));
        testElementColumn.setOnEditCommit(event -> {
            TestManagerApp.TestStep item = event.getRowValue();
            if (item != null) {
                String newValue = event.getNewValue() != null ? event.getNewValue() : "";
                item.setTestElement(newValue);
                System.out.println("Test Element updated to: " + newValue + " for row: " + testData.indexOf(item));
            }
        });
        testElementColumn.setEditable(true);
        testElementColumn.setSortable(false);
        testElementColumn.setReorderable(false);

        TableColumn<TestManagerApp.TestStep, String> dataColumn = new TableColumn<>("Test Data");
        dataColumn.setId("testData");
        dataColumn.setCellValueFactory(new PropertyValueFactory<>("testData"));
        dataColumn.setCellFactory(col -> new TestManagerApp.CustomTextFieldTableCell(0, false));
        dataColumn.setOnEditCommit(event -> {
            TestManagerApp.TestStep item = event.getRowValue();
            if (item != null) {
                item.setTestData(event.getNewValue());
            }
        });
        dataColumn.setSortable(false);
        dataColumn.setReorderable(false);

        TableColumn<TestManagerApp.TestStep, String> descColumn = new TableColumn<>("Test Description");
        descColumn.setId("testDescription");
        descColumn.setCellValueFactory(new PropertyValueFactory<>("testDescription"));
        descColumn.setCellFactory(col -> new TestManagerApp.CustomTextFieldTableCell(0, false));
        descColumn.setOnEditCommit(event -> {
            TestManagerApp.TestStep item = event.getRowValue();
            if (item != null) {
                item.setTestDescription(event.getNewValue());
            }
        });
        descColumn.setSortable(false);
        descColumn.setMinWidth(150);
        descColumn.setPrefWidth(300);
        descColumn.setReorderable(false);

        testTable.getColumns().addAll(idColumn, stepColumn, actionColumn, testElementColumn, dataColumn, descColumn);

        // Enable single-click editing
        testTable.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                TablePosition<TestManagerApp.TestStep, ?> pos = testTable.getSelectionModel().getSelectedCells().stream().findFirst().orElse(null);
                if (pos != null) {
                    TableColumn<TestManagerApp.TestStep, ?> column = pos.getTableColumn();
                    int row = pos.getRow();
                    TablePosition<TestManagerApp.TestStep, ?> editingCell = testTable.getEditingCell();
                    if (editingCell == null || editingCell.getRow() != row || editingCell.getTableColumn() != column) {
                        if (column.isEditable() && row >= 0 && row < testTable.getItems().size()) {
                            testTable.edit(row, column);
                        }
                    }
                }
            }
        });

        return testTable;
    }

    public TableView<TestManagerApp.Element> createElementTable(ObservableList<TestManagerApp.Element> elementData, 
                                                               ObservableList<String> elementNameItems) {
        TableView<TestManagerApp.Element> elementTable = new TableView<>();
        elementTable.setId("elementTable"); // Set table ID
        elementTable.setItems(elementData);
        elementTable.setEditable(true);
        elementTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        elementTable.setStyle("-fx-background-color: " + TABLE_BACKGROUND + "; -fx-control-inner-background: " + 
                TABLE_BACKGROUND + "; -fx-table-cell-border-color: " + TABLE_BACKGROUND + 
                "; -fx-horizontal-grid-lines-visible: true; -fx-vertical-grid-lines-visible: true;" +
                " -fx-selection-bar: " + SELECTION_COLOR + "; -fx-selection-bar-non-focused: " + SELECTION_COLOR + ";");
        elementTable.getSelectionModel().setCellSelectionEnabled(false); // Ensure row-based selection

        // Create columns
        TableColumn<TestManagerApp.Element, String> elementNameColumn = new TableColumn<>("Element Name");
        elementNameColumn.setId("elementName");
        elementNameColumn.setCellValueFactory(new PropertyValueFactory<>("elementName"));
        elementNameColumn.setCellFactory(col -> new TestManagerApp.ElementTextFieldTableCell(true));
        elementNameColumn.setOnEditCommit(event -> {
            TestManagerApp.Element item = event.getRowValue();
            if (item != null) {
                item.setElementName(event.getNewValue());
                elementData.set(elementData.indexOf(item), item);
            }
        });
        elementNameColumn.setSortable(false);
        elementNameColumn.setReorderable(false);

        TableColumn<TestManagerApp.Element, String> automationIdColumn = new TableColumn<>("Automation ID");
        automationIdColumn.setId("automationId");
        automationIdColumn.setCellValueFactory(new PropertyValueFactory<>("automationId"));
        automationIdColumn.setCellFactory(col -> new TestManagerApp.ElementTextFieldTableCell(false));
        automationIdColumn.setOnEditCommit(event -> {
            TestManagerApp.Element item = event.getRowValue();
            if (item != null) {
                item.setAutomationId(event.getNewValue());
            }
        });
        automationIdColumn.setSortable(false);
        automationIdColumn.setReorderable(false);

        TableColumn<TestManagerApp.Element, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setId("name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setCellFactory(col -> new TestManagerApp.ElementTextFieldTableCell(false));
        nameColumn.setOnEditCommit(event -> {
            TestManagerApp.Element item = event.getRowValue();
            if (item != null) {
                item.setName(event.getNewValue());
            }
        });
        nameColumn.setSortable(false);
        nameColumn.setReorderable(false);

        TableColumn<TestManagerApp.Element, String> xpathColumn = new TableColumn<>("Xpath");
        xpathColumn.setId("xpath");
        xpathColumn.setCellValueFactory(new PropertyValueFactory<>("xpath"));
        xpathColumn.setCellFactory(col -> new TestManagerApp.ElementTextFieldTableCell(false));
        xpathColumn.setOnEditCommit(event -> {
            TestManagerApp.Element item = event.getRowValue();
            if (item != null) {
                item.setXpath(event.getNewValue());
            }
        });
        xpathColumn.setSortable(false);
        xpathColumn.setReorderable(false);

        elementTable.getColumns().addAll(elementNameColumn, automationIdColumn, nameColumn, xpathColumn);

        // Enable single-click editing
        elementTable.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                TablePosition<TestManagerApp.Element, ?> pos = elementTable.getSelectionModel().getSelectedCells().stream().findFirst().orElse(null);
                if (pos != null) {
                    TableColumn<TestManagerApp.Element, ?> column = pos.getTableColumn();
                    int row = pos.getRow();
                    TablePosition<TestManagerApp.Element, ?> editingCell = elementTable.getEditingCell();
                    if (editingCell == null || editingCell.getRow() != row || editingCell.getTableColumn() != column) {
                        if (column.isEditable() && row >= 0 && row < elementTable.getItems().size()) {
                            elementTable.edit(row, column);
                        }
                    }
                }
            }
        });

        return elementTable;
    }
}