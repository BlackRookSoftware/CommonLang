/*******************************************************************************
 * Copyright (c) 2009-2016 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.linkedlist.Stack;
import com.blackrook.commons.list.List;

/**
 * A special Lexer that supports common C-like preprocessor statements such as
 * "include", "if", "define", and other well-used statements in C preprocessors.
 * Not as flexible or as feature-rich as most C preprocessors.
 * <p>
 * This is set to "include newline" characters, which are used to parse preprocessor lines.
 * They will not be return by nextToken() to implementors of this class, however.
 * <p>
 * This adds support for the following keywords, should they appear in the line:
 * <ul>
 * <li><b>#include [StringExpression]</b> - includes the contents of another file resource in this stream.</li>
 * <li><b>#define [Identifier] [TokenList]</b> - defines a macro that replaces a token with the list of tokens defined after it.</li>
 * <li><b>#undefine [Identifier]</b> - un-defines a macro.</li>
 * <li><b>#ifdef [Identifier]</b> - includes the following lines if the identifier is defined.</li>
 * <li><b>#ifndef [Identifier]</b> - includes the following lines if the identifier is NOT defined.</li>
 * <li><b>#endif</b> - terminates #ifdef/#ifndef blocks.</li>
 * </ul>
 * @author Matthew Tropiano
 */
public class CommonLexer extends Lexer
{
	/** Table of defined macros. */
	private HashMap<String, MacroSet> defineTable;
	/** Define token stack. */
	private Stack<Lexer.Token> tokenStack;

	/** In if block? */
	private Stack<Boolean> ifBlockStack;

	/**
	 * Creates a new lexer around a String, that will
	 * be wrapped into a StringReader class chain.
	 * This will also assign this lexer a default name.
	 * @param kernel the lexer kernel to use for defining how to parse the input text.
	 * @param in the input stream to read from.
	 */
	public CommonLexer(CommonLexerKernel kernel, String in)
	{
		this(kernel, null, in);
	}
	
	/**
	 * Creates a new lexer around a String, that will
	 * be wrapped into a StringReader class chain.
	 * @param kernel the lexer kernel to use for defining how to parse the input text.
	 * @param name	the name of this lexer.
	 * @param in the input stream to read from.
	 */
	public CommonLexer(CommonLexerKernel kernel, String name, String in)
	{
		this(kernel, name, new StringReader(in));
	}
	
	/**
	 * Constructs this lexer from a reader.
	 * @param kernel the lexer kernel to use for defining how to parse the input text.
	 * @param in the reader to read from.
	 */
	public CommonLexer(CommonLexerKernel kernel, Reader in)
	{
		this(kernel, null, in);
	}

	/**
	 * Creates a new lexer from a reader.
	 * @param kernel the lexer kernel to use for defining how to parse the input text.
	 * @param name the name of this lexer.
	 * @param in the reader to read from.
	 */
	public CommonLexer(CommonLexerKernel kernel, String name, Reader in)
	{
		super(kernel, name, in);
		tokenStack = new Stack<Lexer.Token>();
		defineTable = new HashMap<String, MacroSet>();
		ifBlockStack = new Stack<Boolean>();
	}

