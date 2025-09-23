package com.test.window.gui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Factory class for creating and styling the TestCase table.
 * This encapsulates the table creation, columns, and styling logic.
 */
public class TestTableFactory {
    // Color constants
    private static final String TABLE_BACKGROUND = "#323232"; // Dark gray
    private static final String RUNNING_HIGHLIGHT = "#0788b3"; // Light blue
    private static final String STATUS_PASSED = "#00FF00"; // Green
    private static final String STATUS_FAILED = "#FF6666"; // Rose
    private static final String DEFAULT_FOREGROUND = "#C8C8C8"; // Light gray
    private static final String CHECKBOX_ENABLED_COLOR = "#C8C8C8"; // Light gray
    private static final String DISABLED_COLOR = "#646464";
    private static final String HOVER_COLOR = "#288A48";
    private static final String BORDER_COLOR = DISABLED_COLOR; // Use DISABLED_COLOR for borders
    
    /**
     * Creates a configured TableView for TestCases, including columns, styling, and Select All checkbox.
     * @param tableData The ObservableList of TestCase data.
     * @return A VBox containing the Select All checkbox and the TableView.
     */
    public static VBox createTestTable(ObservableList<TestRunner.TestCase> tableData) {
        TableView<TestRunner.TestCase> tableView = new TableView<>(tableData);

        // Table columns
        TableColumn<TestRunner.TestCase, Boolean> runCol = new TableColumn<>("Run");
        runCol.setCellValueFactory(cellData -> cellData.getValue().runProperty());
        runCol.setCellFactory(CheckBoxTableCell.forTableColumn(runCol));
        runCol.setEditable(true);
        runCol.setMinWidth(40);
        runCol.setMaxWidth(40);
        runCol.setPrefWidth(40);
        // Add border to Run column cells with reduced thickness
        runCol.setCellFactory(column -> new CheckBoxTableCell<TestRunner.TestCase, Boolean>() {
            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("-fx-background-color: transparent; -fx-alignment: CENTER;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0.5; -fx-alignment: CENTER;");
                }
            }
        });

        TableColumn<TestRunner.TestCase, String> idCol = new TableColumn<>("Test Id");
        idCol.setCellValueFactory(cellData -> cellData.getValue().testIdProperty());
        idCol.setMinWidth(80);
        idCol.setMaxWidth(80);
        idCol.setPrefWidth(80);
        // Add border to Test Id column cells with reduced thickness
        idCol.setCellFactory(column -> new TableCell<TestRunner.TestCase, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-text-fill: " + DEFAULT_FOREGROUND + "; -fx-alignment: CENTER;");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: transparent; -fx-text-fill: " + DEFAULT_FOREGROUND + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0.5; -fx-alignment: CENTER;");
                }
            }
        });

        TableColumn<TestRunner.TestCase, String> descCol = new TableColumn<>("Test Description");
        descCol.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descCol.setMinWidth(150);
        descCol.setPrefWidth(300);
        // Add border to Test Description column cells with reduced thickness
        descCol.setCellFactory(column -> new TableCell<TestRunner.TestCase, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-text-fill: " + DEFAULT_FOREGROUND + "; -fx-alignment: CENTER_LEFT;");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: transparent; -fx-text-fill: " + DEFAULT_FOREGROUND + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0.5; -fx-alignment: CENTER_LEFT;");
                }
            }
        });

        TableColumn<TestRunner.TestCase, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setMinWidth(80);
        statusCol.setMaxWidth(80);
        statusCol.setPrefWidth(80);
        // Add border to Status column cells with reduced thickness
        statusCol.setCellFactory(column -> new TableCell<TestRunner.TestCase, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-text-fill: " + DEFAULT_FOREGROUND + "; -fx-alignment: CENTER;");
                } else {
                    setText(status);
                    String textColor = switch (status) {
                        case "Running" -> "black";
                        case "Passed" -> STATUS_PASSED;
                        case "Failed" -> STATUS_FAILED;
                        default -> DEFAULT_FOREGROUND;
                    };
                    setStyle("-fx-background-color: transparent; -fx-text-fill: " + textColor + " !important; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0.5; -fx-alignment: CENTER;");
                }
            }
        });

        // Custom row factory to highlight the currently executing row, manually selected row, or first row by default
        tableView.setRowFactory(tv -> new TableRow<TestRunner.TestCase>() {
            @Override
            protected void updateItem(TestRunner.TestCase item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("-fx-background-color: " + TABLE_BACKGROUND + ";");
                } else {
                    String backgroundColor = TABLE_BACKGROUND;
                    // Prioritize the currently executing test
                    if (item == TestRunner.getCurrentlyExecutingTest()) {
                        backgroundColor = RUNNING_HIGHLIGHT;
                    } else if (TestRunner.getCurrentlyExecutingTest() == null) {
                        // Highlight manually selected row or first row if no selection
                        if (isSelected()) {
                            backgroundColor = RUNNING_HIGHLIGHT;
                        } else if (getIndex() == 0 && !tableData.isEmpty() && 
                                   tableView.getSelectionModel().getSelectedIndices().isEmpty()) {
                            backgroundColor = RUNNING_HIGHLIGHT;
                        }
                    }
                    setStyle("-fx-background-color: " + backgroundColor + " !important;");
                }
            }
        });

        tableView.getColumns().addAll(runCol, idCol, descCol, statusCol);
        // Enhanced TableView styling, no grid lines
        tableView.setStyle("-fx-background-color: " + TABLE_BACKGROUND + "; -fx-control-inner-background: " + 
                           TABLE_BACKGROUND + "; -fx-table-cell-border-color: transparent; " +
                           "-fx-horizontal-grid-lines-visible: false; -fx-vertical-grid-lines-visible: false; " +
                           "-fx-selection-bar: " + RUNNING_HIGHLIGHT + "; -fx-selection-bar-non-focused: " + RUNNING_HIGHLIGHT + ";");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Select All checkbox
        CheckBox selectAllCheckBox = new CheckBox("Select All");
        selectAllCheckBox.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12));
        selectAllCheckBox.setTextFill(javafx.scene.paint.Color.web(CHECKBOX_ENABLED_COLOR));
        selectAllCheckBox.setDisable(true);

        HBox checkBoxBox = new HBox(selectAllCheckBox);
        checkBoxBox.setAlignment(Pos.CENTER_LEFT);
        checkBoxBox.setPadding(new Insets(0, 0, 5, 8));

        // Layout
        VBox tableBox = new VBox(checkBoxBox, tableView);
        tableBox.setSpacing(5);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        return tableBox;
    }
}