package com.bookofthedeadnotifier;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("bookofthedeadreminder")
public interface BookOfTheDeadNotifierConfig extends Config
{
    @ConfigItem(
        keyName = "reminderStyle",
        name = "Reminder Text",
        description = "The style of reminder text to display",
        position = 0
    )
    default BookOfTheDeadNotifierStyle reminderStyle()
    {
        return BookOfTheDeadNotifierStyle.LONG_TEXT;
    }

    @ConfigItem(
        keyName = "notification",
        name = "Notification on Reminder",
        description = "Sends a notification when warning appears",
        position = 1
    )
    default Notification notification()
    {
        return Notification.ON;
    }

    @ConfigItem(
        keyName = "hideReminderHotkey",
        name = "Hide Reminder Hotkey",
        description = "Hotkey to dismiss the warning",
        position = 2
    )
    default Keybind hideReminderHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigSection(
        name = "Notification Conditions",
        description = "Choose which conditions trigger a reminder",
        position = 3,
        closedByDefault = true
    )
    String notificationConditionsSection = "notificationConditions";

    @ConfigItem(
        keyName = "notifyOnMissingBook",
        name = "Notify on Missing Book",
        description = "Show reminder when Book of the Dead is missing",
        position = 0,
        section = notificationConditionsSection
    )
    default boolean notifyOnMissingBook()
    {
        return true;
    }

    @ConfigItem(
        keyName = "notifyOnMissingRunes",
        name = "Notify on Missing Runes",
        description = "Show reminder when thrall runes are missing",
        position = 1,
        section = notificationConditionsSection
    )
    default boolean notifyOnMissingRunes()
    {
        return true;
    }

    @ConfigItem(
        keyName = "notifyOnWrongSpellbook",
        name = "Notify on Wrong Spellbook",
        description = "Show reminder when not on Arceuus spellbook",
        position = 2,
        section = notificationConditionsSection
    )
    default boolean notifyOnWrongSpellbook()
    {
        return true;
    }

    @ConfigSection(
        name = "Rune Thresholds",
        description = "Minimum rune quantities required",
        position = 4,
        closedByDefault = true
    )
    String runeThresholdSection = "runeThresholds";

    @ConfigItem(
        keyName = "minFireRunes",
        name = "Minimum Fire Runes",
        description = "Minimum fire runes needed",
        position = 0,
        section = runeThresholdSection
    )
    default int minFireRunes()
    {
        return 10;
    }

    @ConfigItem(
        keyName = "minBloodRunes",
        name = "Minimum Blood Runes",
        description = "Minimum blood runes needed",
        position = 1,
        section = runeThresholdSection
    )
    default int minBloodRunes()
    {
        return 5;
    }

    @ConfigItem(
        keyName = "minCosmicRunes",
        name = "Minimum Cosmic Runes",
        description = "Minimum cosmic runes needed",
        position = 2,
        section = runeThresholdSection
    )
    default int minCosmicRunes()
    {
        return 1;
    }

    @ConfigSection(
        name = "Display Options",
        description = "Customize the appearance of warnings",
        position = 5
    )
    String displaySection = "displayOptions";

    @ConfigItem(
        keyName = "customText",
        name = "Custom Text",
        description = "Custom text to display when using CUSTOM_TEXT style",
        position = 0,
        section = displaySection
    )
    default String customText()
    {
        return "Cannot cast thralls!";
    }

    @ConfigItem(
        keyName = "flashReminderBox",
        name = "Flash the Reminder Box",
        description = "Makes the reminder box flash between two colors",
        position = 1,
        section = displaySection
    )
    default boolean flashReminderBox()
    {
        return false;
    }

    @Alpha
    @ConfigItem(
        keyName = "reminderColor",
        name = "Color",
        description = "Main color for the reminder box",
        position = 2,
        section = displaySection
    )
    default Color reminderColor()
    {
        return new Color(255, 0, 0, 150);
    }

    @Alpha
    @ConfigItem(
        keyName = "flashColor",
        name = "Flash Color",
        description = "Secondary color to flash between (if flashing enabled)",
        position = 3,
        section = displaySection
    )
    default Color flashColor()
    {
        return new Color(70, 70, 70, 150);
    }
}
