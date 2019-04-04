/*******************************************************************************
 * Copyright (c) 2009-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import com.blackrook.commons.hash.HashMap;

/**
 * Writer for writing XML data.
 * @author Matthew Tropiano
 */
public class XMLWriter
{
	/** Escape map. */
	private static final HashMap<Character, String> ESCAPE_MAP = new HashMap<Character, String>(){{
		put('<', "&lt;");
		put('>', "&gt;");
		put('&', "&amp;");
		put('"', "&quot;");
		put('\'', "&apos;");
}};

	/**
	 * Document type enumeration.
	 */
	public static enum Doctype
	{
		HTML5("html", false, null, null),
		HTML401_STRUCT("html", false, "-//W3C//DTD HTML 4.01//EN", "http://www.w3.org/TR/html4/strict.dtd"),
		HTML401_TRADITIONAL("html", false, "-//W3C//DTD HTML 4.01 Transitional//EN", "http://www.w3.org/TR/html4/loose.dtd"),
		HTML401_FRAMESET("html", false, "-//W3C//DTD HTML 4.01 Frameset//EN", "http://www.w3.org/TR/html4/frameset.dtd"),
		XHTML10_STRICT("html", false, "-//W3C//DTD XHTML 1.0 Strict//EN", "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"),
		XHTML10_TRADITIONAL("html", false, "-//W3C//DTD XHTML 1.0 Transitional//EN", "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"),
		XHTML10_FRAMESET("html", false, "-//W3C//DTD XHTML 1.0 Frameset//EN", "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd"),
		XHTML10_BASIC("html", false, "-//W3C//DTD XHTML Basic 1.0//EN", "http://www.w3.org/TR/xhtml-basic/xhtml-basic10.dtd"),
		XHTML11("html", false, "-//W3C//DTD XHTML 1.1//EN", "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"),
		XHTML11_BASIC("html", false, "-//W3C//DTD XHTML Basic 1.1//EN", "http://www.w3.org/TR/xhtml-basic/xhtml-basic11.dtd"),
		XHTML_MOBILE10("html", false, "-//WAPFORUM//DTD XHTML Mobile 1.0//EN", "http://www.wapforum.org/DTD/xhtml-mobile10.dtd"),
		XHTML_MOBILE11("html", false, "-//WAPFORUM//DTD XHTML Mobile 1.1//EN", "http://www.openmobilealliance.org/tech/DTD/xhtml-mobile11.dtd"),
		XHTML_MOBILE12("html", false, "-//WAPFORUM//DTD XHTML Mobile 1.2//EN", "http://www.openmobilealliance.org/tech/DTD/xhtml-mobile12.dtd"),
		XHTML_RDFA10("html", false, "-//W3C//DTD XHTML+RDFa 1.0//EN", "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd");
		;
		
		final String rootElement;
		final boolean system;
		final String rfi;
		final String dtd;
		
		private Doctype(String rootElement, boolean system, String rfi, String dtd)
		{
			this.rootElement = rootElement;
			this.system = system;
			this.rfi = rfi;
			this.dtd = dtd;
		}
	}

	/** XML Version. */
	private String version;
	/** XML encoding. */
	private String encoding;
	/** Document root element. */
	private String rootElement;
	/** Document RFI. */
	private String rfi;
	/** Document DTD. */
	private String dtd;
	/** Document PUBLIC or SYSTEM. */
	private boolean system;
	
	/**
	 * Creates an XMLWriter with the following headers.
	 * @param version the XML document version. Usually, this is "1.0".
	 * @param encoding the encoding type for this header.
	 * @param rootElement the root element of the document. For HTML, this is "html".
	 * @param rfi the DOCTYPE RFI type. if null, this is not printed.
	 * @param dtd the URI to the DOCTYPE DTD spec. if null, this is not printed.
	 * @param system if true, the DOCTYPE is "SYSTEM", else, "PUBLIC".
	 */
	public XMLWriter(String version, String encoding, String rootElement, String rfi, String dtd, boolean system)
	{
		this.version = version;
		this.encoding = encoding;
		this.rootElement = rootElement;
		this.rfi = rfi;
		this.dtd = dtd;
		this.system = system;
	}
	
	/**
	 * Creates an XMLWriter that prepends output with a modified header.
	 * @param version the XML document version. Usually, this is "1.0".
	 * @param encoding the encoding type for this header.
	 * @param doctype the doctype to use.
	 */
	public XMLWriter(String version, String encoding, Doctype doctype)
	{
		this(version, encoding, doctype.rootElement, doctype.rfi, doctype.dtd, doctype.system);
	}
	
	/**
	 * Creates an XMLWriter that prepends output with a modified header.
	 * This declares no DOCTYPE.
	 * @param version the XML document version. Usually, this is "1.0".
	 * @param encoding the encoding type for this writer.
	 */
	public XMLWriter(String version, String encoding)
	{
		this(version, encoding, null, null, null, false);
	}

	/**
	 * Creates an XMLWriter that prepends output with a modified header.
	 * Declares version "1.0" and encoding "UTF-8".
	 * @param doctype the doctype to use. 
	 */
	public XMLWriter(Doctype doctype)
	{
		this("1.0", "UTF-8", doctype.rootElement, doctype.rfi, doctype.dtd, doctype.system);
	}

	/**
	 * Creates an XMLWriter that prepends output with a standard, non-descript header.
	 * Declares version "1.0" and encoding "UTF-8".
	 * This declares no DOCTYPE.
	 */
	public XMLWriter()
	{
		this("1.0", "UTF-8", null, null, null, false);
	}

