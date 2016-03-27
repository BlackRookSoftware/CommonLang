package com.blackrook.lang.replacer;

/**
 * A replacer kernel that replaces tokens (with no arguments) with another mapped string.
 * Tokens are surrounded by special characters.
 * @author Matthew Tropiano
 * @since 2.9.0
 */
public abstract class SingleTokenReplacerKernel implements ReplacerKernel
{
	/** Token start character. */
	protected char tokenStart;
	/** Token end character. */
	protected char tokenEnd;
	
	/**
	 * Creates a new mapped replacer kernel with no token keys.
	 * @param tokenStart the token start character.
	 * @param tokenEnd the token end character.
	 */
	public SingleTokenReplacerKernel(char tokenStart, char tokenEnd)
	{
		this.tokenStart = tokenStart;
		this.tokenEnd = tokenEnd;
	}
	
	@Override
	public boolean isTokenStarter(char input)
	{
		return input == tokenStart;
	}

	@Override
	public char getTokenEnder(char input)
	{
		return input == tokenStart ? tokenEnd : '\0';
	}

	@Override
	public boolean isArgumentListStarter(char input)
	{
		return false;
	}

	@Override
	public boolean isArgumentSeparator(char input)
	{
		return false;
	}

	@Override
	public abstract String handleToken(String tokenName, String[] arguments);

}
