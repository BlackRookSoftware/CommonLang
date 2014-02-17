/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.test;

import java.io.ByteArrayInputStream;
import java.net.URL;

import com.blackrook.commons.Common;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLStructFactory;
import com.blackrook.lang.xml.XMLWriter;

public class TestXML
{
	public static void main(String[] args) throws Throwable
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(Common.getHTTPByteContent(new URL("http://doomworld.com/idgames/api/api.php?action=get&id=15699")));
		XMLStruct struct = XMLStructFactory.readXML(bis);
		(new XMLWriter()).writeXML(struct, System.out);
		Common.noop();
}
}
