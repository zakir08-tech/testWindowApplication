package com.test.window.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.util.StringConverter;
import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

public class TableManager {

    private enum ColumnIndex {
        TEST_ID(0), REQUEST(1), END_POINT(2), HEADER_KEY(3), HEADER_VALUE(4),
        PARAM_KEY(5), PARAM_VALUE(6), PAYLOAD(7), PAYLOAD_TYPE(8),
        MODIFY_PAYLOAD_KEY(9), MODIFY_PAYLOAD_VALUE(10), RESPONSE_KEY_NAME(11),
        CAPTURE_VALUE(12), AUTHORIZATION(13), SSL_VALIDATION(16), EXPECTED_STATUS(17),
        VERIFY_RESPONSE(18), TEST_DESCRIPTION(19);

        private final int index;
        ColumnIndex(int index) { this.index = index; }
        public int getIndex() { return index; }
    }

    private static final ObservableList<String> HTTP_METHODS = 
        FXCollections.observableArrayList("", "GET", "POST", "PUT", "PATCH", "DELETE");

    private final TableView<String[]> table;
    private final Label statusLabel;
    private final String[] columnNames;

    public TableManager(String[] columnNames, Label statusLabel) {
        this.columnNames = columnNames;
        this.statusLabel = statusLabel;
        this.table = createTable();
    }

    public TableView<String[]> getTable() {
        return table;
    }

    private TableView<String[]> createTable() {
        TableView<String[]> table = new TableView<>();
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setStyle("-fx-background-color: #2E2E2E; -fx-table-cell-border-color: transparent; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-hbar-policy: as-needed; -fx-scrollbar-color: #4A90E2; -fx-scrollbar-background: #3C3F41;");
        table.setPrefWidth(480);

        Label placeholderLabel = new Label("No steps defined");
        placeholderLabel.setStyle("-fx-text-fill: white;");
        table.setPlaceholder(placeholderLabel);

        for (int i = 0; i < columnNames.length; i++) {
            final int index = i;
            TableColumn<String[], String> column = new TableColumn<>();
            column.setText(""); // Prevent default text to avoid duplication
            column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[index]));
            
            // Disable editing for specified columns
            column.setEditable(!(index == ColumnIndex.REQUEST.getIndex() ||
            					 index == ColumnIndex.END_POINT.getIndex() ||
                                 index == ColumnIndex.HEADER_KEY.getIndex() ||
                                 index == ColumnIndex.HEADER_VALUE.getIndex() ||
                                 index == ColumnIndex.PARAM_KEY.getIndex() ||
                                 index == ColumnIndex.PARAM_VALUE.getIndex() ||
                                 index == ColumnIndex.PAYLOAD.getIndex() ||
                                 index == ColumnIndex.MODIFY_PAYLOAD_KEY.getIndex() ||
                                 index == ColumnIndex.MODIFY_PAYLOAD_VALUE.getIndex() ||
                                 index == ColumnIndex.RESPONSE_KEY_NAME.getIndex() ||
                                 index == ColumnIndex.CAPTURE_VALUE.getIndex() ||
                                 index == ColumnIndex.AUTHORIZATION.getIndex() ||
                                 index == ColumnIndex.VERIFY_RESPONSE.getIndex()));
            
            column.setCellFactory(col -> new CustomTextFieldTableCell(table, index, statusLabel));
            column.setOnEditCommit(event -> {
                String newValue = event.getNewValue() != null ? event.getNewValue() : "";
                int colIndex = event.getTablePosition().getColumn();
                int rowIndex = event.getTablePosition().getRow();
                if (colIndex == ColumnIndex.EXPECTED_STATUS.getIndex() && !newValue.matches("\\d+|^$")) {
                    statusLabel.setText("Status must be a number");
                    return;
                } else if (colIndex == ColumnIndex.REQUEST.getIndex()) {
                    if (!newValue.isEmpty() && !HTTP_METHODS.contains(newValue)) {
                        statusLabel.setText("Invalid HTTP method");
                        return; // Prevent committing invalid HTTP method
                    } else {
                        statusLabel.setText("");
                    }
                }
                statusLabel.setText("");
                event.getTableView().getItems().get(rowIndex)[colIndex] = newValue;
                table.refresh();
            });
            
            column.setGraphic(new Label(columnNames[i]) {{
                setTooltip(new Tooltip("Click to edit"));
            }});
            double charWidth = 7.0;
            double minWidth = columnNames[i].length() * charWidth + 20;
            column.setMinWidth(minWidth);
            column.setPrefWidth(index == ColumnIndex.TEST_ID.getIndex() ? 80 : 150);
            column.setStyle("-fx-text-fill: white;");
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

