package com.test.window.setup;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;

public interface User32Extended extends StdCallLibrary {
    User32Extended INSTANCE = Native.load("user32", User32Extended.class);

    boolean EnumWindows(WinUser.WNDENUMPROC lpEnumFunc, Pointer lParam);
    int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
}

