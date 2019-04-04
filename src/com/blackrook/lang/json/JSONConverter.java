/*******************************************************************************
 * Copyright (c) 2009-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.json;

/**
 * JSON converters. Converts between JSON objects to Java Objects and back.
 * @author Matthew Tropiano
 * @param <T> the object type that this converter converts to JSON and back.
 */
public interface JSONConverter<T extends Object>
{
	/**
	 * Returns a JSON object that represents this object instance (and its contents).
	 * The policy of this method is to help return something that can be converted back to 
	 * Java via {@link #getObject(JSONObject)} with close-to or dead-on 100% equivalence. 
	 * @param object the object to inspect.
	 * @return an accurate primitive that represents this object. 
	 * @throws JSONConversionException if object conversion fails. 
	 */
	public JSONObject getJSONObject(T object);

	/**
	 * Returns a Java Object that is equivalent to the input JSON Object. 
	 * @param jsonObject the JSON Object to convert.
	 * @return an accurate primitive that represents this object. 
	 * @throws JSONConversionException if object conversion fails. 
	 */
	public T getObject(JSONObject jsonObject);
	
}
