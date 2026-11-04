
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
import net.wurstclient.settings.EnumSetting;

public final class AutoQuestHack extends Hack implements UpdateListener
{
    private enum QuestMode
    {
        QUEST_28_29("Quest 28 -> 29"),
        QUEST_53_2("Quest 53 -> 2");

        private final String displayName;

        QuestMode(String displayName)
        {
            this.displayName = displayName;
        }

        @Override
        public String toString()
        {
            return displayName;
        }
    }

    private final EnumSetting<QuestMode> mode =
        new EnumSetting<>("Mode", QuestMode.values(),
            QuestMode.QUEST_28_29);

    private int timer;
    private int step;
    private int stepDelayTimer;

    private int firstSlot;
    private int secondSlot;
    private int stepDelay;
    private int cycleInterval;

    private boolean waitingForGui;
    private Screen previousScreen;

    public AutoQuestHack()
    {
        super("AutoQuest");
        setCategory(Category.ITEMS);

        addSetting(mode);
    }

    @Override
    protected void onEnable()
    {
        timer = 0;
        step = 0;
        stepDelayTimer = 0;
        waitingForGui = false;

        loadModeSettings();

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

    private void loadModeSettings()
    {
        if(mode.getSelected() == QuestMode.QUEST_28_29)
        {
            // Mode 1:
            // Slot 28 -> wait 1.5 seconds -> slot 29
            // Repeat 7 seconds after slot 29.
            firstSlot = 27;
            secondSlot = 28;
            stepDelay = 30;
            cycleInterval = 7 * 20;
        }
        else
        {
            // Mode 2:
            // Slot 53 -> wait 1 second -> slot 2
            // Repeat 6 seconds after slot 2.
            firstSlot = 52;
            secondSlot = 1;
            stepDelay = 20;
            cycleInterval = 6 * 20;
        }
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

        // Wait between the first and second actions.
        if(step == 1)
        {
            stepDelayTimer--;

            if(stepDelayTimer <= 0)
                openQuestGui();

            return;
        }

        // Wait for the next cycle after the second action.
        if(step == 2)
        {
            timer++;

            if(timer >= cycleInterval)
                startCycle();

            return;
        }

        // Initial wait before the first cycle.
        timer++;

        if(timer >= cycleInterval)
            startCycle();
    }

    private void startCycle()
    {
        loadModeSettings();

        timer = 0;
        step = 0;

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

        if(step == 0)
        {
            // First action.
            if(!clickQuestSlot(screen, firstSlot))
            {
                waitingForGui = false;
                return;
            }

            step = 1;
            waitingForGui = false;

            // Delay before opening the GUI for the second action.
            stepDelayTimer = stepDelay;
        }
        else if(step == 1)
        {
            // Second action.
            if(!clickQuestSlot(screen, secondSlot))
            {
                waitingForGui = false;
                stepDelayTimer = 1;
                return;
            }

            step = 2;
            waitingForGui = false;

            // Start the cycle timer after the second click.
            timer = 0;
        }
    }

    private boolean clickQuestSlot(
        AbstractContainerScreen<?> screen, int slotIndex)
    {
        if(screen.getMenu().slots.size() <= slotIndex)
            return false;

        Slot slot = screen.getMenu().slots.get(slotIndex);

        screen.slotClicked(
            slot, slot.index, 0, ClickType.PICKUP);

        return true;
    }
}
