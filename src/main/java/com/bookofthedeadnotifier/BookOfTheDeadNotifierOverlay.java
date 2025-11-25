package com.bookofthedeadnotifier;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.*;

public class BookOfTheDeadNotifierOverlay extends OverlayPanel
{
    private final Client client;
    private final BookOfTheDeadNotifierPlugin plugin;
    private final BookOfTheDeadNotifierConfig config;

    @Inject
    private BookOfTheDeadNotifierOverlay(Client client, BookOfTheDeadNotifierPlugin plugin, BookOfTheDeadNotifierConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!shouldRenderWarning())
        {
            return null;
        }

        String displayText = getDisplayText();
        if (displayText == null)
        {
            return null;
        }

        setupPanelContent(displayText);
        configurePanelSize(graphics, displayText);
        configurePanelColor();

        return renderPanel(graphics);
    }

    private boolean shouldRenderWarning()
    {
        if (!plugin.shouldShowWarning())
        {
            return false;
        }

        MissingCondition condition = plugin.getCurrentMissingCondition();
        return condition != MissingCondition.NONE;
    }

    private void setupPanelContent(String displayText)
    {
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(LineComponent.builder()
            .left(displayText)
            .build());
    }

    private void configurePanelSize(Graphics2D graphics, String displayText)
    {
        FontMetrics fontMetrics = graphics.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(displayText);
        int padding = getTextPadding();
        int totalWidth = textWidth + padding;

        panelComponent.setPreferredSize(new Dimension(totalWidth, 0));
    }

    private void configurePanelColor()
    {
        Color backgroundColor = getCurrentBackgroundColor();
        panelComponent.setBackgroundColor(backgroundColor);
    }

    private Color getCurrentBackgroundColor()
    {
        if (shouldFlash())
        {
            return config.flashColor();
        }
        return config.reminderColor();
    }

    private boolean shouldFlash()
    {
        if (!config.flashReminderBox())
        {
            return false;
        }

        int gameCycle = client.getGameCycle();
        return gameCycle % 40 >= 20;
    }

    private Dimension renderPanel(Graphics2D graphics)
    {
        boolean useCustomTextStyle = config.reminderStyle() == BookOfTheDeadNotifierStyle.CUSTOM_TEXT;
        if (useCustomTextStyle)
        {
            return super.render(graphics);
        }
        return panelComponent.render(graphics);
    }

    private String getDisplayText()
    {
        BookOfTheDeadNotifierStyle style = config.reminderStyle();
        
        if (style == BookOfTheDeadNotifierStyle.CUSTOM_TEXT)
        {
            return config.customText();
        }

        MissingCondition condition = plugin.getCurrentMissingCondition();
        
        if (style == BookOfTheDeadNotifierStyle.LONG_TEXT)
        {
            return condition.getLongText();
        }
        
        if (style == BookOfTheDeadNotifierStyle.SHORT_TEXT)
        {
            return condition.getShortText();
        }
        
        return null;
    }

    private int getTextPadding()
    {
        switch (config.reminderStyle())
        {
            case LONG_TEXT:
            case CUSTOM_TEXT:
                return -20;
            case SHORT_TEXT:
                return 10;
            default:
                return 0;
        }
    }
}
