package com.wdroome.osc.eos;

/**
 * Constants and static utility methods for communicating with an EOS controller.
 * @author wdr
 */
public class EOSUtil
{
	public static final int DEFAULT_CUE_LIST = 1;
	
	public static final String GET_VERSION_METHOD = "/eos/get/version";
	public static final String GET_VERSION_REPLY = "/eos/out/get/version";
	
	// Send automatically when first connecting.
	public static final String SHOW_NAME_REPLY = "/eos/out/show/name";
	
	public static final String GET_CUELIST_COUNT_METHOD = "/eos/get/cuelist/count";
	public static final String GET_CUELIST_COUNT_REPLY = "/eos/out/get/cuelist/count";
	
		// Argument is the cuelist number, as a string.
	public static final String GET_CUE_COUNT_METHOD = "/eos/get/cue/%s/count";
	public static final String GET_CUE_COUNT_REPLY = "/eos/out/get/cue/%s/count";
	
			// OSC argument is cue number. Implies cuelist 1.
	public static final String FIRE_CUE_WITH_ARG_METHOD = "/eos/cue/fire";

			// Method argument is cue number. Implies cuelist 1.
	public static final String FIRE_CUE_IN_METHOD = "/eos/cue/%s/fire";

			// Method argument is cuelist number. Implies part 0.
	public static final String FIRE_CUELIST_CUE_METHOD = "/eos/cue/%d/%s/fire";
	
			// Method arguments are cuelist number, cue number and part number.
	public static final String FIRE_CUELIST_CUE_PART_METHOD = "/eos/cue/%d/%s/%d/fire";
	
	/**
	 * Return the method for a "fire-cue" command.
	 * @param cue The cue to fire.
	 * @return An OSC method to fire that cue.
	 */
	public static String makeCueFireRequest(EOSCueNumber cue)
	{
		if (cue.isPart()) {
			return String.format(FIRE_CUELIST_CUE_PART_METHOD,
							cue.getCuelist(), cue.getCueNumber(), cue.getPartNumber());
		} else {
			return String.format(FIRE_CUELIST_CUE_METHOD,
							cue.getCuelist(), cue.getCueNumber());			
		}
	}
	
	/**
	 * Extract the cue number from an OSC command to fire an EOS cue.
	 * @param request The OSC request, as a string. A blank separates the method from the arguments, if any.
	 * @return The EOS cue number of the cue that request will fire,
	 * 			or null if request isn't a valid fire-cue command.
	 */
	public static EOSCueNumber getCueInFireRequest(String request)
	{
		if (request == null) {
			return null;
		}
		String method;
		String arg;
		String[] methodArgs = request.split("[ \t]+");
		switch (methodArgs.length) {
		case 1:
			method = methodArgs[0];
			arg = null;
			break;
		case 2:
			method = methodArgs[0];
			arg = methodArgs[1];
			if (arg.startsWith("\"") && arg.endsWith("\"")) {
				arg = arg.substring(1);
				arg = arg.substring(0, arg.length()-1);
			}
			break;
		default:
			return null;
		}
		if (!(method.startsWith("/eos/cue/") && method.endsWith("/fire"))) {
			return null;
		}
		String[] methodParts = method.substring(1).split("/");
		if (methodParts.length == 3 && arg != null && !arg.isBlank()) {
			return new EOSCueNumber(DEFAULT_CUE_LIST, arg, 0);
		} else if (methodParts.length == 4 && arg == null) {
			return new EOSCueNumber(DEFAULT_CUE_LIST, methodParts[3], 0);
		} else if (methodParts.length == 5 && arg == null) {
			try {
				return new EOSCueNumber(Integer.parseInt(methodParts[2]), methodParts[3], 0);
			} catch (NumberFormatException e) {
				return null;
			}
		} else if (methodParts.length == 6 && arg == null) {
			try {
				return new EOSCueNumber(Integer.parseInt(methodParts[2]), methodParts[3],
						Integer.parseInt(methodParts[4]));
			} catch (NumberFormatException e) {
				return null;
			}
		} else {
			return null;
		}
	}
}
