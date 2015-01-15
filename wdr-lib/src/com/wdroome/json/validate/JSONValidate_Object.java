package com.wdroome.json.validate;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.wdroome.json.*;

/**
 * Validate a JSON Object Dictionary.
 * @author wdr
 */
public class JSONValidate_Object extends JSONValidate
{
	/**
	 * Specify a valid field in this object.
	 * It consists of a test against the field name (the key)
	 * and an optional validation specification for the value.
	 * If a key passes the name test, we check the value
	 * against the associated validation specification.
	 */
	public static class FieldSpec
	{
		// The key specification.
		private final IKeyTest m_keyTest;
				
		// The validation criteria for this field's value (may be null).
		public final JSONValidate m_valueSpec;
		
		/**
		 * Specify a field this dictionary.
		 * @param keyTest Test the key. May not be null.
		 * @param valueTest The validation criteria for this field, or null.
		 */
		public FieldSpec(IKeyTest keyTest, JSONValidate valueTest)
		{
			m_keyTest = keyTest;
			m_valueSpec = valueTest;
		}
	}
	
	/**
	 * Interface for key tests.
	 */
	public static interface IKeyTest
	{
		/**
		 * Test if the key matches.
		 * @param key A key in a JSON Object.
		 * @return True iff "key" matches this test.
		 */
		public boolean keyMatches(String key);
		
		/**
		 * Return the names of any required keys.
		 * @return The names of any required keys.
		 * 		May return null or an empty list.
		 */
		public List<String> requiredKeys();
	}
	
	/**
	 * A fixed key name test.
	 */
	public static class SimpleKey implements IKeyTest
	{
		private final String m_name;
		private final List<String> m_requiredKeys;
		
		/**
		 * Create a new simple key test.
		 * @param name The key.
		 * @param required True iff this key is required.
		 */
		public SimpleKey(String name, boolean required)
		{
			m_name = name;
			if (required) {
				m_requiredKeys = new ArrayList<String>();
				m_requiredKeys.add(name);
			} else {
				m_requiredKeys = null;
			}
		}
		
		/** @see IKeyTest#keyMatches(String) */
		@Override
		public boolean keyMatches(String key)
		{
			return key.equals(m_name);
		}

		/** @see IKeyTest#requiredKeys() */
		@Override
		public List<String> requiredKeys()
		{
			return m_requiredKeys;
		}
	}
	
	/**
	 * A regular-expression test on the key name.
	 */
	public static class RegexKey implements IKeyTest
	{
		private final Pattern m_pattern;
		
		/**
		 * Create a key test using a regular expression to specify the key names.
		 * @param regex The regular expression
		 * @throws PatternSyntaxException
		 * 			If regex isn't a valid regular expression.
		 */
		public RegexKey(String regex) throws PatternSyntaxException
		{
			m_pattern = Pattern.compile(regex);
		}
		
		/** @see IKeyTest#keyMatches(String) */
		public boolean keyMatches(String key)
		{
			return m_pattern.matcher(key).matches();
		}
		
		/** @see IKeyTest#requiredKeys() */
		public List<String> requiredKeys()
		{
			return null;
		}
	}
	
	/**
	 * Any-field test.
	 * Use this at the end of a FieldSpec array to allow
	 * fields that don't match the other FieldSpec tests.
	 */
	public static final FieldSpec ANY_FIELD = new FieldSpec(new RegexKey(".*"), null);
	
	private final FieldSpec[] m_fieldSpecs;
	private final String[] m_requiredFields;

	/**
	 * Create a validation specification for a JSON Object Dictionary.
	 * @param fieldSpecs The fields allowed in this dictionary.
	 * 		If null, allow any field.
	 */
	public JSONValidate_Object(FieldSpec[] fieldSpecs)
	{
		this(fieldSpecs, false);
	}
	
	/**
	 * Create a validation specification for a JSON Object Dictionary.
	 * @param fieldSpecs The fields allowed in this dictionary.
	 * 		If null, allow any field.
	 * @param nullAllowed If true, allow null instead of an object dictionary.
	 */
	public JSONValidate_Object(FieldSpec[] fieldSpecs, boolean nullAllowed)
	{
		super(JSONValue_Object.class, nullAllowed);
		if (fieldSpecs != null) {
			m_fieldSpecs = fieldSpecs;
		} else {
			m_fieldSpecs = new FieldSpec[] {new FieldSpec(new RegexKey(".*"),null)};
		}
		HashSet<String> requiredFields = new HashSet<String>();
		for (FieldSpec fs: fieldSpecs) {
			List<String> rf = fs.m_keyTest.requiredKeys();
			if (rf != null) {
				requiredFields.addAll(rf);
			}
		}
		int nrequired = requiredFields.size();
		if (nrequired > 0) {
			m_requiredFields = requiredFields.toArray(new String[nrequired]);
		} else {
			m_requiredFields = null;
		}
	}
	
	/**
	 * Validate a JSON value.
	 * See {@link JSONValidate#validate(JSONValue,String)}.
	 */
	@Override
	public boolean validate(JSONValue value, String path) throws JSONValidationException
	{
		if (!super.validate(value, path)) {
			return false;
		}
		boolean valid = true;
		// super() verifies that next instanceof test is always true,
		// but java doesn't know that.
		if (value instanceof JSONValue_Object) {
			JSONValue_Object parent = (JSONValue_Object)value;
			
			// For each key-value pair in dictionary,
			// find the first FieldSpec that matches the key,
			// and then validate the value.
			// Flag as unknown any fields that don't match a FieldSpec.
			for (Map.Entry<String, JSONValue> entry: parent.entrySet()) {
				String name = entry.getKey();
				boolean knownField = false;
				for (FieldSpec fieldSpec: m_fieldSpecs) {
					if (fieldSpec.m_keyTest.keyMatches(name)) {
						knownField = true;
						if (fieldSpec.m_valueSpec != null
								&& !fieldSpec.m_valueSpec.validate(entry.getValue(), newPath(path, name))) {
							valid = false;
						}
						break;
					}
				}
				if (!knownField) {
					handleValidationError("Unknown field \"" + name	+ "\"" + atPath(path));
					valid = false;
				}
			}
			
			// Check that required fields exist.
			if (m_requiredFields != null) {
				for (String name : m_requiredFields) {
					if (parent.get(name) == null) {
						handleValidationError("Required field \"" + name
								+ "\" missing" + atPath(path));
						valid = false;
					}
				}
			}
		}
		return valid;
	}
	
	/**
	 * Collect errors in the specified list.
	 * This method invokes collectErrors()
	 * on all JSONValidate objects in the FieldSpec's
	 * passed to the constructor.
	 * See {@link JSONValidate#collectErrors(List)}.
	 */
	@Override
	public void collectErrors(List<String> errors)
	{
		super.collectErrors(errors);
		for (FieldSpec fs: m_fieldSpecs) {
			if (fs.m_valueSpec != null) {
				fs.m_valueSpec.collectErrors(errors);
			}
		}
	}
}
