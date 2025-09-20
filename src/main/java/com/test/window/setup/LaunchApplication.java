package com.test.window.setup;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.remote.DesiredCapabilities;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import io.appium.java_client.windows.WindowsDriver;

/**
 * A utility class to launch and attach to a Windows UWP application using WinAppDriver.
 */
public class LaunchApplication {
    // Instance variables for HWND, hex handle, capabilities, and driver
    public static HWND hwnd;
    public static String hexHandle;
    public static final DesiredCapabilities cap = new DesiredCapabilities();
    public static WindowsDriver driver = null;

    /**
     * Launches a Windows UWP application using the shell command.
     *
     * @param appFamilyName The family name of the UWP app (e.g., "Microsoft.LinkedIn_8wekyb3d8bbwe").
     * @throws IllegalArgumentException if appFamilyName is null or empty.
     */
    public static void launchApp(String appFamilyName) {
        if (appFamilyName == null || appFamilyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application family name cannot be null or empty.");
        }
        try {
            // Execute shell command to launch the UWP app
            Runtime.getRuntime().exec("explorer.exe \"shell:appsFolder\\" + appFamilyName + "!App\"");
            System.out.println("Launched UWP app. Waiting for it to open...");
            Thread.sleep(5000); // Wait 5 seconds for the app to load (adjust if needed)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            System.err.println("Thread interrupted while waiting for app to launch: " + e.getMessage());
            throw new RuntimeException("Failed to wait for app launch due to interruption.", e);
        } catch (IOException e) {
            System.err.println("IO error while launching app: " + e.getMessage());
            throw new RuntimeException("Failed to launch app due to IO error.", e);
        } catch (Exception e) {
            System.err.println("Unexpected error while launching app: " + e.getMessage());
            throw new RuntimeException("Failed to launch app due to unexpected error.", e);
        }
    }

    /**
     * Finds the window handle (HWND) by its window title.
     *
     * @param windowTitle The title of the window to locate (case-sensitive).
     * @throws IllegalArgumentException if windowTitle is null or empty.
     */
    public static void findHWNDByWindowTitle(String windowTitle) {
        if (windowTitle == null || windowTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Window title cannot be null or empty.");
        }
        try {
            hwnd = findWindowByTitle(windowTitle);
            if (hwnd == null) {
                System.err.println("Could not find window with title '" + windowTitle + "'. Verify title and ensure app is open.");
            } else {
                System.out.println("Found window with title: " + windowTitle);
            }
        } catch (Exception e) {
            System.err.println("Error while finding window handle: " + e.getMessage());
            throw new RuntimeException("Failed to find window handle due to unexpected error.", e);
        }
    }

    /**
     * Converts the HWND to a hexadecimal string for use in DesiredCapabilities.
     *
     * @throws IllegalStateException if HWND is null.
     */
    public static void hwndToHex() {
        if (hwnd == null) {
            System.err.println("HWND is null. Cannot convert to hex.");
            throw new IllegalStateException("HWND is null. Ensure window is found before converting to hex.");
        }
        try {
            hexHandle = "0x" + Long.toHexString(Pointer.nativeValue(hwnd.getPointer()));
            System.out.println("Converted HWND to hex: " + hexHandle);
        } catch (Exception e) {
            System.err.println("Error converting HWND to hex: " + e.getMessage());
            throw new RuntimeException("Failed to convert HWND to hex due to unexpected error.", e);
        }
    }

    /**
     * Sets the DesiredCapabilities for attaching to the existing application window.
     *
     * @throws IllegalStateException if hexHandle is null.
     */
    public static void setDesiredCapabilities() {
        if (hexHandle == null) {
            System.err.println("Hex handle is null. Ensure HWND is converted to hex before setting capabilities.");
            throw new IllegalStateException("Hex handle is null. Ensure HWND is converted before setting capabilities.");
        }
        try {
            cap.setCapability("appTopLevelWindow", hexHandle); // Attach to the existing window
            cap.setCapability("platformName", "Windows");
            cap.setCapability("deviceName", "WindowsPC");
            System.out.println("DesiredCapabilities set successfully.");
        } catch (Exception e) {
            System.err.println("Error setting DesiredCapabilities: " + e.getMessage());
            throw new RuntimeException("Failed to set DesiredCapabilities due to unexpected error.", e);
        }
    }

    /**
     * Attaches the WindowsDriver to the application window using WinAppDriver.
     *
     * @throws IllegalStateException if DesiredCapabilities are not properly set.
     */
    public static void attachWindowDriverWithApplication() {
        if (cap.getCapability("appTopLevelWindow") == null) {
            System.err.println("DesiredCapabilities not set properly. Cannot attach driver.");
            throw new IllegalStateException("DesiredCapabilities not set. Ensure capabilities are configured.");
        }
        try {
            // Initialize WindowsDriver to attach to the existing window
            driver = new WindowsDriver<>(new URL("http://127.0.0.1:4723"), cap);
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
            System.out.println("Successfully attached WindowsDriver to the application.");
        } catch (MalformedURLException e) {
            System.err.println("Invalid URL for WinAppDriver: " + e.getMessage());
            throw new RuntimeException("Failed to attach WindowsDriver due to invalid URL.", e);
        } catch (Exception e) {
            System.err.println("Error during WindowsDriver session creation: " + e.getMessage());
            throw new RuntimeException("Failed to attach WindowsDriver due to unexpected error.", e);
        }
    }

    /**
     * Closes the WindowsDriver session, if active.
     */
    public static void closeDriver() {
        if (driver != null) {
            try {
                driver.quit();
                System.out.println("WindowsDriver session closed successfully.");
            } catch (Exception e) {
                System.err.println("Error closing WindowsDriver session: " + e.getMessage());
                throw new RuntimeException("Failed to close WindowsDriver session.", e);
            } finally {
                driver = null;
            }
        } else {
            System.out.println("No active WindowsDriver session to close.");
        }
    }

    /**
     * Finds the HWND of a window by its title using JNA.
     *
     * @param title The title of the window to locate.
     * @return The HWND of the first matching window, or null if none found.
     * @throws IllegalArgumentException if title is null or empty.
     */
    private static HWND findWindowByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Window title cannot be null or empty.");
        }
        final List<HWND> windows = new ArrayList<>();
        final User32Extended user32 = User32Extended.INSTANCE;

        try {
            user32.EnumWindows((hWnd, arg1) -> {
                byte[] windowText = new byte[512];
                try {
                    user32.GetWindowTextA(hWnd, windowText, 512);
                    String wText = new String(windowText).trim();
                    if (!wText.isEmpty() && wText.contains(title)) {
                        windows.add(hWnd);
                    }
                } catch (Exception e) {
                    System.err.println("Error retrieving window text for HWND: " + e.getMessage());
                }
                return true;
            }, null);
        } catch (Exception e) {
            System.err.println("Error enumerating windows: " + e.getMessage());
            throw new RuntimeException("Failed to enumerate windows due to unexpected error.", e);
        }

        return windows.isEmpty() ? null : windows.get(0); // Return first matching window
    }

    /**
     * Gets the current WindowsDriver instance.
     *
     * @return The active WindowsDriver, or null if not initialized.
     */
    public static WindowsDriver getDriver() {
        return driver;
    }
}