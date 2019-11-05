package com.wdroome.altomsgs;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONException;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParser;
import com.wdroome.json.JSONValue_Object;

import com.wdroome.util.StringUtils;

/**
 * Given an incoming JSON message and its media type,
 * return an object representing the associated ALTO message.
 * @author wdr
 */
public class MakeALTOMsg
{
	/** The name of the MEDIA_TYPE field in ALTO message classes. */
	private final static String MEDIA_TYPE_FIELD = "MEDIA_TYPE";

	/** The name of the static inputSizeLimit() method in ALTO message classes. */
	private final static String INPUT_SIZE_LIMIT_METHOD = "inputSizeLimit";

	/** The name of the static validate(JSONValue_Object) method in ALTO message classes. */
	private final static String VALIDATE_METHOD = "validate";

	/** The name of the static getServiceName(String) method in ALTO message classes. */
	private final static String GET_SERVICE_NAME_METHOD = "getServiceName";

	/**
	 * Leaf names of classes that may represent an ALTO message.
	 * These classes must be in the same package as this class.
	 * We ignore any class that does not define
	 * a static String field named "MEDIA_TYPE".
	 */
	private final static String[] ALTO_MSG_CLASSES = new String[] {
			"AltoMsg_Base",
			"AltoResp_Base",
			
			"AltoResp_CostMap",
			"AltoResp_EndpointCost",
			"AltoResp_EndpointProp",
			"AltoResp_Error",
			"AltoResp_IndexedCostMap",
			"AltoResp_InfoResourceDir",
			"AltoResp_NetworkMap",
			
			"AltoReq_EndpointCostParams",
			"AltoReq_EndpointPropParams",
			"AltoReq_FilteredCostMap",
			"AltoReq_FilteredNetworkMap",
			
			"AltoPriv_GeneralResponse",
			"AltoPriv_UpdateData",
		};
		
	/**
	 * List of known ALTO message classes.
	 */
	private static List<Class<? extends AltoMsg_Base>> g_altoMsgClasses = new ArrayList<Class<? extends AltoMsg_Base>>();
	
	/**
	 * Initialize g_altoMsgClasses: for each leaf name in ALTO_MSG_CLASSES,
	 * if the class defines MEDIA_TYPE, add it to the map.
	 */
	static {
		Package pack = AltoMsg_Base.class.getPackage();
		if (pack != null) {
			String packName = pack.getName();
			for (String className: ALTO_MSG_CLASSES) {
				try {
					Class<? extends AltoMsg_Base> msgClass
							= Class.forName(packName + "." + className).asSubclass(AltoMsg_Base.class);
 					Field mediaTypeField = msgClass.getField(MEDIA_TYPE_FIELD);
					String mediaType = (String)mediaTypeField.get(null);
					if (mediaType != null && !mediaType.equals("")) {
						g_altoMsgClasses.add(msgClass);
						if (false) {
							System.out.println("XXX: ALTO msg class " + msgClass.getName()
									+ " mediaType: " + mediaType
									+ " inputLimit: " + classInputSizeLimit(msgClass));
						}
					}
				} catch (Exception e) {
					// Ignore class if any error occurs.
				}
			}
		}
		// System.out.println("XXX: g_altoMsgClasses=" + g_altoMsgClasses);
	}
	
	/**
	 * Given an incoming JSON message and its media type,
	 * return an object representing the associated ALTO message.
	 * @param mediaType The media type of the input.
	 * @param lexan A JSON parser for the input.
	 * @return An object representing the ALTO message.
	 * @throws JSONException
	 * 			If there is no ALTO message class for mediaType,
	 * 			or if a class exists but does not have a constructor
	 * 			that takes a single IJSONLexan arugment,
	 * 			or if that c'tor throws an exception.
	 * @see #jsonToALTO(String, IJSONLexan, List)
	 */
	public static AltoMsg_Base jsonToALTO(String mediaType, IJSONLexan lexan)
			throws JSONException
	{
		return jsonToALTO(mediaType, lexan, null);
	}
	
