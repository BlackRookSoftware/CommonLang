/*******************************************************************************
 * Copyright (c) 2009-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang;

/**
 * Common Lexer Kernel - adds parameters for handling C-like preprocessor directives.
 * @author Matthew Tropiano
 * @since 2.3.0, all Lexer require kernels due to separation of Lexer parameters
 * and scanning state.
 */
public class CommonLexerKernel extends LexerKernel
{
	public static final int TYPE_PREPROCESSOR_DIRECTIVE = 0x7fffffff;

	/**
	 * Creates a new kernel for the {@link CommonLexer}.
	 */
	public CommonLexerKernel()
	{
		super.addSpecialDelimiter('#', TYPE_PREPROCESSOR_DIRECTIVE);
		super.setIncludeNewlines(true);
	}
	
	/** 
	 * Throws an {@link IllegalArgumentException} if attempted to be set to false. 
	 */
	@Override
	public void setIncludeNewlines(boolean includeNewlines)
	{
		if (!includeNewlines)
			throw new IllegalArgumentException("This cannot be set to false.");
		super.setIncludeNewlines(includeNewlines);
	}

	/**
	 * Does the same thing as {@link LexerKernel#addSpecialDelimiter(char, int)},
	 * except throws a {@link IllegalArgumentException} if '#' is attempted to be overridden.
	 */
	@Override
	public void addSpecialDelimiter(char specialDelimiter, int type)
	{
		if (specialDelimiter == '#')
			throw new IllegalArgumentException("This cannot be set to a different type.");
		super.addSpecialDelimiter(specialDelimiter, type);
	}
	
}
