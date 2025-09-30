package com.test.window.gui.com;

import com.test.window.gui.com.CustomTableCells.CustomComboBoxTableCell;
import com.test.window.gui.com.CustomTableCells.CustomTextFieldTableCell;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

public class TableViewManager {
    private final TableView<String[]> table;
    private final TableConfig tableConfig;
    private final Label statusLabel;

    public TableViewManager(TableConfig tableConfig) {
        this.tableConfig = tableConfig;
        this.table = createTable();
        this.statusLabel = new Label();
        this.statusLabel.setStyle("-fx-text-fill: #FF5555;");
        this.statusLabel.setWrapText(true);
    }

    public TableView<String[]> getTable() {
        return table;
    }

    public Label getStatusLabel() {
        return statusLabel;
    }

    // Added getter for tableConfig
    public TableConfig getTableConfig() {
        return tableConfig;
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

        for (int i = 0; i < Constants.COLUMN_NAMES.length; i++) {
            final int index = i;
            TableColumn<String[], String> column = new TableColumn<>(Constants.COLUMN_NAMES[i]);
            column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[index]));
            if (index == 1) {
                column.setCellFactory(col -> new CustomComboBoxTableCell(table, Constants.REQUEST_OPTIONS));
            } else if (index == 8) {
                column.setCellFactory(col -> new CustomComboBoxTableCell(table, Constants.PAYLOAD_TYPE_OPTIONS));
            } else if (index == 13) {
                column.setCellFactory(col -> new CustomComboBoxTableCell(table, Constants.AUTH_OPTIONS));
            } else {
                column.setCellFactory(col -> new CustomTextFieldTableCell(table, index, statusLabel, tableConfig));
            }
            double charWidth = 7.0;
            double minWidth = Constants.COLUMN_NAMES[i].length() * charWidth + 20;
            column.setMinWidth(minWidth);
            column.setPrefWidth(index == 0 ? 24 : 90);
            column.setStyle("-fx-text-fill: white;");
            column.setOnEditCommit(event -> {
                String newValue = event.getNewValue();
                int colIndex = event.getTablePosition().getColumn();
                int rowIndex = event.getTablePosition().getRow();
                if (colIndex == 17 && !newValue.matches("\\d+|^$")) {
                    statusLabel.setText("Status must be a number");
                    return;
                }
                statusLabel.setText("");
                event.getTableView().getItems().get(rowIndex)[colIndex] = newValue;
                table.refresh();
            });
            table.getColumns().add(column);
        }

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

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

        table.getItems().addListener((javafx.collections.ListChangeListener<String[]>) c -> {
            if (!table.getItems().isEmpty() && table.getSelectionModel().getSelectedIndex() < 0) {
                table.getSelectionModel().select(0);
            }
        });

        return table;
    }
}