package com.blackrook.lang.replacer;

import com.blackrook.commons.linkedlist.Queue;

/**
 * Replacer class for replacing tokens in character sequences.
 * The kernel used defines how and what gets replaced.
 * @author Matthew Tropiano
 * @since 2.9.0
 */
public class KeyReplacer
{
	private static final String[] NO_ARGUMENTS = new String[0];
	
	/** The replacer kernel. */
	private ReplacerKernel kernel;
	
	/**
	 * Creates a key replacer instance that uses the provided kernel.
	 * @param kernel the kernel to use for parsing.
	 * @throws IllegalArgumentException if kernel is null.
	 */
	public KeyReplacer(ReplacerKernel kernel)
	{
		if (kernel == null)
			throw new IllegalArgumentException("kernel can't be null.");
		this.kernel = kernel;
	}
	
	/**
	 * Takes a string and replaces text, returning the result.
	 * @param sequence the input character sequence.
	 * @return the new string after replacements.
	 */
	public String replace(CharSequence sequence)
	{
		Context context = new Context();
		final int STATE_STRING = 0;
		final int STATE_TOKEN = 1;
		final int STATE_ARGUMENT = 2;
		context.state = STATE_STRING;
		
		int len = sequence.length(), i = 0;
		
		while (i < len)
		{
			char c = sequence.charAt(i);
			
			switch (context.state)
			{
				case STATE_STRING:
				{
					// token start
					if (kernel.isTokenStarter(c))
					{
						context.tokenEnd = kernel.getTokenEnder(c);
						context.tokenBuilder.delete(0, context.tokenBuilder.length());
						context.state = STATE_TOKEN;
					}
					else
					{
						context.outputBuilder.append(c);
					}
				}
				break;
				
				case STATE_TOKEN:
				{
					// token argument ender.
					if (context.tokenEnd == c)
					{
						context.outputBuilder.append(kernel.handleToken(context.tokenBuilder.toString(), NO_ARGUMENTS));
						context.state = STATE_STRING;
					}
					// token argument starter.
					else if (kernel.isArgumentListStarter(c))
					{
						context.token = context.tokenBuilder.toString();
						context.tokenBuilder.delete(0, context.tokenBuilder.length());
						context.state = STATE_ARGUMENT;
					}
					else
					{
						context.tokenBuilder.append(c);
					}
				}
				break;

				case STATE_ARGUMENT:
				{
					// token argument ender.
					if (context.tokenEnd == c)
					{
						context.arguments.add(context.tokenBuilder.toString());						
						String[] args = new String[context.arguments.size()];
						context.arguments.toArray(args);
						context.outputBuilder.append(kernel.handleToken(context.token, args));
						context.state = STATE_STRING;
					}
					// token argument starter.
					else if (kernel.isArgumentSeparator(c))
					{
						context.arguments.add(context.tokenBuilder.toString());
						context.tokenBuilder.delete(0, context.tokenBuilder.length());
						context.state = STATE_ARGUMENT;
					}
					else
					{
						context.tokenBuilder.append(c);
					}
					
				}
				break;
				
			}
			
			i++;
		}
		
		if (context.state == STATE_TOKEN)
		{
			context.outputBuilder.append(kernel.handleToken(context.tokenBuilder.toString(), NO_ARGUMENTS));
		}
		else if (context.state == STATE_ARGUMENT)
		{
			context.arguments.add(context.tokenBuilder.toString());						
			String[] args = new String[context.arguments.size()];
			context.arguments.toArray(args);
			context.outputBuilder.append(kernel.handleToken(context.token, args));
		}

		return context.outputBuilder.toString();
	}

	/**
	 * Parser context.
	 */
	private static class Context
	{
		private String token;
		private Queue<String> arguments;
		private StringBuilder tokenBuilder;
		private StringBuilder outputBuilder;
		private int state;
		private char tokenEnd;
		
		Context()
		{
			this.token = null;
			this.arguments = new Queue<String>();
			this.tokenBuilder = new StringBuilder();
			this.outputBuilder = new StringBuilder();
			this.state = 0;
			this.tokenEnd = '\0';
		}
		
	}
	
}
