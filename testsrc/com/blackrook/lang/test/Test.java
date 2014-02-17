/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.test;

import com.blackrook.lang.path.PathPattern;

public class Test
{
	public static void main(String[] args) throws Throwable
	{
		PathPattern pp = PathPattern.compile("**");
		System.out.println(pp);
		
		System.out.println(pp.matches("apple/butt.jsp"));
		System.out.println(pp.matches("apple/pear/butt.jsp"));
		System.out.println(pp.matches("apple/orange/asdasd/buttx.jsp"));
		System.out.println(pp.matches("apple/orange/asdasd/butt.jsr"));
		System.out.println(pp.matches("butt.jsp"));
		System.out.println(pp.matches("orange/asdasd/butt.jsp"));
		System.out.println(pp.matches("apple/butt.jsr"));
}
}
