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
    private int delayTimer;

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
        delayTimer = 0;
        waitingForGui = false;

        activeMode = mode.getSelected();

        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);

        timer = 0;
        step = 0;
        delayTimer = 0;
        waitingForGui = false;
        previousScreen = null;
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

        if(step == 1 || step == 4)
        {
            delayTimer--;

            if(delayTimer <= 0)
            {
                if(step == 1)
                {
                    if(activeMode == QuestMode.QUEST_28_29)
                        openQuestGui();
                    else
                        clickCurrentGui(1);
                }
                else
                    clickCurrentGui(2);
            }

            return;
        }

        if(step == 2)
        {
            timer++;

            if(timer >= 6 * 20)
                startCycle();

            return;
        }

        if(step == 3)
        {
            openQuestGui();
            return;
        }

        timer++;

        int interval = activeMode == QuestMode.QUEST_28_29
            ? 7 * 20 : 6 * 20;

        if(timer >= interval)
            startCycle();
    }

    private void startCycle()
    {
        activeMode = mode.getSelected();
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
            if(!clickSlot(screen, 52))
            {
                finishCycle();
                return;
            }

            step = 1;
            delayTimer = 20;
            waitingForGui = false;
            return;
        }

        if(step == 1 && activeMode == QuestMode.QUEST_28_29)
        {
            if(!clickSlot(screen, 28))
            {
                finishCycle();
                return;
            }

            finishCycle();
            return;
        }

        if(step == 3)
        {
            if(!clickSlot(screen, 52))
            {
                finishCycle();
                return;
            }

            step = 4;
            delayTimer = 20;
            waitingForGui = false;
        }
    }

    private void clickCurrentGui(int action)
    {
        if(!(MC.screen instanceof AbstractContainerScreen<?> screen))
        {
            delayTimer = 1;
            return;
        }

        int slotIndex;

        if(activeMode == QuestMode.QUEST_53_2)
            slotIndex = 1;
        else if(action == 1)
            slotIndex = 3;
        else
            slotIndex = 4;

        if(!clickSlot(screen, slotIndex))
        {
            finishCycle();
            return;
        }

        if(activeMode == QuestMode.QUEST_53_4_53_5
            && action == 1)
        {
            step = 3;
            openQuestGui();
            return;
        }

        finishCycle();
    }

    private void finishCycle()
    {
        step = 2;
        timer = 0;
        delayTimer = 0;
        waitingForGui = false;
    }

    private boolean clickSlot(
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
}
```
