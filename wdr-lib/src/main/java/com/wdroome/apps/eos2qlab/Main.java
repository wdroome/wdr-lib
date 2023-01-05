package com.wdroome.apps.eos2qlab;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;

import com.wdroome.util.MiscUtil;

public class Main
{
	public static void main(String[] args)
	{
		boolean running = true;
		PrintStream out = System.out;
		InputStream in = System.in;

		String resumeCmdLine = null;
		while (running) {
			try (EOS2QLab eos2QLab = new EOS2QLab(args)) {
				eos2QLab.prtEOSCueSummary();
				eos2QLab.prtQLabCueSummary();
				while (true) {
					String[] cmd;
					String cmdLine;
					long lastCmdTS = System.currentTimeMillis();
					if (resumeCmdLine != null) {
						cmdLine = resumeCmdLine;
						out.println();
						out.println("Resuming " + resumeCmdLine + " ...");
						resumeCmdLine = null;
					} else {
						out.print("* ");
						cmdLine = MiscUtil.readLine(in);
						if (cmdLine == null) {
							running = false;
							break;
						}
					}
					cmdLine = cmdLine.trim();
					if (cmdLine.isBlank()) {
						continue;
					}
					cmd = cmdLine.split("[ \t]+");
					if (cmd.length == 0) {
						continue;
					} 
					if (Commands.isCmd(cmd[0], Commands.REFRESH_CMD)) {
						break;
					} else if (Commands.isCmd(cmd[0], Commands.QUIT_CMD)) {
						running = false;
						break;
					} else if (Commands.isCmd(cmd[0], Commands.PRINT_CMD)) {
						if (cmd.length >= 2 && Commands.isCmd(cmd[1], Commands.EOS_ARG)) {
							eos2QLab.prtEOSCueSummary();							
						} else if (cmd.length >= 2 && Commands.isCmd(cmd[1], Commands.QLAB_ARG)) {
							eos2QLab.prtQLabCueSummary();							
						} else if (cmd.length >= 2 && Commands.isCmd(cmd[1], Commands.MISSING_ARG)) {
							eos2QLab.prtCuesNotInQLab(true, true);
							eos2QLab.prtCuesNotInEOS();
						} else if (cmd.length == 2 && Commands.isCmd(cmd[1], Commands.CONFIG_ARG)) {
							eos2QLab.prtConfig("   ");
						} else if (cmd.length >= 2) {
							out.print("Unknown print command.");
						} else {
							eos2QLab.prtEOSCueSummary();							
							eos2QLab.prtCuesNotInQLab(true, true);
							eos2QLab.prtQLabCueSummary();							
							eos2QLab.prtCuesNotInEOS();
						}
					} else if (Commands.isCmd(cmd[0], Commands.CHECK_CMD)) {
						eos2QLab.notInQLab();
						eos2QLab.notInEOS();
						eos2QLab.prtCuesNotInEOS();					
						eos2QLab.prtCuesNotInQLab(true, false);							
					} else if (Commands.isCmd(cmd[0], Commands.ADD_CMD)) {
						if (System.currentTimeMillis() - lastCmdTS > 30000) {
							Boolean resp = new ReadResponse(in, out)
											.getYesNoResponse("Refresh QLab & EOS cue lists? ");
							if (resp != null && resp) {
								resumeCmdLine = cmdLine;
								break;
							}
						}
						eos2QLab.add2QLab();
					} else if (Commands.isCmd(cmd[0], Commands.SELECT_CMD)) {
						if (cmd.length >= 2 && Commands.isCmd(cmd[1], Commands.MISSING_ARG)) {
							eos2QLab.selectCuesNotInEOS();							
						} else if (cmd.length >= 2 && Commands.isCmd(cmd[1], Commands.ORDER_ARG)) {
							eos2QLab.selectMisorderedCues();							
						} else {
							out.println("Usage: " + Commands.SELECT_CMD[0]
															+ " {" + Commands.MISSING_ARG[0]
															+ " | " + Commands.ORDER_ARG[0] + "}");
						}
					} else if (Commands.isCmd(cmd[0], Commands.HELP_CMD)) {
						for (String s: Commands.HELP_RESP) {
							out.println(s);
						}
					} else if (Commands.isCmd(cmd[0], new String[] {"test-replace"})) {
						if (cmd.length < 3) {
							out.println("Usage: test-replace eos-cue-number string");
						} else {
							eos2QLab.testReplace(cmd[1], Arrays.copyOfRange(cmd, 2, cmd.length));
						}
					} else if (Commands.isCmd(cmd[0], new String[] {"test-cuenum"})) {
						eos2QLab.testCueNum();
					} else {
						out.println("Unknown command \"" + cmdLine + "\"");
					}
				}
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				running = false;
			} catch (IOException e) {
				System.err.println(e);
				running = false;
			}
		}
	}

}
