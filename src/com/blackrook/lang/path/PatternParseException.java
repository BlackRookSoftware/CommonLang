/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.path;

/**
 * Exception possibly thrown when compiling a {@link PathPattern}.
 * @author Matthew Tropiano
 */
public class PatternParseException extends RuntimeException
{
	private static final long serialVersionUID = 4720384134470091958L;

	/**
	 * Creates a new exception.
	 */
	public PatternParseException()
	{
		super();
	}
	
	/**
	 * Creates a new exception.
	 */
	public PatternParseException(String message)
	{
		super(message);
	}

	/**
	 * Creates a new exception.
	 */
	public PatternParseException(Throwable exception)
	{
		super(exception);
	}
	
	/**
	 * Creates a new exception with a message.
	 */
	public PatternParseException(String message, Throwable exception)
	{
		super(message, exception);
	}
	
}
