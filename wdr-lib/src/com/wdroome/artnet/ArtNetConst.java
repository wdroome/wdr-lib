package com.wdroome.artnet;

/**
 * Common constants for the Art-Net Protocol (version 3).
 * @author wdr
 */
public class ArtNetConst
{
	/** Common message header string. */
	public static final byte[] HEADER_STRING
					= {'A', 'r', 't', '-', 'N', 'e', 't', 0};

	/** Common message header length: header string + opcode. */
	public static final int HDR_OPCODE_LENGTH = HEADER_STRING.length + ArtNetOpcode.SIZE;

	/** Current protocol version. */
	public static final int PROTO_VERS = 14;

	/** Length of protocol version field right after opcode field. */
	public static final int PROTO_VERS_LENGTH = 2;
	
	/** Standard UDP port number. */
	public static final int ARTNET_PORT = 0x1936;
	
	/** Buffer size guaranteed to hold any Art-Net message. */
	public static final int MAX_MSG_LEN = 1024;
	
	/** Max time for a node to reply to a poll request, in millisec. */
	public static final long MAX_POLL_REPLY_MS = 3000;
	
	/** Interval between sending poll requests, in millisec. */ 
	public static final long SEND_POLL_INTVL_MS = 2500;

	/**
	 *  Table 3: Node report codes.
	 */
	
	// Booted in debug mode (Only used in development)
	public static final int RcDebug = 0x0000; 

	// Power On Tests successful
	public static final int RcPowerOk = 0x0001; 

	// Hardware tests failed at Power On
	public static final int RcPowerFail = 0x0002; 

	// Last UDP from Node failed due to truncated length.
	// Most likely caused by a collision.
	public static final int RcSocketWr1 = 0x0003;

	// Unable to identify last UDP transmission.
	// Check OpCode and packet length.
	public static final int RcParseFail = 0x0004; 

	// Unable to open Udp Socket in last transmission attempt
	public static final int RcUdpFail = 0x0005; 

	// Confirms that Short Name programming via ArtAddress, was successful.
	public static final int RcShNameOk = 0x0006; 

	// Confirms that Long Name programming via ArtAddress, was successful.
	public static final int RcLoNameOk = 0x0007; 

	// DMX512 receive errors detected.
	public static final int RcDmxError = 0x0008; 

	// Ran out of internal DMX transmit buffers.
	public static final int RcDmxUdpFull = 0x0009; 

	// Ran out of internal DMX Rx buffers.
	public static final int RcDmxRxFull = 0x000a; 

	// Rx Universe switches conflict.
	public static final int RcSwitchErr = 0x000b; 

	// Product configuration does not match firmware.
	public static final int RcConfigErr = 0x000c; 

	// DMX output short detected. See GoodOutput field.
	public static final int RcDmxShort = 0x000d; 

	// Last attempt to upload new firmware failed.
	public static final int RcFirmwareFail = 0x000e;

	// User changed switch settings when address locked
	// by remote programming. User changes ignored.
	public static final int RcUserFail = 0x000f;
	
	/**
	 * Table 4: Style codes.
	 */

	// A DMX to / from Art-Net device
	public static final int StNode = 0x00;

	// A lighting console.
	public static final int StController = 0x01;

	// A Media Server.
	public static final int StMedia = 0x02;

	// A network routing device.
	public static final int StRoute = 0x03;

	// A backup device.
	public static final int StBackup = 0x04;

	// A configuration or diagnostic tool.
	public static final int StConfig = 0x05;

	// A visualiser.
	public static final int StVisual = 0x06;
	
	/**
	 * Table 5: Priority codes.
	 */
	// Priority code: Low priority message.
	public static final int DpLow = 0x10;

	// Priority code: Medium priority message.
	public static final int DpMed = 0x40;

	// Priority code: High priority message.
	public static final int DpHigh = 0x80;

	// Priority code: Critical priority message.
	public static final int DpCritical = 0xe0;

	// Priority code: Volatile message.
	public static final int DpVolatile = 0xf0;
}
