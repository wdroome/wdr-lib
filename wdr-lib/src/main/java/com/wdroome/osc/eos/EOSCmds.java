package com.wdroome.osc.eos;

public class EOSCmds
{
	public static final String GET_VERSION_CMD = "/eos/get/version";
	public static final String GET_VERSION_RESP = "/eos/out/get/version";
	
	public static final String GET_CUELIST_COUNT_CMD = "/eos/get/cuelist/count";
	public static final String GET_CUELIST_COUNT_RESP = "/eos/out/get/cuelist/count";
	
	public static final String GET_CUELIST_INFO_CMD = "/eos/get/cuelist/index/%d";	// arg is cuelist index, 0-N-1
	public static final String GET_CUELIST_INFO_RESP = "/eos/out/get/cuelist/[^/]+/list/%d/[0-9]+";
	public static final int GET_CUELIST_INFO_RESP_LIST_NUMBER = 4;
	public static final int GET_CUELIST_INFO_RESP_LIST_COUNT = 7;
		
	public static final String GET_CUE_COUNT_CMD = "/eos/get/cue/%d/count";		// arg is cuelist number
	public static final String GET_CUE_COUNT_RESP = "/eos/out/get/cue/%d/count";
}
