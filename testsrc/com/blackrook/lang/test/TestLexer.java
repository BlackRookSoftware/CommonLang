/*******************************************************************************
 * Copyright (c) 2009-2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.test;

import com.blackrook.lang.Lexer;
import com.blackrook.lang.Lexer.Token;
import com.blackrook.lang.LexerKernel;

public class TestLexer
{
	public static void main(String[] args) throws Throwable
	{
//		Lexer.DEBUG = true;
		Lexer lexer = new Lexer(new LexerKernel(), "10 3.5 0x5535 0X44 0434 12e4 3453E4 9x234 3e-6 4.2e3 0e10 4E+5");
		Token token = null;
		while ((token = lexer.nextToken()) != null)
		{
			System.out.println(token);
		}
}
}
