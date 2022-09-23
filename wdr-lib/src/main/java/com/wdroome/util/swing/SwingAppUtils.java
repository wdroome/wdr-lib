package com.wdroome.util.swing;
import javax.swing.SwingUtilities;

import java.awt.GraphicsDevice;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import com.wdroome.util.MiscUtil;

/**
 * Static utility methods for Swing programs.
 * @author wdr
 */
public class SwingAppUtils
{
	/** Environment variable that specifies the look and feel. */
	public static final String ENV_SWING_LNF = "SWING_LNF";
	
	/** Prefix for a command-line argument that specifies the look and feel. */
	public static final String ARG_SWING_LNF = "--lnf=";
	
	/**
	 * Create the initial Swing object, typically by a creating an Object that extends JFrame.
	 */
	@FunctionalInterface
	public interface CreateMainObject
	{
		public void createMainObject();
	}
	
	/**
	 * Handle an error setting the Look And Feel.
	 */
	@FunctionalInterface
	public interface LnfErrorHandler
	{
		/**
		 * Handle an error setting the Look And Feel.
		 * @param lnfSpec The LnF specifier that caused the error.
		 * @param e If null, lnfSpec could not be resolved to a Swing LnF class.
		 * 		If not null, could be resolved to a class, but swing,UIManager returned this error
		 * 		when trying to set it.
		 */
		public void handleLnfError(String lnfSpec, Exception e);
	}
	
	/**
	 * Return the full name of the Swing LookAndFeel class for an LnF.
	 * @param lnf Specify the LookAndFeel. If this is the name of a class
	 * 		which extends the LookAndFeel class, return lnf.
	 * 		If not, return 
	 * @return The name of the Swing LookAndFeel class to use.
	 *		If lnf is the full name of a class which extends LookAndFeel, return lnf.
	 * 		If not, if the description of one the registered LookAndFeel classes
	 *		contains the lnf string, return the name of that class.
	 *		Otherwise return null.  Return null if lnf is null or blank. 
	 */
	public static String getLookAndFeelClass(String lnf)
	{
		if (lnf == null || lnf.isBlank()) {
			return null;
		}
		lnf = lnf.trim();
		try {
			Class<?> lnfClass = Class.forName(lnf);
			if (MiscUtil.classIsa(lnfClass, LookAndFeel.class.getName())) {
				return lnf;
			}			
		} catch (ClassNotFoundException e) {
			// Skip -- lnf isn't a class name.
		}
		for (UIManager.LookAndFeelInfo info: UIManager.getInstalledLookAndFeels()) {
			if (info.getName().toLowerCase().contains(lnf.toLowerCase())) {
				return info.getClassName();
			}
		}
		return null;
	}
	
	/**
	 * Set the LookAndFeel.
	 * @param className The full name of the LookAndFeel class to use.
	 * @return Null if LnF was set successfully, or an Exception if an error occurred.
	 * 		However, if className is null or blank, return null.
	 */
	public static Exception setLookAndFeel(String className)
	{
		if (className == null || className.isBlank()) {
			return null;
		}
		try {
			UIManager.setLookAndFeel(className);
			return null;
		} catch (Exception e) {
			return e;
		} 
	}
	
	/**
	 * Start a Swing app and optionally set the Look And Feel.
	 * The "Look And Feel" is determined by environment variable {@link #ENV_SWING_LNF},
	 * or, if that isn't valid, a command line argument that starts with {@link #ARG_SWING_LNF}.
	 * {@link #getLookAndFeelClass(String)} maps those string to a Look And Feel class.
	 * @param swingInit A function that starts the app. Typically this creates an instance
	 * 		of an object that extends JFrame. Invoke this function on the Swing event-dispatching thread
	 * 		rather than the caller's thread.
	 * @param args The command-line arguments. May be null.
	 * @param errorHandler Handle and error while setting the Look And Feel. If null, ignore those errors.
	 */
	public static void startSwing(CreateMainObject swingInit, String[] args, LnfErrorHandler errorHandler)
	{
	    SwingUtilities.invokeLater(new Runnable() { 
	        public void run() {
	        	String lnfClass = null;
	        	String lnfError = null;
	        	if (args != null) {
	        		for (String arg: args) {
	        			if (arg.startsWith(ARG_SWING_LNF)) {
	        				String lnfTest = arg.substring(ARG_SWING_LNF.length());
	        				lnfClass = getLookAndFeelClass(lnfTest);
	        				if (lnfClass != null) {
	        					break;
	        				} else {
	        					lnfError = lnfTest;
	        				}
	        			}
	        		}	        		
	        	}
	        	if (lnfClass == null) {
	        		String lnfTest = System.getenv(ENV_SWING_LNF);
		        	// lnfTest = "Mac"; // XXX
		        	// lnfTest = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
	        		if (lnfTest != null && !lnfTest.isBlank()) {
	        			lnfClass = getLookAndFeelClass(lnfTest);
	        			if (lnfClass == null) {
	        				lnfError = lnfTest;
	        			}
	        		}
	        	}
	        	if (lnfClass != null) {
	        		Exception e = setLookAndFeel(lnfClass);
	        		if (e != null && errorHandler != null) {
	        			errorHandler.handleLnfError(lnfClass, e);
	        		}
	        	} else if (lnfError != null && errorHandler != null) {
	        		errorHandler.handleLnfError(lnfError, null);
	        	}
	        	swingInit.createMainObject(); 
	        } 
	      }); 
	}
	
	/**
	 * Start a Swing app and optionally set the Look And Feel.
	 * The "Look And Feel" is determined by environment variable {@link #ENV_SWING_LNF},
	 * or, if that isn't valid, a command line argument that starts with {@link #ARG_SWING_LNF}.
	 * {@link #getLookAndFeelClass(String)} maps those string to a Look And Feel class.
	 * If errors occur while setting the Look And Feel, print a message on System.err.
	 * @param swingInit A function that starts the app. Typically this creates an instance
	 * 		of an object that extends JFrame. Invoke this function on the Swing event-dispatching thread
	 * 		rather than the caller's thread.
	 * @param args The command-line arguments. May be null.
	 * @see #startSwing(CreateMainObject, String[], LnfErrorHandler)
	 */
	public static void startSwing(CreateMainObject swingInit, String[] args)
	{
		startSwing(swingInit, args,
				(lnfSpec, e) -> {
					if (e != null) {
						System.err.println("SwingAppUtils.startSwing(): Error setting Look & Feel \""
								+ lnfSpec + "\": " + e);
					} else {
	        			System.err.println("SwingAppUtils.startSwing(): No Look & Feel for \""
								+ lnfSpec + "\"");	        		
					}
				}
		);
	}
	
	/**
	 * Return the name for a Swing GraphicsDevice device type (TYPE_RASTER_SCREEN, etc).
	 * @param type A GrahicsDevice type code.
	 * @return The name of that type.
	 */
	public static String getGraphicsDevTypeCode(int type)
	{
		switch (type) {
		case GraphicsDevice.TYPE_IMAGE_BUFFER:
			return "ImageBuffer";
		case GraphicsDevice.TYPE_PRINTER:
			return "Printer";
		case GraphicsDevice.TYPE_RASTER_SCREEN:
			return "Screen";
		default:
			return "UnknownType";
		}
	}
	
	public static void main(String[] args)
	{
		for (UIManager.LookAndFeelInfo info: UIManager.getInstalledLookAndFeels()) {
			System.out.println(info.getName() + ": " + info.getClassName());
		}
	}
}
