/*******************************************************************************
 * Copyright (c) 2009-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

import com.blackrook.commons.Sizable;
import com.blackrook.commons.linkedlist.Stack;

/**
 * This holds a series of {@link Reader} streams such that the stream on top is the current active stream.
 * This was separated out of {@link Lexer} to mix different Lexers on one stack (different lexers, same stream).
 * It is the user's responsibility to {@link #pop()} streams off of the stack when they reach an end. 
 * @author Matthew Tropiano
 * @since 2.10.0
 */
public class ReaderStack implements Sizable
{
	/** Lexer end-of-stream char. */
	public static final char END_OF_STREAM = '\ufffe';

	/** Stream stack. */
	private Stack<Stream> innerStack;

	/**
	 * Creates a new empty ReaderStack. 
	 */
	public ReaderStack()
	{
		innerStack = new Stack<>();
	}
	
	/**
	 * Creates a new ReaderStack. 
	 * @param name the name to give the first reader.
	 * @param reader the reader itself (assumed open).
	 */
	public ReaderStack(String name, Reader reader)
	{
		this();
		push(name, reader);
	}
	
	/**
	 * Pushes another reader onto the stack.
	 * @param name the name to give this reader.
	 * @param reader the reader itself (assumed open).
	 */
	public final void push(String name, Reader reader)
	{
		innerStack.push(new Stream(name, reader));
	}
	
	/**
	 * Gets the reference to the topmost (current) stream.
	 * @return the topmost stream.
	 */
	public final Stream peek()
	{
		return innerStack.peek();
	}
	
	/**
	 * Pops a {@link Stream} off of the top of this stack.
	 * @return the removed {@link Stream}, or null if empty.
	 * @see #isEmpty()
	 */
	public final Stream pop()
	{
		return innerStack.pop();
	}
	
	/**
	 * @return the current stream name, or null if empty.
	 * @see #isEmpty()
	 */
	public final String getCurrentStreamName()
	{
		if (isEmpty())
			return null;
		return peek().getStreamName();
	}

	/**
	 * Gets the current stream's line number.
	 * This is affected by what is at the top of the stream.
	 * NOTE: Line numbers start at 1.
	 * @return the current stream's line number, or -1 if empty.
	 * @see #isEmpty()
	 */
	public final int getCurrentLine()
	{
		if (isEmpty())
			return -1;
		return peek().getLineNum();
	}

	@Override
	public int size()
	{
		return innerStack.size();
	}

	@Override
	public boolean isEmpty()
	{
		return innerStack.isEmpty();
	}

	/**
	 * Reads the next character.
	 * @return the character read, or {@link ReaderStack#END_OF_STREAM} if end of current stream.
	 * @throws IOException if a token cannot be read by the topmost Reader.
	 */
	protected char readChar() throws IOException
	{
		return peek().readChar();
	}

	/**
	 * Stream encapsulation of a single named Reader.
	 * Also holds current line, character number, line number.
	 */
	public class Stream implements Closeable
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

		/**
		 * Creates a new stream.
		 * @param name the stream name.
		 * @param in the reader used.
		 */
		private Stream(String name, Reader in)
		{
			streamName = name;
			reader = new BufferedReader(in);
			line = null;
			lineNum = 0;
			charNum = 0;
		}
		
		/**
		 * @return the stream name.
		 */
		public String getStreamName()
		{
			return streamName;
		}
		
		/**
		 * @return the contents of the current line of this stream.
		 */
		public String getLine()
		{
			return line;
		}
		
		/**
		 * @return the current line number of this stream.
		 */
		public int getLineNum()
		{
			return lineNum;
		}
		
		/**
		 * Reads a character from the stream.
		 * @return the read character or {@link #END_OF_STREAM} if no more characters.
		 * @throws IOException if a character cannot be read.
		 */
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

		@Override
		public void close() throws IOException
		{
			reader.close();
		}
		
	}
	
}
