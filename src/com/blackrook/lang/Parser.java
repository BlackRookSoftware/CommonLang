/*******************************************************************************
 * Copyright (c) 2009-2015 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang;

import java.io.IOException;

import com.blackrook.commons.list.List;

/**
 * Abstract parser class.
 * This class aids in the creation of top-down (AKA recursive-descent) parsers.
 * @author Matthew Tropiano
 */
public abstract class Parser
{
	/** Lexer used by this parser. */
	private Lexer lexer;
	/** Current lexer token. */
	private Lexer.Token token;
	/** List of error messages. */
	private List<String> errorList;
	
	/**
	 * Constructs the parser and binds a Lexer to it.
	 * @param lexer the lexer that this reads from.
	 */
	protected Parser(Lexer lexer)
	{
		this.lexer = lexer;
		errorList = new List<String>();
	}
	
	/**
	 * Gets the list of error messages.
	 * @return an array of error messages.
	 */
	public String[] getErrorMessages()
	{
		String[] out = new String[errorList.size()];
		int i = 0;
		for (String s : errorList)
			out[i++] = s;
		return out;
	}
	
	/**
	 * Gets the token read from the last {@link #nextToken()} call.
	 * @return the current token.
	 */
	protected Lexer.Token currentToken()
	{
		return token;
	}

	/**
	 * Matches the current token. If matched, this returns true and advances
	 * to the next token. Else, this returns false.
	 * @param tokenType the type to match.
	 * @return true if matched, false if not.
	 */
	protected boolean matchType(int tokenType)
	{
		if (currentType(tokenType))
		{
			nextToken();
			return true;
		}
		return false;
	}

	/**
	 * Attempts to match the type of the current token. If matched, this returns true.
	 * This DOES NOT ADVANCE to the next token.
	 * @param tokenTypes the list of types.
	 * @return true if one was matched, false if not.
	 */
	protected boolean currentType(int ... tokenTypes)
	{
		if (token != null)
		{
			for (int i : tokenTypes)
				if (token.getType() == i)
					return true;
		}
		return false;
	}

	/**
	 * Reads and sets the current token to the next token.
	 * If the current token is null, it is the end of the Lexer's stream.
	 * An error message is added as well.
	 * @throws ParserException if the next token can't be read. 
	 */
	protected void nextToken()
	{
		try {
			token = lexer.nextToken();
		} catch (IOException e) {
			addErrorMessage(e.getMessage());
			throw new ParserException(e.getMessage(), e);
		}
	}

	/**
	 * Adds an error message to error list along with the current token's information
	 * (like line number, etc.).
	 * @param errorMessage the error message.
	 */
	protected void addErrorMessage(String errorMessage)
	{
		String error = null;
		if (token == null)
		{
			error = "(STREAM END) "+errorMessage;
		}
		else
		{
			error = "("+lexer.getCurrentStreamName()+") " +
				"Line "+token.getLine()+
				", Token \""+token.getLexeme()+
				"\": "+errorMessage;
		}
		
		errorList.add(error);
	}
	
}
