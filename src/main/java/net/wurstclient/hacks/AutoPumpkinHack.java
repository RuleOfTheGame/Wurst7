```java
/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General
 * Public License, version 3.
 */

package net.wurstclient.hacks;

import java.util.Comparator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;

@SearchTags({"auto pumpkin", "pumpkin finder"})
public final class AutoPumpkinHack extends Hack
    implements UpdateListener
{
    private final SliderSetting range = new SliderSetting(
        "Range", "Range to search for pumpkins.",
        16, 4, 32, 1, ValueDisplay.INTEGER.withSuffix(" blocks"));

    private BlockPos target;

    public AutoPumpkinHack()
    {
        super("AutoPumpkin");
        setCategory(Category.MOVEMENT);

        addSetting(range);
    }

    @Override
    protected void onEnable()
    {
        target = null;
        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);
        stopMoving();
        target = null;
    }

    @Override
    public void onUpdate()
    {
        if(MC.player == null || MC.level == null)
        {
            stopMoving();
            return;
        }

        target = findNearestPumpkin();

        if(target == null)
        {
            stopMoving();
            return;
        }

        Vec3 targetVec = Vec3.atCenterOf(target);

        // Stop when close enough to the pumpkin.
        if(MC.player.position().distanceToSqr(targetVec) <= 2.25)
        {
            stopMoving();
            return;
        }

        // Face the pumpkin.
        WURST.getRotationFaker().faceVectorClient(targetVec);

        // Move forward.
        MC.options.keyUp.setDown(true);

        // Jump over obstacles.
        boolean shouldJump =
            MC.player.horizontalCollision && MC.player.onGround();

        MC.options.keyJump.setDown(shouldJump);
    }

    private BlockPos findNearestPumpkin()
    {
        BlockPos playerPos =
            BlockPos.containing(MC.player.position());

        int r = range.getValueI();

        BlockPos from = playerPos.offset(-r, -r, -r);
        BlockPos to = playerPos.offset(r, r, r);

        return BlockUtils.getAllInBoxStream(from, to)
            .filter(pos ->
                BlockUtils.getBlock(pos) == Blocks.PUMPKIN)
            .min(Comparator.comparingDouble(
                pos -> pos.distToCenterSqr(
                    MC.player.position())))
            .orElse(null);
    }

    private void stopMoving()
    {
        MC.options.keyUp.setDown(false);
        MC.options.keyJump.setDown(false);
    }
}
```
