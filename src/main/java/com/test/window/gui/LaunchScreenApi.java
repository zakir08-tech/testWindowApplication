package com.test.window.gui;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.InnerShadow;
import java.util.ArrayList;
import java.util.List;

import com.test.window.app.UIConstants;

/**
 * LaunchScreenApi is the main entry point for the Phantom API Test Accelerator application.
 * It creates a launch window with a background image, title, subtitle, and buttons to open
 * the Create/Edit Test and Run Test windows. The window is non-resizable and sized based
 * on screen dimensions. It handles cleanup of child windows on close.
 */
public class LaunchScreenApi extends Application {

    // List to track all open stages for proper cleanup
    private final List<Stage> openStages = new ArrayList<>();
    // Track specific stages to prevent duplicate windows
    private Stage testCreateEditApiTest = null;
    private Stage testRunApiTest = null;
    private Stage envVarListStage = null;

    /**
     * Entry point for the JavaFX application. Initializes the primary stage with the launch screen UI.
     * Calculates window size, loads background image, sets up effects and animations, and configures buttons.
     * 
     * @param primaryStage The primary stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Calculate window size as 40% of screen width and 50% of screen height
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double screenWidth = screenBounds.getWidth();
            double screenHeight = screenBounds.getHeight();
            double windowWidth = screenWidth * 0.4;
            double windowHeight = screenHeight * 0.5;

            // Load the background image from file path
            Image backgroundImage;
            try {
                backgroundImage = new Image("file:" + UIConstants.UI_ICON);
                if (backgroundImage.isError()) {
                    throw new IllegalStateException("Failed to load background image from file: " + backgroundImage.getException().getMessage());
                }
                System.out.println("Background image loaded successfully from file: " + UIConstants.UI_ICON + 
                                   ", Width: " + backgroundImage.getWidth() + ", Height: " + backgroundImage.getHeight());
            } catch (Exception e) {
                System.err.println("Error loading background image from file '" + UIConstants.UI_ICON + "': " + e.getMessage());
                // Fallback to a default image (1x1 pixel placeholder to avoid null issues)
                backgroundImage = new Image("file:default_icon.png");
                System.out.println("Using default image. Width: " + backgroundImage.getWidth() + ", Height: " + backgroundImage.getHeight());
            }

            // Create and configure ImageView for the background image
            ImageView imageView = new ImageView(backgroundImage);
            double imageWidth = windowWidth / 2;
            imageView.setFitWidth(imageWidth);
            imageView.setFitHeight(windowHeight);
            imageView.setPreserveRatio(false); // Stretch to fit the left half
            imageView.setX(0);
            imageView.setY(0);
            // Add a visible border for debugging
            imageView.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            // Log ImageView properties for debugging
            System.out.println("ImageView configured: X=" + imageView.getX() + ", Y=" + imageView.getY() + 
                               ", FitWidth=" + imageView.getFitWidth() + ", FitHeight=" + imageView.getFitHeight());

            // Add a drop shadow effect to the image
            DropShadow dropShadow = new DropShadow();
            dropShadow.setRadius(10.0);
            dropShadow.setOffsetX(5.0);
            dropShadow.setOffsetY(5.0);
            dropShadow.setColor(Color.color(0, 0, 0, 0.5)); // Semi-transparent black shadow
            imageView.setEffect(dropShadow);

            // Initialize the root pane with a dark theme background
            Pane root = new Pane();
            root.setStyle("-fx-background-color: #1a1a1a;");

            // Add the image to the pane
            root.getChildren().add(imageView);

            // Create and style the title text
            Text title = new Text("Phantom");
            title.setFont(Font.font("Impact", FontWeight.BOLD, 50));
            title.setFill(Color.web("#4A90E2"));

            // Add glow and inner shadow effects to the title text
            Glow glow = new Glow(0.6); // Moderate glow intensity
            InnerShadow innerShadow = new InnerShadow();
            innerShadow.setRadius(2.0);
            innerShadow.setOffsetX(2.0);
            innerShadow.setOffsetY(2.0);
            innerShadow.setColor(Color.color(0, 0, 0, 0.4)); // Subtle dark shadow
            title.setEffect(glow); // Apply glow first
            glow.setInput(innerShadow); // Combine with inner shadow

            // Create and style the subtitle text, matching the title's width
            Text subtitle = new Text("API Test Accelerator");
            subtitle.setFont(Font.font("Arial", FontWeight.BOLD, 32));
            subtitle.setFill(Color.LIGHTGRAY);
            subtitle.setWrappingWidth(title.getLayoutBounds().getWidth());

            // Stack title and subtitle vertically with spacing
            VBox textBox = new VBox(10, title, subtitle);
            textBox.setLayoutX(imageWidth + 20); // Padding from image
            textBox.setLayoutY(20); // Padding from top

            // Create and style the "Create/Edit Test" button
            Button createEditButton = new Button("Create/Edit Test");
            createEditButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            createEditButton.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5; -fx-min-width: 100px;");

            // Create and style the "Run Test" button
            Button runTestButton = new Button("Run Test");
            runTestButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            runTestButton.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5; -fx-min-width: 100px;");

            // Create and style the "Environment Variables" button
            Button envButton = new Button("Environment Variables");
            envButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            envButton.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5; -fx-min-width: 100px;");

            // Add hover effects for createEditButton
            ScaleTransition createButtonHover = new ScaleTransition(Duration.millis(200), createEditButton);
            createButtonHover.setToX(1.05); // Scale up by 5%
            createButtonHover.setToY(1.05);
            createEditButton.setOnMouseEntered(e -> {
                createEditButton.setStyle("-fx-background-color: #6AB0FF; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5; -fx-min-width: 100px;");
                createButtonHover.playFromStart();
            });
            createEditButton.setOnMouseExited(e -> {
                createEditButton.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5; -fx-min-width: 100px;");
                createButtonHover.setToX(1.0);
                createButtonHover.setToY(1.0);
                createButtonHover.playFromStart();
            });

            // Add hover effects for runTestButton
            ScaleTransition runButtonHover = new ScaleTransition(Duration.millis(200), runTestButton);
            runButtonHover.setToX(1.05); // Scale up by 5%
            runButtonHover.setToY(1.05);
            runTestButton.setOnMouseEntered(e -> {
                runTestButton.setStyle("-fx-background-color: #6AB0FF; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5; -fx-min-width: 100px;");
                runButtonHover.playFromStart();
            });
            runTestButton.setOnMouseExited(e -> {
                runTestButton.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5; -fx-min-width: 100px;");
                runButtonHover.setToX(1.0);
                runButtonHover.setToY(1.0);
                runButtonHover.playFromStart();
            });

            // Add hover effects for envButton
            ScaleTransition envButtonHover = new ScaleTransition(Duration.millis(200), envButton);
            envButtonHover.setToX(1.05); // Scale up by 5%
            envButtonHover.setToY(1.05);
            envButton.setOnMouseEntered(e -> {
                envButton.setStyle("-fx-background-color: #6AB0FF; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5; -fx-min-width: 100px;");
                envButtonHover.playFromStart();
            });
            envButton.setOnMouseExited(e -> {
                envButton.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5; -fx-min-width: 100px;");
                envButtonHover.setToX(1.0);
                envButtonHover.setToY(1.0);
                envButtonHover.playFromStart();
            });

            // Handle "Create/Edit Test" button action
            createEditButton.setOnAction(e -> {
                try {
                    if (testCreateEditApiTest == null || !testCreateEditApiTest.isShowing()) {
                        // Create a new stage if none exists or it's not showing
                    	testCreateEditApiTest = new Stage();
                        CreateEditAPITest testManagerApp = new CreateEditAPITest();
                        testManagerApp.start(testCreateEditApiTest);
                        openStages.add(testCreateEditApiTest);
                    } else {
                    	testCreateEditApiTest.requestFocus(); // Bring existing window to focus
                    }
                } catch (Exception ex) {
                    System.err.println("Error opening Test Manager: " + ex.getMessage());
                }
            });

            // Handle "Run Test" button action
            runTestButton.setOnAction(e -> {
                try {
                    if (testRunApiTest == null || !testRunApiTest.isShowing()) {
                        // Create a new stage if none exists or it's not showing
                    	testRunApiTest = new Stage();
                        RunApiTest testRunner = new RunApiTest();
                        testRunner.start(testRunApiTest);
                        openStages.add(testRunApiTest);
                    } else {
                    	testRunApiTest.requestFocus(); // Bring existing window to focus
                    }
                } catch (Exception ex) {
                    System.err.println("Error opening Test Runner: " + ex.getMessage());
                }
            });

            // Handle "Environment Variables" button action
            envButton.setOnAction(e -> {
                try {
                    if (envVarListStage == null || !envVarListStage.isShowing()) {
                        // Create a new stage if none exists or it's not showing
                        envVarListStage = new Stage();
                        EnvVarList envApp = new EnvVarList();
                        envApp.start(envVarListStage);
                        openStages.add(envVarListStage);
                    } else {
                        envVarListStage.requestFocus(); // Bring existing window to focus
                    }
                } catch (Exception ex) {
                    System.err.println("Error opening Environment Variables: " + ex.getMessage());
                }
            });

            // Stack buttons vertically with spacing
            VBox buttonBox = new VBox(10, createEditButton, runTestButton, envButton);
            buttonBox.setLayoutX(imageWidth + 20); // Padding from image
            buttonBox.setLayoutY(windowHeight - 100); // Position 100 pixels from bottom

            // Add text and buttons to the pane
            root.getChildren().addAll(textBox, buttonBox);

            // Create the scene with the calculated size
            Scene scene = new Scene(root, windowWidth, windowHeight);
            primaryStage.setResizable(false); // Prevent window resizing

            // Set the window icon
            try {
                primaryStage.getIcons().add(backgroundImage);
            } catch (Exception e) {
                System.err.println("Error setting window icon: " + e.getMessage());
            }

            // Handle window close request to clean up all open stages
            primaryStage.setOnCloseRequest(event -> {
                try {
                    // Close any open EnvVarList from UIComponentsManager
                    UIComponentsManager.closeEnvVarStage();
                    
                    for (Stage stage : new ArrayList<>(openStages)) {
                        if (stage != null && stage.isShowing()) {
                            stage.close();
                        }
                    }
                    openStages.clear();
                } catch (Exception ex) {
                    System.err.println("Error closing stages: " + ex.getMessage());
                }
            });

            // Configure and show the primary stage
            primaryStage.setTitle("Phantom: API Test Accelertaor");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error initializing LaunchScreen: " + e.getMessage());
        }
    }

    /**
     * Main method to launch the JavaFX application.
     * 
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        // Launch the JavaFX application
        launch(args);
    }
}