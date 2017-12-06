/*******************************************************************************
 * Copyright (c) 2009-2016 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;

import com.blackrook.commons.Common;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.TypeProfile;
import com.blackrook.commons.TypeProfile.MethodSignature;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.list.List;
import com.blackrook.lang.json.annotation.JSONIgnore;
import com.blackrook.lang.json.annotation.JSONType;

/**
 * JSON Object abstraction.
 * @author Matthew Tropiano
 * @since 2.3.0
 */
public class JSONObject
{
	/** Empty member list. */
	private static final String[] EMPTY_MEMBER_LIST = new String[0];
	
	/** Not an array. */
	private static final int NOT_ARRAY = -1;
	/** Default converter. */
	private static final JSONDefaultConverter DEFAULT_CONVERTER = new JSONDefaultConverter();

	/** Undefined member. All instances of UNDEFINED are this one. */
	public static final JSONObject UNDEFINED = new JSONObject(Type.UNDEFINED, null, NOT_ARRAY);
	/** Null member. All instances of NULL are this one. */
	public static final JSONObject NULL = new JSONObject(Type.OBJECT, null, NOT_ARRAY);
	
	/** Converter hash. */
	private static final HashMap<Class<?>, JSONConverter<?>> 
		REGISTERED_CONVERTERS = new HashMap<Class<?>, JSONConverter<?>>();
	
	/**
	 * JavaScript type of a JSONObject.
	 */
	public static enum Type
	{
		/** Undefined type for undefined. */ 
		UNDEFINED,
		/** Object type for objects, or null. Stored as HashMap&lt;String, JSONObject&gt;, or null. */
		OBJECT,
		/** Numeric type. */
		NUMBER,
		/** String type. */
		STRING,
		/** Boolean type. */
		BOOLEAN;
	}

	/** JavaScript type. */
	private Type type;
	/** Object internal value. */
	private Object value;
	/** Array length. */
	private int length;
	
