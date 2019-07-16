package com.builtbroken.atomic.map.thermal.thread;

import com.builtbroken.atomic.AtomicScience;
import com.builtbroken.atomic.api.thermal.IThermalNode;
import com.builtbroken.atomic.api.thermal.IThermalSource;
import com.builtbroken.atomic.config.server.ConfigServer;
import com.builtbroken.atomic.lib.thermal.ThermalHandler;
import com.builtbroken.atomic.map.data.DataChange;
import com.builtbroken.atomic.map.data.DataPos;
import com.builtbroken.atomic.map.data.ThreadDataChange;
import com.builtbroken.atomic.map.thermal.node.ThermalNode;
import com.builtbroken.jlib.data.vector.IPos3D;
import com.builtbroken.jlib.lang.StringHelpers;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.PooledMutableBlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Handles updating the radiation map
 * <p>
 * <p>
 * Created by Dark(DarkGuardsman, Robert) on 4/28/2018.
 */
public class ThreadThermalAction extends ThreadDataChange
{
    public ThreadThermalAction()
    {
        super("ThreadThermalAction");
    }

    @Override
    protected boolean updateLocation(DataChange change)
    {
        //Get world
        final World world = DimensionManager.getWorld(change.dim());

        //Position
        final int cx = change.xi();
        final int cy = change.yi();
        final int cz = change.zi();

        //validate that we are the correct type to run
        if (world instanceof WorldServer && change.source instanceof IThermalSource)
        {
            //Setup data storage
            final ThermalThreadData thermalThreadData = createData(world, cx, cy, cz, ConfigServer.THREAD.THREAD_HEAT_PATHING_RANGE);

            //Run pathfinder
            calculateHeatSpread(thermalThreadData, change.value);

            //Mark as completed
            completeUpdateLocation(world, (IThermalSource) change.source, thermalThreadData);
            return true;
        }
        return false;
    }

    /**
     * Creates a new data wrapper for the thread action. Use this to override the data storage
     * system to introduce hooks.
     *
     * @param world
     * @param cx
     * @param cy
     * @param cz
     * @param range
     * @return
     */
    protected ThermalThreadData createData(World world, int cx, int cy, int cz, int range)
    {
        return new ThermalThreadData(world, cx, cy, cz, range);
    }

    /**
     * Called when the action has completed with pathing
     *
     * @param world
     * @param source
     * @param data
     */
    protected void completeUpdateLocation(World world, IThermalSource source, ThermalThreadData data)
    {
        ((WorldServer) world).addScheduledTask(new ThermalServerTask(source, data));
    }

