/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.DecimalFormatSymbols;

import com.blackrook.commons.Common;
import com.blackrook.commons.linkedlist.Stack;
import com.blackrook.commons.list.List;

/**
 * Class used for breaking up a stream of characters into lexicographical tokens.
 * Spaces, newlines, tabs, and breaks in the stream are added if desired,
 * otherwise, they are stripped out. 
 * 
 * Special delimiter characters take precedence over String delimiters.
 * String delimiter characters take precedence over regular delimiters.
 * Delimiter characters take parsing priority over other characters.
 * Delimiter evaluation priority goes: CommentDelimiter, Delimiter.
 * Identifier evaluation priority goes: Keyword, CaseInsensitiveKeyword, Identifier.
 *
 * @author Matthew Tropiano
 * @since 2.3.0, this now supports integers/floating-point numbers with exponent notation,
 * and separates the configuration for how Lexers scan for information with {@link LexerKernel}.
 */
public class Lexer
{
	public static boolean DEBUG = Common.parseBoolean(System.getProperty(Lexer.class.getName()+".debug"), false);
	
	/** Reserved token type: End of lexer. */
	public static final int TYPE_END_OF_LEXER = 		-1;
	/** Reserved token type: End of stream. */
	public static final int TYPE_END_OF_STREAM =		-2;
	/** Reserved token type: Number. */
	public static final int TYPE_NUMBER = 				-3;
	/** Reserved token type: Space. */
	public static final int TYPE_DELIM_SPACE = 			-4;
	/** Reserved token type: Tab. */
	public static final int TYPE_DELIM_TAB = 			-5;
	/** Reserved token type: New line character. */
	public static final int TYPE_DELIM_NEWLINE = 		-6;
	/** Reserved token type: Open comment. */
	public static final int TYPE_DELIM_OPEN_COMMENT = 	-7;
	/** Reserved token type: Close comment. */
	public static final int TYPE_DELIM_CLOSE_COMMENT = 	-8;
	/** Reserved token type: Line comment. */
	public static final int TYPE_DELIM_LINE_COMMENT = 	-9;
	/** Reserved token type: Identifier. */
	public static final int TYPE_IDENTIFIER = 			-10;
	/** Reserved token type: Unknown token. */
	public static final int TYPE_UNKNOWN = 				-11;
	/** Reserved token type: Illegal token. */
	public static final int TYPE_ILLEGAL = 				-12;
	/** Reserved token type: Comment. */
	public static final int TYPE_COMMENT = 				-13;
	/** Reserved token type: Line Comment. */
	public static final int TYPE_LINE_COMMENT = 		-14;
	/** Reserved token type: String. */
	public static final int TYPE_STRING = 				-15;
	/** Reserved token type: Special (never returned). */
	public static final int TYPE_SPECIAL = 				-16;
	/** Reserved token type: Delimiter (never returned). */
	public static final int TYPE_DELIMITER = 			-17;
	/** Reserved token type: Point state (never returned). */
	public static final int TYPE_POINT = 				-18;
	/** Reserved token type: Floating point state (never returned). */
	public static final int TYPE_FLOAT = 				-19;
	/** Reserved token type: Delimiter Comment (never returned). */
	public static final int TYPE_DELIM_COMMENT = 		-20;
	/** Reserved token type: hexadecimal integer (never returned). */
	public static final int TYPE_HEX_INTEGER0 = 		-21;
	/** Reserved token type: hexadecimal integer (never returned). */
	public static final int TYPE_HEX_INTEGER1 = 		-22;
	/** Reserved token type: hexadecimal integer (never returned). */
	public static final int TYPE_HEX_INTEGER = 			-23;
	/** Reserved token type: Exponent state (never returned). */
	public static final int TYPE_EXPONENT = 			-24;
	/** Reserved token type: Exponent power state (never returned). */
	public static final int TYPE_EXPONENT_POWER = 		-25;

