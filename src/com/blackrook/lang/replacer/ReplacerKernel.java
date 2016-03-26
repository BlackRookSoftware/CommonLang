package com.blackrook.lang.replacer;

/**
 * The piece of information that drives the {@link KeyReplacer} class.
 * This interface describes an object that does just that. 
 * @author Matthew Tropiano
 */
public interface ReplacerKernel
{
	
	/**
	 * Tests if a character starts a token.
	 * @param input the input character.
	 * @return true if so, false if not.
	 */
	public boolean isTokenStarter(char input);

	/**
	 * Gets the matching token ender character.
	 * @param input the input character.
	 * @return the matching ending character, or '\0' if no character.
	 */
	public char getTokenEnder(char input);

	/**
	 * Tests if a character starts the argument list.
	 * @param input the input character.
	 * @return true if so, false if not.
	 */
	public boolean isArgumentListStarter(char input);

	/**
	 * Gets the matching argument list ender character.
	 * @param input the input character.
	 * @return the matching ending character, or '\0' if no character.
	 */
	public char getArgumentListEnder(char input);

	/**
	 * Tests if a character separates arguments.
	 * @param input the input character.
	 * @return true if so, false if not.
	 */
	public boolean isArgumentSeparator(char input);
	
	/**
	 * Handles a token that is read from the input.
	 * @param tokenName the token name.
	 * @param arguments the token's arguments, if any.
	 * @return the resultant string after handling.
	 */
	public String handleToken(String tokenName, String[] arguments);
	
}
