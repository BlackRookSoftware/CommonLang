/*******************************************************************************
 * Copyright (c) 2009-2016 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.path;

import java.io.File;

import com.blackrook.commons.list.List;
import com.blackrook.commons.util.ObjectUtils;

/**
 * A pattern that is born from an Apache-like path string (with wildcards).
 * Each Apache path pattern is DOS/UNIX-like, insomuch that stars (*) and question marks
 * (?) replace one or more characters lazily and single characters, respectively. Directories
 * are separated by slashes or backslashes. File separators are slashes or backslashes (/ or \).
 * <p>
 * Each wildcard is as follows:
 * <table summary="Wildcard Examples">
 * <tr><td>*</td><td>Match zero or more characters that is not a path separator.</td></tr>
 * <tr><td>?</td><td>Match one character that is not a path separator.</td></tr>
 * <tr><td>**</td><td>Match zero or more whole folders, lazily. Must be the only set of characters in a separated path.</td></tr>
 * </table>
 * <p>
 * Patterns that consist of a single path node matches only the end of the path, as though
 * they are preceeded with "&#42;&#42;/".
 * <p>
 * Examples of patterns:
 * <table summary="Pattern Examples">
 * <tr><td>*.jsp</td><td>Match all paths that end in ".jsp".</td></tr>
 * <tr><td>apple/*.jsp</td><td>Match paths that end in ".jsp" under "apple".</td></tr>
 * <tr><td>apple/&#42;&#42;/orange/*.jsp</td><td>Match paths that end in ".jsp" under paths "apple/pear/orange" or "apple/pear/lemon/orange".</td></tr>
 * <tr><td>?.htm</td><td>Matches "a.htm" or "b.htm" and so forth.</td></tr>
 * <tr><td>pear/a?b.htm</td><td>Matches "aab.htm" or "abb.htm" and so forth under path "pear".</td></tr>
 * <tr><td>pear/a*b.htm</td><td>Matches "aab.htm" or "aklfalskdjfb.htm" and so forth under path "pear".</td></tr>
 * </table>
 * @author Matthew Tropiano
 */
public class PathPattern 
{
	/** Any directory path. */
	private static final String ANY_DIRECTORY_PATH = "**";
	
	/** The nodes in the compiled pattern. */
	private List<Node> patternNodes;
	/** Generated hashcode (patterns are immutable, so this works. */
	private int hashCode;
	
	// private constructor
	private PathPattern()
	{
		patternNodes = new List<Node>();
		hashCode = 0;
	}
	
	/**
	 * Creates a new path pattern from a path wildcard string.
	 * @param pathPattern the pattern to compile.
	 * @return a new PathPattern that represents the compiled pattern.
	 * @throws PatternParseException if the syntax is incorrect for this pattern.
	 */
	public static PathPattern compile(String pathPattern)
	{
		final char SEPARATOR = File.separatorChar;
		
		if (ObjectUtils.isEmpty(pathPattern))
			throw new PatternParseException("Input pattern cannot be empty or null.");

		if (pathPattern.charAt(pathPattern.length() - 1) == '/' || pathPattern.charAt(pathPattern.length() - 1) == SEPARATOR)
			throw new PatternParseException("Input pattern cannot end with a file separator.");
		
		PathPattern out = new PathPattern();

		int start = 0;
		int end = 0;
		
		while (start < pathPattern.length())
		{
			end = scanNextFolder(pathPattern, start);
			
			String file = pathPattern.substring(start, end);

			if (end == pathPattern.length())
			{
				if (ObjectUtils.isEmpty(file))
					throw new PatternParseException("A file cannot have an empty name or pattern."); 
				else if (ANY_DIRECTORY_PATH.equals(file))
					out.patternNodes.add(new Node(Node.Type.ANY_DIRECTORY, file));
				else if (file.contains(ANY_DIRECTORY_PATH))
					throw new PatternParseException("File pattern cannot contain the lazy directory matcher."); 
				else
					out.patternNodes.add(new Node(Node.Type.FILE, file));
			}
			else
			{
				if (ObjectUtils.isEmpty(file))
					throw new PatternParseException("A path folder cannot have an empty name or pattern."); 
				else if (ANY_DIRECTORY_PATH.equals(file))
					out.patternNodes.add(new Node(Node.Type.ANY_DIRECTORY, file));
				else if (file.contains(ANY_DIRECTORY_PATH))
					throw new PatternParseException("A path folder, if it is the lazy directory matcher, must be the only pattern in that part of the path."); 
				else
					out.patternNodes.add(new Node(Node.Type.DIRECTORY, file));
				
				end++;
			}
			
			start = end;
		}
		
		if (out.patternNodes.size() == 1 && out.patternNodes.getByIndex(0).type != Node.Type.ANY_DIRECTORY)
			out.patternNodes.add(0, new Node(Node.Type.ANY_DIRECTORY, ANY_DIRECTORY_PATH));
		
		out.hashCode = 0;
		for (Node n : out.patternNodes)
			out.hashCode += n.hashCode();
		
		return out;
	}

