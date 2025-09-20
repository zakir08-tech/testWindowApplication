package com.test.window.app;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import org.json.JSONArray;
import org.json.JSONObject;

public class Commons {
	 public static File getDocumentsDirectory() {
	    String userHome = System.getProperty("user.home");
	    File documentsDir;
	    String os = System.getProperty("os.name").toLowerCase();
	    if (os.contains("win")) {
	        documentsDir = new File(userHome, "Documents");
	    } else if (os.contains("mac")) {
	        documentsDir = new File(userHome, "Documents");
	    } else {
	        documentsDir = new File(userHome, "Documents");
	    }
	    if (documentsDir.exists() && documentsDir.isDirectory()) {
	        return documentsDir;
	    } else {
	        return new File(userHome);
	    }
	 }
}