	/** Default lexer name. */
	public static final String DEFAULT_NAME = "Lexer";
	
	/** Lexer end-of-stream char. */
	public static final char END_OF_STREAM = '\ufffe';
	/** Lexer end-of-stream char. */
	public static final char END_OF_LEXER = '\uffff';
	/** Lexer newline char. */
	public static final char NEWLINE = '\n';
	/** The locale's default decimal separator. */
	public static final char DEFAULT_DECIMAL_POINT = DecimalFormatSymbols.getInstance().getDecimalSeparator();
	
	/** The lexer kernel to use. */
	private LexerKernel kernel;
	/** Stream stack. */
	private Stack<Stream> streamStack;
	/** The current state. */
	private int state;
	/** List of error messages. */
	private List<String> preprocessorErrorList;
	/** Current token builder. */
	private StringBuilder builder;
	/** Current string end char. */
	private char stringEnd;
	/** If true, we are in a delimiter break. */
	private boolean delimBreak;
	/** Saved character for delimiter test. */
	private char delimBreakChar;
	/** Current special type. */
	private int specialType;

	/**
	 * Creates a new lexer around a String, that will be wrapped into a StringReader.
	 * This will also assign this lexer a default name.
	 * @param kernel the lexer kernel to use for defining how to parse the input text.
	 * @param in the string to read from.
	 */
	public Lexer(LexerKernel kernel, String in)
	{
		this(kernel, null, in);
	}
	
	/**
	 * Creates a new lexer around a String, that will be wrapped into a StringReader.
	 * @param name	the name of this lexer.
	 * @param in	the reader to read from.
	 */
	public Lexer(LexerKernel kernel, String name, String in)
	{
		this(kernel, name, new StringReader(in));
	}
	
	/**
	 * Creates a new lexer around a reader.
	 * This will also assign this lexer a default name.
	 * @param in	the reader to read from.
	 */
	public Lexer(LexerKernel kernel, Reader in)
	{
		this(kernel, null, in);
	}
	
	/**
	 * Creates a new lexer around a reader.
	 * @param name	the name of this lexer.
	 * @param in	the reader to read from.
	 */
	public Lexer(LexerKernel kernel, String name, Reader in)
	{
		this.kernel = kernel;
		streamStack = new Stack<Stream>();
		state = TYPE_UNKNOWN;
		builder = new StringBuilder();
		preprocessorErrorList = new List<String>();
		pushStream(name, in);
	}
	
	/**
	 * Returns the lexer's current stream name.
	 */
	public String getCurrentStreamName()
	{
		if (streamStack.isEmpty())
			return "LEXER END";
		return streamStack.peek().streamName;
	}

	/**
	 * Returns the lexer's current stream's line number.
	 * Returns -1 if at Lexer end.
	 */
	public int getCurrentLine()
	{
		if (streamStack.isEmpty())
			return -1;
		return streamStack.peek().lineNum;
	}

	/**
	 * Pushes a stream onto the stream stack.
	 * This will reset the token state as well.
	 * @param name	the name of the stream.
	 * @param in	the reader reader.
	 */
	public void pushStream(String name, Reader in)
	{
		streamStack.push(new Stream(name, in));
	}
	
	/**
	 * Gets the current stream.
	 */
	public Stream currentStream()
	{
		return streamStack.peek();
	}
	
