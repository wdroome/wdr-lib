package com.wdroome.altomsgs;

import java.io.Reader;
import java.util.List;

import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONException;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONValueTypeException;
import com.wdroome.json.JSONValue_Object;
import com.wdroome.json.validate.JSONValidate;
import com.wdroome.json.validate.JSONValidate_Array;
import com.wdroome.json.validate.JSONValidate_Number;
import com.wdroome.json.validate.JSONValidate_String;
import com.wdroome.json.validate.JSONValidate_Object;
import com.wdroome.json.validate.JSONValidate_Object.FieldSpec;
import com.wdroome.json.validate.JSONValidate_Object.RegexKey;
import com.wdroome.json.validate.JSONValidate_Object.SimpleKey;

/**
 * Represent an error response from an ALTO server.
 * @author wdr
 */
public class AltoResp_Error extends AltoResp_Base
{
	public static final String MEDIA_TYPE = MEDIA_TYPE_PREFIX + "error" + MEDIA_TYPE_SUFFIX;

	public static final String ERROR_CODE_SYNTAX				= "E_SYNTAX";
	public static final String ERROR_CODE_MISSING_FIELD			= "E_MISSING_FIELD";
	public static final String ERROR_CODE_INVALID_FIELD_TYPE	= "E_INVALID_FIELD_TYPE";
	public static final String ERROR_CODE_INVALID_FIELD_VALUE	= "E_INVALID_FIELD_VALUE";
	
	public static final String[] ERROR_CODE_LIST = new String[] {
			ERROR_CODE_SYNTAX,
			ERROR_CODE_MISSING_FIELD,
			ERROR_CODE_INVALID_FIELD_TYPE,
			ERROR_CODE_INVALID_FIELD_VALUE,
		};
	
	private static final String FN_CODE = "code";
	private static final String FN_FIELD = "field";
	private static final String FN_VALUE = "value";
	private static final String FN_SYNTAX_ERROR = "syntax-error";

	/**
	 * Create a new error response for a given code, with an optional additional field.
	 * @param code The ALTO Error Code.
	 * @param aux1 If code is SYNTAX, a description of the error.
	 * 		Otherwise the name of the invalid field. In either case, may be null.
	 */
	public AltoResp_Error(String code, String aux1)
	{
		this(code, aux1, null);
	}
	
	/**
	 * Create a new error response for a given code, with two optional additional fields.
	 * @param code The ALTO Error Code.
	 * @param aux1 If code is SYNTAX, a description of the error.
	 * 		Otherwise the name of the invalid field. In either case, may be null.
	 * @param aux2 If code is INVALID_FIELD_VALUE, the invalid field, or null.
	 */
	public AltoResp_Error(String code, String aux1, String aux2)
	{
		super();
		setMetaValue(FN_CODE, code);
		if (code.equals(ERROR_CODE_SYNTAX)) {
			if (aux1 != null && !aux1.equals(""))
				setMetaValue(FN_SYNTAX_ERROR, aux1);
		} else {
			if (aux1 != null && !aux1.equals("")) {
				setMetaValue(FN_FIELD, aux1);
			}
			if (code.equals(ERROR_CODE_INVALID_FIELD_VALUE)) {
				if (aux2 != null && !aux2.equals("")) {
					setMetaValue(FN_VALUE, aux2);
				}
			}
		}
	}
	
	/**
	 * Create an object from a JSON parser.
	 * Used to decode a received message.
	 * @param lexan The JSON input parser.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoResp_Error(IJSONLexan lexan)
		throws JSONParseException, JSONValueTypeException
	{
		super(lexan);
	}
	
	/**
	 * Create an object from a JSON string.
	 * Used to decode a received message.
	 * @param jsonSrc The encoded JSON message.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoResp_Error(String jsonSrc)
		throws JSONParseException, JSONValueTypeException
	{
		this(new JSONLexan(jsonSrc));
	}
	
	/**
	 * Create an object from a JSON Reader.
	 * Used to decode a received message.
	 * @param jsonSrc The encoded JSON message.
	 * @throws JSONParseException If jsonSrc isn't a valid JSON string.
	 * @throws JSONValueTypeException If the input isn't a JSON Object dictionary.
	 */
	public AltoResp_Error(Reader jsonSrc)
			throws JSONParseException, JSONValueTypeException
	{
		this(new JSONLexan(jsonSrc));
	}
	
	@Override
	public String getMediaType()
	{
		return MEDIA_TYPE;
	}
	
	/**
	 * Return the "code" string, or "UNKNOWN".
	 * @return The "code" string, or "UNKNOWN".
	 */
	public String getCode()
	{
		String code = getMetaString(FN_CODE);
		if (code == null || code.equals(""))
			code = "UNKNOWN";
		return code;
	}
	
	/**
	 * Return the "field" name, or null.
	 * @return The "field" name, or null.
	 */
	public String getField()
	{
		String field = getMetaString(FN_FIELD);
		if (field != null && field.equals(""))
			field = null;
		return field;
	}
	
	/**
	 * Return the "value" of the invalid field, or null.
	 * @return The "value" of the invalid field, or null.
	 */
	public String getValue()
	{
		String value = getMetaString(FN_VALUE);
		if (value != null && value.equals(""))
			value = null;
		return value;
	}
	
	/**
	 * Return the syntax error message, or null.
	 * @return The syntax error message, or null.
	 */
	public String getSyntaxError()
	{
		String syntaxError = getMetaString(FN_SYNTAX_ERROR);
		if (syntaxError != null && syntaxError.equals(""))
			syntaxError = null;
		return syntaxError;		
	}
	
	/** FieldSpecs to validate Error messages. */
	private static final FieldSpec[] ERROR_FIELD_SPECS = new FieldSpec[] {
		
			// meta:
		new FieldSpec(
				new SimpleKey(AltoResp_Base.FN_META, true),
				new JSONValidate_Object(new FieldSpec[] {
					new FieldSpec(
						new SimpleKey(FN_CODE, true),
						new JSONValidate_String(ERROR_CODE_LIST, false,
								"\"%s\" is not a valid ALTO error code")),
					new FieldSpec(
						new SimpleKey(FN_SYNTAX_ERROR, false),
						JSONValidate_String.STRING),
					new FieldSpec(
						new SimpleKey(FN_FIELD, false),
						JSONValidate_String.STRING),
					new FieldSpec(
						new SimpleKey(FN_VALUE, false),
						JSONValidate_String.STRING),
				})
			)
		};

	/**
	 * Return a new validator for Error messages.
	 */
	@Override
	protected JSONValidate getValidator()
	{
		return new JSONValidate_Object(ERROR_FIELD_SPECS);
	}
	
	/**
	 * Validate a JSON object as an Error message.
	 * @param json The JSON Object to validate.
	 * @return A list of errors, or null if the message passed validation.
	 */
	public static List<String> validate(JSONValue_Object json)
	{
		return validate(json, new JSONValidate_Object(ERROR_FIELD_SPECS));
	}
}
