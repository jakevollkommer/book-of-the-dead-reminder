package com.bookofthedeadnotifier;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.LinkBrowser;

@Slf4j
@PluginDescriptor(
    name = "Book of the Dead Reminder",
    description = "Reminds you when you seem to be missing a thrall requirement (Book of the Dead, Arceuus spellbook, Thrall runes)",
    tags = {"jake", "arceuus", "thrall", "thralls", "book of the dead", "spell", "spellbook", "reminder", "necromancy", "resurrect", "ghost", "skeleton", "zombie", "rune", "runes", "lesser", "superior", "greater", "casts", "air", "earth", "fire", "mind", "death", "blood", "cosmic", "staff", "tome", "rune pouch"}
)
public class BookOfTheDeadNotifierPlugin extends Plugin
{
    private static final int ARCEUUS_SPELLBOOK = 3;
    private static final String SUGGEST_BUTTON_KEY = "suggestButton";
    private static final String SUPPORT_BUTTON_KEY = "supportButton";
    private static final String ISSUES_URL = "https://github.com/jakevollkommer/book-of-the-dead-reminder/issues";
    private static final String KO_FI_URL = "https://ko-fi.com/jakevollkommer";

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
    private PlayerLoadout loadout;

    private boolean hasArceuusSpellbook = false;
    private boolean hasSufficientThrallRunes = false;
    private boolean hasBookOfTheDead = false;
    private boolean warningShown = false;
    private MissingCondition currentMissingCondition = MissingCondition.NONE;
    private int castsAvailable = 0;
    private int magicLevel = 1;

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
        clientThread.invokeLater(this::refreshPlayerState);
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
        if (event.getVarbitId() == VarbitID.SPELLBOOK || PlayerLoadout.isRunePouchVarbit(event.getVarbitId()))
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

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (isInventoryOrEquipment(event.getContainerId()))
        {
            refreshPlayerState();
        }
    }

    // Magic experience arrives constantly in combat, so only a level change can move the auto tier.
    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        boolean magicLevelChanged = event.getSkill() == Skill.MAGIC && event.getLevel() != magicLevel;
        if (magicLevelChanged)
        {
            refreshPlayerState();
        }
    }

    // The config panel cannot host real buttons, so the Feedback "buttons" are checkboxes
    // that act as buttons: any click of the box, tick or untick, opens the link.
    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!BookOfTheDeadNotifierConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }

        if (openFeedbackLink(event))
        {
            return;
        }

        clientThread.invokeLater(this::refreshPlayerState);
    }

    private boolean openFeedbackLink(ConfigChanged event)
    {
        if (event.getNewValue() == null)
        {
            return false;
        }

        if (SUGGEST_BUTTON_KEY.equals(event.getKey()))
        {
            LinkBrowser.browse(ISSUES_URL);
            return true;
        }

        if (SUPPORT_BUTTON_KEY.equals(event.getKey()))
        {
            LinkBrowser.browse(KO_FI_URL);
            return true;
        }

        return false;
    }

    private void refreshPlayerState()
    {
        magicLevel = client.getRealSkillLevel(Skill.MAGIC);
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

    private void checkSpellbook()
    {
        hasArceuusSpellbook = client.getVarbitValue(VarbitID.SPELLBOOK) == ARCEUUS_SPELLBOOK;
    }

    private void checkThrallRunes()
    {
        ThrallTier tier = config.thrallTier().resolve(magicLevel);
        castsAvailable = loadout.castsAvailable(tier);
        hasSufficientThrallRunes = castsAvailable >= config.minCasts();
    }

    private void checkBookOfTheDead()
    {
        hasBookOfTheDead = loadout.carries(ItemID.BOOK_OF_THE_DEAD);
    }

    private void evaluateWarningState()
    {
        boolean shouldWarn = countConditionsMet() == 2;

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

    public String getReminderLongText()
    {
        return isRunningLowOnRunes()
            ? "Low on thrall runes (" + describeCastsRemaining() + ")"
            : currentMissingCondition.getLongText();
    }

    public String getReminderShortText()
    {
        return isRunningLowOnRunes()
            ? describeCastsRemaining()
            : currentMissingCondition.getShortText();
    }

    // Below the configured minimum but not empty, so naming the shortfall beats "missing runes".
    private boolean isRunningLowOnRunes()
    {
        return currentMissingCondition == MissingCondition.THRALL_RUNES && castsAvailable > 0;
    }

    private String describeCastsRemaining()
    {
        return castsAvailable == 1 ? "1 cast" : castsAvailable + " casts";
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

        notifier.notify(config.notification(), "Thrall Reminder: " + getReminderLongText());
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
