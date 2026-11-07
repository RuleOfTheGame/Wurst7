/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General
 * Public License, version 3. If a copy of the GPL was not
 * distributed with this file, You can obtain one at:
 * https://www.gnu.org/licenses/gpl-3.0.txt
 */

package net.wurstclient.hacks;

import java.util.Comparator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import net.wurstclient.Category;
import net.wurstclient.ai.PathFinder;
import net.wurstclient.ai.PathProcessor;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;

public final class AutoPumpkinHack extends Hack
    implements UpdateListener
{
    private final SliderSetting searchRange =
        new SliderSetting("Search range",
            "Range for finding pumpkins.",
            32, 8, 64, 1, ValueDisplay.INTEGER);

    private PumpkinPathFinder pathFinder;
    private PathProcessor processor;

    private BlockPos targetPumpkin;
    private boolean completed;

    public AutoPumpkinHack()
    {
        super("AutoPumpkin");
        setCategory(Category.BLOCKS);

        addSetting(searchRange);
    }

    @Override
    protected void onEnable()
    {
        pathFinder = null;
        processor = null;
        targetPumpkin = null;
        completed = false;

        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);

        PathProcessor.releaseControls();

        pathFinder = null;
        processor = null;
        targetPumpkin = null;
        completed = false;
    }

    @Override
    public void onUpdate()
    {
        if(MC.player == null || MC.level == null)
            return;

        if(completed)
            return;

        if(targetPumpkin == null)
        {
            findPumpkin();
            return;
        }

        if(pathFinder == null)
        {
            pathFinder = new PumpkinPathFinder(targetPumpkin);
            return;
        }

        if(!pathFinder.isDone() && !pathFinder.isFailed())
        {
            PathProcessor.lockControls();

            pathFinder.think();

            if(pathFinder.isDone() || pathFinder.isFailed())
            {
                if(pathFinder.isDone())
                {
                    pathFinder.formatPath();
                    processor = pathFinder.getProcessor();
                }
                else
                {
                    completed = true;
                    PathProcessor.releaseControls();
                }
            }

            return;
        }

        if(processor == null)
        {
            completed = true;
            PathProcessor.releaseControls();
            return;
        }

        if(processor.isDone())
        {
            completed = true;
            PathProcessor.releaseControls();
            return;
        }

        if(!pathFinder.isPathStillValid(processor.getIndex())
            || processor.getTicksOffPath() > 20)
        {
            pathFinder = new PumpkinPathFinder(targetPumpkin);
            processor = null;
            return;
        }

        processor.process();
    }

    private void findPumpkin()
    {
        int range = (int)searchRange.getValue();

        BlockPos playerPos =
            BlockPos.containing(MC.player.position());

        BlockPos min = playerPos.offset(-range, -range, -range);
        BlockPos max = playerPos.offset(range, range, range);

        targetPumpkin = BlockUtils.getAllInBoxStream(min, max)
            .filter(pos -> MC.level.getBlockState(pos)
                .is(Blocks.PUMPKIN))
            .min(Comparator.comparingDouble(
                pos -> MC.player.distanceToSqr(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5)))
            .orElse(null);

        if(targetPumpkin != null)
        {
            pathFinder = new PumpkinPathFinder(targetPumpkin);
            processor = null;
        }
        else
        {
            completed = true;
        }
    }

    private class PumpkinPathFinder extends PathFinder
    {
        private final BlockPos pumpkin;

        public PumpkinPathFinder(BlockPos pumpkin)
        {
            super(BlockPos.containing(MC.player.position()));

            this.pumpkin = pumpkin;
        }

        @Override
        protected boolean isMineable(BlockPos pos)
        {
            return false;
        }

        @Override
        protected boolean checkDone()
        {
            return done =
                current.north().equals(pumpkin)
                || current.south().equals(pumpkin)
                || current.east().equals(pumpkin)
                || current.west().equals(pumpkin);
        }
    }
}
