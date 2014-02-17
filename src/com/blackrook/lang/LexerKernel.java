/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang;

import java.text.DecimalFormatSymbols;

import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.list.SortedList;

/**
 * 
 * @author Matthew Tropiano
 * @since 2.3.0, the information that is used by the lexer in order to
 * scan text for individual tokens was separated out to this class.
 */
public class LexerKernel
{
	/**
	 * Table of single-character (or beginning character of) significant
	 * delimiters. Delimiters immediately break the current token if encountered.
	 */
	private SortedList<Character> delimStartTable;
	/**
	 * Table of single-character (or beginning character of) significant end comment
	 * delimiters.
	 */
	private SortedList<Character> endCommentDelimStartTable;
	/** 
	 * Table of significant delimiters.
	 */
	private HashMap<String, Integer> delimTable;
	/** 
	 * Table of comment-starting delimiters.
	 */
	private HashMap<String, Integer> commentStartTable;
	/** 
	 * Table of line-comment delimiters.
	 */
	private HashMap<String, Integer> commentLineTable;
	/** 
	 * Table of comment-ending delimiters.
	 */
	private HashMap<String, Integer> commentEndTable;
	/**
	 * Table of identifiers mapped to token ids (used for reserved keywords).
	 */
	private HashMap<String, Integer> keywordTable;
	/**
	 * Table of (case-insensitive) identifiers mapped to token ids (used for reserved keywords).
	 */
	private CaseInsensitiveHashMap<Integer> caseInsensitiveKeywordTable;
	/** 
	 * Table of string delimiters, or delimiters that cue the start of
	 * a string of characters. Each is paired with an ending delimiter.
	 * These take precedence over regular delimiters on scanning.
	 */
	private HashMap<Character, Character> stringDelimTable;
	/** 
	 * Table of special prefix delimiters, or delimiters that cue the start of
	 * special delimiter.
	 * These take precedence over regular delimiters and string delimiters on scanning.
	 */
	private HashMap<Character, Integer> specialDelimTable;

	/** Will this lexer add spaces as tokens? */
	private boolean includeSpaces;
	/** Will this lexer add tabs as tokens? */
	private boolean includeTabs;
	/** Will this lexer add newlines as tokens? */
	private boolean includeNewlines;
	/** Will this lexer add stream breaks as tokens? */
	private boolean includeStreamBreak;
	/** Decimal separator. */
	private char decimalSeparator;
	
	/**
	 * Creates a new, blank LexerKernel with default settings.
	 */
	public LexerKernel()
	{
		decimalSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator();
		delimStartTable = new SortedList<Character>();
		endCommentDelimStartTable = new SortedList<Character>();
		delimTable = new HashMap<String, Integer>();
		commentStartTable = new HashMap<String, Integer>(2);
		commentLineTable = new HashMap<String, Integer>(2);
		commentEndTable = new HashMap<String, Integer>(2);
		stringDelimTable = new HashMap<Character, Character>();
		specialDelimTable = new HashMap<Character, Integer>();
		keywordTable = new HashMap<String, Integer>();
		caseInsensitiveKeywordTable = new CaseInsensitiveHashMap<Integer>();
		
		includeSpaces = false;
		includeTabs = false;
		includeNewlines = false;
		includeStreamBreak = false;
	}

	/**
	 * Adds a delimiter to this lexer.
	 * @param delimiter		the delimiter lexeme.
	 * @param type			the type id.
	 * @throws IllegalArgumentException if type is < 0 or delimiter is null or empty.
	 */
	public void addDelimiter(String delimiter, int type)
	{
		typeCheck(type);
		keyCheck(delimiter);
		if (!delimStartTable.contains(delimiter.charAt(0)))
			delimStartTable.add(delimiter.charAt(0));
		delimTable.put(delimiter, type);
	}

	private void typeCheck(int type)
	{
		if (type < 0)
			throw new IllegalArgumentException("Type cannot be < 0.");
	}
	
	private void keyCheck(String name)
	{
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException("String cannot be null nor empty.");
	}

	/**
	 * Adds a string delimiter to this lexer along with its ending character.
	 * @param delimiterStart	the starting delimiter.
	 * @param delimiterEnd		the ending delimiter.
	 */
	public void addStringDelimiter(char delimiterStart, char delimiterEnd)
	{
		stringDelimTable.put(delimiterStart, delimiterEnd);
	}

	/**
	 * Adds a special prefix delimiter to this lexer, along with its type number.
	 * @param specialDelimiter	the starting delimiter.
	 * @param type				the type id.
	 */
	public void addSpecialDelimiter(char specialDelimiter, int type)
	{
		specialDelimTable.put(specialDelimiter, type);
	}

