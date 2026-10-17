
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

public final class AutoChatHack extends Hack implements UpdateListener
{
    private static final String COMMAND = "kho sellall";
    private static final int INTERVAL_TICKS = 1500 * 20;

    private int tickCounter;
    private boolean firstCommandSent;

    public AutoChatHack()
    {
        super("AutoChat");
        setCategory(Category.ITEMS);
    }

    @Override
    protected void onEnable()
    {
        tickCounter = 0;
        firstCommandSent = false;
        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);
    }

    @Override
    public void onUpdate()
    {
        if(MC.player == null || MC.getConnection() == null)
            return;

        if(!firstCommandSent)
        {
            MC.player.connection.sendCommand(COMMAND);
            firstCommandSent = true;
            tickCounter = 0;
            return;
        }

        if(++tickCounter < INTERVAL_TICKS)
            return;

        tickCounter = 0;
        MC.player.connection.sendCommand(COMMAND);
    }
}