	/**
	 * Given an incoming JSON message and its media type,
	 * return an object representing the associated ALTO message.
	 * @param mediaType The media type of the input.
	 * @param lexan A JSON parser for the input.
	 * @param msgClasses A set of classes to decode ALTO messages.
	 * 		The returned object will be an instance of first suitable class.
	 * 		If null, use a list of core message classes.
	 * @return An object representing the ALTO message.
	 * @throws JSONException
	 * 			If there is no ALTO message class for mediaType,
	 * 			or if a class exists but does not have a constructor
	 * 			that takes a single IJSONLexan arugment,
	 * 			or if that c'tor throws an exception.
	 */
	public static AltoMsg_Base jsonToALTO(String mediaType,
									 IJSONLexan lexan,
									 List<Class<? extends AltoMsg_Base>> msgClasses)
			throws JSONException
	{
		String xtype = mediaType;
		if (!xtype.startsWith(AltoMsg_Base.MEDIA_TYPE_PREFIX)) {
			xtype = AltoMsg_Base.MEDIA_TYPE_PREFIX + xtype;
		}
		if (!xtype.endsWith(AltoMsg_Base.MEDIA_TYPE_SUFFIX)) {
			xtype = xtype + AltoMsg_Base.MEDIA_TYPE_SUFFIX;
		}
		if (msgClasses == null) {
			msgClasses = g_altoMsgClasses;
		}
		if (msgClasses != null) {
			for (Class<? extends AltoMsg_Base>msgClass: msgClasses) {
				try {
					Field mediaTypeField = msgClass.getField(MEDIA_TYPE_FIELD);
					String classMediaType = (String) mediaTypeField.get(null);
					if (xtype.equals(classMediaType)) {
						long estSize = lexan.estimatedSize();
						if (estSize > 0) {
							long limit = classInputSizeLimit(msgClass);
							if (limit > 0 && estSize > limit) {
								continue;
							}
						}
						Constructor[] ctors = msgClass.getConstructors();
						if (ctors != null) {
							for (Constructor ctor : ctors) {
								Class<?>[] params = ctor.getParameterTypes();
								if (params != null
										&& params.length == 1
										&& params[0].isInstance(lexan)) {
									try {
										return (AltoMsg_Base) ctor.newInstance(lexan);
									} catch (InvocationTargetException e) {
										// If the c'tor throws a JSON exception,
										// pass that up to the client, because
										// that means something's wrong with the JSON.
										// Ignore other errors.
										Throwable cause = e.getTargetException();
										if (cause instanceof JSONException) {
											throw (JSONException)cause;
										}
									}
								}
							}
						}
					}
				} catch (JSONException e) {
					// A JSONException in the c'tor is fatal; pass that up to the client.
					throw e;
				} catch (Exception e) {
					// Other errors are non-fatal; ignore them & skip this class.
					// System.out.println("XXX: error on " + msgClass.getName() + ": " + e.toString());
				}
			}
		}
		throw new JSONException("No ALTO constructor for media type '" + mediaType + "'");
	}
	
	/**
	 * Return a list of all ALTO message classes.
	 * The list starts with the classes in "preferredClasses".
	 * The other classes follow. 
	 * @param preferredClasses ALTO message classes to place at the head of the list.
	 * 		Ignored if null.
	 * @return All known ALTO message classes.
	 */
	public static List<Class<? extends AltoMsg_Base>> makeClassList(List<Class<? extends AltoMsg_Base>> preferredClasses)
	{
		List<Class<? extends AltoMsg_Base>> newClassList = new ArrayList<Class<? extends AltoMsg_Base>>();
		if (preferredClasses != null) {
			newClassList.addAll(preferredClasses);
		}
		for (Class<? extends AltoMsg_Base> msgClass: g_altoMsgClasses) {
			if (!newClassList.contains(msgClass)) {
				newClassList.add(msgClass);
			}
		}
		return newClassList;
	}

	/**
	 * Return a list of all ALTO message classes.
	 * The list starts with the class "preferredClass".
	 * The other classes follow. 
	 * @param preferredClass An ALTO message classes to place at the head of the list.
	 * 		Ignored if null.
	 * @return All known ALTO message classes.
	 */
	public static List<Class<? extends AltoMsg_Base>> makeClassList(Class<? extends AltoMsg_Base> preferredClass)
	{
		List<Class<? extends AltoMsg_Base>> list = new ArrayList<Class<? extends AltoMsg_Base>>();
		if (preferredClass != null) {
			list.add(preferredClass);
		}
		return makeClassList(list);
	}
	
	/**
	 * Validate a json object using the validator defined by an ALTO message class.
	 * Note this does not create an instance of the ALTO message class.
	 * @param mediaType The media type associated with the josn message.
	 * @param json A JSON object, presumably representing an ALTO message.
	 * @return A list of validation errors, or null if the message is valid.
	 * 		We also return null if the ALTO message class for mediaType
	 * 		does not provide a validator.
	 */
	public static List<String> validate(String mediaType, JSONValue_Object json)
	{
		String xtype = mediaType;
		if (!xtype.startsWith(AltoMsg_Base.MEDIA_TYPE_PREFIX)) {
			xtype = AltoMsg_Base.MEDIA_TYPE_PREFIX + xtype;
		}
		if (!xtype.endsWith(AltoMsg_Base.MEDIA_TYPE_SUFFIX)) {
			xtype = xtype + AltoMsg_Base.MEDIA_TYPE_SUFFIX;
		}
		boolean foundMediaType = false;
		for (Class<? extends AltoMsg_Base>msgClass: g_altoMsgClasses) {
			try {
				Field mediaTypeField = msgClass.getField(MEDIA_TYPE_FIELD);
				String classMediaType = (String) mediaTypeField.get(null);
				if (xtype.equals(classMediaType)) {
					foundMediaType = true;
					Method method = msgClass.getMethod(VALIDATE_METHOD, JSONValue_Object.class);
					Object obj = method.invoke(null, json);
					if (obj == null) {
						return null;
					} else if (obj instanceof List) {
						return StringUtils.makeStringList((List)obj);
					}
				}
			} catch (Exception e) {
				// Other errors are non-fatal; ignore them & skip this class.
			}
		}
		if (foundMediaType) {
			return null;
		} else {
			List<String> ret = new ArrayList<String>(1);
			ret.add("Unknown media type '" + mediaType + "'");
			return ret;
		}
	}

