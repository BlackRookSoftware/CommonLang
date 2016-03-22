/*******************************************************************************
 * Copyright (c) 2009-2016 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.json.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.lang.json.JSONConverter;
import com.blackrook.lang.json.JSONDefaultConverter;
import com.blackrook.lang.json.JSONObject;

/**
 * An annotation for telling {@link JSONObject} that this object type
 * needs special rules for conversion (both to and from this type).
 * @author Matthew Tropiano
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JSONType
{
	/** 
	 * This class's converter class.
	 * @return the JSONConverter class to use. 
	 */
	Class<? extends JSONConverter<Object>> converter() default JSONDefaultConverter.class;
}
