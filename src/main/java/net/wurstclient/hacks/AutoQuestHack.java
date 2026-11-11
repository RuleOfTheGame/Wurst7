
/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General
 * Public License, version 3.
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

public final class AutoQuestHack extends Hack implements UpdateListener
{
    private static final int QUEST1_INTERVAL = 7 * 20;
    private static final int QUEST2_INTERVAL = 6 * 20;

    private static final int QUEST1_DELAY = 30;
    private static final int QUEST2_DELAY = 20;

    private final CheckboxSetting quest1 =
        new CheckboxSetting("Quest 1", true);

    private final CheckboxSetting quest2 =
        new CheckboxSetting("Quest 2", false);

    private int quest1Timer;
    private int quest2Timer;

    private boolean quest1Pending;
    private boolean quest2Pending;

    private int activeRoutine;
    private int step;
    private int waitTimer;

    private boolean waitingForGui;
    private Screen previousScreen;

    public AutoQuestHack()
    {
        super("AutoQuest");
        setCategory(Category.ITEMS);

        addSetting(quest1);
        addSetting(quest2);
    }

    @Override
    protected void onEnable()
    {
        resetState();

        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);

        resetState();
    }

    private void resetState()
    {
        quest1Timer = 0;
        quest2Timer = 0;

        quest1Pending = false;
        quest2Pending = false;

        activeRoutine = 0;
        step = 0;
        waitTimer = 0;

        waitingForGui = false;
    }

    @Override
    public void onUpdate()
    {
        if(MC.player == null || MC.getConnection() == null)
            return;

        // Stop a routine if its checkbox is disabled.
        if(activeRoutine == 1 && !quest1.isChecked()
            || activeRoutine == 2 && !quest2.isChecked())
        {
            finishRoutine();
        }

        updateRoutineTimers();

        if(waitingForGui)
        {
            handleQuestGui();
            return;
        }

        if(activeRoutine != 0)
        {
            handleRoutineDelay();
            return;
        }

        // Start pending routines one at a time.
        if(quest1Pending)
        {
            startRoutine(1);
            return;
        }

        if(quest2Pending)
            startRoutine(2);
    }

    private void updateRoutineTimers()
    {
        if(!quest1.isChecked())
        {
            quest1Timer = 0;
            quest1Pending = false;
        }
        else if(activeRoutine != 1 && !quest1Pending)
        {
            quest1Timer++;

            if(quest1Timer >= QUEST1_INTERVAL)
                quest1Pending = true;
        }

        if(!quest2.isChecked())
        {
            quest2Timer = 0;
            quest2Pending = false;
        }
        else if(activeRoutine != 2 && !quest2Pending)
        {
            quest2Timer++;

            if(quest2Timer >= QUEST2_INTERVAL)
                quest2Pending = true;
        }
    }

    private void startRoutine(int routine)
    {
        activeRoutine = routine;
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

    private void handleRoutineDelay()
    {
        if(waitTimer <= 0)
            return;

        waitTimer--;

        if(waitTimer > 0)
            return;

        if(activeRoutine == 1 && step == 1)
        {
            step = 2;
            openQuestGui();
        }
        else if(activeRoutine == 2 && step == 1)
        {
            step = 2;
            openQuestGui();
        }
        else if(activeRoutine == 2 && step == 3)
        {
            step = 4;
            openQuestGui();
        }
        else if(activeRoutine == 2 && step == 5)
        {
            step = 6;
            openQuestGui();
        }
    }

    private void handleQuestGui()
    {
        if(!(MC.screen instanceof AbstractContainerScreen<?> screen))
            return;

        if(MC.screen == previousScreen)
            return;

        if(activeRoutine == 1)
            handleQuest1(screen);
        else if(activeRoutine == 2)
            handleQuest2(screen);
    }

    private void handleQuest1(AbstractContainerScreen<?> screen)
    {
        if(step == 0)
        {
            // Quest 1: click slot 28.
            if(!clickQuestSlot(screen, 27))
            {
                finishRoutine();
                return;
            }

            step = 1;
            waitTimer = QUEST1_DELAY;
            waitingForGui = false;
        }
        else if(step == 2)
        {
            // Quest 1: click slot 29.
            if(!clickQuestSlot(screen, 28))
            {
                finishRoutine();
                return;
            }

            finishRoutine();
        }
    }

    private void handleQuest2(AbstractContainerScreen<?> screen)
    {
        if(step == 0)
        {
            // Quest 2: first click slot 53.
            if(!clickQuestSlot(screen, 52))
            {
                finishRoutine();
                return;
            }

            step = 1;
            waitTimer = QUEST2_DELAY;
            waitingForGui = false;
        }
        else if(step == 2)
        {
            // Quest 2: click slot 4.
            if(!clickQuestSlot(screen, 3))
            {
                finishRoutine();
                return;
            }

            // Reopen the GUI after the previous GUI closes.
            step = 3;
            waitTimer = 1;
            waitingForGui = false;
        }
        else if(step == 4)
        {
            // Quest 2: second click slot 53.
            if(!clickQuestSlot(screen, 52))
            {
                finishRoutine();
                return;
            }

            step = 5;
            waitTimer = QUEST2_DELAY;
            waitingForGui = false;
        }
        else if(step == 6)
        {
            // Quest 2: click slot 5.
            if(!clickQuestSlot(screen, 4))
            {
                finishRoutine();
                return;
            }

            finishRoutine();
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

    private void finishRoutine()
    {
        if(activeRoutine == 1)
        {
            quest1Timer = 0;
            quest1Pending = false;
        }
        else if(activeRoutine == 2)
        {
            quest2Timer = 0;
            quest2Pending = false;
        }

        activeRoutine = 0;
        step = 0;
        waitTimer = 0;
        waitingForGui = false;
    }
}

