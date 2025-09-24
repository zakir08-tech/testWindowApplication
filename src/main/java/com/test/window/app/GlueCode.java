package com.test.window.app;

import java.util.NoSuchElementException;
import java.io.File;
import java.io.IOException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import io.appium.java_client.windows.WindowsDriver;
import com.test.window.setup.LaunchApplication;

/**
 * Utility class for launching and interacting with a Windows UWP application using Appium's WindowsDriver.
 * Provides methods to invoke the application, interact with UI elements, manage the driver session,
 * and terminate the application process.
 * This class is designed as a singleton utility and should not be instantiated.
 */
public final class GlueCode {

    // Static WindowsDriver instance for interacting with the application
    private static WindowsDriver driver = null;

    // Constants for error messages
    private static final String NULL_OR_EMPTY_APP_FAMILY_NAME = "Application family name must not be null or empty.";
    private static final String NULL_OR_EMPTY_WINDOW_TITLE = "Window title must not be null or empty.";
    private static final String NULL_OR_EMPTY_AUTOMATION_ID = "Automation ID must not be null or empty.";
    private static final String NULL_OR_EMPTY_NAME = "Element Name must not be null or empty.";
    private static final String NULL_OR_EMPTY_XPATH = "XPath must not be null or empty.";
    private static final String NULL_OR_EMPTY_VALUE = "Value to set must not be null or empty.";
    private static final String INVALID_FILE_PATH = "File path is invalid or not writable: ";
    private static final String INVALID_ELEMENT_IDS = "Automation ID, Name, and XPath cannot all be null or empty.";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private GlueCode() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /**
     * Launches a Windows UWP application and attaches a WindowsDriver to it.
     *
     * @param appFamilyName The family name of the UWP app (e.g., "Microsoft.LinkedIn_8wekyb3d8bbwe").
     * @param windowTitle   The case-sensitive title of the application window to locate.
     * @throws IllegalArgumentException If appFamilyName or windowTitle is null or empty.
     * @throws RuntimeException         If launching the application or attaching the driver fails.
     */
    public static void invokeTheApplication(String appFamilyName, String windowTitle) {
        validateInputParameters(appFamilyName, windowTitle);

        try {
            // Initialize new driver session without closing existing one
            LaunchApplication.launchApp(appFamilyName);
            LaunchApplication.findHWNDByWindowTitle(windowTitle);
            LaunchApplication.hwndToHex();
            LaunchApplication.setDesiredCapabilities();
            LaunchApplication.attachWindowDriverWithApplication();

            driver = LaunchApplication.getDriver();
            if (driver == null) {
                throw new IllegalStateException("WindowsDriver instance is null after attachment.");
            }
            System.out.println("DEBUG: Driver initialized for appFamilyName: " + appFamilyName + ", windowTitle: " + windowTitle);
            System.out.println("Successfully invoked application and attached driver for: " + appFamilyName);
        } catch (IllegalStateException e) {
            String errorMsg = String.format("Failed to initialize application '%s' or attach driver: %s",
                appFamilyName, e.getMessage());
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error while invoking application '%s': %s",
                appFamilyName, e.getMessage());
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Performs a click action on an element identified by either Automation ID, Name, or XPath.
     *
     * @param automationId The Automation ID of the element to locate, if available.
     * @param name         The Name of the element to locate, used if automationId is null or empty.
     * @param xpath        The XPath of the element to locate, used if both automationId and name are null or empty.
     * @throws IllegalArgumentException If automationId, name, and xpath are all null or empty.
     * @throws NoSuchElementException   If the element cannot be found.
     * @throws RuntimeException         If interaction with the element fails.
     */
    public static void clickElement(String automationId, String name, String xpath) {
        validateElementIdentifiers(automationId, name, xpath);
        try {
            WebElement element = findElement(automationId, name, xpath);
            element.click();
            System.out.println("Successfully clicked element (AutomationID: '" + automationId + "', Name: '" + name + "', XPath: '" + xpath + "').");
        } catch (NoSuchElementException e) {
            throw e; // Re-throw to allow caller to handle element not found
        } catch (Exception e) {
            String errorMsg = String.format("Failed to click element (AutomationID: '%s', Name: '%s', XPath: '%s'): %s",
                automationId, name, xpath, e.getMessage());
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Sets a value to an element identified by either Automation ID, Name, or XPath.
     *
     * @param automationId The Automation ID of the element to locate, if available.
     * @param name         The Name of the element to locate, used if automationId is null or empty.
     * @param xpath        The XPath of the element to locate, used if both automationId and name are null or empty.
     * @param value        The value to set in the element.
     * @throws IllegalArgumentException If automationId, name, and xpath are all null or empty, or if value is null or empty.
     * @throws NoSuchElementException   If the element cannot be found.
     * @throws RuntimeException         If setting the value fails.
     */
    public static void setValueToElement(String automationId, String name, String xpath, String value) {
        validateElementIdentifiers(automationId, name, xpath);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_VALUE);
        }

        try {
            WebElement element = findElement(automationId, name, xpath);
            element.sendKeys(value);
            System.out.println("Successfully set value '" + value + "' to element (AutomationID: '" + automationId + "', Name: '" + name + "', XPath: '" + xpath + "').");
        } catch (NoSuchElementException e) {
            throw e; // Re-throw to allow caller to handle element not found
        } catch (Exception e) {
            String errorMsg = String.format("Failed to set value to element (AutomationID: '%s', Name: '%s', XPath: '%s'): %s",
                automationId, name, xpath, e.getMessage());
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Captures a screenshot of the active Windows application and saves it to the specified file path.
     *
     * @param filePath The full path where the screenshot will be saved (e.g., "C:\\Screenshots\\screenshot.png").
     * @throws IllegalArgumentException If the file path is null, empty, or not writable.
     * @throws IllegalStateException    If the driver is not initialized.
     * @throws RuntimeException         If capturing or saving the screenshot fails, wrapping any IOException.
     */
    public static void takeScreenshot(String filePath) {
        validateFilePath(filePath);
        if (driver == null) {
            throw new IllegalStateException("WindowsDriver is not initialized. Call invokeTheApplication first.");
        }

        try {
            // Capture screenshot from the driver
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            if (screenshotFile == null || !screenshotFile.exists()) {
                throw new IOException("Failed to capture screenshot: Temporary file is null or does not exist.");
            }
            System.out.println("DEBUG: Temporary screenshot file created at: " + screenshotFile.getAbsolutePath());

            // Create destination file
            File destinationFile = new File(filePath);

            // Ensure parent directories exist
            File parentDir = destinationFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create parent directories for: " + filePath);
                }
                System.out.println("DEBUG: Created parent directories: " + parentDir.getAbsolutePath());
            }

            // Verify destination directory is writable
            if (!parentDir.canWrite()) {
                throw new IOException("Destination directory is not writable: " + parentDir.getAbsolutePath());
            }

            // Copy screenshot to the specified location
            FileUtils.copyFile(screenshotFile, destinationFile);
            if (!destinationFile.exists()) {
                throw new IOException("Screenshot file was not saved successfully: " + filePath);
            }
            System.out.println("Screenshot saved successfully to: " + filePath);
        } catch (IOException e) {
            String errorMsg = String.format("Failed to save screenshot to '%s': %s", filePath, e.getMessage());
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error while capturing screenshot: %s", e.getMessage());
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Captures a screenshot of the active Windows application, draws a red box around the specified element,
     * and saves it to the specified file path.
     *
     * @param filePath     The full path where the screenshot will be saved (e.g., "C:\\Screenshots\\screenshot.png").
     * @param automationId The Automation ID of the element to locate and highlight, if available.
     * @param name         The Name of the element to locate and highlight, used if automationId is null or empty.
     * @param xpath        The XPath of the element to locate and highlight, used if both automationId and name are null or empty.
     * @throws IllegalArgumentException If automationId, name, and xpath are all null or empty, or filePath is invalid.
     * @throws IllegalStateException    If the driver is not initialized.
     * @throws NoSuchElementException   If the element cannot be found.
     * @throws RuntimeException         If capturing, processing, or saving the screenshot fails, wrapping any IOException.
     */
    public static void takeScreenshotWithElementHighlight(String filePath, String automationId, String name, String xpath) {
        validateFilePath(filePath);
        validateElementIdentifiers(automationId, name, xpath);
        if (driver == null) {
            throw new IllegalStateException("WindowsDriver is not initialized. Call invokeTheApplication first.");
        }

        try {
            // Find the element to highlight
            WebElement element = findElement(automationId, name, xpath);

            // Check if element is displayed
            if (!element.isDisplayed()) {
                throw new NoSuchElementException("Element with Name '" + name + "' or XPath '" + xpath + "' is not visible.");
            }

            // Get the element's location and size
            Point location = element.getLocation();
            Dimension size = element.getSize();
            int width = size.getWidth();
            int height = size.getHeight();

            // Debug the coordinates and size
            System.out.println("DEBUG: Element location: x=" + location.getX() + ", y=" + location.getY());
            System.out.println("DEBUG: Element size: width=" + width + ", height=" + height);

            // Validate dimensions
            if (width <= 0 || height <= 0) {
                throw new RuntimeException("Element has invalid dimensions: width=" + width + ", height=" + height);
            }

            // Capture screenshot from the driver
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            if (screenshotFile == null || !screenshotFile.exists()) {
                throw new IOException("Failed to capture screenshot: Temporary file is null or does not exist.");
            }
            System.out.println("DEBUG: Temporary screenshot file created at: " + screenshotFile.getAbsolutePath());

            // Read the screenshot into a BufferedImage
            BufferedImage originalImage = ImageIO.read(screenshotFile);
            if (originalImage == null) {
                throw new IOException("Failed to read screenshot image from: " + screenshotFile.getAbsolutePath());
            }

            // Create a copy of the image to draw on
            BufferedImage imageWithHighlight = new BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = imageWithHighlight.createGraphics();
            try {
                g2d.drawImage(originalImage, 0, 0, null);

                // Draw red rectangle around the element
                g2d.setColor(Color.RED);
                int thickness = 3; // Thickness of the red border
                for (int i = 0; i < thickness; i++) {
                    // Ensure rectangle stays within image bounds
                    int x = Math.max(0, location.getX() + i);
                    int y = Math.max(0, location.getY() + i);
                    int w = Math.min(width - 2 * i, originalImage.getWidth() - x);
                    int h = Math.min(height - 2 * i, originalImage.getHeight() - y);
                    if (w > 0 && h > 0) {
                        g2d.drawRect(x, y, w, h);
                    }
                }
            } finally {
                g2d.dispose(); // Ensure graphics context is disposed
            }

            // Create destination file
            File destinationFile = new File(filePath);

            // Ensure parent directories exist
            File parentDir = destinationFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create parent directories for: " + filePath);
                }
                System.out.println("DEBUG: Created parent directories: " + parentDir.getAbsolutePath());
            }

            // Verify destination directory is writable
            if (!parentDir.canWrite()) {
                throw new IOException("Destination directory is not writable: " + parentDir.getAbsolutePath());
            }

            // Save the modified image
            boolean writeSuccess = ImageIO.write(imageWithHighlight, "png", destinationFile);
            if (!writeSuccess || !destinationFile.exists()) {
                throw new IOException("Failed to save highlighted screenshot to: " + filePath);
            }
            System.out.println("Screenshot with red box around element saved successfully to: " + filePath);
        } catch (NoSuchElementException e) {
            throw e; // Re-throw to allow caller to handle element not found
        } catch (IOException e) {
            String errorMsg = String.format("Failed to process or save screenshot to '%s': %s", filePath, e.getMessage());
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error while capturing and highlighting screenshot: %s", e.getMessage());
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Closes the active WindowsDriver session and the application process.
     * Attempts to close the window via WinAppDriver, then terminates the process and driver session.
     *
     * @param appFamilyName The family name of the UWP app to close (e.g., "Microsoft.LinkedIn_8wekyb3d8bbwe").
     * @throws IllegalArgumentException If appFamilyName is null or empty.
     * @throws RuntimeException If closing the driver or process fails.
     */
    public static void closeApplication(String appFamilyName) {
        if (appFamilyName == null || appFamilyName.trim().isEmpty()) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_APP_FAMILY_NAME);
        }

        try {
            // Attempt to close the window via WinAppDriver
            if (driver != null) {
                try {
                    driver.close();
                    System.out.println("DEBUG: Window closed via WinAppDriver for appFamilyName: " + appFamilyName);
                } catch (Exception e) {
                    System.err.println("WARNING: Failed to close window via WinAppDriver for appFamilyName: " + appFamilyName + ": " + e.getMessage());
                }
            } else {
                System.out.println("DEBUG: No active WinAppDriver session to close window for appFamilyName: " + appFamilyName);
            }

            // Close the application process
            closeApplicationByProcess(appFamilyName);

            // Close the driver session
            if (driver != null) {
                try {
                    LaunchApplication.closeDriver();
                    System.out.println("DEBUG: Application driver closed successfully for appFamilyName: " + appFamilyName);
                } catch (Exception e) {
                    System.err.println("WARNING: Failed to close driver session for appFamilyName: " + appFamilyName + ": " + e.getMessage());
                }
            } else {
                System.out.println("DEBUG: No active driver session to close for appFamilyName: " + appFamilyName);
            }
        } catch (Exception e) {
            String errorMsg = String.format("Failed to close application '%s' or driver: %s", appFamilyName, e.getMessage());
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg, e);
        } finally {
            driver = null; // Ensure driver is nullified to prevent reuse
        }
    }

