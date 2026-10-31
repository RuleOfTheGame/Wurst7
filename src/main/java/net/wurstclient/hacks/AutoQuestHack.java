
/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */


package net.wurstclient.hacks;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public final class AutoQuestHack extends Hack implements UpdateListener
{
    private final CheckboxSetting automob1 =
        new CheckboxSetting("Automob1", true);

    private final CheckboxSetting automob2 =
        new CheckboxSetting("Automob2", true);

    private final CheckboxSetting autoboss =
        new CheckboxSetting("Autoboss", false);

    private final SliderSetting bossInterval =
        new SliderSetting("Autoboss interval", 3, 3, 6, 1,
            ValueDisplay.INTEGER.withSuffix(" minutes"));

    private static final int AUTOMOB_INTERVAL = 5 * 20;
    private static final int CLICK_DELAY = 10;

    private int automob1Timer;
    private int automob2Timer;
    private int autobossTimer;

    private boolean waitingForGui;
    private int pendingSlots;
    private Screen previousScreen;

    private final int[] clickQueue = new int[3];
    private int clickQueueSize;
    private int nextClickIndex;
    private int clickDelayTimer;

    public AutoQuestHack()
    {
        super("AutoQuest");
        setCategory(Category.ITEMS);

        addSetting(automob1);
        addSetting(automob2);
        addSetting(autoboss);
        addSetting(bossInterval);
    }

    @Override
    protected void onEnable()
    {
        automob1Timer = 0;
        automob2Timer = 0;
        autobossTimer = 0;

        waitingForGui = false;
        pendingSlots = 0;

        clickQueueSize = 0;
        nextClickIndex = 0;
        clickDelayTimer = 0;

        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);

        waitingForGui = false;
        pendingSlots = 0;

        clickQueueSize = 0;
        nextClickIndex = 0;
        clickDelayTimer = 0;
    }

    @Override
    public void onUpdate()
    {
        if(MC.player == null || MC.getConnection() == null)
            return;

        // Continue clicking the queued slots one by one.
        if(clickQueueSize > 0)
        {
            if(MC.screen instanceof AbstractContainerScreen<?> screen)
                processQuestClicks(screen);

            return;
        }

        if(waitingForGui)
        {
            handleQuestGui();
            return;
        }

        if(automob1.isChecked())
            automob1Timer++;
        else
            automob1Timer = 0;

        if(automob2.isChecked())
            automob2Timer++;
        else
            automob2Timer = 0;

        if(autoboss.isChecked())
            autobossTimer++;
        else
            autobossTimer = 0;

        if(automob1.isChecked()
            && automob1Timer >= AUTOMOB_INTERVAL)
        {
            pendingSlots |= 1;
        }

        if(automob2.isChecked()
            && automob2Timer >= AUTOMOB_INTERVAL)
        {
            pendingSlots |= 2;
        }

        int bossIntervalTicks =
            bossInterval.getValueI() * 60 * 20;

        if(autoboss.isChecked()
            && autobossTimer >= bossIntervalTicks)
        {
            pendingSlots |= 4;
        }

        if(pendingSlots != 0)
            openQuestGui();
    }

    private void openQuestGui()
    {
        if(MC.player == null || MC.getConnection() == null)
            return;

        previousScreen = MC.screen;

        MC.player.connection.sendCommand("quest");

        waitingForGui = true;
    }

    private void handleQuestGui()
    {
        if(!(MC.screen instanceof AbstractContainerScreen<?> screen))
            return;

        if(MC.screen == previousScreen)
            return;

        clickQueueSize = 0;

        if((pendingSlots & 1) != 0)
            clickQueue[clickQueueSize++] = 27;

        if((pendingSlots & 2) != 0)
            clickQueue[clickQueueSize++] = 28;

        if((pendingSlots & 4) != 0)
            clickQueue[clickQueueSize++] = 29;

        nextClickIndex = 0;
        clickDelayTimer = 0;
        waitingForGui = false;

        processQuestClicks(screen);
    }

    private void processQuestClicks(
        AbstractContainerScreen<?> screen)
    {
        if(nextClickIndex >= clickQueueSize)
        {
            clickQueueSize = 0;
            return;
        }

        if(clickDelayTimer > 0)
        {
            clickDelayTimer--;
            return;
        }

        int slotIndex = clickQueue[nextClickIndex];

        if(screen.getMenu().slots.size() <= slotIndex)
        {
            clickQueueSize = 0;
            return;
        }

        Slot slot = screen.getMenu().slots.get(slotIndex);

        screen.slotClicked(
            slot, slot.index, 0, ClickType.PICKUP);

        if(slotIndex == 27)
        {
            pendingSlots &= ~1;
            automob1Timer = 0;
        }
        else if(slotIndex == 28)
        {
            pendingSlots &= ~2;
            automob2Timer = 0;
        }
        else if(slotIndex == 29)
        {
            pendingSlots &= ~4;
            autobossTimer = 0;
        }

        nextClickIndex++;

        if(nextClickIndex >= clickQueueSize)
        {
            clickQueueSize = 0;
            nextClickIndex = 0;
            clickDelayTimer = 0;
        }
        else
            clickDelayTimer = CLICK_DELAY;
    }
}
