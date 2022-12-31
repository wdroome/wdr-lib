package com.wdroome.apps.eos2qlab;

public class Commands
{
	public final static String[] CHECK_CMD = {"check", "chk", "compare", "cmp"};
	public final static String[] PRINT_CMD = {"print", "prt"};
	public final static String[] QUIT_CMD = {"quit", "q", "exit"};
	public final static String[] REFRESH_CMD = {"refresh"};
	public final static String[] ADD_CMD = {"add", "add-cues"};
	public final static String[] SELECT_CMD = {"select", "sel"};
	public final static String[] HELP_CMD = {"help", "?"};
	
	public final static String[] EOS_ARG = {"eos"};
	public final static String[] QLAB_ARG = {"qlab"};
	public final static String[] MISSING_ARG = {"missing", "miss"};
	public final static String[] ORDER_ARG = {"order", "seqn"};
	public final static String[] CONFIG_ARG = {"config"};
	
	public final static String[] HELP_RESP = {
				"refresh: Get the cue information from EOS & QLab.",
				"check: Find the EOS cues not in QLab, and the QLab cues not in EOS.",
				"add: Add missing EOS cues to QLab.",
				"select missing: Select QLab network cues not in EOS.",
				"select order: Select QLab network cues not in EOS cue order.",
				"print eos: Print a summary of the EOS cues.",
				"print qlab: Print a summary of the QLab cues.",
				"print missing: Print the EOS cues not in QLab and the QLab cues not in EOS.",
				"print config: Print the JSON configuration file.",
				"print: Print all of the above.",
				"quit: Quit.",
	};

	/**
	 * Return true iff "cmd" matches a string in "cmds". Ignore case.
	 */
	public static boolean isCmd(String cmd, String[] cmds)
	{
		for (String s: cmds) {
			if (cmd.equalsIgnoreCase(s)) {
				return true;
			}
		}
		return false;
	}
}
