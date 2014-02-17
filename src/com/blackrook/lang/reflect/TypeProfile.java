/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.HashMap;

/**
 * Type profile for an unknown object that has an ambiguous signature for 
 * applying values to POJOs and beans.
 * This only cares about setter methods with one argument and public fields.
 * @author Matthew Tropiano
 * @since 2.3.0
 * @deprecated Moved to Commons project as com.blackrook.common.TypeProfile.
 */
public class TypeProfile<T extends Object>
{
	/** JSON type profiles. */
	private static final HashMap<Class<?>, TypeProfile<?>> 
		REGISTERED_TYPES = new HashMap<Class<?>, TypeProfile<?>>();

	/** Map of Public fields. */
	private HashMap<String, Field> publicFields;
	/** Map of setters. */
	private HashMap<String, MethodSignature> setterMethods;
	
	/**
	 * Gets a type profile for a type.
	 * This method creates a profile if it hasn't already been made, and returns
	 * what is created.
	 */
	@SuppressWarnings("unchecked")
	public static <E extends Object> TypeProfile<E> getTypeProfile(Class<E> clazz)
	{
		TypeProfile<E> out = null;
		if ((out = (TypeProfile<E>)REGISTERED_TYPES.get(clazz)) == null)
		{
			synchronized (REGISTERED_TYPES)
			{
				if ((out = (TypeProfile<E>)REGISTERED_TYPES.get(clazz)) == null)
				{
					out = new TypeProfile<E>(clazz);
					setTypeProfile(clazz, out);
				}
			}
		}
		else
			out = (TypeProfile<E>)REGISTERED_TYPES.get(clazz);
		
		return out;
	}
	
	/**
	 * Sets a type profile for a type.
	 */
	private static <E extends Object> void setTypeProfile(Class<E> clazz, TypeProfile<E> profile)
	{
		REGISTERED_TYPES.put(clazz, profile);
	}
	
	/** Creates a profile from a class. */
	private TypeProfile(Class<? extends T> inputClass)
	{
		publicFields = new HashMap<String, Field>();
		setterMethods = new HashMap<String, MethodSignature>();
		
		for (Field f : inputClass.getFields())
			publicFields.put(f.getName(), f);
		
		for (Method m : inputClass.getMethods())
			if (Reflect.isSetter(m))
				setterMethods.put(Reflect.getFieldName(m.getName()), new MethodSignature(m.getParameterTypes()[0], m));
	}
	
	/** 
	 * Returns a reference to the map that contains this profile's public fields.
	 * Maps "field name" to {@link Field} object. 
	 */
	public HashMap<String, Field> getPublicFields()
	{
		return publicFields;
	}

	/** 
	 * Returns a reference to the map that contains this profile's public fields.
	 * Maps "field name" to {@link MethodSignature} object, which contains the {@link Class} type
	 * and the {@link Method} itself. 
	 */
	public HashMap<String, MethodSignature> getSetterMethods()
	{
		return setterMethods;
	}

	/**
	 * Method signature.
	 */
	public static class MethodSignature
	{
		/** Object Type. */
		Class<?> type;
		/** Method signature. */
		Method method;
		
		MethodSignature(Class<?> type, Method method)
		{
			this.type = type;
			this.method = method;
		}

		/**
		 * Returns the type that this setter takes as an argument.
		 */
		public Class<?> getType()
		{
			return type;
		}

		/**
		 * Returns the setter method itself.
		 */
		public Method getMethod()
		{
			return method;
		}
	}
	
}
