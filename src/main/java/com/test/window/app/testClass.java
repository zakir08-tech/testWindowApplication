package com.test.window.app;

public class testClass {

	public static void main(String[] args) {
		String xpath = "//Group[@Name='Primary Navigation']"
        		+ "/List[starts-with(@ClassName,'global-nav__primary-items')]"
        		+ "/ListItem[@Name='Messaging']";
		
		try {
	        GlueCode.invokeTheApplication("7EE7776C.LinkedInforWindows_w1wdnht996qgy", "LinkedIn");
	        GlueCode.takeScreenshotWithElementHighlight("C:\\Users\\zakir\\Documents\\ScreenShots\\calculator_screenshot.png",
	        		"",
	        		"",xpath);
	        //GlueCode.takeScreenshot("C:\\Users\\zakir\\Documents\\ScreenShots\\calculator_screenshot.png");
	        GlueCode.clickElement("", "",xpath);
	        GlueCode.closeApplication("LinkedIn");
	        //GlueCode.closeApplicationByProcess("LinkedIn");
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

}
