package com.test.window.app;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.remote.DesiredCapabilities;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

import io.appium.java_client.windows.WindowsDriver;

public class App1 {
    public static WindowsDriver driver = null;

    // JNA interface for additional User32 functions
    public interface User32Extended extends StdCallLibrary {
        User32Extended INSTANCE = Native.load("user32", User32Extended.class);

        boolean EnumWindows(WinUser.WNDENUMPROC lpEnumFunc, Pointer lParam);
        int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
    }

    public static void main(String[] args) {
        // Launch the LinkedIn UWP app via shell (this opens it without WinAppDriver)
        try {
            Runtime.getRuntime().exec("explorer.exe \"shell:appsFolder\\7EE7776C.LinkedInforWindows_w1wdnht996qgy!App\"");
            System.out.println("Launched LinkedIn app. Waiting for it to open...");
            Thread.sleep(10000); // Wait 10 seconds (adjust based on app load time; increase if needed)
        } catch (Exception e) {
            System.err.println("Failed to launch app: " + e.getMessage());
            return;
        }

        // Find the HWND by window title
        HWND hwnd = findWindowByTitle("LinkedIn"); // Replace with exact title if different (case-sensitive)
        if (hwnd == null) {
            System.err.println("Could not find LinkedIn window. Verify title and ensure app is open.");
            return;
        }

        // Convert HWND to hex string for capability
        String hexHandle = "0x" + Long.toHexString(Pointer.nativeValue(hwnd.getPointer()));

        DesiredCapabilities cap = new DesiredCapabilities();
        cap.setCapability("appTopLevelWindow", hexHandle); // Attach to existing window
        cap.setCapability("platformName", "Windows");
        cap.setCapability("deviceName", "WindowsPC");
        // No need for appLaunchTimeout or ms:waitForAppLaunch since we're attaching

        try {
            // Initialize WindowsDriver by attaching
            driver = new WindowsDriver(new URL("http://127.0.0.1:4723"), cap);
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
            //System.out.println(driver.findElementByName("Messaging").getText());
            driver.findElementByXPath("//Group[@Name='Primary Navigation']"
            		+ "/List[starts-with(@ClassName,'global-nav__primary-items')]"
            		+ "/ListItem[@Name='Messaging']").click();

            System.out.println("Attached to LinkedIn app successfully.");
            // Add basic interaction to verify (replace with actual locator)
            // driver.findElement(By.name("SomeElement")).click();

        } catch (MalformedURLException e) {
            System.err.println("Invalid URL for WinAppDriver: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error during session creation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                // driver.quit(); // Uncomment to close session/app at end
            }
        }
    }

    // Method to find HWND by title using JNA
    private static HWND findWindowByTitle(String title) {
        final List<HWND> windows = new ArrayList<>();
        final User32Extended user32 = User32Extended.INSTANCE;

        user32.EnumWindows(new WinUser.WNDENUMPROC() {
            @Override
            public boolean callback(HWND hWnd, Pointer arg1) {
                byte[] windowText = new byte[512];
                user32.GetWindowTextA(hWnd, windowText, 512);
                String wText = new String(windowText).trim();
                if (!wText.isEmpty() && wText.contains(title)) {
                    windows.add(hWnd);
                }
                return true;
            }
        }, null);

        return windows.isEmpty() ? null : windows.get(0); // Return first match (adjust if multiple)
    }
}