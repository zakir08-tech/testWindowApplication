package com.test.window.gui.com;

import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;

public class CustomTableCells {
    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    public static class CustomComboBoxTableCell extends ComboBoxTableCell<String[], String> {
        public CustomComboBoxTableCell(TableView<String[]> table, ObservableList<String> items) {
            super(new StringConverter<String>() {
                @Override
                public String toString(String object) {
                    return object == null ? "" : object;
                }
                @Override
                public String fromString(String string) {
                    return string;
                }
            }, items);

            setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && !isEditing() && !isEmpty()) {
                    startEdit();
                    if (getGraphic() instanceof ComboBox) {
                        @SuppressWarnings("unchecked")
                        ComboBox<String> comboBox = (ComboBox<String>) getGraphic();
                        Platform.runLater(() -> {
                            comboBox.show();
                            comboBox.requestFocus();
                        });
                    }
                }
            });

            setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && !isEditing() && !isEmpty()) {
                    startEdit();
                    if (getGraphic() instanceof ComboBox) {
                        @SuppressWarnings("unchecked")
                        ComboBox<String> comboBox = (ComboBox<String>) getGraphic();
                        Platform.runLater(() -> {
                            comboBox.show();
                            comboBox.requestFocus();
                        });
                    }
                }
            });
        }

        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                if (getGraphic() instanceof ComboBox) {
                    @SuppressWarnings("unchecked")
                    ComboBox<String> comboBox = (ComboBox<String>) getGraphic();
                    Platform.runLater(() -> {
                        comboBox.show();
                        comboBox.requestFocus();
                    });
                }
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() != null ? getItem() : "");
            setGraphic(null);
        }
    }

    public static class CustomTextFieldTableCell extends TextFieldTableCell<String[], String> {
        private final TableView<String[]> table;
        private final int columnIndex;
        private final Label statusLabel;
        private final TableConfig tableConfig;
        private final Set<String> testIds = new HashSet<>();
        private String originalValue;

        public CustomTextFieldTableCell(TableView<String[]> table, int columnIndex, Label statusLabel, TableConfig tableConfig) {
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
            this.tableConfig = tableConfig;

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
                }
                textField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.TAB) {
                        String text = textField.getText() != null ? textField.getText() : "";
                        if (columnIndex == 0) {
                            int rowIndex = getTableRow().getIndex();
                            if (!tableConfig.isValidTestId(text, testIds, originalValue, rowIndex, table)) {
                                @SuppressWarnings("unchecked")
                                TableColumn<String[], String> column = (TableColumn<String[], String>) table.getColumns().get(columnIndex);
                                showError("Cannot commit duplicate/invalid Test ID: " + text);
                                cancelEdit();
                                setText(getItem() != null ? getItem() : "");
                                setGraphic(null);
                                table.refresh();
                                Platform.runLater(() -> {
                                    int newColumn = (columnIndex + 1) % table.getColumns().size();
                                    table.getFocusModel().focus(rowIndex, table.getColumns().get(newColumn));
                                    table.getSelectionModel().select(rowIndex);
                                    table.edit(rowIndex, table.getColumns().get(newColumn));
                                    if (newColumn == 1) {
                                        TableCell<String[], String> nextCell = (TableCell<String[], String>) table.lookup(".table-cell[column=" + newColumn + "][index=" + rowIndex + "]");
                                        if (nextCell instanceof CustomComboBoxTableCell && nextCell.getGraphic() instanceof ComboBox) {
                                            @SuppressWarnings("unchecked")
                                            ComboBox<String> comboBox = (ComboBox<String>) nextCell.getGraphic();
                                            comboBox.show();
                                            comboBox.requestFocus();
                                        }
                                    }
                                    table.scrollTo(rowIndex);
                                    table.scrollToColumn(table.getColumns().get(newColumn));
                                });
                                e.consume();
                                return;
                            }
                        }
                        commitEdit(getConverter().fromString(text));
                        TablePosition<String[], ?> pos = table.getFocusModel().getFocusedCell();
                        int newColumn = (pos.getColumn() + 1) % table.getColumns().size();
                        int newRow = pos.getRow();
                        table.getFocusModel().focus(newRow, table.getColumns().get(newColumn));
                        table.edit(newRow, table.getColumns().get(newColumn));
                        if (newColumn == 1) {
                            Platform.runLater(() -> {
                                TableCell<String[], String> nextCell = (TableCell<String[], String>) table.lookup(".table-cell[column=" + newColumn + "][index=" + newRow + "]");
                                if (nextCell instanceof CustomComboBoxTableCell && nextCell.getGraphic() instanceof ComboBox) {
                                    @SuppressWarnings("unchecked")
                                    ComboBox<String> comboBox = (ComboBox<String>) nextCell.getGraphic();
                                    comboBox.show();
                                    comboBox.requestFocus();
                                }
                            });
                        }
                        table.scrollTo(newRow);
                        table.scrollToColumn(table.getColumns().get(newColumn));
                        e.consume();
                    } else if (e.getCode() == KeyCode.ENTER) {
                        String text = textField.getText() != null ? textField.getText() : "";
                        if (columnIndex == 0) {
                            int rowIndex = getTableRow().getIndex();
                            if (!tableConfig.isValidTestId(text, testIds, originalValue, rowIndex, table)) {
                                showError("Cannot commit duplicate/invalid Test ID: " + text);
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
        public void commitEdit(String newValue) {
            if (columnIndex == 0) {
                int rowIndex = getTableRow().getIndex();
                if (!tableConfig.isValidTestId(newValue, testIds, originalValue, rowIndex, table)) {
                    return;
                }
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
    }
}