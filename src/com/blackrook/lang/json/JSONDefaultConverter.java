/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.json;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Map;

import com.blackrook.commons.AbstractMap;
import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.Reflect;
import com.blackrook.lang.json.annotation.JSONIgnore;

/**
 * Default converter for all types of objects that are not
 * a part of the annotated conversions.
 * @author Matthew Tropiano
 */
public class JSONDefaultConverter implements JSONConverter<Object>
{

	@Override
	public JSONObject getJSONObject(Object object)
	{
		if (object instanceof Enum)
		{
			return JSONObject.create(((Enum<?>)object).name());
		}
		else if (object instanceof Map<?, ?>)
		{
			JSONObject out = JSONObject.createEmptyObject();
			for (Map.Entry<?, ?> entry : ((Map<?, ?>)object).entrySet())
			{
				String key = String.valueOf(entry.getKey());
				out.addMember(key, entry.getValue());
			}
			return out;
		}
		else if (object instanceof AbstractMap<?, ?>)
		{
			JSONObject out = JSONObject.createEmptyObject();
			for (ObjectPair<?, ?> entry : (AbstractMap<?, ?>)object)
			{
				String key = String.valueOf(entry.getKey());
				out.addMember(key, entry.getValue());
			}
			return out;
		}
		else if (object instanceof Iterable<?>)
		{
			JSONObject out = JSONObject.createEmptyArray();
			Iterator<?> it = ((Iterable<?>)object).iterator();
			while (it.hasNext())
				out.append(JSONObject.create(it.next()));
			return out;
		}
		else
		{
			Class<?> clz = object.getClass();
			JSONObject out = JSONObject.createEmptyObject();

			for (String getter : Reflect.getGetterNames(clz))
			{
				Method m = Reflect.getGetter(clz, getter);
				if (m != null && m.getReturnType() != Class.class && !m.isAnnotationPresent(JSONIgnore.class))
					out.addMember(getter, Reflect.invokeBlind(m, object));
			}
			for (String f : Reflect.getPublicFields(object))
			{
				try {
					Field field = clz.getField(f);
					if (!field.isAnnotationPresent(JSONIgnore.class) && !Modifier.isStatic(field.getModifiers()))
					{
						out.addMember(f, Reflect.getField(object, f));
					}
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				}
			}
			return out;
		}
	}

	@Override
	/** Returns null. */
	public Object getObject(JSONObject jsonObject)
	{
		return null;
	}

}
