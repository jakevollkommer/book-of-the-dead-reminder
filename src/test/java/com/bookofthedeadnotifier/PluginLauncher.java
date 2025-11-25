package com.bookofthedeadnotifier;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PluginLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BookOfTheDeadNotifierPlugin.class);
		RuneLite.main(args);
	}
}
