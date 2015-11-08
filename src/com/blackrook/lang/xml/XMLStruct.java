/*******************************************************************************
 * Copyright (c) 2009-2015 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import com.blackrook.commons.Common;
import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.list.List;

/**
 * An object that holds XML data as a data hierarchy.  
 * @author Matthew Tropiano
 */
public class XMLStruct implements Iterable<XMLStruct>
{
	private static final List<XMLStruct> EMPTY_STRUCT_LIST = new List<XMLStruct>();
	private static final String[] EMPTY_STRING_LIST = new String[0];
	
	private static final XMLWriter INTERNAL_WRITER = new XMLWriter();  
	
	/** The name of the structure. */
	protected String name;
	/** Map of attributes, case-insensitive. */
	protected CaseInsensitiveHashMap<String> attributes;
	/** The list of other structures in this one. */
	protected List<XMLStruct> structList;
	/** The value of the inner data. */
	protected String value;

	/**
	 * Creates a new XMLStruct.
	 * @param name the structure name.
	 */
	public XMLStruct(String name)
	{
		setName(name);
	}
	
	/**
	 * Sets the name of this structure.
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * Gets the name of this structure.
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Returns true if the name of the structure is <code>name</code>,
	 * false otherwise.
	 * @since 2.1.0
	 */
	public boolean isName(String name)
	{
		return this.name.equals(name);
	}
	
	/**
	 * Adds a child structure to this one.
	 * @param struct the structure to add.
	 */
	public void addStruct(XMLStruct struct)
	{
		if (structList == null)
			structList = new List<XMLStruct>();
		structList.add(struct);
	}
	
	/**
	 * Removes a child structure from this one.
	 * @param struct the structure to remove.
	 */
	public boolean removeStruct(XMLStruct struct)
	{
		if (structList == null)
			return false;
		return structList.remove(struct);
	}
	
	/**
	 * Gets a child structure from this one.
	 * @param index the desired index.
	 * @return the structure at the desired index or null if out of range. 
	 */
	public XMLStruct getStruct(int index)
	{
		if (structList == null)
			return null;
		return structList.getByIndex(index);
	}
	
	/**
	 * Removes a child structure from this one.
	 * @param index the desired index.
	 * @return the removed structure at the desired index or null if out of range. 
	 */
	public XMLStruct removeStruct(int index)
	{
		if (structList == null)
			return null;
		return structList.removeIndex(index);
	}
	
	@Override
	public Iterator<XMLStruct> iterator()
	{
		if (structList == null)
			return EMPTY_STRUCT_LIST.iterator();
		return structList.iterator();
	}
	
	/**
	 * Returns the count of how many children that this structure has.
	 * @since 2.3.0
	 */
	public int getChildCount()
	{
		if (structList == null)
			return 0;
		return structList.size();
	}

	/**
	 * Returns if this structure has the characteristics of a singleton, specifically
	 * if this object has no children and a null value.
	 * @return true if so, false if not.
	 * @since 2.3.0
	 */
	public boolean isSingleton()
	{
		return (structList == null || structList.isEmpty()) && value == null;
	}

	/**
	 * Sets an attribute on this structure.
	 * @param attr the attribute name.
	 * @param value the value of the attribute.
	 */
	public void setAttribute(String attr, String value)
	{
		if (attributes == null)
			attributes = new CaseInsensitiveHashMap<String>(3);
		attributes.put(attr, value);
	}
	
	/**
	 * Sets an attribute on this structure.
	 * @param attr the attribute name.
	 * @param value the value of the attribute.
	 */
	public void setAttribute(String attr, boolean value)
	{
		setAttribute(attr,String.valueOf(value));
	}
	
	/**
	 * Sets an attribute on this structure.
	 * @param attr the attribute name.
	 * @param value the value of the attribute.
	 */
	public void setAttribute(String attr, byte value)
	{
		setAttribute(attr,String.valueOf(value));
	}
	
	/**
	 * Sets an attribute on this structure.
	 * @param attr the attribute name.
	 * @param value the value of the attribute.
	 */
	public void setAttribute(String attr, char value)
	{
		setAttribute(attr,String.valueOf(value));
	}
	
	/**
	 * Sets an attribute on this structure.
	 * @param attr the attribute name.
	 * @param value the value of the attribute.
	 */
	public void setAttribute(String attr, short value)
	{
		setAttribute(attr,String.valueOf(value));
	}
	
	/**
	 * Sets an attribute on this structure.
	 * @param attr the attribute name.
	 * @param value the value of the attribute.
	 */
	public void setAttribute(String attr, int value)
	{
		setAttribute(attr,String.valueOf(value));
	}
	
	/**
	 * Sets an attribute on this structure.
	 * @param attr the attribute name.
	 * @param value the value of the attribute.
	 */
	public void setAttribute(String attr, float value)
	{
		setAttribute(attr,String.valueOf(value));
	}
	
