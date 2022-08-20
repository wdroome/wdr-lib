package com.wdroome.util.swing;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;

import javax.swing.UIManager;

public class SwingInfo {

	public static void main(String[] args)
	{
		GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		System.out.println("Installed Look & Feels:");
		for (UIManager.LookAndFeelInfo info: UIManager.getInstalledLookAndFeels()) {
			System.out.println("  " + info.getName() + ": " + info.getClassName());
		}
		System.out.println("Font Families:");
		for (String fontName: genv.getAvailableFontFamilyNames()) {
			System.out.println("  " + fontName);
		}
		for (GraphicsDevice gdev: genv.getScreenDevices()) {
			System.out.println("Graphics Device " + gdev.getIDstring() + ":");
			GraphicsConfiguration gconf = gdev.getDefaultConfiguration();
			Rectangle bounds = gconf.getBounds();
			System.out.println("  Bounds: x,y=" + bounds.x + "," + bounds.y +
					" wid,ht=" + bounds.width + "x" + bounds.height);
			System.out.println("  Color Model: " + gconf.getColorModel());
		}
	}
}
