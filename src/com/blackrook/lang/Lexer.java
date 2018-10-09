/*******************************************************************************
 * Copyright (c) 2009-2016 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import com.blackrook.commons.util.IOUtils;
import com.blackrook.commons.util.ValueUtils;
import com.blackrook.lang.ReaderStack.Stream;

import static com.blackrook.lang.LexerKernel.*;

/**
 * Class used for breaking up a stream of characters into lexicographical tokens.
 * Spaces, newlines, tabs, and breaks in the stream are added if desired,
 * otherwise, they are stripped out. 
 * <p>
 * Special delimiter characters take precedence over String delimiters.
 * String delimiter characters take precedence over regular delimiters.
 * Delimiter characters take parsing priority over other characters.
 * Delimiter evaluation priority goes: CommentDelimiter, Delimiter.
 * Identifier evaluation priority goes: Keyword, CaseInsensitiveKeyword, Identifier.
 * <p>
 * The Lexer will also automatically manipulate {@link ReaderStack}s once it reaches the end of a stream.
 * Other implementations of this class may manipulate the stack as well (such as ones that do in-language stream inclusion).
 * <p>
 * If the system property <code>com.blackrook.lang.Lexer.debug</code> is set to <code>true</code>, this does debugging output to {@link System#out}.
 * 
 * @author Matthew Tropiano
 * @since 2.3.0, this now supports integers/floating-point numbers with exponent notation,
 * and separates the configuration for how Lexers scan for information with {@link LexerKernel}.
 * @since 2.10.0, this had its stream stack separated out into a separate object.
 */
public class Lexer
{
	public static boolean DEBUG = ValueUtils.parseBoolean(System.getProperty(Lexer.class.getName()+".debug"), false);
	
	/** Lexer end-of-stream char. */
	public static final char END_OF_LEXER = '\uffff';
	/** Lexer newline char. */
	public static final char NEWLINE = '\n';
	
	/** The current stream stack. */
	private ReaderStack readerStack;
	/** The lexer kernel to use. */
	private LexerKernel kernel;
	/** The current state. */
	private int state;
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
	 * @param kernel the lexer kernel to use for defining how to parse the input text.
	 * @param name the name of this lexer.
	 * @param in the reader to read from.
	 */
	public Lexer(LexerKernel kernel, String name, String in)
	{
		this(kernel, name, new StringReader(in));
	}
	
	/**
	 * Creates a new lexer around a reader.
	 * This will also assign this lexer a default name.
	 * @param kernel the kernel to use for this lexer.
	 * @param in the reader to read from.
	 */
	public Lexer(LexerKernel kernel, Reader in)
	{
		this(kernel, null, in);
	}
	
	/**
	 * Creates a new lexer around a reader.
	 * @param kernel the kernel to use for this lexer.
	 * @param name the name of this lexer.
	 * @param in the reader to read from.
	 */
	public Lexer(LexerKernel kernel, String name, Reader in)
	{
		this.kernel = kernel;
		readerStack = new ReaderStack();
		state = TYPE_UNKNOWN;
		builder = new StringBuilder();
		pushStream(name, in);
	}
	
	/**
	 * Creates a new lexer around a reader stack.
	 * @param kernel the kernel to use for this lexer.
	 * @param readerStack the {@link ReaderStack} to use for this Lexer.
	 * @since 2.10.0
	 */
	public Lexer(LexerKernel kernel, ReaderStack readerStack)
	{
		this.kernel = kernel;
		this.readerStack = readerStack;
		state = TYPE_UNKNOWN;
		builder = new StringBuilder();
	}
	
	/**
	 * @return the reference to the lexer's reader stack (be careful with this!).
	 * @since 2.10.0
	 */
	public ReaderStack getReaderStack()
	{
		return readerStack;
	}
	
	/**
	 * @return the lexer's current stream name.
	 */
	public String getCurrentStreamName()
	{
		if (readerStack.isEmpty())
			return "LEXER END";
		return readerStack.peek().getStreamName();
	}

	/**
	 * Gets the lexer's current stream's line number.
	 * @return the lexer's current stream's line number, or -1 if at Lexer end.
	 */
	public int getCurrentLine()
	{
		if (readerStack.isEmpty())
			return -1;
		return readerStack.peek().getLineNum();
	}

	/**
	 * Pushes a stream onto the encapsulated reader stack.
	 * @param name the name of the stream.
	 * @param in the reader reader.
	 */
	public void pushStream(String name, Reader in)
	{
		readerStack.push(name, in);
	}
	
	/**
	 * Gets the current stream.
	 * @return the name of the current stream.
	 */
	public Stream currentStream()
	{
		return readerStack.peek();
	}
	
