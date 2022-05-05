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
	
	public static final String GET_CUELIST_COUNT_METHOD = "/eos/get/cuelist/count";
	public static final String GET_CUELIST_COUNT_REPLY = "/eos/out/get/cuelist/count";
	
		// Argument is the cuelist number, as a string.
	public static final String GET_CUE_COUNT_METHOD = "/eos/get/cue/%s/count";
	public static final String GET_CUE_COUNT_REPLY = "/eos/out/get/cue/%s/count";
}
