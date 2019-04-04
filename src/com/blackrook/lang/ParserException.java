/*******************************************************************************
 * Copyright (c) 2009-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang;

/**
 * Thrown when a Parser has a problem.
 * @author Matthew Tropiano
 */
public class ParserException extends RuntimeException
{
	private static final long serialVersionUID = 6712240658282073090L;

	public ParserException()
	{
		super("Bad type requested.");
	}

	public ParserException(String s)
	{
		super(s);
	}
	
	public ParserException(String s, Throwable t)
	{
		super(s, t);
	}
}
