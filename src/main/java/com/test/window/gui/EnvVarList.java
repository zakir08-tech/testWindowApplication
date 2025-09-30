package com.test.window.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.scene.control.TextFormatter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.UnaryOperator;

public class EnvVarList extends Application {

    private static final String[] COLUMN_NAMES = {
        "Variable Name", "Value"
    };

    private static final String FIELD_STYLE_UNFOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #888888; -fx-border-radius: 5px;";

    private static final String FIELD_STYLE_FOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #4A90E2; -fx-border-width: 2px; -fx-prompt-text-fill: #888888; -fx-border-radius: 5px;";

    private static final String BUTTON_STYLE = 
        "-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px; -fx-pref-height: 30px;";

    private static final String BUTTON_HOVER_STYLE = 
        "-fx-background-color: #6AB0FF; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px; -fx-pref-height: 30px;";

    private static final String CSS = """
        .table-view .scroll-bar:vertical,
        .table-view .scroll-bar:horizontal {
            -fx-background-color: #252525;
        }
        .table-view .scroll-bar:vertical .track,
        .table-view .scroll-bar:horizontal .track {
            -fx-background-color: #252525;
            -fx-border-color: transparent;
            -fx-background-radius: 0px;
        }
        .table-view .scroll-bar:vertical .thumb,
        .table-view .scroll-bar:horizontal .thumb {
            -fx-background-color: #3C3F41;
            -fx-background-radius: 5px;
        }
        .table-view .scroll-bar:vertical .thumb:hover,
        .table-view .scroll-bar:horizontal .thumb:hover {
            -fx-background-color: #4A90E2;
        }
        .table-view .scroll-bar:vertical .thumb:pressed,
        .table-view .scroll-bar:horizontal .thumb:pressed {
            -fx-background-color: #4A90E2;
        }
        .table-view .scroll-bar .increment-button,
        .table-view .scroll-bar .decrement-button {
            -fx-background-color: #252525;
            -fx-border-color: transparent;
        }
        .table-view .scroll-bar .increment-arrow,
        .table-view .scroll-bar .decrement-arrow {
            -fx-shape: " ";
            -fx-background-color: #3C3F41;
        }
        .table-view .scroll-bar:vertical .increment-arrow:hover,
        .table-view .scroll-bar:vertical .decrement-arrow:hover,
        .table-view .scroll-bar:horizontal .increment-arrow:hover,
        .table-view .scroll-bar:horizontal .decrement-arrow:hover {
            -fx-background-color: #4A90E2;
        }
        .table-view .table-row-cell {
            -fx-cell-size: 30px;
            -fx-pref-height: 30px;
            -fx-min-height: 30px;
            -fx-max-height: 30px;
        }
        .table-view .table-cell .text-field {
            -fx-pref-height: 26px;
            -fx-max-height: 26px;
            -fx-min-height: 26px;
            -fx-padding: 2px;
        }
        .table-view .table-column {
            -fx-alignment: CENTER-LEFT;
        }
        """;

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    private Label statusLabel;

    private boolean isValidEnvName(String name, Set<String> nameSet, String currentId, int rowIndex, TableView<String[]> table) {
        if (name == null || name.isEmpty()) {
            return true; // Allow empty names to clear the cell
        }
        if (!name.equalsIgnoreCase(currentId) && nameSet.contains(name.toLowerCase())) {
            return false;
        }
        if (!name.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            return false;
        }
        return true;
    }

