/*******************************************************************************
 * Copyright (c) 2009-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.lang.replacer;

/**
 * Interface for replacers that use a set of {@link Enum} objects as replacer commands.
 * @author Matthew Tropiano
 */
public interface ReplacerCommand
{
	/**
	 * Returns the replace string using this command and the input arguments.
	 * @param arguments the token arguments.
	 * @return the resultant string.
	 */
	public abstract String handleReplace(String... arguments);
	
}