        table.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            table.refresh();
        });

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1 && event.getButton() == MouseButton.PRIMARY && !table.getSelectionModel().isEmpty()) {
                TablePosition<String[], ?> pos = table.getFocusModel().getFocusedCell();
                if (pos != null && pos.getColumn() >= 0 && pos.getRow() >= 0 && table.getColumns().get(pos.getColumn()).isEditable()) {
                    table.edit(pos.getRow(), table.getColumns().get(pos.getColumn()));
                    event.consume();
                }
            }
        });

        return table;
    }

    private class CustomTextFieldTableCell extends TextFieldTableCell<String[], String> {
        private final TableView<String[]> table;
        private final int columnIndex;
        private final Label statusLabel;
        private final Set<String> testIds = new HashSet<>();
        private String originalValue;

        public CustomTextFieldTableCell(TableView<String[]> table, int columnIndex, Label statusLabel) {
            super(new StringConverter<String>() {
                @Override
                public String toString(String object) {
                    return object == null ? "" : object;
                }
                @Override
                public String fromString(String string) {
                    return string;
                }
            });
            this.table = table;
            this.columnIndex = columnIndex;
            this.statusLabel = statusLabel;

            table.getItems().addListener((javafx.collections.ListChangeListener<String[]>) c -> {
                updateTestIds();
            });
            updateTestIds();
        }

        private void updateTestIds() {
            testIds.clear();
            for (String[] row : table.getItems()) {
                String testId = row[0];
                if (testId != null && !testId.isEmpty()) {
                    testIds.add(testId);
                }
            }
        }

        private String suggestUniqueTestId(String input) {
            if (input == null || input.isEmpty()) {
                return generateUniqueId("1");
            }
            String base = input.replaceAll("[^0-9#]", "");
            if (base.isEmpty()) {
                base = "1";
            }
            return generateUniqueId(base);
        }

        private String generateUniqueId(String base) {
            String candidate = base;
            int suffix = 1;
            while (testIds.contains(candidate)) {
                candidate = base + suffix;
                suffix++;
                if (candidate.length() > 5) {
                    candidate = candidate.substring(0, 5);
                    if (testIds.contains(candidate)) {
                        candidate = String.valueOf(suffix);
                    }
                }
            }
            return candidate;
        }

        private void highlightDuplicateRow(String testId) {
            for (int index = 0; index < table.getItems().size(); index++) {
                String[] row = table.getItems().get(index);
                if (row[0] != null && row[0].equals(testId) && index != getTableRow().getIndex()) {
                    TableRow<String[]> tableRow = (TableRow<String[]>) table.lookup(".table-row-cell[index=" + index + "]");
                    if (tableRow != null) {
                        tableRow.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px;");
                    }
                }
            }
        }

        private void clearDuplicateHighlights() {
            table.getItems().forEach((row) -> {
                int index = table.getItems().indexOf(row);
                TableRow<String[]> tableRow = (TableRow<String[]>) table.lookup(".table-row-cell[index=" + index + "]");
                if (tableRow != null && index != table.getSelectionModel().getSelectedIndex()) {
                    tableRow.setStyle("-fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px;");
                }
            });
            statusLabel.setText("");
        }

        @Override
        public void startEdit() {
            if (table.getItems().isEmpty() || isEditing()) {
                return;
            }
            super.startEdit();
            originalValue = getItem();
            if (getGraphic() instanceof TextField) {
                TextField textField = (TextField) getGraphic();
                textField.setPrefHeight(26);
                textField.setMaxHeight(26);
                textField.setMinHeight(26);
                textField.setStyle("-fx-alignment: center-left;"); // Left-align TextField content
                if (columnIndex == 0) {
                    UnaryOperator<TextFormatter.Change> filter = change -> {
                        String newText = change.getControlNewText();
                        if (newText.length() > 5) {
                            return null;
                        }
                        if (!newText.matches("^[0-9#]*$")) {
                            return null;
                        }
                        if (newText.startsWith("#") && newText.matches(".*[0-9].*")) {
                            return null;
                        }
                        if (newText.matches("^[0-9].*") && newText.contains("#")) {
                            return null;
                        }
                        if (!newText.isEmpty() && !newText.equals(originalValue) && testIds.contains(newText)) {
                            String suggestedId = suggestUniqueTestId(newText);
                            statusLabel.setText("Test ID '" + newText + "' already exists. Try: " + suggestedId);
                            highlightDuplicateRow(newText);
                        } else {
                            statusLabel.setText("");
                            clearDuplicateHighlights();
                        }
                        return change;
                    };
                    textField.setTextFormatter(new TextFormatter<>(filter));
                } else if (columnIndex == 17) {
                    UnaryOperator<TextFormatter.Change> filter = change -> {
                        String newText = change.getControlNewText();
                        if (newText.matches("\\d*")) {
                            return change;
                        }
                        return null;
                    };
                    textField.setTextFormatter(new TextFormatter<>(filter));
                } else if (columnIndex == 1) { // Request column
                    UnaryOperator<TextFormatter.Change> filter = change -> {
                        String newText = change.getControlNewText();
                        if (newText.isEmpty() || HTTP_METHODS.contains(newText)) {
                            statusLabel.setText("");
                            return change;
                        }
                        statusLabel.setText("Invalid HTTP method");
                        return null;
                    };
                    textField.setTextFormatter(new TextFormatter<>(filter));
                }
                textField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.TAB) {
                        String text = textField.getText() != null ? textField.getText() : "";
                        int rowIndex = getTableRow().getIndex();
                        int newColumn = (columnIndex + 1) % table.getColumns().size();
                        if (columnIndex == 0 && !text.isEmpty() && !isValidTestId(text)) {
                            showError("Cannot commit duplicate/invalid Test ID: " + text);
                            cancelEdit();
                            setText(getItem() != null ? getItem() : "");
                            setGraphic(null);
                            table.refresh();
                        } else if (columnIndex == 1 && !text.isEmpty() && !HTTP_METHODS.contains(text)) {
                            showError("Invalid HTTP method: " + text);
                            cancelEdit();
                            setText(getItem() != null ? getItem() : "");
                            setGraphic(null);
                            table.refresh();
                        } else {
                            commitEdit(getConverter().fromString(text));
                        }
                        table.getFocusModel().focus(rowIndex, table.getColumns().get(newColumn));
                        table.getSelectionModel().select(rowIndex);
                        table.edit(rowIndex, table.getColumns().get(newColumn));
                        Platform.runLater(() -> {
                            // Find the TableCell for the new row and column
                            TableCell<?, ?> nextCell = null;
                            for (Node node : table.lookupAll(".table-cell")) {
                                if (node instanceof TableCell) {
                                    TableCell<?, ?> cell = (TableCell<?, ?>) node;
                                    if (cell.getTableRow() != null && cell.getTableRow().getIndex() == rowIndex && cell.getTableColumn() == table.getColumns().get(newColumn)) {
                                        nextCell = cell;
                                        break;
                                    }
                                }
                            }
                            if (nextCell instanceof CustomTextFieldTableCell && nextCell.getGraphic() instanceof TextField) {
                                TextField nextTextField = (TextField) nextCell.getGraphic();
                                nextTextField.requestFocus();
                                nextTextField.selectAll();
                            }
                        });
                        table.scrollTo(rowIndex);
                        table.scrollToColumn(table.getColumns().get(newColumn));
                        e.consume();
                    } else if (e.getCode() == KeyCode.ENTER) {
                        String text = textField.getText() != null ? textField.getText() : "";
                        if (columnIndex == 0) {
                            if (!text.isEmpty() && !isValidTestId(text)) {
                                showError("Cannot commit duplicate/invalid Test ID: " + text);
                                return;
                            }
                        } else if (columnIndex == 1) {
                            if (!text.isEmpty() && !HTTP_METHODS.contains(text)) {
                                showError("Invalid HTTP method: " + text);
                                return;
                            }
                        }
                        commitEdit(getConverter().fromString(text));
                        e.consume();
                    } else if (e.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                        e.consume();
                    }
                });
            }
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getIndex() < 0 || getIndex() >= table.getItems().size()) {
                setText(null);
                setGraphic(null);
                setStyle(""); // Let row factory handle empty cells
            } else {
                // Apply uniform cell style with left alignment for view mode
                if (getTableRow() != null && getTableRow().isSelected()) {
                    setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px; -fx-padding: 2px; -fx-alignment: center-left;");
                } else {
                    setStyle("-fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px; -fx-padding: 2px; -fx-alignment: center-left;");
                }
            }
        }

        @Override
        public void commitEdit(String newValue) {
            if (columnIndex == 0 && !newValue.isEmpty() && !isValidTestId(newValue)) {
                return;
            }
            super.commitEdit(newValue);
            if (columnIndex == 0) {
                clearDuplicateHighlights();
                updateTestIds();
                int rowIndex = getTableRow().getIndex();
                String[] row = table.getItems().get(rowIndex);
                if (row[3] == null || row[3].isEmpty()) {
                    row[3] = "Content-Type";
                }
                if (row[4] == null || row[4].isEmpty()) {
                    row[4] = "application/json";
                }
                table.refresh();
                int currentIndex = getTableRow().getIndex();
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().select(currentIndex);
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() != null ? getItem() : "");
            setGraphic(null);
            if (columnIndex == 0) {
                clearDuplicateHighlights();
                if (getTableRow() != null && getTableRow().getItem() != null && originalValue != null) {
                    getTableRow().getItem()[0] = originalValue;
                    table.refresh();
                }
            }
        }

        private boolean isValidTestId(String testId) {
            if (testId == null || testId.isEmpty()) {
                return false;
            }
            if (!testId.equals(originalValue) && testIds.contains(testId)) {
                return false;
            }
            if (testId.length() > 5) {
                return false;
            }
            if (!testId.matches("^[0-9#]*$")) {
                return false;
            }
            if (testId.startsWith("#") && testId.matches(".*[0-9].*")) {
                return false;
            }
            if (testId.matches("^[0-9].*") && testId.contains("#")) {
                return false;
            }
            return true;
        }

        private void showError(String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.showAndWait();
        }
    }
}