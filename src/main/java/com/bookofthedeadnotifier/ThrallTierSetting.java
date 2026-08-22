package com.bookofthedeadnotifier;

/**
 * The configured thrall tier, which may defer the choice to the player's Magic level.
 */
public enum ThrallTierSetting
{
	AUTO("Auto (by Magic level)", null),
	LESSER("Lesser", ThrallTier.LESSER),
	SUPERIOR("Superior", ThrallTier.SUPERIOR),
	GREATER("Greater", ThrallTier.GREATER);

	private final String displayName;
	private final ThrallTier tier;

	ThrallTierSetting(String displayName, ThrallTier tier)
	{
		this.displayName = displayName;
		this.tier = tier;
	}

	public ThrallTier resolve(int magicLevel)
	{
		return tier == null ? ThrallTier.highestCastableAt(magicLevel) : tier;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