	/**
	 * Tests if a path matches this pattern - case-sensitive. 
	 * @param path the input path.
	 * @return true if the path matches this pattern, false if not.
	 */
	public boolean matches(String path)
	{
		return matches(path, false);
	}
	
	/**
	 * Tests if a path matches this pattern. 
	 * @param path the input path.
	 * @param caseInsensitive if true, letter case does not factor into inequality.
	 * @return true if the path matches this pattern, false if not.
	 */
	public boolean matches(String path, boolean caseInsensitive)
	{
		if (ObjectUtils.isEmpty(path))
			return false;

		int start = 0;
		int end = 0;
		int n = 0;
		
		while (n < patternNodes.size() && start < path.length())
		{
			Node currentNode = patternNodes.getByIndex(n);
			Node nextNode = n < patternNodes.size() - 1 ? patternNodes.getByIndex(n + 1) : null; 

			end = scanNextFolder(path, start);
			String file = path.substring(start, end);

			if (end == path.length())
			{
				if (ObjectUtils.isEmpty(file))
					throw new PatternParseException("The target path cannot have an empty file/folder name."); 
				else if (file.contains("*") || file.contains("?"))
					throw new PatternParseException("The target path cannot contain any wildcard characters."); 
				else if (currentNode.type == Node.Type.ANY_DIRECTORY)
				{
					if (nextNode != null && matchFile(nextNode.pattern, file, caseInsensitive))
						n++;
					else
						return false;
				}
				else if (currentNode.type == Node.Type.FILE)
				{
					if (matchFile(currentNode.pattern, file, caseInsensitive))
					{
						start = end;
						n++;
					}
					else
						return false;
				}
				else if (currentNode.type == Node.Type.DIRECTORY)
					return false;
				else
					return false;
			}
			else
			{
				if (ObjectUtils.isEmpty(file))
					throw new PatternParseException("The target path cannot have an empty file/folder name."); 
				else if (file.contains("*") || file.contains("?"))
					throw new PatternParseException("The target path cannot contain any wildcard characters."); 
				else if (currentNode.type == Node.Type.ANY_DIRECTORY)
				{
					if (nextNode != null)
					{
						if (matchFile(nextNode.pattern, file, caseInsensitive))
							n++;
						else
							start = end + 1;
					}
					else
						n++;
				}
				else if (currentNode.type == Node.Type.DIRECTORY)
				{
					if (matchFile(currentNode.pattern, file, caseInsensitive))
					{
						start = end + 1;
						n++;
					}
					else
						return false;
				}
				else if (currentNode.type == Node.Type.FILE)
					return false;
				else
					return false;
			}
		}
		
		return start == end && n == patternNodes.size();
	}

