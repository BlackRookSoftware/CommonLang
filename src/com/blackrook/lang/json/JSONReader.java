/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import com.blackrook.commons.linkedlist.Stack;
import com.blackrook.lang.Lexer;
import com.blackrook.lang.LexerKernel;
import com.blackrook.lang.Parser;

/**
 * A class for reading JSON data into {@link JSONObject}s. 
 * @author Matthew Tropiano
 */
public class JSONReader
{
	/**
	 * Reads in a new JSONObject from an InputStream.
	 * This does not close the stream after reading, and reads the first structure
	 * that it finds.
	 * @param in the input stream to read from.
	 * @throws IOException if the stream can't be read, or an error occurs.
	 * @throws JSONConversionException if a parsing error occurs, or the JSON is malformed.
	 */
	public static JSONObject readJSON(InputStream in) throws IOException
	{
		return readJSON(new InputStreamReader(in));
	}

	/**
	 * Reads in a new JSONObject from a Reader.
	 * This does not close the stream after reading, and reads the first structure
	 * that it finds.
	 * @param reader the reader to read from.
	 * @throws IOException if the stream can't be read, or a read error occurs.
	 * @throws JSONConversionException if a parsing error occurs, or the JSON is malformed.
	 */
	public static JSONObject readJSON(Reader reader) throws IOException
	{
		return (new ReaderContext(reader)).doRead();
	}

	/**
	 * Reads in a new JSONObject from a string of characters.
	 * @param data the string to read.
	 * @throws IOException if the string can't be read, or a read error occurs.
	 * @throws JSONConversionException if a parsing error occurs, or the JSON is malformed.
	 */
	public static JSONObject readJSON(String data) throws IOException
	{
		return readJSON(new StringReader(data));
	}

	/**
	 * Reads in a new object from an InputStream.
	 * This does not close the stream after reading, and reads the first structure
	 * that it finds and returns it as a new object converted from the JSON.
	 * @param in the input stream to read from.
	 * @throws IOException if the stream can't be read, or an error occurs.
	 * @throws JSONConversionException if a parsing error occurs, or the JSON is malformed.
	 * @since 2.5.1
	 */
	public static <T> T readJSON(Class<T> clazz, InputStream in) throws IOException
	{
		return readJSON(in).newObject(clazz);
	}

	/**
	 * Reads in a new object from a Reader.
	 * This does not close the stream after reading, and reads the first structure
	 * that it finds and returns it as a new object converted from the JSON.
	 * @param reader the reader to read from.
	 * @throws IOException if the stream can't be read, or a read error occurs.
	 * @throws JSONConversionException if a parsing error occurs, or the JSON is malformed.
	 * @since 2.5.1
	 */
	public static <T> T readJSON(Class<T> clazz, Reader reader) throws IOException
	{
		return readJSON(reader).newObject(clazz);
	}

	/**
	 * Reads in a new object from a string of characters and returns it as a 
	 * new object converted from the JSON.
	 * @param data the string to read.
	 * @throws IOException if the string can't be read, or a read error occurs.
	 * @throws JSONConversionException if a parsing error occurs, or the JSON is malformed.
	 * @since 2.5.1
	 */
	public static <T> T readJSON(Class<T> clazz, String data) throws IOException
	{
		return readJSON(data).newObject(clazz);
	}

	/**
	 * Lexer class for use in scanning text data for JSON data. 
	 */
	private static class JSONLexer extends Lexer
	{
		static final int TYPE_TRUE = 		0;
		static final int TYPE_FALSE = 		1;
		static final int TYPE_NULL = 		2;
		static final int TYPE_LBRACE = 		3;
		static final int TYPE_RBRACE = 		4;
		static final int TYPE_COLON = 		5;
		static final int TYPE_COMMA = 		6;
		static final int TYPE_LBRACK = 		7;
		static final int TYPE_RBRACK = 		8;
		static final int TYPE_MINUS = 		9;

		static final LexerKernel KERNEL = new LexerKernel()
		{{
			addStringDelimiter('"', '"');
			addStringDelimiter('\'', '\'');
			
			addDelimiter("{", TYPE_LBRACE);
			addDelimiter("}", TYPE_RBRACE);
			addDelimiter("[", TYPE_LBRACK);
			addDelimiter("]", TYPE_RBRACK);
			addDelimiter(":", TYPE_COLON);
			addDelimiter(",", TYPE_COMMA);
			addDelimiter("-", TYPE_MINUS);
			
			addKeyword("true", TYPE_TRUE);
			addKeyword("false", TYPE_FALSE);
			addKeyword("null", TYPE_NULL);
			
			setDecimalSeparator('.');
		}}; 
		
		JSONLexer(Reader in)
		{
			super(KERNEL, in);
		}
	}
	
	/**
	 * Reader context.
	 */
	private static class ReaderContext extends Parser
	{
		/** Member stack. */
		private Stack<String> currentMember;
		/** JSONObject Stack. */
		private Stack<JSONObject> currentObject;
		
		/** Reader context constructor. */
		ReaderContext(Reader reader)
		{
			super(new JSONLexer(reader));
			currentMember = new Stack<String>();
			currentObject = new Stack<JSONObject>();
		}
		
		/**
		 * Starts parse of JSON object.
		 */
		JSONObject doRead()
		{
			nextToken();

			if (Value())
				return currentObject.pop();
			
			String[] errors = getErrorMessages();
			if (errors.length > 0)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < errors.length; i++)
				{
					sb.append(errors[i]);
					if (i < errors.length-1)
						sb.append('\n');
				}
				throw new JSONConversionException(sb.toString());
			}
			
