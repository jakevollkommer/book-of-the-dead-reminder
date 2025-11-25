# Book of the Dead Reminder

A RuneLite plugin that reminds you when you're missing one requirement to cast thralls.

## Features

This plugin warns you when you have **exactly 2 out of 3 requirements** for casting thralls from the Arceuus spellbook:

### The 3 Requirements:
1. **Arceuus Spellbook** - You must be on the Arceuus spellbook
2. **Thrall Runes** - You must have sufficient thrall runes (blood, cosmic, fire runes)
3. **Book of the Dead** - You must have the Book of the Dead equipped or in your inventory

### Reminder Messages

The plugin displays a reminder above your chatbox when you're missing one of these:

- Missing **Book of the Dead**: "Missing Book of the Dead"
- Wrong **Spellbook**: "Not on Arceuus spellbook"
- Missing **Runes**: "Missing thrall runes"

### Configuration Options

- **Reminder Text Style**: Choose between long text, short text, or custom text
- **Notification on Reminder**: Send system notification when reminder appears
- **Rune Thresholds**: Set minimum quantities (defaults: 10 fire, 5 blood, 1 cosmic)
- **Display Options**: Customize colors and enable flashing
- **Hide Reminder Hotkey**: Set a hotkey to manually dismiss reminders

### Smart Rune Detection

The plugin intelligently detects:
- Runes in both inventory and rune pouch
- Combo runes (Lava, Smoke, Steam count as fire runes)
- Aether runes (count as cosmic runes)
- Fire staves and Tome of Fire (infinite fire runes)
