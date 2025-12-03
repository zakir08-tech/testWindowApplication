package com.test.window.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.test.window.app.UIConstants;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * SSL Configuration Manager - Now saves in proper nested JSON format
 */
public class SSLConfigurationScreen extends Application {
	
	static Stage instance;
	
    private static final String[] COLUMN_NAMES = {
        "SSL Name",
        "Keystore (path)",
        "Keystore (password)",
        "Truststore (path)",
        "Truststore (password)"
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
    private TableView<String[]> table;
    private boolean hasUnsavedChanges = false;

    private boolean isValidSslName(String name, Set<String> nameSet, String currentId, int rowIndex, TableView<String[]> table) {
        if (name == null || name.isEmpty()) return true;
        if (!name.equalsIgnoreCase(currentId) && nameSet.contains(name.toLowerCase())) return false;
        if (!name.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) return false;
        return true;
    }

    private void stopEditing() {
        if (table.getEditingCell() != null) {
            TablePosition<String[], ?> editingCell = table.getEditingCell();
            TableCell<String[], String> cell = getTableCellAt(editingCell.getRow(), editingCell.getColumn());
            if (cell instanceof CustomTextFieldTableCell) {
                CustomTextFieldTableCell customCell = (CustomTextFieldTableCell) cell;
                if (customCell.getGraphic() instanceof javafx.scene.control.TextField) {
                    String text = ((javafx.scene.control.TextField) customCell.getGraphic()).getText();
                    if (customCell.columnIndex == 0 && !text.isEmpty()) {
                        if (isValidSslName(text, customCell.nameSet, customCell.originalValue, editingCell.getRow(), table)) {
                            customCell.commitEdit(text);
                        } else {
                            customCell.cancelEdit();
                        }
                    } else {
                        customCell.commitEdit(text);
                    }
                }
            }
        }
    }

    private TableCell<String[], String> getTableCellAt(int rowIndex, int columnIndex) {
        for (Node node : table.lookupAll(".table-cell")) {
            if (node instanceof TableCell) {
                TableCell<?, ?> cell = (TableCell<?, ?>) node;
                if (cell.getTableRow() != null && cell.getTableRow().getIndex() == rowIndex && cell.getTableColumn() == table.getColumns().get(columnIndex)) {
                    return (TableCell<String[], String>) cell;
                }
            }
        }
        return null;
    }

    private void updateUnsavedChangesFlag() {
        hasUnsavedChanges = true; // Simple but safe — we have new format
    }

    private void reloadEnvJson() {
        try {
            File file = new File("ssl.json");
            if (!file.exists()) {
                table.getItems().clear();
                hasUnsavedChanges = false;
                return;
            }

            table.getItems().clear();

            // Read as generic Map to support both old and new format
            Map<String, Object> root = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});

            for (Map.Entry<String, Object> entry : root.entrySet()) {
                String sslName = entry.getKey();
                Object config = entry.getValue();

                String[] row = new String[COLUMN_NAMES.length];
                row[0] = sslName;
                row[1] = ""; row[2] = ""; row[3] = ""; row[4] = "";

                if (config instanceof Map<?, ?> map) {
                    // New format: keystore/truststore with arrays
                    Object ks = map.get("keystore");
                    Object ts = map.get("truststore");

                    if (ks instanceof java.util.List<?> ksList && ksList.size() >= 2) {
                        row[1] = ksList.get(0).toString();
                        row[2] = ksList.get(1).toString();
                    }
                    if (ts instanceof java.util.List<?> tsList && tsList.size() >= 2) {
                        row[3] = tsList.get(0).toString();
                        row[4] = tsList.get(1).toString();
                    }
                } else if (config instanceof String) {
                    // Backward compatibility: old flat format
                    row[1] = config.toString();
                }

                table.getItems().add(row);
            }

            if (!table.getItems().isEmpty()) {
                Platform.runLater(() -> {
                    table.getSelectionModel().select(0);
                    table.scrollTo(0);
                });
            }
            hasUnsavedChanges = false;
        } catch (Exception ex) {
            System.err.println("Failed to load ssl.json: " + ex.getMessage());
            hasUnsavedChanges = true;
        }
    }

    @Override
    public void start(Stage primaryStage) {
    	instance = primaryStage;
    	
    	try {
            Image icon = new Image("file:" + UIConstants.UI_ICON);
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {}

        table = createTable();

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #FF5555;");
        statusLabel.setWrapText(true);

        HBox buttonsHBox = new HBox(10);
        buttonsHBox.setAlignment(Pos.CENTER);
        buttonsHBox.setStyle("-fx-padding: 10px;");

        Button addButton = new Button("Add");
        addButton.setStyle(BUTTON_STYLE);
        addButton.setOnAction(e -> {
            stopEditing();
            table.getItems().add(new String[COLUMN_NAMES.length]);
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
            stopEditing();
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                table.getItems().remove(idx);
                if (!table.getItems().isEmpty()) {
                    table.getSelectionModel().select(Math.min(idx, table.getItems().size() - 1));
                }
            }
        });
        deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(BUTTON_HOVER_STYLE));
        deleteButton.setOnMouseExited(e -> deleteButton.setStyle(BUTTON_STYLE));
        deleteButton.setDisable(true);

        Button saveButton = new Button("Save");
        saveButton.setStyle(BUTTON_STYLE);
        saveButton.setOnAction(e -> {
            stopEditing();

            Map<String, Map<String, Object>> sslConfigs = new LinkedHashMap<>();

            for (String[] row : table.getItems()) {
                String name = row[0] != null ? row[0].trim() : "";
                if (name.isEmpty()) continue;

                String ksPath = row.length > 1 && row[1] != null ? row[1].trim() : "";
                String ksPass = row.length > 2 && row[2] != null ? row[2].trim() : "";
                String tsPath = row.length > 3 && row[3] != null ? row[3].trim() : "";
                String tsPass = row.length > 4 && row[4] != null ? row[4].trim() : "";

                Map<String, Object> profile = new LinkedHashMap<>();
                profile.put("keystore", java.util.List.of(ksPath, ksPass));
                profile.put("truststore", java.util.List.of(tsPath, tsPass));

                sslConfigs.put(name, profile);
            }

            try {
                File file = new File("ssl.json");
                if (file.getParentFile() != null) file.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    objectMapper.writeValue(fos, sslConfigs);
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setContentText("SSL configurations saved successfully!");
                alert.showAndWait();
                hasUnsavedChanges = false;
                UIComponentsManager.getInstance().loadSslProfilesIntoComboBox();
            } catch (IOException ex) {
                showError("Save failed: " + ex.getMessage());
            }
        });
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(BUTTON_HOVER_STYLE));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(BUTTON_STYLE));

        buttonsHBox.getChildren().addAll(addButton, deleteButton, saveButton);

        reloadEnvJson();

        table.getItems().addListener((javafx.collections.ListChangeListener<String[]>) c -> {
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasUpdated()) {
                    hasUnsavedChanges = true;
                }
            }
        });

        primaryStage.focusedProperty().addListener((obs, oldV, newV) -> {
            if (newV && !hasUnsavedChanges) reloadEnvJson();
        });

        table.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> 
            deleteButton.setDisable(newV.intValue() < 0));

        VBox tableLayout = new VBox(0, table, statusLabel);
        tableLayout.setAlignment(Pos.TOP_CENTER);

        BorderPane mainLayout = new BorderPane();
        mainLayout.setCenter(tableLayout);
        mainLayout.setBottom(buttonsHBox);
        mainLayout.setStyle("-fx-background-color: #2E2E2E;");

        Scene scene = new Scene(mainLayout);
        scene.getStylesheets().add("data:text/css," + CSS.replaceAll("\n", "%0A"));

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = screenBounds.getWidth() * 0.65;
        double height = screenBounds.getHeight() * 0.7;

        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(500);
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.setResizable(true);  // FULLY RESIZABLE
        primaryStage.centerOnScreen();

        table.prefWidthProperty().bind(mainLayout.widthProperty());
        table.prefHeightProperty().bind(mainLayout.heightProperty().subtract(buttonsHBox.heightProperty().add(40)));

        primaryStage.setScene(scene);
        primaryStage.setTitle("SSL Configuration Manager");
        primaryStage.show();
        
        primaryStage.setOnCloseRequest(e -> instance = null);
    }
    
    public static boolean isShowing() {
        return instance != null && instance.isShowing();
    }
    
    private TableView<String[]> createTable() {
        TableView<String[]> table = new TableView<>();
        table.setEditable(true);
        table.setStyle("-fx-background-color: #2E2E2E; -fx-table-cell-border-color: transparent; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");

        Label placeholderLabel = new Label("No SSL configurations defined");
        placeholderLabel.setStyle("-fx-text-fill: white;");
        table.setPlaceholder(placeholderLabel);

        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            final int index = i;
            TableColumn<String[], String> column = new TableColumn<>(COLUMN_NAMES[i]);
            column.setCellValueFactory(cellData -> {
                String[] row = cellData.getValue();
                return new SimpleStringProperty(
                    row != null && row.length > index && row[index] != null ? row[index] : ""
                );
            });

            column.setCellFactory(col -> new CustomTextFieldTableCell(table, index, statusLabel));

            double charWidth = 7.0;
            double minWidth = COLUMN_NAMES[i].length() * charWidth + 30;
            column.setMinWidth(minWidth);
            column.setPrefWidth(180);
            column.setStyle("-fx-text-fill: white; -fx-alignment: CENTER-LEFT;");

            // ONLY THIS LINE ADDED:
            column.setSortable(i == 0);  // Only SSL Name column is sortable

            column.setOnEditCommit(event -> {
                String newValue = event.getNewValue() != null ? event.getNewValue() : "";
                int rowIndex = event.getTablePosition().getRow();
                String[] row = table.getItems().get(rowIndex);
                if (row.length <= index) {
                    String[] newRow = new String[COLUMN_NAMES.length];
                    System.arraycopy(row, 0, newRow, 0, row.length);
                    for (int j = row.length; j < newRow.length; j++) newRow[j] = "";
                    table.getItems().set(rowIndex, newRow);
                    row = newRow;
                }
                row[index] = newValue;
                hasUnsavedChanges = true;
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
                        setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white;");
                    } else {
                        setStyle("-fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px;");
                    }
                }
            }
        });

        table.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> table.refresh());

        return table;
    }

    private static void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    private class CustomTextFieldTableCell extends TextFieldTableCell<String[], String> {
        private final TableView<String[]> table;
        private final int columnIndex;
        private final Label statusLabel;
        private final Set<String> nameSet = new HashSet<>();
        private String originalValue;

        public CustomTextFieldTableCell(TableView<String[]> table, int columnIndex, Label statusLabel) {
            super(new StringConverter<>() {
                @Override public String toString(String obj) { return obj == null ? "" : obj; }
                @Override public String fromString(String str) { return str; }
            });
            this.table = table;
            this.columnIndex = columnIndex;
            this.statusLabel = statusLabel;

            table.getItems().addListener((javafx.collections.ListChangeListener<String[]>) c -> updateNameSet());
            updateNameSet();

            addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1 && !isEditing()) {
                    table.getSelectionModel().select(getIndex());
                    startEdit();
                    e.consume();
                }
            });
        }

        private void updateNameSet() {
            nameSet.clear();
            for (String[] row : table.getItems()) {
                if (row.length > 0 && row[0] != null && !row[0].trim().isEmpty()) {
                    nameSet.add(row[0].trim().toLowerCase());
                }
            }
        }

        @Override
        public void startEdit() {
            if (isEmpty()) return;
            super.startEdit();
            originalValue = getItem();
            if (getGraphic() instanceof javafx.scene.control.TextField tf) {
                if (columnIndex == 0) {
                    tf.setTextFormatter(new TextFormatter<>(change -> {
                        String text = change.getControlNewText();
                        if (text.isEmpty()) return change;
                        if (!text.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) return null;
                        if (!text.equalsIgnoreCase(originalValue) && nameSet.contains(text.toLowerCase())) {
                            statusLabel.setText("SSL Name already exists!");
                            return null;
                        }
                        statusLabel.setText("");
                        return change;
                    }));
                }
                tf.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.TAB) {
                        commitEdit(tf.getText());
                        e.consume();
                    } else if (e.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                        e.consume();
                    }
                });
                Platform.runLater(() -> { tf.requestFocus(); tf.selectAll(); });
            }
        }

        @Override
        public void commitEdit(String newValue) {
            if (columnIndex == 0 && !newValue.isEmpty() && !isValidSslName(newValue, nameSet, originalValue, getIndex(), table)) {
                return;
            }
            super.commitEdit(newValue);
            if (columnIndex == 0) {
                updateNameSet();
                statusLabel.setText("");
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}