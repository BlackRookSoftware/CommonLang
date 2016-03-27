/*******************************************************************************
 * Copyright (c) 2009-2016 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.replacer.kernel;

/**
 * A replacer kernel that replaces tokens (with no arguments) with another mapped string.
 * Tokens are surrounded by special characters.
 * @author Matthew Tropiano
 * @since 2.9.0
 */
public abstract class ArgumentTokenReplacerKernel extends SingleTokenReplacerKernel
{
	/** Argument list separator character. */
	protected char argumentListStart;
	/** Argument separator character. */
	protected char argumentSeparator;
	
	/**
	 * Creates a new replacer kernel.
	 * @param tokenStart the token start character.
	 * @param tokenEnd the token end character.
	 * @param argumentListStart the character that starts the argument list, if any.
	 * @param argumentSeparator the argument separator character.
	 */
	public ArgumentTokenReplacerKernel(char tokenStart, char tokenEnd, char argumentListStart, char argumentSeparator)
	{
		super(tokenStart, tokenEnd);
		this.argumentListStart = argumentListStart;
		this.argumentSeparator = argumentSeparator;
	}
	
	@Override
	public boolean isArgumentListStarter(char input)
	{
		return input == argumentListStart;
	}

	@Override
	public boolean isArgumentSeparator(char input)
	{
		return input == argumentSeparator;
	}

	@Override
	public abstract String handleToken(String tokenName, String[] arguments);

}
