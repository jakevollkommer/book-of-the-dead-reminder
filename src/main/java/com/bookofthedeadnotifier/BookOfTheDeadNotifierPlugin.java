package com.bookofthedeadnotifier;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
    name = "Book of the Dead Reminder",
    description = "Reminds you when you seem to be missing a thrall requirement (Book of the Dead, Arceuus spellbook, Thrall runes)",
    tags = {"arceuus", "thrall", "thralls", "book of the dead", "spell", "reminder", "spellbook"}
)
public class BookOfTheDeadNotifierPlugin extends Plugin
{
    @Inject
    private ClientToolbar clientToolbar;

    private NavigationButton navigationButton;

    private static final int ARCEUUS_SPELLBOOK = 3;
    private static final int[] RUNE_POUCH_RUNE_VARBITS = {
        VarbitID.RUNE_POUCH_TYPE_1,
        VarbitID.RUNE_POUCH_TYPE_2,
        VarbitID.RUNE_POUCH_TYPE_3,
        VarbitID.RUNE_POUCH_TYPE_4,
        VarbitID.RUNE_POUCH_TYPE_5,
        VarbitID.RUNE_POUCH_TYPE_6
    };
    private static final int[] RUNE_POUCH_AMOUNT_VARBITS = {
        VarbitID.RUNE_POUCH_QUANTITY_1,
        VarbitID.RUNE_POUCH_QUANTITY_2,
        VarbitID.RUNE_POUCH_QUANTITY_3,
        VarbitID.RUNE_POUCH_QUANTITY_4,
        VarbitID.RUNE_POUCH_QUANTITY_5,
        VarbitID.RUNE_POUCH_QUANTITY_6
    };

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

    @Inject
    private ClientThread clientThread;

    @Inject
    private ItemManager itemManager;

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

        navigationButton = NavigationButton.builder()
            .tooltip("Book of the Dead Reminder")
            .icon(ImageUtil.loadImageResource(BookOfTheDeadNotifierPlugin.class, "panel_icon.png"))
            .priority(9)
            .panel(new BookOfTheDeadNotifierPanel())
            .build();
        clientToolbar.addNavigation(navigationButton);
        clientThread.invokeLater(this::refreshPlayerState);
        log.info("Book of the Dead Reminder started!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        keyManager.unregisterKeyListener(hotkeyListener);
        clientToolbar.removeNavigation(navigationButton);
        log.info("Book of the Dead Reminder stopped!");
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if (event.getVarbitId() == VarbitID.SPELLBOOK || isRunePouchVarbit(event.getVarbitId()))
        {
            refreshPlayerState();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            refreshPlayerState();
        }
    }

    private boolean isRunePouchVarbit(int varbitId)
    {
        for (int runeVarbit : RUNE_POUCH_RUNE_VARBITS)
        {
            if (varbitId == runeVarbit)
            {
                return true;
            }
        }

        for (int amountVarbit : RUNE_POUCH_AMOUNT_VARBITS)
        {
            if (varbitId == amountVarbit)
            {
                return true;
            }
        }

        return false;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (isInventoryOrEquipment(event.getContainerId()))
        {
            refreshPlayerState();
        }
    }

    private void refreshPlayerState()
    {
        checkSpellbook();
        checkThrallRunes();
        checkBookOfTheDead();
        evaluateWarningState();
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
        
        if (!isConditionNotificationEnabled(missingCondition))
        {
            hideWarning();
            return;
        }
        
        boolean conditionChanged = missingCondition != currentMissingCondition;

        if (conditionChanged)
        {
            currentMissingCondition = missingCondition;
            showWarning();
        }
    }

