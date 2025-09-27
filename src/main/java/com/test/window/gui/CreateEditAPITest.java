package com.test.window.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.scene.control.TextFormatter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public class CreateEditAPITest extends Application {

    private static final String[] COLUMN_NAMES = {
        "Test ID", "Request", "URL", "Header (key)", "Header (value)",
        "Parameter (key)", "Parameter (value)", "Payload", "Payload Type",
        "Modify Payload (key)", "Modify Payload (value)", "Response (key) Name",
        "Capture (key) Value (env var)", "Authorization", "", "",
        "SSL Validation", "Expected Status", "Test Description"
    };

    private static final ObservableList<String> REQUEST_OPTIONS = 
        FXCollections.observableArrayList("", "GET", "POST", "PUT", "PATCH", "DELETE");

    private static final String FIELD_STYLE_UNFOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #888888; -fx-border-radius: 5px;";
    private static final String FIELD_STYLE_FOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #4A90E2; -fx-border-width: 2px; -fx-prompt-text-fill: #888888; -fx-border-radius: 5px;";

    private static final String BUTTON_STYLE = 
        "-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 40px; -fx-max-width: 40px;";

    private static final String CSS = """
        .table-view .scroll-bar:vertical,
        .table-view .scroll-bar:horizontal,
        .scroll-pane .scroll-bar:vertical,
        .scroll-pane .scroll-bar:horizontal {
            -fx-background-color: #252525;
        }
        .table-view .scroll-bar:vertical .track,
        .table-view .scroll-bar:horizontal .track,
        .scroll-pane .scroll-bar:vertical .track,
        .scroll-pane .scroll-bar:horizontal .track {
            -fx-background-color: #252525;
            -fx-border-color: transparent;
            -fx-background-radius: 0px;
        }
        .table-view .scroll-bar:vertical .thumb,
        .table-view .scroll-bar:horizontal .thumb,
        .scroll-pane .scroll-bar:vertical .thumb,
        .scroll-pane .scroll-bar:horizontal .thumb {
            -fx-background-color: #3C3F41;
            -fx-background-radius: 5px;
        }
        .table-view .scroll-bar:vertical .thumb:hover,
        .table-view .scroll-bar:horizontal .thumb:hover,
        .scroll-pane .scroll-bar:vertical .thumb:hover,
        .scroll-pane .scroll-bar:horizontal .thumb:hover {
            -fx-background-color: #4A90E2;
        }
        .table-view .scroll-bar:vertical .thumb:pressed,
        .table-view .scroll-bar:horizontal .thumb:pressed,
        .scroll-pane .scroll-bar:vertical .thumb:pressed,
        .scroll-pane .scroll-bar:horizontal .thumb:pressed {
            -fx-background-color: #4A90E2;
        }
        .table-view .scroll-bar .increment-button,
        .table-view .scroll-bar .decrement-button,
        .scroll-pane .scroll-bar .increment-button,
        .scroll-pane .scroll-bar .decrement-button {
            -fx-background-color: #252525;
            -fx-border-color: transparent;
        }
        .table-view .scroll-bar .increment-arrow,
        .table-view .scroll-bar .decrement-arrow,
        .scroll-pane .scroll-bar .increment-arrow,
        .scroll-pane .scroll-bar .decrement-arrow {
            -fx-shape: " ";
            -fx-background-color: #3C3F41;
        }
        .table-view .scroll-bar:vertical .increment-arrow:hover,
        .table-view .scroll-bar:vertical .decrement-arrow:hover,
        .table-view .scroll-bar:horizontal .increment-arrow:hover,
        .table-view .scroll-bar:horizontal .decrement-arrow:hover,
        .scroll-pane .scroll-bar:vertical .increment-arrow:hover,
        .scroll-pane .scroll-bar:vertical .decrement-arrow:hover,
        .scroll-pane .scroll-bar:horizontal .increment-arrow:hover,
        .scroll-pane .scroll-bar:horizontal .decrement-arrow:hover {
            -fx-background-color: #4A90E2;
        }
        .scroll-pane,
        .scroll-pane .viewport {
            -fx-background-color: #2E2E2E;
        }
        .table-view .table-row-cell {
            -fx-cell-size: 30px; /* Fixed row height */
            -fx-pref-height: 30px;
            -fx-min-height: 30px;
            -fx-max-height: 30px;
        }
        .table-view .table-cell .text-field {
            -fx-pref-height: 26px; /* Slightly smaller to fit within row */
            -fx-max-height: 26px;
            -fx-min-height: 26px;
            -fx-padding: 2px;
        }
        .text-area {
            -fx-font-family: 'Consolas', 'Menlo', 'Courier New', monospace;
            -fx-font-size: 12px;
            -fx-padding: 5px;
        }
        """;

    private static final double TEXT_FIELD_HEIGHT = 30.0; // Constant height for all TextFields
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS); // Note: Jackson does not preserve insertion order by default, handled in formatJson

    private Label statusLabel; // Field for status label
    private VBox mainLayout; // Field for main layout

    // Method to format JSON string if valid, otherwise return original text
    private String formatJson(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input != null ? input : "";
        }
        try {
            // Parse JSON while preserving order using LinkedHashMap
            Object parsedJson = objectMapper.readValue(input, LinkedHashMap.class);
            // Write with 4-space indentation
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedJson);
        } catch (Exception e) {
            // Not valid JSON, return original text
            return input;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // Table setup
        TableView<String[]> table = createTable();

        // Buttons setup
        VBox buttonsVBox = createButtonsVBox(table);

        // HBox for table and buttons
        HBox tableWithButtons = new HBox(10, table, buttonsVBox);
        tableWithButtons.setStyle("-fx-background-color: #2E2E2E;");
        tableWithButtons.prefHeightProperty().bind(table.prefHeightProperty());

        // Set growth priorities
        HBox.setHgrow(table, Priority.ALWAYS);
        HBox.setHgrow(buttonsVBox, Priority.NEVER);
        VBox.setVgrow(tableWithButtons, Priority.NEVER); // Prevent HBox from expanding vertically

        // Bind table height to 60% of stage height
        table.prefHeightProperty().bind(primaryStage.heightProperty().multiply(0.6));

        // Text fields
        HBox textFieldsBox = createTextFieldsBox(table);

        // Scroll panel for additional objects under urlField
        VBox additionalContent = new VBox(10);
        additionalContent.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
        additionalContent.setAlignment(Pos.CENTER_LEFT); // Align TextFields to the left

        // Header Fields (VBox containing HBox pairs of TextFields, wrapped in ScrollPane)
        VBox headerFieldsVBox = new VBox(5);
        headerFieldsVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");

        ScrollPane headerFieldsScroll = new ScrollPane(headerFieldsVBox);
        headerFieldsScroll.setStyle("-fx-background-color: #2E2E2E; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
        headerFieldsScroll.setFitToWidth(true);
        headerFieldsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Disable horizontal scroll bar
        headerFieldsScroll.setPrefHeight(200); // Fixed height

        // Parameter Fields (VBox containing HBox pairs of TextFields, wrapped in ScrollPane)
        VBox paramFieldsVBox = new VBox(5);
        paramFieldsVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");

        ScrollPane paramListField = new ScrollPane(paramFieldsVBox);
        paramListField.setStyle("-fx-background-color: #2E2E2E; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
        paramListField.setFitToWidth(true);
        paramListField.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Disable horizontal scroll bar
        paramListField.setPrefHeight(200); // Fixed height to match headerFieldsScroll

        // Payload Field (TextArea)
        TextArea payloadField = new TextArea();
        payloadField.setPromptText("Payload");
        payloadField.setStyle(FIELD_STYLE_UNFOCUSED);
        payloadField.setPrefHeight(200);
        payloadField.setWrapText(true); // Enable text wrapping
        payloadField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            payloadField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });

        // Handle TAB key in payloadField
        payloadField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB) {
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0) {
                    // Commit current text (raw) to table
                    String rawText = payloadField.getText();
                    table.getItems().get(selectedIndex)[7] = rawText;
                    table.refresh();
                    // Format for display
                    String formattedText = formatJson(rawText);
                    payloadField.setText(formattedText);
                }
                // Move focus to the table or first header TextField
                if (!headerFieldsVBox.getChildren().isEmpty()) {
                    HBox firstHeaderPair = (HBox) headerFieldsVBox.getChildren().get(0);
                    TextField firstHeaderField = (TextField) firstHeaderPair.getChildren().get(0);
                    firstHeaderField.requestFocus();
                } else {
                    table.requestFocus();
                    if (selectedIndex >= 0) {
                        table.getSelectionModel().select(selectedIndex);
                        table.getFocusModel().focus(selectedIndex, table.getColumns().get(0));
                    }
                }
                event.consume();
            }
        });

        // Use GridPane for proportional widths
        GridPane additionalFields = new GridPane();
        additionalFields.setHgap(10);
        additionalFields.setAlignment(Pos.CENTER_LEFT);
        additionalFields.add(headerFieldsScroll, 0, 0);
        additionalFields.add(paramListField, 1, 0);
        additionalFields.add(payloadField, 2, 0);

        // Set column constraints for 26.73%, 26.73%, 46.54%
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(26.73); // headerFieldsScroll
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(26.73); // paramListField
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(46.54); // payloadField
        additionalFields.getColumnConstraints().addAll(col1, col2, col3);

        additionalContent.getChildren().add(additionalFields);

        // Bind additional fields to table selection
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            headerFieldsVBox.getChildren().clear(); // Clear previous header TextFields
            paramFieldsVBox.getChildren().clear(); // Clear previous parameter TextFields
            if (newItem != null) {
                int selectedIndex = table.getSelectionModel().getSelectedIndex();
                String testId = newItem[0];
                if (testId == null || testId.isEmpty()) {
                    // Do not populate if test ID is empty
                    payloadField.clear();
                    return;
                }

                int start = selectedIndex;
                while (start >= 0 && (table.getItems().get(start)[0] == null || table.getItems().get(start)[0].isEmpty())) {
                    start--;
                }
                if (start < 0) start = 0;

                List<Integer> rowIndices = new ArrayList<>();
                for (int i = start; i < table.getItems().size(); i++) {
                    String[] r = table.getItems().get(i);
                    if (i > start && r[0] != null && !r[0].isEmpty()) break; // Stop at next test ID
                    rowIndices.add(i);
                }

                // Create TextField pairs for headers (indices 3 and 4)
                for (Integer rowIndex : rowIndices) {
                    String[] row = table.getItems().get(rowIndex);
                    TextField headerKeyField = new TextField(row[3] != null ? row[3] : "");
                    headerKeyField.setPromptText("Header Key");
                    headerKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                    headerKeyField.setPrefHeight(TEXT_FIELD_HEIGHT); // Constant height
                    headerKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        headerKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    });

                    TextField headerValueField = new TextField(row[4] != null ? row[4] : "");
                    headerValueField.setPromptText("Header Value");
                    headerValueField.setStyle(FIELD_STYLE_UNFOCUSED);
                    headerValueField.setPrefHeight(TEXT_FIELD_HEIGHT); // Constant height
                    headerValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        headerValueField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    });

                    // Create HBox for the header pair
                    HBox headerPair = new HBox(5, headerKeyField, headerValueField);
                    headerPair.setAlignment(Pos.CENTER_LEFT);

                    // Set equal widths for TextFields (50% of headerFieldsScroll viewport width minus padding, spacing, and 2px gap)
                    headerKeyField.prefWidthProperty().bind(headerFieldsScroll.widthProperty().multiply(0.5).subtract(8.5));
                    headerKeyField.maxWidthProperty().bind(headerKeyField.prefWidthProperty());
                    headerKeyField.minWidthProperty().bind(headerKeyField.prefWidthProperty());
                    headerValueField.prefWidthProperty().bind(headerFieldsScroll.widthProperty().multiply(0.5).subtract(8.5));
                    headerValueField.maxWidthProperty().bind(headerValueField.prefWidthProperty());
                    headerValueField.minWidthProperty().bind(headerValueField.prefWidthProperty());

                    // Update table when header TextFields change
                    headerKeyField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[3] = newVal2.trim();
                        table.refresh();
                    });

                    headerValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[4] = newVal2.trim();
                        table.refresh();
                    });

                    headerFieldsVBox.getChildren().add(headerPair);

                    // Create TextField pairs for parameters (indices 5 and 6)
                    TextField paramKeyField = new TextField(row[5] != null ? row[5] : "");
                    paramKeyField.setPromptText("Parameter Key");
                    paramKeyField.setStyle(FIELD_STYLE_UNFOCUSED);
                    paramKeyField.setPrefHeight(TEXT_FIELD_HEIGHT); // Constant height
                    paramKeyField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        paramKeyField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    });

                    TextField paramValueField = new TextField(row[6] != null ? row[6] : "");
                    paramValueField.setPromptText("Parameter Value");
                    paramValueField.setStyle(FIELD_STYLE_UNFOCUSED);
                    paramValueField.setPrefHeight(TEXT_FIELD_HEIGHT); // Constant height
                    paramValueField.focusedProperty().addListener((obs2, oldVal2, newVal2) -> {
                        paramValueField.setStyle(newVal2 ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
                    });

                    // Create HBox for the parameter pair
                    HBox paramPair = new HBox(5, paramKeyField, paramValueField);
                    paramPair.setAlignment(Pos.CENTER_LEFT);

                    // Set equal widths for TextFields (50% of paramListField viewport width minus padding, spacing, and 2px gap)
                    paramKeyField.prefWidthProperty().bind(paramListField.widthProperty().multiply(0.5).subtract(8.5));
                    paramKeyField.maxWidthProperty().bind(paramKeyField.prefWidthProperty());
                    paramKeyField.minWidthProperty().bind(paramKeyField.prefWidthProperty());
                    paramValueField.prefWidthProperty().bind(paramListField.widthProperty().multiply(0.5).subtract(8.5));
                    paramValueField.maxWidthProperty().bind(paramValueField.prefWidthProperty());
                    paramValueField.minWidthProperty().bind(paramValueField.prefWidthProperty());

                    // Update table when parameter TextFields change
                    paramKeyField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[5] = newVal2.trim();
                        table.refresh();
                    });

                    paramValueField.textProperty().addListener((obs2, oldVal2, newVal2) -> {
                        table.getItems().get(rowIndex)[6] = newVal2.trim();
                        table.refresh();
                    });

                    paramFieldsVBox.getChildren().add(paramPair);
                }

                // For payload (only for selected row), format JSON if valid
                String payload = newItem[7] != null ? newItem[7] : "";
                payloadField.setText(formatJson(payload));
            } else {
                payloadField.clear();
            }
        });

        // Update table when payload field changes, store raw text but display formatted
        payloadField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                // Store the raw (unformatted) text in the table
                table.getItems().get(selectedIndex)[7] = newVal;
                table.refresh();
                // Reformat the text for display in payloadField
                String formattedText = formatJson(newVal);
                if (!formattedText.equals(newVal)) {
                    // Prevent infinite loop by checking if reformatting changes the text
                    Platform.runLater(() -> payloadField.setText(formattedText));
                }
            }
        });

        ScrollPane scrollPane = new ScrollPane(additionalContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #2E2E2E;");

        // Main layout
        mainLayout = new VBox(10, tableWithButtons, textFieldsBox, scrollPane);
        mainLayout.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 10px; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
        mainLayout.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Scene and Stage
        Scene scene = new Scene(mainLayout, 800, 600);
        scene.getStylesheets().add("data:text/css," + CSS.replaceAll("\n", "%0A"));
        primaryStage.setMaximized(true);
        primaryStage.setTitle("Table with JSON Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private TableView<String[]> createTable() {
        TableView<String[]> table = new TableView<>();
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setStyle("-fx-background-color: #2E2E2E; -fx-table-cell-border-color: transparent; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; -fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-border-radius: 5px;");
        table.setPrefWidth(480); // 60% of original scene width (800px * 0.6)

        // Set placeholder
        Label placeholderLabel = new Label("No steps defined");
        placeholderLabel.setStyle("-fx-text-fill: white;");
        table.setPlaceholder(placeholderLabel);

        // Create columns
        for (int i = 0; i < COLUMN_NAMES.length; i++) {
            final int index = i;
            TableColumn<String[], String> column = new TableColumn<>(COLUMN_NAMES[i]);
            column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()[index]));
            if (index == 1) { // Request column with ComboBox
                column.setCellFactory(col -> new CustomComboBoxTableCell(table, REQUEST_OPTIONS));
            } else {
                column.setCellFactory(col -> new CustomTextFieldTableCell(table, index, statusLabel));
            }
            column.setMinWidth(18); // 60% of original 30px
            column.setPrefWidth(index == 0 ? 24 : 90); // Test ID: 40px * 0.6 = 24px, others: 150px * 0.6 = 90px
            column.setStyle("-fx-text-fill: white;");
            column.setOnEditCommit(event -> {
                String newValue = event.getNewValue();
                int colIndex = event.getTablePosition().getColumn();
                int rowIndex = event.getTablePosition().getRow();
                if (colIndex == 2 && !newValue.matches("^(https?://)?[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}(/.*)?$|^$")) {
                    statusLabel.setText("Invalid URL format");
                    return;
                } else if (colIndex == 17 && !newValue.matches("\\d+|^$")) {
                    statusLabel.setText("Status must be a number");
                    return;
                }
                statusLabel.setText("");
                event.getTableView().getItems().get(rowIndex)[colIndex] = newValue;
                // Update payloadField if the edited column is Payload (index 7) and the row is selected
                if (colIndex == 7 && rowIndex == table.getSelectionModel().getSelectedIndex()) {
                    TextArea payloadField = (TextArea) ((GridPane) ((VBox) ((ScrollPane) mainLayout.getChildren().get(2)).getContent()).getChildren().get(0)).getChildren().get(2);
                    payloadField.setText(formatJson(newValue));
                    // Force table refresh to ensure consistent row height
                    table.refresh();
                }
            });
            table.getColumns().add(column);
        }

        // Set unconstrained resize policy
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Row factory for highlighting and borders
        table.setRowFactory(tv -> new TableRow<String[]>() {
            @Override
            protected void updateItem(String[] item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    int index = getIndex();
                    // Check if this row is highlighted as a duplicate (style set elsewhere)
                    String currentStyle = getStyle();
                    if (currentStyle.contains("4A90E2")) { // If highlighted as duplicate
                        return; // Preserve duplicate highlight
                    }
                    if (index == table.getSelectionModel().getSelectedIndex()) {
                        setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px;");
                    } else {
                        setStyle("-fx-table-cell-border-color: #3C3F41; -fx-table-cell-border-width: 1px;");
                    }
                }
            }
        });

        // Refresh table on selection change
        table.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            table.refresh();
        });

        // Select first row by default when items are added
        table.getItems().addListener((javafx.collections.ListChangeListener<String[]>) c -> {
            if (!table.getItems().isEmpty() && table.getSelectionModel().getSelectedIndex() < 0) {
                table.getSelectionModel().select(0);
            }
        });

        return table;
    }

    private VBox createButtonsVBox(TableView<String[]> table) {
        VBox buttonsVBox = new VBox(10);
        buttonsVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 10px;");
        buttonsVBox.setAlignment(Pos.TOP_CENTER);
        String buttonStyle = "-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

        Button addStepButton = new Button("Add Step");
        addStepButton.setStyle(buttonStyle);
        addStepButton.setTooltip(new Tooltip("Add a new test step to the table"));
        addStepButton.setOnAction(e -> {
            table.getItems().add(new String[COLUMN_NAMES.length]);
            table.getSelectionModel().select(table.getItems().size() - 1); // Select new row
        });

        Button addAboveButton = new Button("Add Above");
        addAboveButton.setStyle(buttonStyle);
        addAboveButton.setTooltip(new Tooltip("Add a new step above the selected row"));
        addAboveButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().add(selectedIndex, new String[COLUMN_NAMES.length]);
                table.getSelectionModel().select(selectedIndex); // Keep selection on new row
            }
        });

        Button addBelowButton = new Button("Add Below");
        addBelowButton.setStyle(buttonStyle);
        addBelowButton.setTooltip(new Tooltip("Add a new step below the selected row"));
        addBelowButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().add(selectedIndex + 1, new String[COLUMN_NAMES.length]);
                table.getSelectionModel().select(selectedIndex + 1); // Select new row
            }
        });

        Button moveUpButton = new Button("Move Up");
        moveUpButton.setStyle(buttonStyle);
        moveUpButton.setTooltip(new Tooltip("Move the selected step up"));
        moveUpButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex > 0) {
                ObservableList<String[]> items = table.getItems();
                String[] temp = items.get(selectedIndex - 1);
                items.set(selectedIndex - 1, items.get(selectedIndex));
                items.set(selectedIndex, temp);
                table.getSelectionModel().select(selectedIndex - 1);
            }
        });

        Button moveDownButton = new Button("Move Down");
        moveDownButton.setStyle(buttonStyle);
        moveDownButton.setTooltip(new Tooltip("Move the selected step down"));
        moveDownButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < table.getItems().size() - 1) {
                ObservableList<String[]> items = table.getItems();
                String[] temp = items.get(selectedIndex + 1);
                items.set(selectedIndex + 1, items.get(selectedIndex));
                items.set(selectedIndex, temp);
                table.getSelectionModel().select(selectedIndex + 1);
            }
        });

        Button deleteStepButton = new Button("Delete Step");
        deleteStepButton.setStyle(buttonStyle);
        deleteStepButton.setTooltip(new Tooltip("Delete the selected step"));
        deleteStepButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().remove(selectedIndex);
            }
        });

        Button deleteTestCaseButton = new Button("Delete Test Case");
        deleteTestCaseButton.setStyle(buttonStyle);
        deleteTestCaseButton.setTooltip(new Tooltip("Clear all steps in the test case"));
        deleteTestCaseButton.setOnAction(e -> {
            table.getItems().clear();
        });

        Button saveTestButton = new Button("Save Test");
        saveTestButton.setStyle(buttonStyle);
        saveTestButton.setTooltip(new Tooltip("Save the test case to a file"));
        saveTestButton.setOnAction(e -> {
            System.out.println("Save Test clicked"); // Placeholder for save logic
        });

        Button createNewTestButton = new Button("Create New Test");
        createNewTestButton.setStyle(buttonStyle);
        createNewTestButton.setTooltip(new Tooltip("Start a new test case"));
        createNewTestButton.setOnAction(e -> {
            table.getItems().clear();
        });

        Button addEditEnvVarButton = new Button("Add/Edit Env Var");
        addEditEnvVarButton.setStyle(buttonStyle);
        addEditEnvVarButton.setTooltip(new Tooltip("Add or edit environment variables"));
        addEditEnvVarButton.setOnAction(e -> {
            System.out.println("Add/Edit Env Var clicked"); // Placeholder for env var logic
        });

        buttonsVBox.getChildren().addAll(
            addStepButton, addAboveButton, addBelowButton, moveUpButton, moveDownButton,
            deleteStepButton, deleteTestCaseButton, saveTestButton, createNewTestButton,
            addEditEnvVarButton
        );

        // Disable buttons based on selection and table state
        table.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = newVal.intValue();
            addAboveButton.setDisable(selectedIndex < 0);
            addBelowButton.setDisable(selectedIndex < 0);
            moveUpButton.setDisable(selectedIndex <= 0);
            moveDownButton.setDisable(selectedIndex < 0 || selectedIndex >= table.getItems().size() - 1);
            deleteStepButton.setDisable(selectedIndex < 0);
        });

        return buttonsVBox;
    }

    private HBox createTextFieldsBox(TableView<String[]> table) {
        TextField urlField = new TextField();
        urlField.setPromptText("URL");
        urlField.setStyle(FIELD_STYLE_UNFOCUSED);
        urlField.setPrefHeight(TEXT_FIELD_HEIGHT); // Constant height
        urlField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            urlField.setStyle(newVal ? FIELD_STYLE_FOCUSED : FIELD_STYLE_UNFOCUSED);
        });

        // Initialize status label
        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #FF5555;");
        statusLabel.setWrapText(true);

        // Bind text field to table selection
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                urlField.setText(newItem[2] != null ? newItem[2] : "");
                statusLabel.setText("");
            } else {
                urlField.clear();
                statusLabel.setText("");
            }
        });

        // Update table when text field changes
        urlField.textProperty().addListener((obs, oldVal, newVal) -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                if (!newVal.matches("^(https?://)?[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}(/.*)?$|^$")) {
                    statusLabel.setText("Invalid URL format");
                    urlField.setText(oldVal);
                    return;
                }
                statusLabel.setText("");
                table.getItems().get(selectedIndex)[2] = newVal;
                table.refresh();
            }
        });

        HBox textFieldsBox = new HBox(10, urlField, statusLabel);
        textFieldsBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 5px;");
        return textFieldsBox;
    }

    private String convertRowToJson(String[] row) {
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < row.length; i++) {
            if (row[i] != null && !row[i].isEmpty() && !COLUMN_NAMES[i].isEmpty()) {
                json.append(String.format("\"%s\": \"%s\"", COLUMN_NAMES[i], row[i].replace("\"", "\\\"")));
                if (i < row.length - 1) json.append(", ");
            }
        }
        json.append("}");
        return json.toString();
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    private class CustomComboBoxTableCell extends ComboBoxTableCell<String[], String> {

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

            // Handle single click to start editing and show dropdown
            setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && !isEditing() && !isEmpty()) {
                    startEdit();
                    // Show the ComboBox dropdown immediately
                    if (getGraphic() instanceof ComboBox) {
                        @SuppressWarnings("unchecked")
                        ComboBox<String> comboBox = (ComboBox<String>) getGraphic();
                        Platform.runLater(() -> {
                            comboBox.show(); // Show dropdown
                            comboBox.requestFocus(); // Ensure ComboBox is focused
                        });
                    }
                }
            });

            // Handle Enter key to start editing and show dropdown
            setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && !isEditing() && !isEmpty()) {
                    startEdit();
                    if (getGraphic() instanceof ComboBox) {
                        @SuppressWarnings("unchecked")
                        ComboBox<String> comboBox = (ComboBox<String>) getGraphic();
                        Platform.runLater(() -> {
                            comboBox.show(); // Show dropdown
                            comboBox.requestFocus(); // Ensure ComboBox is focused
                        });
                    }
                }
            });
        }

        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                // Ensure ComboBox dropdown is shown
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
            // Ensure the cell displays the current value after canceling
            setText(getItem() != null ? getItem() : "");
            setGraphic(null);
        }
    }

    private class CustomTextFieldTableCell extends TextFieldTableCell<String[], String> {

        private final TableView<String[]> table;
        private final int columnIndex;
        private final Label statusLabel;
        private final Set<String> testIds = new HashSet<>();
        private String originalValue; // Store original value for revert on cancel

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

            // Initialize testIds set
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
                        // Try a completely new base if truncation causes duplicates
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

        private boolean isValidTestId(String newValue) {
            if (newValue == null) {
                return true;
            }
            if (!newValue.isEmpty()) {
                int rowIndex = getTableRow().getIndex();
                String currentId = table.getItems().get(rowIndex)[0];
                if (!newValue.equals(currentId) && testIds.contains(newValue)) {
                    String suggestedId = suggestUniqueTestId(newValue);
                    statusLabel.setText("Test ID '" + newValue + "' already exists. Try: " + suggestedId);
                    highlightDuplicateRow(newValue);
                    return false;
                }
            }
            if (newValue.length() > 5) {
                statusLabel.setText("Test ID cannot exceed 5 characters");
                return false;
            }
            if (!newValue.matches("^[0-9#]*$")) {
                statusLabel.setText("Test ID can only contain numbers or #");
                return false;
            }
            if (newValue.startsWith("#") && newValue.matches(".*[0-9].*")) {
                statusLabel.setText("Test ID starting with # cannot contain numbers");
                return false;
            }
            if (newValue.matches("^[0-9].*") && newValue.contains("#")) {
                statusLabel.setText("Test ID starting with a number cannot contain #");
                return false;
            }
            statusLabel.setText("");
            clearDuplicateHighlights();
            return true;
        }

        @Override
        public void startEdit() {
            if (table.getItems().isEmpty() || isEditing()) {
                return; // Prevent re-entering edit mode
            }
            super.startEdit();
            originalValue = getItem(); // Store original value for revert on cancel
            if (getGraphic() instanceof TextField) {
                TextField textField = (TextField) getGraphic();
                // Set fixed height for TextField to prevent row height changes
                textField.setPrefHeight(26);
                textField.setMaxHeight(26);
                textField.setMinHeight(26);
                if (columnIndex == 0) { // Test ID column
                    UnaryOperator<TextFormatter.Change> filter = change -> {
                        String newText = change.getControlNewText();
                        if (newText.length() > 5) {
                            return null; // Reject if longer than 5 characters
                        }
                        if (!newText.matches("^[0-9#]*$")) {
                            return null; // Allow only numbers and #
                        }
                        if (newText.startsWith("#") && newText.matches(".*[0-9].*")) {
                            return null; // Reject numbers if starts with #
                        }
                        if (newText.matches("^[0-9].*") && newText.contains("#")) {
                            return null; // Reject # if starts with a number
                        }
                        // Real-time duplicate check
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
                }
                textField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.TAB) {
                        String text = textField.getText() != null ? textField.getText() : "";
                        if (columnIndex == 0 && !isValidTestId(text)) {
                            int rowIndex = getTableRow().getIndex();
                            // Cast to the correct type to resolve type mismatch
                            @SuppressWarnings("unchecked")
                            TableColumn<String[], String> column = (TableColumn<String[], String>) table.getColumns().get(columnIndex);
                            showError("Cannot commit duplicate/invalid Test ID: " + text);
                            cancelEdit(); // Exit edit mode
                            // Force update to ensure TextField is removed
                            setText(getItem() != null ? getItem() : "");
                            setGraphic(null);
                            table.refresh();
                            Platform.runLater(() -> {
                                // Move focus to the next column
                                int newColumn = (columnIndex + 1) % table.getColumns().size();
                                table.getFocusModel().focus(rowIndex, table.getColumns().get(newColumn));
                                table.getSelectionModel().select(rowIndex); // Ensure row is selected
                                table.edit(rowIndex, table.getColumns().get(newColumn)); // Start editing the next column
                                // If next column is "Request" (ComboBox), ensure dropdown opens
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
                            e.consume(); // Prevent default TAB behavior
                            return;
                        }
                        commitEdit(getConverter().fromString(text));
                        TablePosition<String[], ?> pos = table.getFocusModel().getFocusedCell();
                        int newColumn = (pos.getColumn() + 1) % table.getColumns().size();
                        int newRow = pos.getRow(); // Stay in the same row
                        table.getFocusModel().focus(newRow, table.getColumns().get(newColumn));
                        table.edit(newRow, table.getColumns().get(newColumn));
                        // If next column is "Request" (ComboBox), ensure dropdown opens
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
                        if (columnIndex == 0 && !isValidTestId(text)) {
                            showError("Cannot commit duplicate/invalid Test ID: " + text);
                            return;
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
            if (columnIndex == 0 && !isValidTestId(newValue)) {
                return; // Keep cell in edit mode
            }
            super.commitEdit(newValue); // Commit valid edit
            if (columnIndex == 0) {
                clearDuplicateHighlights();
                updateTestIds();
                // Set default header values if Header (key) and Header (value) are empty
                int rowIndex = getTableRow().getIndex();
                String[] row = table.getItems().get(rowIndex);
                if (row[3] == null || row[3].isEmpty()) {
                    row[3] = "Content-Type";
                }
                if (row[4] == null || row[4].isEmpty()) {
                    row[4] = "application/json";
                }
                table.refresh(); // Update the table to reflect changes
                // Trigger reselection to update additional fields (including header and param scrolls)
                int currentIndex = getTableRow().getIndex();
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().select(currentIndex);
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            // Ensure the cell is visually updated to non-editing state
            setText(getItem() != null ? getItem() : "");
            setGraphic(null);
            if (columnIndex == 0) {
                clearDuplicateHighlights();
                // Revert to original value only if the row item is non-null
                if (getTableRow() != null && getTableRow().getItem() != null && originalValue != null) {
                    getTableRow().getItem()[0] = originalValue;
                    table.refresh();
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}