    /**
     * Calculates spread of heat from source.
     * <p>
     * Works by pathing all tiles and calculating heat movement towards current tile.
     * In other words: Pulls heat towards tile instead of pushing heat.
     * <p>
     * Heat is not consumed for movement as would be expected with real world movement. Instead
     * its assumed heat will constantly be generated. Thus migrating heat is not needed beyond
     * estimating how much heat would be moved.
     *
     * @param thermalThreadData - data about the current thread job
     * @param heatTotal         - amount of heat to move
     * @return positions and changes (first pos is position, second is data (x -> heat, y -> heat used))
     */
    protected void calculateHeatSpread(final ThermalThreadData thermalThreadData, final int heatTotal)
    {
        //TODO consider splitting over several threads
        //TODO map fluid(water, air, lava, etc) pockets to allow convection currents
        //TODO use fluid pockets to equalize heat levels


        long time = System.nanoTime();
        if (heatTotal > 6)
        {
            //Current nodes to path
            final Queue<DataPos> currentPathQueue = new LinkedList();

            //Nodes pathed in last run
            final Queue<DataPos> normalizeQueue = new LinkedList();

            //Nodes to path in next run
            final List<DataPos> nextPathQueue = new LinkedList();

            //Add center point
            thermalThreadData.setHeat(DataPos.get(thermalThreadData), heatTotal);
            currentPathQueue.add(DataPos.get(thermalThreadData));

            //Breadth first pathfinder
            while (!currentPathQueue.isEmpty() || !nextPathQueue.isEmpty())
            {
                if (currentPathQueue.isEmpty())
                {
                    //Normalize limits of tile before running pushes
                    normalizeQueue.forEach(pos ->
                    {
                        thermalThreadData.normalize(pos);
                        pos.dispose();
                    });

                    //Push next shell to queue
                    currentPathQueue.addAll(nextPathQueue);
                    nextPathQueue.clear();
                }

                //Get next and set to push heat
                final DataPos nextPathPos = thermalThreadData.setToPush(currentPathQueue.poll());

                //Calculate heat pushed from all sides and look for new tiles to path
                pathNext(thermalThreadData, nextPathPos, (x, y, z, heat) ->
                {
                    if (heat > 0)
                    {
                        final DataPos pos = DataPos.get(x, y, z);

                        //Check if we need to queue pathing, if we have head data its already in the queue
                        if (!thermalThreadData.hasData(pos))
                        {
                            nextPathQueue.add(DataPos.get(pos));
                        }

                        //Add heat, will make a new pos as needed
                        thermalThreadData.addHeat(pos, heat);

                        //Recycle
                        pos.dispose();
                    }
                });

                //Queue so we can normalize before running next sheep
                normalizeQueue.offer(nextPathPos);
            }
        }

        //Logging
        if (AtomicScience.runningAsDev)
        {
            time = System.nanoTime() - time;
            AtomicScience.logger.info(String.format("%s: Spread heat %s | %s tiles | %s %s %s | in %s",
                    name,
                    heatTotal,
                    thermalThreadData.getData().size(),
                    thermalThreadData.xi(),
                    thermalThreadData.yi(),
                    thermalThreadData.zi(),
                    StringHelpers.formatNanoTime(time)));
        }
    }

    private void pathNext(final ThermalThreadData thermalThreadData, final DataPos currentPos, HeatPushCallback heatSetter)
    {
        //Block giving heat
        final IBlockState giverBlock = thermalThreadData.world.getBlockState(currentPos.getPos());

        //Total heat to give
        final int totalMovementHeat = thermalThreadData.getHeatToMove(currentPos);

        //Amount of heat lost in the movement
        final int heatLoss = ThermalHandler.getHeatLost(giverBlock, totalMovementHeat);

        //Create a pooled blockpos for reuse to save memory
        final PooledMutableBlockPos blockPos = PooledMutableBlockPos.retain();

        //Only loop values we had within range
        forEach(thermalThreadData, currentPos, (x, y, z, dir) ->
        {
            blockPos.setPos(x, y, z); //TODO use mutable pos

            //Block receiving heat
            final IBlockState targetBlock = thermalThreadData.world.getBlockState(blockPos);

            //Calculate heat to move to current position from direction
            int heatMoved = ThermalHandler.getHeatMoved(targetBlock, totalMovementHeat);
            heatMoved = Math.max(0, heatMoved - heatLoss);

            //Push heat
            heatSetter.pushHeat(x, y, z, heatMoved);
        });

        //Release block pos
        blockPos.release();
    }

    private void forEach(final IPos3D center, final DataPos currentPos, HeatDirConsumer directionConsumer) //TODO move to helper
    {
        for (EnumFacing direction : EnumFacing.values())
        {
            //Check range to prevent infinite spread
            int x = currentPos.x + direction.getXOffset();
            int y = currentPos.y + direction.getYOffset();
            int z = currentPos.z + direction.getZOffset();
            if (inRange(center, x, y, z, ConfigServer.THREAD.THREAD_HEAT_PATHING_RANGE) && y >= 0 && y < 256)
            {
                directionConsumer.accept(x, y, z, direction);
            }
        }
    }

    public interface HeatDirConsumer
    {

        void accept(int x, int y, int z, EnumFacing direction);
    }
}
