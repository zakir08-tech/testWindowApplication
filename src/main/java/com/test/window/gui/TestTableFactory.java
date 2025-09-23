package com.test.window.gui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
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
    // Color constants (duplicated from TestRunner_Template for independence)
    private static final String TABLE_BACKGROUND = "#323232"; // Dark gray
    private static final String RUNNING_HIGHLIGHT = "#0788b3"; // Light blue
    private static final String STATUS_PASSED = "#00FF00"; // Green
    private static final String STATUS_FAILED = "#FF6666"; // Rose
    private static final String DEFAULT_FOREGROUND = "#C8C8C8"; // Light gray
    private static final String CHECKBOX_ENABLED_COLOR = "#C8C8C8"; // Light gray
    private static final String DISABLED_COLOR = "#646464";
    private static final String HOVER_COLOR = "#288A48";
    
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

        TableColumn<TestRunner.TestCase, String> idCol = new TableColumn<>("Test Id");
        idCol.setCellValueFactory(cellData -> cellData.getValue().testIdProperty());
        idCol.setMinWidth(80);
        idCol.setMaxWidth(80);
        idCol.setPrefWidth(80);

        TableColumn<TestRunner.TestCase, String> descCol = new TableColumn<>("Test Description");
        descCol.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descCol.setMinWidth(150);
        descCol.setPrefWidth(300);

        TableColumn<TestRunner.TestCase, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setMinWidth(80);
        statusCol.setMaxWidth(80);
        statusCol.setPrefWidth(80);

        // Custom cell factory for status coloring, preserving grid lines and highlighting selected cells
        statusCol.setCellFactory(column -> new TableCell<TestRunner.TestCase, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                // Reset style to ensure grid lines are not overridden
                setStyle("-fx-border-color: #646464; -fx-border-width: 0 1 1 0;");
                if (empty || status == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + (isSelected() ? RUNNING_HIGHLIGHT : TABLE_BACKGROUND) + 
                             "; -fx-text-fill: " + DEFAULT_FOREGROUND + "; -fx-border-color: #646464; -fx-border-width: 0 1 1 0;");
                } else {
                    setText(status);
                    if ("Running".equals(status)) {
                        setStyle("-fx-background-color: " + (isSelected() ? RUNNING_HIGHLIGHT : RUNNING_HIGHLIGHT) + 
                                 "; -fx-text-fill: black; -fx-border-color: #646464; -fx-border-width: 0 1 1 0;");
                    } else if ("Passed".equals(status)) {
                        setStyle("-fx-background-color: " + (isSelected() ? RUNNING_HIGHLIGHT : TABLE_BACKGROUND) + 
                                 "; -fx-text-fill: " + STATUS_PASSED + "; -fx-border-color: #646464; -fx-border-width: 0 1 1 0;");
                    } else if ("Failed".equals(status)) {
                        setStyle("-fx-background-color: " + (isSelected() ? RUNNING_HIGHLIGHT : TABLE_BACKGROUND) + 
                                 "; -fx-text-fill: " + STATUS_FAILED + "; -fx-border-color: #646464; -fx-border-width: 0 1 1 0;");
                    } else {
                        setStyle("-fx-background-color: " + (isSelected() ? RUNNING_HIGHLIGHT : TABLE_BACKGROUND) + 
                                 "; -fx-text-fill: " + DEFAULT_FOREGROUND + "; -fx-border-color: #646464; -fx-border-width: 0 1 1 0;");
                    }
                }
            }
        });

        tableView.getColumns().addAll(runCol, idCol, descCol, statusCol);
        // Enhanced TableView styling to ensure grid lines are visible and row selector color is uniform blue
        tableView.setStyle("-fx-background-color: " + TABLE_BACKGROUND + "; -fx-control-inner-background: " + 
                TABLE_BACKGROUND + "; -fx-table-cell-border-color: " + DISABLED_COLOR + 
                "; -fx-horizontal-grid-lines-visible: true; -fx-vertical-grid-lines-visible: true; " +
                "-fx-selection-bar: " + RUNNING_HIGHLIGHT + "; -fx-selection-bar-non-focused: " + RUNNING_HIGHLIGHT + ";");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Select All checkbox (without event handler, as it's handled in the main class)
        CheckBox selectAllCheckBox = new CheckBox("Select All");
        selectAllCheckBox.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12));
        selectAllCheckBox.setTextFill(javafx.scene.paint.Color.web(CHECKBOX_ENABLED_COLOR));
        selectAllCheckBox.setDisable(true);

        HBox checkBoxBox = new HBox(selectAllCheckBox);
        checkBoxBox.setAlignment(Pos.CENTER_LEFT);
        checkBoxBox.setPadding(new Insets(0, 0, 5, 8));

        // Layout: VBox with checkbox and table
        VBox tableBox = new VBox(checkBoxBox, tableView);
        tableBox.setSpacing(5);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        return tableBox;
    }
}