	/**
	 * Gets the next token.
	 * If there are no tokens left to read, this will return null.
	 * @return the next token, or null if no more tokens to read.
	 * @throws IOException if a token cannot be read by the underlying Reader.
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
					break;
				}

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
						IOUtils.close(readerStack.pop());
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
					break; // end TYPE_START_OF_LEXER
				}
				
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

					break; // end TYPE_ILLEGAL
				}

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
					break; // end TYPE_POINT
				}
					
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
					break; // end TYPE_FLOAT
				}
					
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
					break; // end TYPE_IDENTIFIER
				}
					
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
						saveChar(c);
					}
					else if (isStringStart(c))
					{
						saveChar(c);
					}
					else if (isDelimiterStart(c))
					{
						saveChar(c);
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
					break; // end TYPE_IDENTIFIER
				}
					
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
					break; // end TYPE_HEX_INTEGER0
				}

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
					break; // end TYPE_HEX_INTEGER1
				}

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
					break; // end TYPE_HEX_INTEGER
				}

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
					break; // end TYPE_NUMBER
				}

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
					break; // end TYPE_EXPONENT
				}
					
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
					break; // end TYPE_EXPONENT_POWER
				}

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
					break; // end TYPE_STRING
				}

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
					break; // end TYPE_DELIMITER
				}
					
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
					break; // end TYPE_COMMENT
				}

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
					break; // end TYPE_DELIM_COMMENT
				}
					
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
					break; // end TYPE_DELIM_COMMENT
				}
				
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
				break;
			}
			
			case TYPE_DELIM_TAB:
			{
				type = TYPE_DELIM_TAB;
				lexeme = "\t";
				break;
			}
			
			case TYPE_DELIM_NEWLINE:
			{
				type = TYPE_DELIM_NEWLINE;
				lexeme = "";
				break;
			}
			
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
				break;
			}
			
			case TYPE_IDENTIFIER:
			{
				type = TYPE_IDENTIFIER;
				if (kernel.getKeywordTable().containsKey(lexeme))
					type = kernel.getKeywordTable().get(lexeme);
				else if (kernel.getCaseInsensitiveKeywordTable().containsKey(lexeme))
					type = kernel.getCaseInsensitiveKeywordTable().get(lexeme);
				break;
			}
			
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
	 * @return the character read, or {@link #END_OF_LEXER} if no more characters, or {@link ReaderStack#END_OF_STREAM} if end of current stream.
	 * @throws IOException if a token cannot be read by the underlying Reader.
	 */
	protected char readChar() throws IOException
	{
		if (readerStack.isEmpty())
			return END_OF_LEXER;

		return readerStack.readChar();
	}

	/**
	 * @return the current state.
	 */
	protected int getState()
	{
		return state;
	}
	
	/**
	 * Sets the current state.
	 * @param state the new state.
	 */
	protected void setState(int state)
	{
		this.state = state;
	}
	
	/**
	 * @return if we are in a delimiter break.
	 */
	protected boolean isOnDelimBreak()
	{
		return delimBreak;
	}
	
	/**
	 * Clears if we are in a delimiter break.
	 */
	protected void clearDelimBreak()
	{
		delimBreak = false;
	}
	
	/**
	 * Sets if we are in a delimiter break.
	 * @param delimChar the delimiter character that starts the break.
	 */
	protected void setDelimBreak(char delimChar)
	{
		delimBreakChar = delimChar;
		delimBreak = true;
	}
	
	/**
	 * Saves a character for the next token.
	 * @param c the character to save into the current token.
	 */
	protected void saveChar(char c)
	{
		builder.append(c);
	}
	
	/**
	 * Sets the end character for a string.
	 * @param c the character to set.
	 */
	protected void setStringStartAndEnd(char c)
	{
		if (isStringStart(c))
			stringEnd = getStringEnd(c);
	}

	/**
	 * Sets and looks up the special type using a delimiter character.
	 * @param c the character to use.
	 */
	protected void setSpecialType(char c)
	{
		if (isSpecialStart(c))
			specialType = getSpecialType(c);
	}

	/**
	 * Gets the current token lexeme.
	 * @return the current contents of the token lexeme builder buffer. 
	 */
	protected String getCurrentLexeme()
	{
		return builder.toString();
	}

	/**
	 * Clears the current token lexeme buffer.
	 */
	protected void clearCurrentLexeme()
	{
		builder.delete(0, builder.length());
	}

	/**
	 * Creates a new token using the current stream name, line, and line number.
	 * @param type the token type to apply.
	 * @param lexeme the token's lexeme.
	 * @return a new Token object.
	 */
	protected Token makeToken(int type, String lexeme)
	{
		return new Token(readerStack.peek().getStreamName(), lexeme, readerStack.peek().getLine(), readerStack.peek().getLineNum(), type);
	}