	/**
	 * Reads the next token.
	 * If there are no tokens left to read, this will return null.
	 */
	public Token nextToken() throws IOException
	{
		boolean breakloop = false;
		while (!breakloop)
		{
			char c = 0;
			if (isOnDelimBreak())
			{
				c = delimBreakChar;
				clearDelimBreak();
			}
			else
				c = readChar();
			
			switch (getState())
			{
				case TYPE_END_OF_LEXER:
				{
					breakloop = true;
				}
					break;

				case TYPE_UNKNOWN:
				{
					if (isLexerEnd(c))
					{
						setState(TYPE_END_OF_LEXER);
						breakloop = true;
					}
					else if (isStreamEnd(c))
					{
						if (kernel.willIncludeStreamBreak())
						{
							setState(TYPE_END_OF_STREAM);
							breakloop = true;
						}
						streamStack.pop();
					}
					else if (isNewline(c))
					{
						if (kernel.willIncludeNewlines())
						{
							setState(TYPE_DELIM_NEWLINE);
							breakloop = true;
						}
					}
					else if (isSpace(c))
					{
						if (kernel.willIncludeSpaces())
						{
							setState(TYPE_DELIM_SPACE);
							breakloop = true;
						}
					}
					else if (isTab(c))
					{
						if (kernel.willIncludeTabs())
						{
							setState(TYPE_DELIM_TAB);
							breakloop = true;
						}
					}
					else if (isWhitespace(c))
					{
					}
					else if (isPoint(c) && isDelimiterStart(c))
					{
						setState(TYPE_POINT);
						saveChar(c);
					}
					else if (isPoint(c) && !isDelimiterStart(c))
					{
						setState(TYPE_FLOAT);
						saveChar(c);
					}
					else if (isSpecialStart(c))
					{
						setState(TYPE_SPECIAL);
						setSpecialType(c);
						saveChar(c);
					}
					else if (isStringStart(c))
					{
						setState(TYPE_STRING);
						setStringStartAndEnd(c);
					}
					else if (isDelimiterStart(c))
					{
						setState(TYPE_DELIMITER);
						saveChar(c);
					}
					else if (isUnderscore(c))
					{
						setState(TYPE_IDENTIFIER);
						saveChar(c);
					}
					else if (isLetter(c))
					{
						setState(TYPE_IDENTIFIER);
						saveChar(c);
					}
					else if (c == '0')
					{
						setState(TYPE_HEX_INTEGER0);
						saveChar(c);
					}
					else if (isDigit(c))
					{
						setState(TYPE_NUMBER);
						saveChar(c);
					}
					else
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
				}
					break; // end TYPE_START_OF_LEXER
				
				case TYPE_ILLEGAL:
				{
					if (isStreamEnd(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isNewline(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpace(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isTab(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isWhitespace(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpecialStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isDelimiterStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isLetter(c))
					{
						saveChar(c);
					}
					else if (isDigit(c))
					{
						saveChar(c);
					}
					else
					{
						saveChar(c);
					}

				}
					break; // end TYPE_ILLEGAL
					

				case TYPE_POINT: // decimal point is seen, but it is a delimiter.
				{
					if (isStreamEnd(c))
					{
						setState(TYPE_DELIMITER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isNewline(c))
					{
						setState(TYPE_DELIMITER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpace(c))
					{
						setState(TYPE_DELIMITER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isTab(c))
					{
						setState(TYPE_DELIMITER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isWhitespace(c))
					{
						setState(TYPE_DELIMITER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpecialStart(c))
					{
						setState(TYPE_DELIMITER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringStart(c))
					{
						setState(TYPE_DELIMITER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isDigit(c))
					{
						setState(TYPE_FLOAT);
						saveChar(c);
					}
					else
					{
						setState(TYPE_DELIMITER);
						if (kernel.getDelimTable().containsKey(getCurrentLexeme() + c))
							saveChar(c);
						else
						{
							setDelimBreak(c);
							breakloop = true;
						}
					}
				}
					break; // end TYPE_POINT
					
				case TYPE_FLOAT:
				{
					if (isStreamEnd(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isNewline(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpace(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isTab(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isWhitespace(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isExponent(c))
					{
						setState(TYPE_EXPONENT);
						saveChar(c);
					}
					else if (isSpecialStart(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringStart(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isDigit(c))
					{
						saveChar(c);
					}
					else if (isDelimiterStart(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
				}
					break; // end TYPE_FLOAT
					
				case TYPE_IDENTIFIER:
				{
					if (isStreamEnd(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isNewline(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpace(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isTab(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isWhitespace(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpecialStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isDelimiterStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isLetter(c))
					{
						saveChar(c);
					}
					else if (isDigit(c))
					{
						saveChar(c);
					}
					else if (isUnderscore(c))
					{
						saveChar(c);
					}
					else
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
				}
					break; // end TYPE_IDENTIFIER
					
				case TYPE_SPECIAL:
				{
					if (isStreamEnd(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isNewline(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpace(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isTab(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isWhitespace(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpecialStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isDelimiterStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isLetter(c))
					{
						saveChar(c);
					}
					else if (isDigit(c))
					{
						saveChar(c);
					}
					else
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
				}
					break; // end TYPE_IDENTIFIER
					
				case TYPE_HEX_INTEGER0:
				{
					if (isStreamEnd(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isNewline(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpace(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isTab(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isWhitespace(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isPoint(c))
					{
						setState(TYPE_FLOAT);
						saveChar(c);
					}
					else if (isSpecialStart(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringStart(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isDelimiterStart(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (c == 'x' || c == 'X')
					{
						setState(TYPE_HEX_INTEGER1);
						saveChar(c);
					}
					else if (isLetter(c))
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
					else if (isDigit(c))
					{
						setState(TYPE_NUMBER);
						saveChar(c);
					}
					else
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
				}
					break; // end TYPE_HEX_INTEGER0

				case TYPE_HEX_INTEGER1:
				{
					if (isStreamEnd(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isNewline(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpace(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isTab(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isWhitespace(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isPoint(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpecialStart(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringStart(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isDelimiterStart(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isHexDigit(c))
					{
						setState(TYPE_HEX_INTEGER);
						saveChar(c);
					}
					else if (isLetter(c))
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
					else
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
				}
					break; // end TYPE_HEX_INTEGER1

				case TYPE_HEX_INTEGER:
				{
					if (isStreamEnd(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isNewline(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpace(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isTab(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isWhitespace(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpecialStart(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringStart(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isDelimiterStart(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isHexDigit(c))
					{
						saveChar(c);
					}
					else if (isLetter(c))
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
					else
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
				}
					break; // end TYPE_HEX_INTEGER

				case TYPE_NUMBER:
				{
					if (isStreamEnd(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isNewline(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpace(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isTab(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isWhitespace(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isPoint(c))
					{
						setState(TYPE_FLOAT);
						saveChar(c);
					}
					else if (isExponent(c))
					{
						setState(TYPE_EXPONENT);
						saveChar(c);
					}
					else if (isSpecialStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isDelimiterStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isLetter(c))
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
					else if (isDigit(c))
					{
						saveChar(c);
					}
					else
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
				}
					break; // end TYPE_NUMBER

				case TYPE_EXPONENT:
				{
					if (isStreamEnd(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isNewline(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpace(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isTab(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isWhitespace(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isExponentSign(c))
					{
						setState(TYPE_EXPONENT_POWER);
						saveChar(c);
					}
					else if (isSpecialStart(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringStart(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isDelimiterStart(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isLetter(c))
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
					else if (isDigit(c))
					{
						setState(TYPE_EXPONENT_POWER);
						saveChar(c);
					}
					else
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
				}
					break; // end TYPE_EXPONENT
					
				case TYPE_EXPONENT_POWER:
				{
					if (isStreamEnd(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isNewline(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpace(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isTab(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isWhitespace(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpecialStart(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringStart(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isDigit(c))
					{
						saveChar(c);
					}
					else if (isDelimiterStart(c))
					{
						setState(TYPE_NUMBER);
						setDelimBreak(c);
						breakloop = true;
					}
					else
					{
						setState(TYPE_ILLEGAL);
						saveChar(c);
					}
				}
					break; // end TYPE_EXPONENT_POWER

				case TYPE_STRING:
				{
					if (isStreamEnd(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isNewline(c))
					{
						setState(TYPE_ILLEGAL);
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringEnd(c))
					{
						breakloop = true;
					}
					else if (isStringEscape(c))
					{
						c = readChar();
						if (isStringEnd(c))
							saveChar(c);
						else if (isStringEscape(c))
							saveChar(c);
						else switch (c)
						{
		    	        	case '0':
		    	        		saveChar('\0');
		    	        		break;
		    	        	case 'b':
		    	        		saveChar('\b');
		    	        	    break;
		    	        	case 't':
		    	        		saveChar('\t');
		    	        	    break;
		    	        	case 'n':
		    	        		saveChar('\n');
		    	        	    break;
		    	        	case 'f':
		    	        		saveChar('\f');
		    	        	    break;
		    	        	case 'r':
		    	        		saveChar('\r');
		    	        	    break;
		    	        	case '/':
		    	        		saveChar('/');
		    	        	    break;
		    	        	case 'u':
		    	        	{
		    	        		StringBuilder sb = new StringBuilder();
		    	        		for (int i = 0; i < 4; i++)
		    	        		{
		    	        			c = readChar();
		    						if (!isHexDigit(c))
		    						{
		    							setState(TYPE_ILLEGAL);
		    							setDelimBreak(c);
		    							breakloop = true;
		    						}
		    						else
		    							sb.append(c);
		    	        		}
		    	        		
		    	        		if (!breakloop)
		    	        		{
		    	        			saveChar((char)(Integer.parseInt(sb.toString(), 16) & 0x0ffff));
		    	        		}
		    	        	}
		    	        	    break;
		    	        	case 'x':
		    	        	{
		    	        		StringBuilder sb = new StringBuilder();
		    	        		for (int i = 0; i < 2; i++)
		    	        		{
		    	        			c = readChar();
		    						if (!isHexDigit(c))
		    						{
		    							setState(TYPE_ILLEGAL);
		    							setDelimBreak(c);
		    							breakloop = true;
		    						}
		    						else
		    							sb.append(c);
		    	        		}
		    	        		
		    	        		if (!breakloop)
		    	        		{
		    	        			saveChar((char)(Integer.parseInt(sb.toString(), 16) & 0x0ff));
		    	        		}
		    	        	}
		    	        	    break;
						}
					}
					else
					{
						saveChar(c);
					}
				}
					break; // end TYPE_STRING

				case TYPE_DELIMITER:
				{
					if (isStreamEnd(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (kernel.getCommentStartTable().containsKey(getCurrentLexeme()+c))
					{
						clearCurrentLexeme();
						setState(TYPE_COMMENT);
					}
					else if (kernel.getCommentLineTable().containsKey(getCurrentLexeme()+c))
					{
						clearCurrentLexeme();
						setState(TYPE_LINE_COMMENT);
					}
					else if (kernel.getDelimTable().containsKey(getCurrentLexeme()+c))
					{
						saveChar(c);
					}
					else if (isNewline(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpace(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isTab(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isWhitespace(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isSpecialStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else if (isStringStart(c))
					{
						setDelimBreak(c);
						breakloop = true;
					}
					else
					{
						setDelimBreak(c);
						breakloop = true;
					}
				}
					break; // end TYPE_DELIMITER
					
				case TYPE_COMMENT:
				{
					if (isStreamEnd(c))
					{
						clearCurrentLexeme();
						setState(TYPE_UNKNOWN);
					}
					else if (kernel.getCommentEndTable().containsKey(getCurrentLexeme()))
					{
						clearCurrentLexeme();
						setState(TYPE_UNKNOWN);
					}
					else if (isCommentEndDelimiterStart(c))
					{
						setState(TYPE_DELIM_COMMENT);
						saveChar(c);
					}
				}
					break; // end TYPE_COMMENT

				case TYPE_DELIM_COMMENT:
				{
					if (isStreamEnd(c))
					{
						clearCurrentLexeme();
						setState(TYPE_COMMENT);
					}
					else if (kernel.getCommentEndTable().containsKey(getCurrentLexeme()+c))
					{
						clearCurrentLexeme();
						setState(TYPE_UNKNOWN);
					}
					else if (isWhitespace(c))
					{
						clearCurrentLexeme();
						setState(TYPE_COMMENT);
					}
					else
					{
						clearCurrentLexeme();
						saveChar(c);
					}
				}
					break; // end TYPE_DELIM_COMMENT
					
				case TYPE_LINE_COMMENT:
				{
					if (isStreamEnd(c))
					{
						clearCurrentLexeme();
						setState(TYPE_UNKNOWN);
					}
					else if (isNewline(c))
					{
						clearCurrentLexeme();
						setState(TYPE_UNKNOWN);
					}
				}
					break; // end TYPE_DELIM_COMMENT
			}
			
		}

		// send token.
		int type = getState();
		String lexeme = getCurrentLexeme();
		clearCurrentLexeme();
		switch (getState())
		{
			case TYPE_DELIM_SPACE:
			{
				type = TYPE_DELIM_SPACE;
				lexeme = " ";
			}
				break;
			case TYPE_DELIM_TAB:
			{
				type = TYPE_DELIM_TAB;
				lexeme = "\t";
			}
				break;
			case TYPE_DELIM_NEWLINE:
			{
				type = TYPE_DELIM_NEWLINE;
				lexeme = "";
			}
				break;
			case TYPE_DELIMITER:
			{
				type = TYPE_DELIMITER;
				if (kernel.getCommentStartTable().containsKey(lexeme))
					type = kernel.getCommentStartTable().get(lexeme);
				else if (kernel.getCommentEndTable().containsKey(lexeme))
					type = kernel.getCommentEndTable().get(lexeme);
				else if (kernel.getCommentLineTable().containsKey(lexeme))
					type = kernel.getCommentLineTable().get(lexeme);
				else if (kernel.getDelimTable().containsKey(lexeme))
					type = kernel.getDelimTable().get(lexeme);
			}
				break;
			case TYPE_IDENTIFIER:
			{
				type = TYPE_IDENTIFIER;
				if (kernel.getKeywordTable().containsKey(lexeme))
					type = kernel.getKeywordTable().get(lexeme);
				else if (kernel.getCaseInsensitiveKeywordTable().containsKey(lexeme))
					type = kernel.getCaseInsensitiveKeywordTable().get(lexeme);
			}
				break;
			case TYPE_SPECIAL:
				type = specialType;
				break;
		}
		
		Token out = null;
		if (getState() != TYPE_END_OF_LEXER)
		{
			out = makeToken(type, lexeme);
			setState(TYPE_UNKNOWN);
		}
		
		if (DEBUG)
			System.out.println(out);
		return out;
	}
	
	/**
	 * Reads the next character.
	 */
	protected char readChar() throws IOException
	{
		if (streamStack.isEmpty())
			return END_OF_LEXER;

		return streamStack.peek().readChar();
	}

	/**
	 * Returns the current state.
	 */
	protected int getState()
	{
		return state;
	}
	
	/**
	 * Sets the current state.
	 */
	protected void setState(int state)
	{
		this.state = state;
	}
	
	/**
	 * Returns if we are in a delimiter break.
	 */
	protected boolean isOnDelimBreak()
	{
		return delimBreak;
	}
	
	/**
	 * Clears if we are in a delimiter break.
	 */
	protected boolean clearDelimBreak()
	{
		return delimBreak = false;
	}
	
	/**
	 * Sets if we are in a delimiter break.
	 */
	protected void setDelimBreak(char delimChar)
	{
		delimBreakChar = delimChar;
		delimBreak = true;
	}
	
	/**
	 * Saves a character for the next token.
	 */
	protected void saveChar(char c)
	{
		builder.append(c);
	}
	
	/**
	 * Sets the end character for a string.
	 */
	protected void setStringStartAndEnd(char c)
	{
		if (isStringStart(c))
			stringEnd = getStringEnd(c);
	}

	/**
	 * Returns true if this is a character that starts a String.
	 */
	protected void setSpecialType(char c)
	{
		if (isSpecialStart(c))
			specialType = getSpecialType(c);
	}

	/**
	 * Finishes the current token.
	 */
	protected String getCurrentLexeme()
	{
		return builder.toString();
	}

	/**
	 * Clears the current lexeme buffer.
	 */
	protected void clearCurrentLexeme()
	{
		builder.delete(0, builder.length());
	}

	/**
	 * Finishes the current token.
	 */
	protected Token makeToken(int type, String lexeme)
	{
		return new Token(streamStack.peek().streamName, lexeme, 
				streamStack.peek().line, streamStack.peek().lineNum, type);
	}

	/**
	 * Convenience method for Character.isLetter().
	 */
	protected boolean isUnderscore(char c)
	{
		return c == '_';
	}
	
	/**
	 * Convenience method for Character.isLetter().
	 */
	protected boolean isLetter(char c)
	{
		return Character.isLetter(c);
	}
	
	/**
	 * Convenience method for Character.isDigit().
	 */
	protected boolean isDigit(char c)
	{
		return Character.isDigit(c);
	}
	
	/**
	 * Returns true if this is a hex digit.
	 */
	protected boolean isHexDigit(char c)
	{
		return (c >= 0x0030 && c <= 0x0039) || 
			(c >= 0x0041 && c <= 0x0046) || 
			(c >= 0x0061 && c <= 0x0066);
	}
	
	/**
	 * Convenience method for Character.isWhitespace().
	 */
	protected boolean isWhitespace(char c)
	{
		return Character.isWhitespace(c);
	}

	/**
	 * Checks if is decimal point (depends on locale).
	 */
	protected boolean isPoint(char c)
	{
		return c == DEFAULT_DECIMAL_POINT;
	}
	
	/**
	 * Returns true if char is the exponent character in a number.
	 */
	protected boolean isExponent(char c)
	{
		return c == 'E' || c == 'e';
	}
	
	/**
	 * Returns true if char is the exponent sign character in a number.
	 */
	protected boolean isExponentSign(char c)
	{
		return c == '+' || c == '-';
	}
	
	/**
	 * Returns true if a char is a space.
	 */
	protected boolean isSpace(char c)
	{
		return c == ' ';
	}

	/**
	 * Returns true if a char is a tab.
	 */
	protected boolean isTab(char c)
	{
		return c == '\t';
	}

	/**
	 * Returns true if this is a character that is a String escape character.
	 */
	protected boolean isStringEscape(char c)
	{
		return c == '\\';
	}
	
	/**
	 * Returns true if this is a character that starts a String.
	 */
	protected boolean isStringStart(char c)
	{
		return kernel.getStringDelimTable().containsKey(c);
	}
	
	/**
	 * Returns true if this is a character that starts a special token.
	 */
	protected boolean isSpecialStart(char c)
	{
		return kernel.getSpecialDelimTable().containsKey(c);
	}
	
	/**
	 * Returns true if this is a character that ends a String.
	 */
	protected boolean isStringEnd(char c)
	{
		return stringEnd == c;
	}
	
	/**
	 * Returns the character that ends a String.
	 * Returns null character ('\0') if this does not end a string.
	 */
	protected char getStringEnd(char c)
	{
		if (!isStringStart(c))
			return '\0';
		return kernel.getStringDelimTable().get(c);
	}
	
	/**
	 * Gets the special type for a special char.
	 */
	protected int getSpecialType(char c)
	{
		return kernel.getSpecialDelimTable().get(c);
	}
	
	/**
	 * Returns true if this is a (or the start of a) delimiter character.
	 */
	protected boolean isDelimiterStart(char c)
	{
		return kernel.getDelimStartTable().contains(c);
	}
	
	/**
	 * Returns true if this is a (or the start of a) block-comment-ending delimiter character.
	 */
	protected boolean isCommentEndDelimiterStart(char c)
	{
		return kernel.getEndCommentDelimStartTable().contains(c);
	}
	
	/**
	 * Returns if a char equals END_OF_STREAM.
	 */
	protected boolean isStreamEnd(char c)
	{
		return c == END_OF_STREAM;
	}

	/**
	 * Returns if a char equals END_OF_LEXER.
	 */
	protected boolean isLexerEnd(char c)
	{
		return c == END_OF_LEXER;
	}

	/**
	 * Returns if a char equals NEWLINE.
	 */
	protected boolean isNewline(char c)
	{
		return c == NEWLINE;
	}

	/**
	 * Adds an error message to error list along with the current token's information
	 * (like line number, etc.).
	 */
	protected void addPreprocessorErrorMessage(String errorMessage, String lexeme)
	{
		String error = "("+getCurrentStreamName()+") " +
			"Line "+(streamStack.peek().lineNum)+
			", Token \""+lexeme+
			"\": "+errorMessage;
		
		preprocessorErrorList.add(error);
	}

	/**
	 * Returns a list of error messages.
	 */
	public String[] getErrorMessages()
	{
		String[] out = new String[preprocessorErrorList.size()];
		int i = 0;
		for (String s : preprocessorErrorList)
			out[i++] = s;
		return out;
	}
	

	/**
	 * Lexer token object.
	 */
	public static class Token
	{
		private String streamName;
		private String lexeme;
		private String lineText;
		private int tokenLine;
		private int type;
		
		protected Token(String streamName, String lexeme, String lineText, int tokenLine, int type)
		{
			this.streamName = streamName;
			this.lexeme = lexeme;
			this.lineText = lineText;
			this.tokenLine = tokenLine;
			this.type = type;
		}

		/**
		 * Returns the name of the 
		 * stream that this token came from.
		 */
		public String getStreamName()
		{
			return streamName;
		}

		/** Returns this token's lexeme. */
		public String getLineText()
		{
			return lineText;
		}

		/**
		 * Returns the line number within the 
		 * stream that this token appeared.
		 */
		public int getLine()
		{
			return tokenLine;
		}

		/** Returns this token's lexeme. */
		public String getLexeme()
		{
			return lexeme;
		}

		/** Returns this token's type. */
		public int getType()
		{
			return type;
		}
		
		/** Sets this token's type. */
		public void setType(int type)
		{
			this.type = type;
		}
		
		@Override
		public String toString()
		{
			return "TOKEN ("+streamName+") id: "+type+"\t Line: "+tokenLine+"\tLexeme: "+lexeme;
		}
	}
	
	/**
	 * Stream encapsulation object.
	 */
	protected class Stream
	{
		/** Name of the stream. */
		private String streamName;
		/** The InputStreamReader. */
		private BufferedReader reader;
		/** The current line. */
		private String line;
		/** The current line number. */
		private int lineNum;
		/** The current character number. */
		private int charNum;

		public Stream(String name, Reader in)
		{
			streamName = name;
			reader = new BufferedReader(in);
			line = null;
			lineNum = 0;
			charNum = 0;
		}
		
		public String getStreamName()
		{
			return streamName;
		}
		
		public char readChar() throws IOException
		{
			if (line == null || charNum == line.length())
			{
				
				line = reader.readLine();
				lineNum++;
				if (line != null)
					line += '\n';
				charNum = 0;
			}
			if (line != null)
				return line.charAt(charNum++); 
			else
				return END_OF_STREAM; 
		}
		
	}
	
}
