package com.test.window.gui.com;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

public class FileOperations {
    private File loadedFile;
    private boolean isModified;
    private final TableView<String[]> table;
    private final Stage primaryStage;

    public FileOperations(TableView<String[]> table, Stage primaryStage) {
        this.table = table;
        this.primaryStage = primaryStage;
        this.isModified = false;
    }

    public File getLoadedFile() {
        return loadedFile;
    }

    public void setLoadedFile(File file) {
        this.loadedFile = file;
    }

    public boolean isModified() {
        return isModified;
    }

    public void setModified(boolean modified) {
        this.isModified = modified;
    }

    public boolean checkUnsavedChanges() {
        if (isModified) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes");
            alert.setContentText("Do you want to save your changes before proceeding?");
            ButtonType saveButton = new ButtonType("Save");
            ButtonType saveAsButton = new ButtonType("Save As");
            ButtonType discardButton = new ButtonType("Discard");
            ButtonType cancelButton = new ButtonType("Cancel");
            alert.getButtonTypes().setAll(saveButton, saveAsButton, discardButton, cancelButton);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == saveButton) {
                    if (loadedFile != null) {
                        return saveToFile(loadedFile);
                    } else {
                        return saveAsToFile();
                    }
                } else if (result.get() == saveAsButton) {
                    return saveAsToFile();
                } else if (result.get() == discardButton) {
                    isModified = false;
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
        return true;
    }

    public boolean saveToFile(File file) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Test Data");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < Constants.COLUMN_NAMES.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(Constants.COLUMN_NAMES[i]);
            }
            for (int i = 0; i < table.getItems().size(); i++) {
                Row row = sheet.createRow(i + 1);
                String[] data = table.getItems().get(i);
                for (int j = 0; j < data.length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(data[j] != null ? data[j] : "");
                }
            }
            for (int i = 0; i < Constants.COLUMN_NAMES.length; i++) {
                sheet.autoSizeColumn(i);
            }
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
            isModified = false;
            loadedFile = file;
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("File Saved");
            alert.setContentText("Test case saved successfully to " + file.getAbsolutePath());
            alert.showAndWait();
            return true;
        } catch (IOException ex) {
            showError("Failed to save file: " + ex.getMessage());
            return false;
        }
    }

    public boolean saveAsToFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Test Case");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/Documents"));
        fileChooser.setInitialFileName("TestCase.xlsx");
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            return saveToFile(file);
        }
        return false;
    }

    public void loadFromFile(File file) {
        try (FileInputStream fileIn = new FileInputStream(file);
             XSSFWorkbook workbook = new XSSFWorkbook(fileIn)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                showError("Sheet not found in the Excel file.");
                return;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                showError("Header row not found in the Excel file.");
                return;
            }

            // Safe header validation using getCellValueAsString
            boolean headersValid = headerRow.getPhysicalNumberOfCells() >= Constants.COLUMN_NAMES.length;
            if (headersValid) {
                for (int i = 0; i < Constants.COLUMN_NAMES.length; i++) {
                    Cell cell = headerRow.getCell(i);
                    String headerValue = getCellValueAsString(cell);
                    if (!Constants.COLUMN_NAMES[i].equals(headerValue)) {
                        headersValid = false;
                        break;
                    }
                }
            }
            if (!headersValid) {
                String fileName = file.getName().replaceFirst("[.][^.]+$", "");
                showError("Invalid test suite " + fileName + ". Headers do not match expected format. Expected: " + String.join(", ", Constants.COLUMN_NAMES));
                return;
            }

            // Clear and load data rows
            table.getItems().clear();
            int lastRowNum = sheet.getLastRowNum();
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row != null && !isRowEmpty(row)) {  // Skip completely empty rows
                    String[] data = new String[Constants.COLUMN_NAMES.length];
                    int cellsFound = row.getPhysicalNumberOfCells();
                    for (int j = 0; j < Constants.COLUMN_NAMES.length; j++) {
                        Cell cell = (j < cellsFound) ? row.getCell(j) : null;
                        data[j] = getCellValueAsString(cell);  // Safe cell reading
                    }
                    table.getItems().add(data);
                }
            }
            table.refresh();
            if (!table.getItems().isEmpty()) {
                table.getSelectionModel().select(0);
            }
            isModified = false;
            loadedFile = file;
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("File Loaded");
            alert.setContentText("Test case loaded successfully from " + file.getAbsolutePath() + ". Loaded " + table.getItems().size() + " rows.");
            alert.showAndWait();
        } catch (IOException ex) {
            System.err.println("Detailed load error: " + ex.getMessage());  // Log for debugging
            ex.printStackTrace();  // Full stack trace to console
            String fileName = file.getName().replaceFirst("[.][^.]+$", "");
            showError("Failed to load test suite " + fileName + ": " + ex.getMessage() + ". Please check the file format and try again.");
        }
    }

    /**
     * Safely extracts a string value from any cell type using DataFormatter.
     * Handles numeric, string, boolean, date, formula, and blank cells.
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        DataFormatter formatter = new DataFormatter(Locale.US);  // Use US locale for consistent number/date formatting
        return formatter.formatCellValue(cell);
    }

    /**
     * Checks if a row is completely empty (all cells null or blank).
     */
    private boolean isRowEmpty(Row row) {
        int cells = row.getPhysicalNumberOfCells();
        for (int j = 0; j < cells; j++) {
            Cell cell = row.getCell(j);
            if (cell != null && !getCellValueAsString(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }
}