	/**
	 * Gets a converter for a type.
	 * @param <E> the class type.
	 * @param clazz the class to get the converter for.
	 * @return a converter to use for JSON conversion.
	 */
	@SuppressWarnings("unchecked")
	public static <E> JSONConverter<E> getConverter(Class<E> clazz)
	{
		JSONConverter<E> out = null;
		if ((out = (JSONConverter<E>)REGISTERED_CONVERTERS.get(clazz)) == null)
		{
			JSONType ajsonType = clazz.getAnnotation(JSONType.class);
			if (ajsonType == null)
				return null;
			
			synchronized (REGISTERED_CONVERTERS)
			{
				if ((out = (JSONConverter<E>)REGISTERED_CONVERTERS.get(clazz)) == null)
				{
					try {
						out = (JSONConverter<E>)ajsonType.converter().newInstance();
						setConverter(clazz, out);
					} catch (Throwable e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		else
			out = (JSONConverter<E>)REGISTERED_CONVERTERS.get(clazz);
		
		return out;
	}
	
	/**
	 * Sets a converter for a type.
	 * @param <E> the class type.
	 * @param clazz the class to get the converter for.
	 * @param converter the converter to use for JSON conversion.
	 */
	public static <E extends Object> void setConverter(Class<E> clazz, JSONConverter<E> converter)
	{
		REGISTERED_CONVERTERS.put(clazz, converter);
	}
	
	/**
	 * Creates a new JSON object.
	 * Objects may have an internal converter.
	 * @param <T> the object type used for finding a converter.
	 * @param object the object to encapsulate.
	 * @return the JSONObject representing the input object.
	 */
	public static <T> JSONObject create(T object)
	{
		if (object == null)
			return NULL;
		else if (Reflect.isArray(object))
		{
			int len = Array.getLength(object);
			JSONObject out = createEmptyArray();
			for (int i = 0; i < len; i++)
				out.append(JSONObject.create(Array.get(object, i)));
			return out;
		}
		else if (object instanceof Boolean)
			return new JSONObject(Type.BOOLEAN, object, NOT_ARRAY);
		else if (object instanceof Number)
			return new JSONObject(Type.NUMBER, object, NOT_ARRAY);
		else if (object instanceof String)
			return new JSONObject(Type.STRING, object, NOT_ARRAY);

		@SuppressWarnings("unchecked")
		Class<T> clz = (Class<T>)object.getClass();
		JSONConverter<T> converter = getConverter(clz);
		if (converter != null)
			return converter.getJSONObject(object);
		else
			return DEFAULT_CONVERTER.getJSONObject(object);
	}
	
	/**
	 * Creates a JSONObject that represents an empty object type.
	 * @return a JSONObject representing a blank object.
	 */
	public static JSONObject createEmptyObject()
	{
		return new JSONObject(Type.OBJECT, new HashMap<String, JSONObject>(2), NOT_ARRAY);
	}
	
	/**
	 * Creates a JSONObject that represents an empty array type.
	 * @return a JSONObject representing an empty array.
	 */
	public static JSONObject createEmptyArray()
	{
		return new JSONObject(Type.OBJECT, new List<JSONObject>(3), 0);
	}
	
	/**
	 * JSON object constructor for generating
	 * @param type the internal JSON type of this object.
	 * @param value the object to encapsulate.
	 * @param arrayLength the length of the array, if an array.
	 */
	JSONObject(Type type, Object value, int arrayLength)
	{
		this.type = type;
		this.value = value;
		this.length = arrayLength;
	}

	/**
	 * Gets this object's JavaScript type.
	 * @return the object type.
	 */
	public Type getType()
	{
		return type;
	}
	
	/**
	 * Checks if this is Object-typed.
	 * @return true if so, false if not.
	 */
	public boolean isObject()
	{
		return type == Type.OBJECT;
	}
	
	/**
	 * Checks if this is Object-typed and is an array.
	 * @return true if so, false if not.
	 */
	public boolean isArray()
	{
		return type == Type.OBJECT && length > NOT_ARRAY;
	}

	/**
	 * Gets length of this array-typed object.
	 * @return the length of this array if this is an array, or -1 if not an array.
	 */
	public int length()
	{
		return length;
	}

	/**
	 * Gets the amount of members in this object, if this is an object.
	 * If this is an array, this returns its length.
	 * @return the member count, or 0 if not an object.
	 * @see #length()
	 */
	public int getMemberCount()
	{
		return isArray() ? length() : (isObject() ? getMap().size() : 0);
	}

	/** 
	 * Returns the underlying value as a map of string to object.
	 * @return the map.   
	 */
	@SuppressWarnings("unchecked")
	protected HashMap<String, JSONObject> getMap()
	{
		return (HashMap<String, JSONObject>)value;
	}
	
	/** 
	 * Returns the underlying value as a list of objects. 
	 * @return the list.   
	 */
	@SuppressWarnings("unchecked")
	protected List<JSONObject> getList()
	{
		return (List<JSONObject>)value;
	}
	
	/**
	 * Returns an array of member names on this object, if
	 * it is Object typed. This may return an array of index numbers,
	 * if this is an array under the covers.
	 * @return an array of member names, suitable for use with {@link #get(String)}, or an empty array,
	 * if this does not represent an object.
	 */
	public String[] getMemberNames()
	{
		String[] out = null;
		if (isArray())
		{
			out = new String[length];
			for (int i = 0; i < length; i++)
				out[i] = String.valueOf(i);
		}
		else if (isObject())
		{
			out = new String[getMemberCount()];
			Iterator<String> it = getMap().keyIterator();
			int i = 0;
			while (it.hasNext())
				out[i++] = it.next();
		}
		else
			out = EMPTY_MEMBER_LIST;
		return out;
	}
	
	/**
	 * Checks if this is a null object.
	 * @return true if so, false if not.
	 */
	public boolean isNull()
	{
		return type == Type.OBJECT && value == null;
	}
	
	/**
	 * Checks if this is UNDEFINED.
	 * @return true if so, false if not.
	 */
	public boolean isUndefined()
	{
		return type == Type.UNDEFINED;
	}
	
	/**
	 * Gets this object's encapsulated value. Can be null.
	 * @return the underlying value.
	 */
	public Object getValue()
	{
		return value;
	}
	
	/**
	 * Returns true if this is an object AND contains a member of a particular name.
	 * @param memberName the name of the member to check.
	 * @return true if it exists on the object, false if not or not an object.
	 */
	public boolean hasMember(String memberName)
	{
		if (isArray())
		{
			try {
				int i = Integer.parseInt(memberName);
				return i >= 0 && i < length; 
			} catch (NumberFormatException e) {
				return false;
			}
		}
		else if (isObject())
			return getMap().containsKey(memberName);
		else
			return false;
	}
	
	/**
	 * Gets an object member of this JSONObject by name.
	 * NOTE: Arrays are objects - their member names are the index position.
	 * @param memberName the name of the member to retrieve.
	 * @return a JSONObject corresponding to this member, or {@link JSONObject#UNDEFINED} if no
	 * member by that name or this isn't a JSON Object. 
	 */
	public JSONObject get(String memberName)
	{
		if (hasMember(memberName))
			return getMap().get(memberName);
		else
			return UNDEFINED;
	}
	
	/**
	 * Gets an object member of this JSONObject by index, if this is an array.
	 * @param index the index to retrieve.
	 * @return a JSONObject corresponding to this index, or {@link JSONObject#UNDEFINED} if no
	 * such index or this isn't a JSON Object or the index is out of bounds. 
	 */
	public JSONObject get(int index)
	{
		if (value == null)
			return UNDEFINED;
		else if (isArray())
		{
			if (index < 0 || index >= length)
				return UNDEFINED;
			else
				return getList().getByIndex(index);
		}
		return get(String.valueOf(index));
	}
	
	/**
	 * Returns the value of this JSON Object as a byte.
	 * @return the byte value, or 0 if this cannot be reasonably converted to a byte.
	 */
	public byte getByte()
	{
		if (type == Type.UNDEFINED)
			return 0;
		else if (type == Type.OBJECT)
			return 0;
		else if (value instanceof Boolean)
			return ((Boolean)value) ? (byte)1 : 0;
		else if (value instanceof Number)
			return ((Number)value).byteValue();
		else if (value instanceof String)
			return Common.parseByte((String)value, (byte)0);
		else
			return 0;
	}
	
	/**
	 * Returns the value of this JSON Object as a short.
	 * @return the short value, or 0 if this cannot be reasonably converted to a short.
	 */
	public short getShort()
	{
		if (type == Type.UNDEFINED)
			return 0;
		else if (type == Type.OBJECT)
			return 0;
		else if (value instanceof Boolean)
			return ((Boolean)value) ? (short)1 : 0;
		else if (value instanceof Number)
			return ((Number)value).shortValue();
		else if (value instanceof String)
			return Common.parseShort((String)value, (short)0);
		else
			return 0;
	}
	
	/**
	 * Returns the value of this JSON Object as an integer.
	 * @return the integer value, or 0 if this cannot be reasonably converted to an integer.
	 */
	public int getInt()
	{
		if (type == Type.UNDEFINED)
			return 0;
		else if (type == Type.OBJECT)
			return 0;
		else if (value instanceof Boolean)
			return ((Boolean)value) ? 1 : 0;
		else if (value instanceof Number)
			return ((Number)value).intValue();
		else if (value instanceof String)
			return Common.parseInt((String)value, 0);
		else
			return 0;
	}
	
	/**
	 * Returns the value of this JSON Object as a float.
	 * @return the float value, or 0f if this cannot be reasonably converted to a float.
	 */
	public float getFloat()
	{
		if (type == Type.UNDEFINED)
			return 0f;
		else if (type == Type.OBJECT)
			return 0f;
		else if (value instanceof Boolean)
			return ((Boolean)value) ? 1f : 0f;
		else if (value instanceof Number)
			return ((Number)value).floatValue();
		else if (value instanceof String)
			return Common.parseFloat((String)value, 0f);
		else
			return 0f;
	}
	
	/**
	 * Returns the value of this JSON Object as a long.
	 * @return the long value, or 0L if this cannot be reasonably converted to a long.
	 */
	public long getLong()
	{
		if (type == Type.UNDEFINED)
			return 0L;
		else if (type == Type.OBJECT)
			return 0L;
		else if (value instanceof Boolean)
			return ((Boolean)value) ? 1L : 0L;
		else if (value instanceof Number)
			return ((Number)value).longValue();
		else if (value instanceof String)
			return Common.parseLong((String)value, 0L);
		else
			return 0L;
	}
	
	/**
	 * Returns the value of this JSON Object as a double.
	 * @return the double value, or 0.0 if this cannot be reasonably converted to a long.
	 */
	public double getDouble()
	{
		if (type == Type.UNDEFINED)
			return 0.0;
		else if (type == Type.OBJECT)
			return 0.0;
		else if (value instanceof Boolean)
			return ((Boolean)value) ? 1.0 : 0.0;
		else if (value instanceof Number)
			return ((Number)value).doubleValue();
		else if (value instanceof String)
			return Common.parseDouble((String)value, 0.0);
		else
			return 0.0;
	}
	
	/**
	 * Returns the value of this JSON Object as an boolean.
	 * Non-null Objects are always true, <i>undefined</i> is always false.
	 * @return the boolean value, or false if this cannot be reasonably converted to a boolean.
	 */
	public boolean getBoolean()
	{
		if (type == Type.UNDEFINED)
			return false;
		else if (type == Type.OBJECT)
			return !isNull();
		else if (value instanceof Boolean)
			return ((Boolean)value);
		else if (value instanceof Number)
			return ((Number)value).doubleValue() != 0.0;
		else if (value instanceof String)
			return "true".equals((String)value);
		else
			return false;
	}
	
	/**
	 * Returns the value of this JSON Object as a String.
	 * If the value of this object is undefined or null, null is returned.
	 * If this is an object, <code>Object</code> is returned.
	 * @return the string value, or false if this cannot be reasonably converted to a boolean.
	 */
	public String getString()
	{
		if (isUndefined())
			return null;
		else if (isNull())
			return null;
		else if (isObject())
			return "Object";
		else if (isArray())
			return "Object";
		else if (value instanceof Boolean)
			return String.valueOf(value);
		else if (value instanceof Number)
			return String.valueOf(value);
		else if (value instanceof String)
			return String.valueOf(value);
		else
			return null;
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(getString());
	}
	
	/**
	 * Promotes this array to an object.
	 */
	private void promoteArrayToObject()
	{
		HashMap<String, JSONObject> newmap = new HashMap<String, JSONObject>();
		for (int i = 0; i < length; i++)
			newmap.put(String.valueOf(i), get(i));
		value = newmap;
		length = NOT_ARRAY;
	}

	/**
	 * Removes a member from this JSONObject, if this is an Object type.
	 * If this is not an object (or array), this causes an error.
	 * WARNING: Array types are promoted to objects whether or not this succeeds.
	 * @param name the name of the member to remove.
	 * @return true if the member was removed, false if not.
	 * @throws IllegalStateException if this JSONObject is not an Object type 
	 * ({@link #isObject()} is false) or is null ({@link #isNull()} is true) or is UNDEFINED ({@link #isUndefined()} is true).
	 */
	public boolean removeMember(String name)
	{
		if (isNull())
			throw new IllegalStateException("Object is null.");
		
		if (isUndefined())
			throw new IllegalStateException("Object is undefined.");
		
		if (!isObject())
			throw new IllegalStateException("This is not an Object type.");
		
		if (isArray())
			promoteArrayToObject();
		
		if (hasMember(name))
			return getMap().removeUsingKey(name) != null;
		else
			return false;
	}
	
	/**
	 * Adds a member to this JSONObject, if this is an Object type.
	 * If this is not an object, this causes an error.
	 * If this is an array type, it is promoted to an object.
	 * @param name the member name. whitespace is trimmed from this.
	 * @param object the object value of the member.
	 * @throws IllegalArgumentException if name is an empty string or just whitespace.
	 * @throws IllegalStateException if this JSONObject is not an Object type 
	 * ({@link #isObject()} is false) or is null ({@link #isNull()} is true) or is UNDEFINED ({@link #isUndefined()} is true).
	 */
	public void addMember(String name, JSONObject object)
	{
		if (Common.isEmpty(name))
			throw new IllegalArgumentException("Member name is empty, null, or whitespace.");

		if (isNull())
			throw new IllegalStateException("Object is null.");
		
		if (isUndefined())
			throw new IllegalStateException("Object is undefined.");
		
		if (!isObject())
			throw new IllegalStateException("This is not an Object type.");
		
		if (isArray())
			promoteArrayToObject();
		
		getMap().put(name, object);
	}
	
	/**
	 * Adds a member to this JSONObject, if this is an Object type.
	 * If this is not an object, this causes an error.
	 * If this is an array type, it is promoted to an object.
	 * @param name the member name. whitespace is trimmed from this.
	 * @param object the object value of the member.
	 * @throws IllegalArgumentException if name is an empty string or just whitespace.
	 * @throws IllegalStateException if this JSONObject is not an Object type 
	 * ({@link #isObject()} is false) or is null ({@link #isNull()} is true) or is UNDEFINED ({@link #isUndefined()} is true).
	 */
	public void addMember(String name, Object object)
	{
		addMember(name, JSONObject.create(object));
	}
	
	/**
	 * Appends a member to the end of this JSONObject, but only if this is an array. 
	 * @param object the object to add.
	 * @throws IllegalStateException if this JSONObject is not an Array type 
	 * ({@link #isArray()} is false) or is null ({@link #isNull()} is true) or is UNDEFINED ({@link #isUndefined()} is true).
	 * @see #isArray()
	 */
	public void append(JSONObject object)
	{
		if (isNull())
			throw new IllegalStateException("Object is null.");
		
		if (isUndefined())
			throw new IllegalStateException("Object is undefined.");
		
		if (!isArray())
			throw new IllegalStateException("This is not an array Object type.");
		
		getList().add(object);
		length = getList().size();
	}
	
	/**
	 * Removes a member from a specific index in this JSONObject, shifting the contents, 
	 * but only if this is an array. 
	 * @param index the index to remove.
	 * @return the JSONObject at that index or null if no object at that index.
	 * @see #isArray()
	 * @throws IllegalStateException if this JSONObject is not an Array type 
	 * ({@link #isArray()} is false) or is null ({@link #isNull()} is true) or is UNDEFINED ({@link #isUndefined()} is true).
	 */
	public JSONObject removeAt(int index)
	{
		if (isNull())
			throw new IllegalStateException("Object is null.");
		
		if (isUndefined())
			throw new IllegalStateException("Object is undefined.");
		
		if (!isArray())
			throw new IllegalStateException("This is not an array Object type.");
		
		JSONObject out = getList().removeIndex(index);
		length = getList().size();
		return out;
	}
	
	/**
	 * Removes a member from the beginning index of this JSONObject, shifting the contents, 
	 * but only if this is an array. 
	 * @return the JSONObject at index 0 or null if this is empty.
	 * @see #isArray()
	 * @throws IllegalStateException if this JSONObject is not an array type.
	 */
	public JSONObject pop()
	{
		if (isNull())
			throw new IllegalStateException("Object is null.");
		
		if (isUndefined())
			throw new IllegalStateException("Object is undefined.");
		
		if (!isArray())
			throw new IllegalStateException("This is not an array Object type.");
		
		return removeAt(0);
	}
	
	/**
	 * Adds a member to a specific index in this JSONObject, shifting the contents,
	 * but only if this is an array.
	 * @param index the index into the array. 
	 * @param object the object to add.
	 * @see #isArray()
	 * @throws IllegalStateException if this JSONObject is not an array type.
	 */
	public void addAt(int index, JSONObject object)
	{
		if (isNull())
			throw new IllegalStateException("Object is null.");
		
		if (isUndefined())
			throw new IllegalStateException("Object is undefined.");
		
		if (!isArray())
			throw new IllegalStateException("This is not an array Object type.");
		
		getList().add(index, object);
		length = getList().size();
	}
	
	/**
	 * Adds a member to the beginning of this JSONObject, shifting the contents, 
	 * but only if this is an array.
	 * @param object the object to add.
	 * @see #isArray()
	 * @throws IllegalStateException if this JSONObject is not an array type.
	 */
	public void push(JSONObject object)
	{
		addAt(0, object);
	}
	
	/**
	 * Creates a new instance of a class, populated with values from this object. 
	 * <p>
	 * This JSON object is applied via the target object's public fields
	 * and setter methods, if this is an object and the target class is not a primitive
	 * or autoboxed primitive.
	 * <p>
	 * For instance, if there is a member on this object called "color", its value
	 * will be applied via the public field "color" or the setter "setColor()". Public
	 * fields take precedence over setters.
	 * <p>
	 * If the input object is an array, then the contents of the indices are replaced,
	 * up to the length of the input array or this JSON array, whichever's shorter.
	 * @param <T> the return type.
	 * @param clazz the class to instantiate and apply.
	 * @return a new instance of this object, or null if this object's value is null (see {@link #isNull()}.
	 * @throws RuntimeException if the object cannot be created.
	 * @throws JSONConversionException if an error occurs during conversion/application.
	 */
	@SuppressWarnings("unchecked")
	public <T> T newObject(Class<T> clazz)
	{
		if (isNull())
			return null;
		
		if (Reflect.isArray(clazz))
		{
			Object obj = Array.newInstance(Reflect.getArrayType(clazz), length);
			return applyToObject(clazz.cast(obj));
		}
		
		if (isObject())
			return applyToObject(Reflect.create(clazz));
		
		if (clazz == Boolean.TYPE)
			return (T)new Boolean(getBoolean());
		else if (clazz == Boolean.class)
			return clazz.cast(getBoolean());
		else if (clazz == Byte.TYPE)
			return (T)new Byte(getByte());
		else if (clazz == Byte.class)
			return clazz.cast(getByte());
		else if (clazz == Short.TYPE)
			return (T)new Short(getShort());
		else if (clazz == Short.class)
			return clazz.cast(getShort());
		else if (clazz == Integer.TYPE)
			return (T)new Integer(getInt());
		else if (clazz == Integer.class)
			return clazz.cast(getInt());
		else if (clazz == Float.TYPE)
			return (T)new Float(getFloat());
		else if (clazz == Float.class)
			return clazz.cast(getFloat());
		else if (clazz == Long.TYPE)
			return (T)new Long(getLong());
		else if (clazz == Long.class)
			return clazz.cast(getLong());
		else if (clazz == Double.TYPE)
			return (T)new Double(getDouble());
		else if (clazz == Double.class)
			return clazz.cast(getDouble());
		
		return null;
	}
	
	/**
	 * Applies this object to an object bean / plain ol' Java object, or Array.
	 * <p>
	 * This JSON object is applied via the target object's public fields
	 * and setter methods, if an object.
	 * <p>
	 * For instance, if there is a member on this object called "color", its value
	 * will be applied via the public field "color" or the setter "setColor()". Public
	 * fields take precedence over setters.
	 * <p>
	 * If the input object is an array, then the contents of the indices are replaced,
	 * up to the length of the input array or this JSON array, whichever's shorter.
	 * @param <T> the return type.
	 * @param object the object to set the fields/indices of.
	 * @return the input object.
	 * @throws JSONConversionException if an error occurs during conversion/application.
	 */
	@SuppressWarnings("unchecked")
	public <T> T applyToObject(T object)
	{
		if (Reflect.isArray(object))
		{
			if (!isArray())
				throw new JSONConversionException("Target is array, but not this object.");
			
			Class<?> atype = Reflect.getArrayType(object);
			int len = Math.min(length, Array.getLength(object));
			for (int i = 0; i < len; i++)
				Array.set(object, i, createForType(String.format("this[%d]", i), get(i), atype));
			
			return object;
		}
		
		TypeProfile<T> profile = TypeProfile.getTypeProfile((Class<T>)object.getClass());

		for (String member : getMemberNames())
		{
			Field field = null; 
			MethodSignature setter = null;
			if ((field = profile.getPublicFields().get(member)) != null)
			{
				if (!field.isAnnotationPresent(JSONIgnore.class))
				{
					Class<?> type = field.getType();
					Reflect.setField(object, member, createForType(member, get(member), type));
				}
			}
			else if ((setter = profile.getSetterMethods().get(member)) != null)
			{
				if (!setter.getMethod().isAnnotationPresent(JSONIgnore.class))
				{
					Class<?> type = setter.getType();
					Method method = setter.getMethod();
					Reflect.invokeBlind(method, object, createForType(member, get(member), type));
				}
			}			
		}
		
		return object;
	}

	// Creates an object for application later.
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> T createForType(String memberName, JSONObject jsonObject, Class<T> type)
	{
		Type jsonType = jsonObject.getType();
		
		switch (jsonType)
		{
			case BOOLEAN:
			{
				if (type == Boolean.TYPE)
					return (T)new Boolean(jsonObject.getBoolean());
				else if (type == Boolean.class)
					return type.cast(jsonObject.getBoolean());
				else if (type == Object.class)
					return type.cast(jsonObject.getBoolean());
				else
					throw new JSONConversionException("Member "+memberName+" is boolean typed; target is not boolean or Boolean.");
			}
				
			case NUMBER:
			{
				if (type == Boolean.TYPE)
					return (T)new Boolean(jsonObject.getBoolean());
				else if (type == Boolean.class)
					return type.cast(jsonObject.getBoolean());
				else if (type == Byte.TYPE)
					return (T)new Byte(jsonObject.getByte());
				else if (type == Byte.class)
					return type.cast(jsonObject.getByte());
				else if (type == Short.TYPE)
					return (T)new Short(jsonObject.getShort());
				else if (type == Short.class)
					return type.cast(jsonObject.getShort());
				else if (type == Integer.TYPE)
					return (T)new Integer(jsonObject.getInt());
				else if (type == Integer.class)
					return type.cast(jsonObject.getInt());
				else if (type == Float.TYPE)
					return (T)new Float(jsonObject.getFloat());
				else if (type == Float.class)
					return type.cast(jsonObject.getFloat());
				else if (type == Long.TYPE)
					return (T)new Long(jsonObject.getLong());
				else if (type == Long.class)
					return type.cast(jsonObject.getLong());
				else if (type == Double.TYPE)
					return (T)new Double(jsonObject.getDouble());
				else if (type == Double.class)
					return type.cast(jsonObject.getDouble());
				else if (type == Object.class)
				{
					if ((double)jsonObject.getLong() == jsonObject.getDouble())
					{
						long ln = jsonObject.getLong();
						if (ln >= (long)Integer.MIN_VALUE && ln <= (long)Integer.MAX_VALUE)
							return type.cast(new Integer(jsonObject.getInt()));
						return type.cast(new Long(jsonObject.getLong()));
					}
					else
						return type.cast(new Double(jsonObject.getDouble()));
				}
				else
					throw new JSONConversionException("Member "+memberName+" is numerically typed; target is not a numeric type.");
			}
			
			case STRING:
			{
				if (type.isEnum())
				{
					try {
						return (T)type.cast(Enum.valueOf((Class<Enum>)type, jsonObject.getString()));
					} catch (IllegalArgumentException e) {
						return null;
					}
				}
				else if (type == String.class)
					return type.cast(jsonObject.getString());
				else if (type == Object.class)
					return type.cast(jsonObject.getString());
				else
					throw new JSONConversionException("Member "+memberName+" is string typed; target is not String.");
			}
			
			case OBJECT:
			{
				JSONConverter<T> converter = getConverter(type);
				if (converter != null)
					return converter.getObject(jsonObject);
				
				// Nulls.
				if (jsonObject.isNull())
					return null;
				
				// Arrays.
				if (jsonObject.isArray())
				{
					// type is array
					if (Reflect.isArray(type))
					{
						Class<?> atype = Reflect.getArrayType(type);
						if (atype == null)
							throw new JSONConversionException("Member "+memberName+" cannot be converted; member is array and target is not array typed.");
						
						Object newarray = Array.newInstance(atype, jsonObject.length);
						for (int i = 0; i < jsonObject.length; i++)
							Array.set(newarray, i, createForType(String.format("%s[%d]", memberName, i), jsonObject.get(i), atype));
							
						return type.cast(newarray);
					}
					else
						throw new JSONConversionException("Member "+memberName+" cannot be converted; member is array and target is not array typed.");
				}
				
				// Objects.
				T out = null;
				try {
					out = type.newInstance();
				} catch (InstantiationException e) {
					throw new JSONConversionException("Member "+memberName+" cannot be converted; no nullary constructor or type is not instantiable.", e);
				} catch (IllegalAccessException e) {
					throw new JSONConversionException("Member "+memberName+" cannot be converted; constructor is not accessible.", e);
				} catch (ExceptionInInitializerError e) {
					throw new JSONConversionException("Member "+memberName+" cannot be converted; problem occurred during instantiation.", e);
				} catch (SecurityException e) {
					throw new JSONConversionException("Member "+memberName+" cannot be converted. Cannot access constructor.", e);
				}
				// whoa. recursive-ish!
				jsonObject.applyToObject(out);
				return out;
			}
			
			case UNDEFINED:
			{
				throw new JSONConversionException("Cannot apply "+memberName+". Undefined types cannot be applied.");
			}
			
			default:
				return null;
			
		}
	}

}
