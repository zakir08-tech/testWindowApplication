package com.test.window.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Manages a customizable TableView for editing API test steps.
 * Handles column configuration, editing behavior, validation, and UI updates.
 * Supports single-row selection and custom cell editing with keyboard navigation.
 */
public class TableManager {

    /**
     * Enum defining the indices of columns in the table.
     * Each entry corresponds to a specific field in an API test step (e.g., TEST_ID at index 0).
     */
    private enum ColumnIndex {
        TEST_ID(0), TEST_DESCRIPTION(1), REQUEST(2), END_POINT(3), HEADER_KEY(4), HEADER_VALUE(5),
        PARAM_KEY(6), PARAM_VALUE(7), PAYLOAD(8), PAYLOAD_TYPE(9),
        RESPONSE_KEY_NAME(10), CAPTURE_VALUE(11), AUTHORIZATION(12),
        AUTH_FIELD1(13), AUTH_FIELD2(14), PROXY(15), SSL_VALIDATION(16), EXPECTED_STATUS(17),
        VERIFY_RESPONSE(18);

        private final int index;
        ColumnIndex(int index) { this.index = index; }
        public int getIndex() { return index; }
    }

    /**
     * Observable list of supported HTTP methods for the request column.
     * Includes an empty option for no method selection.
     */
    private static final ObservableList<String> HTTP_METHODS =
        FXCollections.observableArrayList("", "GET", "POST", "PUT", "PATCH", "DELETE");

    /** The main TableView instance managed by this class. */
    private final TableView<String[]> table;
    /** Label used to display status messages, errors, or suggestions. */
    private final Label statusLabel;
    /** Array of column names for header labels. */
    private final String[] columnNames;
    /** Reference to the parent application for modification tracking and utilities. */
    private final CreateEditAPITest app;

    /**
     * Constructs a TableManager with the given column names, status label, and application reference.
     * Initializes the table with columns, editing policies, and event handlers.
     *
     * @param columnNames Array of strings for column headers.
     * @param statusLabel Label for displaying feedback messages.
     * @param app Reference to the CreateEditAPITest application.
     */
    public TableManager(String[] columnNames, Label statusLabel, CreateEditAPITest app) {
        this.columnNames = columnNames;
        this.statusLabel = statusLabel;
        this.app = app;
        this.table = createTable();
    }

    /**
     * Returns the underlying TableView instance.
     *
     * @return The configured TableView.
     */
    public TableView<String[]> getTable() {
        return table;
    }

    /**
     * Creates and configures a new TableView for API test steps.
     * Sets up columns, editing, selection, styling, and event listeners.
     *
     * @return The fully configured TableView.
     */
    private TableView<String[]> createTable() {
        TableView<String[]> table = new TableView<>();
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        // Apply dark theme styling to the table
        table.setStyle("-fx-background-color: #2E2E2E; -fx-table-cell-border-color: transparent; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-hbar-policy: as-needed; -fx-scrollbar-color: #4A90E2; -fx-scrollbar-background: #3C3F41;");
        table.setPrefWidth(480);

        // Custom placeholder for empty table
        Label placeholderLabel = new Label("No steps defined");
        placeholderLabel.setStyle("-fx-text-fill: white;");
        table.setPlaceholder(placeholderLabel);

        // Configure each column
        for (int i = 0; i < columnNames.length; i++) {
            final int index = i;
            TableColumn<String[], String> column = new TableColumn<>();
            column.setText(""); // Prevent default text to avoid duplication
            // Bind cell value to the corresponding array element
            column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[index]));
            
            // Disable editing for data columns (e.g., keys/values that should not be directly edited in place)
            column.setEditable(!(index == ColumnIndex.REQUEST.getIndex() ||
                                 index == ColumnIndex.END_POINT.getIndex() ||
                                 index == ColumnIndex.HEADER_KEY.getIndex() ||
                                 index == ColumnIndex.HEADER_VALUE.getIndex() ||
                                 index == ColumnIndex.PARAM_KEY.getIndex() ||
                                 index == ColumnIndex.PARAM_VALUE.getIndex() ||
                                 index == ColumnIndex.PAYLOAD.getIndex() ||
                                 index == ColumnIndex.RESPONSE_KEY_NAME.getIndex() ||
                                 index == ColumnIndex.CAPTURE_VALUE.getIndex() ||
                                 index == ColumnIndex.AUTHORIZATION.getIndex() ||
                                 index == ColumnIndex.PROXY.getIndex()||
                                 index == ColumnIndex.SSL_VALIDATION.getIndex()||
                                 index == ColumnIndex.VERIFY_RESPONSE.getIndex()));
            
