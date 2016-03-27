/*******************************************************************************
 * Copyright (c) 2009-2016 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.replacer.kernel;

import java.util.Arrays;

import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.lang.replacer.ReplacerCommand;

/**
 * A replacer kernel that replaces tokens (with no arguments) with another mapped string.
 * Unhandled tokens are returned verbatim. 
 * @author Matthew Tropiano
 * @since 2.9.0
 */
public class EnumReplacerKernel extends ArgumentTokenReplacerKernel
{
	/** Default token boundary character. */
	public static final char TOKEN_CHAR = '%';
	/** Default token argument list start boundary character. */
	public static final char TOKEN_ARGUMENT_LIST_CHAR = ':';
	/** Default token argument boundary character. */
	public static final char TOKEN_ARGUMENT_CHAR = ',';
	
	/** Enumeration map. */
	private HashMap<String, ReplacerCommand> enumMap;
	
	/**
	 * Creates a new enumerant-mapped replacer kernel with no token keys.
	 * The replaced tokens must be surrounded with '%' (e.g. %title%).
	 * @param enumerantValues the enumeration class that contains valid {@link ReplacerCommand} objects.
	 * @param caseInsensitive if true, the token names are case-insensitive.
	 */
	public EnumReplacerKernel(Enum<? extends ReplacerCommand>[] enumerantValues, boolean caseInsensitive)
	{
		this(enumerantValues, caseInsensitive, TOKEN_CHAR, TOKEN_CHAR, TOKEN_ARGUMENT_LIST_CHAR, TOKEN_ARGUMENT_CHAR);
	}
	
	/**
	 * Creates a new enumerant-mapped replacer kernel with no token keys.
	 * @param enumerantValues the enumeration class that contains valid {@link ReplacerCommand} objects.
	 * @param caseInsensitive if true, the token names are case-insensitive.
	 * @param tokenStart the token start character.
	 * @param tokenEnd the token end character.
	 */
	public EnumReplacerKernel(Enum<? extends ReplacerCommand>[] enumerantValues, boolean caseInsensitive, char tokenStart, char tokenEnd)
	{
		this(enumerantValues, caseInsensitive, tokenStart, tokenEnd, TOKEN_ARGUMENT_LIST_CHAR, TOKEN_ARGUMENT_CHAR);
	}
	
	/**
	 * Creates a new enumerant-mapped replacer kernel with no token keys.
	 * @param enumerantValues the enumeration class that contains valid {@link ReplacerCommand} objects.
	 * @param caseInsensitive if true, the token names are case-insensitive.
	 * @param tokenStart the token start character.
	 * @param tokenEnd the token end character.
	 * @param argumentListStart the character that starts the argument list, if any.
	 * @param argumentSeparator the argument separator character.
	 */
	public EnumReplacerKernel(Enum<? extends ReplacerCommand>[] enumerantValues, boolean caseInsensitive, char tokenStart, char tokenEnd, char argumentListStart, char argumentSeparator)
	{
		super(tokenStart, tokenEnd, argumentListStart, argumentSeparator);
		if (caseInsensitive)
			this.enumMap = new CaseInsensitiveHashMap<ReplacerCommand>();
		else
			this.enumMap = new HashMap<String, ReplacerCommand>();
		
		for (Enum<? extends ReplacerCommand> command : enumerantValues)
			enumMap.put(command.name(), (ReplacerCommand)command);
	}

	@Override
	public String handleToken(String tokenName, String[] arguments)
	{
		if (enumMap.containsKey(tokenName))
			return enumMap.get(tokenName).handleReplace(arguments);
		else
			return arguments.length > 0 ? tokenStart + tokenName + tokenEnd + argumentListStart + Arrays.toString(arguments) : tokenStart + tokenName + tokenEnd;
	}

}
