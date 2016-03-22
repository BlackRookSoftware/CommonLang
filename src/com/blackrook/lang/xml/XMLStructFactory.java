/*******************************************************************************
 * Copyright (c) 2009-2016 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.xml;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.blackrook.commons.linkedlist.Stack;

/**
 * A factory class that reads in XML data and returns XML structures.
 * @author Matthew Tropiano
 */
public final class XMLStructFactory
{
	/**
	 * Reads in a stream of XML data and returns an XMLStruct of the data.
	 * It will leave the input stream open after it is done.
	 * @param in the input stream to use.
	 * @return a new XMLStruct parsed from the input.
	 * @throws IOException if a read error happens.
	 * @throws SAXException if a parse error happens.
	 */
	public static XMLStruct readXML(InputStream in) throws IOException, SAXException
	{
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();
		XMLSAXHandler handler = new XMLSAXHandler();
		xmlReader.setContentHandler(handler);
		xmlReader.setErrorHandler(handler);
		xmlReader.parse(new InputSource(in));
		return handler.structStack.peek();
	}
	
	/**
	 * XML Reader for the mapping XML file.
	 */
	private static class XMLSAXHandler extends DefaultHandler
	{
		Stack<XMLStruct> structStack;
		
		public XMLSAXHandler()
		{
		}
		
		@Override
		public void startDocument() throws SAXException
		{
			structStack = new Stack<XMLStruct>();
			structStack.push(new XMLStruct("XML"));
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attribs) throws SAXException
		{
			XMLStruct struct = new XMLStruct(qName != null ? qName : localName);
			if (!structStack.isEmpty())
				structStack.peek().addStruct(struct);
			
			for (int i = 0; i < attribs.getLength(); i++)
				struct.setAttribute(attribs.getLocalName(i), attribs.getValue(i));
			
			structStack.push(struct);
		}
		
		@Override
		public void characters(char[] arg0, int arg1, int arg2) throws SAXException
		{
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < arg2; i++)
				sb.append(arg0[arg1+i]);
			structStack.peek().setValue(sb.toString());
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException
		{
			structStack.pop();
		}
		
		@Override
		public void error(SAXParseException e) throws SAXException
		{
			throw new SAXException(e);
		}
		
		@Override
		public void fatalError(SAXParseException e) throws SAXException
		{
			throw new SAXException(e);
		}


	}
}
