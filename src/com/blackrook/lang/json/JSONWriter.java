/*******************************************************************************
 * Copyright (c) 2009-2016 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.json;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import com.blackrook.commons.Common;

/**
 * A class for writing JSON data to JSON representation.
 * @author Matthew Tropiano
 */
public final class JSONWriter
{
	/**
	 * Writes a JSONObject out to the following output stream.
	 * @param jsonObject the object to write.
	 * @param out the output stream to write to.
	 * @throws IOException if a write error occurs.
	 */
	public static void writeJSON(JSONObject jsonObject, OutputStream out) throws IOException
	{
		(new WriterContext(jsonObject, out)).startWrite();
	}

	/**
	 * Writes a JSONObject out to the following output stream.
	 * @param jsonObject the object to write.
	 * @param writer the writer to write to.
	 * @throws IOException if a write error occurs.
	 */
	public static void writeJSON(JSONObject jsonObject, Writer writer) throws IOException
	{
		(new WriterContext(jsonObject, writer)).startWrite();
	}

	/**
	 * Writes a JSONObject out to the following output stream.
	 * @param jsonObject the object to write.
	 * @return the JSONObject as a JSON string. 
	 * @throws IOException if a write error occurs.
	 * @since 2.5.1
	 */
	public static String writeJSONString(JSONObject jsonObject) throws IOException
	{
		StringWriter sw = new StringWriter();
		writeJSON(jsonObject, sw);
		return sw.toString();
	}

	/**
	 * Writes a JSONObject out to the following output stream.
	 * @param object the object to write.
	 * @param out the output stream to write to.
	 * @throws IOException if a write error occurs.
	 * @since 2.5.1
	 */
	public static void writeJSON(Object object, OutputStream out) throws IOException
	{
		writeJSON(JSONObject.create(object), out);
	}

	/**
	 * Writes a JSONObject out to the following output stream.
	 * @param object the object to write.
	 * @param writer the writer to write to.
	 * @throws IOException if a write error occurs.
	 * @since 2.5.1
	 */
	public static void writeJSON(Object object, Writer writer) throws IOException
	{
		writeJSON(JSONObject.create(object), writer);
	}

	/**
	 * Writes a JSONObject out to the following output stream.
	 * @param object the object to write.
	 * @return the JSONObject as a JSON string. 
	 * @throws IOException if a write error occurs.
	 * @since 2.5.1
	 */
	public static String writeJSONString(Object object) throws IOException
	{
		StringWriter sw = new StringWriter();
		writeJSON(JSONObject.create(object), sw);
		return sw.toString();
	}

	/**
	 * Writer context.
	 */
	private static class WriterContext
	{
		/** Outputstream. May be null. */
		private OutputStream outStream;
		/** Writer. May be null. */
		private Writer writer;
		/** JSON Object. */
		private JSONObject object;
		
		public WriterContext(JSONObject object, Writer writer)
		{
			this.object = object;
			this.writer = writer;
		}

		public WriterContext(JSONObject object, OutputStream outStream)
		{
			this.object = object;
			this.outStream = outStream;
		}
		
		// Writes to an output stream or writer.
		private void writeString(Object object) throws IOException
		{
			if (outStream != null)
				outStream.write(String.valueOf(object).getBytes());
			if (writer != null)
				writer.write(String.valueOf(object).toCharArray());
		}
		
		/**
		 * Starts the write.
		 */
		public void startWrite() throws IOException
		{
			writeObject(object);
		}
		
		/**
		 * Writes an object.
		 */
		public void writeObject(JSONObject object) throws IOException
		{
			if (object.isUndefined())
				writeString("undefined");
			else if (object.isNull())
				writeString("null");
			else if (object.isArray())
			{
				writeString("[");
				for (int i = 0; i < object.length(); i++)
				{
					writeObject(object.get(i));
					if (i < object.length() - 1)
						writeString(",");
				}
				writeString("]");
			}
			else if (object.isObject())
			{
				writeString("{");
				int i = 0;
				int len = object.getMemberCount();
				for (String member : object.getMemberNames())
				{
					writeString("\"");
					writeString(Common.withEscChars(member));
					writeString("\":");
					writeObject(object.get(member));
					if (i < len - 1)
						writeString(",");
					i++;
				}
				writeString("}");
			}
			else
			{
				Object value = object.getValue();
				if (value instanceof Boolean)
					writeString(object.getBoolean());
				else if (value instanceof Byte)
					writeString(object.getByte());
				else if (value instanceof Short)
					writeString(object.getShort());
				else if (value instanceof Integer)
					writeString(object.getInt());
				else if (value instanceof Float)
					writeString(object.getFloat());
				else if (value instanceof Long)
					writeString(object.getLong());
				else if (value instanceof Double)
					writeString(object.getDouble());
				else
				{
					writeString("\"");
					writeString(Common.withEscChars(object.getString()));
					writeString("\"");
				}
			}
				
		}
		
		
	}
	
}