	@Override
	public Token nextToken() throws IOException
	{
		Token token;
		if (!tokenStack.isEmpty())
			token = tokenStack.pop();
		else
			token = super.nextToken();
		
		// newline character.
		if (token == null)
			return null;
		
		// newline
		else if (token.getType() == CommonLexerKernel.TYPE_DELIM_NEWLINE)
			return nextToken();
		
		// non-directive
		else if (token.getType() != CommonLexerKernel.TYPE_PREPROCESSOR_DIRECTIVE)
		{
			if (!ifCheck())
				return nextToken();
				
			switch (token.getType())
			{
				case CommonLexerKernel.TYPE_IDENTIFIER:
				{
					if (defineTable.containsKey(token.getLexeme()))
					{
						MacroSet macro = defineTable.get(token.getLexeme());
						for (int i = macro.tokenList.length-1; i >= 0; i--)
							tokenStack.push(macro.tokenList[i]);
						return nextToken();
					}
				}
					break;
			}
			
			return token;
		}
		
		// #include
		else if (token.getLexeme().equals("#include"))
		{
			if (!ifCheck())
				return nextToken();
				
			Token ptok = super.nextToken();
			if (ptok != null && ptok.getType() == CommonLexerKernel.TYPE_STRING)
			{
				String pathname = ptok.getLexeme();
				InputStreamReader reader;
				
				String nextname = getNextResourceName(getCurrentStreamName(), pathname);
				InputStream in = getResource(nextname);
				
				if (in == null)
					throw createException("Include directive: Resource named '"+nextname+"' could not be found.");
				reader = new InputStreamReader(in);
				
				pushStream(nextname, reader);
				return nextToken();
			}
			else 
				throw createException("Include directive: Expected string token describing a resource path.");
		}
		
		// #define
		else if (token.getLexeme().equals("#define"))
		{
			if (!ifCheck())
				return nextToken();

			Token ptok = super.nextToken();
			if (ptok.getType() == CommonLexerKernel.TYPE_IDENTIFIER)
			{
				String def = ptok.getLexeme();
				List<Lexer.Token> list = new List<Lexer.Token>();

				ptok = super.nextToken();
				if (ptok == null)
					throw createException("Define directive: Unfinished declaration '"+def+"'.");
					
				while (ptok != null && ptok.getType() != CommonLexerKernel.TYPE_DELIM_NEWLINE)
				{
					if (ptok.getLexeme().equals(def))
						throw createException("Define directive: Attempted to create recursive definition '"+def+"'.");
						
					list.add(ptok);
					ptok = super.nextToken();
				}
				
				Lexer.Token[] tokens = new Lexer.Token[list.size()];
				for (int i = 0; i < tokens.length; i++)
					tokens[i] = list.getByIndex(i);
				
				addDefineMacro(def, tokens);
				return nextToken();
			}
			else throw createException("Define directive: Expected identifier.");
		}

		// #undefine
		else if (token.getLexeme().equals("#undefine"))
		{
			if (!ifCheck())
				return nextToken();

			Token ptok = super.nextToken();
			if (ptok.getType() == CommonLexerKernel.TYPE_IDENTIFIER)
			{
				String def = ptok.getLexeme();
				removeDefineMacro(def);
				return nextToken();
			}
			else throw createException("Undefine directive: Expected identifier.");
		}
		
		// #ifdef
		else if (token.getLexeme().equals("#ifdef"))
		{
			Token ptok = super.nextToken();
			if (ptok.getType() == CommonLexerKernel.TYPE_IDENTIFIER)
			{
				ifBlockStack.push(defineTable.containsKey(ptok.getLexeme()));
				return nextToken();
			}
			else throw createException("Ifdef directive: Expected identifier.");
		}
		
		// #ifndef
		else if (token.getLexeme().equals("#ifndef"))
		{
			Token ptok = super.nextToken();
			if (ptok.getType() == CommonLexerKernel.TYPE_IDENTIFIER)
			{
				ifBlockStack.push(!defineTable.containsKey(ptok.getLexeme()));
				return nextToken();
			}
			else throw createException("Ifndef directive: Expected identifier.");
		}
		
		// #endif
		else if (token.getLexeme().equals("#endif"))
		{
			if (!ifBlockStack.isEmpty())
			{
				ifBlockStack.pop();
				return nextToken();
			}
			else throw createException("Endif directive: No previous \"if\" directive.");
		}
		
		else throw createException("Unknown directive '"+token.getLexeme()+"'.");
	}
	
	/**
	 * Adds a define macro to this lexer.
	 * @param defineName	the name of the define.
	 * @param tokens		the associated tokens.
	 */
	public void addDefineMacro(String defineName, Lexer.Token ... tokens)
	{
		defineTable.put(defineName, new MacroSet(tokens));
	}

	/**
	 * Removes a define macro from this function.
	 * @param defineName	the name of the define.
	 */
	public void removeDefineMacro(String defineName)
	{
		defineTable.removeUsingKey(defineName);
	}

	/**
	 * Creates an IOException for preprocessor errors and problems.
	 * @param message the exception message.
	 * @return the exception to throw.
	 */
	protected IOException createException(String message)
	{
		return new IOException("("+getCurrentStreamName()+") Line "+getCurrentLine()+": "+message);
	}
	
	/**
	 * Checks if the "IF" stack is empty; if not, returns the top.
	 * @return the topmost IF boolean value, or true if the stack is empty.
	 */
	protected boolean ifCheck()
	{
		return ifBlockStack.isEmpty() || (!ifBlockStack.isEmpty() && ifBlockStack.peek());
	}

	/**
	 * Returns the next path to use for resolving an included resource, given the current stream. 
	 * Unless overridden, this returns <code>(new File(currentStreamName)).getParentFile().getPath() + File.pathSeparator + includePath</code>
	 * but if it doesn't exist, then <code>includePath</code>.
	 * @param currentStreamName the name/path of the current stream.
	 * @param includePath the path taken from the <code>#include</code> directive.
	 * @return the presumably valid resource name to use (to pass to {@link #getResource(String)}).
	 * @throws IOExeption if the resolution of this new name incurs an IOException. 
	 * @since 2.9.1
	 */
	protected String getNextResourceName(String currentStreamName, String includePath) throws IOException
	{
		String parentPath = new File(currentStreamName).getParentFile().getPath(); 
		File parentSearchFile = new File(parentPath + File.pathSeparator + includePath);
		if (parentSearchFile.exists())
			return parentSearchFile.getPath();
		else
			return includePath;
	}
	
	/**
	 * Returns an open input stream to a resource at a specified path in an "include" directive.
	 * Assumes that the input path does NOT need additional resolving.
	 * @param resourcePath the resource path to "include".
	 * @return an open input stream to the resolved resource.
	 * @throws IOException if the stream cannot be opened.
	 */
	protected InputStream getResource(String resourcePath) throws IOException
	{
		return new FileInputStream(resourcePath);
	}
	
	/**
	 * Macro set class.
	 * @author Matthew Tropiano
	 */
	private class MacroSet
	{
		/** Tokens involved in the macro. */
		Lexer.Token[] tokenList;
		
		MacroSet(Lexer.Token ... tokenList)
		{
			this.tokenList = tokenList;
		}
	}
	
}