			return null;
		}
		
		// Value.
		private boolean Value()
		{
			if (currentType(JSONLexer.TYPE_MINUS))
			{
				nextToken();
				if (currentType(JSONLexer.TYPE_NUMBER))
				{
					if (!ParseNumber("-"+currentToken().getLexeme()))
						return false;
				}
				
				nextToken();
				return true;
			}
			else if (currentType(JSONLexer.TYPE_NUMBER))
			{
				if (!ParseNumber(currentToken().getLexeme()))
					return false;
				
				nextToken();
				return true;
			}
			else if (currentType(JSONLexer.TYPE_STRING))
			{
				currentObject.push(JSONObject.create(currentToken().getLexeme()));
				nextToken();
				return true;
			}
			else if (currentType(JSONLexer.TYPE_TRUE))
			{
				currentObject.push(JSONObject.create(Boolean.TRUE));
				nextToken();
				return true;
			}
			else if (currentType(JSONLexer.TYPE_FALSE))
			{
				currentObject.push(JSONObject.create(Boolean.FALSE));
				nextToken();
				return true;
			}
			else if (currentType(JSONLexer.TYPE_NULL))
			{
				currentObject.push(JSONObject.create(null));
				nextToken();
				return true;
			}
			else if (currentType(JSONLexer.TYPE_LBRACK))
			{
				nextToken();
				currentObject.push(JSONObject.createEmptyArray());
				if (!ArrayValue())
					return false;
				
				if (!matchType(JSONLexer.TYPE_RBRACK))
				{
					addErrorMessage("Expected ']'");
					return false;
				}
				
				return true;
			}
			else if (currentType(JSONLexer.TYPE_LBRACE))
			{
				nextToken();
				currentObject.push(JSONObject.createEmptyObject());
				if (!ObjectValue())
					return false;
				
				if (!matchType(JSONLexer.TYPE_RBRACE))
				{
					addErrorMessage("Expected '}'");
					return false;
				}
				
				return true;
			}
			
			addErrorMessage("Expected value.");
			return false;
		}
		
		/**
		 * ArrayValue := 	Value ArrayBody
		 * 					[e]
		 */
		private boolean ArrayValue()
		{
			if (Value())
			{
				JSONObject json = currentObject.pop();
				currentObject.peek().append(json);
				return ArrayBody();
			}
			
			return true;
		}
		
		/**
		 * ArrayBody := 	"," Value ArrayBody
		 * 					[e]
		 */
		private boolean ArrayBody()
		{
			if (currentType(JSONLexer.TYPE_COMMA))
			{
				nextToken();
				
				if (Value())
				{
					JSONObject json = currentObject.pop();
					currentObject.peek().append(json);
					return ArrayBody();
				}
				else
					return false;
			}
			
			return true;
		}
		
		/**
		 * ObjectValue := 	STRING ":" Value ObjectBody
		 * 					[e]
		 */
		private boolean ObjectValue()
		{
			if (currentType(JSONLexer.TYPE_STRING, JSONLexer.TYPE_IDENTIFIER))
			{
				currentMember.push(currentToken().getLexeme());
				nextToken();
				
				if (!matchType(JSONLexer.TYPE_COLON))
				{
					addErrorMessage("Expected ':'");
					return false;
				}
				
				if (Value())
				{
					JSONObject json = currentObject.pop();
					currentObject.peek().addMember(currentMember.pop(), json);
					return ObjectBody();
				}
				else
					return false;
			}
			
			return true;
		}
		
		/**
		 * ObjectBody := 	"," STRING ":" Value ObjectBody
		 * 					[e]
		 */
		private boolean ObjectBody()
		{
			if (currentType(JSONLexer.TYPE_COMMA))
			{
				nextToken();
				
				if (!currentType(JSONLexer.TYPE_STRING, JSONLexer.TYPE_IDENTIFIER))
				{
					addErrorMessage("Expected member name (string or identifier).");
					return false;
				}

				currentMember.push(currentToken().getLexeme());
				nextToken();
				
				if (!matchType(JSONLexer.TYPE_COLON))
				{
					addErrorMessage("Expected ':'");
					return false;
				}
				
				if (Value())
				{
					JSONObject json = currentObject.pop();
					currentObject.peek().addMember(currentMember.pop(), json);
					return ObjectBody();
				}
			}
			
			return true;
		}
		
		// Parses a validated number.
		private boolean ParseNumber(String s)
		{
			int idx = s.indexOf("x");
			int edx = Math.max(s.indexOf("e"), s.indexOf("E"));
			int pdx = -1;
			if (edx >= 0) 
				pdx = Math.max(edx, s.indexOf("+"));
			
			int fdx = s.indexOf(".");
			if (idx >= 0)
			{
				String lng = s.substring(idx + 1);
				try {
					currentObject.push(JSONObject.create(Long.parseLong(lng, 16)));
				} catch (NumberFormatException n) {
					addErrorMessage("Malformed number.");
					return false;
				}
			}
			else if (edx >= 0)
			{
				String num = s.substring(0, edx);
				String exp = s.substring(pdx + 1);
				try {
					currentObject.push(JSONObject.create(Double.parseDouble(num) * Math.pow(10, Double.parseDouble(exp))));
				} catch (NumberFormatException n) {
					addErrorMessage("Malformed number.");
					return false;
				}
			}
			else if (fdx >= 0)
			{
				try {
					currentObject.push(JSONObject.create(Double.parseDouble(s.toString())));
				} catch (NumberFormatException n) {
					addErrorMessage("Malformed number.");
					return false;
				}
			}
			else
			{
				try {
					currentObject.push(JSONObject.create(Long.parseLong(s.toString())));
				} catch (NumberFormatException n) {
					addErrorMessage("Malformed number.");
					return false;
				}
			}
			return true;
		}
		
	}

}