    /**
     * Terminates the application process using the taskkill command.
     *
     * @param appFamilyName The family name of the UWP app to close (e.g., "Microsoft.LinkedIn_8wekyb3d8bbwe").
     * @throws IllegalArgumentException If appFamilyName is null or empty.
     * @throws RuntimeException If the process termination fails.
     */
    public static void closeApplicationByProcess(String appFamilyName) {
        if (appFamilyName == null || appFamilyName.trim().isEmpty()) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_APP_FAMILY_NAME);
        }

        try {
            // Derive the executable name from the appFamilyName
            String processName = deriveProcessName(appFamilyName);
            System.out.println("DEBUG: Attempting to terminate process: " + processName + " for appFamilyName: " + appFamilyName);

            // Execute taskkill command to terminate the process
            ProcessBuilder processBuilder = new ProcessBuilder("taskkill", "/IM", processName, "/F");
            Process process = processBuilder.start();
            
            // Wait for the taskkill command to complete and check exit code
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("DEBUG: Successfully terminated process: " + processName + " for appFamilyName: " + appFamilyName);
            } else if (exitCode == 128) {
                System.out.println("DEBUG: No process found for application: " + appFamilyName);
                // Skip fallback to avoid closing unrelated windows
            } else {
                System.err.println("WARNING: taskkill command failed with exit code: " + exitCode + " for process: " + processName);
                // Avoid fallback to ApplicationFrameHost.exe to prevent closing other windows
                throw new IOException("taskkill command failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            String errorMsg = String.format("Failed to terminate process for application '%s': %s", 
                appFamilyName, e.getMessage());
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Derives the process name (executable name) from the application family name.
     * This is a simplified approach; actual process names may vary depending on the UWP app.
     *
     * @param appFamilyName The family name of the UWP app.
     * @return The derived process name (e.g., "Calculator.exe").
     */
    private static String deriveProcessName(String appFamilyName) {
        // Map known appFamilyNames to process names
        if (appFamilyName.contains("Microsoft.WindowsCalculator")) {
            return "Calculator.exe";
        } else if (appFamilyName.contains("Microsoft.WindowsNotepad")) {
            return "Notepad.exe";
        }
        // Fallback: extract app name before the underscore
        String[] parts = appFamilyName.split("_");
        String appName = parts[0].contains(".") ? parts[0].substring(parts[0].lastIndexOf(".") + 1) : parts[0];
        return appName + ".exe";
    }

    /**
     * Retrieves the current WindowsDriver instance.
     *
     * @return The active WindowsDriver instance, or null if not initialized.
     */
    public static WindowsDriver getDriver() {
        return driver;
    }

    /**
     * Validates that the input parameters are neither null nor empty.
     *
     * @param appFamilyName The application family name to validate.
     * @param windowTitle   The application window title to validate.
     * @throws IllegalArgumentException If any parameter is null or empty.
     */
    private static void validateInputParameters(String appFamilyName, String windowTitle) {
        if (appFamilyName == null || appFamilyName.trim().isEmpty()) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_APP_FAMILY_NAME);
        }
        if (windowTitle == null || windowTitle.trim().isEmpty()) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_WINDOW_TITLE);
        }
    }

    /**
     * Validates that at least one of the element identifiers (Automation ID, Name, or XPath) is non-null and non-empty.
     *
     * @param automationId The Automation ID to validate.
     * @param name         The Name to validate.
     * @param xpath        The XPath to validate.
     * @throws IllegalArgumentException If all identifiers are null or empty.
     */
    private static void validateElementIdentifiers(String automationId, String name, String xpath) {
        // Debug logging for input parameters
        System.out.println("DEBUG: automationId='" + automationId + "', name='" + name + "', xpath='" + xpath + "'");
        boolean isAutomationIdValid = automationId != null && !automationId.trim().isEmpty();
        boolean isNameValid = name != null && !name.trim().isEmpty();
        boolean isXpathValid = xpath != null && !xpath.trim().isEmpty();
        System.out.println("DEBUG: isAutomationIdValid=" + isAutomationIdValid + ", isNameValid=" + isNameValid + ", isXpathValid=" + isXpathValid);

        if (!isAutomationIdValid && !isNameValid && !isXpathValid) {
            throw new IllegalArgumentException(INVALID_ELEMENT_IDS);
        }
    }

    /**
     * Validates that the file path is non-null, non-empty, and points to a writable location.
     *
     * @param filePath The file path to validate.
     * @throws IllegalArgumentException If the file path is null, empty, or not writable.
     */
    private static void validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path for screenshot must not be null or empty.");
        }
        File destinationFile = new File(filePath);
        File parentDir = destinationFile.getParentFile();
        if (parentDir != null && parentDir.exists() && !parentDir.canWrite()) {
            throw new IllegalArgumentException(INVALID_FILE_PATH + parentDir.getAbsolutePath());
        }
    }

    /**
     * Locates an element by its Automation ID, Name, or XPath.
     *
     * @param automationId The Automation ID of the element, if available.
     * @param name         The Name of the element, used if automationId is null or empty.
     * @param xpath        The XPath of the element, used if both automationId and name are null or empty.
     * @return The located WebElement.
     * @throws NoSuchElementException If the element is not found.
     */
    private static WebElement findElement(String automationId, String name, String xpath) {
        if (automationId != null && !automationId.trim().isEmpty()) {
            return findElementByAutomationId(automationId);
        } else if (name != null && !name.trim().isEmpty()) {
            return findElementByName(name);
        } else if (xpath != null && !xpath.trim().isEmpty()) {
            return findElementByXpath(xpath);
        } else {
            // This should not happen due to prior validation, but added for safety
            throw new IllegalArgumentException(INVALID_ELEMENT_IDS);
        }
    }

    /**
     * Locates an element by its Automation ID.
     *
     * @param automationId The Automation ID of the element to locate.
     * @return The located WebElement.
     * @throws IllegalArgumentException If automationId is null or empty.
     * @throws NoSuchElementException   If the element is not found.
     * @throws IllegalStateException    If the driver is not initialized.
     */
    private static WebElement findElementByAutomationId(String automationId) {
        if (automationId == null || automationId.trim().isEmpty()) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_AUTOMATION_ID);
        }
        if (driver == null) {
            throw new IllegalStateException("WindowsDriver is not initialized.");
        }

        WebElement element = driver.findElementByAccessibilityId(automationId);
        if (element == null) {
            String errorMsg = "Element with Automation ID '" + automationId + "' not found.";
            System.err.println(errorMsg);
            throw new NoSuchElementException(errorMsg);
        }
        System.out.println("DEBUG: Element found by Automation ID: " + element.getClass().getName());
        return element;
    }

    /**
     * Locates an element by its Name.
     *
     * @param name The Name of the element to locate.
     * @return The located WebElement.
     * @throws IllegalArgumentException If name is null or empty.
     * @throws NoSuchElementException   If the element is not found.
     * @throws IllegalStateException    If the driver is not initialized.
     */
    private static WebElement findElementByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_NAME);
        }
        if (driver == null) {
            throw new IllegalStateException("WindowsDriver is not initialized.");
        }

        WebElement element = driver.findElementByName(name);
        if (element == null) {
            String errorMsg = "Element with Name '" + name + "' not found.";
            System.err.println(errorMsg);
            throw new NoSuchElementException(errorMsg);
        }
        System.out.println("DEBUG: Element found by Name: " + element.getClass().getName());
        return element;
    }

    /**
     * Locates an element by its XPath.
     *
     * @param xpath The XPath of the element to locate.
     * @return The located WebElement.
     * @throws IllegalArgumentException If xpath is null or empty.
     * @throws NoSuchElementException   If the element is not found.
     * @throws IllegalStateException    If the driver is not initialized.
     */
    private static WebElement findElementByXpath(String xpath) {
        if (xpath == null || xpath.trim().isEmpty()) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_XPATH);
        }
        if (driver == null) {
            throw new IllegalStateException("WindowsDriver is not initialized.");
        }

        WebElement element = driver.findElementByXPath(xpath);
        if (element == null) {
            String errorMsg = "Element with XPath '" + xpath + "' not found.";
            System.err.println(errorMsg);
            throw new NoSuchElementException(errorMsg);
        }
        System.out.println("DEBUG: Element found by XPath: " + element.getClass().getName());
        return element;
    }
}