```java
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
        QUEST_53_2("Quest 53 -> 2"),
        QUEST_53_4_53_5("Quest 53 -> 4 -> 53 -> 5");

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

    private QuestMode activeMode;

    private int timer;
    private int step;
    private int stepDelayTimer;

    private int firstSlot;
    private int secondSlot;
    private int thirdSlot;
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
        activeMode = mode.getSelected();

        switch(activeMode)
        {
            case QUEST_28_29:
                // Slot 28 -> wait 1.5 seconds
                // -> reopen /quest -> slot 29
                // -> repeat after 7 seconds.
                firstSlot = 27;
                secondSlot = 28;
                thirdSlot = -1;
                stepDelay = 30;
                cycleInterval = 7 * 20;
                break;

            case QUEST_53_2:
                // /quest -> slot 53 -> wait 1 second
                // -> slot 2 on transitioned GUI
                // -> repeat after 6 seconds.
                firstSlot = 52;
                secondSlot = 1;
                thirdSlot = -1;
                stepDelay = 20;
                cycleInterval = 6 * 20;
                break;

            case QUEST_53_4_53_5:
                // /quest -> slot 53 -> wait 1 second
                // -> slot 4 -> /quest -> slot 53
                // -> wait 1 second -> slot 5
                // -> repeat after 6 seconds.
                firstSlot = 52;
                secondSlot = 3;
                thirdSlot = 4;
                stepDelay = 20;
                cycleInterval = 6 * 20;
                break;
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

        // Step 1 or 4: wait before the next click.
        if(step == 1 || step == 4)
        {
            stepDelayTimer--;

            if(stepDelayTimer <= 0)
            {
                if(step == 1)
                {
                    if(activeMode == QuestMode.QUEST_28_29)
                        openQuestGui();
                    else
                        clickSecondSlotOnCurrentGui();
                }
                else
                    clickThirdSlotOnCurrentGui();
            }

            return;
        }

        // Step 2: wait before starting the next cycle.
        if(step == 2)
        {
            timer++;

            if(timer >= cycleInterval)
                startCycle();

            return;
        }

        // Step 0: initial wait.
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
            // First click of the cycle.
            if(!clickQuestSlot(screen, firstSlot))
            {
                finishCycle();
                return;
            }

            step = 1;
            waitingForGui = false;
            stepDelayTimer = stepDelay;
        }
        else if(step == 1
            && activeMode == QuestMode.QUEST_28_29)
        {
            // Mode 1: click slot 29 after reopening /quest.
            if(!clickQuestSlot(screen, secondSlot))
            {
                finishCycle();
                return;
            }

            finishCycle();
        }
        else if(step == 3)
        {
            // Mode 3: second /quest, click slot 53.
            if(!clickQuestSlot(screen, firstSlot))
            {
                finishCycle();
                return;
            }

            step = 4;
            waitingForGui = false;
            stepDelayTimer = stepDelay;
        }
    }

    private void clickSecondSlotOnCurrentGui()
    {
        if(!(MC.screen instanceof AbstractContainerScreen<?> screen))
        {
            stepDelayTimer = 1;
            return;
        }

        if(!clickQuestSlot(screen, secondSlot))
        {
            finishCycle();
            return;
        }

        if(activeMode == QuestMode.QUEST_53_4_53_5)
        {
            // After slot 4, send /quest again.
            step = 3;
            openQuestGui();
        }
        else
            finishCycle();
    }

    private void clickThirdSlotOnCurrentGui()
    {
        if(!(MC.screen instanceof AbstractContainerScreen<?> screen))
        {
            stepDelayTimer = 1;
            return;
        }

        if(!clickQuestSlot(screen, thirdSlot))
        {
            finishCycle();
            return;
        }

        finishCycle();
    }

    private void finishCycle()
    {
        step = 2;
        timer = 0;
        waitingForGui = false;
    }

    private boolean clickQuestSlot(
        AbstractContainerScreen<?> screen, int slotIndex)
    {
        if(slotIndex < 0
            || screen.getMenu().slots.size() <= slotIndex)
            return false;

        Slot slot = screen.getMenu().slots.get(slotIndex);

        screen.slotClicked(
            slot, slot.index, 0, ClickType.PICKUP);

        return true;
    }
}
```
