package com.bookofthedeadnotifier;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
    name = "Book of the Dead Reminder",
    description = "Reminds you when you seem to be missing a thrall requirement (Book of the Dead, Arceuus spellbook, Thrall runes)",
    tags = {"arceuus", "thrall", "thralls", "book of the dead", "spell", "reminder", "spellbook"}
)
public class BookOfTheDeadNotifierPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private BookOfTheDeadNotifierConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private BookOfTheDeadNotifierOverlay overlay;

    @Inject
    private Notifier notifier;

    @Inject
    private KeyManager keyManager;

    private boolean hasArceuusSpellbook = false;
    private boolean hasSufficientThrallRunes = false;
    private boolean hasBookOfTheDead = false;
    private boolean warningShown = false;
    private MissingCondition currentMissingCondition = MissingCondition.NONE;

    private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hideReminderHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            hideWarning();
        }
    };

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        keyManager.registerKeyListener(hotkeyListener);
        log.info("Book of the Dead Reminder started!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        keyManager.unregisterKeyListener(hotkeyListener);
        log.info("Book of the Dead Reminder stopped!");
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if (event.getVarbitId() == Varbits.SPELLBOOK)
        {
            checkSpellbook();
            evaluateWarningState();
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        boolean isRelevantContainer = isInventoryOrEquipment(event.getContainerId());
        if (isRelevantContainer)
        {
            checkThrallRunes();
            checkBookOfTheDead();
            evaluateWarningState();
        }
    }

    private boolean isInventoryOrEquipment(int containerId)
    {
        return containerId == InventoryID.INVENTORY.getId() 
            || containerId == InventoryID.EQUIPMENT.getId();
    }

    private void evaluateWarningState()
    {
        int conditionsMet = countConditionsMet();
        boolean shouldWarn = conditionsMet == 2;

        if (shouldWarn)
        {
            handleWarningState();
        }
        else
        {
            hideWarning();
        }
    }

    private int countConditionsMet()
    {
        int count = 0;
        if (hasArceuusSpellbook) count++;
        if (hasSufficientThrallRunes) count++;
        if (hasBookOfTheDead) count++;
        return count;
    }

    private void handleWarningState()
    {
        MissingCondition missingCondition = determineMissingCondition();
        boolean conditionChanged = missingCondition != currentMissingCondition;

        if (conditionChanged)
        {
            currentMissingCondition = missingCondition;
            showWarning();
        }
    }

    private MissingCondition determineMissingCondition()
    {
        if (!hasBookOfTheDead)
        {
            return MissingCondition.BOOK_OF_THE_DEAD;
        }
        
        if (!hasArceuusSpellbook)
        {
            return MissingCondition.ARCEUUS_SPELLBOOK;
        }
        
        if (!hasSufficientThrallRunes)
        {
            return MissingCondition.THRALL_RUNES;
        }
        
        return MissingCondition.NONE;
    }

    public MissingCondition getCurrentMissingCondition()
    {
        return currentMissingCondition;
    }

    private void checkSpellbook()
    {
        int spellbookVarbit = client.getVarbitValue(Varbits.SPELLBOOK);
        hasArceuusSpellbook = (spellbookVarbit == 3); // 3 = Arceuus spellbook
    }

    private void checkThrallRunes()
    {
        int fireRunes = countFireRunes();
        int bloodRunes = countBloodRunes();
        int cosmicRunes = countCosmicRunes();

        boolean hasEnoughFire = fireRunes >= config.minFireRunes();
        boolean hasEnoughBlood = bloodRunes >= config.minBloodRunes();
        boolean hasEnoughCosmic = cosmicRunes >= config.minCosmicRunes();

        hasSufficientThrallRunes = hasEnoughFire && hasEnoughBlood && hasEnoughCosmic;
    }

    private int countFireRunes()
    {
        if (hasEquippedFireStaff())
        {
            return Integer.MAX_VALUE;
        }

        int total = 0;
        total += countRunesInInventory(this::isFireRune);
        total += countRunesInRunePouch(this::isFireRune);
        return total;
    }

    private int countBloodRunes()
    {
        int total = 0;
        total += countRunesInInventory(this::isBloodRune);
        total += countRunesInRunePouch(this::isBloodRune);
        return total;
    }

    private int countCosmicRunes()
    {
        int total = 0;
        total += countRunesInInventory(this::isCosmicRune);
        total += countRunesInRunePouch(this::isCosmicRune);
        return total;
    }

    private int countRunesInInventory(RuneChecker checker)
    {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        if (inventory == null)
        {
            return 0;
        }

        int count = 0;
        for (Item item : inventory.getItems())
        {
            if (checker.matches(item.getId()))
            {
                count += item.getQuantity();
            }
        }
        return count;
    }

    private int countRunesInRunePouch(RuneChecker checker)
    {
        int count = 0;
        for (int slot = 1; slot <= 6; slot++)
        {
            count += countRunesInPouchSlot(slot, checker);
        }
        return count;
    }

    private int countRunesInPouchSlot(int slot, RuneChecker checker)
    {
        int runeId = getRunePouchRuneId(slot);
        int amount = getRunePouchAmount(slot);

        if (amount > 0 && checker.matches(runeId))
        {
            return amount;
        }
        return 0;
    }

    private int getRunePouchRuneId(int slot)
    {
        switch (slot)
        {
            case 1: return client.getVarbitValue(Varbits.RUNE_POUCH_RUNE1);
            case 2: return client.getVarbitValue(Varbits.RUNE_POUCH_RUNE2);
            case 3: return client.getVarbitValue(Varbits.RUNE_POUCH_RUNE3);
            case 4: return client.getVarbitValue(Varbits.RUNE_POUCH_RUNE4);
            case 5: return client.getVarbitValue(Varbits.RUNE_POUCH_RUNE5);
            case 6: return client.getVarbitValue(Varbits.RUNE_POUCH_RUNE6);
            default: return 0;
        }
    }

    private int getRunePouchAmount(int slot)
    {
        switch (slot)
        {
            case 1: return client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT1);
            case 2: return client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT2);
            case 3: return client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT3);
            case 4: return client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT4);
            case 5: return client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT5);
            case 6: return client.getVarbitValue(Varbits.RUNE_POUCH_AMOUNT6);
            default: return 0;
        }
    }

    private boolean isFireRune(int itemId)
    {
        switch (itemId)
        {
            case ItemID.FIRE_RUNE:
            case ItemID.LAVA_RUNE:
            case ItemID.SMOKE_RUNE:
            case ItemID.STEAM_RUNE:
                return true;
            default:
                return false;
        }
    }

    private boolean isBloodRune(int itemId)
    {
        return itemId == ItemID.BLOOD_RUNE;
    }

    private boolean isCosmicRune(int itemId)
    {
        switch (itemId)
        {
            case ItemID.COSMIC_RUNE:
            case ItemID.AETHER_RUNE:
                return true;
            default:
                return false;
        }
    }

    @FunctionalInterface
    private interface RuneChecker
    {
        boolean matches(int itemId);
    }

    private boolean hasEquippedFireStaff()
    {
        Item weapon = getEquippedWeapon();
        if (weapon == null)
        {
            return false;
        }

        return isFireStaff(weapon.getId());
    }

    private Item getEquippedWeapon()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return null;
        }

        return equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
    }

    private boolean isFireStaff(int itemId)
    {
        switch (itemId)
        {
            case ItemID.STAFF_OF_FIRE:
            case ItemID.FIRE_BATTLESTAFF:
            case ItemID.MYSTIC_FIRE_STAFF:
            case ItemID.LAVA_BATTLESTAFF:
            case ItemID.MYSTIC_LAVA_STAFF:
            case ItemID.STEAM_BATTLESTAFF:
            case ItemID.MYSTIC_STEAM_STAFF:
            case ItemID.SMOKE_BATTLESTAFF:
            case ItemID.MYSTIC_SMOKE_STAFF:
            case ItemID.TOME_OF_FIRE:
            case ItemID.TWINFLAME_STAFF:
                return true;
            default:
                return false;
        }
    }

    private void checkBookOfTheDead()
    {
        hasBookOfTheDead = hasItemInEquipmentOrInventory(ItemID.BOOK_OF_THE_DEAD);
    }

    private boolean hasItemInEquipmentOrInventory(int itemId)
    {
        if (hasItemInEquipment(itemId))
        {
            return true;
        }

        if (hasItemInInventory(itemId))
        {
            return true;
        }

        return false;
    }

    private boolean hasItemInEquipment(int itemId)
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return false;
        }

        return containsItem(equipment, itemId);
    }

    private boolean hasItemInInventory(int itemId)
    {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        if (inventory == null)
        {
            return false;
        }

        return containsItem(inventory, itemId);
    }

    private boolean containsItem(ItemContainer container, int itemId)
    {
        for (Item item : container.getItems())
        {
            if (item.getId() == itemId)
            {
                return true;
            }
        }
        return false;
    }

    private void showWarning()
    {
        boolean isFirstWarning = !warningShown;
        if (isFirstWarning)
        {
            warningShown = true;
            sendNotification();
        }
    }

    private void sendNotification()
    {
        if (!config.notification().isEnabled())
        {
            return;
        }

        String message = currentMissingCondition.getLongText();
        String notificationMessage = "Thrall Reminder: " + message;
        notifier.notify(config.notification(), notificationMessage);
    }

    private void hideWarning()
    {
        if (!warningShown)
        {
            return;
        }

        warningShown = false;
        currentMissingCondition = MissingCondition.NONE;
    }

    public boolean shouldShowWarning()
    {
        return warningShown;
    }

    @Provides
    BookOfTheDeadNotifierConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(BookOfTheDeadNotifierConfig.class);
    }
}
