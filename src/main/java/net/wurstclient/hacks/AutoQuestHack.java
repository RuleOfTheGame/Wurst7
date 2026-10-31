

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

public final class AutoQuestHack extends Hack implements UpdateListener
{
    private static final int CYCLE_INTERVAL = 5 * 20;
    private static final int SECOND_CLICK_DELAY = 20;

    private int cycleTimer;
    private int secondClickTimer;

    private boolean waitingForGui;
    private boolean waitingForSecondClick;

    private Screen previousScreen;

    public AutoQuestHack()
    {
        super("AutoQuest");
        setCategory(Category.ITEMS);
    }

    @Override
    protected void onEnable()
    {
        cycleTimer = CYCLE_INTERVAL;
        secondClickTimer = 0;

        waitingForGui = false;
        waitingForSecondClick = false;

        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);

        waitingForGui = false;
        waitingForSecondClick = false;

        secondClickTimer = 0;
    }

    @Override
    public void onUpdate()
    {
        if(MC.player == null || MC.getConnection() == null)
            return;

        cycleTimer++;

        // Wait for the quest GUI to open.
        if(waitingForGui)
        {
            handleQuestGui();
            return;
        }

        // Wait 1 second before clicking the second slot.
        if(waitingForSecondClick)
        {
            handleSecondClick();
            return;
        }

        // Repeat the whole process every 5 seconds.
        if(cycleTimer >= CYCLE_INTERVAL)
        {
            openQuestGui();
            cycleTimer = 0;
        }
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

        // First click: slot 28 (index 27).
        clickQuestSlot(screen, 27);

        // Schedule the second click after 1 second.
        waitingForGui = false;
        waitingForSecondClick = true;
        secondClickTimer = SECOND_CLICK_DELAY;
    }

    private void handleSecondClick()
    {
        secondClickTimer--;

        if(secondClickTimer > 0)
            return;

        if(!(MC.screen instanceof AbstractContainerScreen<?> screen))
        {
            waitingForSecondClick = false;
            return;
        }

        // Second click: slot 29 (index 28).
        clickQuestSlot(screen, 28);

        waitingForSecondClick = false;
    }

    private void clickQuestSlot(
        AbstractContainerScreen<?> screen, int slotIndex)
    {
        if(screen.getMenu().slots.size() <= slotIndex)
            return;

        Slot slot = screen.getMenu().slots.get(slotIndex);

        screen.slotClicked(
            slot, slot.index, 0, ClickType.PICKUP);
    }
}