    @Override
    public void start(Stage primaryStage) {
        TableView<String[]> table = createTable();

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #FF5555;");
        statusLabel.setWrapText(true);

        HBox buttonsHBox = new HBox(10);
        buttonsHBox.setAlignment(Pos.CENTER);
        buttonsHBox.setStyle("-fx-padding: 10px 10px 20px 10px;");

        Button addButton = new Button("Add");
        addButton.setStyle(BUTTON_STYLE);
        addButton.setOnAction(e -> {
            table.getItems().add(new String[2]);
            int newIndex = table.getItems().size() - 1;
            table.getSelectionModel().select(newIndex);
            table.scrollTo(newIndex);
            statusLabel.setText("");
        });
        addButton.setOnMouseEntered(e -> addButton.setStyle(BUTTON_HOVER_STYLE));
        addButton.setOnMouseExited(e -> addButton.setStyle(BUTTON_STYLE));

        Button deleteButton = new Button("Delete");
        deleteButton.setStyle(BUTTON_STYLE);
        deleteButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().remove(selectedIndex);
                if (!table.getItems().isEmpty()) {
                    int newIndex = Math.min(selectedIndex, table.getItems().size() - 1);
                    table.getSelectionModel().select(newIndex);
                    table.scrollTo(newIndex);
                }
                statusLabel.setText("");
            }
        });
        deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(BUTTON_HOVER_STYLE));
        deleteButton.setOnMouseExited(e -> deleteButton.setStyle(BUTTON_STYLE));
        deleteButton.setDisable(true);

        Button saveButton = new Button("Save");
        saveButton.setStyle(BUTTON_STYLE);
        saveButton.setOnAction(e -> {
            Map<String, String> envMap = new LinkedHashMap<>();
            for (String[] row : table.getItems()) {
                String name = row[0] != null ? row[0].trim() : "";
                String value = row[1] != null ? row[1].trim() : "";
                if (!name.isEmpty()) {
                    envMap.put(name, value);
                }
            }
            try {
                String resourcePath = "src/main/resources/env.json";
                File outputFile = new File(resourcePath);
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    objectMapper.writeValue(fos, envMap);
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Environment variables saved successfully to " + outputFile.getPath());
                alert.showAndWait();
            } catch (IOException ex) {
                showError("Failed to save: " + ex.getMessage());
            }
        });
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(BUTTON_HOVER_STYLE));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(BUTTON_STYLE));

        buttonsHBox.getChildren().addAll(addButton, deleteButton, saveButton);

        // Load from env.json
        try {
            File file = new File("src/main/resources/env.json");
            if (file.exists()) {
                Map<String, String> envMap = objectMapper.readValue(file, new TypeReference<LinkedHashMap<String, String>>() {});
                for (Map.Entry<String, String> entry : envMap.entrySet()) {
                    table.getItems().add(new String[]{entry.getKey(), entry.getValue()});
                }
            }
        } catch (Exception ex) {
            System.err.println("Failed to load env.json: " + ex.getMessage());
        }

        if (!table.getItems().isEmpty()) {
            table.getSelectionModel().select(0);
        }

        table.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            deleteButton.setDisable(newVal.intValue() < 0);
        });

        VBox tableLayout = new VBox(10, table, statusLabel);
        tableLayout.setAlignment(Pos.TOP_CENTER);
        tableLayout.setStyle("-fx-padding: 10px 10px 0px 10px;");

        BorderPane mainLayout = new BorderPane();
        mainLayout.setCenter(tableLayout);
        mainLayout.setBottom(buttonsHBox);
        mainLayout.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 10px; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");

        Scene scene = new Scene(mainLayout);
        scene.getStylesheets().add("data:text/css," + CSS.replaceAll("\n", "%0A"));

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = screenBounds.getWidth() * 0.3;
        double height = screenBounds.getHeight() * 0.3 * 1.96;
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();

        table.prefHeightProperty().bind(mainLayout.heightProperty().subtract(buttonsHBox.heightProperty()).subtract(50).multiply(1.1));

        primaryStage.setScene(scene);
        primaryStage.setTitle("Environment Variables");
        primaryStage.show();
    }

    private TableView<String[]> createTable() {
        TableView<String[]> table = new TableView<>();
        table.setEditable(true);
        table.setStyle("-fx-background-color: #2E2E2E; -fx-table-cell-border-color: transparent; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
        table.setPrefWidth(300);

        Label placeholderLabel = new Label("No environment variables defined");
        placeholderLabel.setStyle("-fx-text-fill: white;");
        table.setPlaceholder(placeholderLabel);

        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            final int index = i;
            TableColumn<String[], String> column = new TableColumn<>(COLUMN_NAMES[i]);
            column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[index]));
            column.setCellFactory(col -> new CustomTextFieldTableCell(table, index, statusLabel));
            double charWidth = 7.0;
            double minWidth = COLUMN_NAMES[i].length() * charWidth + 20;
            column.setMinWidth(minWidth);
            column.setPrefWidth(150);
            column.setStyle("-fx-text-fill: white; -fx-alignment: CENTER-LEFT;");
            column.setOnEditCommit(event -> {
                String newValue = event.getNewValue();
                int colIndex = event.getTablePosition().getColumn();
                int rowIndex = event.getTablePosition().getRow();
                event.getTableView().getItems().get(rowIndex)[colIndex] = newValue;
            });
            table.getColumns().add(column);
        }

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> new TableRow<String[]>() {
            @Override
            protected void updateItem(String[] item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    if (getIndex() == table.getSelectionModel().getSelectedIndex()) {
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

        return table;
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    private class CustomTextFieldTableCell extends TextFieldTableCell<String[], String> {
        private final TableView<String[]> table;
        private final int columnIndex;
        private final Label statusLabel;
        private final Set<String> nameSet = new HashSet<>();
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
                updateNameSet();
            });
            updateNameSet();

            // Enable single-click editing
            addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1 && !isEditing()) {
                    table.getSelectionModel().select(getTableRow().getIndex());
                    startEdit();
                    event.consume();
                }
            });
        }

        private void updateNameSet() {
            nameSet.clear();
            for (String[] row : table.getItems()) {
                String name = row[0];
                if (name != null && !name.isEmpty()) {
                    nameSet.add(name.toLowerCase());
                }
            }
        }

        private String suggestUniqueName(String input) {
            if (input == null || input.isEmpty()) {
                return generateUniqueId("var");
            }
            String base = input.replaceAll("[^a-zA-Z0-9_]", "");
            if (base.isEmpty()) {
                base = "var";
            }
            return generateUniqueId(base);
        }

        private String generateUniqueId(String base) {
            String candidate = base;
            int suffix = 1;
            while (nameSet.contains(candidate.toLowerCase())) {
                candidate = base + "_" + suffix;
                suffix++;
            }
            return candidate;
        }

        private void highlightDuplicateRow(String name) {
            for (int index = 0; index < table.getItems().size(); index++) {
                String[] row = table.getItems().get(index);
                if (row[0] != null && row[0].toLowerCase().equals(name.toLowerCase()) && index != getTableRow().getIndex()) {
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
            if (statusLabel != null) {
                statusLabel.setText("");
            }
        }

        @Override
        public void startEdit() {
            if (table.getItems().isEmpty() || isEditing()) {
                return;
            }
            super.startEdit();
            originalValue = getItem();
            if (getGraphic() instanceof javafx.scene.control.TextField) {
                javafx.scene.control.TextField textField = (javafx.scene.control.TextField) getGraphic();
                textField.setPrefHeight(26);
                textField.setMaxHeight(26);
                textField.setMinHeight(26);
                if (columnIndex == 0) {
                    UnaryOperator<TextFormatter.Change> filter = change -> {
                        String newText = change.getControlNewText();
                        if (newText.isEmpty()) {
                            if (statusLabel != null) {
                                statusLabel.setText("");
                            }
                            clearDuplicateHighlights();
                            return change;
                        }
                        if (!newText.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
                            return null;
                        }
                        if (!newText.equalsIgnoreCase(originalValue) && nameSet.contains(newText.toLowerCase())) {
                            String suggestedId = suggestUniqueName(newText);
                            if (statusLabel != null) {
                                statusLabel.setText("Name '" + newText + "' already exists (case insensitive). Try: " + suggestedId);
                            }
                            highlightDuplicateRow(newText);
                        } else {
                            if (statusLabel != null) {
                                statusLabel.setText("");
                            }
                            clearDuplicateHighlights();
                        }
                        return change;
                    };
                    textField.setTextFormatter(new TextFormatter<>(filter));
                }
                textField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.TAB) {
                        String text = textField.getText() != null ? textField.getText() : "";
                        int rowIndex = getTableRow().getIndex();
                        int newColumn = (columnIndex + 1) % table.getColumns().size();
                        int newRow = rowIndex;
                        if (columnIndex == 0) {
                            if (!text.isEmpty() && (!isValidEnvName(text, nameSet, originalValue, rowIndex, table))) {
                                showError("Cannot commit duplicate/invalid name: " + text);
                                Platform.runLater(textField::requestFocus); // Retain focus in current cell
                                e.consume();
                                return;
                            }
                        }
                        commitEdit(getConverter().fromString(text));
                        if (newColumn == 0 && rowIndex < table.getItems().size() - 1) {
                            newRow = rowIndex + 1; // Move to next row if at last column
                        }
                        final int finalNewRow = newRow;
                        final int finalNewColumn = newColumn;
                        Platform.runLater(() -> {
                            table.getSelectionModel().clearAndSelect(finalNewRow);
                            table.getFocusModel().focus(finalNewRow, table.getColumns().get(finalNewColumn));
                            table.edit(finalNewRow, table.getColumns().get(finalNewColumn));
                            table.scrollTo(finalNewRow);
                            table.scrollToColumn(table.getColumns().get(finalNewColumn));
                            TableCell<?, ?> nextCell = getTableCellAt(finalNewRow, finalNewColumn);
                            if (nextCell != null && nextCell instanceof CustomTextFieldTableCell) {
                                CustomTextFieldTableCell nextTextCell = (CustomTextFieldTableCell) nextCell;
                                if (nextTextCell.getGraphic() instanceof javafx.scene.control.TextField) {
                                    ((javafx.scene.control.TextField) nextTextCell.getGraphic()).requestFocus();
                                }
                            }
                        });
                        e.consume();
                    } else if (e.getCode() == KeyCode.ENTER) {
                        String text = textField.getText() != null ? textField.getText() : "";
                        if (columnIndex == 0) {
                            int rowIndex = getTableRow().getIndex();
                            if (!text.isEmpty() && (!isValidEnvName(text, nameSet, originalValue, rowIndex, table))) {
                                showError("Cannot commit duplicate/invalid name: " + text);
                                Platform.runLater(textField::requestFocus); // Retain focus in current cell
                                e.consume();
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
                Platform.runLater(textField::requestFocus);
            }
        }

        private TableCell<?, ?> getTableCellAt(int rowIndex, int columnIndex) {
            for (Node node : table.lookupAll(".table-cell")) {
                if (node instanceof TableCell) {
                    TableCell<?, ?> cell = (TableCell<?, ?>) node;
                    if (cell.getTableRow() != null && cell.getTableRow().getIndex() == rowIndex && cell.getTableColumn() == table.getColumns().get(columnIndex)) {
                        return cell;
                    }
                }
            }
            return null;
        }

        @Override
        public void commitEdit(String newValue) {
            if (columnIndex == 0 && !newValue.isEmpty()) {
                int rowIndex = getTableRow().getIndex();
                if (!isValidEnvName(newValue, nameSet, originalValue, rowIndex, table)) {
                    return;
                }
            }
            super.commitEdit(newValue);
            if (columnIndex == 0) {
                clearDuplicateHighlights();
                updateNameSet();
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
                // Do not revert to originalValue, allow empty cell
                table.refresh();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}