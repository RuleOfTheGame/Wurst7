
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
import net.minecraft.world.inventory.ContainerInput;
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

    private int automob1Timer;
    private int automob2Timer;
    private int autobossTimer;

    private boolean waitingForGui;
    private int pendingSlots;
    private Screen previousScreen;

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

        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);

        waitingForGui = false;
        pendingSlots = 0;
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

        clickQuestSlot(screen, 27, 1);
        clickQuestSlot(screen, 28, 2);
        clickQuestSlot(screen, 29, 4);

        waitingForGui = false;
    }

    private void clickQuestSlot(
        AbstractContainerScreen<?> screen, int slotIndex, int flag)
    {
        if((pendingSlots & flag) == 0)
            return;

        if(screen.getMenu().slots.size() <= slotIndex)
            return;

        Slot slot = screen.getMenu().slots.get(slotIndex);

        screen.slotClicked(
            slot, slot.index, 0, ContainerInput.PICKUP);

        pendingSlots &= ~flag;

        if(flag == 1)
            automob1Timer = 0;
        else if(flag == 2)
            automob2Timer = 0;
        else if(flag == 4)
            autobossTimer = 0;
    }
}

