package com.test.window.gui.com;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Constants {
    public static final String[] COLUMN_NAMES = {
        "Test ID", "Request", "End-Point", "Header (key)", "Header (value)",
        "Parameter (key)", "Parameter (value)", "Payload", "Payload Type",
        "Modify Payload (key)", "Modify Payload (value)", "Response (key) Name",
        "Capture (key) Value (env var)", "Authorization", "", "",
        "SSL Validation", "Expected Status", "Verify Response", "Test Description"
    };

    public static final ObservableList<String> REQUEST_OPTIONS = 
        FXCollections.observableArrayList("", "GET", "POST", "PUT", "PATCH", "DELETE");

    public static final ObservableList<String> PAYLOAD_TYPE_OPTIONS = 
        FXCollections.observableArrayList("", "none", "form-data", "x-www-form-urlencoded", "JSON");

    public static final ObservableList<String> AUTH_OPTIONS = 
        FXCollections.observableArrayList("", "Basic Auth", "Bearer Token");

    public static final String FIELD_STYLE_UNFOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #3C3F41; -fx-border-width: 1px; -fx-prompt-text-fill: #888888; -fx-border-radius: 5px;";

    public static final String FIELD_STYLE_FOCUSED = 
        "-fx-background-color: #2E2E2E; -fx-control-inner-background: #2E2E2E; -fx-text-fill: white; " +
        "-fx-border-color: #4A90E2; -fx-border-width: 2px; -fx-prompt-text-fill: #888888; -fx-border-radius: 5px;";

    public static final String BUTTON_STYLE = 
        "-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

    public static final String BUTTON_HOVER_STYLE = 
        "-fx-background-color: #6AB0FF; -fx-text-fill: white; -fx-border-radius: 5px; -fx-min-width: 100px;";

    public static final String CSS = """
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
        .text-area {
            -fx-font-family: 'Consolas', 'Menlo', 'Courier New', monospace;
            -fx-font-size: 12px;
            -fx-padding: 5px;
        }
        .text-field:disabled {
            -fx-background-color: #3C3F41;
            -fx-text-fill: #888888;
            -fx-opacity: 1.0;
        }
        .combo-box-base .list-cell {
            -fx-background-color: #2E2E2E;
            -fx-text-fill: white;
        }
        .combo-box-base .combo-box-button {
            -fx-background-color: #2E2E2E;
            -fx-text-fill: white;
        }
        .combo-box-base .combo-box-popup .list-view .list-cell {
            -fx-background-color: #2E2E2E;
            -fx-text-fill: white;
        }
        .combo-box-base .combo-box-popup .list-view .list-cell:hover {
            -fx-background-color: #4A90E2;
            -fx-text-fill: white;
        }
        .combo-box-base .combo-box-popup .list-view .list-cell:selected {
            -fx-background-color: #4A90E2;
            -fx-text-fill: white;
        }
        """;

    public static final double TEXT_FIELD_HEIGHT = 30.0;
}