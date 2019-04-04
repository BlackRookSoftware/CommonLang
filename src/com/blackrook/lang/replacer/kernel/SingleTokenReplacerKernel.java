/*******************************************************************************
 * Copyright (c) 2009-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.replacer.kernel;

import com.blackrook.lang.replacer.ReplacerKernel;

/**
 * A replacer kernel that replaces tokens (with no arguments) with another mapped string.
 * Tokens are surrounded by special characters.
 * @author Matthew Tropiano
 * @since 2.9.0
 */
public abstract class SingleTokenReplacerKernel implements ReplacerKernel
{
	/** Token start character. */
	protected char tokenStart;
	/** Token end character. */
	protected char tokenEnd;
	
	/**
	 * Creates a new replacer kernel.
	 * @param tokenStart the token start character.
	 * @param tokenEnd the token end character.
	 */
	public SingleTokenReplacerKernel(char tokenStart, char tokenEnd)
	{
		this.tokenStart = tokenStart;
		this.tokenEnd = tokenEnd;
	}
	
	@Override
	public boolean isTokenStarter(char input)
	{
		return input == tokenStart;
	}

	@Override
	public char getTokenEnder(char input)
	{
		return input == tokenStart ? tokenEnd : '\0';
	}

	@Override
	public boolean isArgumentListStarter(char input)
	{
		return false;
	}

	@Override
	public boolean isArgumentSeparator(char input)
	{
		return false;
	}

	@Override
	public abstract String handleToken(String tokenName, String[] arguments);

}