	/**
	 * Sets an attribute on this structure.
	 * @param attr the attribute name.
	 * @param value the value of the attribute.
	 */
	public void setAttribute(String attr, long value)
	{
		setAttribute(attr,String.valueOf(value));
	}
	
	/**
	 * Sets an attribute on this structure.
	 * @param attr the attribute name.
	 * @param value the value of the attribute.
	 */
	public void setAttribute(String attr, double value)
	{
		setAttribute(attr,String.valueOf(value));
	}
	
	/**
	 * Gets the String value of an attribute.
	 * If the attribute does not exist, this returns the empty string.
	 * @param attr the attribute name.
	 */
	public String getAttribute(String attr)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return out != null ? out : "";
	}
	
	/**
	 * Gets the String value of an attribute.
	 * If the attribute does not exist, this returns <code>def</code>.
	 * @param attr the attribute name.
	 * @param def the default value to return if the attribute does not exist.
	 */
	public String getAttribute(String attr, String def)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return out != null ? out : def;
	}
	
	/**
	 * Gets the byte value of an attribute.
	 * If the attribute does not exist, this returns the empty string.
	 * @param attr the attribute name.
	 */
	public boolean getAttributeBoolean(String attr)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return Common.parseBoolean(out);
	}
	
	/**
	 * Gets the byte value of an attribute.
	 * If the attribute does not exist, this returns <code>def</code>.
	 * @param attr the attribute name.
	 * @param def the default value to return if the attribute does not exist.
	 */
	public boolean getAttributeBoolean(String attr, boolean def)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return out != null ? Common.parseBoolean(out) : def;
	}
	
	/**
	 * Gets the byte value of an attribute.
	 * If the attribute does not exist, this returns the empty string.
	 * @param attr the attribute name.
	 */
	public byte getAttributeByte(String attr)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return Common.parseByte(out);
	}
	
	/**
	 * Gets the byte value of an attribute.
	 * If the attribute does not exist, this returns <code>def</code>.
	 * @param attr the attribute name.
	 * @param def the default value to return if the attribute does not exist.
	 */
	public byte getAttributeByte(String attr, byte def)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return out != null ? Common.parseByte(out) : def;
	}
	
	/**
	 * Gets the short value of an attribute.
	 * If the attribute does not exist, this returns the empty string.
	 * @param attr the attribute name.
	 */
	public short getAttributeShort(String attr)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return Common.parseShort(out);
	}
	
	/**
	 * Gets the short value of an attribute.
	 * If the attribute does not exist, this returns <code>def</code>.
	 * @param attr the attribute name.
	 * @param def the default value to return if the attribute does not exist.
	 */
	public short getAttributeShort(String attr, short def)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return out != null ? Common.parseShort(out) : def;
	}
	
	/**
	 * Gets the char value of an attribute.
	 * If the attribute does not exist, this returns the empty string.
	 * @param attr the attribute name.
	 */
	public char getAttributeChar(String attr)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return Common.parseChar(out);
	}
	
	/**
	 * Gets the char value of an attribute.
	 * If the attribute does not exist, this returns <code>def</code>.
	 * @param attr the attribute name.
	 * @param def the default value to return if the attribute does not exist.
	 */
	public char getAttributeChar(String attr, char def)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return out != null ? Common.parseChar(out) : def;
	}
	
	/**
	 * Gets the int value of an attribute.
	 * If the attribute does not exist, this returns the empty string.
	 * @param attr the attribute name.
	 */
	public int getAttributeInt(String attr)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return Common.parseInt(out);
	}
	
	/**
	 * Gets the int value of an attribute.
	 * If the attribute does not exist, this returns <code>def</code>.
	 * @param attr the attribute name.
	 * @param def the default value to return if the attribute does not exist.
	 */
	public int getAttributeInt(String attr, int def)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return out != null ? Common.parseInt(out) : def;
	}
	
	/**
	 * Gets the long value of an attribute.
	 * If the attribute does not exist, this returns the empty string.
	 * @param attr the attribute name.
	 */
	public long getAttributeLong(String attr)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return Common.parseLong(out);
	}
	
	/**
	 * Gets the long value of an attribute.
	 * If the attribute does not exist, this returns <code>def</code>.
	 * @param attr the attribute name.
	 * @param def the default value to return if the attribute does not exist.
	 */
	public long getAttributeLong(String attr, long def)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return out != null ? Common.parseLong(out) : def;
	}
	
	/**
	 * Gets the short value of an attribute.
	 * If the attribute does not exist, this returns the empty string.
	 * @param attr the attribute name.
	 */
	public float getAttributeFloat(String attr)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return Common.parseFloat(out);
	}
	
	/**
	 * Gets the float value of an attribute.
	 * If the attribute does not exist, this returns <code>def</code>.
	 * @param attr the attribute name.
	 * @param def the default value to return if the attribute does not exist.
	 */
	public float getAttributeFloat(String attr, float def)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return out != null ? Common.parseFloat(out) : def;
	}
	
	/**
	 * Gets the double value of an attribute.
	 * If the attribute does not exist, this returns the empty string.
	 * @param attr the attribute name.
	 */
	public double getAttributeDouble(String attr)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return Common.parseDouble(out);
	}
	
	/**
	 * Gets the double value of an attribute.
	 * If the attribute does not exist, this returns <code>def</code>.
	 * @param attr the attribute name.
	 * @param def the default value to return if the attribute does not exist.
	 */
	public double getAttributeDouble(String attr, double def)
	{
		String out = attributes != null ? attributes.get(attr) : null;
		return out != null ? Common.parseDouble(out) : def;
	}

	/**
	 * Gets all of the attributes on this.
	 * @return the list of attributes, or an empty array if no attributes.
	 * @since 2.3.0
	 */
	public String[] getAttributes()
	{
		if (attributes == null || attributes.isEmpty())
			return EMPTY_STRING_LIST;
		
		String[] out = new String[attributes.size()];
		Iterator<String> it = attributes.keyIterator();
		int i = 0;
		while (it.hasNext())
			out[i++] = it.next();
			
		return out;
	}
	
	/**
	 * Sets the value of the structure.
	 */
	public void setValue(String value)
	{
		this.value = value;
	}
	
	/**
	 * Sets the value of the structure.
	 */
	public void setValue(boolean value)
	{
		setValue(String.valueOf(value));
	}
	
	/**
	 * Sets the value of the structure.
	 */
	public void setValue(byte value)
	{
		setValue(String.valueOf(value));
	}
	
	/**
	 * Sets the value of the structure.
	 */
	public void setValue(char value)
	{
		setValue(String.valueOf(value));
	}
	
	/**
	 * Sets the value of the structure.
	 */
	public void setValue(short value)
	{
		setValue(String.valueOf(value));
	}
	
	/**
	 * Sets the value of the structure.
	 */
	public void setValue(int value)
	{
		setValue(String.valueOf(value));
	}
	
	/**
	 * Sets the value of the structure.
	 */
	public void setValue(long value)
	{
		setValue(String.valueOf(value));
	}
	
	/**
	 * Sets the value of the structure.
	 */
	public void setValue(float value)
	{
		setValue(String.valueOf(value));
	}
	
	/**
	 * Sets the value of the structure.
	 */
	public void setValue(double value)
	{
		setValue(String.valueOf(value));
	}
	
	/**
	 * Gets the String value of the structure.
	 */
	public String getValue()
	{
		if (getChildCount() > 0)
		{
			StringWriter sw = new StringWriter();
			for (XMLStruct struct : structList)
				try {
					INTERNAL_WRITER.writeXMLNoHeader(struct, sw);
				} catch (IOException e) {
					return "!!EXCEPTION!!";
				}
			return sw.toString();
		}
		else
			return value;
	}

	/**
	 * Gets the byte value of the structure.
	 */
	public byte getValueByte()
	{
		return Common.parseByte(value);
	}

	/**
	 * Gets the short value of the structure.
	 */
	public short getValueShort()
	{
		return Common.parseShort(value);
	}

	/**
	 * Gets the char value of the structure.
	 */
	public char getValueChar()
	{
		return Common.parseChar(value);
	}

	/**
	 * Gets the int value of the structure.
	 */
	public int getValueInt()
	{
		return Common.parseInt(value);
	}

	/**
	 * Gets the long value of the structure.
	 */
	public long getValueLong()
	{
		return Common.parseLong(value);
	}

	/**
	 * Gets the short value of the structure.
	 */
	public float getValueFloat()
	{
		return Common.parseFloat(value);
	}

	/**
	 * Gets the double value of the structure.
	 */
	public double getValueDouble()
	{
		return Common.parseDouble(value);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		toStringRecurse(sb, 0, this);
		return sb.toString();
	}
	
	// For toString().
	private void toStringRecurse(StringBuilder sb, int tabDepth, XMLStruct struct)
	{
		for (int i = 0; i < tabDepth; i++)
			sb.append('\t');
		sb.append(struct.name).append(' ');
		if (struct.attributes != null && struct.attributes.size() > 0)
		{
			sb.append('(').append(' ');
			for (ObjectPair<String, String> pair : struct.attributes)
				sb.append(pair.getKey()).append('=').append(pair.getValue()).append(' ');
			sb.append(')').append(' ');
		}
		if (struct.value != null) 
			sb.append(": ").append(struct.value);
		sb.append('\n');
		if (struct.structList != null) for (XMLStruct child : struct.structList)
			toStringRecurse(sb, tabDepth+1, child);
	}
	
}
