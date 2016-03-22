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

import com.blackrook.lang.json.JSONObject;

/**
 * An annotation for telling {@link JSONObject} that this field or method
 * should not be serialized into a JSON construct of any kind.
 * @author Matthew Tropiano
 * @since 2.4.0 
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JSONIgnore
{
	/** 
	 * Ignore this value?
	 * @return true if so, false if not. 
	 */
	boolean value() default true;
}
