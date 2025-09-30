package com.test.window.gui.com;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CreateEditAPITest extends Application {

	@Override
	public void start(Stage primaryStage) {
	    TableConfig tableConfig = new TableConfig();
	    TableViewManager tableManager = new TableViewManager(tableConfig);
	    ButtonActions buttonActions = new ButtonActions(tableManager.getTable(), primaryStage);
	    FileOperations fileOps = new FileOperations(tableManager.getTable(), primaryStage);
	    UIBuilder uiBuilder = new UIBuilder(tableManager, buttonActions, fileOps, primaryStage);

	    VBox mainLayout = uiBuilder.createMainLayout();
	    Scene scene = new Scene(mainLayout);
	    scene.getStylesheets().add("data:text/css," + Constants.CSS.replaceAll("\n", "%0A")); // Ensure this line is present
	    primaryStage.setScene(scene);
	    primaryStage.setMinWidth(800);
	    primaryStage.setMinHeight(600);
	    primaryStage.setMaximized(true);
	    primaryStage.setTitle("Table with JSON Viewer");
	    primaryStage.show();

	    // Fallback CSS loading
	    scene.getStylesheets().addListener((javafx.collections.ListChangeListener<String>) change -> {
	        if (scene.getStylesheets().isEmpty()) {
	            System.err.println("CSS loading failed, falling back to default style.");
	            mainLayout.setStyle("-fx-background-color: #2E2E2E;");
	        }
	    });
	}

    public static void main(String[] args) {
        launch(args);
    }
}