	/**
	 * Writes an XMLStruct to an output stream, using this XML writer's encoding and doctype.
	 * @param struct the struct to write.
	 * @param out the output stream to write to.
	 * @throws IOException if a write error occurs.
	 */
	public void writeXML(XMLStruct struct, OutputStream out) throws IOException
	{
		WriterKernel wk = new WriterKernel(out);
		wk.writeXMLHeader();
		wk.writeDoctype();
		for (XMLStruct xml : struct)
			wk.writeStruct(xml);
	}
	
	/**
	 * Writes an XMLStruct to a writer, using this XML writer's encoding and doctype.
	 * @param struct the struct to write.
	 * @param writer the writer to write to.
	 * @throws IOException if a write error occurs.
	 */
	public void writeXML(XMLStruct struct, Writer writer) throws IOException
	{
		WriterKernel wk = new WriterKernel(writer);
		wk.writeXMLHeader();
		wk.writeDoctype();
		for (XMLStruct xml : struct)
			wk.writeStruct(xml);
	}
	
	/**
	 * Writes an XMLStruct to an output stream, using this XML writer's encoding and doctype.
	 * Unlike {@link #writeXML(XMLStruct, OutputStream)}, this does NOT write the headers.
	 * @param struct the struct to write.
	 * @param out the output stream to write to.
	 * @throws IOException if a write error occurs.
	 */
	public void writeXMLNoHeader(XMLStruct struct, OutputStream out) throws IOException
	{
		WriterKernel wk = new WriterKernel(out);
		wk.writeStruct(struct);
	}
	
	/**
	 * Writes an XMLStruct to a writer, using this XML writer's encoding and doctype.
	 * Unlike {@link #writeXML(XMLStruct, OutputStream)}, this does NOT write the headers.
	 * @param struct the struct to write.
	 * @param writer the writer to write to.
	 * @throws IOException if a write error occurs.
	 */
	public void writeXMLNoHeader(XMLStruct struct, Writer writer) throws IOException
	{
		WriterKernel wk = new WriterKernel(writer);
		wk.writeStruct(struct);
	}

	/**
	 * Escapes input text for its inclusion in HTML.
	 * @param text the text to convert to escaped HTML.
	 * @return the converted text.
	 */
	public static String escapeTextForHTML(String text)
	{
		StringBuilder sb = new StringBuilder();
		for (char c : text.toCharArray())
		{
			if (c < 0x00A0 && c >= 0x0020)
			{
				if (ESCAPE_MAP.containsKey(c))
					sb.append(ESCAPE_MAP.get(c));
				else
					sb.append(c);
			}
			else
				sb.append("&#").append((c & 0x0ffff)).append(';');
		}
		return sb.toString();
	}

	/**
	 * Checks if a string contains control characters.
	 * @param string the string to test.
	 * @return true if so, false if not.
	 */
	public static boolean containsControlCharacters(String string)
	{
		for (int i = 0; i < string.length(); i++)
			if (string.charAt(i) < 32)
				return true;
		return false;
	}
	
	/**
	 * Writer kernel for writing XML stuff.
	 */
	private class WriterKernel
	{
		/** Outputstream. May be null. */
		private OutputStream outStream;
		/** Writer. May be null. */
		private Writer writer;
		
		public WriterKernel(OutputStream outStream)
		{
			this.outStream = outStream;
		}

		public WriterKernel(Writer writer)
		{
			this.writer = writer;
		}
		
		// Writes to an output stream or writer.
		private void writeString(String text) throws IOException
		{
			if (outStream != null)
				outStream.write(text.getBytes());
			if (writer != null)
				writer.write(text.toCharArray());
		}
		
		// Writes the XML Header
		private void writeXMLHeader() throws IOException
		{
			writeString("<?xml ");
			writeString("version=\"");
			writeString(version);
			writeString("\" encoding=\"");
			writeString(encoding);
			writeString("\"?>\n");
		}
		
		// Writes the DOCTYPE Header
		private void writeDoctype() throws IOException
		{
			if (rootElement == null)
				return;
			
			writeString("<!DOCTYPE ");
			writeString(rootElement);
			if (rfi != null)
			{
				writeString(" ");
				writeString(system ? "SYSTEM" : "PUBLIC");
				writeString(" \"");
				writeString(escapeTextForHTML(rfi));
				writeString("\"");
			}
			if (dtd != null)
			{
				writeString(" \"");
				writeString(escapeTextForHTML(dtd));
				writeString("\"");
			}
			
			writeString(">\n");
		}
		
		// Writes an XML structure.
		private void writeStruct(XMLStruct struct) throws IOException
		{
			writeString("<");
			writeString(struct.getName());
			for (String attrib : struct.getAttributes())
			{
				writeString(" ");
				writeString(attrib);
				writeString("=\"");
				writeString(escapeTextForHTML(struct.getAttribute(attrib)));
				writeString("\"");
			}

			if (struct.isSingleton())
				writeString(" />");
			else
			{
				// close first tag.
				writeString(">");
				
				if (struct.getChildCount() > 0)
				{
					for (XMLStruct xml : struct)
						writeStruct(xml);
				} 
				else if (struct.getValue() != null)
				{
					String v = struct.getValue();
					if (containsControlCharacters(v))
					{
						writeString("<![CDATA[");
						writeString(struct.getValue());
						writeString("]]>");
					}
					else
						writeString(escapeTextForHTML(v));
				} 
				
				// close tag.
				writeString("</");
				writeString(struct.getName());
				writeString(">");
			}
				
		}

	}
	
}