            // Use custom cell factory for enhanced editing
            column.setCellFactory(col -> new CustomTextFieldTableCell(table, index, statusLabel, app));
            // Handle edit commit: validate and update data
            column.setOnEditCommit(event -> {
                String newValue = event.getNewValue() != null ? event.getNewValue() : "";
                int colIndex = event.getTablePosition().getColumn();
                int rowIndex = event.getTablePosition().getRow();
                // Validate expected status code
                if (colIndex == ColumnIndex.EXPECTED_STATUS.getIndex() && !newValue.matches("\\d+|^$")) {
                    statusLabel.setText("Status must be a number");
                    return;
                }
                statusLabel.setText("");
                event.getTableView().getItems().get(rowIndex)[colIndex] = newValue;
                app.setModified(true); // Set modified on edit commit
                table.refresh();
                updateAuthFieldHeaders(rowIndex);
            });
            
            // Set header label with tooltip
            column.setGraphic(new Label(columnNames[i]) {{
                setTooltip(new Tooltip("Click to edit"));
            }});
            // Calculate column width based on header text length
            double charWidth = 7.0;
            double headerBasedWidth = columnNames[i].length() * charWidth + 20;
            column.setMinWidth(headerBasedWidth);
            column.setPrefWidth(headerBasedWidth);
            column.setStyle("-fx-text-fill: white;"); // Style header text
            table.getColumns().add(column);
        }

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Custom row factory for selection-based styling
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

        // Listen for selection changes to update auth fields
        table.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            table.refresh();
            if (newVal != null && newVal.intValue() >= 0) {
                updateAuthFieldHeaders(newVal.intValue());
            }
        });

        // Handle single-click to start editing on editable cells
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

    /**
     * Updates the header labels for authorization fields based on the selected row's auth type.
     * Dynamically changes labels for Basic Auth or Bearer Token.
     *
     * @param rowIndex The index of the row to update headers for.
     */
    public void updateAuthFieldHeaders(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= table.getItems().size()) {
            return;
        }
        String[] row = table.getItems().get(rowIndex);
        String authValue = row[ColumnIndex.AUTHORIZATION.getIndex()];
        TableColumn<String[], String> authField1Col = (TableColumn<String[], String>) table.getColumns().get(ColumnIndex.AUTH_FIELD1.getIndex());
        TableColumn<String[], String> authField2Col = (TableColumn<String[], String>) table.getColumns().get(ColumnIndex.AUTH_FIELD2.getIndex());
        Label authField1Label = (Label) authField1Col.getGraphic();
        Label authField2Label = (Label) authField2Col.getGraphic();

        if ("Basic Auth".equals(authValue)) {
            authField1Label.setText("Username");
            authField2Label.setText("Password");
        } else if ("Bearer Token".equals(authValue)) {
            authField1Label.setText("Token");
            authField2Label.setText("");
        } else {
            authField1Label.setText("");
            authField2Label.setText("");
        }
        table.refresh();
    }

    /**
     * Custom table cell extending TextFieldTableCell for advanced editing features.
     * Handles validation, keyboard navigation (Tab/Enter/Esc), duplicate detection for Test IDs,
     * and JSON formatting for payload fields.
     */
    private class CustomTextFieldTableCell extends TextFieldTableCell<String[], String> {
        /** Reference to the parent table. */
        private final TableView<String[]> table;
        /** Index of the column this cell belongs to. */
        private final int columnIndex;
        /** Status label for feedback. */
        private final Label statusLabel;
        /** Set of existing Test IDs to check for duplicates. */
        private final Set<String> testIds = new HashSet<>();
        /** Original value before editing starts. */
        private String originalValue;
        /** Reference to the parent application. */
        private final CreateEditAPITest app;

        /**
         * Constructs a custom cell with table, column index, status label, and app reference.
         * Sets up item listener to track Test IDs.
         *
         * @param table The parent TableView.
         * @param columnIndex The column index.
         * @param statusLabel Status feedback label.
         * @param app Application reference.
         */
        public CustomTextFieldTableCell(TableView<String[]> table, int columnIndex, Label statusLabel, CreateEditAPITest app) {
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
            this.app = app;
            // Listen for table item changes to update Test ID set
            table.getItems().addListener((javafx.collections.ListChangeListener<String[]>) c -> {
                updateTestIds();
            });
            updateTestIds();
        }

        /** Updates the set of existing Test IDs from table rows. */
        private void updateTestIds() {
            testIds.clear();
            for (String[] row : table.getItems()) {
                String testId = row[0];
                if (testId != null && !testId.isEmpty()) {
                    testIds.add(testId);
                }
            }
        }

        /**
         * Suggests a unique Test ID based on user input.
         * Ensures it's numeric and not already in use.
         *
         * @param input The user's input string.
         * @return A suggested unique numeric Test ID.
         */
        private String suggestUniqueTestId(String input) {
            if (input == null || input.isEmpty() || !input.matches("[0-9]+")) {
                return generateUniqueId("0");
            }
            String base = input.replaceAll("[^0-9]", "");
            if (base.isEmpty()) {
                base = "0";
            }
            return generateUniqueId(base);
        }

        /**
         * Generates a unique numeric ID starting from a base value.
         *
         * @param base The base string to derive from.
         * @return A unique numeric ID (up to 5 digits).
         */
        private String generateUniqueId(String base) {
            String candidate = base;
            int suffix = 0;
            while (testIds.contains(candidate) || !candidate.matches("[0-9]+")) {
                suffix++;
                candidate = String.valueOf(suffix);
                if (candidate.length() > 5) {
                    candidate = candidate.substring(0, 5);
                }
            }
            return candidate;
        }

        /**
         * Temporarily highlights rows with duplicate Test IDs.
         *
         * @param testId The Test ID to check for duplicates.
         */
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

        /** Clears all duplicate highlights and restores normal styling. */
        private void clearDuplicateHighlights() {
            table.getItems().forEach((row) -> {
                int index = table.getItems().indexOf(row);
                TableRow<String[]> tableRow = (TableRow<String[]>) table.lookup(".table-row-cell[index=" + index + "]");
                if (tableRow != null && tableRow.getStyle().contains("4A90E2")) {
                    if (index == table.getSelectionModel().getSelectedIndex()) {
                        tableRow.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px;");
                    } else {
                        tableRow.setStyle("-fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px;");
                    }
                }
            });
        }

        /**
         * Clears all data in a row except the Test ID.
         *
         * @param rowIndex The row index to clear.
         */
        private void clearRowData(int rowIndex) {
            String[] row = table.getItems().get(rowIndex);
            for (int i = 1; i < row.length; i++) {
                row[i] = "";
            }
            app.setModified(true); // Set modified on clear
        }

        @Override
        public void startEdit() {
            super.startEdit();
            if (isEditing() && getGraphic() instanceof TextField) {
                TextField textField = (TextField) getGraphic();
                originalValue = getItem() != null ? getItem() : "";
                if (columnIndex == ColumnIndex.TEST_ID.getIndex()) {
                    // Filter for numeric Test ID input with duplicate check
                    UnaryOperator<TextFormatter.Change> filter = change -> {
                        String newText = change.getControlNewText();
                        if (newText.length() > 5) {
                            return null;
                        }
                        if (!newText.matches("[0-9]*")) {
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
                } else if (columnIndex == ColumnIndex.EXPECTED_STATUS.getIndex()) {
                    // Filter for numeric status code
                    UnaryOperator<TextFormatter.Change> filter = change -> {
                        String newText = change.getControlNewText();
                        if (newText.matches("\\d*")) {
                            return change;
                        }
                        return null;
                    };
                    textField.setTextFormatter(new TextFormatter<>(filter));
                }
                // Handle key presses for navigation and commit
                textField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.TAB) {
                        String text = textField.getText() != null ? textField.getText() : "";
                        int rowIndex = getTableRow().getIndex();
                        int newColumn = (columnIndex + 1) % table.getColumns().size();
                        // Validate Test ID before committing on Tab
                        if (columnIndex == ColumnIndex.TEST_ID.getIndex() && !text.isEmpty() && !CreateEditAPITest.isValidTestId(text, testIds, originalValue)) {
                            CreateEditAPITest.showError("Cannot commit invalid Test ID: " + text + ". Use only 0-9.");
                            cancelEdit();
                            setText(getItem() != null ? getItem() : "");
                            setGraphic(null);
                            table.refresh();
                        } else if (columnIndex == ColumnIndex.TEST_ID.getIndex() && text.isEmpty() && originalValue != null && !originalValue.isEmpty()) {
                            // Confirm deletion of row data on empty Test ID
                            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                            confirmation.setTitle("Confirm Delete Test ID");
                            confirmation.setHeaderText("Delete Test Step Data");
                            confirmation.setContentText("Removing the Test ID will delete all data in this test step. Are you sure?");
                            Optional<ButtonType> result = confirmation.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK) {
                                clearRowData(rowIndex);
                                commitEdit("");
                            } else {
                                cancelEdit();
                                setText(originalValue != null ? originalValue : "");
                                setGraphic(null);
                                table.refresh();
                            }
                        } else {
                            commitEdit(CreateEditAPITest.formatJson(text, statusLabel));
                        }
                        // Move focus to next cell and start editing
                        table.getFocusModel().focus(rowIndex, table.getColumns().get(newColumn));
                        table.getSelectionModel().select(rowIndex);
                        table.edit(rowIndex, table.getColumns().get(newColumn));
                        Platform.runLater(() -> {
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
                        // Commit on Enter with validation
                        String text = textField.getText() != null ? textField.getText() : "";
                        if (columnIndex == ColumnIndex.TEST_ID.getIndex()) {
                            if (!text.isEmpty() && !CreateEditAPITest.isValidTestId(text, testIds, originalValue)) {
                                CreateEditAPITest.showError("Cannot commit invalid Test ID: " + text + ". Use only 0-9.");
                                return;
                            } else if (text.isEmpty() && originalValue != null && !originalValue.isEmpty()) {
                                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                                confirmation.setTitle("Confirm Delete Test ID");
                                confirmation.setHeaderText("Delete Test Step Data");
                                confirmation.setContentText("Removing the Test ID will delete all data in this test step. Are you sure?");
                                Optional<ButtonType> result = confirmation.showAndWait();
                                if (result.isPresent() && result.get() == ButtonType.OK) {
                                    clearRowData(getTableRow().getIndex());
                                    commitEdit("");
                                } else {
                                    cancelEdit();
                                    setText(originalValue != null ? originalValue : "");
                                    setGraphic(null);
                                    table.refresh();
                                    return;
                                }
                            }
                        }
                        commitEdit(CreateEditAPITest.formatJson(text, statusLabel));
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
                // Apply cell styling based on selection
                if (getTableRow() != null && getTableRow().isSelected()) {
                    setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px; -fx-padding: 2px; -fx-alignment: center-left;");
                } else {
                    setStyle("-fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px; -fx-padding: 2px; -fx-alignment: center-left;");
                }
            }
        }

        @Override
        public void commitEdit(String newValue) {
            // Final validation for Test ID
            if (columnIndex == ColumnIndex.TEST_ID.getIndex() && !newValue.isEmpty() && !CreateEditAPITest.isValidTestId(newValue, testIds, originalValue)) {
                return;
            }
            super.commitEdit(newValue);
            app.setModified(true); // Set modified on commit
            if (columnIndex == ColumnIndex.TEST_ID.getIndex() && !newValue.isEmpty()) {
                clearDuplicateHighlights();
                updateTestIds();
                int rowIndex = getTableRow().getIndex();
                String[] row = table.getItems().get(rowIndex);
                // Set default headers if not present
                if (row[ColumnIndex.HEADER_KEY.getIndex()] == null || row[ColumnIndex.HEADER_KEY.getIndex()].isEmpty()) {
                    row[ColumnIndex.HEADER_KEY.getIndex()] = "Content-Type";
                }
                if (row[ColumnIndex.HEADER_VALUE.getIndex()] == null || row[ColumnIndex.HEADER_VALUE.getIndex()].isEmpty()) {
                    row[ColumnIndex.HEADER_VALUE.getIndex()] = "application/json";
                }
                table.refresh();
                int currentIndex = getTableRow().getIndex();
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().select(currentIndex);
            } else if (columnIndex == ColumnIndex.AUTHORIZATION.getIndex()) {
                // Update auth headers on change
                int rowIndex = getTableRow().getIndex();
                updateAuthFieldHeaders(rowIndex);
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() != null ? getItem() : "");
            setGraphic(null);
            if (columnIndex == ColumnIndex.TEST_ID.getIndex()) {
                clearDuplicateHighlights();
                if (getTableRow() != null && getTableRow().getItem() != null && originalValue != null) {
                    getTableRow().getItem()[ColumnIndex.TEST_ID.getIndex()] = originalValue;
                    table.refresh();
                }
            }
        }
    }
}