	/**
	 * Return the input size limit for an ALTO message class,
	 * or -1 if not specified. Invokes the class's static inputSizeLimit() method.
	 * @param clazz An ALTO message class.
	 * @return The input size limit for class "clazz", or -1 if not specified.
	 */
	private static long classInputSizeLimit(Class<? extends AltoMsg_Base> clazz)
	{
		try {
			Method method = clazz.getMethod(INPUT_SIZE_LIMIT_METHOD);
			Object obj = method.invoke(null);
			if (obj != null && obj instanceof Number) {
				return ((Number)obj).longValue();
			}
		} catch (Exception e) {
			// Ignore & fall thru
		}
		return -1;
	}
	
	/**
	 * Return the name of the ALTO service for a media type.
	 * @param mediaType The media type of a server response message.
	 * @param accepts If not null, the media type of a client request message.
	 * @return If accepts is not null, the name of the POST-mode ALTO service
	 * 		that expects a request of type "accepts"
	 * 		and returns a response of type "mediaType."
	 * 		If accepts is null, return the name of the GET-mode ALTO service
	 * 		that returns a response of type "mediaType."
	 * 		Return null if no ALTO service corresponds to these message types.	
	 */
	public static String getServiceName(String mediaType, String accepts)
	{
		if (mediaType == null) {
			return null;
		}
		for (Class<? extends AltoMsg_Base>msgClass: g_altoMsgClasses) {
			try {
				Field mediaTypeField = msgClass.getField(MEDIA_TYPE_FIELD);
				String classMediaType = (String) mediaTypeField.get(null);
				if (mediaType.equals(classMediaType)) {
					Method method = msgClass.getMethod(GET_SERVICE_NAME_METHOD, String.class);
					Object obj = method.invoke(null, accepts);
					if (obj != null && obj instanceof String) {
						return (String)obj;
					}
				}
			} catch (Exception e) {
				// Other errors are non-fatal; ignore them & skip this class.
			}
		}
		return null;
	}
		
	/**
	 * For testing, parse, print and/or validate ALTO json messages.
	 * The arguments are file names and flags.
	 * The first line of each file is an ALTO media type,
	 * and the rest of the file is an ALTO message of that type.
	 * The default is to read each file, parse the json, and then call
	 * {@link #validate(String,JSONValue_Object)} on the JSON object.
	 * If "-p" flag is encountered, for subsequent files, 
	 * call {@link #jsonToALTO(String,IJSONLexan)} to create
	 * the appropriate ALTO message object for that media type,
	 * and then print the message object.
	 * @param args Command line arguments.
	 */
	public static void main(String[] args)
	{
		boolean print = false;
		for (String f:args) {
			if (f.equals("-p")) {
				print = true;
				continue;
			}
			System.out.println();
			System.out.println(f + ":");
			LineNumberReader rdr;
			try {
				rdr = new LineNumberReader(new FileReader(f));
			} catch (FileNotFoundException e1) {
				System.out.println("  Cannot open file");
				continue;
			}
			String mediaType;
			try {
				mediaType = rdr.readLine().trim();
			} catch (IOException e1) {
				System.out.println("  Error reading file: " + e1.toString());
				continue;
			}
			if (print) {
				AltoMsg_Base altoMsg = null;
				try {
					altoMsg = jsonToALTO(mediaType, new JSONLexan(rdr, f, -1));
					System.out.println("Msg class: "
							+ altoMsg.getClass().getName());
					System.out.println(altoMsg.toString());
				} catch (Exception e) {
					e.printStackTrace(System.out);
					continue;
				}
			} else {
				try {
					JSONValue_Object json = JSONParser.parseObject(new JSONLexan(rdr, f, -1), false);
					List<String> errors = validate(mediaType, json);
					if (errors == null) {
						System.out.println("  Passed validation");
					} else {
						System.out.println("  " + errors.size() + " validation errors:");
						for (String error: errors) {
							System.out.println("   " + error);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(System.out);
					continue;
				}
			}
			try {
				rdr.close();
			} catch (IOException e) {
				// Ignore
			}

		}
	}
}