	/**
	 * Convenience method for <code>c == '_'</code>.
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isUnderscore(char c)
	{
		return c == '_';
	}
	
	/**
	 * Convenience method for {@link Character#isLetter(char)}.
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isLetter(char c)
	{
		return Character.isLetter(c);
	}
	
	/**
	 * Convenience method for {@link Character#isDigit(char)}.
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isDigit(char c)
	{
		return Character.isDigit(c);
	}
	
	/**
	 * Returns true if this is a hex digit (0-9, A-F, a-f).
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isHexDigit(char c)
	{
		return (c >= 0x0030 && c <= 0x0039) || 
			(c >= 0x0041 && c <= 0x0046) || 
			(c >= 0x0061 && c <= 0x0066);
	}
	
	/**
	 * Convenience method for {@link Character#isWhitespace(char)}.
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isWhitespace(char c)
	{
		return Character.isWhitespace(c);
	}

	/**
	 * Checks if a character is a decimal point (depends on locale/kernel).
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isPoint(char c)
	{
		return kernel.getDecimalSeparator() == c;
	}
	
	/**
	 * Checks if char is the exponent character in a number.
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isExponent(char c)
	{
		return c == 'E' || c == 'e';
	}
	
	/**
	 * Checks if char is the exponent sign character in a number.
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isExponentSign(char c)
	{
		return c == '+' || c == '-';
	}
	
	/**
	 * Checks if a char is a space.
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isSpace(char c)
	{
		return c == ' ';
	}

	/**
	 * Checks if a char is a tab.
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isTab(char c)
	{
		return c == '\t';
	}

	/**
	 * Checks if this is a character that is a String escape character.
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isStringEscape(char c)
	{
		return c == '\\';
	}
	
	/**
	 * Checks if this is a character that starts a String.
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isStringStart(char c)
	{
		return kernel.getStringDelimTable().containsKey(c);
	}
	
	/**
	 * Checks if this is a character that starts a special token.
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isSpecialStart(char c)
	{
		return kernel.getSpecialDelimTable().containsKey(c);
	}
	
	/**
	 * Checks if this is a character that ends a String.
	 * @param c the character to test.
	 * @return true if so, false if not.
	 */
	protected boolean isStringEnd(char c)
	{
		return stringEnd == c;
	}
	
	/**
	 * Gets the character that ends a String, using the starting character.
	 * @param c the starting character.
	 * @return the corresponding end character, or the null character ('\0') if this does not end a string.
	 */
	protected char getStringEnd(char c)
	{
		if (!isStringStart(c))
			return '\0';
		return kernel.getStringDelimTable().get(c);
	}
	
	/**
	 * Gets the special type for a special char.
	 * @param c the character input.
	 * @return the corresponding type, or {@link LexerKernel#TYPE_UNKNOWN} if no type.
	 */
	protected int getSpecialType(char c)
	{
		if (!kernel.getSpecialDelimTable().containsKey(c))
			return TYPE_UNKNOWN;
		return kernel.getSpecialDelimTable().get(c);
	}
	
	/**
	 * Checks if this is a (or the start of a) delimiter character.
	 * @param c the character input.
	 * @return true if so, false if not.
	 */
	protected boolean isDelimiterStart(char c)
	{
		return kernel.getDelimStartTable().contains(c);
	}
	
	/**
	 * Checks if this is a (or the start of a) block-comment-ending delimiter character.
	 * @param c the character input.
	 * @return true if so, false if not.
	 */
	protected boolean isCommentEndDelimiterStart(char c)
	{
		return kernel.getEndCommentDelimStartTable().contains(c);
	}
	
	/**
	 * Checks if a char equals {@link ReaderStack#END_OF_STREAM}.
	 * @param c the character input.
	 * @return true if so, false if not.
	 */
	protected boolean isStreamEnd(char c)
	{
		return c == ReaderStack.END_OF_STREAM;
	}

	/**
	 * Checks if a char equals {@link #END_OF_LEXER}.
	 * @param c the character input.
	 * @return true if so, false if not.
	 */
	protected boolean isLexerEnd(char c)
	{
		return c == END_OF_LEXER;
	}

	/**
	 * Checks if a char equals {@link #NEWLINE}.
	 * @param c the character input.
	 * @return true if so, false if not.
	 */
	protected boolean isNewline(char c)
	{
		return c == NEWLINE;
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
		 * @return the name of the stream that this token came from.
		 */
		public String getStreamName()
		{
			return streamName;
		}

		/** 
		 * @return this token's lexeme. 
		 */
		public String getLineText()
		{
			return lineText;
		}

		/**
		 * @return the line number within the stream that this token appeared.
		 */
		public int getLine()
		{
			return tokenLine;
		}

		/** @return this token's lexeme. */
		public String getLexeme()
		{
			return lexeme;
		}

		/** @return this token's type. */
		public int getType()
		{
			return type;
		}
		
		/** 
		 * Sets this token's type.
		 * @param type a type corresponding to a type in the lexer or lexer kernel.
		 */
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
	
}
