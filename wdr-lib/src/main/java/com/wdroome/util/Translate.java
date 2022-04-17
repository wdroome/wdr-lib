package com.wdroome.util;

import java.util.HashMap;

public class Translate
{
	// The language-specific translator. Never null.
	private LangOverride m_langOverride = new LangOverride() {
		public String getLanguage() { return "???"; }
		public String translatePhrase(Enum<?> phrase) { return null; }
		public String translateWord(String word) { return null; }
		public boolean appendWord(StringBuilder buff, String word) { return false; }
	};
	
	// Translation cache, to avoid re-calculating them.
	// Populate on demand.
	private HashMap<Enum<?>,String> m_translationCache = new HashMap<>();
	
	// Language independent replacements for words in the enum names.
	private static HashMap<String,String> g_replaceWords = new HashMap<>();
	private static String[] g_initialReplaceWords = new String[] {
			"dot", ".",
			"dots", "...",
			"star", "*",
			"stars", "***",
			"colon", ":",
			"quest", "?",
			"minus", "-",
			"plus", "+",
			"slash", "/",
			"at", "@",
			"gt", ">",
			"lt", "<",
			"lparen", "(",
			"rparen", ")",
			"lbracket", "[",
			"rbracket", "]",
			"percent", "%",
			"sharp", "#",
			"newline", "\n",
			"space", " ",
	
			"controlkey", "\u2303",
			"shortcutkey", "\u2318",
			"optionkey", "\u2325",
			"shiftkey", "\u21E7",
			"capslockkey", "\u21EA",
			"returnkey", "\u23CE",
			"deletekey", "\u232B",
			"rightdeletekey", "\u2326",
			"uparrowkey", "\u2191",
			"downarrowkey", "\u2193",
			"leftarrowkey", "\u2190",
			"rightarrowkey", "\u2192",
	
			"fmtd", "%d",
			"fmts", "%s",
		};
	static {
		for (int i = 0; i+1 < g_initialReplaceWords.length; i += 2) {
			g_replaceWords.put(g_initialReplaceWords[i], g_initialReplaceWords[i+1]);
		}
	}

	public String trans(Enum<?> phrase)
	{
		String s;
		s = m_translationCache.get(phrase);
		if (s != null) {
			return s;
		}
		s = m_langOverride.translatePhrase(phrase);
		if (s != null) {
			m_translationCache.put(phrase, s);			
			return s;
		}
		
		String[] words = phrase.name().replaceAll("^.*__", "").split("_");
		StringBuilder trans = new StringBuilder();
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			if (!m_langOverride.appendWord(trans, word)) {
				if (word.charAt(0) == 'x') {
					word = word.substring(1);
				} else if (i > 0) {
					trans.append(' ');
				}
				String tw = m_langOverride.translateWord(word);
				if (tw == null) {
					tw = g_replaceWords.get(word);
				}
				if (tw != null) {
					trans.append(tw);
				} else {
					trans.append(word.charAt(0));
					trans.append(word.substring(1).toLowerCase());
				}
			}
		}
		s = trans.toString();
		m_translationCache.put(phrase, s);
		return s;
	}
	
	/**
	 * A language-specific translator module to extend or override the standard translator.
	 */
	public interface LangOverride
	{
		/**
		 * The name of this translator's language.
		 * @return The name of this translator's language.
		 */
		public String getLanguage();
		
		/**
		 * Translate a complete phrase.
		 * @param phrase The phrase enum.
		 * @return The complete translation for phrase,
		 * 		or null to use the default word-by-word translation algorithm.
		 */
		public String translatePhrase(Enum<?> phrase);
		
		/**
		 * Translate a word by itself.
		 * @param word The word to translate.
		 * 		This does not include any leading "x";
		 * 		the caller handles those.
		 * @return The translation of word,
		 * or null to use the default translation algorithm.
		 */
		public String translateWord(String word);
		
		/**
		 * Translate a word in the context of the preceding translation,
		 * and append it to the translation.
		 * @param buff The translated phrase preceding this word.
		 * @param word The word. This includes the leading "x", if any.
		 * @return True if this method appended a translation of "word"
		 * 		to "buff", or false if the method did not. If false, the caller
		 * 		uses the default translation algorithm.
		 */
		public boolean appendWord(StringBuilder buff, String word);
	}
	
	private enum TestEnum {
		BUTTON__HALF,
		BUTTON__FULL,
		BUTTON__RELEASE_ALL,
		BUTTON__RELEASE_SELECTED,
		BUTTON__NEW_PRESET,
		BUTTON__NEW_SUBMASTER,
		BUTTON__PREVIEW_ON_STAGE,
		BUTTON__STOP_PREVIEW,
		
		LABEL__ARTNET_UNIVERSES_xcolon,
		LABEL__ARTNET_CONTROLLER_xcolon,
		LABEL__CONTROL_CHANNELS_xcolon,
		LABEL__POLLING_xcolon,
		LABEL__AVAILABLE_ARTNET_NODES_xcolon,
		
		COL_LABEL__PRESET,
		COL_LABEL__SCALE_lparen_x0_xminus_x100_xrparen,
		COL_LABEL__NAME,	
		
		BOX_LABEL__UNIVERSE_NUMBER_xcolon,
		BOX_LABEL__ARTNET_PORT_xcolon,
		BOX_LABEL__SOCKET_ADDR_xcolon,
		BOX_LABEL__SOCKET_ADDR_OPT_xcolon,
		BOX_LABEL__OUTPUT_PORT_NUMBER_xcolon,
		BOX_LABEL__CREATE_CHANNELS_xcolon,
		BOX_LABEL__NAME_xcolon,
		BOX_LABEL__NODE_NAME_xcolon,
		BOX_LABEL__IP_PORTS_xcolon,
		BOX_LABEL__ARTNET_POLLING_PORTS_xcolon,
		BOX_LABEL__ARTNET_POLLING_ADDRESSES_xcolon,
		BOX_LABEL__POLL_INTERVAL_milli_xcolon,
		BOX_LABEL__DEFAULT_WORKSPACE_DIRECTORY_xcolon,
	};

	public static void main(String[] args)
	{
		Translate t = new Translate();
		for (TestEnum e: TestEnum.values()) {
			System.out.println(e + "=>" + t.trans(e));
		}
	}
}
