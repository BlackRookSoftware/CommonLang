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
	 * Matches the current token. If matched, this returns true and advances
	 * to the next token. Else, this throws an error and returns false.
	 * @param tokenType the type to match.
	 * @return true if matched, false if not.
	 * @deprecated in 2.7.0 - Calls {@link #getTypeErrorText(int)}, 
	 * 		but this leads to a loss of clarity and freedom with error message creation, and
	 * 		should not be used.
	 */
	protected boolean matchTypeStrict(int tokenType)
	{
		if (currentType(tokenType))
		{
			nextToken();
			return true;
		}
		addTypeError(tokenType);
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
	 * @throws ParserException if the next token can't be read. 
	 */
	protected void nextToken()
	{
		try {
			token = lexer.nextToken();
		} catch (IOException e) {
			throw new ParserException("Could not read next token from Lexer: "+e.getMessage(), e);
		}
	}

	/**
	 * Returns an error based on an expected type.
	 * See addErrorMessage().
	 * @param tokenTypes the type codes for the tokens that should have been matched.
	 * @deprecated in 2.7.0 - {@link #matchTypeStrict(int)} calls this, 
	 * 		but this leads to a loss of clarity with error message creation, and
	 * 		should not be used.
	 */
	protected void addTypeError(int ... tokenTypes)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Expected");
		for (int i = 0; i < tokenTypes.length; i++)
		{
			sb.append(' ');
			sb.append(getTypeErrorText(tokenTypes[i]));
			if (i < tokenTypes.length-1)
				sb.append(" or");
		}
		sb.append('.');
		addErrorMessage(sb.toString());
	}
	
	/**
	 * Returns a String form if the token type that was expected.
	 * Called by addTypeError().
	 * @param tokenType	the type code for the token that should have been matched.
	 * @return the String representation of the type.
	 * @deprecated in 2.7.0 - {@link #matchTypeStrict(int)} calls this, 
	 * 		but this leads to a loss of clarity with error message creation, and
	 * 		should not be used.
	 */
	@Deprecated
	protected String getTypeErrorText(int tokenType)
	{
		return null;
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
