/*******************************************************************************
 * Copyright (c) 2009-2016 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.json;

/**
 * JSON conversion exception, thrown when a {@link JSONConverter} fails.
 * @author Matthew Tropiano
 */
public class JSONConversionException extends RuntimeException
{
	private static final long serialVersionUID = 1386630496274856561L;

	/**
	 * Creates a new exception.
	 */
	public JSONConversionException()
	{
		super();
	}
	
	/**
	 * Creates a new exception.
	 * @param message the exception message.
	 */
	public JSONConversionException(String message)
	{
		super(message);
	}

	/**
	 * Creates a new exception.
	 * @param exception the exception that caused this one, if any.
	 */
	public JSONConversionException(Throwable exception)
	{
		super(exception);
	}
	
	/**
	 * Creates a new exception with a message.
	 * @param message the exception message.
	 * @param exception the exception that caused this one, if any.
	 */
	public JSONConversionException(String message, Throwable exception)
	{
		super(message, exception);
	}
}
