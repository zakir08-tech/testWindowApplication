package com.test.window.gui.com;

import java.io.File;
import java.util.Optional;

import com.test.window.gui.EnvVarList;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ButtonActions {
    private final TableView<String[]> table;
    private final Stage primaryStage;
    private final FileOperations fileOps;

    public ButtonActions(TableView<String[]> table, Stage primaryStage) {
        this.table = table;
        this.primaryStage = primaryStage;
        this.fileOps = new FileOperations(table, primaryStage);
    }

    public VBox createButtonsVBox() {
        VBox buttonsVBox = new VBox(10);
        buttonsVBox.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 10px;");
        buttonsVBox.setAlignment(Pos.TOP_CENTER);

        Button addStepButton = new Button("Add Step");
        addStepButton.setStyle(Constants.BUTTON_STYLE);
        addStepButton.setTooltip(new Tooltip("Add a new test step to the table"));
        addStepButton.setOnAction(e -> {
            table.getItems().add(new String[Constants.COLUMN_NAMES.length]);
            table.getSelectionModel().select(table.getItems().size() - 1);
            table.refresh();
            fileOps.setModified(true);
        });
        addStepButton.setOnMouseEntered(e -> addStepButton.setStyle(Constants.BUTTON_HOVER_STYLE));
        addStepButton.setOnMouseExited(e -> addStepButton.setStyle(Constants.BUTTON_STYLE));

        Button addAboveButton = new Button("Add Above");
        addAboveButton.setStyle(Constants.BUTTON_STYLE);
        addAboveButton.setTooltip(new Tooltip("Add a new step above the selected row"));
        addAboveButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().add(selectedIndex, new String[Constants.COLUMN_NAMES.length]);
                table.getSelectionModel().select(selectedIndex);
                table.refresh();
                fileOps.setModified(true);
            }
        });
        addAboveButton.setOnMouseEntered(e -> addAboveButton.setStyle(Constants.BUTTON_HOVER_STYLE));
        addAboveButton.setOnMouseExited(e -> addAboveButton.setStyle(Constants.BUTTON_STYLE));

        Button addBelowButton = new Button("Add Below");
        addBelowButton.setStyle(Constants.BUTTON_STYLE);
        addBelowButton.setTooltip(new Tooltip("Add a new step below the selected row"));
        addBelowButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                table.getItems().add(selectedIndex + 1, new String[Constants.COLUMN_NAMES.length]);
                table.getSelectionModel().select(selectedIndex + 1);
                table.refresh();
                fileOps.setModified(true);
            }
        });
        addBelowButton.setOnMouseEntered(e -> addBelowButton.setStyle(Constants.BUTTON_HOVER_STYLE));
        addBelowButton.setOnMouseExited(e -> addBelowButton.setStyle(Constants.BUTTON_STYLE));

        Button moveUpButton = new Button("Move Up");
        moveUpButton.setStyle(Constants.BUTTON_STYLE);
        moveUpButton.setTooltip(new Tooltip("Move the selected step up"));
        moveUpButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex > 0) {
                ObservableList<String[]> items = table.getItems();
                String[] temp = items.get(selectedIndex - 1);
                items.set(selectedIndex - 1, items.get(selectedIndex));
                items.set(selectedIndex, temp);
                table.getSelectionModel().select(selectedIndex - 1);
                table.refresh();
                fileOps.setModified(true);
            }
        });
        moveUpButton.setOnMouseEntered(e -> moveUpButton.setStyle(Constants.BUTTON_HOVER_STYLE));
        moveUpButton.setOnMouseExited(e -> moveUpButton.setStyle(Constants.BUTTON_STYLE));

        Button moveDownButton = new Button("Move Down");
        moveDownButton.setStyle(Constants.BUTTON_STYLE);
        moveDownButton.setTooltip(new Tooltip("Move the selected step down"));
        moveDownButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < table.getItems().size() - 1) {
                ObservableList<String[]> items = table.getItems();
                String[] temp = items.get(selectedIndex + 1);
                items.set(selectedIndex + 1, items.get(selectedIndex));
                items.set(selectedIndex, temp);
                table.getSelectionModel().select(selectedIndex + 1);
                table.refresh();
                fileOps.setModified(true);
            }
        });
        moveDownButton.setOnMouseEntered(e -> moveDownButton.setStyle(Constants.BUTTON_HOVER_STYLE));
        moveDownButton.setOnMouseExited(e -> moveDownButton.setStyle(Constants.BUTTON_STYLE));

        Button deleteStepButton = new Button("Delete Step");
        deleteStepButton.setStyle(Constants.BUTTON_STYLE);
        deleteStepButton.setTooltip(new Tooltip("Delete the selected step"));
        deleteStepButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                String[] selectedRow = table.getItems().get(selectedIndex);
                String testId = selectedRow[0];
                if (testId != null && !testId.isEmpty()) {
                    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmation.setTitle("Confirm Delete");
                    confirmation.setHeaderText("Delete Test Step");
                    confirmation.setContentText("The selected step has a Test ID: " + testId + ". Are you sure you want to delete it?");
                    Optional<ButtonType> result = confirmation.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        table.getItems().remove(selectedIndex);
                        table.refresh();
                        fileOps.setModified(true);
                    }
                } else {
                    table.getItems().remove(selectedIndex);
                    table.refresh();
                    fileOps.setModified(true);
                }
            }
        });
        deleteStepButton.setOnMouseEntered(e -> deleteStepButton.setStyle(Constants.BUTTON_HOVER_STYLE));
        deleteStepButton.setOnMouseExited(e -> deleteStepButton.setStyle(Constants.BUTTON_STYLE));

        Button deleteTestCaseButton = new Button("Delete Test Case");
        deleteTestCaseButton.setStyle(Constants.BUTTON_STYLE);
        deleteTestCaseButton.setTooltip(new Tooltip("Delete all steps for the selected test case"));
        deleteTestCaseButton.setOnAction(e -> {
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                String[] selectedRow = table.getItems().get(selectedIndex);
                String testId = selectedRow[0];
                if (testId == null || testId.isEmpty()) {
                    showError("Please select a row with a valid Test ID to delete the test case.");
                    return;
                }

                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                confirmation.setTitle("Confirm Delete Test Case");
                confirmation.setHeaderText("Delete Test Case: " + testId);
                confirmation.setContentText("Are you sure you want to delete all steps for Test ID: " + testId + "?");
                Optional<ButtonType> result = confirmation.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    int startIndex = selectedIndex;
                    while (startIndex > 0 && (table.getItems().get(startIndex - 1)[0] == null || table.getItems().get(startIndex - 1)[0].isEmpty())) {
                        startIndex--;
                    }
                    int endIndex = startIndex;
                    while (endIndex < table.getItems().size() && (table.getItems().get(endIndex)[0] == null || table.getItems().get(endIndex)[0].isEmpty() || table.getItems().get(endIndex)[0].equals(testId))) {
                        endIndex++;
                    }
                    if (endIndex > startIndex) {
                        table.getItems().subList(startIndex, endIndex).clear();
                    } else {
                        table.getItems().remove(startIndex);
                    }
                    table.refresh();
                    if (!table.getItems().isEmpty()) {
                        int newIndex = Math.min(startIndex, table.getItems().size() - 1);
                        table.getSelectionModel().select(newIndex);
                    } else {
                        table.getSelectionModel().clearSelection();
                    }
                    fileOps.setModified(true);
                }
            }
        });
        deleteTestCaseButton.setOnMouseEntered(e -> deleteTestCaseButton.setStyle(Constants.BUTTON_HOVER_STYLE));
        deleteTestCaseButton.setOnMouseExited(e -> deleteTestCaseButton.setStyle(Constants.BUTTON_STYLE));

        Button saveTestButton = new Button("Save Test");
        saveTestButton.setStyle(Constants.BUTTON_STYLE);
        saveTestButton.setTooltip(new Tooltip("Save the test case to an Excel file"));
        saveTestButton.setOnAction(e -> {
            Alert saveOptionAlert = new Alert(Alert.AlertType.CONFIRMATION);
            saveOptionAlert.setTitle("Save Options");
            saveOptionAlert.setHeaderText("Save Test Case");
            saveOptionAlert.setContentText("Choose an option to save your test case:");
            ButtonType saveButton = new ButtonType("Save");
            ButtonType saveAsButton = new ButtonType("Save As");
            ButtonType cancelButton = new ButtonType("Cancel");
            saveOptionAlert.getButtonTypes().setAll(saveButton, saveAsButton, cancelButton);
            Optional<ButtonType> result = saveOptionAlert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == saveButton) {
                    if (fileOps.getLoadedFile() != null) {
                        fileOps.saveToFile(fileOps.getLoadedFile());
                    } else {
                        fileOps.saveAsToFile();
                    }
                } else if (result.get() == saveAsButton) {
                    fileOps.saveAsToFile();
                }
            }
        });
        saveTestButton.setOnMouseEntered(e -> saveTestButton.setStyle(Constants.BUTTON_HOVER_STYLE));
        saveTestButton.setOnMouseExited(e -> saveTestButton.setStyle(Constants.BUTTON_STYLE));

        Button loadTestButton = new Button("Load Test");
        loadTestButton.setStyle(Constants.BUTTON_STYLE);
        loadTestButton.setTooltip(new Tooltip("Load a test case from an Excel file"));
        loadTestButton.setOnAction(e -> {
            if (!fileOps.checkUnsavedChanges()) {
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Test Case");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Documents"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                fileOps.loadFromFile(file);
            }
        });
        loadTestButton.setOnMouseEntered(e -> loadTestButton.setStyle(Constants.BUTTON_HOVER_STYLE));
        loadTestButton.setOnMouseExited(e -> loadTestButton.setStyle(Constants.BUTTON_STYLE));

        Button createNewTestButton = new Button("Create New Test");
        createNewTestButton.setStyle(Constants.BUTTON_STYLE);
        createNewTestButton.setTooltip(new Tooltip("Start a new test case"));
        createNewTestButton.setOnAction(e -> {
            if (!fileOps.checkUnsavedChanges()) {
                return;
            }
            table.getItems().clear();
            table.refresh();
            fileOps.setModified(false);
            fileOps.setLoadedFile(null);
        });
        createNewTestButton.setOnMouseEntered(e -> createNewTestButton.setStyle(Constants.BUTTON_HOVER_STYLE));
        createNewTestButton.setOnMouseExited(e -> createNewTestButton.setStyle(Constants.BUTTON_STYLE));

        Button addEditEnvVarButton = new Button("Add/Edit Env Var");
        addEditEnvVarButton.setStyle(Constants.BUTTON_STYLE);
        addEditEnvVarButton.setTooltip(new Tooltip("Add or edit environment variables"));
        addEditEnvVarButton.setOnAction(e -> {
            Stage envVarStage = new Stage();
            EnvVarList simpleTableWindow = new EnvVarList();
            try {
                simpleTableWindow.start(envVarStage);
            } catch (Exception ex) {
                showError("Failed to open environment variables window: " + ex.getMessage());
            }
        });
        addEditEnvVarButton.setOnMouseEntered(e -> addEditEnvVarButton.setStyle(Constants.BUTTON_HOVER_STYLE));
        addEditEnvVarButton.setOnMouseExited(e -> addEditEnvVarButton.setStyle(Constants.BUTTON_STYLE));

        buttonsVBox.getChildren().addAll(
            addStepButton, addAboveButton, addBelowButton, moveUpButton, moveDownButton,
            deleteStepButton, deleteTestCaseButton, saveTestButton, loadTestButton,
            createNewTestButton, addEditEnvVarButton
        );

        return buttonsVBox;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }
}