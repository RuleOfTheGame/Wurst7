

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
import net.wurstclient.ai.PathFinder;
import net.wurstclient.ai.PathProcessor;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"pumpkin bot", "pumpkin finder"})
public final class PumpkinBotHack extends Hack
	implements UpdateListener
{
	private static final double SEARCH_ANGLE = 60;

	private final SliderSetting searchRange = new SliderSetting(
		"Search range", "How far to search for pumpkins.",
		32, 4, 64, 1, ValueDisplay.INTEGER);

	private final CheckboxSetting autoWalk =
		new CheckboxSetting("Auto walk",
			"Automatically walk to the nearest pumpkin.", true);

	private BlockPos target;
	private PumpkinPathFinder pathFinder;
	private PathProcessor processor;

	public PumpkinBotHack()
	{
		super("PumpkinBot");
		setCategory(Category.BLOCKS);
		addSetting(searchRange);
		addSetting(autoWalk);
	}

	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}

	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		resetPath();
	}

	@Override
	public void onUpdate()
	{
		if(MC.player == null || MC.level == null)
			return;

		if(!autoWalk.isChecked())
		{
			resetPath();
			return;
		}

		// Target was destroyed by another hack.
		if(target != null && !isPumpkin(target))
		{
			resetPath();
			return;
		}

		// Face the current target.
		if(target != null)
			WURST.getRotationFaker()
				.faceVectorClient(Vec3.atCenterOf(target));

		// Wait next to the target for the other hack to break it.
		if(target != null && isNextToTarget(
			BlockPos.containing(MC.player.position()), target))
		{
			PathProcessor.releaseControls();
			pathFinder = null;
			processor = null;
			return;
		}

		// Find the nearest pumpkin in front of the player.
		if(target == null)
		{
			target = findNearestPumpkin();

			if(target == null)
				return;

			pathFinder = new PumpkinPathFinder(target);
		}

		if(pathFinder == null)
			return;

		// Calculate path.
		if(!pathFinder.isDone() && !pathFinder.isFailed())
		{
			PathProcessor.lockControls();
			pathFinder.think();
			return;
		}

		if(pathFinder.isFailed())
		{
			resetPath();
			return;
		}

		// Create movement processor.
		if(processor == null)
		{
			pathFinder.formatPath();
			processor = pathFinder.getProcessor();
		}

		if(processor == null)
		{
			resetPath();
			return;
		}

		// Check whether the path is still valid.
		if(!pathFinder.isPathStillValid(processor.getIndex())
			|| processor.getTicksOffPath() > 20)
		{
			pathFinder = new PumpkinPathFinder(target);
			processor = null;
			return;
		}

		// Move toward the target.
		PathProcessor.lockControls();
		processor.process();

		if(processor.isDone())
		{
			PathProcessor.releaseControls();
			processor = null;
			pathFinder = null;
		}
	}

	private BlockPos findNearestPumpkin()
	{
		BlockPos playerPos =
			BlockPos.containing(MC.player.position());

		int range = searchRange.getValueI();

		return BlockUtils.getAllInBoxStream(
			playerPos.offset(-range, -range, -range),
			playerPos.offset(range, range, range))
			.filter(this::isPumpkin)
			.filter(this::isInFront)
			.min(Comparator.comparingDouble(
				pos -> pos.distToCenterSqr(MC.player.position())))
			.orElse(null);
	}

	private boolean isInFront(BlockPos pos)
	{
		Vec3 center = Vec3.atCenterOf(pos);

		return RotationUtils.getAngleToLookVec(center)
			<= SEARCH_ANGLE;
	}

	private boolean isPumpkin(BlockPos pos)
	{
		var block = BlockUtils.getBlock(pos);

		return block == Blocks.PUMPKIN
			|| block == Blocks.CARVED_PUMPKIN
			|| block == Blocks.JACK_O_LANTERN;
	}

	private boolean isNextToTarget(BlockPos pos, BlockPos pumpkin)
	{
		return pos.north().equals(pumpkin)
			|| pos.east().equals(pumpkin)
			|| pos.south().equals(pumpkin)
			|| pos.west().equals(pumpkin)
			|| pos.above().equals(pumpkin)
			|| pos.below().equals(pumpkin);
	}

	private void resetPath()
	{
		PathProcessor.releaseControls();

		target = null;
		pathFinder = null;
		processor = null;
	}

	private class PumpkinPathFinder extends PathFinder
	{
		private final BlockPos pumpkin;

		public PumpkinPathFinder(BlockPos pumpkin)
		{
			super(pumpkin);
			this.pumpkin = pumpkin;
		}

		@Override
		protected boolean checkDone()
		{
			return done = isNextToTarget(current, pumpkin);
		}
	}
}
