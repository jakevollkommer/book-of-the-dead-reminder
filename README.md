# Book of the Dead Reminder

A RuneLite plugin that reminds you when you're missing one requirement to cast thralls.

## Features

This plugin warns you when you have **exactly 2 out of 3 requirements** for casting thralls from the Arceuus spellbook:

### The 3 Requirements:
1. **Arceuus Spellbook** - You must be on the Arceuus spellbook
2. **Thrall Runes** - You must have enough runes for the thrall you cast
3. **Book of the Dead** - You must have the Book of the Dead equipped or in your inventory

### All Three Thrall Tiers

Ghost, skeleton and zombie thralls of the same tier cost the same runes, so the tier is all the plugin needs to know:

| Tier | Magic | Runes per cast |
|----------|-------|-----------------------------|
| Lesser | 38 | 10 air, 5 mind, 1 cosmic |
| Superior | 57 | 10 earth, 5 death, 1 cosmic |
| Greater | 76 | 10 fire, 5 blood, 1 cosmic |

By default the plugin checks the highest tier your Magic level can cast, and you can pin it to a specific tier if you deliberately cast a cheaper one.

### Casts Remaining

Instead of raw rune counts, the plugin works out how many thralls you can actually cast and warns when you drop below your **Minimum Casts** setting. When you still have some left, the reminder names the shortfall — "Low on thrall runes (3 casts)".

### Reminder Messages

The plugin displays a reminder above your chatbox when you're missing one of these:

- Missing **Book of the Dead**: "Missing Book of the Dead"
- Wrong **Spellbook**: "Not on Arceuus spellbook"
- Out of **Runes**: "Missing thrall runes"
- Low on **Runes**: "Low on thrall runes (3 casts)"

### Configuration Options

- **Reminder Text Style**: Choose between long text, short text, or custom text
- **Notification on Reminder**: Send system notification when reminder appears
- **Thrall Tier**: The thrall you cast, or Auto to follow your Magic level
- **Minimum Casts**: Warn when you can cast fewer thralls than this (default: 1)
- **Display Options**: Customize colors and enable flashing
- **Hide Reminder Hotkey**: Set a hotkey to manually dismiss reminders

### Smart Rune Detection

The plugin intelligently detects:
- Runes in both inventory and rune pouch, including divine rune pouches
- Combo runes (Dust, Mist and Smoke count as air; Dust, Mud and Lava as earth; Lava, Smoke, Steam and Sunfire as fire)
- Aether runes (count as cosmic runes)
- Elemental staves and tomes as an infinite source of their element (air, earth and fire staves and battlestaves, the combo battlestaves, Tome of Fire and Tome of Earth)
