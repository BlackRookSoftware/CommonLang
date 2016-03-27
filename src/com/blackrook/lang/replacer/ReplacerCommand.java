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
