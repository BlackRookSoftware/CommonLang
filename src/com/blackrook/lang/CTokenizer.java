/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.blackrook.commons.linkedlist.Queue;

/**
 * Custom string tokenizer.
 * Will automatically tokenize Strings separated by whitespace and treats sets of
 * characters enclosed in quotes as one token. In those quoted strings, the common
 * escape characters may be used.
 * @author Matthew Tropiano
 * @deprecated Moved to Commons library for version 2.17.0 as com.blackrook.commons.CommonTokenizer.
 */
public class CTokenizer implements Iterable<String>
{
	/** Internal token list. */
	protected Queue<String> tokenList;
	
	/**
	 * Creates a new CTokenizer from a string.
	 * @param s		the string to tokenize.
	 */
	public CTokenizer(String s)
	{
		tokenList = parse(s);
	}
	
	/**
	 * Tokenizes a string, returns it as a linked list of Strings.
	 * @param s		the string to tokenize.
	 * @return		a linked list of the resulting strings.
	 */
	public static Queue<String> parse(String s)
	{
		Queue<String> out = new Queue<String>();
		
		StringBuilder tok = new StringBuilder();
		char[] c = s.toCharArray();
		
		for (int i = 0; i < c.length; i++)
		{
			if (c[i] == '"')
			{
				if (tok.length() > 0)
				{
					out.add(tok.toString());
					tok.delete(0, tok.length());
				}
				
				i++;
				while (i < c.length && c[i] != '"')
				{
					if (c[i] == '\\')
					{
						i++;
						switch (c[i])
						{
							case 'n':
								tok.append('\n');
								break;
							case '0':
								tok.append('\0');
								break;
							case 'r':
								tok.append('\r');
								break;
							case 'f':
								tok.append('\f');
								break;
							case 'b':
								tok.append('\b');
								break;
							case 't':
								tok.append('\t');
								break;
							case '"':
								tok.append('\"');
								break;
							case '\'':
								tok.append('\'');
								break;
							case '\\':
								tok.append('\\');
								break;
						}
					}
					else
						tok.append(c[i]);
					i++;
				}
				
				out.add(tok.toString());
				tok.delete(0, tok.length());
			}
			
			else if (c[i] > 32)
				tok.append(c[i]);
			else
			{
				if (tok.length() > 0)
				{
					out.add(tok.toString());
					tok.delete(0, tok.length());
				}				
			}
		}

		if (tok.length() > 0)
		{
			out.add(tok.toString());
			tok.delete(0, tok.length());
		}				
		
		return out;
	}
	
	/**
	 * Get the next token in the list of tokens.
	 * @return	the next token as a string.
	 * @throws NoSuchElementException	if there are no more strings left when this is called.
	 */
	public String nextToken() throws NoSuchElementException
	{
	    if (!hasMoreTokens()) throw new NoSuchElementException();
		return tokenList.dequeue();
	}

	/**
	 * Get the next token in the list of tokens as an int.
	 * @return	the next token as an int.
	 * @throws NoSuchElementException	if there are no more strings left when this is called.
	 * @throws NumberFormatException	if the next token cannot be parsed as an int.
	 */
	public int nextInt() throws NoSuchElementException
	{
		return Integer.parseInt(nextToken());
	}

	/**
	 * Get the next token in the list of tokens as a byte.
	 * @return	the next token as a byte.
	 * @throws NoSuchElementException	if there are no more strings left when this is called.
	 * @throws NumberFormatException	if the next token cannot be parsed as a byte.
	 */
	public byte nextByte() throws NoSuchElementException
	{
		return Byte.parseByte(nextToken());
	}

	/**
	 * Get the next token in the list of tokens as a short.
	 * @return	the next token as a short.
	 * @throws NoSuchElementException	if there are no more strings left when this is called.
	 * @throws NumberFormatException	if the next token cannot be parsed as a short.
	 */
	public short nextShort() throws NoSuchElementException
	{
		return Short.parseShort(nextToken());
	}

	/**
	 * Get the next token in the list of tokens as a long.
	 * @return	the next token as a long.
	 * @throws NoSuchElementException	if there are no more strings left when this is called.
	 * @throws NumberFormatException	if the next token cannot be parsed as a long.
	 */
	public long nextLong() throws NoSuchElementException
	{
		return Long.parseLong(nextToken());
	}

	/**
	 * Get the next token in the list of tokens as a float.
	 * @return	the next token as a float.
	 * @throws NoSuchElementException	if there are no more strings left when this is called.
	 * @throws NumberFormatException	if the next token cannot be parsed as a float.
	 */
	public float nextFloat() throws NoSuchElementException
	{
		return Float.parseFloat(nextToken());
	}

	/**
	 * Get the next token in the list of tokens as a double.
	 * @return	the next token as a double.
	 * @throws NoSuchElementException	if there are no more strings left when this is called.
	 * @throws NumberFormatException	if the next token cannot be parsed as a double.
	 */
	public double nextDouble() throws NoSuchElementException
	{
		return Double.parseDouble(nextToken());
	}
	
	/**
	 * Returns true if there are any tokens left. False if not.
	 */
	public boolean hasMoreTokens()
	{
		return !tokenList.isEmpty();
	}

	/**
	 * Add any ol' String to the tokenizer.
	 */
	public void add(String s)
	{
		tokenList.add(s);
	}

	/**
	 * Returns how many tokens are left in the tokenizer.
	 */
	public int size()
	{
		return tokenList.size();
	}
	
	/**
	 * Peek at the topmost token (but don't remove it).
	 */
	public String peek()
	{
		return tokenList.head();
	}
	
	/**
	 * Returns a new Iterator<String> for the rest of the tokens in the list.
	 */
	public Iterator<String> iterator()
	{
		return tokenList.iterator();
	}
}
