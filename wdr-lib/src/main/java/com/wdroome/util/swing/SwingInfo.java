package com.wdroome.util.swing;

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.DisplayMode;

import javax.swing.UIManager;
import javax.swing.LookAndFeel;

/**
 * Print information about the Swing graphics environment.
 * @author wdr
 */
public class SwingInfo {

	public static void main(String[] args)
	{
		boolean showFontFam = false;
		for (String arg: args) {
			if (arg.startsWith("-f")) {
				showFontFam = true;
			}
		}
		
		GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice defGdev = genv.getDefaultScreenDevice();
		
		String indent = "    ";
		for (GraphicsDevice gdev: genv.getScreenDevices()) {
			System.out.println("Graphics Device \"" + gdev.getIDstring()
				+ "\" [" + SwingAppUtils.getGraphicsDevTypeCode(gdev.getType())+ "]:");
			if (gdev.equals(defGdev)) {
				System.out.println(indent + "Default Device");
			}
			GraphicsConfiguration gconf = gdev.getDefaultConfiguration();
			DisplayMode mode = gdev.getDisplayMode();
			Rectangle bounds = gconf.getBounds();
			System.out.println(indent + "width=" + bounds.width
					+ " height=" + bounds.height
					+ " upper-left=" + bounds.x + "," + bounds.y
					+ " depth=" + mode.getBitDepth() + " bits/pixel"
					+ " refresh=" + mode.getRefreshRate() + " hz");
			System.out.println(indent + "Color Model: " + gconf.getColorModel());
		}
		
		System.out.println();
		LookAndFeel curLnF = UIManager.getLookAndFeel();
		System.out.println("Available Swing Look & Feels:");
		for (UIManager.LookAndFeelInfo info: UIManager.getInstalledLookAndFeels()) {
			System.out.println(indent + info.getName() + ": " + info.getClassName());
		}
		System.out.println("Current Look & Feel: "
				+ (curLnF != null ? (curLnF.getID() + "/" + curLnF.getDescription()) : "[none]")); 
		
		if (showFontFam) {
			System.out.println();
			System.out.println("Font Families:");
			for (String fontName : genv.getAvailableFontFamilyNames()) {
				System.out.println(indent + fontName);
			} 
		}
	}
}
