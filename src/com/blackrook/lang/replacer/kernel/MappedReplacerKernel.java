/*******************************************************************************
 * Copyright (c) 2009-2016 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.replacer.kernel;

import com.blackrook.commons.hash.HashMap;

/**
 * A replacer kernel that replaces tokens (with no arguments) with another mapped string.
 * Unhandled tokens are returned verbatim. 
 * @author Matthew Tropiano
 * @since 2.9.0
 */
public class MappedReplacerKernel extends SingleTokenReplacerKernel
{
	/** Default token boundary character. */
	public static final char TOKEN_CHAR = '%';
	
	/** Token map. */
	private HashMap<String, String> tokenMap;
	
	/**
	 * Creates a new mapped replacer kernel with no token keys.
	 * The replaced tokens must be surrounded with '%' (e.g. %title%). 
	 */
	public MappedReplacerKernel()
	{
		this(TOKEN_CHAR, TOKEN_CHAR);
	}
	
	/**
	 * Creates a new mapped replacer kernel with no token keys.
	 * @param tokenStart the token start character.
	 * @param tokenEnd the token end character.
	 */
	public MappedReplacerKernel(char tokenStart, char tokenEnd)
	{
		super(tokenStart, tokenEnd);
		this.tokenMap = new HashMap<String, String>();
	}

	/**
	 * Sets a replace token and corresponding value.
	 * @param tokenName the token name.
	 * @param value the corresponding replacement value.
	 */
	public void set(String tokenName, String value)
	{
		tokenMap.put(tokenName, value);
	}
	
	/**
	 * Removes a replace token name.
	 * @param tokenName the token name.
	 */
	public void clear(String tokenName)
	{
		tokenMap.removeUsingKey(tokenName);
	}
	
	@Override
	public String handleToken(String tokenName, String[] arguments)
	{
		return !tokenMap.containsKey(tokenName) ? tokenStart + tokenName + tokenEnd : tokenMap.get(tokenName);
	}

}
