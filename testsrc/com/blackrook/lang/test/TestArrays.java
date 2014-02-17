/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.test;

public class TestArrays
{
	public static void main(String[] args)
	{
		Object obj = null;
		System.out.println((obj = new boolean[0]).getClass().getName());
		System.out.println((obj = new byte[0]).getClass().getName());
		System.out.println((obj = new short[0]).getClass().getName());
		System.out.println((obj = new int[0]).getClass().getName());
		System.out.println((obj = new long[0]).getClass().getName());
		System.out.println((obj = new float[0]).getClass().getName());
		System.out.println((obj = new double[0]).getClass().getName());
		
		obj.getClass().cast(obj);
}

}
