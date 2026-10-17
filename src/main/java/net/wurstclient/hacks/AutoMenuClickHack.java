
package net.wurstclient.hacks;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

public final class AutoMenuClickHack extends Hack
    implements UpdateListener
{
    private static final int TARGET_SLOT = 13;
    private static final int INTERVAL_TICKS = 9 * 60 * 20;

    private int tickCounter;
    private boolean waitingForMenu;
    private Screen screenBeforeCommand;

    public AutoMenuClickHack()
    {
        super("AutoMenuClick");
        setCategory(Category.ITEMS);
    }

    @Override
    protected void onEnable()
    {
        tickCounter = 0;
        waitingForMenu = false;

        EVENTS.add(UpdateListener.class, this);
        openMenu();
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);
        waitingForMenu = false;
    }

    private void openMenu()
    {
        if(MC.player == null || MC.getConnection() == null)
            return;

        screenBeforeCommand = MC.screen;
        MC.player.connection.sendCommand("menu");
        waitingForMenu = true;
    }

    @Override
    public void onUpdate()
    {
        if(MC.player == null || MC.gameMode == null)
            return;

        if(waitingForMenu)
        {
            if(!(MC.screen instanceof AbstractContainerScreen<?> screen))
                return;

            if(MC.screen == screenBeforeCommand)
                return;

            if(screen.getMenu().slots.size() <= TARGET_SLOT)
            {
                waitingForMenu = false;
                return;
            }

            Slot slot = screen.getMenu().slots.get(TARGET_SLOT);
            screen.slotClicked(
                slot, slot.index, 0, ClickType.PICKUP);

            waitingForMenu = false;
            tickCounter = 0;
            return;
        }

        if(++tickCounter < INTERVAL_TICKS)
            return;

        tickCounter = 0;
        openMenu();
    }
}