	/**
	 * Adds a comment-starting delimiter to this lexer.
	 * @param delimiter		the delimiter lexeme.
	 * @param type			the type id.
	 * @throws IllegalArgumentException if type is < 0 or delimiter is null or empty.
	 */
	public void addCommentStartDelimiter(String delimiter, int type)
	{
		addDelimiter(delimiter, type);
		commentStartTable.put(delimiter, type);
	}

	/**
	 * Adds a comment-ending delimiter to this lexer.
	 * @param delimiter		the delimiter lexeme.
	 * @param type			the type id.
	 * @throws IllegalArgumentException if type is < 0 or delimiter is null or empty.
	 */
	public void addCommentEndDelimiter(String delimiter, int type)
	{
		addDelimiter(delimiter, type);
		if (!endCommentDelimStartTable.contains(delimiter.charAt(0)))
			endCommentDelimStartTable.add(delimiter.charAt(0));
		commentEndTable.put(delimiter, type);
	}

	/**
	 * Adds a line comment delimiter to this lexer.
	 * @param delimiter		the delimiter lexeme.
	 * @param type			the type id.
	 * @throws IllegalArgumentException if type is < 0 or delimiter is null or empty.
	 */
	public void addCommentLineDelimiter(String delimiter, int type)
	{
		addDelimiter(delimiter, type);
		commentLineTable.put(delimiter, type);
	}

	/**
	 * Adds a keyword to the Lexer, case-sensitive. When this identifier is read in,
	 * its token type is specified type. 
	 * @param keyword	the keyword identifier.
	 * @param type		the type id.
	 */
	public void addKeyword(String keyword, int type)
	{
		typeCheck(type);
		keyCheck(keyword);
		keywordTable.put(keyword, type);
	}

	/**
	 * Adds a keyword to the Lexer, case-insensitive. 
	 * When this identifier is read in, its token type is specified type. 
	 * @param keyword	the keyword identifier.
	 * @param type		the type id.
	 */
	public void addCaseInsensitiveKeyword(String keyword, int type)
	{
		typeCheck(type);
		keyCheck(keyword);
		caseInsensitiveKeywordTable.put(keyword, type);
	}

	/** Will this lexer include space tokens? */
	public boolean willIncludeSpaces()
	{
		return includeSpaces;
	}

	/** Sets if this lexer includes space tokens? */
	public void setIncludeSpaces(boolean includeSpaces)
	{
		this.includeSpaces = includeSpaces;
	}

	/** Will this lexer include tab tokens? */
	public boolean willIncludeTabs()
	{
		return includeTabs;
	}

	/** Sets if this lexer includes tab tokens. */
	public void setIncludeTabs(boolean includeTabs)
	{
		this.includeTabs = includeTabs;
	}

	/** Will this lexer include newline tokens? */
	public boolean willIncludeNewlines()
	{
		return includeNewlines;
	}

	/** Sets if this lexer includes newline tokens. */
	public void setIncludeNewlines(boolean includeNewlines)
	{
		this.includeNewlines = includeNewlines;
	}

	/** Will this lexer include stream break tokens? */
	public boolean willIncludeStreamBreak()
	{
		return includeStreamBreak;
	}

	/** Sets if this lexer include stream break tokens. */
	public void setIncludeStreamBreak(boolean includeStreamBreak)
	{
		this.includeStreamBreak = includeStreamBreak;
	}

	/**
	 * Sets the current decimal separator character.
	 * By default, this is the current locale's decimal separator character.
	 * @param c the character to set.
	 */
	public void setDecimalSeparator(char c)
	{
		decimalSeparator = c;
	}

	/**
	 * Gets the current decimal separator character.
	 * By default, this is the current locale's decimal separator character.
	 */
	public char getDecimalSeparator()
	{
		return decimalSeparator;
	}

	SortedList<Character> getDelimStartTable()
	{
		return delimStartTable;
	}

	SortedList<Character> getEndCommentDelimStartTable()
	{
		return endCommentDelimStartTable;
	}

	HashMap<String, Integer> getDelimTable()
	{
		return delimTable;
	}

	HashMap<String, Integer> getCommentStartTable()
	{
		return commentStartTable;
	}

	HashMap<String, Integer> getCommentLineTable()
	{
		return commentLineTable;
	}

	HashMap<String, Integer> getCommentEndTable()
	{
		return commentEndTable;
	}

	HashMap<String, Integer> getKeywordTable()
	{
		return keywordTable;
	}

	CaseInsensitiveHashMap<Integer> getCaseInsensitiveKeywordTable()
	{
		return caseInsensitiveKeywordTable;
	}

	HashMap<Character, Character> getStringDelimTable()
	{
		return stringDelimTable;
	}

	HashMap<Character, Integer> getSpecialDelimTable()
	{
		return specialDelimTable;
	}

}
