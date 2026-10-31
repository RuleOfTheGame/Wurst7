

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
    private static final int STEP_DELAY = 30;

    private int timer;
    private int step;
    private int stepDelayTimer;

    private boolean waitingForGui;
    private Screen previousScreen;

    public AutoQuestHack()
    {
        super("AutoQuest");
        setCategory(Category.ITEMS);
    }

    @Override
    protected void onEnable()
    {
        timer = 0;
        step = 0;
        stepDelayTimer = 0;
        waitingForGui = false;

        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);

        timer = 0;
        step = 0;
        stepDelayTimer = 0;
        waitingForGui = false;
    }

    @Override
    public void onUpdate()
    {
        if(MC.player == null || MC.getConnection() == null)
            return;

        if(waitingForGui)
        {
            handleQuestGui();
            return;
        }

        // Wait 1.5 seconds between the first and second clicks.
        if(step == 1)
        {
            stepDelayTimer--;

            if(stepDelayTimer <= 0)
                openQuestGui();

            return;
        }

        // Wait 5 seconds after the second click.
        if(step == 2)
        {
            timer++;

            if(timer >= CYCLE_INTERVAL)
            {
                timer = 0;
                step = 0;
                openQuestGui();
            }

            return;
        }

        // Start the first quest cycle immediately after 5 seconds.
        timer++;

        if(timer >= CYCLE_INTERVAL)
        {
            timer = 0;
            step = 0;
            openQuestGui();
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

        if(step == 0)
        {
            // First action: click slot 28 (index 27).
            clickQuestSlot(screen, 27);

            step = 1;
            waitingForGui = false;

            // Wait 1.5 seconds before opening the GUI again.
            stepDelayTimer = STEP_DELAY;
        }
        else if(step == 1)
        {
            // Second action: click slot 29 (index 28).
            clickQuestSlot(screen, 28);

            step = 2;
            waitingForGui = false;

            // Start the 5-second timer after the second click.
            timer = 0;
        }
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
