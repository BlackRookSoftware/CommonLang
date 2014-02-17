/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.test;

import java.io.StringWriter;

import com.blackrook.commons.Common;
import com.blackrook.commons.math.Pair;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONReader;
import com.blackrook.lang.json.JSONWriter;
import com.blackrook.lang.test.JunkObject.Type;

public class TestJSON
{
	public static void main(String[] args) throws Throwable
	{
		JunkObject junk = new JunkObject();
		junk.x = 3;
		junk.y = 4;
		junk.setArrayOfInts(new int[]{4,6,7,8,9});
		junk.setName("asdfjlasjdflkjasdf");
		junk.setId(234234234234234234L);
		junk.setPair(new Pair(5,8));
		junk.setTypeA(Type.READ);
		junk.setTypeB(Type.WRITE);
		JSONObject object = JSONObject.create(junk);
		StringWriter sw = new StringWriter();
		JSONWriter.writeJSON(object, sw);
		object = JSONReader.readJSON(sw.toString());
		junk = object.newObject(JunkObject.class);
		Common.noop();
}
}