    private boolean isConditionNotificationEnabled(MissingCondition condition)
    {
        switch (condition)
        {
            case BOOK_OF_THE_DEAD:
                return config.notifyOnMissingBook();
            case THRALL_RUNES:
                return config.notifyOnMissingRunes();
            case ARCEUUS_SPELLBOOK:
                return config.notifyOnWrongSpellbook();
            default:
                return false;
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
        int spellbookVarbit = client.getVarbitValue(VarbitID.SPELLBOOK);
        hasArceuusSpellbook = spellbookVarbit == ARCEUUS_SPELLBOOK;
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
        if (!hasRunePouch())
        {
            return 0;
        }

        int count = 0;
        for (int slot = 1; slot <= 6; slot++)
        {
            count += countRunesInPouchSlot(slot, checker);
        }
        return count;
    }

    private boolean hasRunePouch()
    {
        return hasItemInEquipmentOrInventory(ItemID.BH_RUNE_POUCH)
            || hasItemInEquipmentOrInventory(ItemID.BH_RUNE_POUCH_TROUVER)
            || hasItemInEquipmentOrInventory(ItemID.DIVINE_RUNE_POUCH)
            || hasItemInEquipmentOrInventory(ItemID.DIVINE_RUNE_POUCH_TROUVER);
    }

    private int countRunesInPouchSlot(int slot, RuneChecker checker)
    {
        int runeEnumId = getRunePouchRuneEnumId(slot);
        int amount = getRunePouchAmount(slot);

        if (runeEnumId == 0 || amount <= 0)
        {
            return 0;
        }

        int itemId = convertRuneEnumIdToItemId(runeEnumId);
        if (checker.matches(itemId))
        {
            return amount;
        }
        return 0;
    }

    private int convertRuneEnumIdToItemId(int runeEnumId)
    {
        EnumComposition runepouchEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
        return runepouchEnum.getIntValue(runeEnumId);
    }

    private int getRunePouchRuneEnumId(int slot)
    {
        if (slot < 1 || slot > RUNE_POUCH_RUNE_VARBITS.length)
        {
            return 0;
        }

        return client.getVarbitValue(RUNE_POUCH_RUNE_VARBITS[slot - 1]);
    }

    private int getRunePouchAmount(int slot)
    {
        if (slot < 1 || slot > RUNE_POUCH_AMOUNT_VARBITS.length)
        {
            return 0;
        }

        return client.getVarbitValue(RUNE_POUCH_AMOUNT_VARBITS[slot - 1]);
    }

    private boolean isFireRune(int itemId)
    {
        switch (itemId)
        {
            case ItemID.FIRERUNE:
            case ItemID.LAVARUNE:
            case ItemID.SMOKERUNE:
            case ItemID.STEAMRUNE:
            case ItemID.SUNFIRERUNE:
                return true;
            default:
                return false;
        }
    }

    private boolean isBloodRune(int itemId)
    {
        return itemId == ItemID.BLOODRUNE;
    }

    private boolean isCosmicRune(int itemId)
    {
        switch (itemId)
        {
            case ItemID.COSMICRUNE:
            case ItemID.AETHERRUNE:
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
        return hasInfiniteFireSourceEquipped(EquipmentInventorySlot.WEAPON)
            || hasInfiniteFireSourceEquipped(EquipmentInventorySlot.SHIELD);
    }

    private boolean hasInfiniteFireSourceEquipped(EquipmentInventorySlot slot)
    {
        Item item = getEquippedItem(slot);
        if (item == null)
        {
            return false;
        }

        return isFireStaff(canonicalizeItemId(item.getId()));
    }

    private Item getEquippedItem(EquipmentInventorySlot slot)
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return null;
        }

        return equipment.getItem(slot.getSlotIdx());
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
            case ItemID.LAVA_BATTLESTAFF_PRETTY:
            case ItemID.MYSTIC_LAVA_STAFF_PRETTY:
            case ItemID.STEAM_BATTLESTAFF:
            case ItemID.MYSTIC_STEAM_BATTLESTAFF:
            case ItemID.STEAM_BATTLESTAFF_PRETTY:
            case ItemID.MYSTIC_STEAM_BATTLESTAFF_PRETTY:
            case ItemID.SMOKE_BATTLESTAFF:
            case ItemID.MYSTIC_SMOKE_BATTLESTAFF:
            case ItemID.TOME_OF_FIRE:
            case ItemID.BR_TOME_OF_FIRE:
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
            if (canonicalizeItemId(item.getId()) == itemId)
            {
                return true;
            }
        }
        return false;
    }

    private int canonicalizeItemId(int itemId)
    {
        return itemManager.canonicalize(itemId);
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