	// Returns true if a pattern matches a filename.
	static boolean matchFile(String pattern, String target, boolean caseInsensitive)
	{
		if ("*".equals(pattern))
			return true;
		
		if (pattern.length() == 0 && target.length() == 0)
			return true;
		
		final char ANY_ALL_CHAR = '*';
		final char ANY_ONE_CHAR = '?';

		int pi = 0;
		int ti = 0;
		int plen = pattern.length();
		int tlen = target.length();
		
		while (pi < plen && ti < tlen)
		{
			char p = pattern.charAt(pi);
			char t = target.charAt(ti);
			if (p != ANY_ALL_CHAR)
			{
				if (p == ANY_ONE_CHAR)
				{
					if (t == '/' || t == File.separatorChar)
						return false;
					else
					{
						pi++; 
						ti++;						
					}
				}
				else if (p == t)
				{
					pi++; 
					ti++;
				}
				else if (caseInsensitive && Character.toLowerCase(p) == Character.toLowerCase(t))
				{
					pi++; 
					ti++;
				}
				else if ((p == '/' || p == File.separatorChar) && (t == '/' || t == File.separatorChar))
				{
					pi++; 
					ti++;
				}
				else
					return false;
			}
			else
			{
				char nextChar = pi+1 < plen ? pattern.charAt(pi+1) : '\0';
				if (nextChar == ANY_ALL_CHAR)
					pi++;
				else if (nextChar != '\0')
				{
					// does not match a slash.
					if (t == '/' || t == '\\')
						pi++;
					else if (nextChar == t)
						pi++;
					else if (caseInsensitive && Character.toLowerCase(nextChar) == Character.toLowerCase(t))
						pi++;
					else if ((p == '/' || p == File.separatorChar) && (t == '/' || t == File.separatorChar))
						pi++; 
					else
						ti++;
				}
				// does not match a slash.
				else if (t == '/' || t == File.separatorChar)
					pi++;
				else
					ti++;
			}
		}
		
		if (pi == plen - 1)
			return pattern.charAt(pi) == ANY_ALL_CHAR && ti == tlen;
		return pi == plen && ti == tlen;
	}
	
	// Returns end index.
	static int scanNextFolder(String inputPath, int start)
	{
		final char SEPARATOR = File.separatorChar;
		
		int i = start;
		
		for (; i < inputPath.length(); i++)
		{
			char c = inputPath.charAt(i);
			if (c == '/' || c == SEPARATOR)
				break;
		}
		
		return i;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof PathPattern)
			return equals((PathPattern)obj); 
		return super.equals(obj);
	}
	
	/**
	 * Tests if this path pattern is FUNCTIONALLY EQUIVALENT to another pattern,
	 * which is to say tests the exact same pattern sequence. 
	 * @param pp the PathPattern to compare to.
	 * @return true if this and the other pattern are functionally equivalent, false otherwise.
	 */
	public boolean equals(PathPattern pp)
	{
		if (patternNodes.size() !=  pp.patternNodes.size())
			return false;
		
		for (int i = 0; i < patternNodes.size(); i++)
			if (!patternNodes.getByIndex(i).equals(pp.patternNodes.getByIndex(i)))
				return false;
				
		return true;
	}

	@Override
	public int hashCode()
	{
		return hashCode;
	}
	
	@Override
	public String toString()
	{
		return patternNodes.toString();
	}
	
	/**
	 * A pattern node that represents part of the compiled pattern.
	 */
	static class Node
	{
		// node types.
		static enum Type
		{
			DIRECTORY,
			ANY_DIRECTORY,
			FILE;
		}
		
		/** Type. */
		private Type type;
		/** Pattern. */
		private String pattern;
		
		private Node(Type type, String pattern)
		{
			this.type = type;
			this.pattern = pattern;
		}
		
		/** Get node type. */
		public Type getType()
		{
			return type;
		}
		
		/** Get node pattern. */
		public String getPattern()
		{
			return pattern;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof Node)
				return equals((Node)obj); 
			return super.equals(obj);
		}
		
		private boolean equals(Node n)
		{
			return type == n.type && pattern.equals(n.pattern);
		}
		
		@Override
		public int hashCode()
		{
			return pattern.hashCode() + type.ordinal();
		}
		
		@Override
		public String toString()
		{
			return type.name() +": "+pattern;
		}
		
	}
}
