package com.wdroome.util.swing;

import java.util.HashMap;

import javax.swing.JComponent;

public class SwingComponentMgr
{
	public static final SwingComponentMgr g_defMgr = new SwingComponentMgr();
	
	private final HashMap<String,JComponent> m_idMap = new HashMap<>();
	
	private static final String PROP_NAME_PREFIX = "com.wdroome.util.swing.SwingComppnentMgr";
	private static final String ID_PROP_NAME = PROP_NAME_PREFIX + ".id";
	private static final String STYLE_PROP_NAME = PROP_NAME_PREFIX + ".style";
	
	public SwingComponentMgr() {}
	
	/**
	 * Set the ID for a JComponent.
	 * @param comp The JCmponent.
	 * @param id The id.
	 * @return The JComponent.
	 */
	public JComponent setId(JComponent comp, String id)
	{
		if (comp != null && id != null && !id.isBlank()) {
			m_idMap.put(id, comp);
			comp.putClientProperty(ID_PROP_NAME, id);
			// XXX: set properties for id!!!
		}
		return comp;
	}
	
	/**
	 * Set the style for a JComponent.
	 * @param comp The JCmponent.
	 * @param style The style.
	 * @return The JComponent.
	 */
	public JComponent setStyle(JComponent comp, String style)
	{
		if (comp != null && style != null && !style.isBlank()) {
			comp.putClientProperty(STYLE_PROP_NAME, style);
			// XXX: set properties for style!!!
		}
		return comp;
	}
	
	/**
	 * Set the style and ID for a JComponent.
	 * @param comp The JCmponent.
	 * @param style The style.
	 * @param id The id.
	 * @return The JComponent.
	 */
	public JComponent setStyleId(JComponent comp, String style, String id)
	{
		setStyle(comp, style);
		setId(comp, id);
		return comp;
	}
	
	/**
	 * Get the JComponent with an ID.
	 * @param id An ID.
	 * @return The JComponent with that ID, or null if none.
	 */
	public JComponent getById(String id)
	{
		return m_idMap.get(id);
	}
}
